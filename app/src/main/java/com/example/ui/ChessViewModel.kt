package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChessRepository
import com.example.data.FriendItemUi
import com.example.data.GameModeType
import com.example.data.GameRecordDto
import com.example.data.MatchSummaryUi
import com.example.data.TimeControlOption
import com.example.data.UserProfileDto
import com.example.engine.AiDifficulty
import com.example.engine.BeginnerMovementRoute
import com.example.engine.BoardSquare
import com.example.engine.ChessAiEngine
import com.example.engine.ChessBoardState
import com.example.engine.ChessEngine
import com.example.engine.ChessMove
import com.example.engine.GameStatus
import com.example.engine.PieceColor
import com.example.engine.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingPromotion(
    val from: BoardSquare,
    val to: BoardSquare,
    val color: PieceColor
)

data class ChessUiState(
    val currentUser: UserProfileDto = UserProfileDto(
        id = "user_local_grandmaster",
        username = "alex_mercer",
        displayName = "Alex Mercer"
    ),
    val isSupabaseConfigured: Boolean = false,
    val activeSupabaseUrl: String = "",
    val activeSupabaseAnonKey: String = "",
    val supabaseConnectionStatus: String? = null,
    val isTestingSupabaseConnection: Boolean = false,
    val isAuthenticatedOnline: Boolean = false,
    val authErrorMessage: String? = null,
    val statusBannerMessage: String? = null,

    // Dashboard Match Categorization
    val yourTurnMatches: List<MatchSummaryUi> = emptyList(),
    val awaitingOpponentMatches: List<MatchSummaryUi> = emptyList(),
    val archivedMatches: List<MatchSummaryUi> = emptyList(),

    // Social & Friend Search
    val friends: List<FriendItemUi> = emptyList(),
    val friendSearchQuery: String = "",
    val friendSearchResults: List<UserProfileDto> = emptyList(),

    // Active Match Screen State
    val activeMatch: MatchSummaryUi? = null,
    val boardState: ChessBoardState = ChessEngine.parseFen(),
    val selectedSquare: BoardSquare? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val beginnerRoutes: List<BeginnerMovementRoute> = emptyList(),
    val isBeginnerModeEnabled: Boolean = true,
    val isBoardFlipped: Boolean = false,
    val pendingPromotion: PendingPromotion? = null,
    val hintMove: ChessMove? = null,
    val whiteClockMs: Long = 600_000L,
    val blackClockMs: Long = 600_000L,
    val isBotThinking: Boolean = false,
    val selectedAiDifficulty: AiDifficulty = AiDifficulty.CLUB,
    val lastAiEvalCentipawns: Int = 0,

    // Time Clock Configuration & Settings Menu State
    val isClockEnabled: Boolean = true,
    val defaultTimeControl: TimeControlOption = TimeControlOption.RAPID_10_0,
    val useCustomClockDuration: Boolean = false,
    val customClockMinutes: Int = 10,
    val customIncrementSeconds: Int = 0,

    // Modals & Drawers
    val showAuthSheet: Boolean = false,
    val showFriendDrawer: Boolean = false,
    val showNewMatchSheet: Boolean = false,
    val showSqlSchemaSheet: Boolean = false,
    val showClockSettingsSheet: Boolean = false,
    val preselectedOpponentForChallenge: UserProfileDto? = null,
    val preselectedModeForNewMatch: GameModeType = GameModeType.AI_OPPONENT
)

class ChessViewModel(
    private val repository: ChessRepository = ChessRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChessUiState(
            isSupabaseConfigured = repository.isSupabaseConfigured,
            activeSupabaseUrl = if (repository.isSupabaseConfigured) repository.activeSupabaseUrl else "",
            activeSupabaseAnonKey = if (repository.isSupabaseConfigured) repository.activeSupabaseAnonKey else ""
        )
    )
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private var clockJob: Job? = null
    private var botMoveJob: Job? = null

    init {
        combine(
            repository.currentUser,
            repository.isAuthenticatedOnline,
            repository.friends,
            repository.matches
        ) { user, onlineAuth, friendsList, allMatches ->
            val yourTurn = allMatches.filter { !it.gameStatus.isTerminal && it.isMyTurn }
            val awaiting = allMatches.filter { !it.gameStatus.isTerminal && !it.isMyTurn }
            val archived = allMatches.filter { it.gameStatus.isTerminal }

            _uiState.update { current ->
                val refreshedActive = current.activeMatch?.let { act ->
                    allMatches.firstOrNull { it.record.id == act.record.id } ?: act
                }
                current.copy(
                    currentUser = user,
                    isAuthenticatedOnline = onlineAuth,
                    friends = friendsList,
                    yourTurnMatches = yourTurn,
                    awaitingOpponentMatches = awaiting,
                    archivedMatches = archived,
                    activeMatch = refreshedActive
                )
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            val initialResults = repository.searchUsersByUsername("")
            _uiState.update { it.copy(friendSearchResults = initialResults) }
        }
    }

    // ------------------------ NAVIGATION & MATCH OPENING ------------------------

    fun openMatch(match: MatchSummaryUi) {
        val lastFrom = BoardSquare.fromAlgebraic(match.record.lastMoveFrom)
        val lastTo = BoardSquare.fromAlgebraic(match.record.lastMoveTo)
        val parsedBoard = ChessEngine.parseFen(
            fen = match.record.currentFen,
            lastFrom = lastFrom,
            lastTo = lastTo
        )
        val shouldFlip = match.playerColor == PieceColor.BLACK &&
            match.modeType != GameModeType.PASS_AND_PLAY

        val evalCp = ChessAiEngine.evaluateBoardForColor(parsedBoard, PieceColor.WHITE)

        _uiState.update {
            it.copy(
                activeMatch = match,
                boardState = parsedBoard,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                beginnerRoutes = emptyList(),
                isBoardFlipped = shouldFlip,
                pendingPromotion = null,
                hintMove = null,
                whiteClockMs = match.record.whiteTimeRemainingMs,
                blackClockMs = match.record.blackTimeRemainingMs,
                selectedAiDifficulty = match.aiDifficulty ?: it.selectedAiDifficulty,
                lastAiEvalCentipawns = evalCp,
                statusBannerMessage = null
            )
        }

        if (match.modeType == GameModeType.ONLINE_SUPABASE) {
            repository.subscribeToMatchRealtime(match.record.id) { remoteRecord ->
                onRemoteMatchUpdated(remoteRecord)
            }
        }

        startClockTickerIfNeeded()
        triggerAiMoveIfAiTurn()
    }

    fun closeActiveMatch() {
        clockJob?.cancel()
        botMoveJob?.cancel()
        repository.unsubscribeFromMatchRealtime()
        _uiState.update {
            it.copy(
                activeMatch = null,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                beginnerRoutes = emptyList(),
                pendingPromotion = null,
                hintMove = null,
                isBotThinking = false
            )
        }
    }

    private fun onRemoteMatchUpdated(record: GameRecordDto) {
        val currentActive = _uiState.value.activeMatch ?: return
        if (currentActive.record.id != record.id) return

        val lastFrom = BoardSquare.fromAlgebraic(record.lastMoveFrom)
        val lastTo = BoardSquare.fromAlgebraic(record.lastMoveTo)
        val newBoard = ChessEngine.parseFen(record.currentFen, lastFrom, lastTo)
        val evalCp = ChessAiEngine.evaluateBoardForColor(newBoard, PieceColor.WHITE)

        _uiState.update {
            it.copy(
                activeMatch = currentActive.copy(record = record),
                boardState = newBoard,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                beginnerRoutes = emptyList(),
                whiteClockMs = record.whiteTimeRemainingMs,
                blackClockMs = record.blackTimeRemainingMs,
                lastAiEvalCentipawns = evalCp
            )
        }
    }

    // ------------------------ BOARD INTERACTION & MOVES ------------------------

    fun toggleBeginnerMode() {
        _uiState.update { state ->
            val nextBeginner = !state.isBeginnerModeEnabled
            val routes = if (nextBeginner && state.selectedSquare != null) {
                ChessEngine.computeBeginnerRoutes(state.boardState, state.selectedSquare)
            } else {
                emptyList()
            }
            state.copy(
                isBeginnerModeEnabled = nextBeginner,
                beginnerRoutes = routes
            )
        }
    }

    fun toggleFlipBoard() {
        _uiState.update { it.copy(isBoardFlipped = !it.isBoardFlipped) }
    }

    fun changeActiveAiDifficulty(difficulty: AiDifficulty) {
        _uiState.update { state ->
            val updatedActive = state.activeMatch?.let { act ->
                if (act.modeType == GameModeType.AI_OPPONENT) {
                    act.copy(aiDifficulty = difficulty)
                } else {
                    act
                }
            }
            if (updatedActive != null) {
                repository.updateLocalMatchInList(updatedActive)
            }
            state.copy(
                selectedAiDifficulty = difficulty,
                activeMatch = updatedActive,
                statusBannerMessage = "AI Difficulty set to ${difficulty.title} (${difficulty.eloRating} ELO)"
            )
        }
    }

    fun onSquareTapped(square: BoardSquare) {
        val state = _uiState.value
        val match = state.activeMatch ?: return
        if (state.boardState.status.isTerminal || match.gameStatus.isTerminal || state.isBotThinking) return

        // Check if tapping a valid destination for the currently selected piece
        val currentlySelected = state.selectedSquare
        if (currentlySelected != null) {
            val matchingMoves = state.legalMovesForSelected.filter { it.to == square }
            if (matchingMoves.isNotEmpty()) {
                executeOrPromoteMove(currentlySelected, square, matchingMoves)
                return
            }
        }

        // Otherwise select the tapped square if it contains a piece of the active turn color
        val tappedPiece = state.boardState.pieceAt(square)
        if (tappedPiece != null && tappedPiece.color == state.boardState.turn && canPlayerMovePiece(match, tappedPiece.color)) {
            val legalMoves = ChessEngine.getLegalMovesForSquare(state.boardState, square)
            val routes = if (state.isBeginnerModeEnabled) {
                ChessEngine.computeBeginnerRoutes(state.boardState, square)
            } else {
                emptyList()
            }
            _uiState.update {
                it.copy(
                    selectedSquare = square,
                    legalMovesForSelected = legalMoves,
                    beginnerRoutes = routes,
                    hintMove = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    beginnerRoutes = emptyList()
                )
            }
        }
    }

    fun onPieceDragged(from: BoardSquare, to: BoardSquare) {
        val state = _uiState.value
        val match = state.activeMatch ?: return
        if (state.boardState.status.isTerminal || match.gameStatus.isTerminal || state.isBotThinking) return

        val movingPiece = state.boardState.pieceAt(from) ?: return
        if (movingPiece.color != state.boardState.turn || !canPlayerMovePiece(match, movingPiece.color)) return

        val legalMoves = ChessEngine.getLegalMovesForSquare(state.boardState, from)
        val matchingMoves = legalMoves.filter { it.to == to }
        if (matchingMoves.isNotEmpty()) {
            executeOrPromoteMove(from, to, matchingMoves)
        }
    }

    private fun canPlayerMovePiece(match: MatchSummaryUi, pieceColor: PieceColor): Boolean {
        return when (match.modeType) {
            GameModeType.PASS_AND_PLAY -> true
            GameModeType.AI_OPPONENT -> pieceColor == match.playerColor
            GameModeType.ONLINE_SUPABASE ->
                if (_uiState.value.isAuthenticatedOnline) pieceColor == match.playerColor else true
        }
    }

    private fun executeOrPromoteMove(
        from: BoardSquare,
        to: BoardSquare,
        matchingMoves: List<ChessMove>
    ) {
        val state = _uiState.value
        if (ChessEngine.isPromotionMove(state.boardState, from, to)) {
            _uiState.update {
                it.copy(
                    pendingPromotion = PendingPromotion(
                        from = from,
                        to = to,
                        color = state.boardState.turn
                    )
                )
            }
        } else {
            commitChessMove(matchingMoves.first())
        }
    }

    fun onPromotionPieceSelected(promotionType: PieceType) {
        val state = _uiState.value
        val pending = state.pendingPromotion ?: return
        val legalMoves = ChessEngine.getLegalMovesForSquare(state.boardState, pending.from)
        val chosenMove = legalMoves.firstOrNull {
            it.to == pending.to && it.promotion == promotionType
        }
        _uiState.update { it.copy(pendingPromotion = null) }
        if (chosenMove != null) {
            commitChessMove(chosenMove)
        }
    }

    fun dismissPromotionDialog() {
        _uiState.update { it.copy(pendingPromotion = null) }
    }

    private fun commitChessMove(move: ChessMove) {
        val state = _uiState.value
        val match = state.activeMatch ?: return
        val nextBoard = ChessEngine.applyMove(state.boardState, move)
        val increment = if (state.useCustomClockDuration) {
            state.customIncrementSeconds * 1_000L
        } else {
            match.timeControlOption.incrementMs
        }
        val clockActive = state.isClockEnabled &&
            match.timeControlOption != TimeControlOption.UNLIMITED &&
            (match.timeControlOption.isLiveClock || state.useCustomClockDuration)

        val updatedWhiteClock = if (move.piece.color == PieceColor.WHITE && clockActive) {
            state.whiteClockMs + increment
        } else {
            state.whiteClockMs
        }
        val updatedBlackClock = if (move.piece.color == PieceColor.BLACK && clockActive) {
            state.blackClockMs + increment
        } else {
            state.blackClockMs
        }

        val nextFen = ChessEngine.toFen(nextBoard)
        val evalCp = ChessAiEngine.evaluateBoardForColor(nextBoard, PieceColor.WHITE)

        val endBanner = when (nextBoard.status) {
            GameStatus.CHECKMATE -> {
                val winnerName = if (move.piece.color == PieceColor.WHITE) {
                    match.record.whiteUsername
                } else {
                    match.record.blackUsername
                }
                "Checkmate! $winnerName wins (${move.san})"
            }
            GameStatus.STALEMATE -> "Draw by Stalemate!"
            GameStatus.DRAW_FIFTY_MOVE -> "Draw by 50-Move Rule!"
            else -> if (nextBoard.isCheck) "${nextBoard.turn.displayName} King is in Check!" else null
        }

        viewModelScope.launch {
            val updatedMatch = repository.recordMoveAndBroadcast(
                match = match,
                move = move,
                nextFen = nextFen,
                nextTurn = nextBoard.turn,
                nextStatus = nextBoard.status,
                whiteTimeRemainingMs = updatedWhiteClock,
                blackTimeRemainingMs = updatedBlackClock
            )

            _uiState.update {
                it.copy(
                    activeMatch = updatedMatch,
                    boardState = nextBoard,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    beginnerRoutes = emptyList(),
                    hintMove = null,
                    whiteClockMs = updatedWhiteClock,
                    blackClockMs = updatedBlackClock,
                    lastAiEvalCentipawns = evalCp,
                    statusBannerMessage = endBanner
                )
            }

            triggerAiMoveIfAiTurn()
        }
    }

    private fun triggerAiMoveIfAiTurn() {
        botMoveJob?.cancel()
        val state = _uiState.value
        val match = state.activeMatch ?: return
        if (match.modeType != GameModeType.AI_OPPONENT) return
        if (state.boardState.status.isTerminal || match.gameStatus.isTerminal) return
        if (state.boardState.turn == match.playerColor) return

        val difficulty = match.aiDifficulty ?: state.selectedAiDifficulty

        botMoveJob = viewModelScope.launch {
            _uiState.update { it.copy(isBotThinking = true) }
            delay(420L)
            val currentBoard = _uiState.value.boardState
            val aiMove = withContext(Dispatchers.Default) {
                ChessAiEngine.computeAiMove(currentBoard, difficulty)
            }
            _uiState.update { it.copy(isBotThinking = false) }
            if (aiMove != null) {
                commitChessMove(aiMove)
            }
        }
    }

    fun requestTacticalHint() {
        val state = _uiState.value
        if (state.boardState.status.isTerminal) return
        viewModelScope.launch {
            val best = withContext(Dispatchers.Default) {
                ChessAiEngine.computeAiMove(state.boardState, AiDifficulty.MASTER)
            } ?: return@launch
            val legalMoves = ChessEngine.getLegalMovesForSquare(state.boardState, best.from)
            val routes = ChessEngine.computeBeginnerRoutes(state.boardState, best.from)
            _uiState.update {
                it.copy(
                    selectedSquare = best.from,
                    legalMovesForSelected = legalMoves,
                    beginnerRoutes = routes,
                    hintMove = best,
                    statusBannerMessage = "AI Engine Hint: ${best.san} (${best.from.toAlgebraic()} → ${best.to.toAlgebraic()})"
                )
            }
        }
    }

    fun simulateOpponentResponseInOnlineDemo() {
        val state = _uiState.value
        val match = state.activeMatch ?: return
        if (state.boardState.status.isTerminal || match.gameStatus.isTerminal) return
        viewModelScope.launch {
            val reply = withContext(Dispatchers.Default) {
                ChessAiEngine.computeAiMove(state.boardState, AiDifficulty.CLUB)
            } ?: return@launch
            commitChessMove(reply)
        }
    }

    // ------------------------ RESIGN, DRAW & CLOCKS ------------------------

    fun resignActiveMatch() {
        val match = _uiState.value.activeMatch ?: return
        if (match.gameStatus.isTerminal) return
        viewModelScope.launch {
            val updated = repository.resignMatch(match)
            _uiState.update {
                it.copy(
                    activeMatch = updated,
                    boardState = it.boardState.copy(status = GameStatus.RESIGNED),
                    statusBannerMessage = "Match ended by resignation."
                )
            }
        }
    }

    fun offerOrAcceptDrawInActiveMatch() {
        val match = _uiState.value.activeMatch ?: return
        if (match.gameStatus.isTerminal) return
        viewModelScope.launch {
            val updated = repository.offerOrAcceptDraw(match)
            val newStatus = GameStatus.fromDbValue(updated.record.status)
            val banner = if (newStatus == GameStatus.DRAW_AGREEMENT) {
                "Draw agreed!"
            } else {
                "Draw offer sent to ${match.opponentUsername}."
            }
            _uiState.update {
                it.copy(
                    activeMatch = updated,
                    boardState = it.boardState.copy(status = newStatus),
                    statusBannerMessage = banner
                )
            }
        }
    }

    private fun startClockTickerIfNeeded() {
        clockJob?.cancel()
        val sInitial = _uiState.value
        val match = sInitial.activeMatch ?: return
        if (!sInitial.isClockEnabled ||
            match.timeControlOption == TimeControlOption.UNLIMITED ||
            (!match.timeControlOption.isLiveClock && !sInitial.useCustomClockDuration) ||
            match.gameStatus.isTerminal
        ) {
            return
        }

        clockJob = viewModelScope.launch {
            while (true) {
                delay(200L)
                val s = _uiState.value
                val act = s.activeMatch ?: break
                if (!s.isClockEnabled ||
                    act.timeControlOption == TimeControlOption.UNLIMITED ||
                    s.boardState.status.isTerminal ||
                    act.gameStatus.isTerminal
                ) {
                    break
                }

                if (s.boardState.turn == PieceColor.WHITE) {
                    val nextW = (s.whiteClockMs - 200L).coerceAtLeast(0L)
                    if (nextW == 0L) {
                        val timedOut = act.copy(
                            record = act.record.copy(
                                status = GameStatus.TIMEOUT.dbValue,
                                winnerId = act.record.blackPlayerId
                            )
                        )
                        repository.updateLocalMatchInList(timedOut)
                        _uiState.update {
                            it.copy(
                                whiteClockMs = 0L,
                                activeMatch = timedOut,
                                boardState = it.boardState.copy(status = GameStatus.TIMEOUT),
                                statusBannerMessage = "White flagged on time!"
                            )
                        }
                        break
                    } else {
                        _uiState.update { it.copy(whiteClockMs = nextW) }
                    }
                } else {
                    val nextB = (s.blackClockMs - 200L).coerceAtLeast(0L)
                    if (nextB == 0L) {
                        val timedOut = act.copy(
                            record = act.record.copy(
                                status = GameStatus.TIMEOUT.dbValue,
                                winnerId = act.record.whitePlayerId
                            )
                        )
                        repository.updateLocalMatchInList(timedOut)
                        _uiState.update {
                            it.copy(
                                blackClockMs = 0L,
                                activeMatch = timedOut,
                                boardState = it.boardState.copy(status = GameStatus.TIMEOUT),
                                statusBannerMessage = "Black flagged on time!"
                            )
                        }
                        break
                    } else {
                        _uiState.update { it.copy(blackClockMs = nextB) }
                    }
                }
            }
        }
    }

    // ------------------------ SOCIAL, AUTH & NEW MATCH CREATION ------------------------

    fun updateFriendSearchQuery(query: String) {
        _uiState.update { it.copy(friendSearchQuery = query) }
        viewModelScope.launch {
            val results = repository.searchUsersByUsername(query)
            _uiState.update { it.copy(friendSearchResults = results) }
        }
    }

    fun sendFriendInvite(profile: UserProfileDto) {
        viewModelScope.launch {
            val match = repository.inviteByUsernameAndStartMatch(
                rawUsername = profile.username,
                timeControl = TimeControlOption.RAPID_10_0,
                playAsWhite = true
            )
            _uiState.update {
                it.copy(
                    activeMatch = null,
                    showFriendDrawer = false,
                    showNewMatchSheet = false,
                    statusBannerMessage = "Match started with @${match.opponentUsername}! Added to Your Turn."
                )
            }
        }
    }

    fun acceptFriendInvite(friendshipId: String) {
        viewModelScope.launch {
            val createdMatch = repository.acceptFriendInvite(friendshipId)
            _uiState.update {
                it.copy(
                    activeMatch = null,
                    showFriendDrawer = false,
                    statusBannerMessage = if (createdMatch != null) {
                        "Invite accepted! Match vs ${createdMatch.opponentUsername} added to Your Turn."
                    } else {
                        "Invite accepted! Match added to Your Turn."
                    }
                )
            }
        }
    }

    fun inviteByUsernameAndStartMatch(
        username: String,
        timeControl: TimeControlOption = TimeControlOption.RAPID_10_0,
        playAsWhite: Boolean = true
    ) {
        val cleaned = username.trim().removePrefix("@")
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            val created = repository.inviteByUsernameAndStartMatch(
                rawUsername = cleaned,
                timeControl = timeControl,
                playAsWhite = playAsWhite
            )
            _uiState.update {
                it.copy(
                    activeMatch = null,
                    showNewMatchSheet = false,
                    showFriendDrawer = false,
                    preselectedOpponentForChallenge = null,
                    statusBannerMessage = "Invited ${created.opponentUsername}! New match added to Your Turn."
                )
            }
        }
    }

    fun startMatchFromShareLinkOrQr(
        linkOrCode: String,
        timeControl: TimeControlOption = TimeControlOption.RAPID_10_0
    ) {
        val cleaned = linkOrCode.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            val created = repository.joinOrStartMatchFromInviteLinkOrCode(
                rawLinkOrCode = cleaned,
                timeControl = timeControl
            )
            _uiState.update {
                it.copy(
                    activeMatch = null,
                    showNewMatchSheet = false,
                    showFriendDrawer = false,
                    preselectedOpponentForChallenge = null,
                    statusBannerMessage = "Match vs ${created.opponentUsername} added to Your Turn!"
                )
            }
        }
    }

    fun startNewMatch(
        opponent: UserProfileDto?,
        modeType: GameModeType,
        aiDifficulty: AiDifficulty = _uiState.value.selectedAiDifficulty,
        timeControl: TimeControlOption,
        playAsWhite: Boolean
    ) {
        viewModelScope.launch {
            val created = repository.createNewMatch(
                opponent = opponent,
                modeType = modeType,
                aiDifficulty = aiDifficulty,
                timeControl = timeControl,
                playAsWhite = playAsWhite
            )
            _uiState.update {
                it.copy(
                    selectedAiDifficulty = aiDifficulty,
                    showNewMatchSheet = false,
                    showFriendDrawer = false,
                    preselectedOpponentForChallenge = null
                )
            }
            openMatch(created)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            val res = repository.signInWithEmail(email, password)
            res.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            showAuthSheet = false,
                            authErrorMessage = null,
                            statusBannerMessage = "Signed in as @${profile.username}"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(authErrorMessage = err.message ?: "Authentication failed")
                    }
                }
            )
        }
    }

    fun signUp(email: String, password: String, username: String, displayName: String) {
        viewModelScope.launch {
            val res = repository.signUpWithEmail(email, password, username, displayName)
            res.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            showAuthSheet = false,
                            authErrorMessage = null,
                            statusBannerMessage = "Profile @${profile.username} ready!"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(authErrorMessage = err.message ?: "Sign-up failed")
                    }
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update {
                it.copy(
                    showAuthSheet = false,
                    statusBannerMessage = "Switched to local grandmaster profile."
                )
            }
        }
    }

    fun configureAndTestSupabase(url: String, anonKey: String) {
        repository.configureSupabaseCredentials(url, anonKey)
        _uiState.update {
            it.copy(
                isSupabaseConfigured = repository.isSupabaseConfigured,
                activeSupabaseUrl = repository.activeSupabaseUrl,
                activeSupabaseAnonKey = repository.activeSupabaseAnonKey,
                isTestingSupabaseConnection = true,
                supabaseConnectionStatus = null,
                authErrorMessage = null
            )
        }
        viewModelScope.launch {
            val result = repository.testSupabaseConnection()
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isSupabaseConfigured = repository.isSupabaseConfigured,
                            isTestingSupabaseConnection = false,
                            supabaseConnectionStatus = msg,
                            authErrorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSupabaseConfigured = repository.isSupabaseConfigured,
                            isTestingSupabaseConnection = false,
                            supabaseConnectionStatus = null,
                            authErrorMessage = err.message ?: "Supabase connection test failed."
                        )
                    }
                }
            )
        }
    }

    // ------------------------ SHEET & DRAWER TOGGLES ------------------------

    fun setShowAuthSheet(show: Boolean) {
        _uiState.update { it.copy(showAuthSheet = show, authErrorMessage = null) }
    }

    fun setShowFriendDrawer(show: Boolean) {
        _uiState.update { it.copy(showFriendDrawer = show) }
    }

    fun openNewMatchSheet(
        preselectedOpponent: UserProfileDto? = null,
        preselectedMode: GameModeType = GameModeType.AI_OPPONENT
    ) {
        _uiState.update {
            it.copy(
                showNewMatchSheet = true,
                preselectedOpponentForChallenge = preselectedOpponent,
                preselectedModeForNewMatch = if (preselectedOpponent != null) {
                    GameModeType.ONLINE_SUPABASE
                } else {
                    preselectedMode
                }
            )
        }
    }

    fun closeNewMatchSheet() {
        _uiState.update {
            it.copy(
                showNewMatchSheet = false,
                preselectedOpponentForChallenge = null
            )
        }
    }

    fun setShowSqlSchemaSheet(show: Boolean) {
        _uiState.update { it.copy(showSqlSchemaSheet = show) }
    }

    fun setShowClockSettingsSheet(show: Boolean) {
        _uiState.update { it.copy(showClockSettingsSheet = show) }
    }

    fun setClockEnabled(enabled: Boolean) {
        if (!enabled) {
            clockJob?.cancel()
        }
        _uiState.update { state ->
            val updatedActive = state.activeMatch?.let { act ->
                val targetTc = if (!enabled) {
                    TimeControlOption.UNLIMITED
                } else if (act.timeControlOption == TimeControlOption.UNLIMITED) {
                    state.defaultTimeControl.takeIf { it != TimeControlOption.UNLIMITED }
                        ?: TimeControlOption.RAPID_10_0
                } else {
                    act.timeControlOption
                }
                act.copy(
                    record = act.record.copy(timeControl = targetTc.dbKey)
                )
            }
            if (updatedActive != null) {
                repository.updateLocalMatchInList(updatedActive)
            }
            state.copy(
                isClockEnabled = enabled,
                activeMatch = updatedActive,
                statusBannerMessage = if (enabled) {
                    "Time clock enabled."
                } else {
                    "Time clock turned OFF (Unlimited time)."
                }
            )
        }
        if (enabled) {
            startClockTickerIfNeeded()
        }
    }

    fun applyClockSettings(
        enabled: Boolean,
        timeControl: TimeControlOption,
        useCustom: Boolean,
        customMinutes: Int,
        customIncrementSec: Int,
        applyToActiveMatch: Boolean
    ) {
        clockJob?.cancel()
        val effectiveEnabled = enabled && timeControl != TimeControlOption.UNLIMITED
        val effectiveTimeControl = if (!effectiveEnabled) {
            TimeControlOption.UNLIMITED
        } else {
            timeControl
        }
        val safeMinutes = customMinutes.coerceIn(1, 180)
        val safeIncSec = customIncrementSec.coerceIn(0, 60)
        val targetDurationMs = when {
            !effectiveEnabled -> 0L
            useCustom -> safeMinutes * 60_000L
            else -> effectiveTimeControl.initialTimeMs
        }

        _uiState.update { state ->
            val updatedActive = if (applyToActiveMatch && state.activeMatch != null) {
                val act = state.activeMatch
                val newW = if (targetDurationMs > 0L) targetDurationMs else state.whiteClockMs
                val newB = if (targetDurationMs > 0L) targetDurationMs else state.blackClockMs
                act.copy(
                    record = act.record.copy(
                        timeControl = effectiveTimeControl.dbKey,
                        whiteTimeRemainingMs = newW,
                        blackTimeRemainingMs = newB
                    )
                )
            } else {
                state.activeMatch
            }

            if (updatedActive != null) {
                repository.updateLocalMatchInList(updatedActive)
            }

            val summaryMsg = when {
                !effectiveEnabled -> "Time clock turned OFF (Unlimited time)."
                useCustom -> "Time clock configured: ${safeMinutes}m + ${safeIncSec}s increment."
                else -> "Time clock set to ${effectiveTimeControl.label} (${effectiveTimeControl.shortBadge})."
            }

            state.copy(
                isClockEnabled = effectiveEnabled,
                defaultTimeControl = effectiveTimeControl,
                useCustomClockDuration = useCustom && effectiveEnabled,
                customClockMinutes = safeMinutes,
                customIncrementSeconds = safeIncSec,
                activeMatch = updatedActive,
                whiteClockMs = if (applyToActiveMatch && targetDurationMs > 0L) targetDurationMs else state.whiteClockMs,
                blackClockMs = if (applyToActiveMatch && targetDurationMs > 0L) targetDurationMs else state.blackClockMs,
                showClockSettingsSheet = false,
                statusBannerMessage = summaryMsg
            )
        }

        if (effectiveEnabled) {
            startClockTickerIfNeeded()
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}

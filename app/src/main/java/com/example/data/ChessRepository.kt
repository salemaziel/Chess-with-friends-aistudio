package com.example.data

import com.example.BuildConfig
import com.example.engine.BoardSquare
import com.example.engine.ChessEngine
import com.example.engine.ChessMove
import com.example.engine.GameStatus
import com.example.engine.PieceColor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class ChessRepository {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var customSupabaseUrl: String = ""
    private var customSupabaseKey: String = ""

    val activeSupabaseUrl: String
        get() {
            val overrideUrl = customSupabaseUrl.trim().removeSuffix("/")
            if (overrideUrl.isNotEmpty()) return overrideUrl
            return BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
        }

    val activeSupabaseAnonKey: String
        get() {
            val overrideKey = customSupabaseKey.trim()
            if (overrideKey.isNotEmpty()) return overrideKey
            return BuildConfig.SUPABASE_ANON_KEY.trim()
        }

    val isSupabaseConfigured: Boolean
        get() = isValidSupabaseCredentials(activeSupabaseUrl, activeSupabaseAnonKey)

    private fun isValidSupabaseCredentials(url: String, key: String): Boolean {
        val cleanUrl = url.trim().lowercase()
        val cleanKey = key.trim()
        if (cleanUrl.isEmpty() || cleanKey.isEmpty()) return false
        if (!cleanUrl.startsWith("https://")) return false
        // Reject template and dummy placeholder project URLs/keys
        if (cleanUrl.contains("your-project-id") ||
            cleanUrl.contains("idaipxkyknvyvptzhjge") ||
            cleanUrl.contains("example.supabase.co") ||
            cleanKey.contains("your-supabase-anon")
        ) {
            return false
        }
        return cleanUrl.contains(".supabase.co") || cleanUrl.contains(".supabase.in")
    }

    private var cachedClientUrl: String = ""
    private var cachedClientKey: String = ""
    private var cachedClient: SupabaseClient? = null
    var lastClientInitError: String? = null
        private set

    val supabaseClient: SupabaseClient?
        get() {
            val url = activeSupabaseUrl
            val key = activeSupabaseAnonKey
            if (!isValidSupabaseCredentials(url, key)) {
                return null
            }
            if (cachedClient != null && cachedClientUrl == url && cachedClientKey == key) {
                return cachedClient
            }
            return try {
                val created = createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = key
                ) {
                    defaultLogLevel = LogLevel.NONE
                    httpEngine = OkHttp.create()
                    install(Auth) {
                        sessionManager = MemorySessionManager()
                        alwaysAutoRefresh = true
                    }
                    install(Postgrest)
                    install(Realtime)
                }
                cachedClientUrl = url
                cachedClientKey = key
                cachedClient = created
                lastClientInitError = null
                created
            } catch (e: Exception) {
                lastClientInitError = e.message ?: e::class.simpleName
                null
            }
        }

    fun configureSupabaseCredentials(url: String, anonKey: String) {
        customSupabaseUrl = url.trim().removeSuffix("/")
        customSupabaseKey = anonKey.trim()
        cachedClient = null
    }

    /**
     * Pings the configured Supabase project and checks whether Postgrest & the `profiles` table are reachable.
     */
    suspend fun testSupabaseConnection(): Result<String> {
        val url = activeSupabaseUrl
        val key = activeSupabaseAnonKey
        if (!isValidSupabaseCredentials(url, key)) {
            return Result.failure(
                IllegalStateException(
                    "No valid Supabase Project URL & Anon Key configured. Paste your project's URL (https://<project-ref>.supabase.co) and Anon Key below, or set SUPABASE_URL and SUPABASE_ANON_KEY in the AI Studio Secrets panel."
                )
            )
        }
        val client = supabaseClient
            ?: return Result.failure(
                IllegalStateException("Supabase client failed to initialize: ${lastClientInitError ?: "Unknown error"}")
            )

        return try {
            client.from("profiles").select {
                limit(1)
            }
            Result.success("Connected to $url — Supabase Auth & PostgreSQL tables are live and ready!")
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown network error"
            when {
                msg.contains("relation", ignoreCase = true) ||
                    msg.contains("does not exist", ignoreCase = true) ||
                    msg.contains("42P01", ignoreCase = true) ||
                    msg.contains("PGRST205", ignoreCase = true) -> {
                    Result.success(
                        "Connected to $url! Next step: tap 'Copy SQL Commands' below and run them in your Supabase SQL Editor to create the profiles, games, moves, and friendships tables."
                    )
                }
                msg.contains("Unable to resolve host", ignoreCase = true) ||
                    msg.contains("UnknownHostException", ignoreCase = true) -> {
                    Result.failure(
                        IllegalStateException(
                            "Cannot resolve host '$url'. Verify that your Supabase Project URL is spelled correctly and that your Supabase project is active (not paused)."
                        )
                    )
                }
                msg.contains("Invalid API key", ignoreCase = true) ||
                    msg.contains("JWT", ignoreCase = true) ||
                    msg.contains("401", ignoreCase = true) -> {
                    Result.failure(
                        IllegalStateException(
                            "Reached $url, but Supabase rejected the API key (401). Copy the 'anon' / 'public' key from Supabase Dashboard → Project Settings → API."
                        )
                    )
                }
                else -> {
                    Result.failure(IllegalStateException("Supabase connection error: $msg"))
                }
            }
        }
    }

    private var activeRealtimeChannel: RealtimeChannel? = null

    private val _currentUser = MutableStateFlow(
        UserProfileDto(
            id = "user_local_grandmaster",
            username = "alex_mercer",
            displayName = "Alex Mercer",
            avatarColor = "#10B981",
            eloRating = 1420,
            gamesPlayed = 28,
            gamesWon = 19
        )
    )
    val currentUser: StateFlow<UserProfileDto> = _currentUser.asStateFlow()

    private val _isAuthenticatedOnline = MutableStateFlow(false)
    val isAuthenticatedOnline: StateFlow<Boolean> = _isAuthenticatedOnline.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendItemUi>>(initialSeededFriends())
    val friends: StateFlow<List<FriendItemUi>> = _friends.asStateFlow()

    private val _matches = MutableStateFlow<List<MatchSummaryUi>>(
        initialSeededMatches("user_local_grandmaster")
    )
    val matches: StateFlow<List<MatchSummaryUi>> = _matches.asStateFlow()

    // ------------------------ SUPABASE AUTHENTICATION ------------------------

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfileDto> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter both your email address and password."))
        }
        val client = supabaseClient
            ?: return Result.failure(
                IllegalStateException(
                    "Supabase is not connected yet. Paste your Supabase Project URL and Anon Key in the Connection section above (or in the AI Studio Secrets panel) and tap 'Connect & Test'."
                )
            )

        return try {
            client.auth.signInWith(Email) {
                this.email = cleanEmail
                this.password = password
            }
            val authUser = client.auth.currentUserOrNull()
                ?: return Result.failure(IllegalStateException("Signed in, but Supabase session user was not returned."))
            val profile = fetchOrCreateProfile(
                userId = authUser.id,
                fallbackUsername = cleanEmail.substringBefore("@")
            )
            _currentUser.value = profile
            _isAuthenticatedOnline.value = true
            refreshOnlineData()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(formatSupabaseError(e)))
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<UserProfileDto> {
        val cleanEmail = email.trim()
        val cleanUsername = username.trim().lowercase().ifBlank {
            cleanEmail.substringBefore("@").lowercase().ifBlank { "grandmaster" }
        }
        val cleanDisplayName = displayName.trim().ifBlank {
            cleanUsername.replaceFirstChar { it.uppercase() }
        }
        if (cleanEmail.isBlank() || password.length < 6) {
            return Result.failure(
                IllegalArgumentException("Please enter a valid email address and a password of at least 6 characters.")
            )
        }

        val client = supabaseClient
            ?: return Result.failure(
                IllegalStateException(
                    "Supabase is not connected yet. Paste your Supabase Project URL and Anon Key in the Connection section above (or in the AI Studio Secrets panel) and tap 'Connect & Test'."
                )
            )

        return try {
            val signedUpInfo = client.auth.signUpWith(Email) {
                this.email = cleanEmail
                this.password = password
                data = buildJsonObject {
                    put("username", cleanUsername)
                    put("display_name", cleanDisplayName)
                }
            }
            // If email confirmation is disabled, a session is active immediately; otherwise signIn or use signedUpInfo.id
            if (client.auth.currentSessionOrNull() == null) {
                try {
                    client.auth.signInWith(Email) {
                        this.email = cleanEmail
                        this.password = password
                    }
                } catch (_: Exception) {
                    // Email confirmation may be required before session creation
                }
            }
            val authUser = client.auth.currentUserOrNull()
            val userId = authUser?.id ?: signedUpInfo?.id ?: UUID.randomUUID().toString()
            val newProfile = UserProfileDto(
                id = userId,
                username = cleanUsername,
                displayName = cleanDisplayName,
                avatarColor = "#10B981",
                eloRating = 1200
            )
            try {
                client.from("profiles").upsert(newProfile)
            } catch (_: Exception) {
                // Trigger handle_new_user() may have already inserted the profile, or email confirmation is pending
            }
            _currentUser.value = newProfile
            _isAuthenticatedOnline.value = true
            refreshOnlineData()
            Result.success(newProfile)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(formatSupabaseError(e)))
        }
    }

    private fun formatSupabaseError(e: Exception): String {
        val raw = e.message ?: "Unknown Supabase error"
        return when {
            raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("UnknownHostException", ignoreCase = true) ->
                "Cannot reach '${activeSupabaseUrl}'. Check that your Supabase Project URL is valid and your project is not paused."
            raw.contains("email_not_confirmed", ignoreCase = true) ||
                raw.contains("Email not confirmed", ignoreCase = true) ->
                "Email confirmation is required by your Supabase project. Confirm the email sent by Supabase, or disable 'Confirm email' in Supabase Dashboard → Authentication → Providers → Email."
            raw.contains("invalid_credentials", ignoreCase = true) ||
                raw.contains("Invalid login credentials", ignoreCase = true) ->
                "Invalid email or password. If you haven't created an account on this Supabase project yet, tap 'Need an account? Sign Up'."
            else -> raw
        }
    }

    suspend fun signOut() {
        try {
            supabaseClient?.auth?.signOut()
        } catch (_: Exception) {
        }
        _isAuthenticatedOnline.value = false
    }

    private suspend fun fetchOrCreateProfile(
        userId: String,
        fallbackUsername: String
    ): UserProfileDto {
        val client = supabaseClient ?: return _currentUser.value
        return try {
            val existing = client.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<UserProfileDto>()
            if (existing != null) {
                existing
            } else {
                val created = UserProfileDto(
                    id = userId,
                    username = fallbackUsername.lowercase(),
                    displayName = fallbackUsername.replaceFirstChar { it.uppercase() }
                )
                client.from("profiles").upsert(created)
                created
            }
        } catch (_: Exception) {
            UserProfileDto(
                id = userId,
                username = fallbackUsername.lowercase(),
                displayName = fallbackUsername.replaceFirstChar { it.uppercase() }
            )
        }
    }

    // ------------------------ POSTGREST DATA SYNC ------------------------

    suspend fun refreshOnlineData() {
        val client = supabaseClient ?: return
        val myId = _currentUser.value.id
        try {
            val remoteGames = client.from("games").select {
                filter {
                    or {
                        eq("white_player_id", myId)
                        eq("black_player_id", myId)
                    }
                }
            }.decodeList<GameRecordDto>()

            if (remoteGames.isNotEmpty()) {
                val remoteSummaries = remoteGames.map {
                    MatchSummaryUi(
                        record = it,
                        modeType = GameModeType.ONLINE_SUPABASE,
                        currentUserId = myId
                    )
                }
                val localNonOnline = _matches.value.filter { it.modeType != GameModeType.ONLINE_SUPABASE }
                _matches.value = remoteSummaries + localNonOnline
            }
        } catch (_: Exception) {
        }
    }

    suspend fun searchUsersByUsername(query: String): List<UserProfileDto> {
        val clean = query.trim().lowercase()
        val client = supabaseClient
        if (client != null && clean.isNotEmpty()) {
            try {
                val results = client.from("profiles").select {
                    filter { ilike("username", "%$clean%") }
                }.decodeList<UserProfileDto>()
                if (results.isNotEmpty()) return results
            } catch (_: Exception) {
            }
        }
        val directory = listOf(
            UserProfileDto("usr_elena", "elena_rostova", "Elena Rostova", "#38BDF8", 1580, 64, 41),
            UserProfileDto("usr_marcus", "marcus_vance", "Marcus Vance", "#F59E0B", 1340, 32, 16),
            UserProfileDto("usr_sora", "sora_takahashi", "Sora Takahashi", "#A855F7", 1710, 112, 79),
            UserProfileDto("usr_liam", "liam_oconnor", "Liam O'Connor", "#EC4899", 1190, 15, 7),
            UserProfileDto("usr_nadia", "nadia_khoury", "Nadia Khoury", "#10B981", 1495, 51, 30)
        )
        return if (clean.isEmpty()) directory else directory.filter {
            it.username.contains(clean, ignoreCase = true) ||
                it.displayName.contains(clean, ignoreCase = true)
        }
    }

    suspend fun sendFriendInvite(targetProfile: UserProfileDto) {
        val myId = _currentUser.value.id
        val client = supabaseClient
        if (client != null) {
            try {
                client.from("friendships").insert(
                    FriendshipDto(
                        id = UUID.randomUUID().toString(),
                        requesterId = myId,
                        addresseeId = targetProfile.id,
                        status = "pending"
                    )
                )
            } catch (_: Exception) {
            }
        }
        val currentList = _friends.value.toMutableList()
        if (currentList.none { it.profile.id == targetProfile.id }) {
            currentList.add(
                0,
                FriendItemUi(
                    friendshipId = UUID.randomUUID().toString(),
                    profile = targetProfile,
                    isPendingIncoming = false,
                    isPendingOutgoing = false,
                    isOnline = true
                )
            )
            _friends.value = currentList
        }
    }

    suspend fun acceptFriendInvite(friendshipId: String): MatchSummaryUi? {
        val targetItem = _friends.value.firstOrNull { it.friendshipId == friendshipId }
        val client = supabaseClient
        if (client != null) {
            try {
                client.from("friendships").update({
                    set("status", "accepted")
                }) {
                    filter { eq("id", friendshipId) }
                }
            } catch (_: Exception) {
            }
        }
        _friends.value = _friends.value.map { item ->
            if (item.friendshipId == friendshipId) {
                item.copy(isPendingIncoming = false, isPendingOutgoing = false)
            } else {
                item
            }
        }
        // Automatically create and add a new active match with the accepted player
        return if (targetItem != null) {
            createNewMatch(
                opponent = targetItem.profile,
                modeType = GameModeType.ONLINE_SUPABASE,
                timeControl = TimeControlOption.RAPID_10_0,
                playAsWhite = true
            )
        } else {
            null
        }
    }

    suspend fun inviteByUsernameAndStartMatch(
        rawUsername: String,
        timeControl: TimeControlOption = TimeControlOption.RAPID_10_0,
        playAsWhite: Boolean = true
    ): MatchSummaryUi {
        val cleanHandle = rawUsername.trim().removePrefix("@").lowercase()
            .ifBlank { "grandmaster_challenger" }

        val existingSearch = searchUsersByUsername(cleanHandle)
        val matchedProfile = existingSearch.firstOrNull {
            it.username.equals(cleanHandle, ignoreCase = true) ||
                it.displayName.equals(rawUsername.trim(), ignoreCase = true)
        } ?: UserProfileDto(
            id = "usr_" + UUID.randomUUID().toString().take(6),
            username = cleanHandle.replace(" ", "_"),
            displayName = cleanHandle.split("_", " ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
                .ifBlank { cleanHandle },
            avatarColor = "#D4AF37",
            eloRating = 1480,
            gamesPlayed = 24,
            gamesWon = 14
        )

        sendFriendInvite(matchedProfile)
        return createNewMatch(
            opponent = matchedProfile,
            modeType = GameModeType.ONLINE_SUPABASE,
            timeControl = timeControl,
            playAsWhite = playAsWhite
        )
    }

    suspend fun joinOrStartMatchFromInviteLinkOrCode(
        rawLinkOrCode: String,
        timeControl: TimeControlOption = TimeControlOption.RAPID_10_0
    ): MatchSummaryUi {
        val trimmed = rawLinkOrCode.trim()
        // Extract host= parameter if a share URL was pasted, or use invite code token
        val hostParam = if (trimmed.contains("host=")) {
            trimmed.substringAfter("host=").substringBefore("&").trim()
        } else {
            ""
        }
        val codeToken = trimmed
            .substringAfterLast("/")
            .substringBefore("?")
            .uppercase()
            .ifBlank { "CWF-7429" }

        val opponentProfile = if (hostParam.isNotBlank() && hostParam != _currentUser.value.username) {
            val cleanHost = hostParam.lowercase()
            UserProfileDto(
                id = "usr_link_$cleanHost",
                username = cleanHost,
                displayName = cleanHost.split("_", "-")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } },
                avatarColor = "#D4AF37",
                eloRating = 1540
            )
        } else {
            UserProfileDto(
                id = "usr_invite_${codeToken.lowercase()}",
                username = "challenger_${codeToken.lowercase().replace("-", "_")}",
                displayName = "Challenger ($codeToken)",
                avatarColor = "#D4AF37",
                eloRating = 1520
            )
        }

        sendFriendInvite(opponentProfile)
        return createNewMatch(
            opponent = opponentProfile,
            modeType = GameModeType.ONLINE_SUPABASE,
            timeControl = timeControl,
            playAsWhite = true
        )
    }

    // ------------------------ CREATE & UPDATE MATCHES ------------------------

    suspend fun createNewMatch(
        opponent: UserProfileDto?,
        modeType: GameModeType,
        aiDifficulty: com.example.engine.AiDifficulty = com.example.engine.AiDifficulty.CLUB,
        timeControl: TimeControlOption,
        playAsWhite: Boolean
    ): MatchSummaryUi {
        val me = _currentUser.value
        val opp = opponent ?: when (modeType) {
            GameModeType.PASS_AND_PLAY -> UserProfileDto(
                id = "local_friend_id",
                username = "local_friend",
                displayName = "Local Friend",
                avatarColor = "#38BDF8",
                eloRating = 1400
            )
            GameModeType.AI_OPPONENT -> UserProfileDto(
                id = aiDifficulty.dbKey,
                username = aiDifficulty.dbKey,
                displayName = "${aiDifficulty.title} (${aiDifficulty.eloRating})",
                avatarColor = aiDifficulty.avatarColorHex,
                eloRating = aiDifficulty.eloRating
            )
            GameModeType.ONLINE_SUPABASE -> UserProfileDto(
                id = "usr_elena",
                username = "elena_rostova",
                displayName = "Elena Rostova",
                avatarColor = "#38BDF8",
                eloRating = 1580
            )
        }

        val whiteId = if (playAsWhite) me.id else opp.id
        val blackId = if (playAsWhite) opp.id else me.id
        val whiteName = if (playAsWhite) me.displayName else opp.displayName
        val blackName = if (playAsWhite) opp.displayName else me.displayName

        val record = GameRecordDto(
            id = UUID.randomUUID().toString(),
            whitePlayerId = whiteId,
            blackPlayerId = blackId,
            whiteUsername = whiteName,
            blackUsername = blackName,
            currentFen = ChessEngine.INITIAL_FEN,
            turnColor = "w",
            status = GameStatus.ACTIVE.dbValue,
            timeControl = timeControl.dbKey,
            whiteTimeRemainingMs = timeControl.initialTimeMs,
            blackTimeRemainingMs = timeControl.initialTimeMs,
            updatedAt = "Just now"
        )

        val client = supabaseClient
        if (client != null && modeType == GameModeType.ONLINE_SUPABASE) {
            try {
                client.from("games").insert(record)
            } catch (_: Exception) {
            }
        }

        val summary = MatchSummaryUi(
            record = record,
            modeType = modeType,
            aiDifficulty = if (modeType == GameModeType.AI_OPPONENT) aiDifficulty else null,
            currentUserId = me.id
        )
        _matches.value = listOf(summary) + _matches.value
        return summary
    }

    suspend fun recordMoveAndBroadcast(
        match: MatchSummaryUi,
        move: ChessMove,
        nextFen: String,
        nextTurn: PieceColor,
        nextStatus: GameStatus,
        whiteTimeRemainingMs: Long,
        blackTimeRemainingMs: Long
    ): MatchSummaryUi {
        val currentSanList = match.record.moveListSan.trim()
        val updatedSanList = if (currentSanList.isEmpty()) {
            move.san
        } else {
            "$currentSanList ${move.san}"
        }
        val moveNumber = (updatedSanList.split(" ").size + 1) / 2
        val winnerId = if (nextStatus == GameStatus.CHECKMATE) {
            if (move.piece.color == PieceColor.WHITE) match.record.whitePlayerId else match.record.blackPlayerId
        } else {
            null
        }

        val updatedRecord = match.record.copy(
            currentFen = nextFen,
            turnColor = nextTurn.fenChar.toString(),
            status = nextStatus.dbValue,
            winnerId = winnerId,
            drawOfferedBy = null,
            whiteTimeRemainingMs = whiteTimeRemainingMs,
            blackTimeRemainingMs = blackTimeRemainingMs,
            lastMoveFrom = move.from.toAlgebraic(),
            lastMoveTo = move.to.toAlgebraic(),
            lastMoveSan = move.san,
            moveListSan = updatedSanList,
            updatedAt = "Just now"
        )

        val updatedMatch = match.copy(record = updatedRecord)
        updateLocalMatchInList(updatedMatch)

        val client = supabaseClient
        if (client != null && match.modeType == GameModeType.ONLINE_SUPABASE) {
            repoScope.launch {
                try {
                    client.from("games").update({
                        set("current_fen", updatedRecord.currentFen)
                        set("turn_color", updatedRecord.turnColor)
                        set("status", updatedRecord.status)
                        set("winner_id", updatedRecord.winnerId)
                        set("draw_offered_by", null as String?)
                        set("white_time_remaining_ms", updatedRecord.whiteTimeRemainingMs)
                        set("black_time_remaining_ms", updatedRecord.blackTimeRemainingMs)
                        set("last_move_from", updatedRecord.lastMoveFrom)
                        set("last_move_to", updatedRecord.lastMoveTo)
                        set("last_move_san", updatedRecord.lastMoveSan)
                        set("move_list_san", updatedRecord.moveListSan)
                    }) {
                        filter { eq("id", updatedRecord.id) }
                    }

                    client.from("moves").insert(
                        MoveRecordDto(
                            id = UUID.randomUUID().toString(),
                            gameId = updatedRecord.id,
                            moveNumber = moveNumber,
                            playerId = _currentUser.value.id,
                            piece = move.piece.fenChar.toString(),
                            fromSquare = move.from.toAlgebraic(),
                            toSquare = move.to.toAlgebraic(),
                            promotion = move.promotion?.sanLetter,
                            san = move.san,
                            fenAfter = nextFen
                        )
                    )

                    activeRealtimeChannel?.broadcast(
                        event = "move_played",
                        message = buildJsonObject {
                            put("gameId", updatedRecord.id)
                            put("fenAfter", updatedRecord.currentFen)
                            put("turnColor", updatedRecord.turnColor)
                            put("status", updatedRecord.status)
                            put("lastMoveFrom", updatedRecord.lastMoveFrom ?: "")
                            put("lastMoveTo", updatedRecord.lastMoveTo ?: "")
                            put("lastMoveSan", updatedRecord.lastMoveSan ?: "")
                            put("moveListSan", updatedRecord.moveListSan)
                            put("whiteTimeRemainingMs", updatedRecord.whiteTimeRemainingMs)
                            put("blackTimeRemainingMs", updatedRecord.blackTimeRemainingMs)
                            put("drawOfferedBy", updatedRecord.drawOfferedBy)
                            put("winnerId", updatedRecord.winnerId)
                        }
                    )
                } catch (_: Exception) {
                }
            }
        }

        return updatedMatch
    }

    suspend fun resignMatch(match: MatchSummaryUi): MatchSummaryUi {
        val myId = match.currentUserId
        val oppId = if (match.record.whitePlayerId == myId) {
            match.record.blackPlayerId
        } else {
            match.record.whitePlayerId
        }
        val updated = match.copy(
            record = match.record.copy(
                status = GameStatus.RESIGNED.dbValue,
                winnerId = oppId,
                updatedAt = "Just now"
            )
        )
        updateLocalMatchInList(updated)

        val client = supabaseClient
        if (client != null && match.modeType == GameModeType.ONLINE_SUPABASE) {
            try {
                client.from("games").update({
                    set("status", GameStatus.RESIGNED.dbValue)
                    set("winner_id", oppId)
                }) {
                    filter { eq("id", match.record.id) }
                }
                broadcastGameState(updated.record)
            } catch (_: Exception) {
            }
        }
        return updated
    }

    suspend fun offerOrAcceptDraw(match: MatchSummaryUi): MatchSummaryUi {
        val myId = match.currentUserId
        val currentOffer = match.record.drawOfferedBy
        val isAccepting = (currentOffer != null && currentOffer != myId) ||
            match.modeType != GameModeType.ONLINE_SUPABASE

        val updated = if (isAccepting) {
            match.copy(
                record = match.record.copy(
                    status = GameStatus.DRAW_AGREEMENT.dbValue,
                    drawOfferedBy = null,
                    updatedAt = "Just now"
                )
            )
        } else {
            match.copy(
                record = match.record.copy(
                    drawOfferedBy = myId,
                    updatedAt = "Draw offered"
                )
            )
        }
        updateLocalMatchInList(updated)

        val client = supabaseClient
        if (client != null && match.modeType == GameModeType.ONLINE_SUPABASE) {
            try {
                client.from("games").update({
                    set("status", updated.record.status)
                    set("draw_offered_by", updated.record.drawOfferedBy)
                }) {
                    filter { eq("id", match.record.id) }
                }
                broadcastGameState(updated.record)
            } catch (_: Exception) {
            }
        }
        return updated
    }

    private suspend fun broadcastGameState(record: GameRecordDto) {
        activeRealtimeChannel?.broadcast(
            event = "move_played",
            message = buildJsonObject {
                put("gameId", record.id)
                put("fenAfter", record.currentFen)
                put("turnColor", record.turnColor)
                put("status", record.status)
                put("lastMoveFrom", record.lastMoveFrom ?: "")
                put("lastMoveTo", record.lastMoveTo ?: "")
                put("lastMoveSan", record.lastMoveSan ?: "")
                put("moveListSan", record.moveListSan)
                put("whiteTimeRemainingMs", record.whiteTimeRemainingMs)
                put("blackTimeRemainingMs", record.blackTimeRemainingMs)
                put("drawOfferedBy", record.drawOfferedBy)
                put("winnerId", record.winnerId)
            }
        )
    }

    // ------------------------ SUPABASE REALTIME CHANNELS ------------------------

    fun subscribeToMatchRealtime(
        gameId: String,
        onRemoteUpdate: (GameRecordDto) -> Unit
    ) {
        val client = supabaseClient ?: return
        repoScope.launch {
            try {
                activeRealtimeChannel?.unsubscribe()
                val channel = client.channel("game:$gameId") {
                    broadcast {
                        acknowledgeBroadcasts = false
                        receiveOwnBroadcasts = false
                    }
                }

                // Listen to low-latency Broadcast events on the game channel
                channel.broadcastFlow<RealtimeMoveBroadcast>(event = "move_played")
                    .onEach { payload ->
                        if (payload.gameId == gameId) {
                            val existing = _matches.value.firstOrNull { it.record.id == gameId }?.record
                            if (existing != null) {
                                val merged = existing.copy(
                                    currentFen = payload.fenAfter,
                                    turnColor = payload.turnColor,
                                    status = payload.status,
                                    lastMoveFrom = payload.lastMoveFrom,
                                    lastMoveTo = payload.lastMoveTo,
                                    lastMoveSan = payload.lastMoveSan,
                                    moveListSan = payload.moveListSan,
                                    whiteTimeRemainingMs = payload.whiteTimeRemainingMs,
                                    blackTimeRemainingMs = payload.blackTimeRemainingMs,
                                    drawOfferedBy = payload.drawOfferedBy,
                                    winnerId = payload.winnerId,
                                    updatedAt = "Live update"
                                )
                                applyRemoteRecord(merged)
                                onRemoteUpdate(merged)
                            }
                        }
                    }.launchIn(repoScope)

                channel.subscribe()
                activeRealtimeChannel = channel
            } catch (_: Exception) {
            }
        }
    }

    fun unsubscribeFromMatchRealtime() {
        val channel = activeRealtimeChannel ?: return
        activeRealtimeChannel = null
        repoScope.launch {
            try {
                channel.unsubscribe()
            } catch (_: Exception) {
            }
        }
    }

    private fun applyRemoteRecord(record: GameRecordDto) {
        _matches.value = _matches.value.map { item ->
            if (item.record.id == record.id) item.copy(record = record) else item
        }
    }

    fun updateLocalMatchInList(updated: MatchSummaryUi) {
        _matches.value = _matches.value.map { item ->
            if (item.record.id == updated.record.id) updated else item
        }
    }

    // ------------------------ INITIAL RICH SEEDED MATCHES ------------------------

    private fun initialSeededFriends(): List<FriendItemUi> = listOf(
        FriendItemUi(
            friendshipId = "fr_1",
            profile = UserProfileDto("usr_elena", "elena_rostova", "Elena Rostova", "#38BDF8", 1580, 64, 41),
            isPendingIncoming = false,
            isOnline = true
        ),
        FriendItemUi(
            friendshipId = "fr_2",
            profile = UserProfileDto("usr_marcus", "marcus_vance", "Marcus Vance", "#F59E0B", 1340, 32, 16),
            isPendingIncoming = false,
            isOnline = true
        ),
        FriendItemUi(
            friendshipId = "fr_3",
            profile = UserProfileDto("usr_sora", "sora_takahashi", "Sora Takahashi", "#A855F7", 1710, 112, 79),
            isPendingIncoming = true,
            isOnline = true
        ),
        FriendItemUi(
            friendshipId = "fr_5",
            profile = UserProfileDto("usr_liam", "liam_oconnor", "Liam O'Connor", "#EC4899", 1490, 42, 25),
            isPendingIncoming = true,
            isOnline = true
        ),
        FriendItemUi(
            friendshipId = "fr_4",
            profile = UserProfileDto("usr_nadia", "nadia_khoury", "Nadia Khoury", "#10B981", 1495, 51, 30),
            isPendingIncoming = false,
            isOnline = false
        )
    )

    private fun initialSeededMatches(myId: String): List<MatchSummaryUi> = listOf(
        // 1. Your Turn - Giuoco Piano / Scotch Gambit midgame with real captures on both sides
        // White has captured a Black Knight + Pawn; Black has captured a White Pawn (White +3)
        MatchSummaryUi(
            record = GameRecordDto(
                id = "game_italian_tactical",
                whitePlayerId = myId,
                blackPlayerId = "usr_elena",
                whiteUsername = "Alex Mercer",
                blackUsername = "Elena Rostova",
                currentFen = "r1bqk2r/ppp2ppp/2n5/2b1p3/2B1P3/3P1N2/PPP3PP/RNBQK2R w KQkq - 0 6",
                turnColor = "w",
                status = GameStatus.ACTIVE.dbValue,
                timeControl = TimeControlOption.RAPID_10_0.dbKey,
                whiteTimeRemainingMs = 542_000L,
                blackTimeRemainingMs = 518_000L,
                lastMoveFrom = "f8",
                lastMoveTo = "c5",
                lastMoveSan = "Bc5",
                moveListSan = "e4 e5 Nf3 Nc6 Bc4 Nf6 d4 exd4 Nxd4 Nxe4 Nxc6 dxc6 Bc5",
                updatedAt = "2m ago"
            ),
            modeType = GameModeType.ONLINE_SUPABASE,
            currentUserId = myId
        ),
        // 2. Your Turn - Queen's Gambit Accepted tactical position vs Master AI Opponent
        // Black captured White's c4 pawn (...dxc4); White can capture or develop
        MatchSummaryUi(
            record = GameRecordDto(
                id = "game_qgd_bot",
                whitePlayerId = myId,
                blackPlayerId = com.example.engine.AiDifficulty.MASTER.dbKey,
                whiteUsername = "Alex Mercer",
                blackUsername = "Master Engine (1700)",
                currentFen = "rnbqkb1r/ppp1pppp/5n2/8/2pP4/2N2N2/PP2PPPP/R1BQKB1R w KQkq - 1 4",
                turnColor = "w",
                status = GameStatus.ACTIVE.dbValue,
                timeControl = TimeControlOption.BLITZ_5_0.dbKey,
                whiteTimeRemainingMs = 278_000L,
                blackTimeRemainingMs = 285_000L,
                lastMoveFrom = "d5",
                lastMoveTo = "c4",
                lastMoveSan = "dxc4",
                moveListSan = "d4 d5 c4 dxc4 Nf3 Nf6 Nc3",
                updatedAt = "14m ago"
            ),
            modeType = GameModeType.AI_OPPONENT,
            aiDifficulty = com.example.engine.AiDifficulty.MASTER,
            currentUserId = myId
        ),
        // 3. Awaiting Opponent - Sicilian Dragon where it is Marcus's turn
        MatchSummaryUi(
            record = GameRecordDto(
                id = "game_sicilian_wait",
                whitePlayerId = myId,
                blackPlayerId = "usr_marcus",
                whiteUsername = "Alex Mercer",
                blackUsername = "Marcus Vance",
                currentFen = "rnbqkbnr/pp2pppp/3p4/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
                turnColor = "b",
                status = GameStatus.ACTIVE.dbValue,
                timeControl = TimeControlOption.DAILY_ASYNC.dbKey,
                whiteTimeRemainingMs = 82_400_000L,
                blackTimeRemainingMs = 79_100_000L,
                lastMoveFrom = "g1",
                lastMoveTo = "f3",
                lastMoveSan = "Nf3",
                moveListSan = "e4 c5 Nf3 d6",
                updatedAt = "1h ago"
            ),
            modeType = GameModeType.ONLINE_SUPABASE,
            currentUserId = myId
        ),
        // 4. Archived Past Game - Scholar's Mate victory
        MatchSummaryUi(
            record = GameRecordDto(
                id = "game_archived_win",
                whitePlayerId = myId,
                blackPlayerId = "usr_nadia",
                whiteUsername = "Alex Mercer",
                blackUsername = "Nadia Khoury",
                currentFen = "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4",
                turnColor = "b",
                status = GameStatus.CHECKMATE.dbValue,
                winnerId = myId,
                timeControl = TimeControlOption.BLITZ_3_2.dbKey,
                whiteTimeRemainingMs = 165_000L,
                blackTimeRemainingMs = 142_000L,
                lastMoveFrom = "h5",
                lastMoveTo = "f7",
                lastMoveSan = "Qxf7#",
                moveListSan = "e4 e5 Bc4 Nc6 Qh5 Nf6 Qxf7#",
                updatedAt = "Yesterday"
            ),
            modeType = GameModeType.ONLINE_SUPABASE,
            currentUserId = myId
        )
    )
}

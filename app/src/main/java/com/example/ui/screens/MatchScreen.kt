package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameModeType
import com.example.data.TimeControlOption
import com.example.engine.AiDifficulty
import com.example.engine.BoardSquare
import com.example.engine.ChessEngine
import com.example.engine.ChessPiece
import com.example.engine.GameStatus
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.ui.ChessUiState
import com.example.ui.components.CapturedPiecesTray
import com.example.ui.components.ChessBoardCanvas
import com.example.ui.components.DualPlayerCapturedPiecesPanel
import com.example.ui.components.GrandmasterTopAppBar
import com.example.ui.components.PlatinumActionButton
import com.example.ui.theme.GoldAccentRestrained
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlatinumDark
import com.example.ui.theme.PlatinumLight
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StateActiveTurn
import com.example.ui.theme.StateDanger
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGround
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmBronzeText
import com.example.ui.theme.WoodGrainDark
import com.example.ui.theme.WoodGrainMid
import com.example.ui.theme.WoodPerimeterBorder
import java.util.Locale

@Composable
fun MatchScreen(
    uiState: ChessUiState,
    onBackToDashboard: () -> Unit,
    onToggleBeginnerMode: () -> Unit,
    onToggleFlipBoard: () -> Unit,
    onSquareTapped: (BoardSquare) -> Unit,
    onPieceDragged: (BoardSquare, BoardSquare) -> Unit,
    onSelectPromotionPiece: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onChangeAiDifficulty: (AiDifficulty) -> Unit,
    onRequestHint: () -> Unit,
    onSimulateOpponentReply: () -> Unit,
    onOfferOrAcceptDraw: () -> Unit,
    onResignMatch: () -> Unit,
    onRematch: () -> Unit,
    onOpenFriendDrawer: () -> Unit = {},
    onOpenAuthSheet: () -> Unit = {},
    onOpenClockSettingsSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToDashboard()
    }

    val match = uiState.activeMatch ?: return
    val board = uiState.boardState
    val clipboard = LocalClipboardManager.current
    var showFenDialog by remember { mutableStateOf(false) }
    var showMatchDetailsPanel by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "turn_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val topColor = if (uiState.isBoardFlipped) PieceColor.WHITE else PieceColor.BLACK
    val bottomColor = topColor.opposite

    val topPlayerName = if (topColor == PieceColor.WHITE) match.record.whiteUsername else match.record.blackUsername
    val bottomPlayerName = if (bottomColor == PieceColor.WHITE) match.record.whiteUsername else match.record.blackUsername

    val topClockMs = if (topColor == PieceColor.WHITE) uiState.whiteClockMs else uiState.blackClockMs
    val bottomClockMs = if (bottomColor == PieceColor.WHITE) uiState.whiteClockMs else uiState.blackClockMs

    val topCaptured = if (topColor == PieceColor.WHITE) board.capturedByWhite else board.capturedByBlack
    val bottomCaptured = if (bottomColor == PieceColor.WHITE) board.capturedByWhite else board.capturedByBlack

    val topMaterialAdv = if (topColor == PieceColor.WHITE) {
        board.materialAdvantageWhite.coerceAtLeast(0)
    } else {
        (-board.materialAdvantageWhite).coerceAtLeast(0)
    }
    val bottomMaterialAdv = if (bottomColor == PieceColor.WHITE) {
        board.materialAdvantageWhite.coerceAtLeast(0)
    } else {
        (-board.materialAdvantageWhite).coerceAtLeast(0)
    }

    val evalFormatted = String.format(
        Locale.US,
        "%.1f",
        uiState.lastAiEvalCentipawns / 100.0
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SurfaceGround,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Persistent Shell Header (Crown Back Button + CHESS WITH FRIENDS + Settings + Bell & Avatar)
                GrandmasterTopAppBar(
                    isMatchScreen = true,
                    hasPendingInvites = uiState.friends.any { it.isPendingIncoming },
                    onCrownOrBackClick = onBackToDashboard,
                    onSettingsClick = onOpenClockSettingsSheet,
                    onBellClick = onOpenFriendDrawer,
                    onAvatarClick = onOpenAuthSheet
                )

                val isMatchClockEnabled = uiState.isClockEnabled &&
                    match.timeControlOption != TimeControlOption.UNLIMITED

                // 2. Top Opponent Horology Card (Matches Screenshot 2)
                TopOpponentHorologyCard(
                    playerName = topPlayerName,
                    titleBadge = if (match.modeType == GameModeType.AI_OPPONENT) {
                        "AI"
                    } else {
                        "WGM"
                    },
                    eloRating = if (match.modeType == GameModeType.AI_OPPONENT) {
                        (match.aiDifficulty ?: uiState.selectedAiDifficulty).eloRating
                    } else {
                        1465
                    },
                    pieceColor = topColor,
                    clockMs = topClockMs,
                    isLiveClock = match.timeControlOption.isLiveClock || uiState.useCustomClockDuration,
                    isClockEnabled = isMatchClockEnabled,
                    onClockClick = onOpenClockSettingsSheet,
                    capturedPieces = topCaptured,
                    materialAdvantage = topMaterialAdv,
                    evalString = evalFormatted,
                    isAiBot = match.modeType == GameModeType.AI_OPPONENT && topColor != match.playerColor,
                    isBotThinking = uiState.isBotThinking && topColor != match.playerColor
                )

                // 3. Tactical Assist Engine (Beginner Mode) Toggle Bar (Matches Screenshot 2)
                TacticalAssistEngineBar(
                    isBeginnerModeEnabled = uiState.isBeginnerModeEnabled,
                    isAiMatch = match.modeType == GameModeType.AI_OPPONENT,
                    activeAiDifficulty = match.aiDifficulty ?: uiState.selectedAiDifficulty,
                    onToggleBeginnerMode = onToggleBeginnerMode,
                    onCycleAiDifficulty = {
                        val entries = AiDifficulty.entries
                        val current = match.aiDifficulty ?: uiState.selectedAiDifficulty
                        val next = entries[(entries.indexOf(current) + 1) % entries.size]
                        onChangeAiDifficulty(next)
                    }
                )

                // 4. Optional Game Status / Checkmate / Check Alert Strip
                val statusBanner = uiState.statusBannerMessage ?: when {
                    board.status == GameStatus.CHECKMATE ->
                        "Checkmate — ${board.turn.opposite.displayName} claims victory."
                    board.status == GameStatus.STALEMATE ->
                        "Stalemate — Match drawn."
                    board.status == GameStatus.RESIGNED ->
                        "Match concluded by resignation."
                    board.status == GameStatus.DRAW_AGREEMENT ->
                        "Draw agreed by both players."
                    board.status == GameStatus.TIMEOUT ->
                        "Flagged on time — Match concluded."
                    board.isCheck ->
                        "${board.turn.displayName} King is in Check!"
                    else -> null
                }

                AnimatedVisibility(visible = statusBanner != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (board.isCheck || board.status == GameStatus.CHECKMATE) {
                                    WoodGrainMid
                                } else {
                                    SurfaceElevated
                                }
                            )
                            .border(
                                1.dp,
                                if (board.isCheck || board.status == GameStatus.CHECKMATE) {
                                    GoldAccentRestrained.copy(alpha = 0.6f)
                                } else {
                                    SurfaceBorder
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner ?: "",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (board.status.isTerminal || match.gameStatus.isTerminal) {
                            Text(
                                text = "REMATCH →",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GoldAccentRestrained,
                                modifier = Modifier
                                    .clickable { onRematch() }
                                    .padding(start = 10.dp)
                                    .testTag("btn_rematch")
                            )
                        }
                    }
                }

                // 5. Heirloom 2D Jetpack Compose Canvas Chessboard
                ChessBoardCanvas(
                    boardState = board,
                    selectedSquare = uiState.selectedSquare,
                    legalMovesForSelected = uiState.legalMovesForSelected,
                    beginnerRoutes = uiState.beginnerRoutes,
                    isBeginnerModeEnabled = uiState.isBeginnerModeEnabled,
                    isFlipped = uiState.isBoardFlipped,
                    hintMove = uiState.hintMove,
                    onSquareTapped = onSquareTapped,
                    onPieceDragged = onPieceDragged
                )

                // 6. Bottom Player Horology Card (Alex Mercer (You) | 08:57 | YOUR TURN)
                BottomPlayerHorologyCard(
                    playerName = bottomPlayerName,
                    isLocalUser = bottomColor == match.playerColor,
                    clockMs = bottomClockMs,
                    isLiveClock = match.timeControlOption.isLiveClock || uiState.useCustomClockDuration,
                    isClockEnabled = isMatchClockEnabled,
                    onClockClick = onOpenClockSettingsSheet,
                    capturedPieces = bottomCaptured,
                    materialAdvantage = bottomMaterialAdv,
                    isMyTurn = !board.status.isTerminal && board.turn == bottomColor,
                    pulseAlpha = pulseAlpha
                )

                // 7. Slim Low-Profile Tactical Action Strip
                DarkTacticalControlsDock(
                    isTerminal = board.status.isTerminal || match.gameStatus.isTerminal,
                    isOpponentTurnInOnline = match.modeType == GameModeType.ONLINE_SUPABASE &&
                        board.turn != match.playerColor,
                    isDetailsExpanded = showMatchDetailsPanel,
                    moveCount = (match.sanHistoryList.size + 1) / 2,
                    onFlipBoard = onToggleFlipBoard,
                    onRequestHint = onRequestHint,
                    onToggleMatchDetails = { showMatchDetailsPanel = !showMatchDetailsPanel },
                    onSimulateOpponentReply = onSimulateOpponentReply,
                    onOfferDraw = onOfferOrAcceptDraw,
                    onResign = onResignMatch,
                    onRematch = onRematch
                )

                // 8. Collapsible Match Log & Material Comparison Drawer (hidden by default to keep board UI clean)
                AnimatedVisibility(visible = showMatchDetailsPanel) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GrandmasterSanMoveTape(
                            sanMoves = match.sanHistoryList,
                            onShowFen = { showFenDialog = true }
                        )
                        DualPlayerCapturedPiecesPanel(
                            boardState = board,
                            whitePlayerName = match.record.whiteUsername,
                            blackPlayerName = match.record.blackUsername
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }

    // Pawn Promotion Modal
    val pendingPromo = uiState.pendingPromotion
    if (pendingPromo != null) {
        PawnPromotionDialog(
            color = pendingPromo.color,
            onSelectPiece = onSelectPromotionPiece,
            onDismiss = onDismissPromotion
        )
    }

    // FEN Inspector Dialog
    if (showFenDialog) {
        val currentFen = ChessEngine.toFen(board)
        AlertDialog(
            onDismissRequest = { showFenDialog = false },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "FEN & Board State Inspector",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = SurfaceGround,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = currentFen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = "Castling Rights: ${board.castlingRights.toFen()} • En Passant: ${board.enPassantTarget?.toAlgebraic() ?: "-"}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                PlatinumActionButton(
                    text = "Copy FEN",
                    onClick = {
                        clipboard.setText(AnnotatedString(currentFen))
                        showFenDialog = false
                    },
                    trailingArrow = false
                )
            },
            dismissButton = {
                TextButton(onClick = { showFenDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Top Opponent Card from Screenshot 2:
 * `[ Avatar | [WGM] Elena Rostova / ELO 1465 • Playing Black | [ ⏳ 08:38 ] ]`
 * `[ Captured: ♟ ♟ ♞ | [+1] Eval: 0.0 ]`
 */
@Composable
private fun TopOpponentHorologyCard(
    playerName: String,
    titleBadge: String,
    eloRating: Int,
    pieceColor: PieceColor,
    clockMs: Long,
    isLiveClock: Boolean,
    isClockEnabled: Boolean,
    onClockClick: () -> Unit,
    capturedPieces: List<ChessPiece>,
    materialAdvantage: Int,
    evalString: String,
    isAiBot: Boolean,
    isBotThinking: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with gold status dot
                Box(modifier = Modifier.size(46.dp)) {
                    if (isAiBot) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceGround)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Opponent",
                                tint = GoldAccentRestrained,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_chess_hero_1793508283788),
                            contentDescription = playerName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GoldAccentRestrained)
                            .border(1.5.dp, SurfaceCard, CircleShape)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Gold Title Pill (WGM / AI)
                        Surface(
                            color = Color(0xFF282012),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.border(
                                0.5.dp,
                                GoldAccentRestrained.copy(alpha = 0.5f),
                                RoundedCornerShape(5.dp)
                            )
                        ) {
                            Text(
                                text = titleBadge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = GoldAccentRestrained,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = playerName.substringBefore(" ("),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isBotThinking) {
                            "Engine computing response..."
                        } else {
                            "ELO $eloRating • Playing ${pieceColor.displayName}"
                        },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (isBotThinking) GoldAccentRestrained else TextSecondary
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Digital Horology Pill Box: [ ⏳ 08:38 ] (Tap to configure clock settings)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceGround)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { onClockClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("top_player_clock_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Configure Time Clock",
                    tint = if (isClockEnabled) WarmBronzeText else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        !isClockEnabled -> "OFF"
                        isLiveClock -> formatClockMillis(clockMs)
                        else -> "24:00"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.8.sp,
                    color = if (isClockEnabled) TextPrimary else TextSecondary
                )
            }
        }

        // Dynamic Captured Pieces Tray & Eval Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapturedPiecesTray(
                capturedPieces = capturedPieces,
                materialAdvantage = materialAdvantage,
                testTagPrefix = "top_player_captured_tray",
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Eval: $evalString",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Tactical Assist Engine Bar from Screenshot 2:
 * `[ • Tactical Assist Engine  [Deep Thought v4.2]            (•) ]`
 */
@Composable
private fun TacticalAssistEngineBar(
    isBeginnerModeEnabled: Boolean,
    isAiMatch: Boolean,
    activeAiDifficulty: AiDifficulty,
    onToggleBeginnerMode: () -> Unit,
    onCycleAiDifficulty: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onToggleBeginnerMode() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("beginner_mode_toggle"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (isBeginnerModeEnabled) StateActiveTurn else TextMuted)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Tactical Assist Engine",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .border(0.5.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        if (isAiMatch) onCycleAiDifficulty() else onToggleBeginnerMode()
                    }
                    .testTag("ai_diff_chip_${activeAiDifficulty.dbKey}")
            ) {
                Text(
                    text = if (isAiMatch) {
                        "${activeAiDifficulty.title} (${activeAiDifficulty.eloRating})"
                    } else {
                        "Deep Thought v4.2"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    color = WarmBronzeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Custom Machined Platinum Pill Switch matching Screenshot 2
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isBeginnerModeEnabled) Color(0xFF282B34) else SurfaceGround)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(999.dp))
                .padding(3.dp),
            contentAlignment = if (isBeginnerModeEnabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isBeginnerModeEnabled) {
                                listOf(PlatinumLight, PlatinumDark)
                            } else {
                                listOf(TextMuted, SurfaceElevated)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(SurfaceGround)
                )
            }
        }
    }
}

/**
 * Bottom Player Horology Card from Screenshot 2:
 * Top progress dots: `• • • • • • • • •`
 * Middle: `[ Avatar | Alex Mercer (You) | [ ⏱ 08:57 ] ]`
 * Bottom: `[ Captured: ♟ ♟ ♝ | • YOUR TURN ]`
 */
@Composable
private fun BottomPlayerHorologyCard(
    playerName: String,
    isLocalUser: Boolean,
    clockMs: Long,
    isLiveClock: Boolean,
    isClockEnabled: Boolean,
    onClockClick: () -> Unit,
    capturedPieces: List<ChessPiece>,
    materialAdvantage: Int,
    isMyTurn: Boolean,
    pulseAlpha: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Player Row & Digital Clock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_chess_hero_1793508283788),
                    contentDescription = playerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = playerName.substringBefore(" ("),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isLocalUser) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "(You)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = GoldAccentRestrained,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }

            // Digital Clock Box: [ ⏱ 08:57 ] (Tap to configure clock settings)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { onClockClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("bottom_player_clock_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Configure Time Clock",
                    tint = if (isClockEnabled) TextSecondary else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        !isClockEnabled -> "OFF"
                        isLiveClock -> formatClockMillis(clockMs)
                        else -> "24:00"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.8.sp,
                    color = if (isClockEnabled) TextPrimary else TextSecondary
                )
            }
        }

        // Dynamic Captured Pieces Tray & YOUR TURN Beacon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapturedPiecesTray(
                capturedPieces = capturedPieces,
                materialAdvantage = materialAdvantage,
                testTagPrefix = "bottom_player_captured_tray",
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMyTurn) StateActiveTurn.copy(alpha = pulseAlpha) else TextMuted
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isMyTurn) "YOUR TURN" else "AWAITING TURN",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = if (isMyTurn) TextPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GrandmasterSanMoveTape(
    sanMoves: List<String>,
    onShowFen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PGN:",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = WarmBronzeText
            )
            if (sanMoves.isEmpty()) {
                Text(
                    text = "1. — (Tap or drag any piece to begin)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            } else {
                sanMoves.chunked(2).forEachIndexed { idx, pair ->
                    val w = pair.getOrNull(0) ?: ""
                    val b = pair.getOrNull(1) ?: ""
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${idx + 1}. $w $b".trim(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .border(0.5.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                .clickable { onShowFen() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "FEN",
                    tint = GoldAccentRestrained,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "FEN",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * Sleek, low-profile tactical action strip below the bottom player card.
 */
@Composable
private fun DarkTacticalControlsDock(
    isTerminal: Boolean,
    isOpponentTurnInOnline: Boolean,
    isDetailsExpanded: Boolean,
    moveCount: Int,
    onFlipBoard: () -> Unit,
    onRequestHint: () -> Unit,
    onToggleMatchDetails: () -> Unit,
    onSimulateOpponentReply: () -> Unit,
    onOfferDraw: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TacticalControlTile(
            icon = Icons.Default.SwapVert,
            caption = "Flip",
            onClick = onFlipBoard,
            testTag = "btn_flip_board",
            modifier = Modifier.weight(1f)
        )

        TacticalControlTile(
            icon = Icons.Default.History,
            caption = if (moveCount > 0) "Moves ($moveCount)" else "Moves",
            onClick = onToggleMatchDetails,
            testTag = "btn_toggle_match_details",
            accentTint = if (isDetailsExpanded) GoldAccentRestrained else TextSecondary,
            isHighlighted = isDetailsExpanded,
            modifier = Modifier.weight(1.15f)
        )

        if (!isTerminal) {
            TacticalControlTile(
                icon = Icons.Default.Lightbulb,
                caption = "Hint",
                onClick = onRequestHint,
                testTag = "btn_ai_hint",
                accentTint = GoldAccentRestrained,
                modifier = Modifier.weight(1f)
            )

            if (isOpponentTurnInOnline) {
                TacticalControlTile(
                    icon = Icons.Default.Bolt,
                    caption = "Reply",
                    onClick = onSimulateOpponentReply,
                    testTag = "btn_simulate_reply",
                    accentTint = GoldAccentRestrained,
                    modifier = Modifier.weight(1f)
                )
            }

            TacticalControlTile(
                icon = Icons.Default.Handshake,
                caption = "Draw",
                onClick = onOfferDraw,
                testTag = "btn_offer_draw",
                modifier = Modifier.weight(1f)
            )

            TacticalControlTile(
                icon = Icons.Default.Flag,
                caption = "Resign",
                onClick = onResign,
                testTag = "btn_resign",
                accentTint = StateDanger,
                modifier = Modifier.weight(1f)
            )
        } else {
            PlatinumActionButton(
                text = "New Rematch",
                onClick = onRematch,
                modifier = Modifier
                    .weight(2.5f)
                    .testTag("btn_rematch_bottom")
            )
        }
    }
}

@Composable
private fun TacticalControlTile(
    icon: ImageVector,
    caption: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    accentTint: Color = TextSecondary,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHighlighted) SurfaceElevated else SurfaceCard.copy(alpha = 0.75f))
            .border(
                0.75.dp,
                if (isHighlighted) GoldAccentRestrained.copy(alpha = 0.6f) else SurfaceBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = caption,
            tint = accentTint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = caption,
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = if (isHighlighted) TextPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PawnPromotionDialog(
    color: PieceColor,
    onSelectPiece: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        PieceType.QUEEN to "Queen",
        PieceType.ROOK to "Rook",
        PieceType.BISHOP to "Bishop",
        PieceType.KNIGHT to "Knight"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        title = {
            Text(
                text = "Promote Pawn",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose your promotion piece:",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((type, label) in options) {
                        val symbol = if (color == PieceColor.WHITE) {
                            type.unicodeSymbolWhite
                        } else {
                            type.unicodeSymbolBlack
                        }
                        Surface(
                            color = SurfaceGround,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, WoodPerimeterBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectPiece(type) }
                                .padding(vertical = 12.dp)
                                .testTag("promo_option_${type.sanLetter}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = symbol,
                                    fontSize = 26.sp,
                                    color = GoldAccentRestrained
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = label.uppercase(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun formatClockMillis(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

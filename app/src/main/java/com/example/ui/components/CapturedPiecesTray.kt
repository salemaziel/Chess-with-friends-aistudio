package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ChessBoardState
import com.example.engine.ChessPiece
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.ui.theme.GoldAccentRestrained
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlatinumDark
import com.example.ui.theme.PlatinumLight
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StateInteractiveText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGround
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmBronzeText
import kotlin.math.abs

/**
 * Grouped representation of captured pieces of a single [PieceType] (e.g., 3 captured Pawns).
 */
data class CapturedPieceGroup(
    val type: PieceType,
    val color: PieceColor,
    val count: Int
) {
    val totalValue: Int
        get() = type.materialValue * count
}

/**
 * Groups a raw list of captured pieces by [PieceType] in descending material order
 * (Queen -> Rook -> Bishop -> Knight -> Pawn).
 */
fun groupCapturedPieces(pieces: List<ChessPiece>): List<CapturedPieceGroup> {
    if (pieces.isEmpty()) return emptyList()
    val order = listOf(
        PieceType.QUEEN,
        PieceType.ROOK,
        PieceType.BISHOP,
        PieceType.KNIGHT,
        PieceType.PAWN
    )
    val firstColor = pieces.first().color
    return order.mapNotNull { type ->
        val matching = pieces.count { it.type == type }
        if (matching > 0) {
            CapturedPieceGroup(type = type, color = firstColor, count = matching)
        } else {
            null
        }
    }
}

/**
 * 1. INLINE CAPTURED PIECES TRAY
 * Dynamically renders the captured pieces for a single player inside their Horology Card,
 * using custom 2D Compose Canvas mini-Staunton silhouettes grouped by piece type,
 * plus an animated net material advantage badge (+N).
 */
@Composable
fun CapturedPiecesTray(
    capturedPieces: List<ChessPiece>,
    materialAdvantage: Int,
    testTagPrefix: String = "captured_tray",
    modifier: Modifier = Modifier
) {
    val groups = remember(capturedPieces) { groupCapturedPieces(capturedPieces) }
    val totalCapturedPoints = remember(capturedPieces) {
        capturedPieces.sumOf { it.type.materialValue }
    }
    val accessibilitySummary = remember(groups, materialAdvantage) {
        if (groups.isEmpty()) {
            "No pieces captured yet"
        } else {
            val detail = groups.joinToString(", ") { "${it.count} ${it.type.name.lowercase()}" }
            val adv = if (materialAdvantage > 0) ", +$materialAdvantage material advantage" else ""
            "Captured: $detail$adv"
        }
    }

    Row(
        modifier = modifier
            .animateContentSize()
            .semantics { contentDescription = accessibilitySummary }
            .testTag(testTagPrefix),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Captured:",
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )

        AnimatedContent(
            targetState = groups,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(140))
            },
            label = "captured_groups_anim"
        ) { currentGroups ->
            if (currentGroups.isEmpty()) {
                Text(
                    text = "—",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    modifier = Modifier.testTag("${testTagPrefix}_empty")
                )
            } else {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .testTag("${testTagPrefix}_list"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (group in currentGroups) {
                        CapturedPieceClusterChip(group = group)
                    }
                }
            }
        }

        if (materialAdvantage > 0) {
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .border(0.5.dp, GoldAccentRestrained.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .testTag("${testTagPrefix}_advantage_badge")
            ) {
                Text(
                    text = "+$materialAdvantage",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = GoldAccentRestrained,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Renders a compact cluster of overlapping mini-Staunton vector pieces for a single [CapturedPieceGroup]
 * (e.g., up to 3 overlapping mini silhouettes + a `×N` count label when multiple pieces of that type are captured).
 */
@Composable
fun CapturedPieceClusterChip(
    group: CapturedPieceGroup,
    modifier: Modifier = Modifier
) {
    val visibleStackCount = group.count.coerceAtMost(3)
    val baseIconSize = 18.dp
    val stepWidth = 13.dp
    val canvasWidth = baseIconSize + stepWidth * (visibleStackCount - 1)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(18.dp)
        ) {
            val singleSq = size.height
            val stepPx = stepWidth.toPx()
            for (idx in 0 until visibleStackCount) {
                val centerX = (singleSq / 2f) + idx * stepPx
                val centerY = size.height * 0.54f
                drawStauntonChessPiece(
                    piece = ChessPiece(group.type, group.color),
                    center = Offset(centerX, centerY),
                    squareSize = singleSq,
                    scale = 0.90f,
                    isElevated = false
                )
            }
        }

        if (group.count > 3) {
            Text(
                text = "×${group.count}",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = WarmBronzeText
            )
        }
    }
}

/**
 * 2. DUAL-PLAYER CAPTURED PIECES & MATERIAL BALANCE COMPONENT
 * Displays captured pieces for BOTH White and Black side-by-side along with a dynamic
 * Material Tug-of-War bar that updates automatically based on `ChessBoardState` from `ChessEngine`.
 */
@Composable
fun DualPlayerCapturedPiecesPanel(
    boardState: ChessBoardState,
    whitePlayerName: String,
    blackPlayerName: String,
    modifier: Modifier = Modifier
) {
    val whiteCapturedGroups = remember(boardState.capturedByWhite) {
        groupCapturedPieces(boardState.capturedByWhite)
    }
    val blackCapturedGroups = remember(boardState.capturedByBlack) {
        groupCapturedPieces(boardState.capturedByBlack)
    }

    val netAdvWhite = boardState.materialAdvantageWhite
    val whiteAdv = netAdvWhite.coerceAtLeast(0)
    val blackAdv = (-netAdvWhite).coerceAtLeast(0)

    // Map material advantage (-15..+15) to a 0.15f..0.85f split on the tug-of-war bar
    val targetWhiteFraction = (0.5f + (netAdvWhite.coerceIn(-16, 16) / 36f)).coerceIn(0.12f, 0.88f)
    val animatedWhiteFraction by animateFloatAsState(
        targetValue = targetWhiteFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "material_balance_fraction"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("dual_captured_pieces_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row: Title + Live Material Balance Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GoldAccentRestrained)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CAPTURED PIECES & MATERIAL BALANCE",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.1.sp,
                    color = TextSecondary
                )
            }

            val balanceLabel = when {
                netAdvWhite > 0 -> "WHITE +$netAdvWhite"
                netAdvWhite < 0 -> "BLACK +${abs(netAdvWhite)}"
                else -> "EVEN MATERIAL"
            }

            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .border(0.5.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                    .testTag("material_balance_badge")
            ) {
                Text(
                    text = balanceLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (netAdvWhite != 0) GoldAccentRestrained else TextSecondary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        // Live Material Tug-of-War Bar (Alabaster Platinum for White vs Obsidian/Bronze for Black)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(SurfaceGround)
                .border(0.5.dp, SurfaceBorder, RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWhiteFraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PlatinumLight, PlatinumDark)
                        )
                    )
            )
        }

        // Two-Column Comparison: White's Captures (Black pieces) vs Black's Captures (White pieces)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Tray: Captured by White
            PlayerCapturedColumnCard(
                playerLabel = whitePlayerName.substringBefore(" ("),
                sideBadge = "WHITE",
                capturedGroups = whiteCapturedGroups,
                totalCapturedCount = boardState.capturedByWhite.size,
                advantage = whiteAdv,
                isWhiteSide = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("white_captured_column")
            )

            // Right Tray: Captured by Black
            PlayerCapturedColumnCard(
                playerLabel = blackPlayerName.substringBefore(" ("),
                sideBadge = "BLACK",
                capturedGroups = blackCapturedGroups,
                totalCapturedCount = boardState.capturedByBlack.size,
                advantage = blackAdv,
                isWhiteSide = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("black_captured_column")
            )
        }
    }
}

@Composable
private fun PlayerCapturedColumnCard(
    playerLabel: String,
    sideBadge: String,
    capturedGroups: List<CapturedPieceGroup>,
    totalCapturedCount: Int,
    advantage: Int,
    isWhiteSide: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGround)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                Surface(
                    color = if (isWhiteSide) PlatinumLight else SurfaceElevated,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = sideBadge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (isWhiteSide) StateInteractiveText else TextPrimary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = playerLabel,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (advantage > 0) {
                Text(
                    text = "+$advantage",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = GoldAccentRestrained
                )
            } else {
                Text(
                    text = "$totalCapturedCount",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.5.sp,
                    color = TextMuted
                )
            }
        }

        if (capturedGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "No captures yet",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.5.sp,
                    color = TextMuted
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (group in capturedGroups) {
                    CapturedPieceClusterChip(group = group)
                }
            }
        }
    }
}

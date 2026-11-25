package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.BeginnerMovementRoute
import com.example.engine.BoardSquare
import com.example.engine.ChessBoardState
import com.example.engine.ChessMove
import com.example.engine.ChessPiece
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.ui.theme.BeginnerCaptureRing
import com.example.ui.theme.BeginnerPathLine
import com.example.ui.theme.BeginnerQuietBadge
import com.example.ui.theme.BlackPieceFill
import com.example.ui.theme.BlackPieceInnerHighlight
import com.example.ui.theme.BlackPieceStroke
import com.example.ui.theme.BoardDarkSquare
import com.example.ui.theme.BoardLastMoveBorder
import com.example.ui.theme.BoardLightSquare
import com.example.ui.theme.BoardRimChannel
import com.example.ui.theme.BoardRimMid
import com.example.ui.theme.BoardRimOuter
import com.example.ui.theme.ColdIceBlue
import com.example.ui.theme.GoldAccentRestrained
import com.example.ui.theme.KingCheckGlow
import com.example.ui.theme.WhitePieceFill
import com.example.ui.theme.WhitePieceInnerHighlight
import com.example.ui.theme.WhitePieceStroke
import com.example.ui.theme.WoodGrainHighlight
import com.example.ui.theme.WoodPerimeterBorder
import kotlinx.coroutines.launch

/**
 * Heirloom Grandmaster 2D Jetpack Compose Canvas Chessboard:
 * - Multi-beveled dark walnut border (#2a160d to #160b06) with fine inner routed channel and subtle wood grain.
 * - Warm alabaster eggshell light squares (#ede4d4) & deep espresso walnut dark squares (#3d281d).
 * - Inside-square rank (8-1 on file a) and file (a-h on rank 1) tournament typography.
 * - Dual-layered drop-shadowed Staunton vector pieces with internal crown/mitre/mane detail.
 * - Tactical Assist Engine (Beginner Mode) directional paths & capture target brackets.
 */
@Composable
fun ChessBoardCanvas(
    boardState: ChessBoardState,
    selectedSquare: BoardSquare?,
    legalMovesForSelected: List<ChessMove>,
    beginnerRoutes: List<BeginnerMovementRoute>,
    isBeginnerModeEnabled: Boolean,
    isFlipped: Boolean,
    hintMove: ChessMove?,
    onSquareTapped: (BoardSquare) -> Unit,
    onPieceDragged: (BoardSquare, BoardSquare) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "board_pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_fraction"
    )

    var draggingFromSquare by remember { mutableStateOf<BoardSquare?>(null) }
    var dragHoveredSquare by remember { mutableStateOf<BoardSquare?>(null) }
    val dragPointerOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var boardPixelSize by remember { mutableStateOf(0f) }

    fun pixelToBoardSquare(offset: Offset, canvasSize: Float): BoardSquare? {
        if (canvasSize <= 0f) return null
        val rimThickness = canvasSize * 0.036f
        val gridSpan = canvasSize - rimThickness * 2f
        val sqSize = gridSpan / 8f
        val relX = offset.x - rimThickness
        val relY = offset.y - rimThickness
        if (relX < 0f || relY < 0f || relX >= gridSpan || relY >= gridSpan) return null
        val visualCol = (relX / sqSize).toInt().coerceIn(0, 7)
        val visualRow = (relY / sqSize).toInt().coerceIn(0, 7)
        val logicalRow = if (isFlipped) 7 - visualRow else visualRow
        val logicalCol = if (isFlipped) 7 - visualCol else visualCol
        return BoardSquare(logicalRow, logicalCol)
    }

    fun squareCenterOffset(square: BoardSquare, canvasSize: Float): Offset {
        val rimThickness = canvasSize * 0.036f
        val gridSpan = canvasSize - rimThickness * 2f
        val sqSize = gridSpan / 8f
        val visualRow = if (isFlipped) 7 - square.row else square.row
        val visualCol = if (isFlipped) 7 - square.col else square.col
        return Offset(
            x = rimThickness + (visualCol + 0.5f) * sqSize,
            y = rimThickness + (visualRow + 0.5f) * sqSize
        )
    }

    val coordinateTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(RoundedCornerShape(14.dp))
            .background(BoardRimOuter)
            .border(1.5.dp, WoodPerimeterBorder, RoundedCornerShape(14.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chess_board_canvas")
                .pointerInput(boardState, isFlipped) {
                    detectTapGestures { tapOffset ->
                        val sq = pixelToBoardSquare(tapOffset, size.width.toFloat())
                        if (sq != null) {
                            onSquareTapped(sq)
                        }
                    }
                }
                .pointerInput(boardState, isFlipped) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val canvasW = size.width.toFloat()
                            boardPixelSize = canvasW
                            val sq = pixelToBoardSquare(startOffset, canvasW)
                            val piece = sq?.let { boardState.pieceAt(it) }
                            if (sq != null && piece != null && piece.color == boardState.turn) {
                                draggingFromSquare = sq
                                dragHoveredSquare = sq
                                onSquareTapped(sq)
                                coroutineScope.launch {
                                    dragPointerOffset.snapTo(startOffset)
                                }
                            } else {
                                draggingFromSquare = null
                            }
                        },
                        onDrag = { change, _ ->
                            val activeFrom = draggingFromSquare ?: return@detectDragGestures
                            change.consume()
                            val pos = change.position
                            dragHoveredSquare = pixelToBoardSquare(pos, boardPixelSize)
                            coroutineScope.launch {
                                dragPointerOffset.snapTo(pos)
                            }
                        },
                        onDragEnd = {
                            val fromSq = draggingFromSquare
                            val toSq = dragHoveredSquare
                            val canvasW = boardPixelSize
                            if (fromSq != null) {
                                val isLegalDrop = toSq != null &&
                                    toSq != fromSq &&
                                    legalMovesForSelected.any { it.from == fromSq && it.to == toSq }

                                coroutineScope.launch {
                                    val targetCenter = if (isLegalDrop) {
                                        squareCenterOffset(toSq, canvasW)
                                    } else {
                                        squareCenterOffset(fromSq, canvasW)
                                    }
                                    dragPointerOffset.animateTo(
                                        targetValue = targetCenter,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    if (isLegalDrop) {
                                        onPieceDragged(fromSq, toSq)
                                    }
                                    draggingFromSquare = null
                                    dragHoveredSquare = null
                                }
                            }
                        },
                        onDragCancel = {
                            val fromSq = draggingFromSquare
                            val canvasW = boardPixelSize
                            if (fromSq != null) {
                                coroutineScope.launch {
                                    dragPointerOffset.animateTo(
                                        targetValue = squareCenterOffset(fromSq, canvasW),
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    draggingFromSquare = null
                                    dragHoveredSquare = null
                                }
                            }
                        }
                    )
                }
        ) {
            val canvasW = size.width
            boardPixelSize = canvasW
            val rimThickness = canvasW * 0.036f
            val gridSpan = canvasW - rimThickness * 2f
            val sqSize = gridSpan / 8f

            // 1. Multi-Beveled Heirloom Walnut Perimeter Rim with 45° Wood Grain Striations
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        WoodGrainHighlight,
                        BoardRimMid,
                        BoardRimOuter,
                        BoardRimMid
                    ),
                    start = Offset(canvasW, 0f),
                    end = Offset(0f, canvasW)
                ),
                size = size,
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )

            // Subtle diagonal walnut grain lines across the beveled rim
            val grainCount = 36
            for (g in 0..grainCount) {
                val offsetPos = (canvasW * 2f / grainCount) * g
                drawLine(
                    color = Color.White.copy(alpha = 0.025f),
                    start = Offset(offsetPos, 0f),
                    end = Offset(0f, offsetPos),
                    strokeWidth = 1.5f
                )
            }

            // Inner routed channel around the 8x8 playing surface
            drawRect(
                color = BoardRimChannel,
                topLeft = Offset(rimThickness - 2.5f, rimThickness - 2.5f),
                size = Size(gridSpan + 5f, gridSpan + 5f),
                style = Stroke(width = 3f)
            )

            // 2. Draw 8x8 Heirloom Squares (Warm Alabaster Eggshell & Deep Espresso Walnut)
            for (visualRow in 0..7) {
                for (visualCol in 0..7) {
                    val logicalRow = if (isFlipped) 7 - visualRow else visualRow
                    val logicalCol = if (isFlipped) 7 - visualCol else visualCol
                    val square = BoardSquare(logicalRow, logicalCol)

                    val isLight = (logicalRow + logicalCol) % 2 == 0
                    val topLeft = Offset(
                        x = rimThickness + visualCol * sqSize,
                        y = rimThickness + visualRow * sqSize
                    )

                    val isLastMoveTo = square == boardState.lastMoveTo
                    val isLastMoveFrom = square == boardState.lastMoveFrom
                    val isSelected = square == selectedSquare
                    val isHintSquare = square == hintMove?.from || square == hintMove?.to

                    val baseColor = if (isLight) BoardLightSquare else BoardDarkSquare
                    drawRect(
                        color = baseColor,
                        topLeft = topLeft,
                        size = Size(sqSize, sqSize)
                    )

                    // Subtle diagonal walnut wood grain on dark squares
                    if (!isLight) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.08f)
                                ),
                                start = Offset(topLeft.x + sqSize, topLeft.y),
                                end = Offset(topLeft.x, topLeft.y + sqSize)
                            ),
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    // Selected Square or Last Move Double-Framed Heirloom Highlight (like f6 in reference)
                    if (isSelected || isLastMoveTo || isLastMoveFrom) {
                        val tintAlpha = if (isSelected) 0.28f else 0.22f
                        drawRect(
                            color = GoldAccentRestrained.copy(alpha = tintAlpha),
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                        // Outer hairline frame inside square
                        val inset = sqSize * 0.04f
                        drawRect(
                            color = BoardLastMoveBorder.copy(alpha = if (isSelected) 0.95f else 0.75f),
                            topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
                            size = Size(sqSize - inset * 2f, sqSize - inset * 2f),
                            style = Stroke(width = sqSize * 0.025f)
                        )
                    }

                    // Hover feedback during drag
                    if (square == dragHoveredSquare && draggingFromSquare != null) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize),
                            style = Stroke(width = sqSize * 0.06f)
                        )
                    }

                    // Hint square outline
                    if (isHintSquare) {
                        drawRect(
                            color = ColdIceBlue.copy(alpha = 0.85f),
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize),
                            style = Stroke(width = sqSize * 0.06f)
                        )
                    }

                    // King in Check radial glow
                    if (square == boardState.kingInCheckSquare) {
                        val center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    KingCheckGlow.copy(alpha = 0.75f + pulseFraction * 0.18f),
                                    KingCheckGlow.copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = sqSize * 0.65f
                            ),
                            radius = sqSize * 0.65f,
                            center = center
                        )
                    }

                    // 3. Inside-Square Rank (left column) & File (bottom row) Markers matching reference screenshot
                    coordinateTextPaint.textSize = (sqSize * 0.19f).coerceAtLeast(16f)
                    val coordColor = if (isLight) {
                        android.graphics.Color.argb(190, 92, 70, 56)
                    } else {
                        android.graphics.Color.argb(185, 198, 184, 166)
                    }
                    coordinateTextPaint.color = coordColor

                    if (visualCol == 0) {
                        val rankLabel = (8 - logicalRow).toString()
                        coordinateTextPaint.textAlign = Paint.Align.LEFT
                        drawContext.canvas.nativeCanvas.drawText(
                            rankLabel,
                            topLeft.x + sqSize * 0.08f,
                            topLeft.y + sqSize * 0.24f,
                            coordinateTextPaint
                        )
                    }

                    if (visualRow == 7) {
                        val fileLabel = ('a'.code + logicalCol).toChar().toString()
                        coordinateTextPaint.textAlign = Paint.Align.RIGHT
                        drawContext.canvas.nativeCanvas.drawText(
                            fileLabel,
                            topLeft.x + sqSize * 0.92f,
                            topLeft.y + sqSize * 0.91f,
                            coordinateTextPaint
                        )
                    }
                }
            }

            // 4. Tactical Assist Engine (Beginner Mode) Visual Guidance
            if (isBeginnerModeEnabled && beginnerRoutes.isNotEmpty()) {
                drawBeginnerModeVisualGuidance(
                    routes = beginnerRoutes,
                    sqSize = sqSize,
                    pulseFraction = pulseFraction,
                    squareCenter = { sq -> squareCenterOffset(sq, canvasW) }
                )
            } else if (!isBeginnerModeEnabled && legalMovesForSelected.isNotEmpty()) {
                val uniqueTargets = legalMovesForSelected.distinctBy { it.to }
                for (move in uniqueTargets) {
                    val center = squareCenterOffset(move.to, canvasW)
                    if (move.isCapture) {
                        drawCircle(
                            color = GoldAccentRestrained.copy(alpha = 0.65f),
                            radius = sqSize * 0.40f,
                            center = center,
                            style = Stroke(width = sqSize * 0.06f)
                        )
                    } else {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.28f),
                            radius = sqSize * 0.13f,
                            center = center
                        )
                    }
                }
            }

            // 5. Draw Stationary Shadowed Staunton Pieces
            for (r in 0..7) {
                for (c in 0..7) {
                    val sq = BoardSquare(r, c)
                    if (sq == draggingFromSquare) continue
                    val piece = boardState.pieceAt(sq) ?: continue
                    val center = squareCenterOffset(sq, canvasW)
                    drawStauntonChessPiece(
                        piece = piece,
                        center = center,
                        squareSize = sqSize,
                        scale = 1f,
                        isElevated = false
                    )
                }
            }

            // 6. Draw Elevated Dragged Piece
            val draggedSq = draggingFromSquare
            if (draggedSq != null) {
                val draggedPiece = boardState.pieceAt(draggedSq)
                if (draggedPiece != null) {
                    drawStauntonChessPiece(
                        piece = draggedPiece,
                        center = dragPointerOffset.value,
                        squareSize = sqSize,
                        scale = 1.18f,
                        isElevated = true
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBeginnerModeVisualGuidance(
    routes: List<BeginnerMovementRoute>,
    sqSize: Float,
    pulseFraction: Float,
    squareCenter: (BoardSquare) -> Offset
) {
    for (route in routes) {
        val startCenter = squareCenter(route.origin)
        val lineColor = if (route.isCapture) {
            BeginnerCaptureRing.copy(alpha = 0.75f)
        } else {
            ColdIceBlue.copy(alpha = 0.65f)
        }

        val path = Path().apply {
            moveTo(startCenter.x, startCenter.y)
            for (wp in route.waypoints) {
                val pt = squareCenter(wp)
                lineTo(pt.x, pt.y)
            }
        }

        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.22f),
            style = Stroke(
                width = sqSize * 0.14f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val strokeEffect = if (route.isKnightJump) {
            PathEffect.dashPathEffect(floatArrayOf(sqSize * 0.12f, sqSize * 0.09f), 0f)
        } else {
            null
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = sqSize * 0.055f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = strokeEffect
            )
        )
    }

    for (route in routes) {
        val destCenter = squareCenter(route.destination)
        if (route.isCapture) {
            val ringRadius = sqSize * (0.39f + pulseFraction * 0.03f)
            drawCircle(
                color = BeginnerCaptureRing.copy(alpha = 0.22f),
                radius = ringRadius * 1.08f,
                center = destCenter
            )
            drawCircle(
                color = BeginnerCaptureRing,
                radius = ringRadius,
                center = destCenter,
                style = Stroke(width = sqSize * 0.07f)
            )
            val half = sqSize * 0.43f
            val bracketLen = sqSize * 0.14f
            val strokeW = sqSize * 0.05f
            for (sx in listOf(-1f, 1f)) {
                for (sy in listOf(-1f, 1f)) {
                    val corner = Offset(destCenter.x + sx * half, destCenter.y + sy * half)
                    drawLine(
                        color = BeginnerCaptureRing,
                        start = corner,
                        end = Offset(corner.x - sx * bracketLen, corner.y),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = BeginnerCaptureRing,
                        start = corner,
                        end = Offset(corner.x, corner.y - sy * bracketLen),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        } else {
            val badgeRadius = sqSize * (0.15f + pulseFraction * 0.02f)
            drawCircle(
                color = BeginnerQuietBadge.copy(alpha = 0.30f),
                radius = badgeRadius * 1.45f,
                center = destCenter
            )
            drawCircle(
                color = BeginnerQuietBadge,
                radius = badgeRadius,
                center = destCenter
            )
            drawCircle(
                color = BeginnerPathLine,
                radius = badgeRadius * 0.38f,
                center = destCenter
            )
        }
    }
}

/**
 * Renders Heirloom Staunton chess pieces with dual drop-shadow depth
 * (`drop-shadow(0 4px 6px rgba(0,0,0,0.65))` + `drop-shadow(0 1px 2px rgba(0,0,0,0.8))`)
 * and internal engraving lines matching the Grandmaster Suite reference screenshot.
 */
internal fun DrawScope.drawStauntonChessPiece(
    piece: ChessPiece,
    center: Offset,
    squareSize: Float,
    scale: Float,
    isElevated: Boolean
) {
    val unit = squareSize * 0.38f * scale
    val isWhite = piece.color == PieceColor.WHITE
    val fillColor = if (isWhite) WhitePieceFill else BlackPieceFill
    val strokeColor = if (isWhite) WhitePieceStroke else BlackPieceStroke
    val innerDetailColor = if (isWhite) WhitePieceStroke.copy(alpha = 0.75f) else BlackPieceStroke.copy(alpha = 0.85f)
    val rimGleam = if (isWhite) WhitePieceInnerHighlight else BlackPieceInnerHighlight

    val cx = center.x
    val cy = center.y

    // Dual-layer ambient & contact drop shadows per DESIGN.md Section 5
    val ambientOffsetY = if (isElevated) unit * 0.34f else unit * 0.16f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = if (isElevated) 0.72f else 0.62f),
                Color.Black.copy(alpha = 0.28f),
                Color.Transparent
            ),
            center = Offset(cx, cy + unit * 0.42f + ambientOffsetY),
            radius = unit * 0.95f
        ),
        radius = unit * 0.95f,
        center = Offset(cx, cy + unit * 0.42f + ambientOffsetY)
    )

    // Tight contact shadow directly under the pedestal
    drawOval(
        color = Color.Black.copy(alpha = 0.80f),
        topLeft = Offset(cx - unit * 0.74f, cy + unit * 0.64f),
        size = Size(unit * 1.48f, unit * 0.28f)
    )

    val path = Path()
    val baseBottom = cy + unit * 0.76f
    val baseMid = cy + unit * 0.62f
    val baseTop = cy + unit * 0.48f

    // Two-tiered Staunton pedestal base
    path.moveTo(cx - unit * 0.72f, baseBottom)
    path.lineTo(cx + unit * 0.72f, baseBottom)
    path.lineTo(cx + unit * 0.66f, baseMid)
    path.lineTo(cx + unit * 0.56f, baseTop)
    path.lineTo(cx - unit * 0.56f, baseTop)
    path.lineTo(cx - unit * 0.66f, baseMid)
    path.close()

    when (piece.type) {
        PieceType.PAWN -> {
            path.moveTo(cx - unit * 0.44f, baseTop)
            path.cubicTo(
                cx - unit * 0.36f, cy + unit * 0.12f,
                cx - unit * 0.20f, cy - unit * 0.04f,
                cx - unit * 0.28f, cy - unit * 0.14f
            )
            path.lineTo(cx + unit * 0.28f, cy - unit * 0.14f)
            path.cubicTo(
                cx + unit * 0.20f, cy - unit * 0.04f,
                cx + unit * 0.36f, cy + unit * 0.12f,
                cx + unit * 0.44f, baseTop
            )
            path.close()

            drawEngravedPiecePath(path, fillColor, strokeColor, unit)

            // Collar ring
            drawLine(
                color = innerDetailColor,
                start = Offset(cx - unit * 0.28f, cy - unit * 0.14f),
                end = Offset(cx + unit * 0.28f, cy - unit * 0.14f),
                strokeWidth = unit * 0.09f,
                cap = StrokeCap.Round
            )

            // Spherical head
            val headCenter = Offset(cx, cy - unit * 0.40f)
            drawCircle(color = fillColor, radius = unit * 0.31f, center = headCenter)
            drawCircle(
                color = strokeColor,
                radius = unit * 0.31f,
                center = headCenter,
                style = Stroke(width = unit * 0.10f)
            )
            drawCircle(
                color = rimGleam.copy(alpha = 0.65f),
                radius = unit * 0.09f,
                center = Offset(headCenter.x - unit * 0.09f, headCenter.y - unit * 0.09f)
            )
        }

        PieceType.ROOK -> {
            path.moveTo(cx - unit * 0.48f, baseTop)
            path.lineTo(cx - unit * 0.40f, cy - unit * 0.24f)
            path.lineTo(cx - unit * 0.54f, cy - unit * 0.34f)
            path.lineTo(cx - unit * 0.54f, cy - unit * 0.72f)
            path.lineTo(cx - unit * 0.32f, cy - unit * 0.72f)
            path.lineTo(cx - unit * 0.32f, cy - unit * 0.52f)
            path.lineTo(cx - unit * 0.12f, cy - unit * 0.52f)
            path.lineTo(cx - unit * 0.12f, cy - unit * 0.72f)
            path.lineTo(cx + unit * 0.12f, cy - unit * 0.72f)
            path.lineTo(cx + unit * 0.12f, cy - unit * 0.52f)
            path.lineTo(cx + unit * 0.32f, cy - unit * 0.52f)
            path.lineTo(cx + unit * 0.32f, cy - unit * 0.72f)
            path.lineTo(cx + unit * 0.54f, cy - unit * 0.72f)
            path.lineTo(cx + unit * 0.54f, cy - unit * 0.34f)
            path.lineTo(cx + unit * 0.40f, cy - unit * 0.24f)
            path.lineTo(cx + unit * 0.48f, baseTop)
            path.close()

            drawEngravedPiecePath(path, fillColor, strokeColor, unit)
            // Internal turret seam lines
            drawLine(
                color = innerDetailColor,
                start = Offset(cx - unit * 0.36f, cy - unit * 0.24f),
                end = Offset(cx + unit * 0.36f, cy - unit * 0.24f),
                strokeWidth = unit * 0.08f,
                cap = StrokeCap.Round
            )
        }

        PieceType.KNIGHT -> {
            path.moveTo(cx - unit * 0.50f, baseTop)
            path.cubicTo(
                cx - unit * 0.50f, cy + unit * 0.04f,
                cx - unit * 0.16f, cy - unit * 0.04f,
                cx - unit * 0.14f, cy - unit * 0.20f
            )
            path.lineTo(cx - unit * 0.58f, cy - unit * 0.06f)
            path.lineTo(cx - unit * 0.64f, cy - unit * 0.26f)
            path.lineTo(cx - unit * 0.26f, cy - unit * 0.60f)
            path.lineTo(cx - unit * 0.16f, cy - unit * 0.84f)
            path.lineTo(cx + unit * 0.06f, cy - unit * 0.70f)
            path.cubicTo(
                cx + unit * 0.48f, cy - unit * 0.62f,
                cx + unit * 0.62f, cy - unit * 0.10f,
                cx + unit * 0.50f, baseTop
            )
            path.close()

            drawEngravedPiecePath(path, fillColor, strokeColor, unit)
            // Eye & bridle engraving
            drawCircle(
                color = innerDetailColor,
                radius = unit * 0.065f,
                center = Offset(cx - unit * 0.28f, cy - unit * 0.42f)
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(cx + unit * 0.18f, cy - unit * 0.48f),
                end = Offset(cx + unit * 0.28f, cy + unit * 0.22f),
                strokeWidth = unit * 0.07f,
                cap = StrokeCap.Round
            )
        }

        PieceType.BISHOP -> {
            path.moveTo(cx - unit * 0.46f, baseTop)
            path.cubicTo(
                cx - unit * 0.54f, cy - unit * 0.06f,
                cx - unit * 0.44f, cy - unit * 0.54f,
                cx, cy - unit * 0.70f
            )
            path.cubicTo(
                cx + unit * 0.44f, cy - unit * 0.54f,
                cx + unit * 0.54f, cy - unit * 0.06f,
                cx + unit * 0.46f, baseTop
            )
            path.close()

            drawEngravedPiecePath(path, fillColor, strokeColor, unit)
            val orbCenter = Offset(cx, cy - unit * 0.80f)
            drawCircle(color = fillColor, radius = unit * 0.12f, center = orbCenter)
            drawCircle(
                color = strokeColor,
                radius = unit * 0.12f,
                center = orbCenter,
                style = Stroke(width = unit * 0.08f)
            )
            // Ceremonial cross/slash inside mitre
            drawLine(
                color = innerDetailColor,
                start = Offset(cx, cy - unit * 0.48f),
                end = Offset(cx, cy - unit * 0.18f),
                strokeWidth = unit * 0.09f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(cx - unit * 0.14f, cy - unit * 0.34f),
                end = Offset(cx + unit * 0.14f, cy - unit * 0.34f),
                strokeWidth = unit * 0.09f,
                cap = StrokeCap.Round
            )
        }

        PieceType.QUEEN -> {
            path.moveTo(cx - unit * 0.52f, baseTop)
            path.lineTo(cx - unit * 0.66f, cy - unit * 0.46f)
            path.lineTo(cx - unit * 0.32f, cy - unit * 0.10f)
            path.lineTo(cx - unit * 0.34f, cy - unit * 0.64f)
            path.lineTo(cx, cy - unit * 0.16f)
            path.lineTo(cx, cy - unit * 0.74f)
            path.lineTo(cx, cy - unit * 0.16f)
            path.lineTo(cx + unit * 0.34f, cy - unit * 0.64f)
            path.lineTo(cx + unit * 0.32f, cy - unit * 0.10f)
            path.lineTo(cx + unit * 0.66f, cy - unit * 0.46f)
            path.lineTo(cx + unit * 0.52f, baseTop)
            path.close()

            drawEngravedPiecePath(path, fillColor, strokeColor, unit)

            val pearls = listOf(
                Offset(cx - unit * 0.66f, cy - unit * 0.52f),
                Offset(cx - unit * 0.34f, cy - unit * 0.70f),
                Offset(cx, cy - unit * 0.80f),
                Offset(cx + unit * 0.34f, cy - unit * 0.70f),
                Offset(cx + unit * 0.66f, cy - unit * 0.52f)
            )
            for (pt in pearls) {
                drawCircle(color = fillColor, radius = unit * 0.095f, center = pt)
                drawCircle(
                    color = strokeColor,
                    radius = unit * 0.095f,
                    center = pt,
                    style = Stroke(width = unit * 0.065f)
                )
            }
        }

        PieceType.KING -> {
            path.moveTo(cx - unit * 0.52f, baseTop)
            path.cubicTo(
                cx - unit * 0.66f, cy - unit * 0.08f,
                cx - unit * 0.50f, cy - unit * 0.50f,
                cx, cy - unit * 0.46f
            )
            path.cubicTo(
                cx + unit * 0.50f, cy - unit * 0.50f,
                cx + unit * 0.66f, cy - unit * 0.08f,
                cx + unit * 0.52f, baseTop
            )
            path.close()

            drawEngravedPiecePath(path, fillColor, strokeColor, unit)

            // Sovereign Cross
            val crossVTop = Offset(cx, cy - unit * 0.92f)
            val crossVBot = Offset(cx, cy - unit * 0.46f)
            val crossHLeft = Offset(cx - unit * 0.21f, cy - unit * 0.72f)
            val crossHRight = Offset(cx + unit * 0.21f, cy - unit * 0.72f)

            drawLine(
                color = strokeColor,
                start = crossVTop,
                end = crossVBot,
                strokeWidth = unit * 0.19f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = crossHLeft,
                end = crossHRight,
                strokeWidth = unit * 0.19f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = fillColor,
                start = crossVTop,
                end = crossVBot,
                strokeWidth = unit * 0.09f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = fillColor,
                start = crossHLeft,
                end = crossHRight,
                strokeWidth = unit * 0.09f,
                cap = StrokeCap.Round
            )
        }
    }

    // Two pedestal horizontal engraving rings matching Staunton turnings
    drawLine(
        color = innerDetailColor,
        start = Offset(cx - unit * 0.56f, baseMid),
        end = Offset(cx + unit * 0.56f, baseMid),
        strokeWidth = unit * 0.065f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = rimGleam.copy(alpha = 0.8f),
        start = Offset(cx - unit * 0.48f, baseTop + unit * 0.05f),
        end = Offset(cx + unit * 0.48f, baseTop + unit * 0.05f),
        strokeWidth = unit * 0.055f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawEngravedPiecePath(
    path: Path,
    fillColor: Color,
    strokeColor: Color,
    unit: Float
) {
    drawPath(path = path, color = fillColor, style = Fill)
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = unit * 0.105f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

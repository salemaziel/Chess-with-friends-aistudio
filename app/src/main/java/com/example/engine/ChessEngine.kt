package com.example.engine

import kotlin.math.abs
import kotlin.math.sign

object ChessEngine {
    const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun parseFen(
        fen: String = INITIAL_FEN,
        lastFrom: BoardSquare? = null,
        lastTo: BoardSquare? = null
    ): ChessBoardState {
        val parts = fen.trim().split(Regex("\\s+"))
        val boardPart = parts.getOrNull(0) ?: INITIAL_FEN.substringBefore(" ")
        val turnPart = parts.getOrNull(1) ?: "w"
        val castlingPart = parts.getOrNull(2) ?: "KQkq"
        val epPart = parts.getOrNull(3) ?: "-"
        val halfMovePart = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullMovePart = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val grid = MutableList(8) { MutableList<ChessPiece?>(8) { null } }
        val ranks = boardPart.split("/")
        for (r in 0 until minOf(8, ranks.size)) {
            var c = 0
            for (ch in ranks[r]) {
                if (c >= 8) break
                if (ch.isDigit()) {
                    c += ch.digitToInt()
                } else {
                    val parsed = PieceType.fromFenChar(ch)
                    if (parsed != null) {
                        grid[r][c] = ChessPiece(parsed.first, parsed.second)
                    }
                    c++
                }
            }
        }

        val turn = if (turnPart.lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE
        val castling = CastlingRights.fromFen(castlingPart)
        val epSquare = BoardSquare.fromAlgebraic(epPart)

        val (capturedByWhite, capturedByBlack, materialDiff) = computeCapturedAndMaterial(grid)

        val baseState = ChessBoardState(
            grid = grid,
            turn = turn,
            castlingRights = castling,
            enPassantTarget = epSquare,
            halfMoveClock = halfMovePart,
            fullMoveNumber = fullMovePart,
            lastMoveFrom = lastFrom,
            lastMoveTo = lastTo,
            capturedByWhite = capturedByWhite,
            capturedByBlack = capturedByBlack,
            materialAdvantageWhite = materialDiff
        )

        val kingSquare = findKingSquare(baseState, turn)
        val inCheck = kingSquare != null && isSquareAttacked(baseState, kingSquare, turn.opposite)
        val legalMoves = generateLegalMovesWithoutSan(baseState)

        val evaluatedStatus = when {
            legalMoves.isEmpty() && inCheck -> GameStatus.CHECKMATE
            legalMoves.isEmpty() && !inCheck -> GameStatus.STALEMATE
            halfMovePart >= 100 -> GameStatus.DRAW_FIFTY_MOVE
            else -> GameStatus.ACTIVE
        }

        return baseState.copy(
            status = evaluatedStatus,
            isCheck = inCheck,
            kingInCheckSquare = if (inCheck) kingSquare else null
        )
    }

    fun toFen(state: ChessBoardState): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var emptyCount = 0
            for (c in 0..7) {
                val p = state.grid[r][c]
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (r < 7) sb.append('/')
        }
        sb.append(' ').append(state.turn.fenChar)
        sb.append(' ').append(state.castlingRights.toFen())
        sb.append(' ').append(state.enPassantTarget?.toAlgebraic() ?: "-")
        sb.append(' ').append(state.halfMoveClock)
        sb.append(' ').append(state.fullMoveNumber)
        return sb.toString()
    }

    fun getLegalMoves(state: ChessBoardState): List<ChessMove> {
        if (state.status.isTerminal) return emptyList()
        val rawLegal = generateLegalMovesWithoutSan(state)
        return rawLegal.map { move ->
            move.copy(san = buildSan(state, move, rawLegal))
        }
    }

    fun getLegalMovesForSquare(state: ChessBoardState, square: BoardSquare): List<ChessMove> {
        val piece = state.pieceAt(square) ?: return emptyList()
        if (piece.color != state.turn) return emptyList()
        return getLegalMoves(state).filter { it.from == square }
    }

    fun isPromotionMove(state: ChessBoardState, from: BoardSquare, to: BoardSquare): Boolean {
        val piece = state.pieceAt(from) ?: return false
        if (piece.type != PieceType.PAWN) return false
        val targetRow = if (piece.color == PieceColor.WHITE) 0 else 7
        if (to.row != targetRow) return false
        return getLegalMovesForSquare(state, from).any { it.to == to && it.promotion != null }
    }

    /**
     * Computes rich directional movement paths for the Beginner Mode Visual Engine.
     * Sliding pieces get continuous connected rays; Knights get L-shaped waypoints;
     * Pawns and Kings get direct paths and capture badges.
     */
    fun computeBeginnerRoutes(
        state: ChessBoardState,
        selectedSquare: BoardSquare
    ): List<BeginnerMovementRoute> {
        val piece = state.pieceAt(selectedSquare) ?: return emptyList()
        val legalMoves = getLegalMovesForSquare(state, selectedSquare)
            .distinctBy { it.to } // Collapse 4 promotion choices on the same destination square
        if (legalMoves.isEmpty()) return emptyList()

        return legalMoves.map { move ->
            val dRow = move.to.row - selectedSquare.row
            val dCol = move.to.col - selectedSquare.col
            val waypoints = mutableListOf<BoardSquare>()

            when (piece.type) {
                PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN -> {
                    val stepR = dRow.sign
                    val stepC = dCol.sign
                    var curR = selectedSquare.row + stepR
                    var curC = selectedSquare.col + stepC
                    while (curR != move.to.row || curC != move.to.col) {
                        waypoints.add(BoardSquare(curR, curC))
                        curR += stepR
                        curC += stepC
                    }
                    waypoints.add(move.to)
                    BeginnerMovementRoute(
                        origin = selectedSquare,
                        waypoints = waypoints,
                        destination = move.to,
                        isCapture = move.isCapture
                    )
                }

                PieceType.KNIGHT -> {
                    // Build an L-shaped elbow waypoint along the 2-square axis first
                    val elbow = if (abs(dRow) == 2) {
                        BoardSquare(selectedSquare.row + dRow, selectedSquare.col)
                    } else {
                        BoardSquare(selectedSquare.row, selectedSquare.col + dCol)
                    }
                    waypoints.add(elbow)
                    waypoints.add(move.to)
                    BeginnerMovementRoute(
                        origin = selectedSquare,
                        waypoints = waypoints,
                        destination = move.to,
                        isCapture = move.isCapture,
                        isKnightJump = true
                    )
                }

                PieceType.PAWN -> {
                    if (abs(dRow) == 2) {
                        waypoints.add(BoardSquare(selectedSquare.row + dRow.sign, selectedSquare.col))
                    }
                    waypoints.add(move.to)
                    BeginnerMovementRoute(
                        origin = selectedSquare,
                        waypoints = waypoints,
                        destination = move.to,
                        isCapture = move.isCapture
                    )
                }

                PieceType.KING -> {
                    if (move.isCastlingKingside || move.isCastlingQueenside) {
                        waypoints.add(BoardSquare(selectedSquare.row, selectedSquare.col + dCol.sign))
                    }
                    waypoints.add(move.to)
                    BeginnerMovementRoute(
                        origin = selectedSquare,
                        waypoints = waypoints,
                        destination = move.to,
                        isCapture = move.isCapture,
                        isCastling = move.isCastlingKingside || move.isCastlingQueenside
                    )
                }
            }
        }
    }

    fun applyMove(state: ChessBoardState, move: ChessMove): ChessBoardState {
        val nextBase = applyMoveInternal(state, move)
        val rawFen = toFen(nextBase)
        return parseFen(rawFen, lastFrom = move.from, lastTo = move.to)
    }

    private fun applyMoveInternal(state: ChessBoardState, move: ChessMove): ChessBoardState {
        val newGrid = state.grid.map { it.toMutableList() }.toMutableList()
        val movingPiece = move.piece
        newGrid[move.from.row][move.from.col] = null

        // Handle En Passant capture removal
        if (move.isEnPassant) {
            val capturedPawnRow = move.from.row
            val capturedPawnCol = move.to.col
            newGrid[capturedPawnRow][capturedPawnCol] = null
        }

        // Handle Castling rook transfer
        if (move.isCastlingKingside) {
            val row = move.from.row
            val rook = newGrid[row][7]
            newGrid[row][7] = null
            newGrid[row][5] = rook
        } else if (move.isCastlingQueenside) {
            val row = move.from.row
            val rook = newGrid[row][0]
            newGrid[row][0] = null
            newGrid[row][3] = rook
        }

        // Place moved or promoted piece
        val finalPiece = if (move.promotion != null) {
            ChessPiece(move.promotion, movingPiece.color)
        } else {
            movingPiece
        }
        newGrid[move.to.row][move.to.col] = finalPiece

        // Update Castling Rights
        var rights = state.castlingRights
        if (movingPiece.type == PieceType.KING) {
            rights = if (movingPiece.color == PieceColor.WHITE) {
                rights.copy(whiteKingside = false, whiteQueenside = false)
            } else {
                rights.copy(blackKingside = false, blackQueenside = false)
            }
        }
        if (move.from == BoardSquare(7, 7) || move.to == BoardSquare(7, 7)) {
            rights = rights.copy(whiteKingside = false)
        }
        if (move.from == BoardSquare(7, 0) || move.to == BoardSquare(7, 0)) {
            rights = rights.copy(whiteQueenside = false)
        }
        if (move.from == BoardSquare(0, 7) || move.to == BoardSquare(0, 7)) {
            rights = rights.copy(blackKingside = false)
        }
        if (move.from == BoardSquare(0, 0) || move.to == BoardSquare(0, 0)) {
            rights = rights.copy(blackQueenside = false)
        }

        // Update En Passant Target Square
        val newEpTarget = if (movingPiece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            val epRow = (move.from.row + move.to.row) / 2
            BoardSquare(epRow, move.from.col)
        } else {
            null
        }

        val newHalfMove = if (movingPiece.type == PieceType.PAWN || move.isCapture) {
            0
        } else {
            state.halfMoveClock + 1
        }

        val newFullMove = if (state.turn == PieceColor.BLACK) {
            state.fullMoveNumber + 1
        } else {
            state.fullMoveNumber
        }

        return state.copy(
            grid = newGrid,
            turn = state.turn.opposite,
            castlingRights = rights,
            enPassantTarget = newEpTarget,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove,
            lastMoveFrom = move.from,
            lastMoveTo = move.to
        )
    }

    private fun generateLegalMovesWithoutSan(state: ChessBoardState): List<ChessMove> {
        val pseudoMoves = generatePseudoLegalMoves(state, state.turn)
        val legal = mutableListOf<ChessMove>()
        for (move in pseudoMoves) {
            val afterState = applyMoveInternal(state, move)
            val myKingSq = findKingSquare(afterState, state.turn)
            if (myKingSq != null && !isSquareAttacked(afterState, myKingSq, state.turn.opposite)) {
                legal.add(move)
            }
        }
        return legal
    }

    private fun generatePseudoLegalMoves(
        state: ChessBoardState,
        color: PieceColor
    ): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.grid[r][c] ?: continue
                if (piece.color != color) continue
                val from = BoardSquare(r, c)
                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(state, from, piece, moves)
                    PieceType.KNIGHT -> generateKnightMoves(state, from, piece, moves)
                    PieceType.BISHOP -> generateSlidingMoves(
                        state, from, piece,
                        listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves
                    )
                    PieceType.ROOK -> generateSlidingMoves(
                        state, from, piece,
                        listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves
                    )
                    PieceType.QUEEN -> generateSlidingMoves(
                        state, from, piece,
                        listOf(
                            -1 to -1, -1 to 1, 1 to -1, 1 to 1,
                            -1 to 0, 1 to 0, 0 to -1, 0 to 1
                        ), moves
                    )
                    PieceType.KING -> generateKingMoves(state, from, piece, moves)
                }
            }
        }
        return moves
    }

    private fun generatePawnMoves(
        state: ChessBoardState,
        from: BoardSquare,
        piece: ChessPiece,
        out: MutableList<ChessMove>
    ) {
        val dir = if (piece.color == PieceColor.WHITE) -1 else 1
        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
        val promoRow = if (piece.color == PieceColor.WHITE) 0 else 7
        val promotionTypes = listOf(
            PieceType.QUEEN,
            PieceType.ROOK,
            PieceType.BISHOP,
            PieceType.KNIGHT
        )

        // Single step forward
        val oneStep = BoardSquare(from.row + dir, from.col)
        if (oneStep.isValid && state.pieceAt(oneStep) == null) {
            if (oneStep.row == promoRow) {
                for (promo in promotionTypes) {
                    out.add(ChessMove(from = from, to = oneStep, piece = piece, promotion = promo))
                }
            } else {
                out.add(ChessMove(from = from, to = oneStep, piece = piece))
                // Double step from starting rank
                val twoStep = BoardSquare(from.row + dir * 2, from.col)
                if (from.row == startRow && twoStep.isValid && state.pieceAt(twoStep) == null) {
                    out.add(ChessMove(from = from, to = twoStep, piece = piece))
                }
            }
        }

        // Diagonal captures & En Passant
        for (dc in listOf(-1, 1)) {
            val diag = BoardSquare(from.row + dir, from.col + dc)
            if (!diag.isValid) continue
            val targetPiece = state.pieceAt(diag)
            if (targetPiece != null && targetPiece.color != piece.color) {
                if (diag.row == promoRow) {
                    for (promo in promotionTypes) {
                        out.add(
                            ChessMove(
                                from = from,
                                to = diag,
                                piece = piece,
                                capturedPiece = targetPiece,
                                promotion = promo
                            )
                        )
                    }
                } else {
                    out.add(
                        ChessMove(
                            from = from,
                            to = diag,
                            piece = piece,
                            capturedPiece = targetPiece
                        )
                    )
                }
            } else if (diag == state.enPassantTarget) {
                val epPawn = state.pieceAt(from.row, diag.col)
                if (epPawn != null && epPawn.color != piece.color && epPawn.type == PieceType.PAWN) {
                    out.add(
                        ChessMove(
                            from = from,
                            to = diag,
                            piece = piece,
                            capturedPiece = epPawn,
                            isEnPassant = true
                        )
                    )
                }
            }
        }
    }

    private fun generateKnightMoves(
        state: ChessBoardState,
        from: BoardSquare,
        piece: ChessPiece,
        out: MutableList<ChessMove>
    ) {
        val offsets = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in offsets) {
            val to = BoardSquare(from.row + dr, from.col + dc)
            if (!to.isValid) continue
            val target = state.pieceAt(to)
            if (target == null || target.color != piece.color) {
                out.add(ChessMove(from = from, to = to, piece = piece, capturedPiece = target))
            }
        }
    }

    private fun generateSlidingMoves(
        state: ChessBoardState,
        from: BoardSquare,
        piece: ChessPiece,
        directions: List<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        for ((dr, dc) in directions) {
            var r = from.row + dr
            var c = from.col + dc
            while (r in 0..7 && c in 0..7) {
                val to = BoardSquare(r, c)
                val target = state.pieceAt(to)
                if (target == null) {
                    out.add(ChessMove(from = from, to = to, piece = piece))
                } else {
                    if (target.color != piece.color) {
                        out.add(ChessMove(from = from, to = to, piece = piece, capturedPiece = target))
                    }
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    private fun generateKingMoves(
        state: ChessBoardState,
        from: BoardSquare,
        piece: ChessPiece,
        out: MutableList<ChessMove>
    ) {
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val to = BoardSquare(from.row + dr, from.col + dc)
                if (!to.isValid) continue
                val target = state.pieceAt(to)
                if (target == null || target.color != piece.color) {
                    out.add(ChessMove(from = from, to = to, piece = piece, capturedPiece = target))
                }
            }
        }

        // Castling moves
        val homeRow = if (piece.color == PieceColor.WHITE) 7 else 0
        if (from.row == homeRow && from.col == 4) {
            val canKingside = if (piece.color == PieceColor.WHITE) {
                state.castlingRights.whiteKingside
            } else {
                state.castlingRights.blackKingside
            }
            val canQueenside = if (piece.color == PieceColor.WHITE) {
                state.castlingRights.whiteQueenside
            } else {
                state.castlingRights.blackQueenside
            }

            val enemy = piece.color.opposite
            val kingNotInCheck = !isSquareAttacked(state, from, enemy)

            if (canKingside && kingNotInCheck) {
                val fSq = BoardSquare(homeRow, 5)
                val gSq = BoardSquare(homeRow, 6)
                val rookPiece = state.pieceAt(homeRow, 7)
                if (state.pieceAt(fSq) == null &&
                    state.pieceAt(gSq) == null &&
                    rookPiece?.type == PieceType.ROOK &&
                    rookPiece.color == piece.color &&
                    !isSquareAttacked(state, fSq, enemy) &&
                    !isSquareAttacked(state, gSq, enemy)
                ) {
                    out.add(
                        ChessMove(
                            from = from,
                            to = gSq,
                            piece = piece,
                            isCastlingKingside = true
                        )
                    )
                }
            }

            if (canQueenside && kingNotInCheck) {
                val dSq = BoardSquare(homeRow, 3)
                val cSq = BoardSquare(homeRow, 2)
                val bSq = BoardSquare(homeRow, 1)
                val rookPiece = state.pieceAt(homeRow, 0)
                if (state.pieceAt(dSq) == null &&
                    state.pieceAt(cSq) == null &&
                    state.pieceAt(bSq) == null &&
                    rookPiece?.type == PieceType.ROOK &&
                    rookPiece.color == piece.color &&
                    !isSquareAttacked(state, dSq, enemy) &&
                    !isSquareAttacked(state, cSq, enemy)
                ) {
                    out.add(
                        ChessMove(
                            from = from,
                            to = cSq,
                            piece = piece,
                            isCastlingQueenside = true
                        )
                    )
                }
            }
        }
    }

    fun isSquareAttacked(
        state: ChessBoardState,
        square: BoardSquare,
        byColor: PieceColor
    ): Boolean {
        // 1. Pawn attacks
        val pawnAttackRow = if (byColor == PieceColor.WHITE) square.row + 1 else square.row - 1
        for (dc in listOf(-1, 1)) {
            val p = state.pieceAt(pawnAttackRow, square.col + dc)
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }

        // 2. Knight attacks
        val knightOffsets = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in knightOffsets) {
            val p = state.pieceAt(square.row + dr, square.col + dc)
            if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
        }

        // 3. King attacks
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val p = state.pieceAt(square.row + dr, square.col + dc)
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // 4. Orthogonal sliding attacks (Rook / Queen)
        for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            var r = square.row + dr
            var c = square.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = state.pieceAt(r, c)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        // 5. Diagonal sliding attacks (Bishop / Queen)
        for ((dr, dc) in listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
            var r = square.row + dr
            var c = square.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = state.pieceAt(r, c)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        return false
    }

    private fun findKingSquare(state: ChessBoardState, color: PieceColor): BoardSquare? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.grid[r][c]
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return BoardSquare(r, c)
                }
            }
        }
        return null
    }

    private fun buildSan(
        state: ChessBoardState,
        move: ChessMove,
        allLegalMoves: List<ChessMove>
    ): String {
        val after = applyMoveInternal(state, move)
        val oppKing = findKingSquare(after, state.turn.opposite)
        val oppInCheck = oppKing != null && isSquareAttacked(after, oppKing, state.turn)
        val oppResponses = generateLegalMovesWithoutSan(after)
        val checkSuffix = when {
            oppInCheck && oppResponses.isEmpty() -> "#"
            oppInCheck -> "+"
            else -> ""
        }

        if (move.isCastlingKingside) return "O-O$checkSuffix"
        if (move.isCastlingQueenside) return "O-O-O$checkSuffix"

        val sb = StringBuilder()
        if (move.piece.type == PieceType.PAWN) {
            if (move.isCapture) {
                sb.append(move.from.fileChar).append('x')
            }
            sb.append(move.to.toAlgebraic())
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.sanLetter)
            }
        } else {
            sb.append(move.piece.type.sanLetter)
            // Disambiguate if another same-type piece can move to the same square
            val ambiguous = allLegalMoves.filter {
                it != move &&
                    it.piece.type == move.piece.type &&
                    it.to == move.to &&
                    it.promotion == move.promotion
            }
            if (ambiguous.isNotEmpty()) {
                val sameFile = ambiguous.any { it.from.col == move.from.col }
                val sameRank = ambiguous.any { it.from.row == move.from.row }
                when {
                    !sameFile -> sb.append(move.from.fileChar)
                    !sameRank -> sb.append(move.from.rankNumber)
                    else -> sb.append(move.from.toAlgebraic())
                }
            }
            if (move.isCapture) sb.append('x')
            sb.append(move.to.toAlgebraic())
        }
        sb.append(checkSuffix)
        return sb.toString()
    }

    private fun computeCapturedAndMaterial(
        grid: List<List<ChessPiece?>>
    ): Triple<List<ChessPiece>, List<ChessPiece>, Int> {
        val nonPawnStartCounts = linkedMapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2
        )
        val whiteOnBoard = mutableMapOf<PieceType, Int>()
        val blackOnBoard = mutableMapOf<PieceType, Int>()
        var whiteMaterial = 0
        var blackMaterial = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c] ?: continue
                if (p.color == PieceColor.WHITE) {
                    whiteOnBoard[p.type] = (whiteOnBoard[p.type] ?: 0) + 1
                    whiteMaterial += p.type.materialValue
                } else {
                    blackOnBoard[p.type] = (blackOnBoard[p.type] ?: 0) + 1
                    blackMaterial += p.type.materialValue
                }
            }
        }

        val capturedByWhite = mutableListOf<ChessPiece>() // Missing black pieces captured by White
        val capturedByBlack = mutableListOf<ChessPiece>() // Missing white pieces captured by Black

        var whitePromotedCount = 0
        var blackPromotedCount = 0

        for ((type, initialCount) in nonPawnStartCounts) {
            val bCount = blackOnBoard[type] ?: 0
            if (bCount > initialCount) {
                blackPromotedCount += (bCount - initialCount)
            } else {
                val missingBlack = initialCount - bCount
                repeat(missingBlack) {
                    capturedByWhite.add(ChessPiece(type, PieceColor.BLACK))
                }
            }

            val wCount = whiteOnBoard[type] ?: 0
            if (wCount > initialCount) {
                whitePromotedCount += (wCount - initialCount)
            } else {
                val missingWhite = initialCount - wCount
                repeat(missingWhite) {
                    capturedByBlack.add(ChessPiece(type, PieceColor.WHITE))
                }
            }
        }

        val missingBlackPawns = (8 - (blackOnBoard[PieceType.PAWN] ?: 0) - blackPromotedCount).coerceAtLeast(0)
        repeat(missingBlackPawns) {
            capturedByWhite.add(ChessPiece(PieceType.PAWN, PieceColor.BLACK))
        }

        val missingWhitePawns = (8 - (whiteOnBoard[PieceType.PAWN] ?: 0) - whitePromotedCount).coerceAtLeast(0)
        repeat(missingWhitePawns) {
            capturedByBlack.add(ChessPiece(PieceType.PAWN, PieceColor.WHITE))
        }

        return Triple(capturedByWhite, capturedByBlack, whiteMaterial - blackMaterial)
    }

    /**
     * Tactical Bot Evaluator for Solo Bot Mode and Move Hint suggestions.
     */
    fun chooseBestBotMove(state: ChessBoardState): ChessMove? {
        val legalMoves = getLegalMoves(state)
        if (legalMoves.isEmpty()) return null

        var bestMove = legalMoves.first()
        var bestScore = Int.MIN_VALUE

        for (move in legalMoves) {
            var score = 0
            // 1. Material capture gain (MVV-LVA)
            if (move.capturedPiece != null) {
                score += move.capturedPiece.type.materialValue * 120 - move.piece.type.materialValue * 10
            }
            // 2. Promotion bonus
            if (move.promotion != null) {
                score += move.promotion.materialValue * 100
            }
            // 3. Center control bonus
            val centerDist = abs(3.5 - move.to.row) + abs(3.5 - move.to.col)
            score += ((7.0 - centerDist) * 8).toInt()

            // 4. Castling safety bonus
            if (move.isCastlingKingside || move.isCastlingQueenside) {
                score += 65
            }

            // 5. Evaluate resulting board for checkmate, check, and hanging piece risk
            val nextState = applyMove(state, move)
            if (nextState.status == GameStatus.CHECKMATE) {
                return move
            }
            if (nextState.isCheck) {
                score += 45
            }
            // Penalize moving onto a square attacked by opponent unless profitable trade
            if (isSquareAttacked(nextState, move.to, state.turn.opposite)) {
                score -= move.piece.type.materialValue * 75
            }

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }
}

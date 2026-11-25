package com.example.engine

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Serializable
enum class AiDifficulty(
    val dbKey: String,
    val title: String,
    val eloRating: Int,
    val searchDepth: Int,
    val description: String,
    val avatarColorHex: String
) {
    NOVICE(
        dbKey = "ai_novice",
        title = "Novice Bot",
        eloRating = 850,
        searchDepth = 1,
        description = "Casual 1-ply lookahead with occasional forgiving moves.",
        avatarColorHex = "#38BDF8"
    ),
    CLUB(
        dbKey = "ai_club",
        title = "Club Tactician",
        eloRating = 1300,
        searchDepth = 2,
        description = "2-ply Alpha-Beta search with center control & tactical captures.",
        avatarColorHex = "#10B981"
    ),
    MASTER(
        dbKey = "ai_master",
        title = "Master Engine",
        eloRating = 1700,
        searchDepth = 3,
        description = "3-ply Alpha-Beta pruning with Piece-Square Tables & king safety.",
        avatarColorHex = "#F59E0B"
    ),
    GRANDMASTER(
        dbKey = "ai_grandmaster",
        title = "Grandmaster AI",
        eloRating = 2100,
        searchDepth = 3,
        description = "Deep Alpha-Beta + Quiescence capture extensions & endgame heuristics.",
        avatarColorHex = "#A855F7"
    );

    companion object {
        fun fromDbKey(key: String?): AiDifficulty {
            return entries.firstOrNull { it.dbKey == key } ?: CLUB
        }
    }
}

/**
 * Pure-Kotlin Chess AI Engine implementing Minimax with Alpha-Beta Pruning,
 * MVV-LVA (Most Valuable Victim - Least Valuable Attacker) move ordering,
 * Quiescence tactical capture search, and Grandmaster Piece-Square Tables (PST).
 */
object ChessAiEngine {

    private const val MATE_SCORE = 100_000
    private const val QUIESCENCE_MAX_DEPTH = 2

    // Piece-Square Tables from White's perspective (row 0 = rank 8, row 7 = rank 1)
    private val PAWN_PST = arrayOf(
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(12, 12, 22, 32, 32, 22, 12, 12),
        intArrayOf(6,  6, 12, 26, 26, 12,  6,  6),
        intArrayOf(0,  0,  4, 22, 22,  4,  0,  0),
        intArrayOf(5, -4, -8,  0,  0, -8, -4,  5),
        intArrayOf(5, 10, 10,-20,-20, 10, 10,  5),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val KNIGHT_PST = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 12, 16, 16, 12,  0,-30),
        intArrayOf(-30,  6, 16, 22, 22, 16,  6,-30),
        intArrayOf(-30,  0, 16, 22, 22, 16,  0,-30),
        intArrayOf(-30,  6, 12, 16, 16, 12,  6,-30),
        intArrayOf(-40,-20,  0,  6,  6,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )

    private val BISHOP_PST = arrayOf(
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  8, 12, 12,  8,  0,-10),
        intArrayOf(-10,  6,  6, 12, 12,  6,  6,-10),
        intArrayOf(-10,  0, 12, 12, 12, 12,  0,-10),
        intArrayOf(-10, 12, 12, 12, 12, 12, 12,-10),
        intArrayOf(-10,  6,  0,  0,  0,  0,  6,-10),
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20)
    )

    private val ROOK_PST = arrayOf(
        intArrayOf(0,  0,  0,  4,  4,  0,  0,  0),
        intArrayOf(8, 14, 14, 14, 14, 14, 14,  8),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(0,  0,  0,  8,  8,  0,  0,  0)
    )

    private val QUEEN_PST = arrayOf(
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  6,  6,  6,  6,  0,-10),
        intArrayOf(-5,   0,  6,  8,  8,  6,  0, -5),
        intArrayOf(0,    0,  6,  8,  8,  6,  0, -5),
        intArrayOf(-10,  6,  6,  6,  6,  6,  0,-10),
        intArrayOf(-10,  0,  6,  0,  0,  0,  0,-10),
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20)
    )

    private val KING_MIDGAME_PST = arrayOf(
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-20,-30,-30,-40,-40,-30,-30,-20),
        intArrayOf(-10,-20,-20,-20,-20,-20,-20,-10),
        intArrayOf(20,  20,  0,  0,  0,  0, 20, 20),
        intArrayOf(20,  30, 12,  0,  0, 12, 30, 20)
    )

    /**
     * Selects the best legal move for the active side given the selected [AiDifficulty].
     */
    fun computeAiMove(
        state: ChessBoardState,
        difficulty: AiDifficulty
    ): ChessMove? {
        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) return null
        if (legalMoves.size == 1) return legalMoves.first()

        val orderedMoves = orderMovesForPruning(legalMoves)

        // Novice mode: 1-ply evaluation with slight randomness so beginners can win
        if (difficulty == AiDifficulty.NOVICE) {
            val scored = orderedMoves.map { move ->
                val next = ChessEngine.applyMove(state, move)
                if (next.status == GameStatus.CHECKMATE) return move
                val eval = evaluateBoardForColor(next, state.turn) + Random.nextInt(-65, 65)
                move to eval
            }.sortedByDescending { it.second }
            return scored.first().first
        }

        val useQuiescence = difficulty == AiDifficulty.GRANDMASTER
        val rootColor = state.turn
        var bestMove: ChessMove = orderedMoves.first()
        var bestScore = Int.MIN_VALUE
        var alpha = -MATE_SCORE * 2
        val beta = MATE_SCORE * 2

        for (move in orderedMoves) {
            val nextState = ChessEngine.applyMove(state, move)
            if (nextState.status == GameStatus.CHECKMATE) {
                return move
            }

            val score = -alphaBeta(
                state = nextState,
                depth = difficulty.searchDepth - 1,
                alpha = -beta,
                beta = -alpha,
                perspective = rootColor.opposite,
                useQuiescence = useQuiescence
            )

            // Add tiny tie-breaking jitter among equally strong moves
            val jitter = if (difficulty == AiDifficulty.CLUB) Random.nextInt(-8, 9) else Random.nextInt(-2, 3)
            val totalScore = score + jitter

            if (totalScore > bestScore) {
                bestScore = totalScore
                bestMove = move
            }
            if (score > alpha) {
                alpha = score
            }
        }

        return bestMove
    }

    private fun alphaBeta(
        state: ChessBoardState,
        depth: Int,
        alpha: Int,
        beta: Int,
        perspective: PieceColor,
        useQuiescence: Boolean
    ): Int {
        if (state.status == GameStatus.CHECKMATE) {
            // Current player to move is checkmated; prefer faster mates by adding depth
            return -(MATE_SCORE + depth * 100)
        }
        if (state.status == GameStatus.STALEMATE || state.status == GameStatus.DRAW_FIFTY_MOVE) {
            return 0
        }
        if (depth <= 0) {
            return if (useQuiescence) {
                quiescenceSearch(state, alpha, beta, perspective, QUIESCENCE_MAX_DEPTH)
            } else {
                evaluateBoardForColor(state, perspective)
            }
        }

        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) {
            return if (state.isCheck) -(MATE_SCORE + depth * 100) else 0
        }

        val ordered = orderMovesForPruning(legalMoves)
        var currentAlpha = alpha
        var maxEval = -MATE_SCORE * 2

        for (move in ordered) {
            val nextState = ChessEngine.applyMove(state, move)
            val eval = -alphaBeta(
                state = nextState,
                depth = depth - 1,
                alpha = -beta,
                beta = -currentAlpha,
                perspective = perspective.opposite,
                useQuiescence = useQuiescence
            )
            maxEval = max(maxEval, eval)
            currentAlpha = max(currentAlpha, eval)
            if (currentAlpha >= beta) {
                break // Beta cutoff
            }
        }
        return maxEval
    }

    /**
     * Quiescence Search: Evaluates tactical capture/promotion sequences at leaf nodes
     * so the Grandmaster AI never falls for horizon-effect blunder trades.
     */
    private fun quiescenceSearch(
        state: ChessBoardState,
        alpha: Int,
        beta: Int,
        perspective: PieceColor,
        qDepth: Int
    ): Int {
        val standPat = evaluateBoardForColor(state, perspective)
        if (qDepth <= 0) return standPat
        if (standPat >= beta) return beta

        var currentAlpha = max(alpha, standPat)
        val tacticalMoves = ChessEngine.getLegalMoves(state)
            .filter { it.isCapture || it.promotion != null }

        if (tacticalMoves.isEmpty()) return standPat

        for (move in orderMovesForPruning(tacticalMoves)) {
            val nextState = ChessEngine.applyMove(state, move)
            val score = -quiescenceSearch(
                state = nextState,
                alpha = -beta,
                beta = -currentAlpha,
                perspective = perspective.opposite,
                qDepth = qDepth - 1
            )
            if (score >= beta) return beta
            if (score > currentAlpha) {
                currentAlpha = score
            }
        }
        return currentAlpha
    }

    private fun orderMovesForPruning(moves: List<ChessMove>): List<ChessMove> {
        return moves.sortedByDescending { move ->
            var priority = 0
            if (move.capturedPiece != null) {
                priority += 1000 + move.capturedPiece.type.materialValue * 100 - move.piece.type.materialValue * 10
            }
            if (move.promotion != null) {
                priority += 900 + move.promotion.materialValue * 80
            }
            if (move.isCastlingKingside || move.isCastlingQueenside) {
                priority += 250
            }
            if (move.san.endsWith("#")) {
                priority += 50_000
            } else if (move.san.endsWith("+")) {
                priority += 400
            }
            priority
        }
    }

    /**
     * Static board evaluation in centipawns from [perspective]'s viewpoint.
     */
    fun evaluateBoardForColor(state: ChessBoardState, perspective: PieceColor): Int {
        when (state.status) {
            GameStatus.CHECKMATE -> {
                return if (state.turn == perspective) -MATE_SCORE else MATE_SCORE
            }
            GameStatus.STALEMATE, GameStatus.DRAW_FIFTY_MOVE, GameStatus.DRAW_AGREEMENT -> return 0
            else -> {}
        }

        var whiteScore = 0
        var blackScore = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.grid[r][c] ?: continue
                val baseVal = when (piece.type) {
                    PieceType.PAWN -> 100
                    PieceType.KNIGHT -> 320
                    PieceType.BISHOP -> 330
                    PieceType.ROOK -> 500
                    PieceType.QUEEN -> 900
                    PieceType.KING -> 20_000
                }
                val pstRow = if (piece.color == PieceColor.WHITE) r else 7 - r
                val pstBonus = when (piece.type) {
                    PieceType.PAWN -> PAWN_PST[pstRow][c]
                    PieceType.KNIGHT -> KNIGHT_PST[pstRow][c]
                    PieceType.BISHOP -> BISHOP_PST[pstRow][c]
                    PieceType.ROOK -> ROOK_PST[pstRow][c]
                    PieceType.QUEEN -> QUEEN_PST[pstRow][c]
                    PieceType.KING -> KING_MIDGAME_PST[pstRow][c]
                }

                if (piece.color == PieceColor.WHITE) {
                    whiteScore += baseVal + pstBonus
                } else {
                    blackScore += baseVal + pstBonus
                }
            }
        }

        // Check bonus
        if (state.isCheck) {
            if (state.turn == PieceColor.WHITE) {
                blackScore += 35
            } else {
                whiteScore += 35
            }
        }

        val rawDiff = whiteScore - blackScore
        return if (perspective == PieceColor.WHITE) rawDiff else -rawDiff
    }
}

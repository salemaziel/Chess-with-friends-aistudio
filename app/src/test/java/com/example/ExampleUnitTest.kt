package com.example

import com.example.data.ChessRepository
import com.example.data.TimeControlOption
import com.example.engine.AiDifficulty
import com.example.engine.BoardSquare
import com.example.engine.ChessAiEngine
import com.example.engine.ChessEngine
import com.example.engine.GameStatus
import com.example.engine.PieceColor
import com.example.engine.PieceType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun initialFen_has20LegalOpeningMovesAndBeginnerRoutes() {
        val state = ChessEngine.parseFen(ChessEngine.INITIAL_FEN)
        assertEquals(PieceColor.WHITE, state.turn)
        assertEquals(GameStatus.ACTIVE, state.status)

        val moves = ChessEngine.getLegalMoves(state)
        assertEquals(20, moves.size)

        // Check Beginner Mode routes for e2 pawn
        val e2 = BoardSquare.fromAlgebraic("e2")!!
        val routes = ChessEngine.computeBeginnerRoutes(state, e2)
        assertEquals(2, routes.size)
    }

    @Test
    fun scholarsMate_detectsCheckmateCorrectly() {
        val mateFen = "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4"
        val state = ChessEngine.parseFen(mateFen)
        assertEquals(GameStatus.CHECKMATE, state.status)
        assertTrue(state.isCheck)
        assertTrue(ChessEngine.getLegalMoves(state).isEmpty())
    }

    @Test
    fun enPassantAndCastling_areGeneratedAndAppliedProperly() {
        val epFen = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3"
        val epState = ChessEngine.parseFen(epFen)
        val e5 = BoardSquare.fromAlgebraic("e5")!!
        val f6 = BoardSquare.fromAlgebraic("f6")!!
        val epMoves = ChessEngine.getLegalMovesForSquare(epState, e5)
        val epCapture = epMoves.firstOrNull { it.to == f6 && it.isEnPassant }
        assertNotNull(epCapture)

        val afterEp = ChessEngine.applyMove(epState, epCapture!!)
        assertEquals(PieceType.PAWN, afterEp.pieceAt(f6)?.type)
        assertEquals(null, afterEp.pieceAt(BoardSquare.fromAlgebraic("f5")!!))
    }

    @Test
    fun chessAiEngine_computesValidMovesAcrossAllDifficulties() {
        val midgameFen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 1 5"
        val state = ChessEngine.parseFen(midgameFen)

        for (difficulty in AiDifficulty.entries) {
            val aiMove = ChessAiEngine.computeAiMove(state, difficulty)
            assertNotNull("AI move should not be null for $difficulty", aiMove)
            val nextState = ChessEngine.applyMove(state, aiMove!!)
            assertEquals(PieceColor.BLACK, nextState.turn)
        }
    }

    @Test
    fun acceptInvite_usernameInvite_shareLink_andQrCode_addNewMatchToMatchesScreen() = runBlocking {
        val repo = ChessRepository()
        val initialCount = repo.matches.value.size

        // 1. Accept pending invite from notifications ("fr_3" -> Sora Takahashi)
        val acceptedMatch = repo.acceptFriendInvite("fr_3")
        assertNotNull(acceptedMatch)
        assertEquals("Sora Takahashi", acceptedMatch!!.opponentUsername)
        assertEquals(initialCount + 1, repo.matches.value.size)
        assertEquals(acceptedMatch.record.id, repo.matches.value.first().record.id)
        assertFalse(repo.friends.value.first { it.friendshipId == "fr_3" }.isPendingIncoming)

        // 2. Invite by @username
        val usernameMatch = repo.inviteByUsernameAndStartMatch(
            rawUsername = "@hikaru_n",
            timeControl = TimeControlOption.RAPID_10_0,
            playAsWhite = true
        )
        assertEquals("Hikaru N", usernameMatch.opponentUsername)
        assertEquals(initialCount + 2, repo.matches.value.size)

        // 3. Start match via Share Link
        val linkMatch = repo.joinOrStartMatchFromInviteLinkOrCode(
            rawLinkOrCode = "https://chesswithfriends.app/join/CWF-904?host=nadia_k",
            timeControl = TimeControlOption.RAPID_10_0
        )
        assertEquals("Nadia K", linkMatch.opponentUsername)
        assertEquals(initialCount + 3, repo.matches.value.size)

        // 4. Start match via QR code payload
        val qrMatch = repo.joinOrStartMatchFromInviteLinkOrCode(
            rawLinkOrCode = "https://chesswithfriends.app/join/QR-772?host=viktor_korchnoi",
            timeControl = TimeControlOption.RAPID_10_0
        )
        assertEquals("Viktor Korchnoi", qrMatch.opponentUsername)
        assertEquals(initialCount + 4, repo.matches.value.size)
    }

    @Test
    fun capturedPieces_dynamicallyUpdateFromChessEngineAndGroupCorrectly() {
        // Start from initial board: 0 captures
        val initial = ChessEngine.parseFen(ChessEngine.INITIAL_FEN)
        assertTrue(initial.capturedByWhite.isEmpty())
        assertTrue(initial.capturedByBlack.isEmpty())
        assertEquals(0, initial.materialAdvantageWhite)

        // Italian tactical FEN where White captured a Black Knight + Black Pawn, and Black captured a White Pawn
        val tacticalFen = "r1bqk2r/ppp2ppp/2n5/2b1p3/2B1P3/3P1N2/PPP3PP/RNBQK2R w KQkq - 0 6"
        val midgame = ChessEngine.parseFen(tacticalFen)
        assertEquals(2, midgame.capturedByWhite.size) // 1 Black Knight + 1 Black Pawn
        assertEquals(1, midgame.capturedByBlack.size) // 1 White Pawn
        assertEquals(3, midgame.materialAdvantageWhite)

        // Now execute a capture move: White plays Nxe5 (f3 -> e5) capturing Black's e5 pawn
        val f3 = BoardSquare.fromAlgebraic("f3")!!
        val e5 = BoardSquare.fromAlgebraic("e5")!!
        val captureMove = ChessEngine.getLegalMovesForSquare(midgame, f3).first { it.to == e5 }
        val afterCapture = ChessEngine.applyMove(midgame, captureMove)

        assertEquals(3, afterCapture.capturedByWhite.size) // 1 Black Knight + 2 Black Pawns
        assertEquals(4, afterCapture.materialAdvantageWhite)
        val grouped = com.example.ui.components.groupCapturedPieces(afterCapture.capturedByWhite)
        assertEquals(2, grouped.size)
        assertEquals(PieceType.KNIGHT, grouped[0].type)
        assertEquals(1, grouped[0].count)
        assertEquals(PieceType.PAWN, grouped[1].type)
        assertEquals(2, grouped[1].count)
    }
}

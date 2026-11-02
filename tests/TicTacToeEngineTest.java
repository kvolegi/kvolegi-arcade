package tests;

import arcade.App;
import arcade.TicTacToe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tests.ArcadeDomainTest.HeadlessRecordingGfx;
import tests.ArcadeDomainTest.InMemoryStore;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("kvolegi arcade - TicTacToe Engine Specification Tests")
public class TicTacToeEngineTest {

    private InMemoryStore store;
    private App app;
    private TicTacToe ttt;
    private HeadlessRecordingGfx gfx;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        app = new App(store);
        ttt = (TicTacToe) app.games.get(0);
        gfx = new HeadlessRecordingGfx();
    }

    @Test
    @DisplayName("TTT: Initial round state, board dimension & turn X")
    void testInitialRoundState() {
        ttt.start();
        assertFalse(ttt.isEnded(), "Game is not ended initially");
        assertEquals(0, ttt.getWinner(), "No winner initially");
        assertEquals(1, ttt.getTurn(), "Turn starts with player 1 (X)");
        assertEquals(5, ttt.getBoardSize(), "Initial board size is 5x5");
    }

    @Test
    @DisplayName("TTT: Turn toggles alternately between X (1) and O (2) in 2-player mode")
    void testTurnAlternation() {
        ttt.getOpt("players").sel = 1; // 2 players
        ttt.start();

        assertEquals(1, ttt.getTurn(), "First turn is X (1)");
        ttt.clickCell(0, 0);
        assertEquals(1, ttt.getCell(0, 0), "Cell (0,0) occupied by X");
        assertEquals(2, ttt.getTurn(), "Turn flipped to O (2)");

        ttt.clickCell(0, 1);
        assertEquals(2, ttt.getCell(0, 1), "Cell (0,1) occupied by O");
        assertEquals(1, ttt.getTurn(), "Turn flipped back to X (1)");
    }

    @Test
    @DisplayName("TTT: Clicking an already occupied cell is rejected")
    void testOccupiedCellClickRejected() {
        ttt.getOpt("players").sel = 1; // 2 players
        ttt.start();

        ttt.clickCell(2, 2);
        assertEquals(1, ttt.getCell(2, 2), "Cell (2,2) marked by X");
        assertEquals(2, ttt.getTurn(), "Turn is O");

        ttt.clickCell(2, 2); // Try clicking same cell
        assertEquals(1, ttt.getCell(2, 2), "Cell (2,2) remains X (not overwritten)");
        assertEquals(2, ttt.getTurn(), "Turn remains O (not consumed by invalid move)");
    }

    @Test
    @DisplayName("TTT: Gomoku 5-in-a-row horizontal win detection")
    void testHorizontal5Win() {
        int[][] b = new int[15][15];
        for (int c = 2; c <= 6; c++) b[4][c] = 1;
        assertTrue(ttt.checkWin(b, 4, 4, 1, 5), "Horizontal line of 5 triggers win");
    }

    @Test
    @DisplayName("TTT: Gomoku 5-in-a-row vertical win detection")
    void testVertical5Win() {
        int[][] b = new int[15][15];
        for (int r = 3; r <= 7; r++) b[r][5] = 2;
        assertTrue(ttt.checkWin(b, 5, 5, 2, 5), "Vertical line of 5 triggers win");
    }

    @Test
    @DisplayName("TTT: Gomoku 5-in-a-row main diagonal win detection")
    void testMainDiagonal5Win() {
        int[][] b = new int[15][15];
        for (int i = 0; i < 5; i++) b[2 + i][2 + i] = 1;
        assertTrue(ttt.checkWin(b, 4, 4, 1, 5), "Main diagonal line of 5 triggers win");
    }

    @Test
    @DisplayName("TTT: Gomoku 5-in-a-row anti-diagonal win detection")
    void testAntiDiagonal5Win() {
        int[][] b = new int[15][15];
        for (int i = 0; i < 5; i++) b[2 + i][8 - i] = 2;
        assertTrue(ttt.checkWin(b, 4, 6, 2, 5), "Anti-diagonal line of 5 triggers win");
    }

    @Test
    @DisplayName("TTT: Line of 4 is strictly insufficient for K=5 victory")
    void testLineOf4Insufficient() {
        int[][] b = new int[15][15];
        for (int c = 1; c <= 4; c++) b[3][c] = 1;
        assertFalse(ttt.checkWin(b, 3, 2, 1, 5), "Line of 4 does NOT win in 5-in-a-row");
    }

    @Test
    @DisplayName("TTT: Real play execution detects in-game win")
    void testInGameWinExecution() {
        ttt.getOpt("players").sel = 1; // 2 players
        ttt.start();

        // X plays (0,0), O plays (1,0)...
        ttt.clickCell(0, 0); // X
        ttt.clickCell(1, 0); // O
        ttt.clickCell(0, 1); // X
        ttt.clickCell(1, 1); // O
        ttt.clickCell(0, 2); // X
        ttt.clickCell(1, 2); // O
        ttt.clickCell(0, 3); // X
        ttt.clickCell(1, 3); // O
        ttt.clickCell(0, 4); // X (5th in a row horizontally!)

        assertTrue(ttt.isEnded(), "Game marked as ended");
        assertEquals(1, ttt.getWinner(), "X is declared the winner");
        assertEquals(1, ttt.getWinsX(), "X win count incremented");
    }

    @Test
    @DisplayName("TTT: Best text formatted properly with wins and streaks")
    void testBestTextFormatting() {
        store.putInt("ttt.wins", 12);
        store.putInt("ttt.best", 7);
        ttt.reload();

        String best = ttt.bestText();
        assertTrue(best.contains("wins 12"), "Best text displays wins count");
        assertTrue(best.contains("streak 7"), "Best text displays streak count");
    }

    @Test
    @DisplayName("TTT: Icon renders vector geometry without crashing")
    void testIconRendering() {
        ttt.icon(gfx, 120, 120, 64);
        assertTrue(gfx.drawCalls > 0, "Icon draws vector shapes");
    }

    @Test
    @DisplayName("TTT: Frame loop handles active rendering")
    void testFrameLoopTicking() {
        ttt.start();
        int initialCalls = gfx.drawCalls;
        ttt.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > initialCalls, "Frame loop renders board and UI elements");
    }
}

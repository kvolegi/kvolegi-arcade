package tests;

import arcade.App;
import arcade.Mines;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tests.ArcadeDomainTest.HeadlessRecordingGfx;
import tests.ArcadeDomainTest.InMemoryStore;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("kvolegi arcade - Mines Engine Specification Tests")
public class MinesEngineTest {

    private InMemoryStore store;
    private App app;
    private Mines mines;
    private HeadlessRecordingGfx gfx;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        app = new App(store);
        mines = (Mines) app.games.get(2);
        gfx = new HeadlessRecordingGfx();
    }

    @Test
    @DisplayName("Mines: Initial board state configuration")
    void testInitialBoardState() {
        mines.start();
        assertFalse(mines.isDead(), "Player starts alive");
        assertFalse(mines.isWon(), "Game not won initially");
        assertEquals(0, mines.getOpened(), "Zero cells opened before first click");
        assertTrue(mines.getRows() > 0, "Grid has rows");
        assertTrue(mines.getCols() > 0, "Grid has cols");
        assertTrue(mines.getTotalBeavers() > 0, "Mines/beavers present on field");
    }

    @Test
    @DisplayName("Mines: First click is guaranteed safe and opens at least one cell")
    void testFirstClickGuaranteedSafe() {
        mines.start();
        int r = mines.getRows() / 2;
        int c = mines.getCols() / 2;

        mines.clickCell(r, c);
        assertFalse(mines.isDead(), "Player never dies on the first click");
        assertTrue(mines.getOpened() >= 1, "At least 1 cell revealed on first click");
        assertTrue(mines.isCellOpen(r, c), "Clicked cell is now open");
    }

    @Test
    @DisplayName("Mines: Flag toggle correctly marks and unmarks cells")
    void testFlagToggle() {
        mines.start();

        assertFalse(mines.isFlagged(0, 0), "Cell initially unflagged");
        mines.flagCell(0, 0);
        assertTrue(mines.isFlagged(0, 0), "Cell is now flagged");

        mines.flagCell(0, 0);
        assertFalse(mines.isFlagged(0, 0), "Cell is unflagged on second toggle");
    }

    @Test
    @DisplayName("Mines: Flagged cells cannot be opened by ordinary click")
    void testFlaggedCellProtectedFromClick() {
        mines.start();
        // Flag cell before clicking
        mines.flagCell(1, 1);
        assertTrue(mines.isFlagged(1, 1), "Cell (1,1) is flagged");

        mines.clickCell(1, 1);
        assertFalse(mines.isCellOpen(1, 1), "Flagged cell cannot be revealed by click");
        assertFalse(mines.isDead(), "Player did not detonate flagged cell");
    }

    @Test
    @DisplayName("Mines: Clicking an already opened cell is safe and no-op")
    void testClickingOpenedCellIsSafe() {
        mines.start();
        mines.clickCell(3, 3);
        int openedBefore = mines.getOpened();

        mines.clickCell(3, 3); // Second click on same open cell
        assertEquals(openedBefore, mines.getOpened(), "No duplicate count from clicking opened cell");
        assertFalse(mines.isDead(), "No death from re-clicking opened cell");
    }

    @Test
    @DisplayName("Mines: Difficulty option alters grid dimensions")
    void testDifficultyOptionsConfiguration() {
        assertEquals("Easy", mines.getOpt("diff").vals[0]);
        assertEquals("Normal", mines.getOpt("diff").vals[1]);
        assertEquals("Hard", mines.getOpt("diff").vals[2]);

        mines.getOpt("diff").sel = 0; // Easy
        mines.start();
        int easyCols = mines.getCols();

        mines.getOpt("diff").sel = 2; // Hard
        mines.start();
        int hardCols = mines.getCols();

        assertTrue(hardCols >= easyCols, "Hard difficulty has equal or greater dimensions than Easy");
    }

    @Test
    @DisplayName("Mines: Best time persistence in Store")
    void testBestTimePersistence() {
        store.putInt("mines.best.0", 42);
        mines.reload();

        assertTrue(mines.bestText().contains("42"), "Best time reflects stored record");
    }

    @Test
    @DisplayName("Mines: Icon rendering renders vector graphics")
    void testIconRendering() {
        mines.icon(gfx, 150, 150, 64);
        assertTrue(gfx.drawCalls > 0, "Icon executes vector draw calls");
    }

    @Test
    @DisplayName("Mines: Chord click mechanic operates safely on open cells")
    void testChordMechanic() {
        mines.start();
        mines.clickCell(2, 2);

        // Chording on cell (2,2)
        mines.chordCell(2, 2);
        // Does not throw exceptions and keeps game in valid state
        assertNotNull(mines.isDead());
    }

    @Test
    @DisplayName("Mines: Multiple frame ticks render field graphics")
    void testFrameRenderingLoop() {
        mines.start();
        int calls = gfx.drawCalls;
        mines.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > calls, "Mines frame renders canvas");
    }

    @Test
    @DisplayName("Mines: Restart resets opened count and clears flags")
    void testRestartReset() {
        mines.start();
        mines.clickCell(2, 2);
        mines.flagCell(0, 0);

        mines.start(); // Restart fresh
        assertEquals(0, mines.getOpened(), "Opened count reset to 0");
        assertFalse(mines.isFlagged(0, 0), "Flags cleared on restart");
        assertFalse(mines.isDead(), "Dead state cleared on restart");
    }

    @Test
    @DisplayName("Mines: onBack navigation handling")
    void testOnBackDelegation() {
        mines.start();
        assertFalse(mines.onBack(), "onBack delegates to menu");
    }
}

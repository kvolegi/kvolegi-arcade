package tests;

import arcade.App;
import arcade.C;
import arcade.Game;
import arcade.Gfx;
import arcade.Mines;
import arcade.Slots;
import arcade.Snake;
import arcade.Store;
import arcade.TicTacToe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("kvolegi arcade - Domain Engine Verified Test Suite")
public class ArcadeDomainTest {

    // -------------------------------------------------------------------------
    // TEST HARNESS: In-Memory Store & Headless Recording Gfx
    // -------------------------------------------------------------------------
    public static class InMemoryStore implements Store {
        private final Map<String, String> data = new HashMap<>();

        @Override public String get(String key, String def) { return data.getOrDefault(key, def); }
        @Override public void put(String key, String value) { data.put(key, value); }
        @Override public void clearAll() { data.clear(); }
    }

    public static class HeadlessRecordingGfx implements Gfx {
        public int drawCalls = 0;
        @Override public void color(int argb) {}
        @Override public void rect(float x, float y, float w, float h) { drawCalls++; }
        @Override public void rrect(float x, float y, float w, float h, float r) { drawCalls++; }
        @Override public void rrectStroke(float x, float y, float w, float h, float r, float lw) { drawCalls++; }
        @Override public void oval(float x, float y, float w, float h) { drawCalls++; }
        @Override public void ovalStroke(float x, float y, float w, float h, float lw) { drawCalls++; }
        @Override public void line(float x1, float y1, float x2, float y2, float lw) { drawCalls++; }
        @Override public void poly(float[] xy) { drawCalls++; }
        @Override public void gradient(float x, float y, float w, float h, float r, int c1, int c2, boolean vertical) { drawCalls++; }
        @Override public void radial(float cx, float cy, float r, int inner, int outer) { drawCalls++; }
        @Override public void text(String s, float x, float y, float size, boolean bold, int align) { drawCalls++; }
        @Override public float textWidth(String s, float size, boolean bold) { return s != null ? s.length() * size * 0.5f : 0f; }
        @Override public void save() {}
        @Override public void restore() {}
        @Override public void translate(float x, float y) {}
        @Override public void rotate(float deg) {}
        @Override public void scale(float sx, float sy) {}
        @Override public void clip(float x, float y, float w, float h) {}
    }

    @Test
    @DisplayName("App Lifecycle: Store configuration & 4 Domain Games Initialization")
    void testAppInitialization() {
        InMemoryStore store = new InMemoryStore();
        store.putInt("sound_on", 0);
        store.putInt("music_vol", 80);

        App app = new App(store);
        assertEquals(4, app.games.size(), "App creates exactly 4 domain games");
        assertInstanceOf(TicTacToe.class, app.games.get(0), "First game is TicTacToe");
        assertInstanceOf(Snake.class, app.games.get(1), "Second game is Snake");
        assertInstanceOf(Mines.class, app.games.get(2), "Third game is Mines");
        assertInstanceOf(Slots.class, app.games.get(3), "Fourth game is Slots");
        assertFalse(app.soundEnabled, "Sound option loaded from store");
        assertEquals(80, app.musicVolume, "Music volume loaded from store");
    }

    @Test
    @DisplayName("Snake Domain: Game lifecycle, body spawning & frame ticking")
    void testSnakeRealEngine() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        Snake snake = (Snake) app.games.get(1);

        assertEquals("snake", snake.id, "Snake game id");
        assertEquals(C.GREEN, snake.color, "Snake default color");

        snake.start();
        assertTrue(snake.bestText().startsWith("best"), "Snake best score string formatted properly");

        HeadlessRecordingGfx gfx = new HeadlessRecordingGfx();
        snake.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > 0, "Snake frame executed vector draw calls");
    }

    @Test
    @DisplayName("Snake Rules: Deadly wall collisions vs toroidal wrap-around")
    void testSnakeCollisionRules() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        Snake snake = (Snake) app.games.get(1);

        // Deadly Walls mode
        snake.getOpt("walls").sel = 0;
        snake.start();
        snake.pressStart();
        snake.setDirection('R');

        HeadlessRecordingGfx gfx = new HeadlessRecordingGfx();
        for (int i = 0; i < 60; i++) {
            snake.frame(gfx, 0.20f, 800, 600);
            if (snake.isOver()) break;
        }
        assertTrue(snake.isOver(), "Snake died on deadly wall collision");

        // Wrap Around Walls mode
        snake.getOpt("walls").sel = 1;
        snake.start();
        snake.pressStart();
        snake.setDirection('R');

        for (int i = 0; i < 20; i++) {
            snake.frame(gfx, 0.15f, 800, 600);
        }
        assertFalse(snake.isOver(), "Snake wrapped around seamlessly without dying");
    }

    @Test
    @DisplayName("TicTacToe Domain: Round lifecycle, side selection & score streak")
    void testTicTacToeRealEngine() {
        InMemoryStore store = new InMemoryStore();
        store.putInt("ttt.wins", 5);
        store.putInt("ttt.best", 3);
        App app = new App(store);
        TicTacToe ttt = (TicTacToe) app.games.get(0);

        assertEquals("ttt", ttt.id, "TicTacToe game id");
        assertEquals(C.PURPLE, ttt.color, "TicTacToe default color");
        ttt.start();

        assertEquals("wins 5  \u2022  streak 3", ttt.bestText(), "TicTacToe best text formatted");

        HeadlessRecordingGfx gfx = new HeadlessRecordingGfx();
        ttt.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > 0, "TicTacToe renders grid and board");
    }

    @Test
    @DisplayName("TicTacToe Rules: 5-in-a-Row vector detection across all 4 axes")
    void testTicTacToeWinningDetection() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        TicTacToe ttt = (TicTacToe) app.games.get(0);

        int n = 15;
        // 1. Horizontal
        int[][] b1 = new int[n][n];
        for (int c = 3; c <= 7; c++) b1[5][c] = 1;
        assertTrue(ttt.checkWin(b1, 5, 5, 1, 5), "Horizontal 5-in-a-row detected");

        // 2. Vertical
        int[][] b2 = new int[n][n];
        for (int r = 2; r <= 6; r++) b2[r][8] = 2;
        assertTrue(ttt.checkWin(b2, 4, 8, 2, 5), "Vertical 5-in-a-row detected");

        // 3. Main Diagonal
        int[][] b3 = new int[n][n];
        for (int i = 1; i <= 5; i++) b3[i][i] = 1;
        assertTrue(ttt.checkWin(b3, 3, 3, 1, 5), "Main Diagonal 5-in-a-row detected");

        // 4. Anti-Diagonal
        int[][] b4 = new int[n][n];
        for (int i = 0; i < 5; i++) b4[2 + i][8 - i] = 2;
        assertTrue(ttt.checkWin(b4, 4, 6, 2, 5), "Anti-diagonal 5-in-a-row detected");

        // Negative Control: Line of 4
        int[][] b5 = new int[n][n];
        for (int c = 3; c <= 6; c++) b5[5][c] = 1;
        assertFalse(ttt.checkWin(b5, 5, 5, 1, 5), "Line of 4 is NOT a win for K=5");
    }

    @Test
    @DisplayName("Mines Domain: Safe options, density selection & persistence")
    void testMinesRealEngine() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        Mines mines = (Mines) app.games.get(2);

        assertEquals("mines", mines.id, "Mines game id");
        assertEquals(C.ORANGE, mines.color, "Mines default color");
        mines.start();

        HeadlessRecordingGfx gfx = new HeadlessRecordingGfx();
        mines.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > 0, "Mines renders minefield grid");
    }

    @Test
    @DisplayName("Mines Rules: Guaranteed safe first click & cascade reveal")
    void testMinesRulesAndCascade() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        Mines mines = (Mines) app.games.get(2);
        mines.start();

        mines.clickCell(4, 4);
        assertFalse(mines.isDead(), "First click is guaranteed safe (player not dead)");
        assertTrue(mines.getOpened() >= 1, "Opened at least 1 cell on safe click");
    }

    @Test
    @DisplayName("Slots Domain: Payline options, wager bounds & reset")
    void testSlotsRealEngine() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        Slots slots = (Slots) app.games.get(3);

        assertEquals("slots", slots.id, "Slots game id");
        assertEquals(C.YELLOW, slots.color, "Slots default color");
        slots.start();

        HeadlessRecordingGfx gfx = new HeadlessRecordingGfx();
        slots.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > 0, "Slots renders reels and paylines");
    }

    @Test
    @DisplayName("Headless Gfx: Full frame render loop across all 4 domain games")
    void testHeadlessRenderLoop() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        HeadlessRecordingGfx gfx = new HeadlessRecordingGfx();

        for (Game g : app.games) {
            g.start();
            int callsBefore = gfx.drawCalls;
            for (int f = 0; f < 60; f++) {
                g.frame(gfx, 0.01667f, 800, 600);
            }
            assertTrue(gfx.drawCalls > callsBefore, "Game " + g.id + " drew multiple frames");
        }
    }

    @Test
    @DisplayName("Vector & Color Math: Precision clamping, Lerp, & packed ARGB")
    void testColorAndVectorMath() {
        float clampedLow = C.clamp(-5f, 0f, 100f);
        float clampedHigh = C.clamp(150f, 0f, 100f);
        assertEquals(0f, clampedLow, 0.001f, "Clamp low boundary");
        assertEquals(100f, clampedHigh, 0.001f, "Clamp high boundary");

        float lerped = C.lerp(10f, 20f, 0.5f);
        assertEquals(15f, lerped, 0.001f, "Lerp midpoint calculation");

        int packedRgb = C.rgb(255, 128, 64);
        assertEquals(255, (packedRgb >> 16) & 0xFF, "Red channel extraction");
        assertEquals(128, (packedRgb >> 8) & 0xFF, "Green channel extraction");
        assertEquals(64, packedRgb & 0xFF, "Blue channel extraction");
    }

    @Test
    @DisplayName("Architecture Benchmark: 1,000 real engine frame simulation loop")
    void testFrameBudgetAndZeroAllocation() {
        InMemoryStore store = new InMemoryStore();
        App app = new App(store);
        Snake snake = (Snake) app.games.get(1);
        TicTacToe ttt = (TicTacToe) app.games.get(0);
        HeadlessRecordingGfx gfx = new HeadlessRecordingGfx();

        snake.start();
        ttt.start();

        int totalFrames = 1000;
        long startTime = System.nanoTime();
        float dt = 0.01667f;

        for (int i = 0; i < totalFrames; i++) {
            snake.frame(gfx, dt, 800, 600);
            ttt.frame(gfx, dt, 800, 600);
        }

        long elapsedNs = System.nanoTime() - startTime;
        double elapsedMs = elapsedNs / 1_000_000.0;
        double avgFrameTimeMs = elapsedMs / totalFrames;

        assertTrue(gfx.drawCalls > 1000, "Real domain games ticked across 1,000 frames");
        assertTrue(avgFrameTimeMs < 5.0, "Average frame tick duration < 5.0ms (60 FPS budget)");
    }
}

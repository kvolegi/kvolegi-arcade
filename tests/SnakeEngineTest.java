package tests;

import arcade.App;
import arcade.C;
import arcade.Snake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tests.ArcadeDomainTest.HeadlessRecordingGfx;
import tests.ArcadeDomainTest.InMemoryStore;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("kvolegi arcade - Snake Engine Specification Tests")
public class SnakeEngineTest {

    private InMemoryStore store;
    private App app;
    private Snake snake;
    private HeadlessRecordingGfx gfx;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        app = new App(store);
        snake = (Snake) app.games.get(1);
        gfx = new HeadlessRecordingGfx();
    }

    @Test
    @DisplayName("Snake: Initial state is waiting with default length and score zero")
    void testInitialState() {
        snake.start();
        assertTrue(snake.isWaiting(), "Snake starts in waiting mode until started");
        assertFalse(snake.isOver(), "Snake is not game over initially");
        assertEquals(0, snake.getScore(), "Initial score is zero");
        assertTrue(snake.getBodyLength() >= 3, "Initial snake has at least 3 body segments");
        assertFalse(snake.isPaused(), "Initial game is not paused");
    }

    @Test
    @DisplayName("Snake: pressStart activates ticking movement")
    void testPressStartActivatesMovement() {
        snake.start();
        snake.pressStart();
        assertFalse(snake.isWaiting(), "Waiting is false after pressStart");

        snake.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > 0, "Draw calls executed during active movement");
        assertFalse(snake.isOver(), "Snake alive during initial frames");
    }

    @Test
    @DisplayName("Snake: Direction changes to Left, Right, Up, Down")
    void testDirectionChanges() {
        snake.start();
        snake.pressStart();

        snake.setDirection('U');
        snake.frame(gfx, 0.05f, 800, 600);

        snake.setDirection('R');
        snake.frame(gfx, 0.05f, 800, 600);

        snake.setDirection('D');
        snake.frame(gfx, 0.05f, 800, 600);

        assertFalse(snake.isOver(), "Snake safely navigated right angle turns");
    }

    @Test
    @DisplayName("Snake: Fatal walls mode causes death upon perimeter boundary impact")
    void testFatalWallsImpact() {
        snake.getOpt("walls").sel = 0; // Fatal Walls
        snake.start();
        snake.pressStart();
        snake.setDirection('R');

        // Simulate enough frames to guarantee wall collision
        for (int i = 0; i < 70; i++) {
            snake.frame(gfx, 0.15f, 800, 600);
            if (snake.isOver()) break;
        }

        assertTrue(snake.isOver(), "Snake died upon hitting fatal wall boundary");
    }

    @Test
    @DisplayName("Snake: Pass-Through mode allows seamless toroidal boundary wrap-around")
    void testPassThroughWallsWrapAround() {
        snake.getOpt("walls").sel = 1; // Pass-Through
        snake.start();
        snake.pressStart();
        snake.setDirection('R');

        // Simulate frames that would cross boundary
        for (int i = 0; i < 30; i++) {
            snake.frame(gfx, 0.10f, 800, 600);
        }

        assertFalse(snake.isOver(), "Snake wrapped through wall boundary without death");
    }

    @Test
    @DisplayName("Snake: Pause and resume lifecycle properly toggles state")
    void testPauseAndResumeLifecycle() {
        snake.start();
        snake.pressStart();

        snake.setPaused(true);
        assertTrue(snake.isPaused(), "Snake paused flag is true");

        snake.frame(gfx, 0.016f, 800, 600);
        assertTrue(snake.isPaused(), "Snake remains paused during frame");

        snake.setPaused(false);
        assertFalse(snake.isPaused(), "Snake resumes play seamlessly");
    }

    @Test
    @DisplayName("Snake: Speed levels configured correctly (Easy, Normal, Hard)")
    void testSpeedLevelsConfiguration() {
        assertEquals("Easy", snake.getOpt("diff").vals[0]);
        assertEquals("Normal", snake.getOpt("diff").vals[1]);
        assertEquals("Hard", snake.getOpt("diff").vals[2]);

        snake.getOpt("diff").sel = 2; // Hard
        snake.saveOpts();
        assertEquals(2, store.getInt("snake.opt.diff", 0), "Store persists speed level");
    }

    @Test
    @DisplayName("Snake: Best score persistence in Store")
    void testBestScorePersistence() {
        snake.start();
        String key = "snake.best." + snake.getOpt("diff").sel + snake.getOpt("mode").sel + snake.getOpt("walls").sel;
        store.putInt(key, 140);

        assertTrue(snake.bestText().contains("140"), "Best score text reflects store value: " + snake.bestText());
    }

    @Test
    @DisplayName("Snake: Frame rendering executes icon geometry")
    void testIconGeometryRendering() {
        snake.icon(gfx, 100, 100, 64);
        assertTrue(gfx.drawCalls > 0, "Icon draws vector geometry");
    }

    @Test
    @DisplayName("Snake: onBack handles escape key by pausing internally when playing")
    void testOnBackHandling() {
        snake.start();
        snake.pressStart();
        assertTrue(snake.onBack(), "Snake onBack pauses the active game internally");
        assertTrue(snake.isPaused(), "Snake paused after onBack triggered");
    }

    @Test
    @DisplayName("Snake: Game restart resets score and re-arms waiting state")
    void testGameRestart() {
        snake.start();
        snake.pressStart();
        for (int i = 0; i < 10; i++) snake.frame(gfx, 0.05f, 800, 600);

        snake.start(); // Restart
        assertTrue(snake.isWaiting(), "Restart re-arms waiting state");
        assertEquals(0, snake.getScore(), "Restart resets score to 0");
        assertFalse(snake.isOver(), "Restart resets game over flag");
    }

    @Test
    @DisplayName("Snake: Vector draw calls under rapid frame simulation")
    void testRapidFrameSimulation() {
        snake.start();
        snake.pressStart();

        int before = gfx.drawCalls;
        for (int i = 0; i < 120; i++) {
            snake.frame(gfx, 0.016f, 800, 600);
        }
        assertTrue(gfx.drawCalls > before + 100, "Continuous draw calls rendered");
    }
}

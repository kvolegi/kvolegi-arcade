package tests;

import arcade.App;
import arcade.C;
import arcade.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tests.ArcadeDomainTest.HeadlessRecordingGfx;
import tests.ArcadeDomainTest.InMemoryStore;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("kvolegi arcade - App Lifecycle, Navigation & Color Math Tests")
public class AppLifecycleAndMathTest {

    private InMemoryStore store;
    private App app;
    private HeadlessRecordingGfx gfx;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        app = new App(store);
        gfx = new HeadlessRecordingGfx();
    }

    @Test
    @DisplayName("App: Initial state boots on Screen.MAIN")
    void testInitialScreen() {
        assertEquals(App.Screen.MAIN, app.screen, "Boot screen is Screen.MAIN");
        assertNotNull(app.ui, "UI subsystem instantiated");
        assertNotNull(app.store, "Store reference bound");
    }

    @Test
    @DisplayName("App: 4 Games instantiated with unique IDs and titles")
    void testGamesInventory() {
        assertEquals(4, app.games.size(), "Exactly 4 mini-games configured");
        assertEquals("ttt", app.games.get(0).id);
        assertEquals("snake", app.games.get(1).id);
        assertEquals("mines", app.games.get(2).id);
        assertEquals("slots", app.games.get(3).id);
    }

    @Test
    @DisplayName("App: Sound setting updates in-memory and in Store")
    void testSoundSetting() {
        app.setSound(false);
        assertFalse(app.soundEnabled, "Sound flag is false");
        assertEquals(0, store.getInt("sound_on", 1), "Store persisted sound_on=0");

        app.setSound(true);
        assertTrue(app.soundEnabled, "Sound flag is true");
        assertEquals(1, store.getInt("sound_on", 0), "Store persisted sound_on=1");
    }

    @Test
    @DisplayName("App: Music volume setting persists to Store")
    void testMusicVolumeSetting() {
        app.setVolume(75);
        assertEquals(75, app.musicVolume, "Music volume set to 75");
        assertEquals(75, store.getInt("music_vol", 0), "Store persisted music_vol=75");
    }

    @Test
    @DisplayName("App: Screen transitions via go() transition mechanism")
    void testScreenTransitions() {
        app.go(App.Screen.SETTINGS, null);
        // Simulate frames for fade animation to complete transition
        for (int i = 0; i < 30; i++) {
            app.frame(gfx, 800, 600, 0.016f);
        }
        assertEquals(App.Screen.SETTINGS, app.screen, "Transitioned to SETTINGS screen");
    }

    @Test
    @DisplayName("App: Back navigation from MAIN requests quit")
    void testBackOnMainRequestsQuit() {
        assertFalse(app.quitRequested, "quitRequested false initially");
        app.back();
        assertTrue(app.quitRequested, "Back on MAIN sets quitRequested=true");
    }

    @Test
    @DisplayName("App: Back navigation from SETTINGS returns to MAIN")
    void testBackOnSettingsReturnsToMain() {
        app.screen = App.Screen.SETTINGS;
        app.back();
        for (int i = 0; i < 30; i++) {
            app.frame(gfx, 800, 600, 0.016f);
        }
        assertEquals(App.Screen.MAIN, app.screen, "Returned to MAIN screen");
    }

    @Test
    @DisplayName("Math: C.rgb packs 3 channels into ARGB with full alpha")
    void testRgbPacking() {
        int color = C.rgb(10, 20, 30);
        assertEquals(0xFF, (color >>> 24) & 0xFF, "Alpha is 255 (0xFF)");
        assertEquals(10, (color >> 16) & 0xFF, "Red channel is 10");
        assertEquals(20, (color >> 8) & 0xFF, "Green channel is 20");
        assertEquals(30, color & 0xFF, "Blue channel is 30");
    }

    @Test
    @DisplayName("Math: C.a blends alpha channel accurately")
    void testAlphaBlending() {
        int solidRed = C.rgb(255, 0, 0);
        int halfAlpha = C.a(solidRed, 0.5f);
        int alphaVal = (halfAlpha >>> 24) & 0xFF;
        assertTrue(alphaVal >= 126 && alphaVal <= 129, "Alpha scaled to ~128: " + alphaVal);
        assertEquals(255, (halfAlpha >> 16) & 0xFF, "Red preserved");
    }

    @Test
    @DisplayName("Math: C.mix linearly interpolates all ARGB channels")
    void testColorMixing() {
        int black = C.rgb(0, 0, 0);
        int white = C.rgb(200, 200, 200);
        int mid = C.mix(black, white, 0.5f);

        assertEquals(100, (mid >> 16) & 0xFF, "Red channel midpoint");
        assertEquals(100, (mid >> 8) & 0xFF, "Green channel midpoint");
        assertEquals(100, mid & 0xFF, "Blue channel midpoint");
    }

    @Test
    @DisplayName("Math: C.clamp bounds values within [lo, hi]")
    void testClampFunction() {
        assertEquals(10f, C.clamp(5f, 10f, 20f), 0.001f, "Clamped low");
        assertEquals(20f, C.clamp(25f, 10f, 20f), 0.001f, "Clamped high");
        assertEquals(15f, C.clamp(15f, 10f, 20f), 0.001f, "Within range preserved");
    }

    @Test
    @DisplayName("Math: C.lerp calculates linear interpolation")
    void testLerpFunction() {
        assertEquals(50f, C.lerp(0f, 100f, 0.5f), 0.001f, "Halfway interpolation");
        assertEquals(0f, C.lerp(0f, 100f, 0f), 0.001f, "Zero interpolation");
        assertEquals(100f, C.lerp(0f, 100f, 1f), 0.001f, "Full interpolation");
    }

    @Test
    @DisplayName("Math: C.ease cubic Hermite smoothing (smoothstep)")
    void testEaseFunction() {
        assertEquals(0f, C.ease(0f), 0.001f, "Ease at 0");
        assertEquals(0.5f, C.ease(0.5f), 0.001f, "Ease at 0.5");
        assertEquals(1f, C.ease(1f), 0.001f, "Ease at 1");
    }

    @Test
    @DisplayName("Math: C.easeOut cubic deceleration")
    void testEaseOutFunction() {
        assertEquals(0f, C.easeOut(0f), 0.001f, "EaseOut at 0");
        assertEquals(1f, C.easeOut(1f), 0.001f, "EaseOut at 1");
        assertTrue(C.easeOut(0.5f) > 0.5f, "EaseOut front-loads velocity");
    }

    @Test
    @DisplayName("Math: C.hsv converts hue, saturation, value to RGB")
    void testHsvConversion() {
        int pureRed = C.hsv(0f, 1f, 1f);
        assertEquals(255, (pureRed >> 16) & 0xFF, "Hue 0 is red");
        assertEquals(0, (pureRed >> 8) & 0xFF, "No green in pure red");
        assertEquals(0, pureRed & 0xFF, "No blue in pure red");
    }

    @Test
    @DisplayName("Store: Contract default values when key is absent")
    void testStoreContractDefaults() {
        Store testStore = new InMemoryStore();
        assertEquals(99, testStore.getInt("missing_key", 99), "Fallback default returned for int");
        assertEquals("default_val", testStore.get("missing_key", "default_val"), "Fallback default returned for string");
    }
}

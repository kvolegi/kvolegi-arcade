package arcade;

import java.util.ArrayList;
import java.util.List;

/** Screen management: arcade launcher, dedicated TicTacToe menu, settings, play. */
public class App {
    public enum Screen { MAIN, TTT_MENU, TTT_SETTINGS, SETTINGS, PLAY }

    public static final String NAME = "kvolegi arcade";
    public static final String AUTHOR = "kvolegi";

    public final Ui ui = new Ui();
    public final Store store;
    public final List<Game> games = new ArrayList<>();
    public Screen screen;
    public Game current;
    public boolean soundEnabled = true;
    public int musicVolume = 100;
    public boolean showMoneyModal = false;
    public int addMoneyAmount = 100000;
    /** Set by the platform when the user requests quitting. */
    public boolean quitRequested;

    @FunctionalInterface
    public interface IntConsumer {
        void accept(int val);
    }

    public interface NumberPrompt {
        void prompt(String title, int currentVal, IntConsumer onSet);
    }

    public NumberPrompt numberPrompt;

    private Screen pending; private Game pendingGame; private float fade = 0, fadeDir = 0;

    public App(Store store) {
        this.store = store;
        ui.th = new Theme(true); // Locked to pure dark neon
        soundEnabled = store.getInt("sound_on", 1) == 1;
        musicVolume = store.getInt("music_vol", 100);

        // 4 games
        games.add(new TicTacToe(this));
        games.add(new Snake(this));
        games.add(new Mines(this));
        games.add(new Slots(this));
        current = games.get(0);

        screen = Screen.MAIN;
    }

    public void go(Screen s, Game g) { pending = s; pendingGame = g; fadeDir = 1; showMoneyModal = false; }
    public void leaveGame() {
        showMoneyModal = false;
        if (current == games.get(0)) {
            go(Screen.TTT_MENU, current);
        } else {
            go(Screen.MAIN, current);
        }
    }

    public void setTheme(boolean dark) {
        ui.th = new Theme(true);
        store.put("theme", "dark");
    }

    public void setSound(boolean on) {
        soundEnabled = on;
        store.putInt("sound_on", on ? 1 : 0);
    }

    public void setVolume(int vol) {
        musicVolume = vol;
        store.putInt("music_vol", vol);
    }

    /** ESC / Android back. */
    public void back() {
        if (showMoneyModal) {
            showMoneyModal = false;
            return;
        }
        switch (screen) {
            case PLAY:
                if (current != null && !current.onBack()) {
                    leaveGame();
                }
                break;
            case TTT_SETTINGS:
                go(Screen.TTT_MENU, games.get(0));
                break;
            case TTT_MENU:
            case SETTINGS:
                go(Screen.MAIN, null);
                break;
            case MAIN:
                quitRequested = true;
                break;
        }
    }

    // ------------------------------------------------------------------------------------------
    public void frame(Gfx g, float w, float h, float dt) {
        dt = Math.min(dt, 0.05f);
        ui.beginFrame(w, h, dt);
        if (fadeDir != 0) {
            fade += fadeDir * dt / 0.14f;
            if (fadeDir > 0 && fade >= 1) {
                fade = 1; fadeDir = -1;
                if (screen == Screen.PLAY && current != null) current.stop();
                screen = pending;
                if (pendingGame != null) current = pendingGame;
                if (screen == Screen.PLAY) current.start();
            } else if (fadeDir < 0 && fade <= 0) {
                fade = 0; fadeDir = 0;
            }
        }
        boolean block = fadeDir > 0;
        if (block) { ui.released = false; ui.pressed = false; }

        g.color(ui.th.bg);
        g.rect(0, 0, w, h);
        while (ui.keys.remove("ESC")) { if (!block) back(); }
        if (screen != Screen.PLAY) ui.keys.clear();

        switch (screen) {
            case MAIN: Menus.main(this, g, w, h); break;
            case TTT_MENU: Menus.tttMenu(this, g, w, h); break;
            case TTT_SETTINGS: Menus.tttSettings(this, g, w, h); break;
            case SETTINGS: Menus.settings(this, g, w, h); break;
            case PLAY:
                current.frame(g, dt, w, h);
                ui.authorTag(g, 16, h - 14, Gfx.LEFT);
                break;
        }

        if (showMoneyModal) {
            Menus.moneyModal(this, g, w, h);
        }

        if (fade > 0) {
            g.color(C.a(ui.th.bg, C.ease(fade)));
            g.rect(0, 0, w, h);
        }
        ui.endFrame();
    }
}

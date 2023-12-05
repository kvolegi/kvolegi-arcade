package arcade;

import java.util.ArrayList;
import java.util.List;

/** Base class of every mini game. A game owns its options (shown on its menu) and its play loop. */
public abstract class Game {
    public static class Opt {
        public final String key, label;
        public final String[] vals;
        public int sel;
        Opt(String key, String label, String[] vals, int sel) { this.key = key; this.label = label; this.vals = vals; this.sel = sel; }
    }

    public final String id, title, tagline;
    public final int color;
    public final List<Opt> opts = new ArrayList<>();
    protected final App app;
    protected final Ui ui;
    protected final Store store;

    protected Game(App app, String id, String title, String tagline, int color) {
        this.app = app; this.ui = app.ui; this.store = app.store;
        this.id = id; this.title = title; this.tagline = tagline; this.color = color;
    }

    protected Opt opt(String key, String label, int def, String... vals) {
        int s = store.getInt(id + ".opt." + key, def);
        if (s < 0 || s >= vals.length) s = def;
        Opt o = new Opt(key, label, vals, s);
        opts.add(o);
        return o;
    }

    public void saveOpts() { for (Opt o : opts) store.putInt(id + ".opt." + o.key, o.sel); }

    /** Re-read options/scores after the store was cleared. */
    public void reload() { for (Opt o : opts) o.sel = store.getInt(id + ".opt." + o.key, 0); }

    /** Begin a fresh run with the currently selected options. */
    public abstract void start();

    /** Update and draw one frame of play. */
    public abstract void frame(Gfx g, float dt, float w, float h);

    /** Text shown on the menu card, e.g. "best 120". */
    public abstract String bestText();

    /** Draws the game icon (used on menu cards and exported as app icon art). */
    public abstract void icon(Gfx g, float cx, float cy, float size);

    /** Called by App when ESC / Android back is pressed during play. Return true if handled internally. */
    public boolean onBack() { return false; }

    /** Called when the play screen is left (stop timers etc.). */
    public void stop() {}

    protected void exit() { app.leaveGame(); }

    // helpers shared by games --------------------------------------------------------------
    protected int accent() { return ui.th.accent(color); }

    /** Standard top bar: back (left) and optional right button. Returns true if back pressed. */
    protected boolean backButton(Gfx g, float w, float h) {
        float s = Math.min(46, Math.max(38, h * 0.06f));
        return ui.iconButton(g, Ui.BACK, 12, 12, s, color);
    }

    /** Dim overlay with a centred card used for pause / game over. */
    protected void overlay(Gfx g, float w, float h, String head, String sub, int col) {
        g.color(C.a(ui.th.bg, 0.78f));
        g.rect(0, 0, w, h);
        ui.title(g, head, w / 2, h * 0.34f, Math.min(64, w * 0.12f), col);
        if (sub != null) ui.label(g, sub, w / 2, h * 0.34f + Math.min(64, w * 0.12f) * 0.95f, 20, false, Gfx.CENTER, ui.th.dim, w - 40);
    }
}

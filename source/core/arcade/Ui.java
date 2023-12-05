package arcade;

import java.util.ArrayDeque;

/** Input state + immediate-mode widgets shared by every screen. */
public class Ui {
    public static final int BACK = 0, PAUSE = 1, PLAY = 2, RESTART = 3, GEAR = 4, HOME = 5, FLAG = 6, SHOVEL = 7, EXIT = 8;

    public Theme th = new Theme(true);
    public float W, H, time;

    // pointer
    public float px = -1, py = -1, downX, downY;
    public boolean down, pressed, released, secondary, consumed;
    public float downTime;
    private float lastSwipeX, lastSwipeY;
    public final ArrayDeque<String> keys = new ArrayDeque<>();

    private final Object inputLock = new Object();
    private boolean pendingDown, pendingUp;
    private float pendDownX, pendDownY, pendUpX, pendUpY;

    public void beginFrame(float w, float h, float dt) {
        W = w; H = h; time += dt;
        pressed = false; released = false; consumed = false;
        synchronized (inputLock) {
            if (pendingDown) {
                downX = pendDownX; downY = pendDownY;
                px = pendDownX; py = pendDownY;
                down = true; pressed = true; downTime = 0;
                lastSwipeX = px; lastSwipeY = py;
                pendingDown = false;
            }
            if (pendingUp) {
                px = pendUpX; py = pendUpY;
                down = false;
                released = true;
                pendingUp = false;
            }
        }
        if (down) downTime += dt;
    }

    public void endFrame() {
        // Events are reset at the start of next frame in beginFrame
    }

    public void pointerMove(float x, float y) {
        synchronized (inputLock) {
            px = x; py = y;
        }
    }

    public void pointerDown(float x, float y, boolean sec) {
        synchronized (inputLock) {
            pendDownX = x; pendDownY = y;
            pendingDown = true;
            secondary = sec;
        }
    }

    public void pointerUp(float x, float y) {
        synchronized (inputLock) {
            pendUpX = x; pendUpY = y;
            pendingUp = true;
        }
    }

    public void key(String k) { keys.add(k); if (keys.size() > 16) keys.poll(); }
    public String pollKey() { return keys.poll(); }

    /** Returns 'U','D','L','R' once a drag exceeds the threshold, else 0. */
    public char swipe(float threshold) {
        if (!down) return 0;
        float dx = px - lastSwipeX, dy = py - lastSwipeY;
        if (Math.abs(dx) < threshold && Math.abs(dy) < threshold) return 0;
        lastSwipeX = px; lastSwipeY = py;
        if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? 'R' : 'L';
        return dy > 0 ? 'D' : 'U';
    }

    public static boolean in(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }
    public boolean hover(float x, float y, float w, float h) { return in(px, py, x, y, w, h); }
    public boolean pressedIn(float x, float y, float w, float h) { return down && in(downX, downY, x - 18, y - 18, w + 36, h + 36); }

    /** True on the frame the user releases inside the rect (having also pressed inside it). */
    public boolean clicked(float x, float y, float w, float h) {
        if (consumed || !released || secondary) return false;
        // Generous touch slop tolerance for mobile finger tapping
        float slop = 24f;
        boolean inCurrent = in(px, py, x - slop, y - slop, w + 2 * slop, h + 2 * slop);
        boolean inDown = in(downX, downY, x - slop, y - slop, w + 2 * slop, h + 2 * slop);
        if (inCurrent || inDown) {
            float distSq = (px - downX) * (px - downX) + (py - downY) * (py - downY);
            if (distSq <= 48 * 48 || inCurrent) {
                consumed = true;
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- effects
    public int neon(float offset) { return th.neon(time * 0.12f + offset); }

    /** Soft glow around a rounded rect, drawn as stacked translucent strokes. */
    public void glowRR(Gfx g, float x, float y, float w, float h, float r, int c, float k) {
        k *= th.glow();
        for (int i = 5; i >= 1; i--) {
            float e = i * 2.4f;
            g.color(C.a(c, 0.055f * k * (6 - i) / 2.2f));
            g.rrectStroke(x - e / 2, y - e / 2, w + e, h + e, r + e / 2, e);
        }
    }

    public void glowCircle(Gfx g, float cx, float cy, float r, int c, float k) {
        k *= th.glow();
        g.radial(cx, cy, r, C.a(c, 0.35f * k), C.a(c, 0f));
    }

    /** Neon title: coloured halo + coloured outline + fill in background colour (tic-tac-toe look). */
    public void title(Gfx g, String s, float cx, float cy, float size, int c) {
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            for (int ring = 1; ring <= 3; ring++) {
                float rr = ring * size * 0.07f;
                g.color(C.a(c, (0.10f / ring) * th.glow() * 1.6f));
                g.text(s, cx + (float) Math.cos(a) * rr, cy + (float) Math.sin(a) * rr, size, true, Gfx.CENTER);
            }
        }
        float o = Math.max(1.2f, size * 0.028f);
        g.color(c);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            g.text(s, cx + (float) Math.cos(a) * o, cy + (float) Math.sin(a) * o, size, true, Gfx.CENTER);
        }
        g.color(th.bg);
        g.text(s, cx, cy, size, true, Gfx.CENTER);
    }

    /** Plain text, auto-shrunk to fit maxW. */
    public void label(Gfx g, String s, float x, float y, float size, boolean bold, int align, int col, float maxW) {
        while (size > 8 && g.textWidth(s, size, bold) > maxW) size -= 1;
        g.color(col);
        g.text(s, x, y, size, bold, align);
    }

    // ---------------------------------------------------------------- widgets
    /** Radiant multi-pass DropShadow glow matching JavaFX sharedGlowEffect (radius 50) in PC version. */
    public void dropShadowGlow(Gfx g, float x, float y, float w, float h, float r, int c, float k) {
        k *= th.glow();
        float scale = Math.max(1f, H / 750f);
        for (int step = 6; step >= 1; step--) {
            float e = step * 6.5f * scale;
            float alpha = (0.055f * k * (7 - step)) / 2.5f;
            g.color(C.a(c, alpha));
            g.rrectStroke(x - e / 2, y - e / 2, w + e, h + e, r + e / 2, e);
        }
    }

    public boolean button(Gfx g, String label, float x, float y, float w, float h, int accent) {
        boolean hov = hover(x, y, w, h) && !down;
        boolean prs = pressedIn(x, y, w, h) && hover(x, y, w, h);
        int glowCol = accent != 0 ? accent : Art.cyclingRainbow(time);
        float scale = Math.max(1f, h / 85f);
        float r = Math.max(3f, 4f * scale);
        dropShadowGlow(g, x, y, w, h, r, glowCol, hov || prs ? 1.8f : 1.2f);
        g.color(prs ? 0xFF111827 : 0xFF000000);
        g.rrect(x, y, w, h, r);
        g.color(hov || prs ? 0xFFFFFFFF : 0xFF000000);
        g.rrectStroke(x, y, w, h, r, Math.max(2.5f, 3.0f * scale));
        g.color(0xFFFFFFFF);
        float fsz = Math.min(36, h * 0.38f);
        g.text(label, x + w / 2, y + h / 2, fsz, true, Gfx.CENTER);
        return clicked(x, y, w, h);
    }

    /** Option switch/chip: unselected = black + silver border; selected = black + crisp white border + neon glow. NO ugly gradient! */
    public boolean chip(Gfx g, String label, float x, float y, float w, float h, boolean sel, int accent) {
        int ac = th.accent(accent);
        boolean hov = hover(x, y, w, h) && !down;
        boolean prs = pressedIn(x, y, w, h) && hover(x, y, w, h);
        float scale = Math.max(1f, h / 45f);
        float r = Math.max(2.5f, 3.5f * scale);
        if (sel) dropShadowGlow(g, x, y, w, h, r, ac, 1.4f);
        g.color(prs ? 0xFF111827 : 0xFF000000); // Pure black fill
        g.rrect(x, y, w, h, r);
        // Border: selected = white 2.5px with glow; unselected = silver 2px
        g.color(sel ? 0xFFFFFFFF : (hov ? 0xFFAAAAAA : 0xFF555555));
        g.rrectStroke(x, y, w, h, r, sel ? 2.5f : 1.8f);
        g.color(sel ? 0xFFFFFFFF : 0xFFD1D5DB);
        g.text(label, x + w / 2, y + h / 2, Math.min(h * 0.44f, 20), true, Gfx.CENTER);
        return clicked(x, y, w, h);
    }

    public boolean iconButton(Gfx g, int kind, float x, float y, float s, int accent) {
        // Expand touch area for corner buttons so fingers touching screen edges never miss
        float hitX = x - 12;
        float hitY = y - 12;
        float hitW = s + 24;
        float hitH = s + 24;
        if (y < 45) {
            hitY = 0;
            hitH += y;
        }
        if (x + s > W - 80) {
            hitW = W - hitX;
        }

        boolean prs = pressedIn(x, y, s, s) || (down && in(downX, downY, hitX, hitY, hitW, hitH));
        boolean hov = hover(hitX, hitY, hitW, hitH) && !down;
        int ac = accent != 0 ? accent : Art.cyclingRainbow(time);
        float r = Math.max(3f, s * 0.15f);
        if (hov || prs) dropShadowGlow(g, x, y, s, s, r, ac, 1.5f);
        g.color(prs ? 0xFF111827 : 0xFF000000);
        g.rrect(x, y, s, s, r);
        g.color(hov || prs ? 0xFFFFFFFF : C.a(ac, 0.45f));
        g.rrectStroke(x, y, s, s, r, 2.0f);
        glyph(g, kind, x + s / 2, y + s / 2, s * 0.5f, 0xFFFFFFFF);
        return clicked(hitX, hitY, hitW, hitH);
    }

    public void glyph(Gfx g, int kind, float cx, float cy, float s, int col) {
        g.color(col);
        float lw = Math.max(2f, s * 0.14f), h = s / 2;
        switch (kind) {
            case BACK:
                g.line(cx + h * 0.35f, cy - h * 0.8f, cx - h * 0.5f, cy, lw);
                g.line(cx - h * 0.5f, cy, cx + h * 0.35f, cy + h * 0.8f, lw);
                break;
            case PAUSE:
                g.rrect(cx - h * 0.7f, cy - h * 0.75f, h * 0.5f, h * 1.5f, 2);
                g.rrect(cx + h * 0.2f, cy - h * 0.75f, h * 0.5f, h * 1.5f, 2);
                break;
            case PLAY:
                g.poly(new float[]{cx - h * 0.5f, cy - h * 0.8f, cx + h * 0.8f, cy, cx - h * 0.5f, cy + h * 0.8f});
                break;
            case RESTART:
                for (int i = 0; i < 9; i++) {
                    double a0 = Math.toRadians(-40 + i * 36), a1 = Math.toRadians(-40 + (i + 1) * 36);
                    g.line(cx + (float) Math.cos(a0) * h * 0.8f, cy + (float) Math.sin(a0) * h * 0.8f,
                            cx + (float) Math.cos(a1) * h * 0.8f, cy + (float) Math.sin(a1) * h * 0.8f, lw);
                }
                g.poly(new float[]{cx + h * 0.95f, cy - h * 0.9f, cx + h * 0.95f, cy - h * 0.2f, cx + h * 0.25f, cy - h * 0.35f});
                break;
            case GEAR:
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.line(cx + (float) Math.cos(a) * h * 0.7f, cy + (float) Math.sin(a) * h * 0.7f,
                            cx + (float) Math.cos(a) * h * 1.0f, cy + (float) Math.sin(a) * h * 1.0f, lw * 1.2f);
                }
                g.ovalStroke(cx - h * 0.62f, cy - h * 0.62f, h * 1.24f, h * 1.24f, lw);
                break;
            case HOME:
                g.line(cx - h, cy - h * 0.05f, cx, cy - h * 0.9f, lw);
                g.line(cx, cy - h * 0.9f, cx + h, cy - h * 0.05f, lw);
                g.rrect(cx - h * 0.62f, cy, h * 1.24f, h * 0.9f, 2);
                break;
            case FLAG:
                g.line(cx - h * 0.4f, cy - h, cx - h * 0.4f, cy + h, lw);
                g.poly(new float[]{cx - h * 0.4f, cy - h, cx + h * 0.9f, cy - h * 0.45f, cx - h * 0.4f, cy + h * 0.1f});
                break;
            case EXIT:
                g.ovalStroke(cx - h * 0.65f, cy - h * 0.55f, h * 1.3f, h * 1.3f, lw);
                g.line(cx, cy - h * 0.85f, cx, cy - h * 0.1f, lw * 1.2f);
                break;
            default:
                g.line(cx - h, cy + h, cx + h * 0.4f, cy - h * 0.4f, lw * 1.3f);
                g.oval(cx + h * 0.05f, cy - h * 1.05f, h * 1.1f, h * 1.1f);
        }
    }

    /** Author signature: strictly "@kvolegi" without any extra text or version numbers. */
    public void authorTag(Gfx g, float x, float y, int align) {
        g.color(0xFF64748B);
        g.text("@kvolegi", x, y, 14, false, align);
    }
}

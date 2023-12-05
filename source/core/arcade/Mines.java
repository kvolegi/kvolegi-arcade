package arcade;

import java.util.Random;

/** Bobr sweeper: minesweeper where hidden beavers want to eat your cabbages (flags). */
public class Mines extends Game {
    private final Opt oDiff, oMode;
    private final Random rnd = new Random();

    private int cols, rows, mines, level = 1;
    private boolean[][] beaver, open, flag;
    private float[][] pop;
    private int[][] num;
    private boolean placed, dead, won, flagMode, longFired;
    private float time, endT;
    private int hitR = -1, hitC = -1, opened;
    private boolean newBest;
    private int sonarCharges = 2;
    private boolean sonarActive = false;
    private boolean showSettings = false;

    public Mines(App app) {
        super(app, "mines", "bobr sweeper", "find the beavers before they eat the broccoli", C.ORANGE);
        oDiff = opt("diff", "Difficulty", 1, "Easy", "Normal", "Hard");
        oMode = opt("mode", "Mode", 0, "Classic", "Levels");
    }

    @Override public String bestText() {
        if (oMode.sel == 1) return "best level " + store.getInt("mines.lvl", 0);
        int t = store.getInt("mines.best." + oDiff.sel, 0);
        return t == 0 ? "no best time yet" : "best " + fmt(t);
    }
    private static String fmt(int s) { return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60); }
    @Override public void icon(Gfx g, float cx, float cy, float size) { Art.minesIcon(g, ui, cx, cy, size); }

    @Override public void start() { level = 1; setup(); }

    private void setup() {
        if (oMode.sel == 0) {
            cols = rows = oDiff.sel == 0 ? 8 : oDiff.sel == 1 ? 10 : 12;
            mines = oDiff.sel == 0 ? 10 : oDiff.sel == 1 ? 15 : 25;
        } else {
            cols = rows = Math.min(6 + level, 12);
            mines = Math.max(3, Math.round(cols * rows * (0.12f + 0.005f * level)));
        }
        beaver = new boolean[rows][cols]; open = new boolean[rows][cols]; flag = new boolean[rows][cols];
        pop = new float[rows][cols]; num = new int[rows][cols];
        placed = dead = won = longFired = false; time = 0; endT = 0; hitR = hitC = -1; opened = 0; newBest = false; flagMode = false;
        sonarCharges = 2; sonarActive = false; showSettings = false;
    }

    private void placeMines(int sr, int sc) {
        int left = mines;
        while (left > 0) {
            int r = rnd.nextInt(rows), c = rnd.nextInt(cols);
            if (beaver[r][c] || (Math.abs(r - sr) <= 1 && Math.abs(c - sc) <= 1)) continue;
            beaver[r][c] = true; left--;
        }
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int k = 0;
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                int rr = r + dr, cc = c + dc;
                if (rr >= 0 && cc >= 0 && rr < rows && cc < cols && beaver[rr][cc]) k++;
            }
            num[r][c] = k;
        }
        placed = true;
    }

    private void reveal(int r, int c) {
        if (r < 0 || c < 0 || r >= rows || c >= cols || open[r][c] || flag[r][c]) return;
        if (!placed) placeMines(r, c);
        if (beaver[r][c]) { lose(r, c); return; }
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        q.add(new int[]{r, c}); open[r][c] = true; opened++;
        int order = 0;
        while (!q.isEmpty()) {
            int[] p = q.poll();
            pop[p[0]][p[1]] = -0.02f * (order++ % 40);
            if (num[p[0]][p[1]] != 0) continue;
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                int rr = p[0] + dr, cc = p[1] + dc;
                if (rr >= 0 && cc >= 0 && rr < rows && cc < cols && !open[rr][cc] && !flag[rr][cc] && !beaver[rr][cc]) {
                    open[rr][cc] = true; opened++; q.add(new int[]{rr, cc});
                }
            }
        }
        if (opened == rows * cols - mines) win();
    }

    private void chord(int r, int c) {
        if (!open[r][c] || num[r][c] == 0) return;
        int f = 0;
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
            int rr = r + dr, cc = c + dc;
            if (rr >= 0 && cc >= 0 && rr < rows && cc < cols && flag[rr][cc]) f++;
        }
        if (f != num[r][c]) return;
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) reveal(r + dr, c + dc);
    }

    private void toggleFlag(int r, int c) {
        if (open[r][c] || dead || won) return;
        flag[r][c] = !flag[r][c];
    }

    private int flags() { int k = 0; for (boolean[] row : flag) for (boolean f : row) if (f) k++; return k; }

    private void lose(int r, int c) { dead = true; hitR = r; hitC = c; endT = 0; }

    private void win() {
        won = true; endT = 0;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) if (beaver[r][c]) flag[r][c] = true;
        if (oMode.sel == 1) { if (level > store.getInt("mines.lvl", 0)) store.putInt("mines.lvl", level); }
        else {
            int t = Math.max(1, (int) time), best = store.getInt("mines.best." + oDiff.sel, 0);
            if (best == 0 || t < best) { store.putInt("mines.best." + oDiff.sel, t); newBest = true; }
        }
    }

    // ------------------------------------------------------------------ frame
    private static final int[] NUMC = {0, C.CYAN, C.GREEN, C.RED, C.PURPLE, C.ORANGE, C.PINK, C.YELLOW, 0xFFFFFFFF};

    @Override
    public void frame(Gfx g, float dt, float w, float h) {
        Theme th = ui.th;
        String k;
        while ((k = ui.pollKey()) != null) { if (k.equals("F") || k.equals("SPACE")) flagMode = !flagMode; if (k.equals("R")) setup(); }
        boolean over = dead || won;
        if (placed && !over) time += dt;
        if (over) endT += dt;
        for (float[] row : pop) for (int i = 0; i < row.length; i++) row[i] = Math.min(1, row[i] + dt / 0.22f);

        float scale = h / 750f;
        float top = Math.round(76 * scale), bottomBar = Math.round(64 * scale);
        float availW = w - Math.round(40 * scale), availH = h - top - bottomBar - Math.round(14 * scale);
        float cs = Math.min(availW / cols, availH / rows);
        float bw = cs * cols, bh = cs * rows;
        float bx = (w - bw) / 2, by = top + (availH - bh) / 2;
        int glowCol = Art.cyclingRainbow(ui.time);

        ui.dropShadowGlow(g, bx - 4, by - 4, bw + 8, bh + 8, 4.0f, glowCol, 1.2f);
        g.color(0xFF000000);
        g.rrect(bx - 4, by - 4, bw + 8, bh + 8, 4.0f);
        g.color(glowCol);
        g.rrectStroke(bx - 4, by - 4, bw + 8, bh + 8, 4.0f, 2.2f);

        if (ui.down && !over && !longFired && ui.downTime > 0.42f) {
            int lr = (int) ((ui.downY - by) / cs), lc = (int) ((ui.downX - bx) / cs);
            if (ui.downX >= bx && ui.downY >= by && lr >= 0 && lc >= 0 && lr < rows && lc < cols && ui.hover(bx + lc * cs, by + lr * cs, cs, cs)) {
                toggleFlag(lr, lc); longFired = true;
            }
        }
        if (ui.released) { boolean was = longFired; if (was) { ui.released = false; } longFired = false; }

        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            float x = bx + c * cs, y = by + r * cs, in = Math.max(1.5f, cs * 0.05f);
            boolean hov = !over && ui.hover(x, y, cs, cs) && !ui.down;
            if (open[r][c]) {
                float a = C.easeOut(Math.max(0, pop[r][c]));
                g.color(0xFF08090D); // Deep black opened cell
                g.rrect(x + in, y + in, cs - 2 * in, cs - 2 * in, 2.5f);
                g.color(0xFF1E293B);
                g.rrectStroke(x + in, y + in, cs - 2 * in, cs - 2 * in, 2.5f, 1.0f);
                if (num[r][c] > 0 && a > 0) {
                    ui.label(g, "" + num[r][c], x + cs / 2, y + cs / 2, cs * 0.58f * (0.6f + 0.4f * a), true, Gfx.CENTER, th.accent(NUMC[num[r][c]]), cs);
                }
            } else {
                if (hov) ui.dropShadowGlow(g, x + in, y + in, cs - 2 * in, cs - 2 * in, 2.5f, glowCol, 1.2f);
                g.color(0xFF000000); // Pure black closed cell
                g.rrect(x + in, y + in, cs - 2 * in, cs - 2 * in, 2.5f);
                g.color(hov ? 0xFFFFFFFF : C.a(glowCol, 0.45f));
                g.rrectStroke(x + in, y + in, cs - 2 * in, cs - 2 * in, 2.5f, hov ? 2.2f : 1.6f);
                if (flag[r][c]) {
                    boolean wrong = dead && !beaver[r][c];
                    Art.broccoli(g, x + cs / 2, y + cs / 2, cs * 0.40f);
                    if (wrong) { g.color(th.accent(C.RED)); g.line(x + cs * 0.2f, y + cs * 0.2f, x + cs * 0.8f, y + cs * 0.8f, 3); g.line(x + cs * 0.8f, y + cs * 0.2f, x + cs * 0.2f, y + cs * 0.8f, 3); }
                }
            }
            if (dead && beaver[r][c] && !flag[r][c]) {
                boolean hit = r == hitR && c == hitC;
                float a = C.easeOut(endT * 3 - (Math.abs(r - hitR) + Math.abs(c - hitC)) * 0.08f);
                if (a > 0) {
                    g.color(hit ? C.a(th.accent(C.RED), 0.5f) : C.a(th.dim, 0.15f));
                    g.rrect(x + in, y + in, cs - 2 * in, cs - 2 * in, 2.0f);
                    Art.beaver(g, x + cs / 2, y + cs / 2, cs * 0.42f * a);
                }
            }
            if (!over && !open[r][c] && ui.clicked(x, y, cs, cs)) {
                if (sonarActive) {
                    sonarActive = false;
                    if (sonarCharges > 0) sonarCharges--;
                    sonarScan(r, c);
                } else if (flagMode) {
                    toggleFlag(r, c);
                } else {
                    reveal(r, c);
                }
            } else if (!over && open[r][c] && ui.clicked(x, y, cs, cs)) {
                if (sonarActive) {
                    sonarActive = false;
                    if (sonarCharges > 0) sonarCharges--;
                    sonarScan(r, c);
                } else {
                    chord(r, c);
                }
            }
            if (!over && ui.pressed && ui.secondary && Ui.in(ui.px, ui.py, x, y, cs, cs)) toggleFlag(r, c);
        }

        // HUD
        float s = Math.round(48 * scale);
        float padX = Math.round(18 * scale), padY = Math.round(14 * scale);
        if (ui.iconButton(g, Ui.BACK, padX, padY, s, color)) exit();
        if (ui.iconButton(g, Ui.GEAR, w - s - padX, padY, s, color)) showSettings = !showSettings;
        if (ui.iconButton(g, Ui.RESTART, w - 2 * s - padX - 8, padY, s, color)) setup();

        float sonarW = Math.round(112 * scale);
        if (ui.chip(g, sonarActive ? "SCANNING" : "Sonar (" + sonarCharges + ")", w - 2 * s - padX - sonarW - 16, padY, sonarW, s, sonarActive, C.CYAN)) {
            if (sonarCharges > 0 && !over) sonarActive = !sonarActive;
        }

        ui.label(g, fmt((int) time), w / 2, Math.round(28 * scale), Math.round(32 * scale), true, Gfx.CENTER, th.fg, w - 2 * s - 30);
        Art.beaver(g, w / 2 - Math.round(70 * scale), Math.round(54 * scale), Math.round(12 * scale));
        ui.label(g, "" + Math.max(0, mines - flags()) + " beavers" + (oMode.sel == 1 ? "  \u2022  level " + level : ""), w / 2 + 6, Math.round(54 * scale), Math.round(15 * scale), false, Gfx.CENTER, th.dim, w - 2 * s - 60);
        // tool toggle
        float tw = Math.min(Math.round(160 * scale), (w - 40) / 2), ty = h - bottomBar + Math.round(8 * scale);
        float chipH = Math.round(46 * scale);
        float tx = w / 2 - tw - 4;
        if (ui.chip(g, "dig", tx, ty, tw, chipH, !flagMode, color)) flagMode = false;
        if (ui.chip(g, "broccoli", w / 2 + 4, ty, tw, chipH, flagMode, C.GREEN)) flagMode = true;
        Art.broccoli(g, w / 2 + 4 + Math.round(22 * scale), ty + chipH / 2, Math.round(12 * scale));

        if (showSettings) {
            float boxW = Math.min(Math.round(440 * scale), w - 40);
            float boxH = Math.round(210 * scale);
            float boxX = (w - boxW) / 2, boxY = (h - boxH) / 2;
            ui.dropShadowGlow(g, boxX, boxY, boxW, boxH, 4f, glowCol, 1.4f);
            g.color(0xFF000000);
            g.rrect(boxX, boxY, boxW, boxH, 4f);
            g.color(0xFFFFFFFF);
            g.rrectStroke(boxX, boxY, boxW, boxH, 4f, 2.0f);

            ui.label(g, "Minesweeper Settings", boxX + boxW / 2, boxY + Math.round(22 * scale), Math.round(20 * scale), true, Gfx.CENTER, th.fg, boxW);

            float yDiff = boxY + Math.round(52 * scale);
            float dChipW = (boxW - 36 - 16) / 3;
            float chipH2 = Math.round(40 * scale);
            String[] diffs = {"Easy (8x8)", "Med (10x10)", "Hard (12x12)"};
            for (int idx = 0; idx < 3; idx++) {
                if (ui.chip(g, diffs[idx], boxX + 14 + idx * (dChipW + 8), yDiff, dChipW, chipH2, oDiff.sel == idx, glowCol)) {
                    oDiff.sel = idx;
                }
            }

            float startW = (boxW - 36) / 2;
            float startY = yDiff + chipH2 + Math.round(22 * scale);
            if (ui.button(g, "Apply & Restart", boxX + 14, startY, startW, Math.round(46 * scale), glowCol)) {
                showSettings = false;
                setup();
            }
            if (ui.button(g, "Close", boxX + 14 + startW + 8, startY, startW, Math.round(46 * scale), C.PURPLE)) {
                showSettings = false;
            }
        }

        if (over && endT > (won ? 0.5f : 1.3f)) {
            String head = won ? (oMode.sel == 1 ? "level " + level + " clear" : "beavers beaten") : "beavers won";
            String sub = won ? (oMode.sel == 0 ? "time " + fmt((int) time) + (newBest ? "  \u2022  new best!" : "") : "next: " + Math.min(6 + level + 1, 16) + " x " + Math.min(6 + level + 1, 16)) : "they ate the broccoli...";
            overlay(g, w, h, head, sub, won ? th.accent(C.GREEN) : th.accent(C.RED));
            float obw = Math.min(260, w - 60), yb = h * 0.34f + 100;
            String main = won && oMode.sel == 1 ? "Next level" : (oMode.sel == 1 ? "Retry level" : "Play again");
            if (ui.button(g, main, (w - obw) / 2, yb, obw, 54, color)) { if (won && oMode.sel == 1) level++; setup(); }
            if (ui.button(g, "Menu", (w - obw) / 2, yb + 66, obw, 54, color)) exit();
        }
    }

    private void sonarScan(int cr, int cc) {
        if (!placed) placeMines(cr, cc);
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
            int rr = cr + dr, cc2 = cc + dc;
            if (rr >= 0 && cc2 >= 0 && rr < rows && cc2 < cols) {
                if (beaver[rr][cc2]) {
                    flag[rr][cc2] = true;
                } else if (!open[rr][cc2]) {
                    reveal(rr, cc2);
                }
            }
        }
    }
}

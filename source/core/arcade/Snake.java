package arcade;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/** Beer snake: apples, slow-mo broccoli, beer that flips your controls, levels with obstacles. */
public class Snake extends Game {
    private final Opt oDiff, oWalls, oMode;
    private final Random rnd = new Random();

    private int cols, rows;
    private float cs, ox, oy;
    private final LinkedList<int[]> body = new LinkedList<>();
    private final List<int[]> walls = new ArrayList<>();
    private final LinkedList<Character> dirs = new LinkedList<>();
    private char dir = 'R';
    private int[] beer = {0, 0}, bonus = null;
    private int bonusType;             // 1 broccoli
    private float bonusLeft, stepT, slowT, drunkT, bannerT, spawnBonusT, deathT;
    private int score, level, eaten, grow;
    private boolean waiting, paused, over, newBest, win;
    private final List<float[]> parts = new ArrayList<>();

    public Snake(App app) {
        super(app, "snake", "beer snake", "drink beer, grow, dodge the walls", C.GREEN);
        oDiff = opt("diff", "Difficulty", 1, "Easy", "Normal", "Hard");
        oWalls = opt("walls", "Walls", 0, "Deadly", "Wrap around");
        oMode = opt("mode", "Mode", 1, "Classic", "Levels");
    }

    private String bestKey() { return "snake.best." + oDiff.sel + oMode.sel + oWalls.sel; }
    @Override public String bestText() { return "best " + store.getInt(bestKey(), 0); }

    @Override public void icon(Gfx g, float cx, float cy, float size) { Art.snakeIcon(g, ui, cx, cy, size); }

    @Override
    public void start() {
        cols = 15;
        rows = 12;
        body.clear(); walls.clear(); dirs.clear(); parts.clear();
        int sx = cols / 2, sy = rows / 2;
        for (int i = 0; i < 3; i++) body.add(new int[]{sx - i, sy});
        dir = 'R'; score = 0; level = 1; eaten = 0; grow = 0; bonus = null; slowT = drunkT = bannerT = 0;
        spawnBonusT = 6; waiting = true; paused = false; over = false; newBest = false; win = false; stepT = 0;
        placeBeer();
    }

    private boolean occupied(int x, int y) {
        for (int[] b : body) if (b[0] == x && b[1] == y) return true;
        for (int[] w : walls) if (w[0] == x && w[1] == y) return true;
        if (beer != null && beer[0] == x && beer[1] == y) return true;
        return bonus != null && bonus[0] == x && bonus[1] == y;
    }

    private int[] freeCell() {
        for (int tries = 0; tries < 500; tries++) {
            int x = rnd.nextInt(cols), y = rnd.nextInt(rows);
            if (!occupied(x, y)) return new int[]{x, y};
        }
        return null;
    }

    private void placeBeer() { beer = null; beer = freeCell(); }

    private float interval() {
        float base = oDiff.sel == 0 ? 0.20f : oDiff.sel == 1 ? 0.14f : 0.095f;
        base *= (float) Math.pow(0.93, oMode.sel == 1 ? level - 1 : Math.min(8, eaten / 6));
        if (slowT > 0) base *= 1.7f;
        return Math.max(0.045f, base);
    }

    private void makeWalls() {
        walls.clear();
        int count = Math.min(46, (level - 1) * 4);
        int[] h = body.getFirst();
        for (int i = 0; i < count; i++) {
            for (int tries = 0; tries < 60; tries++) {
                int x = rnd.nextInt(cols), y = rnd.nextInt(rows);
                if (Math.abs(x - h[0]) < 5 && Math.abs(y - h[1]) < 5) continue;
                if (occupied(x, y)) continue;
                walls.add(new int[]{x, y});
                break;
            }
        }
        for (int[] w : walls) if (beer != null && w[0] == beer[0] && w[1] == beer[1]) { placeBeer(); break; }
    }

    // ------------------------------------------------------------------ input
    private void queue(char d) {
        if (over || waiting) return;
        if (drunkT > 0) d = d == 'U' ? 'D' : d == 'D' ? 'U' : d == 'L' ? 'R' : 'L';
        char last = dirs.isEmpty() ? dir : dirs.getLast();
        if ((last == 'U' && d == 'D') || (last == 'D' && d == 'U') || (last == 'L' && d == 'R') || (last == 'R' && d == 'L') || last == d) return;
        if (dirs.size() < 3) dirs.add(d);
    }

    private void input() {
        String k;
        while ((k = ui.pollKey()) != null) {
            switch (k) {
                case "UP": case "W": if (!waiting) queue('U'); break;
                case "DOWN": case "S": if (!waiting) queue('D'); break;
                case "LEFT": case "A": if (!waiting) queue('L'); break;
                case "RIGHT": case "D": if (!waiting) queue('R'); break;
                case "P": case "SPACE":
                    if (waiting) { waiting = false; }
                    else if (!over) paused = !paused;
                    break;
                case "ENTER":
                    if (waiting) { waiting = false; }
                    else if (over) start();
                    break;
                default: break;
            }
        }
        if (!waiting && !paused && !over) {
            char s = ui.swipe(16);
            if (s != 0) queue(s);
        }
    }

    @Override public boolean onBack() {
        if (!over && !paused && !waiting) { paused = true; return true; }
        return false;
    }

    // ------------------------------------------------------------------ simulation
    private void step() {
        if (!dirs.isEmpty()) dir = dirs.poll();
        int[] h = body.getFirst();
        int nx = h[0], ny = h[1];
        switch (dir) { case 'U': ny--; break; case 'D': ny++; break; case 'L': nx--; break; default: nx++; }
        if (oWalls.sel == 1) { nx = (nx + cols) % cols; ny = (ny + rows) % rows; }
        else if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) { die(); return; }
        boolean tailMoves = grow == 0;
        int n = body.size();
        int i = 0;
        for (int[] b : body) {
            i++;
            if (tailMoves && i == n) break;
            if (b[0] == nx && b[1] == ny) { die(); return; }
        }
        for (int[] w : walls) if (w[0] == nx && w[1] == ny) { die(); return; }
        body.addFirst(new int[]{nx, ny});
        if (grow > 0) grow--; else body.removeLast();

        if (beer != null && nx == beer[0] && ny == beer[1]) {
            eat(beer, 0xFFFFD04A, 10, 1);
            eaten++;
            placeBeer();
            if (oMode.sel == 1 && eaten % 8 == 0) { level++; bannerT = 1.6f; makeWalls(); }
        } else if (bonus != null && nx == bonus[0] && ny == bonus[1]) {
            eat(bonus, C.GREEN, 25, 2);
            slowT = 6;
            bonus = null;
            spawnBonusT = 7 + rnd.nextInt(6);
        }
        if (body.size() >= cols * rows) { win = true; die(); }
    }

    private void eat(int[] cell, int col, int pts, int g) {
        score += pts * (oMode.sel == 1 ? level : 1);
        grow += g;
        float cx = ox + (cell[0] + 0.5f) * cs, cy = oy + (cell[1] + 0.5f) * cs;
        for (int i = 0; i < 14; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, sp = 40 + rnd.nextFloat() * 120;
            parts.add(new float[]{cx, cy, (float) Math.cos(a) * (float) sp, (float) Math.sin(a) * (float) sp, 0.6f, col});
        }
    }

    private void die() {
        over = true; deathT = 0;
        int best = store.getInt(bestKey(), 0);
        if (score > best) { store.putInt(bestKey(), score); newBest = score > 0; }
    }

    private void update(float dt) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            float[] p = parts.get(i);
            p[0] += p[2] * dt; p[1] += p[3] * dt; p[4] -= dt;
            if (p[4] <= 0) parts.remove(i);
        }
        if (over) { deathT += dt; return; }
        if (paused || waiting) return;
        slowT = Math.max(0, slowT - dt); drunkT = Math.max(0, drunkT - dt); bannerT = Math.max(0, bannerT - dt);
        if (bonus != null) { bonusLeft -= dt; if (bonusLeft <= 0) { bonus = null; spawnBonusT = 6 + rnd.nextInt(6); } }
        else { spawnBonusT -= dt; if (spawnBonusT <= 0) {
            int[] c = freeCell();
            if (c != null) { bonus = c; bonusType = 1; bonusLeft = 9; }
            spawnBonusT = 8; } }
        stepT += dt;
        float iv = interval();
        int guard = 0;
        while (stepT >= iv && !over && guard++ < 4) { stepT -= iv; step(); }
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void frame(Gfx g, float dt, float w, float h) {
        input();
        update(dt);
        Theme th = ui.th;
        float scale = h / 750f;
        float top = Math.round(76 * scale);
        float aw = w - Math.round(48 * scale), ah = h - top - Math.round(24 * scale);
        cs = Math.min(aw / cols, ah / rows);
        ox = (w - cs * cols) / 2; oy = top + (ah - cs * rows) / 2;
        float gw = cs * cols, gh = cs * rows;
        int neon = ui.neon(0.1f);

        int rainbow = Art.cyclingRainbow(ui.time);

        // board: pure solid black field matching PC, subtle #141414 grid lines
        ui.dropShadowGlow(g, ox, oy, gw, gh, 2.5f, oWalls.sel == 1 ? 0xFF00FFFF : rainbow, 1.2f);
        g.color(0xFF000000); // Pure black board
        g.rrect(ox, oy, gw, gh, 2.5f);

        // Subtle dark grid lines matching PC (Color.web("#141414"))
        g.color(0xFF141414);
        for (int x = 0; x <= cols; x++) g.line(ox + x * cs, oy, ox + x * cs, oy + gh, 1.0f);
        for (int y = 0; y <= rows; y++) g.line(ox, oy + y * cs, ox + gw, oy + y * cs, 1.0f);

        // Neon border
        g.color(oWalls.sel == 1 ? 0xFF00FFFF : rainbow);
        g.rrectStroke(ox, oy, gw, gh, 2.5f, 2.2f);

        // obstacles (black square with neon red border & 'X')
        for (int[] wl : walls) {
            float x = ox + wl[0] * cs + 2, y = oy + wl[1] * cs + 2;
            float sz = cs - 4;
            g.color(0xFF000000);
            g.rrect(x, y, sz, sz, 3.0f);
            g.color(0xFFEF4444);
            g.rrectStroke(x, y, sz, sz, 3.0f, 2.0f);
            g.line(x + 5, y + 5, x + sz - 5, y + sz - 5, 1.5f);
            g.line(x + sz - 5, y + 5, x + 5, y + sz - 5, 1.5f);
        }

        // food is cold beer!
        float pulse = 1 + 0.08f * (float) Math.sin(ui.time * 6);
        if (beer != null) {
            ui.glowCircle(g, ox + (beer[0] + 0.5f) * cs, oy + (beer[1] + 0.5f) * cs, cs * 1.0f, C.YELLOW, 1.0f);
            Art.beer(g, ox + (beer[0] + 0.5f) * cs, oy + (beer[1] + 0.55f) * cs, cs * 0.42f * pulse);
        }
        if (bonus != null && (bonusLeft > 2.5f || ((int) (ui.time * 8)) % 2 == 0)) {
            float bx = ox + (bonus[0] + 0.5f) * cs, by = oy + (bonus[1] + 0.5f) * cs;
            ui.glowCircle(g, bx, by, cs * 1.1f, C.GREEN, 1.2f);
            Art.broccoli(g, bx, by, cs * 0.42f * pulse);
        }

        // snake: pure black segments with crisp neon outline matching PC
        int n = body.size();
        float pad = 2f;
        int[] hd = body.getFirst();
        ui.glowCircle(g, ox + (hd[0] + 0.5f) * cs, oy + (hd[1] + 0.5f) * cs, cs * 1.4f, 0xFF4ADE80, 0.8f);

        java.util.Iterator<int[]> it = body.descendingIterator();
        int idx = n;
        while (it.hasNext()) {
            int[] b = it.next(); idx--;
            float bx = ox + b[0] * cs + pad;
            float by = oy + b[1] * cs + pad;
            float sz = cs - pad * 2;
            float r = 3.0f;

            // Pure black fill
            g.color(0xFF000000);
            g.rrect(bx, by, sz, sz, r);

            if (idx == 0) {
                // Head: bright neon green outline
                g.color(0xFF4ADE80);
                g.rrectStroke(bx, by, sz, sz, r, 2.5f);
            } else {
                // Body: synchronized rainbow cycling neon outline
                float frac = (float) idx / (float) Math.max(1, n);
                int segCol = Art.cyclingRainbow(ui.time - frac * 0.4f);
                g.color(segCol);
                g.rrectStroke(bx, by, sz, sz, r, 2.0f);
            }
        }
        // eyes
        float hx = ox + (hd[0] + 0.5f) * cs, hy = oy + (hd[1] + 0.5f) * cs;
        float fx = dir == 'L' ? -1 : dir == 'R' ? 1 : 0, fy = dir == 'U' ? -1 : dir == 'D' ? 1 : 0;
        float sxp = -fy, syp = fx;
        for (int e = -1; e <= 1; e += 2) {
            float ex = hx + fx * cs * 0.12f + sxp * e * cs * 0.18f, ey = hy + fy * cs * 0.12f + syp * e * cs * 0.18f;
            g.color(0xFFFFFFFF); g.oval(ex - cs * 0.11f, ey - cs * 0.11f, cs * 0.22f, cs * 0.22f);
            g.color(0xFF000000); g.oval(ex + fx * cs * 0.03f - cs * 0.055f, ey + fy * cs * 0.03f - cs * 0.055f, cs * 0.11f, cs * 0.11f);
        }
        for (float[] p : parts) { g.color(C.a((int) p[5], C.clamp(p[4] / 0.6f, 0, 1))); g.oval(p[0] - 3, p[1] - 3, 6, 6); }

        // HUD
        float bs = Math.round(48 * scale);
        float padX = Math.round(18 * scale), padY = Math.round(14 * scale);
        if (ui.iconButton(g, Ui.BACK, padX, padY, bs, color)) exit();
        if (!over && ui.iconButton(g, paused ? Ui.PLAY : Ui.PAUSE, w - bs - padX, padY, bs, color)) paused = !paused;
        int ac = th.accent(color);
        ui.label(g, "" + score, w / 2, Math.round(28 * scale), Math.round(34 * scale), true, Gfx.CENTER, th.fg, w - 2 * bs - 40);
        String sub = (oMode.sel == 1 ? "level " + level + "   " : "") + "best " + Math.max(score, store.getInt(bestKey(), 0));
        ui.label(g, sub, w / 2, Math.round(54 * scale), Math.round(14 * scale), false, Gfx.CENTER, th.dim, w - 2 * bs - 40);
        if (drunkT > 0) ui.label(g, "BEER! controls reversed " + (int) Math.ceil(drunkT), w / 2, oy + gh - 18, 16, true, Gfx.CENTER, th.accent(C.YELLOW), w - 20);
        else if (slowT > 0) ui.label(g, "slow-mo " + (int) Math.ceil(slowT), w / 2, oy + gh - 18, 16, true, Gfx.CENTER, th.accent(C.GREEN), w - 20);

        if (bannerT > 0) {
            g.save();
            ui.title(g, "LEVEL " + level, w / 2, oy + gh * 0.3f, Math.min(54, w * 0.1f), neon);
            g.restore();
        }
        if (waiting && !over) {
            float boxW = Math.min(Math.round(440 * scale), w - 40);
            float boxH = Math.round(255 * scale);
            float boxX = (w - boxW) / 2, boxY = oy + (gh - boxH) / 2;
            ui.dropShadowGlow(g, boxX, boxY, boxW, boxH, 4f, neon, 1.4f);
            g.color(0xFF000000);
            g.rrect(boxX, boxY, boxW, boxH, 4f);
            g.color(0xFFFFFFFF);
            g.rrectStroke(boxX, boxY, boxW, boxH, 4f, 2.0f);

            ui.label(g, "Game Setup", boxX + boxW / 2, boxY + Math.round(20 * scale), Math.round(20 * scale), true, Gfx.CENTER, th.fg, boxW);

            // Mode Toggle
            float chipW = (boxW - 36) / 2;
            float chipH = Math.round(38 * scale);
            float yChips = boxY + Math.round(42 * scale);
            if (ui.chip(g, "25 Levels", boxX + 14, yChips, chipW, chipH, oMode.sel == 1, C.CYAN)) {
                oMode.sel = 1;
            }
            if (ui.chip(g, "Endless", boxX + 14 + chipW + 8, yChips, chipW, chipH, oMode.sel == 0, C.CYAN)) {
                oMode.sel = 0;
            }

            // Difficulty Chips
            float yDiff = yChips + chipH + Math.round(8 * scale);
            float dChipW = (boxW - 36 - 16) / 3;
            String[] diffs = {"Calm", "Normal", "Fast"};
            for (int k = 0; k < 3; k++) {
                if (ui.chip(g, diffs[k], boxX + 14 + k * (dChipW + 8), yDiff, dChipW, chipH, oDiff.sel == k, C.GREEN)) {
                    oDiff.sel = k;
                }
            }

            // Wall Collision Toggle
            float yWalls = yDiff + chipH + Math.round(8 * scale);
            if (ui.chip(g, "Fatal Walls", boxX + 14, yWalls, chipW, chipH, oWalls.sel == 0, C.YELLOW)) {
                oWalls.sel = 0;
            }
            if (ui.chip(g, "Pass-Through", boxX + 14 + chipW + 8, yWalls, chipW, chipH, oWalls.sel == 1, C.YELLOW)) {
                oWalls.sel = 1;
            }

            // Start Button
            float startW = boxW - 28;
            float startY = yWalls + chipH + Math.round(10 * scale);
            if (ui.button(g, "START GAME", boxX + 14, startY, startW, Math.round(44 * scale), C.GREEN)) {
                waiting = false;
            }
        }
        if (paused && !over) {
            overlay(g, w, h, "paused", null, ac);
            float bw = Math.min(260, w - 60);
            float by = h * 0.34f + 90;
            if (ui.button(g, "Resume", (w - bw) / 2, by, bw, 54, color)) paused = false;
            if (ui.button(g, "Menu", (w - bw) / 2, by + 66, bw, 54, color)) exit();
        }
        if (over && deathT > 0.5f) {
            overlay(g, w, h, win ? "perfect!" : "game over", "score " + score + (newBest ? "  \u2022  new best!" : ""), ac);
            float bw = Math.min(260, w - 60);
            float by = h * 0.34f + 100;
            if (ui.button(g, "Play again", (w - bw) / 2, by, bw, 54, color)) start();
            if (ui.button(g, "Menu", (w - bw) / 2, by + 66, bw, 54, color)) exit();
        }
    }
}

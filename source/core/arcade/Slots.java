package arcade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Sigma slots: 5 reels x 4 rows, 10 paylines, wilds, scatter free spins, levels and persistent balance. */
public class Slots extends Game {
    static final int COLS = 5, ROWS = 4, WILD = 7, SCATTER = 8, NSYM = 9;
    // Exactly the 20 paylines from original SigmaSlots (GameController.java)
    static final int[][] LINES = {
            {0, 0, 0, 0, 0}, // Line 1: Row 0
            {1, 1, 1, 1, 1}, // Line 2: Row 1
            {2, 2, 2, 2, 2}, // Line 3: Row 2
            {3, 3, 3, 3, 3}, // Line 4: Row 3
            {0, 1, 2, 1, 0}, // Line 5: Diag 1
            {3, 2, 1, 2, 3}, // Line 6: Diag 2
            {0, 0, 1, 0, 0}, // Line 7: Zigzag 1
            {3, 3, 2, 3, 3}, // Line 8: Zigzag 2
            {1, 2, 3, 2, 1}, // Line 9: V-shape
            {2, 1, 0, 1, 2}, // Line 10: ^-shape
            {0, 1, 1, 1, 0}, // Line 11: Wave 1
            {3, 2, 2, 2, 3}, // Line 12: Wave 2
            {1, 0, 1, 0, 1}, // Line 13: Zigzag 3
            {2, 3, 2, 3, 2}, // Line 14: Zigzag 4
            {0, 2, 0, 2, 0}, // Line 15: Wave 3
            {3, 1, 3, 1, 3}, // Line 16: Wave 4
            {1, 1, 0, 1, 1}, // Line 17: Wave 5
            {2, 2, 3, 2, 2}, // Line 18: Wave 6
            {0, 1, 0, 1, 0}, // Line 19: Zigzag 5
            {3, 2, 3, 2, 3}  // Line 20: Zigzag 6
    };
    /** Payout multipliers: [3 sym, 4 sym, 5 sym] matching authentic Vegas paytable */
    static final double[][] PAY = {
            {0.18, 0.8, 2.5},   // vinograd
            {0.18, 0.8, 2.5},   // arbuz
            {0.20, 0.9, 3.5},   // lemon
            {0.18, 0.8, 2.5},   // ananas
            {0.18, 0.8, 2.5},   // sliva
            {0.35, 1.2, 6.0},   // df
            {0.40, 2.5, 5.0},   // banana
            {1.80, 4.5, 12.0},  // BIGBANANA
            {4.00, 12.0, 30.0}  // BIGWIN
    };
    static final int[] WEIGHT = {17, 16, 14, 11, 15, 9, 7, 4, 3};
    static final int[] BETS = {10, 20, 50, 100, 200, 500, 1000};
    static final double FACTOR = 1.0;
    private static final String[] NAMES = {"vinograd", "arbuz", "lemon", "ananas", "sliva", "df", "banana", "BIGBANANA", "BIGWIN"};

    static class Win { int line, sym, count; double amount; }

    private final Opt oSpeed;
    private final Random rnd = new Random();

    // reels
    private final float[] pos = new float[COLS];
    private final float[] from = new float[COLS], stopAt = new float[COLS], stopT = new float[COLS];
    private final int[] stopBase = new int[COLS];
    private final int[][] finalSyms = new int[COLS][ROWS];
    private final int[] seed = new int[COLS];
    private final boolean[] spinning = new boolean[COLS], easing = new boolean[COLS];
    private int[][] grid = new int[ROWS][COLS];

    private long balance;
    private int currentBet = 10, spins, freeSpins;
    private boolean showBetModal;
    private String customBetInput = "10";
    private float spinT, winShowT, bannerT, idleT;
    private int state;                 // 0 idle, 1 spinning, 2 showing wins
    private List<Win> wins = new ArrayList<>();
    private double lastWin, shownWin;
    private boolean auto, showPay, scatterHit, showAddMoney;
    private String bannerText = "";
    private final List<float[]> coins = new ArrayList<>();
    private boolean freeRound;

    public Slots(App app) {
        super(app, "slots", "sigma slots", "spin the reels, hit the bananas", C.YELLOW);
        oSpeed = opt("speed", "Spin speed", 0, "Normal", "Turbo");
        loadState();
    }

    private void loadState() {
        balance = store.getInt("slots.bal", 1000);
        spins = store.getInt("slots.spins", 0);
        currentBet = Math.max(1, store.getInt("slots.custom_bet", 10));
    }

    @Override public void reload() { super.reload(); loadState(); }
    @Override public String bestText() { return "balance " + store.getInt("slots.bal", 1000) + "  \u2022  top win " + store.getInt("slots.big", 0); }
    @Override public void icon(Gfx g, float cx, float cy, float size) { Art.slotsIcon(g, ui, cx, cy, size); }

    private int level() { return 1 + spins / 25; }
    private int bet() { return Math.max(1, currentBet); }

    @Override
    public void start() {
        loadState();
        state = 0; auto = false; showPay = false; showBetModal = false; freeSpins = 0; lastWin = shownWin = 0; wins.clear(); coins.clear();
        for (int c = 0; c < COLS; c++) { seed[c] = rnd.nextInt(1 << 20); pos[c] = 100; spinning[c] = easing[c] = false; }
        for (int c = 0; c < COLS; c++) for (int r = 0; r < ROWS; r++) grid[r][c] = symAt(c, (int) pos[c] + r);
    }

    @Override public void stop() { saveState(); }
    private void saveState() { store.putInt("slots.bal", (int) Math.min(Integer.MAX_VALUE, balance)); store.putInt("slots.spins", spins); store.putInt("slots.custom_bet", currentBet); }

    // ------------------------------------------------------------------ maths
    static int weighted(Random r) {
        int t = 0; for (int w : WEIGHT) t += w;
        int x = r.nextInt(t);
        for (int i = 0; i < NSYM; i++) { x -= WEIGHT[i]; if (x < 0) return i; }
        return 0;
    }

    private int symAt(int c, int k) {
        if (stopAt[c] != 0 && k >= stopBase[c] && k < stopBase[c] + ROWS) return finalSyms[c][k - stopBase[c]];
        long h = (k * 2654435761L) ^ (seed[c] * 40503L);
        h ^= h >>> 13; h *= 0x5bd1e995L; h ^= h >>> 15;
        Random rr = new Random(h);
        return weighted(rr);
    }

    static List<Win> evaluate(int[][] g, double bet, int[] scatterCount) {
        List<Win> out = new ArrayList<>();
        for (int li = 0; li < LINES.length; li++) {
            int[] L = LINES[li];
            int base = -1, cnt = 0;
            for (int c = 0; c < COLS; c++) {
                int s = g[L[c]][c];
                if (s == SCATTER) break;
                if (s != WILD) { if (base == -1) base = s; else if (s != base) break; }
                cnt++;
            }
            if (cnt >= 3) {
                int sym = base == -1 ? WILD : base;
                Win w = new Win(); w.line = li; w.sym = sym; w.count = cnt; w.amount = PAY[sym][cnt - 3] * bet * FACTOR;
                if (w.amount > 0) out.add(w);
            }
        }
        int sc = 0;
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) if (g[r][c] == SCATTER) sc++;
        if (scatterCount != null) scatterCount[0] = sc;
        return out;
    }

    static double scatterPay(int sc) { return sc >= 5 ? 20 : sc == 4 ? 5 : sc == 3 ? 1.5 : 0; }
    static int scatterSpins(int sc) { return sc >= 5 ? 15 : sc == 4 ? 12 : sc == 3 ? 8 : 0; }

    /** Monte-Carlo return-to-player used to tune the pay table (not used at runtime). */
    public static double simulate(int n) {
        Random r = new Random(1); double paid = 0, staked = 0; int free = 0;
        int[][] g = new int[ROWS][COLS]; int[] sc = new int[1];
        for (int i = 0; i < n; i++) {
            boolean f = free > 0; if (f) free--; else staked += 1;
            for (int a = 0; a < ROWS; a++) for (int b = 0; b < COLS; b++) g[a][b] = weighted(r);
            double w = 0; for (Win x : evaluate(g, 1, sc)) w += x.amount;
            w += scatterPay(sc[0]); free += scatterSpins(sc[0]);
            paid += f ? w * 2 : w;
        }
        return paid / staked;
    }

    // ------------------------------------------------------------------ actions
    private void spin() {
        if (state != 0) return;
        freeRound = freeSpins > 0;
        if (freeRound) freeSpins--;
        else { if (balance < bet()) return; balance -= bet(); }
        spins++; lastWin = 0; shownWin = 0; wins.clear(); bannerT = 0; scatterHit = false;
        state = 1; spinT = 0;
        for (int c = 0; c < COLS; c++) {
            seed[c] = rnd.nextInt(1 << 20); spinning[c] = true; easing[c] = false; stopAt[c] = 0; stopBase[c] = -999999;
            pos[c] = 1000 + rnd.nextInt(100);
            for (int r = 0; r < ROWS; r++) finalSyms[c][r] = weighted(rnd);
        }
        saveState();
    }

    private void finishSpin() {
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) grid[r][c] = finalSyms[c][r];
        int[] sc = new int[1];
        wins = evaluate(grid, bet(), sc);
        double total = 0;
        for (Win w : wins) total += w.amount;
        if (sc[0] >= 3) {
            total += scatterPay(sc[0]) * bet();
            int add = scatterSpins(sc[0]); freeSpins += add;
            scatterHit = true; bannerText = "+" + add + " FREE SPINS"; bannerT = 2.2f;
        }
        if (freeRound) total *= 2;
        lastWin = Math.round(total);
        balance += (long) lastWin;
        if (lastWin > store.getInt("slots.big", 0)) store.putInt("slots.big", (int) lastWin);
        if (lastWin >= bet() * 10 && !scatterHit) { bannerText = lastWin >= bet() * 30 ? "MEGA WIN" : "BIG WIN"; bannerT = 2.4f; }
        if (lastWin >= bet() * 10) for (int i = 0; i < 60; i++) {
            coins.add(new float[]{ui.W / 2 + (rnd.nextFloat() - 0.5f) * 80, ui.H * 0.4f, (rnd.nextFloat() - 0.5f) * 520, -200 - rnd.nextFloat() * 420, 2.2f});
        }
        state = 2; winShowT = 0; saveState();
    }

    // ------------------------------------------------------------------ frame
    @Override
    public void frame(Gfx g, float dt, float w, float h) {
        Theme th = ui.th;
        String k;
        while ((k = ui.pollKey()) != null) if (k.equals("SPACE") || k.equals("ENTER")) { if (showPay) showPay = false; else spin(); }

        float speed = oSpeed.sel == 1 ? 0.5f : 1f;
        if (state == 1) {
            spinT += dt;
            for (int c = 0; c < COLS; c++) {
                float stopTime = (0.85f + c * 0.3f) * speed;
                if (spinning[c]) {
                    pos[c] -= dt * 26;
                    if (spinT >= stopTime) {
                        spinning[c] = false; easing[c] = true; from[c] = pos[c];
                        stopBase[c] = (int) Math.floor(pos[c]) - 7; stopAt[c] = stopBase[c]; stopT[c] = 0;
                    }
                } else if (easing[c]) {
                    stopT[c] += dt / (0.45f * Math.max(speed, 0.6f));
                    float t = C.easeOut(stopT[c]);
                    pos[c] = C.lerp(from[c], stopBase[c], t);
                    if (stopT[c] >= 1) { easing[c] = false; pos[c] = stopBase[c]; }
                }
            }
            boolean done = true;
            for (int c = 0; c < COLS; c++) if (spinning[c] || easing[c]) done = false;
            if (done) finishSpin();
        } else if (state == 2) {
            winShowT += dt;
            shownWin = Math.min(lastWin, shownWin + Math.max(lastWin, 1) * dt / 1.1);
            float need = wins.isEmpty() ? 0.3f : Math.max(1.2f, wins.size() * 0.9f);
            if (winShowT > need && bannerT <= 0.6f) state = 0;
        } else {
            idleT += dt;
            boolean canAuto = (auto && (freeSpins > 0 || balance >= bet())) || freeSpins > 0;
            if (canAuto && idleT > 0.5f) { idleT = 0; spin(); }
            else if (auto && freeSpins == 0 && balance < bet()) auto = false;
        }
        if (state == 0 && !(auto || freeSpins > 0)) idleT = 0;
        bannerT = Math.max(0, bannerT - dt);
        for (int i = coins.size() - 1; i >= 0; i--) { float[] p = coins.get(i); p[0] += p[2] * dt; p[1] += p[3] * dt; p[3] += 900 * dt; p[4] -= dt; if (p[4] <= 0) coins.remove(i); }

        // ---- layout: Centered reels flanked by left & right side panels
        float top = 68;
        float panelW = Math.round(Math.min(205, Math.max(155, w * 0.20f)));
        float availW = w - 2 * panelW - 32;
        float availH = h - top - 14;
        float gap = 6;
        float cs = Math.min((availW - gap * (COLS - 1)) / COLS, (availH - gap * (ROWS - 1)) / ROWS);
        float gw = cs * COLS + gap * (COLS - 1), gh = cs * ROWS + gap * (ROWS - 1);
        float gx = panelW + 16 + (availW - gw) / 2, gy = top + (availH - gh) / 2;
        int gold = th.accent(C.YELLOW), neon = ui.neon(0.15f);

        ui.glowRR(g, gx - 8, gy - 8, gw + 16, gh + 16, 2.5f, gold, 1f);
        g.color(th.dark ? 0xFF05060A : 0xFFF6F6FB); g.rrect(gx - 8, gy - 8, gw + 16, gh + 16, 2.5f);
        g.color(gold); g.rrectStroke(gx - 8, gy - 8, gw + 16, gh + 16, 2.5f, 2.4f);
        boolean showLine = state == 2 && !wins.isEmpty();
        Win active = showLine ? wins.get((int) (winShowT / 0.9f) % wins.size()) : null;
        for (int c = 0; c < COLS; c++) {
            float x = gx + c * (cs + gap);
            g.save(); g.clip(x, gy - 2, cs, gh + 4);
            g.color(th.panel); g.rrect(x, gy, cs, gh, 2.0f);
            float basePos = pos[c];
            int bi = (int) Math.floor(basePos); float frac = basePos - bi;
            boolean moving = state == 1 && (spinning[c] || easing[c]);
            for (int r = -1; r <= ROWS; r++) {
                int sym = moving ? symAt(c, bi + r) : grid[Math.max(0, Math.min(ROWS - 1, r))][c];
                if (!moving && (r < 0 || r >= ROWS)) continue;
                float y = gy + (r - (moving ? frac : 0)) * (cs + gap);
                boolean hl = !moving && showLine && active != null && isOnWin(active, r, c);
                if (hl) { g.color(C.a(neon, 0.22f)); g.rrect(x, y, cs, cs, 2.0f); }
                float bounce = hl ? 1 + 0.08f * (float) Math.sin(ui.time * 10) : 1f;
                Art.fruit(g, sym, x + cs / 2, y + cs / 2 + (sym == 1 ? cs * 0.04f : 0), cs * 0.34f * bounce);
                if (!moving && state == 2 && sym == SCATTER && scatterHit) { g.color(C.a(C.PINK, 0.3f)); g.rrect(x, y, cs, cs, 2.0f); }
            }
            g.restore();
        }
        if (active != null) {
            int[] L = LINES[active.line];
            for (int pass = 2; pass >= 0; pass--) {
                g.color(pass == 0 ? neon : C.a(neon, 0.15f));
                for (int c = 0; c < COLS - 1; c++)
                    g.line(gx + c * (cs + gap) + cs / 2, gy + L[c] * (cs + gap) + cs / 2, gx + (c + 1) * (cs + gap) + cs / 2, gy + L[c + 1] * (cs + gap) + cs / 2, 3 + pass * 4);
            }
            ui.label(g, NAMES[active.sym] + " x" + active.count + "  +" + Math.round(active.amount * (freeRound ? 2 : 1)), w / 2, gy + gh + 22, 16, true, Gfx.CENTER, th.fg, w - 20);
        }

        // ---- top bar
        float s = 46;
        if (ui.iconButton(g, Ui.BACK, 12, 12, s, color)) exit();
        float moneyW = 60, payW = 95;
        if (ui.chip(g, ":(", w - moneyW - 12, 14, moneyW, 42, app.showMoneyModal, C.YELLOW)) app.showMoneyModal = true;
        if (ui.chip(g, "pay table", w - moneyW - payW - 20, 14, payW, 42, showPay, color)) showPay = !showPay;

        float maxInfoW = Math.max(60, (w - moneyW - payW - 32) - (12 + s + 10));
        float infoCx = (12 + s + 10) + maxInfoW / 2;
        if (w > 620) { infoCx = w / 2; maxInfoW = w - 2 * (moneyW + payW + 30); }
        ui.label(g, freeSpins > 0 ? "FREE SPINS: " + freeSpins : "level " + level(), infoCx, 24, 18, true, Gfx.CENTER, freeSpins > 0 ? th.accent(C.PINK) : th.fg, maxInfoW);
        float lv = (spins % 25) / 25f, bw = Math.min(130, maxInfoW * 0.8f);
        g.color(th.line); g.rrect(infoCx - bw / 2, 42, bw, 5, 2.0f);
        g.color(gold); g.rrect(infoCx - bw / 2, 42, bw * lv, 5, 2.0f);

        // ---- SIDE CONTROLS
        boolean idle = state == 0;
        float ly = top + Math.max(4, (availH - 264) / 2);

        // 1. LEFT PANEL (Balance, Bet, -10, +10, Set Bet)
        float lx = 12;
        if (statBox(g, lx, ly, panelW, 52, "balance", "" + balance, th.fg)) {
            app.showMoneyModal = true;
        }
        if (statBox(g, lx, ly + 58, panelW, 52, "bet", "" + bet(), gold)) {
            if (app.numberPrompt != null) {
                app.numberPrompt.prompt("Set Bet Amount", bet(), (val) -> {
                    if (val < 10) val = 10;
                    if (balance >= 10 && val > balance) val = (int) balance;
                    currentBet = val;
                    saveState();
                });
            } else {
                showBetModal = true;
                customBetInput = "" + bet();
            }
        }
        float stepW = (panelW - 8) / 2;
        if (ui.button(g, "-10", lx, ly + 116, stepW, 64, color) && idle) {
            currentBet = Math.max(10, currentBet - 10);
        }
        if (ui.button(g, "+10", lx + stepW + 8, ly + 116, stepW, 64, color) && idle) {
            currentBet += 10;
            if (balance >= 10 && currentBet > balance) currentBet = (int) balance;
        }
        if (ui.button(g, "Set Bet", lx, ly + 186, panelW, 54, color) && idle) {
            if (app.numberPrompt != null) {
                app.numberPrompt.prompt("Set Bet Amount", bet(), (val) -> {
                    if (val < 10) val = 10;
                    if (balance >= 10 && val > balance) val = (int) balance;
                    currentBet = val;
                    saveState();
                });
            } else {
                showBetModal = true;
                customBetInput = "" + bet();
            }
        }

        // 2. RIGHT PANEL (Win, Spin, Auto)
        float rx = w - panelW - 12;
        String winTxt = state == 2 || lastWin > 0 ? "" + Math.round(shownWin) : "0";
        statBox(g, rx, ly, panelW, 52, "win", winTxt, lastWin > 0 ? th.accent(C.GREEN) : th.dim);

        boolean broke = balance < bet() && freeSpins == 0;
        if (broke && idle) {
            if (ui.button(g, ":(", rx, ly + 58, panelW, 108, C.YELLOW)) app.showMoneyModal = true;
        } else {
            String spinTxt = idle ? (freeSpins > 0 ? "FREE" : "SPIN") : "...";
            if (ui.button(g, spinTxt, rx, ly + 58, panelW, 108, color) && idle) spin();
        }
        if (ui.chip(g, auto ? "STOP AUTO" : "AUTO", rx, ly + 172, panelW, 52, auto, C.GREEN)) auto = !auto;

        // ---- banner / coins / paytable
        for (float[] p : coins) { g.color(C.a(0xFFFFD21F, C.clamp(p[4], 0, 1))); g.oval(p[0] - 6, p[1] - 6, 12, 12); g.color(C.a(0xFFB07A00, C.clamp(p[4], 0, 1))); g.ovalStroke(p[0] - 6, p[1] - 6, 12, 12, 2); }
        if (bannerT > 0) {
            float a = C.clamp(bannerT / 0.4f, 0, 1);
            float sc = 1 + 0.06f * (float) Math.sin(ui.time * 8);
            g.save(); g.translate(w / 2, gy + gh / 2); g.scale(sc, sc); g.translate(-w / 2, -(gy + gh / 2));
            g.color(C.a(th.bg, 0.65f * a)); g.rrect(w / 2 - Math.min(w - 20, 420) / 2, gy + gh / 2 - 60, Math.min(w - 20, 420), 120, 2.5f);
            ui.title(g, bannerText, w / 2, gy + gh / 2 - 8, Math.min(54, w * 0.1f), neon);
            ui.label(g, "+" + Math.round(lastWin), w / 2, gy + gh / 2 + 34, 24, true, Gfx.CENTER, th.fg, w - 40);
            g.restore();
        }
        if (showPay) payTable(g, w, h);
        if (showBetModal) renderBetModal(g, w, h);
    }

    private boolean isOnWin(Win wn, int r, int c) { return c < wn.count && LINES[wn.line][c] == r; }

    private boolean statBox(Gfx g, float x, float y, float w, float h, String label, String val, int col) {
        Theme th = ui.th;
        g.color(th.panel); g.rrect(x, y, w, h, 2.5f);
        g.color(th.line); g.rrectStroke(x, y, w, h, 2.5f, 1.4f);
        ui.label(g, label, x + w / 2, y + 12, 12, false, Gfx.CENTER, th.dim, w - 8);
        ui.label(g, val, x + w / 2, y + 31, 22, true, Gfx.CENTER, col, w - 10);
        return ui.clicked(x, y, w, h);
    }

    private void payTable(Gfx g, float w, float h) {
        Theme th = ui.th;
        g.color(C.a(th.bg, 0.94f)); g.rect(0, 0, w, h);
        ui.title(g, "pay table", w / 2, 40, 36, th.accent(C.YELLOW));
        float rowH = Math.min(48, (h - 160) / 9), y = 80;
        for (int i = 0; i < NSYM; i++) {
            float cx = Math.max(40, w / 2 - 190);
            Art.fruit(g, i, cx, y + rowH / 2, rowH * 0.36f);
            String t;
            if (i == SCATTER) t = "3+ anywhere: free spins, x1.5 / x5 / x20";
            else t = "x" + trim(PAY[i][0]) + "   x" + trim(PAY[i][1]) + "   x" + trim(PAY[i][2]) + (i == WILD ? "   (substitutes)" : "");
            ui.label(g, t, cx + 36, y + rowH / 2, 16, false, Gfx.LEFT, th.fg, w - cx - 50);
            y += rowH;
        }
        ui.label(g, "3 / 4 / 5 on a payline, left to right. Free spins pay x2.", w / 2, y + 16, 14, false, Gfx.CENTER, th.dim, w - 30);
        if (ui.button(g, "Close", w / 2 - 90, h - 70, 180, 50, color)) showPay = false;
    }

    private static String trim(double d) { return d == Math.floor(d) ? "" + (int) d : String.format("%.2f", d).replaceAll("0+$", ""); }

    private void renderBetModal(Gfx g, float w, float h) {
        g.color(0xDC000000);
        g.rect(0, 0, w, h);

        float mw = Math.min(440, w - 30);
        float mh = Math.min(360, h - 30);
        float mx = (w - mw) / 2;
        float my = (h - mh) / 2;
        float r = 4.0f;

        ui.dropShadowGlow(g, mx, my, mw, mh, r, C.YELLOW, 1.4f);
        g.color(0xFF000000);
        g.rrect(mx, my, mw, mh, r);
        g.color(0xFFFFFFFF);
        g.rrectStroke(mx, my, mw, mh, r, 2.5f);

        ui.title(g, "ENTER BET AMOUNT", mx + mw / 2, my + 28, 20, C.YELLOW);

        // Display box (Type your custom amount)
        g.color(0xFF0D0D12);
        g.rrect(mx + 20, my + 48, mw - 40, 50, 2.0f);
        g.color(ui.neon(0.1f));
        g.rrectStroke(mx + 20, my + 48, mw - 40, 50, 2.0f, 2.0f);
        String displayStr = customBetInput.isEmpty() ? "0" : customBetInput;
        ui.label(g, "$ " + displayStr, mx + mw / 2, my + 73, 28, true, Gfx.CENTER, 0xFFFFFFFF, mw - 60);

        // Numeric Keypad
        float kw = (mw - 40 - 2 * 10) / 3;
        float kh = 42;
        float ky = my + 108;

        String[][] grid = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"}
        };

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                String d = grid[row][col];
                float bx = mx + 20 + col * (kw + 10);
                float by = ky + row * (kh + 8);
                if (ui.chip(g, d, bx, by, kw, kh, false, C.CYAN)) {
                    if (customBetInput.equals("0")) customBetInput = d;
                    else if (customBetInput.length() < 7) customBetInput += d;
                }
            }
        }

        // Row 4: Clear, 0, Backspace
        float by4 = ky + 3 * (kh + 8);
        if (ui.chip(g, "Clear", mx + 20, by4, kw, kh, false, C.RED)) {
            customBetInput = "0";
        }
        if (ui.chip(g, "0", mx + 20 + (kw + 10), by4, kw, kh, false, C.CYAN)) {
            if (!customBetInput.equals("0") && customBetInput.length() < 7) customBetInput += "0";
        }
        if (ui.chip(g, "⌫ Del", mx + 20 + 2 * (kw + 10), by4, kw, kh, false, C.RED)) {
            if (customBetInput.length() > 1) customBetInput = customBetInput.substring(0, customBetInput.length() - 1);
            else customBetInput = "0";
        }

        // Bottom Action Row: Cancel & SET
        float by5 = by4 + kh + 10;
        float btnW = (mw - 40 - 10) / 2;
        if (ui.chip(g, "Cancel", mx + 20, by5, btnW, 46, false, C.PURPLE)) {
            showBetModal = false;
        }
        if (ui.button(g, "SET", mx + 20 + btnW + 10, by5, btnW, 46, C.GREEN)) {
            try {
                int val = Integer.parseInt(customBetInput);
                if (val < 10) val = 10;
                if (balance >= 10 && val > balance) val = (int) balance;
                currentBet = val;
            } catch (Exception ignored) {}
            showBetModal = false;
        }
    }
}

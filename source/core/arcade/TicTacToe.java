package arcade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Tic-tac-toe with N-in-a-row, endless expanding board, score mode and an AI with 3 levels. */
public class TicTacToe extends Game {
    final Opt oPlayers, oLevel, oMode, oBoard, oWin, oSide;
    private final Random rnd = new Random();

    private int n;
    private int[][] b;
    private float[][] anim;
    private int turn;                 // 1 = X, 2 = O
    private int winner;               // 0 none, 1, 2, 3 = draw
    private final List<int[]> winCells = new ArrayList<>();
    private int lastR = -1, lastC = -1;
    private float aiT, overT, timeLeft, banner;
    private int winsX, winsO, draws;
    private int scoreX = 0, scoreO = 0;
    private boolean ended;

    public TicTacToe(App app) {
        super(app, "ttt", "tic tac toe", "lines, scores and an endless board", C.PURPLE);
        oPlayers = opt("players", "Players", 0, "1 player", "2 players");
        oLevel = opt("level", "Computer level", 3, "Easy", "Normal", "Hard", "Nightmare");
        oMode = opt("mode", "Mode", 0, "Line", "Score (60s)");
        oBoard = opt("board", "Board", 0, "Endless");
        oWin = opt("win", "Line length", 0, "5 in a row");
        oSide = opt("side", "You play", 0, "X", "O");
    }

    @Override public String bestText() { return "wins " + store.getInt("ttt.wins", 0) + "  \u2022  streak " + store.getInt("ttt.best", 0); }
    @Override public void icon(Gfx g, float cx, float cy, float size) { Art.tttIcon(g, ui, cx, cy, size); }

    private boolean vsAi() { return oPlayers.sel == 0; }
    private int human() { return oSide.sel == 0 ? 1 : 2; }
    private int winLen() { return 5; } // strictly 5 in a row
    private boolean score() { return oMode.sel == 1; }
    private boolean endless() { return true; }

    @Override
    public void start() { winsX = winsO = draws = 0; streak = 0; newRound(); }

    private int streak;

    private void newRound() {
        n = 5;
        b = new int[n][n]; anim = new float[n][n];
        turn = 1; winner = 0; winCells.clear(); lastR = lastC = -1; overT = 0; aiT = 0.5f; timeLeft = 60; ended = false; banner = 1.2f;
        scoreX = 0; scoreO = 0;
    }

    private boolean aiTurn() { return vsAi() && winner == 0 && turn != human(); }

    // ------------------------------------------------------------------ rules
    private int run(int[][] bd, int r, int c, int dr, int dc, int p) {
        int k = 0;
        int rr = r + dr, cc = c + dc;
        while (rr >= 0 && cc >= 0 && rr < bd.length && cc < bd.length && bd[rr][cc] == p) { k++; rr += dr; cc += dc; }
        return k;
    }

    private static final int[][] D = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    private boolean winsAt(int[][] bd, int r, int c, int p, int K, List<int[]> out) {
        for (int[] d : D) {
            int a = run(bd, r, c, d[0], d[1], p), z = run(bd, r, c, -d[0], -d[1], p);
            if (a + z + 1 >= K) {
                if (out != null) {
                    out.clear();
                    for (int i = -z; i <= a; i++) out.add(new int[]{r + d[0] * i, c + d[1] * i});
                }
                return true;
            }
        }
        return false;
    }

    // Exact PC scoring logic: line >= 5 awards 5 pts; line >= 3 awards line pts in all 4 directions
    private int countConsecutive(int[][] bd, int row, int col, int p) {
        int N = bd.length;
        int count = 0;
        for (int[] dir : D) {
            int line = 1;
            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < N && c >= 0 && c < N && bd[r][c] == p) {
                line++;
                r += dir[0]; c += dir[1];
            }
            r = row - dir[0]; c = col - dir[1];
            while (r >= 0 && r < N && c >= 0 && c < N && bd[r][c] == p) {
                line++;
                r -= dir[0]; c -= dir[1];
            }
            if (line >= 5) count += 5;
            else if (line >= 3) count += line;
        }
        return count;
    }

    private int filled() { int k = 0; for (int[] row : b) for (int v : row) if (v != 0) k++; return k; }

    private void place(int r, int c) {
        b[r][c] = turn; anim[r][c] = 0; lastR = r; lastC = c;
        if (score()) {
            int pts = countConsecutive(b, r, c, turn);
            if (turn == 1) scoreX += pts; else scoreO += pts;
        } else if (winsAt(b, r, c, turn, winLen(), winCells)) { finish(turn); return; }
        if (endless() && n + 2 <= 15 && filled() * 2 > n * n) expand();
        if (filled() == n * n) { finish(score() ? scoreWinner() : 3); return; }
        turn = 3 - turn; aiT = 0.45f;
    }

    private int scoreWinner() {
        return scoreX == scoreO ? 3 : (scoreX > scoreO ? 1 : 2);
    }

    private void expand() {
        int m = n + 2;
        int[][] nb = new int[m][m]; float[][] na = new float[m][m];
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) { nb[r + 1][c + 1] = b[r][c]; na[r + 1][c + 1] = anim[r][c]; }
        b = nb; anim = na; n = m; lastR++; lastC++; banner = 1f;
    }

    private void finish(int w) {
        winner = w; overT = 0; ended = true;
        if (w == 1) winsX++; else if (w == 2) winsO++; else draws++;
        if (vsAi()) {
            if (w == human()) {
                streak++;
                store.putInt("ttt.wins", store.getInt("ttt.wins", 0) + 1);
                if (streak > store.getInt("ttt.best", 0)) store.putInt("ttt.best", streak);
            } else streak = 0;
        }
    }

    // ------------------------------------------------------------------ AI
    private double lineValue(int[][] bd, int r, int c, int p, int K) {
        int N = bd.length;
        double tot = 0;
        for (int[] d : D) {
            int a = run(bd, r, c, d[0], d[1], p), z = run(bd, r, c, -d[0], -d[1], p);
            int len = a + z + 1;
            int er = r + d[0] * (a + 1), ec = c + d[1] * (a + 1), fr = r - d[0] * (z + 1), fc = c - d[1] * (z + 1);
            int open = 0;
            if (er >= 0 && ec >= 0 && er < N && ec < N && bd[er][ec] == 0) open++;
            if (fr >= 0 && fc >= 0 && fr < N && fc < N && bd[fr][fc] == 0) open++;
            if (len >= K) { tot += 1e7; continue; }
            if (open == 0) continue;
            tot += Math.pow(9, len) * (open == 2 ? 3 : 1) * (len == K - 1 ? 4 : 1);
        }
        return tot;
    }

    private List<int[]> candidates(int[][] bd) {
        int N = bd.length;
        List<int[]> out = new ArrayList<>();
        boolean any = false;
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) if (bd[r][c] != 0) any = true;
        if (!any) { out.add(new int[]{N / 2, N / 2}); return out; }
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            if (bd[r][c] != 0) continue;
            boolean near = false;
            for (int dr = -2; dr <= 2 && !near; dr++) for (int dc = -2; dc <= 2; dc++) {
                int rr = r + dr, cc = c + dc;
                if (rr >= 0 && cc >= 0 && rr < N && cc < N && bd[rr][cc] != 0) { near = true; break; }
            }
            if (near || N <= 5) out.add(new int[]{r, c});
        }
        return out;
    }

    private int[] aiMove() {
        int me = turn, op = 3 - me, K = winLen(), lvl = oLevel.sel;
        if (score()) return aiScoreMove(me, op, lvl);
        List<int[]> cand = candidates(b);
        if (lvl == 2 && n == 3) return minimaxMove(me);
        // forced win / block (Easy sometimes misses them)
        boolean sharp = lvl > 0 || rnd.nextInt(100) < 55;
        if (sharp) {
            for (int[] m : cand) { b[m[0]][m[1]] = me; boolean w = winsAt(b, m[0], m[1], me, K, null); b[m[0]][m[1]] = 0; if (w) return m; }
            for (int[] m : cand) { b[m[0]][m[1]] = op; boolean w = winsAt(b, m[0], m[1], op, K, null); b[m[0]][m[1]] = 0; if (w) return m; }
        }
        if (lvl == 0 && rnd.nextInt(100) < 65) return cand.get(rnd.nextInt(cand.size()));
        double bestV = -1e18; int[] best = cand.get(0);
        List<double[]> scored = new ArrayList<>();
        for (int i = 0; i < cand.size(); i++) {
            int[] m = cand.get(i);
            double v = lineValue(b, m[0], m[1], me, K) * 1.05 + lineValue(b, m[0], m[1], op, K) * (lvl == 2 ? 1.0 : 0.85);
            v += rnd.nextDouble() * 3;
            scored.add(new double[]{v, i});
        }
        scored.sort((x, y) -> Double.compare(y[0], x[0]));
        if (lvl < 2) { best = cand.get((int) scored.get(0)[1]); return best; }
        // hard: two-ply search over the best candidates
        int top = Math.min(8, scored.size());
        for (int t = 0; t < top; t++) {
            int[] m = cand.get((int) scored.get(t)[1]);
            double v = scored.get(t)[0];
            b[m[0]][m[1]] = me;
            double replyBest = 0;
            for (int[] q : candidates(b)) {
                double rv = lineValue(b, q[0], q[1], op, K) + 0.2 * lineValue(b, q[0], q[1], me, K);
                if (rv > replyBest) replyBest = rv;
            }
            b[m[0]][m[1]] = 0;
            double total = v - replyBest * 0.9;
            if (total > bestV) { bestV = total; best = m; }
        }
        return best;
    }

    private int[] aiScoreMove(int me, int op, int lvl) {
        List<int[]> cand = new ArrayList<>();
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) if (b[r][c] == 0) cand.add(new int[]{r, c});
        if (cand.isEmpty()) return new int[]{0, 0};
        if (lvl == 0 && rnd.nextInt(100) < 55) return cand.get(rnd.nextInt(cand.size()));
        double bestV = -1e9; int[] best = cand.get(0);
        for (int[] m : cand) {
            b[m[0]][m[1]] = me;
            double v = countConsecutive(b, m[0], m[1], me);
            b[m[0]][m[1]] = op;
            v += countConsecutive(b, m[0], m[1], op) * (lvl == 2 ? 1.0 : 0.7); // deny opponent points
            b[m[0]][m[1]] = 0;
            v += rnd.nextDouble() * 0.5;
            if (v > bestV) { bestV = v; best = m; }
        }
        return best;
    }

    private int[] minimaxMove(int me) {
        int bestS = -2; List<int[]> bests = new ArrayList<>();
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            if (b[r][c] != 0) continue;
            b[r][c] = me;
            int s = winsAt(b, r, c, me, 3, null) ? 1 : -negamax(3 - me, me);
            b[r][c] = 0;
            if (s > bestS) { bestS = s; bests.clear(); }
            if (s == bestS) bests.add(new int[]{r, c});
        }
        return bests.get(rnd.nextInt(bests.size()));
    }

    /** value for player p to move (p), root = me. */
    private int negamax(int p, int me) {
        boolean full = true; int best = -2;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            if (b[r][c] != 0) continue;
            full = false;
            b[r][c] = p;
            int s = winsAt(b, r, c, p, 3, null) ? 1 : -negamax(3 - p, me);
            b[r][c] = 0;
            if (s > best) best = s;
        }
        return full ? 0 : best;
    }

    // ------------------------------------------------------------------ frame
    @Override
    public void frame(Gfx g, float dt, float w, float h) {
        Theme th = ui.th;
        ui.pollKey();
        if (!ended && score()) { timeLeft -= dt; if (timeLeft <= 0) { timeLeft = 0; finish(scoreWinner()); } }
        if (ended) overT += dt;
        banner = Math.max(0, banner - dt);
        for (float[] row : anim) for (int i = 0; i < row.length; i++) row[i] = Math.min(1, row[i] + dt / 0.2f);
        if (aiTurn() && !ended) { aiT -= dt; if (aiT <= 0) { int[] m = aiMove(); place(m[0], m[1]); } }

        float scale = h / 750f;
        int cx = th.accent(C.CYAN), co = th.accent(C.RED);
        float top = Math.round(82 * scale), side = Math.round(20 * scale);
        float availW = w - side * 2, availH = h - top - Math.round(20 * scale);
        float bs = Math.min(availW, availH);
        float bx = (w - bs) / 2, by = top + (availH - bs) / 2;
        float gap = Math.max(3f, Math.round(3.5f * scale));
        float cs = (bs - gap * (n - 1)) / n;
        int neon = ui.neon(0.2f);

        ui.glowRR(g, bx - 6, by - 6, bs + 12, bs + 12, 12, neon, 0.85f);
        boolean canPlay = !ended && (!vsAi() || turn == human());
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) {
            float x = bx + c * (cs + gap), y = by + r * (cs + gap);
            boolean hov = canPlay && b[r][c] == 0 && ui.hover(x, y, cs, cs) && !ui.down;
            boolean isLast = (r == lastR && c == lastC);

            // Pure solid black tile fill
            g.color(0xFF000000);
            g.rrect(x, y, cs, cs, Math.max(2.5f, cs * 0.08f));

            if (isLast) {
                ui.dropShadowGlow(g, x, y, cs, cs, Math.max(2.5f, cs * 0.08f), b[r][c] == 1 ? 0xFF22C55E : 0xFFEF4444, 1.4f);
            }

            // Glowing neon borders between cells
            int borderCol = isLast ? 0xFFFFFFFF : neon;
            g.color(hov ? 0xFFFFFFFF : C.a(borderCol, 0.65f));
            g.rrectStroke(x, y, cs, cs, Math.max(2.5f, cs * 0.08f), hov || isLast ? 2.5f : 1.8f);

            if (b[r][c] != 0) drawMark(g, b[r][c], x + cs / 2, y + cs / 2, cs, anim[r][c], isLast, cx, co);
            else if (hov) drawMark(g, turn, x + cs / 2, y + cs / 2, cs, 0.40f, false, cx, co);

            if (canPlay && b[r][c] == 0 && ui.clicked(x, y, cs, cs)) place(r, c);
        }
        if (!winCells.isEmpty()) {
            int[] a = winCells.get(0), z = winCells.get(winCells.size() - 1);
            float x1 = bx + a[1] * (cs + gap) + cs / 2, y1 = by + a[0] * (cs + gap) + cs / 2;
            float x2 = bx + z[1] * (cs + gap) + cs / 2, y2 = by + z[0] * (cs + gap) + cs / 2;
            float k = C.easeOut(overT / 0.5f);
            for (int i = 3; i >= 0; i--) { g.color(C.a(neon, i == 0 ? 1f : 0.12f)); g.line(x1, y1, C.lerp(x1, x2, k), C.lerp(y1, y2, k), cs * (0.08f + i * 0.07f)); }
        }

        // Top bar: Restart and Menu buttons centered 1-to-1 matching PC version
        float topBtnW = Math.round(145 * scale);
        float topBtnH = Math.round(48 * scale);
        float topGap = Math.round(18 * scale);
        float topY = Math.round(14 * scale);
        float topX = (w - (2 * topBtnW + topGap)) / 2;

        if (ui.button(g, "Restart", topX, topY, topBtnW, topBtnH, neon)) {
            newRound();
        }
        if (ui.button(g, "Menu", topX + topBtnW + topGap, topY, topBtnW, topBtnH, neon)) {
            exit();
        }

        String status;
        int sc;
        if (ended) { status = winner == 3 ? "draw" : (vsAi() ? (winner == human() ? "you win!" : "computer wins") : (winner == 1 ? "X wins!" : "O wins!")); sc = winner == 1 ? cx : winner == 2 ? co : th.fg; }
        else if (vsAi()) { status = turn == human() ? "your turn" : "thinking..."; sc = turn == 1 ? cx : co; }
        else { status = (turn == 1 ? "X" : "O") + " turn"; sc = turn == 1 ? cx : co; }

        float sideW = Math.max(120, topX - 20);
        ui.label(g, status, topX / 2, topY + topBtnH / 2, Math.round(20 * scale), true, Gfx.CENTER, sc, sideW);

        String line = "X " + winsX + "  \u2013  " + winsO + " O";
        if (score()) line = "X " + scoreX + " pts \u2022 " + (int) Math.ceil(timeLeft) + "s \u2022 " + scoreO + " pts O";
        ui.label(g, line, w - topX / 2, topY + topBtnH / 2, Math.round(16 * scale), false, Gfx.CENTER, th.dim, sideW);

        if (banner > 0 && !ended && endless() && n > 5) ui.title(g, n + " x " + n, w / 2, by + bs / 2, Math.min(60, w * 0.12f), neon);

        if (ended && overT > 0.9f) {
            String head = winner == 3 ? "draw" : (vsAi() ? (winner == human() ? "you win" : "you lose") : (winner == 1 ? "X wins" : "O wins"));
            overlay(g, w, h, head, vsAi() && winner == human() ? "streak " + streak : (score() ? "X " + scoreX + " : " + scoreO + " O" : null), winner == 2 ? co : cx);
            float bw = Math.min(Math.round(280 * scale), w - 60), yb = h * 0.34f + Math.round(90 * scale);
            if (ui.button(g, "Next round", (w - bw) / 2, yb, bw, Math.round(54 * scale), color)) newRound();
            if (ui.button(g, "Menu", (w - bw) / 2, yb + Math.round(68 * scale), bw, Math.round(54 * scale), color)) exit();
        }
    }

    private void drawMark(Gfx g, int p, float cx, float cy, float cs, float a, boolean last, int colX, int colO) {
        float fsz = cs * 0.54f;
        g.color(0xFFFFFFFF);
        g.text(p == 1 ? "X" : "O", cx, cy, fsz, true, Gfx.CENTER);
    }
}

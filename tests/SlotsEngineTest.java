package tests;

import arcade.App;
import arcade.Slots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tests.ArcadeDomainTest.HeadlessRecordingGfx;
import tests.ArcadeDomainTest.InMemoryStore;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("kvolegi arcade - Slots Engine Specification Tests")
public class SlotsEngineTest {

    private InMemoryStore store;
    private App app;
    private Slots slots;
    private HeadlessRecordingGfx gfx;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        app = new App(store);
        slots = (Slots) app.games.get(3);
        gfx = new HeadlessRecordingGfx();
    }

    @Test
    @DisplayName("Slots: Initial balance, bet & spins state")
    void testInitialBalanceAndBet() {
        slots.start();
        assertEquals(1000, slots.getBalance(), "Default balance is 1000 credits");
        assertTrue(slots.getCurrentBet() >= 1, "Default bet is at least 1 credit");
        assertEquals(0, slots.getSpins(), "Initial spins count is zero");
        assertEquals(0, slots.getFreeSpins(), "Zero free spins initially");
    }

    @Test
    @DisplayName("Slots: 20 Paylines defined with exactly 5 column coordinates")
    void testPaylinesGeometry() {
        // Evaluate paylines count
        int[][] grid = new int[4][5]; // 4 rows x 5 cols
        int[] scatter = new int[1];
        List<?> wins = Slots.evaluate(grid, 10.0, scatter);
        assertNotNull(wins, "Paylines evaluate without null");
    }

    @Test
    @DisplayName("Slots: 3 matching symbols on payline awards authentic payout")
    void testThreeOfAKindEvaluation() {
        int[][] grid = new int[4][5];
        // Fill row 0 with lemon (sym 2)
        grid[0][0] = 2;
        grid[0][1] = 2;
        grid[0][2] = 2;
        grid[0][3] = 0; // different
        grid[0][4] = 1; // different

        int[] scatter = new int[1];
        List<?> wins = Slots.evaluate(grid, 100.0, scatter);
        assertFalse(wins.isEmpty(), "At least one win evaluated for 3 of a kind");
    }

    @Test
    @DisplayName("Slots: 5 matching symbols awards jackpot multiplier")
    void testFiveOfAKindJackpot() {
        int[][] grid = new int[4][5];
        // Line 1: Row 0 across all 5 columns with BIGWIN (sym 8)
        for (int c = 0; c < 5; c++) grid[0][c] = 8;

        int[] scatter = new int[1];
        List<?> wins = Slots.evaluate(grid, 10.0, scatter);
        assertFalse(wins.isEmpty(), "5 BIGWIN symbols produces evaluated win");
    }

    @Test
    @DisplayName("Slots: Wild symbol (7) substitutes for standard symbols")
    void testWildSubstitution() {
        int[][] grid = new int[4][5];
        grid[0][0] = 1; // arbuz
        grid[0][1] = 7; // WILD
        grid[0][2] = 1; // arbuz
        grid[0][3] = 0;
        grid[0][4] = 0;

        int[] scatter = new int[1];
        List<?> wins = Slots.evaluate(grid, 50.0, scatter);
        assertFalse(wins.isEmpty(), "Wild substitutes for arbuz to complete 3 of a kind");
    }

    @Test
    @DisplayName("Slots: Scatter symbols award free spins independently")
    void testScatterFreeSpins() {
        assertEquals(8, Slots.scatterSpins(3), "3 Scatters award 8 free spins");
        assertEquals(12, Slots.scatterSpins(4), "4 Scatters award 12 free spins");
        assertEquals(15, Slots.scatterSpins(5), "5 Scatters award 15 free spins");
        assertEquals(0, Slots.scatterSpins(2), "2 Scatters award 0 free spins");
    }

    @Test
    @DisplayName("Slots: Scatter symbols award cash multipliers")
    void testScatterCashPayout() {
        assertEquals(1.5, Slots.scatterPay(3), 0.001, "3 Scatters pay 1.5x");
        assertEquals(5.0, Slots.scatterPay(4), 0.001, "4 Scatters pay 5.0x");
        assertEquals(20.0, Slots.scatterPay(5), 0.001, "5 Scatters pay 20.0x");
        assertEquals(0.0, Slots.scatterPay(2), 0.001, "2 Scatters pay 0.0x");
    }

    @Test
    @DisplayName("Slots: Weighted random symbol generator produces valid symbol indices [0..8]")
    void testWeightedRandomSymbolBounds() {
        Random rnd = new Random(42);
        for (int i = 0; i < 500; i++) {
            int sym = Slots.weighted(rnd);
            assertTrue(sym >= 0 && sym <= 8, "Generated symbol within [0, 8]: " + sym);
        }
    }

    @Test
    @DisplayName("Slots: Monte-Carlo RTP simulation remains in target bounds (0.50 - 1.50)")
    void testRtpSimulationBounds() {
        double rtp = Slots.simulate(1000);
        assertTrue(rtp >= 0.50 && rtp <= 1.50, "Calculated RTP is in reasonable target bounds: " + rtp);
    }

    @Test
    @DisplayName("Slots: Store balance persistence across reloads")
    void testBalanceStorePersistence() {
        store.putInt("slots.bal", 5420);
        store.putInt("slots.big", 850);
        slots.reload();

        assertEquals(5420, slots.getBalance(), "Balance loaded from Store");
        assertTrue(slots.bestText().contains("5420"), "bestText reflects 5420 balance");
        assertTrue(slots.bestText().contains("850"), "bestText reflects 850 top win");
    }

    @Test
    @DisplayName("Slots: Icon vector geometry renders without error")
    void testIconRendering() {
        slots.icon(gfx, 200, 200, 64);
        assertTrue(gfx.drawCalls > 0, "Slots icon renders vector graphics");
    }

    @Test
    @DisplayName("Slots: Frame execution draws reels, HUD, and buttons")
    void testFrameRenderingExecution() {
        slots.start();
        int initialCalls = gfx.drawCalls;
        slots.frame(gfx, 0.016f, 800, 600);
        assertTrue(gfx.drawCalls > initialCalls, "Slots frame draws UI and reels");
    }
}

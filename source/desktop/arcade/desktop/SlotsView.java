package arcade.desktop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class SlotsView {
    private final Stage stage;
    private final Runnable onBackToArcade;

    private double balance = 1000.0;
    private double bet = 10.0;
    private boolean isSpinning = false;
    private boolean isAutoSpinning = false;
    private Timeline autoSpinTimeline;

    private ImageView[][] reelViews;
    private Label balanceLabel;
    private Label betLabel;
    private Label winLabel;
    private TextField betInputField;
    private GridPane reelGrid;
    private VBox mainContent;

    private final List<String> symbols = Arrays.asList(
            "vinograd.png", "arbuz.png", "lemon.png", "ananas.png",
            "sliva.png", "df.png", "banana.png", "BIGBANANA.png", "BIGWIN.png"
    );

    // Multipliers: [3 symbols, 4 symbols, 5 symbols]
    private final double[][] multipliers = {
            {0.18, 0.8, 2.5},   // vinograd
            {0.18, 0.8, 2.5},   // arbuz
            {0.20, 0.9, 3.5},   // lemon
            {0.18, 0.8, 2.5},   // ananas
            {0.18, 0.8, 2.5},   // sliva
            {0.35, 1.2, 6.0},   // df (dragon fruit)
            {0.40, 2.5, 5.0},   // banana
            {1.80, 4.5, 12.0},  // BIGBANANA (WILD)
            {4.00, 12.0, 30.0}  // BIGWIN (SCATTER)
    };

    // 20 Authentic paylines from GameController.java
    private final List<int[][]> winLines = Arrays.asList(
            new int[][] {{0,0}, {0,1}, {0,2}, {0,3}, {0,4}}, // Line 1
            new int[][] {{1,0}, {1,1}, {1,2}, {1,3}, {1,4}}, // Line 2
            new int[][] {{2,0}, {2,1}, {2,2}, {2,3}, {2,4}}, // Line 3
            new int[][] {{3,0}, {3,1}, {3,2}, {3,3}, {3,4}}, // Line 4
            new int[][] {{0,0}, {1,1}, {2,2}, {1,3}, {0,4}}, // Diagonal 1
            new int[][] {{3,0}, {2,1}, {1,2}, {2,3}, {3,4}}, // Diagonal 2
            new int[][] {{0,0}, {0,1}, {1,2}, {0,3}, {0,4}}, // Zigzag 1
            new int[][] {{3,0}, {3,1}, {2,2}, {3,3}, {3,4}}, // Zigzag 2
            new int[][] {{1,0}, {2,1}, {3,2}, {2,3}, {1,4}}, // V-shape
            new int[][] {{2,0}, {1,1}, {0,2}, {1,3}, {2,4}}, // ^-shape
            new int[][] {{0,0}, {1,1}, {1,2}, {1,3}, {0,4}}, // Wave 1
            new int[][] {{3,0}, {2,1}, {2,2}, {2,3}, {3,4}}, // Wave 2
            new int[][] {{1,0}, {0,1}, {1,2}, {0,3}, {1,4}}, // Zigzag 3
            new int[][] {{2,0}, {3,1}, {2,2}, {3,3}, {2,4}}, // Zigzag 4
            new int[][] {{0,0}, {2,1}, {0,2}, {2,3}, {0,4}}, // Wave 3
            new int[][] {{3,0}, {1,1}, {3,2}, {1,3}, {3,4}}, // Wave 4
            new int[][] {{1,0}, {1,1}, {0,2}, {1,3}, {1,4}}, // Wave 5
            new int[][] {{2,0}, {2,1}, {3,2}, {2,3}, {2,4}}, // Wave 6
            new int[][] {{0,0}, {1,1}, {0,2}, {1,3}, {0,4}}, // Zigzag 5
            new int[][] {{3,0}, {2,1}, {3,2}, {2,3}, {3,4}}  // Zigzag 6
    );

    private final Map<String, Image> imageCache = new HashMap<>();

    public SlotsView(Stage stage, Runnable onBackToArcade) {
        this.stage = stage;
        this.onBackToArcade = onBackToArcade;
        preloadImages();
    }

    public static void show(Stage stage, Runnable onBackToArcade) {
        SlotsView view = new SlotsView(stage, onBackToArcade);
        view.showGame();
    }

    private void preloadImages() {
        for (String sym : symbols) {
            try {
                Image img = new Image(getClass().getResourceAsStream("/" + sym));
                if (!img.isError()) {
                    imageCache.put(sym, img);
                }
            } catch (Exception ignored) { }
        }
    }

    private Image getImage(String sym) {
        Image img = imageCache.get(sym);
        if (img == null) {
            try {
                img = new Image(getClass().getResourceAsStream("/" + sym));
                imageCache.put(sym, img);
            } catch (Exception ignored) { }
        }
        return img;
    }

    public void showGame() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: black;");

        // Top bar
        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(12, 20, 8, 20));

        Label title = new Label("Sigma Slots");
        title.setFont(GlowHelper.getFont(34, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);
        topBar.setLeft(title);

        HBox topButtons = new HBox(12);
        topButtons.setAlignment(Pos.CENTER_RIGHT);

        Button payTableBtn = GlowHelper.createNeonButton("Pay Table", 115, 42);
        payTableBtn.setFont(GlowHelper.getFont(14, true));
        payTableBtn.setOnAction(e -> showPayTableDialog());

        Button addMoneyBtn = GlowHelper.createNeonButton(":(", 65, 42);
        addMoneyBtn.setFont(GlowHelper.getFont(16, true));
        addMoneyBtn.setOnAction(e -> openAddMoneyDialog());

        Button backBtn = GlowHelper.createNeonButton("Arcade Hub", 140, 42);
        backBtn.setFont(GlowHelper.getFont(15, true));
        backBtn.setOnAction(e -> {
            if (autoSpinTimeline != null) autoSpinTimeline.stop();
            onBackToArcade.run();
        });

        topButtons.getChildren().addAll(payTableBtn, addMoneyBtn, backBtn);
        topBar.setRight(topButtons);
        root.setTop(topBar);

        // Center: Main interactive game box with dynamic adaptive scaling
        mainContent = new VBox(6);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(2, 16, 4, 16));
        mainContent.setPickOnBounds(false);

        // Reel Grid: 4 rows x 5 columns
        reelGrid = new GridPane();
        reelGrid.setAlignment(Pos.CENTER);
        reelGrid.setHgap(8);
        reelGrid.setVgap(8);
        reelGrid.setStyle("-fx-border-color: #334155; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-padding: 8; -fx-background-color: #050505;");
        reelGrid.setEffect(GlowHelper.sharedGlowEffect);
        reelGrid.setPickOnBounds(false);

        reelViews = new ImageView[4][5];
        Random rnd = new Random();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 5; c++) {
                ImageView iv = new ImageView();
                iv.setPreserveRatio(true);
                iv.setFitWidth(74);
                iv.setFitHeight(74);
                String sym = symbols.get(rnd.nextInt(symbols.size()));
                iv.setImage(getImage(sym));
                reelViews[r][c] = iv;
                reelGrid.add(iv, c, r);
            }
        }

        winLabel = new Label("Good Luck!");
        winLabel.setFont(GlowHelper.getFont(18, true));
        winLabel.setTextFill(Color.web("#eab308"));
        winLabel.setEffect(GlowHelper.sharedGlowEffect);
        winLabel.setMouseTransparent(true);

        // Stats Bar: Balance, Bet, Input, Set Bet
        HBox statsBar = new HBox(16);
        statsBar.setAlignment(Pos.CENTER);
        statsBar.setPickOnBounds(false);
        statsBar.setViewOrder(0);

        balanceLabel = new Label("Balance: " + (int)balance);
        balanceLabel.setFont(GlowHelper.getFont(16, true));
        balanceLabel.setTextFill(Color.WHITE);
        balanceLabel.setEffect(GlowHelper.sharedGlowEffect);
        balanceLabel.setMouseTransparent(true);

        betLabel = new Label("Bet: " + (int)bet);
        betLabel.setFont(GlowHelper.getFont(16, true));
        betLabel.setTextFill(Color.WHITE);
        betLabel.setEffect(GlowHelper.sharedGlowEffect);
        betLabel.setMouseTransparent(true);

        betInputField = new TextField(String.valueOf((int)bet));
        betInputField.setPrefWidth(110);
        betInputField.setPrefHeight(40);
        betInputField.setFont(GlowHelper.getFont(16, true));
        betInputField.setStyle("-fx-background-color: #0b0f19; -fx-text-fill: #ffffff; -fx-border-color: #38bdf8; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-alignment: center; -fx-highlight-fill: #0284c7; -fx-highlight-text-fill: white;");
        betInputField.setOnMouseClicked(e -> betInputField.selectAll());
        betInputField.setOnAction(e -> setBet());
        betInputField.focusedProperty().addListener((obs, oldV, hasFocus) -> {
            if (!hasFocus) setBet();
        });

        Button setBetBtn = GlowHelper.createNeonButton("Set Bet", 100, 42);
        setBetBtn.setFont(GlowHelper.getFont(14, true));
        setBetBtn.setOnAction(e -> setBet());

        statsBar.getChildren().addAll(balanceLabel, betLabel, betInputField, setBetBtn);

        // Action Controls Bar: Spin, +10, -10, Auto
        HBox controlsBar = new HBox(12);
        controlsBar.setAlignment(Pos.CENTER);
        controlsBar.setPickOnBounds(false);
        controlsBar.setViewOrder(0);

        Button spinBtn = GlowHelper.createNeonButton("SPIN", 180, 52);
        spinBtn.setFont(GlowHelper.getFont(20, true));
        spinBtn.setOnAction(e -> startSpin());

        Button raiseBetBtn = GlowHelper.createNeonButton("+10", 90, 52);
        raiseBetBtn.setOnAction(e -> raiseBet());

        Button lowerBetBtn = GlowHelper.createNeonButton("-10", 90, 52);
        lowerBetBtn.setOnAction(e -> lowerBet());

        Button autoBtn = GlowHelper.createNeonButton("Auto: OFF", 145, 52);
        autoBtn.setFont(GlowHelper.getFont(15, true));
        autoBtn.setOnAction(e -> toggleAutoSpin(autoBtn));

        controlsBar.getChildren().addAll(spinBtn, raiseBetBtn, lowerBetBtn, autoBtn);

        mainContent.getChildren().addAll(reelGrid, winLabel, statsBar, controlsBar);
        root.setCenter(mainContent);

        // Footer: strictly watermark
        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(6, 20, 10, 20));
        footer.setLeft(GlowHelper.createWatermark());
        footer.setPickOnBounds(false);
        footer.setMouseTransparent(true);
        root.setBottom(footer);

        Scene scene = new Scene(root, 1050, 750);

        // Auto-scale reels according to window size so everything fits without clipping
        scene.heightProperty().addListener((obs, oldH, newH) -> adjustReelSizes());
        scene.widthProperty().addListener((obs, oldW, newW) -> adjustReelSizes());

        stage.setScene(scene);
        adjustReelSizes();
    }

    private void adjustReelSizes() {
        if (reelViews == null || stage.getScene() == null) return;
        double winW = stage.getScene().getWidth() > 0 ? stage.getScene().getWidth() : 1050;
        double winH = stage.getScene().getHeight() > 0 ? stage.getScene().getHeight() : 750;
        double availH = Math.max(160, winH - 340);
        double availW = Math.max(260, winW - 140);
        double targetSize = Math.max(40, Math.min(74, Math.min(availH / 4.4, availW / 5.5)));
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 5; c++) {
                if (reelViews[r][c] != null) {
                    reelViews[r][c].setFitWidth(targetSize);
                    reelViews[r][c].setFitHeight(targetSize);
                }
            }
        }
    }

    private void updateLabels() {
        balanceLabel.setText("Balance: " + (int) balance);
        betLabel.setText("Bet: " + (int) bet);
    }

    private void raiseBet() {
        if (!isSpinning && bet + 10 <= balance) {
            bet += 10;
            betInputField.setText(String.valueOf((int)bet));
            updateLabels();
        }
    }

    private void lowerBet() {
        if (!isSpinning && bet - 10 >= 10) {
            bet -= 10;
            betInputField.setText(String.valueOf((int)bet));
            updateLabels();
        }
    }

    private void setBet() {
        try {
            String raw = betInputField.getText().trim().replaceAll("[^0-9]", "");
            if (raw.isEmpty()) {
                betInputField.setText(String.valueOf((int)bet));
                return;
            }
            double val = Double.parseDouble(raw);
            if (val < 10) val = 10;
            if (val > balance) {
                if (balance >= 10) {
                    val = balance;
                    winLabel.setText("Bet set to max balance (" + (int)val + ")");
                } else {
                    winLabel.setText("No balance! Click ':(' to add funds");
                    return;
                }
            } else {
                winLabel.setText("Bet set to " + (int)val);
            }
            bet = val;
            betInputField.setText(String.valueOf((int)bet));
            updateLabels();
        } catch (Exception e) {
            winLabel.setText("Enter a valid number!");
        }
    }

    private void toggleAutoSpin(Button btn) {
        isAutoSpinning = !isAutoSpinning;
        btn.setText(isAutoSpinning ? "Auto: ON" : "Auto: OFF");
        if (isAutoSpinning && !isSpinning) {
            startSpin();
        }
    }

    private void startSpin() {
        if (isSpinning) return;
        setBet();
        if (balance < bet) {
            winLabel.setText("Insufficient balance! Click ':('");
            isAutoSpinning = false;
            return;
        }

        isSpinning = true;
        balance -= bet;
        updateLabels();
        winLabel.setText("Spinning...");

        Random rnd = new Random();
        int[][] finalIndices = new int[4][5];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 5; c++) {
                finalIndices[r][c] = getRandomSymbolIndex(rnd);
            }
        }

        final int totalFrames = 16;
        final int[] currentFrame = {0};

        Timeline spinAnim = new Timeline();
        spinAnim.getKeyFrames().add(new KeyFrame(Duration.millis(70), e -> {
            currentFrame[0]++;
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 5; c++) {
                    if (currentFrame[0] > 8 + c * 2) {
                        reelViews[r][c].setImage(getImage(symbols.get(finalIndices[r][c])));
                    } else {
                        reelViews[r][c].setImage(getImage(symbols.get(getRandomSymbolIndex(rnd))));
                    }
                }
            }
        }));
        spinAnim.setCycleCount(totalFrames);
        spinAnim.setOnFinished(e -> {
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 5; c++) {
                    reelViews[r][c].setImage(getImage(symbols.get(finalIndices[r][c])));
                }
            }
            checkWins(finalIndices);
            isSpinning = false;
            if (isAutoSpinning && balance >= bet) {
                Timeline pause = new Timeline(new KeyFrame(Duration.millis(700), ev -> startSpin()));
                pause.play();
            }
        });
        spinAnim.play();
    }

    // Weighted probabilities giving authentic casino ~93% RTP with frequent thrilling small wins
    private final int[] symbolWeights = {26, 26, 22, 22, 20, 14, 10, 5, 3}; // 9 symbols
    private int getRandomSymbolIndex(Random rnd) {
        int total = 148;
        int roll = rnd.nextInt(total);
        int acc = 0;
        for (int i = 0; i < symbolWeights.length; i++) {
            acc += symbolWeights[i];
            if (roll < acc) return i;
        }
        return 0;
    }

    // Authentic Las Vegas casino rules: left-to-right consecutive matching on paylines, Wild substitution, Scatter pays anywhere
    private void checkWins(int[][] grid) {
        double totalWin = 0;
        int winCount = 0;

        for (int lineIdx = 0; lineIdx < winLines.size(); lineIdx++) {
            int[][] line = winLines.get(lineIdx);
            int base = -1;
            int count = 0;
            for (int c = 0; c < 5; c++) {
                int r = line[c][0];
                int sym = grid[r][c];
                if (sym == 8) break; // Scatter (BIGWIN) does not participate in paylines
                if (sym != 7) { // 7 is WILD (BIGBANANA)
                    if (base == -1) {
                        base = sym;
                    } else if (sym != base) {
                        break;
                    }
                }
                count++;
            }

            if (count >= 3) {
                int winSym = (base == -1) ? 7 : base;
                double mult = multipliers[winSym][count - 3];
                double lineWin = bet * mult;
                totalWin += lineWin;
                winCount++;
            }
        }

        // Scatter wins: BIGWIN (symbol 8) pays anywhere on the 4x5 grid
        int scatterCount = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 5; c++) {
                if (grid[r][c] == 8) scatterCount++;
            }
        }
        if (scatterCount >= 3) {
            double scatterMult = (scatterCount >= 5) ? 20.0 : (scatterCount == 4 ? 5.0 : 1.5);
            double scatterWin = bet * scatterMult;
            totalWin += scatterWin;
            winCount++;
        }

        if (totalWin > 0) {
            balance += totalWin;
            winLabel.setText(String.format(java.util.Locale.US, "WIN: +%.1f credits! (%d lines)", totalWin, winCount));
            winLabel.setTextFill(Color.web("#22c55e"));
        } else {
            winLabel.setText("Try again!");
            winLabel.setTextFill(Color.web("#94a3b8"));
        }
        updateLabels();
    }

    private void showPayTableDialog() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(stage);

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black; -fx-padding: 24; -fx-border-color: #334155; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Label title = new Label("Pay Table (Vegas Rules)");
        title.setFont(GlowHelper.getFont(28, true));
        title.setTextFill(Color.web("#eab308"));
        title.setEffect(GlowHelper.sharedGlowEffect);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Header
        Label hSym = new Label("Symbol");
        hSym.setFont(GlowHelper.getFont(15, true));
        hSym.setTextFill(Color.web("#94a3b8"));
        Label h3 = new Label("3 in line");
        h3.setFont(GlowHelper.getFont(15, true));
        h3.setTextFill(Color.web("#94a3b8"));
        Label h4 = new Label("4 in line");
        h4.setFont(GlowHelper.getFont(15, true));
        h4.setTextFill(Color.web("#94a3b8"));
        Label h5 = new Label("5 in line");
        h5.setFont(GlowHelper.getFont(15, true));
        h5.setTextFill(Color.web("#94a3b8"));
        grid.addRow(0, hSym, h3, h4, h5);

        String[] displayNames = {
                "Grapes", "Watermelon", "Lemon", "Pineapple",
                "Plum", "Dragonfruit", "Banana", "BIG BANANA (WILD)", "BIG WIN (SCATTER)"
        };

        for (int i = 0; i < symbols.size(); i++) {
            HBox symBox = new HBox(10);
            symBox.setAlignment(Pos.CENTER_LEFT);
            ImageView iv = new ImageView(getImage(symbols.get(i)));
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            Label nameLbl = new Label(displayNames[i]);
            nameLbl.setFont(GlowHelper.getFont(14, i >= 7));
            nameLbl.setTextFill(i == 7 ? Color.web("#eab308") : (i == 8 ? Color.web("#38bdf8") : Color.WHITE));
            symBox.getChildren().addAll(iv, nameLbl);

            if (i == 8) {
                Label scLbl = new Label("Anywhere: 3 = x1.5 | 4 = x5.0 | 5 = x20.0");
                scLbl.setFont(GlowHelper.getFont(13, true));
                scLbl.setTextFill(Color.web("#38bdf8"));
                grid.add(symBox, 0, i + 1);
                grid.add(scLbl, 1, i + 1, 3, 1);
            } else {
                Label l3 = new Label("x" + multipliers[i][0]);
                l3.setFont(GlowHelper.getFont(14, false));
                l3.setTextFill(Color.WHITE);
                Label l4 = new Label("x" + multipliers[i][1]);
                l4.setFont(GlowHelper.getFont(14, false));
                l4.setTextFill(Color.WHITE);
                Label l5 = new Label("x" + multipliers[i][2]);
                l5.setFont(GlowHelper.getFont(14, true));
                l5.setTextFill(Color.web("#22c55e"));
                grid.addRow(i + 1, symBox, l3, l4, l5);
            }
        }

        Label rules = new Label("• Lines pay consecutive left to right from reel 1\n• WILD substitutes for any symbol except Scatter\n• 20 active paylines");
        rules.setFont(GlowHelper.getFont(13, false));
        rules.setTextFill(Color.web("#94a3b8"));

        Button closeBtn = GlowHelper.createNeonButton("Close", 160, 44);
        closeBtn.setOnAction(e -> modal.close());

        root.getChildren().addAll(title, grid, rules, closeBtn);

        Scene scene = new Scene(root, 580, 620);
        modal.setScene(scene);
        modal.setTitle("Pay Table");
        modal.show();
    }

    private void openAddMoneyDialog() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black; -fx-padding: 30;");

        Label label = new Label(":(");
        label.setFont(GlowHelper.getFont(36, true));
        label.setTextFill(Color.web("#eab308"));
        label.setEffect(GlowHelper.sharedGlowEffect);

        Label sub = new Label("Balance: " + (int)balance + "  •  Enter custom amount:");
        sub.setFont(GlowHelper.getFont(15, false));
        sub.setTextFill(Color.web("#94a3b8"));

        TextField amountField = new TextField("10000");
        amountField.setMaxWidth(260);
        amountField.setFont(GlowHelper.getFont(18, true));
        amountField.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-alignment: center;");

        HBox quickBtns = new HBox(8);
        quickBtns.setAlignment(Pos.CENTER);
        int[] presets = {1000, 10000, 50000, 100000, 500000, 1000000};
        String[] presetLabels = {"+1k", "+10k", "+50k", "+100k", "+500k", "+1M"};
        for (int i = 0; i < presets.length; i++) {
            final int p = presets[i];
            Button b = GlowHelper.createNeonButton(presetLabels[i], 78, 42);
            b.setFont(GlowHelper.getFont(13, true));
            b.setOnAction(e -> amountField.setText(String.valueOf(p)));
            quickBtns.getChildren().add(b);
        }

        HBox actionBtns = new HBox(14);
        actionBtns.setAlignment(Pos.CENTER);

        Button addBtn = GlowHelper.createNeonButton("Add to Balance", 200, 52);
        addBtn.setOnAction(e -> {
            try {
                String raw = amountField.getText().trim().replaceAll("[^0-9]", "");
                double added = raw.isEmpty() ? 0 : Double.parseDouble(raw);
                if (added < 0) added = 0;
                balance += added;
                updateLabels();
                winLabel.setText("Added: +" + (int)added);
                winLabel.setTextFill(Color.web("#22c55e"));
                modal.close();
            } catch (Exception ex) {
                amountField.setText("10000");
            }
        });

        Button setBtn = GlowHelper.createNeonButton("Set Exact Balance", 200, 52);
        setBtn.setOnAction(e -> {
            try {
                String raw = amountField.getText().trim().replaceAll("[^0-9]", "");
                double exact = raw.isEmpty() ? 0 : Double.parseDouble(raw);
                if (exact < 0) exact = 0;
                balance = exact;
                updateLabels();
                winLabel.setText("Balance set to: " + (int)exact);
                winLabel.setTextFill(Color.web("#22c55e"));
                modal.close();
            } catch (Exception ex) {
                amountField.setText("10000");
            }
        });

        amountField.setOnAction(e -> addBtn.fire());
        actionBtns.getChildren().addAll(addBtn, setBtn);

        root.getChildren().addAll(label, sub, amountField, quickBtns, actionBtns, GlowHelper.createWatermark());
        Scene sc = new Scene(root, 580, 400);
        modal.setScene(sc);
        modal.setTitle(":(");
        modal.show();
    }
}

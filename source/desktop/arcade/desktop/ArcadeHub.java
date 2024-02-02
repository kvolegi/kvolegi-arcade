package arcade.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ArcadeHub {
    private static boolean soundEnabled = true;
    private static double musicVolume = 0.8;
    private static boolean darkTheme = true;

    // High-precision filled SVG path data for settings gear and exit power icon
    private static final String SVG_GEAR =
            "M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.34 19.43,11L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.96 19.05,5.05L16.56,6.05C16.04,5.66 15.5,5.32 14.87,5.07L14.5,2.42C14.46,2.18 14.25,2 14,2H10C9.75,2 9.54,2.18 9.5,2.42L9.13,5.07C8.5,5.32 7.96,5.66 7.44,6.05L4.95,5.05C4.73,4.96 4.46,5.05 4.34,5.27L2.34,8.73C2.21,8.95 2.27,9.22 2.46,9.37L4.57,11C4.53,11.34 4.5,11.67 4.5,12C4.5,12.33 4.53,12.65 4.57,12.97L2.46,14.63C2.27,14.78 2.21,15.05 2.34,15.27L4.34,18.73C4.46,18.95 4.73,19.03 4.95,18.95L7.44,17.94C7.96,18.34 8.5,18.68 9.13,18.93L9.5,21.58C9.54,21.82 9.75,22 10,22H14C14.25,22 14.46,21.82 14.5,21.58L14.87,18.93C15.5,18.67 16.04,18.34 16.56,17.94L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z";

    private static final String SVG_EXIT =
            "M16.56,5.44L15.15,6.86C16.9,8.37 18,10.6 18,13A6,6 0 0,1 12,19A6,6 0 0,1 6,13C6,10.6 7.1,8.37 8.84,6.85L7.44,5.43C5.3,7.31 4,10 4,13A8,8 0 0,0 12,21A8,8 0 0,0 20,13C20,10 18.7,7.32 16.56,5.44M13,3H11V13H13V3Z";

    public static void show(Stage stage) {
        BorderPane container = new BorderPane();
        container.setStyle("-fx-background-color: black;");

        // Top bar: top-right corner vector icon buttons (Settings & Exit)
        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(20, 25, 0, 25));
        topBar.setPickOnBounds(false);
        topBar.setViewOrder(-10);

        HBox cornerButtons = new HBox(14);
        cornerButtons.setAlignment(Pos.CENTER_RIGHT);
        cornerButtons.setPickOnBounds(false);
        cornerButtons.setViewOrder(-20);

        Button settingsIconBtn = GlowHelper.createIconButton(SVG_GEAR, "Настройки");
        settingsIconBtn.setOnAction(e -> openSettingsModal(stage));

        Button exitIconBtn = GlowHelper.createIconButton(SVG_EXIT, "Выход");
        exitIconBtn.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });

        cornerButtons.getChildren().addAll(settingsIconBtn, exitIconBtn);
        topBar.setRight(cornerButtons);
        container.setTop(topBar);

        // Center content: Logo + 4 wide game buttons
        VBox centerBox = new VBox(24);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10, 40, 20, 40));
        centerBox.setPickOnBounds(false);
        centerBox.setViewOrder(0);

        // 1. Logo Box: Comic Sans MS "kvolegi" (black fill, NO white stroke) + "arcade"
        javafx.scene.Node logoBox = GlowHelper.createArcadeLogo();

        // 2. Wide game buttons with concise English names
        Button tttBtn = GlowHelper.createNeonButton("Tic Tac Toe", 480, 85);
        tttBtn.setOnAction(e -> TicTacToeView.show(stage, () -> ArcadeHub.show(stage)));

        Button snakeBtn = GlowHelper.createNeonButton("Beer Snake", 480, 85);
        snakeBtn.setOnAction(e -> SnakeView.show(stage, () -> ArcadeHub.show(stage)));

        Button slotsBtn = GlowHelper.createNeonButton("Sigma Slots", 480, 85);
        slotsBtn.setOnAction(e -> SlotsView.show(stage, () -> ArcadeHub.show(stage)));

        Button minesBtn = GlowHelper.createNeonButton("Minesweeper", 480, 85);
        minesBtn.setOnAction(e -> MinesView.show(stage, () -> ArcadeHub.show(stage)));

        centerBox.getChildren().addAll(logoBox, tttBtn, snakeBtn, slotsBtn, minesBtn);
        container.setCenter(centerBox);

        // Footer: strictly watermark "@kvolegi"
        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(12, 24, 16, 24));
        footer.setLeft(GlowHelper.createWatermark());
        footer.setPickOnBounds(false);
        footer.setMouseTransparent(true);
        container.setBottom(footer);

        Scene scene = new Scene(container, 1050, 780);

        // Adaptive scaling for window resizing
        Runnable adaptSizes = () -> {
            double winW = scene.getWidth() > 0 ? scene.getWidth() : 1050;
            double winH = scene.getHeight() > 0 ? scene.getHeight() : 780;
            double btnH = Math.max(48, Math.min(85, (winH - 240) / 4.9));
            double btnW = Math.max(320, Math.min(500, Math.min(winW - 80, btnH * 5.8)));
            double gap = Math.max(8, Math.min(24, (winH - 500) / 14.0));
            centerBox.setSpacing(gap);
            for (Button b : new Button[]{tttBtn, snakeBtn, slotsBtn, minesBtn}) {
                b.setMinSize(btnW, btnH);
                b.setPrefSize(btnW, btnH);
                b.setFont(GlowHelper.getFont(Math.max(15, Math.min(20, btnH * 0.26)), true));
            }
        };

        scene.widthProperty().addListener((obs, oldW, newW) -> adaptSizes.run());
        scene.heightProperty().addListener((obs, oldH, newH) -> adaptSizes.run());
        adaptSizes.run();

        stage.setTitle("kvolegi arcade");
        stage.setScene(scene);
        stage.show();
    }

    private static void openSettingsModal(Stage parentStage) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(parentStage);

        VBox root = new VBox(22);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black; -fx-padding: 30;");

        Label title = new Label("Settings");
        title.setFont(GlowHelper.getFont(32, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);

        // Sound FX toggle
        HBox soundBox = new HBox(15);
        soundBox.setAlignment(Pos.CENTER);
        Label soundLabel = new Label("Sound FX:");
        soundLabel.setFont(GlowHelper.getFont(18, true));
        soundLabel.setTextFill(Color.WHITE);

        Button soundBtn = GlowHelper.createNeonButton(soundEnabled ? "ON" : "OFF", 140, 48);
        soundBtn.setOnAction(e -> {
            soundEnabled = !soundEnabled;
            soundBtn.setText(soundEnabled ? "ON" : "OFF");
        });
        soundBox.getChildren().addAll(soundLabel, soundBtn);

        // Music volume slider
        VBox volumeBox = new VBox(8);
        volumeBox.setAlignment(Pos.CENTER);
        Label volLabel = new Label("Music Volume: " + (int)(musicVolume * 100) + "%");
        volLabel.setFont(GlowHelper.getFont(16, true));
        volLabel.setTextFill(Color.WHITE);

        Slider volSlider = new Slider(0, 100, musicVolume * 100);
        volSlider.setMaxWidth(260);
        volSlider.valueProperty().addListener((obs, oldV, newV) -> {
            musicVolume = newV.doubleValue() / 100.0;
            volLabel.setText("Music Volume: " + newV.intValue() + "%");
        });
        volumeBox.getChildren().addAll(volLabel, volSlider);

        Button saveBtn = GlowHelper.createNeonButton("Save & Close", 240, 55);
        saveBtn.setOnAction(e -> modal.close());

        root.getChildren().addAll(title, soundBox, volumeBox, saveBtn, GlowHelper.createWatermark());

        Scene scene = new Scene(root, 500, 440);
        modal.setScene(scene);
        modal.setTitle("Settings");
        modal.show();
    }
}

package arcade.desktop;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.InputStream;

public class GlowHelper {
    public static DropShadow sharedGlowEffect;
    public static Timeline globalGlowAnimation;
    private static Font comicFont;

    static {
        initGlow();
        initFont();
    }

    private static void initFont() {
        try {
            InputStream is = GlowHelper.class.getResourceAsStream("/fonts/ComicSansMS.ttf");
            if (is != null) {
                comicFont = Font.loadFont(is, 18);
            }
        } catch (Exception ignored) { }
        if (comicFont == null) {
            comicFont = Font.font("Comic Sans MS", FontWeight.BOLD, 18);
        }
    }

    public static Font getFont(double size, boolean bold) {
        String family = comicFont != null ? comicFont.getFamily() : "Comic Sans MS";
        return Font.font(family, bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
    }

    private static void initGlow() {
        sharedGlowEffect = new DropShadow();
        sharedGlowEffect.setRadius(50);
        sharedGlowEffect.setSpread(0.22);

        globalGlowAnimation = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(sharedGlowEffect.colorProperty(), Color.CYAN)),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(sharedGlowEffect.colorProperty(), Color.PURPLE)),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(sharedGlowEffect.colorProperty(), Color.RED)),
                new KeyFrame(Duration.seconds(3),
                        new KeyValue(sharedGlowEffect.colorProperty(), Color.YELLOW)),
                new KeyFrame(Duration.seconds(4),
                        new KeyValue(sharedGlowEffect.colorProperty(), Color.GREEN)),
                new KeyFrame(Duration.seconds(5),
                        new KeyValue(sharedGlowEffect.colorProperty(), Color.LIME)),
                new KeyFrame(Duration.seconds(6),
                        new KeyValue(sharedGlowEffect.colorProperty(), Color.CYAN))
        );
        globalGlowAnimation.setCycleCount(Timeline.INDEFINITE);
        globalGlowAnimation.setAutoReverse(true);
        globalGlowAnimation.play();
    }

    public static Button createNeonButton(String text) {
        return createNeonButton(text, 480, 88);
    }

    public static Button createNeonButton(String text, double minWidth, double minHeight) {
        Button button = new Button(text);
        button.setFont(getFont(20, true));
        button.setTextFill(Color.WHITE);
        button.setPadding(javafx.geometry.Insets.EMPTY);
        button.setEllipsisString("");
        button.setStyle("-fx-background-color: black; -fx-border-color: black; -fx-border-width: 3px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;");
        button.setEffect(sharedGlowEffect);
        button.setMinSize(minWidth, minHeight);
        button.setPrefSize(minWidth, minHeight);
        button.setPickOnBounds(false); // CRITICAL: strictly click inside button rectangle only!

        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: black; -fx-border-color: white; -fx-border-width: 3px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;")
        );
        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: black; -fx-border-color: black; -fx-border-width: 3px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;")
        );

        return button;
    }

    // Hollow vector SVG icon button with neon glow, placed in corner
    public static Button createIconButton(String svgPathData, String tooltipText) {
        Button button = new Button();
        button.setFocusTraversable(false);
        button.setPickOnBounds(false); // CRITICAL: strictly 54x54 hit area
        button.setMinSize(54, 54);
        button.setPrefSize(54, 54);
        button.setMaxSize(54, 54);
        button.setPadding(javafx.geometry.Insets.EMPTY);

        SVGPath path = new SVGPath();
        path.setContent(svgPathData);
        path.setFill(Color.WHITE);
        path.setMouseTransparent(true);

        DropShadow iconGlow = new DropShadow();
        iconGlow.setRadius(12);
        iconGlow.colorProperty().bind(sharedGlowEffect.colorProperty());
        path.setEffect(iconGlow);

        button.setGraphic(path);

        // Solid black background, clean white border on hover
        String baseStyle = "-fx-background-color: black; -fx-border-color: black; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: center; -fx-padding: 0;";
        String hoverStyle = "-fx-background-color: #111118; -fx-border-color: white; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: center; -fx-padding: 0;";

        button.setStyle(baseStyle);
        button.hoverProperty().addListener((obs, wasHover, isHover) -> {
            button.setStyle(isHover ? hoverStyle : baseStyle);
        });

        return button;
    }

    public static Label createWatermark() {
        Label authorLabel = new Label("@kvolegi");
        authorLabel.setFont(getFont(12, false));
        authorLabel.setTextFill(Color.web("#1c2230")); // Blends closely with black background
        authorLabel.setOpacity(0.35); // Barely noticeable per user request
        authorLabel.setMouseTransparent(true);
        authorLabel.setPickOnBounds(false);
        return authorLabel;
    }

    public static javafx.scene.Node createArcadeLogo() {
        VBox logoBox = new VBox(-12); // Clean vertical spacing
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setMouseTransparent(true);
        logoBox.setPickOnBounds(false);

        DropShadow innerGlow = new DropShadow();
        innerGlow.setRadius(16);
        innerGlow.colorProperty().bind(sharedGlowEffect.colorProperty());

        DropShadow outerBloom = new DropShadow();
        outerBloom.setRadius(48);
        outerBloom.colorProperty().bind(sharedGlowEffect.colorProperty());
        outerBloom.setInput(innerGlow);

        Text title = new Text("kvolegi");
        title.setFont(getFont(82, true));
        title.setFill(Color.web("#050508"));
        title.strokeProperty().bind(sharedGlowEffect.colorProperty());
        title.setStrokeWidth(2.6);
        title.setStrokeType(StrokeType.CENTERED);
        title.setEffect(outerBloom);
        title.setMouseTransparent(true);

        DropShadow arcadeInner = new DropShadow();
        arcadeInner.setRadius(18);
        arcadeInner.colorProperty().bind(sharedGlowEffect.colorProperty());

        DropShadow arcadeGlow = new DropShadow();
        arcadeGlow.setRadius(54); // 8% wider glow
        arcadeGlow.colorProperty().bind(sharedGlowEffect.colorProperty());
        arcadeGlow.setInput(arcadeInner);

        Text subtitle = new Text("arcade");
        subtitle.setFont(getFont(25, true));
        subtitle.setFill(Color.web("#050508"));
        subtitle.strokeProperty().bind(sharedGlowEffect.colorProperty());
        subtitle.setStrokeWidth(1.8);
        subtitle.setStrokeType(StrokeType.CENTERED);
        subtitle.setEffect(arcadeGlow);
        subtitle.setMouseTransparent(true);

        HBox subBox = new HBox(subtitle);
        subBox.setAlignment(Pos.CENTER);
        subBox.setTranslateX(60);
        subBox.setMouseTransparent(true);
        subBox.setPickOnBounds(false);

        logoBox.getChildren().addAll(title, subBox);
        return logoBox;
    }
}

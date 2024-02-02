package arcade.desktop;

/** Plain launcher so the app starts from a classpath jar without the JavaFX module check. */
public class Launcher {
    public static void main(String[] args) {
        javafx.application.Application.launch(Main.class, args);
    }
}

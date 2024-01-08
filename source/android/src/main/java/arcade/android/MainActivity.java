package arcade.android;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import arcade.App;

public class MainActivity extends Activity {
    private static final String TAG = "KvolegiArcade";
    private App app;
    private ArcadeView arcadeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Prevent unhandled crashes from killing process silently
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Uncaught exception in " + thread.getName(), throwable);
            }
        });

        super.onCreate(savedInstanceState);

        try {
            // Lock to Landscape matching desktop PC view
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

            // Fullscreen flags (safe across all Android versions)
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
            hideSystemUI();

            // Initialize pure Java core App & Store
            app = new App(new PrefsStore(this));
            app.numberPrompt = (title, currentVal, onSet) -> {
                runOnUiThread(() -> {
                    try {
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(title);

                        final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
                        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        input.setText(String.valueOf(currentVal));
                        input.setSelectAllOnFocus(true);
                        input.setPadding(40, 30, 40, 30);
                        builder.setView(input);

                        builder.setPositiveButton("SET", (dialog, which) -> {
                            try {
                                String txt = input.getText().toString().trim();
                                if (!txt.isEmpty()) {
                                    int val = Integer.parseInt(txt);
                                    onSet.accept(val);
                                }
                            } catch (Throwable ignored) {}
                            hideSystemUI();
                        });
                        builder.setNegativeButton("Cancel", (dialog, which) -> hideSystemUI());
                        builder.setOnDismissListener(d -> hideSystemUI());

                        android.app.AlertDialog dialog = builder.create();
                        if (dialog.getWindow() != null) {
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                        dialog.show();
                        input.requestFocus();
                    } catch (Throwable t) {
                        Log.e(TAG, "Error showing number prompt", t);
                    }
                });
            };

            arcadeView = new ArcadeView(this, app);
            setContentView(arcadeView);
        } catch (Throwable t) {
            Log.e(TAG, "Error in onCreate", t);
        }
    }

    private void hideSystemUI() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                View decorView = getWindow().getDecorView();
                if (decorView != null) {
                    decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (app != null) {
                app.back();
                if (app.quitRequested) {
                    finish();
                }
            } else {
                super.onBackPressed();
            }
        } catch (Throwable t) {
            super.onBackPressed();
        }
    }
}

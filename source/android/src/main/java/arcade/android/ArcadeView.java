package arcade.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import arcade.App;

public class ArcadeView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "ArcadeView";
    private final App app;
    private final AndroidGfx gfx;
    private Thread renderThread;
    private volatile boolean running;
    private volatile boolean surfaceReady = false;

    public ArcadeView(Context context, App app) {
        super(context);
        this.app = app;
        this.gfx = new AndroidGfx();

        try {
            Typeface bold = null;
            try {
                bold = Typeface.createFromAsset(context.getAssets(), "fonts/ComicSansMS.ttf");
            } catch (Throwable ignored) {}
            if (bold == null) {
                try {
                    bold = Typeface.createFromAsset(context.getAssets(), "fonts/Fredoka-Bold.ttf");
                } catch (Throwable ignored) {}
            }
            Typeface reg = null;
            try {
                reg = Typeface.createFromAsset(context.getAssets(), "fonts/Nunito-Regular.ttf");
            } catch (Throwable ignored) {}

            gfx.setTypefaces(
                reg != null ? reg : Typeface.DEFAULT,
                bold != null ? bold : Typeface.DEFAULT_BOLD
            );
        } catch (Throwable t) {
            Log.w(TAG, "Using default system typefaces", t);
        }

        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (renderThread == null || !renderThread.isAlive()) {
            running = true;
            renderThread = new Thread(this, "ArcadeRenderLoop");
            renderThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceReady = (width > 0 && height > 0);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        running = false;
        try {
            if (renderThread != null) {
                renderThread.join(600);
            }
        } catch (InterruptedException ignored) {}
        renderThread = null;
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        SurfaceHolder holder = getHolder();

        while (running) {
            if (!surfaceReady) {
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                continue;
            }

            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - lastTime) / 1_000_000_000f);
            lastTime = now;

            Canvas canvas = null;
            try {
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    try {
                        canvas = holder.lockHardwareCanvas();
                    } catch (Throwable ignored) {}
                }
                if (canvas == null) {
                    canvas = holder.lockCanvas();
                }
                if (canvas != null && canvas.getWidth() > 0 && canvas.getHeight() > 0) {
                    gfx.setCanvas(canvas);
                    app.frame(gfx, canvas.getWidth(), canvas.getHeight(), dt);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error during frame rendering", t);
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Throwable ignored) {}
                }
            }

            if (app.quitRequested) {
                running = false;
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (getContext() instanceof Activity) {
                            ((Activity) getContext()).finish();
                        }
                    }
                });
                break;
            }

            // Target ~60 FPS
            long elapsedMs = (System.nanoTime() - now) / 1_000_000;
            long sleepMs = 16 - elapsedMs;
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            float x = event.getX();
            float y = event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    app.ui.pointerDown(x, y, false);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    app.ui.pointerMove(x, y);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    app.ui.pointerUp(x, y);
                    return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in touch event", t);
        }
        return super.onTouchEvent(event);
    }
}

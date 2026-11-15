package megalodonte.base;

import android.os.Handler;
import android.os.Looper;
import java.util.Objects;

/**
 * Helper for running code on the Android main (UI) thread.
 * <p>
 * Android counterpart of the desktop {@code megalodonte.base.UI}: it wraps the
 * native {@link Handler} of the main {@link Looper}, the Android analogue of
 * JavaFX {@code Platform.runLater}. Use it when background work (threads,
 * executors or asynchronous callbacks) needs to change {@code State},
 * {@code ListState} or any other value that feeds the mounted UI. The runtime
 * then pushes the update to the native views on the main thread.
 * </p>
 * <p>
 * The bridge only accepts commands on the main thread, so a state change made
 * from a background thread without this helper fails explicitly.
 * </p>
 */
public final class UI {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private UI() { }

    /**
     * Executes {@code task} on the Android main thread.
     * <p>
     * When the caller is already on the main thread the task runs immediately;
     * otherwise it is posted to the main looper queue. This matches
     * {@code Activity.runOnUiThread} and keeps updates made from UI callbacks
     * synchronous.
     * </p>
     *
     * @param task the code to execute on the UI thread
     */
    public static void runOnUi(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (Looper.myLooper() == Looper.getMainLooper()) task.run();
        else MAIN.post(task);
    }
}

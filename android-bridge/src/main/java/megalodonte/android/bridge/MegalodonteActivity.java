package megalodonte.android.bridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Insets;
import android.os.Build;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import java.util.HashMap;
import java.util.Map;
import megalodonte.application.Context;
import megalodonte.application.MegalodonteApp;

/** Android host: the client only connects its usual Main entry point. */
public abstract class MegalodonteActivity extends Activity {
    private static final String SCREEN_STATE = "megalodonte.screen";
    private Context application;
    protected abstract void main();
    @Override public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        megalodonte.base.scale.ScaleProvider.setDetector(() -> 1.0);
        FrameLayout host = new FrameLayout(this);
        host.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(host);
        host.requestApplyInsets();
        Map<String, String> restored = new HashMap<>();
        Bundle screenState = savedInstanceState == null ? null : savedInstanceState.getBundle(SCREEN_STATE);
        if (screenState != null) for (String key : screenState.keySet()) {
            String value = screenState.getString(key);
            if (value != null) restored.put(key, value);
        }
        application = MegalodonteApp.launch(new AndroidNativeBridge(host), restored, this::main);
    }
    @Override protected final void onSaveInstanceState(Bundle outState) {
        Bundle screenState = new Bundle();
        application.saveState().forEach(screenState::putString);
        outState.putBundle(SCREEN_STATE, screenState);
        super.onSaveInstanceState(outState);
    }
    @Override protected final void onDestroy() {
        if (application != null) application.close(isFinishing());
        super.onDestroy();
    }
}

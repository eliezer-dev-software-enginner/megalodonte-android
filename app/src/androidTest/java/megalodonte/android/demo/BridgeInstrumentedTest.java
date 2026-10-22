package megalodonte.android.demo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicInteger;
import megalodonte.android.bridge.AndroidNativeBridge;
import megalodonte.android.core.BridgeRuntime;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.base.theme.ThemeColors;
import megalodonte.base.theme.ThemeTypography;
import megalodonte.base.theme.ThemeSpacing;
import megalodonte.base.theme.ThemePadding;
import megalodonte.base.theme.ThemeBorder;
import megalodonte.components.Text;
import megalodonte.components.Button;
import megalodonte.components.Checkbox;
import megalodonte.components.ProgressBar;
import megalodonte.components.Scroll;
import megalodonte.components.inputs.Input;
import megalodonte.components.inputs.OnChangeResult;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.v2.Show;

/** Native-widget tests without a test-framework dependency; run with adb am instrument. */
public final class BridgeInstrumentedTest extends Instrumentation {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            Intent launch = new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext(), "my_app.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = startActivitySync(launch);
            Activity testActivity = activity;
            runOnMainSync(() -> {
                require(containsText(testActivity.findViewById(android.R.id.content), "Hello World"), "Real client did not render");
                FrameLayout host = new FrameLayout(testActivity);
                State<String> inputState = State.of("start");
                State<Boolean> checked = State.of(false);
                State<Integer> progress = State.of(10);
                State<Boolean> visible = State.of(false);
                AtomicInteger inputChanges = new AtomicInteger();
                Input input = new Input(inputState).onChange(value -> {
                    inputChanges.incrementAndGet();
                    return OnChangeResult.same(value.toUpperCase(java.util.Locale.ROOT));
                });
                Checkbox checkbox = new Checkbox("Check", checked);
                ProgressBar bar = new ProgressBar(progress);
                Text derived = new Text(inputState.map(value -> "Length: " + value.length()));
                Show show = Show.when(visible, () -> new Text("visible"));
                Button toggle = new Button("toggle").onClick(() -> visible.set(!visible.get()));
                Scroll root = new Scroll(new Column().children(input, derived,
                    new Row().children(checkbox, toggle), bar, show));
                AndroidNativeBridge bridge = new AndroidNativeBridge(host);
                BridgeRuntime runtime = new BridgeRuntime(bridge, root);
                ThemeInterface originalTheme = ThemeManager.theme();
                try {
                    EditText nativeInput = host.findViewWithTag(input.bridgeId());
                    CheckBox nativeCheckbox = host.findViewWithTag(checkbox.bridgeId());
                    TextView nativeDerived = host.findViewWithTag(derived.bridgeId());
                    View nativeShow = host.findViewWithTag(show.bridgeId());
                    android.widget.ProgressBar nativeProgress = host.findViewWithTag(bar.bridgeId());
                    require(host.getChildAt(0) instanceof android.widget.ScrollView, "Scroll must be native");
                    require(nativeShow.getVisibility() == View.GONE, "Hidden branch must release layout space");
                    nativeInput.setText("native");
                    require(inputState.get().equals("NATIVE") && nativeInput.getText().toString().equals("NATIVE"), "Formatted input feedback failed");
                    require(inputChanges.get() == 1 && nativeDerived.getText().toString().equals("Length: 6"), "Input loop/derived binding failed");
                    try {
                        bridge.setText(input.bridgeId(), "bypass");
                        throw new AssertionError("setText accepted an Input");
                    } catch (IllegalArgumentException expected) { }
                    require(inputState.get().equals("NATIVE") && nativeInput.getText().toString().equals("NATIVE"),
                        "Rejected text update changed the input");
                    nativeCheckbox.setChecked(true);
                    require(checked.get(), "Native checkbox must update state");
                    progress.set(65);
                    require(nativeProgress.getProgress() == 65, "Native progress binding failed");
                    host.findViewWithTag(toggle.bridgeId()).performClick();
                    require(nativeShow.getVisibility() == View.VISIBLE, "Show click must update native visibility");
                    ThemeManager.setTheme(new ThemeInterface() {
                        @Override public ThemeColors colors() {
                            ThemeColors c = originalTheme.colors();
                            return new ThemeColors(c.background(), c.surface(), "#AA3300", c.secondary(), "#112233",
                                c.textSecondary(), c.border(), c.placeholder(), c.selection(), c.focusRing(), c.hover(), c.success(), c.warning(), c.danger());
                        }
                        @Override public ThemeTypography typography() { return originalTheme.typography(); }
                        @Override public ThemeSpacing spacing() { return originalTheme.spacing(); }
                        @Override public ThemePadding padding() { return originalTheme.padding(); }
                        @Override public ThemeBorder border() { return originalTheme.border(); }
                    });
                    require(nativeDerived.getCurrentTextColor() == 0xff112233 && nativeInput.getCurrentTextColor() == 0xff112233,
                        "Shared theme must restyle native text and input");
                    require(host.<View>findViewWithTag(toggle.bridgeId()).getBackgroundTintList().getDefaultColor() == 0xffaa3300,
                        "Shared primary token must restyle native buttons");
                    runtime.close();
                    ThemeManager.setTheme(originalTheme);
                    require(nativeDerived.getCurrentTextColor() == 0xff112233, "Disposed runtime kept observing global theme");
                    nativeCheckbox.setChecked(false);
                    nativeInput.setText("disposed");
                    inputState.set("after");
                    require(checked.get() && inputChanges.get() == 1 && nativeInput.getText().toString().equals("disposed"), "Native listeners/bindings survived disposal");
                    require(host.getChildCount() == 0, "Host must release native views");
                } finally { runtime.close(); ThemeManager.setTheme(originalTheme); }
            });
            result.putString("stream", "Native client/render/input/checkbox/progress/show/theme/disposal tests passed\n");
            result.putString("megalodonte.result", "passed");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));
            result.putString("megalodonte.result", "failed");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) {
                Activity closing = activity;
                runOnMainSync(closing::finish);
            }
        }
    }
    private static boolean containsText(View view, String value) {
        if (view instanceof TextView text && value.equals(text.getText().toString())) return true;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++)
            if (containsText(group.getChildAt(i), value)) return true;
        return false;
    }
}

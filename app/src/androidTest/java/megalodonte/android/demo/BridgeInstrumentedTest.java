package megalodonte.android.demo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import megalodonte.android.bridge.AndroidNativeBridge;
import megalodonte.android.core.BridgeRuntime;
import megalodonte.base.UI;
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
import megalodonte.components.Switch;
import megalodonte.components.ProgressBar;
import megalodonte.components.Scroll;
import megalodonte.components.inputs.Input;
import megalodonte.components.inputs.OnChangeResult;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.v2.Show;
import megalodonte.v2.ListState;

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
                State<Boolean> switched = State.of(false);
                State<Integer> progress = State.of(10);
                State<Boolean> visible = State.of(false);
                AtomicInteger inputChanges = new AtomicInteger();
                Input input = new Input(inputState).onChange(value -> {
                    inputChanges.incrementAndGet();
                    return OnChangeResult.same(value.toUpperCase(java.util.Locale.ROOT));
                });
                Checkbox checkbox = new Checkbox("Check", checked);
                AtomicInteger switchChanges = new AtomicInteger();
                Switch switchControl = new Switch("Notifications", switched)
                    .onCheckedChange(value -> switchChanges.incrementAndGet());
                ProgressBar bar = new ProgressBar(progress);
                Text derived = new Text(inputState.map(value -> "Length: " + value.length()));
                Show show = Show.when(visible, () -> new Text("visible"));
                Button toggle = new Button("toggle").onClick(() -> visible.set(!visible.get()));
                Scroll root = new Scroll(new Column().children(input, derived,
                    new Row().children(checkbox, switchControl, toggle), bar, show));
                AndroidNativeBridge bridge = new AndroidNativeBridge(host);
                BridgeRuntime runtime = new BridgeRuntime(bridge, root);
                ThemeInterface originalTheme = ThemeManager.theme();
                try {
                    EditText nativeInput = host.findViewWithTag(input.bridgeId());
                    CheckBox nativeCheckbox = host.findViewWithTag(checkbox.bridgeId());
                    android.widget.CompoundButton nativeSwitch = host.findViewWithTag(switchControl.bridgeId());
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
                    nativeSwitch.setChecked(true);
                    require(switched.get() && switchChanges.get() == 1
                            && "Notifications".contentEquals(nativeSwitch.getContentDescription()),
                        "Native switch must update state and expose its accessible label");
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
                    nativeSwitch.setChecked(false);
                    nativeInput.setText("disposed");
                    inputState.set("after");
                    require(checked.get() && switched.get() && switchChanges.get() == 1 && inputChanges.get() == 1
                        && nativeInput.getText().toString().equals("disposed"),
                        "Native listeners/bindings survived disposal");
                    require(host.getChildCount() == 0, "Host must release native views");
                    FrameLayout listHost = new FrameLayout(testActivity);
                    ListState<String> names = ListState.of(new ArrayList<>(List.of("Ada", "Grace")));
                    AtomicInteger listClicks = new AtomicInteger();
                    Column listColumn = new Column().items(names, name ->
                        new Button(name).onClick(() -> listClicks.incrementAndGet()));
                    AndroidNativeBridge listBridge = new AndroidNativeBridge(listHost);
                    BridgeRuntime listRuntime = new BridgeRuntime(listBridge, listColumn);
                    try {
                        LinearLayout layout = (LinearLayout) listHost.getChildAt(0);
                        require(buttonLabels(layout).equals(List.of("Ada", "Grace")), "Reactive list must mount its items");
                        names.add("Linus");
                        require(buttonLabels(layout).equals(List.of("Ada", "Grace", "Linus")), "Reactive list must add native views");
                        buttonWithText(layout, "Linus").performClick();
                        require(listClicks.get() == 1, "Reactive list buttons must dispatch");
                        android.widget.Button removed = buttonWithText(layout, "Ada");
                        names.remove("Ada");
                        require(buttonLabels(layout).equals(List.of("Grace", "Linus")), "Reactive list must remove native views");
                        removed.performClick();
                        require(listClicks.get() == 1, "Removed list buttons must stop dispatching");
                        names.clear();
                        require(buttonLabels(layout).isEmpty(), "Reactive list must release cleared views");

                        State<String> suffix = State.of("!");
                        ListState<String> labels = ListState.of(new ArrayList<>(List.of("A")));
                        Column textColumn = new Column().items(labels, label ->
                            new Text(suffix.map(value -> label + value)));
                        FrameLayout textHost = new FrameLayout(testActivity);
                        AndroidNativeBridge textBridge = new AndroidNativeBridge(textHost);
                        BridgeRuntime textRuntime = new BridgeRuntime(textBridge, textColumn);
                        try {
                            LinearLayout textLayout = (LinearLayout) textHost.getChildAt(0);
                            require(textValues(textLayout).equals(List.of("A!")), "Mapped text must mount");
                            labels.add("B");
                            suffix.set("?");
                            require(textValues(textLayout).equals(List.of("A?", "B?")), "Mapped text states must bind");
                        } finally { textRuntime.close(); }
                    } finally { listRuntime.close(); }
                    require(listHost.getChildCount() == 0, "Reactive list host must be released");
                } finally { runtime.close(); ThemeManager.setTheme(originalTheme); }
            });
            // UI.runOnUi: immediate on the main thread, posted from background
            // threads and able to feed mounted state reactively.
            AtomicReference<Thread> helperThread = new AtomicReference<>();
            runOnMainSync(() -> {
                UI.runOnUi(() -> helperThread.set(Thread.currentThread()));
                require(helperThread.get() == Thread.currentThread(),
                    "UI.runOnUi on the main thread must run immediately");
            });
            FrameLayout helperHost = new FrameLayout(getTargetContext());
            State<String> remote = State.of("before");
            Text remoteText = new Text(remote);
            BridgeRuntime[] helperRuntime = new BridgeRuntime[1];
            runOnMainSync(() -> helperRuntime[0] = new BridgeRuntime(
                new AndroidNativeBridge(helperHost), new Column().children(remoteText)));
            try {
                AtomicReference<Thread> postedThread = new AtomicReference<>();
                CountDownLatch posted = new CountDownLatch(1);
                Thread worker = new Thread(() -> UI.runOnUi(() -> {
                    postedThread.set(Thread.currentThread());
                    remote.set("from background");
                    posted.countDown();
                }), "ui-helper-test");
                worker.start();
                require(posted.await(10, TimeUnit.SECONDS), "UI.runOnUi must reach the main thread");
                require(postedThread.get() == Looper.getMainLooper().getThread(),
                    "UI.runOnUi from background must execute on the main thread");
                runOnMainSync(() -> require("from background".equals(
                    ((TextView) helperHost.findViewWithTag(remoteText.bridgeId())).getText().toString()),
                    "Background state update through UI.runOnUi must reach native views"));
            } finally {
                runOnMainSync(() -> helperRuntime[0].close());
            }
            require(helperHost.getChildCount() == 0, "Helper host must release native views");
            result.putString("stream", "Native client/render/input/checkbox/progress/show/theme/reactive-list/ui-helper/disposal tests passed\n");
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
    private static List<String> buttonLabels(LinearLayout layout) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            if (child instanceof android.widget.Button button) labels.add(button.getText().toString());
        }
        return labels;
    }
    private static android.widget.Button buttonWithText(LinearLayout layout, String text) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof android.widget.Button button && text.contentEquals(button.getText())) return button;
        }
        throw new AssertionError("Missing native button: " + text);
    }
    private static List<String> textValues(LinearLayout layout) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            if (child instanceof TextView text && !(child instanceof android.widget.Button)) values.add(text.getText().toString());
        }
        return values;
    }
    private static boolean containsText(View view, String value) {
        if (view instanceof TextView text && value.equals(text.getText().toString())) return true;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++)
            if (containsText(group.getChildAt(i), value)) return true;
        return false;
    }
}

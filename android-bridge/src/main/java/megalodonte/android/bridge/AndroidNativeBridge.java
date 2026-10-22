package megalodonte.android.bridge;

import android.os.Looper;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import megalodonte.android.core.NativeBridge;
import megalodonte.base.components.Component;
import megalodonte.base.theme.ThemeManager;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.components.Text;
import megalodonte.components.Button;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.LinearComponent;
import megalodonte.components.layout_components.Row;
import megalodonte.components.layout_components.Column;
import megalodonte.components.inputs.Input;
import megalodonte.components.Checkbox;
import megalodonte.components.ProgressBar;
import megalodonte.components.Scroll;
import megalodonte.components.Select;
import megalodonte.components.DatePicker;
import megalodonte.components.Image;
import megalodonte.v2.Show;
import megalodonte.base.state.State;
import org.json.JSONArray;
import org.json.JSONObject;
import megalodonte.base.scale.ScaleProvider;
import megalodonte.contracts.BackendContract;
import megalodonte.contracts.Capability;

/** Renders actual Android widgets. No WebView, JavaScript engine or JavaFX. */
public final class AndroidNativeBridge implements NativeBridge {
    private final ViewGroup host;
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, Component> components = new HashMap<>();
    private Consumer<Event> events;
    private final List<Runnable> cleanup = new ArrayList<>();
    private final Set<String> updating = new HashSet<>();
    public AndroidNativeBridge(ViewGroup host) { this.host = host; }
    private void checkThread() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("The bridge must run on the Android UI thread");
    }
    @Override public void mount(Component root, Consumer<Event> events) {
        checkThread();
        close();
        this.events = events;
        host.addView(create(root), new ViewGroup.LayoutParams(-1, -1));
    }
    private View create(Component component) {
        View view;
        if (component instanceof Text text) {
            TextView nativeText = new TextView(host.getContext());
            ThemeInterface theme = ThemeManager.getTheme();
            nativeText.setText(text.value());
            nativeText.setTextSize(text.props().getFontSize() == null ? theme.typography().body() : ScaleProvider.scale(text.props().getFontSize()));
            nativeText.setTextColor(color(theme.colors().textPrimary()));
            nativeText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            view = nativeText;
        } else if (component instanceof Button button) {
            android.widget.Button nativeButton = new android.widget.Button(host.getContext());
            nativeButton.setText(button.label());
            nativeButton.setTextSize(button.props().getFontSize() == null
                ? ThemeManager.theme().typography().body() : ScaleProvider.scale(button.props().getFontSize()));
            nativeButton.setLayoutParams(new ViewGroup.LayoutParams(button.props().isFillWidth() ? -1 : -2,
                    button.props().getHeight() < 0 ? -2 : dp(button.props().getHeight())));
            nativeButton.setOnClickListener(v -> {
                if (events != null) events.accept(new Event(component.bridgeId(), "click"));
            });
            view = nativeButton;
        } else if (component instanceof AndroidJsoncControl control) {
            view = control.create(host.getContext(), cleanup);
        } else if (component instanceof Select select) {
            view = new AndroidJsoncControl("Select", new JSONObject(),
                    State.of(new JSONArray(select.options).toString()), select.state,
                    null, null, null, null, null).create(host.getContext(), cleanup);
        } else if (component instanceof DatePicker datePicker) {
            view = new AndroidJsoncControl("DatePicker", new JSONObject(),
                    null, datePicker.state, null, null, null, null, null)
                    .create(host.getContext(), cleanup);
        } else if (component instanceof Image image) {
            JSONObject props = new JSONObject();
            try {
                props.put("description", image.description);
                props.put("width", image.width);
                props.put("height", image.height);
            } catch (org.json.JSONException error) {
                throw new IllegalStateException("Could not configure image", error);
            }
            view = new AndroidJsoncControl("Image", props, image.source,
                    null, null, null, null, null, null).create(host.getContext(), cleanup);
        } else if (component instanceof megalodonte.components.SpacerVertical || component instanceof megalodonte.components.SpacerHorizontal) {
            view = new View(host.getContext());
        } else if (component instanceof Input input) {
            EditText field = new EditText(host.getContext());
            field.setInputType(InputType.TYPE_CLASS_TEXT | (input.password() ? InputType.TYPE_TEXT_VARIATION_PASSWORD : 0)
                | (input.multiline() ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
            field.setSingleLine(!input.multiline());
            field.setText(input.value());
            field.setHint(input.props.getPlaceholder());
            field.setEnabled(!input.props.isDisabled());
            if (!input.props.isEditable()) field.setKeyListener(null);
            field.setImeOptions(input.multiline() ? EditorInfo.IME_FLAG_NO_ENTER_ACTION : EditorInfo.IME_ACTION_DONE);
            field.setLayoutParams(new ViewGroup.LayoutParams(input.props.getWidth() < 0 ? input.props.getWidth() : dp(input.props.getWidth()),
                    input.props.getHeight() < 0 ? (input.multiline() ? dp(120) : -2) : dp(input.props.getHeight())));
            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable value) {
                    if (events != null && !updating.contains(input.bridgeId())) events.accept(new Event(input.bridgeId(), "change", value.toString()));
                }
            };
            field.addTextChangedListener(watcher);
            field.setOnEditorActionListener((v, action, event) -> {
                if (action != EditorInfo.IME_ACTION_DONE && !(event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) return false;
                if (events != null) events.accept(new Event(input.bridgeId(), "enter"));
                return true;
            });
            cleanup.add(() -> { field.removeTextChangedListener(watcher); field.setOnEditorActionListener(null); });
            view = field;
        } else if (component instanceof Checkbox checkbox) {
            CheckBox field = new CheckBox(host.getContext());
            field.setText(checkbox.label);
            field.setChecked(Boolean.TRUE.equals(checkbox.state.get()));
            field.setOnCheckedChangeListener((button, value) -> {
                if (events != null && !updating.contains(checkbox.bridgeId())) events.accept(new Event(checkbox.bridgeId(), "change", value));
            });
            cleanup.add(() -> field.setOnCheckedChangeListener(null));
            view = field;
        } else if (component instanceof ProgressBar progress) {
            android.widget.ProgressBar field = new android.widget.ProgressBar(host.getContext(), null, android.R.attr.progressBarStyleHorizontal);
            field.setMax(100);
            view = field;
        } else if (component instanceof Scroll scroll) {
            ScrollView field = new ScrollView(host.getContext());
            field.setFillViewport(true);
            field.addView(create(scroll.children().get(0)), new ScrollView.LayoutParams(-1, -2));
            view = field;
        } else if (component instanceof Show show) {
            FrameLayout field = new FrameLayout(host.getContext());
            for (Component child : show.children()) field.addView(create(child), new FrameLayout.LayoutParams(-2, -2));
            view = field;
        } else if (component instanceof LinearComponent container) {
            LinearLayout layout = new LinearLayout(host.getContext());
            layout.setOrientation(component instanceof Row ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            layout.setGravity((container.props().isCenterVertical() ? Gravity.CENTER_VERTICAL : Gravity.TOP)
                | (container.props().isCenterHorizontal() ? Gravity.CENTER_HORIZONTAL : Gravity.START));
            layout.setBackgroundColor(color(container.props().getBgColor() == null ? ThemeManager.theme().colors().background() : container.props().getBgColor()));
            int padding = dp(container.props().getPadding());
            layout.setPadding(padding, padding, padding, padding);
            for (Component child : container.children()) {
                int width = -2, height = -2;
                if (child instanceof LinearComponent childLayout) {
                    width = dimension(childLayout.props().getWidth()); height = dimension(childLayout.props().getHeight());
                }
                View childView = create(child);
                ViewGroup.LayoutParams requested = childView.getLayoutParams();
                if (requested != null) {
                    if (width == -2 && requested.width != -2) width = requested.width;
                    if (height == -2 && requested.height != -2) height = requested.height;
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                if (height == -1 && layout.getOrientation() == LinearLayout.VERTICAL) { params.height = 0; params.weight = 1; }
                if (width == -1 && layout.getOrientation() == LinearLayout.HORIZONTAL) { params.width = 0; params.weight = 1; }
                if (child instanceof megalodonte.components.SpacerVertical space) {
                    params.width = 0; params.height = space.flexible && !(component instanceof Row) ? 0 : dp(space.size.get());
                    if (space.flexible && !(component instanceof Row)) params.weight = 1;
                } else if (child instanceof megalodonte.components.SpacerHorizontal space) {
                    params.height = 0; params.width = space.flexible && component instanceof Row ? 0 : dp(space.size.get());
                    if (space.flexible && component instanceof Row) params.weight = 1;
                }
                if (layout.getChildCount() > 0) {
                    if (component instanceof Row) params.setMarginStart(dp(container.props().getSpacing()));
                    else params.topMargin = dp(container.props().getSpacing());
                }
                layout.addView(childView, params);
            }
            view = layout;
        } else throw new IllegalArgumentException("Unsupported component: " + component.getClass());
        view.setTag(component.bridgeId());
        components.put(component.bridgeId(), component);
        if (views.putIfAbsent(component.bridgeId(), view) != null)
            throw new IllegalArgumentException("Duplicate component id: " + component.bridgeId());
        return view;
    }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
    private int dimension(double value) { return value < 0 ? (int) value : dp((int) Math.round(value)); }
    private int color(String value) { return "transparent".equals(value) ? Color.TRANSPARENT : Color.parseColor(value); }
    @Override public BackendContract contract() {
        return new BackendContract("android", BackendContract.CURRENT_VERSION,
            Set.of(Capability.TEXT, Capability.BUTTON, Capability.CONTAINER, Capability.REACTIVE_STATE, Capability.THEME_TOKENS,
                Capability.INPUT, Capability.CHECKBOX, Capability.PROGRESS, Capability.ROW, Capability.COLUMN,
                Capability.SCROLL, Capability.CONDITIONAL_RENDERING, Capability.SPACING));
    }
    @Override public void applyTheme(ThemeInterface theme) {
        checkThread();
        host.setBackgroundColor(color(theme.colors().background()));
        views.forEach((id, view) -> {
            Component component = components.get(id);
            if (view instanceof TextView nativeText) {
                if (component instanceof Button button) {
                    nativeText.setTextColor(color(theme.colors().background()));
                    view.setBackgroundTintList(ColorStateList.valueOf(color(theme.colors().primary())));
                    nativeText.setTextSize(button.props().getFontSize() == null
                        ? theme.typography().body() : ScaleProvider.scale(button.props().getFontSize()));
                } else if (component instanceof Text text) {
                    nativeText.setTextColor(color(theme.colors().textPrimary()));
                    nativeText.setTextSize(text.props().getFontSize() == null
                        ? theme.typography().body() : ScaleProvider.scale(text.props().getFontSize()));
                } else if (component instanceof Input input) {
                    nativeText.setTextColor(color(theme.colors().textPrimary()));
                    nativeText.setHintTextColor(color(theme.colors().placeholder()));
                    nativeText.setTextSize(input.props.getFontSize() == null ? theme.typography().body() : ScaleProvider.scale(input.props.getFontSize()));
                } else if (component instanceof Checkbox checkbox) {
                    nativeText.setTextColor(color(theme.colors().textPrimary()));
                    nativeText.setTextSize(checkbox.props.getFontSize() == null ? theme.typography().body() : ScaleProvider.scale(checkbox.props.getFontSize()));
                    ((CheckBox) view).setButtonTintList(ColorStateList.valueOf(color(theme.colors().primary())));
                }
                if (theme.typography().fontFamily() != null)
                    nativeText.setTypeface(Typeface.create(theme.typography().fontFamily(), Typeface.NORMAL));
                else nativeText.setTypeface(Typeface.DEFAULT);
            } else if (component instanceof LinearComponent layout)
                view.setBackgroundColor(color(layout.props().getBgColor() == null ? theme.colors().background() : layout.props().getBgColor()));
            else if (view instanceof android.widget.ProgressBar progress)
                progress.setProgressTintList(ColorStateList.valueOf(color(theme.colors().primary())));
        });
    }
    @Override public void setValue(String id, Object value) {
        checkThread();
        View view = views.get(id);
        Component component = components.get(id);
        if (view == null) throw new IllegalArgumentException("Unknown mounted component: " + id);
        updating.add(id);
        try {
            if (component instanceof Input && value instanceof String text) {
                EditText field = (EditText) view;
                if (!field.getText().toString().equals(text)) { field.setText(text); field.setSelection(text.length()); }
            } else if (component instanceof Checkbox && (value == null || value instanceof Boolean))
                ((CheckBox) view).setChecked(Boolean.TRUE.equals(value));
            else if (component instanceof ProgressBar && (value == null || value instanceof Integer))
                ((android.widget.ProgressBar) view).setProgress(value == null ? 0 : Math.max(0, Math.min(100, (Integer) value)));
            else if (component instanceof Show show && (value == null || value instanceof Boolean)) {
                boolean visible = Boolean.TRUE.equals(value);
                ViewGroup group = (ViewGroup) view;
                group.getChildAt(0).setVisibility(visible ? View.VISIBLE : View.GONE);
                if (group.getChildCount() > 1) group.getChildAt(1).setVisibility(visible ? View.GONE : View.VISIBLE);
                view.setVisibility(visible || group.getChildCount() > 1 ? View.VISIBLE : View.GONE);
            } else if (value instanceof Integer size && (component instanceof megalodonte.components.SpacerVertical || component instanceof megalodonte.components.SpacerHorizontal)) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                if (component instanceof megalodonte.components.SpacerVertical space && !space.flexible) params.height = dp(Math.max(0, size));
                if (component instanceof megalodonte.components.SpacerHorizontal space && !space.flexible) params.width = dp(Math.max(0, size));
                view.setLayoutParams(params);
            } else throw new IllegalArgumentException("Invalid property update for " + component.getClass().getSimpleName());
        } finally { updating.remove(id); }
    }
    @Override public void setText(String id, String value) {
        checkThread();
        View view = views.get(id);
        if (!(components.get(id) instanceof Text) || !(view instanceof TextView))
            throw new IllegalArgumentException("Not a mounted Text: " + id);
        ((TextView) view).setText(value);
    }
    @Override public void close() {
        checkThread();
        events = null;
        cleanup.forEach(Runnable::run);
        cleanup.clear();
        updating.clear();
        views.values().forEach(view -> view.setOnClickListener(null));
        views.clear();
        components.clear();
        host.removeAllViews();
    }
}

package megalodonte.android.bridge;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Looper;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.widget.Toolbar;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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
import megalodonte.common_platform.ButtonVariant;
import megalodonte.components.Text;
import megalodonte.components.Button;
import megalodonte.components.layout_components.LinearComponent;
import megalodonte.components.layout_components.Row;
import megalodonte.components.inputs.Input;
import megalodonte.components.Checkbox;
import megalodonte.components.ProgressBar;
import megalodonte.components.Scroll;
import megalodonte.components.Select;
import megalodonte.components.DatePicker;
import megalodonte.components.Image;
import megalodonte.components.MenuBar;
import megalodonte.props.ButtonProps;
import megalodonte.props.InputProps;
import megalodonte.v2.Show;
import megalodonte.v2.ForEach;
import megalodonte.base.state.State;
import org.json.JSONArray;
import org.json.JSONObject;
import megalodonte.base.scale.ScaleProvider;
import megalodonte.contracts.BackendContract;
import megalodonte.contracts.Capability;

/** Renders actual Android widgets. No WebView, JavaScript engine or JavaFX. */
public final class AndroidNativeBridge implements NativeBridge {
    /** One reactive list block, rendered inline inside its parent layout. */
    private static final class ListBlock {
        final LinearComponent container;
        final LinearLayout parent;
        final boolean horizontal;
        final int spacing;
        final View anchor;
        final List<Component> components = new ArrayList<>();
        final List<View> views = new ArrayList<>();
        ListBlock(LinearComponent container, LinearLayout parent, int spacing, View anchor) {
            this.container = container;
            this.parent = parent;
            this.horizontal = container instanceof Row;
            this.spacing = spacing;
            this.anchor = anchor;
        }
    }
    private final ViewGroup host;
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, EditText> inputFields = new HashMap<>();
    private final Map<String, com.google.android.material.textfield.TextInputLayout> inputLayouts = new HashMap<>();
    private final Map<String, Component> components = new HashMap<>();
    private final Map<String, ListBlock> listBlocks = new HashMap<>();
    private final Map<String, List<Runnable>> componentCleanup = new HashMap<>();
    private final Set<View> listAnchors = Collections.newSetFromMap(new IdentityHashMap<>());
    private Consumer<Event> events;
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
        List<Runnable> cleanup = new ArrayList<>();
        View view;
        if (component instanceof MenuBar menuBar) {
            view = AndroidMenuBar.create(host.getContext(), menuBar);
        } else if (component instanceof Text text) {
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
            styleButton(nativeButton, button.props(), ThemeManager.theme());
            nativeButton.setLayoutParams(new ViewGroup.LayoutParams(button.props().isFillWidth() ? -1 : -2,
                    button.props().getHeight() < 0 ? -2 : dp(button.props().getHeight())));
            nativeButton.setOnClickListener(v -> {
                if (events != null) {
                    events.accept(new Event(button.bridgeId(), "click"));
                    styleButton(nativeButton, button.props(), ThemeManager.theme());
                }
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
            boolean decorated = input.props.getVariant() != InputProps.Variant.PLAIN;
            Context fieldContext = decorated
                ? new ContextThemeWrapper(host.getContext(), com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
                : host.getContext();
            EditText field = decorated
                ? new com.google.android.material.textfield.TextInputEditText(fieldContext)
                : new EditText(fieldContext);
            field.setInputType(InputType.TYPE_CLASS_TEXT | (input.password() ? InputType.TYPE_TEXT_VARIATION_PASSWORD : 0)
                | (input.multiline() ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
            field.setSingleLine(!input.multiline());
            field.setText(input.value());
            field.setEnabled(!input.props.isDisabled());
            if (!input.props.isEditable()) field.setKeyListener(null);
            int imeAction = imeAction(input);
            field.setImeOptions(imeAction);
            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable value) {
                    if (events != null && !updating.contains(input.bridgeId())) events.accept(new Event(input.bridgeId(), "change", value.toString()));
                }
            };
            field.addTextChangedListener(watcher);
            field.setOnEditorActionListener((v, action, event) -> {
                boolean hardwareEnter = event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER;
                if (action == EditorInfo.IME_ACTION_NONE && (!hardwareEnter || input.multiline())) return false;
                int resolvedAction = action == EditorInfo.IME_ACTION_NONE ? imeAction : action;
                if (events != null) events.accept(new Event(input.bridgeId(), "keyboardAction",
                    (Runnable) () -> performDefaultImeAction(field, resolvedAction)));
                return true;
            });
            cleanup.add(() -> { field.removeTextChangedListener(watcher); field.setOnEditorActionListener(null); });
            inputFields.put(input.bridgeId(), field);
            if (decorated) {
                com.google.android.material.textfield.TextInputLayout layout =
                    new com.google.android.material.textfield.TextInputLayout(fieldContext);
                layout.setBoxBackgroundMode(input.props.getVariant() == InputProps.Variant.OUTLINED
                    ? com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                    : com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_FILLED);
                layout.setHint(input.props.getLabel());
                layout.setPlaceholderText(input.props.getPlaceholder());
                layout.addView(field, new com.google.android.material.textfield.TextInputLayout.LayoutParams(-1, -2));
                inputLayouts.put(input.bridgeId(), layout);
                view = layout;
            } else {
                field.setHint(input.props.getPlaceholder());
                view = field;
            }
            view.setLayoutParams(new ViewGroup.LayoutParams(
                input.props.getWidth() < 0 ? input.props.getWidth() : dp(input.props.getWidth()),
                input.props.getHeight() < 0 ? (input.multiline() ? dp(120) : -2) : dp(input.props.getHeight())));
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
                if (child instanceof ForEach<?> forEach) {
                    mountListBlock(layout, container, forEach);
                } else {
                    View childView = create(child);
                    layout.addView(childView, linearParams(container, child, childView));
                }
            }
            applySpacing(layout, container.props().getSpacing(), component instanceof Row);
            view = layout;
        } else if (component instanceof ForEach<?>) {
            throw new IllegalArgumentException("ForEach must be a direct child of a Column or Row");
        } else throw new IllegalArgumentException("Unsupported component: " + component.getClass());
        view.setTag(component.bridgeId());
        components.put(component.bridgeId(), component);
        if (views.putIfAbsent(component.bridgeId(), view) != null)
            throw new IllegalArgumentException("Duplicate component id: " + component.bridgeId());
        if (!cleanup.isEmpty()) componentCleanup.put(component.bridgeId(), cleanup);
        return view;
    }
    private void mountListBlock(LinearLayout layout, LinearComponent container, ForEach<?> forEach) {
        View anchor = new View(host.getContext());
        anchor.setVisibility(View.GONE);
        listAnchors.add(anchor);
        layout.addView(anchor, new LinearLayout.LayoutParams(0, 0));
        ListBlock block = new ListBlock(container, layout, container.props().getSpacing(), anchor);
        for (Component child : forEach.components()) {
            View childView = create(child);
            layout.addView(childView, linearParams(container, child, childView));
            block.components.add(child);
            block.views.add(childView);
        }
        if (listBlocks.putIfAbsent(forEach.bridgeId(), block) != null)
            throw new IllegalArgumentException("Duplicate reactive list id: " + forEach.bridgeId());
    }
    @Override public void setChildren(String componentId, List<Component> children) {
        checkThread();
        ListBlock block = listBlocks.get(componentId);
        if (block == null) throw new IllegalArgumentException("Not a mounted reactive list: " + componentId);
        List<Component> next = List.copyOf(children);
        for (Component previous : block.components) discard(previous);
        for (View previous : block.views) block.parent.removeView(previous);
        block.components.clear();
        block.views.clear();
        int index = block.parent.indexOfChild(block.anchor) + 1;
        for (Component child : next) {
            View childView = create(child);
            block.parent.addView(childView, index++, linearParams(block.container, child, childView));
            block.components.add(child);
            block.views.add(childView);
        }
        applySpacing(block.parent, block.spacing, block.horizontal);
    }
    private LinearLayout.LayoutParams linearParams(LinearComponent container, Component child, View childView) {
        boolean horizontal = container instanceof Row;
        int width = -2, height = -2;
        if (child instanceof LinearComponent childLayout) {
            width = dimension(childLayout.props().getWidth()); height = dimension(childLayout.props().getHeight());
        } else if (child instanceof Scroll && !horizontal) {
            width = -1;
        }
        ViewGroup.LayoutParams requested = childView.getLayoutParams();
        if (requested != null) {
            if (width == -2 && requested.width != -2) width = requested.width;
            if (height == -2 && requested.height != -2) height = requested.height;
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        if (height == -1 && !horizontal) { params.height = 0; params.weight = 1; }
        if (width == -1 && horizontal) { params.width = 0; params.weight = 1; }
        if (child instanceof megalodonte.components.SpacerVertical space) {
            params.width = 0; params.height = space.flexible && !horizontal ? 0 : dp(space.size.get());
            if (space.flexible && !horizontal) params.weight = 1;
        } else if (child instanceof megalodonte.components.SpacerHorizontal space) {
            params.height = 0; params.width = space.flexible && horizontal ? 0 : dp(space.size.get());
            if (space.flexible && horizontal) params.weight = 1;
        }
        return params;
    }
    private void applySpacing(LinearLayout layout, int spacing, boolean horizontal) {
        int visible = 0;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (listAnchors.contains(child)) continue;
            if (!(child.getLayoutParams() instanceof LinearLayout.LayoutParams params)) continue;
            if (visible == 0) {
                params.topMargin = 0;
                params.setMarginStart(0);
            } else if (horizontal) {
                params.setMarginStart(dp(spacing));
            } else {
                params.topMargin = dp(spacing);
            }
            child.setLayoutParams(params);
            visible++;
        }
    }
    private void discard(Component component) {
        if (component instanceof ForEach<?> forEach) {
            ListBlock block = listBlocks.remove(forEach.bridgeId());
            if (block != null) listAnchors.remove(block.anchor);
            for (Component child : forEach.components()) discard(child);
        } else {
            for (Component child : component.children()) discard(child);
        }
        components.remove(component.bridgeId());
        views.remove(component.bridgeId());
        inputFields.remove(component.bridgeId());
        inputLayouts.remove(component.bridgeId());
        List<Runnable> cleanup = componentCleanup.remove(component.bridgeId());
        if (cleanup != null) cleanup.forEach(Runnable::run);
    }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
    private int dimension(double value) { return value < 0 ? (int) value : dp((int) Math.round(value)); }
    private int color(String value) { return "transparent".equals(value) ? Color.TRANSPARENT : Color.parseColor(value); }
    @Override public BackendContract contract() {
        return new BackendContract("android", BackendContract.CURRENT_VERSION,
            Set.of(Capability.TEXT, Capability.BUTTON, Capability.CONTAINER, Capability.REACTIVE_STATE, Capability.THEME_TOKENS,
                Capability.INPUT, Capability.CHECKBOX, Capability.PROGRESS, Capability.ROW, Capability.COLUMN,
                Capability.SCROLL, Capability.CONDITIONAL_RENDERING, Capability.SPACING, Capability.LIST_RENDERING));
    }
    @Override public void applyTheme(ThemeInterface theme) {
        checkThread();
        host.setBackgroundColor(color(theme.colors().background()));
        views.forEach((id, view) -> {
            Component component = components.get(id);
            if (component instanceof Input input) {
                EditText field = inputFields.get(id);
                if (field != null) {
                    field.setTextColor(color(theme.colors().textPrimary()));
                    field.setHintTextColor(color(theme.colors().placeholder()));
                    field.setTextSize(input.props.getFontSize() == null ? theme.typography().body() : ScaleProvider.scale(input.props.getFontSize()));
                    field.setTypeface(theme.typography().fontFamily() == null
                        ? Typeface.DEFAULT : Typeface.create(theme.typography().fontFamily(), Typeface.NORMAL));
                }
                com.google.android.material.textfield.TextInputLayout layout = inputLayouts.get(id);
                if (layout != null) {
                    layout.setBoxStrokeColor(color(theme.colors().primary()));
                    layout.setDefaultHintTextColor(ColorStateList.valueOf(color(theme.colors().textPrimary())));
                    layout.setPlaceholderTextColor(ColorStateList.valueOf(color(theme.colors().placeholder())));
                }
                return;
            }
            if (view instanceof TextView nativeText) {
                if (component instanceof Button button) {
                    styleButton((android.widget.Button) view, button.props(), theme);
                } else if (component instanceof Text text) {
                    nativeText.setTextColor(color(theme.colors().textPrimary()));
                    nativeText.setTextSize(text.props().getFontSize() == null
                        ? theme.typography().body() : ScaleProvider.scale(text.props().getFontSize()));
                } else if (component instanceof Checkbox checkbox) {
                    nativeText.setTextColor(color(theme.colors().textPrimary()));
                    nativeText.setTextSize(checkbox.props.getFontSize() == null ? theme.typography().body() : ScaleProvider.scale(checkbox.props.getFontSize()));
                    ((CheckBox) view).setButtonTintList(ColorStateList.valueOf(color(theme.colors().primary())));
                }
                if (theme.typography().fontFamily() != null)
                    nativeText.setTypeface(Typeface.create(theme.typography().fontFamily(), Typeface.NORMAL));
                else nativeText.setTypeface(Typeface.DEFAULT);
            } else if (component instanceof MenuBar menuBar && view instanceof Toolbar toolbar)
                AndroidMenuBar.applyStyle(toolbar, menuBar, theme);
            else if (component instanceof LinearComponent layout)
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
                EditText field = inputFields.get(id);
                if (field == null) throw new IllegalStateException("Mounted Input has no native field: " + id);
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
        componentCleanup.values().forEach(runs -> runs.forEach(Runnable::run));
        componentCleanup.clear();
        listBlocks.clear();
        listAnchors.clear();
        updating.clear();
        views.values().forEach(view -> view.setOnClickListener(null));
        views.clear();
        inputFields.clear();
        inputLayouts.clear();
        components.clear();
        host.removeAllViews();
    }

    //------------------
    private int imeAction(Input input) {
        return switch (input.props.getImeAction()) {
            case DONE -> EditorInfo.IME_ACTION_DONE;
            case NEXT -> EditorInfo.IME_ACTION_NEXT;
            case PREVIOUS -> EditorInfo.IME_ACTION_PREVIOUS;
            case GO -> EditorInfo.IME_ACTION_GO;
            case SEARCH -> EditorInfo.IME_ACTION_SEARCH;
            case SEND -> EditorInfo.IME_ACTION_SEND;
            case DEFAULT -> input.multiline() ? EditorInfo.IME_FLAG_NO_ENTER_ACTION : EditorInfo.IME_ACTION_DONE;
        };
    }

    private void performDefaultImeAction(EditText field, int action) {
        if (action == EditorInfo.IME_ACTION_NEXT || action == EditorInfo.IME_ACTION_PREVIOUS) {
            int direction = action == EditorInfo.IME_ACTION_NEXT ? View.FOCUS_FORWARD : View.FOCUS_BACKWARD;
            View target = field.focusSearch(direction);
            if (target != null) target.requestFocus();
        } else if (action == EditorInfo.IME_ACTION_DONE) {
            InputMethodManager keyboard = (InputMethodManager) host.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
    }

    private void styleButton(android.widget.Button b, ButtonProps p, ThemeInterface theme) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color(p.resolveBackgroundColor(theme)));
        shape.setCornerRadius(dp(p.resolveBorderRadius(theme)));
        int borderWidth = p.resolveBorderWidth(theme);
        if (borderWidth > 0) shape.setStroke(dp(borderWidth), color(p.resolveBorderColor(theme)));

        int text = color(p.resolveTextColor(theme));
        int ripple = (text & 0x00FFFFFF) | 0x33000000; // feedback de toque, no lugar da animação de opacity do JavaFX
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple), shape, null));
        b.setTextColor(text);
        b.setTextSize(ScaleProvider.scale(p.resolveFontSize()));

        // O Button nativo vem com caixa alta, sombra e minHeight/minWidth próprios;
        // sem isso o padding não manda no tamanho, como acontece no JavaFX.
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setMinHeight(0); b.setMinimumHeight(0);
        b.setMinWidth(0);  b.setMinimumWidth(0);

        int[] pad = p.resolvePaddingUnits(); // top, right, bottom, left
        b.setPadding(dp(pad[3]), dp(pad[0]), dp(pad[1]), dp(pad[2])); // Android: left, top, right, bottom

        b.setEnabled(p.getVariant() != ButtonVariant.DISABLED);
    }
}

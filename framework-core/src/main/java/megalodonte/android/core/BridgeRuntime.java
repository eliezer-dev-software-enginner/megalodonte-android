package megalodonte.android.core;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.components.Button;
import megalodonte.components.Text;
import megalodonte.base.state.Subscription;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.inputs.Input;
import megalodonte.components.Checkbox;
import megalodonte.components.ProgressBar;
import megalodonte.v2.Show;

/** Internal mounted tree. Screen authors never send native commands themselves. */
public final class BridgeRuntime implements AutoCloseable {
    private final NativeBridge bridge;
    private final Map<String, Component> components = new HashMap<>();
    private final List<Subscription> bindings = new ArrayList<>();
    private boolean closed;
    public BridgeRuntime(NativeBridge bridge, Component root) {
        this.bridge = Objects.requireNonNull(bridge);
        index(Objects.requireNonNull(root));
        try {
            bridge.mount(root, this::dispatch);
            bindings.add(ThemeManager.state().observe(theme -> {
                if (!closed && theme != null) bridge.applyTheme(theme);
            }));
            components.values().forEach(component -> {
                if (component instanceof Text text && text.state() != null) bind(text);
                else if (component instanceof Input input) bindings.add(input.displayState().observe(value -> updateValue(input.bridgeId(), value)));
                else if (component instanceof Checkbox checkbox) bindings.add(checkbox.state.observe(value -> updateValue(checkbox.bridgeId(), value)));
                else if (component instanceof ProgressBar progress) bindings.add(progress.state.observe(value -> updateValue(progress.bridgeId(), value)));
                else if (component instanceof Show show) bindings.add(show.condition.observe(value -> updateValue(show.bridgeId(), value)));
                else if (component instanceof megalodonte.components.SpacerVertical space) bindings.add(space.size.observe(value -> updateValue(space.bridgeId(), value)));
                else if (component instanceof megalodonte.components.SpacerHorizontal space) bindings.add(space.size.observe(value -> updateValue(space.bridgeId(), value)));
            });
        } catch (RuntimeException error) {
            close();
            throw error;
        }
    }
    private void index(Component component) {
        if (component instanceof megalodonte.components.SpacerVertical || component instanceof megalodonte.components.SpacerHorizontal)
            bridge.contract().require(megalodonte.contracts.Capability.SPACING);
        if (component instanceof Input) bridge.contract().require(megalodonte.contracts.Capability.INPUT);
        if (component instanceof Checkbox) bridge.contract().require(megalodonte.contracts.Capability.CHECKBOX);
        if (component instanceof ProgressBar) bridge.contract().require(megalodonte.contracts.Capability.PROGRESS);
        if (component instanceof Show) bridge.contract().require(megalodonte.contracts.Capability.CONDITIONAL_RENDERING);
        if (component instanceof megalodonte.components.Scroll) bridge.contract().require(megalodonte.contracts.Capability.SCROLL);
        if (component instanceof megalodonte.components.layout_components.Row) bridge.contract().require(megalodonte.contracts.Capability.ROW);
        if (component instanceof megalodonte.components.layout_components.Column) bridge.contract().require(megalodonte.contracts.Capability.COLUMN);
        if (component instanceof Text) bridge.contract().require(megalodonte.contracts.Capability.TEXT);
        if (component instanceof Button) bridge.contract().require(megalodonte.contracts.Capability.BUTTON);
        if (component instanceof megalodonte.components.layout_components.Container)
            bridge.contract().require(megalodonte.contracts.Capability.CONTAINER);
        if (components.putIfAbsent(component.bridgeId(), component) != null)
            throw new IllegalArgumentException("Component reused in the same tree: " + component.bridgeId());
        component.children().forEach(this::index);
    }
    private void bind(Text text) {
        String id = text.bridgeId();
        bindings.add(text.state().observe(value -> setText(id, value)));
    }
    private void dispatch(NativeBridge.Event event) {
        if (closed) return;
        Component target = components.get(event.componentId());
        if (target instanceof Button button && "click".equals(event.name())) button.dispatchClick();
        else if (target instanceof Input input) {
            if ("change".equals(event.name()) && event.value() instanceof String value)
                updateValue(input.bridgeId(), input.dispatchChange(value));
            else if ("enter".equals(event.name())) input.dispatchEnter();
        } else if (target instanceof Checkbox checkbox && "change".equals(event.name()) && event.value() instanceof Boolean value)
            checkbox.state.set(value);
    }
    private void updateValue(String id, Object value) { if (!closed) bridge.setValue(id, value); }
    public void setText(String id, String value) {
        if (closed) return;
        if (!(components.get(id) instanceof Text)) throw new IllegalArgumentException("Not a Text: " + id);
        bridge.setText(id, Objects.requireNonNull(value));
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        bindings.forEach(Subscription::close);
        bindings.clear();
        components.clear();
        bridge.close();
    }
}

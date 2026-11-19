package megalodonte.android.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.Subscription;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.Text;
import megalodonte.components.Checkbox;
import megalodonte.components.ProgressBar;
import megalodonte.components.inputs.Input;
import megalodonte.v2.ForEach;
import megalodonte.v2.Show;

/** Internal mounted tree. Screen authors never send native commands themselves. */
public final class BridgeRuntime implements AutoCloseable {
    private final NativeBridge bridge;
    private final Map<String, Component> components = new HashMap<>();
    private final Map<String, Subscription> subscriptions = new HashMap<>();
    private Subscription theme;
    private boolean closed;
    public BridgeRuntime(NativeBridge bridge, Component root) {
        this.bridge = Objects.requireNonNull(bridge);
        index(Objects.requireNonNull(root));
        try {
            bridge.mount(root, this::dispatch);
            theme = ThemeManager.state().observe(value -> {
                if (!closed && value != null) bridge.applyTheme(value);
            });
            bindTree(root);
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
        if (component instanceof ForEach) bridge.contract().require(megalodonte.contracts.Capability.LIST_RENDERING);
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
    private void bindTree(Component component) {
        bind(component);
        if (component instanceof ForEach<?> forEach) {
            forEach.components().forEach(this::bindTree);
        } else {
            component.children().forEach(this::bindTree);
        }
    }
    private void bind(Component component) {
        String id = component.bridgeId();
        if (closed || subscriptions.containsKey(id)) return;
        Subscription binding;
        if (component instanceof Text text) {
            binding = text.state() == null ? null : text.state().observe(value -> setText(id, value));
        } else if (component instanceof Input input) {
            binding = input.displayState().observe(value -> updateValue(id, value));
        } else if (component instanceof Checkbox checkbox) {
            binding = checkbox.state.observe(value -> updateValue(id, value));
        } else if (component instanceof ProgressBar progress) {
            binding = progress.state.observe(value -> updateValue(id, value));
        } else if (component instanceof Show show) {
            binding = show.condition.observe(value -> updateValue(id, value));
        } else if (component instanceof megalodonte.components.SpacerVertical space) {
            binding = space.size.observe(value -> updateValue(id, value));
        } else if (component instanceof megalodonte.components.SpacerHorizontal space) {
            binding = space.size.observe(value -> updateValue(id, value));
        } else if (component instanceof ForEach<?> forEach) {
            // observe() delivers the current list immediately; the initial items
            // were already mounted by mount(), so skip that first notification.
            boolean[] initial = { true };
            binding = forEach.state().observe(ignored -> {
                if (initial[0]) { initial[0] = false; return; }
                refreshList(forEach);
            });
        } else {
            binding = null;
        }
        if (binding != null) subscriptions.put(id, binding);
    }
    private void refreshList(ForEach<?> forEach) {
        if (closed) return;
        List<Component> previous = forEach.components();
        forEach.refresh();
        bridge.setChildren(forEach.bridgeId(), forEach.components());
        previous.forEach(this::unmount);
        List<Component> next = forEach.components();
        next.forEach(this::index);
        next.forEach(this::bindTree);
    }
    private void unmount(Component component) {
        Subscription binding = subscriptions.remove(component.bridgeId());
        if (binding != null) binding.close();
        if (component instanceof ForEach<?> forEach) forEach.components().forEach(this::unmount);
        else component.children().forEach(this::unmount);
        components.remove(component.bridgeId());
    }
    private void dispatch(NativeBridge.Event event) {
        if (closed) return;
        Component target = components.get(event.componentId());
        if (target instanceof Button button && "click".equals(event.name())) button.dispatchClick();
        else if (target instanceof Input input) {
            if ("change".equals(event.name()) && event.value() instanceof String value)
                updateValue(input.bridgeId(), input.dispatchChange(value));
            else if ("enter".equals(event.name())) input.dispatchEnter();
            else if ("keyboardAction".equals(event.name()) && event.value() instanceof Runnable performDefaultAction)
                input.dispatchKeyboardAction(performDefaultAction);
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
        subscriptions.values().forEach(Subscription::close);
        subscriptions.clear();
        if (theme != null) { theme.close(); theme = null; }
        components.clear();
        bridge.close();
    }
}

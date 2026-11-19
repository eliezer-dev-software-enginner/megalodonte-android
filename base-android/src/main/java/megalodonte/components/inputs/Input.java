package megalodonte.components.inputs;
import java.util.Objects;
import java.util.function.Function;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.base.state.ReadableState;
import megalodonte.props.InputProps;

public class Input extends Component {
    public final State<String> state;
    public final InputProps props;
    private Function<String, OnChangeResult> onChange;
    private Function<String, OnChangeResult> onInitialize;
    private Runnable onEnter;
    private KeyboardActionHandler onKeyboardAction;
    public Input(State<String> state) { this(state, new InputProps()); }
    public Input(State<String> state, InputProps props) { this.state = Objects.requireNonNull(state); this.props = Objects.requireNonNull(props); }
    public Input(String value) { this(State.of(value)); }
    public Input onChange(Function<String, OnChangeResult> handler) { onChange = Objects.requireNonNull(handler); return this; }
    public Input onInitialize(Function<String, OnChangeResult> handler) { onInitialize = Objects.requireNonNull(handler); return this; }
    public Input onEnter(Runnable handler) { onEnter = Objects.requireNonNull(handler); return this; }
    public Input onKeyboardAction(KeyboardActionHandler handler) { onKeyboardAction = Objects.requireNonNull(handler); return this; }
    public String value() { return formatted(state.get()); }
    public ReadableState<String> displayState() { return state.map(this::formatted); }
    private String formatted(String value) {
        String safe = value == null ? "" : value;
        if (onInitialize == null) return safe;
        OnChangeResult result = onInitialize.apply(safe);
        return result == null || result.displayValue() == null ? safe : result.displayValue();
    }
    public String dispatchChange(String value) {
        OnChangeResult result = onChange == null ? OnChangeResult.same(value) : onChange.apply(value);
        if (result == null) return value;
        if (result.stateValue() != null) state.set(result.stateValue());
        return result.displayValue() == null ? value : result.displayValue();
    }
    public void dispatchEnter() { if (onEnter != null) onEnter.run(); }
    public void dispatchKeyboardAction(Runnable performDefaultAction) {
        Runnable nativeDefault = Objects.requireNonNull(performDefaultAction);
        if (onKeyboardAction != null) onKeyboardAction.onKeyboardAction(nativeDefault);
        else if (onEnter != null) onEnter.run();
        else nativeDefault.run();
    }
    public boolean password() { return false; }
    public boolean multiline() { return false; }
}

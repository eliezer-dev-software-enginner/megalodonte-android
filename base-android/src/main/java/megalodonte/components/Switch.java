package megalodonte.components;

import java.util.Objects;
import java.util.function.Consumer;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.props.SwitchProps;

/** A native on/off control bound bidirectionally to a boolean state. */
public final class Switch extends Component {
    private final String accessibleLabel;
    private final State<Boolean> state;
    private final SwitchProps props;
    private Consumer<Boolean> onCheckedChange = ignored -> { };

    public Switch(String accessibleLabel, State<Boolean> state) {
        this(accessibleLabel, state, new SwitchProps());
    }

    public Switch(String accessibleLabel, State<Boolean> state, SwitchProps props) {
        if (accessibleLabel == null || accessibleLabel.isBlank()) {
            throw new IllegalArgumentException("accessibleLabel must not be blank");
        }
        this.accessibleLabel = accessibleLabel;
        this.state = Objects.requireNonNull(state, "state");
        this.props = Objects.requireNonNull(props, "props");
    }

    public String accessibleLabel() { return accessibleLabel; }
    public State<Boolean> state() { return state; }
    public SwitchProps props() { return props; }

    /** Runs after a user interaction changes the checked state. */
    public Switch onCheckedChange(Consumer<Boolean> handler) {
        onCheckedChange = Objects.requireNonNull(handler, "handler");
        return this;
    }

    public void dispatchChange(boolean currentState) {
        state.set(currentState);
        onCheckedChange.accept(currentState);
    }
}

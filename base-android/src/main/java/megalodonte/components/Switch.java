package megalodonte.components;

import java.util.Objects;
import java.util.function.Consumer;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;

/** A native on/off control bound bidirectionally to a boolean state. */
public final class Switch extends Component {
    private final String accessibleLabel;
    private final State<Boolean> state;
    private Consumer<Boolean> onCheckedChange = ignored -> { };

    public Switch(String accessibleLabel, State<Boolean> state) {
        if (accessibleLabel == null || accessibleLabel.isBlank()) {
            throw new IllegalArgumentException("accessibleLabel must not be blank");
        }
        this.accessibleLabel = accessibleLabel;
        this.state = Objects.requireNonNull(state, "state");
    }

    public String accessibleLabel() { return accessibleLabel; }
    public State<Boolean> state() { return state; }

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

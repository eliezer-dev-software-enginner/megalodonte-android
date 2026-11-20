package megalodonte.components;

import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;

/** A native on/off control bound bidirectionally to a boolean state. */
public final class Switch extends Component {
    private final String accessibleLabel;
    private final State<Boolean> state;

    public Switch(String accessibleLabel, State<Boolean> state) {
        if (accessibleLabel == null || accessibleLabel.isBlank()) {
            throw new IllegalArgumentException("accessibleLabel must not be blank");
        }
        this.accessibleLabel = accessibleLabel;
        this.state = Objects.requireNonNull(state, "state");
    }

    public String accessibleLabel() { return accessibleLabel; }
    public State<Boolean> state() { return state; }
}

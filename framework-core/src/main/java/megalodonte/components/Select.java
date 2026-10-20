package megalodonte.components;

import java.util.List;
import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;

/** Native Android selection control with a two-way String state. */
public final class Select extends Component {
    public final State<String> state;
    public final List<String> options;

    public Select(State<String> state, List<String> options) {
        this.state = Objects.requireNonNull(state);
        this.options = List.copyOf(Objects.requireNonNull(options));
    }
}

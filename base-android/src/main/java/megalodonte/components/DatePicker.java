package megalodonte.components;

import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;

/** Native Android date picker; state contains an ISO-8601 yyyy-MM-dd date. */
public final class DatePicker extends Component {
    public final State<String> state;
    public DatePicker(State<String> state) { this.state = Objects.requireNonNull(state); }
}

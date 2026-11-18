package megalodonte.components;
import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
public final class ProgressBar extends Component {
    public final State<Integer> state;
    public ProgressBar(State<Integer> state) { this.state = Objects.requireNonNull(state); }
}

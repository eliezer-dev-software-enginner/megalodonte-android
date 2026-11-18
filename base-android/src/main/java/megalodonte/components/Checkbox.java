package megalodonte.components;
import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.props.CheckboxProps;
public final class Checkbox extends Component {
    public final String label;
    public final State<Boolean> state;
    public final CheckboxProps props;
    public Checkbox(String label, State<Boolean> state) { this(label, state, new CheckboxProps()); }
    public Checkbox(String label, State<Boolean> state, CheckboxProps props) {
        this.label = Objects.requireNonNull(label); this.state = Objects.requireNonNull(state); this.props = Objects.requireNonNull(props);
    }
}

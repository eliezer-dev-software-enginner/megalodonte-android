package megalodonte.components.inputs;
import megalodonte.base.state.State;
import megalodonte.props.InputProps;
public final class PasswordInput extends Input {
    public PasswordInput(State<String> state, InputProps props) { super(state, props); }
    @Override public boolean password() { return true; }
}

package megalodonte.components.inputs;
import megalodonte.base.state.State;
import megalodonte.props.InputProps;
public final class TextAreaInput extends Input {
    public TextAreaInput(State<String> state, InputProps props) { super(state, props); }
    @Override public boolean multiline() { return true; }
}

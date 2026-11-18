package megalodonte.components;

import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.ReadableState;
import megalodonte.props.TextProps;

public final class Text extends Component {
    private final String value;
    private final ReadableState<String> state;
    private final TextProps props;
    public Text(String value) { this(value, new TextProps()); }
    public Text(String value, TextProps props) {
        this.value = Objects.requireNonNull(value);
        this.state = null;
        this.props = Objects.requireNonNull(props);
    }
    public Text(ReadableState<String> state) { this(state, new TextProps()); }
    public Text(ReadableState<String> state, TextProps props) {
        this.value = null;
        this.state = Objects.requireNonNull(state);
        this.props = Objects.requireNonNull(props);
    }
    public String value() { return state == null ? value : Objects.requireNonNull(state.get()); }
    public ReadableState<String> state() { return state; }
    public TextProps props() { return props; }
}

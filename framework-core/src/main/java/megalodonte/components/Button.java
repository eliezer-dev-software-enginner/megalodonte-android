package megalodonte.components;

import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.props.ButtonProps;

public final class Button extends Component {
    private final String label;
    private final ButtonProps props;
    private Runnable onClick = () -> {};
    public Button(String label) { this(label, new ButtonProps()); }
    public Button(String label, ButtonProps props) {
        this.label = Objects.requireNonNull(label);
        this.props = Objects.requireNonNull(props);
    }
    public Button onClick(Runnable action) { onClick = Objects.requireNonNull(action); return this; }
    public String label() { return label; }
    public ButtonProps props() { return props; }
    /** Backend event entry point; client code declares callbacks through onClick. */
    public void dispatchClick() { onClick.run(); }
}

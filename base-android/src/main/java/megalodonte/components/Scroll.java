package megalodonte.components;
import java.util.List;
import java.util.Objects;
import megalodonte.base.components.Component;
public final class Scroll extends Component {
    private final Component child;
    public Scroll(Component child) { this.child = Objects.requireNonNull(child); }
    @Override public List<Component> children() { return List.of(child); }
}

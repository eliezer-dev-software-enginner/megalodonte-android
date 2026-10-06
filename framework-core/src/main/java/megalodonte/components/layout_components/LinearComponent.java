package megalodonte.components.layout_components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.props.LayoutProps;

public abstract class LinearComponent extends Component {
    private final List<Component> children = new ArrayList<>();
    private final LayoutProps<?> props;
    protected LinearComponent(LayoutProps<?> props) { this.props = Objects.requireNonNull(props); }
    protected void append(Component... components) { children.addAll(List.of(components)); }
    @Override public List<Component> children() { return List.copyOf(children); }
    public LayoutProps<?> props() { return props; }
}

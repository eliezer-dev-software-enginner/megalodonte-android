package megalodonte.components.layout_components;

import java.util.List;
import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.props.ContainerProps;

public final class Container extends LinearComponent {
    public Container() { this(new ContainerProps()); }
    public Container(ContainerProps props) { super(props); }
    public Container children(Component... children) { append(children); return this; }
    public Container c_child(Component child) { append(child); return this; }
    @Override public ContainerProps props() { return (ContainerProps) super.props(); }
}

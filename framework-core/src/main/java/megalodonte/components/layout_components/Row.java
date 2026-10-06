package megalodonte.components.layout_components;
import java.util.List;
import java.util.function.Function;
import megalodonte.base.components.Component;
import megalodonte.props.RowProps;

public final class Row extends LinearComponent {
    public Row() { this(new RowProps()); }
    public Row(RowProps props) { super(props); }
    public Row children(Component... children) { append(children); return this; }
    public Row c_child(Component child) { append(child); return this; }
    public Row items(List<? extends Component> children) { children.forEach(this::c_child); return this; }
    public <T> Row items(List<T> data, Function<T, ? extends Component> factory) { data.forEach(item -> c_child(factory.apply(item))); return this; }
    @Override public RowProps props() { return (RowProps) super.props(); }
}

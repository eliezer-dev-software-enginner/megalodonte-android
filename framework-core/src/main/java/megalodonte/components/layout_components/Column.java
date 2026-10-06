package megalodonte.components.layout_components;
import java.util.List;
import java.util.function.Function;
import megalodonte.base.components.Component;
import megalodonte.props.ColumnProps;

public final class Column extends LinearComponent {
    public Column() { this(new ColumnProps()); }
    public Column(ColumnProps props) { super(props); }
    public Column children(Component... children) { append(children); return this; }
    public Column c_child(Component child) { append(child); return this; }
    public Column items(List<? extends Component> children) { children.forEach(this::c_child); return this; }
    public <T> Column items(List<T> data, Function<T, ? extends Component> factory) { data.forEach(item -> c_child(factory.apply(item))); return this; }
    @Override public ColumnProps props() { return (ColumnProps) super.props(); }
}

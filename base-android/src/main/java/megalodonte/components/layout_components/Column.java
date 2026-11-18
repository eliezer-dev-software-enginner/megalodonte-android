package megalodonte.components.layout_components;
import java.util.List;
import java.util.function.Function;
import megalodonte.base.components.Component;
import megalodonte.base.state.ReadableState;
import megalodonte.props.ColumnProps;
import megalodonte.v2.ForEach;

public final class Column extends LinearComponent {
    public Column() { this(new ColumnProps()); }
    public Column(ColumnProps props) { super(props); }
    public Column children(Component... children) { append(children); return this; }
    public Column c_child(Component child) { append(child); return this; }
    public Column items(List<? extends Component> children) { children.forEach(this::c_child); return this; }
    public <T> Column items(List<T> data, Function<T, ? extends Component> factory) { data.forEach(item -> c_child(factory.apply(item))); return this; }

    /**
     * Renders a reactive list. The mapper runs again whenever {@code data}
     * changes, replacing only the views of this list inside the column.
     *
     * @param data    observable list, such as a {@code ListState}
     * @param factory builds one component per item; must return a new instance
     *                for each position
     * @param <T>     item type
     */
    public <T> Column items(ReadableState<List<T>> data, Function<T, ? extends Component> factory) {
        append(ForEach.of(data, factory));
        return this;
    }

    @Override public ColumnProps props() { return (ColumnProps) super.props(); }
}

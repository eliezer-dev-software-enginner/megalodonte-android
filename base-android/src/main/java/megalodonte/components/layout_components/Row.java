package megalodonte.components.layout_components;
import java.util.List;
import java.util.function.Function;
import megalodonte.base.components.Component;
import megalodonte.base.state.ReadableState;
import megalodonte.props.RowProps;
import megalodonte.v2.ForEach;

public final class Row extends LinearComponent {
    public Row() { this(new RowProps()); }
    public Row(RowProps props) { super(props); }
    public Row children(Component... children) { append(children); return this; }
    public Row c_child(Component child) { append(child); return this; }
    public Row items(List<? extends Component> children) { children.forEach(this::c_child); return this; }
    public <T> Row items(List<T> data, Function<T, ? extends Component> factory) { data.forEach(item -> c_child(factory.apply(item))); return this; }

    /**
     * Renders a reactive list. The mapper runs again whenever {@code data}
     * changes, replacing only the views of this list inside the row.
     *
     * @param data    observable list, such as a {@code ListState}
     * @param factory builds one component per item; must return a new instance
     *                for each position
     * @param <T>     item type
     */
    public <T> Row items(ReadableState<List<T>> data, Function<T, ? extends Component> factory) {
        append(ForEach.of(data, factory));
        return this;
    }

    @Override public RowProps props() { return (RowProps) super.props(); }
}

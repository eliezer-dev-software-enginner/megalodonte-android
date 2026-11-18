package megalodonte.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import megalodonte.base.components.Component;
import megalodonte.base.state.ReadableState;

/**
 * Reactive list block rendered inside a {@code Column} or {@code Row}.
 * <p>
 * The component observes a {@link ReadableState} holding a list and maps every
 * item to a component. When the list changes, the Android runtime replaces the
 * views of this block and discards the previous subtree. The surrounding
 * layout, spacing and {@code fillWidth} rules stay the same as for
 * {@code Column.items(List, Function)} because the generated views are added
 * directly to the parent layout instead of a nested container.
 * </p>
 * <p>
 * The mapper must return a new component instance for each item. Reusing one
 * instance for two positions is rejected by the runtime, as for any other
 * component in the same tree.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ListState<String> names = ListState.ofEmpty();
 * new Column(new ColumnProps().fillWidth())
 *     .items(names, name -> new Button(name, new ButtonProps().fillWidth()));
 * }</pre>
 *
 * @param <T> item type held by the observed list
 */
public final class ForEach<T> extends Component implements megalodonte.base.state.ForEachState<T, Component> {

    private final ReadableState<List<T>> state;
    private final Function<T, ? extends Component> factory;
    private List<Component> components;

    private ForEach(ReadableState<List<T>> state, Function<T, ? extends Component> factory) {
        this.state = Objects.requireNonNull(state);
        this.factory = Objects.requireNonNull(factory);
        this.components = map(state.get());
    }

    /**
     * Creates a reactive list block for the supplied state and mapper.
     *
     * @param state   observable list; may be a {@code ListState}, {@code State}
     *                or a derived state
     * @param factory builds one component per item
     * @param <T>     item type
     * @return a list block suitable for {@code Column} or {@code Row}
     */
    public static <T> ForEach<T> of(
            ReadableState<List<T>> state,
            Function<T, ? extends Component> factory) {
        return new ForEach<>(state, factory);
    }

    /** Returns the observed list state. */
    public ReadableState<List<T>> state() {
        return state;
    }

    @Override public ReadableState<List<T>> getState() { return state(); }

    /**
     * Returns the components mapped from the last observed list snapshot.
     * The returned list is an immutable copy.
     */
    public List<Component> components() {
        return List.copyOf(components);
    }

    @Override public List<Component> getComponents() { return components(); }

    /**
     * Rebuilds the mapped components from the current list snapshot.
     * <p>
     * Called once for every notification of the observed state. Rebuilding is
     * unconditional because a notification is always a request to re-render:
     * {@code ListState} publishes a new list for list mutations and also
     * notifies through {@code refresh()} when an item was mutated in place.
     * </p>
     */
    public void refresh() {
        components = map(state.get());
    }

    @Override
    public List<Component> children() {
        return components();
    }

    private List<Component> map(List<T> items) {
        if (items == null) {
            return List.of();
        }
        List<Component> mapped = new ArrayList<>(items.size());
        for (T item : items) {
            mapped.add(Objects.requireNonNull(factory.apply(item),
                    "ForEach mapper returned null for item: " + item));
        }
        return mapped;
    }
}

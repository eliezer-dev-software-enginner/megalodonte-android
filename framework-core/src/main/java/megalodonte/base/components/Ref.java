package megalodonte.base.components;

/**
 * Mutable holder for a component instance, analogous to React's {@code useRef}.
 * <p>
 * Bind it with {@link Component#ref(Ref)} to read the component created during
 * {@code render()} from the screen or from a view model. On Android the holder
 * exposes the declarative component only; the native view stays owned by the
 * bridge, so this is not a way to reach {@code android.view.View}.
 * </p>
 *
 * @param <T> the component type held by this reference
 */
public class Ref<T> {
    private T current;

    public Ref() {
    }

    public Ref(T initial) {
        this.current = initial;
    }

    public T current() {
        return current;
    }

    public void setCurrent(T component) {
        this.current = component;
    }
}

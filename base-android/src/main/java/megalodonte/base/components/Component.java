package megalodonte.base.components;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Declarative component; native identity is allocated by the framework. */
public abstract class Component implements megalodonte.contracts.ComponentContract<Component> {
    private static final AtomicLong nextId = new AtomicLong();
    private final String id = "component-" + nextId.incrementAndGet();
    public final String bridgeId() { return id; }
    public List<Component> children() { return List.of(); }

    /**
     * Binds this component to {@code ref}, making it reachable from the screen
     * or view model that created the tree.
     * <p>
     * The reference is assigned while the tree is composed, before the bridge
     * mounts it. It exposes the declarative component, not the native view.
     * </p>
     *
     * @param ref the reference that receives this component
     * @return this component for chaining
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Component ref(Ref<T> ref) {
        return ref((megalodonte.contracts.RefContract<T>) ref);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> Component ref(megalodonte.contracts.RefContract<T> ref) {
        ref.setCurrent((T) this);
        return this;
    }
}

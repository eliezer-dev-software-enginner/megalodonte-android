package megalodonte.base.components;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Declarative component; native identity is allocated by the framework. */
public abstract class Component {
    private static final AtomicLong nextId = new AtomicLong();
    private final String id = "component-" + nextId.incrementAndGet();
    public final String bridgeId() { return id; }
    public List<Component> children() { return List.of(); }
}

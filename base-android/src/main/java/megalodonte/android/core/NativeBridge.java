package megalodonte.android.core;

import java.util.List;
import java.util.function.Consumer;
import megalodonte.base.components.Component;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.contracts.BackendContract;

/** Commands flow to native UI; native events flow back through the supplied sink. */
public interface NativeBridge extends AutoCloseable {
    record Event(String componentId, String name, Object value) {
        public Event(String componentId, String name) { this(componentId, name, null); }
    }
    void mount(Component root, Consumer<Event> events);
    void setText(String componentId, String value);
    void setValue(String componentId, Object value);
    /**
     * Replaces the native views generated for a reactive list block. The
     * bridge discards the previous subtree and creates the supplied components
     * in its place, keeping the list position inside the parent layout.
     *
     * @param componentId id of the mounted reactive list ({@code ForEach})
     * @param children    new mapped components, in display order
     */
    void setChildren(String componentId, List<Component> children);
    void applyTheme(ThemeInterface theme);
    BackendContract contract();
    @Override void close();
}

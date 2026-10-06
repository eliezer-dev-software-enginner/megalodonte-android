package megalodonte.android.core;

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
    void applyTheme(ThemeInterface theme);
    BackendContract contract();
    @Override void close();
}

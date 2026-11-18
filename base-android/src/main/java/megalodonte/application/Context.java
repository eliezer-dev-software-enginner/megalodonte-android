package megalodonte.application;

import java.util.Map;
import java.util.Objects;
import megalodonte.android.core.BridgeRuntime;
import megalodonte.android.core.NativeBridge;
import megalodonte.base.components.ScreenComponent;

/** Application context supplied by the platform host. */
public final class Context implements AutoCloseable {
    private final NativeBridge bridge;
    private final Map<String, String> restoredState;
    private ScreenComponent screen;
    private BridgeRuntime runtime;
    private boolean closed;
    private java.util.function.Consumer<MegalodonteApp.Event> events = event -> {};
    void setEventHandler(java.util.function.Consumer<MegalodonteApp.Event> handler) { events = Objects.requireNonNull(handler); }
    Context(NativeBridge bridge, Map<String, String> restoredState) {
        this.bridge = Objects.requireNonNull(bridge);
        this.restoredState = Map.copyOf(restoredState);
    }
    public void useView(ScreenComponent screen) {
        if (closed) throw new IllegalStateException("Application is closed");
        Objects.requireNonNull(screen);
        // The initial screen receives Activity state before its first render.
        if (this.screen == null) screen.restoreState(restoredState);
        if (runtime != null) runtime.close();
        if (this.screen != null) this.screen.onDestroy();
        runtime = null;
        this.screen = screen;
        runtime = new BridgeRuntime(bridge, screen.render());
        screen.onMount();
    }
    public Map<String, String> saveState() { return screen == null ? Map.of() : Map.copyOf(screen.saveState()); }
    public void useView(Class<? extends ScreenComponent> screen) {
        try { useView(screen.getDeclaredConstructor().newInstance()); }
        catch (ReflectiveOperationException error) { throw new IllegalArgumentException("Cannot instantiate screen: " + screen.getName(), error); }
    }
    boolean hasView() { return runtime != null; }
    public megalodonte.contracts.BackendContract backendContract() { return bridge.contract(); }
    @Override public void close() {
        close(true);
    }
    public void close(boolean closeRequest) {
        if (closed) return;
        closed = true;
        if (runtime != null) runtime.close();
        else bridge.close();
        if (screen != null) screen.onDestroy();
        runtime = null;
        screen = null;
        if (closeRequest) events.accept(MegalodonteApp.Event.CloseRequest);
    }
}

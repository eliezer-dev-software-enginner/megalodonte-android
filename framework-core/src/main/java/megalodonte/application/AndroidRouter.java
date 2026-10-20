package megalodonte.application;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import megalodonte.base.components.ScreenComponent;

/** Named screen routes backed by the Android Megalodonte context. */
public final class AndroidRouter {
    private final Context context;
    private final Map<String, Supplier<? extends ScreenComponent>> routes = new HashMap<>();
    private final Deque<String> history = new ArrayDeque<>();
    private String currentPath;

    public AndroidRouter(Context context) { this.context = Objects.requireNonNull(context); }

    public AndroidRouter route(String path, Supplier<? extends ScreenComponent> screen) {
        if (path == null || !path.startsWith("/") || path.isBlank())
            throw new IllegalArgumentException("Route paths must start with /");
        if (routes.putIfAbsent(path, Objects.requireNonNull(screen)) != null)
            throw new IllegalArgumentException("Duplicate route: " + path);
        return this;
    }

    public void start(String path) {
        history.clear();
        show(path);
    }

    public void navigate(String path) {
        requireRoute(path);
        if (path.equals(currentPath)) return;
        if (currentPath != null) history.push(currentPath);
        show(path);
    }

    public boolean back() {
        if (history.isEmpty()) return false;
        show(history.pop());
        return true;
    }

    public String currentPath() { return currentPath; }

    private void show(String path) {
        ScreenComponent screen = requireRoute(path).get();
        if (screen == null) throw new IllegalStateException("Route returned no screen: " + path);
        context.useView(screen);
        currentPath = path;
    }

    private Supplier<? extends ScreenComponent> requireRoute(String path) {
        Supplier<? extends ScreenComponent> screen = routes.get(path);
        if (screen == null) throw new IllegalArgumentException("Unknown route: " + path);
        return screen;
    }
}

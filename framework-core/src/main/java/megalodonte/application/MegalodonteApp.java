package megalodonte.application;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import megalodonte.android.core.NativeBridge;

public final class MegalodonteApp {
    public enum Event { CloseRequest }
    private static final ThreadLocal<Context> launching = new ThreadLocal<>();
    private MegalodonteApp() { }
    /** Same client entry point as the desktop framework. */
    public static void run(Consumer<Context> configure) {
        Context context = launching.get();
        if (context == null) throw new IllegalStateException("MegalodonteApp.run requires a platform host");
        Objects.requireNonNull(configure).accept(context);
    }
    public static void run(Consumer<Context> configure, Consumer<Event> events) {
        run(context -> { context.setEventHandler(events); configure.accept(context); });
    }
    /** Platform bootstrap; Android's framework Activity supplies the bridge. */
    public static Context launch(NativeBridge bridge, Map<String, String> restoredState, Runnable main) {
        if (launching.get() != null) throw new IllegalStateException("Nested application launch");
        Context context = new Context(bridge, restoredState);
        launching.set(context);
        try {
            Objects.requireNonNull(main).run();
            if (!context.hasView()) throw new IllegalStateException("Main must select a screen with context.useView");
            return context;
        } catch (RuntimeException error) {
            context.close(false);
            throw error;
        } finally {
            launching.remove();
        }
    }
}

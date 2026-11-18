package my_app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;
import megalodonte.android.core.NativeBridge;
import megalodonte.application.Context;
import megalodonte.application.MegalodonteApp;
import megalodonte.base.components.Component;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.contracts.BackendContract;
import megalodonte.contracts.Capability;
import megalodonte.components.Text;
import megalodonte.components.Button;
import megalodonte.components.inputs.Input;
import megalodonte.v2.Show;

public final class ClientFixtureTest {
    static final class Bridge implements NativeBridge {
        final List<Component> nodes = new ArrayList<>();
        final Map<String, Object> values = new HashMap<>();
        Consumer<Event> events;
        void collect(Component root) { nodes.add(root); root.children().forEach(this::collect); }
        @Override public void mount(Component root, Consumer<Event> events) { collect(root); this.events = events; }
        @Override public void setText(String id, String text) { values.put(id, text); }
        @Override public void setValue(String id, Object value) { values.put(id, value); }
        @Override public void setChildren(String id, List<Component> children) { children.forEach(this::collect); }
        @Override public void applyTheme(ThemeInterface theme) { }
        @Override public BackendContract contract() { return new BackendContract("fixture", 1, java.util.EnumSet.allOf(Capability.class)); }
        @Override public void close() { }
        <T> T find(Class<T> type) { return nodes.stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow(); }
    }
    static void require(boolean condition) { if (!condition) throw new AssertionError("Original client binding/event contract failed"); }
    static void runClient(String number) {
        String packageName = switch (number) {
            case "2" -> "megalodonte_app2_counter";
            case "3" -> "megalodonte_app3_string_lenght";
            case "4" -> "megalodonte_app4_hide_seek";
            default -> throw new AssertionError("Unknown fixture");
        };
        try {
            var main = Class.forName(packageName + ".Main").getDeclaredMethod("main", String[].class);
            main.setAccessible(true);
            main.invoke(null, (Object) new String[0]);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot launch original client " + number, error);
        }
    }
    public static void main(String[] args) {
        Bridge bridge = new Bridge();
        try (Context app = MegalodonteApp.launch(bridge, Map.of(), () -> runClient(args[0]))) {
            switch (args[0]) {
                case "2" -> {
                    Button increment = bridge.nodes.stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(button -> button.label().equals("Increment")).findFirst().orElseThrow();
                    bridge.events.accept(new NativeBridge.Event(increment.bridgeId(), "click"));
                    Text counter = bridge.find(Text.class);
                    require("1".equals(bridge.values.get(counter.bridgeId())));
                }
                case "3" -> {
                    Input input = bridge.find(Input.class);
                    bridge.events.accept(new NativeBridge.Event(input.bridgeId(), "change", "hello"));
                    require(input.state.get().equals("hello"));
                    require("Size is: 5".equals(bridge.values.get(bridge.find(Text.class).bridgeId())));
                }
                case "4" -> {
                    Show show = bridge.find(Show.class);
                    require(Boolean.FALSE.equals(bridge.values.get(show.bridgeId())));
                    bridge.events.accept(new NativeBridge.Event(bridge.find(Button.class).bridgeId(), "click"));
                    require(Boolean.TRUE.equals(bridge.values.get(show.bridgeId())));
                }
                default -> throw new AssertionError("Unknown fixture");
            }
        }
        System.out.println("Unchanged desktop client " + args[0] + " passed");
    }
}

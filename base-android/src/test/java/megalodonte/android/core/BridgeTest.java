package megalodonte.android.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import megalodonte.application.Context;
import megalodonte.application.MegalodonteApp;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.components.Button;
import megalodonte.components.Menu;
import megalodonte.components.MenuBar;
import megalodonte.components.MenuItem;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Container;
import megalodonte.props.ContainerProps;
import megalodonte.props.TextProps;
import megalodonte_app1_welcome.WelcomeAppTest;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.contracts.BackendContract;

/** Contract and source-compatibility tests; no Android/JUnit dependency. */
public final class BridgeTest {
    static final class FakeBridge implements NativeBridge {
        Consumer<Event> events;
        Component root;
        final Map<String, String> texts = new HashMap<>();
        final Map<String, Object> values = new HashMap<>();
        final Map<String, List<Component>> lists = new HashMap<>();
        int closeCount;
        @Override public void mount(Component root, Consumer<Event> events) {
            this.root = root; this.events = events; texts.clear(); index(root);
        }
        private void index(Component component) {
            if (component instanceof Text text) texts.put(component.bridgeId(), text.value());
            component.children().forEach(this::index);
        }
        @Override public void setText(String id, String value) { texts.put(id, value); }
        @Override public void setValue(String id, Object value) { values.put(id, value); if (value instanceof String text) texts.put(id, text); }
        @Override public void setChildren(String id, List<Component> children) {
            lists.put(id, List.copyOf(children));
            children.forEach(this::index);
        }
        @Override public void close() { closeCount++; }
        @Override public void applyTheme(ThemeInterface theme) { }
        @Override public BackendContract contract() { return new BackendContract("test", 1, java.util.EnumSet.allOf(megalodonte.contracts.Capability.class)); }
        void click(Button button) { events.accept(new Event(button.bridgeId(), "click")); }
    }
    static final class CounterScreen implements ScreenComponent {
        final State<Integer> clicks = State.of(0);
        int mounts, destroys;
        Text text;
        Button button;
        @Override public Component render() {
            text = new Text(clicks.map(value -> "Cliques: " + value), new TextProps().fontSize(30));
            button = new Button("Incrementar").onClick(() -> clicks.set(clicks.get() + 1));
            return new Container(new ContainerProps().paddingAll(20)).children(text, button);
        }
        @Override public void onMount() { mounts++; }
        @Override public void onDestroy() { destroys++; }
        @Override public Map<String, String> saveState() { return Map.of("clicks", clicks.get().toString()); }
        @Override public void restoreState(Map<String, String> saved) {
            clicks.set(Integer.parseInt(saved.getOrDefault("clicks", "0")));
        }
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        int[] menuCalls = {0};
        MenuBar menuBar = MenuBar.of()
                .title("Meu app")
                .subtitle("Área principal")
                .bgColor("#112233")
                .textColor("#FFFFFF")
                .menu(new Menu("Projeto")
                .item("Novo", () -> menuCalls[0]++)
                .itemIf(false, "Oculto", () -> menuCalls[0]++));
        require(menuBar.getMenus().size() == 1 && menuBar.getMenus().get(0).getItems().size() == 1,
                "MenuBar must preserve the desktop menu/item contract");
        require("Meu app".equals(menuBar.getTitle()) && "Área principal".equals(menuBar.getSubtitle())
                        && "#112233".equals(menuBar.getBackgroundColor())
                        && "#FFFFFF".equals(menuBar.getTextColor()),
                "MenuBar must preserve native toolbar presentation");
        MenuItem menuAction = menuBar.getMenus().get(0).getItems().get(0);
        menuAction.run();
        require(menuCalls[0] == 1 && "Novo".equals(menuAction.getTitle()),
                "Native menu command must run its declared action");
        Menu directAction = new Menu("Atualizar", () -> menuCalls[0]++);
        require(directAction.hasAction() && directAction.getItems().isEmpty(),
                "A direct Menu action must remain a leaf command");
        directAction.run();
        require(menuCalls[0] == 2, "A direct Menu action must run its callback");
        try {
            directAction.item("Inválido", () -> {});
            throw new AssertionError("A direct Menu action must reject nested items");
        } catch (IllegalStateException expected) { }
        FakeBridge welcomeBridge = new FakeBridge();
        try (Context ignored = MegalodonteApp.launch(welcomeBridge, Map.of(), WelcomeAppTest::runOriginalMain)) {
            require(welcomeBridge.root instanceof Container, "Original client must render Container");
            Container container = (Container) welcomeBridge.root;
            Text hello = (Text) container.children().get(0);
            require(container.props().getPadding() == 20 && hello.props().getFontSize() == 50
                && "Hello World".equals(hello.value()), "Original props/content must survive mounting");
        }
        FakeBridge bridge = new FakeBridge();
        CounterScreen screen = new CounterScreen();
        Context app = MegalodonteApp.launch(bridge, Map.of(),
            () -> MegalodonteApp.run(context -> context.useView(screen)));
        require("Cliques: 0".equals(bridge.texts.get(screen.text.bridgeId())) && screen.mounts == 1,
            "State binding and lifecycle must be automatic");
        bridge.click(screen.button); bridge.click(screen.button);
        require(screen.clicks.get() == 2 && "Cliques: 2".equals(bridge.texts.get(screen.text.bridgeId())),
            "Client onClick/State.map must update text without commands or ids");
        bridge.events.accept(new NativeBridge.Event("unknown", "click"));
        bridge.events.accept(new NativeBridge.Event(screen.button.bridgeId(), "unknown"));
        require(screen.clicks.get() == 2, "Unknown events must be ignored");
        Map<String, String> saved = app.saveState();
        app.close(); app.close();
        bridge.click(screen.button);
        screen.clicks.set(3);
        require(bridge.closeCount == 1 && screen.destroys == 1
            && "Cliques: 2".equals(bridge.texts.get(screen.text.bridgeId())), "Close must stop events/bindings once");
        FakeBridge restoredBridge = new FakeBridge();
        CounterScreen restored = new CounterScreen();
        Context restoredApp = MegalodonteApp.launch(restoredBridge, saved,
            () -> MegalodonteApp.run(context -> context.useView(restored)));
        require("Cliques: 2".equals(restoredBridge.texts.get(restored.text.bridgeId())),
            "State must be restored before render");
        Consumer<NativeBridge.Event> oldEvents = restoredBridge.events;
        CounterScreen replacement = new CounterScreen();
        restoredApp.useView(replacement);
        oldEvents.accept(new NativeBridge.Event(restored.button.bridgeId(), "click"));
        restored.clicks.set(9);
        require(restored.destroys == 1 && replacement.mounts == 1
            && "Cliques: 0".equals(restoredBridge.texts.get(replacement.text.bridgeId())),
            "Replacing a screen must dispose the old runtime/bindings");
        restoredBridge.click(replacement.button);
        require(replacement.clicks.get() == 1, "Replacement must receive events");
        restoredApp.close();
        Text reused = new Text("reused");
        try {
            new BridgeRuntime(new FakeBridge(), new Container().children(reused, reused));
            throw new AssertionError("Expected duplicate instance validation");
        } catch (IllegalArgumentException expected) { }
        try {
            MegalodonteApp.run(context -> {});
            throw new AssertionError("Expected platform host validation");
        } catch (IllegalStateException expected) { }
        FakeBridge failedBridge = new FakeBridge();
        try {
            MegalodonteApp.launch(failedBridge, Map.of(), () -> {});
            throw new AssertionError("Expected missing view validation");
        } catch (IllegalStateException expected) { }
        require(failedBridge.closeCount == 1, "Failed bootstrap must dispose host");
        State<Boolean> checked = State.of(false);
        State<Boolean> switched = State.of(false);
        State<Integer> progress = State.of(10);
        megalodonte.components.Checkbox checkbox = new megalodonte.components.Checkbox("Checked", checked);
        megalodonte.components.Switch toggle = new megalodonte.components.Switch("Notifications", switched);
        megalodonte.components.ProgressBar bar = new megalodonte.components.ProgressBar(progress);
        FakeBridge controls = new FakeBridge();
        BridgeRuntime controlRuntime = new BridgeRuntime(controls, new Container().children(checkbox, toggle, bar));
        controls.events.accept(new NativeBridge.Event(checkbox.bridgeId(), "change", true));
        controls.events.accept(new NativeBridge.Event(toggle.bridgeId(), "change", true));
        progress.set(65);
        require(checked.get() && switched.get() && Integer.valueOf(65).equals(controls.values.get(bar.bridgeId())),
            "Control bindings must be bidirectional/reactive");
        controlRuntime.close();
        progress.set(99);
        require(Integer.valueOf(65).equals(controls.values.get(bar.bridgeId())), "Controls must detach on disposal");
        FakeBridge routeBridge = new FakeBridge();
        try (Context ignored = MegalodonteApp.launch(routeBridge, Map.of(), () ->
                MegalodonteApp.run(context -> {
                    var router = new megalodonte.application.AndroidRouter(context)
                            .route("/list", () -> () -> new Text("List"))
                            .route("/form", () -> () -> new Text("Form"));
                    router.start("/list");
                    require("List".equals(((Text) routeBridge.root).value()), "Initial route must mount");
                    router.navigate("/form");
                    require("Form".equals(((Text) routeBridge.root).value()), "Navigation must mount the destination");
                    require(router.back() && "List".equals(((Text) routeBridge.root).value()), "Back must restore the previous route");
                    require(!router.back(), "Back at the entry route must return false");
                }))) { }
        System.out.println("Bridge, lifecycle and unchanged desktop welcome client tests passed");
    }
}

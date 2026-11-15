package megalodonte.android.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import megalodonte.application.Context;
import megalodonte.application.MegalodonteApp;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.components.Button;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.contracts.BackendContract;
import megalodonte.contracts.Capability;
import megalodonte.v2.ListState;

/** Reactive list rendering contract; runs without Android or a test framework. */
public final class ListRenderingTest {

    static final class RecordingBridge implements NativeBridge {
        Component root;
        Consumer<Event> events;
        final Map<String, String> texts = new HashMap<>();
        final Map<String, List<Component>> lists = new HashMap<>();
        boolean supportsLists = true;
        int setChildrenCalls;
        int closeCount;

        @Override public void mount(Component root, Consumer<Event> events) {
            this.root = root;
            this.events = events;
            index(root);
        }
        private void index(Component component) {
            if (component instanceof Text text) texts.put(component.bridgeId(), text.value());
            component.children().forEach(this::index);
        }
        @Override public void setChildren(String componentId, List<Component> children) {
            setChildrenCalls++;
            lists.put(componentId, List.copyOf(children));
            children.forEach(this::index);
        }
        @Override public void setText(String componentId, String value) { texts.put(componentId, value); }
        @Override public void setValue(String componentId, Object value) { }
        @Override public void applyTheme(ThemeInterface theme) { }
        @Override public BackendContract contract() {
            EnumSet<Capability> capabilities = EnumSet.allOf(Capability.class);
            if (!supportsLists) capabilities.remove(Capability.LIST_RENDERING);
            return new BackendContract("test", BackendContract.CURRENT_VERSION, capabilities);
        }
        @Override public void close() { closeCount++; }
        void click(Button button) { events.accept(new Event(button.bridgeId(), "click")); }
    }

    static final class MountListScreen implements ScreenComponent {
        final ListState<String> names = ListState.ofEmpty();
        final Column column = new Column().items(names, name -> new Text(name));
        int renders;

        @Override public Component render() {
            renders++;
            return new Container().children(column);
        }
        @Override public void onMount() {
            names.add("first");
            names.add("second");
        }
    }

    static final class MutableLabel {
        String value;
        MutableLabel(String value) { this.value = value; }
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String listId(Column column) {
        return column.children().get(0).bridgeId();
    }

    public static void main(String[] args) {
        State<Integer> clicks = State.of(0);
        ListState<String> names = ListState.of(List.of("Ada"));
        Column column = new Column().items(names, name ->
                new Button(name).onClick(() -> clicks.set(clicks.get() + 1)));
        RecordingBridge bridge = new RecordingBridge();
        BridgeRuntime runtime = new BridgeRuntime(bridge, column);
        try {
            String id = listId(column);
            require(bridge.setChildrenCalls == 0, "Initial list must be part of the mount, not a replacement");

            names.add("Grace");
            List<Component> rendered = bridge.lists.get(id);
            require(bridge.setChildrenCalls == 1 && rendered != null && rendered.size() == 2,
                    "Adding to a ListState must replace the list views");
            Button ada = (Button) rendered.get(0);
            bridge.click(ada);
            require(clicks.get() == 1, "Buttons created by the mapper must dispatch clicks");

            names.remove("Ada");
            require(bridge.lists.get(id).size() == 1, "Removing from a ListState must replace the list views");
            bridge.click(ada);
            require(clicks.get() == 1, "Buttons removed with the list must stop dispatching");

            names.clear();
            require(bridge.lists.get(id).isEmpty(), "Clearing a ListState must release the list views");

            State<String> suffix = State.of("!");
            ListState<String> labels = ListState.of(List.of("one"));
            Column textColumn = new Column().items(labels, label -> new Text(suffix.map(value -> label + value)));
            RecordingBridge textBridge = new RecordingBridge();
            try (BridgeRuntime textRuntime = new BridgeRuntime(textBridge, textColumn)) {
                String textListId = listId(textColumn);
                labels.add("two");
                List<Component> firstSnapshot = textBridge.lists.get(textListId);
                String firstId = firstSnapshot.get(0).bridgeId();
                String secondId = firstSnapshot.get(1).bridgeId();
                suffix.set("?");
                require("one?".equals(textBridge.texts.get(firstId)) && "two?".equals(textBridge.texts.get(secondId)),
                        "Text states inside mapped items must bind reactively");

                labels.clear();
                suffix.set("#");
                require("one?".equals(textBridge.texts.get(firstId)),
                        "Text states removed with the list must stop updating");
            }

            RecordingBridge noLists = new RecordingBridge();
            noLists.supportsLists = false;
            try {
                new BridgeRuntime(noLists, new Column().items(
                        ListState.of(List.of("x")), value -> new Text(value)));
                throw new AssertionError("Expected LIST_RENDERING capability validation");
            } catch (UnsupportedOperationException expected) {
                require(expected.getMessage().contains("LIST_RENDERING"), "Missing capability must be named");
            }

            MutableLabel label = new MutableLabel("before");
            ListState<MutableLabel> mutable = ListState.of(List.of(label));
            Column mutableColumn = new Column().items(mutable, item -> new Text(item.value));
            RecordingBridge mutableBridge = new RecordingBridge();
            try (BridgeRuntime mutableRuntime = new BridgeRuntime(mutableBridge, mutableColumn)) {
                String mutableId = listId(mutableColumn);
                label.value = "after";
                mutable.refresh();
                require(((Text) mutableBridge.lists.get(mutableId).get(0)).value().equals("after"),
                        "ListState.refresh() must rebuild items mutated in place");
            }
        } finally {
            runtime.close();
        }
        require(bridge.closeCount == 1, "Closing the runtime must close the bridge once");

        RecordingBridge mountBridge = new RecordingBridge();
        MountListScreen screen = new MountListScreen();
        try (Context ignored = MegalodonteApp.launch(mountBridge, Map.of(),
                () -> MegalodonteApp.run(context -> context.useView(screen)))) {
            String id = listId(screen.column);
            List<Component> rendered = mountBridge.lists.get(id);
            require(screen.renders == 1, "onMount must run after a single render");
            require(mountBridge.setChildrenCalls == 2 && rendered != null && rendered.size() == 2,
                    "ListState changes in onMount must reach the mounted UI");
            require("first".equals(((Text) rendered.get(0)).value())
                    && "second".equals(((Text) rendered.get(1)).value()),
                    "List items must preserve their order");
        }
        System.out.println("Reactive list rendering tests passed");
    }
}

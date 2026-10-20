package megalodonte.android.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.components.Button;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Renders the portable subset of Megalodonte JSONC with native Android widgets. */
public final class AndroidJsoncScreen implements ScreenComponent {
    private final JSONObject definition;
    private final Map<String, State<String>> states = new HashMap<>();
    private Consumer<String> actions;

    public AndroidJsoncScreen(String source) {
        this(source, action -> { throw new IllegalStateException("No handler for JSONC action: " + action); });
    }

    public AndroidJsoncScreen(String source, Consumer<String> actions) {
        this.actions = java.util.Objects.requireNonNull(actions);
        try {
            definition = new JSONObject(withoutComments(source));
            JSONObject declarations = definition.optJSONObject("states");
            if (declarations != null) {
                var names = declarations.keys();
                while (names.hasNext()) {
                    String name = names.next();
                    JSONObject declaration = declarations.optJSONObject(name);
                    if (declaration == null || !"string".equals(declaration.optString("type")))
                        throw new IllegalArgumentException("Android JSONC supports string states only: " + name);
                    states.put(name, State.of(declaration.optString("initial", "")));
                }
            }
        } catch (JSONException error) {
            throw new IllegalArgumentException("Invalid JSONC view", error);
        }
    }

    public AndroidJsoncScreen onAction(Consumer<String> handler) {
        actions = java.util.Objects.requireNonNull(handler);
        return this;
    }

    public void setState(String name, String value) {
        State<String> state = states.get(name);
        if (state == null) throw new IllegalArgumentException("Unknown JSONC state: " + name);
        state.set(java.util.Objects.requireNonNull(value));
    }

    @Override public Component render() {
        JSONObject view = definition.optJSONObject("view");
        if (view == null) throw new IllegalArgumentException("JSONC view must contain a view object");
        return component(view);
    }

    private Component component(JSONObject node) {
        String type = node.optString("type");
        JSONObject props = node.optJSONObject("props");
        if (props == null) props = new JSONObject();
        JSONArray children = node.optJSONArray("children");
        List<Component> items = new ArrayList<>();
        if (children != null) for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.optJSONObject(i);
            if (child == null) throw new IllegalArgumentException("JSONC children must be objects");
            items.add(component(child));
        }
        return switch (type) {
            case "Text" -> {
                TextProps textProps = new TextProps();
                JSONObject style = node.optJSONObject("style");
                if (style != null && style.optInt("fontSize") > 0) textProps.fontSize(style.optInt("fontSize"));
                String binding = props.optString("bind", "");
                if (!binding.isBlank()) {
                    State<String> state = states.get(binding);
                    if (state == null) throw new IllegalArgumentException("Unknown JSONC state: " + binding);
                    yield new Text(state, textProps);
                }
                yield new Text(props.optString("text", ""), textProps);
            }
            case "Button" -> {
                String action = props.optString("action", "");
                yield new Button(props.optString("text", "Button"))
                        .onClick(() -> { if (!action.isBlank()) actions.accept(action); });
            }
            case "Column" -> new Column(layout(new ColumnProps(), props)).children(items.toArray(Component[]::new));
            case "Row" -> new Row(layout(new RowProps(), props)).children(items.toArray(Component[]::new));
            case "Container" -> new Container(layout(new ContainerProps(), props)).children(items.toArray(Component[]::new));
            default -> throw new IllegalArgumentException("Unsupported Android JSONC component: " + type);
        };
    }

    private static <T extends megalodonte.props.LayoutProps<T>> T layout(T result, JSONObject props) {
        if (props.has("padding")) result.paddingAll(props.optInt("padding"));
        if (props.has("spacing")) result.spacingOf(props.optInt("spacing"));
        if (props.optBoolean("fillWidth")) result.fillWidth();
        return result;
    }

    private static String withoutComments(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean quoted = false, escaped = false, line = false, block = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i), next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (line) {
                if (c == '\n' || c == '\r') { line = false; result.append(c); }
            } else if (block) {
                if (c == '*' && next == '/') { block = false; i++; }
                else if (c == '\n' || c == '\r') result.append(c);
            } else if (quoted) {
                result.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') { quoted = true; result.append(c); }
            else if (c == '/' && next == '/') { line = true; i++; }
            else if (c == '/' && next == '*') { block = true; i++; }
            else result.append(c);
        }
        if (quoted || block) throw new IllegalArgumentException("Unterminated JSONC string or comment");
        return result.toString();
    }
}

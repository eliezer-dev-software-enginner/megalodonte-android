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
import megalodonte.components.Scroll;
import megalodonte.components.inputs.Input;
import megalodonte.components.inputs.PasswordInput;
import megalodonte.components.inputs.TextAreaInput;
import megalodonte.components.inputs.OnChangeResult;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import megalodonte.props.InputProps;
import megalodonte.props.ButtonProps;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Renders the portable subset of Megalodonte JSONC with native Android widgets. */
public final class AndroidJsoncScreen implements ScreenComponent {
    private final JSONObject definition;
    private final Map<String, State<?>> states = new HashMap<>();
    private final Map<String, Component> slots = new HashMap<>();
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
                    if (declaration == null) throw new IllegalArgumentException("Invalid JSONC state: " + name);
                    State<?> state = switch (declaration.optString("type")) {
                        case "string" -> State.of(declaration.optString("initial", ""));
                        case "boolean" -> State.of(declaration.optBoolean("initial"));
                        case "integer" -> State.of(declaration.optInt("initial"));
                        case "decimal" -> State.of(declaration.optString("initial", "0"));
                        default -> throw new IllegalArgumentException("Unsupported JSONC state type: " + name);
                    };
                    states.put(name, state);
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
        stringState(name).set(java.util.Objects.requireNonNull(value));
    }

    public AndroidJsoncScreen slot(String name, Component content) {
        slots.put(java.util.Objects.requireNonNull(name), java.util.Objects.requireNonNull(content));
        return this;
    }

    private State<String> stringState(String name) {
        State<?> state = states.get(name);
        if (state == null || !(state.get() instanceof String))
            throw new IllegalArgumentException("Unknown string JSONC state: " + name);
        @SuppressWarnings("unchecked") State<String> typed = (State<String>) state;
        return typed;
    }

    private State<Integer> integerState(String name) {
        State<?> state = states.get(name);
        if (state == null || !(state.get() instanceof Integer))
            throw new IllegalArgumentException("Unknown integer JSONC state: " + name);
        @SuppressWarnings("unchecked") State<Integer> typed = (State<Integer>) state;
        return typed;
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
        JSONObject bind = node.optJSONObject("bind");
        if (bind == null) bind = new JSONObject();
        JSONObject events = node.optJSONObject("events");
        if (events == null) events = new JSONObject();
        JSONObject format = node.optJSONObject("format");
        if (format == null) format = new JSONObject();
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
                String binding = bind.optString("text", props.optString("bind", ""));
                if (!binding.isBlank()) {
                    State<?> state = states.get(binding);
                    if (state == null) throw new IllegalArgumentException("Unknown JSONC state: " + binding);
                    JSONObject textFormat = format;
                    yield new Text(state.map(value -> display(value, textFormat)), textProps);
                }
                yield new Text(display(props.optString("text", ""), format), textProps);
            }
            case "Button" -> {
                String action = events.optString("onClick", props.optString("action", ""));
                ButtonProps buttonProps = new ButtonProps();
                if (props.optBoolean("fillWidth")) buttonProps.fillWidth();
                if (props.has("height")) buttonProps.height(props.optInt("height"));
                JSONObject style = node.optJSONObject("style");
                if (style != null && style.optInt("fontSize") > 0) buttonProps.fontSize(style.optInt("fontSize"));
                yield new Button(props.optString("text", "Button"), buttonProps)
                        .onClick(() -> { if (!action.isBlank()) actions.accept(action); });
            }
            case "Column" -> new Column(layout(new ColumnProps(), props)).children(items.toArray(Component[]::new));
            case "Row" -> new Row(layout(new RowProps(), props)).children(items.toArray(Component[]::new));
            case "Container" -> new Container(layout(new ContainerProps(), props)).children(items.toArray(Component[]::new));
            case "Scroll" -> {
                if (items.size() != 1) throw new IllegalArgumentException("Scroll requires one child");
                yield new Scroll(items.get(0));
            }
            case "Input", "PasswordInput", "TextArea" -> {
                String key = bind.optString("value", props.optString("bind", ""));
                State<String> state = key.isBlank() ? State.of(props.optString("value", "")) : stringState(key);
                InputProps inputProps = new InputProps().placeHolder(props.optString("placeholder", ""));
                if (props.has("width")) inputProps.width(props.optInt("width"));
                if (props.has("height")) inputProps.height(props.optInt("height"));
                if (props.optBoolean("disabled")) inputProps.disable();
                if (props.has("editable")) inputProps.editable(props.optBoolean("editable"));
                if (props.optBoolean("readOnly")) inputProps.editable(false);
                Input input = type.equals("PasswordInput") ? new PasswordInput(state, inputProps)
                        : type.equals("TextArea") ? new TextAreaInput(state, inputProps)
                        : new Input(state, inputProps);
                if (!format.optString("kind").isBlank()) {
                    if (type.equals("PasswordInput")) throw new IllegalArgumentException("PasswordInput does not accept format");
                    JSONObject inputFormat = format;
                    input.onChange(text -> OnChangeResult.of(display(storage(text, inputFormat), inputFormat),
                            storage(text, inputFormat)));
                    input.onInitialize(text -> OnChangeResult.of(display(text, inputFormat), text));
                }
                String enter = events.optString("onEnter", "");
                if (!enter.isBlank()) input.onEnter(() -> actions.accept(enter));
                yield input;
            }
            case "Slot" -> slots.getOrDefault(props.optString("name"), new Container());
            case "Select", "DatePicker", "Image", "ImagePreview", "ImageSequence", "SoundSequence",
                    "DataList", "DiffView", "Table", "BarChart", "PieChart" -> {
                String dataName = switch (type) {
                    case "Image", "ImagePreview" -> bind.optString("source", "");
                    case "DiffView" -> bind.optString("text", "");
                    default -> bind.optString("items", "");
                };
                State<String> data = dataName.isBlank() ? null : stringState(dataName);
                String valueName = bind.optString("value", type.equals("Table") ? bind.optString("selected", "") : "");
                State<String> value = valueName.isBlank() ? null : stringState(valueName);
                String indexName = type.equals("Table") ? "" : bind.optString("selected", "");
                State<Integer> selected = indexName.isBlank() ? null : integerState(indexName);
                String selectedItemsName = bind.optString("selectedItems", "");
                State<String> selectedItems = selectedItemsName.isBlank() ? null : stringState(selectedItemsName);
                yield new AndroidJsoncControl(type, props, data, value, selected, selectedItems,
                        events.optString("onSelect", ""), events.optString("onDoubleClick", ""),
                        action -> actions.accept(action));
            }
            default -> throw new IllegalArgumentException("Unsupported Android JSONC component: " + type);
        };
    }

    private static <T extends megalodonte.props.LayoutProps<T>> T layout(T result, JSONObject props) {
        if (props.has("padding")) result.paddingAll(props.optInt("padding"));
        if (props.has("spacing")) result.spacingOf(props.optInt("spacing"));
        if (props.optBoolean("fillWidth")) result.fillWidth();
        if (props.optBoolean("fillHeight")) result.fillHeight();
        if (props.has("width")) result.width(props.optInt("width"));
        if (props.has("height")) result.height(props.optInt("height"));
        if (props.has("bgColor")) result.bgColor(props.optString("bgColor"));
        if ("center".equals(props.optString("alignment"))) {
            result.centerHorizontally(); result.centerVertically();
        }
        return result;
    }

    private static String storage(String text, JSONObject format) {
        return switch (format.optString("kind")) {
            case "mask" -> {
                String digits = text.replaceAll("[^0-9]", "");
                String pattern = format.optString("pattern");
                int count = (int) pattern.chars().filter(c -> c == '#').count();
                yield digits.substring(0, Math.min(digits.length(), count));
            }
            case "currency" -> text.replaceAll("[^0-9]", "");
            case "uppercase" -> text.toUpperCase(java.util.Locale.ROOT);
            case "lowercase" -> text.toLowerCase(java.util.Locale.ROOT);
            default -> text;
        };
    }

    private static String display(Object value, JSONObject format) {
        String text = String.valueOf(value == null ? "" : value);
        String kind = format.optString("kind");
        if (kind.equals("mask")) {
            String digits = storage(text, format), pattern = format.optString("pattern");
            StringBuilder shown = new StringBuilder();
            int next = 0;
            for (char c : pattern.toCharArray()) {
                if (next >= digits.length()) break;
                shown.append(c == '#' ? digits.charAt(next++) : c);
            }
            return shown.toString();
        }
        if (kind.equals("currency") || kind.equals("number")) {
            java.util.Locale locale = java.util.Locale.forLanguageTag(format.optString("locale", "pt-BR"));
            java.text.NumberFormat number = kind.equals("currency")
                    ? java.text.NumberFormat.getCurrencyInstance(locale) : java.text.NumberFormat.getNumberInstance(locale);
            if (kind.equals("currency")) number.setCurrency(java.util.Currency.getInstance(format.optString("currency", "BRL")));
            java.math.BigDecimal decimal = kind.equals("currency") && value instanceof String
                    ? new java.math.BigDecimal(storage(text, format).isEmpty() ? "0" : storage(text, format)).movePointLeft(2)
                    : new java.math.BigDecimal(text.isBlank() ? "0" : text);
            return number.format(decimal);
        }
        return storage(text, format);
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

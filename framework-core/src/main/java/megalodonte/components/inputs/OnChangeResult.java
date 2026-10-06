package megalodonte.components.inputs;
public record OnChangeResult(String displayValue, String stateValue) {
    public String getDisplayValue() { return displayValue; }
    public String getStateValue() { return stateValue; }
    public static OnChangeResult of(String display, String state) { return new OnChangeResult(display, state); }
    public static OnChangeResult same(String value) { return new OnChangeResult(value, value); }
}

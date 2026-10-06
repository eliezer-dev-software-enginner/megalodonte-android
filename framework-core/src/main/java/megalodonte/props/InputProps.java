package megalodonte.props;
public class InputProps extends TextProps {
    private String placeholder = "";
    private boolean editable = true;
    private boolean disabled;
    public InputProps placeHolder(String value) { placeholder = value; return this; }
    public InputProps editable(boolean value) { editable = value; return this; }
    public InputProps disable() { disabled = true; return this; }
    @Override public InputProps fontSize(int value) { super.fontSize(value); return this; }
    public String getPlaceholder() { return placeholder; }
    public boolean isEditable() { return editable; }
    public boolean isDisabled() { return disabled; }
}

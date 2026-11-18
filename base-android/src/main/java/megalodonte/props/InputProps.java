package megalodonte.props;
public class InputProps extends TextProps {
    private String placeholder = "";
    private boolean editable = true;
    private boolean disabled;
    private int width = -1, height = -2;
    public InputProps placeHolder(String value) { placeholder = value; return this; }
    public InputProps editable(boolean value) { editable = value; return this; }
    public InputProps disable() { disabled = true; return this; }
    public InputProps width(int value) { if (value <= 0) throw new IllegalArgumentException("Input width must be positive"); width = value; return this; }
    public InputProps height(int value) { if (value <= 0) throw new IllegalArgumentException("Input height must be positive"); height = value; return this; }
    @Override public InputProps fontSize(int value) { super.fontSize(value); return this; }
    public String getPlaceholder() { return placeholder; }
    public boolean isEditable() { return editable; }
    public boolean isDisabled() { return disabled; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}

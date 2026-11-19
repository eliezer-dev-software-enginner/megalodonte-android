package megalodonte.props;
public class InputProps extends TextProps {
    public enum Variant { PLAIN, FILLED, OUTLINED }
    public enum ImeAction { DEFAULT, DONE, NEXT, PREVIOUS, GO, SEARCH, SEND }

    private String placeholder = "";
    private String label = "";
    private boolean editable = true;
    private boolean disabled;
    private int width = -1, height = -2;
    private Variant variant = Variant.PLAIN;
    private ImeAction imeAction = ImeAction.DEFAULT;
    public InputProps placeHolder(String value) { placeholder = value; return this; }
    public InputProps label(String value) { label = value == null ? "" : value; return this; }
    public InputProps variant(Variant value) { variant = java.util.Objects.requireNonNull(value); return this; }
    public InputProps filled() { return variant(Variant.FILLED); }
    public InputProps outlined() { return variant(Variant.OUTLINED); }
    public InputProps imeAction(ImeAction value) { imeAction = java.util.Objects.requireNonNull(value); return this; }
    public InputProps editable(boolean value) { editable = value; return this; }
    public InputProps disable() { disabled = true; return this; }
    public InputProps width(int value) { if (value <= 0) throw new IllegalArgumentException("Input width must be positive"); width = value; return this; }
    public InputProps height(int value) { if (value <= 0) throw new IllegalArgumentException("Input height must be positive"); height = value; return this; }
    @Override public InputProps fontSize(int value) { super.fontSize(value); return this; }
    public String getPlaceholder() { return placeholder; }
    public String getLabel() { return label; }
    public Variant getVariant() { return variant; }
    public ImeAction getImeAction() { return imeAction; }
    public boolean isEditable() { return editable; }
    public boolean isDisabled() { return disabled; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}

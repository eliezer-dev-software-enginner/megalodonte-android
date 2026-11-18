package megalodonte.props;

public class LayoutProps<T extends LayoutProps<T>> {
    private int padding, spacing;
    private double width = -2, height = -2;
    private boolean centerHorizontal, centerVertical;
    private String bgColor;
    @SuppressWarnings("unchecked") protected T self() { return (T) this; }
    public T paddingAll(int value) { if (value < 0) throw new IllegalArgumentException("Negative padding"); padding = value; return self(); }
    public T spacingOf(int value) { if (value < 0) throw new IllegalArgumentException("Negative spacing"); spacing = value; return self(); }
    public T width(double value) { if (value < 0) throw new IllegalArgumentException("Negative width"); width = value; return self(); }
    public T height(double value) { if (value < 0) throw new IllegalArgumentException("Negative height"); height = value; return self(); }
    public T fillWidth() { width = -1; return self(); }
    public T hugWidth() { width = -2; return self(); }
    public T fillHeight() { height = -1; return self(); }
    public T centerHorizontally() { centerHorizontal = true; return self(); }
    public T centerVertically() { centerVertical = true; return self(); }
    public T bgColor(String value) { bgColor = value; return self(); }
    public int getPadding() { return padding; }
    public int getSpacing() { return spacing; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isCenterHorizontal() { return centerHorizontal; }
    public boolean isCenterVertical() { return centerVertical; }
    public String getBgColor() { return bgColor; }
}

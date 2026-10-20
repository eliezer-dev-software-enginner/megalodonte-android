package megalodonte.props;

public class ButtonProps {
    private Integer fontSize;
    private boolean fillWidth;
    private int height = -2;
    public ButtonProps fontSize(int value) {
        if (value <= 0) throw new IllegalArgumentException("Font size must be positive");
        fontSize = value;
        return this;
    }
    public Integer getFontSize() { return fontSize; }
    public ButtonProps fillWidth() { fillWidth = true; return this; }
    public ButtonProps height(int value) { if (value <= 0) throw new IllegalArgumentException("Button height must be positive"); height = value; return this; }
    public boolean isFillWidth() { return fillWidth; }
    public int getHeight() { return height; }
}

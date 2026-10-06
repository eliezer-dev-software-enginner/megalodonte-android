package megalodonte.props;

public class ButtonProps {
    private Integer fontSize;
    public ButtonProps fontSize(int value) {
        if (value <= 0) throw new IllegalArgumentException("Font size must be positive");
        fontSize = value;
        return this;
    }
    public Integer getFontSize() { return fontSize; }
}

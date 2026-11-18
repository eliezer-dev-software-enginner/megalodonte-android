package megalodonte.props;

public class TextProps {
    private Integer fontSize;
    public TextProps fontSize(int value) {
        if (value <= 0) throw new IllegalArgumentException("Font size must be positive");
        fontSize = value;
        return this;
    }
    public Integer getFontSize() { return fontSize; }
}

package megalodonte.props;

import megalodonte.base.theme.ThemeInterface;

/** Visual properties for a native switch label. */
public final class SwitchProps extends TextProps {
    private String textColor;

    @Override public SwitchProps fontSize(int value) {
        super.fontSize(value);
        return this;
    }

    public SwitchProps textColor(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Text color must not be blank");
        textColor = value;
        return this;
    }

    public String resolveTextColor(ThemeInterface theme) {
        return textColor == null ? theme.colors().textPrimary() : textColor;
    }
}

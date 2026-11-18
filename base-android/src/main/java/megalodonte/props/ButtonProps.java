package megalodonte.props;

import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.common_platform.ButtonSize;
import megalodonte.common_platform.ButtonStyle;
import megalodonte.common_platform.ButtonVariant;

public class ButtonProps {
    private Integer fontSize;
    private boolean fillWidth;
    private int height = -2;

    private State<String> bgColorState;

    private static final int UNSET = -1;

    private ButtonVariant variant = ButtonVariant.PRIMARY;
    private ButtonStyle style = ButtonStyle.FILLED;
    private ButtonSize size = ButtonSize.MEDIUM;
    private String bgColor, textColor, borderColor;
    private int borderWidth, borderRadius;
    private int paddingTop = UNSET, paddingRight = UNSET, paddingDown = UNSET, paddingLeft = UNSET;

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


    public ButtonProps variant(ButtonVariant v) { variant = v; return this; }
    public ButtonProps primary()   { variant = ButtonVariant.PRIMARY; return this; }
    public ButtonProps secondary() { variant = ButtonVariant.SECONDARY; return this; }
    public ButtonProps success()   { variant = ButtonVariant.SUCCESS; return this; }
    public ButtonProps warning()   { variant = ButtonVariant.WARNING; return this; }
    public ButtonProps danger()    { variant = ButtonVariant.DANGER; return this; }
    public ButtonProps ghost()     { variant = ButtonVariant.GHOST; return this; }
    public ButtonProps disabled()  { variant = ButtonVariant.DISABLED; return this; }

    public ButtonProps style(ButtonStyle s) { style = s; return this; }
    public ButtonProps filled()   { style = ButtonStyle.FILLED; return this; }
    public ButtonProps outlined() { style = ButtonStyle.OUTLINED; return this; }
    public ButtonProps text()     { style = ButtonStyle.TEXT; return this; }

    public ButtonProps size(ButtonSize s) { size = s; return this; }
    public ButtonProps small()  { size = ButtonSize.SMALL; return this; }
    public ButtonProps medium() { size = ButtonSize.MEDIUM; return this; }
    public ButtonProps large()  { size = ButtonSize.LARGE; return this; }

    public ButtonProps bgColor(String c) { bgColor = c; return this; }
    public ButtonProps bgColorState(State<String> c) { bgColorState = c; return this; }
    public ButtonProps textColor(String c) { textColor = c; return this; }
    public ButtonProps borderColor(String c) { borderColor = c; return this; }
    public ButtonProps borderWidth(int w) { borderWidth = w; return this; }
    public ButtonProps borderRadius(int r) { borderRadius = r; return this; }

    public ButtonProps paddingAll(int u) { paddingTop = paddingRight = paddingDown = paddingLeft = u; return this; }
    public ButtonProps paddingTop(int u) { paddingTop = u; return this; }
    public ButtonProps paddingRight(int u) { paddingRight = u; return this; }
    public ButtonProps paddingDown(int u) { paddingDown = u; return this; }
    public ButtonProps paddingLeft(int u) { paddingLeft = u; return this; }

    public ButtonVariant getVariant() { return variant; }

    // ---- Resolução (mesmas regras do ButtonProps.applyTheme do JavaFX, sem tipos Android) ----

    private record SizePreset(int fontSize, int paddingH, int paddingV) {}

    private SizePreset preset() {
        return switch (size) {
            case SMALL -> new SizePreset(13, 10, 4);
            case MEDIUM -> new SizePreset(14, 16, 8);
            case LARGE -> new SizePreset(16, 22, 11);
        };
    }

    public String resolveAccentColor(ThemeInterface theme) {
        if (bgColor != null) return bgColor;
        return switch (variant) {
            case SECONDARY -> theme.colors().secondary();
            case SUCCESS -> theme.colors().success();
            case WARNING -> theme.colors().warning();
            case DANGER -> theme.colors().danger();
            case GHOST -> "transparent";
            case DISABLED -> theme.colors().textSecondary();
            case PRIMARY -> theme.colors().primary();
        };
    }

    public String resolveBackgroundColor(ThemeInterface theme) {
        return style == ButtonStyle.FILLED ? resolveAccentColor(theme) : "transparent";
    }

    public String resolveTextColor(ThemeInterface theme) {
        if (textColor != null) return textColor;
        return switch (style) {
            case FILLED -> (variant == ButtonVariant.GHOST || variant == ButtonVariant.DISABLED)
                    ? theme.colors().textSecondary() : "white";
            case OUTLINED, TEXT -> resolveAccentColor(theme);
        };
    }

    public int resolveBorderWidth(ThemeInterface theme) {
        if (borderWidth > 0) return borderWidth;
        return style == ButtonStyle.OUTLINED ? Math.max(1, theme.border().width()) : 0;
    }

    public String resolveBorderColor(ThemeInterface theme) {
        return borderColor != null ? borderColor : resolveAccentColor(theme);
    }

    public int resolveBorderRadius(ThemeInterface theme) {
        return borderRadius > 0 ? borderRadius : theme.border().radiusMd();
    }

    public int resolveFontSize() {
        return fontSize != null ? fontSize : preset().fontSize();
    }

    /** {top, right, bottom, left}, em unidades (a bridge converte pra dp). Lado não setado cai no preset do size. */
    public int[] resolvePaddingUnits() {
        var p = preset();
        return new int[] {
                paddingTop != UNSET ? paddingTop : p.paddingV(),
                paddingRight != UNSET ? paddingRight : p.paddingH(),
                paddingDown != UNSET ? paddingDown : p.paddingV(),
                paddingLeft != UNSET ? paddingLeft : p.paddingH()
        };
    }


}

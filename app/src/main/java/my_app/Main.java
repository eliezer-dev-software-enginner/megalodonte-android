package my_app;

import megalodonte.application.MegalodonteApp;
import megalodonte.base.theme.ThemeManager;
import megalodonte.theme.DefaultTheme;

public class Main {
    public static void main() {
        ThemeManager.setTheme(new DefaultTheme());
        MegalodonteApp.run(context -> context.useView(new WelcomeScreen()));
    }
}

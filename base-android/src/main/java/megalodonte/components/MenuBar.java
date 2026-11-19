package megalodonte.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import megalodonte.base.components.Component;

/** Declarative menu bar. The Android bridge renders its menus in a native Toolbar. */
public final class MenuBar extends Component {
    private final List<Menu> menus = new ArrayList<>();
    private String title = "";
    private String subtitle = "";
    private String backgroundColor;
    private String textColor;

    public MenuBar addMenu(Menu menu) { menus.add(Objects.requireNonNull(menu)); return this; }
    public MenuBar menu(Menu menu) { return addMenu(menu); }
    public MenuBar menuIf(boolean condition, Menu menu) { return condition ? addMenu(menu) : this; }
    public MenuBar menu(String title, MenuItem... items) {
        Menu menu = new Menu(title);
        for (MenuItem item : items) menu.addItem(item);
        return addMenu(menu);
    }
    /** Sets the native Android Toolbar title. */
    public MenuBar title(String value) { title = Objects.requireNonNull(value); return this; }
    /** Sets the optional native Android Toolbar subtitle. */
    public MenuBar subtitle(String value) { subtitle = Objects.requireNonNull(value); return this; }
    /** Overrides the theme surface used by the native Toolbar. */
    public MenuBar bgColor(String value) { backgroundColor = Objects.requireNonNull(value); return this; }
    /** Overrides the theme primary text color for title, subtitle and overflow icon. */
    public MenuBar textColor(String value) { textColor = Objects.requireNonNull(value); return this; }
    public List<Menu> getMenus() { return List.copyOf(menus); }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getBackgroundColor() { return backgroundColor; }
    public String getTextColor() { return textColor; }
    public static MenuBar of() { return new MenuBar(); }
}

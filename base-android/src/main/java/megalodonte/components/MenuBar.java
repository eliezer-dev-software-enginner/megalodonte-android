package megalodonte.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import megalodonte.base.components.Component;

/** Declarative menu bar. The Android bridge renders its menus in a native Toolbar. */
public final class MenuBar extends Component {
    private final List<Menu> menus = new ArrayList<>();

    public MenuBar addMenu(Menu menu) { menus.add(Objects.requireNonNull(menu)); return this; }
    public MenuBar menu(Menu menu) { return addMenu(menu); }
    public MenuBar menuIf(boolean condition, Menu menu) { return condition ? addMenu(menu) : this; }
    public MenuBar menu(String title, MenuItem... items) {
        Menu menu = new Menu(title);
        for (MenuItem item : items) menu.addItem(item);
        return addMenu(menu);
    }
    public List<Menu> getMenus() { return List.copyOf(menus); }
    public static MenuBar of() { return new MenuBar(); }
}

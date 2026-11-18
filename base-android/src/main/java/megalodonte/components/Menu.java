package megalodonte.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Named group of menu commands, matching the desktop fluent API. */
public final class Menu {
    private String title;
    private final List<MenuItem> items = new ArrayList<>();

    public Menu(String title) { this.title = Objects.requireNonNull(title); }
    public Menu addItem(MenuItem item) { items.add(Objects.requireNonNull(item)); return this; }
    public Menu item(MenuItem item) { return addItem(item); }
    public Menu item(String title, Runnable action) { return addItem(new MenuItem(title, action)); }
    public Menu itemIf(boolean condition, String title, Runnable action) {
        return condition ? item(title, action) : this;
    }
    public List<MenuItem> getItems() { return List.copyOf(items); }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = Objects.requireNonNull(title); }
    public static Menu of(String title) { return new Menu(title); }
}

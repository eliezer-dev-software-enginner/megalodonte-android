package megalodonte.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Named group of menu commands, matching the desktop fluent API. */
public final class Menu {
    private String title;
    private final List<MenuItem> items = new ArrayList<>();
    private final Runnable action;

    public Menu(String title) { this(title, null); }
    public Menu(String title, Runnable onClick) {
        this.title = Objects.requireNonNull(title);
        this.action = onClick;
    }
    public Menu addItem(MenuItem item) {
        if (action != null) throw new IllegalStateException("An action Menu cannot contain items");
        items.add(Objects.requireNonNull(item));
        return this;
    }
    public Menu item(MenuItem item) { return addItem(item); }
    public Menu item(String title, Runnable action) { return addItem(new MenuItem(title, action)); }
    public Menu itemIf(boolean condition, String title, Runnable action) {
        return condition ? item(title, action) : this;
    }
    public List<MenuItem> getItems() { return List.copyOf(items); }
    public boolean hasAction() { return action != null; }
    public void run() {
        if (action == null) throw new IllegalStateException("This Menu is a group and has no direct action");
        action.run();
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = Objects.requireNonNull(title); }
    public static Menu of(String title) { return new Menu(title); }
    public static Menu of(String title, Runnable action) { return new Menu(title, action); }
}

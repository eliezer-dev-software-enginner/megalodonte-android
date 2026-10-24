package megalodonte.components;

import java.util.Objects;

/** Platform-independent command in a {@link Menu}. */
public final class MenuItem {
    private String title;
    private final Runnable action;
    private boolean showAsAction;

    public MenuItem(String title) { this(title, () -> {}); }
    public MenuItem(String title, Runnable action) {
        this.title = Objects.requireNonNull(title);
        this.action = Objects.requireNonNull(action);
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = Objects.requireNonNull(title); }
    public void run() { action.run(); }
    /** Prefer showing this command on the Android Toolbar when space allows. */
    public MenuItem showAsAction() { showAsAction = true; return this; }
    public boolean isShowAsAction() { return showAsAction; }
    public static MenuItem of(String title) { return new MenuItem(title); }
    public static MenuItem of(String title, Runnable action) { return new MenuItem(title, action); }
}

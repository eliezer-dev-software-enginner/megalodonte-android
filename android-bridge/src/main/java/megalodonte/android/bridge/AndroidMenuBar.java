package megalodonte.android.bridge;

import android.content.Context;
import android.graphics.Color;
import android.view.SubMenu;
import android.view.ViewGroup;
import android.widget.Toolbar;
import megalodonte.base.theme.ThemeInterface;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Menu;
import megalodonte.components.MenuBar;

/** Renders the shared MenuBar contract with Android's native overflow menu. */
public final class AndroidMenuBar {
    private AndroidMenuBar() { }

    public static Toolbar create(Context context, MenuBar bar) {
        Toolbar toolbar = new Toolbar(context);
        toolbar.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        int id = 1;
        boolean oneGroup = bar.getMenus().size() == 1;
        for (Menu group : bar.getMenus()) {
            if (group.hasAction()) {
                android.view.MenuItem action = toolbar.getMenu().add(
                        0, id++, android.view.Menu.NONE, group.getTitle());
                action.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
                action.setOnMenuItemClickListener(clicked -> { group.run(); return true; });
                continue;
            }
            android.view.Menu destination;
            if (oneGroup) destination = toolbar.getMenu();
            else {
                SubMenu submenu = toolbar.getMenu().addSubMenu(group.getTitle());
                submenu.getItem().setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
                destination = submenu;
            }
            for (megalodonte.components.MenuItem command : group.getItems()) {
                android.view.MenuItem item = destination.add(0, id++, android.view.Menu.NONE, command.getTitle());
                item.setShowAsAction(oneGroup && command.isShowAsAction()
                        ? android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM | android.view.MenuItem.SHOW_AS_ACTION_WITH_TEXT
                        : android.view.MenuItem.SHOW_AS_ACTION_NEVER);
                item.setOnMenuItemClickListener(clicked -> { command.run(); return true; });
            }
        }
        applyStyle(toolbar, bar, ThemeManager.theme());
        return toolbar;
    }

    /** Compatibility adapter for Activities that provide their title separately. */
    public static Toolbar create(Context context, MenuBar bar, String title, String subtitle) {
        Toolbar toolbar = create(context, bar);
        toolbar.setTitle(title);
        toolbar.setSubtitle(subtitle);
        return toolbar;
    }

    public static void applyStyle(Toolbar toolbar, MenuBar bar, ThemeInterface theme) {
        toolbar.setTitle(bar.getTitle());
        toolbar.setSubtitle(bar.getSubtitle());
        toolbar.setBackgroundColor(color(bar.getBackgroundColor() == null
                ? theme.colors().surface() : bar.getBackgroundColor()));
        int foreground = color(bar.getTextColor() == null
                ? theme.colors().textPrimary() : bar.getTextColor());
        toolbar.setTitleTextColor(foreground);
        toolbar.setSubtitleTextColor(foreground);
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().setTint(foreground);
    }

    private static int color(String value) {
        return "transparent".equals(value) ? Color.TRANSPARENT : Color.parseColor(value);
    }
}

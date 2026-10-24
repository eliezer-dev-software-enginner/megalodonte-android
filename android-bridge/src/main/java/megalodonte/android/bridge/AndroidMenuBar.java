package megalodonte.android.bridge;

import android.content.Context;
import android.view.SubMenu;
import android.widget.Toolbar;
import megalodonte.components.Menu;
import megalodonte.components.MenuBar;

/** Renders the shared MenuBar contract with Android's native overflow menu. */
public final class AndroidMenuBar {
    private AndroidMenuBar() { }

    public static Toolbar create(Context context, MenuBar bar, String title, String subtitle) {
        Toolbar toolbar = new Toolbar(context);
        toolbar.setTitle(title);
        toolbar.setSubtitle(subtitle);
        int id = 1;
        boolean oneGroup = bar.getMenus().size() == 1;
        for (Menu group : bar.getMenus()) {
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
        return toolbar;
    }
}

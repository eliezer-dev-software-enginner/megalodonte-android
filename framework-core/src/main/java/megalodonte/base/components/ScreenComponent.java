package megalodonte.base.components;

import java.util.Map;

public interface ScreenComponent extends megalodonte.contracts.ScreenLifecycle {
    Component render();
    default void onMount() { }
    default void onDestroy() { }
    /** Optional Android lifecycle hooks; plain screens only implement render(). */
    default Map<String, String> saveState() { return Map.of(); }
    default void restoreState(Map<String, String> state) { }
}

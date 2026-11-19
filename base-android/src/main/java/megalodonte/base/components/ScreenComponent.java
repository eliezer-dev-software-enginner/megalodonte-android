package megalodonte.base.components;

import java.util.Map;

public interface ScreenComponent extends megalodonte.contracts.ScreenComponentContract<Component> {

    /**
     * Builds a fresh component tree for this screen. The runtime calls it once
     * per mount, before {@link #onMount()} and before subscribing to the states
     * used by the tree. Keep domain side effects out of this method; change
     * state from callbacks or from {@link #onMount()}.
     *
     * @return the root component of the tree
     */
    Component render();

    /**
     * Called once, immediately after {@link #render()} returned and the
     * platform mounted the tree. Consequently, {@code render()} runs before
     * this hook and is not called again when state changes.
     * <p>
     * The runtime is already subscribed to the rendered states when this method
     * runs, so state changes made here reach the mounted UI reactively. For
     * example, a {@code ListState} populated in {@code onMount()} updates a
     * reactive {@code Column.items(...)} without a new render.
     * </p>
     */
    default void onMount() { }

    /** Called when the screen is replaced or the host is destroyed. */
    default void onDestroy() { }

    /**
     * Returns the values that Android should retain when recreating this screen.
     * Keys and values must be non-null because the host stores them in a Bundle.
     */
    default Map<String, String> saveState() { return Map.of(); }

    /**
     * Restores state immediately before the first {@link #render()} call.
     * The map itself is never null, but it is empty on a fresh launch and may
     * omit any optional key. Consequently, {@code state.get(key)} may return
     * null; use {@code getOrDefault}, {@code containsKey}, or another explicit
     * fallback instead of assuming that a saved value exists.
     *
     * @param state non-null, possibly empty values restored by the Android host
     */
    default void restoreState(Map<String, String> state) { }
}

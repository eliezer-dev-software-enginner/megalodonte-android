package megalodonte.components.inputs;

/** Handles an IME action and optionally invokes its native default behavior. */
@FunctionalInterface
public interface KeyboardActionHandler {
    void onKeyboardAction(Runnable performDefaultAction);
}

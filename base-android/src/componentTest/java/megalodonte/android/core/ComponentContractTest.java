package megalodonte.android.core;

import megalodonte.base.components.Component;
import megalodonte.base.components.Ref;
import megalodonte.components.Button;
import megalodonte.components.Text;
import megalodonte.components.Switch;
import megalodonte.components.inputs.Input;
import megalodonte.props.InputProps;

/** Component-level contracts; runs without Android or a test framework. */
public final class ComponentContractTest {

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Ref<Button> buttonRef = new Ref<>();
        require(buttonRef instanceof megalodonte.contracts.RefContract<?>, "Ref must implement the shared contract");
        require(buttonRef.current() == null, "A Ref starts empty");

        Button button = new Button("Salvar");
        require(button instanceof megalodonte.contracts.ComponentContract<?>, "Component must implement the shared contract");
        Component returned = button.ref(buttonRef);
        require(returned == button, "ref must return the same component for chaining");
        require(buttonRef.current() == button, "ref must expose the bound component");

        Text seed = new Text("Inicial");
        Ref<Text> seeded = new Ref<>(seed);
        require(seeded.current() == seed, "A Ref can start with a component");

        Text replacement = new Text("Substituto");
        seeded.setCurrent(replacement);
        require(seeded.current() == replacement, "setCurrent must replace the component");

        InputProps inputProps = new InputProps()
                .label("Username")
                .outlined()
                .imeAction(InputProps.ImeAction.NEXT);
        require(inputProps.getVariant() == InputProps.Variant.OUTLINED
                        && inputProps.getImeAction() == InputProps.ImeAction.NEXT
                        && "Username".equals(inputProps.getLabel()),
                "InputProps must expose platform-neutral Material and IME options");

        int[] keyboardCalls = {0};
        int[] defaultCalls = {0};
        Input input = new Input("").onKeyboardAction(performDefaultAction -> {
            keyboardCalls[0]++;
            performDefaultAction.run();
        });
        input.dispatchKeyboardAction(() -> defaultCalls[0]++);
        require(keyboardCalls[0] == 1 && defaultCalls[0] == 1,
                "KeyboardActionHandler must choose when to invoke the native default action");

        Input inputWithoutHandler = new Input("");
        inputWithoutHandler.dispatchKeyboardAction(() -> defaultCalls[0]++);
        require(defaultCalls[0] == 2,
                "Input must preserve the native IME behavior when no custom handler is installed");

        var enabled = megalodonte.base.state.State.of(false);
        Switch toggle = new Switch("Notifications", enabled);
        require("Notifications".equals(toggle.accessibleLabel()) && toggle.state() == enabled,
                "Switch must preserve its accessible label and boolean state");
        try {
            new Switch(" ", enabled);
            throw new AssertionError("Switch must reject a blank accessible label");
        } catch (IllegalArgumentException expected) { }

        System.out.println("Component contract tests passed");
    }
}

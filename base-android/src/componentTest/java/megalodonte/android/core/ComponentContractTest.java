package megalodonte.android.core;

import megalodonte.base.components.Component;
import megalodonte.base.components.Ref;
import megalodonte.components.Button;
import megalodonte.components.Text;

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

        System.out.println("Component contract tests passed");
    }
}

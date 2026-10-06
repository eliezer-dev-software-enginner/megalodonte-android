package my_app;

import java.util.Map;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.components.Button;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Container;
import megalodonte.props.ContainerProps;
import megalodonte.props.TextProps;

public class WelcomeScreen implements ScreenComponent {
    private final State<Integer> clicks = State.of(0);

    @Override public Component render() {
        return new Container(new ContainerProps().paddingAll(20).spacingOf(16)).children(
            new Text("Hello World", new TextProps().fontSize(50)),
            new Text(clicks.map(value -> "Cliques: " + value)),
            new Button("Clique aqui").onClick(() -> clicks.set(clicks.get() + 1))
        );
    }

    // Optional lifecycle hooks preserve the counter when Android recreates the Activity.
    @Override public Map<String, String> saveState() { return Map.of("clicks", clicks.get().toString()); }
    @Override public void restoreState(Map<String, String> saved) {
        clicks.set(Integer.parseInt(saved.getOrDefault("clicks", "0")));
    }
}

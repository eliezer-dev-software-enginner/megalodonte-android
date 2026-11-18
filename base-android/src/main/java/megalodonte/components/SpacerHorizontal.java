package megalodonte.components;
import megalodonte.base.components.Component;
import megalodonte.base.state.ReadableState;
import megalodonte.base.state.State;
public final class SpacerHorizontal extends Component {
    public final ReadableState<Integer> size;
    public boolean flexible;
    public SpacerHorizontal() { this(0); flexible = true; }
    public SpacerHorizontal(int size) { this(State.of(size)); }
    public SpacerHorizontal(ReadableState<Integer> size) { this.size = java.util.Objects.requireNonNull(size); }
    public SpacerHorizontal fill() { flexible = true; return this; }
}

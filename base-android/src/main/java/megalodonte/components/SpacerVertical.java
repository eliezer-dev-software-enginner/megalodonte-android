package megalodonte.components;
import megalodonte.base.components.Component;
import megalodonte.base.state.ReadableState;
import megalodonte.base.state.State;
public final class SpacerVertical extends Component {
    public final ReadableState<Integer> size;
    public boolean flexible;
    public SpacerVertical() { this(0); flexible = true; }
    public SpacerVertical(int size) { this(State.of(size)); }
    public SpacerVertical(ReadableState<Integer> size) { this.size = java.util.Objects.requireNonNull(size); }
    public SpacerVertical fill() { flexible = true; return this; }
}

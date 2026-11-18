package megalodonte.v2;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import megalodonte.base.components.Component;
import megalodonte.base.state.ReadableState;
import megalodonte.base.state.State;
public final class Show extends Component implements megalodonte.contracts.ConditionalContent<Component> {
    public final ReadableState<Boolean> condition;
    private final List<Component> children;
    private final Component trueContent;
    private final Component falseContent;
    private Show(ReadableState<Boolean> condition, Component yes, Component no) {
        this.condition = Objects.requireNonNull(condition);
        this.trueContent = Objects.requireNonNull(yes);
        this.falseContent = no;
        children = no == null ? List.of(yes) : List.of(yes, no);
    }
    @Override public ReadableState<Boolean> condition() { return condition; }
    @Override public Component trueContent() { return trueContent; }
    @Override public java.util.Optional<Component> falseContent() { return java.util.Optional.ofNullable(falseContent); }
    public static Show when(ReadableState<Boolean> condition, Supplier<Component> yes) { return new Show(condition, yes.get(), null); }
    public static Show when(ReadableState<Boolean> condition, Supplier<Component> yes, Supplier<Component> no) { return new Show(condition, yes.get(), no.get()); }
    public static Show when(boolean condition, Supplier<Component> yes) {
        return new Show(State.of(condition), condition ? yes.get() : new megalodonte.components.layout_components.Container(), null);
    }
    public static Show when(boolean condition, Supplier<Component> yes, Supplier<Component> no) {
        return new Show(State.of(true), condition ? yes.get() : no.get(), null);
    }
    @Override public List<Component> children() { return children; }
}

package megalodonte.components;

import java.util.Objects;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;

/** Native Android image loaded from a content URI, file URI or asset path. */
public final class Image extends Component {
    public final State<String> source;
    public final String description;
    public final int width;
    public final int height;

    public Image(State<String> source, String description, int width, int height) {
        this.source = Objects.requireNonNull(source);
        this.description = Objects.requireNonNull(description);
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Image dimensions must be positive");
        this.width = width;
        this.height = height;
    }
}

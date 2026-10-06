package my_app;

import megalodonte.android.bridge.MegalodonteActivity;

/** Android entry point. UI and application setup stay in WelcomeScreen and Main. */
public final class MainActivity extends MegalodonteActivity {
    @Override protected void main() { Main.main(); }
}

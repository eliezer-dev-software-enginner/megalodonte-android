# Android navigation

`AndroidRouter` provides named routes and a back stack for Megalodonte Android
applications. It uses `Context.useView` to mount a fresh `ScreenComponent` at
each destination. Both Java-native and JSONC screens can be registered.

```java
MegalodonteApp.run(context -> {
    router = new AndroidRouter(context)
            .route("/list", () -> new ListScreen(router))
            .route("/editor", () -> new EditorScreen(router));
    router.start("/list");
});
```

Call `router.navigate("/editor")` from a button action. `router.back()` returns
`true` when it restores a previous route and `false` at the entry route. An
Android Activity can delegate its Back callback to it, then call the platform
default when it returns `false`.

For JSONC, register a supplier that loads each view asset into a separate
`AndroidJsoncScreen`. Its `onAction` handler can navigate through the same
router. The view stays declarative; the Java host owns routing and data.

See `applications/notas-rapidas` for JSONC routes and
`applications/habitos-diarios` for native routes in `megalodonte-world`.

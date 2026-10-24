# API da Aplicação Cliente

## Main — mesmo formato do app1-welcome

```java
package my_app;

import megalodonte.application.MegalodonteApp;
import megalodonte.base.theme.ThemeManager;
import megalodonte.theme.DefaultTheme;

public class Main {
    public static void main() {
        ThemeManager.setTheme(new DefaultTheme());
        MegalodonteApp.run(context -> context.useView(new WelcomeScreen()));
    }
}
```

## Tela — componentes e props clássicos

```java
package my_app;

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
}
```

O `Text` recebe o estado observável como no desktop. O framework assina o estado,
gera os IDs e envia as atualizações à view nativa. A aplicação só descreve a tela
e altera seu estado no callback.

## Ponto de entrada Android

```java
package my_app;

import megalodonte.android.bridge.MegalodonteActivity;

public final class MainActivity extends MegalodonteActivity {
    @Override protected void main() { Main.main(); }
}
```

Declare `my_app.MainActivity` como launcher no manifest. Essa pequena conexão é
necessária porque Android inicia Activities. O host do framework cuida da bridge,
insets, montagem e descarte. `Main.main()` deve ser chamado por esse host.

## Ciclo de vida opcional

`onMount()` e `onDestroy()` podem ser sobrescritos como no desktop.
Para preservar dados na recriação Android, uma tela também pode implementar:

```java
@Override public Map<String, String> saveState() {
    return Map.of("clicks", clicks.get().toString());
}
@Override public void restoreState(Map<String, String> saved) {
    clicks.set(Integer.parseInt(saved.getOrDefault("clicks", "0")));
}
```

O host salva o mapa em Bundle e o Context restaura antes de chamar `render()`.
Esses hooks são extensões Android opcionais; uma tela estática só precisa de render.

## Escopo suportado

| Recurso clássico | Implementado no Android |
| --- | --- |
| `Component` / `ScreenComponent` | Árvore neutra, render, onMount/onDestroy |
| `Container` | Construtor vazio/com props e children |
| `ContainerProps` | paddingAll, spacingOf (dp) |
| `Text` | String ou ReadableState, com/sem props |
| `TextProps` | fontSize (sp) |
| `Button` | Label, ButtonProps opcional, onClick |
| `ButtonProps` | fontSize (sp) |
| `State` / `ReadableState` | Fontes originais, incluindo map |
| `ThemeManager` / `DefaultTheme` | Core/tema reais, com todos os tokens; dependência Gradle do cliente |
| `MegalodonteApp.run` / `context.useView` | Inicialização e montagem/troca de tela |

Os nomes/imports preservam a experiência clássica nesse subconjunto. As classes
Android substituem as implementações JavaFX e não devem ser combinadas com os
jars desktop. APIs específicas de JavaFX, o contrato completo de temas, outros
props/componentes e roteamento ainda não foram portados.

Desde 2026-10-06, consulte [COMPATIBILITY.md](COMPATIBILITY.md) para layouts,
inputs, checkbox, progresso, Show e reatividade portátil. O tema agora é um
artefato real: `implementation("megalodonte:megalodonte-theme:1.0.0-beta")`.
`context.backendContract().supports(Capability.DESKTOP_WINDOWS)` permite
distinguir capacidades. Use isso junto da matriz: não significa que todos os
overloads legados de uma categoria foram portados.

## MenuBar nativa no Android

`megalodonte.components.MenuBar`, `Menu` e `MenuItem` mantêm a API fluida de menus e ações do desktop (`MenuBar.of().menu(new Menu("Projeto").item("Salvar", action))`). No Android, `AndroidNativeBridge` renderiza o componente com uma `android.widget.Toolbar`; os comandos abrem no menu de três pontos. Um único grupo é apresentado diretamente, enquanto vários grupos aparecem como submenus. `MenuItem.showAsAction()` solicita exibição na barra quando houver espaço; os demais itens ficam no overflow. O adaptador `AndroidMenuBar.create(context, bar, title, subtitle)` permite usar o mesmo contrato também em uma `Activity` Android tradicional, como no editor de vídeo. As ações são `Runnable` executadas na thread da UI. Recursos exclusivos da barra JavaFX, como componentes arbitrários em `trailing` e estilos de `bgColor`, não fazem parte deste contrato nativo.

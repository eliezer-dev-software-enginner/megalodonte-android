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

## Referências de componentes

`Ref<T>` guarda a instância de um componente criado em `render()`, com o mesmo
FQN e assinatura do desktop:

```java
import megalodonte.base.components.Component;
import megalodonte.base.components.Ref;
import megalodonte.components.Button;

private final Ref<Button> botaoSalvar = new Ref<>();

@Override public Component render() {
    return new Button("Salvar").ref(botaoSalvar);
}
```

`botaoSalvar.current()` devolve o componente assim que a árvore é composta, antes
da montagem. No Android a referência expõe apenas o componente declarativo
(label, props, estado associado); a view nativa continua encapsulada na bridge.
Use `State`/`ListState` para mudanças de dados e `Ref` somente quando uma operação
imperativa realmente não puder ser expressa por estado.

## Listas reativas

`Column.items` e `Row.items` aceitam um `ReadableState<List<T>>` — por exemplo um
`ListState` — e um mapper que devolve um componente por item:

```java
private final ListState<String> nomes = ListState.ofEmpty();

@Override public Component render() {
    return new Column(new ColumnProps().fillWidth()).items(
        nomes,
        nome -> new Button(nome, new ButtonProps().fillWidth())
    );
}

@Override public void onMount() {
    nomes.set(List.of("Ana", "Bruno"));
}
```

Também existe `Column.items(List<T>, mapper)` para uma lista estática já materializada.
No overload reativo, cada mudança do estado recria apenas as views do bloco,
mantendo posição, espaçamento e largura no layout pai. O mapper precisa devolver
uma instância nova por item; reutilizar a mesma instância é recusado. Mutar a
lista no lugar não é observado: publique uma nova lista, como as operações do
`ListState` fazem. Ainda não há reconciliação por identidade nem preservação de
foco/estado das views; itens são recriados a cada mudança.

## Atualizações vindas de threads

O helper é o mesmo do desktop: `megalodonte.base.UI.runOnUi(Runnable)`. No Android
ele encapsula um `Handler` do looper principal — o análogo nativo de
`Platform.runLater`. Se o chamador já estiver na thread principal, a tarefa roda
imediatamente (como `Activity.runOnUiThread`); caso contrário, é enfileirada.

```java
new Thread(() -> {
    String resultado = consultaDemorada();
    UI.runOnUi(() -> status.set(resultado));
}).start();
```

Use-o ao voltar de thread de fundo, executor ou callback assíncrono para alterar
`State`/`ListState` que alimentam a tela. Não é necessário (nem permitido) tocar
em views nativas: a bridge só aceita comandos na thread principal e falha
explicitamente fora dela. Alterações de estado feitas na própria thread principal
não precisam do helper.

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

A ordem no `Context.useView` é `restoreState` → `render()` → montagem/assinaturas →
`onMount()`. Ou seja, `render()` sempre roda antes de `onMount()` e não é chamado
de novo quando o estado muda. Como as assinaturas já estão ativas em `onMount`,
preencher um `State` ou uma `ListState` nesse hook atualiza a UI montada
reativamente, inclusive listas. O javadoc de `render()`/`onMount()` registra essa
ordem em inglês.

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
| `ListState` / `ForEachState` | Artefato portátil; `ListState` funciona como `ReadableState<List<T>>` |
| `Ref<T>` / `Component.ref(Ref<T>)` | Holder do componente declarativo; `ref` atribui durante a composição, antes da montagem |
| `UI.runOnUi` | `Handler` do looper principal; executa na hora se já estiver na main thread |
| `Column` / `Row` | children/c_child, items estático e items reativo (`ReadableState<List<T>>`) |

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

`megalodonte.components.MenuBar`, `Menu` e `MenuItem` mantêm a API fluida de menus e ações do desktop. No Android, `AndroidNativeBridge` renderiza o componente como uma `android.widget.Toolbar` completa e com largura total por padrão: `.title(...)`, `.subtitle(...)`, `.bgColor(...)` e `.textColor(...)` configuram a apresentação nativa, enquanto os comandos abrem no menu de três pontos. Um único grupo é apresentado diretamente, enquanto vários grupos aparecem como submenus. `MenuItem.showAsAction()` solicita exibição na barra quando houver espaço; os demais itens ficam no overflow.

```java
return new MenuBar()
        .title("My App")
        .bgColor("#D32F2F")
        .textColor("#FFFFFF")
        .menu(new Menu("Detalhes").item("Sobre", action));
```

O adaptador `AndroidMenuBar.create(context, bar)` atende telas Megalodonte; o overload legado `create(context, bar, title, subtitle)` continua disponível para Activities Android tradicionais. As ações são `Runnable` executadas na thread da UI. Componentes arbitrários via `trailing(...)` continuam exclusivos da barra Desktop.

`new Menu("Atualizar", action)` representa uma ação direta: no Android ela entra
como item clicável do overflow e executa o callback, sem criar submenu. Esse modo
é uma folha e não pode receber `.item(...)`; use `new Menu("Grupo").item(...)`
quando houver comandos internos.

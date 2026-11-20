# Contexto do Projeto

## Objetivo

Criar o primeiro passo do Megalodonte para Android: texto e botão nativos,
com uma bridge entre a declaração da interface e o renderer da plataforma.

Em 2026-10-06, o usuário confirmou o MVP e pediu análise/refatoração e expansão.
Core/tema comuns já foram extraídos; a migração de toda a UI ainda está em andamento.

## Estrutura

- Java 17 como nível de código/bytecode e JVM do Gradle; Gradle 8.13;
  Android Gradle Plugin 8.12.0.
- `compileSdk`, `targetSdk` e `minSdk` da aplicação: 35 (Android 15).
- `megalodonte-base-android` (diretório `base-android`): API cliente nos pacotes clássicos `megalodonte.application`,
  `megalodonte.base.components`, `megalodonte.components`, `megalodonte.props` e tema.
  `BridgeRuntime` e `NativeBridge` são infraestrutura interna em `megalodonte.android.core`.
- `megalodonte-bridge-android` (diretório `android-bridge`): `AndroidNativeBridge` e `MegalodonteActivity`, com widgets, insets
  e ciclo de vida Android encapsulados no framework.
- `app`: `my_app.Main`, `WelcomeScreen implements ScreenComponent` e `MainActivity`
  que somente liga o ponto de entrada `Main.main()` ao host.
- Estado, tema, escala, lifecycle e matcher vêm do artefato megalodonte-core.
- O cliente declara `megalodonte-theme` real. `megalodonte-base-android`
  compila os fontes neutros originais de
  ComputedState/ListState/ForEachState/ListenerManager diretamente do checkout
  `megalodonte-reactivity`; não há artefato portable intermediário.
- Composite builds ligam core e tema; não há cópia de estado no backend.
- Os módulos delegam `prepareKotlinBuildScriptModel` à raiz para atender à
  solicitação de preparação dos scripts Gradle pelo IntelliJ.

## Comportamento atual

A tela exibe “Hello World”, “Cliques: 0” e um botão “Clique aqui”.
`Main` configura `ThemeManager` e chama `MegalodonteApp.run(context -> context.useView(...))`.
`WelcomeScreen.render()` compõe `Container`, `Text` e `Button` com props e `State.map`.
IDs e assinaturas são automáticos; a tela não importa nem chama a bridge.
O clique altera o `State` e o runtime atualiza o texto. Hooks opcionais de
`saveState`/`restoreState` preservam o contador na recriação da Activity.
O layout respeita barras do sistema, recortes, dp e sp.

Listas reativas são suportadas em `Column.items`/`Row.items` com um
`ReadableState<List<T>>` (por exemplo, `ListState`) e um mapper. O runtime
substitui apenas as views do bloco quando a lista muda. `render()` executa antes
de `onMount()`; como as assinaturas já existem nesse momento, mudanças de estado
em `onMount` (inclusive em uma `ListState`) aparecem na UI sem novo render.

## Limites desta etapa

A bridge é síncrona, em Java e na thread principal. `megalodonte.base.UI.runOnUi`
volta de threads de fundo ao looper principal (`Handler`), análogo ao
`Platform.runLater` do desktop. Não há runtime JavaScript,
serialização, diff da árvore, roteamento ou equivalência completa com o backend JavaFX.
Suportados: Text/Button, Container/Column/Row, Scroll, espaçadores,
Input/PasswordInput/TextAreaInput (incluindo variantes Material e ações IME), Checkbox, Switch, ProgressBar, Show.when, listas

`Show.when` preenche a largura quando usado em uma `Column`, inclusive quando o
ramo contém outra `Column` com inputs ou botões `fillWidth`; quando oculto, não
reserva espaço no layout.
reativas via `ForEach`/`items(ReadableState<List<T>>, ...)` e `Ref<T>` com
`Component.ref(...)`, com recriação das views ao mudar (sem reconciliação por
identidade nem preservação de foco); a
API parcial detalhada em COMPATIBILITY.md. ThemeInterface/tokens são os tipos
reais do core. O renderer reestiliza as views quando ThemeManager muda.
BackendContract v1, ScreenLifecycle e RouteMatcher são comuns. O matcher já é
usado no desktop; Router Android completo ainda não foi implementado.
`Show` implementa o contrato comum `ConditionalContent<Component>` e `ForEach`
implementa `ForEachState<T, Component>`. `Component`, `Ref` e
`ScreenComponent` também implementam contratos neutros compartilhados com o
Desktop; identidade e montagem continuam específicas do renderer Android.
State/map e estados calculados/listas têm assinaturas descartáveis; o runtime
remove seus bindings ao fechar. Os clientes originais 1–4 passam como fixtures.
`MenuBar` já usa `Toolbar` nativa com título, subtítulo, cores e overflow; tabelas,
modais, recursos, router/async e props avançadas continuam pendentes.
Os contratos comuns serão ampliados progressivamente para props, router e
demais bibliotecas; todo artefato específico Android termina em `-android`.
Um emulador existente API 37 foi iniciado sem janela para instrumentação: cliente,
input formatado, checkbox, progresso, Show, tema reativo e descarte passaram.
Android 15/API 35, rotação e layouts avançados ainda precisam de validação específica.

## Documentação da sessão

Ler `AI_RULES.md`, `CONTEXT.md`, `DECISIONS.md`, `TODO.md`, `PROMPT.md` e o README
no início de uma sessão. O status de validação fica em `TODO.md`.

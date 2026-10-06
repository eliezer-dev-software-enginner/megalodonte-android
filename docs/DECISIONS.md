# Decisões Arquiteturais

## 2026-10-06: Preparação Kotlin DSL solicitada pelo IntelliJ

O usuário encontrou `prepareKotlinBuildScriptModel` ausente em `:app` ao testar
no IntelliJ. O erro foi reproduzido no wrapper Gradle 9.2.0; a tarefa existe na
raiz, mas não nos módulos. Registradas tarefas nos três subprojetos que dependem
da preparação real da raiz, preservando seu comportamento e atendendo à
solicitação da IDE. Não foi acrescentado plugin Kotlin ao código Java.

O modelo Kotlin DSL é documentado pelo
[Gradle](https://docs.gradle.org/9.2.0/javadoc/org/gradle/tooling/model/kotlin/dsl/KotlinDslScriptsModel.html).
A verificação por CLI cobre a seleção das tarefas; a recarga na interface do
IntelliJ ainda precisa ser confirmada pelo usuário.

## 2026-10-06: Artefatos comuns e contrato v1

O usuário validou o MVP e pediu expansão com contratos/reuso. Extraídos para
megalodonte-core: estado, ThemeInterface/tokens/registro, Fonts, escala e hooks.
JavaFxPlatform fornece detecção/fontes; Android mantém conversão dp/sp no renderer.
O tema depende somente do core, gera bytecode Java 17 e é declarado pelo cliente
Android. O backend deixa de duplicar tipos de estado/tema.

BackendContract v1 e ScreenLifecycle são comuns. RouteTable desktop usa o matcher
genérico: rota literal precede parâmetros, empate ambíguo gera erro.
observe/unsubscribe/Subscription e map lazy permitem descarte real. ComputedState
e ForEachState têm close; ListenerManager.disposeAll limpa registros associados.
Reactivity-portable prepara os mesmos fontes neutros originais, excluindo Show/JavaFX.

Portados componentes adicionais e testados clientes originais 1–4. Inventário,
análise e lacunas em LIBRARY_ANALYSIS.md/COMPATIBILITY.md. Paridade total não concluída.
Recompilar consumidores desktop: o dono JPMS dos tipos mudou e applyFontFamily
recebe Object em vez de Scene. Versão beta mantida para desenvolvimento local;
versionar essa migração antes de distribuição externa.

## 2026-10-05: API cliente clássica — substitui a API inicial

**Contexto:** o cliente inicial precisava conhecer IDs, `NativeBridge`, runtime e
assinaturas manuais. O usuário pediu a escrita de UI do `megalodonte-app1-welcome`.

**Decisão:** expor implementações Android alternativas nos mesmos pacotes clássicos:
`ScreenComponent.render()`, `Component`, `Container(props).children(...)`, `Text`,
`Button.onClick`, props, `ThemeManager`/`DefaultTheme` e `MegalodonteApp.run` com `context.useView`.
IDs são gerados dentro dos componentes. `Text(ReadableState<String>, TextProps)`
é ligado à bridge pelo `BridgeRuntime`, sem código de binding na tela.

`MegalodonteActivity` encapsula host/insets/ciclo de vida. O cliente tem apenas
uma subclasse mínima que chama `Main.main()`. `onMount`/`onDestroy` seguem o padrão
desktop; `saveState`/`restoreState` são extensões opcionais para recriação Android.
Bindings mantêm referências fracas ao runtime porque o State original ainda não
tem unsubscribe. Isso evita reter a Activity, mas não remove entradas de listeners.

**Validação:** os dois fontes originais `Main.java` e `WelcomeScreen.java` do
app1-welcome são preparados como fixtures sem alterações, compilados e executados
nos testes do core. Testes também cobrem binding, cliques, descarte, substituição
de tela e restauração. Build do APK e lint passaram.

**Limite:** compatibilidade de escrita para o subconjunto implementado, não
compatibilidade completa/binária do framework desktop. O contrato de tema Android
é mínimo; jars desktop com os mesmos nomes de classes não podem coexistir no APK.
As decisões iniciais abaixo registram a primeira versão, substituída por esta.

## 2026-10-05: Primeiro backend Android

**Contexto:** o `Component` desktop encapsula `javafx.scene.Node`. Os componentes
desktop e seus props não podem ser enviados diretamente ao renderer Android.

**Decisão:** criar descrições neutras no pacote `megalodonte.android.core`, mantendo
a forma declarativa de `Text`, `Button.onClick` e composição por `Column`.
O contrato `NativeBridge` isola criação da árvore, atualização de texto e eventos.
O renderer converte a árvore em widgets Android; o código da tela usa somente o core.

**Consequência:** esta é uma nova camada experimental do Megalodonte, não uma
portabilidade automática das telas JavaFX existentes. A extração de um contrato
comum aos dois backends fica para uma etapa posterior.

## 2026-10-05: Reutilização do estado original

**Decisão:** compilar apenas `State.java` e `ReadableState.java` dos fontes do
`megalodonte-base`, junto com o core, sem copiar ou alterar a biblioteca desktop.
Uma tarefa `prepareSharedState` prepara somente esses dois arquivos em `build/generated`,
evitando que o javac descubra implicitamente o `module-info.java` desktop.

**Consequência:** o checkout irmão do ecossistema é requisito de build. Os caminhos
e a limitação estão documentados em `GETTING_STARTED.md`. A separação do estado
em uma biblioteca publicável e independente de plataforma é uma pendência.

## 2026-10-05: Target, thread e ciclo de vida

**Decisão:** API 35 como compile/target/min SDK desta etapa; Java 17; AGP 9.0.1;
Gradle 9.2.0. Uma bridge síncrona na thread principal, com uma tela por runtime.
IDs únicos roteiam eventos e comandos. O fechamento é idempotente e remove
listeners/views; eventos ou atualizações posteriores ao fechamento são ignorados.

**Consequência:** Android anteriores ao 15 não instalam a demonstração.
Ainda não existe executor separado, fila de comandos ou protocolo serializado.
O estado pertence à tela e deixa de ser usado após seu fechamento; o `State`
atual não oferece unsubscribe. Não reutilizar um State de longa duração entre telas.

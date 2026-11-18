# Decisões Arquiteturais
## 2026-11-15: `Ref<T>` no Android

**Contexto:** telas portadas do desktop usam `Ref<Button>` etc. para guardar um
componente criado em `render()`; no Android o símbolo não existia
(`Cannot resolve symbol 'Ref'`). O `Component` Android também não tinha o helper
`ref(...)` que o desktop expõe em `megalodonte.base.components`.

**Decisão:** portar `Ref<T>` e `Component.ref(Ref<T>)` com o mesmo FQN e
assinatura para `framework-core`, que é Java puro e continua testável por JVM.
`ref` atribui a instância durante a composição de `render()`, antes da montagem,
como no desktop: `new Button("Salvar").ref(botaoSalvar)`.

A referência expõe somente o componente declarativo — label, props e estado
associado. A view nativa permanece encapsulada na bridge, então `Ref` não é um
caminho para `android.view.View`, para mutar filhos nem para substituir
`State`/`ListState`. A regra do projeto continua: estado para dados, `Ref` apenas
para uma operação imperativa inevitável.

**Validação:** `:megalodonte-base-android:componentTest` cobre Ref vazio, binding com
retorno do mesmo componente, valor inicial e `setCurrent`. O
`learn-android-app` compila com `Ref<Button>`.

## 2026-11-15: `UI.runOnUi` no Android

**Contexto:** o desktop expõe `megalodonte.base.UI.runOnUi(Runnable)` para voltar
de threads de fundo à thread JavaFX (`Platform.runLater`). O Android não tinha o
equivalente, então telas que atualizam `State` após trabalho assíncrono não tinham
como fazer isso sem tocar no `Activity.runOnUiThread` ou repetir `Handler` na mão.

**Decisão:** criar `megalodonte.base.UI.runOnUi(Runnable)` no módulo
`android-bridge`, com o mesmo FQN e assinatura do desktop, implementado com um
`Handler` do `Looper.getMainLooper()` — o recurso nativo análogo ao
`Platform.runLater`. Não foi criada uma abstração de dispatcher: o helper é
pequeno e usa a API Android direta, como `Redirect`/`Async` fazem no desktop.

Se o chamador já está na thread principal, a tarefa executa imediatamente, como
`Activity.runOnUiThread`, evitando recriações extras quando o código já roda em
callbacks de UI. Fora dela, a tarefa é enfileirada no looper principal. O helper
só agenda; a alteração de estado continua reagindo pelo `BridgeRuntime`, que exige
a thread principal e falha explicitamente fora dela.

O arquivo fica em `android-bridge` e não em `framework-core` porque este último é
um módulo Java puro (sem `android.jar`), mantido testável por JVM. O app depende
de `android-bridge` (que expõe `framework-core` via `api`), então o cliente vê o
mesmo pacote.

**Validação:** teste instrumentado em emulador API 35 cobre execução imediata na
main thread, envio de thread de fundo para a main thread e atualização de um
`TextView` a partir de um `State` alterado por `UI.runOnUi`. O app
`learn-android-app` compila com o helper.

**Limite:** ainda não há `Async`/`Scope`/`ErrorReporter` no Android; só o retorno
explícito à thread principal foi portado.

## 2026-11-15: Listas reativas no Android

**Contexto:** uma tela do cliente escreveu `Column.items(ListState<String>, mapper)`
e recebeu `Required type: List<T> / Provided: ListState<String>`.
`ListState` implementa `ReadableState<List<E>>`, não `List`, então o overload
estático existente não podia aceitá-lo. O pedido era fazer esse código funcionar
sem alterar a tela e documentar que `onMount` roda após `render`.

**Decisão:** `ForEach<T>` (`megalodonte.v2`) representa um bloco de lista que
observa um `ReadableState<List<T>>` e mapeia cada item para um componente.
`Column.items`/`Row.items` ganharam um overload que anexa esse bloco; o overload
de `List<T>` estática continua existindo. A capacidade `LIST_RENDERING` passa a ser
exigida na indexação e declarada pela bridge Android.

O `BridgeRuntime` assina o estado da lista uma vez por bloco. Ao mudar, chama
`NativeBridge.setChildren(id, novosComponentes)`, descarta as assinaturas da
subárvore anterior, indexa e assina a nova subárvore. Cada item deve ser uma
instância nova; o runtime continua recusando a mesma instância em dois lugares.

No `AndroidNativeBridge` o bloco é renderizado inline no `LinearLayout` pai, sem
container aninhado, para preservar espaçamento e `fillWidth`. Uma âncora `GONE`
marca a posição do bloco; `setChildren` remove apenas suas views, recria na mesma
posição e reaplica o espaçamento do pai. Listeners e limpezas por componente são
registrados por ID e executados no descarte, portanto itens removidos param de
disparar eventos.

**Validação:** `:megalodonte-base-android:listRenderingTest` cobre bind/unbind, clique em
item dinâmico, remoção, limpeza, binding de texto dentro de itens e ausência da
capacidade. A instrumentação API 35 monta a lista, adiciona, remove, limpa,
clica em item e verifica binding de texto. O `learn-android-app` compila com a
tela original sem alterações.

**Limite:** não há reconciliação por identidade nem preservação de foco/estado
das views; toda mudança recria as views do bloco. Mutar a lista no lugar não é
observável: `ListState` precisa publicar uma nova lista, como já faz em suas
operações. O javadoc de `render()`/`onMount()` passou a registrar, em inglês, a
ordem render → montagem → `onMount` e que assinaturas já existem nesse ponto.

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
## 2026-11-18: Contratos comuns e nomes explícitos por plataforma

**Decisão:** comportamento independente de renderer pertence a interfaces do
`megalodonte-core`. As implementações desktop preservam a base existente e passam
a declarar esses contratos; Android adapta seu lifecycle e seus widgets nativos.
`Show` agora implementa `ConditionalContent<Component>` nos dois hosts, e o bloco
Android `ForEach` implementa o contrato comum `ForEachState<T, Component>`.

Essa direção se estende a componentes, props, router e futuras bibliotecas. Métodos
estáticos de fábrica continuam em cada plataforma porque Java não os sobrescreve.
Artefatos neutros permanecem sem sufixo; módulos específicos usam `-desktop` ou
`-android`. Os módulos internos atuais passam a se chamar
`megalodonte-base-android` e `megalodonte-bridge-android`; os diretórios históricos
permanecem para não quebrar checkouts e ferramentas que localizam o código-fonte.

`Component`, `Ref` e `ScreenComponent` implementam, nos dois hosts, os contratos
neutros do `megalodonte-core`. Esses contratos cobrem composição, referências e
ciclo de vida sem expor JavaFX ou tipos nativos do Android. Identidade, node/view
e montagem continuam pertencendo a cada implementação de plataforma.

`megalodonte.base.components` contém as abstrações fundamentais, enquanto
`megalodonte.components` contém controles e layouts concretos. São camadas
distintas, não implementações duplicadas. Diretórios de fontes `theme` vazios e
legados dentro de `framework-core` não fazem parte da API Android.

---


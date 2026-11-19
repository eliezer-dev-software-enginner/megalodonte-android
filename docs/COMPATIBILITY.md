# Compatibilidade Atual — Contrato v1

| Família | Android atual | JavaFX / pendências |
| --- | --- | --- |
| State / ReadableState | Mesmo JAR; get/set/subscribe/map/unsubscribe/observe | Mesmo código |
| ThemeInterface / tokens | Mesmo JAR e cinco grupos de tokens | Mesmo código; cores precisam ser aceitas pelo parser de cada plataforma |
| DefaultTheme | Dependência Gradle real megalodonte-theme | Mesmo JAR, sem base/JavaFX transitivo |
| ScaleProvider | Política comum; detector 1, conversão dp/sp no host | Detector de Screen instalado pelo bootstrap |
| ComputedState / ListState / ForEachState / ListenerManager | Artefato portátil, mesmos fontes; assinaturas descartáveis | Originais desktop; Show continua no adapter |
| `Ref<T>` / `Component.ref` | Holder do componente declarativo; `ref` atribui durante a composição, antes da montagem | Mesmo FQN/assinatura; no desktop o componente envolve um Node JavaFX |
| ScreenComponent / Main / Context | render, lifecycle, useView, backendContract, run com callback CloseRequest | Lifecycle/consulta comum; demais métodos de Context não portados |
| Text / Button | Construtores básicos, Text(ReadableState), fontSize, onClick | Peso, cor, família/estados em props e ícones ainda parciais/pendentes |
| Container / Column / Row | children/c_child; Column/Row.items estático e reativo (`ReadableState<List<T>>`); padding/spacing/dimensões/centro/bgColor básicos | ForEachState.items, detalhes de limites/alinhamento e listeners de layout pendentes |
| ForEach / listas reativas | Capacidade `LIST_RENDERING`; recria as views do bloco ao mudar; sem reconciliação nem preservação de foco | ForEachState continua no adapter desktop |
| SpacerVertical / Horizontal | Tamanho fixo/reativo e fill | Implementado com views/weights nativos |
| Input | State/String, placeholder, editable, disable, fontSize, onChange/Initialize/Enter | Adornos Node, foco/caret e todas as props legadas ainda pendentes |
| Input / PasswordInput / TextAreaInput | State+InputProps; plain/filled/outlined, label flutuante, ações IME, mascaramento/múltiplas linhas nativos | Botão reveal e tipos de teclado avançados pendentes |
| Checkbox | Label, State<Boolean>, fontSize, binding bidirecional | Estilos avançados pendentes |
| ProgressBar | State<Integer>, faixa 0–100 | Controle nativo; estilos adicionais pendentes |
| Scroll | Filho único, scroll vertical | Eixo horizontal e API de ScrollPane não compartilhados |
| Show.when | Condição reativa/boolean, ramo único/ternário, GONE no Android | Transitions/expandWhen pendentes |
| JSONC Android | Renderer próprio para os 21 tipos do schema, incluindo controles de dados, mídia e gráficos; veja [JSONC_ANDROID.md](JSONC_ANDROID.md) | Paridade de propriedades e APIs Java independentes ainda não é completa |
| Route matching | Matcher comum disponível no core | Desktop usa o matcher; Router/Context de navegação Android pendentes |
| Select / DatePicker / Image como API Java | Facades Android nativas implementadas; `Select` usa `State<String>` e opções imutáveis, `DatePicker` usa data ISO e `Image` usa URI ou asset | API distinta da versão JavaFX; demais propriedades e estilos ainda pendentes |
| Icon / CurrencyInput como API Java | Pendente | Portar facades Java independentes e testes de dispositivo |
| Card / Stack / GridFlow / FlowRow / Canva / TextFlow / Line | Pendente | Portar layout/modelo e traduzir comportamento |
| SimpleTable / PaginatedTable / Menu / MenuBar / Modal | Pendente | Extrair modelos; não prometer ObservableList/Stage no mobile |
| UI (thread principal) | `megalodonte.base.UI.runOnUi(Runnable)` com `Handler` do looper principal; executa na hora se já estiver na main thread, senão enfileira | `UI.runOnUi` usa `Platform.runLater` |
| Async / Scope / ErrorReporter | Pendente no Android | Exigem executor e logging injetáveis |
| FontLoader/custom assets | Não declarado como capacidade Android | Serviço JavaFX; fonte de sistema no Android resolve via Typeface |
| Node / Scene / Stage / CSS JavaFX / spawnWindow | Exclusivo JavaFX | Uma capacidade ausente deve falhar explicitamente, nunca ser ignorada |

O contrato não promete equivalência binária de todas as classes legadas. A API
de UI usa facades alternativas nos pacotes clássicos; somente estado, tema e
contratos extraídos têm um tipo único. Fixtures originais 1–4 comprovam a
compatibilidade de escrita exercitada; não cobrem todas as linhas desta matriz.

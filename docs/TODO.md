# TODO

## Importação no IntelliJ — 2026-10-06

- [x] Reproduzir a ausência de `:app:prepareKotlinBuildScriptModel`.
- [x] Delegar a preparação dos três módulos à tarefa real da raiz.
- [x] Validar tarefas dos módulos, `:app:assembleDebug` e `:app:lintDebug` por CLI.
- [ ] Confirmar Reload All Gradle Projects na interface do IntelliJ do usuário.

## Core/contratos e expansão — 2026-10-06

- [x] MVP aceito pelo usuário; bibliotecas auditadas em LIBRARY_ANALYSIS.md.
- [x] Core publicável com estado/tema/escala e contratos comuns.
- [x] Tema Gradle real e reatividade portátil com os mesmos fontes.
- [x] Assinaturas descartáveis, lifecycle, matcher e capacidades versão 1.
- [x] Inputs, checkbox, progresso, layouts/scroll/espaçadores e Show Android.
- [x] Fontes originais dos clientes 1–4 compilados e executados nos testes.
- [x] Módulos JavaFX recompilados; tema/core/portable testados; APK/lint passaram.
- [x] Checks de imports/bytecode e grafo Android sem JavaFX.
- [x] Instrumentação nativa em API 37: cliente, input, checkbox, progresso, Show, tema e descarte.
- [ ] Validar especificamente em API 35, incluindo rotação/layouts/acessibilidade.
- [ ] Unificar contratos de Component/Props/eventos por método.
- [ ] Router Android/Navigator neutro com back stack e restauração.
- [ ] TaskExecutor/UiDispatcher/logging comuns para Async/Scope.
- [x] List rendering reativo (`ForEach`, `items(ReadableState<List<T>>, ...)`) e descarte da subárvore anterior.
- [ ] Reconciliação por identidade e preservação de foco/estado de views em listas reativas.
- [ ] TableModel/seleção/paginação/exportação comuns e renderer Android.
- [ ] Select/DatePicker/Image/Icon/CurrencyInput e fontes/recursos.
- [ ] Menu/Modal/Stack/Grid/Flow/Canva/TextFlow, animações e props avançadas.
- [x] Organizar repositórios próprios e submódulos de core/portable.
- [ ] Versionar a migração antes de distribuição externa.

## Concluído — estrutura inicial, 2026-10-05

- [x] Projeto Gradle com core, bridge e app separados.
- [x] Declaração de texto, botão e coluna sem widgets no core.
- [x] Reutilização do `State` original sem dependência JavaFX.
- [x] Renderer com views nativas e retorno de eventos pelo runtime.
- [x] Contador na tela e restauração pelo estado da Activity.
- [x] Insets Android 15, dp/sp e descarte dos listeners.
- [x] Documentação no padrão `/docs`.

## Validação

- [x] API cliente alinhada ao app1-welcome: `Main`, tela/props, tema e `context.useView`.
- [x] IDs automáticos e binding de `Text(ReadableState)` encapsulado no runtime.
- [x] Activity genérica do framework; a Activity cliente somente chama Main.
- [x] Compilar e executar Main/WelcomeScreen originais do app1-welcome sem alterações.
- [x] Testar cliques, estado derivado, descarte, troca de tela e restauração.

- [x] Executar `:framework-core:check`: testes de contrato passaram em 2026-10-05.
- [x] Contrato `LIST_RENDERING` e teste Java de lista reativa (`:framework-core:listRenderingTest`, 2026-11-15).
- [x] Instrumentação API 35 em 2026-11-15: montar, adicionar, remover, limpar, clique em item dinâmico e binding de texto em lista reativa.
- [x] Gerar APK com `:app:assembleDebug`: `app/build/outputs/apk/debug/app-debug.apk`.
- [x] Executar `:app:lintDebug`: build/check/lint concluídos com sucesso em 2026-10-05.
- [x] Ícone da aplicação e regras de extração de dados definidos no manifest.
- Avisos esperados do lint: API 35 não é o target/compile SDK mais recente;
  mantida por solicitação explícita de mirar Android 15. Nenhum erro de lint.
- [ ] Validar texto, cliques, rotação e acessibilidade em dispositivo Android 15.

## Próximos passos

- [x] Extrair estado publicável (workspace usa includeBuild; artefatos também publicam em Maven).
- [ ] Definir contrato de componentes/props compartilhável com o backend JavaFX.
- [x] Adicionar observe/unsubscribe e remover bindings ao fechar.
- [ ] Adicionar testes instrumentados do renderer e da Activity.
- [ ] Evoluir atualização da árvore, props/temas e componentes além de Text/Button/Container.
- [ ] Avaliar navegação e eventual fila assíncrona após estabilizar o contrato inicial.

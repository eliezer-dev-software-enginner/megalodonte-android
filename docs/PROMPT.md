# Tarefa Inicial

## Solicitação atual — 2026-10-06

Corrigir o erro `Task 'prepareKotlinBuildScriptModel' not found in project ':app'`
ao carregar o projeto no IntelliJ (IDE esclarecida pelo usuário).

## Solicitação de expansão — 2026-10-06

MVP validado pelo usuário. Expandir a partir de megalodonte-libs, analisar
acoplamentos JavaFX e definir contratos/reuso, permitindo declarar o tema no
Gradle do cliente. Registrar resultados e lacunas para completar a paridade.

## Solicitação — 2026-10-05

Criar `megalodonte-world/megalodonte-android` como primeiro passo para desenvolver
aplicativos Android com o framework Megalodonte. Mirar Android 15 e usar uma
arquitetura de bridge inspirada na separação entre framework e UI nativa.
O objetivo inicial é exibir um texto e um botão.

Documentar o projeto no padrão `/docs`, com contexto, decisões, regras e pendências.

## Ajuste solicitado — 2026-10-05

A aplicação cliente deve permitir escrever a UI como no Megalodonte clássico,
usando `megalodonte-app1-welcome` como referência: `Main`, tema, `MegalodonteApp.run`,
`context.useView`, `ScreenComponent.render`, `Container`, `Text` e props.
Bridge, IDs e binding manual devem ficar dentro do framework.

## Critérios de aceite

- Projeto Gradle que pode ser aberto no Android Studio.
- Componentes declarativos separados do renderer Android.
- Texto e botão renderizados por widgets nativos.
- Clique retorna pela bridge e atualiza o texto mediante `State`.
- Configuração API 35 e documentação de build/limites/continuidade.
- Compilar e executar os fontes originais do app1-welcome contra a API Android.

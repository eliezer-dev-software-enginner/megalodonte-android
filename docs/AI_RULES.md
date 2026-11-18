# Regras de Manutenção

- Ler o README e os documentos de contexto, decisões, pendências e tarefa antes de alterar o projeto.
- Manter a lógica de tela independente de widgets Android; o backend pertence ao `android-bridge`.
- Preservar a API declarativa e reutilizar os recursos neutros existentes do Megalodonte.
- Usar os imports/API clássicos nas telas: `ScreenComponent.render`, props e
  `MegalodonteApp.run(context -> context.useView(...))`. IDs, bindings e bridge
  pertencem à infraestrutura; não expô-los como obrigação da aplicação cliente.
- Manter os testes que compilam/executam os clientes originais 1–4 sem alterações.
- Não trazer JavaFX/desktop ao core compartilhado ou ao APK. O module-info do
  core exporta somente pacotes neutros; o Android não usa JPMS em execução.
- Usar dependências reais para core/tema/reatividade; não duplicar tipos no backend.
- Não anunciar paridade completa a partir de nomes iguais. Atualizar inventário,
  matriz de métodos suportados e testes de capacidades/fixtures.
- Fazer chamadas da bridge e alterações do estado usado pela UI na thread principal.
- Garantir IDs únicos, descarte de listeners/views e respeito aos insets do Android 15.
- Validar mudanças do contrato com `:megalodonte-base-android:check`; validar Android com build e lint.
- Registrar decisões em `DECISIONS.md` e manter `CONTEXT.md` e `TODO.md` coerentes com o resultado real.
- Não registrar testes em emulador ou build como concluídos sem executá-los.

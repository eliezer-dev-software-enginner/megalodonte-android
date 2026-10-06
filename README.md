# Megalodonte Android

Backend experimental Android do framework Megalodonte: componentes declarativos,
bridge e widgets nativos. O MVP foi validado pelo usuário. Estado, tema e contratos
agora vêm do megalodonte-core compartilhado; o cliente declara megalodonte-theme
como dependência Gradle real. A paridade completa de todas as APIs ainda está pendente.

A aplicação cliente usa o padrão clássico do Megalodonte, incluindo seus imports:

```java
public class WelcomeScreen implements ScreenComponent {
    private final State<Integer> clicks = State.of(0);

    @Override public Component render() {
        return new Container(new ContainerProps().paddingAll(20)).children(
            new Text("Hello World", new TextProps().fontSize(50)),
            new Text(clicks.map(value -> "Cliques: " + value)),
            new Button("Clique aqui").onClick(() -> clicks.set(clicks.get() + 1))
        );
    }
}
```

Em `Main`, configure o tema e chame
`MegalodonteApp.run(context -> context.useView(new WelcomeScreen()))`.
Veja os fontes completos em `app/src/main/java/my_app/` e o
[guia da API cliente](docs/CLIENT_API.md).

Android 15 (API 35) é o alvo e a versão mínima desta primeira etapa.

## Documentação

- [Contexto e estado atual](docs/CONTEXT.md)
- [Decisões arquiteturais](docs/DECISIONS.md)
- [Pendências e validação](docs/TODO.md)
- [Regras para manutenção](docs/AI_RULES.md)
- [Tarefa inicial](docs/PROMPT.md)
- [Arquitetura e contrato da bridge](docs/ARCHITECTURE.md)
- [Configuração, build e execução](docs/GETTING_STARTED.md)
- [Análise das bibliotecas e refatorações](docs/LIBRARY_ANALYSIS.md)
- [Matriz de compatibilidade e lacunas](docs/COMPATIBILITY.md)
- [Inventário reproduzível](docs/library-inventory.json)

## Módulos

| Módulo | Responsabilidade |
| --- | --- |
| `framework-core` | Facades cliente Android e runtime; depende dos artefatos comuns |
| `android-bridge` | Renderers nativos, propriedades/eventos, tema, insets e lifecycle |
| `app` | `Main`, `WelcomeScreen` e conexão mínima da Activity com o host do framework |

## Build

O workspace inclui via composite build os projetos irmãos megalodonte-core,
megalodonte-theme e megalodonte-reactivity-portable. Não recompila State/tema
dentro do backend; consulte a matriz antes de portar telas maiores.

Com o JDK compatível, `ANDROID_HOME` e SDK 35 configurados:

```powershell
./gradlew.bat :framework-core:check :app:assembleDebug :app:lintDebug
```

O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`.
Abra esta pasta como projeto no Android Studio para executar a demonstração.

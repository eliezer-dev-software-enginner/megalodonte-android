<p align="center">
  <img src="docs/images/megalodonte-logo.png" width="180" alt="Megalodonte logo">
</p>

# Megalodonte Android

Backend experimental Android do framework Megalodonte: componentes declarativos,
bridge e widgets nativos. Estado, tema e contratos
vêm do megalodonte-core compartilhado; o cliente declara megalodonte-theme
como dependência Gradle real. A paridade completa de todas as APIs ainda está pendente.

A aplicação cliente usa o padrão clássico do Megalodonte. Este trecho é uma
versão reduzida de `app/src/main/java/my_app/WelcomeScreen.java`; consulte o
arquivo para os imports e hooks de persistência do contador:

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

## Listas reativas e ciclo de vida

`Column.items` e `Row.items` aceitam um `ReadableState<List<T>>` (por exemplo,
`ListState`) além da lista estática:

```java
private final ListState<String> nomes = ListState.ofEmpty();

@Override public Component render() {
    return new Column(new ColumnProps().fillWidth())
        .items(nomes, nome -> new Button(nome, new ButtonProps().fillWidth()));
}

@Override public void onMount() {
    nomes.set(List.of("Ana", "Bruno"));
}
```

Cada mudança recria apenas as views do bloco, na mesma posição do layout; ainda
não há reconciliação por identidade nem preservação de foco/estado. `render()`
sempre roda antes de `onMount()`, e o runtime já está assinando os estados nesse
momento — por isso preencher uma `ListState` em `onMount` atualiza a tela sem
novo render. A ordem completa está no javadoc de `ScreenComponent` e em
[Arquitetura](docs/ARCHITECTURE.md).

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
- [Componentes JSONC no Android](docs/JSONC_ANDROID.md)
- [Inventário reproduzível](docs/library-inventory.json)

## Módulos

| Módulo | Responsabilidade |
| --- | --- |
| `framework-core` | Facades cliente Android e runtime; depende dos artefatos comuns |
| `android-bridge` | Renderers nativos, propriedades/eventos, tema, insets e lifecycle |
| `app` | `Main`, `WelcomeScreen` e conexão mínima da Activity com o host do framework |

O `android-bridge` também contém `AndroidJsoncScreen`, que interpreta views
JSONC e produz componentes Megalodonte renderizados pela bridge Android. Os
21 tipos de componentes do schema têm renderer Android; as diferenças de
propriedades em relação ao desktop estão em [JSONC_ANDROID.md](docs/JSONC_ANDROID.md). O
gerador externo `megalodonte-cli` oferece os modos Android `jsonc` e `native`;
consulte [JSONC_ANDROID.md](docs/JSONC_ANDROID.md) para o escopo dessa integração.

## Build

O workspace inclui via composite build os projetos irmãos megalodonte-core,
megalodonte-theme e megalodonte-reactivity-portable. Não recompila State/tema
dentro do backend; consulte a matriz antes de portar telas maiores.

O `settings.gradle.kts` exige esta disposição de diretórios:

```text
framework/
  megalodonte-android/
  libraries/
    megalodonte-core/
    megalodonte-theme/
    megalodonte-reactivity-portable/
  megalodonte-ecossystem/
    megalodonte-app1-welcome/
    megalodonte-app2-counter/
    megalodonte-app3-string-lenght/
    megalodonte-app4-hide-seek/
```

Esses projetos irmãos e exemplos não estão incluídos neste repositório. Os
exemplos em `framework/megalodonte-ecossystem/` são necessários para
`:framework-core:check`, conforme os caminhos definidos em
`framework-core/build.gradle.kts`.

Com JDK 25, Android SDK Platform 35, `ANDROID_HOME` configurado, as três
bibliotecas irmãs e os quatro exemplos presentes nos caminhos acima, execute
no PowerShell, a partir da raiz:

```powershell
./gradlew.bat :framework-core:check :app:assembleDebug :app:lintDebug
```

O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`.
Abra a raiz do repositório como projeto no Android Studio para executar a demonstração.
Veja [configuração e execução](docs/GETTING_STARTED.md) para requisitos,
instalação no dispositivo e comandos de verificação. O código Java usa nível 17;
o JDK 25 é exigido pelos projetos comuns, conforme esse guia.

## Licença

Este repositório não inclui um arquivo de licença. As condições de uso e
redistribuição precisam ser definidas pelos responsáveis pelo projeto.

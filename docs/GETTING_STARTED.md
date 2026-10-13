# Configuração e Execução

## Requisitos

- Android Studio compatível com AGP 9.0.1, ou ferramentas Android por linha de comando.
- JDK 25 disponível para os toolchains dos projetos comuns; o bytecode portátil/Android permanece Java 17.
- Android SDK Platform 35 e Build Tools 36.0.0 (padrão do AGP 9.0.1).
- Emulador/dispositivo Android 15 ou posterior para execução.
- Checkout das bibliotecas em `framework/libraries`, incluindo core/theme/reactivity-portable.

```text
megalodonte-world/
  framework/
    megalodonte-android/
    libraries/
      megalodonte-core/
      megalodonte-theme/
      megalodonte-reactivity-portable/
      megalodonte-reactivity/
```

Composite builds resolvem as coordenadas Maven para esses projetos irmãos.
Estado/tema têm artefatos únicos; portable prepara os fontes neutros originais.
O APK não depende do build desktop/SDK JavaFX. Fora deste workspace, publique
os artefatos, retire os includeBuild e configure o repositório Maven.

## Android Studio

Abra `megalodonte-android`, selecione um JDK compatível em Gradle JDK e instale
a plataforma 35 no SDK Manager. Aguarde o sync. Execute a configuração `app`
em um dispositivo API 35 ou posterior.

## IntelliJ IDEA

Abra a pasta raiz `megalodonte-android`, que contém `settings.gradle.kts`,
usando o wrapper Gradle do projeto. Configure o Gradle JVM com um JDK compatível
e use **Reload All Gradle Projects** na janela Gradle após alterações.
O build pela linha de comando também funciona a partir dessa configuração.
Para executar pelo botão Run da IDE, é necessário suporte Android instalado e
compatível com o AGP; o Android Studio fornece esse suporte.

Se aparecer `Task 'prepareKotlinBuildScriptModel' not found in project ':app'`,
recarregue o projeto com o `build.gradle.kts` atualizado. Os três módulos agora
expõem essa tarefa e delegam à tarefa real da raiz. Ela prepara o modelo dos
scripts `.gradle.kts` para a IDE, mesmo quando o código da aplicação é Java.

```powershell
./gradlew.bat :app:prepareKotlinBuildScriptModel
```

## PowerShell

No diretório do projeto, ajuste os caminhos para sua instalação:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
./gradlew.bat :framework-core:check :app:assembleDebug :app:lintDebug
```

Alternativamente, configure `sdk.dir` no `local.properties` (ignorado no Git).
Não altere o compile SDK apenas para aproveitar outra plataforma instalada.

## Instalar o APK

Com um dispositivo conectado e depuração USB habilitada, ou um emulador iniciado:

```powershell
./gradlew.bat :app:installDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n megalodonte.android.demo/my_app.MainActivity
```

## Verificação manual

1. Confirmar “Hello World”, contador e botão visíveis, sem sobreposição às barras.
2. Clicar duas vezes e confirmar “Cliques: 2”.
3. Girar o dispositivo e confirmar preservação do contador.
4. Repetir com escala de fonte aumentada e verificar leitura/toque.

O teste `:framework-core:check` compila e executa os fontes originais do
`megalodonte-app1-welcome`, sem modificá-los. Também cobre binding automático,
clique → estado → texto, eventos desconhecidos, componente duplicado, descarte,
troca de tela e restauração antes do render.
Esse check executa `bridgeTest` por `JavaExec`; o task `test` convencional está
desabilitado porque esta etapa usa um runner próprio sem JUnit.
Clientes 2–4 também são testados por runners vinculados ao check. Validação completa:

```powershell
./gradlew.bat :framework-core:check :megalodonte-core:check :megalodonte-reactivity-portable:check :app:verifySharedDependencies :app:assembleDebug :app:lintDebug
```

Build/lint não substituem a verificação visual em dispositivo.

## Instrumentação nativa

O runner próprio testa widgets reais, formatação sem loop, bidirecionalidade,
progresso, Show, troca de tema e descarte. Foi executado com sucesso no emulador
API 37; a validação específica de API 35 continua pendente.

```powershell
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
# Ajuste o serial para seu emulador de teste.
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5556 shell am instrument -w megalodonte.android.demo.test/megalodonte.android.demo.BridgeInstrumentedTest
```

Sucesso é informado por `Native ... tests passed` no stream. Falhas mostram a
exceção/assertion; não usar somente o exit code do adb para concluir que passou.

## Referências oficiais

- [Configuração Android 15 / API 35](https://developer.android.com/about/versions/15/setup-sdk?hl=pt-br)
- [Compatibilidade AGP 9.0.1](https://developer.android.com/build/releases/agp-9-0-0-release-notes)

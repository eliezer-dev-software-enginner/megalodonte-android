pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "megalodonte-android"
include(":framework-core", ":android-bridge", ":app")
includeBuild("../megalodonte-ecossystem/megalodonte-libs/megalodonte-core")
includeBuild("../megalodonte-ecossystem/megalodonte-libs/megalodonte-theme")
includeBuild("../megalodonte-ecossystem/megalodonte-libs/megalodonte-reactivity-portable")

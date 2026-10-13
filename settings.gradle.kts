pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "megalodonte-android"
include(":framework-core", ":android-bridge", ":app")
includeBuild("../libraries/megalodonte-core")
includeBuild("../libraries/megalodonte-theme")
includeBuild("../libraries/megalodonte-reactivity-portable")

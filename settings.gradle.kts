pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "megalodonte-android"
include(":megalodonte-base-android", ":megalodonte-bridge-android", ":app")
project(":megalodonte-base-android").projectDir = file("base-android")
project(":megalodonte-bridge-android").projectDir = file("android-bridge")
includeBuild("../libraries/megalodonte-core")
includeBuild("../libraries/megalodonte-theme")

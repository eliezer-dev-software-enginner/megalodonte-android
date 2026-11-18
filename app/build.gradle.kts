plugins { id("com.android.application") }
android {
    namespace = "megalodonte.android.demo"
    compileSdk = 35
    defaultConfig {
        applicationId = "megalodonte.android.demo"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "megalodonte.android.demo.BridgeInstrumentedTest"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":megalodonte-bridge-android"))
    implementation("megalodonte:megalodonte-theme:1.0.0-beta")
}
val verifySharedDependencies = tasks.register("verifySharedDependencies") {
    doLast {
        val forbidden = setOf("megalodonte-base", "megalodonte-base-desktop", "megalodonte-components", "megalodonte-reactivity", "megalodonte-router")
        val resolution = configurations.getByName("debugRuntimeClasspath").incoming.resolutionResult
        resolution.allDependencies.forEach { dependency ->
            if (dependency is org.gradle.api.artifacts.result.UnresolvedDependencyResult) throw GradleException("Unresolved shared dependency", dependency.failure)
        }
        resolution.allComponents.forEach { component ->
            val module = component.moduleVersion ?: return@forEach
            check(module.group != "org.openjfx" && !(module.group == "megalodonte" && module.name in forbidden)) {
                "Desktop dependency leaked into the APK: $module"
            }
        }
    }
}
tasks.named("check") { dependsOn(verifySharedDependencies) }

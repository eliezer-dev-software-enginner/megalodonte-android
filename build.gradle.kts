plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false
}

// IntelliJ can request Kotlin DSL preparation from a module rather than the root.
// Delegate to Gradle's root task so the actual model preparation still runs.
subprojects {
    tasks.register("prepareKotlinBuildScriptModel") {
        description = "Delegates Kotlin DSL model preparation to the root project."
        dependsOn(rootProject.tasks.named("prepareKotlinBuildScriptModel"))
    }
}

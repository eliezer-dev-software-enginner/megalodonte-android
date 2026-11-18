plugins { `java-library` }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    api("megalodonte:megalodonte-core:1.0.0-beta")
    testImplementation("megalodonte:megalodonte-theme:1.0.0-beta")
}

// These neutral APIs keep their established packages on Desktop and Android.
// Compile the original sources directly into megalodonte-base-android instead
// of publishing an empty wrapper module that only copied them.
val sharedReactivityRoot = file("../../libraries/megalodonte-reactivity/src/main/java")
val sharedReactivityPaths = listOf(
    "megalodonte/ComputedState.java",
    "megalodonte/ListenerManager.java",
    "megalodonte/ForEachState.java",
    "megalodonte/v2/ListState.java"
)
val prepareSharedReactivity = tasks.register<Sync>("prepareSharedReactivity") {
    doFirst {
        val missing = sharedReactivityPaths.filterNot { sharedReactivityRoot.resolve(it).isFile }
        if (missing.isNotEmpty()) {
            throw GradleException("Missing neutral reactivity sources in $sharedReactivityRoot: ${missing.joinToString()}")
        }
    }
    from(sharedReactivityRoot) { include(sharedReactivityPaths) }
    into(layout.buildDirectory.dir("generated/sources/shared-reactivity"))
}
sourceSets.main { java.srcDir(prepareSharedReactivity) }

// Compile the original desktop welcome client unchanged against the Android API.
val welcomeFixture = tasks.register<Sync>("prepareWelcomeFixture") {
    from("../../megalodonte-ecossystem/megalodonte-app1-welcome/src/main/java") {
        include("megalodonte_app1_welcome/Main.java", "megalodonte_app1_welcome/HomeScreen.java")
    }
    into(layout.buildDirectory.dir("generated/sources/welcome-fixture"))
}
sourceSets.test { java.srcDir(welcomeFixture) }
tasks.register<JavaExec>("bridgeTest") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("megalodonte.android.core.BridgeTest")
}
tasks.check { dependsOn("bridgeTest") }
// Contract tests run through JavaExec; no JUnit engine is used in this bootstrap.
tasks.test { enabled = false }

// Reactive list rendering is validated without the desktop client fixtures, which
// may be mid-migration in sibling repositories.
val listRenderingTest = sourceSets.create("listRenderingTest") {
    java.srcDir("src/listRenderingTest/java")
    compileClasspath += sourceSets.main.get().output + configurations.testCompileClasspath.get()
    runtimeClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
}
tasks.register<JavaExec>("listRenderingTest") {
    dependsOn(tasks.named(listRenderingTest.classesTaskName))
    classpath = listRenderingTest.runtimeClasspath
    mainClass.set("megalodonte.android.core.ListRenderingTest")
}
tasks.check { dependsOn("listRenderingTest") }

// Component-level contracts run through JavaExec as well, without the desktop
// client fixtures that reactive/list tests must avoid.
val componentTest = sourceSets.create("componentTest") {
    java.srcDir("src/componentTest/java")
    compileClasspath += sourceSets.main.get().output + configurations.testCompileClasspath.get()
    runtimeClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
}
tasks.register<JavaExec>("componentTest") {
    dependsOn(tasks.named(componentTest.classesTaskName))
    classpath = componentTest.runtimeClasspath
    mainClass.set("megalodonte.android.core.ComponentContractTest")
}
tasks.check { dependsOn("componentTest") }

listOf(2 to "megalodonte-app2-counter", 3 to "megalodonte-app3-string-lenght", 4 to "megalodonte-app4-hide-seek").forEach { (number, app) ->
    val fixtureSources = tasks.register<Sync>("prepareClient$number") {
        from("../../megalodonte-ecossystem/$app/src/main/java") { include("**/*.java") }
        into(layout.buildDirectory.dir("generated/sources/client-$number"))
    }
    val fixture = sourceSets.create("client$number") {
        java.srcDir(fixtureSources)
        java.srcDir("src/clientFixtures/java")
        compileClasspath += sourceSets.main.get().output + configurations.testCompileClasspath.get()
        runtimeClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    }
    val verify = tasks.register<JavaExec>("client${number}Test") {
        dependsOn(tasks.named(fixture.classesTaskName))
        classpath = fixture.runtimeClasspath
        mainClass.set("my_app.ClientFixtureTest")
        args(number.toString())
    }
    tasks.check { dependsOn(verify) }
}

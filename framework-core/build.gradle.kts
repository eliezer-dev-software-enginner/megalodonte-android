plugins { `java-library` }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    api("megalodonte:megalodonte-core:1.0.0-beta")
    api("megalodonte:megalodonte-reactivity-portable:1.0.0-beta")
    testImplementation("megalodonte:megalodonte-theme:1.0.0-beta")
}

// Compile the original desktop welcome client unchanged against the Android API.
val welcomeFixture = tasks.register<Sync>("prepareWelcomeFixture") {
    from("../../megalodonte-ecossystem/megalodonte-app1-welcome/src/main/java") {
        include("my_app/Main.java", "my_app/WelcomeScreen.java")
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

listOf(2 to "megalodonte-app2-counter", 3 to "megalodonte-app3-string-lenght", 4 to "megalodonte-app4-hide-seek").forEach { (number, app) ->
    val fixtureSources = tasks.register<Sync>("prepareClient$number") {
        from("../../megalodonte-ecossystem/$app/src/main/java") { include("my_app/*.java") }
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

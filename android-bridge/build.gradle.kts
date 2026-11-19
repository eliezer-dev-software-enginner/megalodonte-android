plugins { id("com.android.library") }
android {
    namespace = "megalodonte.android.bridge"
    compileSdk = 35
    defaultConfig { minSdk = 25 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    api(project(":megalodonte-base-android"))
    implementation("com.google.android.material:material:1.14.0")
}

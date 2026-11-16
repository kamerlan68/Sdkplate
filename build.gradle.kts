plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.extratools.manager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.extratools.manager"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
}
dependencies {
    implementation("androidx.documentfile:documentfile:1.0.1")
}

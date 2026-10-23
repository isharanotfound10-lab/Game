plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.velvet.superkeyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.velvet.superkeyboard"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { buildConfig = true }
}

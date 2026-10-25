// ============================================================
// IMPORTANTE: este app declara targetSdk 22 de propósito.
// No Android novo, apps com targetSdk alto recebem
//   "You cannot keep your settings in the secure settings."
// ao gravar min/peak_refresh_rate (mesmo com WRITE_SECURE_SETTINGS).
// Com targetSdk baixo o sistema libera. Para instalar, use a flag:
//   --bypass-low-target-sdk-block
// (Run > Edit Configurations > Install Flags, ou adb install).
// ============================================================
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val APP_TARGET_SDK = 22

android {
    namespace = "com.devthiago.ultrahertz"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devthiago.ultrahertz"
        minSdk = 22
        targetSdk = APP_TARGET_SDK
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
}

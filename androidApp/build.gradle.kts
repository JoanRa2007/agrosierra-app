plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "pe.upeu.agrosierra"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.upeu.agrosierra"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-unidad2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.koin.android)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.sqldelight.android.driver)
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Signing: keystore.properties locally (gitignored), SINQ_* env vars on CI.
// Both absent → release builds stay unsigned instead of failing.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(property: String, envVar: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(envVar)

android {
    namespace = "com.agpeya.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.agpeya.app"
        minSdk = 26
        targetSdk = 36
        // Versioning policy (semver-style):
        //   MINOR for features, PATCH for fixes/small tweaks.
        //   versionCode increments by 1 on EVERY update, no exceptions.
        versionCode = 45
        versionName = "1.0.5"

    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("storeFile", "SINQ_KEYSTORE_FILE")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("storePassword", "SINQ_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SINQ_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SINQ_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}

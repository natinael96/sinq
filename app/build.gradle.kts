import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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
        // Android 6.0. Below this the alarm architecture loses the APIs it is
        // built on (setAndAllowWhileIdle, FLAG_IMMUTABLE, the battery-optimisation
        // settings screen), all of which arrived in 23. java.time and
        // java.util.Base64 are carried down from 26 by desugaring, below.
        minSdk = 23
        targetSdk = 36
        // Versioning policy (semver-style):
        //   MINOR for features, PATCH for fixes/small tweaks.
        //   versionCode increments by 1 on EVERY update, no exceptions.
        versionCode = 58
        versionName = "1.7.0"

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
            // v2 only covers API 24+. With the floor at 23 the APK must also
            // carry the old JAR signature, or it will not install on Android 6
            // at all — the very devices this drop is for.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
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
        // Backports java.time (~50 files) and java.util.Base64 to the new floor,
        // so lowering minSdk costs no source changes for either.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
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
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.sqlite.jdbc)
}

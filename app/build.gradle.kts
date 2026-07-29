plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// CI (GitHub Actions) sets GITHUB_RUN_NUMBER, which increases by one on every workflow run -
// used as the build number so each APK that comes out of CI is a distinct, higher version than
// the last, and Android/Play-style tooling can tell them apart instead of treating every debug
// build as the same "1.0".
val ciBuildNumber = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0)

android {
    namespace = "gr.gtar.jobclosure"
    compileSdk = 35

    defaultConfig {
        applicationId = "gr.gtar.jobclosure"
        minSdk = 26
        targetSdk = 35
        versionCode = 1 + ciBuildNumber
        versionName = "1.0.$ciBuildNumber"
    }

    // A fixed debug keystore, committed to the repo: without this, the Android Gradle Plugin
    // auto-generates a fresh ~/.android/debug.keystore on whatever machine builds it - and since
    // every CI run happens on a brand-new GitHub Actions VM, every build would get signed with a
    // different key. Android refuses to install an APK over an existing app when the signing
    // certificate doesn't match, so the in-app updater's "install over the old version" would
    // silently require a full uninstall first, wiping all app data (settings, tokens, every saved
    // booking) on every single update. A debug key has no confidentiality to protect in the first
    // place (Android's own default has a universally-known password) - committing one just pins it
    // stable across builds instead of leaving it to chance per-machine.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))

    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Room (local storage of bookings)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (settings: home address, API key, reminder minutes)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Retrofit + Moshi (Google Directions API for travel time)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

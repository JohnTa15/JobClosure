plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Simulator + real-device targets; only the simulator one can actually run without a paid
    // Apple Developer account (real-device deployment needs a signing certificate that requires
    // enrolling in the program).
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // api, not implementation: consumers (Android app, desktop app, iOS app) construct
                // HttpClient instances themselves and pass them into GoogleCalendarRepository/
                // GoogleOAuthTokenService, so HttpClient needs to be on their compile classpath
                // too, not just this module's.
                api("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                // Multiplatform replacement for java.time, which doesn't exist on Kotlin/Native -
                // Booking.ceremonyStart/receptionStart and GoogleCalendarRepository's date
                // handling are built on this instead so the same code compiles for iOS too.
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                // Ktor client engine: CIO works on the JVM (Android app + desktop app) but not on
                // Kotlin/Native Apple targets, which need Darwin instead (see iosMain below) - no
                // engine is chosen explicitly in HttpClientFactory itself, so each target's own
                // single available engine is what Ktor resolves at compile time.
                implementation("io.ktor:ktor-client-cio:2.3.12")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

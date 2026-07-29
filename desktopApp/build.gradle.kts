import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI sets GITHUB_RUN_NUMBER; reused here so desktop and Android builds share the same version
// numbering scheme and a distributable can always be traced back to the CI run that produced it.
val ciBuildNumber = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0)

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

compose.desktop {
    application {
        mainClass = "gr.gtar.jobclosure.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "JobClosure"
            packageVersion = "1.0.${ciBuildNumber.coerceAtLeast(1)}"
            // jpackage's Windows/MSI packaging embeds this into a Win32 resource version-info
            // structure, which has historically had trouble with non-ASCII text - keep it plain
            // ASCII rather than Greek to avoid that failure mode.
            description = "JobClosure - wedding, baptism and event booking tracker"
            vendor = "JobClosure"
        }
    }
}

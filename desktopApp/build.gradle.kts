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
    // For DesktopUpdateChecker's own @Serializable GitHub API response classes - shared's own use
    // of kotlinx.serialization is `implementation`-scoped there, so it isn't visible here.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // shared.model.Booking's dates are kotlinx.datetime.Instant (multiplatform, so it also works
    // on iOS) - this pulls in just the toJavaInstant()/toKotlinInstant() interop conversions so
    // this JVM-only app's own UI code can keep using plain java.time/DateTimeFormatter.
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
}

// Optional: a Google Cloud OAuth "Desktop app" Client ID/Secret baked in at build time (from
// GitHub Actions secrets, never committed to source), so a fresh install can just show "Sign in
// with Google" instead of asking the user to paste their own credentials in first. Left blank
// (the default whenever these env vars aren't set - e.g. every build in this dev sandbox, and any
// fork/local build without the secrets configured) SignInScreen falls back to today's manual-entry
// form exactly as before, so this is additive and never a hard requirement to build the app.
val embeddedGoogleClientId = System.getenv("GOOGLE_OAUTH_CLIENT_ID") ?: ""
val embeddedGoogleClientSecret = System.getenv("GOOGLE_OAUTH_CLIENT_SECRET") ?: ""

// Bakes the same CI build number used for the packaged installer's own version, plus the optional
// embedded OAuth credentials above, into a small resource file - a plain Kotlin/JVM module like
// this one has no AGP-style BuildConfig to read either of these from otherwise.
val generateVersionProperties by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/version")
    outputs.dir(outputDir)
    inputs.property("embeddedGoogleClientId", embeddedGoogleClientId)
    inputs.property("embeddedGoogleClientSecret", embeddedGoogleClientSecret)
    doLast {
        val file = outputDir.get().asFile.resolve("version.properties")
        file.parentFile.mkdirs()
        file.writeText(
            "version=1.0.${ciBuildNumber.coerceAtLeast(1)}\n" +
                "embeddedGoogleClientId=$embeddedGoogleClientId\n" +
                "embeddedGoogleClientSecret=$embeddedGoogleClientSecret\n",
        )
    }
}

sourceSets {
    main {
        resources.srcDir(generateVersionProperties)
    }
}

tasks.named("processResources") {
    dependsOn(generateVersionProperties)
}

compose.desktop {
    application {
        mainClass = "gr.gtar.jobclosure.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            // jpackage bundles a trimmed JDK runtime (via jlink), built from an automatic jdeps
            // scan - which isn't reliable for classpath-mode apps and misses modules that aren't
            // among the obvious ones. GoogleAuthManager uses com.sun.net.httpserver.HttpServer
            // (the OAuth loopback redirect server) from the jdk.httpserver module; without listing
            // it explicitly here, the packaged app throws NoClassDefFoundError for it at runtime
            // even though `./gradlew run` (against a full, untrimmed JDK) works fine.
            modules("jdk.httpserver")
            packageName = "JobClosure"
            packageVersion = "1.0.${ciBuildNumber.coerceAtLeast(1)}"
            // jpackage's Windows/MSI packaging embeds this into a Win32 resource version-info
            // structure, which has historically had trouble with non-ASCII text - keep it plain
            // ASCII rather than Greek to avoid that failure mode.
            description = "JobClosure - wedding, baptism and event booking tracker"
            vendor = "JobClosure"

            windows {
                menu = true
                menuGroup = "JobClosure"
                shortcut = true
                perUserInstall = true
                // Fixed so every version's .msi shares one upgrade code: without it jpackage
                // generates a new one per build, and Windows treats each install as an unrelated
                // product - installing an update side-by-side instead of replacing the old one,
                // and leaving stale/duplicate entries in Settings > Apps that don't uninstall
                // cleanly. With a fixed code, installing a new version's .msi upgrades in place
                // and "Uninstall" in Settings > Apps (or Control Panel) always removes it fully -
                // Windows Installer provides that automatically, no separate uninstall.exe needed.
                upgradeUuid = "B07FAC92-44BC-44B8-BF6B-5B37A9F55D64"
            }
        }
    }
}

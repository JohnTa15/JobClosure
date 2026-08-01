package gr.gtar.jobclosure.desktop.update

import java.util.Properties

/**
 * The running app's own version, e.g. "1.0.42" - read from version.properties, a small resource
 * file desktopApp/build.gradle.kts generates at build time from the same CI build number baked
 * into the packaged installer's version, so this always matches what jpackage actually shipped.
 */
object AppVersion {
    val current: String by lazy {
        val props = Properties()
        Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("version.properties")
            ?.use { props.load(it) }
        props.getProperty("version", "1.0.0")
    }
}

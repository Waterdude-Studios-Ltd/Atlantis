package de.waterdu.atlantis.tine.gradle

import org.gradle.api.Project
import java.util.Properties

data class TineVersions(
    val group: String,
    val atlantis: String,
    val java: Int,
    val minecraft: String,
    val parchment: String,
    val fabricLoader: String,
    val fabricApi: String
) {

    val atlantisArtifact: String get() = "$atlantis+$minecraft"

    companion object {
        fun load(project: Project): TineVersions {
            val defaults = Properties().apply {
                TineVersions::class.java.getResourceAsStream("versions.properties").use { load(requireNotNull(it) { "Tine plugin is missing versions.properties" }) }
            }
            fun version(key: String): String = project.findProperty("tine.$key")?.toString() ?: requireNotNull(defaults.getProperty(key)) { "Missing version '$key'" }
            return TineVersions(
                group = defaults.getProperty("group"),
                atlantis = version("atlantis"),
                java = version("java").toInt(),
                minecraft = version("minecraft"),
                parchment = version("parchment"),
                fabricLoader = version("fabricLoader"),
                fabricApi = version("fabricApi")
            )
        }
    }
}

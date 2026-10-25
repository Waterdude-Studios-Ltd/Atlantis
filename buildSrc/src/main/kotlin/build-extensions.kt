import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*

val Project.mod: ModData get() = ModData(this)
fun Project.prop(key: String): String? = findProperty(key)?.toString()
fun String.upperCaseFirst() = replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }

fun RepositoryHandler.strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
    forRepository { maven(url) { name = alias } }
    filter { groups.forEach(::includeGroup) }
}

val Project.stonecutterBuild get() = extensions.getByType<StonecutterBuildExtension>()
val Project.stonecutterController get() = extensions.getByType<StonecutterControllerExtension>()

val Project.common get() = requireNotNull(stonecutterBuild.node.sibling("common")) {
    "No common project for $project"
}
val Project.commonProject get() = rootProject.project(stonecutterBuild.current.project)
val Project.commonMod get() = commonProject.mod

val Project.loader: String? get() = prop("loader")

/**
 * Publishes [jar] as `<group>.atlantis:<artifact>:<version>+<mc>` to the staging repository in the root build
 * directory, from which the root `publishToMavenRemote` task uploads to the Waterdude maven.
 * Loom strips jar-in-jar from mods it remaps for development, so the dependencies of [bundled] are declared in the POM
 * as runtime dependencies.
 */
fun Project.publishAtlantis(artifact: String, jar: String = "remapJar", sources: String? = null, bundled: String? = null) {
    pluginManager.apply("maven-publish")
    extensions.configure<PublishingExtension> {
        publications.create<MavenPublication>("atlantis") {
            groupId = "${commonMod.group}.atlantis"
            artifactId = artifact
            version = "${commonMod.version}+${commonMod.mc}"
            artifact(tasks.named(jar))
            sources?.let { artifact(tasks.named(it)) { classifier = "sources" } }
            pom {
                name = "${commonMod.name} ($artifact)"
                description = commonMod.description
                url = commonMod.homepage
                licenses {
                    commonMod.license.split(",").map { it.trim() }.forEach { license ->
                        license { name = license }
                    }
                }
                developers {
                    developer { name = commonMod.author }
                }
                scm {
                    url = commonMod.github
                }
            }
            bundled?.let { configuration ->
                pom.withXml {
                    val dependencies = asNode().appendNode("dependencies")
                    configurations.getByName(configuration).allDependencies.forEach { dependency ->
                        dependencies.appendNode("dependency").apply {
                            appendNode("groupId", dependency.group)
                            appendNode("artifactId", dependency.name)
                            appendNode("version", dependency.version)
                            appendNode("scope", "runtime")
                            appendNode("exclusions").appendNode("exclusion").apply {
                                appendNode("groupId", "*")
                                appendNode("artifactId", "*")
                            }
                        }
                    }
                }
            }
        }
        repositories {
            maven(rootProject.layout.buildDirectory.dir(MAVEN_STAGING)) { name = "Staging" }
        }
    }
    tasks.matching { it.name.endsWith("ToStagingRepository") }.configureEach {
        mustRunAfter(":cleanMavenStaging")
    }
}

const val MAVEN_STAGING = "maven-staging"

@JvmInline
value class ModData(private val project: Project) {
    val id: String get() = modProp("id")
    val name: String get() = modProp("name")
    val version: String get() = modProp("version")
    val group: String get() = modProp("group")
    val author: String get() = modProp("author")
    val description: String get() = modProp("description")
    val license: String get() = modProp("license")
    val github: String get() = modProp("github")
    val homepage: String get() = modProp("homepage")
    val mc: String get() = depOrNull("minecraft") ?: project.stonecutterBuild.current.version
    val pixelmon: String get() = prop("pixelmon_version")
    val cobblemon: String get() = prop("cobblemon_version")
    val impactor: String get() = prop("impactor_version")
    val fpapi: String get() = prop("fpapi_version")
    val vault: String get() = prop("vault_version")

    fun propOrNull(key: String) = project.prop(key)
    fun prop(key: String) = requireNotNull(propOrNull(key)) { "Missing '$key'" }
    fun modPropOrNull(key: String) = project.prop("mod.$key")
    fun modProp(key: String) = requireNotNull(modPropOrNull(key)) { "Missing 'mod.$key'" }
    fun depOrNull(key: String): String? = project.prop("deps.$key")?.takeIf { it.isNotEmpty() && it != "" }
    fun dep(key: String) = requireNotNull(depOrNull(key)) { "Missing 'deps.$key'" }
    fun modrinth(name: String, version:String) = "maven.modrinth:$name:$version"
}
package de.waterdu.atlantis.tine.gradle

import groovy.json.JsonSlurper
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

/**
 * Sets up a Tine project: compiles against vanilla Minecraft with Mojang mappings and Parchment plus Atlantis' common
 * API, builds a Mojang-named jar, and adds a Fabric server run that loads the Tine through Trident, plus a client run
 * that joins it. Tines are server-only, so the client run doesn't load the Tine itself.
 *
 * Mixins stay in Mojang names without a refmap, since NeoForge runs on Mojang names and Atlantis remaps Tines for
 * Fabric production itself, so Loom's mixin annotation processor is turned off.
 *
 * Versions default to the Atlantis release this plugin was built with, and can be overridden with the Gradle
 * properties `tine.atlantis`, `tine.minecraft`, `tine.parchment`, `tine.fabricLoader` and `tine.fabricApi`.
 */
class TinePlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        pluginManager.apply("java")
        pluginManager.apply("fabric-loom")

        val versions = TineVersions.load(this)

        repositories.apply {
            mavenLocal()
            mavenCentral()
            maven("https://maven.fabricmc.net/") { name = "Fabric" }
            exclusiveContent {
                forRepository { maven("https://maven.parchmentmc.org") { name = "ParchmentMC" } }
                filter { includeGroup("org.parchmentmc.data") }
            }
            maven("https://waterdu.de/maven") {
                name = "Waterdude"
                content { includeGroupAndSubgroups(versions.group) }
            }
        }

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(versions.java))
        }
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(versions.java)
        }

        val loom = extensions.getByType<LoomGradleExtensionAPI>()
        loom.mixin.useLegacyMixinAp.set(false)
        dependencies.apply {
            add("minecraft", "com.mojang:minecraft:${versions.minecraft}")
            add("mappings", loom.layered {
                officialMojangMappings()
                parchment("org.parchmentmc.data:parchment-${versions.minecraft}:${versions.parchment}@zip")
            })
            add("modCompileOnly", "${versions.group}:atlantis-common:${versions.atlantisArtifact}")
            add("compileOnly", "org.jspecify:jspecify:1.0.0")

            add("modLocalRuntime", "net.fabricmc:fabric-loader:${versions.fabricLoader}")
            add("modLocalRuntime", "net.fabricmc.fabric-api:fabric-api:${versions.fabricApi}+${versions.minecraft}")
            add("modLocalRuntime", "${versions.group}:atlantis-fabric:${versions.atlantisArtifact}")
        }

        configureArtifact(versions)
        configureRuns(loom)
    }

    private fun Project.configureArtifact(versions: TineVersions) {
        tasks.named<ProcessResources>("processResources") {
            val tokens = mapOf(
                "version" to project.version.toString(),
                "atlantisVersion" to versions.atlantis,
                "minecraftVersion" to versions.minecraft
            )
            inputs.properties(tokens)
            filesMatching(TINE_JSON) {
                filter { line -> tokens.entries.fold(line) { expanded, (key, value) -> expanded.replace("\${$key}", value) } }
            }
            doLast {
                checkTineJson(destinationDir.resolve(TINE_JSON))
            }
        }

        tasks.named<Jar>("jar") {
            archiveClassifier.set("")
            destinationDirectory.set(layout.buildDirectory.dir("libs"))
        }
        tasks.matching { it.name == "remapJar" || it.name == "remapSourcesJar" }.configureEach {
            enabled = false
        }
    }

    private fun Project.configureRuns(loom: LoomGradleExtensionAPI) {
        val sourceSets = extensions.getByType<SourceSetContainer>()
        val main = sourceSets.getByName("main")
        val tineRun = sourceSets.create(TINE_RUN)
        configurations.named(tineRun.runtimeClasspathConfigurationName) {
            extendsFrom(configurations.getByName(main.runtimeClasspathConfigurationName))
        }

        val serverDir = layout.projectDirectory.dir(SERVER_RUN_DIR)
        val prepareTineRun = tasks.register("prepareTineRun") {
            group = "tine"
            description = "Accepts the EULA and turns off online mode for the Tine Server run, so the Tine Client can join it."
            val eula = serverDir.file("eula.txt").asFile
            val properties = serverDir.file(SERVER_PROPERTIES).asFile
            outputs.files(eula, properties)
            outputs.upToDateWhen { false }
            doLast {
                serverDir.asFile.mkdirs()
                eula.writeText("eula=true\n")
                val lines = if (properties.isFile) properties.readLines().filterNot { it.startsWith("$ONLINE_MODE=") } else emptyList()
                properties.writeText((lines + "$ONLINE_MODE=false").joinToString("\n", postfix = "\n"))
            }
        }
        tasks.named(tineRun.classesTaskName) {
            dependsOn(main.classesTaskName, prepareTineRun)
            val outputDirs = provider { tineRun.output.classesDirs.files + listOfNotNull(tineRun.output.resourcesDir) }
            doLast {
                outputDirs.get().forEach { it.mkdirs() }
            }
        }

        val tinePaths = provider {
            (main.output.classesDirs.files + listOfNotNull(main.output.resourcesDir)).joinToString(File.pathSeparator) { it.absolutePath }
        }
        loom.runs.named("client") {
            client()
            configName = "Tine Client"
            source(tineRun)
            runDir(CLIENT_RUN_DIR)
            ideConfigGenerated(true)
            programArgs("--quickPlayMultiplayer", "localhost:${serverPort(serverDir.file(SERVER_PROPERTIES).asFile)}")
        }
        loom.runs.named("server") {
            server()
            configName = "Tine Server"
            source(tineRun)
            runDir(SERVER_RUN_DIR)
            ideConfigGenerated(true)
            vmArg("-D$DEV_PROPERTY=${tinePaths.get()}")
            programArg("nogui")
        }
    }

    private fun serverPort(properties: File): String {
        val port = if (properties.isFile) properties.readLines().firstOrNull { it.startsWith("server-port=") }?.substringAfter('=')?.trim() else null
        return port?.takeIf { it.isNotEmpty() } ?: DEFAULT_PORT
    }

    private fun checkTineJson(file: File) {
        if (!file.isFile) {
            throw GradleException("Tines need a $TINE_JSON in src/main/resources, see https://waterdu.de/atlantis/schemas/tine.schema.json")
        }
        val metadata = JsonSlurper().parse(file) as? Map<*, *> ?: throw GradleException("$TINE_JSON must be a JSON object")
        val id = metadata["id"] as? String
        if (id == null || !ID.matches(id)) {
            throw GradleException("$TINE_JSON has invalid id '$id', IDs may only contain [a-z0-9_.-]")
        }
        if (metadata["version"] !is String) {
            throw GradleException("$TINE_JSON is missing a version")
        }
        val mixins = metadata["mixins"] ?: return
        if (mixins !is List<*> || mixins.any { it !is String }) {
            throw GradleException("$TINE_JSON has invalid mixins, they must be a list of mixin config paths")
        }
        mixins.filterIsInstance<String>().filterNot { file.resolveSibling(it).isFile }.takeIf { it.isNotEmpty() }?.let { missing ->
            throw GradleException("$TINE_JSON lists mixin configs that aren't in src/main/resources: ${missing.joinToString()}")
        }
    }

    private companion object {
        const val TINE_JSON = "tine.json"
        const val TINE_RUN = "tineRun"
        const val SERVER_RUN_DIR = "run/server"
        const val CLIENT_RUN_DIR = "run/client"
        const val SERVER_PROPERTIES = "server.properties"
        const val ONLINE_MODE = "online-mode"
        const val DEFAULT_PORT = "25565"
        const val DEV_PROPERTY = "atlantis.trident.dev"
        val ID = Regex("[a-z0-9_.-]+")
    }
}

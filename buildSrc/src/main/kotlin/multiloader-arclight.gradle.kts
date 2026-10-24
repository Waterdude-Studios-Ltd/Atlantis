import groovy.json.JsonSlurper
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    id("java")
}

val arclightLoader = requireNotNull(loader) { "No loader set for $project" }
val arclightEconomy = (findProperty("arclight.economy")?.toString() ?: "vault").lowercase()
require(arclightEconomy in setOf("vault", "impactor")) {
    "Unknown arclight.economy '$arclightEconomy', expected 'vault' or 'impactor'"
}

val arclightRunDir = layout.projectDirectory.dir("arclight-run")
val arclightDownloads = layout.buildDirectory.dir("arclight/downloads")

fun download(name: String, url: String, fileName: String) = tasks.register<DownloadFile>("download${name.upperCaseFirst()}") {
    group = "arclight"
    this.url = url
    destination = arclightDownloads.map { it.file(fileName) }
}

val arclightJarName = "arclight-$arclightLoader-${commonMod.mc}-${commonMod.prop("arclight_build")}.jar"
val downloadArclight = download(
    "arclight",
    "https://github.com/IzzelAliz/Arclight/releases/download/${commonMod.prop("arclight_release")}/$arclightJarName",
    arclightJarName
)

val pluginDownloads = if (arclightEconomy == "vault") {
    val vault = commonMod.prop("vault_plugin_version")
    val essentials = commonMod.prop("essentialsx_version")
    listOf(
        download("vaultPlugin", "https://github.com/MilkBowl/Vault/releases/download/$vault/Vault.jar", "Vault-$vault.jar"),
        download("essentialsXPlugin", "https://github.com/EssentialsX/Essentials/releases/download/$essentials/EssentialsX-$essentials.jar", "EssentialsX-$essentials.jar")
    )
} else {
    emptyList()
}

val arclightMods: Configuration by configurations.creating {
    isTransitive = false
    isCanBeConsumed = false
}

dependencies {
    if (arclightEconomy == "impactor") {
        arclightMods(commonMod.modrinth("impactor", "${commonMod.impactor}+${commonMod.mc}-${if (arclightLoader == "fabric") "Fabric" else "Neoforge"}"))
    }
    if (arclightLoader == "fabric") {
        arclightMods("net.fabricmc.fabric-api:fabric-api:${commonMod.dep("fabric_api")}+${commonMod.mc}")
    }
}

/**
 * Places the loader installer Arclight wants on first launch into the server directory, because Arclight's own
 * download mirror is unreachable.
 */
fun seedArclightInstaller(arclightJar: File, runDir: File, loader: String) {
    val installer = ZipFile(arclightJar).use { zip ->
        zip.getInputStream(zip.getEntry("META-INF/installer.json")).use { JsonSlurper().parse(it) as Map<*, *> }
    }["installer"] as Map<*, *>
    val (url, target, hash) = if (loader == "fabric") {
        val version = installer["fabricLoader"]
        Triple(
            "https://maven.fabricmc.net/net/fabricmc/fabric-loader/$version/fabric-loader-$version.jar",
            runDir.resolve("libraries/net/fabricmc/fabric-loader/$version/fabric-loader-$version.jar"),
            installer["fabricLoaderHash"]
        )
    } else {
        val version = installer["neoforge"]
        Triple(
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/$version/neoforge-$version-installer.jar",
            runDir.resolve("neoforge-$version-installer.jar"),
            installer["neoforgeHash"]
        )
    }
    val sha1 = { file: File -> MessageDigest.getInstance("SHA-1").digest(file.readBytes()).joinToString("") { "%02x".format(it) } }
    if (target.isFile && sha1(target).equals(hash.toString(), ignoreCase = true)) {
        return
    }
    target.parentFile.mkdirs()
    URI(url).toURL().openStream().use { input -> target.outputStream().use { input.copyTo(it) } }
}

val modJar = tasks.named<AbstractArchiveTask>(if (arclightLoader == "fabric") "remapJar" else "jar").flatMap { it.archiveFile }

val prepareArclightServer = tasks.register("prepareArclightServer") {
    group = "arclight"
    description = "Installs Atlantis and the $arclightEconomy economy into the Arclight server directory."

    val runDir = arclightRunDir.asFile
    val mods = files(modJar, arclightMods)
    val plugins = files(pluginDownloads)
    val arclightJar = downloadArclight.flatMap { it.destination }
    inputs.files(mods, plugins)
    inputs.file(arclightJar)
    outputs.upToDateWhen { false }

    doLast {
        val manifest = runDir.resolve(".atlantis-managed")
        if (manifest.isFile) {
            manifest.readLines().filter { it.isNotBlank() }.forEach { runDir.resolve(it).delete() }
        }

        val installed = mutableListOf<String>()
        fun install(files: Iterable<File>, folder: String) = files.forEach { file ->
            val target = runDir.resolve("$folder/${file.name}")
            file.copyTo(target, overwrite = true)
            installed += "$folder/${file.name}"
        }
        install(mods, "mods")
        install(plugins, "plugins")
        manifest.writeText(installed.joinToString("\n"))
        seedArclightInstaller(arclightJar.get().asFile, runDir, arclightLoader)

        runDir.resolve("eula.txt").writeText("eula=true\n")
        val properties = runDir.resolve("server.properties")
        if (!properties.exists()) {
            properties.writeText(
                """
                online-mode=false
                enable-rcon=true
                rcon.port=25575
                rcon.password=atlantis
                motd=Atlantis Arclight ($arclightLoader)
                """.trimIndent() + "\n"
            )
        }
    }
}

tasks.register<JavaExec>("runArclightServer") {
    group = "arclight"
    description = "Runs an Arclight ($arclightLoader) server with Atlantis and the $arclightEconomy economy installed."
    dependsOn(prepareArclightServer)

    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    classpath(downloadArclight.flatMap { it.destination })
    mainClass = "io.izzel.arclight.server.Launcher"
    maxHeapSize = "4G"
    args("nogui")

    workingDir(arclightRunDir)
    standardInput = System.`in`
}

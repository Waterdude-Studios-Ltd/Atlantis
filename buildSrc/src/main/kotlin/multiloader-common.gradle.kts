plugins {
    id("java")
    id("idea")
    id("java-library")
}

version = "${loader}-${commonMod.version}+mc${stonecutterBuild.current.version}"

base {
    archivesName = commonMod.id
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(commonProject.prop("java.version")!!)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://libraries.minecraft.net")
    maven("https://plugins.gradle.org/m2/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    exclusiveContent {
        forRepository {
            maven("https://repo.spongepowered.org/repository/maven-public") { name = "Sponge" }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepositories(
            maven("https://maven.parchmentmc.org") { name = "ParchmentMC" },
            maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        )
        filter { includeGroup("org.parchmentmc.data") }
    }
    maven("https://www.cursemaven.com")
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://jitpack.io")
    maven("https://waterdu.de/maven") { content { includeGroupAndSubgroups("de.waterdu") } }
    maven("https://hub.spigotmc.org/nexus/repository/public/")
    maven("https://maven.impactdev.net/repository/development/")
    exclusiveContent {
        forRepositories(maven("https://maven.impactdev.net/repository/development/") { name = "ImpactDev" })
        filter {
            includeGroup("com.cobblemon")
            includeGroup("net.impactdev.impactor.api")
        }
    }
    exclusiveContent {
        forRepositories(maven("https://maven.envyware.co.uk/releases") { name = "EnvyWare" })
        filter { includeGroup("com.envyful.papi") }
    }

    ivy {
        setUrl("https://download.nodecdn.net/containers/reforged/universal/release")
        metadataSources {
            artifact()
            gradleMetadata()
        }
        patternLayout {
            artifact("[artifact].[ext]")
        }
    }

    flatDir {
        val libsDir = rootProject.file("libs").apply { mkdirs() }
        val localLibs = file("${System.getProperty("user.home")}/Code/Libraries")
        val envLibs = System.getenv("LIBRARIES_DIR")?.let { file(it) }
        dirs(listOfNotNull(libsDir, localLibs.takeIf { it.exists() }, envLibs?.takeIf { it.exists() }))
    }
}

tasks {

    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val expandProps = mapOf(
            "javaVersion" to commonMod.propOrNull("java.version"),
            "modId" to commonMod.id,
            "modName" to commonMod.name,
            "modVersion" to commonMod.version,
            "modGroup" to commonMod.group,
            "modAuthor" to commonMod.author,
            "modDescription" to commonMod.description,
            "modLicense" to commonMod.license,
            "modGitHub" to commonMod.github,
            "modHomepage" to commonMod.homepage,
            "minecraftVersion" to commonMod.propOrNull("minecraft_version"),
            "minMinecraftVersion" to commonMod.propOrNull("min_minecraft_version"),
            "fabricLoaderVersion" to (commonMod.depOrNull("fabric_loader") ?: commonMod.depOrNull("fabric-loader")),
            "fabricLoaderMinVersion" to (commonMod.depOrNull("fabric_loader_min") ?: commonMod.depOrNull("fabric_loader") ?: commonMod.depOrNull("fabric-loader")),
            "fabricApiVersion" to (commonMod.depOrNull("fabric_api") ?: commonMod.depOrNull("fabric-api")),
            "neoForgeVersion" to commonMod.depOrNull("neoforge"),
            "forgeVersion" to commonMod.depOrNull("forge"),
            // "yaclVersion" to commonMod.depOrNull("yacl"),
            "modMenuVersion" to commonMod.depOrNull("modmenu")
        ).filterValues { it?.isNotEmpty() == true }.mapValues { (_, v) -> v!! }

        val jsonExpandProps = expandProps.mapValues { (_, v) -> v.replace("\n", "\\\\n") }

        filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
            expand(expandProps)
        }

        filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
            expand(jsonExpandProps)
        }

        inputs.properties(expandProps)
    }
}

tasks.named("processResources") {
    dependsOn(":common:${commonMod.propOrNull("minecraft_version")}:stonecutterGenerate")
}

val mockitoAgent: Configuration by configurations.creating

dependencies {
    "testImplementation"(platform("org.junit:junit-bom:5.11.4"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testImplementation"("org.assertj:assertj-core:3.27.3")
    "testImplementation"("org.mockito:mockito-core:5.15.2")
    mockitoAgent("org.mockito:mockito-core:5.15.2") {
        isTransitive = false
    }
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-javaagent:${mockitoAgent.singleFile.absolutePath}", "-Xshare:off")
    })
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs tests tagged 'integration'."
    group = "verification"
    val test = tasks.named<Test>("test").get()
    testClassesDirs = test.testClassesDirs
    classpath = test.classpath
    useJUnitPlatform {
        includeTags("integration")
    }
}
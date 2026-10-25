repositories {
    maven("https://libraries.minecraft.net")
    maven("https://plugins.gradle.org/m2/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://jitpack.io")
    maven("https://waterdu.de/maven") { content { includeGroupAndSubgroups("de.waterdu") } }
    maven("https://hub.spigotmc.org/nexus/repository/public/")
    mavenCentral()
    mavenLocal()

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

plugins {
    id("multiloader-common")
    id("fabric-loom")
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.3.12"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.14"
    id("java-library")
    id("maven-publish")
}

stonecutter {

}

fletchingTable {
    j52j.register("main") {
        extension("json", "**/*.json5")
    }
}

loom {
    mixin {
        useLegacyMixinAp = false
    }
    interfaceInjection {
        enableDependencyInterfaceInjection = true
    }
    accessWidenerPath = rootProject.file("common/src/main/resources/atlantis.accesswidener")
}

dependencies {
    val is_atlantis = commonMod.id == "atlantis"

    fun localOptionalJar(name: String): File? = listOfNotNull(
        rootProject.file("libs/$name"),
        file("${System.getProperty("user.home")}/Code/Libraries/$name"),
        System.getenv("LIBRARIES_DIR")?.let { file("$it/$name") }
    ).firstOrNull { it.isFile }

    fun optionalIntegration(
        version: String,
        dependencies: (String) -> List<String>,
        localJar: String,
        transitive: Boolean = true
    ) {
        version.takeIf { it.isNotBlank() }?.let { dependencies(it).forEach { dependency -> compileOnly(dependency) { isTransitive = transitive } } }
            ?: localOptionalJar(localJar)?.let { compileOnly(files(it)) }
    }

    minecraft("com.mojang:minecraft:${commonMod.mc}")
    mappings(loom.layered {
        officialMojangMappings()
        commonMod.depOrNull("parchment")?.let { parchmentVersion ->
            parchment("org.parchmentmc.data:parchment-${commonMod.mc}:$parchmentVersion@zip")
        }
    })

    compileOnly("org.spongepowered:mixin:0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")

    implementation("org.jspecify:jspecify:1.0.0")
    annotationProcessor("org.jspecify:jspecify:1.0.0")

    implementation(fileTree("libs") {
        include("*.jar")
    })

    if (is_atlantis) {
        compileOnly("io.netty:netty-all:4.1.86.Final")
        compileOnly("com.zaxxer:HikariCP:4.0.3")
        compileOnly("mysql:mysql-connector-java:8.0.29")
        compileOnly("io.lettuce:lettuce-core:6.2.3.RELEASE")
        compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.4")
        compileOnly("io.github.classgraph:classgraph:4.8.197")

        compileOnly("org.spigotmc:spigot-api:${commonMod.mc}-R0.1-SNAPSHOT")

        listOf(
            "io.netty:netty-all:4.1.86.Final",
            "com.zaxxer:HikariCP:4.0.3",
            "mysql:mysql-connector-java:8.0.29",
            "io.lettuce:lettuce-core:6.2.3.RELEASE",
            "com.github.ben-manes.caffeine:caffeine:3.2.4",
            "io.github.classgraph:classgraph:4.8.197",
        ).forEach { testImplementation(it) }

        testImplementation("org.spongepowered:mixin:0.8.5")
        testImplementation("io.github.llamalad7:mixinextras-common:0.4.1")
        testImplementation("com.google.testing.compile:compile-testing:0.21.0")
        testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.testcontainers:mysql")
    } else {
        implementation("de.waterdu.atlantis:Atlantis121:0.0.1")
    }

    optionalIntegration(
        commonMod.cobblemon,
        { version -> listOf("com.cobblemon:neoforge:$version") },
        "Cobblemon.jar",
        transitive = false
    )
    optionalIntegration(
        commonMod.impactor,
        { version -> listOf("economy", "core").map { "net.impactdev.impactor.api:$it:$version" } },
        "Impactor.jar",
        transitive = false
    )
    optionalIntegration(
        commonMod.fpapi,
        { version -> listOf("com.envyful.papi:api:$version") },
        "FPAPI121.jar"
    )
}

val gametest: SourceSet = sourceSets.create("gametest") {
    val main = sourceSets.main.get()
    compileClasspath += main.compileClasspath + main.output
    runtimeClasspath += main.runtimeClasspath + main.output
}

val commonJava: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonGametestJava: Configuration = configurations.create("commonGametestJava") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonGametestResources: Configuration = configurations.create("commonGametestResources") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    afterEvaluate {
        val mainSourceSet = sourceSets.main.get()
        mainSourceSet.java.sourceDirectories.files.forEach {
            add(commonJava.name, it)
        }
        mainSourceSet.resources.sourceDirectories.files.forEach {
            add(commonResources.name, it)
        }
        gametest.java.sourceDirectories.files.forEach {
            add(commonGametestJava.name, it)
        }
        gametest.resources.sourceDirectories.files.forEach {
            add(commonGametestResources.name, it)
        }
    }
}

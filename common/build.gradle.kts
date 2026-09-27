import org.codehaus.groovy.runtime.DefaultGroovyMethods.mixin
import org.gradle.kotlin.dsl.annotationProcessor
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.include
import org.gradle.kotlin.dsl.kotlin

repositories {
    maven("https://libraries.minecraft.net")
    maven("https://plugins.gradle.org/m2/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://jitpack.io")
    maven("https://waterdu.de/maven")
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
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.14"
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.3.1"
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
}

sourceSets.main {
    resources.srcDir(rootProject.file("fabric/src/main/resources"))
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
        dependency: (String) -> String,
        localJar: String
    ) {
        version.takeIf { it.isNotBlank() }?.let { implementation(dependency(it)) }
            ?: localOptionalJar(localJar)?.let { implementation(files(it)) }
    }

    minecraft("com.mojang:minecraft:${commonMod.mc}")
    mappings(loom.layered {
        officialMojangMappings()
        commonMod.depOrNull("parchment")?.let { parchmentVersion ->
            parchment("org.parchmentmc.data:parchment-${commonMod.mc}:$parchmentVersion@zip")
        }
    })

    compileOnly("org.spongepowered:mixin:0.8.5")

    implementation("org.jspecify:jspecify:1.0.0")
    annotationProcessor("org.jspecify:jspecify:1.0.0")

    implementation(fileTree("libs") {
        include("*.jar")
    })

    if (is_atlantis) {
        shadow("io.netty:netty-all:4.1.86.Final")
        shadow("com.zaxxer:HikariCP:4.0.3")
        shadow("mysql:mysql-connector-java:8.0.29")
        shadow("io.lettuce:lettuce-core:6.2.3.RELEASE")
        shadow("com.github.ben-manes.caffeine:caffeine:3.2.4")
        shadow("io.github.classgraph:classgraph:4.8.197")

        compileOnly("org.spigotmc:spigot-api:${commonMod.mc}-R0.1-SNAPSHOT")
    } else {
        implementation("de.waterdu.atlantis:Atlantis121:0.0.1")
    }

    implementation("pixelmon:Pixelmon-${commonMod.mc}-${commonMod.pixelmon}-universal:${commonMod.pixelmon}")

    optionalIntegration(
        commonMod.cobblemon,
        { version -> "com.cobblemon.mod:Cobblemon:$version" },
        "Cobblemon.jar"
    )
    optionalIntegration(
        commonMod.impactor,
        { version -> "net.impactdev.impactor:Impactor:$version" },
        "Impactor.jar"
    )
    optionalIntegration(
        commonMod.fpapi,
        { version -> "com.envyful.papi:FPAPI121:$version" },
        "FPAPI121.jar"
    )
}

val commonJava: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources: Configuration by configurations.creating {
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
    }
}

tasks.shadowJar {
    configurations = listOf(project.configurations["shadow"])
    archiveClassifier.set("")
    isZip64 = true

    exclude("dummyThing")
    exclude("de.waterdu.atlantis.mixin**")
    relocate("com.google.protobuf", "de.waterdu.atlantis.shade.com.google.protobuf")
    relocate("com.mysql", "de.waterdu.atlantis.shade.com.mysql")
    relocate("com.zaxxer", "de.waterdu.atlantis.shade.com.zaxxer")
    relocate("io.github", "de.waterdu.atlantis.shade.io.github")
    relocate("io.lettuce", "de.waterdu.atlantis.shade.io.lettuce")
    relocate("io.netty", "de.waterdu.atlantis.shade.io.netty")
    relocate("org.reactivestreams", "de.waterdu.atlantis.shade.org.reactivestreams")
    relocate("org.slf4j", "de.waterdu.atlantis.shade.org.slf4j")
    relocate("reactor", "de.waterdu.atlantis.shade.reactor")
    exclude("**/module-info.class")
    exclude("**/*.properties")
    exclude("**/*.xml")
    exclude("META-INF/versions/9/**")
    exclude("META-INF/native-image/**")
    exclude("META-INF/services/**")
}

tasks.jar {
    finalizedBy(tasks.shadowJar)
}
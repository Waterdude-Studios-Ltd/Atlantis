import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

val atlantis = Properties().apply {
    listOf("../gradle.properties", "../versions/1.21.1/gradle.properties").forEach { path ->
        file(path).reader().use { reader -> Properties().apply { load(reader) }.forEach { (key, value) -> if (value.toString().isNotBlank()) put(key, value) } }
    }
}

fun atlantis(key: String): String = requireNotNull(atlantis.getProperty(key)) { "Missing '$key' in Atlantis' gradle.properties" }

group = "${atlantis("mod.group")}.atlantis"
version = atlantis("mod.version")

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    implementation("net.fabricmc:fabric-loom:1.11-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("tine") {
            id = "de.waterdu.atlantis.tine"
            displayName = "Atlantis Tine"
            description = "Builds and runs Tines, mods loaded by Atlantis' Trident loader."
            implementationClass = "de.waterdu.atlantis.tine.gradle.TinePlugin"
        }
    }
}

val generateVersions = tasks.register<WriteProperties>("generateVersions") {
    destinationFile = layout.buildDirectory.file("generated/tine/de/waterdu/atlantis/tine/gradle/versions.properties")
    property("atlantis", atlantis("mod.version"))
    property("group", "${atlantis("mod.group")}.atlantis")
    property("java", atlantis("java.version"))
    property("minecraft", atlantis("minecraft_version"))
    property("parchment", atlantis("deps.parchment"))
    property("fabricLoader", atlantis("deps.fabric_loader"))
    property("fabricApi", atlantis("deps.fabric_api"))
}

sourceSets.main {
    resources.srcDir(generateVersions.map { layout.buildDirectory.dir("generated/tine").get() })
}

java {
    withSourcesJar()
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Atlantis Tine Gradle plugin"
            description = "Builds and runs Tines, mods loaded by Atlantis' Trident loader."
            url = atlantis("mod.homepage")
            licenses {
                atlantis("mod.license").split(",").map { it.trim() }.forEach { license ->
                    license { name = license }
                }
            }
            developers {
                developer { name = atlantis("mod.author") }
            }
            scm {
                url = atlantis("mod.github")
            }
        }
    }
    repositories {
        maven(layout.projectDirectory.dir("../build/maven-staging")) { name = "Staging" }
    }
}

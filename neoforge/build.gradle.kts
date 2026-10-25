import org.gradle.kotlin.dsl.kotlin

plugins {
    `multiloader-loader`
    id("net.neoforged.moddev")
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.3.12"
    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.13"
    `multiloader-arclight`
}

neoForge {
    enable {
        version = commonMod.dep("neoforge")
    }
}

java {
    withSourcesJar()
}

publishAtlantis("atlantis-neoforge", jar = "jar", sources = "sourcesJar")

dependencies {
    compileOnly("pixelmon:Pixelmon-${commonMod.mc}-${commonMod.pixelmon}-universal:${commonMod.pixelmon}")

    listOf(
        "com.github.ben-manes.caffeine:caffeine:3.2.4",
        "io.github.classgraph:classgraph:4.8.197",
        "com.zaxxer:HikariCP:4.0.3",
        "com.mysql:mysql-connector-j:8.1.0",
        "com.google.protobuf:protobuf-java:3.19.4",
        "io.lettuce:lettuce-core:6.2.3.RELEASE",
        "io.projectreactor:reactor-core:3.4.27",
        "org.reactivestreams:reactive-streams:1.0.4",
    ).forEach { library ->
        val dependency = implementation(library) {
            exclude(group = "io.netty")
            exclude(group = "org.slf4j")
        }
        jarJar(dependency!!)
        "additionalRuntimeClasspath"(library) {
            exclude(group = "io.netty")
            exclude(group = "org.slf4j")
        }
    }
}

val gametest: SourceSet = sourceSets.create("gametest") {
    val main = sourceSets.main.get()
    compileClasspath += main.compileClasspath + main.output
    runtimeClasspath += main.runtimeClasspath + main.output
}

neoForge {
    accessTransformers.from(rootProject.file("common/src/main/resources/META-INF/accesstransformer.cfg"))
    interfaceInjectionData.from(rootProject.file("common/interfaces.json"))

    runs {
        register("client") {
            client()
            ideName = "NeoForge Client (${project.path})"
        }
        register("server") {
            server()
            ideName = "NeoForge Server (${project.path})"
        }
        register("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gametest
            gameDirectory = project.file("build/run/gameTest")
            systemProperty("neoforge.enabledGameTestNamespaces", "atlantis_test")
            ideName = "NeoForge GameTest Server (${project.path})"
        }
    }

    parchment {
        commonMod.depOrNull("parchment")?.let {
            mappingsVersion = it
            minecraftVersion = commonMod.mc
        }
    }

    mods {
        register(commonMod.id) {
            sourceSet(sourceSets.main.get())
        }
        register("atlantis_test") {
            sourceSet(gametest)
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

tasks.matching { it.name == "runGameTestServer" }.configureEach {
    val runDir = layout.buildDirectory.dir("run/gameTest")
    doFirst {
        runDir.get().asFile.let { dir -> listOf("config", "world", "ops.json").forEach { dir.resolve(it).deleteRecursively() } }
    }
}

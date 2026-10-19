import org.gradle.kotlin.dsl.kotlin

plugins {
    `multiloader-loader`
    id("net.neoforged.moddev")
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.13"
}

neoForge {
    enable {
        version = commonMod.dep("neoforge")
    }
}

dependencies {
    interfaceInjectionData("pixelmon:Pixelmon-${commonMod.mc}-${commonMod.pixelmon}-universal-interfaceinjection:${commonMod.pixelmon}@json")

    listOf(
        "com.github.ben-manes.caffeine:caffeine:3.2.4",
        "io.github.classgraph:classgraph:4.8.197",
        "com.zaxxer:HikariCP:4.0.3",
        "mysql:mysql-connector-java:8.0.29",
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
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

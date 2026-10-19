import org.gradle.kotlin.dsl.kotlin

plugins {
    `multiloader-loader`
    id("fabric-loom")
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.14"
}

stonecutter {

}

dependencies {
    minecraft("com.mojang:minecraft:${commonMod.mc}")
    mappings(loom.layered {
        officialMojangMappings()
        commonMod.depOrNull("parchment")?.let { parchmentVersion ->
            parchment("org.parchmentmc.data:parchment-${commonMod.mc}:$parchmentVersion@zip")
        }
    })

    modImplementation("net.fabricmc:fabric-loader:${commonMod.dep("fabric_loader")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${commonMod.dep("fabric_api")}+${commonMod.mc}")

    // Required dependencies
    modImplementation("com.terraformersmc:modmenu:${commonMod.dep("modmenu")}")
    modImplementation(include("me.lucko:fabric-permissions-api:0.3.1")!!)

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
        include(implementation(library) {
            isTransitive = false
        }!!)
    }
}

loom {
    mixin {
        useLegacyMixinAp = false
    }
    accessWidenerPath = rootProject.file("common/src/main/resources/atlantis.accesswidener")

    interfaceInjection {
        enableDependencyInterfaceInjection = true
    }

    mods {
        register(commonMod.id) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        getByName("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
        }
        getByName("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
        }
    }
}

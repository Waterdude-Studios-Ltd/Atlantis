import org.gradle.kotlin.dsl.kotlin

plugins {
    `multiloader-loader`
    id("fabric-loom")
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.3.12"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.14"
    `multiloader-arclight`
}

stonecutter {

}

java {
    withSourcesJar()
}

publishAtlantis("atlantis-fabric", sources = "remapSourcesJar", bundled = "include")

dependencies {
    minecraft("com.mojang:minecraft:${commonMod.mc}")
    mappings(loom.layered {
        officialMojangMappings()
        commonMod.depOrNull("parchment")?.let { parchmentVersion ->
            parchment("org.parchmentmc.data:parchment-${commonMod.mc}:$parchmentVersion@zip")
        }
    })

    modImplementation("net.fabricmc:fabric-loader:${commonMod.dep("fabric_loader")}")
    testImplementation("net.fabricmc:fabric-loader-junit:${commonMod.dep("fabric_loader")}")
    testCompileOnly("org.jspecify:jspecify:1.0.0")
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
        "net.fabricmc:tiny-remapper:0.11.1",
        "net.fabricmc:mapping-io:0.7.1",
    ).forEach { library ->
        include(implementation(library) {
            isTransitive = false
        }!!)
    }
}

sourceSets.test {
    val commonTests = rootProject.file("common/src/test/java")
    java.srcDir(commonTests)
    java.exclude { !it.isDirectory && it.file.startsWith(commonTests) && !it.file.startsWith(commonTests.resolve("de/waterdu/atlantis/test")) }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "atlantis_test"
        enableGameTests = true
        enableClientGameTests = false
        eula = true
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

afterEvaluate {
    tasks.test {
        setDependsOn(dependsOn.filterNot { dependency ->
            when (dependency) {
                is Task -> dependency.name == "runGameTest"
                is TaskProvider<*> -> dependency.name == "runGameTest"
                is String -> dependency.endsWith("runGameTest")
                else -> false
            }
        })
    }
}

tasks.matching { it.name == "runGameTest" }.configureEach {
    val runDir = layout.buildDirectory.dir("run/gameTest")
    doFirst {
        runDir.get().asFile.let { dir -> listOf("config", "world", "ops.json").forEach { dir.resolve(it).deleteRecursively() } }
    }
}

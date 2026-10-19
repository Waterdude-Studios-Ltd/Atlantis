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
}

neoForge {
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
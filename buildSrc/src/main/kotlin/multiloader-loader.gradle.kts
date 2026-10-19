plugins {
    id("java")
    id("idea")
    id("multiloader-common")
}

val commonJava: Configuration by configurations.creating {
    isCanBeResolved = true
}
val commonResources: Configuration by configurations.creating {
    isCanBeResolved = true
}

val is_atlantis = commonMod.id == "atlantis"

fun localOptionalJar(name: String): File? = listOfNotNull(
    rootProject.file("libs/$name"),
    file("${System.getProperty("user.home")}/Code/Libraries/$name"),
    System.getenv("LIBRARIES_DIR")?.let { file("$it/$name") }
).firstOrNull { it.isFile }

fun DependencyHandlerScope.optionalIntegration(
    version: String,
    dependency: (String) -> String,
    localJar: String
) {
    version.takeIf { it.isNotBlank() }?.let { "compileOnly"(dependency(it)) }
        ?: localOptionalJar(localJar)?.let { "compileOnly"(files(it)) }
}

dependencies {
    val commonPath = common.hierarchy.toString()
    compileOnly(project(path = commonPath))
    commonJava(project(path = commonPath, configuration = "commonJava"))
    commonResources(project(path = commonPath, configuration = "commonResources"))

    "compileOnly"("org.spongepowered:mixin:0.8.5")
    "compileOnly"("org.jspecify:jspecify:1.0.0")
    "annotationProcessor"("org.jspecify:jspecify:1.0.0")

    "compileOnly"("net.fabricmc:fabric-loader:${commonMod.dep("fabric_loader")}")
    "compileOnly"("net.fabricmc.fabric-api:fabric-api:${commonMod.dep("fabric_api")}+${commonMod.mc}")
    "compileOnly"("net.neoforged:neoforge:${commonMod.dep("neoforge")}:universal")
    "compileOnly"("net.neoforged.fancymodloader:loader:4.0.35")
    "compileOnly"("net.neoforged.fancymodloader:earlydisplay:4.0.35")
    "compileOnly"("net.neoforged:bus:8.0.2")

    if (is_atlantis) {
        "compileOnly"("io.netty:netty-all:4.1.86.Final")
        "compileOnly"("com.zaxxer:HikariCP:4.0.3")
        "compileOnly"("mysql:mysql-connector-java:8.0.29")
        "compileOnly"("io.lettuce:lettuce-core:6.2.3.RELEASE")
        "compileOnly"("com.github.ben-manes.caffeine:caffeine:3.2.4")
        "compileOnly"("org.spigotmc:spigot-api:${commonMod.mc}-R0.1-SNAPSHOT")
    }

    "compileOnly"("pixelmon:Pixelmon-${commonMod.mc}-${commonMod.pixelmon}-universal:${commonMod.pixelmon}")
    "compileOnly"("pixelmon:Pixelmon-${commonMod.mc}-${commonMod.pixelmon}-universal-interfaceinjection:${commonMod.pixelmon}@json")

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

tasks {
    compileJava {
        dependsOn(commonJava)
        source(commonJava)
    }

    processResources {
        dependsOn(commonResources)
        from(commonResources) {
            exclude("fabric.mod.json")
        }
    }
}

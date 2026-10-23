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

val commonGametestJava: Configuration = configurations.create("commonGametestJava") {
    isCanBeResolved = true
}
val commonGametestResources: Configuration = configurations.create("commonGametestResources") {
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
    dependencies: (String) -> List<String>,
    localJar: String,
    transitive: Boolean = true
) {
    version.takeIf { it.isNotBlank() }?.let { dependencies(it).forEach { dependency -> "compileOnly"(dependency) { isTransitive = transitive } } }
        ?: localOptionalJar(localJar)?.let { "compileOnly"(files(it)) }
}

dependencies {
    val commonPath = common.hierarchy.toString()
    compileOnly(project(path = commonPath))
    commonJava(project(path = commonPath, configuration = "commonJava"))
    commonResources(project(path = commonPath, configuration = "commonResources"))
    commonGametestJava(project(path = commonPath, configuration = "commonGametestJava"))
    commonGametestResources(project(path = commonPath, configuration = "commonGametestResources"))

    "compileOnly"("org.jspecify:jspecify:1.0.0")
    "annotationProcessor"("org.jspecify:jspecify:1.0.0")
    "compileOnly"("io.github.llamalad7:mixinextras-common:0.4.1")

    if (is_atlantis) {
        "compileOnly"("io.netty:netty-all:4.1.86.Final")
        "compileOnly"("com.zaxxer:HikariCP:4.0.3")
        "compileOnly"("mysql:mysql-connector-java:8.0.29")
        "compileOnly"("io.lettuce:lettuce-core:6.2.3.RELEASE")
        "compileOnly"("com.github.ben-manes.caffeine:caffeine:3.2.4")
        "compileOnly"("io.github.classgraph:classgraph:4.8.197")
        "compileOnly"("org.spigotmc:spigot-api:${commonMod.mc}-R0.1-SNAPSHOT")
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

sourceSets.matching { it.name == "gametest" }.configureEach {
    java.srcDir(commonGametestJava)
    resources.srcDir(commonGametestResources)
}

tasks.withType<ProcessResources>().matching { it.name != "processResources" }.configureEach {
    val sourceSetName = name.removePrefix("process").removeSuffix("Resources").replaceFirstChar { it.lowercase() }
    val mainResources = layout.buildDirectory.dir("resources/main").get().asFile
    val generatedKsp = layout.buildDirectory.dir("generated/ksp").get().asFile
    val ownKsp = generatedKsp.resolve(sourceSetName)
    exclude { it.file.startsWith(mainResources) || (it.file.startsWith(generatedKsp) && !it.file.startsWith(ownKsp)) }
    mustRunAfter(tasks.matching { it.name.startsWith("ksp") })
}

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1"
val minecraft = providers.gradleProperty("publish.minecraft").getOrElse("1.21.1")

val cleanMavenStaging = tasks.register<Delete>("cleanMavenStaging") {
    delete(layout.buildDirectory.dir(MAVEN_STAGING))
}

val stageTineGradle = tasks.register<GradleBuild>("stageTineGradle") {
    dir = file("tine-gradle")
    tasks = listOf("publishAllPublicationsToStagingRepository")
    mustRunAfter(cleanMavenStaging)
}

fun SftpTask.connection() {
    fun setting(key: String) = providers.gradleProperty("waterdude.sftp.$key")
    host = setting("host")
    port = setting("port").orElse("22").map(String::toInt)
    user = setting("user")
    password = setting("password")
    dryRun = providers.gradleProperty("dryRun").map { it != "false" }.orElse(false)
}

val mavenIndex = layout.buildDirectory.file("maven-index/index.html")

tasks.register<UploadMaven>("publishToMavenRemote") {
    group = "publishing"
    description = "Publishes Atlantis, its loader jars and the Tine Gradle plugin to the Waterdude maven over SFTP."
    dependsOn(cleanMavenStaging, stageTineGradle)
    listOf("common", "fabric", "neoforge").forEach { dependsOn(":$it:$minecraft:publishAllPublicationsToStagingRepository") }

    connection()
    staging = layout.buildDirectory.dir(MAVEN_STAGING)
    schema = layout.projectDirectory.file("common/src/main/resources/schemas/tine.schema.json")
    mavenPath = providers.gradleProperty("waterdude.sftp.mavenPath")
    schemaPath = providers.gradleProperty("waterdude.sftp.schemaPath")
    repositoryUrl = providers.gradleProperty("waterdude.maven.url")
    indexOutput = mavenIndex
    force = providers.gradleProperty("force").map { it != "false" }.orElse(false)
}

tasks.register<UpdateMavenIndex>("updateMavenIndex") {
    group = "publishing"
    description = "Regenerates the index page of the Waterdude maven from the artifacts on the server."

    connection()
    mavenPath = providers.gradleProperty("waterdude.sftp.mavenPath")
    repositoryUrl = providers.gradleProperty("waterdude.maven.url")
    output = mavenIndex
}

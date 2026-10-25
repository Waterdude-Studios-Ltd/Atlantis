import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Uploads a staged maven repository. Remote `maven-metadata.xml` files are merged with the staged ones so existing
 * versions stay listed, and released versions that are already on the server are only overwritten with [force].
 * The repository's index page is regenerated afterwards. With [dryRun], the server is read but nothing is written.
 */
@DisableCachingByDefault(because = "Uploads to a remote server")
abstract class UploadMaven : SftpTask() {

    @get:InputDirectory
    abstract val staging: DirectoryProperty

    @get:InputFile
    abstract val schema: RegularFileProperty

    @get:Input
    abstract val mavenPath: Property<String>

    @get:Input
    abstract val schemaPath: Property<String>

    @get:Input
    abstract val force: Property<Boolean>

    @get:Input
    abstract val repositoryUrl: Property<String>

    @get:Internal
    abstract val indexOutput: RegularFileProperty

    @TaskAction
    fun upload() {
        val root = staging.get().asFile
        val artifacts = root.walkTopDown().filter { it.name == METADATA }.map { it.parentFile }.sorted().toList()
        if (artifacts.isEmpty()) {
            throw GradleException("Nothing staged in $root")
        }

        withSftp { sftp ->
            val uploads = mutableListOf<Pair<File, String>>()
            val conflicts = mutableListOf<String>()
            for (local in artifacts) {
                val relative = local.relativeTo(root).invariantSeparatorsPath
                val remote = "${mavenPath.get().trimEnd('/')}/$relative"
                val staged = XmlParser().parse(local.resolve(METADATA))
                val group = staged.text("groupId")!!
                val artifact = staged.text("artifactId")!!
                val new = staged.versions()
                val existing = sftp.read("$remote/$METADATA")?.let { XmlParser().parseText(it).versions() } ?: emptyList()

                conflicts += new.filter { it in existing && !it.endsWith("-SNAPSHOT") }.map { "$group:$artifact:$it" }
                writeMetadata(local.resolve(METADATA), group, artifact, existing + new.filterNot { it in existing }, new.last())

                for (version in new) {
                    local.resolve(version).listFiles()!!.sorted().forEach { uploads += it to "$remote/$version/${it.name}" }
                }
                local.listFiles { file -> file.name.startsWith(METADATA) }!!.sorted().forEach { uploads += it to "$remote/${it.name}" }
            }

            if (conflicts.isNotEmpty() && !force.get()) {
                throw GradleException("Already published, bump the version or pass -Pforce:\n  ${conflicts.joinToString("\n  ")}")
            }

            val schemaFile = schema.get().asFile
            uploads += schemaFile to "${schemaPath.get().trimEnd('/')}/${schemaFile.name}"

            for ((file, target) in uploads) {
                if (dryRun.get()) {
                    logger.lifecycle("Would upload ${file.relativeToOrSelf(root)} -> $target")
                    continue
                }
                sftp.mkdirs(target.substringBeforeLast('/'))
                sftp.put(file.path, target)
                logger.info("Uploaded $target")
            }
            logger.lifecycle("${if (dryRun.get()) "Would upload" else "Uploaded"} ${uploads.size} file(s) for ${artifacts.size} artifact(s) to ${host.get()}")

            val remote = RepositoryView.Sftp(sftp, mavenPath.get())
            val view = if (dryRun.get()) RepositoryView.Overlay(RepositoryView.Local(root), remote) else remote
            MavenIndex.update(sftp, mavenPath.get(), view, repositoryUrl.get(), indexOutput.get().asFile, dryRun.get(), logger)
        }
    }

    private fun writeMetadata(file: File, group: String, artifact: String, versions: List<String>, latest: String) {
        val release = if (latest.endsWith("-SNAPSHOT")) "" else "\n    <release>$latest</release>"
        val listed = versions.joinToString("\n") { "      <version>${XmlUtil.escapeXml(it)}</version>" }
        val updated = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        file.writeText(
            """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<metadata>
            |  <groupId>$group</groupId>
            |  <artifactId>$artifact</artifactId>
            |  <versioning>
            |    <latest>$latest</latest>$release
            |    <versions>
            |$listed
            |    </versions>
            |    <lastUpdated>$updated</lastUpdated>
            |  </versioning>
            |</metadata>
            |""".trimMargin()
        )
        val bytes = file.readBytes()
        CHECKSUMS.forEach { (extension, algorithm) ->
            file.resolveSibling("${file.name}.$extension").writeText(MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(it) })
        }
    }

    private companion object {
        const val METADATA = "maven-metadata.xml"
        val CHECKSUMS = mapOf("md5" to "MD5", "sha1" to "SHA-1", "sha256" to "SHA-256", "sha512" to "SHA-512")
    }
}

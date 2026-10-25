import com.jcraft.jsch.ChannelSftp
import groovy.json.JsonOutput
import groovy.xml.XmlParser
import org.gradle.api.logging.Logger
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Generates the `index.html` of a maven repository from what is on the server: every artifact with its version
 * history and copyable dependency snippets. Gradle plugin markers are shown as plugins, in place of the artifact
 * they point to.
 */
object MavenIndex {

    private const val METADATA = "maven-metadata.xml"
    private const val INDEX = "index.html"
    private const val TEMPLATE = "maven-index.html"
    private const val DATA_PLACEHOLDER = "__MAVEN_INDEX_DATA__"
    private const val PLUGIN_MARKER = ".gradle.plugin"
    private val CHECKSUM = Regex("\\.(md5|sha1|sha256|sha512|asc)$")
    private val DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)

    fun update(sftp: ChannelSftp, root: String, view: RepositoryView, repositoryUrl: String, output: File, dryRun: Boolean, logger: Logger) {
        val (html, count) = render(view, repositoryUrl)
        output.parentFile.mkdirs()
        output.writeText(html)
        if (dryRun) {
            logger.lifecycle("Would upload the index of $count artifact(s), preview at ${output.toURI()}")
        } else {
            sftp.put(output.path, "${root.trimEnd('/')}/$INDEX")
            logger.lifecycle("Updated the index of $count artifact(s)")
        }
    }

    fun render(view: RepositoryView, repositoryUrl: String): Pair<String, Int> {
        val described = crawl(view, "").map { describe(view, it) }
        val byCoordinate = described.associateBy { "${it["group"]}:${it["artifact"]}" }
        val artifacts = described.map { artifact ->
            val implementation = byCoordinate[artifact["implementation"]] ?: return@map artifact
            artifact + mapOf("name" to implementation["name"], "description" to (implementation["description"] ?: artifact["description"]))
        }
        val hidden = artifacts.mapNotNull { it["implementation"] as String? }.toSet()
        val listed = artifacts
            .filter { "${it["group"]}:${it["artifact"]}" !in hidden && (it["versions"] as List<*>).isNotEmpty() }
            .sortedWith(
                compareBy<Map<String, Any?>> { it["plugin"] == null }
                    .thenByDescending { ((it["versions"] as List<*>).first() as Map<*, *>)["date"] as String }
                    .thenBy { it["group"] as String }
                    .thenBy { it["artifact"] as String }
            )

        val data = mapOf(
            "repository" to repositoryUrl.trimEnd('/'),
            "generated" to DATE.format(Instant.now()),
            "artifacts" to listed
        )
        val template = MavenIndex::class.java.getResourceAsStream("/$TEMPLATE")!!.use { it.readBytes().decodeToString() }
        return template.replace(DATA_PLACEHOLDER, JsonOutput.toJson(data).replace("</", "<\\/")) to listed.size
    }

    private fun crawl(view: RepositoryView, directory: String): List<String> {
        val entries = view.list(directory)
        if (entries.any { it.name == METADATA }) {
            return listOf(directory)
        }
        return entries
            .filter { it.directory && !it.name.startsWith(".") }
            .sortedBy { it.name }
            .flatMap { crawl(view, if (directory.isEmpty()) it.name else "$directory/${it.name}") }
    }

    private fun describe(view: RepositoryView, directory: String): Map<String, Any?> {
        val metadata = XmlParser().parseText(view.read("$directory/$METADATA")!!)
        val group = metadata.text("groupId")!!
        val artifact = metadata.text("artifactId")!!
        val listed = metadata.versions()
        val folders = view.list(directory).filter { it.directory }.map { it.name }

        val versions = (listed + folders.filterNot { it in listed }).mapNotNull { version ->
            val files = view.list("$directory/$version").filter { !it.directory && !CHECKSUM.containsMatchIn(it.name) }
            val pom = files.firstOrNull { it.name.endsWith(".pom") } ?: return@mapNotNull null
            mapOf(
                "version" to version,
                "date" to DATE.format(Instant.ofEpochSecond(pom.modified)),
                "files" to files.map { it.name }.sortedWith(compareBy({ !it.endsWith(".jar") || it.contains("-sources") }, { it }))
            )
        }.sortedWith { a, b -> compareVersions(b["version"] as String, a["version"] as String) }

        val latest = metadata.elements("release").firstOrNull()?.text()
            ?: metadata.elements("latest").firstOrNull()?.text()
            ?: versions.firstOrNull()?.get("version") as String?
        val pom = latest?.let { version -> view.read("$directory/$version/$artifact-$version.pom") }?.let { XmlParser().parseText(it) }
        val isMarker = artifact.endsWith(PLUGIN_MARKER) && artifact.removeSuffix(PLUGIN_MARKER) == group
        val implementation = if (isMarker) {
            pom?.elements("dependency")?.firstOrNull()?.let { "${it.text("groupId")}:${it.text("artifactId")}" }
        } else {
            null
        }

        return mapOf(
            "group" to group,
            "artifact" to artifact,
            "name" to (pom?.text("name") ?: if (isMarker) group else artifact),
            "description" to pom?.text("description"),
            "plugin" to if (isMarker) group else null,
            "implementation" to implementation,
            "latest" to (latest?.takeIf { version -> versions.any { it["version"] == version } } ?: versions.firstOrNull()?.get("version")),
            "versions" to versions
        )
    }

    private fun compareVersions(a: String, b: String): Int {
        val tokens = Regex("\\d+|[A-Za-z]+")
        val left = tokens.findAll(a).map { it.value }.toList()
        val right = tokens.findAll(b).map { it.value }.toList()
        for (i in 0 until maxOf(left.size, right.size)) {
            val x = left.getOrNull(i) ?: return if (right[i].first().isDigit()) -1 else 1
            val y = right.getOrNull(i) ?: return if (x.first().isDigit()) 1 else -1
            val result = when {
                x.first().isDigit() && y.first().isDigit() -> x.toBigInteger().compareTo(y.toBigInteger())
                x.first().isDigit() -> 1
                y.first().isDigit() -> -1
                else -> x.compareTo(y, ignoreCase = true)
            }
            if (result != 0) {
                return result
            }
        }
        return a.compareTo(b)
    }
}

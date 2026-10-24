import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class DownloadFile : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val destination: RegularFileProperty

    @TaskAction
    fun download() {
        val target = destination.get().asFile.toPath()
        val partial = target.resolveSibling("${target.fileName}.part")
        Files.createDirectories(target.parent)
        URI(url.get()).toURL().openStream().use { input -> Files.copy(input, partial, StandardCopyOption.REPLACE_EXISTING) }
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING)
    }
}

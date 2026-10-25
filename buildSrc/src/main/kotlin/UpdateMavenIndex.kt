import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Reads and writes a remote server")
abstract class UpdateMavenIndex : SftpTask() {

    @get:Input
    abstract val mavenPath: Property<String>

    @get:Input
    abstract val repositoryUrl: Property<String>

    @get:Internal
    abstract val output: RegularFileProperty

    @TaskAction
    fun update() = withSftp { sftp ->
        MavenIndex.update(sftp, mavenPath.get(), RepositoryView.Sftp(sftp, mavenPath.get()), repositoryUrl.get(), output.get().asFile, dryRun.get(), logger)
    }
}

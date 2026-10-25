import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.SftpException
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File

/**
 * Base for tasks that work on the Waterdude maven over SFTP with password authentication. The server's host key must
 * already be in the global 'known_hosts' file.
 */
abstract class SftpTask : DefaultTask() {

    @get:Input
    abstract val host: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val user: Property<String>

    @get:Internal
    abstract val password: Property<String>

    @get:Input
    abstract val dryRun: Property<Boolean>

    protected fun <T> withSftp(action: (ChannelSftp) -> T): T {
        val session = JSch().apply {
            setKnownHosts(File(System.getProperty("user.home"), ".ssh/known_hosts").path)
        }.getSession(user.get(), host.get(), port.get()).apply {
            setPassword(password.get())
            setConfig("StrictHostKeyChecking", "yes")
            setConfig("PreferredAuthentications", "password")
            connect(TIMEOUT)
        }
        val sftp = session.openChannel("sftp") as ChannelSftp
        sftp.connect(TIMEOUT)
        try {
            return action(sftp)
        } finally {
            sftp.disconnect()
            session.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT = 20_000
    }
}

fun ChannelSftp.read(path: String): String? = try {
    get(path).use { it.readBytes().decodeToString() }
} catch (e: SftpException) {
    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) null else throw e
}

fun ChannelSftp.mkdirs(path: String) {
    var current = if (path.startsWith("/")) "" else "."
    for (part in path.split('/').filter { it.isNotEmpty() }) {
        current = "$current/$part"
        try {
            stat(current)
        } catch (e: SftpException) {
            if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) throw e
            mkdir(current)
        }
    }
}

fun ChannelSftp.list(path: String): List<ChannelSftp.LsEntry> = try {
    ls(path).filterIsInstance<ChannelSftp.LsEntry>().filter { it.filename != "." && it.filename != ".." }
} catch (e: SftpException) {
    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) emptyList() else throw e
}

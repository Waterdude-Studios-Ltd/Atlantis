import com.jcraft.jsch.ChannelSftp
import java.io.File

/** Read access to a maven repository, with paths relative to its root. */
interface RepositoryView {

    data class Entry(val name: String, val directory: Boolean, val modified: Long)

    fun list(path: String): List<Entry>

    fun read(path: String): String?

    class Sftp(private val sftp: ChannelSftp, root: String) : RepositoryView {
        private val root = root.trimEnd('/')

        override fun list(path: String) = sftp.list(resolve(path)).map { Entry(it.filename, it.attrs.isDir, it.attrs.mTime.toLong()) }

        override fun read(path: String) = sftp.read(resolve(path))

        private fun resolve(path: String) = if (path.isEmpty()) root else "$root/$path"
    }

    class Local(private val root: File) : RepositoryView {
        override fun list(path: String) = root.resolve(path).listFiles().orEmpty().map { Entry(it.name, it.isDirectory, it.lastModified() / 1000) }

        override fun read(path: String) = root.resolve(path).takeIf { it.isFile }?.readText()
    }

    /** Shows [top] over [bottom], as the repository will look once [top] is uploaded. */
    class Overlay(private val top: RepositoryView, private val bottom: RepositoryView) : RepositoryView {
        override fun list(path: String): List<Entry> {
            val upper = top.list(path)
            val names = upper.map { it.name }.toSet()
            return upper + bottom.list(path).filterNot { it.name in names }
        }

        override fun read(path: String) = top.read(path) ?: bottom.read(path)
    }
}

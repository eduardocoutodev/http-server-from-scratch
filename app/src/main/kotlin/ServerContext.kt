import java.nio.file.Path
import java.nio.file.Paths

object ServerContext {
    var filesDirectory: Path = Paths.get("").toAbsolutePath()

    fun registerServerContext(args: Array<String>) {
        if (args.size == 2 && args[0] == FILES_DIRECTORY_FLAG) {
            println("Overriding files directory")
            val directoryToOverride = Paths.get(args[1])
            filesDirectory = directoryToOverride.toAbsolutePath()
        }
    }
}

package mason

import cats.effect.*
import cats.syntax.all.*
import fs2.Pipe
import fs2.Stream
import fs2.io.readOutputStream
import fs2.io.writeOutputStream
import io.github.iltotore.iron.*
import java.nio.file.StandardOpenOption.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import pillars.Pillars

object generator:
    case class FileEntry(name: FileName, data: Stream[IO, Byte]):
        def asZipEntry: ZipEntry = new ZipEntry(name)
    end FileEntry

    def zipPipe(chunkSize: Int = 4096): Pipe[IO, FileEntry, Byte] = (files: Stream[IO, FileEntry]) =>
        readOutputStream[IO](chunkSize): outputStream =>
            Resource.fromAutoCloseable(IO(new ZipOutputStream(outputStream))).use: zipOut =>
                val writeOutput = writeOutputStream[IO](zipOut.pure[IO], closeAfterUse = false)
                files
                    .evalMap: (archive: FileEntry) =>
                        for
                            _ <- IO(zipOut.putNextEntry(archive.asZipEntry))
                            _ <- archive.data.through(writeOutput).compile.drain
                            _ <- IO(zipOut.closeEntry())
                        yield ()
                    .compile.drain

    def generateFiles(project: Project)(using Pillars[IO]): Stream[IO, FileEntry] =
        val srcDir = s"src/main/scala/${project.packagePath}"
        Stream.emits(
          Map(
            ".gitignore"           -> txt.gitignore(),
            "build.sbt"            -> txt.buildsbt(project),
            "project/plugins.sbt"  -> txt.pluginssbt(),
            s"$srcDir/Main.scala"  -> txt.Main(project),
            s"$srcDir/codec.scala" -> txt.codec(project),
            s"$srcDir/model.scala" -> txt.model(project)
          )
              .map: (name, content) =>
                  FileEntry(name.assume, Stream.emit(content.body).through(fs2.text.utf8.encode))
              .toSeq
        )
    end generateFiles
end generator

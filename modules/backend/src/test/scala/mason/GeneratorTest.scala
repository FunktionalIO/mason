package mason

import cats.effect.IO
import fs2.io.file.{Files, Path}
import io.github.iltotore.iron.*
import mason.generator.{FileEntry, zipPipe}
import munit.CatsEffectSuite

class GeneratorTest extends CatsEffectSuite:
    test("Generate a ZIP file"):
        val files = fs2.Stream(
          FileEntry("file1.txt", fs2.Stream.emits("Content of file 1".getBytes).covary[IO]),
          FileEntry("file2.txt", fs2.Stream.emits("Content of file 2".getBytes).covary[IO]),
          FileEntry("file3.txt", fs2.Stream.emits("Another file with more content".getBytes).covary[IO])
        )

        val zipStream = files.through(zipPipe())

        val path: Path = Path("test.zip")
        // Write to a temporary file
        for
            _    <- Files[IO].createFile(path)
            // Write the ZIP to the temp file
            _    <- zipStream.through(Files[IO].writeAll(path)).compile.drain
            // Get the file size
            size <- Files[IO].size(path)
            // Log the size
            _    <- IO.println(s"Generated ZIP size: $size bytes")
        // Stream the file (e.g., serve to a user)
        //        _    <- Files[IO].readAll(path, chunkSize = 4096)
        //                    .through(fs2.text.utf8.decode)
        //                    .evalMap(IO.println) // Here you would write the stream to a user-facing output
        //                    .compile
        //                    .drain
        yield ()
        end for
end GeneratorTest

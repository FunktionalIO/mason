package mason

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.Files
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import java.nio.file.StandardOpenOption.*
import pillars.Controller
import pillars.Controller.HttpEndpoint
import pillars.Pillars
import pillars.logger

val versionsController: Controller[IO] =
    def versions: HttpEndpoint[IO] = endpoints.v0.versions.serverLogicSuccess: _ =>
        List(
          Version("0.3.20", "latest")
        ).pure[IO]
    List(versions)
end versionsController

def downloadController(using p: Pillars[IO]): Controller[IO] =
    def download: HttpEndpoint[IO] = endpoints.v0.download[IO].serverLogicSuccess: project =>
        for
            _    <- logger.info(s"Downloading project ${project.name}...")
            path <- Files[IO].createTempFile
            _    <- generator.generateFiles(project)
                        .through(generator.zipPipe())
                        .through(Files[IO].writeAll(path))
                        .compile
                        .drain
            size <- Files[IO].size(path)
        yield ProjectContents(Files[IO].readAll(path), (project.name + ".zip").assume, size.assume)
        end for
    end download

    List(download)
end downloadController

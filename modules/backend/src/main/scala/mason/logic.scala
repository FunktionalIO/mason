package mason

import cats.effect.IO
import cats.syntax.all.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import pillars.Controller
import pillars.Controller.HttpEndpoint

val versionsController: Controller[IO] =
    def versions: HttpEndpoint[IO] = ApiEndpoints.versions.serverLogicSuccess: _ =>
        List(
          Version("0.3.20", "latest")
        ).pure[IO]
    List(versions)
end versionsController

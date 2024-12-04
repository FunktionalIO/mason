package mason

import cats.effect.IO
import mason.build.BuildInfo
import pillars.server
import pillars.AppInfo
import pillars.Run
import pillars.logger

object Main extends pillars.EntryPoint:
    override def app: pillars.App[IO] = new pillars.App[IO]:
        override def infos: AppInfo = BuildInfo.toAppInfo

        override def run: Run[IO, IO[Unit]] =
            for
                _ <- logger.info("Starting Mason")
                _ <- server.start(versionsController)
            yield ()
end Main

package mason

import cats.effect.*
import mason.build.BuildInfo
import mason.versionsController
import mason.downloadController
import pillars.*
import pillars.flags.*

object Main extends pillars.IOApp(FeatureFlags):
    def infos: AppInfo = BuildInfo.toAppInfo

    def run: Run[IO, IO[Unit]] =
        for
            _ <- logger.info(s"🏛️ Welcome to ${config.name}!")
            _ <- server.start(versionsController, downloadController)
        yield ()
        end for
    end run
end Main

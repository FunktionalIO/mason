package mason

import cats.effect.*
import mason.build.BuildInfo
import pillars.AppInfo
import pillars.Pillars
import pillars.Run
import pillars.flags.*
import pillars.httpclient.HttpClient
import pillars.logger
import pillars.server

object Main extends pillars.IOApp(FeatureFlags, HttpClient):
    def infos: AppInfo = BuildInfo.toAppInfo

    def run: Run[IO, IO[Unit]] =
        val p = summon[Pillars[IO]]
        for
            _        <- logger.info(s"🏛️ Welcome to ${pillars.config.name}!")
            config   <- p.readConfig[Config]
            versions <- versions.service(config)
            _        <- server.start(versionsController(versions), downloadController)
        yield ()
        end for
    end run
end Main

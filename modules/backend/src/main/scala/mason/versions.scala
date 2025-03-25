package mason

import cats.effect.IO
import cats.effect.Ref
import io.circe.Codec
import io.circe.Json
import io.circe.optics.JsonPath.*
import io.circe.parser.*
import io.github.iltotore.iron.*
import org.http4s.EntityDecoder
import org.http4s.Headers
import org.http4s.Method
import org.http4s.Request
import org.http4s.Uri
import org.http4s.circe.jsonOf
import pillars.Run
import pillars.httpclient.http

object versions:
    trait Service:
        def getVersions: IO[List[Version]]

    private final case class Release(tag_name: String, name: String) derives Codec.AsObject

    def service(config: Config): Run[IO, IO[Service]] =

        given EntityDecoder[IO, List[Release]] = jsonOf[IO, List[Release]]

        def fetchReleases: IO[List[Version]] =
            import scala.math.Ordering.Implicits.*
            val request = Request[IO](
                method = Method.GET,
                uri =
                    Uri.unsafeFromString(s"https://api.github.com/repos/${config.owner}/${config.repository}/releases")
            ).withHeaders(
                "Accept" -> "application/vnd.github.v3+json"
            )
            for
                releases <- http.expect[List[Release]](request)
                versions  = releases
                                .map(r => Version(r.tag_name.tail.refineUnsafe, r.name.refineUnsafe))
                                .filter(_ >= config.minVersion)
            yield versions
            end for
        end fetchReleases

        for
            initialVersions <- fetchReleases
            cache           <- Ref.of[IO, List[Version]](initialVersions)
        yield new Service:
            def getVersions: IO[List[Version]] = cache.get
        end for
    end service
end versions

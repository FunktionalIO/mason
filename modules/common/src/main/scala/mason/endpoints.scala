package mason

import mason.codec.json.given
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.HeaderNames
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody

object endpoints:
    object v0:
        private val base = endpoint.in("api" / "v0")
        val versions: Endpoint[Unit, Unit, Failure, List[Version], Any] =
            base.get
                .in("versions")
                .name("versions")
                .description("Return the current version of the API")
                .out(jsonBody[List[Version]])
                .errorOut(jsonBody[Failure])

        private def zippedFileStream[F[_]] = streamBinaryBody(Fs2Streams[F])(CodecFormat.Zip())

        def download[F[_]]: Endpoint[Unit, Project, Failure, ProjectContents[F], Fs2Streams[F]] =
            base.post
                .in("download")
                .name("download")
                .description("Download project files")
                .in(jsonBody[Project])
                .out(zippedFileStream[F])
                .out(header(HeaderNames.AccessControlExposeHeaders, HeaderNames.ContentDisposition))
                .out(header[String](HeaderNames.ContentDisposition))
                .out(header[Long](HeaderNames.ContentLength))
                .mapOutTo[ProjectContents[F]]
                .errorOut(jsonBody[Failure])
    end v0
end endpoints

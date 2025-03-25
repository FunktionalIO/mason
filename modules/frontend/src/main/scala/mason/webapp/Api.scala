package mason.webapp

import com.raquo.airstream.core.EventStream
import io.circe.syntax.*
import mason.*
import mason.codec.json.given
import org.scalajs.dom
import org.scalajs.dom.Fetch
import org.scalajs.dom.Headers
import org.scalajs.dom.HttpMethod
import org.scalajs.dom.RequestInit
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.*
import sttp.tapir.DecodeResult
import sttp.tapir.client.sttp.SttpClientInterpreter

case class Api(config: Config):
    private val backend = FetchBackend()

    def fetchVersions: EventStream[List[Version]] =

        val request: Request[DecodeResult[Either[Failure, List[Version]]], Any] =
            SttpClientInterpreter()
                .toRequest(mason.endpoints.v0.versions, Some(config.backendUri))
                .apply(())

        val eventualVersions =
            backend.send(request).map: response =>
                response.body match
                    case DecodeResult.Value(Right(versions)) => versions
                    case _                                   => Nil
        EventStream.fromFuture(eventualVersions)
    end fetchVersions

    def download(project: Project): Unit =
        // can't use FetchBackend here because it doesn't support blob responses
        Fetch
            .fetch(
                config.backendUri.toString + mason.endpoints.v0.download.showPathTemplate(),
                new RequestInit:
                    method = HttpMethod.POST
                    headers = Headers(js.Dictionary(
                        "Content-Type" -> "application/json"
                    ))
                    body = project.asJson.noSpaces
            )
            .flatMap: response =>
                val fileName: String = if response.headers.has("ContentDisposition") then
                    response.headers.get("ContentDisposition").split("filename=")(1)
                else s"${project.name}.zip"
                val blob             = response.blob()
                blob.toFuture.map(blob => (blob, fileName))
            .foreach: (blob, fileName) =>
                // Create a temporary link for the file
                val url  = dom.URL.createObjectURL(blob)
                val link = dom.document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
                link.href = url
                link.download = fileName     // Replace with your desired filename
                dom.document.body.appendChild(link)
                link.click()
                dom.URL.revokeObjectURL(url) // Clean up the object URL
                dom.document.body.removeChild(link)
    end download

end Api

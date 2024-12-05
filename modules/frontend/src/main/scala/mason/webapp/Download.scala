package mason.webapp

import io.circe.syntax.*
import mason.Project
import mason.codec.json.given
import org.scalajs.dom
import org.scalajs.dom.Fetch
import org.scalajs.dom.Headers
import org.scalajs.dom.HttpMethod
import org.scalajs.dom.RequestInit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*

object Download:
    def download(apiUrl: String, project: Project): Unit =
        Fetch
            .fetch(
              apiUrl,
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
end Download

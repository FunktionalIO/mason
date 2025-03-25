package mason.webapp

import sttp.model.Uri

case class Config(
                     backendUri: Uri
)

object Config:
    val default: Either[Error, Config] =
        val envVar     = sys.env.getOrElse("MASON_BACKEND_URI", "http://localhost:8765")
        Uri.parse(envVar) match
            case Left(value)  => Left(Error.MalformedBackendUri(envVar))
            case Right(value) => Right(Config(backendUri = value))
    end default
end Config

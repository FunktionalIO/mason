package mason

import pillars.PillarsError
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody

object ApiEndpoints:
    import mason.codec.json.given
    val versions: Endpoint[Unit, Unit, PillarsError.View, List[Version], Any] =
        endpoint.get
            .in("versions")
            .name("versions")
            .description("Return the current version of the API")
            .out(jsonBody[List[Version]])
            .errorOut(jsonBody[PillarsError.View])
end ApiEndpoints


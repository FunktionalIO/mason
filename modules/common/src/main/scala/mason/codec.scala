package mason

object codec:
    object json:
        import io.circe.*
        import io.circe.generic.semiauto.deriveCodec
        import io.github.iltotore.iron.circe.given
        import sttp.tapir.Schema
        import sttp.tapir.codec.iron.given

        given Codec[Version]  = deriveCodec[Version]
        given Schema[Version] = Schema.derived[Version]

        given Codec[Project]  = deriveCodec[Project]
        given Schema[Project] = Schema.derived

        given Codec[Failure]  = deriveCodec[Failure]
        given Schema[Failure] = Schema.derived

        given Encoder[mason.Module] = Encoder.encodeString.contramap(_.name)
        given Decoder[Module]       =
            Decoder.decodeString.map(Module.fromString(_).getOrElse(throw DecodingFailure("Invalid module", Nil)))
        given Schema[Module]        = Schema.string
    end json
end codec

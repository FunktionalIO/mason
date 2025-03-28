package mason

import io.circe.Codec
import io.circe.derivation.Configuration
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import mason.Dependencies.Observability

case class Dependencies(
    scala: VersionNumber = "3.6.3",
    iron: VersionNumber = "2.6.0",
    tapir: VersionNumber = "1.11.20",
    munit: VersionNumber = "1.1.0",
    munitCE: VersionNumber = "2.0.0",
    doobie: VersionNumber = "1.0.0-RC8",
    observability: Observability = Observability()
)

object Dependencies:
    case class Observability(
        signoz: VersionNumber = "0.76.2",
        otelCollector: VersionNumber = "0.111.34",
        clickhouse: VersionNumber = "24.1.2",
        zookeeper: VersionNumber = "3.7.1"
    )
    given Configuration                     = Configuration.default.withKebabCaseMemberNames.withKebabCaseConstructorNames.withDefaults
    given Codec[Dependencies.Observability] = Codec.AsObject.derivedConfigured
    given Codec[Dependencies]               = Codec.AsObject.derivedConfigured
end Dependencies

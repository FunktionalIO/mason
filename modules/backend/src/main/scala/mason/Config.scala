package mason

import io.circe.Codec
import io.circe.derivation.Configuration
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import pillars.codec.given
import scala.concurrent.duration.*

final case class Config(
    owner: Owner = "FunktionalIO",
    repository: Repository = "pillars",
    refresh: FiniteDuration = 1.hour,
    minPillarsVersion: VersionNumber = "0.4.0",
    dependencies: Dependencies = Dependencies()
):
    def minVersion: Version = Version(minPillarsVersion, "Minimum Pillars Version")
end Config

object Config:
    given Configuration = Configuration.default.withKebabCaseMemberNames.withKebabCaseConstructorNames.withDefaults
    given Codec[Config] = Codec.AsObject.derivedConfigured

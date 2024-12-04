package mason

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

final case class Version(
    number: VersionNumber,
    name: VersionName
)

final case class Project(
    name: ProjectName,
    version: Version,
    modules: Set[Module]
)
type ProjectNameConstraint = Not[Blank] DescribedAs "Project name must not be blank"
type ProjectName           = String :| ProjectNameConstraint
object ProjectName extends RefinedTypeOps.Transparent[ProjectName]

type PackageNameConstraint = Not[Blank] DescribedAs "Project name must not be blank"
type PackageName           = String :| ProjectNameConstraint
object PackageName extends RefinedTypeOps.Transparent[PackageName]

type VersionNameConstraint = Not[Blank] DescribedAs "Version name must not be blank"
type VersionName           = String :| VersionNameConstraint
object VersionName extends RefinedTypeOps.Transparent[VersionName]

type VersionNumberConstraint = SemanticVersion DescribedAs "Version Number must be a valid semantic version"
type VersionNumber           = String :| VersionNumberConstraint
object VersionNumber extends RefinedTypeOps.Transparent[VersionNumber]

enum Module(val name: String):
    case DbDoobie     extends Module("db-doobie")
    case DbSkunk      extends Module("db-skunk")
    case DBMigration  extends Module("db-migration")
    case FeatureFlags extends Module("feature-flags")
    case HttpClient   extends Module("http-client")
    case Redis        extends Module("redis")
    case RabbitMQ     extends Module("rabbitmq")
end Module

object Module:
    def fromString(name: String): Option[Module] = name match
        case "db-doobie"     => Some(Module.DbDoobie)
        case "db-skunk"      => Some(Module.DbSkunk)
        case "db-migration"  => Some(Module.DBMigration)
        case "feature-flags" => Some(Module.FeatureFlags)
        case "http-client"   => Some(Module.HttpClient)
        case "redis"         => Some(Module.Redis)
        case "rabbitmq"      => Some(Module.RabbitMQ)
        case _               => None
    end fromString
end Module

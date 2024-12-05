package mason

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

final case class Version(
    number: VersionNumber,
    name: VersionName
)

final case class Project(
    name: ProjectName,
    packageName: PackageName,
    organizationName: OrganizationName,
    version: Version,
    modules: Set[Module],
    generateCI: Boolean,
    generateDocker: Boolean
):
    def packagePath: String = packageName.replace(".", "/")
    def hasModule(module: Module): Boolean = modules.contains(module)
end Project

type ProjectNameConstraint = Not[Blank] DescribedAs "Project name must not be blank"
type ProjectName           = String :| ProjectNameConstraint
object ProjectName extends RefinedTypeOps.Transparent[ProjectName]

type PackageNameConstraint = Not[Blank] DescribedAs "Package name must not be blank"
type PackageName           = String :| ProjectNameConstraint
object PackageName extends RefinedTypeOps.Transparent[PackageName]

type OrganizationNameConstraint = Not[Blank] DescribedAs "Organization name must not be blank"
type OrganizationName           = String :| ProjectNameConstraint
object OrganizationName extends RefinedTypeOps.Transparent[OrganizationName]

type VersionNameConstraint = Not[Blank] DescribedAs "Version name must not be blank"
type VersionName           = String :| VersionNameConstraint
object VersionName extends RefinedTypeOps.Transparent[VersionName]

type VersionNumberConstraint = SemanticVersion DescribedAs "Version Number must be a valid semantic version"
type VersionNumber           = String :| VersionNumberConstraint
object VersionNumber extends RefinedTypeOps.Transparent[VersionNumber]

type FileNameConstraint = Not[Blank] DescribedAs "File name must not be blank"
type FileName           = String :| FileNameConstraint
object FileName extends RefinedTypeOps.Transparent[FileName]

type FileSizeConstraint = Positive DescribedAs "File size must be greater than or equal to 0"
type FileSize           = Long :| FileSizeConstraint
object FileSize extends RefinedTypeOps.Transparent[FileSize]

enum Module(val name: String, val support: String, val function: String, val packageName: PackageName):
    case DbDoobie     extends Module("db-doobie", "DBDoobie", "db", "pillars.db")
    case DbSkunk      extends Module("db-skunk", "DB", "sessions", "pillars.db")
    case DBMigration  extends Module("db-migration", "DBMigration", "dbMigration", "pillars.db.migrations")
    case FeatureFlags extends Module("flags", "FeatureFlags", "flag", "pillars.flags")
    case HttpClient   extends Module("http-client", "HttpClient", "http", "pillars.httpclient")
    case Redis        extends Module("redis-rediculous", "Redis", "redis", "pillars.redis_rediculous")
    case RabbitMQ     extends Module("rabbitmq-fs2", "RabbitMQ", "rabbit", "pillars.rabbitmq.fs2")
end Module

object Module:
    def fromString(name: String): Option[Module] = name match
        case "db-doobie"    => Some(Module.DbDoobie)
        case "db-skunk"     => Some(Module.DbSkunk)
        case "db-migration" => Some(Module.DBMigration)
        case "flags"        => Some(Module.FeatureFlags)
        case "http-client"  => Some(Module.HttpClient)
        case "redis"        => Some(Module.Redis)
        case "rabbitmq"     => Some(Module.RabbitMQ)
        case _              => None
    end fromString
end Module

final case class Failure(code: String, message: String, details: Option[String])
final case class ProjectContents[F[_]](bytes: fs2.Stream[F, Byte], fileName: FileName, length: FileSize)

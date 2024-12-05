package mason.webapp

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import mason.Module.{DbDoobie, DbSkunk}
import mason.{Module, OrganizationName, PackageName, Project, ProjectName, Version}

case class FormState(
    projectName: Either[String, ProjectName] = Right("healthy-dog"),
    packageName: Either[String, PackageName] = Right("com.acme.myapp"),
    orgName: Either[String, OrganizationName] = Right("com.acme"),
    version: Version = FormState.latestVersion,
    modules: Set[Module] = Set.empty,
    withCI: Boolean = true,
    withDocker: Boolean = true,
    showErrors: Boolean = false
):
    def hasErrors: Boolean                        =
        projectName.isLeft || packageName.isLeft || orgName.isLeft || modules.exists(module =>
            moduleError(module.name).isDefined
        )
    def nameError: Option[String]                 = projectName.left.toOption
    def packageError: Option[String]              = packageName.left.toOption
    def orgError: Option[String]                  = orgName.left.toOption
    def modulesErrors: Option[String]             = Option.when(modules.contains(DbSkunk) && modules.contains(DbDoobie))(
      s"${DbDoobie.name} and ${DbSkunk.name} can't be used at the same time"
    )
    def moduleError(name: String): Option[String] =
        modules.find(_.name == name).flatMap:
            case DbDoobie if modules.contains(DbSkunk) =>
                Some(s"${DbDoobie.name} and ${DbSkunk.name} can't be used at the same time")
            case DbSkunk if modules.contains(DbDoobie) =>
                Some(s"${DbDoobie.name} and ${DbSkunk.name} can't be used at the same time")
            case _                                     => None

    def project: Project =
        (for
            name <- projectName
            pkg  <- packageName
            org  <- orgName
        yield Project(
          name = name,
          packageName = pkg,
          organizationName = org,
          version = version,
          modules = modules,
          generateCI = withCI,
          generateDocker = withDocker
        )).getOrElse(throw IllegalStateException("Invalid state"))
end FormState

object FormState:
    val availableVersions: List[Version] = List(
      Version("0.4.2", "latest (0.4.2)"),
      Version("0.4.1", "0.4.1"),
      Version("0.4.0", "0.4.0"),
      Version("0.3.23", "0.3.23"),
      Version("0.3.22", "0.3.22"),
      Version("0.3.21", "0.3.21"),
      Version("0.3.20", "0.3.20")
    )
    val latestVersion: Version           = availableVersions.head
end FormState

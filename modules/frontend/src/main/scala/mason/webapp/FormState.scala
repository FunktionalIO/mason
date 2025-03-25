package mason.webapp

import io.funktional.rumpel.*
import io.funktional.rumpel.dictionaries.Adjectives
import io.funktional.rumpel.dictionaries.Animals
import io.funktional.rumpel.dictionaries.Colors
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import mason.Module
import mason.Module.DbDoobie
import mason.Module.DbSkunk
import mason.OrganizationName
import mason.PackageName
import mason.Project
import mason.ProjectName
import mason.Version

case class FormState(
    projectName: Either[String, ProjectName] = Right(FormState.nameGenerator.generate.refineUnsafe),
    packageName: Either[String, PackageName] = Right("com.acme.myapp"),
    orgName: Either[String, OrganizationName] = Right("com.acme"),
    version: Option[Version] = None,
    modules: Set[Module] = Set.empty,
    withCI: Boolean = true,
    withDocker: Boolean = true,
    showErrors: Boolean = false
):
    def randomName(): FormState                   = copy(projectName = Right(FormState.nameGenerator.generate.refineUnsafe))
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
            version = version.get,
            modules = modules,
            generateCI = withCI,
            generateDocker = withDocker
        )).getOrElse(throw IllegalStateException("Invalid state"))
end FormState

object FormState:
    private val config = RumpelConfig(
        dictionaries = List(Adjectives, Colors, Animals),
        separator = "-",
        length = 3
    )

    private val nameGenerator: Rumpel = Rumpel(config)

end FormState

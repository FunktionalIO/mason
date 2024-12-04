package mason.webapp

import com.raquo.airstream.web.FetchBuilder
import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L.given
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import mason.{Module, PackageName, Project, ProjectName, Version}
import mason.Module.DbDoobie
import mason.Module.DbSkunk
import org.scalajs.dom

object Main:
    def main(args: Array[String]): Unit =
        renderOnDomContentLoaded(
          dom.document.getElementById("app"),
          app()
        )

    private def app(): Element =
        div(
          h2("Create a new project"),
          projectUI(),
          div(
            "Your choice:",
            ul(
              li(
                strong("Project name: "),
                child.text <-- stateVar.signal.map(_.projectName.fold(identity, identity))
              ),
              li(
                strong("Package name: "),
                child.text <-- stateVar.signal.map(_.packageName.fold(identity, identity))
              ),
              li(
                strong("Version: "),
                child.text <-- stateVar.signal.map(_.version.name)
              ),
              li(
                strong("Modules: "),
                child.text <-- stateVar.signal.map(_.modules.mkString(", "))
              )
            )
          )
        )
    end app

    private val versions = List(
      Version("0.4.0", "latest"),
      Version("0.3.23", "0.3.23"),
      Version("0.3.22", "0.3.22"),
      Version("0.3.21", "0.3.21"),
      Version("0.3.20", "0.3.20")
    )

    private case class State(
        projectName: Either[String, ProjectName] = Right("healthy-dog"),
        packageName: Either[String, PackageName] = Right("com.example.myapp"),
        version: Version = versions.head,
        modules: Set[Module] = Set.empty,
        withCI: Boolean = true,
        withDocker: Boolean = true,
        showErrors: Boolean = false
    ):
        def hasErrors: Boolean                        =
            projectName.isLeft || packageName.isLeft || modules.exists(module => moduleError(module.name).isDefined)
        def nameError: Option[String]                 = projectName.left.toOption
        def packageError: Option[String]              = packageName.left.toOption
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

        def project: Project = Project(
          projectName.getOrElse(""),
          version,
          modules
        )
    end State

    private val stateVar: Var[State] = Var(State())

    private val nameWriter    =
        stateVar.updater[String]((state, name) => state.copy(projectName = name.refineEither))
    private val packageWriter =
        stateVar.updater[String]((state, name) => state.copy(packageName = name.refineEither))
    private val versionWriter =
        stateVar.updater[String]((state, v) =>
            state.copy(version = versions.find(_.number == v).getOrElse(versions.head))
        )
    private val ciUpdater     = stateVar.updater[Boolean]((state, v) => state.copy(withCI = v))
    private val dockerUpdater = stateVar.updater[Boolean]((state, v) => state.copy(withDocker = v))
    private val moduleWriter  =
        stateVar.updater[String]((state, v) =>
            val newModules = Module
                .values
                .find(_.name == v)
                .map: module =>
                    if state.modules.contains(module) then state.modules - module
                    else state.modules + module
                .getOrElse(state.modules)
            state.copy(modules = newModules)
        )

    private val submitter = Observer[State] { state =>
        if state.hasErrors then
            stateVar.update(_.copy(showErrors = true))
        else
            FetchBuilder[Project, ]()
            FetchStream.post("/api/project", _.body(state.project)).map { response =>
                if response.ok then
                    dom.window.alert("Project created successfully")
                else
                    dom.window.alert("An error occurred while creating the project")
            }
            dom.window.alert(state.toString)
    }

    private def projectUI(): Element =
        form(
          onSubmit.preventDefault.mapTo(stateVar.now()) --> submitter,
          label("Project name", forId := "projectName"),
          input(
            typ                       := "text",
            idAttr                    := "projectName",
            placeholder               := "Enter your project name",
            controlled(
              value <-- stateVar.signal.map(_.projectName.getOrElse("")),
              onInput.mapToValue --> nameWriter
            ),
            aria.invalid <-- stateVar.signal.map(_.projectName.isLeft.toString)
          ),
          small(
            child.text <-- stateVar.signal.map(_.nameError.getOrElse(""))
          ),
          label("Package name", forId := "packageName"),
          input(
            typ                       := "text",
            idAttr                    := "packageName",
            placeholder               := "com.example.myapp",
            controlled(
              value <-- stateVar.signal.map(_.packageName.getOrElse("")),
              onInput.mapToValue --> packageWriter
            ),
            aria.invalid <-- stateVar.signal.map(_.packageName.isLeft.toString)
          ),
          small(
            child.text <-- stateVar.signal.map(_.packageError.getOrElse(""))
          ),
          label("Version", forId      := "version"),
          select(
            idAttr                    := "version",
            controlled(
              value <-- stateVar.signal.map(_.version.number),
              onChange.mapToValue --> versionWriter
            ),
            versions.map(version => option(value := version.number, version.name))
          ),
          fieldSet(
            legend("Modules"),
            div(
              cls := "modules",
              Module.values.map { module =>
                  div(
                    input(
                      typ       := "checkbox",
                      role      := "switch",
                      idAttr    := module.name,
                      value     := module.name,
                      onChange.mapToValue --> moduleWriter,
                      aria.invalid <-- stateVar.signal.map(_.moduleError(module.name).isDefined.toString)
                    ),
                    label(forId := module.name, module.name)
                  )
              }
            ),
            aria.invalid <-- stateVar.signal.map(_.modulesErrors.isDefined.toString)
          ),
          small(
            child.text <-- stateVar.signal.map(_.modulesErrors.getOrElse(""))
          ),
          fieldSet(
            legend("Options"),
            div(
              input(
                typ       := "checkbox",
                role      := "switch",
                idAttr    := "withCI",
                value     := "withCI",
                onChange.mapToChecked --> ciUpdater
              ),
              label(forId := "withCI", "Generate Github Actions configuration")
            ),
            div(
              input(
                typ    := "checkbox",
                role   := "switch",
                idAttr := "withDocker",
                value  := "withDocker",
                onChange.mapToChecked --> dockerUpdater
              ),
              label(
                forId  := "withDocker",
                "Include docker-compose configuration including observability tools and databases"
              )
            )
          ),
          input(
            typ                       := "submit",
            value                     := "Create project",
            disabled <-- stateVar.signal.map(_.hasErrors)
          )
        )
end Main

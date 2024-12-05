package mason.webapp

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L.given
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import mason.Module
import mason.PackageName
import mason.ProjectName
import mason.Version
import org.scalajs.dom

object Main:
    def main(args: Array[String]): Unit =
        renderOnDomContentLoaded(
          dom.document.getElementById("app"),
          app()
        )

    private val stateVar: Var[FormState] = Var(FormState())

    private val nameWriter =
        stateVar.updater[String]((state, name) => state.copy(projectName = name.refineEither))

    private val packageWriter =
        stateVar.updater[String]((state, name) => state.copy(packageName = name.refineEither))

    private val orgWriter =
        stateVar.updater[String]((state, name) => state.copy(orgName = name.refineEither))

    private val versionWriter =
        stateVar.updater[String]((state, v) =>
            state.copy(version = FormState.availableVersions.find(_.number == v).getOrElse(FormState.latestVersion))
        )

    private val ciUpdater = stateVar.updater[Boolean]((state, v) => state.copy(withCI = v))

    private val dockerUpdater = stateVar.updater[Boolean]((state, v) => state.copy(withDocker = v))

    private val moduleUpdater =
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
    private val submitter     = Observer[FormState] { state =>
        if state.hasErrors then
            stateVar.update(_.copy(showErrors = true))
        else
            Download
                .download("http://localhost:9876/api/v0/download", state.project)

    }

    private def app(): Element = div(h2("Create a new project"), projectUI)

    lazy val projectUI: Element =
        form(
          onSubmit.preventDefault.mapTo(stateVar.now()) --> submitter,
          //
          label("Project name", forId := "projectName"),
          stringInput("projectName", "Enter your project name", nameWriter, _.projectName),
          small(child.text <-- stateVar.signal.map(_.nameError.getOrElse(""))),
          //
          label("Organization", forId := "orgName"),
          stringInput("orgName", "com.acme", orgWriter, _.orgName),
          small(child.text <-- stateVar.signal.map(_.orgError.getOrElse(""))),
          //
          label("Package name", forId := "packageName"),
          stringInput("packageName", "com.example.myapp", packageWriter, _.packageName),
          small(child.text <-- stateVar.signal.map(_.packageError.getOrElse(""))),
          //
          label("Version", forId      := "version"),
          select(
            idAttr                    := "version",
            controlled(
              value <-- stateVar.signal.map(_.version.number),
              onChange.mapToValue --> versionWriter
            ),
            FormState.availableVersions.map(version => option(value := version.number, version.name))
          ),
          //
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
                      onChange.mapToValue --> moduleUpdater,
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

    private def stringInput(
        id: String,
        invite: String,
        writer: Observer[String],
        valueGetter: FormState => Either[String, String]
    )         =
        input(
          typ         := "text",
          idAttr      := id,
          placeholder := invite,
          controlled(
            value <-- stateVar.signal.map(valueGetter).map(_.getOrElse("")),
            onInput.mapToValue --> writer
          ),
          aria.invalid <-- stateVar.signal.map(state => state.projectName).map(_.isLeft.toString)
        )
end Main

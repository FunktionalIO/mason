package mason.webapp

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import mason.License
import mason.Module
import mason.PackageName
import mason.ProjectName
import mason.Version
import mason.webapp.ui.DynamicSelect
import mason.webapp.ui.StringInput
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement
import org.scalajs.dom.HTMLInputElement

case class Form(state: FormState, api: Api):
    private val stateVar: Var[FormState]     = Var(state)
    private val versions: Var[List[Version]] = Var(Nil)

    private val nameWriter =
        stateVar.updater[String]((s, value) => s.copy(projectName = value.refineEither))

    private val nameSignal =
        stateVar.signal.map(_.projectName).map(_.getOrElse(""))

    private val nameValidation =
        stateVar.signal.map(_.nameError.isDefined)

    private val packageWriter     =
        stateVar.updater[String]((s, value) => s.copy(packageName = value.refineEither))
    private val packageSignal     =
        stateVar.signal.map(_.packageName).map(_.getOrElse(""))
    private val packageValidation =
        stateVar.signal.map(_.packageError.isDefined)

    private val orgWriter     =
        stateVar.updater[String]((s, value) => s.copy(orgName = value.refineEither))
    private val orgSignal     =
        stateVar.signal.map(_.orgName).map(_.getOrElse(""))
    private val orgValidation =
        stateVar.signal.map(_.orgError.isDefined)

    private val versionWriter =
        stateVar.updater[String]: (s, value) =>
            s.copy(version = versions.now().find(_.number == value))

    private val licenseUpdater = stateVar.updater[String]((s, value) => s.copy(license = License.fromString(value)))

    private val dockerUpdater = stateVar.updater[Boolean]((s, value) => s.copy(withDocker = value))

    private val moduleUpdater =
        stateVar.updater[String]((s, value) =>
            val newModules = Module
                .values
                .find(_.name == value)
                .map: module =>
                    if s.modules.contains(module) then s.modules - module
                    else s.modules + module
                .getOrElse(s.modules)
            s.copy(modules = newModules)
        )

    private val submitter = Observer[FormState]: s =>
        if s.hasErrors then
            stateVar.update(_.copy(showErrors = true))
        else
            api.download(s.project)

    def render(): Element = projectUI

    private val projectNameInput =
        StringInput("projectName", "Project name", "my-project", nameSignal, nameWriter, nameValidation).element

    private lazy val organizationInput =
        StringInput("orgName", "organization", "com.acme", orgSignal, orgWriter, orgValidation).element

    private val packageInput =
        StringInput("packageName", "Package name", "com.acme.myapp", packageSignal, packageWriter, packageValidation).element

    private val versionInput: ReactiveHtmlElement[HTMLDivElement] =
        DynamicSelect(
            id = "version",
            lbl = "Version",
            initialSelectedId = None,
            valueSignal = stateVar.signal.map(_.version.map(_.number).getOrElse("")),
            writer = versionWriter,
            optionStream = api.fetchVersions.map { vs =>
                versions.set(vs)
                vs.map(v => DynamicSelect.Option(v.number, v.name))
            }
        ).element
    end versionInput

    private val licenseInput: ReactiveHtmlElement[HTMLDivElement] =
        div(
            fieldSet(
                label("License", forId := "license"),
                select(
                    idAttr := "license",
                    controlled(
                        value <-- stateVar.signal.map(_.license.getOrElse(License.MIT).name),
                        onChange.mapToValue --> licenseUpdater
                    ),
                    License.values.map { license =>
                        option(
                            value := license.name,
                            selected <-- stateVar.signal.map(_.license == license),
                            license.name
                        )
                    }
                )
            ),
        )
    private val modulesInput: ReactiveHtmlElement[HTMLDivElement] =
        div(
            fieldSet(
                legend("Modules"),
                div(
                    cls := "modules",
                    Module.values.map { module =>
                        div(
                            input(
                                typ     := "checkbox",
                                role    := "switch",
                                idAttr  := module.name,
                                value   := module.name,
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
            )
        )

    private val dockerInput: ReactiveHtmlElement[HTMLDivElement] =
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

    private val submitButton: ReactiveHtmlElement[HTMLInputElement] =
        input(
            typ   := "submit",
            value := "Create project",
            cls   := "contrast",
            disabled <-- stateVar.signal.map(_.hasErrors)
        )

    private lazy val projectUI: Element =
        form(
            onSubmit.preventDefault.mapTo(stateVar.now()) --> submitter,
            projectNameInput,
            organizationInput,
            packageInput,
            versionInput,
            licenseInput,
            modulesInput,
            div(
                fieldSet(
                    legend("Options"),
//                    ciInput,
                    dockerInput
                )
            ),
            submitButton
        )

end Form

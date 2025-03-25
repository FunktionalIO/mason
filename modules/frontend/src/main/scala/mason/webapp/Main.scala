package mason.webapp

import com.raquo.laminar.api.L.*
import org.scalajs.dom

object Main:
    def main(args: Array[String]): Unit =
        Config.default match
            case Left(value) => dom.console.error(value)
            case Right(config) =>  renderOnDomContentLoaded(dom.document.getElementById("app"), app(config))

    private def app(config: Config): Element =
        div(
            h2("Create a new project"),
            Form(FormState(), Api(config)).render()
        )
    end app

end Main

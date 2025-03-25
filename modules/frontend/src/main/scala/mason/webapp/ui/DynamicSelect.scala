package mason.webapp.ui

import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement
import scala.util.Try

case class DynamicSelect(
    id: String,
    lbl: String,
    cssClass: String = "",
    valueSignal: Signal[String],
    writer: Observer[String],
    initialSelectedId: Option[String],
    optionStream: EventStream[List[DynamicSelect.Option]],
    onSelect: String => Unit = _ => ()
):
    private val selectedIdVar: Var[Option[String]]          = Var(None)
    private val loadingVar: Var[Boolean]                    = Var(true)
    private val errorVar: Var[Option[String]]               = Var(None)
    private val optionsVar: Var[List[DynamicSelect.Option]] = Var(Nil)

    def element: ReactiveHtmlElement[HTMLDivElement] =
        val e       = div(
            label(lbl, forId := id),
            // Loading indicator
            child.maybe <-- loadingVar.signal.map(isLoading =>
                if isLoading then Some(div(cls := "loading", "Loading options..."))
                else None
            ),

            // Error message
            child.maybe <-- errorVar.signal.map(errorOpt =>
                errorOpt.map(error => div(cls := "error", s"Error: $error"))
            ),
            select(
                idAttr       := id,
                cls          := cssClass,
                disabled <-- loadingVar.signal,
                controlled(
                    value <-- valueSignal,
                    onChange.mapToValue --> writer
                ),

                // Default empty option
                option(
                    value    := "",
                    disabled := true,
                    selected := true,
                    "Select an option"
                ),
                onChange.mapToValue --> { value =>
                    selectedIdVar.set(Some(value))
                    onSelect(value)
                },
                children <-- optionsVar.signal.map { options =>
                    options.map: opt =>
                        option(
                            value := opt.id,
                            selected <-- selectedIdVar.signal.map(_.contains(opt.id)),
                            opt.label
                        )
                }
            )
        )
        given Owner = OneTimeOwner(() => ())
        optionStream.addObserver(
            new Observer[List[DynamicSelect.Option]]:
                override def onNext(nextValue: List[DynamicSelect.Option]): Unit =
                    optionsVar.set(nextValue)
                    loadingVar.set(false)

                override def onError(err: Throwable): Unit =
                    errorVar.set(Some(err.getMessage))
                    loadingVar.set(false)

                override def onTry(nextValue: Try[List[DynamicSelect.Option]]): Unit =
                    nextValue.fold(
                        err => errorVar.set(Some(err.getMessage)),
                        optionsVar.set
                    )
        )
        e
    end element
end DynamicSelect

object DynamicSelect:
    case class Option(id: String, label: String)

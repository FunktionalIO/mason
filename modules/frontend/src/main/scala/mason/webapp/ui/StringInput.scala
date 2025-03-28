package mason.webapp.ui

import com.raquo.laminar.api.L.*

case class StringInput(
    id: String,
    lbl: String,
    invite: String,
    signal: Signal[String],
    writer: Observer[String],
    validationSignal: Signal[Boolean]
):
    lazy val element: HtmlElement =
        div(
            label(lbl, forId := id),
            input(
                typ          := "text",
                idAttr       := id,
                placeholder  := invite,
                controlled(
                    value <-- signal,
                    onInput.mapToValue --> writer
                )
            ),
            small(child.text <-- validationSignal)
        )
end StringInput

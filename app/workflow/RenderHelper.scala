package workflow

import workflows4s.mermaid.MermaidFlowchart

object RenderHelper {

  def toImg(in: MermaidFlowchart) = {
    "https://mermaid.ink/img/" + in.toViewUrl.replaceAll("https://mermaid.live/edit#base64:", "")
  }

  def toMarkdownImg(in: MermaidFlowchart, title: String = "Diagramme Mermaid") = {
    s"![title](${{ toImg(in) }})"
  }

  def toMarkdown(in: MermaidFlowchart) = {
    s"""
       |```mermaid
       |${in.render}```
    """.stripMargin
  }
}

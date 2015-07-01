package discosearch

import spray.json._
import edu.arizona.sista.discourse.rstparser._
import DiscoParser.{ Action, Shift, Reduce }
import DiscoState.Ok

sealed trait Edu
case class Terminal(text: String) extends Edu
case class NonTerminal(label: String, nucleus: String, left: Edu, right: Edu) extends Edu

object Edu {
  def fromDiscourseTree(tree: DiscourseTree): Edu =
    if (tree.isTerminal) Terminal(tree.rawText)
    else {
      val label = tree.relationLabel
      val nucleus = tree.relationDirection match {
        case RelationDirection.LeftToRight => "left"
        case RelationDirection.RightToLeft => "right"
        case RelationDirection.None => "both"
      }
      val left = fromDiscourseTree(tree.children(0))
      val right = fromDiscourseTree(tree.children(1))
      NonTerminal(label, nucleus, left, right)
    }
}

object DiscoJsonProtocol extends DefaultJsonProtocol {

  implicit object eduFormat extends RootJsonFormat[Edu] {
    def read(json: JsValue) = ???
    def write(edu: Edu) = edu match {
      case t: Terminal => JsObject("text" -> JsString(t.text))
      case n: NonTerminal => JsObject(
        "label" -> JsString(n.label),
        "nucleus" -> JsString(n.nucleus),
        "left" -> eduFormat.write(n.left),
        "right" -> eduFormat.write(n.right)
      )
    }
  }

  implicit object actionFormat extends RootJsonFormat[Action] {
    def read(json: JsValue) = {
      val obj = json.asJsObject
      val action = obj.fields("action").asInstanceOf[JsString].value
      if (action == "shift") Shift
      else if (action == "reduce") {
        val label = obj.fields("label").asInstanceOf[JsString].value
        val nucleus = obj.fields("nucleus").asInstanceOf[JsString].value
        Reduce(label, nucleus)
      } else sys.error("we need better error handling")
    }

    def write(action: Action) = action match {
      case Shift => JsObject("action" -> JsString("shift"))
      case Reduce(label, nucleus) => JsObject(
        "action" -> JsString("reduce"),
        "label" -> JsString(label),
        "nucleus" -> JsString(nucleus)
      )
    }
  }

  implicit def okFormat = jsonFormat1(Ok)

}

package discosearch

import edu.arizona.sista.discourse.rstparser._
import edu.arizona.sista.discourse.rstparser.Utils.mkGoldEDUs
import edu.arizona.sista.processors.Document

class DiscoParser {

  import DiscoParser._

  // parser's internal state
  private var buffer: List[DiscourseTree] = Nil
  private var stack: List[DiscourseTree] = Nil
  private var doc: Document = _
  private var tree: DiscourseTree = _
  private var edus: Array[Array[(Int, Int)]] = _

  // starts a new parse
  def startParse(t: DiscourseTree, d: Document): Unit = {
    tree = t
    doc = d
    edus = mkGoldEDUs(tree, doc)
    buffer = mkBuffer()
    stack = Nil
  }

  private def mkBuffer(): List[DiscourseTree] = for {
    i <- List.range(0, edus.size)
    j <- List.range(0, edus(i).size)
    edu = edus(i)(j)
  } yield new DiscourseTree(i, edu._1, edu._2, doc, j)

  // returns all valid actions given the current state
  def validActions: List[Action] = {
    val labels = List(
      "comparison", "background", "textual-organization", "joint", "attribution",
      "enablement", "condition", "temporal", "explanation", "cause", "contrast",
      "evaluation", "topic-change", "same-unit", "manner-means", "summary",
      "topic-comment", "elaboration"
    )
    val allReduces = for {
      l <- labels
      n <- Seq("left", "right", "both")
    } yield Reduce(l, n)

    if (buffer.nonEmpty && stack.size >= 2) {
      // all actions are valid
      Shift :: allReduces
    } else if (buffer.nonEmpty && stack.size < 2) {
      // not enough nodes in the stack
      // no reduce
      List(Shift)
    } else if (buffer.isEmpty && stack.size >= 2) {
      // buffer is empty
      // no shift
      allReduces
    } else {
      // either we are done parsing or we haven't even started
      Nil
    }

  }

  // returns feature vector for current state
  def features: Map[String, Any] = ???

  // returns the optimal action for the current state
  def goldAction: Action = ???

  // transition to the next state by performing action
  def perform(action: Action): Unit = action match {
    case Shift =>
      // move next node in the buffer to the top of the stack
      stack = buffer.head :: stack
      buffer = buffer.tail
    case Reduce(label, nucleus) =>
      // reduce two nodes on top of the stack
      val right :: left :: rest = stack
      val children = Array(left, right)
      val direction = nucleusToDirection(nucleus)
      val node = new DiscourseTree(label, direction, children)
      stack = node :: rest
  }

}

object DiscoParser {

  sealed trait Action
  case object Shift extends Action
  case class Reduce(label: String, nucleus: String) extends Action

  def nucleusToDirection(nucleus: String): RelationDirection.Value = nucleus match {
    case "left" => RelationDirection.LeftToRight
    case "right" => RelationDirection.RightToLeft
    case "both" => RelationDirection.None
  }

}

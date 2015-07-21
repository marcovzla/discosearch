package discosearch

import edu.arizona.sista.discourse.rstparser._
import edu.arizona.sista.discourse.rstparser.Utils.mkGoldEDUs
import edu.arizona.sista.processors.Document

class DiscoParser(
  val tree: DiscourseTree,
  val doc: Document,
  val corpusStats: CorpusStats
) {

  import DiscoParser._

  // parser's internal state
  private var edus: Array[Array[(Int, Int)]] = mkGoldEDUs(tree, doc)
  private var buffer: List[DiscourseTree] = mkBuffer()
  private var stack: List[DiscourseTree] = Nil

  private var costTracker: CostTracker = new CostTracker(tree, doc, leftToRight = true)

  // feature extraction
  val discoFeatures = new DiscoFeatures

  private def mkBuffer(): List[DiscourseTree] = for {
    i <- List.range(0, edus.size)
    j <- List.range(0, edus(i).size)
    edu = edus(i)(j)
  } yield new DiscourseTree(i, edu._1, edu._2, doc, j)

  def isDone: Map[String, Boolean] = Map("done" -> (buffer.isEmpty && stack.size == 1))

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

    if (buffer.nonEmpty && stack.size > 1) {
      // all actions are valid
      // Shift :: allReduces
      List(Shift, Reduce("comparison", "left"))
    } else if (buffer.nonEmpty && stack.size < 2) {
      // not enough nodes in the stack
      // no reduce
      List(Shift)
    } else if (buffer.isEmpty && stack.size > 1) {
      // buffer is empty
      // no shift
      // allReduces
      List(Reduce("comparison", "left"))
    } else {
      // either we are done parsing or we haven't started yet
      Nil
    }

  }

  // returns feature vector for current state
  def features: Map[String, Seq[(String, Double)]] =
    discoFeatures.mkFeatures(stack, buffer, doc, edus, corpusStats)

  // returns the optimal action for the current state
  def goldAction: Action = {
    val actions = validActions
    if (actions.isEmpty) sys.error("no valid actions")
    else if (actions.size == 1) actions.head
    else {
      val scoredActions = actions map {
        case a @ Shift => (a, costTracker.nextCost(buffer.head))
        case a: Reduce =>
          val right = stack(0)
          val left = stack(1)
          val children = Array(left, right)
          val direction = nucleusToDirection(a.nucleus)
          val node = new DiscourseTree(a.label, direction, children)
          (a, costTracker.nextCost(node))
      }
      scoredActions.minBy(_._2)._1
    }
  }

  // transition to the next state by performing action
  def perform(action: Action): Unit = action match {
    case Shift =>
      // move next node in the buffer to the top of the stack
      stack = buffer.head :: stack
      buffer = buffer.tail
      costTracker.addNode(stack.head)
    case Reduce(label, nucleus) =>
      // reduce two nodes on top of the stack
      val right :: left :: rest = stack
      val children = Array(left, right)
      val direction = nucleusToDirection(nucleus)
      val node = new DiscourseTree(label, direction, children)
      stack = node :: rest
      costTracker.addNode(stack.head)
  }

  def parsedTree: DiscourseTree = {
    require(stack.size == 1)
    stack.head
  }

  def loss: Double = {
    val scorer = new DiscourseScorer
    val score = new DiscourseScore
    scorer.score(parsedTree, tree, score, ScoreType.OnlyStructure)
    1.0 - score.f1
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

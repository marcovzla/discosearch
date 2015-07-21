package discosearch

import edu.arizona.sista.struct.Interval
import edu.arizona.sista.processors.Document
import edu.arizona.sista.discourse.rstparser._

class CostTracker(
  tree: DiscourseTree,
  doc: Document,
  leftToRight: Boolean
) {

  val sentenceOffset: Array[Int] = doc.sentences.map(_.size).scanLeft(0)(_ + _)

  val goldIntervals: Set[NodeInterval] = getIntervals(tree)

  var falsePositives: Set[NodeInterval] = Set.empty
  var falseNegatives: Set[NodeInterval] = Set.empty

  var seenIntervals: Set[NodeInterval] = Set.empty

  def cost: Double = cost(falsePositives, falseNegatives)

  def cost(fp: Set[NodeInterval], fn: Set[NodeInterval]): Double =
    fp.size + fn.size

  def nextCost(node: DiscourseTree): Double = {
    val int = mkInterval(node)
    val (nextFP, nextFN) = structCost(int)
    cost(nextFP, nextFN)
  }

  def addNode(node: DiscourseTree): Unit = {
    val int = mkInterval(node)
    val (nextFP, nextFN) = structCost(int)
    falsePositives = nextFP
    falseNegatives = nextFN
    seenIntervals += int
  }

  def structCost(node: NodeInterval): (Set[NodeInterval], Set[NodeInterval]) = {
    val (nextFP, nextFN) = if (!goldIntervals.contains(node)) {
      val fp = falsePositives + node
      val crossImpossible = goldIntervals filter node.overlaps
      val fn = falseNegatives ++ crossImpossible
      (fp, fn)
    } else (falsePositives, falseNegatives)
    // impossible if we are parsing left-to-right
    val lrImpossible = if (node.isTerminal && leftToRight) {
      // we just added a terminal node
      // all nodes to our left that haven't been merged are false negatives
      goldIntervals filter (_.end <= node.start) diff seenIntervals
    } else Nil
    (nextFP, nextFN ++ lrImpossible)
  }

  def getIntervals(node: DiscourseTree): Set[NodeInterval] = {
    @annotation.tailrec
    def collect(nodes: List[DiscourseTree], intervals: List[NodeInterval]): List[NodeInterval] =
      nodes match {
        case Nil => intervals
        case n :: rest if n.isTerminal => collect(rest, mkInterval(n) :: intervals)
        case n :: rest => collect(n.children.toList ::: rest, mkInterval(n) :: intervals)
      }
    collect(List(node), Nil).toSet
  }

  def mkInterval(node: DiscourseTree): NodeInterval =
    NodeInterval(
      Interval(globalToken(node.firstToken), globalToken(node.lastToken)),
      node.isTerminal
    )

  def globalToken(tokenOffset: TokenOffset): Int =
    tokenOffset.token + sentenceOffset(tokenOffset.sentence)

}

case class NodeInterval(
  interval: Interval,
  isTerminal: Boolean
) {
  val start = interval.start
  val end = interval.end
  def overlaps(that: NodeInterval): Boolean =
    this.interval overlaps that.interval
}

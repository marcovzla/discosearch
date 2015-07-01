package discosearch

import akka.actor.{ Actor, ActorLogging }
import edu.arizona.sista.discourse.rstparser._
import edu.arizona.sista.processors.Document
import DiscoParser.Action

class DiscoState extends Actor with ActorLogging {

  import DiscoState._

  val dataDir = "/Users/marcov/data/RST_cached_preprocessing/rst_train"
  val dependencySyntax = true

  val processor = CacheReader.getProcessor(dependencySyntax)
  val (treedocs, corpusStats) = RSTParser.mkTrees(dataDir, processor)
  log.info("loaded trees")

  // we need to call startNextParse() to get started
  var currentIndex = -1
  var tree: DiscourseTree = _
  var doc: Document = _
  val parser = new DiscoParser

  def receive = {
    case GetDiscourseTree => sender ! getDiscourseTree()
    case StartNextParse => sender ! startNextParse()
    case GetValidActions => sender ! parser.validActions
    case PerformAction(a) => sender ! performAction(a)
  }

  def getDiscourseTree(): Edu = {
    val (tree, doc) = treedocs(currentIndex)
    Edu.fromDiscourseTree(tree)
  }

  def startNextParse() = {
    currentIndex += 1
    val (t, d) = treedocs(currentIndex)
    tree = t
    doc = d
    parser.startParse(tree, doc)
    Ok("started parse")
  }

  def performAction(a: Action) = {
    parser.perform(a)
    Ok(s"performed action $a")
  }

}

object DiscoState {
  case class Ok(msg: String)
  case object GetDiscourseTree
  case object StartNextParse
  case object GetValidActions
  case class PerformAction(action: Action)
}

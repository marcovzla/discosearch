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
  val (_treedocs, corpusStats) = RSTParser.mkTrees(dataDir, processor)
  val treedocs = _treedocs.toVector
  log.info("loaded trees")

  // we need to call startNextParse() to get started
  var currentIndex = -1
  var tree: DiscourseTree = _
  var doc: Document = _
  val parser = new DiscoParser

  def receive = {
    case GetDocIds => sender ! getDocIds()
    case GetDiscourseTree => sender ! getDiscourseTree()
    case StartParse(id) => sender ! startParse(id)
    case GetValidActions => sender ! parser.validActions
    case PerformAction(a) => sender ! performAction(a)
    case IsDone => sender ! parser.isDone
    case GetLoss => sender ! Map("loss" -> parser.loss)
  }

  def getDiscourseTree(): Edu = {
    val (tree, doc) = treedocs(currentIndex)
    Edu.fromDiscourseTree(tree)
  }

  def startParse(id: Int) = {
    currentIndex = id
    val (t, d) = treedocs(currentIndex)
    tree = t
    doc = d
    parser.startParse(tree, doc)
    Ok(s"started parsing tree $id")
  }

  def performAction(a: Action) = {
    parser.perform(a)
    Ok(s"performed action $a")
  }

  def getDocIds(): List[Int] = List.range(0, treedocs.size)

}

object DiscoState {
  case class Ok(msg: String)
  case object GetDiscourseTree
  case class StartParse(id: Int)
  case object GetValidActions
  case class PerformAction(action: Action)
  case object GetDocIds
  case object IsDone
  case object GetLoss
}

package discosearch

import akka.actor.{ Actor, ActorLogging }
import edu.arizona.sista.discourse.rstparser._
import edu.arizona.sista.processors.Document
import DiscoParser.Action

class DiscoState extends Actor with ActorLogging {

  import DiscoState._

  var parser: DiscoParser = _

  def receive = {
    case GetDocIds => sender ! getDocIds()
    case GetDiscourseTree(uuid) => sender ! getDiscourseTree(uuid)
    case StartParse(id) => sender ! startParse(id)
    case StopParse(uuid) => sender ! stopParse(uuid)
    case GetValidActions(uuid) => sender ! getValidActions(uuid)
    case GetGoldAction(uuid) => sender ! getGoldAction(uuid)
    case PerformAction(uuid, a) => sender ! performAction(uuid, a)
    case IsDone(uuid) => sender ! isDone(uuid)
    case GetLoss(uuid) => sender ! Map("loss" -> getLoss(uuid))
    case GetFeatures(uuid) => sender ! getFeatures(uuid)
  }

  def getDiscourseTree(uuid: String): Edu = {
    val tree = DiscoState.sessions(uuid).tree
    Edu.fromDiscourseTree(tree)
  }

  def startParse(id: Int) = {
    val (tree, doc) = treedocs(id)
    parser = new DiscoParser(tree, doc, corpusStats)
    //val uuid = mkUUID()
    //DiscoState.sessions += (uuid -> parser)
    Ok("x")
  }

  def stopParse(uuid: String) = {
    //DiscoState.sessions -= uuid
    Ok(s"stoped parse $uuid")
  }

  def getValidActions(uuid: String) =
    // DiscoState.sessions(uuid).validActions
    parser.validActions

  def getGoldAction(uuid: String) =
    // DiscoState.sessions(uuid).goldAction
    parser.goldAction

  def performAction(uuid: String, a: Action) = {
    // DiscoState.sessions(uuid).perform(a)
    parser.perform(a)
    Ok(s"performed action $a")
  }

  def isDone(uuid: String) =
    // DiscoState.sessions(uuid).isDone
    parser.isDone

  def getLoss(uuid: String) =
    // DiscoState.sessions(uuid).loss
    parser.loss

  def getFeatures(uuid: String) =
    // DiscoState.sessions(uuid).features
    parser.features

  def getDocIds(): List[Int] = List.range(0, treedocs.size)

  def mkUUID() = java.util.UUID.randomUUID.toString

}

object DiscoState {
  case class StartParse(id: Int)
  case class StopParse(uuid: String)
  case class Ok(msg: String)
  case class GetDiscourseTree(uuid: String)
  case class GetValidActions(uuid: String)
  case class GetGoldAction(uuid: String)
  case class PerformAction(uuid: String, action: Action)
  case object GetDocIds
  case class IsDone(uuid: String)
  case class GetLoss(uuid: String)
  case class GetFeatures(uuid: String)

  val dataDir = "/Users/marcov/data/RST_cached_preprocessing/rst_small"
  val dependencySyntax = true

  val processor = CacheReader.getProcessor(dependencySyntax)
  val (_treedocs, corpusStats) = RSTParser.mkTrees(dataDir, processor)
  val treedocs = _treedocs.toVector
  println("ready to parse!")

  var sessions: Map[String, DiscoParser] = Map.empty
}

package discosearch

import akka.actor.{ Actor, ActorLogging }
import edu.arizona.sista.discourse.rstparser._

class DiscoState extends Actor with ActorLogging {

  import DiscoState._

  val dataDir = "/Users/marcov/data/RST_cached_preprocessing/rst_train"
  val dependencySyntax = true

  val processor = CacheReader.getProcessor(dependencySyntax)
  val (treedocs, corpusStats) = RSTParser.mkTrees(dataDir, processor)
  log.info("loaded trees")

  var currentIndex = 0

  def getEdu(): Edu = {
    val (tree, doc) = treedocs(currentIndex)
    Edu.fromDiscourseTree(tree)
  }

  def receive = {
    case GetEdu(_) => sender ! Ok(getEdu())
  }

}

object DiscoState {
  case class Ok(edu: Edu)
  case class GetEdu(id: Int)
}

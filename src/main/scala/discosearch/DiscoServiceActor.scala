package discosearch

import akka.actor.{ Actor, Props }
import akka.util.Timeout
import akka.pattern.ask
import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport._
import scala.util.{ Success, Failure }
import scala.concurrent._
import scala.concurrent.duration._
import MediaTypes._
import DiscoJsonProtocol._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class DiscoServiceActor extends Actor with DiscoService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

}

// this trait defines our service behavior independently from the service actor
trait DiscoService extends HttpService {

  import DiscoState._

  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(5.seconds)

  //Our worker Actor handles the work of the request.
  val worker = actorRefFactory.actorOf(Props[DiscoState], "worker")

  val myRoute = path("") {
    get {
      respondWithMediaType(`application/json`) {
        complete {
          (worker ? GetEdu(0)).mapTo[Ok].map(_.edu).recover{ case _ => Terminal("error") }
        }
      }
    }
  }

}

package bitwolf

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.{Done, NotUsed}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Sink, Flow, Keep}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}

import play.api.libs.json.Json

import scala.concurrent.Future


object PriceStream {
  case object Subscribe

  val subscriptionPayload = """
    {
      "event": "subscribe",
      "channel": "trades",
      "symbol": "BTCUSD"
    }
  """

  def props(): Props = Props(new PriceStream)
}

class PriceStream extends Actor with ActorLogging {
  import PriceStream.{Subscribe, subscriptionPayload}
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val materializer = ActorMaterializer()

  var subscribers = List[ActorRef]()

  val handleIncoming: Sink[Message, Future[Done]] = Sink.foreach {
    case message: TextMessage.Strict =>
      val json = Json.parse(message.text)
      for {
        messageType <- (json \ 1).asOpt[String]
        if messageType.contains("te")
        trade <- ExecutedTrade(json)
      } subscribers.foreach { _ ! trade }
    case _ =>
  }

  val websocketSubscription: Source[Message, NotUsed] = Source.single(TextMessage(subscriptionPayload)).concatMat(Source.maybe[Message])(Keep.left)
  val flow: Flow[Message, Message, Future[Done]] = Flow.fromSinkAndSourceMat(handleIncoming, websocketSubscription)(Keep.left)
  val (upgradedResponse, closed) = Http()(context.system).singleWebSocketRequest(WebSocketRequest("wss://api.bitfinex.com/ws"), flow)

  val connected = upgradedResponse.map { upgrade =>
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Done
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  connected.onComplete(x => log.info(s"$x"))
  closed.foreach(_ => log.info("closed"))

  def receive: Receive = {
    case Subscribe =>
      subscribers = subscribers :+ sender()
    case _ =>
  }
}
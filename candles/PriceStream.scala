package candles

import akka.actor.{Actor, ActorRef, Props}
import akka.{Done, NotUsed}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Sink, Flow, Keep}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}

import play.api.libs.json.Json

import scala.concurrent.Future


object PriceStream {
  def props(subscriber: ActorRef): Props = Props(new PriceStream(subscriber))
}

class PriceStream(subscriber: ActorRef) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val materializer = ActorMaterializer()

  val subscriptionPayload = """
    {
      "event": "subscribe",
      "channel": "trades",
      "symbol": "BTCUSD"
    }
  """

  val handleIncoming: Sink[Message, Future[Done]] = Sink.foreach {
    case message: TextMessage.Strict =>
      val json = Json.parse(message.text)
      for {
        messageType <- (json \ 1).asOpt[String]
        if messageType.contains("te")
        trade = ExecutedTrade(json)
      } subscriber ! trade
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

  connected.onComplete(println)
  closed.foreach(_ => println("closed"))

  def receive: Receive = {
    case _ =>
  }
}
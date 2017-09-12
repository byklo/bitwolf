import akka.actor.{ActorSystem, Actor, ActorLogging, Props}
import akka.{Done, NotUsed}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Sink, Flow, Keep}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}

import play.api.libs.json.Json

import scala.concurrent.Future


object CandleBuilder {
  def props(): Props = Props(new CandleBuilder)
}

class CandleBuilder extends Actor with ActorLogging {
  def receive: Receive = {
    case price: Double => println(price)
    case _ =>
  }
}


object App {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    val candleman = system.actorOf(CandleBuilder.props(), "candleman")

    val printSink: Sink[Message, Future[Done]] = Sink.foreach {
      case message: TextMessage.Strict =>
        val json = Json.parse(message.text)
        val messageType = (json \ 1).asOpt[String]
        if (messageType.contains("te")) {
          candleman ! (json \ 4).as[Double]
        }
      case _ =>
    }

    val subscriptionPayload = """
      {
        "event": "subscribe",
        "channel": "trades",
        "symbol": "BTCUSD"
      }
    """

    val subscribeTrades: Source[Message, NotUsed] = Source.single(TextMessage(subscriptionPayload)).concatMat(Source.maybe[Message])(Keep.left)

    val flow: Flow[Message, Message, Future[Done]] = Flow.fromSinkAndSourceMat(printSink, subscribeTrades)(Keep.left)

    val (upgradedResponse, closed) = Http().singleWebSocketRequest(WebSocketRequest("wss://api.bitfinex.com/ws"), flow)

    val connected = upgradedResponse.map { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Done
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }

    connected.onComplete(println)
    closed.foreach(_ => println("closed"))

    println("ok")
  }
}
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


object TradeStream {
  case object Subscribe
}

abstract class TradeStream extends Actor with ActorLogging {
  import TradeStream.Subscribe

  var subscribers = List[ActorRef]()

  def publish(trade: ExecutedTrade) {
    subscribers.foreach{ _ ! trade }
  }

  def receive: Receive = {
    case Subscribe =>
      subscribers = subscribers :+ sender()
    case _ =>
  }
}


object CsvTradeStream {
  def props(filepath: String, rate: Int): Props = Props(new CsvTradeStream(filepath, rate))
}

class CsvTradeStream(filepath: String, rate: Int) extends TradeStream {
  import TradeStream.Subscribe
  import java.io.File
  import com.github.tototoshi.csv.CSVReader

  val reader = CSVReader.open(new File(filepath))

  def start() {
    reader.iterator.foreach{ line =>
      ExecutedTrade(line.map(_.toString).toList).foreach{ trade =>
        println(s"${trade.pretty}")
        publish(trade)
      }

      if (0 < rate && rate < 1000) {
        Thread.sleep((1000.0 / rate).toLong)
      }
    }
  }

  override def receive: Receive = {
    case Subscribe =>
      subscribers = subscribers :+ sender()
      start()
    case _ =>
  }
}


object BitfinexTradeStream {
  val subscriptionPayload = """
    {
      "event": "subscribe",
      "channel": "trades",
      "symbol": "BTCUSD"
    }
  """

  def props(): Props = Props(new BitfinexTradeStream)
}

class BitfinexTradeStream extends TradeStream {
  import TradeStream.Subscribe
  import BitfinexTradeStream.subscriptionPayload
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val materializer = ActorMaterializer()

  val handleIncoming: Sink[Message, Future[Done]] = Sink.foreach {
    case message: TextMessage.Strict =>
      val json = Json.parse(message.text)
      for {
        messageType <- (json \ 1).asOpt[String]
        if messageType.contains("te")
        trade <- ExecutedTrade(json)
      } publish(trade)
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
}

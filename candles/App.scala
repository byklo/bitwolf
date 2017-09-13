package candles

import akka.actor.ActorSystem

object CandlesApp {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    import system.dispatcher

    val candles1M = system.actorOf(CandleBuilder.props(60), "candles1M")
    val priceStream = system.actorOf(PriceStream.props(candles1M), "priceStream")
  }
}
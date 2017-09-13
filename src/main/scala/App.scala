package bitwolf

import akka.actor.ActorSystem


object Bitwolf {
  def main(args: Array[String]) {
    val system = ActorSystem()

    val priceStream = system.actorOf(PriceStream.props(), "priceStream")
    val candles1M = system.actorOf(CandleBuilder.props(1 * 60, priceStream), "candles1M")
    val candles2M = system.actorOf(CandleBuilder.props(2 * 60, priceStream), "candles2M")
  }
}
package bitwolf

import akka.actor.ActorSystem


object Bitwolf {
  def main(args: Array[String]) {
    val system = ActorSystem()

    val priceStream = system.actorOf(PriceStream.props(), "priceStream")
    val trader1M = system.actorOf(Trader.props(10, priceStream), "trader1M")
  }
}
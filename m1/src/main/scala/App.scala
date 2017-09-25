package bitwolf

import akka.actor.ActorSystem
import scopt.OptionParser


case class Config(
	ema10: Option[Double] = None,
	ema21: Option[Double] = None,
	ema100: Option[Double] = None
)

object Bitwolf {
  def main(args: Array[String]) {
  	val parser = new OptionParser[Config]("bitwolf") {
  		head("bitwolf 0.1")
  		opt[Double]("ema10").action( (x,c) => c.copy(ema10 = Some(x)) ).text("initial ema10 value")
  		opt[Double]("ema21").action( (x,c) => c.copy(ema21 = Some(x)) ).text("initial ema21 value")
  		opt[Double]("ema100").action( (x,c) => c.copy(ema100 = Some(x)) ).text("initial ema100 value")
      help("help").text("usage dialog")
  	}

  	parser.parse(args, Config()) match {
  		case Some(config) =>
				val system = ActorSystem()
				val priceStream = system.actorOf(PriceStream.props(), "priceStream")
				val trader1M = system.actorOf(Trader.props(60, priceStream, config), "trader1M")
  		case None =>
  			println("#@!%%@ BAD ARGS #@!#@!")
  	}
  }
}
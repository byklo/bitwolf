package bitwolf

import akka.actor.ActorSystem
import scopt.OptionParser


case class BitwolfConfig(
	ema10: Option[Double] = None,
	ema21: Option[Double] = None,
	ema100: Option[Double] = None
)

object Bitwolf {
  def main(args: Array[String]) {
  	val parser = new OptionParser[BitwolfConfig]("bitwolf") {
  		head("BITWOLF")
  		opt[Double]("ema10").action( (x,c) => c.copy(ema10 = Some(x)) ).text("initial ema10 value")
  		opt[Double]("ema21").action( (x,c) => c.copy(ema21 = Some(x)) ).text("initial ema21 value")
  		opt[Double]("ema100").action( (x,c) => c.copy(ema100 = Some(x)) ).text("initial ema100 value")
      help("help").text("usage dialog")
  	}

  	parser.parse(args, BitwolfConfig()) match {
  		case Some(config) =>
				val system = ActorSystem()
				val tradeStream = system.actorOf(BitfinexTradeStream.props(), "BFXTradeStream")
				val trader1M = system.actorOf(Trader.props(60, tradeStream, config), "trader1M")
  		case None =>
  			println("#@!%%@ BAD ARGS #@!#@!")
  	}
  }
}


case class BacktestConfig(
  tradesFilepath: String = "",
  rate: Int = 1000
)

object Backtest {
  def main(args: Array[String]) {
    val parser = new OptionParser[BacktestConfig]("backtest") {
      head("BACKTEST")
      opt[String]("trades").required().action( (x,c) => c.copy(tradesFilepath = x) ).text("trades csv")
      opt[Int]("rate").action( (x,c) => c.copy(rate = x) ).text("# trades read per second (0 < rate < 1000)")
      help("help").text("usage dialog")
    }

    parser.parse(args, BacktestConfig()) match {
      case Some(config) =>
        val system = ActorSystem()
        val tradeStream = system.actorOf(CsvTradeStream.props(config.tradesFilepath, config.rate), "CSVTradeStream")
        val trader15M = system.actorOf(Trader.props(4*60*60, tradeStream, BitwolfConfig()), "trader15M")
      case None =>
        println("#@!%%@ BAD ARGS #@!#@!")
    }
  }
}
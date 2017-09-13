package bitwolf

import akka.actor.{Actor, ActorRef, ActorLogging, Props}


object CandleBuilder {
  def props(intervalSeconds: Long, priceStream: ActorRef): Props = Props(new CandleBuilder(intervalSeconds, priceStream))
}

class CandleBuilder(intervalSeconds: Long, priceStream: ActorRef) extends Actor with ActorLogging {
  import scala.math.{ceil, max, min, abs}
  import PriceStream.Subscribe

  var firstTrade = true
  var endTimestamp = 0L
  var high = Double.NegativeInfinity
  var low = Double.PositiveInfinity
  var open = 0.0
  var lastPrice = 0.0
  var units = 0.0

  priceStream ! Subscribe

  def receive: Receive = {
    case trade: ExecutedTrade =>
      val newEndTimestamp = (ceil(trade.timestamp / intervalSeconds) * intervalSeconds).toLong
      if (endTimestamp == newEndTimestamp) {
        // same candle
        high = max(high, trade.price)
        low = min(low, trade.price)
        lastPrice = trade.price
        units += abs(trade.units)
      } else {
        // start new candle
        if (firstTrade) {
          firstTrade = false
          high = trade.price
          low = trade.price
        } else {
          val newCandle = Candle(endTimestamp, intervalSeconds, high, low, open, lastPrice, units)
          log.info(s"$newCandle")
          high = Double.NegativeInfinity
          low = Double.PositiveInfinity
        }
        endTimestamp = newEndTimestamp
        open = trade.price
        lastPrice = trade.price
        units = abs(trade.units)
      }
    case _ =>
  }
}
package candles

import akka.actor.{Actor, ActorLogging, Props}


object CandleBuilder {
  def props(intervalSeconds: Long): Props = Props(new CandleBuilder(intervalSeconds))
}

class CandleBuilder(intervalSeconds: Long) extends Actor with ActorLogging {
  import scala.math.{ceil, max, min, abs}

  var firstTrade = true
  var endTimestamp = 0L
  var high = Double.NegativeInfinity
  var low = Double.PositiveInfinity
  var open = 0.0
  var lastPrice = 0.0
  var units = 0.0

  def receive: Receive = {
    case trade: ExecutedTrade =>
      println(trade)
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
          println(newCandle)
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
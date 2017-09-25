package bitwolf

import akka.actor.{Actor, ActorRef, ActorLogging, Props}


class PriceWatch(intervalSeconds: Long, config: BitwolfConfig) {
  import scala.math.{ceil, max, min, abs}

  var endTimestamp = 0L
  var high = Double.NegativeInfinity
  var low = Double.PositiveInfinity
  var open = 0.0
  var lastPrice = 0.0
  var units = 0.0

  val ema10 = EMA(10, config.ema10)
  val ema21 = EMA(21, config.ema21)
  val ema100 = EMA(100, config.ema100)

  def consume(trade: ExecutedTrade) = {
    val newEndTimestamp = (ceil(trade.timestamp / intervalSeconds) * intervalSeconds).toLong
    if (endTimestamp == newEndTimestamp) {
      // same candle
      high = max(high, trade.price)
      low = min(low, trade.price)
      lastPrice = trade.price
      units += abs(trade.units)
    } else {
      // start new candle
      if (endTimestamp == 0L) {
        // first candle
        high = trade.price
        low = trade.price
      } else {
        val newCandle = Candle(endTimestamp, intervalSeconds, high, low, open, lastPrice, units)
        println(s"$newCandle")
        // currently sends CLOSING PRICE to EMA, consider using mean or >>[median]<<
        ema10.update(lastPrice)
        ema21.update(lastPrice)
        ema100.update(lastPrice)

        println(s"EMA10 = ${ema10.current}")
        println(s"EMA21 = ${ema21.current}")
        println(s"EMA100 = ${ema100.current}")
        high = Double.NegativeInfinity
        low = Double.PositiveInfinity
      }
      endTimestamp = newEndTimestamp
      open = trade.price
      lastPrice = trade.price
      units = abs(trade.units)
    }
  }
}


object Trader {
  def props(intervalSeconds: Long, tradeStream: ActorRef, config: BitwolfConfig): Props = Props(new Trader(intervalSeconds, tradeStream, config))
}

class Trader(intervalSeconds: Long, tradeStream: ActorRef, config: BitwolfConfig) extends Actor with ActorLogging {
  import TradeStream.Subscribe

  val pricing = new PriceWatch(intervalSeconds, config)

  tradeStream ! Subscribe

  def receive: Receive = {
    case trade: ExecutedTrade =>
      pricing.consume(trade)
    case _ =>
  }
}
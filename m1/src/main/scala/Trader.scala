package bitwolf

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

class SimpleMovingAverage(n: Int) {
  var prices = List[Double]()
  var current = 0.0

  def peek(price: Double) = {
    if (prices.size < n) {
      (current * prices.size + price) / (prices.size + 1)
    } else {
      (current * n - prices.last + price) / n
    }
  }

  def update(price: Double) {
    if (prices.size < n) {
      current = (current * prices.size + price) / (prices.size + 1)
    } else {
      current = (current * n - prices.last + price) / n
      prices = prices.dropRight(1)
    }
    prices = price :: prices
  }
}

class PriceWatch(intervalSeconds: Long) {
  import scala.math.{ceil, max, min, abs}

  var firstTrade = true
  var endTimestamp = 0L
  var high = Double.NegativeInfinity
  var low = Double.PositiveInfinity
  var open = 0.0
  var lastPrice = 0.0
  var units = 0.0

  val sma10 = new SimpleMovingAverage(10)
  val sma20 = new SimpleMovingAverage(20)

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
      if (firstTrade) {
        firstTrade = false
        high = trade.price
        low = trade.price
      } else {
        val newCandle = Candle(endTimestamp, intervalSeconds, high, low, open, lastPrice, units)
        println(s"$newCandle")
        sma10.update(lastPrice)
        sma20.update(lastPrice)
        println(s"MA10 = ${sma10.current}")
        println(s"MA20 = ${sma20.current}")
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
  def props(intervalSeconds: Long, priceStream: ActorRef): Props = Props(new Trader(intervalSeconds, priceStream))
}

class Trader(intervalSeconds: Long, priceStream: ActorRef) extends Actor with ActorLogging {
  import PriceStream.Subscribe

  val pricing = new PriceWatch(intervalSeconds)

  priceStream ! Subscribe

  def receive: Receive = {
    case trade: ExecutedTrade =>
      pricing.consume(trade)
    case _ =>
  }
}
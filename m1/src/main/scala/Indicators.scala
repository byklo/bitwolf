package bitwolf


abstract class MovingAverage(n: Int) {
  var current = 0.0
  def update(price: Double)
}

object EMA {
  def apply(n: Int, initialEMA: Option[Double]) =
    if (initialEMA.isEmpty) new FreshEMA(n) else new InitializedEMA(n, initialEMA.get)
  def apply(n: Int) = new FreshEMA(n)
}

abstract class EMA(n: Int) extends MovingAverage(n) {
  val k = 2.0 / (n + 1)
  def updateEMA(price: Double) {
    current = (price - current) * k + current
  }
}

class InitializedEMA(n: Int, initialEMA: Double) extends EMA(n) {
  current = initialEMA
  println(s"created EMA($n) with initial value $initialEMA")

  override def update(price: Double) {
    updateEMA(price)
  }
}

class FreshEMA(n: Int) extends EMA(n) {
  // n:           period
  // intial ema:  sma(n)
  // multiplier:  (2 / (n + 1))
  // EMA:         {Close - EMA(previous day)} x multiplier + EMA(previous day)
  val sma = new SMA(n)
  println(s"created EMA($n)")

  override def update(price: Double) {
    if (sma.prices.size < n) {
      sma.update(price)
      current = sma.current
    } else {
      updateEMA(price)
    }
  }
}

class SMA(n: Int) extends MovingAverage(n) {
  var prices = List[Double]()
  println(s"created SMA($n)")

  def peek(price: Double) = {
    if (prices.size < n) {
      (current * prices.size + price) / (prices.size + 1)
    } else {
      (current * n - prices.last + price) / n
    }
  }

  override def update(price: Double) {
    if (prices.size < n) {
      current = (current * prices.size + price) / (prices.size + 1)
    } else {
      current = (current * n - prices.last + price) / n
      prices = prices.dropRight(1)
    }
    prices = price :: prices
  }
}
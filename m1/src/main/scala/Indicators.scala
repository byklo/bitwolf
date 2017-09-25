package bitwolf


class ExponentialMovingAverage(n: Int) {
  // n:				period
  // intial ema:	sma(n)
  // multiplier:	(2 / (n + 1))
  // EMA:			{Close - EMA(previous day)} x multiplier + EMA(previous day)

  val k = 2.0 / (n + 1)
  val sma = new SimpleMovingAverage(n)
  var current = 0.0

  def update(price: Double) {
    if (sma.prices.size < n) {
      sma.update(price)
      current = sma.current
    } else {
      current = (price - current) * k + current
    }
  }
}

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
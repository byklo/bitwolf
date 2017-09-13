package candles

import play.api.libs.json.JsValue


case class Candle (
  timestamp: Long,
  duration: Long,
  high: Double,
  low: Double,
  open: Double,
  close: Double,
  units: Double
)


case class ExecutedTrade (
  timestamp: Long,
  price: Double,
  units: Double
)

object ExecutedTrade {
  def apply(json: JsValue): ExecutedTrade = ExecutedTrade(
    // expects [ 570381, "te", "11867708-BTCUSD", 1505224405, 4253.9, -0.37758208 ]
    (json \ 3).as[Long],
    (json \ 4).as[Double],
    (json \ 5).as[Double]
  )
}
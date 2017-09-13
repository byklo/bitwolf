package bitwolf

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
) {

  def prettyDate = {
    import java.util.Date
    import java.text.SimpleDateFormat
    (new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z")).format(new Date(timestamp * 1000))
  }

  def pretty = s"[$prettyDate] ${if (units >= 0.0) "BUY" else "SELL"} $price ($units)"
}

object ExecutedTrade {
  def apply(json: JsValue): Option[ExecutedTrade] = for {
    // expects [ 570381, "te", "11867708-BTCUSD", 1505224405, 4253.9, -0.37758208 ]
    timestamp <- (json \ 3).asOpt[Long]
    price <- (json \ 4).asOpt[Double]
    units <- (json \ 5).asOpt[Double]
  } yield ExecutedTrade(timestamp, price, units)
}
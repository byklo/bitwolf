import org.scalatest.FlatSpec
import scala.math.round
import scala.util.Random

import bitwolf.{SMA, EMA}


// the ema/sma example used as a test is from
// http://d.stockcharts.com/school/data/media/chart_school/technical_indicators_and_overlays/moving_averages/mova-1-sprdsheet.png

class EMASpec extends FlatSpec {
	"EMA" should "equal SMA for first n values" in {
		val n = (Random.nextFloat * 50).toInt + 10
		val sma10 = new SMA(n)
		val ema10 = EMA(n)

		(0 until n).foreach{ x =>
			val price = Random.nextDouble
			sma10.update(price)
			ema10.update(price)
			assert(sma10.current == ema10.current)
		}
	}

	"EMA" should "match the EMA10 example given" in {
		val n = 10
		val inputPrices = List(22.27, 22.19, 22.08, 22.17, 22.18, 22.13, 22.23, 22.43, 22.24, 22.29, 22.15, 22.39, 22.38, 22.61, 23.36,
			24.05, 23.75, 23.83, 23.95, 23.63, 23.82, 23.87)
		val expectedEMAs = List(22.22, 22.21, 22.24, 22.27, 22.33, 22.52, 22.80, 22.97, 23.13, 23.28, 23.34, 23.43, 23.51)

		val ema10 = EMA(n)

		inputPrices.take(n - 1).foreach{ ema10.update(_) }

		inputPrices.drop(n - 1).zip(expectedEMAs).foreach{
			case (price: Double, expected: Double) =>
				ema10.update(price)
				val rounded = round(ema10.current * 100) / 100.0
				assert(rounded == expected)
		}
	}
}

class SMASpec extends FlatSpec {
	"SMA" should "match the SMA10 example given" in {
		val n = 10
		val inputPrices = List(22.27, 22.19, 22.08, 22.17, 22.18, 22.13, 22.23, 22.43, 22.24, 22.29, 22.15, 22.39, 22.38, 22.61, 23.36,
			24.05, 23.75, 23.83, 23.95, 23.63, 23.82, 23.87)
		val expectedSMAs = List(22.22, 22.21, 22.23, 22.26, 22.3, 22.42, 22.61, 22.77, 22.91, 23.08, 23.21, 23.38, 23.53)

		val sma10 = new SMA(n)

		inputPrices.take(n - 1).foreach{ sma10.update(_) }

		inputPrices.drop(n - 1).zip(expectedSMAs).foreach{
			case (price: Double, expected: Double) =>
				sma10.update(price)
				val rounded = round(sma10.current * 100) / 100.0
				assert(rounded == expected)
		}
	}
}

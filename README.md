# bitwolf
a bitfinex trading bot built in scala.
focusing mainly on btc/usd at the moment.
strategy will be based on mean reversion (EMAs).

## progress
1. ~~connect to bitfinex websocket api (https://bitfinex.readme.io/v1/docs/ws-general)~~
2. ~~read incoming trades and build candles for arbitrary intervals~~
3. ~~simple moving averages~~
4. ~~exponential moving averages~~
5. ~~be able to read trades from a csv~~
6. develop strategy, state logic, buy/sell signals
7. backtest, optimize strategy
8. implement trade execution

## usage
root level directory is intended to hold different scala projects.
`candles` is just a POC of candle building logic.
`m1` holds the current and first iteration of the bot.
`m1` has 2 entry points: `Backtest` and `Bitwolf`.

under `bitwolf/m1`:
- `sbt run --help` for usage for both entry points
- `sbt test` to run tests

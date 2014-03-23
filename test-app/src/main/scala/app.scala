import com.alexknvl.btce.api._
import com.alexknvl.btce.site._

object App {
  def main(args: Array[String]) {
    val publicApi = new PublicApi()

    publicApi.info()
    publicApi.fee(Pair("btc_usd"))

    publicApi.ticker(Pair("xxx_xxx"))

    publicApi.ticker(Pair("btc_usd"))
    publicApi.depth(Pair("btc_usd"))
    publicApi.trades(Pair("btc_usd"))

    publicApi.ticker(List(Pair("btc_usd"), Pair("xxx_xxx")), true)
    publicApi.depth(List(Pair("btc_usd"), Pair("xxx_xxx")), true)
    publicApi.trades(List(Pair("btc_usd"), Pair("xxx_xxx")), true)

    val siteApi = new SiteApi()
    siteApi.scrape("en")
    siteApi.scrape("ru")
    siteApi.scrape
  }
}
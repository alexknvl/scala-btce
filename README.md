# Scala BTC-e Library

# Features

* Public API v3 (https://btc-e.com/api/3/...)
* [Trade API](https://btc-e.com/api/documentation)
* Main page scraping
* Chat client

# Public API 

```Scala
> import com.alexknvl.btce.api.PublicApi
> val publicApi = new PublicApi
> publicApi.info()
Right(
  Info(serverTime = 1395615339,
    Map(
      Pair("xpm_btc") -> PairInfo(
        decimalPlaces = 5,
        minPrice = 0.0001,
        maxAmount = 10,
        minAmount = 0.1,
        hidden = false,
        fee = 0.2
      ),    
      ...
    )
  )
)
```

```Scala
> publicApi.fee(Pair("btc_usd"))
Right(0.2)
```

```Scala
> publicApi.ticker(Pair("btc_usd"))
Right(
  Ticker(
    high = 577,
    low = 562.35101,
    avg = 569.675505,
    vol = 1302814.76128,
    vol_cur = 2288.89544,
    last = 567,
    buy = 569.395,
    sell = 567,
    updated = 1395615990
  )
)
```

```Scala
> publicApi.depth(Pair("btc_usd"))
Right(
  Depth(
    asks = List(
      (569.198,0.0105), 
      (569.199,3.15),
      ...
    ),
    bids = List(
      (560.5,0.2), 
      (560.3,0.01),
    )
  )
)
```

```Scala
> publicApi.trades(Pair("btc_usd"))
Right(
  List(
    Trade(
      tpe = "ask",
      price = 566.6,
      amount = 0.25,
      tid = 33146553,
      timestamp = 1395616525
    ), 
    ...
  )
)
```

# Trade API 

```Scala
> import com.alexknvl.btce.api.TradeApi
> val tradeApi = new TradeApi(API_KEY, API_SECRET)
> tradeApi.accountInfo
Right(
  AccountInfo(
    funds = Funds(
      usd = 0.33,
      btc = 0.57,
      ltc = 0,
      nmc = 0,
      ...
    ),
    rights = Rights(
      info = true,
      trade = true,
      withdraw = false
    ),
    transactionCount = 87,
    openOrders = 9,
    serverTime = 1395616525
  )
)
```

```Scala
> tradeApi.transactionHistory(fromId = 30000, order = "DESC")
Right(
  Map(
    123214 -> TransactionHistoryEntry(
      tpe = 0,
      amount = 0.1,
      currency = "usd",
      desc = "",
      status = 0,
      timestamp = 1395616525
    ),
    ...
  )
)
```

```Scala
> tradeApi.tradeHistory()
Right(
  Map(
    123214 -> TradeHistoryEntry(
      orderId = 12345
      pair = Pair("btc_usd"),
      tpe = "buy",
      amount = 0.1,
      rate = 600.0,
      isMine = true,
      timestamp = 1395616525
    ),
    ...
  )
)
```

```Scala
> tradeApi.activeOrders()
Right(
  Map(
    123214 -> OrderListEntry(
      pair = Pair("btc_usd"),
      tpe = "buy",
      amount = 0.1,
      rate = 600.0,
      createdTimestamp = 1395616525,
      status = 0
    ),
    ...
  )
)
```

```Scala
> tradeApi.trade(Pair("btc_usd"), "buy", 600.0, 0.1)
Right(
  TradeResponse(
    received = 0.0,
    remains = 0,
    orderId = 12341,
    funds = Funds(
      usd = 0.30,
      ...
    )
  )
)
```

```Scala
> tradeApi.cancelOrder(1234)
CancelOrderResponse(
  1234,
  funds = Funds(
    usd = 0.30,
    ...
  )
)
```

# API error handling
```Scala
> publicApi.fee(Pair("xxx_xxx"))
Left(InvalidPairName(pair = Pair("xxx_xxx")))
```

# Site scraping

```Scala
> import com.alexknvl.btce.site.SiteApi
> val siteApi = new SiteApi
> siteApi.scrape
ScrapingResult(
  messages = List(
    ChatMessage(
      id = 12600126,
      time = "24.03.14 03:15:05",
      user = "japandrew73",
      message = "everyone dumppppp"
    ),
    ...
  ),
  userCount = 3695,
  botCount = 855,
  isDevOnline = false,
  isSupportOnline = false,
  isAdminOnline = false
)
```

# Chat API

# Donation
If you find this library useful and would like to donate, please send some coins here:

<table>
  <tr>
    <td><strong>BTC</strong></td>
    <td>1FSGZajWeSfZuuQMgvVSCYZf9B1AhJbmzg</td>
  </tr>
  <tr>
    <td><strong>LTC</strong></td>
    <td>LMup82KHyGFm7fj3HorAVRjDJoXkUTMhDp</td>
  </tr>
  <tr>
    <td><strong>DOGE</strong></td>
    <td>DB1Fwe3Tp9ARdkiPiS4HKYVKsQsS3LmERx</td>
  </tr>
</table>
   

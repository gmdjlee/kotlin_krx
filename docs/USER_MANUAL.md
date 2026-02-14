# KrxKt User Manual

> pykrx(Python KRX Library)의 Native Kotlin 구현체.
> Python 런타임 없이 한국거래소(KRX) 시장 데이터에 직접 접근.

---

## Table of Contents

1. [Setup](#1-setup)
2. [Quick Start](#2-quick-start)
3. [KrxStock - 주식 데이터](#3-krxstock---주식-데이터)
4. [KrxEtf - ETF 데이터](#4-krxetf---etf-데이터)
5. [KrxIndex - 지수 데이터](#5-krxindex---지수-데이터)
6. [Data Models](#6-data-models)
7. [Enums](#7-enums)
8. [Error Handling](#8-error-handling)
9. [Network Requirements](#9-network-requirements)
10. [pykrx Compatibility Map](#10-pykrx-compatibility-map)
11. [Android Integration](#11-android-integration)
12. [Testing](#12-testing)

---

## 1. Setup

### Gradle 의존성 (settings.gradle.kts)

kotlin_krx를 서브모듈 또는 로컬 프로젝트로 포함:

```kotlin
// settings.gradle.kts
include(":krxkt")
project(":krxkt").projectDir = file("../kotlin_krx")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":krxkt"))
}
```

### 필수 라이브러리 (자동 전이)

| Library | Version | Purpose |
|---------|---------|---------|
| OkHttp | 4.12.0 | HTTP client (`api` scope - 전이됨) |
| Gson | 2.10.1 | JSON parsing |
| kotlinx-coroutines-core | 1.7.3 | Coroutine support (`api` scope - 전이됨) |
| kotlinx-datetime | 0.5.0 | Date handling |

### 요구 사항

- JVM 17+
- Kotlin 2.1.0+
- 한국 네트워크 접속 (KRX API 제한)

---

## 2. Quick Start

```kotlin
import com.krxkt.KrxStock
import com.krxkt.KrxEtf
import com.krxkt.KrxIndex
import com.krxkt.model.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val krxStock = KrxStock()
    val krxEtf = KrxEtf()
    val krxIndex = KrxIndex()

    try {
        // 삼성전자 기간 OHLCV
        val history = krxStock.getOhlcvByTicker("20210101", "20210131", "005930")
        history.forEach { println("${it.date}: 종가=${it.close}") }

        // KOSPI 200 지수
        val kospi200 = krxIndex.getKospi200("20210101", "20210131")
        kospi200.forEach { println("${it.date}: ${it.close}") }

        // 영업일 목록
        val bizDays = krxIndex.getBusinessDays("20210101", "20210131")
        println("1월 영업일: ${bizDays.size}일")
    } finally {
        krxStock.close()
        krxEtf.close()
        krxIndex.close()
    }
}
```

### 날짜 형식

모든 날짜 파라미터는 `yyyyMMdd` 형식의 `String`을 사용합니다.

```kotlin
"20210122"  // 2021년 1월 22일
"20250214"  // 2025년 2월 14일
```

### 리소스 관리

모든 API 클래스는 내부 OkHttpClient를 사용하므로, 사용 후 `close()`를 호출하세요.

```kotlin
val krxStock = KrxStock()
try {
    // ... API 호출
} finally {
    krxStock.close()
}
```

KrxClient를 공유하면 리소스 절약 가능:

```kotlin
val client = KrxClient()
val krxStock = KrxStock(client)
val krxIndex = KrxIndex(client)

try {
    // ... API 호출
} finally {
    client.close()  // 한 번만 close
}
```

---

## 3. KrxStock - 주식 데이터

### 생성자

```kotlin
class KrxStock(
    client: KrxClient = KrxClient(),
    tickerCache: TickerCache = TickerCache()
)
```

### 3.1 전종목 OHLCV 조회

특정일 전종목 시세 데이터. pykrx: `stock.get_market_ohlcv("20210122")`

```kotlin
suspend fun getMarketOhlcv(
    date: String,                    // 조회 날짜 (yyyyMMdd)
    market: Market = Market.ALL      // 시장 구분
): List<MarketOhlcv>
```

**반환**: `MarketOhlcv` - ticker, name, open, high, low, close, volume, tradingValue, changeRate

```kotlin
val ohlcvList = krxStock.getMarketOhlcv("20210122")
val kospiOnly = krxStock.getMarketOhlcv("20210122", Market.KOSPI)

ohlcvList.forEach { stock ->
    println("${stock.ticker} ${stock.name}: ${stock.close}원 (${stock.changeRate}%)")
}
```

### 3.2 개별종목 OHLCV 기간 조회

특정 종목의 날짜별 OHLCV. pykrx: `stock.get_market_ohlcv("20210101", "20210131", "005930")`

```kotlin
suspend fun getOhlcvByTicker(
    startDate: String,    // 시작 날짜
    endDate: String,      // 종료 날짜
    ticker: String        // 종목코드 (예: "005930")
): List<StockOhlcvHistory>
```

**반환**: `StockOhlcvHistory` - date, open, high, low, close, volume, tradingValue, changeRate

```kotlin
val samsung = krxStock.getOhlcvByTicker("20210101", "20210131", "005930")
samsung.forEach { day ->
    println("${day.date}: 시=${day.open} 고=${day.high} 저=${day.low} 종=${day.close}")
}
```

> 1년 초과 기간은 자동 분할 조회됩니다 (KRX API 제한 대응).

### 3.3 전종목 시가총액 조회

pykrx: `stock.get_market_cap("20210122")`

```kotlin
suspend fun getMarketCap(
    date: String,
    market: Market = Market.ALL
): List<MarketCap>
```

**반환**: `MarketCap` - ticker, name, close, changeRate, marketCap, sharesOutstanding

```kotlin
val caps = krxStock.getMarketCap("20210122")
caps.sortedByDescending { it.marketCap }.take(10).forEach { stock ->
    println("${stock.name}: 시총=${stock.marketCap}원, 주식수=${stock.sharesOutstanding}")
}
```

### 3.4 전종목 투자지표 조회

pykrx: `stock.get_market_fundamental("20210122")`

```kotlin
suspend fun getMarketFundamental(
    date: String,
    market: Market = Market.ALL
): List<StockFundamental>
```

**반환**: `StockFundamental` - ticker, name, close, eps, per, bps, pbr, dps, dividendYield

```kotlin
val fundamentals = krxStock.getMarketFundamental("20210122")
fundamentals.filter { it.per > 0 }.sortedBy { it.per }.take(10).forEach { f ->
    println("${f.name}: PER=${f.per}, PBR=${f.pbr}, 배당률=${f.dividendYield}%")
}
```

### 3.5 종목 리스트 조회

pykrx: `stock.get_market_ticker_list("20210122")`

```kotlin
suspend fun getTickerList(
    date: String,
    market: Market = Market.ALL
): List<TickerInfo>
```

**반환**: `TickerInfo` - ticker, name, marketName, isinCode

```kotlin
val tickers = krxStock.getTickerList("20210122", Market.KOSPI)
tickers.forEach { println("${it.ticker} ${it.name} (${it.marketName})") }
```

### 3.6 투자자별 거래실적 - 전체시장

pykrx: `stock.get_market_trading_value_and_volume_on_market_by_date()`

```kotlin
suspend fun getMarketTradingByInvestor(
    startDate: String,
    endDate: String,
    market: Market = Market.ALL,
    valueType: TradingValueType = TradingValueType.VALUE,   // VALUE=거래대금, VOLUME=거래량
    askBidType: AskBidType = AskBidType.NET_BUY             // SELL, BUY, NET_BUY
): List<InvestorTrading>
```

**반환**: `InvestorTrading` - date, financialInvestment, insurance, investmentTrust, privateEquity, bank, otherFinance, pensionFund, institutionalTotal, otherCorporation, individual, foreigner, total

```kotlin
val trading = krxStock.getMarketTradingByInvestor("20210118", "20210122")
trading.forEach { day ->
    println("${day.date}: 외국인=${day.foreigner}, 개인=${day.individual}, 기관=${day.institutionalTotal}")
}
```

### 3.7 투자자별 거래실적 - 개별종목

pykrx: `stock.get_market_trading_value_and_volume_on_ticker_by_date()`

```kotlin
suspend fun getTradingByInvestor(
    startDate: String,
    endDate: String,
    ticker: String,                                           // 종목코드
    valueType: TradingValueType = TradingValueType.VALUE,
    askBidType: AskBidType = AskBidType.NET_BUY
): List<InvestorTrading>
```

```kotlin
val samsungTrading = krxStock.getTradingByInvestor("20210118", "20210122", "005930")
samsungTrading.forEach { day ->
    println("${day.date}: 외국인 순매수=${day.foreignerNetBuy}")
}
```

### 3.8 공매도 거래 - 전종목

pykrx: `stock.get_shorting_volume_by_ticker()`

```kotlin
suspend fun getShortSellingAll(
    date: String,
    market: Market = Market.KOSPI
): List<ShortSelling>
```

**반환**: `ShortSelling` - ticker, name, shortVolume, shortValue, totalVolume, totalValue, volumeRatio

```kotlin
val shorts = krxStock.getShortSellingAll("20210122")
shorts.sortedByDescending { it.volumeRatio ?: 0.0 }.take(10).forEach { s ->
    println("${s.name}: 공매도비중=${s.volumeRatio}%, 공매도량=${s.shortVolume}")
}
```

### 3.9 공매도 거래 - 개별종목 기간

pykrx: `stock.get_shorting_volume_by_date()`

```kotlin
suspend fun getShortSellingByTicker(
    startDate: String,
    endDate: String,
    ticker: String
): List<ShortSellingHistory>
```

**반환**: `ShortSellingHistory` - date, shortVolume, shortValue, totalVolume, totalValue
- 계산 속성: `volumeRatio`, `valueRatio`

```kotlin
val shortHistory = krxStock.getShortSellingByTicker("20210118", "20210122", "005930")
shortHistory.forEach { day ->
    println("${day.date}: 공매도=${day.shortVolume}주 (${day.volumeRatio}%)")
}
```

### 3.10 공매도 잔고 - 전종목

pykrx: `stock.get_shorting_balance_by_ticker()`

```kotlin
suspend fun getShortBalanceAll(
    date: String,
    market: Market = Market.KOSPI
): List<ShortBalance>
```

**반환**: `ShortBalance` - ticker, name, balanceQuantity, balanceAmount, listedShares, balanceRatio

### 3.11 공매도 잔고 - 개별종목 기간

pykrx: `stock.get_shorting_balance_by_date()`

```kotlin
suspend fun getShortBalanceByTicker(
    startDate: String,
    endDate: String,
    ticker: String
): List<ShortBalanceHistory>
```

**반환**: `ShortBalanceHistory` - date, balanceQuantity, balanceAmount, listedShares, balanceRatio

---

## 4. KrxEtf - ETF 데이터

### 생성자

```kotlin
class KrxEtf(
    client: KrxClient = KrxClient(),
    tickerCache: TickerCache = TickerCache()
)
```

### 4.1 전종목 ETF 시세

pykrx: `etf.get_etf_ohlcv_by_ticker("20210122")`

```kotlin
suspend fun getEtfPrice(date: String): List<EtfPrice>
```

**반환**: `EtfPrice` - ticker, name, nav, open, high, low, close, volume, tradingValue, underlyingIndex, changeRate

```kotlin
val etfPrices = krxEtf.getEtfPrice("20210122")
etfPrices.take(5).forEach { etf ->
    println("${etf.ticker} ${etf.name}: 종가=${etf.close}, NAV=${etf.nav}")
}
```

### 4.2 개별 ETF OHLCV 기간 조회

pykrx: `etf.get_etf_ohlcv_by_date("20210101", "20210131", "069500")`

```kotlin
suspend fun getOhlcvByTicker(
    startDate: String,
    endDate: String,
    ticker: String       // ETF 종목코드 (예: "069500")
): List<EtfOhlcvHistory>
```

**반환**: `EtfOhlcvHistory` - date, nav, open, high, low, close, volume, tradingValue, underlyingIndex

```kotlin
val kodex200 = krxEtf.getOhlcvByTicker("20210101", "20210131", "069500")
kodex200.forEach { day ->
    println("${day.date}: 종가=${day.close}, NAV=${day.nav}")
}
```

### 4.3 ETF 종목 리스트

```kotlin
suspend fun getEtfTickerList(date: String): List<EtfInfo>
```

**반환**: `EtfInfo` - ticker, name, isinCode, indexName, targetIndexName, indexProvider, cu, totalFee

```kotlin
val etfList = krxEtf.getEtfTickerList("20210122")
etfList.forEach { etf ->
    println("${etf.ticker} ${etf.name}: 보수율=${etf.totalFee}%")
}
```

### 4.4 ETF 이름 조회

```kotlin
suspend fun getEtfName(ticker: String, date: String): String?
```

```kotlin
val name = krxEtf.getEtfName("069500", "20210122")
println(name)  // "KODEX 200"
```

### 4.5 ETF 구성종목 (PDF)

pykrx: `etf.get_etf_portfolio_deposit_file("20210122", "069500")`

```kotlin
suspend fun getPortfolio(
    date: String,
    ticker: String
): List<EtfPortfolio>
```

**반환**: `EtfPortfolio` - ticker, name, shares, valuationAmount, amount, weight

```kotlin
val portfolio = krxEtf.getPortfolio("20210122", "069500")
portfolio.sortedByDescending { it.weight ?: 0.0 }.take(10).forEach { p ->
    println("${p.ticker} ${p.name}: 비중=${p.weight}%, 금액=${p.amount}")
}
```

---

## 5. KrxIndex - 지수 데이터

### 생성자

```kotlin
class KrxIndex(
    client: KrxClient = KrxClient()
)
```

### 지수 티커 상수

```kotlin
KrxIndex.TICKER_KOSPI        // "1001" - KOSPI
KrxIndex.TICKER_KOSPI_200    // "1028" - KOSPI 200
KrxIndex.TICKER_KOSPI_LARGE  // "1002" - KOSPI 대형주
KrxIndex.TICKER_KOSPI_MID    // "1003" - KOSPI 중형주
KrxIndex.TICKER_KOSPI_SMALL  // "1004" - KOSPI 소형주
KrxIndex.TICKER_KOSDAQ        // "2001" - KOSDAQ
KrxIndex.TICKER_KOSDAQ_150    // "2203" - KOSDAQ 150
```

**티커 구조**: `[타입코드 1자리][지수코드 3자리]`
- 타입코드: 1=KOSPI, 2=KOSDAQ, 3=파생, 4=테마
- 예: "1028" = KOSPI(1) + 028 = KOSPI 200

### 5.1 지수 OHLCV 기간 조회

pykrx: `index.get_index_ohlcv("20210101", "20210131", "1028")`

```kotlin
suspend fun getOhlcvByTicker(
    startDate: String,
    endDate: String,
    ticker: String       // 지수 티커 (예: "1028")
): List<IndexOhlcv>
```

**반환**: `IndexOhlcv` - date, open, high, low, close, volume, tradingValue, changeType, change
- 계산 속성: `isUp`, `isDown`, `isUnchanged`

```kotlin
val kospi200 = krxIndex.getOhlcvByTicker("20210101", "20210131", "1028")
kospi200.forEach { day ->
    val direction = when {
        day.isUp -> "+"
        day.isDown -> "-"
        else -> "="
    }
    println("${day.date}: ${day.close} ($direction${day.change})")
}
```

### 5.2 편의 메서드 (주요 지수)

```kotlin
suspend fun getKospi(startDate: String, endDate: String): List<IndexOhlcv>
suspend fun getKospi200(startDate: String, endDate: String): List<IndexOhlcv>
suspend fun getKosdaq(startDate: String, endDate: String): List<IndexOhlcv>
suspend fun getKosdaq150(startDate: String, endDate: String): List<IndexOhlcv>
```

```kotlin
val kospi = krxIndex.getKospi("20210101", "20210131")
val kosdaq = krxIndex.getKosdaq("20210101", "20210131")
```

### 5.3 전종목 지수 OHLCV (특정일)

pykrx: `index.get_index_ohlcv_by_ticker("20210122", "KOSPI")`

```kotlin
suspend fun getIndexOhlcv(
    date: String,
    market: IndexMarket = IndexMarket.KOSPI
): List<IndexOhlcvByTicker>
```

**반환**: `IndexOhlcvByTicker` - name, open, high, low, close, volume, tradingValue, marketCap, changeType, change, changeRate
- 계산 속성: `isUp`, `isDown`, `isUnchanged`

```kotlin
val allIndex = krxIndex.getIndexOhlcv("20210122", IndexMarket.KOSPI)
allIndex.forEach { idx ->
    println("${idx.name}: ${idx.close} (${idx.changeRate}%)")
}
```

### 5.4 지수 목록 조회

```kotlin
suspend fun getIndexList(
    date: String,
    market: IndexMarket = IndexMarket.ALL
): List<IndexInfo>
```

**반환**: `IndexInfo` - ticker, code, name, typeCode, baseDate
- 계산 속성: `isKospi`, `isKosdaq`, `isDerivatives`, `isTheme`

```kotlin
val indexList = krxIndex.getIndexList("20210122")
indexList.filter { it.isKospi }.forEach { idx ->
    println("${idx.ticker} ${idx.name}")
}
```

### 5.5 지수 이름 조회

```kotlin
suspend fun getIndexName(ticker: String, date: String): String?
```

```kotlin
val name = krxIndex.getIndexName("1028", "20210122")
println(name)  // "코스피 200"
```

### 5.6 지수 구성종목 조회

pykrx: `index.get_index_portfolio_deposit_file("1028", "20210122")`

```kotlin
suspend fun getIndexPortfolio(
    date: String,
    ticker: String
): List<IndexPortfolio>
```

**반환**: `IndexPortfolio` - ticker, name, close, changeType, change, changeRate, marketCap

```kotlin
val kospi200Stocks = krxIndex.getIndexPortfolio("20210122", "1028")
println("KOSPI 200 구성종목: ${kospi200Stocks.size}개")
kospi200Stocks.take(10).forEach { p ->
    println("  ${p.ticker} ${p.name}: ${p.close}원 (${p.changeRate}%)")
}
```

### 5.7 지수 구성종목 티커만 조회

pykrx 호환 (returns list of tickers)

```kotlin
suspend fun getIndexPortfolioTickers(
    date: String,
    ticker: String
): List<String>
```

```kotlin
val tickers = krxIndex.getIndexPortfolioTickers("20210122", "1028")
println(tickers)  // ["005930", "000660", "051910", ...]
```

### 5.8 최근 영업일 조회

pykrx: `get_nearest_business_day_in_a_week(date, prev)`

지정일이 영업일이면 그대로 반환. 아니면 7일 범위 내에서 가장 가까운 영업일 탐색.

```kotlin
suspend fun getNearestBusinessDay(
    date: String,
    prev: Boolean = true   // true=이전 영업일, false=다음 영업일
): String
```

```kotlin
// 2021-01-23 (토요일)
val prevBizDay = krxIndex.getNearestBusinessDay("20210123", prev = true)
println(prevBizDay)  // "20210122" (금요일)

val nextBizDay = krxIndex.getNearestBusinessDay("20210123", prev = false)
println(nextBizDay)  // "20210125" (월요일)

// 영업일은 그대로 반환
val friday = krxIndex.getNearestBusinessDay("20210122", prev = true)
println(friday)  // "20210122"
```

> 7일 내 영업일이 없으면 `IllegalStateException` 발생

### 5.9 기간 내 영업일 목록 조회

pykrx: `get_previous_business_days(fromdate, todate)`

```kotlin
suspend fun getBusinessDays(
    startDate: String,
    endDate: String
): List<String>
```

```kotlin
val bizDays = krxIndex.getBusinessDays("20210118", "20210124")
println(bizDays)  // ["20210118", "20210119", "20210120", "20210121", "20210122"]
```

### 5.10 월별 영업일 목록 조회

pykrx: `get_previous_business_days(year, month)`

```kotlin
suspend fun getBusinessDaysByMonth(
    year: Int,      // 연도 (1990-2100)
    month: Int      // 월 (1-12)
): List<String>
```

```kotlin
val janDays = krxIndex.getBusinessDaysByMonth(2021, 1)
println("2021년 1월 영업일: ${janDays.size}일")
println("첫 영업일: ${janDays.first()}")
println("마지막 영업일: ${janDays.last()}")
```

---

## 6. Data Models

모든 데이터 모델은 `com.krxkt.model` 패키지의 `data class`입니다.

### Stock Models

| Class | Fields | Usage |
|-------|--------|-------|
| `MarketOhlcv` | ticker, name, open, high, low, close, volume, tradingValue, changeRate | 전종목 OHLCV |
| `StockOhlcvHistory` | date, open, high, low, close, volume, tradingValue, changeRate | 개별종목 기간 OHLCV |
| `MarketCap` | ticker, name, close, changeRate, marketCap, sharesOutstanding | 시가총액 |
| `StockFundamental` | ticker, name, close, eps, per, bps, pbr, dps, dividendYield | PER/PBR 등 |
| `TickerInfo` | ticker, name, marketName, isinCode | 종목 목록 |
| `InvestorTrading` | date, financialInvestment, insurance, ..., individual, foreigner, total | 투자자별 거래 |
| `ShortSelling` | ticker, name, shortVolume, shortValue, totalVolume, totalValue, volumeRatio | 공매도 (전종목) |
| `ShortSellingHistory` | date, shortVolume, shortValue, totalVolume, totalValue | 공매도 (기간) |
| `ShortBalance` | ticker, name, balanceQuantity, balanceAmount, listedShares, balanceRatio | 공매도 잔고 (전종목) |
| `ShortBalanceHistory` | date, balanceQuantity, balanceAmount, listedShares, balanceRatio | 공매도 잔고 (기간) |

### ETF Models

| Class | Fields | Usage |
|-------|--------|-------|
| `EtfPrice` | ticker, name, nav, open, high, low, close, volume, tradingValue, underlyingIndex, changeRate | 전종목 ETF 시세 |
| `EtfOhlcvHistory` | date, nav, open, high, low, close, volume, tradingValue, underlyingIndex | 개별 ETF 기간 OHLCV |
| `EtfInfo` | ticker, name, isinCode, indexName, targetIndexName, indexProvider, cu, totalFee | ETF 종목 정보 |
| `EtfPortfolio` | ticker, name, shares, valuationAmount, amount, weight | ETF 구성종목 |

### Index Models

| Class | Fields | Usage |
|-------|--------|-------|
| `IndexOhlcv` | date, open, high, low, close, volume, tradingValue, changeType, change | 지수 기간 OHLCV |
| `IndexOhlcvByTicker` | name, open, high, low, close, volume, tradingValue, marketCap, changeType, change, changeRate | 전종목 지수 (특정일) |
| `IndexInfo` | ticker, code, name, typeCode, baseDate | 지수 목록 |
| `IndexPortfolio` | ticker, name, close, changeType, change, changeRate, marketCap | 지수 구성종목 |

### 타입 참고

- 주식 가격: `Long` (원 단위, 예: `82200`)
- 지수: `Double` (소수점 포함, 예: `3062.52`)
- 등락률: `Double` (%, 예: `-0.50`)
- 거래량/주식수: `Long`
- Nullable 필드: API 응답에 값이 없을 수 있는 필드는 `?` 표기

---

## 7. Enums

### Market (주식/ETF 시장 구분)

```kotlin
enum class Market(val code: String) {
    KOSPI("STK"),      // 코스피
    KOSDAQ("KSQ"),     // 코스닥
    KONEX("KNX"),      // 코넥스
    ALL("ALL")         // 전체
}
```

### IndexMarket (지수 시장 구분)

```kotlin
enum class IndexMarket(val code: String, val krxCode: String) {
    ALL("", "01"),           // 전체
    KOSPI("1", "02"),        // KOSPI 계열
    KOSDAQ("2", "03"),       // KOSDAQ 계열
    DERIVATIVES("3", "04"),  // 파생상품 지수
    THEME("4", "04")         // 테마 지수
}
```

### TradingValueType (거래 유형)

```kotlin
enum class TradingValueType(val code: String) {
    VOLUME("1"),   // 거래량
    VALUE("2")     // 거래대금
}
```

### AskBidType (매수/매도)

```kotlin
enum class AskBidType(val code: String) {
    SELL("1"),      // 매도
    BUY("2"),       // 매수
    NET_BUY("3")    // 순매수
}
```

---

## 8. Error Handling

### 에러 타입

```kotlin
sealed class KrxError : Exception {
    class NetworkError       // 네트워크 에러 (재시도 가능)
    class ParseError         // JSON 파싱 에러
    class InvalidDateError   // 잘못된 날짜 형식
}
```

### 에러 처리 전략

| 상황 | 동작 | 처리 |
|------|------|------|
| 네트워크 에러 | 3회 재시도 (1s/2s/4s backoff) 후 `KrxError.NetworkError` | catch & retry 또는 사용자 알림 |
| JSON 파싱 실패 | 해당 항목 `null` 반환 → `mapNotNull`로 필터 | 빈 리스트 가능 |
| 잘못된 날짜 | `IllegalArgumentException` | 입력 검증 |
| 공휴일/휴장일 | 빈 리스트 반환 | 정상 응답 |
| LOGOUT 응답 | `IOException` (한국 네트워크 필요) | VPN 연결 확인 |
| 7일 내 영업일 없음 | `IllegalStateException` | 예외 처리 |

### 에러 처리 예시

```kotlin
try {
    val ohlcv = krxStock.getMarketOhlcv("20210122")
    if (ohlcv.isEmpty()) {
        println("데이터 없음 (공휴일이거나 휴장일)")
    }
} catch (e: KrxError.NetworkError) {
    println("네트워크 에러: ${e.message}")
    // 재시도 가능: e.isRetriable() == true
} catch (e: IllegalArgumentException) {
    println("잘못된 입력: ${e.message}")
}
```

---

## 9. Network Requirements

**KRX API는 한국 네트워크에서만 접근 가능합니다.**

| 환경 | 동작 |
|------|------|
| 한국 내 네트워크 | 정상 작동 |
| 해외 네트워크 | "LOGOUT" 응답 → `IOException` |
| 한국 VPN 사용 | 정상 작동 |

### LOGOUT 에러 발생 시

1. 한국 VPN 연결 확인
2. KRX 서비스 상태 확인 (http://data.krx.co.kr)
3. 세션 문제일 수 있음 - 새 KrxClient 인스턴스로 재시도

### 타임아웃 설정

기본 30초. 커스텀 OkHttpClient로 변경 가능:

```kotlin
val customClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

val krxClient = KrxClient(okHttpClient = customClient)
val krxStock = KrxStock(client = krxClient)
```

---

## 10. pykrx Compatibility Map

### Stock

| pykrx | KrxKt | Note |
|-------|-------|------|
| `stock.get_market_ohlcv("20210122")` | `krxStock.getMarketOhlcv("20210122")` | |
| `stock.get_market_ohlcv("20210101", "20210131", "005930")` | `krxStock.getOhlcvByTicker("20210101", "20210131", "005930")` | |
| `stock.get_market_cap("20210122")` | `krxStock.getMarketCap("20210122")` | |
| `stock.get_market_fundamental("20210122")` | `krxStock.getMarketFundamental("20210122")` | |
| `stock.get_market_ticker_list("20210122")` | `krxStock.getTickerList("20210122")` | |
| `stock.get_market_trading_value_by_date(...)` | `krxStock.getMarketTradingByInvestor(...)` | |
| `stock.get_market_trading_value_by_date(..., ticker)` | `krxStock.getTradingByInvestor(...)` | |
| `stock.get_shorting_volume_by_ticker(...)` | `krxStock.getShortSellingAll(...)` | 전종목 특정일 |
| `stock.get_shorting_volume_by_date(...)` | `krxStock.getShortSellingByTicker(...)` | 개별종목 기간 |
| `stock.get_shorting_balance_by_ticker(...)` | `krxStock.getShortBalanceAll(...)` | 전종목 특정일 |
| `stock.get_shorting_balance_by_date(...)` | `krxStock.getShortBalanceByTicker(...)` | 개별종목 기간 |

### ETF

| pykrx | KrxKt | Note |
|-------|-------|------|
| `etf.get_etf_ohlcv_by_ticker("20210122")` | `krxEtf.getEtfPrice("20210122")` | |
| `etf.get_etf_ohlcv_by_date("20210101", "20210131", "069500")` | `krxEtf.getOhlcvByTicker("20210101", "20210131", "069500")` | |
| `etf.get_etf_ticker_list("20210122")` | `krxEtf.getEtfTickerList("20210122")` | |
| `etf.get_etf_portfolio_deposit_file("20210122", "069500")` | `krxEtf.getPortfolio("20210122", "069500")` | |

### Index

| pykrx | KrxKt | Note |
|-------|-------|------|
| `index.get_index_ohlcv("20210101", "20210131", "1028")` | `krxIndex.getOhlcvByTicker("20210101", "20210131", "1028")` | |
| `index.get_index_ohlcv_by_ticker("20210122")` | `krxIndex.getIndexOhlcv("20210122")` | |
| `index.get_index_portfolio_deposit_file("1028")` | `krxIndex.getIndexPortfolio("20210122", "1028")` | |
| `index.get_index_portfolio_deposit_file("1028")` (tickers only) | `krxIndex.getIndexPortfolioTickers("20210122", "1028")` | |
| (N/A) | `krxIndex.getKospi(start, end)` | 편의 메서드 |
| (N/A) | `krxIndex.getKospi200(start, end)` | 편의 메서드 |
| (N/A) | `krxIndex.getKosdaq(start, end)` | 편의 메서드 |
| (N/A) | `krxIndex.getKosdaq150(start, end)` | 편의 메서드 |

### Business Days

| pykrx | KrxKt | Note |
|-------|-------|------|
| `get_nearest_business_day_in_a_week(date)` | `krxIndex.getNearestBusinessDay(date)` | |
| `get_nearest_business_day_in_a_week(date, prev=False)` | `krxIndex.getNearestBusinessDay(date, prev=false)` | |
| `get_previous_business_days(fromdate, todate)` | `krxIndex.getBusinessDays(start, end)` | |
| `get_previous_business_days(year=2021, month=1)` | `krxIndex.getBusinessDaysByMonth(2021, 1)` | |

---

## 11. Android Integration

### Hilt DI 설정 예시

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object KrxModule {

    @Provides
    @Singleton
    fun provideKrxClient(): KrxClient = KrxClient()

    @Provides
    @Singleton
    fun provideTickerCache(): TickerCache = TickerCache()

    @Provides
    @Singleton
    fun provideKrxStock(
        client: KrxClient,
        cache: TickerCache
    ): KrxStock = KrxStock(client, cache)

    @Provides
    @Singleton
    fun provideKrxEtf(
        client: KrxClient,
        cache: TickerCache
    ): KrxEtf = KrxEtf(client, cache)

    @Provides
    @Singleton
    fun provideKrxIndex(
        client: KrxClient
    ): KrxIndex = KrxIndex(client)
}
```

### ViewModel 사용 예시

```kotlin
@HiltViewModel
class StockViewModel @Inject constructor(
    private val krxStock: KrxStock,
    private val krxIndex: KrxIndex
) : ViewModel() {

    private val _ohlcvData = MutableStateFlow<List<MarketOhlcv>>(emptyList())
    val ohlcvData: StateFlow<List<MarketOhlcv>> = _ohlcvData

    fun loadMarketData(date: String) {
        viewModelScope.launch {
            try {
                _ohlcvData.value = krxStock.getMarketOhlcv(date)
            } catch (e: KrxError.NetworkError) {
                // 에러 처리
            }
        }
    }

    fun loadWithBusinessDay(date: String) {
        viewModelScope.launch {
            // 영업일 자동 보정
            val bizDay = krxIndex.getNearestBusinessDay(date, prev = true)
            _ohlcvData.value = krxStock.getMarketOhlcv(bizDay)
        }
    }
}
```

### ProGuard / R8 규칙

Gson reflection을 사용하지 않으므로 (수동 JSON 파싱) 특별한 ProGuard 규칙 불필요.
OkHttp의 기본 규칙만 포함:

```proguard
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
```

---

## 12. Testing

### 단위 테스트 (네트워크 불필요)

MockWebServer 기반, 어디서든 실행 가능:

```bash
# Windows
gradlew.bat test

# Unix/macOS
./gradlew test
```

### 통합 테스트 (한국 네트워크 필요)

실제 KRX API 호출:

```bash
# 특정 테스트 클래스 실행
./gradlew runIntegrationTest -PmainClass=com.krxkt.integration.IndexExtensionTestKt

# 기본 통합 테스트
./gradlew runIntegrationTest
```

### 테스트 작성 시 기준 데이터

| 항목 | 값 | 설명 |
|------|-----|------|
| 기준 날짜 | `"20210122"` | 과거 안정 데이터 (금요일) |
| 주식 종목 | `"005930"` | 삼성전자 |
| ETF 종목 | `"069500"` | KODEX 200 |
| KOSPI 지수 | `"1001"` | KOSPI |
| KOSPI 200 | `"1028"` | KOSPI 200 |
| 공휴일 | `"20210101"` | 빈 응답 확인용 |
| 주말 | `"20210123"` | 영업일 탐색 테스트용 (토요일) |

### MockWebServer 테스트 패턴

```kotlin
class MyTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var krxStock: KrxStock

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val client = KrxClient(
            baseUrl = mockWebServer.url("/").toString()
        )
        krxStock = KrxStock(client)
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
        krxStock.close()
    }

    @Test
    fun `test market ohlcv`() = runTest {
        val mockResponse = """
            {"OutBlock_1":[{
                "ISU_SRT_CD":"005930",
                "ISU_ABBRV":"삼성전자",
                "TDD_OPNPRC":"83,000",
                "TDD_HGPRC":"84,400",
                "TDD_LWPRC":"82,200",
                "TDD_CLSPRC":"82,200",
                "ACC_TRDVOL":"30,000,000",
                "ACC_TRDVAL":"2,500,000,000,000",
                "FLUC_RT":"-0.50"
            }]}
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(mockResponse))

        val result = krxStock.getMarketOhlcv("20210122")
        assertEquals(1, result.size)
        assertEquals("005930", result[0].ticker)
        assertEquals(82200L, result[0].close)
    }
}
```

---

## Work Summary

이 프로젝트에서 구현된 전체 기능 목록:

### Phase 1-2: Core Infrastructure
- KrxClient (OkHttp, retry, session management)
- KrxJsonParser (OutBlock_1 파싱, 쉼표 숫자 처리)
- KrxEndpoints (모든 BLD 엔드포인트 상수)

### Phase 3: Stock APIs
- getMarketOhlcv, getOhlcvByTicker, getMarketCap, getMarketFundamental, getTickerList

### Phase 4: ETF & Index APIs
- KrxEtf: getEtfPrice, getOhlcvByTicker, getEtfTickerList, getPortfolio
- KrxIndex: getOhlcvByTicker, getIndexList, getKospi/getKosdaq 편의 메서드

### Phase 5: Advanced Features
- Investor Trading: getMarketTradingByInvestor, getTradingByInvestor
- Short Selling: getShortSellingAll, getShortSellingByTicker, getShortBalanceAll, getShortBalanceByTicker

### Phase 6: pykrx Migration (5 functions)
- getIndexPortfolio, getIndexPortfolioTickers (MDCSTAT00601)
- getIndexOhlcv (MDCSTAT00101 - 전종목 지수 특정일)
- getNearestBusinessDay (MDCSTAT00301 기반 영업일 탐색)
- getBusinessDays, getBusinessDaysByMonth (기간 내 영업일 목록)

**총 30+ API 함수**, **25+ 데이터 모델**, **50+ 단위 테스트**

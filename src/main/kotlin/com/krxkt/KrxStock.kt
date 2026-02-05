package com.krxkt

import com.krxkt.api.KrxClient
import com.krxkt.api.KrxEndpoints
import com.krxkt.model.*
import com.krxkt.parser.KrxJsonParser
import com.krxkt.util.DateUtils

/**
 * KRX 주식 데이터 API
 *
 * pykrx의 stock 모듈과 호환되는 Kotlin 구현
 *
 * 사용 예:
 * ```
 * val krxStock = KrxStock()
 *
 * // 전종목 OHLCV
 * val ohlcvList = krxStock.getMarketOhlcv("20210122")
 *
 * // 개별종목 기간 조회
 * val history = krxStock.getOhlcvByTicker("20210101", "20210131", "005930")
 *
 * // 시가총액
 * val marketCaps = krxStock.getMarketCap("20210122")
 * ```
 *
 * @param client HTTP 클라이언트 (테스트용 주입 가능)
 */
class KrxStock(
    private val client: KrxClient = KrxClient()
) {
    /**
     * 전종목 OHLCV 조회
     *
     * pykrx: stock.get_market_ohlcv("20210122")
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param market 시장 구분 (기본: ALL)
     * @return 전종목 OHLCV 리스트 (공휴일/휴장일은 빈 리스트)
     */
    suspend fun getMarketOhlcv(
        date: String,
        market: Market = Market.ALL
    ): List<MarketOhlcv> {
        DateUtils.validateDate(date)

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.STOCK_OHLCV_ALL,
            "mktId" to market.code,
            "trdDd" to date
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { MarketOhlcv.fromJson(it) }
    }

    /**
     * 개별종목 OHLCV 기간 조회
     *
     * pykrx: stock.get_market_ohlcv("20210101", "20210131", "005930")
     *
     * @param startDate 시작 날짜 (yyyyMMdd)
     * @param endDate 종료 날짜 (yyyyMMdd)
     * @param ticker 종목코드 (예: "005930")
     * @return 날짜별 OHLCV 리스트 (최신순 정렬)
     */
    suspend fun getOhlcvByTicker(
        startDate: String,
        endDate: String,
        ticker: String
    ): List<StockOhlcvHistory> {
        DateUtils.validateDateRange(startDate, endDate)

        // ISIN 코드 조회 필요 (KRX API는 ISIN 사용)
        val isinCode = getIsinCode(ticker, endDate)
            ?: return emptyList()

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.STOCK_OHLCV_BY_TICKER,
            "isuCd" to isinCode,
            "strtDd" to startDate,
            "endDd" to endDate
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { StockOhlcvHistory.fromJson(it) }
    }

    /**
     * 전종목 시가총액 조회
     *
     * pykrx: stock.get_market_cap("20210122")
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param market 시장 구분 (기본: ALL)
     * @return 전종목 시가총액 리스트
     */
    suspend fun getMarketCap(
        date: String,
        market: Market = Market.ALL
    ): List<MarketCap> {
        DateUtils.validateDate(date)

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.MARKET_CAP,
            "mktId" to market.code,
            "trdDd" to date
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { MarketCap.fromJson(it) }
    }

    /**
     * 전종목 투자지표 조회
     *
     * pykrx: stock.get_market_fundamental("20210122")
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param market 시장 구분 (기본: ALL)
     * @return 전종목 펀더멘탈 리스트 (PER, PBR, EPS, BPS, DPS, 배당수익률)
     */
    suspend fun getMarketFundamental(
        date: String,
        market: Market = Market.ALL
    ): List<StockFundamental> {
        DateUtils.validateDate(date)

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.FUNDAMENTAL,
            "mktId" to market.code,
            "trdDd" to date
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { StockFundamental.fromJson(it) }
    }

    /**
     * 종목 리스트 조회
     *
     * pykrx: stock.get_market_ticker_list("20210122")
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param market 시장 구분 (기본: ALL)
     * @return 종목 정보 리스트
     */
    suspend fun getTickerList(
        date: String,
        market: Market = Market.ALL
    ): List<TickerInfo> {
        DateUtils.validateDate(date)

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.TICKER_LIST,
            "mktId" to market.code,
            "trdDd" to date
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { TickerInfo.fromJson(it) }
    }

    /**
     * 종목코드로 ISIN 코드 조회
     *
     * @param ticker 종목코드 (예: "005930")
     * @param date 기준 날짜
     * @return ISIN 코드 (예: "KR7005930003"), 없으면 null
     */
    internal suspend fun getIsinCode(ticker: String, date: String): String? {
        val tickerList = getTickerList(date, Market.ALL)
        return tickerList.find { it.ticker == ticker }?.isinCode
    }

    // ============================================================
    // 투자자별 거래실적 (Investor Trading)
    // ============================================================

    /**
     * 전체시장 투자자별 거래실적 (일별 추이)
     *
     * pykrx: stock.get_market_trading_value_and_volume_on_market_by_date()
     *
     * @param startDate 시작 날짜 (yyyyMMdd)
     * @param endDate 종료 날짜 (yyyyMMdd)
     * @param market 시장 구분 (기본: ALL)
     * @param valueType 거래량/거래대금 (기본: VALUE)
     * @param askBidType 매수/매도/순매수 (기본: NET_BUY)
     * @return 일별 투자자별 거래실적 리스트
     */
    suspend fun getMarketTradingByInvestor(
        startDate: String,
        endDate: String,
        market: Market = Market.ALL,
        valueType: TradingValueType = TradingValueType.VALUE,
        askBidType: AskBidType = AskBidType.NET_BUY
    ): List<InvestorTrading> {
        DateUtils.validateDateRange(startDate, endDate)

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.INVESTOR_TRADING_MARKET_DAILY,
            "strtDd" to startDate,
            "endDd" to endDate,
            "mktId" to market.code,
            "trdVolVal" to valueType.code,
            "askBid" to askBidType.code
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { InvestorTrading.fromJson(it) }
    }

    /**
     * 개별종목 투자자별 거래실적 (일별 추이)
     *
     * pykrx: stock.get_market_trading_value_and_volume_on_ticker_by_date()
     *
     * @param startDate 시작 날짜 (yyyyMMdd)
     * @param endDate 종료 날짜 (yyyyMMdd)
     * @param ticker 종목코드 (예: "005930")
     * @param valueType 거래량/거래대금 (기본: VALUE)
     * @param askBidType 매수/매도/순매수 (기본: NET_BUY)
     * @return 일별 투자자별 거래실적 리스트
     */
    suspend fun getTradingByInvestor(
        startDate: String,
        endDate: String,
        ticker: String,
        valueType: TradingValueType = TradingValueType.VALUE,
        askBidType: AskBidType = AskBidType.NET_BUY
    ): List<InvestorTrading> {
        DateUtils.validateDateRange(startDate, endDate)

        val isinCode = getIsinCode(ticker, endDate)
            ?: return emptyList()

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.INVESTOR_TRADING_TICKER_DAILY,
            "strtDd" to startDate,
            "endDd" to endDate,
            "isuCd" to isinCode,
            "trdVolVal" to valueType.code,
            "askBid" to askBidType.code
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { InvestorTrading.fromJson(it) }
    }

    // ============================================================
    // 공매도 (Short Selling)
    // ============================================================

    /**
     * 전종목 공매도 거래 현황 (특정일)
     *
     * pykrx: stock.get_shorting_volume_by_ticker()
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param market 시장 구분 (기본: KOSPI)
     * @return 전종목 공매도 거래 리스트
     */
    suspend fun getShortSellingAll(
        date: String,
        market: Market = Market.KOSPI
    ): List<ShortSelling> {
        DateUtils.validateDate(date)

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.SHORT_SELLING_ALL,
            "trdDd" to date,
            "mktId" to market.code
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { ShortSelling.fromJson(it) }
    }

    /**
     * 개별종목 공매도 거래 일별 추이
     *
     * pykrx: stock.get_shorting_volume_by_date()
     *
     * @param startDate 시작 날짜 (yyyyMMdd)
     * @param endDate 종료 날짜 (yyyyMMdd)
     * @param ticker 종목코드 (예: "005930")
     * @return 일별 공매도 거래 리스트
     */
    suspend fun getShortSellingByTicker(
        startDate: String,
        endDate: String,
        ticker: String
    ): List<ShortSellingHistory> {
        DateUtils.validateDateRange(startDate, endDate)

        val isinCode = getIsinCode(ticker, endDate)
            ?: return emptyList()

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.SHORT_SELLING_BY_TICKER,
            "strtDd" to startDate,
            "endDd" to endDate,
            "isuCd" to isinCode
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { ShortSellingHistory.fromJson(it) }
    }

    /**
     * 전종목 공매도 잔고 현황 (특정일)
     *
     * pykrx: stock.get_shorting_balance_by_ticker()
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param market 시장 구분 (기본: KOSPI)
     * @return 전종목 공매도 잔고 리스트
     */
    suspend fun getShortBalanceAll(
        date: String,
        market: Market = Market.KOSPI
    ): List<ShortBalance> {
        DateUtils.validateDate(date)

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.SHORT_BALANCE_ALL,
            "trdDd" to date,
            "mktId" to market.code
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { ShortBalance.fromJson(it) }
    }

    /**
     * 개별종목 공매도 잔고 일별 추이
     *
     * pykrx: stock.get_shorting_balance_by_date()
     *
     * @param startDate 시작 날짜 (yyyyMMdd)
     * @param endDate 종료 날짜 (yyyyMMdd)
     * @param ticker 종목코드 (예: "005930")
     * @return 일별 공매도 잔고 리스트
     */
    suspend fun getShortBalanceByTicker(
        startDate: String,
        endDate: String,
        ticker: String
    ): List<ShortBalanceHistory> {
        DateUtils.validateDateRange(startDate, endDate)

        val isinCode = getIsinCode(ticker, endDate)
            ?: return emptyList()

        val params = mapOf(
            "bld" to KrxEndpoints.Bld.SHORT_BALANCE_BY_TICKER,
            "strtDd" to startDate,
            "endDd" to endDate,
            "isuCd" to isinCode
        )

        val response = client.post(params)
        val jsonArray = KrxJsonParser.parseOutBlock(response)

        return jsonArray.mapNotNull { ShortBalanceHistory.fromJson(it) }
    }

    /**
     * 리소스 정리
     */
    fun close() {
        client.close()
    }
}

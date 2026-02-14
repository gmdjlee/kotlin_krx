# PLAN.md — pykrx Migration Implementation Plan

## Function 1: get_index_portfolio_deposit_file (지수 구성종목)

### pykrx Analysis
- **Signature**: `get_index_portfolio_deposit_file(ticker: str, date: str = None, alternative: bool = False) -> list`
- **BLD endpoint**: `dbms/MDC/STAT/standard/MDCSTAT00601`
- **Parameters**: `trdDd` (date), `indIdx2` (ticker[1:]), `indIdx` (ticker[0])
- **Response fields**: `ISU_SRT_CD`, `ISU_ABBRV`, `TDD_CLSPRC`, `FLUC_TP_CD`, `STR_CMP_PRC`, `FLUC_RT`, `MKTCAP`
- **Logic**: Returns list of constituent stock tickers; validates date >= "20140501"
- **Return**: List of stock tickers (strings)

### Kotlin Implementation Plan
- **Method**: `KrxIndex.getIndexPortfolio(date: String, ticker: String): List<IndexPortfolio>`
- **Data model**: `IndexPortfolio` (ticker, name, close, changeRate, marketCap)
- **Endpoint constant**: `KrxEndpoints.Bld.INDEX_PORTFOLIO = "dbms/MDC/STAT/standard/MDCSTAT00601"`
- **Changes**: Add to KrxIndex.kt, new IndexPortfolio.kt model, new BLD constant

## Function 2: get_index_ohlcv_by_ticker (전종목 지수 OHLCV - 특정일)

### pykrx Analysis
- **Signature**: `get_index_ohlcv_by_ticker(date: str, market: str = "KOSPI", alternative: bool = False)`
- **BLD endpoint**: `dbms/MDC/STAT/standard/MDCSTAT00101` (same as INDEX_LIST)
- **Parameters**: `trdDd` (date), `idxIndMidclssCd` (market code: 01=KRX, 02=KOSPI, 03=KOSDAQ, 04=Theme)
- **Response fields**: `IDX_NM`, `CLSPRC_IDX`, `FLUC_TP_CD`, `CMPPREVDD_IDX`, `FLUC_RT`, `OPNPRC_IDX`, `HGPRC_IDX`, `LWPRC_IDX`, `ACC_TRDVOL`, `ACC_TRDVAL`, `MKTCAP`
- **Logic**: Returns all index OHLCV data for a specific date; market param maps to code
- **Return**: DataFrame indexed by index name with OHLCV columns

### Kotlin Implementation Plan
- **Method**: `KrxIndex.getIndexOhlcv(date: String, market: IndexMarket): List<IndexOhlcvByTicker>`
- **Data model**: `IndexOhlcvByTicker` (name, open, high, low, close, volume, tradingValue, marketCap, changeType, change, changeRate)
- **Note**: Uses existing INDEX_LIST BLD but with OHLCV fields in response
- **Changes**: Add new method to KrxIndex.kt, new IndexOhlcvByTicker.kt model

## Function 3: get_index_ohlcv (router)

### pykrx Analysis
- **Signature**: `get_index_ohlcv(*args, **kwargs)` - dispatches based on arg pattern
- **Logic**: If 2 dates → calls `get_index_ohlcv_by_date()` (existing `getOhlcvByTicker`); if 1 date → calls `get_index_ohlcv_by_ticker()` (new Function 2)
- **Note**: This is a routing function. In Kotlin we'll have explicit named methods instead.

### Kotlin Implementation Plan
- **Already exists**: `getOhlcvByTicker(startDate, endDate, ticker)` covers the by-date case
- **New**: `getIndexOhlcv(date, market)` covers the by-ticker/single-date case (Function 2)
- **No router needed**: Kotlin uses explicit method names

## Function 4: get_nearest_business_day_in_a_week (최근 영업일)

### pykrx Analysis
- **Signature**: `get_nearest_business_day_in_a_week(date: str = None, prev: bool = True) -> str`
- **Logic**: Queries KRX for OHLCV data around the given date. Finds nearest trading day by checking up to 7 days before/after.
- **Implementation approach**: Uses OHLCV data from a known active stock to find valid trading dates
- **Return**: Date string (yyyyMMdd)

### Kotlin Implementation Plan
- **Method**: `DateUtils.getNearestBusinessDay(date: String, prev: Boolean = true): String`
- **Logic**: Use KrxIndex.getIndexList(date) or similar KRX query to find if date is a trading day. If not, iterate backwards (or forwards) up to 7 days.
- **Alternative approach**: Query OHLCV for a range around the date and find nearest non-empty date
- **Changes**: Add to DateUtils.kt (requires KrxClient dependency) or create BusinessDayUtils class

## Function 5: get_previous_business_days (이전 영업일 목록)

### pykrx Analysis
- **Signature**: `get_previous_business_days(**kwargs) -> list`
- **Params**: `year`+`month` OR `fromdate`+`todate`
- **Logic**: Gets OHLCV data for a known stock and extracts the trading dates
- **Return**: List of trading date strings

### Kotlin Implementation Plan
- **Methods**:
  - `DateUtils.getBusinessDays(startDate: String, endDate: String): List<String>` (range-based)
  - `DateUtils.getBusinessDaysByMonth(year: Int, month: Int): List<String>` (month-based)
- **Logic**: Query KRX OHLCV data (e.g., KOSPI index) for the date range and extract TRD_DD values
- **Changes**: Add to DateUtils or BusinessDayUtils, requires KrxClient

## Implementation Order
1. Add BLD constants (INDEX_PORTFOLIO, INDEX_OHLCV_ALL)
2. Create IndexPortfolio data model
3. Create IndexOhlcvByTicker data model
4. Add getIndexPortfolio() to KrxIndex
5. Add getIndexOhlcv() to KrxIndex
6. Add business day functions (getNearestBusinessDay, getBusinessDays)
7. Unit tests for all new functions
8. Build verification

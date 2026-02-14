# Migration Report: pykrx Functions to Kotlin

## Summary

Successfully migrated 5 missing pykrx functions to native Kotlin in the KrxKt project.

| # | pykrx Function | Kotlin Method | Status |
|---|---------------|---------------|--------|
| 1 | `get_index_portfolio_deposit_file` | `KrxIndex.getIndexPortfolio()` | Done |
| 2 | `get_index_ohlcv` (by ticker/date) | `KrxIndex.getIndexOhlcv()` | Done |
| 3 | `get_index_ohlcv_by_ticker` | `KrxIndex.getIndexOhlcv()` | Done |
| 4 | `get_nearest_business_day_in_a_week` | `KrxIndex.getNearestBusinessDay()` | Done |
| 5 | `get_previous_business_days` | `KrxIndex.getBusinessDays()` / `getBusinessDaysByMonth()` | Done |

## Files Changed

### New Files
| File | Description |
|------|-------------|
| `src/main/kotlin/com/krxkt/model/IndexPortfolio.kt` | Index portfolio constituent data model |
| `src/main/kotlin/com/krxkt/model/IndexOhlcvByTicker.kt` | All-index OHLCV data model (single date) |
| `src/test/kotlin/com/krxkt/KrxIndexTest.kt` | Unit tests for all new KrxIndex methods |
| `src/test/kotlin/com/krxkt/model/IndexPortfolioTest.kt` | IndexPortfolio model tests |
| `src/test/kotlin/com/krxkt/model/IndexOhlcvByTickerTest.kt` | IndexOhlcvByTicker model tests |

### Modified Files
| File | Changes |
|------|---------|
| `src/main/kotlin/com/krxkt/KrxIndex.kt` | Added 6 new methods: getIndexOhlcv, getIndexPortfolio, getIndexPortfolioTickers, getNearestBusinessDay, getBusinessDays, getBusinessDaysByMonth |
| `src/main/kotlin/com/krxkt/api/KrxEndpoints.kt` | Added INDEX_PORTFOLIO BLD constant |
| `src/main/kotlin/com/krxkt/model/IndexInfo.kt` | Added `krxCode` property to IndexMarket enum |
| `src/test/kotlin/com/krxkt/model/IndexOhlcvTest.kt` | Fixed pre-existing test bug (CLPR_IDX → CLSPRC_IDX) |
| `CLAUDE.md` | Updated API mapping table, BLD endpoints, phase status |

## API Endpoint Details

### Function 1: getIndexPortfolio
- **BLD**: `dbms/MDC/STAT/standard/MDCSTAT00601`
- **Params**: `trdDd`, `indIdx` (market type), `indIdx2` (index code)
- **Response**: `ISU_SRT_CD`, `ISU_ABBRV`, `TDD_CLSPRC`, `FLUC_TP_CD`, `STR_CMP_PRC`, `FLUC_RT`, `MKTCAP`
- **Convenience method**: `getIndexPortfolioTickers()` returns just the ticker strings

### Function 2-3: getIndexOhlcv
- **BLD**: `dbms/MDC/STAT/standard/MDCSTAT00101`
- **Params**: `trdDd`, `idxIndMidclssCd` (market code: 02=KOSPI, 03=KOSDAQ, 04=Theme)
- **Response**: `IDX_NM`, `OPNPRC_IDX`, `HGPRC_IDX`, `LWPRC_IDX`, `CLSPRC_IDX`, `ACC_TRDVOL`, `ACC_TRDVAL`, `MKTCAP`, `FLUC_TP_CD`, `CMPPREVDD_IDX`, `FLUC_RT`
- **Note**: pykrx's `get_index_ohlcv` is a router that dispatches to by-date or by-ticker. In Kotlin we use explicit method names.

### Function 4: getNearestBusinessDay
- **Strategy**: Queries KRX index OHLCV for consecutive dates (max 7 days) to find nearest trading day
- **Params**: `date` (yyyyMMdd), `prev` (boolean, default true)
- **Returns**: Nearest business day date string

### Function 5: getBusinessDays / getBusinessDaysByMonth
- **Strategy**: Queries KOSPI index OHLCV for date range and extracts TRD_DD values
- **getBusinessDays**: Range-based (startDate, endDate)
- **getBusinessDaysByMonth**: Month-based (year, month) - auto-calculates date range

## Test Coverage

| Test File | Tests | Status |
|-----------|-------|--------|
| KrxIndexTest.kt | 14 tests | All pass |
| IndexPortfolioTest.kt | 5 tests | All pass |
| IndexOhlcvByTickerTest.kt | 6 tests | All pass |
| **Total new tests** | **25** | **All pass** |

## Bug Fixes

- Fixed pre-existing test failure in `IndexOhlcvTest.kt`: test data used wrong field name `CLPR_IDX` instead of `CLSPRC_IDX`

## Build Verification

```
./gradlew build → BUILD SUCCESSFUL
./gradlew test → All tests pass (0 failures)
```

## Architecture Notes

- All new methods follow existing patterns: `suspend fun`, `KrxClient.post()`, `KrxJsonParser.parseOutBlock()`, `fromJson()` companion
- Business day functions are on `KrxIndex` (not `DateUtils`) because they require KRX API access
- `IndexMarket` enum extended with `krxCode` property for the MDCSTAT00101 endpoint's `idxIndMidclssCd` parameter
- No new dependencies added

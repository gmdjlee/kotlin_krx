# PROGRESS.md — Missing Functions Migration
## Status: LOOP_COMPLETE

## Completed
- [x] M-001: Read pykrx source for 5 target functions
- [x] M-002: Document each function's signature, logic, API endpoints → PLAN.md
- [x] M-003: Architect-Reviewer approved PLAN.md
- [x] M-004: Implement get_index_portfolio_deposit_file (IndexPortfolio model + KrxIndex.getIndexPortfolio)
- [x] M-005: Implement get_index_ohlcv + get_index_ohlcv_by_ticker (IndexOhlcvByTicker model + KrxIndex.getIndexOhlcv)
- [x] M-006: Implement get_nearest_business_day_in_a_week + get_previous_business_days (KrxIndex.getNearestBusinessDay, getBusinessDays, getBusinessDaysByMonth)
- [x] M-007: Integrated into existing KrxIndex class (no Repository/UseCase/Hilt needed - this is a library, not an Android app)
- [x] M-008: Unit tests for all 5 functions (25 new tests, all passing)
- [x] M-009: Build: ./gradlew build passes
- [x] M-010: Updated CLAUDE.md + generated MIGRATION_REPORT.md

## Current
(none - all tasks complete)

## Findings

### pykrx Source Analysis
- get_index_portfolio_deposit_file: BLD=MDCSTAT00601, returns constituent stock tickers
- get_index_ohlcv_by_ticker: BLD=MDCSTAT00101, returns all-index OHLCV for single date
- get_index_ohlcv: Router function dispatching to by-date or by-ticker variants
- get_nearest_business_day_in_a_week: Queries OHLCV to find trading days within 7-day window
- get_previous_business_days: Extracts trading dates from OHLCV data for date range or month

### Implementation Decisions
- Business day functions placed on KrxIndex (requires KRX API access)
- IndexMarket enum extended with krxCode for MDCSTAT00101 parameter
- Pre-existing IndexOhlcvTest bug fixed (CLPR_IDX → CLSPRC_IDX)

### Build Status
- ./gradlew build: SUCCESSFUL
- ./gradlew test: All tests pass (0 failures)
- New files: 5 (2 models, 3 test files)
- Modified files: 5

---

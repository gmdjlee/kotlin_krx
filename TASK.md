# TASK.md — Missing pykrx Functions Migration

## Phase 1: Analysis (iterations 1-2)
- [x] **M-001** Read pykrx source for 5 target functions: get_index_portfolio_deposit_file, get_index_ohlcv, get_index_ohlcv_by_ticker, get_nearest_business_day_in_a_week, get_previous_business_days
- [x] **M-002** Document each function's signature, logic, API endpoints, return types → write PLAN.md
- [x] **M-003** Architect-Reviewer approves PLAN.md before implementation

## Phase 2: Implementation (iterations 3-7)
- [x] **M-004** Implement get_index_portfolio_deposit_file in kotlin_krx module
- [x] **M-005** Implement get_index_ohlcv + get_index_ohlcv_by_ticker (related, batch together)
- [x] **M-006** Implement get_nearest_business_day_in_a_week + get_previous_business_days (date utils, batch)
- [x] **M-007** Integrate into existing Repository/UseCase layers, update Hilt DI

## Phase 3: Verification (iterations 8-10)
- [x] **M-008** Unit tests for all 5 functions (input/output parity with pykrx)
- [x] **M-009** Build: ./gradlew assembleDebug passes
- [x] **M-010** Update CLAUDE.md + generate MIGRATION_REPORT.md

## Target Functions
| # | pykrx function | module | notes |
|---|---------------|--------|-------|
| 1 | get_index_portfolio_deposit_file | index | 지수 구성종목 |
| 2 | get_index_ohlcv | index | 지수 OHLCV |
| 3 | get_index_ohlcv_by_ticker | index | 종목별 지수 OHLCV |
| 4 | get_nearest_business_day_in_a_week | util | 최근 영업일 |
| 5 | get_previous_business_days | util | 이전 영업일 목록 |

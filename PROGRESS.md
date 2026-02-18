# PROGRESS.md — KrxKt Project

## Status: ALL PHASES COMPLETE

## Completed

- Phase 1~5: pykrx 핵심 기능 Kotlin 이식 완료
- Stock: OHLCV, MarketCap, Fundamental, TickerList, Investor Trading, Short Selling
- ETF: Price, OHLCV, TickerList, Portfolio
- Index: OHLCV, IndexList, Portfolio, Business Days
- Bugfix: MARKET_CAP BLD 수정 (MDCSTAT01602 → MDCSTAT01501)

## Current

(none)

## Findings

- MDCSTAT01602는 전종목등락률 엔드포인트 (시가총액 아님)
- pykrx의 get_market_cap은 MDCSTAT01501(전종목시세)에서 MKTCAP/LIST_SHRS 추출
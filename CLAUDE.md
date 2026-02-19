# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**KrxKt** - pykrx(Python KRX 데이터 라이브러리)를 Native Kotlin으로 전환하는 프로젝트.
Android 앱에서 Python 런타임 없이 한국거래소(KRX) 시장 데이터에 직접 접근.

**Goals:**
- Zero Python dependencies
- Full API compatibility with pykrx core functions
- Coroutine-based async operations
- Minimal APK size impact (< 1MB)
- Production-ready error handling with retry logic

## Documentation

| 문서 | 내용 |
|------|------|
| `KrxKt_Implementation_Specification.md` | Claude Code 설정 가이드 (Skills, Agents, Hooks) |
| `docs/krx-api-reference.md` | KRX API 엔드포인트 상세 |

## Build Commands

```bash
# Windows (CMD/PowerShell)
gradlew.bat build            # Full build
gradlew.bat test             # Unit tests

# Unix/macOS/Git Bash
./gradlew build              # Full build
./gradlew test               # Unit tests
```

| Command | Description |
|---------|-------------|
| `build` | Full build |
| `test` | Unit tests (MockWebServer) |
| `runIntegrationTest` | Integration test (한국 네트워크 필요) |
| `runIntegrationTest -PmainClass=<class>` | 특정 테스트 클래스 실행 |

## Tech Stack

| Library | Version | Purpose |
|---------|---------|---------|
| OkHttp | 4.12.0 | HTTP client |
| Gson | 2.10.1 | JSON parsing |
| Coroutines | 1.7.3 | Async operations |
| kotlinx-datetime | 0.5.0 | Date handling |

## Package Structure

```
com.krxkt/
├── api/                          # HTTP clients
│   ├── KrxClient.kt              # OkHttp-based HTTP client
│   ├── KrxEndpoints.kt           # Endpoint constants (bld values)
│   └── NaverClient.kt            # Naver Finance client (optional)
├── model/                        # Data classes
│   ├── MarketOhlcv.kt
│   ├── MarketCap.kt
│   ├── StockFundamental.kt
│   ├── EtfPrice.kt
│   ├── InvestorTrading.kt
│   ├── DerivativeIndex.kt
│   └── OptionVolume.kt
├── parser/                       # Response parsers
│   └── KrxJsonParser.kt          # JSON parsing (comma removal)
├── cache/                        # Caching layer
│   └── TickerCache.kt            # In-memory ticker cache
├── KrxStock.kt                   # Main public API
├── KrxEtf.kt                     # ETF API
└── KrxIndex.kt                   # Index API
```

## Implementation Phases

| Phase | Status | Scope |
|-------|--------|-------|
| Phase 1 | ✅ Done | Core Infrastructure (KrxClient, KrxJsonParser, KrxEndpoints) |
| Phase 2 | ✅ Done | Stock Data APIs (getMarketOhlcv, getMarketCap, getMarketFundamental) |
| Phase 3 | ✅ Done | ETF & Index APIs (KrxEtf, KrxIndex) |
| Phase 4 | ✅ Done | Advanced Features (Investor Trading, Short Selling) |
| Phase 5 | ✅ Done | Index Extensions & Business Days (Portfolio, All-Index OHLCV, Business Days) |
| Phase 6 | ✅ Done | Derivative APIs (VKOSPI, Bond Index, Option Volume) |

---

## Coding Standards

### Kotlin 패턴
- 모든 API 호출 함수는 `suspend fun`으로 작성
- Data class에 nullable 타입 적절히 사용 (KRX API가 빈 값 반환 가능)
- 숫자 파싱 시 쉼표 제거 처리 필수: `"82,200"` → `82200L`

### KRX API 패턴
- Base URL: `https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd`
- Method: POST (application/x-www-form-urlencoded)
- Referer 헤더 필수 (outerLoader — 모든 엔드포인트에서 동작)
- 응답 구조: JSON → `OutBlock_1`, `block1`, 또는 `output` 배열에 데이터 행 포함
- 날짜 형식: `yyyyMMdd` (예: `20210122`)
- CookieJar로 세션 쿠키 자동 관리

### 테스트 기준
- 기준 날짜: `20210122` (과거 안정 데이터)
- 기준 종목: `005930` (삼성전자), `069500` (KODEX 200)
- 공휴일 테스트: `20210101` (빈 응답 확인)

---

## Skills & Agents 활용 지침

### Skills (자동 로드)

| Skill | 용도 | 트리거 |
|-------|------|--------|
| `krx-data` | KRX JSON 파싱, OutBlock_1 처리, 쉼표 숫자 변환 | KRX 데이터 파싱 작업 시 |
| `kotlin-codegen` | suspend 함수, OkHttp 클라이언트, data class 패턴 | 새 Kotlin 파일 생성 시 |

### Agents (자동 위임)

| Agent | 용도 | 도구 | 트리거 |
|-------|------|------|--------|
| `krx-api-researcher` | pykrx 소스 분석, KRX 엔드포인트 조사 | Read, Grep, WebFetch | 새 API 엔드포인트 조사 시 |
| `kotlin-implementer` | Kotlin 코드 구현 | Read, Write, Edit, Bash | 모듈 구현/리팩토링 시 |
| `code-reviewer` | 코드 품질, 에러 핸들링 검토 | Read, Grep, Glob | 구현 완료 후 리뷰 시 |

### Custom Commands

| Command | 용도 |
|---------|------|
| `/test-krx` | KRX API 테스트 스위트 실행 |
| `/implement-endpoint {name}` | 엔드포인트 구현 워크플로우 (조사→구현→테스트→리뷰) |
| `/cross-validate` | KIS API로 데이터 크로스 검증 |

---

## KRX API Quick Reference

### Market IDs

| Market | mktId |
|--------|-------|
| KOSPI | `STK` |
| KOSDAQ | `KSQ` |
| KONEX | `KNX` |
| ALL | `ALL` |

### Key Endpoints (bld values)

| Function | BLD Value |
|----------|-----------|
| All Stocks OHLCV | `dbms/MDC/STAT/standard/MDCSTAT01501` |
| Stock History | `dbms/MDC/STAT/standard/MDCSTAT01701` |
| Market Cap | `dbms/MDC/STAT/standard/MDCSTAT01501` |
| Fundamentals | `dbms/MDC/STAT/standard/MDCSTAT03501` |
| Ticker List | `dbms/MDC/STAT/standard/MDCSTAT01901` |
| ETF Price | `dbms/MDC/STAT/standard/MDCSTAT04301` |
| ETF History | `dbms/MDC/STAT/standard/MDCSTAT04501` |
| ETF Ticker List | `dbms/MDC/STAT/standard/MDCSTAT04601` |
| ETF Portfolio | `dbms/MDC/STAT/standard/MDCSTAT05001` |
| Index OHLCV | `dbms/MDC/STAT/standard/MDCSTAT00301` |
| Index List / All-Index OHLCV | `dbms/MDC/STAT/standard/MDCSTAT00101` |
| Index Portfolio | `dbms/MDC/STAT/standard/MDCSTAT00601` |
| Investor Trading (Market) | `dbms/MDC/STAT/standard/MDCSTAT02203` |
| Investor Trading (Ticker) | `dbms/MDC/STAT/standard/MDCSTAT02303` |
| Derivative Index (VKOSPI, Bond) | `dbms/MDC/STAT/standard/MDCSTAT01201` |
| Option Trading Volume | `dbms/MDC/STAT/standard/MDCSTAT13102` |
| Short Selling (All) | `dbms/MDC/STAT/srt/MDCSTAT30101` |
| Short Selling (Ticker) | `dbms/MDC/STAT/srt/MDCSTAT30102` |
| Short Balance (All) | `dbms/MDC/STAT/srt/MDCSTAT30501` |
| Short Balance (Ticker) | `dbms/MDC/STAT/srt/MDCSTAT30502` |

### pykrx → Kotlin API Mapping

| pykrx Function | Kotlin Function |
|----------------|-----------------|
| `stock.get_market_ohlcv("20210122")` | `krxStock.getMarketOhlcv(date)` |
| `stock.get_market_ohlcv("20210101", "20210131", "005930")` | `krxStock.getOhlcvByTicker(start, end, ticker)` |
| `stock.get_market_cap("20210122")` | `krxStock.getMarketCap(date)` |
| `stock.get_market_fundamental("20210122")` | `krxStock.getMarketFundamental(date)` |
| `stock.get_market_ticker_list("20210122")` | `krxStock.getTickerList(date)` |
| `etf.get_etf_ohlcv_by_ticker("20210122")` | `krxEtf.getEtfPrice(date)` |
| `etf.get_etf_ohlcv_by_date("20210101", "20210131", "069500")` | `krxEtf.getOhlcvByTicker(start, end, ticker)` |
| `etf.get_etf_ticker_list("20210122")` | `krxEtf.getEtfTickerList(date)` |
| `etf.get_etf_portfolio_deposit_file("20210122", "069500")` | `krxEtf.getPortfolio(date, ticker)` |
| `index.get_index_ohlcv("20210101", "20210131", "1001")` | `krxIndex.getOhlcv(start, end, ticker)` |
| (N/A) | `krxIndex.getKospi(start, end)` |
| (N/A) | `krxIndex.getKosdaq(start, end)` |
| (N/A) | `krxIndex.getKospi200(start, end)` |
| `stock.get_market_trading_value_by_date(...)` | `krxStock.getMarketTradingByInvestor(...)` |
| `stock.get_market_trading_value_by_date(..., ticker)` | `krxStock.getTradingByInvestor(...)` |
| `stock.get_shorting_volume_by_ticker(...)` | `krxStock.getShortSellingByTicker(...)` |
| `stock.get_shorting_balance_by_ticker(...)` | `krxStock.getShortBalanceByTicker(...)` |
| `index.get_index_portfolio_deposit_file(ticker, date)` | `krxIndex.getIndexPortfolio(date, ticker)` |
| `index.get_index_ohlcv_by_ticker(date, market)` | `krxIndex.getIndexOhlcv(date, market)` |
| `get_nearest_business_day_in_a_week(date, prev)` | `krxIndex.getNearestBusinessDay(date, prev)` |
| `get_previous_business_days(fromdate, todate)` | `krxIndex.getBusinessDays(start, end)` |
| `get_previous_business_days(year, month)` | `krxIndex.getBusinessDaysByMonth(year, month)` |
| feargreed.py `KRXFetcher.get_index()` (VKOSPI) | `krxIndex.getVkospi(start, end)` |
| feargreed.py `KRXFetcher.get_index()` (5yr bond) | `krxIndex.getBond5y(start, end)` |
| feargreed.py `KRXFetcher.get_index()` (10yr bond) | `krxIndex.getBond10y(start, end)` |
| feargreed.py `KRXFetcher.get_option()` (Call) | `krxIndex.getCallOptionVolume(start, end)` |
| feargreed.py `KRXFetcher.get_option()` (Put) | `krxIndex.getPutOptionVolume(start, end)` |

---

## Error Handling

| Error Type | Strategy |
|------------|----------|
| Network Errors | Exponential backoff (3 attempts: 1s/2s/4s) |
| Parse Errors | Return empty list with logged warning |
| Invalid Date | Throw IllegalArgumentException |
| Empty Response | Return empty list (market may be closed) |
| LOGOUT Response | KRX 세션 만료 - 한국 네트워크/VPN 필요 |

## Network Requirements

⚠️ **outerLoader Referer를 사용하면 대부분의 엔드포인트가 네트워크 제한 없이 동작합니다.**

- outerLoader Referer: 모든 엔드포인트에서 동작 (MDCSTAT01501, MDCSTAT01201, MDCSTAT13102 등)
- mdiLoader Referer: 한국 IP 필요 (사용하지 않음)
- 단위 테스트(MockWebServer)는 네트워크 제한 없음

```bash
# 단위 테스트 (네트워크 불필요)
gradlew.bat test                    # Windows
./gradlew test                      # Unix/macOS

# 통합 테스트 (한국 네트워크 필요)
gradlew.bat runIntegrationTest -PmainClass=com.krxkt.integration.EtfPortfolioTestKt   # Windows
./gradlew runIntegrationTest -PmainClass=com.krxkt.integration.EtfPortfolioTestKt     # Unix/macOS
```

## External Resources

- **pykrx 소스**: https://github.com/sharebook-kr/pykrx
- **KRX Data System**: http://data.krx.co.kr
- **KIS API MCP**: 데이터 크로스 검증용

## Related Project

Reference `../mini_stock/` for Android integration patterns (Hilt DI, Room caching, MVVM architecture).

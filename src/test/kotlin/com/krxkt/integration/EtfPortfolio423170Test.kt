package com.krxkt.integration

import com.krxkt.KrxEtf
import com.krxkt.api.KrxClient

/**
 * ETF 423170 구성종목 조회 통합 테스트
 *
 * 실행:
 *   gradlew.bat runIntegrationTest -PmainClass=com.krxkt.integration.EtfPortfolio423170TestKt
 */
fun main() {
    val krxId = System.getProperty("krxId") ?: error("krxId required")
    val krxPw = System.getProperty("krxPw") ?: error("krxPw required")

    val client = KrxClient()

    println("=== KRX 로그인 ===")
    val success = client.login(krxId, krxPw)
    if (!success) {
        println("로그인 실패")
        client.close()
        return
    }
    println("로그인 성공")

    val etf = KrxEtf(client)
    val date = "20260304"
    val ticker = "423170"

    println("\n=== ETF $ticker 구성종목 ($date) ===")
    val portfolio = kotlinx.coroutines.runBlocking {
        etf.getPortfolio(date, ticker)
    }

    if (portfolio.isEmpty()) {
        println("구성종목 데이터 없음")
    } else {
        println("총 ${portfolio.size}개 구성종목\n")
        println("%-8s %-20s %12s %10s".format("종목코드", "종목명", "평가금액", "비중(%)"))
        println("-".repeat(55))
        portfolio.forEach { item ->
            println("%-8s %-20s %,12d %10s".format(
                item.ticker,
                item.name,
                item.valuationAmount,
                item.weight?.let { "%.2f".format(it) } ?: "-"
            ))
        }
    }

    client.close()
}

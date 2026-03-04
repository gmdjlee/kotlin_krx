package com.krxkt.integration

import com.krxkt.KrxStock
import com.krxkt.api.KrxClient

/**
 * 삼성전자(005930) 2026-03-04 종가 조회 통합 테스트
 *
 * 실행:
 *   gradlew.bat runIntegrationTest -PmainClass=com.krxkt.integration.SamsungCloseTestKt -PkrxId=YOUR_ID -PkrxPw=YOUR_PW
 */
fun main() {
    val krxId = System.getProperty("krxId") ?: error("시스템 프로퍼티 'krxId'가 필요합니다. -DkrxId=YOUR_ID")
    val krxPw = System.getProperty("krxPw") ?: error("시스템 프로퍼티 'krxPw'가 필요합니다. -DkrxPw=YOUR_PW")

    val client = KrxClient()

    println("=== KRX 로그인 시도 ===")
    val loginSuccess = client.login(krxId, krxPw)
    if (!loginSuccess) {
        println("❌ 로그인 실패. ID/PW를 확인하세요.")
        client.close()
        return
    }
    println("✅ 로그인 성공")

    val stock = KrxStock(client)

    println("\n=== 삼성전자(005930) 2026-03-04 종가 조회 ===")
    val ohlcvList = kotlinx.coroutines.runBlocking {
        stock.getMarketOhlcv("20260304")
    }

    val samsung = ohlcvList.find { it.ticker == "005930" }
    if (samsung != null) {
        println("종목: ${samsung.name} (${samsung.ticker})")
        println("종가: ${samsung.close}")
        println("시가: ${samsung.open}")
        println("고가: ${samsung.high}")
        println("저가: ${samsung.low}")
        println("거래량: ${samsung.volume}")
    } else {
        println("❌ 삼성전자(005930) 데이터를 찾을 수 없습니다.")
        if (ohlcvList.isEmpty()) {
            println("   → 응답이 비어 있습니다. 휴장일이거나 로그인 세션 문제일 수 있습니다.")
        } else {
            println("   → 총 ${ohlcvList.size}개 종목 수신됨. 첫 3개:")
            ohlcvList.take(3).forEach { println("     ${it.ticker} ${it.name}") }
        }
    }

    client.close()
}

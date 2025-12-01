package com.loopers.infrastructure.payment

import com.loopers.application.payment.TransactionInfo
import com.loopers.domain.payment.PaymentRelay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PaymentCoreRelay : PaymentRelay {
    companion object {
        private val logger = LoggerFactory.getLogger(PaymentCoreRelay::class.java)
        private val restTemplate = RestTemplate()
    }

    override fun notify(callbackUrl: String, transactionInfo: TransactionInfo) {
        runCatching {
            restTemplate.postForEntity(callbackUrl, transactionInfo, Any::class.java)
        }.onFailure { e -> logger.error("콜백 호출을 실패했습니다. {}", e.message, e) }
    }
}

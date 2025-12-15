package com.loopers.domain.payment

import com.loopers.application.payment.TransactionInfo

interface PaymentRelay {
    fun notify(callbackUrl: String, transactionInfo: TransactionInfo)
}

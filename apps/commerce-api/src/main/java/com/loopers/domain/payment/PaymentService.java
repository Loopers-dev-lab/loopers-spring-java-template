package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentClient paymentClient;

    public void createPayment(Integer amount, String orderKey) {
        Payment payment = Payment.createPayment(amount, orderKey);
        paymentRepository.savePayment(payment);
    }

    public Payment getPaymentByOrderKey(String orderKey) {
        return paymentRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    public void validatePaymentStatusPending(Payment payment) {
        if (!payment.isPending()) {
            throw new CoreException(ErrorType.CONFLICT, "결제 대기 상태가 아닙니다.");
        }
    }

    public Card createCard(String cardType, String cardNumber) {
        return Card.createCard(cardType, cardNumber);
    }

    public void applyCardInfo(Payment savedPayment, Card card) {
        savedPayment.updateCard(card);
    }

    public void requestPaymentToPg(Payment payment, String loginId) {
        PaymentClient.PaymentRequest request = new PaymentClient.PaymentRequest(
                payment.getOrderKey(),
                payment.getCard().getCardType(),
                payment.getCard().getCardNumber(),
                payment.getAmount(),
                loginId
        );

        paymentClient.requestPayment(request);
    }

    public Payment getPendingPaymentByOrderKey(String orderKey) {
        Payment payment = paymentRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (payment.isCompleted()) {
            throw new CoreException(ErrorType.CONFLICT, "이미 성공한 결제입니다.");
        }

        return payment;
    }

    public List<Payment> getPendingPaymentsCreatedBefore(ZonedDateTime before) {
        return paymentRepository.getPendingPaymentsCreatedBefore(before);
    }

    public Payment checkPaymentStatusFromPg(Payment payment, String loginId) {
        PaymentClient.PaymentResponse paymentResponse;

        if (payment.hasTransactionKey()) {
            PaymentClient.PaymentStatusRequest request = new PaymentClient.PaymentStatusRequest(
                    payment.getTransactionKey(),
                    loginId
            );
            paymentResponse = paymentClient.getPaymentStatusByTransactionKey(request);
        } else {
            PaymentClient.PaymentStatusByOrderKeyRequest request = new PaymentClient.PaymentStatusByOrderKeyRequest(
                    payment.getOrderKey(),
                    loginId
            );
            paymentResponse = paymentClient.getPaymentStatusByOrderKey(request);

        }

        if (paymentResponse.isSuccess()) {
            payment.updateStatus(PaymentStatus.COMPLETED);
            payment.updateTransactionKey(paymentResponse.transactionKey());
        } else {
            payment.updateStatus(PaymentStatus.FAILED);
        }

        return payment;
    }
}


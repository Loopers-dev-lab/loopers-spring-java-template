package com.loopers.interfaces.api.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentCallbackRequest {

    @NotBlank(message = "transactionKey는 필수입니다")
    private String transactionKey;

    @NotBlank(message = "status는 필수입니다")
    private String status;

    private String reason;
}

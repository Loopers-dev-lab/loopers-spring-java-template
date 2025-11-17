package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverPhoneNumber;

    @Column(nullable = false)
    private String baseAddress;

    @Column(nullable = false)
    private String detailAddress;

    @Builder
    private Delivery(String receiverName, String receiverPhoneNumber, String baseAddress, String detailAddress) {
        validate(receiverName, receiverPhoneNumber, baseAddress, detailAddress);
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }

    public static Delivery createDelivery(String receiverName, String receiverPhoneNumber, String baseAddress,
                                          String detailAddress) {
        return Delivery.builder()
                .receiverName(receiverName)
                .receiverPhoneNumber(receiverPhoneNumber)
                .baseAddress(baseAddress)
                .detailAddress(detailAddress)
                .build();
    }

    private void validate(String receiverName, String receiverPhoneNumber, String baseAddress, String detailAddress) {
        validateReceiverName(receiverName);
        validateReceiverPhoneNumber(receiverPhoneNumber);
        validateBaseAddress(baseAddress);
        validateDetailAddress(detailAddress);
    }

    private void validateReceiverName(String receiverName) {
        if (receiverName == null || receiverName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수령인 이름은 필수입니다.");
        }
    }

    private void validateReceiverPhoneNumber(String receiverPhoneNumber) {
        if (receiverPhoneNumber == null || receiverPhoneNumber.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수령인 핸드폰번호는 필수입니다.");
        }

        if (!receiverPhoneNumber.matches("^\\d{3}-\\d{4}-\\d{4}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "핸드폰 번호는 xxx-xxxx-xxxx 형식이어야 합니다.");
        }
    }

    private void validateBaseAddress(String baseAddress) {
        if (baseAddress == null || baseAddress.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주소는 필수입니다.");
        }
    }

    private void validateDetailAddress(String detailAddress) {
        if (detailAddress == null || detailAddress.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상세 주소는 필수입니다.");
        }
    }
}

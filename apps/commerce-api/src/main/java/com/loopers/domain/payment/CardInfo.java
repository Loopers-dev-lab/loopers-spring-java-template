package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CardInfo {

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @Column(name = "card_no", nullable = false, length = 20)
    private String cardNo;

    private CardInfo(String cardType, String cardNo) {
        validateCardInfo(cardType, cardNo);
        this.cardType = cardType;
        this.cardNo = cardNo;
    }

    public static CardInfo of(String cardType, String cardNo) {
        return new CardInfo(cardType, cardNo);
    }

    private void validateCardInfo(String cardType, String cardNo) {
        if (cardType == null || cardType.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 타입은 필수입니다.");
        }
        if (cardNo == null || cardNo.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 필수입니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardInfo cardInfo = (CardInfo) o;
        return cardType.equals(cardInfo.cardType) && cardNo.equals(cardInfo.cardNo);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(cardType, cardNo);
    }
}

package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "card_number")
    private String cardNumber;

    public Card(String cardType, String cardNumber) {
        validate(cardType, cardNumber);
        this.cardType = cardType;
        this.cardNumber = cardNumber;
    }

    public static Card createCard(String cardType, String cardNumber) {
        return new Card(cardType, cardNumber);
    }

    private void validate(String cardType, String cardNumber) {
        if (cardType == null || cardType.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 타입은 필수입니다.");
        }
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 필수입니다.");
        }
        if (!cardNumber.matches("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다.");
        }
    }
}




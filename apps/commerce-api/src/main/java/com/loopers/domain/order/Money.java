package com.loopers.domain.order;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private BigDecimal amount;
  @Enumerated(EnumType.STRING)
  private CurrencyCode currency;

  public Money(BigDecimal amount, CurrencyCode currency) {
    // 💡 음수 검증
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("금액은 음수가 될 수 없습니다.");
    }

    this.amount = amount;
    this.currency = currency;
  }

  public Money add(Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException("통화가 다릅니다.");
    }
    return new Money(this.amount.add(other.amount), this.currency);
  }

  public Money subtract(Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException("통화가 다릅니다.");
    }
    return new Money(this.amount.subtract(other.amount), this.currency);
  }

  public Money multiply(long multiplier) {
    BigDecimal multiplierDecimal = BigDecimal.valueOf(multiplier);
    BigDecimal newAmount = this.amount.multiply(multiplierDecimal);
    return new Money(newAmount, this.currency);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return Objects.equals(currency, money.currency) &&
        amount.compareTo(money.amount) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount.stripTrailingZeros(), currency);
  }

  public boolean isZero() {
    return this.amount.compareTo(ZERO) == 0;
  }

  public static Money wons(long amount) {
    BigDecimal amountDecimal = BigDecimal.valueOf(amount);
    return new Money(amountDecimal, CurrencyCode.KRW);
  }

  public static Money of(BigDecimal amount, CurrencyCode currencyCode) {
    return new Money(amount, currencyCode);
  }
}

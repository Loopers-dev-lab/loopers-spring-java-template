package com.loopers.domain.payment;


public enum CardType {
  SAMSUNG("삼성카드"),
  KB("국민카드"),
  HYUNDAI("현대카드");

  private final String description;

  CardType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}

package com.loopers.domain.outbox;

public enum OutboxStatus {
  NEW("NEW", "전송 대기"),
  SENDING("SENDING", "전송 중"),
  SENT("SENT", "전송 완료"),
  DEAD("DEAD", "전송 실패");

  private final String code;
  private final String description;

  OutboxStatus(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }
}

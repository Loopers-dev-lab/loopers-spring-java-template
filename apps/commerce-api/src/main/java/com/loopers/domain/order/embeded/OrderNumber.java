package com.loopers.domain.order.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Embeddable
@Getter
public class OrderNumber {

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;
    
    protected OrderNumber() {}
    
    private OrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public static OrderNumber generate(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return new OrderNumber("ORD-" + timestamp + "-" + uuid);
    }
    
    public static OrderNumber of(String orderNumber) {
        validateOrderNumber(orderNumber);
        return new OrderNumber(orderNumber);
    }
    
    public String getValue() {
        return orderNumber;
    }
    
    private static void validateOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문번호는 필수입니다.");
        }
        if (!orderNumber.matches("^ORD-\\d{14}-[A-Z0-9]{8}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "올바르지 않은 주문번호 형식입니다.");
        }
    }
}

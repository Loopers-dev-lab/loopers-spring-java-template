package com.loopers.domain.order;

import com.loopers.domain.order.embeded.OrderNumber;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderNumber 테스트")
class OrderNumberTest {

    @Test
    @DisplayName("주문번호가 올바른 형식으로 생성된다")
    void generate_ShouldCreateValidFormat() {
        Long userId = 12345L;

        OrderNumber orderNumber = OrderNumber.generate(userId);

        String value = orderNumber.getValue();
        
        assertThat(value).matches("^ORD-\\d{17}-[A-F0-9]{8}$");
        assertThat(value).startsWith("ORD-");
        assertThat(value).hasSize(30); // ORD(3) + -(1) + timestamp(17) + -(1) + hash(8) = 30
    }

    @Test
    @DisplayName("동일한 사용자ID로 생성해도 주문번호가 다르다")
    void generate_SameUserId_ShouldCreateDifferentNumbers() {
        Long userId = 12345L;

        OrderNumber first = OrderNumber.generate(userId);
        OrderNumber second = OrderNumber.generate(userId);

        assertThat(first.getValue()).isNotEqualTo(second.getValue());
    }

    @Test
    @DisplayName("다른 사용자ID로 생성하면 주문번호가 다르다")
    void generate_DifferentUserIds_ShouldCreateDifferentNumbers() {
        Long userId1 = 12345L;
        Long userId2 = 67890L;

        OrderNumber first = OrderNumber.generate(userId1);
        OrderNumber second = OrderNumber.generate(userId2);

        assertThat(first.getValue()).isNotEqualTo(second.getValue());
    }

    @Test
    @DisplayName("동시에 여러 주문번호를 생성해도 중복되지 않는다")
    void generate_Concurrent_ShouldNotCreateDuplicates() throws InterruptedException {
        int threadCount = 100;
        int orderPerThread = 10;
        Long userId = 12345L;
        
        Set<String> orderNumbers = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < orderPerThread; j++) {
                    OrderNumber orderNumber = OrderNumber.generate(userId);
                    orderNumbers.add(orderNumber.getValue());
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(orderNumbers).hasSize(threadCount * orderPerThread);
    }

    @Test
    @DisplayName("올바른 주문번호 문자열로 OrderNumber 객체 생성이 성공한다")
    void of_ValidOrderNumber_ShouldCreateObject() {
        String validOrderNumber = "ORD-20250806193638123-ABCD1234";

        OrderNumber orderNumber = OrderNumber.of(validOrderNumber);

        assertThat(orderNumber.getValue()).isEqualTo(validOrderNumber);
    }

    @Test
    @DisplayName("null 주문번호로 생성 시 예외가 발생한다")
    void of_NullOrderNumber_ShouldThrowException() {
        String nullOrderNumber = null;

        assertThatThrownBy(() -> OrderNumber.of(nullOrderNumber))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("주문번호는 필수입니다");
    }

    @Test
    @DisplayName("빈 문자열 주문번호로 생성 시 예외가 발생한다")
    void of_EmptyOrderNumber_ShouldThrowException() {
        String emptyOrderNumber = "";

        assertThatThrownBy(() -> OrderNumber.of(emptyOrderNumber))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("주문번호는 필수입니다");
    }

    @Test
    @DisplayName("잘못된 형식의 주문번호로 생성 시 예외가 발생한다")
    void of_InvalidFormat_ShouldThrowException() {
        String invalidOrderNumber = "INVALID-FORMAT";

        assertThatThrownBy(() -> OrderNumber.of(invalidOrderNumber))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("올바르지 않은 주문번호 형식입니다");
    }

    @Test
    @DisplayName("짧은 timestamp를 가진 주문번호는 유효하지 않다")
    void of_ShortTimestamp_ShouldThrowException() {
        String shortTimestamp = "ORD-2025080619363-ABCD1234"; // 13자리 timestamp

        assertThatThrownBy(() -> OrderNumber.of(shortTimestamp))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("올바르지 않은 주문번호 형식입니다");
    }

    @Test
    @DisplayName("소문자를 포함한 해시 코드는 유효하지 않다")
    void of_LowercaseHash_ShouldThrowException() {
        String lowercaseHash = "ORD-20250806193638123-abcd1234";

        assertThatThrownBy(() -> OrderNumber.of(lowercaseHash))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("올바르지 않은 주문번호 형식입니다");
    }

    @Test
    @DisplayName("getValue() 메서드가 올바른 값을 반환한다")
    void getValue_ShouldReturnCorrectValue() {
        Long userId = 12345L;
        OrderNumber orderNumber = OrderNumber.generate(userId);

        String value = orderNumber.getValue();

        assertThat(value).matches("^ORD-\\d{17}-[A-F0-9]{8}$");
        assertThat(value).startsWith("ORD-");
    }
}

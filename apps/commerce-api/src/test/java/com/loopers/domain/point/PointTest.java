package com.loopers.domain.point;

import com.loopers.domain.example.ExampleModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.StatusResultMatchersExtensionsKt.isEqualTo;

public class PointTest {
    
    @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
    @Test
    void return_fail_when_charge_point_with_invalid_amount() {
        // given
        Point point = Point.builder()
                .id("user1")
                .pointAmount(1000L)
                .build();
    
        // when
        CoreException result1 = assertThrows(CoreException.class, () -> point.chargePoints(0));
        CoreException result2 = assertThrows(CoreException.class, () -> point.chargePoints(-500));
    
        // then
        assertThat(result1.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result2.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }
}

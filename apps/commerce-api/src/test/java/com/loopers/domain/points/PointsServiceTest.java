package com.loopers.domain.points;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
@SpringBootTest
public class PointsServiceTest {
    @Autowired
    private PointsService pointsService;

    @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
    @Test
    void chargePointsWithZeroOrNegativeValue() {
        // arrange
        BigDecimal invalidAmount = BigDecimal.valueOf(-100);
        PointsModel pointsModel = PointsModel.from(1L, invalidAmount);
        // act
        CoreException exception = assertThrows(
            CoreException.class, 
            () -> pointsService.chargePoints(pointsModel, invalidAmount)
        );
        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }
}

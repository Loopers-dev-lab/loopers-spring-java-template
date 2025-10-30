package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.loopers.support.error.ErrorMessages.INVALID_POINT_AMOUNT;

@Entity
@Table(name = "point")
@Getter
@NoArgsConstructor
public class Point {

    @Id
    private String id;
    private Long pointAmount;

    @Builder
    public Point (String id, Long pointAmount){
        validatePointAmount(pointAmount);

        this.id = id;
        this.pointAmount = pointAmount;
    }

    private void validatePointAmount(Long pointAmount) {
        if(pointAmount == null || pointAmount < 0){
            throw new CoreException(ErrorType.BAD_REQUEST, INVALID_POINT_AMOUNT);
        }
    }


}

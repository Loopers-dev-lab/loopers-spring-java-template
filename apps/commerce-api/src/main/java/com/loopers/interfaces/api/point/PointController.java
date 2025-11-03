package com.loopers.interfaces.api.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Point", description = "포인트 관련 API")
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(summary = "보유 포인트 조회")
    @GetMapping("")
    public ResponseEntity<ApiResponse<Object>> getPoints(
            @RequestHeader(value="X-USER-ID", required = false) String id
    ) {
        if (id == null || id.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("400","X-USER-ID 헤더가 없습니다."));
        }

        Point point = pointService.getPoints(id);
        if (point == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("404","존재하지 않는 ID입니다."));
        }

        return ResponseEntity.ok(
                ApiResponse.success(point.getPointAmount())
        );
    }

    @Operation(summary = "포인트 충전")
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<Object>> chargePoints(
            @RequestHeader(value="X-USER-ID", required = false) String id,
            @RequestParam("amount") Long amount
    ) {
        if (id == null || id.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("400","X-USER-ID 헤더가 없습니다."));
        }

        Point point = pointService.getPoints(id);
        if (point == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("404","존재하지 않는 ID입니다."));
        }

        Point updated = pointService.chargePoints(id, amount);

        return ResponseEntity.ok(
                ApiResponse.success(updated.getPointAmount())
        );
    }
}

package com.loopers.interfaces.api.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import org.springframework.data.domain.Page;

import com.loopers.interfaces.api.order.OrderV1Dtos;

/**
 * @author hyunjikoh
 * @since 2025. 11. 21.
 */
@Schema(description = "페이징 응답")
public record PageResponse<T>(
        @Schema(description = "데이터 목록")
        List<T> content,

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int pageNumber,

        @Schema(description = "페이지 크기", example = "20")
        int pageSize,

        @Schema(description = "전체 요소 개수", example = "100")
        long totalElements,

        @Schema(description = "전체 페이지 개수", example = "5")
        int totalPages,

        @Schema(description = "첫 페이지 여부", example = "true")
        boolean first,

        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last,

        @Schema(description = "비어있는 페이지 여부", example = "false")
        boolean empty
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}

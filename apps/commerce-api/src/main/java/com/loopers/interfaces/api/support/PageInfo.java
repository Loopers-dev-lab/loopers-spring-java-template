package com.loopers.interfaces.api.support;

import java.util.Objects;
import org.springframework.data.domain.Page;

public record PageInfo(
    long totalElements,
    int currentPage,
    int totalPages,
    int pageSize,
    String sort
) {

  public static PageInfo from(Page<?> page) {
    Objects.requireNonNull(page, "page는 null일 수 없습니다.");
    return new PageInfo(
        page.getTotalElements(),
        page.getNumber(),
        page.getTotalPages(),
        page.getSize(),
        page.getSort().toString()
    );
  }

  public static PageInfo from(Page<?> page, String sortDescription) {
    Objects.requireNonNull(page, "page는 null일 수 없습니다.");
    Objects.requireNonNull(sortDescription, "sortDescription은 null일 수 없습니다.");
    return new PageInfo(
        page.getTotalElements(),
        page.getNumber(),
        page.getTotalPages(),
        page.getSize(),
        sortDescription
    );
  }
}

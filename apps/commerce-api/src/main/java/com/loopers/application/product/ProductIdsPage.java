package com.loopers.application.product;

import java.util.List;

public record ProductIdsPage(
    List<Long> ids,
    long totalElements
) {

}

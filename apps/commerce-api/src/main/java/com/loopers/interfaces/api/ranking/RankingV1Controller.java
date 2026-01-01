package com.loopers.interfaces.api.ranking;

import com.loopers.application.product.ProductListItem;
import com.loopers.application.ranking.RankingFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RankingV1Controller implements RankingV1ApiSpec {

  private final RankingFacade rankingFacade;

  @Override
  public ApiResponse<ProductV1Dto.ProductListsResponse> getRankings(Long userId, String date, int size, int page) {
    Page<ProductListItem> productPage = rankingFacade.getProductRankingsByDate(userId, date, size, page);
    List<ProductV1Dto.ProductListResponse> products = productPage.getContent().stream()
        .map(ProductV1Dto.ProductListResponse::from)
        .toList();

    ProductV1Dto.ProductListsResponse response = new ProductV1Dto.ProductListsResponse(
        products,
        productPage.getTotalElements()
    );
    return ApiResponse.success(response);
  }
}

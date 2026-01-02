package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductCreateRequest;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductSearchRequest;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {
    private final ProductFacade productFacade;

    @RequestMapping(method = RequestMethod.POST)
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> createProduct(@RequestBody ProductV1Dto.ProductCreateRequest request) {
        ProductCreateRequest createRequest = new ProductCreateRequest(
                request.name(),
                request.brandId(),
                new Price(request.price()),
                new Stock(request.stock()),
                0
        );
        ProductInfo productInfo = productFacade.createProduct(createRequest);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(productInfo);
        return ApiResponse.success(response);
    }

    @RequestMapping(method = RequestMethod.GET)
    @Override
    public ApiResponse<ProductV1Dto.ProductsPageResponse> getProductList(@PageableDefault(size = 20) Pageable pageable) {
        Pageable normalizedPageable = normalizePageable(pageable);
        ProductSearchRequest request = new ProductSearchRequest(normalizedPageable);
        Page<ProductInfo> products = productFacade.getProductList(request);
        ProductV1Dto.ProductsPageResponse response = ProductV1Dto.ProductsPageResponse.from(products);
        return ApiResponse.success(response);
    }

    /**
     * API에서 받은 정렬 옵션을 내부에서 사용할 수 있는 정규화된 Pageable로 변환
     * - like_desc -> likeCount: DESC (ProductMetrics 기준 정렬)
     * - price_asc -> price: ASC
     * - latest -> createdAt: DESC (기본값)
     * - 정렬 없음 -> createdAt: DESC (기본값)
     */
    private Pageable normalizePageable(Pageable pageable) {
        Sort sort = pageable.getSort();
        String sortProperty = sort.toString().split(":")[0].trim();

        Sort normalizedSort;
        if (StringUtils.equals(sortProperty, "like_desc")) {
            // 좋아요 내림차순: ProductMetrics의 likeCount 기준
            normalizedSort = Sort.by(Sort.Direction.DESC, "likeCount");
        } else if (StringUtils.equals(sortProperty, "price_asc")) {
            // 가격 오름차순: Product의 price 기준
            normalizedSort = Sort.by(Sort.Direction.ASC, "price");
        } else if (StringUtils.equals(sortProperty, "latest")) {
            // 최신순: Product의 createdAt 기준
            normalizedSort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            // 알 수 없는 정렬 옵션: 기본값 사용
            normalizedSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> getProductDetail(@PathVariable Long productId) {
        ProductFacade.ProductInfoWithRanking infoWithRanking = productFacade.getProductDetailWithRanking(productId);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(
                infoWithRanking.productInfo(),
                ProductV1Dto.RankingInfo.from(infoWithRanking.rankingInfo())
        );
        return ApiResponse.success(response);
    }
}

package com.loopers.core.service.product;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductDetail;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.domain.product.event.ProductDetailViewEvent;
import com.loopers.core.domain.product.repository.ProductCacheRepository;
import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductRanking;
import com.loopers.core.service.product.query.GetProductDetailQuery;
import com.loopers.core.service.product.query.GetProductListQuery;
import com.loopers.core.service.product.query.GetProductQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductCacheRepository cacheRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final EventOutboxRepository eventOutboxRepository;
    private final ProductRankingCacheRepository productRankingCacheRepository;

    public Product getProductBy(GetProductQuery query) {
        return productRepository.getById(new ProductId(query.getProductId()));
    }

    public ProductListView getProductList(GetProductListQuery query) {
        return productRepository.findListWithCondition(
                new BrandId(query.getBrandId()),
                OrderSort.from(query.getCreatedAtSort()),
                OrderSort.from(query.getPriceSort()),
                OrderSort.from(query.getLikeCountSort()),
                query.getPageNo(),
                query.getPageSize()
        );
    }

    @Transactional
    public ProductDetail getProductDetail(GetProductDetailQuery query) {
        ProductId productId = new ProductId(query.getProductId());

        EventId eventId = EventId.generate();
        ProductDetailViewEvent productDetailViewEvent = new ProductDetailViewEvent(eventId, productId);
        eventOutboxRepository.save(
                EventOutbox.create(
                        eventId,
                        AggregateType.PRODUCT,
                        productId.toAggregateId(),
                        EventType.VIEW_PRODUCT_DETAIL,
                        new EventPayload(JacksonUtil.convertToString(productDetailViewEvent))
                )
        );
        ProductRanking dailyRanking = productRankingCacheRepository.getDailyRankingBy(productId);

        return cacheRepository.findDetailBy(productId)
                .orElseGet(() -> {
                    Product product = productRepository.getById(productId);
                    Brand brand = brandRepository.getBrandById(product.getBrandId());
                    ProductDetail productDetail = ProductDetail.create(product, brand);
                    cacheRepository.save(productDetail);

                    return productDetail;
                }).with(dailyRanking);
    }
}

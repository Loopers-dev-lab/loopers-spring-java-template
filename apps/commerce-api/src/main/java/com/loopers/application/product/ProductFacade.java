package com.loopers.application.product;

import com.loopers.application.event.ProductViewedEvent;
import com.loopers.application.like.LikeInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.Money;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.view.ProductListView;
import com.loopers.domain.view.ProductListViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductFacade {
  private final BrandService brandService;
  private final ProductService productService;
  private final ProductQueryService productQueryService;
  private final ProductListViewService productListViewService;
  private final StockService stockService;
  private final OutboxService outboxService;
  private final ApplicationEventPublisher eventPublisher;
  private final RedisTemplate<String, String> redisTemplate;

  @Transactional(readOnly = true)
  public Page<ProductListItem> getProductList(long userId,
                                              Long brandId,
                                              String sortType,
                                              int page,
                                              int size) {
    return productQueryService.getProductList(userId, brandId, sortType, page, size);
  }

  @Transactional
  public ProductDetailInfo getProductDetail(long userId, long productId) {
    ProductDetailInfo productDetail = productQueryService.getProductDetail(userId, productId);

    // 조회수 이벤트 발행 (배치 처리용)
    ProductViewedEvent viewedEvent = new ProductViewedEvent(userId, productId);
    outboxService.saveEvent(
        "Product",
        String.valueOf(productId),
        "ProductViewed",
        viewedEvent
    );

    return productDetail;
  }


  @Transactional
  public ProductDetailInfo createProduct(Long brandId, String name, Money price) {
    Brand brand = brandService.getExistingBrand(brandId);

    Product product = Product.create(brand, name, price);
    Product savedProduct = productService.save(product);

    Stock stock = Stock.create(savedProduct.getId(), 0L);
    Stock savedStock = stockService.save(stock);

    //목록 뷰 동기화
    productListViewService.save(ProductListView.create(savedProduct, savedStock));

    ProductStock productStock = ProductStock.from(savedProduct, savedStock);
    LikeInfo likeInfo = LikeInfo.from(0L, false);
    return ProductDetailInfo.from(productStock, likeInfo, null);
  }

  @Transactional(readOnly = true)
  public ProductDetailInfo getProductWithLike(Long productId) {
    return productQueryService.getProductDetail(0L, productId); // 비회원으로 조회
  }

}

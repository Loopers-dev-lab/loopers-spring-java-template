package com.loopers.application.product;

import com.loopers.application.like.LikeInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.view.ProductListView;
import com.loopers.domain.view.ProductListViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

  @Transactional(readOnly = true)
  public Page<ProductWithLikeCount> getProductList(long userId,
                                                   Long brandId,
                                                   String sortType,
                                                   int page,
                                                   int size) {
    return productQueryService.getProductList(userId, brandId, sortType, page, size);
  }

  @Transactional(readOnly = true)
  public ProductDetailInfo getProductDetail(long userId, long productId) {
    return productQueryService.getProductDetail(userId, productId);
  }

  @Transactional
  public ProductDetailInfo createProduct(Long brandId, String name, long priceAmount, long initialStock) {

    Brand brand = brandService.getExistingBrand(brandId);

    Product product = Product.create(brand, name, Money.wons(priceAmount));
    Product savedProduct = productService.save(product);

    Stock stock = Stock.create(savedProduct.getId(), initialStock);
    Stock savedStock = stockService.save(stock);

    //목록 뷰 동기화
    productListViewService.save(ProductListView.create(savedProduct, savedStock));

    ProductStock productStock = ProductStock.from(savedProduct, savedStock);
    LikeInfo likeInfo = LikeInfo.from(0L, false);
    return ProductDetailInfo.from(productStock, likeInfo);
  }


}

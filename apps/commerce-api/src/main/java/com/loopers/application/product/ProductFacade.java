package com.loopers.application.product;

import com.loopers.application.brand.BrandFacade;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.*;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ProductFacade {
    private final BrandFacade brandFacde;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductService productService;
    private final PlatformTransactionManager tm;

    public ProductFacade(BrandFacade brandFacde, ProductRepository productRepository, ProductOptionRepository productOptionRepository, ProductService productService, PlatformTransactionManager tm) {
        this.brandFacde = brandFacde;
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.productService = productService;
        this.tm = tm;
    }
    public ProductCommand.ProductData.ProductItem getProduct(Long productId) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "productId는 null이면 안됩니다.");
        }

        ProductModel product = getProductModelById(productId);
        BrandModel brand = brandFacde.getByIdOrThrow(product.getBrandId().getValue());

        return productService.createProductItem(
                product,
                brand.getId(),
                brand.getBrandNaem().getValue()
        );
    }
    public ProductCommand.ProductData getProductList(ProductCommand.Request.GetList request) {
        if (request.brandId() != null) {
            return getProductListByBrand(request);
        }
        return getAllProductList(request);
    }

    private ProductCommand.ProductData getProductListByBrand(ProductCommand.Request.GetList request) {
        BrandModel brandModel = brandFacde.getByIdOrThrow(request.brandId());

        Page<ProductModel> productModels = productRepository.search(
            brandModel.getId(), request.sort(), request.page(), request.size()
        );

        List<ProductCommand.ProductData.ProductItem> productItems =
                productService.toListWithSingleBrand(productModels.getContent(), brandModel);

        return productService.createProductData(productModels, productItems);
    }

    private ProductCommand.ProductData getAllProductList(ProductCommand.Request.GetList request) {
        Page<ProductModel> productPage = productRepository.search(
                null, request.sort(), request.page(), request.size()
        );
        List<ProductModel> productList = productPage.getContent();
        if(productList.isEmpty()){
            return productService.createProductData(productPage, new ArrayList<>());
        }
        List<Long> distinctBrandIds = productService.toDistinctBrandIds(productList);
        List<BrandModel> brandList = brandFacde.getByIds(distinctBrandIds);
        Map<Long, String> brandMap = productService.createBrandNameMap(brandList, distinctBrandIds);

        List<ProductCommand.ProductData.ProductItem> productItems =
                productService.toListWithBrands(productList, brandMap);

        return productService.createProductData(productPage, productItems);
    }
    public ProductModel getProductModelById(Long productModelId){
        return productRepository.findById(productModelId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
    }
    public void register(ProductOptionModel model) {
        if (existsByOptionId(model.getId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 등록된 옵션입니다.");
        }
        productOptionRepository.save(model);
    }
    public boolean existsByOptionId(Long option){
        return productOptionRepository.existsById(option);
    }
    public ProductOptionModel getProductOptionByOptionId(Long optionId) {
        return productOptionRepository.findById(optionId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품 옵션입니다.")
        );
    }

    @Transactional
    public ProductModel decreaseStock(Long productId, BigDecimal quantity) {
        ProductModel product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        product.decreaseStock(quantity);
        return productRepository.save(product);
    }


    @Transactional
    public ProductModel restoreStock(Long productId, BigDecimal quantity) {
        ProductModel product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        product.restoreStock(quantity);
        return productRepository.save(product);
    }




}

package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.MoneyFixture;
import org.instancio.Instancio;
import org.instancio.Model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.instancio.Select.field;

public class ProductFixture {

  private static final Model<Product> PRODUCT_MODEL = Instancio.of(Product.class)
      .ignore(field(Product::getId))
      .ignore(field(Product::getCreatedAt))
      .ignore(field(Product::getUpdatedAt))
      .ignore(field(Product::getDeletedAt))
      .ignore(field(Product::getBrand))
      .generate(field(Product::getName), gen -> gen.string())
      .set(field(Product::getPrice), MoneyFixture.create())
      .toModel();

  public static Product createProduct() {
    Product product = Instancio.of(PRODUCT_MODEL).create();
    Brand brand = BrandFixture.createBrand();
    product.setBrand(brand);
    return product;
  }

  public static Product createProduct(Brand brand) {
    Product product = Instancio.of(PRODUCT_MODEL).create();
    product.setBrand(brand);
    return product;
  }

  /**
   * 특정 필드만 override
   */
  public static Product createProductWith(String name, Money price) {
    Product stock = Instancio.of(PRODUCT_MODEL)
        .set(field(Product::getName), name)
        .set(field(Product::getPrice), price)
        .create();
    return stock;
  }

  public static Product createUserWithoutBrand() {
    return Instancio.of(PRODUCT_MODEL).create();
  }

  public static List<Product> createProductList(int size) {
    return Instancio.ofList(Product.class)
        .size(size)
        .create();
  }

  public static List<Product> createProductList(List<Brand> brandList) {
    return IntStream.range(0, brandList.size())
        .mapToObj(i -> Instancio.of(PRODUCT_MODEL)
            .set(field(Product::getBrand), brandList.get(i))
            .create()
        )
        .collect(Collectors.toList());
  }
}

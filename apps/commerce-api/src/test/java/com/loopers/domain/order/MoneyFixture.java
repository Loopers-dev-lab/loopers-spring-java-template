package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.product.Product;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.instancio.Select.field;

public class MoneyFixture {

  private static final Model<Money> MODEL = Instancio.of(Money.class)
      .generate(field(Money::getAmount),
          gen -> gen.math().bigDecimal().scale(0).min(BigDecimal.ZERO))
      .generate(field(Money::getCurrency),
          gen -> gen.enumOf(CurrencyCode.class))
      .toModel();

  public static Money create() {
    return Instancio.of(MODEL).create();
  }

  public static Money createWith(BigDecimal amount, CurrencyCode currency) {
    return Instancio.of(MODEL)
        .set(field(Money::getAmount), amount)
        .set(field(Money::getCurrency), currency)
        .create();
  }
}

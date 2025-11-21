package com.loopers.domain.stock;

import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

public class StockFixture {

  private static final Model<Stock> STOCK_MODEL = Instancio.of(Stock.class)
      .ignore(field(Stock::getId))
      .ignore(field(Stock::getCreatedAt))
      .ignore(field(Stock::getUpdatedAt))
      .ignore(field(Stock::getDeletedAt))
      .generate(field(Stock::getRefProductId), gen -> gen.longs().min(0L))
      .generate(field(Stock::getAvailable),
          gen -> gen.longs().min(0L)
      )
      .toModel();

  /**
   * Stock + Point 자동 연결
   */
  public static Stock createStock() {
    Stock stock = Instancio.of(STOCK_MODEL).create();
    return stock;
  }

  /**
   * 특정 필드만 override
   */
  public static Stock createStockWith(Long productId, long available) {
    Stock stock = Instancio.of(STOCK_MODEL)
        .set(field(Stock::getRefProductId), productId)
        .set(field(Stock::getAvailable), available)
        .create();
    return stock;
  }
}

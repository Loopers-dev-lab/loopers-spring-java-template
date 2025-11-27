package com.loopers.performance;

import com.loopers.domain.brand.Brand;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 성능 테스트용 대량 데이터 생성기
 *
 * 데이터 분포:
 * - Brand: 10개
 * - Product: 50만개 (브랜드당 5만개 균등 분배)
 * - ProductLike: 약 600만개 (극단적 쏠림)
 *   - 상위 1% (5,000개) → 좋아요 1000개씩 = 500만개
 *   - 상위 1~10% (45,000개) → 좋아요 10개씩 = 45만개
 *   - 상위 10~30% (100,000개) → 좋아요 2개씩 = 20만개
 *   - 하위 70% (350,000개) → 좋아요 0개
 * - User: 1만명
 * - Point: 1만개 (유저당 1개)
 * - Order: 5만개 (유저당 평균 5개)
 * - OrderItem: 10만개 (주문당 평균 2개)
 * - price: 10,000 ~ 100,000 (1만원 단위 균등 분배)
 */
@Disabled
@SpringBootTest(
    properties = {
        "datasource.mysql-jpa.main.jdbc-url=${TEST_DB_URL:jdbc:mysql://localhost:3306/loopers}",
        "datasource.mysql-jpa.main.username=${TEST_DB_USERNAME:application}",
        "datasource.mysql-jpa.main.password=${TEST_DB_PASSWORD:application}"
    }
)
@ActiveProfiles("local")
class ProductDataGenerator {

  private static final Logger log = LoggerFactory.getLogger(ProductDataGenerator.class);

  // 데이터 규모 상수
  private static final int BRAND_COUNT = 10;
  private static final int PRODUCT_COUNT = 500_000;
  private static final int PRODUCTS_PER_BRAND = PRODUCT_COUNT / BRAND_COUNT;
  private static final int USER_COUNT = 10_000;
  private static final int ORDER_COUNT = 50_000;

  private static final int BATCH_SIZE = 5000;
  private static final long DEFAULT_STOCK = 100L;

  private static final LocalDateTime REQUEST_AT = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
  private static final Random RANDOM = new Random(42); // 시드 고정으로 재현 가능

  @Autowired
  private BrandJpaRepository brandRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("50만 상품 + 약 565만 좋아요 + 1만 유저 + 5만 주문 데이터 생성")
  void generateTestData() {
    long startTime = System.currentTimeMillis();

    // 1. 브랜드 10개 생성
    List<Brand> brands = generateBrands();
    log.info("브랜드 {}개 생성 완료", brands.size());

    // 2. 상품 50만개 생성 (브랜드당 5만개) - JDBC batch
    int productCount = generateProductsBatch(brands);
    log.info("상품 {}개 생성 완료", productCount);

    // 3. 유저 1만명 생성
    int userCount = generateUsersBatch();
    log.info("유저 {}명 생성 완료", userCount);

    // 4. 포인트 1만개 생성 (유저당 1개)
    int pointCount = generatePointsBatch();
    log.info("포인트 {}개 생성 완료", pointCount);

    // 5. 좋아요 200만개 생성 - JDBC batch
    int likeCount = generateLikesBatch();
    log.info("좋아요 {}개 생성 완료", likeCount);

    // 6. 주문 5만개 생성
    int orderCount = generateOrdersBatch();
    log.info("주문 {}개 생성 완료", orderCount);

    // 7. 주문상품 10만개 생성
    int orderItemCount = generateOrderItemsBatch();
    log.info("주문상품 {}개 생성 완료", orderItemCount);

    long elapsed = System.currentTimeMillis() - startTime;
    log.info("전체 데이터 생성 완료 (소요 시간: {}초)", elapsed / 1000);
  }

  private List<Brand> generateBrands() {
    List<Brand> brands = new ArrayList<>();
    for (int i = 1; i <= BRAND_COUNT; i++) {
      Brand brand = Brand.of("브랜드" + i, "브랜드 " + i + " 설명");
      brands.add(brand);
    }
    return brandRepository.saveAll(brands);
  }

  private int generateProductsBatch(List<Brand> brands) {
    String sql = "INSERT INTO product (ref_brand_id, name, price, description, stock, like_count, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    Timestamp now = Timestamp.valueOf(REQUEST_AT);
    List<Object[]> batchArgs = new ArrayList<>();
    int totalInserted = 0;

    for (Brand brand : brands) {
      for (int i = 0; i < PRODUCTS_PER_BRAND; i++) {
        long price = ((i % 10) + 1) * 10_000L;
        int likeCount = calculateLikeCount(totalInserted);

        batchArgs.add(new Object[]{
            brand.getId(),
            "상품_" + brand.getId() + "_" + (i + 1),
            price,
            "상품 설명",
            DEFAULT_STOCK,
            likeCount,
            now,
            now
        });

        if (batchArgs.size() >= BATCH_SIZE) {
          jdbcTemplate.batchUpdate(sql, batchArgs);
          totalInserted += batchArgs.size();
          batchArgs.clear();
          if (totalInserted % 50_000 == 0) {
            log.info("  - 상품 {}개 저장 완료", totalInserted);
          }
        }
      }
    }

    if (!batchArgs.isEmpty()) {
      jdbcTemplate.batchUpdate(sql, batchArgs);
      totalInserted += batchArgs.size();
    }

    return totalInserted;
  }

  /**
   * 상품 순서에 따른 좋아요 수 계산 (극단적 쏠림)
   * - 상위 1% (0~4,999): 1000개씩 = 500만개
   * - 상위 1~10% (5,000~49,999): 10개씩 = 45만개
   * - 상위 10~30% (50,000~149,999): 2개씩 = 20만개
   * - 하위 70% (150,000~499,999): 0개
   */
  private int calculateLikeCount(int productIndex) {
    if (productIndex < 5_000) {
      return 1000; // 상위 1%: 1000개 (극단적 쏠림)
    } else if (productIndex < 50_000) {
      return 10; // 상위 1~10%: 10개
    } else if (productIndex < 150_000) {
      return 2; // 상위 10~30%: 2개
    } else {
      return 0; // 하위 70%: 0개
    }
  }

  private int generateUsersBatch() {
    String sql = "INSERT INTO users (login_id, email, birth, gender, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    Timestamp now = Timestamp.valueOf(REQUEST_AT);
    List<Object[]> batchArgs = new ArrayList<>();
    int totalInserted = 0;
    String[] genders = {"MALE", "FEMALE"};

    for (int i = 1; i <= USER_COUNT; i++) {
      String paddedId = String.format("%04d", i);
      LocalDate birth = LocalDate.of(1970 + RANDOM.nextInt(36), 1 + RANDOM.nextInt(12), 1 + RANDOM.nextInt(28));
      String gender = genders[i % 2];

      batchArgs.add(new Object[]{
          "user" + paddedId,
          "user" + paddedId + "@test.com",
          Date.valueOf(birth),
          gender,
          now,
          now
      });

      if (batchArgs.size() >= BATCH_SIZE) {
        jdbcTemplate.batchUpdate(sql, batchArgs);
        totalInserted += batchArgs.size();
        batchArgs.clear();
        log.info("  - 유저 {}명 저장 완료", totalInserted);
      }
    }

    if (!batchArgs.isEmpty()) {
      jdbcTemplate.batchUpdate(sql, batchArgs);
      totalInserted += batchArgs.size();
    }

    return totalInserted;
  }

  private int generatePointsBatch() {
    String sql = "INSERT INTO point (ref_user_id, amount, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?)";

    Timestamp now = Timestamp.valueOf(REQUEST_AT);
    List<Object[]> batchArgs = new ArrayList<>();
    int totalInserted = 0;

    for (long userId = 1; userId <= USER_COUNT; userId++) {
      long amount = RANDOM.nextInt(1_000_001); // 0 ~ 1,000,000

      batchArgs.add(new Object[]{
          userId,
          amount,
          now,
          now
      });

      if (batchArgs.size() >= BATCH_SIZE) {
        jdbcTemplate.batchUpdate(sql, batchArgs);
        totalInserted += batchArgs.size();
        batchArgs.clear();
      }
    }

    if (!batchArgs.isEmpty()) {
      jdbcTemplate.batchUpdate(sql, batchArgs);
      totalInserted += batchArgs.size();
    }

    return totalInserted;
  }

  private int generateLikesBatch() {
    String sql = "INSERT INTO product_like (ref_user_id, ref_product_id, liked_at, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?)";

    Timestamp now = Timestamp.valueOf(REQUEST_AT);
    List<Object[]> batchArgs = new ArrayList<>();
    int totalInserted = 0;

    // 상품 ID는 1부터 시작한다고 가정
    for (long productId = 1; productId <= PRODUCT_COUNT; productId++) {
      int likeCount = calculateLikeCount((int) productId - 1);

      // 유저 ID를 순차적으로 할당 (중복 방지)
      // 상품마다 시작 유저를 다르게 하여 분산
      int startUserId = (int) ((productId - 1) % USER_COUNT);

      for (int i = 0; i < likeCount; i++) {
        long userId = 1 + ((startUserId + i) % USER_COUNT);

        batchArgs.add(new Object[]{userId, productId, now, now, now});

        if (batchArgs.size() >= BATCH_SIZE) {
          jdbcTemplate.batchUpdate(sql, batchArgs);
          totalInserted += batchArgs.size();
          batchArgs.clear();

          if (totalInserted % 100_000 == 0) {
            log.info("  - 좋아요 {}개 저장 완료", totalInserted);
          }
        }
      }
    }

    if (!batchArgs.isEmpty()) {
      jdbcTemplate.batchUpdate(sql, batchArgs);
      totalInserted += batchArgs.size();
    }

    return totalInserted;
  }

  private int generateOrdersBatch() {
    String sql = "INSERT INTO orders (ref_user_id, status, total_amount, ordered_at, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    Timestamp now = Timestamp.valueOf(REQUEST_AT);
    List<Object[]> batchArgs = new ArrayList<>();
    int totalInserted = 0;

    // status 분포: PENDING 10%, COMPLETED 85%, PAYMENT_FAILED 5%
    String[] statuses = new String[100];
    for (int i = 0; i < 10; i++) statuses[i] = "PENDING";
    for (int i = 10; i < 95; i++) statuses[i] = "COMPLETED";
    for (int i = 95; i < 100; i++) statuses[i] = "PAYMENT_FAILED";

    for (int i = 0; i < ORDER_COUNT; i++) {
      long userId = 1 + (i % USER_COUNT); // 유저당 평균 5개 주문
      String status = statuses[RANDOM.nextInt(100)];
      long totalAmount = (1 + RANDOM.nextInt(50)) * 10_000L; // 10,000 ~ 500,000

      batchArgs.add(new Object[]{
          userId,
          status,
          totalAmount,
          now,
          now,
          now
      });

      if (batchArgs.size() >= BATCH_SIZE) {
        jdbcTemplate.batchUpdate(sql, batchArgs);
        totalInserted += batchArgs.size();
        batchArgs.clear();
        log.info("  - 주문 {}개 저장 완료", totalInserted);
      }
    }

    if (!batchArgs.isEmpty()) {
      jdbcTemplate.batchUpdate(sql, batchArgs);
      totalInserted += batchArgs.size();
    }

    return totalInserted;
  }

  private int generateOrderItemsBatch() {
    String sql = "INSERT INTO order_item (ref_order_id, ref_product_id, product_name, quantity, order_price, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    Timestamp now = Timestamp.valueOf(REQUEST_AT);
    List<Object[]> batchArgs = new ArrayList<>();
    int totalInserted = 0;

    for (long orderId = 1; orderId <= ORDER_COUNT; orderId++) {
      int itemCount = 1 + RANDOM.nextInt(3); // 1~3개 상품

      for (int i = 0; i < itemCount; i++) {
        long productId = 1 + RANDOM.nextInt(PRODUCT_COUNT);
        long quantity = 1 + RANDOM.nextInt(3); // 1~3개
        long orderPrice = (1 + RANDOM.nextInt(10)) * 10_000L; // 10,000 ~ 100,000

        batchArgs.add(new Object[]{
            orderId,
            productId,
            "상품_" + productId,
            quantity,
            orderPrice,
            now,
            now
        });

        if (batchArgs.size() >= BATCH_SIZE) {
          jdbcTemplate.batchUpdate(sql, batchArgs);
          totalInserted += batchArgs.size();
          batchArgs.clear();
          if (totalInserted % 50_000 == 0) {
            log.info("  - 주문상품 {}개 저장 완료", totalInserted);
          }
        }
      }
    }

    if (!batchArgs.isEmpty()) {
      jdbcTemplate.batchUpdate(sql, batchArgs);
      totalInserted += batchArgs.size();
    }

    return totalInserted;
  }
}

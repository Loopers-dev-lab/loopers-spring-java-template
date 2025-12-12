package com.loopers.fixture;


import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TestFixture {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final PointRepository pointRepository;

    public TestFixture(
            UserRepository userRepository,
            BrandRepository brandRepository,
            ProductRepository productRepository,
            CouponRepository couponRepository,
            PointRepository pointRepository
    ) {
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.pointRepository = pointRepository;
    }

    // ===== User =====
    public User createUser() {
        return userRepository.save(UserFixture.defaultUser());
    }

    public User createUser(String loginId) {
        return userRepository.save(UserFixture.withLoginId(loginId));
    }

    public User createUser(String loginId, String email, String birthDate, Gender gender) {
        return userRepository.save(UserFixture.custom(loginId, email, birthDate, gender));
    }

    public List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(userRepository.save(UserFixture.indexed(i)));
        }
        return users;
    }

    // ===== Brand =====
    public Brand createBrand() {
        return brandRepository.save(BrandFixture.defaultBrand());
    }

    public Brand createBrand(String name) {
        return brandRepository.save(BrandFixture.withName(name));
    }

    // ===== Product =====
    public Product createProduct(Brand brand) {
        return productRepository.save(ProductFixture.defaultProduct(brand));
    }

    public Product createProduct(Brand brand, Long price, Integer stock) {
        return productRepository.save(ProductFixture.withPriceAndStock(brand, price, stock));
    }

    public Product createProduct(String name, Long price, Integer stock, Brand brand) {
        return productRepository.save(ProductFixture.custom(name, price, stock, brand));
    }

    public Product createOutOfStockProduct(Brand brand) {
        return productRepository.save(ProductFixture.outOfStock(brand));
    }

    public List<Product> createProducts(Brand brand, int count) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            products.add(productRepository.save(ProductFixture.indexed(brand, i + 1)));
        }
        return products;
    }

    // ===== Coupon =====
    public Coupon createFixedAmountCoupon(User user, Long amount) {
        return couponRepository.save(CouponFixture.fixedAmount(user, amount));
    }

    public Coupon createPercentageCoupon(User user, Long percent) {
        return couponRepository.save(CouponFixture.percentage(user, percent));
    }

    public Coupon createCoupon(User user, String name, DiscountType type, Long value) {
        return couponRepository.save(CouponFixture.custom(user, name, type, value));
    }

    // ===== Point =====
    public Point createPoint(Long userId) {
        return pointRepository.save(PointFixture.empty(userId));
    }

    public Point createPoint(Long userId, Long balance) {
        return pointRepository.save(PointFixture.withBalance(userId, balance));
    }

    public Point createSufficientPoint(Long userId) {
        return pointRepository.save(PointFixture.sufficient(userId));
    }

    // ===== 복합 셋업 =====

    /**
     * 기본 테스트 환경 구성
     * - User 1명
     * - Brand 1개
     * - Product 1개
     * - Point (충분한 잔액)
     */
    public TestData setupBasic() {
        User user = createUser();
        Brand brand = createBrand();
        Product product = createProduct(brand);
        Point point = createSufficientPoint(user.getId());
        return new TestData(user, brand, product, null, point);
    }

    /**
     * 쿠폰 포함 테스트 환경 구성
     */
    public TestData setupWithCoupon(Long couponAmount) {
        User user = createUser();
        Brand brand = createBrand();
        Product product = createProduct(brand);
        Point point = createSufficientPoint(user.getId());
        Coupon coupon = createFixedAmountCoupon(user, couponAmount);
        return new TestData(user, brand, product, coupon, point);
    }

    /**
     * 동시성 테스트용 환경 구성
     * - 다수 User
     * - 재고 지정 Product
     */
    public ConcurrencyTestData setupForConcurrency(int userCount, int productStock, long productPrice) {
        List<User> users = createUsers(userCount);
        Brand brand = createBrand();
        Product product = createProduct(brand, productPrice, productStock);

        // 각 유저에게 포인트 부여
        for (User user : users) {
            createPoint(user.getId(), productPrice * 10);
        }

        return new ConcurrencyTestData(users, brand, product);
    }

    // ===== Result Records =====

    public record TestData(
            User user,
            Brand brand,
            Product product,
            Coupon coupon,
            Point point
    ) {
    }

    public record ConcurrencyTestData(
            List<User> users,
            Brand brand,
            Product product
    ) {
        public User getUser(int index) {
            return users.get(index);
        }

        public int getUserCount() {
            return users.size();
        }
    }
}

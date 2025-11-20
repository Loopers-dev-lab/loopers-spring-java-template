package com.loopers.application;

import com.loopers.application.like.LikeCommand;
import com.loopers.application.like.UserLikeProductFacade;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.like.ProductLikeRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
public class LikeFacadeTest {
    @Autowired
    private UserLikeProductFacade userLikeProductFacade;

    @Autowired
    private ProductLikeRepository productLikeRepository;


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    static final int INIT_POINT = 1000;

    UserModel user;
    BrandModel brand;
    ProductModel product;

    @BeforeEach
    void setUp() {
        user = new UserModel(
                "testuser1",
                "user2",
                "유저테스트",
                "a@b.com",
                "1997-09-28",
                "M",
                INIT_POINT);
        brand = new BrandModel(
                "테스트브랜드",
                "테스트브랜드입니다.",
                BrandStatus.REGISTERED);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("사용자는 유효한 상품을 좋아요할 수 있다.")
    void likeProudct_whenValidProduct() {
        // given
        UserModel resultUser = userRepository.save(user);
        BrandModel resultBrand = brandRepository.save(brand);
        product = ProductModel.builder()
                .name("상품B")
                .category("테스트상품")
                .price(0)
                .stock(0)
                .status(ProductStatus.ON_SALE)
                .brand(resultBrand)
                .build();
        ProductModel resultProduct = productRepository.save(product);
        LikeCommand.Like likeCommand = new LikeCommand.Like(resultUser.getUserId(), resultProduct.getId());

        // when, then
        assertDoesNotThrow(() -> userLikeProductFacade.userLikeProduct(likeCommand));
    }

    @Test
    @DisplayName("사용자는 자신이 좋아요 한 유효한 상품을 취소할 수 있다.")
    void unlikeProudct_whenValidProduct() {
        // given
        UserModel resultUser = userRepository.save(user);
        BrandModel resultBrand = brandRepository.save(brand);
        product = ProductModel.builder()
                .name("상품B")
                .category("테스트상품")
                .price(0)
                .stock(0)
                .status(ProductStatus.ON_SALE)
                .brand(resultBrand)
                .build();
        ProductModel resultProduct = productRepository.save(product);
        LikeCommand.Like likeCommand = new LikeCommand.Like(resultUser.getUserId(), resultProduct.getId());
        productLikeRepository.insertIgnore(resultUser.getId(), resultProduct.getId());

        // when, then
        assertDoesNotThrow(() -> userLikeProductFacade.userUnlikeProduct(likeCommand));
    }


}

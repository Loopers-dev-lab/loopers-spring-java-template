package com.loopers.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1) // 다른 CommandLineRunner보다 먼저 실행
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${data.init.enabled:true}")
    private boolean dataInitEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!dataInitEnabled) {
            System.out.println("데이터 초기화가 비활성화되어 있습니다.");
            return;
        }
        
        String activeProfile = System.getProperty("spring.profiles.active", "local");
        if (!"local".equals(activeProfile) && !"dev".equals(activeProfile)) {
            return;
        }

        System.out.println("=== 데이터 초기화 시작 ===");
        
        try {
            insertBrandData();

            insertProductData();

            insertUserData();
            
            insertBrandLikeData();
            
            insertProductLikeData();
            
        } catch (Exception e) {
            System.err.println("데이터 초기화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertBrandData() {
        Long brandCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM brand", Long.class);
        if (brandCount > 0) {
            System.out.println("브랜드 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO brand (name, brand_like_count, brandis_active, brand_sns_lick, created_at, updated_at) VALUES ");
        
        for (int i = 1; i <= 40; i++) {
            if (i > 1) sql.append(", ");
            sql.append(String.format("('Brand %d', %d, true, 'https://example.com/brand%d', NOW(), NOW())", 
                i, (int)(Math.random() * 1000), i));
        }
        
        jdbcTemplate.execute(sql.toString());
    }

    private void insertProductData() {

        Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        if (productCount > 0) {
            System.out.println("상품 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        int batchSize = 10000;
        int totalProducts = 1000000;
        
        for (int batch = 0; batch < totalProducts / batchSize; batch++) {
            insertProductBatch(batch * batchSize, batchSize);
            
            if ((batch + 1) % 10 == 0) {
                System.out.printf("상품 데이터 진행률: %d/%d 배치 완료\\n", batch + 1, totalProducts / batchSize);
            }
        }
        
    }

    private void insertProductBatch(int startId, int batchSize) {
        StringBuilder sql = new StringBuilder("INSERT INTO product (name, description, price, brand_id, img_url, product_like_count, status, stock, created_at, updated_at) VALUES ");
        
        for (int i = 0; i < batchSize; i++) {
            if (i > 0) sql.append(", ");
            
            int productId = startId + i + 1;
            int brandId = (productId % 40) + 1;
            int price = generateRealisticPrice();
            int likeCount = generateRealisticLikeCount();
            int stock = (int)(Math.random() * 100) + 1;
            String status = Math.random() > 0.05 ? "ACTIVE" : "DISCONTINUED";
            
            sql.append(String.format(
                "('Product %d', 'Product description %d', %d, %d, 'https://example.com/product%d.jpg', %d, '%s', %d, NOW(), NOW())",
                productId, productId, price, brandId, productId, likeCount, status, stock
            ));
        }
        
        jdbcTemplate.execute(sql.toString());
    }

    private void insertUserData() {

        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member", Long.class);
        if (userCount > 0) {
            System.out.println("사용자 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        int batchSize = 1000;
        int totalUsers = 10000;
        
        for (int batch = 0; batch < totalUsers / batchSize; batch++) {
            insertUserBatch(batch * batchSize, batchSize);
        }
        
    }

    /**
     * 사용자 데이터 배치 삽입
     */
    private void insertUserBatch(int startId, int batchSize) {
        StringBuilder sql = new StringBuilder("INSERT INTO member (login_id, user_email, grender, birth, created_at, updated_at) VALUES ");
        
        for (int i = 0; i < batchSize; i++) {
            if (i > 0) sql.append(", ");
            
            int userId = startId + i + 1;
            String[] genders = {"M", "F"};
            String gender = genders[(int)(Math.random() * 2)];
            
            sql.append(String.format(
                "('user%d', 'user%d@example.com', '%s', '1990-01-01', NOW(), NOW())",
                userId, userId, gender
            ));
        }
        
        jdbcTemplate.execute(sql.toString());
    }

    private void insertBrandLikeData() {

        Long likeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM brand_like", Long.class);
        if (likeCount > 0) {
            System.out.println("브랜드 좋아요 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        int totalLikes = 0;
        
        for (int brandId = 1; brandId <= 40; brandId++) {
            int likesForBrand = (int)(Math.random() * 400) + 100; // 100~500개
            insertBrandLikeBatch(brandId, likesForBrand);
            totalLikes += likesForBrand;
        }
        
        updateBrandLikeCounts();
        
    }
    private void insertBrandLikeBatch(int brandId, int likeCount) {
        StringBuilder sql = new StringBuilder("INSERT INTO brand_like (brand_id, user_id, liked_at, created_at, updated_at) VALUES ");
        
        for (int i = 0; i < likeCount; i++) {
            if (i > 0) sql.append(", ");
            
            int userId = (int)(Math.random() * 10000) + 1; // 1~10000 사용자 중 랜덤
            
            sql.append(String.format(
                "(%d, %d, NOW(), NOW(), NOW())",
                brandId, userId
            ));
        }
        
        try {
            jdbcTemplate.execute(sql.toString());
        } catch (Exception e) {
            System.out.println("브랜드 " + brandId + " 좋아요 중복 데이터 스킵");
        }
    }

    /**
     * 브랜드 좋아요 수 업데이트
     */
    private void updateBrandLikeCounts() {
        String sql = """
            UPDATE brand b 
            SET brand_like_count = (
                SELECT COUNT(*) 
                FROM brand_like bl 
                WHERE bl.brand_id = b.id AND bl.deleted_at IS NULL
            )
            """;
        jdbcTemplate.execute(sql);
    }

    private void insertProductLikeData() {

        Long likeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_like", Long.class);
        if (likeCount > 0) {
            System.out.println("상품 좋아요 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        int totalLikes = 0;
        int batchSize = 1000;
        
        for (int batch = 0; batch < 200; batch++) { // 200 배치
            int likesInBatch = insertProductLikeBatch(batch, batchSize);
            totalLikes += likesInBatch;
            
            if ((batch + 1) % 50 == 0) {
                System.out.printf("상품 좋아요 진행률: %d/200 배치 완료\\n", batch + 1);
            }
        }
        
        updateProductLikeCounts();
        
    }

    private int insertProductLikeBatch(int batch, int batchSize) {
        StringBuilder sql = new StringBuilder("INSERT INTO product_like (product_id, user_id, liked_at, created_at, updated_at) VALUES ");
        int insertedCount = 0;
        
        for (int i = 0; i < batchSize; i++) {
            int productId = (int)(Math.random() * 1000000) + 1;
            
            int usersForProduct = (int)(Math.random() * 10) + 1;
            
            for (int j = 0; j < usersForProduct; j++) {
                if (insertedCount > 0) sql.append(", ");
                
                int userId = (int)(Math.random() * 10000) + 1;
                
                sql.append(String.format(
                    "(%d, %d, NOW(), NOW(), NOW())",
                    productId, userId
                ));
                
                insertedCount++;
            }
        }
        
        try {
            if (insertedCount > 0) {
                jdbcTemplate.execute(sql.toString());
            }
        } catch (Exception e) {
            System.out.println("상품 좋아요 중복 데이터 스킵");
        }
        
        return insertedCount;
    }

    private void updateProductLikeCounts() {
        String sql = """
            UPDATE product p 
            SET product_like_count = (
                SELECT COUNT(*) 
                FROM product_like pl 
                WHERE pl.product_id = p.id AND pl.deleted_at IS NULL
            )
            """;
        jdbcTemplate.execute(sql);
    }

    private int generateRealisticPrice() {
        double random = Math.random();
        if (random < 0.7) {
            return 10000 + (int)(Math.random() * 40000);
        } else if (random < 0.9) {
            return 50000 + (int)(Math.random() * 50000);
        } else {
            return 100000 + (int)(Math.random() * 100000);
        }
    }

    private int generateRealisticLikeCount() {
        double random = Math.random();
        if (random < 0.6) {
            return (int)(Math.random() * 6);
        } else if (random < 0.8) {
            return 6 + (int)(Math.random() * 15);
        } else if (random < 0.95) {
            return 21 + (int)(Math.random() * 80);
        } else {
            return 100 + (int)(Math.random() * 900);
        }
    }
}

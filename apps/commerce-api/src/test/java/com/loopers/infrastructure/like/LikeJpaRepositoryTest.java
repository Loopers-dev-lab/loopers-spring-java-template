package com.loopers.infrastructure.like;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.like.Like;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LikeJpaRepositoryTest {
    @Autowired
    private LikeJpaRepository likeJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("saveIgnore 호출시,")
    @Nested
    class saveIgnore {
        @Test
        void 처음_INSERT_시_1을_반환한다() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            // act
            int inserted = likeJpaRepository.saveIgnore(userId, productId);

            // assert
            assertThat(inserted).isEqualTo(1);
        }

        @Test
        void 이미_존재하는_경우_INSERT는_무시되고_0을_반환한다() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;
            Like like = Like.create(userId, productId);
            likeJpaRepository.save(like);

            // act
            int inserted = likeJpaRepository.saveIgnore(userId, productId);

            // assert
            assertThat(inserted).isEqualTo(0);
            assertThat(likeJpaRepository.findAll()).hasSize(1);
        }
    }

}

package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointServiceIntegrationTest {

    /*
    - [ ] 해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.
    - [ ] 해당 ID 의 회원이 존재하지 않을 경우, null이 반환된다.
    - [ ] 존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.
     */

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("해당 userId의 회원이 존재할 경우, 보유 포인트를 반환한다.")
    @Test
    void returnsPointBalance_when_user_exists() throws Exception {
        // arrange
        UserEntity user = userJpaRepository.save(
                new UserEntity("happygimy",
                        "happygimy@naver.com",
                        Gender.MALE,
                        "1997-09-23",
                        "test1234!"
                )
        );
        Point point = pointJpaRepository.save(new Point(user.getId(), 5000L));

        // act
        mockMvc.perform(get("/api/v1/points/{userId}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                // assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.balance").value(point.getBalance()));

        Point found = pointJpaRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(found.getBalance()).isEqualTo(5000L);
    }

    @DisplayName("존재하지 않는 유저 ID로 충전을 시도한 경우 실패한다.")
    @Test
    void throwsNotFoundException_when_user_not_exists() {
        // arrange
        Long nonExistentUserId = 9999L;

        // act
        final CoreException result = assertThrows(CoreException.class, () -> {
            pointService.getPointBalanceByUserId(nonExistentUserId);
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

}

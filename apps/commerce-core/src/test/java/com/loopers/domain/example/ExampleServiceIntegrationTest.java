package com.loopers.domain.example;

import com.loopers.infrastructure.example.ExampleJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = com.loopers.CommerceCoreApplication.class)
class ExampleServiceIntegrationTest {
    @Autowired
    private ExampleService exampleService;

    @Autowired
    private ExampleJpaRepository exampleJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("예시를 조회할 때,")
    @Nested
    class Get {
        @DisplayName("존재하는 예시 ID를 주면, 해당 예시 정보를 반환한다.")
        @Test
        void returnsExampleInfo_whenValidIdIsProvided() {
            // arrange
            ExampleModel exampleModel = exampleJpaRepository.save(
                new ExampleModel("예시 제목", "예시 설명")
            );

            // act
            ExampleModel result = exampleService.getExample(exampleModel.getId());

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(exampleModel.getId()),
                () -> assertThat(result.getName()).isEqualTo(exampleModel.getName()),
                () -> assertThat(result.getDescription()).isEqualTo(exampleModel.getDescription())
            );
        }

        @DisplayName("존재하지 않는 예시 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenInvalidIdIsProvided() {
            // arrange
            Long invalidId = 999L; // Assuming this ID does not exist

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                exampleService.getExample(invalidId);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}

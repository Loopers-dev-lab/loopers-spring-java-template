package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserModelTest {
    @DisplayName("예시 모델을 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("제목과 설명이 모두 주어지면, 정상적으로 생성된다.")
        @Test
        void createsExampleModel_whenNameAndDescriptionAreProvided() {
            // arrange
            String name = "제목";
            String description = "설명";

            // act
            UserModel userModel = new UserModel(name, description);

            // assert
            assertAll(
                () -> assertThat(userModel.getId()).isNotNull(),
                () -> assertThat(userModel.getName()).isEqualTo(name),
                () -> assertThat(userModel.getDescription()).isEqualTo(description)
            );
        }

        @DisplayName("제목이 빈칸으로만 이루어져 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenTitleIsBlank() {
            // arrange
            String name = "   ";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel(name, "설명");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("설명이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenDescriptionIsEmpty() {
            // arrange
            String description = "";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new UserModel("제목", description);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}

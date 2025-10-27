package com.loopers.core.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    @Nested
    @DisplayName("create()")
    class CreateTest {

        @Nested
        @DisplayName("id값이 null이라면")
        class IdIsNull {

            @Test
            @DisplayName("IllegalArgumentException이 발생한다.")
            void shouldThrowIllegalArgumentException_whenIdIsNull() {

            }
        }
    }
}

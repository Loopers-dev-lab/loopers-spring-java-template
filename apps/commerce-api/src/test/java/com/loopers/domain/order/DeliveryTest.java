package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DeliveryTest {

    @DisplayName("배송 정보를 생성할 때,")
    @Nested
    class CreateDelivery {

        @DisplayName("모든 필드가 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsDelivery_whenAllFieldsAreValid() {
            // arrange
            String receiverName = "홍길동";
            String receiverPhoneNumber = "010-1234-5678";
            String baseAddress = "서울시 강남구";
            String detailAddress = "테헤란로 123";

            // act
            Delivery delivery = Delivery.createDelivery(receiverName, receiverPhoneNumber, baseAddress, detailAddress);

            // assert
            assertAll(
                    () -> assertThat(delivery.getReceiverName()).isEqualTo(receiverName),
                    () -> assertThat(delivery.getReceiverPhoneNumber()).isEqualTo(receiverPhoneNumber),
                    () -> assertThat(delivery.getBaseAddress()).isEqualTo(baseAddress),
                    () -> assertThat(delivery.getDetailAddress()).isEqualTo(detailAddress)
            );
        }

        @DisplayName("수령인 이름이 비어있으면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void throwsException_whenReceiverNameIsBlank(String invalidReceiverName) {
            // arrange
            String receiverPhoneNumber = "010-1234-5678";
            String baseAddress = "서울시 강남구";
            String detailAddress = "테헤란로 123";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Delivery.createDelivery(invalidReceiverName, receiverPhoneNumber, baseAddress, detailAddress);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("수령인 이름은 필수입니다.");
        }

        @DisplayName("수령인 핸드폰번호가 비어있으면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void throwsException_whenReceiverPhoneNumberIsBlank(String invalidPhoneNumber) {
            // arrange
            String receiverName = "홍길동";
            String baseAddress = "서울시 강남구";
            String detailAddress = "테헤란로 123";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Delivery.createDelivery(receiverName, invalidPhoneNumber, baseAddress, detailAddress);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("수령인 핸드폰번호는 필수입니다.");
        }

        @DisplayName("주소가 비어있으면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void throwsException_whenBaseAddressIsBlank(String invalidAddress) {
            // arrange
            String receiverName = "홍길동";
            String receiverPhoneNumber = "010-1234-5678";
            String detailAddress = "테헤란로 123";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Delivery.createDelivery(receiverName, receiverPhoneNumber, invalidAddress, detailAddress);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("주소는 필수입니다.");
        }

        @DisplayName("상세 주소가 비어있으면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void throwsException_whenDetailAddressIsBlank(String invalidDetailAddress) {
            // arrange
            String receiverName = "홍길동";
            String receiverPhoneNumber = "010-1234-5678";
            String baseAddress = "서울시 강남구";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Delivery.createDelivery(receiverName, receiverPhoneNumber, baseAddress, invalidDetailAddress);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("상세 주소는 필수입니다.");
        }

        @DisplayName("핸드폰 번호가 xxx-xxxx-xxxx 형식이 아니면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "01012345678",
                "010-1234-567",
                "010-123-5678",
                "0101234-5678",
                "010-12345678",
                "0101234-5678",
                "01-1234-5678",
                "010-12345-6789",
                "010-123-45678",
                "0101234567",
                "010-1234-56789",
                "phone-number"
        })
        void throwsException_whenPhoneNumberFormatIsInvalid(String invalidPhoneNumber) {
            // arrange
            String receiverName = "홍길동";
            String baseAddress = "서울시 강남구";
            String detailAddress = "테헤란로 123";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Delivery.createDelivery(receiverName, invalidPhoneNumber, baseAddress, detailAddress);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("핸드폰 번호는 xxx-xxxx-xxxx 형식이어야 합니다.");
        }
    }
}

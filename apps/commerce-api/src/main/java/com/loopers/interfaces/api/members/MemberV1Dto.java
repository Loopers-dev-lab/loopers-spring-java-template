package com.loopers.interfaces.api.members;

import com.loopers.application.members.MemberInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MemberV1Dto {

    @Schema(description = "회원 가입 요청")
    public record MemberRegisterRequest(
            @Schema(description = "사용자 ID", example = "test123")
            @NotBlank(message = "사용자 ID는 필수입니다.")
            @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{1,10}$", message = "사용자 ID는 영문 및 숫자 10자 이내여야 합니다.")
            String memberId,
            @Schema(description = "이름", example = "홍길동")
            @NotBlank(message = "이름은 필수입니다.")
            String name,
            @Schema(description = "이메일", example = "test@example.com")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,
            @Schema(description = "비밀번호", example = "password123")
            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,
            @Schema(description = "생년월일", example = "1990-01-01")
            @NotBlank(message = "생년월일은 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일 형식이 올바르지 않습니다.")
            String birthDate,
            @Schema(description = "성별", example = "MALE")
            @NotNull(message = "성별은 필수입니다.")
            @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 MALE 또는 FEMALE이어야 합니다.")
            String gender
    ) {}

    @Schema(description = "회원 정보 응답")
    public record MemberResponse(
            @Schema(description = "회원 ID")
            Long id,
            @Schema(description = "사용자 ID")
            String memberId,
            @Schema(description = "이름")
            String name,
            @Schema(description = "이메일")
            String email,
            @Schema(description = "생년월일")
            LocalDate birthDate,
            @Schema(description = "성별")
            String gender
    ) {
        /**
         * Create a MemberResponse by mapping fields from the given domain MemberInfo.
         *
         * @param memberInfo the source MemberInfo whose id, memberId, name, email, birthDate and gender are copied
         * @return a MemberResponse populated with values from {@code memberInfo}
         */
        public static MemberResponse from(MemberInfo memberInfo) {
            return new MemberResponse(
                    memberInfo.id(),
                    memberInfo.memberId(),
                    memberInfo.name(),
                    memberInfo.email(),
                    memberInfo.birthDate(),
                    memberInfo.gender()
            );
        }
    }

}
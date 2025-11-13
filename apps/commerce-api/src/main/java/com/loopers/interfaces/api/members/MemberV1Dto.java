package com.loopers.interfaces.api.members;

import com.loopers.application.members.MemberInfo;
import com.loopers.domain.members.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public class MemberV1Dto {

    @Schema(description = "회원 가입 요청")
    public record RegisterMemberRequest(
            @Schema(description = "회원 ID", example = "test123")
            @NotBlank(message = "회원 ID는 필수입니다.")
            @Pattern(regexp = "^[a-zA-Z0-9]{1,10}$", message = "ID는 영문 및 숫자 10자 이내여야 합니다.")
            String memberId,
            
            @Schema(description = "이메일", example = "test@example.com")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,
            
            @Schema(description = "비밀번호", example = "password123")
            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,
            
            @Schema(description = "생년월일", example = "1990-01-01")
            @NotBlank(message = "생년월일은 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다.")
            String birthDate,
            
            @Schema(description = "성별", example = "MALE")
            @NotNull(message = "성별은 필수입니다.")
            Gender gender
    ) {}

    @Schema(description = "회원 정보 응답")
    public record MemberResponse(
            @Schema(description = "회원 ID")
            Long id,
            
            @Schema(description = "회원 ID")
            String memberId,
            
            @Schema(description = "이메일")
            String email,
            
            @Schema(description = "생년월일")
            LocalDate birthDate,
            
            @Schema(description = "성별")
            Gender gender
    ) {
        public static MemberResponse from(MemberInfo info) {
            return new MemberResponse(
                    info.id(),
                    info.memberId(),
                    info.email(),
                    info.birthDate(),
                    info.gender()
            );
        }
    }
}
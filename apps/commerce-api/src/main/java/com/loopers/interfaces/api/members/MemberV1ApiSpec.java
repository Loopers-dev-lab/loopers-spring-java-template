package com.loopers.interfaces.api.members;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member", description = "회원 관리 API")
public interface MemberV1ApiSpec {

    /**
     * Register a new member.
     *
     * @param request the registration details for the new member
     * @return an ApiResponse containing the created member's MemberResponse
     */
    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    ApiResponse<MemberV1Dto.MemberResponse> registerMember(MemberV1Dto.MemberRegisterRequest request);

    /**
     * Retrieve information for the currently authenticated member.
     *
     * @param userId the user ID of the member to retrieve
     * @return an ApiResponse containing the member's response data
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 정보를 조회합니다.")
    ApiResponse<MemberV1Dto.MemberResponse> getMemberInfo(
            @Parameter(description = "사용자 ID", required = true) String userId
    );

}
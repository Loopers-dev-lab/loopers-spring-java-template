package com.loopers.interfaces.api.members;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member V1", description = "회원 관리 API")
public interface MemberV1ApiSpec {

    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    ApiResponse<MemberV1Dto.MemberResponse> registerMember(MemberV1Dto.RegisterMemberRequest request);

    @Operation(summary = "회원 조회", description = "회원 ID로 정보를 조회합니다.")
    ApiResponse<MemberV1Dto.MemberResponse> getMemberByMemberId(String memberId);

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회합니다.")
    ApiResponse<MemberV1Dto.MemberResponse> getMyInfo(String userId);
}
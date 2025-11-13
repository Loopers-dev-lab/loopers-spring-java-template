package com.loopers.interfaces.api.members;

import com.loopers.application.members.MemberFacade;
import com.loopers.application.members.MemberInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class MemberV1Controller implements MemberV1ApiSpec {

    private final MemberFacade memberFacade;

    @PostMapping("/members")
    @Override
    public ApiResponse<MemberV1Dto.MemberResponse> registerMember(
            @Valid @RequestBody MemberV1Dto.RegisterMemberRequest request
    ) {
        MemberInfo memberInfo = memberFacade.registerMember(
                request.memberId(),
                request.email(),
                request.password(),
                request.birthDate(),
                request.gender()
        );
        
        MemberV1Dto.MemberResponse response = MemberV1Dto.MemberResponse.from(memberInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/members/{memberId}")
    @Override
    public ApiResponse<MemberV1Dto.MemberResponse> getMemberByMemberId(
            @PathVariable String memberId
    ) {
        MemberInfo memberInfo = memberFacade.getMemberByMemberId(memberId);
        MemberV1Dto.MemberResponse response = MemberV1Dto.MemberResponse.from(memberInfo);
        return ApiResponse.success(response);
    }
}
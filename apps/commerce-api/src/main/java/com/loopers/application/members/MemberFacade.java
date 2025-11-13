package com.loopers.application.members;

import com.loopers.domain.members.Gender;
import com.loopers.domain.members.MemberModel;
import com.loopers.domain.members.MemberService;
import com.loopers.domain.points.PointService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class MemberFacade {
    private final MemberService memberService;
    private final PointService pointService;

    @Transactional
    public MemberInfo registerMember(String memberId, String email, String password, String birthDate, Gender gender) {
        MemberModel member = memberService.registerMember(memberId, email, password, birthDate, gender);
        pointService.initializeMemberPoints(memberId);
        return MemberInfo.from(member);
    }

    public MemberInfo getMemberByMemberId(String memberId) {
        MemberModel member = memberService.getMemberByMemberId(memberId);
        if (member == null) {
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "존재하지 않는 회원입니다."
            );
        }
        return MemberInfo.from(member);
    }
}

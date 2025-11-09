package com.loopers.application.members;

import com.loopers.domain.members.MemberModel;
import com.loopers.domain.members.MemberService;
import com.loopers.domain.points.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class MemberFacade {
    private final MemberService memberService;
    private final PointService pointService;

    /**
     * Registers a new member and initializes their point balance.
     *
     * Creates and persists a member with the provided identity and contact details, initializes the member's points, and returns a MemberInfo view of the created member.
     *
     * @param memberId unique identifier for the member
     * @param name member's full name
     * @param email member's email address
     * @param password account password
     * @param birthDate member's birth date as an ISO-8601 date string (e.g., "YYYY-MM-DD")
     * @param gender member's gender identifier
     * @return the created member represented as a MemberInfo
     */
    public MemberInfo registerMember(String memberId, String name, String email, String password, String birthDate, String gender) {
        MemberModel member = memberService.registerMember(memberId, name, email, password, birthDate, gender);
        pointService.initializeMemberPoints(memberId);
        return MemberInfo.from(member);
    }

    /**
     * Retrieve public information for a member by their identifier.
     *
     * @param memberId the unique identifier of the member to look up
     * @return a {@code MemberInfo} built from the member data, or {@code null} if no member exists with the given identifier
     */
    public MemberInfo getMemberInfo(String memberId) {
        MemberModel member = memberService.getMember(memberId);
        return member != null ? MemberInfo.from(member) : null;
    }

    /**
     * Retrieve the current point balance for a member.
     *
     * @param memberId the unique identifier of the member
     * @return the member's point balance as a BigDecimal
     */
    public BigDecimal getMemberPoints(String memberId) {
        return pointService.getMemberPoints(memberId);
    }
}
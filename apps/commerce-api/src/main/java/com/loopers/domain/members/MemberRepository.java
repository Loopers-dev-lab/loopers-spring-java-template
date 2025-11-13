package com.loopers.domain.members;

public interface MemberRepository {
    MemberModel save(MemberModel member);
    MemberModel findByMemberId(String memberId);
    boolean existsByMemberId(String memberId);
}

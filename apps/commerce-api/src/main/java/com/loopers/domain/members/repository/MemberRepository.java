package com.loopers.domain.members.repository;

import com.loopers.domain.members.Member;

public interface MemberRepository {
    Member save(Member member);
    Member findByMemberId(String memberId);
    boolean existsByMemberId(String memberId);
}

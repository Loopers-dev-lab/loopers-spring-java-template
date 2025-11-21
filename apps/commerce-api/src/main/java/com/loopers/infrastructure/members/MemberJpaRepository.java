package com.loopers.infrastructure.members;

import com.loopers.domain.members.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    Member findByMemberId(String memberId);
    boolean existsByMemberId(String memberId);
}
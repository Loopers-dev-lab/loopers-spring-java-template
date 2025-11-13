package com.loopers.infrastructure.members;

import com.loopers.domain.members.MemberModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberJpaRepository extends JpaRepository<MemberModel, Long> {
    MemberModel findByMemberId(String memberId);
    boolean existsByMemberId(String memberId);
}
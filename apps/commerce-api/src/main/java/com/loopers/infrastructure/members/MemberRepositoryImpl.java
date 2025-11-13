package com.loopers.infrastructure.members;

import com.loopers.domain.members.MemberModel;
import com.loopers.domain.members.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class MemberRepositoryImpl implements MemberRepository {
    private final MemberJpaRepository memberJpaRepository;

    @Override
    public MemberModel save(MemberModel member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public MemberModel findByMemberId(String memberId) {
        return memberJpaRepository.findByMemberId(memberId);
    }

    @Override
    public boolean existsByMemberId(String memberId) {
        return memberJpaRepository.existsByMemberId(memberId);
    }
}

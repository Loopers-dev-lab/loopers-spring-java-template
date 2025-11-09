package com.loopers.infrastructure.members;

import com.loopers.domain.members.MemberModel;
import com.loopers.domain.members.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class MemberRepositoryImpl implements MemberRepository {
    private final MemberJpaRepository memberJpaRepository;

    /**
     * Retrieve a member by its memberId.
     *
     * @param memberId the unique identifier of the member to look up
     * @return an Optional containing the MemberModel when a member with the given id exists, or Optional.empty() otherwise
     */
    @Override
    public Optional<MemberModel> findByMemberId(String memberId) {
        return memberJpaRepository.findByMemberId(memberId);
    }

    /**
     * Persist the given member and return the persisted entity.
     *
     * @param memberModel the member to save; may be updated with persistence-generated values (for example, generated id or timestamps)
     * @return the persisted {@code MemberModel}, potentially containing persistence-generated values
     */
    @Override
    public MemberModel save(MemberModel memberModel) {
        return memberJpaRepository.save(memberModel);
    }

    /**
     * Checks whether a member with the given memberId exists.
     *
     * @param memberId the member's unique identifier
     * @return `true` if a member with the given memberId exists, `false` otherwise
     */
    @Override
    public boolean existsByMemberId(String memberId) {
        return memberJpaRepository.existsByMemberId(memberId);
    }
}
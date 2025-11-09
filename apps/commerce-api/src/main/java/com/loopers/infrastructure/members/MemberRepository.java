package com.loopers.infrastructure.members;

import com.loopers.domain.members.MemberModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberModel, Long> {
    /**
 * Finds a member by its memberId.
 *
 * @param memberId the member's unique identifier
 * @return an Optional containing the MemberModel with the given memberId, or empty if none exists
 */
Optional<MemberModel> findByMemberId(String memberId);

    /**
 * Determine whether a member with the given memberId exists.
 *
 * @param memberId the unique identifier of the member to check
 * @return `true` if a member with the given memberId exists, `false` otherwise
 */
boolean existsByMemberId(String memberId);
}
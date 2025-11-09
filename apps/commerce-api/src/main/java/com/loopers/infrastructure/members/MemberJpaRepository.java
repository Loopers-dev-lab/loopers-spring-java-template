package com.loopers.infrastructure.members;

import com.loopers.domain.members.MemberModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberModel, Long> {
    /**
 * Locate a member by its unique member identifier.
 *
 * @param memberId the unique identifier of the member to find
 * @return an {@link Optional} containing the matching {@link MemberModel} if present, or an empty {@link Optional} otherwise
 */
Optional<MemberModel> findByMemberId(String memberId);
    /**
 * Checks whether a member with the given memberId exists.
 *
 * @param memberId the member's unique identifier
 * @return true if a member with the given memberId exists, false otherwise
 */
boolean existsByMemberId(String memberId);
}
package com.loopers.domain.members;

import java.util.Optional;

public interface MemberRepository {
    /**
 * Retrieve a member by its memberId.
 *
 * @param memberId the unique identifier of the member to look up
 * @return an Optional containing the MemberModel if a member with the given memberId exists, Optional.empty() otherwise
 */
Optional<MemberModel> findByMemberId(String memberId);

    /**
 * Persists the given member and returns the persisted instance.
 *
 * @param memberModel the member to persist
 * @return the saved MemberModel, potentially with updated state such as generated identifiers
 */
MemberModel save(MemberModel memberModel);

    /**
 * Determines whether a member with the given memberId exists.
 *
 * @param memberId the unique identifier of the member to check
 * @return true if a member with the specified memberId exists, false otherwise
 */
boolean existsByMemberId(String memberId);
}
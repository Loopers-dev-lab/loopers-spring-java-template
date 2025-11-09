package com.loopers.domain.members;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new member with the provided details and persist it.
     *
     * @param memberId  the unique identifier for the member
     * @param name      the member's full name
     * @param email     the member's email address
     * @param password  the member's plaintext password (will be encoded before storage)
     * @param birthDate the member's birth date as a string
     * @param gender    the member's gender
     * @return the persisted {@code MemberModel} for the newly created member
     * @throws CoreException if a member with the given {@code memberId} already exists
     */
    @Transactional
    public MemberModel registerMember(String memberId, String name, String email, String password, String birthDate, String gender) {
        if (memberRepository.existsByMemberId(memberId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 사용자 ID입니다.");
        }
        
        String encodedPassword = passwordEncoder.encode(password);
        MemberModel member = MemberModel.create(memberId, name, email, encodedPassword, birthDate, gender);
        return memberRepository.save(member);
    }

    /**
     * Retrieves a member by their memberId.
     *
     * @param memberId the unique identifier of the member to retrieve
     * @return the MemberModel matching the given memberId, or null if no member is found
     */
    @Transactional(readOnly = true)
    public MemberModel getMember(String memberId) {
        return memberRepository.findByMemberId(memberId).orElse(null);
    }
}
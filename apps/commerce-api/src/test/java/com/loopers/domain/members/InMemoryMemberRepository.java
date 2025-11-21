package com.loopers.domain.members;

import com.loopers.domain.members.repository.MemberRepository;

import java.util.HashMap;
import java.util.Map;

public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> store = new HashMap<>();

    @Override
    public Member save(Member member) {
        store.put(member.getMemberId(), member);
        return member;
    }

    @Override
    public Member findByMemberId(String memberId) {
        return store.get(memberId);
    }

    @Override
    public boolean existsByMemberId(String memberId) {
        return store.containsKey(memberId);
    }

    public void clear() {
        store.clear();
    }
}

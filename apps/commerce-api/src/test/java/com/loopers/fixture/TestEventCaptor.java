package com.loopers.fixture;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 테스트에서 발행된 이벤트를 캡처하는 유틸리티 클래스.
 *
 * 사용법:
 * 1. @Import(TestEventCaptor.class)로 테스트에 등록
 * 2. @Autowired로 주입받아 사용
 * 3. 테스트 전 clear() 호출
 * 4. 테스트 후 getEvents() 또는 getEventsOfType()으로 검증
 */
@Component
public class TestEventCaptor {

    private final List<Object> capturedEvents = new ArrayList<>();

    @EventListener
    public void capture(Object event) {
        capturedEvents.add(event);
    }

    public List<Object> getEvents() {
        return new ArrayList<>(capturedEvents);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getEventsOfType(Class<T> eventType) {
        return capturedEvents.stream()
                .filter(eventType::isInstance)
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }

    public <T> T getFirstEventOfType(Class<T> eventType) {
        return getEventsOfType(eventType).stream()
                .findFirst()
                .orElse(null);
    }

    public <T> T getLastEventOfType(Class<T> eventType) {
        List<T> events = getEventsOfType(eventType);
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }

    public boolean hasEventOfType(Class<?> eventType) {
        return capturedEvents.stream().anyMatch(eventType::isInstance);
    }

    public int countEventsOfType(Class<?> eventType) {
        return (int) capturedEvents.stream().filter(eventType::isInstance).count();
    }

    public void clear() {
        capturedEvents.clear();
    }

    public int size() {
        return capturedEvents.size();
    }
}

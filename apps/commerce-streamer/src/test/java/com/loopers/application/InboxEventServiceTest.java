package com.loopers.application;

import com.loopers.domain.event.InboxEvent;
import com.loopers.domain.event.InboxEventRepository;
import com.loopers.domain.event.InboxEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional; // Add this import

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboxEventServiceTest {

    @Mock
    private InboxEventRepository inboxEventRepository;

    @InjectMocks
    private InboxEventService inboxEventService;

    @Test
    @DisplayName("이미 처리된 메시지면 액션을 실행하지 않는다")
    void process_alreadyHandled() {
        // given
        String messageId = "msg-1";
        LocalDateTime currentEventTimestamp = LocalDateTime.now();
        InboxEvent existingEvent = InboxEvent.builder().messageId(messageId).eventTimestamp(currentEventTimestamp.plusSeconds(1)).build();
        when(inboxEventRepository.findByMessageId(messageId)).thenReturn(Optional.of(existingEvent));
        Runnable action = mock(Runnable.class);

        // when
        inboxEventService.process(messageId, currentEventTimestamp, action);

        // then
        verify(action, never()).run();
        verify(inboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("처음 처리하는 메시지면 액션을 실행하고 저장한다")
    void process_newMessage() {
        // given
        String messageId = "msg-2";
        LocalDateTime currentEventTimestamp = LocalDateTime.now();
        when(inboxEventRepository.findByMessageId(messageId)).thenReturn(Optional.empty());
        Runnable action = mock(Runnable.class);

        // when
        inboxEventService.process(messageId, currentEventTimestamp, action);

        // then
        verify(action).run();
        verify(inboxEventRepository).save(any(InboxEvent.class));
    }
    
    @Test
    @DisplayName("동시성 문제로 저장 실패 시 예외를 무시하고 정상 종료한다")
    void process_raceCondition() {
        // given
        String messageId = "msg-3";
        LocalDateTime currentEventTimestamp = LocalDateTime.now();
        when(inboxEventRepository.findByMessageId(messageId)).thenReturn(Optional.empty());
        Runnable action = mock(Runnable.class);
        when(inboxEventRepository.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));

        // when
        inboxEventService.process(messageId, currentEventTimestamp, action);

        // then
        verify(action).run(); 
        verify(inboxEventRepository).save(any(InboxEvent.class));
    }
}

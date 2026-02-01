package com.loopers.batch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChunkListener {

    @AfterChunk
    void afterChunk(ChunkContext chunkContext) {
        log.info(
            "청크 종료: readCount: ${chunkContext.stepContext.stepExecution.readCount}, " +
                    "writeCount: ${chunkContext.stepContext.stepExecution.writeCount}"
        );
    }
}

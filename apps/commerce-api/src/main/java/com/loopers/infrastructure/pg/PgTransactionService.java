package com.loopers.infrastructure.pg;

import com.loopers.infrastructure.pg.dto.PgTransactionDetail;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PgTransactionService {

    private final PgClient pgClient;

    @Retry(name = "pgRetry")
    public PgTransactionDetail getTransaction(String userId, String pgTransactionId) {
        return pgClient.getTransaction(userId, pgTransactionId);
    }
}

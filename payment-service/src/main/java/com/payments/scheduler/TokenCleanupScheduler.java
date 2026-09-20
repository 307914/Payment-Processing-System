package com.payments.scheduler;

import com.payments.repository.OutboxEventRepository;
import com.payments.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Cleaned up {} expired refresh tokens", deleted);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupProcessedOutboxEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = outboxEventRepository.deleteProcessedBefore(cutoff);
        log.info("Cleaned up {} processed outbox events older than 7 days", deleted);
    }
}

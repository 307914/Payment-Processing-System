package com.payments.scheduler;

import com.payments.entity.OutboxEvent;
import com.payments.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            try {
                String topic = resolveTopic(event.getEventType());

                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event {} to Kafka: {}",
                                        event.getId(), ex.getMessage());
                            }
                        });

                outboxEventRepository.markAsProcessed(event.getId(), Instant.now());

                log.info("Published outbox event: id={}, type={}, topic={}",
                        event.getId(), event.getEventType(), topic);

            } catch (Exception e) {
                log.error("Error processing outbox event {}: {}", event.getId(), e.getMessage());
                break;
            }
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "TRANSACTION_COMPLETED" -> "transaction-events";
            case "TRANSACTION_REVERSED" -> "transaction-events";
            default -> "payment-events";
        };
    }
}

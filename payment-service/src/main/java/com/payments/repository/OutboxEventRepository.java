package com.payments.repository;

import com.payments.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.processed = true, o.processedAt = :now WHERE o.id = :id")
    int markAsProcessed(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.processed = true AND o.processedAt < :before")
    int deleteProcessedBefore(@Param("before") Instant before);
}

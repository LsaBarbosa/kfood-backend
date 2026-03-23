package com.kfood.eventing.infra.persistence;

import com.kfood.eventing.domain.OutboxEventStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  Page<OutboxEvent> findByPublicationStatusOrderByCreatedAtAsc(
      OutboxEventStatus publicationStatus, Pageable pageable);

  Optional<OutboxEvent> findByDedupKey(String dedupKey);
}

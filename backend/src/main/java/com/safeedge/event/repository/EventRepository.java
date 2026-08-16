package com.safeedge.event.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

	Optional<EventEntity> findByProviderAndExternalEventId(String provider, String externalEventId);

}

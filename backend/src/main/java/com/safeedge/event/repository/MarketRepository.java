package com.safeedge.event.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<MarketEntity, Long> {

	Optional<MarketEntity> findByProviderAndExternalMarketId(String provider, String externalMarketId);

	List<MarketEntity> findByEvent_Id(Long eventId);

}

package com.safeedge.event.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelectionRepository extends JpaRepository<SelectionEntity, Long> {

	Optional<SelectionEntity> findByMarket_IdAndExternalOutcomeNo(Long marketId, Integer externalOutcomeNo);

	List<SelectionEntity> findByMarket_Id(Long marketId);

}

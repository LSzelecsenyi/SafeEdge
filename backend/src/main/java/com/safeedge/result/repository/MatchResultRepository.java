package com.safeedge.result.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchResultRepository extends JpaRepository<MatchResultEntity, Long> {

	Optional<MatchResultEntity> findByEvent_Id(Long eventId);

}

package com.safeedge.odds.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OddsSnapshotRepository extends JpaRepository<OddsSnapshotEntity, Long> {

	List<OddsSnapshotEntity> findBySelection_IdOrderByCapturedAtAscIdAsc(Long selectionId);

	long countBySelection_Id(Long selectionId);

}

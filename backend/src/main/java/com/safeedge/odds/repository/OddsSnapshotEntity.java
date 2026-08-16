package com.safeedge.odds.repository;

import com.safeedge.event.repository.SelectionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "odds_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class OddsSnapshotEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "selection_id", nullable = false)
	private SelectionEntity selection;

	@Column(nullable = false, precision = 12, scale = 4)
	private BigDecimal odds;

	@Column(name = "captured_at", nullable = false)
	private Instant capturedAt;

	@Column(name = "provider_market_version")
	private Integer providerMarketVersion;

}

package com.safeedge.event.repository;

import com.safeedge.event.domain.SelectionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "betting_selection")
@Getter
@Setter
@NoArgsConstructor
public class SelectionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "market_id", nullable = false)
	private MarketEntity market;

	@Column(nullable = false, length = 32)
	private String provider;

	@Column(name = "external_outcome_no", nullable = false)
	private Integer externalOutcomeNo;

	@Column(name = "external_outcome_real_no")
	private Integer externalOutcomeRealNo;

	@Column(name = "provider_outcome_name", nullable = false, length = 512)
	private String providerOutcomeName;

	@Enumerated(EnumType.STRING)
	@Column(name = "selection_type", nullable = false, length = 32)
	private SelectionType selectionType;

	@Column(precision = 12, scale = 4)
	private BigDecimal line;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}

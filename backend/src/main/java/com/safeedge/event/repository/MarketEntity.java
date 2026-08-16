package com.safeedge.event.repository;

import com.safeedge.event.domain.MarketType;
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
@Table(name = "betting_market")
@Getter
@Setter
@NoArgsConstructor
public class MarketEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private EventEntity event;

	@Column(nullable = false, length = 32)
	private String provider;

	@Column(name = "external_market_id", nullable = false, length = 64)
	private String externalMarketId;

	@Column(name = "provider_market_real_no")
	private Integer providerMarketRealNo;

	@Column(name = "provider_market_name", nullable = false, length = 512)
	private String providerMarketName;

	@Column(name = "provider_market_type")
	private Integer providerMarketType;

	@Column(name = "provider_market_sub_type")
	private Integer providerMarketSubType;

	@Column(name = "provider_market_version")
	private Integer providerMarketVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "market_type", nullable = false, length = 32)
	private MarketType marketType;

	@Column(precision = 12, scale = 4)
	private BigDecimal line;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}

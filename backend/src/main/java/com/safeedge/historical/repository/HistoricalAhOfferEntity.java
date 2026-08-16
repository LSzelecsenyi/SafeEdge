package com.safeedge.historical.repository;

import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
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
@Table(name = "historical_ah_offer")
@Getter
@Setter
@NoArgsConstructor
public class HistoricalAhOfferEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "historical_match_id", nullable = false)
	private HistoricalMatchEntity historicalMatch;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private HistoricalSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "quote_source", nullable = false, length = 32)
	private HistoricalQuoteSource quoteSource;

	@Column(name = "home_handicap_line", nullable = false, precision = 12, scale = 4)
	private BigDecimal homeHandicapLine;

	@Column(name = "home_odds", nullable = false, precision = 12, scale = 4)
	private BigDecimal homeOdds;

	@Column(name = "away_odds", nullable = false, precision = 12, scale = 4)
	private BigDecimal awayOdds;

	@Enumerated(EnumType.STRING)
	@Column(name = "observation_type", nullable = false, length = 32)
	private HistoricalObservationType observationType;

	@Column(name = "observed_at")
	private Instant observedAt;

	@Column(name = "source_line_column", nullable = false, length = 32)
	private String sourceLineColumn;

	@Column(name = "source_home_odds_column", nullable = false, length = 32)
	private String sourceHomeOddsColumn;

	@Column(name = "source_away_odds_column", nullable = false, length = 32)
	private String sourceAwayOddsColumn;

	@Column(name = "raw_line_value", length = 64)
	private String rawLineValue;

	@Column(name = "raw_home_odds_value", length = 64)
	private String rawHomeOddsValue;

	@Column(name = "raw_away_odds_value", length = 64)
	private String rawAwayOddsValue;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}

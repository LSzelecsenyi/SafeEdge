package com.safeedge.historical.repository;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "historical_match")
@Getter
@Setter
@NoArgsConstructor
public class HistoricalMatchEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private HistoricalSource source;

	@Column(name = "source_competition_code", nullable = false, length = 16)
	private String sourceCompetitionCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "canonical_competition", nullable = false, length = 32)
	private CanonicalCompetition canonicalCompetition;

	@Column(name = "season_start_year", nullable = false)
	private Integer seasonStartYear;

	@Column(name = "season_end_year", nullable = false)
	private Integer seasonEndYear;

	@Column(name = "match_date", nullable = false)
	private LocalDate matchDate;

	@Column(name = "source_kickoff_time", length = 16)
	private String sourceKickoffTime;

	@Column(name = "kickoff_utc")
	private Instant kickoffUtc;

	@Column(name = "source_home_team_name", nullable = false, length = 512)
	private String sourceHomeTeamName;

	@Column(name = "source_away_team_name", nullable = false, length = 512)
	private String sourceAwayTeamName;

	@Column(name = "home_goals", nullable = false)
	private Integer homeGoals;

	@Column(name = "away_goals", nullable = false)
	private Integer awayGoals;

	@Column(name = "source_file", nullable = false, length = 256)
	private String sourceFile;

	@Column(name = "source_row_number", nullable = false)
	private Integer sourceRowNumber;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}

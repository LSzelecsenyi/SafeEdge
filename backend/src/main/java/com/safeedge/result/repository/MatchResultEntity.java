package com.safeedge.result.repository;

import com.safeedge.event.repository.EventEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_result")
@Getter
@Setter
@NoArgsConstructor
public class MatchResultEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private EventEntity event;

	@Column(nullable = false, length = 32)
	private String provider;

	@Column(name = "external_event_id", nullable = false, length = 64)
	private String externalEventId;

	@Column(name = "betradar_id")
	private Long betradarId;

	@Column(name = "home_goals", nullable = false)
	private Integer homeGoals;

	@Column(name = "away_goals", nullable = false)
	private Integer awayGoals;

	@Column(name = "result_observed_at", nullable = false)
	private Instant resultObservedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}

package com.safeedge.event.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "betting_event")
@Getter
@Setter
@NoArgsConstructor
public class EventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 32)
	private String provider;

	@Column(name = "external_event_id", nullable = false, length = 64)
	private String externalEventId;

	@Column(name = "betradar_id")
	private Long betradarId;

	@Column(nullable = false, length = 512)
	private String name;

	@Column(name = "start_time", nullable = false)
	private Instant startTime;

	@Column(name = "competition_external_id", nullable = false, length = 64)
	private String competitionExternalId;

	@Column(name = "competition_name", nullable = false, length = 512)
	private String competitionName;

	@Column(name = "home_participant_external_id", nullable = false, length = 64)
	private String homeParticipantExternalId;

	@Column(name = "home_participant_name", nullable = false, length = 512)
	private String homeParticipantName;

	@Column(name = "away_participant_external_id", nullable = false, length = 64)
	private String awayParticipantExternalId;

	@Column(name = "away_participant_name", nullable = false, length = 512)
	private String awayParticipantName;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}

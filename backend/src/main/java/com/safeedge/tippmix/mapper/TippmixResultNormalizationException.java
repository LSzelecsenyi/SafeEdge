package com.safeedge.tippmix.mapper;

public class TippmixResultNormalizationException extends RuntimeException {

	private final Long tippmixEventId;

	public TippmixResultNormalizationException(Long tippmixEventId, String message) {
		super(message);
		this.tippmixEventId = tippmixEventId;
	}

	public Long getTippmixEventId() {
		return tippmixEventId;
	}

}

package com.safeedge.tippmix.manager;

public class TippmixIngestionException extends RuntimeException {

	private final Long tippmixEventId;

	public TippmixIngestionException(Long tippmixEventId, String message) {
		super(message);
		this.tippmixEventId = tippmixEventId;
	}

	public Long getTippmixEventId() {
		return tippmixEventId;
	}

}

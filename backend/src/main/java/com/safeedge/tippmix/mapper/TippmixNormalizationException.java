package com.safeedge.tippmix.mapper;

public class TippmixNormalizationException extends RuntimeException {

	private final Long tippmixEventId;
	private final Long tippmixMarketId;

	public TippmixNormalizationException(Long tippmixEventId, Long tippmixMarketId, String message) {
		super(message);
		this.tippmixEventId = tippmixEventId;
		this.tippmixMarketId = tippmixMarketId;
	}

	public Long getTippmixEventId() {
		return tippmixEventId;
	}

	public Long getTippmixMarketId() {
		return tippmixMarketId;
	}

}

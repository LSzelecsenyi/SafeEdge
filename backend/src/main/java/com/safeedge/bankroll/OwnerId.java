package com.safeedge.bankroll;

import java.util.UUID;

public record OwnerId(UUID value) {

	public OwnerId {
		if (value == null) {
			throw new BankrollException("Owner id is required");
		}
	}

}

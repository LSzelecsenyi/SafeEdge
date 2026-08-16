package com.safeedge.bankroll;

import java.math.BigDecimal;
import java.time.Instant;

public record BankrollTransaction(
		OwnerId ownerId,
		BankrollTransactionType type,
		BigDecimal activeBankrollDelta,
		BigDecimal vaultDelta,
		BigDecimal realizedPnlDelta,
		Instant occurredAt,
		String referenceId) {

	public BankrollTransaction {
		if (ownerId == null) {
			throw new BankrollException("Owner id is required");
		}
		if (type == null) {
			throw new BankrollException("Transaction type is required");
		}
		if (activeBankrollDelta == null) {
			throw new BankrollException("activeBankrollDelta is required");
		}
		if (vaultDelta == null) {
			throw new BankrollException("vaultDelta is required");
		}
		if (realizedPnlDelta == null) {
			throw new BankrollException("realizedPnlDelta is required");
		}
		if (occurredAt == null) {
			throw new BankrollException("occurredAt is required");
		}
		if (referenceId == null) {
			throw new BankrollException("referenceId is required");
		}
		activeBankrollDelta = activeBankrollDelta.stripTrailingZeros();
		vaultDelta = vaultDelta.stripTrailingZeros();
		realizedPnlDelta = realizedPnlDelta.stripTrailingZeros();
	}

	public BigDecimal totalEquityDelta() {
		return activeBankrollDelta.add(vaultDelta);
	}

}

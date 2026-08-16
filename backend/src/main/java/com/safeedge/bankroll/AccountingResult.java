package com.safeedge.bankroll;

import java.util.List;

public record AccountingResult(BankrollState state, List<BankrollTransaction> transactions) {

	public AccountingResult {
		if (state == null) {
			throw new BankrollException("Bankroll state is required");
		}
		if (transactions == null || transactions.isEmpty()) {
			throw new BankrollException("Accounting transactions are required");
		}
		transactions = List.copyOf(transactions);
	}

}

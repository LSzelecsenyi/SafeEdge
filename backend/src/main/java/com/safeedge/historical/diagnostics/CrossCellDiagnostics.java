package com.safeedge.historical.diagnostics;

public record CrossCellDiagnostics(String rowKey, String columnKey, EdgeQualityGroupSummary summary) {

	public CrossCellDiagnostics {
		if (rowKey == null || rowKey.isBlank()) {
			throw new IllegalArgumentException("rowKey is required");
		}
		if (columnKey == null || columnKey.isBlank()) {
			throw new IllegalArgumentException("columnKey is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
	}
}

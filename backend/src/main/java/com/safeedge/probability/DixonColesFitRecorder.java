package com.safeedge.probability;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional diagnostics sink for v2 fit snapshots. Not used in production
 * candidate generation.
 */
public final class DixonColesFitRecorder {

	private final List<DixonColesFitSnapshot> snapshots = new ArrayList<>();

	public void record(DixonColesFitSnapshot snapshot) {
		if (snapshot == null) {
			throw new ProbabilityModelException("snapshot is required");
		}
		snapshots.add(snapshot);
	}

	public List<DixonColesFitSnapshot> snapshots() {
		return List.copyOf(snapshots);
	}
}

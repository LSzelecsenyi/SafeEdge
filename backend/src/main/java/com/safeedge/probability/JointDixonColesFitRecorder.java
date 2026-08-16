package com.safeedge.probability;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional diagnostics sink for v3 fit snapshots. Not used in production
 * candidate generation.
 */
public final class JointDixonColesFitRecorder {

	private final List<JointDixonColesFitSnapshot> snapshots = new ArrayList<>();
	private int fittingFailures;

	public void record(JointDixonColesFitSnapshot snapshot) {
		if (snapshot == null) {
			throw new ProbabilityModelException("snapshot is required");
		}
		snapshots.add(snapshot);
	}

	public void recordFailure() {
		fittingFailures++;
	}

	public List<JointDixonColesFitSnapshot> snapshots() {
		return List.copyOf(snapshots);
	}

	public int fittingFailures() {
		return fittingFailures;
	}
}

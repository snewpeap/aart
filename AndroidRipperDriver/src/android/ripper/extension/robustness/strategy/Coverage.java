package android.ripper.extension.robustness.strategy;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.coverage.AllCoverage;
import android.ripper.extension.robustness.strategy.coverage.LongIdleFirstToStateCoverage;

import java.util.Collection;
import java.util.Set;

public interface Coverage {
	Collection<Transition> cherryPick(Set<Transition> transitions);

	static Coverage of(String s) {
		switch (s) {
			case "lifts":
			case "longIdleFirstToState":
				return new LongIdleFirstToStateCoverage();
			case "all":
			default:
				return new AllCoverage();
		}
	}
}

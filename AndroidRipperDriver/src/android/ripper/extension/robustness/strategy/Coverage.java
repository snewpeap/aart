package android.ripper.extension.robustness.strategy;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.coverage.AllCoverage;

import java.util.Set;

public interface Coverage {
	Set<Transition> cherryPick(Set<Transition> transitions);

	static Coverage of(String s) {
		switch (s) {
			case "all":
			default:
				return new AllCoverage();
		}
	}
}

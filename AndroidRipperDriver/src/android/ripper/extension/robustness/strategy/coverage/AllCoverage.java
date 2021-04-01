package android.ripper.extension.robustness.strategy.coverage;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Coverage;

import java.util.Set;

public class AllCoverage implements Coverage {
	@Override
	public Set<Transition> cherryPick(Set<Transition> transitions) {
		return transitions;
	}
}

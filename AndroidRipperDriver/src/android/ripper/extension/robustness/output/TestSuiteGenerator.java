package android.ripper.extension.robustness.output;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Coverage;
import android.ripper.extension.robustness.strategy.Perturb;

import java.util.Set;

public class TestSuiteGenerator {
	public void generate(Set<Transition> transitions) {
		//TODO
		//for each transition, generate testcase
		for (Transition transition : coverage.cherryPick(transitions)) {
			//always start running from the root state
			//fire task's events, insert perturbation according to (TODO) strategy
			//check final state: tell the differences/similarity only
				//if unable to reach final state, i.e. stuck at some state
				//report, manually check for true/false positive later
		}
	}

	private final String AUT_PACKAGE;
	private final Coverage coverage;
	private final Perturb perturb;

	public TestSuiteGenerator(String AUT_PACKAGE, String coverage, String perturb) {
		this.AUT_PACKAGE = AUT_PACKAGE;
		this.coverage = Coverage.of(coverage);
		this.perturb = Perturb.of(perturb);
	}
}

package android.ripper.extension.robustness.output;

import android.ripper.extension.robustness.model.Transition;

import java.util.Set;

public abstract class TestSuiteGenerator {
	public abstract void generate(Set<Transition> transitions);
}

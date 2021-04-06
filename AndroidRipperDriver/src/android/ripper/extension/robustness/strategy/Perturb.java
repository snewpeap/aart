package android.ripper.extension.robustness.strategy;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.perturb.OperationFactory;
import android.ripper.extension.robustness.strategy.perturb.RandomPerturb;

public interface Perturb {

	String perturb(Transition transition, OperationFactory operationFactory);

	String recover(OperationFactory operationFactory);

	static Perturb of(String s){
		switch (s) {
			case "random":
			default:
				return new RandomPerturb();
		}
	}
}

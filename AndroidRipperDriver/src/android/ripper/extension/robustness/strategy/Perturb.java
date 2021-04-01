package android.ripper.extension.robustness.strategy;

import android.ripper.extension.robustness.strategy.perturb.RandomPerturb;

public interface Perturb {

	static Perturb of(String s){
		switch (s) {
			case "random":
			default:
				return new RandomPerturb();
		}
	}
}

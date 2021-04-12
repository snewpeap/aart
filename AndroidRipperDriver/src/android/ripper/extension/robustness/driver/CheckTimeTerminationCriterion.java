package android.ripper.extension.robustness.driver;

import it.unina.android.ripper.driver.AbstractDriver;
import it.unina.android.ripper.termination.TerminationCriterion;

public class CheckTimeTerminationCriterion implements TerminationCriterion {
	public static final int CHECK_TIME = 50;
	private int countdown = CHECK_TIME;

	@Override
	public void init(AbstractDriver driver) {
		//do nothing
	}

	@Override
	public boolean check() {
		boolean term = --countdown > 0;
		if (term) {
			System.out.printf("Checked %d times. TERMINATE.%n", CHECK_TIME);
		}
		return term;
	}
}

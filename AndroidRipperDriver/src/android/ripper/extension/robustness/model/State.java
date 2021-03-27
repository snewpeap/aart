package android.ripper.extension.robustness.model;

import it.unina.android.shared.ripper.model.state.ActivityDescription;
import org.apache.commons.lang3.StringUtils;

public class State extends ActivityDescription implements Comparable<State> {
	public static final String DUMMY_UID = "0";

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			return compareTo((State) obj) == 0;
		} else
			return false;
	}

	/**
	 * Compare between State, but they needn't to be identical to return 0.
	 * What's compared here:
	 * TODO 设定比较标准
	 *
	 * @param o another State
	 * @return 0 if they are considered to be same State.
	 */
	@Override
	public int compareTo(State o) {
		if (StringUtils.isEmpty(getUid()) || StringUtils.isEmpty(o.getUid())) {
			return 0;
		} else return getUid().compareTo(o.getUid());
	}

	@Override
	public String toString() {
		return super.toString();//TODO
	}

	public static class Comparator implements java.util.Comparator<State> {
		@Override
		public int compare(State o1, State o2) {
			return o1.compareTo(o2);
		}
	}

}

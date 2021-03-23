package android.ripper.extension.robustness.comparator.model;

import it.unina.android.shared.ripper.model.state.ActivityDescription;

import java.util.Objects;

public class State extends ActivityDescription {
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			return Objects.equals(getUid(), ((State) obj).getUid());
		} else
			return false;
	}
}

package android.ripper.extension.robustness.model;

import android.ripper.extension.robustness.tools.ObjectTool.Comparand;
import it.unina.android.shared.ripper.model.state.ActivityDescription;

import java.util.ArrayList;
import java.util.function.Supplier;

import static android.ripper.extension.robustness.tools.ObjectTool.propEquals;

public class State extends ActivityDescription {
	public static final String DUMMY_UID = "0";

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			State s = (State) obj;
			if (getUid() == null || s.getUid() == null) {
				boolean widgetsEquality = //TODO may have to be shallower
						(propEquals(getWidgets(), s.getWidgets(), ArrayList::size, false) &&
								getWidgets().containsAll(s.getWidgets())) || (getWidgets() == s.getWidgets());
				Supplier<Comparand<State>> su = () -> Comparand.of(this, s);
				return widgetsEquality && propEquals(su, State::getTitle) &&
						propEquals(su, State::getName) &&
						propEquals(su, State::getActivityClass) &&
						propEquals(su, State::hasMenu) &&
						propEquals(su, State::handlesKeyPress) &&
						propEquals(su, State::handlesLongKeyPress) &&
						(isTabActivity() ?
								s.isTabActivity() &&
								propEquals(su, State::getTabsCount) &&
								propEquals(su, State::getCurrentTab) :
								!s.isTabActivity()
						);
			} else
				return getUid().equals(s.getUid());
		} else
			return false;
	}

	@Override
	public String toString() {
		return super.toString();//TODO LOW
	}

}

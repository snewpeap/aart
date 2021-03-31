package android.ripper.extension.robustness.model;

import android.ripper.extension.robustness.tools.ObjectTool.Comparand;
import it.unina.android.shared.ripper.model.state.ActivityDescription;
import it.unina.android.shared.ripper.model.state.WidgetDescription;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import static android.ripper.extension.robustness.tools.ObjectTool.propEquals;

public class State extends ActivityDescription {
	public static final String LOWEST_UID = "0";
	private final ActivityDescription ad;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			State s = (State) obj;
			if (StringUtils.isEmpty(getUid()) || StringUtils.isEmpty(s.getUid())) {
				boolean widgetsEquality = //TODO may have to be shallower
						(propEquals(getWidgets(), s.getWidgets(), ArrayList::size, false) &&
								getWidgets().containsAll(s.getWidgets())) || (getWidgets() == s.getWidgets());
				Supplier<Comparand<State>> su = () -> Comparand.of(this, s);
				return widgetsEquality && propEquals(su, State::getTitle) &&
						propEquals(su, State::getName) &&
						propEquals(su, State::getClassName) &&
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

	public State(ActivityDescription activityDescription) {
		ad = activityDescription;
	}

	private static final State EXIT_STATE = new State(new ActivityDescription());
	static {
		EXIT_STATE.setId("");
		EXIT_STATE.setName("");
		EXIT_STATE.setTitle("");
		EXIT_STATE.setClassName("");
		EXIT_STATE.setUid("-1");
	}
	public static State EXIT_STATE() {
		EXIT_STATE.setUid("-1");
		return EXIT_STATE;
	}

	@Override
	public void addWidget(WidgetDescription widget) {
		ad.addWidget(widget);
	}

	@Override
	public String getId() {
		return ad.getId();
	}

	@Override
	public void setUid(String id) {
		ad.setUid(id);
	}

	@Override
	public String getUid() {
		return ad.getUid();
	}

	@Override
	public void setId(String id) {
		ad.setId(id);
	}

	@Override
	public String getTitle() {
		return ad.getTitle();
	}

	@Override
	public void setTitle(String title) {
		ad.setTitle(title);
	}

	@Override
	public String getName() {
		return ad.getName();
	}

	@Override
	public void setName(String name) {
		ad.setName(name);
	}

	@Override
	public Class<?> getActivityClass() {
		return ad.getActivityClass();
	}

	@Override
	public void setActivityClass(Class<?> activityClass) {
		ad.setActivityClass(activityClass);
	}

	@Override
	public ArrayList<WidgetDescription> getWidgets() {
		return ad.getWidgets();
	}

	@Override
	public Boolean hasMenu() {
		return ad.hasMenu();
	}

	@Override
	public void setHasMenu(Boolean hasMenu) {
		ad.setHasMenu(hasMenu);
	}

	@Override
	public Boolean handlesKeyPress() {
		return ad.handlesKeyPress();
	}

	@Override
	public void setHandlesKeyPress(Boolean handlesKeyPress) {
		ad.setHandlesKeyPress(handlesKeyPress);
	}

	@Override
	public Boolean handlesLongKeyPress() {
		return ad.handlesLongKeyPress();
	}

	@Override
	public void setHandlesLongKeyPress(Boolean handlesLongKeyPress) {
		ad.setHandlesLongKeyPress(handlesLongKeyPress);
	}

	@Override
	public Boolean isTabActivity() {
		return ad.isTabActivity();
	}

	@Override
	public void setIsTabActivity(Boolean isTabActivity) {
		ad.setIsTabActivity(isTabActivity);
	}

	@Override
	public void setWidgets(ArrayList<WidgetDescription> widgets) {
		ad.setWidgets(widgets);
	}

	@Override
	public HashMap<String, Boolean> getListeners() {
		return ad.getListeners();
	}

	@Override
	public void addListener(String key, Boolean value) {
		ad.addListener(key, value);
	}

	@Override
	public void setListeners(HashMap<String, Boolean> listeners) {
		ad.setListeners(listeners);
	}

	@Override
	public ArrayList<String> getSupportedEvents() {
		return ad.getSupportedEvents();
	}

	@Override
	public void setSupportedEvents(ArrayList<String> supportedEvents) {
		ad.setSupportedEvents(supportedEvents);
	}

	@Override
	public void addSupportedEvent(String key) {
		ad.addSupportedEvent(key);
	}

	@Override
	public String getClassName() {
		return ad.getClassName();
	}

	@Override
	public void setClassName(String className) {
		ad.setClassName(className);
	}

	@Override
	public Boolean getHasMenu() {
		return ad.getHasMenu();
	}

	@Override
	public Boolean getHandlesKeyPress() {
		return ad.getHandlesKeyPress();
	}

	@Override
	public Boolean getHandlesLongKeyPress() {
		return ad.getHandlesLongKeyPress();
	}

	@Override
	public Boolean getIsTabActivity() {
		return ad.getIsTabActivity();
	}

	@Override
	public int getTabsCount() {
		return ad.getTabsCount();
	}

	@Override
	public void setTabsCount(int tabsCount) {
		ad.setTabsCount(tabsCount);
	}

	@Override
	public int getCurrentTab() {
		return ad.getCurrentTab();
	}

	@Override
	public void setCurrentTab(int currentTab) {
		ad.setCurrentTab(currentTab);
	}

	@Override
	public boolean hasOrientationListener() {
		return ad.hasOrientationListener();
	}

	@Override
	public boolean hasLocationListener() {
		return ad.hasLocationListener();
	}

	@Override
	public boolean hasSensorListener() {
		return ad.hasSensorListener();
	}

	@Override
	public boolean hasListener(String listenerName) {
		return ad.hasListener(listenerName);
	}

	@Override
	public boolean isListenerActive(String listenerName) {
		return ad.isListenerActive(listenerName);
	}

	@Override
	public Boolean isRootActivity() {
		return ad.isRootActivity();
	}

	@Override
	public void setIsRootActivity(Boolean isRootActivity) {
		ad.setIsRootActivity(isRootActivity);
	}

	@Override
	public String toString() {
		return ad.toString();//TODO LOW
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTitle(), getName(), getClassName(),
				getHasMenu(), getHandlesKeyPress(), getHandlesLongKeyPress(),
				getIsTabActivity(), getTabsCount(), getCurrentTab(), getWidgets());
	}
}

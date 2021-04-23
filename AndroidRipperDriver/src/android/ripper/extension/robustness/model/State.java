package android.ripper.extension.robustness.model;

import android.ripper.extension.robustness.model.compare.Comparand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unina.android.shared.ripper.model.state.ActivityDescription;
import it.unina.android.shared.ripper.model.state.WidgetDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import static android.ripper.extension.robustness.tools.ObjectTool.propEquals;
import static android.ripper.extension.robustness.tools.ObjectTool.stringsEmpty;

public class State extends ActivityDescription {
    @JsonIgnore
    public static final String LOWEST_UID = "0";
    private final ActivityDescription ad;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof State) {
            State s = (State) obj;
            if (stringsEmpty(getUid()) || stringsEmpty(s.getUid())) {
                boolean widgetsEquality =
//						(propEquals(getWidgets(), s.getWidgets(), ArrayList::size, false) &&
//								getWidgets().containsAll(s.getWidgets())) ||
                        hierarchyEquals(s) ||
                                getWidgets() == s.getWidgets(); //null case
                Supplier<Comparand<State>> su = () -> Comparand.of(this, s);
                return widgetsEquality &&
                        propEquals(su, State::getName) &&
                        propEquals(su, State::getClassName) &&
                        propEquals(su, State::hasMenu) &&
                        (isTabActivity() ?
                                s.isTabActivity() &&
                                        propEquals(su, State::getTabsCount) &&
                                        propEquals(su, State::getCurrentTab) :
                                !s.isTabActivity()
                        );
            } else
                return Objects.equals(getUid(), s.getUid());
        } else
            return false;
    }

    private final HashMap<Integer, HashMap<String, VirtualWD>> hierarchy = new HashMap<>();

    /**
     * Test if the state is hierarchically equals to given state
     * In compared states' view trees, define hierarchically equation:
     * 1. Their VWDs (Virtual WidgetDescription, abstraction of views that are same class
     * and share same parents, recursively) have same layers and same VWD in each layer
     * 1. Same VWD have identical capabilities set and enable/visible status
     *
     * @param state state compared to this
     * @return if two states are considered hierarchically equal
     */
    public boolean hierarchyEquals(State state) {
        return getHierarchy().equals(state.getHierarchy());
    }

    public static String getLowestUid() {
        return LOWEST_UID;
    }

    public static State getExitState() {
        return EXIT_STATE;
    }

    private HashMap<Integer, HashMap<String, VirtualWD>> getHierarchy() {
        if (hierarchy.isEmpty()) {
            HashMap<Integer, VirtualWD> indexMap = new HashMap<>();
            for (WidgetDescription widget : getWidgets()) {
                if (widget.getDepth() < 0) {
                    continue;
                }
                String className = widget.getClassName();
                if (widget.getDepth() > 0) {
                    className = indexMap.get(widget.getParentIndex()).getClassName() + ">" + className;
                }
                indexMap.put(widget.getIndex(), new VirtualWD(className,
                        widget.getListeners(),
                        widget.isEnabled(),
                        widget.isVisible(),
                        widget.getDepth()));
            }
            indexMap.values().forEach(vwd -> hierarchy.compute(vwd.getDepth(), (depth, vwds) -> {
                String className = vwd.getClassName();
                if (vwds == null) {
                    vwds = new HashMap<>();
                    vwds.put(className, vwd);
                } else {
                    vwds.merge(className, vwd, (a, b) -> {
                        //Fusing views capability into VWD by simply logical OR them
                        a.setEnabled(a.getEnabled() | b.getEnabled());
                        a.setVisible(a.getVisible() | b.getVisible());
                        HashMap<String, Boolean> la = a.getListeners(), lb = b.getListeners();
                        lb.forEach((key, value) -> la.put(key, la.getOrDefault(key, false) | value));
                        return a;
                    });
                }
                return vwds;
            }));
        }
        return hierarchy;
    }

    public State() {
        ad = new ActivityDescription();
    }

    public State(ActivityDescription activityDescription) {
        ad = activityDescription;
    }

    @JsonIgnore
    public static final State EXIT_STATE = new State(new ActivityDescription()) {
        @Override
        public String getUid() {
            return "-1";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof State && "-1".equals(((State) obj).getUid());
        }
    };

    static {
        EXIT_STATE.setId("");
        EXIT_STATE.setName("");
        EXIT_STATE.setTitle("");
        EXIT_STATE.setClassName("");
    }

    public ActivityDescription getAd() {
        return ad;
    }

    @Override
    public void setScrollDownAble(Boolean scrollDownAble) {
        ad.setScrollDownAble(scrollDownAble);
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
    public Boolean getPopupShowing() {
        return ad.getPopupShowing();
    }

    @Override
    public void setPopupShowing(Boolean popupShowing) {
        ad.setPopupShowing(popupShowing);
    }

    @Override
    public String toString() {
        return ad.toString();//TODO LOW
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getClassName(),
                getHasMenu(), getHandlesKeyPress(), getHandlesLongKeyPress(),
                getIsTabActivity(), getTabsCount(), getCurrentTab(), getHierarchy());
    }
}

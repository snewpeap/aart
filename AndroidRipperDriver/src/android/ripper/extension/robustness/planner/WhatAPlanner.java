package android.ripper.extension.robustness.planner;

import android.ripper.extension.robustness.model.State;
import android.ripper.extension.robustness.model.VirtualWD;
import it.unina.android.ripper.planner.HandlerBasedPlanner;
import it.unina.android.ripper.planner.Planner;
import it.unina.android.ripper.planner.widget_events.*;
import it.unina.android.ripper.planner.widget_inputs.EditTextInputPlanner;
import it.unina.android.ripper.tools.actions.Actions;
import it.unina.android.shared.ripper.model.state.ActivityDescription;
import it.unina.android.shared.ripper.model.state.WidgetDescription;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.task.TaskList;
import it.unina.android.shared.ripper.model.transition.Input;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static android.ripper.extension.robustness.model.VirtualWD.getDepthFromClassName;
import static it.unina.android.shared.ripper.constants.InteractionType.*;
import static it.unina.android.shared.ripper.constants.SimpleType.*;

public class WhatAPlanner extends Planner {
	public static final String
			SPAN = "SPAN";//指示优先规划不同类型操作
	private static final String[] inputWidgetList = {
			EDIT_TEXT,
			AUTOCOMPLETE_TEXTVIEW,
			SEARCH_BAR
	};
	private static final int SAME_TYPE_MAX_INTERACTIONS = 3;
	private HashMap<String, Integer> typeInteractionMap;

	private void init(State state) {
		typeInteractionMap = new HashMap<>();
		HashMap<Integer, HashMap<String, VirtualWD>> hierarchy = state.getHierarchy();
		hierarchy.entrySet().stream()
				.filter(e -> e.getKey() > 0)
				.forEach(e -> e.getValue().forEach((name, wd) ->
						typeInteractionMap.put(name, Math.min(SAME_TYPE_MAX_INTERACTIONS, wd.getOriginalCount()))));
	}

	@Override
	public TaskList plan(Task currentTask, ActivityDescription activity, String... options) {
		init(((State) activity));
		TaskList taskList = new TaskList();
		if (activity.getPopupShowing() || Actions.softKeyboardShowing()) {
			activity.setScrollDownAble(false);
		}
		planForActivity(taskList, activity, currentTask);
		activity.getWidgets().forEach(wd -> {
			if (ArrayUtils.contains(inputWidgetList, wd.getSimpleType()))
				planForInput(taskList, wd, currentTask);
			else if (wd.getDepth() > 0)
				planForWidget(taskList, wd, currentTask);
		});
//		if (ArrayUtils.contains(options, SPAN)) {
//			//TODO
//		}
		return taskList;
	}

	private void planForActivity(TaskList taskList, ActivityDescription ad, Task t) {
		if (ad.getScrollDownAble() == null || ad.getScrollDownAble())
			taskList.addNewTaskForActivity(t, SCROLL_DOWN);
		if (ad.isTabActivity())
			for (int i = 0; i < ad.getTabsCount(); i++)
				taskList.addNewTaskForActivity(t, SWAP_TAB, Integer.toString(i));
	}

	private void planForInput(TaskList taskList, WidgetDescription wd, Task t) {
		ArrayList<Input> inputs = new ArrayList<>(1);
		inputs.add(new EditTextInputPlanner(wd).getInputForWidget());
		taskList.add(new Task(t, wd, WRITE_TEXT, inputs));
	}

	private void planForWidget(TaskList taskList, WidgetDescription wd, Task t) {
		WidgetEventPlanner widgetEventPlanner = new WidgetEventPlanner(wd);
		boolean shouldLimitInteraction = true;
		for (Map.Entry<String, Function<WidgetDescription, WidgetEventPlanner>> e : provider.entrySet())
			if (e.getKey().equals(wd.getSimpleType())) {
				widgetEventPlanner = e.getValue().apply(wd);
				shouldLimitInteraction = false;
				break;
			}
		if (widgetEventPlanner != null && shouldLimitInteraction) {
			for (Map.Entry<String, Integer> next : typeInteractionMap.entrySet()) {
				String name = next.getKey();
				if (name.endsWith(wd.getClassName()) && getDepthFromClassName(name) == wd.getDepth()) {
					int left = next.getValue();
					if (left > 0) {
						shouldLimitInteraction = false;
						left -= 1;
						next.setValue(left);
					}
					break;
				}
			}
		}
		if (widgetEventPlanner != null && !shouldLimitInteraction) {
			taskList.addAll(widgetEventPlanner.planForWidget(t, null));
		}
	}

	private static final Map<String, Function<WidgetDescription, WidgetEventPlanner>> provider = new HashMap<>();
	static {
//		provider.put(RECYCLER_VIEW, wd -> new RecyclerViewEventPlanner(wd, MAX_INTERACTIONS_FOR_LIST));
//		provider.put(LIST_VIEW, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_LIST));
		provider.put(DRAWER_LIST_VIEW, DrawerListViewEventPlanner::new);
		provider.put(PREFERENCE_LIST, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_PREFERENCES_LIST));
		provider.put(SINGLE_CHOICE_LIST, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_SINGLE_CHOICE_LIST));
		provider.put(MULTI_CHOICE_LIST, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_MULTI_CHOICE_LIST));
		provider.put(SPINNER, wd -> new SpinnerEventPlanner(wd, MAX_INTERACTIONS_FOR_SPINNER));
		provider.put(RADIO_GROUP, wd -> new RadioGroupEventPlanner(wd, MAX_INTERACTIONS_FOR_RADIO_GROUP));
		provider.put(SEEK_BAR, SeekBarEventPlanner::new);
		provider.put(RATING_BAR, SeekBarEventPlanner::new);
		provider.put(MENU_ITEM, MenuItemEventPlanner::new);
		provider.put(LIST_ITEM, UnlimitedItemEventPlanner::new);
		Function<WidgetDescription, WidgetEventPlanner> nul = wd -> null;
		provider.put(WEB_VIEW, nul);
		for (String type : HandlerBasedPlanner.inputWidgetList)
			provider.put(type, nul);
	}
}

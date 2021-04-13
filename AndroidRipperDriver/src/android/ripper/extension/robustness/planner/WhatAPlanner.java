package android.ripper.extension.robustness.planner;

import it.unina.android.ripper.planner.HandlerBasedPlanner;
import it.unina.android.ripper.planner.Planner;
import it.unina.android.ripper.planner.widget_events.*;
import it.unina.android.ripper.planner.widget_inputs.EditTextInputPlanner;
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

import static it.unina.android.shared.ripper.constants.InteractionType.*;
import static it.unina.android.shared.ripper.constants.SimpleType.*;

public class WhatAPlanner extends Planner {
	public static final String
			SPAN = "SPAN";//指示优先规划不同类型操作

	@Override
	public TaskList plan(Task currentTask, ActivityDescription activity, String... options) {
		TaskList taskList = new TaskList();
		if (activity.getPopupShowing()) {
			activity.setScrollDownAble(false);
		}
		planForActivity(taskList, activity, currentTask);
		activity.getWidgets().forEach(wd -> {
			if (ArrayUtils.contains(HandlerBasedPlanner.inputWidgetList, wd.getSimpleType()))
				planForInput(taskList, wd, currentTask);
			else
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
		if (EDIT_TEXT.equals(wd.getSimpleType()) || AUTOCOMPLETE_TEXTVIEW.equals(wd.getSimpleType()))
			inputs.add(new EditTextInputPlanner(wd).getInputForWidget());
		//else else else...
		taskList.add(new Task(t, wd, WRITE_TEXT, inputs));
	}

	private void planForWidget(TaskList taskList, WidgetDescription wd, Task t) {
		WidgetEventPlanner widgetEventPlanner = new WidgetEventPlanner(wd);
		for (Map.Entry<String, Function<WidgetDescription, WidgetEventPlanner>> e : provider.entrySet())
			if (e.getKey().equals(wd.getSimpleType())) {
				widgetEventPlanner = e.getValue().apply(wd);
				break;
			}
		if (widgetEventPlanner != null)
			taskList.addAll(widgetEventPlanner.planForWidget(t, null));
	}

	private static final Map<String, Function<WidgetDescription, WidgetEventPlanner>> provider = new HashMap<>();
	static {
		provider.put(LIST_VIEW, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_LIST));
		provider.put(DRAWER_LIST_VIEW, DrawerListViewEventPlanner::new);
		provider.put(PREFERENCE_LIST, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_PREFERENCES_LIST));
		provider.put(SINGLE_CHOICE_LIST, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_SINGLE_CHOICE_LIST));
		provider.put(MULTI_CHOICE_LIST, wd -> new ListViewEventPlanner(wd, MAX_INTERACTIONS_FOR_MULTI_CHOICE_LIST));
		provider.put(SPINNER, wd -> new SpinnerEventPlanner(wd, MAX_INTERACTIONS_FOR_SPINNER));
		provider.put(RADIO_GROUP, wd -> new RadioGroupEventPlanner(wd, MAX_INTERACTIONS_FOR_RADIO_GROUP));
		provider.put(TEXT_VIEW, TextViewEventPlanner::new);
		provider.put(IMAGE_VIEW, ImageViewEventPlanner::new);
		provider.put(SEEK_BAR, SeekBarEventPlanner::new);
		provider.put(RATING_BAR, SeekBarEventPlanner::new);
		provider.put(MENU_ITEM, MenuItemEventPlanner::new);
		Function<WidgetDescription, WidgetEventPlanner> nul = wd -> null;
		provider.put(RELATIVE_LAYOUT, nul);
		provider.put(LINEAR_LAYOUT, nul);
		provider.put(WEB_VIEW, nul);
		for (String type : HandlerBasedPlanner.inputWidgetList)
			provider.put(type, nul);
	}
}

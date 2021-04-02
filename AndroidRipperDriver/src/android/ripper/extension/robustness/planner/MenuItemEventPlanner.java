package android.ripper.extension.robustness.planner;

import it.unina.android.shared.ripper.model.state.WidgetDescription;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.task.TaskList;
import it.unina.android.shared.ripper.model.transition.Input;

import java.util.ArrayList;

public class MenuItemEventPlanner extends it.unina.android.ripper.planner.widget_events.MenuItemEventPlanner {
	/**
	 * Constructor
	 *
	 * @param widgetDescription widget
	 */
	public MenuItemEventPlanner(WidgetDescription widgetDescription) {
		super(widgetDescription);
	}

	@Override
	public TaskList planForWidget(Task currentTask, ArrayList<Input> inputs, String... options) {
		TaskList taskList = new TaskList();
		if (mWidget.getEnabled()) {
			taskList.addAll(tap(currentTask, inputs, options));
		}
		return taskList;
	}
}

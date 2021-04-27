package android.ripper.extension.robustness.planner;

import it.unina.android.ripper.planner.widget_events.ListViewEventPlanner;
import it.unina.android.shared.ripper.constants.InteractionType;
import it.unina.android.shared.ripper.model.state.WidgetDescription;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.task.TaskList;
import it.unina.android.shared.ripper.model.transition.Input;

import java.util.ArrayList;

public class RecyclerViewEventPlanner extends ListViewEventPlanner {
	/**
	 * Constructor
	 *
	 * @param widgetDescription Widget
	 * @param MAX_INTERACTIONS  Max list items to be considered
	 */
	public RecyclerViewEventPlanner(WidgetDescription widgetDescription, int MAX_INTERACTIONS) {
		super(widgetDescription, MAX_INTERACTIONS);
	}

	@Override
	protected TaskList tap(Task currentTask, ArrayList<Input> inputs, String... options) {
		TaskList t = new TaskList();
		int count = mWidget.getCount() != null ? mWidget.getCount() : 0;
		for (int i = 1; i <= Math.min(count, MAX_INTERACTIONS); i++)
			t.addNewTaskForWidget(currentTask, mWidget, InteractionType.SELECT_RECYCLER_VIEW_ITEM, inputs, Integer.toString(i));
		return t;
	}

	@Override
	protected TaskList longTap(Task currentTask, ArrayList<Input> inputs, String... options) {
		return new TaskList();
	}
}

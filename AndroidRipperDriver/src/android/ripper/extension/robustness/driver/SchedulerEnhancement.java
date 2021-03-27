package android.ripper.extension.robustness.driver;

import it.unina.android.ripper.scheduler.Scheduler;
import it.unina.android.shared.ripper.model.transition.IEvent;

import java.util.ListIterator;

public interface SchedulerEnhancement extends Scheduler {
	/**
	 * return task to execute in form of ListIterator
	 *
	 * @return task iterator
	 */
	ListIterator<IEvent> nextTaskIterator();

	void needRecovery();
}

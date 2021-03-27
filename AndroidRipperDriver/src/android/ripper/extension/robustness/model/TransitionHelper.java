package android.ripper.extension.robustness.model;

import it.unina.android.shared.ripper.constants.InteractionType;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.transition.Event;

import java.util.LinkedList;
import java.util.List;

public class TransitionHelper {
	/*
	Transition factory method
	 */
	public static Transition v(State fromState, State toState, Task task) {
		return new TransitionInfo(fromState, toState, task);
	}

	public static class TransitionInfo extends Transition {
		private final LinkedList<Event> possibleBackEvents = new LinkedList<>();

		public TransitionInfo(State fromState, State toState, Task task) {
			super(fromState, toState, task);
		}

		public void goodFeedback() {
			//TODO
		}

		public void poorFeedback() {
			//TODO
		}

		/**
		 * learn possible back event from previous transitions between fromState and toState
		 *
		 * @param formerTransitions previous transitions between fromState and toState
		 * @return self
		 */
		public TransitionInfo learn(List<Task> formerTransitions) {
			//TODO
			return this;
		}

		public Task backRecommend() {
			Task recommend = (Task) super.getTask().clone();
			if (possibleBackEvents.isEmpty()) {
				Event simplyBack = new Event(InteractionType.BACK);
				possibleBackEvents.add(simplyBack);
				recommend.add(simplyBack);
			} else {
				recommend.add(possibleBackEvents.getLast());
			}
			return recommend;
		}
	}
}

package android.ripper.extension.robustness.model;

import it.unina.android.shared.ripper.model.task.Task;

public class Transition {
	private final long id;
	private final State fromState, toState;
	private final Task task;

	Transition(State fromState, State toState, Task task) {
		this.fromState = fromState;
		this.toState = toState;
		this.task = task;
		this.id = System.currentTimeMillis();
	}

	public State getFromState() {
		return fromState;
	}

	public long getId() {
		return id;
	}

	public State getToState() {
		return toState;
	}

	public Task getTask() {
		return task;
	}
}

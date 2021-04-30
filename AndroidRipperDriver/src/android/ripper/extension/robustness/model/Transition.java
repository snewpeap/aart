package android.ripper.extension.robustness.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.transition.Event;
import it.unina.android.shared.ripper.model.transition.IEvent;

import java.util.ArrayList;
import java.util.List;

public class Transition {
	private long id;
	private State fromState, toState;
	private Task task;

	public Transition() {
	}

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

	@JsonIgnore
	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (IEvent iEvent : task) {
			events.add((Event) iEvent);
		}
		return events;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setFromState(State fromState) {
		this.fromState = fromState;
	}

	public void setToState(State toState) {
		this.toState = toState;
	}

	public void setTask(Task task) {
		this.task = task;
	}
}

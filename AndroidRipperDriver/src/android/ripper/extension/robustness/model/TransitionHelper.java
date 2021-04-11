package android.ripper.extension.robustness.model;

import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.transition.Event;
import it.unina.android.shared.ripper.model.transition.IEvent;
import it.unina.android.shared.ripper.model.transition.Input;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static it.unina.android.shared.ripper.constants.InteractionType.*;

public class TransitionHelper {
	/*
	Transition factory method
	 */
	public static Transition v(State fromState, State toState, Task task) {
		return new TransitionInfo(fromState, toState, task);
	}

	public static class TransitionInfo extends Transition {
		private final LinkedList<Event> possibleBackEvents = new LinkedList<>();
		private boolean needLearning = true;
		private static final int MAX_RECOMMEND_REPEAT = 2;

		public TransitionInfo(State fromState, State toState, Task task) {
			super(fromState, toState, task);
			Map<Predicate<Event>, Function<Event, Event>> backs = new HashMap<>(backKnowledge);
			backs.put(
					e -> e.is(SWAP_TAB) && fromState.isTabActivity() && toState.isTabActivity(),
					e -> new Event(SWAP_TAB, null, Integer.toString(fromState.getCurrentTab()))
			);
			backs.put(
					//for fromState that shows popup, put event=input[back, input_cause_popup]
					e -> fromState.getPopupShowing() && !State.EXIT_STATE.equals(toState)
							&& (e.is(CLICK) || e.is(CLICK_MENU_ITEM)),
					e -> {
						Event event = new Event();
						event.addInput(toState.getWidgets().get(0), BACK, "");
						Event tapToPopupEvent = (Event) task.get(task.size() - 2);
						if (tapToPopupEvent.getInputs() == null) {
							event.addInput(tapToPopupEvent.getWidget(), tapToPopupEvent.getInteraction(), "");
						} else {
							for (Input input : tapToPopupEvent.getInputs()) {
								event.addInput(input.getWidget(), input.getInputType(), input.getValue());
							}
						}
						return event;
					}
			);

			Event tail = (Event) task.getLast();
			for (Map.Entry<Predicate<Event>, Function<Event, Event>> e : backs.entrySet()) {
				if (e.getKey().test(tail)) {
					possibleBackEvents.addLast(e.getValue().apply(tail));
					needLearning = false;
					break;
				}
			}
			possibleBackEvents.addLast(new Event(BACK));
			possibleBackEvents.addLast(null);
		}

		public void goodFeedback() {
			Event recommend = possibleBackEvents.getFirst();
			if (recommend != null && possibleBackEvents.lastIndexOf(recommend) < MAX_RECOMMEND_REPEAT)
				possibleBackEvents.addFirst(recommend);
		}

		public void poorFeedback() {
			if (possibleBackEvents.getFirst() != null)
				possibleBackEvents.addLast(possibleBackEvents.pollFirst());
			else
				needLearning = true;
		}

		private boolean similar(State a, State b) {
			//TODO can't be too shallow. use distance?
			return a.equals(b);
		}

		/**
		 * learn possible back event from previous transitions between fromState and toState
		 *
		 * @param formerTransitions previous transitions between fromState and toState
		 * @return self
		 */
		public TransitionInfo learnToBack(Collection<Transition> formerTransitions) {
			if (needLearning) {
				Map<IEvent, Long> m = formerTransitions.parallelStream()
						.filter(t -> similar(getFromState(), t.getToState()) && similar(getToState(), t.getFromState()))
						.collect(Collectors.groupingBy(t -> t.getTask().getLast(), Collectors.counting()));
				if (!m.isEmpty())
					possibleBackEvents.addAll(0, m.entrySet().stream()
							.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
							.collect(LinkedList<Event>::new, (l, e) -> l.add((Event) e.getKey()), LinkedList::addAll));
				needLearning = false;
			}
			return this;
		}

		public ListIterator<IEvent> backRecommend() {
			Task recommend = (Task) super.getTask().clone();
			int index = recommend.size();
			if (possibleBackEvents.getFirst() != null) {
				recommend.add(possibleBackEvents.getFirst());
			}
			return recommend.listIterator(index);
		}

		private static final Map<Predicate<Event>, Function<Event, Event>> backKnowledge = new HashMap<>();

		static {
			backKnowledge.put(e -> e.is(SCROLL_DOWN),//滑上又滑落
					e -> new Event(SCROLL_UP));
			backKnowledge.put(e -> e.is(WRITE_TEXT),
					e -> new Event(WRITE_TEXT, e.getWidget(), ""));
		}
	}
}

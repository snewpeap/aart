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
		}

		public void goodFeedback() {
			Event recommend = getPossibleBackEvents().getFirst();
			if (recommend != null && getPossibleBackEvents().lastIndexOf(recommend) < MAX_RECOMMEND_REPEAT)
				getPossibleBackEvents().addFirst(recommend);
		}

		public void poorFeedback() {
			if (getPossibleBackEvents().getFirst() != null)
				getPossibleBackEvents().addLast(getPossibleBackEvents().pollFirst());
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
					getPossibleBackEvents().addAll(0, m.entrySet().stream()
							.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
							.collect(LinkedList<Event>::new, (l, e) -> l.add((Event) e.getKey()), LinkedList::addAll));
				needLearning = false;
			}
			return this;
		}

		public ListIterator<IEvent> backRecommend() {
			Task recommend = (Task) super.getTask().clone();
			int index = recommend.size();
			if (getPossibleBackEvents().getFirst() != null) {
				recommend.add(getPossibleBackEvents().getFirst());
			}
			return recommend.listIterator(index);
		}

		protected LinkedList<Event> getPossibleBackEvents() {
			if (possibleBackEvents.isEmpty()) {

				Map<Predicate<Event>, Function<Event, Event>> backs = new HashMap<>(backKnowledge);
				backs.put(
						e -> e.is(SWAP_TAB) && getFromState().isTabActivity() && getToState().isTabActivity(),
						e -> new Event(SWAP_TAB, null, Integer.toString(getFromState().getCurrentTab()))
				);
				backs.put(
						//for fromState that shows popup, put event=input[back, input_cause_popup]
						e -> getFromState().getPopupShowing() && !State.EXIT_STATE.equals(getToState())
								&& (e.is(CLICK) || e.is(CLICK_MENU_ITEM)) && getTask().size() > 1,
						e -> {
							Event event = new Event();
							event.addInput(getToState().getWidgets().get(0), BACK, "");
							Event tapToPopupEvent = (Event) getTask().get(getTask().size() - 2);
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

				Event tail = (Event) getTask().getLast();
				for (Map.Entry<Predicate<Event>, Function<Event, Event>> e : backs.entrySet()) {
					if (e.getKey().test(tail)) {
						possibleBackEvents.addLast(e.getValue().apply(tail));
						break;
					}
				}
				possibleBackEvents.addLast(new Event(BACK));
				possibleBackEvents.addLast(null);
			}
			return possibleBackEvents;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof TransitionInfo)) return false;
			TransitionInfo t = ((TransitionInfo) obj);
			boolean equals = getFromState().equals(t.getFromState()) && getToState().equals(t.getToState());
			if (equals) {
				Event thisTail = ((Event) getTask().getLast()), thatTail = ((Event) t.getTask().getLast());
				ArrayList<Input> thisInputs = thisTail.getInputs(), thatInputs = thatTail.getInputs();
				if (thisInputs == null && thatInputs == null) {
					equals = Objects.equals(thisTail.getWidget(), thatTail.getWidget()) &&
							Objects.equals(thisTail.getInteraction(), thatTail.getInteraction());
				} else if (equals = thisInputs != null && thatInputs != null) {
					if (equals = thisInputs.size() == thatInputs.size()) {
						for (int i = 0; i < thisInputs.size(); i++) {
							Input thisInput = thatInputs.get(i), thatInput = thatInputs.get(i);
							equals = Objects.equals(thisInput.getInputType(), thatInput.getInputType()) &&
									Objects.equals(thisInput.getWidget(), thatInput.getWidget());
						}
					}
				}
			}
			return equals;
		}

		@Override
		public int hashCode() {
			return Objects.hash(getFromState(), getToState());
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

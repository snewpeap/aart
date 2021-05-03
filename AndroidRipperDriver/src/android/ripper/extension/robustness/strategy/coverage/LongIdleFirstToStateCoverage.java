package android.ripper.extension.robustness.strategy.coverage;

import android.ripper.extension.robustness.model.State;
import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Coverage;
import it.unina.android.shared.ripper.model.transition.Event;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class LongIdleFirstToStateCoverage implements Coverage {
	@Override
	public Collection<Transition> cherryPick(Set<Transition> transitions) {
		//first group by toState
		//then group by idle
		Map<State, Map<Integer, LinkedList<Transition>>> toStateIdleMap = transitions.parallelStream()
				.filter(t -> !State.EXIT_STATE.equals(t.getToState()) && t.getToState().reentered())
				.collect(Collectors.groupingBy(Transition::getToState, Collectors.toMap(
						t -> ((Event) t.getTask().getLast()).getIdle(),
						t -> new LinkedList<Transition>() {{
							add(t);
						}},
						// l1 is existing and l2 is the new one
						(l1, l2) -> {
							//sort by id
							for (Transition toAdd : l2) {
								ListIterator<Transition> iter = l1.listIterator(0);
								while (iter.hasNext()) {
									if (iter.next().getId() > toAdd.getId()) {
										iter.previous();
										iter.add(toAdd);
										break;
									}
								}
							}
							return l1;
						}
				)));
		ArrayList<Transition> result = new ArrayList<>();
		toStateIdleMap.forEach((k, v) -> v.forEach((kk, vv) -> {
			//pickNum = 1 + log2(idle / Event.IDLE_SIZE)
			int pickNum = (int) (Math.log(BigDecimal.valueOf(kk).divideToIntegralValue(BigDecimal.valueOf(Event.IDLE_SIZE)).intValue()) / Math.log(2));
			result.addAll(vv.subList(0, Math.min(pickNum + 1, vv.size())));
		}));
		return result;
	}
}

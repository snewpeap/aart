package android.ripper.extension.robustness.driver;

import android.ripper.extension.robustness.exception.RipperIllegalArgException;
import android.ripper.extension.robustness.exception.RipperNullMsgException;
import android.ripper.extension.robustness.model.State;
import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.model.TransitionHelper;
import android.ripper.extension.robustness.model.TransitionHelper.TransitionInfo;
import android.ripper.extension.robustness.output.TestSuiteGenerator;
import android.ripper.extension.robustness.planner.WhatAPlanner;
import it.unina.android.ripper.driver.AbstractDriver;
import it.unina.android.ripper.driver.exception.RipperRuntimeException;
import it.unina.android.ripper.net.RipperServiceSocket;
import it.unina.android.ripper.termination.TerminationCriterion;
import it.unina.android.ripper.tools.actions.Actions;
import it.unina.android.ripper.tools.logcat.LogcatDumper;
import it.unina.android.shared.ripper.input.RipperInput;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.task.TaskList;
import it.unina.android.shared.ripper.model.transition.Event;
import it.unina.android.shared.ripper.model.transition.IEvent;
import it.unina.android.shared.ripper.net.Message;
import it.unina.android.shared.ripper.output.RipperOutput;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Paths;
import java.util.*;

import static android.ripper.extension.robustness.tools.MethodNameHelper.caller;
import static android.ripper.extension.robustness.tools.MethodNameHelper.here;

/**
 * Android Automated Robustness Test Driver
 *
 * @author YRD & LC
 */
public class AARTDriver extends AbstractDriver {
	private static final String RIPPER_SERVICE_PKG = "it.unina.android.ripper_service";
	private static final FastDateFormat LOGCAT_TIME_FORMAT = FastDateFormat.getInstance("MM-dd hh:mm:ss.mmm");
	private String loopStartTime = LOGCAT_TIME_FORMAT.format(0);

	private final HashMap<State, State> states = new HashMap<>();
	private final SortedSet<Transition> transitions = new TreeSet<>(Comparator.comparing(Transition::getId));
	private State prevState = null;
	private State lastSavedState = null;
	private State currState;

	private final SchedulerEnhancement yabScheduler;
	private final TestSuiteGenerator testSuiteGenerator;
	private final boolean generateTestSuite;

	@Override
	public void rippingLoop() {
		startupDevice();
		setupEnvironment();
		int i = 0;
		do {
			readyToLoop();

			Task taskJustDone = null;
			while (true) {
				currState = getCurrentDescriptionAsState();
				if (states.isEmpty()) {					//the beginning of everything
					currState.setUid(State.LOWEST_UID);
					addNewState();
					yabScheduler.addTasks(planner.plan(taskJustDone, currState, WhatAPlanner.SPAN));
				} else {
					if (State.EXIT_STATE.equals(currState)) {
						transitions.add(TransitionHelper.v(prevState, currState, taskJustDone));
						notifyRipperLog("Accidentally exit AUT");
						break;
					} else if (states.containsKey(currState)) {	//already occurred state
						currState = states.get(currState);
					} else {									//brand new state
						currState.setUid(increaseUid(lastSavedState.getUid()));
						addNewState();
						yabScheduler.addTasks(planner.plan(taskJustDone, currState));
					}
					if (prevState != null && taskJustDone != null && !taskJustDone.isEmpty())	//normal transition
						transitions.add(TransitionHelper.v(prevState, currState, taskJustDone));
					else					//null prevState and nonempty states set mean accident
						yabScheduler.needRecovery();
				}
				ListIterator<IEvent> taskTodo = yabScheduler.nextTaskIterator();
				if (taskTodo == null || !taskTodo.hasNext()) {
					notifyRipperLog(String.format("State %s %s has no task to execute.",
							currState.getUid(), currState.getName()));
					break;
				}
				executeTask(taskTodo);
				taskJustDone = iterToTask(taskTodo);
				prevState = currState;
			}

			endLoop();
			i++;
		} while (running && !checkTerminationCriteria() && i < 1);

		//TODO Model output

		if (generateTestSuite)
			testSuiteGenerator.generate(new TreeSet<>(transitions));
		notifyRipperEnded();
	}

	public AARTDriver(RipperInput ripperInput, RipperOutput ripperOutput, boolean generateTestsuite,
					  String coverage, String perturb) {
		this.ripperInput = ripperInput;
		this.ripperOutput = ripperOutput;

		YetAnotherBreadthScheduler yabs = new YetAnotherBreadthScheduler();
		addTerminationCriterion(yabs);
		this.yabScheduler = yabs;
		//TODO AUT_PACKAGE and AUT_MAIN_ACTIVITY is none
		this.testSuiteGenerator = new TestSuiteGenerator(AUT_PACKAGE, coverage, perturb, AUT_MAIN_ACTIVITY);
		this.planner = new WhatAPlanner();

		this.generateTestSuite = generateTestsuite;
	}

	@Override
	public void startupDevice() {
		//TODO LOW 需要监控线程
		if (device.isStarted())
			return;
		device.start();
		device.waitForDevice(); //TODO LOW 方法没有反馈，加上返回值
		device.setStarted(true);
	}

	@Override
	public Message waitAck() {
		Message message;
		for (int retry = 0; retry < ACK_MAX_RETRY; retry++) {
			try {
				if ((message = rsSocket.readMessage(1000, false)) != null)
					return message;
			} catch (SocketException ignored) {}
		}
		throw new RipperNullMsgException(AARTDriver.class, caller() + "->" + here(), "readMessage()");
	}

	public void executeEvent(Event event) {
		if (event == null || (StringUtils.isEmpty(event.getInteraction()) && event.getInputs() == null))
			throw new RipperIllegalArgException(AARTDriver.class, here(), "event", String.valueOf(event));
		event.setEventUID(eventsUIDCounter++);
		notifyRipperLog("executeEvent.event=" + event);
		if (event.getInputs() != null) {
			rsSocket.sendInputs(Long.toString(event.getEventUID()), event.getInputs());
			for (int i = event.getInputs().size(); i > 0; i--) {
				waitAck();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ignored) {
			}
		}
		else {
			rsSocket.sendEvent(event);
			waitAck();
		}
	}

	private void executeTask(Iterator<IEvent> taskIter) {
		while (taskIter.hasNext()) {
			executeEvent((Event) taskIter.next());
		}
	}

	/**
	 * Start ripper and AUT, check alive and then create log file.
	 */
	private void readyToLoop() {
		new LogcatDumper(device.getName(),
				String.format("%slogcat_%s_%d.txt", LOGCAT_PATH, device.getName(), LOGCAT_FILE_NUMBER++),
				loopStartTime).start();
		loopStartTime = LOGCAT_TIME_FORMAT.format(new Date());
		checkSocket();
		super.startup();

		notifyRipperLog("Check alive...");
		try {
			boolean alive = rsSocket.isAlive() && Actions.checkCurrentForegroundActivityPackage(AUT_PACKAGE);
			if (!alive)
				throw new RipperRuntimeException(AARTDriver.class, here(), "Not alive!");
		} catch (SocketException ex) {
			throw new RipperRuntimeException(AARTDriver.class, here(), ex.getMessage(), ex);
		}
		createLogFile();
	}

	private void endLoop() {
		endLogFile();

		notifyRipperLog("End message...");
		rsSocket.sendMessage(Message.getEndMessage());
		waitAck();

		uninstallAPKs(false);
	}

	public final State getCurrentDescriptionAsState() {
		try {
			String cd;
			try {
				cd = getCurrentDescription();
			} catch (RipperRuntimeException e) {
				return State.EXIT_STATE;
			}
			return new State(ripperInput.inputActivityDescription(Optional.ofNullable(cd)
					.orElseThrow(() -> new RipperNullMsgException(AARTDriver.class, here(), "getCurrentDescription"))));
		} catch (IOException e) {
			throw new RipperRuntimeException(AARTDriver.class, here(), e.getMessage(), e);
		}
	}

	private void setupEnvironment() {
		endRipperTask(false, false);
		if (INSTALL_FROM_SDCARD) {
			Actions.pushToSD(Paths.get(TEMP_PATH, "aut.apk").toAbsolutePath().toString());
			Actions.pushToSD(Paths.get(TEMP_PATH, "ripper.apk").toAbsolutePath().toString());
		}
		if (device.isVirtualDevice())
			device.unlockDevice();

		//install and start service
		if (Actions.checkApplicationInstalled(RIPPER_SERVICE_PKG))
			Actions.uninstallAPK(RIPPER_SERVICE_PKG);
		Actions.installAPK(SERVICE_APK_PATH);
		startService();
	}

	private void startService() {
		//TODO LOW 需要守护线程
		notifyRipperLog("Start ripper service...");
		Actions.startAndroidRipperService();
		if (device.isVirtualDevice())
			Actions.redirectPort(SERVICE_HOST_PORT, SERVICE_HOST_PORT);
	}

	private void checkSocket() {
		if (rsSocket == null) {
			rsSocket = new RipperServiceSocket(device.getIpAddress(), SERVICE_HOST_PORT);
		} else {
			Socket socket = rsSocket.getSocket();
			if (socket == null || socket.isClosed())
				rsSocket = new RipperServiceSocket(device.getIpAddress(), SERVICE_HOST_PORT);
		}
	}

	private void addNewState() {
		states.put(currState, currState);
		lastSavedState = currState;
		notifyRipperLog(String.format("new state %s = %s", currState.getUid(), currState.toString()));
	}

	private String increaseUid(String lastUid) {
		try {
			return Integer.toString(Integer.parseInt(lastUid) + 1);
		} catch (NumberFormatException e) {
			throw new RipperRuntimeException(AARTDriver.class, here(), "Last_State=" + lastSavedState.toString(), e);
		}
	}

	public Task iterToTask(ListIterator<IEvent> iter) {
		if (iter == null) return null;
		while (iter.hasPrevious()) iter.previous();
		Task task = new Task();
		while (iter.hasNext()) task.add(iter.next());
		return task;
	}

	public class YetAnotherBreadthScheduler implements TerminationCriterion, SchedulerEnhancement {
		private final HashMap<State, TaskList> schedule = new HashMap<State, TaskList>() {
			@Override
			public TaskList putIfAbsent(State key, TaskList value) {
				if (key == null || StringUtils.isEmpty(key.getUid()))
					throw new RipperIllegalArgException(YetAnotherBreadthScheduler.class,
							here(), "key", String.valueOf(key));
				return super.putIfAbsent(key, value);
			}
		};

		private boolean back = false;
		private boolean recovery = false;
		private Pair<State, TaskList> pivot = null;	//current BFS pivot state and its taskList
		private final Deque<State> bfsDeque = new ArrayDeque<>();
		private final Set<String> pivoted = new HashSet<>();//uid of states that used to be pivot
		private Task pathToPivot = null;

		@Override
		public ListIterator<IEvent> nextTaskIterator() {
			ListIterator<IEvent> nextTask = null;
			if (back) {	//have to go back to pivot
				if (currState.equals(pivot.getKey())) {
					notifyRipperLog("A loop at pivot, continue...");
					ListIterator<Task> iter = pivot.getValue().getIterator();
					nextTask = iter.hasNext() ? nextTaskAtPivot(iter) : newPivotAndNextTask();
				} else {
					TransitionInfo transitionInfo = (TransitionInfo) transitions.last();
					//learn from former transitions then make recommend
					nextTask = transitionInfo.learnToBack(transitions).backRecommend();
					notifyRipperLog("Going back...");
					back = false;
				}
			} else {	//at pivot, or need to recover to pivot
				if (pivot == null) {
					pivot = Pair.of(currState, schedule.get(currState));
					ListIterator<Task> iter = pivot.getValue().getIterator();
					if (iter.hasNext()) {
						nextTask = nextTaskAtPivot(iter);
					}
				} else {
					TransitionInfo transitionInfo = (TransitionInfo) transitions.last();
					if (currState.equals(pivot.getKey())) {	//current state should be pivot state if back as expected
						recovery = false;                	//if not, null would be returned
						notifyRipperLog("We're at pivot!");
						transitionInfo.goodFeedback();   	//last back was done great
						ListIterator<Task> iter = pivot.getValue().getIterator();
						//hasNext() == true: current pivot has remaining task to execute
						//else: pivot has been completely searched, so choose a "downstream" state as the new pivot
						nextTask = iter.hasNext() ? nextTaskAtPivot(iter) : newPivotAndNextTask();
					} else if (recovery) {    			//need recovery...
						notifyRipperLog("Recovering...");
						transitionInfo.poorFeedback();	//...because last back event is not the right one
						if (pathToPivot == null) {
							recovery = false;
							ListIterator<Task> iter = pivot.getValue().getIterator();
							nextTask = iter.hasNext() ? nextTaskAtPivot(iter) : newPivotAndNextTask();
						} else
							nextTask = pathToPivot.listIterator(0);
					}
				}
			}
			return nextTask;
		}

		@Override
		public Task nextTask() {
			return iterToTask(nextTaskIterator());
		}

		@Override
		public void addTask(Task t) {}

		@Override
		public void addTasks(TaskList taskList) {
			if (taskList != null)
				schedule.putIfAbsent(currState, taskList);
		}

		@Override
		public TaskList getTaskList() {
			return null;
		}

		@Override
		public void clear() {}

		/**
		 * Assign pivot to a "downstream" state of the current state, meanwhile current state is marked pivoted,
		 * and latest transition (task) from it to new pivot state is return.
		 * if no new pivot assigned, null return, and check() should return true after the invocation
		 *
		 * @return latest transition task to new pivot state, or null when no new pivot
		 */
		private Task assignNextPivot() {
			State prevPivot = this.pivot.getKey();
			pivoted.add(prevPivot.getUid());
			transitions.stream()
					.filter(t -> {
						State to = t.getToState();
						return t.getFromState().equals(prevPivot) &&
								!State.EXIT_STATE.equals(to) && !pivoted.contains(to.getUid());
					})//enqueue bfs target, possibly no state enqueue
					.map(Transition::getToState).distinct().forEachOrdered(bfsDeque::push);

			Task task = new Task();
			if (!bfsDeque.isEmpty()) {
				State newPivot = bfsDeque.removeLast();
				this.pivot = Pair.of(newPivot, schedule.get(newPivot));
				notifyRipperLog(String.format("Assign new pivot = State %s", newPivot.getUid()));
				//find the latest transition from previous pivot to new pivot
				Transition lastTransitionToNewPivot = null;
				for (Transition t : transitions)
					if (t.getFromState().equals(prevPivot) && t.getToState().equals(newPivot))
						lastTransitionToNewPivot = t;
				if (lastTransitionToNewPivot == null) {
					notifyRipperLog(String.format("No path found from this pivot(State %s) to new pivot(State %s)",
							prevPivot.getUid(), newPivot.getUid()));
					return null;
				} else task = lastTransitionToNewPivot.getTask();
			} else {
				pivot = null;
			}
			return task;
		}

		private ListIterator<IEvent> newPivotAndNextTask() {
			pathToPivot = assignNextPivot();
			if (pivot == null) {
				notifyRipperLog("No more new pivot");
				return null;
			}
			if (pathToPivot == null) {
				pathToPivot = (Task) schedule.get(pivot.getKey()).get(0).clone();
				pathToPivot.remove(pathToPivot.size() - 1);
				return pathToPivot.listIterator(pathToPivot.size());
			}
			return pathToPivot.listIterator(pathToPivot.size() - 1);
		}

		private ListIterator<IEvent> nextTaskAtPivot(ListIterator<Task> iter) {
			back = true;
			Task t = iter.next();
			return t.listIterator(t.size() - 1);
		}

		@Override
		public void init(AbstractDriver driver) {
			//do nothing
		}

		@Override
		public boolean check() {
			return bfsDeque.isEmpty() && pivot == null;
		}

		@Override
		public void needRecovery() {
			notifyRipperLog("We've lost the way!");
			recovery = true;
			back = false;
		}
	}
}

package android.ripper.extension.robustness.driver;

import android.ripper.extension.robustness.exception.RipperIllegalArgException;
import android.ripper.extension.robustness.exception.RipperNullMsgException;
import android.ripper.extension.robustness.model.State;
import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.model.TransitionHelper;
import android.ripper.extension.robustness.model.TransitionHelper.TransitionInfo;
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

	private final HashMap<State, String> states = new HashMap<>();
	private final SortedSet<Transition> transitions = new TreeSet<>(Comparator.comparing(Transition::getId));

	private State prevState = null, lastSavedState = null, currState;

	private final SchedulerEnhancement schedulerEnhancement;

	@Override
	public void rippingLoop() {
		startupDevice();
		setupEnvironment();
		do {
			dumpLastLoopLogcat();
			loopStartTime = LOGCAT_TIME_FORMAT.format(new Date());
			readyToLoop();

			Task taskJustDone = null;
			while (true) {
				currState = getCurrentDescriptionAsState();
				if (states.isEmpty()) {						//the beginning of everything
					currState.setUid(State.DUMMY_UID);
					states.put(currState, currState.getUid());
					lastSavedState = currState;
					schedulerEnhancement.addTasks(planner.plan(taskJustDone, currState, WhatAPlanner.SPAN));
				} else {
					if (currState.equals(prevState))		//just to save time
						currState.setUid(prevState.getUid());
					else if (states.containsKey(currState))	//already occurred state
						currState.setUid(states.get(currState));
					else {									//brand new state
						currState.setUid(increaseUid(lastSavedState.getUid()));
						states.put(currState, currState.getUid());
						lastSavedState = currState;
						schedulerEnhancement.addTasks(planner.plan(taskJustDone, currState));
					}
					if (prevState != null)	//normal transition
						transitions.add(TransitionHelper.v(prevState, currState, taskJustDone));
					else					//null prevState and nonempty states set mean accident
						schedulerEnhancement.needRecovery();
				}
				ListIterator<IEvent> taskTodo = schedulerEnhancement.nextTaskIterator();
				if (taskTodo == null || !taskTodo.hasNext())
					break;
				executeTask(taskTodo);
				taskJustDone = iterToTask(taskTodo);
				prevState = currState;
			}

			endLoop();
		} while (running && !checkTerminationCriteria());

		notifyRipperEnded();
	}

	public AARTDriver(RipperInput ripperInput, RipperOutput ripperOutput) {
		this.ripperInput = ripperInput;
		this.ripperOutput = ripperOutput;

		YetAnotherBreadthScheduler yabs = new YetAnotherBreadthScheduler();
		addTerminationCriterion(yabs);
		this.schedulerEnhancement = yabs;

		this.planner = new WhatAPlanner();
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
				message = rsSocket.readMessage(1000, false);
			} catch (SocketException e) {
				continue;
			}
			if (message != null)
				return message;
		}
		throw new RipperNullMsgException(AARTDriver.class, caller() + "->" + here(), "readMessage()");
	}

	public void executeEvent(Event event) {
		if (event == null || StringUtils.isEmpty(event.getInteraction()))
			throw new RipperIllegalArgException(AARTDriver.class, here(), "event", String.valueOf(event));
		event.setEventUID(eventsUIDCounter++);
		notifyRipperLog("executeEvent.event=" + event.toString());
		if (event.getInputs() != null)
			rsSocket.sendInputs(Long.toString(event.getEventUID()), event.getInputs());
		else
			rsSocket.sendEvent(event);
		waitAck();//FIXME 同步和验证
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
			//TODO LOW build State here
			return (State) ripperInput.inputActivityDescription(Optional.ofNullable(getCurrentDescription())
					.orElseThrow(() -> new RipperNullMsgException(AARTDriver.class, here(), "getCurrentDescription")));
		} catch (IOException e) {
			throw new RipperRuntimeException(AARTDriver.class, here(), e.getMessage(), e);
		}
	}

	private void setupEnvironment() {
		endRipperTask(false, false);
		if (INSTALL_FROM_SDCARD) {
			Actions.pushToSD(TEMP_PATH + "/aut.apk");
			Actions.pushToSD(TEMP_PATH + "/ripper.apk");
		}
		if (device.isVirtualDevice())
			device.unlockDevice();

		//install and start service
		if (!Actions.checkApplicationInstalled(RIPPER_SERVICE_PKG))
			Actions.installAPK(SERVICE_APK_PATH);
		startService();
	}

	private void startService() {
		//TODO LOW 需要守护线程
		notifyRipperLog("Start ripper service...");
		Actions.startAndroidRipperService();
	}

	private void dumpLastLoopLogcat() {
		new LogcatDumper(device.getName(),
				LOGCAT_PATH + "logcat_" + device.getName() + "_" + LOGCAT_FILE_NUMBER++ + ".txt",
				loopStartTime).start();
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
			TransitionInfo transitionInfo = (TransitionInfo) transitions.last();
			if (back) {	//have to go back to pivot
				nextTask = transitionInfo.learnToBack(transitions	//learn from former transitions...
//								.parallelStream()		//...between previous state and current state...
//								.filter(t -> t.getFromState().equals(prevState) && t.getToState().equals(currState))
//								.collect(Collectors.toList())
				).backRecommend();	//...then make recommend
				back = false;
			} else {	//at pivot, or need to recover to pivot
				if (pivot == null)
					pivot = Pair.of(currState, schedule.get(currState));
				ListIterator<Task> iter = pivot.getValue().getIterator();
				if (currState == pivot.getKey()) {	//current state should be pivot state if back as expected
					recovery = false;                //if not, null would be returned
					transitionInfo.goodFeedback();   //last back was done great
					if (iter.hasNext()) {			//current pivot has remaining task to execute
						Task t = iter.next();
						nextTask = t.listIterator(t.size() - 1);
						back = true;		//next task would be back task
					} else {				//pivot has been completely searched...
						pathToPivot = assignNextPivot();//...so choose a "downstream" state as the new pivot
						nextTask = pathToPivot.listIterator(pathToPivot.size() - 1);
					}
				} else if (recovery) {	//need recovery...
					transitionInfo.poorFeedback();	//...because last back event is not the right one
					nextTask = pathToPivot.listIterator(0);
				}
			}
			return nextTask;
		}

		@Override
		public Task nextTask() {
			return iterToTask(nextTaskIterator());
		}

		@Override
		public void addTask(Task t) {

		}

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
		public void clear() {

		}

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
					.filter(t -> t.getFromState().equals(prevPivot) && !pivoted.contains(t.getToState().getUid()))
					.map(Transition::getToState).distinct()
					.forEachOrdered(bfsDeque::push);//enqueue bfs target, possibly no state enqueue

			Task task = new Task();
			if (!bfsDeque.isEmpty()) {
				State newPivot = bfsDeque.removeLast();
				this.pivot = Pair.of(newPivot, schedule.get(newPivot));

				//find the latest transition from previous pivot to new pivot
				Transition lastTransitionToNewPivot = null;
				for (Transition t : transitions)
					if (t.getFromState().equals(prevPivot) && t.getToState().equals(newPivot))
						lastTransitionToNewPivot = t;
				task = Objects.requireNonNull(lastTransitionToNewPivot).getTask();//shouldn't be null
			}
			return task;
		}

		@Override
		public void init(AbstractDriver driver) {
			//do nothing
		}

		@Override
		public boolean check() {
			return bfsDeque.isEmpty() && pathToPivot != null;
		}

		@Override
		public void needRecovery() {
			recovery = true;
		}
	}
}

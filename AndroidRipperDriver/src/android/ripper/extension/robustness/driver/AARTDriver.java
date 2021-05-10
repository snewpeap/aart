package android.ripper.extension.robustness.driver;

import android.ripper.extension.robustness.exception.RipperIllegalArgException;
import android.ripper.extension.robustness.exception.RipperNullMsgException;
import android.ripper.extension.robustness.model.State;
import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.model.TransitionHelper;
import android.ripper.extension.robustness.model.TransitionHelper.TransitionInfo;
import android.ripper.extension.robustness.model.VirtualWD;
import android.ripper.extension.robustness.output.InstrumentationTestSuiteGenerator;
import android.ripper.extension.robustness.output.TestSuiteGenerator;
import android.ripper.extension.robustness.planner.WhatAPlanner;
import android.ripper.extension.robustness.strategy.Coverage;
import android.ripper.extension.robustness.strategy.RealtimePerturb;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unina.android.ripper.driver.AbstractDriver;
import it.unina.android.ripper.driver.exception.RipperRuntimeException;
import it.unina.android.ripper.net.RipperServiceSocket;
import it.unina.android.ripper.termination.TerminationCriterion;
import it.unina.android.ripper.tools.actions.Actions;
import it.unina.android.ripper.tools.logcat.LogcatDumper;
import it.unina.android.shared.ripper.constants.SimpleType;
import it.unina.android.shared.ripper.input.RipperInput;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.task.TaskList;
import it.unina.android.shared.ripper.model.transition.Event;
import it.unina.android.shared.ripper.model.transition.IEvent;
import it.unina.android.shared.ripper.net.Message;
import it.unina.android.shared.ripper.output.RipperOutput;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static android.ripper.extension.robustness.model.State.EXIT_STATE;
import static android.ripper.extension.robustness.tools.MethodNameHelper.here;
import static it.unina.android.shared.ripper.model.transition.Event.IDLE_SIZE;

//import static android.ripper.extension.robustness.tools.MethodNameHelper.caller;

/**
 * Android Automated Robustness Test Driver
 *
 * @author YRD & LC
 */
public class AARTDriver extends AbstractDriver {
    private static final String RIPPER_SERVICE_PKG = "it.unina.android.ripper_service";
    private static final FastDateFormat LOGCAT_TIME_FORMAT = FastDateFormat.getInstance("MM-dd hh:mm:ss.mmm",
            TimeZone.getTimeZone(Actions.getDeviceTimeZone()));
    private String loopStartTime = LOGCAT_TIME_FORMAT.format(0);

    private final HashMap<State, State> states = new HashMap<>();
    private final HashMap<Transition, Transition> transitions = new HashMap<>();
    private State prevState = null;
    private State lastSavedState = null;
    private State currState;
    private Transition lastTransition;
    private int idle;
    private boolean backgroundProgressing = false;

    private final SchedulerEnhancement yabScheduler;
    private final boolean generateTestSuite, doExplore, doTest;
    private final String coverage, perturb;

    @Override
    public void rippingLoop() {
        startupDevice();
        setupEnvironment();
        TestSuiteGenerator testSuiteGenerator = new ARDrivenTestSuiteGenerator(coverage, perturb);
        do {
            if (!doExplore) {
                break;
            }
            readyToLoop();
            backgroundProgressing = false;
            Task taskJustDone = null;
            while (true) {
                boolean noTaskJustDone = (taskJustDone == null) || taskJustDone.isEmpty();
//				new Scanner(System.in).nextLine();
                currState = getCurrentDescriptionAsState();
                if (states.isEmpty()) {                    //the beginning of everything
                    currState.setUid(State.LOWEST_UID);
                    addNewState();
                    yabScheduler.addTasks(planner.plan(taskJustDone, currState, WhatAPlanner.SPAN));
                } else {
                    if (EXIT_STATE.equals(currState)) {
                        Transition transition = TransitionHelper.v(prevState, currState, taskJustDone);
                        if ((lastTransition = transitions.putIfAbsent(transition, transition)) == null) {
                            lastTransition = transition;
                        }
                        notifyRipperLog("Accidentally exit AUT");
                        break;
                    } else if (states.containsKey(currState)) {    //already occurred state
                        currState = states.get(currState);
                    } else {                                    //brand new state
                        currState.setUid(increaseUid(lastSavedState.getUid()));
                        addNewState();
                        yabScheduler.addTasks(planner.plan(taskJustDone, currState));
                    }

                    if (prevState != null && !noTaskJustDone) {
                        Transition transition = TransitionHelper.v(prevState, currState, taskJustDone);
                        if ((lastTransition = transitions.putIfAbsent(transition, transition)) == null) {
                            lastTransition = transition;
                        }
                    } else                    //null prevState and nonempty states set mean accident
                        yabScheduler.needRecovery();
                }

                if (noTaskJustDone) {
                    currState.updateIdle(idle);
                } else {
                    ((Event) taskJustDone.getLast()).updateIdle(idle);
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
        } while (running && !checkTerminationCriteria());

        if (doTest) {
            HashSet<Transition> set = new HashSet<>(transitions.values());
            if (!doExplore) {
                ObjectMapper objectMapper = new ObjectMapper();
                BufferedReader bufferedReader;
                try {
                    bufferedReader = Files.newBufferedReader(Paths.get(RESULTS_PATH, "..", "transitions.json"));
                    set = objectMapper.readValue(bufferedReader, new TypeReference<HashSet<Transition>>() {
                    });
                    InstrumentationTestSuiteGenerator.reMark(set);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            testSuiteGenerator.generate(set);
        }
        notifyRipperEnded();
    }

    public AARTDriver(RipperInput ripperInput, RipperOutput ripperOutput, boolean generateTestsuite,
                      String coverage, String perturb, String doExplore, String doTest) {
        this.ripperInput = ripperInput;
        this.ripperOutput = ripperOutput;

        YetAnotherBreadthScheduler yabs = new YetAnotherBreadthScheduler();
        addTerminationCriterion(yabs);
        this.yabScheduler = yabs;
        this.planner = new WhatAPlanner();
//		addTerminationCriterion(new CheckTimeTerminationCriterion());
        this.generateTestSuite = generateTestsuite;
        this.doExplore = Boolean.parseBoolean(doExplore);
        this.doTest = Boolean.parseBoolean(doTest);
        this.coverage = coverage;
        this.perturb = perturb;
    }

    @Override
    public void startupDevice() {
        if (device.isStarted())
            return;
        device.start();
        device.waitForDevice();
        device.setStarted(true);
    }

    @Override
    public Message waitAck() {
        Message message;
        for (int retry = 0; retry < ACK_MAX_RETRY; retry++) {
            try {
                if ((message = rsSocket.readMessage(1000, false)) != null)
                    return message;
            } catch (SocketException ignored) {
            }
        }
        return null;
//		throw new RipperNullMsgException(AARTDriver.class, caller() + "->" + here(), "readMessage()");
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
        } else {
            rsSocket.sendEvent(event);
            waitAck();
        }
    }

    private void executeTask(Iterator<IEvent> taskIter) {
        while (taskIter.hasNext()) {
            Event next = ((Event) taskIter.next());
            executeEvent(next);
            if (taskIter.hasNext()) {
                idle(next.getIdle());
            }
        }
    }

    private void idle(int ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms + 3000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Start ripper and AUT, check alive and then create log file.
     */
    private void readyToLoop() {
        new LogcatDumper(device.getName(),
                String.format("%slogcat_%s_%d.txt", LOGCAT_PATH, device.getName(), LOGCAT_FILE_NUMBER++),
                loopStartTime);
//				.start();
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

    @Override
    public boolean checkTerminationCriteria() {
        for (TerminationCriterion tc : terminationCriteria) {
            if (tc.check())
                return true;
        }
        return false;
    }

    public final State getCurrentDescriptionAsState() {
        State state = EXIT_STATE;
        idle = 0;
        for (int j = 0; j < 5; j++) {
            try {
                int i = 0;
                boolean showing = backgroundProgressing;
                while (i < 10) {
                    Thread.sleep(IDLE_SIZE);
                    if ((showing = Actions.progressingNotificationShowing(AUT_PACKAGE)) && !backgroundProgressing) {
                        notifyRipperLog("AUT is doing something in background, waiting...");
                    } else {
                        backgroundProgressing = false;
                        break;
                    }
                    i++;
                }
                backgroundProgressing = showing;
                idle += (i + 1) * IDLE_SIZE;

                String cd;
                try {
                    cd = getCurrentDescription();
                } catch (RuntimeException e) {
                    break;
                }
                state = new State(ripperInput.inputActivityDescription(Optional.ofNullable(cd)
                        .orElseThrow(() -> new RipperNullMsgException(AARTDriver.class, here(), "getCurrentDescription"))));
                if (state.getWidgets().parallelStream().anyMatch(w -> w.getClassName().contains("ProgressBar") ||
                        w.getSimpleType().equals(SimpleType.PROGRESS))) {
                    Thread.sleep(IDLE_SIZE);
                    idle += IDLE_SIZE;
                } else {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                throw new RipperRuntimeException(AARTDriver.class, here(), e.getMessage(), e);
            }
        }
        return state;
    }

    private void setupEnvironment() {
        Actions.turnoffAnimation();
        Actions.showTouches();
        endRipperTask(false, false);
        if (INSTALL_FROM_SDCARD) {
            Actions.pushToSD(Paths.get(TEMP_PATH, "aut.apk").toAbsolutePath().toString());
            Actions.pushToSD(Paths.get(TEMP_PATH, "ripper.apk").toAbsolutePath().toString());
        }
//		if (device.isVirtualDevice())
//			device.unlockDevice();

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
        private Pair<State, TaskList> pivot = null;    //current BFS pivot state and its taskList
        private final Deque<State> bfsDeque = new ArrayDeque<>();
        private final Set<String> pivoted = new HashSet<>();//uid of states that used to be pivot
        private Task pathToPivot = null;
        private static final int MAX_FAIL = 1;
        private int fail = MAX_FAIL;

        public YetAnotherBreadthScheduler() {
            pivoted.add("0");
        }

        @Override
        public ListIterator<IEvent> nextTaskIterator() {
            ListIterator<IEvent> nextTask = null;
            if (back) {    //have to go back to pivot
                if (currState.equals(pivot.getKey())) {
                    notifyRipperLog("A loop at pivot, continue...");
                    ListIterator<Task> iter = pivot.getValue().getIterator();
                    nextTask = iter.hasNext() ? nextTaskAtPivot(iter) : newPivotAsNextTask();
                } else {
                    TransitionInfo transitionInfo = (TransitionInfo) lastTransition;
                    //learn from former transitions then make recommend
                    nextTask = transitionInfo.learnToBack(transitions.values()).backRecommend();
                    notifyRipperLog("Going back...");
                    back = false;
                }
            } else {    //at pivot, or need to recover to pivot
                if (pivot == null) {
                    pivot = Pair.of(currState, schedule.get(currState));
                    ListIterator<Task> iter = pivot.getValue().getIterator();
                    if (iter.hasNext()) {
                        nextTask = nextTaskAtPivot(iter);
                    }
                } else {
                    TransitionInfo transitionInfo = (TransitionInfo) lastTransition;
                    if (currState.equals(pivot.getKey())) {    //current state should be pivot state if back as expected
                        recovery = false;                    //if not, null would be returned
                        notifyRipperLog("We're at pivot!");
                        fail = MAX_FAIL;
                        transitionInfo.goodFeedback();    //last back was done great
                        ListIterator<Task> iter = pivot.getValue().getIterator();
                        //hasNext() == true: current pivot has remaining task to execute
                        //else: pivot has been completely searched, so choose a "downstream" state as the new pivot
                        nextTask = iter.hasNext() ? nextTaskAtPivot(iter) : newPivotAsNextTask();
                    } else if (recovery) {                //need recovery...
                        notifyRipperLog("Recovering...");
                        transitionInfo.poorFeedback();    //...because last back event is not the right one
                        if (fail == 0) {
                            recovery = false;
                            nextTask = newPivotAsNextTask();
                        } else if (pathToPivot == null) {
                            recovery = false;
                            ListIterator<Task> iter = pivot.getValue().getIterator();
                            nextTask = iter.hasNext() ? nextTaskAtPivot(iter) : newPivotAsNextTask();
                        } else {
                            fail -= 1;
                            nextTask = pathToPivot.listIterator(0);
                        }
                    } else {
                        ListIterator<Task> iter = pivot.getValue().getIterator();
                        if (!iter.hasNext()) {
                            nextTask = newPivotAsNextTask();
                        } else {
                            Optional<Transition> transition;
                            if ((transition = transitions.values().stream()
                                    .filter(t -> currState.equals(t.getFromState()) &&
                                            pivot.getKey().equals(t.getToState())).min(Comparator.comparing(Transition::getId))).isPresent()) {
                                Task task = transition.get().getTask();
                                nextTask = task.listIterator(task.size() - 1);
                                recovery = true;
                                fail -= 1;
                            }
                        }
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
        public void addTask(Task t) {
        }

        @Override
        public void addTasks(TaskList taskList) {
            if (taskList != null && !taskList.isEmpty()) {
                notifyRipperLog(taskList.size() + " tasks scheduled for State just found.");
                schedule.putIfAbsent(currState, taskList);
            }
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
            State prevPivot = pivot.getKey();
            //enqueue bfs target, possibly no state enqueue
            transitions.values().stream()
                    .sorted(Comparator.comparing(t -> t.getToState().getUid(), Comparator.comparingInt(Integer::parseInt)))
                    .filter(t -> {
                        State to = t.getToState();
                        return !EXIT_STATE.equals(to) &&
                                !pivoted.contains(to.getUid()) &&
                                t.getFromState().equals(prevPivot) &&
                                schedule.containsKey(to);
                    }).map(Transition::getToState).distinct().forEachOrdered(e -> {
                pivoted.add(e.getUid());
                bfsDeque.push(e);
            });

            Task task = new Task();
            if (!bfsDeque.isEmpty()) {
                fail = MAX_FAIL;
                State newPivot = bfsDeque.removeLast();
                pivot = Pair.of(newPivot, schedule.get(newPivot));
                notifyRipperLog(String.format("Assign new pivot = State %s", newPivot.getUid()));
                //find the latest transition from previous pivot to new pivot
                Transition transitionToNewPivot = transitions.values().stream()
                        .filter(t -> t.getFromState().equals(currState) && t.getToState().equals(newPivot))
                        .min(Comparator.comparing(Transition::getId)).orElse(null);
                if (transitionToNewPivot == null) {
                    notifyRipperLog(String.format("No path found from this pivot(State %s) to new pivot(State %s)",
                            prevPivot.getUid(), newPivot.getUid()));
                    task = null;
                } else {
                    task = transitionToNewPivot.getTask();
                }
            } else {
                pivot = null;
            }
            return task;
        }

        private ListIterator<IEvent> newPivotAsNextTask() {
            Task currPathToPivot = assignNextPivot();
            if (pivot == null) {
                notifyRipperLog("No more new pivot");
                return null;
            }
            pathToPivot = (Task) schedule.get(pivot.getKey()).get(0).clone();
            pathToPivot.remove(pathToPivot.size() - 1);
            return currPathToPivot == null ?
                    pathToPivot.listIterator(pathToPivot.size()) :
                    currPathToPivot.listIterator(currPathToPivot.size() - 1);
        }

        private ListIterator<IEvent> nextTaskAtPivot(ListIterator<Task> iter) {
            back = true;
            pivot.getKey().reenter();
            Actions.screenShot(SCREENSHOT_OUTPUT_PATH + "State_" + pivot.getKey().getUid() + ".png");
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

    public class ARDrivenTestSuiteGenerator extends TestSuiteGenerator {
        private final Coverage coverage;
        private final ArrayList<RealtimePerturb> perturbs;
        ObjectMapper objectMapper;
        private final static String compareMapFormat = "%s expect:%s %s\n%s actual:%s %s\n";
        private final static String reportPath = "report/";
        private final ArrayList<Triple<Integer, String, String>> resultCollection = new ArrayList<>();
        private final int MAX = 2;
        private final int MEDIUM = 1;
        private final int MIN = 0;

        @Override
        public void generate(Set<Transition> transitions) {
            int reportId = 0;
            Collection<Transition> cherryPick = coverage.cherryPick(transitions);
            for (Transition transition : cherryPick) {
                List<Transition> pickAgain = cherryPick.stream().filter(t -> t.getFromState().equals(transition.getToState())).collect(Collectors.toList());
                for (Transition transition1 : pickAgain) {
                    List<Event> events = transition.getEvents();
                    for (RealtimePerturb perturb : perturbs) {
                        perturb.recover();
                        readyToLoop();
                        if (transition.getFromState().getIdle() > 0) {
                            idle(transition.getFromState().getIdle());
                        }
                        for (int i = 0, eventsSize = events.size(); i < eventsSize; i++) {
                            Event event = events.get(i);
                            event.setEventUID(eventsUIDCounter++);
                            notifyRipperLog("executeEvent.event=" + event);

                            if (event.getInputs() != null) {
                                rsSocket.sendInputs(Long.toString(event.getEventUID()), event.getInputs());
                                if (i == eventsSize - 1) {
                                    perturb.perturb();
                                }
                                for (int j = event.getInputs().size(); j > 0; j--) {
                                    waitAck();
                                }
                            } else {
                                rsSocket.sendEvent(event);
                                if (i == eventsSize - 1) {
                                    perturb.perturb();
                                }
                                waitAck();
                            }
                            if (event.getIdle() > 0) {
                                idle(event.getIdle());
                            }
                        }
                        //recover
                        perturb.recover();

                        State actual = getCurrentDescriptionAsState();
                        State expect = transition.getToState();
                        // need to add level
                        if (!expect.equals(actual)) {
                            int result = report(expect, actual, perturb.getClass().getName(), reportId++, MEDIUM, transition.getId() + "");
                            if (result != -1) {
                                resultCollection.add(new ImmutableTriple<>(result, "Transition id = " + transition.getId(), perturb.getClass().getName()));
                            }
                            continue;
                        } else {
                            Event event = ((Event) transition1.getTask().getLast());
                            event.setEventUID(eventsUIDCounter++);
                            notifyRipperLog("executeEvent.event=" + event);
                            if (event.getInputs() != null) {
                                rsSocket.sendInputs(Long.toString(event.getEventUID()), event.getInputs());
                                for (int j = event.getInputs().size(); j > 0; j--) {
                                    waitAck();
                                }
                            } else {
                                rsSocket.sendEvent(event);
                                waitAck();
                            }
                            if (event.getIdle() > 0) {
                                idle(event.getIdle());
                            }

                            actual = getCurrentDescriptionAsState();
                            expect = transition1.getToState();
                            if (!expect.equals(actual)) {
                                int result = report(expect, actual, perturb.getClass().getName(), reportId++, MAX, transition.getId() + " " + transition1.getId());
                                if (result != -1) {
                                    resultCollection.add(new ImmutableTriple<>(result, "Transition id = " + transition.getId(), perturb.getClass().getName()));
                                }
                            } else {
                                int result = report(expect, actual, perturb.getClass().getName(), reportId++, MIN, transition.getId() + " " + transition1.getId());
                                if (result != -1) {
                                    resultCollection.add(new ImmutableTriple<>(result, "Transition id = " + transition.getId(), perturb.getClass().getName()));
                                }
                            }
                        }

                        endLoop();


                    }
                }
            }
            for (Triple<Integer, String, String> triple : resultCollection) {
                System.err.println("report " + triple.getLeft() + " find in " + triple.getMiddle() + " " + triple.getRight());
            }
        }

        /**
         * A  B  C  b same c different
         *
         * @param expect
         * @param actual
         * @param perturbName
         * @param id
         * @return
         */
        public int report(State expect, State actual, String perturbName, int id, int level, String transitionId) {
            //TODO
            //1. assertEqual two String
            //2. print difference between getHierarchy
            //3. print all difference between two State
            //4. screenShot .
            StringBuilder stringBuilder = new StringBuilder();
            String expectString = null;
            String actualString = null;
            String[] levels = new String[]{"_min_", "_medium_", "_max_"};
            String level_ = levels[level];
            stringBuilder.append("******").append("REPORT IN TEST").append(id).append("    ").append(perturbName).append("******\n ");
            stringBuilder.append(transitionId).append("\n");
            boolean fail = false;
            try {
                expectString = objectMapper.writeValueAsString(expect);
                actualString = objectMapper.writeValueAsString(actual);
                fail = !expectString.equals(actualString);
            } catch (JacksonException e) {
            }
            try {
                if (fail) {
                    stringBuilder.append("difference between Hierarchy\n");
                    String resultOfCompareHierarchy = compareHierarchy(expect.getHierarchy(), actual.getHierarchy());
                    stringBuilder.append(resultOfCompareHierarchy);
                }
                stringBuilder.append("difference between State\n");
                String resultOfCompareState = compareStateAfterSerialization(expectString, actualString);
                stringBuilder.append(resultOfCompareState);
                Actions.screenShot(reportPath + AUT_PACKAGE + level_ + "screenShot_" + id + ".jpg");
                BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath + AUT_PACKAGE + level_ + "report_" + id + ".txt"));
                writer.write(stringBuilder.toString());
                writer.close();
                // if fail == True means need to Assert.fail()
                if (fail) {
                    System.err.println("ERROR! REPORT TO report_" + id);
                    return id;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
        }

        /**
         * if two hierarchy is difference, this method will return the difference on a format like this
         * "
         * expect:<key> <value> // default none
         * actual:<key> <value> // default none
         * “
         *
         * @param expect
         * @param actual
         * @return
         */
        public String compareHierarchy(HashMap<Integer, HashMap<String, VirtualWD>> expect, HashMap<Integer, HashMap<String, VirtualWD>> actual) throws JsonProcessingException {
            StringBuilder stringBuilder = new StringBuilder();
            HashSet<Integer> intersection = new HashSet<>(expect.keySet());
            intersection.retainAll(actual.keySet());

            HashSet<Integer> differenceKey = new HashSet<>(expect.keySet());
            differenceKey.removeAll(intersection);

            if (differenceKey.size() != 0) for (int o : differenceKey)
                stringBuilder.append(String.format(compareMapFormat, createSpace(0), o + "", objectMapper.writeValueAsString(expect.get(o)), createSpace(0), -1, "null"));

            differenceKey = new HashSet<>(actual.keySet());
            differenceKey.removeAll(intersection);
            if (differenceKey.size() != 0) for (int o : differenceKey)
                stringBuilder.append(String.format(compareMapFormat, createSpace(0), "-1", null, createSpace(0), o + "", objectMapper.writeValueAsString(actual.get(o))));

            if (intersection.size() != 0) {
                for (int o : intersection) {
                    stringBuilder.append("Key: ").append(o).append("\n");
                    stringBuilder.append(compareHierarchyVirtualWD(expect.get(o), actual.get(o)));
                }
            }
//            if (intersection.size() != 0) {
//                for (Object o : intersection) {
//                    Object o1 = m1.get(o);
//                    Object o2 = m2.get(o);
//                    if (o1 == null || o2 == null) {
//                        if (!(o1 == null && o2 == null))
//                            for (int i = 0; i < space; i++) stringBuilder.append(" ");
//                        stringBuilder.append(String.format(compareMapFormat, o.toString(), objectMapper.writeValueAsString(m1.get(o)), o, objectMapper.writeValueAsString(m2.get(o))));
//                    } else if (o1 instanceof Map && o2 instanceof Map)
//                        stringBuilder.append(compareMap((Map) o1, (Map) o2, space += 2));
//                    else if (!(o1 instanceof Map) && !(o2 instanceof Map) && !o1.equals(o2)) {
//                        for (int i = 0; i < space; i++) stringBuilder.append(" ");
//                        stringBuilder.append(String.format(compareMapFormat, o.toString(), objectMapper.writeValueAsString(m1.get(o)), o, objectMapper.writeValueAsString(m2.get(o))));
//                    }
//                }
//            }

            return stringBuilder.toString();
        }


        private String compareHierarchyVirtualWD(HashMap<String, VirtualWD> expect, HashMap<String, VirtualWD> actual) throws JsonProcessingException {
            StringBuilder stringBuilder = new StringBuilder();

            if (expect == null && actual == null) return "";

            if (expect == null || actual == null) {
                if (expect == null) {
                    stringBuilder.append(createSpace(4)).append("expect = null\n").append(createSpace(4)).append("actual = ").append(objectMapper.writeValueAsString(actual)).append("\n");
                }
                if (actual == null) {
                    stringBuilder.append(createSpace(4)).append("expect = ").append(objectMapper.writeValueAsString(expect)).append("\n").append(createSpace(4)).append("actual = null\n");
                }
                return stringBuilder.toString();
            }

            HashSet<String> intersection = new HashSet<>(expect.keySet());

            intersection.retainAll(actual.keySet());

            HashSet<String> differenceKey = new HashSet<>(expect.keySet());
            differenceKey.removeAll(intersection);

            if (differenceKey.size() != 0) for (String o : differenceKey)
                stringBuilder.append(String.format(compareMapFormat, createSpace(3), o, objectMapper.writeValueAsString(expect.get(o)), createSpace(3), -1, null));
//                stringBuilder.append(createSpace(4)).append("expect: ").append(o).append(createSpace(2)).append(objectMapper.writeValueAsString(expect.get(o))).append("\n").append(createSpace(4)).append("actual: -1 null\n");

            differenceKey = new HashSet<>(actual.keySet());
            differenceKey.removeAll(intersection);

            if (differenceKey.size() != 0) for (String o : differenceKey)
                stringBuilder.append(String.format(compareMapFormat, createSpace(3), -1, null, createSpace(3), o, objectMapper.writeValueAsString(actual.get(o))));
            //stringBuilder.append(createSpace(4)).append("expect: -1 null").append("\n").append(createSpace(4)).append("actual: ").append(o).append(createSpace(2)).append(objectMapper.writeValueAsString(actual.get(o))).append("\n");

            for (String s : intersection) {
                stringBuilder.append(compareVirtualWD(expect.get(s), actual.get(s)));
            }

            return stringBuilder.toString();
        }

        /**
         * String className,
         * Boolean isClickable,
         * Boolean isLongClickable,
         * Boolean enable,
         * Boolean visible,
         * Integer depth
         *
         * @param expect
         * @param actual
         * @return
         */
        private String compareVirtualWD(VirtualWD expect, VirtualWD actual) {
            StringBuilder stringBuilder = new StringBuilder();
            if (!expect.getClassName().equals(actual.getClassName()))
                stringBuilder.append(compareString("ClassName", expect.getClassName(), actual.getClassName()));
            if (!expect.getClickable() == actual.getClickable())
                stringBuilder.append(compareString("Clickable", expect.getClickable().toString(), actual.getClickable().toString()));
            if (!expect.getLongClickable() == actual.getLongClickable())
                stringBuilder.append(compareString("LongClickable", expect.getLongClickable().toString(), actual.getClickable().toString()));
            if (!expect.getEnabled() == actual.getEnabled())
                stringBuilder.append(compareString("Enable", expect.getEnabled().toString(), actual.getEnabled().toString()));
            if (!expect.getVisible() == actual.getVisible())
                stringBuilder.append(compareString("Visible", expect.getVisible().toString(), actual.getVisible().toString()));
            if (!expect.getDepth().equals(actual.getDepth()))
                stringBuilder.append(compareString("Depth", expect.getDepth().toString(), actual.getDepth().toString()));
            return stringBuilder.toString();
        }

        private String compareString(String name, String expect, String actual) {
            return createSpace(8) + name + ":\n" + createSpace(12) + "expect:" + expect + "\n" + createSpace(12) + "actual:" + actual + "\n";
        }

        private String createSpace(int space) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < space; i++) stringBuilder.append(" ");
            return stringBuilder.toString();
        }


        public String compareStateAfterSerialization(String s1, String s2) throws JsonProcessingException {
            return compareMap(objectMapper.readValue(s1, Map.class), objectMapper.readValue(s2, Map.class));
        }

        /**
         * m1 and m2 must have same type of key and value.
         *
         * @param m1
         * @param m2
         * @return
         */
        private String compareMap(Map m1, Map m2) throws JsonProcessingException {
            boolean iteration = false;
            StringBuilder stringBuilder = new StringBuilder();
            HashSet intersection = new HashSet(m1.keySet());
            intersection.retainAll(m2.keySet());
            HashSet differenceKey = new HashSet(m1.keySet());
            differenceKey.removeAll(intersection);
            if (differenceKey.size() != 0) {
                for (Object o : differenceKey)
                    stringBuilder.append(String.format(compareMapFormat, createSpace(3), o.toString(), objectMapper.writeValueAsString(m1.get(o)), createSpace(3), -1, "null"));
            }
            differenceKey = new HashSet<>(m2.keySet());
            differenceKey.removeAll(intersection);
            if (differenceKey.size() != 0) {
                for (Object o : differenceKey)
                    stringBuilder.append(String.format(compareMapFormat, createSpace(3), o.toString(), objectMapper.writeValueAsString(m2.get(o)), createSpace(3), -1, "null"));
            }
            if (intersection.size() != 0) {
                for (Object o : intersection) {
                    Object o1 = m1.get(o);
                    Object o2 = m2.get(o);
                    //TODO add WidgetCompare
                    if (o1 == null && o2 == null) {
                        continue;
                    }
                    if (o1 == null || o2 == null) {
                        stringBuilder.append(String.format(compareMapFormat, createSpace(3), o.toString(), objectMapper.writeValueAsString(m1.get(o)), createSpace(3), o, objectMapper.writeValueAsString(m2.get(o))));
                    } else if (o1 instanceof Map && o2 instanceof Map)
                        stringBuilder.append(compareMap((Map) o1, (Map) o2));
                    else if (!(o1 instanceof Map) && !(o2 instanceof Map) && !o1.equals(o2)) {
                        stringBuilder.append(String.format(compareMapFormat, createSpace(3), o.toString(), objectMapper.writeValueAsString(m1.get(o)), createSpace(3), o, objectMapper.writeValueAsString(m2.get(o))));
                    }
                }
            }

            return stringBuilder.toString();
        }

        public ARDrivenTestSuiteGenerator(String coverage, String perturb) {
            this.coverage = Coverage.of(coverage);
            String[] split = perturb.split(",");
            this.perturbs = new ArrayList<>(split.length);
            for (String s : split) {
                perturbs.add(RealtimePerturb.of(s));
            }
            objectMapper = new ObjectMapper();
            File dir = new File(reportPath);
            if (!dir.exists())
                dir.mkdirs();
        }
    }
}

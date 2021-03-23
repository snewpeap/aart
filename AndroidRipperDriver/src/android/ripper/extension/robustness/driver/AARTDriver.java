package android.ripper.extension.robustness.driver;

import android.ripper.extension.robustness.exception.NullMsgException;
import android.ripper.extension.robustness.comparator.model.State;
import android.ripper.extension.robustness.comparator.model.Transition;
import it.unina.android.ripper.comparator.IComparator;
import it.unina.android.ripper.driver.AbstractDriver;
import it.unina.android.ripper.driver.exception.AckNotReceivedException;
import it.unina.android.ripper.driver.exception.NullMessageReceivedException;
import it.unina.android.ripper.driver.exception.RipperRuntimeException;
import it.unina.android.ripper.driver.systematic.SystematicDriver;
import it.unina.android.ripper.net.RipperServiceSocket;
import it.unina.android.ripper.tools.actions.Actions;
import it.unina.android.ripper.tools.lib.AndroidTools;
import it.unina.android.ripper.tools.logcat.LogcatDumper;
import it.unina.android.shared.ripper.constants.InteractionType;
import it.unina.android.shared.ripper.model.state.ActivityDescription;
import it.unina.android.shared.ripper.model.state.WidgetDescription;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.transition.Event;
import it.unina.android.shared.ripper.model.transition.IEvent;
import it.unina.android.shared.ripper.net.Message;
import it.unina.android.shared.ripper.net.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static android.ripper.extension.robustness.tools.MethodNameHelper.here;

/**
 * Android Automated Robustness Test Driver
 *
 * @author YRD & LC
 */
public class AARTDriver extends AbstractDriver {
	private static final String RIPPER_SERVICE_PKG = "it.unina.android.ripper_service";
	private final SimpleDateFormat logcatTimeFormat = new SimpleDateFormat("MM-dd hh:mm:ss.mmm");
	private String loopStartTime = logcatTimeFormat.format(new Date(0));

	private IComparator comparator;
	private Set<State> states;
	private Set<Transition> transitions;

	@Override
	public void rippingLoop() {
		startupDevice();
		setupEnvironment();
		while (running) {
			dumpLastLoopLogcat();
			loopStartTime = logcatTimeFormat.format(new Date());
			startup(); //Start up AUT and ripper
			createLogFile();

			ActivityDescription ad =  getCurrentDescriptionAsActivityDescription();

			List<WidgetDescription> widgetDescriptionList = ad.getWidgets();
			widgetDescriptionList.stream().filter(widgetDescription -> widgetDescription.getValue().equals("START"));
			WidgetDescription wd = widgetDescriptionList.get(0);
			Event e = new Event();
			e.setWidget(wd);
			e.setInteraction(InteractionType.CLICK);
			Task t = new Task();
			t.add(e);
			Message m = executeTask(t);

			dealWithResult(m);

			Actions.sleepMilliSeconds(SLEEP_AFTER_EVENT);

			disturb();

			this.ifIsPausedDoPause();

			this.stopTestingTimeCounter();

			endLogFile();
		}
	}

	protected void disturb(){

		try{
			AndroidTools.adb("shell svc data disable");
		}catch (IOException exception){
			exception.printStackTrace();
		}

		Actions.sleepMilliSeconds(SLEEP_AFTER_EVENT);

		try{
			AndroidTools.adb("shell svc data enable");
		}catch (IOException exception){
			exception.printStackTrace();
		}

	}


	protected void dealWithResult(Message msg){
		//handle execution result
		if (msg == null || !running) {
			// do nothing
			notifyRipperLog("msg == null || running == false");
		} else {

			if (msg != null && msg.isTypeOf(MessageType.ACK_MESSAGE)) {

				nTasks++;
				nEvents++;
				notifyRipperLog("#Events = " + nEvents);

			} else if ((msg != null && msg.isTypeOf(MessageType.FAIL_MESSAGE))) {

				nFails++;
				this.appendLineToLogFile("\n<failure type=\"fail_message\" />\n");

			} else {

				notifyRipperLog("executeTask(): something went wrong?!?");
				this.appendLineToLogFile("\n<error type='executeTask' />\n");

			}

		}

	}

	protected Message executeTask(Task t) {
		Message msg = null;

		if (t != null && t.size() > 0) {

			IEvent evt = t.get(0);
			try {
				this.appendLineToLogFile(this.ripperOutput.outputFiredEvent(evt));
				msg = executeEvent(evt);
			} catch (AckNotReceivedException e1) {
				msg = null;
				notifyRipperLog("executeTask(): AckNotReceivedException"); // failure
				this.appendLineToLogFile("\n<error type='AckNotReceivedException' />\n");
			} catch (NullMessageReceivedException e2) {
				msg = null;
				notifyRipperLog("executeTask(): NullMessageReceivedException"); // failure
				this.appendLineToLogFile("\n<error type='NullMessageReceivedException' />\n");
				Actions.setRipperActive(false);
			}

			Actions.sleepMilliSeconds(SLEEP_AFTER_EVENT);

		}

		return msg;
	}

	@Override
	public void startupDevice() {
		//TODO 需要监控线程
		if (device.isStarted())
			return;
		device.start();
		device.waitForDevice(); //FIXME 方法没有反馈，加上返回值
		device.setStarted(true);
	}

	@Override
	public void startup() {
		checkSocket();

		super.startup();

		notifyRipperLog("Check alive...");
		try {
			boolean alive = rsSocket.isAlive() && Actions.checkCurrentForegroundActivityPackage(AUT_PACKAGE);
			if (!alive)
				throw new RipperRuntimeException(SystematicDriver.class, "rippingLoop", "Not alive!");
		} catch (SocketException ex) {
			throw new RipperRuntimeException(SystematicDriver.class, "rippingLoop", ex.getMessage(), ex);
		}
	}

	@Override
	public final ActivityDescription getCurrentDescriptionAsActivityDescription() {
		try {
			return ripperInput.inputActivityDescription(Optional.of(getCurrentDescription())
					.orElseThrow(() -> new NullMsgException(AARTDriver.class, here(), "getCurrentDescription()")));
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
		if (!Actions.checkApplicationInstalled(RIPPER_SERVICE_PKG)) {
			Actions.installAPK(SERVICE_APK_PATH);
		}
		startService();
	}

	private void startService() {
		//TODO 需要守护线程
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
			if (socket == null || socket.isClosed()) {
				rsSocket = new RipperServiceSocket(device.getIpAddress(), SERVICE_HOST_PORT);
			}
		}
	}
}

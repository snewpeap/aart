/**
 * GNU Affero General Public License, version 3
 *
 * Copyright (c) 2014-2017 REvERSE, REsEarch gRoup of Software Engineering @ the University of Naples Federico II, http://reverse.dieti.unina.it/
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/

package it.unina.android.ripper.driver;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import it.unina.android.ripper.driver.device.AbstractDevice;
import it.unina.android.ripper.driver.exception.AckNotReceivedException;
import it.unina.android.ripper.driver.exception.NullMessageReceivedException;
import it.unina.android.ripper.driver.exception.RipperRuntimeException;
import it.unina.android.ripper.net.RipperServiceSocket;
import it.unina.android.ripper.observer.RipperEventListener;
import it.unina.android.ripper.planner.Planner;
import it.unina.android.ripper.scheduler.Scheduler;
import it.unina.android.ripper.termination.TerminationCriterion;
import it.unina.android.ripper.tools.actions.Actions;
import it.unina.android.ripper.tools.logcat.LogcatDumper;
import it.unina.android.ripper.tools.strace.AdbStraceDumper;
import it.unina.android.ripper.tools.strace.StraceDumper;
import it.unina.android.ripper.tools.tcpdump.TcpdumpDumper;
import it.unina.android.shared.ripper.input.RipperInput;
import it.unina.android.shared.ripper.model.state.ActivityDescription;
import it.unina.android.shared.ripper.model.task.Task;
import it.unina.android.shared.ripper.model.task.TaskList;
import it.unina.android.shared.ripper.model.transition.Event;
import it.unina.android.shared.ripper.model.transition.IEvent;
import it.unina.android.shared.ripper.net.Message;
import it.unina.android.shared.ripper.net.MessageType;
import it.unina.android.shared.ripper.output.RipperOutput;

/**
 * Managers the Ripper Process
 *
 * @author Nicola Amatucci - REvERSE
 */
public abstract class AbstractDriver {

	/**
	 * TODO: parameter
	 */
	public static int SERVICE_HOST_PORT = 18888;

	/**
	 * DeviceInterface
	 */
	public AbstractDevice device = null;

	/**
	 * Package of the AUT
	 */
	public String AUT_PACKAGE = "";

	/**
	 * Model Output;
	 */
	public boolean MODEL_OUTPUT_ENABLE = false;

	/**
	 * Main Activity Class of the AUT
	 */
	public String AUT_MAIN_ACTIVITY = "";

	/**
	 * No reinstall
	 */
	public boolean NO_REINSTALL = false;

	/**
	 * Sleep time after each event
	 */
	public int SLEEP_AFTER_EVENT = 0;

	/**
	 * Sleep time after each task
	 */
	public int SLEEP_AFTER_TASK = 0;

	/**
	 * Sleep time after each restart
	 */
	public int SLEEP_AFTER_RESTART = 0;

	/**
	 * Sleep time before start ripping
	 */
	public int SLEEP_BEFORE_START_RIPPING = 0;

	/**
	 * Report file name
	 */
	public String REPORT_FILE = "report.xml";

	/**
	 * Prefix of the log file name
	 */
	public String LOG_FILE_PREFIX = "log_";

	/**
	 * PING message max retry number
	 */
	public int PING_MAX_RETRY = 10;

	/**
	 * ACK message max retry number
	 */
	public int ACK_MAX_RETRY = 3;

	/**
	 * Send message failure threshold
	 */
	public int FAILURE_THRESHOLD = 10;

	/**
	 * Restart threshold without events
	 */
	public int RESTART_THRESHOLD = 5;

	/**
	 * PING message failure threshold
	 */
	public int PING_FAILURE_THRESHOLD = 3;

	/**
	 * Socket Exception threshold
	 */
	public int SOCKET_EXCEPTION_THRESHOLD = 10;

	/**
	 * Storage path of logcat files
	 */
	public String LOGCAT_PATH = "";

	/**
	 * Storage path of xml files
	 */
	public String XML_OUTPUT_PATH = "";

	/**
	 * Scheduler instance
	 */
	public Scheduler scheduler;

	/**
	 * Planner instance
	 */
	public Planner planner;

	/**
	 * RipperServiceSocket instance
	 */
	public RipperServiceSocket rsSocket;

	/**
	 * RipperInput instance
	 */
	public RipperInput ripperInput;

	/**
	 * Termination Criteria
	 */
	public ArrayList<TerminationCriterion> terminationCriteria;

	/**
	 * Ripping Process Running Status
	 */
	public boolean running = true;

	/**
	 * Current log file name
	 */
	public String currentLogFile;

	/**
	 * RipperOutput instance
	 */
	public RipperOutput ripperOutput;

	/**
	 * Current Event Number
	 */
	public int nEvents = 0;

	/**
	 * Current Task Number
	 */
	public int nTasks = 0;

	/**
	 * Number of Failures
	 */
	public int nFails = 0;

	/**
	 * Number of Restarts
	 */
	public int nRestart = 0;

	/**
	 * Application APK
	 */
	public String AUT_APK = null;

	/**
	 * AndroidRipperService.apk PATH
	 */
	public String SERVICE_APK_PATH = null;

	/**
	 * Base AUT results path
	 */
	public String RESULTS_PATH = null;

	/**
	 * Temporary files path
	 */
	public String TEMP_PATH = null;

	/**
	 * Ripper started
	 */
	public boolean started = false;

	/**
	 * Install APK from SDCARD
	 */
	public boolean INSTALL_FROM_SDCARD = true;

	/**
	 * Storage path of tools
	 */
	public String TOOLS_PATH = null;

	/**
	 * Minutes to wait after install
	 */
	public int WAIT_AFTER_INSTALL = 0;

	/**
	 * Minutes to wait before install
	 */
	public int WAIT_BEFORE_INSTALL = 0;

	/**
	 * Number of Executed Events for Model UID
	 */
	public long eventsUIDCounter = 0;

	/**
	 * Finished Status
	 */
	public boolean finished = false;

	/**
	 * tcpdump tool thread holder
	 */
	TcpdumpDumper tcpDumper = null;

	/**
	 * strace tool thread holder
	 */
	StraceDumper straceDumper = null;

	/**
	 * Tcpdump enabled
	 */
	public boolean TCPDUMP_ENABLED = false;

	/**
	 * Strace enabled
	 */
	public boolean STRACE_ENABLED = false;

	/**
	 * Storage path of capture files
	 */
	public String CAPTURE_PATH = null;

	/**
	 * Exploration Watchdog
	 */
	public boolean EXPLORATION_WATCHDOG_ENABLED = false;

	/**
	 * Constructor
	 */
	public AbstractDriver() {
		super();
		terminationCriteria = new ArrayList<>();
	}

	/**
	 * Start Ripping Process
	 */
	public void startRipping() {
		this.running = true;
		this.finished = false;
		notifyRipperLog("Start Ripping Loop...");
		this.rippingLoop();
		notifyRipperLog("...Ripping Finished");
		this.finished = true;
		this.running = false;
	}

	/**
	 * Ripping Process Paused
	 */
	public boolean doPause = false;

	/**
	 * Pause Ripping Process
	 */
	public void pauseRipping() {
		this.doPause = true;
	}

	/**
	 * Resume Ripping Process
	 */
	public void resumeRipping() {
		this.doPause = false;
	}

	/**
	 * Stop Ripping Process
	 */
	public void stopRipping() {
		this.running = false;
	}

	/**
	 * Check if the Ripping Process is running
	 *
	 * @return if ripping process is running
	 */
	public boolean isRunning() {
		return this.running;
	}

	public boolean isPausing() {
		return this.doPause;
	}


	public boolean isPaused = false;

	/**
	 * Sleep if Ripping Process is paused
	 */
	public boolean ifIsPausedDoPause() {
		boolean ret = false;

		if (doPause) {
			isPaused = true;

			do {
				Actions.sleepMilliSeconds(500);
			} while (doPause);

			isPaused = false;
		}

		return ret;
	}

	/**
	 * Main Ripping Loop
	 */
	public abstract void rippingLoop();

	/**
	 * Return the Scheduler TaskList
	 *
	 * @return Task List
	 */
	public TaskList getTaskList() {
		return scheduler.getTaskList();
	}

	/**
	 * Ripper Process Observer
	 */
	RipperEventListener mRipperDriverListener = null;

	/**
	 * Set Ripper Process Observer
	 */
	public void setRipperEventListener(RipperEventListener l) {
		this.mRipperDriverListener = l;
	}

	/**
	 * Notify Ripper Process status to Observer
	 */
	public void notifyRipperStatus(String status) {
		if (mRipperDriverListener != null)
			this.mRipperDriverListener.ripperStatusUpdate(status);
	}

	/**
	 * Notify Ripper Process log entry to Observer
	 */
	public void notifyRipperLog(String log) {
		if (mRipperDriverListener != null)
			this.mRipperDriverListener.ripperLog(log);
	}

	/**
	 * Notify Ripper Process Task Endend to Observer
	 */
	public void notifyRipperTaskEnded() {
		if (mRipperDriverListener != null)
			this.mRipperDriverListener.ripperTaskEneded();
	}

	/**
	 * Notify Ripper Process Endend to Observer
	 */
	public void notifyRipperEnded() {
		if (mRipperDriverListener != null)
			this.mRipperDriverListener.ripperEneded();
	}

	/**
	 * Notify Ripper Process Paused to Observer
	 */
	public void notifyRipperPaused() {
		if (mRipperDriverListener != null)
			this.mRipperDriverListener.ripperPaused();
	}

	/**
	 * Write the final Report file
	 *
	 * @param report Report file name (with full path)
	 */
	public void writeReportFile(String report) {
		this.writeStringToFile(report, XML_OUTPUT_PATH + REPORT_FILE);
	}

	/**
	 * Write a string to a file (closes the file)
	 *
	 * @param string String to write
	 * @param file   File to write to
	 */
	public void writeStringToFile(String string, String file) {
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),
				StandardCharsets.UTF_8))) {
			out.write(string);
			out.flush();
		} catch (Exception ex) {
			notifyRipperLog(ex.getMessage());
		}
	}

	/**
	 * Write a string to a file (append to the file)
	 *
	 * @param string String to write
	 * @param file   File to write to
	 */
	public void appendStringToFile(String string, String file) {
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true),
				StandardCharsets.UTF_8))) {
			out.write(string);
			out.flush();
		} catch (Exception ex) {
			notifyRipperLog(ex.getMessage());
		}
	}

	/**
	 * Number of the current log file
	 */
	public int LOG_FILE_NUMBER = 0;

	/**
	 * Create an xml log file. The name is generated by using
	 * System.currentTimeMillis()
	 */
	public void createLogFileAtCurrentTimeMillis() {

		if (MODEL_OUTPUT_ENABLE) {
			currentLogFile = XML_OUTPUT_PATH + LOG_FILE_PREFIX + System.currentTimeMillis() + ".xml";
			this.doCreateLogFile();
		}
	}

	/**
	 * Create an xml log file. The name is generated by using LOG_FILE_NUMBER
	 * variable
	 */
	public void createLogFile() {
		if (MODEL_OUTPUT_ENABLE) {
			currentLogFile = XML_OUTPUT_PATH + LOG_FILE_PREFIX + LOG_FILE_NUMBER + ".xml";
			this.doCreateLogFile();
			LOG_FILE_NUMBER++;
		}
	}

	private void doCreateLogFile() {
		if (MODEL_OUTPUT_ENABLE) {
			writeLineToLogFile("<?xml version=\"1.1\"?><root>\n\r", false);
		}
	}

	/**
	 * Closes the xml log file
	 */
	public void endLogFile() {
		if (MODEL_OUTPUT_ENABLE) {
			writeLineToLogFile("\n\r</root>", true);
		}
	}

	/**
	 * Append a line to xml log file
	 *
	 * @param s line
	 */
	public void appendLineToLogFile(String s) {
		if (MODEL_OUTPUT_ENABLE) {
			writeLineToLogFile(s, true);
		}
	}

	private void writeLineToLogFile(String s, boolean append) {
		if (currentLogFile == null || currentLogFile.equals(""))
			return;

		try (FileWriter fileWriter = new FileWriter(currentLogFile, append)) {
			try (BufferedWriter bufferWriter = new BufferedWriter(fileWriter)) {
				bufferWriter.write(s);
				bufferWriter.flush();
			}
		} catch (Exception ex) {
			notifyRipperLog(ex.getMessage());
		}
	}

	/**
	 * Handle PING Request and Response
	 *
	 * @return If ping success
	 */
	public boolean ping() {
		int pingRetryCount = 0;

		try {
			do {
//				notifyRipperLog("Ping...");
				Message m = rsSocket.ping();

				if (m != null && m.getType().equals(MessageType.PONG_MESSAGE)) {
					return true;
				} else if (m != null && m.getType().equals(MessageType.WAIT_APP_READY)) {
					continue;
				} else if (m != null && !m.getType().equals(MessageType.PONG_MESSAGE)) {
					notifyRipperLog("Message != PONG -> " + m.getType());
				}

				if (!this.running)
					return false;

				if (pingRetryCount++ > PING_MAX_RETRY) {
					appendLineToLogFile("\n<failure type=\"ping\" />\n");
					return false;
				}

			} while (true);
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Wait for an ACK message
	 *
	 * @return Message
	 * @throws AckNotReceivedException
	 * @throws NullMessageReceivedException
	 */
	public Message waitAck() throws AckNotReceivedException, NullMessageReceivedException {
		Message msg = null;

		int retryCount = 0;

		do {

			try {
				notifyRipperLog("Wait ack (" + retryCount + ")...");
				msg = rsSocket.readMessage(1000, false);
			} catch (Exception ex) {
				// return null;
			}

			if (msg != null)
				break;

			if (!Actions.checkCurrentForegroundActivityPackage(AUT_PACKAGE)) {
				throw new AckNotReceivedException("waitAck() : check current foreground activity package");
			}

		} while (running && retryCount++ < ACK_MAX_RETRY);

		if (!running) {
			notifyRipperLog("running == false");
			return null;
		}

		if (retryCount > ACK_MAX_RETRY) {
			msg = Message.getAckMessage();
		}

		if (msg == null) {
			// notifyRipperLog("null message");
			throw new NullMessageReceivedException("waitAck() : null message received");
		}

		return msg;
	}

	/**
	 * Handle retrieval of the current ActivityDescription from the device
	 *
	 * @return Serialized ActivityDescription (XML)
	 * @throws IOException
	 */
	public String getCurrentDescription() throws IOException {
		// describe
		if (Actions.checkCurrentForegroundActivityPackage(AUT_PACKAGE)) {

			notifyRipperLog("Sending describe msg...");

			String xml = rsSocket.describe();

			if (xml != null) {
				xml = xml.replaceAll("&", "");
				notifyRipperLog("Description Received!");
			}

			return xml;
		} else {
			throw new RipperRuntimeException(AbstractDriver.class, "getCurrentDescription", "AUT not in foreground!");
		}
	}

	/**
	 * Update the ActivtyDescription currently stored in the
	 * lastActivityDescription variable.
	 * <p>
	 * Call getCurrentDescriptionAsActivityDescription()
	 *
	 * @throws IOException
	 */
	public void updateLatestDescriptionAsActivityDescription() throws IOException {
		this.getCurrentDescriptionAsActivityDescription();
	}

	/**
	 * Latest assigned ActivityDescription Unique ID
	 */
	int activityUID = 0;

	/**
	 * Update and Return the ActivtyDescription of the current Activity
	 * <p>
	 * Store the description in the lastActivityDescription variable.
	 *
	 * @return Current ActivityDescription
	 * @throws IOException
	 */
	public ActivityDescription getCurrentDescriptionAsActivityDescription() throws IOException {
		this.lastActivityDescription = null;

		String xml = this.getCurrentDescription();

		if (xml != null) {
			this.lastActivityDescription = ripperInput.inputActivityDescription(xml);
			if (lastActivityDescription != null) {
				this.lastActivityDescription.setUid(Integer.toString(++activityUID));
			}
		}
		return this.lastActivityDescription;
	}

	/**
	 * Latest retrieved ActivityDescription.
	 */
	public ActivityDescription lastActivityDescription = null;

	/**
	 * Return latest retrieved ActivityDescription.
	 *
	 * @return last activity description
	 */
	public ActivityDescription getLastActivityDescription() {
		return lastActivityDescription;
	}

	/**
	 * Current Logcat file number
	 */
	public int LOGCAT_FILE_NUMBER = 0;

	public void startupDevice() {

		// start the device
		device.start();
		device.waitForDevice();

		// starts adb logcat dumper
		new LogcatDumper(device.getName(),
				LOGCAT_PATH + "logcat_" + device.getName() + "_" + (LOGCAT_FILE_NUMBER) + ".txt").start();
		LOGCAT_FILE_NUMBER++;

		// uninstall packages
		endRipperTask(false, false);

		// install AndroidRipperService.apk
		if (Actions.checkApplicationInstalled("it.unina.android.ripper_service")) {
			Actions.uninstallAPK("it.unina.android.ripper_service");
		}
		Actions.installAPK(SERVICE_APK_PATH);

		this.uninstallAPKs(true); //FIXME 卸载两次，真有你的，，，

		// start ripper service
		notifyRipperLog("Start ripper service...");
		Actions.startAndroidRipperService();

		if (device.isVirtualDevice()) {
			// redirect port
			notifyRipperLog("Redir port...");
			Actions.redirectPort(SERVICE_HOST_PORT, SERVICE_HOST_PORT); // WHY？？？
		}

		// int screenStatus = Actions.checkDeviceScreen();
		// if (screenStatus == 1)
		// device.unlockDevice();
		if (device.isVirtualDevice())
			device.unlockDevice();

		if (INSTALL_FROM_SDCARD) {
			Actions.pushToSD(TEMP_PATH + "/aut.apk");
			Actions.pushToSD(TEMP_PATH + "/ripper.apk");
		}
	}

	public boolean APK_INSTALLED = false;

	/**
	 * StartUp of the Ripping Process
	 * 这个方法做了太多事情
	 *
	 * @return
	 */
	public void startup() {
		int pingFailures = 0;

		long startup_t1 = System.currentTimeMillis();

		long time = System.currentTimeMillis();

		if (TCPDUMP_ENABLED) {
			if (tcpDumper == null) {
				tcpDumper = new TcpdumpDumper(AUT_PACKAGE, CAPTURE_PATH, device, time);
				tcpDumper.start();
			}
		}

		if (STRACE_ENABLED) {
			straceDumper = new AdbStraceDumper(AUT_PACKAGE, CAPTURE_PATH, device, time);
			straceDumper.start();
		}

		if (!APK_INSTALLED) {
			// Install APK
			if (WAIT_BEFORE_INSTALL > 0) {
				notifyRipperLog("Waiting " + WAIT_BEFORE_INSTALL + " minutes");
				try {
					for (int i = 1; i < WAIT_BEFORE_INSTALL + 1; i++) {
						Thread.sleep(60000);
						// System.out.println("Waited "+i+" minutes");
						notifyRipperLog("WAIT_BEFORE_INSTALL: waited " + i + " minutes");
					}
				} catch (InterruptedException e) {
					// System.out.println(e.getMessage());
					notifyRipperLog("WAIT_BEFORE_INSTALL interrupted!");
				}
			}

			installAPKs();

			// post-install
			if (device.isVirtualDevice()) {
				notifyRipperLog("adb chmod 777...");
				Actions.adbShell("chmod", "777", "/data/data/" + AUT_PACKAGE + "/");//FIXME 路径有问题
			} else {
				notifyRipperLog("adb chmod 777...");
				Actions.adbSuShell("chmod", "777", "/data/data/" + AUT_PACKAGE + "/");
			}

			notifyRipperLog("Installation finished!");
			if (WAIT_AFTER_INSTALL > 0)
				notifyRipperLog("Waiting " + WAIT_AFTER_INSTALL + " minutes");
			try {
				for (int i = 1; i < WAIT_AFTER_INSTALL + 1; i++) {
					Thread.sleep(60000);
					notifyRipperLog("WAIT_AFTER_INSTALL: waited " + i + " minutes");
				}
			} catch (InterruptedException e) {
				notifyRipperLog("WAIT_AFTER_INSTALL interrupted!");
			}

			APK_INSTALLED = true;
		}

		String curPackage = Actions.getCurrentForegroundActivityPackage();
		if (!curPackage.endsWith("launcher") && !curPackage.endsWith("Launcher")) {
			notifyRipperLog("Killing " + curPackage + "...");
			Actions.killApp(curPackage);
		}

		Actions.setRipperActive(running);

		// start android ripper test case
		notifyRipperLog("Start ripper...");
		Actions.startAndroidRipper(AUT_PACKAGE);

		Actions.waitForForegroundActivityPackage(AUT_PACKAGE);

		Actions.sleepMilliSeconds(SLEEP_BEFORE_START_RIPPING);

		int socket_exception_count = 0;

		try {
			notifyRipperLog("Connect...");

			do {
				try {
					if (!rsSocket.isConnected()) {
						notifyRipperLog("Connecting to Android Ripper Service...");
						rsSocket.connect();
					} else {
						break;
					}
				} catch (Exception se) {
					socket_exception_count++;
				}
			} while (socket_exception_count <= SOCKET_EXCEPTION_THRESHOLD);
			if (socket_exception_count > SOCKET_EXCEPTION_THRESHOLD) {
				started = false;
				throw new RipperRuntimeException(AbstractDriver.class, "startup", "Android Ripper Service Connection Error: Socket Threshold Exceeded");
			}
			boolean ping;
			do {
				ping = this.ping();
				if (!ping)
					pingFailures++;

			} while (!ping && pingFailures <= PING_FAILURE_THRESHOLD);

			long startup_time = System.currentTimeMillis() - startup_t1;
			notifyRipperLog("Startup time: " + startup_time);

			// ready to go
			if (running && (pingFailures <= PING_FAILURE_THRESHOLD) // too many
				// pings,
				// need a
				// restart，但是为什么不用ping：boolean
			) {
				// TODO: this.notifyRipperStarted()，啥意思，暂且蒙在鼓里
				started = true;
			} else {
				throw new RipperRuntimeException(AbstractDriver.class, "startup", "Android Ripper Service Connection Error: Ping Threshold Exceeded");
			}

		} catch (Exception se) {
			throw new RipperRuntimeException(AbstractDriver.class, "startup", "Android Ripper Service Connection Error", se);
		}

		// started = true;
	}

	/**
	 * Kills the emulator
	 */
	public void shutdownDevice() {
		device.stop();
	}

	public boolean stopApp() {
		if (Actions.checkApplicationInstalled(AUT_PACKAGE)) {

			Actions.stopAndClearAPP(AUT_PACKAGE);

			return true;
		}

		return false;
	}

	public boolean endRipperTask(boolean end, boolean notify) {

		if (end && straceDumper != null) {
			straceDumper.stopProcess();
			straceDumper = null;
		}

		if (end && tcpDumper != null) {
			tcpDumper.pull();
			tcpDumper = null;
		}

		uninstallAPKs(end);

		if (notify) {
			this.notifyRipperTaskEnded();
		}

		started = false;
		return true;
	}

	public boolean uninstallAPKs(boolean end) {
		// uninstall APK
		if (Actions.checkApplicationInstalled(AUT_PACKAGE)) {
			notifyRipperLog("Uninstall AUT APK...");
			Actions.uninstallAPK(AUT_PACKAGE);
		}

		// uninstall RipperTestCase APK
		if (Actions.checkApplicationInstalled("it.unina.android.ripper")) {
			notifyRipperLog("Uninstall Android Ripper APK...");
			Actions.uninstallAPK("it.unina.android.ripper");
		}

		APK_INSTALLED = false;
		return true;
	}

	public void installAPKs() {
		if (!APK_INSTALLED) {
			if (!INSTALL_FROM_SDCARD) {
				if (!Actions.installAPK(TEMP_PATH + "/aut.apk")) {
					// throw new RuntimeException("Install AUT APK Fail!");
					throw new RipperRuntimeException(AbstractDriver.class, "startup", "Install AUT APK Fail!");
				}

				// Install RipperTestCase APK
				if (!Actions.installAPK(TEMP_PATH + "/ripper.apk")) {
					// throw new RuntimeException("Install Ripper APK Fail!");
					throw new RipperRuntimeException(AbstractDriver.class, "startup", "Install Ripper APK Fail!");
				}
			} else {
				Actions.installFromSD("aut.apk");
				Actions.installFromSD("ripper.apk");
			}

			APK_INSTALLED = true;
		}
	}

	/**
	 * Return Scheduler instance
	 *
	 * @return scheduler instance
	 */
	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Return Planner instance
	 *
	 * @return planner instance
	 */
	public Planner getPlanner() {
		return planner;
	}

	/**
	 * Return RipperOutput instance
	 *
	 * @return ripperOutput instance
	 */
	public RipperOutput getRipperOutput() {
		return ripperOutput;
	}

	/**
	 * Evaluate each termination criterion (AND Operator)
	 *
	 * @return should terminate
	 */
	public boolean checkTerminationCriteria() {
		for (TerminationCriterion tc : this.terminationCriteria) {
			if (!tc.check())
				return false;
		}

		return true;
	}

	/**
	 * Add a termination criterion
	 *
	 * @param tc TerminationCriterion to add
	 */
	public void addTerminationCriterion(TerminationCriterion tc) {
		tc.init(this);
		terminationCriteria.add(tc);
	}

	/**
	 * Use the Planner to plan tasks basing on an ActivityDescription
	 *
	 * @param t        Latest executed Task
	 * @param activity ActivityDescription
	 * @return task list
	 */
	public TaskList plan(Task t, ActivityDescription activity) {
		notifyRipperLog("Plan...");
		TaskList plannedTasks = planner.plan(t, activity);

		if (plannedTasks != null && plannedTasks.size() > 0) {
			notifyRipperLog("plannedTasks " + plannedTasks.size());
		} else {
			throw new RipperRuntimeException(AbstractDriver.class, "plan", "No Planned Tasks");
		}

		return plannedTasks;
	}

	/**
	 * Schedule next task using the Scheduler
	 *
	 * @return next task
	 */
	public Task schedule() {
		return scheduler.nextTask();
	}

	/**
	 * Execute an event and returns the message received after its execution or
	 * throws an exception if error
	 *
	 * @param evt Event
	 * @return Message
	 * @throws AckNotReceivedException
	 * @throws NullMessageReceivedException
	 */
	public Message executeEvent(IEvent evt) throws AckNotReceivedException, NullMessageReceivedException {

		if (evt != null) {

			if (evt instanceof Event) {

				((Event) evt).setEventUID(eventsUIDCounter++);

				notifyRipperLog("executeEvent.event:" + evt.toString());
				if (Actions.checkCurrentForegroundActivityPackage(AUT_PACKAGE)) {
					rsSocket.sendEvent((Event) evt);

					//if (Actions.checkCurrentForegroundActivityPackage(AUT_PACKAGE)) {
					return this.waitAck();
					//}
				}

			}

		}

		return null;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	long testingTime = 0;
	long testingTimeStart = 0;
	boolean testingTimeStarted = false;

	public long getTestingTime() {
		return testingTime;
	}

	public void resetTestingTime() {
		testingTime = 0;
		testingTimeStart = 0;
	}

	public void startTestingTimeCounter() {
		testingTimeStart = System.currentTimeMillis();
		testingTimeStarted = true;
	}

	public void stopTestingTimeCounter() {
		testingTime += System.currentTimeMillis() - testingTimeStart;
		testingTimeStarted = false;
	}

	public boolean getTestingTimeStarted() {
		return testingTimeStarted;
	}

	protected void handleEndOfLoop() {
		if (device.isStarted()) {

			this.endRipperTask(true, false);

			//this.uninstallAPKs(true);

			try {
				rsSocket.disconnect();
			} catch (Exception ex) {
				notifyRipperLog("[AbstractDriver.handleEndOfLoop()] " + "rsSocket.disconnect(): " + ex.getMessage());
			}

			shutdownDevice();

			notifyRipperLog("Wait process end...");
			Actions.waitForProcessToEnd(AUT_PACKAGE);
			notifyRipperLog("Wait test_case end...");

		}
	}

}

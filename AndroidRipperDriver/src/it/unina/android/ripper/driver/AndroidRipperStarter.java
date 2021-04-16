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

import it.unina.android.ripper.comparator.ActivityNameComparator;
import it.unina.android.ripper.comparator.ActivityStructureComparator;
import it.unina.android.ripper.comparator.IComparator;
import it.unina.android.ripper.comparator.WidgetPropertiesComparator;
import it.unina.android.ripper.driver.device.AndroidVirtualDevice;
import it.unina.android.ripper.driver.device.HardwareDevice;
import it.unina.android.ripper.driver.exception.RipperRuntimeException;
import it.unina.android.ripper.driver.exception.RipperUncaughtExceptionHandler;
import it.unina.android.ripper.driver.random.RandomDriver;
import it.unina.android.ripper.driver.systematic.SystematicDriver;
import it.unina.android.ripper.installer.OSSpecific;
import it.unina.android.ripper.installer.SearchableManifest;
import it.unina.android.ripper.installer.ZipUtils;
import it.unina.android.ripper.observer.RipperEventListener;
import it.unina.android.ripper.planner.Planner;
import it.unina.android.ripper.scheduler.BreadthScheduler;
import it.unina.android.ripper.scheduler.DepthScheduler;
import it.unina.android.ripper.scheduler.Scheduler;
import it.unina.android.ripper.scheduler.UniformRandomScheduler;
import it.unina.android.ripper.termination.EmptyActivityStateListTerminationCriterion;
import it.unina.android.ripper.termination.MaxEventsTerminationCriterion;
import it.unina.android.ripper.termination.TerminationCriterion;
import it.unina.android.ripper.termination.TestingTimeBasedTerminationCriterion;
import it.unina.android.ripper.tools.actions.Actions;
import it.unina.android.ripper.utils.RipperStringUtils;
import it.unina.android.shared.ripper.input.RipperInput;
import it.unina.android.shared.ripper.output.RipperOutput;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/**
 * Configure the AndroidRipperDriver, handle console output, start up the
 * ripping process
 *
 * @author Nicola Amatucci - REvERSE
 */
public class AndroidRipperStarter {

	public static String shell_CMD = OSSpecific.getShellCommand();

	/**
	 * Driver Type = RANDOM
	 */
	public final static String DRIVER_RANDOM = "random";

	/**
	 * Driver Type = SYSTEMATIC
	 */
	public final static String DRIVER_SYSTEMATIC = "systematic";

	/**
	 * Version
	 */
	public final static String VERSION = "2017.10";

	/**
	 * Configuration
	 */
	Properties conf;

	/**
	 * Driver Instance
	 */
	AbstractDriver driver;

	/**
	 * Configuration file name
	 */
	String configFile;

	/**
	 * Path to apk to test
	 */
	String apkToTest;

	/**
	 * RipperEventListener
	 */
	RipperEventListener eventListener;

	public RipperUncaughtExceptionHandler mRipperUncaughtExceptionHandler;

	/**
	 * Constructor
	 *
	 * @param apk        Apk to test
	 * @param configFile Configuration file name
	 */
	public AndroidRipperStarter(String apk,
								String configFile,
								RipperEventListener eventListener,
								RipperUncaughtExceptionHandler ripperUncaughtExceptionHandler) {
		super();

		//set UncaughtExceptionHandler
		mRipperUncaughtExceptionHandler = ripperUncaughtExceptionHandler;
//		Thread.currentThread().setUncaughtExceptionHandler(mRipperUncaughtExceptionHandler);

		String debugRipper = System.getenv("ANDROID_RIPPER_DEBUG");
		if (debugRipper != null && debugRipper.equals("1")) {
			mRipperUncaughtExceptionHandler.setPrintStackTrace(true);
		}

		if (!new File(configFile).exists()) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "AndroidRipperStarter", "File " + configFile + " not Found!");
		}

		if (!new File(apk).exists()) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "AndroidRipperStarter", "Apk " + apk + " not Found!");
		}

		this.configFile = configFile;
		this.apkToTest = apk;
		this.eventListener = eventListener;

		println("Loading configuration");
		conf = this.loadConfigurationFile(this.configFile);
	}

	public AndroidRipperStarter(String apkFile, String configFile, RipperEventListener eventListener, RipperUncaughtExceptionHandler ripperUncaughtExceptionHandler,
								HashMap<String, String> configurationOverride) {
		this(sanitizePath(apkFile), configFile,
				eventListener, ripperUncaughtExceptionHandler);

		for (String key : configurationOverride.keySet()) {
			String value = configurationOverride.get(key);

			if (key.equals("load")) {
				conf.put("load_manual_sequences", value);
			} else if (key.equals("store")) {
				conf.put("store_manual_sequences", value);
			} else {
				conf.put(key, value);
			}
		}
	}

	/**
	 * Starting staus. False if is started or not started
	 */
	boolean mIsStarting = false;

	public boolean isStarting() {
		return this.mIsStarting;
	}

	/**
	 * Configure and StartUp Ripping Process
	 */
	public void startRipping() {

		this.mIsStarting = true;

		validateEnvironment();

		Scheduler scheduler;
		Planner planner;
		RipperInput ripperInput;
		RipperOutput ripperOutput;
		TerminationCriterion terminationCriterion;
		IComparator comparator;

		if (conf != null) {
			String driverType = conf.getProperty("driver", "AART");

			switch (driverType) {
				case DRIVER_SYSTEMATIC:
					println("Systematic Ripper " + VERSION);
					break;
				case DRIVER_RANDOM:
					println("Random Ripper " + VERSION);
					break;
				case "AART":
					println("AART");
					break;
				default:
					throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "Driver Type not supported!");
			}

			String reportFile = "report.xml";
			String logFilePrefix = "log_";
			String avd_name_x86 = conf.getProperty("avd_name_x86", null);
			String avd_name_arm = conf.getProperty("avd_name_arm", null);
			String avd_port = conf.getProperty("avd_port", "5554");

			boolean model_output_enable = conf.getProperty("model", "0").equals("1");
			boolean planner_rotation_enable = conf.getProperty("planner.rotation", "0").equals("1");

			String ping_max_retry = conf.getProperty("socket.ping_max_retry", "10");
			String ack_max_retry = conf.getProperty("socket.ack_max_retry", "10");
			String failure_threshold = conf.getProperty("socket.failure_threshold", "10");
			String ping_failure_threshold = conf.getProperty("socket.ping_failure_threshold", "5");
			String sleep_after_task = conf.getProperty("sleep_after_task", "5");
			String sleep_after_event = conf.getProperty("sleep_after_event", "0");

			String target = conf.getProperty("target", "avd");
			String device = conf.getProperty("device", null);

			String wait_after_install = conf.getProperty("sleep_after_install", "0");
			String wait_before_install = conf.getProperty("sleep_before_install", "0");
			String wait_after_manual_sequence = conf.getProperty("sleep_after_manual_sequence", "0");

			if (target.equals("device") && (device == null || device.equals(""))) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "No Device SET!");
			}

			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			Date date = new Date();
			String apkName = apkToTest.substring(apkToTest.lastIndexOf('/') + 1, apkToTest.length() - 4);
			String subDirName = apkName + "/" + dateFormat.format(date);

			String base_result_dir;
			try {
				base_result_dir = new File(".").getCanonicalPath() + "/" + subDirName;
			} catch (Exception ex) {
				base_result_dir = "./" + subDirName;
			}

			if (!mkdirs(base_result_dir)){
				throw new RipperRuntimeException(AndroidRipperStarter.class,
						"startRipping",
						"Error create result directory");
			}

			// temp path
			String tempPath = base_result_dir + "/temp";
			String logcatPath = base_result_dir + "/logcat/";
			String xmlOutputPath = base_result_dir + "/model/";

			// installer parameters
			String aut_apk = apkToTest;
			String extractorClass = "SimpleNoValuesExtractor";

			// myPath是tools文件夹...那为啥不起名toolsPath...差评
			String myPath = sanitizePath(Paths.get("").toAbsolutePath() + "/tools/");

			String debugKeyStorePath = myPath + "/";// conf.getProperty("android_keystore_path", null);
			String testSuitePath = myPath + "/AndroidRipper/";// conf.getProperty("testsuite_path", null);
			String serviceApkPath = myPath + "/AndroidRipperService.apk";// conf.getProperty("service_apk_path", null);
			String toolsPath = myPath + "/";// conf.getProperty("tools_path", null);

			String straceEnabled = conf.getProperty("strace", "0");
			String tcpdumpEnabled = conf.getProperty("tcpdump", "0");
			boolean explorationWatchdogEnabled = conf.getProperty("exploration_watchdog", "0").equals("1");

			// validation
			if (target.equals("avd")) {
				if (avd_name_x86 == null) {
					throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "avd_name_x86 null!");
				}

//                if (avd_name_arm == null) {
//                    throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "avd_name_arm null!");
//                }
			}

			if (aut_apk == null) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "aut_apk null!");
			}

			if (!new File(aut_apk).exists()) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", aut_apk + " does not exist!");
			}

			// get apk infos
			String[] APKinfos = extractAPKInfos(aut_apk);

			String aut_package = conf.getProperty("aut_package", APKinfos[0]);
			String aut_main_activity = conf.getProperty("main_activity", APKinfos[1]);

			println("DETECTED aut_package = " + aut_package);
			println("DETECTED aut_main_activity = " + aut_main_activity);

			if (aut_package == null || aut_package.equals("")) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "aut_package null!");
			} else {
				mRipperUncaughtExceptionHandler.setPackageName(aut_package);

				//aut blacklist
				//此黑名单文件不存在于2017.10发布，令人好奇，什么App会上黑名单？
				if (new File("blacklist.txt").exists()) {
					String line;
					try (
							InputStream fis = new FileInputStream("blacklist.txt");
							InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
							BufferedReader br = new BufferedReader(isr)
					) {
						while ((line = br.readLine()) != null) {
							if (line.equalsIgnoreCase(aut_package)) {
								throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "App BlackListed!");
							}
						}
					} catch (IOException e1) {
						throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", e1.getMessage(), e1);
					}
				}
			}

			String sleep_before_start_ripping;
			if (aut_main_activity == null) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "aut_main_activity null!");
			} else {
				sleep_before_start_ripping = conf.getProperty("sleep_before_start_ripping", howMuchSleepINeed(aut_main_activity));
			}

			// check avd
			String avd_name = avd_name_x86;
			if (target.equals("avd")) {

				if (!checkAVD(avd_name_x86)) {
					throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "AVD X86 does not exist!");
				}

//				if (!checkAVD(avd_name_arm)) {
//					throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "AVD ARM does not exist!");
//				}

				try {
					if (ZipUtils.containsDirectory(aut_apk, "lib")) {
						if (ZipUtils.containsDirectory(aut_apk, "lib/x86")
								|| ZipUtils.containsDirectory(aut_apk, "lib/x86_64")) {
							avd_name = avd_name_x86;
						} else if (ZipUtils.containsDirectory(aut_apk, "lib/armeabi")
								|| ZipUtils.containsDirectory(aut_apk, "lib/armeabi-v7a")
								|| ZipUtils.containsDirectory(aut_apk, "lib/arm64-v8a")) {
							avd_name = avd_name_arm;
						} else {
							throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "APK problem inspecting libs: unknown architecture!");
						}
					}
				} catch (Exception ex) {
					throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "APK problem inspecting libs!");
				}
			}

			if (!new File(serviceApkPath).exists()) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", serviceApkPath + "/AndroidRipperService.apk does not exist!");
			}

			if (!new File(toolsPath).exists() || !new File(toolsPath).isDirectory()) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", toolsPath + " does not exist or is not a directory!");
			}

//			if (!new File(testSuitePath).exists() || !new File(testSuitePath).isDirectory()) {
//				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", testSuitePath + " does not exist or is not a directory!");
//			}

			if (!new File(debugKeyStorePath + "/debug.keystore").exists()) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", debugKeyStorePath + "/debug.keystore does not exist!");
			}

			String ANDROID_RIPPER_SERVICE_WAIT_SECONDS = "3";
			String ANDROID_RIPPER_WAIT_SECONDS = "3";

			// RANDOM CONFIGURATION PARAMETERS
			String legacyRandomNumEvents = conf.getProperty("events", "50");
			String randomNumEvents = conf.getProperty("random.events", legacyRandomNumEvents);
			println("Number of random events : " + randomNumEvents);
			String randomTime = conf.getProperty("random.time_sec", null);

			// seed
			String myDefaultSeed = Long.toString(System.currentTimeMillis());
			String legacyRandomseed = conf.getProperty("seed", myDefaultSeed);
			String randomSeed = conf.getProperty("random.seed", legacyRandomseed);
			println("Random Seed = " + randomSeed);

			try {
				Files.copy(Paths.get(configFile), Paths.get(base_result_dir + "/default.properties"));
			} catch (IOException e1) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", e1.getMessage(), e1);
			}

			String coverageFrequency = conf.getProperty("coverage.frequency", "100");
			String newLogFrequency = "100";
			String num_events_per_session = "0";

			//Planner Configuration
			Planner.CAN_CHANGE_ORIENTATION = planner_rotation_enable;
			planner = new it.unina.android.ripper.planner.HandlerBasedPlanner();

			ripperInput = new it.unina.android.shared.ripper.input.XMLRipperInput();
			ripperOutput = new it.unina.android.shared.ripper.output.XMLRipperOutput();

			for (String path : new String[]{tempPath, logcatPath, xmlOutputPath}) {
				mkdirs(path);
			}

			long seedLong = System.currentTimeMillis();
			if (!randomSeed.equals("")) {
				seedLong = Long.parseLong(randomSeed);
			}

			// create APKs
			println("Creating APKs...");
			createAPKs(testSuitePath, aut_package, aut_main_activity, extractorClass, toolsPath, debugKeyStorePath,
					aut_apk, tempPath);

			println("Starting Ripper...");

			String schedulerClass = conf.getProperty("scheduler",
					((driverType.equals(DRIVER_RANDOM)) ? "random" : "breadth"));
			switch (driverType) {
				case "AART":
					boolean generateTestsuite = Boolean.parseBoolean(conf.getProperty("AART.generate_testsuite"));
					String coverage = conf.getProperty("AART.coverage", "");
					String perturb = conf.getProperty("AART.perturb", "");
					try {
						Class<?> AARTDriverClass = Class.forName("android.ripper.extension.robustness.driver.AARTDriver");
						driver = (AbstractDriver) AARTDriverClass.getConstructor(RipperInput.class,
								RipperOutput.class,
								boolean.class,
								String.class,
								String.class,
								String.class).newInstance(ripperInput, ripperOutput, generateTestsuite, coverage, perturb, aut_main_activity);
						break;
					} catch (Exception e) {
						e.printStackTrace();
						println("Can't init AART driver instance, fallback to systematic driver.");
					}
				case DRIVER_SYSTEMATIC:
					if (schedulerClass != null && schedulerClass.equals("breadth")) {
						scheduler = new BreadthScheduler();
						println("Breadth First Scheduler");
					} else if (schedulerClass != null && schedulerClass.equals("depth")) {
						scheduler = new DepthScheduler();
						println("Depth First Scheduler");
					} else {
						throw new RipperRuntimeException(AndroidRipperStarter.class,
								"startRipping",
								"Scheduler not valid.");
					}
					terminationCriterion = new EmptyActivityStateListTerminationCriterion();

					String comparatorName = conf.getProperty("comparator", "activity-structure");
					if (comparatorName != null && comparatorName.equals("activity-name")) {
						comparator = new ActivityNameComparator();
					} else if (comparatorName != null && comparatorName.equals("widget-properties")) {
						comparator = new WidgetPropertiesComparator();
					} else {
						//default: "activity-structure"
						comparator = new ActivityStructureComparator();
					}

					driver = new SystematicDriver(scheduler, planner, ripperInput, comparator, terminationCriterion,
							ripperOutput);
//                terminationCriterion.init(driver);
					break;
				case DRIVER_RANDOM:
					scheduler = new UniformRandomScheduler(seedLong);
					if (randomTime == null) {
						terminationCriterion = new MaxEventsTerminationCriterion(Integer.parseInt(randomNumEvents));
					} else {
						terminationCriterion = new TestingTimeBasedTerminationCriterion(Long.parseLong(randomTime) * 1000);
					}

					try {
						System.out.println(base_result_dir + "/random-seed.txt");
						Files.write(Paths.get(base_result_dir + "/random-seed.txt"), randomSeed.getBytes());
					} catch (IOException e1) {
						throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", e1.getMessage(), e1);
					}

					driver = new RandomDriver(scheduler, planner, ripperInput, ripperOutput, terminationCriterion);
					((RandomDriver) driver).RANDOM_SEED = seedLong;
					((RandomDriver) driver).NUM_EVENTS = Integer.parseInt(randomNumEvents);
					((RandomDriver) driver).NUM_EVENTS_PER_SESSION = Integer.parseInt(num_events_per_session);
					((RandomDriver) driver).NEW_LOG_FREQUENCY = Integer.parseInt(newLogFrequency);
					((RandomDriver) driver).COVERAGE_FREQUENCY = Integer.parseInt(coverageFrequency);
					break;
				default:
					throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "Driver Type not supported!");
			}

			if (driver != null) {

				// installer
				driver.SERVICE_APK_PATH = serviceApkPath;
				driver.TEMP_PATH = tempPath;
				driver.RESULTS_PATH = base_result_dir;
				// apply common configuration parameters
				driver.REPORT_FILE = reportFile;
				driver.LOG_FILE_PREFIX = logFilePrefix;
				driver.AUT_PACKAGE = aut_package;
				System.out.println("TARGET_PACKAGE:" + aut_package);
				driver.AUT_MAIN_ACTIVITY = aut_main_activity;
				System.out.println("TARGET_CLASS:"+ aut_main_activity);
				driver.SLEEP_AFTER_TASK = Integer.parseInt(sleep_after_task);
				driver.SLEEP_AFTER_EVENT = Integer.parseInt(sleep_after_event);
				driver.PING_MAX_RETRY = Integer.parseInt(ping_max_retry);
				driver.ACK_MAX_RETRY = Integer.parseInt(ack_max_retry);
				driver.FAILURE_THRESHOLD = Integer.parseInt(failure_threshold);
				driver.PING_FAILURE_THRESHOLD = Integer.parseInt(ping_failure_threshold);
				driver.LOGCAT_PATH = logcatPath;
				driver.XML_OUTPUT_PATH = xmlOutputPath;
				driver.TOOLS_PATH = toolsPath;
				driver.WAIT_AFTER_INSTALL = Integer.parseInt(wait_after_install);
				driver.WAIT_BEFORE_INSTALL = Integer.parseInt(wait_before_install);

				driver.MODEL_OUTPUT_ENABLE = model_output_enable;

				driver.SLEEP_BEFORE_START_RIPPING = Integer.parseInt(sleep_before_start_ripping);

				driver.TCPDUMP_ENABLED = tcpdumpEnabled != null && tcpdumpEnabled.equals("1");
				driver.STRACE_ENABLED = straceEnabled != null && straceEnabled.equals("1");

				driver.EXPLORATION_WATCHDOG_ENABLED = explorationWatchdogEnabled;


				if (target.equals("device")) {
					driver.device = new HardwareDevice(device);
				} else { // target.equals("avd")
					driver.device = new AndroidVirtualDevice(avd_name, Integer.parseInt(avd_port));
				}

				Actions.ANDROID_RIPPER_SERVICE_WAIT_SECONDS = Integer.parseInt(ANDROID_RIPPER_SERVICE_WAIT_SECONDS);
				Actions.ANDROID_RIPPER_WAIT_SECONDS = Integer.parseInt(ANDROID_RIPPER_WAIT_SECONDS);

				driver.setRipperEventListener(eventListener);
				driver.startRipping();
				this.mIsStarting = false;

				while (driver.isRunning()) {
					// TODO: stop pause commands
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}
			}

		} else {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "startRipping", "Missing configuration file!");
		}

	}

	/**
	 * Load the configuration file into a Properties class instance
	 *
	 * @param fileName configuration file name
	 * @return properties
	 */
	private Properties loadConfigurationFile(String fileName) {
		Properties conf = new Properties();

		try {
			conf.load(new FileInputStream(fileName));
			return conf;
		} catch (IOException ex) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "loadConfigurationFile", "Unable to load cofngiruation file!", ex);
		}
	}

	/**
	 * Print a formatted debug line
	 *
	 * @param line line to print
	 */
	protected void println(String line) {
		if (eventListener != null) {
			eventListener.ripperLog(line);
		} else {
			System.out.println("[" + System.currentTimeMillis() + "] " + line);
		}
	}

	protected String[] getAppInfo(String sourcePath) {
		String[] ret = new String[2];

		String path = sourcePath + File.separator + "AndroidManifest.xml";
		SearchableManifest doc = new SearchableManifest(path);

		String thePackage = doc.parseXpath(MANIFEST_XPATH);
		String theClass = doc.parseXpath(CLASS_XPATH);

		String dot = (theClass.endsWith(".") || theClass.startsWith(".")) ? "" : ".";
		theClass = thePackage + dot + theClass;

		ret[0] = thePackage;
		ret[1] = theClass;

		return ret;
	}

	public final static String MANIFEST_XPATH = "//manifest[1]/@package";
	public final static String CLASS_XPATH = "//activity[intent-filter/action/@name='android.intent.action.MAIN'][1]/@name";

	protected void validateEnvironment() {
		for (String exe : new String[]{"java", "jarsigner", "zipalign", "apksigner.bat", "adb", "emulator"}) {
			if (!validateCommand(exe)) {
				throw new RipperRuntimeException(AndroidRipperStarter.class, "validateEnvironment", exe + " executable not in PATH!");
			}
		}
	}

	protected boolean validateCommand(String cmd) {
		try {
			Process proc;
			if(OSSpecific.isMac()||OSSpecific.isUnix())
			 	proc = Runtime.getRuntime().exec(shell_CMD + cmd);
			else
				proc = Runtime.getRuntime().exec(cmd);
			try {
				proc.destroy();
			} catch (Exception ignored) {
			}
			return true;
		} catch (IOException e1) {
			// e1.printStackTrace();
			return false;
		}
	}

	protected void createAPKs(String testSuitePath, String appPackage, String appMainActivity, String extractorClass,
							  String toolsPath, String debugKeyStorePath, String autAPK, String tempPath) {
		//repack
		println("Unpacking Ripper APK (using apktool)");
		Path unpackedRipperPath = Paths.get(tempPath, "unpacked");
		execCommand(String.format("java -jar %s --quiet d -o %s %s",
				Paths.get(toolsPath, "apktool.jar").toAbsolutePath(),
				unpackedRipperPath,
				Paths.get(toolsPath, "ar.apk")));
		// replace strings
		testSuitePath = unpackedRipperPath.toAbsolutePath().toString();
		println("Editing 'Configuration.java'");
		replaceStringsInFile(
//				testSuitePath + "/smali/it/unina/android/ripper/configuration/Configuration.smali.template",
				Paths.get(testSuitePath, "/smali/it/unina/android/ripper/configuration/Configuration.smali")
						.toAbsolutePath().toString(),
				appPackage, appMainActivity);
		println("Editing 'AndroidManifest.xml'");
		replaceStringsInFile(
//				testSuitePath + "/AndroidManifest.xml.template",
				Paths.get(testSuitePath, "/AndroidManifest.xml").toAbsolutePath().toString(),
				appPackage, appMainActivity);

		try {
			println("Repacking Ripper APK (using apktool)");
			execCommand(String.format("java -jar %sapktool.jar --quiet b %s -o %s/ar.apk", toolsPath, testSuitePath, tempPath));

			println("Repacked. Signing AndroidRipper...");

			//签名和对齐，ripper.apk是成品
//			execCommand("jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore " + debugKeyStorePath
//					+ "/debug.keystore -storepass android -keypass android " + tempPath + "/ar.apk androiddebugkey");
			execCommand("zipalign 4 " + tempPath + "/ar.apk " + tempPath + "/ripper.apk");
			execCommand(String.format("apksigner sign --ks %s/debug.keystore --ks-pass pass:android %s/ripper.apk", debugKeyStorePath, tempPath));

			Files.copy(FileSystems.getDefault().getPath(autAPK),
					FileSystems.getDefault().getPath(tempPath + "/temp.apk"),
					StandardCopyOption.REPLACE_EXISTING);

			println("Signing AUT...");
			ZipUtils.deleteFromZip(tempPath + "/temp.apk");//删除META-INF
//			execCommand("jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore " + debugKeyStorePath
//					+ "/debug.keystore -storepass android -keypass android " + tempPath
//					+ "/temp.apk androiddebugkey");
//			execCommand("jarsigner -verify " + tempPath + "/temp.apk");
			execCommand("zipalign 4 " + tempPath + "/temp.apk " + tempPath + "/aut.apk");
			execCommand(String.format("apksigner sign --ks %s/debug.keystore --ks-pass pass:android %s/aut.apk", debugKeyStorePath, tempPath));

		} catch (Exception t) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "createAPKs", "apk build failed", t);
		}
	}

	protected void replaceStringsInFile(String filePath, String appPackage, String appMainActivity) {
		Path templatePath = Paths.get(filePath + ".template");
		try {
			Files.copy(Paths.get(filePath), templatePath);
			replaceStringsInFile(templatePath.toAbsolutePath().toString(), filePath, appPackage, appMainActivity);
		} catch (IOException e) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "replaceStringsInFile", "", e);
		}
	}

	protected void replaceStringsInFile(String templateFilePath, String outputFilePath, String appPackage,
										String appMainActivity) {
		try {

			Path templatePath = Paths.get(templateFilePath);
			Path outPath = Paths.get(outputFilePath);
			Charset charset = StandardCharsets.UTF_8;

			String content = new String(Files.readAllBytes(templatePath), charset);
			appPackage = appPackage.replaceAll("\\$", "\\\\\\$");
			content = content.replaceAll("%%_PACKAGE_NAME_%%", appPackage);
			appMainActivity = appMainActivity.replaceAll("\\$", "\\\\\\$");
			content = content.replaceAll("%%_CLASS_NAME_%%", appMainActivity);

			Files.write(outPath, content.getBytes(charset));

		} catch (Exception ex) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "replaceStringsInFile", ex.getMessage(), ex);
		}
	}

	protected boolean checkAVD(String avdName) {
		try {
			Process proc = Runtime.getRuntime().exec(OSSpecific.getAndroidListAVDCommand());
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String s;
			while ((s = stdInput.readLine()) != null) {
				if (s.contains("Name: ")) {
					String name = s.substring(s.indexOf("Name: ") + 6).trim();

					if (name.equals(avdName)) {
						return true;
					}
				}
			}
		} catch (IOException e1) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "replaceStringsInFile", e1.getMessage(), e1);
		}

		return false;
	}

	public static void execCommand(String cmd) {
		execCommand(cmd, true);
	}

	public static void execCommand(String cmd, boolean wait) {
		try {
			final Process p = Runtime.getRuntime().exec(shell_CMD + cmd);

			Thread t = new Thread(() -> {
				try {
					String line;
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((line = input.readLine()) != null) {
						System.out.println(line);
					}
					input.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			t.start();
			p.waitFor();
		} catch (Exception ex) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "execCommand", ex.getMessage(), ex);
		}
	}

	public static String[] extractAPKInfos(String apk) {
		String[] ret = new String[2];
		ret[0] = null;
		ret[1] = null;

		try {
			final Process p;
			if(OSSpecific.isMac()){
				String c = "'aapt dump badging " + apk+"'";
				p = Runtime.getRuntime().exec("/bin/zsh -c "+c);
			}
			else p = Runtime.getRuntime().exec("aapt dump badging " + apk);

			String line;
			BufferedReader aaptReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String contentLine;
			while ((contentLine = aaptReader.readLine()) != null) {
				if (ret[0] == null && contentLine.startsWith("package:")) {
					int ind = contentLine.indexOf("name=") + "name='".length();
					ret[0] = contentLine.substring(ind, contentLine.indexOf('\'', ind + 1)).trim();
				}

				if (ret[1] == null && contentLine.startsWith("launchable-activity:")) {
					int ind = contentLine.indexOf("name=") + "name='".length();
					ret[1] = contentLine.substring(ind, contentLine.indexOf('\'', ind + 1)).trim();
				}

				if (ret[0] != null && ret[1] != null) {
					break;
				}
			}

			try {
				aaptReader.close();
			} catch (Throwable ignored) {

			}

			try {
				p.waitFor();
			} catch (Throwable ignored) {

			}

			if (ret[1] == null) {
				final Process p2;
				if(OSSpecific.isMac()){
					p2 = Runtime.getRuntime().exec(new String[]{"/bin/zsh","-c" ,"aapt dump xmltree " + apk + " AndroidManifest.xml"});
				}
				else p2 = Runtime.getRuntime().exec("aapt dump xmltree " + apk + " AndroidManifest.xml");
				BufferedReader reader = new BufferedReader(new InputStreamReader(p2.getInputStream()));
				String mainActivity = null;
				while ((line = reader.readLine()) != null) { //TODO LOW 用DOM会不会更好？此情况是否足够常见以值得优化
					if (line.contains("targetActivity")) {
						mainActivity = line.trim().split("\"")[1];
						System.out.println(mainActivity);
					} else if (line.contains("android.intent.category.LAUNCHER")) {
						break;
					}
				}

				try {
					reader.close();
				} catch (Throwable ignored) {

				}

				try {
					p2.waitFor();
				} catch (Throwable ignored) {

				}

				ret[1] = mainActivity;
			}

		} catch (Exception ex) {
			throw new RipperRuntimeException(AndroidRipperStarter.class, "extractAPKInfos", ex.getMessage(), ex);
		}

		return ret;
	}

	private boolean mkdirs(String path) {
		File dir = new File(path);
		if (!dir.exists())
			return dir.mkdirs();
		else return true;
	}

	private static String sanitizePath(String pathToSanitize) {
		String path = new File(pathToSanitize).getAbsolutePath();
		if (File.separatorChar != '/') {
			path = path.replace(File.separatorChar, '/');
		}

		return path;
	}

	public AbstractDriver getDriver() {
		return this.driver;
	}

	public String howMuchSleepINeed(String mainActivityClass) {
		String simpleClassName = mainActivityClass.substring(mainActivityClass.lastIndexOf('.') + 1).toLowerCase();
		//判断mainActivity是否为开屏界面，并且等待更多时长
		//目前还无法判断这种经验主义做法是否仍然有效
		//同时还有另一种开屏（如新版本介绍、使用介绍）问题需要解决 TODO LOW
		if (RipperStringUtils.stringContainsItemFromList(simpleClassName, new String[]{"splash", "welcome", "intro", "loading", "logo"})) {
			return "10000";
		}

		return "1000";
	}
}

package android.ripper.extension.test;

/*	Add package and import declarations. Try CTRL+SHIFT+O if you're using Eclipse.
//	Robotium must be in the build path!!! Otherwise, Solo cannot be resolved to a class.
//	Additionally, the targetPackage for Instrumentation in AndroidManifest.xml should
//	be set to the package of the application under test (that is PACKAGE_NAME below) */

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import com.robotium.solo.Solo;
import android.ripper.extension.robustness.model.State;
import android.ripper.extension.robustness.model.VirtualWD;
import android.ripper.extension.test.extractor.IExtractor;
import android.ripper.extension.test.extractor.ReflectionExtractor;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.*;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import junit.framework.Assert;
import android.test.ActivityInstrumentationTestCase2;
import org.apache.commons.lang3.StringEscapeUtils;
import androidx.test.uiautomator.UiDevice;
import static android.content.Context.WINDOW_SERVICE;
import static android.view.Surface.*;

@SuppressWarnings("rawtypes")
public class TestSuite extends ActivityInstrumentationTestCase2 {

    // Attributes
    private Activity theActivity;
    private Map<Integer, View> theViews;
    private ArrayList<View> allViews = new ArrayList<View>(); // A list of all widgets
    private Solo solo;
    private TabHost tabs;
    private int tabNum;

    public final static String CLASS_NAME = <CLASS_NAME>;
    private final static String TestTag = "TestSuite_" + CLASS_NAME;
    public final static int SLEEP_AFTER_EVENT = 1000;
    public final static int SLEEP_AFTER_RESTART = 1000;
    public final static int SLEEP_ON_THROBBER = 1000;
    public final static int SLEEP_AFTER_TASK = 1000;
    public final static boolean FORCE_RESTART = false;

    public final static boolean IN_AND_OUT_FOCUS = true;

    public final static String CLICK = "click";
    public final static String BACK = "back";
    public final static String SCROLL_DOWN = "scrollDown";
    public final static String SCROLL_UP = "scrollUp";
    public final static String SWAP_TAB = "swapTab";
    public final static String TYPE_TEXT = "editText";
    public final static String LIST_SELECT = "selectListItem";
    public final static String LONG_CLICK = "longClick";
    public final static String LIST_LONG_SELECT = "longClickListItem";
    public final static String OPEN_MENU = "openMenu";
    public final static String SET_BAR = "setBar";
    public final static String SPINNER_SELECT = "selectSpinnerItem";
    public final static String CHANGE_ORIENTATION = "changeOrientation";
    public final static String CLICK_ON_TEXT = "clickText";
    public final static String WRITE_TEXT = "writeText";
    public final static String PRESS_KEY = "pressKey";
    public final static String RADIO_SELECT = "selectRadioItem";
    public final static String ACCELEROMETER_SENSOR_EVENT = "accelerometerSensorEvent";
    public final static String ORIENTATION_SENSOR_EVENT = "orientationSensorEvent";
    public final static String MAGNETIC_FIELD_SENSOR_EVENT = "magneticFieldSensorEvent";
    public final static String TEMPERATURE_SENSOR_EVENT = "temperatureSensorEvent";
    public final static String AMBIENT_TEMPERATURE_SENSOR_EVENT = "ambientTemperatureSensorEvent";
    public final static String GPS_LOCATION_CHANGE_EVENT = "gpsLocationChangeEvent";
    public final static String GPS_PROVIDER_DISABLE_EVENT = "gpsProviderDisableEvent";
    public final static String INCOMING_SMS_EVENT = "incomingSMSEvent";
    public final static String INCOMING_CALL_EVENT = "incomingCallEvent";
    public final static String FOCUS = "focus";
    public final static String BUTTON = "button";
    public final static String EDIT_TEXT = "editText";
    public final static String SEARCH_BAR = "searchBar";
    public final static String RADIO = "radio";
    public final static String RADIO_GROUP = "radioGroup";
    public final static String CHECKBOX = "check";
    public final static String TOGGLE_BUTTON = "toggle";
    public final static String TAB_HOST = "tabHost";
    public final static String NULL = "null";
    public final static String LIST_VIEW = "listView";
    public final static String SPINNER = "spinner";
    public final static String TEXT_VIEW = "text";
    public final static String MENU_VIEW = "menu";
    public final static String IMAGE_VIEW = "image";
    public final static String DIALOG_VIEW = "dialog";
    public final static String SEEK_BAR = "seekBar";
    public final static String RATING_BAR = "ratingBar";
    public final static String LINEAR_LAYOUT = "linearLayout";
    public final static String WEB_VIEW = "webPage";
    public final static String DATE_PICKER = "datePicker";
    public final static String TIME_PICKER = "timePicker";
    public final static String LIST_ITEM = "listItem";
    public final static String SPINNER_INPUT = "spinnerInput";
    public final static String MENU_ITEM = "menuItem";
    public final static String EMPTY_LIST = "emptyList";
    public final static String SINGLE_CHOICE_LIST = "singleChoiceList";
    public final static String MULTI_CHOICE_LIST = "multiChoiceList";
    public final static String PREFERENCE_LIST = "preferenceList";
    public final static String EMPTY_SPINNER = "emptySpinner";
    public final static String RELATIVE_LAYOUT = "relativeLayout";
    public final static String FOCUSABLE_EDIT_TEXT = "focusableEditText";
    public final static String[] PRECRAWLING = {};
    private final static String compareMapFormat = "expect:%s %s\nactual:%s %s\n";
    private IExtractor extractor;
    public ObjectMapper objectMapper = new ObjectMapper();
    private static Class<?> theClass;
    private UiDevice device;
    static {
        try {
            theClass = Class.forName(CLASS_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public TestSuite() {
        super(theClass);
        this.theViews = new HashMap<>();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        operation1(true);
        this.solo = new Solo(getInstrumentation(), new Solo.Config() {{
            commandLogging = true;
        }}, getActivity());
        this.extractor = new ReflectionExtractor(solo, this);
        this.device = UiDevice.getInstance(getInstrumentation());
//        restart();
        afterRestart();
        refreshCurrentActivity();
    }

    @Override
    protected void tearDown() throws Exception {
//        try {
//            solo.finalize();
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
        solo.finishOpenedActivities();
//        theActivity.finish();
        super.tearDown();
    }

    // Test Cases

    <TEST_TRACE_ID>

/*	// Generated from trace 0
	public void testTrace00000 () {

		// Testing base activity
		retrieveWidgets();
		// Testing current activity: should be a1
		solo.assertCurrentActivity("Testing base activity", "Tomdroid");
		doTestWidget(16908298, "android.widget.ListView", "");

		// Testing transition
		// Firing event: e0
		fireEvent (2131296257, 3, "", "image", "click");

		retrieveWidgets();
		// Testing current activity: should be a1
		solo.assertCurrentActivity("", "Tomdroid");
		doTestWidget(16908298, "android.widget.ListView", "");

		solo.sleep (SLEEP_AFTER_TASK);
	}*/


    //Helper methods for doing the actual work with instrumentation

    public void afterRestart() {
        solo.setActivityOrientation(Solo.PORTRAIT);
        solo.sleep(SLEEP_AFTER_RESTART);
//        waitOnThrobber();
        if (PRECRAWLING.length > 0) {
            refreshCurrentActivity();
            retrieveWidgets();
            processPrecrawling();
        }

        Log.d("nofatclips", "Ready to operate after restarting...");
    }

    private void processPrecrawling() {
        Log.i("nofatclips", "Processing precrawling");
        String[] params = new String[3];
        int paramCount = 0;
        for (String s : PRECRAWLING) {
            if (s == null) {
                switch (paramCount) {
                    case 0:
                        continue;
                    case 1: {
                        Log.i("nofatclips", "Firing event " + params[0]);
                        fireEventOnView(null, params[0], null);
                        break;
                    }
                    case 2: {
                        Log.i("nofatclips", "Firing event " + params[0] + " with value: " + params[1]);
                        fireEventOnView(null, params[0], params[1]);
                        break;
                    }
                    case 3: {
                        View v = getWidget(Integer.parseInt(params[1]));
                        Log.i("nofatclips", "Firing event " + params[0] + " on widget #" + params[1] + " with value: " + params[2]);
                        fireEventOnView(v, params[0], params[2]);
                        break;
                    }
                }
                ;
                paramCount = 0;
            } else {
                params[paramCount] = s;
                paramCount++;
            }
        }
    }


    /*
    public void waitOnThrobber() {
        boolean flag;
        do {
            flag = false;
            ArrayList<ProgressBar> bars = solo.getCurrentProgressBars();
            for (ProgressBar b: bars) {
                if (b.isShown() && b.isIndeterminate()) {
                    Log.d("nofatclips", "Waiting on Progress Bar #" + b.getId());
                    flag = true;
                    solo.sleep(500);
                }
            }
        } while (flag);
    }
    */
    public void waitOnThrobber() {
        int sleepTime = SLEEP_ON_THROBBER;
        if (sleepTime == 0) return;

        boolean flag;
        do {
            flag = false;
            ArrayList<ProgressBar> bars = solo.getCurrentViews(ProgressBar.class);
            for (ProgressBar b : bars) {
                if (b.isShown() && b.isIndeterminate()) {
                    Log.d("nofatclips", "Waiting on Progress Bar #" + b.getId());
                    flag = true;
                    solo.sleep(500);
                    sleepTime -= 500;
                }
            }
        } while (flag && (sleepTime > 0));
        sync();
    }

    public void retrieveWidgets() {
        home();
        clearWidgetList();
        ArrayList<View> viewList = (isInAndOutFocusEnabled()) ? solo.getViews() : solo.getCurrentViews();
        for (View w : viewList) {
            String text = (w instanceof TextView) ? ": " + ((TextView) w).getText().toString() : "";
            Log.d("nofatclips", "Found widget: id=" + w.getId() + " (" + w.toString() + ")" + text); // + " in window at [" + xy[0] + "," + xy[1] + "] on screen at [" + xy2[0] + "," + xy2[1] +"]");

            //
            allViews.add(w);
            //

            if (w.getId() > 0) {
                theViews.put(w.getId(), w); // Add only if the widget has a valid ID
            }
            if (w instanceof TabHost) {
                setTabs((TabHost) w);
            }
        }
    }

    public void setTabs(TabHost t) {
        this.tabs = t;
        this.tabNum = t.getTabWidget().getTabCount();
    }

    public Map<Integer, View> getWidgets() {
        return this.theViews;
    }

    public View getWidget(int key) {
        return getWidgets().get(key);
    }

    public View getWidget(int theId, String theType, String theName) {
        for (View testee : getWidgetsById(theId)) {
            if (checkWidgetEquivalence(testee, theId, theType, theName)) {
                return testee;
            }
        }
        return null;
    }

    public View getWidget(String theType, String theName) {
        for (View testee : getWidgetsByType(theType)) {
            if (checkWidgetEquivalence(testee, theType, theName)) {
                return testee;
            }
        }
        return null;
    }

    public boolean checkWidgetEquivalence(View testee, int theId, String theType, String theName) {
        return ((theId == testee.getId()) && checkWidgetEquivalence(testee, theType, theName));
    }

    public boolean checkWidgetEquivalence(View testee, String theType, String theName) {
        Log.i("nofatclips", "Retrieved from return list id=" + testee.getId());
        String testeeSimpleType = getSimpleType(testee);
        String testeeType = testee.getClass().getName();
        Log.i("nofatclips", "Testing for type (" + testeeType + ") against the original (" + theType + ")");
        String testeeText = (testee instanceof TextView) ? (((TextView) testee).getText().toString()) : "";

        String testeeName = testeeText;
        if (testee instanceof EditText) {
            CharSequence hint = ((EditText) testee).getHint();
            testeeName = (hint == null) ? "" : hint.toString();
        }

        //		String testeeName = (testee instanceof EditText)?(((EditText)testee).getHint().toString()):testeeText;
        Log.i("nofatclips", "Testing for name (" + testeeName + ") against the original (" + theName + ")");
        if (((theType.equals(testeeType)) || (theType.equals(testeeSimpleType))) && (theName.equals(testeeName))) {
            return true;
        }
        return false;
    }


    public ArrayList<View> getWidgetsById(int id) {
        ArrayList<View> theList = new ArrayList<View>();
        for (View theView : getAllWidgets()) {
            if (theView.getId() == id) {
                Log.i("nofatclips", "Added to return list id=" + id);
                theList.add(theView);
            }
        }
        return theList;
    }

    public ArrayList<View> getWidgetsByType(String type) {
        ArrayList<View> theList = new ArrayList<View>();
        for (View theView : getAllWidgets()) {
            if (theView.getClass().getName().equals(type)) {
                Log.i("nofatclips", "Added to return list " + type + " with id=" + theView.getId());
                theList.add(theView);
            }
        }
        return theList;
    }

    public ArrayList<View> getAllWidgets() {
        return solo.getCurrentViews();
    }

    public void clearWidgetList() {
        theViews.clear();
        allViews.clear();
    }

    public void doTestWidget(int theId, String theType, String theName) {
        if (theId == -1) return;
        assertNotNull("Testing for id #" + theId + " (" + theType + "): " + theName, getWidget(theId, theType, theName));
    }

    public void doTestWidget(String theType, String theName) {
        assertNotNull("Testing for type " + theType + "): " + theName, getWidget(theType, theName));
    }

    public void idle(int ms) {
        solo.sleep(ms + 3000);
    }

    public void fireEvent(int widgetId, int widgetIndex, String widgetType, String eventType) {
        //fireEvent(widgetId, widgetIndex, widgetType, eventType, null);
        fireEvent(widgetId, widgetIndex, "", widgetType, eventType);
    }

    public void fireEvent(int widgetId, int widgetIndex, String widgetName, String widgetType, String eventType) {
        fireEvent(widgetId, widgetIndex, widgetName, widgetType, eventType, null);
    }

    public void fireEvent(int widgetIndex, String widgetName, String widgetType, String eventType) {
        fireEvent(widgetIndex, widgetName, widgetType, eventType, null);
    }

    public void fireEvent(int widgetId, int widgetIndex, String widgetName, String widgetType, String eventType, String value) {

        View v = null;
        ArrayList<View> views = getAllWidgets();
        if (widgetIndex < views.size()) {
            v = views.get(widgetIndex); // Search widget by index
        }
        if ((v != null) && !checkWidgetEquivalence(v, widgetId, widgetType, widgetName)) {
            v = getWidget(widgetId, widgetType, widgetName);
        }
        if (v == null) {
            v = getWidget(widgetId);
        }
        if (v == null) {
            v = solo.getCurrentActivity().findViewById(widgetId);
        }
        Log.i(TestTag, "fire event in view:" + v);
        Log.i(TestTag, "eventType is:" + eventType);
        Log.i(TestTag, "value is:" + value);
        fireEventOnView(v, eventType, value);
    }

    public void fireEvent(int widgetIndex, String widgetName, String widgetType, String eventType, String value) {
        View v = null;
        if (eventType.equals(BACK) || eventType.equals(SCROLL_DOWN)) {
            fireEventOnView(null, eventType, null);
            return;
        } else if (eventType.equals(CLICK_ON_TEXT)) {
            Log.d("nofatclips", "Firing event: type= " + eventType + " value= " + value);
            fireEventOnView(null, eventType, value);
        }
        if (widgetType.equals(BUTTON)) {
            v = solo.getButton(widgetName);
        } else if (widgetType.equals(MENU_ITEM)) {
            v = solo.getText(widgetName);
        } else if (widgetType.equals(LIST_VIEW)) {
            v = solo.getCurrentViews(ListView.class).get(0);
        }
        if (v == null) {
            for (View w : getAllWidgets()) {
                if (w instanceof Button) {
                    Button candidate = (Button) w;
                    if (candidate.getText().equals(widgetName)) {
                        v = candidate;
                    }
                }
                if (v != null) break;
            }
        }
        Log.i(TestTag, "fire event in view:" + v);
        Log.i(TestTag, "eventType is:" + eventType);
        Log.i(TestTag, "value is:" + value);
        fireEventOnView(v, eventType, value);
    }

    private void fireEventOnView(View v, String eventType, String value) {
        injectInteraction(v, eventType, value);
        solo.sleep(SLEEP_AFTER_EVENT);
//        waitOnThrobber();
//        refreshCurrentActivity();
    }

    public void setInput(int widgetId, String inputType, String value) {
        View v = null;
        if (!inputType.equals(BACK)) {
            v = getWidget(widgetId);
            if (v == null) {
                v = theActivity.findViewById(widgetId);
            }
        }
        injectInteraction(v, inputType, value);
    }

    private void injectInteraction(View v, String interactionType, String value) {
        if (v != null) {
            requestView(v);
        }
        if (interactionType.equals(CLICK)) {
            click(v);
        } else if (interactionType.equals(LONG_CLICK)) {
            longClick(v);
        } else if (interactionType.equals(BACK)) {
            solo.goBack();
        } else if (interactionType.equals(CHANGE_ORIENTATION)) {
            changeOrientation();
        } else if (interactionType.equals(CLICK_ON_TEXT)) {
            clickOnText(value);
        } else if (interactionType.equals(PRESS_KEY)) {
            pressKey(value);
        } else if (interactionType.equals(OPEN_MENU)) {
            solo.sendKey(Solo.MENU);
        } else if (interactionType.equals(SCROLL_DOWN)) {
            solo.scrollDown();
        } else if (interactionType.equals(SWAP_TAB) && (value != null)) {
            if (v instanceof TabHost) {
                swapTab((TabHost) v, value);
            } else {
                swapTab(this.tabs, value);
            }
        } else if (interactionType.equals(LIST_SELECT)) {
            selectListItem((ListView) v, value);
        } else if (interactionType.equals(LIST_LONG_SELECT)) {
            selectListItem((ListView) v, value, true);
        } else if (interactionType.equals(SPINNER_SELECT)) {
            selectSpinnerItem((Spinner) v, value);
        } else if (interactionType.equals(TYPE_TEXT)) {
            typeText((EditText) v, value);
        } else if (interactionType.equals(WRITE_TEXT)) {
            writeText((EditText) v, value);
        } else if (interactionType.equals(SET_BAR)) {
            solo.setProgressBar((ProgressBar) v, Integer.parseInt(value));
        } else {
            return;
        }
    }

    protected void typeText(EditText v, String value) {
        solo.enterText(v, value);
        int imeOp = v.getImeOptions();
        if (!value.isEmpty()
                && (imeOp == EditorInfo.IME_ACTION_GO ||
                imeOp == EditorInfo.IME_ACTION_SEARCH)) {
            solo.clickOnView(v);
            if (imeOp == EditorInfo.IME_ACTION_GO) {
                solo.pressSoftKeyboardGoButton();
            } else {
                solo.pressSoftKeyboardSearchButton();
            }
        }
    }

    protected void writeText(EditText v, String value) {
        typeText(v, "");
        if (!value.isEmpty())
            typeText(v, value);
    }

    public void changeOrientation() {
        Display display = ((WindowManager) getInstrumentation().getContext().getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int angle = display.getRotation();
        int newAngle = ((angle == ROTATION_0) || (angle == ROTATION_180)) ? Solo.LANDSCAPE : Solo.PORTRAIT;
        solo.setActivityOrientation(newAngle);
    }

    private void swapTab(TabHost t, String tab) {
        swapTab(t, Integer.valueOf(tab));
    }

    private void swapTab(final TabHost t, int num) {
        final int n = Math.min(this.tabNum, Math.max(1, num)) - 1;
        Log.i("nofatclips", "Swapping to tab " + num);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                t.setCurrentTab(n);
            }
        });
        sync();
    }

    private void clickOnText(String text) {
        solo.clickOnText(text);
    }

    private void selectListItem(ListView l, String item) {
        selectListItem(l, Integer.valueOf(item), false);
    }

    private void selectListItem(ListView l, String item, boolean longClick) {
        selectListItem(l, Integer.valueOf(item), longClick);
    }

    private void selectListItem(final ListView l, int num, boolean longClick) {
        final int n = Math.min(l.getCount(), Math.max(1, num)) - 1;
        requestFocus(l);
        Log.i("nofatclips", "Swapping to listview item " + num);
        solo.sendKey(Solo.DOWN);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                l.setSelection(n);
            }
        });
        sync();
        if (n < l.getCount() / 2) {
            solo.sendKey(Solo.DOWN);
            solo.sendKey(Solo.UP);
        } else {
            solo.sendKey(Solo.UP);
            solo.sendKey(Solo.DOWN);
        }
        sync();
        if (longClick) {
            longClick(l.getSelectedView());
        } else {
            click(l.getSelectedView());
        }
    }

    public static boolean isInAndOutFocusEnabled() {
        return IN_AND_OUT_FOCUS;
    }

    protected void requestFocus(final View v) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                v.requestFocus();
            }
        });
        sync();
    }

    public void pressKey(String keyCode) {
        pressKey(Integer.parseInt(keyCode));
    }

    public void pressKey(int keyCode) {
        solo.sendKey(keyCode);
    }

    // Scroll until the view is on the screen if IN_AND_OUT_OF_FOCUS is enabled or if the force parameter is true
    protected void requestView(final View v, boolean force) {
        if (force || isInAndOutFocusEnabled()) {
            home();
            solo.sendKey(Solo.UP); // Solo.waitForView() requires a widget to be focused
            solo.waitForView(v, 1000, true);
        }
        requestFocus(v);
    }

    protected void requestView(final View v) {
        requestView(v, false);
    }


    private void selectSpinnerItem(Spinner l, String item) {
        selectSpinnerItem(l, Integer.valueOf(item));
    }

    private void selectSpinnerItem(final Spinner s, int num) {
        assertNotNull(s, "Cannon press spinner item: the spinner does not exist");
        Log.i("nofatclips", "Clicking the spinner view");
        click(s);
        sync();
        selectListItem(solo.getCurrentViews(ListView.class).get(0), num, false);
    }

    protected void click(View v) {
        // TouchUtils.clickView(this, v);
        solo.clickOnView(v);
    }

    protected void longClick(View v) {
        solo.clickLongOnView(v);
    }

    public void home() {

        // Scroll listviews up
        final ArrayList<ListView> viewList = solo.getCurrentViews(ListView.class);
        if (viewList.size() > 0) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    viewList.get(0).setSelection(0);
                }
            });
        }

        // Scroll scrollviews up
        final ArrayList<ScrollView> viewScroll = solo.getCurrentViews(ScrollView.class);
        if (viewScroll.size() > 0) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    viewScroll.get(0).fullScroll(ScrollView.FOCUS_UP);
                }
            });
        }
        sync();
    }


    public void restart() {
        if (FORCE_RESTART) {
            ContextWrapper main = new ContextWrapper(solo.getCurrentActivity());
            Intent i = main.getBaseContext().getPackageManager().getLaunchIntentForPackage(main.getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            main.startActivity(i);
        }
    }

    private void refreshCurrentActivity() {
        this.theActivity = solo.getCurrentActivity();
        Log.i("nofatclips", "Current activity is " + getActivity().getLocalClassName());
    }

    public String getSimpleType(View v) {
        String type = v.getClass().getName();
        if (type.endsWith("null"))
            return NULL;
        if (type.endsWith("RadioButton"))
            return RADIO;
        if (type.endsWith("CheckBox") || type.endsWith("CheckedTextView"))
            return CHECKBOX;
        if (type.endsWith("ToggleButton"))
            return TOGGLE_BUTTON;
        if (type.endsWith("IconMenuView"))
            return MENU_VIEW;
        if (type.endsWith("DatePicker"))
            return DATE_PICKER;
        if (type.endsWith("TimePicker"))
            return TIME_PICKER;
        if (type.endsWith("IconMenuItemView"))
            return MENU_ITEM;
        if (type.endsWith("DialogTitle"))
            return DIALOG_VIEW;
        if (type.endsWith("Button"))
            return BUTTON;
        if (type.endsWith("EditText"))
            return EDIT_TEXT;
        if (type.endsWith("Spinner")) {
            Spinner s = (Spinner) v;
            if (s.getCount() == 0) return EMPTY_SPINNER;
            return SPINNER;
        }
        if (type.endsWith("SeekBar"))
            return SEEK_BAR;
        if (v instanceof RatingBar && (!((RatingBar) v).isIndicator()))
            return RATING_BAR;
        if (type.endsWith("TabHost"))
            return TAB_HOST;
        if (type.endsWith("ListView") || type.endsWith("ExpandedMenuView")) {
            ListView l = (ListView) v;
            if (l.getCount() == 0) return EMPTY_LIST;

            if (l.getAdapter().getClass().getName().endsWith("PreferenceGroupAdapter")) {
                return PREFERENCE_LIST;
            }

            switch (l.getChoiceMode()) {
                case ListView.CHOICE_MODE_NONE:
                    return LIST_VIEW;
                case ListView.CHOICE_MODE_SINGLE:
                    return SINGLE_CHOICE_LIST;
                case ListView.CHOICE_MODE_MULTIPLE:
                    return MULTI_CHOICE_LIST;
            }
        }
        if (type.endsWith("TextView"))
            return TEXT_VIEW;
        if (type.endsWith("ImageView"))
            return IMAGE_VIEW;
        if (type.endsWith("LinearLayout"))
            return LINEAR_LAYOUT;
        if ((v instanceof WebView) || type.endsWith("WebView"))
            return WEB_VIEW;
        if (type.endsWith("TwoLineListItem"))
            return LIST_ITEM;
        return "";
    }

    protected void assertNotNull(final View v) {
        ActivityInstrumentationTestCase2.assertNotNull(v);
    }

    protected void assertNotNull(final View v, String errorMessage) {
        ActivityInstrumentationTestCase2.assertNotNull(errorMessage, v);
    }

    public void sync() {
//		getInstrumentation().waitForIdleSync();
    }

    public void debug(String msg) {
        Log.d("nofatclips", msg);
        for (View x : getWidgets().values()) {
            if (x instanceof TextView) {
                Log.i("nofatclips", ((TextView) x).getText().toString() + "[" + x.toString() + "]: " + x.getId());
            } else {
                Log.i("nofatclips", "[" + x.toString() + "]: " + x.getId());
            }
        }
    }

    public void report(String expect, String actual, int id) {
        //TODO
        //1. assertEqual two String
        //2. print difference between getHierarchy
        //3. print all difference between two State
        //4. screenShot .
        if (!expect.equals(actual)) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                State shouldBeState_ = objectMapper.readValue(expect, State.class);
                State actual_ = objectMapper.readValue(actual, State.class);
                stringBuilder.append("******").append("REPORT IN TEST").append(id).append("******\n");
                stringBuilder.append("difference between Hierarchy\n");
                String resultOfCompareHierarchy = compareHierarchy(shouldBeState_.getHierarchy(), actual_.getHierarchy());
                stringBuilder.append(resultOfCompareHierarchy);
                stringBuilder.append("difference between State\n");
                String resultOfCompareState = compareStateAfterSerialization(expect, actual);
                stringBuilder.append(resultOfCompareState);
                device.takeScreenshot(new File("test_" + id + ".png"));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("report_" + id));
                bufferedWriter.write(stringBuilder.toString());
                bufferedWriter.close();
                Assert.fail();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        return compareMap(expect, actual, 0);
    }

    public String compareStateAfterSerialization(String s1, String s2) throws JsonProcessingException {
        return compareMap(objectMapper.readValue(s1, Map.class), objectMapper.readValue(s2, Map.class), 0);
    }

    /**
     * m1 and m2 must have same type of key and value.
     *
     * @param m1
     * @param m2
     * @return
     */
    private String compareMap(Map m1, Map m2, int space) throws JsonProcessingException {
        boolean iteration = false;
        StringBuilder stringBuilder = new StringBuilder();
        HashSet intersection = new HashSet(m1.keySet());
        intersection.retainAll(m2.keySet());
        HashSet differenceKey = new HashSet(m1.keySet());
        differenceKey.removeAll(intersection);
        if (differenceKey.size() != 0) {
            for (int i = 0; i < space; i++) stringBuilder.append(" ");
            for (Object o : differenceKey)
                stringBuilder.append(String.format(compareMapFormat, o, objectMapper.writeValueAsString(m1.get(o)), -1, "null"));
        }
        differenceKey = new HashSet<>(m2.keySet());
        differenceKey.removeAll(intersection);
        if (differenceKey.size() != 0) {
            for (int i = 0; i < space; i++) stringBuilder.append(" ");
            for (Object o : differenceKey)
                stringBuilder.append(String.format(compareMapFormat, o, objectMapper.writeValueAsString(m2.get(o)), -1, "null"));
        }
        if (intersection.size() != 0) {
            Object key = intersection.iterator().next();
            // need to iteration
            if (m1.get(key) instanceof Map) {
                iteration = true;
            }
            // enter this branch means intersection is not 0 and don't need to iteration.
            else {
                for (Object o : intersection) {
                    Object o1 = m1.get(o);
                    Object o2 = m2.get(o);
                    if(!o1.equals(o2)) {
                        for (int i = 0; i < space; i++) stringBuilder.append(" ");
                        for (Object o : differenceKey)
                            stringBuilder.append(String.format(compareMapFormat, o, objectMapper.writeValueAsString(m1.get(o)), o, objectMapper.writeValueAsString(m2.get(o))));
                    }
                }
            }
        }

        if (iteration) {
            for (Object o : intersection) {
                stringBuilder.append(compareMap((Map) m1.get(o), (Map) m2.get(o), space + 2));
            }
        }

        return stringBuilder.toString();
    }

    protected void operation0(Boolean obj_0) {
        try {
            device.executeShellCommand(String.format("svc data %s", obj_0 ? "enable" : "disable"));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        this.solo.setMobileData(obj_0);
    }

    protected void operation1(Boolean obj_0) {
        try {
            device.executeShellCommand(String.format("svc data %s", obj_0 ? "enable" : "disable"));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        this.solo.setMobileData(obj_0);
    }

}


/**
 * GNU Affero General Public License, version 3
 * <p>
 * Copyright (c) 2014-2017 REvERSE, REsEarch gRoup of Software Engineering @ the University of Naples Federico II, http://reverse.dieti.unina.it/
 * <p>
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package android.ripper.extension.test.extractor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsSpinner;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import android.ripper.extension.test.constants.RipperSimpleType;
import android.ripper.extension.test.extractor.helper.ReflectionHelper;
import android.ripper.extension.test.log.Debug;

import com.robotium.solo.Solo;

import it.unina.android.shared.ripper.constants.InteractionType;
import it.unina.android.shared.ripper.constants.SimpleType;
import it.unina.android.shared.ripper.model.state.ActivityDescription;
import it.unina.android.shared.ripper.model.state.WidgetDescription;

/**
 * Uses the JAVA Reflection API to extract information about the current GUI
 * Interface
 *
 * update: replace IRobot with Solo.
 *
 * @OriginalAuthor Nicola Amatucci - REvERSE
 * @NewAuthor RiDong Yang & Chun Li
 *
Original author
New author
 *
 */
@SuppressLint("NewApi")
public class ReflectionExtractor implements IExtractor {

    private final Solo solo;


    /**
     *
     * @param solo solo
     */
    public ReflectionExtractor(Solo solo) {
        this.solo = solo;
    }

    @Override
    public ActivityDescription extract() {

        ActivityDescription ret = new ActivityDescription();

//		Activity activity = robot.getCurrentActivity();
        Activity activity = solo.getCurrentActivity();
        // activity info
        ret.setTitle((activity.getTitle() != null) ? activity.getTitle().toString() : "");
        ret.setActivityClass(activity.getClass());
        ret.setName(activity.getClass().getSimpleName());
        ret.setHasMenu(this.activityHasMenu(activity));
        ret.setHandlesKeyPress(this.handlesKeyPress(activity));
        ret.setHandlesLongKeyPress(this.handlesLongKeyPress(activity));

        boolean isTabActivity = this.isTabActivity(activity);
        ret.setIsTabActivity(isTabActivity);
        if (isTabActivity) {
            ret.setTabsCount(this.getTabActivityTabsCount(activity));
            ret.setCurrentTab(this.getTabActivityPosition(activity));
        }

        if (activity.isTaskRoot()) {
            ret.setIsRootActivity(true);
        }

        ret.setListeners(this.getActivityListeners(activity));

        try {

            solo.hideSoftKeyboard();

            // widgets
//			ArrayList<View> viewList = robot.getViews();
            ArrayList<View> viewList = solo.getCurrentViews();
            HashMap<String, Integer> objectsMap = new HashMap<>();
            HashMap<String, Boolean> objectsVisibilityMap = new HashMap<>();
            ArrayList<Integer> drawerIndexs = new ArrayList<>();
            if (viewList != null) {
                int i;
                for (i = viewList.size() - 2; i >= 0; i--) {
                    if (viewList.get(i).getClass().getName().endsWith("DecorView")) {
                        break;
                    }
                }
                if (i > 0) {
                    ret.setPopupShowing(true);
                }
                HashMap<Integer, Integer> depths = new HashMap<>();
                for (int i1 = 0, viewListSize = viewList.size(); i1 < viewListSize; i1++) {
                    if (i1 <= i) {
                        continue;
                    }
                    View v = viewList.get(i1);
                    WidgetDescription wd = new WidgetDescription();

                    Debug.info(this, "Found widget: id=" + v.getId() + " (" + v.toString() + ")");

                    wd.setId(v.getId());
                    wd.setType(v.getClass());
                    wd.setName(this.detectName(v));

                    wd.setIndex(i1);
                    objectsMap.put(v.toString(), i1);

                    this.setViewListeners(ret, wd, v);

                    this.setValue(v, wd);

                    wd.setEnabled(v.isEnabled());

                    wd.setVisible(v.getVisibility() == View.VISIBLE);
                    objectsVisibilityMap.put(v.toString(), v.getVisibility() == View.VISIBLE);

                    // wd.setTextualId(this.reflectTextualIDbyNumericalID(v.getId()));
                    if (v.getId() != View.NO_ID && activity.getResources() != null) {
                        try {
                            String debugMe = activity.getResources().getResourceEntryName(v.getId());
                            wd.setTextualId(debugMe);
                            Debug.info(this, "Widget: id=" + v.getId() + " r_id= " + debugMe + ")");
                        } catch (Throwable t) {
                            wd.setTextualId(Integer.toString(v.getId()));
                        }
                    }

                    if (v instanceof TextView) {
                        wd.setTextType(((TextView) v).getInputType());
                    }

                    if (v instanceof TabHost) {
                        // Log.d(TAG, "Found tabhost: id=" + w.getId());
                    }

                    if (v instanceof ImageView) {

                    }

                    setCount(v, wd);

                    // ripper like
                    try {
                        wd.setSimpleType(RipperSimpleType.getSimpleType(v));

                        // if (wd.getSimpleType() != null &&
                        // wd.getSimpleType().equals(android.ripper.extension.test.constants.SimpleType.DRAWER_LAYOUT))
                        // {
                        // drawerIndexs.add(wd.getIndex());
                        // }

                        try {
                            Class<?> c = Class.forName("android.support.design.internal.ScrimInsetsFrameLayout");
                            if (c.isAssignableFrom(v.getClass())) {
                                drawerIndexs.add(wd.getIndex());

                                if (wd.getSimpleType() != null && wd.getSimpleType().equals("")) {
                                    wd.setSimpleType(
                                            it.unina.android.shared.ripper.constants.SimpleType.SCRIM_INSETS_FRAME_LAYOUT);
                                }

                            }
                        } catch (Throwable t) {
                            // t.printStackTrace();
                        }
                    } catch (Throwable t) {
                        System.out.println("DescriptionError: " + t.getMessage());
                        wd.setSimpleType("");
                    }
                    ViewParent parentView = null;

                    try {
                        parentView = v.getParent();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    if (parentView instanceof View) {
                        View parent = (View) parentView;

                        if (objectsVisibilityMap.containsKey(parent.toString())) {
                            if (!objectsVisibilityMap.get(parent.toString())) {
                                wd.setVisible(false);
                                objectsVisibilityMap.put(v.toString(), false);
                            }
                        }

                        Integer parentIndex = objectsMap.get(parent.toString());
                        wd.setParentIndex((parentIndex != null) ? parentIndex : -1);
                        wd.setDepth(wd.getParentIndex().equals(-1) ? 0 : depths.get(parentIndex) + 1);
                        depths.put(wd.getIndex(), wd.getDepth());

                        if (parentIndex != null) {
                            for (Integer drawerIndex : drawerIndexs) {
                                if (parentIndex.equals(drawerIndex)) {
                                    if (wd.getSimpleType() != null && wd.getSimpleType()
                                            .equals(SimpleType.LIST_VIEW)) {
                                        wd.setSimpleType(SimpleType.DRAWER_LIST_VIEW);
                                    } else {
                                        drawerIndexs.add(wd.getIndex());
                                    }
                                    break;
                                }
                            }
                        }

                        wd.setParentId(parent.getId());
                        wd.setParentType(parent.getClass().getCanonicalName());
                        wd.setParentName(this.detectName(parent));

                        if (parent.getId() == View.NO_ID) {
                            View ancestor = detectFirstAncestorWithId(parent);

                            if (ancestor != null) {
                                wd.setAncestorId(ancestor.getId());
                                wd.setAncestorType(ancestor.getClass().getCanonicalName());
                            } else {
                                wd.setAncestorType("null");
                            }

                        }
                    } else {
                        wd.setParentType("null");
                    }

                    if (wd.getVisible()) {
                        ret.addWidget(wd);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return ret;
    }

    /**
     * Detect the first Ancestor that owns a valid id value
     *
     * @param v Widget
     * @return first ancestor
     */
    protected View detectFirstAncestorWithId(View v) // throws Exception
    {
        if ((v == null) || (v != null && v.getParent() == null))
            return null;

        ViewParent parentView = v.getParent();

        if (parentView instanceof View) {
            View parent = (View) parentView;

            if (parent != null && parent.getId() != View.NO_ID) {
                return parent;
            } else {
                return detectFirstAncestorWithId(parent);
            }
        } else {
            // throw new Exception("null found");
            return null;
        }
    }

    /**
     * Set Listeners for the Widget
     *
     * @param ad
     *            ActivityDescription instance
     * @param wd
     *            WidgetDescription instance
     * @param v
     *            Widget
     */
    protected void setViewListeners(ActivityDescription ad, WidgetDescription wd, View v) {
        if (!(v instanceof android.opengl.GLSurfaceView) && v instanceof View) {
            wd.setListeners(ReflectionHelper.reflectViewListeners(v));
        }

        try {
            // Class.isInstance <-> instanceof
            if (Class.forName("com.android.internal.view.menu.IconMenuItemView").isInstance(v) && ad.hasMenu()) {
                wd.addListener("OnClickListener", true);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        if (v instanceof TabHost) {
            wd.addSupportedEvent(InteractionType.SWAP_TAB);
        }
    }

    /**
     * Detect Name of the Widget
     *
     * @param v Widget
     * @return name
     */
    protected String detectName(View v) {
        String name = "";
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            name = (t.getText() != null) ? t.getText().toString() : "";
            if (v instanceof EditText) {
                CharSequence hint = ((EditText) v).getHint();
                name = (hint == null) ? "" : hint.toString();
            }
        } else if (v instanceof RadioGroup) {
            RadioGroup g = (RadioGroup) v;
            int max = g.getChildCount();
            String text;
            for (int i = 0; i < max; i++) {
                View c = g.getChildAt(i);
                text = detectName(c);
                if (!text.equals("")) {
                    name = text;
                    break;
                }
            }
        }
        return name;
    }

    /**
     * Set Value of the Widget
     *
     * @param v
     *            Widget
     * @param wd
     *            WidgetDescription instance
     */
    protected void setValue(View v, WidgetDescription wd) {

        // Checkboxes, radio buttons and toggle buttons -> the value is the
        // checked state (true or false)
        if (v instanceof Checkable) {
            wd.setValue(String.valueOf(((Checkable) v).isChecked()));
        }

        // Textview, Editview et al. -> the value is the displayed text
        if (v instanceof TextView) {
            wd.setValue(((TextView) v).getText().toString());
            // wd.setValue("");
            return;
        }

        // Progress bars, seek bars and rating bars -> the value is the current
        // progress
        if (v instanceof ProgressBar) {
            wd.setValue(String.valueOf(((ProgressBar) v).getProgress()));
        }

        if (v instanceof ImageView) {
            ImageView imgView = (ImageView) v;
            // TODO:
        }

    }

    /**
     * Set Count of the Widget
     *
     * @param v
     *            Widget
     * @param w
     *            WidgetDescription instance
     */
    @SuppressWarnings("rawtypes")
    public static void setCount(View v, WidgetDescription w) {
        // For lists, the count is set to the number of rows in the list
        // (inactive rows - e.g. separators - count as well)
        if (v instanceof AdapterView) {
            w.setCount(((AdapterView) v).getCount());
            return;
        }

        // For Spinners, the count is set to the number of options
        if (v instanceof AbsSpinner) {
            w.setCount(((AbsSpinner) v).getCount());
            return;
        }

        // For the tab layout host, the count is set to the number of tabs
        if (v instanceof TabHost) {
            w.setCount(((TabHost) v).getTabWidget().getTabCount());
            return;
        }

        // For grids, the count is set to the number of icons, for RadioGroups
        // it's set to the number of RadioButtons
        if (v instanceof ViewGroup) {
            w.setCount(((ViewGroup) v).getChildCount());
            return;
        }

        // For progress bars, seek bars and rating bars, the count is set to the
        // maximum value allowed
        if (v instanceof ProgressBar) {
            w.setCount(((ProgressBar) v).getMax());
        }
    }

    /**
     * Get Listeners for the Activity
     *
     * @param activity
     *            Activity
     * @return activity's listeners
     */
    protected HashMap<String, Boolean> getActivityListeners(Activity activity) {
        HashMap<String, Boolean> ret = new HashMap<>();

        // sensors
        ret.put("SensorEventListener",
                ReflectionHelper.scanClassForInterface(activity.getClass(), "android.hardware.SensorEventListener"));
        ret.put("SensorListener",
                ReflectionHelper.scanClassForInterface(activity.getClass(), "android.hardware.SensorListener"));
        ret.put("OrientationListener",
                ReflectionHelper.scanClassForInterface(activity.getClass(), "android.view.OrientationListener"));

        // location
        ret.put("LocationListener",
                ReflectionHelper.scanClassForInterface(activity.getClass(), "android.location.LocationListener"));

        return ret;
    }

    /**
     * Check if the Activity has a menu
     *
     * @param activity
     *            Activity
     * @return if activity has menu
     */
    protected Boolean activityHasMenu(Activity activity) {
        return (ReflectionHelper.hasDeclaredMethod(activity.getClass(), "onCreateOptionsMenu")
                || ReflectionHelper.hasDeclaredMethod(activity.getClass(), "onPrepareOptionsMenu"));
    }

    /**
     * Check if the Activity handles keypress events
     *
     * @param activity
     *            Activity
     * @return if activity handles key press
     */
    protected boolean handlesKeyPress(Activity activity) {
        return ReflectionHelper.hasDeclaredMethod(activity.getClass(), "onKeyDown");
    }

    /**
     * Check if the Activity handles long keypress events
     *
     * @param activity
     *            Activity
     * @return if activity handles long key press
     */
    protected boolean handlesLongKeyPress(Activity activity) {
        return ReflectionHelper.hasDeclaredMethod(activity.getClass(), "onKeyLongPress");
    }

    /**
     * Check if Activity is a tab activity
     *
     * @param activity
     *            Activity
     * @return is activity is tab activity
     */
    protected boolean isTabActivity(Activity activity) {
        return ReflectionHelper.isDescendant(activity.getClass(), android.app.TabActivity.class);
    }

    /**
     * Get Tabs count
     *
     * @param activity
     *            Activity
     * @return tab activity's tab count
     */
    public int getTabActivityTabsCount(Activity activity) {
        // return
        // ((android.app.TabActivity)activity).getTabHost().getChildCount();
        // return
        // ((android.app.TabActivity)activity).getTabHost().getTabWidget().getTabCount();
        return ((android.app.TabActivity) activity).getTabWidget().getTabCount();
    }

    public int getTabActivityPosition(Activity activity) {
        // return
        // ((android.app.TabActivity)activity).getTabHost().getChildCount();
        // return
        // ((android.app.TabActivity)activity).getTabHost().getTabWidget().getTabCount();
        return ((android.app.TabActivity) activity).getTabHost().getCurrentTab();
    }
}

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
import android.app.UiAutomation;
import android.graphics.drawable.Animatable;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
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
import java.util.Objects;

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
	private ActivityInstrumentationTestCase2<?> testCase = null;


    /**
     *
     * @param solo solo
     */
    public ReflectionExtractor(Solo solo, ActivityInstrumentationTestCase2<?> testCase) {
    	this.testCase = testCase;
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
			ArrayList<View> viewList = solo.getViews();
			HashMap<View, Integer> objectsMap = new HashMap<>();
			HashMap<View, Boolean> objectsVisibilityMap = new HashMap<>();
			HashMap<Integer, ArrayList<View>> drawerIndexs = new HashMap<>();

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

					this.setViewListeners(ret, wd, v);
					wd.setClickable(v.isClickable());
					wd.setLongClickable(v.isLongClickable());
					this.setValue(v, wd);

					wd.setEnabled(v.isEnabled());

					wd.setVisible(v.getVisibility() == View.VISIBLE && crossValidateViewExistence(v));
					objectsVisibilityMap.put(v, wd.getVisible());

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
					wd.setSimpleType(RipperSimpleType.getSimpleType(v));

					if (v.getClass().getName().contains("Progress")) {
						if (v instanceof ProgressBar) {
							wd.setVisible(((ProgressBar) v).isAnimating());
							wd.setSimpleType(SimpleType.PROGRESS);
						} else {
							try {
								Debug.info(this, v.toString());
								wd.setVisible(((Animatable) v.getBackground()).isRunning());
								wd.setSimpleType(SimpleType.PROGRESS);
							} catch (ClassCastException e) {
								e.printStackTrace();
							}
						}
					}

					setCount(v, wd);

					// ripper like
					try {
						if (wd.getSimpleType() != null && wd.getSimpleType().equals(SimpleType.DRAWER_LAYOUT)) {
							ViewGroup drawer;
							if (v instanceof ViewGroup) {
								drawer = ((ViewGroup) v);
								ArrayList<View> ignore = new ArrayList<>();
								for (int j = drawer.getChildCount() - 1; j >= 0; j--) {
									View child = drawer.getChildAt(j);
									if (child.getVisibility() == View.VISIBLE) {
										for (int k = j - 1; k >= 0; k--) {
											View ignored = drawer.getChildAt(k);
											Debug.info(this, "Ignoring " + ignored.toString());
											ignore.add(ignored);
										}
										break;
									}
								}
								drawerIndexs.put(wd.getIndex(), ignore);
							}
						}

						try {
							Class<?> c = Class.forName("android.support.design.internal.ScrimInsetsFrameLayout");
							if (c.isAssignableFrom(v.getClass())) {
								drawerIndexs.put(wd.getIndex(), new ArrayList<>());

								if (wd.getSimpleType() != null && wd.getSimpleType().equals("")) {
									wd.setSimpleType(
											SimpleType.SCRIM_INSETS_FRAME_LAYOUT);
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

						if (objectsVisibilityMap.containsKey(parent) && !wd.getSimpleType().equals(SimpleType.PROGRESS)) {
							if (!objectsVisibilityMap.get(parent)) {
								wd.setVisible(false);
								objectsVisibilityMap.put(v, false);
							}
						}

						Integer parentIndex = objectsMap.get(parent);
						if (parentIndex == null && i1 != i + 1 && !wd.getSimpleType().equals(SimpleType.PROGRESS)) {
							continue;
						}
						if (parentIndex != null) {
							boolean cont = false;
							for (Integer drawerIndex : drawerIndexs.keySet()) {
								if (parentIndex.equals(drawerIndex)) {
									ArrayList<View> ignore = drawerIndexs.get(drawerIndex);
									if (ignore.contains(v)) {
										if (wd.getVisible()) {
											parentInfo(wd, parentIndex, parent, depths);
											ret.addWidget(wd);
										}
										cont = true;
									}
									if (wd.getSimpleType() != null && wd.getSimpleType()
											.equals(SimpleType.LIST_VIEW)) {
										wd.setSimpleType(SimpleType.DRAWER_LIST_VIEW);
									} else {
										if (!ignore.isEmpty() && wd.getClickable() && wd.isEnabled()) {
											wd.setSimpleType(SimpleType.LIST_ITEM);
										}
										drawerIndexs.put(wd.getIndex(), ignore);
									}
									break;
								}
							}
							if (cont) continue;
						}
						parentInfo(wd, parentIndex, parent, depths);
						objectsMap.put(v, i1);

					} else {
						wd.setParentType("null");
					}

					if (wd.getVisible()) {
						ret.addWidget(wd);
					}
					Debug.log(this, "wd " + wd + " " + (wd.getVisible() ? "visible" : "invisible"));
				}
			}
		} catch (java.lang.Throwable t) {
			t.printStackTrace();
		}
		if (ret.getWidgets().get(0).getType().getCanonicalName().contains("Popup")) {
			ret.setPopupShowing(true);
		}

		return ret;
	}

	private void parentInfo(WidgetDescription wd, Integer parentIndex, View parent, HashMap<Integer, Integer> depths) {
		wd.setParentIndex((parentIndex != null) ? parentIndex : -1);
		wd.setDepth(wd.getParentIndex().equals(-1) ? 0 : depths.get(parentIndex) + 1);
		depths.put(wd.getIndex(), wd.getDepth());
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
	}

	public UiAutomation getUiAutomation() {
		return testCase.getInstrumentation().getUiAutomation();
	}

    public boolean crossValidateViewExistence(View v) {
        AccessibilityNodeInfo root = null;
        do {
//			for (AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
//				if (window.getId() == Integer.MAX_VALUE) {
//					root = window.getRoot();
//				}
//			}
            root = getUiAutomation().getRootInActiveWindow();
        } while (root == null);
        String resourceId = null;
        if (v.getId() != View.NO_ID && testCase.getActivity().getResources() != null) {
            try {
                resourceId = testCase.getActivity().getResources().getResourceEntryName(v.getId());
            } catch (Exception ignored) {
            }
        }
        return found(root, v, resourceId);
    }

    private boolean found(AccessibilityNodeInfo nodeInfo, View v, String resourceId) {
        String viewIdResourceName = nodeInfo.getViewIdResourceName();
        if (((resourceId != null && viewIdResourceName != null && viewIdResourceName.endsWith(resourceId)) ||
                Objects.equals(nodeInfo.getClassName(), v.getAccessibilityClassName())) &&
                Objects.equals(nodeInfo.isClickable(), v.isClickable()) &&
                Objects.equals(nodeInfo.isEnabled(), v.isEnabled()) &&
                Objects.equals(nodeInfo.isLongClickable(), v.isLongClickable()) &&
                Objects.equals(nodeInfo.isFocused(), v.isFocused())) {
            return true;
        } else {
            Debug.log(this, String.format("Expect %s,%s,%s,%s,%s,%s but is %s,%s,%s,%s,%s,%s",
                    resourceId, v.getAccessibilityClassName(), v.isClickable(), v.isEnabled(), v.isLongClickable(), v.isFocused(),
                    viewIdResourceName, nodeInfo.getClassName(), nodeInfo.isClickable(), nodeInfo.isEnabled(), nodeInfo.isLongClickable(), nodeInfo.isFocused()));
            int count = nodeInfo.getChildCount();
            if (count != 0) {
                for (int i = 0; i < count; i++) {
                    AccessibilityNodeInfo child = nodeInfo.getChild(i);
                    if (child != null && child.isVisibleToUser() && found(child, v, resourceId)) {
                        Debug.info(this, "Found");
                        return true;
                    } else {
                        child.recycle();
                    }
                }
            }
            return false;
        }
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

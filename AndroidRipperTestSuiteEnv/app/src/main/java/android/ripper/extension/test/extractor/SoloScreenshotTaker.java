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

package android.ripper.extension.test.extractor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import com.robotium.solo.Solo;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * @author Nicola Amatucci - REvERSE
 *
 */
public class SoloScreenshotTaker implements IScreenshotTaker
{
	
	/**
	 * Instrumentation
	 */
	Instrumentation instrumentation;
	
	/**
	 * Context
	 */
	Context context;
	
	/**
	 * solo Instance
	 */
	Solo solo;

	/**
	 * constructor
	 * @param solo
	 * @param instrumentation
	 */
	public SoloScreenshotTaker(Solo solo, Instrumentation instrumentation)
	{
		this.solo = solo;
		this.instrumentation = instrumentation;
		this.context = instrumentation.getContext();
	}
	
	/* (non-Javadoc)
	 * @see it.unina.android.ripper.extractor.screenshoot.IScreenshotTaker#takeScreenshot(android.app.Activity)
	 */
	@SuppressLint("WorldWriteableFiles")
	public void takeScreenshot(Activity activity, String filename)
	{
		FileOutputStream fileOutput = null;
		try {
			fileOutput = this.context.openFileOutput(filename, Context.MODE_PRIVATE);
			ArrayList<View> views = solo.getCurrentViews();
			if (views != null && views.size() > 0)
			{
				final View view = views.get(0);
				final boolean flag = view.isDrawingCacheEnabled();
				activity.runOnUiThread(new Runnable() {
					public void run() {
						if (!flag) {
							view.setDrawingCacheEnabled(true);
						}
			            view.buildDrawingCache();
					}
				});
				this.instrumentation.waitForIdleSync();
				Bitmap b = view.getDrawingCache();
	            b = b.copy(b.getConfig(), false);
				activity.runOnUiThread(new Runnable() {
					public void run() {
						if (!flag) {
							view.setDrawingCacheEnabled(false);
						}
					}
				});

				if (fileOutput != null) {
					b.compress(Bitmap.CompressFormat.JPEG, 90, fileOutput);
					Log.i("ScreenShot", "Saved image on disk: " + filename);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fileOutput != null) {
				try { fileOutput.close(); } catch(Exception ex) {}
			}
		}
	}
}

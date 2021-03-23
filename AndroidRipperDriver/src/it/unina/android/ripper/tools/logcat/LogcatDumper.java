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

package it.unina.android.ripper.tools.logcat;

import java.io.FileOutputStream;
import java.io.PrintStream;

import it.unina.android.ripper.tools.lib.AndroidTools;
import org.apache.commons.lang3.StringUtils;

/**
 * Dumps the Logcat to a File
 * 
 * @author Nicola Amatucci - REvERSE
 *
 */
public class LogcatDumper extends Thread {
	
	/**
	 * Device
	 */
	String device;
	
	/**
	 * Destination File Name
	 */
	String filename;

	/**
	 * -t 'time' argument
	 */
	String timeOption = "";

	/**
	 * Constructor
	 * 
	 * @param device device name
	 */
	public LogcatDumper(String device) {
		this(device, "logcat_" + System.currentTimeMillis() + ".txt");
	}
	
	/**
	 * Constructor
	 * 
	 * @param device device name
	 * @param filename Destination File Name
	 */	
	public LogcatDumper(String device, String filename) {
		this.device = device;
		this.filename = filename;
	}

	public LogcatDumper(String device, String filename, String afterTime) {
		this.device = device;
		this.filename = filename;
		if (!StringUtils.isEmpty(afterTime))
			this.timeOption = String.format("-t '%s'", afterTime);
	}

	@Override
	public void run() {		
				
		try {
			
			FileOutputStream fos = new FileOutputStream(filename, true);
			PrintStream ps = new PrintStream(fos);
			//明明是dump，却不加-d参数，无语
			AndroidTools.adb("-s", device, "logcat", timeOption).connectStderr(ps).connectStdout(ps).waitForSuccess();

			try { ps.close(); } catch (Exception ex) {}
			try { fos.close(); } catch (Exception ex) {}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

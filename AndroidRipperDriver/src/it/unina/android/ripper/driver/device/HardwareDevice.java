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

package it.unina.android.ripper.driver.device;

/**
 * Android Hardware Device Instance
 * 
 * @author Nicola Amatucci - REvERSE
 *
 */
import it.unina.android.ripper.tools.actions.Actions;

public class HardwareDevice extends AbstractDevice {

	String name;
	
	public HardwareDevice(String device) {
		super(device);
		Actions.DEVICE = device;
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}

	@Override
	public String getIpAddress() {
		return Actions.getRealDeviceIP();
	}

}

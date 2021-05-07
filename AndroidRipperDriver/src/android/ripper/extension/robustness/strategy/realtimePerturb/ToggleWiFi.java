package android.ripper.extension.robustness.strategy.realtimePerturb;

import android.ripper.extension.robustness.strategy.RealtimePerturb;
import it.unina.android.ripper.tools.lib.AndroidTools;

import java.io.IOException;

public class ToggleWiFi extends RealtimePerturb {
	@Override
	public void perturb() {
		try {
			AndroidTools.adb("shell", "svc", "wifi", "disable");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void recover() {
		try {
			AndroidTools.adb("shell", "svc", "wifi", "enable");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

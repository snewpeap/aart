package android.ripper.extension.robustness.strategy;

import android.ripper.extension.robustness.strategy.realtimePerturb.DoRotation;
import android.ripper.extension.robustness.strategy.realtimePerturb.ToggleData;
import android.ripper.extension.robustness.strategy.realtimePerturb.ToggleScreen;
import android.ripper.extension.robustness.strategy.realtimePerturb.ToggleWiFi;

public class RealtimePerturb {
	public void perturb() {

	}

	public void recover() {

	}

	public static RealtimePerturb of(String s) {
		switch (s) {
			case "wifi":
				return new ToggleWiFi();
			case "data":
				return new ToggleData();
			case "screen":
				return new ToggleScreen();
			case "rotation":
				return new DoRotation();
			default:
				return new RealtimePerturb();
		}
	}
}

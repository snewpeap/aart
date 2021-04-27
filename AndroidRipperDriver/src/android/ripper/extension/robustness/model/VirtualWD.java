package android.ripper.extension.robustness.model;

import it.unina.android.shared.ripper.model.state.WidgetDescription;

//import java.util.HashMap;

public class VirtualWD extends WidgetDescription {
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VirtualWD) {
			VirtualWD vwd = (VirtualWD) obj;
			return capabilitiesEquals(vwd) && className.equals(vwd.className);
		}
		return false;
	}

	public VirtualWD(String className,
					 Boolean isClickable,
					 Boolean isLongClickable,
					 Boolean enable,
					 Boolean visible,
					 Integer depth) {
		this.className = className;
		setClickable(isClickable);
		setLongClickable(isLongClickable);
		setEnabled(enable);
		setVisible(visible);
		setDepth(depth);
	}

	private final String className;

	@Override
	public String getClassName() {
		return className;
	}
}

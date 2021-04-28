package android.ripper.extension.robustness.model;

import it.unina.android.shared.ripper.model.state.WidgetDescription;

import java.util.Objects;

//import java.util.HashMap;

public class VirtualWD extends WidgetDescription {
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VirtualWD) {
			VirtualWD vwd = (VirtualWD) obj;
			return className.equals(vwd.className) && capabilitiesEquals(vwd);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(className, getClickable(), getLongClickable(), getVisible(), getEnabled());
	}

	public VirtualWD(String className,
					  String parentName,
					  Boolean isClickable,
					  Boolean isLongClickable,
					  Boolean enable,
					  Boolean visible,
					  Integer depth) {
		this(parentName.isEmpty() ? className : parentName + ">" + className,
				isClickable, isLongClickable, enable, visible, depth);
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

	public int getOriginalCount() {
		return originalCount;
	}

	private int originalCount = 1;

	@Override
	public String getClassName() {
		return className;
	}

	public VirtualWD merge(VirtualWD virtualWD) {
		originalCount += virtualWD.originalCount;
		//Fusing views capability into VWD by simply logical OR them
		setEnabled(getEnabled() || virtualWD.getEnabled());
		setVisible(getVisible() || virtualWD.getVisible());
		setClickable(judgeClickable() || virtualWD.judgeClickable());
		setLongClickable(judgeLongClickable() || virtualWD.judgeLongClickable());
		return this;
	}

	public static int getDepthFromClassName(String className) {
		if (className == null || className.isEmpty()) {
			return 0;
		} else {
			int count = 0;

			for(int i = 0; i < className.length(); ++i) {
				if ('>' == className.charAt(i)) {
					++count;
				}
			}

			return count;
		}
	}
}

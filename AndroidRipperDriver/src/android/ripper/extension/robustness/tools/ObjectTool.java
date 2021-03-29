package android.ripper.extension.robustness.tools;

import java.util.Objects;
import java.util.function.Function;

public class ObjectTool {
	public static <T, R> boolean propEquals(T o1, T o2, Function<T, R> prop) {
		if (o1 != null && o2 != null)
			return Objects.equals(prop.apply(o1), prop.apply(o2));
		else return o1 == null && o2 == null;
	}
}

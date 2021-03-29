package android.ripper.extension.robustness.tools;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectTool {
	public static <T, R> boolean propEquals(T o1, T o2, Function<T, R> prop) {
		return propEquals(o1, o2, prop, true);
	}

	public static <T, R> boolean propEquals(T o1, T o2, Function<T, R> prop, boolean nullable) {
		if (o1 != null && o2 != null)
			return Objects.equals(prop.apply(o1), prop.apply(o2));
		else return nullable && o1 == null && o2 == null;
	}

	public static <T, R> boolean propEquals(Supplier<Comparand<T>> comparands, Function<T, R> prop) {
		Comparand<T> c = comparands.get();
		return propEquals(c.getFirst(), c.getLast(), prop);
	}


	public static class Comparand<T> {
		private final T first, last;

		Comparand(T first, T last) {
			this.first = first;
			this.last = last;
		}

		public static <T> Comparand<T> of(T t1, T t2) {
			return new Comparand<>(t1, t2);
		}

		public T getFirst() {
			return first;
		}

		public T getLast() {
			return last;
		}
	}
}

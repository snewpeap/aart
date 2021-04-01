package android.ripper.extension.robustness.model.compare;

public class Comparand<T> {
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

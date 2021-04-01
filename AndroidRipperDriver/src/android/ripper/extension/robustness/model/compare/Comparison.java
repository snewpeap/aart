package android.ripper.extension.robustness.model.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class Comparison {
	private final String Type;
	private final List<Entry<String, Comparand<?>>> differences;

	public Comparison(String type) {
		Type = type;
		differences = new ArrayList<>();
	}

	public void addDifference(Entry<String, Comparand<?>> difference) {
		differences.add(difference);
	}

	public String getType() {
		return Type;
	}

	public List<Entry<String, Comparand<?>>> getDifferences() {
		return differences;
	}
}

package android.ripper.extension.robustness.tools;

public class MethodNameHelper {
	public static String here() {
		return new Throwable().getStackTrace()[1].getMethodName();
	}

	public static String caller() {
		return new Throwable().getStackTrace()[2].getMethodName();
	}
}

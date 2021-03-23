package android.ripper.extension.robustness.tools;

public class MethodNameHelper {
	public static String here() {
		return new Throwable().getStackTrace()[1].getMethodName();
	}
}

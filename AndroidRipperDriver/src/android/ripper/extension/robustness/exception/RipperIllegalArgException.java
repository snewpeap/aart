package android.ripper.extension.robustness.exception;

import it.unina.android.ripper.driver.exception.RipperRuntimeException;

public class RipperIllegalArgException extends RipperRuntimeException {
	public RipperIllegalArgException(Class<?> sourceClass, String method, String whichArg, String argToString) {
		super(sourceClass, method, "Illegal argument " + whichArg + "=" + argToString);
	}
}

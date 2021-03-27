package android.ripper.extension.robustness.exception;

import it.unina.android.ripper.driver.exception.RipperRuntimeException;

public class RipperNullMsgException extends RipperRuntimeException {
	public RipperNullMsgException(Class<?> sourceClass, String method, String whichMethod) {
		super(sourceClass, method, whichMethod + " returns null message");
	}
}

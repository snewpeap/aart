package android.ripper.extension.robustness.exception;

import it.unina.android.ripper.driver.exception.RipperRuntimeException;

public class NullMsgException extends RipperRuntimeException {
	public NullMsgException(Class<?> sourceClass, String method, String whichMethod) {
		super(sourceClass, method, whichMethod + " returns null message");
	}
}

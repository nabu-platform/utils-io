package be.nabu.utils.io.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IORuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1283065211903611257L;
	private List<Exception> suppressedExceptions = new ArrayList<Exception>();

	public IORuntimeException() {
		super();
	}

	public IORuntimeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public IORuntimeException(String arg0) {
		super(arg0);
	}

	public IORuntimeException(Throwable arg0) {
		super(arg0);
	}

	public void addSuppressedException(Exception...exception) {
		suppressedExceptions.addAll(Arrays.asList(exception));
	}
	
	public List<Exception> getSuppressedExceptions() {
		return suppressedExceptions;
	}
}

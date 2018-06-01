package ro.cst.tsearch.utils;

public class FormatException extends Exception {

	public FormatException() {}
	public FormatException(String msg) {
		super(msg);
	}
	
	public FormatException(Throwable ex) {
		super(ex);
	}
	public FormatException(Exception ex) {
		super(ex);
	}

}

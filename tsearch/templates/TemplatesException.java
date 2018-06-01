
package ro.cst.tsearch.templates;

public class TemplatesException extends Exception{
	static final long serialVersionUID=0xFAFAFAAA;
	public TemplatesException() {}
	public TemplatesException(String msg) {
		super(msg);
	}
	
	public TemplatesException(Throwable ex) {
		super(ex);
	}
	public TemplatesException(Exception ex) {
		super(ex);
	}
}

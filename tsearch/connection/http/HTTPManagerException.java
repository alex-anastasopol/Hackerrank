package ro.cst.tsearch.connection.http;

public class HTTPManagerException extends Exception
{
	static final long serialVersionUID = 1;
	
	String msg;
	
	HTTPManagerException(String msg)
	{
		this.msg = msg;
	}
	
	public String toString()
	{
		System.err.println(msg);
		return super.toString();
	}
}

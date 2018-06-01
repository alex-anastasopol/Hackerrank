package ro.cst.tsearch.pdftiff;

public class DPError 
{		
	String message = null;
	int code;
	
	public DPError( int code, String message )
	{
		this.code = code;
		this.message = message;
	}
	
	public DPError( int code )
	{
		this.code = code;	
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public int getCode()
	{
		return code;
	}
}

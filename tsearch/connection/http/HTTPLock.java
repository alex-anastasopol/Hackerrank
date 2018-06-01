package ro.cst.tsearch.connection.http;

public class HTTPLock
{
	private boolean locked = false;

	public void lock()
	{
		locked = true;		
	}
	
	public void unlock()
	{		
		locked = false;
	}
	
	public boolean isLocked()
	{
		return locked;
	}
}

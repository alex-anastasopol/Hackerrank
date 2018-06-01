package ro.cst.tsearch.pdftiff.util;


public class Chronometer 
{
	private long start = 0, stop = 0;	
	
	public Chronometer()
	{
		start = System.currentTimeMillis();
	}
	
	public void stop()
	{
		stop = System.currentTimeMillis();
	}
	
	public long get()
	{
		return stop - start;
	}
}

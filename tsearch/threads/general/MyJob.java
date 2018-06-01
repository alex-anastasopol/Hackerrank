package ro.cst.tsearch.threads.general;

public interface MyJob
{
	/**
	 * Method Execute.
	 */
	public void Execute(boolean vbIsLastJob);
	public int getPriority();
	public boolean equals(Object o);
	}

package ro.cst.tsearch.threads.general;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Category;
///////
public abstract class JobsSolverThread
{
	protected static final Category logger= Category.getInstance(JobsSolverThread.class.getName());
	protected int SLEEP_FOR=1000;//sleep for 1 second
	private SortedSet jobs;
	private boolean bIsWorking=false;
	///////////////////////////////////////////////////////////////////////////
	//Start Thread
	protected void StartThread(String sThreadName, Comparator JobsComparator)
	{
		jobs=Collections.synchronizedSortedSet(new TreeSet(JobsComparator));
		InitThread(sThreadName);		
	}
	protected void  StartThread(String sThreadName)
	{
		jobs=Collections.synchronizedSortedSet(new TreeSet());
		InitThread(sThreadName);
	}
	///////////////////////////////////////////////////////////////////////////
	public final void addJob(MyJob job)
	{
		logger.info("Adding one to : " + new Integer(jobs.size()).toString() );
		jobs.add(job);
		logger.info("Now there are: " + new Integer(jobs.size()).toString()  );
	}
	///////////////////////////////////////////////////////////////////////////
	public final boolean isEmpty()
	{
		logger.info("Testing if it is empty: " + new Integer(jobs.size()).toString()  );
		return	(jobs.isEmpty() && (!bIsWorking));
	}
	///////////////////////////////////////////////////////////////////////////
	private void InitThread(String sThreadName)
	{
		//		Thread thread= new Thread(this);
		Thread thread= new Thread(new Runnable()
		{
			public void run()
			{
				Worker();
			}
		});
		thread.setName(sThreadName);
		thread.start();
	}
	/////job solver
	private void doJob(MyJob job, boolean vbIsLastJob)
	{
		try
		{
			job.Execute(vbIsLastJob);
			Thread.sleep(300); //hehe....ca sa nu se blocheze tabela aia de tot
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	private void Worker()
	{
		while (true)
		{
			MyJob job= null;
			try{
				bIsWorking=true;
				job= (MyJob) jobs.first() ; 
				jobs.remove(job);
				logger.info("One taken - remained: " + new Integer(jobs.size()).toString()  );
			}catch(NoSuchElementException e){}
			catch(Exception e1){//ignore it
				e1.printStackTrace() ;
			}
			if (job != null)
			{
				logger.info("Executing:"  );
				doJob(job,jobs.isEmpty());
				logger.info("Executed: "  );
				bIsWorking=false;
			}
			else
			{
				bIsWorking=false;
				try
				{
					Thread.sleep(SLEEP_FOR); 
				}
				catch (Exception e2)
				{
					e2.printStackTrace();
				}
			}
		}
	}
}

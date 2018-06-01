package ro.cst.tsearch.threads;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.*;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.ReflectionUtils;

public class ASMaster {

    private static ASMaster master = new ASMaster();
    //static Monitor monitor;
    static Random random = new Random();
    
    private ASMaster() {
        
        /*monitor = new Monitor();
		monitor.setDaemon(true);
		monitor.start();*/
    }

    public static final int ABORT_EXISTING		=	1;
    public static final int JOIN_EXISTING		=	2;
    public static final int START_NEW			=	3;
    public static final int BACKGROUND_SEARCH	=	4;
    
    public static ASThread startSearch(Search search, int mode, String continueSearch, String goBackOneLevel, User user) {

        ASThread asThread = null;
        
		//();
        
        switch (mode) {
        
        	case ABORT_EXISTING :
        	    
        	    if (getSearch(search) != null) {
                    stopSearch(search);
                    removeSearch(search);
                }
        	    
        	    asThread = new ASThread(search, user);
        	    
        	    asThread.continueSearch = continueSearch;
        		asThread.goBackOneLevel = goBackOneLevel;
        		
        	    asThread.start();
                master.addThread(asThread);
                
        	    break;
        	    
        	case JOIN_EXISTING :
        	case BACKGROUND_SEARCH :
        	    
        	    boolean add = false;
        	    if ((asThread = getSearch(search)) == null) {
        	        asThread = new ASThread( search, user );
        	        add = true;
        	    }
        	    
        	    asThread.continueSearch = continueSearch;
        		asThread.goBackOneLevel = goBackOneLevel;
        		
        	    if (add) {
        	    	
        	        asThread.start();
        	        master.addThread(asThread);
        	    }
        	    
        	    break;
        	    
        	case START_NEW :
        	    
        	    asThread = new ASThread( search, user );
        	    
        	    asThread.continueSearch = continueSearch;
        		asThread.goBackOneLevel = goBackOneLevel;
        		
        	    asThread.start();
                master.addThread(asThread);
                
        	    break;
        
        }
        return asThread;
    }
    
    public static void startTestCaseSearch( Search search, User user, String sPath)
    {
        //();
        CommunityAttributes ca = null;
        
        try
        {
            ca = CommunityUtils.getCommunityFromId( user.getUserAttributes().getCOMMID().longValue() );
        }
        catch (BaseException e)
        {
            e.printStackTrace();
        }
        
        
		ASThread asThread = new ASThread(search, user, ca);
        
		asThread.continueSearch = "true";
        asThread.goBackOneLevel = "false";
        asThread.setTestCaseSearch();
                        
        asThread.start();
        master.addThread(asThread);
    }
    
    public static void startBackgroundSearch(Search search, User user, CommunityAttributes ca) {

        //();
        
		ASThread asThread = new ASThread(search, user, ca);
        
		asThread.interactive = false;
		asThread.continueSearch = "true";
        asThread.goBackOneLevel = "false";
        
        asThread.start();
        master.addThread(asThread);
    }

    public static void stopSearch(Search search) {

        ASThread thread = master.getThread(search);
        if (thread != null) {
            
            thread.setStarted(false);
            
            waitForSearch(search);
            
            /*while (thread.isAlive()) {
                try {
                    
                    thread.interrupt();
                    thread.stop();
                    
                    Thread.sleep(random.nextInt(500));
                    
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }*/
        }
        
    }

    public static void removeSearch(Search search) {
    	
        ASThread thread = master.removeThread(search);
        // clear all fields of thread, so that the memory is garbage-collected
        // even if the thread is not garbage-collected yet
        // for some reason the Thread garbage collection is delayed  
        ReflectionUtils.nullifyReferenceFields(thread);
    }
    
    public static ASThread getSearch(Search search) {

        return master.getThread(search);
    }
    public static ASThread getSearch(long searchID) {

        return master.getThread(searchID);
    }

    public static Hashtable getSearches() {
        return master.threads;
    }
    
    public static boolean waitForSearch(Search search) {

        ASThread thread = master.getThread(search);

        if( thread==null ){
        	return false;
        }
        
        synchronized (thread.notifier) {

            try {

                while (!thread.searchFinished) {
                    
                    thread.notifier.wait(500);
                    
                    //aici se poate adauga o verificare a statusului (pe ce server e, etc..)
                    
                    // verificam daca l-a omorat cineva
                    if (!thread.isAlive() && !thread.searchFinished) {                    
                        return false;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    } 
    
    ///////////////////////////
    

    private Hashtable threads = new Hashtable();
    
    private boolean addThread(ASThread thread) {

        try {
            threads.put(new Long(thread.getSearch().getSearchID()), thread);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private ASThread removeThread(Search search) {
        
        if (search == null)
            return null;
        
        try {

            return (ASThread) threads.remove(new Long(search.getSearchID()));
            
        } catch (Exception e) {
            return null;
        }
        
    }

    private ASThread getThread(Search search) {

        if (search == null)
            return null;
        
        return getThread(search.getSearchID());
    }
    private ASThread getThread(long searchID) {
        return (ASThread) threads.get(new Long(searchID));
    }
      
    protected class Monitor extends Thread {
		
		public void run() {
			
			setName("ASMaster Monitor");

			while (true) {

				try {

					synchronized (threads) {
					    
					   Enumeration e = threads.elements();
					   while (e.hasMoreElements()) {
					       
					       ASThread thread = (ASThread) e.nextElement();
					       if (!thread.isAlive()) {
					           master.removeThread(thread.getSearch());
					       }
					   }
					    
					}

					Thread.sleep(60000); // sleep 20 sec
				
				} catch (Exception ignored) {}
			}

		}
	}

	public static int getNoOfBackgroundAliveSearches() {
		int i=0;
		for(Object key:master.threads.keySet()){
			Long lkey = (Long)key;
			ASThread t = (ASThread)master.threads.get(lkey);
			if(t!=null&&t.isAlive()&&t.isBackgroundSearch()&&t.getSearch()!=null&&t.getSearch().isBackgroundSearch()){
				i++;
			}
		}
		return i;
	}
	
}
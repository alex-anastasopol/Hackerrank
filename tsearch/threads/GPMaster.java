package ro.cst.tsearch.threads;

import java.util.Hashtable;
import java.util.Iterator;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.User;


public class GPMaster {
    
    private GPMaster() {}
    
    private static Hashtable threads = new Hashtable(); 
    public static GPThread generateTSR(Search search, User user, CommunityAttributes ca) {
        
    	GPThread gptTemp = GPMaster.getThread(search.getID());
    	
    	if(gptTemp != null) {
    		return gptTemp;
    	}
    	
        GPThread thread = new GPThread(search, user, ca);
        
        if (thread != null) {
            threads.put(new Long(thread.createTime), thread);
        }
        
        return thread;
    }
    
    public static void removeThread(GPThread thread) {

        threads.remove(new Long(thread.createTime));
               
        //();
    }
    
    public static Hashtable getThreads() {
        return threads;
    }
    
    public static GPThread getThread(long searchId) {
    	for (Iterator i = threads.values().iterator(); i.hasNext();) {
    		GPThread thread = (GPThread) i.next();
    		// threadul se identifica dupa ID si nu dupa searchID.
			if (thread.getOriginalSearch().getID() == searchId)
				return thread;
		}
    	return null;
    }
    
    public static boolean waitForThread(GPThread thread) {

        synchronized (thread.notifier) {

            try {

                while (!thread.conversionFinished) {
                    
                    thread.notifier.wait(1000);
                    
                    // verificam daca l-a omorat cineva
                    if (!thread.isAlive() && !thread.conversionFinished) {                    
                        return false;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    } 
    
   

}

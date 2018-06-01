/*
 * Created on Apr 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.connection;

import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * @author
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CookieManager {
    private static Hashtable h = new Hashtable();

//    private static Lock l = new Lock();

    private static final int timeout = 1; //minutes

    private static Calendar watchdogTimer = Calendar.getInstance();

    public static synchronized void addCookie(String key, String cookie) {
//        l.lock();
        if (cookie != null) {
            ATSCookie c = new ATSCookie(cookie);
            h.put(key, c);
        } else
            h.remove(key);
        //doCleanup();
//        l.releaseLock();
    }

    public static synchronized String getCookie(String key) {
        String ret = "";
//        l.lock();
        ATSCookie c = (ATSCookie) h.get(key);
        if (c != null)
            ret = c.getCookie();
        //doCleanup();
//        l.releaseLock();
        return ret;
    }

    public static synchronized void resetCookie( String key, boolean perfectMatch )
    {
        /*
         * reset the stored cookies
         * if perfectMatch is true, only the cookie that matches perfecly to key will be reset
         * if perfectMatch is false, all the cookies that contain "key" will be reset
         */
//        l.lock();
        
        /*
         * try - catch to prevent deadlock
         */
        try
        {
            if( perfectMatch )
            {
                //remove the cookie associated with key
                h.remove( key );
            }
            else
            {
                //get all the keys
                Enumeration allKeys = h.keys();
                
                //search for "key" within all the keys
                while( allKeys.hasMoreElements() )
                {
                    String keyEntry = (String) allKeys.nextElement();
                    
                    if( keyEntry.indexOf( key ) != -1 )
                    {
                        //found a partial match
                        h.remove( keyEntry );
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
//        l.releaseLock();
    }
    
    public static boolean isCookieValid(String key) {

        ATSCookie c = (ATSCookie) h.get(key);

        if (c == null)
            return false;

        if (c.getCookie() == null || "".equals(c.getCookie()))
            return false;

        long t = c.getCtime().getTimeInMillis();
        long crt = Calendar.getInstance().getTimeInMillis();
        //if older than 20 min.
        if ((crt - t) > 20 * 60 * 1000)
            return false;

        return true;
    }

    /**
     *  
     */
    private static void doCleanup() {
        // TODO Auto-generated method stub
        Calendar crttime = Calendar.getInstance();
        crttime.add(Calendar.MINUTE, -timeout);
        if (crttime.after(watchdogTimer)) {
            crttime = Calendar.getInstance();
            Collection cc = (Collection) h.keySet();
            Iterator i = cc.iterator();
            Calendar time;
            while (i.hasNext()) {
                String key = (String) i.next();
                ATSCookie c = (ATSCookie) h.get(key);
                time = c.getCtime();
                time.add(Calendar.MINUTE, timeout);
                if (time.before(crttime))
                    h.remove(key);
            }
            watchdogTimer = Calendar.getInstance();
        }
    }
}
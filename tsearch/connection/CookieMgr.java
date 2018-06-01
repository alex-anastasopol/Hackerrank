package ro.cst.tsearch.connection;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class CookieMgr {
    
    private static Hashtable cookies = new Hashtable();
    private CookieMgr() {}
    
    public static void removeCookie(String server) {
    	if (cookies.containsKey(server))
    		cookies.remove(server);
    }
    
    public static void resetCookie( String key, boolean perfectMatch )
    {
        /*
         * reset the stored cookies
         * if perfectMatch is true, only the cookie that matches perfecly to key will be reset
         * if perfectMatch is false, all the cookies that contain "key" will be reset
         */
        
        /*
         * try - catch to prevent deadlock
         */
        try
        {
            if( perfectMatch )
            {
                //remove the cookie associated with key
                cookies.remove( key );
            }
            else
            {
                //get all the keys
                Enumeration allKeys = cookies.keys();
                
                //search for "key" within all the keys
                while( allKeys.hasMoreElements() )
                {
                    String keyEntry = (String) allKeys.nextElement();
                    
                    if( keyEntry.indexOf( key ) != -1 )
                    {
                        //found a partial match
                        cookies.remove( keyEntry );
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public static void addCookie(String server, Object obj) {
        
        if (obj == null)
            return;
    
        String cookie = (String) cookies.get(server);
        if (cookie == null)
            cookie = "";
        
        String newCookie = obj.toString();
        
        newCookie = newCookie.replaceAll("\\[|\\]", "");
        newCookie = newCookie.replaceAll("; path=/", "");
        newCookie = newCookie.replaceAll("; expires=.*", "");
        newCookie = newCookie.replaceAll(",", ";");
        
        if (newCookie.indexOf("Session") != -1 && cookie.indexOf("Session") != -1) {
            cookie = cookie.replaceAll("(.*)(Session[0-9]?=\\w+)", "$1" + newCookie);
        } else if (cookie.indexOf(newCookie) == -1) {
            cookie += (cookie.length() > 0 ? "; " : "") + newCookie;
        }
        
        cookies.put(server, cookie);
        
        //System.err.println("Set-Cookie:" + cookie);
    }
    
    public static String getCookie(String server) {
        
        System.err.println("Get-Cookie:" + cookies.get(server));
        
        return (String) cookies.get(server);
        
    }
    
    public static String prepareCookie(String cookie, Object obj) {
        
        if (cookie == null)
            cookie = "";
        if (obj==null)
        	obj="";
        String newCookie = obj.toString();
        
        newCookie = newCookie.replaceAll("\\[|\\]", "");
        newCookie = newCookie.replaceAll("; path=/", "");
        newCookie = newCookie.replaceAll("; expires=.*", "");
        newCookie = newCookie.replaceAll(",", ";");
        
        if (newCookie.indexOf("Session") != -1 && cookie.indexOf("Session") != -1) {
            cookie = cookie.replaceAll("(.*)(Session=\\w+)", "$1" + newCookie);
        } else if (cookie.indexOf(newCookie) == -1) {
            cookie += (cookie.length() > 0 ? "; " : "") + newCookie;
        }
        
        return cookie;
    }
    
}

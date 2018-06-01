package ro.cst.tsearch.connection.http;

import java.util.LinkedList;

import org.apache.commons.httpclient.HttpClient;

public class HTTPSession extends HTTPLock implements Comparable 
{
    protected int SID;
    protected HTTPSession(int id)
    {
        this.SID = id;
        httpClient = HTTPManager.createHttpClient();            
    }
    
    public long lastRequestTime = 0;
    LinkedList requestTimes = new LinkedList(); 
    
    long requests = 0;
    protected HttpClient httpClient;
    private String lastRequest = null;
    
    public static final int STATUS_NOT_KNOWN = 0;
    public static final int STATUS_LOGGING_IN = 1;
    public static final int STATUS_LOGGED_IN = 2;
    private int status = STATUS_NOT_KNOWN;
    
    public int getStatus()
    {
        return status;
    }

    public int compareTo(Object o)
    {
        if ( requests == ((HTTPSession) o).requests && SID == ((HTTPSession) o).SID  )
            return 0;
        else
            if (requests > ((HTTPSession) o).requests)
                return 1;
            else
                return -1;          
    }
    
    //boolean loggedIn = false;
    
    /*public boolean isLoggedIn()
    {
        return loggedIn;
    }*/

    /*public void setLoggedIn(boolean loggedIn)
    {
        HTTPManager.log( this + " logged in : " + loggedIn);
        this.loggedIn = loggedIn;
    }*/

    public String getLastRequest()
    {
        return lastRequest;
    }

    public void setLastRequest(String lastRequest)
    {
        this.lastRequest = lastRequest;     
    }

    public String getSID()
    {
        return String.valueOf(SID);
    }

    public void setStatus(int status)
    {
        this.status = status;       
    }
    
    public String toString()
    {
        return 
            "[" + super.toString().substring(super.getClass().getName().length()+1)
            + " locked: " + isLocked() + " loggedin: " + getStatus() + "]";
    }
}

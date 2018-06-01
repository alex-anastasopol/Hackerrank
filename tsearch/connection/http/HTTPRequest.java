package ro.cst.tsearch.connection.http;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.httpclient.methods.multipart.Part;

import ro.cst.tsearch.servers.bean.DataSite;

public class HTTPRequest implements Serializable
{
	public static final String USER_AGENT_DEFAULT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)";
	
	private HTTPResponse bypassResponse = null;
	
	public void setBypassResponse(HTTPResponse response){
		this.bypassResponse = response;
	}
	
	public HTTPResponse getBypassResponse(){
		return bypassResponse;
	}
	
	private static final long serialVersionUID = 2115800624235834423L;

	public class ParametersVector extends Vector {
		
		public String toString() {
			
			StringBuffer sb = new StringBuffer();
			for ( int i = 0; i < size(); i++ ) {
				
				if ( i > 0 )
					sb.append( "," );
				
				sb.append( elementAt( i ) );
			}
			
			return sb.toString();
		}
		
		public String getFirstParameter() {
			if( size() > 0 )
	        {
	            return (String) elementAt( 0 );
	        }
	        
	        return null;
		}
	}
    protected boolean inQueue = false;
    public boolean noRedirects = false;
    private boolean replaceSpaceOnredirect = false;
    protected boolean isWaiting = false;
    protected long searchId;
    //protected String siteName;
    protected DataSite dataSite;
    
    public String entity = "";
    public static int GET = 0;
    public static int POST = 1;
    public HashMap headers = new HashMap();
    public HashMap<String, ParametersVector> postParameters = new HashMap<String, ParametersVector>();
    
    private String redirectLocation = "";
    
    //override default mode to post parameters
    //if this variable is non null, it will be used instead of the other post parameters
    private String xmlPostData = null;
    
    //used for multipart/form-data
    private Part[] partPostData = null;
    
    public Vector<String> postParameterOrder = null;
    
    private transient HTTPSiteInterface httpSite;
    
    public long enq, start, leave;
    ///////////////
    protected int method = GET;
    public int getMethod()
    {
        return method;
    }
    
    public void setMethod(int method)
    {
        this.method = method;
        
        if ( method == POST )
        {
            setHeader("Content-Type", "application/x-www-form-urlencoded");
        }
    }
    
    String URL;

    public String getURL()
    {
        return URL;
    }

    public void  setURL(String url)
    {
        URL=url;
    }

    /**
     * Use this method only in onBeforeRequest to modify the URL, don't use the same HTTPRequest object twice by changing the URL
     */
    public void modifyURL(String url)
    {
        URL = url;
    }
    
    public void clearPostParameters(){
        this.postParameters.clear();
    }
    
    public void clearHeaders(){
        this.headers.clear();
    }
    
    public void setHeaders(HashMap m){
        this.headers=m;
    }
    
    public HashMap getHeaders(){
        return headers;
    }
    
    public HTTPRequest(String url)
    {
    	url = url.replace(" ", "%20");
    	
        setHeader("User-Agent", USER_AGENT_DEFAULT);
        setHeader("Accept","image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, */*");
        setHeader("Accept-Language","en-us");
        setHeader("Connection","Keep-Alive");
        
        URL = url;
    }
    public HTTPRequest(String url, int method)
    {
    	this(url);
    	setMethod(method);
    }
    
    public void setHeader(String name, String value)
    {
        headers.put(name, value);       
    }
    
    public String getHeader(String name)
    {
        return (String) headers.get( name );
    }

    public void addPostParameters(Map<String,String> params){
    	for(Map.Entry<String, String> entry: params.entrySet()){
    		setPostParameter(entry.getKey(), entry.getValue());
    	}
    }
    
    public void setPostParameter(String name, String value)
    {
    	ParametersVector paramValues = postParameters.get( name );
        boolean newParam = false;
        
        if( paramValues == null )
        {
            paramValues = new ParametersVector();
            newParam = true;
        }
        
        if( !paramValues.contains( value ) )
        {
            paramValues.add( value );
        }
        
        if( newParam )
        {
            postParameters.put( name, paramValues );
        }
    }
    
    public void setPostParameter(String name, String value, boolean removeTheOldValue){
    	if (!removeTheOldValue){
    		setPostParameter(name, value);
    	}else{
    		removePostParameters(name);
    		setPostParameter(name, value);
    	}
    }
    
    public void setPostParametersVector( String name, ParametersVector values )
    {
        postParameters.put( name, values );
    }
    
    
    public void setPostParameters(HashMap<String, ParametersVector> postParameters)
    {
        this.postParameters = postParameters;
    }
    
    public ParametersVector getPostParameter(String name)
    {
        return postParameters.get(name);               
    }
    
    public boolean hasPostParameter(String name){
    	return postParameters.containsKey(name);
    }
    
    public String getPostFirstParameter(String name)
    {
    	ParametersVector allValues = postParameters.get( name );
        
        if( allValues != null && allValues.size() > 0 )
        {
            return (String) allValues.elementAt( 0 );
        }
        
        return null;
    }
    
    public void removePostParameters( String name )
    {
        postParameters.remove( name );
    }
    
    public HashMap<String, ParametersVector> getPostParameters()
    {
        return postParameters;              
    }
    
    public String toString(){
        
        String ret="";
        if(this.method==GET) 
            ret+= "Type = GET   URL="+this.getURL()+"\n";
        else 
            ret+= "Type = POST   URL="+this.getURL()+"\n";
        ret+="........... HEADER PARAMETERS ............\n";
        if(headers!=null){
            java.util.Iterator it = this.headers.entrySet().iterator(); 
            if(it !=null)
            while(it.hasNext()){
                Entry en=(Entry )it.next();
                if(en!=null)
                    ret+=en.getKey()+"           "+en.getValue()+"\n";
            }
        }
        ret+=".......... POST PARAMETERS .............\n";
        if(postParameters!=null){
            java.util.Iterator it = this.postParameters.keySet().iterator();  
            if(it != null)
            while(it.hasNext()){
                String paramName = ( String )it.next();
                
                if(paramName!=null)
                {
                	ParametersVector paramValues = this.postParameters.get( paramName );
                    if( paramValues != null )
                    {
                        for( int i = 0 ; i < paramValues.size() ; i ++ )
                        {
                            ret += paramName + "           " + (String)paramValues.elementAt( i ) + "\n";
                        }
                    }
                }
            }
            
        }
        
        return ret;
    }
    
    public Object removeHeader(String key){
        return headers.remove(key);
    }
    
   
    
    public void setHttpSite( HTTPSiteInterface httpSite )
    {
        this.httpSite = httpSite;
    }
    
    public HTTPSiteInterface getHttpSite()
    {
        return this.httpSite;
    }
    
    public synchronized void setInQueue( boolean inQueue )
    {
        this.inQueue = inQueue;
    }
    
    public synchronized boolean getInQueue()
    {
        return this.inQueue;
    }
    
    public synchronized void setIsWaiting( boolean isWaiting )
    {
        this.isWaiting = isWaiting;
    }
    
    public synchronized boolean getIsWaiting()
    {
        return this.isWaiting;
    }
    
    public void setEntity(String entity){
    	this.entity = entity; 
    }
    
    public String getEntity(){
    	return entity; 
    }
    
    public void setPostParameterOrder( Vector<String> postParameterOrderVector ){
    	this.postParameterOrder = postParameterOrderVector;
    }
    
    public Vector<String> getPostParameterOrder(){
    	return this.postParameterOrder;
    }
    
    public void setXmlPostData( String xmlPostData ){
    	this.xmlPostData = xmlPostData;
    }
    
    public String getXmlPostData(){
    	return this.xmlPostData;
    }

    /**
     * set multipart/form-data
     */
    
    public void setPartPostData(Part[] partPostData ){
    	this.partPostData = partPostData;
    }
    /**
     * get multipart/form-data
     */
    public Part[] getPartPostData(){
    	return this.partPostData;
    }
    
	public boolean isReplaceSpaceOnredirect() {
		return replaceSpaceOnredirect;
	}

	public void setReplaceSpaceOnredirect(boolean replaceSpaceOnredirect) {
		this.replaceSpaceOnredirect = replaceSpaceOnredirect;
	}

	public String getRedirectLocation(){
		return redirectLocation;
	}
	
	public void setRedirectLocation( String redirectLocation ){
		this.redirectLocation = redirectLocation;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public DataSite getDataSite() {
		return dataSite;
	}

	public void setDataSite(DataSite dataSite) {
		this.dataSite = dataSite;
	}
	
	public void setReferer(String referer) {
		setHeader("Referer", referer);
	}

	/*
	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}
	*/
}

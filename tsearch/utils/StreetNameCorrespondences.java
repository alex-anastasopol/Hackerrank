package ro.cst.tsearch.utils;

import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.log4j.Category;


public class StreetNameCorrespondences
{
	private Hashtable<String,String> correspList = null;
    
    protected static final Category logger = Category.getInstance(StreetNameCorrespondences.class.getName());
    
    private static StreetNameCorrespondences correspInstance = null;    
    
    private long searchId = -1;
    
    private StreetNameCorrespondences(long searchId)
    {	this.searchId = searchId;
    	correspList = new Hashtable<String,String>();
 
        
    	correspList.put("OAK KNL", "OAK KNOLL");
    	correspList.put("OAK KNOLL", "OAK KNL");
    	correspList.put("OAKHILL", "OAK HILL");
    	correspList.put("OAK HILL", "OAKHILL");
        correspList.put("WILDGROVE", "WILD GROVE");
        correspList.put("WILD GROVE", "WILDGROVE");
        correspList.put("OAKCREST", "OAK CREST");
        correspList.put("OAK CREST", "OAKCREST");
        correspList.put("RIVERVIEW", "RIVER VIEW");
        correspList.put("RIVER VIEW", "RIVERVIEW");
        
    }
    
    public static synchronized StreetNameCorrespondences getInstance(long searchId)
    {
        if( correspInstance == null )
        {
        	correspInstance = new StreetNameCorrespondences(searchId);
        }
        
        return correspInstance;
    }  
    
    public String getCorrespondent(String originalStr)
    {
    	if(originalStr == null)
    		return "";
        /*
         * returns the correspondent of originalStr using the correspondent lists
         */
        String correspondent = "";
        
        if (correspList.containsKey(originalStr)){
        	correspondent = correspList.get(originalStr);
        } 
        
        if( !correspondent.equalsIgnoreCase( originalStr ) )
        {
            IndividualLogger.info( "StreetNameCorrespondence: " + originalStr + " replaced with " + correspondent, searchId );
        }
        
        return correspondent.trim();
    }
  
    public boolean hasCorrespondence(String originalStr){
    	
    	boolean hasCorresp = false;
    	if(originalStr == null)
    		hasCorresp = false;
    	
	    if (correspList.containsKey(originalStr)){
	    	hasCorresp =  true;
	    } else {
	    	hasCorresp = false;
	    }
    	
    	return hasCorresp;
    }
}
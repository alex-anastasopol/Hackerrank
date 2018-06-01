package ro.cst.tsearch.search.filter.newfilters.name;

import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;

public class NameFilterForNext extends GenericNameFilter {
	private static final long serialVersionUID 	= 1000000001L;
	protected static final Category logger 		= Logger.getLogger(NameFilterForNext.class);
	private static final int LOW_THRESHOLD		= 10;
	
	public NameFilterForNext(long searchId){
		super("", searchId);
	}
	
	public NameFilterForNext(String key, long searchId,TSServerInfoModule module, boolean ignoreSuffix){
		super(key, searchId, false, module, ignoreSuffix);
	}
	
	public NameFilterForNext(String key, long searchId, boolean useSubdivisionName, TSServerInfoModule module, boolean ignoreSuffix){
		super(key, searchId, useSubdivisionName, module, ignoreSuffix); 
	}
	
	public NameFilterForNext(String key, long searchId,boolean useSubdivisionName,TSServerInfoModule module, 
			boolean ignoreSuffix, int stringCleaner){
		super(key, searchId, useSubdivisionName, module, ignoreSuffix, stringCleaner);
	}
	
    @SuppressWarnings("rawtypes")
	protected void analyzeResult(ServerResponse sr, Vector rez) throws ServerResponseException
    {           
    	int initialCount = sr.getParsedResponse().getInitialResultsCount();
    	int threshold = Math.min(LOW_THRESHOLD, initialCount/2);
        if( (initialCount - rez.size()) >= threshold)
        {
            //filtered --> stop, do not go to next results
            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
        }
    }
	
	

	@Override
	public String getFilterName() {
		return "Filter For Next";
	}
	
}
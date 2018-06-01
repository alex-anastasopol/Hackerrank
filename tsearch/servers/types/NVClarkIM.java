package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

public class NVClarkIM extends TSServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NVClarkIM(long searchId) {
    	super(searchId);
    }
	
	public NVClarkIM(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		ParsedResponse parsedResponse = Response.getParsedResponse();
    	String rsResponce = Response.getResult();
    	logger.debug(rsResponce);
    	
    	String errorMessage = "Image could not be downloaded";
    	
    	if(rsResponce.startsWith("Message: ")) {
    		errorMessage = rsResponce.substring(9);
    	}
    	
    	Response.setError(errorMessage);
    	parsedResponse.setError(errorMessage);
		
    	/**
    	 * Clean HTML response
    	 */
		Response.setResult("");
		parsedResponse.setResponse("");
	}

}

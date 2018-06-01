package ro.cst.tsearch.servers.types;



import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
* @author mihaib
**/

public class TXJohnsonTR extends TXGenericSMediaTR {

	public static final long serialVersionUID = 10000000L;
	
	public TXJohnsonTR(long searchId) {
		super(searchId);
	}

	public TXJohnsonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// change account number if necessary
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
        	String pid = module.getFunction(0).getParamValue();

       		pid = pid.replaceAll("(?is)\\p{Punct}", "");
       		pid = pid.replaceAll("(?is)\\s+", "");
       		pid = pid.replaceAll("(?is)(\\d{3})(\\d{4})(\\d{5})", "$1-$2-$3");

           	module.getFunction(0).setParamValue(pid);
          
        }
        return super.SearchBy(module, sd);
	}
	

}
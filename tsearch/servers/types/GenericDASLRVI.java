package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;



/**
 * @author MihaiB 
 */
public class GenericDASLRVI extends GenericDASLRV implements TSServerROLikeI {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7451493664862862170L;

	public GenericDASLRVI(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	public GenericDASLRVI(long searchId) {
		super(searchId);
	}
	
	public void afterDownloadImage(boolean downloaded){
		afterDownloadImage(downloaded, GWTDataSite.RV_TYPE);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {}
}

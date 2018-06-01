package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

public class TXDentonTR extends TXGenericSMediaTR{
	
	/**
	 * @author mihaib
	 */
	private static final long serialVersionUID = 3922610441378189743L;

	public TXDentonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {

		// remove fist letter and add DEN at the end of the pin 
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
        	String pin = module.getFunction(0).getParamValue();
        	if (pin.matches("(?is)\\A[A-Z].*")){
        		pin = pin.replaceAll("(?is)[A-Z](\\d+)", "$1");
        		if (!pin.endsWith("DEN")){
	        		if (pin.length() == 6){
	        			pin += "DEN";
	        		} else if (pin.length() > 6){
	        			pin = pin.substring(0, 6) + "DEN";
	        		}
        		}
				module.getFunction(0).setParamValue(pin.trim());
			}
          
        }
        return super.SearchBy(module, sd);
	}
	
	@Override
	protected String cleanBaseLink(){
		String baseLink = getBaseLink();
		if (StringUtils.isEmpty(baseLink)){
			return "";
		} else {
			int idx = baseLink.indexOf(".com");
			if(idx == -1){
				throw new RuntimeException("Cannot clean the base link");
			}
			return baseLink.substring(0, idx) + ".com/taxwebsite/";
		}
	}
	
}

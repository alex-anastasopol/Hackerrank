package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.utils.StringUtils;

public class TXTarrantTR extends TXGenericSMediaTR{
	
	/**
	 * @author mihaib
	 */
	private static final long serialVersionUID = 3922610441378189743L;

	public TXTarrantTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
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
			return baseLink.substring(0, idx) + ".com/";
		}
	}
	
}

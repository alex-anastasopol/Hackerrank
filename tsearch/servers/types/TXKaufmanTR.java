package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ServerResponse;

public class TXKaufmanTR extends TXGenericAO{

	private static final long serialVersionUID = 6332556775421206541L;


	public TXKaufmanTR(long searchId) {
		super(searchId);
	}

	public TXKaufmanTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	} 
	

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put("OtherInformationSet.SrcType","TR");
		return super.parseAndFillResultMap(response,detailsHtml,map);
	}
	
	protected int getStartYear(){
		return 2007;
	}
	
}

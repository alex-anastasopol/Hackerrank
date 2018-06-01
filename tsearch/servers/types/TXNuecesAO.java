package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ServerResponse;

public class TXNuecesAO extends TXGenericSite{

	private static final long serialVersionUID = 679493777119984078L;

	public TXNuecesAO(long searchId) {
		super(searchId);
	}

	public TXNuecesAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	} 
	

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put("OtherInformationSet.SrcType","AO");
		return super.parseAndFillResultMap(response,detailsHtml,map);
	}
	
}
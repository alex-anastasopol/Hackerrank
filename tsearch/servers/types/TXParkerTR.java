package ro.cst.tsearch.servers.types;


import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * @author mihaib
  */

public class TXParkerTR extends TXGenericSWData {

	public static final long serialVersionUID = 10583750000L;
	
	
	public TXParkerTR(long searchId) {
		super(searchId);
	}

	public TXParkerTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put("OtherInformationSet.SrcType","TR");
		return super.parseAndFillResultMap(response,detailsHtml,map);
	}
}
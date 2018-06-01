package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ServerResponse;

public class TXKendallTR extends TXGenericAO {

	private static final long serialVersionUID = 679493777119984078L;

	public TXKendallTR(long searchId) {
		super(searchId);
	}

	public TXKendallTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put("OtherInformationSet.SrcType", "TR");
		return super.parseAndFillResultMap(response, detailsHtml, map);
	}


}

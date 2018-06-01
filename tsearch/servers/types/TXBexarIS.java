package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ServerResponse;




public class TXBexarIS extends TXGenericAO{
	
	private static final long serialVersionUID = -211586944970644086L;

	public TXBexarIS(long searchId) {
		super(searchId);
	}

	public TXBexarIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	} 


	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put("OtherInformationSet.SrcType","AO");	//used as AO
		return super.parseAndFillResultMap(response,detailsHtml,map);
	}
	
	@Override
	protected int getStartYear(){
		return 2006;
	}
	
}

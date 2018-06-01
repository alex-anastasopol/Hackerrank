package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;

/**
 * Implementation for TN Spring Hill (TN Williamson YD)
 */

public class TNSpringHill extends TNGenericCityCT {
	
	private static final long serialVersionUID = -756527404819642467L;

	public TNSpringHill(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected String getSpecificCntySrvName() {
		return super.getSpecificCntySrvName().replaceFirst("(?i)^spring( )?hill$", "cityofspringhill");
	}
	
	@Override
	public ResultMap parseIntermediary(TableRow row, long searchId)	throws Exception {
		ResultMap resultMap = super.parseIntermediary(row, searchId);
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "YD");
		resultMap.removeTempDef();
		return resultMap;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForAutoSearch(l);
	}

}

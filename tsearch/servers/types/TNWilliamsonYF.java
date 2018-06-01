package ro.cst.tsearch.servers.types;

import java.util.Collection;
import java.util.HashMap;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 4, 2012
 */

public class TNWilliamsonYF extends TNWilliamsonTR {
	// Nolensville

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 6202025197715943008L;

	public TNWilliamsonYF(long searchId) {
		super(searchId);
	}

	public TNWilliamsonYF(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		
		if(StringUtils.isEmpty(city))
			return;
		
		if (!StringUtils.isEmpty(city)) {
			if (!city.startsWith(getDataSite().getCityName().toUpperCase())) {
				return;
			}
		}
		
		super.setModulesForAutoSearch(serverInfo);
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Collection<ParsedResponse> intermediaryResponse = super.smartParseIntermediary(response, table, outputTable);

		return intermediaryResponse;
	}

	protected void putSrcType(ResultMap m) {
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "YF");
	}

	protected void loadDataHash(HashMap<String, String> data, String taxYear) {
		if (data != null) {
			data.put("type", "CITYTAX");
			data.put("year", taxYear);
		}
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		return super.parseAndFillResultMap(response, detailsHtml, map);
	}
}

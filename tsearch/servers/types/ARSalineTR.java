package ro.cst.tsearch.servers.types;



import static ro.cst.tsearch.datatrace.Utils.setupMultipleSelectBox;
import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.util.HashMap;

import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.RegExUtils;

public class ARSalineTR extends ARGenericCountyDataAOTR {
	
	private static String SUBDIVISION_SELECT = "";
	private static String MARKET_AREA_SELECT = "";
	private static String SCHOOL_DISTRICT_SELECT = "";
	private static String IMPROVEMENT_DISTRICT_SELECT = "";
	private static String USE_CODE_SELECT = "";
	
	
	
	static {
		String selectFile = "ARSalineSubdivisionList.xml";
		String selects = getHtmlSelectFromFile(selectFile);
		SUBDIVISION_SELECT = selects;
		
		selectFile = "ARSalineMarketAreaList.xml";
		selects = getHtmlSelectFromFile(selectFile);
		MARKET_AREA_SELECT = selects;
		
		selectFile = "ARSalineSchoolDistrictList.xml";
		selects = getHtmlSelectFromFile(selectFile);
		SCHOOL_DISTRICT_SELECT = selects;
		
		selectFile = "ARSalineImprovementDistrictList.xml";
		selects = getHtmlSelectFromFile(selectFile);
		IMPROVEMENT_DISTRICT_SELECT = selects;
		
		selectFile = "ARSalineUseCodesList.xml";
		selects = getHtmlSelectFromFile(selectFile);
		USE_CODE_SELECT = selects;
		
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);

		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(19), MARKET_AREA_SELECT);
			setupMultipleSelectBox(tsServerInfoModule.getFunction(20), SUBDIVISION_SELECT);
			setupSelectBox(tsServerInfoModule.getFunction(27), SCHOOL_DISTRICT_SELECT);
			setupSelectBox(tsServerInfoModule.getFunction(28), IMPROVEMENT_DISTRICT_SELECT);
			setupSelectBox(tsServerInfoModule.getFunction(31), USE_CODE_SELECT);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	
	public ARSalineTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type","CNTYTAX");
		data.put("dataSource","TR");
		return data;
	}
	
	@Override
	public int getSiteType() {
		return GWTDataSite.TR_TYPE;
	}

	@Override
	public String getCorrectPIN(String pid) {
		String regEx = "(\\d{3,3})(\\d{5,5})(\\d{3,3})";
		if (pid.matches(regEx)) {
			pid = pid.replaceAll(regEx, "$1-$2-$3");
		}
		return pid;
	}
	
}

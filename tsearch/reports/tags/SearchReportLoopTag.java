/**
 * 
 */
package ro.cst.tsearch.reports.tags;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.reports.comparators.OrderDateDayReportComparator;
import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.threads.CommAdminNotifier;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalUtils;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;

/**
 * @author radu bacrau
 *
 */
public class SearchReportLoopTag extends DayReportLoopTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SearchReportLoopTag.class);
	
	protected Object[] createObjectList() throws Exception {

		DayReportLineData[] ReportData = new DayReportLineData[0];
		if (logger.isDebugEnabled())
			logger.debug("createObjectList Start ...");

		//getting current user and community
		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		int commId = -1;
		String commIdStr=(String)ses.getAttribute("commId");
		commId = Integer.parseInt(commIdStr);
		// search current search window or all
		loadAttribute(RequestParams.REPORTS_SEARCH_ALL);
		
		
		//loading attributes from request
		loadAttribute(RequestParams.REPORTS_STATUS);	
		
		loadAttribute(RequestParams.REPORTS_DATE_TYPE);
		loadAttribute(RequestParams.REPORTS_STATE); //B3321
		//if (reportState.length == 1 && reportState[0] != -1)
		loadAttribute(RequestParams.REPORTS_COUNTY);
		
		if (!UserUtils.isAgent(ua)){	
			loadAttribute(RequestParams.REPORTS_AGENT);
			loadAttribute(RequestParams.REPORTS_ABSTRACTOR);
		}else{
			reportAbstractor[0] = ua.getID().intValue();
			reportAgent[0] = -1;
		}

		// which field to search on
		loadAttribute(RequestParams.REPORTS_SEARCH_FIELD);
		loadAttribute(RequestParams.REPORTS_SEARCH_FIELD_FROM);
		
		// search criteria
		loadAttribute(RequestParams.REPORTS_SEARCH_TSR);

		// display order
		loadAttribute(RequestParams.REPORTS_ORDER_BY);
		loadAttribute(RequestParams.REPORTS_ORDER_TYPE);

		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
		loadAttribute(RequestParams.REPORTS_FILTER_STATUS_CHANGED);
		int payrateType = 0;
		if (UserUtils.isTSAdmin(currentUser.getUserAttributes()))
			payrateType = 1;

		// convert search term to upper case
		TSRsearchString = TSRsearchString.toUpperCase();
		//by default All statuses is used for search
		//if (! filterStatusChanged){
		//	setReportStatus("-1");
		//}
		HashMap<String, Object> extraSearchFields = new HashMap<String, Object>();
		if(reportsSearchField.equalsIgnoreCase("Legal Description")) {
			extraSearchFields = LegalUtils.getLegalParams(TSRsearchString);
			if(extraSearchFields.get("SNname")!=null) {
				extraSearchFields.put("SNname", ((String)extraSearchFields.get("SNname")).replaceAll("[\\s-]", ""));
			}
			
		} else {
			if(reportsSearchField.equalsIgnoreCase("Property Address")) {
				extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-]", ""));
			} else {
				if("Property Owners".equalsIgnoreCase(reportsSearchField)) {
					extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-]", ""));
				} else if("TSR File ID".equalsIgnoreCase(reportsSearchField)){
					extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-_]", ""));
				} else {
					extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-]", ""));
				}
				
			}
		}
		// read data from DB		
		loadAttribute(RequestParams.SEARCH_ID);
		if( reportsSearchFieldFrom.equals( "starter") && StringUtils.isEmpty(TSRsearchString)) {
			
			ReportData = getStarterData(
					InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext(), 
					ua, 
					payrateType,
					reportCounty, 
					reportAbstractor, 
					reportAgent, 
					reportState, 
					reportCompanyAgent, 
					orderBy, 
					orderType, 
					reportStatus, 
					invoice, 
					reportDateType);
			
			for (int i = 0; i < ReportData.length; i++) {
				String fileLink = ReportData[i].getFileLink();
				fileLink = fileLink.replaceAll("menusForStarters\\[(\\d+)\\]","menusForStarters[" + i + "]");
				ReportData[i].setFileLink(fileLink);
			}
			
		} else {
			if(reportsSearchAll.equals("on")){
				ReportData = DBReports.getSearchReportAllInOne(
					reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
					extraSearchFields.containsKey("SNname")?(String)extraSearchFields.get("SNname"):"",
					1, 1, 1950, 31, 12, 2050, reportsSearchField,ua, payrateType,
					extraSearchFields.containsKey("LOT")?(String)extraSearchFields.get("LOT"):"",
					extraSearchFields.containsKey("BLOCK")?(String)extraSearchFields.get("BLOCK"):"",
					extraSearchFields.containsKey("PHASE")?(String)extraSearchFields.get("PHASE"):"",
					extraSearchFields.containsKey("SECTION")?(String)extraSearchFields.get("SECTION"):"",
					false, reportDateType
				);
	
			}else{
				// type of current search window 
				loadAttribute(RequestParams.TSR_SEARCH_TYPE);
	
				if(searchType.equals(RequestParamsValues.TSR_SEARCH_INTERVAL) ||
				   searchType.equals(RequestParamsValues.TSR_SEARCH_DAY)){
				 	loadAttribute(RequestParams.REPORTS_FROM_DAY);
					loadAttribute(RequestParams.REPORTS_FROM_MONTH);
					loadAttribute(RequestParams.REPORTS_FROM_YEAR);
					
					loadAttribute(RequestParams.REPORTS_TO_DAY);
					loadAttribute(RequestParams.REPORTS_TO_MONTH);
					loadAttribute(RequestParams.REPORTS_TO_YEAR);
					
					ReportData = DBReports.getSearchReportAllInOne(
						reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
						extraSearchFields.containsKey("SNname")?(String)extraSearchFields.get("SNname"):"",
						fromDay, fromMonth, fromYear, toDay, toMonth, toYear, reportsSearchField,ua, payrateType,
						extraSearchFields.containsKey("LOT")?(String)extraSearchFields.get("LOT"):"",
						extraSearchFields.containsKey("BLOCK")?(String)extraSearchFields.get("BLOCK"):"",
						extraSearchFields.containsKey("PHASE")?(String)extraSearchFields.get("PHASE"):"",
						extraSearchFields.containsKey("SECTION")?(String)extraSearchFields.get("SECTION"):"",
						false, reportDateType
					);
								
			    }else if(searchType.equals(RequestParamsValues.TSR_SEARCH_MONTH)){
			    	loadAttribute(RequestParams.REPORTS_MONTH);
			    	loadAttribute(RequestParams.REPORTS_YEAR);
			    	
			    	Calendar now = Calendar.getInstance();
			    	now.set(Calendar.MONTH, monthReport-1);
			    	now.set(Calendar.YEAR, yearReport);
					
					ReportData = DBReports.getSearchReportAllInOne(
						reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
						extraSearchFields.containsKey("SNname")?(String)extraSearchFields.get("SNname"):"",
						1, monthReport, yearReport, now.getActualMaximum(Calendar.DAY_OF_MONTH), monthReport, yearReport, reportsSearchField,ua, payrateType,
						extraSearchFields.containsKey("LOT")?(String)extraSearchFields.get("LOT"):"",
						extraSearchFields.containsKey("BLOCK")?(String)extraSearchFields.get("BLOCK"):"",
						extraSearchFields.containsKey("PHASE")?(String)extraSearchFields.get("PHASE"):"",
						extraSearchFields.containsKey("SECTION")?(String)extraSearchFields.get("SECTION"):"",
						false, reportDateType
					);
			    			    	
			    }else if(searchType.equals(RequestParamsValues.TSR_SEARCH_YEAR)){
			    	
			    	loadAttribute(RequestParams.REPORTS_YEAR);
			    	ReportData = DBReports.getSearchReportAllInOne(
							reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
							extraSearchFields.containsKey("SNname")?(String)extraSearchFields.get("SNname"):"",
							1, 1, yearReport, 31, 12, yearReport, reportsSearchField,ua, payrateType,
							extraSearchFields.containsKey("LOT")?(String)extraSearchFields.get("LOT"):"",
							extraSearchFields.containsKey("BLOCK")?(String)extraSearchFields.get("BLOCK"):"",
							extraSearchFields.containsKey("PHASE")?(String)extraSearchFields.get("PHASE"):"",
							extraSearchFields.containsKey("SECTION")?(String)extraSearchFields.get("SECTION"):"",
							false, reportDateType
						);
			    }
			}
		}
		
		if( Util.isValueInArray( 14, reportStatus ) )
		{
		    reportStatus = Util.NandTnotO( reportStatus );
		}
		if (logger.isDebugEnabled()){
			logger.debug("Date: " + yearReport + "-" + monthReport  + "-" + dayReport);		
			logger.debug("Agent/Abstractor: " + Util.getStringFromArray(reportAgent) + "-" + Util.getStringFromArray(reportAbstractor));		
			logger.debug("County/State: " + Util.getStringFromArray(reportCounty) + "-" + Util.getStringFromArray(reportState));		
			logger.debug("Status filter: " + Util.getStringFromArray(reportStatus));
		}
		
		if(ReportData != null){
			ReportData = CommAdminNotifier.filterResults( ReportData, reportStatus, new BigDecimal(String.valueOf(commId)) );
		}

		if (logger.isDebugEnabled())
			logger.debug("End.");
				
		return ReportData;
	}

	public static DayReportLineData[] getStarterData(
			Search search, 
			UserAttributes ua, 
			int payrateType, 
			int[] reportCounty, 
			int[] reportAbstractor, 
			int[] reportAgent, 
			int[] reportState, 
			String[] reportCompanyAgent, 
			String orderBy, 
			String orderType, 
			int[] reportStatus, 
			int invoice, 
			int reportDateType ) {
		DayReportLineData[] ReportData;
		
		HashMap<Long, DayReportLineData> allSearches = new HashMap<Long, DayReportLineData>();
		int commId = search.getCommId();
		boolean searchWithNames = true;
		
		String isCondoStr = search.getSa().getAtribute(SearchAttributes.IS_CONDO);
		boolean isCondo = StringUtils.isEmpty(isCondoStr)?false:Boolean.valueOf(isCondoStr);
		
		String unitStr = search.getSa().getAtribute( SearchAttributes.P_STREETUNIT );
		isCondo = isCondo || (StringUtils.isEmpty(unitStr)?false:true);
		
		String addressSearch = search.getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME_NO_SPACE);
		if(!isCondo){
			addressSearch = search.getSa().getAtribute(SearchAttributes.P_STREETNAME).replaceAll("\\s+", "");
		}
		
		if(!StringUtils.isEmpty(addressSearch)){
			DayReportLineData[] someSearches = DBReports.getSearchReportAllInOne(
					reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
					addressSearch.toUpperCase(),
					1, 1, 1950, 31, 12, 2050, "Property Address",ua, payrateType,
					"",
					"",
					"",
					"", true, reportDateType
				); 
			for (int i = 0; i < someSearches.length; i++) {
				if(!allSearches.containsKey(someSearches[i].getId())&& someSearches[i].getId() != search.getID())
					allSearches.put(someSearches[i].getId(), someSearches[i]);
			}
			if(someSearches.length > 0) {
				searchWithNames = false;
			}
		}
		
		search.loadLegalFromSearchAttributes();
		Legal searchLegal = search.getLegal();
		if(!StringUtils.isEmpty(searchLegal.getSubdivision().getName())){
			DayReportLineData[] someSearches = DBReports.getSearchReportAllInOne(
					reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
					searchLegal.getSubdivision().getName().replaceAll("[\\s-]", ""),
					1, 1, 1950, 31, 12, 2050, "Legal Description",ua, payrateType,
					/*lotExpanded*/"",
					/*searchLegal.getSubdivision().getBlock()*/"",
					/*searchLegal.getSubdivision().getPhase()*/"",
					/*searchLegal.getTownShip().getSection()*/"", 
					true, reportDateType
				); 
			for (int i = 0; i < someSearches.length; i++) {
				if(!allSearches.containsKey(someSearches[i].getId())&& someSearches[i].getId() != search.getID())
					allSearches.put(someSearches[i].getId(), someSearches[i]);
			}
			if(someSearches.length > 0) {
				searchWithNames = false;
			}
		}
		
		if(searchWithNames) {
			Party owners = (Party)search.getSa().getOwners();
			for (Iterator<NameI> iterator = owners.getNames().iterator(); iterator.hasNext();) {
				Name singleOwner = (Name) iterator.next();
				DayReportLineData[] someSearches = DBReports.getSearchReportAllInOne(
					reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
					(singleOwner.getFirstName() + singleOwner.getLastName()).replaceAll("[\\s-]", ""),
					1, 1, 1950, 31, 12, 2050, "Property Owners",ua, payrateType,
					"",
					"",
					"",
					"", true, reportDateType
				); 
				for (int i = 0; i < someSearches.length; i++) {
					if(!allSearches.containsKey(someSearches[i].getId()) && someSearches[i].getId() != search.getID())
						allSearches.put(someSearches[i].getId(), someSearches[i]);
				}				
			}
		}
		
		Vector<DayReportLineData> allSearchesVector = new Vector<DayReportLineData>();
		
		for (DayReportLineData dayReportLineData : allSearches.values()) {
			allSearchesVector.add(dayReportLineData);
		}
		//TODO: should sort according to the myats settings or page settings
		Collections.sort(allSearchesVector, Collections.reverseOrder(new OrderDateDayReportComparator()));
		ReportData = new DayReportLineData[allSearchesVector.size()];
		ReportData = allSearchesVector.toArray(ReportData);
		return ReportData;
	}

	protected boolean filterStatusChanged;
	public void setFilterStatusChanged(String s){
		filterStatusChanged = s.equals("1");
	}
	
	public boolean getFilterStatusChanged(){
		return filterStatusChanged;
	}
	
	protected long searchId;

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}
	
	protected String reportsSearchFieldFrom = "";
	public void setReportsSearchFieldFrom(String s) {
		reportsSearchFieldFrom = s;
	}
	public String getReportsSearchFieldFrom() {
		return reportsSearchFieldFrom;
	}
	
	

}

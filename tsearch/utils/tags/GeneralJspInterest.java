package ro.cst.tsearch.utils.tags;

import javax.servlet.http.HttpServletRequest;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.MaintenanceMessage;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;

public final class GeneralJspInterest {

	public static  String createPriorFilesLink(long searchId){
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = global.getSa();
		
		return "/title-search/jsp/reports/reports_search.jsp?searchId=" + searchId + 
				"&"+RequestParams.REPORTS_SEARCH_FIELD_FROM+"=starter&" + 
				RequestParams.REPORTS_COUNTY+"="+sa.getAtribute(SearchAttributes.P_COUNTY)+"&" + 
				RequestParams.REPORTS_STATE+"="+sa.getAtribute(SearchAttributes.P_STATE)+"&" +
				RequestParams.REPORTS_DASHBOARD+"=2";
	}
	
	public static String getMaintenanceMessage(HttpServletRequest request){
		return MaintenanceMessage.getFormattedMessage(request);
	}
	
	
	public static  String createAdditionalInfoLink(long searchId){
		return "/title-search/jsp/additionalInfo.jsp?searchId=" + searchId;
	}
	
}

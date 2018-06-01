package ro.cst.tsearch.tags;

import org.apache.log4j.Logger;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.utils.ParameterNotFoundException;
import ro.cst.tsearch.utils.RequestParams;

public class CommunitiesSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(CommunitiesSelect.class);
	
	private int[] reportCommunity = {-1};
	

	public String getReportCommunity() {
		return Util.getStringFromArray(reportCommunity);
	}


	public void setReportCommunity(String s) {
		this.reportCommunity = Util.extractArrayFromString(s);
	}


	@Override
	protected String createOptions() throws Exception {
		
		CommunityAttributes[] allCommunities = CommunityUtils.getAllCommunities(ses);
		loadAttribute(RequestParams.REPORTS_COMMUNITY);
		try {
			pp.getMultipleStringParameter(RequestParams.REPORTS_COMMUNITY); 
		} catch (ParameterNotFoundException pnfe) {											//first time there in no such parameter
			String currentCommunity = (String)ses.getAttribute("commId");					//so initialize with current community
			setReportCommunity(currentCommunity);
		}
		logger.info("communityId=" + Util.getStringFromArray(reportCommunity));
		
		StringBuffer sb = new StringBuffer(3000);		
		sb.append(allOption(reportCommunity));
		for (int i = 0; i < allCommunities.length; i++) {
			sb.append(
				"<option "
					+ (Util.isValueInArray(allCommunities[i].getID().intValue(), reportCommunity) ? "selected" : "")
					+ " value='"
					+ allCommunities[i].getID().intValue()
					+ "'>"
					+ allCommunities[i].getNAME()
					+ "</option>");
		}
		return sb.toString();
	}

	protected  String allOption(int[] id)	throws Exception {

		if(all) {
			return "<option "+(Util.isValueInArray(-1, id)?"selected":"")+" value='-1'>All Communities</option>" ;
		} else {
			return "";
		}
	}
}

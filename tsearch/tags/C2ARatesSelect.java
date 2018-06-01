package ro.cst.tsearch.tags;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

public class C2ARatesSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String[] c2arates = {"-1"};
	
	public void setC2arates(String s) {
		c2arates = Util.extractStringArrayFromString(s);
	}	
	public String getC2arates() {
		return Util.getStringFromStringArray(c2arates);
	}
	
	private int[] reportState = {-2};	
	public void setReportState(String s) {
		reportState = Util.extractArrayFromString(s);
	}	
	public String getReportState() {
		return Util.getStringFromArray(reportState);
	}
	
	@Override
	protected String createOptions() throws Exception {
		loadAttribute(RequestParams.REPORTS_STATE);
		loadAttribute(RequestParams.C2ARATES);
		
		String[] userIds = req.getParameterValues(RequestParams.REPORTS_USER);
		if(userIds != null) {
			for(String userId : userIds) {
				if("-1".equals(userId)) {
					UserManagerI userManager = UserManager.getInstance();
					try {
						userManager.getAccess();
						int commId = Integer.parseInt((String)ses.getAttribute("commId"));
						List<String> allUserIds = new ArrayList<String>();
						for(UserI user : userManager.getUsersByCommunity(commId)) {
							allUserIds.add(user.getUserId()+"");
						}
						userIds = allUserIds.toArray(new String[0]);
						break;
					}catch(Exception e) {
						e.printStackTrace();
					}finally{
						userManager.releaseAccess();
					}
				}
			}
		}
		StringBuffer sb = new StringBuffer(allOption(c2arates));
		Vector<BigDecimal> rates = DBManager.getDistinctUserRatings( "C2ARATEINDEX", reportState, userIds);
		
		for (BigDecimal rate : rates) {
			sb.append(
					"<option value='" +
					rate.toString() + "'" + 
					(Util.isValueInArray(rate.toString(), c2arates) ? " selected >" : ">") +
					StringUtils.HTMLEntityEncode(rate.toString()) +
					"</option>"
			);
		}
		
		
		return sb.toString();
	}
	
	protected  String allOption(String[] id)	throws Exception {
		if(all) {
			return "<option "+(Util.isValueInArray("-1", id)?"selected":"") +
				" value='-1'>All</option>\n" ;
		} else {
			return "";
		}
	}

}

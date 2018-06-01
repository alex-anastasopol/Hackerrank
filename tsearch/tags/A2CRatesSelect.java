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

public class A2CRatesSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String[] a2crates = {"-1"};
	
	public void setA2crates(String s) {
		a2crates = Util.extractStringArrayFromString(s);
	}	
	public String getA2crates() {
		return Util.getStringFromStringArray(a2crates);
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
		loadAttribute(RequestParams.A2CRATES);
		
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
		
		
		StringBuffer sb = new StringBuffer(allOption(a2crates));
		Vector<BigDecimal> rates = DBManager.getDistinctUserRatings( "ATS2CRATEINDEX", reportState,userIds );
		
		for (BigDecimal rate : rates) {
			sb.append(
					"<option value='" +
					rate.toString() + "'" + 
					(Util.isValueInArray(rate.toString(), a2crates) ? " selected >" : ">") +
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

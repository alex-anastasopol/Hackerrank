package ro.cst.tsearch.tags;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

public class UsersSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(UsersSelect.class);

	private int[] reportUser = {-2};
	public void setReportUser(String s) {
		reportUser = Util.extractArrayFromString(s);
	}	
	public String getReportUser() {
		return Util.getStringFromArray(reportUser);
	}
	private boolean noAgents = false;
	public void setNoAgents(String s) {
		if("true".equalsIgnoreCase(s))
			noAgents = true;
	}
	public String getNoAgents(){
		if(noAgents)
			return "true";
		return "false";
	}
	
	@Override
	protected String createOptions() throws Exception {
		loadAttribute(RequestParams.REPORTS_USER);
		int commId = Integer.parseInt((String)ses.getAttribute("commId"));
		UserManagerI userManager = UserManager.getInstance();
		StringBuffer sb = new StringBuffer();
		sb.append(noOption(reportUser));
		sb.append(allOption(reportUser));
		try {
			userManager.getAccess();
			
			List<UserI> allUsers = null;
			if(noAgents) {
				allUsers = userManager.getUsersByCommunity(commId, 
						GroupAttributes.ABS_ID,
						GroupAttributes.CA_ID,
						GroupAttributes.CCA_ID,
						GroupAttributes.TA_ID);
			} else {
				allUsers = userManager.getUsersByCommunity(commId);
			}
			
			Collections.sort(allUsers );
			
			for (UserI users : allUsers) {
				
				String toView = StringUtils.HTMLEntityEncode(users.getFirstName() + " " 
				+ users.getLastName()
				+ " - " 
				+ users.getUserName());
				sb.append(
						"<option "
							+ (Util.isValueInArray(new Long(users.getUserId()).intValue(), reportUser) ? "selected" : "")
							 + " title='"
							+ toView
							+ "' "
							+ " value='"
							+ users.getUserId()
							+ "'>"
							+ toView
							+ "</option>");
			}
		} catch (Exception e) {
			logger.error("Error while writting user select ", e);
		} finally {
			if(userManager != null) {
				userManager.releaseAccess();
			}
		}
		return StringUtils.sortSelectListByText(sb.toString(), false, true, true, true, true);
	}
	
	protected  String allOption(int[] id)	throws Exception {
		if(all) {
			return "<option "+(Util.isValueInArray(-1, id)?"selected":"")+" value='-1'>All " + (noAgents?"Abstractors":"Users") + "</option>" ;
		} else {
			return "";
		}
	}
	
	protected  String noOption(int[] id)	throws Exception {
		if(none) {
			return "<option "+(Util.isValueInArray(-2, id)?"selected":"")+" value='-2'>No " + (noAgents?"Abstractors":"Users") + "</option>" ;
		} else {
			return "";
		}
	}

}

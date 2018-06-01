package ro.cst.tsearch.tags;

import org.apache.log4j.Logger;

import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

public class AgentsSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(AgentsSelect.class);

	private int[] reportAgent = {-1};	
	public void setReportAgent(String s) {
		reportAgent = Util.extractArrayFromString(s);
	}	
	public String getReportAgent() {
		return Util.getStringFromArray(reportAgent);
	}
	private String[] reportCompanyAgent = {"-1"};	
	public void setReportCompanyAgent(String s) {
		reportCompanyAgent = Util.extractStringArrayFromString(s);
	}	
	public String getReportCompanyAgent() {
		return Util.getStringFromStringArray(reportCompanyAgent);
	}
	private boolean preselectRestrictions = false;
	public void setPreselectRestrictions(String s) {
		preselectRestrictions = (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}
	public boolean getPreselectRestrictions(){
		return preselectRestrictions;
	}
	private boolean preselectAssign = false;
	public void setPreselectAssign(String s) {
		preselectAssign = (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}
	public boolean getPreselectAssign(){
		return preselectAssign;
	}
	
	private boolean readOnly = false;
	public void setReadOnly(String s) {
		if("true".equalsIgnoreCase(s))
			readOnly = true;
	}
	public String getReadOnly(){
		if(readOnly)
			return "true";
		return "false";
	}

	/**
	 * @see ro.cst.tsearch.generic.tag.SelectTag#createOptions()
	 */
	protected String createOptions() throws Exception {
		loadAttribute(RequestParams.REPORTS_AGENT);
		long selectedUser = -1;
		long activeUserId = -1;
		int commId = -1;
		try {
			String selectedUserString = getRequest().getParameter(UserAttributes.USER_ID);
			if(StringUtils.isNotEmpty(selectedUserString) && !"null".equals(selectedUserString)) {
				selectedUser = Long.parseLong(selectedUserString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean preselectAll = false;
		StringBuffer sb = new StringBuffer();
		UserI activeUser = null;
		UserManagerI userManager = UserManager.getInstance();
		try {
			userManager.getAccess();
			if(selectedUser > -1) {
				activeUser = userManager.getUser(selectedUser);
			}
			String commIdStr=(String)ses.getAttribute("commId");
			commId = Integer.parseInt(commIdStr);
			
			

			if (activeUser != null && activeUser.ifOfType(GroupAttributes.AG_ID)){
				activeUserId = activeUser.getUserId();
				sb.append(
						"<option selected"
							+ " value='"
							+ activeUserId
							+ "'>"
							+ StringUtils.HTMLEntityEncode(activeUser.getFirstName())
							+ " " 
							+ StringUtils.HTMLEntityEncode(activeUser.getLastName())
							+ "</option>");
			} else {
				boolean hasRestriction = activeUser!= null && activeUser.getRestriction().hasAgentAllowed();
				boolean hasAssignedAgent = activeUser!= null && activeUser.getRestriction().hasAgentAssigned();
				preselectAll = true;
				for (UserI agents : userManager.getUsersByCommunity(commId, GroupAttributes.AG_ID)) {
					boolean preselect = false;
					boolean showRow = true;
					if(preselectAssign) {
						preselect = hasAssignedAgent && activeUser.getRestriction().isAgentAssigned((int)agents.getUserId());
						if(hasRestriction && !activeUser.getRestriction().isAgentAllowed((int)agents.getUserId())){
							showRow = false;
						}
								
					} else {
						preselect = activeUser!= null && hasRestriction && activeUser.getRestriction().isAgentAllowed((int)agents.getUserId());
					}
					if(readOnly && !preselect)
						showRow = false;
					if(showRow) {
						if(preselect) {
							preselectAll = false;
						}
						String toView = StringUtils.HTMLEntityEncode(agents.getFirstName()
								+ " " + agents.getLastName() + " - " + agents.getUserName());
						sb.append(
							"<option "
								+ (preselect? "selected" : "")
								+ " title='" + toView + "' " + " value='"
								+ agents.getUserId()
								+ "'>"
								+ toView
								+ "</option>");
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while writting agent select for user: " + activeUserId, e);
		} finally {
			if(userManager != null) {
				userManager.releaseAccess();
			}
		}
		
		sb.insert(0, allOption(reportAgent, preselectAll));
		
		return StringUtils.sortSelectListByText(sb.toString(), false, true, true, true, true);
	}

	protected  String allOption(int[] id, boolean preselect)	throws Exception {

		if(all && !readOnly) {
			return "<option value='-1' " +
					(preselect?"selected":"") +
					" >All Agents</option>" ;
		} else {
			return "";
		}
	}

}

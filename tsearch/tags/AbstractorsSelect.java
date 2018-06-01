package ro.cst.tsearch.tags;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.reports.throughputs.ThroughputBean;
import ro.cst.tsearch.reports.throughputs.ThroughputOpCode;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

public class AbstractorsSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(AbstractorsSelect.class);

	private int[] reportAbstractor = {-1};	
	public void setReportAbstractor(String s) {
		reportAbstractor = Util.extractArrayFromString(s);
	}	
	public String getReportAbstractor() {
		return Util.getStringFromArray(reportAbstractor);
	}
	
	private boolean useThBean = false;
	public void setUseThBean(String s){
		useThBean = (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}

	public boolean getUseThBean(){
		return useThBean;
	}
	
	private boolean preselectRestrictions = false;
	public void setPreselectRestrictions(String s) {
		preselectRestrictions = (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}
	public boolean getPreselectRestrictions(){
		return preselectRestrictions;
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

		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		long selectedUser = -1;
		long activeUserId = -1;
		try {
			String selectedUserString = getRequest().getParameter(UserAttributes.USER_ID);
			if(StringUtils.isNotEmpty(selectedUserString) && !"null".equals(selectedUserString)) {
				selectedUser = Long.parseLong(selectedUserString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		loadAttribute(RequestParams.REPORTS_ABSTRACTOR);
		
		logger.info("abstractorId=" + Util.getStringFromArray(reportAbstractor));

		StringBuffer sb = new StringBuffer(3000);		
		
		UserI activeUser = null;
		UserManagerI userManager = UserManager.getInstance();
		try {
			userManager.getAccess();
			if(selectedUser > -1) {
				activeUser = userManager.getUser(selectedUser);
			} else {
				activeUser = userManager.getUser(currentUser.getUserAttributes().getID().longValue());
			}
			activeUserId = activeUser.getUserId();

			if (activeUser.ifOfType(GroupAttributes.AG_ID)){				
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
				UserAttributes allAbstractors[] = null;
				int groupId = -1;
				ThroughputBean thBean = null;
				if (useThBean){
					if (getRequest().getParameter("type")!=null && getRequest().getParameter("type").equals(ThroughputOpCode.INCOME_BEAN)) {
						thBean = (ThroughputBean)ses.getAttribute(ThroughputOpCode.INCOME_BEAN);
					} else {
						thBean = (ThroughputBean)ses.getAttribute(ThroughputOpCode.THROUGHPUT_BEAN);
					}
				}
				if (thBean != null){
					String commId = thBean.getSelectCommunities();
					groupId = Integer.parseInt(thBean.getSelectGroups());
					if ("-1".equals(commId))
						commId = Util.getStringsList(thBean.getMultiCommunities(),",");
					//I clicked others
					if (groupId < -1) groupId = -1;
	 				allAbstractors = DBManager.getAllAbstractorsForSelect(commId, groupId);
				} else {
					String commId = (String)ses.getAttribute("commId");
					allAbstractors = DBManager.getAllAbstractorsForSelect(commId);
				}
				
				//remove abstractors which are not in the selected communities
				List<Integer> newReportAbstractor = new ArrayList<Integer>();
				for (int i=0;i<reportAbstractor.length;i++) {
					int value = reportAbstractor[i];
					boolean found = false;
					for (UserAttributes ag: allAbstractors) {
						if (value==ag.getID().intValue()) {
							found = true;
							break;
						}
					}
					if (found)
						newReportAbstractor.add(value);
				}
				int size = newReportAbstractor.size();
				if (size==0) {
					reportAbstractor = new int[1];
					reportAbstractor[0] = -1;
				} else {
					reportAbstractor = new int[size];
					for (int i=0;i<size;i++) {
						reportAbstractor[i] = newReportAbstractor.get(i).intValue();
					}
				}
				
				sb.append(allOption(reportAbstractor));
				
				
					
				 
				boolean hasRestriction = activeUser.getRestriction().hasAbstractorAssigned();
				
				for (int i = 0; i < allAbstractors.length; i++) {
					boolean preselect = Util.isValueInArray(allAbstractors[i].getID().intValue(), reportAbstractor);
					String toView = StringUtils.HTMLEntityEncode(allAbstractors[i].getFIRSTNAME() 
							+ " " 
							+ allAbstractors[i].getLASTNAME()
							+ " - " 
							+ allAbstractors[i].getLOGIN());
					
					if(preselectRestrictions) {
						if(	hasRestriction &&
								activeUser.getRestriction().isAbstractorAssigned(allAbstractors[i].getID().intValue())) {
							preselect = true;
						} else {
							preselect = false;
						}
						if(readOnly ) {
							if(preselect) {
								sb.append(
										"<option selected " + " title='" + toView + "' " + " value='"
											+ allAbstractors[i].getID()
											+ "'>"
											+ toView
											+ "</option>");
							}
						} else {
							sb.append(
									"<option "
										+ (preselect? "selected" : "")
										+ " " + " title='" + toView + "' " + " value='"
										+ allAbstractors[i].getID()
										+ "'>"
										+ toView
										+ "</option>");
						}
						
					} else {
						if(	!hasRestriction || 
								(hasRestriction && activeUser.getRestriction().isAbstractorAssigned(allAbstractors[i].getID().intValue()))
								) {
							
							if(readOnly ) {
								if(preselect) {
									sb.append(
										"<option selected " + " title='" + toView + "' " + " value='"
											+ allAbstractors[i].getID()
											+ "'>"
											+ toView
											+ "</option>");
								}
							} else {
								sb.append(
									"<option "
										+ (preselect? "selected" : "")
										+ " " + " title='" + toView + "' " + " value='"
										+ allAbstractors[i].getID()
										+ "'>"
										+ toView
										+ "</option>");
							}
						} 
					
					}
				}
				
				
			}
		} catch (Exception e) {
			logger.error("Error while writting abstractor select for user: " + activeUserId, e);
		} finally {
			if(userManager != null) {
				userManager.releaseAccess();
			}
		}
		return StringUtils.sortSelectListByText(sb.toString(), false, true, true, true, true);
	}

	protected  String allOption(int[] id)	throws Exception {

		if(all) {
			if(!readOnly) {
				return "<option "+(Util.isValueInArray(-1, id) && !preselectRestrictions?"selected":"")+" value='-1'>All Abstractors</option>" ;
			}
		} 
		return "";

	}

}

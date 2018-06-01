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
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

public class CompaniesAgentsSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(CompaniesAgentsSelect.class);

	private String[] reportCompanyAgent = {"-1"};	
	public void setReportCompanyAgent(String s) {
		reportCompanyAgent = Util.extractStringArrayFromString(s);
	}	
	public String getReportCompanyAgent() {
		return Util.getStringFromStringArray(reportCompanyAgent);
	}

	private boolean useThBean = false;
	public void setUseThBean(String s){
		useThBean = (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}

	public boolean getUseThBean(){
		return useThBean;
	}	
	
	/**
	 * @see ro.cst.tsearch.generic.tag.SelectTag#createOptions()
	 */
	protected String createOptions() throws Exception {

		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		
		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
		logger.info("companyName=" + Util.getStringFromStringArray(reportCompanyAgent));

		StringBuffer sb = new StringBuffer(3000);
		
		if (!UserUtils.isAgent(ua)){	
			String commIds = "-1";
			String companies[];
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
				commIds = thBean.getSelectCommunities();
				if ("-1".equals(commIds))
					commIds = Util.getStringsList(thBean.getMultiCommunities(), ",");
				groupId = Integer.parseInt(thBean.getSelectGroups());
				if (groupId < -1) groupId = -1;
				companies = DBManager.getAllCompaniesForSelect(commIds, groupId);
			} else {
				commIds =(String)ses.getAttribute("commId");
				companies = DBManager.getAllCompaniesForSelect(commIds);
			}
			
			//remove companies which are not in the selected communities
			List<String> newReportCompanyAgent = new ArrayList<String>();
			for (int i=0;i<reportCompanyAgent.length;i++) {
				String value = reportCompanyAgent[i];
				boolean found = false;
				for (int j=0;j<companies.length;j++) {
					if (companies[j].startsWith(value)) {
						found = true;
						break;
					}
				}
				if (found)
					newReportCompanyAgent.add(value);
			}
			int size = newReportCompanyAgent.size();
			if (size==0) {
				reportCompanyAgent = new String[1];
				reportCompanyAgent[0] = "-1";
			} else {
				reportCompanyAgent = new String[size];
				for (int i=0;i<size;i++) {
					reportCompanyAgent[i] = newReportCompanyAgent.get(i);
				}
			}
			
			sb.append(allOption(reportCompanyAgent));
			for (int i = 0; i < companies.length; i++) {
				String currentCompany = companies[i];
				String shortCurrentCompany = currentCompany.substring(0, currentCompany.length()>3 ? 3 : currentCompany.length());
				sb.append(
					"<option "
						+ (Util.isValueInArray(shortCurrentCompany,reportCompanyAgent) ? "selected" : "")
						+ " value='"
						+ shortCurrentCompany
						+ "'>"
						+ StringUtils.HTMLEntityEncode(companies[i])
						+ "" 
						+ "</option>");
			}
		}else
		sb.append(
			"<option selected"
				+ " value='"
				+ ua.getCOMPANY().substring(0, 3)
				+ "'>"
				+ StringUtils.HTMLEntityEncode(ua.getCOMPANY().substring(0, 3))
				+ "</option>");
		
		return StringUtils.sortSelectListByText(sb.toString(), false, true, true, true, true);
	}

	protected  String allOption(String[] compName)	throws Exception {

		if(all) {
			return "<option "+(Util.isValueInArray("-1", compName)?"selected":"")+" value='-1'>All Agencies</option>" ;
		} else {
			return "";
		}
	}

}

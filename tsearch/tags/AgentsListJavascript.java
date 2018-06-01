package ro.cst.tsearch.tags;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.GenericTag;
import ro.cst.tsearch.reports.throughputs.ThroughputBean;
import ro.cst.tsearch.reports.throughputs.ThroughputOpCode;
import ro.cst.tsearch.user.AgentAttributes;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;


public class AgentsListJavascript extends GenericTag {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(AgentsListJavascript.class);
   
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

	private boolean useThBean = false;
	public void setUseThBean(String s){
		useThBean = (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}

	public boolean getUseThBean(){
		return useThBean;
	}	

	private int selectSize = 5;
	public void setSelectSize(String s){
		selectSize = Integer.parseInt(s);
	}

	public int getSelectSize(){
		return selectSize;
	}	
	
	private boolean selectMultiple = true;
	public void setSelectMultiple(String s){
		selectMultiple= (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}

	public boolean getSelectMultiple(){
		return selectMultiple;
	}	
	private boolean preselectRestrictions = false;
	public void setPreselectRestrictions(String s) {
		preselectRestrictions = (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}
	public boolean getPreselectRestrictions(){
		return preselectRestrictions;
	}
	
    public int doStartTag() {
        try {
        	long startTime = System.currentTimeMillis();
            initialize();
                        
            User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
            long selectedUser = -1;
    		long activeUserId = -1;
    		try {
    			String selectedUserString = getRequest().getParameter(UserAttributes.USER_ID);
    			if(StringUtils.isNotEmpty(selectedUserString)) {
    				selectedUser = Long.parseLong(selectedUserString);
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		loadAttribute(RequestParams.REPORTS_AGENT);
    		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
    		

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
    			logger.debug("AgentsListJavascript loading active user took: " + (System.currentTimeMillis() - startTime));
    			
    		//if the user is an agent he has only one option in the agents select box (his name)
    			if (activeUser.ifOfType(GroupAttributes.AG_ID)){
	    			sb.append("<SCRIPT LANGUAGE=JavaScript TYPE='text/javascript'> \n");
	    			sb.append(
	    					"function loadAgentSelectOptions(selectOnlyFirstOption) { \n" 
	    					);
	    			sb.append("" +    					 
	    	   	     		" var x = \"\"; \n" +		
	        	     		" x = \"<select name=\\\""+ RequestParams.REPORTS_AGENT +"\\\" size=\\\""+ selectSize +"\\\" "+ 
	        	     		(selectMultiple?"multiple":"") +
	        	     		" style=\\\"visibility: hidden; width: 0px;\\\"  " +
	        	     		"onblur=\\\"javascript:reportAgentSelectOnBlur();\\\"  " +
	        	     		"onfocus=\\\"javascript:reportAgentSelectOnFocus();\\\"  " +
	        	     		"onchange=\\\"javascript:reportAgentSelectOnChange();\\\"  >\";" +
	    					" x = x + \"<option selected value='"+
	    					activeUserId + "' >"+
	    					activeUser.getFirstName() +" " + 
	    					activeUser.getLastName() + "</option> \" \n" +
	    					" x = x + \"</select>\"; \n" +
	    					"document.getElementById('agentsSelectSpan').innerHTML = x; \n"
	    					);		
	    	    	sb.append("}");
	    			sb.append("function filterAgentSelectOptions() { \n" +"}");
	    			sb.append("</SCRIPT>");
	    			
	    		    pageContext.getOut().print(sb.toString());
	                
	    		} else {
	    		
		    		sb.append("<SCRIPT LANGUAGE=JavaScript TYPE='text/javascript'> \n" +
							    " function companyWithAgents(aId,aList,isSel) {\n" + 
								" this.companyId = aId;\n " + 
								" this.agentList = aList; \n" + 
								" this.isSelected = isSel; \n" +
								" }\n" +
								"companiesWithAgents = new Array(" +
								"");
		
		
		    		String commIds = "-1";
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
							commIds = Util.getStringsList(thBean.getMultiCommunities(),",");
						groupId = Integer.parseInt(thBean.getSelectGroups());
						if (groupId < -1) 
							groupId = -1;
						
						
					} else {
						commIds = (String)ses.getAttribute("commId");
					}
					logger.debug("AgentsListJavascript time elapsed before getting agents with companies took: " + (System.currentTimeMillis() - startTime));
					List<AgentAttributes> allAgents = DBManager.getAllCompaniesAndAgentsForSelect(commIds, groupId);
					if(allAgents == null) {
						return(SKIP_BODY);
					}
					
					//remove agents which are not in the selected communities
					List<Integer> newReportAgent = new ArrayList<Integer>();
					for (int i=0;i<reportAgent.length;i++) {
						int value = reportAgent[i];
						boolean found = false;
						for (AgentAttributes ag: allAgents) {
							if (ag.getID().intValue()==value) {
								found = true;
								break;
							}
						}
						if (found)
							newReportAgent.add(value);
					}
					int size = newReportAgent.size();
					if (size==0) {
						reportAgent = new int[1];
						reportAgent[0] = -1;
					} else {
						reportAgent = new int[size];
						for (int i=0;i<size;i++) {
							reportAgent[i] = newReportAgent.get(i).intValue();
						}
					}
					
					//remove agencies which are not in the selected communities
					List<String> newReportAgency = new ArrayList<String>();
					for (int i=0;i<reportCompanyAgent.length;i++) {
						String value = reportCompanyAgent[i];
						boolean found = false;
						for (AgentAttributes ag: allAgents) {
							if (ag.getCOMPANY().startsWith(value)) {
								found = true;
								break;
							}
						}
						if (found)
							newReportAgency.add(value);
					}
					size = newReportAgency.size();
					if (size==0) {
						reportCompanyAgent = new String[1];
						reportCompanyAgent[0] = "-1";
					} else {
						reportCompanyAgent = new String[size];
						for (int i=0;i<size;i++) {
							reportCompanyAgent[i] = newReportAgency.get(i);
						}
					}
										
					boolean hasRestriction = activeUser.getRestriction().hasAgentAllowed();
					logger.debug("AgentsListJavascript time elapsed before for " + allAgents.size() + " companies took: " + (System.currentTimeMillis() - startTime));
					
					LinkedHashSet<String> companyList = new LinkedHashSet<String>();
					
						
					StringBuilder sb1 = new StringBuilder();
					
					int allAgentsSize = allAgents.size();
					for (int i = 0; i < allAgentsSize; i++) {
						AgentAttributes agentAttributes = allAgents.get(i);
						String currentCompany = agentAttributes.getCOMPANY();
						companyList.add(currentCompany);
						
						while ( true ) {
							
							sb1.append(
									"new companyWithAgents('" +	currentCompany + "',\""
							);
									
							boolean preselect = Util.isValueInArray(agentAttributes.getID().intValue(),reportAgent);
							if(preselectRestrictions) {
								if(	hasRestriction &&
										activeUser.getRestriction().isAgentAllowed(agentAttributes.getID().intValue())) {
									preselect = true;
								}
								sb1.append(
										"<option value=" + agentAttributes.getID() + (preselect ? " selected " : "") + ">"  +
				    					 StringUtils.HTMLEntityEncode(agentAttributes.getFIRST_NAME()) +
										" " +
										StringUtils.HTMLEntityEncode(agentAttributes.getLAST_NAME()) +
										"  (" +
										StringUtils.HTMLEntityEncode(agentAttributes.getCOMPANY()) +
										")" + 
										"</option>"
								);
							} else {
								if(	!hasRestriction || 
										(hasRestriction && activeUser.getRestriction().isAgentAllowed(agentAttributes.getID().intValue()))
										) {
									sb1.append(
											"<option value=" + agentAttributes.getID() + (preselect ? " selected " : "") + ">"  +
					    					 StringUtils.HTMLEntityEncode(agentAttributes.getFIRST_NAME()) +
											" " +
											StringUtils.HTMLEntityEncode(agentAttributes.getLAST_NAME()) +
											"  (" +
											StringUtils.HTMLEntityEncode(agentAttributes.getCOMPANY()) +
											")" + 
											"</option>"
									);
								}
							}
							
							sb1.append('\"');
							sb1.append(
									"," +
			    					( (Util.isValueInArrayIgnoreCase(
			    						currentCompany.substring(0, currentCompany.length()>3 ? 3 : currentCompany.length()), 
			    						reportCompanyAgent) || Util.isValueInArray("-1", reportCompanyAgent) ) ? "true" : "false")
			    			);
			    			sb1.append("),");
							
							if( i+1 < allAgentsSize && currentCompany.equalsIgnoreCase(allAgents.get(i+1).getCOMPANY())){
								i++;
								agentAttributes = allAgents.get(i);
					
							} else {
								break;
							}
							
						}
						
		    		}
					
					sb.append(StringUtils.sortSelectListByText(sb1.toString(), true, true, false, true, true));
					
					if (companyList.size() != 0) {
						sb.setCharAt(sb.length()-1, ')');
					} else {
						sb.append(")");
					}
		     		sb.append(";\n");
		     		sb.append(
		         			"function loadAgentSelectOptions(selectOnlyFirstOption) { \n" +
		    	     		"	var x = \"\"; \n" +		
		    	     		"   x = \"<select name=\\\""+ RequestParams.REPORTS_AGENT +"\\\" size=\\\""+ selectSize +"\\\" "+ (selectMultiple?"multiple":"") +
		    	     		" style=\\\"width: " + currentUser.getUserAttributes().getMyAtsAttributes().getAgentsSelectWidth() + "px\\\"  " +
		    	     		"onblur=\\\"javascript:reportAgentSelectOnBlur();\\\"  " +
		    	     		"onfocus=\\\"javascript:reportAgentSelectOnFocus();\\\"  " +
		    	     		"onchange=\\\"javascript:reportAgentSelectOnChange();\\\"  ><option" + 
		    	     		(Util.isValueInArray(-1, reportAgent) && !preselectRestrictions  ? " selected ":""  ) + 
		    	     		" value='-1' >All Agents</option>\";" +
		    	     		"	for(i = 0; i < companiesWithAgents.length; i++){ \n" +
		    	     		"		if(companiesWithAgents[i].isSelected == true) { \n" +
		    	     		"			x = x + companiesWithAgents[i].agentList; \n" +
		    	     		"			} \n" +
		    	     		"		} \n" +
		    	     		"	x = x + \"</select>\"; \n" +
		    	     		"	document.getElementById('agentsSelectSpan').innerHTML = x; \n" +
		         			"   var reportsAgent = document.getElementsByName('"+ RequestParams.REPORTS_AGENT + "')[0]; \n" +
		    	     		"   if( selectOnlyFirstOption ==true ) reportsAgent.selectedIndex = 0; \n" +
		    	     		" } \n"
		    	     		);
		     		sb.append(
		     			"  function filterAgentSelectOptions() { \n" +
		     			"  var reportsCompanyAgent = document.getElementsByName('"+ RequestParams.REPORTS_COMPANY_AGENT + "')[0]; \n" +
		     			"  var allAgentsSelected = (reportsCompanyAgent.options[reportsCompanyAgent.selectedIndex].value == -1); \n" +
		     			"		if(reportsCompanyAgent.selectedIndex == -1 ||  allAgentsSelected == true) { \n" +
		     			"			for(i = 0; i < companiesWithAgents.length; i++){ \n" +
		     			"				companiesWithAgents[i].isSelected = true; \n" +
		     			"  			} \n" +
		     			"			return true; \n" +
		     			"			} \n" +
		     			"if(reportsCompanyAgent.options[0].value == -1)  { \n" +
		     			"		for(i=0;i<companiesWithAgents.length;i++) { \n" +
		     			"			var index = -1; \n" +	
		     			"			for (j=1;j<reportsCompanyAgent.options.length;j++) { \n" +
		     			"				if(companiesWithAgents[i].companyId.toLowerCase()==reportsCompanyAgent.options[j].text.toLowerCase()) { \n" +
		     			"				index = j; \n" + 
		     			"				j = reportsCompanyAgent.options.length; \n " +
		     			"			}	\n" + 
		     			" 			if (index!=-1 && reportsCompanyAgent.options[index].selected == true) { \n" + 
		     			"				companiesWithAgents[i].isSelected = true; \n" +
		     			"			} else { \n " + 
		     			" 				companiesWithAgents[i].isSelected = false; \n" +
		     			"			} \n " +
		     			"		} \n " +
		     			"	} \n" +
		     			"} \n" +
		     			"} \n"
		     		);
		    		 sb.append("</SCRIPT>");
		    		 
		            pageContext.getOut().print(sb.toString());
	    		}
    			
    		} catch (Exception e) {
				logger.error("Error while writting agent select for user: " + activeUserId, e);
			} finally {
				if(userManager != null) {
					userManager.releaseAccess();
				}
				logger.debug("Full AgentsListJavascript took: " + (System.currentTimeMillis() - startTime));
			}
            return(SKIP_BODY);
        }
        catch (Exception e) {
            e.printStackTrace();
			logger.error(this.getClass().toString()+"#doStartTag Exception in Tag " + this.getClass().toString()); 
        }
               
        return(SKIP_BODY);
    }
}

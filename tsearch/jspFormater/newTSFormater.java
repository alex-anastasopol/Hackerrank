package ro.cst.tsearch.jspFormater;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManagerI;

public class newTSFormater
{
	private static final Category logger = Logger.getLogger(newTSFormater.class);
	
	public static final int ALL_DISABLED                                                        = 0;
	public static final int PARENT_SITE_ENABLED_AUTOMATIC_DISABLED   = 1;
	public static final int PARENT_SITE_ENABLED_AUTOMATIC_ENABLED    = 2;
//	private static final int PARENT_SITE_DISABLED_AUTOMATIC_ENABLED   = 1;
	
	// for users pages
	public static String getDirectionCombo(String userAtribute, UserAttributes  ua)
	{		
		String value = ua.getSTREETDIRECTION();
		String html="<select name=\"" + userAtribute + "\""  + ">";
		html +="<option value=\"\" " + getSelected(value,"") + ">-</option>";
		for (Iterator iter= AddressAbrev.getAllDirectionsValues().iterator(); iter.hasNext();)
		{
			String direction=(String) iter.next();
			html +="<option value=\"" + direction + "\" " + getSelected(value, direction) + ">"+direction+"</option>";
		}
		html +="</select>";
		return html;
	}
	
	// for user pages
	public static String getSufixesCombo(String userAtribute, UserAttributes ua)
	{
		String value = ua.getSTREETSUFFIX();
		String html="<select name=\"" + userAtribute + "\"" + ">";
		html +="<option value=\"\" " + getSelected(value,"") + ">-</option>";
		for (Iterator iter= AddressAbrev.getAllStreetSuffixes().iterator(); iter.hasNext();)
		{
			String sufix=(String) iter.next();
			html +="<option value=\"" + sufix + "\" " + getSelected(value, sufix) + ">"+sufix+"</option>";
		}
		html +="</select>";
		return html;
	}
	
	public static String getSelected(String value, String compValue)
	{
		return (value.equals(compValue)? "selected" : "");
	}
	
    public static String getCountiesSelect( String selectedCounty, String selectedState,long searchId )
    {
        //used in search sites config page
        UserAttributes currentUser = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
        return getCountiesSelect(selectedCounty, selectedState, currentUser);
    }
    
    public static String getCountiesSelect(String selectedCounty, String selectedState, UserAttributes currentUser){    
        String selectStr = "";
        
        Vector<CountyState> counties=new Vector<CountyState>();
        try
        {
        	counties = DBManager.getCounties(Integer.parseInt(selectedState));
        }
        catch (BaseException ignoredException)
        {
            logger.error(" ERRor when fetching counties: ", ignoredException);
            ignoredException.printStackTrace();
        }
        
        long countyID = 0;
        try {
        	if(org.apache.commons.lang.StringUtils.isNotBlank(selectedCounty)) {
        		countyID = Long.parseLong(selectedCounty);
        	}
        } catch (Exception ignored) {
        	logger.error("Somebody gave countyID as [" + selectedCounty + "] and this is wrong", ignored);
        }
        
        selectStr +="<option value=\"0\" " + ( countyID == 0 ? "selected" : "") + " >Select One</option>";
        
        for (CountyState county : counties) {
			String countyName =StringUtils.prepareStringForHTML(county.getCountyName());
            if( currentUser.isAllowedCounty( (int)county.getCountyId() ) )
            {
                selectStr +="<option value=\"" +county.getCountyId() +"\" " + (county.getCountyId() == countyID?"selected":"") + ">";
                selectStr += countyName;
                selectStr +="</option>";
            }
        }
        
        return selectStr;
    }
        
	public static String getCountiesCombo(String countySearchAtribute,String stateSearchAtribute, SearchAttributes sa, String extra)
	{	
		//used in new TS jsp
		String stateFromSA = sa.getAtribute (stateSearchAtribute);
		String countyFromSA = sa.getAtribute (countySearchAtribute);
		
		//logger.debug("countyFromSA" + countyFromSA);
        /*
		Vector counties=new Vector();
		try
		{
			counties= Counties.getCounties(stateFromSA);
		}
		catch (BaseException ignoredException)
		{
			logger.error(" ERRor when fetching counties: ", ignoredException);
			ignoredException.printStackTrace();
		}*/
		String html="<select name=\"" + countySearchAtribute + "\" " + extra +" style=\"width:100%\">";
		/*
		int countyID = 0;
		try {
			countyID = Integer.parseInt(countyFromSA);
		} catch (Exception ignored) {}
		
		html +="<option value=\"0\" " + ( countyID == 0 ? "selected" : "") + " >Select One</option>";
		
		for(int i=0;i<counties.size();i++)
		{
			County county=(County)counties.elementAt(i);			
			String countyName =StringUtils.prepareStringForHTML(county.getName());
			BigDecimal countyId = county.getCountyId();
			//logger.debug("countyName" + countyName);
			html +="<option value=\"" +countyId +"\" " + (countyId.toString().equals(countyFromSA)?"selected":"") + ">";
			html += countyName;
			html +="</option>";
		}*/
        html += getCountiesSelect( countyFromSA, stateFromSA ,sa.getSearchId());
		html +="</select>";
		return html;
	}
    
    public static String getStatesSelect( String selectedState )
    {
        String stateSelect = "";
		
        Vector states=new Vector();
        try
        {
            states= State.getStates();
        }catch (BaseException ignoredException)
        {
            logger.error(" ERRor when fetching states: ", ignoredException);
            ignoredException.printStackTrace();
        }
        
        stateSelect +="<option value=\"0\">Select One</option>";
        for(int i=0;i<states.size();i++)
        {
            State  stateElm = (State)states.elementAt(i);
            String stateAbv = stateElm.getStateAbv().trim();
            stateSelect +="<option id=\""+ stateAbv + "\""+" value=\"" + /*stateAbv*/stateElm.getStateId()+ "\" " + (stateElm.getStateId().toString().equals( selectedState )?"selected":"") /*(stateAbv.equals(sa.getAtribute(stateSearchAtribute))?"selected":"")*/ +">";
            stateSelect +=StringUtils.prepareStringForHTML(stateElm.getName().trim());
            stateSelect +="</option>";
        }
        
        return stateSelect;
    }
    
    public static String getStatesSelect( String selectedState, long searchId)
    {	//returns only the states allowed for the current user, identified by searchId
        
        UserAttributes currentUser = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
        return getStatesSelect(selectedState, currentUser);
    }
    
    public static String getStatesSelect(String selectedState, UserAttributes currentUser){
    	String stateSelect = "";
        Vector states=new Vector();
        try
        {
            states= State.getStates();
        }catch (BaseException ignoredException)
        {
            logger.error(" ERRor when fetching states: ", ignoredException);
            ignoredException.printStackTrace();
        }
        
        stateSelect +="<option value=\"0\">Select One</option>";
        for(int i=0;i<states.size();i++)
        {
        	
            State  stateElm = (State)states.elementAt(i);
            if (currentUser.isAllowedState(stateElm.getStateId())){
            	String stateAbv = stateElm.getStateAbv().trim();
            	stateSelect +="<option id=\""+ stateAbv + "\""+" value=\"" + /*stateAbv*/stateElm.getStateId()+ "\" " + (stateElm.getStateId().toString().equals( selectedState )?"selected":"") /*(stateAbv.equals(sa.getAtribute(stateSearchAtribute))?"selected":"")*/ +">";
            	stateSelect +=StringUtils.prepareStringForHTML(stateElm.getName().trim());
            	stateSelect +="</option>";
            }
        }
        
        return stateSelect;
    }    
    
    public static String getStatesCombo(String comboName, String extra, String currentState){
    	String html="<SELECT NAME=\"" + comboName + "\" ID=\"" + comboName + "\" " + extra + " style=\"width:100%\" >";
        html += getStatesSelect(currentState);
		html +="</SELECT>";
		return html;
    }
    
    public static String getStatesCombo(String comboName, String extra, String currentState, UserAttributes ua){
    	String html="<SELECT NAME=\"" + comboName + "\" ID=\"" + comboName + "\" " + extra + " style=\"width:100%\" >";
        html += getStatesSelect(currentState, ua);
		html +="</SELECT>";
		return html;
    }
    
    public static String getCountiesCombo(String comboName, String extra, String currentState, String currentCounty,long searchId){
    	String html="<SELECT NAME=\"" + comboName + "\"  ID=\"" + comboName + "\" " + extra + " style=\"width:100%\" >";
        html += getCountiesSelect(currentCounty, currentState,searchId);
		html +="</SELECT>";
		return html;
    }
    
    public static String getCountiesCombo(String comboName, String extra, String currentState, String currentCounty, UserAttributes ua){
    	String html="<SELECT NAME=\"" + comboName + "\"  ID=\"" + comboName + "\" " + extra + " style=\"width:100%\" >";
        html += getCountiesSelect(currentCounty, currentState, ua);
		html +="</SELECT>";
		return html;
    }
    
	public static String getStatesCombo(String stateSearchAtribute,String formName, SearchAttributes sa, String extra)
	{
        /*
		Vector states=new Vector();
		try
		{
			states= State.getStates();
		}catch (BaseException ignoredException)
		{
			logger.error(" ERRor when fetching states: ", ignoredException);
			ignoredException.printStackTrace();
		}
		*/
		String html="<SELECT NAME=\"" +stateSearchAtribute+"\" " + extra + " style=\"width:100%\" >";
        
//		html +="<option value=\"0\">Select One</option>";
//		for(int i=0;i<states.size();i++)
//		{
//			State  stateElm = (State)states.elementAt(i);
//			String stateAbv = stateElm.getStateAbv().trim();
//			html +="<option value=\"" + /*stateAbv*/stateElm.getStateId()+ "\" " + (stateElm.getStateId().toString().equals(sa.getAtribute(stateSearchAtribute))?"selected":"") /*(stateAbv.equals(sa.getAtribute(stateSearchAtribute))?"selected":"")*/ +">";
//			html +=StringUtils.prepareStringForHTML(stateElm.getName().trim());
//			html +="</option>";
//		}
        html += getStatesSelect( sa.getAtribute( stateSearchAtribute ), sa.getSearchId() );
		html +="</SELECT>";
		return html;
	}
	public static String getStatesCombo(String stateSearchAtribute)
	{
		Vector states=new Vector();
		try
		{
			states= State.getStates();
		}catch (BaseException ignoredException)
		{
			logger.error(" ERRor when fetching states: ", ignoredException);
			ignoredException.printStackTrace();
		}
		
		String html="<SELECT NAME=\"" + stateSearchAtribute + "\" style=\"width:100%\" >";
		html +="<option value=\"0\">Select One</option>";
		for(int i=0;i<states.size();i++)
		{
			State  stateElm = (State)states.elementAt(i);
			//String stateAbv = stateElm.getStateAbv().trim();
			html += "<option value=\"" + stateElm.getStateId() + "\" >";
			html += StringUtils.prepareStringForHTML(stateElm.getName().trim());
			html += "</option>";
		}
		html +="</SELECT>";
		return html;
	}
	
	public static String getStatesCombo(String stateSearchAtribute, String extraHtml)
	{
		Vector states=new Vector();
		try
		{
			states= State.getStates();
		}catch (BaseException ignoredException)
		{
			logger.error(" ERRor when fetching states: ", ignoredException);
			ignoredException.printStackTrace();
		}
		
		String html="<SELECT NAME=\"" + stateSearchAtribute + "\"" + extraHtml + ">";
		html +="<option value=\"0\">Select One</option>";
		for(int i=0;i<states.size();i++)
		{
			State  stateElm = (State)states.elementAt(i);
			//String stateAbv = stateElm.getStateAbv().trim();
			html += "<option value=\"" + stateElm.getStateId() + "\" >";
			html += StringUtils.prepareStringForHTML(stateElm.getName().trim());
			html += "</option>";
		}
		html +="</SELECT>";
		return html;
	}
	
	// for users pages
	public static String getStatesCombo(String stateUserAtribute, String formName, UserAttributes ua, String extra)
	{
		Vector states=new Vector();
		try
		{
			states= State.getStates();
		}catch (BaseException ignoredException)
		{
			logger.error(" ERRor when fetching states: ", ignoredException);
			ignoredException.printStackTrace();
		}
		
		String html="<SELECT NAME=\"" +stateUserAtribute+"\" " + extra + " style=\"width:100%\" >";
		html +="<option value=\"0\">Select One</option>";
		for(int i=0;i<states.size();i++)
		{
			State  stateElm = (State)states.elementAt(i);
			String stateAbv = stateElm.getStateId().toString();
			html +="<option value=\"" + stateAbv+ "\" " + (stateAbv.equals( ua.getSTATE_ID()!=null?ua.getSTATE_ID().toString():"0") ?"selected":"") +">";
			html +=StringUtils.prepareStringForHTML(stateElm.getName().trim());
			html +="</option>";
		}
		html +="</SELECT>";
		return html;
	}
	
	public static boolean existAgent(long searchId){
		if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent() == null)
			return false;
		return true;
	}
	
	public static String getHTMLAgentDetails(long searchId) {
	    
	    return getHTMLUserDetails(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent(),searchId);
	}
	
	public static String getHTMLUserDetails(UserAttributes user,long searchId) {
		String innerHTML = new String();
		String[] tokDetails = getTokUserDetails(user,searchId);
		innerHTML = "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\">";
		innerHTML += "<tr class=\"headerRow\"><td colspan=\"2\"><strong>Agent details</strong></td><tr>";
		for (int i=0; i < tokDetails.length; i = i + 2){
			if ((i/2)%2 == 1){
				innerHTML += "<tr class=row1><td width=\"50%\">" + tokDetails[i] 
						+ "</td><td><strong>" + tokDetails[i + 1] 
						+ "</strong></td></tr>";
			} else {
				innerHTML += "<tr class=row2><td width=\"50%\">" + tokDetails[i] 
						+ "</td><td><strong>" + tokDetails[i + 1] 
						+ "</strong></td></tr>";
			}
		}
		innerHTML += "</table>";
		return innerHTML;
	}
	
	public static String getAgentDetails(long searchId){
	    
	    return getUserDetails(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent(),searchId);
	}
	
	public static String getUserDetails(UserAttributes user,long searchId){
		String javascriptAlertText = new String();
		String[] tokDetails = getTokUserDetails(user,searchId);
		for (int i=0; i < tokDetails.length; i = i + 2){
			javascriptAlertText += tokDetails[i] + tokDetails[i + 1] + "\\n";
		}
		return javascriptAlertText;
	}
	
	public static String[] getTokAgentDetails(long searchId) {
	    return getTokUserDetails(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent(),searchId);
	}
	
	public static String[] getTokUserDetails(UserAttributes agent,long searchId)
	{
		String[] tokAgentInfo = new String[10];
		String distribModeText = new String();

		
		try{
			distribModeText += getTemapltes(agent);
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		//javascriptAlertText += "Agent information: \\n\\n";
		tokAgentInfo[0] = "Company: ";
		tokAgentInfo[1] = (agent!=null)?agent.getCOMPANY():"N/A";
		tokAgentInfo[2] = "Company address: ";
		tokAgentInfo[3] = (agent!=null)?agent.getWADDRESS():"N/A";
		tokAgentInfo[4] = "Distribution type: ";
		tokAgentInfo[5] = (agent!=null)?( agent.getDISTRIBUTION_TYPE().equals("0")?"PDF":"TIFF" ):"N/A";
		tokAgentInfo[6] = "Distribution mode: ";
		tokAgentInfo[7] = (agent!=null)?distribModeText:"N/A";
		tokAgentInfo[8] = "E-mail address: ";
		tokAgentInfo[9] = (agent!=null)?agent.getEMAIL():"N/A";
		
		return tokAgentInfo;
	}
	
	public static String getTemapltes(UserAttributes agent) {
		StringBuilder retHtml = new StringBuilder("<table  cellpadding=0 cellspacing=0>");
		if (agent == null)
			return "";
		List<CommunityTemplatesMapper> userTemplates = null;
		try {
			userTemplates = UserUtils.getUserTemplates(agent.getID().longValue(), -1, -1, -1, false);
		} catch (Exception e) {
			return "</strong><font color=red>No policy documents defined for this user</font><strong>";
		}
		if(userTemplates!=null){
			int i = 0;
			for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
				if (i%2 == 1) {
					retHtml.append("<tr class=row1><td nowrap>");
				} else {
					retHtml.append("<tr class=row2><td nowrap>");
				}
				
				retHtml.append("<strong>" + communityTemplatesMapper.getShortName()
						+ " - "
						+ communityTemplatesMapper.getName() + "</strong>");
				
				retHtml.append("</td></tr>");
				i++;
			}
			
		}
		retHtml.append("</table>");
		return retHtml.toString();
	}
	
	public static String getAgentCombo(String formName,SearchAttributes sa, HttpServletRequest request)
	{
		UserAttributes allAgents[] = null;
		BigDecimal commId = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCurrentCommunity().getID();
		
		allAgents = UserManager.getUsersInGroup(GroupAttributes.AG_ID, commId,false);
		
		String prefixHtml ="<SELECT NAME=\"agentCombo\" style=\"width: 275px\" onChange=\"" + getPopulateAgentHref(formName,sa) + "\">";
		
		UserAttributes currentAgent = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext().getAgent();
		HttpSession session = request.getSession(true);
        UserAttributes currentUser = ((User) session.getAttribute(SessionParams.CURRENT_USER)).getUserAttributes();
		if (currentUser.getGROUP().intValue() == UserAttributes.GROUP_AGENT) {
			InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext().setAgent(currentUser);
			return "<b><i>" + currentUser.getUserFullName() + "</i></b>";
		}
		
		long selectedUser = -1;
		try {
			String selectedUserString = currentUser.getID().toString();
			if(StringUtils.isNotEmpty(selectedUserString)) {
				selectedUser = Long.parseLong(selectedUserString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		UserI activeUser = null;
		
		UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
		int comId = commId.intValue();
		
		StringBuilder html = new StringBuilder();
		boolean someSelected = false;
		try {
			userManager.getAccess();
			if(selectedUser > -1) {
				activeUser = userManager.getUser(selectedUser);
			}
			boolean hasRestriction = activeUser!= null && activeUser.getRestriction().hasAgentAllowed();
			
			if(allAgents != null){
				for (UserI agents : userManager.getUsersByCommunity(comId, GroupAttributes.AG_ID)){
					for(int i=0;i<allAgents.length;i++)
					{
						UserAttributes agentAttributes = allAgents[i];
						String company = agentAttributes.getCOMPANY();
						
						int agentId = agentAttributes.getID().intValue();
						
						
						String agentFirstName = agentAttributes.getFIRSTNAME();
						String agentMiddleName = agentAttributes.getMIDDLENAME();
						String agentLastName = agentAttributes.getLASTNAME();
						
						if (!hasRestriction) {
							if (currentAgent != null && currentAgent.getID() != null){
								int currentAgentId = currentAgent.getID().intValue();
								if(agentId == currentAgentId) {
									someSelected = true;
								}
								html.append("<option value=\"").append(agentId).append("\" ").append(agentId == currentAgentId?"selected":"").append(">");
							} else {
								html.append("<option value=\"").append(agentId).append("\">");
							}

							html.append(StringUtils.HTMLEntityEncode((agentFirstName.equals("")?"":agentFirstName)))
									.append(" ")
									.append(StringUtils.HTMLEntityEncode((agentMiddleName.equals("")?"":(agentMiddleName.substring(0,1)+ "."))))
									.append(" ")
									.append(StringUtils.HTMLEntityEncode((agentLastName.equals("")?"":agentLastName)
											+ ("".equals(company) ? "" : (" (" + company.substring(0, company.length() > 3 ? 3 : company.length()) + ")"))	))
									.append("</option>");
						} else {
							
							if (agents.getUserId() == agentAttributes.getID().longValue()) {
								if (activeUser.getRestriction().isAgentAllowed((int)agents.getUserId())){
									if (currentAgent != null && currentAgent.getID() != null){
										int currentAgentId = currentAgent.getID().intValue();
										if(agentId == currentAgentId) {
											someSelected = true;
										}
										html.append("<option value=\"").append(agentId).append("\" ").append(agentId == currentAgentId?"selected":"").append(">");
									} else {
										html.append("<option value=\"").append(agentId).append("\">");
									}
									html.append(StringUtils.HTMLEntityEncode((agentFirstName.equals("")?"":agentFirstName)))
										.append(" ")
										.append(StringUtils.HTMLEntityEncode((agentMiddleName.equals("")?"":(agentMiddleName.substring(0,1)+ "."))))
										.append(" ")
										.append(StringUtils.HTMLEntityEncode((agentLastName.equals("")?"":agentLastName)
											+ ("".equals(company) ? "" : (" (" + company.substring(0, company.length() > 3 ? 3 : company.length()) + ")"))	))
										.append("</option>");
								}
							} 
						}
					}
					if (!hasRestriction)
						break;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(userManager != null) {
				userManager.releaseAccess();
			}
		}
		
		if(someSelected) {
			html.insert(0, prefixHtml + "<option value=\"-1\">Select One</option>").append("</SELECT>");
		} else {
			html.insert(0, prefixHtml + "<option value=\"-1\" selected>Select One</option>").append("</SELECT>");
		}
		
		return StringUtils.sortSelectListByText(html.toString(), false, false, true, true, true);
	}
	
	public static String getAgentCombo(String comboName, SearchAttributes sa, HttpServletRequest request, String extra, String currentAgentId,String agency) 
	{
		UserAttributes allAgents[] = null;
		BigDecimal commId = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCurrentCommunity().getID();
		HttpSession session = request.getSession(true);
		
		allAgents =
			(agency.equals("-1"))?
					allAgents = UserManager.getUsersInGroup(GroupAttributes.AG_ID, commId,false):
					UserManager.getUsersInGroup(GroupAttributes.AG_ID, commId,agency);
		String html="<SELECT NAME=\"" +comboName +  "\" ID=\"" + comboName + "\" " + extra + ">";
		
		UserAttributes currentAgent = null;
		try {
			 currentAgent =UserUtils.getUserFromId(new BigDecimal(currentAgentId));
			 if (currentAgent == null){
				 currentAgent = UserUtils.getUserFromId(Long.parseLong(request.getParameter("USER_ID")));
			 }
		}
		catch(Exception e) { }
			
        UserAttributes currentUser = ((User) session.getAttribute(SessionParams.CURRENT_USER)).getUserAttributes();
		if (currentUser.getGROUP().intValue() == UserAttributes.GROUP_AGENT) {
			InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext().setAgent(currentUser);
			html += "<option value=\""+ currentUser.getID() +"\" selected>"+currentUser.getUserFullName()+"</option>";
			html += "</select>";
			return html;
		}
		html += "<option value=\"-1\" selected>Select One</option>";
		
		long selectedUser = -1;
		try {
			String selectedUserString = currentUser.getID().toString();
			if(StringUtils.isNotEmpty(selectedUserString)) {
				selectedUser = Long.parseLong(selectedUserString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		UserI activeUser = null;
		UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
		int comId = commId.intValue();
		
		try {
			userManager.getAccess();
			if(selectedUser > -1) {
				activeUser = userManager.getUser(Long.parseLong(request.getParameter("USER_ID")));
			}
			boolean hasRestriction = activeUser!= null && activeUser.getRestriction().hasAgentAllowed();
			
			if(allAgents != null){
				for (UserI agents : userManager.getUsersByCommunity(comId, GroupAttributes.AG_ID)){
					for(int i=0;i<allAgents.length;i++)
					{
						String company = allAgents[i].getCOMPANY();
						
						if (!hasRestriction) {
							if (currentAgent != null && currentAgent.getID() != null){
								html += "<option value=\"" + allAgents[i].getID() +  "\" " 
										+ (allAgents[i].getID().toString().equals(
											currentAgent.getID().toString())?"selected":"") +">";
							} else {
								html += "<option value=\"" + allAgents[i].getID() +  "\">";
							}

							html +=
								    StringUtils.HTMLEntityEncode((allAgents[i].getFIRSTNAME().equals("")?"":allAgents[i].getFIRSTNAME()) + " "
									+ (allAgents[i].getMIDDLENAME().equals("")?"":(allAgents[i].getMIDDLENAME().substring(0,1)+ "."))
									+ " " + (allAgents[i].getLASTNAME().equals("")?"":allAgents[i].getLASTNAME())
									+ ("".equals(company) ? "" : (" (" + allAgents[i].getCOMPANY().substring(0, company.length() > 3 ? 3 : company.length()) + ")"))	);
							html += "</option>";
						} else {

							if (agents.getUserId() == allAgents[i].getID().longValue()) {
								if (activeUser.getRestriction().isAgentAllowed((int)agents.getUserId())){
									if (currentAgent != null && currentAgent.getID() != null){
										html += "<option value=\"" + allAgents[i].getID() +  "\" " 
												+ (allAgents[i].getID().toString().equals(
												currentAgent.getID().toString())?"selected":"") +">";
									} else {
											html += "<option value=\"" + allAgents[i].getID() +  "\">";
									}
									html +=
											StringUtils.HTMLEntityEncode((allAgents[i].getFIRSTNAME().equals("")?"":allAgents[i].getFIRSTNAME()) + " "
											+ (allAgents[i].getMIDDLENAME().equals("")?"":(allAgents[i].getMIDDLENAME().substring(0,1)+ "."))
											+ " " + (allAgents[i].getLASTNAME().equals("")?"":allAgents[i].getLASTNAME())
											+ ("".equals(company) ? "" : (" (" + allAgents[i].getCOMPANY().substring(0, company.length() > 3 ? 3 : company.length()) + ")"))	);
									html += "</option>";
								}
							} 
						}
					}
					if (!hasRestriction)
						break;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(userManager != null) {
				userManager.releaseAccess();
			}
		}
		html +="</SELECT>";
		return html;
	}
	
	private static String getPopulateAgentHref(String formName,SearchAttributes sa)
		{
			/*return "javascript:document."
				+ formName
				+ ".action='"
				+ URLMaping.path
				+ URLMaping.validateInputs
				+ "'"
				+ ";window.document.forms[0].changeAgent.value=1" 
				+ ";window.document.forms[0].submit()";*/
        
            return "javascript: changeAgentAJAX();";
		}
	
	/**
	 * Create the radio buttons for servers types
	 * @param global current search
	 * @param extra extra exta html code, like events and attributes
	 * @param parentSite is for parent site
	 * @return the html code of the radio group
	 */
	public static String getServerTypesSelection(Search global, String extra, boolean parentSite) throws BaseException{
		
		String retVal = "";
		global.setSearchType(Search.PARENT_SITE_SEARCH);		//force Search on PS
		int productId = global.getProductId();
		int commId = global.getCommId();

		Map<String, DataSite> searchCounty = null;
		try{
			County county = County.getCounty(Integer.parseInt(global.getSa().getAtribute(SearchAttributes.P_COUNTY)));
			State state = State.getState(Integer.parseInt(global.getSa().getAtribute(SearchAttributes.P_STATE)));
			String name = county.getName();
			
			System.err.println( " --- newTSFormater -- getServerTypesSelection - Comunity State County  ................ " + commId + state.getStateAbv() + name );
			
			searchCounty = HashCountyToIndex.getServers(Integer.parseInt(global.getSa().getAtribute(SearchAttributes.P_COUNTY)));
		}
		catch(Exception e){
			logger.error(e);
			e.printStackTrace();
		}
		
		if(searchCounty == null || searchCounty.isEmpty() ){
			return "";
		}
			
		int selectedValue = parentSite?global.getCityCheckedParentSite():global.getCitychecked();		
		List<String> options = new ArrayList<String>();
		int serverCurrentState = 0;		
		DataSite firstServer = null;
		
		boolean forceFirstServerSelected = true;
		
		ArrayList<DataSite> allSites = new ArrayList<DataSite>(searchCounty.values());
		Collections.sort(allSites);
		
		for (DataSite searchServer : allSites) {
			if(!searchServer.isEnableSite(commId)) {
				continue;
			}
			if(firstServer == null) {
				firstServer = searchServer;
			}
			if(!parentSite) {
				if(searchServer.isEnabledAutomatic(productId, commId)) {
					forceFirstServerSelected = false;
					break;
				}
			} else {
				
			}
			
		}
		firstServer = null;
		
		// append the enabled sites
		for (DataSite searchServer: allSites) {
			
			if(!searchServer.isEnableSite(commId)) {
				continue;
			}
			
			String enabled = extra; 			
			String optVal = searchServer.getSiteTypeAbrev();
							
			if(!parentSite){ // search page		
				
				// skip the unimplemented sites
				if(searchServer.isEnabledLinkOnly(commId))
				{//continue; 
					enabled = "disabled";
					selectedValue = -1;
				}	
				
				// skip the disabled sites
				if(!searchServer.isEnableSite(commId))
				{// continue;
					enabled = "disabled";
					selectedValue = -1;
				}
				
				// determine hint
				if(searchServer.isEnabledAutomatic(productId, commId)){
					serverCurrentState = PARENT_SITE_ENABLED_AUTOMATIC_ENABLED;
				} else {
					enabled = "disabled";
					if(selectedValue == searchServer.getType()){
						selectedValue = -1;
					}
					serverCurrentState = PARENT_SITE_ENABLED_AUTOMATIC_DISABLED;
				}				
					
				if (firstServer == null && (searchServer.isEnabledAutomatic(productId, commId) || forceFirstServerSelected)) {
					firstServer = searchServer;
					HashCountyToIndex.setSearchServer(global, firstServer.getType());
					options.add(getServerType(optVal, searchServer.getType(), searchServer.getType(), enabled, serverCurrentState, searchServer, global));
					selectedValue = -1;
				} else {
					options.add(getServerType(optVal, searchServer.getType(), selectedValue, enabled, serverCurrentState, searchServer, global));
				}

					options.add(getServerLinkHidden(searchServer.getDocumentServerLink(), searchServer.getType()));
					options.add(getServerIdHidden(
							(int)TSServersFactory.getSiteId(searchServer.getStateAbrev(), searchServer.getCountyName(), searchServer.getSiteTypeAbrev()),
							searchServer.getType()));
				} else { // parent site	
					
				// skip the disabled sites that are implemented
				if(!searchServer.isEnableSite(commId) && !searchServer.isEnabledLinkOnly(commId))
				{// continue;
					enabled = "disabled";
					selectedValue = -1;
				}
				
				// determine hint
				if(searchServer.isEnabledAutomatic(productId, commId)){
					serverCurrentState = PARENT_SITE_ENABLED_AUTOMATIC_ENABLED;
				} else if(searchServer.isEnableSite(commId)){
					serverCurrentState = PARENT_SITE_ENABLED_AUTOMATIC_DISABLED;
				} else if(searchServer.isEnabledLinkOnly(commId)){
					serverCurrentState = ALL_DISABLED;
				} else {
					serverCurrentState = ALL_DISABLED;
				}
				
							    	
				options.add(getServerType(optVal, searchServer.getType(), selectedValue, extra, serverCurrentState, searchServer, global));
				options.add(getServerLinkHidden(searchServer.getDocumentServerLink(), searchServer.getType()));
				options.add(getServerIdHidden(
						(int)TSServersFactory.getSiteId(searchServer.getStateAbrev(), searchServer.getCountyName(), searchServer.getSiteTypeAbrev()),
						searchServer.getType()));
			}			
			

			
			// update the first server value, but only if the server was enabled
			if (firstServer == null && !"disabled".equals(enabled)) {
				firstServer = searchServer;
			}
		}


		// construct the return html code
		retVal =  StringUtils.join(options, "&nbsp;");
		
		return retVal;		
	}
	
	private static String getServerLinkHidden(String link, int serverId) {
		return "<input type='hidden' name='link"+serverId+"' id='link"+serverId+"' value='"+link+"'>";
	}
	
	private static String getServerIdHidden(int serverId, int serevrType) {
		return "<input type='hidden' name='serverId"+serevrType+"' id='serverId"+serevrType+"' value='"+serverId+"'>";
	}
	
	private static  String getServerType(String name, int value, int selectedIndex, String extra, int serverCurrentState, DataSite siteData, Search global){
		
	    String param = "cityChecked";
		String checked = ((selectedIndex == value) ? "checked" : "");
		String hint = "";
		String radio_but_def = "";

		String tsServerClassName = siteData.getTsServerClassName();
		boolean hasNoModules = false;
		
		if (StringUtils.isEmpty(tsServerClassName) && Util.isSiteEnabled(siteData.getEnabled(global.getCommId()))) {
			hasNoModules = true;
		} else {
			int moduleCount = 0;

			long siteId = TSServersFactory.getSiteId(siteData.getStateAbrev(), siteData.getCountyName(), siteData.getSiteTypeAbrev());
			TSInterface tsServer = TSServersFactory.GetServerInstance(Long.valueOf(siteId).intValue(), global.getID());
			if (tsServer != null) {
				TSServerInfo defaultServerInfo = tsServer.getDefaultServerInfo();
				moduleCount = defaultServerInfo.getModuleCount();
			} else {
				moduleCount = 0;
			}
			hasNoModules = (moduleCount == 0);
		}
		
		if (value == 0){
			int i = 98;
			String val1 = "" + i;//98
	    	String val2 = "" + (++i);//99
	    	String val3 = "" + (++i);//100
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    name = "AO";
		}else if (value == 1) {
			int i = 101;
			String val1 = "" + i;//101
	    	String val2 = "" + (++i);//102
	    	String val3 = "" + (++i);//103
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    name = "isCityTax"; YA
		} else if ((value == 2)) {
			String cityName = org.apache.commons.lang.StringUtils.defaultIfEmpty(siteData.getCityName(), "");
		    if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_DISABLED && !hasNoModules)
		    {
		    	hint = "showCityName(104,Style[9],'"+ cityName +"')";
		    }
		    else if (serverCurrentState==ALL_DISABLED || hasNoModules)
		    {
		    	hint = "showCityName(105,Style[9],'"+ cityName +"')";
		    }
		    else if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_ENABLED)
		    {
		    	hint = "showCityName(106,Style[9],'"+ cityName +"')";
		    }
		    //YB,YC,YD.YE,YF 
		    //in order for all the type of city tax to show up their city  name a hack had to be made by setting the starter index  tooltipStyle.js for 
		    // the first index for city sites to 257 and then calculate the correct index with the help of serverCurrentState and the siteTypeId. 
		    //If the current indexes for YA,YB,YC are to be modified pay attention to this if.
		} if ((value == 28)||(value == 29)||(value == 30)||(value == 31)||(value == 32)) {
			String cityName = org.apache.commons.lang.StringUtils.defaultIfEmpty(siteData.getCityName(), "");
			int toolTipIndexInToolTipStyleJS = 257 + (value-28)*3+serverCurrentState; 
		    if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_DISABLED && !hasNoModules){
		    	hint = "showCityName("+ toolTipIndexInToolTipStyleJS +",Style[9],'"+ cityName +"')";
		    }
		    else if (serverCurrentState==ALL_DISABLED || hasNoModules){
		    	hint = "showCityName("+ toolTipIndexInToolTipStyleJS +",Style[9],'"+ cityName +"')";
		    }
		    else if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_ENABLED){
		    	hint = "showCityName(" + toolTipIndexInToolTipStyleJS + ",Style[9],'"+ cityName +"')";
		    }
		} else if (value == 3) {
			int i = 107;
			String val1 = "" + i;//107
	    	String val2 = "" + (++i);//108
	    	String val3 = "" + (++i);//109
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    name = "RO";
		} else if (value == 4) {
			int i = 110;
			String val1 = "" + i;//110
	    	String val2 = "" + (++i);//111
	    	String val3 = "" + (++i);//112
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    hint = "Daily News";//DN
		} else if (value == 5) {
			int i = 113;
			String val1 = "" + i;//113
	    	String val2 = "" + (++i);//114
	    	String val3 = "" + (++i);//115
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    name = "PA";
		} else if (value == 6){
			int i = 116;
			String val1 = "" + i;//116
	    	String val2 = "" + (++i);//117
	    	String val3 = "" + (++i);//118
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);					    
//			hint = "Uniform Commercial Code";//CC
		} else if (value == 7) {
			int i = 119;
			String val1 = "" + i;//119
	    	String val2 = "" + (++i);//120
	    	String val3 = "" + (++i);//121
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);		    		    
//		    hint = "Courts";//CO
		} else if (value == 8) {
			int i = 122;
			String val1 = "" + i;//122
	    	String val2 = "" + (++i);//123
	    	String val3 = "" + (++i);//124
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    hint = "Pacer";//PC
		} else if (value == 9) {
			int i = 125;
			String val1 = "" + i;//125
	    	String val2 = "" + (++i);//126
	    	String val3 = "" + (++i);//127
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);					    
//		    hint = "Orbit";//OR
		} else if (value == 10) {
			int i = 128;
			String val1 = "" + i;//128
	    	String val2 = "" + (++i);//129
	    	String val3 = "" + (++i);//130
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    hint = "Metropolitan Sewer District";//MS
		} else if (value == 11) {
			int i = 131;
			String val1 = "" + i;//131
	    	String val2 = "" + (++i);//132
	    	String val3 = "" + (++i);//133
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    hint = "Probate Courts";//PR
		} else if (value == 12) {
			int i = 162;
			String val1 = "" + i;//162
	    	String val2 = "" + (++i);//163
	    	String val3 = "" + (++i);//164
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		    hint = "Stewart Prior files";//SP
		} else if (value == 13) {
			int i = 153;
			String val1 = "" + i;//153
	    	String val2 = "" + (++i);//154
	    	String val3 = "" + (++i);//155
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//	    hint = "Data Trace";//DT
		} else if (value == 14) {
			int i = 156;
			String val1 = "" + i;//156
	    	String val2 = "" + (++i);//157
	    	String val3 = "" + (++i);//158
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//	    hint = "Land data IL";//DL -> LA
		} else if (value == 15) {
			int i = 159;
			String val1 = "" + i;//159
	    	String val2 = "" + (++i);//160
	    	String val3 = "" + (++i);//161
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
//		hint = "Information Services IL";//AP->IS
		}
		else if (value == 16) {
			int i = 165;
			String val1 = "" + i;//165
	    	String val2 = "" + (++i);//166
	    	String val3 = "" + (++i);//167
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
	//	hint = "Information Services IL";//AP->IS
		}
		else if (value == 19) {
			int i = 181;
			String val1 = "" + i;//181
	    	String val2 = "" + (++i);//182
	    	String val3 = "" + (++i);//183
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
	//	hint = "TU IL";//
		} else if (value == 20) {
			int i = 207;
			String val1 = "" + i;//207
	    	String val2 = "" + (++i);//208
	    	String val3 = "" + (++i);//209
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 21) {
			hint = setHintValues(serverCurrentState, hint, "319", "320", "321", hasNoModules);
 		} else if (value == 23) {
			int i = 239;
			String val1 = "" + i;//239
	    	String val2 = "" + (++i);//240
	    	String val3 = "" + (++i);//241
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 24) {
			int i = 242;
			String val1 = "" + i;//242
	    	String val2 = "" + (++i);//243
	    	String val3 = "" + (++i);//244
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 25) {
			int i = 245;
			String val1 = "" + i;//245
	    	String val2 = "" + (++i);//246
	    	String val3 = "" + (++i);//247
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 26) {
			int i = 248;
			String val1 = "" + i;//248
	    	String val2 = "" + (++i);//249
	    	String val3 = "" + (++i);//250
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 27) {
			int i = 251;
			String val1 = "" + i;//251
	    	String val2 = "" + (++i);//252
	    	String val3 = "" + (++i);//253
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 22) {
			int i = 254;
			String val1 = "" + i;//254
	    	String val2 = "" + (++i);//255
	    	String val3 = "" + (++i);//256
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 33) {
			int i = 272;
			String val1 = "" + i;//272
	    	String val2 = "" + (++i);//273
	    	String val3 = "" + (++i);//274
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 34) {//SB
			int i = 275;
			String val1 = "" + i;//275
	    	String val2 = "" + (++i);//276
	    	String val3 = "" + (++i);//277
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 37) {//R2
			int i = 278;
			String val1 = "" + i;//278
	    	String val2 = "" + (++i);//279
	    	String val3 = "" + (++i);//280
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 36) {//YG
			int i = 281;
			String val1 = "" + i;//281
	    	String val2 = "" + (++i);//282
	    	String val3 = "" + (++i);//283
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 39) {//HO
			int i = 286;
			String val1 = "" + i;//286
	    	String val2 = "" + (++i);//287
	    	String val3 = "" + (++i);//288
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 40) {//WP
			int i = 289;
			String val1 = "" + i;//289
	    	String val2 = "" + (++i);//290
	    	String val3 = "" + (++i);//291
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 42) {//NR
			int i = 295;
			String val1 = "" + i;//295
	    	String val2 = "" + (++i);//296
	    	String val3 = "" + (++i);//297
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 43) {//DD
			int i = 292;
			String val1 = "" + i;//292
	    	String val2 = "" + (++i);//293
	    	String val3 = "" + (++i);//294
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 41) {//LN
			int i = 298;
			String val1 = "" + i;//298
	    	String val2 = "" + (++i);//299
	    	String val3 = "" + (++i);//300
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 45) {//DI
			int i = 301;
			String val1 = "" + i;//301
	    	String val2 = "" + (++i);//302
	    	String val3 = "" + (++i);//303
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		}else if (value == 44) {//FD
			int i = 304;
			String val1 = "" + i;//304
	    	String val2 = "" + (++i);//305
	    	String val3 = "" + (++i);//306
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 46) {//OI
			int i = 307;
			String val1 = "" + i;//307
	    	String val2 = "" + (++i);//308
	    	String val3 = "" + (++i);//309
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 38) {//AC
		    hint = setHintValues(serverCurrentState, hint, "310", "311", "312", hasNoModules);
		} else if (value == 47) {//AM
			int i = 313;
			String val1 = "" + i;//313
	    	String val2 = "" + (++i);//314
	    	String val3 = "" + (++i);//315
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 48) {//DR
			int i = 316;
			String val1 = "" + i;//316
	    	String val2 = "" + (++i);//317
	    	String val3 = "" + (++i);//318
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 51) {//IL
			int i = 322;
			String val1 = "" + i;//322
	    	String val2 = "" + (++i);//323
	    	String val3 = "" + (++i);//324
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 50) {//BT
			int i = 325;
			String val1 = "" + i;//325
	    	String val2 = "" + (++i);//326
	    	String val3 = "" + (++i);//327
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 52) {//LW
			int i = 328;
			String val1 = "" + i;//328
	    	String val2 = "" + (++i);//329
	    	String val3 = "" + (++i);//330
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 53) {//MH
			int i = 331;
			String val1 = "" + i;//331
	    	String val2 = "" + (++i);//332
	    	String val3 = "" + (++i);//333
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == 54) {//PD
			int i = 334;
			String val1 = "" + i;//334
	    	String val2 = "" + (++i);//335
	    	String val3 = "" + (++i);//336
		    hint = setHintValues(serverCurrentState, hint, val1, val2, val3, hasNoModules);
		} else if (value == GWTDataSite.ADI_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "337", "338", "339", hasNoModules);
		} else if (value == GWTDataSite.ATI_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "343", "344", "345", hasNoModules);
		} else if (value == GWTDataSite.MC_TYPE) {//MC
		    hint = setHintValues(serverCurrentState, hint, "340", "341", "342", hasNoModules);
		} else if (value == GWTDataSite.NETR_TYPE) {//NETR
		    hint = setHintValues(serverCurrentState, hint, "346", "347", "348", hasNoModules);
		} else if (value == GWTDataSite.SPS_TYPE) {//SPS
		    hint = setHintValues(serverCurrentState, hint, "349", "350", "351", hasNoModules);
		} else if (value == GWTDataSite.VU_TYPE) {//VU
		    hint = setHintValues(serverCurrentState, hint, "352", "353", "354", hasNoModules);
		} else if (value == GWTDataSite.ATS_TYPE) {	//ATS
		    hint = setHintValues(serverCurrentState, hint, "355", "356", "357", hasNoModules);
		} else if (value == GWTDataSite.NTN_TYPE) {//NTN
		    hint = setHintValues(serverCurrentState, hint, "358", "359", "360", hasNoModules);
		} else if (value == GWTDataSite.DG_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "361", "362", "363", hasNoModules);
		} else if (value == GWTDataSite.NA_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "364", "365", "366", hasNoModules);
		} else if (value == GWTDataSite.SRC_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "367", "368", "369", hasNoModules);
		} else if (value == GWTDataSite.BS_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "370", "371", "372", hasNoModules);
		} else if (value == GWTDataSite.MERS_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "373", "374", "375", hasNoModules);
		} else if (value == GWTDataSite.GM_TYPE) {
		    hint = setHintValues(serverCurrentState, hint, "376", "377", "378", hasNoModules);
		} else if (value == GWTDataSite.COM_TYPE) { //COM
			//	hint: "Court site on OH"
		    hint = setHintValues(serverCurrentState, hint, "379", "380", "381", hasNoModules);	
		} else if (value == GWTDataSite.DMV_TYPE) { //DMV
			hint = setHintValues(serverCurrentState, hint, "382", "383", "384", hasNoModules);	
		} else if (value == GWTDataSite.BOR_TYPE) {
			hint = setHintValues(serverCurrentState, hint, "385", "386", "387", hasNoModules);	
		} else if (value == GWTDataSite.AOM_TYPE) {
			hint = setHintValues(serverCurrentState, hint, "388", "389", "390", hasNoModules);	
		} else if (value == GWTDataSite.PRI_TYPE) {
			hint = setHintValues(serverCurrentState, hint, "391", "392", "393", hasNoModules);	
		} else if (value == GWTDataSite.TR2_TYPE) {
			hint = setHintValues(serverCurrentState, hint, "394", "395", "396", hasNoModules);	
		} else if (value == GWTDataSite.RVI_TYPE) {
			hint = setHintValues(serverCurrentState, hint, "397", "398", "399", hasNoModules);	
		}
		
		int enableATSStatus = -1;
		if(serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_DISABLED && !hasNoModules) {
			enableATSStatus = PARENT_SITE_ENABLED_AUTOMATIC_DISABLED;
		} else if(serverCurrentState==ALL_DISABLED || hasNoModules) {
			enableATSStatus = ALL_DISABLED;
		} else {
			enableATSStatus = PARENT_SITE_ENABLED_AUTOMATIC_ENABLED;
		}
		
		
		if (extra.equalsIgnoreCase("disabled") ) {
			StringBuilder stringBuilder = new StringBuilder();
			addSiteName(name, enableATSStatus, stringBuilder);	
			stringBuilder.append("<input type=\"radio\" name=\"");
			stringBuilder.append(param);
			stringBuilder.append("\" onMouseOver=\"");
			stringBuilder.append(hint);
			stringBuilder.append("\" value=\"");
			stringBuilder.append(value);
			stringBuilder.append("\" ");
			stringBuilder.append(checked);
			stringBuilder.append(" ");
			if(!extra.equalsIgnoreCase("disabled")) { 
				stringBuilder.append(extra);
			}
			
			stringBuilder.append(" enableATSStatus=\"" + enableATSStatus + "\" ");
			
			stringBuilder.append(" enabled onmouseout=\"htm()\" >");
			radio_but_def = stringBuilder.toString();
		} else {
			StringBuilder stringBuilder = new StringBuilder();
			addSiteName(name, enableATSStatus, stringBuilder);
			stringBuilder.append("<input type=\"radio\" name=\"");
			stringBuilder.append(param);
			stringBuilder.append("\"");
			stringBuilder.append(" onMouseOver=\"");
			stringBuilder.append(hint);
			stringBuilder.append("\" value=\"");
			stringBuilder.append(value);
			stringBuilder.append("\" ");
			stringBuilder.append(checked);
			stringBuilder.append(" ");
			stringBuilder.append(extra);
			
			stringBuilder.append(" enableATSStatus=\"" + enableATSStatus + "\" ");
			stringBuilder.append(" onmouseout=\"htm()\" >");
			radio_but_def = stringBuilder.toString();
		}

		return radio_but_def;
	}

	public static String setHintValues(int serverCurrentState, String hint, String val1, String val2, String val3, boolean hasNoModules) {
		if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_DISABLED && !hasNoModules)
		{
			hint = "stm(" + val1 + ",Style[9])";
		}
		else if (serverCurrentState==ALL_DISABLED || hasNoModules)
		{
			hint = "stm(" + val2 + ",Style[9])";
		}
		else if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_ENABLED)
		{
			hint = "stm(" + val3 + ",Style[9])";
		}
		return hint;
	}

	public static void addSiteName(String name, int serverCurrentState, StringBuilder stringBuilder) {
		if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_DISABLED || serverCurrentState==ALL_DISABLED){
			stringBuilder.append("<font color=\"#808080\">");
		}
		stringBuilder.append(name);
		if (serverCurrentState==PARENT_SITE_ENABLED_AUTOMATIC_DISABLED || serverCurrentState==ALL_DISABLED){
			stringBuilder.append("</font>");
		}
	}
	
	/*Comboboxes for my Ats Settings*/
	
	public static LinkedHashMap<String, String> defaultViewMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultTsrSortByMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultHomepageMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> dashboardSortByMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> nameCaseMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> legalCaseMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> nameFormatMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> ocrFormatMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> startViewDateOptionsMap = new LinkedHashMap<String, String>();
	
	static {
	
	defaultViewMap.put(URLMaping.REPORTS_INTERVAL, "Interval");
	defaultViewMap.put(URLMaping.REPORT_MONTH, "Month");
	defaultViewMap.put(URLMaping.REPORTS_MONTH_DETAILED, "Month Detailed");
	defaultViewMap.put(URLMaping.REPORT_DAY, "Day");
	defaultViewMap.put(URLMaping.REPORT_YEAR, "Year");
	
	defaultTsrSortByMap.put("SORTBY_SRCTYPE", "Data Source");
	defaultTsrSortByMap.put("SORTBY_DATE" , "Date Up");
	defaultTsrSortByMap.put("SORTBY_DATE_DESC" , "Date Down");
	defaultTsrSortByMap.put("SORTBY_GRANTOR" , "Grantor");
	defaultTsrSortByMap.put("SORTBY_GRANTEE" , "Grantee");
	defaultTsrSortByMap.put("SORTBY_INSTTYPE" , "Type");
	defaultTsrSortByMap.put("SORTBY_INST" , "Instrument");
	
	defaultHomepageMap.put(URLMaping.REPORTS_INTERVAL, "Dashboard");
	defaultHomepageMap.put(URLMaping.INVOICE_MONTH, "Invoice");
	defaultHomepageMap.put(URLMaping.REPORTS_TABLE_MONTH, "Table reports");
	defaultHomepageMap.put(URLMaping.StartTSPage , "New Search Page");

	dashboardSortByMap.put("abstractor", "Abstractor");
	dashboardSortByMap.put("agent" , "Agent");
	dashboardSortByMap.put("owner" , "Owner");
	dashboardSortByMap.put("TSR" , "TS Order");
	dashboardSortByMap.put("TSR_DATE" , "TS Done");
	dashboardSortByMap.put("FileID" , "TSR File ID");
	dashboardSortByMap.put("DUE_DATE" , "Note");
	
	nameCaseMap.put("-1","No formatting");
	nameCaseMap.put("1","Uppercase");
	nameCaseMap.put("2","Lowercase");
	nameCaseMap.put("3","Title Case");
	
	legalCaseMap.put("-1","No change");
	legalCaseMap.put("1","Uppercase");
	legalCaseMap.put("2","Lowercase");
	legalCaseMap.put("3","Title Case");
	
	nameFormatMap.put("-1","No formatting");
	nameFormatMap.put("1","Last, First Middle, Sf");
	nameFormatMap.put("2","First Middle Last, Sf");
	
	startViewDateOptionsMap.put("-1", "Default");
	startViewDateOptionsMap.put("0", "CD of Prior File/Search");
	
	}
	
	public static String getDefaultViewCombo(String comboName, String selectedValue) {	
		return getCombo(comboName,selectedValue,defaultViewMap);
	}
	
	public static String getDefaultView(String defaultView) {				
		return defaultViewMap.get(defaultView);
	}
	
	public static String getDefaultTsrSortByCombo(String comboName, String selectedValue) {
		return getCombo(comboName,selectedValue,defaultTsrSortByMap);
	}
	
	public static String getDefaultTsrSortBy(String TsrSortBy) {
		return defaultTsrSortByMap.get(TsrSortBy);
	}	  
	
	public static String getDefaultHomepageCombo(String comboName, String selectedValue) {
		return getCombo(comboName,selectedValue,defaultHomepageMap);
	}
	
	public static String getDefaultHomepage(String defaultHomepage) {
		return defaultHomepageMap.get(defaultHomepage);
	}
	
	public static String getDashboardSortByCombo(String comboName, String selectedValue) {
		return getCombo(comboName,selectedValue,dashboardSortByMap);
	}
	
	public static String getDashboardSortBy(String DashboardSortBy) {			
		return dashboardSortByMap.get(DashboardSortBy);
	}	 
	
	public static String getNameCaseCombo(String comboName, String selectedValue) {
		//if(selectedValue.equals("-1")) selectedValue = "3";	
		return getCombo(comboName,selectedValue,nameCaseMap);
	}
	
	public static String getNameCase(String option) {
		//default value is TitleCase
		//if(option.equals("-1")) option = "3";
		return nameCaseMap.get(option);
	}	
	
	public static String getLegalCaseCombo(String comboName, String selectedValue) {	
		return getCombo(comboName,selectedValue,legalCaseMap);
	}
	
	public static String getStartViewDateOptionsCombo(String comboName, String selectedValue) {	
		return getCombo(comboName, selectedValue, startViewDateOptionsMap);
	}
	
	public static String getLegalCase(String option) {
		return legalCaseMap.get(option);
	}	
	
	public static String getNameFormatCombo(String comboName, String selectedValue) {
		//if(selectedValue.equals("-1")) selectedValue = "1";
		return getCombo(comboName,selectedValue,nameFormatMap);
	}
	
	public static String getNameFormat(String option) {
		//default value is FML
		//if(option.equals("-1")) option = "1";		
		return nameFormatMap.get(option);
	}
		
	public static String getCombo(String comboName, String selectedValue, Map<String,String> values) {
		String combo  = "<SELECT name='" + comboName +"'>"; 
		
		for (Iterator<String> iter = values.keySet().iterator(); iter.hasNext();) {
			String value = (String) iter.next();
			combo +=  "<OPTION value='" + value + "' "+ (value.equals(selectedValue)?" selected ":"")+">" + values.get(value) +"</OPTION>";
		}
		combo += "</SELECT>";
		
		return combo;
	}
}

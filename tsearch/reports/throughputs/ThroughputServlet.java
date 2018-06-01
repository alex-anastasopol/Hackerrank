package ro.cst.tsearch.reports.throughputs;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.procedures.GraphicReportProcedure.INTERVAL_TYPES;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.date.MonthFormat;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

public class ThroughputServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Category logger = Logger.getLogger(ThroughputServlet.class);

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(true);
	
		
		ThroughputBean thBean = null;
		String beanName = "";
		if(request.getParameter("type")!=null && request.getParameter("type").equals(ThroughputOpCode.INCOME_BEAN)){
			thBean = (ThroughputBean)session.getAttribute(ThroughputOpCode.INCOME_BEAN);
			beanName = ThroughputOpCode.INCOME_BEAN;
			//thBean.setLastPage(URLMaping.REPORT_INCOME);
		} else {
			thBean = (ThroughputBean)session.getAttribute(ThroughputOpCode.THROUGHPUT_BEAN);
			beanName = ThroughputOpCode.THROUGHPUT_BEAN;
			//thBean.setLastPage(URLMaping.REPORT_THROUGHPUT);
		}
		UserAttributes userAttributes = ((User)session.getAttribute(SessionParams.CURRENT_USER)).getUserAttributes(); 
		
		// if no bean exists, we create a new one with default settings
		if(thBean==null) {
			thBean = new ThroughputBean();
			
			long commId =-1;
			String commIdStr = (String)session.getAttribute("commId");
			try{
				commId = Long.parseLong(commIdStr);
				CommunityAttributes ca = CommunityUtils.getCommunityFromId(commId);
				if(userAttributes.isCommAdmin() && userAttributes.isTSCAdmin()){
					//we must set groups(categories) and communities
					thBean.setSelectGroups(ca.getCATEGORY().toString());
					thBean.setSelectCommunities(ca.getID().toString());
					thBean.setShowCommunities(false);
					thBean.setShowGroups(false);
					thBean.setShowAbstractors(true);
					thBean.setShowAgents(true);
					
				} else if(userAttributes.isAgent()){
					//we must set groups(categories) and communities
					thBean.setSelectGroups(ca.getCATEGORY().toString());
					thBean.setSelectCommunities(ca.getID().toString());
					thBean.setSelectAbstractors(userAttributes.getID().toString());
					thBean.setSelectAgents(userAttributes.getID().toString());
					thBean.setShowCommunities(false);
					thBean.setShowGroups(false);
					thBean.setShowAbstractors(true);
					thBean.setShowAgents(true);
				} else if(userAttributes.isAbstractor()){
					thBean.setSelectGroups(ca.getCATEGORY().toString());
					thBean.setSelectCommunities(ca.getID().toString());
					thBean.setSelectAbstractors(userAttributes.getID().toString());
					thBean.setShowCommunities(false);
					thBean.setShowGroups(false);
					thBean.setShowAbstractors(true);
					thBean.setShowAgents(true);
				}
			}
			catch(Exception e ){
				e.printStackTrace();
			}
			
			session.setAttribute(beanName,thBean);
		}
		
		int opCode = -1;
		String selectObject = request.getParameter(ThroughputOpCode.SELECT_OBJECT);
		if(selectObject==null) selectObject = ThroughputBean.INVALID;
		String colorObject = request.getParameter(ThroughputOpCode.COLOR_OBJECT);
		if(colorObject==null) colorObject = ThroughputBean.INVALID;
		try {
			opCode = Integer.parseInt(request.getParameter(ThroughputOpCode.OPCODE));
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug(e.getMessage());
		}
		if (opCode == ThroughputOpCode.DRILL_GROUPS) {
			thBean.setShowGroups(false);
			thBean.setShowCommunities(true);
			thBean.getNewSelectGroups().removeAllElements();
			thBean.getOldSelectGroups().add(thBean.getSelectGroups());
			thBean.setSelectGroups(selectObject);
			thBean.setNameGroups(request.getParameter(ThroughputOpCode.NAME_OBJECT));
		} else if (opCode == ThroughputOpCode.FILTER_GROUPS) {
			thBean.getNewSelectGroups().removeAllElements();					//delete the forward list
			thBean.getOldSelectGroups().add(thBean.getSelectGroups());
			thBean.setSelectGroups(selectObject);
			thBean.setColorGroups(colorObject);
			
		} else if (opCode == ThroughputOpCode.DRILL_COMMUNITIES) {
			thBean.setShowCommunities(false);
			thBean.setShowAbstractors(true);
			thBean.setShowAgents(true);
			thBean.getNewSelectCommunities().removeAllElements();
			//thBean.getOldSelectCommunities().add(thBean.getSelectCommunities());
			thBean.setSelectCommunities(selectObject);
			thBean.setNameCommunities(request.getParameter(ThroughputOpCode.NAME_OBJECT));
		
		} else if (opCode == ThroughputOpCode.FILTER_COMMUNITIES) {
			thBean.getNewSelectCommunities().removeAllElements();					//delete the forward list
			thBean.getOldSelectCommunities().add(thBean.getSelectCommunities());
			thBean.setSelectCommunities(selectObject);
			thBean.setColorCommunities(colorObject);
			
		} else if (opCode == ThroughputOpCode.DRILL_STATES) {
			thBean.setShowStates(false);
			thBean.setShowCounties(true);
			thBean.getNewSelectStates().removeAllElements();
			thBean.getOldSelectStates().add(thBean.getSelectStates());
			thBean.setSelectStates(selectObject);
			thBean.setNameStates(request.getParameter(ThroughputOpCode.NAME_OBJECT));
			String[] aux = {ThroughputBean.INVALID};
			thBean.setMultiCounties(aux);
			
		} else if (opCode == ThroughputOpCode.FILTER_PRODUCTS) {
			thBean.getNewSelectProducts().removeAllElements();					//delete the forward list
			thBean.getOldSelectProducts().add(thBean.getSelectProducts());		//add to backward list
			thBean.setSelectProducts(selectObject);
			thBean.setColorProducts(colorObject);
			
		} else if (opCode == ThroughputOpCode.FILTER_STATES) {
			thBean.getNewSelectStates().removeAllElements();					//delete the forward list
			thBean.getOldSelectStates().add(thBean.getSelectStates());
			thBean.setSelectStates(selectObject);
			thBean.setColorStates(colorObject);
			String[] aux = {ThroughputBean.INVALID};
			thBean.setMultiCounties(aux);
			
		} else if (opCode == ThroughputOpCode.FILTER_AGENTS) {
			thBean.getNewSelectAgents().removeAllElements();					//delete the forward list
			thBean.getOldSelectAgents().add(thBean.getSelectAgents());
			thBean.setSelectAgents(selectObject);
			thBean.setColorAgents(colorObject);
			
		} else if (opCode == ThroughputOpCode.FILTER_ABSTRACTORS) {
			thBean.getNewSelectAbstractors().removeAllElements();					//delete the forward list
			thBean.getOldSelectAbstractors().add(thBean.getSelectAbstractors());
			thBean.setSelectAbstractors(selectObject);
			thBean.setColorAbstractors(colorObject);
			
		} else if (opCode == ThroughputOpCode.FILTER_COUNTIES) {
			thBean.getNewSelectCounties().removeAllElements();					//delete the forward list
			thBean.getOldSelectCounties().add(thBean.getSelectCounties());
			thBean.setSelectCounties(selectObject);
			thBean.setColorCounties(colorObject);
			
		} else if (opCode == ThroughputOpCode.RESET_FILTERS) {
			thBean = new ThroughputBean();
			long commId =-1;
			String commIdStr = (String)session.getAttribute("commId");
			try{
				commId = Long.parseLong(commIdStr);
			}
			catch(Exception e ){
				e.printStackTrace();
			}
			
			UserAttributes ua = ((User)session.getAttribute(SessionParams.CURRENT_USER)).getUserAttributes();
			if(!ua.isTSAdmin() && !ua.isTSCAdmin()) {
				//we must set groups(categories) and communities
				CommunityAttributes ca;
				try {
					ca = CommunityUtils.getCommunityFromId(commId);
					thBean.setSelectGroups(ca.getCATEGORY().toString());
					thBean.setSelectCommunities(ca.getID().toString());
					thBean.setShowCommunities(false);
					thBean.setShowGroups(false);
					thBean.setShowAbstractors(true);
					thBean.setShowAgents(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (opCode == ThroughputOpCode.RESET_GROUPS) {
			thBean.getNewSelectGroups().removeAllElements();					//delete the forward list
			if(!thBean.getSelectGroups().equals(ThroughputBean.INVALID))
				thBean.getOldSelectGroups().add(thBean.getSelectGroups());
			thBean.setSelectGroups(ThroughputBean.INVALID);
			
		} else if (opCode == ThroughputOpCode.RESET_COMMUNITIES) {
			thBean.getNewSelectCommunities().removeAllElements();					//delete the forward list
			if(!thBean.getSelectCommunities().equals(ThroughputBean.INVALID))
				thBean.getOldSelectCommunities().add(thBean.getSelectCommunities());
			thBean.setSelectCommunities(ThroughputBean.INVALID);
			
			
		} else if (opCode == ThroughputOpCode.RESET_ABSTRACTORS) {
			thBean.getNewSelectAbstractors().removeAllElements();					//delete the forward list
			if(!thBean.getSelectAbstractors().equals(ThroughputBean.INVALID))
				thBean.getOldSelectAbstractors().add(thBean.getSelectAbstractors());
			thBean.setSelectAbstractors(ThroughputBean.INVALID);
			
		} else if (opCode == ThroughputOpCode.RESET_AGENTS) {
			thBean.getNewSelectAgents().removeAllElements();					//delete the forward list
			thBean.getOldSelectAgents().add(thBean.getSelectAgents());
			thBean.setSelectAgents(ThroughputBean.INVALID);
			
		} else if (opCode == ThroughputOpCode.RESET_COUNTIES) {
			thBean.getNewSelectCounties().removeAllElements();					//delete the forward list
			if(!thBean.getSelectCounties().equals(ThroughputBean.INVALID))
				thBean.getOldSelectCounties().add(thBean.getSelectCounties());
			thBean.setSelectCounties(ThroughputBean.INVALID);
			
		} else if (opCode == ThroughputOpCode.RESET_PRODUCTS) {
			thBean.getNewSelectProducts().removeAllElements();					//delete the forward list
			if(!thBean.getSelectProducts().equals(ThroughputBean.INVALID))
				thBean.getOldSelectProducts().add(thBean.getSelectProducts());
			thBean.setSelectProducts(ThroughputBean.INVALID);
			
		} else if (opCode == ThroughputOpCode.RESET_STATES) {
			thBean.getNewSelectStates().removeAllElements();					//delete the forward list
			if(!thBean.getSelectStates().equals(ThroughputBean.INVALID))
				thBean.getOldSelectStates().add(thBean.getSelectStates());
			thBean.setSelectStates(ThroughputBean.INVALID);
			
		} else if (opCode == ThroughputOpCode.GO_BACK_GROUPS) {
			if(thBean.getOldSelectGroups().size()>0){
				thBean.getNewSelectGroups().add(thBean.getSelectGroups());
				thBean.setSelectGroups(thBean.getOldSelectGroups().lastElement());
				thBean.getOldSelectGroups().removeElementAt(thBean.getOldSelectGroups().size()-1);
			}
			
		} else if (opCode == ThroughputOpCode.GO_BACK_COMMUNITIES) {
			if(thBean.getOldSelectCommunities().size()>0){
				thBean.getNewSelectCommunities().add(thBean.getSelectCommunities());
				thBean.setSelectCommunities(thBean.getOldSelectCommunities().lastElement());
				thBean.getOldSelectCommunities().removeElementAt(thBean.getOldSelectCommunities().size()-1);
			}
			
		} else if (opCode == ThroughputOpCode.GO_BACK_ABSTRACTORS) {
			if(thBean.getOldSelectAbstractors().size()>0){
				thBean.getNewSelectAbstractors().add(thBean.getSelectAbstractors());
				thBean.setSelectAbstractors(thBean.getOldSelectAbstractors().lastElement());
				thBean.getOldSelectAbstractors().removeElementAt(thBean.getOldSelectAbstractors().size()-1);
			}
			
		} else if (opCode == ThroughputOpCode.GO_BACK_AGENTS) {
			if(thBean.getOldSelectAgents().size()>0){
				thBean.getNewSelectAgents().add(thBean.getSelectAgents());
				thBean.setSelectAgents(thBean.getOldSelectAgents().lastElement());
				thBean.getOldSelectAgents().removeElementAt(thBean.getOldSelectAgents().size()-1);
			}
			
		} else if (opCode == ThroughputOpCode.GO_BACK_COUNTIES) {
			if(thBean.getOldSelectCounties().size()>0){
				thBean.getNewSelectCounties().add(thBean.getSelectCounties());
				thBean.setSelectCounties(thBean.getOldSelectCounties().lastElement());
				thBean.getOldSelectCounties().removeElementAt(thBean.getOldSelectCounties().size()-1);
			}	
			
		} else if (opCode == ThroughputOpCode.GO_BACK_PRODUCTS) {
			if(thBean.getOldSelectProducts().size()>0){
				thBean.getNewSelectProducts().add(thBean.getSelectProducts());
				thBean.setSelectProducts(thBean.getOldSelectProducts().lastElement());
				thBean.getOldSelectProducts().removeElementAt(thBean.getOldSelectProducts().size()-1);
			}	
			
		} else if (opCode == ThroughputOpCode.GO_BACK_STATES) {
			if(thBean.getOldSelectStates().size()>0){
				thBean.getNewSelectStates().add(thBean.getSelectStates());
				thBean.setSelectStates(thBean.getOldSelectStates().lastElement());
				thBean.getOldSelectStates().removeElementAt(thBean.getOldSelectStates().size()-1);
			}	
		} else if (opCode == ThroughputOpCode.GO_FW_ABSTRACTORS) {
			if(thBean.getNewSelectAbstractors().size()>0){
				thBean.getOldSelectAbstractors().add(thBean.getSelectAbstractors());
				thBean.setSelectAbstractors(thBean.getNewSelectAbstractors().lastElement());
				thBean.getNewSelectAbstractors().removeElementAt(thBean.getNewSelectAbstractors().size()-1);
			}
			
		} else if (opCode == ThroughputOpCode.GO_FW_AGENTS) {
			if(thBean.getNewSelectAgents().size()>0){
				thBean.getOldSelectAgents().add(thBean.getSelectAgents());
				thBean.setSelectAgents(thBean.getNewSelectAgents().lastElement());
				thBean.getNewSelectAgents().removeElementAt(thBean.getNewSelectAgents().size()-1);
			}
			
		} else if (opCode == ThroughputOpCode.GO_FW_COUNTIES) {
			if(thBean.getNewSelectCounties().size()>0){
				thBean.getOldSelectCounties().add(thBean.getSelectCounties());
				thBean.setSelectCounties(thBean.getNewSelectCounties().lastElement());
				thBean.getNewSelectCounties().removeElementAt(thBean.getNewSelectCounties().size()-1);
			}	
			
		} else if (opCode == ThroughputOpCode.GO_FW_PRODUCTS) {
			if(thBean.getNewSelectProducts().size()>0){
				thBean.getOldSelectProducts().add(thBean.getSelectProducts());
				thBean.setSelectProducts(thBean.getNewSelectProducts().lastElement());
				thBean.getNewSelectProducts().removeElementAt(thBean.getNewSelectProducts().size()-1);
			}	
			
		} else if (opCode == ThroughputOpCode.GO_FW_COMMUNITIES) {
			if(thBean.getNewSelectCommunities().size()>0){
				thBean.getOldSelectCommunities().add(thBean.getSelectCommunities());
				thBean.setSelectCommunities(thBean.getNewSelectCommunities().lastElement());
				thBean.getNewSelectCommunities().removeElementAt(thBean.getNewSelectCommunities().size()-1);
			}
		} else if (opCode == ThroughputOpCode.GO_FW_GROUPS) {
			if(thBean.getNewSelectGroups().size()>0){
				thBean.getOldSelectGroups().add(thBean.getSelectGroups());
				thBean.setSelectGroups(thBean.getNewSelectGroups().lastElement());
				thBean.getNewSelectGroups().removeElementAt(thBean.getNewSelectGroups().size()-1);
			}
		} else if (opCode == ThroughputOpCode.GO_FW_STATES) {
			if(thBean.getNewSelectStates().size()>0){
				thBean.getOldSelectStates().add(thBean.getSelectStates());
				thBean.setSelectStates(thBean.getNewSelectStates().lastElement());
				thBean.getNewSelectStates().removeElementAt(thBean.getNewSelectStates().size()-1);
			}
			
		} else if (opCode == ThroughputOpCode.BACK_TO_COMMUNITIES) {
			thBean.setShowCommunities(true);
			thBean.getNewSelectCommunities().removeAllElements();
			thBean.getOldSelectCommunities().removeAllElements();
			
			thBean.setSelectCommunities(ThroughputBean.INVALID);
			//thBean.getNewSelectCommunities().add(thBean.getSelectCommunities());
			//if(thBean.getOldSelectCommunities().size()>0)
			//	thBean.getOldSelectCommunities().remove(thBean.getOldSelectCommunities().size()-1);
			
			
			thBean.setShowCounties(false);
			thBean.getOldSelectCounties().removeAllElements();
			thBean.getNewSelectCounties().removeAllElements();
			thBean.setSelectCounties(ThroughputBean.INVALID);
			thBean.setShowStates(true);
			
			
			
						
			thBean.setShowAbstractors(false);
			thBean.getOldSelectAbstractors().removeAllElements();
			thBean.getNewSelectAbstractors().removeAllElements();
			thBean.setSelectAbstractors(ThroughputBean.INVALID);
			
			thBean.setShowAgents(false);
			thBean.getOldSelectAgents().removeAllElements();
			thBean.getNewSelectAgents().removeAllElements();
			thBean.setSelectAgents(ThroughputBean.INVALID);
						
		} else if (opCode == ThroughputOpCode.BACK_TO_GROUPS) {
			thBean.setShowGroups(true);
			thBean.setSelectGroups(ThroughputBean.INVALID);
			
			if(!thBean.getShowStates()){
				thBean.setShowStates(true);
				thBean.getOldSelectStates().removeAllElements();
				thBean.getNewSelectStates().removeAllElements();
				thBean.setNameStates("");
				thBean.setSelectStates(ThroughputBean.INVALID);
			}
			
			thBean.setShowCommunities(false);
			thBean.getOldSelectCommunities().removeAllElements();
			thBean.getNewSelectCommunities().removeAllElements();
			thBean.setNameCommunities("");
			thBean.setSelectCommunities(ThroughputBean.INVALID);
			
			thBean.setShowAbstractors(false);
			thBean.getOldSelectAbstractors().removeAllElements();
			thBean.getNewSelectAbstractors().removeAllElements();
			thBean.setSelectAbstractors(ThroughputBean.INVALID);
			
			thBean.setShowAgents(false);
			thBean.getOldSelectAgents().removeAllElements();
			thBean.getNewSelectAgents().removeAllElements();
			thBean.setSelectAgents(ThroughputBean.INVALID);
			
			thBean.setShowCounties(false);
			thBean.getOldSelectCounties().removeAllElements();
			thBean.getNewSelectCounties().removeAllElements();
			thBean.setSelectCounties(ThroughputBean.INVALID);
			
			
		} else if (opCode == ThroughputOpCode.BACK_TO_STATES) {
			thBean.setShowStates(true);
			thBean.setShowCounties(false);
			thBean.setSelectStates(ThroughputBean.INVALID);
			thBean.setSelectCounties(ThroughputBean.INVALID);
			
		} else if (opCode == ThroughputOpCode.APPLY_REFRESH){
			String aux [];
			aux = request.getParameterValues(RequestParams.REPORTS_COMMUNITY);
			if(!StringUtils.equals(aux,thBean.getMultiCommunities())){
				thBean.setSelectCommunities(ThroughputBean.INVALID);
				thBean.setMultiCommunities(aux);
			}
			aux = request.getParameterValues(RequestParams.REPORTS_STATE);
			if(!StringUtils.equals(aux,thBean.getMultiStates())){				//daca am modificat ceva la selectia multipla resetez statele si county
				thBean.setSelectStates(ThroughputBean.INVALID);
				thBean.setShowCounties(false);
				thBean.setShowStates(true);
				thBean.setSelectCounties(ThroughputBean.INVALID);
				thBean.setMultiStates(aux);							//setez noua selectie
			}
			aux = request.getParameterValues(RequestParams.REPORTS_COUNTY);
			if(!StringUtils.equals(aux,thBean.getMultiCounties())){				//daca am modificat ceva la selectia multipla resetez statele si county
				thBean.setSelectCounties(ThroughputBean.INVALID);
				thBean.setMultiCounties(aux);							//setez noua selectie
			}
			aux = request.getParameterValues(RequestParams.SEARCH_PRODUCT_TYPE);
			if(!StringUtils.equals(aux,thBean.getMultiProducts())){
				thBean.setSelectProducts(ThroughputBean.INVALID);
				thBean.setMultiProducts(aux);							
			}
			aux = request.getParameterValues(RequestParams.REPORTS_ABSTRACTOR);
			if(!StringUtils.equals(aux,thBean.getMultiAbstractors())){				//daca am modificat ceva la selectia multipla resetez statele si county
				thBean.setSelectAbstractors(ThroughputBean.INVALID);
				thBean.setMultiAbstractors(aux);							//setez noua selectie
			}
			aux = request.getParameterValues(RequestParams.REPORTS_AGENT);
			if(!StringUtils.equals(aux,thBean.getMultiAgents())){				//daca am modificat ceva la selectia multipla resetez statele si county
				thBean.setSelectAgents(ThroughputBean.INVALID);
				thBean.setMultiAgents(aux);							//setez noua selectie
			}
			aux = request.getParameterValues(RequestParams.REPORTS_COMPANY_AGENT);
			if(!StringUtils.equals(aux,thBean.getMultiCompaniesAgents())){
				thBean.setMultiCompaniesAgents(aux);
			}
			aux = request.getParameterValues(RequestParams.REPORTS_STATUS);
			if(!StringUtils.equals(aux,thBean.getMultiStatus())){
				thBean.setMultiStatus(aux);
			}
			
		} else if (opCode == ThroughputOpCode.EXPORT_STATES) {
			
			GenericState[] states = DBManager.getAllStates();
			
			HashMap<Long, String> stateData = new HashMap<Long, String>();
			for (int i = 0; i < states.length; i++) {
				stateData.put(states[i].getId(), states[i].getStateAbv());
			}
			
			generateExportReport(
					request, 
					response, 
					thBean, 
					stateData,
					"State", 
					thBean.getStateInfoMap());
			return;
			
		} else if (opCode == ThroughputOpCode.EXPORT_COUNTIES) {
			
			Map<Long,String> countiesData = new HashMap<Long, String>();
			Collection<CountyWithState> counties = DBManager.getAllCountiesForState(Integer.parseInt(thBean.getSelectStates()));
			for(CountyWithState county: counties){
				countiesData.put((long)county.getCountyId(), county.getCountyName());
			}	
			
			generateExportReport(
					request, 
					response, 
					thBean, 
					countiesData,
					"County", 
					thBean.getCountyInfoMap());
			return;
		} else if (opCode == ThroughputOpCode.EXPORT_ABSTRACTORS) {
			Map<Long,String> nameMap = new HashMap<Long, String>();
			UserManagerI userManager = UserManager.getInstance();
			try {
				userManager.getAccess();
				for (Entry<Long, Long> element : thBean.getAbstractorInfoMap()) {
					UserI user = userManager.getUser(element.getKey());
					if(user != null) {
						nameMap.put(element.getKey(), user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());
					}
				}
				
			} catch (Exception e) {
				logger.error("Error while loading abstractor map", e);
			} finally {
				userManager.releaseAccess();
			}
			generateExportReport(
					request, 
					response, 
					thBean, 
					nameMap,
					"Abstractor", 
					thBean.getAbstractorInfoMap());
			return;
		} else if (opCode == ThroughputOpCode.EXPORT_AGENTS) {
			Map<Long,String> nameMap = new HashMap<Long, String>();
			UserManagerI userManager = UserManager.getInstance();
			try {
				userManager.getAccess();
				for (Entry<Long, Long> element : thBean.getAgentInfoMap()) {
					UserI user = userManager.getUser(element.getKey());
					if(user != null) {
						nameMap.put(element.getKey(), user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());
					}
				}
				
			} catch (Exception e) {
				logger.error("Error while loading agent map", e);
			} finally {
				userManager.releaseAccess();
			}
			generateExportReport(
					request, 
					response, 
					thBean, 
					nameMap,
					"Agent", 
					thBean.getAgentInfoMap());
			return;
		} else if (opCode == ThroughputOpCode.EXPORT_PRODUCTS) {
			
			int[] communities = new int[1];
			communities[0] = Integer.parseInt(thBean.getSelectCommunities());
			String[] commStr = thBean.getMultiCommunities(); 
			if (commStr!=null) {
				communities = new int[commStr.length];
				for (int i=0;i<commStr.length;i++)
					communities[i] = Integer.parseInt(commStr[i]);
			}
			
			generateExportReport(
					request, 
					response, 
					thBean, 
					Products.getAllProductShortNameForCommunity(communities),
					"Product", 
					thBean.getProductInfoMap());
			return; 
		} else {
			logger.debug("Cod operatie inexistent pt modulul Throughputs " + opCode);
		}
		//if we have allowed counties and all counties are selected, force the list to allowed counties
		thBean.setAllowedCounties(userAttributes);
		session.setAttribute(beanName,thBean);
		
		
		try {
			
			request.getRequestDispatcher( thBean.getLastPage()).forward((ServletRequest) request, (ServletResponse) response);
		
		} catch (SocketException se) {
			se.printStackTrace();
		} catch (IllegalStateException ise) {
			ise.printStackTrace();
		}
		

	}

	public void generateExportReport(HttpServletRequest request,
			HttpServletResponse response, ThroughputBean thBean,
			Map<Long, String> stateData, String titleName,
			List<Entry<Long, Long>> infoMap) {
		String pathTemplate = getServletConfig().getServletContext().getRealPath("/") 
				+ File.separator + "WEB-INF"
				+ File.separator + "classes" + File.separator + "ro"
				+ File.separator + "cst" + File.separator + "tsearch"
				+ File.separator + "reports" + File.separator + "templates"
				+ File.separator + "pieReportXLS.jrxml";
		JasperReport jRep = null;
		JasperPrint jPrint = null;
		Map<String, Object> mapParameter = new HashMap<String, Object>();
		mapParameter.put("reportTitle", thBean.getName());
		mapParameter.put("reportDate", "Report Date " + Calendar.getInstance().getTime());
		mapParameter.put("reportParameters", thBean.getSelectionsFullStatus());
		
		
		String intervalType = request.getParameter("intervalType");
		
		if(intervalType == null || INTERVAL_TYPES.GENERAL.toString().equals(intervalType)) {
			mapParameter.put("intervalDescription", "All Years");
		} else if (INTERVAL_TYPES.YEAR.toString().equals(intervalType)) {
			mapParameter.put("intervalDescription", "Year: " + request.getParameter("yearReport"));
		} else if (INTERVAL_TYPES.MONTH.toString().equals(intervalType)) {
			String monthName = "unknown";
			try {
				monthName = org.apache.commons.lang.StringUtils.capitalize(MonthFormat.values()[Integer.parseInt(request.getParameter("monthReport"))].name().toLowerCase());
			} catch (Exception e) {
				logger.error("Error getting month name", e);
			}
			mapParameter.put("intervalDescription", "Year: " + request.getParameter("yearReport") + ", Month: " + monthName);
		}
		
		mapParameter.put("titleName", titleName);
		mapParameter.put("titleValue", "Number Of Orders");

		try {
			response.setContentType("application/vnd.ms-excel");
			response.setHeader(
					"Content-Disposition",
					" attachment; filename=\""
						+ "ATS " + thBean.getName() + "_" + titleName + ".xls"
						+ "\"");
			
			jRep = JasperCompileManager.compileReport(pathTemplate);
			Collection<PieReportXLSBean> toLoad = loadExportData(
					infoMap,
					stateData
					);
			long total = 0;
			if(toLoad.size() == 0) {
				toLoad.add(new PieReportXLSBean("",""));
			} else {
				for (PieReportXLSBean pieReportXLSBean : toLoad) {
					try {
						total += Long.parseLong(pieReportXLSBean.getValue());
					} catch (Exception e) {
						logger.error("Error while trying to parse value for " + pieReportXLSBean.getName() + ": " + pieReportXLSBean.getValue(), e);
					}
				}
			}
			mapParameter.put("total", Long.toString(total));
			jPrint = JasperFillManager.fillReport(jRep, mapParameter,
					new JRBeanCollectionDataSource(toLoad));
			JExcelApiExporter excelExporter = new JExcelApiExporter();
			excelExporter.setParameter(JRExporterParameter.JASPER_PRINT, jPrint);
			excelExporter.setParameter(
					JRExporterParameter.OUTPUT_STREAM, response.getOutputStream());
			excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
			excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
			excelExporter.exportReport();

		} catch (Exception e) {
			e.printStackTrace();				
		}
	}

	private Collection<PieReportXLSBean> loadExportData(
			List<Entry<Long, Long>> infoMap, Map<Long, String> nameData) {
		Collection<PieReportXLSBean> result = new Vector<PieReportXLSBean>();
		
		if(infoMap != null) {
			for (Entry<Long,Long> entry : infoMap) {
				if(nameData.get(entry.getKey())==null)
					continue;
				result.add(new PieReportXLSBean(nameData.get(entry.getKey()), 
						entry.getValue().toString()));
			}
		}
		
		return result;
	}

}

package ro.cst.tsearch.reports.throughputs;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.urls.PieURLGenerator;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.community.CategoryAttributes;
import ro.cst.tsearch.community.CategoryUtils;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityFilter;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;

public class PieReportBean {
	
	private HashMap<String, String> hashMap = null;
	private HashMap<String, String> colorHashMap = null;
	private String fromPage = "";
	private String monthReport = "";
	private String yearReport =	"";
	private String dayReport = "";
	private String beanName = "";
	private Integer[] testData = null; 
	
	private int[] reportState = {-1};
	private int[] reportCounty = {-1};
	private int[] reportAgent = {-1};
	private int[] reportAbstractor = {-1};
	private String[] reportCompanyAgent = {"-1"}; 
	
	public PieReportBean() {
		testData = new Integer[35];
		for (int i = 0; i < 35; i++) {
			testData[i] = new Integer(i+1);
		}
		
	}

	
	private DefaultPieDataset getDatasetGroups(HttpSession session) throws Exception{
		
		//ThroughputBean thBean = (ThroughputBean)session.getAttribute(ThroughputOpCode.THROUGHPUT_BEAN);
		DefaultPieDataset dataset = new DefaultPieDataset();
		
		ThroughputBean thBean = null;
		//if(getFromPage().contains("throughput"))
		//	thBean = (ThroughputBean)session.getAttribute(ThroughputOpCode.THROUGHPUT_BEAN);
		//else
		thBean = (ThroughputBean)session.getAttribute(beanName);
		
		CategoryAttributes[] sgas;
		try {
			sgas = CategoryUtils.getCategories(CategoryAttributes.CATEGORY_NAME);
		} catch (BaseException e) {
			e.printStackTrace();
			return null;
		}
		hashMap = new HashMap<String, String>();
		
		
		HashMap<Long, Long> hmData = null;
		int productId = Integer.parseInt(thBean.getSelectProducts());
		
		
		
		loadAttribute(thBean, RequestParams.REPORTS_STATE);
		loadAttribute(thBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(thBean, RequestParams.REPORTS_AGENT);
		loadAttribute(thBean, RequestParams.REPORTS_COUNTY);
		loadAttribute(thBean, RequestParams.REPORTS_COMPANY_AGENT);
				
		if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT) || getFromPage().contains(URLMaping.REPORT_INCOME))
			hmData = PieDatabase.getGroupDataGeneral(beanName, reportCounty, reportAbstractor, 
					reportAgent, reportState, reportCompanyAgent, productId);
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_Y) || getFromPage().contains(URLMaping.REPORT_INCOME_Y)){
			hmData = PieDatabase.getGroupDataAnnual(beanName, reportCounty, reportAbstractor, 
					reportAgent, reportState, reportCompanyAgent, productId, getYearReport());
		} else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_M) || getFromPage().contains(URLMaping.REPORT_INCOME_M)){
			hmData = PieDatabase.getGroupDataMonthly(beanName, reportCounty, reportAbstractor, 
					reportAgent, reportState, reportCompanyAgent, productId, getYearReport(),getMonthReport());
		}
		
		List<Map.Entry<Long, Long>> hmDataList = getSortedMap(hmData, false);
		HashMap<Long, String> groupData = new HashMap<Long, String>();
		
		for (int i = 0; i < sgas.length; i++) {
			groupData.put(new Long(sgas[i].getID().longValue()), sgas[i].getNAME());
		}
		
		int sel = -1;
		String selectGroups =  thBean.getSelectGroups();
		if (selectGroups!=null)
			sel = Integer.parseInt(selectGroups);
		
		if( sel >=0 ) {
			dataset.setValue(groupData.get(new Long(sel)), hmData.get(new Long(sel)));
			hashMap.put(groupData.get(new Long(sel)), new Integer(sel).toString());
		} else 
			dataset = getDatasetFrom(sel,hmDataList,groupData);
		
		return dataset;
	}
	
	private DefaultPieDataset getDatasetCommunities(HttpSession session) throws Exception{
		
		DefaultPieDataset dataset = new DefaultPieDataset();
		ThroughputBean thBean = null;
		thBean = (ThroughputBean)session.getAttribute(beanName);
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		CommunityAttributes[] comm;
		
		try {
			comm = CommunityUtils.getCommunitiesInCategory(new BigDecimal(groupId), new CommunityFilter());
		} catch (BaseException e) {
			e.printStackTrace();
			return null;
		}
		hashMap = new HashMap<String, String>();
		
		HashMap<Long, Long> hmData = null;
		int productId = Integer.parseInt(thBean.getSelectProducts());
		
		loadAttribute(thBean, RequestParams.REPORTS_STATE);
		loadAttribute(thBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(thBean, RequestParams.REPORTS_AGENT);
		loadAttribute(thBean, RequestParams.REPORTS_COUNTY);
		loadAttribute(thBean, RequestParams.REPORTS_COMPANY_AGENT);
		
		if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT) || getFromPage().contains(URLMaping.REPORT_INCOME))
			hmData = PieDatabase.getCommunitiesDataGeneral(beanName, reportCounty, reportAbstractor, reportAgent, reportState, 
					reportCompanyAgent, productId, groupId);
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_Y) || getFromPage().contains(URLMaping.REPORT_INCOME_Y))
			hmData = PieDatabase.getCommunitiesDataAnnual(beanName, reportCounty, reportAbstractor, reportAgent, reportState, 
					reportCompanyAgent, productId, groupId, getYearReport());
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_M) || getFromPage().contains(URLMaping.REPORT_INCOME_M))
			hmData = PieDatabase.getCommunitiesDataMonthly(beanName, reportCounty, reportAbstractor, reportAgent, reportState, 
					reportCompanyAgent, productId, groupId, getYearReport(), getMonthReport());
		
		List<Map.Entry<Long, Long>> hmDataList = getSortedMap(hmData, false);
		HashMap<Long, String> commData = new HashMap<Long, String>();
		
		for (int i = 0; i < comm.length; i++) {
			commData.put(new Long(comm[i].getID().longValue()), comm[i].getNAME());
		}
		for (int i = 0; i < hmDataList.size(); i++) {
			Map.Entry<Long, Long> element = hmDataList.get(i);
			dataset.setValue(commData.get(element.getKey()), element.getValue());
			hashMap.put(commData.get(element.getKey()), element.getKey().toString());
			
		}
		String selectCommunities =  thBean.getSelectCommunities();
		int sel = -1;
		if (selectCommunities!=null)
			sel = Integer.parseInt(selectCommunities);
		
		if( sel >=0 ) {
			dataset.setValue(commData.get(new Long(sel)), hmData.get(new Long(sel)));
			hashMap.put(commData.get(new Long(sel)), new Integer(sel).toString());
		} else 
			dataset = getDatasetFrom(sel,hmDataList,commData);
		
		return dataset;
	}

	
	private DefaultPieDataset getDatasetStates(HttpSession session) throws Exception{
		
		DefaultPieDataset dataset = new DefaultPieDataset();
		ThroughputBean thBean = null;
		int payrateType = 0;
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		boolean isTSAdmin = UserUtils.isTSAdmin(currentUser.getUserAttributes());
		if (isTSAdmin)
			payrateType = 1;
		thBean = (ThroughputBean)session.getAttribute(beanName);
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		if(groupId<0) groupId = -1;				//if we've clicked "Other" this would decrease below -1 
		int commId = Integer.parseInt(thBean.getSelectCommunities());
		if(commId<0) commId = -1;				//if we've clicked "Other" this would decrease below -1 
		
		String selectStates = thBean.getSelectStates();
		hashMap = new HashMap<String, String>();
		
		int productId = Integer.parseInt(thBean.getSelectProducts());
		if(productId < -1) productId = -1; 
		
		loadAttribute(thBean, RequestParams.REPORTS_STATE);
		thBean.setWarningMessage("");
		
		String[] strArray = thBean.getMultiCounties();
		if(strArray!=null && strArray.length>0){
			reportCounty = new int[strArray.length];
			for(int i=0; i<strArray.length; i++)
				reportCounty[i] = Integer.parseInt(strArray[i]);
		} else {
			reportCounty = new int[1];
			reportCounty[0] = -1;
		}
		loadAttribute(thBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(thBean, RequestParams.REPORTS_AGENT);
		loadAttribute(thBean, RequestParams.REPORTS_COMPANY_AGENT);
		
		GenericState[] states = DBManager.getAllStates();
				
		HashMap<Long, String> stateData = new HashMap<Long, String>();
		for (int i = 0; i < states.length; i++) {
			stateData.put(states[i].getId(), states[i].getName());
		}
		
		HashMap<Long, Long> hmData = null; 
		if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT) || getFromPage().contains(URLMaping.REPORT_INCOME))
			hmData = PieDatabase.getStatesDataGeneral(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, productId, groupId, commId, payrateType);
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_Y) || getFromPage().contains(URLMaping.REPORT_INCOME_Y))
			hmData = PieDatabase.getStatesDataAnnual(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, productId, groupId, commId, payrateType, getYearReport());
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_M) || getFromPage().contains(URLMaping.REPORT_INCOME_M))
			hmData = PieDatabase.getStatesDataMonthly(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, productId, groupId, commId, payrateType, getYearReport(), getMonthReport());
		
		hmData.remove(new Long(0)); 	//for the cases where we have State_id = 0, and we shouldn't
		List<Map.Entry<Long, Long>> hmDataList = getSortedMap(hmData, false);
		
		int sel = -1;
		if (selectStates!=null)
			sel = Integer.parseInt(selectStates);
		
		if( sel >=0 ) {
			dataset.setValue(stateData.get(new Long(sel)), hmData.get(new Long(sel)));
			hashMap.put(stateData.get(new Long(sel)), new Integer(sel).toString());
		} else 
			dataset = getDatasetFrom(sel,hmDataList,stateData);
		
		
		return dataset;
	}

	private DefaultPieDataset getDatasetCounties(HttpSession session) throws Exception{
				
		DefaultPieDataset dataset = new DefaultPieDataset();
		ThroughputBean thBean = null;
	
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		boolean isTSAdmin = UserUtils.isTSAdmin(currentUser.getUserAttributes());
		
		thBean = (ThroughputBean)session.getAttribute(beanName);
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		int commId = Integer.parseInt(thBean.getSelectCommunities());
		int productId = Integer.parseInt(thBean.getSelectProducts());
		reportState = new int[1];
		reportState[0] = Integer.parseInt(thBean.getSelectStates());				//just one state will be selected
		Collection<CountyWithState> counties = DBManager.getAllCountiesForState(reportState[0]);
		boolean isOk = true;
		String warningMessage = "";
		String[] strArray = thBean.getMultiCounties();			//get Multi filter select
		if(strArray!=null && strArray.length>0){				//if we have something selected
			reportCounty = new int[strArray.length];
			for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
				reportCounty[i] = Integer.parseInt(strArray[i]);
				if(reportCounty[i]==-1)
					isOk = true;
				else{		
					for(CountyWithState county: counties){
						if(reportCounty[i] == county.getCountyId()){
							isOk = true;
							break;
						}					
					}
				}
				if(!isOk && warningMessage.length()==0){
					warningMessage = "The state-county selections are not correlated.<br>" +
							"Please use the filters above to select counties belonging to the already selected state.";
				}
			}
			
		} else 
			reportCounty[0] = Integer.parseInt(thBean.INVALID);
		//if(warningMessage.length()>0)
		thBean.setWarningMessage(warningMessage);
		int sel = Integer.parseInt(thBean.getSelectAbstractors());
		loadAttribute(thBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(thBean, RequestParams.REPORTS_AGENT);
		loadAttribute(thBean, RequestParams.REPORTS_COMPANY_AGENT);
		
		String selectCounties = thBean.getSelectCounties();
		hashMap = new HashMap<String, String>();
		
		HashMap<Long, Long> hmData = null; 
		if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT) || getFromPage().contains(URLMaping.REPORT_INCOME))
			hmData = PieDatabase.getCountiesDataGeneral(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, productId, groupId, commId, (isTSAdmin)?1:0);
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_Y) || getFromPage().contains(URLMaping.REPORT_INCOME_Y))
			hmData = PieDatabase.getCountiesDataAnnual(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, productId, groupId, commId, (isTSAdmin)?1:0, getYearReport());
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_M) || getFromPage().contains(URLMaping.REPORT_INCOME_M))
			hmData = PieDatabase.getCountiesDataMonthly(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, productId, groupId, commId, (isTSAdmin)?1:0, getYearReport(), getMonthReport());
		
		hmData.remove(new Long(0)); 	//for the cases where we have State_id = 0, and we shouldn't
		List<Map.Entry<Long, Long>> hmDataList = getSortedMap(hmData, false);

		//setting the hashmap containing county id-name pairs for current selected state
		//GenericCounty[] counties = DBManager.getAllCounties();
		HashMap<Long,String> countiesData = new HashMap<Long, String>();
		for(CountyWithState county: counties){
			countiesData.put((long)county.getCountyId(), county.getCountyName());
		}		
		
		if(selectCounties!=null) 
			sel = Integer.parseInt(selectCounties);
		else
			sel = -1;
		
		if(sel >= 0){
			dataset.setValue(countiesData.get(new Long(sel)), hmData.get(new Long(sel)));
			hashMap = null;
		} else {
			dataset = getDatasetFrom(sel,hmDataList,countiesData);
		}
		return dataset;
	}
	
	
	private DefaultPieDataset getDatasetProducts(HttpSession session) throws Exception{	
		DefaultPieDataset dataset = new DefaultPieDataset();
		ThroughputBean thBean = null;
		thBean = (ThroughputBean)session.getAttribute(beanName);
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		int payrateType = 0;
		if (UserUtils.isTSAdmin(currentUser.getUserAttributes()))
			payrateType = 1;
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		if(groupId<0) groupId = -1;				//if we've clicked "Other" this would decrease below -1
		int commId = Integer.parseInt(thBean.getSelectCommunities());
		if(commId<0) commId = -1;				//if we've clicked "Other" this would decrease below -1
		
		long currentUserCommId = currentUser.getUserAttributes().getCOMMID().longValue();
		
		int sel;
		
		loadAttribute(thBean, RequestParams.REPORTS_STATE);
		loadAttribute(thBean, RequestParams.REPORTS_COUNTY);
		loadAttribute(thBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(thBean, RequestParams.REPORTS_AGENT);
		loadAttribute(thBean, RequestParams.REPORTS_COMPANY_AGENT);
		
				
		String selectProducts = thBean.getSelectProducts();
		if(selectProducts!=null) 
			sel = Integer.parseInt(selectProducts);
		else
			sel = -1;
		hashMap = new HashMap<String, String>();
		HashMap<Long, Long> hmData = null;
		if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT) || getFromPage().contains(URLMaping.REPORT_INCOME))
			hmData = PieDatabase.getProductsDataGeneral(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, payrateType, groupId, commId, sel);
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_Y) || getFromPage().contains(URLMaping.REPORT_INCOME_Y))
			hmData = PieDatabase.getProductsDataAnnual(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, payrateType, groupId, commId, getYearReport(),sel);
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_M) || getFromPage().contains(URLMaping.REPORT_INCOME_M))
			hmData = PieDatabase.getProductsDataMonthly(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, payrateType, groupId, commId, getYearReport(), getMonthReport(), sel);
		
		//setting the hashmap containing product id-name pairs 		
		
		Products communityProducts = CommunityProducts.getProduct(currentUserCommId);
		
		HashMap<Long,String> productData = new HashMap<Long, String>();
		productData.put(new Long(1), communityProducts.getProductName(Products.FULL_SEARCH_PRODUCT));
		productData.put(new Long(2), communityProducts.getProductName(Products.CURRENT_OWNER_PRODUCT));
		productData.put(new Long(3), communityProducts.getProductName(Products.CONSTRUCTION_PRODUCT));
		productData.put(new Long(4), communityProducts.getProductName(Products.COMMERCIAL_PRODUCT));
		productData.put(new Long(5), communityProducts.getProductName(Products.REFINANCE_PRODUCT));
		productData.put(new Long(6), communityProducts.getProductName(Products.OE_PRODUCT));
		productData.put(new Long(7), communityProducts.getProductName(Products.LIENS_PRODUCT));
		productData.put(new Long(8), communityProducts.getProductName(Products.ACREAGE_PRODUCT));
		productData.put(new Long(9), communityProducts.getProductName(Products.SUBLOT_PRODUCT));
		productData.put(new Long(10), communityProducts.getProductName(Products.UPDATE_PRODUCT));
		productData.put(new Long(12), communityProducts.getProductName(Products.FVS_PRODUCT));
		
		
		List<Map.Entry<Long, Long>> hmDataList = getSortedMap(hmData, false);
		
		
		
		if(sel>=0){
			dataset.setValue(productData.get(new Long(sel)), hmData.get(new Long(sel)));
			hashMap = null;
		} else {
			dataset = getDatasetFrom(sel,hmDataList,productData);
		}
		return dataset;
	}
	
	private DefaultPieDataset getDatasetAbstractors(HttpSession session) throws Exception{
		//System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		DefaultPieDataset dataset = new DefaultPieDataset();
		ThroughputBean thBean = null;
		thBean = (ThroughputBean)session.getAttribute(beanName);
		UserAttributes ua = ((User) session.getAttribute(SessionParams.CURRENT_USER)).getUserAttributes();
		
		int payrateType = 0;
		if (UserUtils.isTSAdmin(ua))
			payrateType = 1;
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		int commId = Integer.parseInt(thBean.getSelectCommunities());		
		int productId = Integer.parseInt(thBean.getSelectProducts());
		
		loadAttribute(thBean, RequestParams.REPORTS_STATE);
		loadAttribute(thBean, RequestParams.REPORTS_COUNTY);
		loadAttribute(thBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(thBean, RequestParams.REPORTS_AGENT);
		loadAttribute(thBean, RequestParams.REPORTS_COMPANY_AGENT);
		
		
		hashMap = new HashMap<String, String>();
		
		HashMap<Long, Long> hmData = null;
		if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT) || getFromPage().contains(URLMaping.REPORT_INCOME))
			hmData = PieDatabase.getAbstractorsDataGeneral(beanName, reportState, reportCounty, 
					reportAbstractor, reportAgent, reportCompanyAgent, payrateType, productId, groupId, commId);		
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_Y) || getFromPage().contains(URLMaping.REPORT_INCOME_Y))
			hmData = PieDatabase.getAbstractorsDataAnnual(beanName, reportState, reportCounty, 
					reportAbstractor, reportAgent, reportCompanyAgent, payrateType, productId, groupId, commId, getYearReport());
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_M) || getFromPage().contains(URLMaping.REPORT_INCOME_M))
			hmData = PieDatabase.getAbstractorsDataMonthly(beanName, reportState, reportCounty, 
					reportAbstractor, reportAgent, reportCompanyAgent, payrateType, productId, groupId, commId, getYearReport(), getMonthReport());
			

		//setting the hashmap containing abstractor id-name pairs 
		//UserAttributes[] abstractors = DBManager.getAllAbstractorsForSelect();
		//AgentAttributes[] agents = DBManager.getAllAgentsForSelect();
		UserAttributes[] users = DBManager.getAllUsersFromCategory(groupId);
		HashMap<Long,String> abstrData = new HashMap<Long, String>();
		for (int i = 0; i < users.length; i++) {
			
			abstrData.put(users[i].getID().longValue(), users[i].getFIRSTNAME() + " " + users[i].getLASTNAME() + " - " + users[i].getLOGIN());
		}
//		this will remove entries in hmData that have no correspondat in the agantsData
		//usefull if a user doesn't exist any more
		validateData(hmData,abstrData);
		/*
		for (int i = 0; i < agents.length; i++) {
			abstrData.put(agents[i].getID().longValue(), agents[i].getFIRST_NAME() + " " + agents[i].getLAST_NAME());
		}
		if(UserUtils.isAgent(ua)){
			abstrData.put(ua.getID().longValue(), ua.getFIRSTNAME() + " " + ua.getLASTNAME() + " - " + ua.getLOGIN());
		}
		*/
		/*
		//defensive against cases where an Abstractor is also an Agent (user_id = 133)
		Vector<Long> keys = new Vector<Long>(hmData.keySet());
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Long element = (Long) iter.next();
			if(abstrData.get(element)==null){
				hmData.remove(element);
			}
		}
		*/
		List<Map.Entry<Long, Long>> hmDataList = getSortedMap(hmData, false);
		
		int sel = -1;
		String selectAbstractors = thBean.getSelectAbstractors();
		if(selectAbstractors!=null) 
			sel = Integer.parseInt(selectAbstractors);
		
		if(sel>=0){
			dataset.setValue(abstrData.get(new Long(sel)), hmData.get(new Long(sel)));
			hashMap = null;
		} else {
			dataset = getDatasetFrom(sel,hmDataList,abstrData);
		}
		
		return dataset;
	}

	private DefaultPieDataset getDatasetAgents(HttpSession session) throws Exception{
		//System.err.println("========================================================");
		DefaultPieDataset dataset = new DefaultPieDataset();
		ThroughputBean thBean = null;
		thBean = (ThroughputBean)session.getAttribute(beanName);
		
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		int payrateType = 0;
		if (UserUtils.isTSAdmin(currentUser.getUserAttributes()))
			payrateType = 1;
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		int commId = Integer.parseInt(thBean.getSelectCommunities());
		int productId = Integer.parseInt(thBean.getSelectProducts());
		
		loadAttribute(thBean, RequestParams.REPORTS_STATE);
		loadAttribute(thBean, RequestParams.REPORTS_COUNTY);
		loadAttribute(thBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(thBean, RequestParams.REPORTS_AGENT);
		loadAttribute(thBean, RequestParams.REPORTS_COMPANY_AGENT);
		
		
		hashMap = new HashMap<String, String>();
		
		HashMap<Long, Long> hmData = null;
		if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT) || getFromPage().contains(URLMaping.REPORT_INCOME))
			hmData = PieDatabase.getAgentsDataGeneral(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, payrateType, productId, groupId, commId);
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_Y) || getFromPage().contains(URLMaping.REPORT_INCOME_Y))
			hmData = PieDatabase.getAgentsDataAnnual(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, payrateType, productId, groupId, commId, getYearReport());
		else if(getFromPage().contains(URLMaping.REPORT_THROUGHPUT_M) || getFromPage().contains(URLMaping.REPORT_INCOME_M))
			hmData = PieDatabase.getAgentsDataMonthly(beanName, reportState, reportCounty, reportAbstractor, reportAgent, 
					reportCompanyAgent, payrateType, productId, groupId, commId, getYearReport(), getMonthReport());
		
		hmData.remove(new Long(0));			//for the cases where we have agent_id = 0, and we shouldn't
		hmData.remove(new Long(-1));		//for the cases where we have agent_id = -1, and we shouldn't
		

		//setting the hashmap containing abstractor id-name pairs 
		//AgentAttributes[] agents = DBManager.getAllAgentsForSelect(commId);
		UserAttributes[] users = DBManager.getAllUsersFromCategory(groupId);
		HashMap<Long,String> agentsData = new HashMap<Long, String>();
		for (int i = 0; i < users.length; i++) {
			agentsData.put(users[i].getID().longValue(), users[i].getFIRSTNAME() + " " + users[i].getLASTNAME());
		}
		//this will remove entries in hmData that have no correspondat in the agantsData
		//usefull if a user doesn't exist any more
		validateData(hmData,agentsData);
		/*
		//defensive against cases where an Abstractor is also an Agent (user_id = 133)
		Vector<Long> keys = new Vector<Long>(hmData.keySet());
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Long element = (Long) iter.next();
			if(agentsData.get(element)==null){
				hmData.remove(element);
			}
		}
		*/
		List<Map.Entry<Long, Long>> hmDataList = getSortedMap(hmData, false);
		
		int sel = -1;
		String selectAgents = thBean.getSelectAgents();
		
		if(selectAgents!=null) 
			sel = Integer.parseInt(selectAgents);
		
		
		if(sel>=0){
			dataset.setValue(agentsData.get(new Long(sel)), hmData.get(new Long(sel)));
			hashMap = null;
		} else {
			dataset = getDatasetFrom(sel,hmDataList,agentsData);
		}
		
		return dataset;
	}
	
	/**
	 * This method removes any entries in hmData that havo no correspondat entry in validateAgainst hashmap
	 * @param hmData the hashmap that will be validated
	 * @param validateAgainst the hashmap used to validate 
	 */
	private void validateData(HashMap<Long, Long> hmData, HashMap<Long, String> validateAgainst) {
		Set<Long> keys = hmData.keySet();
		Vector<Long> toBeRemoved = new Vector<Long>();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Long key = (Long) iter.next();
			if(validateAgainst.get(key)==null)
				toBeRemoved.add(key);
		}
		for (Iterator iter = toBeRemoved.iterator(); iter.hasNext();) {
			Long key = (Long) iter.next();
			hmData.remove(key);	
		}
	}


	private DefaultPieDataset getDatasetAux(int selectOption, List<Map.Entry<Long, Long>> rawData, HashMap<Long, String> nameData) {
		DefaultPieDataset dataset = new DefaultPieDataset();
		Vector<String> sectionName = new Vector<String>();
		Vector<Long> data = new Vector<Long>();
		
		double limit = 0;
		for (int i = 0; i < rawData.size(); i++) {
			limit += rawData.get(i).getValue();
		}
		limit /= 36;					//limit = limit * 10 / 360;
		
		double other = 0;
		
		for (int i = 0; i < rawData.size(); i++) {
			if(rawData.get(i).getValue() > limit){
				if(nameData.get(rawData.get(i).getKey())==null)
					continue;
				sectionName.add(nameData.get(rawData.get(i).getKey()));	//getting the name for that id
				data.add(rawData.get(i).getValue());
				hashMap.put(nameData.get(rawData.get(i).getKey()), rawData.get(i).getKey().toString());
			}
			else
				other += rawData.get(i).getValue();
		}
		
		if(other>0){
			//pentru cazul cand avem other, voi seta selectia astfel:
			//primul other are valoare -2, al doilea other are valoare -3...
			//deci voi scade 1 din ultima selectie facuta
			//cum initial am -1 (pt nimic selectat) voi avea -2, si tot asa
			//daca selectia va fi pozitiva e clar ca nu se va pune problema lui other pentru ca deja am selectat ceva valid
			sectionName.add("Other");
			//if(other<limit)				//setting other to at least 10 degres
				other = limit;
			data.add(new Double(other).longValue());
			hashMap.put("Other", new Integer(selectOption-1).toString());	
		}
		
		for (int i = 0; i < data.size(); i++) {
			/*if(sectionName.elementAt(i)==null)
				System.err.println("sectionName.elementAt(i): " + i);
			if(data.elementAt(i)==null)
				System.err.println("data.elementAt(i): " + i);*/
			dataset.setValue(sectionName.elementAt(i), data.elementAt(i));
		}
		return dataset;
	}
	
	private DefaultPieDataset getDatasetFrom(int selected, List<Entry<Long, Long>> hmDataList, HashMap<Long, String> selectedData) {
		
		DefaultPieDataset dataset = new DefaultPieDataset();
		
		if (selected == -1){
			dataset = getDatasetAux(selected, hmDataList, selectedData);
		} else {
			int aux = 0;
			double prag = 0;
			
			//aici stiu ca am selectat other
			//vreau sa elimin din hmData toate elementele care nu sunt continute de selectia other anterioara
			for(int i=-2; i>=selected; i--){
				prag = 0;
				for (int j = 0; j < hmDataList.size(); j++) {
					prag += hmDataList.get(j).getValue();
				}
				prag /= 36;		//prag = prag * 10 / 360
				//trebuie sa elimin acum toate intrarile din hmData cu value > prag
				aux=0;
				for (int j = 0; j < hmDataList.size(); j++) {
					if(hmDataList.get(j).getValue() <= prag){
						//cand ajung sub prag tin minte cate elemente trebuie sa elimin
						aux = j;
						break;
					}
				}
				hmDataList = hmDataList.subList(aux, hmDataList.size());
			}
			dataset = getDatasetAux(selected, hmDataList, selectedData);
			
		}
		return dataset;
	}
	
	private void loadAttribute(ThroughputBean thBean, String attributeName){
		int sel = 0;
		String[] strArray;
		if(attributeName.equals(RequestParams.REPORTS_STATE)){
			sel = Integer.parseInt(thBean.getSelectStates());
			if(sel>0){
				reportState = new int[1];
				reportState[0] = sel;
			} else {
				strArray = thBean.getMultiStates();						//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportState = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportState[i] = Integer.parseInt(strArray[i]);
						if(reportState[i] < -1)
							reportState[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_ABSTRACTOR)){
			sel = Integer.parseInt(thBean.getSelectAbstractors());
			if(sel>0){
				reportAbstractor = new int[1];
				reportAbstractor[0] = sel;
			} else {
				strArray = thBean.getMultiAbstractors();				//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportAbstractor = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportAbstractor[i] = Integer.parseInt(strArray[i]);
						if(reportAbstractor[i] < -1)
							reportAbstractor[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_AGENT)){
			sel = Integer.parseInt(thBean.getSelectAgents());
			if(sel>0){
				reportAgent = new int[1];
				reportAgent[0] = sel;
			} else {
				strArray = thBean.getMultiAgents();						//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportAgent = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportAgent[i] = Integer.parseInt(strArray[i]);
						if(reportAgent[i] < -1)
							reportAgent[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_COUNTY)){
			sel = Integer.parseInt(thBean.getSelectCounties());
			if(sel>0){
				reportCounty = new int[1];
				reportCounty[0] = sel;
			} else {
				strArray = thBean.getMultiCounties();					//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportCounty = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportCounty[i] = Integer.parseInt(strArray[i]);
						if(reportCounty[i] < -1)
							reportCounty[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_COMPANY_AGENT)){
			reportCompanyAgent = thBean.getMultiCompaniesAgents();
		}
	}
	
	public String getChartViewer(HttpServletRequest request, HttpServletResponse response, String chartType) {
		
		String pathInfo = "http://";
		pathInfo += request.getServerName();
		int port = request.getServerPort();
		pathInfo += ":" + String.valueOf(port);
		pathInfo += request.getContextPath();
		//String pathServlet = pathInfo + "/ThroughputServlet?";
		String argGoToPage = "javascript:goToPage('";
		String tooltipText = "";
		StringBuffer reqUrl = request.getRequestURL();
		setFromPage(reqUrl.substring(reqUrl.lastIndexOf(request.getContextPath())+request.getContextPath().length()));
		setYearReport((String)request.getSession().getAttribute("yearReport"));
		setMonthReport((String)request.getSession().getAttribute("monthReport"));
		setDayReport((String)request.getSession().getAttribute("dayReport"));
		
		HttpSession session = request.getSession();
		ThroughputBean thBean = null;
	
		if(getFromPage().contains("throughput")){
			thBean = (ThroughputBean)session.getAttribute(ThroughputOpCode.THROUGHPUT_BEAN);
			beanName = ThroughputOpCode.THROUGHPUT_BEAN;
			tooltipText = "{0} = {1}";
		}
		else {
			thBean = (ThroughputBean)session.getAttribute(ThroughputOpCode.INCOME_BEAN);
			beanName = ThroughputOpCode.INCOME_BEAN;
			tooltipText = "{0} = $ {1}";
			
		}
		argGoToPage += "type=" + beanName + "&";					//adding type (throughput or income)
		
		DefaultPieDataset dataset = null;
		String lastColor = null;
		if(chartType.equals(ThroughputOpCode.GROUPS)){
			try {
				dataset = getDatasetGroups(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(chartType.equals(ThroughputOpCode.COMMUNITIES)){
			try {
				dataset = getDatasetCommunities(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(chartType.equals(ThroughputOpCode.STATES)){
			lastColor = thBean.getColorStates();
			try {
				dataset = getDatasetStates(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(chartType.equals(ThroughputOpCode.COUNTIES)){
			lastColor = thBean.getColorCounties();
			try {
				dataset = getDatasetCounties(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(chartType.equals(ThroughputOpCode.PRODUCTS)){
			lastColor = thBean.getColorProducts();
			try {
				dataset = getDatasetProducts(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(chartType.equals(ThroughputOpCode.ABSTRACTORS)){
			lastColor = thBean.getColorAbstractors();
			try {
				dataset = getDatasetAbstractors(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(chartType.equals(ThroughputOpCode.AGENTS)){
			lastColor = thBean.getColorAgents();
			try {
				dataset = getDatasetAgents(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			//System.err.println("Primit cerere pentru pie de tipul " + chartType + " dar nu este implementat");
			//dataset = getDatasetGroups(thBean);
		}
		
		// create the chart...
		JFreeChart chart = ChartFactory.createPieChart(null, // chart title
				dataset, // data
				false, // include legend
				true, // tooltips?
				false // URLs?
				);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);
				
		//LegendTitle legendTitle = chart.getLegend();
		//legendTitle.setBorder(BlockBorder.NONE);
		//legendTitle.setItemFont(new Font(null,Font.PLAIN,10));
		//LegendItemSource[] litem = legendTitle.getSources();
		
		
		
		// get a reference to the plot for further customisation...
		PiePlot plot = (PiePlot)chart.getPlot();
		plot.setNoDataMessage("No data available");
		plot.setBackgroundPaint(Color.white);
		//plot.setLabelLinksVisible(false);		
		//plot.setLabelGenerator(null);				//alea galbene care orbiteaza in jurul pie-ului :)
		//plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}"));
		plot.setLabelGenerator(new ATSPieSectionLabelGenerator(chartType, "{0}"));
		//plot.getLab
		//plot.setLabel
		//plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator("{0}"));
		
		
		if(dataset.getItemCount()==1)
			plot.setSectionOutlinesVisible(false);
		else
			plot.setSectionOutlinePaint(Color.white);
		
		
		colorHashMap = new HashMap<String, String>();

		for (int i = 0; i < dataset.getItemCount(); i++) {
			Color c = (Color)plot.getSectionPaint(i);
			//colorHashMap.put(dataset.getKey(i).toString(), new Integer(c.getRGB()).toString());
		}
		//daca avem o culoare deja setata si daca avem doar un element, inseamna ca trebuie sa ii setez culoarea
		if(lastColor!=null && dataset.getItemCount()==1 && lastColor.length()!=0 && !lastColor.equals(thBean.INVALID)){
			plot.setSectionPaint(0, new Color(Integer.parseInt(lastColor)));
			colorHashMap.put(dataset.getKey(0).toString(), lastColor);
		}
		/*
		if(chartType.equals(ThroughputOpCode.GROUPS)){
			plot.setURLGenerator(new StandardPieLinkGenerator(
					argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.DRILL_GROUPS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_GROUPS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					"&" + ThroughputOpCode.NAME_OBJECT + "=", hashMap));
		}
		else if(chartType.equals(ThroughputOpCode.COMMUNITIES)){
			plot.setURLGenerator(new StandardPieLinkGenerator(
					argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.DRILL_COMMUNITIES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_COMMUNITIES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					"&" + ThroughputOpCode.NAME_OBJECT + "=", 
					hashMap));
		}
		else if(chartType.equals(ThroughputOpCode.STATES)){
			if(thBean.getShowAbstractors()){
				plot.setURLGenerator(new StandardPieLinkGenerator(
						argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.DRILL_STATES + "&" + ThroughputOpCode.SELECT_OBJECT +"="	,
						argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_STATES + "&" + ThroughputOpCode.SELECT_OBJECT +"=", 
						"&" + ThroughputOpCode.NAME_OBJECT + "=", 
						hashMap));
			}
			else if(hashMap!=null) {
				//if we are here it means that we should only be able to filtes states
				//and if we have already set a state(which mean selectStates is positive) we shouldn't display any link
				if(Integer.parseInt(thBean.getSelectStates())<0){
					plot.setURLGenerator(new StandardPieLinkGenerator(
							argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_STATES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
							"&" + ThroughputOpCode.COLOR_OBJECT + "=",
							hashMap, colorHashMap));
				} 
				
			}
		}
		else if(chartType.equals(ThroughputOpCode.PRODUCTS)){
			if(hashMap!=null){	
				plot.setURLGenerator(new StandardPieLinkGenerator(
						argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_PRODUCTS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "=",
						hashMap,colorHashMap));
			}
		}
		else if(chartType.equals(ThroughputOpCode.COUNTIES)){
			if(hashMap!=null){	
				plot.setURLGenerator(new StandardPieLinkGenerator(
						argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_COUNTIES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "=",
						hashMap,colorHashMap));
			}
		}
		else if(chartType.equals(ThroughputOpCode.ABSTRACTORS)){
			if(hashMap!=null){	
				plot.setURLGenerator(new StandardPieLinkGenerator(
						argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_ABSTRACTORS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "="
						,hashMap,colorHashMap));
			}
		}
		else if(chartType.equals(ThroughputOpCode.AGENTS)){
			if(hashMap!=null){	
				plot.setURLGenerator(new StandardPieLinkGenerator(
						argGoToPage + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_AGENTS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "="
						,hashMap,colorHashMap));
			}
		}
		*/
		plot.setToolTipGenerator(new StandardPieToolTipGenerator(tooltipText));


		int size = 0;
		
		if(thBean.getShowGroups()){
			size = 400;
		} else if (thBean.getShowCommunities()){
			size = 400;
		} else {
			size = 300;
		}
		
		ChartRenderingInfo info = null;
		double rnd = Math.random();
		try {

			// Create RenderingInfo object
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart
					.createBufferedImage(size, 300, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			
			//session.setAttribute("pieChart"+chartType, null);
			session.setAttribute("pieChart"+chartType + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "imageMap" + chartType, info, false);
			writer.flush();

		} catch (Exception e) {
			// handel your exception here
		}

		
		String chartViewer = pathInfo + "/ChartViewer?chartName=pieChart"+chartType+rnd;
				
		return chartViewer;
	}
	
	public static List<Map.Entry<Long, Long>> getSortedMap(HashMap<Long, Long> hmap, boolean ascend)
	{
		List<Map.Entry<Long, Long>> list = new Vector<Map.Entry<Long, Long>>(hmap.entrySet());
		
		//Sort the list using an annonymous inner class implementing Comparator for the compare method
		if (ascend) {
	        Collections.sort(list, new Comparator<Map.Entry<Long, Long>>(){
	            public int compare(Map.Entry<Long, Long> entry, Map.Entry<Long, Long> entry1)
	            {
	                // Return 0 for a match, -1 for less than and +1 for more then
	                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
	            }
	        });
		} else {
			Collections.sort(list, new Comparator<Map.Entry<Long, Long>>(){
	            public int compare(Map.Entry<Long, Long> entry, Map.Entry<Long, Long> entry1)
	            {
	                // Return 0 for a match, 1 for less than and -1 for more then
	                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? -1 : 1));
	            }
	        });
		}
		return list;
	}
	
	public String getFromPage() {
		return fromPage;
	}
	public void setFromPage(String fromPage) {
		this.fromPage = fromPage;
	}
	public String getDayReport() {
		return dayReport;
	}
	public void setDayReport(String dayReport) {
		this.dayReport = dayReport;
	}
	public String getMonthReport() {
		return monthReport;
	}
	public void setMonthReport(String monthReport) {
		this.monthReport = monthReport;
	}
	public String getYearReport() {
		return yearReport;
	}
	public void setYearReport(String yearReport) {
		this.yearReport = yearReport;
	}
}




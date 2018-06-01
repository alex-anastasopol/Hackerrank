package ro.cst.tsearch.webservices;

import static ro.cst.tsearch.bean.SearchAttributes.ABSTRACTOR_FILENO;
import static ro.cst.tsearch.bean.SearchAttributes.ADDITIONAL_INFORMATION;
import static ro.cst.tsearch.bean.SearchAttributes.ADDITIONAL_LENDER_LANGUAGE;
import static ro.cst.tsearch.bean.SearchAttributes.BM1_LENDERNAME;
import static ro.cst.tsearch.bean.SearchAttributes.BM1_LOADACCOUNTNO;
import static ro.cst.tsearch.bean.SearchAttributes.BM2_LOADACCOUNTNO;
import static ro.cst.tsearch.bean.SearchAttributes.ECORE_AGENT_ID;
import static ro.cst.tsearch.bean.SearchAttributes.FROMDATE;
import static ro.cst.tsearch.bean.SearchAttributes.IS_CONDO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_BOOKNO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_LOTNO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_PAGENO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_PARCELNO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_BLOCK;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_NAME;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_PHASE;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_RNG;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_SEC;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_TWN;
import static ro.cst.tsearch.bean.SearchAttributes.LEGAL_DESCRIPTION;
import static ro.cst.tsearch.bean.SearchAttributes.ORDERBY_FILENO;
import static ro.cst.tsearch.bean.SearchAttributes.P_CITY;
import static ro.cst.tsearch.bean.SearchAttributes.P_COUNTY;
import static ro.cst.tsearch.bean.SearchAttributes.P_STATE;
import static ro.cst.tsearch.bean.SearchAttributes.P_STREETDIRECTION;
import static ro.cst.tsearch.bean.SearchAttributes.P_STREETNAME;
import static ro.cst.tsearch.bean.SearchAttributes.P_STREETNO;
import static ro.cst.tsearch.bean.SearchAttributes.P_STREETSUFIX;
import static ro.cst.tsearch.bean.SearchAttributes.P_STREETUNIT;
import static ro.cst.tsearch.bean.SearchAttributes.P_ZIP;
import static ro.cst.tsearch.bean.SearchAttributes.QUARTER_ORDER;
import static ro.cst.tsearch.bean.SearchAttributes.SEARCH_ORIGIN;
import static ro.cst.tsearch.bean.SearchAttributes.SEARCH_PRODUCT;
import static ro.cst.tsearch.bean.SearchAttributes.STEWARTORDERS_CUSTOMER_GUID;
import static ro.cst.tsearch.bean.SearchAttributes.STEWARTORDERS_ORDER_ID;
import static ro.cst.tsearch.bean.SearchAttributes.SURECLOSE_FILE_ID;
import static ro.cst.tsearch.bean.SearchAttributes.TITLEDESK_ORDER_ID;
import static ro.cst.tsearch.bean.SearchAttributes.TITLE_UNIT;
import static ro.cst.tsearch.bean.SearchAttributes.TODATE;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;
import static ro.cst.tsearch.utils.XmlUtils.getChildren;
import static ro.cst.tsearch.utils.XmlUtils.getNodeValue;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mailtemplate.MailTemplateUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.SearchExternalFlag;
import ro.cst.tsearch.database.transactions.SaveSearchTransaction;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.InvalidEmailOrderException;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servlet.TSD;
import ro.cst.tsearch.servlet.UserValidation;
import ro.cst.tsearch.servlet.ValidateInputs;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.warning.MultiplePropertiesReceivedWarning;
import com.stewart.ats.base.warning.WarningInfoI;

/**
 * Place order service
 * @author radu bacrau
 */
public class PlaceOrderService extends AbstractService {

	public static final Logger loggerLocal = Logger.getLogger(PlaceOrderService.class);
	
	private String atsUser = "";
	private String atsPassword = "";
	
	private Search search = null;
	private String htmlOrder = null;
	private String commFileId = null;
	private User user = null;
	private List<WarningInfoI> warningList = null;
	
	/**
	 * Constructor
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 */
	public PlaceOrderService(String appId, String userName, String password, String order, String dateReceived){
		super(appId, userName, password, order, dateReceived);	
		successMessage = "placed";
		warningList = new ArrayList<WarningInfoI>();
	}

	/**
	 * 
	 */
	public void process(){		
		
		// check for errors from constructor
		if(errors.size() > 0){
			loggerLocal.error( logPrefix + ": .... Something got wrong in  PlaceOrderService constructor ....\n" + this.order  );
			return;
		}
		
		try {

			// create search attributes
			SearchAttributes sa = new SearchAttributes(-1);
			

			// fill the search attributes	
			parseOrder(orderDoc, sa);
			// delay checking for errors until we also check the user
			
			// check valid account
			User user = null;
			if(isEmpty(atsUser)){
				errors.add("Empty ats user");
			}
			if(isEmpty(atsPassword)){
				errors.add("Empty ats password");
			}
			if(!isEmpty(atsUser) && !isEmpty(atsPassword)){
				user = getUser(atsUser, atsPassword);
				if(user == null || user.getUserAttributes() == null){
					errors.add("Invalid ats user/password: " + atsUser + "/" + atsPassword);
				}
			    if (user!=null&&user.getUserAttributes() != null&&!UserUtils.isAgent(user.getUserAttributes())){
			    	errors.add("To place an order the user must have Agent role , the following user has not: " + atsUser + "/" + atsPassword);
			    }
			}
			
			this.user = user;
			
			// check for both parseOrder errors and user not found errors
			if(errors.size() > 0){
				loggerLocal.error( logPrefix + ": .... parseOrder errors and user not found errors ....\n" + this.order  );
				return;
			}

			// determine user attributes and community attributes
			UserAttributes ua = user.getUserAttributes();
			int crtCommId = ua.getCOMMID().intValue();
			sa.setCommId(crtCommId);	//force community id
			
			CommunityAttributes ca = CommunityUtils.getCommunityFromId(crtCommId); 
						
			// if one of ORDERBY_FILENO, ABSTRACTOR_FILENO is missing, copy the other one into it
			if("".equals(sa.getAtribute(ORDERBY_FILENO))){
		        sa.setAtribute(ORDERBY_FILENO, sa.getAtribute(ABSTRACTOR_FILENO));            
		    } else if("".equals(sa.getAtribute(ABSTRACTOR_FILENO))){
		        sa.setAtribute(ABSTRACTOR_FILENO, sa.getAtribute(ORDERBY_FILENO));
		    }
			
			// check that the search attributes contain enough info
			verifySearchAttributes(sa, ca);
			if(errors.size() > 0){
				loggerLocal.error( logPrefix + ": .... check that the search attributes contain enough info ....\n" + this.order  );
				return;
			}	
		
			// create search and connect it to search attributes			
			Search search = SearchManager.addNewSearch(user, false);
			if( search.getID()<=0 ){
				loggerLocal.error( logPrefix + ": .... Internal error Search generated with ID = "+ search.getID() +" .... \n" + this.order  );
				errors.add( "Internal error Search generated with ID"+ search.getID() );
				return;
			}
			
			 
			SearchExternalFlag externalFlag = new SearchExternalFlag(search.getID());
			externalFlag.setSoOrderId(org.apache.commons.lang.StringUtils.defaultIfBlank(
					sa.getAtribute(STEWARTORDERS_ORDER_ID), null));
			externalFlag.setSoCustomerGuid(org.apache.commons.lang.StringUtils.defaultIfBlank(
					sa.getAtribute(STEWARTORDERS_CUSTOMER_GUID), null));
			externalFlag.setSoOrderProductId(org.apache.commons.lang.StringUtils.defaultIfBlank(
					sa.getAtribute(SearchAttributes.STEWARTORDERS_ORDER_PRODUCT_ID), null));
			externalFlag.setScFileId(org.apache.commons.lang.StringUtils.defaultIfBlank(
					sa.getAtribute(SURECLOSE_FILE_ID), null));
			externalFlag.setTdOrderId(org.apache.commons.lang.StringUtils.defaultIfBlank(
					sa.getAtribute(TITLEDESK_ORDER_ID), null));
			externalFlag.setSoToUpdateGuid(org.apache.commons.lang.StringUtils.defaultIfBlank(
					sa.getAtribute(SearchAttributes.STEWARTORDERS_TO_UPDATE_ORDER_GUID), null));
			externalFlag.setSoParentGuid(org.apache.commons.lang.StringUtils.defaultIfBlank(
					sa.getAtribute(SearchAttributes.STEWARTORDERS_PARENT_ORDER_GUID), null));
			
			if(!externalFlag.saveToDatabase()) {
				logger.error(logPrefix + ":Could not save SearchExternalFlag for searchid " + search.getID());
			}
			
			if (sa.getProductId() == Products.FVS_PRODUCT){
				sa.setAtribute(SearchAttributes.FVS_UPDATE, "true");
			}
			
			sa.setSearchId(search.getID());
			search.setSa(sa);
			SearchAttributes initialSearchAttributes = (SearchAttributes)sa.clone();
			

			loggerLocal.info( logPrefix + ": .... Order received and good Search created.... "+ 
					" Search origin:"+sa.getAtribute(SEARCH_ORIGIN)+""+
					" AbstrFileNo: "+ sa.getAtribute(ABSTRACTOR_FILENO)+""+
					" AgentFileNo: "+ sa.getAtribute(ORDERBY_FILENO)+"\n"+
					" SearchId: " + search.getID()
					);
			
			// set abstractor and agent to be the same as the user			
	        search.getSa().setAbstractor(ua);
			search.setAgent(ua);
			search.getSa().setOrderedBy(ua);
			
			// setup the search and search ua
			InstanceManager.getManager().getCurrentInstance(search.getID()).setCrtSearchContext(search);
	        InstanceManager.getManager().getCurrentInstance(search.getID()).setCurrentUser(ua);
	        InstanceManager.getManager().getCurrentInstance(search.getID()).setCurrentCommunity(ca);

	        for (WarningInfoI warning : warningList) {
	        	 search.getSearchFlags().addWarning(warning);
			}
	       
	        
	        
	        this.search = search;
	        
	        boolean cancelFVS = false;
	        
			// treat update case - may change the FROMDATE and TODATE values
	        try {
	        	ValidateInputs.checkIfUpdate(search, TSOpCode.SUBMIT_ORDER, null); // pass null request
		    
	        	if (sa.isFVSUpdate()){
	        		if (search.getParentSA() != null) {
						if (search.getParentSA().getCertificationDate() == null
								|| search.getParentSA().getCertificationDate().getDate() == null) {
							cancelFVS = true;
						}
					} else{
						cancelFVS = true;
					}
	        	}
	        	if (cancelFVS){
	        		String commAdminEmail = null;
					try {
						UserAttributes commAdmin = UserUtils.getUserFromId(CommunityUtils.getCommunityAdministrator(ca));
						commAdminEmail = commAdmin.getEMAIL();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Util.sendMail(null, user.getUserAttributes().getEMAIL(), commAdminEmail, null,
							"FVS Report on " + SearchLogger.getCurDateTimeCST() + " for fileID: " + sa.getAtribute(ORDERBY_FILENO) + ", searchid: " + search.getID(),
							"Missing Effective Date on Original File!\n\nFVS Update not started.");
					
					logger.error(logPrefix + ": Missing Effective Date on Original File!FVS Update not started.");
					loggerLocal.error( logPrefix + ": .... Missing Effective Date on Original File!FVS Update not started....\n" + this.order  );
					errors.add("Missing Effective Date on Original File!FVS Update not started.");
					
					SearchManager.removeSearch(search.getID(), true);
					DBManager.deleteSearch(search.getID(),  Search.SEARCH_TSR_NOT_CREATED);
					return;
	        	}
			    // treat the refinance case
			    ValidateInputs.checkIfRefinance(null, search); // pass null request
			    
	        } catch(InvalidEmailOrderException e){
	        	logger.error(logPrefix + ": Exception", e);
	        	loggerLocal.error( logPrefix + ": .... checkIfUpdate  checkIfRefinance ....\n" + this.order, e  );
	        	errors.add(e.getMessage());
	        	return;
	        }
	
		    sa.setAbstractorFileName(search);		
			
			// fill and save the search order
			this.htmlOrder = MailTemplateUtils.fillAndSaveSearchOrder(search, initialSearchAttributes, user);
			
			try {
				HashCountyToIndex.setSearchServer(search, HashCountyToIndex.getFirstServer( 
						search.getProductId(), 
						search.getCommId(), 
						Integer.parseInt(sa.getCountyId()) ));				
			} catch(NotImplFeatureException npe) {
				ValidateInputs.checkCountyImpl(search);
			} catch(NumberFormatException nfe) {  
				logger.error(logPrefix + ":Imposible error!", nfe);
			}
			
			search.fillEmptySearchStatus();
			
	        // set search type
	        if(!search.isCountyImplemented()){ 						
				search.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
	        }else{
				search.setSearchType(Search.AUTOMATIC_SEARCH);
			}		
			DBManager.setSearchOwner(search.getID(),(search.isCountyImplemented() ? -1 : 0));
	        
	        //String sPath = BaseServlet.REAL_PATH + File.separator + "title-search";
	        String sPath = ServerConfig.getRealPath();
	        TSD.initSearch(user, search, sPath, null);
	        
			try {			
				// save empty search
				int saved = DBManager.saveCurrentSearch(user, search, Search.SEARCH_TSR_NOT_CREATED, null); // pass null request			
				if(saved != SaveSearchTransaction.STATUS_SUCCES){
					logger.error(logPrefix + ": Error saving the search received through web-services");
					loggerLocal.error( logPrefix + ": .... Error saving the search received through web-services " + saved + "....\n" + this.order  );
					errors.add("Internal error: search could not be saved");
					SearchManager.removeSearch(search.getID(), true);
					return;
				}
				// set search owner to 0, like in AShread.java
				DBManager.setSearchOwner(search.getID(), 0);
			} catch (SaveSearchException e1) {
				logger.error(logPrefix + ": Error saving the search received through web-services", e1);
				loggerLocal.error( logPrefix + ": .... Error saving the search received through web-services ....\n" + this.order  );
				errors.add("Internal error: search could not be saved");
				SearchManager.removeSearch(search.getID(), true);
				return;
			}
			
			try{
				DBManager.zipAndSaveSearchToDB( search );
			}
			catch(Exception e2){
				logger.error(logPrefix + ": Error saving the search context received through web-services", e2);
				loggerLocal.error( logPrefix + ":  ... Error saving the search context received through web-services \n" + this.order  );
				errors.add("Internal error: search context could not be saved");
				SearchManager.removeSearch(search.getID(), true);
				DBManager.deleteSearch(search.getID(),  Search.SEARCH_TSR_NOT_CREATED);
				return;
			}
			String userLogin = "";
	        if (user != null){
	        	 userLogin = user.getUserAttributes().getNiceName();
	        }
	        
	        String commProductName = "";
	        try {
				commProductName = (CommunityProducts.getProduct(ca.getID().longValue())).getProductName(sa.getProductId());
			} catch (Exception e) {
			}
			String msgStr = "\n</div><div><BR><B>Search ";
			if (org.apache.commons.lang.StringUtils.isNotEmpty(commProductName)){
				msgStr += "(" + commProductName + ") ";
			}
			msgStr += "sent in Dashboard from %s on</B>: "
					+ SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "</BR></div>\n";

			if (PlaceOrderService.isTitleDesk(search.getSa())) {
				msgStr = String.format(msgStr, "Title Desk");
			} else if(org.apache.commons.lang.StringUtils.isNotBlank(externalFlag.getSoOrderId())) {
				msgStr = String.format(msgStr, "OG");
			} else {
				msgStr = String.format(msgStr, "AIM");
			}
			
			SearchLogger.infoUpdateToDB(msgStr, search.getID());
			// start automatic search thread.
			try{
				if(search.isCountyImplemented()) {
					synchronized (search) {
						ASMaster.startBackgroundSearch(search, user, ca);			
						SearchManager.addBackgroundSearch(search);
					}
				}
			} catch(Exception e) {
				logger.error(logPrefix + ": Automatic could not be started", e);
				loggerLocal.error( logPrefix + ": .... Automatic could not be started ....\n" + this.order  );
				warnings.add("Automatic search not started");
			}
			
			/////- B4548 - ///////
			if(crtCommId==31 && sa.getAtribute(SearchAttributes.SEARCH_ORIGIN).toUpperCase().startsWith("STEWARTORDERS")){
				sa.setAtribute(SearchAttributes.SEARCH_STEWART_TYPE, "STARTER");
			}/////- B4548 - end ///////
			
			loggerLocal.info( logPrefix + ": .... Order  processed  ...."+ 
					" Search origin: "+sa.getAtribute(SEARCH_ORIGIN)+""+
					" AbstrFileNo: "+ sa.getAtribute(ABSTRACTOR_FILENO)+""+
					" AgentFileNo: "+ sa.getAtribute(ORDERBY_FILENO)+""
					);
			
			
			
		}catch(Exception e){
			e.printStackTrace();
			loggerLocal.error( logPrefix + ": .... Internal error for  ....\n" + this.order, e  );
			errors.add(logPrefix + ": Internal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Authenticate ATS user 
	 * @param userName
	 * @param password
	 * @return null if the operation failed
	 */
	private User getUser(String userName, String password){
		try {
			UserAttributes userAttributes = UserManager.getUser(userName, password);
			User user = new User(UserValidation.getDirPath(ServerConfig.getFilePath()));
			user.setUserAttributes(userAttributes);		
			return user;
		} catch(RuntimeException e) {
			logger.error(logPrefix + ": Error validating user " + userName + "/" + password , e);
			return null;
		}		
	}

	
	/**
	 * 
	 * @param node
	 * @param sa
	 */
	private void parseOrder(Node node, SearchAttributes sa){
		
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
		
		// isolate the order
		Node order = null;
		for(Node child: getChildren(node)){
			if("ats".equalsIgnoreCase(child.getNodeName())){
				for(Node grand: getChildren(child)){
					if("order".equalsIgnoreCase(grand.getNodeName())){
						order = grand;
						break;
					} else {
						warnings.add("Node ignored: " + grand.getNodeName());
					}
				}
			} else {
				warnings.add("Node ignored: " + child.getNodeName());
			}
		}
		if(order == null){
			errors.add("Could not find <ats><order>");
			return;
		}
		
		// set search origin
		sa.setAtribute(SEARCH_ORIGIN, appId);

		// process the order		
		for(Node child: getChildren(order)){
			String childName = child.getNodeName();
			if("additionalLenderLanguage".equalsIgnoreCase(childName)){
				String additional = getNodeValue(child);
				sa.setAtribute(ADDITIONAL_LENDER_LANGUAGE, additional);					
			} else if("agent_id".equalsIgnoreCase(childName)){
				String orderId = getNodeValue(child);
				sa.setAtribute(ECORE_AGENT_ID, orderId);	
			}
			else if("title_unit".equalsIgnoreCase(childName)){
				String titleunit = getNodeValue(child);
				sa.setAtribute(TITLE_UNIT, titleunit);
			}
			else if("product_type".equalsIgnoreCase(childName)){
				parseProductType(getNodeValue(child), sa);
			}else if("from_date".equalsIgnoreCase(childName)){
				Date date = parseDate(child);
				if(date == null){
					warnings.add("could not parse from_date");
				} else {
					sa.setAtribute(FROMDATE, sdf.format(date));
				}
			} else if("thru_date".equalsIgnoreCase(childName)){
				Date date = parseDate(child);
				if(date == null){
					warnings.add("could not parse thru_date");
				} else {
					sa.setAtribute(TODATE, sdf.format(date));
				}
			} else if("need_by".equalsIgnoreCase(childName)){
				Date date = parseDate(child);
				if(date == null){
					warnings.add("could not parse need_by");
				} else {
					String info = sa.getAtribute(ADDITIONAL_INFORMATION);
					info += " Need By Date: " + new SimpleDateFormat("MM/dd/yyy").format(date);
					sa.setAtribute(ADDITIONAL_INFORMATION, info);
				}
			} else if("property".equalsIgnoreCase(childName)){
				parseProperty(child, sa);
			} else if("owners".equalsIgnoreCase(childName)){				
				parseParties(child, sa);
			} else if("buyers".equalsIgnoreCase(childName)){
				parseParties(child, sa);
			}else if("ignoreMiddleOwner".equalsIgnoreCase(childName)){
				String ignoreMiddleOwner = getNodeValue(child);
				if("1".equalsIgnoreCase(ignoreMiddleOwner)||"true".equalsIgnoreCase(ignoreMiddleOwner)){
					sa.setAtribute(SearchAttributes.IGNORE_MNAME, "true");
				}
			}else if("ignoreMiddleBuyer".equalsIgnoreCase(childName)){
				String ignoreMiddleBuyer = getNodeValue(child);
				if("1".equalsIgnoreCase(ignoreMiddleBuyer)||"true".equalsIgnoreCase(ignoreMiddleBuyer)){
					sa.setAtribute(SearchAttributes.IGNORE_MNAME_BUYER, "true");
				}
			}
			else if("sale_value".equalsIgnoreCase(childName)){
				String saleValue = getNodeValue(child);
				sa.setAtribute(BM2_LOADACCOUNTNO, saleValue);
			} else if("lender".equalsIgnoreCase(childName)){
				String lender = getNodeValue(child);
				sa.setAtribute(BM1_LENDERNAME, lender);
			} else if("loan_amount".equalsIgnoreCase(childName)){
				String loanAmount = getNodeValue(child);
				sa.setAtribute(BM1_LOADACCOUNTNO, loanAmount);
			} else if("agent_file_id".equalsIgnoreCase(childName)) {
				String agentFileId = getNodeValue(child);
				sa.setAtribute(ORDERBY_FILENO, agentFileId);
			} else if("community_file_id".equalsIgnoreCase(childName)){
				String commFileId = getNodeValue(child);
				sa.setAtribute(ABSTRACTOR_FILENO, commFileId);
				this.commFileId = commFileId;
			} else if("information".equalsIgnoreCase(childName)){
				String information = sa.getAtribute(ADDITIONAL_INFORMATION) + " " + getNodeValue(child);				
				sa.setAtribute(ADDITIONAL_INFORMATION, information);
			} 
			else if("TitleDeskOrderID".equalsIgnoreCase(childName)){
				String orderId = getNodeValue(child);
				sa.setAtribute(TITLEDESK_ORDER_ID, orderId);	
			}
			else if("StewartOrdersGUID".equalsIgnoreCase(childName)){
				String orderId = getNodeValue(child);
				sa.setAtribute(STEWARTORDERS_ORDER_ID, orderId);	
			}
			else if("CustomerGUID".equalsIgnoreCase(childName)){
				String customerGuid = getNodeValue(child);
				sa.setAtribute(STEWARTORDERS_CUSTOMER_GUID, customerGuid);	
			} else if("StewartOrdersToUpdateGUID".equalsIgnoreCase(childName)){
				String customerGuid = getNodeValue(child);
				sa.setAtribute(SearchAttributes.STEWARTORDERS_TO_UPDATE_ORDER_GUID, customerGuid);
			} else if("StewartOrdersParentGUID".equalsIgnoreCase(childName)){
				String customerGuid = getNodeValue(child);
				sa.setAtribute(SearchAttributes.STEWARTORDERS_PARENT_ORDER_GUID, customerGuid);	
			}else if("SurecloseUTI".equalsIgnoreCase(childName)){
				String surecloseUTI = getNodeValue(child);
				sa.setAtribute( SURECLOSE_FILE_ID, surecloseUTI );	
			}
			else if("OrderProductId".equalsIgnoreCase(childName)){
				String orderProductId = getNodeValue(child);
				sa.setAtribute(SearchAttributes.STEWARTORDERS_ORDER_PRODUCT_ID, orderProductId);
			}else if("ATSUserName".equalsIgnoreCase(childName)){
				atsUser = getNodeValue(child);
				sa.setAtribute(SearchAttributes.AGENT_USER, atsUser);
			} else if("ATSPassword".equalsIgnoreCase(childName)){
				atsPassword = getNodeValue(child);
				sa.setAtribute(SearchAttributes.AGENT_PASSWORD, atsPassword);
			} else if("searchType".equalsIgnoreCase(childName)){
				//B4548 sa.setAtribute(SearchAttributes.SEARCH_STEWART_TYPE, getNodeValue(child));	
			} else {
				warnings.add("Node ignored: " + childName);
			}
		}
		
		try {
		/* Set manual search page fields */
		  for( String attribute : new String[] {
					SearchAttributes.P_STREETNAME, 
					SearchAttributes.P_STREETNO, 
					SearchAttributes.P_STREETDIRECTION,
					SearchAttributes.P_STREET_POST_DIRECTION,
					SearchAttributes.P_STREETSUFIX,
					SearchAttributes.P_STREETUNIT,
					SearchAttributes.P_CITY,
					SearchAttributes.P_ZIP,
					SearchAttributes.LD_PARCELNO,
					SearchAttributes.LD_INSTRNO,
					SearchAttributes.LD_BOOKPAGE,
					SearchAttributes.LD_LOTNO, 
					SearchAttributes.LD_SUBLOT,
					SearchAttributes.LD_SUBDIV_NAME, 
					SearchAttributes.LD_SUBDIV_SEC,
					SearchAttributes.LD_SUBDIV_PHASE,
					SearchAttributes.LD_SUBDIV_BLOCK,
					SearchAttributes.LD_SUBDIV_UNIT,
					SearchAttributes.LD_SUBDIV_TRACT
					} )	{
			  sa.getSearchPageManualFields().put(attribute,sa.getAtribute(attribute));
		  }
		  
		  for(NameI name : sa.getOwners().getNames()) {
			  sa.getSearchPageManualOwners().add(name);
		  }
		  for(NameI name : sa.getBuyers().getNames()) {
			  sa.getSearchPageManualBuyers().add(name);
			  if (Integer.parseInt(sa.getAtribute(SearchAttributes.SEARCH_PRODUCT)) == SearchAttributes.SEARCH_PROD_REFINANCE) {
					sa.getSearchPageManualOwners().add(name); //CR 3870
				}
		  }
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Extract the date from a date subtree
	 * @param node
	 * @return
	 */
	private Date parseDate(Node node){
				
		// extract month, day, year
		String day = "";
		String month = "";
		String year = "";
		for(Node child: getChildren(node)){
			String childName = child.getNodeName();
			if("month".equals(childName)){
				month = getNodeValue(child);
			} else if("day".equals(childName)){
				day = getNodeValue(child);
			} else if("year".equals(childName)){
				year = getNodeValue(child);
			} else {
				warnings.add("Node ignored: " + childName);
			}
		}
		
		// parse the date
		if(month.matches("\\d{1,2}") && day.matches("\\d{1,2}") && year.matches("\\d{4}")){
			// fix month, day, year
			if(month.length() == 1){ 
				month = "0" + month; 
			}
			if(day.length() == 1){ 
				day = "0" + day; 
			}
			if(year.length() != 4){ 
				year = "2000".substring(0, 4 - year.length()) + year;
			}
			try {
				return new SimpleDateFormat("MM/dd/yyyy").parse(month + "/" + day + "/" + year);
			}catch(ParseException e){
				logger.error(logPrefix + ": error parsing date",  e);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param node
	 * @param sa
	 */
	private void parseParties(Node node, SearchAttributes sa){
		
		String prefix = "owners".equalsIgnoreCase(node.getNodeName())? "OWNER_" : "BUYER_";
		PartyI party = "owners".equalsIgnoreCase(node.getNodeName()) ? sa.getOwners() : sa.getBuyers();
			
//		int count = 0;		
		for(Node child: getChildren(node)){				
			String childName = child.getNodeName();			
			if("company".equalsIgnoreCase(childName) || "person".equalsIgnoreCase(childName)){	
				boolean company = "company".equalsIgnoreCase(childName);
//				String longPrefix = prefix;				
//				switch(count){
//					case 0: break;
//					case 1: longPrefix += "SPOUSE_"; break;  
//				}								
				String last = "";
				String first = "";
				String middle = "";
				String suffix = "";
				String ssn4 = "";
				String guid = "";
				
				for(Node grand: getChildren(child)){
					String grandName = grand.getNodeName();
					if("name".equalsIgnoreCase(grandName) || "last".equalsIgnoreCase(grandName)){
						last = StringUtils.cleanInitialOrderName(getNodeValue(grand));
					} else if ("first".equalsIgnoreCase(grandName)){
						first = StringUtils.cleanInitialOrderName(getNodeValue(grand));
					} else if("middle".equalsIgnoreCase(grandName)){
						middle = StringUtils.cleanInitialOrderName(getNodeValue(grand));
					} else if("suffix".equalsIgnoreCase(grandName)){
						suffix = StringUtils.cleanInitialOrderName(getNodeValue(grand));
					} else if("ssn4".equalsIgnoreCase(grandName)){
						ssn4 = getNodeValue(grand);
					} else if("guid".equalsIgnoreCase(grandName)){
						guid = getNodeValue(grand);
					} else {
						warnings.add("Node ignored: " + grandName);
					}
				}
				if(!isEmpty(first) || !isEmpty(middle) || !isEmpty(suffix) || !isEmpty(last)){
//					if(count < 2) {
//						sa.setAtribute(longPrefix + "FNAME", first);
//						sa.setAtribute(longPrefix + "MNAME", (middle + " " + suffix).trim());
//						sa.setAtribute(longPrefix + "LNAME", last);
//					}
					NameI name = new Name(first, middle, last);
					name.setCompany(company);
					name.setSufix(suffix);
					name.setSsn4Decoded(ssn4);
					name.setExternalSystemGuid(guid);
					
					if(prefix.equals("BUYER_")){
						name.setValidated(true);
					}
					name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
					party.add(name);					
//					count++;
				}
			} else {
				warnings.add("Node ignored: " + childName);
			}
		}
	}
	
	private static Map<Integer, String> TRANSLATION_TABLE = new HashMap<Integer, String>();
	
	static {
		TRANSLATION_TABLE.put(1,  "1");
		TRANSLATION_TABLE.put(2,  "5");
		TRANSLATION_TABLE.put(3,  "2");
		TRANSLATION_TABLE.put(4,  "10");
		TRANSLATION_TABLE.put(5,  "4");
		TRANSLATION_TABLE.put(6,  "3");
		TRANSLATION_TABLE.put(7,  "7");
		TRANSLATION_TABLE.put(8,  "8");
		TRANSLATION_TABLE.put(9,  "6");
		TRANSLATION_TABLE.put(10, "9");
		TRANSLATION_TABLE.put(12, "12");
	}
	
	/**
	 * Set search product
	 * @param value
	 * @param sa
	 */
	private void parseProductType(String value, SearchAttributes sa){

		int product = 0;		
		value = value.trim();
		if(value.length()==0){
			errors.add("Empty product type");	
			return;
		}
		if(value.matches("\\d+")){
			product = Integer.parseInt(value); 
		} else{
			errors.add("Incorrect product type: " + value);
		}		
		if(TRANSLATION_TABLE.containsKey(product)){
			sa.setAtribute(SEARCH_PRODUCT, TRANSLATION_TABLE.get(product));						
		} else {
			errors.add("Incorrect product type: " + value);
		}
	}
	
	/**
	 * 
	 * @param node
	 * @param sa
	 */
	private void parseProperty(Node node, SearchAttributes sa){
		for(Node child: getChildren(node)){
			String childName = child.getNodeName();
			if("address".equalsIgnoreCase(childName)){				
				parseAddress(child, sa);
			} else if("legal".equalsIgnoreCase(childName)){
				parseLegal(child, sa);
			} else if("property_id".equalsIgnoreCase(childName)){
				String pid = getNodeValue(child);
				sa.setAtribute(LD_PARCELNO, pid);
			} else if ("property_type".equalsIgnoreCase(childName)) {
				String propertyType = getNodeValue(child);
				sa.setAtribute(SearchAttributes.PROPERTY_TYPE, propertyType);
			}else if("is_condo".equalsIgnoreCase(childName)){
				String isCondo = getNodeValue(child);
				if(isCondo.length()==0){
					warnings.add("empty is_condo value");
				} else if("true".equalsIgnoreCase(isCondo) || "false".equalsIgnoreCase(isCondo)){
					sa.setAtribute(IS_CONDO, isCondo);
				} else {
					warnings.add("invalid is_condo value: " + isCondo);
				}
			} else {			
				warnings.add("Node ignored: " + childName);
			}
		}
	}

	/**
	 * 
	 * @param node
	 * @param sa
	 */
	private void parseAddress(Node node, SearchAttributes sa){
		String stateStr = null;
		String countyStr = null;
		for(Node child: getChildren(node)){
			String childName = child.getNodeName();
			if("street".equalsIgnoreCase(childName)){
				String name = "";
				String postDir = "";
				String number = "";
				for(Node grand: getChildren(child)){
					String grandName = grand.getNodeName();
					if("number".equals(grandName)){
						number += " " + getNodeValue(grand);
					} else if ("number_list".equals(grandName)){
						String interval = getInterval(grandName, grand);
						if(!number.contains(interval)) { 
							number += " " + interval;
						}
					} else if ("pre_direction".equals(grandName)){
						sa.setAtribute(P_STREETDIRECTION, getNodeValue(grand));
					} else if ("name".equals(grandName)){
						name =  getNodeValue(grand);
					} else if ("suffix".equals(grandName)){
						sa.setAtribute(P_STREETSUFIX, getNodeValue(grand));
					} else if ("post_direction".equals(grandName)){
						postDir =  getNodeValue(grand);
					} else if ("unit".equals(grandName)){
//						String uType = "";
						String uNum = "";
						for(Node gg: getChildren(grand)){
							String ggName = gg.getNodeName();
							if("type".equalsIgnoreCase(ggName)){
//								uType = getNodeValue(gg);
							} else if("number".equalsIgnoreCase(ggName)){
								uNum += " " + getNodeValue(gg);
							} else if ("number_list".equalsIgnoreCase(ggName)) {
								String interval = getInterval(grandName + ":" + ggName, gg);
								if (!uNum.contains(interval)) {
									uNum += " " + interval;
								}
							} else {
								warnings.add("Node ignored: " + ggName);
							}
						}
						sa.setAtribute(P_STREETUNIT,  uNum.trim());
					} else {
						warnings.add("Node ignored: " + grandName);
					}
				}
				
				String[] numbers = number.trim().split("[\\s-,;]+");
				if(numbers.length > 1) {
					sa.setAtribute(P_STREETNO, numbers[0] );
					MultiplePropertiesReceivedWarning warning = new MultiplePropertiesReceivedWarning("Street Number", number.trim());
					warnings.add(warning.toString());
					warningList.add(warning);
				} else {
					sa.setAtribute(P_STREETNO, number.trim() );
				}
				sa.setAtribute(P_STREETNAME, (name + " " + postDir).trim() );
				sa.setAtribute(SearchAttributes.P_ORDER_STREETNAME, name);
			} else if("city".equalsIgnoreCase(childName)){
				sa.setAtribute(P_CITY, getNodeValue(child));
			} else if("zip".equalsIgnoreCase(childName)){
				sa.setAtribute(P_ZIP, getNodeValue(child));
			} else if("state".equalsIgnoreCase(childName)){
				stateStr = getNodeValue(child);
			} else if("county".equalsIgnoreCase(childName)){
				countyStr = getNodeValue(child);
			} else {
				warnings.add("Node ignored: " + childName);
			}
		}	
		
		// check the state string
		if(isEmpty(stateStr)){
			if(isTitleDesk(sa)){
				stateStr = "FL";
				warnings.add("State info missing. Defaulted to FL");
			} else {
				errors.add("State info missing");
				return;
			}
		}
		
		// check actual state and county
		if(!isEmpty(stateStr)){
			GenericState state = DBManager.getStateForAbvStrict(stateStr);
			if(state == null){
				errors.add("State " + stateStr + " not found!");
				return;				
			}
			long stateId = state.getId();
			sa.setAtribute(P_STATE, "" + stateId);
			if(!isEmpty(countyStr)){
				if("AK".equalsIgnoreCase(state.getStateAbv())){
					if(countyStr.toLowerCase().contains("anchorage")){
						countyStr = "Anchorage Borough";
					}
				}
				GenericCounty county = DBManager.getCountyForNameAndStateIdStrict(countyStr, stateId,true);
				if(county == null){
					errors.add("County " + countyStr + " not found on state " + stateStr);
					return;
				}
				long countyId = county.getId();
				sa.setAtribute(P_COUNTY, "" + countyId);
			}
		}
				
	}
	
	/**
	 * 
	 * @param node
	 * @param sa
	 */
	private void parseLegal(Node node, SearchAttributes sa){
		for(Node child: getChildren(node)){
			String childName = child.getNodeName();
			if("subdivided".equalsIgnoreCase(childName)){
				parseSubdivided(child, sa);
			} else if("sectional".equalsIgnoreCase(childName)){
				parseSectional(child, sa);
			} else if("freeform".equalsIgnoreCase(childName)){
				sa.setAtribute(LEGAL_DESCRIPTION, getNodeValue(child));
			} else {
				warnings.add("Node ignored (legal): " + childName);
			}
		}
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	private String getInterval(String parentTagName, Node node){
		String retVal = "";
		for(Node child: getChildren(node)){
			String childName = child.getNodeName();
			if("value".equalsIgnoreCase(childName)){				
				retVal += " " + getNodeValue(child);				
			} else if("interval".equalsIgnoreCase(childName)){
				String low = "";
				String high = "";
				for(Node grand: getChildren(child)){
					String grandName = grand.getNodeName();
					if("high".equalsIgnoreCase(grandName)){
						high = getNodeValue(grand);
					} else if("low".equalsIgnoreCase(grandName)){
						low = getNodeValue(grand);
					} else {
						warnings.add("Node ignored: " + grandName);
					}
				}
				if(!isEmpty(low) && !isEmpty(high)){
					retVal += " " + low + "-" + high;
				} else if(!isEmpty(low)){
					retVal += " " + low;
					warnings.add("Invalid interval for tag " + parentTagName + ". Missing high value (low received " + low + ")");
				} else if(!isEmpty(high)){
					retVal += " " + high;
					warnings.add("Invalid interval for tag " + parentTagName + ". Missing low value (high received " + high + ")");
				} else {
//					warnings.add("Invalid interval for tag " + parentTagName + ". No low and high value");
				}
			} else {
				warnings.add("Node ignored: " + childName);
			}
		}		
		return retVal.trim();
	}
	
	/**
	 * 
	 * @param node
	 * @param sa
	 */
	private void parseSubdivided(Node node, SearchAttributes sa){
		String lot = "";
		String subLot = "";
		String block = "";		
		for(Node child: getChildren(node)){
			String childName = child.getNodeName();
			if("lot".equals(childName)){
				lot += " " + getNodeValue(child);
			} else if("lot_list".equals(childName)){
				String interval = getInterval(childName, child);
				if(!lot.contains(interval)) {
					lot += " " + interval;
				}
			} else if("subLot".equals(childName)){
				subLot += " " + getNodeValue(child);
			} else if("block".equals(childName)){
				block += " " + getNodeValue(child);
			} else if("block_list".equals(childName)){
				String interval = getInterval(childName, child);
				if(!block.contains(interval)) {
					block += " " + interval;
				}
			} else if("section".equals(childName)){
				String section = getNodeValue(child);
				if(section.length()!=0){
					sa.setAtribute(LD_SUBDIV_SEC, section);
				}
			} else if("subdivision".equals(childName)){
				String subdivision = getNodeValue(child);
				if(subdivision.length()!=0){
					sa.setAtribute(LD_SUBDIV_NAME, subdivision);
				}
			} else if("plat".equals(childName)){
				for(Node grand: getChildren(child)){					
					String grandName = grand.getNodeName();
					if("book".equalsIgnoreCase(grandName)){
						sa.setAtribute(LD_BOOKNO, getNodeValue(grand));		
					} else if("page".equalsIgnoreCase(grandName)){
						sa.setAtribute(LD_PAGENO, getNodeValue(grand));
					}
				}								
			} else if("phase".equals(childName)){
				String phase = getNodeValue(child);
				if(phase.length()!=0){
					sa.setAtribute(LD_SUBDIV_PHASE, phase);
				}
			} else if("unit".equals(childName)){
				String unit = getNodeValue(child);
				if(org.apache.commons.lang.StringUtils.isNotBlank(unit)) {
					sa.setAtribute(SearchAttributes.LD_SUBDIV_UNIT, unit.trim());
				}
			} else {
				warnings.add("Node ignored(Subdivided): " + childName);
			}
		}	
		
		
		sa.setAtribute(LD_LOTNO, lot.trim());
		sa.setAtribute(SearchAttributes.LD_SUBLOT, subLot.trim());
		sa.setAtribute(LD_SUBDIV_BLOCK, block.trim());
	}
	
	/**
	 * 
	 * @param node
	 * @param sa
	 */
	private void parseSectional(Node node, SearchAttributes sa){
		for(Node child: getChildren(node)) {
			String childName = child.getNodeName();
			String value = getNodeValue(child);
			if(value.length()==0){
				continue;
			}
			if("section".equals(childName)){
				sa.setAtribute(LD_SUBDIV_SEC, value);
			} else if("township".equals(childName)){
				sa.setAtribute(LD_SUBDIV_TWN, value);
			} else if("range".equals(childName)){
				sa.setAtribute(LD_SUBDIV_RNG, value);
			} else if("quarter".equals(childName)){
				sa.setAtribute(QUARTER_ORDER, value);
			} else {
				warnings.add("Node ignored(Sectional): " + childName);
			}
		}
	}
	/**
	 * Check that the file ids are valid
	 * @param value
	 * @param field
	 */
	private void verifyFileId(String value, String field){
		if(value.length() > 100){
			errors.add(field + " has length greater than 100: " + value);
		}		
		if(!value.matches("[A-Za-z0-9._~-]*")){
			errors.add("ATS will only accept orders which contains letters, numbers, and the characters ._~- in the " + field + "field of AFW.");
			errors.add("Spaces in the " + field + " number field are not accepted.");
		}
	}
	
	/**
	 * 
	 * @param sa
	 * @return
	 */
	public static boolean isTitleDesk(SearchAttributes sa){
		String origin = sa.getAtribute(SEARCH_ORIGIN).toLowerCase();
		return origin.contains("title") && origin.contains("desk");		
	}
	
	/**
	 * 
	 * @param sa
	 * @return
	 */
	public static boolean isStewartOrders(SearchAttributes sa){
		String origin = sa.getAtribute(SEARCH_ORIGIN).toLowerCase();
		return origin.contains("stewart") && origin.contains("orders");		
	}
	
	/**
	 * Verify search attributes, if errors are found, they are added to interval list <code>errors</code> 
	 * @param sa search attributes to be verified
	 * @param ca (not used anymore)
	 */
	private void verifySearchAttributes(SearchAttributes sa, CommunityAttributes ca){
		
		// check state
		if(isEmpty(sa.getAtribute(P_STATE))){
			errors.add("State is missing");			
		}
		
		// check county
		if(isEmpty(sa.getAtribute(P_COUNTY))){
			errors.add("County is missing");			
		}
		
		// check file id after they were  copied if the case between each other	
		if(isEmpty(sa.getAtribute(ORDERBY_FILENO))){
			// verify the TitleDeskOrderId only for Title Desk 
			if(isTitleDesk(sa)){
				String orderId = sa.getAtribute(TITLEDESK_ORDER_ID);
				if(isEmpty(orderId)){
					errors.add("Titledesk order id missing");
				}else if(!orderId.matches("\\d+")){
					errors.add("Titledesk order id not a number:" + orderId);
				}else{
					sa.setAtribute(ORDERBY_FILENO, orderId);
					sa.setAtribute(ABSTRACTOR_FILENO, orderId);
				}
			}else if(isStewartOrders(sa)){
				String orderId = sa.getAtribute(STEWARTORDERS_ORDER_ID);
				if(isEmpty(orderId)){
					errors.add("StewartOrders order id missing");
				}else{
					sa.setAtribute(ORDERBY_FILENO, orderId);
					sa.setAtribute(ABSTRACTOR_FILENO, orderId);
				}
			}else{
				errors.add("Could not acept an order with emty <agent_file_id> and <community_file_id>");
			}
		}
		
		// check file ids to contain only legitimate characters
		verifyFileId(sa.getAtribute(ABSTRACTOR_FILENO), "community_file_id");
		verifyFileId(sa.getAtribute(ORDERBY_FILENO), "agent_file_id");
		
		String prodType = sa.getAtribute(SEARCH_PRODUCT);
		if(StringUtils.isEmpty(prodType)){
			errors.add("Missing product type");
		}
		
		// cleanup names
		warnings.addAll(sa.cleanupNames());

		// cleanup legal
		warnings.addAll(sa.cleanupLegal());
		
		// cleanup address
		warnings.addAll(sa.cleanupAddress());
		
		warnings.addAll(sa.detectTimeShare());
		
		// log warnings
		/* they don't want it
		String info = sa.getAtribute(ADDITIONAL_INFORMATION);			
		if(warnings.size() != 0){
			info += "\nWARNINGS:\n";
			for(String warning: warnings){
				info += warning + "\n";
			}
			sa.setAtribute(ADDITIONAL_INFORMATION, info);
		}
		*/
		
	}
	
	/**
	 * Get full name of user
	 * @return
	 */
	private String getUserFullName(){
		if(user != null){
			try {
				return user.getUserAttributes().getUserFullName();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return "";
	}
	
	/**
	 * Get email of user
	 * @return
	 */
	private String getUserEmail(){
		if(user != null){
			try {
				return user.getUserAttributes().getEMAIL();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return "";
	}
		
	/**
	 * Get community administrator email
	 * @return
	 */
	private String getCommAdminEmail() { 
	    BigDecimal commAdminID = InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentCommunity().getCOMM_ADMIN();
	    UserAttributes commAdmin = UserManager.getUser(commAdminID);
	    return commAdmin.getEMAIL();
	}

	/**
	 * Get email from
	 * @return
	 */
	private String getMailFrom(){
		String from = MailConfig.getMailFrom();
        if(StringUtils.isEmpty(from)){
        	from = getUserEmail();
        }
        return from;
	}
	
	/**
	 * Get body of email
	 * @return
	 */
	private String getEmailBody(){
		
        StringBuilder sb = new StringBuilder();
        
    	sb.append("<b>Application</b>: " + appId);
    	sb.append("<hr/>");
    	
        // add errors
        if(errors.size() != 0){
        	sb.append("<b>Errors</b>:<br/>");
        	for(String error: errors){
        		sb.append("<li>" + error + "</li>");
        	}
        	sb.append("<hr/>");
        	
        }
        
        // add warnings
        if(warnings.size() != 0){
        	sb.append("<b>Warnings</b>:<br/>");
        	for(String warning: warnings){
        		sb.append("<li>" + warning + "</li>");
        	}
        	sb.append("<hr/>");
        }
        

        // add html if present  
        if(!StringUtils.isEmpty(htmlOrder)){
        	sb.append(htmlOrder);
        }
        
        // add XML order if any error
        if(errors.size() != 0 || StringUtils.isEmpty(htmlOrder)){
        	if(!StringUtils.isEmpty(htmlOrder)){
        		sb.append("<hr/>");
        	}
        	if(orderDoc != null && errors.size() != 0 ) {
        		sb.append("<b>XML Order:</b>");
	        	sb.append("<pre>");
	        	sb.append(XmlUtils.createHtml(orderDoc.getFirstChild()));
	        	sb.append("</pre>");
        	} else if(!StringUtils.isEmpty(order)){
        		sb.append("<b>XML Order:</b>");
        		sb.append("<code>");
        		sb.append(StringEscapeUtils.escapeHtml(order));
        		sb.append("</code>");
        	}
        }
        
        return sb.toString();
	}
	
	/**
	 * Send notification email
	 */
	public void sendNotification() {
		try { 
			boolean deliveryNotification = false;
			deliveryNotification = ((InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentUser() != null) 
								&& (InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentUser().getMyAtsAttributes().getReceive_notification() == 1));
			if (deliveryNotification) {
				if (errors.size() == 0) {
					
					// success
					String fileNo = search.getSa().getAtribute("ABSTRACTOR_FILENO") + "/" + search.getSa().getAtribute("ORDERBY_FILENO");
					String from = getMailFrom();
					String to = getCommAdminEmail();
					String bcc = MailConfig.getOrdersEmailAddress() + "," + getUserEmail();
					String subject = fileNo + " - A new order has been received from agent " + getUserFullName() + ". Please schedule it for further processing";
	
					EmailClient email = new EmailClient();
					email.setFrom(from);
					email.addTo(to);
					email.addBcc(bcc);
					email.setSubject(subject);
					email.setContent(getEmailBody(), "text/html");
					email.sendNow();
					
				} else {
					
					// errors
					String from = getMailFrom();
					String to = getUserEmail();
					String cc = MailConfig.getSupportEmailAddress();
					String subject = "ERROR: ";
					if (!StringUtils.isEmpty(commFileId)) {
						subject += commFileId + " ";
					}
					subject += "order not placed";
	
					EmailClient email = new EmailClient();
					email.setFrom(from);
					if (!StringUtils.isEmpty(to)) {
						email.addTo(to);
						email.addCc(cc);
					} else {
						email.addTo(cc);
					}
					email.setSubject(subject);
					email.setContent(getEmailBody(), "text/html");
					email.sendNow();
					
				}
			}
		} catch (Exception e) {
			loggerLocal.error(logPrefix + ": Exception in SendNotification", e);
			try {
				// errors
				String from = getMailFrom();
				String to = getUserEmail();
				String cc = MailConfig.getSupportEmailAddress();
				String subject = "ERROR: ";
				if (!StringUtils.isEmpty(commFileId)) {
					subject += commFileId + " ";
				}
				subject += "order not placed";

				EmailClient email = new EmailClient();
				email.setFrom(from);
				if (!StringUtils.isEmpty(to)) {
					email.addTo(to);
					email.addCc(cc);
				} else {
					email.addTo(cc);
				}
				email.setSubject(subject);
				email.setContent(getEmailBody(), "text/html");
				email.sendNow();
				
			} catch (Exception exc) {
				loggerLocal.error(logPrefix + ": Exception in SendNotification in the inner \"try\"", exc);
			}
		}
	}
}

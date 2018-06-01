package ro.cst.tsearch.servlet;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import mailtemplate.MailTemplateUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.bean.OrderToUpdateResult;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.CountyCommunityManager;
import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.SearchUpdateMapper;
import ro.cst.tsearch.database.transactions.UserInfoTransaction;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.InvalidEmailOrderException;
import ro.cst.tsearch.log.AtsStandardNames;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.titledocument.abstracts.FidelityTSD;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.SharedDriveUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.XmlUtils;

import com.oreilly.servlet.ParameterParser;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.SsfDocumentMapper;
import com.stewart.ats.tsrindex.client.CertificationDate;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn;

@SuppressWarnings("serial")
public class ValidateInputs extends BaseServlet
{
	private static final Category logger = Logger.getLogger(ValidateInputs.class);
	protected static final DecimalFormat format = new DecimalFormat("#,##0.00");
	
	//B 4069
	private static Map<String, String> attributes = new HashMap<String, String>(){{
		put(SearchAttributes.P_STREETNAME, "Street Name");
		put(SearchAttributes.P_STREETNO, "Street Number");
		put(SearchAttributes.P_STREETDIRECTION, "Street Direction");
		put(SearchAttributes.P_STREET_POST_DIRECTION, "Street Post Direction");
		put(SearchAttributes.P_STREETSUFIX, "Street Sufix");
		put(SearchAttributes.P_STREETUNIT, "Street Unit");
		put(SearchAttributes.P_IDENTIFIER_TYPE, "Property Type");
		put(SearchAttributes.P_CITY, "City");
		put(SearchAttributes.P_ZIP,"Zip");
		put(SearchAttributes.LD_PARCELNO, "Parcel ID");
		put(SearchAttributes.LD_INSTRNO, "Instrument Number");
		put(SearchAttributes.LD_BOOKPAGE, "Book/Page");
		put(SearchAttributes.LD_LOTNO, "Lot");
		put(SearchAttributes.LD_SUBLOT, "SubLot");
		put(SearchAttributes.LD_SUBDIV_NAME, "Subdivision Name");
		put(SearchAttributes.LD_SUBDIV_UNIT, "Subdivision Unit");
		put(SearchAttributes.LD_SUBDIV_SEC, "Township Section");
		put(SearchAttributes.LD_SECTION, "Subdivision Section");
		put(SearchAttributes.LD_SUBDIV_PHASE, "Phase");
		put(SearchAttributes.LD_SUBDIV_BLOCK, "Block");
		put(SearchAttributes.LD_SUBDIV_TRACT, "Tract");
		put(SearchAttributes.LD_SUBDIV_TWN, "Township");
		put(SearchAttributes.LD_SUBDIV_RNG, "Range");
		put(SearchAttributes.LD_BOOKNO, "Plat Book");
		put(SearchAttributes.LD_PAGENO, "Plat Page");
		put(SearchAttributes.QUARTER_ORDER, "Quarter Order");
		put(SearchAttributes.QUARTER_VALUE, "Quarter Value");
		put(SearchAttributes.ARB, "Arb");
		put(SearchAttributes.LD_ABS_NO, "Abs Number");
	}};
	
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, BaseException
	{
		
		HttpSession session = request.getSession(true);
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		
		Search global = (Search) currentUser.getSearch(request);
		SearchAttributes sa = global.getSa();
		int opCode = -1;
		try {
			opCode = Integer.parseInt(request.getParameter(TSOpCode.OPCODE));
		}catch (Exception e) {}

		DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(global.getID(),currentUser.getUserAttributes().getID().longValue(), DBManager.CHECK_OWNER, false);
    	
		if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {
	
		    String errorBody = searchAvailable.getErrorMessage();
		    	    		
			request.setAttribute(RequestParams.ERROR_BODY, errorBody);
			request.getRequestDispatcher(URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS)
				.forward(request, response);
			return;
		}
		if( sa == null) {
			request.setAttribute(RequestParams.ERROR_BODY, "An error appeared while validating search (Invalid search attributes).");
			request.getRequestDispatcher(URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS)
				.forward(request, response);
			
			return;
		}
		
		String fowardLink = request.getParameter(RequestParams.FORWARD_LINK);
		fowardLink = fowardLink==null?URLMaping.StartTS:fowardLink;
				
		if (opCode == TSOpCode.VALIDATE_INPUTS_ONLY_TO_SEND_EMAIL) {
			request.setAttribute(TSOpCode.OPCODE, Integer.valueOf(opCode));
		}
		
		String resetForm = request.getParameter("resetform");
		if(resetForm != null && Integer.parseInt(resetForm) == 1) // dupa ce s-a dat "clear form", se reia de la 0 ciclul de searchuri
		{
			global.searchCycle = 0;
		}

		int searchType = 0;
        try
        {
            searchType = Integer.parseInt(request.getParameter("searchtype"));
        }catch( NumberFormatException nfe ) {}
        global.setSearchType( searchType );		
        
        
        int numberOfValidatedOwners = saveSearchAttributes(request, global);
        
		DBManager.getTransactionTemplate().execute(new UserInfoTransaction(global));

		if(changeAgentRequested(request)){
			
			//	numai la operatia de schimbare a agentului are voie sa ia info din BD referitoare la agent
			
			global.setAllowGetAgentInfoFromDB(true);
			changeAgent( currentUser, global, request );
			
			Log.sendEmail2("Change Of Agent In ValidateInputs", "Change Of Agent In ValidateInputs so do NOT delete that");
			
			sendRedirect(request, response, AppLinks.getBackToSearchPageHref(global.getSearchID()));
			return;
		}
		
		updateUserAndAgentAttributes(request, currentUser, sa);
		
		if (sa != null && !SearchAttributes.DATE_DOWN.equalsIgnoreCase(request.getParameter(SearchAttributes.ADDITIONAL_SEARCH_TYPE))) {
			sa.setCertificationDate();
		}

		String originalValueOfIS_UPDATED_ONCE = sa.getAtribute(SearchAttributes.IS_UPDATED_ONCE);
		if ("".equals(fowardLink)){//when changing product to UPdate, to not fill yet the search page with criteria from orig search
			//sa.setAtribute(SearchAttributes.IS_UPDATED_ONCE, "TRUE");
			if ("".equals(sa.getAtribute(SearchAttributes.IS_PRODUCT_CHANGED_ONCE))){
				sa.setAtribute(SearchAttributes.IS_PRODUCT_CHANGED_ONCE, "TRUE");
			}
		}
		
		if (changeStateRequested(request)) {
			String orderTS = request.getParameter("orderTS");
			if ("1".equals(orderTS))
				response.sendRedirect(AppLinks.getBackToOrderPageHref(global.getSearchID()));
			else
			    response.sendRedirect(AppLinks.getBackToSearchPageHref(global.getSearchID()));
			try {
				HashCountyToIndex.setSearchServer(request, global, false);
				//global.resetSearchStatus();			
			} catch(BaseException be) {
				checkCountyImpl(request,global);
			}
			return;
		}
		
		if(!StringUtils.isEmpty(request.getParameter(SearchAttributes.ADDITIONAL_SEARCH_TYPE))) {
			sa.setAtribute(SearchAttributes.ADDITIONAL_SEARCH_TYPE, request.getParameter(SearchAttributes.ADDITIONAL_SEARCH_TYPE));
		}
		
		//daca nu am resetat searchul si sunt la primul ciclu de search, salvez 'search page' 
		int searchTypeValue = 0;
		try{
			searchTypeValue = Integer.parseInt(request.getParameter("searchtype"));
		}catch( Exception e ){
			
		}
		
		//check if middle name is missing in original order
		//and if we have poor data to search with 
		setSearchWarningFlags(global, sa);
    	//done checking if middle name is missing in original search order
    	
		saveSearchFile(sa, global, currentUser);
		sa.setAtribute(SearchAttributes.IS_UPDATE_OF_A_REFINANCE, "false");
		
		checkIfUpdate(request, global, opCode);
        
        checkIfRefinance(request, global);
        
        checkIfDateDown(request,global);
        
        String legalDescriptionStatus = sa.getAtribute(SearchAttributes.LEGAL_DESCRIPTION_STATUS);
        if ("0".equals(legalDescriptionStatus)) {
        	//nu a fost actualizat explicit, actualizam noi acum
        	       	
        	String legalDescription = TemplateBuilder.getDefaultLegalDescription(global, null);
        	if(!sa.getLegalDescription().isEdited()){
        		sa.getLegalDescription().setLegal(legalDescription);
        	}
        	
        	DBManager.setSearchLegalDescription(global, legalDescription, 0);
        }
       
		try {
			
			if ("2".equals(request.getParameter("searchtype")) || // interactive search
                "0".equals(request.getParameter("searchtype"))) // automatic search
			{
				boolean isParentSiteClicked = false;
				if("2".equals(request.getParameter("searchtype")) && fowardLink.equals(URLMaping.StartTS))
					isParentSiteClicked = true;
				HashCountyToIndex.setSearchServer(request, global, isParentSiteClicked);
					
			}
		} catch(BaseException be) {
			checkCountyImpl(request,global);
		}
		
		if(StringUtils.isEmpty(request.getParameter(SearchAttributes.ABSTRACTOR_FILENO)) &&
				StringUtils.isEmpty(request.getParameter(SearchAttributes.ORDERBY_FILENO))) {
			request.setAttribute(RequestParams.ERROR_BODY, 
					"Please enter the Abstractor File ID!");
			
			request.getRequestDispatcher(URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS  + 
					"&" + RequestParams.ERROR_TYPE + "=" + TSOpCode.NO_FILEID_ERROR)
				.forward(request, response);

			return;
		}
		
		if(isSearchStarted(request) && global.isCountyImplemented()) {
			global.setSearchStarted(true);
		} else {
			global.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
		}
		String continueSearch = request.getParameter(RequestParams.CONTINUE_SEARCH);
		String goBackOneLevel = request.getParameter(RequestParams.GO_BACK_ONE_LEVEL);
		
		
		if(("true".equalsIgnoreCase(request.getParameter("searchstarted"))&&(("0".equals(request.getParameter("searchtype")))))){
			if ("true".equals(goBackOneLevel)){
				ASMaster.startSearch(global, ASMaster.START_NEW, continueSearch, goBackOneLevel, currentUser);
			}
			else{
//				ASMaster.startSearch(global, ASMaster.JOIN_EXISTING, continueSearch, goBackOneLevel);
				if (sa.isDataSource()) {
					//abort any other automatic search if an automatic data source is performed
					ASMaster.startSearch(global, ASMaster.ABORT_EXISTING, continueSearch, goBackOneLevel, currentUser);
				} else {
					ASMaster.startSearch(global, ASMaster.JOIN_EXISTING, continueSearch, goBackOneLevel, currentUser);
				}
			}
		}
		
		
		UserAttributes agent= global.getAgent();
		try {
			
			if( fowardLink.contains("startCreatingTsr") ) {
				
					String error = "";
					
					if(agent==null) {
						error = "Please select an Agent!<br>Please check the TSD Template in Agent's profile!";
					}
					
					if(StringUtils.isEmpty(global.getAbstractorFileNo())) {
						if(StringUtils.isNotEmpty(error)) {
							error += "<br>Please set the Abstractor File Number!";
						} else {
							error = "Please set the Abstractor File Number!";
						}
					}
					
					if(agent!=null){
						try{
							List<CommunityTemplatesMapper>userTemplates = UserUtils.getUserTemplates(agent.getID().longValue(),global.getProductId());
					    	if(!FidelityTSD.containsTSDTemplate(userTemplates)){
					    		if(StringUtils.isNotEmpty(error)) {
									error += "<br>Please check the TSD Template in Agent's profile!";
								} else {
									error = "Please check the TSD Template in Agent's profile!";
								}
					    	}
						}
						catch(Exception e){
							
						}
					}
					
					if(!error.isEmpty()) {
						fowardLink = fowardLink.replaceAll("action=([a-zA-Z0-9])+","error="+URLEncoder.encode(error,"UTF-8"));
					}else {
						fowardLink =URLMaping.path +  "/jsp/newtsdi/tsdindexpage.jsp?searchId="+sa.getSearchId()+"&userId="+currentUser.getUserAttributes().getID()+"&startCreatingTsr=1"+(fowardLink.contains("removeInvoicePage")?"&removeInvoicePage=true":"");
						
						response.sendRedirect(encodeUrl(fowardLink));
						return;
					}
				}
			String id = String.valueOf(System.nanoTime()) + "_extraInfo"; //B 4069
			SearchLogFactory logFactory = SearchLogFactory.getSharedInstance();
			SearchLogPage searchLogPage = logFactory.getSearchLogPage(global.getSearchID());
			
			
			if (fowardLink.contains("/TSD")) {
				searchLogPage.addChangePageMessage(currentUser.toString(), 
						AtsStandardNames.PAGE_NAME_SEARCH_PAGE,AtsStandardNames.PAGE_NAME_TSR_INDEX);
				SearchLogger.info("</div><div id='" + id + "' style=\"display:none\">The user <b>" 
						+ ua.getAttribute(1).toString()
						+ "</b> was passing from Search Page to TSR Index at "
		    			+ SearchLogger.getCurDateTimeCST()  +".<BR></div><div>", sa.getSearchId());
			} else if (fowardLink.contains("/StartTS")) {
				searchLogPage.addChangePageMessage(currentUser.toString(), AtsStandardNames.PAGE_NAME_SEARCH_PAGE,
						AtsStandardNames.PAGE_NAME_PARENT_SITE);
				SearchLogger.info("</div><div id='" + id + "' style=\"display:none\">The user <b> " 
						+ ua.getAttribute(1).toString()
						+ "</b> was passing from Search Page to Parent Site at "
		    			+ SearchLogger.getCurDateTimeCST()  +".<BR></div><div>", sa.getSearchId());
			} else if( fowardLink.contains("clearForm") ) {
				sa.clearOwners();
				sa.getSearchPageManualOwners().clear();
				sa.getSearchPageManualBuyers().clear();
				sa.getSearchPageManualFields().clear();
				
				SearchLogger.info("</div><div>All the information from <b>Search Page was deleted</b> "+SearchLogger.getTimeStamp(sa.getSearchId())
						+".<BR><div>", sa.getSearchId());
				searchLogPage.deleteAllOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE);
				
				
			}
			if ("".equals(fowardLink)){//after the product is changed in UPDATE, the search page can be filled 
										//with criteria when the search is starting or going to tsri
				fowardLink = URLMaping.StartTSPage;
				sa.setAtribute(SearchAttributes.IS_UPDATED_ONCE, originalValueOfIS_UPDATED_ONCE);
			}
		}catch(Throwable t) {}
		
		request.getRequestDispatcher(fowardLink).forward(request, response);
		
	}
	
	private void checkIfDateDown(HttpServletRequest request, Search global) {     
		if(global.getSa().isDateDown()) {
			try {
				Calendar defaultSdate = Calendar.getInstance();                                                
		    	defaultSdate.setTime(global.getSa().getStartDate());  
		        setStartDateForUpdate(global.getSa(), defaultSdate.getTime(), global.getSa());
		        global.getSa().setAtribute(SearchAttributes.TODATE, SearchAttributes.DEFAULT_DATE_PARSER.format(new Date()));
		        DocumentsManager manager = (DocumentsManager) global.getDocManager();
		        try{ 
					manager.getAccess();
					manager.setEndViewDate(new Date());
				}catch(Exception e) {
					e.printStackTrace();
				}
				finally{
					manager.releaseAccess();
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	//for some warnings (missing middle name in initial search order, poor data in initial order) I set some flags only when the order is submitted for the first time
	//If the flags are not set, it means the order was submited before this feature was implemented, and I calculate the flags based on the xml saved in orderfile
	public static void setSearchWarningFlags(Search global, SearchAttributes sa){

		String poor_data = "";
		PartyI owners = new Party(PType.GRANTOR);; 
		PartyI buyers = new Party(PType.GRANTEE);;
		String pin = "", address_no = "", address_street = "";
//		File searchOrder = new File(global.getSearchDir() + "orderFile.html");
		Boolean[] tempMissingInitial = (Boolean[])sa.getObjectAtribute(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING);
		boolean recalc = false;
		
//		if(!searchOrder.exists()) {
//			if(DBReports.hasOrderInDatabase(global.getID())) {
//    			byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(global.getID(), FileServlet.VIEW_ORDER, false);
//    			if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0) {
//    				try {
//						FileUtils.writeByteArrayToFile(searchOrder, orderFileAsByteArray);
//					} catch (IOException e) {
//						logger.error("Cannot force log from database for searchId " + global.getID(), e);
//					}
//    			}
//    		}
//		}
		
		byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(global.getID(), FileServlet.VIEW_ORDER, false);
		if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0) {
			
//    	if (searchOrder.exists()){
    		//this is not the first time this form was submitted, if the flags are not set, I have to compute them based on the order file
    		poor_data = sa.getAtribute(SearchAttributes.POOR_SEARCH_DATA);
    		
    		if ((tempMissingInitial[0] == null || poor_data.equals("")) && tempMissingInitial[1] == null){
    			//read the initial search from xml (this means that searchAttributes were saved before I added the new attribute)
    			Document initialOrder = global.getInitialOrderXMLFromOrderFile();
    			if (initialOrder != null){
	    			Node n;
	    			NodeList nl = initialOrder.getElementsByTagName("person");
	    			for(int x = 0; x < nl.getLength(); x++){
	    				n = nl.item(x);
	    				String p[];
	    				if (n.getParentNode().getNodeName().equals("owners")){
    						p = parsePerson(n);
    						owners.add(new Name(p[0], p[1], p[2]));
	    				}
	    				if (n.getParentNode().getNodeName().equals("buyers")){
    						p = parsePerson(n);
    						buyers.add(new Name(p[0], p[1], p[2]));
	    				}
	    			}  
	    			//search for street number and name
	    			nl = initialOrder.getElementsByTagName("street");
	    			for(int x = 0; x < nl.getLength(); x++){
	    				n = nl.item(x);
	    				for (Node parseStreet : XmlUtils.getChildren(n)){
	    					if (parseStreet.getNodeName().equals("number_list") 
	    							|| parseStreet.getNodeName().equals("number")){
	    						address_no = "there is a number set";
	    					}
	    					else if (parseStreet.getNodeName().equals("name")){
	    						address_street = "there is a street name";
	    					}
	    				}
	    			}
	    			//property Id
	    			nl = initialOrder.getElementsByTagName("property_id");
	    			if (nl.getLength() > 0) pin = "there is a pin";
	    			nl = initialOrder.getElementsByTagName("company");
	    			for (int x=0; x<nl.getLength(); x++){
	    				n = nl.item(x);
	    				if (n.getParentNode().getNodeName().equals("owners")) {
	    					owners.add(new Name("", "", "there is a company"));
	    				}
	    			}
	    		}
    			recalc = true;
    		}

    	}
    	if (!(orderFileAsByteArray != null && orderFileAsByteArray.length != 0) || 
    			(tempMissingInitial[0] == null && tempMissingInitial[1] !=null&&tempMissingInitial[1]==false )){
    		if (tempMissingInitial[1] == null){
    			owners = sa.getSearchPageManualOwners();
    			buyers = sa.getSearchPageManualBuyers();
    		} else {
    			owners = sa.getOwners();
    			buyers = sa.getBuyers();
    		}
    		address_no = sa.getAtribute(SearchAttributes.P_STREETNO);
    		address_street = sa.getAtribute(SearchAttributes.P_STREETNAME);
    		pin = sa.getAtribute(SearchAttributes.LD_PARCELNO);
    		recalc = true;
    	}
    	//initial missing
	    if (recalc){
	    	tempMissingInitial[0] = partyMissingInitial(owners);
	    	tempMissingInitial[1] = tempMissingInitial[0];
	    	tempMissingInitial[2] = partyMissingInitial(buyers);
	    	tempMissingInitial[3] = tempMissingInitial[0];    	
	        //poor data
	        int criteriaCount = 0;
	        if (pin.length() > 0){
	        	criteriaCount++;
	        }
	        if ((address_no+address_street).length() > 0){
	        	criteriaCount++;
	        }
	        if (owners.getNames().size() > 0){
	        	criteriaCount++;
	        }
	        if (poor_data.equals("")){
	        	if (criteriaCount < 2){
	        		poor_data = "true";
	        	} else {
	        		poor_data = "false";
	        	}
	        }
	    	for (int ij = 0; ij< 4; ij++){
	    		if (tempMissingInitial[ij] == null){
	    			tempMissingInitial[ij] = false;
	    		}
	    	}
	    	sa.setObjectAtribute(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING, tempMissingInitial);
	    	sa.setAtribute(SearchAttributes.POOR_SEARCH_DATA, poor_data);
	    }
	}
	
	/**
	 * checks in a PartyI object if there are persons names 
	 * <br> that don't have middle name
	 * @param o - Party object
	 * @return true if there is a person without middle name
	 * <br>false if there is no person with missing middle name
	 */
	public static boolean partyMissingInitial(PartyI o){
		if (o.getNames() != null){
			Iterator<NameI> i = o.getNames().iterator();
			while (i.hasNext()){
				NameI n = i.next();
				if (n.getFirstName().length() > 0 && n.getMiddleName().length() == 0){
					return true;
				}
			}
 		}
		return false;
	}
	
	public static  String[] parsePerson(Node n){
		String p[] = new String[]{"", "", ""};
		for (Node parsePerson : XmlUtils.getChildren(n)){
			if (parsePerson.getNodeName().equals("first")){
				p[0] = XmlUtils.getNodeValue(parsePerson);
			}
			if (parsePerson.getNodeName().equals("middle")){
				p[2] = XmlUtils.getNodeValue(parsePerson);
			}
			if (parsePerson.getNodeName().equals("last")){
				p[1] = XmlUtils.getNodeValue(parsePerson);
			}
		}
		return p;
	}
    
     /*
    Selers (owners) se copiaza si la Buyers daca acesti 
    lipsesc si invers, Sellers se copiaza si la Buyers daca acestia lipsesc.
 */
    public static void checkIfRefinance(HttpServletRequest request, Search global) throws BaseException     
    {
        
        int searchProduct = -1;
        try {
            searchProduct = Integer.parseInt(global.getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT));
        } catch (NumberFormatException e1) {
            e1.printStackTrace();
        }
        if ( searchProduct != SearchAttributes.SEARCH_PROD_REFINANCE){
        	return;
        }
     
        PartyI owners = global.getSa().getOwners();
        PartyI buyers = global.getSa().getBuyers();
        
        Set<NameI> ownerNames = owners.getNames();
        Set<NameI> buyerNames = buyers.getNames();
        
        // if buyers == empty then buyers = owners
       if(buyerNames.size() == 0 && ownerNames.size() > 0){
        	for(NameI name: ownerNames){
        		
        		name.setValidated(true);
        		buyers.add(name);
        	}
        } else if(ownerNames.size() == 0 && buyerNames.size() > 0){// if owners == empty then owners = buyers
        	for(NameI name: buyerNames){
        		owners.add(name);
        	}
        } else {
	        if(ownerNames.size() > 0){
	        	buyers.clear();
	        	for(NameI name: ownerNames){
	        		
//	        		name.setValidated(true);
	        		buyers.add(name);
	        	}
	        }
        }
  	
        String value = global.getSa().getAtribute(SearchAttributes.IGNORE_MNAME);
        if(!"true".equalsIgnoreCase(value)){
        	value = global.getSa().getAtribute(SearchAttributes.IGNORE_MNAME_BUYER);
        }
        
        if("true".equalsIgnoreCase(value)){
        	global.getSa().setAtribute(SearchAttributes.IGNORE_MNAME, "true");
        	global.getSa().setAtribute(SearchAttributes.IGNORE_MNAME_BUYER, "true");
        }
        
    }
    
    public static void checkIfOriginalSearchIsRefinance(Search oldSearch, Search global) throws BaseException     
    {
        
    	if (oldSearch == null){
    		return;
    	}
        int searchProduct = -1;
        try {
            searchProduct = Integer.parseInt(oldSearch.getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT));
        } catch (NumberFormatException e1) {
            e1.printStackTrace();
        }
        if ( searchProduct != SearchAttributes.SEARCH_PROD_REFINANCE){
        	global.getSa().setAtribute(SearchAttributes.IS_UPDATE_OF_A_REFINANCE, "false");
        	return;
        }
        global.getSa().setAtribute(SearchAttributes.IS_UPDATE_OF_A_REFINANCE, "true");
        
        PartyI owners = global.getSa().getOwners();
        PartyI buyers = global.getSa().getBuyers();
                
        Set<NameI> ownerNames = owners.getNames();
        Set<NameI> buyerNames = buyers.getNames();
        
        for(NameI nameB: buyerNames){
        	for(NameI name: ownerNames){
        		if (nameB.equals(name)){
        			buyers.remove(nameB);
        			if (name.isValidated()){
        				nameB.setValidated(true);
        			} else {
        				nameB.setValidated(false);
        			}
        			buyers.add(nameB);
        		}
        	}
        }
    }
    
	public static void saveSearchFile(SearchAttributes sa, Search global, User currentUser)
	{
	    try {
	    	
	    	boolean hasOrder = false;
	    	
	    	String fullPath = SharedDriveUtils.getSharedLogFolderForSearch(global.getID());
			fullPath += "orderFile.html";
			try {
				hasOrder = new File(fullPath).exists();
			} catch (Exception e) {
				System.err.println("Check samba failed for path " + fullPath);
				e.printStackTrace();
				Log.sendExceptionViaEmail(
						MailConfig.getMailLoggerToEmailAddress(), 
						"Order File Check on Samba failed", 
						e, 
						"SearchId used: " + global.getID() + ", path used: " + fullPath);
			}
	    	
	    	if(!hasOrder) {
	    		File searchOrder = new File(global.getSearchDir() + "orderFile.html");
		    	if (searchOrder.exists()) {
		    		hasOrder = true;
		    	} else {
		    		if(DBReports.hasOrderInDatabase(global.getID())) {
		    			byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(global.getID(), FileServlet.VIEW_ORDER, false);
		    			if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0) {
		    				FileUtils.writeByteArrayToFile(searchOrder, orderFileAsByteArray);
		    			}
		    			hasOrder = true;
		    		}
		    	}	
	    	}
	    	
	    	if(!hasOrder) {	
		    	// fill search order and save it
		    	MailTemplateUtils.fillAndSaveSearchOrder(global, sa, currentUser);
	    	}
	    	
	    } catch (Exception e) 
	    {
		    e.printStackTrace();
		}
	}
	
	public static boolean isSearchStarted(HttpServletRequest req) {
		String searchStarted = req.getParameter(RequestParams.SEARCH_STARTED);
		return !StringUtils.isStringBlank(searchStarted)
			&& searchStarted.equals("true");
	}

	public static void checkCountyImpl(HttpServletRequest req, Search global) {
		if (isSearchStarted(req)) {
			for (int i = 0; i <= 3; i++) {
				global.setCitychecked(i);
				try {
					HashCountyToIndex.setSearchServer(global, i);
				} catch (BaseException be) {
					logger.debug(
						"CityChecked =["
							+ global.getCitychecked()
							+ "] :: "
							+ "P1 =["
							+ global.getP1()
							+ "] :: "
							+ "P2 =["
							+ global.getP2()
							+ "] =>ImplNA");
					global.updateSearchStatus(
						global.getCitychecked(),
						Search.SERVER_NA_MSG);
					continue;
				}
				break;
			}
		}
	}
	
	public static void checkCountyImpl(Search global) {
			for (int i = 0; i <= 3; i++) {
				global.setCitychecked(i);
				try {
					HashCountyToIndex.setSearchServer(global, i);
				} catch (BaseException be) {
					logger.debug(
						"CityChecked =["
							+ global.getCitychecked()
							+ "] :: "
							+ "P1 =["
							+ global.getP1()
							+ "] :: "
							+ "P2 =["
							+ global.getP2()
							+ "] =>ImplNA");
					global.updateSearchStatus(
						global.getCitychecked(),
						Search.SERVER_NA_MSG);
					continue;
				}
				break;
		}
	}
	

	private boolean changeStateRequested(HttpServletRequest request) {
		String change = request.getParameter("changeState");
		return (change != null && change.equals("1"));
	}

	private void updateUserAndAgentAttributes(HttpServletRequest request, User currentUser, SearchAttributes sa) {
		try
		{					
			updateAgentAttributes(currentUser.getUserAttributes(), request, sa);
			updateUserAttributes(currentUser.getUserAttributes() ,sa);
		}
		catch (DataException e)
		{logger.error( "Following error occured while updating user: " + currentUser.getUserAttributes().getFIRSTNAME() + currentUser.getUserAttributes().getLASTNAME() +
							"/n Error: " + e.getMessage() );}
		catch (BaseException e)
		{logger.error( "Following error occured while updating user: " + currentUser.getUserAttributes().getFIRSTNAME() + currentUser.getUserAttributes().getLASTNAME() +
									"/n Error: " + e.getMessage() );}
	}
	private void changeAgent(User currentUser, Search global, HttpServletRequest request) {
		String agentId = (String)request.getParameter("agentCombo");
		
		SearchAttributes sa = global.getSa();

		if(!agentId.equals("-1")){
			//old versions
			UserAttributes aa = UserManager.getUser(new BigDecimal(agentId));
			currentUser.setUserAgentClient(aa);			
			sa.setOrderedBy(aa);
			try {
				updateUserAttributes(currentUser.getUserAttributes() ,sa);
				global.setAgent(aa);
			}
			catch (DataException e) {
				logger.error( "Following error occured while updating user: " + 
						currentUser.getUserAttributes().getFIRSTNAME() + 
						currentUser.getUserAttributes().getLASTNAME(), e );
			}
			catch (BaseException e){
				logger.error( "Following error occured while updating user: " + 
						currentUser.getUserAttributes().getFIRSTNAME() + 
						currentUser.getUserAttributes().getLASTNAME(), e );
			}				
		} else{
			currentUser.setUserAgentClient(null);
			currentUser.getUserAttributes().setAGENTID(new BigDecimal(-1));
			global.setAgent(null);
			sa.resetOrderedBy();
		}
	}
	
	
	private boolean changeAgentRequested(HttpServletRequest request) {
		String agentId = (String)request.getParameter("agentCombo");
		String changeAgent =(String)request.getParameter("changeAgent"); 

		return (changeAgent!=null&& changeAgent.equals("1") && agentId!=null &&!agentId.equals(""));
	}
	
	private static final String ABSTR_FID_NOT_FILLED_MESSAGE = "You must fill in the Abstractor File ID to request un update";
	private static final String UPDATE_FILE_NOT_FOUND_MESSAGE_WITH_AGENT_FILE_ID = 
			"No file found to update for Abstractor File ID: [{0}] and Agent File ID: [{1}]";
	private static final String UPDATE_FILE_NOT_FOUND_MESSAGE_WITH_AGENT_AND_GUID_FILE_ID = 
			"No file found to update for Abstractor File ID: [{0}] and Agent File ID: [{1}] or To Update GUID: [{2}]";
	private static final String UPDATE_FILE_NOT_FOUND_MESSAGE = 
			"No file found to update for Abstractor File ID: [{0}]";
	private static final String UPDATE_FILE_NOT_FOUND_AND_GUID_MESSAGE = 
			"No file found to update for Abstractor File ID: [{0}] or To Update GUID: [{1}]";
	//private static final String MORE_THAN_ONE_FILE_FOUND_MESSAGE = "More that one file with the search criteria found to update for Abstractor File ID: ";
	private static final String PROBLEM_LOADING_OLD_SEARCH_MESSAGE = "Problem loading old search (search to be updated is undefined) for Abstractor File ID: ";
	
	public static void checkIfUpdate(Search global, int opCode, HttpServletRequest request) throws InvalidEmailOrderException, BaseException{
		checkIfUpdate(global, opCode, request, SearchAttributes.ABSTRACTOR_FILENO);
	}
	
	/**
	 * wrapper that throws InvalidEmailOrderException for the exceptions
	 * that needed to be treated separately for emailOrders
	 * @param global
	 * @param opCode
	 * @param request 
	 * @throws InvalidEmailOrderException
	 */
	public static void checkIfUpdate(Search global, int opCode, HttpServletRequest request, String key) throws InvalidEmailOrderException, BaseException{
		try{
			checkIfUpdate(request, global, opCode, key);
		}catch (BaseException be){
			String msg = be.getMessage();
			if(msg.startsWith(ABSTR_FID_NOT_FILLED_MESSAGE)){
				InvalidEmailOrderException e = new InvalidEmailOrderException(be.getMessage());
				e.setUpdNoFID(true);
				throw e;				
			}
			if(msg.startsWith("No file found to update for Abstractor File ID:")){	
				InvalidEmailOrderException e = new InvalidEmailOrderException(be.getMessage());
				e.setUpdWrongFID(true);
				if(ServerConfig.isCheckAgentFileNoInUpdate()) {
					e.setFidAgent(global.getSa().getAtribute(SearchAttributes.ORDERBY_FILENO));
				}
				e.setFid(global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO));
				
				throw(e);
			}
			throw be;
		}
	}
	
	public static void checkIfUpdate(HttpServletRequest request, Search global, int opCode) throws BaseException {
		checkIfUpdate(request, global, opCode, SearchAttributes.ABSTRACTOR_FILENO);
	}
	
	// code to check whether or not an update is requested,
	// and if yes, if the update is correct.
	public static void checkIfUpdate(HttpServletRequest request, Search global, int opCode, String key) throws BaseException {
		SearchAttributes sa = global.getSa();
		if (global.isSearchProductTypeOfUpdate() && global.getParentSearchId() == Search.NO_UPDATED_SEARCH) {

			OrderToUpdateResult orderToUpdate = getOrderToUpdate(global, key);

			// when opening an update and his parent search no longer exists
			if (orderToUpdate.getToUpdateSearchInfo() == null && global.getParentSearchId() != Search.NO_UPDATED_SEARCH) {
				if (global.getOrigSA() != null) {
					sa.setAtribute(SearchAttributes.FROMDATE, global.getOrigSA().getAtribute(SearchAttributes.FROMDATE));
				} else {
					if (global.getTSROrderDate() != null) {
						sa.setAtribute(SearchAttributes.FROMDATE, SearchAttributes.DEFAULT_DATE_PARSER.format(global.getTSROrderDate()));
					}
				}

			} else {
				// just what we need: One search that has all test fields OK
				global.setTsuNo(orderToUpdate.getFinishedUpdates() + 1);

				sa.setAtribute(SearchAttributes.FROMDATE, SearchAttributes.DEFAULT_DATE_PARSER.format(orderToUpdate.getToUpdateSearchInfo().getTsrCreated()));
				Search oldSearch = SearchManager.getSearchFromDisk(orderToUpdate.getToUpdateSearchInfo().getSearchId());

				if (oldSearch != null) {
					
					if ((oldSearch.isProductType(Products.UPDATE_PRODUCT) || oldSearch.isProductType(Products.FVS_PRODUCT)) 
							&& orderToUpdate.getFinishedUpdates() == 0) {
						global.setTsuNo(oldSearch.getTsuNo() + 1);
					}
					
					SearchAttributes origSa = oldSearch.getSa();
					global.setParentSearchId(origSa.getSearchId());
					
					if (global.isProductType(SearchAttributes.SEARCH_PROD_UPDATE)) {// 9305 Comment 4
						List<SsfDocumentMapper> sffDocumentWithType = oldSearch.getSffDocumentWithType(DocAdminConn.TEMPLATE_INDEX_TYPE);
						if(sffDocumentWithType != null) {
							SsfDocumentMapper tsdOldSearch = null;
							for (SsfDocumentMapper ssfDocumentMapper : sffDocumentWithType) {
								if("text/html".equals(ssfDocumentMapper.getMimeType())) {
									tsdOldSearch = ssfDocumentMapper;
									//do not break because on reopen (for old searches) previous files were not cleaned and the correct TSD is the last one
								}
								
							}
							if(tsdOldSearch != null) {
								global.setTsriParentLink(tsdOldSearch.getSsfLink());
							}
						}
						
//						long parentSearchId = global.getParentSearchId();
//						if (parentSearchId != Search.NO_UPDATED_SEARCH) {
//							try {
//								Map<String, Object> tsriParentLinkMap = DBSearch.getTsriLinkInfoFromDB(parentSearchId);
//								String tsriParentLink = org.apache.commons.lang.StringUtils.defaultString((String) tsriParentLinkMap
//										.get(DBConstants.FIELD_SEARCH_TSRI_LINK));
//								global.setTsriParentLink(tsriParentLink);
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}
					}
					
					if (sa.isFVSUpdate()) {
						global.setParentSA(origSa);
						sa.getReviewCheckList(origSa);
					}

					Calendar defaultSdate = Calendar.getInstance();
					defaultSdate.setTime(orderToUpdate.getToUpdateSearchInfo().getTsrCreated());
					defaultSdate.add(Calendar.DATE, -7); // go back N days

					Calendar dateNow = Calendar.getInstance();

					int allowUpdatePeriod = ServerConfig.getAllowUpdatePeriod();

					if (orderToUpdate.getParentSearchInfo() != null) {
						Calendar dateTSRCreated = Calendar.getInstance();
						dateTSRCreated.setTime(orderToUpdate.getParentSearchInfo().getTsrCreated());

						dateNow.add(Calendar.MONTH, -allowUpdatePeriod);

						if (dateTSRCreated.before(dateNow)) {
							sa.setAtribute(SearchAttributes.ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR, "true");
						} else {
							sa.setAtribute(SearchAttributes.ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR, "false");
						}
					}

					setStartDateForUpdate(sa, defaultSdate.getTime(), origSa);
					if ("TRUE".equalsIgnoreCase(sa.getAtribute(SearchAttributes.IS_PRODUCT_CHANGED_ONCE))
							|| !"TRUE".equalsIgnoreCase(sa.getAtribute(SearchAttributes.IS_UPDATED_ONCE))) {
						sa.setCertificationDate();
						sa.setAtribute(SearchAttributes.IS_PRODUCT_CHANGED_ONCE, "FALSE");
					}
					checkIfOriginalSearchIsRefinance(oldSearch, global);

					sa.setAtribute(SearchAttributes.P_COUNTY, origSa.getAtribute(SearchAttributes.P_COUNTY));
					sa.setAtribute(SearchAttributes.P_STATE, origSa.getAtribute(SearchAttributes.P_STATE));

					if (!"TRUE".equalsIgnoreCase(sa.getAtribute(SearchAttributes.IS_UPDATED_ONCE))) {

						String toLog = "Updating order with FileId: [<b>" + orderToUpdate.getFileId() + 
								"</b>], SearchID: [<b>" + orderToUpdate.getToUpdateSearchInfo().getSearchId();
						String toUpdateGuid = sa.getAtribute(SearchAttributes.STEWARTORDERS_TO_UPDATE_ORDER_GUID);
						if(org.apache.commons.lang.StringUtils.isNotBlank(toUpdateGuid)) {
							toLog += "</b>], SO GUID: [<b>" + toUpdateGuid;
						}
						toLog += "</b>]";
						
						SearchLogger.info(toLog, global.getID());
						
						logger.info(">> FROMDATE: (" + sa.getAtribute(SearchAttributes.FROMDATE) + ")");
						logger.info(">> TODATE: (" + sa.getAtribute(SearchAttributes.TODATE) + ")");

						sa.setAtribute(SearchAttributes.P_STREETNO, origSa.getAtribute(SearchAttributes.P_STREETNO));
						sa.setAtribute(SearchAttributes.P_STREETDIRECTION, origSa.getAtribute(SearchAttributes.P_STREETDIRECTION));
						sa.setAtribute(SearchAttributes.P_STREET_POST_DIRECTION, origSa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION));
						sa.setAtribute(SearchAttributes.P_STREETNAME, origSa.getAtribute(SearchAttributes.P_STREETNAME));
						sa.setAtribute(SearchAttributes.P_STREETSUFIX, origSa.getAtribute(SearchAttributes.P_STREETSUFIX));
						sa.setAtribute(SearchAttributes.P_STREETUNIT, origSa.getAtribute(SearchAttributes.P_STREETUNIT));
						sa.setAtribute(SearchAttributes.P_IDENTIFIER_TYPE, origSa.getAtribute(SearchAttributes.P_IDENTIFIER_TYPE));
						sa.setAtribute(SearchAttributes.P_CITY, origSa.getAtribute(SearchAttributes.P_CITY));
						sa.setAtribute(SearchAttributes.LD_LOTNO, origSa.getAtribute(SearchAttributes.LD_LOTNO));
						sa.setAtribute(SearchAttributes.LD_SUBLOT, origSa.getAtribute(SearchAttributes.LD_SUBLOT));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_NAME, origSa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_SEC, origSa.getAtribute(SearchAttributes.LD_SUBDIV_SEC));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, origSa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_PHASE, origSa.getAtribute(SearchAttributes.LD_SUBDIV_PHASE));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_TRACT, origSa.getAtribute(SearchAttributes.LD_SUBDIV_TRACT));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_TWN, origSa.getAtribute(SearchAttributes.LD_SUBDIV_TWN));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_RNG, origSa.getAtribute(SearchAttributes.LD_SUBDIV_RNG));
						sa.setAtribute(SearchAttributes.LD_SUBDIV_UNIT, origSa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT));
						sa.setAtribute(SearchAttributes.LD_SECTION, origSa.getAtribute(SearchAttributes.LD_SECTION));
						sa.setAtribute(SearchAttributes.LD_PARCELNO, origSa.getAtribute(SearchAttributes.LD_PARCELNO));
						sa.setAtribute(SearchAttributes.LD_INSTRNO, origSa.getAtribute(SearchAttributes.LD_INSTRNO));
						sa.setAtribute(SearchAttributes.LD_BOOKPAGE, origSa.getAtribute(SearchAttributes.LD_BOOKPAGE));
						sa.setAtribute(SearchAttributes.LD_BOOKNO_1, origSa.getAtribute(SearchAttributes.LD_BOOKNO_1));
						sa.setAtribute(SearchAttributes.LD_PAGENO_1, origSa.getAtribute(SearchAttributes.LD_PAGENO_1));
						sa.setAtribute(SearchAttributes.LD_BOOKNO, origSa.getAtribute(SearchAttributes.LD_BOOKNO));
						sa.setAtribute(SearchAttributes.LD_PAGENO, origSa.getAtribute(SearchAttributes.LD_PAGENO));
						sa.setAtribute(SearchAttributes.QUARTER_ORDER, origSa.getAtribute(SearchAttributes.QUARTER_ORDER));
						sa.setAtribute(SearchAttributes.QUARTER_VALUE, origSa.getAtribute(SearchAttributes.QUARTER_VALUE));
						sa.setAtribute(SearchAttributes.ARB, origSa.getAtribute(SearchAttributes.ARB));
						sa.setAtribute(SearchAttributes.LD_ABS_NO, origSa.getAtribute(SearchAttributes.LD_ABS_NO));

						sa.setAtribute(SearchAttributes.ATIDS_FILE_REFERENCE_ID, origSa.getAtribute(SearchAttributes.ATIDS_FILE_REFERENCE_ID));
						sa.setSearchIdSKLD(origSa.getSearchIdSKLD());

						treatNamesInUpdate(sa.getOwners(), origSa.getOwners());
						treatNamesInUpdate(sa.getBuyers(), origSa.getBuyers());

						if (origSa.getForUpdateSearchGrantorNames() != null) {
							for (Long serverId : origSa.getForUpdateSearchGrantorNames().keySet()) {
								sa.addForUpdateSearchGrantorNames(origSa.getForUpdateSearchGrantorNames().get(serverId), serverId);
							}
						}
						if (origSa.getForUpdateSearchGranteeNames() != null) {
							for (Long serverId : origSa.getForUpdateSearchGranteeNames().keySet()) {
								sa.addForUpdateSearchGranteeNames(origSa.getForUpdateSearchGranteeNames().get(serverId), serverId);
							}
						}
						if (origSa.getForUpdateSearchAddresses() != null) {
							for (Long serverId : origSa.getForUpdateSearchAddresses().keySet()) {
								sa.addForUpdateSearchAddresses(origSa.getForUpdateSearchAddresses().get(serverId), serverId);
							}
						}

						if (origSa.getForUpdateSearchLegals() != null) {
							for (Long serverId : origSa.getForUpdateSearchLegals().keySet()) {
								sa.addForUpdateSearchLegals(origSa.getForUpdateSearchLegals().get(serverId), serverId);
							}
						}

						sa.setAtribute(SearchAttributes.IS_UPDATED_ONCE, "TRUE");

						long originalSearchId = origSa.getOriginalSearchId();

						if (originalSearchId <= 0) {
							originalSearchId = origSa.getSearchId();
						}

						sa.setOriginalSearchId(originalSearchId);

					}

				} else {
					throw new BaseException(PROBLEM_LOADING_OLD_SEARCH_MESSAGE + orderToUpdate.getFileId());
				}
			}

		}
	}
	
	private static OrderToUpdateResult getOrderToUpdate(Search global, String key) throws BaseException {
		SearchAttributes sa = global.getSa();

		List<SearchUpdateMapper> searches = new ArrayList<SearchUpdateMapper>();

		String sFileNo = sa.getAtribute(key);
		if (sFileNo.indexOf("_") != -1) // if it contains "_" it is the full name that contains the fileID
			sFileNo = sFileNo.replaceAll("TSR-(.*?)_(.*?)_(.*)", "$1");
		if (StringUtils.isEmpty(sFileNo)) {
			throw new BaseException(ABSTR_FID_NOT_FILLED_MESSAGE);
		}

		UserAttributes agent = global.getAgent();
		if (agent == null) {
			throw new BaseException("The order :" + sFileNo + " does not have agent ");
		}

		int commId = InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentCommunity().getID().intValue();
		OrderToUpdateResult orderToUpdateResult = new OrderToUpdateResult();
		orderToUpdateResult.setFileId(sFileNo);

		if (sa.isFVSUpdate()) {
			searches = DBManager.getSearchIdsForFVSUpdate(global.getFVSParentSearchID());
			
			if (searches.size() > 0){
				internalFillOrderToUpdateAtsStyle(global, searches, sFileNo, agent, orderToUpdateResult);
				if (orderToUpdateResult.getToUpdateSearchInfo() != null || orderToUpdateResult.getParentSearchInfo() != null) {
					return orderToUpdateResult;
				}
			}
		}
		{
			String toUpdateGuid = global.getSa().getAtribute(SearchAttributes.STEWARTORDERS_TO_UPDATE_ORDER_GUID);
			if (org.apache.commons.lang.StringUtils.isNotBlank(toUpdateGuid)) {
				searches = DBManager.getSearchIdsForUpdate(
						toUpdateGuid,
						commId,
						sa.getAtribute(SearchAttributes.P_COUNTY));
				if (!searches.isEmpty()) {
					orderToUpdateResult.setToUpdateSearchInfo(searches.get(0));
				}
			}

			String parentGuid = global.getSa().getAtribute(SearchAttributes.STEWARTORDERS_PARENT_ORDER_GUID);
			if (org.apache.commons.lang.StringUtils.isNotBlank(parentGuid)) {
				searches = DBManager.getSearchIdsForUpdate(
						parentGuid,
						commId,
						sa.getAtribute(SearchAttributes.P_COUNTY));
				if (!searches.isEmpty()) {
					orderToUpdateResult.setParentSearchInfo(searches.get(0));
				}
			}

			if (orderToUpdateResult.getToUpdateSearchInfo() == null || orderToUpdateResult.getParentSearchInfo() == null) {
				// I need the backup code, that has to load both objects
				searches = DBManager.getSearchIdsForUpdate(
						"___-" + sFileNo.replaceAll("[_]*", "") + "\\_",
						commId,
						sa.getAtribute(SearchAttributes.P_COUNTY),
						sa.getAtribute(SearchAttributes.ORDERBY_FILENO));

				internalFillOrderToUpdateAtsStyle(global, searches, sFileNo, agent, orderToUpdateResult);

			}

		}

		return orderToUpdateResult;

	}

	private static void internalFillOrderToUpdateAtsStyle(Search global, List<SearchUpdateMapper> searches, String sFileNo, UserAttributes agent,
			OrderToUpdateResult orderToUpdateResult) throws BaseException {

		SearchAttributes sa = global.getSa();

		int updatesFinished = 0;
		List<SearchUpdateMapper> goodSearchesWithSameAgent = new ArrayList<SearchUpdateMapper>();
		List<SearchUpdateMapper> goodSearches = new ArrayList<SearchUpdateMapper>();

		for (SearchUpdateMapper searchUpdateMapper : searches) {
			if (searchUpdateMapper.isUpdate()) {
				updatesFinished++;
				goodSearches.add(searchUpdateMapper);
			}
			else {
				goodSearches.add(searchUpdateMapper);
			}
		}

		if(orderToUpdateResult.getToUpdateSearchInfo() == null) {
			orderToUpdateResult.setFinishedUpdates(updatesFinished);
		}

		StringBuilder sb = new StringBuilder();
		int size = goodSearches.size();
		if (size == 0) {
			// if we found only updates still no good
			String toUpdateGuid = global.getSa().getAtribute(SearchAttributes.STEWARTORDERS_TO_UPDATE_ORDER_GUID);
			if (org.apache.commons.lang.StringUtils.isNotBlank(toUpdateGuid)) {
				if (ServerConfig.isCheckAgentFileNoInUpdate()) {
					throw new BaseException(MessageFormat.format(UPDATE_FILE_NOT_FOUND_MESSAGE_WITH_AGENT_AND_GUID_FILE_ID, sFileNo,
							sa.getAtribute(SearchAttributes.ORDERBY_FILENO), toUpdateGuid));
				} else {
					throw new BaseException(MessageFormat.format(UPDATE_FILE_NOT_FOUND_AND_GUID_MESSAGE, sFileNo, toUpdateGuid));
				}
			} else {
				if (ServerConfig.isCheckAgentFileNoInUpdate()) {
					throw new BaseException(MessageFormat.format(UPDATE_FILE_NOT_FOUND_MESSAGE_WITH_AGENT_FILE_ID, sFileNo,
							sa.getAtribute(SearchAttributes.ORDERBY_FILENO)));
				} else {
					throw new BaseException(MessageFormat.format(UPDATE_FILE_NOT_FOUND_MESSAGE, sFileNo));
				}
			}
		} else if (size == 1) {
			if (orderToUpdateResult.getToUpdateSearchInfo() == null) {
				orderToUpdateResult.setToUpdateSearchInfo(goodSearches.get(0));
			}
			if (orderToUpdateResult.getParentSearchInfo() == null) {
				orderToUpdateResult.setParentSearchInfo(goodSearches.get(0));
			}
		} else {
			Collections.sort(goodSearches);
			sb.append("Analyzing ")
					.append(goodSearches.size())
					.append(" searches for update ")
					.append(global.getID()).append("/").append(sFileNo);

			for (SearchUpdateMapper searchUpdateMapper : goodSearches) {
				try {
					sb.append("\n").append("Trying to open ").append(searchUpdateMapper.getSearchId());
					Search oldSearch = SearchManager.getSearchFromDisk(searchUpdateMapper.getSearchId());
					if (oldSearch != null) {
						sb.append(" and succedded to open Search. Trying to get initial order");
						Document initialOrder = oldSearch.getInitialOrderXMLFromOrderFile();
						if (initialOrder != null) {
							sb.append(" and succedded to get intial order ");
							Node n;
							NodeList nl = initialOrder.getElementsByTagName("agent_name");
							sb.append(" with ").append(nl.getLength()).append(" nodes with tagName agent_name");
							for (int x = 0; x < nl.getLength(); x++) {
								n = nl.item(x);
								String agentName = n.getTextContent();
								if (agentName != null) {
									agentName = agentName.trim();
									sb.append(" and found agent name[")
											.append(agentName)
											.append("] with size ")
											.append(agentName.length())
											.append(" and I wil test against [").append(agent.getLOGIN())
											.append("] with size ").append(agent.getLOGIN().length());
									if (agentName.equals(agent.getLOGIN().trim())) {
										if (orderToUpdateResult.getToUpdateSearchInfo() == null) {
											orderToUpdateResult.setToUpdateSearchInfo(searchUpdateMapper);
											sb.append(" and FOUND MY search to update");
										}
										goodSearchesWithSameAgent.add(searchUpdateMapper);
									}
								}
							}
							if (orderToUpdateResult.getToUpdateSearchInfo() == null) {
								sb.append(" but failed to match or find agent name\n");
							}
						} else {
							sb.append(" and failed to get intial order ");
							// fix for 5537
							String agentName = oldSearch.getAgent().getLOGIN();
							if (agentName != null) {
								agentName = agentName.trim();
								sb.append(" but found agent name ").append(agentName);
								if (agentName.equals(agent.getLOGIN().trim())) {
									if (orderToUpdateResult.getToUpdateSearchInfo() == null) {
										orderToUpdateResult.setToUpdateSearchInfo(searchUpdateMapper);
										sb.append(" and FOUND my search to update");
									}
									goodSearchesWithSameAgent.add(searchUpdateMapper);
								}
							} else {
								sb.append(" and failed to get agent name ");
							}
						}
					} else {
						sb.append(" and failed ");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.info(sb.toString());
		}

		if (orderToUpdateResult.getToUpdateSearchInfo() == null) {
			System.err.println(sb.toString());
			String toUpdateGuid = global.getSa().getAtribute(SearchAttributes.STEWARTORDERS_TO_UPDATE_ORDER_GUID);
			if (org.apache.commons.lang.StringUtils.isNotBlank(toUpdateGuid)) {
				throw new BaseException("Could not identify the search with fileno :[" + sFileNo + "] or To Update GUID: [" + toUpdateGuid + "] initially ordered by :[" + agent.getLOGIN() + "]");
			} else {
				throw new BaseException("Could not identify the search with fileno :[" + sFileNo + "] initially ordered by :[" + agent.getLOGIN() + "]");
			}
		}

		if (orderToUpdateResult.getParentSearchInfo() == null) {
			if (goodSearchesWithSameAgent != null && goodSearchesWithSameAgent.size() > 0) {
				Collections.sort(goodSearchesWithSameAgent);
				orderToUpdateResult.setParentSearchInfo(goodSearchesWithSameAgent.get(goodSearchesWithSameAgent.size() - 1));
			}
		}
	}

	/**
	 * Copies new names received in update to the original ones from the updated search
	 * @param receivedNames the party received (in order)
	 * @param originalNames the party already present in the updated search
	 */
	private static void treatNamesInUpdate(PartyI receivedNames, PartyI originalNames) {
		for (NameI origName : originalNames.getNames()) {
        	NameI receivedName = receivedNames.getNameEqualTo(origName);
        	if(receivedName == null) {
        		origName.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.UPDATE));
        		receivedNames.add(origName);
        	} else {
        		receivedName.getNameFlags().addSourceType(new NameSourceType(NameSourceType.UPDATE));
        	}        	
		}
	}

	private static void setStartDateForUpdate(SearchAttributes currentSa, Date defaultCertifDate, SearchAttributes origSa) {
		
		Date start = null;
		String startS = "";
		SimpleDateFormat sdfX = new SimpleDateFormat("MM/dd/yyyy");
		String searchFrom = Products.getSearchFrom(currentSa.getCommId(), currentSa.getProductId());
		
		boolean useCertificationDateAsStartDate = true;
		
		if (Products.isOneOfUpdateProductType(currentSa.getProductId()) && 
        		(searchFrom != null && !Products.SearchFromOptions.CD.toString().equals(searchFrom))){
			useCertificationDateAsStartDate = false;
        	
        	if (Products.SearchFromOptions.DEF.toString().equals(searchFrom)){
        		int yearsBackValue = CountyCommunityManager.getInstance()
									.getCountyCommunityMapper(Integer.parseInt(currentSa.getCountyId()), currentSa.getCommId())
									.getDefaultStartDateOffset();
        		searchFrom = Integer.toString(yearsBackValue);
        	}
        	if (NumberUtils.isDigits(searchFrom)){
        		int offset = Integer.parseInt(searchFrom);
        		Calendar date = Calendar.getInstance();
        		date.add(Calendar.YEAR, - offset);
        		startS = SearchAttributes.DEFAULT_DATE_PARSER.format(date.getTime());
        	}
        }
		
		if (Products.isOneOfUpdateProductType(currentSa.getProductId()) && useCertificationDateAsStartDate){
			CertificationDate certificationDate = origSa.getCertificationDate();
			if (certificationDate != null && certificationDate.getDate() != null){
				start = origSa.getCertificationDate().getDate();
			} else if (origSa.getAtribute(SearchAttributes.CERTICICATION_DATE) != null && !"".equals(origSa.getAtribute(SearchAttributes.CERTICICATION_DATE))){
					try{
						start = sdfX.parse(origSa.getAtribute(SearchAttributes.CERTICICATION_DATE));
					} catch (ParseException e){ }
			} else{
				 logger.info("Setting FROMDATE to " + SearchAttributes.DEFAULT_DATE_PARSER.format(defaultCertifDate.getTime()));
				 System.err.println("Setting FROMDATE to " + SearchAttributes.DEFAULT_DATE_PARSER.format(defaultCertifDate.getTime()));
				    
				 currentSa.setAtribute(SearchAttributes.FROMDATE, SearchAttributes.DEFAULT_DATE_PARSER.format(defaultCertifDate.getTime()));  
			}
			
			if (start != null){
		        sdfX.applyPattern("MMM d, yyyy");
		        startS = sdfX.format(start);
	        }
		}
		if (org.apache.commons.lang.StringUtils.isNotEmpty(startS)){
			logger.info("Setting FROMDATE to " + startS);
		    System.err.println("Setting FROMDATE to " + startS);
		    currentSa.setAtribute(SearchAttributes.FROMDATE, startS);
		}
	}
	/**
	 * Method updateUserAttributes.
	 * @param userAttributes
	 * @param sa
	 */
	private void updateUserAttributes(UserAttributes userAttributes, SearchAttributes sa) throws DataException, BaseException
	{
		//sa.setAbstractor(userAttributes); //update the search Attributes if current user has been updated
		String agentId = sa.getAtribute(SearchAttributes.ORDERBY_ID);
		if(!agentId.equals("")){	
			if(!new BigDecimal(agentId).equals(userAttributes.getAGENTID())) {
				userAttributes.setAGENTID(new BigDecimal(agentId));
				/*UserManager.updateUserLite( userAttributes);*/
				DataAttribute.updateAttributes(DBConstants.TABLE_USER,UserAttributes.USER_AGENTID,
											   agentId,
											   UserAttributes.USER_ID+ "=" + userAttributes.getID());
			}
		}
	}
	
	private void updateAgentAttributes(UserAttributes ua, HttpServletRequest request , SearchAttributes sa) throws DataException, BaseException
	{
		String agentId = (String)request.getParameter("agentCombo");
		sa.setAtribute(SearchAttributes.ORDERBY_ID,/*aa.getID().toString()*/agentId);
			
	}
	
	private static boolean isChangedAttribute(HttpServletRequest request, SearchAttributes sa, String attribute) { 
		try {
			//B3792 - if in search page zip is 5 digits, use only those for comparation
			String valReq = request.getParameter(attribute);
			String valSA = sa.getAtribute(attribute);
			if (attribute.equals(SearchAttributes.P_ZIP) && sa.getStateCounty().startsWith("TN")){
				valReq = valReq.replaceAll("-", "");
				valSA = valSA.replaceAll("-", "");
				if (valReq.length() == 5
					&& valSA.length() >=5 
					&& !valReq.substring(0, 5).equalsIgnoreCase(valSA.substring(0, 5)))
				{
						return true;
				}
				if ((valReq.length() != 5 || valSA.length() < 5 )
					&& !valSA.equalsIgnoreCase(valReq))
				{
					return true;
				}
			} else if(!request.getParameter(attribute).equalsIgnoreCase(sa.getAtribute(attribute))) {
				return true;	
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void checkTNZip(SearchAttributes sa, HttpServletRequest request){
		//B3792
		try {
			if (sa.getStateCounty().startsWith("TN")
				&& sa.getSearchPageManualFields().containsKey(SearchAttributes.P_ZIP))
			{
				String zipSA = sa.getAtribute(SearchAttributes.P_ZIP).replaceAll("-", "");
				String zipReq = request.getParameter(SearchAttributes.P_ZIP);
				String zipManual = sa.getSearchPageManualFields().get(SearchAttributes.P_ZIP).replaceAll("-", "");
				if (zipReq == null){
					zipReq = "";
				}
				zipReq = zipReq.replaceAll("-", "");
				if (zipReq.length() == 5
					&& zipSA.length() >= 5
					&& zipManual.length() >= 5
					&& zipReq.equalsIgnoreCase(zipSA.substring(0, 5))
					&& !zipReq.equalsIgnoreCase(zipManual.substring(0, 5))){
						sa.getSearchPageManualFields().remove(SearchAttributes.P_ZIP);
				}
				if (zipReq.length() != 5
					&& zipReq.equalsIgnoreCase(zipSA)
					&& !zipReq.equalsIgnoreCase(zipManual))
				{
					sa.getSearchPageManualFields().remove(SearchAttributes.P_ZIP);
				}
					
			}
		}catch(Exception e) {
			/* Exception in submit order page */
			e.printStackTrace();
		}
	}
	
	public static int saveSearchAttributes(HttpServletRequest request, Search global)
	{
		int numberOfValidatedOwners = 0;
		String o = (String)request.getParameter("orderTS");
		char c = 0x092;
		ParameterParser parameterParser = new ParameterParser(request);
		SearchAttributes sa = global.getSa();

		SearchLogFactory logFactory = SearchLogFactory.getSharedInstance();
		SearchLogPage searchLogPage = logFactory.getSearchLogPage(global.getSearchID());

		long searchId = sa.getSearchId();
		String currentUser = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
		Products currentCommunityProducts = CommunityProducts.getProduct(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue());
		if (!StringUtils.isEmpty(sa.getAtribute(SearchAttributes.SEARCH_PRODUCT)) && !StringUtils.isEmpty(request.getParameter(SearchAttributes.SEARCH_PRODUCT))){
			if (!sa.getAtribute(SearchAttributes.SEARCH_PRODUCT).equals(request.getParameter(SearchAttributes.SEARCH_PRODUCT))) {
		        String productName = currentCommunityProducts.getProductName(Integer.parseInt(sa.getAtribute(SearchAttributes.SEARCH_PRODUCT)));
				String productName2 = currentCommunityProducts.getProductName(Integer.parseInt(request.getParameter(SearchAttributes.SEARCH_PRODUCT)));
				searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, AtsStandardNames.VARIABLE_PRODUCT_TYPE, productName, productName2, currentUser);
				
				boolean isOSOTOY = false;
				if ("true".equals(sa.getAtribute(SearchAttributes.ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR))){
					isOSOTOY = true;
				}
				if (isOSOTOY){
					SearchLogger.info("</div><div>In Search Page, the Product Type <b>" 
							+ productName + "</b> was changed automatically to the original search product <b>" + productName2 
							+ "</b> because the original search is older than two years " + SearchLogger.getTimeStamp(sa.getSearchId())
							+".<BR><div>", searchId);
				} else {
					SearchLogger.info("</div><div>In Search Page, the Product Type <b>"
			        		+ productName+ "</b> was changed to <b>" 
			        		+ productName2+ "</b> by <b>"
			        		+ "</b> at "
			    			+ SearchLogger.getCurDateTimeCST() 
			    			+ ".<BR>", searchId);
				}
			}
		}
		String initialValue = sa.getAtribute(SearchAttributes.FROMDATE);
		String afterValue = initialValue;
		
		String loadedsearchDatesInterval = request.getParameter("loadedsearchDatesInterval");
		
		if("1".equals(loadedsearchDatesInterval)) {
			afterValue = request.getParameter(SearchAttributes.FROMDATE);
		}
		
		
		
		
		String MM_dd_yyyy_regEx = "\\d{1,2}/\\d{1,2}/\\d{4}";
		initialValue = switchToDefaultFormat(initialValue, MM_dd_yyyy_regEx);
		afterValue = switchToDefaultFormat(afterValue, MM_dd_yyyy_regEx);
		
		if (!StringUtils.isEmpty(initialValue) && !StringUtils.isEmpty(afterValue)){
			try {
				int x = SearchAttributes.DEFAULT_DATE_PARSER.parse(initialValue).
							compareTo(SearchAttributes.DEFAULT_DATE_PARSER.parse(afterValue));
				if (x != 0) {
					searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, AtsStandardNames.VARIABLE_START_DATE, 
							initialValue, afterValue, currentUser);
			        SearchLogger.info("</div><div>In Search Page, the Start Date <b>"
			        		+ initialValue+ "</b> was changed to <b>" 
			        		+ afterValue+ "</b> by <b>"
			        		+ currentUser + "</b> at "
			    			+ SearchLogger.getCurDateTimeCST() 
			    			+ ".<BR>", searchId);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
		
		String initialToDate = sa.getAtribute(SearchAttributes.TODATE);
		String afterToDate = initialToDate;
		if("1".equals(loadedsearchDatesInterval)) {
			afterToDate = request.getParameter(SearchAttributes.TODATE);
		}
		
		afterToDate = switchToDefaultFormat(afterToDate, MM_dd_yyyy_regEx);
		initialToDate = switchToDefaultFormat(initialToDate, MM_dd_yyyy_regEx);
		
		if (!StringUtils.isEmpty(initialToDate) && !StringUtils.isEmpty(afterToDate)){
			try {
				int x = SearchAttributes.DEFAULT_DATE_PARSER.parse(initialToDate).
							compareTo(SearchAttributes.DEFAULT_DATE_PARSER.parse(afterToDate));
				if (x != 0) {
				searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, AtsStandardNames.VARIABLE_END_DATE, 
							initialToDate, afterToDate, currentUser);
		        SearchLogger.info("</div><div>In Search Page, the End Date <b>"
		        		+ initialToDate+ "</b> was changed to <b>" 
		        		+ afterToDate+ "</b> by <b>"
		        		+ currentUser+ "</b> at "
		    			+ SearchLogger.getCurDateTimeCST() 
		    			+ ".<BR>", searchId);
		        
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		sa.setAtribute(SearchAttributes.FROMDATE, afterValue);
		sa.setAtribute(SearchAttributes.TODATE, afterToDate);
		sa.setAtribute(SearchAttributes.SEARCHUPDATE, request.getParameter(SearchAttributes.SEARCHUPDATE));
		sa.setAtribute(SearchAttributes.SEARCH_PRODUCT, request.getParameter(SearchAttributes.SEARCH_PRODUCT));
		// 1) Property Adress
		for( String attribute : new String[] { 
				SearchAttributes.P_STREETNO, 
				SearchAttributes.P_STREETDIRECTION,
				SearchAttributes.P_STREETNAME, 
				SearchAttributes.P_STREET_POST_DIRECTION,
				SearchAttributes.P_STREETSUFIX,
				SearchAttributes.P_STREETUNIT,
				SearchAttributes.P_IDENTIFIER_TYPE,
				SearchAttributes.P_CITY,
				SearchAttributes.P_ZIP,
				SearchAttributes.LD_PARCELNO,
				SearchAttributes.LD_INSTRNO,
				SearchAttributes.LD_BOOKPAGE,
				SearchAttributes.LD_BOOKNO,
				SearchAttributes.LD_PAGENO
				} )	{
			String inititialAttributeValue = sa.getAtribute(attribute);
			String afterAttributeValue = request.getParameter(attribute);
			if(isChangedAttribute(request,sa,attribute)) {
				
				String atributeToBeChanged = (String) attributes.get(attribute);
				searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, atributeToBeChanged, 
						inititialAttributeValue, afterAttributeValue, currentUser);
				if(StringUtils.isEmpty(afterAttributeValue)) {
					SearchLogger.info("</div><div>In Search Page, "+ atributeToBeChanged +" <b>"
			        		+ inititialAttributeValue+ "</b> was cleared by <b>"
			        		+ currentUser+ "</b> at "
			    			+ SearchLogger.getCurDateTimeCST() 
			    			+ ".<BR>", searchId);
					if (SearchAttributes.LD_PARCELNO.equals(attribute)) {
						sa.setAtribute(SearchAttributes.LD_PARCELNO_PARCEL, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO2, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO2_ALTERNATE, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO3, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_CONDO, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_CTRL_MAP, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_CTRL_MAP_GENERIC_TR, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNONDB, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP_GENERIC_TR, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP_GENERIC_TR, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_PREFIX, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_RANGE, "");
						sa.setAtribute(SearchAttributes.LD_PARCELNO_TOWNSHIP, "");
					}
				} else {
					SearchLogger.info("</div><div>In Search Page, "+ atributeToBeChanged +" <b>"
			        		+ inititialAttributeValue+ "</b> was changed to <b>" 
			        		+ afterAttributeValue+ "</b> by <b>"
			        		+ currentUser+ "</b> at "
			    			+ SearchLogger.getCurDateTimeCST() 
			    			+ ".<BR>", searchId);
				}
				try {
					sa.getSearchPageManualFields().put(attribute,afterAttributeValue.trim());
				}catch(Exception e) {}
			}
			if(sa.getSearchPageManualFields().containsKey(attribute) &&
					afterAttributeValue.equalsIgnoreCase(inititialAttributeValue) && 
					!afterAttributeValue.equalsIgnoreCase(sa.getSearchPageManualFields().get(attribute))
			) {
				sa.getSearchPageManualFields().remove(attribute);
			}
		}
		checkTNZip(sa, request);
				
		sa.setAtribute(SearchAttributes.P_STREETDIRECTION, request.getParameter(SearchAttributes.P_STREETDIRECTION));
		sa.setAtribute(SearchAttributes.P_STREETNO, request.getParameter(SearchAttributes.P_STREETNO));
		
		sa.setAtribute(SearchAttributes.P_STREET_POST_DIRECTION, request.getParameter(SearchAttributes.P_STREET_POST_DIRECTION));
		sa.setAtribute(SearchAttributes.P_STREETNAME, request.getParameter(SearchAttributes.P_STREETNAME));
		sa.setAtribute(SearchAttributes.P_STREETSUFIX, request.getParameter(SearchAttributes.P_STREETSUFIX));
		sa.setAtribute(SearchAttributes.P_STREETUNIT, request.getParameter(SearchAttributes.P_STREETUNIT));
		sa.setAtribute(SearchAttributes.P_IDENTIFIER_TYPE, request.getParameter(SearchAttributes.P_IDENTIFIER_TYPE));
		sa.setAtribute(SearchAttributes.P_CITY, request.getParameter(SearchAttributes.P_CITY));
		sa.setAtribute(SearchAttributes.P_STATE, request.getParameter(SearchAttributes.P_STATE));
		sa.setAtribute(SearchAttributes.P_COUNTY, request.getParameter(SearchAttributes.P_COUNTY));
		sa.setAtribute(SearchAttributes.P_ZIP, request.getParameter(SearchAttributes.P_ZIP));
		logger.debug("P_state = " + request.getParameter(SearchAttributes.P_STATE));
		logger.debug("P_county= " + request.getParameter(SearchAttributes.P_COUNTY));
		// 2) Legal Description
		sa.setAtribute(SearchAttributes.LD_PARCELNO, request.getParameter(SearchAttributes.LD_PARCELNO));
		sa.setAtribute(SearchAttributes.LD_INSTRNO, request.getParameter(SearchAttributes.LD_INSTRNO));
		sa.setAtribute(SearchAttributes.LD_BOOKPAGE, request.getParameter(SearchAttributes.LD_BOOKPAGE));
		
		String oldBookNo = sa.getAtribute(SearchAttributes.LD_BOOKNO);
		String oldPageNo = sa.getAtribute(SearchAttributes.LD_PAGENO);
		String oldBookNo_1 = sa.getAtribute(SearchAttributes.LD_BOOKNO_1);
		String oldPageNo_1 = sa.getAtribute(SearchAttributes.LD_PAGENO_1);
		String newBookNo = request.getParameter(SearchAttributes.LD_BOOKNO);
		String newPageNo = request.getParameter(SearchAttributes.LD_PAGENO);
		
		if(oldBookNo.equalsIgnoreCase(oldBookNo_1) && oldPageNo.equalsIgnoreCase(oldPageNo_1)) {
			sa.setAtribute(SearchAttributes.LD_BOOKNO, newBookNo);
			sa.setAtribute(SearchAttributes.LD_PAGENO, newPageNo);	
			sa.setAtribute(SearchAttributes.LD_BOOKNO_1, newBookNo);
			sa.setAtribute(SearchAttributes.LD_PAGENO_1, newPageNo);	
		} else {
			sa.setAtribute(SearchAttributes.LD_BOOKNO, newBookNo);
			sa.setAtribute(SearchAttributes.LD_PAGENO, newPageNo);	
		}
		
		
		//FormatSa.setBookAndPage(sa, request.getParameter(SearchAttributes.LD_BOOKPAGE), false); 
		saveSaSubdiv(sa, request);
		
		boolean deleteNotDisplayed= false;
		if(sa.getAtribute(SearchAttributes.ORDERBY_FILENO).equals("") && !request.getParameter(SearchAttributes.ORDERBY_FILENO).equals(""))
		{
			SearchLogger.info("</div><div>Agent <b>File ID</b> value was set to <b>"
					+ request.getParameter(SearchAttributes.ORDERBY_FILENO) + "</b> "
					+ SearchLogger.getTimeStamp(searchId)
					+".<BR><div>", searchId);
			searchLogPage.setValueOperation(AtsStandardNames.VARIABLE_AGENT,
					AtsStandardNames.VARIABLE_PROPERTY_FILE_ID, request.getParameter(SearchAttributes.ORDERBY_FILENO));
		}
		else if (!sa.getAtribute(SearchAttributes.ORDERBY_FILENO).equals(request.getParameter(SearchAttributes.ORDERBY_FILENO)))
		{
			if ("".equals(request.getParameter(SearchAttributes.ORDERBY_FILENO)))
			{
				if (!sa.getAtribute(SearchAttributes.ORDERBY_FILENO).equals(request.getParameter(SearchAttributes.ABSTRACTOR_FILENO))){
					SearchLogger.info("</div><div>Agent <b>File ID</b> was <b>deleted</b> "
							+ SearchLogger.getTimeStamp(searchId)
							+".<BR><div>", searchId);
				searchLogPage.deleteValueOperation(AtsStandardNames.VARIABLE_AGENT,
						AtsStandardNames.VARIABLE_PROPERTY_FILE_ID);
				}
				else
					deleteNotDisplayed = true;
			}
			else
			{
				SearchLogger.info("</div><div>Agent <b>File ID</b> changed from <b>"
						+ sa.getAtribute(SearchAttributes.ORDERBY_FILENO) + "</b> to <b>"
						+ request.getParameter(SearchAttributes.ORDERBY_FILENO) + "</b> "
						+ SearchLogger.getTimeStamp(searchId)
						+".<BR><div>", searchId);
				searchLogPage.changeValueOperation(AtsStandardNames.VARIABLE_AGENT,
						AtsStandardNames.VARIABLE_PROPERTY_FILE_ID, sa.getAtribute(SearchAttributes.ORDERBY_FILENO),
						request.getParameter(SearchAttributes.ORDERBY_FILENO));
			}
		}
		sa.setAtribute(SearchAttributes.ORDERBY_FILENO, request.getParameter(SearchAttributes.ORDERBY_FILENO));
		
		if(sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO).equals("") && !request.getParameter(SearchAttributes.ABSTRACTOR_FILENO).equals(""))
		{
			SearchLogger.info("</div><div>Abstractor <b>File ID</b> value was set to <b>"
					+ request.getParameter(SearchAttributes.ABSTRACTOR_FILENO) + "</b> "
					+ SearchLogger.getTimeStamp(searchId)
					+".<BR><div>", searchId);
			searchLogPage.setValueOperation(AtsStandardNames.VARIABLE_ABSTRACTOR, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID,request.getParameter(SearchAttributes.ABSTRACTOR_FILENO));
		}
		else if (!sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO).equals(request.getParameter(SearchAttributes.ABSTRACTOR_FILENO)))
		{
			if ("".equals(request.getParameter(SearchAttributes.ABSTRACTOR_FILENO)))
			{
				SearchLogger.info("</div><div>Abstractor <b>File ID</b> was <b>deleted</b> "
						+ SearchLogger.getTimeStamp(searchId)
						+".<BR><div>", searchId);
				searchLogPage.deleteValueOperation(AtsStandardNames.VARIABLE_ABSTRACTOR, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID);
			}
			else
			{
				SearchLogger.info("</div><div>Abstractor <b>File ID</b> changed from <b>"
						+ sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + "</b> to <b>"
						+ request.getParameter(SearchAttributes.ABSTRACTOR_FILENO) + "</b> "
						+ SearchLogger.getTimeStamp(searchId)
						+".<BR><div>", searchId);
				searchLogPage.changeValueOperation(AtsStandardNames.VARIABLE_ABSTRACTOR, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID,sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO),request.getParameter(SearchAttributes.ABSTRACTOR_FILENO));
			}
		}
		sa.setAtribute(SearchAttributes.ABSTRACTOR_FILENO, request.getParameter(SearchAttributes.ABSTRACTOR_FILENO));

        
        if( sa.getAtribute( SearchAttributes.ORDERBY_FILENO ).equals("") )
        {
            //agent file name not set , set it from ABSTRACTOR file name
    		if(sa.getAtribute(SearchAttributes.ORDERBY_FILENO).equals("") && !request.getParameter(SearchAttributes.ABSTRACTOR_FILENO).equals(""))
    		{
    			if (!sa.getAtribute(SearchAttributes.ORDERBY_FILENO).equals(request.getParameter(SearchAttributes.ABSTRACTOR_FILENO)) && !deleteNotDisplayed)
    				SearchLogger.info("</div><div>Agent <b>File ID</b> value was set to <b>"
    						+ request.getParameter(SearchAttributes.ABSTRACTOR_FILENO) + "</b> "
    						+ SearchLogger.getTimeStamp(searchId)
    						+".<BR><div>", searchId);
    				searchLogPage.setValueOperation(AtsStandardNames.VARIABLE_AGENT, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID, request.getParameter(SearchAttributes.ABSTRACTOR_FILENO));
    		}
    		else if (!sa.getAtribute(SearchAttributes.ORDERBY_FILENO).equals(request.getParameter(SearchAttributes.ABSTRACTOR_FILENO)))
    		{
    			if ("".equals(request.getParameter(SearchAttributes.ABSTRACTOR_FILENO)))
    			{
    				SearchLogger.info("</div><div>Agent <b>File ID</b> was <b>deleted</b> "
    						+ SearchLogger.getTimeStamp(searchId)
    						+".<BR><div>", searchId);
    				searchLogPage.deleteValueOperation(AtsStandardNames.VARIABLE_AGENT, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID);
    			}
    			else
    			{
	    			SearchLogger.info("</div><div>Agent <b>File ID</b> changed from <b>"
	    					+ sa.getAtribute(SearchAttributes.ORDERBY_FILENO) + "</b> to <b>"
	    					+ request.getParameter(SearchAttributes.ABSTRACTOR_FILENO) + "</b> "
	    					+ SearchLogger.getTimeStamp(searchId)
	    					+".<BR><div>", searchId);
	    			searchLogPage.changeValueOperation(AtsStandardNames.VARIABLE_AGENT, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID, 
	    					sa.getAtribute(SearchAttributes.ORDERBY_FILENO), request.getParameter(SearchAttributes.ABSTRACTOR_FILENO));
    			}
    		}
        	sa.setAtribute(SearchAttributes.ORDERBY_FILENO, request.getParameter(SearchAttributes.ABSTRACTOR_FILENO));
        }
        
        if( sa.getAtribute( SearchAttributes.ABSTRACTOR_FILENO ).equals( "" ) )
        {
            //set the abstractor file no from order by file no, if  not set
    		if(sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO).equals("") && !request.getParameter(SearchAttributes.ORDERBY_FILENO).equals(""))
    		{
    			SearchLogger.info("</div><div>Abstractor <b>File ID</b> value was set to <b>"
    					+ request.getParameter(SearchAttributes.ORDERBY_FILENO) + "</b> "
    					+ SearchLogger.getTimeStamp(searchId)
    					+".<BR><div>", searchId);
    			searchLogPage.setValueOperation(AtsStandardNames.VARIABLE_ABSTRACTOR, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID, request.getParameter(SearchAttributes.ORDERBY_FILENO));
    		}
    		else if (!sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO).equals(request.getParameter(SearchAttributes.ORDERBY_FILENO)))
    		{
    			if ("".equals(request.getParameter(SearchAttributes.ORDERBY_FILENO)))
    			{
    				SearchLogger.info("</div><div>Abstractor <b>File ID</b> was <b>deleted</b> "
    						+ SearchLogger.getTimeStamp(searchId)
    						+".<BR><div>", searchId);
    				searchLogPage.deleteValueOperation(AtsStandardNames.VARIABLE_ABSTRACTOR, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID);
    			}
    			else
    			{
	    			SearchLogger.info("</div><div>Abstractor <b>File ID</b> changed from <b>"
	    					+ sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + "</b> to <b>"
	    					+ request.getParameter(SearchAttributes.ORDERBY_FILENO) + "</b> "
	    					+ SearchLogger.getTimeStamp(searchId)
	    					+".<BR><div>", searchId);
	    			searchLogPage.changeValueOperation(AtsStandardNames.VARIABLE_ABSTRACTOR, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID, 
	    					sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO), request.getParameter(SearchAttributes.ORDERBY_FILENO));
    			}
    		}
        	sa.setAtribute( SearchAttributes.ABSTRACTOR_FILENO, request.getParameter( SearchAttributes.ORDERBY_FILENO ) );
        }

        // 7) 1st Borrower Mortgage
		sa.setAtribute(SearchAttributes.BM1_LENDERNAME, request.getParameter(SearchAttributes.BM1_LENDERNAME));
		sa.setAtribute(SearchAttributes.BM1_LOADACCOUNTNO, request.getParameter(SearchAttributes.BM1_LOADACCOUNTNO));
		// 8) 2nd Borrower Mortgage
		sa.setAtribute(SearchAttributes.BM2_LENDERNAME, request.getParameter(SearchAttributes.BM2_LENDERNAME));
		sa.setAtribute(SearchAttributes.BM2_LOADACCOUNTNO, request.getParameter(SearchAttributes.BM2_LOADACCOUNTNO));
		
		String payrateNewValue = "";
		try {
			payrateNewValue = format.parse(request.getParameter
								(SearchAttributes.PAYRATE_NEW_VALUE).replaceAll("\\$","").trim()).toString();
		} catch (Exception e ) {
			payrateNewValue = request.getParameter(SearchAttributes.PAYRATE_NEW_VALUE);
		}
		
		sa.setAtribute(SearchAttributes.PAYRATE_NEW_VALUE, payrateNewValue);
		
		try {
			int opCode = Integer.parseInt(request.getParameter(TSOpCode.OPCODE));
			if (opCode == TSOpCode.SUBMIT_ORDER) {
				saveAdditionalInformation(request, c, sa);
			}
		} catch (Exception e) {}
		
		String additionalRestrictions = request.getParameter(SearchAttributes.ADDITIONAL_REQUIREMENTS);
		if (additionalRestrictions != null) {
			additionalRestrictions = additionalRestrictions.replaceAll("\\$", "USD");
			additionalRestrictions = additionalRestrictions.replace(c, '\'');
		}
		sa.setAtribute(SearchAttributes.ADDITIONAL_REQUIREMENTS, additionalRestrictions != null ? additionalRestrictions : "");
        
		//additional exceptions
        String additionalExceptions = request.getParameter(SearchAttributes.ADDITIONAL_EXCEPTIONS);
        if (additionalExceptions != null) {
            additionalExceptions = additionalExceptions.replaceAll("\\$", "USD");
        }
        sa.setAtribute(SearchAttributes.ADDITIONAL_EXCEPTIONS, additionalExceptions != null ? additionalExceptions : "");
        
        
        
		/*
		 * abstractorFileName se seteaza acum in servletul care porneste conversia
		 * Am presupus ca de apare bugul de duplicare a linkurilor in reports
		 */
		
		    sa.setAbstractorFileName(global);
		
		String ignoreMiName = request.getParameter(SearchAttributes.IGNORE_MNAME);
		if(ignoreMiName!=null && ignoreMiName.equals("checked"))
			sa.setAtribute(SearchAttributes.IGNORE_MNAME, "true");
		else
			sa.setAtribute(SearchAttributes.IGNORE_MNAME, "false");
	
		String ignoreMiNameBuyer = request.getParameter(SearchAttributes.IGNORE_MNAME_BUYER);
		if(ignoreMiNameBuyer != null && ignoreMiNameBuyer.equals("checked"))
			sa.setAtribute(SearchAttributes.IGNORE_MNAME_BUYER, "true");
		else
			sa.setAtribute(SearchAttributes.IGNORE_MNAME_BUYER, "false");
	
		String is_condo = request.getParameter(SearchAttributes.IS_CONDO);
		if(is_condo!=null && is_condo.equals("checked"))
			sa.setAtribute(SearchAttributes.IS_CONDO, "true");
		else
			sa.setAtribute(SearchAttributes.IS_CONDO, "false");
		
		Party ownersList=(Party)sa.getOwners();
		Party ownersBefore = (Party)ownersList.clone();
		ownersList.clear();
				
		Party buyersList=(Party)sa.getBuyers();
		Party buyersBefore = (Party)buyersList.clone();
		buyersList.clear();
		
		int counter = 0;
		boolean stop=true;
		int i=1;	
		do {
			String currentLast = StringUtils.cleanTSOrder(request.getParameter(SearchAttributes.OWNER_LNAME + i), o);
			if(currentLast!=null){
				currentLast=currentLast.replaceAll((char)146+"", "'");
			}
		
			String currentMiddle = StringUtils.cleanTSOrder(request.getParameter(SearchAttributes.OWNER_MNAME + i), o);
			String currentFirst = StringUtils.cleanTSOrder(request.getParameter(SearchAttributes.OWNER_FNAME + i), o);
			String currentNameKey = parameterParser.getStringParameter(SearchAttributes.OWNER_NAME_KEY + i, "");
			String currentSuffix = parameterParser.getStringParameter(SearchAttributes.OWNER_NAME_SUFFIX + i, "");	
			String currentGuid = parameterParser.getStringParameter(SearchAttributes.OWNER_GUID + i, "");	
			String currentValidated = parameterParser.getStringParameter(RequestParams.SEARCH_PAGE_OWNER_MANUAL_VALIDATION_PREFIX + i, "");
			
			if (!StringUtils.isEmpty(currentLast)) {
				Name name = new Name(currentFirst, currentMiddle, currentLast);
				name.setExternalSystemGuid(currentGuid);
				name.setSufix(currentSuffix);
				name.setCompany(request.getParameter(SearchAttributes.OWNER_COMPTYPE + i).equals("1") ? true : false);
				if(StringUtils.isNotEmpty(currentValidated)) {
					name.setValidated(true);
					numberOfValidatedOwners ++;
				} else {
					name.setValidated(false);
				}
				if (!name.isCompany() 
						&& StringUtils.isEmpty(currentMiddle) 
						&& StringUtils.isEmpty(currentFirst)
						&&!StringUtils.isEmpty(currentLast)
						&& NameUtils.isCompany(currentLast)) {
					name.setCompany(true);
				}

				if(ownersBefore.contains(name) && name.getUniqueKey().equals(currentNameKey)) {
					//if the name did not change we should keep the SSN4 and other flags
					NameI tempName = ownersBefore.getNameEqualTo(name);
					tempName.setValidated(name.isValidated());	//change checkbox value
					boolean added = ownersList.add( tempName );
					if(!added) {
						NameI presentName = ownersList.getNameByUniqueKey(tempName.getUniqueKey());
						if(presentName != null) {
							//keep also validation info, no matter which record was validated
							if(!presentName.isValidated()) {
								if(name.isValidated()) {
									ownersList.remove(presentName);
									ownersList.add(name);
								} 
							}
						}
					} 
					counter ++;
				} else {
					//owner changed - forget ssn4
					boolean added = ownersList.add(name);
					if(!added) {
						NameI presentName = ownersList.getNameByUniqueKey(name.getUniqueKey());
						if(presentName != null) {
							//keep also validation info, no matter which record was validated
							if(!presentName.isValidated()) {
								if(name.isValidated()) {
									ownersList.remove(presentName);
									ownersList.add(name);
								} 
							}
						}
					}
					name.getNameFlags().addSourceType(new NameSourceType(NameSourceType.MANUAL));
					
					sa.getSearchPageManualOwners().add(name);
					if (Integer.parseInt(request.getParameter(SearchAttributes.SEARCH_PRODUCT)) == SearchAttributes.SEARCH_PROD_REFINANCE) {
						sa.getSearchPageManualBuyers().add(name); //CR 3870
					}
					
					//B 4069
					Set<NameI> names = ownersBefore.getNames();
					Iterator<NameI> itr = names.iterator();
					while (itr.hasNext()) {
						for (int j = 0; j < names.size(); j++){
							try {
								String ownerName = itr.next().toString();
								if (j == counter) {
									SearchLogger.info("</div><div>In Search Page, the owner <b>"
							        		+ ownerName+ "</b> was changed to <b>" 
							        		+ name+ "</b> by <b>"
							        		+ currentUser+ "</b> at "
							    			+ SearchLogger.getCurDateTimeCST() 
							    			+ ".<BR>", searchId);
									searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, AtsStandardNames.VARIABLE_OWNER, ownerName, name.toString(), currentUser);
								} 
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					counter ++;
					if (ownersList.size() > ownersBefore.size()) {						
						SearchLogger.info("</div><div>In Search Page, was added a new grantor: First= <b>"
				        		+ name.getFirstName()+ "</b>, Middle= <b>" + name.getMiddleName() + "</b>, Last= <b>" + name.getLastName() + "</b> by <b>"
				        		+ currentUser+ "</b> at "
				    			+ SearchLogger.getCurDateTimeCST() 
				    			+ ".<BR>", searchId);
						searchLogPage.addNewGrantor(name, currentUser);
						
					}
				}
			}

			if (currentLast == null)
				stop = false;

			i++;
		} while (stop);
		counter = 0;
		i=1;
		stop=true;
		do {
			String currentLast = StringUtils.cleanTSOrder(request.getParameter(SearchAttributes.BUYER_LNAME + i), o);
			String currentMiddle = StringUtils.cleanTSOrder(request.getParameter(SearchAttributes.BUYER_MNAME + i), o);
			String currentFirst = StringUtils.cleanTSOrder(request.getParameter(SearchAttributes.BUYER_FNAME + i), o);
			String currentNameKey = parameterParser.getStringParameter(SearchAttributes.BUYER_NAME_KEY + i, "");
			String currentSuffix = parameterParser.getStringParameter(SearchAttributes.BUYER_NAME_SUFFIX + i, "");
			String currentGuid = parameterParser.getStringParameter(SearchAttributes.BUYER_GUID + i, "");
			String currentValidated = parameterParser.getStringParameter(RequestParams.SEARCH_PAGE_BUYER_MANUAL_VALIDATION_PREFIX + i, "");
			
			if (!StringUtils.isEmpty(currentLast)) {
				Name name = new Name(currentFirst, currentMiddle, currentLast);
				name.setExternalSystemGuid(currentGuid);
				name.setSufix(currentSuffix);
				name.setCompany("1".equals(request.getParameter(SearchAttributes.BUYER_COMPTYPE + i)) ? true : false);
				if(StringUtils.isNotEmpty(currentValidated)) {
					name.setValidated(true);
				} else {
					name.setValidated(false);
				}
				if (!name.isCompany() && 
						StringUtils.isEmpty(currentMiddle) && 
						StringUtils.isEmpty(currentFirst)) {
					//if it is not allowed, it means it really is a company
					boolean isCompany = !CompanyNameExceptions.allowed(currentLast,global.getID(), false); 
					name.setCompany(isCompany);

				}
				if(buyersBefore.contains(name) && name.getUniqueKey().equals(currentNameKey)) {
					//if the name did not change we should keep the SSN4
					NameI tempName = buyersBefore.getNameEqualTo(name);
					tempName.setValidated(name.isValidated());
					boolean added = buyersList.add( tempName );
					if(!added) {
						NameI presentName = buyersList.getNameByUniqueKey(tempName.getUniqueKey());
						if(presentName != null) {
							//keep also validation info, no matter which record was validated
							if(!presentName.isValidated()) {
								if(name.isValidated()) {
									buyersList.remove(presentName);
									buyersList.add(name);
								} 
							}
						}
					}
				} else {
					//buyer changed - forget ssn4
					boolean added = buyersList.add(name);
					if(!added) {
						NameI presentName = buyersList.getNameByUniqueKey(name.getUniqueKey());
						if(presentName != null) {
							//keep also validation info, no matter which record was validated
							if(name.isValidated()) {
								buyersList.remove(presentName);
								buyersList.add(name);
							}
						}
					}
					sa.getSearchPageManualBuyers().add(name);
					if (Integer.parseInt(request.getParameter(SearchAttributes.SEARCH_PRODUCT)) == SearchAttributes.SEARCH_PROD_REFINANCE) {
						sa.getSearchPageManualOwners().add(name); //CR 3870
					}
					//B 4069
					Set<NameI> names = buyersBefore.getNames();
					Iterator<NameI> itr = names.iterator();
					while (itr.hasNext()) {
						for (int j = 0; j < names.size(); j++){
							try {
								String buyerName = itr.next().toString();
								if (j == counter && !buyerName.equals(name.toString())) {
									SearchLogger.info("</div><div>In Search Page, the buyer <b>"
							        		+ buyerName+ "</b> was changed to <b>" 
							        		+ name+ "</b> by <b>"
							        		+ currentUser + "</b> at "
							    			+ SearchLogger.getCurDateTimeCST() 
							    			+ ".<BR>", searchId);
									searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, AtsStandardNames.VARIABLE_BUYER, buyerName, name.toString(), currentUser);
								} 
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					counter ++;
					if (buyersList.size() > buyersBefore.size()) {
						SearchLogger.info("</div><div>In Search Page, was added a new grantee: First= <b>"
				        		+ name.getFirstName()+ "</b>, Middle= <b>" + name.getMiddleName() + "</b>, Last= <b>" + name.getLastName() + "</b> by <b>"
				        		+ currentUser+ "</b> at "
				    			+ SearchLogger.getCurDateTimeCST() 
				    			+ ".<BR>", searchId);
						searchLogPage.addNewGrantee(AtsStandardNames.PAGE_NAME_SEARCH_PAGE,name, currentUser);
					}
				}
				
			}
			if (currentLast == null)
				stop = false;
			i++;
		} while (stop);
		if (request.getParameter("orderTS") != null){
			sa.cleanupNames();
		}
		
		Collection<String> warnings = sa.detectTimeShare();
		for(String text:warnings){
			SearchLogger.info( "<b><i>"+text+"</i></b><br>", searchId);
		}
		
		sa.setAtribute(SearchAttributes.LD_TS_SUBDIV_NAME, sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		sa.setAtribute(SearchAttributes.LD_TS_PLAT_BOOK, sa.getAtribute(SearchAttributes.LD_BOOKNO));
		sa.setAtribute(SearchAttributes.LD_TS_PLAT_PAGE, sa.getAtribute(SearchAttributes.LD_PAGENO));
		
		sa.setAtribute(SearchAttributes.LD_TS_LOT, sa.getAtribute(SearchAttributes.LD_LOTNO));
		sa.setAtribute(SearchAttributes.LD_TS_BLOCK, sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
		
		sa.setAtribute(SearchAttributes.QUARTER_ORDER, request.getParameter(SearchAttributes.QUARTER_ORDER));
		sa.setAtribute(SearchAttributes.QUARTER_VALUE, request.getParameter(SearchAttributes.QUARTER_VALUE));
		sa.setAtribute(SearchAttributes.ARB, request.getParameter(SearchAttributes.ARB));
		sa.setAtribute(SearchAttributes.LD_ABS_NO, request.getParameter(SearchAttributes.LD_ABS_NO));
		
		sa.setSet(true);
		
		sa.updateValidatedProperty();
		
		return numberOfValidatedOwners;
	}

	private static String switchToDefaultFormat(String initialValue, String MM_dd_yyyy_regEx) {
		if (StringUtils.isNotEmpty(initialValue) && RegExUtils.matches(MM_dd_yyyy_regEx ,initialValue) ){
				try {
					Date parse = SearchAttributes.DATE_FORMAT_MM_dd_yyy_PARSER.parse(initialValue);
					initialValue = SearchAttributes.DEFAULT_DATE_PARSER.format(parse);
				} catch (ParseException e) {
					e.printStackTrace();
				}
		}
		return initialValue;
	}

	public static void saveAdditionalInformation(HttpServletRequest request, char c, SearchAttributes sa) {
		String additionalInformation = request.getParameter(SearchAttributes.ADDITIONAL_INFORMATION);
		if (additionalInformation != null) {
		    additionalInformation = additionalInformation.replaceAll("\\$", "USD");
		    
		    additionalInformation = additionalInformation.replace(c, '\'');
		}
		sa.setAtribute(SearchAttributes.ADDITIONAL_INFORMATION, additionalInformation != null ? additionalInformation : "");
	}


	private static void saveSaSubdiv(SearchAttributes sa, HttpServletRequest request) {
		boolean changed = false;
		
		SearchLogFactory logFactory = SearchLogFactory.getSharedInstance();
		SearchLogPage searchLogPage = logFactory.getSearchLogPage(sa.getSearchId());
		
		for( String attribute : new String[] { 
				SearchAttributes.LD_LOTNO, 
				SearchAttributes.LD_SUBLOT, 
				SearchAttributes.LD_SUBDIV_NAME,
				SearchAttributes.LD_SECTION,
				SearchAttributes.LD_SUBDIV_UNIT,
				SearchAttributes.LD_SUBDIV_SEC,
				SearchAttributes.LD_SUBDIV_PHASE,
				SearchAttributes.LD_SUBDIV_BLOCK,
				//SearchAttributes.LD_SUBDIV_TRACT,
				SearchAttributes.LD_SUBDIV_RNG,
				SearchAttributes.LD_SUBDIV_TWN,
				SearchAttributes.QUARTER_ORDER,
				SearchAttributes.QUARTER_VALUE,
				SearchAttributes.ARB,
				SearchAttributes.LD_ABS_NO
				} )	{
			if(isChangedAttribute(request,sa,attribute)) {
				try {
					String currentUser = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCurrentUser().getAttribute(1).toString();
					String newValue = request.getParameter(attribute);
					if(StringUtils.isEmpty(newValue)) {
						SearchLogger.info("</div><div>In Search Page, "+ attributes.get(attribute) +" <b>"
				        		+ sa.getAtribute(attribute)+ "</b> was cleared by <b>"
				        		+ currentUser+ "</b> at "
				    			+ SearchLogger.getCurDateTimeCST() 
				    			+ ".<BR>", sa.getSearchId());
					} else {
						SearchLogger.info("</div><div>In Search Page, "+ attributes.get(attribute) +" <b>"
				        		+ sa.getAtribute(attribute)+ "</b> was changed to <b>" 
				        		+ newValue+ "</b> by <b>"
				        		+ currentUser+ "</b> at "
				    			+ SearchLogger.getCurDateTimeCST() 
				    			+ ".<BR>", sa.getSearchId());
					}
					
					searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, ""+attributes
							.get(attribute), sa.getAtribute(attribute), request.getParameter(attribute), currentUser);
					
					sa.getSearchPageManualFields().put(attribute,request.getParameter(attribute).trim());
				} catch(Exception e) {
					logger.error("Error in saveSaSubdiv", e);
				}
			}
			if(sa.getSearchPageManualFields().containsKey(attribute) &&
					request.getParameter(attribute).equalsIgnoreCase(sa.getAtribute(attribute)) && 
					!request.getParameter(attribute).equalsIgnoreCase(sa.getSearchPageManualFields().get(attribute))
			) {
				sa.getSearchPageManualFields().remove(attribute);
			}
		}
		
		changed = saveAtt(sa, request, SearchAttributes.LD_LOTNO) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBLOT) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_NAME) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_SEC) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_PHASE) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_BLOCK) || changed;
		//changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_TRACT) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_TWN) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_RNG) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SUBDIV_UNIT) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_SECTION) || changed;
		changed = saveAtt(sa, request, SearchAttributes.QUARTER_ORDER) || changed;
		changed = saveAtt(sa, request, SearchAttributes.QUARTER_VALUE) || changed;
		changed = saveAtt(sa, request, SearchAttributes.ARB) || changed;
		changed = saveAtt(sa, request, SearchAttributes.LD_ABS_NO) || changed;
		
		if (changed){
			String s = ""; 
			s += "Lot " + StringUtils.join(StringUtils.splitString(sa.getAtribute(SearchAttributes.LD_LOTNO)),", ");
			s += "SubLot " + StringUtils.join(StringUtils.splitString(sa.getAtribute(SearchAttributes.LD_SUBLOT)),", ");
			s += addText(sa,SearchAttributes.LD_SUBDIV_NAME, " "); 
			s += addText(sa,SearchAttributes.LD_SUBDIV_SEC, ", Township Section "); 
			s += addText(sa,SearchAttributes.LD_SUBDIV_PHASE, ", Phase ");
			s += addText(sa,SearchAttributes.LD_SUBDIV_TWN, ", Township ");
			s += addText(sa,SearchAttributes.LD_SUBDIV_RNG, ", Range ");
			s += addText(sa,SearchAttributes.LD_SUBDIV_BLOCK, ", Block ");
			s += addText(sa,SearchAttributes.LD_SECTION, ", Subdivision Section ");
			s += addText(sa,SearchAttributes.LD_SUBDIV_UNIT, ", Subdivision Unit ");
			s += addText(sa,SearchAttributes.QUARTER_ORDER, ", Quarter Order ");
			s += addText(sa,SearchAttributes.QUARTER_VALUE, ", Quarter Value ");
			s += addText(sa,SearchAttributes.ARB, ", Arb ");
			s += addText(sa,SearchAttributes.LD_ABS_NO, ", Abs ");
			//s += addText(sa,SearchAttributes.LD_SUBDIV_TRACT, ", Tract "); 
			sa.setAtribute(SearchAttributes.LD_SUBDIVISION, s);
		}
		
	}
	
	private static String addText(SearchAttributes sa,  String saAtt, String extra){
		String val = sa.getAtribute(saAtt);
		if (!StringUtils.isStringBlank(val)){
			return extra + val; 
		}else{
			return "";
		}
	}

	private static boolean saveAtt(SearchAttributes sa, HttpServletRequest request, String saAtt){
		boolean changed = false;
		String oldVal = sa.getAtribute(saAtt);
		String newVal = request.getParameter(saAtt);
		if (!oldVal.equals(newVal)){
			sa.setAtribute(saAtt, newVal);
			changed = true;
		}
		return changed;
	}

}



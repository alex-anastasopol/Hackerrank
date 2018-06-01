package ro.cst.tsearch.emailOrder;

import static ro.cst.tsearch.bean.SearchAttributes.ORDERBY_FILENO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;

import mailtemplate.MailTemplateUtils;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.InvalidEmailOrderException;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.ServletServerComm.CompareInstrumentsAfterRecordedDate;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.TSD;
import ro.cst.tsearch.servlet.ValidateInputs;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.TSOpCode;

/**
 * 
 * @author radu bacrau
 *
 */
public class PlaceOrder{
	
	protected static final Category logger = Logger.getLogger(PlaceOrder.class);

	private static String DATE_FORMAT = "MMM d, yyyy";
	private static java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
	
	/**
	 * 
	 * @param search
	 * @param currentUser
	 * @throws IOException
	 */
	public static void placeOrder(Search search, MailOrder mailOrder, boolean setDates, String sourceToLog) throws IOException, BaseException, InvalidEmailOrderException{
	
		//User currentUser = SearchManager.getUser(search);
		User currentUser = mailOrder.savedUser;

		int crtCommId = currentUser.getUserAttributes().getCOMMID().intValue(); //community id
		CommunityAttributes ca = CommunityUtils.getCommunityFromId(crtCommId); //community attributes
		
		InstanceManager.getManager().getCurrentInstance(search.getID()).setCrtSearchContext(search);
        InstanceManager.getManager().getCurrentInstance(search.getID()).setCurrentUser(currentUser.getUserAttributes());       		
        InstanceManager.getManager().getCurrentInstance(search.getID()).setCurrentCommunity(ca);
        
		// set search (from and to) dates
        if(setDates){
			java.util.Calendar date = java.util.Calendar.getInstance();
			date.add(java.util.Calendar.YEAR,-SearchAttributes.YEARS_BACK); // go back N years 
			
			search.getSa().setAtribute(SearchAttributes.FROMDATE, sdf.format(date.getTime()));
			search.getSa().setAtribute(SearchAttributes.TODATE, 
			        CSTCalendar.getDateFromInt(CSTCalendar.getDefaultInitDate("MDY"), "MDY"));
        }
        
        SearchAttributes sa = search.getSa();
		sa.setCommId(crtCommId);	//force community id
        
        SearchAttributes initialSearchAttributes = (SearchAttributes)sa.clone();  
        
        // if one of ORDERBY_FILENO, ABSTRACTOR_FILENO is missing, copy the other one into it
 		if("".equals(sa.getAtribute(SearchAttributes.ORDERBY_FILENO))){
 	        sa.setAtribute(SearchAttributes.ORDERBY_FILENO, sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO));            
 	    } else if("".equals(sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO))){
 	        sa.setAtribute( SearchAttributes.ABSTRACTOR_FILENO, sa.getAtribute(SearchAttributes.ORDERBY_FILENO));
 	    }
        
		// treat update case - may change the FROMDATE and TODATE values
	    ValidateInputs.checkIfUpdate(search, TSOpCode.SUBMIT_ORDER, null); // pass null request
	    
	    boolean cancelFVS = false;
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
			Util.sendMail(null, currentUser.getUserAttributes().getEMAIL(), commAdminEmail, null,
					"FVS Report on " + SearchLogger.getCurDateTimeCST() + " for fileID: " + sa.getAtribute(ORDERBY_FILENO) + ", searchid: " + search.getID(),
					"Missing Effective Date on Original File!\n\nFVS Update not started.");
			
			logger.error("Missing Effective Date on Original File!FVS Update not started.");
			
			SearchManager.removeSearch(search.getID(), true);
			DBManager.deleteSearch(search.getID(),  Search.SEARCH_TSR_NOT_CREATED);
			
			return;
    	}
    	
	    // treat the refinance case
	    ValidateInputs.checkIfRefinance(null, search); // pass null request
		
		// set abstractor and agent to be the same as the user
        search.getSa().setAbstractor(currentUser.getUserAttributes());
		search.setAgent(currentUser.getUserAttributes());
		search.getSa().setOrderedBy(currentUser.getUserAttributes());

	    sa.setAbstractorFileName(search);		
		
		// fill and save the search order
		MailTemplateUtils.fillAndSaveSearchOrder(search, initialSearchAttributes, currentUser);
		
		try {
			HashCountyToIndex.setSearchServer(search, HashCountyToIndex.getFirstServer( 
					search.getProductId(),
					search.getCommId(), 
					Integer.parseInt(sa.getCountyId()) ));				
		}catch(NotImplFeatureException npe) {
			ValidateInputs.checkCountyImpl(search);
		}
        catch(NumberFormatException nfe) {}
		
        // set search type
        if(!search.isCountyImplemented()){ 						
			search.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
        }else{
			search.setSearchType(Search.AUTOMATIC_SEARCH);
		}
		
		DBManager.setSearchOwner(search.getID(),(search.isCountyImplemented() ? -1 : 0));
        
        sa.setObjectAtribute( SearchAttributes.INSTR_LIST, new ArrayList() );
        sa.setObjectAtribute( SearchAttributes.RO_CROSS_REF_INSTR_LIST, new ArrayList<Instrument>() );
        
        sa.cleanupNames();
        sa.cleanupLegal();
        sa.cleanupAddress();
        
        //String sPath = BaseServlet.REAL_PATH + File.separator + "title-search";
        String sPath = BaseServlet.REAL_PATH;
        TSD.initSearch(currentUser, search, sPath, null);
    	String userLogin = "";
        UserAttributes uaLogin = search.getAgent();
        if (uaLogin != null)
        	userLogin = uaLogin.getNiceName();
		
        String commProductName = "";
        try {
			commProductName = (CommunityProducts.getProduct(ca.getID().longValue())).getProductName(sa.getProductId());
		} catch (Exception e) {
		}
        
        String msgStr = "\n</div><div><BR><B>Search ";
		if (org.apache.commons.lang.StringUtils.isNotEmpty(commProductName)){
			msgStr += "(" + commProductName + ") ";
		}
		msgStr += "sent in Dashboard by " + sourceToLog + "</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "</BR></div>\n";
		SearchLogger.infoUpdateToDB(msgStr, search.getID());				
        
		try {			
			// save empty search
			DBManager.saveCurrentSearch(currentUser, search, Search.SEARCH_TSR_NOT_CREATED, null); // pass null request
			
			// set search owner to 0, like in AShread.java
			DBManager.setSearchOwner(search.getID(), 0);
		} catch (SaveSearchException e1) {
			logger.error("Error saving the searched received through " + sourceToLog + "!");
			e1.printStackTrace();
		}
		
		try{
			DBManager.zipAndSaveSearchToDB( search );
		}
		catch(Exception e2){
			logger.error("Error saving the search context received through " + sourceToLog, e2);
			SearchManager.removeSearch(search.getID(), true);
			DBManager.deleteSearch(search.getID(),  Search.SEARCH_TSR_NOT_CREATED);
			return;
		}
		search.fillEmptySearchStatus();
		// start automatic search thread.
		if(search.isCountyImplemented()) {
			synchronized (search) {
				ASMaster.startBackgroundSearch(search, currentUser, ca);			
				//SearchManager.removeSearch(search.getSearchID(), true);
				SearchManager.addBackgroundSearch(search);
			}
		}

	}
}
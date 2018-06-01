package ro.cst.tsearch.threads;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.log.AtsStandardNames;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.reports.invoice.UpdateReportServlet;
import ro.cst.tsearch.search.Decision;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.ServerSearchesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.servers.DataSiteAutomaticPosComparator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.templates.TemplateUtils;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.AutomaticNotifier;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MemoryAllocation;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.ResultsLimitPerModuleWarning;
import com.stewart.ats.base.warning.SkipUpdateWarning;
import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningInfo;
import com.stewart.ats.base.warning.WarningUtils;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

public class ASThread extends Thread {
    private long searchId = -1;
    protected static final Category logger= Logger.getLogger(ASThread.class);
    
    private TSServerInfoModule crtModule = null;
    
	private Search search = null;    
	
    protected ASThread( Search search, User user) {
        this.search = search;
        this.user = user;
        search.setBackgroundSearch(false);
        searchId = search.getSearchID();
        CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
        String state = currentInstance.getCurrentState().getStateAbv();
    	String county = currentInstance.getCurrentCounty().getName();
    	
    	allServerSize = HashCountyToIndex.getAllServersNames(currentInstance.getCommunityId(), state,county,true, search.getProductId()).size();
    	if(allServerSize > 1 && search.getSa().isDataSource()) {
    		allServerSize = 1;
    	}
    	
    	setName("ASThread - " + search.getSearchID() + " on " + state + county + " for community " + currentInstance.getCurrentCommunity().getNAME());
    	touchedServersInAutomatic = new LinkedHashSet<String>();
    }
    private User user = null;
    private boolean backgroundSearch = false;
    private boolean testCaseSearch = false;
    
    protected ASThread(Search search, User user, CommunityAttributes ca) {
        this.search = search;
        search.setBackgroundSearch(true);
        this.user = user;
        this.backgroundSearch = true;
        setName("ASThread BG - " + search.getSearchID());
        searchId = search.getSearchID();
        CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
        String state = currentInstance.getCurrentState().getStateAbv();
    	String county = currentInstance.getCurrentCounty().getName();
    	allServerSize = HashCountyToIndex.getAllServersNames(currentInstance.getCommunityId(), state,county,true, search.getProductId()).size();
    	if(allServerSize > 1 && search.getSa().isDataSource()) {
    		allServerSize = 1;
    	}
    	setName("ASThread - " + search.getSearchID() + " on " + state + county + " for community " + currentInstance.getCurrentCommunity().getNAME());
    	touchedServersInAutomatic = new LinkedHashSet<String>();
    }
    
    /// init DATA ///
    public String continueSearch;
	public String goBackOneLevel;
	public boolean interactive = true;
    /// response DATA ///
    public String sHTML;
    public String forwardTo;
    /////////////////////
    
    public Object notifier = new Object();
    public AutomaticNotifier statusNotifier = new AutomaticNotifier();
    
    public boolean searchFinished = false;
    private long startTime = 0, finishTime = 0;
    private long startTimeServer = 0, finishTimeServer = 0;
    private long currentServerStartTime = 0;
    
    private int allServerSize;
    private int startedFromIndex = 0;
    private int currentServerIndex = 0;
    private Set<String> touchedServersInAutomatic;
    
    public double getCompletion(){
    	if (search.getSearchType() == Search.GO_BACK_ONE_LEVEL_SEARCH)
    	{
		    String county = search.getSa().getAtribute(SearchAttributes.P_COUNTY);
	        String state =  search.getSa().getAtribute(SearchAttributes.P_STATE);
	        int countyInt = Integer.parseInt(county);
			int stateInt = Integer.parseInt(state);
			int productId = search.getProductId();
			Vector<DataSite> vec1;
			int l=1;
			try {
				vec1 = HashCountyToIndex.getAllDataSites(InstanceManager.getManager().getCommunityId(search.getID()),
						ro.cst.tsearch.servers.parentsite.State.getState(
								stateInt).getStateAbv(), County.getCounty(
								countyInt).getName(), false, productId);
				DataSite[] vec = getGBSortedByAutomaticPosition(vec1, productId);
				l = vec.length;
			} catch (Exception e) {
				// TODO: handle exception
			}
			return ((double)currentServerIndex)/((double)l);		
    	} else {
    		if(search.getSa().isDataSource()) {
    			return 0.6;
    		} else if(startedFromIndex == -1) { //undefined
    			return ((double)currentServerIndex - 1)/((double)allServerSize);
    		} else {
    			return ((double)currentServerIndex - 1 + startedFromIndex)/((double)allServerSize);
    		}
    	}
    }
    
    public void run() {
        
        /*
		 - this was intended to protect the OCR server from to many orders 
		in a short period of time but now we make full OCR just for CA
		try{
        	int tries = 0;
	    	while( ASMaster.getNoOfBackgroundAliveSearches()>30 && backgroundSearch && search!=null && search.isBackgroundSearch()){
	    		try{
	    			Random rnd = new Random(System.currentTimeMillis());
	    			Thread.sleep(60000 *  rnd.nextInt(5) + 60000);//min 1 minute, max 5 minutes
	    			tries++;
	    			if(tries==10) break;//do not trust ASMaster detections :) 
	    		}
	    		catch (InterruptedException e) {
	    			break;
				}
	    	}
        }
        catch(Exception e){}*/
    	
    	MemoryAllocation.getInstance().addStartedSearch();
    	
        startTime = System.currentTimeMillis();

        search.clearCompanyNameWarning();
        // search in progress 
        if (backgroundSearch || search.isUpdate()) {
            DBManager.setSearchOwner(search.getID(), -1);
        }
        
        TransferI transferWithNameForGoBack = null;
        if("true".equalsIgnoreCase(goBackOneLevel)){
        	DocumentsManagerI manager = search.getDocManager();
        	try {
        		manager.getAccess();
        		transferWithNameForGoBack = manager.getLastTransfer(true);
        	} catch (Exception e) {
				e.printStackTrace();
			}finally{
				manager.releaseAccess();
			}
        }
        
        
        forwardTo = search(continueSearch, goBackOneLevel,searchId);
        SearchAttributes sa = search.getSa();
        if (sa != null && "0".equals(sa.getAtribute(SearchAttributes.LEGAL_DESCRIPTION_STATUS))) {
        	//nu a fost actualizat explicit, actualizam noi acum
        	String legalDescription = TemplateBuilder.getDefaultLegalDescription(search, null);
        	if(!sa.getLegalDescription().isEdited()){
        		sa.getLegalDescription().setLegal(legalDescription);
        	}
        	DBManager.setSearchLegalDescription(search, legalDescription, 0);
        }
        
        boolean searchSaved = false;        
        CurrentInstance	ci = InstanceManager.getManager().getCurrentInstance(searchId);
        if( !testCaseSearch )
        {
	        if (backgroundSearch) {
	            
	        	//let's do automatic redistribution
	        	long automaticDistributedUserId = 0;
	        	UserManagerI userManager = null;
	        	try {
	        		userManager = UserManager.getInstance();
	        		userManager.getAccess();
	        		automaticDistributedUserId = userManager.getCapableAbstractor(
	        				search, user.getUserAttributes().getCOMMID().intValue());	
	        	} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if(userManager != null) {
						userManager.releaseAccess();
					}
				}
	        	
	            // save search after automatic search (database and disk)
	            try {
	            	ro.cst.tsearch.user.UserAttributes abstractorAttributes = user.getUserAttributes();
	            	if(automaticDistributedUserId > 0) {
	            		try {
	            			userManager.getAccess();
	            			abstractorAttributes = userManager.getOldUserAttributes(automaticDistributedUserId);
	            			SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
	            			SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
	            			SearchLogger.info("Search assigned automatically to Abstractor: <b>" + 
	            					abstractorAttributes.getLOGIN() + "</b> " + 
	            					SearchLogger.getTimeStampAndLocation(searchId), searchId);
	            			searchLogPage.assignSearchToSmbd(AtsStandardNames.VARIABLE_ABSTRACTOR, abstractorAttributes.getLOGIN());
	            			
	            			if(abstractorAttributes!=null && UserAttributes.OS_SUS.equals(abstractorAttributes.getOUTSOURCE())){
	            				byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(searchId, ro.cst.tsearch.servlet.FileServlet.VIEW_ORDER, false);
			        			if(orderFileAsByteArray!=null){
			        				String orderFile = new String(orderFileAsByteArray);
				        			UpdateReportServlet.sendMail(orderFile, abstractorAttributes, searchId, false);
			        			}
	            			}
	            		} catch (Exception e) {
	    					e.printStackTrace();
	    				} finally {
	    					if(userManager != null) {
	    						userManager.releaseAccess();
	    					}
	    				}
	    				DBManager.saveCurrentSearch(user, search, Search.SEARCH_TSR_NOT_CREATED, null);
	    				DBManager.setSearchOwnerLockedUnlocked(search.getID(), automaticDistributedUserId, abstractorAttributes.isAUTO_ASSIGN_SEARCH_LOCKED());
	    				
	            	} else {
	            		DBManager.saveCurrentSearch(user, search, Search.SEARCH_TSR_NOT_CREATED, null);	
	            		DBManager.setSearchOwner(search.getID(), automaticDistributedUserId);
	            	}
					
					searchSaved = true;
				} catch (SaveSearchException e) {
					e.printStackTrace();
					logger.error("NU TREBUIE SA SE ARUNCE EXCEPTIA AICI", e);
				}
	            
	            boolean checkAutoTSRAllowed = isAutoTSRAllowed();
				if(checkAutoTSRAllowed ){
					/*B4548*/
					UtilForGwtServer.startCreatingTSR(searchId, user, URLMaping.path,false);
		        } else {
		        	// remove search from SearchManager
		        	SearchManager.removeSearch(search.getSearchID(), true);
		        }
				// finish ASThread
	            ASMaster.removeSearch(search);
	            
	        } else  {
	            // save search after automatic search (database and disk)
	            try {
	            	
					DBManager.saveCurrentSearch(user, search, Search.SEARCH_TSR_NOT_CREATED, null);
					DBManager.setSearchOwner(search.getID(), ci.getCurrentUser().getID().longValue());
					searchSaved = true;
				} catch (SaveSearchException e) {
					logger.error("NU TREBUIE SA SE ARUNCE EXCEPTIA AICI");
					e.printStackTrace();
				}
	            //StartTS.saveSearch(getSearch());
	        }
        }
        else
        {
            forwardTo = "";
        }
        
        synchronized (notifier) {
            searchFinished = true;
            notifier.notify();
        }

		//save search context to database
		if( searchSaved ){
			AsynchSearchSaverThread.getInstance().saveSearchContext( search );
		}
        
        finishTime = System.currentTimeMillis();
        
        MemoryAllocation.getInstance().addEndedSearch();
         
        if("true".equalsIgnoreCase(goBackOneLevel)){
        	DocumentsManagerI manager = search.getDocManager();
        	try {
        		manager.getAccess();
        		TransferI transfer = manager.getLastTransfer(true);
        		boolean doGoBack = false;
        		
        		if(transfer != null) {
        			if(transferWithNameForGoBack == null 
        					|| !transfer.equals(transferWithNameForGoBack)) {
	        			if(transfer.getGrantee().size()>0){
	    	        		for(NameI name:transfer.getGrantee().getNames()){
	    	        			String cand = name.getFullName();
	    	        			//BIG 7108
	    	        			String []FNMA = {"Federal National Mortgage Association",
	    	        					"FEDERAL NATL MTG ASSN FNMA",
	    	        					"FEDERAL NATIONAL MORTGAGE ASSN"};
	    	        			for(String ref:FNMA){ 
	    		        			if(GenericNameFilter.isMatchGreaterThenScoreForSubdivision(cand, ref, 0.9d)||GenericNameFilter.isMatchGreaterThenScoreForSubdivision(ref, cand, 0.9d)){
	    		        				doGoBack = true;
	    		        				break;
	    		        			}
	    	        			}
	    	        		}
	            		}
        			}
        		}
        		
        		
        		if(doGoBack){
        			UtilForGwtServer.startGoBackSearch(searchId, 1, user);
        		}
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				manager.releaseAccess();
			}
        }
        
    }
    
    /**
     * Checks to see if conditions for auto TSR as the end of automatic are met
     * @return
     */
    private boolean isAutoTSRAllowed() {
    	
    	UserAttributes agent = search.getAgent();
    	
    	if(agent == null) {
    		return false;
    	}
    	
    	if(agent.isAutoExportTsr()) {
    		return true;
    	}
    	
    	boolean isFVSAllowed = false;
    	if (search.getSa().isFVSUpdateProduct()){
    		SearchAttributes sa = search.getSa();
    		if (sa.isFVSAutoLaunched()){
    			if (sa.isLastScheduledFVSUpdate()){
    				isFVSAllowed = true;
    			}
    		} else{
    			isFVSAllowed = true;
    		}
    	}
    	if((search.getSa().isUpdate() || isFVSAllowed) && agent.isAUTO_UPDATE()) {
    		boolean passAutoUpdateRule = internalIsAutoTSRRoLikeAllowed();
            if(passAutoUpdateRule) {
            	if (StateContants.FL_STRING.equals(search.getStateId()) && ServerConfig.getFtsCommunityId() == search.getCommId()) {
        			long parentSearchId = search.getParentSearchId();
        			if (parentSearchId > 0) {
        				Search parentSearch = SearchManager.getSearchFromDisk(parentSearchId);
        				if (parentSearch != null && parentSearch.getStartDate() != null) {

        					Date threshold = ServerConfig.getFtsAutoUpdateThreshold();
        					Date parentDateFormatted = FormatDate.getDateFromGMTDate(parentSearch.getStartDate());

        					if (threshold != null && parentDateFormatted != null && parentDateFormatted.after(threshold)) {
        						return true;
        					}
        				}
        			}
        		} else {
        			return true;
        		}
            }
    	}
    	
		return false;
	}

    /**
     * Strictly analyzing ROlike information from the search
     * @return true if non of the rules about ROlike data are broken
     */
	private boolean internalIsAutoTSRRoLikeAllowed() {
		DocumentsManagerI manager = search.getDocManager();
			
		if(TemplateUtils.foundRoDocs(manager, false)) {
			return false;
		}
		
		
		boolean atLeastOneRealRoLike = false;
		for (Integer siteType : GWTDataSite.REAL_RO_LIKE_TYPE) {
			String serverStatusFromCityChecked = search.getServerStatusFromCityChecked(siteType);
			if(serverStatusFromCityChecked != null && 
					(serverStatusFromCityChecked.contains(Search.SKIPPED) 
							|| serverStatusFromCityChecked.contains(Search.TIME_OUT)
							|| serverStatusFromCityChecked.contains("not responding")
							|| serverStatusFromCityChecked.contains("site warning"))) {
				return false;
			}
			if(!atLeastOneRealRoLike && !Search.SERVER_NA_MSG.equals(serverStatusFromCityChecked)) {
				atLeastOneRealRoLike = true;
			}
		}
		
		if(!atLeastOneRealRoLike) {
			return false;
		}
      
		for(NameI buyer : search.getSa().getBuyers().getNames()) {
			if(buyer.isCompany() 
					&& buyer.getNameFlags().isNewFromOrder() 
					&& !CompanyNameExceptions.allowed(buyer.getLastName(), search.getID(), false)) {
				return false;
			}
		}
		return true;
	}
	private boolean skipCurrentSite = false;
    public void setSkipCurrentSite(boolean value) {
    	skipCurrentSite = value;
    }
    public boolean getSkipCurrentSite() {
    	return skipCurrentSite;
    }
    
    private boolean stopAutomaticSearch = false;
    public void setStopAutomaticSearch(boolean value) {
    	stopAutomaticSearch = value;
    }
    public boolean getStopAutomaticSearch() {
    	return stopAutomaticSearch;
    }
    
    private String search(String continueSearch, String goBackOneLevel,long searchId) {
        
        String result = "";
        boolean initialOrderTooOld = false;
        
        IndividualLogger.initLog(searchId);
		SearchLogger.init(searchId);
		SearchLogger.info("</div><div>", searchId);
		SearchLogFactory logFactory = SearchLogFactory.getSharedInstance();
		SearchLogPage searchLogPage = logFactory.getSearchLogPage(searchId);
		if ("true".equals(goBackOneLevel)) {
			IndividualLogger.info("Starting Automatic Search - Go Back", searchId);
			SearchLogger.info("<BR><B>Starting Go Back Automatic Search</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
			searchLogPage.addInfoMessage("start.search.back");
		} else if(getSearch().getSa().isDateDown()) {
			IndividualLogger.info("Starting Automatic Search - DateDown", searchId);
			SearchLogger.info("<BR><B>Starting Automatic Search - DateDown</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
			searchLogPage.addInfoMessage("start.search.datedown");
		} else if(getSearch().getSa().isDataSource()) {
			IndividualLogger.info("Starting Automatic Search - DataSource", searchId);
			SearchLogger.info("<BR><B>Starting Automatic Search - DataSource</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
			searchLogPage.addInfoMessage("start.search.datasource");
		} else {
			IndividualLogger.info("Starting Automatic Search", searchId);
			SearchLogger.info("<BR><B>Starting Automatic Search</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
			searchLogPage.addInfoMessage("start.search");
		}

        try {
        	try {
        		if (search.isUpdate() && !interactive && isBackgroundSearch()){
        			String originalSearchIsTooOld = search.getSa().getAtribute(SearchAttributes.ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR);
        			if ("true".equalsIgnoreCase(originalSearchIsTooOld)){
        				initialOrderTooOld = true;
        			}
        		} 
        		
        		search.clearValidatedLinks();
        		search.clearVisitedLinks();
        		search.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "");
        		search.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, "");
        		search.getSearchFlags().getWarningList().remove(new WarningInfo(Warning.WARNING_NO_LEGAL_SEARCH_WAS_PERFORMED_ID));
        		
        		if (initialOrderTooOld){
        			SkipUpdateWarning warning = new SkipUpdateWarning(Warning.WARNING_UPDATE_SKIPPED_BECAUSE_PARENT_ORDER_IS_TOO_OLD,
							ServerConfig.getAllowUpdatePeriod());
        			
					search.getSearchFlags().addWarning(warning);
					SearchLogger.info("<br/><span class='error'><b>Automatic stopped because initial order is too old</b> </span>" + SearchLogger.getTimeStamp(searchId) + "<br/>", searchId);
        		} else{
        			if (search.isUpdate()){

        				search.removeWarning(SkipUpdateWarning.class);
        				search.getSa().setAtribute(SearchAttributes.ORIGINAL_SEARCH_OLDER_THAN_ONE_YEAR, "false");
        			}
        			search.performAdditionalProcessingBeforeRunningAutomatic(this);
        			result = doSearch(continueSearch, goBackOneLevel);
        			search.applyQARules();
            		search.performAdditionalProcessingAfterRunningAutomatic(this);
            		search.ocrDocuments();
            		search.clearValidatedLinks();
            		search.clearVisitedLinks();
        		}
        		search.getSa().setCertificationDate();	//force calculation since it's possible the search is not opened on this day
        		WarningUtils.calculateWarnings(searchId);
        		search.analyzeReportForFVSUpdates();
        	} catch( Exception e ) {
        		e.printStackTrace();
        	}
        	
            ////////////////////////////////

            DataSite serverDataSite = null;
            boolean currentServerError = false;
            int productId = search.getProductId();
            try
            {
            	serverDataSite = HashCountyToIndex.getDataSite(
            			InstanceManager.getManager().getCommunityId(search.getID()),
            			Integer.parseInt(getSearch().getP1()),
            			Integer.parseInt(getSearch().getP2()));
                //serverName = TSServersFactory.GetServerInstance(getSearch().getP1(),getSearch().getP2(),getSearch().getID()).toString().replaceAll("[\\]\\[ ]","");
            	if (serverDataSite.equals("")){
            		currentServerError = true;
            	}
            }
            catch( Exception e )
            {
                currentServerError = true;
                logger.error("Error received when getting current server with parameters: searchId: " 
                		+ search.getID() + ", p1=" + search.getP1() + ", p2=" + search.getP2(), e);
            }
            
            if( ! currentServerError )
            {
                int serverIndex = serverDataSite.getSiteTypeInt();
                
                if (URLMaping.PARENT_SITE_RESPONSE.equals(result)) {
                
                    if ( backgroundSearch ) 
                    {
    
                        search.updateSearchStatus(serverIndex, Search.SKIPPED);
                        try {
                            
                            HashCountyToIndex.setSearchServer(getSearch(), 
                            		HashCountyToIndex.getNextServer( productId, search.getCommId(), serverDataSite ).getSiteTypeInt() );
                            getServer(getSearch(), BaseServlet.FILES_PATH);
                            
                        } catch (Exception e) {
                            try {
                                HashCountyToIndex.setSearchServer(getSearch(), 
                                	 HashCountyToIndex.getNextServer( productId, search.getCommId(), serverDataSite ).getSiteTypeInt() );
                            } catch (Exception ee) {}
                        }
                        
                        search.setSearchIterator(null);
                        
                        DBManager.setSearchOwner(search.getID(), -1);
                        
                        result = search(continueSearch, goBackOneLevel,searchId);
                    }
                    else
                        getSearch().setTS_SEARCH_STATUS(Search.SEARCH_STATUS_N);
                
                } else
                    getSearch().setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
            } else {
            	getSearch().setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            
        }
        
        SearchLogger.info("</div><hr/>", searchId);
        searchLogPage.addHR();
        if (search.getSearchType() == Search.GO_BACK_ONE_LEVEL_SEARCH ||(goBackOneLevel!=null&& goBackOneLevel.equals("true"))) {
        	GBManager gbm = (GBManager) search.getSa().getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
        	if(gbm.getErr()!=null&&gbm.getErr().size()>0){
            for (Iterator iterator = gbm.getErr().keySet().iterator(); iterator.hasNext();) {
				String  idDoc = (String) iterator.next();
				String  error=gbm.getErr().get(idDoc);				
         		IndividualLogger.info("<div>"+error+"<br></div>",searchId);
        		SearchLogger.info("<div>"+error+"<br></div>", searchId);
        		searchLogPage.addErrorMessage(error);
             }	
        	}
        	IndividualLogger.info("Automatic Search finished - Go Back ", searchId);
			IndividualLogger.finishLog(searchId);
			SearchLogger.info("<B>Automatic Go Back Search Completed</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
			searchLogPage.addInfoMessage("finish.search.back");
			SearchLogger.finish(searchId);
        } else if(getSearch().getSa().isDateDown()) {
				IndividualLogger.info("Automatic Search finished - DateDown", searchId);
				SearchLogger.info("<BR><B>Automatic DateDown Search Completed</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
				searchLogPage.addInfoMessage("finish.search.datedown");
        } else if(getSearch().getSa().isDataSource()) {
			IndividualLogger.info("Automatic Search finished - DataSource", searchId);
			SearchLogger.info("<BR><B>Automatic DataSource Search Completed</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
			searchLogPage.addInfoMessage("finish.search.datasource");
		} else {
			IndividualLogger.info("Automatic Search finished", searchId);
			IndividualLogger.finishLog(searchId);
			SearchLogger.info("<B>Automatic Search Completed</B> " + SearchLogger.getTimeStamp(searchId) + ".<BR>", searchId);
			searchLogPage.addInfoMessage("finish.search");
			SearchLogger.finish(searchId);
		}
        SearchLogger.info("<div>", searchId);
        search.getSa().setAtribute(SearchAttributes.ADDITIONAL_SEARCH_TYPE, "");
        return result;
    }
    
    public Search getSearch() {
        return search;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getFinishTime() {
        return finishTime;
    }
        
    //long lastNotifyTime = 0;
    private String doSearch(String continueSearch, String goBackOneLevel) throws Exception {
        int defaultSelectedRadioButton = 0;		// set radio button on AO
        //this is set after the search has finished
        //InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().setSearchType(Search.AUTOMATIC_SEARCH);
        String county = search.getSa().getAtribute(SearchAttributes.P_COUNTY);
        String state =  search.getSa().getAtribute(SearchAttributes.P_STATE);
        CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
        
		if ("0".equals(county )) // nici un county selectat - Select One -
		{
		   search.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_N);
		   search.setSearchStarted(false);
		   search.setSearchType(Search.SEARCH_NONE);
		   
		   return URLMaping.TSD;
		}
		int productId = search.getProductId();
		int countyInt = Integer.parseInt(county);
		int stateInt = Integer.parseInt(state);
		try {
			defaultSelectedRadioButton = HashCountyToIndex.getFirstServer(
					productId, search.getCommId(),
					countyInt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//get string params		
		String realPath = BaseServlet.FILES_PATH;
		SearchAttributes sa = search.getSa();

		if (logger.isDebugEnabled()){
			logger.debug("Start AutomaticSearch SearchType =" + search.getSearchType());
			logger.debug("SearchFinished =" + sa.getAtribute(SearchAttributes.SEARCHFINISH));
		}
		search.setSubdivisionExceptionPrintedRO(false);
		if(search.getSearchType() == Search.AUTOMATIC_SEARCH) {
			sa.setAtribute( SearchAttributes.SEARCHFINISH ,"false");
		}
		
		search.setOrigSA(new SearchAttributes(sa,search.getID()));

		int oldSearchType = search.getSearchType();
		
        
		if (onGoBackOneLevelSearch(goBackOneLevel)){
			Vector<DataSite> vec1 = HashCountyToIndex.getAllDataSites(
					currentInstance.getCommunityId(),
					ro.cst.tsearch.servers.parentsite.State.getState(stateInt).getStateAbv(),
					County.getCounty(countyInt).getName(), false, productId );
			DataSite[] vec = getRoLikeSortedByAutomaticPosition(vec1, productId);
			if(vec.length>0){
				DataSite toGobackSite = vec [0];
				int type = toGobackSite.getCityCheckedInt();
				HashCountyToIndex.setSearchServer(getSearch(), type);
				defaultSelectedRadioButton  = type;
				sa.advanceToPreviousOwner(searchId,search.getGoBackType());
				sa.setAtribute( SearchAttributes.SEARCHFINISH ,"false");
				search.setSearchType(Search.GO_BACK_ONE_LEVEL_SEARCH);
				search.clearVisitedLinks();
				search.clearValidatedLinks();
			}

		}
		
		int searchType = search.getSearchType();

		logger.debug("Stating ASThread.doSearch: getInMemoryDocsCount() = " + search.getInMemoryDocsCount());

		boolean continueSearchFlag = false;
		
		boolean repeatSomeSearchesFlag = false;
		
		
		startedFromIndex = -1;	//undefined
		
		HashSet<String> searchedModulesKeys = new HashSet<String>();
		
		LinkedHashSet<String> repeatDataSource = new LinkedHashSet<String>();
		LinkedHashSet<TSInterface> repeatDataSrc = new LinkedHashSet<TSInterface>();
		
		
		int siteCycleIndex = -1;
		
		do { //start automatic search; go server by server
			siteCycleIndex ++;
			TSInterface intrfServer = null;
			try {
				if (repeatSomeSearchesFlag){
					String p2 = "0";
					if (repeatDataSource != null && repeatDataSource.size() > 0){
						for (Iterator<String> iterator = repeatDataSource.iterator(); iterator.hasNext();) {
							p2 = iterator.next();
							intrfServer = TSServersFactory.GetServerInstance(search.getP1(),	p2,	search.getID());
							intrfServer.modifyDefaultServerInfo();
							intrfServer.setServerForTsd(search, realPath);
							
							if(logger.isDebugEnabled()) {
								StringBuilder sbLog = new StringBuilder(100);
								sbLog.append("ASThread: SearchId = [").append(searchId)
									.append("], siteCycleIndex = [").append(siteCycleIndex)
									.append("], intrfServer = [").append(intrfServer)
									.append("], search.getP1() = [").append(search.getP1())
									.append("], search.getP2() = [").append(search.getP2())
									.append("], search.getCitychecked() = [").append(search.getCitychecked())
									.append("], repeating = [").append(true)
									.append("]");
								logger.debug(sbLog.toString());
							}
							
							
							break;
						}
					} else {
						repeatSomeSearchesFlag = false;
						if(logger.isDebugEnabled()) {
							StringBuilder sbLog = new StringBuilder(100);
							sbLog.append("ASThread: SearchId = [").append(searchId)
								.append("], siteCycleIndex = [").append(siteCycleIndex)
								.append("], intrfServer = [").append(intrfServer)
								.append("], search.getP1() = [").append(search.getP1())
								.append("], search.getP2() = [").append(search.getP2())
								.append("], search.getCitychecked() = [").append(search.getCitychecked())
								.append("], repeating = [").append("not this time")
								.append("]");
							logger.debug(sbLog.toString());
						}
					}
				} else {
					if(logger.isDebugEnabled()) {
						StringBuilder sbLog = new StringBuilder(100);
						sbLog.append("ASThread: SearchId = [").append(searchId)
							.append("], siteCycleIndex = [").append(siteCycleIndex)
							.append("], intrfServer = [").append(intrfServer)
							.append("], search.getP1() = [").append(search.getP1())
							.append("], search.getP2() = [").append(search.getP2())
							.append("], search.getCitychecked() = [").append(search.getCitychecked())
							.append("], no repeating and getting server");
						logger.debug(sbLog.toString());
					}
					intrfServer = getServer(search,  realPath);
					if (intrfServer.isRepeatDataSource() && !onGoBackOneLevelSearch(goBackOneLevel)){
						repeatDataSrc.add(intrfServer);
						repeatDataSource.add(search.getP2());
					}
				}
			} catch (Exception e) {
				logger.error("Error getting current server for parameters searchId=" + search.getID() + " and realPath = " + realPath , e);
				throw e;	//just for logging, the rest is the same as before this try catch
			}
		    
			if(logger.isDebugEnabled()) {
				StringBuilder sbLog = new StringBuilder(100);
				sbLog.append("ASThread: SearchId = [").append(searchId)
					.append("], siteCycleIndex = [").append(siteCycleIndex)
					.append("], intrfServer = [").append(intrfServer)
					.append("], search.getP1() = [").append(search.getP1())
					.append("], search.getP2() = [").append(search.getP2())
					.append("], search.getCitychecked() = [").append(search.getCitychecked())
					.append("]");
				logger.debug(sbLog.toString());
			}
		    intrfServer.performAdditionalProcessingBeforeRunningAutomatic();
		    
		    if (onGoBackOneLevelSearch(goBackOneLevel)) {
		    	if( TSServer.isGBLike(intrfServer.getServerID())){
		    		currentServerIndex++;
		    	}
		    }
		    else {
		    	currentServerIndex++;
		    	if(startedFromIndex == -1){ //if still undefined
		        	Vector<DataSite> servers = HashCountyToIndex.getAllDataSites(
		        			currentInstance.getCommunityId(),
		        			currentInstance.getCurrentState().getStateAbv(),
		        			currentInstance.getCurrentCounty().getName(),
		        			true, productId);
		        	Collections.sort(servers, new DataSiteAutomaticPosComparator());
		        	int i = 0;
					for (DataSite server : servers) {
						if(((TSServer)intrfServer).getCurrentServerName().equals(server.getName())){
							startedFromIndex = i;
							break;
						} else {
							i ++;
						}
						
					}				
				}
		    }
		    DataSite dat = null;
		    try{
		    	dat = HashCountyToIndex.getDateSiteForMIServerID(
		    			currentInstance.getCommunityId(), 
		    			intrfServer.getServerID());
		    	try {
		    		if(dat != null) {
			    		int p2 = Integer.parseInt(dat.getP2());
				    	if (repeatSomeSearchesFlag){
				    		search.setCitychecked(p2);
				    	}
		    		}
		    	} catch(Exception e){e.printStackTrace();}
		    }
		    catch(Exception e){e.printStackTrace();}
		    if(dat!=null){
			    if(!Util.isSiteEnabledAutomaticForProduct(productId, dat.getEnabled(currentInstance.getCommunityId()))){
			    	setSkipCurrentSite(true);
			    }
		    } else {
		    	setSkipCurrentSite(true);
		    }
		    
		    if(logger.isDebugEnabled()) {
				StringBuilder sbLog = new StringBuilder(100);
				sbLog.append("ASThread: SearchId = [").append(searchId)
					.append("], siteCycleIndex = [").append(siteCycleIndex)
					.append("], intrfServer = [").append(intrfServer)
					.append("], search.getP1() = [").append(search.getP1())
					.append("], search.getP2() = [").append(search.getP2())
					.append("], search.getCitychecked() = [").append(search.getCitychecked())
					.append("], currentServerIndex = [").append(currentServerIndex)
					.append("], isSkipCurrentSite = [").append(getSkipCurrentSite())
					.append("]");
				logger.debug(sbLog.toString());
			}
		    		    		   
			// mark start time for this server
			((TSServer)intrfServer).setStartTime(System.currentTimeMillis());
            currentServerStartTime = System.currentTimeMillis();

            ServerResponse result= null;
			ServerResponse crtResult = null;
			ServerResponse recursiveAnalyzeResponseResult = null;
			ServerSearchesIterator ssi = null;
				
			ssi = getSearchIterator(continueSearch, goBackOneLevel, search, intrfServer);
			//intrfServer = 
			
			//	vezi VSSHistory
			if (intrfServer.getServerID() != ssi.getIntrfServer().getServerID()) //replace the server with the one saved for Interactive Search
			{
				search.setSearchIterator(null);
				ssi = getSearchIterator(continueSearch, goBackOneLevel, search, intrfServer);
			}
	
			boolean loggedStartMessage = false;
			boolean hasSearchedOnThisServer = false;
			
			int innerCycleIndex = -1;
			try {
				do { // performing the search until I find the first results and I save them
					// only for register I have to continue search even after finding and saving results
					// sometimes I must go and display partial results to user
					innerCycleIndex ++;
					ssi.resetPreviousResponse();
					int initialDocumentsListSize = search.getDocManager().getDocumentsCount();
					List<DocumentI> documentsList = null;
					
					while (ssi.hasNext(searchId) && this.isStarted() && ((TSServer)intrfServer).continueSeach() &&
                            ((TSServer)intrfServer).continueSeachOnThisServer() && !getSkipCurrentSite() && !getStopAutomaticSearch())
					{
						if( search.maxDocLimitReached() ){
							break;
						}
						getTouchedServersInAutomatic().add(intrfServer.getDataSite().getSiteTypeAbrev());
						ssi.goToNext();
						TSServerInfoModule module = (TSServerInfoModule) ssi.current();
						if (module != null && module.isSkipModule()){
							continue;
						}
						if(logger.isDebugEnabled()) {
							StringBuilder sbLog = new StringBuilder(100);
							sbLog.append("ASThread: SearchId = [").append(searchId)
								.append("], siteCycleIndex = [").append(siteCycleIndex)
								.append("], innerCycleIndex = [").append(innerCycleIndex)
								.append("], intrfServer = [").append(intrfServer)
								.append("], moduleName = [").append(module.getName())
								.append("], moduleIdx = [").append(module.getModuleIdx())
								.append("]");
							logger.debug(sbLog.toString());
						}
						
						int searchedModulesKeysSizeBefore = searchedModulesKeys.size();
						String moduleKey = intrfServer.getServerID() + "_" +  module.getUniqueKey();
						if(searchedModulesKeys.contains(moduleKey) && !repeatSomeSearchesFlag) {
							continue;
						} else {
							searchedModulesKeys.add(moduleKey);
						}
						int searchedModulesKeysSizeAfter = searchedModulesKeys.size();
						
						if(!loggedStartMessage && searchedModulesKeysSizeAfter>searchedModulesKeysSizeBefore) {
							loggedStartMessage = true;
							hasSearchedOnThisServer = true;
							intrfServer.logStartServer(null);
						}
						
						initialDocumentsListSize = search.getDocManager().getDocumentsCount();
						documentsList = null;
						DocumentsManagerI manager = search.getDocManager();
						try {
							manager.getAccess();
							documentsList = manager.getDocumentsList();
						} finally {
							manager.releaseAccess();
						}
						
						
						synchronized (this) {
							crtModule = module;
						}
						
						//if (System.currentTimeMillis() - lastNotifyTime > 2000) {
						
							
							synchronized( statusNotifier )
							{
                                statusNotifier.setServerChanged();
							    statusNotifier.notifyAll();
							}
							
							//lastNotifyTime = System.currentTimeMillis();
						//}

						
						
						//set the current server for this module
						//we need this to know what server is the module using
						module.setTSInterface( intrfServer );
						
						ServerResponse sr = intrfServer.performAction(TSServer.REQUEST_SEARCH_BY,  null, module, new SearchDataWrapper(sa));

						crtResult = sr;
						
						ssi.manageResponse(sr, module, searchId);
						
						if ((ssi.timeToStop(new Decision(sr,intrfServer),searchId)) || 
								(ssi.mustStopToShowPartialResults(searchId))){
							break;
						}
						
						
						
					}
					result= ssi.getLastGoodResponse();
					String srcType = "";
					if(crtResult != null&& srcType.isEmpty() && (crtModule.getIteratorType() == ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER ||
							crtModule.getIteratorType() == ModuleStatesIterator.TYPE_OCR)) {
						((TSServer)intrfServer).setOcrAdded(true);
						srcType = "IP";
					}
					else{
						((TSServer)intrfServer).setOcrAdded(false);
					}
					/*
					if(crtResult != null && crtResult.isFakeResponse()) {
						
						srcType = intrfServer.getDataSite().getSiteTypeAbrev();
						((TSServer)intrfServer).createFakeHtmlResponse(crtModule.getCurrentSearchInstrument(searchId), srcType);
						
					} else 
					*/	
					if (intrfServer.isTimeToSave(result)){
						//saving all the document and all the links
						LinkInPage selflink = result.getParsedResponse().getPageLink();
						if (( selflink != null) && (selflink.getActionType() == TSServer.REQUEST_SAVE_TO_TSD))  {
							//am rezultat unic si trebuie sa-l salvez
							if (logger.isDebugEnabled())
								logger.debug("am obtinut un fisier unic...trebuie sa-l salvez  " + selflink );
							intrfServer.performLinkInPage(selflink);
						}else{
							if (logger.isDebugEnabled())
								logger.debug("analizez " + result.getParsedResponse().getResultRows().size()  + " rows  ");
							recursiveAnalyzeResponseResult = intrfServer.recursiveAnalyzeResponse(result);
						}
						
						int finalDocumentsListSize = search.getDocManager().getDocumentsCount();
						if(finalDocumentsListSize - initialDocumentsListSize > Search.MAX_NO_OF_DOCUMENTS_SAVED_PER_MODULE 
								&& documentsList != null) {
							//we need to delete all saved documents generated by this module
							DocumentsManagerI manager = search.getDocManager();
							try {
								manager.getAccess();
								manager.replaceAllDocumentsWith(documentsList);
							} finally {
								manager.releaseAccess();
							}
							
							String moduleName = crtModule.getLabel();
							if(StringUtils.isEmpty(moduleName)) {
								moduleName = "Unknown search module";
							}
							
							ResultsLimitPerModuleWarning warning = new ResultsLimitPerModuleWarning(
									Warning.WARNING_RESULTS_LIMIT_PER_MODULE_ID,
									intrfServer.getDataSite().getSiteTypeAbrev(),
									finalDocumentsListSize - initialDocumentsListSize,
									Search.MAX_NO_OF_DOCUMENTS_SAVED_PER_MODULE, 
									moduleName); 
							SearchLogger.info("<br><font color=\"red\"><b>WARNING: </b>" + warning.toString() + "</font><br>", searchId);
							getSearch().getSearchFlags().addWarning(warning);
							
						}
						
						if (search.isUpdate()) {
						    try {
								DBManager.saveCurrentSearch(user, search, Search.SEARCH_TSR_NOT_CREATED, null);
								
							} catch (SaveSearchException e1) {
								logger.error("NU TREBUIE SA SE ARUNCE EXCEPTIA AICI");
								e1.printStackTrace();
							}
						}
					}
					if (logger.isDebugEnabled())
						logger.debug("getInMemoryDocsCount() = " + search.getInMemoryDocsCount());

					if( search.maxDocLimitReached() ){
						break;
					}
					
					
					
					
				} while (intrfServer.anotherSearchForThisServer(recursiveAnalyzeResponseResult==null?result:recursiveAnalyzeResponseResult)                        
						&& ( !ssi.mustStopToShowPartialResults(searchId) || searchType == Search.AUTOMATIC_SEARCH||searchType==Search.GO_BACK_ONE_LEVEL_SEARCH )
					    && ssi.hasNext(searchId) 
					    && this.isStarted()
					    && ((TSServer)intrfServer).continueSeach()
                        && ((TSServer)intrfServer).continueSeachOnThisServer()
                        && !getSkipCurrentSite() && !getStopAutomaticSearch());

			} catch (ServerResponseException e) {
				result = e.getServerResponse();
				if( result.getErrorCode() != ServerResponse.NOT_PERFECT_MATCH_WARNING ) {
					ssi.setMaxErrorCount();	//set max errors so we know that the status should be "not responding"
				}
			}
			
			if(loggedStartMessage) {
				intrfServer.logFinishServer("<hr>");
			}
			if (!hasSearchedOnThisServer && !search.maxDocLimitReached()) {
				search.updateSearchStatus((TSServer)intrfServer, Search.SKIPPED + " - insufficient parameters");
			}
			
			//use multiple pin strategy on each server
			search.setAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN, Boolean.FALSE);

			if( search.maxDocLimitReached() ){
				search.updateSearchStatus((TSServer)intrfServer, Search.SKIPPED + " - more than " + Search.MAX_TSR_INDEX_DOCUMENT_COUNT+ " documents");
				continueSearchFlag = intrfServer.goToNextServer(search);
				if (((TSServer)intrfServer).isRepeatDataSource()) {
					continueSearchFlag = false;
				}
				
				synchronized( statusNotifier )
				{
                    statusNotifier.setServerChanged();
				    statusNotifier.notifyAll();
				}
			}
			else if (getSkipCurrentSite() ||
					getStopAutomaticSearch() ||
					(sa.isDataSource() && !intrfServer.mustGoBackToUser(result)) ||
					(result.getParsedResponse().isMultiple() && !interactive && backgroundSearch && intrfServer.isAoLike())){
				
				if(sa.isDataSource()) {
					((TSServer)intrfServer).setStopTime(System.currentTimeMillis());
					continueSearchFlag = false;
					if (!((TSServer)intrfServer).continueSeachOnThisServer())
                    {
                        search.updateSearchStatus((TSServer)intrfServer, Search.TIME_OUT);
                    }
                    else {
                    	if (hasSearchedOnThisServer) {
                    		search.updateSearchStatus((TSServer)intrfServer,ssi);
                    	}
                    }
				} else if(getSkipCurrentSite()){
					setSkipCurrentSite(false);
					search.updateSearchStatus((TSServer)intrfServer, Search.SKIPPED);
					continueSearchFlag = intrfServer.goToNextServer(search);
					
					if(logger.isDebugEnabled()) {
						StringBuilder sbLog = new StringBuilder(100);
						sbLog.append("ASThread: SearchId = [").append(searchId)
							.append("], siteCycleIndex = [").append(siteCycleIndex)
							.append("], innerCycleIndex = [").append(innerCycleIndex)
							.append("], intrfServer = [").append(intrfServer)
							.append("], getSkipCurrentSite = [").append(getSkipCurrentSite())
							.append("]");
						logger.debug(sbLog.toString());
					}
					
				} else if(getStopAutomaticSearch()){ 
					SearchLogger.info("<br/><span class='error'><b>Automatic stopped by user!</b> </span>"+SearchLogger.getTimeStamp(searchId)+"<br/>", searchId);
					continueSearchFlag = false;
				} else {
					search.updateSearchStatus((TSServer)intrfServer, "multiple results - " + Search.SKIPPED);
					SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
					SearchLogPage page = sharedInstance.getSearchLogPage(searchId);
					SearchLogger.info("<span class='error'>Multiple results - skipped!</span><br/>", searchId);
					page.addErrorMessage("results.skipped");
					continueSearchFlag = intrfServer.goToNextServer(search);
				}
				
				synchronized( statusNotifier )
				{
                    statusNotifier.setServerChanged();
				    statusNotifier.notifyAll();
				}

			} 
			else if (intrfServer.mustGoBackToUser(result) && ((TSServer)intrfServer).continueSeachOnThisServer()) 
			{

				if (intrfServer.getClass().toString().endsWith("DN"))
			    {
						        continueSearchFlag = (!result.isError()
                            && !result.getParsedResponse().isError()
                            && intrfServer.anotherSearchForThisServer(recursiveAnalyzeResponseResult==null?result:recursiveAnalyzeResponseResult) && ssi
                            .hasNext(searchId));
                    if (!continueSearchFlag) {
                        if (searchType == Search.AUTOMATIC_SEARCH
                                || searchType == Search.INTERACTIVE_SEARCH||searchType==Search.GO_BACK_ONE_LEVEL_SEARCH) {
                            continueSearchFlag = intrfServer
                                    .goToNextServer(search);
                        }
                    }

                    ((TSServer) intrfServer).setStopTime(System.currentTimeMillis());
                    if (!((TSServer)intrfServer).continueSeachOnThisServer())
                    {
                        search.updateSearchStatus((TSServer)intrfServer, Search.TIME_OUT);
                    }
                    else {
                    	if (hasSearchedOnThisServer) {
                    		search.updateSearchStatus((TSServer)intrfServer,ssi);
                    	}
                    }
                }
			    else
			    {
					//afisare rezultat la user pt interventia acestuia in caz de eroare, de multiple, sau de none
				    try{
					sHTML = ServletServerComm.getHtml(intrfServer,search, TSServer.REQUEST_SEARCH_BY, result);
				    }
				    catch (Exception e)
				    {
				    //pt cazul in care a cazut la validare
				    }
					search.setSearchType(oldSearchType);
					((TSServer)intrfServer).setStopTime(System.currentTimeMillis());
                     if (!((TSServer)intrfServer).continueSeachOnThisServer())
                    {
                        search.updateSearchStatus((TSServer)intrfServer, Search.TIME_OUT);
                    }
                    else {
                    	if (hasSearchedOnThisServer) {
                    		search.updateSearchStatus((TSServer)intrfServer,ssi);
                    	}
                    }
                    
                    if (!sa.isDataSource()) {
                    	search.setSearchState(Search.STATE_FROM_PARENT_SITE);
                    } else {
                    	search.setSearchState(Search.STATE_FROM_DATASOURCE_SEARCH);
                    }
										
					if (backgroundSearch) {
						DBManager.setSearchOwner(search.getID(), 0);
			        } else if (getSearch().isUpdate()) {
						DBManager.setSearchOwner(search.getID(), InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue());
			        }

					// pentru resetare iterator
					search.setSearchIterator(null);
					search.getSa().setAtribute(SearchAttributes.SEARCHFINISH, "true");
					ssi = getSearchIterator(continueSearch, goBackOneLevel, search, intrfServer);
					
					return URLMaping.PARENT_SITE_RESPONSE;
			    }
			    
			} else { 
				continueSearchFlag = (!result.isError() 
									&& !result.getParsedResponse().isError() 
									&& intrfServer.anotherSearchForThisServer(recursiveAnalyzeResponseResult==null?result:recursiveAnalyzeResponseResult)
									&& ssi.hasNext(searchId)
                                    &&  ((TSServer)intrfServer).continueSeachOnThisServer());
				if (!continueSearchFlag && !repeatSomeSearchesFlag) {
					if (searchType == Search.AUTOMATIC_SEARCH || searchType == Search.INTERACTIVE_SEARCH||searchType==Search.GO_BACK_ONE_LEVEL_SEARCH) {
						continueSearchFlag = intrfServer.goToNextServer(search);
					} 
				}
				
				((TSServer)intrfServer).setStopTime(System.currentTimeMillis());
                if (!((TSServer)intrfServer).continueSeachOnThisServer())
                {
                    search.updateSearchStatus((TSServer)intrfServer, Search.TIME_OUT);
                }
                else {
                	if (hasSearchedOnThisServer) {
                		search.updateSearchStatus((TSServer)intrfServer,ssi);
                	}
                }
            }
			
						
			synchronized( statusNotifier )
			{
                statusNotifier.setServerChanged();
			    statusNotifier.notifyAll();
			}
			
			if(logger.isDebugEnabled()) {
				StringBuilder sbLog = new StringBuilder(100);
				sbLog.append("ASThread: SearchId = [").append(searchId)
					.append("], siteCycleIndex = [").append(siteCycleIndex)
					.append("], innerCycleIndex = [").append(innerCycleIndex)
					.append("], intrfServer = [").append(intrfServer)
					.append("], savingSearch = [").append(true)
					.append("]");
				logger.debug(sbLog.toString());
			}
			
			//save search context to database
			//if( searchSaved ){
				Search.saveSearch(search);
				AsynchSearchSaverThread.getInstance().saveSearchContext( search );
			//}
			
			intrfServer.performAdditionalProcessingAfterRunningAutomatic();
			
			if (repeatSomeSearchesFlag && repeatDataSource != null && repeatDataSource.size() > 0){
				for (Iterator<String> iterator = repeatDataSource.iterator(); iterator.hasNext();) {
					String p2 = iterator.next();
					if (p2.equals(dat.getP2())){
						repeatDataSource.remove(p2);
						break;
					}
				}
			}
			if (!search.getSa().isDataSource() && !continueSearchFlag && !repeatSomeSearchesFlag){
				if (repeatDataSource != null && repeatDataSource.size() > 0){
					repeatSomeSearchesFlag = true;
				}
			}
			if (repeatDataSource != null && repeatDataSource.size() == 0){
				repeatSomeSearchesFlag = false;
			}
						
		} while ((searchType == Search.AUTOMATIC_SEARCH||searchType==Search.GO_BACK_ONE_LEVEL_SEARCH) && 
				(continueSearchFlag || repeatSomeSearchesFlag) && this.isStarted());
		
		sa.setAtribute( SearchAttributes.SEARCHFINISH ,(!continueSearchFlag) +"");
		if (logger.isDebugEnabled())
			logger.debug (" ma intorc la user cu partial results- continueSearchFlag = " + continueSearchFlag);
		
		
		if (!continueSearchFlag){
			search.removeAllInMemoryDocs();
			
			Object savedObject = search.getAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS);
			Object savedATSDocs = search.getAdditionalInfo(AdditionalInfoKeys.PRESENT_ATS_DOCS_AT_AUTOMATIC_START);
			search.removeAllAdditionaInfo();
			search.setAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS, savedObject);
			search.setAdditionalInfo(AdditionalInfoKeys.PRESENT_ATS_DOCS_AT_AUTOMATIC_START, savedATSDocs);
			
			search.removeAllVisitedDocs();
			search.removeAllRemovedInstruments();
		}
		
		if (logger.isDebugEnabled())
			logger.debug("getInMemoryDocsCount() = " + search.getInMemoryDocsCount());

		search.setSearchType(oldSearchType);
		search.getSa().setAtribute(SearchAttributes.SEARCHFINISH, "true");
		HashCountyToIndex.setSearchServer(getSearch(), defaultSelectedRadioButton);
		search.searchCycle ++;
		ASMaster.removeSearch(search);
		return URLMaping.TSD;
    }
    
    private DataSite[] getRoLikeSortedByAutomaticPosition(Vector<DataSite> v, int productId){
    	Set<DataSite> set = new TreeSet<DataSite> ();
    	Collections.sort(v);
    	if(v!=null){
    		for(int i=0;i<v.size();i++){
    			DataSite curent = v.get(i);
    			if(TSServer.isRoLike(curent.getCityCheckedInt())&&
    					Util.isSiteEnabledAutomaticForProduct(productId, curent.getEnabled(search.getCommId()))){
    				set.add(curent);
    			}
    		}
    	}
    	return set.toArray(new DataSite[set.size()]);
    }
    
    private DataSite[] getGBSortedByAutomaticPosition(Vector<DataSite> v, int productId){
    	Set<DataSite> set = new TreeSet<DataSite> ();
    	if(v!=null){
    		for(int i=0;i<v.size();i++){
    			DataSite curent = v.get(i);
    			if(TSServer.isGBLike(curent.getCityCheckedInt())&&
    					Util.isSiteEnabledAutomaticForProduct(productId, curent.getEnabled(search.getCommId()))){
    				set.add(curent);
    			}
    		}
    	}
    	return set.toArray(new DataSite[set.size()]);
    }
    private boolean started = true;
    public boolean isStarted() {
        return started;
    }
    public void setStarted(boolean started) {
        this.started = started;
    }
    
    private boolean onGoBackOneLevelSearch(String goBackOneLevel){
		return ((goBackOneLevel != null) && (goBackOneLevel.equals(RequestParamsValues.TRUE)))?
			true:
			false;		
	}
	
	private ServerSearchesIterator getSearchIterator(String continueSearch, String goBackOneLevel,
													Search search,
													TSInterface intrfServer) throws NotImplFeatureException {

		if ((continueSearch == null) || (!continueSearch.equals(RequestParamsValues.TRUE))){
			search.setSearchIterator(null);
		}

		int moduleTypes = ServerSearchesIterator.AUTO_MODULES;
		if (onGoBackOneLevelSearch(goBackOneLevel)){
			search.setSearchIterator(null);
			moduleTypes = ServerSearchesIterator.GO_BACK_ONE_LEVEL_MODULES;
		}

		if (search.getSearchIterator() == null) {
			search.setSearchIterator(ServerSearchesIterator.getInstance(intrfServer, moduleTypes,searchId));				
		}
		
		return search.getSearchIterator();
	}


	private TSInterface getServer(Search search, String realPath) throws NumberFormatException, BaseException
	{
		TSInterface intrfServer= TSServersFactory.GetServerInstance(
				search.getP1(),
				search.getP2(),
				search.getID());
		intrfServer.setServerForTsd(search, realPath);
		return intrfServer;
	}
	
	public void setTestCaseSearch()
	{
	    testCaseSearch = true;
	}
	
	public boolean isTestCaseThread()
	{
	    return testCaseSearch;
	}
	
	public User getUser()
	{
	    return user;
	}

    /**
     * @return Returns the finishTimeServer.
     */
    public long getFinishTimeServer() {
        return finishTimeServer;
    }

    /**
     * @param finishTimeServer The finishTimeServer to set.
     */
    public void setFinishTimeServer(long finishTimeServer) {
        this.finishTimeServer = finishTimeServer;
    }

    /**
     * @return Returns the startTimeServer.
     */
    public long getStartTimeServer() {
        return startTimeServer;
    }

    public long getCurrentServerStartTime()
    {
        return currentServerStartTime;
    }
    
    /**
     * @param startTimeServer The startTimeServer to set.
     */
    public void setStartTimeServer(long startTimeServer) {
        this.startTimeServer = startTimeServer;
    }
    
    public boolean isBackgroundSearch(){
    	return backgroundSearch;
    }
    
    public void setBackgroundSearch( boolean backgroundSearch ){
    	this.backgroundSearch = backgroundSearch;
    }
    
    /**
     * For some reason the Thread instances are not garbage collected immediately
     * so we needed a function to remove all references
     */
    /*
	public void clearAllFields(){
	    search = null;       
	    user = null;
	    continueSearch = null;
		goBackOneLevel = null;
		interactive = true;
	    sHTML = null;
	    forwardTo  = null;
	    notifier = null;
	    statusNotifier = null;
	}
	*/
	
	public boolean isGoBack(){
		try{
			int searchType = search.getSearchType();
			return( searchType==Search.GO_BACK_ONE_LEVEL_SEARCH && "true".equals(goBackOneLevel) );
		}
		catch(Exception e){
			return false;
		}
	}
	
	public synchronized TSServerInfoModule getCrtModule() {
		return crtModule;
	}

	public Set<String> getTouchedServersInAutomatic() {
		return touchedServersInAutomatic;
	}
	
}
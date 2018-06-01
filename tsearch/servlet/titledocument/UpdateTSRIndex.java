package ro.cst.tsearch.servlet.titledocument;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import mailtemplate.MailTemplateUtils;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.transactions.SaveSearchTransaction;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.InvalidEmailOrderException;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.TSD;
import ro.cst.tsearch.servlet.ValidateInputs;
import ro.cst.tsearch.templates.CompileTemplateResult;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.templates.TemplateUtils;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.threads.AsynchSearchSaverThread;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.XMLMerger;
import ro.cst.tsearch.utils.XStreamManager;
import ro.cst.tsearch.utils.ZipUtils;
import ro.cst.tsearch.webservices.PlaceOrderService;

import com.stewart.ats.archive.OcrFileArchiveSaver;
import com.stewart.ats.base.document.BoilerPlateObject;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.reuse.ClipboardAtsI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.template.TemplateInfo;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;

public class UpdateTSRIndex extends BaseServlet {
	
	private static final long serialVersionUID = 1L;

	protected static final Category logger = Logger.getLogger(UpdateTSRIndex.class);

	public void doRequest(HttpServletRequest request, HttpServletResponse response) 
    	throws IOException, BaseException {

        HttpSession session = request.getSession(true);
        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
        int opCode = -1;
        try {
            opCode = Integer.parseInt(request.getParameter(TSOpCode.OPCODE));
        } catch (Exception ignored){}
        
		boolean deliveryNotification = true;
        deliveryNotification = ((currentUser != null) && (currentUser.getUserAttributes().getMyAtsAttributes().getReceive_notification()==1));
        
        Search search = (Search) currentUser.getSearch(request);
        long my_searchId = search.getID();
        CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(my_searchId).getCurrentCommunity();
        
       if (opCode == TSOpCode.SKIP_SITE) {
      		
      		ASThread searchThread = ASMaster.getSearch(search);
      		if (searchThread != null && searchThread.isStarted()) {
      			System.err.println("Skip site");
      			searchThread.setSkipCurrentSite(true);
      		}
      		
      		String site = "site";
      		try {
      			if (search != null)
      				site = Search.getServerTypeFromCityChecked(search.getCitychecked());
      		} catch (Exception e) {
      			e.printStackTrace();
      		}
      		
            try
            {
                response.getOutputStream().write(("Skipping " + site).getBytes());
            }
            catch( Exception e ) 
            {
                e.printStackTrace();
            }
            
      		
      		return;
      
        } else if (opCode == TSOpCode.OPEN_SEARCH || opCode == TSOpCode.REUSE_SEARCH_CODE || opCode == TSOpCode.DATEDOWN_SEARCH_CODE || opCode == TSOpCode.CLONE_SEARCH_CODE
        			|| opCode == TSOpCode.FVS_UPDATE) {
        	
        	try {
                long searchId = Long.parseLong(request.getParameter("newSearchId"));
                long oldSearchId = -1;
                try {
                	oldSearchId = Long.parseLong(request.getParameter("searchId"));
                } catch (Exception e) {
					oldSearchId = -1;
				}
                String urlString=null;
                
                ASThread thread = ASMaster.getSearch(searchId);
                Search openedSearch = SearchManager.getSearch( searchId, false );
                if (thread != null && thread.isAlive()  && openedSearch!=null) {
                    // search in progress
            		synchronized( openedSearch ){
	                	//test if this is a background search
	                	if( thread.isBackgroundSearch() ){
	                		//if this is a background search, we must clear the background flag.
                			openedSearch.setBackgroundSearch( false );
	                		
	                		thread.setBackgroundSearch( false );
	                		
	                		//remove the search from the background searches list and add it to current user search list
	                		
	                		SearchManager.moveSearch( searchId , currentUser );
	                		
	                		InstanceManager.getManager().getCurrentInstance( searchId ).setCrtSearchContext( openedSearch );
	                		InstanceManager.getManager().getCurrentInstance( searchId ).setCurrentUser( currentUser.getUserAttributes() );
	                		InstanceManager.getManager().getCurrentInstance( searchId ).setCurrentCommunity( ca );
	                	}
            		}
                	openedSearch.setRequestClean(false);
                	if(openedSearch.getStartIntervalWorkDate() == null) {
                		openedSearch.setStartIntervalWorkDate(new Date());
                	}
                    urlString = URLMaping.path + URLMaping.TSD 
					    			+ "?" + "searchId" + "=" + searchId
					    			+ "&" + RequestParams.SEARCH_TYPE + "=" + Search.AUTOMATIC_SEARCH
					    			+ "&" + RequestParams.SEARCH_STARTED + "=" + "true"
						        	+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.OPEN_SEARCH;
                } else {
                	if (opCode != TSOpCode.FVS_UPDATE){
                    // start new thread
	                String contextPath = DBManager.getSearchTSRFolder(searchId);
	                if(contextPath==null) {
	                	contextPath = DBManager.getSearchTSRFolder(oldSearchId);
	                	if(contextPath!=null)
	                		contextPath = contextPath.replaceAll(
	                				String.valueOf(oldSearchId), 
	                				String.valueOf(searchId));
	                }
	                 search = null;
	                
	                XStreamManager xstream = XStreamManager.getInstance();
	            	
	                try {
	                    
	                	//REPLICATION
	                	//check if context exists on disk, if not try to take it from database
	                	File contextPathFile = new File( contextPath);
	                	if( !contextPathFile.exists() ){
	                		//search not on disk, search it into database and unzip it on disk
	                		byte[] databaseContext = null;
	                		if(opCode!=TSOpCode.CLONE_SEARCH_CODE)
	                			databaseContext = DBManager.loadSearchDataFromDB( searchId );
	                		else
	                			databaseContext = DBManager.loadSearchDataFromDB( oldSearchId );
	                		ZipUtils.unzipContext( databaseContext, contextPath, searchId );
	                	}
	                	else{
	                		//if context exists on disk, check to see if we have the latest version
	                		long searchDbVersion = -1;
	                		long searchFolderVersion = -1;
	                		
	                		try{searchFolderVersion = Search.getSavedVersionFromFile( contextPath );}catch(Exception e){}
	                		
	                		if(opCode!=TSOpCode.CLONE_SEARCH_CODE){
	                			searchDbVersion = DBManager.getSearchDBVersion( searchId );
	                		} else {
	                			searchDbVersion = DBManager.getSearchDBVersion( oldSearchId );
	                		}
	                		
	                		if( searchDbVersion > searchFolderVersion ){
	                			//database version is newer, get search from DB
	                			System.err.println("------------- Inainte sa ia din baza");
	                			//erase old context
	                			FileUtils.deleteDir( contextPathFile );

	                			//get newer context from database
	                			byte[] databaseContext = null;
		                		if(opCode!=TSOpCode.CLONE_SEARCH_CODE)
		                			databaseContext = DBManager.loadSearchDataFromDB( searchId );
		                		else
		                			databaseContext = DBManager.loadSearchDataFromDB( oldSearchId );
		                		ZipUtils.unzipContext( databaseContext, contextPath, searchId );
		                		System.err.println("------------- Luat cu success din baza");
	                		}
	                	}
	                	
	                	File file =new File(contextPath + "__search.xml");
	                	
	                	if( file.length()< 40 * 1024 * 1024){
//		        			StringBuffer strBuf = new StringBuffer (org.apache.commons.io.FileUtils.readFileToString(file)
//		        					.replaceAll("/opt/resin/webapps/title-search/TSD", "/opt/TSD"));
//		        			
//		        			logger.debug("#####################################################");
//		        			logger.debug("SIZE = " +strBuf.length()+" compare to " + 40 * 1024 * 1024+ 
//		        					" FILE_NAME = " + contextPath + "__search.xml");
//		        			logger.debug("#####################################################");
		        				
		        			search = (Search)xstream.fromXML(org.apache.commons.io.FileUtils.readFileToString(file)
		        					.replaceAll("/opt/resin/webapps/title-search/TSD", "/opt/TSD"));

			            	
	                	}
	        			else{
	        				
	        				logger.error("#####################################################");
	        				logger.error("SIZE = bigger then " + 40 * 1024 * 1024+ " FILE_NAME = " + contextPath + "__search.xml");
	        				logger.error("##########------- BLANK PAGE  -------########");
	        				logger.error("#####################################################");
	        				
	        				String errorBody = "This search has exceeded the limit of 40MB and cannot be opened in ATS. We are sorry for the inconvenience.";
		    	    		request.setAttribute("title", "File exceeding size limit!");
	        				request.setAttribute("msg", errorBody);
							forward(request, response, "/jsp/simpleErrorPage.jsp");
	        				return;
	        			}
	        			
	                	if(search==null || opCode == TSOpCode.CLONE_SEARCH_CODE)
	                		search = SearchManager.getSearch( searchId );
	                	
	                	if(search.getID() == Search.SEARCH_NONE) {
	                		request.setAttribute(RequestParams.ERROR_BODY, "This search cannot be opened. Please contact ATS support.");
	                		forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
	                		return;
	                	}
	                    search.disposeTime = 0;
	                    // pentru searchurile deserealizate nu mai 
	                    // are voie sa faca update de date pe agent
	                    search.setAllowGetAgentInfoFromDB(false);
	                    search.makeServerCompatible(contextPath);
	                    
	                } catch(Exception e) {
	                    e.printStackTrace();
	                }
	                
	                if(search==null)
                		search = SearchManager.getSearch( searchId );
	                if(search.getID() == Search.SEARCH_NONE) {
                		request.setAttribute(RequestParams.ERROR_BODY, "This search cannot be opened. Please contact ATS support.");
                		forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
                		return;
                	}
	                
	                search.setStartIntervalWorkDate(new Date());
	                
	                /**
	                 * Getting here means that we correctly opened the search.
	                 * 
	                 */
	                try {
	                	
	                	if(opCode == TSOpCode.OPEN_SEARCH && !search.isWasOpened()){
		                	//bug 6770
	                		String dbOpenDate = DBManager.getSearchOpenDate(search.getSearchID(), null);
	                    	
	                    	if(StringUtils.isEmpty(dbOpenDate)){
	                    		DBManager.updateSearchOpenDate(search.getSearchID(), new Date());
	                    	}
	                	}
	                	
	                	int dbCommForThisSearch = (int) DBManager.getCommunityForSearch(search.getID());
	                	if(dbCommForThisSearch > 0 ) {
	                		if(dbCommForThisSearch != search.getCommId()) {
	                			Log.sendEmail2("Overriding comm_id from database",
	                					"SearchID:\n " + searchId + "\n has comm_id loaded " + search.getCommId() +
	            		    			", but database comm_id is " + dbCommForThisSearch + ", overriding loaded comm_id with database value");
	                			search.getSa().setCommId(dbCommForThisSearch);
	                		}
	                	} else {
	                		//invalid - maybe not there
                			int sessionCommId = ((BigDecimal)session.getAttribute(CommunityAttributes.COMMUNITY_ID)).intValue();
                			if(sessionCommId != search.getCommId() && sessionCommId > 0) {
	                			Log.sendEmail2("Overriding comm_id from session",
	                					"SearchID:\n " + searchId + "\n has comm_id loaded " + search.getCommId() +
	            		    			", but session comm_id is " + sessionCommId + ", overriding loaded comm_id with session value");
	                			search.getSa().setCommId(sessionCommId);
	                		}
	                		
	                	}
	                	
	                	
	                } catch (Exception e) {
						logger.error("Error while tring to check community", e);
					}
	                
                	}
	                search.setRequestClean(false);
	                if(opCode != TSOpCode.CLONE_SEARCH_CODE)
	                	search.setUpdate(true);
	                
			        search.setWasOpened(true);
			        
			        if(search.getTS_SEARCH_STATUS()==Search.SEARCH_STATUS_N) {
			        	search.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
			        	DBManager.setSearchStatus(search.getID(), Search.SEARCH_STATUS_T);
			        }
				    
				    
				    long userId = currentUser.getUserAttributes().getID().longValue();
				    if (opCode == TSOpCode.REUSE_SEARCH_CODE || opCode == TSOpCode.CLONE_SEARCH_CODE || opCode == TSOpCode.DATEDOWN_SEARCH_CODE){
				    	search.setTSDFileName("");
				    	search.getSearchFlags().setClosed(false);
				    	if(opCode == TSOpCode.REUSE_SEARCH_CODE || opCode == TSOpCode.DATEDOWN_SEARCH_CODE) {
				    		search.getSearchFlags().setCreationSourceType(CREATION_SOURCE_TYPES.REOPENED);
				    	} else if (opCode == TSOpCode.CLONE_SEARCH_CODE){
				    		search.getSearchFlags().setCreationSourceType(CREATION_SOURCE_TYPES.CLONED);
						}
				    	
				    	
				    }
				    
				    if(opCode == TSOpCode.REUSE_SEARCH_CODE ) {
				    	//we must keep the old main abstractor
				    	//we must keep the new abstractor in a different field if it is different that current one
				    	
				    	if(search.getSecondaryAbstractorId() == null) {
				    		if (search.getSa().getAbstractorObject().getID().longValue() != 
					    			currentUser.getUserAttributes().getID().longValue()) {
					    		search.setSecondaryAbstractorId(search.getSa().getAbstractorObject().getID().longValue());
					    	}
				    	}
				    } else if (opCode == TSOpCode.CLONE_SEARCH_CODE) {
				    	//on clone keep the original abstractor in a different field
				    	
				    	if(search.getSecondaryAbstractorId() == null) {
				    		if (search.getSa().getAbstractorObject().getID().longValue() != 
					    			currentUser.getUserAttributes().getID().longValue()) {
					    		search.setSecondaryAbstractorId(search.getSa().getAbstractorObject().getID().longValue());	
					    	}	
				    	}
				    	
				    }
				    
				    search.getSa().setAbstractor(currentUser.getUserAttributes());	
				    			    	
			    	DBManager.SearchAvailabitily searchAvailable = 
			    		DBManager.checkAvailability(
			    				search.getID(),
			    				currentUser.getUserAttributes().getID().longValue(), 
			    				(opCode == TSOpCode.OPEN_SEARCH ? DBManager.CHECK_OWNER : DBManager.CHECK_REUSE), false);
			    	
			    	if(searchAvailable.status == DBManager.SEARCH_WAS_CLOSED) {
			    		if(opCode == TSOpCode.REUSE_SEARCH_CODE || opCode == TSOpCode.CLONE_SEARCH_CODE || opCode == TSOpCode.DATEDOWN_SEARCH_CODE
			    				|| opCode == TSOpCode.FVS_UPDATE) {
				    		searchAvailable =  
					    		DBManager.checkAvailability(
					    				search.getID(),
					    				currentUser.getUserAttributes().getID().longValue(), 
					    				(opCode == TSOpCode.OPEN_SEARCH ? DBManager.CHECK_OWNER : DBManager.CHECK_REUSE), true);
				    		if(searchAvailable.status == DBManager.SEARCH_AVAILABLE) {
				    			DBManager.updateSearchesInvoiceStatus(searchId + "", "CHECKED_BY", 0);
				    			Vector<Long> toUnclose = new Vector<Long>();
				    			toUnclose.add(searchId);
						    	DBManager.uncloseSearch(toUnclose);
				    		}
			    		
			    		}
			    	
			    	}
			    	
			    	
			    	
			    	boolean searchWasUnlocked = "true".equals(request.getParameter(RequestParams.SEARCH_WAS_UNLOCKED));
			    	
						if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {
							String errorBody = searchAvailable.getErrorMessage();
		    	    		
							request.setAttribute(RequestParams.ERROR_BODY, errorBody);
						
							forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
			
				    	} else {
				    		SearchManager.removeJustSearchFromLocalHash(search.getID());
				    		currentUser.setSearch(search);
						    request.setAttribute("searchId=", search.getID());
						    
						    InstanceManager.getManager().getCurrentInstance(search.getID()).setup(currentUser, request, response, session);
						    InstanceManager.getManager().getCurrentInstance(search.getID()).setCrtSearchContext(search);
						    
						    request.setAttribute("searchId=", search.getSearchID());
						    InstanceManager.getManager().getCurrentInstance(search.getSearchID()).setup(currentUser, request, response, session);
						    InstanceManager.getManager().getCurrentInstance(search.getSearchID()).setCrtSearchContext(search);
						    
						    if (opCode == TSOpCode.REUSE_SEARCH_CODE ){
			                	HashMap<String, String> generatedTemp = search.getGeneratedTemp();
		                		for (String generatedTemplate : generatedTemp.values()) {
			                		try{	
										TemplateBuilder.cleanJustLink(generatedTemplate, search);
			                		} catch (Exception e) {
										logger.error("Something happened while saving saved templates for searchId = " 
												+ searchId + " generatedTemplate = " + generatedTemplate, e);
									}
								}
		                		try {
									DBManager.resetFVSFlagWhenReopenOrDeleteSearch(search.getID(), null, true);
								} catch (Exception e) {
									e.printStackTrace();
								}
			                }
						    
						    
						    if(searchWasUnlocked) {
						    	try {
						    		search.getDocManager().setSortBy(currentUser.getUserAttributes().getMyAtsAttributes().getTSR_SORTBY_NEW());
						    	}catch(Exception e) {
						    		System.err.println("Cannot set sorting for unlocked search: " + search.getID());
						    		e.printStackTrace();
						    	}
						    }
						    
						    if("true".equalsIgnoreCase(request.getParameter("isDateDown"))) {
						    	opCode = TSOpCode.DATEDOWN_SEARCH_CODE;
			                }
					        urlString = URLMaping.path + URLMaping.StartTSPage 
							    			+ "?" + "searchId" + "=" + search.getSearchID()				        	
								        	+ "&" + TSOpCode.OPCODE + "=" + opCode;
					        
					        DBManager.setSearchOwner(search, userId);
					        if(opCode == TSOpCode.REUSE_SEARCH_CODE){
					        	SearchLogger.info("</div><div><BR><B>Reopen Search</B> " + SearchLogger.getTimeStamp(searchId) 
					        			+ (currentUser.getBrowserVersion()!=null?" using browser: <span class=\"timestamp\">" + currentUser.getBrowserVersion() + "</span>":"")
					        			+ ".<BR></div>", search.getID());
					        } else if(opCode == TSOpCode.CLONE_SEARCH_CODE){
					        	if (search.getSa() != null){
				        			if (search.getProductId() == Products.UPDATE_PRODUCT && search.getSa().isFVSUpdate()){
				        				search.getSa().setProductId(Products.FVS_PRODUCT);
				        			}
			        			}
					        	SearchLogger.info("</div><div><BR><B>Clone Search</B> initial SearchID: <b>" + oldSearchId + "</b> -> new SearchID: <b>" + search.getID() + "</b> " + SearchLogger.getTimeStamp(searchId) 
					        			+ (currentUser.getBrowserVersion()!=null?" using browser: <span class=\"timestamp\">" + currentUser.getBrowserVersion() + "</span>":"")
					        			+ ".<BR></div>", search.getID());
					        } else if (opCode == TSOpCode.FVS_UPDATE){
					        	SearchLogger.info("</div><div><BR><B>FVSUpdate Search</B> " + SearchLogger.getTimeStamp(searchId) 
					        			+ (currentUser.getBrowserVersion()!=null?" using browser: <span class=\"timestamp\">" + currentUser.getBrowserVersion() + "</span>":"")
					        			+ ".<BR></div>", search.getID());
					        }
					        else if(opCode == TSOpCode.OPEN_SEARCH){
					        	String userLogin = "";
					            UserAttributes uaLogin = currentUser.getUserAttributes();
					            if (uaLogin != null)
					            	userLogin = uaLogin.getNiceName();
						    	String msgStr = "\n</div><div><BR><B>Search Opened</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") 
						    						+ " using browser: <span class=\"timestamp\">" 
						    						+ StringUtils.getPrettyBrowserString(request.getHeader("User-Agent")) 
						    						+ "</span></BR></div>\n";
			        			SearchLogger.infoUpdateToDB(msgStr, searchId);
			        			
			        			// Task 7085 - when a search is opened and an Automatic or a DataSource is started,
								// the end date is set to the current date and the start date is set to 45 years before, if 
								// they weren't edited manually before starting the search
								updateSearchDateInterval(search);
								
								//when open an old FVS that was saved as Update, now become a FVS Search
								if (search.getSa() != null){
				        			if (search.getProductId() == Products.UPDATE_PRODUCT && search.getSa().isFVSUpdate()){
				        				search.getSa().setProductId(Products.FVS_PRODUCT);
				        			}
			        			}
					        }
					        Search.saveSearch(search);
				    	}
							
				    }
                
			    if(urlString!=null){
			    	response.sendRedirect(urlString);
			    }
			    
			}
        	catch( SaveSearchException sse ){
				session.setAttribute("SaveSearchException", sse.getMessage());
				response.sendRedirect( URLMaping.path + URLMaping.TSDIndexedPage + "?" + TSOpCode.OPCODE + "="
		                + TSOpCode.TSD_INDEX_PAGE_FRAMESET + "&" + "searchId" + "=" + search.getSearchID() );
        	}
        	catch (Exception e) {
			    e.printStackTrace();
			}
			
        } else if (opCode == TSOpCode.SAVE_SEARCH || opCode == TSOpCode.SAVE_SEARCH_STARTER) {
        	
        	 	
            try {
				Search global= (Search) currentUser.getSearch(request);
				if(global.getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT)==null || 
						global.getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT).length()==0)
					global.getSa().setAtribute(SearchAttributes.SEARCH_PRODUCT, (String)request.getParameter(SearchAttributes.SEARCH_PRODUCT));
				
				DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(global.getID(),currentUser.getUserAttributes().getID().longValue(), DBManager.CHECK_OWNER, false);
		    	
				//if there is TSR in progress
				if (searchAvailable.tsrInProgress == true){
					String errorBody = searchAvailable.getErrorMessage();
							  errorBody += "Please try again after the tsr file is created.";
					forward(request, response, URLMaping.StartTSPage + 
						    "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS +
						    "&" + RequestParams.ERROR_TYPE + "=" + TSOpCode.TSR_IN_PROGRESS_ERROR +
						    "&" + RequestParams.ERROR_BODY + "=" +errorBody );
				}
				
				//else other states != available
				if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {
					String errorBody = searchAvailable.getErrorMessage();
    	    		
					request.setAttribute(RequestParams.ERROR_BODY, errorBody);
					
					forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
				} else {
			    	String userLogin = "";
			        UserAttributes uaLogin = currentUser.getUserAttributes();
			        if (uaLogin != null)
			        	userLogin = uaLogin.getNiceName();
					
					global.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
					ValidateInputs.saveSearchFile(global.getSa(), global, currentUser);
					boolean undoStarter = false;
					if (opCode == TSOpCode.SAVE_SEARCH_STARTER) {
						//only if the search wasn't previously set as base file we might undo the setting if the the save is not successful
						if(global.getSearchFlags().isBase())
							undoStarter = true;	
						global.getSearchFlags().setBase(true);
					}
					int searchSaved = DBManager.saveCurrentSearch(currentUser, global, Search.SEARCH_TSR_NOT_CREATED, request);
					if (searchSaved == SaveSearchTransaction.STATUS_SUCCES){
						String msgStr = "\n<BR><B>Search Saved" + (opCode == TSOpCode.SAVE_SEARCH ? " and Locked" : " as Base Search") + "</B> on: "
							+ SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") 
							+ " using browser: <span class=\"timestamp\">" 
    						+ StringUtils.getPrettyBrowserString(request.getHeader("User-Agent")) 
    						+ "</span></BR>\n";
						SearchLogger.info(msgStr, global.getID());
					} else {
						String msgStr = "\n<BR><B>Failed to save " + (opCode == TSOpCode.SAVE_SEARCH ? "locked " : "base ") + "search</B> on: "
							+ SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") 
							+ " using browser: <span class=\"timestamp\">" 
    						+ StringUtils.getPrettyBrowserString(request.getHeader("User-Agent")) 
    						+ "</span></BR>\n";
						SearchLogger.info(msgStr, global.getID());
					}
		    		if( searchSaved != SaveSearchTransaction.STATUS_SUCCES && undoStarter)
		    			global.getSearchFlags().setBase(false);
					DBManager.setSearchOwner(global.getID(), currentUser.getUserAttributes().getID().longValue());
                    global.setWasOpened( true );
                    
                    AsynchSearchSaverThread.getInstance().saveSearchContext( global );
                    if("newTS".equals(request.getParameter("fromPage")))
                    	forward(request, response, URLMaping.StartTSPage);
                    else
                    	forward(request, response, URLMaping.TSDIndexedPage);
		    	}
				
			} catch (SaveSearchException e) {
				session.setAttribute("SaveSearchException", e.getMessage());
				response.sendRedirect( URLMaping.path + URLMaping.TSDIndexedPage + "?" + TSOpCode.OPCODE + "="
		                + TSOpCode.TSD_INDEX_PAGE_FRAMESET + "&" + "searchId" + "=" + search.getSearchID() );
			} catch (ServletException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
        } else if (opCode == TSOpCode.SAVE_UNLOCKED_SEARCH || opCode == TSOpCode.SAVE_SEARCH_FOR_REVIEW) {
            try {
				Search global= (Search) currentUser.getSearch(request);
				
				
				DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(global.getID(),currentUser.getUserAttributes().getID().longValue(), DBManager.CHECK_OWNER, false);
		    	
				if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {
					String errorBody = searchAvailable.getErrorMessage();
    	    		
					request.setAttribute(RequestParams.ERROR_BODY, errorBody);
					
					forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
	
		    	} else {
			    	String userLogin = "";
			        UserAttributes uaLogin = currentUser.getUserAttributes();
			        if (uaLogin != null)
			        	userLogin = uaLogin.getNiceName();
		    		
			        boolean forReview = false;
			        if (opCode == TSOpCode.SAVE_SEARCH_FOR_REVIEW){
			        	global.getSearchFlags().setForReview(true);
			        	forReview = true;
			        }
		    		if (DBManager.saveCurrentSearchLockedUnlocked(currentUser, global, Search.SEARCH_TSR_NOT_CREATED, request, false)
		    				== SaveSearchTransaction.STATUS_SUCCES){
						String msgStr = "\n<BR><B>Search Saved " + (forReview ? "rEview " : "") + "and Unlocked" + "</B> on: "
    						+ SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") 
    						+ " using browser: <span class=\"timestamp\">" 
			    			+ StringUtils.getPrettyBrowserString(request.getHeader("User-Agent")) 
			    			+ "</span></BR>\n";

		    			SearchLogger.info(msgStr, global.getID());
		    		} else {
		    			String msgStr = "\n<BR><B>Failed to save search " + (forReview ? "rEview " :"")+ "unlocked" + "</B> on: " 
    						+ SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") 
    						+ " using browser: <span class=\"timestamp\">" 
			    			+ StringUtils.getPrettyBrowserString(request.getHeader("User-Agent")) 
			    			+ "</span></BR>\n";
		    			SearchLogger.info(msgStr, global.getID());
		    		}
					DBManager.setSearchOwnerLockedUnlocked(global.getID(), currentUser.getUserAttributes().getID().longValue(), false);
                    global.setWasOpened( true );
                    
                    AsynchSearchSaverThread.getInstance().saveSearchContext( global );
                    
                    if("newTS".equals(request.getParameter("fromPage")))
                    	forward(request, response, URLMaping.StartTSPage);
                    else
					forward(request, response, URLMaping.TSDIndexedPage);
		    	}
				
			} catch (SaveSearchException e) {
					//nu mai este valabila exceptia asta
			} catch (ServletException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
        } else if (opCode == TSOpCode.SUBMIT_ORDER) {
        	
        	
        	logger.debug( "SUBMIT_ORDER\n\n" );
        	
        	Enumeration parameters = request.getParameterNames();
        	StringBuffer sb = new StringBuffer();
        	while ( parameters.hasMoreElements() ) {
        		
        		String name = (String) parameters.nextElement();
        		String value = request.getParameter(name);
        		
        		sb.append( name + " = [" + value + "],   " );
        	}
        	logger.debug(sb.toString());
        	
           
            Search global = (Search) currentUser.getSearch(request);
            
            
            ValidateInputs.saveSearchAttributes(request, global);
           
            SearchAttributes initialSearchAttributes = (SearchAttributes) global.getSa().clone();


            //          set search (from and to) dates
			java.util.Calendar date = java.util.Calendar.getInstance();
			date.add(java.util.Calendar.YEAR,-SearchAttributes.YEARS_BACK); // go back N years 
			
			
			//B2034
			if(initialSearchAttributes.getAtribute(SearchAttributes.FROMDATE)!=null)
				initialSearchAttributes.setAtribute(SearchAttributes.FROMDATE, SearchAttributes.DEFAULT_DATE_PARSER.format(date.getTime()));
			
			if(initialSearchAttributes.getAtribute(SearchAttributes.TODATE)!=null)
				initialSearchAttributes.setAtribute(SearchAttributes.TODATE, 
				        CSTCalendar.getDateFromInt(CSTCalendar.getDefaultInitDate("MDY"), "MDY"));
			
			
			global.getSa().setAtribute(SearchAttributes.FROMDATE, SearchAttributes.DEFAULT_DATE_PARSER.format(date.getTime()));
			global.getSa().setAtribute(SearchAttributes.TODATE, 
			        CSTCalendar.getDateFromInt(CSTCalendar.getDefaultInitDate("MDY"), "MDY"));
            
            
            SearchAttributes sa = global.getSa();
            sa.setAbstractorFileName(global);
			global.setAgent(sa.getAbstractorObject());

			String commProductName = "";
	        try {
				commProductName = (CommunityProducts.getProduct(ca.getID().longValue())).getProductName(sa.getProductId());
			} catch (Exception e) {
			}
	        String prodStr = "";
	        if (org.apache.commons.lang.StringUtils.isNotEmpty(commProductName)){
	        	prodStr += "(" + commProductName + ") ";
			}
			// check if update - it may change the fromdate and todate
            try{
            	try {
        	    	String userLogin = "";
        	        UserAttributes uaLogin = currentUser.getUserAttributes();
        	        if (uaLogin != null)
        	        	userLogin = uaLogin.getNiceName();
        			SearchLogger.infoUpdateToDB("\n<div><BR><B>Search " + prodStr + "sent in Dashboard by Submit Order</B> on: " + 
        					SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "<BR></div>\n", global.getID());

            		ValidateInputs.checkIfUpdate(global, opCode, request, SearchAttributes.ABSTRACTOR_FILENO);
            	}catch(InvalidEmailOrderException ieoe){
            		if("010".equals(request.getParameter("aid"))){
            	    	String userLoginAIM = "";
            	        UserAttributes uaLoginAIM = currentUser.getUserAttributes();
            	        if (uaLoginAIM != null)
            	        	userLoginAIM = uaLoginAIM.getNiceName();
            	        
            			String msgStr = "\n<BR><B>Search " + prodStr + "sent in Dashboard from %s</B> on: "
								+ SearchLogger.getTimeStampFormat1(userLoginAIM, "&#44; ") + "<BR>\n";
            			
            			if (PlaceOrderService.isTitleDesk(search.getSa())) {
            				msgStr = String.format(msgStr, "Title Desk");
            			} else {
            				msgStr = String.format(msgStr, "AIM");
            			}
            			
						SearchLogger.info(msgStr, global.getID());
            			ValidateInputs.checkIfUpdate(global, opCode, request, SearchAttributes.ORDERBY_FILENO);
            		} else {
            			throw ieoe;
            		}
            	}
            }catch(InvalidEmailOrderException e){
            	// we caught an exception that is thrown only if update and no fid or fid does not
            	// correspond to a previously finished search
            	try{
	            	String fileNo = global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
	            	if(fileNo != null){
	            		fileNo += " ";
	            	}else{
	            		fileNo = "";
	            	}
	            	String subject = "ERROR: " + fileNo + "update order was not placed!";
	            	String body = "Your update order was not placed due to following reason:\n";
	            	body += ("  - " + e.getMessage() + "\n");
	            	InternetAddress from = new InternetAddress(MailConfig.getMailFrom());
	            	InternetAddress to = new InternetAddress(global.getSa().getAbstractorObject().getEMAIL());
	            	InternetAddress cc = new InternetAddress(MailConfig.getSupportEmailAddress());
	            	if (deliveryNotification){
	            		sendMail(from, to, cc, subject, body);
	            	}
            	}catch(Exception e2){
            		e2.printStackTrace();
            	}
            	// re-throw the exception, to be treated the same as before
            	throw new BaseException(e.getMessage());
            }
            
            ValidateInputs.checkIfRefinance(request, global);
			
            if ("".equals(global.getSa().getAtribute(SearchAttributes.ORDERBY_FILENO))) {                
                
            	global.getSa().setAtribute(SearchAttributes.ORDERBY_FILENO,
                        global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO));
            	
            } else if ("".equals(global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO))) {
            	
            	global.getSa().setAtribute(SearchAttributes.ABSTRACTOR_FILENO,
                        global.getSa().getAtribute(SearchAttributes.ORDERBY_FILENO));
            }
            sa.setAbstractorFileName(global);
                      
            // fill searchOrder and save it into proper folder
            String htmlBody = MailTemplateUtils.fillAndSaveSearchOrder(global, initialSearchAttributes, currentUser);
		
			global.setAgent(currentUser.getUserAttributes());
			global.getSa().setOrderedBy(currentUser.getUserAttributes());
			
			int stateId = 0;
			int countyId = 0;
			
			try{
				stateId = Integer.parseInt(sa.getAtribute("P_STATE"));
			}
			catch( Exception e ){
				e.printStackTrace();
			}
			
			try{
				countyId = Integer.parseInt(sa.getAtribute("P_COUNTY"));
			}
			catch( Exception e ){
				e.printStackTrace();
			}
			
			
			try {
				HashCountyToIndex.setSearchServer(global, HashCountyToIndex.getFirstServer( 
						global.getProductId(), 
						global.getCommId(), 
						countyId ));
			}catch(NotImplFeatureException npe) {
				ValidateInputs.checkCountyImpl(global);
			}
            catch( NumberFormatException nfe )
            {}
			
            //if state and county could not be set, threre is no point in continuing
            //return to user and print message
            if( stateId == 0 || countyId == 0 ){
            	String fileno = "";
            	try {
            		fileno = URLEncoder.encode(global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO), "UTF-8");
            	}catch(Exception e) { 
            		e.printStackTrace();
            	}
    			String urlString = URLMaping.path + URLMaping.OrderTSPage + "?" 
	        			+ "searchId" + "=" + User.NEW_SEARCH
	        			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_SEARCH
	        			+ "&" + SearchAttributes.ABSTRACTOR_FILENO 
	        			+ "=" + fileno;
	
    			session.setAttribute("ShowError" + global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO), "State and/or county not selected!");
    			try{
	            	String fileNo = global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
	            	if(fileNo != null){
	            		fileNo += " ";
	            	}else{
	            		fileNo = "";
	            	}
	            	String subject = "ERROR: " + fileNo + "update order was not placed!";
	            	String body = "Your order was not placed due to following reason:\n";
	            	body += ("  - State or county not selected!\n");
	            	InternetAddress from = new InternetAddress(MailConfig.getMailFrom());
	            	InternetAddress to = new InternetAddress(global.getSa().getAbstractorObject().getEMAIL());
	            	InternetAddress cc = new InternetAddress(MailConfig.getSupportEmailAddress());
	            	if (deliveryNotification) {
	            		sendMail(from, to, cc, subject, body);
	            	}
    			}
    			catch( Exception e ){
    				e.printStackTrace();
    			}
    			
    			response.sendRedirect(urlString);
    			
    			return;
            }
            else {
            	if (deliveryNotification) {
            		sendMail(htmlBody, currentUser, sa.getAtribute("ABSTRACTOR_FILENO") + "/" + sa.getAtribute("ORDERBY_FILENO"),my_searchId);
            	}
            	//sendMail(htmlBody, currentUser, sa.getAtribute("ORDERBY_FILENO"),my_searchId);
            }
            

			if (global.isCountyImplemented()) {
				global.setSearchType(Search.AUTOMATIC_SEARCH);
			} else {
				global.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);
			}

			
			// create an empty TSDIndexPage
			String sPath = getServletConfig().getServletContext().getRealPath(request.getContextPath());
			TSD.initSearch(currentUser, global, sPath, request);
			
			
			
			
			try {
				// save empty search
				DBManager.saveCurrentSearch(currentUser, global, Search.SEARCH_TSR_NOT_CREATED, request);
				AsynchSearchSaverThread.getInstance().saveSearchContext( global );
			} catch (SaveSearchException e1) {
				logger.error(">>>>>>>>>>>>>> NU ARE CUM SA FIE ARUNCATA EXCEPTIA ASTA");
				e1.printStackTrace();
			}
			DBManager.setSearchOwner(global.getID(),(global.isCountyImplemented() ? -1 : 0));

			try{
				DBManager.zipAndSaveSearchToDB( search );
			}
			catch(Exception e2){
				e2.printStackTrace();
				logger.error("Error saving the search context received through web-services", e2);
				SearchManager.removeSearch(search.getID(), true);
				DBManager.deleteSearch(search.getID(),  Search.SEARCH_TSR_NOT_CREATED);
				return;
			}
			
			// start automatic search thread.
			if(global.isCountyImplemented()) {
				synchronized (global) {
					ASMaster.startBackgroundSearch(global, currentUser, ca);			
					SearchManager.removeSearch(global.getSearchID(), false);
					SearchManager.addBackgroundSearch(global);
				}
			}
            
        	String fileno = "";
        	try {
        		fileno = URLEncoder.encode(global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO), "UTF-8");
        	}catch(Exception e) { 
        		e.printStackTrace();
        	}
        	
			String urlString = URLMaping.path + URLMaping.OrderTSPage + "?" 
			        	+ "searchId" + "=" + User.NEW_SEARCH
			        	+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_SEARCH
			        	+ "&" + SearchAttributes.ABSTRACTOR_FILENO 
			        	+ "=" + fileno;
			
			session.setAttribute("ShowConfirmation" + global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO), "true");
			
            response.sendRedirect(urlString);
            
        } else if (opCode == TSOpCode.LOAD_STARTER) {
        	String starterSearchId = request.getParameter(RequestParams.STARTER_SEARCH_ID);
        	PrintWriter out = response.getWriter();
        	if(StringUtils.isEmpty(starterSearchId)){
        		out.print("message=\"No Base Search Id\"");
        	} else {
        		if (loadBaseSearch(session, search, Long.parseLong(starterSearchId)))
        			out.print("OK");
        		else 
        			out.print("message=\"Merge Failed\"");
        	}
        	out.flush();
        	return;
        	
        } else if (opCode == TSOpCode.COPY_SEARCH) {
        	ClipboardAtsI clipboardAts = null;
        	synchronized(session){
        		clipboardAts =(ClipboardAtsI) session.getAttribute(SessionParams.CLIPBOARD_ATS);
        	}
        	String starterSearchId = request.getParameter(RequestParams.STARTER_SEARCH_ID);
        	long lastSaveForMergeSearch = Search.SEARCH_NONE;
        	try {
        		lastSaveForMergeSearch = Long.parseLong(starterSearchId);
        	} catch (Exception e) {
				logger.error("Invalid searchId to copy to clipboard:" + starterSearchId , e);
			}
        	clipboardAts.setLastSaveForMergeSearch(lastSaveForMergeSearch);
        	
         	response.getWriter().print("OK");
        	
        } else if(opCode == TSOpCode.STOP_AUTOMATIC_SEARCH) {
        	ASThread searchThread = ASMaster.getSearch(search);
      		if (searchThread != null && searchThread.isStarted()) {
      			System.err.println("Stopping Automatic Search! by user request "+SearchLogger.getCurDateTimeCST());
      			searchThread.setStopAutomaticSearch(true);
      		}
      		
            try
            {
            	ASMaster.waitForSearch(search);
            	response.sendRedirect("/title-search/jsp/newtsdi/tsdindexpage.jsp?searchId="+search.getID()+"&userId="+currentUser.getUserAttributes().getID());
            }
            catch( Exception e ) 
            {
                e.printStackTrace();
            }
            
      		
      		return;
        	
        } else {
        	Search global = (Search) currentUser.getSearch(request);
	        synchronized (global) {
	
	        	DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(global.getID(),currentUser.getUserAttributes().getID().longValue() ,DBManager.CHECK_OWNER, false);
		    	
				if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {
					String errorBody = searchAvailable.getErrorMessage();
    	    		
					request.setAttribute(RequestParams.ERROR_BODY, errorBody);
					
					try {
						forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
					} catch (ServletException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;
				}
	           
	        }
	
	        // save search after automatic search
            if (global.isUpdate()) {
            	/*
            	 * 		Aici searchul nu poate fi checkat de altcineva pentru ca aici se termina automaticul,
            	 * 		pe parcursul atutomaticului, searchul nefiind disponibil. So, nu trebuie sa arunce exceptie la salvare.
            	 */
                try {
					Search.saveSearch(global);
				} catch (SaveSearchException e) {
					session.setAttribute("SaveSearchException", e.getMessage());
					e.printStackTrace();
				}
            }
            
	        String url = URLMaping.path + URLMaping.TSDIndexedPage + "?" + TSOpCode.OPCODE + "="+ TSOpCode.TSD_INDEX_PAGE_FRAMESET + "&" + "searchId" + "=" + global.getSearchID();
	
	        response.sendRedirect(url);
        }
    }
	
	private void updateSearchDateInterval(Search global) {
		SearchAttributes sa = global.getSa();
		Calendar cal = Calendar.getInstance();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy");
		
		String oldDate = sa.getAtribute(SearchAttributes.TODATE);
		String newDate = sdf.format(cal.getTime());
		
		DocumentsManagerI manager = global.getDocManager();
		try {
			manager.getAccess();
		
			if(!oldDate.equals(newDate)) {
				SearchLogger.info("<div>In Search Page, 'Search to' <b>" + oldDate 
						+ "</b> was automatically changed to <b>" + newDate + "</b>", global.getID());
				sa.setAtribute(SearchAttributes.TODATE, newDate);
				manager.setEndViewDate(sdf.parse(newDate));
			}
			
			if(!(global.isProductType(SearchAttributes.SEARCH_PROD_UPDATE) || global.isProductType(Products.FVS_PRODUCT))) {
				cal.add(java.util.Calendar.YEAR, -SearchAttributes.YEARS_BACK); // go back N years
				oldDate = sa.getAtribute(SearchAttributes.FROMDATE);
				newDate = sdf.format(cal.getTime());
				
				if(!oldDate.equals(newDate)) {
					SearchLogger.info("<div>In Search Page, 'Search from' <b>" + oldDate 
							+ "</b> was automatically changed to <b>" + newDate + "</b>", global.getID());
					sa.setAtribute(SearchAttributes.FROMDATE, newDate);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			manager.releaseAccess();
		}
	}
    
    /**
     * Tries to load the base search and then copy chapters from the started to the current search
     * @param currentSearch
     * @param starterSearchId
     * @return
     */
	public static  boolean loadBaseSearch(HttpSession session, Search currentSearch, long starterSearchId) {
		
    	
    	Search openedSearch = SearchManager.getSearchFromDisk(starterSearchId);
        if(openedSearch == null)
        	return false;
        
        SearchLogger.info("Copied tsr index from search " + openedSearch.getAbstractorFileNo() + " to " + currentSearch.getAbstractorFileNo() + " <br>" , currentSearch.getID());
        
        DocumentsManagerI openedDocumentsManager = openedSearch.getDocManager();
        DocumentsManagerI currentDocumentsManager = currentSearch.getDocManager();
        
        try {
			openedDocumentsManager.getAccess();
			currentDocumentsManager.getAccess();
			
			boolean hasAlreadyAssessorDocuments = currentDocumentsManager.getDocumentsWithDocType("ASSESSOR").size() != 0; 
			
			Collection<DocumentI> allOpenedDocuments = openedDocumentsManager.getDocumentsList();
			
			for (DocumentI documentI : allOpenedDocuments) {
				if(!currentDocumentsManager.contains(documentI)) {
					DocumentI newDoc = documentI.clone();
					
					if(documentI.is(DType.ASSESOR)){
						if(!hasAlreadyAssessorDocuments) {
							currentDocumentsManager.add(newDoc);
						}
						
					} else {
						String oldSearchID = Long.toString(starterSearchId);
						String currentSearchID = Long.toString(currentSearch.getSearchID());
						String newDocId = DocumentsManager.generateDocumentUniqueId(currentSearch.getID(), documentI.getInstrument());
						newDoc.setId(newDocId);
						
						String contentDocIndex = DBManager.getDocumentIndex(newDoc.getIndexId());
						if (contentDocIndex != null) {
							if (contentDocIndex.indexOf("searchId=" + oldSearchID) != -1)
								contentDocIndex = contentDocIndex.replaceAll("(?is)(searchId=)" + oldSearchID, "$1" + currentSearchID);
							newDoc.setIndexId(DBManager.addDocumentIndex(contentDocIndex, currentSearch));
						}
						
						boolean addToDocManager = true;
						if(newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_EXC) 
								|| newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_REQ)
								|| newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_ESTATE)
								|| newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_COMMENT)) {
							ArrayList<RegisterDocumentI> registerDocuments = currentDocumentsManager.getRegisterDocuments(newDoc, true);
							if(!registerDocuments.isEmpty()) {
								addToDocManager = false;
								
								for (RegisterDocumentI currentDocument : registerDocuments) {
									Set<String> codeBookSourceSet = newDoc.getCodeBookCodes();
			        				for (String codeBookCode : codeBookSourceSet) {
			        					BoilerPlateObject codeBookCodeSource = newDoc.getCodeBookObject(codeBookCode);
			        					//skip deleted codes
			        					if(!codeBookCodeSource.isManuallyDeleted()) {
			        						BoilerPlateObject codeBookCurrentObject = currentDocument.getCodeBookObject(codeBookCode);
				        					
											if(codeBookCurrentObject != null) {
												if(!codeBookCurrentObject.hasModifiedStatement()) {
													if(codeBookCodeSource.hasModifiedStatement()) {
														//copy statement if the source is manually modified
														codeBookCurrentObject.setModifiedStatement(codeBookCodeSource.getModifiedStatement());
													}
												}
											} else {
												//I do not have this code but it might have been manually added so let's try to copy it
												if(codeBookCodeSource.isManuallyAdded()) {
													currentDocument.addCodeBookCode(codeBookCodeSource.clone());
												}
											}
			        					}
									}
								}
							}
						}
						
						if(addToDocManager) {
							currentDocumentsManager.add(newDoc);
						}
					}
					
					ImageI image = newDoc.getImage();
					
					if(image != null) {
						image.setViewCount(new AtomicInteger());	//this image was never viewed
						
//						String oldImagePath = image.getPath();
//	        			if(StringUtils.isNotEmpty(oldImagePath)) {
//		        			image.setPath(currentSearch.getImagePath() + image.getFileName());
//	        			}
	        			
	        			String oldImageFileName = image.getFileName();
	        			image.setFileName( oldImageFileName.replace( documentI.getId(), newDoc.getId()));
	        			image.setPath(currentSearch.getImagePath() + image.getFileName());
	        			
	        			
	        			OcrFileArchiveSaver ocrFileArchiveSaverSource = new OcrFileArchiveSaver(openedSearch);
		        		OcrFileArchiveSaver ocrFileArchiveSaverDestination = new OcrFileArchiveSaver(currentSearch);
		        		
		        		List<File> ocrSourceFiles = ocrFileArchiveSaverSource.getLocalFiles(true, documentI.getId());
		        		String ocrFileArchiveFolderPath = OcrFileArchiveSaver.getOcrFileLocalFolder(currentSearch);
		        		for (File file : ocrSourceFiles) {
		        			String fileName = file.getName();
		        			File copiedFile = new File(ocrFileArchiveFolderPath + File.separator + fileName.replace(documentI.getId(), newDoc.getId()));
		        			if(fileName.endsWith("." + OcrFileArchiveSaver.SMART_VIEWER_JS_EXT) ) {
		        				String fileContent = org.apache.commons.io.FileUtils.readFileToString(file);
								fileContent = fileContent.replaceAll(documentI.getId(), newDoc.getId());
//								fileContent = fileContent.replaceAll("searchId=" + openedSearch.getID(), "searchId=" + currentSearch.getID());
								org.apache.commons.io.FileUtils.writeStringToFile(copiedFile, fileContent);
		        			} else {
		        				org.apache.commons.io.FileUtils.copyFile(file, copiedFile);
		        			}
		        			ocrFileArchiveSaverDestination.addLocalFile(copiedFile);
		        			
						}
		        		
		        		new Thread(
		        				ocrFileArchiveSaverDestination, 
		        				"OcrFileArchiveSaver on clone " + openedSearch.getID() + " to " + currentSearch.getID() + " - document " + newDoc.getNiceFullFileName())
		        			.start();
	        			
//	        			List<OcrFileMapper> ocrFileMappers = OcrFileMapper.getOcrFileMappers(starterSearchId, newDoc.getId());
//						for (OcrFileMapper ocrFileMapper : ocrFileMappers) {
//							ocrFileMapper.setSearchId(currentSearch.getID());
//							ocrFileMapper.setSaveTime(new Date());
//							ocrFileMapper.insertInDatabase();
//						}
					}
				}
			}
			currentSearch.applyQARules();
			
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
			return false;
		} finally {
			openedDocumentsManager.releaseAccess();
			currentDocumentsManager.releaseAccess();
		}
        
        
		SearchLogger.info("Merged successfully with " + openedSearch.getAbstractorFileNo() + "<br>", currentSearch.getID());
		
	  	long userID = currentSearch.getSa().getAbstractorObject().getID().longValue();
	  	
		SearchLogger.info("Merging .ats templates... <br>" , currentSearch.getID());
        String tsrFolder = DBManager.getSearchTSRFolder(starterSearchId);
        List<TemplateInfo> templates = TemplateUtils.getTemplatesInfo( currentSearch.getSearchID(),  false );

        for(TemplateInfo ti : templates) {
    		String templateNameCurrentSearch = ti.getPath();
    		if(templateNameCurrentSearch == null) continue;
			try {		
				long templateId = ti.getId();
				
				String fileExtension = templateNameCurrentSearch.substring(templateNameCurrentSearch.lastIndexOf(".") + 1);
				if (templateNameCurrentSearch != null && "ats".equals(fileExtension.toLowerCase()) ){
					File fileTempl = new File(tsrFolder	+ Search.TEMP_DIR_FOR_TEMPLATES.replaceAll("//", "/") + File.separator + templateNameCurrentSearch);
					if (fileTempl.exists()) {
						String pathx = tsrFolder + Search.TEMP_DIR_FOR_TEMPLATES.replaceAll("//", "/") + File.separator + templateNameCurrentSearch;
						String starterTemplate = org.apache.commons.io.FileUtils.readFileToString(new File(pathx));
						String tsrFolderCurrentSearch = null;
						File templateFileCurrentSearch = null;
						try {
							tsrFolderCurrentSearch = DBManager.getSearchTSRFolder(currentSearch.getSearchID());
						} catch (Exception e) {
							e.printStackTrace();
						}
						templateFileCurrentSearch = new File(tsrFolderCurrentSearch + Search.TEMP_DIR_FOR_TEMPLATES.replaceAll("//", "/") + File.separator
								+ templateNameCurrentSearch);
						
						// download images before conversion process
						try {
						     openedSearch.downloadImages(false);
						    //just close the last opened <div> tag
						    SearchLogger.info("</div>\n", openedSearch.getID());
						} catch (Exception e) {
							logger.error("Error while downloading images in the orriginal search " +  openedSearch.getID(), e);
						}
						
						
						if( UtilForGwtServer.uploadImagesToSSF(starterSearchId,false,false)<0 ){
							SearchLogger.info("<b>Could NOT RESERVE SSF TRANSACTION ID</b> for uploading images", starterSearchId);
						}
						

						if (tsrFolderCurrentSearch == null || !templateFileCurrentSearch.exists()) {						
							CompileTemplateResult templStuff = TemplateUtils.compileTemplate( currentSearch.getSearchID(), userID, templateNameCurrentSearch, false, null, null, false, null);
							
							TemplateUtils.saveTemplate(session, null, currentSearch.getSearchID(), userID, templateNameCurrentSearch, templateId, templStuff.getTemplateContent(), "");
							tsrFolderCurrentSearch = DBManager.getSearchTSRFolder(currentSearch.getSearchID());
							templateFileCurrentSearch = new File(tsrFolderCurrentSearch + Search.TEMP_DIR_FOR_TEMPLATES.replaceAll("//", "/") + File.separator
									+ templateNameCurrentSearch);
						}
						
						
						
						if (templateFileCurrentSearch.exists()) {
							SearchLogger.info("Merging " + templateNameCurrentSearch + " template... <br>" , currentSearch.getID());
							String pathy = tsrFolderCurrentSearch + Search.TEMP_DIR_FOR_TEMPLATES.replaceAll("//", "/") + File.separator + templateNameCurrentSearch;
							
							String currentSearchTemplate = org.apache.commons.io.FileUtils.readFileToString(new File(pathy));
							
							starterTemplate = TemplateBuilder.replaceImageLinksInTemplate(starterTemplate, openedSearch, false, true);

							String result = XMLMerger.templateMerge(starterTemplate, currentSearchTemplate);
							TemplateUtils.saveTemplate(session, null, currentSearch.getSearchID(), userID, templateNameCurrentSearch, templateId, result, "");
							SearchLogger.info("The template " + templateNameCurrentSearch + " was successfully merged.<br>" , currentSearch.getID());
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				SearchLogger.info("Failed merging " + templateNameCurrentSearch + " template<br>", currentSearch.getID());
			}
		}
			
		return true;
	}

	private boolean sendMail(String body, User currentUser, String fileNo,long searchId) {
        
        try {
            
        	String userEmail=currentUser.getUserAttributes().getEMAIL();
            BigDecimal commAdminID = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getCOMM_ADMIN();
            UserAttributes commAdmin = UserManager.getUser(commAdminID);
            
			Properties props = System.getProperties();
			props.put("mail.smtp.host", MailConfig.getMailSmtpHost());
			Session session = Session.getDefaultInstance(props,null);
			MimeMessage msg = new MimeMessage(session);
			           
			InternetAddress fromAddress = null;
			try {
			    fromAddress = new InternetAddress(currentUser.getUserAttributes().getEMAIL());
			} catch (Exception ex) {
			    fromAddress = new InternetAddress(MailConfig.getMailFrom());
			}
			msg.setFrom(fromAddress);
			
			msg.setSubject(fileNo +" - A new order has been received from agent " + currentUser.getUserAttributes().getUserFullName() + ". Please schedule it for further processing.");
			msg.setRecipients(javax.mail.Message.RecipientType.TO, 
							InternetAddress.parse(commAdmin.getEMAIL()));
			msg.setRecipients(javax.mail.Message.RecipientType.BCC, 
					InternetAddress.parse(MailConfig.getOrdersEmailAddress() + "," + userEmail));
			msg.setContent(body, "text/html");
			Transport.send(msg);
			
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
		
		return true;
    }
    
    /**
     * Send simple email message. No attachments
     * @param from
     * @param to
     * @param cc
     * @param subject
     * @param body
     * @throws Exception
     */
    public void sendMail(
    		InternetAddress from, 
    		InternetAddress to, 
    		InternetAddress cc,
    		String subject, 
    		String body)throws Exception{
    	
    	// get session
    	Properties props = System.getProperties();
		props.put("mail.smtp.host", MailConfig.getMailSmtpHost());
		Session session = Session.getDefaultInstance(props,null);
		
		// create message
		Message newMessage = new MimeMessage(session);
		
		// set from, to, cc and subject
        newMessage.setFrom(from);
        newMessage.addRecipient(javax.mail.Message.RecipientType.TO, to);
        if(cc != null){
        	newMessage.addRecipient(javax.mail.Message.RecipientType.CC, cc);
        }
        newMessage.setSubject(subject);
        
        // set body
        newMessage.setText(body);
        
        // send new message
        Transport.send(newMessage);
		
    }
    
	
	public static void recalculateFreeForm(long searchId, UserAttributes currentAgent) {
		try {
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI docManager = search.getDocManager(); 
			//if(search.wasNeverOpened()) {
				try {
					docManager.getAccess();
					for(DocumentI doc : docManager.getDocumentsList()) {
						if(doc.getUserModifiedFields()!=null) {
							if(!doc.isFieldModified(DocumentI.Fields.GRANTOR)) {
								TSServer.calculateAndSetFreeForm(doc,PType.GRANTOR, search.getID(),currentAgent);	
							}
							if(!doc.isFieldModified(DocumentI.Fields.GRANTEE)) { 
								TSServer.calculateAndSetFreeForm(doc,PType.GRANTEE, search.getID(),currentAgent);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					docManager.releaseAccess();
				}
			//}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		try {
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			search.getOpenCount().incrementAndGet();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
package ro.cst.tsearch;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Timer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBManager.SearchAvailabitily;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.search.util.SearchRemoverTask;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.threads.deadlock.ReentrantInfoLock;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileLogger;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.ParameterNotFoundException;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XStreamManager;
import ro.cst.tsearch.utils.ZipUtils;

import com.stewart.ats.base.search.IllegalUsageException;

public class SearchManager {

	protected static final Category logger = Logger.getLogger(SearchManager.class);
	
    public static final int NEW_SEARCH = -1;
    private static final long REMOVE_DELAY = 1000 * 60;			//one minute
    private static final long REMOVE_PERIOD = 1000 * 60;		//one minute
    
    private static final ReentrantInfoLock lockAllSearches = new ReentrantInfoLock();
    private static final Hashtable allSearches 	       = new Hashtable();
    
    private static final Vector allFakeSearches           = new Vector();
    private static final Hashtable<Object, Object> toRemoveSearches = new Hashtable<Object, Object>();
    
    private SearchManager() {}

    public static Search getSearch(HttpServletRequest request) {

        long searchId = -1;
        MultipartParameterParser mppSec = null;

        try {
            if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
            	try{
            		mppSec = new MultipartParameterParser(request);
            	}
            	catch(Exception ioe){
            		//ioe.printStackTrace();
            		logger.error("getMultipart error, trying to use attributes or parameter to get the searchId");
            	}
                if (mppSec != null) {
                    searchId = mppSec.getMultipartLongParameter(RequestParams.SEARCH_ID);
                }
                else if( request.getAttribute( RequestParams.SEARCH_ID ) != null ){
                	searchId = request.getAttribute(RequestParams.SEARCH_ID) != null ? Long.parseLong((String)request
                            .getAttribute(RequestParams.SEARCH_ID)) : 0;
                }
                else {
                    searchId = request.getParameter(RequestParams.SEARCH_ID) != null ? Long.parseLong(request
                            .getParameter(RequestParams.SEARCH_ID)) : 0;
                }   
            } else if( request.getAttribute( RequestParams.SEARCH_ID ) != null ){
            	searchId = request.getAttribute(RequestParams.SEARCH_ID) != null ? Long.parseLong((String)request
                        .getAttribute(RequestParams.SEARCH_ID)) : 0;
            }
            else {
                searchId = request.getParameter(RequestParams.SEARCH_ID) != null ? Long.parseLong(request
                        .getParameter(RequestParams.SEARCH_ID)) : 0;
            }

        } catch (NumberFormatException e1) {
            e1.printStackTrace();
        } catch (ParameterNotFoundException e1) {
            e1.printStackTrace();
            return null;
        }

        return getSearch(searchId);
    }

    public static Search getSearch(long searchId) {
        return getSearch(searchId, true);
    }
    
    /**
     * holder for lazy initialization
     * @author radu bacrau
     */
    private static class NoneSearchHolder {
    	static final Search search = new Search (User.fakeUser, Search.SEARCH_NONE) {
			private static final long serialVersionUID = 1L;

			@Override
    		public void setID(long ID) {
    			long oldID = this.getID();
    			super.setID(ID);
    			try {
    				if( ID != oldID && !(oldID == 0 && ID == Search.SEARCH_NONE)) {
	    				String message = "Setting ID to a cached NoneSearchHolder, old ID: " 
							+ oldID + ", newID = " + ID + "\n";
	    				ThreadInfo threadInfo =  ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId());
	    				message += ServerInfoSingleton.getThreadInfoRepresentation(threadInfo);
	    				Log.sendEmail2("Setting ID to a cached NoneSearchHolder", message);
    				}
    			} catch (Exception e) {
					SearchManager.logger.error("Setting ID to a cached NoneSearchHolder, old ID: " 
							+ oldID + ", newID = " + ID, e);
				}
    		}
    	};
    }
    
    /**
     * holder for lazy initialization
     * @author radu bacrau
     */
    private static class ZeroSearchHolder{
    	static final Search search = new Search (User.fakeUser, 0) {
			private static final long serialVersionUID = 1L;

			@Override
    		public void setID(long ID) {
    			long oldID = this.getID();
    			super.setID(ID);
    			try {
    				if( ID != oldID) {
	    				String message = "Setting ID to a cached ZeroSearchHolder, old ID: " 
							+ oldID + ", newID = " + ID + "\n";
	    				ThreadInfo threadInfo =  ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId());
	    				message += ServerInfoSingleton.getThreadInfoRepresentation(threadInfo);
	    				Log.sendEmail2("Setting ID to a cached ZeroSearchHolder", message);
    				}
    			} catch (Exception e) {
					SearchManager.logger.error("Setting ID to a cached ZeroSearchHolder, old ID: " 
							+ oldID + ", newID = " + ID, e);
				}
    		}
    	};
    }
    
    public static Search getSearch(long searchId, boolean fakeIfNotExists) {

        if (searchId == NEW_SEARCH ) {
            return null;
        }
     
        // if dummy search requested, return the same instance each time, 
        // do not bother to create a new one
        if(searchId == Search.SEARCH_NONE){
        	return NoneSearchHolder.search;
        }
        
        // if dummy search requested, return the same instance each time, 
        // do not bother to create a new one
        if(searchId == 0){
        	return ZeroSearchHolder.search;
        }        

        lockAllSearches.lock();
        try {
        	Collection c = allSearches.values();
        	
            for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                Vector searches = (Vector) iterator.next();
                for (int i = 0; i < searches.size(); i++) {
                    Search search = (Search) searches.get(i);
                    if (search.getSearchID() == searchId) {
                        return search;
                    }
                }
            }
        } finally {
        	lockAllSearches.unlock();
        }
        
        /*
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
        if(search != null && search.getID() == searchId) {
        	return search;
        }
        */
        if (fakeIfNotExists) {
        	logger.debug("*****************    A CAUTAT SI NU A GASIT " + searchId);
            FileLogger.info( "*****************    A CAUTAT SI NU A GASIT " + searchId, FileLogger.SEARCH_OWNER_LOG );
        	status();
        }
        
        return fakeIfNotExists ? new Search(User.fakeUser, Search.SEARCH_NONE) : null;
        //throw new IllegalArgumentException("Search not found.");
    }
    
    
    public static Search getSearchByDBId(long id) {


        lockAllSearches.lock();
        try {
        	Collection c = allSearches.values();
        	
            for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                Vector searches = (Vector) iterator.next();
                for (int i = 0; i < searches.size(); i++) {
                    Search search = (Search) searches.get(i);
                    if (search.getID() == id) {
                        return search;
                    }
                }
            }
        } finally {
    		lockAllSearches.unlock();
    	}
        
        return null;
    }

        
    public static Search  addNewFakeSearch(User user) {
    	
        Search search;
        
        lockAllSearches.lock();
        try {
        	Vector userSearches = (Vector) allSearches.get(user);
            
            if (userSearches == null) {
                userSearches = new Vector();
                allSearches.put(user, userSearches);
            }
            
        	search = new Search(getNextSearchId());
        	search.setID(search.getSearchID());

            userSearches.add(search);
            allFakeSearches.add(search.getID());
            
            logger.debug("*****************    addNewSearch " + search.getSearchID());
            FileLogger.info( "*****************    addNewSearch " + search.getSearchID(), FileLogger.SEARCH_OWNER_LOG );
        	status();
    	} finally {
    		lockAllSearches.unlock();
    	}

        return search;   	
    }
    
    public static Search addNewSearch(User user, boolean interactive) {
        
        Search search;
        
        lockAllSearches.lock();
        try {
        	Vector userSearches = (Vector) allSearches.get(user);
            
            if (userSearches == null) {
                userSearches = new Vector();
                allSearches.put(user, userSearches);
            }
            
        	search = new Search(user, getNextSearchId());
        	search.setID(search.getSearchID());
        	if(interactive){
        		String dbOpenDate = DBManager.getSearchOpenDate(search.getSearchID(), null);
            	
            	if(StringUtils.isEmpty(dbOpenDate)){
            		DBManager.updateSearchOpenDate(search.getSearchID(), new Date());
            	}
        	}
        		

            userSearches.add(search);
            
            logger.debug("*****************    addNewSearch " + search.getSearchID());
            FileLogger.info( "*****************    addNewSearch " + search.getSearchID(), FileLogger.SEARCH_OWNER_LOG );
        	status();
    	} finally {
    		lockAllSearches.unlock();
    	}

        if(search!=null && interactive){
        	search.setOpenCount(new AtomicInteger(1));
		}
        
        return search;
    }
    
    public static void addBackgroundSearch(Search search) {

    	lockAllSearches.lock();
    	try {
    		
    		Vector backgroundSearches = (Vector) allSearches.get("Background");
            
            if (backgroundSearches == null) {
                backgroundSearches = new Vector();
                allSearches.put("Background", backgroundSearches);
            }

            backgroundSearches.add(search);
            
            logger.debug("*****************    addBackgroundSearch " + search.getSearchID());
            FileLogger.info( "*****************    addBackgroundSearch " + search.getSearchID(), FileLogger.SEARCH_OWNER_LOG );
            status();
		} finally {
			lockAllSearches.unlock();
		}
    }
    
    public static User getUser( Search search )
    {
    	User temp = null;

    	lockAllSearches.lock();
    	try {

	        Enumeration allUsers = allSearches.keys();
	        Object nextelem;
	        
	        Vector allUserSearches = null;
	        for(; allUsers.hasMoreElements() ;)
			{
	            nextelem = allUsers.nextElement();
	            if( nextelem instanceof User )
	            {
	                temp = (User)nextelem;
	                allUserSearches = (Vector) allSearches.get(temp);
	            }
	            else if( nextelem instanceof String )
	            {
	                allUserSearches = (Vector) allSearches.get( (String) nextelem);                
	            }
	            
				if( allUserSearches.contains( search ) )
				{
			    	try {
			    		logger.debug("1. Search " + search.getID() + " has user :" + temp.getUserAttributes().getID());
			    	}catch(Exception e) {}
			    	
				    return temp;
				}else {
					temp = null;
				}
			}
    	} finally {
			lockAllSearches.unlock();
		}
    	
    	try {
    		logger.debug("2. Search " + search.getID() + " has user :" + temp.getUserAttributes().getID());
    	}catch(Exception e) {}
        return temp;
    }
    
    public static void setSearch(Search newSearch, User user) {
        
    	lockAllSearches.lock();
    	try {
            Vector userSearches = (Vector) allSearches.get(user);
            
            if (userSearches == null) {
                userSearches = new Vector();
                allSearches.put(user, userSearches);
            }
        
            for (int i = 0; i < userSearches.size(); i++) {
                Search search = (Search) userSearches.get(i);
                if (search.getSearchID() == newSearch.getSearchID()) {
                    userSearches.remove(i);
                }
            }
            
            userSearches.add(newSearch);
            
            logger.debug("*****************    setSearch " + newSearch.getSearchID());
            FileLogger.info( "*****************    setSearch " + newSearch.getSearchID(), FileLogger.SEARCH_OWNER_LOG );
            status();
    	} finally {
			lockAllSearches.unlock();
		}
    }
        
    public static Hashtable getSearches() {
    	if( !lockAllSearches.isLocked() ){
			throw new IllegalUsageException(" Please call locklockAllSearches.lock() before calling SearchManager.getSearches() !!! ");
		}
		if( !lockAllSearches.isHeldByCurrentThread() ){
			throw new IllegalUsageException(" Please call locklockAllSearches.lock() before calling SearchManager.getSearches() !!!  ");
		}
        return allSearches;
    }
    
    @SuppressWarnings("unchecked")
	public static void removeSearches(final User user) {
    	
    	lockAllSearches.lock();
    	try {
    		
    		Vector<Search> userSearches = (Vector<Search>) allSearches.get(user);
    		if(userSearches == null){
    			return;
    		}
    		
    		for(final Search search: userSearches){
    			
        		new Thread(new Runnable() {
    				
    				@Override
    				public void run() {
    					// TODO Auto-generated method stub
    					// save search if it has a file no 	               
    	    			String abFileNo = search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
    	    			String agFileNo = search.getSa().getAtribute(SearchAttributes.ORDERBY_FILENO);
    	    			String productType = search.getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT);
    	    			if(StringUtils.isNotEmpty(productType) && (!StringUtils.isEmpty(abFileNo) || !StringUtils.isEmpty(agFileNo))){
    	    				SearchAvailabitily availability = DBManager.checkAvailability(
    							   search.getID(),
    							   user.getUserAttributes().getID().longValue(), 
    							   DBManager.CHECK_AVAILABLE, false);
    	    				logger.info("Saving search in ro.cst.tsearch.SearchManager.removeSearches(" + user.getUserAttributes().getLOGIN() + ")");
    	    				logger.info("Search status for " + search.getID() + " is " + availability.status);
    	    				try {
    	    					if(DBManager.getTSRGenerationStatus(search.getID())!= Search.SEARCH_TSR_CREATED 
    	    							&&
    	    							availability.status != DBManager.SEARCH_WAS_CLOSED
    	    							&&
    	    							availability.status != DBManager.SEARCH_AUTOMATIC_IN_PROGRESS
    	    							&&
    	    							!availability.tsrInProgress
    	    							) {
    	    						//don't mark this autosave as worked by the user.
    	    						search.setStartIntervalWorkDate(null);
									DBManager.saveCurrentSearch(
											user,
											search,
											Search.SEARCH_TSR_NOT_CREATED,
											null);
    	    					}
    	    				}catch(Exception e){
    	    					logger.error("Error while saving search in ro.cst.tsearch.SearchManager.removeSearches", e);
    	    				}
    	    			}
    	               
    	    			// remove search
    	    			ASThread thread = ASMaster.getSearch(search.getID());
    	    			GPThread gpt = GPMaster.getThread(search.getID());
    	    			if((thread == null || !thread.isAlive()) && gpt == null) {
    	    				removeSearch(search, true);
    	    			} 
    				}
    			}, 
    			"Remove Search " + search.getID() + " on user logout by " + user.getUserAttributes().getLOGIN())
        			.start();
    			
    			
    			
    		}
    		
    		allSearches.put(user, new Vector());
    	} finally {
			lockAllSearches.unlock();
		} 
    }
        
    /**
     * Move search with searchID in the list of user user
     * 
     * @param searchId
     * @param user
     */
    public static void moveSearch( long searchId, User user ){
    	
    	Search search = null;
    	
    	lockAllSearches.lock();
    	try {
    		//remove search from current location
            Collection c = allSearches.values();
            for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                Vector searches = (Vector) iterator.next();
                for (int i = searches.size() - 1; i >= 0; i--) {
                    Search tmpSearch = (Search) searches.get(i);
                    if (tmpSearch.getSearchID() == searchId) {
                    	searches.remove( i );
                    	
                    	search = tmpSearch;
                    	
                    	break;
                    }
                }
            }
            
            if( search != null ){
            	setSearch( search, user );
            }

    	} finally {
			lockAllSearches.unlock();
		} 
    	
    }
    
    /**
     * 
     * @param search
     * @param removeInstance
     */
    private static void removeSearch(Search search, boolean removeInstance){
    	
    	long searchId = search.getSearchID();
        HTTPManager.releaseSite(new BigDecimal(searchId));
        HttpManager.destroySearch(search);
        InstanceManager.getManager().getCurrentInstance(searchId).removeAllSites( searchId );
        search.setRequestClean(removeInstance);
        
        if(removeInstance) {
	        SearchRemoverTask srt = new SearchRemoverTask(search);
	        Timer timer = new Timer("SearchRemoverTask - " + searchId);
	        timer.schedule(srt, REMOVE_DELAY, REMOVE_PERIOD);
        }
        /*
        if(removeInstance){
        	
            InstanceManager.getManager().removeCurrentInstance(searchId);
            ReflectionUtils.nullifyReferenceFields(search);                                    	
        }
        */
        logger.debug("*****************    removeSearch " + search.getSearchID());
        FileLogger.info( "*****************    removeSearch " + search.getSearchID(), FileLogger.SEARCH_OWNER_LOG );    	
    }
    
    /**
     * 
     * @param searchId
     * @param removeNow
     * @param removeCurrentInstance - true if must remove current instance for this search
     */
    public static void removeSearch(long searchId, boolean removeCurrentInstance){
        
    	lockAllSearches.lock();
    	try {
            Collection c = allSearches.values();
            for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                Vector searches = (Vector) iterator.next();
                for (int i = searches.size() - 1; i >= 0; i--) {
                    Search search = (Search) searches.get(i);
                    if (search.getSearchID() == searchId) {
                        searches.remove(i);
                        removeSearch(search, removeCurrentInstance);
                        status();
                        break;
                    }
                }                
                if (searches.size() == 0){
                	iterator.remove();
                }
            }
        } finally {
			lockAllSearches.unlock();
		} 
    }
    
    public static void removeJustSearchFromLocalHash(long searchId){
        
    	lockAllSearches.lock();
    	try {
            Collection c = allSearches.values();
            for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                Vector searches = (Vector) iterator.next();
                for (int i = searches.size() - 1; i >= 0; i--) {
                    Search search = (Search) searches.get(i);
                    if (search.getSearchID() == searchId) {
                        searches.remove(i);
                        status();
                        break;
                    }
                }
                if (searches.size() == 0){
                	iterator.remove();
                }
            }
        } finally {
			lockAllSearches.unlock();
		} 
    }
    
    private static long getNextSearchId() {
        
        try {
            return DBManager.getNextId(DBConstants.TABLE_SEARCH);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
    
 
    /**
     * Removes a search from the not used list of searches
     * @param search
     */
    public static void removeFakeSearch(Search search,User user){
    	
    	  synchronized (allFakeSearches) {
    		   long searchId = search.getID();
    		    for (int i=0;i<allFakeSearches.size();i++)
    		    	 if (Long.parseLong(allFakeSearches.get(i).toString()) == searchId)
    		    		 allFakeSearches.remove(i);
    		  
    		    search.unFakeSearch(user);
    	  }
    }
    
    /**
     * Test if a search is not used in the application
     * @param search
     * @return
     */
    public static boolean isFakeSearch (Search search){
    	    	    
           return allFakeSearches.contains(search.getID());
    }
    
    
    public static void status() {
    	
    	lockAllSearches.lock();
    	try {
    		Enumeration users = allSearches.keys();
    		
    		logger.debug("**************************************");
            FileLogger.info( "**************************************", FileLogger.SEARCH_OWNER_LOG );
    		while ( users.hasMoreElements() ) {
    			
    			try {
    				
	    			Object key = users.nextElement();
	    			
	    			Vector searches = (Vector) allSearches.get(key);
	    			
	    			if (key instanceof User)
                    {
	    				logger.debug("User " + ( (User) key).getUserAttributes().getUserFullName());
                        FileLogger.info( "User " + ( (User) key).getUserAttributes().getUserFullName(), FileLogger.SEARCH_OWNER_LOG );
                    }
	    			else
                    {
	    				logger.debug("User " + key.toString());
                        FileLogger.info( "User " + key.toString(), FileLogger.SEARCH_OWNER_LOG );
                    }
	    			
	    			String s = "		Searches  ";
	    			for (int i = 0; i < searches.size(); i++) {
	    				s += ( ( (Search) searches.elementAt(i) ).getSearchID() + ", ");
	    			}
	    			
	    			logger.debug(s);
                    FileLogger.info( s, FileLogger.SEARCH_OWNER_LOG );
	    			
    			} catch (Exception e) {

    				e.printStackTrace();
    			}
    		}
            FileLogger.info( "**************************************", FileLogger.SEARCH_OWNER_LOG );
    	} finally {
			lockAllSearches.unlock();
		} 
    }
    
    /**
     * Loads a search from the disk or database<br/>
     * Returns null if the search was not found
     * @param searchId
     * @return the Search object corresponding to the searchId
     */
    public static Search getSearchFromDisk(long searchId) {
    	Search search = null;
    	String contextPath = null;
    	try {
            	
        	// try to load search from memory - because it was not found on thread
    		search = SearchManager.getSearch(searchId,false);
    		
    		if (search != null){ 
    			logger.info("Search " + searchId + " opened from memory.");
    			return search;
    		}
    		
    		//if the search is not in memory it tries to load it from disk
            contextPath = DBManager.getSearchTSRFolder(searchId);
            
            if(contextPath == null) {
            	return null;
            }
            
            XStreamManager xstream = XStreamManager.getInstance();
            try {
                
            	//REPLICATION
            	//check if context exists on disk, if not try to take it from database
            	File contextPathFile = new File( contextPath );
            	
            	if( !contextPathFile.exists() ){
            		//search not on disk, search it into database and unzip it on disk
            		System.err.print(" -  ----- //search not on disk, search it into database and unzip it on disk");
            		byte[] databaseContext = DBManager.loadSearchDataFromDB( searchId );
            		ZipUtils.unzipContext( databaseContext, contextPath, searchId );
            	}
            	else{
            		//if context exists on disk, check to see if we have the latest version
            		long searchDbVersion = DBManager.getSearchDBVersion( searchId );
            		long searchFolderVersion = Search.getSavedVersionFromFile( contextPath );
            		
            		if( searchDbVersion > searchFolderVersion ){
            			//database version is newer, get search from DB
            			
            			//erase old context
            			FileUtils.deleteDir( contextPathFile );

            			//get newer context from database
                		byte[] databaseContext = DBManager.loadSearchDataFromDB( searchId );
                		ZipUtils.unzipContext( databaseContext, contextPath, searchId );
            		}
            	}
            	
            	File file =new File(contextPath + "__search.xml");
            	
            	if( file.length()< 40 * 1024 * 1024){
            		String path = contextPath + "__search.xml";
                	Reader inputReader = new FileReader(path);
                    
                    StringBuffer sb = new StringBuffer();
                    char[] buffer= new char[1024];
        			while (true)
        			{
        				int bytes_read= inputReader.read(buffer);
        				if (bytes_read == -1)
        					break;
        				
        				sb.append(buffer, 0, bytes_read);
        			}
        			inputReader.close();
        			StringBuffer strBuf = new StringBuffer (sb.toString().replaceAll("/opt/resin/webapps/title-search/TSD", "/opt/TSD"));
        			
    				System.err.println("#####################################################");
    				System.err.println("SIZE = " +strBuf.length()+" compare to " + 40 * 1024 * 1024+ " FILE_NAME = ");
    				String searchString = sb.toString()
    						.replaceAll("/opt/resin/webapps/title-search/TSD", "/opt/TSD")
    						.replace( "<readyToProcess>false</readyToProcess>", "<readyToProcess>true</readyToProcess>" );
    				
    				search = (Search)xstream.fromXML( searchString );
    				search.makeServerCompatible(contextPath);
    				
				} else {
    				
    				System.err.println("SIZE = bigger then " + 40 * 1024 * 1024+ " FILE_NAME = ");
    				System.err.println("##########------- BLANK PAGE  -------########");
    				
    				return null;
    			}                    
            } catch(Exception e) {
                e.printStackTrace();
            }        					
		}
    	catch (Exception e) {
		    e.printStackTrace();
		}
    	
    	if(search != null && ServerConfig.isEnableLogOldField()){
			InstanceManager.getManager().getCurrentInstance(searchId).setCrtSearchContext(search);
			if(contextPath != null) {
				try {
					//let's update the log file from the database
	            	File logFile = new File(contextPath + "logFile.html");
	            	byte[] logContentFromDatabase = 
	            		DBManager.getSearchOrderLogs(search.getID(), FileServlet.VIEW_LOG_OLD_STYLE, true);
	            	if(!logFile.exists()) {
	            		if(logContentFromDatabase !=null) {
	            			org.apache.commons.io.FileUtils.writeByteArrayToFile(logFile, logContentFromDatabase);
	            		} else {
	            			logger.error("Failed to update search log from the database for searchId = " + searchId);
	            		}
	            	} else {
		            	byte[] logContentFromDisk = org.apache.commons.io.FileUtils.readFileToByteArray(logFile);
		            	
		            	if(logContentFromDatabase != null &&
		            			logContentFromDisk != null &&
		            			logContentFromDatabase.length != logContentFromDisk.length) {
		            		if(logFile.delete()) {
		            			org.apache.commons.io.FileUtils.writeByteArrayToFile(logFile, logContentFromDatabase);
		            			logger.debug("Succesfully updated search log from the database for searchId = " + searchId);
		            		} else {
		            			logger.error("Failed to update search log from the database for searchId = " + searchId);
		            		}
		            	} else {
		            		logger.debug("No need to update the search log from the database for searchId = " + searchId);
		            	}
	            	}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
    	
    	return search;
    }

	public static ReentrantInfoLock getLockAllSearches() {
		return lockAllSearches;
	}
    
}
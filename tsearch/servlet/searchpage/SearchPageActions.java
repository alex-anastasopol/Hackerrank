package ro.cst.tsearch.servlet.searchpage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.database.rowmapper.SearchUserTimeMapper;
import ro.cst.tsearch.database.transactions.SaveSearchTransaction;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.servlet.TSD;
import ro.cst.tsearch.threads.ASStopperThread;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningInfo;

public class SearchPageActions extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		HttpSession session = request.getSession();
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		//long commId = ua.getCOMMID().longValue();

		ParameterParser pp = new ParameterParser(request);

		//long searchId = pp.getLongParameter(RequestParams.SEARCH_ID, -1);
		int opCode = pp.getIntParameter(TSOpCode.OPCODE, 0);
		
		if(opCode == TSOpCode.CANCEL_SEARCH) {
			long startTimeCancel = System.currentTimeMillis();
		  	Search oldSearch = (Search) currentUser.getSearch(request);
		  	
		  	DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(
		  			oldSearch.getID(),
		  			ua.getID().longValue(), 
		  			DBManager.CHECK_OWNER, 
		  			false);
		    	
			if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {

	    	    String errorBody = searchAvailable.getErrorMessage();
	    	    	    		
	    		request.setAttribute(RequestParams.ERROR_BODY, errorBody);
	    		request.getRequestDispatcher(URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS)
	    			.forward( request,  response);
	    		return;
	    	}
			System.err.println("Time CANCEL1: " + (System.currentTimeMillis()-startTimeCancel) + " millis, searchIdOld = " + oldSearch.getID());
		  	
			// copy search order file
//	        File file = new File(oldSearch.getSearchDir() + "orderFile.html");
//	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	        boolean isSearchOrder = file.exists();
//	        if (isSearchOrder) {
//	            IOUtil.copy(new FileInputStream(file), baos);
//	        } else if(DBReports.hasOrderInDatabase(oldSearch.getID())) {
//	        	byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(oldSearch.getID(), FileServlet.VIEW_ORDER, false);
//    			if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0) {
//    				FileUtils.writeByteArrayToFile(file, orderFileAsByteArray);
//    				isSearchOrder = file.exists();
//    		        if (isSearchOrder) {
//    		            IOUtil.copy(new FileInputStream(file), baos);
//    		        }
//    			} else {
//    				System.err.println("Could not find file and content while cancelling " + file.getAbsolutePath());
//    			}
//	        } else {
//	        	System.err.println("Could not find file while cancelling " + file.getAbsolutePath());
//	        }
	        if (currentUser != null && oldSearch.getAgent() != null)
		        currentUser.getUserAttributes().setAGENTID(oldSearch.getAgent().getID());
			
	        Search search = new Search(currentUser, DBManager.getNextId(DBConstants.TABLE_SEARCH));
			search.setAgent(oldSearch.getAgent());
			
			if (oldSearch.getOrigSA() != null){
				search.setSa(oldSearch.getOrigSA());
				search.getSa().restoreSearchCriteria();
			} else {
				search.setSa(oldSearch.getSa());
			}
			
			search.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "");
			search.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, "");
			search.getSearchFlags().getWarningList().remove(new WarningInfo(Warning.WARNING_NO_LEGAL_SEARCH_WAS_PERFORMED_ID));
			search.getSa().setDefaultToDate();
			Boolean[] w = (Boolean[])search.getSa().getObjectAtribute(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING);
			if (w[0] != null || w[1] != null || w[2] != null || w[3] != null){
				search.getSa().setObjectAtribute(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING, new Boolean[]{null, false, null, null});
			}
			if (search.getSa().getAtribute(SearchAttributes.POOR_SEARCH_DATA) != null){
				search.getSa().setAtribute(SearchAttributes.POOR_SEARCH_DATA, null);
			}	
			search.setOrigSA(oldSearch.getSa());
			search.getSa().setAbstractor(currentUser.getUserAttributes());
			search.setSecondaryAbstractorId(oldSearch.getSecondaryAbstractorId());
			search.setSearchFlags(oldSearch.getSearchFlags());
			
			//B 4484
			search.setTSROrderDate(oldSearch.getTSROrderDate());
			search.setSearchDueDate(DBManager.getSearchDueDate(oldSearch.getID()));
			
			w = (Boolean[])search.getOrigSA().getObjectAtribute(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING);
			if (w[0] != null || w[1] != null || w[2] != null || w[3] != null){
				search.getOrigSA().setObjectAtribute(SearchAttributes.INITIAL_SEARCH_MIDDLE_NAME_MISSING, new Boolean[]{null, false, null, null});
			}
			if (search.getOrigSA().getAtribute(SearchAttributes.POOR_SEARCH_DATA) != null){
				search.getOrigSA().setAtribute(SearchAttributes.POOR_SEARCH_DATA, null);
			}		
			
			search.setID(search.getSearchID());
			search.constructSearchDirs();				//constructs the folders structure

			InstanceManager.getManager().setCurrentInstance(search.getID(), InstanceManager.getManager().getCurrentInstance(oldSearch.getID()));
			InstanceManager.getManager().getCurrentInstance(search.getID()).setCrtSearchContext(search);
			
			// write back the search order file
//	        if (isSearchOrder) {
//				try {
//					FileOutputStream orderFile = new FileOutputStream(search.getSearchDir() + "orderFile.html");
//					orderFile.write(baos.toByteArray());
//					orderFile.flush();
//					orderFile.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//	        }
	        
	        long oldSearchId = oldSearch.getID();
			String oldSearchDir = oldSearch.getSearchDir();
			
			search.copyDatabaseFieldsFrom(oldSearch);
			
			// we need to do this before removing the old log from DB
			SearchLogger.copyLogFromOldSearchToNewSearch(oldSearchId, oldSearchDir, search);
			search.copyLogInSambaFrom(oldSearch.getID());
			
			SearchUserTimeMapper.copyUserTime(oldSearch, search);
			
			if (oldSearch.isUpdate()) {
				search.setTS_SEARCH_STATUS(Search.SEARCH_STATUS_T);						
				try {
					int searchSaved = DBManager.saveCurrentSearch(currentUser, search, Search.SEARCH_TSR_NOT_CREATED, null);
					if (searchSaved == SaveSearchTransaction.STATUS_SUCCES){
						search.setNotes(oldSearch.getNotes(oldSearchId));
					}
					
					if(DBManager.deleteSearch( oldSearch.getID(), Search.SEARCH_TSR_NOT_CREATED ) == 0) {
						DBManager.deleteSearch( oldSearch.getID(), Search.SEARCH_TSR_CREATED );
					}
				} catch (SaveSearchException e) {
					e.printStackTrace();
				}
				search.setUpdate(true);
			}
			search.setWasOpened(true);
			search.setOpenCount(new AtomicInteger(1));
			
			String sPath = getServletConfig().getServletContext().getRealPath(request.getContextPath());
			
			InstanceManager.getManager().setCurrentInstance(search.getID(), InstanceManager.getManager().getCurrentInstance(oldSearch.getID()));
			InstanceManager.getManager().getCurrentInstance(search.getID()).setCrtSearchContext(search);
			
			SearchManager.setSearch(search, currentUser);
			
			TSD.initSearch(currentUser, search, sPath, request);
			
			RequestCount.updateRequestCountSearchID(oldSearchId, search.getID()); //task 7846
			
			
			
			ASStopperThread stopper = new ASStopperThread(oldSearch);
	        stopper.start();
	        oldSearch.makeServerCompatible("");
	        
	     	//log that this search is the result of a cancel operation		
	      	String msgStr = "\n</div><div><BR><b>The initial order with SearchId: " + oldSearchId + " was canceled on: </b>" 
	   						+ SearchLogger.getTimeStamp(search.getID()) + "<b> and the new SearchId is " + search.getID()
	      					+ "</b></BR></div>\n";
	      	SearchLogger.info(msgStr, search.getID());
	      	//done logging
	      	
			response.getWriter().println("<script language=\"JavaScript\"> ");
			try {
				if ((Long) session.getAttribute("searchIdHome") == oldSearch.getID()) {
					session.setAttribute("searchIdHome", search.getSearchID());
				}
			}catch(Exception e) {}
			response.getWriter().println("top.location=\"" + URLMaping.path + URLMaping.StartTSPage + 
					"?" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_TS + "&searchId=" + search.getSearchID() + "\";");
			response.getWriter().println("</script>");
			
			return;
		}
	}
	

}

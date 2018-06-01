package ro.cst.tsearch.servlet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.titledocument.abstracts.FidelityTSD;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileCopy;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

@Deprecated
public class ConvertToPdf extends BaseServlet {
    
	
	private static final long serialVersionUID = -6807835162452851307L;
	private static final Category logger = Logger.getLogger(ConvertToPdf.class);
    public static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    
    public static SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmmss");
    
    public static Random randGen = new Random();
    
    static { 
        sdf.setLenient(false);

    }
    
	public void doRequest(HttpServletRequest request, HttpServletResponse response) 
		throws IOException, ServletException {
	    
	    HttpSession session = request.getSession();
	    String testFromTSD = request.getParameter("TSD");
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		
		Search global = (Search) currentUser.getSearch(request);
		
	    
		boolean boolFromTSD = false;
		if( testFromTSD!=null && testFromTSD.equalsIgnoreCase("true")){
			boolFromTSD =true;
		}
		
		int opCode = -1;
	    try {
	        opCode = Integer.parseInt(request.getParameter(TSOpCode.OPCODE));
	    } catch (Exception e) {
	        opCode = TSOpCode.CREATE_TSR;
	        e.printStackTrace();
	    }

	    /*
	     * The presence of userResponse indicates that this is a response from CreateTSROptions.jsp
	     * "0" - create TSR and continue search
	     * "TSOpCode.REMOVE_OLD_ADD_AND_NEW_SEARCH"
	     */
		String userResponse = request.getParameter("userResponse");
		if(userResponse != null) {
			
			try{
				
				long newID;
				
				if("0".equals(userResponse)){								
			        
					// clonam search-ul curent
			    	Search localSearch = (Search) global.clone();
			        
			        // setam noile id-uri pentru search-ul clonat
			    	localSearch.setID(DBManager.getNextId(DBConstants.TABLE_SEARCH));
			        localSearch.setSearchID(localSearch.getID());
			        
			        // inlocuim id-ul search-ului initial cu noul id in toate link-urile
			        localSearch = Search.replaceAllSearchId(localSearch, global.getSearchID());
			
			        // copiem directorul search-ului curent
			        FileCopy.copy(new File(global.getSearchDir()), new File(localSearch.getSearchDir()));
			        
			        SearchManager.setSearch(localSearch, currentUser);
			        
			        session.setAttribute("new_search_" + localSearch.getID(), localSearch);
			        newID = localSearch.getID();
					
				}else{			
					// use old search as new search
					session.setAttribute("new_search_" + global.getID(), global);
					newID = global.getID();
				}
				
		        // -- setez numele fisierului TSR final aici pentru un workaround pt duplicarea linkurilor in reports				
				SearchAttributes sa = global.getSa();
				sa.setAbstractorFileName(global);
				
				// forward to createTSD
				forward(request, response, URLMaping.CREATE_TSD + "?local_dispatcher=1&versign=1&newID=" + newID + "&bypass=true" + "&actionId=" + userResponse);

			}catch(Exception e){
				e.printStackTrace();
			}
		        
			return;
		}

	    if (opCode == TSOpCode.CREATE_TSR) {
	        
	        DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(global.getID(), currentUser.getUserAttributes().getID().longValue(),DBManager.CHECK_OWNER, false);
	    	
			if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {

	    	    String errorBody = searchAvailable.getErrorMessage();
	    	    	    		
	    		request.setAttribute(RequestParams.ERROR_BODY, errorBody);
	    		
	    		forward(request, response, URLMaping.REDIRECT_TO_OPENER + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
	    		
	    		return;
	    	}
    		
		

		    try {
			    
		    	//verificam daca are agent setat pe search
                String errorBody = "";
                
                if( global.getSa().getAbstractorFileName().indexOf( "UnknownFileNo" ) >= 0 )
                {
                    errorBody += "Please provide a FileID!";
                }
                        
		    	UserAttributes currentAgent = global.getAgent();
			    if (currentAgent == null) {
                    errorBody += "\\r\\nYou have to select one agent in order to complete your Create TSR operation!";
			    
			    }
			    else{
			    	try{
				    	List<CommunityTemplatesMapper>userTemplates = UserUtils.getUserTemplates(currentAgent.getID().longValue(),global.getProductId());
				    	if(!FidelityTSD.containsTSDTemplate(userTemplates)){
				    		errorBody += "\\r\\n"+TemplatesServlet.TSD_TEMPLATE_UNCHECKED_MESSAGE;
				    	}
			    	}
			    	catch(Exception e){
			    		if(e.getMessage().contains("No templates defined for this user")){
			    			errorBody += "\\r\\n"+TemplatesServlet.TSD_TEMPLATE_UNCHECKED_MESSAGE;
			    		}
			    	}
			    }
                
                try
                {
                    errorBody = URLEncoder.encode( errorBody , "UTF-8");
                }
                catch( Exception e ) {}
                
                if( !"".equals( errorBody ) )
                {
                    request.setAttribute(RequestParams.ERROR_BODY, errorBody );
                    forward(request, response, URLMaping.REDIRECT_TO_OPENER + 
                            "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS +
                            "&" + RequestParams.ERROR_TYPE + "=" + TSOpCode.NO_AGENT_ERROR);
                    return;                        
                }
        
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
					
	        
			if (!boolFromTSD) {
				forward(request, response, "/jsp/TSR/CreateTSROptions.jsp");
			}	
			
	        return;
	    }
		
	    if(!boolFromTSD){
	
        	// set search status as TSR in progress, to don't allow save of the search at this moment
        	DBManager.setSearchTSRCreationInProgress(global.getSearchID());
        	
		    GPThread thread = GPMaster.generateTSR(global, currentUser,
		            InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentCommunity());
	
		  //if by any chance this thread was already started don't create another one
		    if(thread.isAlive()) {
		    	//this shouldn'b be happening 
		    	Log.sendEmail("Thread: " + thread.getName() + " is already running. " +
		    			"Unauthorized access while trying to start the same thread for creating TSR in ConvertToPdf");
		    	
		    } else {
		    
			    thread.TSRFolder = request.getParameter("PDFFileName");
			    thread.TSRFileName = request.getParameter("PDFFileNameShort");
			    thread.COMFileName = request.getParameter("COMFileName");
			    thread.TSDFileName = request.getParameter("TSDFileName");
			    thread.contextPath = request.getContextPath();	    
			    thread.currentCounty = InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentCounty();
			    thread.currentState = InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentState();
			    
			    String newID = request.getParameter("newID");
			    
			    logger.info("\n\n\n\n\nConvertToPDF - Start GPThread.\n\n\n\n\n");
			    
			    thread.start();
		    		    
		    }
		    forward(request, response, 
		            URLMaping.CONVERT_TO_PDF_REDIR 
		            //+ "?" + RequestParams.SEARCH_ID + "=" + global.getSearchID() 
		            + "?" + TSOpCode.OPCODE + "=" + TSOpCode.CREATE_TSR_DONE);
	    }
	}
	
	public static void copyFile(String source, String destination) {
        
	    File src = new File(source);
        File des = new File(destination);
        
        try {
            
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(des);

            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while((bytesRead=in.read(buffer))>0)
                out.write(buffer, 0, bytesRead);

            in.close();
            out.close();
            
        } catch (IOException e) {
            logger.info(e.getMessage());
            e.printStackTrace();
        }
    }
}

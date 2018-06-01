package ro.cst.tsearch.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;






import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.ServletUtils;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class URLConnectionReader extends BaseServlet {

    
	private static final long serialVersionUID = 1923674591L;

	private static final Category logger = Logger.getLogger(URLConnectionReader.class);

    public static final int SAVE_TO_TSD_ACTION = -3;
    public static final int MULTIPLE_SAVE_TO_TSD_ACTION = -4;
    public static final int CONTINUE_TO_NEXT_SERVER_ACTION = -5;

    public final static String PRM_NAME_P1 = "p1"; //the parameter name which
                                                   // identify the server in jsp
                                                   // or the user name

    public final static String PRM_NAME_P2 = "p2"; //the parameter name which
                                                   // identify the server in jsp
                                                   // or the password

    private static final String PRM_NAME_LINK = "Link"; //the parameter name wich
                                                 // identify that is a call from
                                                 // a link which server itself
                                                 // built and it know how to
                                                 // solve it

    public static final String PRM_DISPATCHER = "dispatcher"; //site dispacher
    public static final String ERROR_MESSAGE_DOWNLOAD_IMAGE = 
    	"<font color='red'>Error getting image from the document server ! Close this page and continue your search!</font>";
                                                       
    //so, after we make a request and got sesion ID expired, then we delete the
    // old seesion ID, get a new one and repeat the request
    public void doRequest(HttpServletRequest request, HttpServletResponse response)
    	throws IOException, ServletException, BaseException {
        //I don't need to keep the connection occupied
        //InstanceManager.getCurrentInstance().releaseDBConnection();
        String sP1 = null;
        String sP2 = null;

        int iDispatcher = -1;
        String initialNumberOfDocsMultiDocSave = null;
        String sDispatcher = request.getParameter(PRM_DISPATCHER);
        if (sDispatcher != null && !sDispatcher.equals(""))
            iDispatcher = Integer.parseInt(sDispatcher);

        if (iDispatcher == SAVE_TO_TSD_ACTION || iDispatcher == MULTIPLE_SAVE_TO_TSD_ACTION ||
        		iDispatcher == CONTINUE_TO_NEXT_SERVER_ACTION) {
            
        	initialNumberOfDocsMultiDocSave = request.getParameter("initialNumberOfDocsMultiDocSave");
        	String parameters = request.getParameter("ServerID");
            sP1 = parameters.substring(parameters.indexOf(PRM_NAME_P1) + PRM_NAME_P1.length() + 1, parameters.indexOf(PRM_NAME_P2) - 1);
            sP2 = parameters.substring(parameters.indexOf(PRM_NAME_P2) + PRM_NAME_P2.length() + 1);
        }
        else {
            sP1 = request.getParameter(PRM_NAME_P1);
            sP2 = request.getParameter(PRM_NAME_P2);
        }
        String parentSiteSaveType = request.getParameter(RequestParams.PARENT_SITE_SAVE_TYPE);
        if(parentSiteSaveType == null || parentSiteSaveType.equals(""))
        	parentSiteSaveType = RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF;
        String parentSiteSaveTypeCombined = request.getParameter(RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED);
        if(parentSiteSaveTypeCombined == null || parentSiteSaveTypeCombined.equals(""))
        	parentSiteSaveTypeCombined = RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF;
        
        if (parentSiteSaveType.equals(RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF)) {
        	parentSiteSaveType = "";
        } else if (parentSiteSaveTypeCombined.equals(RequestParamsValues.PARENT_SITE_SAVE_TYPE_COMBINED_CROSSREF)) {
        	parentSiteSaveType = "&saveSomeWithCrossRef=true";
        } else {
        	parentSiteSaveType = "&saveWithoutCrossRef=true";
        }
        	
        String forUpdateSearchParams = request.getParameter(RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS);
        
        Map<String, Object> extraParams = new HashMap<String, Object>();
        if("on".equals(forUpdateSearchParams)) {
        	extraParams.put(RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS, true);
        }
        
        
        //      check session
        HttpSession session = request.getSession(true);
        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
        Search global = currentUser.getSearch(request);
        
        if (global != null){
        	global.clearVisitedLinks();
        	global.clearValidatedLinks();
        	global.clearRecursiveAnalisedLinks();
        }
        
		int initialNumberOfDocs = (initialNumberOfDocsMultiDocSave == null ? global.getNumberOfDocs() : Integer.parseInt(initialNumberOfDocsMultiDocSave));
		
		Set<String> initialListOfPADocs = getListOfPADocs(global);
        
        //get server
        TSInterface intrfServer = TSServersFactory.GetServerInstance(sP1, sP2,global.getID());
        if( global.getSearchType() == Search.PARENT_SITE_SEARCH )
        	intrfServer.setParentSite(true);
       
        String realPath = BaseServlet.FILES_PATH;
        intrfServer.setServerForTsd(global, realPath);
        logger.debug(" realPath = [" + realPath + "]");

        
        // verificam daca are acces la search
        logger.info("verificam daca userul are acces la search....");
        DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(global.getID(),currentUser.getUserAttributes().getID().longValue(), DBManager.CHECK_OWNER, false);
    	
		if (  searchAvailable.status != DBManager.SEARCH_AVAILABLE ) {

    	    String errorBody = searchAvailable.getErrorMessage();
    	    	    		
    		request.setAttribute(RequestParams.ERROR_BODY, errorBody);
    		
    		forward(request, response, URLMaping.StartTSPage + "?" + TSOpCode.OPCODE + "=" + TSOpCode.ERROR_OCCURS);
    		
    		return;
    	}
        
		boolean requestNumberOfSavedDocuments = "1".equals(request.getParameter(RequestParams.PARENT_SITE_REQUEST_NUMBER_OF_SAVED_DOCUMENTS));
        
        String requestParam = "";
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = (String) parameterNames.nextElement();
            if (name.startsWith("___")) {
                
                String value = request.getParameter(name);
                requestParam += "&" + name.replaceFirst("___", "") + "=" + value;
                
            } else if ( (name.startsWith("__") || name.startsWith("_ctl")) &&
            			!( /* added to prevent parameter duplicates */ request.getQueryString().contains( name ) )) {
                String value = request.getParameter(name);
                requestParam += "&" + name + "=" + value;
            }
        }
        
        logger.debug("Starting URLConnectionReader");
        int iServerAction;
        TSServerInfoModule module = null;
        String requestParams = "";
        String[] docLink = new String[0];
        
        if (iDispatcher == SAVE_TO_TSD_ACTION) {
           
        	requestParams = request.getParameter(TSServer.SAVE_TO_TSD_PARAM_NAME);
            iServerAction = TSServer.REQUEST_SAVE_TO_TSD;

        } else if (iDispatcher == MULTIPLE_SAVE_TO_TSD_ACTION) {
        	
        	docLink = request.getParameterValues("docLink");
        	if (docLink == null) docLink = new String[0];
        	
            iServerAction = TSServer.REQUEST_SAVE_TO_TSD;
        } else if ( iDispatcher == CONTINUE_TO_NEXT_SERVER_ACTION ) {
        	
        	docLink = request.getParameterValues("docLink");
        	if (docLink == null) docLink = new String[0];
        	
        	iServerAction = TSServer.REQUEST_CONTINUE_TO_NEXT_SERVER;
        } else if (request.getParameter(PRM_NAME_LINK) == null ) {
        	
            module = intrfServer.getCurrentClassServerInfo().getModuleForSearch(iDispatcher, new SearchDataWrapper(request));
            HashMap<String, String> selectBoxes = new HashMap<String, String>();
            
            // 4 knox RO
            if (sP1.equals("097") && sP2.equals("1")) {
                String[] ss = { "" };
                ss = request.getParameterValues("param_12_13");
                if (ss != null)
                    if (ss.length > 0) {
                        int iFunc;
                        for (int iter = 0; iter < ss.length; iter++) {
                            iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam("DocTypes", ss[iter]);
                            module.getFunction(iFunc).setParamValue(ss[iter]);
                        }
                    }
                
            } else if (sP1.equals("070") && sP2.equals("1")) {
                // KSJohnsonRO
                String[] ss = { "" };
                ss = request.getParameterValues("param_12_20");
                
                if (ss != null)
                    if (ss.length > 0) {
                        int iFunc;
                        for (int iter = 0; iter < ss.length; iter++) {
                            iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam("DocTypes", ss[iter]);
                            module.getFunction(iFunc).setParamValue(ss[iter]);
                        }
                    }
                
            }else if (sP1.equals("06112") && sP2.equals("0")) {
                // ARPulaskiAO
                String[] ss = { "" };
                ss = request.getParameterValues("param_0_20");
                
                if (ss != null)
                    if (ss.length > 0) {
                        int iFunc;
                        for (int iter = 0; iter < ss.length; iter++) {
                            iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam("Subdivision", ss[iter]);
                            module.getFunction(iFunc).setParamValue(ss[iter]);
                        }
                    }
                
            } else if (sP1.equals("019") && sP2.equals("1")) {
            	selectBoxes.put("param_0_13", "DocTypeCats");
            	selectBoxes.put("param_0_19", "DocTypes");
            } else if (sP1.equals("06282") && sP2.equals("1")) {//IL Kane RO
				String[] ss = null;
				if (module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
					ss = request.getParameterValues("param_12_26");// advanced module
				}
				else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX39) {
					ss = request.getParameterValues("param_39_0");// docType module
				}
				// IL Kane RO moofed
                if (ss != null)
                    if (ss.length > 0) {
                        int iFunc;
                        for (int iter = 0; iter < ss.length; iter++) {
                            iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam("oDocumentType.InstrumentCodes", ss[iter]);
                            module.getFunction(iFunc).setParamValue(ss[iter]);
                        }
                    }
            } else if (sP1.equals("06617") && sP2.equals("2")) {
               String[] ss = { "" };
               int moduleIdx = module.getModuleIdx();
               if (moduleIdx == 0) {
            	   ss = request.getParameterValues("param_0_4");
               } else if (moduleIdx == 1) {
            	   ss = request.getParameterValues("param_1_3");
               } else if (moduleIdx == 2) {
            	   ss = request.getParameterValues("param_2_3");
               } else if (moduleIdx == 15) {
            	   ss = request.getParameterValues("param_15_3");
               } 
            
              // MO Cass TR moofed
                if (ss != null)
                    if (ss.length > 0) {
                        int iFunc;
                        for (int iter = 0; iter < ss.length; iter++) {
                            iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam("TaxYear", ss[iter]);
                            module.getFunction(3).setParamValue(ss[iter]);
                            module.getFunction(iFunc).setParamValue(ss[iter]);
                        }
                    }
            } else if (sP1.equals("074") && sP2.equals("1")) { //OHFranklinRO
            	selectBoxes.put("param_12_13", "DocTypes");
            } else if( sP1.equals( "082" ) && sP2.equals( "1" ) ){ //Wayne RO
            	
            	String[] ss = { "" };
            	ss = request.getParameterValues( "param_3_3" );
            	
                if (ss != null)
                    if (ss.length > 0) {
                        int iFunc;
                        for (int iter = 0; iter < ss.length; iter++) {
                            iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam("f:d3", ss[iter]);
                            module.getFunction(iFunc).setParamValue(ss[iter]);
                        }
                    }
            } else if (sP1.equals("06119") && sP2.equals("1")) { // ILWill RO

				selectBoxes.put("param_0_22", "DocTypeCats");
				selectBoxes.put("param_0_23", "DocTypes");
            
            } else if (sP1.equals("06009") && sP2.equals("37")) { // FLGenericMFCServerRO2
            	selectBoxes.put("param_0_5", "documentTypes");

			} else if (sP1.equals("06113") && sP2.equals("1")) { // NVClark RO
				// Retrieve the Category combo box array and implode it.
				String[] ss = request.getParameterValues("param_2_1");

				if (ss == null) {
					module.getFunction(1).setParamValue(
							"--- ALL DOCUMENT TYPES ---");
				} else {
					String implode = "";
					for (int i = 0; i < ss.length; i++) {
						if (i != 0) {
							implode += ",";
						}
						implode += ss[i];
					}

					// Set the imploded value. (eg. AFF, AFFLP etc.)
					module.getFunction(1).setParamValue(implode);
				}

			} else if (sP2.equals("8")) { // PC
            	selectBoxes.put("param_18_0", "all_region");
            	selectBoxes.put("param_19_0", "bk_region");
            	selectBoxes.put("param_19_15", "chapter");
            	selectBoxes.put("param_25_0", "dc_region");
            	selectBoxes.put("param_25_22", "nos");
            	selectBoxes.put("param_26_0", "dc_region");
            	selectBoxes.put("param_27_0", "ap_region");
            	selectBoxes.put("param_27_22", "ap_nos");
            	selectBoxes.put("param_28_0", "dc_region");
            } else if("52".equals(sP2)){ //TXStateBarServerLW
            	selectBoxes.put("param_0_12", "TBLSCertified");
            	selectBoxes.put("param_0_13", "PracticeArea");
            	selectBoxes.put("param_0_14", "ServicesProvided");
            	selectBoxes.put("param_0_15", "LanguagesSpoken");
            	selectBoxes.put("param_0_16", "LawSchool");
            } else if (sP2.equals(Integer.toString(GWTDataSite.ATS_TYPE))) { // ATS
            	selectBoxes.put("param_103_2", "product");
            	selectBoxes.put("param_26_2", "product");
            	selectBoxes.put("param_2_2", "product");
            	selectBoxes.put("param_0_3", "product");
            	selectBoxes.put("param_7_8", "product");
            	selectBoxes.put("param_1_4", "product");
            } else if (sP1.equals("07098") && sP2.equals("1") ) {
            	selectBoxes.put("param_0_6", "SearchDocType"); //AR Benton RO
            } else if (sP1.equals("06813") && sP2.equals("1") ) {
            	//TX Bexar RO
            	
            	if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX)
            		selectBoxes.put("param_0_5", "SearchDocType"); 
            	
            	if(module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX)
            		selectBoxes.put("param_2_5", "SearchDocType"); 
            	
            	if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX)
            		selectBoxes.put("param_1_10", "SearchDocType");
            	
            	if(module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX)
            		selectBoxes.put("param_3_8", "SearchDocType");
            	
            	if(module.getModuleIdx() == TSServerInfo.BGN_END_DATE_MODULE_IDX)
            		selectBoxes.put("param_4_2", "SearchDocType"); 
            	
            } else if ( sP2.equals("49") ) {	//GenericBS
            	if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
            		String[] ss = { "" };
                    
            		ss = request.getParameterValues("param_0_9");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam("lbState", ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                    
                    ss = request.getParameterValues("param_0_11");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam("lbCountry", ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                    
                    ss = request.getParameterValues("param_0_13");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam("lbTypeOfInstitution", ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                        
            	}
            } else if (sP1.equals("07165") && sP2.equals("1") ) {	//AR Washington RO
            	
            	if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX ||
            		module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX ||
            		module.getModuleIdx() == TSServerInfo.CONDOMIN_MODULE_IDX ||
            		module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX) {
            	
            		String indexTypesParameter = "ctl00$cphMain$SrchIndexInformation1$lbIndexTypes";
            		String groupsParameter = "ctl00$cphMain$SrchIndexInformation1$lbKindGroups";
            		String kindParameter = "ctl00$cphMain$SrchIndexInformation1$lbKinds";
            		
            		String suffix = "";
            		if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX) {
            			suffix = "_2";
            		} else if (module.getModuleIdx() == TSServerInfo.CONDOMIN_MODULE_IDX) {
            			suffix = "_3";
            		} if (module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX) {
            			suffix = "_4";
            		} 
            		
            		String[] ss = { "" };
                    
            		ss = request.getParameterValues(indexTypesParameter+suffix);
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam(indexTypesParameter, ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                    
                    ss = request.getParameterValues(groupsParameter+suffix);
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam(groupsParameter, ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                    
                    ss = request.getParameterValues(kindParameter+suffix);
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam(kindParameter, ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
            	
            	}
            	
            	if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX ||
        		    module.getModuleIdx() == TSServerInfo.CONDOMIN_MODULE_IDX) {
            		
            		String subdivisionsParameter = "ctl00$cphMain$SrchProperty1$lbSubdivisions";
            		
            		String[] ss = { "" };
                    
            		ss = request.getParameterValues(subdivisionsParameter);
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam(subdivisionsParameter, ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                 	
            	}
            
            } else if (sP1.equals("06731") && sP2.equals("1")) { //OH Delaware RO
            	int moduleIdx =  module.getModuleIdx();
            	if (moduleIdx == TSServerInfo.PROP_NO_IDX || moduleIdx == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
            		String propTypeLabel = "";
            		String propTypeParamID = "";
            		String propSubdivLabel = "";
            		String propSubdivParamID = "";
            		String categParam = "param_12_26";
            		String categLabel = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repGroups";
            		String subcategParam = "param_12_27";
            		String subcategLabel = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repKinds";
            		
                	String[] subdivParams = { "" };
            		
            		if (moduleIdx == TSServerInfo.PROP_NO_IDX) {
            			propTypeLabel = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$ddlPropertyType";
            			propSubdivLabel = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$lbSubdivisions";
            			propTypeParamID = "param_15_1";
            			propSubdivParamID = "param_15_3";
            			
            		} else if (moduleIdx == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
            			propTypeLabel = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$ddlPropertyType";
            			propSubdivLabel = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$lbSubdivisions";
            			propTypeParamID = "param_12_13";
            			propSubdivParamID = "param_12_15";
            			
            			String[] categListParam = { "" };
            			String[] subcategListParam = { "" };
                		
            			categListParam = request.getParameterValues(categParam);
            			if (categListParam != null && categListParam.length > 0) {
            				int iFunc;
                            for (int iter = 0; iter < categListParam.length; iter++) {
                                 iFunc = module.addFunction();
                                 module.getFunction(iFunc).setHiddenParam(categLabel + categListParam[iter], "on");
                                 module.getFunction(iFunc).setParamValue("on");
                            }
                        }
            			
            			subcategListParam = request.getParameterValues(subcategParam);
            			if (subcategListParam != null && subcategListParam.length > 0) {
            				int iFunc;
            				for (int iter = 0; iter < subcategListParam.length; iter++) {
            					iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam(subcategLabel + subcategListParam[iter], "on");
                                module.getFunction(iFunc).setParamValue("on");
                            }
                        }
            		}
            		
            		subdivParams = request.getParameterValues(propSubdivParamID);
            		if (subdivParams != null && subdivParams.length > 0) {
            			int iFunc;
        				for (int iter = 0; iter < subdivParams.length; iter++) {
        					iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam(propSubdivLabel, subdivParams[iter]);
                            module.getFunction(iFunc).setParamValue(subdivParams[iter]);
                        }
            		}
            	}

          
            } else if (sP1.equals("06755") && sP2.equals("1")) { //OH Licking RO
            	int moduleIdx =  module.getModuleIdx();
            	if (moduleIdx == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
            		String doctypeParamLabel = "DocTypes";
            		String doctypeParamID =  "param_12_14";
            		String[] doctypeParams =  request.getParameterValues(doctypeParamID);
            		if (doctypeParams != null && doctypeParams.length > 0) {
            			int iFunc;
        				for (int iter = 0; iter < doctypeParams.length; iter++) {
        					iFunc = module.addFunction();
                            module.getFunction(iFunc).setHiddenParam(doctypeParamLabel, doctypeParams[iter]);
                            module.getFunction(iFunc).setParamValue(doctypeParams[iter]);
                        }
            		}
            	}
            	
            } else if (sP1.equals("06198") && sP2.equals("0")) { //CO Fremont AO
            	int moduleIdx =  module.getModuleIdx();
            	String accTypeParam = "";
            	String[] accTypeList = { "" };
            	String accTypeLabel = "__search_select";
            	
            	if (moduleIdx == TSServerInfo.PROP_NO_IDX) {
            		accTypeParam = "param_15_0";
            	} else if (moduleIdx == TSServerInfo.PARCEL_ID_MODULE_IDX) {
            		accTypeParam = "param_2_0";
            	} else if (moduleIdx == TSServerInfo.NAME_MODULE_IDX) {
            		accTypeParam = "param_0_0";
            	} else if (moduleIdx == TSServerInfo.ADDRESS_MODULE_IDX) {
            		accTypeParam = "param_1_0";
            	} else if (moduleIdx == TSServerInfo.SUBDIVISION_MODULE_IDX) {
            		accTypeParam = "param_7_0";
            	}
            	
            	accTypeList =  request.getParameterValues(accTypeParam);
            	
            	if (accTypeList != null && accTypeList.length > 0) {
    				int iFunc;
                    for (int iter = 0; iter < accTypeList.length; iter++) {
                         iFunc = module.addFunction();
                         module.getFunction(iFunc).setHiddenParam(accTypeLabel, accTypeList[iter]);
                         module.getFunction(iFunc).setParamValue(accTypeList[iter]);
                    }
                }
            	
            } else if ( sP1.equals("06205") && sP2.equals("0") ) {	//COGunnisonAO
            	if(module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {	//Sales Search
            		String[] ss = { "" };
                    
            		ss = request.getParameterValues("param_3_12");	//Subdivision Name
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam("sub_code[]", ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                    
                    ss = request.getParameterValues("param_3_13");	//Condo Name
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam("condo_code[]", ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                        
            	}
            } else if (sP1.equals("06286") && sP2.equals("2") ) {	//IL Kendall TR
            	
            	if (module.getModuleIdx() == TSServerInfo.MODULE_IDX38) {	//Advanced Search
            	
            		String[] ss = { "" };
                    
            		ss = request.getParameterValues("param_38_10");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                if (!module.getFunction(10).getParamValue().equals(ss[iter])) {
                                	iFunc = module.addFunction();
                                    module.getFunction(iFunc).setHiddenParam("nrel", ss[iter]);
                                    module.getFunction(iFunc).setParamValue(ss[iter]);
                                }
                            }
                        }
                    }
                    
                    ss = request.getParameterValues("param_38_11");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                if (!module.getFunction(11).getParamValue().equals(ss[iter])) {
                                	iFunc = module.addFunction();
                                    module.getFunction(iFunc).setHiddenParam("owntype", ss[iter]);
                                    module.getFunction(iFunc).setParamValue(ss[iter]);
                                }
                            }
                        }
                    }
                    
                    ss = request.getParameterValues("param_38_23");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                if (!module.getFunction(23).getParamValue().equals(ss[iter])) {
                                	iFunc = module.addFunction();
                                    module.getFunction(iFunc).setHiddenParam("taxcode", ss[iter]);
                                    module.getFunction(iFunc).setParamValue(ss[iter]);
                                }
                            }
                        }
                    }
                    
                    ss = request.getParameterValues("param_38_24");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                if (!module.getFunction(24).getParamValue().equals(ss[iter])) {
                                	iFunc = module.addFunction();
                                    module.getFunction(iFunc).setHiddenParam("taxdistrict", ss[iter]);
                                    module.getFunction(iFunc).setParamValue(ss[iter]);
                                }
                            }
                        }
                    }
                    
                    ss = request.getParameterValues("param_38_26");
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                if (!module.getFunction(26).getParamValue().equals(ss[iter])) {
                                	iFunc = module.addFunction();
                                    module.getFunction(iFunc).setHiddenParam("neighborhood", ss[iter]);
                                    module.getFunction(iFunc).setParamValue(ss[iter]);
                                }
                            }
                        }
                    }
                 
            	}
            	
            } else if (sP1.equals("080") && sP2.equals("1") ) {	//IL Cook RO
            	if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX ||
            			module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
            		String[] ss = { "" };
                    
            		ss = request.getParameterValues("param_0_1");
            		if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
            			ss = request.getParameterValues("param_4_1");
            		}
                    if (ss != null) {
                    	if (ss.length > 0) {
                            int iFunc;
                            for (int iter = 0; iter < ss.length; iter++) {
                                iFunc = module.addFunction();
                                module.getFunction(iFunc).setHiddenParam("SearchFormEx1$ACSDropDownList_DocumentType", ss[iter]);
                                module.getFunction(iFunc).setParamValue(ss[iter]);
                            }
                        }
                    }
                        
            	}
            } else if ((sP1.equals("06755")||sP1.equals("06734")) && sP2.equals("7") ) {	//OH Licking CO, OH Fairfield CO
            	selectBoxes.put("param_0_7", "caseCd");
            	selectBoxes.put("param_0_8", "statCd");
            	selectBoxes.put("param_0_9", "ptyCd");
            }
            
            for (String selectBox : selectBoxes.keySet()) {
				takeCareOfMultipleSelect(request, module, selectBox, selectBoxes.get(selectBox));
			}
            
            iServerAction = TSServer.REQUEST_SEARCH_BY;
        } else {
        	//DASL parent site search simulation
        	if ( (TSConnectionURL.idDASL+"").equals(request.getParameter("ActionType") )){
        		//request.getParameterMap()
                module = intrfServer.getCurrentClassServerInfo().getModuleForSearch(iDispatcher, new SearchDataWrapper(request));
        		 iServerAction = TSServer.REQUEST_SEARCH_BY;
        	}
        	else{
	            requestParams = request.getQueryString() + requestParam;
	            iServerAction = TSServer.REQUEST_GO_TO_LINK;
        	}
        }

        logger.debug("getInMemoryDocsCount() = " + global.getInMemoryDocsCount());
        logger.debug("module = " + module);

        //
        ServerResponse Result = null;
        try {

            if (iServerAction == TSServer.REQUEST_SAVE_TO_TSD) {
                requestParams += "&parentSite=true" + parentSiteSaveType;
            }
            
	        if (iServerAction == TSServer.REQUEST_CONTINUE_TO_NEXT_SERVER) {
                requestParams += "&parentSite=true" + parentSiteSaveType;
	        }
	        
            if (iDispatcher == MULTIPLE_SAVE_TO_TSD_ACTION) {
                Vector<String> clickedDocuments = global.getClickedDocuments();
                if( clickedDocuments.size() > 0 )
                {
                    for( int i = 0 ; i < clickedDocuments.size() ; i++ )
                    {
                        String clickedDocLink = clickedDocuments.elementAt( i );
                        
                        try
                        {
                            clickedDocLink = URLDecoder.decode( clickedDocLink, "UTF-8" );
                        }catch(Exception e) {}
                        
                        Result = intrfServer.performAction(iServerAction, clickedDocLink + "&parentSite=true" + parentSiteSaveType, module, new SearchDataWrapper(request));
                    }
                }
                else
                {
                	for (int i = 0; i < docLink.length; i++) {                    
                		docLink[i] = URLDecoder.decode(docLink[i]);
                		Result = intrfServer.performAction(iServerAction, docLink[i] + "&parentSite=true" + parentSiteSaveType, module, new SearchDataWrapper(request), extraParams);
                	}
                }
            } else{
            	if(module!=null && module.getModuleIdx()==TSServerInfo.IMG_MODULE_IDX){
            		try {
            			Result = intrfServer.performAction(iServerAction, requestParams, module, new ImageLinkInPage(false) );
            		}catch(Exception e) {
            			response.getWriter().print(ERROR_MESSAGE_DOWNLOAD_IMAGE);
            		}
            	}
            	else{
            		Result = intrfServer.performAction(iServerAction, requestParams, module, new SearchDataWrapper(request), extraParams);
            	}
            }
            if(Result!=null){
	            DownloadImageResult dResult = Result.getImageResult();
	            if(dResult!=null){
					if( dResult.getStatus() == DownloadImageResult.Status.OK ){
						ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
					} else {
						//image was not downloaded
						response.getWriter().print(ERROR_MESSAGE_DOWNLOAD_IMAGE);
					}
	            }
            }
			
            if (iServerAction == TSServer.REQUEST_SAVE_TO_TSD ) {
                boolean searchNotFinished = false;
                boolean searchFinished = (new Boolean(global.getSa()
                        .getAtribute(SearchAttributes.SEARCHFINISH)))
                        .booleanValue();

                SearchAttributes sa = global.getSa();
                
                if (!(global.getSearchType() == Search.PARENT_SITE_SEARCH))
                	searchNotFinished = intrfServer.goToNextServer(global);
                if (!(global.getSearchType() == Search.PARENT_SITE_SEARCH && searchFinished)) {
                    sa.setAtribute(SearchAttributes.SEARCHFINISH, !(searchNotFinished) + "");
                }
                
                if (global.isUpdate()) {
                    try {
						DBManager.saveCurrentSearch(currentUser, global, 
						    Search.SEARCH_TSR_NOT_CREATED, null);
					} catch (SaveSearchException e1) {
						logger.error(">>>>>>>>>> NU ARE CUM SA FIE ARUNCA EXCEPTIA AICI");
						e1.printStackTrace();
					}
                }
                
                /* sometimes we do not have a RO !!!
				if (global.getSearchType() == Search.PARENT_SITE_SEARCH)
						HashCountyToIndex.setSearchServer(global, 3); // set radio button on RO
				*/
                
                //ServletServerComm.bustrap(global.getSa(), Result, intrfServer.getServerID());
                logger.debug("Before remove - getInMemoryDocsCount() = " + global.getInMemoryDocsCount());
                if (!(global.getSearchType() == Search.PARENT_SITE_SEARCH)){
                	global.removeAllInMemoryDocs();
                }
                global.removeAllVisitedDocs();
                global.removeAllRemovedInstruments();
                
                //if a PA document is saved in TSRI and a new search is performed with the same criteria then the old doc is removed from TSRI
                // and the new document found is saved in TSRI. Because of that, on saving, is displayed the message 'No documents saved' because the number 
                // of documents in docManager is the same (a document is replaced with other document)
                if (intrfServer.getDataSite().getSiteTypeInt() == GWTDataSite.PA_TYPE){
	                int diff = global.getNumberOfDocs() - initialNumberOfDocs;
	                if (iDispatcher == SAVE_TO_TSD_ACTION && diff == 0 && initialListOfPADocs.size() > 0) {
	                	Set<String> listOfPADocs = getListOfPADocs(global);
	                	if (listOfPADocs.size() > 0){
	                		for (String firstDocId : initialListOfPADocs){
	                			if (!listOfPADocs.contains(firstDocId)){
	                				initialNumberOfDocs--;
	                				break;
	    						}
							}
	                	}
	                }
                }
                //response.getWriter().print("<!--NumberOfSavedDocuments=" + (global.getNumberOfDocs() - initialNumberOfDocs) + "-->");
                
                logger.debug("After remove - getInMemoryDocsCount() = " + global.getInMemoryDocsCount());
                forward(request, response, URLMaping.TSD + (requestNumberOfSavedDocuments?"?NumberOfSavedDocuments=" + (global.getNumberOfDocs() - initialNumberOfDocs):""));
                return;
            }
            
	        if (iServerAction == TSServer.REQUEST_CONTINUE_TO_NEXT_SERVER) 
	        {
	            boolean searchNotFinished1 = intrfServer.goToNextServer(global);
	            boolean searchFinished1 = (new Boolean(global.getSa()
	                    .getAtribute(SearchAttributes.SEARCHFINISH)))
	                    .booleanValue();
	
	            SearchAttributes sa1 = global.getSa();
	            if (!(global.getSearchType() == Search.PARENT_SITE_SEARCH && searchFinished1)) {
	                sa1.setAtribute(SearchAttributes.SEARCHFINISH, !(searchNotFinished1) + "");
	            }
	            
	            if (global.isUpdate()) {
	                try {
						DBManager.saveCurrentSearch(currentUser, global, Search.SEARCH_TSR_NOT_CREATED, null);
					} catch (SaveSearchException e1) {
						logger.error(">>>>>>>>>> NU ARE CUM SA FIE ARUNCA EXCEPTIA AICI");
						e1.printStackTrace();
					}
	            }
	
	            logger.debug("Before remove - getInMemoryDocsCount() = " + global.getInMemoryDocsCount());
	            global.removeAllInMemoryDocs();
	            global.removeAllVisitedDocs();
	            global.removeAllRemovedInstruments();
	            logger.debug("After remove - getInMemoryDocsCount() = " + global.getInMemoryDocsCount());
	            
	            //response.getWriter().print("<!--NumberOfSavedDocuments=" + (global.getNumberOfDocs() - initialNumberOfDocs) + "-->");
	            
	            forward(request, response, URLMaping.TSD + (requestNumberOfSavedDocuments?"?NumberOfSavedDocuments=" + (global.getNumberOfDocs() - initialNumberOfDocs):""));
		        return;
        }
        } catch (ServerResponseException e) {
            Result = e.getServerResponse();
        } catch (Throwable error) {
			logger.error("Throwable Received in URLConnectionReader", error);
		}

        if (Result != null && !Result.getParsedResponse().isSolved()) {//isSolved = true atunci cind userul a facut click pe linkul unei imagini
        	if(module!=null){
	        	if((module.getParserID() == TSServer.ID_GET_IMAGE || module.getModuleIdx()==TSServerInfo.IMG_MODULE_IDX) && StringUtils.isEmpty(Result.getParsedResponse().getError())){
	        		return;
	        	}
        	}
        	String sHTML = ServletServerComm.getHtml(intrfServer, global, iServerAction, Result);
            request.setAttribute("serverResponse", sHTML);
            request.setAttribute("displayMode", String.valueOf(Result.getDisplayMode()));
            forward(request, response, URLMaping.PARENT_SITE_RESPONSE);
        }
    }

	/**
	 * @param global
	 */
	public Set<String> getListOfPADocs(Search global) {
		Set<String> initialListOfPADocs = new HashSet<String>();
		if (global != null){
			DocumentsManagerI manager = global.getDocManager();
			try{
	         	manager.getAccess();
	         	List<DocumentI> paDocList = manager.getDocumentsWithDataSource(false, HashCountyToIndex.getServerAbbreviationByType(GWTDataSite.PA_TYPE));
		    	if (paDocList != null && paDocList.size() > 0){
		    		for (DocumentI doc : paDocList){
		    			initialListOfPADocs.add(doc.getId());
		    		}
				}
	    	 } catch(Exception e){  
	         	e.printStackTrace(); 
	         } finally{
	         	manager.releaseAccess();
	         }
		}
		
		return initialListOfPADocs;
	}

	public void takeCareOfMultipleSelect(HttpServletRequest request,
			TSServerInfoModule module, String selectBoxInPS, String selectBoxOnSite) {
		
		Pattern pattern = Pattern.compile("param_(\\d+)_(\\d+)");
		Matcher matcher = pattern.matcher(selectBoxInPS);
		
		if(matcher.matches()) {
			int moduleId = Integer.parseInt(matcher.group(1));
			int fnctId = Integer.parseInt(matcher.group(2));
			
			if(moduleId != module.getModuleIdx()) {
				return;
			}
			
			String[] ss = request.getParameterValues(selectBoxInPS);
			if (ss != null && ss.length > 0) {
				int iFunc;
				for (int iter = 0; iter < ss.length; iter++) {
					if(!module.getFunction(fnctId).getParamValue().equals(ss[iter])) {
						iFunc = module.addFunction();
						TSServerInfoFunction newFunction = new TSServerInfoFunction(module.getFunction(fnctId));
						newFunction.setParamValue(ss[iter]);
						module.setFunction(iFunc, newFunction);
					}
				}
				
			}
			
		}
		
		
		
	}
}
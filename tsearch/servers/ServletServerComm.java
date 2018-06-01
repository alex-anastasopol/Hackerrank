package ro.cst.tsearch.servers;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.propertyInformation.InstrumentConstants;
import ro.cst.tsearch.search.address.AddressStringUtils;
import ro.cst.tsearch.search.address.Normalize;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.filter.newfilters.name.ExactNameFilter;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PartyNameSet;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.FLHernandoTR;
import ro.cst.tsearch.servers.types.OHCuyahogaRO;
import ro.cst.tsearch.servers.types.TNWilliamsonYB;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servers.types.XXStewartPriorPF;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.FormatSa;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;


public class ServletServerComm implements Serializable {

    static final long serialVersionUID = 10000000;

    private static final Category logger = Logger.getLogger(ServletServerComm.class);

    private static final Category loggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + ServletServerComm.class.getName());

    private static final String FOUND_RESULTS_1 = "<b>The search returned ";

    private static final String FOUND_RESULTS_2 = " results per page</b>";

    private static final String INTERNAL_ERR_MSG = "<b>The following error had occurred:</b>";

    private static final String WARNING_MSG = "<b>Warning!</b>";

    private static final String SITE_IS_UPDATING_ERR_MSG = "<b>ATS could not access the Parent Site</b>";

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static int MAX_CROSS_REFS_SEARCH = 10;

    public static String getHtml(TSInterface serverInterf, Search search,
            int viServerAction, ServerResponse Response) {

    	serverInterf.getClass();
        String onClickAction0 = "javascript:window.top.location='"
                + AppLinks.getBackToSearchPageHref(search.getSearchID()) + "'";
        
        String link = AppLinks.getParentSiteNoSaveHref(search.getSearchID());
        String onClickAction10 = "window.location.href='" + link + "'";
        String onClickAction11 = "history.go(-1)";
        
        if (Response.getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST)
            Response.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING);
        
        String onClickActionB = (Response.getParsedResponse().isMultiple() || Response
                .getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING)
                && (search.getSearchType() != Search.PARENT_SITE_SEARCH) ? onClickAction0
                : onClickAction11;
       
        
        String onClickActionBTP = (Response.getParsedResponse().isMultiple() || Response
                .getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING)
                && (search.getSearchType() != Search.PARENT_SITE_SEARCH) ? onClickAction0
                : onClickAction10;
        String sBackLink="";
        
        String p1 = (serverInterf.isParentSite())?search.getP1ParentSite():search.getP1();
        String p2 = (serverInterf.isParentSite())?search.getP2ParentSite():search.getP2();
        
    	int miServerID = serverInterf.getServerID();
        if( miServerID == 321702){ //FLCollierTR  //B 3806
        	String rsResponse = Response.getResult();
	        if (!StringUtils.isEmpty(rsResponse) && rsResponse.contains("repeat.php")) {
	        		String backToListFLCollierTR ="";
	        		String backFLCollierTR = rsResponse.replaceAll("(?is).*Back\\s*To\\s*List\\\"\\s*onclick\\s*=\\s*\\\"\\s*location\\s*=\\s*'\\s*([^']+).*", "$1");
		        	backToListFLCollierTR = "window.location.href='/title-search/URLConnectionReader?"+URLConnectionReader.PRM_NAME_P1 + "="+ p1 + "&" + 
		        	URLConnectionReader.PRM_NAME_P2 + "=" +  p2 + "&searchId=" + search.getSearchID() + "&ActionType=2&Link=search/"+ backFLCollierTR + "'";
		        	onClickActionB = backToListFLCollierTR;
	        }
        }
        
        /*Back button is disabled and has a tool tip for FLHernandoTR, TNWilliamsonYB and OHCuyahogaRO*/
        if(serverInterf instanceof FLHernandoTR ||  serverInterf instanceof OHCuyahogaRO) {
        	sBackLink = "<br><br>"
                    + "<input type=\"button\" name=\"Button\" value=\"Back\" disabled=\"disabled\" title=\"Disabled for " + 
        			serverInterf.getSearch().getStateCounty() + serverInterf.getDataSite().getSiteTypeAbrev() + "\" class=\"button\">";
        } else if (serverInterf instanceof XXStewartPriorPF && Response.getResult().indexOf("action=\"FileDetails.aspx") != -1) {
        	 /*nu mai apare link de Back in pag de detalii pt site-ul PF*/
        	sBackLink = "<br><br>";

        } else {
        	sBackLink = "<br><br>"
                + "<input type=\"button\" name=\"Button\" value=\"Back\" onClick=\""
                + onClickActionB + "\" class=\"button\">";
        }
        
        String onClickActionSTData = "";
        
        String sContinueForm = serverInterf.getContinueForm(p1, p2, search.getID());
        	
        String sBackLinkBTP = ("<br><br>".equals(sBackLink)) ? "" : "&nbsp;&nbsp;&nbsp;&nbsp;";
        sBackLink +=  "<input type=\"button\" name=\"Button\" value=\"Back  to Parent Site\" onClick=\""
            + onClickActionBTP + "\" class=\"button\">";

        CurrentInstance curInst = InstanceManager.getManager().getCurrentInstance(search.getID());
        
        long x = search.getID();
        
        UserAttributes ua = curInst.getCurrentUser();
        boolean isTSAdmin = true; 
        
        try{
        
        	isTSAdmin = UserUtils.isTSAdmin(ua);
        
        }
        catch(Exception e){
        	
        }
        
        
        //Saves test data
        //String SaveTestData = "&nbsp;&nbsp;&nbsp;&nbsp;" + "<input type=\"button\" name=\"Button\" value=\"Save test Data\" onClick=\"" + onClickActionSTData + "\" class=\"button\">"
        
//        String SaveTestData = onClickActionSTData + "<FORM action=\"" + URLMaping.path + "/presenceTestServlet\" method=\"post\">  <INPUT type=\"submit\" class=\"button\" value=\"Save test case\">  <INPUT TYPE=\"hidden\"  NAME= \"searchID\"  value="+x+" >    </FORM>";              
        
        
        
        if ( search.getSearchType() == Search.PARENT_SITE_SEARCH ) { 
        	
        	if ( Response.getDisplayMode() != ServerResponse.HIDE_BACK_TO_PARENT_SITE_BUTTON )
        	{
        		sBackLink += sBackLinkBTP;
        		
//        		if( isTSAdmin == true  )
//        			sBackLink += SaveTestData;
        		
        	}
        	else
        		sBackLink = "<br><br>"
                    + "<input type=\"button\" name=\"Button\" value=\"Close window\" onClick=\"window.close();\" class=\"button\">";
        }
        
        if (Response.getDisplayMode()==ServerResponse.HIDE_ALL_CONTROLS) {
        	sBackLink = "";
        	sContinueForm = "";
        }

        StringBuilder sHTML = new StringBuilder();
        //error from the parent server
        if (Response.isError()) {
            if (Response.getErrorCode() == ServerResponse.ZERO_MODULE_ITERATIONS_ERROR)
                sHTML.append(INTERNAL_ERR_MSG + "<br>" + "<font color=\"red\">" + Response.getError() + "</font>"                 
                        + "<hr>" + sBackLink);
            else if (Response.getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING) 
            {           	
                ParsedResponse pr = Response.getParsedResponse();
            	String headerAndRows = pr.getHeader() + StringUtils.join(pr.getResultRowsAsStrings(), "");
            	
            	if( headerAndRows.trim().endsWith( "</td>" ) ){
            		headerAndRows += "</tr></table>";
            	}
            	
            	sHTML.append("<b><font color=blue>" + Response.getError()
                        + "</font></b>" + "<hr>" + headerAndRows
                        + pr.getFooter() + "<hr>" + sBackLink + "&nbsp;")
                        .append(sContinueForm);
                String saveSearchParametersButton = serverInterf.getSaveSearchParametersButton(Response);
            	if(saveSearchParametersButton != null && serverInterf.isParentSite()) {
            		sHTML.append(saveSearchParametersButton); 
            	}
            } else {
            	sHTML.append(SITE_IS_UPDATING_ERR_MSG)
                		.append(" on ").append(serverInterf.getDataSite().getName()).append(" [")
                		.append(search.getAbstractorFileNo()).append("][").append(search.getID()).append("]")
                        .append("<br><br>The error message is:\n\n </br>").append(Response.getParsedResponse().getError()).append("\n\n") 
                        .append((Response.getResult() != null ? "<BR>" + Response.getResult() : "<BR>No Server response available"))
                        .append(sBackLink);
                String saveSearchParametersButton = serverInterf.getSaveSearchParametersButton(Response);
            	if(saveSearchParametersButton != null && serverInterf.isParentSite()) {
            		sHTML.append(saveSearchParametersButton); 
            	}
            }
            
        }
        //parsing error
        else if (Response.getParsedResponse().isError()){
        	Object show_error = Response.getParsedResponse().getAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE);
        	if(show_error!=null && "true".equals(show_error.toString()))
        		sHTML.delete(0, sHTML.length()); 
        	else
        		sHTML.append(INTERNAL_ERR_MSG);
        	sHTML.append("<br>" + "<font color=\"red\">" + Response.getParsedResponse().getError() + "</font>" + sBackLink);
            String saveSearchParametersButton = serverInterf.getSaveSearchParametersButton(Response);
        	if(saveSearchParametersButton != null && serverInterf.isParentSite()) {
        		sHTML.append(saveSearchParametersButton); 
        	}
        } else if (Response.getParsedResponse().isWarning()) {
            sHTML.append(WARNING_MSG + "<br>" + "<font color=\"red\">" + Response.getParsedResponse().getWarning() + "</font>" + sBackLink);
            String saveSearchParametersButton = serverInterf.getSaveSearchParametersButton(Response);
        	if(saveSearchParametersButton != null && serverInterf.isParentSite()) {
        		sHTML.append(saveSearchParametersButton); 
        	}
        } else if (Response.getParsedResponse().isNone()) {
        	
        	sHTML.append("<b>" + TSServer.NO_DATA_FOUND + "</b>" + "<br>" + sBackLink);
        	String saveSearchParametersButton = serverInterf.getSaveSearchParametersButton(Response);
        	if(saveSearchParametersButton != null && serverInterf.isParentSite()) {
        		sHTML.append(saveSearchParametersButton); 
        	}
        }
        //there is a parsed response
        else {
            ParsedResponse pr = Response.getParsedResponse();
            Vector rows = pr.getResultRowsAsStrings();
            if ((rows == null) || (rows.size() == 0)) {
                //pt combatibilitate cu versiunea veche, parserii de rows nu
                // sint implementati pt alte servere decit Shelby
                sHTML.append(pr.getResponse());
            } else {
            	
            	try {/*State.getState(new BigDecimal(search.getSearchState())).getName()*/
            		sHTML.append("<b>Searching on " + 
            				State.getState(new BigDecimal(search.getSa().getAtribute(SearchAttributes.P_STATE))).getName() + ", "
            				+ County.getCounty(new BigDecimal(search.getSa().getAtribute(SearchAttributes.P_COUNTY))).getName() + ":</b> " + 
            				Search.getServerTypeFromCityChecked(serverInterf.isParentSite()?search.getCityCheckedParentSite():search.getCitychecked()) + ".<br>");
            	} catch (Exception e) {}
            	
            	int size = 0;
            	for (Object object : pr.getResultRows()) {
					if (object instanceof ParsedResponse) {
						ParsedResponse parsedResponse = (ParsedResponse) object;
						Boolean possibleNavigationRow = (Boolean)parsedResponse.getAttribute(ParsedResponse.SERVER_NAVIGATION_LINK);
						if(!Boolean.TRUE.equals(possibleNavigationRow)) {
							size ++;
						} 
					} else {
						size ++;
					}
				}
            	
                sHTML.append(FOUND_RESULTS_1 + size + FOUND_RESULTS_2 + 
                		(ua.isTSAdmin()?" (parsed in about " + ((System.currentTimeMillis() - Response.getCreationTime())/1000) + " seconds)":"") +
                		":<BR><BR>");
                
                sHTML.append(pr.getHeader() + StringUtils.join(rows, "") + pr.getFooter());
            }
            
            int indexOfEndBody = sHTML.toString().toLowerCase().indexOf("</body");
            
            if ( indexOfEndBody != -1 ) {
                //insert back link
                sHTML.delete(indexOfEndBody, sHTML.length());
            }

            sHTML.append(sBackLink);
            sHTML.append(sContinueForm);
            
            String saveSearchParametersButton = serverInterf.getSaveSearchParametersButton(Response);
        	if(saveSearchParametersButton != null && serverInterf.isParentSite()) {
        		sHTML.append(saveSearchParametersButton); 
        	}
            
        }

        return sHTML.toString();
    }

    public static void bootstrap(SearchAttributes sa, ServerResponse result, int serverID) {
    	
    	CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(sa.getSearchId());
    	int commId = currentInstance.getCommunityId();
    	DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(String.valueOf(serverID) );
    	boolean nameBootstrapEnabled =  dataSite.isEnabledNameBootstrapping(commId);
    	boolean addressBootstrapEnabled =  dataSite.isEnabledAddressBootstrapping(commId);
    	boolean legalBootstrapEnabled =  dataSite.isEnabledLegalBootstrapping(commId);
    	String serverName = dataSite.getName();
    	
    	DocumentI document = result.getParsedResponse().getDocument();
    	
    	if (serverName != null) {    		
    		 if (TSServersFactory.isRegister(serverID)
    	                || TSServersFactory.isDailyNews(serverID)
    	                ||TSServer.isRoLike(serverID,true)) {
    	            boostrapROBookPageCrossRef(result,sa);
    		 }  
    		
    		// bug #531: save subdivision name retrieved from MOJacksonRO docs
	    	if("MOJacksonRO".equals(serverName)){
	    		Vector v = result.getParsedResponse().getPropertyIdentificationSet();
	    		for(Iterator it=v.iterator(); it.hasNext();){
	    			PropertyIdentificationSet pis = (PropertyIdentificationSet)it.next();
	    			String subdName = pis.getAtribute("SubdivisionName");
	    			String previousValue = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME_MOJACKSONRO);
	    			// if not empty subdivision
	    			if(!"".equals(subdName)){
	    				// if previous value empty, overwrite it
	    				if("".equals(previousValue)){
	    					sa.setAtribute(SearchAttributes.LD_SUBDIV_NAME_MOJACKSONRO, subdName);
	    				} else{
	    					// if previous value does not contain an ' and current does, overwrite it
                            if( previousValue != null ){ //null pointer exception bugfix
    	    					if(!previousValue.contains("'") && subdName.contains("'")){
    	    						sa.setAtribute(SearchAttributes.LD_SUBDIV_NAME_MOJACKSONRO, subdName);
    	    					}
                            }
	    				}
	    			}    			
	    		}
	    	}
	    	
	    	// check if we are allowed to bootstrap
	    	if (!nameBootstrapEnabled && !addressBootstrapEnabled && !legalBootstrapEnabled) {
	    		return;
	    	}
    	}
    	
        if (legalBootstrapEnabled) {
        	if (TSServersFactory.isRegister(serverID)
                    || TSServersFactory.isDailyNews(serverID)) {
                if (result.getParsedResponse().getSaleDataSetsCount() != 0) {

                    Vector t = getSaleDataSetsOrdedByDate(result
                            .getParsedResponse().getSaleDataSet());
                    SaleDataSet sds = (SaleDataSet) t.get(t.size() - 1);

                    String docType = sds.getAtribute("DocumentType").trim();

                    //bootstrap plat book page if any
                    Vector allPis = ( Vector ) result.getParsedResponse().infVectorSets.get( "PropertyIdentificationSet" );
                    if( allPis != null ){

                    	//do not bootstrap list if we are on Wayne and found more than 10 PIS
                    	boolean bootstrap = true;
                    	if( "MIWayneRO".equals(serverName) ){
                    		if( allPis.size() > 10 ){
                    			bootstrap = false;
                    		}
                    	}
                    	
                    	int countPlats = 0;
                    	try{
    	                	for(int pisIdx = 0 ; pisIdx < allPis.size() ; pisIdx++ ){
    	                		PropertyIdentificationSet pisPlat = (PropertyIdentificationSet) allPis.elementAt( pisIdx );
    	                		if(!StringUtils.isEmpty(pisPlat.getAtribute( "PlatBook" ))){
    	                			countPlats ++;
    	                		}
    	                	}
                    	}catch(Exception e){}
                    	
                    	if( bootstrap && countPlats <= MAX_CROSS_REFS_SEARCH){
    	                	for( int pisIdx = 0 ; pisIdx < allPis.size() ; pisIdx++ ){
    	                		PropertyIdentificationSet pisPlat = (PropertyIdentificationSet) allPis.elementAt( pisIdx );
    			                if( pisPlat != null ){
    			                	String platBook = pisPlat.getAtribute( "PlatBook" );
    			                	String platPage = pisPlat.getAtribute( "PlatNo" );
    			
    			                	//logger.debug("Bootstraping plat book " + platBook + " page " + platPage );
    			                	//System.err.println("Bootstraping plat book " + platBook + " page " + platPage);
    			                	
    			                	if( !"".equals( platBook ) && !"".equals( platPage ) ){
    			                        FormatSa.addBookAndPage( sa, platBook, platPage, false, false);
    			                	}
    			                }
    	                	}
                    	}
                    }
                    
                    
                    if (TSServersFactory.isDailyNews(serverID)
                    /*
                     * && RegisterDocsValidator.isOfDocType( docType, new int[] {
                     * DocumentTypes.TRANSFER_TYPE, DocumentTypes.LIEN_TYPE,
                     * DocumentTypes.MORTGAGE_TYPE, DocumentTypes.ASSIGNMENT_TYPE,
                     * DocumentTypes.COURT, DocumentTypes.MISCELANOUS_TYPE,
                     * DocumentTypes.CORPORATION, DocumentTypes.RELEASE_TYPE, })
                     */// toate doc DN se verifica si pe RO
                    ) {
                        if (logger.isDebugEnabled())
                            logger.debug("Collected InstrumentNumber from DN =["
                                    + sds.getAtribute("InstrumentNumber") + "]");
                        Instrument crtInst = fillInstrumentObj(result
                                .getParsedResponse());
                        crtInst.setOverwrite(true);
                        if(!((List) sa.getObjectAtribute(SearchAttributes.INSTR_LIST)).contains(crtInst))
                        	((List) sa.getObjectAtribute(SearchAttributes.INSTR_LIST)).add(crtInst);
                    }
                    if ("PLAT".equalsIgnoreCase(docType)) {
                    	if(!serverName.startsWith("TX")){
                    		boostrapBookPage(sds, sa);
                    	}
                    }

                }
                
                // cautare dupa crossreferinte

                if (serverID == TSServersFactory.getSiteId("TN", "Rutherford", "RO")
                        || serverID == TSServersFactory.getSiteId("TN", "Montgomery", "RO")) {
                    boostrapBookPageRef(sa, result);
                }

              if (serverID == TSServersFactory.getSiteId("TN", "Knox", "RO")) 
               {
                  boostrapInstrumentListFromCrossRefSets(sa, result);
               }
            }
        	
        }
        
        Search s = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext();
        if (!TSServersFactory.isAssesor(serverID)
                && !TSServersFactory.isCountyTax(serverID)
                && !TSServersFactory.isCountyTUTax(serverID)
                && !TSServersFactory.isCityTax(serverID)
                && !TSServersFactory.isPatriots(serverID)
                && result.getParsedResponse().getAttribute("DASL_TAX_DOCUMENT")==null// adica if (RO sau
                                                          // DN) ???
        ) {
            if (logger.isDebugEnabled())
                logger.debug("This server does not support bootstrap.");
			IndividualLogger.info( "This server does not support bootstrap." ,sa.getSearchId());
            return;
        }

        
        if ( TSServersFactory.isAssesor(serverID) || TSServersFactory.isCountyTax(serverID) || TSServersFactory.isCountyTUTax(serverID) ) {
        	PropertyAppraisalSet pas = result.getParsedResponse().getPropertyAppraisalSet();
	 		addFromIS(sa, pas, SearchAttributes.ASSESSED_VALUE, "TotalAssessment");
	 	}
        
        if (logger.isDebugEnabled())
			logger.debug("Start bootstraping =>Original SearchAttributes =[" + sa + "]");
		IndividualLogger.info("Start bootstraping =>Original SearchAttributes =[" + sa + "]",sa.getSearchId());
		SearchLogger.info("<br>Bootstrapping started: " + sa.display() + "<br>",sa.getSearchId());

		if (result.getParsedResponse().getPropertyIdentificationSetCount() != 0) 
		{
			
		PropertyIdentificationSet pis = result.getParsedResponse().getPropertyIdentificationSet(0);
         Vector<PartyNameSet> owners=(Vector<PartyNameSet>) result.getParsedResponse().infVectorSets.get("PartyNameSet");
			//  InfSet is=result.getParsedResponse().getv
			if (addressBootstrapEnabled) {
				if (serverID == TSServersFactory.getSiteId("TN", "Davidson", "AO")) {
					manageAddressDavidson(pis);
				}
			}
         	
			boolean addrBoostraped = bootstrapAddress(serverID, sa, pis, dataSite, addressBootstrapEnabled);

			if (addrBoostraped) 
			{
				if (legalBootstrapEnabled) {
					
					if (s.searchCycle == 0) {				
						
						bootstrapPidInternal(sa, pis);
						addFromIS(sa, pis, SearchAttributes.LD_GEO_NUMBER, "GeoNumber");
						addFromIS(sa, pis, SearchAttributes.LD_PARCELNO2, "ParcelID2");//used RedVision automatic
						addFromIS(sa, pis, SearchAttributes.LD_PARCELNONDB, "ParcelIDNDB");
						addFromIS(sa, pis, SearchAttributes.LD_PARCELNO2_ALTERNATE, "ParcelID2_ALTERNATE");//used RedVision automatic 
						addFromIS(sa, pis, SearchAttributes.LD_PARCELNO3, "ParcelID3");//used Data Trace automatic  
						////addFromIS(sa, pis, SearchAttributes.LD_QUARTER_VAL, "QuarterValue");//used Data Trace automatic  
						addFromIS(sa, pis, SearchAttributes.LD_PARCELNO_PREFIX, "ParcelIDGroup"); // used by KYJeffersonTR automatic
	                    addFromIS(sa, pis, SearchAttributes.LD_ACRES, "Acres");
	                    addFromIS(sa, pis, SearchAttributes.LD_NCB_NO, "NcbNo");
	                    addFromIS(sa, pis, SearchAttributes.LD_ABS_NO, "AbsNo");
	                    addFromIS(sa, pis, SearchAttributes.LD_DISTRICT, "District");

						if(serverID == TSServersFactory.getSiteId("FL", "Hillsborough", "TR") ){
							FormatSa.addBookAndPage(sa, pis.getAtribute("PlatBook").trim(), pis.getAtribute("PlatNo").trim(), "FL".equals(dataSite.getStateAbbreviation()), true);
							String subdivision = sa.getAtribute(SearchAttributes.LD_SUBDIVISION);
							if(StringUtils.isEmpty(subdivision)){
								addFromIS(sa, pis, SearchAttributes.LD_SUBDIVISION, "Subdivision");
							}
							
							String subdivname = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
							if(StringUtils.isEmpty(subdivname)){
								addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");
							}
							String newValue = pis.getAtribute("SubdivisionName").trim();
							if(newValue !=null && subdivname!=null && newValue.toUpperCase().contains(subdivname.toUpperCase()) ){
									addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");
							}
							String subdSec = sa.getAtribute(SearchAttributes.LD_SECTION);
							if(StringUtils.isEmpty(subdSec)){
								addFromIS(sa, pis, SearchAttributes.LD_SECTION, PropertyIdentificationSetKey.SECTION.getShortKeyName());
							}
							String lotNo = sa.getAtribute(SearchAttributes.LD_LOTNO);
							if(StringUtils.isEmpty(lotNo)){
								addFromIS(sa, pis, SearchAttributes.LD_LOTNO, "SubdivisionLotNumber");
							}
							String subLotNo = sa.getAtribute(SearchAttributes.LD_SUBLOT);
							if(StringUtils.isEmpty(subLotNo)){
								addFromIS(sa, pis, SearchAttributes.LD_SUBLOT, "SubLot");
							}					
							
							String block = sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
							if(StringUtils.isEmpty(block)){
								addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_BLOCK, "SubdivisionBlock");
							}
							
							String sec = sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC);
							if(StringUtils.isEmpty(sec)){
								 addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_SEC, "SubdivisionSection");
							}
							
							String townShip = sa.getAtribute(SearchAttributes.LD_SUBDIV_TWN);
							if(StringUtils.isEmpty(townShip)){
								addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TWN, "SubdivisionTownship");
							}
							
							String range = sa.getAtribute(SearchAttributes.LD_SUBDIV_RNG);
							if(StringUtils.isEmpty(range)){
								 addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_RNG, "SubdivisionRange");
							}
							
							String unit = sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
							if(StringUtils.isEmpty(unit)){
								addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_UNIT, "SubdivisionUnit");
							}
							
							String phase = sa.getAtribute(SearchAttributes.LD_SUBDIV_PHASE);
							if(StringUtils.isEmpty(phase)){
								addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_PHASE, "SubdivisionPhase");
							}
							
						}
						else if( serverID == TSServersFactory.getSiteId("KY", "Jefferson", "TR") ) {
							FormatSa.addBookAndPage(sa, pis.getAtribute("PlatBook").trim(), pis.getAtribute("PlatNo").trim(), "FL".equals(dataSite.getStateAbbreviation()), true);
							// if we are on KYJeffersonTR do not bootstrap legal
							// info, as it was taken from the last transfer on RO,or
							// from AO
						} else if( CountyConstants.AK_Anchorage_Borough_STRING.equals(sa.getCountyId()) && 
								"true".equalsIgnoreCase(sa.getAtribute(SearchAttributes.IS_CONDO))  
								) {
							bootstrapLegalInfo(s, dataSite, result.getParsedResponse().getDocument(), false);
							
						} else {
							
							if(CountyConstants.AK_Anchorage_Borough_STRING.equals(sa.getCountyId()) && dataSite.getType() == GWTDataSite.AO_TYPE) {
								//ignore - causes: 6623
							} else {
								FormatSa.addBookAndPage(sa, pis.getAtribute("PlatBook").trim(), pis.getAtribute("PlatNo").trim(), "FL".equals(dataSite.getStateAbbreviation()), true);
							}
							
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIVISION, "Subdivision");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");
	                        addFromIS(sa, pis, SearchAttributes.LD_SECTION, PropertyIdentificationSetKey.SECTION.getShortKeyName());
	                        addFromIS(sa, pis, SearchAttributes.LD_LOTNO, "SubdivisionLotNumber");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBLOT, "SubLot");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_BLOCK, "SubdivisionBlock");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_SEC, "SubdivisionSection");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TWN, "SubdivisionTownship");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_RNG, "SubdivisionRange");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_UNIT, "SubdivisionUnit");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_PHASE, "SubdivisionPhase");
	                    }
	                        
	                    addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_CODE, "SubdivisionCode");
						addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TRACT, "SubdivisionTract");
	                    addFromIS(sa, pis, SearchAttributes.BUILDING, "SubdivisionBldg");
	                    
						addFromIS(sa, pis, SearchAttributes.QUARTER_ORDER, PropertyIdentificationSetKey.QUARTER_ORDER.getShortKeyName());
						addFromIS(sa, pis, SearchAttributes.QUARTER_VALUE, PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName());
						addFromIS(sa, pis, SearchAttributes.ARB, PropertyIdentificationSetKey.ARB.getShortKeyName());

						if (serverID == TSServersFactory.getSiteId("MO", "Jackson", "TR") || 
								serverID == TSServersFactory.getSiteId("KS", "Johnson", "TR") || 
								serverID == TSServersFactory.getSiteId("TN", "Hamilton", "TR")) {
							
							bootstrapPidInternal(sa, pis);
							
		                    addFromIS(sa, pis, SearchAttributes.LD_SUBDIVISION, "Subdivision");
			                addFromIS(sa, pis, SearchAttributes.LD_LOTNO, "SubdivisionLotNumber");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBLOT, "SubLot");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");
			                addFromIS(sa, pis, SearchAttributes.LD_SECTION, PropertyIdentificationSetKey.SECTION.getShortKeyName());
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_SEC, "SubdivisionSection");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TWN, "SubdivisionTownship");
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_RNG, "SubdivisionRange");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_CODE, "SubdivisionCode");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_PHASE, "SubdivisionPhase");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_UNIT, "SubdivisionUnit");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_BLOCK, "SubdivisionBlock");
			                addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TRACT, "SubdivisionTract");
			            }
						
						if (serverID == TSServersFactory.getSiteId("TN", "Shelby", "AO") || serverID == TSServersFactory.getSiteId("TN", "Davidson", "AO")
								|| serverID == TSServersFactory.getSiteId("TN", "Williamson", "AO")) {
							checkForCondominiums(sa, pis);
						}
	                        
		            } 
					else // completez doar la urmatoarele cicluri de search
					{
		                if ("".equals(sa.getAtribute("LD_PARCELNO")))
		                	boostrapParcelID(serverID, pis, sa);
		            	    
	            	    if ("".equals(sa.getAtribute("LD_SUBDIVISION")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIVISION, "Subdivision");	                    
	                    if ("".equals(sa.getAtribute("LD_LOTNO")))
	                        addFromIS(sa, pis, SearchAttributes.LD_LOTNO, "SubdivisionLotNumber");
	                    if ("".equals(sa.getAtribute(SearchAttributes.LD_SUBLOT)))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBLOT, "SubLot");
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_NAME")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");
	                    if ("".equals(sa.getAtribute("LD_SECTION")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SECTION, PropertyIdentificationSetKey.SECTION.getShortKeyName());
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_SEC")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_SEC, "SubdivisionSection");
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_TWN")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TWN, "SubdivisionTownship");
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_RNG")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_RNG, "SubdivisionRange");
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_CODE")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_CODE, "SubdivisionCode");
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_PHASE")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_PHASE, "SubdivisionPhase");
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_UNIT")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_UNIT, "SubdivisionUnit");	                    	                   
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_BLOCK")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_BLOCK, "SubdivisionBlock");
	                    if ("".equals(sa.getAtribute("LD_SUBDIV_TRACT")))
	                        addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TRACT, "SubdivisionTract");
	                    if ("".equals(sa.getAtribute("BUILDING")))
	                        addFromIS(sa, pis, SearchAttributes.BUILDING, "SubdivisionBldg");
	                    if ("".equals(sa.getAtribute(SearchAttributes.LD_DISTRICT)))
	                        addFromIS(sa, pis, SearchAttributes.LD_DISTRICT, PropertyIdentificationSetKey.DISTRICT.getShortKeyName());
		            }
				}
				
				if (nameBootstrapEnabled) {
					bustrapOwnerName(sa, pis, serverID, owners, result);
				}
				
			}
		} else{
			if (document != null){
				bootstrapPIDInfoFromDocument(s, dataSite, document, serverID, true);
				if (addressBootstrapEnabled){
					bootstrapAddressInfoFromDocument(s, dataSite, document, serverID, true);
				}
				if (legalBootstrapEnabled){
					bootstrapLegalInfo(s, dataSite, document, true);
					//bootstrapReferencesFromDocument(s, dataSite, document, serverID, true);
				}
				if (nameBootstrapEnabled){
					bootstrapOwnerNameInfoFromDocument(s, dataSite, document, serverID, true);
				}
			}
		}

		if (legalBootstrapEnabled) {
			
			if (result.getParsedResponse().getSaleDataSetsCount() != 0) {

				Vector t = getSaleDataSetsOrdedByDate(result.getParsedResponse().getSaleDataSet());
				SaleDataSet sds = (SaleDataSet) t.get(t.size() - 1);
				addFromIS(sa, sds, SearchAttributes.LD_INSTRNO, "InstrumentNumber");
			}
			
			// bootstrap the plat number
			if (result.getParsedResponse().getPropertyIdentificationSetCount() != 0){
				PropertyIdentificationSet pis = result.getParsedResponse().getPropertyIdentificationSet(0);
				addFromIS(sa, pis, SearchAttributes.LD_BOOKPAGE, "PlatInstr");
			}

			
		        List instrList = (List) sa
		                .getObjectAtribute(SearchAttributes.INSTR_LIST);
		        for (Iterator iter = result.getParsedResponse().getSaleDataSet()
		                .iterator(); iter.hasNext();) {
		            SaleDataSet sds = (SaleDataSet) iter.next();
		            Instrument instr = fillInstrumentFromSds(sds, new Vector(), new Vector());
		            // bootstrap rule for Baldwin County
		            if (serverID == TSServersFactory.getSiteId("AL", "Baldwin", "AO")) {
		                String bookNo = sds.getAtribute("Book");
		                String pageNo = sds.getAtribute("Page");
		                if (bookNo.matches("0+")) {
		                    //special case => search by instrument no => book = 0000 ,
		                    // page = xxx = instrument no
		                    instr.setInstrumentNo(pageNo);
		                    instr.setBookNo("");
		                    instr.setPageNo("");
		                } else {
		                    // search by book and page , no instrument no
		                }
		            }
		            
		            if (serverID == TSServersFactory.getSiteId("TN", "Wilson", "AO")) {
			            //Bug 2128
		                String b = sds.getAtribute("Book");
		                String p = sds.getAtribute("Page");
		                b = b.replaceAll("^0+", "");
		                p = p.replaceAll("^0+", "");
		                instr.setBookNo(b);
		                instr.setPageNo(p);
		                instr.setRealdoctype("TRANSFER");
		            }
		            
		            if (serverID == TSServersFactory.getSiteId("TN", "Davidson", "AO")) {
		                String b = sds.getAtribute("Book");
		                String p = sds.getAtribute("Page");
		                b = b.replaceAll("^0+", "");
		                p = p.replaceAll("^0+", "");
		                instr.setBookNo(b);
		                instr.setPageNo(p);
		            }
		
		            if (serverID == TSServersFactory.getSiteId("TN", "Montgomery", "AO") || 
	                        serverID == TSServersFactory.getSiteId("TN", "Montgomery", "TR")) 
	                {
		                instr.setBookNo(sds.getAtribute("Book").replaceAll("^V", ""));
		            }
		
		            if (serverID == TSServersFactory.getSiteId("TN", "Rutherford", "AO")) {
		                instr.setBookNo(sds.getAtribute("Book").replaceAll("^WB", ""));
		            }
		            if(!instrList.contains(instr))
		            	instrList.add(instr);
		        }
		
		        if (logger.isDebugEnabled())
		            logger.debug("Current InstrumentList =[" + instrList + "]");
		        IndividualLogger.info( "Current InstrumentList =[" + instrList + "]" ,sa.getSearchId());
		}
		
        if (logger.isDebugEnabled())
            logger.debug("Boostraping finished with SearchAttributes =[" + sa
                    + "]");
        IndividualLogger.info( "Boostraping finished with SearchAttributes =[" + sa + "]",sa.getSearchId() );
        
        SearchLogger.info("Bootstrapping finished: " + sa.display() + "<br>",sa.getSearchId());
        
    }
    
    /**
     * Designed to bootstrap information from document to the Legal Description Area in Search Page
     * @param search
     * @param dat 
     * @param document
     * @param overrideIfAvailable if true forces the new value even if something is already available
     * @return
     */
    private static boolean bootstrapLegalInfo(Search search, DataSite dat, DocumentI document, boolean overrideIfAvailable) {
    	SearchAttributes sa = search.getSa();
    	
    	if(document == null) {
    		return false;	//no source
    	}
    	Set<PropertyI> properties = document.getProperties();
    	if(properties == null || properties.isEmpty()) {
    		return false;	//nothing to bootstrap
    	}
    	PropertyI mainProperty = properties.iterator().next();
    	
    	if(mainProperty == null) {
    		return false;
    	}
    	
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIVISION))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIVISION, mainProperty.getLegal().getSubdivision().getName());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIV_NAME, mainProperty.getLegal().getSubdivision().getName());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SECTION))){
			setNewSaValue(sa, SearchAttributes.LD_SECTION, mainProperty.getLegal().getSubdivision().getSection());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_LOTNO))){
			setNewSaValue(sa, SearchAttributes.LD_LOTNO, mainProperty.getLegal().getSubdivision().getLot());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBLOT))){
			setNewSaValue(sa, SearchAttributes.LD_SUBLOT, mainProperty.getLegal().getSubdivision().getSubLot());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIV_BLOCK, mainProperty.getLegal().getSubdivision().getBlock());
		}
		
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIV_SEC, mainProperty.getLegal().getTownShip().getSection());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_TWN))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIV_TWN, mainProperty.getLegal().getTownShip().getTownship());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_RNG))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIV_RNG, mainProperty.getLegal().getTownShip().getRange());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIV_UNIT, mainProperty.getLegal().getSubdivision().getUnit());
		}
		if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_PHASE))){
			setNewSaValue(sa, SearchAttributes.LD_SUBDIV_PHASE, mainProperty.getLegal().getSubdivision().getPhase());
		}

		if(CountyConstants.AK_Anchorage_Borough_STRING.equals(sa.getCountyId()) && dat.getType() == GWTDataSite.AO_TYPE) {
			//do not bootstrap plat book-page
			//ignore - causes: 6623
		} else {
			if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_BOOKNO))){
				setNewSaValue(sa, SearchAttributes.LD_BOOKNO, mainProperty.getLegal().getSubdivision().getPlatBook());
			}
			if(overrideIfAvailable || StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_PAGENO))){
				setNewSaValue(sa, SearchAttributes.LD_PAGENO, mainProperty.getLegal().getSubdivision().getPlatPage());
			}
		}
		
		return true;
		
	}

    /**
     * Designed to bootstrap information from document to the Address Area in Search Page
     * @param search
     * @param dataSite 
     * @param document
     * @param overrideIfAvailable if true forces the new value even if something is already available
     * @return
     */
    private static boolean bootstrapAddressInfoFromDocument(Search search, DataSite dataSite, DocumentI document, int serverID, boolean overrideIfAvailable) {
    	SearchAttributes sa = search.getSa();
    	
    	if (document == null) {
    		return false;	//no source
    	}
    	Set<PropertyI> properties = document.getProperties();
    	if (properties == null || properties.isEmpty()) {
    		return false;	//nothing to bootstrap
    	}
    	PropertyI mainProperty = properties.iterator().next();
    	
    	if (mainProperty == null) {
    		return false;
    	}
    	
    	AddressI address = mainProperty.getAddress();
        
    	String streetNumberNew = address.getNumber();
        String streetNameNew = address.getStreetName();
        
		// daca sunt pe serverul de taxe, bootstrapez numai daca nu exista deja date in SA
	    String assFile = search.getSearchDir() + "Assessor" + File.separator;
	    File f = new File(assFile);
	    String trFile = search.getSearchDir() + "County Tax" + File.separator;
	    File ft = new File(trFile);         
	    if ((TSServersFactory.isCountyTax(serverID) || TSServersFactory.isCountyTUTax(serverID)&& f.exists()) ||
	    		(TSServersFactory.isCityTax(serverID) && (f.exists() || ft.exists()))){

			// if (search.searchCycle == 0){ 

			setNewSaValue(sa, SearchAttributes.P_STREETNO, address.getNumber());
			setNewSaValue(sa, SearchAttributes.P_STREETNAME, address.getStreetName());
			setNewSaValue(sa, SearchAttributes.P_STREETUNIT, address.getIdentifierNumber());
			setNewSaValue(sa, SearchAttributes.P_ZIP, address.getZip());
			setNewSaValue(sa, SearchAttributes.P_STREETSUFIX, address.getSuffix());
			setNewSaValue(sa, SearchAttributes.P_STREETDIRECTION, address.getPreDiretion());
			setNewSaValue(sa, SearchAttributes.P_STREET_POST_DIRECTION, address.getPostDirection());
			// }

			if (!dataSite.is(CountyConstants.NV_Clark, GWTDataSite.TR_TYPE)) { // 8181
				if ("".equals(sa.getAtribute(SearchAttributes.P_CITY).trim()))
					setNewSaValue(sa, SearchAttributes.P_CITY, address.getCity());
			}
		} else {
	    		
	    		streetNameNew = address.getPreDiretion() + " " + address.getStreetName() + " " + address.getSuffix() 
						+ " " + address.getPostDirection() + " " + address.getIdentifierType() + " " + address.getIdentifierNumber();
	    		streetNameNew = streetNameNew.replaceAll("\\s+", " ").replaceAll("\\s*#\\s*$", " ").trim();
	    		
	    		if (search.searchCycle == 0){ // bustrapez doar daca sunt la primul ciclu de search        
	    			
	    			setNewSaValue(sa, SearchAttributes.P_STREETNO, address.getNumber());
	    			setNewSaValue(sa, SearchAttributes.P_STREET_FULL_NAME, streetNameNew);
	    	            
	    			setNewSaValue(sa, SearchAttributes.P_STREET_FULL_NAME_EX, 
	    					sa.getAtribute(SearchAttributes.P_STREETNO)+ " " + 
	    					sa.getAtribute(SearchAttributes.P_STREETDIRECTION)+ " " + 	                    
	    					sa.getAtribute(SearchAttributes.P_STREETNAME) + " " +
	    					sa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION));
	    	            
	    			setNewSaValue(sa, SearchAttributes.P_CITY, address.getCity());
	    			setNewSaValue(sa, SearchAttributes.P_ZIP, address.getZip());

	    			setNewSaValue(sa, SearchAttributes.P_STREETNAME_SUFFIX_UNIT_NO, 
	    					(sa.getAtribute(SearchAttributes.P_STREETDIRECTION) + " " + 
	    							sa.getAtribute(SearchAttributes.P_STREETNAME) + " " + 
	    							sa.getAtribute(SearchAttributes.P_STREETSUFIX) + " " +
	    							sa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION) + " " +
	    							sa.getAtribute(SearchAttributes.P_STREETUNIT)  + " " +
	    							sa.getAtribute(SearchAttributes.P_STREETNO)).replaceAll("\\s+", " "));
	    		} else{ // daca nu sunt in primul ciclu de search completez doar cu date 
	    			if ("".equals(sa.getAtribute(SearchAttributes.P_STREETNO).trim())){
	    				setNewSaValue(sa, SearchAttributes.P_STREETNO, address.getNumber());
	    			}

	    			if ("".equals(sa.getAtribute(SearchAttributes.P_STREET_FULL_NAME).trim())){
	    				setNewSaValue(sa, SearchAttributes.P_STREET_FULL_NAME, streetNameNew);
	    			}

	    			if ("".equals(sa.getAtribute(SearchAttributes.P_ZIP).trim())){
	    				setNewSaValue(sa, SearchAttributes.P_ZIP, address.getZip());
	    			}
	    				
	    			if ("".equals(sa.getAtribute(SearchAttributes.P_CITY).trim())){
	    				setNewSaValue(sa, SearchAttributes.P_CITY, address.getCity());
	    			}
	    		}
	        }
	        return true;
		
	}
    
    /**
     * Designed to bootstrap information from document to the PID in Search Page
     * @param search
     * @param dataSite 
     * @param document
     * @param overrideIfAvailable if true forces the new value even if something is already available
     * @return
     */
    private static boolean bootstrapPIDInfoFromDocument(Search search, DataSite dataSite, DocumentI document, int serverID, boolean overrideIfAvailable) {
    	SearchAttributes sa = search.getSa();
    	
    	if (document == null) {
    		return false;	//no source
    	}
    	Set<PropertyI> properties = document.getProperties();
    	if (properties == null || properties.isEmpty()) {
    		return false;	//nothing to bootstrap
    	}
    	PropertyI mainProperty = properties.iterator().next();
    	
    	if (mainProperty == null) {
    		return false;
    	}
    	if(CountyConstants.OH_Franklin_STRING.equals(sa.getCountyId())){
    		accumulatePinsForOHFranklin(sa, mainProperty.getPin().getPin(PinI.PinType.PID));
    	} else if (CountyConstants.MI_Wayne_STRING.equals(sa.getCountyId()) || StateContants.IL_STRING.equals(sa.getStateId())){
    		PropertyIdentificationSet pis = new PropertyIdentificationSet();
    		pis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), mainProperty.getPin().getPin(PinI.PinType.PID));
    		bootstrapPidInternal(sa, pis);
    	} else{
    		setNewSaValue(sa, SearchAttributes.LD_PARCELNO, mainProperty.getPin().getPin(PinI.PinType.PID));
    		if (StringUtils.isNotEmpty(mainProperty.getPin().getPin(PinI.PinType.PID_ALT1))) {
    			setNewSaValue(sa, SearchAttributes.LD_PARCELNO2_ALTERNATE, mainProperty.getPin().getPin(PinI.PinType.PID_ALT1));
    		}
    	}
    	
	    return true;
		
	}
    
    private static void accumulatePinsForOHFranklin(SearchAttributes sa, String newPin){
    	
    	// concatenate old pin to new pin
        String oldPin = sa.getAtribute(SearchAttributes.LD_PARCELNO);       
        if(!StringUtils.isEmpty(oldPin)){
        	newPin = oldPin + "," + newPin;
        }        
        
        // remove spaces, starting and trailing commas
        newPin = newPin.replaceAll("\\s+","").trim();
        newPin = newPin.replaceFirst("^,", "");
		newPin = newPin.replaceFirst(",$", "");	
		
		// remove duplicates
		Set<String>pins = new LinkedHashSet<String>();
		for (String pin : newPin.split(",")){			
    		// add it to set
    		if ((pin.matches("\\d{3}-\\d{6}-00") && pins.contains(pin.replaceFirst("-00$", "")))//010-257355-00 010-257355
    				|| (pin.matches("\\d{3}\\d{6}00") && pins.contains(pin.replaceFirst("00$", "")))//01025735500 010257355
    				|| (pin.matches("\\d{3}-\\d{6}-00") && pins.contains(pin.replaceAll("-", "").replaceFirst("-?00$", "")))//010-272619-00 010272619 
    				
    				|| (pins.contains(pin.replaceAll("-", ""))) 
    				|| (pins.contains(pin.replaceAll("\\A(\\d{3})(\\d{6})$", "$1-$2")))//010257355 010-257355
    				|| (pins.contains(pin.replaceAll("\\A(\\d{3})(\\d{6})(\\d{2})$", "$1-$2-$3")))//01025735500  010-257355-00
    				|| (pin.matches("\\d{3}\\d{6}") 
    						&& (pins.contains(pin.replaceAll("\\A(\\d{3})(\\d{6})$", "$1-$2") + "-00") 
    								|| pins.contains(pin + "00"))))//010257355  010-257355-00 01025735500
    			
    		{
    			continue;
    		} else{
    			boolean addIfTheSameProperty = false;
    			for (String pinis : pins) {
    				pinis = pinis.replaceAll("[-]+", "");
					if (pinis.length() > 8 && pin.replaceAll("[-]+", "").length() > 8){
						if (pinis.substring(0, 9).equals(pin.replaceAll("[-]+", "").substring(0, 9))){
							addIfTheSameProperty = true;
						}
					}
				}
    			if (addIfTheSameProperty || pins.size() == 0){
    				pins.add(pin);
    			}
    		}
		}
		newPin = "";
		for(String pin: pins){
			newPin += "," + pin;
		}
		
		// removes starting and trailing commas
        newPin = newPin.replaceFirst("^,", "");
		newPin = newPin.replaceFirst(",$", "");
		
		// update search attributes
		sa.setAtribute(SearchAttributes.LD_PARCELNO, newPin);
    }
    
    /**
     * Designed to bootstrap information from document to the references in Search Page
     * @param search
     * @param dataSite 
     * @param document
     * @param overrideIfAvailable if true forces the new value even if something is already available
     * @return
     */
    private static boolean bootstrapReferencesFromDocument(Search search, DataSite dataSite, DocumentI document, int serverID, boolean overrideIfAvailable) {
    	SearchAttributes sa = search.getSa();
    	
    	if (document == null) {
    		return false;	//no source
    	}    	
    	Set<InstrumentI> references = document.getParsedReferences();
    	if (references == null){
    		return false;
    	}
    	
    	List instrList = (List) sa.getObjectAtribute(SearchAttributes.INSTR_LIST);
    	for (InstrumentI instrumentI : references) {
			instrList.add(instrumentI);
		}
	    return true;
		
	}
    
    /**
     * Designed to bootstrap information from document to the Owners in Search Page
     * @param search
     * @param dataSite 
     * @param document
     * @param overrideIfAvailable if true forces the new value even if something is already available
     * @return
     */
    private static boolean bootstrapOwnerNameInfoFromDocument(Search search, DataSite dataSite, DocumentI document, int serverID, boolean overrideIfAvailable) {
    	SearchAttributes sa = search.getSa();
    	
    	if (document == null) {
    		return false;	//no source
    	}
    	Set<PropertyI> properties = document.getProperties();
    	if (properties == null || properties.isEmpty()) {
    		return false;	//nothing to bootstrap
    	}
    	PropertyI mainProperty = properties.iterator().next();
    	
    	if (mainProperty == null) {
    		return false;
    	}
        
		DocumentsManagerI managerI = search.getDocManager();
		
		if (search.searchCycle == 0 && dataSite.isTaxLikeSite()) {
			fillSaOwnerObjNameFromDocument(sa, mainProperty.getOwner(), document, serverID);
		} 
		if (search.searchCycle != 0 && dataSite.isTaxLikeSite()) {
			try{
				managerI.getAccess();
				if (managerI.getDocumentsWithDataSource(false, TSServersFactory.getType(serverID)).size() == 0){
					fillSaOwnerObjNameFromDocument(sa, mainProperty.getOwner(), document, serverID);
				}
			}
			finally{
				managerI.releaseAccess();
			}
		}
		
		if (TSServersFactory.isAssesor(serverID)){ // de pe assesor
			// bootstrapez numele doar daca  sunt la primul ciclu de search
			if (search.searchCycle == 0){
				fillSaOwnerObjNameFromDocument(sa, mainProperty.getOwner(), document, serverID);
			}
			else if ("".equals(sa.getAtribute("OWNER_FNAME")) && "".equals(sa.getAtribute("OWNER_LNAME"))){
				fillSaOwnerObjNameFromDocument(sa, mainProperty.getOwner(), document, serverID);
			}
			
		}
		return true;
	}
    
    public static void fillSaOwnerObjNameFromDocument(SearchAttributes sa, PartyI party, DocumentI document, int serverId) {

    	
    	HashSet<String> countynames = new HashSet<String>();
    	countynames.add("jefferson");
    	countynames.add("douglas");
    	countynames.add("boulder");
    	countynames.add("adams");
    	countynames.add("arapahoe");
    	countynames.add("clear creek");
    	countynames.add("el paso");
    	countynames.add("eagle");
    	countynames.add("broomfield");
    	countynames.add("denver");
    	
    	boolean boostrapNames = true;
    	try{
	    	if (sa != null){
	    		DataSite data = HashCountyToIndex.getDateSiteForMIServerID(sa.getCommId(),serverId);
	    		if (data != null){
	    			if (data.isTaxLikeSite() && "co".equalsIgnoreCase(data.getStateAbbreviation()) 
	    					&&  countynames.contains(data.getCountyName().toLowerCase())){
	    				boostrapNames = false;
	    			}
	    		}
	    	}
    	}catch(Exception e){}
    	
    	if (boostrapNames){
	        if (party != null) {
				if (party.size() > 0) {
					for (NameI name : party.getNames()) {
						name.setMiddleName(NameCleaner.processMiddleName(name.getMiddleName()));
						name.setSufix(NameCleaner.processNameSuffix(name.getSufix()));
						if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name, 0.99)){
							sa.getOwners().add(name);
						}
					}
				}
			}
    	}
    }
    
	@SuppressWarnings("unchecked")
	private static void boostrapROBookPageCrossRef(ServerResponse result, SearchAttributes sa) {
		Vector<CrossRefSet> crossRefs = result.getParsedResponse().getCrossRefSets();
		if (crossRefs.size()>MAX_CROSS_REFS_SEARCH){
			return;
		}
		ArrayList<Instrument> list = (ArrayList<Instrument>)sa.getObjectAtribute(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
		for (CrossRefSet set : crossRefs) {
			Instrument inst = null;
			String book = set.getAtribute("Book");
			String page = set.getAtribute("Page");
			String instrNo = set.getAtribute("InstrumentNumber");
			String bookPageType = set.getAtribute("Book_Page_Type");
			String crossRefSource = set.getAtribute("CrossRefSource");
			
			//if we have book-page crossref
			if(!(StringUtils.isEmpty(book) || StringUtils.isEmpty(page))){
				inst = new Instrument();
				inst.setInstrumentType(Instrument.TYPE_BOOK_PAGE);
				inst.setBookNo(book);
				inst.setPageNo(page);
				if(!StringUtils.isEmpty(bookPageType))
					inst.setBookPageType(bookPageType);
			} else if(!StringUtils.isEmpty(instrNo)){
				inst = new Instrument();
				inst.setInstrumentType(Instrument.TYPE_INSTRUMENT_NO);
				inst.setInstrumentNo(instrNo);
			}
			if(inst!=null && !list.contains(inst)){
				if(!StringUtils.isEmpty(crossRefSource))
					inst.setExtraInfo(InstrumentConstants.CROSS_REF_SOURCE_TYPE, crossRefSource);
				list.add(inst);
			}
		}
		
	}


	private static void boostrapBookPage(SaleDataSet sds, SearchAttributes sa) {
        loggerDetails.debug("before add new bookpage "
                + sa.getAtribute(SearchAttributes.LD_BOOKPAGE));
        IndividualLogger.info( "before add new bookpage " + sa.getAtribute(SearchAttributes.LD_BOOKPAGE) ,sa.getSearchId());
        
        loggerDetails.debug("before add book " + sds.getAtribute("Book")
                + " page=" + sds.getAtribute("Page"));
        IndividualLogger.info( "before add book " + sds.getAtribute("Book") + " page=" + sds.getAtribute("Page") ,sa.getSearchId());
        
        FormatSa.addBookAndPage(sa, sds.getAtribute("Book"), sds
                .getAtribute("Page"), false, false);
        
        loggerDetails.debug("after add new bookpage "
                + sa.getAtribute(SearchAttributes.LD_BOOKPAGE));
        IndividualLogger.info( "after add new bookpage " + sa.getAtribute(SearchAttributes.LD_BOOKPAGE),sa.getSearchId() );

    }

    private static void bustrapOwnerName(SearchAttributes sa, PropertyIdentificationSet pis,int serverID, 
    						Vector<PartyNameSet> owners, ServerResponse result) {
    	
		Search s = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext();
		DocumentsManagerI managerI = s.getDocManager();
			
	 	if (s.searchCycle == 0 && 
	 			(TSServersFactory.isCityTax(serverID) 
	 				|| TSServersFactory.isCountyTUTax(serverID)
	 				|| TSServersFactory.isCountyTax(serverID))) {
	 		
	 		fillSaOwnerObjNameFromPis(sa, pis,owners, serverID);
		} 
	 	if (s.searchCycle != 0 && 
	 			(TSServersFactory.isCityTax(serverID) || TSServersFactory.isCountyTUTax(serverID) || TSServersFactory.isCountyTax(serverID))) {

			 	try{
		    		managerI.getAccess();
		    		if (managerI.getDocumentsWithDataSource(false, TSServersFactory.getType(serverID)).size() == 0){
		    			fillSaOwnerObjNameFromPis(sa, pis,owners, serverID);
		    		}
		    	}
		    	finally{
		    		managerI.releaseAccess();
		    	}
		}
        
	 	if (TSServersFactory.isAssesor(serverID) ) // de pe assesor
        {
	 		// bootstrapez numele doar daca  sunt la primul ciclu de search
	 	    if (s.searchCycle == 0)
	 	        fillSaOwnerObjNameFromPis(sa, pis,owners, serverID);
	 	    else 
	 	        if ( "".equals(sa.getAtribute("OWNER_FNAME")) && "".equals(sa.getAtribute("OWNER_LNAME")) )
		 	    {
	 	        	fillSaOwnerObjNameFromPis(sa, pis,owners, serverID);
		 	    }
	 	    
        } else if ( serverID == TSServersFactory.getSiteId("TN", "Hamilton", "TR")) {
            logger.info("HamitlonTR! bustraping name from tax data!");
            fillSaOwnerObjNameFromPis(sa, pis, owners, serverID);
            
        }
    }
    
    public static boolean isAttributeChanged(String saValue, String infsetValue) {
    	return !saValue.equalsIgnoreCase(infsetValue.trim()) && !"".equalsIgnoreCase(saValue);
    }

    public static boolean isAttributeChanged(String saValue, String infsetValue, boolean isChanged) {
    	return isChanged?true:isAttributeChanged(saValue, infsetValue);
    }

    
    public static boolean isBoostrapNameDeleteOnly(String fName, String mName, String lName,
    											String fNameP, String mNameP, String lNameP){
    	fName = fName.toUpperCase();
    	mName = mName.toUpperCase();
    	lName = lName.toUpperCase();
    	fNameP = fNameP.toUpperCase();
    	mNameP = mNameP.toUpperCase();
    	lNameP = lNameP.toUpperCase();
    	if (!"".equals(fNameP) && ! fName.equals(fNameP)){
    		return false;
    	}
    	if (!"".equals(mNameP) && ! mName.equals(mNameP)){
    		return false;
    	}
    	if (!"".equals(lNameP) && ! lName.equals(lNameP)){
    		return false;
    	}
    	return true;
    }
    
    /**
     * B2670:If in the name bootstrap process there is only a delete operation 
     * (there is no change operation involved ) do not apply the bootstrap
     * @param sa
     * @param pis
     * @param owners 
     * @param serverId the serverId that generated this data
     */
    public static void fillSaOwnerObjNameFromPis(SearchAttributes sa,
            PropertyIdentificationSet pis, Vector<PartyNameSet> owners, int serverId) {
    	
    	HashSet<String> countynames = new HashSet<String>();
    	countynames.add("jefferson");
    	countynames.add("douglas");
    	countynames.add("boulder");
    	countynames.add("adams");
    	countynames.add("arapahoe");
    	countynames.add("clear creek");
    	countynames.add("el paso");
    	countynames.add("eagle");
    	countynames.add("broomfield");
    	countynames.add("denver");
    	
    	boolean boostrapNames = true;
    	try{
	    	if(sa!=null){
	    		DataSite data = HashCountyToIndex.getDateSiteForMIServerID(sa.getCommId(),serverId);
	    		if(data!=null){
	    			if(data.isTaxLikeSite() && "co".equalsIgnoreCase(data.getStateAbbreviation()) 
	    					&&  countynames.contains(data.getCountyName().toLowerCase())){
	    				boostrapNames = false;
	    			}
	    		}
	    	}
    	}catch(Exception e){}
    	
    	if(boostrapNames){
	        if (owners != null) {
				if (owners.size() > 0) {
					for (int i = 0; i < owners.size(); i++) {
						
						NameI name = new com.stewart.ats.base.name.Name(
								owners.get(i).getAtribute("FirstName").trim(), 
								NameCleaner.processMiddleName(owners.get(i).getAtribute("MiddleName")),
								owners.get(i).getAtribute("LastName").trim());
						name.setSufix(NameCleaner.processNameSuffix(owners.get(i).getAtribute("Suffix")));
						if(owners.get(i).getAtribute("isCompany") != null && 
								owners.get(i).getAtribute("isCompany").equalsIgnoreCase("yes"))
							name.setCompany(true);
						if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name, 0.99)){
							sa.getOwners().add(name);
						}
						
					}
				} else {
					if (!"".equals(pis.getAtribute("OwnerLastName").trim())){
						String[] names = GenericFunctions.extractSuffix(pis.getAtribute("OwnerMiddleName").trim());
						NameI name=new com.stewart.ats.base.name.Name(
								pis.getAtribute("OwnerFirstName").trim(),
								NameCleaner.processMiddleName(names[0]),
								pis.getAtribute("OwnerLastName").trim());
						name.setSufix(NameCleaner.processNameSuffix(names[1]));
		    	    	if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name,0.99))
		    	    		sa.getOwners().add(name);
					}
					if (!"".equals(pis.getAtribute("SpouseLastName").trim())){
						String[] names = GenericFunctions.extractSuffix(pis.getAtribute("SpouseMiddleName").trim());
		    	    	NameI name=new com.stewart.ats.base.name.Name(
		    	    			pis.getAtribute("SpouseFirstName").trim(),
		    	    			NameCleaner.processMiddleName(names[0]),
		    	    			pis.getAtribute("SpouseLastName").trim());
		    	    	name.setSufix(NameCleaner.processNameSuffix(names[1]));
		    	    	if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name,0.99))
		    	    		sa.getOwners().add(name);
		    	    }
				}
			}
    	}
    }
    
    public static boolean addFromIS(SearchAttributes sa, InfSet is,
            String saID, String isID) {
    	String newValue="";
    	
    	try {
    	
    		newValue = is.getAtribute(isID).trim();
    	
    	} catch (NullPointerException e) {}
    	
        return setNewSaValue(sa, saID, newValue);
    }

    // daca proprietatea veche o include pe cea noua nu se mai suprascrie
    public static boolean addFromISNotIncluded(SearchAttributes sa, InfSet is,
            String saID, String isID) {
        String oldValue = sa.getAtribute(saID);
        String newValue = is.getAtribute(isID).trim();
        if (oldValue.indexOf(newValue) == -1)
            return setNewSaValue(sa, saID, newValue);
        else
            return false;
    }
    
    /**
     * Sets the <b>not</b> empty <code>newValue</code> to the specified <code>key</code> in Search Attributes
     * @param sa the Search Attributes to be updated 
     * @param key the key to will be modified
     * @param newValue must not be empty
     * @return <code>true</code> it the new value was set
     */
    private static boolean setNewSaValue(SearchAttributes sa, String key, String newValue) {
        if (StringUtils.isNotEmpty(newValue)) {
            if (key.equals(SearchAttributes.P_STREET_FULL_NAME)) {
            	String oldValue = sa.getAtribute(key).trim();
                newValue = AddressStringUtils.escapeString(newValue,
                        AddressStringUtils.getEscapedStrings(oldValue));
            }
            sa.setAtribute(key, newValue);
            return true;
        }
        return false;
    }

    public static boolean addToIS(SearchAttributes sa, InfSet is, String saID,
            String isID) {
        String oldValue = is.getAtribute(isID).trim();
        String newValue = sa.getAtribute(saID).trim();
        loggerDetails.debug("old value = " + oldValue);
        loggerDetails.debug("new value = " + newValue);
        if (//StringUtils.isStringBlank(oldValue) && //the new specs are that
            // we
        // should alwalys bustrap , overwritting the information from the
        // formular
        (!StringUtils.isStringBlank(newValue))) {
            is.setAtribute(isID, newValue);
            return true;
        } else
            return false;
    }

    private static class CompareSDSAfterRecordedDate implements Comparator {
        public int compare(Object o1, Object o2) {

            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT,
                    Locale.US);
            Date d1;
            Date d2;
            try {
            	String date = ((SaleDataSet) o1).getAtribute("RecordedDate");
            	date = date.replace("T", " ");
                d1 = df.parse(date);
                d2 = df.parse(((SaleDataSet) o2).getAtribute("RecordedDate"));
                d1 = Util.setTime(d1, ((SaleDataSet) o1).getAtribute("RecordedTime")); // add hh:mm:ss info if available
                d2 = Util.setTime(d2, ((SaleDataSet) o2).getAtribute("RecordedTime")); // add hh:mm:ss info if available
            } catch (ParseException e) {
                logger.error(e.toString());
                return 1;
            }
            if (d1.compareTo(d2) <= 0)
                return -1;
            else
                return 1;
        }
    }

    private static Vector getSaleDataSetsOrdedByDate(Vector t) {
        //return a vector sorted by date
        Collections.sort(t, new CompareSDSAfterRecordedDate());
        return t;
    }

    public static class CompareInstrumentsAfterRecordedDate implements
            Comparator, Serializable {

        static final long serialVersionUID = 10000000;

        public int compare(Object o1, Object o2) {

            Date d1 = ((Instrument) o1).getFileDate();
            Date d2 = ((Instrument) o2).getFileDate();
            if (o1.equals(o2)) {
                return 0;
            } else if (d1.compareTo(d2) <= 0)
                return 1;
            else
                return -1;
        }
    }

    public static Instrument fillInstrumentObj(ParsedResponse pr) {
        Instrument inst = null;
        if (pr.getSaleDataSetsCount() != 0) {
            Vector t = getSaleDataSetsOrdedByDate(pr.getSaleDataSet());
            SaleDataSet sds = (SaleDataSet) t.get(t.size() - 1);
            inst = fillInstrumentFromSds(sds, pr.getGrantorNameSet(), pr.getGranteeNameSet());
        }
        return inst;
    }

    private static Instrument fillInstrumentFromSds(SaleDataSet sds,
            Vector grantorNames, Vector granteeNames) {
        Instrument inst = new Instrument();
        inst.setInstrumentNo(sds.getAtribute("InstrumentNumber"));
        inst.setBookNo(sds.getAtribute("Book"));
        inst.setPageNo(sds.getAtribute("Page"));
        inst.setOrigin(Instrument.SALES_DATA);
        if (((SaleDataSet) sds).getAtribute("RecordedDate").length()==4) ((SaleDataSet) sds).setAtribute("RecordedDate","01/01/"+((SaleDataSet) sds).getAtribute("RecordedDate"));
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
        Date recDate = new Date();
        
        try {
            recDate = df.parse(((SaleDataSet) sds).getAtribute("RecordedDate"));
        } catch (ParseException e) {
            df = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
            
            try {
                recDate = df.parse(((SaleDataSet) sds).getAtribute("RecordedDate"));
            } catch (ParseException er) {
                //logger.error("Recorded date parsing error: ", er);
                
            	recDate = Util.dateParser2(((SaleDataSet) sds).getAtribute("RecordedDate"));
            	if(recDate ==null){
            		//logger.error("Recorded date parsing error in fillInstrumentFromSds: ", er);
            		logger.error("Recorded date parsing error in fillInstrumentFromSds: " + er);
            		recDate = new Date();
            	}
            	
            }
            
            try{
            	recDate = Util.setTime(recDate, ((SaleDataSet) sds).getAtribute("RecordedTime"));
            }catch(Exception e1){ 
            	logger.error("Recorded time parsing error: ", e1);
            }
        }
        Date instrumentDate = new Date();
        String instrumentDateString = ((SaleDataSet) sds).getAtribute("InstrumentDate");
        if(StringUtils.isNotEmpty(instrumentDateString)) {
	        try {
	            instrumentDate = df.parse(instrumentDateString);
	        } catch (ParseException e) {
	            df = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
	            
	            try {
	            	instrumentDate = df.parse(instrumentDateString);
	            } catch (ParseException er) {
	            	
	            	instrumentDate = Util.dateParser2(instrumentDateString);
	            	if(instrumentDate ==null){
	            		logger.debug("Instrument date parsing error: ", er);
	            		instrumentDate = new Date();
	            	}
	            	
	               
	            }
	        }
        }
        inst.setFileDate(recDate);
        inst.setInstrumentDate(instrumentDate);
        return inst;
    }

    private static void boostrapParcelID(int serverID,
            PropertyIdentificationSet pis, SearchAttributes sa) {
        if (serverID != TSServersFactory.getSiteId("TN", "Shelby", "TR")
                && serverID != TSServersFactory.getSiteId("TN", "Shelby", "CT"))
        {
        	bootstrapPidInternal(sa, pis);
        }
        else if (StringUtils.isStringBlank(sa
                .getAtribute(SearchAttributes.LD_PARCELNO))) {
        	bootstrapPidInternal(sa, pis);
        }
    }

    private static Pattern ilCookPidPattern = Pattern.compile("(\\d\\d)(\\d\\d)(\\d\\d\\d)(\\d\\d\\d)(\\d\\d\\d\\d)");

    private static void accumulatePin(SearchAttributes sa, PropertyIdentificationSet pis){
    	
    	// concatenate old pin to new pin
    	String newPin = pis.getAtribute("ParcelID").trim();
        String oldPin = sa.getAtribute(SearchAttributes.LD_PARCELNO);       
        if(!StringUtils.isEmpty(oldPin)){
        	newPin = oldPin + "," + newPin;
        }        
        
        // remove spaces, starting and trailing commas
        newPin = newPin.replaceAll("[-\\s]+","").trim();
        newPin = newPin.replaceFirst("^,", "");
		newPin = newPin.replaceFirst(",$", "");	
		
		// remove duplicates
		Set<String>pins = new LinkedHashSet<String>();
		for(String pin: newPin.split(",")){			
			// bring each PIN to normalized form
    		Matcher pinMatcher = ilCookPidPattern.matcher(pin);
    		if(pinMatcher.matches()){
    			pin = 
    				pinMatcher.group(1) + "-" +
    				pinMatcher.group(2) + "-" +
    				pinMatcher.group(3) + "-" +
    				pinMatcher.group(4) + "-" +
    				pinMatcher.group(5);
    		}
    		// add it to set
    		// Task 7775 - do not add 19-24-417-002-0000, if pins contains 19-24-417-002

    		if(!(pin.matches("\\d{2}-\\d{2}-\\d{3}-\\d{3}-0000") && pins.contains(pin.replaceFirst("-0000$", "")))
    				&& !(pins.contains(pin.replaceAll("\\A(\\d{2})(\\d{2})(\\d{3})(\\d{3})$", "$1-$2-$3-$4") + "-0000") || pins.contains(pin + "0000"))
    				&& !(pins.contains(pin.replaceAll("-", ""))) 
    				&& !(pins.contains(pin.replaceAll("\\A(\\d{2})(\\d{2})(\\d{3})(\\d{3})$", "$1-$2-$3-$4"))) ){
    			pins.add(pin);
    		}
		}
		newPin = "";
		for(String pin: pins){
			newPin += "," + pin;
		}
		
		// removes starting and trailing commas
        newPin = newPin.replaceFirst("^,", "");
		newPin = newPin.replaceFirst(",$", "");
		
		// update search attributes
		sa.setAtribute(SearchAttributes.LD_PARCELNO, newPin);
    }
    
    private static void bootstrapPidInternal(SearchAttributes sa, PropertyIdentificationSet pis){

    	// preprocess the input PIN
    	if(	"ILCook".equals(sa.getStateCounty())){
        	// add dashes back to the ILCook
    		String parcelNo = pis.getAtribute("ParcelID");
    		Matcher ilCookPidMatcher = ilCookPidPattern.matcher(parcelNo);
    		if(ilCookPidMatcher.matches()){
    			pis.setAtribute("ParcelID", 
    					ilCookPidMatcher.group(1) + "-" + 
    					ilCookPidMatcher.group(2) + "-" + 
    					ilCookPidMatcher.group(3) + "-" + 
    					ilCookPidMatcher.group(4) + "-" + 
    					ilCookPidMatcher.group(5));
    		}            
    	} else if("MIWayne".equals(sa.getStateCounty())){
    		// remove part after : for MIWayne
    		String parcelNo = pis.getAtribute("ParcelID");
    		pis.setAtribute("ParcelID", parcelNo.replaceFirst(":\\d+$", ""));
    	}
    	
    	// accumulate all PINs for IL counties
    	if(sa.getStateCounty().startsWith("IL")){
    		accumulatePin(sa, pis);
		} else {
			addFromIS(sa, pis, SearchAttributes.LD_PARCELNO, "ParcelID");
		}        
        addFromIS(sa, pis, SearchAttributes.LD_PARCELNO_MAP, "ParcelIDMap");
        addFromIS(sa, pis, SearchAttributes.LD_PARCELNO_GROUP, "ParcelIDGroup");
        addFromIS(sa, pis, SearchAttributes.LD_PARCELNO_PARCEL, "ParcelIDParcel");
        addFromIS(sa, pis, SearchAttributes.LD_PARCELNO_CONDO, "ParcelIDCondo");    	
    }    
    
    private static ArrayList condominiumTokens;

    static {
        condominiumTokens = new ArrayList();
        condominiumTokens.add("CONDO");
        condominiumTokens.add("CONDOS");
        condominiumTokens.add("CONDOMINIUM");
        condominiumTokens.add("ARMS");
        condominiumTokens.add("COND");
    }

    private static void checkForCondominiums(SearchAttributes sa, PropertyIdentificationSet pis)     
    {
    	String subdName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
    	String condoName = pis.getAtribute("SubdivisionCond");
    	
    	if (sa.getAtribute(SearchAttributes.P_STREETUNIT).length() == 0 && sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT).length() > 0 &&
    			!StringUtils.isStringBlank(subdName) && isCondominiumSubd(subdName, condoName)){
    		sa.setAtribute(SearchAttributes.P_STREETUNIT, sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT)); // fix for bug #1344
    	}
    		
    	// this generate B 3893
        /*if (sa.getAtribute(SearchAttributes.LD_LOTNO).length() == 0 &&
                sa.getAtribute(SearchAttributes.P_STREETUNIT).length() > 0)
        {
            sa.setAtribute(SearchAttributes.LD_LOTNO, sa.getAtribute(SearchAttributes.P_STREETUNIT));
        }*/
        if (!StringUtils.isStringBlank(sa
                .getAtribute(SearchAttributes.LD_SUBDIV_UNIT))) {
            return;
        }
        
    }

    private static boolean isCondominiumSubd(String subdName, String condo) {
    	if (condo != null && condo.length() > 0)
    		return true;
    	
        for (Iterator iter = condominiumTokens.iterator(); iter.hasNext();) {
            String token = (String) iter.next();
            if (Pattern.compile("(?i)\\b" + token + "\\b").matcher(subdName)
                    .find())
                return true;
        }
        return false;
    }

    private static String extractUnitNo(String addrUnit) {
        Matcher m = Pattern.compile("\\d+$").matcher(addrUnit);
        if (m.find())
            return m.group(0);
        return "";
    }

    private static ArrayList invalidInstrumentNo;

    static {
        invalidInstrumentNo = new ArrayList();
        invalidInstrumentNo.add("SSN");
        invalidInstrumentNo.add("LM");
        invalidInstrumentNo.add("TIM");
        invalidInstrumentNo.add("STARTER");
    }

    private static boolean isInvalidInstrument(Instrument instr) {
        for (Iterator iter = invalidInstrumentNo.iterator(); iter.hasNext();) {
            String token = (String) iter.next();
            if (Pattern.compile("(?i)^" + token + "$").matcher(
                    instr.getInstrumentNo()).find())
                return true;
        }
        return false;
    }
    
    public static void boostrapInstrumentListFromIndex(Vector instrVect,
            Search crtSearch) {
        List instrList = (List) (crtSearch.getSa()
                .getObjectAtribute(SearchAttributes.INSTR_LIST));

        instrList.clear();
        for (Iterator iter = instrVect.iterator(); iter.hasNext();) {
            Instrument element = (Instrument) iter.next();
            if (!isInvalidInstrument(element)) {
            	if(!instrList.contains(element))
            		instrList.add(element);
            } else {
                logger.debug("Invalid Instrument#  =["
                        + element.getInstrumentNo() + "]");
            }
        }

        if (logger.isDebugEnabled())
            logger
                    .debug("Current instrument list after boostraping from index =["
                            + instrList + "]");
    }

    private static ArrayList invalidAddressTokens;

    static {
        invalidAddressTokens = new ArrayList();
        invalidAddressTokens.add("NASHVILLE");
        invalidAddressTokens.add("MADISON");
        invalidAddressTokens.add("HERMITAGE");
        invalidAddressTokens.add("ANTIOCH");
        invalidAddressTokens.add("HICKORY");
    }

    public static void manageAddressDavidson(PropertyIdentificationSet pis) {
        String s = pis.getAtribute("StreetName");
        if (StringUtils.isStringBlank(s)) {
            if (logger.isDebugEnabled())
                logger.debug("No address defined for DavidsonAO");
            return;
        }
        String city = pis.getAtribute("City");
        if (!StringUtils.isStringBlank(city)
                && s.matches("(?i)\\b" + city + "\\b")) {
            s.replaceAll("(i?)\\b" + city + "\\b", "");
        } else {
        	// MAIN ANTIOCH
        	
        	String[] tokens = s.split("\\s+");
        	int tokenNumber = 0;
        	for (int i = 0; i < tokens.length; i++) 
        	{
        		if (!Normalize.isSuffix(tokens[i]))
        			tokenNumber++;
        	}
        	
        	if (tokenNumber > 1) {
	            for (Iterator iter = invalidAddressTokens.iterator(); iter
	                    .hasNext();) {
	                String token = (String) iter.next();
	                s = s.replaceAll("(?i)\\b" + token + "\\b", "");
	            }
        	}
        }
        pis.setAtribute("StreetName", s);
    }

    public static boolean bootstrapAddress(int serverID, SearchAttributes sa,
            PropertyIdentificationSet pis, DataSite dataSite, boolean addressBootstrapEnabled) {

        String streetFullName = sa
                .getAtribute(SearchAttributes.P_STREET_FULL_NAME);
        String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
        String streetFullNameNew = pis.getAtribute("StreetName");
        String streetNoNew = pis.getAtribute("StreetNo");
        
        boolean ceva = false;
        boolean isCR = false;
        if (TSServer.getCrtTSServerName(serverID).equals("FLSt. JohnsAO") && (streetFullNameNew.contains("CR ") || streetFullNameNew.contains("COUNTY ROAD "))) {
        	sa.setAtribute(SearchAttributes.P_STREETNAME, streetFullNameNew.replaceAll("COUNTY ROAD", "CR").replaceAll("(CR\\s+\\d+).*", "$1"));
        	sa.setAtribute(SearchAttributes.P_STREETSUFIX, "");
        	
        	isCR = true;
        }
        if (!StringUtils.isStringBlank(streetFullName)
                && !StringUtils.isStringBlank(streetNo)
                && serverID == TSServersFactory.getSiteId("TN", "Rutherford", "CT")
                && !StringUtils.flexibleEqualsIgnoreCaseAndBlank(
                        streetFullName, streetFullNameNew,sa.getSearchId())) {
            return false;
        }
        
        if (addressBootstrapEnabled) {
        	// daca sunt pe serverul de taxe, bootstrapez numai daca nu exista deja date in SA
            Search s = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext();
            String assFile = s.getSearchDir() + "Assessor" + File.separator;
    		File f = new File(assFile);
    		String trFile = s.getSearchDir() + "County Tax" + File.separator;
    		File ft = new File(trFile);         
    		if ((TSServersFactory.isCountyTax(serverID)||TSServersFactory.isCountyTUTax(serverID)&& f.exists()) ||
                       (TSServersFactory.isCityTax(serverID) && (f.exists() || ft.exists()) ))                    
            {
    			if (s.searchCycle == 0) // bustrapez doar daca sunt la primul ciclu de search
            	{
    				addFromIS(sa, pis, SearchAttributes.P_STREETNO, "StreetNo");
    				addFromIS(sa, pis, SearchAttributes.P_STREET_FULL_NAME,"StreetName");
            	}

                if ("".equals(sa.getAtribute(SearchAttributes.P_STREETUNIT).trim())) { //fix for bug #343
            		// parse the string composed of street number and street full name; street unit will be extracted from the full address string
            		StandardAddress stdAddr = new StandardAddress(streetNoNew + " " + streetFullNameNew);
        			String parsedUnit = (stdAddr
        					.getAddressElement(StandardAddress.STREET_SEC_ADDR_IDENT) + stdAddr
        					.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE))
        					.trim();
        			if(parsedUnit.length()>0){
        				setNewSaValue(sa, SearchAttributes.P_STREETUNIT, parsedUnit);
        			}
                }
                    
                
                if ("".equals(sa.getAtribute(SearchAttributes.P_ZIP).trim()))
                    addFromIS(sa, pis, SearchAttributes.P_ZIP, "Zip");

                
                if( ! dataSite.is(CountyConstants.NV_Clark, GWTDataSite.TR_TYPE)) {		//8181
    	            if ("".equals(sa.getAtribute(SearchAttributes.P_CITY).trim()))
    	                addFromIS(sa, pis, SearchAttributes.P_CITY, "City");  
                }
                
                addFromIS(sa, pis, SearchAttributes.P_MUNICIPALITY, "MunicipalJurisdiction");
                
            } else {
    			
            	if (s.searchCycle == 0) // bustrapez doar daca sunt la primul ciclu de search
            	{        		            

            	    StandardAddress stdAddress = new StandardAddress((pis.getAtribute("StreetNo") + " " + pis.getAtribute("StreetName")).toUpperCase().trim());
            	    if (isCR == true)
            	    	stdAddress.setAddressElement(StandardAddress.STREET_NAME, streetFullNameNew);
            	    String strName = stdAddress.getAddressElement(StandardAddress.STREET_NAME);     
    	            String saStrName = sa.getAtribute(SearchAttributes.P_STREETNAME); 
    	            
    	            addFromIS(sa, pis, SearchAttributes.P_STREETNO, "StreetNo");     
    	            if (TSServer.getCrtTSServerName(serverID).equals("FLSt. JohnsAO") && (streetFullNameNew.contains("CR ") || streetFullNameNew.contains("COUNTY ROAD "))) 
    	            	ceva = true;
    	            else
    	            	addFromIS(sa, pis, SearchAttributes.P_STREET_FULL_NAME,  "StreetName");
    	            
    	            setNewSaValue(sa, SearchAttributes.P_STREET_FULL_NAME_EX, 
    	                    sa.getAtribute(SearchAttributes.P_STREETNO)+ " " + 
    	                    sa.getAtribute(SearchAttributes.P_STREETDIRECTION)+ " " + 	                    
    	                    sa.getAtribute(SearchAttributes.P_STREETNAME) + " " +
    	                    sa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION));
    	            addFromIS(sa, pis, SearchAttributes.P_CITY, "City");
    	            addFromIS(sa, pis, SearchAttributes.P_ZIP, "Zip");

    	            addFromIS(sa, pis, SearchAttributes.P_MUNICIPALITY, "MunicipalJurisdiction");

    	            setNewSaValue(sa, SearchAttributes.P_STREETNAME_SUFFIX_UNIT_NO, 
    	            		(sa.getAtribute(SearchAttributes.P_STREETDIRECTION) + " " + 
    	            		sa.getAtribute(SearchAttributes.P_STREETNAME) + " " + 
    	            		sa.getAtribute(SearchAttributes.P_STREETSUFIX) + " " +
    	            		sa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION) + " " +
    	            		sa.getAtribute(SearchAttributes.P_STREETUNIT)  + " " +
    	            		sa.getAtribute(SearchAttributes.P_STREETNO)).replaceAll("\\s+", " "));
    	            
    	            addFromIS(sa, pis, SearchAttributes.OWNER_ZIP, "OwnerZipCode");
            	
            	}
            	else // daca nu sunt in primul ciclu de search completez doar cu date 
            	{
    				if ("".equals(sa.getAtribute(SearchAttributes.P_STREETNO).trim()))
    							  addFromIS(sa, pis, SearchAttributes.P_STREETNO, "StreetNo");

    			  if ("".equals(sa.getAtribute(SearchAttributes.P_STREET_FULL_NAME)
    					  .trim()))
    				  addFromIS(sa, pis, SearchAttributes.P_STREET_FULL_NAME,
    						  "StreetName");

    			  if ("".equals(sa.getAtribute(SearchAttributes.P_ZIP).trim()))
    				  addFromIS(sa, pis, SearchAttributes.P_ZIP, "Zip");

    			  if ("".equals(sa.getAtribute(SearchAttributes.P_CITY).trim()))
    				  addFromIS(sa, pis, SearchAttributes.P_CITY, "City");
    			  
    			  if ("".equals(sa.getAtribute(SearchAttributes.OWNER_ZIP).trim()))
    				  addFromIS(sa, pis, SearchAttributes.OWNER_ZIP, "OwnerZipCode");
            	}
            }
        }
        return true;

    }

    public static void boostrapBookPageRef(SearchAttributes sa,
            ServerResponse result) {
        List instrList = (List) (sa
                .getObjectAtribute(SearchAttributes.INSTR_LIST));

        Vector crossRefSets = result.getParsedResponse()
                .getPropertyIdentificationSet();
        if (crossRefSets.size() > MAX_CROSS_REFS_SEARCH) // daca am gasit mai mult de n crossrefuri, nu mai caut dupa ele
        // dar marchez documentul respectiv ca nu s-a cautat dupa crossref pt a fi evidentiat in TSR Index
        {
            return;
        }
        if (result.getParsedResponse().getPropertyIdentificationSetCount() != 0) {
            String bps = result.getParsedResponse()
                    .getPropertyIdentificationSet(0)
                    .getAtribute("BookPageRefs");
            if (!StringUtils.isStringBlank(bps)) {
                String[] t = bps.split(" ");
                if (t != null) {
                    for (int i = 0; i < t.length; i++) {
                        String[] b_p = t[i].split("/");
                        if (!StringUtils.isStringBlank(b_p[0])
                                && !StringUtils.isStringBlank(b_p[1])) {
                            Instrument instrument = new Instrument();
                            instrument
                                    .setInstrumentType(Instrument.TYPE_BOOK_PAGE);
                            instrument.setBookNo(b_p[0]);
                            instrument.setPageNo(b_p[1]);
                            if(!instrList.contains(instrument))
                            	instrList.add(instrument);
                        }
                    }
                }
            }
        }
    }

    public static void boostrapInstrumentListFromCrossRefSets(
            SearchAttributes sa, ServerResponse result) {
        List instrList = (List) (sa
                .getObjectAtribute(SearchAttributes.INSTR_LIST));
        Vector crossRefSets = result.getParsedResponse().getCrossRefSets();

        if (crossRefSets.size() >= MAX_CROSS_REFS_SEARCH) // daca am gasit mai mult de n crossrefuri, nu mai caut dupa ele
        // dar marchez documentul respectiv ca nu s-a cautat dupa crossref pt a fi evidentiat in TSR Index
        {
            return;
        }

        for (Iterator iter = crossRefSets.iterator(); iter.hasNext();) {
            CrossRefSet element = (CrossRefSet) iter.next();
            Instrument instr = new Instrument();
            
            String instrNo = element.getAtribute("InstrumentNumber");
            String book = element.getAtribute("Book");
            String page = element.getAtribute("Page");
            String bp_type = element.getAtribute("Book_Page_Type");
            String instr_ref_type = element.getAtribute("Instrument_Ref_Type");
            
            if (!StringUtils.isStringBlank(instrNo)) {
                instr.setInstrumentNo(instrNo);
                instr.setInstrumentType(Instrument.TYPE_INSTRUMENT_NO);
                if(!instrList.contains(instr))
                	instrList.add(instr);
            } else if (!StringUtils.isStringBlank(book)
                    && !StringUtils.isStringBlank(page)) {
                instr.setBookNo(book);
                instr.setPageNo(page);
                if(!StringUtils.isEmpty(bp_type))
                	instr.setBookPageType(bp_type);
                if(!StringUtils.isEmpty(instr_ref_type))
                	instr.setInstrumentRefType(instr_ref_type);
                
                instr.setInstrumentType(Instrument.TYPE_BOOK_PAGE);
                if(!instrList.contains(instr))
                instrList.add(instr);
            }
        }
    }

}

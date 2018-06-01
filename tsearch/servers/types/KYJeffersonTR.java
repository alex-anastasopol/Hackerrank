package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;

public class KYJeffersonTR extends TSServer{
	
	private static final long serialVersionUID = -1433966764070682964L;
    
    public static final String nextLinkToken = "<INPUT TYPE=Submit NAME=\"fpdbr_0_PagingMove\" VALUE=\"  >   \">";
    public static final String prevLinkToken = "<INPUT TYPE=Submit NAME=\"fpdbr_0_PagingMove\" VALUE=\"   <  \">";
    
    public static final String resultsDelim = "(?i)<hr>";
    
    public static final Pattern pidPattern = Pattern.compile( "(?si)Property ID:</td>\\s*</tr>\\s*<tr>\\s*<td>\\s*([0-9A-Za-z]*)\\s*</td" );
    
	public KYJeffersonTR( String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
    {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        //resultType = MULTIPLE_RESULT_TYPE;
    }
	
	public TSServerInfo getDefaultServerInfo() {
		
	 	TSServerInfo msiServerInfoDefault = null;
        TSServerInfoModule simTmp = null;
       
            msiServerInfoDefault = new TSServerInfo(3);
            
            msiServerInfoDefault.setServerAddress( "www.jcsoky.org" );

            msiServerInfoDefault.setServerLink( "http://www.jcsoky.org/" );

            msiServerInfoDefault.setServerIP( "www.jcsoky.org" );
        
        
            {//search by name
//        	simTmp = SetModuleSearchByName( 
//                    3, 
//                    msiServerInfoDefault, 
//                    TSServerInfo.NAME_MODULE_IDX, 
//                    "/ptax_search_results_name.asp", 
//                    TSConnectionURL.idGET, 
//                    "searchField", 
//                    "");
            
                simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.NAME_MODULE_IDX, 2);
                simTmp.setName("Name"); //it will be displayed in jsp
                simTmp.setDestinationPage("/ptax_search_results_name.asp");
                simTmp.setRequestMethod(TSConnectionURL.idPOST);
                simTmp.setParserID(ID_SEARCH_BY_NAME);
            
                simTmp.getFunction(0).setName("Last Name:"); //it will be displayed in jsp
                simTmp.getFunction(0).setParamName("WEONM1");
                simTmp.getFunction(0).setSaKey(SearchAttributes.OWNER_LFM_NAME);
                try
                {
                    PageZone searchByName = new PageZone("searchByName", "Owners Name Search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                    searchByName.setBorder(true);
                    
                    HTMLControl ownerName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "WEONM1", "Enter a Name:", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                    ownerName.setJustifyField( true );
                    ownerName.setRequiredCritical( true );
                    ownerName.setFieldNote("(Last First Middle Initial)");
                    searchByName.addHTMLObject( ownerName );
    
                    HTMLControl nextString = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "fpdbr_0_PagingMove", "", 1, 1, 1, 1, 30, "  |<  ", simTmp.getFunction( 1 ), searchId );
                    nextString.setHiddenParam( true );
                    searchByName.addHTMLObject( nextString );
                    
                    simTmp.setModuleParentSiteLayout( searchByName );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
                
                simTmp.getFunction( 1 ).setHiddenParam( "fpdbr_0_PagingMove", "  |<  " );
                
            }
            
            
            {//search by parcel id

                /*simTmp = SetModuleSearchByParcelNo(
                        2,
                        msiServerInfoDefault,
                        TSServerInfo.PARCEL_ID_MODULE_IDX,
                        "/ptax_search_results_pid.asp",
                        TSConnectionURL.idPOST,
                        "WEPROP"
                        );
                */
            	
            	simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.PARCEL_ID_MODULE_IDX, 2);
                simTmp.setName("Parcel Number"); //it will be displayed in jsp
                simTmp.setDestinationPage("/ptax_search_results_pid.asp");
                simTmp.setRequestMethod(TSConnectionURL.idPOST);
                simTmp.setParserID(ID_SEARCH_BY_PARCEL);
                simTmp.getFunction(0).setName("Parcel No:"); //it will be displayed
                simTmp.getFunction(0).setParamName("WEPROP");
                simTmp.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
                // simTmp.getFunction(0).setIteratorType(TSServerInfoFunction.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
                
                try
                {
                    PageZone searchByParcel = new PageZone("searchByParcel", "Property ID search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                    searchByParcel.setBorder(true);
                    
                    HTMLControl parcelId = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "WEPROP", "Property ID:", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                    parcelId.setJustifyField( true );
                    parcelId.setRequiredCritical( true );
                    parcelId.setFieldNote("(14 digits)");
                    searchByParcel.addHTMLObject( parcelId );
    
                    HTMLControl nextString = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "fpdbr_0_PagingMove", "", 1, 1, 1, 1, 30, "  |<  ", simTmp.getFunction( 1 ), searchId );
                    nextString.setHiddenParam( true );
                    searchByParcel.addHTMLObject( nextString );
                    
                    simTmp.setModuleParentSiteLayout( searchByParcel );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }

                simTmp.getFunction( 0 ).setSaKey( SearchAttributes.LD_PARCELNO_GENERIC_TR );
                simTmp.getFunction( 1 ).setHiddenParam( "fpdbr_0_PagingMove", "  |<  " );
            }

            {
                simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.ADDRESS_MODULE_IDX, 2);
                simTmp.setName("Address"); //it will be displayed in jsp
                simTmp.setDestinationPage( "/ptax_search_results_addr.asp" );
                simTmp.setRequestMethod( TSConnectionURL.idPOST );
                simTmp.setParserID(ID_SEARCH_BY_ADDRESS);
                simTmp.getFunction(0).setName("Property Address:");
                simTmp.getFunction(0).setParamName( "WEADDR" );
                simTmp.getFunction( 0 ).setSaKey( SearchAttributes.P_STREET_FULL_NAME_EX );
                
                try
                {
                    PageZone searchByAddress = new PageZone("searchByAddress", "Property Address Search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                    searchByAddress.setBorder(true);
                    
                    HTMLControl address = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "WEADDR", "Property Address:", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                    address.setJustifyField( true );
                    address.setRequiredCritical( true );
                    address.setFieldNote("(ex. 600 W Jefferson St)");
                    searchByAddress.addHTMLObject( address );
    
                    HTMLControl nextString = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "fpdbr_0_PagingMove", "", 1, 1, 1, 1, 30, "  |<  ", simTmp.getFunction( 1 ), searchId );
                    nextString.setHiddenParam( true );
                    searchByAddress.addHTMLObject( nextString );
                    
                    simTmp.setModuleParentSiteLayout( searchByAddress );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
                
                simTmp.getFunction( 1 ).setHiddenParam( "fpdbr_0_PagingMove", "  |<  " );
            }
            
            msiServerInfoDefault.setupParameterAliases();
            setModulesForAutoSearch(msiServerInfoDefault);
            
        return msiServerInfoDefault;
	}
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
        int iStart = -1;
        
		String rsResponse = Response.getResult();
        String sTmp = CreatePartialLink(TSConnectionURL.idPOST);
        
        String postNextResults = "";
        String paramName = "";
        if( viParseID ==ID_SEARCH_BY_NAME )
        {
            postNextResults = "/ptax_search_results_name.asp";
            paramName = "WEONM1";
        }
        else if( viParseID ==ID_SEARCH_BY_ADDRESS )
        {
            postNextResults = "/ptax_search_results_addr.asp";
            paramName = "WEADDR";
        }
        
		switch(viParseID){
		
			case ID_SEARCH_BY_NAME:
            case ID_SEARCH_BY_PARCEL:
            case ID_SEARCH_BY_ADDRESS:
            case ID_DETAILS:
            case ID_SAVE_TO_TSD:
                String nextLink = "";
                String prevLink = "";
                
                int pozSt = -1;
                int pozSf = -1;
                
				pozSt  = rsResponse.indexOf("&nbsp;<div align=\"center\">");
                if( pozSt >= 0 )
                {
                    rsResponse = rsResponse.substring(pozSt);
                }
				
				pozSf = rsResponse.indexOf("Would you like to run another ");
                if( pozSf >= 0 )
                {
                    rsResponse = rsResponse.substring(0,pozSf);
                }
				
                rsResponse = rsResponse.replaceAll("bgcolor=\"[#0-9A-F]*\"", "");
                rsResponse = rsResponse.replaceAll("color: #FFFFFF", "color: #000000");
                rsResponse = rsResponse.replaceAll( "bordercolor=\"[#0-9A-F]*\"", "bordercolor=\"#000000\"" );
				
				//scot partea cu:    Disclaimer: The tax data found on this site is unofficial. For official ...
                //rsResponse = rsResponse.replaceAll("<p.*?>[^$]*?</p>", ""); 
                // commented out the previous line because the disclaimer is already deleted at this point 
                // and deleting paragraphs will delete also the separation seq between documents (resultsDelim) - parser bug reported by ATS on 11/17/2006 
                
                
                rsResponse = rsResponse.replaceAll( "(?i)<div.*?>", "" );
                rsResponse = rsResponse.replaceAll( "(?i)</div>", "" );                
				if(rsResponse.indexOf("&nbsp;")==0){
                    rsResponse = rsResponse .substring("&nbsp;".length());
				}
				
                String formStr = "";
                
                iStart = rsResponse.indexOf( "<form" );
                if( iStart < 0 )
                {
                    iStart = rsResponse.indexOf( "<FORM" );
                }
                
                if( iStart >= 0 )
                {
                    //found form in response
                    formStr = rsResponse.substring( iStart );
                }
                
                HashMap formParams = HttpUtils.getFormParams( formStr );
                
                // treat the WEADDR separately - the getFormParam() does not allow spaces inside value
                Pattern weaddrPat = Pattern.compile("<INPUT TYPE=HIDDEN NAME=\"WEADDR\" VALUE=\"([^\"]*)\">");
                Matcher weaddrMat = weaddrPat.matcher(formStr);
                if(weaddrMat.find()){
                	formParams.put("WEADDR", weaddrMat.group(1));
                }

                //next and previous links
                if( rsResponse.indexOf( nextLinkToken ) >= 0 )
                {
                    nextLink = sTmp + postNextResults +"&" + paramName + "=" + formParams.get( paramName ) + "&fpdbr_0_PagingMove=++%3E+++";
                }

                if( rsResponse.indexOf( prevLinkToken ) >= 0 )
                {
                    prevLink = sTmp + postNextResults + "&" + paramName + "=" + formParams.get( paramName ) + "&fpdbr_0_PagingMove=+++%3C++";
                }
                
                rsResponse = rsResponse.replace( formStr, "" );
                
                String result = "";
                Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
                
                if (viParseID == ID_SAVE_TO_TSD)
                {
                    String pidNo = getInstrNoFromResponse( rsResponse );
                    
                    rsResponse = rsResponse.replaceAll( "(?i)<hr>", " " );
                    
                    msSaveToTSDFileName = pidNo + ".html";
                    Response.getParsedResponse().setFileName(
                            getServerTypeDirectory() + msSaveToTSDFileName);
                    msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD(true);
                    
                    ParsedResponse pr = Response.getParsedResponse();

                    
                    parser.Parse(pr, rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
                    
                    pr.setOnlyResponse(rsResponse);
                }
                else
                {
                    String[] searchResults = rsResponse.split( resultsDelim );
                    
                    String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
                    		+ "\n<table width=\"100%\" border=\"1\" style='border-collapse: collapse'>\n" 
                    		+ "<tr><th>"+ SELECT_ALL_CHECKBOXES + "</th><th></th>";
                    result += header;
                    
                    for( int i = 0 ; i < searchResults.length ; i ++ )
                    {
                        String initialResponse = searchResults[i];
                        
                        String row = initialResponse;
                        
                        String pidNo = getInstrNoFromResponse( initialResponse );
                        
                        if( "none".equals( pidNo ) )
                        {
                            continue;
                        }
                        
                        String instrumentOriginalLink = getOriginalLink( pidNo );
                        String sSave2TSDLink = sTmp + instrumentOriginalLink;

                        String checkBox = "";
                        HashMap<String, String> data = new HashMap<String, String>();
    					data.put("type","CNTYTAX");
                        
    					if (isInstrumentSaved(pidNo, null, data)) {
                        	checkBox = "saved";
                        } else {
                        	checkBox = "<input type='checkbox' name='docLink' value='" 
                        			+ sSave2TSDLink + "'>";
                        }

                        row = row.replaceFirst("(?si)(<table.*?>)",
                        		"<tr><td>" + checkBox + "</td><td>$1</td></tr>");

                        mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
                        ParsedResponse pr = new ParsedResponse();
                        pr.setPageLink(new LinkInPage(sSave2TSDLink, instrumentOriginalLink, TSServer.REQUEST_SAVE_TO_TSD));
                        parser.Parse(pr, row, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
                        
                        parsedRows.add(pr);
                        result += row;
                    }
                    
                    String footer = "\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
                	result += footer;
                    rsResponse = result;
                    
                    if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH) {
                        if (!prevLink.equals("")) {
                            ParsedResponse pr = new ParsedResponse();
                            parser.Parse(pr, "<tr><td colspan='2' align='center'><a href=\"" + prevLink + "\">Previous</a>&nbsp;&nbsp;&nbsp;&nbsp;</td></tr>", Parser.NO_PARSE);
                            parsedRows.add(pr);
                        }
    
                        if (!nextLink.equals("")) {
                            ParsedResponse pr1 = new ParsedResponse();
                            parser.Parse(pr1, "<tr><td colspan='2' align='center'><a href=\"" + nextLink + "\">Next</a>&nbsp;&nbsp;&nbsp;</td></tr>", Parser.NO_PARSE);
                            parsedRows.add(pr1);
                        }
                    }
                    
                    parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
                    Response.getParsedResponse().setResultRows(parsedRows);
                    Response.getParsedResponse().setHeader(header);
                    Response.getParsedResponse().setFooter(footer);
                    
                    if(!nextLink.equals("")){
                    	nextLink = "<a href=\"" + nextLink + "\">Next</a>";
                    }
                    System.err.println(nextLink);
                    Response.getParsedResponse().setNextLink(nextLink);
                    
                    if( mSearch.getSearchType() == Search.AUTOMATIC_SEARCH && parsedRows.size() == 1 )
                    {
                        //if on automatic only one result found --> save to tsd
                        Response.getParsedResponse().setPageLink( ((ParsedResponse)parsedRows.elementAt( 0 )).getPageLink() );
                    }
                }
				break;
            case ID_GET_LINK:
                
                if( sAction.indexOf( "/ptax_search_results_name.asp" ) >= 0 )
                {
                    ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
                }
                else if( sAction.indexOf( "/ptax_search_results_addr.asp" ) >= 0 )
                {
                    ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
                }
                else
                {
                    ParseResponse(sAction, Response, ID_DETAILS);
                }
                
                break;
		}
	
	}
	
	 public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart,
	            int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
	        
	        p.splitResultRows(
	                pr,
	                htmlString,
	                pageId,
	                "<div",
	                "</div>", linkStart, action);

	        // remove table header
	        Vector rows = pr.getResultRows();

	        if (rows.size()>0)
	        { 
	            ParsedResponse firstRow = (ParsedResponse)rows.remove(0);
	            ParsedResponse secondRow = (ParsedResponse)rows.remove(0);
	            ParsedResponse laastRow = (ParsedResponse)rows.remove(rows.size()-1);
	            pr.setResultRows(rows);
	            pr.setHeader(pr.getHeader()+firstRow.getResponse()+secondRow.getResponse());
	            pr.setFooter(pr.getFooter()+laastRow.getResponse());
	        }
	        
	        rows = pr.getResultRows();
	        
	    }
	
     public ServerResponse SearchBy( TSServerInfoModule module, Object sd) throws ServerResponseException
     {           
         if( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() == Search.AUTOMATIC_SEARCH )
         {
             if( module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX )
             {
                 //<Str#><><%><><Street>
                 SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
                 
                 String streetNo = sa.getAtribute( SearchAttributes.P_STREETNO );
                 String streetName = sa.getAtribute( SearchAttributes.P_STREETNAME );
                 
                 //module.getFunction( 0 ).setParamValue( streetNo + " % " + streetName );
                 module.getFunction( 0 ).setParamValue( (streetNo + " " + streetName).trim() );
             }
         }
         
         return super.SearchBy(module, sd);
     }
     
        protected void setModulesForAutoSearch(TSServerInfo serverInfo)
        {
            List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

            TSServerInfoModule m;
            FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
    		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
    		
            
            // search by PID
            if(hasPin()){
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));           
	            m.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_FULL);
	            l.add(m);
            }
            
            //search by address
            if(hasStreet()){
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	            l.add(m);
            }
            
            //search by owner name
            if( hasOwner() ){
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	            m.clearSaKeys();
	            m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
	            
	            m.addFilter(addressFilter);
				m.addFilter(nameFilterHybrid);
	            
//	            m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
	            m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
//	            m.setIteratorType(ModuleStatesIterator.TYPE_TR_KYJEFFERSON);

	            m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;F;","L;M;","L;f;","L;m;"});
				m.addIterator(nameIterator);
	            
	            l.add(m);
            }
            
            serverInfo.setModulesForAutoSearch(l);
        }
     
        private String getInstrNoFromResponse(String response)
        {
            Matcher pidMatcher = pidPattern.matcher( response );
            
            if( pidMatcher.find() )
            {
                return pidMatcher.group( 1 );
            }
            
            return "none";
        }

        private String getOriginalLink( String pid )
        {
            return "/ptax_search_results_pid.asp&WEPROP=" + pid + "&fpdbr_0_PagingMove=++%7C%3C++";
        }
}

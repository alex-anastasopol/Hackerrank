package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.validator.DocsValidator;
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
import ro.cst.tsearch.utils.InstanceManager;

public class KYJeffersonMS extends TSServer implements TSServerROLikeI {

	
	private static final long serialVersionUID = -2323053816510135050L;
	  
    public static final Pattern resultsTable = Pattern.compile( "(?si)<table[^>]*bordercolor=pink[^>]*>(.*?)</table>" );
    public static final Pattern subresultsPattern = Pattern.compile( "(?si)<tr.*?</tr>" );
    public static final Pattern pidPattern = Pattern.compile( "(?si)<td[^>]*>(\\d+)</td><td[^>]*>(\\d+)</td><td[^>]*>(\\d+)</td>" );
    
	
	public KYJeffersonMS(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink, long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
            resultType = MULTIPLE_RESULT_TYPE;
	}


	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = null;
        TSServerInfoModule simTmp = null;

        // SET SERVER
        msiServerInfoDefault = new TSServerInfo(2);
        msiServerInfoDefault.setServerAddress( "http://msdlouky.org" );
        msiServerInfoDefault.setServerLink( "http://msdlouky.org" );
        msiServerInfoDefault.setServerIP( "msdlouky.org" );
        
        {//adress search
    		
        	/*
        	txtbn	
			txtsn	Brown
			cmds	Search
			h	768
			*/
        	
        	//(Request-Line)	POST http://msdlouky.org/msdwarrants/index.asp?flg=L HTTP/1.0
            simTmp = SetModuleSearchByAddress2(
                    6, 
                    msiServerInfoDefault, 
                    TSServerInfo.ADDRESS_MODULE_IDX, 
                    "/msdwarrants/index.asp?flg=L", 
                    TSConnectionURL.idPOST, 
                    "txtbn", 
                    "txtsn",
                    "txtdir",
                    "txtsuf");
            try
            {
            	PageZone searchByAddress = new PageZone("searchByAddress", "Address Search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(730), new Integer(250),HTMLObject.PIXELS , true);
                searchByAddress.setBorder(true);
                
                HTMLControl txtbn = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtdir", "", 1, 1, 1, 1, 20, "", simTmp.getFunction( 0 ), searchId );
                txtbn.setJustifyField( true );
                txtbn.setHiddenParam(true);
                searchByAddress.addHTMLObject( txtbn );

                HTMLControl txtsn = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtsn", "Street Name", 1, 1, 2, 2, 20, null, simTmp.getFunction( 1 ), searchId );
                txtsn.setJustifyField( true );
                txtsn.setRequiredExcl( true );
                txtsn.setRequiredCritical( true );
                txtsn.setFieldNote("");
                searchByAddress.addHTMLObject( txtsn );
                
                HTMLControl txtdir = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtbn", "", 1, 1, 1, 1, 20, "", simTmp.getFunction( 2 ), searchId );
                txtdir.setJustifyField( true );
                txtdir.setHiddenParam(true);
                searchByAddress.addHTMLObject( txtdir );
                
                HTMLControl txtsuf = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtsuf", "", 1, 1, 1, 1, 20, "", simTmp.getFunction( 3 ), searchId );
                txtsuf.setJustifyField( true );
                txtsuf.setHiddenParam(true);
                searchByAddress.addHTMLObject( txtsuf );
                
                HTMLControl cmds = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "cmds", "", 1, 1, 1, 1, 30, "Search", simTmp.getFunction( 4 ), searchId );
                cmds.setHiddenParam( true );
                searchByAddress.addHTMLObject( cmds);
                
                HTMLControl h = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "h", "", 1, 1, 1, 1, 30, "768", simTmp.getFunction( 5 ), searchId );
                h.setHiddenParam( true );
                searchByAddress.addHTMLObject( h);
              
                
                simTmp.setModuleParentSiteLayout( searchByAddress );
            }
            catch(Exception e){
            	e.printStackTrace();
            }
    	}
        
//      parcel id search
        {   //(Request-Line)    POST /propertymax/search_property.asp?go.x=1 HTTP/1.1
            /*simTmp = SetModuleSearchByParcelNo( 
                    4, 
                    msiServerInfoDefault, 
                    TSServerInfo.PARCEL_ID_MODULE_IDX, 
                    "/msdwarrants/index.asp?flg=L", 
                    TSConnectionURL.idPOST, 
                    "parcelid" );*/
        	SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
        	String parcelID = sa.getAtribute( SearchAttributes.LD_PARCELNO );
            if (parcelID!=null && parcelID.length() != 0){
    	        if (parcelID.length() == 12) {     	            //AO pid
    	            parcelID = parcelID.substring(0, 4);
    	        } else if (parcelID.length() == 14) {			//CnT pid
    	            parcelID = parcelID.substring(2, 6);
    	        }
            }
    	            	
	        simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.PARCEL_ID_MODULE_IDX, 4);
	        simTmp.setName("Parcel Number"); //it will be displayed in jsp
            simTmp.setDestinationPage("/msdwarrants/index.asp?flg=L");
            simTmp.setRequestMethod(TSConnectionURL.idPOST);
            simTmp.setParserID(ID_SEARCH_BY_PARCEL);
            simTmp.getFunction(0).setName("Parcel No:"); //it will be displayed in jsp
            simTmp.getFunction(0).setParamName("parcelid");
            
            try{
                PageZone searchByAddress = new PageZone("searchByAddress", "Tax-Block Search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(730), new Integer(250),HTMLObject.PIXELS , true);
                searchByAddress.setBorder(true);
                
                HTMLControl txtbn = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtbn", "Tax-Block Number ", 1, 1, 1, 1, 20, parcelID, simTmp.getFunction( 0 ), searchId );
                txtbn.setJustifyField( true );
                txtbn.setRequiredExcl( true );
                txtbn.setRequiredCritical( true );
                txtbn.setFieldNote("(first 4 digits of the PID from AO)");
                //txtbn.setHiddenParam(true);
                searchByAddress.addHTMLObject( txtbn );

                HTMLControl txtsn = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtsn", "Street Name", 1, 1, 2, 2, 20, "", simTmp.getFunction( 1 ), searchId );
                txtsn.setJustifyField( true );
                txtsn.setHiddenParam(true);
                searchByAddress.addHTMLObject( txtsn );
                
                HTMLControl cmds = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "cmds", "", 1, 1, 1, 1, 30, "Search", simTmp.getFunction( 2 ), searchId );
                cmds.setHiddenParam( true );
                searchByAddress.addHTMLObject( cmds);
                
                HTMLControl h = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "h", "", 1, 1, 1, 1, 30, "768", simTmp.getFunction( 3 ), searchId );
                h.setHiddenParam( true );
                searchByAddress.addHTMLObject( h);
                
                simTmp.setModuleParentSiteLayout( searchByAddress );
            }
            catch(Exception e){
                e.printStackTrace(System.err);
            }
        }
        
        msiServerInfoDefault.setupParameterAliases();
        setModulesForAutoSearch(msiServerInfoDefault);
        
        return msiServerInfoDefault;
        
	}
	
    protected void setModulesForAutoSearch(TSServerInfo serverInfo)
    {
        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

        TSServerInfoModule m;
        
        SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
        boolean validateWithDates = applyDateFilter();
        DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
        
        String parcelID = sa.getAtribute( SearchAttributes.LD_PARCELNO );
        if(parcelID!=null && parcelID.length() != 0){
	        if( parcelID.length() == 12 )
	        {
	            //AO pid
	            parcelID = parcelID.substring( 0, 4 );
	        }
	        else if( parcelID.length() == 14 )
	        {
	            //CnT pid
	            parcelID = parcelID.substring( 2, 6 );
	        }
	        
	        // search by PID
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
	        m.clearSaKey(0);
	        m.setParamValue(0, parcelID );
	        m.addFilter(PINFilterFactory.getDefaultPinFilter(searchId));
            if (validateWithDates) {
    			m.addValidator(recordedDateValidator);
    			m.addCrossRefValidator(recordedDateValidator);
    		}
	        l.add(m);
        }
   
        String streetName = sa.getAtribute( SearchAttributes.P_STREETNAME);
        if(streetName!=null && !streetName.replaceAll("\\s", "").equals("")){
	        // search by address
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	        m.setSaKey(1, SearchAttributes.P_STREETNAME );
	        m.clearSaKey(2);
	        m.addFilter(AddressFilterFactory.getAddressHighPassFilter(searchId, 0.85d));
            if (validateWithDates) {
    			m.addValidator(recordedDateValidator);
    			m.addCrossRefValidator(recordedDateValidator);
    		}
	        l.add(m);
        }
        
        serverInfo.setModulesForAutoSearch(l);
    }
    
    private String getInstrNoFromResponse(String response)
    {
        Matcher pidMatcher = pidPattern.matcher( response );
        
        if( pidMatcher.find() )
        {
            return pidMatcher.group( 1 ) + pidMatcher.group( 2 ) + pidMatcher.group( 3 ) + "_ms";
        }
        
        return "none";
    }
	
    protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException
    {
        String rsResponse = Response.getResult();
        String sTmp = CreatePartialLink(TSConnectionURL.idPOST);
        
        switch( viParseID )
        {
            case ID_DETAILS:
            case ID_SAVE_TO_TSD:
            case ID_SEARCH_BY_ADDRESS:
            case ID_SEARCH_BY_PARCEL:
            	
            	Matcher m = Pattern.compile("(?is)<b>(No records found.*?)<").matcher(rsResponse);
            	if(m.find()) {
            		Response.getParsedResponse().setError(m.group(1));
            	}
                
                String result = "";
                Vector parsedRows = new Vector();
                
                if( viParseID == ID_SAVE_TO_TSD )
                {
                    String pidNo = getInstrNoFromResponse( rsResponse );
                    
                    msSaveToTSDFileName = pidNo + ".html";
                    Response.getParsedResponse().setFileName(
                            getServerTypeDirectory() + msSaveToTSDFileName);
                    msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD(true);

                    rsResponse = addTable(rsResponse);
                    rsResponse = rsResponse.replaceAll( "(?s)<script.*?</script>", "" );
                    
                    ParsedResponse pr = Response.getParsedResponse();

                    parser.Parse(pr, rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
                    
                    pr.setOnlyResponse(rsResponse);
                }
                else
                {
                    Matcher resultsTableMatcher = resultsTable.matcher( rsResponse );
                    if( resultsTableMatcher.find() )
                    {
                        rsResponse = resultsTableMatcher.group( 0 );
                    }
                    else
                    {
                    	//Log.sendEmail("catalin@cst.ro", "KYJeffersonMS", rsResponse);
                    }
                    
                    rsResponse = rsResponse.replaceAll( "</?p>", "" );
                    rsResponse = rsResponse.replaceAll( "&nbsp;", " " );
                    rsResponse = rsResponse.replaceAll( "&nbsp", " " );
                    rsResponse = rsResponse.replaceAll( "(?i)<tr.*?>", "<tr>" );
                    rsResponse = rsResponse.replaceAll( "(?i)<font.*?>", "" );
                    rsResponse = rsResponse.replaceAll( "(?i)</font>", "" );
                    rsResponse = rsResponse.replaceAll( "(?s)<script.*?</script>", "" );
                    
                    //split the results
                    
                    Matcher subResultsMatcher = subresultsPattern.matcher( rsResponse );
                    
                    int rowNo = 0;
                    while( subResultsMatcher.find() )
                    {
                    	rowNo++;
                    	if(rowNo == 1){
                    		continue;
                    	}

                        String initialResponse = subResultsMatcher.group( 0 );
                        
                        String row = initialResponse;
                        
                        row = "<table cellpadding=\"4\" cellspacing=\"0\">" + row + "</table>";
                        
                        ParsedResponse pr = new ParsedResponse();
                        
                        if( row.indexOf( "<a href" ) >= 0 )
                        {
                            //header
                            row = row.replaceAll( "</td></tr>", "</td><td>&nbsp;</td></tr>" );
                            row = row.replaceAll( "(?i)onmouseover=\".*?\"", "" );
                            row = row.replaceAll( "(?i)<a .*?>", "" );
                            row = row.replaceAll( "(?i)</a>", "" );
                            parser.Parse(pr, row, Parser.NO_PARSE);
                        }
                        else
                        {
                            //result row
                            String pidNo = getInstrNoFromResponse( initialResponse );
                            
                            if( "none".equals( pidNo ) )
                            {
                                continue;
                            }
                            
                            String instrumentOriginalLink = sAction + "&" + Response.getQuerry() + "&dummyPid=" + pidNo;
                            
                            String sSave2TSDLink = sTmp + instrumentOriginalLink;
                            
                            if (FileAlreadyExist(pidNo + ".html") )
                            {
                                row += CreateFileAlreadyInTSD(true) + "<br>";
                            }
                            else
                            {
                                row = row.replaceAll( "</td></tr>", "</td><td><input type='radio' name='docLink' value='" 
                                  + sSave2TSDLink + "'></td></tr>" );
                                mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
                            }
                            row = row.replaceAll( "(?i)<a .*?>", "" );
                            row = row.replaceAll( "(?i)</a>", "" );
                            pr.setPageLink(new LinkInPage(sSave2TSDLink, instrumentOriginalLink, TSServer.REQUEST_SAVE_TO_TSD));
                            
                            row = addTable(row);
                            
                            parser.Parse(pr, row, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
                        }
                        
                        parsedRows.add(pr);
                        result += row;
                    }
                    
                    
                    //if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH)
                    //{
                        Response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST"));
                        
                        Response.getParsedResponse().setFooter(CreateSaveToTSDFormEnd(SAVE_DOCUMENT_BUTTON_LABEL, viParseID, -1));
                    //}
                    
                    parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
                    Response.getParsedResponse().setResultRows(parsedRows);
                }
                
                break;
        }
    }

    private String addTable(String html) {
    	
    	return
    	"<table border=1>" +
	    	"<tr>" +
	    		"<td><b>Tax-Block</b></td>" +
	    		"<td><b>Lot</b></td>" +
	    		"<td><b>Sublot</b></td>" +
	    		"<td><b>Property Address</b></td>" +
	    		"<td><b>Warrant #</b></td>" +
	    		"<td><b>Series</b></td>" +
	    		"<td><b>Face Value</b></td>" +
	    		"<td><b>Issued On</b></td>" +
	    		"<td><b>Warrant Holder</b></td>" +
	    		"<td><b>PVA Ref #</b></td>" +
	    	"</tr>" +
	    	html.replaceAll("<td>\\s+</td>", "<td>&nbsp;</td>") + 
    	"</table>";
    	
    }
	
}

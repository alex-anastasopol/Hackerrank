package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
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
import ro.cst.tsearch.utils.InstanceManager;

public class MIWayneTR extends TSServer {

	private static final long serialVersionUID = 2365499467770066299L;

	
	
	private boolean downloadingForSave;
	
	private Pattern parcelPattern = Pattern.compile( "(?is)Parcel\\s+ID" );
	
	private Pattern nextPagePattern = Pattern.compile( "(?is)<a  href=Find.asp\\?Page=(\\d+)><font size=\"3\"><IMG style=\"border: none\" src=\"./images/btn_next.gif\".*?</a>" );
	
	private Pattern prevPagePattern = Pattern.compile( "(?is)<a  href=Find.asp\\?Page=(\\d+)><font size=\"3\"><IMG  style=\"border: none\" src='./images/btn_prev.gif'.*?</a>" );
			
	public static String citySelect = "<OPTION  style='font: courier' value='30'>Allen Park&nbsp;(&nbsp;30&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='31'>Belleville&nbsp;(&nbsp;31&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='70'>Brownstown Twp&nbsp;(&nbsp;70&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='71'>Canton Twp&nbsp;(&nbsp;71&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='32'>Dearborn&nbsp;(&nbsp;32&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='33'>Dearborn Heights&nbsp;(&nbsp;33&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='01'>Detroit&nbsp;(&nbsp;01&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='34'>Ecorse&nbsp;(&nbsp;34&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='58'>Flat Rock&nbsp;(&nbsp;58&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='35'>Garden City&nbsp;(&nbsp;35&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='36'>Gibraltar&nbsp;(&nbsp;36&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='73'>Grosse Ile Twp&nbsp;(&nbsp;73&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='37'>Grosse Pointe City&nbsp;(&nbsp;37&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='38'>Grosse Pointe Farms&nbsp;(&nbsp;38&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='39'>Grosse Pointe Park&nbsp;(&nbsp;39&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='74'>Grosse Pointe Twp&nbsp;(&nbsp;74&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='40'>Grosse Pointe Woods&nbsp;(&nbsp;40&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='41'>Hamtramck&nbsp;(&nbsp;41&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='42'>Harper Woods&nbsp;(&nbsp;42&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='43'>Highland Park&nbsp;(&nbsp;43&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='75'>Huron Twp&nbsp;(&nbsp;75&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='44'>Inkster&nbsp;(&nbsp;44&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='45'>Lincoln Park&nbsp;(&nbsp;45&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='46'>Livonia&nbsp;(&nbsp;46&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='47'>Melvindale&nbsp;(&nbsp;47&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='48'>Northville City&nbsp;(&nbsp;48&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='77'>Northville Twp&nbsp;(&nbsp;77&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='49'>Plymouth City&nbsp;(&nbsp;49&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='78'>Plymouth Twp&nbsp;(&nbsp;78&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='79'>Redford Twp&nbsp;(&nbsp;79&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='50'>River Rouge&nbsp;(&nbsp;50&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='51'>Riverview&nbsp;(&nbsp;51&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='52'>Rockwood&nbsp;(&nbsp;52&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='80'>Romulus&nbsp;(&nbsp;80&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='53'>Southgate&nbsp;(&nbsp;53&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='81'>Sumpter Twp&nbsp;(&nbsp;81&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='60'>Taylor&nbsp;(&nbsp;60&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='54'>Trenton&nbsp;(&nbsp;54&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='83'>Van Buren Twp&nbsp;(&nbsp;83&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='55'>Wayne&nbsp;(&nbsp;55&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='56'>Westland&nbsp;(&nbsp;56&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='59'>Woodhaven&nbsp;(&nbsp;59&nbsp;)</OPTION>" +
					"<OPTION  style='font: courier' value='57'>Wyandotte&nbsp;(&nbsp;57&nbsp;)</OPTION>";
	
	public MIWayneTR(long searchId) {super(searchId); }

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIWayneTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId,int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId,mid);
	}

	public TSServerInfo getDefaultServerInfo()
    {
        TSServerInfo msiServerInfoDefault = null;
        TSServerInfoModule simTmp = null;
        
        msiServerInfoDefault = new TSServerInfo(2);
        
        msiServerInfoDefault.setServerAddress( "www.waynecounty.com" );

        msiServerInfoDefault.setServerLink( "http://www.waynecounty.com/pta/Disclaimer.asp" );

        msiServerInfoDefault.setServerIP( "www.waynecounty.com" );
        
            {
                //address Search
                {
                    simTmp = SetModuleSearchByAddress(6, msiServerInfoDefault, TSServerInfo.ADDRESS_MODULE_IDX, "/pta/Find.asp", TSConnectionURL.idPOST, "addno", "addname");
                   
                    try
                    {
                        PageZone searchByAddress = new PageZone("searchByAddress", "Address Search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                        searchByAddress.setBorder(true);

                        HTMLControl streetNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "addno", "Number", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                        streetNo.setJustifyField( true );
                        streetNo.setRequiredExcl( true );
                        searchByAddress.addHTMLObject( streetNo );
                        
                        HTMLControl streetName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "addname", "Street name", 1, 1, 2, 2, 30, null, simTmp.getFunction( 1 ), searchId );
                        streetName.setJustifyField( true );
                        streetName.setRequiredExcl( true );
                        searchByAddress.addHTMLObject( streetName );
                                                
                        HTMLControl municipality = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "mun", "Municipality(ID)", 1, 1, 3, 3, 30, null, simTmp.getFunction( 2 ), searchId );
                        municipality.setJustifyField( true );
                        searchByAddress.addHTMLObject( municipality );
                        
                        HTMLControl pid = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "pid", "", 1, 1, 1, 1, 30, null, simTmp.getFunction( 3 ), searchId );
                        pid.setHiddenParam( true );
                        searchByAddress.addHTMLObject( pid );
                        
                        HTMLControl searchx = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Search.x", "", 1, 1, 1, 1, 30, "49", simTmp.getFunction( 4 ), searchId );
                        searchx.setHiddenParam( true );
                        searchByAddress.addHTMLObject( searchx );
                        
                        HTMLControl searchy = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Search.y", "", 1, 1, 1, 1, 30, "3", simTmp.getFunction( 5 ), searchId );
                        searchy.setHiddenParam( true );
                        searchByAddress.addHTMLObject( searchy );
                        
                        //HTMLControl Search = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Search", "", 1, 1, 1, 1, 30, "Search", simTmp.getFunction( 6 ), searchId );
                        //Search.setHiddenParam( true );
                        //searchByAddress.addHTMLObject( Search );
                        
                        simTmp.setModuleParentSiteLayout( searchByAddress );
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                    
                    simTmp.getFunction( 2 ).setHtmlformat("<SELECT name=\"mun\">" +
                    		"<OPTION selected></OPTION>" +
                    		citySelect +
                    		"</SELECT>");

                }
                
                //pid search
                {
                	simTmp = SetModuleSearchByParcelNo(6, msiServerInfoDefault, TSServerInfo.PARCEL_ID_MODULE_IDX, "/pta/Find.asp", TSConnectionURL.idPOST, "pid" );
                    
                	try
                    {
                        PageZone searchByAddress = new PageZone("searchByAddress", "Parcel Id Search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                        searchByAddress.setBorder(true);

                        HTMLControl pid = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "pid", "Parcel ID", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                        pid.setJustifyField( true );
                        pid.setRequiredExcl( true );
                        searchByAddress.addHTMLObject( pid );
                        
                        HTMLControl streetNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "addno", "Number", 1, 1, 1, 1, 30, null, simTmp.getFunction( 1 ), searchId );
                        streetNo.setHiddenParam( true );
                        searchByAddress.addHTMLObject( streetNo );
                        
                        HTMLControl streetName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "addname", "Street name", 1, 1, 2, 2, 30, null, simTmp.getFunction( 2 ), searchId );
                        streetName.setHiddenParam( true );
                        searchByAddress.addHTMLObject( streetName );
                                                
                        HTMLControl municipality = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "mun", "Municipality(ID)", 1, 1, 3, 3, 30, "", simTmp.getFunction( 3 ), searchId );
                        municipality.setHiddenParam( true );
                        searchByAddress.addHTMLObject( municipality );
                        
                        HTMLControl searchx = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Search.x", "", 1, 1, 1, 1, 30, "49", simTmp.getFunction( 4 ), searchId );
                        searchx.setHiddenParam( true );
                        searchByAddress.addHTMLObject( searchx );
                        
                        HTMLControl searchy = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Search.y", "", 1, 1, 1, 1, 30, "3", simTmp.getFunction( 5 ), searchId );
                        searchy.setHiddenParam( true );
                        searchByAddress.addHTMLObject( searchy );
                        
                        //HTMLControl Search = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Search", "", 1, 1, 1, 1, 30, "Search", simTmp.getFunction( 6 ), searchId );
                        //Search.setHiddenParam( true );
                        //searchByAddress.addHTMLObject( Search );
                        
                        simTmp.setModuleParentSiteLayout( searchByAddress );
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                }

                msiServerInfoDefault.setupParameterAliases();
                setModulesForAutoSearch( msiServerInfoDefault );
            }
                    
        return msiServerInfoDefault;
    }
	
	 protected String getFileNameFromLink(String link) {
		 return org.apache.commons.lang.StringUtils.substringBetween(link, "id=", "&") + ".html";
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		
		if(rsResponse.contains("The search produced no results")){
			return; 
		}
		// check if we have received an error
		
		String initialResponse = rsResponse;
		
		String keyNumber = "";
		
		String sTmp;
		
		int istart, iend;
			
		switch( viParseID ){
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:
				
				String linkNext = "";
				String linkPrev = "";
				String linkNextForAutomatic = "";
				
				if( rsResponse.indexOf( "Property and Tax Information" ) >= 0 ){
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}

				sTmp = CreatePartialLink(TSConnectionURL.idGET);
				
				Matcher nextLinkMatcher = nextPagePattern.matcher( rsResponse );
				if( nextLinkMatcher.find() ){
					linkNextForAutomatic = "<a href=\"" + sTmp + "/pta/Find.asp&Page=" + nextLinkMatcher.group( 1 ) + "\">Next</a>";
					linkNext = "<a href=\"" + sTmp + "/pta/Find.asp&Page=" + nextLinkMatcher.group( 1 ) + "\">Next</a>";
				}
				
				Matcher prevLinkMatcher = prevPagePattern.matcher( rsResponse );
				if( prevLinkMatcher.find() ){
					linkPrev = "<a href=\"" + sTmp + "/pta/Find.asp&Page=" + prevLinkMatcher.group( 1 ) + "\">Previous</a>";;
				}
				
				istart = rsResponse.indexOf( "<TABLE");
				istart = rsResponse.indexOf( "<TABLE" , istart + 1);
//				istart = rsResponse.indexOf( "<TABLE" , istart + 1);
				
				iend = rsResponse.indexOf( "</TABLE>", istart + 1 );
				
				if( istart < 0 || iend < 0 ){
					return;
				}
				
				rsResponse = rsResponse.substring( istart , iend + 8 );
				
				rsResponse = rsResponse.replaceAll( "(?is)<TR[^>]*>" , "<TR>" );
				rsResponse = rsResponse.replaceAll( "(?is)<TD[^>]*>" , "<TD>" );
				
				
				
				rsResponse = rsResponse.replaceAll( "(?is)<a href='Details.asp\\?id=([^']*)'>" , "<a href='" + sTmp + "/pta/Details.asp&id=$1'>" );
				
				if( !"".equals( linkPrev ) ){
					rsResponse += "<br>" + linkPrev;
				}
				
				if( !"".equals( linkNext ) ){
					rsResponse += "<br>" + linkNext;
				}
				
				parser.Parse( Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				
				if( !"".equals( linkNextForAutomatic ) ){
					Response.getParsedResponse().setNextLink( linkNextForAutomatic );
				}
				
				break;
			case ID_DETAILS:
				
				istart = rsResponse.indexOf( "<table" );
//				iend = rsResponse.lastIndexOf( "</Table>" );
				iend = rsResponse.lastIndexOf( "</TR>" );
				iend = rsResponse.indexOf( "</table>" , iend);
				
				if( istart < 0 || iend < 0 ){
					return;
				}
				
				rsResponse = rsResponse.substring( istart , iend + 8 );
				
				rsResponse = rsResponse.replaceAll( "(?is)<img[^>]*>" , "" );
				
				Matcher findParcelMatcher = parcelPattern.matcher( rsResponse );
				if( findParcelMatcher.find() ){
					int keyStart = findParcelMatcher.start();
					
					keyStart = rsResponse.indexOf( "<TR", keyStart + 1 );
					keyStart = rsResponse.indexOf( "<TD", keyStart + 1 );
					keyStart = rsResponse.indexOf( "<TD", keyStart + 1 );
					keyStart = rsResponse.indexOf( "&nbsp;", keyStart + 1 );
					
					keyNumber = rsResponse.substring( keyStart + 6, rsResponse.indexOf( "</" , keyStart + 1));
					
					keyNumber = keyNumber.replaceAll( "\\." , "" );
				}
				
				if( !downloadingForSave ){
            		//not saving to TSR
            		
    				String qry = Response.getQuerry();
    				qry = "dummy=" + keyNumber + "&" + qry;
    				Response.setQuerry(qry);
    				String originalLink = sAction + "&" + qry;
                    String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + originalLink;

    				if (FileAlreadyExist(keyNumber + ".html") ) {
    					rsResponse += CreateFileAlreadyInTSD();
    				} else {
    					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
    					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
    				}

    				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
    				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
            	}
            	else{
            		//saving
                    msSaveToTSDFileName = keyNumber + ".html" ;
                    Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                    msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
                    
                    rsResponse = rsResponse.replaceAll("(?i)(<td valign=\\w+ [^>]*)valign=\\w+>", "$1>"); // remove a "valign" attribute when found twice in one TD            		
                    parser.Parse(
                        Response.getParsedResponse(),
                        rsResponse,
                        Parser.PAGE_DETAILS,
                        getLinkPrefix(TSConnectionURL.idGET),
                        TSServer.REQUEST_SAVE_TO_TSD);
            	}
				
				break;
			case ID_SAVE_TO_TSD:
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
			case ID_GET_LINK:
				if( sAction.indexOf( "Details" ) >= 0 ){
					ParseResponse(sAction, Response, ID_DETAILS);	
				}
				else{
					ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				}
				
				break;
			default:
				break;
		}
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext().getSa();
		
		boolean emptyPid = "".equals( sa.getAtribute( SearchAttributes.LD_PARCELNO ) );
		boolean emptyAddress = "".equals( sa.getAtribute( SearchAttributes.P_STREETNAME ) );
		
		String cityName = sa.getAtribute( SearchAttributes.P_CITY );
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		//search by pid
		if( !emptyPid ){
            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
            l.add( m );
		}
		
		//search by address
		if( !emptyAddress ){
			//search with city
			if( !"".equals( cityName ) ){
				Pattern cityPattern = Pattern.compile( "value=\'([^\']*)\'>" + cityName + "&nbsp;" );
				Matcher findCity = cityPattern.matcher( citySelect );
				if( findCity.find() ){
					 m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.ADDRESS_MODULE_IDX ) );
					 m.getFunction( 0 ).setSaKey(SearchAttributes.P_STREETNO);
					 m.getFunction( 1 ).setSaKey(SearchAttributes.P_STREETNAME);
					 m.getFunction( 2 ).setParamValue( findCity.group( 1 ) );
					 m.addFilter(addressFilter);
					 l.add( m );
				}
			}
			
			//search without city
			 m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.ADDRESS_MODULE_IDX ) );
			 m.getFunction( 0 ).setSaKey(SearchAttributes.P_STREETNO);
			 m.getFunction( 1 ).setSaKey(SearchAttributes.P_STREETNAME);
			 m.addFilter(addressFilter);
			 l.add( m );
		}
		
		serverInfo.setModulesForAutoSearch(l);
	}
	
	
	public static void splitResultRows( Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException
    {
    	p.splitResultRows( pr, htmlString, pageId, "<TR", "</TABLE>", linkStart, action);
		// remove table header
		Vector rows = pr.getResultRows();
		if (rows.size() > 0) {
			ParsedResponse firstRow = (ParsedResponse) rows.remove(0);
			pr.setResultRows(rows);
			pr.setHeader(pr.getHeader() + firstRow.getResponse());
		}    	
    }    
}

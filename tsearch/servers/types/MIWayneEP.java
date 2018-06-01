package ro.cst.tsearch.servers.types;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.StringUtils;


public class MIWayneEP  extends TSServer{

	
	private static final int       AO_DOCUMENT = 100;
	private static final int       EP_DOCUMENT = 200;
	
	private final static String TAX_LINK_IDENTIFICATION = "Current Tax";
	private final static String INTERMEDIAR_RESULTS_IDENTIFICATION = "Search Results From";
	private final static String FINAL_DOCUMENT_IDENTIFICATION = "General Property Information";
	private final static String FINAL_TAX_DOCUMENT_IDENTIFICATION ="Detailed Tax Information";
	private final static String FINAL_DOCUMENT_START_SECV = "Please confirm you wish to continue";
	private final static String  CONTENT_END = "<!-- Content End -->";
	
	private final static String CLEAN_FINAL_RESULT_END = "GeneralAssgDetails_CheckBoxBuilding";
	
	private static final long serialVersionUID = 1237433567887747875L;

	private static final Pattern patStartTable = Pattern.compile("(?i)<[ ]*table.*\"searchList_DataGridRecords\"");

	private static final Pattern patStopTable = Pattern.compile("(?i)<tr[ ]+class=[ ]*\"SetupListFooterStyle\"");
	
	static public final  Pattern patPostdetailParameters = Pattern.compile("(?i)(<[ \t\r\n]*input[^>]+name[ \t\r\n]*=[ \t\r\n]*\"([^\"]+)\".*?)((value[ \n\r\t]*=[\n\t\r]*\"([^\"]*)\"[^>]*)|([>]))");
	
	private static final Pattern patNextPrevLink = Pattern.compile("(?i)(<a href=\"java[^_]*__doPostBack[(]'(searchList[$]DataGridRecords[$]_ctl[0-9]*[$]_ctl([0-9]*))',''[)]\">[^<]*</a>[^<]*)|(<span>[1-9][0-9]*</span>)");
	
	private static final Pattern patCurrentForm = Pattern.compile("(?i)<[ ]*form[ ]+name[ ]*=[ ]*\"[^\"]*\" method=\"post\" action=\"([^\"]*)\" id=\"[^\"]*\"");
	
	private static final Pattern parcelPattern = Pattern.compile("(?i)<[ \t\n\r]*font[ \n\t\r]*class[ \t\n\r]*=[ \t\r\n]*[\"]?[']?DetailPageDetails[\"]?[']?>[^>]*>Parcel[^>]*>([0-9.-]*)<[^/]*/font[ \t\n\r]*>");
	
	private static final Pattern patCurrentTax = Pattern.compile("(?i)<[ \t\r\n]*td[^;]*;location.href[ \t\n\r]*=[^']*'([^']*)'[^>]*>"+TAX_LINK_IDENTIFICATION+"<[\n\t\r ]*/[\n\t\r ]*td[\n\t\r ]*>");
	
	private static final Pattern patSumary = Pattern.compile("(?i)<td[^>]*>[^<]*</td>[ \r\t\n]*<td[^>]*>[$,0-9.]*</td>[ \r\t\n]*<td[^>]*>[$,0-9.]*</td><td[^>]*>[^<]*</td>[ \r\n\t]*<td[^>]*>[$,0-9.]*</td><td[^>]*>.*?</td>");
	
	private static final Pattern patStartTaxTable = Pattern.compile("(?i)<tr[^>]*>[ \t\n\r]*<td>[^<]*</td>[ \t\n\r]*<td>[ \t\n\r]*<[^>]*>Year[ ]*/[ ]*Season</font>[ \t\n\r]*</td><td[^>]*><font[^>]*>Total[^<]*</font>[ \t\r\n]*[^>]*>[^>]*>[^>]*>Total[^<]*</font>[ \n\t\r]*</td>[ \t\r\n]*[^>]*>[^>]*>Last[^<]*</font>([^>]*>){3}Total[^<]*</font>([^>]*>){3}[ \t\n\r]*</tr>");
	
	private boolean downloadingForSave;
	
	public MIWayneEP(long searchId){
		super(searchId);
	}
	
	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIWayneEP(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId,int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId,mid);
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		
		String initialResponse = rsResponse;
	
		String imgLink = null;
		
		String keyNumber = "";
		
		int istart=0, iend=rsResponse.length();
	 
		Matcher mat  = null;
		if(viParseID !=ID_DETAILS){
			mat =ro.cst.tsearch.connection.http.MIWayneEP.patPostHiddenParameters.matcher(rsResponse);
		}
		
		Matcher matForm = patCurrentForm.matcher(rsResponse);
		String local__EVENTTARGET="";
		String local__EVENTARGUMENT="";
		String local__VIEWSTATE="";
		ro.cst.tsearch.connection.http.MIWayneEP site =  (ro.cst.tsearch.connection.http.MIWayneEP)HTTPSiteManager.pairHTTPSiteForTSServer("MIWayneYA", searchId,miServerID);
		
		if(mat!=null){
			while(mat.find()){
				String name = mat.group(1);
				String value = mat.group(2);
			    if(value ==null){
			    	value ="";
			    }
				if("__EVENTTARGET".equals(name)){
					local__EVENTTARGET = value;	
				}
				else if("__EVENTARGUMENT".equals(name)){
					local__EVENTARGUMENT = value;
				}
				else if("__VIEWSTATE".equals(name)){
					local__VIEWSTATE = value;
				}
			}
		}
		
		String str1 =rsResponse;
		switch( viParseID ){
		
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_NAME:
				
				String linkNext = "";
				String linkPrev = "";
				String linkNextForAutomatic = "";
				
				Matcher matNext = patNextPrevLink.matcher(rsResponse);
				
				int curentPage=-1;
				Vector<Integer> otherPages = new Vector<Integer>();
				HashMap<Integer,String> mapPages = new HashMap<Integer,String> ();
				while(matNext.find()){
					try{
						System.err.println(matNext.group());
						String current = matNext.group();
						if(current .contains("span")){
							String number = current.replaceAll( "[\t\n\r span/<>]","");
							curentPage = Integer.parseInt(number);
						}
						else{
										String number = matNext.group(3);
										otherPages.add( Integer.parseInt(number) );
										String value = matNext.group(2);
										mapPages.put(  Integer.parseInt(number), value.replaceAll("[$]", ":") );
						}
					}
					catch(Exception e){
						e.printStackTrace();
						continue;
					}
				}
				
				 site.set__EVENTTARGET_PARTIAL_PREV(local__EVENTTARGET);
				 site.set__EVENTTARGET_PARTIAL_NEXT(local__EVENTTARGET);
				 site.set__EVENTARGUMENT_PARTIAL(local__EVENTARGUMENT)	;
				 site.set__VIEWSTATE_PARTIAL(local__VIEWSTATE)	;
				 
				String sTmp = CreatePartialLink(TSConnectionURL.idPOST);
				String sTmpGet = CreatePartialLink(TSConnectionURL.idGET);
				
				Matcher matStartTable = patStartTable.matcher(rsResponse);
				
				if(matStartTable .find()){
					istart = matStartTable.start();
					rsResponse  = rsResponse.substring(istart);
				}
				
				Matcher matStopTable = patStopTable.matcher(rsResponse);
				
				if(matStopTable .find()){
					iend = matStopTable.start();
					rsResponse  = rsResponse.substring(0,iend);
				}
	
				rsResponse  = rsResponse.substring(0,iend);
				rsResponse = rsResponse +"</table>";
				
				if(matForm.find()){
					  if (hasNextLink(curentPage,otherPages)){
								String url = matForm.group(1);
								if(url .contains("from")){
									url  = url.replace("__prev__","__next__" );
								}
								else{
									url = url+"&from=__next__";
								}
								site.set__EVENTTARGET_PARTIAL_NEXT(mapPages.get(curentPage));
								url = url.replaceAll( "\\?" , "&");
								linkNext = ("<a href=\"" + sTmp + "/bsa.is/AssessingServices/"+url+"\">Next</a>").replaceAll("&amp;","&");
								linkNextForAutomatic = linkNext;
								rsResponse += "<br>" + linkNext;
						}
						
						if (hasPreviousLink(curentPage,otherPages)){
							String url = matForm.group(1);
							
							if(url .contains("from")){
								url  = url.replace("__next__","__prev__" );
							}
							else{
								url = url+"&from=__prev__";
							}
							url = url.replaceAll( "\\?" , "&");
							site.set__EVENTTARGET_PARTIAL_PREV(mapPages.get(curentPage-2));
							linkPrev = ("<a href=\"" + sTmp + "/bsa.is/AssessingServices/"+url+"\">Previous</a>").replaceAll("&amp;","&");
							rsResponse += "<br>" + linkPrev;
						}
				}
				
				rsResponse = rsResponse.replaceAll("(?i)(<tr[^>]*>[^<]*<td[^<]*<A[ ]+href[ ]*=')([^\\?]*)\\?([^']*'[ ]*>[0-9.]*([^<]*<){10})","$1"+sTmpGet+"$2"+"&"+"$3");
								
				rsResponse = rsResponse.replaceAll( "(?is)align=\"Left\"" , " ");
				
				parser.Parse( Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				
				if( !"".equals( linkNextForAutomatic ) ){
					Response.getParsedResponse().setNextLink( linkNextForAutomatic );
				}	
			break;
			
			case ID_DETAILS:
				
				str1 = obtainFinalDocument(rsResponse,site);
				
				keyNumber = getParcelNumber(str1);
				
//				str1 = str1.replaceAll( "(?is)<style>.*?</style>" , "" );
//				str1 = str1.replaceAll( "(?is)<script[^>]*>.*?</script>" , "" );
//				str1 = str1.replaceAll( "(?is)<font[^>]*>" , "" );
//				str1 = str1.replaceAll( "(?is)</font>" , "" );
//				str1 = str1.replaceAll( "(?is)</b>" , "" );
//				str1 = str1.replaceAll( "(?is)<b>" , "" );						
				str1 = str1.replaceAll( "(?is)</tr>\\s*<table" , "</tr><tr><td><table" );
				
				if( !downloadingForSave ){
					String qry = Response.getQuerry();
					qry = "dummy=" + keyNumber + "&" + qry;
					Response.setQuerry(qry);
					String originalLink = sAction + "&" + qry;
	                String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + originalLink;
	
					if (FileAlreadyExist(keyNumber + ".html") ) {
						str1 += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
						str1 = addSaveToTsdButton(str1, sSave2TSDLink, viParseID);
					}
	
					parser.Parse(Response.getParsedResponse(), str1, Parser.NO_PARSE);
				}
				else {
					msSaveToTSDFileName = keyNumber + ".html" ;
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = str1 + CreateFileAlreadyInTSD();
	                if (imgLink != null){
	                    Response.getParsedResponse().addImageLink(new ImageLinkInPage (imgLink, keyNumber + ".tiff" ));
	                }

	               parser.Parse(
	                        Response.getParsedResponse(),
	                        str1,
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
					if (  rsResponse.contains(INTERMEDIAR_RESULTS_IDENTIFICATION)  ){
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);	
					}
					if( rsResponse.contains(FINAL_DOCUMENT_START_SECV) || rsResponse.contains(FINAL_DOCUMENT_IDENTIFICATION) ){
						ParseResponse(sAction, Response, ID_DETAILS);
					}
			break;
				
			default:
				break;
		}
		
	}
	
	 protected String getFileNameFromLink(String link)
	 {
		 String parcelId = org.apache.commons.lang.StringUtils.getNestedString(link,"dp=","&");
		 return parcelId + ".html";
	}
	
	String citySelect="";
	public TSServerInfo getDefaultServerInfo()
    {
		//HTTPSite site =  (ro.cst.tsearch.connection.http.MIWayneEP)HTTPSiteManager.pairHTTPSiteForTSServer("MIWayneEP", searchId);
        TSServerInfo msiServerInfoDefault = null;
        TSServerInfoModule simTmp = null;
  
        msiServerInfoDefault = new TSServerInfo(3);
        
        msiServerInfoDefault.setServerAddress( "https://is.bsasoftware.com" );

        msiServerInfoDefault.setServerLink( "https://is.bsasoftware.com/bsa.is/login.aspx" );

        msiServerInfoDefault.setServerIP( "https://is.bsasoftware.com" );
        
            {
                //address Search
                {
                    simTmp = SetModuleSearchByAddress(14, msiServerInfoDefault, TSServerInfo.ADDRESS_MODULE_IDX, "/bsa.is/AssessingServices/ServiceAssessingSearch.aspx", TSConnectionURL.idPOST, "StreetFrom", "TextBoxStreetName_sna");
                   
                    try
                    {
                        PageZone searchByAddress = new PageZone("searchByAddress", "Search by address", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                        searchByAddress.setBorder(true);

                        HTMLControl streetNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetFrom", "Number", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                        streetNo.setJustifyField( true );
                        streetNo.setRequiredExcl( true );
                        searchByAddress.addHTMLObject( streetNo );
                        
                        HTMLControl streetName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxStreetName_sna", "Street name", 1, 1, 2, 2, 30, null, simTmp.getFunction( 1 ), searchId );
                        streetName.setJustifyField( true );
                        streetName.setRequiredExcl( true );
                        searchByAddress.addHTMLObject( streetName );
                                                
                        HTMLControl streetDir = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxStreetDir_sd", "Street Dir", 1, 1, 3, 3, 30, null, simTmp.getFunction( 2 ), searchId );
                        streetDir.setJustifyField( true );
                        searchByAddress.addHTMLObject( streetDir );
                        
                        HTMLControl to = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetTo", "To", 1, 1, 4, 4, 30, null, simTmp.getFunction( 3 ), searchId );
                        to.setJustifyField( true );
                        searchByAddress.addHTMLObject( to );
                        
                        
                        HTMLControl streetNoP = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetFrom_p", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 4 ), searchId );
                        streetNoP.setHiddenParam( true );
                        searchByAddress.addHTMLObject( streetNoP );
                        
                        HTMLControl toP = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetTo_p", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 5 ), searchId );
                        toP.setHiddenParam( true );
                        searchByAddress.addHTMLObject( toP);
                        
                        HTMLControl searchy = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "MaskedParcelNumber", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 6 ), searchId );
                        searchy.setHiddenParam( true );
                        searchByAddress.addHTMLObject( searchy );
                        
                        HTMLControl Search = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "MaskedParcelNumber_p", "", 1, 1, 1, 1, 30,
                        		URLDecoder.decode("%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02", "UTF-8"), simTmp.getFunction( 7 ), searchId );
                        Search.setHiddenParam( true );
                        searchByAddress.addHTMLObject( Search );
                        
                        HTMLControl Search1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxOwnerName_on", "", 1, 1, 1, 1, 30,"", simTmp.getFunction( 8), searchId );
                        Search1.setHiddenParam( true );
                        searchByAddress.addHTMLObject( Search1 );
                        
                        HTMLControl Search2 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "RadioButtonRecordCount", "", 1, 1, 1, 1, 30,"50", simTmp.getFunction(9 ), searchId );
                        Search2.setHiddenParam( true );
                        searchByAddress.addHTMLObject( Search2 );
                        
                        HTMLControl Search3 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ButtonSearchRecords", "", 1, 1, 1, 1, 30,"Search", simTmp.getFunction(10 ), searchId );
                        Search3.setHiddenParam( true );
                        searchByAddress.addHTMLObject( Search3 );
                        
                        HTMLControl Search4 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTTARGET", "", 1, 1, 1, 1, 30,"", simTmp.getFunction(11 ), searchId );
                        Search4.setHiddenParam( true );
                        searchByAddress.addHTMLObject( Search4 );
                        
                        HTMLControl Search5 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTARGUMENT", "", 1, 1, 1, 1, 30,"", simTmp.getFunction(12 ), searchId );
                        Search5.setHiddenParam( true );
                        searchByAddress.addHTMLObject( Search5 );
                        
                        HTMLControl Search6 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATE", "", 1, 1, 1, 1, 30,
                        		"dDw5NjE0NDY0OTQ7dDw7bDxpPDA+Oz47bDx0PDtsPGk8MTk+Oz47bDx0PDtsPGk8MD47aTwxPjtpPDI+O2k8Mz47aTw0PjtpPDU+O2k8Nz47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPGw8Ymdjb2xvcjtzdHlsZTs+O2w8I0YxRjFGMTtCT1JERVItVE9QOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1MRUZUOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1SSUdIVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItQk9UVE9NOiAjQjhEM0VCIDFweCBzb2xpZFw7IEhFSUdIVDogNXB4XDsgQkFDS0dST1VORC1QT1NJVElPTjogY2VudGVyXDsgQkFDS0dST1VORC1JTUFHRTogdXJsKC9ic2EuaXMvUGFnZUJhc2UvRGV0YWlsQm94SW1hZ2UuYXNweD9iZ2NvbG9yPUYxRjFGMSZoZWlnaHQ9MjApXDs7Pj47Oz47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwxPjtpPDM+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPDtwPGw8b25rZXlkb3duOz47bDxmblRyYXBLRChCdXR0b25TZWFyY2hSZWNvcmRzLGV2ZW50KTs+Pj47Oz47Pj47Pj47dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDw7cDxsPG9ua2V5ZG93bjs+O2w8Zm5UcmFwS0QoQnV0dG9uU2VhcmNoUmVjb3JkcyxldmVudCk7Pj4+Ozs+Oz4+Oz4+Oz4+Oz4+Oz4+O3Q8O2w8aTwwPjs+O2w8dDxwPGw8Ymdjb2xvcjtzdHlsZTs+O2w8I0YxRjFGMTtCT1JERVItVE9QOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1MRUZUOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1SSUdIVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItQk9UVE9NOiAjQjhEM0VCIDFweCBzb2xpZFw7IEhFSUdIVDogNXB4XDsgQkFDS0dST1VORC1QT1NJVElPTjogY2VudGVyXDsgQkFDS0dST1VORC1JTUFHRTogdXJsKC9ic2EuaXMvUGFnZUJhc2UvRGV0YWlsQm94SW1hZ2UuYXNweD9iZ2NvbG9yPUYxRjFGMSZoZWlnaHQ9MjApXDs7Pj47Oz47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwxPjtpPDI+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPHA8bDxQO0U7PjtsPENDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDOwICAgICAgICAgICAgICAgICAgICAgICAgICOz4+Oz47Oz47Pj47Pj47dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDs+O2w8XGU7Pj47Pjs7Pjs+Pjs+Pjs+Pjs+Pjs+Pjt0PDtsPGk8MD47PjtsPHQ8cDxsPGJnY29sb3I7c3R5bGU7PjtsPCNGMUYxRjE7Qk9SREVSLVRPUDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItTEVGVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItUklHSFQ6ICNCOEQzRUIgMXB4IHNvbGlkXDsgQk9SREVSLUJPVFRPTTogI0I4RDNFQiAxcHggc29saWRcOyBIRUlHSFQ6IDVweFw7IEJBQ0tHUk9VTkQtUE9TSVRJT046IGNlbnRlclw7IEJBQ0tHUk9VTkQtSU1BR0U6IHVybCgvYnNhLmlzL1BhZ2VCYXNlL0RldGFpbEJveEltYWdlLmFzcHg/Ymdjb2xvcj1GMUYxRjEmaGVpZ2h0PTIwKVw7Oz4+Ozs+Oz4+O3Q8O2w8aTwwPjs+O2w8dDw7bDxpPDE+Oz47bDx0PDtsPGk8MT47aTwyPjs+O2w8dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDw7cDxsPG9ua2V5ZG93bjs+O2w8Zm5UcmFwS0QoQnV0dG9uU2VhcmNoUmVjb3JkcyxldmVudCk7Pj4+Ozs+Oz4+Oz4+O3Q8O2w8aTwxPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7PjtsPFxlOz4+Oz47Oz47Pj47Pj47Pj47Pj47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8Nj47aTw4Pjs+O2w8dDxwPHA8bDxCYWNrQ29sb3I7Qm9yZGVyQ29sb3I7XyFTQjs+O2w8MjwwLCAyNTUsIDI1NSwgMjU1PjsyPDAsIDEwNywgMTIyLCAxNjU+O2k8MjQ+Oz4+Oz47Oz47dDxwPHA8bDxCYWNrQ29sb3I7Qm9yZGVyQ29sb3I7XyFTQjs+O2w8MjwwLCAyNTUsIDI1NSwgMjU1PjsyPDAsIDEwNywgMTIyLCAxNjU+O2k8MjQ+Oz4+Oz47Oz47Pj47Pj47Pj47Pj47Pj47bDxCYW5uZXJvQnV0dG9ubztFeHBhbmRvQ29sbGFwc29CdXR0b25vO1N0cmVldEZyb207U3RyZWV0VG87TWFza2VkUGFyY2VsTnVtYmVyOz4+C0ORdnEA2i7LJfrCb5gyWeBSHts="
                        		, simTmp.getFunction(13 ), searchId );
                        Search6.setHiddenParam( true );
                        searchByAddress.addHTMLObject( Search6 );
                        
                        
                        simTmp.setModuleParentSiteLayout( searchByAddress );
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }

                }
                
                
                //pid search
                {
                	simTmp = SetModuleSearchByParcelNo(14, msiServerInfoDefault, TSServerInfo.PARCEL_ID_MODULE_IDX, "/bsa.is/AssessingServices/ServiceAssessingSearch.aspx", TSConnectionURL.idPOST, "MaskedParcelNumber" );
                    
                	try
                    {
                        PageZone searchByParcelID = new PageZone("searchByParcelID", "Search by Parcel ID", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                        searchByParcelID.setBorder(true);

                        HTMLControl pid = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "MaskedParcelNumber", "Parcel ID", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                        pid.setJustifyField( true );
                        pid.setRequiredExcl( true );
                        searchByParcelID.addHTMLObject( pid );
                        
                        HTMLControl streetNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetFrom", "Number", 1, 1, 1, 1, 30, "", simTmp.getFunction(1 ), searchId );
                        streetNo.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( streetNo );
                        
                        HTMLControl streetName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxStreetName_sna", "Street name", 1, 1, 2, 2, 30, null, simTmp.getFunction( 2 ), searchId );
                        streetName.setHiddenParam(true);
                        searchByParcelID.addHTMLObject( streetName );
                                                
                        HTMLControl streetDir = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxStreetDir_sd", "Street Dir", 1, 1, 3, 3, 30, null, simTmp.getFunction( 3 ), searchId );
                        streetDir.setHiddenParam(true);
                        searchByParcelID.addHTMLObject( streetDir );
                        
                        HTMLControl to = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetTo", "To", 1, 1, 4, 4, 30, null, simTmp.getFunction( 4 ), searchId );
                        to.setHiddenParam(true);
                        searchByParcelID.addHTMLObject( to );
                        
                        
                        HTMLControl streetNoP = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetFrom_p", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 5 ), searchId );
                        streetNoP.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( streetNoP );
                        
                        HTMLControl toP = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetTo_p", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 6 ), searchId );
                        toP.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( toP);
                        
                        HTMLControl Search = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "MaskedParcelNumber_p", "", 1, 1, 1, 1, 30,
                        		URLDecoder.decode("%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02", "UTF-8"), simTmp.getFunction( 7 ), searchId );
                        Search.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( Search );
                        
                        HTMLControl Search1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxOwnerName_on", "", 1, 1, 1, 1, 30,"", simTmp.getFunction( 8), searchId );
                        Search1.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( Search1 );
                        
                        HTMLControl Search2 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "RadioButtonRecordCount", "", 1, 1, 1, 1, 30,"50", simTmp.getFunction(9 ), searchId );
                        Search2.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( Search2 );
                        
                        HTMLControl Search3 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ButtonSearchRecords", "", 1, 1, 1, 1, 30,"Search", simTmp.getFunction(10 ), searchId );
                        Search3.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( Search3 );
                        
                        HTMLControl Search4 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTTARGET", "", 1, 1, 1, 1, 30,"", simTmp.getFunction(11 ), searchId );
                        Search4.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( Search4 );
                        
                        HTMLControl Search5 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTARGUMENT", "", 1, 1, 1, 1, 30,"", simTmp.getFunction(12 ), searchId );
                        Search5.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( Search5 );
                        
                        HTMLControl Search6 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATE", "", 1, 1, 1, 1, 30,
                        		"dDw5NjE0NDY0OTQ7dDw7bDxpPDA+Oz47bDx0PDtsPGk8MTk+Oz47bDx0PDtsPGk8MD47aTwxPjtpPDI+O2k8Mz47aTw0PjtpPDU+O2k8Nz47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPGw8Ymdjb2xvcjtzdHlsZTs+O2w8I0YxRjFGMTtCT1JERVItVE9QOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1MRUZUOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1SSUdIVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItQk9UVE9NOiAjQjhEM0VCIDFweCBzb2xpZFw7IEhFSUdIVDogNXB4XDsgQkFDS0dST1VORC1QT1NJVElPTjogY2VudGVyXDsgQkFDS0dST1VORC1JTUFHRTogdXJsKC9ic2EuaXMvUGFnZUJhc2UvRGV0YWlsQm94SW1hZ2UuYXNweD9iZ2NvbG9yPUYxRjFGMSZoZWlnaHQ9MjApXDs7Pj47Oz47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwxPjtpPDM+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPDtwPGw8b25rZXlkb3duOz47bDxmblRyYXBLRChCdXR0b25TZWFyY2hSZWNvcmRzLGV2ZW50KTs+Pj47Oz47Pj47Pj47dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDw7cDxsPG9ua2V5ZG93bjs+O2w8Zm5UcmFwS0QoQnV0dG9uU2VhcmNoUmVjb3JkcyxldmVudCk7Pj4+Ozs+Oz4+Oz4+Oz4+Oz4+Oz4+O3Q8O2w8aTwwPjs+O2w8dDxwPGw8Ymdjb2xvcjtzdHlsZTs+O2w8I0YxRjFGMTtCT1JERVItVE9QOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1MRUZUOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1SSUdIVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItQk9UVE9NOiAjQjhEM0VCIDFweCBzb2xpZFw7IEhFSUdIVDogNXB4XDsgQkFDS0dST1VORC1QT1NJVElPTjogY2VudGVyXDsgQkFDS0dST1VORC1JTUFHRTogdXJsKC9ic2EuaXMvUGFnZUJhc2UvRGV0YWlsQm94SW1hZ2UuYXNweD9iZ2NvbG9yPUYxRjFGMSZoZWlnaHQ9MjApXDs7Pj47Oz47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwxPjtpPDI+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPHA8bDxQO0U7PjtsPENDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDOwICAgICAgICAgICAgICAgICAgICAgICAgICOz4+Oz47Oz47Pj47Pj47dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDs+O2w8XGU7Pj47Pjs7Pjs+Pjs+Pjs+Pjs+Pjs+Pjt0PDtsPGk8MD47PjtsPHQ8cDxsPGJnY29sb3I7c3R5bGU7PjtsPCNGMUYxRjE7Qk9SREVSLVRPUDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItTEVGVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItUklHSFQ6ICNCOEQzRUIgMXB4IHNvbGlkXDsgQk9SREVSLUJPVFRPTTogI0I4RDNFQiAxcHggc29saWRcOyBIRUlHSFQ6IDVweFw7IEJBQ0tHUk9VTkQtUE9TSVRJT046IGNlbnRlclw7IEJBQ0tHUk9VTkQtSU1BR0U6IHVybCgvYnNhLmlzL1BhZ2VCYXNlL0RldGFpbEJveEltYWdlLmFzcHg/Ymdjb2xvcj1GMUYxRjEmaGVpZ2h0PTIwKVw7Oz4+Ozs+Oz4+O3Q8O2w8aTwwPjs+O2w8dDw7bDxpPDE+Oz47bDx0PDtsPGk8MT47aTwyPjs+O2w8dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDw7cDxsPG9ua2V5ZG93bjs+O2w8Zm5UcmFwS0QoQnV0dG9uU2VhcmNoUmVjb3JkcyxldmVudCk7Pj4+Ozs+Oz4+Oz4+O3Q8O2w8aTwxPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7PjtsPFxlOz4+Oz47Oz47Pj47Pj47Pj47Pj47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8Nj47aTw4Pjs+O2w8dDxwPHA8bDxCYWNrQ29sb3I7Qm9yZGVyQ29sb3I7XyFTQjs+O2w8MjwwLCAyNTUsIDI1NSwgMjU1PjsyPDAsIDEwNywgMTIyLCAxNjU+O2k8MjQ+Oz4+Oz47Oz47dDxwPHA8bDxCYWNrQ29sb3I7Qm9yZGVyQ29sb3I7XyFTQjs+O2w8MjwwLCAyNTUsIDI1NSwgMjU1PjsyPDAsIDEwNywgMTIyLCAxNjU+O2k8MjQ+Oz4+Oz47Oz47Pj47Pj47Pj47Pj47Pj47bDxCYW5uZXJvQnV0dG9ubztFeHBhbmRvQ29sbGFwc29CdXR0b25vO1N0cmVldEZyb207U3RyZWV0VG87TWFza2VkUGFyY2VsTnVtYmVyOz4+C0ORdnEA2i7LJfrCb5gyWeBSHts="
                        		, simTmp.getFunction(13 ), searchId );
                        Search6.setHiddenParam( true );
                        searchByParcelID.addHTMLObject( Search6 );
                        
                        simTmp.setModuleParentSiteLayout( searchByParcelID );
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
                
                    //name Search
                    {
                        simTmp = SetModuleSearchByName(15, msiServerInfoDefault, TSServerInfo.NAME_MODULE_IDX, "/bsa.is/AssessingServices/ServiceAssessingSearch.aspx", TSConnectionURL.idPOST, "TextBoxOwnerName_on", "");
                       
                        try
                        {
                            PageZone searchByName = new PageZone("searchByName", "Search by name", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250),HTMLObject.PIXELS , true);
                            searchByName.setBorder(true);

                            simTmp.setSeparator(", ");
                            
                            HTMLControl fname = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "", "First Name", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                            fname .setHiddenParam(true);
                            searchByName.addHTMLObject( fname );
                            
                            HTMLControl lname = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxOwnerName_on", "Owner Name", 1, 1, 2, 2, 30, null, simTmp.getFunction( 1 ), searchId );
                            lname.setJustifyField( true );
                            lname.setRequiredExcl( true );
                            searchByName.addHTMLObject( lname );
                            
                            HTMLControl streetNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetFrom", "Number", 1, 1, 1, 1, 30, null, simTmp.getFunction( 2 ), searchId );
                            streetNo .setHiddenParam(true);
                            searchByName.addHTMLObject( streetNo );
                            
                            HTMLControl streetName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxStreetName_sna", "Street name", 1, 1, 1, 1, 30, null, simTmp.getFunction( 3 ), searchId );
                            streetName.setHiddenParam(true);
                            searchByName.addHTMLObject( streetName );
                                                    
                            HTMLControl streetDir = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "TextBoxStreetDir_sd", "Street Dir", 1, 1, 1, 1, 30, null, simTmp.getFunction( 4 ), searchId );
                            streetDir.setHiddenParam(true);
                            searchByName.addHTMLObject( streetDir );
                            
                            HTMLControl to = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetTo", "To", 1, 1, 1, 1, 30, null, simTmp.getFunction( 5 ), searchId );
                            to.setHiddenParam(true);
                            searchByName.addHTMLObject( to );
                            
                            HTMLControl streetNoP = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetFrom_p", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 6 ), searchId );
                            streetNoP.setHiddenParam( true );
                            searchByName.addHTMLObject( streetNoP );
                            
                            HTMLControl toP = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StreetTo_p", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 7), searchId );
                            toP.setHiddenParam( true );
                            searchByName.addHTMLObject( toP);
                            
                            HTMLControl searchy = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "MaskedParcelNumber", "", 1, 1, 1, 1, 30, "", simTmp.getFunction( 8 ), searchId );
                            searchy.setHiddenParam( true );
                            searchByName.addHTMLObject( searchy );
                            
                            HTMLControl Search = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "MaskedParcelNumber_p", "", 1, 1, 1, 1, 30,
                            		URLDecoder.decode("%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02%02", "UTF-8"), simTmp.getFunction( 9 ), searchId );
                            Search.setHiddenParam( true );
                            searchByName.addHTMLObject( Search );
                         
                            HTMLControl Search2 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "RadioButtonRecordCount", "", 1, 1, 1, 1, 30,"50", simTmp.getFunction(10 ), searchId );
                            Search2.setHiddenParam( true );
                            searchByName.addHTMLObject( Search2 );
                            
                            HTMLControl Search3 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ButtonSearchRecords", "", 1, 1, 1, 1, 30,"Search", simTmp.getFunction(11 ), searchId );
                            Search3.setHiddenParam( true );
                            searchByName.addHTMLObject( Search3 );
                            
                            HTMLControl Search4 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTTARGET", "", 1, 1, 1, 1, 30,"", simTmp.getFunction(12 ), searchId );
                            Search4.setHiddenParam( true );
                            searchByName.addHTMLObject( Search4 );
                            
                            HTMLControl Search5 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTARGUMENT", "", 1, 1, 1, 1, 30,"", simTmp.getFunction(13 ), searchId );
                            Search5.setHiddenParam( true );
                            searchByName.addHTMLObject( Search5 );
                            
                            HTMLControl Search6 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATE", "", 1, 1, 1, 1, 30,
                            		"dDw5NjE0NDY0OTQ7dDw7bDxpPDA+Oz47bDx0PDtsPGk8MTk+Oz47bDx0PDtsPGk8MD47aTwxPjtpPDI+O2k8Mz47aTw0PjtpPDU+O2k8Nz47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPGw8Ymdjb2xvcjtzdHlsZTs+O2w8I0YxRjFGMTtCT1JERVItVE9QOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1MRUZUOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1SSUdIVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItQk9UVE9NOiAjQjhEM0VCIDFweCBzb2xpZFw7IEhFSUdIVDogNXB4XDsgQkFDS0dST1VORC1QT1NJVElPTjogY2VudGVyXDsgQkFDS0dST1VORC1JTUFHRTogdXJsKC9ic2EuaXMvUGFnZUJhc2UvRGV0YWlsQm94SW1hZ2UuYXNweD9iZ2NvbG9yPUYxRjFGMSZoZWlnaHQ9MjApXDs7Pj47Oz47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwxPjtpPDM+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPDtwPGw8b25rZXlkb3duOz47bDxmblRyYXBLRChCdXR0b25TZWFyY2hSZWNvcmRzLGV2ZW50KTs+Pj47Oz47Pj47Pj47dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDw7cDxsPG9ua2V5ZG93bjs+O2w8Zm5UcmFwS0QoQnV0dG9uU2VhcmNoUmVjb3JkcyxldmVudCk7Pj4+Ozs+Oz4+Oz4+Oz4+Oz4+Oz4+O3Q8O2w8aTwwPjs+O2w8dDxwPGw8Ymdjb2xvcjtzdHlsZTs+O2w8I0YxRjFGMTtCT1JERVItVE9QOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1MRUZUOiAjQjhEM0VCIDFweCBzb2xpZFw7IEJPUkRFUi1SSUdIVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItQk9UVE9NOiAjQjhEM0VCIDFweCBzb2xpZFw7IEhFSUdIVDogNXB4XDsgQkFDS0dST1VORC1QT1NJVElPTjogY2VudGVyXDsgQkFDS0dST1VORC1JTUFHRTogdXJsKC9ic2EuaXMvUGFnZUJhc2UvRGV0YWlsQm94SW1hZ2UuYXNweD9iZ2NvbG9yPUYxRjFGMSZoZWlnaHQ9MjApXDs7Pj47Oz47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwxPjtpPDI+Oz47bDx0PDtsPGk8MT47PjtsPHQ8O2w8aTwwPjs+O2w8dDxwPHA8bDxQO0U7PjtsPENDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDOwICAgICAgICAgICAgICAgICAgICAgICAgICOz4+Oz47Oz47Pj47Pj47dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDxwPGw8VGV4dDs+O2w8XGU7Pj47Pjs7Pjs+Pjs+Pjs+Pjs+Pjs+Pjt0PDtsPGk8MD47PjtsPHQ8cDxsPGJnY29sb3I7c3R5bGU7PjtsPCNGMUYxRjE7Qk9SREVSLVRPUDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItTEVGVDogI0I4RDNFQiAxcHggc29saWRcOyBCT1JERVItUklHSFQ6ICNCOEQzRUIgMXB4IHNvbGlkXDsgQk9SREVSLUJPVFRPTTogI0I4RDNFQiAxcHggc29saWRcOyBIRUlHSFQ6IDVweFw7IEJBQ0tHUk9VTkQtUE9TSVRJT046IGNlbnRlclw7IEJBQ0tHUk9VTkQtSU1BR0U6IHVybCgvYnNhLmlzL1BhZ2VCYXNlL0RldGFpbEJveEltYWdlLmFzcHg/Ymdjb2xvcj1GMUYxRjEmaGVpZ2h0PTIwKVw7Oz4+Ozs+Oz4+O3Q8O2w8aTwwPjs+O2w8dDw7bDxpPDE+Oz47bDx0PDtsPGk8MT47aTwyPjs+O2w8dDw7bDxpPDE+Oz47bDx0PDtsPGk8MD47PjtsPHQ8cDw7cDxsPG9ua2V5ZG93bjs+O2w8Zm5UcmFwS0QoQnV0dG9uU2VhcmNoUmVjb3JkcyxldmVudCk7Pj4+Ozs+Oz4+Oz4+O3Q8O2w8aTwxPjs+O2w8dDw7bDxpPDA+Oz47bDx0PHA8cDxsPFRleHQ7PjtsPFxlOz4+Oz47Oz47Pj47Pj47Pj47Pj47Pj47dDw7bDxpPDA+Oz47bDx0PDtsPGk8Nj47aTw4Pjs+O2w8dDxwPHA8bDxCYWNrQ29sb3I7Qm9yZGVyQ29sb3I7XyFTQjs+O2w8MjwwLCAyNTUsIDI1NSwgMjU1PjsyPDAsIDEwNywgMTIyLCAxNjU+O2k8MjQ+Oz4+Oz47Oz47dDxwPHA8bDxCYWNrQ29sb3I7Qm9yZGVyQ29sb3I7XyFTQjs+O2w8MjwwLCAyNTUsIDI1NSwgMjU1PjsyPDAsIDEwNywgMTIyLCAxNjU+O2k8MjQ+Oz4+Oz47Oz47Pj47Pj47Pj47Pj47Pj47bDxCYW5uZXJvQnV0dG9ubztFeHBhbmRvQ29sbGFwc29CdXR0b25vO1N0cmVldEZyb207U3RyZWV0VG87TWFza2VkUGFyY2VsTnVtYmVyOz4+C0ORdnEA2i7LJfrCb5gyWeBSHts="
                            		, simTmp.getFunction(14 ), searchId );
                            Search6.setHiddenParam( true );
                            searchByName.addHTMLObject( Search6 );
                            
                            
                            simTmp.setModuleParentSiteLayout( searchByName );
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                        
                        simTmp.getFunction( 1 ).setSaKey( SearchAttributes.OWNER_LF_NAME );

                    }
                
                
            msiServerInfoDefault.setupParameterAliases();
            setModulesForAutoSearch( msiServerInfoDefault );
        }
                
    return msiServerInfoDefault;
}
	
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;
		
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if(!StringUtils.isEmpty(city)){
			if(!city.startsWith("DETROIT")){
				return;
			}			
		}
		
		//search by PID
		if(hasPin()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}
		
		//search by owner + address
		if(hasOwner() && hasStreet()){
			
			//search by owner lname, street# and street name
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			m.getFunction( 0 ).setSaKey( "" );
			m.getFunction( 1 ).setSaKey( SearchAttributes.OWNER_LNAME );
			m.getFunction( 2 ).setSaKey( SearchAttributes.P_STREETNO );
			m.getFunction( 3 ).setSaKey( SearchAttributes.P_STREETNAME );
			m.addFilter(addressFilter);
			l.add(m);
			
			//search by owner lname, street name
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			m.getFunction( 0 ).setSaKey( "" );
			m.getFunction( 1 ).setSaKey( SearchAttributes.OWNER_LNAME );
			m.getFunction( 3 ).setSaKey( SearchAttributes.P_STREETNAME );
			m.addFilter(addressFilter);
			l.add(m);
		}
		
		//search by address
		if(hasStreet()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.addFilter(addressFilter);
			l.add(m);
		}
		
		//search by name
		if(hasOwner()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
            m.addFilter(addressFilter);
            GenericNameFilter nameFilter =  (GenericNameFilter)NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			nameFilter.setUseSynonymsForCandidates(false);
			m.addFilter(nameFilter);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			
			m.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {"L;F;","L;M;"})
					);
			l.add(m);			
		}
		
		serverInfo.setModulesForAutoSearch(l);		
	}
	

	private boolean hasNextLink(int curentPage,Vector<Integer> otherPages){
		if(curentPage<=0){
			return false;
		}
		return otherPages.contains(curentPage);
	}
	
	private boolean hasPreviousLink(int curentPage,Vector<Integer> otherPages){
		if(curentPage<=0){
			return false;
		}
		return otherPages.contains(curentPage-2);
	}
	
	private String cleanFinalDocument(String content,int type){
		String str1 = content;
		
		if(type == AO_DOCUMENT){
			int pozStart = str1.indexOf(FINAL_DOCUMENT_IDENTIFICATION);	
			if(pozStart>=0){
				pozStart = str1.indexOf("<td", pozStart);
			   str1 = str1 .substring(pozStart);
			   str1 = "<table border='0' cellspacing='0' cellpadding='0' width='100%'>\n<tr>\n"+str1;
			}
			
			str1  = str1.replaceAll("\"display:[^\"]*\"","\"\"");
			
			int poz1 = 0;
			int 	poz2 = str1.lastIndexOf( CLEAN_FINAL_RESULT_END );
			int pozEnd = str1.lastIndexOf(CONTENT_END);
			if(poz2 < pozEnd){
				poz2 = pozEnd;
			}
			if(poz1>=0&&poz2>=0&&poz1<poz2){
				str1 = str1.substring(poz1,poz2);
			}
			poz2 = str1.lastIndexOf("<tr>");
			if(poz1>=0&&poz2>=0&&poz1<poz2){
				str1 = str1.substring(poz1,poz2);
			}
			
			str1 = str1.replaceAll( "(?is)<img[^>]*>" , "" );
		}
		else if ( type == EP_DOCUMENT ){
			
			
			
			
			
			int pozStart = str1.indexOf(FINAL_TAX_DOCUMENT_IDENTIFICATION);
			if(pozStart>=0){
				pozStart = str1.indexOf("<td", pozStart);
			   str1 = str1 .substring(pozStart);
			   str1 = "<table border='0' cellspacing='0' cellpadding='0' width='100%'>\n<tr>\n"+str1;
			}
			 str1 = str1.replaceAll("(?i)<div[ \t\n\r]*id=\"DataGridRecords[^\"]*\"[^>]*>[ \t\r\n]*<div[^=]*=[^>]*>","<tr><td>");
			 str1 = str1.replaceAll("(?i)</div>[ \t\r\n]+</div>", "</td></tr>");
			 str1 = str1.replaceAll("(?i)<script[^>]*>[^<]*</script>", "");
			 
			 str1 = str1.replaceFirst("(?i)</form>", "");
			 int a = str1.indexOf("</body>");
			 if(a>0){
				 str1 = str1.substring(0, a);
			 }
			 
			 
			 StringBuffer strbuf = new StringBuffer(str1);
				Matcher matSumary =  patSumary.matcher(strbuf);
			    Vector vec = new Vector();
				
			    while(matSumary .find()){
					vec.add("<tr><td>&nbsp;</td>"+matSumary.group()+"</tr>");
					strbuf.replace(matSumary.start(), matSumary.end(), "");
					matSumary.reset();
				}
			
				Matcher matSatrtTaxTable = patStartTaxTable.matcher(strbuf);
				
				if(matSatrtTaxTable.find()){
					int offset = matSatrtTaxTable.end();
					for(int i=0;i<vec.size();i++){
						strbuf.insert(offset, (String)vec.get(i));
						offset += ((String)vec.get(i)).length();
					}
				}
				str1 = strbuf.toString();
			 
		}
		
	
		return str1.replaceAll("Printer friendly version", "").replaceAll("(?i)bgcolor[\n\t ]*=[\n\t ]*\"[^\"]*\"", "").replaceAll("(?i)bgcolor[\n\t ]*=[\n\t ]*'[^']*'", "").replaceAll("(?i)<a[^>]*>Privacy Policy</a>", "")
					.replaceAll("(?i)<a[^>]*>[<b>**]*Note.*?</a>", "").replaceAll("(?i)<a[^>]*><tr><td[^>]*><table.*?</a>", "").replaceAll("(?i)<input[^\"]*\"[^\"]*\"[^\"]*\"[0-9]+/[0-9]+/[0-9]+[^>]*>", "").replaceAll("(?i)<input[^\"]*\"[^\"]*\"[^\"]*\"ButtonRecalc\"[ \n\t\r]*value[ \t\r\n]*=[ \t\r\n]*\"Re-Calculate\"[^>]*>", "")
					.replaceAll("&nbsp", "&nbsp;").replaceAll(";;", ";");
		
	}
	
	private String getParcelNumber(String content){
		String str1 = content;
		String keyNumber="";
		Matcher findParcelMatcher = parcelPattern.matcher( str1 );
		if( findParcelMatcher.find() ){
			keyNumber = findParcelMatcher.group(1).replaceAll("[.-]", "");
		}
		else {
			keyNumber = "not_available";
		}
		return keyNumber;
	}
	
	private String obtainFinalDocument(String rsResponse1,HTTPSiteInterface site){
		String str1=rsResponse1;
		HTTPRequest request=null;
		HTTPResponse response = null;
		Matcher matForm = null;
		HashMap<String,String>   otherPopstParameters=null;
		
			if(  !str1.contains(FINAL_DOCUMENT_IDENTIFICATION)  ){
				   
				   int poz1 = str1.indexOf("<form");
				   int poz2 = str1.indexOf("</form>");
				   matForm = patCurrentForm.matcher(str1);
				   
				   if(poz1>0&&poz2>0&&poz1<poz2){
					    otherPopstParameters = HttpUtils.getFormParams(str1.substring(poz1,poz2+"</form>".length()));
						if(matForm.find()){
							String url = matForm.group(1);
							if( url != null ){
								
								request = new HTTPRequest("https://is.bsasoftware.com/bsa.is/"+url.replaceAll("&amp;", "&"));
								request .setMethod(HTTPRequest.POST);
	
								Iterator it  = otherPopstParameters.keySet().iterator();
								
								while(it.hasNext()){
									String name = (String)it.next();
									String value ="";
									try	{
										value =URLDecoder.decode(otherPopstParameters.get(name), "UTF-8") ;
									}
									catch(Exception e){
										e.printStackTrace();
									}
									request .setPostParameter(name, value.replaceAll("[\n\r]", ""));
								}
								
								request .removePostParameters( "ButtonAccept" );
								request .removePostParameters( "ButtonDeposit" );
								request .removePostParameters("DropDownListMonth");
								request .setPostParameter("DropDownListMonth", "1");
								request .removePostParameters("DropDownListYear");
								request .setPostParameter("DropDownListYear", "2006");
								request .removePostParameters("TextState");
								request .setPostParameter("TextState", "Tex");
								request .removePostParameters("__EVENTTARGET");
								request .setPostParameter( "__EVENTTARGET", "ButtonAccept");
								
								response= site.process(request);
								str1  = response.getResponseAsString();
								str1 = str1.replaceAll("(?i)[^\"]*\"([^\"]*)\"([^>]*>){3}","$1");
								request= new HTTPRequest ("https://is.bsasoftware.com"+str1);
								 response= site.process(request);
								 
								 str1 = response.getResponseAsString();
								 matForm = patCurrentForm.matcher(str1);
								 poz1 = str1.indexOf("<form");
								 poz2 = str1.indexOf("</form>");
								 otherPopstParameters = HttpUtils.getFormParams(str1.substring(poz1,poz2+"</form>".length()));
								 
								 if(matForm.find()){
									 url = matForm.group(1);
								 }
								 
								 request = new HTTPRequest( "https://is.bsasoftware.com/bsa.is/"+url.replaceAll("&amp;", "&") );
								 request .setMethod(HTTPRequest.POST);//ButtonAccept=Proceed
								 it  = otherPopstParameters.keySet().iterator();
									
									while(it.hasNext()){
										String name = (String)it.next();
										String value ="";
										try	{
											value =URLDecoder.decode(otherPopstParameters.get(name), "UTF-8") ;
										}
										catch(Exception e){
											e.printStackTrace();
										}
										request .setPostParameter(name, value.replaceAll("[\n\r]", ""));
									}
									
									response = site.process(request);
									str1 = response.getResponseAsString();	
						   }
						}
					}
				}
			String temp = str1;
			String strAODocument = cleanFinalDocument(str1, AO_DOCUMENT);
			
			//tax part
			for(int i=0;i<2;i++){
				str1 = temp;
				String taxUrl = "";
				Matcher mat = patCurrentTax.matcher(str1);
				
				if(mat .find()){
					taxUrl  =( "https://is.bsasoftware.com"+ mat.group(1).replaceAll("&amp;", "&"));
				}
				request = new HTTPRequest(taxUrl);
				request.setReplaceSpaceOnredirect(true);
				response = site.process(request);
				str1 = response.getResponseAsString();
				
				
				if( str1.contains(FINAL_DOCUMENT_START_SECV) ){
					
					  int poz1 = str1.indexOf("<form");
					   int poz2 = str1.indexOf("</form>");
					   matForm = patCurrentForm.matcher(str1);
					   
					   if(poz1>0&&poz2>0&&poz1<poz2){
						    otherPopstParameters = HttpUtils.getFormParams(str1.substring(poz1,poz2+"</form>".length()));
							if(matForm.find()){
								String url = matForm.group(1);
								if( url != null ){
									
									request = new HTTPRequest("https://is.bsasoftware.com/bsa.is/"+url.replaceAll("&amp;", "&"));
									request .setMethod(HTTPRequest.POST);
		
									Iterator it  = otherPopstParameters.keySet().iterator();
									
									while(it.hasNext()){
										String name = (String)it.next();
										String value ="";
										try	{
											value =URLDecoder.decode(otherPopstParameters.get(name), "UTF-8") ;
										}
										catch(Exception e){
											e.printStackTrace();
										}
										request .setPostParameter(name, value.replaceAll("[\n\r]", ""));
									}
									
									request .removePostParameters( "ButtonAccept" );
									request .removePostParameters( "ButtonDeposit" );
									request .removePostParameters("DropDownListMonth");
									request .setPostParameter("DropDownListMonth", "1");
									request .removePostParameters("DropDownListYear");
									request .setPostParameter("DropDownListYear", "2006");
									request .removePostParameters("TextState");
									request .setPostParameter("TextState", "Tex");
									request .removePostParameters("__EVENTTARGET");
									request .setPostParameter( "__EVENTTARGET", "ButtonAccept");
									
									response= site.process(request);
									str1  = response.getResponseAsString();
									str1 = str1.replaceAll("(?i)[^\"]*\"([^\"]*)\"([^>]*>){3}","$1");
									request= new HTTPRequest ("https://is.bsasoftware.com"+str1);
									 response= site.process(request);
									 
									 str1 = response.getResponseAsString();
									 matForm = patCurrentForm.matcher(str1);
									 poz1 = str1.indexOf("<form");
									 poz2 = str1.indexOf("</form>");
									 otherPopstParameters = HttpUtils.getFormParams(str1.substring(poz1,poz2+"</form>".length()));
									 
									 if(matForm.find()){
										 url = matForm.group(1);
									 }
									 
									 //for(int i=0;i<2;i++){
										 request = new HTTPRequest( "https://is.bsasoftware.com/bsa.is/"+url.replaceAll("&amp;", "&") );
										 request .setMethod(HTTPRequest.POST);
										 it  = otherPopstParameters.keySet().iterator();
											
											while(it.hasNext()){
												String name = (String)it.next();
												String value ="";
												try	{
													value =URLDecoder.decode(otherPopstParameters.get(name), "UTF-8") ;
												}
												catch(Exception e){
													e.printStackTrace();
												}
												request .setPostParameter(name, value.replaceAll("[\n\r]", ""));
											}
											
											response = site.process(request);
											str1 = response.getResponseAsString();	
									// }
							   }
							}
						}
				}
			}
			String strEPDocument = cleanFinalDocument(str1, EP_DOCUMENT);
		
			return strAODocument + "<table><tr><td><br/><br/><b> TAX DOCUMENT: </b><br/><br/></td></tr></table>"+strEPDocument;
		
	}
	
	
	public static void splitResultRows( Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException
    {
    	p.splitResultRows( pr, htmlString, pageId, "</tr><tr class=", "</table><br>", linkStart, action);
    }   
	
}

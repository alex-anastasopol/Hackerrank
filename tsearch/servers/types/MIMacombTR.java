package ro.cst.tsearch.servers.types;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class MIMacombTR extends TSServer {

	private static final long serialVersionUID = 4760453061875693625L;

	
	
	private boolean downloadingForSave;
	
	public static final String searchByNameTableHeader2 = "<TR bgcolor=\"#ECECEC\"><th>Tax ID #</th><th>Tax Payer Name</th><th>Property Address</th></TR>";
	public static final String searchByNameTableHeader = "<TR bgcolor=\"#ECECEC\"><th>Tax ID #</th><th>Tax Payer Name</th><th>Property Address</th><th>Community</th><th>Tax Pay Address</th><th>Tax Pay City</th><th>Tax Pay State</th><th>Tax Pay Zip</th><th>Tax Year</th><th>Total Due($)</th></TR>";
	public static final String searchByAddressAndPidTableHeader = "<TR bgcolor=\"#ECECEC\"><th>Tax ID #</th><th>Property Address</th><th>Community</th></TR>";
	
	
	public static final Pattern prevNextLinks = Pattern.compile("(?is)<value><int>(\\d+)</int></value>\\s+<value><int>(\\d+)</int></value>\\s+<value><int>(\\d+)</int></value>");
	public static final Pattern structPattern = Pattern.compile( "(?is)<struct>(.*?)</struct>" );
	public static final Pattern nameValuePattern = Pattern.compile( "(?is)<member>\\s*<name>([^<]*)</name>\\s*<value><string>([^<]*)</string></value>" );	
	
	public MIMacombTR(long searchId) {super(searchId); }

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIMacombTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId,int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId,mid);
	}

	 protected String getFileNameFromLink(String link) {
		 String parcelId = StringUtils.getTextBetweenDelimiters( "viewTaxDetails=" , "&", link);
		 return parcelId + ".html";
	}
	
	 private String getPrevNextLink(String initialResponse, ServerResponse r){
		 Matcher m = prevNextLinks.matcher(initialResponse);
		 String prevNextLinkHtml = "";
		 if (m.find()){
			 int currentPage;
			 int totalPages;
			 try {
				 currentPage = Integer.parseInt(m.group(1));
				 totalPages = Integer.parseInt(m.group(2));
			 }catch(NumberFormatException e){ 
				 return "";
			 }
			 String sTmp = CreatePartialLink(TSConnectionURL.idPOST);
			 LinkParser lp = new LinkParser(sTmp + "/freeance/Server/Dzeims3.php?" + r.getQuerry().replaceAll("%", URLEncoder.encode("%")));
			 
			 if (currentPage > 1){
				 int prevPage = currentPage;
				 prevPage--;
				 lp.addParameter("page", Integer.toString(prevPage));
				 prevNextLinkHtml += "<a href=\"" + lp.toString() + "\">&lt;Prev</a>";
			 } else {
				 prevNextLinkHtml += "&lt;Prev";
			 }
			 prevNextLinkHtml += "&nbsp;Page " + currentPage + " out of " + totalPages + "&nbsp;";
			 if (currentPage <totalPages){
				 int nextPage = currentPage;
				 nextPage++;
				 lp.addParameter("page", Integer.toString(nextPage));
				 prevNextLinkHtml += "<a href=\"" + lp.toString() + "\">Next&gt;</a>";
				 if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH)
				 {
					 r.getParsedResponse().setNextLink("<a href=\"" + lp.toString()  + "\">Next&gt;</a>");
				 }
			} else {
				 prevNextLinkHtml += "Next&gt;";
			 }
		 }
		 return prevNextLinkHtml;
	 }
	 
	@SuppressWarnings("unchecked")
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		rsResponse = rsResponse.replace("&apos;", "&#39;");  
		
		String initialResponse = rsResponse;
		String keyNumber = "";
		
		switch( viParseID ){
		
			case ID_SEARCH_BY_NAME:
				rsResponse = getNameSearchResults( initialResponse );
				
				if( rsResponse == null ){
					return;
				}
				rsResponse = getPrevNextLink(initialResponse, Response) + "<table border=\"1\" cellspacing=\"0\" cellpadding=\"0\">" + rsResponse + "</table>";
				parser.Parse( Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD);
				break;
				
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:
				
				rsResponse = getAddressAndPidSearchResults( initialResponse );
				
				if( rsResponse == null ){
					return;
				}
				
				rsResponse = getPrevNextLink(initialResponse, Response) + "<table border=\"1\" cellspacing=\"0\" cellpadding=\"0\">" + rsResponse + "</table>";				
				parser.Parse( Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD);				
				break;
				
			case ID_DETAILS:
				
				rsResponse = formatFinalPage(initialResponse);
				
				String respQuery = Response.getQuerry();
				keyNumber = StringUtils.getTextBetweenDelimiters( "viewTaxDetails=" , "&", respQuery);
				String ownerName = "";
				//String ownerName = StringUtils.getTextBetweenDelimiters( "ownerName=" , "&", respQuery);
				
				if (respQuery.contains("ownerName")) { // B3205, if we don't have a name in intermediary results, than the 
															// the owner name will become ownerName=viewTaxDetails...
					ownerName = respQuery.replaceFirst(".*\\bownerName=(.*?)&(?=\\w+=|$).*", "$1");
				}
				String ownerCity = StringUtils.getTextBetweenDelimiters( "ownerCity=" , "&", respQuery);
				
				//get delinquent tax info
				try{
					HTTPSiteInterface site = (HTTPSiteInterface)HTTPSiteManager.pairHTTPSiteForTSServer("MIMacombTR",searchId,miServerID);
					HTTPRequest req = new HTTPRequest( "http://gis.macombcountymi.gov/freeance/Server/Dzeims3.php" );
					req.setMethod( HTTPRequest.POST );
					req.setXmlPostData("<methodCall>" +
					        	"<methodName>SQL.execute.definedPagedQuery</methodName>" +
					        	"<params>" +
					        	"<param>" +
					        	"<value><string>Delinquent_TaxID</string></value>" +
					        	"</param>" +
					        	"<param>" +
					        	"<value><i4>50</i4></value>" +
					        	"</param>" +
					        	"<param>" +
					        	"<value><i4>1</i4></value>" +
					        	"</param>" +
					        	"<param>" +
					        	"<value><array><data>" +
					        	"<value><array><data>" +
					        	"<value><string>taxid</string></value>" +
					        	"<value><string>" + keyNumber + "</string></value>" +
					        	"</data></array>" +
					        	"</value>" +
					        	"</data></array>" +
					        	"</value>" +
					        	"</param>" +
					        	"</params>" +
					        	"</methodCall>");
					HTTPResponse response = site.process( req );
					
					String delinquentResponse = response.getResponseAsString();
					
					rsResponse += getDelinquent( delinquentResponse );
				} catch( Exception e ){
					e.printStackTrace();
				}

				String[] ownerNames = StringFormats.parseNameNashville( ownerName );
				if(!StringUtils.isEmpty(ownerName)){
					rsResponse = "Owner:&nbsp;&nbsp;&nbsp;" + ownerName + "<br/>" + rsResponse;
				}
//				if(!StringUtils.isEmpty(ownerNames[2] + ownerNames[0] + ownerNames[1])){
//					rsResponse = "Owner:&nbsp;&nbsp;&nbsp;" + ownerNames[2] + ", " + ownerNames[0] + " " + ownerNames[1] + "<br/>" + rsResponse;
//				}
//            	if(!StringUtils.isEmpty(ownerNames[5] + ownerNames[3] + ownerNames[4])){
//            		rsResponse = "Co-owner:&nbsp;&nbsp;&nbsp;" + ownerNames[5] + ", " + ownerNames[3] + " " + ownerNames[4] + "<br/>" + rsResponse;
//            	}
            	
				if( !downloadingForSave ){
            		//not saving to TSR
            		
    				String qry = Response.getQuerry();
    				Response.setQuerry(qry);
    				String originalLink = sAction + "&" + qry;
                    String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idPOST) + originalLink;

    				if (FileAlreadyExist(keyNumber + ".html") ) {
    					rsResponse += CreateFileAlreadyInTSD();
    				} else {
    					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
    					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
    				}

    				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
    				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
    				
            	} else {
            		//saving
                    msSaveToTSDFileName = keyNumber + ".html" ;
                    Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                    msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
            		
                    parser.Parse(
                        Response.getParsedResponse(),
                        rsResponse,
                        Parser.PAGE_DETAILS,
                        getLinkPrefix(TSConnectionURL.idPOST),
                        TSServer.REQUEST_SAVE_TO_TSD);
                    
                    if( !"".equals( ownerName ) ){

                    	PropertyIdentificationSet pis = null;                    	
                    	if( ((Vector<PropertyIdentificationSet>)Response.getParsedResponse().infVectorSets.get( "PropertyIdentificationSet" )).size() == 0 ){
                    		pis = new PropertyIdentificationSet();                    		
                    		Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>)Response.getParsedResponse().getPropertyIdentificationSet();                    		
                    		pisVector.add( pis );                    		
                    		//Response.getParsedResponse().infVectorSets.put( "PropertyIdentificationSet" , pisVector);
                    	} else {
                    		pis = (PropertyIdentificationSet) ((Vector)Response.getParsedResponse().infVectorSets.get( "PropertyIdentificationSet" )).elementAt( 0 );                    		
                    	}
                    	
                    	pis.setAtribute("OwnerFirstName", ownerNames[0]);
                    	pis.setAtribute("OwnerMiddleName", ownerNames[1]);
                    	pis.setAtribute("OwnerLastName", ownerNames[2]);
                    	pis.setAtribute("SpouseFirstName", ownerNames[3]);
                    	pis.setAtribute("SpouseMiddleName", ownerNames[4]);
                    	pis.setAtribute("SpouseLastName", ownerNames[5]);
                    	pis.setAtribute("City", ownerCity);

                    }
            	}
				
				break;
				
			case ID_SAVE_TO_TSD:
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
				
			case ID_GET_LINK:
				if( Response.getQuerry().contains( "viewTaxDetails=" ) ){
					ParseResponse(sAction, Response, ID_DETAILS);	
				} else{
					if (Response.getQuerry().contains("taxPayerName=")){
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
					} else {
						ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
					}
				}				
				break;
		}
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();

		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		
		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}

		if (hasStreet()) {
			// cleanup street name
			streetName = streetName.replaceAll("\\s{2,}", " ").trim().replaceFirst("\\s*\\.$", "");
			// create set of names
			Set<String> names = new LinkedHashSet<String>();
			names.add(streetName);
			// try with first word only, if long enough
			int idx = streetName.indexOf(" ");
			if (idx > 5) {
				names.add(streetName.substring(0, idx));
			}
			// try without pre-direction
			names.add(streetName.replaceFirst("^[^NESW] ", ""));
			// iterate through all names
			for (String name : names) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				m.getFunction(1).setSaKey("");
				m.getFunction(1).setParamValue("%" + name);
				m.addFilter(addressFilter);
				m.addFilter(nameFilterHybrid);
				l.add(m);
			}
		}

		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);			
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);
			
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}
	
    public static void splitResultRows( Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException
    {
    	p.splitResultRows( pr, htmlString, pageId, "<tr", "</table>", linkStart, action);
    }
    
    private String getNameSearchResults( String xmlResponse ){
    	StringBuffer sbNameResponse = new StringBuffer();
    	
    	sbNameResponse.append( searchByNameTableHeader2 );
    	
    	Matcher structMatcher = structPattern.matcher( xmlResponse );
    	int rows = 0;
    	
    	String sTmp = CreatePartialLink(TSConnectionURL.idPOST);
    	
    	while( structMatcher.find() ){
    		String result = structMatcher.group( 1 );
    		
    		Matcher nameValueMatcher = nameValuePattern.matcher( result );
    		
    		String taxId = "";
    		String address = "";
    		String community = "";
    		String taxPayer = "";
    		String taxPayerAddress = "";
    		String taxPayCity = "";
    		String taxPayState = "";
    		String taxPayZip = "";
    		String taxYear = "";
    		String totalDue = "";
    		
    		boolean foundSmth = false;
    		
    		while( nameValueMatcher.find() ){
    			String fieldName = nameValueMatcher.group( 1 );
    			String fieldValue = nameValueMatcher.group( 2 );
    			
    			if( fieldName.equals( "[owner].[DELINQ].[SIDWELL]" ) ){
    				taxId = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "[owner].[DELINQ].[MST_PROPERTY_STREET_ADDRESS]" ) ){
    				address = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "Delinquent Parcels.CVT_NAME" ) ){
    				community = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "[owner].[DELINQ].[MST_TAXPAYER_NAME]" ) ){
    				taxPayer = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "Delinquent Parcels.MST_TAXPAYSTADDR" ) ){
    				taxPayerAddress = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "Delinquent Parcels.MST_TAXPAYCITY" ) ){
    				taxPayCity = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "Delinquent Parcels.MST_TAXPAYSTATE" ) ){
    				taxPayState = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "Delinquent Parcels.MST_TAXPAYZIP" ) ){
    				taxPayZip = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "Delinquent Parcels.TAX_TAX_YEAR" ) ){
    				taxYear = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "Delinquent Parcels.TOTAL_DUE" ) ){
    				totalDue = fieldValue;
    				foundSmth = true;
    			}
    		}
    		
    		taxId = "<a href=\"" + sTmp + "/freeance/Server/Dzeims3.php&viewTaxDetails=" + taxId + "&ownerName=" + taxPayer + "&ownerCity=" + taxPayCity + "\">" + taxId + "</a>";
    		
    		sbNameResponse.append( "<tr>" +
    								"<td>" + taxId + "</td>" +
    								"<td>" + taxPayer + "</td>" +
    								"<td>" + address + "</td>" /*+
    								"<td>" + community + "</td>" +
    								"<td>" + taxPayerAddress + "</td>" +
    								"<td>" + taxPayCity + "</td>" +
    								"<td>" + taxPayState + "</td>" +
    								"<td>" + taxPayZip + "</td>" +
    								"<td>" + taxYear + "</td>" +
    								"<td>" + totalDue + "</td>" */+
    								"</tr>" );
    		if( foundSmth ){
    			rows ++;
    		}
    	}
    	
    	if( rows == 0 ){
    		return null;
    	}
    	
    	return sbNameResponse.toString();
    }
    
    private String getAddressAndPidSearchResults( String xmlResponse ){
    	StringBuffer sbAddressResponse = new StringBuffer();
    	
    	sbAddressResponse.append( searchByAddressAndPidTableHeader );
    	
    	Matcher structMatcher = structPattern.matcher( xmlResponse );
    	int rows = 0;
    	
    	String sTmp = CreatePartialLink(TSConnectionURL.idPOST);
    	
    	while( structMatcher.find() ){
    		String result = structMatcher.group( 1 );
    		
    		Matcher nameValueMatcher = nameValuePattern.matcher( result );
    		
    		String taxId = "";
    		String address = "";
    		String community = "";
    		
    		boolean foundSmth = false;
    		
    		while( nameValueMatcher.find() ){
    			String fieldName = nameValueMatcher.group( 1 );
    			String fieldValue = nameValueMatcher.group( 2 );
    			
    			if( fieldName.equals( "[owner].[Parcels_Web].[TAX_ID]" ) ){
    				taxId = fieldValue;
    				taxId = "<a href=\"" + sTmp + "/freeance/Server/Dzeims3.php&viewTaxDetails=" + taxId + "\">" + taxId + "</a>";
    				foundSmth = true;
    			} else if( fieldName.equals( "[owner].[Parcels_Web].[ADDRESS]" ) ){
    				address = fieldValue;
    				foundSmth = true;
    			} else if( fieldName.equals( "[owner].[Parcels_Web].[CVT_NAME]" ) ){
    				community = fieldValue;
    				foundSmth = true;
    			}
    		}
    		
    		sbAddressResponse.append( "<tr>" +
    								"<td>" + taxId + "</td>" +
    								"<td>" + address + "</td>" +
    								"<td>" + community + "</td>" +
    								"</tr>" );
    		if( foundSmth ){
    			rows ++;
    		}
    	}
    	
    	if( rows == 0 ){
    		return null;
    	}
    	
    	return sbAddressResponse.toString();
    }
    
    private String formatFinalPage( String initialResponse ){
    	StringBuffer sbFinalResponse = new StringBuffer();
    	
    	sbFinalResponse.append( "<table>" );   	
    	Matcher nameValueMatcher = nameValuePattern.matcher( initialResponse );
		
    	while( nameValueMatcher.find() ){
			String fieldName = nameValueMatcher.group( 1 );
			String fieldValue = nameValueMatcher.group( 2 );
			
			if( fieldName.equals( "TAX_ID" ) ){
				sbFinalResponse.append( "<tr><td>Tax ID#</td><td>" + fieldValue + "</td></tr>" );
			} else if( fieldName.equals( "ADDRESS" ) ){
				sbFinalResponse.append( "<tr><td>Property Address</td><td>" + fieldValue + "</td></tr>" );
			} else if( fieldName.equals( "CVT_NAME" ) ){
				sbFinalResponse.append( "<tr><td>Community</td><td>" + fieldValue + "</td></tr>" );
			} else if( fieldName.equals( "[owner].[LEGAL].[LEG_LEGALDESC]" ) ){
				sbFinalResponse.append( "<tr><td>Legal Description</td><td>" + fieldValue + "</td></tr>" );
			}
    	}
    	
    	sbFinalResponse.append( "</table>" );
    	
    	return sbFinalResponse.toString();
    }
    
    private String getDelinquent( String response ){
    	StringBuffer sbDelinquent = new StringBuffer();
    	
    	Matcher nameValueMatcher = nameValuePattern.matcher( response );
    	
    	String year = "";
    	
    	sbDelinquent.append( "<table border=\"1\"><tr><td>Year</td><td>Delinquent($)</td></tr>" );
    	
    	while( nameValueMatcher.find() ){
			String fieldName = nameValueMatcher.group( 1 );
			String fieldValue = nameValueMatcher.group( 2 );
			
			if( fieldName.equals( "[owner].[DELINQ].[TAX_TAX_YEAR]" ) ){
				year = fieldValue;

			} else if( fieldName.equals( "[owner].[DELINQ].[TOTAL_DUE]" ) ){
				sbDelinquent.append( "<tr><td>" + year + "</td><td>" + fieldValue + "</td></tr>" );
			}
    	}
    	
    	sbDelinquent.append( "</table>" );
    	
    	return sbDelinquent.toString();
    }
    
}

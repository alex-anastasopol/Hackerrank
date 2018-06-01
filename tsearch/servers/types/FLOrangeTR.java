package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

@SuppressWarnings("deprecation")
public class FLOrangeTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave;

	
	private static final Pattern RECEIPT_LINK = Pattern.compile("(?is)(ViewTaxBill[^']+)"); 
	
	public void setServerID(int ServerID){
		super.setServerID(ServerID);
	}
	
	public FLOrangeTR(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink,
			long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		
	}

	public static void splitResultRows(
			Parser p,
			ParsedResponse pr,
			String htmlString,
			int pageId,
			String linkStart,
			int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException
			{
			
			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"<tr>",
				"</table>",
				linkStart,
				action);
		}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)
			throws ServerResponseException
	{
			String sTmp1 = "";
			String rsResponse = Response.getResult();
			String initialResponse = rsResponse;

	        if (rsResponse.matches("(?is).*No\\s*records\\s*match\\s*your\\s*search.*")) {
	        	return;
	        }
	        String pageNotFoundMessage = "We're sorry but we could not find the page you are looking for.";
	        if (rsResponse.contains(pageNotFoundMessage)) {
	        	Response.getParsedResponse().setError(pageNotFoundMessage);
	        	return;
	        }
	        switch (viParseID)
			{
				case ID_SEARCH_BY_NAME :
				case ID_SEARCH_BY_PARCEL :
				case ID_SEARCH_BY_ADDRESS :
				case ID_SEARCH_BY_PROP_NO :
				    if (rsResponse.contains("Current Taxes and Unpaid Delinquent Warrants")) {
				    	ParseResponse(sAction, Response, ID_DETAILS);
				    	return;
				    }

				    try
				    {
				    	rsResponse = new String (rsResponse.getBytes(),"UTF-8");//ISO-8859-1
				    	initialResponse = new String (initialResponse.getBytes(),"UTF-8");//ISO-8859-1
				    }
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }
				    sTmp1 = CreatePartialLink(TSConnectionURL.idGET);
		    
				    //rsResponse = rsResponse.replaceAll("<\\s*tr\\s*>\\s*<\\s*td[^>]*>","\n<tr><td>");
				    //rsResponse = rsResponse.replaceAll("(?is)<\\s*/?font[^>]*>", "");
				    //rsResponse = rsResponse.replaceFirst("(?is).*Account\\s+Id</b>\\s*<\\s*/td\\s*>\\s*<\\s*/tr\\s*>", 
				    	//	"<table border=\"1\" width=\"750\" cellpadding=\"3\" cellspacing=\"1\">\n" + 
						  //  "<tr>\n" +
						    //"<td><b>No</b></td><td><b>Person / Company Name</b></td>\n"+
						   // "<td><b>Address</b></td>\n" +
						    //"<td><b>Parcel Id</b></td><td><b>Account Id</b></td>\n</tr>");
				    //rsResponse = rsResponse.replaceFirst("(?is)<\\s*/table\\s*>\\s*<\\s*/center[^>]*>\\s*<\\s*/form[^>]*>.*", "\n</table>\n");
				    try {
						org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
						NodeList mainList = htmlParser.parse(null);
						rsResponse = HtmlParser3.getNodeByID("ctl00_mainContent_grdResults", mainList, true).toHtml();
					} catch (Exception e) {
						e.printStackTrace();
					}
				    
				    //remove links from table header
				    rsResponse = rsResponse.replaceAll("(?is)(<th[^>]*>)\\s*<a[^>]*>([^<]*)</a>\\s*(</th>)", "$1$2$3");
				    
				    rsResponse = rsResponse.replaceAll("(?is)(\\s*<\\s*td[^>]*>\\s*)<\\s*a\\s+href\\s*=\\s*\"([^>]+)\">", 
				    		"$1" + "<a href="+sTmp1+"/Octc/PropertyTax/$2>");
				    
				    rsResponse = rsResponse.replaceAll("(?is)&amp;", "&");
				    rsResponse = rsResponse.replaceAll("(?is)<img[^>]+>", "");
				    //rsResponse = rsResponse.replaceAll("(?is)<center>\\s*<table id\\s*=\\s*\"\\s*tableNavigation.*?</table>\\s*</center>", "");
				    rsResponse = rsResponse.replaceAll("<\\s*/tr\\s*>","\n</tr>\n");
				    //to avoid parsing of first table row, which is the header
				    rsResponse = rsResponse.replaceFirst("(<tr)", "$1 style=\"font-weight: bold;\"");
				    
					parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.PAGE_ROWS,
							getLinkPrefix(TSConnectionURL.idGET),
							TSServer.REQUEST_SAVE_TO_TSD);
					break;
				case ID_DETAILS:
				    rsResponse = rsResponse.replaceAll("(?is)(border=\"[^\"]\")\\s+border=\"[^\"]\"", "$1");
				    
				    int index = rsResponse.indexOf("<table border=\"1\"");
				    if(index > 0) {
				    	rsResponse = rsResponse.substring(index);
				    	index = rsResponse.lastIndexOf("</table>");
				    	if(index > 0) {
				    		rsResponse = rsResponse.substring(0,index) + "</table>";
				    	}
				    } else if(rsResponse.toLowerCase().contains("<html ")){
				    	try {
					    	org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(rsResponse, null);
							NodeList nodeList = parser.parse(null);
							
							NodeList contentTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_Table1"), true)
									.elementAt(0).getParent().getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false);
							
							if(contentTables != null) {
								//contentTables.remove(contentTables.size() - 1); // remove "Search Again" Button
								rsResponse = contentTables.toHtml();
							}
				    	} catch (Exception e) {
				    		logger.error("Error while getting details", e);
				    	}
				    }
				    
				    //get detailed document addressing code
	                String keyCode = "File";
	                String parcelNo = "";
	    			Document doc = Tidy.tidyParse(rsResponse);

	    			parcelNo = HtmlParserTidy.getValueFromTagById(doc, "ctl00_mainContent_cellDisplayIdentifier", "td");
	    			String cleanParcelNo = parcelNo.replaceAll("-","").trim();
	    			if(StringUtils.isNotEmpty(cleanParcelNo))
	    			{
	    				keyCode = cleanParcelNo;
	    			} 
	    			
					String imageLink1 = StringUtils.extractParameter(rsResponse, "<a href='(ViewTaxBill[^']*)'.*?>.*?TaxBill-MouseOver.*?</a>");
					String imageLink2 = "";
					String imageLink = "";
					if (StringUtils.isEmpty(imageLink1)) {
						imageLink2 = StringUtils.extractParameter(rsResponse, "(?is)<td[^>+]id=\"imageLink[^>]*>([^<]*)</td>");
						imageLink = imageLink2;
						rsResponse = rsResponse.replaceFirst("(?is)<td[^>+]id=\"imageLink[^>]*>([^<]*)</td>", "");
					} else {
						imageLink = imageLink1;
					}
					if(!StringUtils.isEmpty(imageLink)){
						String sFileLink = keyCode + ".pdf";
						ImageLinkInPage ilip = new ImageLinkInPage(CreatePartialLink(TSConnectionURL.idGET) + getDataSite().getLink().replaceFirst("(?is)Search.aspx$", "") + imageLink, sFileLink);
						Response.getParsedResponse().addImageLink(ilip);
					}
					
					List<String> receipt = new ArrayList<String>();
					Matcher matc = RECEIPT_LINK.matcher(rsResponse);
					while (matc.find()) {
						receipt.add(matc.group(1));
					}
				    
					for (int i = 0; i < receipt.size(); i++){
				    	HTTPRequest reqP = new HTTPRequest("http://www.octaxcol.com/Octc/PropertyTax/" + receipt.get(i));
				    	HTTPResponse resP = null;
			        	HttpSite site = HttpManager.getSite("FLOrangeTR", searchId);
						try
						{
							resP = site.process(reqP);
						} finally 
						{
							HttpManager.releaseSite(site);
						}	
						try {
							String rsp = PDFUtils.extractTextFromPDF(resP.getResponseAsStream());
							if (rsp.contains("PAID")) {
								if (rsp.matches("(?is).*PAID\\s*[\\d-]+\\s+PAID\\s*[\\d-]+.*")) {
									//multiple instalments
									String tmp = rsp;
									tmp = tmp.replaceFirst("(?is).*?(((PAID)\\s*([\\d-]+)\\s*){2,}\\s+([\\$\\.,\\d]+\\s+\\d{1,2}/\\d{1,2}/\\d{4}\\s*)+).*", "$1");
									rsp = rsp.replaceFirst("(?is)(.*?)(?:(?:(?:PAID)\\s*(?:[\\d-]+)\\s*){2,}\\s+(?:[\\$\\.,\\d]+\\s+\\d{1,2}/\\d{1,2}/\\d{4}\\s*)+)(.*)", 
											"$1" + "### Extract installment payments ### \n" + "$2");
									tmp = tmp.replaceAll("\\$", "@");
									
									boolean hasInstallment = true;
									String arrangedInstallmentsInfo = "";
									int idxRecNo = tmp.lastIndexOf("PAID");
									int idxAmtPaid = tmp.lastIndexOf("@");
									int finishIdx = tmp.length();
									while (hasInstallment) {
										String txt1 = tmp.substring(idxRecNo, idxRecNo + 19);
										String txt2 = tmp.substring(idxAmtPaid, finishIdx);
										txt2 = txt2.replaceFirst("@", "\\$");
										tmp = tmp.substring(0, idxAmtPaid);
										tmp = tmp.replaceFirst("(?is)" + txt1 + "[\\r\\n]*", "").trim();
										
										arrangedInstallmentsInfo += txt1.replaceAll("[\\r\\n]", "") + " # ";
										txt2 = txt2.replaceFirst("(?is)([\\$\\.,\\d]+)\\s+(\\d{1,2}/\\d{1,2}/\\d{4}\\s*)", "$1 # $2");
										txt2 = txt2.replaceFirst("@", "\\$");
										arrangedInstallmentsInfo += txt2 + "\n";
										
										finishIdx = tmp.length();
										idxAmtPaid = tmp.lastIndexOf("@");
										idxRecNo = tmp.lastIndexOf("PAID");
										if (finishIdx == 0) {
											hasInstallment = false;
										}
									}
									arrangedInstallmentsInfo = arrangedInstallmentsInfo.replaceFirst("[\\r\\n]+", "\n");
									rsp = arrangedInstallmentsInfo;
									
								} else {
									rsp = rsp.replaceAll("(?is).*(PAID)\\s*([\\d-]+)\\s*([\\(\\)\\$\\.\\d,]+)\\s*([\\d/]+).*", "$1 $2 # $3 # $4");
								}
								
								rsp = rsp.replaceAll("(?is)[\\(\\)]+", "");
								rsp = rsp.replaceAll("(?is)\\.", "\\\\.");
								rsp = rsp.replaceAll("(?is)\\$", "DOLAR");
							} 
							
							String link = receipt.get(i).replaceAll("(?is)\\.", "\\\\.");
							link = link.replaceAll("(?is)\\?", "\\\\?");
							rsResponse = rsResponse.replaceAll("(?is)<a\\s+href\\s*=\\s*'" + link +"[^>]+>\\s*(?:<img[^>]*>)?" , (link.contains("&StillDue=true")) ? " " : rsp);
							rsResponse = rsResponse.replaceAll("(?is)DOLAR" , "\\$");
							
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					rsResponse = rsResponse.replaceAll("(?is)<a\\s+href\\s*=[^>]+>\\s*(?:<img[^>]*>)?", "");
					rsResponse = rsResponse.replaceAll("</a>", "");
				    rsResponse = rsResponse.replaceAll("(?is)<\\s*/?font[^>]*>", "");
				    rsResponse = rsResponse.replaceAll(
				    		"(?is)<td[^>]*><center>\\s*<a\\s+href[^>]+>\\s*" +
				    			"<img[^>]*>(</a>\\s*<a\\s+href[^>]+>\\s*" +
				    			"<img[^>]*>)?(\\s*</center>)?</a></td>", "");
				    
				    rsResponse = rsResponse.replaceAll("(?is)\\(View Taxbill For Receipt\\)", "");
				    rsResponse = rsResponse.replaceFirst("(?is)\\bDownload Taxbill\\b", "Taxbill Details");
				    
				    rsResponse = properArrangementOfInstallments(rsResponse, (rsResponse.contains(" - Installment ")));
				    
				    if(!StringUtils.isEmpty(imageLink1)){
				    	rsResponse = rsResponse.replaceFirst("(?is)(<td[^>]+id=\"ctl00_mainContent_cellDisplayIdentifier\"[^>]*>[^<]*</td>)",
								"$1<td id=\"imageLink\" style=\"visibility:hidden;display:none\">" + imageLink1 + "</td>");
				    }
				    	                		    		
	                if ((!downloadingForSave))
					{
						String qry_aux = Response.getRawQuerry();
						qry_aux = "dummy=" + keyCode + "&" + qry_aux;
						String originalLink = sAction + "&" + qry_aux;
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type","CNTYTAX");
						
						if(isInstrumentSaved(parcelNo, null, data)){
			                rsResponse += CreateFileAlreadyInTSD();
						} else {
							mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
							rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
						}

						Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
						parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.NO_PARSE);
					} 
					else
					{//for html
						msSaveToTSDFileName = keyCode + ".html";

						Response.getParsedResponse().setFileName(
							getServerTypeDirectory() + msSaveToTSDFileName);

						msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

					    parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS);
						
					}
					break;
				case ID_GET_LINK :
					//if (rsResponse.matches("(?is).*REAL\\s*ESTATE\\s*PROPERTY.*")/* || rsResponse.contains("TANGIBLE PERSONAL PROPERTY")*/)
						ParseResponse(sAction, Response, ID_DETAILS);
					//else
						//ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
					break;
				case ID_SAVE_TO_TSD :					
					downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
					downloadingForSave = false;
					break;
				default:
					break;

			}
		}

	

	private String properArrangementOfInstallments(String rsResponse, boolean hasInstallments) {
		if (!hasInstallments) {
			return rsResponse;
		}
		
		String correctedResp = Matcher.quoteReplacement(rsResponse);
		String regExp = "(?is)(\\d{4}\\s*)-\\s*(Installment\\s+\\d+\\s*<br>[^>]+>[^/]+/center>[^\\\"]+\\\"[^<]+<center>\\s*<span>\\s*\\\\\\$[\\d\\.,]+</span>)";
		correctedResp = correctedResp.replaceAll(regExp, "$1" + "$2");
		
		regExp = "(?is)(\\d{4}\\s*)-\\s*(Installment\\s+(\\d+)\\s*<br>[^\\*]+)(\\*\\s*PAID[^<]+)(</span>[^\\\"]+[^>]+>\\s*<center>)([^<]+)(</center>)";
		Matcher m = Pattern.compile(regExp).matcher(correctedResp);
		
		while (m.find()) {
			int installmentNo = Integer.parseInt(m.group(3).trim());
			String[] paymentInfo = m.group(6).trim().split("\n");
			String paymentValue = "";
			
			if (paymentInfo.length > 0) {
				paymentValue = paymentInfo[installmentNo -1];
				correctedResp = correctedResp.replaceFirst(regExp, m.group(1) + m.group(2) + m.group(4).replaceAll("\\*", "@") + m.group(5) + paymentValue + m.group(7));
			}
		}
		
		correctedResp = correctedResp.replaceAll("@", "\\*");
		correctedResp = correctedResp.replaceAll("\\\\\\$", "\\$");
		return correctedResp;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) 
	{
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
		
			TSServerInfoModule m = null;
			
			if( hasPin() ) {//search by Parcel Number
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				m.clearSaKeys();
				m.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);
				m.addFilter(taxYearFilter);
				
				l.add(m);
			}
			if( hasStreetNo() && hasStreet()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				m.clearSaKeys();
				m.getFunction(0).setSaKey(SearchAttributes.P_STREET_NO_NAME);
				
				m.addFilter(taxYearFilter);
				m.addFilter(addressFilter);
				m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, m));
				
				l.add(m);			
			}
			ConfigurableNameIterator nameIterator = null;
			if( hasOwner()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.clearSaKeys();
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				
				m.addFilter(taxYearFilter);
				m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, m));
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {"L F M;;"});
				
				m.addIterator(nameIterator);
				l.add(m);
	        }
		
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	protected String getFileNameFromLink(String url)
	{
		String keyCode = "File";
		if (url.contains("dummy="))
			keyCode = org.apache.commons.lang.StringUtils.substringBetween(
				url,
				"dummy=",
				"&");
		
		return keyCode+".html";
	}

}

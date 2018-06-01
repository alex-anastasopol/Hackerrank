package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
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
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class FLLeeTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave;
	
    /* ------------ -------------- ---------- pt restul detaliilor din pagina de detalii pt un anumit rezultat adus ------------------ ------------------ ------------------ */       
    private static final Pattern TaxDetail = Pattern.compile("(?is)<a\\s*href=\\s*\\\"search_detail.asp\\?SearchType=RP&TaxYear=([^&]+)&Account=([^&]+)&Option=DETAIL\\\">\\s*Tax Detail\\s*</a>");
    private static final Pattern PaymentsMode = Pattern.compile("(?is)<a\\s*href=\\s*\\\"search_detail.asp\\?SearchType=RP&TaxYear=([^&]+)&Account=([^&]+)&Option=PAYMENT\\\">\\s*Payments Made\\s*</a>");
    private static final Pattern UnpaidTaxes = Pattern.compile("(?is)<a\\s*href=\\s*\\\"search_detail.asp\\?SearchType=RP&TaxYear=([^&]+)&Account=([^&]+)&Option=UNPAID\\\">\\s*All Unpaid Taxes\\s*</a>");
    private static final Pattern TaxHistory = Pattern.compile("(?is)<a\\s*href=\\s*\\\"search_detail.asp\\?SearchType=RP&TaxYear=([^&]+)&Account=([^&]+)&Option=HISTORY\\\">\\s*Tax History\\s*</a>");
   /* ------------ -------------- ----------- --------------------------------- ------------------------------------------------- ------------------ ------------------- ----------------- */
	
    public void setServerID(int ServerID){
		super.setServerID(ServerID);
	}
	
	public FLLeeTR(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink,
			long searchId, int mid){
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException{
			
		StringBuffer sb = new StringBuffer();
		String rsResponse = Response.getResult();
	        
		switch (viParseID){
			
		case ID_SEARCH_BY_NAME :
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_ADDRESS:	
			if (rsResponse.matches("(?is).*No\\s*records\\s*match\\s*your\\s*search.*")
					|| rsResponse.matches("(?is).*sorry[^a-z]+we[\\s]+could[\\s]+not[\\s]+find[\\s]+any[\\s]+matches[\\s]+for[\\s]+your[\\s]+search.*")){
				Response.getParsedResponse().setError("No records match your search.");
				return;
			}

			rsResponse = rsResponse.replaceAll("(?is)<!--.*?-->", "");
			try{
				
				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();
				
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0){
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
				}
						
			} catch(Exception e){
				e.printStackTrace();
			}
			break;
		case ID_DETAILS:
				   if (rsResponse.matches("(?is).*Contact\\s*Delinquent\\s*Taxes\\s*Immediately.*"))
				  {
						rsResponse ="<table><th><b>Contact Delinquent Taxes Immediately!</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
					    return;
				  }
				  else if (rsResponse.matches("(?is).*No\\s*records\\s*match\\s*your\\s*search.*")
						   || rsResponse.matches("(?is).*sorry[^a-z]+we[\\s]+could[\\s]+not[\\s]+find[\\s]+any[\\s]+matches[\\s]+for[\\s]+your[\\s]+search.*"))
			      {
			    	  	rsResponse ="<table><th><b>No records match your search.</b></th></table>";
			    	  	Response.getParsedResponse().setOnlyResponse(rsResponse);
			    	  	return;
			      }

                 				   
				   //  pt editarea html-ului primit ca raspuns in urma apasarii pe 'View'
				   /*  ---------------------------------------------------------------------------------- */
				   String details = rsResponse;
				   if(rsResponse.toLowerCase().contains("<html")){	  
					   Node tbl = HtmlParserTidy.getNodeByTagAndAttr(rsResponse, "div", "style", "width: 500px; border: 1px solid #000000;");
					   String html = "";
					   try {
						   html = HtmlParserTidy.getHtmlFromNode(tbl);
					   } catch (TransformerException e) {
						e.printStackTrace();
					   }
					   details = html;
					   
					   sb = new StringBuffer(rsResponse);		  
					   String rsRespTaxDet="", rsRespPayMode="", rsRespUnpTax="", rsRespTaxHist="";
						 
					   Matcher TaxDetailMat =TaxDetail.matcher(sb);
					   Matcher PaymentsModeMat = PaymentsMode.matcher(sb);
					   Matcher UnpaidTaxesMat = UnpaidTaxes.matcher(sb);
					   Matcher TaxHistoryMat = TaxHistory.matcher(sb);	
					   List<Node> nodes ;
					  
					   if (TaxDetailMat.find()) {
						  
						  String taxYear=TaxDetailMat.group(1);
						  String parcel = TaxDetailMat.group(2);
						  
						  String url = "http://www.leetc.com/search_detail.asp?SearchType=RP&TaxYear="+taxYear+"&Account="+parcel+"&Option=DETAIL";
						  String referer = "http://www.leetc.com/search_detail.asp?account="+parcel+"&SearchType=RP&TaxYear="+taxYear;
						  HTTPRequest req1 = new HTTPRequest(url);
						  req1.setMethod( HTTPRequest.GET );
						  req1.setHeader("Referer", referer);
							
						  HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer( "FLLeeTR", searchId, miServerID).process(req1);
				          rsRespTaxDet = res.getResponseAsString();
				          try
						  {
						   	rsRespTaxDet = new String (rsRespTaxDet.getBytes(),"ISO-8859-1");// UTF-8
						  } catch (Exception e) {
						    	e.printStackTrace();
						   }
						    
						  tbl = HtmlParserTidy.getNodeByTagAndAttr(rsRespTaxDet, "div", "class", "subprint");
						  nodes = HtmlParserTidy.getNodeListByTagAndAttr(rsRespTaxDet, "div", "style", "width: 500px; border: 1px solid #000000;");
						  html = "";
						  try {
							if (nodes.size() == 2){
								html = HtmlParserTidy.getHtmlFromNode(nodes.get(1));
							} else {
								html = HtmlParserTidy.getHtmlFromNode(tbl);
							}
						  } catch (TransformerException e) {
								e.printStackTrace();
						  }
						  html = html.replaceAll("&amp;amp;", "&amp;");
						  details += "<br><br><br>" + html;
					   }	  
					   /* ----------------------------------------------------------------------------------- */
					  if (PaymentsModeMat.find()){
						  
						  String taxYear=PaymentsModeMat.group(1);
						  String parcel = PaymentsModeMat.group(2);
						  
						  String url = "http://www.leetc.com/search_detail.asp?SearchType=RP&TaxYear="+taxYear+"&Account="+parcel+"&Option=PAYMENT";
						  String referer = "http://www.leetc.com/search_detail.asp?account="+parcel+"&SearchType=RP&TaxYear="+taxYear;
						  HTTPRequest req2 = new HTTPRequest(url);
						  req2.setMethod( HTTPRequest.GET );
						  req2.setHeader("Referer", referer);
							
						  HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer( "FLLeeTR", searchId, miServerID).process(req2);
						  rsRespPayMode = res.getResponseAsString();
						  try {
						    rsRespPayMode = new String (rsRespPayMode.getBytes(),"ISO-8859-1");// UTF-8
						  } catch (Exception e) {
						    	e.printStackTrace();
						  }
						    
						  tbl = HtmlParserTidy.getNodeByTagAndAttr(rsRespPayMode, "table", "class", "tblResult");
						  nodes = HtmlParserTidy.getNodeListByTagAndAttr(rsRespPayMode, "div", "style", "width: 500px; border: 1px solid #000000;");
						  html = "";
						  try {
							if (nodes.size() == 2){
								html = HtmlParserTidy.getHtmlFromNode(nodes.get(1));
							} else {
								html = HtmlParserTidy.getHtmlFromNode(tbl);
							}
						  } catch (TransformerException e) {
								e.printStackTrace();
						  }
						  details += "<br><br><br>" + "<div style=\"width: 500px; border: 1px solid #000000;\">" + html + "</div>";
				      }	
					  /* ----------------------------------------------------------------------------------- */
					  if (UnpaidTaxesMat.find()){
						  
						  String taxYear=UnpaidTaxesMat.group(1);
						  String parcel = UnpaidTaxesMat.group(2);
						  
						  String url = "http://www.leetc.com/search_detail.asp?SearchType=RP&TaxYear="+taxYear+"&Account="+parcel+"&Option=UNPAID";
						  String referer = "http://www.leetc.com/search_detail.asp?account="+parcel+"&SearchType=RP&TaxYear="+taxYear;
						  HTTPRequest req3 = new HTTPRequest(url);
						  req3.setMethod( HTTPRequest.GET );
						  req3.setHeader("Referer", referer);
							
						  HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer( "FLLeeTR", searchId, miServerID).process(req3);
						  rsRespUnpTax = res.getResponseAsString();
						  try {
							  rsRespUnpTax = new String (rsRespUnpTax.getBytes(),"ISO-8859-1");// UTF-8
						  } catch (Exception e){
						    	e.printStackTrace();
						  }
						    
						  tbl = HtmlParserTidy.getNodeByTagAndAttr(rsRespUnpTax, "table", "class", "tblResult");
						  nodes = HtmlParserTidy.getNodeListByTagAndAttr(rsRespUnpTax, "div", "style", "width: 500px; border: 1px solid #000000;");
						  html = "";
						  try {
							if (nodes.size() == 2){
								html = HtmlParserTidy.getHtmlFromNode(nodes.get(1));
							} else {
								html = HtmlParserTidy.getHtmlFromNode(tbl);
							}
						  } catch (TransformerException e) {
								e.printStackTrace();
						  }
						  org.w3c.dom.Document doc = Tidy.tidyParse(html);
						  String inputValue = HtmlParserTidy.getValueFromInputByName(doc, "txtDate");
						  html = html.replaceAll("(?is)<form.*</form>", inputValue);
						  details += "<br><br><br>" + "<div style=\"width: 500px; border: 1px solid #000000;\">" + html + "</div>";
				      }					  
					  /* ----------------------------------------------------------------------------------- */
					  if (TaxHistoryMat.find()){
						  
						  String taxYear=TaxHistoryMat.group(1);
						  String parcel = TaxHistoryMat.group(2);
						  
						  String url = "http://www.leetc.com/search_detail.asp?SearchType=RP&TaxYear="+taxYear+"&Account="+parcel+"&Option=HISTORY";
						  String referer = "http://www.leetc.com/search_detail.asp?account="+parcel+"&SearchType=RP&TaxYear="+taxYear;
						  HTTPRequest req4 = new HTTPRequest(url);
						  req4.setMethod( HTTPRequest.GET );
						  req4.setHeader("Referer", referer);
							
				          HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer( "FLLeeTR", searchId, miServerID).process(req4);
				          rsRespTaxHist = res.getResponseAsString();
				          try {
				        	  rsRespTaxHist = new String (rsRespTaxHist.getBytes(),"ISO-8859-1");// UTF-8
						  } catch (Exception e){
						    	e.printStackTrace();
						  }
						    
						  tbl = HtmlParserTidy.getNodeByTagAndAttr(rsRespTaxHist, "table", "class", "tblResult");
						  nodes = HtmlParserTidy.getNodeListByTagAndAttr(rsRespTaxHist, "div", "style", "width: 500px; border: 1px solid #000000;");
						  html = "";
						  try {
							if (nodes.size() == 2){
								html = HtmlParserTidy.getHtmlFromNode(nodes.get(1));
							} else {
								html = HtmlParserTidy.getHtmlFromNode(tbl);
							}
						  } catch (TransformerException e) {
								e.printStackTrace();
						  }
						  details += "<br><br><br>" + "<div style=\"width: 500px; border: 1px solid #000000;\">" + html + "</div>";
						  details = details.replaceAll("(?is)<button[^>]+>\\s*<img[^>]+>\\s*</button>", "");
						  details = details.replaceAll("(?is)</?a[^>]*>", "");
				      }
				   }
					  
				  /* ----------------------------------------------------------------------------------- */ 				  
				    //get detailed document addressing code
				  String taxYear = StringUtils.extractParameterFromUrl(Response.getRawQuerry(), "TaxYear");
				  String keyCode = "File"; 
				  String pid = StringUtils.extractParameterFromUrl(Response.getRawQuerry(), "Account");
				  
	                if ((!downloadingForSave)){
						String qry_aux = Response.getRawQuerry();
						qry_aux = "dummy=" + keyCode + "&" + qry_aux;
						String originalLink = sAction + "&" + qry_aux;
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type","CNTYTAX");
						data.put("year", taxYear);

						if(isInstrumentSaved(pid, null, data)){
						    details += CreateFileAlreadyInTSD();
						} else {
							mSearch.addInMemoryDoc(sSave2TSDLink, details);
						    details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
						}

						Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
						parser.Parse(
							Response.getParsedResponse(),
							details,
							Parser.NO_PARSE);
					} else {//for html
						msSaveToTSDFileName = keyCode + ".html";

						Response.getParsedResponse().setFileName(
							getServerTypeDirectory() + msSaveToTSDFileName);

						msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

					    parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS);
						
					}
					break;

				case ID_GET_LINK :
					if (rsResponse.matches("(?is).*Real\\s*Property\\s*Information.*"))
						ParseResponse(sAction, Response, ID_DETAILS);
					else
						ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);							
				break;
				case ID_SAVE_TO_TSD :
					if (sAction.equals("/PropertySearch.aspx"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
					else
					{// on save
						downloadingForSave = true;
						ParseResponse(sAction, Response, ID_DETAILS);
						downloadingForSave = false;
					}
					break;
				default:
					break;
			}
		}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			table = Tidy.tidyParse(table, null);
			table = table.replaceFirst("(?i)(<table.*?class=\\\"tblResult\\\")", "$1 id=\"tblResult\" ");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "tblResult"), true).elementAt(0);
			
			if(mainTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = mainTable.getRows();
			
			String header = "<table class=\"tblResult\" style=\"width: 95%;\" cellpadding=\"0\" cellspacing=\"0\" border=\"1\">";

			for (int i = 1; i < rows.length; i++){
				if (i == 1){
					header += rows[1].toHtml();
					continue;
				}
				
				if(rows[i].getColumnCount() == 4) {
					
					TableColumn[] cols = rows[i].getColumns();
					NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					
					String pin = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0).getChildren().toHtml();
					
					String link = CreatePartialLink(TSConnectionURL.idGET) + "/" + ((LinkTag) aList.elementAt(0)).extractLink();
					String rowHtml =  rows[i].toHtml();
					rowHtml = rowHtml.replaceAll("(?is)<img[^>]+>", "");
					rowHtml = rowHtml.replaceAll("(?is)</?a[^>]*>", "");
					rowHtml = rowHtml.replaceAll("(?is)\\A\\s*(<tr[^>]*>\\s*<td[^>]*>\\s*<span[^>]*>)([^<]*)(</span[^>]*>)", "$1<a href=\"" + link + "\">$2</a>$3");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
													
					ResultMap resultMap = new ResultMap();
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
					
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin.trim());
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), pin.trim().replaceAll("(?is)[\\s-]+", ""));
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), pin.trim().replaceAll("(?is)[\\s-]+", ""));
					
					try {
						String year = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true).elementAt(0).getChildren().toHtml();
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year.trim());
					} catch (Exception e) {
						logger.error("Exception throwed when parsing tax year in intermediary on FLLeeTR, searchId: " + searchId);
					}
					
					
					try {
						String nameAddress = cols[2].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true).elementAt(0).getChildren().toHtml();
						if (StringUtils.isNotEmpty(nameAddress)) {
							String[] parts = nameAddress.split("\\s*<br>\\s*");
							String names = "";
							if (parts.length > 1){
								for (int p = 0; p < parts.length - 1; p++) {
									names += "   " + parts[p];
								}
								
								if (StringUtils.isNotEmpty(names)){
									resultMap.put("tmpOwnerName", names);
									ro.cst.tsearch.servers.functions.FLLeeTR.partyNamesInterFLLeeTR(resultMap, searchId);
								}
								String address = parts[parts.length - 1];
								if (StringUtils.isNotEmpty(address)){
									resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
									resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()));
								}
							}
						}
					} catch (Exception e) {
						logger.error("Exception throwed when parsing name/address in intermediary on FLLeeTR, searchId: " + searchId);
					}
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					resultMap.removeTempDef();
						
					DocumentI document = (TaxDocumentI) bridge.importData();						
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}	        
		    String sTmp1 = CreatePartialLink(TSConnectionURL.idPOST);
		    
		    String lnStr = "/search_results.asp", lpStr = "/search_results.asp", alnStr="<a><b>Next</b></a>", alpStr="<a><b>Prev</b></a>";
		    		  
		    Form form = new SimpleHtmlParser(response.getResult()).getForm("PageButton");
			Map<String, String> paramsLink = form.getParams();

			if (paramsLink != null){
				String cPageStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("Current_Page"));
				if (StringUtils.isEmpty(cPageStr)){
					cPageStr = "1";
				}
				String tPageStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("Total_Page"));
				String tRecordsStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("total_records"));
				String pPageStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("per_page"));
				String sOnStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("searchon"));
				String yearStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("queryAddl"));
				String accountStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("query1"));
				String streetNrStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("query2"));
				String extensiveOptStr = org.apache.commons.lang.StringUtils.defaultString(paramsLink.get("queryx10sv"));
				
				int pageNr = Integer.parseInt(cPageStr);
			    int totalPages = Integer.parseInt(tPageStr);
			    if (totalPages > 1){
				    int year = Integer.parseInt(yearStr);
				    if (pageNr < totalPages){
				    	lnStr += "&Current_Page=" + pageNr + "&Total_Page=" + tPageStr + "&total_records=" + tRecordsStr + "&per_page=" + pPageStr + "&PageAction=Next" + 
				    			"&searchon=" + sOnStr + "&searchtype=RP&criteriapage=search_criteria.asp&resultpage=search_results.asp&detailproc=search_detail.asp&queryAddl=" + 
				    			year + "&query1=" + accountStr + "&PerPage=" + pPageStr;
				    }
				    if (pageNr > 1){
				    	lpStr += "&Current_Page=" + pageNr + "&Total_Page=" + tPageStr + "&total_records=" + tRecordsStr + "&per_page=" + pPageStr + "&PageAction=Prev" + 
				    			"&searchon=" + sOnStr + "&searchtype=RP&criteriapage=search_criteria.asp&resultpage=search_results.asp&detailproc=search_detail.asp&queryAddl=" + 
				    			year + "&query1=" + accountStr + "&PerPage=" + pPageStr;
				    }
				    if (streetNrStr != ""){
				    	lnStr += "&query2=" + streetNrStr;
				    	lpStr += "&query2=" + streetNrStr;
				    }
				    if (extensiveOptStr != ""){
				    	lnStr += "&queryx10sv=on";
				    	lpStr += "&queryx10sv=on";
				    }
				    alnStr = "<a href=\"" + sTmp1 + lnStr + "\">" + "<b>Next</b></a>";
				    
				    if (Search.AUTOMATIC_SEARCH == mSearch.getSearchType() && !"<a><b>Next</b></a>".equals(alnStr)){
				    	response.getParsedResponse().setNextLink(alnStr);
				    }
				    alpStr = "&nbsp; &nbsp; &nbsp; <a href=\"" + sTmp1 + lpStr + "\">" + "<b>Prev</b></a>";
			    }
			}
			
			response.getParsedResponse().setHeader(alpStr + "&nbsp;&nbsp;" + alnStr + "<br><br>" + header);
			response.getParsedResponse().setFooter("</table><br><br>");			
			
			outputTable.append(table);
			outputTable.append(table);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo){
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule m;
		FilterResponse adressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		
		if(hasPin()){//search by Parcel Number
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			
			l.add(m);
		}

		if (hasStreet()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			
			m.addFilter(NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
			m.addFilter(adressFilter);
			
			l.add(m);
		}
		
		if(hasOwner()){//Search by Owners Name
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			m.addFilter(NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
			m.addFilter(adressFilter);
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;F;","L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);
			
			l.add(m);
        }
		serverInfo.setModulesForAutoSearch(l);
	}

	protected String getFileNameFromLink(String url)
	{
		String keyCode = "File";
		if (url.contains("dummy="))
			keyCode = org.apache.commons.lang.StringUtils.substringBetween(
				url, "dummy=", "&");
		
		return keyCode+".html";
	}
}

/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 * @new modifications by Olivia 
 * 
 * ADD here the new county implemented with this Generic
 * 
 * for  Alachua, Broward, Charlotte, Citrus, Indian River, Lake, Miami-Dade, 
 * Monroe, Okaloosa, Osceola, Pinellas, St Lucie, Sumter, Volusia -like sites        
 *
 */
public class FLGenericGrantStreet3TR extends TSServer {
	
	private static final long serialVersionUID = 6482214335164602005L;
	private static final Pattern ASSESSOR_LINK_PAT = Pattern.compile("(?i)<a.*?href\\s*=\\s*\\\"([^\\\"]+)\\\".*?>\\s*Property Appraiser");

	/**
	 * @param searchId
	 */
	public FLGenericGrantStreet3TR(long searchId) {
		super(searchId);
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public FLGenericGrantStreet3TR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {
		
		String rsResponse = response.getResult();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
				
				// no result
				if (rsResponse.indexOf("No accounts matched your search") > -1) {
					response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				Matcher m = Pattern.compile("This account is currently not available for viewing. Please contact the Tax " +
							"Collector's office for additional information.")
							.matcher(rsResponse.replaceAll("<a[^>]*>(.*)</a>", "$1").replaceAll("\\s+", " "));
				if (m.find()) {
					response.getParsedResponse().setError(m.group());
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				String nextLink = getNextLink(rsResponse);
				String prevLink = getPrevLink(rsResponse);
				
				String prevAndNext = "<div align=\"center\">";
				if (!"Prev".equals(prevLink)) 
					prevAndNext += prevLink + "&nbsp;&nbsp;&nbsp;";
				if (!"Next".equals(nextLink)) 
					prevAndNext += nextLink;
				prevAndNext += "</div>";
				
				//parsedResponse.setHeader("<div align=\"center\">" + prevLink + "&nbsp;&nbsp;&nbsp;" + nextLink + "</div><table width=\"100%\" border=\"1\">");	
				//parsedResponse.setFooter("</table><div align=\"center\">" + prevLink + "&nbsp;&nbsp;&nbsp;" + nextLink + "</div>");
				parsedResponse.setHeader(prevAndNext + "<table width=\"100%\" border=\"1\">");
				parsedResponse.setFooter("</table>" + prevAndNext);
				parsedResponse.setNextLink(nextLink);
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("Notice of Ad Valorem Taxes and Non-ad Valorem Assessments") > -1
						|| rsResponse.indexOf("roll details") > -1) {
					ParseResponse(sAction, response, ID_DETAILS);
				} else {
					ParseResponse(sAction, response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				details = details.replaceAll("(?is)<img[^>]*>", "");
				
				if(viParseID == ID_DETAILS) {
					//test;  the 3 lines below is for testing purposes; they should be commented before commit
					if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
//						parseAndFillResultMap(response, details, new ResultMap());
					}	

					String originalLink = sAction.replace("?", "&") + "&" + response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					if (isInstrumentSaved(accountId.toString().replaceAll("/", "").trim(), null, data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);
				} else {
					String filename = accountId.toString().replaceAll("/", "") + ".html";
					smartParseDetails(response,details);
					
					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					parsedResponse.setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
		}
		
	}
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.FLGenericGrantStreet3TR.parseAndFillResultMap(detailsHtml, map, searchId);
		return null;
	}
		
	@SuppressWarnings("unchecked")
	private String getDetails(String page, StringBuilder accountId) {
		
		if(!page.toLowerCase().contains("<html")){
			return page;
		}
		
		StringBuilder details = new StringBuilder();
//		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
		 
		String siteLink = "";
		//if ("alachua".equalsIgnoreCase(crtCounty) || "indian river".equalsIgnoreCase(crtCounty)){////when is no page for current year
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		siteLink = site.getSiteLink();
		HttpManager.releaseSite(site);
		//}
		
		try {
			
			page = page.replaceAll("(?is)(\\\"standard-table bill-identifiers )no-alt-key\\\"", "$1\"");//for St Lucie
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nl = htmlParser.parse(new TagNameFilter("body"));
			
			Div divContent = (Div) nl.extractAllNodesThatMatch(new HasAttributeFilter("id", "content"), true).elementAt(0);
			
			if (countyId == CountyConstants.FL_Alachua) {
				NodeList nodeList = divContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "property-tax-navigation"), true);
				
				LinkTag linkTag = (LinkTag) nodeList
						.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("src", "/shared-static/images/icons/receipts-text.png")), true)
						.elementAt(0);
				
				Matcher ma = Pattern.compile("(\\d{4}) information is not available yet\\.").matcher(page);//there is no page for current year 
				if (ma.find()) {
					String yearString = ma.group(1);
					int year = Integer.parseInt(yearString);
					year--;
					String linkExtractedToBeUsed = linkTag.extractLink();
					if (StringUtils.isNotEmpty(linkExtractedToBeUsed)){
						linkExtractedToBeUsed = linkExtractedToBeUsed.replaceAll("(?is)/bills$", "");
						String parcelDetailsLink = siteLink + linkExtractedToBeUsed + "?year=" + year;
						page = getLinkContents(parcelDetailsLink);
						
						if (StringUtils.isNotEmpty(page)){
							htmlParser = org.htmlparser.Parser.createParser(page, null);
							nl = htmlParser.parse(new TagNameFilter("body"));
							
							divContent = (Div) nl.extractAllNodesThatMatch(new HasAttributeFilter("id", "content"), true).elementAt(0);
							nodeList = divContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "column-container parcel-misc-data"), true);
							
							linkTag = (LinkTag) nodeList
									.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("src", "/shared-static/images/icons/receipt-invoice.png")), true)
									.elementAt(0);
							page = getLinkContents(siteLink + linkTag.extractLink());
						}
					}
				}
			}

			htmlParser = org.htmlparser.Parser.createParser(page, null);
			nl = htmlParser.parse(new TagNameFilter("body"));

			divContent = (Div) nl.extractAllNodesThatMatch(new HasAttributeFilter("id", "content"), true).elementAt(0);
			
			//nodeList.keepAllNodesThatMatch(new TagNameFilter("body"), true);
			//htmlParser = org.htmlparser.Parser.createParser(nodeList.toHtml(), null);
			//nodeList = htmlParser.parse(null);
			NodeList nodeList = divContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "individual-bill individual-bill-re"), true);
						
			if (nodeList != null && nodeList.size() > 0){
				nodeList = nodeList.elementAt(0).getChildren();
			} else{
				nodeList = divContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "individual-bill individual-bill-installment"), true);
				if (nodeList != null && nodeList.size() > 0){
					nodeList = nodeList.elementAt(0).getChildren();
				} else {
					nodeList = divContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "individual-bill individual-bill-tp"), true);
					if (nodeList != null && nodeList.size() > 0){
						nodeList = nodeList.elementAt(0).getChildren();
					}
				}
			}
			
			
			
			// get general info
			TableTag table1 = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "standard-table bill-identifiers "), true)
				.elementAt(0);
			//for these counties column 1 (Parcel Number) is taken
			if (countyId == CountyConstants.FL_Citrus || countyId == CountyConstants.FL_Monroe || countyId == CountyConstants.FL_Pinellas) 		
				accountId.append(table1.getRow(1).getColumns()[1].toPlainTextString().replaceAll("[\\s/]", ""));
			else
				accountId.append(table1.getRow(1).getColumns()[0].toPlainTextString().replaceAll("[\\s/]", ""));
			if(page.indexOf("<html") < 0) {
				return page;
			}
			
			//get label with tax year
			Div divBillLabel = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "bill-label"), true)
								.elementAt(0);
			if (divBillLabel != null){
				divBillLabel.setAttribute("id", "billLabel");
				divBillLabel.setAttribute("width", "80%");
				divBillLabel.setAttribute("align", "center");
				divBillLabel.setAttribute("border", "1");
				divBillLabel.setAttribute("style", "\"color:#266B85;\"");
				String divBill = divBillLabel.toHtml();
				divBill = divBill.replaceAll("(?s)<form.*?</form>", "");
				details.append("<h2>");
				details.append(divBill);
				details.append("</h2>");
				details.append("<br><br>");
			}
			
			table1.setAttribute("id", "info_tb1");
			table1.setAttribute("width", "80%");
			table1.setAttribute("align", "center");
			table1.setAttribute("border", "1");
			details.append(table1.toHtml()
					.replaceAll("(?is)<a\\s.*?>(.*?)</a>", "$1"));
			details.append("<br><br>");
			
			Div divReceipt = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "column-container"), true)
											.elementAt(0);
			if (divReceipt != null){
				divReceipt.setAttribute("id", "infoPaid");
				divReceipt.setAttribute("width", "80%");
				divReceipt.setAttribute("align", "center");
				divReceipt.setAttribute("border", "1");
				details.append(divReceipt.toHtml().replaceAll("(?is)<form.*?</form>", "")
												.replaceAll("(?is)<a\\s.*?>([^<]*)</a>", "").replaceAll("(?is)<ul.*?</ul>", ""));
				details.append("<br><br>");
			}
			
			// get complete legal description
			LinkTag linkTag = (LinkTag) nodeList
				.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("src", "/shared-static/images/icons/map.png")), true)
				.elementAt(0);
			
			
			
			String parcelDetailsLink = siteLink + linkTag.extractLink();
			String parcelDetails = getLinkContents(parcelDetailsLink);
			
			if (countyId == CountyConstants.FL_Indian_River) {
				Matcher ma = Pattern.compile("(\\d{4}) information is not available yet\\.").matcher(parcelDetails);
				if (ma.find()) {															//there is no page for current year
					String yearString = ma.group(1);										//so we take the legal description from 
					int year = Integer.parseInt(yearString);								//the previous year
					year--;
					parcelDetailsLink += "?year=" + year;
					parcelDetails = getLinkContents(parcelDetailsLink);
				}
			}
			
			org.htmlparser.Parser htmlParser1 = org.htmlparser.Parser.createParser(parcelDetails, null);
			NodeList nodeList1 = htmlParser1.parse(null);
			
			String legal = "";
			NodeList legalList = nodeList1
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "parcel-legal-description"), true);
			if (legalList.size()>0) {
				legal = legalList.elementAt(0).toPlainTextString().trim();
			}
			
			// get other info
			NodeList nl1 = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "vertical-deflist"), true);
			String legalHtml = nl1.elementAt(3).toHtml();
			
			if (StringUtils.isNotEmpty(legal)){
				// replace with full legal
				legalHtml = legalHtml.replaceAll("(?is)(<dd class=\"legal-description\">).*?</dd>", "$1" + legal + "</dd>");
			}
			
			// keep only address and legal
			legalHtml = legalHtml
				.replaceFirst("(?is).*(<dt>\\s*Situs\\s+address.*?</dd>).*?(<dt>\\s*Legal\\s+description.*?</dd>).*", "<dl>$1$2</dl>");
			
			details.append("<table id=\"info_tb2\" width=\"80%\" align=\"center\" border=\"1\"><tr><td>" 
					+ nl1.elementAt(2).toHtml() 
					+ legalHtml
					+ "</td></tr></table>");
			details.append("<br><br>");
			
			TableTag table2 = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "standard-table bill-taxes advalorem"), true)
				.elementAt(0);
			table2.setAttribute("id", "tax_tb1");
			table2.setAttribute("width", "80%");
			table2.setAttribute("align", "center");
			table2.setAttribute("border", "1");
			details.append(table2.toHtml());
			details.append("<br><br>");
			
			TableTag table3 = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "standard-table bill-taxes nonadvalorem"), true)
				.elementAt(0);
			table3.setAttribute("id", "tax_tb2");
			table3.setAttribute("width", "80%");
			table3.setAttribute("align", "center");
			table3.setAttribute("border", "1");
			details.append(table3.toHtml());
			details.append("<br><br>");
			
			ParagraphTag combinedAssess = (ParagraphTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "combined"), true)
											.elementAt(0);
			if (combinedAssess != null){
				combinedAssess.setAttribute("id", "combinedAssessement");
				combinedAssess.setAttribute("width", "80%");
				combinedAssess.setAttribute("align", "center");
				combinedAssess.setAttribute("border", "1");
				details.append("<b> <div style=\"color:#266B85;\">");
				details.append(combinedAssess.toHtml());
				details.append("</div></b>");
			}
			
			TableTag tablePayingStatus = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "bill-if-paid-by"), true).elementAt(0);
			
			if (tablePayingStatus == null){
				tablePayingStatus = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "standard-table bill-taxes "), true).elementAt(0);
			}
			if (tablePayingStatus != null){
				tablePayingStatus.setAttribute("id", "bill-if-paid-by");
				tablePayingStatus.setAttribute("width", "80%");
				tablePayingStatus.setAttribute("align", "center");
				tablePayingStatus.setAttribute("border", "1");
				details.append(tablePayingStatus.toHtml());
				details.append("<br><br>");
			}

			Div divPayingStatus = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "validation-inline"), true).elementAt(0);
			if (divPayingStatus == null){
				divPayingStatus = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "bill-validation"), true).elementAt(0);
			}
			if (divPayingStatus != null){
				divPayingStatus.setAttribute("id", "validation-inline");
				divPayingStatus.setAttribute("width", "100%");
				divPayingStatus.setAttribute("align", "center");
				divPayingStatus.setAttribute("border", "1");
				details.append("<h3 align=\"center\"> <font style=\"color:#266B85;\"> Tax summary: <br> </font> </h3>");
				details.append(divPayingStatus.toHtml());
				details.append("<br><br>");
			}
			
			NodeList nodeListLinks = divContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "property-tax-navigation"), true).elementAt(0)
									.getChildren();
			// get tax info
			linkTag = (LinkTag) nodeListLinks
				.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("src", "/shared-static/images/icons/receipts-text.png")), true)
				.elementAt(0);
			String taxLink = siteLink + linkTag.extractLink();
			String taxPage = getLinkContents(taxLink);
			
			org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(taxPage, null);
			NodeList nodeList2 = htmlParser2.parse(null);
			
			TableTag taxTable = (TableTag) nodeList2
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "standard-table striped-bodies bill-history"), true)
				.elementAt(0);
			taxTable.setAttribute("id", "tax_tb3");
			taxTable.setAttribute("width", "80%");
			taxTable.setAttribute("align", "center");
			taxTable.setAttribute("border", "1");
			String taxHtml = taxTable.toHtml();
			taxHtml = taxHtml.replaceAll("(?is)<tr class=\"(footer|year-divider)\">.*?</tr>", "")
				.replaceAll("(?is)<td class=\"action\"[^>]*>.*?</td>", "")
				.replaceAll("(?is)<a\\s.*?>(.*?)</a>", "$1")
				.replaceAll("(?is)<form.*?</form>", "");
			details.append("<h2 align=\"center\"> <font style=\"color:#266B85;\"> Tax History: </font> </h2>");
			details.append(taxHtml);
			
			if (countyId == CountyConstants.FL_Citrus || countyId == CountyConstants.FL_Osceola || countyId == CountyConstants.FL_Okaloosa
				|| countyId == CountyConstants.FL_Charlotte || countyId == CountyConstants.FL_Pinellas 	|| countyId == CountyConstants.FL_Lake)
			{
				Map<String, String> assessorLinks = (Map<String, String>) mSearch.getAdditionalInfo(getCurrentServerName() + ":paramsAssessorLinks:");
				if (assessorLinks != null) {
					String assessorLink = assessorLinks.get(accountId.toString());
					if (StringUtils.isNotEmpty(assessorLink)){
						if (countyId == CountyConstants.FL_Okaloosa){
							assessorLink = assessorLink.replaceAll("(?is)[^=]+=", "http://www.qpublic.net/cgi-bin/okaloosa_display.cgi?KEY=");
						}
						HttpSite siteAssessor = HttpManager.getSite(getCurrentServerName(), searchId);
						try {
							String saleHistTable = ((ro.cst.tsearch.connection.http2.FLGenericGrantStreet3TR)siteAssessor).getPage(assessorLink);
							saleHistTable = saleHistTable.replaceAll("(?is)</?span[^>]*>", "");
							saleHistTable = saleHistTable.replaceAll("(?is)<table([^>]*>\\s*<tr[^>]*>\\s*<td[^>]*>\\s*Sales History)", "<table id=\"saleHistory\" $1");
							htmlParser = org.htmlparser.Parser.createParser(Tidy.tidyParse(saleHistTable,null), null);
							NodeList mainList = htmlParser.parse(null);
							NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);			
							String table = "";
							for (int k = tables.size() - 1; k >= 0; k--){
								if (tables.elementAt(k).toHtml().contains("Sale Price") || tables.elementAt(k).toHtml().contains("Deed Type")
										|| tables.elementAt(k).toHtml().contains("Selling Price") || tables.elementAt(k).toHtml().contains("Sale Date")){
									table = tables.elementAt(k).toHtml();
									break;
								}
							}
							
							if (countyId == CountyConstants.FL_Osceola) {
								table = "";
							}
							
							table = table.replaceAll("(?is)<table[^>]*>", "<TABLE id=\"saleHistory\" border=\"1\" cellspacing=\"1\" cellpadding=\"2\" width=\"100%\">");
							table = table.replaceAll("(?is)<img[^>]*>", "");
							table = table.replaceAll("(?is)</?a[^>]*>", "");
							details.append("<br><br>" + table);
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							HttpManager.releaseSite(siteAssessor);
						}
					}
				}
			}
			
				
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String detail = details.toString();
		detail = detail.replaceAll("(?is)SAVE TIME PAY ONLINE @ WWW\\.ACTCFL\\.ORG", "");
		
		return detail;
	}
	
	private String getNextLink(String page) {
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			LinkTag linkTag = (LinkTag) nodeList
				.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("title", "More")), true)
				.elementAt(0);
			
			if(linkTag != null) {
				return "<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim() + "\">Next</a>";
			} else {
				return "Next";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String getPrevLink(String page) {
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			LinkTag linkTag = (LinkTag) nodeList
				.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("title", "Back")), true)
				.elementAt(0);
			
			if(linkTag != null) {
				return "<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim() + "\">Prev</a>";
			} else {
				return "Prev";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
	Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			//table = table.replaceAll("(?is)<dl class=\"vertical-deflist\">", "$0<dt>Account Number</dt>");
			table = table.replaceAll("(?is)<form.*?</form>", "");
			table = table.replaceAll("(?is)&mdash;", "-");
			table = table.replaceAll("(?is)<img\\s+[^>]+>", "");
			Map<String, String> assessorLinks = new HashMap<String, String>();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "results"), true).elementAt(0);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			Pattern TBODY_PAT = Pattern.compile("(?is)(<tbody[^>]*>.*?</tbody>)");
			Matcher mat = TBODY_PAT.matcher(interTable.toHtml());
			
//			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
			 
			while (mat.find()){
				String tbody = mat.group(1).replaceAll("(?is)<tbody[^>]*>", "<table>").replaceAll("(?is)</tbody[^>]*>", "</table>");
				org.htmlparser.Parser htmlParserT = org.htmlparser.Parser.createParser(tbody, null);
				NodeList mainTableListT = htmlParserT.parse(null);
				//mainTableListT.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "actions")), true);
				
				TableTag mainTableT = (TableTag) mainTableListT.elementAt(0);
				
				TableRow[] rows = mainTableT.getRows();
				
				ResultMap resultMap = new ResultMap();
				
				int firstRealRow = 0;
				if (rows[0].toHtml().contains("class=\"past-year\"")){
					firstRealRow++;
				}
				
				LinkTag linkTag = (LinkTag) rows[firstRealRow].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(link);
								
				String backupName = "";
				
				for (int i = firstRealRow; i < rows.length; i++){
					String rowType = rows[i].getAttribute("class");
					TableColumn[] cols = rows[i].getColumns();
					if (rowType.indexOf("identifier") > -1){
						if (countyId == CountyConstants.FL_Pinellas || countyId == CountyConstants.FL_Miami_Dade || countyId == CountyConstants.FL_Sumter) {
							backupName = cols[0].toHtml();
							backupName = backupName.substring(0, backupName.indexOf(" -"));
							backupName = backupName.replaceAll("<td.*>", "").replaceAll("<a.*>", "").trim();
						} else if(countyId == CountyConstants.FL_Citrus) {
							backupName = cols[0].toPlainTextString().replaceFirst("(?is)-.*", "").trim();
						}
							
						String address = cols[0].toHtml();
						if (address.contains(" at ") || address.contains(" #")){
							address = address.replaceAll("(?is)</?span[^>]*>", "");
							address = address.replaceAll("(?is).*?\\s+(?:at\\s+|#)([^<]*)</a>.*", "$1").trim();
							resultMap.put("tmpAddress", address);
							if (cols[0].toHtml().toLowerCase().contains("real estate")){
								resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
							}
						}
					} else if (rowType.indexOf("data") > -1){
						String ownerName = cols[0].getChildrenHTML().replaceAll("(?is)</?span[^>]*>", "").trim();
						if (countyId == CountyConstants.FL_Pinellas || countyId == CountyConstants.FL_Miami_Dade) {						
							//sometimes in the table row with class="data"																
							int br2Position = ownerName.indexOf("<br/>");					//there is only the address, so the name is
							String firstLineownerName ="";									//taken from the row with class="identifier"
							if (br2Position!=-1) 
								firstLineownerName = ownerName.substring(0, br2Position);
							if (firstLineownerName.indexOf("<br>") == -1) 
								ownerName = backupName + "<br>" + ownerName;
						} else if(countyId == CountyConstants.FL_Citrus) {
							ownerName = backupName;
						} else if(countyId == CountyConstants.FL_Indian_River) {			
							//sometimes instead of name and address is legal description
							org.htmlparser.Node textNode = rows[i].getPreviousSibling();//e.g. Account Number 32-39-13-00001-0000-00075/0
							if (textNode!=null) {										//so we take the name from <a>
								textNode = textNode.getPreviousSibling();
								if (textNode!=null && !textNode.toPlainTextString().toLowerCase().contains("address")) {
									org.htmlparser.Node nameNode = textNode.getPreviousSibling();
									if (nameNode!=null) {
										nameNode = nameNode.getPreviousSibling();
										if (nameNode!=null) {
											String name = nameNode.toPlainTextString();
											name = name.replaceAll("\\s-\\s.*", "").trim();
											ownerName = name;
										}
									}
								}
							}
						}
						
						resultMap.put("tmpOwner", ownerName);						
																					
					} else if (rowType.indexOf("actions") > -1){
						String accountNumber = "", geoNo = "";
						Span spanList = (Span) cols[0].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "identifiers"), true)
								.elementAt(0);
						if (spanList != null){
							cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
							cols[0].getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("a")), true);
							cols[0].getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("ul")), true);
							cols[0].getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("li")), true);
							NodeList sL = spanList.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"));
							if (sL.size() == 1){
								Span spanAcc = (Span) sL.elementAt(0);
								accountNumber = spanAcc.getStringText();
								if (StringUtils.isNotEmpty(accountNumber)){
									accountNumber = accountNumber.replaceAll("(?is)</?span[^>]*>", "").trim();
									resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber);
								}
							} else if (sL.size() == 2){
								Span spanAcc = null;
								//for these counties Parcel Number is taken
								if (countyId == CountyConstants.FL_Citrus || countyId == CountyConstants.FL_Monroe || countyId == CountyConstants.FL_Pinellas) 		
								{													 
									spanAcc = (Span) sL.elementAt(1);
									accountNumber = spanAcc.getStringText().replaceAll("[\\s/]", "");
								}	
								else
								{
									spanAcc = (Span) sL.elementAt(0);
									accountNumber = spanAcc.getStringText().replaceAll("[\\s/]", "");
								}	
								if (StringUtils.isNotEmpty(accountNumber)){
									accountNumber = accountNumber.replaceAll("(?is)</?span[^>]*>", "").trim();
									resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber);
								}
								if (countyId != CountyConstants.FL_Alachua){
									Span spanParc = (Span) spanList.childAt(1);
									geoNo = spanParc.getStringText();
									if (StringUtils.isNotEmpty(geoNo)){
										geoNo = geoNo.replaceAll("(?is)</?span[^>]*>", "").trim();
										resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNo);
									}
								}
							}
						}
						Matcher aoMat = ASSESSOR_LINK_PAT.matcher(tbody);
						if (aoMat.find()){
							assessorLinks.put(accountNumber, aoMat.group(1));
						}
						
					} 
					
				}
				String rowHtml =  mainTableListT.elementAt(0).toHtml().replaceAll("(?is)<table>", "<tbody>").replaceAll("(?is)</table>", "</tbody>");
					
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				rowHtml = rowHtml.replaceAll("<img src=\"/shared-static/images/icons/shield.png\" alt=\"\">", "");
				currentResponse.setOnlyResponse(rowHtml);
				currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
												
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
				
				if (countyId == CountyConstants.FL_Indian_River) {
					ro.cst.tsearch.servers.functions.FLIndianRiver.stdFLIndianRiverTR(resultMap, searchId);
				} else { 
					ro.cst.tsearch.servers.functions.FLGenericGrantStreet3TR.parseNamesIntermediary(resultMap, searchId);
				}
				ro.cst.tsearch.servers.functions.FLGenericGrantStreet3TR.parseAddress(resultMap);
				
				Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
				resultMap.removeTempDef();
					
				DocumentI document = (TaxDocumentI) bridge.importData();						
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsAssessorLinks:", assessorLinks);
			outputTable.append(table);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		
		String link = getBaseLink();
		msiServerInfoDefault.setServerAddress(link.replaceAll("(?is)[^:]+://", ""));
		msiServerInfoDefault.setServerIP(link.replaceAll("(?is)[^:]+://", ""));
		msiServerInfoDefault.setServerLink(link);
		
		if(tsServerInfoModule != null) {
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					if (miServerID == 320702){//Alachua
						htmlControl.setFieldNote("(e.g. Smith John / 05917 009 000 / 11206 NW 61ST TER)");
					} else if (miServerID == 321202){//Broward
						htmlControl.setFieldNote("(e.g. Smith Barry / 494130-A6-0070 / 10208 NW 24 PL)");
					} else if (miServerID == 321402){ //Charlotte
						htmlControl.setFieldNote("(e.g. Smith John / 402116403014 / 1390 RONA DR)");
					} else if (miServerID == 321502){ //Citrus
						htmlControl.setFieldNote("(e.g. Smith John / 1282357 / 52 LOURAE DR)");
					} else if (miServerID == 323602){ //Indian River
						htmlControl.setFieldNote("(e.g. Smith John / 33-40-06-00002-0000-00025/1  / 1934 ROBALO DR)");
					} else if (miServerID == 324002){ //Lake
						htmlControl.setFieldNote("(e.g. Smith John / 0419241000-000-08900 / 704 SMITH ST)");
					} else if (miServerID == 324902){ //Miami Dade
						htmlControl.setFieldNote("(e.g. Smith John / 30-4026-006-0190  / 13000 SAN MATEO ST)");
					} else if (miServerID == 325002){ //Monroe
						htmlControl.setFieldNote("(e.g. Smith John / 1512958 / 153 SEBRING DR)");
					} else if (miServerID == 325202){ //Okaloosa
						htmlControl.setFieldNote("(e.g. Smith Daniel / 333N232269000C0020 / 303 JOHN KING RD)");
					} else if (miServerID == 325502){ //Osceola
						htmlControl.setFieldNote("(e.g. Smith John / R252528-164800010940 / 4434 PHILADELPHIA CIR )");
					} else if (miServerID == 325802){ //Pinellas
						htmlControl.setFieldNote("(e.g. Smith John / R442202 / 460 HARBOR DR)");
					} else if (miServerID == 326502){ //St Lucie
						htmlControl.setFieldNote("(e.g. Smith John / 2401-811-0013-000/1  / 1831 CRESTVIEW DR)");
					} else if (miServerID == 326602) { //Sumter
						htmlControl.setFieldNote("(e.g. Powell Lenard / D29-093  / 103 SUGAR MAPLE AVE)");
					} else if (miServerID == 327002){ //Volusia
						htmlControl.setFieldNote("(e.g. Smith John / 741615D20050 / 520 PENINSULA)");
					}
				}
			}
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// add punctuation to pin if necessary 
        if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX){
        	String pin = module.getFunction(0).getParamValue();
        	if (miServerID == 326502){ //St Lucie
        		if (pin.matches("[\\d-/]+")){
        			if (pin.length() >= 15){
        				pin = pin.replaceAll("(?is)\\p{Punct}", "");
        				pin = pin.replaceAll("(?is)\\A(\\d{4})(\\d{3})(\\d{4})(\\d{3})(\\d)$", "$1-$2-$3-$4/$5");
        			}
        		}
        	} else if (miServerID == 325802){ //Pinellas
        		if (!pin.contains("/")){
        			if (pin.length() >= 15){
        				pin = pin.replaceAll("(?is)\\p{Punct}", "");
        				pin = pin.replaceAll("(?is)\\A(\\d{2})(\\d{2})(\\d{2})(\\d{5})(\\d{3})(\\d{4})$", "$1/$2/$3/$4/$5/$6");
        			}
        		}
        	}
        	
           	module.getFunction(0).setParamValue(pin);
          
        }
        return super.SearchBy(module, sd);
	}
	
	protected void saveTestDataToFiles(ResultMap map) {
		
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String pin = "" + map.get("PropertyIdentificationSet.ParcelID");
			String text = pin + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") + "\r\n\r\n\r\n";
			String path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);

			// text = pin + "\r\n" +
			// map.get("PropertyIdentificationSet.AddressOnServer") +
			// "\r\n\r\n\r\n";
			// ro.cst.tsearch.utils.FileUtils.appendToTextFile(path +
			// "address.txt", text);

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.NameOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt", text);
			// end test
		}
		
	}


	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
				
			TSServerInfoModule m;

			FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
			PinFilterResponse pinFilter = PINFilterFactory.getPinFilter(searchId, SearchAttributes.LD_PARCELNO_GENERIC_TR, false, false);
			DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			RejectNonRealEstate propertyTypeFilter = new RejectNonRealEstate(searchId);
			propertyTypeFilter.setThreshold(new BigDecimal("0.95"));
			
			if(hasPin()){			
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addFilter(propertyTypeFilter);
				m.addFilter(pinFilter);
				m.clearSaKeys();			
				m.setSaKey(0, SearchAttributes.LD_PARCELNO_GENERIC_TR);
				l.add(m);
			}
		
			if(hasStreet()){
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.clearSaKeys();
				m.setSaKey(0, SearchAttributes.P_STREET_FULL_NAME_EX );
				m.addFilter(propertyTypeFilter);
				m.addFilter(CityFilterFactory.getCityFilter(searchId, 0.6d));
				m.addFilter(addressFilter);
				m.addFilter(nameFilterHybrid);
				m.addValidator(defaultLegalValidator);
				l.add(m);
			}
			
			if( hasOwner() )
		    {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.clearSaKeys();
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);			
				m.addFilter(propertyTypeFilter);
				m.addFilter(addressFilter);
				m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims( SearchAttributes.OWNER_OBJECT, searchId, m));
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {"L;F;"});
				m.addIterator(nameIterator);			
				l.add(m);
		    }
		}
	
		serverInfo.setModulesForAutoSearch(l);
	
	}
}

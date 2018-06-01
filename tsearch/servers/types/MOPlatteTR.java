package ro.cst.tsearch.servers.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.html.parser.HtmlHelper;
import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;

public class MOPlatteTR extends TSServer {

	private static final long serialVersionUID = 1L;
	
	private static String SUBMIT_SUBDIVISION_JS_FUNCTION = " function submitSubdivision(){"
			+ "var f = document.realform;"
			+ "f.subdivision.value = f.subdivision.options[f.subdivision.selectedIndex].value;"
			+ "action=f.action	;"
			+ "action = action + \"&subdivision=\" +  f.subdivision.value;"
					+ "f.action = action;"
			+ "f.submit();"																												
			+" }";
	
	public MOPlatteTR(long searchId){
		super(searchId);
	}
	
	public MOPlatteTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		TSServerInfoModule module = null;
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module);
		FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);

		if(hasPin()) {
	    				    	
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));			
			module.getFunction(0).setSaKey(SearchAttributes.LD_MO_PLATTE_TWN);
			module.getFunction(1).setSaKey(SearchAttributes.LD_MO_PLATTE_AREA);
			module.getFunction(2).setSaKey(SearchAttributes.LD_MO_PLATTE_SECT);
			module.getFunction(3).setSaKey(SearchAttributes.LD_MO_PLATTE_QTRSECT);
			module.getFunction(4).setSaKey(SearchAttributes.LD_MO_PLATTE_BLOCK);
			module.getFunction(5).setSaKey(SearchAttributes.LD_MO_PLATTE_PARCEL);
			module.addFilter(addressFilter);
			module.addFilter(pinFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet() && hasStreetNo()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			
			module.getFunction(0).setSaKey(SearchAttributes.P_STREET_NO_NAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);			
		}
		
		// search by name
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			module.addFilter(pinFilter);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
															.getConfigurableNameIterator(module, searchId, new String[] {"L, F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	/**
	 * @param rsResponse
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException{
		
		String rsResponse = Response.getResult();
		
		if (rsResponse.indexOf("Number of Records Found: 0") >= 0) {
			Response.getParsedResponse().setError("No Records Found.");
			return;
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:			
			try {
				 
				rsResponse = Tidy.tidyParse(rsResponse, null);
				
				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();
																	
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
	            }
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			break;
			
		case ID_SAVE_TO_TSD :
		case ID_DETAILS :
			
        	String details = getDetails(rsResponse);
        	String pin = "";
        	
        	try {
        		HtmlParser3 parser= new HtmlParser3(details);
    			NodeList nodeList = parser.getNodeList();
    			
    			pin = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "PARCEL ID"), "", false).trim();
				
        	} catch (Exception e){
        		logger.error("Error on parsing html on MOPlatteTR: " + searchId);
        	}
        	
			if (viParseID == ID_SAVE_TO_TSD){
				
				smartParseDetails(Response, details);
				
                msSaveToTSDFileName = pin + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
                
            } else{
            	           	
            	String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				
				if (isInstrumentSaved(pin.trim(), null, data)){
					details += CreateFileAlreadyInTSD();
				} else{
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
            }
			
			break;
		
		case ID_SEARCH_BY_SUBDIVISION_NAME:
			HtmlParser3 parser = new HtmlParser3(rsResponse);

			NodeList hiddenParameters = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "INPUT", "type", "hidden", true);

			String buildHtml = "";
			try {
				String linkStart = CreatePartialLink(TSConnectionURL.idPOST);

				int seq = getSeq();
				SimpleNodeIterator elements = hiddenParameters.elements();

				Map<String, String> atsParameters = new HashMap<String, String>();
				while (elements.hasMoreNodes()) {
					InputTag nextNode = (InputTag) elements.nextNode();
					String name = nextNode.getAttribute("name");
					String value = nextNode.getAttribute("value");
					
					atsParameters.put(name, value);
				}
				String paramsLink = "/realresults.php?seq=" + seq;														
				String formAction = linkStart + paramsLink;
				
				atsParameters.put("ptwn", "");
				atsParameters.put("parea", "");
				atsParameters.put("psect", "");
				atsParameters.put("pqtrsect", "");
				atsParameters.put("pblock", "");
				atsParameters.put("pparcel", "");
				atsParameters.put("name", "");
				atsParameters.put("strtname", "");
				
				FormTag formTag = HtmlHelper.createFormTag(formAction, "realform", "POST");

				Tag tableTag = HtmlHelper.createTableTag();

				NodeList subdivisionSelectList = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "SELECT", "NAME", "subdivision", true);

				CompositeTag submitButton = HtmlHelper.createTagFromText("<input type=\"submit\" value=\"Search\">");
				submitButton.setAttribute("onClick", "submitSubdivision();");
				if (subdivisionSelectList != null && subdivisionSelectList.size() > 0) {

					Tag subdivisionTableRowTag = HtmlHelper.createTableRowTag();
					Tag selectTableColumnTag = HtmlHelper.createTableColumnTag();
					HtmlHelper.addTagToTag(selectTableColumnTag, HtmlHelper.createPlainText("Subdivision Name:"));

					HtmlHelper.addTagToTag(selectTableColumnTag, subdivisionSelectList);
					HtmlHelper.addTagToTag(subdivisionTableRowTag, selectTableColumnTag);
					HtmlHelper.addTagToTag(tableTag, subdivisionTableRowTag);
				}

				// add the submit button to the site form
				Tag buttonRowTag = HtmlHelper.createTableRowTag();

				Tag buttonTableColumnTag = HtmlHelper.createTableColumnTag();

				HtmlHelper.addTagToTag(buttonTableColumnTag, submitButton);
				HtmlHelper.addTagToTag(buttonRowTag, buttonTableColumnTag);
				HtmlHelper.addTagToTag(tableTag, buttonRowTag);

				formTag.setChildren(tableTag.getChildren());
				formTag.getChildren().add(hiddenParameters);
				Set<Entry<String, String>> entrySet = atsParameters.entrySet();

				for (Entry<String, String> entry : entrySet) {
					String inputFormat = MessageFormat.format("<input type=\"hidden\" name=\"{0}\" value=\"{1}\">", entry.getKey(),
							entry.getValue());
					
					CompositeTag createTagFromText = HtmlHelper.createTagFromText(inputFormat);
					HtmlHelper.addTagToTag(formTag, createTagFromText);
				}

				BodyTag bodyTag = HtmlHelper.createBodyTag();
				/*
				 * In order to send the subdivisionLetterPArameter the JS
				 * function responsible for submit had to be modified in order
				 * to make the submit. It would have helped if Title-search
				 * would have parsed the parameters sent in the body (post
				 * parameters).
				 */

				Tag jsTag = HtmlHelper.createJSTag(SUBMIT_SUBDIVISION_JS_FUNCTION);
				HtmlHelper.addTagToTag(bodyTag, jsTag);
				HtmlHelper.addTagToTag(bodyTag, formTag);

				buildHtml = bodyTag.toHtml();
				this.mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, atsParameters);

			} catch (Exception e) {
				e.printStackTrace();
			}

			this.parser.Parse(Response.getParsedResponse(), buildHtml, Parser.NO_PARSE);
			break;
		case ID_GET_LINK:
			if (sAction.indexOf("realresults") >= 0) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if (sAction.indexOf("realview") >= 0){
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		default:
			break;
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			 			
			Form form = new SimpleHtmlParser(table).getForm("realview");
			String PHPSESSID = "";
			String action = "";
			if (form != null){
				PHPSESSID = form.getParams().get("PHPSESSID");
				action = form.action;
			}
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(table, null);

			TableTag mainTable = null;
			
			NodeList divList =  parser.parse(new TagNameFilter("div"));
			if (divList != null && divList.size() > 0){
				Div mainDiv = (Div) divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "mainresult")).elementAt(0);
				
				if (mainDiv != null){
					NodeList mainTableList = mainDiv.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (mainTableList != null && mainTableList.size() > 0){
						mainTable = (TableTag) mainTableList.elementAt(0);
					}
				}
			}
			
			String header = "";
			
			if (mainTable != null){
				TableRow[] rows = mainTable.getRows();
				for (int i = 0; i < rows.length; i++){
					TableRow row = rows[i];
					
					if (row.getColumnCount() > 4){
						
						if (i == 0){
							header = row.toHtml();
							header = header.replaceAll("(?is)</?a[^>]*>", "").replaceAll("(?is)<img[^>]*>", "");
							header = header.replaceAll("(?is)(<tr[^>]*>)\\s*<td[^>]*>\\s*</td>", "$1");
							continue;
						}
						
						TableColumn[] cols = row.getColumns();
						
						String link = CreatePartialLink(TSConnectionURL.idPOST);
						
						if (StringUtils.isNotEmpty(PHPSESSID) && StringUtils.isNotEmpty(action)){
							link += "/" + action + "PHPSESSID=" + PHPSESSID;
						}
						if (cols.length > 5){
							InputTag checkbox = (InputTag) cols[0].getChild(0);
							if (checkbox != null){
								String name = checkbox.getAttribute("name");
								String value = checkbox.getAttribute("value");
								if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)){
									link += "&" + name + "=" + value;
								}
							}
							NodeList checkboxList = cols[4].getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
							if (checkboxList != null && checkboxList.size() > 0){
								for (int n = 0; n < checkboxList.size(); n++){
									checkbox = (InputTag) checkboxList.elementAt(n);
									if (checkbox != null){
										String name = checkbox.getAttribute("name");
										String value = checkbox.getAttribute("value");
										if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)){
											link += "&" + name + "=" + value;
										}
									}
								}
							}
							
							if (getSearch().getSearchType() == Search.AUTOMATIC_SEARCH){
								link = link.replaceAll("(?is)&amp;", "&");
							}
							
							link += "&dummy=true";
							
							String rowHtml = row.toHtml();
								
							rowHtml = rowHtml.replaceAll("(?i)</?font[^>]*>", "");
							rowHtml = rowHtml.replaceAll("(?is)(<tr[^>]*>)\\s*<td[^>]*>\\s*<input[^>]*>\\s*</td>\\s*", "$1");
							rowHtml = rowHtml.replaceAll("(?is)(<tr[^>]*>\\s*<td[^>]*>)([^<]+)(</td>)\\s*", "$1 <a href=\"" + link + "\">$2</a>$3");
								
							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);
							currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
								
							ResultMap m = parseIntermediaryRow(row, searchId);
							Bridge bridge = new Bridge(currentResponse, m ,searchId);
								
							DocumentI document = (TaxDocumentI) bridge.importData();				
							currentResponse.setDocument(document);
							
							intermediaryResponse.add(currentResponse);
						}
					}
				}
			}
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
				response.getParsedResponse().setHeader("<table border=\"1\" id=\"mainresults\">" + header);
				response.getParsedResponse().setFooter("</table>");
		    }
			
			outputTable.append(table);
				
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	private String getDetails(String response) {
		
		/* If from memory - use it as is */
		if(!response.contains("<html")){
			return response;
		}
		
		String result = "";
		response = Tidy.tidyParse(response, null);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			
			NodeList divList =  htmlParser.parse(new TagNameFilter("div"));
			if (divList != null && divList.size() > 0){
				Div mainDiv = (Div) divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "mainresult")).elementAt(0);
				
				if (mainDiv != null){
					result = mainDiv.toHtml();
						
					result = result.replaceAll("(?is)</?a[^>]*>", "");
					result = result.replaceAll("(?is)<img[^>]*>", "");
					result = result.replaceAll("(?is)<script.*?</script>", "");
					result = result.replaceAll("(?is)<B>Disclaimer:.*?</div>", "</div>");
					result = result.replaceAll("(?is)<table ", "<table border=\"1\" ");
					
					result = cleanDetails(result);
				}
			}
			
			Pattern pdfLink = Pattern.compile("<a target='_blank' href=.*?Link=([^']+)'[^>]*>");
			Matcher pdfLinkMatcher = pdfLink.matcher(result);
			int numberOfDelinquentYears = 0;
			StringBuilder delinquent = new StringBuilder();
			
			Map<String, String> totalAssessedValuation = new LinkedHashMap<>();
			while (pdfLinkMatcher.find()) {
				String pdf = getPDF(pdfLinkMatcher.group(1));
				if (!pdf.isEmpty()) {
					
					Pattern delinquentTable = Pattern
							.compile("(?is)After december 31.*?penalty and interest as follows.*?(\\d{4}\\s+TAX\\s+PAID\\s+IN\\s+\\d{4}).*");
					Matcher delinquentTableMatcher = delinquentTable.matcher(pdf);

					if (delinquentTableMatcher.find()) {
						numberOfDelinquentYears++;
						if (numberOfDelinquentYears == 1) {
							delinquent.append("<table border=\"1\" cellspacing=\"1\" cellpadding=\"1\" width=\"30%\""
									+ " style=\"font-weight:bolder;font-size:8pt;\"><tr><th colspan=\"2\">After "
									+ "December 31st,<br> Pay With Penalty and Interest as Follows:</th></tr>");
						}
						String delinquentContent = delinquentTableMatcher.group(0).replaceAll("Pay\\s+this\\s+total\\s+prior\\s+to.*?\\d{4}", "");
						Pattern delinquentTaxes = Pattern.compile("(?is)(January|February|March|April|May|June|July|August|Sept-Dec)\\s+([\\d,.]+)");
						Matcher delinquentTaxesMatcher = delinquentTaxes.matcher(delinquentContent);
						delinquent.append("<tr><td id=\"delinquent" + numberOfDelinquentYears + "\" align=\"center\" colspan=\"2\">"
								+ delinquentTableMatcher.group(1)
								+ "</td></tr>");
						while (delinquentTaxesMatcher.find()) {
							delinquent.append("<tr><td>" + delinquentTaxesMatcher.group(1) + "</td><td>" + delinquentTaxesMatcher.group(2) + "</td></tr>");
						}
					}
					
					Pattern totalAssessedValuationPattern = Pattern.compile("([\\d,]+)TOTAL ASSESSED VALUATION");
					Matcher matcher = totalAssessedValuationPattern.matcher(pdf);
					
					if(matcher.find()) {
						totalAssessedValuation.put(RegExUtils.getFirstMatch("year=(\\d+)", pdfLinkMatcher.group(1), 1), matcher.group(1));
					}
				}
				else {// if statement PDF is empty, remove the statement link
					result = result.replaceFirst(Pattern.quote(pdfLinkMatcher.group(0)) + ".*?</a>", "Rec/Statement is Empty");
				}
			}
			
			if(!totalAssessedValuation.isEmpty()) {
				StringBuilder totalAssessedValuationBuilder = new StringBuilder();
				totalAssessedValuationBuilder.append("<table border=\"1\" cellspacing=\"1\" cellpadding=\"1\" width=\"30%\""
									+ " style=\"font-weight:bolder;font-size:8pt;\"><tr><th colspan=\"2\">TOTAL ASSESSED VALUATION per year</th></tr>");
				
				for (String year : totalAssessedValuation.keySet()) {
					totalAssessedValuationBuilder.append("<tr><td>" + year + "</td><td><span id=\"tav" + year  + "\">" + totalAssessedValuation.get(year) + "</span></td></tr>");
				}
				totalAssessedValuationBuilder.append("</table>");
				
				result += totalAssessedValuationBuilder.toString();
			}
			
			if (numberOfDelinquentYears > 0) {
				delinquent.append("</table>");
				result += delinquent.toString();
			}
		} catch (Exception e){
			e.printStackTrace();
		}

		return result;
		
	}
	
	private String getPDF(String pdfUrl) throws Exception {
		String rsp = "";
		HTTPRequest reqP = new HTTPRequest(getDataSite().getServerHomeLink() + pdfUrl.substring(1, pdfUrl.length()));
		HTTPResponse resP = null;
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			resP = site.process(reqP);
			if (resP.getContentLenght() > 0) {
				rsp = PDFUtils.extractTextFromPDF(resP.getResponseAsStream(), true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		return rsp;
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		
		if (row != null){			
			for(int i = 0; i < cols.length; i++){
				String contents = "";
				org.htmlparser.Node colText = null;
				try {
					colText = HtmlParser3.getFirstTag(cols[i].getChildren(), TextNode.class, true);
				} catch (Exception e) {
					break;
				}
				
				if (colText != null){
					contents = colText.getText();
					
					if (StringUtils.isNotEmpty(contents)){
						contents = contents.replaceAll("(?is)&amp;", "&").replaceAll("(?is)&nbsp;", " ").trim();
						
						switch(i){								
							case 1:
								resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),contents);
								break;
		
							case 2:
								resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), contents.trim());								
								break;
							
							case 3:
								if (contents.startsWith("%")){
									String nameOnServer = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
									contents = contents.replaceAll("(?is)(%.*?)\\b(CAMDEN POINT|DEARBORN|EAST LEAVENWORTH|EDGERTON|FARLEY|FERRELVIEW|HOUSTON LAKE|IATAN|KANSAS CITY).*", "$1");
									contents = contents.replaceAll("(?is)(%.*?)\\b(LAKE WAUKOMIS|NORTHMOOR|PARKVILLE|PLATTE CITY|PLATTE WOODS|RIDGELY|RIVERSIDE|TRACY|WALDRON).*", "$1");
									contents = contents.replaceAll("(?is)(%.*?)\\b(WEATHERBY LAKE|WESTON).*", "$1");
									nameOnServer += " " + contents;
									
									resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer.trim());
								}
								break;
								
							case 4:
								if (contents.contains("-")){
									String city = contents.replaceAll("(?is)[^-]+\\s*-\\s*(\\w+)\\s*$", "$1");
									resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
								}
								
								contents = contents.replaceAll("(?is)\\s*-\\s*\\w+\\s*$", "");
								resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(contents));
								resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(contents));
								break;
								
							case 5:
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), StringFormats.StreetNo(contents));
								break;
						}
					}
				}
			}
			parseNames(resultMap);
		}
		
		return resultMap;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
			
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		try {
			HtmlParser3 parser= new HtmlParser3(detailsHtml);
			NodeList nodeList = parser.getNodeList();
    			
			String pin = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "PARCEL ID"), "", false).trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin);
			
			String name = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "NAME"), "", false).trim();
			if (StringUtils.isNotEmpty(name)){
				
				String nameFromMailingAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "MAILING ADDRESS"), "", false).trim();
				if (nameFromMailingAddress.startsWith("%")){
					
					nameFromMailingAddress = nameFromMailingAddress.replaceAll("(?is)(%.*?)\\b(CAMDEN POINT|DEARBORN|EAST LEAVENWORTH|EDGERTON|FARLEY|FERRELVIEW|HOUSTON LAKE|IATAN|KANSAS CITY).*", "$1");
					nameFromMailingAddress = nameFromMailingAddress.replaceAll("(?is)(%.*?)\\b(LAKE WAUKOMIS|NORTHMOOR|PARKVILLE|PLATTE CITY|PLATTE WOODS|RIDGELY|RIVERSIDE|TRACY|WALDRON).*", "$1");
					nameFromMailingAddress = nameFromMailingAddress.replaceAll("(?is)(%.*?)\\b(WEATHERBY LAKE|WESTON).*", "$1");
					name += " " + nameFromMailingAddress;
				}
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			}
			
			String address = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "PROPERTY ADDRESS"), "", false).trim();
			if (StringUtils.isNotEmpty(address)){
				if (address.contains("-")){
					String city = address.replaceAll("(?is)[^-]+\\s*-\\s*(\\w+)\\s*$", "$1");
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				}
				
				address = address.replaceAll("(?is)\\s*-\\s*\\w+\\s*$", "");
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			}
			
			String subdivision = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "SUBDIVISION"), "", false).trim();
			if (StringUtils.isNotEmpty(subdivision)){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
			}
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			TableTag taxTable = null;
			
			if (tableList != null && tableList.size() > 0){
				for (int i = 1; i < tableList.size(); i++){
					String table = tableList.elementAt(i).toHtml();
					if (table.contains("TAX YEAR")){
						taxTable = (TableTag) tableList.elementAt(i);
						break;
					}
				}
				
				String delinqAmount = parseDelinquentAmount(resultMap, nodeList);
				
				if (taxTable != null){
					List<List<String>> taxes = HtmlParser3.getTableAsList(taxTable, false);
					if (taxes.size() > 0){
						String taxYear = taxes.get(taxes.size() - 1).get(0).trim();
						if (StringUtils.isNotEmpty(taxYear)){
							resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
						}
						for (List<String> list : taxes){
							String year = list.get(0).trim();
							if (taxYear.equals(year)){
								String amount = list.get(2);
								amount = amount.replaceAll("[\\$,]+", "").trim();
								resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), amount);
								String paidDate = list.get(4);
								if (StringUtils.isNotEmpty(paidDate)){
									resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amount);
									try {
										Util.dateParser3(paidDate);
										resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), paidDate);
									} catch(Exception e) {
										logger.error("Cannot parse date paid from " + paidDate + " on searchid " + searchId, e);
									}
								} else{
									resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amount);
								}
								
								
							} else{
								String amount = list.get(2);
								amount = amount.replaceAll("[\\$,]+", "").trim();
								
								if (StringUtils.isEmpty(list.get(4)) && list.get(6).contains("Empty")) {
									// 9226-if PDF in link is empty, add to delq amount value from TAX AMOUNT column
									delinqAmount += "+" + amount;
								}
							}
							
							
						}
						delinqAmount = GenericFunctions.sum(delinqAmount, searchId);
						resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), delinqAmount);
					}
				}
			}
			
			String taxYearString = (String) resultMap.get(TaxHistorySetKey.YEAR.getKeyName());
			if(StringUtils.isNotBlank(taxYearString)) {
				String totalAssessedValuation = HtmlParser3.getNodeValueByID("tav" + taxYearString, nodeList, true);
				if(totalAssessedValuation != null) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssessedValuation.replaceAll("[^\\d.]+", ""));
				}
			}
			
			parseNames(resultMap);
		} catch (Exception e){
			logger.error("ERROR while parsing details on MOPlatteTR: " + searchId + "\n" + e.toString());
		}
		
		return null;
	}

	private String parseDelinquentAmount(ResultMap resultMap, NodeList nodeList) {
		double delinqAmount = 0.0;
		Node delinquent = HtmlParser3.getNodeByID("delinquent1", nodeList, true);
		if (delinquent != null) {
			TableTag delinquentTable = (TableTag) delinquent.getParent().getParent();
			TableRow[] rows = delinquentTable.getRows();
			int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			String monthString = "";
			switch (currentMonth) {
			case 0:
				monthString = "January";
				break;
			case 1:
				monthString = "February";
				break;
			case 2:
				monthString = "March";
				break;
			case 3:
				monthString = "April";
				break;
			case 4:
				monthString = "May";
				break;
			case 5:
				monthString = "June";
				break;
			case 6:
				monthString = "July";
				break;
			case 7:
				monthString = "August";
				break;
			case 8:
			case 9:
			case 10:
			case 11:
				monthString = "Sept-Dec";
				break;
			default:
				monthString = "Invalid month";
				break;
			}
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumns()[0].toPlainTextString().contains(currentYear + " TAX PAID IN " + (currentYear + 1))) {
					// current year taxes are not added to delinquent amount
					break;
				}
				if (row.getColumns()[0].toPlainTextString().contains(monthString)) {
					delinqAmount += Double.parseDouble(row.getColumns()[1].toPlainTextString().trim().replaceAll("[$,\\s]", ""));
				}
			}
		}
		return String.valueOf(delinqAmount);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap){
		
		String nameOnServer = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, types, otherTypes;
		String[] ownersArray = nameOnServer.split("\\s*%\\s*");
		
		names = StringFormats.parseNameNashville(ownersArray[0].trim(), true);
		
		types = GenericFunctions.extractAllNamesType(names);
		otherTypes = GenericFunctions.extractAllNamesOtherType(names);
		suffixes = GenericFunctions.extractNameSuffixes(names);
		
		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		
		if (ownersArray.length == 2){
			String owners = ownersArray[1].trim();
			owners = owners.replaceAll("(?is)([A-Z]+\\s+[A-Z])\\s*&\\s*([A-Z]+\\s+[A-Z]\\s+[A-Z]+)", "$2 & $1");
			names = StringFormats.parseNameDesotoRO(owners.trim(), true);
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@SuppressWarnings("deprecation")
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

		String link = image.getLink();
		String fullParcel = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(link, "fullparcel");
		String year = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(link, "year");
		
		if (StringUtils.isNotEmpty(fullParcel) && StringUtils.isNotEmpty(year)){
			byte[] contentAsBytes = null;
			
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				contentAsBytes = ((ro.cst.tsearch.connection.http2.MOPlatteTR)site).getPDFDocument(fullParcel, year); 
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				HttpManager.releaseSite(site);
			}
			
			if (contentAsBytes == null){
				return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
			}
			
			afterDownloadImage();
			ServerResponse resp = new ServerResponse();
	
			String imageName = image.getPath();
			if (FileUtils.existPath(imageName)) {
				contentAsBytes = FileUtils.readBinaryFile(imageName);
				return new DownloadImageResult(DownloadImageResult.Status.OK, contentAsBytes, image.getContentType());
			}
	
			resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, contentAsBytes, ((ImageLinkInPage) image)
					.getContentType()));
	
			DownloadImageResult dres = resp.getImageResult();
			
		// System.out.println("image");

		return dres;
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}
	}
	
	@SuppressWarnings("deprecation")
	protected String getPDF(ServerResponse response, ResultMap map, String accountNumber, String year, boolean onlyPDF) {
		// get the PDF
		ImageLinkInPage imageLink = new ImageLinkInPage(false);
		imageLink.setContentType("application/pdf");
		String pdfUrl = ro.cst.tsearch.connection.http2.MOPlatteTR.PDF_URL.replace("$$accountId$$", accountNumber).replace("$$year$$", year);
		imageLink.setLink(pdfUrl);
		imageLink.setPath(accountNumber + ".pdf");

		// response.getParsedResponse().addImageLink(new ImageLinkInPage(pdfUrl,
		// accountNumber + ".pdf"));

		ByteArrayOutputStream bas = new ByteArrayOutputStream();
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		byte[] pdfPage = null;
		try {
			if (StringUtils.isNotEmpty(accountNumber)) {
				pdfPage = ((ro.cst.tsearch.connection.http2.MOPlatteTR) site).getPDFDocument(accountNumber, year);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		if (pdfPage.length > 0 && pdfPage.length != 93) { //93 is the length of the response when there is no PDF
			
			if (!onlyPDF) {
				// get the link in order to make the fileName
				response.getParsedResponse().addImageLink(imageLink);
			}
			
			String fileName = accountNumber.replaceAll("-|\\.", "");
			String filePath = getImagePath() + ".pdf"; // getSearch().getSearchDir()
														// + File.separator +
														// fileName + ".pdf";

			try {
				if(ro.cst.tsearch.utils.FileUtils.createDirectory(getImageDirectory())) {
					new File(filePath).createNewFile();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			byte[] contentAsBytes = pdfPage;

			InputStream stream = new ByteArrayInputStream(contentAsBytes);
			ro.cst.tsearch.utils.FileUtils.writeStreamToFile(stream, filePath);
			imageLink.setPath(filePath);
			imageLink.setDownloadStatus("DOWNLOAD_OK");
			imageLink.setSolved(true);
			imageLink.setImageFileName(fileName + ".pdf");

			Writer output = null;
			PDDocument document = null;
			try {
				document = PDDocument.load(filePath);
				output = new OutputStreamWriter(bas);

				PDFTextStripper stripper = new PDFTextStripper();
				stripper.getTextMatrix();
				stripper.setWordSeparator("     ");

				stripper.setLineSeparator("\n");
				stripper.setPageSeparator("\n");

				// do not change this. will blow up the output of the pdf and
				// the
				// parsing of him will fail
				stripper.setSortByPosition(true);
				// PDFText2HTML stripper = new PDFText2HTML();

				stripper.writeText(document, output);
				
				if (!onlyPDF) {
					map.put("tmpPdf", bas.toString());
				}
				

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (document != null) {
					try {
						document.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return bas.toString();
	}
	
	/** 
	 * this method creates the tables requested in Task 7737 and Task 7678
	 */
	public void parseTaxes(ResultMap resultMap, String accountNumber, int currentYear, String pdfText) {
		String delinquentYears = (String) resultMap.get("tmpDelinquentYears");
		boolean delinquentYearsPresent = false;
		
		StringBuilder tablePreviousDelinquent = new StringBuilder();
		tablePreviousDelinquent.append("Delinquent Years:</br>")
			.append("<table id='tableDelinquentYears' border=\"1\"><tr><th>Tax Year</th><th>Base Amount</th><th>Amount Due</th><th>Notes</th></tr>");
		
		StringBuilder tableTaxes = new StringBuilder();
		tableTaxes.append("Tax Information:</br>")
			.append("<table id='tableTaxInfo' border=\"1\"><tr><th>Tax Year</th><th>Base Amount</th><th>Amount Paid</th>" +
					"<th>Paid On</th><th>Receipt#</th><th>Amount Due</th><th>Prior Delinquent</th></tr>");
		
		String delinquentAmount = "0.0";
		LinkedList<String[]> history = new LinkedList<String[]>();
		
		for(int year = currentYear;; year--) {
			String yearText = "";
			
			if(year == currentYear) {
				yearText = pdfText;
			} else {
				try {
					yearText = getPDF(null, null, accountNumber, year + "", true);
				} catch (Exception e) {
					//the pdf could not be obtained
					break;
				}
			}
			
			if (!StringUtils.isEmpty(yearText)) {
				ResultMap tmpMap = new ResultMap();
				ro.cst.tsearch.servers.functions.MOPlatteTR atsParser = ro.cst.tsearch.servers.functions.MOPlatteTR.getInstance();
				atsParser.setTaxData(yearText, tmpMap);
				
				String[] record = new String[6];
				record[0] = String.valueOf(year);
				record[1] = (String) tmpMap.get("TaxHistorySet.BaseAmount");
				record[2] = (String) tmpMap.get("TaxHistorySet.AmountPaid");
				record[3] = (String) tmpMap.get("TaxHistorySet.DatePaid");
				record[4] = (String) tmpMap.get("TaxHistorySet.ReceiptNumber");
				record[5] = (String) tmpMap.get("TaxHistorySet.TotalDue");
				history.addFirst(record);
			} else {
				break;
			}
		}
		
		for(String[] record : history) {
			String year = record[0] != null ? record[0] : "-";
			String baseAmount = record[1] != null ? record[1] : "-";
			String amountPaid = record[2] != null ? record[2] : "-";
			String datePaid = record[3] != null ? record[3] : "-";
			String receiptNo = record[4] != null ? record[4] : "-";
			String amountDue = record[5] != null ? record[5] : "-";
			
			tableTaxes.append("<tr><td>" + year + "</td>");
			tableTaxes.append("<td>" + baseAmount + "</td>");
			tableTaxes.append("<td>" + amountPaid + "</td>");
			tableTaxes.append("<td>" + datePaid + "</td>");
			tableTaxes.append("<td>" + receiptNo + "</td>");
			tableTaxes.append("<td>" + amountDue + "</td>");
			tableTaxes.append("<td>" + GenericFunctions.sum(delinquentAmount, searchId) + "</td></tr>");
			
			if (delinquentYears.contains(year)) {
				delinquentAmount += "+" + amountDue;
				delinquentYearsPresent = true;
				tablePreviousDelinquent.append("<tr><td>"+year+"</td>");
				tablePreviousDelinquent.append("<td>"+baseAmount.replaceAll(",", "")+"</td>");
				tablePreviousDelinquent.append("<td>"+amountDue.replaceAll(",", "")+"</td><td>DLQ</td></tr>");
			}
		}
		
		tableTaxes.append("</table>");
		resultMap.put("tmpTableTaxes", tableTaxes.toString());
		
		if (delinquentYearsPresent) {
			tablePreviousDelinquent.append("</table>");
			resultMap.put("tmpTablePreviousDelinquent", tablePreviousDelinquent.toString());
		}
	}
	
	
	protected String cleanDetails(String response) {
		response = response.replaceAll("(?is)<a.*?</a>", "");
		
		// replace the buttons for printing the tax receipts with links for viewing them
		Matcher mPid = Pattern.compile("(?is)\\bPARCEL ID.*?>([\\d.-]+)<").matcher(response);
		if(!mPid.find()) {
			return response;
		}

		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		Matcher m = Pattern.compile("(?is)<input.*?onclick=\\\"getstatereceipt\\('([^']+)',\\s*'([^']+)',\\s*'([^']+)'\\)\\\"\\s*/?>").matcher(response);
		
		while(m.find()) {
			response = response.replace(m.group(), "<a target='_blank' href='" 
					+ linkStart + "//onlinerec/platter_rec_state.php?fullparcel=" + m.group(1) + "&year=" + m.group(2)  + "&dateenc=" + m.group(3) + "'>" 
					+ "View Rec/Statement</a>");			
		}
		
		return response;
	}
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent,
			boolean forceOverritten) {
		ADD_DOCUMENT_RESULT_TYPES result =  super.addDocumentInATS(response, htmlContent, forceOverritten);
		try {
			if(result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				UtilForGwtServer.uploadDocumentToSSF(searchId, response.getParsedResponse().getDocument());
			}
		} catch (Exception e) {
			logger.error("Error while saving index for " + searchId, e);
		}
		return result;
	}
}

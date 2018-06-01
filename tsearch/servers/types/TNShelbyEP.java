package ro.cst.tsearch.servers.types;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class TNShelbyEP extends TSServer {

	protected static final Category logger = Logger.getLogger(TNShelbyEP.class);
	
	static final long serialVersionUID = 10000000;
	
	private static final Pattern EXTRACT_RECEIPT_PATTERN = Pattern.compile("ShowHistory\\('([^']+)',\\s*'([^']+)',\\s*'([^']+)'\\);");
	 

	public TNShelbyEP(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule m;
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if(!StringUtils.isEmpty(city)){
			if(!city.startsWith("MEMPHIS")){
				return;
			}			
		}
		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_PARCEL_ID_FAKE);
			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
			l.add(m);
		}

		if (hasStreet()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_NOT_EMPTY);
			l.add(m);

			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_EMPTY);
			l.add(m);
		}

		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(
					SearchAttributes.OWNER_OBJECT, searchId, m));

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L;M;", "L;m;"});
			
			m.addIterator(nameIterator);
			
			l.add(m);
		}
		

		serverInfo.setModulesForAutoSearch(l);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		
		switch (viParseID) {
		
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
			ParseResponse(sAction, Response, rsResponse.contains("ctl00_MainBodyPlaceHolder_gridSearchResults")
					? ID_INTERMEDIARY
					: ID_DETAILS);
			break;
			
		case ID_INTERMEDIARY:
			// no result
			if (rsResponse.indexOf("No records were found") > -1) {
				Response.getParsedResponse().setError("No results found");
				return;
			}
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			break;
			
			
		case ID_SEARCH_BY_TAX_BIL_NO:
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_GET_LINK:
		case ID_SAVE_TO_TSD:
			if (!rsResponse.contains("ctl00_MainBodyPlaceHolder_lblParcelNo")) {
				Response.getParsedResponse().setError("No results found");
				return;
			}
			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponse, accountId);						
			String filename = accountId + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				if (FileAlreadyExist(filename) ) {
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse( details );
				
			} else {
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse( details );
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			
			break;
		default:			
			break;
		}
	}
	
	private String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList accountIdList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainBodyPlaceHolder_lblParcelNo"),true);
			if(accountIdList.size() != 1) {
				return null;
			}
			accountId.append(accountIdList.elementAt(0).toPlainTextString().replaceAll("\\s+", ""));
			
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				return rsResponse;
			}
			
			
			
			NodeList infoTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"),true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width","80%"))
				.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if(infoTableList.size() != 1) {
				return null;
			}
			details.append(infoTableList.elementAt(0).toHtml());
			
			NodeList receiptList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_MainBodyPlaceHolder_gridDetail"),true);
			
			if(receiptList.size() == 1) {
				
				TreeMap<String, String> linksToFollow = new TreeMap<String, String>();
				TableTag receiptTable = (TableTag) receiptList.elementAt(0);
				
				NodeList linkList = receiptList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
				String formatTemplate = "https://epayments.memphistn.gov/property/History.aspx?ParcelNo={0}&Year={1}&BillNo={2}";
				for (int i = 0; i < linkList.size(); i++) {
					LinkTag receiptLink = (LinkTag) linkList.elementAt(i);
					String oldLink = receiptLink.extractLink();
					
					Matcher matcher = EXTRACT_RECEIPT_PATTERN.matcher(oldLink);
					if(matcher.find()) {
						String year = matcher.group(2).replaceAll(" ", ""); 
						String link = MessageFormat.format(formatTemplate, 
								matcher.group(1), 
								year, 
								matcher.group(3));
						linksToFollow.put(year, link);
						receiptLink.setLink("#year" + year);	
					}
				}
				details.append(receiptTable.toHtml());
				
				for (String year : linksToFollow.keySet()) {
					String receiptHtml = getReceiptHtml(linksToFollow.get(year));
					if(receiptHtml != null) {
						details.append("<A name=\"year" + year + "\">Receipt for year " + year + "</A>");
						details.append(receiptHtml);
					}
				}
				
			}
			return details.toString();
			
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	private String getReceiptHtml(String link) {
		String receiptHtml = getLinkContents(link);
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(receiptHtml, null);
			NodeList formList = htmlParser.parse(null).extractAllNodesThatMatch(new TagNameFilter("form"),true);
			if(formList.size() == 1) {
				formList.keepAllNodesThatMatch(new NotFilter(new TagNameFilter("input")), true);
				formList.keepAllNodesThatMatch(new NotFilter(new TagNameFilter("a")), true);
				receiptHtml = formList.elementAt(0).getChildren().toHtml();
			}
			
			
		} catch (Exception e) {
			logger.error("Error while getting receipt " + link, e);
		}
		return receiptHtml;
	}
	
	

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_MainBodyPlaceHolder_gridSearchResults"), true);
			
			if(mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			String footer = "";
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 3) {
	
					LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					
					String link = CreatePartialLink(TSConnectionURL.idGET) + "/property/" + 
						linkTag.extractLink().trim().replaceAll("\\s", "%20");
					
					linkTag.setLink(link);
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.TNShelbyEP.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} 
			}
			footer = processLinks(response,nodeList);
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + rows[0].toHtml());
			response.getParsedResponse().setFooter(footer + "</table>");

			
			outputTable.append(table);
			
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data for TNShelbyYA", t);
		}
		
		return intermediaryResponse;
	}

	private String processLinks(ServerResponse response, NodeList nodeList) {
		NodeList selectList = nodeList.extractAllNodesThatMatch(new TagNameFilter("select"), true);
		String result = "";
		if(selectList.size() > 0) {
			SelectTag select  = (SelectTag) selectList.elementAt(0);
			OptionTag[] options = select.getOptionTags();
			
			Map<String, String> hiddenParams = HttpSite.fillAndValidateConnectionParams(
					response.getResult(), 
					ro.cst.tsearch.connection.http2.TNShelbyEP.REQ_PARAM_NAMES, 
					ro.cst.tsearch.connection.http2.TNShelbyEP.FORM_NAME);
			
			getSearch().setAdditionalInfo(
					ro.cst.tsearch.connection.http2.TNShelbyEP.PARAMETERS_NAVIGATION_LINK, 
					hiddenParams);
			result = "<tr><td colspan=\"3\" align=\"center\" >";
			for (int i = 0; i < options.length; i++) {
				OptionTag option = options[i];
				if(option.getAttribute("selected") != null) {
					if(i > 0) {
						String formatTemplate = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.NEXT_LINK_MODULE_IDX) + 
							"&__EVENTTARGET=ctl00$MainBodyPlaceHolder$dropPageNav&ctl00$MainBodyPlaceHolder$dropPageNav={0}";
					
						result += "<A href=\"" + MessageFormat.format(formatTemplate, String.valueOf(Integer.parseInt(option.getValue()) - 1)) + "\">Previous</a>";
					} 
					if(i < options.length - 1) {
						String formatTemplate = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.NEXT_LINK_MODULE_IDX) + 
							"&__EVENTTARGET=ctl00$MainBodyPlaceHolder$dropPageNav&ctl00$MainBodyPlaceHolder$dropPageNav={0}";
						if(i > 0) {
							result += "&nbsp;&nbsp;";
						}
						result += "<a href=\"" + MessageFormat.format(formatTemplate, String.valueOf(Integer.parseInt(option.getValue()) + 1)) + "\">Next</a>";
					}
				}
			}
			result += "</td></tr>";
		}
		return result;
		
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap resultMap) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList someNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainBodyPlaceHolder_lblParcelNo"),true);
			if(someNodeList.size() != 1) {
				return null;
			}
			String accountId = someNodeList.elementAt(0).toPlainTextString().trim();//replaceAll("\\s+", "");
			
			resultMap.put("PropertyIdentificationSet.ParcelID", accountId);
			resultMap.put("PropertyIdentificationSet.City","Memphis");
			resultMap.put("OtherInformationSet.SrcType","YA");
			
			someNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainBodyPlaceHolder_lblOwnerName"),true);
			if(someNodeList.size() == 1) {
				resultMap.put("tmpOwnerFullName", someNodeList.elementAt(0).toPlainTextString().trim());
				resultMap.put("PropertyIdentificationSet.OwnerLastName", resultMap.get("tmpOwnerFullName"));
			}
			
			someNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainBodyPlaceHolder_lblOwnerAddress"),true);
			if(someNodeList.size() == 1) {
				String[] address = StringFormats.parseAddressShelbyAO(someNodeList.elementAt(0).toPlainTextString().trim());
				resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
				resultMap.put("PropertyIdentificationSet.StreetName", address[1]);
			}
			
			NodeList receiptList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_MainBodyPlaceHolder_gridDetail"),true);
			
			if(receiptList.size() == 1) {
				TableTag receiptTable = (TableTag) receiptList.elementAt(0);
				TableRow[] rows = receiptTable.getRows();
				for (int i = 1; i < rows.length; i++) {
					TableColumn[] columns = rows[i].getColumns();
					if(columns.length == 9) {
						if(i == 1) {
							resultMap.put("TaxHistorySet.Year", columns[0].toPlainTextString().trim());
							resultMap.put("TaxHistorySet.BaseAmount", columns[5].toPlainTextString().trim().replaceAll("[$,]", ""));
							resultMap.put("tmpTotalDue", columns[8].toPlainTextString().trim().replaceAll("[$,]", ""));
							resultMap.put("tmpPenalty", columns[6].toPlainTextString().trim().replaceAll("[$,]", ""));
							resultMap.put("tmpOtherCharges", columns[7].toPlainTextString().trim().replaceAll("[$,]", ""));
						} else {
							resultMap.put("tmpTotalDue", resultMap.get("tmpTotalDue") + "+" + columns[8].toPlainTextString().trim().replaceAll("[$,]", ""));
						}
					}
					
				}
				
				someNodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "gridDetail"))
					.extractAllNodesThatMatch(new HasAttributeFilter("rules","all"));
				
				if(someNodeList.size() > 0) {
				
				
					ResultTable receipts = new ResultTable();
					Map<String, String[]> map = new HashMap<String, String[]>();
					String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
					List<List<String>> bodyRT = new ArrayList<List<String>>();
					
					
					for (int i = 0; i < someNodeList.size(); i++) {
						TableTag smallReceiptTable = (TableTag) someNodeList.elementAt(i);
						TableRow[] smallReceiptRows = smallReceiptTable.getRows();
						for (int j = 1; j < smallReceiptRows.length; j++) {
							TableColumn[] smallReceiptColumns = smallReceiptRows[j].getColumns();
							if(smallReceiptColumns.length == 8) {
								List<String> paymentRow = new ArrayList<String>();
								paymentRow.add(smallReceiptColumns[2].toPlainTextString().trim());
								paymentRow.add(smallReceiptColumns[7].toPlainTextString().trim().replaceAll("[$,]", ""));
								paymentRow.add(smallReceiptColumns[6].toPlainTextString().trim());
								bodyRT.add(paymentRow);
							}
						}	
					}
					
					
					
					map.put("ReceiptNumber", new String[] {"ReceiptNumber", "" });
					map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
					map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
					receipts.setHead(header);
					receipts.setMap(map);
					receipts.setBody(bodyRT);
					receipts.setReadOnly();
					resultMap.put("TaxHistorySet", receipts);
				
				}
				
				GenericFunctions1.stdPisWilliamsonAO(resultMap, searchId);
				GenericFunctions1.partyNamesTNShelbyTR(resultMap, searchId);
				ro.cst.tsearch.servers.functions.TNShelbyEP.taxShelbyEP(resultMap, searchId);
				
				
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	
	private String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}

	@Override
	protected String extraText(int fromIdx, String rsResponce) {
		int iTmp1 = rsResponce.indexOf("History&Year=", fromIdx);
		iTmp1 += 13;
		return "<b>Tax Year: " + rsResponce.substring(iTmp1, rsResponce.indexOf("&", iTmp1)) + "</b><br>";
	}

	@Override
	protected String getOtherLinksHref(String rsResponce, int linkStartIdx, String linkEnd) {
		return rsResponce.substring(linkStartIdx, rsResponce.indexOf(linkEnd, linkStartIdx)).replaceAll("\\+", " ");
	}

	@Override
	protected String getFileNameFromLink(String link) {
		String parcelId = StringUtils.getTextBetweenDelimiters("Parcel=", "&", link).trim();
		return parcelId + ".html";
	}
	
	
	/*public ServerResponse removeUnnecessaryResults(ServerResponse response) {
		HashMap<String, ParsedResponse> resultHtml = new HashMap<String, ParsedResponse>();
		Vector<ParsedResponse> newResultsRows = new Vector<ParsedResponse>();
		
		for( int i = 0 ; i < response.getParsedResponse().getResultsCount() ; i ++ ) {
			ParsedResponse pr = (ParsedResponse) (response.getParsedResponse().getResultRows()).elementAt( i );
			Matcher intermediateMatcher = intermediateResultsPattern.matcher( pr.getResponse() );
			
			if( !intermediateMatcher.find() ){
				continue;
			}
			String link = "";
			if(intermediateMatcher.groupCount() >= 3)
				link = intermediateMatcher.group(1);
			
			if(!link.isEmpty()) {
				if(!resultHtml.containsKey(link)){
					resultHtml.put(link, pr);
					newResultsRows.add(pr);
				}
			}
		}
		response.getParsedResponse().setResultRows( newResultsRows );
		return response;
	}
	*/

}

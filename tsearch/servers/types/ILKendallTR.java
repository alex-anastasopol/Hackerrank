package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

@SuppressWarnings("deprecation")
public class ILKendallTR extends TSServerAssessorandTaxLike {

	private static final long serialVersionUID = -356001205651646977L;
	
	private static String ALL_NAME_RELATIONSHIPS = "";
	private static String ALL_OWNER_TYPES = "";
	private static String ALL_TAX_CODES = "";
	private static String ALL_TAX_DISTRICTS = "";
	private static String ALL_NEIGHBORHOODS = "";
	
	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath	+ "] does not exist. Module Information not loaded!");
		}
		try {
			ALL_NAME_RELATIONSHIPS = FileUtils.readFileToString(new File(folderPath	+ File.separator + "ILKendallTRAllNameRelationships.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_OWNER_TYPES = FileUtils.readFileToString(new File(folderPath	+ File.separator + "ILKendallTRAllOwnerTypes.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_TAX_CODES = FileUtils.readFileToString(new File(folderPath	+ File.separator + "ILKendallTRAllTaxCodes.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_TAX_DISTRICTS = FileUtils.readFileToString(new File(folderPath	+ File.separator + "ILKendallTRAllTaxDistricts.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_NEIGHBORHOODS = FileUtils.readFileToString(new File(folderPath	+ File.separator + "ILKendallTRAllNeighborhoods.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getALL_NAME_RELATIONSHIPS() {
		return ALL_NAME_RELATIONSHIPS;
	}

	public static String getALL_OWNER_TYPES() {
		return ALL_OWNER_TYPES;
	}

	public static String getALL_TAX_CODES() {
		return ALL_TAX_CODES;
	}

	public static String getALL_TAX_DISTRICTS() {
		return ALL_TAX_DISTRICTS;
	}

	public static String getALL_NEIGHBORHOODS() {
		return ALL_NEIGHBORHOODS;
	}

	public ILKendallTR(long searchId) {
		super(searchId);
	}

	public ILKendallTR(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX38);

		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(10), ALL_NAME_RELATIONSHIPS);
			setupSelectBox(tsServerInfoModule.getFunction(11), ALL_OWNER_TYPES);
			setupSelectBox(tsServerInfoModule.getFunction(23), ALL_TAX_CODES);
			setupSelectBox(tsServerInfoModule.getFunction(24), ALL_TAX_DISTRICTS);
			setupSelectBox(tsServerInfoModule.getFunction(26), ALL_NEIGHBORHOODS);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		
		case ID_SEARCH_BY_MODULE38:			//Advanced Search
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,	outputTable.toString());
			} else {
				Response.getParsedResponse().setError("No data found.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}

			break;

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			if (rsResponse.indexOf("was not found.  Please enter a different number or select a different search method.") > -1) {
				Response.getParsedResponse().setError("No data found.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
			break;

		default:
			break;
		}

	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {

			Matcher ma = Pattern.compile("(?is)\\bInformation\\s+for\\s+Parcel\\s+([0-9-]+),").matcher(rsResponse);
			
			String parcelID = "";
			
			/* If from memory - use it as is */
			if (!rsResponse.toLowerCase().contains("<html")) {
				if (ma.find()) {
					parcelID = ma.group(1);
					parcelNumber.append(parcelID);
				}
				
				return rsResponse;
			}
			
			StringBuilder details = new StringBuilder();

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);

			if (ma.find()) {
				parcelID = ma.group(1);
				parcelNumber.append(parcelID);
			}

			parcelID = parcelID.replaceAll("-", "");
			
			String source = "";
			Matcher sourceMatcher = Pattern.compile("(?is)<div\\s+id=\"item_source\"\\s+value=\"([^\"]+)\"\\s*>").matcher(rsResponse);
			if (sourceMatcher.find()) {
				source = sourceMatcher.group(1);
			}
			
			String sourceDiv = "<div id=\"sourceDiv\" style=\"display: none;\">" + source + "</div>";
			details.append(sourceDiv);
			
			String currentYear = "";
			List<String> years = new ArrayList<String>();
			Node taxYearNode = HtmlParser3.getNodeByID("tax_year", nodeList, true);
			if (taxYearNode!=null) {
				String taxYearString = taxYearNode.toHtml();
				Matcher currentYearsMatcher = Pattern.compile("(?is)<option\\s+selected=\"selected\"\\s*>(\\d{4})</option>").matcher(taxYearString);
				if (currentYearsMatcher.find()) {
					currentYear = currentYearsMatcher.group(1);
				}
				Matcher yearsMatcher = Pattern.compile("(?is)<option>(\\d{4})</option>").matcher(taxYearString);
				while (yearsMatcher.find()) {
					String year = yearsMatcher.group(1); 
					if (year.compareTo(currentYear)<0) {
						years.add(year);
					}
				}
			}
			
			String link = getBaseLink() + "/print/" + source + "/"  + parcelID + "/" + currentYear;
			String currentYearPage = getLinkContents(link);
			org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(currentYearPage, null);
			NodeList nodeList2 = htmlParser2.parse(null);
			Node content = HtmlParser3.getNodeByID("printcontent", nodeList2, true);
			if (content!=null) {
				Matcher headerMatcher = Pattern.compile("(?is)<h2>Information\\s+for\\s+Parcel\\s+.*?</h2>").matcher(currentYearPage);
				if (headerMatcher.find()) {
					details.append(headerMatcher.group(0));
				}
				currentYearPage = content.toHtml().replaceAll("(?is)<script>\\s*window.print\\(\\);\\s*</script>", "");
				currentYearPage = currentYearPage.replaceAll("(?is)(<table)", "<br>$1 border=\"1\" width=\"100%\" style=\"border-collapse: collapse\"");
				currentYearPage = currentYearPage.replaceFirst("(?is)<br>\\s*(<table)", "$1");
				currentYearPage = currentYearPage.replaceAll("(?is)<span[^>]*>Generated\\s*<script>.*?</script>.*?</span>", "");
				currentYearPage = currentYearPage.replaceAll("(?is)(<thead\\s+class=\"ui-state-active\">\\s*<tr)(>)", 
					"$1 style=\"background-color: #baa97b; color: black;\"$2");
				details.append(currentYearPage);
			}
			
			details.append("<h3>Tax History</h3>");
			for (int i=0;i<years.size();i++) {
				String year = years.get(i);
				link = getBaseLink() + "/print/" + source + "/"  + parcelID + "/" + year;
				String page = getLinkContents(link);
				org.htmlparser.Parser htmlParser3 = org.htmlparser.Parser.createParser(page, null);
				NodeList nodeList3 = htmlParser3.parse(null);
				Node payments = HtmlParser3.getNodeByTypeAndAttribute(nodeList3, "table", "summary", "Payments", true);
				if (payments!=null) {
					page = payments.toHtml();
					page = page.replaceAll("(?is)(<table)", "$1 border=\"1\" width=\"100%\" style=\"border-collapse: collapse\"");
					page = page.replaceAll("(?is)(<thead\\s+class=\"ui-state-active\">\\s*<tr)(>)", 
						"$1 style=\"background-color: #baa97b; color: black;\"$2");
					if (i>0) {
						details.append("<br><br>");
					}
					details.append("<b>Tax Tear ").append(year).append("</b>").append(page);
				}
				Node paymentDetail = HtmlParser3.getNodeByTypeAndAttribute(nodeList3, "table", "summary", "Payment Detail", true);
				if (paymentDetail!=null) {
					page = paymentDetail.toHtml();
					page = page.replaceAll("(?is)(<table)", "$1 border=\"1\" width=\"100%\" style=\"border-collapse: collapse\"");
					page = page.replaceAll("(?is)(<thead\\s+class=\"ui-state-active\">\\s*<tr)(>)", 
						"$1 style=\"background-color: #baa97b; color: black;\"$2");
					details.append("<br>").append(page);
				}
			}
			
			String detailsString = details.toString();
			detailsString = detailsString.replaceAll("(?is)(<td[^>]+)class=\"([A-Z]+)\"([^>]*>)", "$1 style=\"color:$2\"$3");
			detailsString = detailsString.replaceAll("(?is)<td[^>]+class=\"[^\"]+\\bhidden\"[^>]*>.*?</td>", "");
			detailsString = detailsString.replaceAll("(?is)<img[^>]+>", "");
			return detailsString;

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			StringBuilder newtable = new StringBuilder();
			
			JSONObject jsonObject = new JSONObject(table);
			JSONObject results = (JSONObject)jsonObject.get("results");
			if (results!=null) {
				JSONArray parcel = (JSONArray) results.get("parcel");
				int len = parcel.length();
				for (int i=0;i<len;i++) {
					JSONObject elem = (JSONObject)parcel.get(i);
					String parcelID = (String)elem.get("property_key");
					String name = (String)elem.get("name");
					String address = (String)elem.get("address");
					String source = (String)elem.get("source");
					String year = (String)elem.get("year");
					String link = "/view/" + source + "/" + parcelID.replaceAll("-", "") + "/" + year;
					link = CreatePartialLink(TSConnectionURL.idGET) + link;
					String htmlRow = "<tr><td><a href=\"" + link + "\">" + parcelID + "</a></td><td>" + 
						name + "</td><td>" + address + "</td></tr>";
					newtable.append(htmlRow);
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = new ResultMap();
					m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
					m.put("tmpOwnerName", name);
					m.put("tmpAddress", address);
					ro.cst.tsearch.servers.functions.ILMcHenryTR.partyNamesILMcHenryTR(m, searchId);
					ro.cst.tsearch.servers.functions.ILKendallTR.parseAddress(m);
					m.removeTempDef();
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
			response.getParsedResponse().setHeader("<table align=\"center\" border=\"1\"><tr>" +
					"<th>Account/Parcel Number</th><th>Name</th><th>Address</th></tr>");
				
			response.getParsedResponse().setFooter("</table>");
												
			outputTable.append(newtable);
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
	
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
		
		try {
						
			Matcher ma1 = Pattern.compile("(?is)\\bInformation\\s+for\\s+Parcel\\s+([0-9-]+)\\s*,\\s+Tax\\s+Year\\s+(\\d+)").matcher(detailsHtml);
			if (ma1.find()) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), ma1.group(1));
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), ma1.group(2));
			}
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String baseAmount = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Total Tax"), "", true);
			baseAmount = baseAmount.replaceAll("(?is)\\bTotal\\s+Tax\\b", "").replaceAll("(?is)<[^>]+>", "");
			baseAmount = baseAmount.replaceAll("[$,()]", "").trim();
			resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			
			Node relatedNamesNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "summary", "Related Names", true);
			if (relatedNamesNode!=null) {
				String relatedNameTable = relatedNamesNode.toHtml();
				StringBuilder ownerName = new StringBuilder();
				List<List<String>> ownerNameList = HtmlParser3.getTableAsList(relatedNameTable, false);
					for (int i=1;i<ownerNameList.size();i++){
						List<String> name = ownerNameList.get(i);
						if (name.size()>2) {
							if (name.get(1).toUpperCase().contains("OWNER") && name.get(2).toUpperCase().contains("CURRENT")){
								ownerName.append(StringEscapeUtils.unescapeXml(name.get(0).trim())).append("@@@");
							}
						}
					}
				String ownerNameString = ownerName.toString();
				ownerNameString = ownerNameString.replaceAll("(?is)@@@$", "");
				resultMap.put("tmpOwnerName", StringUtils.isNotEmpty(ownerNameString) ? ownerNameString : "");
			}
			
			Node siteAddressesNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "summary", "Site Addresses", true);
			if (siteAddressesNode!=null) {
				if (siteAddressesNode instanceof TableTag) {
					TableTag siteAddressestable = (TableTag)siteAddressesNode;
					if (siteAddressestable.getRowCount()>2) {
						TableRow row1 = siteAddressestable.getRow(1);
						if (row1.getColumnCount()>2) {
							String houseNumber = row1.getColumns()[0].toPlainTextString().replaceAll("(?is)House\\s+Number", "").trim();
							String houseNumberSuffix  = row1.getColumns()[1].toPlainTextString().replaceAll("(?is)House\\s+Number\\s+Suffix", "").trim();
							houseNumber += " " + houseNumberSuffix;
							resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), houseNumber.trim());
							String streetName = row1.getColumns()[2].toPlainTextString().replaceAll("(?is)Street\\s+Name", "").trim();
							resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringEscapeUtils.unescapeXml(streetName));
						}
						TableRow row2 = siteAddressestable.getRow(2);
						if (row2.getColumnCount()>2) {
							String city = row2.getColumns()[0].toPlainTextString().replaceAll("(?is)City", "").trim();;
							resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), StringEscapeUtils.unescapeXml(city));
							String zipCode = row2.getColumns()[2].toPlainTextString().replaceAll("(?is)Zip\\s+Code", "").trim();
							resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zipCode);
						}
					}
				}
			}
			
			Node legalDescriptionsNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "summary", "Legal Descriptions", true);
			if (legalDescriptionsNode!=null) {
				if (legalDescriptionsNode instanceof TableTag) {
					TableTag legalDescriptionstable = (TableTag)legalDescriptionsNode;
					if (legalDescriptionstable.getRowCount()>1) {
						TableRow row1 = legalDescriptionstable.getRow(1);
						if (row1.getColumnCount()>1) {
							String legalDescription = row1.getColumns()[0].toPlainTextString().replaceAll("(?is)Legal\\s+Description", "").trim();
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), StringEscapeUtils.unescapeXml(legalDescription));
							String sectionTownshipRange = row1.getColumns()[1].toPlainTextString().replaceAll("(?is)Section/Township/Range", "").trim();
							String[] split = sectionTownshipRange.split("\\s+");
							if (split.length==3) {
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), split[0]);
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), split[1]);
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), split[2]);
							}
						}
					}
				}
			}
			
			Node salesHistoryNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "summary", "Sales History", true);
			if (salesHistoryNode!=null) {
				if (salesHistoryNode instanceof TableTag) {
					TableTag salesHistoryTable = (TableTag)salesHistoryNode;
					if (salesHistoryTable.getRowCount()>2) {
						List<List> tablebodyRef = new ArrayList<List>();
						List<String> list;
						for (int i=2;i<salesHistoryTable.getRowCount();i++) {
							TableRow row = salesHistoryTable.getRow(i);
							if (row.getColumnCount()>6) {
								list = new ArrayList<String>();
								list.add(row.getColumns()[1].toPlainTextString().trim());
								list.add(row.getColumns()[2].toPlainTextString().trim());
								list.add(row.getColumns()[3].toPlainTextString().trim());
								list.add(row.getColumns()[6].toPlainTextString().replaceAll(",", "") .trim());
								tablebodyRef.add(list);
							}
						}
						if (tablebodyRef.size()>0) {
							String[] headerRef = {SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(),SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
								SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName()};
							ResultTable crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
							if (crossRef != null){
								resultMap.put("SaleDataSet", crossRef);
							}
						}
					}
				}
			}
			
			String tmpFirstInstallmentDueDate = "";
			String tmpSecondInstallmentDueDate = "";
			String tmpFirstInstallmentTaxBilled = "";
			String tmpSecondInstallmentTaxBilled = "";
			boolean hasTaxBilled = false;
			String tmpFirstInstallmentTotalBilled = "";
			String tmpSecondInstallmentTotalBilled = "";
			boolean hasTotalBilled = false;
			
			String tmpFirstInstallmentAmountPaid = "";
			String tmpSecondInstallmentAmountPaid = "";
			String tmpFirstInstallmentAmountDue = "";
			String tmpSecondInstallmentAmountDue = "";
			String tmpFirstInstallmentReceipt = "";
			String tmpSecondInstallmentReceipt = "";
			String tmpFirstInstallmentDatePaid = "";
			String tmpSecondInstallmentDatePaid = "";
			
			Node paymentsNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "summary", "Payments", true);
			if (paymentsNode!=null) {
				if (paymentsNode instanceof TableTag) {
					TableTag paymentsTable = (TableTag)paymentsNode;
					for (int i=2;i<paymentsTable.getRowCount();i++) {
						TableRow row = paymentsTable.getRow(i);
						if (row.getColumnCount()>0) {
							String label = row.getColumns()[0].toPlainTextString().trim();
							if (label.equalsIgnoreCase("First")) {
								if (row.getColumnCount()>7) {
									tmpFirstInstallmentDueDate = row.getColumns()[1].toPlainTextString().trim().replaceAll("[$,()]+", "");
									tmpFirstInstallmentTaxBilled = row.getColumns()[2].toPlainTextString().trim().replaceAll("[$,()]+", "");
									hasTaxBilled = true;
									tmpFirstInstallmentTotalBilled = row.getColumns()[6].toPlainTextString().trim().replaceAll("[$,()]+", "");
									hasTotalBilled = true;
									tmpFirstInstallmentAmountPaid = row.getColumns()[7].toPlainTextString().trim().replaceAll("[$,()]+", "");
									tmpFirstInstallmentAmountDue = row.getColumns()[8].toPlainTextString().trim().replaceAll("[$,()]+", "");
								}
							} else if (label.equalsIgnoreCase("Second")) {
								if (row.getColumnCount()>7) {
									tmpSecondInstallmentDueDate = row.getColumns()[1].toPlainTextString().trim().replaceAll("[$,()]+", "");
									tmpSecondInstallmentTaxBilled = row.getColumns()[2].toPlainTextString().trim().replaceAll("[$,()]+", "");
									hasTaxBilled = true;
									tmpSecondInstallmentTotalBilled = row.getColumns()[6].toPlainTextString().trim().replaceAll("[$,()]+", "");
									hasTotalBilled = true;
									tmpSecondInstallmentAmountPaid = row.getColumns()[7].toPlainTextString().trim().replaceAll("[$,()]+", "");
									tmpSecondInstallmentAmountDue = row.getColumns()[8].toPlainTextString().trim().replaceAll("[$,()]+", "");
								}
							}
						}
					}
				}
			}
			
			Node paymentDetailNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "summary", "Payment Detail", true);
			if (paymentDetailNode!=null) {
				if (paymentDetailNode instanceof TableTag) {
					TableTag paymentDetailNodeTable = (TableTag)paymentDetailNode;
					for (int i=2;i<paymentDetailNodeTable.getRowCount();i++) {
						TableRow row = paymentDetailNodeTable.getRow(i);
						if (row.getColumnCount()>0) {
							String label = row.getColumns()[0].toPlainTextString().trim();
							if (label.equalsIgnoreCase("First")) {
								if (row.getColumnCount()>2) {
									if(!tmpFirstInstallmentAmountPaid.matches("0[.]0+")) {
										tmpFirstInstallmentReceipt = row.getColumns()[1].toPlainTextString().trim();
										tmpFirstInstallmentDatePaid = row.getColumns()[2].toPlainTextString().trim();
									}
								}
							} else if (label.equalsIgnoreCase("Second")) {
								if (row.getColumnCount()>2) {
									if(!tmpSecondInstallmentAmountPaid.matches("0[.]0+")) {
										tmpSecondInstallmentReceipt = row.getColumns()[1].toPlainTextString().trim();
										tmpSecondInstallmentDatePaid = row.getColumns()[2].toPlainTextString().trim();
									}
								}
							} else if (label.equalsIgnoreCase("Both")) {
								if (row.getColumnCount()>2) {
									if(!tmpFirstInstallmentAmountPaid.matches("0[.]0+") && !tmpSecondInstallmentAmountPaid.matches("0[.]0+")) { 
										tmpFirstInstallmentDatePaid = row.getColumns()[2].toPlainTextString().trim();
										tmpSecondInstallmentDatePaid = row.getColumns()[2].toPlainTextString().trim();
									}
								}
							}
						}
					}
				}
			}
			
			String tmpFirstInstallmentBaseAmount = "";
			String tmpSecondInstallmentBaseAmount = "";
			if (hasTaxBilled) {
				tmpFirstInstallmentBaseAmount = tmpFirstInstallmentTaxBilled;
				tmpSecondInstallmentBaseAmount = tmpSecondInstallmentTaxBilled;
			} else if (hasTotalBilled) {
				tmpFirstInstallmentBaseAmount = tmpFirstInstallmentTotalBilled;
				tmpSecondInstallmentBaseAmount = tmpSecondInstallmentTotalBilled;
			}
			
			resultMap.put("tmpFirstInstallmentBaseAmount", tmpFirstInstallmentBaseAmount);
			resultMap.put("tmpSecondInstallmentBaseAmount", tmpSecondInstallmentBaseAmount);
			
			resultMap.put("tmpFirstInstallmentAmountPaid", tmpFirstInstallmentAmountPaid);
			resultMap.put("tmpSecondInstallmentAmountPaid", tmpSecondInstallmentAmountPaid);
			
			resultMap.put("tmpFirstInstallmentAmountDue", tmpFirstInstallmentAmountDue);
			resultMap.put("tmpSecondInstallmentAmountDue", tmpSecondInstallmentAmountDue);
			
			resultMap.put("tmpFirstInstallmentReceipt", tmpFirstInstallmentReceipt);
			resultMap.put("tmpSecondInstallmentReceipt", tmpSecondInstallmentReceipt);
			
			resultMap.put("tmpFirstInstallmentDatePaid", tmpFirstInstallmentDatePaid);
			resultMap.put("tmpSecondInstallmentDatePaid", tmpSecondInstallmentDatePaid);
			
			resultMap.put("tmpFirstInstallmentDueDate", tmpFirstInstallmentDueDate);
			resultMap.put("tmpSecondInstallmentDueDate", tmpSecondInstallmentDueDate);
			
			ro.cst.tsearch.servers.functions.ILMcHenryTR.partyNamesILMcHenryTR(resultMap, searchId);
			ro.cst.tsearch.servers.functions.ILMcHenryTR.legalILMcHenryTR(resultMap, searchId);
			ro.cst.tsearch.servers.functions.ILMcHenryTR.taxILMcHenryTR(resultMap, searchId);
		
		} catch (ParserException pe) {
			logger.error("Error while getting details", pe);
		} catch (Exception e) {
			logger.error("Error parsing names", e);
		}
		return null;
	}

	@Override
	protected int getResultType(){
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		boolean result = mSearch.getSa().getPins(-1).size() > 1 &&
			    		 mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
		return result?true:super.anotherSearchForThisServer(sr);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		FilterResponse cityFilter = CityFilterFactory.getCityFilter(searchId, 0.6d);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){
				Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				search.setAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN, Boolean.TRUE);
				for(String pin: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(0).forceValue(pin);
					modules.add(module);	
				}			
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
			
			if(hasPin()){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);  
				modules.add(module);		
			}
			
			if (hasStreet()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));
				module.clearSaKeys();
				module.getFunction(13).forceValue(streetNo);
				module.getFunction(16).forceValue(streetName);
				module.addFilter(cityFilter);
				module.addFilter(addressFilter);
				modules.add(module);
			}
			
			if(hasOwner()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module);
				nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.addFilter(cityFilter);
				module.addFilter(nameFilterHybridDoNotSkipUnique);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.
						getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;f;"});
				module.addIterator(nameIterator);
				modules.add(module);
	        }
		}
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	public void  addAdditionalDocuments(DocumentI doc, ServerResponse response) {
		if (numberOfYearsAllowed > 1 && mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
        	numberOfYearsAllowed--; 
            try {
				addDocument(response, ((TaxDocumentI) doc).getInstno(), numberOfYearsAllowed, ((TaxDocumentI) doc).getYear());
			} catch (ServerResponseException e) {
				e.printStackTrace();
			}
        }
	}
	
	protected void addDocument(ServerResponse response, String pid, int numberOfYearsAllowed, int yearInt) throws ServerResponseException{
		
		String source  = "";
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response.getResult(), null);
		try {
			NodeList nodeList = htmlParser.parse(null);
			Node sourceDivNode = HtmlParser3.getNodeByID("sourceDiv", nodeList, true);
			if (sourceDivNode!=null) {
				source = sourceDivNode.toPlainTextString().trim();
			} else {
				Matcher sourceMatcher = Pattern.compile("(?is)<div\\s+id=\"item_source\"\\s+value=\"([^\"]+)\"\\s*>").matcher(response.getResult());
				if (sourceMatcher.find()) {
					source = sourceMatcher.group(1);
				}
			}	
		} catch (ParserException e) {
			logger.error("Error while getting source", e);
		}
		
		String sAction = getBaseLink() + "/view/" + source + "/"  + pid.replaceAll("-", "") + "/" + Integer.toString(yearInt-1);
		String yearContent = getLinkContents(sAction);
		ServerResponse Response = new ServerResponse();
		Response.setResult(yearContent);
		
		yearContent = getDetails(yearContent, new StringBuilder());
			
		super.solveHtmlResponse(sAction, ID_SAVE_TO_TSD, "SaveToTSD", Response, yearContent);
	}
	
}

package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;

@SuppressWarnings("deprecation")
public class KSWyandotteTR extends TSServer {

	private static final long serialVersionUID = 3370989821814382995L;

	public static final String PARAMETER_NAMES[][] = {{"Name"},	//Name Search
		{"Number", "Direction", "Name", "AddressType", "City"},	//Address Search
		{"Subdivision"}};										//Subdivision Search
	
	private static int seq = 0;
	
	protected synchronized static int getSeq() {
		return seq++;
	}
	
	//for Subdivision combobox
	private static String SUBDIVISION_SELECT = "";
	
	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH
						+ "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath
					+ "] does not exist. Module Information not loaded!");
		}
		try {
			String select = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath
					+ File.separator + "KSWyandotteTRSubdivision.xml"));
			SUBDIVISION_SELECT = select;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public KSWyandotteTR(long searchId) {
		super(searchId);
	}

	public KSWyandotteTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule1 = msiServerInfoDefault
				.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);

		if (tsServerInfoModule1 != null) {
			setupSelectBox(tsServerInfoModule1.getFunction(0), SUBDIVISION_SELECT);
		}
		
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_INSTRUMENT_NO: 				//State Parcel Search
		case ID_SEARCH_BY_SUBDIVISION_NAME:
			
			if (rsResponse.indexOf("Name was not found") > -1) {
				Response.getParsedResponse().setError("No data found.");
				return;
			}
			
			if (rsResponse.indexOf("Select a subdivision") > -1) {
				Response.getParsedResponse().setError("No data found.");
				return;
			}
			
			if (rsResponse.indexOf("KUPN does not exist") > -1) {
				Response.getParsedResponse().setError("No data found.");
				return;
			}
			
			if (rsResponse.indexOf("We're sorry, your search produced no results") > -1) {
				Response.getParsedResponse().setError("No data found.");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
					Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(
						smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
						outputTable.toString());
			}

			break;

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			if (rsResponse.indexOf("Parcel Number invalid") > -1) {
				Response.getParsedResponse().setError("No data found.");
				return;
			}
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.toLowerCase()
					.contains("parcel information")?ID_SEARCH_BY_PARCEL:ID_SEARCH_BY_NAME);
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

			rsResponse = Tidy.tidyParse(rsResponse, null);
			
			StringBuilder details = new StringBuilder();

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);

			/* If from memory - use it as is */
			if (!rsResponse.toLowerCase().contains("<html")) {
				NodeList parcelList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("span"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_FormView1_Label1"));
				String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
				parcelNumber.append(parcelID);

				return rsResponse;
			}

			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_FormView1_Label1"));
			String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
			parcelNumber.append(parcelID);

			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("style", "background-image: url(/Wycokck/Images/Internet2_Tab_BeforeCUT_05.jpg); padding-right: 25px; padding-left: 23px;"));
			if (tables.size() > 0) {
				details.append(tables.elementAt(0).toHtml());
			}
			
			String detailsString = details.toString();
			
			//table headers
			detailsString = detailsString.replaceAll("(?is)(<td\\s+class=\\\"sectionHeading (?:full|(?:longer)?half)Heading\\\"\\s*>\\s*<span>)(.*?)(</span>\\s*</td>)", 
				"$1<b>$2</b>$3");
			
			//different formatting in table headers
			detailsString = detailsString.replaceAll("(?is)(<span[^>]+class=\"ApprNonCertDisclaimer\"[^>]*>)([^<]+)", "$1($2)");
						
			detailsString = detailsString.replaceAll("(?is)<img[^>]+>", "");
			detailsString = detailsString.replaceAll("(?is)<input[^>]+>", "");
			detailsString = detailsString.replaceAll("(?is)<a[^>]+>[^<]+</a>", "");
			
			return detailsString;

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		StringBuilder linkForPages = new StringBuilder();
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				linkForPages.append("/Search/SearchResults.aspx");
				moduleSource = (TSServerInfoModule) objectModuleSource;
				int index = moduleSource.getParserID();
				if (index==ID_SEARCH_BY_NAME) {
					linkForPages.append("?Type=Name");
					for (int i=0;i<PARAMETER_NAMES[0].length;i++) {
						String value = moduleSource.getParamValue(i);
						if (StringUtils.isEmpty(value)) {
							value = "";
						}
						linkForPages.append("&").append(PARAMETER_NAMES[0][i]).append("=").append(value);
					}
				} else if (index==ID_SEARCH_BY_ADDRESS) {
					linkForPages.append("?Type=Address");
					for (int i=0;i<PARAMETER_NAMES[1].length;i++) {
						String value = moduleSource.getParamValue(i);
						if (StringUtils.isEmpty(value)) {
							value = "";
						}
						linkForPages.append("&").append(PARAMETER_NAMES[1][i]).append("=").append(value);
					}
				} else if (index==ID_SEARCH_BY_SUBDIVISION_NAME) {
					linkForPages.append("?Type=Subdivision");
					for (int i=0;i<PARAMETER_NAMES[2].length;i++) {
						String value = moduleSource.getParamValue(i);
						if (StringUtils.isEmpty(value)) {
							value = "";
						}
						linkForPages.append("&").append(PARAMETER_NAMES[2][i]).append("=").append(value);
					}
				}
			}
		} else {
			linkForPages = new StringBuilder(response.getLastURI().toString());
		}
		
		TableTag resultsTable = null;
		String header = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_SearchGridView"), true);

			if (mainTable.size() != 0)
				resultsTable = (TableTag) mainTable.elementAt(0);

			// if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 0) {
				
				TableRow[] rows = resultsTable.getRows();

				// row 0 is the header
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];

					//find another rows with the same Parcel
					//if the names are different, add them to the current row
					if (row.getColumnCount()==4) {
						String currentParcel = row.getColumns()[3].toPlainTextString().trim();  
						for (int j=i+1;j<rows.length;j++) {
							if (rows[j].getColumnCount()==4) {
								String otherParcel = rows[j].getColumns()[3].toPlainTextString().trim();
								if (currentParcel.equals(otherParcel)) {
									NodeList currentChildren = row.getChildren();
									NodeList otherChildren = rows[j].getChildren();
									String currentNames = currentChildren.elementAt(2).getFirstChild().getText();
									String otherNames = otherChildren.elementAt(2).getFirstChild().getText(); 
									if (!currentNames.contains(otherNames)) {
										currentChildren.elementAt(2).getFirstChild().setText(
												 currentNames +	"<br>" + otherNames);
										row.setChildren(currentChildren);
									}
									otherChildren.elementAt(4).getFirstChild().setText("ALREADYSAVED");
									rows[j].setChildren(otherChildren);
								}
							} 
						}
					}
					
					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();

					String link = linkForPages.toString();
					String column = row.getColumns()[0].toHtml();
					String eventTarget = "";
					String eventArgument = "";
					boolean alreadySaved = true;
					if (row.getColumnCount()==4) {			//table row
						String parcel = row.getColumns()[3].toPlainTextString().trim();
						if (!"ALREADYSAVED".equals(parcel)) {
							alreadySaved = false;
							Matcher matcher = Pattern.compile("(?is)\"javascript:__doPostBack\\('([^']+)','([^']+)'\\)\\\"").matcher(column);
							if (matcher.find()) {
								Map<String, String> params = 
									ro.cst.tsearch.connection.http2.KSWyandotteTR.isolateParams(response.getResult(), "aspnetForm");
								params.remove("__EVENTTARGET");
								params.remove("__EVENTARGUMENT");
								eventTarget = matcher.group(1);
								eventArgument = matcher.group(2);
								params.put("__EVENTTARGET", eventTarget);
								params.put("__EVENTARGUMENT", eventArgument);
								int seq = getSeq();
								mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
								link = CreatePartialLink(TSConnectionURL.idPOST) + link + "&seq=" + seq;
								htmlRow = htmlRow.replaceFirst("(?is)<td>\\s*<input[^>]+>\\s*</td>", 
										"<td align=\"center\"><a href=\"" + link + "\">select</a>");
							}
						}
					} 
										
					if (!alreadySaved) {
						currentResponse.setPageLink(new LinkInPage(link, link,	TSServer.REQUEST_SAVE_TO_TSD));

						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
						currentResponse.setOnlyResponse(htmlRow);

						ResultMap m = ro.cst.tsearch.servers.functions.KSWyandotteTR.parseIntermediaryRow(row, searchId);
						Bridge bridge = new Bridge(currentResponse, m, searchId);

						DocumentI document = (TaxDocumentI) bridge.importData();
						currentResponse.setDocument(document);
					
						intermediaryResponse.add(currentResponse);
					}
				
				}

				header = "<table width=\"100%\" cellspacing=\"0\" border=\"border=0\"><tr>"
						+ "<th align=\"center\">&nbsp;</th>"
						+ "<th align=\"left\">Property Owner Name</th>"
						+ "<th align=\"left\">Property Address</th>"
						+ "<th align=\"left\">Parcel</th>"
						+ "</tr>";
				
				
				String footer = "";
				TableRow lastRow = rows[rows.length-1];
				if (lastRow.getColumnCount()!=4) { 		//row with navigation links (1 2 ...)
					String column = lastRow.getColumns()[0].toHtml();
					Matcher matcher = Pattern.compile("(?is)<a href=\"javascript:__doPostBack\\('([^']+)','([^']+)'\\)\">").matcher(column);
					String eventTarget = "";
					String eventArgument = "";
					footer = lastRow.toHtml();
					String link = linkForPages.toString();
					while (matcher.find()) {
						Map<String, String> params = 
							ro.cst.tsearch.connection.http2.KSWyandotteTR.isolateParams(response.getResult(), "aspnetForm");
						params.remove("__EVENTTARGET");
						params.remove("__EVENTARGUMENT");
						eventTarget = matcher.group(1);
						eventArgument = matcher.group(2);
						params.put("__EVENTTARGET", eventTarget);
						params.put("__EVENTARGUMENT", eventArgument);
						int seq = getSeq();
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						footer = footer.replace(matcher.group(0), 
								"<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + link + "&seq=" + seq + "\">");
					}
				}
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer + "</table>");

				outputTable.append(table);
			}

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
	
		try {
						
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
							
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_FormView1_Label1"));
			if (parcelList.size()>0) {
				String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			}
			
			NodeList kupnList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_FormView1_Label3"));
			if (kupnList.size()>0) {
				String kupn = kupnList.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), kupn);
			}

			NodeList sectionList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_FormView1_Label9"));
			if (sectionList.size()>0) {
				String section = sectionList.elementAt(0).toPlainTextString();
				section = section.replaceFirst("^[^\\d+]+", "");
				section = section.replaceFirst("^0+", "").trim();
				if (StringUtils.isNotEmpty(section)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
				}
			}
			
			NodeList addressList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_AddressDataList"));
			if (addressList.size()>0) {
				TableTag addressTable = (TableTag)addressList.elementAt(0);
				StringBuilder sb = new StringBuilder();
				for (int i=0;i<addressTable.getRowCount();i++) {
					sb.append(addressTable.getRows()[i].toPlainTextString().trim()).append("<br>");
				}
				String address = sb.toString();
				address = address.replaceAll("(?is)&nbsp;", " ");
				address = address.replaceAll("\\s{1,}", " ");
				address = address.trim().replaceFirst("(?is)<br>$", "");
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			}
			
			NodeList subdivisionList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_LegalDataList_ctl00_SUBDIVLabel"));
			if (subdivisionList.size()>0) {
				String subdivisionName = subdivisionList.elementAt(0).toPlainTextString().trim();
				if (StringUtils.isNotEmpty(subdivisionName)) {
					subdivisionName = subdivisionName.replaceAll("(?is)\\bB\\d+(-\\d+)?\\b", "");
					subdivisionName = subdivisionName.replaceAll("(?is)\\bL\\d+([-,]\\d+)?\\b", "");
					subdivisionName = subdivisionName.replaceAll("(?is)\\b(RES)\\s*,.*", "$1");
					subdivisionName = subdivisionName.replaceAll("(?is)\\b(SUB)\\b.*", "$1");
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
				}
			}
			
			NodeList strList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_LegalDataList_ctl00_SECTWNRGLabel"));
			if (strList.size()>0) {
				String str = strList.elementAt(0).toPlainTextString().trim();
				Matcher matcher = Pattern.compile("(\\d+)-(\\d+)-(\\d+)").matcher(str);
				if (matcher.matches()) {
					String section = matcher.group(1);
					String township = matcher.group(2);
					String range = matcher.group(3);
					if (!section.matches("0+")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section.replaceFirst("^0+", ""));
					}
					if (!township.matches("0+")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township.replaceFirst("^0+", ""));
					}
					if (!range.matches("0+")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range.replaceFirst("^0+", ""));
					}
				}
			}
			
			NodeList descriptionList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_LegalDataList_ctl00_LEGALLabel"));
			if (descriptionList.size()>0) {
				String description = descriptionList.elementAt(0).toPlainTextString().trim();
				if (StringUtils.isNotEmpty(description)) {
					resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), description);
				}
			}
			
			String bookPage = "";
			NodeList bookPageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_LegalDataList_ctl00_BKLabel"));
			if (bookPageList.size()>0) {
				bookPage = bookPageList.elementAt(0).toPlainTextString().trim();
				String[] bookAndPage = bookPage.split("-");
				if (bookAndPage.length==2) {
					String book = bookAndPage[0].replaceFirst("^0+", "");
					String page = bookAndPage[1].replaceFirst("^0+", "");
					if (book.matches("\\d{4}[A-Za-z]")) {
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), page);
					} else {
						resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book);
						resultMap.put(SaleDataSetKey.PAGE.getKeyName(), page);
					}
				}
			}
			
			NodeList nameList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_AllUserView_OwnerNames"));
			if (nameList.size()>0) {
				TableTag namesTable = (TableTag)nameList.elementAt(0);
				StringBuilder sb = new StringBuilder();
				for (int i=0;i<namesTable.getRowCount();i++) {
					sb.append(namesTable.getRows()[i].toPlainTextString().trim()).append("<br>");
				}
				String name = sb.toString();
				name = name.trim().replaceFirst("(?is)<br>$", "");
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			}
						
			ro.cst.tsearch.servers.functions.KSWyandotteTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.KSWyandotteTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.KSWyandotteTR.parseLegalSummary(resultMap);
			ro.cst.tsearch.servers.functions.KSWyandotteTR.parseTaxes(nodeList, resultMap, searchId);
		
		} catch (ParserException e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			moduleList.add(module);
		}

		boolean hasOwner = hasOwner();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(
					SearchAttributes.OWNER_OBJECT, searchId, module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREETNO);
			module.setSaKey(2, SearchAttributes.P_STREETNAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilter);
			moduleList.add(module);
		}

		if (hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,	searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;", "L;M;" }));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
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

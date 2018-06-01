package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.html.parser.HtmlHelper;
import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;

public class MOPlatteAO extends TemplatedServer {

	private static String SUBMIT_SUBDIVISION_JS_FUNCTION = "";

	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			SUBMIT_SUBDIVISION_JS_FUNCTION = FileUtils.readFileToString(new File(folderPath + File.separator + "MOPlatteAOJS.JS"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MOPlatteAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		int[] intermediary_cases = { ID_SEARCH_BY_NAME, ID_SEARCH_BY_ADDRESS, ID_SEARCH_BY_PARCEL, ID_SEARCH_BY_SUBDIVISION_NAME,
				ID_INTERMEDIARY };
		setDetailsMessage("Assessor Data");
		setIntermediaryMessage(" records were found");
		setIntermediaryCases(intermediary_cases);

	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		String initialResponse = response.getResult();
		switch (viParseID) {
		case ID_SEARCH_BY_SUBDIVISION_NAME:
			HtmlParser3 parser = new HtmlParser3(initialResponse);
			NodeList letterSelectList = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "SELECT", "NAME", "subdivletter",
					true);

			NodeList hiddenParameters = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "INPUT", "type", "Hidden", true);

			String buildHtml = "";
			try {
				String linkStart = CreatePartialLink(TSConnectionURL.idPOST);

				int seq = getSeq();
				SimpleNodeIterator elements = hiddenParameters.elements();
				StringBuilder parametersToSave = new StringBuilder();
				while (elements.hasMoreNodes()) {
					InputTag nextNode = (InputTag) elements.nextNode();
					String name = nextNode.getAttribute("name");
					String value = nextNode.getAttribute("value");
					String format = MessageFormat.format("&{0}={1}", name, value);
					parametersToSave.append(format);
				}
				String paramsLink = "/platteco/searchform.jsp?seq=" + seq;// +
																			// parametersToSave;
				String formAction = linkStart + paramsLink;
				Map<String, String> atsParameters = StringUtils
						.extractParametersFromQuery(formAction.substring(formAction.indexOf("?") + 1));
				FormTag formTag = HtmlHelper.createFormTag(formAction, "searchForm", "POST");

				Tag tableTag = HtmlHelper.createTableTag();
				Tag tableRowTag = HtmlHelper.createTableRowTag();

				// Tag createTableColumnTag = HtmlHelper.createTableColumnTag();
				Tag letterTableColumnTag = HtmlHelper.createTableColumnTag();
				HtmlHelper.addTagToTag(letterTableColumnTag, HtmlHelper.createPlainText("First letter:"));
				HtmlHelper.addTagToTag(letterTableColumnTag, letterSelectList);
				HtmlHelper.addTagToTag(tableRowTag, letterTableColumnTag);
				HtmlHelper.addTagToTag(tableTag, tableRowTag);
				/*
				 * if the response contains the subdivision list then is the
				 * request after selecting the letter.
				 */
				NodeList subdivisionSelectList = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "SELECT", "NAME",
						"subdivname", true);

				CompositeTag submitButton = HtmlHelper.createTagFromText("<input name=\"startSearch\" type=\"button\" value=\"Search\">");
				submitButton.setAttribute("onClick", "submitSubdivision(null);");
				if (subdivisionSelectList != null && subdivisionSelectList.size() > 0) {

					// submitButton.setAttribute("onClick",
					// "submitSubdivision('subdivname');");
					Tag subdivisionTableRowTag = HtmlHelper.createTableRowTag();
					Tag selectTableColumnTag = HtmlHelper.createTableColumnTag();
					HtmlHelper.addTagToTag(selectTableColumnTag, HtmlHelper.createPlainText("Subdivision Name:"));

					HtmlHelper.addTagToTag(selectTableColumnTag, subdivisionSelectList);
					// HtmlHelper.addTagToTag(tableRowTag,
					// selectTableColumnTag);
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
					String format = MessageFormat.format("{0}={1}&", entry.getKey(), entry.getValue());

					parametersToSave.append(format);
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
				this.mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, parametersToSave);

			} catch (Exception e) {
				e.printStackTrace();
			}

			this.parser.Parse(response.getParsedResponse(), buildHtml, Parser.NO_PARSE);
			break;
		case ID_SEARCH_BY_MODULE20:
		case ID_SEARCH_BY_MODULE21:
		case ID_SEARCH_BY_MODULE22:
		case ID_SEARCH_BY_MODULE23: {

			String deedLink = "";

			parser = new HtmlParser3(initialResponse);
			String javaScriptSection = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "SCRIPT", "LANGUAGE", "JavaScript",
					true).asString();
			String pidListEntry = RegExUtils.parseValuesForRegEx(javaScriptSection, "pidList.*(?=\\|\\\")");
			if (StringUtils.isNotEmpty(pidListEntry)) {
				pidListEntry = pidListEntry.replace("pidList", "").replace("=", "").replace("\"", "").trim() + "|";
			}
			Pattern pattern = Pattern.compile("sf\\.(h_.*)=(.*)(?=\\;)");
			Matcher matcher = pattern.matcher(javaScriptSection);
			HashMap<String, String> map = new HashMap<String, String>();

			while (matcher.find()) {
				String param = matcher.group(1).replace(".value", "").trim();
				String paramValue = matcher.group(2).replaceAll("\"", "").trim();
				map.put(param, paramValue);
			}
			if(pidListEntry != null) {
				map.put("h_pid1Entry", URLEncoder.encode(pidListEntry));
			}
			Set<Entry<String, String>> entrySet = map.entrySet();
			StringBuilder linkPArameters = new StringBuilder();
			for (Entry<String, String> entry : entrySet) {
				linkPArameters.append(MessageFormat.format("{0}={1}&", entry.getKey(), entry.getValue()));
			}

			deedLink = "/platteco/searchform.jsp?" + linkPArameters.toString();
			
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				initialResponse = ((ro.cst.tsearch.connection.http2.MOPlatteAO) site).getDeedIntermediary(deedLink);
			}finally {
				HttpManager.releaseSite(site);
			}
			
			response.setResult(initialResponse);
			super.ParseResponse(action, response, ID_INTERMEDIARY);
		}
			break;
		case ID_GET_LINK:
			if (initialResponse.contains("Subdivision Name:")) {
				ParseResponse(action, response, ID_SEARCH_BY_SUBDIVISION_NAME);
			} else {
				super.ParseResponse(action, response, viParseID);
			}
			break;
		default:
			super.ParseResponse(action, response, viParseID);
		}

	}

	@Override
	protected void setMessages() {
		getErrorMessages().addServerErrorMessage("Apache Tomcat/4.1.29 - Error report");
		getErrorMessages().addNoResultsMessages("No records found.");
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String[] links = processLinks(response);

		HtmlParser3 parser = new HtmlParser3(table);
		TableTag resultsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(parser.getNodeList(), "table", "bgcolor", "FFFFFF", true);

		List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap((TableTag) resultsTable);
		outputTable.append(resultsTable.toHtml());
		
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);

		for (int i = 0, j = i + 3; i < tableAsListMap.size(); i++, j = j + 2) {
			ParsedResponse currentResponse = new ParsedResponse();
			Node currentHtmlRow = resultsTable.getChild(j);

			SimpleNodeIterator elements = currentHtmlRow.getChildren().elements();

			int x = 0;
			ResultMap resultMap = new ResultMap();
			while (elements.hasMoreNodes()) {
				Node nextNode = elements.nextNode();
				if (nextNode instanceof TableColumn) {
					String html = nextNode.getChildren().toHtml();
					html = html.replaceAll("(?is)</?font[^>]*>", "");

					if (x == 1) {
						String accountNumber = getAccountNumber(html);
						resultMap.put("PropertyIdentificationSet.ParcelID", accountNumber);
//						ro.cst.tsearch.utils.FileUtils.appendToTextFile(ro.cst.tsearch.servers.functions.MOPlatteAO.TEST_FILES_DEPLOYFOLDER + "name_intermediary_data.txt", accountNumber);
//						ro.cst.tsearch.utils.FileUtils.appendToTextFile(ro.cst.tsearch.servers.functions.MOPlatteAO.TEST_FILES_DEPLOYFOLDER
//								+ "address_intermediary_data.txt", accountNumber);
//						ro.cst.tsearch.utils.FileUtils.appendToTextFile(ro.cst.tsearch.servers.functions.MOPlatteAO.TEST_FILES_DEPLOYFOLDER
//								+ "legal_intermediary_data.txt", accountNumber);
					}

					if (x == 2) {
						resultMap.put("PropertyIdentificationSet.NameOnServer", html);
//						ro.cst.tsearch.utils.FileUtils.appendToTextFile(ro.cst.tsearch.servers.functions.MOPlatteAO.TEST_FILES_DEPLOYFOLDER + "name_intermediary_data.txt", html);
					}

					if (x == 3) {
						resultMap.put("PropertyIdentificationSet.AddressOnServer", html);
//						ro.cst.tsearch.utils.FileUtils.appendToTextFile(ro.cst.tsearch.servers.functions.MOPlatteAO.TEST_FILES_DEPLOYFOLDER
//								+ "address_intermediary_data.txt", html);
					}

					if (x == 4) {
						resultMap.put("PropertyIdentificationSet.Subdivision", html);
					}

					if (x == 5) {
						resultMap.put("PropertyIdentificationSet.LegalDescriptionOnServer", html);
//						ro.cst.tsearch.utils.FileUtils.appendToTextFile(ro.cst.tsearch.servers.functions.MOPlatteAO.TEST_FILES_DEPLOYFOLDER
//								+ "legal_intermediary_data.txt", html);
					}
					x++;
				}

			}

			String text = resultMap + "\r\n\r\n\r\n";
//			ro.cst.tsearch.utils.FileUtils.appendToTextFile(ro.cst.tsearch.servers.functions.MOPlatteAO.TEST_FILES_DEPLOYFOLDER+"intermediary_data.txt", text);

			ro.cst.tsearch.servers.functions.MOPlatteAO.parseAndFillResultMap(resultMap, tableAsListMap.get(i));
			String pin = (String) resultMap.get("PropertyIdentificationSet.ParcelID");

			LinkTag linkTag = HtmlParser3.getFirstTag(currentHtmlRow.getChildren(), LinkTag.class, true);

			String newLink = linkStart + MessageFormat.format("/platteco/resultsform.jsp?h_pid1={0}&h_criteria=gen", pin);
			linkTag.setLink(newLink);

			String rowHtml = currentHtmlRow.toHtml();
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
			currentResponse.setOnlyResponse(rowHtml);
			currentResponse.setPageLink(new LinkInPage(newLink, newLink, TSServer.REQUEST_SAVE_TO_TSD));

			// Map<String, String> parseLegalFromPIN =
			// ro.cst.tsearch.servers.functions.MOPlatteAO.parseLegalFromPIN(pin);

			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			try {
				document = bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			intermediaryResponse.add(currentResponse);

		}

		Node tableHeader = resultsTable.getChild(1);
		String aHref = "<a href=\"{1}\" >{0}</a>";
		String linksTable = "";
		if (links != null) {
			String bof = MessageFormat.format(aHref, "Begining Of Records", links[3]);
			String next = MessageFormat.format(aHref, "Next", links[0]);
			String prev = MessageFormat.format(aHref, "Previous", links[1]);
			String eof = MessageFormat.format(aHref, "End Of Records", links[2]);
			linksTable = MessageFormat.format("<table><tr><td>{0}</td><td>{1}</td><td>{2}</td><td>{3}</td><tr></table>", bof, prev, next,
					eof);
			response.getParsedResponse().setNextLink(links[0]);
		}

		response.getParsedResponse().setHeader(
				linksTable + "<table>" + tableHeader.toHtml().replaceAll("class=\"hdrDateTime\"", "style=\"color: white;\""));

		response.getParsedResponse().setFooter("</table>" + linksTable);
		
		return intermediaryResponse;
	}

	/**
	 * Had to imitate the behavior of "incrRecords" javascript function from
	 * response.
	 * 
	 * @param response
	 * @return
	 */
	private String[] processLinks(ServerResponse response) {
		String baseLink = CreatePartialLink(TSConnectionURL.idPOST) + "/platteco/searchform.jsp";

		String result = response.getResult();

		// get available post parameters
		HtmlParser3 parser = new HtmlParser3(result);

		Node javascriptSource = HtmlParser3.getNodeByTypeAndAttribute(parser.getNodeList(), "SCRIPT", "LANGUAGE", "JavaScript", true);
		NodeList hiddenInput = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "INPUT", "type", "Hidden", true);

		String jsAsText = javascriptSource == null ? "" : javascriptSource.toHtml();
		String incrRecordsJSFunction = RegExUtils.parseValuesForRegEx(jsAsText, "(?is)function incrRecords.*?(?=function)");
		String nextLink = "", previousLink = "", eofLink = "", bofLink = "";
		boolean showLinks = true;

		if (StringUtils.isNotEmpty(incrRecordsJSFunction)) {
			String typeOfSearch = RegExUtils.getFirstMatch("(?is)\"(.*?)\"", incrRecordsJSFunction, 1);
			Pattern pattern = Pattern.compile("var\\s(\\w*)\\s=\\s(\\d+)");
			Matcher matcher = pattern.matcher(incrRecordsJSFunction);
			Map<String, String> functionLocalVariables = new HashMap<String, String>();
			
			
			while (matcher.find()) {
				String variableName = matcher.group(1);
				String variableValue = matcher.group(2);

				functionLocalVariables.put(variableName, variableValue);
			}
			//for deed take count and limit from second encounter
			if (RegExUtils.matches("\"deed\" == \"deed\"", incrRecordsJSFunction)){
				String currentLimit = functionLocalVariables.get("limit");
				String deedIf = RegExUtils.getFirstMatch("(?is)if \\(\\\"deed\\\" == \\\"deed\\\"\\) \\{.*?(?=})", incrRecordsJSFunction, 0);
				String count = RegExUtils.getFirstMatch("(?is)count\\s=\\s(\\d+)", deedIf, 1);
				String limit = RegExUtils.getFirstMatch("(?is)limit\\s=\\s(\\d+)", deedIf, 1);
				functionLocalVariables.put("limit", limit);
				functionLocalVariables.put("count", count);
				
			}
			
			Map<String, String> parametersValues = new HashMap<String, String>();

			if (hiddenInput != null) {
				SimpleNodeIterator elements = hiddenInput.elements();
				while (elements.hasMoreNodes()) {
					String asHtml = elements.nextNode().toHtml();
					String paramName = RegExUtils.getFirstMatch("name=\"(.*?)(?=\")", asHtml, 1);
					String paramValue = RegExUtils.getFirstMatch("value=\"(.*?)(?=\")", asHtml, 1);

//					try {
						parametersValues.put(paramName, paramValue);
//						parametersValues.put(paramName, URLDecoder.decode(paramValue,"UTF-8"));
//					} catch (UnsupportedEncodingException e) {
//						e.printStackTrace();
//					}
				}
			}
			String key = "startPo";
			int startPo = getValueForkey(functionLocalVariables, key);

			key = "endPo";
			int endPo = getValueForkey(functionLocalVariables, key);

			key = "limit";
			int limit = getValueForkey(functionLocalVariables, key);

			key = "count";
			int count = getValueForkey(functionLocalVariables, key);

			int h_startPo = 0, h_endPo = 0;

			if (count < limit) {
				showLinks = false;
			}

			nextLink = baseLink + buildNextLink(typeOfSearch, parametersValues, endPo, limit, count);
			previousLink = baseLink + buildPrevLink(typeOfSearch, parametersValues, h_startPo, h_endPo, limit, count);
			eofLink = baseLink + buildEOFLink(typeOfSearch, parametersValues, h_startPo, h_endPo, limit, count);
			bofLink = baseLink + buildBOFLink(typeOfSearch, parametersValues, h_startPo, h_endPo, limit, count);

		}
		String[] links = null;
		if (showLinks) {
			links = new String[] { nextLink, previousLink, eofLink, bofLink };
		}
		return links;
	}

	private String buildNextLink(String typeOfSearch, Map<String, String> parametersValues, int endPo, int limit, int count) {
		int h_startPo;
		int h_endPo;
		h_startPo = endPo + 1;
		h_endPo = endPo + limit;

		if (h_startPo > count) {
			h_startPo = 1;
			h_endPo = limit;
		}
		modifyParametersValues(typeOfSearch, parametersValues, h_startPo, h_endPo);

		String nextLinkParameters = "";
		nextLinkParameters = StringUtils.addParametersToUrl(nextLinkParameters, parametersValues);
		return nextLinkParameters;
	}

	private String buildPrevLink(String typeOfSearch, Map<String, String> parametersValues, int startPo, int endPo, int limit, int count) {

		int h_startPo;
		int h_endPo;
		h_startPo = startPo - limit;
		h_endPo = endPo - limit;

		if (h_startPo < 1 || h_endPo < 1) {
			h_startPo = 1;
			h_endPo = limit;
		}

		modifyParametersValues(typeOfSearch, parametersValues, h_startPo, h_endPo);

		String nextLinkParameters = "";
		nextLinkParameters = StringUtils.addParametersToUrl(nextLinkParameters, parametersValues);
		return nextLinkParameters;
	}

	private String buildBOFLink(String typeOfSearch, Map<String, String> parametersValues, int startPo, int endPo, int limit, int count) {

		int h_startPo = 1;
		int h_endPo = limit;

		modifyParametersValues(typeOfSearch, parametersValues, h_startPo, h_endPo);

		String nextLinkParameters = "";
		nextLinkParameters = StringUtils.addParametersToUrl(nextLinkParameters, parametersValues);
		return nextLinkParameters;
	}

	private String buildEOFLink(String typeOfSearch, Map<String, String> parametersValues, int startPo, int endPo, int limit, int count) {

		int h_startPo = count - limit;
		int h_endPo = count;

		modifyParametersValues(typeOfSearch, parametersValues, h_startPo, h_endPo);

		String nextLinkParameters = "";
		nextLinkParameters = StringUtils.addParametersToUrl(nextLinkParameters, parametersValues);
		return nextLinkParameters;
	}

	private void modifyParametersValues(String typeOfSearch, Map<String, String> parametersValues, int h_startPo, int h_endPo) {
		parametersValues.put("h_rs1StartPo", "" + h_startPo);
		parametersValues.put("h_rs1EndPo", "" + h_endPo);
		parametersValues.put("h_gogo", "Search");
		parametersValues.put("h_criteria", typeOfSearch);
	}

	private int getValueForkey(Map<String, String> functionLocalVariables, String key) {
		String value;
		value = org.apache.commons.lang.StringUtils.defaultIfEmpty(functionLocalVariables.get(key), "0");
		int endPo = Integer.valueOf(value);
		return endPo;
	}

	@Override
	protected String clean(String response) {
		return super.clean(response);
	}

	@Override
	protected String cleanDetails(String response) {
		response = response.replaceAll("(?is)<a.*?</a>", "");
		response = response.replaceAll("<INPUT.*?>", "");
		String criteriaStyle = "color:White; font-family:Times New Roman,Arial,Helvetica,sans-serif;font-size:20px;"
				+ "	font-weight:bold;text-align:left;";

		response = response.replaceAll("class=\"hdrCriteria\"", "style=\"" + criteriaStyle + "\"");
		String titleStyle = "color:White;font-family:Arial,Helvetica,sans-serif;font-size:16px;" + "font-weight:bold;text-align:left;";

		response = response.replaceAll("class=\"hdrTitle\"", "style=\"" + titleStyle + "\"");
		response = response.replaceAll("(?is)<!DOCTYPE.*<FORM name=\"resultsForm\">", "");
		response = response.replaceAll("(?is)</FORM>\\s*</BODY>\\s*</HTML>", "");
		return response;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		List<String> pidEncounters = RegExUtils.getMatches("\\d{2,2}-\\d{1,1}.\\d{1,1}-\\d{2,2}-\\d{3,3}-\\d{1,3}-\\d{1,3}-\\d{1,3}",
				serverResult, 0);
		String pid = "";

		if (pid != null && pidEncounters.size() >= 1) {
			pid = pidEncounters.get(0);
		}

		return pid;
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "ASSESSOR");
		return data;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		HtmlParser3 parser3 = new HtmlParser3(response.getResult());

		Node node = HtmlParser3.getNodeByTypeAndAttribute(parser3.getNodeList(), "table", "bgcolor", "FFFFFF", true);
		NodeList tableList = node.getChildren();

		// HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(,
		// "Prior Taxes Due"),"", true)
		// .replaceAll("</?font[^>]*>", "").trim();

		ro.cst.tsearch.servers.functions.MOPlatteAO.extractDataFromResponse(map, parser3);

		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		String city = getSearch().getSa().getAtribute(SearchAttributes.P_CITY);
		String zip = getSearch().getSa().getAtribute(SearchAttributes.P_ZIP);
		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);

		// GenericAddressFilter addressHighPassFilter =
		// AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d);
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);

		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
//			pin = convertPinToDashesFormat(pin);
//			String[] pinNumbers = org.apache.commons.lang.StringUtils.split(pin, "-");
//			if (pinNumbers.length == 7) {
//				module.clearSaKeys();
//				for (int i = 0; i < pinNumbers.length; i++) {
//					module.getFunction(i).forceValue(pinNumbers[i].trim());
//				}
//			
			pin = convertPinToDashesFormat(pin);
			module.getFunction(0).forceValue(pin);
			module.getFunction(1).forceValue(pin);
			
			modules.add(module);
//		}
		
		}

		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo);
			module.getFunction(2).forceValue(streetName);
			module.addFilter(pinFilter);
			modules.add(module);
		}

		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );

		if (hasOwner()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();

			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
										.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			modules.add(module);
			
		}
		
		
		serverInfo.setModulesForAutoSearch(modules);

	}

	public static String convertPinToDashesFormat(String pin) {
		return pin.replaceAll("(\\d{2,2})(\\d{1,1})(\\d{1,1})(\\d{2,2})(\\d{3,3})(\\d{3,3})(\\d{3,3})(\\d{3,3})", "$1-$2.$3-$4-$5-$6-$7-$8");
	}
	
	
}

package ro.cst.tsearch.servers.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parentsitedescribe.ParentSiteEditorUtils;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.FirstDocumentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.PersonBean;
import ro.cst.tsearch.servers.functions.XXGenericPublicData.PersonIdentificationEnum;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * 
 * Ilie Liviu
 * 
 * @author Oprina George
 * 
 *         Aug 11, 2011
 */

public class XXGenericPublicData extends TemplatedServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6904914966305780247L;

	private int viParseID = -1;

	public XXGenericPublicData(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);

		resultType = MULTIPLE_RESULT_TYPE;

		int[] intermediary_cases = { ID_INTERMEDIARY, ID_SEARCH_BY_NAME,
				ID_SEARCH_BY_ADDRESS };
		setIntermediaryCases(intermediary_cases);

		// int[] details_cases = { TSServer.ID_DETAILS };
		// setDetailsCases(details_cases);
		//
		// int[] save_cases = { TSServer.ID_SAVE_TO_TSD };
		// setSAVE_CASES(save_cases);

		setIntermediaryMessage("Results for");
		setDetailsMessage("details_results");

	}

	@Override
	protected String cleanDetails(String response) {
		return ro.cst.tsearch.servers.functions.XXGenericPublicData
				.cleanDetails(response);
	}

	@Override
	protected String clean(String response) {
		String firstMatch = RegExUtils.getFirstMatch(
				"(?is)<table class='srch_results'>.*?</table>", response, 0);
		String script = RegExUtils.getFirstMatch("(?is)var pageidstring.*}",
				response, 0);
		firstMatch += "<input type=\"hidden\" name=\"java_script_function\">"
				+ script + "</input>";
		return firstMatch;
	}

	@Override
	public DocumentI smartParseDetails(ServerResponse response,
			String detailsHtml) {
		DocumentI parseDetailsToDocument = ro.cst.tsearch.servers.functions.XXGenericPublicData
				.parseDetailsToDocument(detailsHtml, this.searchId);
		response.getParsedResponse().setResponse(detailsHtml);
		response.getParsedResponse().setSearchId(this.searchId);
		response.getParsedResponse().setUseDocumentForSearchLogRow(true);
		if (parseDetailsToDocument != null) {
			response.getParsedResponse().setDocument(parseDetailsToDocument);
		}
		return parseDetailsToDocument;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.XXGenericPublicData.parseDetails(
				detailsHtml, searchId, map);
		return null;
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response,
			int viParseID) throws ServerResponseException {
		this.viParseID = viParseID;
		if (viParseID == ID_SAVE_TO_TSD)
			response.setResult(cleanDetails(response.getResult()));
		super.ParseResponse(action, response, viParseID);
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		HtmlParser3 parser = new HtmlParser3(table);
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		int numberOfUncheckedElements = 0;

		NodeList tableTag = HtmlParser3.getTag(parser.getNodeList(),
				new TableTag(), false);
		TableTag resultTable = (TableTag) tableTag.elementAt(0);
		if (resultTable != null) {

			TableRow[] rows = resultTable.getRows();

			int nameColumn = 0;
			int dobColumn = 0;
			int sourceColumn = 0;
			SimpleNodeIterator iterator = rows[0].getChildren().elements();
			List<Integer> indexes = new LinkedList<Integer>();
			int i = 0;
			while (iterator.hasMoreNodes()) {
				Node node = iterator.nextNode();
				if (node instanceof TableHeader) {
					indexes.add(i);
				}
				i++;
			}

			boolean vin = false;
			boolean plate = false;
			if (rows[0].getChildren().toHtml().contains("VIN"))
				vin = true;
			else if (rows[0].getChildren().toHtml().contains("Plate"))
				plate = true;

			if (indexes.size() == 4) {
				nameColumn = indexes.get(1);
				dobColumn = indexes.get(2);
				sourceColumn = indexes.get(3);
			}

			String startLink = CreatePartialLink(TSConnectionURL.idPOST);

			for (i = 1; i < rows.length
					&& "srch_results".equals(rows[i].getAttribute("class")); i++) {

				NodeList cells = rows[i].getChildren();

				if (cells.size() == 4) {
					nameColumn = 1;
					dobColumn = 2;
					sourceColumn = 3;
				}

				PersonBean personBean = new PersonBean();

				Node nameNode = cells.elementAt(nameColumn);
				String name = nameNode.toPlainTextString();
				LinkTag nameTag = HtmlParser3.getFirstTag(
						nameNode.getChildren(), LinkTag.class, true);
				if (nameTag != null) {
					String text = nameTag.getLink();
					String database = RegExUtils.getFirstMatch("db=(.*?)&",
							text, 1);
					String recordId = RegExUtils.getFirstMatch("rec=(.*?)&",
							text, 1);
					String ed = RegExUtils.getFirstMatch("ed=(.*?)&", text, 1);
					personBean.setInformationSourceKey(database);
					personBean.setRecordId(recordId);
					personBean.setEd(ed);
				}
				personBean.setName(name);

				Node dobNode = cells.elementAt(dobColumn);
				String dob_vin_plate = dobNode.toPlainTextString();

				if (vin)
					personBean.setVin(dob_vin_plate);
				else if (plate)
					personBean.setPlate(dob_vin_plate);
				else
					personBean.setDob(dob_vin_plate);

				Node infoSourceNode = cells.elementAt(sourceColumn);
				String sourceInfo = infoSourceNode.toPlainTextString();
				personBean.setInformationSource(sourceInfo);

				ParsedResponse currentResponse = new ParsedResponse();
				NodeList linkTag = HtmlParser3.getNodeListByType(
						rows[i].getChildren(), "a", true);

				String link = "";

				if (linkTag != null && linkTag.size() > 0
						&& linkTag.elementAt(0) instanceof LinkTag) {
					LinkTag detailLinkTag = (LinkTag) linkTag.elementAt(0);
					link = detailLinkTag.getLink();
					String url = startLink + "" + link;
					detailLinkTag.setLink(url);
					currentResponse.setPageLink(new LinkInPage(url, url,
							TSServer.REQUEST_SAVE_TO_TSD));
				}

				String rowHtml = rows[i].toHtml();
				if (linkTag.size() > 1) {
					Node secondLink = linkTag.elementAt(1);
					if (secondLink instanceof LinkTag) {
						String linkText = ((LinkTag) secondLink).getLinkText();
						String html = ((LinkTag) secondLink).toHtml();
						rowHtml = rowHtml.replace(html, linkText);
					}
				}

				ro.cst.tsearch.servers.functions.XXGenericPublicData
						.createDocument(searchId, currentResponse, personBean);

				String checkBox = "checked";
				if (isInstrumentSaved(personBean.getUniqueID(),
						currentResponse.getDocument(), null)
						&& !Boolean.TRUE.equals(getSearch().getAdditionalInfo(
								"RESAVE_DOCUMENT"))) {
					checkBox = "saved";
				} else {
					numberOfUncheckedElements++;
					LinkInPage linkInPage = new LinkInPage(startLink + link,
							startLink + link, TSServer.REQUEST_SAVE_TO_TSD);
					checkBox = "<input type='checkbox' name='docLink' value='"
							+ startLink + link + "'>";
					if (getSearch().getInMemoryDoc(link) == null) {
						getSearch().addInMemoryDoc(link, currentResponse);
					}
					currentResponse.setPageLink(linkInPage);
				}

				rowHtml = "<tr><td ALIGN=Left>"
						+ checkBox
						+ "</td>"
						+ rowHtml
								.replaceAll("(?ism)<tr[^>]*>", "")
								.replaceAll("(?ism)</tr>", "")
								.replaceAll("(class='srch_results')",
										"$1 width='35%'") + "</tr>";

				currentResponse.setOnlyResponse(rowHtml);

				currentResponse.setAttribute(
						ParsedResponse.SERVER_ROW_RESPONSE, rowHtml.replaceAll(
								"(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
				intermediaryResponse.add(currentResponse);
			}

			String tableHeader = rows[0].getChildren().toHtml();
			Node node = HtmlParser3.getNodeByTypeAndAttribute(
					resultTable.getChildren(), "tr", "class", "srch_pages",
					true);
			String rawFooter = node.toHtml();
			// dlnumber=LANDAT001&dlstate=CORP&id=3EFC0FFE6F81D1B160C07831FBF08353&p1=SMITH&p2=&p3=&o=GRP_SXO_NAME&input=grp_sxo_name&ed=20110628160434&type=NAME&page=3&pageid_string=p1%3D69855896%7Cp2%3D69861163%7C&searchmoreid=69866733

			List<HashMap<String, String>> matchesAsMap = RegExUtils
					.getMatchesAsMap("name='(.*)' value='(.*)'", rawFooter, 1,
							2);
			HashMap<String, String> parameters = new HashMap<String, String>();

			for (HashMap<String, String> hashMap : matchesAsMap) {
				parameters.put(hashMap.get("1"), hashMap.get("2"));
			}

			List<String> pagesLinkList = RegExUtils.getMatches(
					"document.write\\(\"(.*)\"\\)", rawFooter, 1);
			String pageParameters = RegExUtils.getFirstMatch(
					"pageidstring\\s=\\s'(.*)';", table, 1);
			List<HashMap<String, String>> pagesIds = RegExUtils
					.getMatchesAsMap("p(\\d+)=(\\d+)", pageParameters, 1, 2);
			Map<String, String> pageNumberToPageId = new HashMap<String, String>();

			for (HashMap<String, String> hashMap : pagesIds) {
				pageNumberToPageId.put(hashMap.get("1"), hashMap.get("2"));
			}
			String formAction = RegExUtils.getFirstMatch(
					"name='pagechoiceform'.*action=\\'(.*)'", rawFooter, 1);

			for (HashMap<String, String> hashMap : matchesAsMap) {
				parameters.put(hashMap.get("1"), hashMap.get("2"));
			}

			StringBuilder nextLinks = new StringBuilder("<tr><td>");
			String url = startLink + formAction;
			String linkFormat = "<a href=\"%s\">%s</a> ";
			List<String> multiplePages = RegExUtils.getMatches(
					"(?is)document.write\\('\\s*?...'\\)", table, 0);

			for (String pageLink : pagesLinkList) {
				String currentPage = RegExUtils.getFirstMatch("\\d+", pageLink,
						0);
				parameters.put("searchmoreid",
						pageNumberToPageId.get(currentPage));
				parameters.put("page", currentPage);
				String newUrl = ro.cst.tsearch.utils.StringUtils
						.addParametersToUrl(url + "?", parameters);

				if (pageLink.contains("Prev")) {
					nextLinks = nextLinks.append(String.format(linkFormat,
							newUrl, "Prev"));
					if (multiplePages.size() == 2) {
						nextLinks = nextLinks.append("...");
					}
				}
				if (pageLink.contains("curpagelink")) {
					nextLinks = nextLinks.append("" + currentPage);
				} else {
					if (!(pageLink.contains("Next"))) {
						nextLinks = nextLinks.append(String.format(linkFormat,
								newUrl, currentPage));
					}
				}
				if (pageLink.contains("Next")) {
					if (multiplePages.size() == 1) {
						nextLinks = nextLinks.append("...");
					}
					nextLinks = nextLinks.append(String.format(linkFormat,
							newUrl, "Next"));
				}
			}

			nextLinks = nextLinks.append("</td></tr>");

			if (response != null) {
				response.getParsedResponse().setHeader("<table border=\"1\">");
				response.getParsedResponse().setFooter("</table>");

				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
					tableHeader = CreateSaveToTSDFormHeader(
							URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION,
							"POST")
							+ "<TR> "
							+ "<TH ALIGN=Left>"
							+ SELECT_ALL_CHECKBOXES
							+ "</TH>"
							+ tableHeader
							+ "</TR>";

					response.getParsedResponse().setHeader(
							"<table width='75%' border=\"1\" align=\"center\">"
									+ tableHeader);

					String footer = "";

					if (numberOfUncheckedElements > 0) {
						footer = nextLinks.toString().replaceFirst("(<td)>",
								"$1 colspan=4>")
								+ "\n</table><br>"
								+ CreateSaveToTSDFormEnd(
										SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL,
										this.viParseID,
										(Integer) numberOfUncheckedElements);
					} else {
						footer = nextLinks.toString().replaceFirst("(<td)>",
								"$1 colspan=4>")
								+ "\n</table><br>"
								+ CreateSaveToTSDFormEnd(
										SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL,
										this.viParseID, -1);
					}

					response.getParsedResponse().setFooter(footer);
				}
			}
		}

		return intermediaryResponse;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		String dataSource = ro.cst.tsearch.servers.functions.XXGenericPublicData
				.getDataSource(serverResult);
		PersonIdentificationEnum dataTypeEnum = PersonIdentificationEnum
				.valueOfByDataSource(dataSource);
		String documentIDCandidate = "";
		if (dataTypeEnum != null) {
			HtmlParser3 parser3 = new HtmlParser3(serverResult);
			Map<String, String> labelToValue = dataTypeEnum
					.getLabelToValueMap(parser3);
			documentIDCandidate = dataTypeEnum.getDocumentIDCandidate(
					serverResult, labelToValue);
		}

		// PersonIdentificationEnum.getDocumentIDCandidate(serverResult
		// labelToValue);
		return documentIDCandidate;
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo,
			DocumentI documentToCheck, HashMap<String, String> data,
			boolean checkMiServerId) {
		// return super.isInstrumentSaved(instrumentNo, documentToCheck,
		// data,false);

		DocumentsManagerI documentManager = getSearch().getDocManager();
		try {
			documentManager.getAccess();
			if (documentToCheck != null) {
				if (documentManager
						.getDocument(documentToCheck.getInstrument()) != null)
					return true;
			} else {
				InstrumentI instr = new com.stewart.ats.base.document.Instrument(
						instrumentNo);
				if (data != null) {
					if (!StringUtils.isEmpty(data.get("type"))) {
						String serverDocType = data.get("type");
						String docCateg = DocumentTypes.getDocumentCategory(
								serverDocType, searchId);
						instr.setDocType(docCateg.toUpperCase());
						instr.setDocSubType(DocumentTypes
								.getDocumentSubcategory(serverDocType, searchId)
								.toUpperCase());
					}

					instr.setBook(data.get("book"));
					instr.setPage(data.get("page"));
					instr.setDocno(data.get("docno"));
				}

				try {
					instr.setYear(Integer.parseInt(data.get("year")));
				} catch (Exception e) {
				}

				if (documentManager.getDocument(instr) != null) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}

		return false;
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("norecords.jpg");
	}

	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.NAME_MODULE_IDX);

		String state = getDataSite().getStateAbbreviation();

		// just 4 texas
		if (tsServerInfoModule != null && "TX".equals(state)) {
			PageZone pageZone = (PageZone) tsServerInfoModule
					.getModuleParentSiteLayout();

			Map<String, List<Map<String, String>>> state_options = prepareDataForJSBuild(XXGenericPublicDataParentSiteConfiguration.listOfOptions_state);

			String jsFunctionForClientHtmlSelect = ParentSiteEditorUtils
					.generateJSFunctionForClientHtmlSelect("param_0_3",
							"param_0_4", "param_0_5", state_options, "", false);

			Map<String, List<Map<String, String>>> type_options = prepareDataForJSBuild(XXGenericPublicDataParentSiteConfiguration.listOfOptions_county);

			String generateJSFunctionForClientHtmlSelect = ParentSiteEditorUtils
					.generateJSFunctionForClientHtmlSelect("param_0_4",
							"param_0_5", type_options, "seconddropdownlist");

			StringBuilder customJSFunction = new StringBuilder(
					jsFunctionForClientHtmlSelect);
			customJSFunction = customJSFunction
					.append(generateJSFunctionForClientHtmlSelect);
			pageZone.setCustomJSFunction(customJSFunction);

			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc()
						.getParamName();
				if ("o".equals(functionName)) {
					// name=o_value= ,name=_value=Please select a search
					// type,name=GRP_CIV_NAME_value=Civil Court Name
					// Search,name=GRP_MW_NAME_value=Most Wanted Name
					// Search,name=GRP_SXO_NAME_value=Sex Offender Name
					// Search,name=GRP_CRI_NAME_value=Criminal Name
					// Search,name=GRP_TERROR_KB_NAME_value=Terrorism Knowledge
					// Base,name=GRP_VOTER_NAME_value=Voter Name
					// Search,name=GRP_DL_NAME_value=Driver's License Name
					// Search,name=GRP_SOS_NAME_value=Secretary of State
					// Corporation Name
					// Search,name=grp_pl_name_value=Professional License Name
					// Search - Select Data Source
					htmlControl
							.setJSFunction("javascript: dropdownlist(this.options[this.selectedIndex].value);");
				}

				if ("o2".equals(functionName)) {
					htmlControl
							.setJSFunction("javascript: seconddropdownlist(this.options[this.selectedIndex].value);");
				}
			}
		}

		tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX);

		if (tsServerInfoModule != null) {
			PageZone pageZone = (PageZone) tsServerInfoModule
					.getModuleParentSiteLayout();

			Map<String, List<Map<String, String>>> state_options = prepareDataForJSBuild(XXGenericPublicDataParentSiteConfiguration.listOfOptions_DMV);

			String jsFunctionForClientHtmlSelect = ParentSiteEditorUtils
					.generateJSFunctionForClientHtmlSelect("param_12_1",
							"param_12_2", "", state_options, "DMV_list", false);

			Map<String, List<Map<String, String>>> type_options = prepareDataForJSBuild(XXGenericPublicDataParentSiteConfiguration.listOfOptions_DMV_DPPA);

			String generateJSFunctionForClientHtmlSelect = ParentSiteEditorUtils
					.generateJSFunctionForClientHtmlSelect("param_12_2",
							"param_12_3", type_options, "DMV_DPPA_list");

			StringBuilder customJSFunction = new StringBuilder(
					jsFunctionForClientHtmlSelect);
			customJSFunction = customJSFunction
					.append(generateJSFunctionForClientHtmlSelect);
			pageZone.setCustomJSFunction(customJSFunction);

			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc()
						.getParamName();
				if ("o".equals(functionName)) {
					htmlControl
							.setJSFunction("javascript: DMV_list(this.options[this.selectedIndex].value);");
				}

				if ("input".equals(functionName)) {
					htmlControl
							.setJSFunction("javascript: DMV_DPPA_list(this.options[this.selectedIndex].value);");
				}
			}
		}

		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	/**
	 * @param listOfOptions2
	 * @return
	 */
	public Map<String, List<Map<String, String>>> prepareDataForJSBuild(
			List<String[][]> listOfOptions2) {
		Map<String, List<Map<String, String>>> options = new HashMap<String, List<Map<String, String>>>();
		for (String[][] optionArray : listOfOptions2) {
			int firstRecord = 0;
			List<Map<String, String>> list = new ArrayList<Map<String, String>>();
			for (String[] iterable_element : optionArray) {

				Map<String, String> option3 = new HashMap<String, String>();

				if (firstRecord == 0) {
					options.put(iterable_element[0], list);
					firstRecord++;
				}
				option3.put("value", iterable_element[0]);
				option3.put("label", iterable_element[1]);

				list.add(option3);
			}
		}
		return options;
	}

	@Override
	protected String addInfoToDetailsPage(ServerResponse response,
			String serverResult, int viParseID) {
		StringBuilder result = new StringBuilder(serverResult);
		Map<String, String> usefulData = new HashMap<String, String>();
		URI lastURI = response.getLastURI();
		if (lastURI != null) {
			try {
				String query = lastURI.getQuery();
				List<HashMap<String, String>> asMap = RegExUtils
						.getMatchesAsMap("(\\w+)=(.*?)&", query, 1, 2);
				for (HashMap<String, String> hashMap : asMap) {
					usefulData.put(hashMap.get("1"), hashMap.get("2"));
				}
			} catch (URIException e) {
				e.printStackTrace();
			}
			Set<Entry<String, String>> entrySet = usefulData.entrySet();
			for (Entry<String, String> entry : entrySet) {
				result.append(String.format(
						"\n<input type='hidden' name='%s'  value='%s'/>",
						entry.getKey(), entry.getValue()));
			}
		}
		saveTestCases(result.toString());
		return result.toString();
	}

	public void saveTestCases(String result) {
		/*
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			String path = "D:\\work\\PublicData\\DriversSearch\\saveFromTS\\";
			String fileName = "default_name.txt";
			fileName = org.apache.commons.lang.StringUtils
					.defaultIfEmpty(
							RegExUtils.getFirstMatch(
									ro.cst.tsearch.servers.functions.XXGenericPublicData.SOURCE_DOC_REG_EX,
									result, 1), fileName).trim();
			fileName = fileName.replaceAll("/", "_");
			File file = new File(path + fileName + ".html");
			int i = 1;
			while (file.exists()) {
				file = new File(path + fileName + "_" + i + ".html");
				i++;
			}

			ro.cst.tsearch.utils.FileUtils.appendToTextFile(
					file.getAbsolutePath(), result);
		}
		*/
		// String path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";
		// ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt",
		// text);
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> d = new HashMap<String, String>();
		d.put("type", "PUBLIC DATA");
		return d;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		if (hasOwner()) {
			FilterResponse defaultNameFilter = NameFilterFactory
					.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT,
							searchId, module);
			
			FirstDocumentFilterResponse first_Doc = new FirstDocumentFilterResponse(searchId);
			
			defaultNameFilter.setSkipUnique(false);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1,
					FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(2,
					FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			
			String val[] = {};

			if ("TX".equals(getDataSite().getStateAbbreviation()))
				val = new String[] { "GRP_CIV_NAME" // Civil Court Name Search
						, "GRP_SXO_NAME"// Sex Offender Name Search
						, "GRP_CRI_NAME"// Criminal Name Search
						, "GRP_VOTER_NAME"// Voter Name Search
						, "GRP_DL_NAME"// Driver's License Name Search
						, "grp_pl_name"// Professional License Name Search
						, "grp_cad_name"// Property Tax Name Search
				};

			for (String s : val) {
				module.forceValue(3, s);
				module.forceValue(4, s.replaceAll("(?i)NAME", "")
						+ getDataSite().getStateAbbreviation() + "_NAME"); // parameter
				// to
				// search
				// all
				// counties/document
				// subtypes
				// in0
				// this
				// state
				module.addFilter(defaultNameFilter);
				module.addFilter(first_Doc);
				module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId,
								new String[] { "L;F;M", "L;F;" }));

				modules.add(module);
			}
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}

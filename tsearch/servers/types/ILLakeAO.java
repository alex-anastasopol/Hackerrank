package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;

public class ILLakeAO extends TSServer {

	private static final long serialVersionUID = 1L;
	private static final Pattern pinPattern = Pattern.compile("(?is)Pin:.*?(?=</tr>)");

	public ILLakeAO(long searchId) {
		super(searchId);
	}

	public ILLakeAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	boolean downloadingForSave = false;

	@Override
	protected void ParseResponse(String action, ServerResponse Response, int viParseID) throws ServerResponseException {
		String response = Response.getResult();
		String contents = "";

		boolean addressSearchSuccessful = !response.contains("No records were found for the information specified");
		boolean pinSearchSuccessful = !response.contains("Requested Pin was not found");
		//if automatic doesn't provide all required fields
		boolean incompleteFormInformation = response.contains("Information is incomplete or incorrect.") && response.contains("Enter the following information for the property. Fields with \"*\" are required.");   
		
		if (!addressSearchSuccessful || !pinSearchSuccessful || incompleteFormInformation) {
			if (!pinSearchSuccessful) {
				Response
						.getParsedResponse()
						.setError(
								"<font color=\"red\">No results found.</font>  Enter the 10 or 14 digit Property Index Number (PIN) with or without dashes for the property.");
			} else if (incompleteFormInformation){
				Response.getParsedResponse().setError("<font color=\"red\">Incomplete form information. Please provide all the required fields</font>");
			} else {
				Response.getParsedResponse().setError("<font color=\"red\">No results found for the information specified.</font>");
			}
			return;
		}

		switch (viParseID) {
		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
			List<Node> nodeList = HtmlParserTidy.getNodeListByTagAndAttr(response, "table", "id", "property");

			try {
				for (Node node : nodeList) {
					contents = contents + HtmlParserTidy.getHtmlFromNode(node);

				}
				nodeList = HtmlParserTidy.getNodeListByTagAndAttr(response, "td", "class", "style9");
				contents = contents + HtmlParserTidy.getHtmlFromNode(nodeList.get(0));
			} catch (TransformerException e) {
				e.printStackTrace();
			}

			String keyCode = getPin(contents);

			contents = contents.replaceFirst("<a href.*Click.*a>", "");
			contents = contents.replaceFirst("(<td.*Click.*?</td>)", "");
			contents = contents.replaceFirst("(?is)<div class=\"style9\".*Changes.*</div>", "");

			if ((!downloadingForSave)) {
				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + keyCode + "&" + qry_aux;
				String originalLink = action + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "ASSESSOR");
				data.put("dataSource", "AO");

				if (isInstrumentSaved(keyCode, null, data)) {
					contents += CreateFileAlreadyInTSD();
				} else {
					contents = addSaveToTsdButton(contents, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, response);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));

				Response.getParsedResponse().setResponse(contents);
			} else {
				msSaveToTSDFileName = keyCode + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = contents + CreateFileAlreadyInTSD();
				smartParseDetails(Response, contents);
			}
			break;
		case ID_SEARCH_BY_ADDRESS:
			boolean multipleResults = response.contains("The following properties were found that matched the information specified:");
			if (multipleResults) {
				contents = response.replaceAll("(?is)<p>.*<p>", "");
				// Response.setResult(contents);
				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();
				try {
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, contents, outputTable);
					if (smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						// parsedResponse.setOnlyResponse(outputTable.toString());
						// parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
						// outputTable.toString());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				ParseResponse(action, Response, ID_DETAILS);
			}

			break;
		case ID_GET_LINK:
			if (action.indexOf("asmt2.asp?pin=") != -1) {
				ParseResponse(action, Response, ID_DETAILS);
			} else {
				ParseResponse(action, Response, ID_SEARCH_BY_ADDRESS);
			}
			break;
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(action, Response, ID_DETAILS);
			downloadingForSave = false;
		}
	}

	private String getPin(String contents) {
		Matcher pinMatcher = pinPattern.matcher(contents);
		String keyCode = "";
		if (pinMatcher.find()) {
			keyCode = pinMatcher.group();
			keyCode = keyCode.replaceAll("</td>", "");
			keyCode = keyCode.replaceAll("<td>", "");
			keyCode = keyCode.replaceAll("Pin:&nbsp;", "");

			keyCode = keyCode.replaceAll("-", "").trim();
		}
		return keyCode;
	}

	private String getPropertyFromTable(String propertyName, String contents) {
		String propertyPattern = "(?is)" + propertyName + ":.*?(?=</tr>)";
		Pattern pattern = Pattern.compile(propertyPattern);
		Matcher matcher = pattern.matcher(contents);
		String value = "";
		if (matcher.find()) {
			String keyCode = matcher.group();
			keyCode = keyCode.replaceAll("</td>", "");
			keyCode = keyCode.replaceAll("<td>", "");
			keyCode = keyCode.replaceAll(propertyName + ":&nbsp;", "");
			value = keyCode.trim();
		}
		return value;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);

		HtmlParser3 parser = new HtmlParser3(response.getResult());

		NodeList tableTags = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		TableTag responseTable = (TableTag) tableTags.elementAt(1);
		TableRow[] rows = responseTable.getRows();
		String newLinkURI = "spassessor/assessments/";
		// parse each row from response and create an intermediary response
		for (TableRow tableRow : rows) {
			TableColumn[] columns = tableRow.getColumns();
			LinkTag link = (LinkTag) columns[0].childAt(1);
			if (link != null) {

				String newLink = linkStart + newLinkURI + link.extractLink();
				link.setLink(newLink);
				String linkDisplayValue = link.getStringText();
				ResultMap resultMap = ro.cst.tsearch.servers.functions.ILLakeAO.parseIntermediaryLink(linkDisplayValue);

				String rowHtml = tableRow.getChildren().toHtml();
				ParsedResponse currentResponse = new ParsedResponse();
				rowHtml = "<tr>" + rowHtml + "</tr>";
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				currentResponse.setPageLink(new LinkInPage(newLink, newLink, TSServer.REQUEST_SAVE_TO_TSD));

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
		}
		String nextLink = "";

		if (table.contains("input type=\"submit\" name=\"morebutton\" value=\"More\"")) {
			FormTag formTag = (FormTag) HtmlParser3.getTag(parser.getNodeList(), new FormTag(), true).elementAt(0);
			nextLink = buildNextLink(formTag);
			nextLink = CreatePartialLink(TSConnectionURL.idPOST) + newLinkURI + nextLink;
		}

		response.getParsedResponse().setHeader("<table width=\"90%\" >");
		if (!StringUtils.isEmpty(nextLink)) {
			nextLink = // "<div align=\"right\" >" +
			"<a href=\"" + nextLink + "\"" + "><font size=\"6px\">More</font></a>";
			// "</div>";
		}
		response.getParsedResponse().setFooter("</table><br><br>" + nextLink);
		outputTable.append(responseTable.toHtml());

		return intermediaryResponse;
	}

	private String buildNextLink(FormTag formTag) {
		SimpleNodeIterator elements = formTag.getFormInputs().elements();
		StringBuilder link = new StringBuilder();

		while (elements.hasMoreNodes()) {
			InputTag input = (InputTag) elements.nextNode();
			String nameAttribute = input.getAttribute("name");
			String valueAttribute = input.getAttribute("value");
			link.append(nameAttribute + "=" + valueAttribute);
			link.append("&");
		}
		link.insert(0, formTag.getAttribute("actionB") + "?");
		return link.toString();
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String contents, ResultMap map) {

		String pin = getPropertyFromTable("Pin", contents);
		pin = pin.replaceAll("-", "");
		map.put("PropertyIdentificationSet.ParcelID", pin);

		String address = getPropertyFromTable("Street Address", contents);

		map.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
		map.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));

		String city = getPropertyFromTable("City", contents);
		map.put("PropertyIdentificationSet.City", city);

		String zipCode = getPropertyFromTable("Zip Code", contents);
		map.put("PropertyIdentificationSet.Zip", zipCode);

		String totalAmount = getPropertyFromTable("Total Amount", contents).replaceAll("\\$", "").replaceAll(",", "");
		map.put("PropertyAppraisalSet.TotalAssessment", totalAmount);

		String landAmount = getPropertyFromTable("Land Amount", contents).replaceAll("\\$", "").replaceAll(",", "");
		map.put("PropertyAppraisalSet.TotalAssessment", landAmount);

		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetDirection = getSearch().getSa().getAtribute(SearchAttributes.P_STREETDIRECTION);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		String streetSuffix = getSearch().getSa().getAtribute(SearchAttributes.P_STREETSUFIX);
		String zip = getSearch().getSa().getAtribute(SearchAttributes.P_ZIP);

		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);

		FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pid: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(0).forceValue(pid);
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
		
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);

			modules.add(module);
		}

		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo);
			module.getFunction(1).forceValue(streetDirection);
			module.getFunction(2).forceValue(streetName);
			module.getFunction(3).forceValue(streetSuffix);
			module.getFunction(4).forceValue(zip);

			module.addFilter(multiplePINFilter);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
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
}

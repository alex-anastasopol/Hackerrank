package ro.cst.tsearch.servers.types;



import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.StateBarDocumentI;

/**
 * @author mihaib
*/

public class TXStateBarServerLW extends TSServer{
	
	private boolean downloadingForSave;
	private static final long serialVersionUID = 1L;
	
	private static final Pattern PAGE_PATTERN = Pattern.compile("(?is)href=\\\"javascript:PageClick\\(\\s*([^,]*)\\s*,\\s*([^\\)]*\\s*)\\)[^>]+>");
	private static final Pattern PREV_NEXT_PATTERN = Pattern.compile("(?is)href=\\\"javascript:ButtonClick\\(\\s*'\\s*([^']*)\\s*'[^>]+>");
	
	private static final Pattern BCN_PATTERN = Pattern.compile("(?is)<span[^>]*>\\s*Bar\\s+Card\\s+Number:\\s*</span>\\s*<span[^>]*>([^<]*)</span>");
	private static final Pattern LICENSE_PATTERN = Pattern.compile("(?is)<span[^>]*>\\s*Texas\\s+License\\s+Date:\\s*</span>\\s*<span[^>]*>([^<]*)</span>");
	private static final Pattern FIRM_PATTERN = Pattern.compile("(?is)<span[^>]*>\\s*Firm:\\s*</span>\\s*<span[^>]*>([^<]*)</span>");
	private static final Pattern STATUS_PATTERN = Pattern.compile("(?is)<[^>]*>\\s*Current Member Status\\s*</[^>]*>\\s*<[^>]*>([^<]*)</[^>]*>");
	
	public TXStateBarServerLW(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public TXStateBarServerLW(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
		
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;
		
		if (rsResponse.indexOf("Your search has returned no result") != -1){
			Response.getParsedResponse().setError("Your search has returned no result.");
			return;
		} 
	
		switch (viParseID) {
			case ID_SEARCH_BY_NAME :
				
			try {

				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();

				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
						Response, rsResponse, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
			
			case ID_DETAILS :
				
				String details = getDetails(rsResponse, Response);
				
				String docNo = StringUtils.extractParameterFromUrl(Response.getRawQuerry(), "ContactID");

				if ((!downloadingForSave)){	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + docNo + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					originalLink = originalLink.replaceAll("(?is)&$", "");
					try {
						originalLink = URLDecoder.decode(originalLink, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type", "STATEBAR");
	    				
					if (isInstrumentSaved(docNo, null, data)){
	                	details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					Response.getParsedResponse().setPageLink(
							new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else {      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = docNo + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	               
				}
				break;
				
			case ID_GET_LINK :
				if (Response.getQuerry().indexOf("ContactID") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (Response.getQuerry().indexOf("ButtonName") != -1) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
				
				ParseResponse(sAction, Response, ID_DETAILS);
				
				downloadingForSave = false;
				break;

			
	
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String html, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(html, null);
			
			NodeList nodeList = htmlParser.parse(null);
			NodeList divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "result-set"))
									.extractAllNodesThatMatch(new HasAttributeFilter("class", "profile-wrapper"), true);
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>")
					.append("<tr><th width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "Save All</th>" +
									"<th>Information</th></tr>");
			
			if (divList != null && divList.size() > 0){
				for (int i = 0; i < divList.size(); i++) {
					Div divRow = (Div) divList.elementAt(i);
					divRow.getChildren().keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "search-result-links")));
					
					StringBuffer link = new StringBuffer(CreatePartialLink(TSConnectionURL.idGET));
					String nameFromLink = "", contactID = "";
					
					NodeList aList = divRow.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if (aList != null && aList.size() > 0) {
						for (int j = 0; j < aList.size(); j++) {
							String lnk = ((LinkTag) aList.elementAt(j)).getLink();
							if (lnk.contains("ContactID")){
								link.append(lnk);
								nameFromLink = ((LinkTag) aList.elementAt(j)).getLinkText().trim();
								contactID = StringUtils.extractParameterFromUrl(lnk, "ContactID");
								break;
							}
						}
					}
						
					ParsedResponse currentResponse = new ParsedResponse();

					RegisterDocumentI document = (RegisterDocumentI) currentResponse.getDocument();
					
					ResultMap resultMap = new ResultMap();
					
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "LW");
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "STATEBAR");
					if (StringUtils.isNotEmpty(nameFromLink)){
						nameFromLink = nameFromLink.replaceAll("\\s+", " ");
						resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), nameFromLink);
					}
					String status = "", firm = "";
					NodeList list = divRow.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "status wai"), true);
					if (list != null && list.size() > 0){
						status = list.elementAt(0).toPlainTextString();
					}
					
					list = divRow.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "org"), true);
					if (list != null && list.size() > 0){
						firm = list.elementAt(0).toPlainTextString();
					}
					
					Bridge bridge = new Bridge(currentResponse, resultMap, getSearch().getID());
					document = (StateBarDocumentI) bridge.importData();
					if (StringUtils.isNotEmpty(status)){
						((StateBarDocumentI)document).setStatus(status);
					}
					if (StringUtils.isNotEmpty(firm)){
						((StateBarDocumentI)document).setFirm(firm);
					}
					
					currentResponse.setDocument(document);
					String checkBox = "checked";
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", "STATEBAR");
					
					if (isInstrumentSaved(contactID, null, data)
							&& !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(link.toString(), link.toString(), TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link.toString() + "\">";
						currentResponse.setPageLink(linkInPage);
						
					}
					
					String row = "<tr border='1'><td>" + checkBox + "</td><td>" + divRow.toHtml() + "</td></tr>";
					row = row.replaceFirst("(?is)(href=\\\")", "$1" + CreatePartialLink(TSConnectionURL.idGET));
					
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>"
							+ row + "</table>");
					
					currentResponse.setOnlyResponse(row);
					newTable.append(currentResponse.getResponse());

					intermediaryResponse.add(currentResponse);
				}
			}
			String navRow = "";
			NodeList paginationDivList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
										.extractAllNodesThatMatch(new HasAttributeFilter("class", "result-pagination"));
			if (paginationDivList != null && paginationDivList.size() > 0){
				NodeList formList = paginationDivList.extractAllNodesThatMatch(new HasAttributeFilter("name", "HiddenFormFields"), true);
				
				if (formList != null && formList.size() > 0){
					FormTag form = (FormTag) formList.elementAt(0);
					if (form != null){
						String action = form.getAttribute("action");
						String link = CreatePartialLink(TSConnectionURL.idPOST) + action;
						
						Map<String,String> paramsForNav = new HashMap<String, String>();
						NodeList inputs = form.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
						for (int j = 0; j < inputs.size(); j++){
							InputTag input = (InputTag) inputs.elementAt(j);
							if ("hidden".equals(input.getAttribute("type"))){
								if (input.getAttribute("name") != null){
									if (input.getAttribute("value") != null){
										paramsForNav.put(input.getAttribute("name"), input.getAttribute("value"));
									}
								}
							}
						}
						paramsForNav.put("Start", "");
						if (!paramsForNav.isEmpty()){
							mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsNav:", paramsForNav);
						}
						
						NodeList prevLiList = paginationDivList.extractAllNodesThatMatch(new TagNameFilter("li"), true)
												.extractAllNodesThatMatch(new HasAttributeFilter("class", "prev"), true);
						if (prevLiList != null){
							navRow = prevLiList.toHtml();
						}
						
						NodeList spanList = paginationDivList.extractAllNodesThatMatch(new TagNameFilter("span"), true);
						if (spanList != null){
							navRow += spanList.toHtml();
						}
						
						NodeList nextLiList = paginationDivList.extractAllNodesThatMatch(new TagNameFilter("li"), true)
												.extractAllNodesThatMatch(new HasAttributeFilter("class", "next"), true);
						if (nextLiList != null){
							navRow += nextLiList.toHtml();
						}
						
						/* function from the site
						 * function PageClick(pageNbr, maxRows){
								document.HiddenFormFields.ButtonName.value = 'Page';
								document.HiddenFormFields.Page.value = ( ( (pageNbr - 1) * maxRows ) + 1).toString()  ;
    						}*/
						
						if (StringUtils.isNotEmpty(navRow)){
							Matcher mat = PAGE_PATTERN.matcher(navRow);
							while (mat.find()){
								int pageNbr = Integer.parseInt(mat.group(1));
								int maxRows = Integer.parseInt(mat.group(2));
								String pageValue = Integer.toString(((pageNbr - 1) * maxRows) + 1);
								navRow = navRow.replaceFirst(PAGE_PATTERN.toString(), 
														"href=\"" + link + "&ButtonName=Page&Page=" + pageValue + "\">");
								paramsForNav.remove("Page");
							}
							mat = PREV_NEXT_PATTERN.matcher(navRow);
							while (mat.find()){
								navRow = navRow.replaceFirst(PREV_NEXT_PATTERN.toString(), 
														"href=\"" + link + "&ButtonName=" + mat.group(1) + "\">");
							}
							
							navRow = navRow.replaceAll("(?is)</?li[^>]*>", "");
						}
					}
				}
			}
			
			
			String header1 = "<tr><th width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "Save All</th>" +
									"<th>Information</th></tr>";

			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
					+ "<br>" + navRow + "<br>"
					+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
			
			response.getParsedResponse().setFooter("</table>" + 
							"<br>" + navRow + "<br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
			
			newTable.append("</table>");
			outputTable.append(newTable);
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String details = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);

			if (mainList != null && mainList.size() > 0){
				NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "public-profile"));
				if (divList != null && divList.size() > 0){

					NodeList list = divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "contact-info"), true);
					if (list != null && list.size() > 0){
						list.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "member-contact-links")), true);
						list.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "member-badges")), true);
						list.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "no-img")), true);
						details += list.toHtml();
					}
					
					list = divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "section-overview"), true);
					if (list != null && list.size() > 0){
						details += list.toHtml();
					}
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		details = details.replaceAll("(?is)<img[^>]*>", "");
		details = details.replaceAll("(?is)<a[^>]*>[^<]*</a>", "");
		details = details.replaceAll("(?is)\\|", "");
		
		return details;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "LW");
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
			
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), 
							ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(response.getQuerry(), "ContactID"));
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			NodeList spanList = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true);
										
			Matcher mat = LICENSE_PATTERN.matcher(detailsHtml);
			if (mat.find()){
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), mat.group(1));
			}
			mat.reset();
			
			mat = BCN_PATTERN.matcher(detailsHtml);
			if (mat.find()){
				resultMap.put("tmpBcn", mat.group(1));
			}
			
			mat = STATUS_PATTERN.matcher(detailsHtml);
			if (mat.find()){
				resultMap.put("tmpStatus", mat.group(1));
			}
			
			mat = FIRM_PATTERN.matcher(detailsHtml);
			if (mat.find()){
				resultMap.put("tmpFirm", mat.group(1));
			}
			
			String name = "";
			if (spanList != null && spanList.size() > 0) {
				
				NodeList sList = spanList.extractAllNodesThatMatch(new HasAttributeFilter("class", "family-name"));
				if (sList != null && sList.size() > 0){
					name += sList.elementAt(0).toPlainTextString();
				}
				sList = spanList.extractAllNodesThatMatch(new HasAttributeFilter("class", "given-name"));
				if (sList != null && sList.size() > 0){
					name += " " + sList.elementAt(0).toPlainTextString();
				}
				
				sList = spanList.extractAllNodesThatMatch(new HasAttributeFilter("class", "additional-name"));
				if (sList != null && sList.size() > 0){
					name += " " + sList.elementAt(0).toPlainTextString();
				}
			}
			if (StringUtils.isNotEmpty(name)){
				name = name.replaceAll("(?is)</?h1[^>]*>", "").trim();
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			}
			
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "STATEBAR");
			resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), "STATEBAR");
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "State Bar of Texas");
			
			parseNames(resultMap, searchId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		StringBuilder justResponse = new StringBuilder(detailsHtml);
		try {
			ResultMap map = new ResultMap();
							
			parseAndFillResultMap(response, detailsHtml, map);
			
			String bcn = (String) map.get("tmpBcn");
			String status = (String) map.get("tmpStatus");
			String firm = (String) map.get("tmpFirm");
			
			map.removeTempDef();
			
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			
			document = bridge.importData();
			((StateBarDocumentI)document).setBarCardNumber(bcn);
			((StateBarDocumentI)document).setStatus(status);
			((StateBarDocumentI)document).setFirm(firm);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(fillServerResponse) {
			response.getParsedResponse().setOnlyResponse(justResponse.toString());
			if(document!=null) {
				response.getParsedResponse().setDocument(document);
			}
		}
		
		return document;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m, long searchId) throws Exception{
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		ArrayList<List> grantor = new ArrayList<List>();
		
		String tmpPartyGtor = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			tmpPartyGtor = tmpPartyGtor.replaceAll("\\sDBA\\s+", " / ");
			
			names = StringFormats.parseNameNashville(tmpPartyGtor, true);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
				
			GenericFunctions.addOwnerNames(tmpPartyGtor, names, suffixes[0],
						suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), grantor);
			}
			
			m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
	}

}
		

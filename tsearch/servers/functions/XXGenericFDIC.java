package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Tidy;

public class XXGenericFDIC extends ParseClassWithExceptionTreatment {

	private XXGenericFDIC(ParseInterface parser) {
		super(parser);
	}

	private static XXGenericFDIC _instance = null;

	private static MessageFormat linkFormat;

	public static MessageFormat getLinkFormat() {
		return linkFormat;
	}

	public static void setLinkFormat(MessageFormat linkFormat) {
		XXGenericFDIC.linkFormat = linkFormat;
	}

	private XXGenericFDIC() {
		super(_instance);
	}

	public static XXGenericFDIC getInstance() {
		if (_instance == null) {
			_instance = new XXGenericFDIC();
		}
		return _instance;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		HtmlParser3 parser = new HtmlParser3(response);
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "FD");
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
		resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);

		//boolean exactMatch = true;
		NodeList nodeList = HtmlParser3.getNodeListByType(parser.getNodeList(), "Table", true);
		if (nodeList != null) {
			if (nodeList.size() > 1 && !isTheOtherDetail(response)) {
				Node elementAt = nodeList.elementAt(1);
				HtmlParser3.getNodeByAttribute(elementAt.getChildren(), "HEADER", "column1a", true).toPlainTextString();

				String id = HtmlParser3.getNodeByAttribute(elementAt.getChildren(), "HEADER", "column1a", true).toPlainTextString();
				String name = HtmlParser3.getNodeByAttribute(elementAt.getChildren(), "HEADER", "column2a", true).toPlainTextString();
				String city = HtmlParser3.getNodeByAttribute(elementAt.getChildren(), "HEADER", "column3a", true).toPlainTextString();
				String state = HtmlParser3.getNodeByAttribute(elementAt.getChildren(), "HEADER", "column4a", true).toPlainTextString();

				HtmlParser3.getNodeValue(parser, "BHC ID", 1, 0);

				resultMap.put(SaleDataSet.SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), id);
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
				setName(resultMap, name);
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(), state);
			} else {
				Node elementAt = nodeList.elementAt(0);
				String id = org.apache.commons.lang.StringUtils.defaultIfEmpty(
						RegExUtils.getFirstMatch("Total Offices for(.*?):", elementAt.toHtml(), 1), "").trim();
				resultMap.put(SaleDataSet.SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), id);
				String name = RegExUtils.getFirstMatch("(?is)<font color=\"#990033\".*?<b>(.*?)</b>", elementAt.toHtml(), 1);
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
				setName(resultMap, name);

				List<String> address = RegExUtils.getFirstMatch(
						"(?is)<br>\\s*<FONT face=\"Arial, Helvetica\" size=\"2\">(.*?)<br>(.*)</p>", elementAt.toHtml(), 1, 2);
				if (address.size() == 2) {
					parseAddress(address.get(0), resultMap);

					Pattern pattern = Pattern.compile("(?is)(.*?),(.*?),(.*?)");
					Matcher matcher = pattern.matcher(address.get(1));

					if (matcher.find()) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), matcher.group(1).trim());
						resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(), matcher.group(2).trim());
						resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matcher.group(3).trim());
					}
				}
			}
		}
	}

	@Override
	public void parseAddress(String address, ResultMap resultMap) {

		String streetName = StringFormats.StreetName(address);
		String streetNo = StringFormats.StreetNo(address);
		resultMap.put("PropertyIdentificationSet.StreetName", streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);

	}

	/**
	 * It appears the other details are the ones found at institution/offices search<br>
	 * The mail details are considered those received at "Bank Holding Cos." search
	 * @param response
	 * @return
	 */
	public static boolean isTheOtherDetail(String response) {
		return response.contains("Offices and Branches of");
	}

	public Vector<ParsedResponse> parseIntermediary1(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(response.replaceAll("&nbsp;", " "), null)).getNodeList();
			
			NodeList auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("action","findOffices.asp"))
					.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width","100%"))
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing","0"))
					.extractAllNodesThatMatch(new HasAttributeFilter("border","0"));

			String prevNextLine = "";
			
			if (auxNodes.size() > 0) {
				String startLink = createPartialLink(format, TSConnectionURL.idGET);
				
				TableTag t = (TableTag) auxNodes.elementAt(0);

				TableRow[] rows = t.getRows();

				NodeList auxRows = new NodeList();

				boolean flag = false;

				for (int i = 0; i < rows.length; i++) {
					if(rows[i].toHtml().contains("Previous") && rows[i].toHtml().contains("Next") && StringUtils.isEmpty(prevNextLine)){
						prevNextLine = rows[i].toHtml();
						continue;
					}
					
					if (rows[i].toHtml().contains("rpt_offices.asp") || (flag && i == rows.length - 1)) {
						flag = true;

						if (auxRows.size() > 0) {
							// process gathered rows

							// get instr no
							String parcelId = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxRows, "Cert:"), "", false).trim();

							if (StringUtils.isNotEmpty(parcelId)) {
								
								ResultMap resultMap = new ResultMap();
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), parcelId.replaceAll(".*Cert: (\\d+).*","$1"));

								String name = parcelId.split("Cert:")[0];
								resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
								setName(resultMap, name);

//								String county = "";
//								resultMap.put(PropertyIdentificationSetKey.COUNTY.getKeyName(), county);
//
//								String state = "";
//								resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(), state);

								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "FD");
								resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
								resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);

								ParsedResponse currentResponse = new ParsedResponse();

								LinkTag detailLinkTag = null;

								Node n = HtmlParser3.findNode(auxRows, "Cert:");
								if(n!=null && n.getParent() instanceof TableColumn){
									auxNodes = n.getParent().getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
									
									if(auxNodes.size() == 2){
										detailLinkTag = (LinkTag) auxNodes.elementAt(1);
									}
								}
								
								if (detailLinkTag != null) {
									String link = detailLinkTag.getLink();
									String url = startLink + "/idasp/" + link;
									detailLinkTag.setLink(url);
									currentResponse.setPageLink(new LinkInPage(url, url, TSServer.REQUEST_SAVE_TO_TSD));
								}

								String rowHtml = "<tr><td><table>"+auxRows.toHtml().replaceAll("(?ism)<a[^>]*confirmation.asp[^>]*>([^<]*)</a>","$1")
										.replaceAll("(?ism)<a[^>]*definitions.asp[^>]*>(.*?)</a>","$1")+"</table></td></tr>";

								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								createDocument(searchId, currentResponse, resultMap);
								intermediaryResponse.add(currentResponse);
							}

							auxRows = new NodeList();
						}
					}

					if (flag && !rows[i].toHtml().contains("Previous")) {
						auxRows.add(rows[i]);
					}
				}
			}
			
			if (serverResponse != null) {
				String tableFooter = createNextLinks(serverResponse, format, "", prevNextLine.replaceAll("(?ism)</?a[^>]*>", ""));
				serverResponse.getParsedResponse().setHeader("<table id=intermediaryRes width=95% border=1>");
				serverResponse.getParsedResponse().setFooter("</table>" + tableFooter);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		if(response.contains("Selection Results")){
			return parseIntermediary1(serverResponse, response, searchId, format);
		}
		
		HtmlParser3 parser = new HtmlParser3(Tidy.tidyParse(response, null));
		
		TableColumn noOfResultsTd = (TableColumn) parser.getNodeList()
				.extractAllNodesThatMatch(new RegexFilter("(?is)\\s*showing\\s+record"), true)
				.elementAt(0)
				.getParent();
		String noOfResults = "";
		if (noOfResultsTd != null)
		{
			noOfResults = "<table id=\"numberOfResults\"><tr><td>" + noOfResultsTd.toHtml().replaceAll("(?is)</?font[^>]*>", "") + "</td></tr></table>";
		}
		NodeList width990Tables = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"));
		
		NodeList multipleResultsTables = width990Tables.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "02"));

		if(width990Tables == null || width990Tables.size() == 0){
			width990Tables = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "990"));
			multipleResultsTables = width990Tables.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"));
			if(multipleResultsTables.size() > 2){
				Node n = multipleResultsTables.elementAt(2);
				multipleResultsTables.removeAll();
				multipleResultsTables.add(n);
			}
		}
		
		String startLink = createPartialLink(format, TSConnectionURL.idGET);
		TableTag resultTable = null;
		if (multipleResultsTables.size() >= 1) {

			resultTable = (TableTag) multipleResultsTables.elementAt(0);
			
			TableRow[] rows = resultTable.getRows();

			int instrNumber = 0;
			int nameColumn = 1;
			int cityCol = 2;
			int stateAbrev = 3;
			
			boolean isInstitutionModule = false;
			
			for (int i = 1; i < rows.length; i++) {
				if(rows[i].getColumnCount() == 0 || rows[i].toPlainTextString().contains("Institution Name")){
					continue;
				}
				
				TableColumn[] cols = rows[i].getColumns();

				Node parcelColumnNode = cols[instrNumber];
				String parcelId = "";

				if (parcelColumnNode != null) {
					parcelId = StringUtils.defaultIfEmpty(parcelColumnNode.toPlainTextString(), "").trim();
				}

				if (StringUtils.isNotEmpty(parcelId)) {
					ResultMap resultMap = new ResultMap();
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), parcelId);

					Node nameNode = cols[nameColumn];
					String name = nameNode.toPlainTextString();
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
					setName(resultMap, name);

					String city = cols[cityCol].toPlainTextString();
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);

					String state = cols[stateAbrev].toPlainTextString();
					resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(), state);

					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "FD");
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
					resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);

					ParsedResponse currentResponse = new ParsedResponse();
					
					LinkTag detailLinkTag = null;
					
					if ((cols.length == 7 || cols.length == 8) && cols[instrNumber].getChild(1) instanceof LinkTag) {
						detailLinkTag = (LinkTag) cols[instrNumber].getChild(1);
						isInstitutionModule = true;
					} else if(cols.length == 7 && cols[nameColumn].getChild(0) instanceof LinkTag){
						detailLinkTag = (LinkTag) cols[nameColumn].getChild(0);
					}
					
					if (cols.length == 5 && cols[instrNumber].getChild(0) instanceof LinkTag) {
						detailLinkTag = (LinkTag) cols[instrNumber].getChild(0);
					}

					if (detailLinkTag != null) {
						String link = detailLinkTag.getLink();
						if (isInstitutionModule) {
							link = link.replaceAll("(?is)confirmation.asp\\?inCert1=(.*?)&.*", "rpt_Offices.asp?cert=$1");
						}
						String url = startLink + "/idasp/" + link;
						detailLinkTag.setLink(url);
						currentResponse.setPageLink(new LinkInPage(url, url, TSServer.REQUEST_SAVE_TO_TSD));
					}
					
					String rowHtml = rows[i].toHtml().replaceAll("<a[^>]*Call TFR Report[^>]*>(.*?)</a>", "$1")
							.replaceAll("<a[^>]*View Preliminary FFIEC Call TFR Report[^>]*>(.*?)</a>", "$1");
					
					if(isInstitutionModule){
						StringBuffer sb = new StringBuffer();
						Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]*)</a>").matcher(rowHtml);
						while (ma.find()) {
							if (ma.group(1).startsWith(startLink)) {
								ma.appendReplacement(sb, ma.group(0));
							} else {
								ma.appendReplacement(sb, ma.group(2));
							}
						}
						ma.appendTail(sb);
						rowHtml = sb.toString();
					} else {
						rowHtml = rowHtml.replaceAll("<a[^>]*View a combined Summary Financial Report[^>]*>(.*?)</a>", "$1");
					}
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					createDocument(searchId, currentResponse, resultMap);
					intermediaryResponse.add(currentResponse);
				}
			}

			String tableHeader = rows[0].getChildren().toHtml();
			
			if(!tableHeader.contains("<th")){
				tableHeader = rows[1].getChildren().toHtml();
			}
			
			if (serverResponse != null) {
				String tableFooter = createNextLinks(serverResponse, format, "", tableHeader.replaceAll("(?ism)</?a[^>]*>", ""));
				serverResponse.getParsedResponse().setHeader(
						noOfResults + "<table id=intermediaryRes width=95%>" + tableHeader.replaceAll("(?ism)</?a[^>]*>", ""));
				serverResponse.getParsedResponse().setFooter("</table>" + tableFooter);
			}
		} 
		
		return intermediaryResponse;
	}

	private String createNextLinks(ServerResponse serverResponse, MessageFormat format, String formAction, String htmlPart) {
		String startLinkPost = createPartialLink(format, TSConnectionURL.idPOST);
		String startLinkGet = createPartialLink(format, TSConnectionURL.idGET);

		boolean isOffice = false;
		
		if(serverResponse.getResult().contains("Selection Results") && serverResponse.getResult().contains("Total Offices found within criteria:")){
			isOffice = true;
		}
		
		NodeList nodes = new HtmlParser3(Tidy.tidyParse(serverResponse.getResult(), null)).getNodeList();

		NodeList auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("action", "BHClist.asp"));

		if (auxNodes.size() == 0) {
			auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("action", "findOffices.asp"));
		}
		
		if (auxNodes.size() == 0) {
			auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("action", "FindAnInstitution.asp"));
		}
			
		if (auxNodes.size() > 0) {
			FormTag f = (FormTag) auxNodes.elementAt(0);

			String currentPage = "";

			auxNodes = nodes.extractAllNodesThatMatch(new HasAttributeFilter("name", "PageNumber1"),true);

			boolean isTest = false;
			
			if (auxNodes.size() == 0) {
				auxNodes = nodes.extractAllNodesThatMatch(new HasAttributeFilter("name", "test"),true);
				isTest = true;
			}
			
			if(auxNodes.size() > 0){
				auxNodes = auxNodes.elementAt(0).getChildren().extractAllNodesThatMatch(new HasAttributeFilter("selected"));

				if (auxNodes.size() > 0) {
					currentPage = auxNodes.elementAt(0).toPlainTextString();
				}
			}

			auxNodes = f.getFormInputs();

			HashMap<String, String> params = new HashMap<String, String>();

			for (int i = 0; i < auxNodes.size(); i++) {
				InputTag in = (InputTag) auxNodes.elementAt(i);
				params.put(StringUtils.defaultString(in.getAttribute("name")), StringUtils.defaultString(in.getAttribute("value")));
			}

			StringBuffer paramsBuffer = new StringBuffer();

			for (Entry<String, String> e : params.entrySet()) {
				if (!e.getKey().equals("PageNumber") && !e.getKey().equals("pageNum"))
					paramsBuffer.append(e.getKey() + "=" + e.getValue() + "&");
			}

			String baseLink = startLinkPost + "/idasp/"+f.getAttribute("action")+"?" + paramsBuffer.toString().replaceAll("&$", "");
			baseLink = baseLink.replaceAll("\\s", "%20");
			
			String baseLink1 = startLinkGet + "/idasp/"+f.getAttribute("action")+"?" + paramsBuffer.toString().replaceAll("&$", "");
			baseLink1 = baseLink1.replaceAll("\\s", "%20");

			if (StringUtils.isNotEmpty(currentPage)) {

				boolean hasPrev = false;
				boolean hasNext = false;

				int cPage = Integer.parseInt(currentPage);

				if (serverResponse.getResult().contains("extPage(" + (cPage - 1) + ")")) {
					hasPrev = true;
				}

				if (serverResponse.getResult().contains("extPage(" + (cPage + 1) + ")")) {
					hasNext = true;
				}

				String res = "<table id=links><tr>";

				if (hasPrev) {
					if (!isOffice) {
						res += "<td><a href=" + baseLink + (isTest ? "&pageNum=" : "&PageNumber=") + (cPage - 1) + ">Prev</td>";
					} else {
						res += "<td><a href=" + baseLink1 + "&PageNumber1=" + cPage + "&PageNumber2=" + cPage + "&pageNum=" + (cPage - 1) + ">Prev</td>";
					}
				}

				if (hasNext) {
					if (!isOffice) {
						res += "<td><a href=" + baseLink + (isTest ? "&pageNum=" : "&PageNumber=") + (cPage + 1) + ">Next</td>";
					} else {
						res += "<td><a href=" + baseLink1 + "&PageNumber1=" + cPage + "&PageNumber2=" + cPage + "&pageNum=" + (cPage + 1) + ">Next</td>";
					}
				}

				res += "</tr></table>";

				return res;

			}
		}

		return "";
	}

	/**
	 * @param navigationForm
	 * @param rawLink
	 * @param hrefRegEx
	 * @param javascriptCall
	 * @return
	 */
	public String constructTheNextLinks(String navigationForm, String rawLink, String hrefRegEx, String javascriptCall) {
		while (RegExUtils.matches(hrefRegEx, navigationForm)) {
			String pageNumber = RegExUtils.getFirstMatch(hrefRegEx, navigationForm, 2);
			// navigationForm = navigationForm.replaceAll(hrefRegEx, rawLink);
			String pageNumberKey = "PageNumber=";
			String pageNumber1Key = "PageNumber1=";
			String pageNumKey = "pageNum=";
			String link = "";
			if (rawLink.contains(pageNumberKey)) {
				link = rawLink.replace(pageNumberKey, pageNumberKey + pageNumber);
			} else {
				link = rawLink.replace(pageNumKey + "0", pageNumKey + pageNumber);
				if (!link.contains(pageNumKey)) {
					int i = 0;
					if (StringUtils.isNumeric(pageNumber)) {
						i = Integer.valueOf(pageNumber) + 1;
					}
					link += pageNumKey + pageNumber + "&" + pageNumber1Key + i;
				}
			}
			navigationForm = navigationForm.replace(String.format(javascriptCall, pageNumber), link);
		}
		return navigationForm;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setName(ResultMap resultMap, String name) {
		ArrayList namesBody = new ArrayList<String>();
		name = GenericFunctions2.cleanOwnerNameFromPrefix(name);
		String[] names = { "", "", name, "", "", "" };
		String[] suffixes = { "", "" };

		ParseNameUtil.putNamesInResultMap(resultMap, namesBody, names, suffixes);

		if (StringUtils.isNotEmpty(name)) {
			GenericFunctions.addOwnerNames(name, names, namesBody);
		}
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
		try {
			resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(namesBody));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String[] description,
			boolean recursive) {
		NodeList returnList = null;

		if (!StringUtils.isEmpty(attributeName)) {
			if (StringUtils.isNotEmpty(type)) {
				returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
						new HasAttributeFilter(attributeName, attributeValue), recursive);
			} else {
				returnList = nl.extractAllNodesThatMatch(new HasAttributeFilter(attributeName, attributeValue), recursive);
			}
		} else {
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);
		}

		for (int i = returnList.size() - 1; i >= 0; i--) {
			boolean flag = true;
			for (String s : description) {
				if (!StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s))
					flag = false;
			}
			if (flag)
				return returnList.elementAt(i);
		}

		return null;
	}
}

package ro.cst.tsearch.servers.types;

import java.net.Authenticator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.WebAuth;

public class KSJohnsonAO extends TSServer {

	static final long	serialVersionUID	= 10000000;
	private boolean		downloadingForSave;
	static final HashMap<String,String> citiesList =  new HashMap<String, String>();
	static {
		citiesList.put("BONNER SPRINGS", "2");
		citiesList.put("BUCYRUS", "33");
		citiesList.put("DESOTO", "3");
		citiesList.put("EDGERTON", "4");
		citiesList.put("EUDORA", "31");
		citiesList.put("FAIRWAY", "5");
		citiesList.put("GARDNER", "6");
		citiesList.put("LAKE QUIVIRA", "8");
		citiesList.put("LEAWOOD", "9");
		citiesList.put("LENEXA", "10");
		citiesList.put("MERRIAM", "13");
		citiesList.put("MISSION", "14");
		citiesList.put("MISSION HILLS", "15");
		citiesList.put("MISSION WOODS", "16");
		citiesList.put("NEW CENTURY", "32");
		citiesList.put("OLATHE", "18");
		citiesList.put("OVERLAND PARK", "20");
		citiesList.put("PRAIRIE VILLAGE", "22");
		citiesList.put("ROELAND PARK", "23");
		citiesList.put("SHAWNEE", "24");
		citiesList.put("SPRING HILL", "26");
		citiesList.put("STILWELL", "30");
		citiesList.put("WESTWOOD", "28");
		citiesList.put("WESTWOOD HILLS", "29");
	}
	
	public KSJohnsonAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			String streetDirection = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa()
					.getAtribute(SearchAttributes.P_STREETDIRECTION).toUpperCase();

			if (streetDirection == null) {
				streetDirection = "";
			}
			tsServerInfoModule.getFunction(1).setHtmlformat("<select NAME=\"ddlDir\">" +
					"<option " + ("All".equals(streetDirection) ? "selected" : "") + " value=\"All\">All</option>" +
					"<option " + ("N".equals(streetDirection) ? "selected" : "") + " value=\"N\">N</option>" +
					"<option " + ("S".equals(streetDirection) ? "selected" : "") + " value=\"S\">S</option>" +
					"<option " + ("E".equals(streetDirection) ? "selected" : "") + " value=\"E\">E</option>" +
					"<option " + ("W".equals(streetDirection) ? "selected" : "") + " value=\"W\">W</option>" +
					"</select>");
			
			String city = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getAtribute(SearchAttributes.P_CITY)
					.toUpperCase();
			tsServerInfoModule.getFunction(4).setHtmlformat(
					"<select NAME=\"ddlCity\">" +
							"<option" + ("All".toUpperCase().equals(city) ? "selected" : "") + " value=\"All\">All</option>" +
							"<option " + ("Bonner Springs".toUpperCase().equals(city) ? "selected" : "") + " value=\"2\">Bonner Springs</option>" +
							"<option " + ("Bucyrus".toUpperCase().equals(city) ? "selected" : "") + " value=\"33\">Bucyrus</option>" +
							"<option " + ("DeSoto".toUpperCase().equals(city) ? "selected" : "") + " value=\"3\">DeSoto</option>" +
							"<option " + ("Edgerton".toUpperCase().equals(city) ? "selected" : "") + " value=\"4\">Edgerton</option>" +
							"<option " + ("Eudora".toUpperCase().equals(city) ? "selected" : "") + " value=\"31\">Eudora</option>" +
							"<option " + ("Fairway".toUpperCase().equals(city) ? "selected" : "") + " value=\"5\">Fairway</option>" +
							"<option " + ("Gardner".toUpperCase().equals(city) ? "selected" : "") + " value=\"6\">Gardner</option>" +
							"<option " + ("Lake Quivira".toUpperCase().equals(city) ? "selected" : "") + " value=\"8\">Lake Quivira</option>" +
							"<option " + ("Leawood".toUpperCase().equals(city) ? "selected" : "") + " value=\"9\">Leawood</option>" +
							"<option " + ("Lenexa".toUpperCase().equals(city) ? "selected" : "") + " value=\"10\">Lenexa</option>" +
							"<option " + ("Merriam".toUpperCase().equals(city) ? "selected" : "") + " value=\"13\">Merriam</option>" +
							"<option " + ("Mission".toUpperCase().equals(city) ? "selected" : "") + " value=\"14\">Mission</option>" +
							"<option " + ("Mission Hills".toUpperCase().equals(city) ? "selected" : "") + " value=\"15\">Mission Hills</option>" +
							"<option " + ("Mission Woods".toUpperCase().equals(city) ? "selected" : "") + " value=\"16\">Mission Woods</option>" +
							"<option " + ("New Century".toUpperCase().equals(city) ? "selected" : "") + " value=\"32\">New Century</option>" +
							"<option " + ("Olathe".toUpperCase().equals(city) ? "selected" : "") + " value=\"18\">Olathe</option>" +
							"<option " + ("Overland Park".toUpperCase().equals(city) ? "selected" : "") + " value=\"20\">Overland Park</option>" +
							"<option " + ("Prairie Village".toUpperCase().equals(city) ? "selected" : "") + " value=\"22\">Prairie Village</option>" +
							"<option " + ("Roeland Park".toUpperCase().equals(city) ? "selected" : "") + " value=\"23\">Roeland Park</option>" +
							"<option " + ("Shawnee".toUpperCase().equals(city) ? "selected" : "") + " value=\"24\">Shawnee</option>" +
							"<option " + ("Spring Hill".toUpperCase().equals(city) ? "selected" : "") + " value=\"26\">Spring Hill</option>" +
							"<option " + ("Stilwell".toUpperCase().equals(city) ? "selected" : "") + " value=\"30\">Stilwell</option>" +
							"<option " + ("Westwood".toUpperCase().equals(city) ? "selected" : "") + " value=\"28\">Westwood</option>" +
							"<option " + ("Westwood Hills".toUpperCase().equals(city) ? "selected" : "") + " value=\"29\">Westwood Hills</option>" +
							"</select>");
							
			msiServerInfoDefault.setupParameterAliases();
			setModulesForAutoSearch(msiServerInfoDefault);
							
		}
		
		return msiServerInfoDefault;
	}


	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String parcelId = sa.getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_AO);
		String direction = sa.getAtribute(SearchAttributes.P_STREETDIRECTION);
		String cityName = sa.getAtribute(SearchAttributes.P_CITY).toUpperCase();

		if (!"".equals(parcelId)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}
		
		if (!"".equals(streetName)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			if (!"".equals(direction)) {
				if ("N".equals(direction.toUpperCase()) ||
					"S".equals(direction.toUpperCase()) || 
					"E".equals(direction.toUpperCase()) || 
					"W".equals(direction.toUpperCase())) 
				{
					m.forceValue(1, direction);
				}
			}
			if (!"".equals(cityName)) {
				String cityIdx = "";
				if (citiesList.containsKey(cityName)) {
					cityIdx = citiesList.get(cityName);
				}
				
				if (!"".equals(cityIdx)) {
					m.forceValue(4, cityIdx);
				}
			}
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_EMPTY);
			m.addFilter(new AddressFilterResponse2("",searchId));
			l.add(m);
		}
		
		if (!"".equals(streetName)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_NOT_EMPTY);
			m.addFilter(new AddressFilterResponse2("",searchId));
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);

	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		Authenticator.setDefault(WebAuth.getInstance(searchId));

		int serverInfoModuleID = module.getModuleIdx();
		if (serverInfoModuleID == 2) {
			String sParcelNo = module.getFunction(0).getParamValue();
			if (sParcelNo != null && sParcelNo.matches("(?i)\\s*[\\dA-Z]{8}\\s+[\\dA-Z]{3,5}\\s*")) {
				// replace YYYYYYYY XXXX with YYYYYYYY-XXXX
				// e.g. replace "3F221328 2006" with "3F221328-2006"
				// otherwise results aren't fetched
				String[] parcelTokens = sParcelNo.trim().split("\\s+");
				sParcelNo = parcelTokens[0] + "-" + parcelTokens[1];
				module.getFunction(0).setParamValue(sParcelNo);
			}
		}
		return super.SearchBy(module, sd);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponce = Response.getResult();
		String initialResponse = rsResponce;

		if (viParseID == ID_SEARCH_BY_ADDRESS && rsResponce.indexOf("Found:&nbsp;") == -1) {
			viParseID = ID_DETAILS;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
			HtmlParser3 htmlParser = new HtmlParser3(Tidy.tidyParse(rsResponce, null));
			NodeList nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdResults"), true);	
			
			if(nodeList.size() > 0) {
				rsResponce = nodeList.elementAt(0).toHtml();
			}
			
			rsResponce = rsResponce.replaceAll("<hr[^>]*>", "");
			rsResponce = rsResponce.replaceAll("width=\".*\"", "");
			rsResponce = rsResponce.replaceAll("</?span[^>]*>", "");

			String linkStart = CreatePartialLink(TSConnectionURL.idGET);

			rsResponce = rsResponce.replaceAll("<a href=\"residentialsummary.aspx\\?([^\"]+)\">", "<A HREF='" + linkStart + "/residentialsummary.aspx&" + "$1"
					+ "'>");
			rsResponce = rsResponce.replaceAll("<a href=\"commercialsummary.aspx\\?([^\"]+)\">", "<A HREF='" + linkStart + "/commercialsummary.aspx&" + "$1"
					+ "'>");

			parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);

			break;

		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_MODULE38:// search by quick Ref #
		case ID_SEARCH_BY_MODULE39:// search by KUP #

			// get parcel id
			StringBuilder keyNumberSB = new StringBuilder();
			rsResponce = getDetails(rsResponce, keyNumberSB);
			
			// something went wrong when extracting the details
			if(StringUtils.isEmpty(keyNumberSB.toString())) {
				return;
			}
			
			// clean keynumber
			String keyNumber = keyNumberSB.toString().replaceAll("[ -]", "");
			
			if (!downloadingForSave) {

				String originalLink = sAction + "&dummy=" + keyNumber + "&" + Response.getQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (FileAlreadyExist(keyNumber + ".html")) {
					rsResponce += CreateFileAlreadyInTSD();
				} else {
					rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);

			} else {

				// for html
				smartParseDetails(Response, rsResponce);

				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(rsResponce);
				msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();

				// parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_DETAILS);
			}

			break;

		case ID_GET_LINK:
		case ID_SAVE_TO_TSD:

			if (sAction.equals("/SearchResults.html"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			else if (viParseID == ID_GET_LINK)
				ParseResponse(sAction, Response, ID_DETAILS);
			else {// on save
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
			}

			break;

		}
	}

	private String getDetails(String details, StringBuilder keyNumberSB) {
		details  = details.replaceAll("(?is)(<div[^>]*)(<div)", "$1>$2"); //fix an unclodes div open tag
		details = Tidy.tidyParse(details, null);
		HtmlParser3 htmlParser3 = new HtmlParser3(details);

		try {// get parcelID
			NodeList nodes = htmlParser3.getNodeList();

			if (nodes.size() > 0) {
				nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(new TagNameFilter("table"), true);
				Node pidNode = htmlParser3.getNodeById("lblParcelID");
				if (pidNode != null) {
					keyNumberSB.append(pidNode.toPlainTextString().replace("&nbsp;", ""));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		/* If from memory - use it as is */
		if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(details, "<html")) {
			return details;
		}
		details = "<tr id=\"mainDetails\"><td>" + details + "</td></tr>";

		int istart = -1;
		int iend = -1;

		// add property details
		String pageWithExtraDetailsLink = "";
		String parcelNumber = keyNumberSB.toString();
		parcelNumber = parcelNumber.replaceAll(" ", "+");
		String link = "http://ims.jocogov.org/aims2/identifypropfeature.asp?db=jocopub&lyr=property_pl&fld=geopropid&id=" + parcelNumber + "&src=aims2";

		pageWithExtraDetailsLink = getLinkContents(link);

		istart = pageWithExtraDetailsLink.indexOf("identifyprint");
		iend = pageWithExtraDetailsLink.indexOf("aims2", istart) + 5;
		String extraDetailsLink = "";
		if (istart < iend && istart >= 0 && iend >= 0) {
			extraDetailsLink = "http://ims.jocogov.org/aims2/" + pageWithExtraDetailsLink.substring(istart, iend);
			String propertyInfo = getLinkContents(extraDetailsLink);
			istart = propertyInfo.indexOf("<table");
			iend = propertyInfo.indexOf("</table>") + "</table>".length();
			if (istart < iend && istart >= 0 && iend >= 0) {
				propertyInfo = propertyInfo.substring(istart, iend);
				propertyInfo = Tidy.tidyParse(propertyInfo, null);
				details += "<tr id=\"propertyInfo\"><td>" + propertyInfo + "</td></tr>";
			}
		}

		// <a id="navTaxBill" href="http://taxbill.jocogov.org/retaxbill.aspx?taxYear=2013&amp;parcelID=CP02000000 0017" target="_blank">Tax Bill</a>
		Node taxBillLinkNode = htmlParser3.getNodeById("navTaxBill");
		if (taxBillLinkNode != null && taxBillLinkNode instanceof LinkTag) {
			String taxBillLink = StringEscapeUtils.unescapeHtml(((LinkTag) taxBillLinkNode).getLink());
			String taxBillContents = getLinkContents(taxBillLink);
			htmlParser3 = new HtmlParser3(taxBillContents);
			Node taxBillTable = htmlParser3.getNodeById("NavContentTable");
			if (taxBillTable != null) {
				taxBillContents = taxBillTable.toHtml()
						.replaceFirst("(?is)<div\\s+id=\"pnlPayment\"[^>]*>.*?</div>", "")
						.replaceAll("(?is)(<[^>]*)\\bstyle=\"[^\"]*\"", "$1")
						.replaceAll("(?is)<input[^>]*>", "");
				details += "<tr id=\"taxBillDetails\"><td>" + taxBillContents + "</td></tr>";
			}
		}

		details = details.replaceFirst("(?is)<ul\\s+id=\"nav\"[^>]*>.*?</ul>", "")
				.replaceFirst("(?is)<div[^>]*>\\s*Phone\\s*:\\s+[^<]*</div>", "")
				.replaceAll("(?is)<img\\b[^>]*>", "")
				.replaceAll("(?is)<a[^>]*>[^<]*</a>", "")
				.replaceAll("(<div)", "$1 align=\"center\"")
				.replaceAll("<hr[^>]*>", "")
				.replaceAll("width=\".*\"", "")
				.replaceAll("(?is)(<h)(?:1|2)([^>]*>)", "$13 align=\"center\" $2")
				.replaceAll("(?is)<head[^>]*>.*?</head>", "")
				.replaceAll("(?is)<script[^>]*>.*?</script>", "")
				.replaceAll("(?is)</?(html|body|head|title|script)[^>]*>", "");
		// .replaceAll("(?ism)<span[^>]*>", "")
		// .replaceAll("(?ism)</span>", "");
		details = "<table id=\"allDetails\" border=\"1\" align=\"center\" width=\"800px\">" + details + "</table>";

		return details;
	}

	@Override 
	protected String getFileNameFromLink(String url) {
		return url.substring(url.indexOf("pin=") + 4, url.length());
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {

		p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart, action);
		@SuppressWarnings("rawtypes")
		Vector rows = pr.getResultRows();

		for (int i = 1; i < rows.size(); i++) {
			ParsedResponse row = (ParsedResponse) rows.get(i);
			PropertyIdentificationSet data = row.getPropertyIdentificationSet(0);
			String pid = data.getAtribute("ParcelID");
			// now instrument number must be placed as a dummy parameter in HTTP
			// link query
			row.getPageLink().setOnlyLink(row.getPageLink().getLink().replaceFirst("\\.html", ".html&dummy=" + pid));
			row.getPageLink().setOnlyOriginalLink(row.getPageLink().getOriginalLink().replaceFirst("\\.html", ".html&dummy=" + pid));
		}

		// remove table header
		if (rows.size() > 0) {
			ParsedResponse firstRow = (ParsedResponse) rows.remove(0);
			pr.setResultRows(rows);
			pr.setHeader(pr.getHeader() + firstRow.getResponse());
		}

	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			String detailsEscapedHtml = StringEscapeUtils.unescapeHtml(detailsHtml)
					.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "")
					.replaceAll("(?is)<th\\b", "<td");

			HtmlParser3 htmlParser3 = new HtmlParser3(detailsEscapedHtml);
			Node mainDetailsRow = htmlParser3.getNodeById("mainDetails");
			Node propertyInfoRow = htmlParser3.getNodeById("propertyInfo");

			// get parcelID
			Node pidNode = htmlParser3.getNodeById("lblParcelID");

			if (pidNode != null) {
				String parcelID = pidNode.toPlainTextString().trim();
				if (StringUtils.isNotEmpty(parcelID)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}
			}

			// get address and city
			Node addressNode = htmlParser3.getNodeById("lblAddress");
			Node cityNode = htmlParser3.getNodeById("lblAddress2");

			String city = "";
			String address = "";
			if (addressNode != null) {
				address = addressNode.toPlainTextString();
			} else if (propertyInfoRow != null) {
				address = HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(propertyInfoRow.getChildren(), "Situs Address"), "", true)
						.replaceAll("(?s)<[^>]+>", "");

			}
			if (StringUtils.isNotEmpty(address)) {
				ro.cst.tsearch.servers.functions.KSJohnsonAO.parseAddress(resultMap, address.trim());
			}

			if (cityNode != null) {
				city = cityNode.toPlainTextString();
				if (city.contains(",")) {
					city = city.substring(0, city.indexOf(","));
				}
			} else if (propertyInfoRow != null) {
				city = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(propertyInfoRow.getChildren(), "City"), "", true)
						.replaceAll("(?s)<[^>]+>", "");
			}
			if (StringUtils.isNotEmpty(city)) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.trim());
			}

			// get owners
			if (propertyInfoRow != null) {
				String owner1 = HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(propertyInfoRow.getChildren(), "Owner1 Name"), "", true)
						.replaceAll("(?s)<[^>]+>", "").trim();
				String owner2 = HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(propertyInfoRow.getChildren(), "Owner2 Name"), "", true)
						.replaceAll("(?s)<[^>]+>", "").trim();

				ArrayList<String> names = new ArrayList<String>();
				if (StringUtils.isNotEmpty(owner1)) {
					names.add(owner1);
				}
				if (StringUtils.isNotEmpty(owner2)) {
					names.add(owner2);
				}

				if (names.size() > 0) {
					ro.cst.tsearch.servers.functions.KSJohnsonAO.parseNames(resultMap, names, "");
				}

				// get legal description
				String legal = HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(propertyInfoRow.getChildren(), "Legal Desc."), "", true)
						.replaceAll("(?s)<[^>]+>", "").trim();

				// get subdivision name
				String subdivisionName = HtmlParser3
						.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(propertyInfoRow.getChildren(), "Subdivision Name"), "", true)
						.replaceAll("(?s)<[^>]+>", "").trim();
				if (StringUtils.isNotEmpty(subdivisionName)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);

					if (StringUtils.isNotEmpty(legal)) {
						legal = legal.replaceFirst(subdivisionName, "");
					}
				}

				if (StringUtils.isNotEmpty(legal)) {
					resultMap.put("tmpLegal", legal);
					ro.cst.tsearch.servers.functions.KSJohnsonAO.parseLegal(resultMap, searchId);
				}

				// get book and page
				String bookPage = HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(propertyInfoRow.getChildren(), "Book/Page"), "", true)
						.replaceAll("(?s)<[^>]+>", "").trim();
				if (StringUtils.isNotEmpty(bookPage) && bookPage.contains("/")) {

					String book = bookPage.substring(0, bookPage.indexOf("/")).trim();
					if (StringUtils.isNotEmpty(book)) {
						resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book);
					}
					String page = bookPage.substring(bookPage.indexOf("/"), bookPage.length()).replaceFirst("/", "").trim();
					if (StringUtils.isNotEmpty(page)) {
						resultMap.put(SaleDataSetKey.PAGE.getKeyName(), page);
					}
				}

				// get quarter section
				String quarterSection = HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(propertyInfoRow.getChildren(), "Quarter Section"), "",
						true).replaceAll("(?s)<[^>]+>", "").trim();
				if (StringUtils.isNotEmpty(quarterSection)) {
					resultMap.put(PropertyIdentificationSetKey.QUARTER_VALUE.getKeyName(), quarterSection);
				}
			}

			// get appraised value
			String appraised = HtmlParser3.getValueFromAbsoluteCell(1, 0,
					HtmlParser3.findNode(mainDetailsRow.getChildren(), "Appraised Value"), "", false)
					.replaceAll("[^\\d.]+", "");
			if (StringUtils.isNotEmpty(appraised)) {
				resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), appraised);
			}

			// get assessed value
			String assessed = HtmlParser3.getValueFromAbsoluteCell(1, 0,
					HtmlParser3.findNode(mainDetailsRow.getChildren(), "Assessed Value"), "", false)
					.replaceAll("[^\\d.]+", "");
			if (StringUtils.isNotEmpty(assessed)) {
				resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessed);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
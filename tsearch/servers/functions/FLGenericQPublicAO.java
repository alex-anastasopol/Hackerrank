package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

public class FLGenericQPublicAO extends ParseClass {
	private static final String SALES_TABLE_HEADER = "(?is)<TABLE BORDER COLS=1 WIDTH=\"100%\">.*?\"sales_header\".*?</TABLE>";

	private static FLGenericQPublicAO mainInstance = new FLGenericQPublicAO();

	private FLGenericQPublicAO() {
	}

	public static enum FlGenericQPublicAOParseType {
		FLWashingtonAO, FLGilchristAO, FLGulfAO, DefaultGenericQPublicAOParse, FLWakullaAO;
	}

	private static FLGenericQPublicAO wakullaFactory = mainInstance.new FLWakullaAO();
	private static FLGenericQPublicAO washingtonFactory = mainInstance.new FLWashingtonAO();
	private static FLGenericQPublicAO gulfFactory = mainInstance.new FLGulfAO();
	private static FLGenericQPublicAO gilchristFactory = mainInstance.new FLGilchristAO();
	private static FLGenericQPublicAO defaultFactory = mainInstance.new FLGenericQPublicAODefault();

	public static FLGenericQPublicAO getInstance(FlGenericQPublicAOParseType parseType) {
		FLGenericQPublicAO _instance = defaultFactory;

		if (parseType.ordinal() == FlGenericQPublicAOParseType.FLWashingtonAO.ordinal()) {
			_instance = washingtonFactory;
		}
		if (parseType.ordinal() == FlGenericQPublicAOParseType.FLGilchristAO.ordinal()) {
			_instance = gilchristFactory;
		}

		if (parseType.ordinal() == FlGenericQPublicAOParseType.FLGulfAO.ordinal()) {
			_instance = gulfFactory;
		}

		if (parseType.ordinal() == FlGenericQPublicAOParseType.FLWakullaAO.ordinal()) {
			_instance = wakullaFactory;
		}

		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		HtmlParser3 parser = new HtmlParser3(response);
		NodeList node = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "tr", "onmouseout",
				"this.style.backgroundColor=''", true);
		// for sale search there is a different identifier
		boolean saleSearchParse = false;

		if (node.size() == 0) {
			node = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "tr", "onmouseout",
					"this.style.backgroundColor='#FFFFFF'", true);
			if (node.size() > 0) {
				saleSearchParse = true;
			}
		}
		SimpleNodeIterator elements = node.elements();
		String startLink = createPartialLink(format, TSConnectionURL.idGET);

		while (elements.hasMoreNodes()) {
			ParsedResponse currentResponse = new ParsedResponse();

			Node nextNode = elements.nextNode();
			String rowHtml = nextNode.toHtml();

			NodeList tableColumns = nextNode.getChildren();

			SimpleNodeIterator tableColumnsIterator = tableColumns.elements();

			int i = 0;
			ResultMap resultMap = new ResultMap();
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
			while (tableColumnsIterator.hasMoreNodes()) {
				Node nextColumn = tableColumnsIterator.nextNode();
				if (nextColumn instanceof TableColumn) {
					addValueToResultMap(i, resultMap, nextColumn, saleSearchParse);
					i++;
				}
			}

			// saveTestDataToFiles(resultMap);
			parseName("", resultMap);
			parseAddress("", resultMap);
			setSecTwnRng("", resultMap);
			setCorrectInstrumentNumber(resultMap);
			resultMap.removeTempDef();

			String regExLink = "(?<=<a href=\")(.*?)(?=\")";
			String link = RegExUtils.getFirstMatch(regExLink, rowHtml, 1);
			String newLink = startLink + link;
			rowHtml = rowHtml.replaceAll(regExLink, newLink);

			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
			currentResponse.setOnlyResponse(rowHtml);
			currentResponse.setPageLink(new LinkInPage(newLink, newLink, TSServer.REQUEST_SAVE_TO_TSD));

			createDocument(searchId, currentResponse, resultMap);
			intermediaryResponse.add(currentResponse);
		}

		String formatHeader = "";
		if (!saleSearchParse) {
			formatHeader = MessageFormat.format("<tr><th>{0}</th><th>{1}</th><th>{2}</th><th>{3}</th></tr>", "Parcel Number", "Owner Name",
					"Address", "Homestead");
		} else {
			formatHeader = MessageFormat.format("<tr><th>{0}</th><th>{1}</th><th>{2}</th><th>{3}</th><th>{4}</th><th>{5}</th>"
					+ "<th>{6}</th><th>{7}</th><th>{8}</th><th>{9}</th><th>{10}</th><th>{11}</th>" + "</tr>", "Parcel Number",
					"Sec-Twn-Rng", "Sale<br>Date", "Sale<br>Price", "Heated<br>Square<br>Footage", "Acreage", "Book", "Page",
					"Inst<br>Type", "Inst<br>Number", "Sale<br>Qualification", "Sale<br>Vacant<br>Improved");
		}

		StringBuilder header = new StringBuilder("<table width=\"100%\">" + formatHeader);
		serverResponse.getParsedResponse().setHeader(header.toString());
		String footer = "</table>";
		serverResponse.getParsedResponse().setFooter(footer);

		return intermediaryResponse;
	}

	private void setCorrectInstrumentNumber(ResultMap resultMap) {
		String saleDate = (String) resultMap.get("SaleDataSet.InstrumentDate");
		String book = (String) resultMap.get("SaleDataSet.Book");
		String page = (String) resultMap.get("SaleDataSet.Page");
		if (StringUtils.isNotEmpty(saleDate) && StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			int bookAsInt = 0;
			if (org.apache.commons.lang.math.NumberUtils.isDigits(book)) {
				bookAsInt = Integer.parseInt(book);
			}
			if (saleDate.contains(book) && bookAsInt >= 2000) {
				resultMap.put("SaleDataSet.InstrumentNumber", page);
				try {
					resultMap.remove("SaleDataSet.Book");
					resultMap.remove("SaleDataSet.Page");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static final Map<String, String> columnIndexToResultMapKeyMap = new HashMap<String, String>();

	static {
		columnIndexToResultMapKeyMap.put("1", "tmpSecTwnRng");
		columnIndexToResultMapKeyMap.put("2", "SaleDataSet.InstrumentDate");
		columnIndexToResultMapKeyMap.put("3", "SaleDataSet.SalesPrice");
		columnIndexToResultMapKeyMap.put("6", "SaleDataSet.Book");
		columnIndexToResultMapKeyMap.put("7", "SaleDataSet.Page");
		columnIndexToResultMapKeyMap.put("8", "SaleDataSet.DocumentType");
		columnIndexToResultMapKeyMap.put("9", "SaleDataSet.InstrumentNumber");
	}

	private void addValueToResultMap(int i, ResultMap resultMap, Node nextColumn, boolean saleSearchParse) {

		String key = "PropertyIdentificationSet.ParcelID";
		int expectedColumnIndex = 0;
		String value = putValueInResultMap(i, expectedColumnIndex, resultMap, nextColumn, key);
		if (!saleSearchParse) {

			key = "PropertyIdentificationSet.NameOnServer";
			expectedColumnIndex = 1;
			putValueInResultMap(i, expectedColumnIndex, resultMap, nextColumn, key);

			key = "PropertyIdentificationSet.AddressOnServer";
			expectedColumnIndex = 2;
			putValueInResultMap(i, expectedColumnIndex, resultMap, nextColumn, key);
		} else {

			key = columnIndexToResultMapKeyMap.get("" + i);
			if (StringUtils.isNotEmpty(key)) {
				putValueInResultMap(i, i, resultMap, nextColumn, key);
			}
		}
	}

	private String putValueInResultMap(int columnIndex, int expectedColumnIndex, ResultMap resultMap, Node nextColumn, String key) {
		String value = ro.cst.tsearch.utils.StringUtils.cleanHtml(nextColumn.toPlainTextString());
		resultMap.put(key,
				(columnIndex == expectedColumnIndex && StringUtils.isEmpty((String) resultMap.get(key))) ? value : resultMap.get(key));
		return value;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		response = response.toUpperCase();
		
		HtmlParser3 parser = new HtmlParser3(response);
		String parcelID = getNodeValue(parser, "PARCEL NUMBER");
		parcelID = ro.cst.tsearch.utils.StringUtils.cleanHtml(parcelID).trim();

		resultMap.put("PropertyIdentificationSet.ParcelID", parcelID);

		String ownerName = getNodeValue(parser, "OWNER NAME").trim();
		if (ownerName.endsWith("TRUSTEE") || ownerName.endsWith("&") || ownerName.endsWith("MISS") || ownerName.endsWith(" OF")
				|| ownerName.endsWith(" ETAL")) {
			String trim = getNodeValue(parser, "MAILING ADDRESS").trim();
			ownerName += " " + trim;
		} else {
			String currentNodeValue = "";
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			currentNodeValue = ro.cst.tsearch.utils.StringUtils.cleanHtml(HtmlParser3.getNodeValue(parser, "MAILING ADDRESS", 0, 1)).trim();
			boolean mailingAddresIsAddress = !StringUtils.isEmpty(currentNodeValue) && RegExUtils.matches("\\d+.*", currentNodeValue);
			int mailingAddressDepth = getMailingAddressDepth(parser);
			if (mailingAddressDepth > 1) {
				for (int i = 0; i < mailingAddressDepth - 1; i++) {
					currentNodeValue = ro.cst.tsearch.utils.StringUtils
							.cleanHtml(HtmlParser3.getNodeValue(parser, "MAILING ADDRESS", i, 1)).trim();
					if (crtCounty.equalsIgnoreCase("wakulla"))
					{		
						if (!isAddress(currentNodeValue)) ownerName += " " + currentNodeValue;
					}	 
					else ownerName += " & " + currentNodeValue;
				}
			}
		}

		parseName(ownerName, resultMap);

		String locationAddress = getNodeValue(parser, "LOCATION ADDRESS");
		parseAddress(locationAddress, resultMap);

		String secTwnRng = HtmlParser3.getNodeValue(parser, "SECTION TOWNSHIP RANGE", 0, 1).trim();
		setSecTwnRng(secTwnRng, resultMap);

		setSaleDataSetInfo(response, resultMap);

		String legalDescriptionText = getLegalDescriptionText(response);
		parseLegalDescription(legalDescriptionText, resultMap);
	}

	public boolean isAddress(String current)
	{
		String[] tokens = current.split(" ");
		int number = tokens.length;
		for (int i=1; i<number; i++) if (Normalize.isSuffix(tokens[i]) && tokens[i-1].matches("\\w+")) return true;
		return false;
	}
	
	private int getMailingAddressDepth(HtmlParser3 parser) {
		int depth = 0;
		try {
			boolean zipEncountered = false;
			while (!zipEncountered && depth < 6) {
				String nextNode = ro.cst.tsearch.utils.StringUtils.cleanHtml(HtmlParser3.getNodeValue(parser, "MAILING ADDRESS", depth, 1));
				zipEncountered = !StringUtils.isEmpty(nextNode) && RegExUtils.matches(".*, \\w+\\s\\d+", nextNode);
				if (!zipEncountered) {
					depth++;
				}
			}
		} catch (Exception e) {
			depth = 1;
			e.printStackTrace();
		}
		return depth;
	}

	public String getNodeValue(HtmlParser3 parser, String nodeLabel) {
		String cell = parser.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(parser.getNodeList(), nodeLabel), "", true);
		cell = ro.cst.tsearch.utils.StringUtils.cleanHtml(cell);
		return cell;
	}

	@SuppressWarnings("rawtypes")
	protected void setSaleDataSetInfo(String response, ResultMap resultMap) {
		try {
			Node setSaleDataSetTableNode = HtmlParser3.getNodeByTypeAttributeDescription(new HtmlParser3(response).getNodeList(), "table", "", "",
					new String[] { "SALE DATE", "SALE PRICE", "INSTRUMENT", "DEED BOOK", "DEED PAGE", "GRANTOR", "GRANTEE" }, true);
			if (setSaleDataSetTableNode != null) {
				List<List> sdsBody = new ArrayList<List>();
				ResultTable resT = new ResultTable();

				String[] sdsHeader = new String[] { SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
						SaleDataSetKey.BOOK.getShortKeyName(),
						SaleDataSetKey.PAGE.getShortKeyName(),
						SaleDataSetKey.SALES_PRICE.getShortKeyName(),
						SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(),
						SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
						SaleDataSetKey.GRANTOR.getShortKeyName(),
						SaleDataSetKey.GRANTEE.getShortKeyName()
				};

				Map<String, String[]> sdsHeaderMap = new HashMap<String, String[]>();
				for (String s : sdsHeader) {
					sdsHeaderMap.put(s, new String[] { s, "" });
				}

				TableRow[] sdsTableRows = ((TableTag) setSaleDataSetTableNode).getRows();

				for (int i = 2; i < sdsTableRows.length; i++) {
					List<String> sdsRow = new ArrayList<String>();
					TableColumn[] columns = sdsTableRows[i].getColumns();
					if (columns.length >= 8) {
						String defaultStr = "";

						// get sale date
						String value = StringUtils.defaultIfEmpty(columns[0].toPlainTextString(), defaultStr).trim();
						String saleDate = ro.cst.tsearch.utils.StringUtils.cleanHtml(value);

						// get book
						value = StringUtils.defaultIfEmpty(columns[3].toPlainTextString(), defaultStr).trim();
						String book = ro.cst.tsearch.utils.StringUtils.cleanHtml(value);
						book = RegExUtils.getFirstMatch("(?s)\\s*(\\d+)", book, 1);

						// get page
						value = StringUtils.defaultIfEmpty(columns[4].toPlainTextString(), defaultStr).trim();
						String page = ro.cst.tsearch.utils.StringUtils.cleanHtml(value);
						if ("0".equals(book) && "0".equals(page)) {
							book = "";
							page = "";
						}

						// get sale price
						value = StringUtils.defaultIfEmpty(columns[1].toPlainTextString(), defaultStr);
						String cleanPrice = ro.cst.tsearch.utils.StringUtils.cleanAmount(value).trim();

						int bookAsInt = 0;
						String instYear = "";
						String instNo = "";
						if (org.apache.commons.lang.math.NumberUtils.isDigits(book)) {
							bookAsInt = Integer.parseInt(book);

							// get inst year and inst No
							if (saleDate.contains(book) && bookAsInt >= 2000) {
								instYear = book;
								instNo = page;
								book = "";
								page = "";
							}
						}

						// get doc type
						value = StringUtils.defaultIfEmpty(columns[2].toPlainTextString(), defaultStr);
						String docType = ro.cst.tsearch.utils.StringUtils.cleanAmount(value).trim();

						String grantor = "";
						String grantee = "";

						if (columns.length == 8) {// e.g. Washington county
							// get grantor
							value = StringUtils.defaultIfEmpty(columns[6].toPlainTextString(), defaultStr);
							grantor = ro.cst.tsearch.utils.StringUtils.cleanAmount(value).trim();

							// get grantee
							value = StringUtils.defaultIfEmpty(columns[7].toPlainTextString(), defaultStr);
							grantee = ro.cst.tsearch.utils.StringUtils.cleanAmount(value).trim();
						} else if (columns.length == 9) {
							// get grantor
							value = StringUtils.defaultIfEmpty(columns[7].toPlainTextString(), defaultStr);
							grantor = ro.cst.tsearch.utils.StringUtils.cleanAmount(value).trim();

							// get grantee
							value = StringUtils.defaultIfEmpty(columns[8].toPlainTextString(), defaultStr);
							grantee = ro.cst.tsearch.utils.StringUtils.cleanAmount(value).trim();
						}

						// add to row
						sdsRow.add(instNo);
						sdsRow.add(book);
						sdsRow.add(page);
						sdsRow.add(cleanPrice);
						sdsRow.add(saleDate);
						sdsRow.add(docType);
						sdsRow.add(grantor);
						sdsRow.add(grantee);

						if (sdsRow.size() == sdsHeaderMap.size()) {
							sdsBody.add(sdsRow);
						}
					}
				}

				if (!sdsBody.isEmpty()) {
					resT.setHead(sdsHeader);
					resT.setMap(sdsHeaderMap);
					resT.setBody(sdsBody);
					resT.setReadOnly();
					resultMap.put("SaleDataSet", resT);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		if (StringUtils.isEmpty(addressOnServer)) {
			addressOnServer = StringUtils.defaultIfEmpty((String) resultMap.get("PropertyIdentificationSet.AddressOnServer"), "");
		}
		addressOnServer = StringUtils.defaultIfEmpty(addressOnServer, "");
		addressOnServer = ro.cst.tsearch.utils.StringUtils.cleanHtml(addressOnServer);

		addressOnServer = addressOnServer.replaceAll("\\bOFF\\b", "");

		String abreviation = AddressAbrev.detectAbbreviation(addressOnServer);
		String direction = AddressAbrev.detectDirection(addressOnServer);
		boolean containsAbreviation = StringUtils.isNotEmpty(abreviation);
		boolean containsDirection = StringUtils.isNotEmpty(direction);

		if (containsAbreviation && containsDirection) {
			String strippedAddress = addressOnServer.replaceFirst("\\b" + abreviation + "\\b", "").replaceFirst("\\b" + direction + "\\b", "");
			if (RegExUtils.matches("\\d+\\s+\\d+", strippedAddress)) {
				String[] split = strippedAddress.split("\\s+");
				resultMap.put("PropertyIdentificationSet.StreetNo", split[0]);
				// street without number
				String addressWithoutNumbers = addressOnServer.replace(split[0], "");
				resultMap.put("PropertyIdentificationSet.StreetName", addressWithoutNumbers);// direction
																								// +
																								// " "
																								// +
																								// split[1]
																								// +
																								// " "
																								// +
																								// abreviation
			} else if (RegExUtils.matches("^\\d+$", strippedAddress.trim())) {// ^\\d+$
				resultMap.put("PropertyIdentificationSet.StreetName", direction + " " + strippedAddress + " " + abreviation);
			} else {
				standardAddressParse(addressOnServer, resultMap);
			}
		} else {
			standardAddressParse(addressOnServer, resultMap);
		}

	}

	private void standardAddressParse(String addressOnServer, ResultMap resultMap) {
		String streetName = StringFormats.StreetName(addressOnServer).trim();
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put("PropertyIdentificationSet.StreetName", streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		if (StringUtils.isEmpty(name)) {
			name = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		}
		name = StringUtils.defaultIfEmpty(name, "");
		name = name.trim().replaceAll("&(\\r\\n|$)", "");
		name = ro.cst.tsearch.utils.StringUtils.cleanHtml(name);
		name = name.replaceAll("& ETAL", "ETAL");
		name = name.replaceAll("\\bET AL\\b", "ETAL");
		name = name.replaceAll("\\bET UX\\b", "ETUX");
		name = name.replaceAll("\\bC/O\\b", "");
		name = name.replaceAll("& TRUSTEES", "TRUSTEES");
		
		if (name.contains("AND/OR")) {
			String[] split = name.split("\\s");
			String firstName = split[0];
			if (name.lastIndexOf(firstName) > 0) {
				String substring = " AND " + name.substring(13);
				name = name.substring(0, 13) + substring;
			}
		}

		name = name.replaceAll("AND/OR", "");
		List body = new ArrayList();
		if (!NameUtils.isCompany(name) && name.contains("&")) {
			String[] split = name.split("&");
			if (split.length == 2 && RegExUtils.matches("\\w+\\s+\\w{1,}\\s+\\w{2,}", split[1])) {
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[0], body);
				ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, split[1], body);
			} else {
				parseMultipleNames(resultMap, body, name);
			}
		} else {
			parseMultipleNames(resultMap, body, name);
		}
	}

	private void parseMultipleNames(ResultMap resultMap, List body, String name) {
		int i = 0;
		String[] split = name.split("&");
		if (split.length > 2) {
			while (i < split.length) {
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[i], body);
				i++;
			}
		} else {
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, name, body);
		}
	}

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {

	}

	protected String getLegalDescriptionText(String response) {
		String match = RegExUtils.getFirstMatch("(?s)SHORT LEGAL.*-1>\\s(.*)</font>", response, 1);
		return match;
	}

	private class FLWashingtonAO extends FLGenericQPublicAO {
		@Override
		public void parseName(String name, ResultMap resultMap) {
			if (StringUtils.isEmpty(name)) {
				name = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
			}
			name = ro.cst.tsearch.utils.StringUtils.cleanHtml(name);
			FLWashingtonTR.parseNames(resultMap, name, false);
		}
	}

	private class FLWakullaAO extends FLGenericQPublicAO {
		@Override
		public void parseName(String name, ResultMap resultMap) {
			if (StringUtils.isEmpty(name)) {
				name = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
			}
			if (StringUtils.isNotEmpty(name)){
				name = ro.cst.tsearch.utils.StringUtils.cleanHtml(name);
				name = name.replaceAll("(?is)\\b(?:C/O|\\s*&)\\s+(\\w+)\\s*&\\s*(\\w+(?:\\s+\\w+)?)\\s*$", " C/O $1 $2");
				resultMap.put("tmpOwnerInfo", name);
				if (StringUtils.isNotEmpty(name) &&  name.contains("C/O")) {
					String[] split = name.split("C/O");
					if (split.length==2){
						List body = new ArrayList();
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[0], body );
						if (!split[1].trim().matches("(?i)\\w+\\s+SUPERVISOR"))
							ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, split[1], body);
					}else{
						FLWakullaTR.parseNames(resultMap, false, -1l);
					}
				} 
				else if ((StringUtils.isNotEmpty(name) &&  name.contains("DBA")))
				{
					String[] split = name.split("DBA");
					if (split.length==2){
						List body = new ArrayList();
						ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, split[0], body );
						ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, split[1], body );
					}else{
						FLWakullaTR.parseNames(resultMap, false, -1l);
					}
				}
				else {
					FLWakullaTR.parseNames(resultMap, false, -1l);
				}
			}
		}
	}

	private class FLWashingtonWakullaAO extends FLGenericQPublicAO {
		@Override
		protected void setSaleDataSetInfo(String response, ResultMap resultMap) {
			if (!response.contains("No sales information associated with this parcel")) {
				String table = RegExUtils.getFirstMatch(SALES_TABLE_HEADER, response, 0);
				table = table.replaceAll("(?is)<TR>.*?table_header.*?</TR>", "");
				table = table.replaceAll("<TR>.*?Click on the Book-Page to view the Official Record.*?</TR>", "");

				Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String, String>();
				resultBodyHeaderToSourceTableHeader.put("InstrumentDate", "SALE DATE");
				resultBodyHeaderToSourceTableHeader.put("Book", "Book");
				resultBodyHeaderToSourceTableHeader.put("Page", "Page");
				resultBodyHeaderToSourceTableHeader.put("SalesPrice", "PRICE");
				resultBodyHeaderToSourceTableHeader.put("DocumentType", "INSTRUMENT");
				resultBodyHeaderToSourceTableHeader.put("Grantor", "GRANTOR");
				resultBodyHeaderToSourceTableHeader.put("Grantee", "GRANTEE");

				putDataIntoSaleDataSet(resultMap, table, resultBodyHeaderToSourceTableHeader);

			}
		}

		private void putDataIntoSaleDataSet(ResultMap resultMap, String table, Map<String, String> resultBodyHeaderToSourceTableHeader) {
			List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(table);
			for (HashMap<String, String> hashMap : tableAsListMap) {
				String bookPageValue = hashMap.get("BOOK-PAGE");
				String price = hashMap.get("PRICE");

				if (StringUtils.isNotEmpty(price)) {
					String cleanPrice = ro.cst.tsearch.utils.StringUtils.cleanAmount(price);
					hashMap.put("PRICE", cleanPrice);
				}

				if (StringUtils.isNotEmpty(bookPageValue) && bookPageValue.contains("-")) {
					String[] split = bookPageValue.split("-");
					hashMap.put("Book", split[0]);
					hashMap.put("Page", split[1]);
				}
			}

			String[] header = new String[] { "Book", "Page", "SalesPrice", "InstrumentDate", "DocumentType", "Grantor", "Grantee" };
			ResultBodyUtils.buildInfSet(resultMap, tableAsListMap, header, resultBodyHeaderToSourceTableHeader, SaleDataSet.class);
		}

	}

	private class FLGenericQPublicAODefault extends FLGenericQPublicAO {
		@Override
		protected String getLegalDescriptionText(String response) {
			return super.getLegalDescriptionText(response);
		}
	}

	private class FLGulfAO extends FLGenericQPublicAO {

	}

	private class FLGilchristAO extends FLGenericQPublicAO {
		@Override
		protected String getLegalDescriptionText(String response) {
			return super.getLegalDescriptionText(response);
		}
	}

	public void setSecTwnRng(String name, ResultMap resultMap) {
		String secTwnRng = (String) resultMap.get("tmpSecTwnRng");
		if (StringUtils.isEmpty(secTwnRng)) {
			secTwnRng = name;
			if (StringUtils.isNotEmpty(secTwnRng)) {
				secTwnRng = ro.cst.tsearch.utils.StringUtils.cleanHtml(secTwnRng);
				String[] split = secTwnRng.split("-");
				if (split.length == 3) {
					resultMap.put("PropertyIdentificationSet.SubdivisionSection", split[0]);
					resultMap.put("PropertyIdentificationSet.SubdivisionTownship", split[1]);
					resultMap.put("PropertyIdentificationSet.SubdivisionRange", split[2]);
				}
			}
		}
	}

}

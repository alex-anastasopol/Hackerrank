package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

public class SCGreenvilleTR extends ParseClassWithExceptionTreatment {

	private static SCGreenvilleTR _instance = null;

	private static MessageFormat linkFormat;

	public static MessageFormat getLinkFormat() {
		return linkFormat;
	}

	public static void setLinkFormat(MessageFormat linkFormat) {
		SCGreenvilleTR.linkFormat = linkFormat;
	}

	private SCGreenvilleTR() {
		super(_instance);
	}

	public static SCGreenvilleTR getInstance() {
		if (_instance == null) {
			_instance = new SCGreenvilleTR();
		}
		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		HtmlParser3 parser = new HtmlParser3(response);
		NodeList tableTag = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		TableTag resultTable = (TableTag) tableTag.elementAt(1);
		if (resultTable != null) {

			TableRow[] rows = resultTable.getRows();

			int nameColumn = 5;
			int parcelColumn = 3;
			int streetNumberColumn = 7;
			int streetNameColumn = 9;
			int firstColumn = 1;

			String startLink = createPartialLink(format, TSConnectionURL.idGET);

			for (int i = 1; i < rows.length; i++) {
				NodeList cells = rows[i].getChildren();

				Node parcelColumnNode = cells.elementAt(parcelColumn);
				String parcelId = "";
				if (parcelColumnNode != null) {
					parcelId = StringUtils.defaultIfEmpty(parcelColumnNode.toPlainTextString(), "").trim();
				}

				if (StringUtils.isNotEmpty(parcelId)) {
					ResultMap resultMap = new ResultMap();
					parcelId = parcelId.replaceAll("\\.|\\-", "");
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);

					Node nameNode = cells.elementAt(nameColumn);
					String name = nameNode.toPlainTextString();
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
					_instance.parseName(name, resultMap);

					Node streetNumberNode = cells.elementAt(streetNumberColumn);
					String streetNumber = streetNumberNode.toPlainTextString();
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.STREET_NO.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(streetNumber));

					Node streetNameNode = cells.elementAt(streetNameColumn);
					String streetName = streetNameNode.toPlainTextString();
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);

					ParsedResponse currentResponse = new ParsedResponse();
					rows[i].removeChild(firstColumn);
					Node spanTag = parcelColumnNode.getFirstChild();

					if (spanTag instanceof LinkTag) {
						LinkTag linkTag = (LinkTag) spanTag;
						String url = startLink + "/vrealpr24/" + linkTag.getLink();

						((LinkTag) spanTag).setLink(url);
						currentResponse.setPageLink(new LinkInPage(url, url, TSServer.REQUEST_SAVE_TO_TSD));
					}

					String rowHtml = rows[i].toHtml();
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					createDocument(searchId, currentResponse, resultMap);
					intermediaryResponse.add(currentResponse);
				}
			}

			rows[0].removeChild(firstColumn);
			String tableHeader = rows[0].getChildren().toHtml();

			String formAction = RegExUtils.getFirstMatch("name=\"actionForm\" value=\"(.*?)\"", response, 1);
			if (serverResponse != null) {
				String tableFooter = "";
				serverResponse.getParsedResponse().setHeader("<table>" + tableHeader);
				serverResponse.getParsedResponse().setFooter(tableFooter + "</table>");
			}
		}

		return intermediaryResponse;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		HtmlParser3 parser = new HtmlParser3(response);
		int offsetRow = 0;
		int offsetColumn = 1;
		String mapNo = HtmlParser3.getNodeValue(parser, "Map #", offsetRow, offsetColumn);
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), mapNo);

		String year = HtmlParser3.getNodeValue(parser, "Year", offsetRow, offsetColumn);
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), RegExUtils.getFirstMatch("\\d+" , year, 0));

		String owner1 = HtmlParser3.getNodeValue(parser, "Owner 1", offsetRow, offsetColumn);
		resultMap.put("tmpOwner1", owner1);

		String owner2 = HtmlParser3.getNodeValue(parser, "Owner 2", offsetRow, offsetColumn);
		resultMap.put("tmpOwner2", owner2);

		String careOf = HtmlParser3.getNodeValue(parser, "Care Of", offsetRow, offsetColumn);
		resultMap.put("tmpOwner3", careOf);
		parseName("", resultMap);

		String address = HtmlParser3.getNodeValue(parser, "Loc", offsetRow, offsetColumn);
		parseAddress(address, resultMap);

		String subdiv = HtmlParser3.getNodeValue(parser, "SubDiv", offsetRow, offsetColumn);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

		String deedBookPage = HtmlParser3.getNodeValue(parser, "Deed Book-Pg", offsetRow, offsetColumn);

		if (StringUtils.isNotEmpty(deedBookPage)) {
			String[] split = deedBookPage.split("-");
			if (split != null && split.length == 2) {
				resultMap.put(SaleDataSetKey.BOOK.getKeyName(), split[0].trim());
				resultMap.put(SaleDataSetKey.PAGE.getKeyName(), split[1].trim());
			}
		}
		String deedDate = HtmlParser3.getNodeValue(parser, "Deed Date", offsetRow, offsetColumn);
		resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), deedDate);

		String salesPrice = HtmlParser3.getNodeValue(parser, "Sales Price", offsetRow, offsetColumn);
		resultMap.put(SaleDataSetKey.SALES_PRICE.getKeyName(), ro.cst.tsearch.utils.StringUtils.cleanAmount(salesPrice));

		String platBookPg = HtmlParser3.getNodeValue(parser, "Plat Book/ Pg", offsetRow, offsetColumn);
		if (StringUtils.isNotEmpty(platBookPg)) {
			String[] split = platBookPg.split("/");
			if (split != null && split.length == 2) {
				resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), split[0].trim());
				resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), split[1].trim());
			}
		}

		String taxableMarketValue = HtmlParser3.getNodeValue(parser, "Taxable Market Value", offsetRow, offsetColumn);
		resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), taxableMarketValue);
		putExternalTablesInResultMap(resultMap, parser);

		Node nodeByID = HtmlParser3.getNodeByID("taxHistoryId", parser.getNodeList(), true);
		putTaxHistory(resultMap, nodeByID);

	}

	/**
	 * @param resultMap
	 * @param parser
	 */
	public void putExternalTablesInResultMap(ResultMap resultMap, HtmlParser3 parser) {
		NodeList externalTables = parser.getNodeListByTypeAndAttribute("table", "rules", "groups", true);
		SimpleNodeIterator elements = externalTables.elements();
		String taxMarketValueKey = "Taxable Market Value:";
		String ownerNameKey = "Owner Name:";
		String[] assessmentHistoryLabels = new String[] { "Tax Year:", "PIN / Tax Map #:", ownerNameKey, taxMarketValueKey, "Taxes:" };
		String deedBookKey = "Deed Book:";
		String deedPageKey = "Deed Page:";
		String deedDateKey = "Deed Date:";
		String salePriceKey = "Sales Price:";
		String[] ownershipHistoryLabels = new String[] { "PIN / Tax Map #:", ownerNameKey, salePriceKey, deedDateKey, deedBookKey,
				deedPageKey };

		while (elements.hasMoreNodes()) {
			Node nextNode = elements.nextNode();

			if (nextNode.toHtml().contains("Assessment History")) {
				List<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();
				int offsetRow2 = 0;
				int offestColumn = 1;
				TableTag tableTag = (TableTag) nextNode;
				results = getResultsFromTable(assessmentHistoryLabels, offsetRow2, offestColumn, tableTag);

				String[] header = new String[] { PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getShortKeyName() };
				Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String, String>();
				resultBodyHeaderToSourceTableHeader.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getShortKeyName(), taxMarketValueKey);

				Class setName = PropertyAppraisalSet.class;
				// ResultBodyUtils.buildInfSet(resultMap, results, header,
				// resultBodyHeaderToSourceTableHeader, setName);

			}

			if (nextNode.toHtml().contains("Ownership History")) {
				List<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();
				int offsetRow2 = 0;
				int offestColumn = 1;
				TableTag tableTag = (TableTag) nextNode;
				results = getResultsFromTable(ownershipHistoryLabels, offsetRow2, offestColumn, tableTag);

				String[] header = new String[] { SaleDataSet.SaleDataSetKey.BOOK.getShortKeyName(),
						SaleDataSet.SaleDataSetKey.PAGE.getShortKeyName(), SaleDataSet.SaleDataSetKey.GRANTOR.getShortKeyName(),
						SaleDataSet.SaleDataSetKey.SALES_PRICE.getShortKeyName(),
						SaleDataSet.SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName() };

				Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String, String>();
				resultBodyHeaderToSourceTableHeader.put(SaleDataSet.SaleDataSetKey.BOOK.getShortKeyName(), deedBookKey);
				resultBodyHeaderToSourceTableHeader.put(SaleDataSet.SaleDataSetKey.PAGE.getShortKeyName(), deedPageKey);
				resultBodyHeaderToSourceTableHeader.put(SaleDataSet.SaleDataSetKey.GRANTOR.getShortKeyName(), ownerNameKey);
				resultBodyHeaderToSourceTableHeader.put(SaleDataSet.SaleDataSetKey.SALES_PRICE.getShortKeyName(), salePriceKey);
				resultBodyHeaderToSourceTableHeader.put(SaleDataSet.SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), deedDateKey);

				Class setName = SaleDataSet.class;
				ResultBodyUtils.buildInfSet(resultMap, results, header, resultBodyHeaderToSourceTableHeader, setName);

			}
		}
	}

	/**
	 * @param nodeByID
	 */
	public void putTaxHistory(ResultMap resultMap, Node nodeByID) {
		if (nodeByID instanceof TableTag) {
			List<HashMap<String, String>> list = HtmlParser3.getTableAsListMap(nodeByID.toHtml());
			String amountsPartialKey = "Base Amount";
			String assessmentPartialKey = "Assessment";
			String namePartialKey = "Name";
			String distKey = "Dist";

			List<HashMap<String, String>> taxHistoryResults = new ArrayList<HashMap<String, String>>();
			String regExSplit = "(?is)\\s+";

			for (HashMap<String, String> hashMap : list) {
				Set<Entry<String, String>> entrySet = hashMap.entrySet();
				HashMap<String, String> map = new HashMap<String, String>();
				for (Entry<String, String> entry : entrySet) {
					String key = entry.getKey();
					if (key.contains(amountsPartialKey)) {
						String value = entry.getValue();
						String[] split = value.split(regExSplit);
						if (split.length == 3) {
							map.put(TaxHistorySetKey.BASE_AMOUNT.getShortKeyName(), getAmountFromIndex(split, 0));
							map.put(TaxHistorySetKey.AMOUNT_PAID.getShortKeyName(), getAmountFromIndex(split, 1));
							map.put(TaxHistorySetKey.TOTAL_DUE.getShortKeyName(), getAmountFromIndex(split, 2));
						}
					}

					if (key.contains(assessmentPartialKey)) {
						String value = entry.getValue();
						String[] split = value.split(regExSplit);
						map.put(TaxHistorySetKey.DATE_PAID.getShortKeyName(), getAmountFromIndex(split, 1));
					}

					if (key.contains(namePartialKey)) {
						String value = entry.getValue();
						String taxYear = RegExUtils.getFirstMatch("(?is)\\b(\\d{4,4})\\b", value, 1);
						map.put(TaxHistorySetKey.YEAR.getShortKeyName(), taxYear);
					}

					if (key.contains(distKey)) {
						String value = entry.getValue();
						String[] status = value.split(regExSplit);
						if (status.length > 0 ){
							String s = status[status.length - 1];
							if (StringUtils.isNotEmpty(s)) {
								map.put("tmpStatus", s);
							}
						}
					}

				}
				taxHistoryResults.add(map);
			}

			
			Comparator<? super HashMap<String, String>> cDesc = new Comparator<HashMap>() {
				@Override
				public int compare(HashMap o1, HashMap o2) {
					String  year1 = (String) o1.get(TaxHistorySetKey.YEAR.getShortKeyName());
					String  year2 = (String) o2.get(TaxHistorySetKey.YEAR.getShortKeyName());
					int returnValue = -1;
					if ( NumberUtils.isNumber(year1) && NumberUtils.isNumber(year2)){
						returnValue = Integer.valueOf(year2).intValue() - Integer.valueOf(year1).intValue();
					}
					return returnValue;
				}
				
			};
			
			Comparator<? super HashMap<String, String>> cAsc = new Comparator<HashMap>() {
				@Override
				public int compare(HashMap o1, HashMap o2) {
					String  year1 = (String) o1.get(TaxHistorySetKey.YEAR.getShortKeyName());
					String  year2 = (String) o2.get(TaxHistorySetKey.YEAR.getShortKeyName());
					int returnValue = -1;
					if ( NumberUtils.isNumber(year1) && NumberUtils.isNumber(year2)){
						returnValue = (Integer.valueOf(year2).intValue() - Integer.valueOf(year1).intValue())*-1;
					}
					return returnValue;
				}
				
			};
			
			Collections.sort(taxHistoryResults, cAsc);
			
			int maxYear = 0;
			int currentYear = Integer.valueOf( (String) resultMap.get(TaxHistorySetKey.YEAR.getKeyName())).intValue();
			BigDecimal priorDelinquent = new BigDecimal(0.0);
			for (HashMap<String, String> hashMap : taxHistoryResults) {
				if (hashMap.containsKey("tmpStatus")) {
					String status = hashMap.get("tmpStatus");
					String taxYear = hashMap.get(TaxHistorySetKey.YEAR.getShortKeyName());
					if ("D".equals(status)) {
						String number = hashMap.get(TaxHistorySetKey.TOTAL_DUE.getShortKeyName());
						if (NumberUtils.isNumber(number) && NumberUtils.isNumber(taxYear)){
							int taxYearInt = Integer.valueOf(taxYear);
							if (currentYear-1!= taxYearInt ){
								priorDelinquent = priorDelinquent.add(new BigDecimal(number));
							}
							hashMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getShortKeyName(), ""+ priorDelinquent);
						}
					}
				}else{
					hashMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getShortKeyName(), "0");
				}
			}
			
			Collections.sort(taxHistoryResults, cDesc);
//			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), ""+ priorDelinquent);
			
			Class setName = TaxHistorySet.class;

			String[] header = new String[] { TaxHistorySetKey.BASE_AMOUNT.getShortKeyName(),
					TaxHistorySetKey.AMOUNT_PAID.getShortKeyName(), TaxHistorySetKey.TOTAL_DUE.getShortKeyName(),
					TaxHistorySetKey.DATE_PAID.getShortKeyName(), TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.PRIOR_DELINQUENT.getShortKeyName() };

			ResultBodyUtils.buildInfSet(resultMap, taxHistoryResults, header, setName);
		}

	}

	/**
	 * @param split
	 * @return
	 */
	public String getAmountFromIndex(String[] split, int index) {
		return (split.length >= (index + 1) ? ro.cst.tsearch.utils.StringUtils.cleanAmount((split[index])) : "");
	}

	/**
	 * @param assessmentHistoryLabels
	 * @param offsetRow2
	 * @param offestColumn
	 * @param tableTag
	 * @return TODO
	 */
	public List<HashMap<String, String>> getResultsFromTable(String[] assessmentHistoryLabels, int offsetRow2, int offestColumn,
			TableTag tableTag) {
		List<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();
		TableRow[] rows = tableTag.getRows();
		String rowSplitter = "Record";
		HashMap<String, String> map = new HashMap<String, String>();
		for (TableRow tableRow : rows) {
			for (String label : assessmentHistoryLabels) {

				String valueFromAbsoluteCell = HtmlParser3.getValueFromAbsoluteCell(offsetRow2, offestColumn,
						HtmlParser3.findNode(tableRow.getChildren(), label), "", true);
				if (StringUtils.isNotEmpty(valueFromAbsoluteCell)) {
					map.put(label, valueFromAbsoluteCell);
				}
			}

			TableRow lastRow = rows[rows.length - 1];
			if (tableRow.toHtml().contains(rowSplitter) || tableRow.equals(lastRow)) {
				if (map.size() > 0) {
					results.add(map);
				}
				map = new HashMap<String, String>();
			}
		}
		return results;
	}

	private String cleanName(String name){
		name = name.replaceAll("\\(LIFE-ESTATE\\)", "LIFE ESTATE");
		name = name.replaceAll("\\(LIFE EST\\)", "LIFE EST");
		return name;
	}
	
	@Override
	public void parseName(String name, ResultMap resultMap) {
		List body = new ArrayList<String>();
		if (StringUtils.isNotEmpty(name)) {
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, name, body);
		} else {
			String o1 = StringUtils.defaultIfEmpty((String) resultMap.get("tmpOwner1"), "");
			String o2 = StringUtils.defaultIfEmpty((String) resultMap.get("tmpOwner2"), "");
			
			o1 = cleanName(o1);
			o2 = cleanName(o2);
			
			String regEx = "^\\b\\w*\\b";
			String o1StartName = StringUtils.defaultIfEmpty(RegExUtils.getFirstMatch(regEx, o1, 0), "");
			String o2StartName =  StringUtils.defaultIfEmpty(RegExUtils.getFirstMatch(regEx, o2, 0), "");
				
			if (o1StartName.equals(o2StartName)){
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, o1, body);
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, o2, body);
			}else{
				String tmpOwnerName = (o1 + " " + o2).trim();
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, tmpOwnerName, body);
			}
			

			String o3 = StringUtils.defaultIfEmpty((String) resultMap.get("tmpOwner3"), "");
			if (StringUtils.isNotEmpty(o3)) {
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, o3, body);
			}
		}
	}

	@Override
	public void parseAddress(String address, ResultMap resultMap) {

		String streetName = StringFormats.StreetName(address).trim();
		streetName = ro.cst.tsearch.utils.StringUtils.cleanHtml(streetName);
		String streetNo = StringFormats.StreetNo(address);
		streetNo = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(streetNo);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(streetNo));

	}

}

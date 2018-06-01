package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.math.NumberUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class FLColumbiaTR extends FLGenericGovernmaxTR {

	private static final long serialVersionUID = 1L;
	
	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row) {
			String searchdat5 = getSearchdat5(row);
			if (searchdat5 != "")
				return searchdat5.matches("(?is).*\\w*\\-\\d*-\\d*.*");
			else
				return row.matches("(?is).*\\w*\\-\\d*-\\d*.*");
		}
	};

	public FLColumbiaTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		// table =
		// table.replaceAll("(?is)<TABLE WIDTH=100% BORDER=0 BORDERCOLOR=Black VALIGN=TOP CELLSPACING=0 CELLPADDING=3>",
		// "");

		String[] rows = table.split("<HR>");

		for (int i = 0; i < rows.length - 1; i++) {
			String row = rows[i];
			ParsedResponse currentResponse = new ParsedResponse();

			ResultMap resultMap = new ResultMap();
			ro.cst.tsearch.servers.functions.FLColumbiaTR.parseAndFillResultMap(resultMap, row);
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			try {
				document = (TaxDocumentI) bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row);
			currentResponse.setOnlyResponse(row);
			String link = RegExUtils.getFirstMatch("(?is)<a href=\"(.*?)\"", row, 1);
			currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

			intermediaryResponse.add(currentResponse);
		}

		response.getParsedResponse().setHeader("<TABLE WIDTH=100% BORDER=0 BORDERCOLOR=Black VALIGN=TOP CELLSPACING=0 CELLPADDING=3>");
		response.getParsedResponse().setFooter("</table>");
		response.getParsedResponse().setResultRows(intermediaryResponse);
		response.getParsedResponse().setOnlyResultRows(intermediaryResponse);
		response.getParsedResponse().setOnlyResponse(table);

		// response.getParsedResponse().setResponse(table);
		return intermediaryResponse;
	}

	private static final String eraseFontRegEx = "(?is)</?(font|b)[^>]*>";

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		HtmlParser3 parser = new HtmlParser3(detailsHtml);

		String accountNumber = HtmlParser3
				.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(parser.getNodeList(), "Account Number"), "", true)
				.replaceAll(eraseFontRegEx, "").trim();
		map.put("PropertyIdentificationSet.ParcelID", accountNumber);

		//String mailingAddressCellContents = HtmlParser3.getValueFromAbsoluteCell(0, 0,
				//HtmlParser3.findNode(parser.getNodeList(), "Mailing Address"), "", true);
		//List<String> mailingAddressCellFonts = RegExUtils.getMatches("(?is)<FONT\\s*.*?>(.*?)</FONT>", mailingAddressCellContents, 1);
		
		String owners = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(parser.getNodeList(), "Mailing Address"), "", true).trim();
		owners = owners.replaceAll("(?is)Mailing Address", "").replaceAll("(?is)\\A\\s*<br>", " ")
						.replaceAll("(?is)</?b>", "").replaceAll("(?is)</?font[^>]*>", "").trim();
		if (owners != null) {
			ro.cst.tsearch.servers.functions.FLColumbiaTR.parseName(map, owners);
		}
		
		/*if (mailingAddressCellFonts.size() >= 3) {
			String name = org.apache.commons.lang.StringUtils.defaultIfEmpty(mailingAddressCellFonts.get(2), "").trim();
			int i = 3;
			while (name.endsWith("&") && mailingAddressCellFonts.size() > i) {
				name += mailingAddressCellFonts.get(i++).trim();
			}
			if (name != null) {
				name = org.apache.commons.lang.StringUtils.strip(name.replaceAll(eraseFontRegEx, ""));
				ro.cst.tsearch.servers.functions.FLColumbiaTR.parseName(map, name);
			}
		}*/

		String propertyAddressCell = HtmlParser3.getValueFromAbsoluteCell(0, 0,
				HtmlParser3.findNode(parser.getNodeList(), "Property Address"), "", true);
		List<String> propertyAddressCellList = RegExUtils.getMatches("(?is)<FONT\\s*.*?>(.*?)</FONT>", propertyAddressCell, 1);

		if (propertyAddressCellList.size() == 6) {
			String address = StringUtils.cleanHtml(propertyAddressCellList.get(2));
			ro.cst.tsearch.servers.functions.FLColumbiaTR.parseAddress(map, address);

			String geoNumber = propertyAddressCellList.get(5);
			map.put("PropertyIdentificationSet.GeoNumber", geoNumber);
		}

		String legalDescriptionText = RegExUtils.getFirstMatch("(?s)Legal Description.*?</FONT>\\s*</TD>\\s*</TR>\\s*</TABLE>",
				detailsHtml, 0);
		List<String> legalDescriptions = RegExUtils.getMatches("(?is)<FONT\\s*.*?>(.*?)</FONT>", legalDescriptionText, 1);

		/*if (legalDescriptions.size() == 3) {
			String legal = "";
			if (legalDescriptions.get(2).contains("See Tax Roll For Extra Legal")) {
				legal = legalDescriptions.get(0);
				// check to see if the othe legal has something extra
				String charSeq = legal.substring(0, legal.length() / 10).trim();
				String secondLegal = legalDescriptions.get(2);
				if (secondLegal.contains(charSeq.trim())) {
					legal = secondLegal.substring(0, secondLegal.indexOf(charSeq)).trim() + " " + legal.trim();

				}
			} else {
				legal = legalDescriptions.get(2);
			}
			ro.cst.tsearch.servers.functions.FLColumbiaTR.parseLegalDescription(map, legal);
		}*/
		ro.cst.tsearch.servers.functions.FLColumbiaTR.parseLegalDescription(map, legalDescriptions.get(0));

		// tax history tables
		setTaxData(detailsHtml, map, parser);
		return null;
	}

	private void setTaxData(String detailsHtml, ResultMap map, HtmlParser3 parser) {
		NodeList nodeListByTypeAndAttribute = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "TABLE", "id", "paymentHistory", true);
		
		String priorYearsDue = extracted(parser, 0, 1, "Prior Years Due");
		String baseAmount = extracted(parser, 1, 0, "Taxes & Assessments");
		String amountDue = extracted(parser, 0, 1, "Amount Due");
		String amountPaid = extracted(parser, 0, 1, "Amount Paid");
		BigDecimal calculateAmountPaid = new BigDecimal(0);
		int j = 2;
		while (StringUtils.isNotEmpty(amountPaid) && NumberUtils.isNumber(amountPaid)) {
			calculateAmountPaid = calculateAmountPaid.add(new BigDecimal(amountPaid));
			amountPaid = extracted(parser, 0, j, "Amount Paid");
			j++;
		}
		amountPaid = calculateAmountPaid.toPlainString();
		String currentTaxYear = extracted(parser, 0, 1, "Tax Year");

		// extract amount due
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);

			TableTag amountDueTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "53%"), false).elementAt(1);

			TableRow[] rows = amountDueTable.getRows();
			for (TableRow row : rows) {
				if (row.toPlainTextString().indexOf("Amount Due") < 0 && row.toHtml().matches("(?is).*<b>.*</b>.*")) {
					amountDue = row.getColumns()[1].toPlainTextString().replaceAll("[$,]|&nbsp;", "").trim();
				}
			}

		} catch (Exception e) {
			logger.error("Error while parsing details on searchId: " + searchId, e);
		}

		boolean amountPaidForCurrentTaxYear = false;

		List<HashMap<String, String>> sourceSet = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < nodeListByTypeAndAttribute.size(); i++) {
			int colOffSet = 0;
			int rowOffSet = 1;
			HtmlParser3 tableParser = new HtmlParser3(nodeListByTypeAndAttribute.elementAt(i).toHtml());
			String labelToLookFor = "Year";
			String year = extracted(tableParser, colOffSet, rowOffSet, labelToLookFor);
			labelToLookFor = "Receipt";
			String receiptNumber = extracted(tableParser, colOffSet, rowOffSet, labelToLookFor);
			labelToLookFor = "Amount Billed";
			labelToLookFor = "Amount Paid";
			String amountPaidForCurrentYear = extracted(tableParser, colOffSet, rowOffSet, labelToLookFor);
			labelToLookFor = "Date Paid";
			String datePaid = extracted(tableParser, colOffSet, rowOffSet, labelToLookFor);

			HashMap<String, String> taxMap = new HashMap<String, String>();
			if (StringUtils.isNotEmpty(year) && StringUtils.isNotEmpty(currentTaxYear) && !amountPaidForCurrentTaxYear) {
				amountPaidForCurrentTaxYear = currentTaxYear.trim().equals(year.trim());
			}
			taxMap.put("Year", year);
			taxMap.put("ReceiptAmount", amountPaidForCurrentYear);
			taxMap.put("ReceiptDate", datePaid);
			taxMap.put("ReceiptNumber", receiptNumber);

			sourceSet.add(taxMap);
		}

		// get prior taxes due
		String priorTaxesTable = RegExUtils.getFirstMatch("(?is)Prior Year Taxes Due.*?(<TABLE.*?</TABLE>)", detailsHtml, 1);
		HashMap<String, HashMap<String, String>> delinquentSourceSet = new HashMap<String, HashMap<String, String>>();
		if (StringUtils.isNotEmpty(priorTaxesTable) && !priorTaxesTable.contains("NO DELINQUENT TAXES")) {
			int firstDelinquentYear = 0;
			int lastDelinquentYear = 0;
			boolean firstIteration = true;
			List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(priorTaxesTable);
			for (HashMap<String, String> hashMap : tableAsListMap) {
				Set<Entry<String, String>> entrySet = hashMap.entrySet();
				HashMap<String, String> taxMap = new HashMap<String, String>();
				int currentYear = 0;
				for (Entry<String, String> entry : entrySet) {
					String key = entry.getKey();
					key = StringUtils.cleanHtml(key).trim();
					if ("Year".equals(key) || "Amount".equals(key)) {
						String value = StringUtils.cleanHtml(entry.getValue());
						if ("Amount".equals(key)) {
							key = "CurrentYearDue";
						} else {
							if (org.apache.commons.lang.math.NumberUtils.isNumber(value)) {
								currentYear = Integer.valueOf(value);
								if (firstIteration) {
									firstDelinquentYear = currentYear;
									lastDelinquentYear = currentYear;
									firstIteration = false;
								} else {
									firstDelinquentYear = firstDelinquentYear > currentYear ? currentYear : firstDelinquentYear;
									lastDelinquentYear = lastDelinquentYear < currentYear ? currentYear : lastDelinquentYear;
								}
							}
						}
						taxMap.put(key, StringUtils.cleanHtml(value).replaceAll("(?is),|\\$", ""));
					}
				}
				delinquentSourceSet.put("" + currentYear, taxMap);
			}

			map.put("TaxHistorySet.BaseAmount", baseAmount);
			map.put("TaxHistorySet.Year", currentTaxYear);

			// calculate priorDelinquent for each year
			for (int i = firstDelinquentYear + 1; i <= lastDelinquentYear; i++) {
				HashMap<String, String> currentMap = delinquentSourceSet.get("" + i);
				HashMap<String, String> priorMap = delinquentSourceSet.get("" + (i - 1));
				if (priorMap != null) {
					String priorMapPriorDelinquent = priorMap.get("PriorDelinquent");
					String currentYearDuePriorMap = priorMap.get("CurrentYearDue");
					BigDecimal priorPriorDelinquent = new BigDecimal(0);
					BigDecimal currentYearDuePrior = new BigDecimal(0);
					if (NumberUtils.isNumber(priorMapPriorDelinquent)) {
						priorPriorDelinquent = priorPriorDelinquent.add(new BigDecimal(priorMapPriorDelinquent));
					}

					if (NumberUtils.isNumber(currentYearDuePriorMap)) {
						currentYearDuePrior = currentYearDuePrior.add(new BigDecimal(currentYearDuePriorMap));
					}
					currentMap.put("PriorDelinquent", priorPriorDelinquent.add(currentYearDuePrior).toPlainString());

					String currentYearDueMap = priorMap.get("CurrentYearDue");
					BigDecimal currentYearDue = new BigDecimal(0);

					if (NumberUtils.isNumber(currentYearDueMap)) {
						currentYearDue = currentYearDue.add(new BigDecimal(currentYearDueMap));
					}

					currentMap.put("TotalDue", priorPriorDelinquent.add(currentYearDue).toPlainString());
				}
			}

			if (delinquentSourceSet.size() > 0) {
				map.put("TaxHistorySet.BaseAmount", baseAmount);

				HashMap<String, String> currentYearDelinquency = delinquentSourceSet.get(currentTaxYear);
				if (currentYearDelinquency != null) {
					String priorDelinquent = org.apache.commons.lang.StringUtils.defaultIfEmpty(
							currentYearDelinquency.get("PriorDelinquent"), "");
					String crtYearDelinq = org.apache.commons.lang.StringUtils.defaultIfEmpty(currentYearDelinquency.get("CurrentYearDue"),
							"");
					map.put("TaxHistorySet.TotalDue", crtYearDelinq);
					map.put("TaxHistorySet.CurrentYearDue", crtYearDelinq);
					map.put("TaxHistorySet.PriorDelinquent", priorDelinquent);
					map.put("TaxHistorySet.AmountPaid", "0");
					map.put("TaxHistorySet.DatePaid", "");
					map.put("TaxHistorySet.ReceiptNumber", "");
				} else {
					if (NumberUtils.isNumber(priorYearsDue)) {// R11287-000
						map.put("TaxHistorySet.TotalDue", amountDue);
						// map.put("TaxHistorySet.CurrentYearDue",
						// crtYearDelinq);
						map.put("TaxHistorySet.PriorDelinquent", priorYearsDue);
						map.put("TaxHistorySet.AmountPaid", "0");
						map.put("TaxHistorySet.DatePaid", "");
						map.put("TaxHistorySet.ReceiptNumber", "");
					}
				}
			}
			// sourceSet.addAll(delinquentSourceSet.values());
		}

		if (StringUtils.isNotEmpty(amountDue)) {
			if (amountPaidForCurrentTaxYear == Boolean.FALSE) {
				amountPaid = "";
			}
			map.put("TaxHistorySet.TotalDue", amountDue);
			map.put("TaxHistorySet.AmountPaid", amountPaid);
			map.put("TaxHistorySet.BaseAmount", baseAmount);
			map.put("TaxHistorySet.Year", currentTaxYear);

			if(!StringUtils.isEmpty(amountPaid) && Double.valueOf(amountPaid) > 0) {
				HashMap<String, String> taxMap = new HashMap<String, String>();
				taxMap.put("Year", currentTaxYear);
				taxMap.put("AmountPaid", amountPaid);
				taxMap.put("DatePaid", "");
				taxMap.put("ReceiptNumber", "");
				taxMap.put("BaseAmount", baseAmount);
				sourceSet.add(taxMap);
			}
		}

		String[] header = { "Year", "AmountPaid", "DatePaid", "ReceiptNumber", "BaseAmount", "TotalDue", "CurrentYearDue",
				"PriorDelinquent", "ReceiptDate", "ReceiptAmount" };
		ResultBodyUtils.buildInfSet(map, sourceSet, header, TaxHistorySet.class);
	}

	private String extracted(HtmlParser3 parser, int colOffSet, int rowOffSet, String labelToLookFor) {
		String value = parser.getValueFromAbsoluteCell(colOffSet, rowOffSet, labelToLookFor).replaceAll(eraseFontRegEx, "");
		value = org.apache.commons.lang.StringUtils.defaultIfEmpty(value, "");
		value = StringUtils.cleanHtml(value).replaceAll("(?is),|\\$", "");
		return value;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module;
		FilterResponse rejectNonRealEstate = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstate.setThreshold(new BigDecimal("0.65"));

		// P1 : search by PIN - in case the user has input directly the TR PIN
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.setData(0, prepareAPN(searchId));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.addFilter(rejectNonRealEstate);
			modules.add(module);
		}

		// PX : search by GEO - from the PIN
		String geo = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		if (!StringUtils.isEmpty(geo)) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
			module.addFilter(rejectNonRealEstate);
			modules.add(module);
		}

		// PX : search by Address
		if (hasAddress()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREET_NO_NAME);
			module.addFilter(rejectNonRealEstate);
			module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			modules.add(module);
		}

		if (hasName()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));

			FilterResponse fr = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
			fr.setThreshold(new BigDecimal("0.65"));

			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(fr);

			GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT,
					searchId, module);
			nameFilter.setUseSynonymsForCandidates(false);
			module.addFilter(nameFilter);

			if (hasLegal()) {
				module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			}
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
					module, searchId, new String[] { "L F M;;", "L F;;", "L f;;" });

			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	private String prepareAPN(long searchId) {
		CurrentInstance ci = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = ci.getCrtSearchContext();
		String apn = search.getSa().getAtribute(SearchAttributes.LD_PARCELNO);

		String regEx = ro.cst.tsearch.servers.functions.FLColumbiaTR.ACCOUNT_NUMBER_REG_EX;
		if (RegExUtils.matches(regEx, apn)) {
			apn = RegExUtils.getFirstMatch(regEx, apn, 0);
			search.getSa().setAtribute(SearchAttributes.LD_PARCELNO2, apn);
		}

		return apn;
	}

}

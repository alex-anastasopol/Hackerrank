package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.Tidy;

public class OHDelawareTR {
	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] cols = row.getColumns();

		List<String> l = new ArrayList<String>();
		String owner = "";
		String parcel = "";
		String address = "";
		if (cols.length == 4 || cols.length == 3) {
			
			if ( additionalInfo == 2 || additionalInfo == 3) {
				owner = StringUtils.strip(cols[2].toPlainTextString());
				parcel = StringUtils.strip(cols[0].toPlainTextString());
				address = StringUtils.strip(cols[1].toPlainTextString());
			}
			else if(additionalInfo == 1)
			{
				owner = StringUtils.strip(cols[0].toPlainTextString());
				parcel = StringUtils.strip(cols[1].toPlainTextString());
				address = StringUtils.strip(cols[2].toPlainTextString());
			}

			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.defaultString(parcel));
			l.add(owner.replaceAll("\\sET AL(\\s|$)", " ETAL$1").replace("@2", ""));
			parseNames(m, l, "");
			parseAddress(m, address);
		}
		return m;
	}

	public static void parseNames(ResultMap m, List<String> all_names, String setName) {
		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			for (String n : all_names) {
				n = StringUtils.strip(n);
				n = n.replace("CO-TRUSTEES", "TRUSTEES");
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), StringUtils.strip(nameOnServer.toString()).replaceAll("\\&$", ""));
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		String[] addressTokens = addr.split("<br\\s*/?>");
		if (StringUtils.isEmpty(addr)) {
			return;
		}
		String streetNoAndName = addressTokens[0];
		String cityAndZip = "";
		if (addressTokens.length > 2) {
			cityAndZip = addressTokens[2].trim();
		}
		
		if (cityAndZip.equals("") && addressTokens.length > 1 && !addressTokens[1].trim().isEmpty()) {
			cityAndZip = addressTokens[1].trim();
		}
		
		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("<br\\s*/?>", " "));

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(streetNoAndName)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(streetNoAndName)));
		if (cityAndZip.length() > 0) {
			Pattern pat = Pattern.compile("(?is)^(.*?)\\s+([A-Z]{2})\\s+(\\d+)(?:-\\d+)?$");
			Matcher mat = pat.matcher(cityAndZip);
			if (mat.find()) {
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(),
						StringUtils.defaultString(mat.group(1)));
				m.put(PropertyIdentificationSetKey.STATE.getKeyName(),
						StringUtils.defaultString(mat.group(2)));
				m.put(PropertyIdentificationSetKey.ZIP.getKeyName(),
						StringUtils.defaultString(mat.group(3)));
			}
		}
	}

	public static void parseLegal(ResultMap m, String legal) {
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes);

		Pattern LEGAL_LOT = Pattern.compile("LOTS?\\s*[A-Z-]*(\\d+\\b\\s*([&,-]\\s*\\d+\\b\\s*)*)");
		Pattern LEGAL_TRACT = Pattern.compile("TRACT ?([\\d-]+)");
		Pattern LEGAL_BLDG = Pattern.compile("BLDG ?(\\w+)");
		Pattern LEGAL_PHASE = Pattern.compile("PHA?S?E? ?(\\w+)");
		Pattern LEGAL_SECTION = Pattern.compile("\\bSEC(?:TION)?\\s*(?:NO\\.?)?\\s*(\\w+\\b\\s*([&,]\\s*\\w+\\b\\s*)*)");
		Pattern LEGAL_SECTION2 = Pattern.compile("\\bS\\s*(\\d+\\b\\s*([&,]\\s*\\w+\\b\\s*)*)");
		Pattern LEGAL_UNIT = Pattern.compile("UNIT ?(\\w+)");

		Matcher matcher = LEGAL_LOT.matcher(legalDes);

		// get lot
		StringBuffer lots = new StringBuffer();
		while (matcher.find()) {
			String lot = matcher.group(1).replaceAll("\\s*,\\s*", " ");
			lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot).replaceAll("\\s+", " ");
			lots.append(StringUtils.strip(lot) + " ");
			legalDes = legalDes.replaceFirst(matcher.group(), " ").replaceAll("\\s+", " ");
		}

		if (!lots.toString().isEmpty()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), StringUtils.strip(lots.toString()));
		}

		// get tract
		matcher = LEGAL_TRACT.matcher(legalDes);
		if (matcher.find()) {
			String tract = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), StringUtils.strip(tract));
		}

		// get bldg
		matcher = LEGAL_BLDG.matcher(legalDes);
		if (matcher.find()) {
			String bldg = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), StringUtils.strip(bldg));
		}

		// get phase
		matcher = LEGAL_PHASE.matcher(legalDes);
		if (matcher.find()) {
			String phase = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), StringUtils.strip(phase));
		}

		// get section
		matcher = LEGAL_SECTION.matcher(legalDes);
		String sec = new String();
		if (matcher.find()) {
			sec = matcher.group(1).replaceAll("[&,]|\\s+", " ").trim();
		}
		else {
			// get section(short form) (e.g.: SUNBURY MILLS S3 - for PID 417-420-07-014-000)
			matcher = LEGAL_SECTION2.matcher(legalDes);
			if (matcher.find()) {
				sec = matcher.group(1).replaceAll("[&,]|\\s+", " ").trim();
			}
		}
		if (!sec.isEmpty()) {
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
		}
		
		// get unit
		matcher = LEGAL_UNIT.matcher(legalDes);
		if (matcher.find()) {
			String unit = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), StringUtils.strip(unit));
		}

		// get subdivision name
		try {
			legalDes = legalDes.replaceFirst("(?s)^\\s*[A-Z]*[-\\d+]+\\b", "");
			legalDes = legalDes.replaceFirst("(?s)(?:VILLAGE|VILLAS)\\s+AT\\s+.*", "");
			legalDes = legalDes.replaceAll("(?s)^[\\d\\s]+$", "");
			legalDes = legalDes.replaceAll("(?s)\\bP(?:AR)?T?(?:\\b|\\d+).*", "");
			legalDes = legalDes.replaceAll("(?s)LANDS?\\s+SURVEY.*", "");
			legalDes = legalDes.replaceAll("(?s)LEA\\s+AT", "");
			legalDes = legalDes.replaceAll("(?s)\\bCONDOS?\\b(?:\\s\\d+(?:TH|ST|RD)?\\s+(?:AMEND)?)?", "");
			legalDes = legalDes.replaceAll("(?s)\\bSUB(?:DIVISION)?\\s*$", "");
			legalDes = legalDes.replaceAll("(?s)\\bLANDS?[\\s\\d]+(?:PARCEL[\\s\\d]+)?", "");
			legalDes = legalDes.replaceAll("(?s)(?:NORTH|EAST|SOUTH|WEST)\\s+OF\\s+.*", "").trim();
			if (!legalDes.isEmpty()) {
				String[] legalTokens = legalDes.split("\\s+");
				int numberOfTokens = legalTokens.length;
				String romanNumber = legalTokens[numberOfTokens - 1];
				if (Roman.isRoman(romanNumber)) {
					String number = Roman.transformToArabic(romanNumber);
					legalDes = legalDes.replaceFirst(romanNumber, number);
				}
			}
			legalDes = legalDes.replaceFirst("\\d+\\s*$", "").trim();
			if (!legalDes.isEmpty()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legalDes);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(detailsHtml, null)).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			NodeList finalRes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "finalResults"));
			String deedNumber = "";
			String convNumber = "";
			String date = "";

			if (finalRes.size() > 0) {
				// parcel ID
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), getParcelNoFromHtml(nodes));
				
				// address
				String address = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(finalRes, "Property Address"), "", true);
				parseAddress(resultMap, address);
				
				//name in intermediaries
				String query = response.getQuerry();
				String ownerInIntermediaries = "";
				if (StringUtils.isNotEmpty(query) && query.contains("ownerNameIntermediary")) {
					ownerInIntermediaries = URLDecoder.decode(query.replaceFirst(".*?&ownerNameIntermediary=(.*?)&.*", "$1"), "UTF-8");
				}
				
				// names
				Node ownerNode = finalRes.extractAllNodesThatMatch(new RegexFilter("Owner\\s+Name"), true).elementAt(0);
				if (ownerNode != null) {

					TableColumn ownerColumn = (TableColumn) ownerNode.getParent().getParent().getLastChild().getPreviousSibling();
					String owner = ownerColumn.toPlainTextString();

					if (!ownerInIntermediaries.isEmpty() && !ownerInIntermediaries.trim().equals(owner.trim()) && !owner.contains("&")) {
						owner = owner.replaceFirst(ownerInIntermediaries, ownerInIntermediaries + " &");
					}

					List<String> l = new ArrayList<String>();
					l.add(owner.replaceAll("\\sET AL(\\s|$)", " ETAL$1").replace("@2", ""));
					parseNames(resultMap, l, "");
				}
				// legal
				Node legalNode = finalRes.extractAllNodesThatMatch(new HasAttributeFilter("id", "Summary"), true)
						.extractAllNodesThatMatch(new RegexFilter("Description"), true).elementAt(0);
				if (legalNode != null)
				{
					TableRow legal = (TableRow) legalNode.getParent().getParent().getNextSibling().getNextSibling();
					parseLegal(resultMap, legal.toPlainTextString());
				}
				// BA,AD,PD,AP
				Node taxes = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "ui-corner-all", new String[] { "Prior", "1st Half", "2nd Half" },
						true);
				String amountPaidString = "";
				
				if (taxes != null) {
					TableTag taxesTable = (TableTag) taxes;
					TableRow[] rows = taxesTable.getRows();
					double baseAmount = 0;
					double amountDue = 0;
					double priorDelinquent = 0;
					double amountPaid = 0;

					for (int i = 0; i < rows.length; i++)
					{
						TableRow r = rows[i];
						if (StringUtils.containsIgnoreCase(r.getColumns()[0].toPlainTextString(), "Net")) {
							String ba1 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							String ba2 = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							double ba1d = StringUtils.isEmpty(ba1) ? 0 : Double.parseDouble(ba1);
							double ba2d = StringUtils.isEmpty(ba2) ? 0 : Double.parseDouble(ba2);
							baseAmount = ba1d + ba2d;
						}
						if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Tax Paid")) {
							String ap1 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							String ap2 = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							double ap1d = StringUtils.isEmpty(ap1) ? 0 : Double.parseDouble(ap1);
							double ap2d = StringUtils.isEmpty(ap2) ? 0 : Double.parseDouble(ap2);

							amountPaid = ap1d + ap2d;
							amountPaidString = Double.toString(amountPaid).replaceAll("(\\d+\\.\\d{2})\\d+", "$1");
						}
						if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Tax Due")) {
							String ad1 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							String ad2 = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							double ad1d = StringUtils.isEmpty(ad1) ? 0 : Double.parseDouble(ad1);
							double ad2d = StringUtils.isEmpty(ad2) ? 0 : Double.parseDouble(ad2);

							amountDue = ad1d + ad2d;
						}
						if (StringUtils.containsIgnoreCase(r.getColumns()[0].toPlainTextString(), "Balance Due")) {
							String prior = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
							priorDelinquent = StringUtils.isEmpty(prior) ? 0 : Double.parseDouble(prior);
						}
					}
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), Double.toString(baseAmount).replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaidString);
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(amountDue).replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), Double.toString(priorDelinquent).replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
				}

				// get date paid
				if (StringUtils.isNotEmpty(amountPaidString)) {
					Node paymentInfoNode = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "", "",
							new String[] { "Date", "Half", "Prior", "1st Half", "2nd Half" }, true);
					if (paymentInfoNode != null) {
						TableRow[] rows = ((TableTag) paymentInfoNode).getRows();
						if (rows.length > 1) {
							TableColumn[] cols = rows[1].getColumns();
							if (cols.length > 0) {
								String datePaid = cols[0].toPlainTextString().replaceAll("\\s+", "");
								if (datePaid.matches("\\d{1,2}(?:/|-)\\d{1,2}(?:/|-)\\d{2}(?:\\d{2})?")) {
									resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), datePaid);
								}
							}
						}
					}
				}

				// land
				Node landImprTable = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "ui-corner-all",
						new String[] { "Assessment Info", "Current Value", "Recent Transfer" },
						true);
				if (landImprTable != null) {
					NodeList landImprList = landImprTable.getChildren();
					String land = StringUtils.strip(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(landImprList, "Mkt Land Value"), "", true)
							.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")).replaceAll("[\\s$,-]", "");

					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), StringUtils.defaultString(land));
					// improvements
					String improvements = StringUtils.strip(
							HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(landImprList, "Mkt Impr Value"), "",
									true).replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")).replaceAll("[\\s$,-]", "");

					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), StringUtils.defaultString(improvements));
					// total appraisal/assessment
					String totalAppraised = StringUtils.strip(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(landImprList, "Total"), "", true)
							.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1"));
					totalAppraised = totalAppraised.replaceAll("[\\s$,-]", "");

					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), StringUtils.defaultString(totalAppraised));
				}

				Node assessedTable = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "ui-corner-all",
						new String[] { "Full Rate", "Assessed Value" },
						true);
				if (assessedTable != null) {
					NodeList assessetTableList = assessedTable.getChildren();
					String totalAssessed = StringUtils.strip(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(assessetTableList, "Total"), "",
							true)
							.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1"));
					totalAssessed = totalAssessed.replaceAll("[\\s$,-]", "");
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), StringUtils.defaultString(totalAssessed));
				}
			}

			Node deedTable = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "ui-corner-all",
					new String[] { "Assessment Info", "Current Value", "Recent Transfer" },
					true);
			if (deedTable != null)
			{
				NodeList deedTableList = deedTable.getChildren();
				deedNumber = StringUtils.strip(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(deedTableList, "Deed #"), "", true)
						.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")).replace("&nbsp;", "").trim();

				convNumber = StringUtils.strip(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(deedTableList, "Conveyance"), "", true)
						.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")).replace("&nbsp;", "").trim();

				date = StringUtils.strip(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(deedTableList, "Sale Date"), "", true)
						.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")).replace("&nbsp;", "").trim();
			}
			// transfers
			Node transferList = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "ui-widget-content ui-table ui-corner-all", new String[] { "Date",
					"Sale Amount", "To", "Transfer Type", "Conveyance", "Deed" },
					true);
			if (transferList != null) {
				TableTag transfersT = (TableTag) transferList;
				TableRow[] transfersR = transfersT.getRows();

				String[] transferHeader = { SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName(),
						SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), SaleDataSetKey.GRANTOR.getShortKeyName() };
				List<List> transferBody = new ArrayList<List>();
				List transferRow = new ArrayList();
				ResultTable resT = new ResultTable();
				Map<String, String[]> transferMap = new HashMap<String, String[]>();
				for (String s : transferHeader)
				{
					transferMap.put(s, new String[] { s, "" });
				}

				for (int i = 1; i < transfersR.length; i++) {
					transferRow = new ArrayList<String>();
					TableRow r = transfersR[i];
					TableColumn[] cols = r.getColumns();
					if (cols.length == 7) {
						String conv = cols[4].toPlainTextString().replaceAll("\\s+", "").replace("&nbsp;", "").trim();
						if (StringUtils.isNotEmpty(conv) && !"0".equals(conv)) {
							transferRow.add(conv);
							transferRow.add(cols[1].toPlainTextString().replaceAll("[\\s$,-]", "").replace("&nbsp;", ""));
							transferRow.add(cols[0].toPlainTextString().replaceAll("\\s+", "").replace("&nbsp;", ""));
							transferRow.add(cols[2].toPlainTextString().replaceAll("\\s+", " ").replace("&nbsp;", ""));
							transferBody.add(transferRow);
						}
					}
				}
				if (StringUtils.isNotEmpty(convNumber) && !"0".equals(convNumber)) {
					transferRow = new ArrayList<String>();
					transferRow.add(convNumber);
					transferRow.add("");
					transferRow.add(date);
					transferRow.add("");
					transferBody.add(transferRow);
				}

				if (StringUtils.isNotEmpty(deedNumber) && !"0".equals(deedNumber)) {
					transferRow = new ArrayList<String>();
					transferRow.add(deedNumber);
					transferRow.add("");
					transferRow.add(date);
					transferRow.add("");
					transferBody.add(transferRow);
				}
				if (!transferBody.isEmpty()) {
					resT.setHead(transferHeader);
					resT.setMap(transferMap);
					resT.setBody(transferBody);
					resT.setReadOnly();
					resultMap.put("SaleDataSet", resT);
				}
			}

			// parse installments
			Node installments = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "ui-corner-all", new String[] { "Prior", "1st Half", "2nd Half" },
					true);

			if (installments != null)
			{
				Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
				TableTag installmentsTable = (TableTag) installments;
				TableRow[] installmentsRows = installmentsTable.getRows();
				List<List> installmentsBody = new ArrayList<List>();
				List installmentsRow = new ArrayList();
				ResultTable resultTable = new ResultTable();

				String[] installmentsHeader =
				{
						TaxInstallmentSetKey.HOMESTEAD_EXEMPTION.getShortKeyName(),
						TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
						TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
						TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
						TaxInstallmentSetKey.STATUS.getShortKeyName(),
						TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName()
				};

				for (String s : installmentsHeader)
				{
					installmentsMap.put(s, new String[] { s, "" });

				}

				for (int inst = 0; inst < 2; inst++)
				{
					int installmentColumn = (inst == 0) ? 3 : 5;
					installmentsRow = new ArrayList<String>();
					for (int i = 1; i < installmentsRows.length; i++) {

						TableRow r = installmentsRows[i];
						TableColumn[] cols = r.getColumns();
						if (cols.length == 7) {
							if (StringUtils.containsIgnoreCase(r.getColumns()[0].toPlainTextString(), "Homestead")) {
								String homesteadExemption = cols[installmentColumn].toPlainTextString().replaceAll("[\\s$,]", "").replace("&nbsp;", "").trim();
								installmentsRow.add(homesteadExemption);
							} else if (StringUtils.containsIgnoreCase(r.getColumns()[0].toPlainTextString(), "Net")) {
								String baseAmount = cols[installmentColumn].toPlainTextString().replaceAll("[\\s$,]", "").replace("&nbsp;", "").trim();
								installmentsRow.add(baseAmount);
							} else if (StringUtils.containsIgnoreCase(r.getColumns()[0].toPlainTextString(), "Penalty")) {
								String penalty = cols[installmentColumn+1].toPlainTextString().replaceAll("[\\s$,]", "").replace("&nbsp;", "").trim();
								installmentsRow.add(penalty);
							} else if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Tax Due")) {
								String due = cols[installmentColumn].toPlainTextString().replaceAll("[\\s$,]", "").replace("&nbsp;", "").trim();
								installmentsRow.add(due);
								if (Double.parseDouble(due.replaceAll("[ $,]", "")) == 0.0)
									installmentsRow.add("PAID");
								else if (Double.parseDouble(due.replaceAll("[ $,]", "")) > 0.0)
									installmentsRow.add("UNPAID");
							} else if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Tax Paid")) {
								String paid = cols[installmentColumn].toPlainTextString().replaceAll("[\\s$,]", "").replace("&nbsp;", "").trim();
								installmentsRow.add(paid);
							}

						}

					}
					if (installmentsRow.size() == 6) {
						installmentsBody.add(installmentsRow);
					}
				}
				if (!installmentsBody.isEmpty()) {
					resultTable.setHead(installmentsHeader);
					resultTable.setMap(installmentsMap);
					resultTable.setBody(installmentsBody);
					resultTable.setReadOnly();
					resultMap.put("TaxInstallmentSet", resultTable);
				}
			}

			Node n = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "specialAssessment", new String[] { "Prior", "1st Half", "2nd Half" },
					true);
			// compute assessment taxes from SpecialAssessmentSet
			if (n != null) {
				String[][] sa = new String[4][3];
				NodeList saTable = n.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "ui-widget-content ui-corner-all"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "720"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "695"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"), true)
						.extractAllNodesThatMatch(new TagNameFilter("table"), true);

				boolean sa0 = false;
				boolean sa1 = false;
				boolean sa2 = false;
				boolean sa3 = false;

				BigDecimal saTotalDelinquent = new BigDecimal(0);
				BigDecimal saPriorDelinquent = new BigDecimal(0);
				BigDecimal saTotalPaid = new BigDecimal(0);
				BigDecimal saTotalDue = new BigDecimal(0);
				BigDecimal saBaseAmount = new BigDecimal(0);

				for (int j = 0; j < saTable.size(); j++)
				{
					TableTag t = (TableTag) saTable.elementAt(j);

					TableRow[] rows = t.getRows();

					for (int i = 1; i < rows.length; i++) {
						TableRow r = rows[i];

						if (r.getColumnCount() == 7) {
							if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Charge")) {
								sa[0][0] = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
								sa[0][1] = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
								sa[0][2] = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
								sa0 = true;
							}
							if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Pen/Int")) {
								sa[1][0] = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								sa[1][1] = r.getColumns()[4].toPlainTextString().replaceAll("[ $,-]", "");
								sa[1][2] = r.getColumns()[6].toPlainTextString().replaceAll("[ $,-]", "");
								sa1 = true;
							}
							if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Paid")) {
								sa[2][0] = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
								sa[2][1] = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
								sa[2][2] = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
								sa2 = true;
							}
							if (r.getColumns()[0].toPlainTextString().trim().equalsIgnoreCase("Owed")) {
								sa[3][0] = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
								sa[3][1] = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
								sa[3][2] = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
								sa3 = true;
							}
						}
					}
					if (sa0 && sa1 && sa2 && sa3)
					{
						// prior delinquent
						saPriorDelinquent = saPriorDelinquent.add(new BigDecimal(sa[3][0]));

						// base amount
						saBaseAmount = saBaseAmount.add(new BigDecimal(sa[0][1])).add(new BigDecimal(sa[0][2]));

						// total paid
						saTotalPaid = saTotalPaid.add(new BigDecimal(sa[2][1])).add(new BigDecimal(sa[2][2]));

						// total due
						saTotalDue = saTotalDue.add(new BigDecimal(sa[3][1])).add(new BigDecimal(sa[3][2]));

						// total delinquent
						if (Double.parseDouble(sa[1][1]) != 0) {
							saTotalDelinquent = saTotalDelinquent.add(new BigDecimal(sa[3][1]));
						}
						if (Double.parseDouble(sa[1][2]) != 0) {
							saTotalDelinquent = saTotalDelinquent.add(new BigDecimal(sa[3][2]));
						}
						saTotalDelinquent = saTotalDelinquent.add(saPriorDelinquent);
					}
				}
//				String status = "";
//				if (saTotalDue.compareTo(new BigDecimal(0)) > 0)
//				{
//					status = "UNPAID";
//				} else {
//					status = "PAID";
//				}
				//SA installments:
				Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
				List<List> installmentsBody = new ArrayList<List>();
				List installmentsRow = new ArrayList();
				ResultTable resultTable = new ResultTable();
				String[] installmentsHeader = { "BaseAmount", "TotalDue", "AmountPaid", "PenaltyAmount", "Status" };

				for (String s : installmentsHeader)
				{
					installmentsMap.put(s, new String[] { s, "" });
				}
				
				for (int inst = 0; inst < 2; inst++)
				{
					installmentsRow = new ArrayList<String>();
					// baseAmount
					installmentsRow.add((sa[0][inst + 1]).toString());
					
					// amountDue
					installmentsRow.add((sa[3][inst + 1]).toString());
					
					// amountPaid
					installmentsRow.add((sa[2][inst + 1]).toString());
					
					// penalty
					installmentsRow.add((sa[1][inst + 1]).toString());
					
					
					String statusSA = "PAID";
					if (Double.parseDouble(sa[3][inst + 1]) > 0.0) {
						statusSA = "UNPAID";		
					}
					installmentsRow.add(statusSA);
					if (installmentsRow.size() == 5) {
						installmentsBody.add(installmentsRow);
					}
				}

				if (!installmentsBody.isEmpty()) {
					try {
						resultTable.setHead(installmentsHeader);
						resultTable.setMap(installmentsMap);
						resultTable.setBody(installmentsBody);
					} catch (Exception e) {
						e.printStackTrace();
					}
					resultTable.setReadOnly();
					resultMap.put("SpecialAssessmentSet", resultTable);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String description,
			boolean recursive) {
		NodeList returnList = null;

		if (!StringUtils.isEmpty(attributeName))
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
					new HasAttributeFilter(attributeName, attributeValue), recursive);
		else
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

		if (returnList != null)
			for (int i = 0; i < returnList.size(); i++)
				if (returnList.elementAt(i).toHtml().contains(description))
					return returnList.elementAt(i);

		return null;
	}

	public static String getParcelNoFromHtml(NodeList nodes) {
		String parcelNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "Property Number"), "", true);
		if (StringUtils.defaultString(parcelNo).isEmpty()) {
			parcelNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "Parcel Number"), "", true);
		}
		return StringUtils.defaultString(parcelNo).replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1").trim();
	}
	
}

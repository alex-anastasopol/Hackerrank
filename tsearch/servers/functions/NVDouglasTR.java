package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

public class NVDouglasTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {

		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] cols = row.getColumns();

		String name = cols[0].toPlainTextString().trim();
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				name);
		parseNames(resultMap, searchId);

		String streetNumber = cols[1].toPlainTextString()
				.replaceAll("&nbsp;", "").trim();
		if (!streetNumber.equals("0"))
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
					streetNumber);

		String streetName = cols[2].toPlainTextString().trim();
		if (!streetNumber.equals(""))
			resultMap.put(
					PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
					streetName);

		if (!streetNumber.equals("0") && !streetNumber.equals(""))
			resultMap
					.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER
							.getKeyName(), streetNumber + " " + streetName);

		String parcelID = cols[3].toPlainTextString().trim();
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
				parcelID);

		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {

		String owner = (String) resultMap
				.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			return;

		owner = owner.replaceAll("\\*", "");
		owner = owner.replaceAll("&\\s*&", "&");
		owner = owner.replaceAll("\\bET\\s+AL\\b", "ETAL");
		owner = owner.replaceAll("\\b(CO-)?T(RUS)?TEES?\\b", "TR");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, type, otherType;
		boolean hasMoreThanTwoNames = false;
		String[] names3 = { "", "", "", "", "", "" };
		String[] suffixes3 = { "", "" }, type3, otherType3;
		StringBuffer nameOnServerBuff = new StringBuffer();
		String ow12 = owner;
		String ow3 = "";

		int ampersandPosition1 = owner.indexOf("&");
		int ampersandPosition2 = -1;

		if (ampersandPosition1 != -1) {
			ampersandPosition2 = owner.indexOf("&", ampersandPosition1 + 1);
			if (ampersandPosition2 != -1) // if are more than two owners
			{
				hasMoreThanTwoNames = true;
				ow12 = owner.substring(0, ampersandPosition2 - 1).trim();
				ow3 = owner.substring(ampersandPosition2 + 1).trim();
			}
		}

		names = StringFormats.parseNameNashville(ow12, true);
		if (hasMoreThanTwoNames) {
			if (!NameUtils.isCompany(ow3) && !ow3.contains(","))
				ow3 = names[5] + ", " + ow3;
			names3 = StringFormats.parseNameNashville(ow3, true);
		}

		// if the spouse name is in FML format
		if (ow12.indexOf("&") != -1) {
			String spouse = ow12.substring(ow12.indexOf("&") + 1).trim();
			if (!spouse.trim().equals("") && !NameUtils.isCompany(spouse)
					&& !spouse.contains(",")) {
				String[] spouseNames = { "", "", "", "", "", "" };
				spouseNames = StringFormats.parseNameDesotoRO(spouse, true);
				if (LastNameUtils.isLastName(spouseNames[2])
						&& !FirstNameUtils.isFirstName(spouseNames[2])) {
					names[3] = spouseNames[0];
					names[4] = spouseNames[1];
					names[5] = spouseNames[2];
				}
			}
		}

		String middlenames[] = names[1].split(" "); // if a last name is put as
													// a middle name
		for (int j = 0; j < middlenames.length; j++)
			// example: SMITH, SANDRA L CASEY-
			if (LastNameUtils.isLastName(middlenames[j])
					&& !FirstNameUtils.isFirstName(middlenames[j])) {
				String separator = " ";
				if (middlenames[j].endsWith("-"))
					separator = "";
				names[2] = middlenames[j] + separator + names[2];
				middlenames[j] = "";
			}
		String middlename = "";
		for (int j = 0; j < middlenames.length; j++)
			middlename += " " + middlenames[j];
		middlename = middlename.trim();
		names[1] = middlename;

		if (!NameUtils.isCompany(ow12))
			suffixes = GenericFunctions.extractNameSuffixes(names);
		type = GenericFunctions.extractAllNamesType(names);
		otherType = GenericFunctions.extractAllNamesOtherType(names);
		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type,
				otherType, NameUtils.isCompany(names[2]),
				NameUtils.isCompany(names[5]), body);

		if (hasMoreThanTwoNames) {
			if (!NameUtils.isCompany(ow3))
				suffixes3 = GenericFunctions.extractNameSuffixes(names3);
			type3 = GenericFunctions.extractAllNamesType(names3);
			otherType3 = GenericFunctions.extractAllNamesOtherType(names3);
			GenericFunctions.addOwnerNames(names3, suffixes3[0], suffixes3[1],
					type3, otherType3, NameUtils.isCompany(names3[2]),
					NameUtils.isCompany(names3[5]), body);
		}
		nameOnServerBuff.append("/").append(owner);

		String nameOnServer = nameOnServerBuff.toString();
		nameOnServer = nameOnServer.replaceFirst("/", "");
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				nameOnServer);
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseTaxes(NodeList nodeList, ResultMap resultMap,
			long searchId) {

		String year = "";
		double baseAmount = 0.0;
		double totalDue = 0.0;
		double priorDelinquent = 0.0;
		double amountPaid = 0.0;
		double otherCurrentYearAmounts = 0.0;
		boolean isBaseAmount = false;
		boolean isPriorDelinquent = false;
		boolean isAmountPaid = false;

		NodeList yearList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("h2"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("align", "center"));
		if (yearList.size() != 0) {
			year = yearList.elementAt(0).toPlainTextString();
			Matcher ma = Pattern.compile(
					"(.*)Tax Summary For (\\d\\d\\d\\d) - (\\d\\d\\d\\d)")
					.matcher(year);
			if (ma.find())
				year = ma.group(2);
		}

		NodeList tableList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("table"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("id", "table1"));

		if (tableList.size() != 0) {
			TableTag taxes = (TableTag) tableList.elementAt(0);
			TableRow[] rows = taxes.getRows();

			totalDue = Double.parseDouble(rows[1].getColumns()[2]
					.toPlainTextString().replaceAll("[\\(\\),\\$]", ""));

			for (int i = 2; i < rows.length; i++) {
				String row = rows[i].toPlainTextString().replaceAll("&nbsp;",
						"");
				if (row.equals(""))
					continue;
				if (row.contains("Current Year Taxes")) {
					isBaseAmount = true;
					isPriorDelinquent = false;
					isAmountPaid = false;
				} else if (row
						.contains("Prior Installments and Other Amounts (if any)")) {
					isBaseAmount = false;
					isPriorDelinquent = true;
					isAmountPaid = false;
				} else if (row.contains("Current Year Payment Dates")) {
					isBaseAmount = false;
					isPriorDelinquent = false;
					isAmountPaid = true;
				} else if (row.contains("Other Current Year Amounts (if any)")) {
					isBaseAmount = false;
					isPriorDelinquent = false;
					isAmountPaid = false;
				} else if (row.contains("Penalty") || row.contains("Interest")
						|| row.contains("Maintenance")) {
					otherCurrentYearAmounts += Double.parseDouble(rows[i]
							.getColumns()[1].toPlainTextString().replaceAll(
							"[\\(\\),\\$]", ""));
				} else // row with a sum of money
				{
					if (isBaseAmount)
						baseAmount += Double
								.parseDouble(rows[i].getColumns()[1]
										.toPlainTextString().replaceAll(
												"[\\(\\),\\$]", ""));
					else if (isPriorDelinquent)
						priorDelinquent += Double.parseDouble(rows[i]
								.getColumns()[0].toPlainTextString()
								.replaceAll("[\\(\\),\\$]", ""));
					else if (isAmountPaid)
						amountPaid += Double
								.parseDouble(rows[i].getColumns()[1]
										.toPlainTextString().replaceAll(
												"[\\(\\),\\$]", ""));
				}
			}

			if (baseAmount + otherCurrentYearAmounts < amountPaid)
				amountPaid = baseAmount + otherCurrentYearAmounts;
			
			totalDue = baseAmount + otherCurrentYearAmounts - amountPaid;
			//priorDelinquent = priorDelinquent - otherCurrentYearAmounts;

			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					Double.toString(baseAmount));
			resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),
					Double.toString(totalDue));
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(),
					Double.toString(priorDelinquent));
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),
					Double.toString(amountPaid));

		}
	}
}

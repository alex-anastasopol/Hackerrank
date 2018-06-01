package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLGilchristTR {

	public static final String			INTERMEDIARY_YEAR_PATTERN			= "(?s)^(\\d{4})\\s*--\\s*";
	public static final String			INTERMEDIARY_ADDRESS_PATTERN		= "(?is)^\\s*((\\d+)\\b\\s*[NEWS]{0,2}\\b.*)";
	public static final String			REAL_ESTATE_ACCOUNT_TYPE_PATTERN	= "(?is)([\\dA-Z]{6})\\s*-\\s*([\\dA-Z]{8})\\s*-\\s*([\\dA-Z]{4})";
	public static final Pattern			INTERMEDIARY_RESULT_PATTERN			= Pattern.compile("(?is)</a>\\s*<br\\s*/?>(.*?<br\\s*/?>)"
																					+ "(.*?<br\\s*/?>)?(.*?<br\\s*/?>)?(.*?<br\\s*/?>)?"
																					+ "(.*?<br\\s*/?>)?(.*?<br\\s*/?>)?(.*?<br\\s*/?>)?");
	public static final String			BR_TAG_PATTERN						= "(?is)\\s*<br\\s*/?>\\s*";
	protected static String				NON_AMOUNT_CHARS_PATTERN			= "[^\\d.\\(\\)]+";
	protected static String				AMOUNT_NO_PARANTHESES_PATTERN		= "\\(([\\d.]+)\\)";
	protected static String				AMOUNT_PATTERN						= "-?\\d+(\\.\\d+)?";
	private static ArrayList<String>	CITIES								= new ArrayList<String>();
	
	static {
		CITIES.add("Bell");
		CITIES.add("Fanning Springs");
		CITIES.add("High Springs");
		CITIES.add("Jacksonville");
		CITIES.add("Trenton");
	}

	public static void parseIntermediaryRow(ResultMap resultMap, String row, long searchId) {
		try {
			String ownerName = "";
			String address = "";
			String legal = "";
			boolean isRealEstate = false;

			String rawAccountNumber = RegExUtils.getFirstMatch("(?is)<a[^>]*>\\s*(.*?)\\s*</a>", row, 1);
			String year = RegExUtils.getFirstMatch(INTERMEDIARY_YEAR_PATTERN, rawAccountNumber, 1);
			if (StringUtils.isNotEmpty(year)) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			}

			String accountNumber = rawAccountNumber.replaceFirst(INTERMEDIARY_YEAR_PATTERN, "");
			accountNumber = accountNumber.replaceFirst("\\s*-\\s*$", "");
			if (StringUtils.isNotEmpty(accountNumber)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber);
			}

			if (accountNumber.trim().matches(REAL_ESTATE_ACCOUNT_TYPE_PATTERN)) {
				isRealEstate = true;
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
			}

			Matcher rowMatcher = INTERMEDIARY_RESULT_PATTERN.matcher(row);

			if (rowMatcher.find()) {

				// get names
				String tmpGroup = org.apache.commons.lang.StringUtils.defaultString(rowMatcher.group(1)).trim();
				ownerName += tmpGroup + " ";
				tmpGroup = org.apache.commons.lang.StringUtils.defaultString(rowMatcher.group(2)).trim();
				if (RegExUtils.getFirstMatch(INTERMEDIARY_ADDRESS_PATTERN, tmpGroup, 1).isEmpty()
						&& RegExUtils.getFirstMatch("(?is)^(P\\.?O\\.?\\s+BOX)", tmpGroup, 1).isEmpty()) {
					ownerName += tmpGroup + " ";
				}

				if (StringUtils.isNotEmpty(ownerName)) {
					resultMap.put("tmpOwner", ownerName);
				}

				partyNamesFLGilchristTR(resultMap, searchId);
				if (isRealEstate) {

					int i = 8;
					while (i > 2) {
						// get legal info
						i--;
						tmpGroup = org.apache.commons.lang.StringUtils.defaultString(rowMatcher.group(i)).replaceFirst(BR_TAG_PATTERN, "").trim();
						if (!tmpGroup.isEmpty()) {
							if (RegExUtils.getFirstMatch("(?i)(\\bFOLIO\\s*-)", tmpGroup, 1).isEmpty()) {
								if (!RegExUtils.getFirstMatch(INTERMEDIARY_ADDRESS_PATTERN, tmpGroup, 1).isEmpty()) {
									// get address info
									address = RegExUtils.getFirstMatch(INTERMEDIARY_ADDRESS_PATTERN, tmpGroup, 1);

								} else if (RegExUtils.getFirstMatch("(?i)(\\bNO\\s+DATA\\b)", tmpGroup, 1).isEmpty()) {
									legal = tmpGroup;
								}
								break;
							}
						}
					}

					if (StringUtils.isNotEmpty(address)) {
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
						parseAddress(resultMap);
					}

					if (StringUtils.isNotEmpty(legal)) {
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
						parseLegalFLGilchristTR(resultMap, searchId);
					}
				}
			}
			resultMap.removeTempDef();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void parseAddress(ResultMap m) {

		String address = (String) m.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address)) {
			return;
		}

		String streetName = "";
		String streetNo = StringUtils.extractParameter(address, "^\\s*(\\d+)\\b\\s+");
		String city = "";
		String zip = "";

		Pattern pattern = Pattern.compile("(?is)([NESW]{1,2}\\s*)?\\b(C|S)R\\s+(\\d+([A-Z])?)\\b(?:\\s*&\\s*\\d+)?");
		Matcher matcher = pattern.matcher(address);
		if (matcher.find()) {// e.g SW CR, 334 & 313 TRENTON for PIN 021014-00000006-0000
			// or 7909 NE SR 47 HIGH SPRINGS for PIN 300716-00570000-0020
			streetName = org.apache.commons.lang.StringUtils.defaultString(matcher.group(1)) + (matcher.group(2).equalsIgnoreCase("C") ? "COUNTY " : "STATE") + " ROAD "
					+ matcher.group(3);
			address = address.replaceFirst(Pattern.quote(matcher.group(0)), "").replaceAll("\\s+", " ");
		}
		
		// get city
		for (String tmpCity : CITIES) {
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(address, tmpCity)) {
				city = tmpCity;
				address = address.replaceFirst("(?is),?\\s*" + tmpCity + "\\s*", "");
				break;
			}
		}

		// add comma to separate city from addr
		if (!address.contains(",")) {
			String[] addrTokens = address.split(" ");
			for (int j = 0; j < addrTokens.length; j++) {
				if (AddressAbrev.isStreetSufix(addrTokens[j])) {
					address = address.replaceFirst("(" + addrTokens[j] + ")", "$1,");
					break;
				}
			}
		}
		// get city and zip
		if (address.contains(",")) {
			String cityZipPattern = ",\\s*(.*)\\s*(\\d+(-\\d+))?\\s*$";
			if (StringUtils.isEmpty(city)) {
				city = RegExUtils.getFirstMatch(cityZipPattern, address, 1);
			}
			zip = RegExUtils.getFirstMatch(cityZipPattern, address, 2);
			if (StringUtils.isNotEmpty(zip)) {
				m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
			}
			address = address.replaceFirst("(.*?),.*", "$1");
		}

		if (StringUtils.isNotEmpty(city)) {
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
		}

		if (address.contains("/")) {// e.g. SE 77 LN/SE 73 TRENTON for PIN 111016-05510009-0010
			address = address.substring(0, address.indexOf("/"));
		}

		if (StringUtils.isNotEmpty(streetNo)) {
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
			address = address.replaceFirst("\\s*" + streetNo + "\\s*", "");
		}

		if (StringUtils.isEmpty(streetName)) {
			if (org.apache.commons.lang.StringUtils.isNotBlank(address)) {
				streetName = address;
			}
		}
		if (StringUtils.isNotEmpty(streetName)) {
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static void parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap m, long searchId, DataSite dataSite) {
		detailsHtml = detailsHtml.replaceAll("(?is)</?(b|font|em)>", "")
				.replaceAll("(?is)(</?t)h\\b", "$1d");// replace all table headers with table columns;
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			int currentTaxYearInt = dataSite.getCurrentTaxYear();

			// get parcel ID
			String parcelID = "";
			NodeList mainNodeChildren = null;
			Node pidNode = htmlParser3.getNodeById("dnn_ctr389_ModuleContent");
			if (pidNode != null) {
				mainNodeChildren = pidNode.getChildren();
				parcelID = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainNodeChildren, "Account"), "", true);
				parcelID = org.apache.commons.lang.StringUtils.defaultString(parcelID).trim();
				if (StringUtils.isNotEmpty(parcelID)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}

				// get property type
				String propType = "";
				propType = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainNodeChildren, "Property Type"), "", true);
				propType = org.apache.commons.lang.StringUtils.defaultString(propType).trim();
				if (StringUtils.isNotEmpty(propType)) {
					m.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), propType);
				}

				// get owners
				String owners = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainNodeChildren, "Mailing Address"), "", true);
				owners = org.apache.commons.lang.StringUtils.defaultString(owners).replaceFirst("(?i)\\s*Mailing Address\\s*:?\\s*", "")
						.replaceFirst("(?i)\\A" + BR_TAG_PATTERN, " ").trim();
				String[] ownerTokens = owners.split(BR_TAG_PATTERN);
				owners = "";
				for (int i = 0; i < ownerTokens.length; i++) {
					if (ownerTokens[i].matches(INTERMEDIARY_ADDRESS_PATTERN)) {
						break;
					}
					owners += ownerTokens[i] + "<br>";
				}

				m.put("tmpOwner", owners);

				// get address
				String situsAddress = "";
				situsAddress = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainNodeChildren, "SITUS:"), "", true);
				situsAddress = org.apache.commons.lang.StringUtils.defaultString(situsAddress).replaceFirst("(?i)\\s*\\bSITUS\\s*:?\\s*", "")
						.replaceAll(BR_TAG_PATTERN, " ").replaceAll("\\s+", " ").trim();
				m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), situsAddress);
			}

			// get geo number
			String geoNumber = "";
			geoNumber = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainNodeChildren, "Geo Number:"), "", true);
			geoNumber = org.apache.commons.lang.StringUtils.defaultString(geoNumber).replaceFirst("(?i)\\s*\\bGeo Number:\\s*:?\\s*", "")
					.replaceAll(BR_TAG_PATTERN, " ").trim();
			if (StringUtils.isNotEmpty(geoNumber)) {
				m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), StringFormats.StreetName(geoNumber));
			}

			// get year
			String taxYear = "";
			Node taxBillsTable = htmlParser3.getNodeById("dnn_ctr390_ModuleContent");
			if (taxBillsTable != null) {
				NodeList taxBills = taxBillsTable.getChildren();
				taxYear = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(taxBills, "Year"), "", true);
				taxYear = org.apache.commons.lang.StringUtils.defaultString(taxYear).trim();
				if (StringUtils.isNotEmpty(taxYear)) {
					m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
				}
			}

			// get legal description
			String legalDescription = "";
			legalDescription = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainNodeChildren, "Legal Description"), "", true);
			legalDescription = org.apache.commons.lang.StringUtils.defaultString(legalDescription).replaceAll(BR_TAG_PATTERN, " ").replaceAll("\\s+", " ")
					.replaceAll("(?is)[\n\t]+", " ");
			if (StringUtils.isNotEmpty(legalDescription)) {
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription);
			}

			// get base amount, amount due, amount paid, prior delinquent
			String amountDue = "";
			String baseAmount = "";
			String noPaymentDuePattern = "(?is)(^\\s*No\\s+payment(\\(?s\\)?)?\\s+due)";
			Node billsHistoryNode = htmlParser3.getNodeById("390");
			Node amountDueNode = htmlParser3.getNodeById("lxT477");
			Node delinquentAmountNode = htmlParser3.getNodeById("lxT471");
			Node receiptsNode = htmlParser3.getNodeById("lxT397");
			
			if (billsHistoryNode != null) {
				NodeList billNodeList = billsHistoryNode.getChildren();
				String firstTableRowYear = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(billNodeList, "Year"), "", true).trim();
				firstTableRowYear = org.apache.commons.lang.StringUtils.defaultString(firstTableRowYear).trim();
				
				int firstTableRowYearInt = Integer.parseInt(firstTableRowYear);
				if (firstTableRowYearInt == currentTaxYearInt || firstTableRowYearInt == currentTaxYearInt + 1) {
					baseAmount = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(billNodeList, "Net Tax"), "", true);
					baseAmount = org.apache.commons.lang.StringUtils.defaultString(baseAmount).replaceAll(NON_AMOUNT_CHARS_PATTERN, "");

					if (!baseAmount.isEmpty()) {
						if (baseAmount.contains("(")) {
							baseAmount = "-" + StringUtils.extractParameter(baseAmount, AMOUNT_NO_PARANTHESES_PATTERN);
						}
					}

					if (baseAmount.matches(AMOUNT_PATTERN)) {
						m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
					}
					amountDue = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(billNodeList, "Amount Due"), "", true);
					amountDue = org.apache.commons.lang.StringUtils.defaultString(amountDue).replaceAll(NON_AMOUNT_CHARS_PATTERN, "");

				}
			}

			if (amountDue.isEmpty()) {// if amount due still empty get it from the div on the right
				if (amountDueNode != null) {
					amountDue = amountDueNode.toPlainTextString();
				}
			}

			if (RegExUtils.getFirstMatch(noPaymentDuePattern, amountDue, 1).isEmpty()) {
				amountDue = amountDue.replaceAll(NON_AMOUNT_CHARS_PATTERN, "");
				if (amountDue.contains("(")) {
					amountDue = "-" + StringUtils.extractParameter(amountDue, AMOUNT_NO_PARANTHESES_PATTERN);
				}
			}
			if (amountDue.matches(AMOUNT_PATTERN)) {
				m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
			}

			// get prior delinquent
			if (delinquentAmountNode != null) {
				String priorDelinquent = delinquentAmountNode.toPlainTextString();
				if (RegExUtils.getFirstMatch(noPaymentDuePattern, priorDelinquent, 1).isEmpty()) {
					priorDelinquent = delinquentAmountNode.toPlainTextString().replaceAll(NON_AMOUNT_CHARS_PATTERN, "");
					if (priorDelinquent.contains("(")) {
						priorDelinquent = "-" + StringUtils.extractParameter(priorDelinquent, AMOUNT_NO_PARANTHESES_PATTERN);
					}
				}
				if (priorDelinquent.matches(AMOUNT_PATTERN)) {
					m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
				}
			}

			Node currentYearBillNode = htmlParser3.getNodeById("currentBillDetails");
			if (currentYearBillNode != null) {
				String currentyearBillHtml = currentYearBillNode.toHtml().replaceFirst("(?is)(>)\\s*(Paid\\s*<)", "$1Amount $2");
				htmlParser3 = new HtmlParser3(currentyearBillHtml);

				// get amount paid
				Node paymentNode = htmlParser3.getNodeById("dnn_ctr397_ContentPane");
				if (paymentNode != null) {
					NodeList paymentNodes = paymentNode.getChildren();
					String amountPaid = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(paymentNodes, "Amount Paid"), "", true);
					amountPaid = org.apache.commons.lang.StringUtils.defaultString(amountPaid).replaceAll(NON_AMOUNT_CHARS_PATTERN, "");
					if (amountPaid.contains("(")) {
						amountPaid = "-" + StringUtils.extractParameter(amountPaid, AMOUNT_NO_PARANTHESES_PATTERN);
					}
					if (amountPaid.matches(AMOUNT_PATTERN)) {
						m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
					}
				}
			}
			
			if (receiptsNode != null) {
				List<List> bodyTaxes = new ArrayList<List>();
				List<String> line = null;

				NodeList paymentTablesList = receiptsNode.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (paymentTablesList != null) {
					for (int i = 0; i < paymentTablesList.size(); i++) {
						List<List<String>> taxTable = HtmlParser3.getTableAsList(paymentTablesList.elementAt(i).toHtml(), false);
						if (taxTable.size() > 0) {
							List lst = taxTable.get(0);
							line = new ArrayList<String>();
							if (lst.size() > 4) {
								line.add(lst.get(0).toString().trim());
								line.add(lst.get(1).toString().trim());
								line.add(lst.get(2).toString().trim());
								line.add(lst.get(4).toString().replaceAll("[\\$,]", "").trim());
								bodyTaxes.add(line);
							}
						}
					}

					if (bodyTaxes != null) {
						if (!bodyTaxes.isEmpty()) {
							ResultTable rt = new ResultTable();
							String[] header = { "ReceiptDate", "Year", "ReceiptNumber", "ReceiptAmount" };
							rt = GenericFunctions2.createResultTable(bodyTaxes, header);
							m.put("TaxHistorySet", rt);
						}
					}
				}
			}
			parseAddress(m);
			partyNamesFLGilchristTR(m, searchId);
			parseLegalFLGilchristTR(m, searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesFLGilchristTR(ResultMap resultMap, long searchId) throws Exception {

		String owner = (String) resultMap.get("tmpOwner");

		if (StringUtils.isEmpty(owner)) {
			return;
		}

		String[] ownerRows = owner.split(BR_TAG_PATTERN);
		String stringOwner = "";
		for (String row : ownerRows) {
			if (row.trim().toLowerCase().contains("box")) {
				break;
			} else if (row.trim().toLowerCase().startsWith("pmb")) {
				break;
			} else {
				stringOwner += row.trim() + "<br>";
			}
		}

		stringOwner = stringOwner.replaceAll("(?i)\\bWI?FE?\\b", " ");
		stringOwner = stringOwner.replaceAll("(?i)\\bMRS\\b", "&");
		stringOwner = stringOwner.replaceAll("(?i)[\\(\\)]+", "");
		stringOwner = stringOwner.replaceAll("(?i)\\bOR\\b", "&");
		stringOwner = stringOwner.replaceAll("(?i)\\bET\\s*(AL|UX|VIR)\\b", "ET$1");
		stringOwner = stringOwner.replaceAll("(?i)\\bSUITE\\s+\\d+", "");
		stringOwner = stringOwner.replaceAll("(?i)\\bAS\\s+(TRUSTEES?)\\b", "$1");
		stringOwner = stringOwner.replaceAll("%", "<br>");
		stringOwner = stringOwner.replaceFirst("(?i)(&(?:\\s*[A-Z]+)?)" + BR_TAG_PATTERN + "([A-Z]+(?:\\s+[A-Z])?)\\s*$", "$1 $2");
		stringOwner = stringOwner.replaceAll("(?i)(&\\s*[A-Z]+)" + BR_TAG_PATTERN + "([A-Z]+)\\s*&\\s*", " $1 $2 <br>");
		stringOwner = stringOwner.replaceFirst("(?i)&\\s*([A-Z]+)" + BR_TAG_PATTERN + "([A-Z]+\\s+TRUSTEES?)\\s*$", "& $1 $2");
		stringOwner = stringOwner.replaceFirst("(?i)" + BR_TAG_PATTERN + "$", "");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;
		String ln = "";
		String[] owners = stringOwner.split("[^&]\\s*" + BR_TAG_PATTERN);

		boolean has2Owners = false;
		for (int i = 0; i < owners.length; i++) {
			owners[i] = owners[i].replaceAll("\\A\\s*(&|C/O|ATT:|DBA(\\s*/|\\s*:))", "").replaceAll(BR_TAG_PATTERN, "");
			names = StringFormats.parseNameNashville(owners[i], true);
			if (i == 0 && StringUtils.isNotEmpty(names[5].trim())) {
				has2Owners = true;
			}

			if (i > 0) {
				names = StringFormats.parseNameDesotoRO(owners[i], true);
				if (has2Owners && LastNameUtils.isNotLastName(names[2])) {
					names = StringFormats.parseNameNashville(owners[i], true);
				}
				// 031014-00000008-0010
				if (FirstNameUtils.isFirstName(names[0]) && FirstNameUtils.isFirstName(names[2]) && StringUtils.isEmpty(names[1])) {
					names[1] = names[2];
					names[2] = ln;
				}
			}

			ln = names[2];
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);

		stringOwner = stringOwner.replaceAll(BR_TAG_PATTERN, " ").replaceAll("\\b(C/O|ATT:|DBA\\b(\\s*/|\\s*:))", " ").replaceAll("\\s+", " ");
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);

		String[] a = StringFormats.parseNameNashville(stringOwner, true);
		if (org.apache.commons.lang.StringUtils.isNotBlank(a[0])) {
			resultMap.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(a[1])) {
			resultMap.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(a[2])) {
			resultMap.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(a[3])) {
			resultMap.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(a[4])) {
			resultMap.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(a[5])) {
			resultMap.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void parseLegalFLGilchristTR(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal)) {
			return;
		}

		String city = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.CITY.getKeyName()));
		String owners = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpOwner"));
		String[] owner = owners.split(BR_TAG_PATTERN);

		legal = legal.replaceAll("(?is)\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)\\b(?:UNREC\\s+)?(?:RE)?PLAT\\s+OF[a-z\\s&.,-]+\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:" + city + ")?\\s*\\b(ORIG(?:INAL)?\\s+SURVEY|(?:&\\s*|ALSO\\s*)?VACAT(?:ED|ING)(\\s+ALL?E?Y)?|ALLEY\\s+VACAT(?:ED|ING))\\b",
				"");
		legal = legal.replaceFirst("\\s*(ALSO\\s+)?\\bA\\s+STRIP\\s+OF\\s+[\\d.,]*\\s*FT\\b", "");
		legal = legal.replaceAll("(?is)[NWES]{1,2}\\s+[\\d.]+\\s+(DEG|FT)\\b", "");// e.g. 350714-00000023-0020 : so that the lot is not parsed wrong: "N 3"
		// legal = legal.replaceAll("(?is)\\b(COM\\s+AT|[NEWS]{1,2}(/\\w)?(//s+OF)?|LN)\\b", "");
		
		String specialSubdivPattern = "(?is)(K\\s*K\\s*L\\s*&\\s*S(\\s+ESTATES))";
		if (!RegExUtils.getFirstMatch(specialSubdivPattern, legal, 1).isEmpty()) {
			legal = legal.replaceFirst(specialSubdivPattern, "KKL & S$2");// pin 300716-00570000-0020
		}
		
		for (int i = 0; i < owner.length; i++) {// e.g. 2013 -- 161015-00440000-0040 contains name in legal desc
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(legal, owner[i])) {
				legal = legal.replaceFirst(owner[i], "");
			}
		}

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		String legalTemp = legal;

		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d,\\s&]+[A-Z]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}

		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?)\\s*([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UN?I?T)\\s*([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), "UNIT ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);// ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section from legal description
		p = Pattern.compile("(?is)\\bTR(?:ACT)?\\s+(\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(1));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract STR
		p = Pattern.compile("(?is)\\s+(\\d{1,2})\\s*-\\s*(\\d{1,2}\\s*[A-Z]?)\\s*-\\s*(\\d{1,2}\\s*[A-Z]?)\\s+");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(1));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3));
			legalTemp = legalTemp.replaceFirst(ma.group(0), "SEC");
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract Section
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("\\b(PB)\\s*(\\d+)\\s*[,|/]?\\s*(PG?S?)?\\s*([\\d-&\\s]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = pb + " " + ma.group(2);
			pg = pg + " " + ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb.trim());
			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg.trim());
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\s+(\\d+)\\s*/\\s*(\\d+(?:\\s*(&|-)\\s*\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String book = ma.group(1).trim();
			String page = ma.group(2).trim();
			String[] pagesFromInterval = StringFormats.ReplaceIntervalWithEnumeration(page).split(" ");
			if (pagesFromInterval.length > 1 || page.contains("&")) {
				String[] pages = new String[20];
				if (pagesFromInterval.length > 1) {
					pages = pagesFromInterval;
				} else {
					pages = page.split("&");
				}

				for (String eachPage : pages) {
					List<String> line = new ArrayList<String>();
					line.add(book);
					line.add(eachPage.trim());
					line.add("");
					bodyCR.add(line);
				}
			} else {
				List<String> line = new ArrayList<String>();
				line.add(book);
				line.add(page);
				line.add("");
				bodyCR.add(line);
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), "");
		}
		legal = legalTemp;

		p = Pattern.compile("(?is)\\bOR\\s*(\\d+)\\s*PG\\s*(\\d+(?:\\s*[&|-]\\s*\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String book = ma.group(1).trim();
			String page = ma.group(2).trim();
			if (page.contains("&")) {
				String[] pages = page.split("&");
				for (String eachPage : pages) {
					List<String> line = new ArrayList<String>();
					line.add(book);
					line.add(eachPage.trim());
					line.add("");
					bodyCR.add(line);
				}
			} else {
				List<String> line = new ArrayList<String>();
				line.add(book);
				line.add(page);
				line.add("");
				bodyCR.add(line);
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), "");
		}
		legal = legalTemp;

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(?:UNIT)\\s+(.*?)(SUBD\\b|$)");
		ma = p.matcher(legal.trim());
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\b(?:BLO?C?KS?)\\s+(.*?)(SUBD\\b|$)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\b(?:LOTS?)\\s+(.*?)(SUBD\\b|$)");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		if (subdiv.length() != 0) {
			//e.g. 171015-00490013-0020, 161015-00480012-0003
			subdiv = subdiv.replaceAll("(?is)\\b(OR|(TAX\\s*)?DEED|(DB\\s*)+|TRIANGULAR(IN\\s*SHAPE)?|M/L|R/W|ORDER(\\s*OF)?|SUMMARY|"
					+ "ADMINISTRATION|CASE|#CP\\s*\\d+|UNREC|(1ST|2ND|3RD|4TH)?\\s*ADDIT(ION)?(\\s*TO|\\s*\\d+)?|(ORIGINAL)?(\\s*SUR)?)\\b", "");

			// e.g. 170914-00000015-0010
			subdiv = subdiv.replaceAll("\\b((PART)?OF(SEC)?(GO?)|SEC|GO(\\s*(TO|[NEWS]{1,2}))?|FT(\\s+GO)?|LESS(\\s+TINY)?|LOT(\\s+FOR)?(\\s+ACCESS)?"
					+ "(\\s*TO(\\s+MCCOOK))?|POB|PARCEL(\\s+\\w)?|(ALSO\\s+)?(THAT\\s+)?PART(\\s+OF)?(\\s+ABANDON(\\s+SUN)?(\\s+SPGS)?)?|RW)\\b", "");

			subdiv = subdiv.replaceAll("\\b(BOOK\\s*\\d*/?|PG\\s*\\d*)\\b","");
			subdiv = subdiv.replaceAll("(?is)\\s+DESC\\b.*", "");
			subdiv = subdiv.replaceAll("(?is)&?\\s*(\\d+)?\\s*(/|-)\\s*\\d*\\s*", "");
			subdiv = subdiv.replaceAll("(?is)(\\d+\\s*)[\\d+.]+\\s+(FT\\s*\\d*)?", "");
			subdiv = subdiv.replaceFirst("\\s*\\d{2,}(\\.\\d+)?\\s*$", "");
			
			for (String tmpCity : CITIES) {
				if (subdiv.matches("(?is)\\s*" + tmpCity + "\\s*")) {
					subdiv = subdiv.replaceFirst("(?is),?\\s*" + tmpCity + "\\s*", "");
					break;
				}
			}

			if (org.apache.commons.lang.StringUtils.isNotBlank(subdiv)) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
			}
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv.trim());
		}

		String parcelID = (String) m.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());

		if (StringUtils.isNotEmpty(parcelID)) {
			parcelID = parcelID.replaceAll("-", "").trim();
			if (parcelID.length() == 18) {
				String sec = parcelID.substring(0, 2);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec.trim().replaceFirst("\\A0+", ""));
				String twn = parcelID.substring(2, 4);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twn.trim().replaceFirst("\\A0+", ""));
				String rng = parcelID.substring(4, 6);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng.trim().replaceFirst("\\A0+", ""));
			}
		}
	}

}

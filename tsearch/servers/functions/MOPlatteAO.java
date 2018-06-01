package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.reports.comparators.IntegerComparator;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

public class MOPlatteAO {

	private static Map<String, String> siteKeyValuesToAtsKeyValues = new HashMap<String, String>();
	static {
		siteKeyValuesToAtsKeyValues.put("Parcel Number", "PropertyIdentificationSet.ParcelID");
		siteKeyValuesToAtsKeyValues.put("Owner Name", "PropertyIdentificationSet.NameOnServer");
		siteKeyValuesToAtsKeyValues.put("Location", "PropertyIdentificationSet.AddressOnServer");
		siteKeyValuesToAtsKeyValues.put("Subdivision", "PropertyIdentificationSet.SubdivisionName");
		siteKeyValuesToAtsKeyValues.put("Lot/Tract", "PropertyIdentificationSet.LegalDescriptionOnServer");
	}

	public static void parseAndFillResultMap(ResultMap resultMap, HashMap<String, String> tableRowData) {
		Set<String> keySet = tableRowData.keySet();
		for (String key : keySet) {
			String resultMapKey = siteKeyValuesToAtsKeyValues.get(StringUtils.trim(key));
			if (StringUtils.isNotEmpty(resultMapKey)) {
				resultMap.put(resultMapKey, StringUtils.trim(tableRowData.get(key)));
			}
		}
		setName(resultMap, (String) resultMap.get("PropertyIdentificationSet.NameOnServer"));
		setAddress(resultMap, (String) resultMap.get("PropertyIdentificationSet.AddressOnServer"));

		setIntermediaryLegal(resultMap, (String) resultMap.get("PropertyIdentificationSet.LegalDescriptionOnServer"));

	}

	public static void setName(ResultMap resultMap, String tmpOwnerName) {
		String[] ownerName = { "", "", "", "", "", "" };

		tmpOwnerName = GenericFunctions2.cleanOwnerNameFromPrefix(tmpOwnerName);
		tmpOwnerName = tmpOwnerName.replaceAll("/", " & ");
		tmpOwnerName = tmpOwnerName.replaceAll(", INC", " INC");
		ArrayList body = new ArrayList<List>();
		String[] split = tmpOwnerName.split("&");
		if (split.length > 2) {// BALL, BARTLETT F & GLORIA A & ETHAN J & SASHA
								// R
			String firstName = "";
			for (int i = 0; i < split.length; i++) {
				if (split[i].contains(",")) {// isolate the firstName
					firstName = split[i].split(",")[0];
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[i], body);
				} else {
					String string = split[i];
					if (RegExUtils.matches("\\w+\\s?\\w?", string)) {// FirstName
																		// MiddleInitial
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, firstName + "," + string, body);
					}
				}
			}
		} else {

			if (split.length == 2 && split[1].contains(",") && !NameUtils.isCompany(tmpOwnerName)) {
				for (String string : split) {
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, string, body);
				}
			} else {
				if (!NameUtils.isCompany(tmpOwnerName)) {
					if (split.length == 2 && RegExUtils.matches("\\w+\\s+\\w{2,}", split[1])) {// KRIER,
																								// KEVIN
																								// W
																								// &
																								// STEPHANIE
																								// HIGGENS
						String[] strings = split[1].trim().split("\\s");
						if (LastNameUtils.isLastName(strings[1].trim())) {
							ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[0], body);
							ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap,split[1], body);
							//ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[1], body);
						} else {
							ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, tmpOwnerName, body);
						}
					} else {
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, tmpOwnerName, body);
					}
				} else {
					parseCompanyName(resultMap, tmpOwnerName, body);
				}
			}
		}
	}

	private static void parseCompanyName(ResultMap resultMap, String tmpOwnerName, List body) {
		String[] parsedName = new String[] { "", "", "", "", "", "" };
		parsedName[2] = tmpOwnerName;
		String[] suffixes = GenericFunctions.extractNameSuffixes(parsedName);
		GenericFunctions.addOwnerNames(parsedName, suffixes[0], suffixes[1], NameUtils.isCompany(parsedName[2]),
				NameUtils.isCompany(parsedName[5]), body);

		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setIntermediaryLegal(ResultMap resultMap, String legalOnServer) {
		String[] split = legalOnServer.split("/");
		String lotToParse = "";
		String tract = "";
		if (split.length >= 2) {
			lotToParse = split[split.length - 2].trim();
			tract = split[split.length - 1].trim();
		} else {
			lotToParse = legalOnServer;
		}

		setLot(resultMap, lotToParse);
		setTract(resultMap, tract);
	}

	public static void setDetailsLegal(ResultMap resultMap, String legalOnServer) {
		legalOnServer = setBlock(resultMap, legalOnServer);
		legalOnServer = setUnit(resultMap, legalOnServer);
		legalOnServer = setSection(resultMap, legalOnServer);
	}

	/*
	 * public static void setLot(ResultMap resultMap, String legalDescription,
	 * Map<String, String> map) { String[] lotIndividual =
	 * ro.cst.tsearch.utils.StringUtils
	 * .findRepeatedOcurrenceByRegExMarker(legalDescription, "LOT,?",
	 * "(?is)\\bLOT (\\d*)", 1); String regExLots = "(?is)LOTS ([\\d+\\s]+)";
	 * String result =
	 * ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("",
	 * lotIndividual); if (legalDescription.contains("LOTS")) { // clean //
	 * replace 3,4,5, with 3 4 5 legalDescription =
	 * legalDescription.replaceAll("(\\d)(,)", "$1 "); String values =
	 * ro.cst.tsearch.utils.StringUtils.getSuccessionOFValues(legalDescription,
	 * "LOTS", regExLots); String[] lotSuccession = values.split(" "); result =
	 * ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString(result,
	 * lotSuccession); String lotIntervalDelimiters = "(THRU|TO)"; String
	 * lotInterval =
	 * ro.cst.tsearch.utils.StringUtils.parseByRegEx(legalDescription,
	 * "(?is)LOTS\\s*(\\d+\\s" + lotIntervalDelimiters + "\\s*?\\d+)", 1);
	 * lotInterval = lotInterval.replaceAll(lotIntervalDelimiters, "-"); result
	 * = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString(result,
	 * new String[] { lotInterval }); } result =
	 * LegalDescription.cleanValues(result, true, true); String keyPrefix =
	 * "PropertyIdentificationSet."; // addValueToMaps(keyPrefix,
	 * "SubdivisionLotNumber", resultMap, map, // result); //
	 * resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", //
	 * result); }
	 */
	public static String setDetailsLot(ResultMap resultMap, String tmpLegalDescription) {

		int lotIndex = tmpLegalDescription.indexOf("LOTS");
		lotIndex = (lotIndex == -1) ? tmpLegalDescription.indexOf("LTS") : lotIndex;
		// cleaning
		tmpLegalDescription = tmpLegalDescription.replaceAll(";", " ").replaceAll("-", " ").replaceAll(",", " ");

		// clean values like 6.5 which can be mistaken by the subsequent parse:
		// 6.5 FT S PT OF LOT 51 & ALL LOT 52 & 6.5 FT S PT OF LOT 36 & L S LO T
		// 35 BLK 56
		tmpLegalDescription = tmpLegalDescription.replaceAll("\\d+\\.\\d+\\s+FT", "");
		StringBuilder lot = new StringBuilder("");
		// LOT 51 & 30
		tmpLegalDescription = tmpLegalDescription.replaceAll("(LOT \\d+\\s+)(&)(\\s+\\d+\\b)", "$1 LOT$3");
		// if (lotIndex > -1) {// LOTS 1,23 & 24
		String regExLots = "(?is)LO?TS?\\s?([\\d+\\s]+)";
		String regExMarker = "LOT";
		String lots = ro.cst.tsearch.utils.StringUtils.getSuccessionOfValuesByRegEX(tmpLegalDescription, regExMarker, regExLots);
		lot.append(lots.replaceAll("\\s+", " "));

		regExMarker = "LTS";
		lots = ro.cst.tsearch.utils.StringUtils.getSuccessionOfValuesByRegEX(tmpLegalDescription, regExMarker, regExLots);
		lot.append(" " + lots.replaceAll("\\s+", " "));
		String lotAndLotRegEx = "LO?TS?\\s+(\\d+)\\s+(AND|&)\\s+(\\d+)";
		if (RegExUtils.matches(lotAndLotRegEx, tmpLegalDescription)) {
			String[] lotAndLot = RegExUtils.parseByRegEx(tmpLegalDescription, lotAndLotRegEx, new int[] { 1, 3 });
			if (lotAndLot.length > 0) {
				for (String string : lotAndLot) {
					if (!RegExUtils.matches("\\b" + string + "\\b", lot.toString())) {
						lot.append(" " + string);
					}
				}
			}
		}

		String intervalRegEx = "LOTS\\s+\\d+\\s+THRU\\s+\\d+";
		if (RegExUtils.matches(intervalRegEx, tmpLegalDescription)) {
			String intervalLots = RegExUtils.parseValuesForRegEx(tmpLegalDescription, intervalRegEx);
			intervalLots = intervalLots.replaceAll("LOTS", "").trim();
			intervalLots = identifyLots(intervalLots, new String[] { "THRU" });
			lot.append(" " + intervalLots);
		}

		// get what's inside lot box and add it to what's found in
		// legaldescription
		String currentValue = (String) resultMap.get("PropertyIdentificationSet.SubdivisionLotNumber");
		String identifyLots = identifyLots(currentValue, splitters);

		lot.append(" " + identifyLots);

		// sort and eliminate duplicates
		String[] strings = (lot.toString()).split("\\s+");
		Arrays.sort(strings, new IntegerComparator());
		String lotValues = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings).trim();

		resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lotValues);
		return tmpLegalDescription;
	}

	/**
	 * The return value has the block data removed
	 * 
	 * @param resultMap
	 * @param legalOnServer
	 * @return
	 */
	public static String setBlock(ResultMap resultMap, String legalOnServer) {
		String regex = "BLO?C?K\\s?(\\d+)";
		String key = "PropertyIdentificationSet.SubdivisionBlock";
		return setAndRemoveValueFromRegEx(resultMap, legalOnServer, regex, key);
	}

	public static String setSection(ResultMap resultMap, String legalOnServer) {
		String regex = "SEC\\s?(\\d+)";
		String key = "PropertyIdentificationSet.SubdivisionSection";
		return setAndRemoveValueFromRegEx(resultMap, legalOnServer, regex, key);
	}

	public static String setUnit(ResultMap resultMap, String legalOnServer) {
		String regex = "UNIT\\s?(\\d+)";
		String key = "PropertyIdentificationSet.SubdivisionSection";
		return setAndRemoveValueFromRegEx(resultMap, legalOnServer, regex, key);
	}

	private static String setAndRemoveValueFromRegEx(ResultMap resultMap, String legalOnServer, String regex, String key) {
		String block = RegExUtils.parseValuesForRegEx(legalOnServer, regex);
		String existingBlock = (String) resultMap.get(key);

		if (!RegExUtils.matches("\\b" + block + "\\b", existingBlock)) {
			resultMap.put(key, existingBlock + " " + block);
		}
		legalOnServer = legalOnServer.replaceAll(regex, "");
		return legalOnServer;
	}

	// public static String setSe

	public static void setTract(ResultMap resultMap, String legalOnServer) {
		if (StringUtils.isNotEmpty(legalOnServer)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionTract", legalOnServer);
		}
	}

	private static String[] splitters = new String[] { "THRU", "-", "&", ",", "/", "\\s+" };

	public static void setLot(ResultMap resultMap, String legalOnServer) {
		// first delete what's not an lot number
		legalOnServer = legalOnServer.replaceAll("(PA?R?T)?\\s(OF|LO?TS?)", "");
		legalOnServer = legalOnServer.replaceAll("(PA?R?T)?\\s(OF|LO?TS?)", "");
		legalOnServer = legalOnServer.replaceAll("(PA?R?T)", "");
		legalOnServer = legalOnServer.replaceAll("(N|S|W|E)\\s1/2", "");
		legalOnServer = legalOnServer.replaceAll("(N|S|W|E)\\s\\d{2,2}'", "");
		legalOnServer = legalOnServer.replaceAll("(N|S|W|E)\\s\\d{1,2}\\.\\d{1,2}", "");
		legalOnServer = legalOnServer.replaceAll("VACATED ST", "");
		legalOnServer = legalOnServer.replaceAll("(N|S|E|W)\\s?(?=\\d+)", "");
		legalOnServer = legalOnServer.replaceAll("\\bEXC\\s+[NSEW]{1,2}\\s*$", "");

		// StringUtils.splitPreserveAllTokens(str, separatorChars)
		String lotValues = identifyLots(legalOnServer, splitters);
		resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lotValues);
	}

	public static String identifyLots(String legalOnServer, String[] splitters) {
		String[] strings = ro.cst.tsearch.utils.StringUtils.splitAndKeepSplitters(legalOnServer, splitters, true);
		int i = 0;
		int prevIndex = 0;
		int nextIndex = 0;
		String prevLotNumberAsString = "";
		int prevLotNumber = 0;
		int nextLotNumber = 0;
		String nextLotNumberAsString = "";
		StringBuilder lots = new StringBuilder();
		for (i = 0; i < strings.length; i++) {
			// deal with enumeration like 2,3,4 & 5
			// deal with enumeration like 1-2-3-4
			// deal with intervals like 1-5
			// deal with intervals lie 1 THRU 5
			String curr = strings[i];
			if (ro.cst.tsearch.utils.StringUtils.isStringInStringArray(splitters, curr.trim())) {
				prevIndex = i - 1;
				nextIndex = i + 1;
				if (prevIndex >= 0 && nextIndex < strings.length) {
					prevLotNumberAsString = strings[prevIndex].trim();
					if (StringUtils.isNumeric(prevLotNumberAsString)) {
						prevLotNumber = Integer.valueOf(prevLotNumberAsString);
					}
					nextLotNumberAsString = strings[nextIndex].trim();
					if (StringUtils.isNumeric(nextLotNumberAsString)) {
						nextLotNumber = Integer.valueOf(nextLotNumberAsString);
					}
					boolean isIntervalNotEnumeration = ro.cst.tsearch.utils.StringUtils.isStringInStringArray(new String[] { "-" },
							curr.trim())
							&& (prevLotNumber + 1 != nextLotNumber);

					if (// prevLotNumber+1 == nextLotNumber &&
					!isIntervalNotEnumeration
							&& ro.cst.tsearch.utils.StringUtils.isStringInStringArray(new String[] { "-", "&", "\\s+", ",", "/" },
									curr.trim())) {
						// this test is for "-" which can be interpreted as
						// intervals,
						// as well as enumerations
						boolean containsPrevious = RegExUtils.matches("\\b" + prevLotNumber + "\\b", lots.toString())
								|| ro.cst.tsearch.utils.StringUtils.isStringInStringArray(splitters, prevLotNumberAsString);
						boolean containsNext = RegExUtils.matches("\\b" + nextLotNumber + "\\b", lots.toString())
								|| ro.cst.tsearch.utils.StringUtils.isStringInStringArray(splitters, nextLotNumberAsString);
						if (!containsPrevious && !containsNext) {
							lots.append(MessageFormat.format("{0} {1} ", prevLotNumber, nextLotNumber));
						} else {
							if (!containsPrevious) {
								lots.append(MessageFormat.format("{0} ", prevLotNumber));
							}
							if (!containsNext) {
								lots.append(MessageFormat.format("{0} ", nextLotNumber));
							}
						}
					} else { // it is considered to be an interval
						if (ro.cst.tsearch.utils.StringUtils.isStringInStringArray(new String[] { "-", "THRU" }, curr.trim())) {
							for (int k = prevLotNumber; k <= nextLotNumber; k++) {
								boolean containsK = lots.toString().matches("\\b" + k + "\\b");
								if (!containsK) {
									lots.append(MessageFormat.format("{0} ", k));
								}
							}
						} else {// is a single simple value
							if (!RegExUtils.matches("\\b" + prevLotNumberAsString + "\\b+", lots.toString())) {
								lots.append(MessageFormat.format("{0} ", prevLotNumberAsString));
							}

						}
					}
				}
			}
		}
		// arrange lots in ascending order
		if (StringUtils.isEmpty(lots.toString())) {
			lots.append(ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings));
		}
		strings = (lots.toString()).split("\\s+");
		Arrays.sort(strings, new IntegerComparator());
		int binarySearch = Arrays.binarySearch(strings, "0");
		if (binarySearch >= 0) {
			strings[binarySearch] = "";

		}
		String lotValues = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings).trim();
		return lotValues;
	}

	public static void setAddress(ResultMap resultMap, String addressOnServer) {
		resultMap.put("PropertyIdentificationSet.AddressOnServer", addressOnServer);

		if (addressOnServer.contains("/")) {
			addressOnServer = addressOnServer.replace("/", "-");
		}

		String[] split = addressOnServer.replaceAll("&nbsp;", "").split("-");

		String address = "";
		String city = "";

		if (split.length == 2) {
			address = split[0].trim();
			city = split[1].trim();
		} else {
			address = addressOnServer;
		}

		// String[] split = addressOnServer.replaceAll("&nbsp;", "").split(",");
		//
		// String address = "";
		// String city = "";
		// String state = "";
		// String zip = "";
		//
		// if (split.length==3){
		// address = split[0];
		// city = split[1];
		// state = split[2];
		// }

		String streetName = StringFormats.StreetName(address);
		String streetNo = StringFormats.StreetNo(address);

		resultMap.put("PropertyIdentificationSet.StreetName", streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);
		resultMap.put("PropertyIdentificationSet.City", city);
		// resultMap.put("PropertyIdentificationSet.State", );
		// resultMap.put("PropertyIdentificationSet.Zip", streetNo);

	}

	public static void extractDataFromResponse(ResultMap map, HtmlParser3 parser3) {

		// Assessor Data Table
		String columnID = "Parcel Number";
		String pid = MOPlatteAO.getValueFromNextCell(parser3, columnID);
		map.put("PropertyIdentificationSet.ParcelID", pid);
		columnID = "Owner Name";
		String nameOnServer = MOPlatteAO.getValueFromNextCell(parser3, columnID);
		map.put("PropertyIdentificationSet.NameOnServer", nameOnServer);

		columnID = "Physical Address";
		String addressOnServer = MOPlatteAO.getValueFromNextCell(parser3, columnID);
		map.put("PropertyIdentificationSet.AddressOnServer", addressOnServer);

		columnID = "Subdivision";
		String subdivision = MOPlatteAO.getValueFromNextCell(parser3, columnID);
		subdivision = subdivision.replaceAll("(?is)\\bADD(ITION)?\\b", "").replaceAll("(?is)(\\d+)[ST|ND|RD|TH]+\\b", "$1").trim();
		map.put("PropertyIdentificationSet.SubdivisionName", subdivision);

		columnID = "Property Description";
		String legalDescriptionOnServer = MOPlatteAO.getValueFromNextCell(parser3, columnID);
		map.put("PropertyIdentificationSet.LegalDescriptionOnServer", legalDescriptionOnServer);

		columnID = "Lot";
		String lot = MOPlatteAO.getValueFromNextCell(parser3, columnID);
		lot = lot.replaceAll("(?is)\\bEXC\\s+[NSEW]{1,2}(\\s+PT)?", "").trim();
		map.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);

		columnID = "Block";
		String block = MOPlatteAO.getValueFromNextCell(parser3, columnID);
		map.put("PropertyIdentificationSet.SubdivisionBlock", block);

		// Tax code information table
		columnID = "Section";
		String section = parseValuesFromTaxCodeInfo(parser3, columnID);
		map.put("PropertyIdentificationSet.SubdivisionSection", section);

		columnID = "Township";
		String township = parseValuesFromTaxCodeInfo(parser3, columnID);
		map.put("PropertyIdentificationSet.SubdivisionTownship", township);

		columnID = "Range";
		String range = parseValuesFromTaxCodeInfo(parser3, columnID);
		map.put("PropertyIdentificationSet.SubdivisionRange", range);

		// Current Appraised value

		columnID = "Land";
		map.put("PropertyAppraisalSet.LandAppraisal", parseValuesFromTaxCodeInfo(parser3, columnID));

		columnID = "Improvements";
		map.put("PropertyAppraisalSet.ImprovementAppraisal", parseValuesFromTaxCodeInfo(parser3, columnID));

		columnID = "Total";
		map.put("PropertyAppraisalSet.TotalAppraisal", parseValuesFromTaxCodeInfo(parser3, columnID));

		columnID = "Assessed Total";
		map.put("PropertyAppraisalSet.TotalAssessment", parseValuesFromTaxCodeInfo(parser3, columnID));

		// Collector Data Table
		Node assessorTable = HtmlParser3.findNode(parser3.getNodeList(), "Tax Year", true).getParent().getParent().getParent();

		if (assessorTable instanceof TableTag) {
			String cleanedTable = assessorTable.toHtml().replaceAll("(?is)</?font[^>]*>", "");
			cleanedTable = cleanedTable.replaceAll("</TR>", "");
			cleanedTable = cleanedTable.replaceAll("<TR>(?!=</td>)", "</TR><TR>");
			List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(cleanedTable);

			List<HashMap<String, String>> sourceSet = new LinkedList<HashMap<String, String>>();
			HashMap<String, String> tempMap = new HashMap<String, String>();

			for (HashMap<String, String> hashMap : tableAsListMap) {
				tempMap.put("Year", hashMap.get("Tax Year"));
				tempMap.put("AmountPaid", hashMap.get("Tax Amount").replaceAll("$", "").replaceAll(",", ""));
				tempMap.put("DatePaid", hashMap.get("Date Paid"));
				sourceSet.add(tempMap);
			}

			String[] header = new String[] { "Year", "AmountPaid", "DatePaid" };
			ResultBodyUtils.buildInfSet(map, sourceSet, header, TaxHistorySet.class);
		}

		// Recorder data table
		columnID = "Grantor";
		String grantor = getValueFromNextCell(parser3, columnID);
		map.put("SaleDataSet.Grantor", grantor);

		columnID = "Grantee";
		String grantee = getValueFromNextCell(parser3, columnID);
		map.put("SaleDataSet.Grantor", grantee);

		columnID = "Book";
		String book = getValueFromNextCell(parser3, columnID);
		map.put("SaleDataSet.Book", book);

		columnID = "Page";
		String page = getValueFromNextCell(parser3, columnID);
		map.put("SaleDataSet.Page", page);

		columnID = "Instrument No.";
		String instrumentNo = getValueFromNextCell(parser3, columnID);
		map.put("SaleDataSet.InstrumentNumber", instrumentNo);

		columnID = "Instrument Date";
		String instrumentDate = getValueFromNextCell(parser3, columnID);
		map.put("SaleDataSet.InstrumentDate", instrumentDate);

		columnID = "File Date";
		String fileDate = getValueFromNextCell(parser3, columnID);
		// map.put("SaleDataSet.RecordedDate", fileDate);

		List<HashMap<String, String>> sourceSet = new LinkedList<HashMap<String, String>>();

		HashMap<String, String> saleDataSetMap = new HashMap<String, String>();
		saleDataSetMap.put("Grantor", grantor);
		saleDataSetMap.put("Grantee", grantee);
		saleDataSetMap.put("Book", book);
		saleDataSetMap.put("Page", page);
		saleDataSetMap.put("InstrumentNumber", instrumentNo);
		saleDataSetMap.put("InstrumentDate", instrumentDate);
		saleDataSetMap.put("RecordedDate", fileDate);
		sourceSet.add(saleDataSetMap);

		String[] header = new String[] { "Grantor", "Grantee", "Book", "Page", "InstrumentNumber", "InstrumentDate", "RecordedDate" };
		ResultBodyUtils.buildInfSet(map, sourceSet, header, SaleDataSet.class);

		// parse the raw data
		setName(map, (String) map.get("PropertyIdentificationSet.NameOnServer"));
		setAddress(map, (String) map.get("PropertyIdentificationSet.AddressOnServer"));

		setDetailsLot(map, (String) map.get("PropertyIdentificationSet.LegalDescriptionOnServer"));
		// setIntermediaryLegal(map, (String)
		// map.get("PropertyIdentificationSet.LegalDescriptionOnServer"));

		// test
		/*
		 * String text = pid + "\r\n" + nameOnServer + "\r\n\r\n\r\n";
		 * ro.cst.tsearch
		 * .utils.FileUtils.appendToTextFile(MOPlatteAO.TEST_FILES_DEPLOYFOLDER
		 * + "name.txt", text); text = pid + "\r\n" + addressOnServer +
		 * "\r\n\r\n\r\n";
		 * ro.cst.tsearch.utils.FileUtils.appendToTextFile(MOPlatteAO
		 * .TEST_FILES_DEPLOYFOLDER + "address.txt", text); text = pid + "\r\n"
		 * + legalDescriptionOnServer + "\r\n" + lot + "\r\n" + block + "\r\n" +
		 * "\r\n\r\n\r\n";
		 * ro.cst.tsearch.utils.FileUtils.appendToTextFile(MOPlatteAO
		 * .TEST_FILES_DEPLOYFOLDER + "legal_description.txt", text);
		 * 
		 * text = pid + "\r\n" + lot + "\r\n\r\n\r\n";
		 * ro.cst.tsearch.utils.FileUtils
		 * .appendToTextFile(MOPlatteAO.TEST_FILES_DEPLOYFOLDER +
		 * "legal_description.txt", text);
		 */
		// end test
	}

	public static String parseValuesFromTaxCodeInfo(HtmlParser3 parser3, String columnID) {
		return HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(parser3.getNodeList(), columnID), "", true).replaceAll(
				"(?is)</?font[^>]*>", "");
	}

	public static String getValueFromNextCell(HtmlParser3 parser3, String columnID) {
		String string = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(parser3.getNodeList(), columnID), "", true).replaceAll(
				"(?is)</?font[^>]*>", "");
		string = cleanTheHtml(string);
		return string;
	}

	public static String cleanTheHtml(String legalDescriptionOnServer) {
		return StringUtils.isNotEmpty(legalDescriptionOnServer) ? legalDescriptionOnServer.replace("&nbsp;", "") : "";
	}

	public static final String TEST_FILES_DEPLOYFOLDER = "D:\\work\\MOPlatteAO\\";

}

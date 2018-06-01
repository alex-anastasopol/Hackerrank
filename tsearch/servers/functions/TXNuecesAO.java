package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.CountyCities;
import ro.cst.tsearch.utils.Roman;

import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

public class TXNuecesAO {

	@SuppressWarnings("unchecked")
	public static List buildLineForResultsBody(String pdfContent) {
		
		// get the Sales data(?is)Date\\s*?Vol.*\\d{2}/\\d{2}/\\d{4}
		Pattern salesPattern = Pattern.compile("(?is)Date\\s*?Volume\\s*?Page\\s*?Seller\\s*?Name.*Improvements");
		Matcher salesMatcher = salesPattern.matcher(pdfContent);
		String salesRawData = "";
		if (salesMatcher.find()) {
			salesRawData = salesMatcher.group();
		}
		String[] instrumentRows = salesRawData.split("\r\n|\r|\n");
		String[] rowsForConcatenation = new String[2];
		List<String> line = new ArrayList<String>();
		List<List> body = new ArrayList<List>();
		int currentRow = 0;
		for (String row : instrumentRows) {
			if (!row.matches("(?is).*?\\d{1,2}\\s*/\\s*\\d{1,2}\\s*/\\s*\\d{1,2}.*")){
				if (!(row.trim()).matches("\\d{5,}.*")){// R43473 TXSan Patricio
					currentRow++;
					continue;
				}
			}
			// sometimes the date it's switched with docNo
			row = row.replaceAll("(.*)(\\d{2}/\\d{2}/\\d{4})", "$2    $1");
			rowsForConcatenation = row.split("\\s{3,}");
			String rightLimit = "";
			if (rowsForConcatenation[1].trim().endsWith("-")){
				String nextRow = instrumentRows[currentRow+1];
				String[] itemsFromNextRow = nextRow.split("\\s{3,}");
				for(String item : itemsFromNextRow){
					if (item.matches("-\\d+.*")){
						rightLimit = item.replaceAll("[A-Z/-]+", "");
						break;
					}
				}
			}
			String[] newRows = {"", ""};
			if (rowsForConcatenation.length >= 3 && !rowsForConcatenation[1].matches("(?is)/\\w+")){
				if (rowsForConcatenation[0].trim().matches("\\d+")){//R43473 TXSan Patricio
					newRows[0] = rowsForConcatenation[0].trim();
				} else{
					newRows[0] = rowsForConcatenation[1].replaceAll("(?is)[A-Z,&]", "").replaceAll("[/|#]", "").trim() + rightLimit.trim();
					newRows[1] = rowsForConcatenation[2].replaceAll("(?is)[A-Z,&]", "").replaceAll("/", "").trim();
				}
				if (newRows[0].trim().endsWith("-") && newRows[1].trim().matches("\\d+-\\d+")){
					newRows[0] = newRows[0].replaceAll("-", "") + newRows[1];
					newRows[1] = "";
				}
				String bpi = constructInstrumentNumber(newRows);
				String[] items = bpi.split("###");
				if (items.length > 1){
					String[] pages = items[2].split("-");
					if (pages.length > 1){
						for (String page : pages){
							line = new ArrayList<String>();
							line.add("");
							line.add(items[1]);
							line.add(page);
							body.add(line);
						}
					} else {
						line = new ArrayList<String>();
						line.add("");
						line.add(items[1]);
						line.add(items[2]);
						body.add(line);
					}
				} else if (items.length == 1){   //R240237
					String[] instrs = items[0].split("@@@");
					for (String instr : instrs){
						line = new ArrayList<String>();
						line.add(instr.replaceAll("-", ""));
						line.add("");
						line.add("");
						body.add(line);
					}
				}
			}
			currentRow++;
		}

		return body;
	}

	public static String constructInstrumentNumber(String[] rowsForConcatenation) {
		String finalInstr = "", book = "", page = "";
		List<String> line = new ArrayList<String>();
		if (rowsForConcatenation[1].length() != 0) {
			// the case with intervals in Page column
			if (rowsForConcatenation[0].contains("-")) {
				String[] intervalLimits = rowsForConcatenation[0].split("-");
				if (intervalLimits.length == 2) {
					int leftLimit = Integer.valueOf(StringUtils.strip(intervalLimits[0].trim(), "0"));
					int rightLimit = Integer.valueOf(StringUtils.strip(intervalLimits[1].trim(), "0"));
					if (( rightLimit - leftLimit > 0) && (rightLimit - leftLimit <= 9)) {
						for (int i = leftLimit; i <= rightLimit; i++) {
							line.add(""+rowsForConcatenation[1].replace("-", "").trim()+i);
						}
					}
				} else {
					finalInstr = rowsForConcatenation[0].replaceAll("-", "").trim() + rowsForConcatenation[1].replaceAll("", "").trim();
				}
			} else {
				rowsForConcatenation[1] = rowsForConcatenation[1].replaceAll("\\A\\s*-", "").trim(); //R270009   
				if (rowsForConcatenation[0].length() < 6){
					book = rowsForConcatenation[0].trim();
					page = rowsForConcatenation[1];
				} else {
					finalInstr = rowsForConcatenation[0] + rowsForConcatenation[1];
				}
				
			}
		} else if (rowsForConcatenation[1].length() == 0) {
			String[] intervals = rowsForConcatenation[0].split("-");
			boolean notEmpty = (intervals.length==2) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(intervals[0].trim())&&ro.cst.tsearch.utils.StringUtils.isNotEmpty(intervals[1].trim());
			if (notEmpty) { // the case with intervals in
											// Volume column
				int length = intervals[0].length();
				int leftLimit = Integer.valueOf(intervals[0].substring(length - intervals[1].length(), length));
				int rightLimit = Integer.valueOf(StringUtils.stripStart(intervals[1].trim(), "0"));
				int beginInstr = Integer.valueOf(StringUtils.stripStart(intervals[0].trim(), "0"));
				if (( rightLimit - leftLimit > 0) && (rightLimit - leftLimit <= 9)) {
					for (int i = leftLimit; i <= rightLimit; i++) {
						finalInstr += "@@@" + beginInstr++;
					}
				}
			} else {// no intervals
				if (rowsForConcatenation[0].trim().matches("\\d+\\s+\\d+")){// R38548 TXSan Patricio
					String[] instr = rowsForConcatenation[0].split("\\s{1,}");
					for (String in : instr){
						finalInstr += "@@@" + in;
					}
				} else {
					finalInstr = rowsForConcatenation[0].replaceAll("-", "").trim();
				}
			}
			finalInstr = StringUtils.stripStart(finalInstr, "@@@");
			finalInstr = StringUtils.stripStart(finalInstr, "0");
		}
		finalInstr = StringUtils.stripStart(finalInstr, "0");
		book = StringUtils.stripStart(book, "0");
		page = StringUtils.stripStart(page, "0");
		line.add(finalInstr);
		line.add(book);
		line.add(page);
		
		return finalInstr + "###" + book + "###" + page;
	}

	public static ResultMap parseIntermediaryRow(HtmlTableRow tableRow, long searchId, int serverId) {
		ResultMap resultMap = new ResultMap();

		String crtTSServerName = TSServer.getCrtTSServerName(serverId);
		if (crtTSServerName.endsWith("TR")) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		}
		
		List<HtmlTableCell> cells = tableRow.getCells();
		if (cells.size() == 4) {
			String propertyId = org.apache.commons.lang.StringUtils.defaultString(cells.get(0).getTextContent()).trim();
			resultMap.put("PropertyIdentificationSet.ParcelID", propertyId);

			String owner = org.apache.commons.lang.StringUtils.defaultString(cells.get(1).getTextContent()).trim();
			resultMap.put("tmpOwner", owner);

			String address = org.apache.commons.lang.StringUtils.defaultString(cells.get(2).getTextContent()).trim();
			resultMap.put("tmpAddress", address);
			try {
				parseNames(resultMap, searchId, serverId);
			} catch (Exception e) {
				e.printStackTrace();
			}

			parseAddress(resultMap, searchId, serverId);

		}
		return resultMap;
	}

	public static void parseAddress(ResultMap resultMap, long searchId, int serverId) {
		String rawAddress = (String) resultMap.get("tmpAddress");

		if (StringUtils.isNotEmpty(rawAddress)) {
			rawAddress = rawAddress.replaceAll("(?is)tx( \\d+,?)?", "");// remove
			rawAddress = rawAddress.replaceAll("(?is)NO\\s+ACCESS", "");// remove
			// zip
			// and
			// state

			String[] cities = CountyCities.TX_NUECES;
			if ("TXSan PatricioTR".equals(TSServer.getCrtTSServerName(serverId))){
				cities = CountyCities.TX_SAN_PATRICIO;
			} else if ("TXGuadalupeTR".equals(TSServer.getCrtTSServerName(serverId))){
				cities = CountyCities.TX_GUADALUPE;
			}
			String[] address = StringFormats.parseCityFromAddress(rawAddress, cities);

			resultMap.put("PropertyIdentificationSet.City", address[0]);
			String streetName = address[1].trim().replaceAll("[,\\)\\(]+", "");
			resultMap.put("PropertyIdentificationSet.StreetName", streetName);
			//resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(streetName));
		}
	}

	@SuppressWarnings("unchecked")
	public static void parseNames(ResultMap resultMap, long searchId, int serverId) throws Exception {
		String ownerRawString = (String) resultMap.get("tmpOwner");
		String coOwnerString = (String) resultMap.get("tmpcoOwner");
		boolean addCoOwner = false;
		if (StringUtils.isNotEmpty(ownerRawString)) {
			ownerRawString = ownerRawString.replaceAll("\\bAND\\s+(ETUX)\\b", "$1").replaceAll("\\bAND\\s+(ETAL)\\b", "$1")
						.replaceAll("\\s*&amp;\\s*", " & ").replaceAll("\\s*,\\s*$", " & ")
						.replaceAll("(?is)(&\\s*\\w+\\s*),(\\s*\\w+\\s+\\w+\\s*)", "$1 & $2").trim();
			ownerRawString = ownerRawString.replaceAll("-VLB\\b", "").replaceAll("\\bATTY?\\b", "").replaceAll("\\bDO\\s*$", "")
											.replaceAll("\\bIND\\s+EX\\b", "");
			String cleanOwnerNameFromPrefix = GenericFunctions2.cleanOwnerNameFromPrefix(ownerRawString);
			cleanOwnerNameFromPrefix = cleanOwnerNameFromPrefix.replaceAll("(?is)\\b(TRUSTEE)(\\s+OF|\\s+FOR)", "$1 ");
			cleanOwnerNameFromPrefix = cleanOwnerNameFromPrefix.replaceAll("(?is)\\bCO\\s*-\\s*(TRU?S(?:TEES?)?)\\b", "$1");
			cleanOwnerNameFromPrefix = cleanOwnerNameFromPrefix.replaceAll("(?is)\\b(LIV(?:ING)?\\s+TR)\\b", "$1UST");
			cleanOwnerNameFromPrefix = GenericFunctions2.cleanOwnerNameFromMarriageStatus(cleanOwnerNameFromPrefix);
			cleanOwnerNameFromPrefix = cleanOwnerNameFromPrefix.replaceAll("/", " AND ").replaceAll(",\\s*$", "");
			cleanOwnerNameFromPrefix = GenericFunctions2.resolveOtherTypes(cleanOwnerNameFromPrefix);
			String[] owners = cleanOwnerNameFromPrefix.split("&");
			if(coOwnerString != null){
				coOwnerString = coOwnerString.replaceAll(",", "").trim();
			}
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(coOwnerString)){
				if (!coOwnerString.trim().matches("(?is)\\A(#?\\d+|P\\s*\\.?\\s*O\\s*\\.?\\s*BOX|GENERAL|%\\s+BAD\\s+ADDRESS).*")){
					coOwnerString = coOwnerString.replaceAll("(?is)(.*?)(\\d+|P\\s*O\\s*BOX).*", "$1").trim().replaceAll("\\s*&amp;\\s*", " & ").
											replaceAll("\\bWFE?\\b", "");
					coOwnerString = coOwnerString.replaceAll("\\bMRS\\b", "").replaceAll("%", "").replaceAll("\\A\\s*&", "")
													.replaceAll("\\b(TR(?:USTEES?)?)-", "$1").replaceAll("\\s*\\(\\s*", " & ").replaceAll("\\s*\\)\\s*", "").trim();
					coOwnerString = coOwnerString.replaceAll("\\b(\\w+\\s+\\w+)\\s*&\\s*(\\w+\\s+\\w+\\s+\\w+)\\s*-\\s*(TRUSTEES)", "$2 and $1 $3").trim();
					
					//ghertzoiala 
					coOwnerString = coOwnerString.replaceAll("(.+)\\s+(COMPASS.*)", "$1 & $2");
					cleanOwnerNameFromPrefix = GenericFunctions2.resolveOtherTypes(cleanOwnerNameFromPrefix);
					//end
					
					addCoOwner = true;
					if (coOwnerString.matches("\\w+")){
						cleanOwnerNameFromPrefix += " & " + coOwnerString;
						addCoOwner = false;
						cleanOwnerNameFromPrefix = cleanOwnerNameFromPrefix.replaceAll("\\s+&\\s+&\\s+", " & ");
					}
				} else if (owners.length == 3){
					coOwnerString = owners[2].trim();//R285965
					cleanOwnerNameFromPrefix = cleanOwnerNameFromPrefix.replaceAll("(\\s*&\\s*)?" + owners[2], "");
					addCoOwner = true;
				}
			}
			String[] ownerNames = { "", "", "", "", "", "" }, coOwnerNames = { "", "", "", "", "", "" };

			// parse owner name
			ownerNames = StringFormats.parseNameNashville(cleanOwnerNameFromPrefix, true);
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(ownerNames[5])){
				if (LastNameUtils.isNotLastName(ownerNames[5]) && NameUtils.isNotCompany(ownerNames[5])){
					ownerNames[4] = ownerNames[3];
					ownerNames[3] = ownerNames[5];
					ownerNames[5] = ownerNames[2];
				}
			}
			String[] types = GenericFunctions.extractAllNamesType(ownerNames);
			String[] otherTypes = GenericFunctions.extractAllNamesOtherType(ownerNames);
			String[] suffixes = GenericFunctions.extractNameSuffixes(ownerNames);
			List<List> body = new ArrayList<List>();
			GenericFunctions.addOwnerNames(ownerNames, suffixes[0], suffixes[1], types, otherTypes, 
											NameUtils.isCompany(ownerNames[2]), NameUtils.isCompany(ownerNames[5]), body);
			String nameOnServer = cleanOwnerNameFromPrefix;
			
			if (addCoOwner){
				coOwnerString = coOwnerString.replaceAll("\\bF/B/O\\b", "");
				coOwnerString = coOwnerString.replaceAll("\\bP\\.?O\\.?\\s+BOX\\b", "");
				if (coOwnerString.trim().matches("\\w+\\s*&\\s*\\w+") && NameUtils.isNotCompany(coOwnerString)){
					coOwnerString = coOwnerString.replaceAll("(\\w+)\\s*&\\s*(\\w+)", "$1 " + ownerNames[2] + " & $2 " + ownerNames[2]); 
				}
				String[] coowners = coOwnerString.split("&");
				if (coOwnerString.trim().matches("\\w+\\s*&\\s*\\w+\\s+\\w+(\\s+\\w+)?") && NameUtils.isNotCompany(coOwnerString)){//P41393
					coOwnerString = coOwnerString.replaceAll("(\\w+)\\s*&\\s*(\\w+\\s+\\w+(?:\\s+\\w+)?)", "$2 & $1");
					coowners = coOwnerString.split("&*&&##@");
				}
				
				for (String name : coowners) {
					coOwnerNames = StringFormats.parseNameDesotoRO(name, true);
					if (LastNameUtils.isNotLastName(coOwnerNames[2]) && NameUtils.isNotCompany(coOwnerNames[2])){
						coOwnerNames[1] = coOwnerNames[2];
						coOwnerNames[2] = ownerNames[2];
					}
					types = GenericFunctions.extractAllNamesType(coOwnerNames);
					otherTypes = GenericFunctions.extractAllNamesOtherType(coOwnerNames);
					suffixes = GenericFunctions.extractNameSuffixes(coOwnerNames);
					GenericFunctions.addOwnerNames(coOwnerNames, suffixes[0], suffixes[1], types, otherTypes,
												NameUtils.isCompany(coOwnerNames[2]), NameUtils.isCompany(coOwnerNames[5]), body);
				}
				nameOnServer += " & " + coOwnerString;
			}
			
			/*String legal = (String) resultMap.get("PropertyIdentificationSet.PropertyDescription");
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legal)){
				if (legal.matches("\\A\\s*Lease.*")){
					String otherOwners = legal.replaceAll("(?is)\\A\\s*Lease\\s*#\\s+\\d+\\s*([^\\)]+).*", "$1").replaceAll("(?is)\\s*\\(\\s*", " & ")
													.replaceAll("#\\s*\\d+", "");
					ownerNames = StringFormats.parseNameNashville(otherOwners);
					suffixes = GenericFunctions.extractNameSuffixes(ownerNames);
					GenericFunctions.addOwnerNames(ownerNames, suffixes[0], suffixes[1], NameUtils.isCompany(ownerNames[2]),
							NameUtils.isCompany(ownerNames[5]), body);
					nameOnServer += " & " + otherOwners;
				}
			}*/
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);

			String[] a = StringFormats.parseNameNashville(nameOnServer, true);
			resultMap.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
			resultMap.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
			resultMap.put("PropertyIdentificationSet.OwnerLastName", a[2]);
			resultMap.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
			resultMap.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
			resultMap.put("PropertyIdentificationSet.SpouseLastName", a[5]);

			resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);

		}
	}

	public static void parseLegal(ResultMap resultMap, long searchId, int serverId) throws Exception {
		
		String legal = (String) resultMap.get("PropertyIdentificationSet.PropertyDescription");
		
		if (ro.cst.tsearch.utils.StringUtils.isEmpty(legal)){
			return;
		}
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		if ("TXGuadalupeTR".equals(TSServer.getCrtTSServerName(serverId))){
			parseLegalTXGuadalupeTR(resultMap, legal, searchId);
		} else {
			parseLegalTXNuecesAO(resultMap, legal, searchId);
		}
		
	}
	
	public static void parseLegalTXNuecesAO(ResultMap resultMap, String legal, long searchId){
		
		legal = legal.replaceAll("\\s*&amp;\\s*", " & ");
		legal = legal.replaceAll("(\\d+),\\s+?(\\d+)", "$1 $2");
		legal = legal.replaceAll("\\b(\\d+)\\s+(?:THRU|THUR)\\s+(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("\\b\\d+'\\s+OF\\b", "");
		legal = legal.replaceAll("\\AND\\s+ALL\\s+OF\\b", "");
		legal = legal.replaceAll("\\b(\\d+)\\s+(?:THRU|THUR)\\s+(\\d+)\\b", "$1-$2");
		
		String legalTemp = legal;
		
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d-,\\s&]+[A-Z]?|[\\d&A-Z]+|\\d+[A-Z]{1,2})\\b");
		Matcher mat = p.matcher(legal);
		String lot = "";
		while (mat.find()){
			lot += " " + mat.group(2);
			legalTemp = legalTemp.replaceFirst(mat.group(0), mat.group(1) + " ");
		}
		if (lot.length() != 0) {
			lot = lot.replaceAll("\\s*&\\s*", " & ");
			lot = LegalDescription.cleanValues(lot, false, true);
			lot = StringUtils.strip(lot, "&");
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", StringUtils.defaultString(lot.trim()));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(?:TR|TRACT)\\s+([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionTract", mat.group(1).trim());
		}
		
		// extract abstract from legal description

		p = Pattern.compile("(?is)\\bABS?T?\\s+(\\d+[A-Z-]?)\\b");
		mat = p.matcher(legal);
		if (mat.find()) {
			resultMap.put("PropertyIdentificationSet.AbsNo", mat.group(1).trim());
			legal = legal.replaceAll(mat.group(1), "");
		}
		
		p = Pattern.compile("(?is)\\bBL?O?KS?\\s+([\\d-&\\s,]+[A-Z]?|[A-Z])\\b");
		mat = p.matcher(legal);
		String block = "";
		while (mat.find()){
			block += " " + mat.group(1);
			legal = legal.replaceFirst(mat.group(1), "");
		}
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			block = StringUtils.strip(block, "&");
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", StringUtils.defaultString(block.trim()));
		}
		
		p = Pattern.compile("(?is)\\b(?:UN|UNITS?)\\s+(\\d+[A-Z]?|[A-Z])\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit", mat.group(1).trim());
		}
		
		p = Pattern.compile("(?is)\\bBLDG\\s+(\\d+|[A-Z]-\\d+|[A-Z])\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionBldg", mat.group(1).trim());
		}
		
		p = Pattern.compile("(?is)\\bSEC\\s+([\\d,\\s]+|[A-Z])\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", mat.group(1).trim());
		}
		
		p = Pattern.compile("(?is)\\b(?:PH|PHASE)\\s+(\\d+)\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", mat.group(1).trim());
		}
		
		legal = legal.replaceAll("\\b[A-Z]\\d?/\\d+", "");
		String subdiv = "";
		p = Pattern.compile("(?is)BLO?C?KS?\\s+([^,]+)\\b");
		mat = p.matcher(legal);
		if (mat.find()) {
			subdiv = mat.group(1);
		} else {
			p = Pattern.compile("(?:\\s*)?(.+)\\s+(UNIT|BLOCKS?|TR|TRACT|BLKS?|LO?TS?|TRACT)\\b*");
			mat = p.matcher(legal);
			if (mat.find()) {
				subdiv = mat.group(1);
			} else {
				p = Pattern.compile("\\b(?:\\s*)?(.+?)(?:LO?T|TR|BLOCK|TRACT|PH |UNIT |BLDG)");
				mat.reset();
				mat.usePattern(p);
				if (mat.find()) {
					subdiv = mat.group(1);
				} else {
					p = Pattern.compile("\\b(ABS?T?)\\s+(.+?)\\s+(UND|SUR(?:VEY)?).*");
					mat.reset();
					mat.usePattern(p);
					if (mat.find()) {
						subdiv = mat.group(2);
					} 
				}
			}
		}
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("NO\\s+\\d+", "");
			subdiv = subdiv.replaceAll("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)", "$1" + "$2");
			subdiv = subdiv.replaceAll("(.*)\\s+(PARCEL|PHASE|UNIT).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(ADD|LO?T|BLK).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(UNREC.*)", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(TRACT|LTS?|[\\d+\\.,]+\\s+ACS).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(UNDIV).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+([A-Z]/[A-Z]).*", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceAll("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceAll("(.+) #\\s+ACRES.*", "$1");
			subdiv = subdiv.replaceAll("\\b[A-Z]\\d+/\\d+", "");
			subdiv = subdiv.replaceAll("\\A[\\d+\\s+|-](.*)", "$1");
			subdiv = subdiv.replaceAll("\\ALT\\s+(.*)", "$1");
			subdiv = subdiv.replaceAll("[\\d\\.,]+", "");
			subdiv = subdiv.replaceAll("\\bLYING IN.*", "");
			subdiv = subdiv.replaceAll("COM\\s+AT", "");
			subdiv = subdiv.replaceAll("COR\\s+OF", "");
			resultMap.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				resultMap.put("PropertyIdentificationSet.SubdivisionCond", subdiv.trim());
		}
	}
	
	public static void parseLegalTXGuadalupeTR(ResultMap resultMap, String legal, long searchId){
		
		legal = legal.replace(" &amp; ", " & ");
		legal = legal.replace("\\d+/\\d+\\s*&", "& ");
		legal = legal.replaceAll("(\\d+),\\s+?(\\d+)", "$1 $2");
		legal = legal.replaceAll("\\b(\\d+)\\s+(?:THRU|THUR)\\s+(\\d+)\\b", "$1-$2");
		
		Pattern p = Pattern.compile("(?is)\\bLOTS?\\s*:?\\s+([\\d\\s&,-]+[A-Z]?)\\b");
		Matcher mat = p.matcher(legal);
		String lot = "";
		while (mat.find()){
			lot += " " + mat.group(1);
		}
		if (lot.length() != 0) {
			lot = lot.replaceAll("&", " & ");
			lot = LegalDescription.cleanValues(lot, false, true);
			lot = StringUtils.strip(lot, "&");
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", StringUtils.defaultString(lot.trim()));
		}		
		p = Pattern.compile("(?is)\\bBLO?C?KS?\\s*:?\\s+(\\d+|[A-Z]{1,2})\\s+\\b");
		mat = p.matcher(legal);
		String block = "";
		while (mat.find()){
			block += " " + mat.group(1);
		}
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			block = StringUtils.strip(block, "&");
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", StringUtils.defaultString(block.trim()));
		}
		
		// extract ncb from legal description
		p = Pattern.compile("(?is)\\bCB\\s*:?\\s+(\\d+[A-Z]?)");
		mat = p.matcher(legal);
		if (mat.find()) {
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.NcbNo", mat.group(1).trim());
		}
				
		// extract abstract from legal description

		p = Pattern.compile("(?is)\\bABS\\s*:?\\s+(\\d+)\\b");
		mat = p.matcher(legal);
		if (mat.find()) {
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.AbsNo", mat.group(1).trim());
		}

		
		p = Pattern.compile("(?is)\\bSEC\\s*:?\\s+(\\d+)\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", mat.group(1).trim());
		}
		
		p = Pattern.compile("(?is)\\b(?:UNIT|UT-)\\s*:?\\s+([#\\d]+)\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit", mat.group(1).trim().replaceAll("#", ""));
		}
		
		p = Pattern.compile("(?is)\\bPH\\s*:?\\s+([#\\d]+)\\b");
		mat = p.matcher(legal);
		if (mat.find()){
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", mat.group(1).trim().replaceAll("#", ""));
		}
		p = Pattern.compile("(?is)\\b(?:PLAT)?\\s*:?\\s*(\\d{3,5})\\s*/\\s*(\\d+)\\b");
		mat = p.matcher(legal);
		if (mat.find()) {
			legal = legal.replaceAll(mat.group(0), "");
			resultMap.put("PropertyIdentificationSet.PlatBook", mat.group(1).trim());
			resultMap.put("PropertyIdentificationSet.PlatNo", mat.group(2).trim());
		}
		
		String subdiv = "";
		p = Pattern.compile("(?is)\\bADDN\\s*:?\\s*(.+)(?:\\s+UNIT|PH|TEX|VA|[\\d\\.]*|$)\\b");
		mat = p.matcher(legal);
		if (mat.find()) {
			subdiv = mat.group(1);
		} else {
			p = Pattern.compile("(?is)\\(([^\\)]+)");
			mat.usePattern(p);
			mat.reset();
			if (mat.find()) {
				subdiv = mat.group(1);
			} else {
				p = Pattern.compile("(?is)\\A([^,]+)");
				mat.usePattern(p);
				mat.reset();
				if (mat.find()) {
					subdiv = mat.group(1);
				} 
			}
		}
		
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("NO\\s+\\d+", "");
			subdiv = subdiv.replaceAll("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)", "$1" + "$2");
			subdiv = subdiv.replaceAll("(.*)\\s+(PARCEL|PHASE|UNIT).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(ADD|LO?T|BLK).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(UNREC.*)", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+(TRACT|LTS?|[\\d+\\.,]+\\s+ACS).*", "$1");
			subdiv = subdiv.replaceAll("(.*)\\s+([A-Z]/[A-Z]).*", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceAll("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceAll("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+TEX\\s*#.*", "$1");
			subdiv = subdiv.replaceAll("COM\\s+AT", "");
			subdiv = subdiv.replaceAll("COR\\s+OF", "");
			resultMap.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				resultMap.put("PropertyIdentificationSet.SubdivisionCond", subdiv.trim());
		}
	}

	@SuppressWarnings("unchecked")
	public static void parseTax(ResultMap resultMap, String detailsHtml, long searchId, int serverId) {
		HtmlParser3 parser = new HtmlParser3(detailsHtml);
		String taxTable = parser.getNodeContentsById("tblFees");
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(taxTable)){
			try {
				org.htmlparser.Parser parserO = org.htmlparser.Parser
						.createParser(taxTable, null);
				NodeList mainTableList = parserO.parse(null);
				TableTag mainTable = (TableTag) mainTableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
				int lastYear = 0;
				List<List> body = new ArrayList<List>();
				List<String> line = null;
				String priorDelinq = "";
				if ("TXGuadalupeTR".equals(TSServer.getCrtTSServerName(serverId))){
					for (int i = 0; i < rows.length; i++) {
						TableRow row = rows[i];
						if (row.getColumnCount() > 0) {
							/*if (row.toPlainTextString().contains("Past Years Due") && rows.length > 2){
								TableColumn[] co = rows[i+1].getColumns();
								String priorDelinq = co[0].toPlainTextString().trim();
								priorDelinq = priorDelinq.replaceAll("(?is)[\\$,]+", "");
								resultMap.put("TaxHistorySet.PriorDelinquent", StringUtils.isNotBlank(priorDelinq) ? priorDelinq : "");
							}*/
							
							if (row.toPlainTextString().contains("Guadalupe County")){
								TableColumn[] cols = row.getColumns();
								if (lastYear == 0){
									line = new ArrayList<String>();
									String taxYear = cols[0].toPlainTextString().trim();
									resultMap.put("TaxHistorySet.Year", StringUtils.isNotBlank(taxYear) ? taxYear : "");
									
									String baseAmount = cols[2].toPlainTextString().trim();
									baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
									resultMap.put("TaxHistorySet.BaseAmount", StringUtils.isNotBlank(baseAmount) ? baseAmount : "");
									
									String datePaid = cols[6].toPlainTextString().trim();
									line.add(datePaid);
								
									String amountPaid = cols[7].toPlainTextString().trim();
									amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
									resultMap.put("TaxHistorySet.AmountPaid", StringUtils.isNotBlank(amountPaid) ? amountPaid : "");
									line.add(amountPaid);
									body.add(line);
									
									String totalDue = cols[8].toPlainTextString().trim();
									totalDue = totalDue.replaceAll("(?is)[\\$,]+", "");
									resultMap.put("TaxHistorySet.TotalDue", StringUtils.isNotBlank(totalDue) ? totalDue : "");
									lastYear++;
								} else {
									line = new ArrayList<String>();
									
									String datePaid = cols[6].toPlainTextString().trim();
									line.add(datePaid);
									
									String amountPaid = cols[7].toPlainTextString().trim();
									amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
									line.add(amountPaid);
									body.add(line);
									
									if (cols.length > 8){
										priorDelinq += "+" + cols[8].toPlainTextString().trim().replaceAll("(?is)[\\$,]+", "");
									}
								}
							}
						}
					}
				} else if ("TXSan PatricioTR".equals(TSServer.getCrtTSServerName(serverId))){
					for (int i = 0; i < rows.length; i++) {
						TableRow row = rows[i];
						if (row.getColumnCount() > 0) {
							/*if (row.toPlainTextString().contains("Past Years Due") && rows.length > 2){
								TableColumn[] co = rows[i+1].getColumns();
								String priorDelinq = co[0].toPlainTextString().trim();
								priorDelinq = priorDelinq.replaceAll("(?is)[\\$,]+", "");
								resultMap.put("TaxHistorySet.PriorDelinquent", StringUtils.isNotBlank(priorDelinq) ? priorDelinq : "");
							}*/
							if (row.toPlainTextString().contains("San Patricio County")){
								TableColumn[] cols = row.getColumns();
								if (lastYear == 0){
									line = new ArrayList<String>();
									String taxYear = cols[0].toPlainTextString().trim();
									resultMap.put("TaxHistorySet.Year", StringUtils.isNotBlank(taxYear) ? taxYear : "");
									
									String baseAmount = cols[2].toPlainTextString().trim();
									baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
									resultMap.put("TaxHistorySet.BaseAmount", StringUtils.isNotBlank(baseAmount) ? baseAmount : "");
									
									String datePaid = cols[6].toPlainTextString().trim();
									line.add(datePaid);
								
									String amountPaid = cols[7].toPlainTextString().trim();
									amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
									resultMap.put("TaxHistorySet.AmountPaid", StringUtils.isNotBlank(amountPaid) ? amountPaid : "");
									line.add(amountPaid);
									body.add(line);
									
									String totalDue = cols[8].toPlainTextString().trim();
									totalDue = totalDue.replaceAll("(?is)[\\$,]+", "");
									resultMap.put("TaxHistorySet.TotalDue", StringUtils.isNotBlank(totalDue) ? totalDue : "");
									lastYear++;
								} else {
									line = new ArrayList<String>();
									
									String datePaid = cols[6].toPlainTextString().trim();
									line.add(datePaid);
									
									String amountPaid = cols[7].toPlainTextString().trim();
									amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
									line.add(amountPaid);
									body.add(line);
									
									if (cols.length > 8){
										priorDelinq += "+" + cols[8].toPlainTextString().trim().replaceAll("(?is)[\\$,]+", "");
									}
								}
							}
	
						}
					}
				}
				resultMap.put("TaxHistorySet.PriorDelinquent", GenericFunctions.sum(priorDelinq, searchId));
				ResultTable rt = new ResultTable();;
				String[] header = {"ReceiptDate", "ReceiptAmount"};
				rt = GenericFunctions2.createResultTable(body, header);
				resultMap.put("TaxHistorySet", rt);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

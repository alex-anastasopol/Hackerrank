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

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class MOStLouisTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId, boolean isSubdivName) {
				
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
				
		TableColumn[] cols = row.getColumns();
		
		if (cols.length == 4) {
			String parcelID = cols[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim(); 
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			
			if (isSubdivName) {
				String subdivName= cols[2].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName);
			} else {
				String address= cols[2].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			}
			
			
			String owner = cols[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim().replaceAll("\\s{2,}", " ");
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
			
			parseNames(resultMap, searchId);
			parseAddress(resultMap);
		}
				
		return resultMap;
	}
	
	public static void putValue(ResultMap resultMap, String key, String value) {
		value = value.replaceAll("(?is)&nbsp;?", " ");
		value = value.replaceAll("(?is)\\bPT\\b", "");
		value = value.trim();
		String existingLot = (String)resultMap.get(key);
		if (!StringUtils.isEmpty(existingLot)) {
			value = existingLot + " " + value;
			value = LegalDescription.cleanValues(value, false, true);
		}
		if (!StringUtils.isEmpty(value)) {
			resultMap.put(key, value);
		}
	}
	
	public static void putSubdivision(ResultMap resultMap, String subdivision) {
		subdivision = subdivision.replaceAll("(?is)&nbsp;?", " ").trim();
		String lotPatt1 = "(?is)\\bLOT\\s+(?:PT\\s+)?(\\d+(?:\\s*&\\s*(?:PT\\s+)?(?:\\d+))?)";
		Matcher ma1 = Pattern.compile(lotPatt1).matcher(subdivision);
		StringBuilder sb = new StringBuilder();
		while (ma1.find()) {
			sb.append(ma1.group(1).replaceAll("(?is)\\bPT\\b", "").replaceAll("&", " ")).append(" ");
		}
		String lotPatt2 = "(?is)\\bLOTS?\\s+((?:\\d+-)+)";
		Matcher ma2 = Pattern.compile(lotPatt2).matcher(subdivision);
		while (ma2.find()) {
			sb.append(ma2.group(1).replaceAll("-", " ")).append(" ");
		}
		String lot = sb.toString();
		if (!StringUtils.isEmpty(lot)) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			subdivision = subdivision.replaceAll(lotPatt1, "");
			subdivision = subdivision.replaceAll(lotPatt2, "");
		}
		
		String sectionPatt = "(?is)\\bSECTION\\s+(.+?)$";
		Matcher ma3 = Pattern.compile(sectionPatt).matcher(subdivision);
		if (ma3.find()) {
			String section = ma3.group(1).trim().toUpperCase();
			section = ro.cst.tsearch.extractor.xml.GenericFunctions1.replaceNumbers(section);
			subdivision = subdivision.replaceAll(sectionPatt, "").trim();
			resultMap.put(PropertyIdentificationSetKey.SECTION.getKeyName(), section);
		}
		
		subdivision = subdivision.replaceFirst("(?is)\\bBdy\\s+Adj\\b", "");
		subdivision = subdivision.replaceFirst("(?is)&\\s*$", "");
		subdivision = subdivision.replaceFirst("(?is)\\(THE\\)\\s*$", "");
		subdivision = subdivision.replaceFirst("(?is)\\b(CONDO\\b.*)CONDOMINIUM(?:\\s+PLAT)?\\s*$", "$1");	//Lakeshire Condo K Condominium
		subdivision = subdivision.trim();
		if (!StringUtils.isEmpty(subdivision)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
			if (subdivision.matches(".*\\bCONDO.*")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivision);
			}
		}
	}
	
	public static String cleanName(String s) {
		s = s.replaceAll("(?is)\\bJ/T\\b", "");
		s = s.replaceAll("(?is)\\bH/([HW]\\b)?", "");
		s = s.replaceAll("(?is)H/([HW])?\\b", "");
		s = s.replaceAll("(?is)\\bTRUSTEE\\b", "TR");
		s = s.replaceAll("(?is)\\bTRUSTEES\\b", "TRS");
		s = s.replaceAll("(?is)\\b(SMITH)\\s+(ALMEIDA)(\\s.+)", "$1@@@$2$3");	//22M620165, SMITH ALMEIDA PAMELA J
		s = s.replaceAll("(?is)\\b(REARDON)(KATHLEEN)\\b", "$1 $2");
		s = s.replaceAll("(?is)\\b(AWADALLA)(SYLVIA)\\b", "$1 $2");
		s = s.replaceAll("(?is)\\b(DONNELLY)(MARGARET)\\b", "$1 $2");
		s = s.replaceAll("(?is)\\b([A-Z])(JR)\\b", "$1 $2");
		s = s.trim();
		if (!s.contains("&") && !NameUtils.isCompany(s)) {
			String ss = s.replaceAll(GenericFunctions1.nameSuffixString, "");
			ss = ss.replaceAll(GenericFunctions1.nameTypeString, "");
			ss = ss.replaceAll(GenericFunctions1.nameOtherTypeString, "");
			String[] split = ss.split("\\s+");
			if (split.length>2 && split[0].length()>1) {
				int nr = 0;
				for (int i=1;i<split.length;i++) {
					if (split[i].length()>2) {
						nr++;
					}
				}
				if (nr>=2) {
					s = s.replaceAll("  ", " & ");	//separate multiple names
				}
			}
		}
		//PIERCE ALVIN E SR ORA M ->PIERCE ALVIN E SR & ORA M
		s = s.replaceAll("(?is)(.*?" + GenericFunctions1.nameSuffixString + ")\\s+([A-Z]+(?:\\s+[A-Z]+)?)\\b", "$1 & $3");
		return s;
	}
	
	public static boolean isFirstName(String s) {
		List<String> additionalNames = new ArrayList<String>();
		additionalNames.add("PATRICEA");		//12O341243, SMITH ANTONIO L & PATRICEA BUMBREY H/W
		if (additionalNames.contains(s.toUpperCase())) {
			return true;
		}
		return FirstNameUtils.isFirstName(s);
	}
	
	public static boolean isCompany(String s) {
		if (s.matches("(?is).*?ST\\.?\\s+LOUIS\\s+COUNTY.*")) {	//19K320348, ST LOUIS COUNTY TRUSTEE
			return true;
		}
		return NameUtils.isCompany(s);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner)) {
			return;
		}
	   
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		List<List> body = new ArrayList<List>();
		
		owner = owner.replaceAll("\\bC/O\\b", "<br>###");
		String[] split = owner.split("<br>");
		for (int i=0;i<split.length;i++) {
			String ow = split[i];
			ow = cleanName(ow);
			boolean isCompany = false;
			if (isCompany(ow)) {
				names [0] = names[1] = names[3] = names[4] = names[5]; 
				names[2]  = ow;
				isCompany = true;
			} else {
				if (ow.startsWith("###")) {
					names = StringFormats.parseNameDesotoRO(ow.replaceFirst("###", ""), true);
				} else {
					names = StringFormats.parseNameNashville(ow, true);
				}
			}
			if (names[2].equals(names[5])) {	//19K320261, VIRANT JOHN A & WILLARD MARY KATHERINE H/W
				int firstType = 0;
				int middleType = 0;
				if (FirstNameUtils.isMaleName(names[3]) && !FirstNameUtils.isFemaleName(names[3])) {
					firstType = 1;	//male only
				} else if (FirstNameUtils.isFemaleName(names[3]) && !FirstNameUtils.isMaleName(names[3])) {
					firstType = 2;	//female only
				}
				String spl[] = names[4].split("\\s+");
				if (firstType!=0 && spl.length>0) {
					if (FirstNameUtils.isMaleName(spl[0]) && !FirstNameUtils.isFemaleName(spl[0])) {
						middleType = 1;
					} else if (FirstNameUtils.isFemaleName(spl[0]) && !FirstNameUtils.isMaleName(spl[0])) {
						middleType = 2;
					}
					for (int j=1;j<spl.length;j++) {
						int newMiddleType = 0;
						if (FirstNameUtils.isMaleName(spl[j]) && !FirstNameUtils.isFemaleName(spl[j])) {
							newMiddleType = 1;
						} else if (FirstNameUtils.isFemaleName(spl[j]) && !FirstNameUtils.isMaleName(spl[j])) {
							newMiddleType = 2;
						}
						if (newMiddleType!=0 && newMiddleType!=middleType) {
							middleType = 0;
							break;
						}
					}
					if (middleType!=0 && middleType!=firstType) {
						String[] newNames = {"", "", "", "", "", ""};
						newNames = StringFormats.parseNameNashville(names[3] + " " + names[4], true);
						names[3] = newNames[0];
						names[4] = newNames[1];
						names[5] = newNames[2];
					}
				}
			}
			names[2] = names[2].replaceAll("(?is)\\b(SMITH)@@@(ALMEIDA)\\b", "$1 $2");
			if (!isFirstName(names[3]) && LastNameUtils.isLastName(names[3]) &&		//23J421621, SMITH ANDREW G & CYNTHIA GRAVILLE H/W
					isFirstName(names[5]) && !LastNameUtils.isLastName(names[5])) {
				String aux = names[3];
				names[3] = names[5];
				names[5] = aux;
			}
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
				isCompany?true:NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAddress(ResultMap resultMap)  {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		String[] split = address.split("(?is)<br>");
		
		String addr = "";
		String cityZip = "";
		if (split.length==3) {
			addr = split[0].trim();
			String line1 = split[1].trim();
			Matcher ma1 = Pattern.compile("(?is)\\bUnit:?\\s+([\\w-/]+)").matcher(line1);
			if (ma1.find()) {
				addr += " #" + ma1.group(1);
			}
			cityZip = split[2].trim();
		} else if (split.length==2) {
			addr = split[0];
			cityZip = split[1];
		} else if (split.length==1) {
			addr = split[0];
		}
		
		addr = addr.replaceFirst("(?is)\\bAPT\\s+([\\w-/]+)$", "#$1");
		addr = addr.replaceFirst("(?is)\\s+([A-Z0-9])$", " #$1");
		String streetName = StringFormats.StreetName(addr).trim();
		String streetNo = StringFormats.StreetNo(addr);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		
		if (!StringUtils.isEmpty(cityZip)) {
			Matcher ma2 = Pattern.compile("(?is)(.+?)\\s*,\\s*[A-Z]{2}\\s+(\\d{5})").matcher(cityZip);
			if (ma2.find()) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), ma2.group(1).trim());
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), ma2.group(2));
			}
		}
		
		address = address.replaceAll("(?is)\\s*<br>\\s*", "; ");
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
	}
		
	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap)
	{
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		String lotExpr = "(?is)\\bLOTS?\\s+((?:[NSEW]+\\s+)?(?:P(?:AR)?TS?\\s+)?\\d+" + 
				"(?:(-\\d+))?(?:\\s*,\\s*(?:[NSEW]+)?(?:[NSEW]+\\s+)?(?:P(?:AR)?TS?\\s+)?\\d+)*(?:\\s*&\\s*(?:[NSEW]+)?(?:[NSEW]+\\s+)?(?:P(?:AR)?TS?\\s+)?\\d+)*)";
		List<String> lot = RegExUtils.getMatches(lotExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(lotExpr, " LOT ");
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<lot.size(); i++) {
			String lt = lot.get(i);
			lt = lt.replaceAll("(?is)\\b[NSEW]+\\b", "");
			lt = lt.replaceAll("(?is)\\bP(?:AR)?TS?\\b", "");
			lt = lt.replaceAll("[&,]", " ");
			lt = lt.trim();
			sb.append(lt).append(" ");
		}
		String subdivisionLot = sb.toString().trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			putValue(resultMap, PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		String blockExpr = "(?is)\\bBLK\\s+(\\d+)";
		List<String> block = RegExUtils.getMatches(blockExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(blockExpr, " BLOCK ");
		sb = new StringBuilder();
		for (int i=0; i<block.size(); i++) {
			sb.append(block.get(i)).append(" ");
		}
		String subdivisionBlock = sb.toString().trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			putValue(resultMap, PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		
		String bldgExpr = "(?is)\\bBLDG\\s+(\\d+)";
		List<String> bldg = RegExUtils.getMatches(bldgExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(bldgExpr, " BLDG ");
		sb = new StringBuilder();
		for (int i=0; i<bldg.size(); i++) {
			sb.append(bldg.get(i)).append(" ");
		}
		String subdivisionBldg = sb.toString().trim();
		if (subdivisionBldg.length() != 0) {
			subdivisionBldg = LegalDescription.cleanValues(subdivisionBldg, false, true);
			putValue(resultMap, PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), subdivisionBldg);
		}
		
		String unitExpr = "(?is)\\bUNIT(?:\\s+NO)?\\s+(\\d+(?:-[A-Z])?|[A-Z])";
		List<String> unit = RegExUtils.getMatches(unitExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(unitExpr, " UNIT ");
		sb = new StringBuilder();
		for (int i=0; i<unit.size(); i++) {
			sb.append(unit.get(i)).append(" ");
		}
		String subdivisionUnit = sb.toString().trim();
		if (subdivisionUnit.length() != 0) {
			subdivisionUnit = LegalDescription.cleanValues(subdivisionUnit, false, true);
			putValue(resultMap, PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		}
		
		String strExpr = "(?i)\\b\\(?\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*\\)?\\s*$";
		Matcher ma1 = Pattern.compile(strExpr).matcher(legalDescription);
		legalDescription = legalDescription.replaceAll(strExpr, "");
		if (ma1.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma1.group(1));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma1.group(2));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma1.group(3));
		}
		
		String refExpr = "(?is)\\(.*?(\\d+)[-\\s]+(\\d+)\\)";
		Matcher ma2 = Pattern.compile(refExpr).matcher(legalDescription);
		legalDescription = legalDescription.replaceAll(refExpr, "");
		if (ma2.find()) {
			String book = ma2.group(1);
			String page = ma2.group(2);
			List<List> tablebodyRef = new ArrayList<List>();
			List<String> list = new ArrayList<String>();
			list.add("");	//grantor
			list.add("");	//grantee
			list.add("");	//recorded date
			list.add("");	//instrument number
			list.add(book);	//book
			list.add(page);	//page
			list.add("");	//document type
			list.add("");	//sales price
			tablebodyRef.add(list);
			String[] headerRef = {SaleDataSetKey.GRANTOR.getShortKeyName(),SaleDataSetKey.GRANTEE.getShortKeyName(),
					SaleDataSetKey.RECORDED_DATE.getShortKeyName(),	SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
					SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName(),
					SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName()};
			ResultTable crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
			ResultTable existingCrossRef = (ResultTable)resultMap.get("SaleDataSet");
			if (existingCrossRef!=null) {
				String[][] body = existingCrossRef.getBody();
				boolean found = false;
				for (int i=0;i<body.length;i++) {
					if (body[i].length==8 && book.equals(body[i][4]) && page.equals(body[i][5])) {
						found = true;
						break;
					}
				}
				if (!found) {
					try {
						crossRef = ResultTable.joinVertical(existingCrossRef, crossRef, true);
						resultMap.put("SaleDataSet", crossRef);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				resultMap.put("SaleDataSet", crossRef);
			}
		}
		  
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		
		String baseAmount = "0.0";
		String totalDue = "0.0";
		String priorDelinquent = "0.0";
		String amountPaid = "0.0";
		String year = "";
		
		ResultTable rt = new ResultTable();		//tax history table
		List<List> tablebody = new ArrayList<List>();
		List<String> list;
		
		String baseAmt = "0.0";
		String amtDue = "0.0";
		String amtPaid = "0.0";
		String datePaid = "0.0";
		String taxYear = "";
		
		NodeList taxesPaidList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_TaxesDueData1_tableTaxPaidRE"));
		if (taxesPaidList.size()>0) {
			TableTag taxesPaidTable = (TableTag)taxesPaidList.elementAt(0);
			if (taxesPaidTable.getRowCount()>2) {
				TableRow row = taxesPaidTable.getRow(2);
				if (row.getColumnCount()==8) {
					year = row.getColumns()[0].toPlainTextString().trim();
					baseAmount = row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim();
					amountPaid = row.getColumns()[6].toPlainTextString().replaceAll("[,$*]", "").trim();
					datePaid = row.getColumns()[7].toPlainTextString().trim();
					list = new ArrayList<String>();
					list.add(year);
					list.add(baseAmount);
					list.add("0.0");
					list.add(amountPaid);
					list.add(datePaid);
					tablebody.add(list);
				}
			}
		} else {
			NodeList taxesDueList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_TaxesDueData1_tableTaxDueRE"));
			if (taxesDueList.size()>0) {
				TableTag taxesDueTable = (TableTag)taxesDueList.elementAt(0);
				if (taxesDueTable.getRowCount()>2) {
					TableRow row = taxesDueTable.getRow(2);
					if (row.getColumnCount()==6) {
						year = row.getColumns()[0].toPlainTextString().trim();
						baseAmount = row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim();
						totalDue = row.getColumns()[5].toPlainTextString().replaceAll("[,$]", "").trim();
					}
				}
				StringBuilder delq = new StringBuilder("0.0");
				for (int i=3;i<taxesDueTable.getRowCount();i++) {
					TableRow row = taxesDueTable.getRow(i);
					if (row.getColumnCount()==6) {
						taxYear = row.getColumns()[0].toPlainTextString().trim();
						baseAmt = row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim();
						amtDue = row.getColumns()[5].toPlainTextString().replaceAll("[,$]", "").trim();
						delq.append("+").append(amtDue);
					}
				}
				priorDelinquent = GenericFunctions.sum(delq.toString(), searchId);
			}
		}	
		
		if (StringUtils.isNotEmpty(year)) {
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
		}
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
		resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
		
		NodeList taxHistoryList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_RealEstateHistoryData1_tableTaxHistory"));
		if (taxHistoryList.size()>0) {
			TableTag taxHistoryTable = (TableTag)taxHistoryList.elementAt(0);
			for (int i=1;i<taxHistoryTable.getRowCount();i++) {
				TableRow row = taxHistoryTable.getRow(i);
				if (row.getColumnCount()==8) {
					taxYear = row.getColumns()[0].toPlainTextString().trim();
					amtDue = row.getColumns()[5].toPlainTextString().replaceAll("[,$]", "").trim();
					amtPaid = row.getColumns()[6].toPlainTextString().replaceAll("[,$*]", "").trim();
					datePaid = row.getColumns()[7].toPlainTextString().trim();
					if (!datePaid.toLowerCase().contains("not paid") && StringUtils.isNotEmpty(amountPaid)) {
						list = new ArrayList<String>();
						list.add(taxYear);
						list.add("");	//base amount
						list.add(amtDue);
						list.add(amtPaid);
						list.add(datePaid);
						tablebody.add(list);
					}
				}
			}	
		}
										
		String[] header = {TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.BASE_AMOUNT.getShortKeyName(), 
				TaxHistorySetKey.TOTAL_DUE.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName(),
				TaxHistorySetKey.RECEIPT_DATE.getShortKeyName()};
		rt = GenericFunctions2.createResultTable(tablebody, header);
		if (rt != null){
			resultMap.put("TaxHistorySet", rt);
		}
		
	}
}

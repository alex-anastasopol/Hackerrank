package ro.cst.tsearch.servers.functions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class KSWyandotteTR {

	public static final String[] COMPANY_NAMES = {"U.S.D.#500"}; //Unified School District 500
	
	public static final String[] CITIES = {"BONNER SPRINGS", "EDWARDSVILLE", "KANSAS CITY", "LAKE QUIVIRA", "TURNER", "WELBORN"};
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
				
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
				
		TableColumn[] cols = row.getColumns();
		if (cols.length==4) {
					
			String owner = cols[1].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
			String address= cols[2].toHtml();
			address = address.replaceAll("(?is)</?[^>]+>", "").replaceAll("\\s{1,}", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			String parcelID = cols[3].toPlainTextString().trim(); 
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			
			parseNames(resultMap, searchId);
			parseAddress(resultMap);
		}
				
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		owner = owner.replaceAll("(?is)&amp;", "&");
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		List<List> body = new ArrayList<List>();
		String[] split = owner.split("<br>");
		for (int i=0;i<split.length;i++) {
			String ow = split[i];
			ow = ow.replaceFirst("(?is)^\\s*C/O\\b", "").trim();
			if (NameUtils.isCompany(ow)) {
				ow = ow.replaceAll("/", "@@@");						//UNIFIED GOVERNMENT WY CO/KCK	
			} else {
				String[] spl = ow.split(",");
				if (spl.length==2) {
					String last = spl[0];
					if (last.matches("(?is)\\w+\\s+DE\\s+\\w+")) {	//GARCIA DE GOMEZ, LINDA I
						ow = last.replaceAll("\\s+", "'-'") + "," + spl[1];
					}
				}
			}
			boolean isCompany = false;
			for (int j=0;j<COMPANY_NAMES.length;j++) {
				if (ow.toUpperCase().equals(COMPANY_NAMES[j])) {
					isCompany = true;
					names[0] = names[1] = names[3] = names[4] = names[5];
					names[2] = ow;
					break;
				}
			}
			if (!isCompany) {
				names = StringFormats.parseNameNashville(ow, true);
			}
			if (NameUtils.isCompany(names[2])) {
				names[2] = names[2].replaceAll("@@@", "/");
			} else if (names[2].matches("(?is)\\w+'-'DE'-'\\w+")) {
				names[2] = names[2].replaceAll("'-'", " ");
			}
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			if (!isCompany) {
				isCompany = NameUtils.isCompany(names[2]);
			}
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					isCompany, NameUtils.isCompany(names[5]), body);
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
		
		String[] split = address.split("<br>");
		String firstAddress = split[0];	//take only the first address
		
		Matcher matcher = Pattern.compile("(?is)(.*?)\\s+KS\\s+(\\d+)").matcher(firstAddress);
		if (matcher.matches()) {
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matcher.group(2));
			firstAddress = matcher.group(1);
		}
		split = StringFormats.parseCityFromAddress(firstAddress, CITIES);
		resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), split[0]);
		firstAddress = split[1];
		
		String streetName = StringFormats.StreetName(firstAddress).trim();
		String streetNo = StringFormats.StreetNo(firstAddress);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		
		address = address.replaceAll("(?is)<br>", "; ");
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
	}
		
	public static void parseLegalSummary(ResultMap resultMap)
	{
		
		String subdivisionName = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
		if (StringUtils.isNotEmpty(subdivisionName)) {
			Matcher matcher = Pattern.compile("(?is)\\bPH(?:ASE)?\\s+(\\d+(?:\\s*&\\s*\\d+)?)\\b").matcher(subdivisionName);
			if (matcher.find()) {
				String phase = matcher.group(1);
				phase = phase.replaceAll("&", " ");
				phase = LegalDescription.cleanValues(phase, true, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			}
		}
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
			
		legalDescription = legalDescription.replaceAll("(?is)\\b(L\\d+)\\s+TO\\s+L(\\d+)\\b", "$1-$2");
		String lotExpr1 = "(?is)\\bL(\\d+(?:-\\d+)?)\\b";
		List<String> lot = RegExUtils.getMatches(lotExpr1, legalDescription, 1);
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<lot.size(); i++) {
			sb.append(lot.get(i)).append(" ");
		}
		String lotExpr2 = "(?is)\\bLOT\\s+([A-Z0-9]+)\\b";
		lot = RegExUtils.getMatches(lotExpr2, legalDescription, 1);
		for (int i=0; i<lot.size(); i++) {
			sb.append(lot.get(i)).append(" ");
		}
		String subdivisionLot = sb.toString().trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		String blockExpr1 = "(?is)\\bB(\\d+)\\b";
		List<String> block = RegExUtils.getMatches(blockExpr1, legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<block.size(); i++) {
			sb.append(block.get(i)).append(" ");
		}
		String blockExpr2 = "(?is)\\bBLK\\s+([A-Z0-9]+)\\b";
		block = RegExUtils.getMatches(blockExpr2, legalDescription, 1);
		for (int i=0; i<block.size(); i++) {
			sb.append(block.get(i)).append(" ");
		}
		String subdivisionBlock = sb.toString().trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		  
	}
	
	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		
		String baseAmount = "0.0";
		String totalDue = "0.0";
		String priorDelinquent = "0.0";
		String amountPaid = "0.0";
		
		String year = "";
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
    	Date payDate = HashCountyToIndex.getPayDate(currentInstance.getCommunityId(), "KS", "Wyandotte", DType.TAX);
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
    	year = formatter.format(payDate);
		NodeList taxList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_TaxRollGridView"));
		if (taxList.size()>0) {
			TableTag assessedTable = (TableTag)taxList.elementAt(0);
			if (assessedTable.getRowCount()>1) {
				TableRow row = assessedTable.getRows()[1];
				if (row.getColumnCount()>6) {
					year = row.getColumns()[1].toPlainTextString().trim();
					baseAmount = row.getColumns()[3].toPlainTextString().trim().replaceAll("[\\$,]", "");
					totalDue = row.getColumns()[6].toPlainTextString().trim().replaceAll("[\\$,]", "");
					amountPaid = GenericFunctions.sum(baseAmount + "+-" + totalDue, searchId);
					double amountPaidDouble = Double.parseDouble(amountPaid);
					if (amountPaidDouble<0.0d) {
						amountPaid = "0.0";
					}
				}
			}
			StringBuilder sb = new StringBuilder("0.0");
			for (int i=2;i<assessedTable.getRowCount()-1;i++) {
				if (assessedTable.getRows()[i].getColumnCount()>6) {
					sb.append("+").append(assessedTable.getRows()[i].getColumns()[6].toPlainTextString().replaceAll("[\\$,]", ""));
				}
			}
			priorDelinquent = GenericFunctions.sum(sb.toString(), searchId);
		}
		
		if (StringUtils.isNotEmpty(year)) {
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
		}
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
		resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
		
		NodeList assessedList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainAreaContentPlaceHolder_AppraisalGridView"));
		if (assessedList.size()>0) {
			TableTag assessedTable = (TableTag)assessedList.elementAt(0);
			for (int i=1;i<assessedTable.getRowCount();i++) {
				TableRow row = assessedTable.getRows()[i];
				if (row.getColumnCount()>2) {
					String yearRow = row.getColumns()[0].toPlainTextString().trim();
					if (yearRow.compareTo(year)<=0) {
						String assessedValue = row.getColumns()[2].toPlainTextString().trim();
						assessedValue = assessedValue.replaceAll("[\\$,]", "");
						resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue);
						break;
					}
				}
			}
		}
		
	}
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class ARWashingtonTR {
	
	public static final String[] CITIES = {"Elkins", "Elm Springs", "Farmington", "Fayetteville",	//cities and towns
		                                   "Goshen", "Greenland", "Johnson",
		                                   "Lincoln", "Prairie Grove", "Springdale",
		                                   "Tontitown", "West Fork", "Winslow",
		                                   "Cincinnati", "Canehill", "Summers",						//communities
		                                   "Rural"};												//not an actual city or town 
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 2) {
				String text = cols[1].toHtml();
				
				Matcher ma1 = Pattern.compile("(?is)Parcel:\\s*<a.*?>\\s*<b>([\\d-]+)\\s*</a>\\s*</B>").matcher(text);
				if (ma1.find())
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), ma1.group(1).trim());
				
				Matcher ma2 = Pattern.compile("(?is)Location Address:\\s*<B>(.*?)</B>").matcher(text);
				if (ma2.find())
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), ma2.group(1).trim());
				
				Matcher ma3 = Pattern.compile("(?is)Owner Name:\\s*<B>(.*?)</B>").matcher(text);
				if (ma3.find())
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ma3.group(1).trim());
			}

			parseNames(resultMap, searchId);
			parseAddress(resultMap);
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		boolean done = false;
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		owner = owner.trim();
		owner = owner.replaceAll("(?is)\\bOR\\b", "&");
		owner = owner.replaceAll("(?is)\\AMRS?\\b", "");
		owner = owner.replaceAll("(?is)\\bMRS?\\z", "");
		owner = owner.replaceAll("(?is),?\\s*\\**\\s*\\z", "");
		owner = owner.replaceAll("(?is)\\b(?:;\\s*)?(?:CO)?-?(TTEES?)(?:\\s+OF\\s+THE)?\\b", " $1;");
		owner = owner.replaceFirst(";\\s*\\z", "");
		owner = owner.replaceAll("(?is);\\s*(.+)\\s+(JR|SR|II|III|IV)", " & $1 $2");
		owner = owner.replaceAll(",\\s*&", " &");
		
		Matcher ma = Pattern.compile("(?is)(.+?)\\bTTEES\\b(.+)").matcher(owner);
		if (ma.find()) {
			String part1 = ma.group(1).replaceAll(";", " TR;");
			String part2 = ma.group(2);
			owner = part1 + "TRS" + part2;
		}
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		
		String[] coNames = owner.split("(?is)c/o");
		if (coNames.length==2) {
			
			done = true;
			
			names = StringFormats.parseNameNashville(coNames[0], true);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			names = StringFormats.parseNameDesotoRO(coNames[1], true);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
			
		if (!done) {
			
			if (owner.indexOf(";")==-1) {
				owner = owner.replaceFirst("(?is)\\b(TRU?ST)\\s*,", "$1 ;");
			}
			
			owner = owner.replaceAll("/", ";");
			String[] split = owner.split(";");
			for (int i=0;i<split.length;i++) {
				
				String ow = split[i].trim();
				ow = ow.replaceAll("(?is)WILSON,\\s*WILSON", "WILSON,");
				ow = ow.replaceFirst(",", "@@@COMMA@@@");
				ow = ow.replaceFirst("\\s+([^\\s]+)\\s*,", " & $1");
				ow = ow.replaceFirst("@@@COMMA@@@", ",");
								
				if (ow.split("\\s").length==1 && NameUtils.isNotCompany(ow)) //only a last name, e.g. SMITH, CECELIA ; SMITH
					continue;
				
				boolean hasAlternateName = false;
				String alternateName = "";
				
				Matcher matcher = Pattern.compile("\\((.*?)\\)").matcher(ow);
				if (matcher.find()) {
					hasAlternateName = true;
					alternateName = matcher.group(1);
				}
				
				if (NameUtils.isCompany(ow)) {
					names[2] = ow;
					names[0] = names[1] = names[3] = names[4] = names[5] = ""; 
				} else
					names = StringFormats.parseNameNashville(ow, true);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				
				if (hasAlternateName) {
					names[2] = alternateName;
					names[3] = names[4] = names[5] = "";
					suffixes[1] = "";
					type[1] = "";
					otherType[1] = "";
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				}
			}
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void parseAddress(ResultMap resultMap)  {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName().trim());
		if (StringUtils.isEmpty(address))
			return;
		
		address = address.replaceFirst("(\\d+)\\s(\\d+)\\s(NE|NW|SE|SW|N|S|E|W)", "$1 #$2 $3");		//e.g. 55 5 E 15TH ST
		
		String[] split = address.split("\\s");
		int split_len = split.length;
		for (int i=0;i<CITIES.length;i++) {
			String[] split_cities = CITIES[i].split("\\s"); 
			int len = split_cities.length;
			if (split_len>=len) {
				boolean isCity = true;
				for (int j=0;j<len&&isCity;j++)
					if (!split_cities[j].equalsIgnoreCase(split[split_len-len+j]))
						isCity = false;
				if (isCity) {										//remove city
					StringBuilder sb = new StringBuilder();
					for (int j=0;j<split_len-len;j++)
						sb.append(split[j]).append(" ");
					address = sb.toString().trim();
					sb = new StringBuilder();
					for (int j=0;j<len;j++)
						sb.append(split[split_len-len+j]).append(" ");
					String city = sb.toString().trim();
					if (!"rural".equals(city.toLowerCase()))
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				}
			}
		}
		
		String streetName = StringFormats.StreetName(address).trim();
		String streetNo = StringFormats.StreetNo(address);
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
	}

	public static void parseLegalSummary(ResultMap resultMap) {
		
		//sometimes Phase appears in Subdivision Name, e.g. BUTTERFIELD ESTATES PHASE II (PIN 815-32611-000)
		String phasePattern = "(?is)\\bPH(ASE)?\\s([\\w-]+)";
		String subdivisionName = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
		String subdivisionPhase = "";
		if (subdivisionName!=null) {
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			subdivisionName = Roman.normalizeRomanNumbersExceptTokens(subdivisionName, exceptionTokens);
			List<String> phase = RegExUtils.getMatches(phasePattern, subdivisionName, 2);
			StringBuilder sb = new StringBuilder(); 
			for (int i=0; i<phase.size(); i++) 
				sb.append(" ").append(phase.get(i));
			subdivisionPhase = sb.toString().trim();
			if (subdivisionPhase.length() != 0) {
				subdivisionPhase = LegalDescription.cleanValues(subdivisionPhase, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
			}
			//on RO appears as SUBDIVISION
			subdivisionName = subdivisionName.replaceAll("(?is)\\bS/D\\b", "SUBDIVISION");
			//SPRINGDALE OUTLOTS, SW SW (PIN 815-29424-002)
			subdivisionName = subdivisionName.replaceFirst("(?is),\\s*[NSEW]{2}\\s+[NSEW]{2}\\s*$", "");
			//620 NORTH COLLEGE AVENUE - HPR
			subdivisionName = subdivisionName.replaceAll("\\s-\\s"," ");
			//ARCADIA CONDOS (THE) - HPR
			subdivisionName = subdivisionName.replaceAll("(?is)\\(\\s*THE\\s*\\)","");
			subdivisionName = subdivisionName.replaceAll("\\s{2,}", " ");
			subdivisionName = subdivisionName.trim().toUpperCase();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
		}
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legalDescription = Roman.normalizeRomanNumbersExceptTokens(legalDescription, exceptionTokens);
		
		List<String> lot = RegExUtils.getMatches("(?is)\\bLO\\s?TS?\\s(\\w+(\\s*[-&]\\s*\\w+)*)", legalDescription, 1);
		StringBuilder sb = new StringBuilder();
		String alreadyLot = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
		if (alreadyLot!=null)
			sb.append(alreadyLot).append(" ");
		for (int i=0; i<lot.size(); i++) 
			sb.append(lot.get(i).replaceAll("[-&]", " ").replaceFirst("(\\bPT\\s*\\z)", "")).append(" ");
		String subdivisionLot = sb.toString().trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		String blockPatt = "(?is)\\bBLO?C?K?S?\\s(\\d+(?:-\\d+)*(?:\\s*&\\s*PT\\s*\\d+(?:-\\d+)*)*)";
		List<String> block = RegExUtils.getMatches(blockPatt, legalDescription, 1);
		sb = new StringBuilder();
		String alreadyBlock = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());
		if (alreadyBlock!=null)
			sb.append(alreadyBlock).append(" ");
		for (int i=0; i<block.size(); i++) 
			sb.append(block.get(i).replaceAll("(?is)[-&(PT)]", " ")).append(" ");
		String subdivisionBlock = sb.toString().trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		
		List<String> phase = RegExUtils.getMatches("(?is)\\bPH(?:ASE)?\\s([^\\s]+)", legalDescription, 1);
		sb = new StringBuilder();
		String alreadyPhase = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName());
		if (alreadyPhase!=null)
			sb.append(alreadyPhase).append(" ");
		for (int i=0; i<phase.size(); i++) 
			sb.append(phase.get(i)).append(" ");
		subdivisionPhase = sb.toString().trim();
		if (subdivisionPhase.length() != 0) {
			subdivisionPhase = LegalDescription.cleanValues(subdivisionPhase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
		}
		
		List<String> tract = RegExUtils.getMatches("(?is)\\bTRACT\\s([^\\s]+)", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<tract.size(); i++) 
			sb.append(tract.get(i)).append(" ");
		String subdivisionTract = sb.toString().trim();
		if (subdivisionTract.length() != 0) {
			subdivisionTract = LegalDescription.cleanValues(subdivisionTract, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), subdivisionTract);
		}
		
		String platBookPagePatt1 = "(?is)\\bPLAT\\s(\\d+)-(\\d+)";
		List<String> book = RegExUtils.getMatches(platBookPagePatt1, legalDescription, 1);
		List<String> page = RegExUtils.getMatches(platBookPagePatt1, legalDescription, 2);
		sb = new StringBuilder();
		String platBookPage = "";
		for (int i=0; i<book.size(); i++) 
			sb.append(" ").append(book.get(i).trim()).append("&").append(page.get(i).trim());
		String platBookPagePatt2 = blockPatt + "\\s*\\(\\s*(\\d+)\\s*-\\s*(\\d+)\\s*\\)";
		book = RegExUtils.getMatches(platBookPagePatt2, legalDescription, 2);
		page = RegExUtils.getMatches(platBookPagePatt2, legalDescription, 3);
		for (int i=0; i<book.size(); i++) 
			sb.append(" ").append(book.get(i).trim()).append("&").append(page.get(i).trim());
		platBookPage = sb.toString().trim();
		if (platBookPage.length() != 0)
		{
			platBookPage = LegalDescription.cleanValues(platBookPage, false, true);
			String[] values = platBookPage.split("\\s");
			
			ResultTable platTable = new ResultTable();			
			@SuppressWarnings("rawtypes")
			List<List> tablebodyPlat = new ArrayList<List>();
			List<String> list;
			for (int i=0; i<values.length; i++)			
			{
				String[] bookAndPage = values[i].split("&");
				list = new ArrayList<String>();
				list.add(bookAndPage[0]);
				list.add(bookAndPage[1]);
				tablebodyPlat.add(list);
			}
			String[] headerPlat = {PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()};
			platTable = GenericFunctions2.createResultTable(tablebodyPlat, headerPlat);
			if (platTable != null){
				resultMap.put("PropertyIdentificationSet", platTable);
			}
		}
	}
	
	public static void parseTaxes(String detailsHtml, NodeList nodeList, ResultMap resultMap, long searchId) {
		
		String baseAmount = "0.0";
		String amountDue = "0.0";
		String amountPaid = "0.0";
		String priorDelinquent = "0.0";
		Boolean isDelinquentWithoutSum = false;
		
		int tmpYear = 0;
		Matcher ma = Pattern.compile("(?is)Last Appraised:\\s*<B>(\\d{4})</B>").matcher(detailsHtml);
		if (ma.find()) {
			tmpYear = Integer.parseInt(ma.group(1)); 		//year form "Last Appraised"
		}
		
		NodeList nodeTaxes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
		TableTag mainTable = null;
		TableTag taxHistoryTable = null;
		TableTag taxPaymentHistoryTable = null;
		if (nodeTaxes!=null) {
			for (int i=0;i<nodeTaxes.size();i++) {
				if (nodeTaxes.elementAt(i).toHtml().contains("Parcel Number"))
					mainTable = (TableTag)nodeTaxes.elementAt(i);	
				else if (nodeTaxes.elementAt(i).toHtml().contains("Tax History")) 
					taxHistoryTable = (TableTag)nodeTaxes.elementAt(i);
				else if (nodeTaxes.elementAt(i).toHtml().contains("Tax Payment History")) 
					taxPaymentHistoryTable = (TableTag)nodeTaxes.elementAt(i);
				if (taxHistoryTable!=null && taxPaymentHistoryTable!=null)
					break;
			}
		}
		
		int max = 0;
		int max1 = 0;					//the most recent year from Tax History table
		if (taxHistoryTable!=null && taxHistoryTable.getRowCount()>2) 
			max1 = Integer.parseInt(taxHistoryTable.getRow(2).getColumns()[0].toPlainTextString().trim());	
		int max2 = 0;					//the most recent year from Tax Payment History table
		if (taxPaymentHistoryTable!=null && taxPaymentHistoryTable.getRowCount()>2) 
			max2 = Integer.parseInt(taxPaymentHistoryTable.getRow(2).getColumns()[0].toPlainTextString().trim());	
		
		String year = "";
		max = max1>=max2?max1:max2;
		if (max>tmpYear) 
			year = Integer.toString(max);
		else if (tmpYear-1==max)
			year = Integer.toString(max);
		else
			year = Integer.toString(tmpYear);
		
		float taxableValue = 0.0f;
		
		if (mainTable!=null) {
			int line = mainTable.getRowCount()-2;
			if (mainTable.getRow(line).getColumnCount()==3)
				taxableValue = Float.parseFloat(mainTable.getRow(line).getColumns()[2].toPlainTextString().replaceAll("[\\$,]", ""));
		}
		
		if (taxHistoryTable!=null) {										//Tax History table present										
			int offset = 3;													//the row where previous years start
			if (taxHistoryTable.getRowCount()>2) {
				if (tmpYear-max>=2) {										//tax year not present in the table		
					baseAmount = "0.0";
					amountDue = "0.0";
					amountPaid = "0.0";
					offset = 2;
				} else {
					TableRow firstRow = taxHistoryTable.getRow(2);
					if (firstRow.getColumnCount()==11) {
						baseAmount = firstRow.getColumns()[8].toPlainTextString().trim().replaceAll("[,\\$]", "");
						amountPaid = firstRow.getColumns()[9].toPlainTextString().trim().replaceAll("[,\\$]", "");
						String status = firstRow.getColumns()[10].toPlainTextString().trim().toLowerCase();
						status = status.trim();
						if ("paid".equals(status)) 
							amountDue = "0.0";
						else {
							float ba = Float.parseFloat(firstRow.getColumns()[8].toPlainTextString().trim().replaceAll("[,\\$]", ""));
							float ap = Float.parseFloat(firstRow.getColumns()[9].toPlainTextString().trim().replaceAll("[,\\$]", ""));
							if (ba>ap) {	//unpaid
								amountDue = String.valueOf(ba-ap);
							}
						} 
							
					} else if (firstRow.toPlainTextString().contains("D E L I N Q U E N T")) {
						int i = offset;
						float value = 0.0f;
						float paid = 0.0f;
						while (taxHistoryTable.getRow(i).toPlainTextString().contains("D E L I N Q U E N T")) {
							i++;
						}
						if (i<taxHistoryTable.getRowCount() && taxHistoryTable.getRow(i).getColumnCount()==11) {
							float olderYearTaxableValue = Float.parseFloat(
									taxHistoryTable.getRow(i).getColumns()[1].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearAdValoremTax = Float.parseFloat(
									taxHistoryTable.getRow(i).getColumns()[2].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearImpDistTax = Float.parseFloat(
									taxHistoryTable.getRow(i).getColumns()[3].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearTimberTax = Float.parseFloat(
									taxHistoryTable.getRow(i).getColumns()[4].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearVoluntaryTax = Float.parseFloat(
									taxHistoryTable.getRow(i).getColumns()[5].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearA79Credit = Float.parseFloat(
									taxHistoryTable.getRow(i).getColumns()[6].toPlainTextString().trim().replaceAll("[\\$,]", ""));	
							if (olderYearTaxableValue!=0.0f)
								value = taxableValue*olderYearAdValoremTax/olderYearTaxableValue
									-(olderYearImpDistTax+olderYearTimberTax+olderYearVoluntaryTax+olderYearA79Credit);
						} else
							isDelinquentWithoutSum = true;
						String currentYear = firstRow.getColumns()[0].toPlainTextString().trim();
						if (taxPaymentHistoryTable!=null) {
						for (int j=0;j<taxPaymentHistoryTable.getRowCount();j++) {
								TableRow row = taxPaymentHistoryTable.getRow(j); 
								if (row.getColumnCount()==9 && currentYear.equals(row.getColumns()[0].toPlainTextString().trim()))
									paid += Float.parseFloat(row.getColumns()[8].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							}
						}
						if (value>paid) {
							baseAmount = String.valueOf(value);
							amountPaid = String.valueOf(paid);
							amountDue = String.valueOf(value-paid);
						} else {
							baseAmount = amountDue = String.valueOf(value);
							amountPaid = "0.0";
						}
					}
				}
				StringBuilder sb = new StringBuilder();
				sb.append("0.0");
				for (int i=offset;i<taxHistoryTable.getRowCount();i++) {
					TableRow row = taxHistoryTable.getRow(i);
					if (row.getColumnCount()==11) {
						String status = row.getColumns()[10].toPlainTextString().trim().toLowerCase();
						status = status.trim();
						if ("".equals(status)) {
							float ba = Float.parseFloat(row.getColumns()[8].toPlainTextString().trim().replaceAll("[,\\$]", ""));
							float ap = Float.parseFloat(row.getColumns()[9].toPlainTextString().trim().replaceAll("[,\\$]", ""));
							if (ba>ap) {	//unpaid
								sb.append("+").append(String.valueOf(ba-ap));
							}
						}
						if ("unpaid".equals(status))
							sb.append("+").append(row.getColumns()[8].toPlainTextString().trim().replaceAll("[,\\$]", ""));
					}
				}
				
				int i = offset;
				while (i<taxHistoryTable.getRowCount()) {
					//find consecutive delinquent lines 
					if (taxHistoryTable.getRow(i).toPlainTextString().contains("D E L I N Q U E N T")) {
						int lineBeforeDelinquent = i-1;
						isDelinquentWithoutSum = false;
						int numberOfDelinquentYears = 0;
						while (lineBeforeDelinquent>=offset &&
								taxHistoryTable.getRow(lineBeforeDelinquent).getColumnCount()!=11)
							lineBeforeDelinquent--;
						if (lineBeforeDelinquent<offset)
							lineBeforeDelinquent = -1;
						while (taxHistoryTable.getRow(i).toPlainTextString().contains("D E L I N Q U E N T")) {
							i++;
							numberOfDelinquentYears++;
						}
						i--;
						int lineAfterDelinquent = i+1;
						while (lineAfterDelinquent<taxHistoryTable.getRowCount() &&
								taxHistoryTable.getRow(lineAfterDelinquent).getColumnCount()!=11)
							lineAfterDelinquent++;
						if (lineAfterDelinquent>=taxHistoryTable.getRowCount())
							lineAfterDelinquent = -1;
						if (lineBeforeDelinquent==-1 && lineAfterDelinquent==-1) {	//delinquent, but the sum is unknown
							isDelinquentWithoutSum = true;
							priorDelinquent = "0.0";
						} else if (lineAfterDelinquent==-1) {						//the last lines are delinquent
							float olderYearBaseAmount = Float.parseFloat(
									taxHistoryTable.getRow(lineBeforeDelinquent).getColumns()[8].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							sb.append("+").append(Float.toString(numberOfDelinquentYears*olderYearBaseAmount));
						} else {
							if (lineBeforeDelinquent!=-1)
								taxableValue = Float.parseFloat(
									taxHistoryTable.getRow(lineBeforeDelinquent).getColumns()[1].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearTaxableValue = Float.parseFloat(
									taxHistoryTable.getRow(lineAfterDelinquent).getColumns()[1].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearAdValoremTax = Float.parseFloat(
									taxHistoryTable.getRow(lineAfterDelinquent).getColumns()[2].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearImpDistTax = Float.parseFloat(
									taxHistoryTable.getRow(lineAfterDelinquent).getColumns()[3].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearTimberTax = Float.parseFloat(
									taxHistoryTable.getRow(lineAfterDelinquent).getColumns()[4].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearVoluntaryTax = Float.parseFloat(
									taxHistoryTable.getRow(lineAfterDelinquent).getColumns()[5].toPlainTextString().trim().replaceAll("[\\$,]", ""));
							float olderYearA79Credit = Float.parseFloat(
									taxHistoryTable.getRow(lineAfterDelinquent).getColumns()[6].toPlainTextString().trim().replaceAll("[\\$,]", ""));	
							float value = 0.0f;
							if (olderYearTaxableValue!=0.0f)
								value = taxableValue*olderYearAdValoremTax/olderYearTaxableValue
									-(olderYearImpDistTax+olderYearTimberTax+olderYearVoluntaryTax+olderYearA79Credit);
							sb.append("+").append(Float.toString(value*numberOfDelinquentYears));
							if (year.equals(Integer.toString(max1)) && taxHistoryTable.getRow(2).toPlainTextString().contains("D E L I N Q U E N T")) {
								baseAmount = Float.toString(value);
								amountDue = Float.toString(value);
								amountPaid = "0.0";
							}
						}
					}
					i++;
				}
				priorDelinquent = GenericFunctions.sum(sb.toString(), searchId);
			}
		} else if (taxPaymentHistoryTable!=null) {							//only Tax Payment History table present
			if (taxPaymentHistoryTable.getRowCount()>2) {
				if (tmpYear-max>=2) {										//tax year not present in the tables		
					baseAmount = "0.0";
					amountPaid = "0.0";
				} else {
					TableRow firstRow = taxPaymentHistoryTable.getRow(2);
					if (firstRow.getColumnCount()==9) {
						baseAmount = firstRow.getColumns()[8].toPlainTextString().trim().replaceAll("[,\\$]", "");
						amountPaid = baseAmount;
					}
				}
				amountDue = "0.0";
				priorDelinquent = "0.0";
			}	
		} else if (taxHistoryTable==null && taxPaymentHistoryTable==null) {	//neither table present
			//base amount, amount due, amount paid and prior delinquent remain 0.0
		}
		
		Date dueDate = HashCountyToIndex.getDueDate(InstanceManager.getManager()

				.getCurrentInstance(searchId).getCurrentCommunity().getID().intValue(), 
				"AR", "Washington", DType.TAX); 
		Date now = new Date();
        if (now.compareTo(dueDate)>=0) {
        	priorDelinquent += "+" + amountDue;
        	amountDue = "0.0";
        	priorDelinquent = GenericFunctions.sum(priorDelinquent, searchId);
        }
		
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
		resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
		if (isDelinquentWithoutSum)
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "-1");
		else
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
		
	}
}

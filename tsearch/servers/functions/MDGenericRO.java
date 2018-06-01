package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
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
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class MDGenericRO {
	
	public static final String[] CHARLES_CITIES = {"INDIAN HEAD", "LA PLATA",		//incorporated
		   "PORT TOBACCO VILLAGE", "PORT TOBACCO",
		   "BENNSVILLE", "BRYANS ROAD", "HUGHESVILLE", "POTOMAC HEIGHTS",			//unincorporated
		   "SAINT CHARLES", "ST CHARLES", "WALDORF",						 
		   "BEL ALTON", "BENEDICT", "BRYANTOWN", "COBB ISLAND",						//other unincorporated places
		   "DENTSVILLE", "FAULKNER", "GRAYTON", "IRONSIDES",
		   "ISSUE", "MALCOLM", "MARBURY", "MORGANTOWN",
		   "MOUNT VICTORIA", "NANJEMOY", "NEWBURG", "PISGAH",
		   "POMFRET", "POPES CREEK", "POMONKEY", "RIPLEY",
		   "RISON", "ROCK POINT", "SWAN POINT", "WELCOME",
		   "WHITE PLAINS"};
	public static final String[] DORCHESTER_CITIES = {"CAMBRIDGE", "BROOKVIEW",		//INCORPORATED
		   "CHURCH CREEK ", "EAST NEW MARKET", "ENMKT", "ELDORADO", 
		   "GALESTOWN", "HURLOCK", "SECRETARY", "VIENNA"};

	
	public static final int NAME_INTERMEDIARY = 0;
	public static final int NAME_DETAILS = 1;
	
	public static final int BODYLINE_LEN = 14;
	
	public static final String LOT_PATTERN1 = "(?is)\\bLS\\s+(\\d+(?:\\s+\\d+)*\\s*&\\s*\\d+)";
	public static final String LOT_PATTERN2 = "(?is)\\bLS?\\s*(\\d+(?:\\s*[-&]\\s*\\d+)*)\\s+";
	public static final String LOT_PATTERN3 = "(?is)\\bL\\s*(\\d+)";
	public static final String LOT_PATTERN4 = "(?is)\\bLO?T\\s*(\\d+)";
	public static final String BLOCK_PATTERN1 = "(?is)\\bBlock:\\s+([\\d\\w ]+)";
	public static final String BLOCK_PATTERN2 = "(?is)\\bBLK\\s+(\\d+|[A-Z])";
	public static final String BLOCK_PATTERN3 = "(?is)\\bBLKS((?:\\s+(?:\\d+|[A-Z])\\s*TO\\s*(?:\\d+|[A-Z]))+)";
	public static final String SECTION_PATTERN = "(?is)\\bSEC\\s+(\\d+(?:-[A-Z])?|[A-Z](?:-\\d+[A-Z])?(?:\\s+\\d+-[A-Z])?)";
	public static final String UNIT_PATTERN = "(?is)\\bUNIT\\s+(\\d+|[A-Z])";
	public static final String BLDG_PATTERN = "(?is)\\bBLDG\\s+(\\d+)";
	public static final String PARCEL_PATTERN1 = "(?is)\\bP(?:ARCE)?L\\s+(\\d+(?:-\\d+)+)";
	public static final String PARCEL_PATTERN2 = "(?is)\\bP(?:ARCE)?L\\s+(\\d+(?:-?[A-Z])?|[A-Z])\\b";
	public static final String UNIPATT1 = "&([A-Z])";
	public static final String UNIPATT2 = "(\\d+(?:-\\d+)?)";
	
	@SuppressWarnings("rawtypes")
	public static ResultMap parseIntermediaryRow(String row, String county) {

		List<List> bodyPIS = new ArrayList<List>();
		List<String> bodyLine = new ArrayList<String>();
		
		ResultMap m = new ResultMap();

		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(row, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tdList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), false);
			
			if (tdList.size()==6) {
				String bp = tdList.elementAt(3).toHtml();
				Matcher ma = Pattern.compile(ro.cst.tsearch.servers.types.MDGenericRO.BOOK_PAGE_PATT1).matcher(bp);
				if (ma.find()) {
					String book = ma.group(2).trim();
					String page = ma.group(3).trim();
					page = page.replaceFirst("-.*", "");
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
				}
				
				String recordedDate = tdList.elementAt(0).toPlainTextString().trim();
				if (!StringUtils.isEmpty(recordedDate)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
				}
				
				String docType = tdList.elementAt(2).toPlainTextString().trim();
				docType = ro.cst.tsearch.servers.types.MDGenericRO.cleanDoctype(docType);
				if (!StringUtils.isEmpty(docType)) {
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
				}
				
				String legal = tdList.elementAt(4).toPlainTextString().trim();
				if (!StringUtils.isEmpty(legal)) {
					m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
				}
				
				StringBuilder grantor = new StringBuilder();
				StringBuilder grantee = new StringBuilder();
				Node node = tdList.elementAt(1).getFirstChild();
				if (node instanceof TableTag) {
					TableTag table = (TableTag)node;
					for (int i=0;i<table.getRowCount();i++) {
						String s = table.getRow(i).toPlainTextString();
						Matcher mat = Pattern.compile("(?is)(.*?)\\bTax\\s+Account\\s+No\\.:\\s*([^\\s]+)").matcher(s);
						if (mat.find()) {
							s = mat.group(1);
							s = ro.cst.tsearch.servers.types.MDGenericRO.cleanPin(s);
							bodyLine.add(mat.group(2));
						}
						s = s.replaceFirst("\n\\s*\\bCapacity:\\s*TRUS\\b(?:\\s+ETC\\b)?", " TR");
						s = s.replaceFirst("\n\\s*\\bCapacity:\\s*TST\\b", " TR");
						s = s.replaceFirst("\n\\s*\\bCapacity:\\s*(" + 
							ro.cst.tsearch.extractor.xml.GenericFunctions1.nameTypeString + ")(?:\\s+ETC\\b)?", " $1");
						s = s.replaceFirst("\n\\s*\\bCapacity:.*", "");
						s = s.replaceAll("\\s", " ");
						s = s.replaceAll("\\s{2,}", " ");
						s = s.trim();
						if (s.matches("(?is)\\bGrantor:.*")) {
							grantor.append(s.replaceFirst("(?is)\\bGrantor:", "").trim()).append("<br>");
						} else if (s.matches("(?is)\\bGrantee:.*")) {
							grantee.append(s.replaceFirst("(?is)\\bGrantee:", "").trim()).append("<br>");
						}
					}
				}
				
				String grantorString = grantor.toString().replaceFirst("<br>$", "");
				String granteeString = grantee.toString().replaceFirst("<br>$", "");
				if (!StringUtils.isEmpty(grantorString)) {
					m.put("tmpGrantor", grantorString);
				}
				if (!StringUtils.isEmpty(granteeString)) { 
					m.put("tmpGrantee", granteeString);
				}
				
			}
			
			int l = bodyLine.size();
			for (int i=l;i<2;i++) {
				bodyLine.add("");	//PARCEL_ID and SUBDIVISION_BLOCK, if not already added
			}
			bodyPIS.add(bodyLine);
			
			parseNames(m, NAME_INTERMEDIARY);
			parseLegals(m, bodyPIS, county);
			
			String[] header = {PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(),
			           		   PropertyIdentificationSetKey.CITY.getShortKeyName(),
							   PropertyIdentificationSetKey.STREET_NAME.getShortKeyName(),
			           		   PropertyIdentificationSetKey.STREET_NO.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(),
					           PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_COND.getShortKeyName(),
					           PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(),
					           PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()};
			ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
			m.put("PropertyIdentificationSet", rt);
			
			m.removeTempDef();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return m;

	}
	
	public static ResultMap parseIntermediaryRow(TableRow row) {
		
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
				
		TableColumn[] cols = row.getColumns();
		if (cols.length==5) {
			String book = cols[2].toPlainTextString().trim();
			book = book.replaceFirst("^.*\\s", "");
			String s = cols[2].toHtml();
			String page = StringUtils.extractParameter(s, "b_sp=([^&?]*)");
			m.put(SaleDataSetKey.BOOK.getKeyName(), book);
			m.put(SaleDataSetKey.PAGE.getKeyName(), page);
		}
		return m;
	}
	
	public static String cleanName(String name) {
		
		name = name.replaceFirst("^\\s*-+\\s*$", "");
		
		name = name.replaceAll("(?is)\\bDIR(\\s+OF)?\\s+FIN(ANCE)?\\b", "");
		name = name.replaceAll("(?is)\\b/?IND\\s+&\\s+AIF\\b", "");
		name = name.replaceAll("(?is)\\bCO\\s+P(ART)?(NER)?\\b", "");
		name = name.replaceAll("(?is)\\bBY\\s+AIF\\b", "");
		name = name.replaceAll("(?is),(BY\\s+)?AIF\\b", "");
		name = name.replaceAll("(?is)-(?:BY\\s+)?(A/?F|P/A)\\b", "");
		name = name.replaceAll("(?is)-(?:SUB\\s+)?(TR)\\b", " $1");					//substitute trustee
		name = name.replaceAll("(?is)-INDIV\\s*&\\s*AI?F\\b", "");
		name = name.replaceAll("(?is)-BY\\s*ATTY\\b", "");
		
		if (!isCompany(name)) {
			name = name.replaceAll("(?is)([A-Z]+)\\s+([A-Z]+)\\s*/\\s*AKA\\s*([A-Z]+)\\s+([A-Z]+)", "$1 $2 $4 & $3 $4");
			
			name = name.replaceAll("(?is)\\bC/P\\b", ""); 					//child/parent
			name = name.replaceAll("(?is)[/-]\\s*DECD?\\b", ""); 			//deceased
			name = name.replaceAll("(?is)\\bDEC(EASE)?D?\\b", ""); 			//deceased
			name = name.replaceAll("(?is)\\bEXT?RX?\\b", ""); 				//executor,executrix
			name = name.replaceAll("(?is)\\bCUST\\b", ""); 					//custodian
			name = name.replaceAll("(?is)\\bIND\\s+&\\s+(TRS?)\\b", "$1"); 	//individual
			name = name.replaceAll("(?is)\\bMC[DL]\\b", "");
			name = name.replaceAll("(?is)\\bCOMM\\b", ""); 					//commissioner
			name = name.replaceAll("(?is)\\bAKA\\b", "");					//also known as
			name = name.replaceAll("(?is)\\bS/K/A\\b", "");
			name = name.replaceAll("(?is)/\\s*AIF\\b", "");
			name = name.replaceAll("(?is)\\bJ/P\\b", "");					//joint property
			name = name.replaceAll("(?is)\\bAGENT\\b", "");
			name = name.replaceAll("(?is)\\bPERS\\b", "");					//person
			name = name.replaceAll("(?is)\\bDIR\\b", "");					//director
			name = name.replaceAll("(?is)\\bGDN\\b", "");
			
			name = name.replaceAll("[/-]\\s*(" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString + ")", " $1");
		}
		
		name = name.replaceAll("(?is)\\bPERS?(\\s+REP)?\\b", "");
		name = name.replaceAll("(?is)\\bP/R\\b", "");
		name = name.replaceAll("(?is)[/-](.*?)\\bP\\s*R\\b", " $1");
		
		name = name.replaceAll("[/-]\\s*(" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameTypeString + ")", " $1");
		name = name.replaceAll("[/-]\\s*(" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameOtherTypeString + ")", " $1");
		
		name = name.replaceAll("\\s{2,}", " ");
		
		return name.trim();
	}
	
	public static boolean isCompany(String s) {
		List<String> list = new ArrayList<String>();
		list.add("LAIRD & SMALL");
		if (list.contains(s.toUpperCase())) {
			return true;
		}
		return NameUtils.isCompany(s);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, ResultMap resultMap, int format) {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split("(?is)<br>");
		
		for (int i=0;i<split.length;i++) {
			
			String name = split[i];
			name = cleanName(name);
				
			if (!StringUtils.isEmpty(name)) {
				if (isCompany(name)) {
					names = new String[]{"", "", "", "", "", ""};
					names[2] = name;
				} else if (format==NAME_INTERMEDIARY) {
					names = StringFormats.parseNameNashville(name, true);
				} else if (format==NAME_DETAILS) {
					names = StringFormats.parseNameDesotoRO(name, true);
				}
					
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
					suffixes[1], type, otherType, isCompany(names[2]), isCompany(names[5]), list);
			}
			
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, int format) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpPartyGtor = (String)resultMap.get("tmpGrantor");
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			parseName(tmpPartyGtor, grantor, resultMap, format);
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenateNames(grantor));
		
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpPartyGtee = (String)resultMap.get("tmpGrantee");
		if (StringUtils.isNotEmpty(tmpPartyGtee)){
			parseName(tmpPartyGtee, grantee, resultMap, format);
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenateNames(grantee));
		
		resultMap.removeTempDef();
	}

	@SuppressWarnings("rawtypes")
	public static String concatenateNames(ArrayList<List> nameList) {
		String result = "";
		
		List<String> names = new ArrayList<String>();
		for (List list: nameList) {
			if (list.size()>3) {
				String s = list.get(3) + ", " + list.get(1) + " " + list.get(2);
				if (!names.contains(s)) {
					names.add(s);
				}
			}
		}
		
		StringBuilder resultSb = new StringBuilder();
		for (String s: names) {
			resultSb.append(s).append(" / ");
		}
		result = resultSb.toString().replaceAll("/\\s*,\\s*/", " / ").replaceAll(",\\s*/", " /").
			replaceAll("\\s{2,}", " ").replaceAll("/\\s*$", "").trim();
		return result;
	}
	
	public static boolean hasLegal(String s) {
		Matcher ma1 = Pattern.compile(LOT_PATTERN1).matcher(s);
		if (ma1.find()) {
			return true;
		}
		Matcher ma2 = Pattern.compile(LOT_PATTERN2).matcher(s);
		if (ma2.find()) {
			return true;
		}
		Matcher ma2_2 = Pattern.compile(LOT_PATTERN3).matcher(s);
		if (ma2_2.find()) {
			return true;
		}
		Matcher ma2_3 = Pattern.compile(LOT_PATTERN4).matcher(s);
		if (ma2_3.find()) {
			return true;
		}
		Matcher ma3 = Pattern.compile(BLOCK_PATTERN1).matcher(s);
		if (ma3.find()) {
			return true;
		}
		Matcher ma4 = Pattern.compile(BLOCK_PATTERN2).matcher(s);
		if (ma4.find()) {
			return true;
		}
		Matcher ma4_2 = Pattern.compile(BLOCK_PATTERN3).matcher(s);
		if (ma4_2.find()) {
			return true;
		}
		Matcher ma5 = Pattern.compile(SECTION_PATTERN).matcher(s);
		if (ma5.find()) {
			return true;
		}
		Matcher ma6 = Pattern.compile(UNIT_PATTERN).matcher(s);
		if (ma6.find()) {
			return true;
		}
		Matcher ma7 = Pattern.compile(BLDG_PATTERN).matcher(s);
		if (ma7.find()) {
			return true;
		}
		Matcher ma8 = Pattern.compile(PARCEL_PATTERN1).matcher(s);
		if (ma8.find()) {
			return true;
		}
		Matcher ma9 = Pattern.compile(PARCEL_PATTERN2).matcher(s);
		if (ma9.find()) {
			return true;
		}
		return false;
	}
	
	public static String isAddress(String s, int len, String[] split, String county) {
		if (len>1) {
			if (!s.matches("(?is).+?\\bN/S\\b.+") && !hasLegal(s)) {
				if (Normalize.isSuffix(split[len-1]) && !s.matches("(?is).*\\b\\d(\\.\\d+)\\s*AC\\b.*") &&
						!s.matches("(?is).*\\bVARIOUS\\s+L\\s+O\\s+G\\b.*") && (len>2 && !"THE".equals(split[len-2]))) {
					return s;
				} else if (len>2 && Normalize.isSuffix(split[len-2]) && !split[len-1].matches("\\d+/(\\d|-)") && !"PT".equalsIgnoreCase(split[len-2])) {
					String oldS = s;
					s = s.replaceFirst("\\s+" + UNIPATT1 + "$", " #$1");
					s = s.replaceFirst("\\s+" + UNIPATT2 + "$", " #$1");
					if (!oldS.equals(s)) {
						return s;
					} else if ("Charles".equals(county)) {
						String addressAndCity[] = StringFormats.parseCityFromAddress(s, CHARLES_CITIES);
						if (StringUtils.isNotEmpty(addressAndCity[0])) {	//has city
							return s;
						}
					} else if ("Dorchester".equals(county)) {
						String addressAndCity[] = StringFormats.parseCityFromAddress(s, DORCHESTER_CITIES);
						if (StringUtils.isNotEmpty(addressAndCity[0])) {	//has city
							return s;
						}
					}	
				} else if (len==2) {
					if (Normalize.isCompositeNumber(split[0]) && split[1].matches("\\w+")
							 && !split[1].matches("(?is)LOTS?") && !split[1].matches("(?is)P(ARCE)?L")) {
						if (!(split[0].matches("\\d+") && split[1].matches("\\d+"))) {
							return s;
						}
					} else if (Normalize.isSuffix(split[1])) {
						return s;
					}
				} else if (len==3) {
					if (Normalize.isCompositeNumber(split[0]) && Normalize.isDirectional(split[1]) 
							&& split[2].matches("\\w+")) {
						return s;
					}
				} else if (len==4) {
					if (Normalize.isCompositeNumber(split[0]) && Normalize.isDirectional(split[1])  
							&& split[2].matches("\\w+") && Normalize.isDirectional(split[3])) {
						return s;
					}
				}
			}
		}
		return null;
	}
	
	public static String processAddress(List<String> bodyLine, String s, String county, StringBuilder lot) {
		
		s = s.replaceFirst("\\*\\d{2}/\\d{2}/\\d{4}\\s*$", "");
		s = s.replaceFirst("(?is)\\s*\\d+/(\\d+|-)\\s*$", "");
		s = s.replaceFirst("(?is).*\\b\\d\\.\\d+\\s*AC\\b\\s+[NSEW]{1,2}/[NSEW]{1,2}", "");
		s = s.replaceFirst("\\s*&$", "");
		s = s.replaceFirst("(?is).*?\\b\\d+/\\d+\\b", "");
		s = s.replaceAll("(?is).*?\\bK/?A", "");
		s = s.replaceAll("(?is)\\bREAL\\s*&\\s*PERS(ONAL)?(\\s+F\\s*S)?\\b", "");
		s = s.replaceAll("(?is)^\\s*\\d+D\\b", "");
		s = s.replaceAll("(?is)\\b([A-Z]/[A-Z]\\s+)?PARCEL\\s+PT\\b", "");
		s = s.replaceAll("(?is)\\b\\d+\\s+PARCELS?\\b", "");
		s = s.replaceAll("(?is)/[A-Z]+\\s*$", "");
		
		if (StringUtils.isEmpty(s)) {
			return null;
		}
		
		s = s.trim();
		String pin = "";
		Matcher ma1 = Pattern.compile("(?is)(\\d{10,12})\\s+AD(\\d+)(.+)").matcher(s);
		if (ma1.matches()) {
			String[] splt = ma1.group(3).split("\\s+");
			int idx = splt.length-1;
			if (splt[idx].matches("(?is)\\bLB\\d+\\b")) {
				idx--;
			}
			if (Normalize.isSuffix(splt[idx])) {
				pin = ma1.group(1);
				s = ma1.group(2) + " " + ma1.group(3);
			}
		} else {
			Matcher ma2 = Pattern.compile("(?i)(\\d{10,12})\\s+([A-Z]+)\\s+([A-Z]+)").matcher(s);
			if (ma2.matches() && Normalize.isSuffix(ma2.group(3))) {
				pin = ma2.group(1);
				s = ma2.group(2) + " " + ma2.group(3);
			}
		} 
		if (!StringUtils.isEmpty(pin)) {
			if ("Baltimore".equals(county)) {
				if (bodyLine.size()>0) {
					if (bodyLine.size()>0) {
						bodyLine.set(0, pin);
					}
				}
			}
		}
		
		String[] sep = new String[]{"&", "(?is)[NSEW]{1,2}/[NSEW]{1,2}"};
		for (int i=0;i<sep.length;i++) {
			String[] spl = s.split(sep[i]);
			if (spl.length==2) {
				String[] spl1 = spl[0].trim().split("\\s+");
				String[] spl2 = spl[1].trim().split("\\s+");
				if (spl1.length>1 && spl2.length>1 &&
						(Normalize.isSuffix(spl1[spl1.length-1]) || spl1[spl1.length-1].trim().startsWith("#")) && 
						(Normalize.isSuffix(spl2[spl2.length-1]) || spl2[spl2.length-1].trim().startsWith("#")) ) {
					if (i==0) {
						s = spl[0];
					} else if (i==1) {
						s = spl[1];
					}
				}
			}
		}
		
		s = s.trim();
		
		if (s.matches("(?is)\\d+\\s+THE\\s+ALAMEDA")) {
			return s;
		}
		
		String[] split = s.split("\\s+");
		
		int len = split.length;
		if (split.length>=4) {
			int middle = len/2;
			if (middle+1!=len-1 && Normalize.isSuffix(split[len-1])) {
				if (Normalize.isSuffix(split[middle]) || Normalize.isSuffix(split[middle-1]) || Normalize.isSuffix(split[middle+1])) {
					return null;
				}
			}
		}
		
		if ("DISTRICT".equalsIgnoreCase(split[len-1])) {
			return null;
		}
		
		if ("Charles".equals(county)) {
			if (!Normalize.isCompositeNumber(split[0])) {
				return null;
			}
		}
		
		String res = isAddress(s, len, split, county);
		if (res!=null) {
			return res;
		}
		
		s = s.replaceFirst("\\b\\d+D.+", "").trim();	//143 MAIN STREET 4DSMITH PROPERTY (Baltimore Book 1, p. 189)
		split = s.split("\\s+");
		len = split.length;
		res = isAddress(s, len, split, county);
		if (res!=null) {
			return res;
		}
		
		if ("Dorchester".equals(county)) {			//AD/IMPS 2 AC N/S CROCHERON RD/108/163 (Book 322, pp. 96-100)
			String ss = s.replaceFirst("(?is).*?\\bN/S\\b", "").replaceFirst("/.*", "").trim();
			split = ss.split("\\s+");
			len = split.length;
			res = isAddress(ss, len, split, county);
			if (res!=null) {
				return res;
			}
			Matcher maLot = Pattern.compile(LOT_PATTERN4).matcher(s);
			if (maLot.find()) {
				lot.append(" ").append(maLot.group(0));
				ss = s.replaceFirst(".*" + LOT_PATTERN4, "").trim();
				split = ss.split("\\s+");
				len = split.length;
				res = isAddress(ss, len, split, county);
				if (res!=null) {
					return res;
				}
			}
		}
		
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseLegals(ResultMap resultMap, List<List> bodyPIS, String county) {
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription = legalDescription.replaceAll("(?is)\\b(BLOCK:)\\s+", "$1 ");
		
		String[] split = legalDescription.split("\n");
		for (int i=0;i<split.length;i++) {
			if (!StringUtils.isEmpty(split[i])) {
				if (i==0) {
					parseLegal(resultMap, split[i], bodyPIS, county);
				} else {
					List<List> newBodyPIS = new ArrayList<List>();
					parseLegal(resultMap, split[i], newBodyPIS, county);
					for (int j=0;j<newBodyPIS.size();j++) {
						List<String> l = (List<String>)newBodyPIS.get(j);
						boolean found = false;
						for (int k=0;k<l.size();k++) {
							if (!StringUtils.isEmpty(l.get(k))) {
								found = true;
								break;
							}
						}
						if (found) {
							bodyPIS.add(l);
						}
					}
				}
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseLegal(ResultMap resultMap, String legalDescription, List<List> bodyPIS, String county) {
		
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription = legalDescription.replaceAll("(?is)\\b(BLOCK:)\\s*", "$1 ");
		legalDescription = legalDescription.replaceAll("(?is)\\b(SEC)(\\d+)\\b", "$1 $2");
		legalDescription = legalDescription.replaceAll("(?is)\\b(CONDO)(UNIT)(\\b|\\d+)", "$1 $2 $3");
		legalDescription = legalDescription.replaceAll("(?is)\\b(UNIT)(\\d+)\\b", "$1 $2");
		legalDescription = legalDescription.replaceAll("(?is)\\b(PK)(PT)\\b", "$1 $2");
		legalDescription = legalDescription.replaceAll("(?is)\\b(EST)(PT)\\b", "$1 $2");
		
		legalDescription = legalDescription.replaceAll("(?is)\\d+.*?\\s+DISTRICT", "");
		legalDescription = legalDescription.replaceAll("(?is)DISTRICT\\s*\\d+", "");
		
		if ("Charles".equals(county) || "Dorchester".equals(county)) {
			String[] cities = CHARLES_CITIES;
			if ("Dorchester".equals(county)) {
				cities = DORCHESTER_CITIES;
				legalDescription = legalDescription.replaceAll("(?is)\\b(E)\\s*(NMKT)\\b", "$1$2 ");
			}
			for (int i=0;i<cities.length;i++) {
				Matcher ma = Pattern.compile("(?is)(.+)\\s+([^\\s]+)(" + cities[i] + ")").matcher(legalDescription);
				if (ma.find()) {
					legalDescription = ma.group(1) + " " + ma.group(2) + " " + ma.group(3);
					break;
				}
			}
		}
		
		String platBook = "";
		String platNo = "";
		String pattPlatBookPage = "(?is)\\bMAP\\s*(\\d+)\\s*P\\s*(\\d+)\\b";
		Matcher maPlatBookPage = Pattern.compile(pattPlatBookPage).matcher(legalDescription);
		if (maPlatBookPage.find()) {
			platBook = maPlatBookPage.group(1);
			platNo = maPlatBookPage.group(2);
		}
		legalDescription = legalDescription.replaceAll(pattPlatBookPage, "");
		
		List<List> newBodyPIS = new ArrayList<List>();
		
		List<String> bodyLine = new ArrayList<String>();
		if (bodyPIS.size()>0) {
			bodyLine = (List<String>)bodyPIS.get(0);
		} else {
			bodyPIS.add(bodyLine);
		}
		if (bodyLine.size()==0) {
			bodyLine.add("");	//PARCEL_ID
			bodyLine.add("");	//SUBDIVISION_BLOCK
		}
		
		Matcher mat = Pattern.compile("(?is)([\\d\\s]+(?:\\s+[A-Z]+)?)\\s+\\bAD\\b(.+)").matcher(legalDescription);
		if (mat.find()) {
			String pin = ro.cst.tsearch.servers.types.MDGenericRO.cleanPin(mat.group(1));
			if (StringUtils.isEmpty(bodyLine.get(1))) {
				bodyLine.set(0, pin);
			}
			legalDescription = mat.group(2);
		}
		
		legalDescription = legalDescription.trim();
		
		//Anne Arundel Book 7232, p. 303 or Book 7725, p. 713
		Matcher maCR1 = Pattern.compile("(\\d+)-(\\d+)").matcher(legalDescription);
		if (maCR1.matches()) {
			List<String> crossRefBook = new ArrayList<String>();
			List<String> crossRefPage = new ArrayList<String>();
			crossRefBook.add(maCR1.group(1));
			crossRefPage.add(maCR1.group(2));
			addCrossRef(resultMap, crossRefBook, crossRefPage);
			return;
		}
		
		//Carroll Book 1685, p. 23 or Book 1793, p. 776
		Matcher maCR2 = Pattern.compile("(\\d+)/(\\d+)").matcher(legalDescription);
		if (maCR2.matches()) {
			List<String> crossRefBook = new ArrayList<String>();
			List<String> crossRefPage = new ArrayList<String>();
			crossRefBook.add(maCR2.group(1));
			crossRefPage.add(maCR2.group(2));
			addCrossRef(resultMap, crossRefBook, crossRefPage);
			return;
		}
		
		//Carroll Book 1786, pp. 786-789 or Book 2245, pp. 456-464
		Matcher maParcelNo1 = Pattern.compile("PT(\\d+)").matcher(legalDescription);
		if (maParcelNo1.matches()) {
			bodyLine.set(0, maParcelNo1.group(1));
			return;
		}
		
		StringBuilder lotInAddress = new StringBuilder();
		
		List<String> addressesList = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		boolean foundAddress = false;
		String address = "";
		String[] split = legalDescription.split("\n");
		for (int i=0;i<split.length;i++) {
			String s = split[i];
			s = s.trim();
			boolean toAdd = false;
			if (!StringUtils.isEmpty(s)) {
				address = processAddress(bodyLine, s, county, lotInAddress);
				if (address==null) {
					address = "";
					toAdd = true;
				} else {
					foundAddress = true;
					addressesList.add(address);
				}
				if (toAdd) {
					sb.append(s).append("\n");
				}
			}
		}
		
		//Anne Arundel Book 7466, pp. 16-18 or Book 7711, pp. 154-156
		String parcelNoPatt = "(?is)\\bTAX\\s+#(\\d-\\d{3}-\\d{4})-(\\d{4})";
		Matcher maParcelNo = Pattern.compile(parcelNoPatt).matcher(legalDescription);
		if (maParcelNo.find()) {
			String parcelNo = maParcelNo.group(1) + maParcelNo.group(2);
			bodyLine.set(0, parcelNo);
		}
		legalDescription = legalDescription.replaceAll(parcelNoPatt, "");
		
		StringBuilder subdivisionLot = new StringBuilder();
		
		if (foundAddress) {
			for (int i=0;i<addressesList.size();i++) {
				address = addressesList.get(i);
				boolean hasNewBodyLine = false;
				List<String> newBodyLine = new ArrayList<String>();
				if (bodyLine.size()!=2) {
					hasNewBodyLine = true;
					newBodyLine.add("");
					newBodyLine.add("");
				}
				Matcher ma1 = Pattern.compile("(?is)(.+?)LB(\\d+(?:&\\d+)?)(?:\\s+LF\\b)?").matcher(address);
				if (ma1.find()) {
					address = ma1.group(1).replaceFirst("(?is)\\s*(?:VL\\s*)?AD(\\d+)", "$1").trim();
					subdivisionLot.append(ma1.group(2).replaceAll("&", " ").trim()).append(" ");
				} else {
					if ("Baltimore".equals(county)) {
						Matcher ma2 = Pattern.compile("(?is)(\\d{10,12})(.+?)\\bLOT#(\\d+)").matcher(address);
						if (ma2.find()) {
							if (hasNewBodyLine) {
								newBodyLine.set(0, ma2.group(1));
							} else {
								bodyLine.set(0, ma2.group(1));
							}
							address = ma2.group(2).replaceFirst("(?is)\\s*(?:VL\\s*)?AD(\\d+)", "$1").trim();
							subdivisionLot.append(ma2.group(3).replaceAll("&", " ").trim()).append(" ");
						}
					}
				}
				if ("Charles".equals(county) || "Dorchester".equals(county)) {
					String[] cities = CHARLES_CITIES;
					if ("Dorchester".equals(county)) {
						cities = DORCHESTER_CITIES;
					}
					String addressAndCity[] = StringFormats.parseCityFromAddress(address, cities);
					if (StringUtils.isNotEmpty(addressAndCity[0])) {
						addressAndCity[0] = addressAndCity[0].replaceAll("(?is)\\bENMKT\\b", "EAST NEW MARKET");
						if (hasNewBodyLine) {
							newBodyLine.add(addressAndCity[0]);
						} else {
							bodyLine.add(addressAndCity[0]);
						}
					} else {
						if (hasNewBodyLine) {
							newBodyLine.add("");	//CITY
						} else {
							bodyLine.add("");	//CITY
						}
					}
					address = addressAndCity[1];
				} else {
					if (hasNewBodyLine) {
						newBodyLine.add("");		//CITY
					} else {
						bodyLine.add("");		//CITY
					}
				}
				if (hasNewBodyLine) {
					newBodyLine.add(StringFormats.StreetName(address));
					newBodyLine.add(StringFormats.StreetNo(address));
				} else {
					bodyLine.add(StringFormats.StreetName(address));
					bodyLine.add(StringFormats.StreetNo(address));
				}
				if (hasNewBodyLine) {
					int len = newBodyLine.size();
					for (int j=0;j<BODYLINE_LEN-len;j++) {
						newBodyLine.add("");
					}
					newBodyPIS.add(newBodyLine);
				}
			}
		} else {
			bodyLine.add("");			//CITY
			bodyLine.add("");			//STREET_NAME
			bodyLine.add("");			//STREET_NO
		}
		
		String legal = sb.toString() + lotInAddress;
		
		boolean foundLot = false;
		if(StringUtils.isEmpty(legal)) {
			String subdivisionLotString = subdivisionLot.toString().trim();
			if (subdivisionLotString.length() != 0) {
				subdivisionLotString = LegalDescription.cleanValues(subdivisionLotString, false, true);
				subdivisionLotString = sortValues(subdivisionLotString);
				bodyLine.add(subdivisionLotString);
				foundLot = true;
			}
			int len = bodyLine.size();
			for (int j=0;j<BODYLINE_LEN-len-2;j++) {
				bodyLine.add("");
			}
			bodyLine.add(platBook);
			bodyLine.add(platNo);
			return;
		}
		
		legal = legal.replaceAll("(?is)\\bLS\\s+(\\d+)\\s*T(?:HRU|O)\\s*(\\d+)\\b", "LS $1-$2");
		
		List<String> lot = RegExUtils.getMatches(LOT_PATTERN1, legal, 1);
		legal = legal.replaceAll(LOT_PATTERN1, " LOT ");
		for (int i=0; i<lot.size(); i++) {
			String s = lot.get(i).replaceAll("&", " ");
			s = cleanValues(s);
			subdivisionLot.append(s).append(" ");
		} 
		lot = RegExUtils.getMatches(LOT_PATTERN2, legal, 1);
		legal = legal.replaceAll(LOT_PATTERN2, " LOT ");
		for (int i=0; i<lot.size(); i++) {
			String s = lot.get(i).replaceAll("&", " ");
			s = cleanValues(s);
			subdivisionLot.append(s).append(" ");
		}
		lot = RegExUtils.getMatches(LOT_PATTERN3, legal, 1);
		legal = legal.replaceAll(LOT_PATTERN3, " LOT ");
		for (int i=0; i<lot.size(); i++) {
			String s = lot.get(i);
			s = cleanValues(s);
			subdivisionLot.append(s).append(" ");
		}
		lot = RegExUtils.getMatches(LOT_PATTERN4, legal, 1);
		legal = legal.replaceAll(LOT_PATTERN4, " LOT ");
		for (int i=0; i<lot.size(); i++) {
			String s = lot.get(i);
			s = cleanValues(s);
			subdivisionLot.append(s).append(" ");
		}
		String subdivisionLotString = subdivisionLot.toString().trim();
		if (subdivisionLotString.length() != 0) {
			subdivisionLotString = LegalDescription.cleanValues(subdivisionLotString, false, true);
			subdivisionLotString = sortValues(subdivisionLotString);
			bodyLine.add(subdivisionLotString);
			foundLot = true;
		}
		
		if (!foundLot) {
			bodyLine.add("");	//SUBDIVISION_LOT_NUMBER
		}
		
		int index = 0; 
		Matcher ma1 = Pattern.compile(BLOCK_PATTERN1).matcher(legal);
		while (ma1.find()) {
			String block =  ma1.group(1);
			block = block.replaceFirst("^0+", "");
			if (!StringUtils.isEmpty(block)) {
				if (index==0) {
					bodyLine.set(1, block);
				} else {
					int newPISIndex = -1;
					for (int i=0;i<newBodyPIS.size();i++) {
						List<String> l = newBodyPIS.get(i);
						if (l.size()>1 && "".equals(l.get(1))) {
							l.set(1, block);
							newPISIndex = i;
							break;
						}
					}
					if (newPISIndex==-1) {
						List<String> newBodyLine = new ArrayList<String>();
						newBodyLine.add("");	//PARCEL_ID
						newBodyLine.add(block);
						int len = newBodyLine.size();
						for (int j=0;j<BODYLINE_LEN-len-2;j++) {
							newBodyLine.add("");
						}
						bodyLine.add(platBook);
						bodyLine.add(platNo);
						newBodyPIS.add(newBodyLine);
					}
				}
				index++;
			}
		}
		legal = legal.replaceAll(BLOCK_PATTERN1, " BLOCK ");
		if (index==0) {
			List<String> block = RegExUtils.getMatches(BLOCK_PATTERN2, legal, 1);
			legal = legal.replaceAll(BLOCK_PATTERN2, " BLOCK ");
			StringBuilder subdivisionBlock = new StringBuilder();
			for (int i=0; i<block.size(); i++) {
				subdivisionBlock.append(block.get(i)).append(" ");
			}
			block = RegExUtils.getMatches(BLOCK_PATTERN3, legal, 1);
			legal = legal.replaceAll(BLOCK_PATTERN3, " BLOCK ");
			for (int i=0; i<block.size(); i++) {
				subdivisionBlock.append(block.get(i).replaceAll("(?is)\\s*TO\\s*", "-")).append(" ");
			}
			String subdivisionBlockString = subdivisionBlock.toString().trim();
			if (subdivisionBlockString.length() != 0) {
				subdivisionBlockString = LegalDescription.cleanValues(subdivisionBlockString, false, true);
				subdivisionBlockString = sortValues(subdivisionBlockString);
				bodyLine.set(1, subdivisionBlockString);
			}
		}
		
		List<String> sec = RegExUtils.getMatches(SECTION_PATTERN, legal, 1);
		legal = legal.replaceAll(SECTION_PATTERN, " SECTION ");
		StringBuilder subdivisionSec = new StringBuilder();
		for (int i=0; i<sec.size(); i++) {
			subdivisionSec.append(sec.get(i)).append(" ");
		} 
		String subdivisionSecString = subdivisionSec.toString().trim();
		if (subdivisionSecString.length() != 0) {
			subdivisionSecString = LegalDescription.cleanValues(subdivisionSecString, false, true);
			subdivisionSecString = sortValues(subdivisionSecString);
			bodyLine.add(subdivisionSecString);
		} else {
			bodyLine.add("");	//SUBDIVISION_SECTION
		}
		
		List<String> unit = RegExUtils.getMatches(UNIT_PATTERN, legal, 1);
		legal = legal.replaceAll(UNIT_PATTERN, " UNIT ");
		StringBuilder subdivisionUnit = new StringBuilder();
		for (int i=0; i<unit.size(); i++) {
			subdivisionUnit.append(unit.get(i)).append(" ");
		} 
		String subdivisionUnitString = subdivisionUnit.toString().trim();
		if (subdivisionUnitString.length() != 0) {
			subdivisionUnitString = LegalDescription.cleanValues(subdivisionUnitString, false, true);
			subdivisionUnitString = sortValues(subdivisionUnitString);
			bodyLine.add(subdivisionUnitString);
		} else {
			bodyLine.add("");	//SUBDIVISION_UNIT
		}
		
		List<String> bldg = RegExUtils.getMatches(BLDG_PATTERN, legal, 1);
		legal = legal.replaceAll(BLDG_PATTERN, " BLDG ");
		StringBuilder subdivisionBldg = new StringBuilder();
		for (int i=0; i<bldg.size(); i++) {
			subdivisionBldg.append(bldg.get(i)).append(" ");
		} 
		String subdivisionBldgString = subdivisionBldg.toString().trim();
		if (subdivisionBldgString.length() != 0) {
			subdivisionBldgString = LegalDescription.cleanValues(subdivisionBldgString, false, true);
			subdivisionBldgString = sortValues(subdivisionBldgString);
			bodyLine.add(subdivisionBldgString);
		} else {
			bodyLine.add("");	//SUBDIVISION_BLDG
		}
				
		String crosseRefPat1 = "(\\d+)/(\\d+)\\b";
		List<String> crossRefBook = new ArrayList<String>();
		List<String> crossRefPage = new ArrayList<String>();
		Matcher matCR1 = Pattern.compile("(?is)\\bPARCEL\\s+" + crosseRefPat1).matcher(legal);
		while (matCR1.find()) {
			crossRefBook.add(matCR1.group(1));
			crossRefPage.add(matCR1.group(2));
		}
		legal = legal.replaceAll("\\b" + crosseRefPat1, "");
		String crosseRefPat2 = "(?is)\\bBK\\s+(\\d+)\\s+PG\\s+(\\d+)\\b";
		Matcher matCR2 = Pattern.compile(crosseRefPat2).matcher(legal);
		while (matCR2.find()) {
			crossRefBook.add(matCR2.group(1));
			crossRefPage.add(matCR2.group(2));
		}
		legal = legal.replaceAll(crosseRefPat2, "");
		addCrossRef(resultMap, crossRefBook, crossRefPage);
		
		List<String> parcel = RegExUtils.getMatches(PARCEL_PATTERN1, legal, 1);
		legal = legal.replaceAll(PARCEL_PATTERN1, " PARCEL ");
		StringBuilder parcelSb = new StringBuilder();
		for (int i=0; i<parcel.size(); i++) {
			String s = parcel.get(i).trim();
			s = cleanValues(s);
			parcelSb.append(s).append(" ");
		}
		parcel = RegExUtils.getMatches(PARCEL_PATTERN2, legal, 1);
		legal = legal.replaceAll(PARCEL_PATTERN2, " PARCEL ");
		for (int i=0; i<parcel.size(); i++) {
			String s = parcel.get(i).trim();
			parcelSb.append(s).append(" ");
		}
		
		String parcelString = parcelSb.toString().trim();
		if (parcelString.length() != 0) {
			parcelString = LegalDescription.cleanValues(parcelString, false, true);
			parcelString = sortValues(parcelString);
			bodyLine.add(parcelString);
		} else {
			bodyLine.add("");	//PARCEL_ID_PARCEL
		}
		
		String subdName = "";
		String subdExpr1 = "(?is).*\\b(?:PARCEL|BLDG|SECTION|BLOCK|LOT)\\b(.*)\\b\\d+/(?:\\d+|-)";
		Matcher ma2 = Pattern.compile(subdExpr1).matcher(legal);
		if (ma2.find()) {
			subdName = ma2.group(1);
			subdName = subdName.replaceAll("(?is)\\bPT\\b", "");
		}
		if (StringUtils.isEmpty(subdName)) {
			String subdExpr2 = "(?is).*\\b(?:PARCEL|BLDG|BLOCK|LOT)\\b(.*)\\bSECTION\\b\\s+\\d+/(?:\\d+|-)";
			Matcher ma3 = Pattern.compile(subdExpr2).matcher(legal);
			if (ma3.find()) {
				subdName = ma3.group(1);
			}
		}
		if (StringUtils.isEmpty(subdName)) {
			String subdExpr3 = "(?is).*\\b(?:PARCEL|BLDG|SECTION|BLOCK|LOT)\\b(.*)\\s*$";
			Matcher ma3 = Pattern.compile(subdExpr3).matcher(legal);
			if (ma3.find()) {
				subdName = ma3.group(1);
				subdName = subdName.replaceFirst("\\s*/[^\\s]*", "");
				subdName = subdName.replaceFirst("(?is)E?D(IST)?\\s+\\d+", "");
				if (subdName.matches("(?is)^\\s*TM?\\s*\\d+.*")) {
					subdName = "";
				}
			}
		}
		if (StringUtils.isEmpty(subdName)) {
			String subdExpr4 = "(?is)(.+)\\bUNIT\\s*$";
			Matcher ma4 = Pattern.compile(subdExpr4).matcher(legal);
			if (ma4.find()) {
				subdName = ma4.group(1);
			}
		}
		if (StringUtils.isEmpty(subdName)) {
			String subdExpr5 = "(?is)(.+)\\bPT\\s+(?:\\d+|[A-Z])\\s*$";
			Matcher ma5 = Pattern.compile(subdExpr5).matcher(legal);
			if (ma5.find()) {
				subdName = ma5.group(1);
			}
		}
		
		subdName = subdName.trim();
		if ("PT".equalsIgnoreCase(subdName) || "NONE".equalsIgnoreCase(subdName)) {
			subdName = "";
		}
		
		if (!StringUtils.isEmpty(subdName)) {
			subdName = subdName.replaceAll("\\bBLDG\\b", "");
			subdName = subdName.replaceAll("\\bUNIT\\b", "");
			subdName = subdName.replaceAll(".*?\\bP(ARCE)?L\\b", "");
			subdName = subdName.replaceAll("\\bSECTION\\b", "");
			subdName = subdName.replaceAll(".*?\\bBLOCK\\b", "");
			subdName = subdName.replaceAll(".*?\\bLOTS?\\b", "");
			subdName = subdName.replaceAll(".*?\\bP\\d+\\b", "");
			subdName = subdName.trim();
			bodyLine.add(subdName);
			if (subdName.matches(".*\\bCOND.*")) {
				bodyLine.add(subdName);
			} else {
				bodyLine.add("");	//SUBDIVISION_COND
			}
		} else {
			bodyLine.add("");		//SUBDIVISION_NAME
			bodyLine.add("");		//SUBDIVISION_COND
		}
		
		if ("Charles".equals(county)) {
			if (StringUtils.isEmpty(subdName)) {
				legal = legal.replaceAll("(?is)\\bLOTS?\\b", "");
				String spl[] = legal.split("\\s+");
				boolean isSubd = true;
				for (int i=0;i<spl.length;i++) {
					if (!spl[i].matches("\\w+")) {
						isSubd = false;
						break;
					}
				}
				if (isSubd) {
					String addressAndCity[] = StringFormats.parseCityFromAddress(legal, CHARLES_CITIES);
					if (StringUtils.isEmpty(addressAndCity[0])) {	//is not the city
						if (bodyLine.size()>10) {
							bodyLine.set(10, legal);
						}
						if (legal.matches(".*\\bCOND.*")) {
							if (bodyLine.size()>11) {
								bodyLine.set(11, legal);
							}
						}
					}
				}
			}
		}
		
		bodyLine.add(platBook);
		bodyLine.add(platNo);
		
		for (int i=0;i<newBodyPIS.size();i++) {
			bodyPIS.add(newBodyPIS.get(i));
		}
		
	}
	
	public static String cleanValues(String s) {
		String[] spl = s.split("-");
		if (spl.length>2) {
			s = s.replaceAll("-", " ");
		} else if (spl.length==2) {
			String s1 = spl[0].trim();
			String s2 = spl[1].trim();
			if (s1.matches("\\d+") && s1.matches("\\d+")) {
				int i1 = Integer.parseInt(s1);
				int i2 = Integer.parseInt(s2);
				if (i1+1 == i2) {
					s = i1 + " " + i2;
				}
			}
		}
		return s;
	}
	
	public static String sortValues(String s) {
		StringBuilder res = new StringBuilder();
		String[] split = s.split("\\s+");
		List<String> digits = new ArrayList<String>();
		List<String> nondigits = new ArrayList<String>();
		for (int i=0;i<split.length;i++) {
			if (split[i].matches("\\d+(-\\d+)?")) {
				digits.add(split[i]);
			} else {
				nondigits.add(split[i]);
			}
		}
		Collections.sort(nondigits);
		for (String el: digits) {
			res.append(el).append(" ");
		}
		for (String el: nondigits) {
			res.append(el).append(" ");
		}
		
		return res.toString().trim();
	}
	
	public static boolean contains(String[][] body, String book, String page) {
		for (int i=0;i<body.length;i++) {
			String[] line = body[i]; 
			if (line.length>1) {
				if (book.equals(line[0]) && page.equals(line[1])) {
					return true;
				}
			}
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public static void addCrossRef(ResultMap resultMap, List<String> crossRefBook, List<String> crossRefPage) {
		if (crossRefBook.size()>0 && crossRefBook.size()==crossRefPage.size()) {
			ResultTable rt = (ResultTable)resultMap.get("CrossRefSet");
			if (rt==null) {
				List<List> bodyCR = new ArrayList<List>();
				List<String> line;
				for (int i=0;i<crossRefBook.size();i++) {
					String bk = crossRefBook.get(i);
					String pg = crossRefPage.get(i);
					if (!ro.cst.tsearch.servers.types.MDGenericRO.contains(bodyCR, bk, pg)) {
						line = new ArrayList<String>();
						line.add(bk);
						line.add(pg);
						line.add("");
						line.add("");
						line.add("");
						line.add("");
						bodyCR.add(line);
					}
				}
				if (bodyCR.size() > 0) {
					String[] header = { CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(),
							CrossRefSetKey.YEAR.getShortKeyName(), CrossRefSetKey.MONTH.getShortKeyName(),
							CrossRefSetKey.DAY.getShortKeyName(), CrossRefSetKey.INSTRUMENT_REF_TYPE.getShortKeyName()};
					rt = GenericFunctions2.createResultTable(bodyCR, header);
					resultMap.put("CrossRefSet", rt);
				}
			} else {
				String[][] body = rt.getBody();
				List<List<String>> newBody = new ArrayList<List<String>>();
				for(String[] line: body){
					newBody.add(Arrays.asList(line));
				}
				List<String> line;
				for (int i=0;i<crossRefBook.size();i++) {
					String bk = crossRefBook.get(i);
					String pg = crossRefPage.get(i);
					if (!contains(body, bk, pg)) {
						line = new ArrayList<String>();
						line.add(bk);
						line.add(pg);
						line.add("");
						line.add("");
						line.add("");
						line.add("");
						newBody.add(line);
					}
				}
				try {
					rt.setReadOnly(false);
					rt.setBody(newBody);
					rt.setReadOnly(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
				resultMap.put("CrossRefSet", rt);
			}
		}
	}
	
}

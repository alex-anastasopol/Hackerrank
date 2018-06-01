package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
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
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

public class GenericCountyRecorderRO {
	
	public static ResultMap parseIntermediaryRow(String row, long searchId) {

		ResultMap m = new ResultMap();

		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(row, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tdList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), false);
			
			if (tdList.size()==6) {
				String instrumentNumber = tdList.elementAt(1).toPlainTextString();
				instrumentNumber = instrumentNumber.trim().replaceFirst("^0+", "");
				if (ro.cst.tsearch.servers.types.GenericCountyRecorderRO.instrNoIsBookPage(instrumentNumber, searchId)) {
					Matcher ma = Pattern.compile(ro.cst.tsearch.servers.types.GenericCountyRecorderRO.BOOK_PAGE_PATT).matcher(instrumentNumber);
					if (ma.find()) {
						m.put(SaleDataSetKey.BOOK_TYPE.getKeyName(), ma.group(1));
						m.put(SaleDataSetKey.BOOK.getKeyName(), ma.group(2));
						m.put(SaleDataSetKey.PAGE.getKeyName(), ma.group(3));
					}
				} else {
					m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber);
				}
				
				String recordedDate = tdList.elementAt(2).toPlainTextString().trim();
				if (!StringUtils.isEmpty(recordedDate)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
				}
				
				String docType = ro.cst.tsearch.servers.types.GenericCountyRecorderRO.cleanType(tdList.elementAt(3).toPlainTextString().trim());
				if (!StringUtils.isEmpty(docType)) {
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
				}
				
				StringBuilder grantor = new StringBuilder();
				StringBuilder grantee = new StringBuilder();
				int table1RowCount = 0;
				int table2RowCount = 0;
				Node node1 = tdList.elementAt(4).getFirstChild();
				TableTag table1 = null;
				if (node1 instanceof TableTag) {
					table1 = (TableTag)node1;
					table1RowCount = table1.getRowCount();
				}
				Node node2 = tdList.elementAt(5).getFirstChild();
				TableTag table2 = null;
				if (node2 instanceof TableTag) {
					table2 = (TableTag)node2;
					table2RowCount = table2.getRowCount();
				}
				if (table1!=null && table2!=null && table1RowCount==table2RowCount) {
					for (int i=0;i<table1RowCount;i++) {
						String value1 = "";
						String value2 = "";
						TableRow row1 = table1.getRow(i);
						TableRow row2 = table2.getRow(i);
						if (row1.getColumnCount()>0) {
							value1 = row1.getColumns()[0].toPlainTextString().trim();
						}
						if (row2.getColumnCount()>0) {
							value2 = row2.getColumns()[0].toPlainTextString().trim();
						}
						if ("GRANTOR".equalsIgnoreCase(value2) || "OTHER".equalsIgnoreCase(value2)) {
							grantor.append(value1).append("<br>");
						} else if ("GRANTEE".equalsIgnoreCase(value2)) {
							grantee.append(value1).append("<br>");
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
			}
			
			parseNames(m);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return m;

	}
	
	public static String cleanName(String name) {
		
		name = name.replaceFirst("^\\s*-\\s*", "");
		name = name.replaceFirst("\\s*-\\s*$", "");
		name = name.trim();
		
		if (name.matches("(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)" +
				"\\s+\\d{1,2},\\s+\\d{4}")) {
			return "";
		}
		
		name = name.replaceAll("(?is)(\\b|\\*)[AFN]/?K/?A\\b", "");
		name = name.replaceAll("(?is)\\bMRS\\b", "");
		name = name.replaceAll("(?is)\\bFORMERLY\\s*$", "");
		name = name.replaceAll("(?is)\\bFNA\\s*$", "");
		name = name.replaceAll("(?is)\\bOR\\s+ORDER\\s*$", "");
		name = name.replaceAll("(?is)\\bDECEASED\\s*$", "");
		name = name.replaceAll("(?is)\\bATTORNEY\\s+IN\\s+FACT\\s*$", "");
		name = name.replaceAll("(?is)\\b(?:PUBLIC|SUCCESSOR\\s*,)\\s+(TRUSTEE)\\s*(-\\s*)?$", "$1");
		name = name.replaceAll("(?is)\\bRECEIVER(\\s+FOR)?\\s*$", "");
		name = name.replaceAll("(?is)\\bFBO\\s*$", "");
		name = name.replaceAll("(?is)\\bHOLDER(\\s+OF\\s+EXECUTIVE\\s+RIGHTS)?\\s*$", "");
		name = name.replaceAll("(?is)\\bIRA(\\s+(#\\s*)?\\d+)?(\\d+%)?\\s*$", "");
		name = name.replaceAll("(?is)\\bSUCCESSOR\\s+TO\\s*$", "");
		name = name.replaceAll("(?is)\\d{5}\\s*$", "");		//ZIP code
		name = name.replaceAll("(?is)\\bAGENT\\s*$", "");
		name = name.replaceAll("(?is)\\bEXECUTOR\\s*$", "");
		name = name.replaceAll("(?is)\\bHEIR\\s*$", "");
		name = name.replaceAll("(?is)\\bPRESIDENT\\s*$", "");
		name = name.replaceAll("(?is)\\bSERVICER\\s*$", "");
		name = name.replaceAll("(?is)\\bMANAGER\\s*$", "");
		name = name.replaceAll("(?is)\\bADMINISTRATOR\\s*$", "");
		name = name.replaceAll("(?is)\\bA(DM|S)INISTRATRIX\\s*$", "");
		name = name.replaceAll("(?is)\\bPER\\s+REP(RESENTATION)?\\s*$", "");
		name = name.replaceAll("(?is)\\bPR\\s*$", "");
		name = name.replaceAll("(?is)\\bSUCCESSOR(\\s+(BY\\s+MERGER|IN\\s+INTEREST))?\\s*$", "");
		name = name.replaceAll("(?is)\\b(A\\s+GE(NER|RNE)AL\\s+)?PARTNER\\s*$", "");
		name = name.replaceAll("(?is),\\s*ESTATE\\s*$", "");
		name = name.replaceAll("(?is)\\bCONSERVATOR\\s*$", "");
		name = name.replaceAll("(?is)\\SUPERINTENDENT\\s*$", "");
		name = name.replaceAll("(?is)\\bSUPT\\s*$", "");
		name = name.replaceAll("(?is)\\bBY\\s*$", "");
		name = name.replaceAll("(?is)\\bNOMINEE\\s*$", "");
		name = name.replaceAll("(?is)\\bGUARDIAN\\s*$", "");
		name = name.replaceAll("(?is)\\bQUALIFIED\\s+PERSONAL\\s*$", "");
		name = name.replaceAll("(?is)\\bPOA\\s*$", "");
		name = name.replaceAll("(?is)\\bP\\.R\\.\\s*$", "");
		name = name.replaceAll("(?is),\\s*(TRUSTEE)\\s*$", " $1");
		name = name.replaceAll(",?(\\s*AS)?\\s*$", "");
		
		name = name.replaceAll("(?is)(FEDERAL)(DEPOSIT)(INSURANCE)(CORPORATION)", "$1 $2 $3 $4");
		
		if (!isCompany(name)) {
			name = name.replaceAll("^([A-Z]+)\\s+(JR)\\s+([A-Z]+)$", "$1 $3 $2");	//GARCIA JR ERNEST, instrNo 341847
		}
		
		return name.trim();
	}
	
	public static boolean isCompany(String s) {
		if (s.matches("(?is)[A-Z]+\\s*&\\s*[A-Z]+")) {
			return true;
		}
		if (s.matches("(?is).*\\bCREDIT\\b.*")) {
			return true;
		}
		if (s.matches("(?is)CHASE\\s+\\d+")) {
			return true;
		}
		return NameUtils.isCompany(s);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, ResultMap resultMap) {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split("(?is)<br>");
		
		for (int i=0;i<split.length;i++) {
			
			String name = split[i];
			
			name = name.replaceAll("(?is)\\b(HOLDERS?\\s+)?EX(EC)?\\s+RIGHTS\\s+FOR\\b", "<br>");
			name = name.replaceAll("(?is)\\b(HOLDERS?\\s+)?EX(EC)?\\s+RIGHTS(\\s+FBO?)?(\\s+OF)?\\b", "<br>");
			name = name.replaceAll("(?is)\\bDBA\\b", "<br>");
			name = name.replaceAll("(?is)\\bFBO\\b", "<br>");
			name = name.replaceAll("(?is)\\b(TRUSTEE)\\b", "$1<br>");
			name = name.replaceAll("(?is)\\bPR\\b(.+\\bESTATE\\b)", "<br>$1");
			name = name.replaceAll("(?is)\\bAGENT\\s+FOR\\b", "<br>");
			
			String[] newSplit = name.split("(?is)<br>"); 
			
			for (int j=0;j<newSplit.length;j++) {
			
				String newName = newSplit[j];
				newName = cleanName(newName);
				
				if (!StringUtils.isEmpty(newName)) {
					if (isCompany(newName)) {
						names = new String[6];
						names[2] = newName;
						names[0] = names[1] = names[3] = names[4] = names[5] = ""; 
					}
					else if (j==0) {
						names = StringFormats.parseNameNashville(newName, true);
					} else {
						names = StringFormats.parseNameDesotoRO(newName, true);
						if (LastNameUtils.isLastName(names[0]) && !FirstNameUtils.isFirstName(names[0])) {
							String aux = names[0];
							names[0] = names[1];
							names[1] = names[2];
							names[2] = aux;
						}
					}
					
					if (isCompany(names[2]) && names[2].matches("(?is).+\\bOF\\s+TRUSTEES\\b.+")) {
						type = new String[2];
						type[0] = type[1] = "";
					} else {
						type = GenericFunctions.extractAllNamesType(names);
					}
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
							suffixes[1], type, otherType, isCompany(names[2]), isCompany(names[5]), list);
				}
				
			}
			
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpPartyGtor = (String)resultMap.get("tmpGrantor");
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			parseName(tmpPartyGtor, grantor, resultMap);
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
			parseName(tmpPartyGtee, grantee, resultMap);
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenateNames(grantee));
	}

	@SuppressWarnings("rawtypes")
	public static String concatenateNames(ArrayList<List> nameList) {
		String result = "";
		
		StringBuilder resultSb = new StringBuilder();
		for (List list: nameList) {
			if (list.size()>3) {
				resultSb.append(list.get(3)).append(", ").append(list.get(1)).append(" ").append(list.get(2)).append(" / ");
			}
		}
		result = resultSb.toString().replaceAll("/\\s*,\\s*/", " / ").replaceAll(",\\s*/", " /").
			replaceAll("\\s{2,}", " ").replaceAll("/\\s*$", "").trim();
		return result;
	}
	
	public static void parseAddress(ResultMap resultMap)  {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		
	}
	
}

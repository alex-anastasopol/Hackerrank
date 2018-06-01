package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class OHCuyahogaRO {
	
	public static final int NAME_NASHVILLE_PARSING = 0;
	public static final int NAME_DESOTO_PARSING = 1;
	public static final int PARTIAL_PARSING = 2;
	
	@SuppressWarnings("rawtypes")
	public static ResultMap parseIntermediaryRow(TableRow row, int format) {
		
		ResultMap m = new ResultMap();
		
		if (format!=PARTIAL_PARSING) {
			m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
		}
				
		TableColumn[] cols = row.getColumns();
		if (cols.length>8) {
			
			String instrNo = cols[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
			m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
			
			String docType = cols[2].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
			if (!StringUtils.isEmpty(docType)) {
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
			}
			
			String recordedDate = cols[5].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
			if (!StringUtils.isEmpty(recordedDate)) {
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			}
			
			String bookPage = cols[8].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
			if (!StringUtils.isEmpty(bookPage)) {
				String[] bp = bookPage.split("/");
				if (bp.length==2) {
					m.put(SaleDataSetKey.BOOK.getKeyName(), bp[0].trim());
					m.put(SaleDataSetKey.PAGE.getKeyName(),bp[1].trim());
				}
			}
			
			if (format!=PARTIAL_PARSING) {
				String grantor = cols[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				if (!StringUtils.isEmpty(grantor)) {
					m.put("tmpGrantor", grantor);
				}
				
				String grantee = cols[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				if (!StringUtils.isEmpty(grantee)) {
					m.put("tmpGrantee", grantee);
				}
				
				String references = cols[6].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				if (!StringUtils.isEmpty(references)) {
					String[] refs = references.split(";");
					if (refs.length>0) {
						
						String[] refHeader = {CrossRefSetKey.CROSS_REF_TYPE.getShortKeyName(), CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
								CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(), CrossRefSetKey.YEAR.getShortKeyName()};
						List<List> refBody = new ArrayList<List>();
						List<String> refRow;

						for (String s: refs) {
							refRow = new ArrayList<String>();
							refRow.add("");			//type
							refRow.add(s.trim());	//instrument number
							refRow.add("");			//book
							refRow.add("");			//page
							refRow.add("");			//year
							refBody.add(refRow);
						}

						m.put("CrossRefSet", GenericFunctions2.createResultTableFromList(refHeader, refBody));
						
					}
				}
				
				String legal = cols[7].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				if (!StringUtils.isEmpty(legal)) {
					m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
				}
				
				parseNames(m, format);
				parseLegal(m);
			}
			
		}
		return m;
	}
	
	public static String clean(String s) {
		
		s = s.replaceAll("(?is)&amp;", "&");
		s = s.replaceAll("(?is)-DEC\\b", "");		//deceased
		s = s.trim();
		
		return s;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, ResultMap resultMap, int format) {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split("(?is)<br>");
		
		for (int i=0;i<split.length;i++) {
			
			String name = split[i];
			name = clean(name);
				
			if (!StringUtils.isEmpty(name)) {
				if (NameUtils.isCompany(name)) {
					names = new String[]{"", "", "", "", "", ""};
					names[2] = name;
				} else if (format==NAME_DESOTO_PARSING) {
					names = StringFormats.parseNameDesotoRO(name, true);
				} else if (format==NAME_NASHVILLE_PARSING) {
					names = StringFormats.parseNameNashville(name, true);
				}
					
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
					suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
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
				String s = list.get(3) + " " + list.get(1) + " " + list.get(2);
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
	
	
	@SuppressWarnings("rawtypes")
	public static void parseLegal(ResultMap resultMap) {
		String legal = (String)resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(legal)) {
			return;
		}
		
		List<List> refBody = new ArrayList<List>();
		
		putCrossref(resultMap, refBody, legal);
		if (refBody.size()>0) {
			addReferences(resultMap, refBody);
			return;
		} else if (isFakeStreet(legal)) {
			return;
		}
		
		String parcel = "";
		String sublot = "";
		String lot = "";
		String street = "";
		Matcher ma1 = Pattern.compile("(?is)^(\\d{3}-\\d{2}-\\d{3})(.*)").matcher(legal);
		if (ma1.find()) {
			parcel = ma1.group(1);
			legal = ma1.group(2);
		} else {
			legal = " " + legal;
		}
		if (!StringUtils.isEmpty(parcel)) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.replaceAll("-", ""));
		}
		Matcher ma2 = Pattern.compile("(?is)^\\s(\\d+)(.*)").matcher(legal);
		if (ma2.find()) {
			sublot = ma2.group(1);
			legal = ma2.group(2);
		}
		if (!StringUtils.isEmpty(sublot)) {
			resultMap.put(PropertyIdentificationSetKey.SUB_LOT.getKeyName(), sublot);
		}
		String[] split = legal.split("\\s{2}");
		Matcher ma3 = Pattern.compile("(?is)[A-Z0-9\\s]+?\\s+(\\d+)(\\s+[A-Z0-9\\s]+)?").matcher(legal.trim());
		if (ma3.find() && split.length!=2) {	//has lot between township and street
			lot = ma3.group(1);
			if (!StringUtils.isEmpty(lot)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			}
			street = ma3.group(2);
			putStreet(resultMap, refBody, street);
		} else {		//only township and street remained
			if (split.length==2) {
				street = split[1];
				putStreet(resultMap, refBody, street);
			}
		}
		
		if (refBody.size()>0) {
			addReferences(resultMap, refBody);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void putCrossref(ResultMap resultMap, List<List> refBody, String street) {
		if (!StringUtils.isEmpty(street)) {
			street = street.trim();
			Matcher ma1 = Pattern.compile("(?is)\\bVL\\s+(\\d+)\\s+PG\\s+(\\d+)").matcher(street);
			if (ma1.find()) {		//e.g. 00000100, Book 7473, Page 34
				List<String> refRow = new ArrayList<String>();
				refRow.add("");				//document type
				refRow.add("");				//instrument number 
				refRow.add(ma1.group(1));	//book
				refRow.add(ma1.group(2));	//page
				refRow.add("");				//year
				refBody.add(refRow);
			} else {
				Matcher ma2 = Pattern.compile("(?is)\\bREF\\s+V(\\d+)-(\\d+)\\s+PG\\s+(\\d+)").matcher(street);
				if (ma2.find()) {	//e.g. AFN 00511570, Book 5219, Page 43
					List<String> refRow = new ArrayList<String>();
					refRow.add("");				//document type
					refRow.add("");				//instrument number 
					refRow.add(ma2.group(2));	//book
					refRow.add(ma2.group(3));	//page
					refRow.add(ma2.group(1));	//year
					refBody.add(refRow);
				}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void putStreet(ResultMap resultMap, List<List> refBody, String street) {
		if (!StringUtils.isEmpty(street)) {
			putCrossref(resultMap, refBody, street);
			if (refBody.size()==0) {
				if (!isFakeStreet(street)) {
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), street);
				}
			}
		}
	}
	
	public static boolean isFakeStreet(String street) {
		if (street.matches("(?is)\\d+-[A-Z]+\\s+[A-Z]+\\d+\\s+\\d+")) {		//5-A C3 40, AFN 00190234
			return true;
		}
		if (street.matches("(?is)CERT#\\s+\\d+")) {		//CERT# 8800, AFN 195101019299
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public static void addReferences(ResultMap resultMap, List<List> refBody) {
		String[] refHeader = {CrossRefSetKey.CROSS_REF_TYPE.getShortKeyName(), CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
				CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(), CrossRefSetKey.YEAR.getShortKeyName()};
		Object crs = resultMap.get("CrossRefSet");
		if (crs!=null) {	//CrossRefSet already exists
			ResultTable rt = (ResultTable)crs;
			ResultTable newRt =  GenericFunctions2.createResultTableFromList(refHeader, refBody);
			try {
				rt = ResultTable.joinVertical(rt, newRt, true);
				resultMap.put("CrossRefSet", rt);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {			//create CrossRefSet
			resultMap.put("CrossRefSet", GenericFunctions2.createResultTableFromList(refHeader, refBody));
		}
	}
	
}

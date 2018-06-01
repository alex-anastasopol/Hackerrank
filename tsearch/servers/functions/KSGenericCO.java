package ro.cst.tsearch.servers.functions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class KSGenericCO {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CO");
		
		if (row.getColumnCount()==5) {
			String instrNo = row.getColumns()[0].toPlainTextString().trim();
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
									
			String type = row.getColumns()[1].toPlainTextString().trim();
			String subType = row.getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			type += " " + subType;
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type.toUpperCase().trim());
			
			try {
				String recordedDate = row.getColumns()[3].toPlainTextString().trim();
				String oldFormat = "EEE, MMM dd, yyyy";
				String newFormat = "yyyy-MM-dd";
				SimpleDateFormat dateFormat = new SimpleDateFormat(oldFormat);
				Date date = dateFormat.parse(recordedDate);
				dateFormat.applyPattern(newFormat);
				recordedDate = dateFormat.format(date);
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			StringBuilder grantor = new StringBuilder();
			StringBuilder grantee = new StringBuilder();
			String name = row.getColumns()[4].toHtml().trim().replaceAll("(?is)</?td>", "");
			String[] names = name.split("(?is)<br\\s*/?\\s*>");
			int len = names.length;
			int i=0;
			while (i<len) {
				String line1 = names[i].trim().replaceAll("\\s{2,}", " ");
				String instrNoModif = instrNo.replaceFirst("^\\d{2}", "").replaceAll("-0+", "").replaceAll("-", "");
				line1 = line1.replaceFirst(instrNoModif + "$", "").trim();	//JOHN SMITH 10CV608 -> JOHN SMITH (instrument number is 2010-CV-000608)
				i++;
				if (i<len) {
					String line2 = names[i].trim();
					i++;
					if (line2.equalsIgnoreCase("(Plaintiff)") || line2.equalsIgnoreCase("(Plaintiff Alias)")) {
						grantor.append(line1).append("<br>");
					} else if (line2.equalsIgnoreCase("(Defendant)") || line2.equalsIgnoreCase("(Defendant Alias)")) {
						grantee.append(line1).append("<br>");
					}
				}
			}
			
			resultMap.put("tmpGrantor", grantor.toString().replaceFirst("<br>$", ""));
			resultMap.put("tmpGrantee", grantee.toString().replaceFirst("<br>$", ""));
			
			parseNamesIntermediary(resultMap);
			
		}
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNamesIntermediary(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpPartyGtor = (String)resultMap.get("tmpGrantor");
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			parseNameIntermediary(tmpPartyGtor, grantor, resultMap);
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		resultMap.remove("tmpGrantor");
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenateNames(grantor));
		
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpPartyGtee = (String)resultMap.get("tmpGrantee");
		if (StringUtils.isNotEmpty(tmpPartyGtee)){
			parseNameIntermediary(tmpPartyGtee, grantee, resultMap);
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.remove("tmpGrantee");
		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenateNames(grantee));
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNameIntermediary(String rawNames, ArrayList<List> list, ResultMap resultMap) {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split("(?is)<br>");
		
		for (int i=0;i<split.length;i++) {
			String name = split[i];
			if (NameUtils.isCompany(name)) {
				names[2] = name;
				names[0] = names[1] = names[3] = names[4] = names[5];
			} else {
				names = StringFormats.parseNameDesotoRO(name, true);
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
					suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
			
		}
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
	
	@SuppressWarnings("rawtypes")
	public static void parseNamesDetails(String instrNo, String text, ArrayList<List> list) {
		
		try {
			
			String instrNoModif = instrNo.replaceFirst("^\\d{2}", "").replaceAll("-0+", "").replaceAll("-", "");
			
			if (text.startsWith("Party")) {
				
				text = text.replaceFirst("(?is)^[^<]+</h4>", "").trim();
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(text, null);
				NodeList nodeList;
				
				nodeList = htmlParser.parse(null);
								
				NodeList tables =  nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tables.size()>1) {
					TableTag table = (TableTag)tables.elementAt(1);
					if (table.getRowCount()==2) {
						String lastName = "";
						String firstName = "";
						String middleName = "";
						String suffix = "";
						TableRow row = table.getRow(0);
						if (row.getColumnCount()==1) {
							lastName = row.getColumns()[0].toPlainTextString().replaceAll("(?is)\\bLast\\s+Name\\s+\\(or\\s+Business\\s+Name\\)\\s*:", "").trim();
							lastName = lastName.replaceFirst(instrNoModif + "$", "").trim();
						}
						row = table.getRow(1);
						if (row.getColumnCount()==3) {
							firstName = row.getColumns()[0].toPlainTextString().replaceAll("(?is)\\bFirst\\s+Name\\s*:", "").trim();
							middleName = row.getColumns()[1].toPlainTextString().replaceAll("(?is)\\bMiddle\\s*:", "").trim();
							suffix = row.getColumns()[2].toPlainTextString().replaceAll("(?is)\\bSuffix\\s*:", "").trim();
						}
						String fullName = lastName + " " + firstName + " " + middleName + " " + suffix;
						fullName = fullName.replaceAll("\\s{2,}", " ") .trim();
						if (fullName.length()>0) {
							String names[] = {firstName, middleName, lastName, "", "", ""};
							String[] suffixes = {suffix, ""};
							String[] type, otherType;
							type = GenericFunctions.extractAllNamesType(names);
							otherType = GenericFunctions.extractAllNamesOtherType(names);
							GenericFunctions.addOwnerNames(fullName, names, suffixes[0],
									suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
						}
					}
				}
				
				
			} else if (text.startsWith("Alias")) {
				
				text = text.replaceFirst("(?is)^[^<]+</h4>", "").trim();
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(text, null);
				NodeList nodeList;
				
				nodeList = htmlParser.parse(null);
								
				NodeList tables =  nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tables.size()>0) {
					TableTag table = (TableTag)tables.elementAt(0);
					if (table.getRowCount()==1) {
						String lastName = "";
						String firstName = "";
						String middleName = "";
						String suffix = "";
						TableRow row = table.getRow(0);
						if (row.getColumnCount()==4) {
							lastName = row.getColumns()[0].toPlainTextString().replaceAll("(?is)\\bLast\\s+Name\\s*:", "").trim();
							lastName = lastName.replaceFirst(instrNoModif + "$", "").trim();
							firstName = row.getColumns()[1].toPlainTextString().replaceAll("(?is)\\bFirst\\s*:", "").trim();
							middleName = row.getColumns()[2].toPlainTextString().replaceAll("(?is)\\bMiddle\\s*:", "").trim();
							suffix = row.getColumns()[3].toPlainTextString().replaceAll("(?is)\\bSuffix\\s*:", "").trim();
						}
						String fullName = lastName + " " + firstName + " " + middleName + " " + suffix;
						fullName = fullName.replaceAll("\\s{2}", " ").trim();
						if (fullName.length()>0) {
							String names[] = {firstName, middleName, lastName, "", "", ""};
							String[] suffixes = {suffix, ""};
							String[] type, otherType;
							type = GenericFunctions.extractAllNamesType(names);
							otherType = GenericFunctions.extractAllNamesOtherType(names);
							GenericFunctions.addOwnerNames(fullName, names, suffixes[0],
									suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
						}
					}
				}
				
			} 
			
		} catch (ParserException e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void putNamesDetails(ResultMap resultMap, ArrayList<List> list, int party) {
		
		try {
			if (party==0) {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(list, true));
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenateNames(list));
			} else if (party==1) {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(list, true));
				resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenateNames(list));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

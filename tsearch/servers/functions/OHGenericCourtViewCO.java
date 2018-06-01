package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class OHGenericCourtViewCO {
	
	public static final int NASHVILLE = 0;
	public static final int DE_SOTO = 0;
	
	public static ResultMap parseIntermediaryRow(String row, int offset) {
				
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CO");
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(row, null);
			NodeList nodeList = htmlParser.parse(null);
			
			if (nodeList.size()>=6) {
					
				String caseNumber = nodeList.elementAt(0).toPlainTextString().replaceAll("\\s", "");
				if (!StringUtils.isEmpty(caseNumber)) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), caseNumber);
				}
				
				String caseType = nodeList.elementAt(1).toPlainTextString().trim();
				if (!StringUtils.isEmpty(caseType)) {
					caseType = ro.cst.tsearch.servers.types.OHGenericCourtViewCO.correctCaseType(caseType);
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), caseType);
				}
				
				String fileDate = nodeList.elementAt(2).toPlainTextString().trim();
				if (!StringUtils.isEmpty(fileDate)) {
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), fileDate);
				}
					
				List<String> partyCompanyList = new ArrayList<String>();
				List<String> partyTypeList = new ArrayList<String>();
				
				TableTag partyCompanyTable = (TableTag)nodeList.elementAt(3 + offset).getFirstChild();
				if (partyCompanyTable!=null) {
					for (int i=0;i<partyCompanyTable.getRowCount();i++) {
						partyCompanyList.add(partyCompanyTable.getRow(i).toPlainTextString().trim());
					}
				}
				
				TableTag partyTypeTable = (TableTag)nodeList.elementAt(4 + offset).getFirstChild();
				if (partyTypeTable!=null) {
					for (int i=0;i<partyTypeTable.getRowCount();i++) {
						partyTypeList.add(partyTypeTable.getRow(i).toPlainTextString().trim());
					}
				}
				
				StringBuilder grantor = new StringBuilder();
				StringBuilder grantee = new StringBuilder();
				
				for (int i=0;i<partyCompanyList.size();i++) {
					String partyCompany = partyCompanyList.get(i);
					if (i<partyTypeList.size()) {
						String partyType = partyTypeList.get(i);
						if (partyType.matches("(?is).*\\b(PLAINTIFF|PETITIONER|APPELLANT|CREDITOR)\\b.*")) {
							grantor.append(partyCompany).append(" / ");
						} else if (partyType.matches("(?is).*\\b(DEFENDANT|RESPONDENT|APPELLEE|DEBTOR)\\b.*")) {
							grantee.append(partyCompany).append(" / ");
						}
					}
				}
				
				String grantorString = grantor.toString().replaceFirst(" / $", "");
				String granteeString = grantee.toString().replaceFirst(" / $", "");
				
				if (!StringUtils.isEmpty(grantorString)) {
					resultMap.put("tmpGrantorLF", grantorString);
				}
				if (!StringUtils.isEmpty(granteeString)) {
					resultMap.put("tmpGranteeLF", granteeString);
				}
				
				parseNames(resultMap);
				
				resultMap.removeTempDef();
				
				return resultMap;
					
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpGrantorLF = (String)resultMap.get("tmpGrantorLF");
		if (StringUtils.isNotEmpty(tmpGrantorLF)){
			parseName(tmpGrantorLF, grantor, NASHVILLE);
		}
		String tmpGrantorFL = (String)resultMap.get("tmpGrantorFL");
		if (StringUtils.isNotEmpty(tmpGrantorFL)){
			parseName(tmpGrantorFL, grantor, DE_SOTO);
		}
		grantor = removeDuplicates(grantor);
		if (grantor.size()>0) {
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenateNames(grantor));
		
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpGranteeLF = (String)resultMap.get("tmpGranteeLF");
		if (StringUtils.isNotEmpty(tmpGranteeLF)){
			parseName(tmpGranteeLF, grantee, NASHVILLE);
		}
		String tmpGranteeFL = (String)resultMap.get("tmpGranteeFL");
		if (StringUtils.isNotEmpty(tmpGranteeFL)){
			parseName(tmpGranteeFL, grantee, DE_SOTO);
		}
		grantee = removeDuplicates(grantee);
		if (grantee.size()>0) {
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenateNames(grantee));
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, int mode) {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split(" / ");
		
		for (int i=0;i<split.length;i++) {
			String name = split[i];
			name = cleanName(name);
			
			if (!StringUtils.isEmpty(name)) {
				if (mode==NASHVILLE) {
					names = StringFormats.parseNameNashville(name, true);
				} else {
					names = StringFormats.parseNameDesotoRO(name, true);
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
					
				GenericFunctions.addOwnerNames(split[i], names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
			}
			
		}
	}
	
	public static String cleanName(String name) {
		
		name = name.replaceFirst("(?is)\\bAS\\s+(TRUSTEE)\\s*$", "$1");
		name = name.replaceAll("(?is)\\bDECEASED\\b", "");
		name = name.replaceFirst("(?is)^\\s*UNK(NOWN)?\\b.*", "");
		name = name.replaceFirst("(?is)(.*?)\\bA\\s+MINOR\\s+CHILD.*?(,.*)", "$1$2");
		name = name.replaceFirst("(?is)^(.*?,)\\s*\\b(JR|SR|II|III|IV)\\s*,(.*)$", "$1$3 $2");
		
		return name;
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayList<List> removeDuplicates(ArrayList<List> list) {
		ArrayList<List> newList = new ArrayList<List>();
		for (List l: list) {
			l.set(0, "");
			if (!newList.contains(l)) {
				newList.add(l);
			}
		}
		return newList;
	}
	
}

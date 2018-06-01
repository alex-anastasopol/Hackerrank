package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class OHCuyahogaCO {
	
	public static final int NASHVILLE = 0;
	public static final int DESOTO = 1;

	public static ResultMap parseIntermediaryRow(TableRow row, int moduleType) {
		
		ResultMap resultMap = new ResultMap();
		
		String instrNo = "";
		String grantorFL = "";	//first last
		String granteeFL = "";
		String grantorLF = "";	//last first
		String granteeLF = "";
		String address = "";
		String city = "";
		String zip = "";
		String parcel = "";
		String recordedDate = "";
		
		if (row.getColumnCount()==2) {			//CIVIL SEARCH BY CASE, CRIMINAL SEARCH BY CASE, COURT OF APPEALS SEARCH BY CASE
			instrNo = row.getColumns()[0].toPlainTextString().trim();
			String names = row.getColumns()[1].toPlainTextString().trim();
			String[] split = names.split("\\bvs\\.");
			if (split.length==2) {
				grantorFL = split[0].trim();
				granteeFL = split[1].trim();
			}
		} else if (row.getColumnCount()==5) {	//CIVIL SEARCH BY NAME, COURT OF APPEALS SEARCH BY NAME
			String name = row.getColumns()[0].toPlainTextString().trim();
			String role = row.getColumns()[2].toPlainTextString().trim();
			if ("PLAINTIFF".equals(role) || "CREDITOR".equals(role) || "APPELLANT".equals(role)) {
				grantorLF = name;
			} else if ("DEFENDANT".equals(role) || "DEBTOR".equals(role) || "APPELLEE".equals(role)) {
				granteeLF = name;
			}
			instrNo = row.getColumns()[3].toPlainTextString().trim();
			String names = row.getColumns()[4].toPlainTextString().trim();
			String[] split = names.split("\\bv\\b");
			if (split.length==2) {
				if ("APPELLANT".equals(role) || "APPELLEE".equals(role)) {
					granteeFL = split[0].trim();
					grantorFL = split[1].trim();
				} else {
					grantorFL = split[0].trim();
					granteeFL = split[1].trim();
				}
			}
		} else if (row.getColumnCount()==8) {	//FORECLOSURE SEARCH
			granteeFL = row.getColumns()[0].toPlainTextString().trim();
			address = row.getColumns()[1].toPlainTextString().trim();
			city = row.getColumns()[2].toPlainTextString().trim();
			zip = row.getColumns()[3].toPlainTextString().trim();
			instrNo = row.getColumns()[4].toPlainTextString().trim();
			parcel = row.getColumns()[5].toPlainTextString().trim();
			recordedDate = row.getColumns()[7].toPlainTextString().trim();
		} else if (row.getColumnCount()==6) {	//CRIMINAL SEARCH BY NAME, first level
			granteeFL = row.getColumns()[0].toPlainTextString().trim();
		} else if (row.getColumnCount()==4) {	//CRIMINAL SEARCH BY NAME, second level
			instrNo = row.getColumns()[0].toPlainTextString().trim();
			recordedDate = row.getColumns()[2].toPlainTextString().trim();
		}
		
		if (!StringUtils.isEmpty(instrNo)) {
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
		}
		if (!StringUtils.isEmpty(grantorFL)) {
			resultMap.put("tmpGrantorFL", grantorFL);
		}
		if (!StringUtils.isEmpty(granteeFL)) {
			resultMap.put("tmpGranteeFL", granteeFL);
		}
		if (!StringUtils.isEmpty(grantorLF)) {
			resultMap.put("tmpGrantorLF", grantorLF);
		}
		if (!StringUtils.isEmpty(granteeLF)) {
			resultMap.put("tmpGranteeLF", granteeLF);
		}
		if (!StringUtils.isEmpty(address)) {
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
		}
		if (!StringUtils.isEmpty(city)) {
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
		}
		if (!StringUtils.isEmpty(zip)) {
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
		}
		if (!StringUtils.isEmpty(parcel)) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
		}
		if (!StringUtils.isEmpty(recordedDate)) {
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
		}
		
		if (moduleType==0) {
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "CIVIL");
		} else if (moduleType==1) {
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "CRIMINAL");
		}
		
		parseNames(resultMap);
		parseAddress(resultMap);
		
		resultMap.removeTempDef();
		
		return resultMap;
	}
	
	public static void parseAddress(ResultMap resultMap)  {
		
		String address = (String)resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpGrantorFL = (String)resultMap.get("tmpGrantorFL");
		if (StringUtils.isNotEmpty(tmpGrantorFL)){
			parseName(tmpGrantorFL, grantor, DESOTO);
		}
		String tmpGrantorLF = (String)resultMap.get("tmpGrantorLF");
		if (StringUtils.isNotEmpty(tmpGrantorLF)){
			parseName(tmpGrantorLF, grantor, NASHVILLE);
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
		String tmpGranteeFL = (String)resultMap.get("tmpGranteeFL");
		if (StringUtils.isNotEmpty(tmpGranteeFL)){
			parseName(tmpGranteeFL, grantee, DESOTO);
		}
		String tmpGranteeLF = (String)resultMap.get("tmpGranteeLF");
		if (StringUtils.isNotEmpty(tmpGranteeLF)){
			parseName(tmpGranteeLF, grantee, NASHVILLE);
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
			boolean isCompanyAndFunction = false;
			boolean isFunctionAndCompany = false;
			boolean isCompanyAndCO = false;
			
			String[] spl = name.split("(?is)(\\b[ANF]KA|FNA|DBA:?\\b)|(%)");
			if (spl.length==1) {
				spl = name.split("(?is)\\bEXECUTIVE\\b");										//MANNERS BIG BOY,INC. EXECUTIVE COMMONS E (CV-81-026617) 
				if (spl.length==2) {
					isCompanyAndFunction = true;
				}
			}
			if (spl.length==1) {
				spl = name.split("(?is)\\bASSIGNEE\\b|\\s*[-\\s]\\s*(AS\\s+)?ADMRX?\\.?\\b");	//CONNIE HERMAN AS ADMRX EST OF ANTHONY MARTIN (CV-93-261548)
				if (spl.length==2) {															//GREEN,DAVID H. - ADMR.EST.OF ANN H GREEN GENERAL E (CV-85-089365)
					isFunctionAndCompany = true;
				}
			}
			if (spl.length==1) {
				spl = name.split("(?is)\\bC/O\\b");												//CHICAGO TITLE INSURANCE CO C/O CYNTHIA KITKO, ESCROW OFFICER (CA-07-089365)
				if (spl.length==2) {
					isCompanyAndCO = true;
				}
			}
			if (spl.length==1) {
				Matcher ma = Pattern.compile("(?is)(.+)\\bMINOR\\s+(.*)\\bBY\\s+(.+)\\b(?:FATHER|MOTHER)(.*)").matcher(name);
				if (ma.find()) {
					spl = new String[2];
					spl[0] = ma.group(1) + " "  + ma.group(2);
					spl[1] = ma.group(3) + " "  + ma.group(4);
				}
			}
			boolean onlyPersons = true;
			for (int j=0;j<spl.length;j++) {
				if (NameUtils.isCompany(spl[j])) {
					onlyPersons = false;
					break;
				}
			}
			String lastName = "";
			for (int j=0;j<spl.length;j++) {
				String nm = spl[j];
				nm = cleanName(nm, mode);
				if (j==0 && isFunctionAndCompany) {
					if (nm.contains(",")) {
						names = StringFormats.parseNameNashville(nm, true);
					} else {
						names = StringFormats.parseNameDesotoRO(nm, true);
					}
				} else if (j==1 && isCompanyAndFunction) {
					names = StringFormats.parseNameNashville(nm, true);
				} else if (j==1 && isCompanyAndCO) {
					names = StringFormats.parseNameDesotoRO(nm, true);
				} else {
					if (isCompany(nm)) {
						names[2] = nm;
						names[0] = names[1] = names[3] = names[4] = names[5];
					} else {
						if (mode==NASHVILLE || (!isCompany(nm) && nm.indexOf(",")!=-1)) {
							names = StringFormats.parseNameNashville(nm, true);
						} else if (mode==DESOTO) {
							names = StringFormats.parseNameDesotoRO(nm, true);
						}
					}
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				if (onlyPersons) {
					if (j==0) {
						lastName = names[2];
					} else {
						if (names[2].length()<2 || names[2].matches("[A-Z]\\.")) {
							names[1] = names[2];
							names[2] = lastName;
						}
					}
				}
				
				GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
					suffixes[1], type, otherType, isCompany(names[2]), isCompany(names[5]), list);
			}
			
		}
	}
	
	public static String cleanName(String name, int mode) {
		
		name = name.replaceFirst("(?is)\\b\\s*-?\\s*ET\\s*AL\\.?", " ETAL ");
		
		name = name.replaceFirst("(?is),?\\s*AS\\s+TREASURER\\b.*", "");
		name = name.replaceFirst("(?is)-AS\\s+TREASURER\\b.*", "");
		name = name.replaceFirst("(?is)-AS\\s+STATUTORY\\s+AGENT\\b.*", "");
		name = name.replaceFirst("(?is)-\\s*ATTORNEY\\s+GENERAL\\b.*", "");
		name = name.replaceFirst("(?is)\\b+COUNTY\\s+PROSECUTOR\\b.*", "");
		
		name = name.replaceFirst("(?is)(UNKNOWN\\s+)?(HEIRS|SPOUSE)\\b.*?\\bOF", "");
		
		name = name.replaceFirst("(?is)\\bDEC'D\\b", "");
		name = name.replaceFirst("(?is)\\bDECEASED\\b", "");
		
		name = name.replaceFirst("(?is)\\bSHAREHOLDER(\\s+ETC)?\\b", "");
		name = name.replaceFirst("(?is)\\bACTING(\\s+ADMINISTRATOR)?\\b", "");
		name = name.replaceFirst("(?is)\\bESCROW\\s+OFFICER\\b", "");
		name = name.replaceFirst("(?is)\\bREG\\s+AGT\\b", "");
		
		name = name.replaceAll("(?is)\\bMR\\b", "");
		
		name = name.replaceAll("(?is)\\bA\\s*CORPORATION\\s*$", "");
		
		name = name.replaceAll("(?is)" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString + "\\.", "$1");
		name = name.replaceAll("(?is)[,-]\\s*" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameOtherTypeString, " $1");
		
		if (mode==NASHVILLE && name.indexOf(",")==-1) {
			name = name.replaceAll("(?is)" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString + "(\\s+.+)", "$1,$2");
		}
		
		name = name.replaceFirst("^\\s*:\\s*", "");
		name = name.replaceFirst("\\s*-\\s*$", "");
		name = name.replaceFirst("\\s*,\\s*$", "");
		
		return name;
	}
	
	public static boolean isCompany(String name) {
		if (name.matches("(?is).*\\bCORP\\..*")) {
			return true;
		}
		if (name.matches("(?is).*\\bEST.OF\\b.*")) {
			return true;
		}
		return NameUtils.isCompany(name); 
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

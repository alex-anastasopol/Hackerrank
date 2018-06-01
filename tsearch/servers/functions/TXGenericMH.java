package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class TXGenericMH {
		
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId, boolean isArchived) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "MH");
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "MANUFACTURED HOUSING");
				
		TableColumn[] cols = row.getColumns();
		
		String instrumentNumber = "";
		
		if (!isArchived) {
			instrumentNumber = cols[3].toPlainTextString();
			if (instrumentNumber.matches("0+") || instrumentNumber.matches("X+"))
				instrumentNumber = cols[0].toPlainTextString();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), cols[2].toPlainTextString());
			parseNames(resultMap,false);
		}
		else {
			instrumentNumber = cols[1].toPlainTextString();
			if (instrumentNumber.matches("0+"))
				instrumentNumber = cols[0].toPlainTextString();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), cols[3].toPlainTextString());
			parseNames(resultMap,true);
		}
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber);
		
		return resultMap;
	}
	
	public static boolean isSuffix(String s) {
		s = s.trim().replaceFirst("\\.\\z", "");
		if ("BROOKS".equalsIgnoreCase(s) ||	//BROOKS SMITH
			"FORD".equalsIgnoreCase(s) ||  	//FORD CONSUMER FINANCE
			"GREEN".equalsIgnoreCase(s))  	//GREEN TREE SERVICING LLC.
			return false;	
		return  Normalize.isSuffix(s);
	}
	
	public static boolean isAddress(String s) {
		s = s.trim();
		s = s.replaceAll("\\s{2,}", " ");
		if (s.matches("#?\\d+-?(\\w+)?\\s.*") || s.matches("(?is).*?(\\bP\\.?\\s?O\\.?\\s?\\s)?BOX\\s\\d+.*")
				|| s.matches("(?is)<br/>.*") || s.matches("(?is).*?(\\bP\\.?\\s?O\\.?\\s?\\s)?DRAWER\\s\\w+\\b.*")
				|| s.matches("(?is).*\\bLOT\\s.*") || s.matches("\\[\\w+\\]"))
			return true;
		String tokens[] = s.split("\\s");
		if (isSuffix(tokens[0])) return true;
		int len = tokens.length;
		//STAR ROUTE BOX 15 D
		if (len>=2 && tokens[0].matches("\\w+") && isSuffix(tokens[1]))
			return true;
		//FM 1187 @ HIGHWAY 377 
		if (len>=2 && isSuffix(tokens[len-2]) && tokens[len-1].matches("\\w+") && !NameUtils.isCompany(s))
			return true;
		//LUFKIN , TX
		if (len>=2 && ",".equals(tokens[len-2]) && Normalize.isStateAbbreviation(tokens[len-1]))
			return true;
		//SULPHUR SPRINGS , TX 75483
		if (len>=3 && ",".equals(tokens[len-3]) && Normalize.isStateAbbreviation(tokens[len-2]) && tokens[len-1].matches("\\d+"))
			return true;
		return false;
	}
	
	public static String cleanNames(String names, String separator) {
		String[] split = names.split(separator);
		if (split.length>=3 && split[1].toUpperCase().matches("[A-Z]-[A-Z-']+")) 
			split[1] = split[1].replaceFirst("-", " & ");
		StringBuilder sb = new StringBuilder("");
		for (int i=0;i<split.length;i++)
			if (i>0 && isAddress(split[i])) break;		
			else if (split[i].trim().length()!=0 && !NameUtils.isCompany(split[i].trim())) {
				String tmp = split[i].trim().replaceAll("\\s+", " ").replaceAll("\\*", "");
				if (tmp.indexOf("&")==-1 && tmp.toUpperCase().indexOf(" AND ")==-1) {
					String newSplit[] = tmp.split("\\s");
					int len = newSplit.length;
					while (newSplit[len-1].matches(GenericFunctions1.nameSuffixString) || 
							newSplit[len-1].matches(GenericFunctions1.nameTypeString) ||
							newSplit[len-1].matches(GenericFunctions1.nameOtherTypeString))
						   len--;
					//two names, e g. RAY A. INEZ G. SMITHHART -> RAY A. & INEZ G. SMITHHART
					if (len>=4 && newSplit[1].toUpperCase().matches("[A-Z]\\.?") && !LastNameUtils.isLastName(newSplit[2])) {
						StringBuilder newsb = new StringBuilder();
						newsb.append(newSplit[0]).append(" ").append(newSplit[1]).append(" &");
						for (int j=2;j<newSplit.length;j++) newsb.append(" ").append(newSplit[j]);
						tmp = newsb.toString();
					}
					//RAYMOND O-BETTY JO SMITH -> RAYMOND O & BETTY JO SMITH
					else if (newSplit.length>=3 && newSplit[1].toUpperCase().matches("[A-Z]-[A-Z-']+")) {
						newSplit[1] = newSplit[1].replaceFirst("-", " & ");
						StringBuilder newsb = new StringBuilder();
						for (int j=0;j<newSplit.length;j++) newsb.append(" ").append(newSplit[j]);
						tmp = newsb.toString();
					}
				}	
				//first name and last name on different lines, e.g. RAY A. INEZ G.\nSMITHHART
				if (tmp.toUpperCase().matches(".*\\s[A-Z]\\.?")) sb.append(tmp).append(" ");
				else sb.append(tmp).append("<br/>");
			}
			else sb.append(split[i].trim()).append("<br/>");
			
		String s = sb.toString();
		
		s = s.replaceFirst("<br/>\\z", "");
		s = s.replaceAll("(?is)\\bFA\\s*<br/>\\s*MILY\\b", "FAMILY");
		s = s.replaceAll("(?is)\\bBANK\\s*<br/>\\s*OF\\b", "BANK OF");
		s = s.replaceAll("(?is)\\bHOM\\s*<br/>\\s*INCORPORATED\\b", "HOM INCORPORATED");
		s = s.replaceFirst("\\(.*?\\)", "");
		s = s.replaceFirst("(?is),?\\s*([JS]R)\\.", " $1");
		s = s.replaceAll("(?is),?\\s*(&/)?\\bOR\\b", " &");
		s = s.replaceAll(",\\s*\\z", "");
		s = s.replaceAll("\\bC/O\\b", "&");
		s = s.replaceAll("\\*", "");
		s = s.replaceAll("(?is)\\bMR\\.? AND MRS\\.?\\b", "");
		s = s.replaceAll("(?is)\\bMrs?\\.?", "");
		s = s.replaceFirst("(?is)\\bESTATE OF ", "");
		s = s.replaceFirst("(?is),\\sEXECUTRIX\\b", "");
		if (!NameUtils.isCompany(s))
			s = s.replaceFirst("(?is)[\\d-]+\\z", "");		//ROBERT J HORN 3929-381536
		if (!NameUtils.isCompany(s)) 
			s = s.replaceAll("(?is)\\bAND\\b", "&");
		
		//ALBERT & ALVA L. SMITH -> ALBERT SMITH & ALVA L. SMITH
		String ampersandSplitted[] = s.split("&");
		sb = new StringBuilder();
		if (ampersandSplitted.length>1 && !NameUtils.isCompany(ampersandSplitted[ampersandSplitted.length-1].trim())) {
			String namesForLastName[] = StringFormats.parseNameFML(ampersandSplitted[ampersandSplitted.length-1], new Vector<String>(), true, true);
			for (int i=0;i<ampersandSplitted.length-1;i++)
				if (!NameUtils.isCompany(ampersandSplitted[i].trim()))
					sb.append(ampersandSplitted[i].trim()).append(" ").append(namesForLastName[2]).append(" ").append("&").append(" ");
				else sb.append(ampersandSplitted[i].trim()).append(" & ");
			sb.append(ampersandSplitted[ampersandSplitted.length-1].trim());
		} else sb.append(s);
				
		return sb.toString();
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, boolean isArchived) {
		
		String separator = "";
		if (isArchived) separator = "\n";
		else separator = "<br/?>";
		
		String names1[] = {"", "", "", "", "", ""};
		String[] suffixes1, type1, otherType1;
		ArrayList<List> grantor = new ArrayList<List>();
		String tmpPartyGtor = (String)resultMap.get("tmpGrantor");
		if (tmpPartyGtor!=null) {
			tmpPartyGtor = cleanNames(tmpPartyGtor,separator);
			String grantors[] = tmpPartyGtor.split("<br/>");
			for (int i=0;i<grantors.length;i++) {
				if (StringUtils.isNotEmpty(grantors[i])){
					
					if (NameUtils.isCompany(grantors[i])) 
						grantors[i] = grantors[i].replaceAll("&", "___");
					
					names1 = StringFormats.parseNameFML(grantors[i], new Vector<String>(), true, true);
					type1 = GenericFunctions.extractAllNamesType(names1);
					otherType1 = GenericFunctions.extractAllNamesOtherType(names1);
					suffixes1 = GenericFunctions.extractNameSuffixes(names1);
					
					if (NameUtils.isCompany(grantors[i])) 
						names1[2] = names1[2].replaceAll("___", "&");
							
					GenericFunctions.addOwnerNames("", names1, suffixes1[0],
								suffixes1[1], type1, otherType1,
								NameUtils.isCompany(names1[2]),
								NameUtils.isCompany(names1[5]), grantor);
				}
			}
				
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
				
		String names2[] = {"", "", "", "", "", ""};
		String[] suffixes2, type2, otherType2;
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpPartyGtee = (String)resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (tmpPartyGtee!=null) {
			tmpPartyGtee = cleanNames(tmpPartyGtee,separator);
			String grantees[] = tmpPartyGtee.split("<br/>");
			for (int i=0;i<grantees.length;i++) {
				if (StringUtils.isNotEmpty(grantees[i])){
					
					if (NameUtils.isCompany(grantees[i])) 
						grantees[i] = grantees[i].replaceAll("&", "___");
						
					names2= StringFormats.parseNameFML(grantees[i], new Vector<String>(), true, true);
					type2 = GenericFunctions.extractAllNamesType(names2);
					otherType2 = GenericFunctions.extractAllNamesOtherType(names2);
					suffixes2 = GenericFunctions.extractNameSuffixes(names2);
					
					names2[3] = names2[3].replaceFirst("\\A-", "");
					if (NameUtils.isCompany(grantees[i])) 
						names2[2] = names2[2].replaceAll("&", "___");
					
					GenericFunctions.addOwnerNames("", names2, suffixes2[0],
								suffixes2[1], type2, otherType2,
								NameUtils.isCompany(names2[2]),
								NameUtils.isCompany(names2[5]), grantee);
				}
			}
				
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address))
			return;
		
		String split[] = address.split("\n");
		for (int i=0;i<split.length;i++)
			if (split[i].trim().length()!=0) {
				address = split[i].trim();
				break;
			}
		
		address = address.replaceAll("(?is)LOT\\s[^\\s]+", "");
		address = address.replaceAll("(?is)BOX\\s[^\\s]+", "");
		
		String streetNo = StringFormats.StreetNo(address);
		String streetName = StringFormats.StreetName(address);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
	}
	
}

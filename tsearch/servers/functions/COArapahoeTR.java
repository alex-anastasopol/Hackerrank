package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;


public class COArapahoeTR {

	@SuppressWarnings("unchecked")
	public static void parseNames(ResultMap map, String owner, String coOwner) {		
		ArrayList<List> grantor = new ArrayList<List>();
		String grantorAsString = "";
		String firstName[] = null;
		boolean doDesoto = false;
		owner = owner.toUpperCase();
		coOwner = coOwner.toUpperCase();
		for (String token : new String[] {owner, coOwner}) {
			if(StringUtils.isNotEmpty(token)) {
				String names[] = {"", "", "", "", "", ""};
				token = cleanOwner(token);
				if(token.trim().isEmpty()) {
					continue;
				}
				if(firstName != null && token.equals(cleanOwner(coOwner))) {
					if(token.matches("(?is)\\s*JT\\s+TEN\\s*")) {
						continue;
					} else if(token.matches("(?is).*,?\\s*JT TEN")) {
						token = firstName[2] + ", " + token.replaceAll("(?is)([^,]*),?\\s*JT TEN","$1");
					} 
				}
				if(firstName != null && !token.contains(",") && !token.contains(firstName[2])) {
					if(owner.contains("&")) {
						doDesoto = true;
					} else {
						String separator = ", ";
						if (token.matches("(?is)\\s*TRUSTEE\\s*")) {
							separator = " ";
						}
						token = firstName[2] + separator + token;
					}
				}

				String[] splitToken;
				if (!token.contains("&") && token.split("\\s+").length > 3 && (splitToken = token.split(",")).length == 2) {
					if (splitToken[0].trim().split("\\s+")[0].equals(splitToken[1].trim().split("\\s+")[0])) {
						// e.g. "Brown James E, Brown Linney F" for PIN 031750784
						token = token.replaceFirst("\\s*,\\s*", " & ");
					}
				}

				if(!doDesoto) {
					names = StringFormats.parseNameNashville(cleanOwner(token), true);
				} else {
					names = StringFormats.parseNameDesotoRO(cleanOwner(token), true);
				}
				firstName = names;
				grantorAsString += token + "/";
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantor);
			}
		}
		
		if(StringUtils.isNotEmpty(grantorAsString)) {
			map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), grantorAsString.substring(0,grantorAsString.length() - 1));
		} 
		try {
			try {
				grantor = removeDuplicates(grantor);
				GenericFunctions.storeOwnerInPartyNames(map, grantor, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static String cleanOwner(String s) {
		s = s.replaceAll("\\d+/\\d+\\s+INT", "");
		s = s.replaceAll("\\d+\\s*%\\s*INT", "");
		s = s.replaceAll("[\\*%]", "");
		s = s.replaceAll("^\\s*&", "");
		s = s.replaceAll("%", " ");
		s = s.replaceAll("\\bDATED\\s+[\\d-]+", "");
		s = s.replaceAll("/(TRUSTEES)/OF OWN", " $1");
		s = s.replaceAll("\\s{2,}", " ");
		s = s.replaceAll("^TRUSTS$", "");
		
		return s.trim();
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 5) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), cols[0].toPlainTextString().trim());
			
			String nameOnServer = cols[1].toPlainTextString().trim();
			nameOnServer = nameOnServer.replace("&amp;", "&");
			
			String[] address = StringFormats.parseAddressShelbyAO(cols[2].toPlainTextString().trim());
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), address[0]);
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address[1]);

			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), cols[3].toPlainTextString().trim());
			
			String type = cols[4].toPlainTextString().trim();
			if("RE".equalsIgnoreCase(type)) {
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(),"Real Estate");
			} else if( "PP".equalsIgnoreCase(type) ){
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(),"Personal Property");
			} else if( "CA".equalsIgnoreCase(type) ){
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(),"Centrally Assessed");
			}
			
			parseNames(resultMap, nameOnServer, "");
			
			for (int i = 0; i < cols.length; i++) {
				cols[i].removeAttribute("width");
			}

		}
		resultMap.removeTempDef();
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static ArrayList<List> removeDuplicates(ArrayList<List> list) {
		ArrayList<List> newList = new ArrayList<List>();
		for (List l : list) {
			if (!newList.contains(l)) {
				newList.add(l);
			}
		}
		return newList;
	}

}

package ro.cst.tsearch.servers.functions;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class CASantaClaraAO {

	public static final String[] CITIES = {"AGNEW", "ALVISO", "CAMPBELL", "COYOTE", "CUPERTINO", 
								           "GILROY", "HOLY CITY", "LOS ALTOS", "LOS ALTOS HILLS", "LOS GATOS",
								           "MILPITAS", "MOFFET FIELD", "MONTE SERENO", "MORGAN HILL", "MOUNTAIN VIEW",
								           "MOUNT HAMILTON", "PALO ALTO", "REDWOOD ESTATES", "SAN JOSE", "SAN MARTIN",
								           "SANTA CLARA", "SARATOGA", "STANFORD", "SUNNYVALE", "WATSONVILLE",
								           "UNINCORPORATED"};

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (!StringUtils.isEmpty(address)) {
			String addr[] = address.split(" / ");
			if (addr.length>1) {
				String apn = (String)resultMap.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
				Vector pisVector = new Vector();
				for (int i=0;i<addr.length;i++) {
					
					String eachAddress = addr[i].replaceFirst("(?is)^\\s*0\\b", "").trim();		//"0 VIRGINIA AV", APN 404-07-039
					
					String streetNo = "";
					String streetName = "";
					String city = "";
					String zip = "";
					
					Matcher matcher = Pattern.compile("(.*)(\\d{5}-\\d{4})$").matcher(eachAddress);
					if (matcher.find()) {
						eachAddress = matcher.group(1).trim();
						zip = matcher.group(2);
					}
					
					eachAddress = eachAddress.replaceFirst("\\bCA\\s*$", "").trim();
					String addressAndCity[] = StringFormats.parseCityFromAddress(eachAddress, CITIES);
					if (StringUtils.isNotEmpty(addressAndCity[0])) {
						city = addressAndCity[0];
					}
					eachAddress = addressAndCity[1];
					
					streetNo = StringFormats.StreetNo(eachAddress);
					streetName = StringFormats.StreetName(eachAddress);
										
					if (!StringUtils.isEmpty(streetNo) || !StringUtils.isEmpty(streetName) || !StringUtils.isEmpty(city) || !StringUtils.isEmpty(zip)) {
						PropertyIdentificationSet pis = new PropertyIdentificationSet();
						pis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), apn);
						pis.setAtribute(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getShortKeyName(), address);
						pis.setAtribute(PropertyIdentificationSetKey.STREET_NO.getShortKeyName(), streetNo);
						pis.setAtribute(PropertyIdentificationSetKey.STREET_NAME.getShortKeyName(), streetName);
						pis.setAtribute(PropertyIdentificationSetKey.CITY.getShortKeyName(), city);
						pis.setAtribute(PropertyIdentificationSetKey.ZIP.getShortKeyName(), zip);
						pisVector.add(pis);
					}
				}
				resultMap.put("PropertyIdentificationSet", pisVector);
			} else {
				String eachAddress = address.replaceFirst("(?is)^\\s*0\\b", "").trim();		//"0 VIRGINIA AV", APN 404-07-039
				
				String streetNo = "";
				String streetName = "";
				String city = "";
				String zip = "";
				
				Matcher matcher = Pattern.compile("(.*)(\\d{5}-\\d{4})$").matcher(eachAddress);
				if (matcher.find()) {
					eachAddress = matcher.group(1).trim();
					zip = matcher.group(2);
				}
				
				eachAddress = eachAddress.replaceFirst("\\bCA\\s*$", "").trim();
				String addressAndCity[] = StringFormats.parseCityFromAddress(eachAddress, CITIES);
				if (StringUtils.isNotEmpty(addressAndCity[0])) {
					city = addressAndCity[0];
				}
				eachAddress = addressAndCity[1];
				
				streetNo = StringFormats.StreetNo(eachAddress);
				streetName = StringFormats.StreetName(eachAddress);
				
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
				
			}
		}
	}

}

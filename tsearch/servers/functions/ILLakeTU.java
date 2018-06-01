package ro.cst.tsearch.servers.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class ILLakeTU {

	public static void parseAddress(ResultMap resultMap)  {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address))
			return;
		
		String[] split = address.split("<br>");
		address = split[0];
		String streetName = StringFormats.StreetName(address).trim();
		String streetNo = StringFormats.StreetNo(address);
		if (streetNo.matches("0+")) {
			streetNo = "";
		}
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		if (!StringUtils.isEmpty(streetNo)) {
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		}
		if (split.length>1) {
			String cityZIP = split[1];
			Matcher matcher = Pattern.compile("(.*?)\\s*(\\d{5})$").matcher(cityZIP);
			if (matcher.find()) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), 
						matcher.group(1).trim());
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(),
						matcher.group(2));
			}
		}
	}
	
}

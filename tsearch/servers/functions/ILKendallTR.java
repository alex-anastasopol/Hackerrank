package ro.cst.tsearch.servers.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class ILKendallTR {

	public static final String[] CITIES = {"AURORA", "BOULDER HILL", "BRISTOL", "HELMAR", "JOLIET", 
									       "LISBON", "MILLBROOK", "MILLINGTON", "MINOOKA", "MONTGOMERY",						 
									       "NEWARK", "OSWEGO", "PLAINFIELD", "PLANO", "PLATTVILLE",
									       "SANDWICH", "YORKVILLE", "LITTLE ROCK"};

	public static void parseAddress(ResultMap resultMap)  {
		
		String address = (String) resultMap.get("tmpAddress");
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		Matcher ma = Pattern.compile("(?is)(.*?)\\b[A-Z]{2}\\b\\s+(\\d{5}(?:-(?:\\d{4})?)?)").matcher(address);
		if (ma.find()) {
			address = ma.group(1);
			String zip = ma.group(2);
			zip = zip.replaceFirst("-$", "");
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
		}
		
		String addressAndCity[] = StringFormats.parseCityFromAddress(address, CITIES);
		if (StringUtils.isNotEmpty(addressAndCity[0])) {
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), addressAndCity[0]);
		}
		address = addressAndCity[1];
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		
	}
	
}

package ro.cst.tsearch.servers.functions;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;

public class ILLakeAO {
	
	public static ResultMap  parseIntermediaryLink(String linkString){
		ResultMap map = new ResultMap();
		
		String[] address = linkString.split(",");
		if (address.length==2){
			map.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address[0].trim()));
			map.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address[0].trim()));
			
			String[] cityZip= address[1].trim().split("\\s+");
			if (cityZip.length==2){
				map.put("PropertyIdentificationSet.City", cityZip[0].trim());
				map.put("PropertyIdentificationSet.Zip", cityZip[1].trim());
			}
		}
		return map;
	}

}

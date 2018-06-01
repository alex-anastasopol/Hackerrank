package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

public class OHCuyahogaBOR {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ResultMap parseIntermediaryRow(String row, long searchId) {
				
		int listLen = 5;
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "BOR");
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "COMPLAINT");
		
		try {
			
			List<List> bodyPIS = new ArrayList<List>();
			List<String> bodyLine = new ArrayList<String>();
			StringBuilder names = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(row, null);
			NodeList nodeList = htmlParser.parse(null);
			
			if (nodeList.size()==5) {
					
				String complaintNumber = nodeList.elementAt(0).toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				if (!StringUtils.isEmpty(complaintNumber)) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), complaintNumber);
				}
					
				String year = nodeList.elementAt(1).toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
				if (!StringUtils.isEmpty(year)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}
				
				TableTag parcelTable = (TableTag)nodeList.elementAt(2).getFirstChild();
				if (parcelTable!=null) {
					for (int i=0;i<parcelTable.getRowCount();i++) {
						bodyLine = new ArrayList<String>();
						bodyLine.add(parcelTable.getRow(i).toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
						bodyPIS.add(bodyLine);
					}
				}
					
				TableTag nameTable = (TableTag)nodeList.elementAt(3).getFirstChild();
				if (nameTable!=null) {
					for (int i=0;i<nameTable.getRowCount();i++) {
						names.append(nameTable.getRow(i).toPlainTextString().replaceAll("(?is)&nbsp;", "").trim()).append(" / ");
					}
					String namesString = names.toString();
					namesString = namesString.replaceFirst("\\s*/\\s*$", "");
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), namesString);
					
					TableTag addressTable = (TableTag)nameTable.getNextSibling();
					if (addressTable!=null) {
						for (int i=0;i<addressTable.getRowCount();i++) {
							String rawAddress = addressTable.getRow(i).toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
							String streetNoName = "";
							String city = "";
							String[] split = rawAddress.split(",");
							if (split.length==2) {
								streetNoName = split[0].trim();
								city = split[1].trim();
							}
							String streetNo = StringFormats.StreetNo(streetNoName);
							String streetName = StringFormats.StreetName(streetNoName);
							if (i<bodyPIS.size()) {
								bodyLine = bodyPIS.get(i);
								bodyLine.add(streetNo);
								bodyLine.add(streetName);
								bodyLine.add(city);
								bodyLine.add("");			//ZIP	
							} else {
								bodyLine = new ArrayList<String>();
								bodyLine.add("");			//parcel ID
								bodyLine.add(streetNo);
								bodyLine.add(streetName);
								bodyLine.add(city);
								bodyLine.add("");			//ZIP
								bodyPIS.add(bodyLine);
							}
						}
					}
				}
				
				for (int i=0;i<bodyPIS.size();i++) {
					bodyLine = bodyPIS.get(i);
					int len = bodyLine.size();
					for (int j=0;j<listLen-len;j++) {
						bodyLine.add("");
					}
				}
				
				String date = nodeList.elementAt(4).toPlainTextString().replaceAll("(?is)&nbsp;", "").replaceFirst("\\d+:\\d+:\\d+.*", "").trim();
				if (!StringUtils.isEmpty(date)) {
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), date);
				}
				
				parseNames(resultMap, searchId);
					
				String[] header = {PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(),
								   PropertyIdentificationSetKey.STREET_NO.getShortKeyName(),
								   PropertyIdentificationSetKey.STREET_NAME.getShortKeyName(),
								   PropertyIdentificationSetKey.CITY.getShortKeyName(),
								   PropertyIdentificationSetKey.ZIP.getShortKeyName()};
				ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
				resultMap.put("PropertyIdentificationSet", rt);
										
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner)) {
			return;
		}
	   
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		ArrayList<List> list = new ArrayList<List>();
		
		String[] split = owner.split(" / ");
		for (int i=0;i<split.length;i++) {
			String ow = split[i];
			names = StringFormats.parseNameNashville(ow, true);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
				suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
			
		}
		
		try {
			resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(list, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}

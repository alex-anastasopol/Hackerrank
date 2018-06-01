package ro.cst.tsearch.servers.functions;


import java.util.ArrayList;
import java.util.List;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class COBroomfieldAO {
	
    
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&nbsp", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "")
									.replaceAll("(?is)</?h[\\d]*[^>]*>", "").replaceAll("(?is)</th>", "</td>").replaceAll("\n", "").replaceAll("</?div[^>]*>", "");
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");

		try {		
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String acctNumber = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Account Number:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), acctNumber);
			
			String parcelNumber = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parcel Number:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelNumber);

			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner Name:"), "", true).trim();
			m.put("tmpOwner", ownerName);
				
			String address = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Property Address:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			
			String legal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Legal Description:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
			
			String lot = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Lot:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			
			String block = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Block:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
			
			String subdivisionName = HtmlParser3.getValueFromAbsoluteCell(-2, 1, HtmlParser3.findNode(mainList, "Block"), "", true).trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
			
			String section = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Section:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
			
			String township = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Township:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
			
			String range = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Range:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
			
			String quarterSection = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Quarter Section:"), "", true).trim();
			m.put(PropertyIdentificationSetKey.QUARTER_VALUE.getKeyName(), quarterSection);
			
			String salesDate = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Sale Date:"), "", true).trim();
			m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), salesDate);
			
			String salesPrice = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Sale Price:"), "", true).trim();
			salesPrice = salesPrice.replaceAll("[\\$,]+", "");
			m.put(SaleDataSetKey.SALES_PRICE.getKeyName(), salesPrice);
			
			String landAppraisal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Assessed Land Value:"),"", true).trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landAppraisal);
			
			String improvementAppraisal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Assessed Improvements Value:"),"", true).trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvementAppraisal);
			
			String assessed = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Total Assessed Value"),"", true).trim();
			assessed = assessed.replaceAll("(?is)([\\d\\.]+).*", "$1");
			m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessed);
						
			try {
				partyNamesCOBroomfieldAO(m, searchId);
				COBroomfieldTR.parseLegalCOBroomfieldTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
		
	@SuppressWarnings("rawtypes")
	public static void partyNamesCOBroomfieldAO(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.replaceAll("\\d+\\s*%\\s*(INT(EREST)?)?\\b", "");
		stringOwner = stringOwner.replaceAll("\\s*-\\s*", "-");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\s*\\(\\s*", " ");
		stringOwner = stringOwner.replaceAll("\\s*\\)\\s*", " ");
		stringOwner = stringOwner.replaceAll("&\\s*WF", "");
		stringOwner = stringOwner.replaceAll("ETAL\\s*&\\s*", " ETAL ");
		stringOwner = stringOwner.replaceAll("\\s*&\\s*(\\w+)\\s+(\\w)", " AND $1 $2");
		stringOwner = stringOwner.replaceAll("\\bAND\\s+(\\w{2,}\\s+\\w{2,}(?:\\s+\\w{2,})?)\\s*$", " & $1");

		//157332112031
		stringOwner = stringOwner.replaceAll("\\b(\\w+)(\\s+\\w+\\s+(?:\\w+\\s+)?)AND\\s+(\\w+\\s+\\w+(?:\\s+\\w+)?)\\s+(AND\\s+\\w+(?:\\s+\\w+)?\\s*$)"
												, "$1 $2 & $3 $1 $4");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;
		String ln = "";
		//boolean coOwner = false;
		String[] owners = stringOwner.split(" & ");
		
		for (int i = 0; i < owners.length; i++) {
			if (i == 0) {
				names = StringFormats.parseNameNashville(owners[i], true);
				if (StringUtils.isNotEmpty(names[5]) && 
						LastNameUtils.isNotLastName(names[5]) && 
						NameUtils.isNotCompany(names[5])){
					names[4] = names[3];
					names[3] = names[5];
					names[5] = names[2];
				}
				ln = names[2];
			} else {
				names = StringFormats.parseNameDesotoRO(owners[i], true);
				//coOwner = true;
				
				// Task 7732 - PAGE MICHAEL SHANE AND CHRISTINE MARIE
				if(LastNameUtils.isNotLastName(names[2]) && FirstNameUtils.isFirstName(names[2])) {
					if(StringUtils.isEmpty(names[1]) && StringUtils.isNotEmpty(ln)) {
						names[1] = names[2];
						names[2] = ln;
						ln = "";
					}
				}
			}
			// B 5003
			if (stringOwner.matches("\\A\\w+\\s+\\w\\s+.*")){
				String[] toks = names[1].split("\\s+");
				if (toks[0].length() == 1){
					String temp = names[0];
					names[0] = toks[0];
					names[1] = temp + " " + names[1].replaceAll(toks[0] + "\\s+", "");
				}
			}
			//B 5003
			if (stringOwner.matches(".*\\s+AND\\s+\\w\\s+\\w+") && names[4].length() == 1 && names[3].length() > 1){
				String temp = names[3];
				names[3] = names[4];
				names[4] = temp;
			}
			names[2] = names[2].replaceAll("(?is),", "");
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);
		
	}
}

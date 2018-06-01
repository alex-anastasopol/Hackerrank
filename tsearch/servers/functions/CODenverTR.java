package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
*/

public class CODenverTR {

	
	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesCODenverTR(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		String stringOwner2 = org.apache.commons.lang.StringUtils.defaultIfEmpty((String) m.get("tmpCoOwner"), "").trim();
		if (StringUtils.isNotEmpty(stringOwner2)){
			stringOwner = stringOwner +  "@@@" + stringOwner2;
		}
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		
		String[] owners;
		owners = stringOwner.split("@@@");// for CODenverAO
		
		for (int i = 0; i < owners.length; i++) {
			
			names = StringFormats.parseNameNashville(owners[i], true);
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		String[] a = StringFormats.parseNameNashville(stringOwner.replaceAll("@@@", " & "), true);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner.replaceAll("@@@", " & "));
		
	}
	
	public static void parseAddressCODenverTR(ResultMap m, long searchId) throws Exception {
		
		String address = (String) m.get("tmpAddress");
		
		if (StringUtils.isEmpty(address))
			return;
		
		address = address.replaceAll("APPRX", "");
		address = address.replaceAll("VCNT", "");
		address = address.replaceAll("REAR", "");
		address = address.replaceAll("MASTR", "");
		address = address.replaceAll("MISC", "");
		address = address.replaceAll("#BBRD", "");
		address = address.replaceAll("\\d+\\s*-\\s*\\d+", "");
		address = address.replaceAll("(?is)-(\\d+)\\s*$", "#$1");
		
		m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
		m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void taxCODenverTR(ResultMap m, long searchId) throws Exception{
		
		String taxYear = (String) m.get("tmpTaxYear");
		if (StringUtils.isNotEmpty(taxYear)){
			taxYear = taxYear.replaceAll("(?is).*?Tax\\s*Year\\s*(\\d+)\\s*.*", "$1");
			if (taxYear.trim().matches("\\d+")){
				m.put("TaxHistorySet.Year", taxYear);
			}
		}
		
		String tmpInstalPaid1 = (String) m.get("tmpInstalPaid1");
		String tmpInstalPaid2 = (String) m.get("tmpInstalPaid2");
		String tmpFullPaid = (String) m.get("tmpFullPaid");
		String tmpInstalDue1 = (String) m.get("tmpInstalDue1");
		String tmpInstalDue2 = (String) m.get("tmpInstalDue2");
		//String tmpFullDue = (String) m.get("tmpFullDue");
		String tmpInstalPaidDate1 = (String) m.get("tmpInstalPaidDate1");
		String tmpInstalPaidDate2 = (String) m.get("tmpInstalPaidDate2");
		String tmpFullPaidDate = (String) m.get("tmpFullPaidDate");
		
		List<String> line = new ArrayList<String>();
		List<List> body = new ArrayList<List>();
		double amtPaid = 0.00;
		double amtDue = 0.00;
		
		if (StringUtils.isNotEmpty(tmpFullPaidDate)){
			m.put("TaxHistorySet.AmountPaid", tmpFullPaid);
			line.add(tmpFullPaidDate);
			line.add(tmpFullPaid);
			body.add(line);
		} else {
			if (StringUtils.isNotEmpty(tmpInstalPaidDate1)){
				amtPaid = amtPaid + Double.valueOf(tmpInstalPaid1);
				line = new ArrayList<String>();
				line.add(tmpInstalPaidDate1);
				line.add(tmpInstalPaid1);
				body.add(line);
			} else {
				amtDue = amtDue + Double.valueOf(tmpInstalDue1);
			}
			
			if (StringUtils.isNotEmpty(tmpInstalPaidDate2)){
				amtPaid = amtPaid + Double.valueOf(tmpInstalPaid2);
				line = new ArrayList<String>();
				line.add(tmpInstalPaidDate2);
				line.add(tmpInstalPaid2);
				body.add(line);
			} else {
				amtDue = amtDue + Double.valueOf(tmpInstalDue2);
			}
			
			m.put("TaxHistorySet.AmountPaid", Double.toString(amtPaid));
		}
		m.put("TaxHistorySet.TotalDue", Double.toString(amtDue));
		
		
		String[] header = {"ReceiptDate", "ReceiptAmount"};
		ResultTable rt = new ResultTable();
		rt = GenericFunctions2.createResultTable(body, header);
		m.put("TaxHistorySet", rt);
	}
		
	
}

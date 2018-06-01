package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLHernandoTR {
	
	public static void interFLHernandoTR(ResultMap m, long searchId) throws Exception {
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void	legalFLHernandoTR(ResultMap m, long searchId) throws Exception {
		
		String  parcelID = (String) m.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
		if (StringUtils.isNotEmpty(parcelID)){
			setFLHernandoPropertyType(m, parcelID);
			if (parcelID.trim().startsWith("R"));
			parcelID = parcelID.replaceAll("(?is)\\AR", "").replaceAll("(?is)\\s+", "");
			String section = parcelID.substring(0, 2);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
			String township = parcelID.substring(3, 5);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
			String range = parcelID.substring(5, 7);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
		}
		
	
		String subName = "";
		String[] tmpAdr = null;

		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		if (legal != null) {
			legal = GenericFunctions.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
			legal = legal.replaceFirst("(?is)(?:\\bA\\b)?\\s+\\bLOT\\b\\s+(\\d+X)+\\d+\\s*\\bFT\\b(?:\\s+A\\.?K\\.?A\\.?)?", "");
			legal = legal.replaceFirst("(?is)\\bLESS\\b\\s+THE\\b", "");
			legal = legal.replaceFirst("(?is)\\b[NSEW]\\b\\s*\\d+\\s*FT\\b", "");
			legal = legal.replaceFirst("(?is)\\bTHERE\\s*OF\\b\\s+AS\\s+DES(?:CRIEBED)?\\b\\s+\\bIN\\b", "");
			legal = legal.replaceFirst("(?is)(<br/?>\\s*)+", "<br>");
			
			tmpAdr = legal.split("(?is)<br>");
			if (tmpAdr.length > 1) {
				subName = tmpAdr[1];
				
				String subdivisionName = "";
				int unitIndex = tmpAdr[0].toLowerCase().indexOf("unit");
				if (unitIndex != -1) subdivisionName = tmpAdr[0].substring(0, unitIndex).trim();
				if (subdivisionName.length() != 0)	
					m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
				if (tmpAdr[0].toLowerCase().contains("condo")){
					m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), tmpAdr[0].trim());
				}

				if (!subName.equals(subName.replaceAll("(?is)lot", ""))) {
					subName = subName.replaceAll("(?is).*?(lot.*)", "$1");
					if (subName.toLowerCase().contains("orb ")) {
						subName = subName.replaceAll("(?is)ORB\\b\\s+\\d+(?:\\s+PG\\b\\s+\\d+)?", "");
					}
					subName = subName.replaceAll("(?is)lots", "");
					subName = subName.replaceAll("(?is)lot", "");
					if (subName.matches("(?is)\\s*(\\d+)\\s+\\1[WESN]\\s*")) {
						subName = subName.replaceFirst("(?is)(\\d+)\\s+\\1[WESN]", "$1");
					}
					m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(),
							subName.replaceAll("(?is)[^a-zA-Z0-9\\s-]+", "").trim());
				}
				subName = tmpAdr[1];
				if (!subName.equals(subName.replaceAll("(?is)blk", ""))) {
					subName = subName.replaceAll(
							"(?is).*?blk(.*)?((lot.*)|(E1.*)).*", "$1");
					m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
							subName.replaceAll("(?is)[^a-zA-Z0-9\\s]+", "").trim());

				}
				subName = tmpAdr[1];
				if (!subName.equals(subName.replaceAll("(?is)\\s+TR\\s+", ""))) {
					subName = subName.replaceAll(
							"(?is).*?\\s+tr\\s+([^\\s]+).*", "$1");
					m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(),
							subName.replaceAll("(?is)[^a-zA-Z0-9\\s]+", ""));

				}
			}
			if (tmpAdr.length > 0) {
				subName = tmpAdr[0];
				if (!subName.equals(subName.replaceAll("(?is).*\\s+PH\\s+.*",""))
						|| !subName.equals(subName.replaceAll("(?is).*\\s+PHase\\s+.*", ""))
						|| !subName.equals(subName.replaceAll("(?is).*\\s+PHs\\s+.*", ""))) {
					subName = subName.replaceAll("(?is).*?\\s+ph\\s+(.*)", "$1");
					subName = subName.replaceAll("(?is).*?\\s+phs\\s+(.*)",	"$1");
					subName = subName.replaceAll("(?is).*?\\s+PHASE\\s+(.*)", "$1");
					m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),
							subName.replaceAll("(?is)[^a-zA-Z0-9\\s]+", ""));
				}
				subName = tmpAdr[0];
				if (!subName.equals(subName.replaceAll("(?is).*\\s+Unit\\s+.*", ""))) {
					subName = subName.replaceAll("(?is).*?\\s+Unit\\s+([^\\s]+)", "$1");
					subName = subName.replaceAll("(?is)REPLAT", "");
					subName = subName.replaceAll("(?is)UNREC", "").trim();
					m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subName);
				}
				
				if (!subName.equals(subName.replaceFirst("(?is).*\\s+\\bUNREC\\b.*", ""))) {
					//this should be saved as subdiv name (R14 122 19 0251 0000 0010)
					subName = subName.replaceAll("(?is)(.*)\\s+\\bUNREC\\b.*", "$1");
					if (m.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName()) != null) {
						subName = subName.replaceFirst("(?is)\\bUNREC\\b", "").trim();
					} else {
						m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subName);
					}
				}
			}
		}
		String book = "";
		String page = "";

		List<String> line = new ArrayList<String>();
		List<List> bodyPlat = new ArrayList<List>();
		tmpAdr = legal.split("(?is)<br>");
		
		for (int i = 0; i < tmpAdr.length; i++) {
			line = new ArrayList<String>();
			if (!tmpAdr[i].equals(tmpAdr[i].replaceAll("(?is)ORB.*?PG", ""))) {
				if (!tmpAdr[i].equals(tmpAdr[i].replaceAll("(?is)ORB.*?Pgs\\s+.*", ""))) {
					page = tmpAdr[i].replaceAll("(?is)orb\\s+[a-zA-Z0-9-_,]*\\spgs(.*)", "$1");
					page = page.replaceAll("(?is)[^a-zA-Z0-9\\s]+", "");
					page = page.replaceAll("(?is)[\\s]+", " ");
					book = tmpAdr[i].replaceAll("(?is)orb\\s+([a-zA-Z0-9-_,]*)\\spgs.*", "$1");
					String[] multiplePage = page.split(" ");
					for (int j = 0; j < multiplePage.length; j++) {

						if (!"".equals(multiplePage[j])) {
							line = new ArrayList<String>();
							line.add(book.replaceAll("(?is)[^a-zA-Z0-9-_]+", ""));
							line.add(multiplePage[j].replaceAll("(?is)[^a-zA-Z0-9-_]+", ""));
							bodyPlat.add(line);
						}
					}

				} else {
					line.add(tmpAdr[i].replaceAll("(?is).*orb\\s+([a-zA-Z0-9-_,]*)\\s.*", "$1"));
					line.add(tmpAdr[i].replaceAll("(?is).*orb\\s+[a-zA-Z0-9-_,]*\\spg(.*)", "$1"));
					bodyPlat.add(line);
				}

			}
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });

			ResultTable rt = GenericFunctions2.createResultTable(bodyPlat, header);
			m.put("CrossRefSet", rt);

		}
	}
	
	private static void setFLHernandoPropertyType(ResultMap m, String temp) {
		String propertyType = "";

		if (StringUtils.isNotEmpty(temp)) {
			if (temp.startsWith("R")) {
				propertyType = "Real Estate";
			}

			if (temp.startsWith("M")) {
				propertyType = "Mobile Property";
			}

			if (temp.startsWith("P")) {
				propertyType = "Personal Property";
			}
			m.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), propertyType);
		}
	}
	
	
	public static void taxFLHernandoTR(ResultMap m, long searchId) throws Exception {
		
		String totalTaxDue = (String)m.get("tmpTotalTaxDue");
		String totalDue = (String)m.get("TaxHistorySet.TotalDue");
		
		if(StringUtils.isNotEmpty(totalTaxDue) 
				&& StringUtils.isNotEmpty(totalDue)
				&& totalTaxDue.matches("[0-9\\.]+")
				&& totalDue.matches("[0-9\\.]+")){
			
			m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), new BigDecimal(totalTaxDue).subtract(new BigDecimal(totalDue)).toString());
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseOwnerNames(String ownersName, ResultMap resultMap, long searchId) throws Exception {
		
		if (StringUtils.isEmpty(ownersName))
			return;
				
				
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		String[] owners = ownersName.split("<br>");
		String nameOnServer = "";
		
		for (String owner : owners){
		
			owner = owner.replaceAll("(?is)\\A\\s*\\d+\\s+", "");
			owner = owner.replaceAll("(?is)\\b(ET)\\s+", "$1");
			nameOnServer += owner + " & ";
			names = StringFormats.parseNameNashville(owner, true);
	
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
				
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
													NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
		String[] a = StringFormats.parseNameNashville(nameOnServer, true);
		resultMap.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		resultMap.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		resultMap.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		resultMap.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		resultMap.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		resultMap.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);
	}

}

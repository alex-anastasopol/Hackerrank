package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;

public class UTSaltLakeTR {
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 3) {
			
			String parcelNumber = cols[1].toPlainTextString()
				.replaceAll("<?td.*>", "")
				.replaceAll("<?span.*>", "")
				.replaceAll("<br>", "")
				.replaceAll("-", "")
				.replaceAll("\\s", "");
							
			resultMap.put("PropertyIdentificationSet.ParcelID",parcelNumber);
			
			String name = cols[2].toPlainTextString()
				.replaceAll("<?td.*>", "")
				.trim();
						
			resultMap.put("PropertyIdentificationSet.NameOnServer",name);
						
			parseNames(resultMap, searchId);

		}
		resultMap.removeTempDef();
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
			
		String owner = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		if (StringUtils.isEmpty(owner))
			   return;
		owner = owner.replaceAll(";\\s*JT", "");
		owner = owner.replaceAll(";\\s*ET\\s?AL", " ETAL");
		owner = owner.replaceAll(";\\s*TR", " TR");
		owner = owner.replaceAll(";\\s*TRS", " TR");
		owner = owner.replaceAll("\\(EMREV TR\\)", "");
		owner = owner.replaceAll("2.5 ACRES ", "");
		owner = owner.replaceAll("\\.", ",");					//SMITH. CONNIE T & LIVSEY, AMBER; JT			
		String[] ownerRows = owner.split("\n");
		StringBuffer stringOwnerBuff = new StringBuffer();
		for (String row : ownerRows){
			if (row.trim().matches("\\d+\\s+.*")){
				break;
			} else if (LastNameUtils.isNoNameOwner(row)) {
				break;
			} else {
				stringOwnerBuff.append(row + "\n");
			}
		}
		String stringOwner = stringOwnerBuff.toString();
		stringOwner = stringOwner.replaceAll("\n$", "");
		String[] nameLines = stringOwner.split("\n");

		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		StringBuffer nameOnServerBuff = new StringBuffer();
		for (int i=0; i < nameLines.length; i++){
			String ow = nameLines[i];
			ow = clean(ow);
			names = StringFormats.parseNameNashville(ow, true);
			if (!NameUtils.isCompany(ow)) suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			nameOnServerBuff.append("/").append(ow);
		}
		String nameOnServer = nameOnServerBuff.toString();
		nameOnServer = nameOnServer.replaceFirst("/", "");
		resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] a = StringFormats.parseNameNashville(owner, true);
		resultMap.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		resultMap.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		resultMap.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		resultMap.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		resultMap.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		resultMap.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}
		
	private static String clean(String str) {
		if(StringUtils.isEmpty(str)) return "";
		return 
			str .trim()
				.replaceAll("<br>", "\n")
				.replaceAll("\\b[\\d/]+\\s+INT(?:\\s+EA)?", "&")
				.replaceAll("\\b[W|H]\\s*&\\s*[H|W]\\b", "")
				.replaceAll("\\b[A-Z]\\s*/\\s*[A-Z]\\b", "")
				.replaceAll("(?is)\\bJTWRS\\b", "")
				.replaceAll("(?is)\\bPR\\b", "")
				.replaceAll("(?is)\\b-?POA\\b", "")
				.replaceAll("(?is)\\A\\s*OF\\s+", "")
				.replaceAll("(?is)\\bTRE\\b", "TRUSTEE")
				.replaceAll("(?is)\\bCO[-|\\s+](TR(?:USTEE)?S?)\\b", "$1");
		}
	
	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		
		NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("class", "taxDue"));
		
		double baseAmount = 0.0;
		double totalDue = 0.0;
		double priorDelinquent = 0.0;
						
		Node taxYear = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("class", "date")).elementAt(0);
		String year = taxYear.toPlainTextString().substring(20);			
		resultMap.put("TaxHistorySet.Year", year);
		
		TableTag taxTable = (TableTag) tableList.elementAt(2);
		TableRow[] rows = taxTable.getRows();
		
		for (int i = 1; i < rows.length; i++) {
			TableColumn[] columns = rows[i].getColumns();
			String colYear = columns[2].toPlainTextString().trim();
			if (colYear.equals(year)) 
			{
				try {
					baseAmount += Double.parseDouble(columns[3].toPlainTextString().trim().replaceAll(",",""));
				} catch (Exception e) {
					e.printStackTrace();
				
				}
				try {
					totalDue += Double.parseDouble(columns[11].toPlainTextString().trim().replaceAll(",",""));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (colYear.compareTo(year)<0)
				try {
					priorDelinquent += Double.parseDouble(columns[11].toPlainTextString().trim().replaceAll(",",""));
				} catch (Exception e) {
					e.printStackTrace();
				} 
			
		}	
		
		resultMap.put("TaxHistorySet.BaseAmount", Double.toString(baseAmount));
		resultMap.put("TaxHistorySet.TotalDue", Double.toString(totalDue));
		resultMap.put("TaxHistorySet.PriorDelinquent", Double.toString(priorDelinquent));
			
	}
}

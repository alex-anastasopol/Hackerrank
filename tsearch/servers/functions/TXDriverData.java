
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class TXDriverData {

	public static ResultMap parseRow(String rowHtml) { // TODO make the patterns static
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "DD");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rowHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String owner = "";
			String address = "";
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if(tables.size() == 2) { // Driver's License Search
				TableTag table1 = (TableTag) tables.elementAt(0);
				TableRow[] rows = table1.getRows();
				if(rows.length < 4) {
					return resultMap;
				}
				owner = rows[0].toPlainTextString().replaceAll("^\\[\\d+\\]\\s*", "");
				address = rows[1].toPlainTextString().replaceAll("&nbsp;", "") + "\n" +
					rows[2].toPlainTextString().replaceAll("&nbsp;", "");
				
				Matcher m = Pattern.compile("(?is)DL#:\\s*(\\d+)").matcher(rows[3].toPlainTextString());
				if(m.find()) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), m.group(1));
				}
				m = Pattern.compile("(?is)DOB:\\s*(\\d+/\\d+/\\d+)").matcher(rows[3].toPlainTextString());
				if(m.find()) {
					resultMap.put("tmpDOB", m.group(1));
				}
				
				// get last renewal date and last activity date
				String lastRenewal = "";
				String lastActivity = "";
				TableTag table2 = (TableTag) tables.elementAt(1);
				rows = table2.getRows();
				for(TableRow row : rows) {
					if(row.getColumnCount() == 5) {
						if(StringUtils.isEmpty(lastActivity)) {
							lastActivity = row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;", "").trim();
						}
						if(row.getColumns()[2].toPlainTextString().indexOf("Renew") > -1) {
							lastRenewal = row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;", "").trim();
							break;
						}
					}
				}
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), lastRenewal);
				resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), lastActivity);
				resultMap.put("tmpRenewalDate", lastRenewal);
				
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Driver License");
				resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Texas Driver's License");				
			} else if(rowHtml.matches("(?is)<pre>.*</pre>")) { // Vehicle Search 
				String rowText = rowHtml.replaceAll("\\s*\n\\s*", "\n");
				
				Matcher m = Pattern.compile("(?is)Owner:\\s*<b>(.*?)</b>").matcher(rowText);
				if(m.find()) {
					owner = m.group(1);
				}
				
				address = rowText.split("\n")[1].replaceFirst(" {2,}", "\n");
				
				m = Pattern.compile("\\bRegistration Date:\\s*(\\d+/\\d+/\\d+)").matcher(rowHtml);
				if(m.find()) {
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), m.group(1));
					resultMap.put("tmpRenewalDate", m.group(1));
				}
				m = Pattern.compile("\\bRegistration Expires:\\s*(\\d+)/(\\d+)").matcher(rowHtml);
				if(m.find()) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), m.group(1) + "/01/" + m.group(2));
					resultMap.put("tmpExpirationDate", m.group(1) + "/01/" + m.group(2));
				}
				
				m = Pattern.compile("\\bPlate:\\s*<b>(\\w+)</b>").matcher(rowHtml);
				if(m.find()) {
					resultMap.put("tmpPlate", m.group(1));
				}
				
				m = Pattern.compile("\\bVIN:\\s*<b>(\\w+)</b>").matcher(rowHtml);
				if(m.find()) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), m.group(1));
				}
				
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Motor Vehicle");
				resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Texas Department of Motor Vehicles");
			} else if(tables.size() >= 4) { // Voter Registration Search 
				TableTag table1 = (TableTag) tables.elementAt(0);
				TableRow[] rows = table1.getRows();
				owner = rows[0].toPlainTextString().trim();
				address = rows[1].toPlainTextString().replaceAll("&nbsp;", "").trim() + "\n" +
					rows[2].toPlainTextString().replaceAll("&nbsp;", "").trim();
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				
				TableTag table2 = (TableTag) tables.elementAt(tables.size() - 1);
				String tableText = table2.toPlainTextString().replaceAll("&nbsp;", " ");
				
				Matcher m = Pattern.compile("(?is)\\bCert:\\s*(\\d+)").matcher(tableText);
				if(m.find()) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), m.group(1));
				}
				
				m = Pattern.compile("(?is)\\bDOB:\\s*([\\d/]+)").matcher(tableText);
				if(m.find()) {
					resultMap.put("tmpDOB", m.group(1));
				}
				
				m = Pattern.compile("\\bRegistration Roles Date:\\s*(\\d+/\\d+/\\d+)").matcher(tables.toHtml());
				if(m.find()) {
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), m.group(1));
				}
				
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Voter Registration");
				resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Texas Voter Registration");
			}
			
			parseNames(resultMap, owner);
			parseAddress(resultMap, address);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultMap;
	}

	private static void parseAddress(ResultMap resultMap, String address) {
		String[] lines = address.split("\n");
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(lines[0]));
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(lines[0]));
		
		Matcher m = Pattern.compile("(?is)(.*),\\s*TX\\s+(\\d+)").matcher(lines[1]);
		if(m.find()) {
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), m.group(1));
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), m.group(2));
		}
	}

	private static void parseNames(ResultMap resultMap, String ownerName) {
		String owner = ownerName;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		String[] lines = splitAndFixLines(owner, companyExpr);
		
		String docType = (String) resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());
		resultMap.put("tmpAll", ownerName);
		if(docType.equals("Driver License")) {
			COFremontAO.genericParseNames(resultMap, "GrantorSet", lines, null, companyExpr, 
					new Vector<String>(), new Vector<String>(), false, COFremontAO.ALL_NAMES_LF, -1);
		} else {
			COFremontAO.genericParseNames(resultMap, "GrantorSet", lines, null, companyExpr, 
					new Vector<String>(), new Vector<String>(), false, COFremontAO.ALL_NAMES_FL, -1);
		}
	}

	private static String[] splitAndFixLines(String owner, Vector<String> companyExpr) {
		ArrayList<String> lines = new ArrayList<String>();
		String[] names = owner.split("\\band\\b");
		boolean allAreCompanies = true;
		
		for(String name : names) {
			if(NameUtils.isNotCompany(name)) {
				allAreCompanies = false;
			}
		}
		if(allAreCompanies) {
			lines.add(owner);
		} else {
			for(String name : names) {
				lines.add(name);
			}
		}
		
		return lines.toArray(new String[lines.size()]);
	}

}

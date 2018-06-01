
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
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
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 */
public class NVGenericCountyTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {

		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[0].toPlainTextString().trim());
		
		parseNames(resultMap, row.getColumns()[2].toPlainTextString().trim(), false);
		
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		Matcher m = Pattern.compile("Parcel\\s*#\\s*(([0-9A-Z]|-)+)").matcher(detailsHtml);
		if(m.find()) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), m.group(1));
		}
		
		try {
			detailsHtml = detailsHtml.replaceAll("(?is)<br>", "\n").replaceAll("&nbsp;", "");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "mainTable"), true).elementAt(0);
			TableTag table1 = (TableTag) mainTable.getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			TableTag table2 = (TableTag) mainTable.getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(1);
		
			String labels1 = table1.getRow(0).getColumns()[0].toPlainTextString().trim();
			String dataCell1 = table1.getRow(0).getColumns()[1].toPlainTextString().trim();
			String ownerInfo = "";
			String address = "";
			if(labels1.indexOf("Current Owner") > -1) {
				ownerInfo = dataCell1.split("\n", 3)[2];
				address = dataCell1.split("\n", 3)[1];
			} else {
				ownerInfo = dataCell1.split("\n", 2)[1];
				address = dataCell1.split("\n", 2)[0];
			}
			parseAddress(resultMap, address.split(",")[0]);
			
			String dataCell2 = table1.getRow(0).getColumns()[3].toPlainTextString().trim();
			String year = dataCell2.split("\n")[1];
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
		
			double delinquentAmount = 0d;
			TableRow[] rows = table2.getRows();
			for(TableRow row : rows) {
				String rowText = row.toPlainTextString().toLowerCase();
				TableColumn[] cols = row.getColumns();
				
				if(rowText.indexOf("totals") > -1) {
					String tax = cols[1].toPlainTextString().replaceAll(",", "");
					String total = cols[3].toPlainTextString().replaceAll(",", "");
					String paid = cols[4].toPlainTextString().replaceAll(",", "");
					String totalDue = String.valueOf(Double.valueOf(total) - Double.valueOf(paid));
					
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), tax);
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), paid);
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
				}
				if(cols[0].toPlainTextString().trim().matches("\\d+")){
					delinquentAmount += Double.valueOf(cols[3].toPlainTextString().replaceAll(",", ""));
					delinquentAmount -= Double.valueOf(cols[4].toPlainTextString().replaceAll(",", ""));
				}
			}
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), String.valueOf(delinquentAmount));
			
			// parse payment history table
			TableTag taxHistTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxHistTable"), true).elementAt(0);
			List<List> body = new ArrayList<List>();
			rows = taxHistTable.getRows();
			for(TableRow row : rows) {
				if(row.getColumnCount() == 5) {
					String col2 = row.getColumns()[2].toPlainTextString().trim();
					String col3 = row.getColumns()[3].toPlainTextString().trim();
					if(col3.indexOf("-") > -1) {
						List<String> line = new ArrayList<String>();
						line.add(col2);
						line.add(col3.replaceAll("-", ""));
						body.add(line);
					}
				}
			}
			String[] header = {"ReceiptDate", "ReceiptAmount"};
			ResultTable rt = GenericFunctions2.createResultTable(body, header);
			if (rt != null){
				resultMap.put("TaxHistorySet", rt);
			}
			
			parseNames(resultMap, ownerInfo, true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		String streetNo = StringFormats.StreetNo(address);
		String streetName = StringFormats.StreetName(address);
		
		streetName = streetName.replaceAll("\\bPAR\\b", "#"); // unit
		streetName = streetName.replaceAll("\\bFR\\b", ""); // [Lyon] instr# 017-321-04 --> 2465 E 5TH ST FR PAR 4
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
	}

	public static void parseNames(ResultMap resultMap, String ownerInfo, boolean hasAddress) {
		
		String owner = ownerInfo;
		if(StringUtils.isEmpty(ownerInfo)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("PAIUTE");
		companyExpr.add("HUD & SONS");
		companyExpr.add("MCFARLAND & HULLINGER");
		companyExpr.add("PACIFIC");
		companyExpr.add("IND[.]");
		Vector<String> excludeWords = new Vector<String>();
		excludeWords.add("ST");
		excludeWords.add("GEORGE");
		excludeWords.add("WAY");
		excludeWords.add("SIXTH");
		excludeWords.add("6TH");
		excludeWords.add("FLOOR");
		excludeWords.add("VALLEY");
		excludeWords.add("PL");
		excludeWords.add("RANCH");
		
		owner = fixLines(ownerInfo);
		String[] lines = owner.split("\n");
		if(hasAddress) {
			if(lines.length > 1 && lines[lines.length-2].matches("(\\d+|P\\s*O\\s+BOX).*")) {
				lines = GenericFunctions.removeAddressFLTR(lines, excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE, true);
			} else {
				lines = GenericFunctions.removeAddressFLTR(lines, excludeWords, new Vector<String>(), 1, Integer.MAX_VALUE, true);
			}
		}
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i], companyExpr);
		}
		lines = splitLines(lines, companyExpr);
		for(int i = 0; i < lines.length; i++) {
			lines[i] = lines[i].replaceAll("%", "").trim();
		}
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.ALL_NAMES_LF, -1);
	}

	private static String[] splitLines(String[] lines, Vector<String> companyExpr) {
		
		ArrayList<String> properLines = new ArrayList<String>();
		for(String line : lines) {
			line = line.trim();
			if(COFremontAO.isCompany(line, companyExpr)) {
				properLines.add(line);
			} else {
				String[] parts = line.replaceAll("%\\s*", "").split("\\s*(&|(?<!\\d)/(?!\\d))\\s*");
				if(line.startsWith("%") || line.matches("\\w+\\s+(\\w\\s+)?&.*")) {
					for(int i = parts.length - 1; i >= 0; i--) {
						String part = parts[i];
						if(i == parts.length - 1) {
							properLines.add("[FML]" + part);
						} else {
							properLines.add(part);
						}
					}
				} else {
					for(String part : parts) {
						if(!StringUtils.isEmpty(part)) {
							properLines.add(part);
						}
					}
				}
			}
		}
		
		return properLines.toArray(new String[properLines.size()]);
	}

	private static String cleanName(String owner, Vector<String> companyExpr) {
		
		String properOwner = owner;
		properOwner = properOwner.replaceAll("\\bU/C\\b", "");
		properOwner = properOwner.replaceAll("UNKNOWN\\s+OWNER", "");
		properOwner = properOwner.replaceAll("TAX\\s+DEPARTMENT", "");
		properOwner = properOwner.replaceAll("\\bC/O\\b", "%");
		properOwner = properOwner.replaceAll("\\bUNIT(\\d+)", "#$1");
		properOwner = properOwner.replaceAll("\\bATTN\\b.*", "");
		properOwner = properOwner.replaceAll(",\\s*$", "");
		properOwner = COLarimerTR.genericCleanName(properOwner, new Vector<String>(), "&");
		
		return properOwner;
	}

	private static String fixLines(String owner) {

		StringBuilder sb = new StringBuilder();
		String[] lines = owner.split("\n");
		Pattern p = Pattern.compile("(,[^&/,]*),");
		for(String line : lines) {
			if(!line.matches("(?is).*(\\d+|REV(OCABLE)?|LIV(ING)?|FAM(ILY)?|VIV(OS)?).*")) {
				line = line.replaceAll("\\bTR\\b", "TRUSTEE") + "\n";
			} else {
				line = line.replaceAll("\\bTR\\b", "TRUST") + "\n";				
			}
			if(NameUtils.isNotCompany(line)) {
				Matcher m = p.matcher(line);
				while(m.find()) {
					line = m.replaceFirst("$1 & ");
					m.reset(line);
				}
			}
			sb.append(line + "\n");
		}
		String properOwner = sb.toString().trim();
		properOwner = properOwner.replaceAll("#(\\d+)", "UNIT$1");
		properOwner = properOwner.replaceAll("\\b(?:CO(?:-| )?)?TT\\b", "TRUSTEE");
		properOwner = properOwner.replaceAll("\\b(?:CO(?:-| )?)?TRS\\b", "TRUSTEES");
		properOwner = properOwner.replaceAll("(?:,\\s*)?\\b(?:CO(?:-| )?)?(T(?:(?:RU?)?S)?TEE?S?)\\b", "\n$1\n");
		properOwner = properOwner.replaceAll("(?:,\\s*)?\\b(ET)\\s*(AL|UX|VIR)\\b[.]?", "\n$1$2\n");
		properOwner = properOwner.replaceAll("\\s*\n\\s*", "\n");
		
		return properOwner;
	}

}

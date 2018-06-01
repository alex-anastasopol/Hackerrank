
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

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class ARBentonTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {

		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[0].toPlainTextString().trim());
		
		parseNames(resultMap, row.getColumns()[1].toPlainTextString()
				.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").trim());
		
		String address = row.getColumns()[2].toPlainTextString().replaceAll("&nbsp;", " ").trim();
		if(!address.matches("P\\s*O\\s+BOX.*")) {
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		}
		
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), row.getColumns()[5].toPlainTextString().replaceAll("&nbsp;", " ").trim());
		
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		try {
			String properHtml = detailsHtml.replaceAll("(?is)<br>", "\n").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(properHtml, null);
			NodeList nodeList = parser.parse(null);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), false);
			
			String owner = "";
			String address = "";
			String legal = "";
			String datePaid = "";
			String amountPaid = "";
			String receiptAmount = "";
			
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag)tables.elementAt(i);
				String tableText = table.toPlainTextString();
				if(tableText.indexOf("Parcel information") > -1) { // parse general info
					String cellText = table.getRow(1).getColumns()[0].toPlainTextString().trim();
					String[] fields = cellText.split("\\s*\n\\s*", 3);
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), fields[0]);
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), fields[1]);
					String[] ownerInfo = fields[2].split("\n");
					owner = ownerInfo[0].trim();
					address = ownerInfo[1].trim();
					legal = ownerInfo[2].trim();
					if(legal.indexOf("Business Personal") > -1 && NameUtils.isCompany(address)) {
						owner += "\n" + address.replaceFirst("%", "");
						address = "";
					}
				}
				
				else if (tableText.indexOf("Tax Description") > -1) {
					TableTag table2 = (TableTag) table.getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
					if (table2 != null) {
						TableRow[] rows = table2.getRows();
						Double amountDue = 0.00, delinquentAmount = 0.00;
						Double baseAmount = 0.0;

						for (TableRow row : rows) {
							if (row.getHeaderCount() != 0)
								continue;
							if (row.getColumnCount() == 6) {
								TableColumn[] cols = row.getColumns();
								if (cols[5].toPlainTextString().equals(resultMap.get("TaxHistorySet.Year")))
								{
									Matcher m = Pattern.compile("(?is)Ad\\s+Valorem").matcher(row.toPlainTextString());
									if (m.find())
									{
										baseAmount = Double.parseDouble(cols[3].toPlainTextString().replaceAll(",", ""));
										amountDue = Double.parseDouble(cols[4].toPlainTextString().replaceAll(",", ""));
									}
									Matcher m1 = Pattern.compile("(?is)Homestead\\s+Credit").matcher(row.toPlainTextString());
									if (m1.find())
									{
										baseAmount += Double.parseDouble(cols[3].toPlainTextString().replaceAll(",", ""));
										amountDue += Double.parseDouble(cols[4].toPlainTextString().replaceAll(",", ""));
									}
								}
								if (Double.parseDouble(cols[5].toPlainTextString()) < Double.parseDouble(resultMap.get("TaxHistorySet.Year").toString()))
									delinquentAmount += Double.parseDouble(cols[4].toPlainTextString().replaceAll(",", ""));
							}
						}
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount.toString());
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue.toString());
						resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), delinquentAmount.toString());
					}
				}
				else if (tableText.indexOf("Mandatory Owed") > -1) {// parse tax amounts
					tableText = tableText.replaceAll(",|\\$", "");
					Matcher m = Pattern.compile("(?is)Mandatory\\s+Owed\\s+([\\d.-]+)\\s+Paid\\s+([\\d.-]+)").matcher(tableText);
					if (m.find()) {
						amountPaid = m.group(2).replaceAll("-", "");
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);		
					}

				} else if(tableText.indexOf("Receipt History") > -1) { // parse payment history
					
					List<List> body = new ArrayList<List>();
					
					TableTag historyTable1 = (TableTag) table.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView3"), true).elementAt(0);
					if(historyTable1 != null) {
						TableRow[] rows = historyTable1.getRows();
						for(TableRow row : rows) {
							if(row.getColumnCount() == 3 && row.toPlainTextString().trim().matches("\\d+.*")) {
								TableColumn[] cols = row.getColumns();
								List<String> line = new ArrayList<String>();
								
								line.add(cols[0].toPlainTextString().replaceAll("[^\\d]", ""));
								datePaid = cols[1].toPlainTextString().trim();
								line.add(datePaid);
								receiptAmount = cols[2].toPlainTextString().replaceAll(",|\\$|-", "");
								line.add(receiptAmount);
								if(receiptAmount.equals(amountPaid)) {
									resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), datePaid);
								}
								body.add(line);
							}
						}
					}
					
					TableTag historyTable2 = (TableTag) table.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView4"), true).elementAt(0);
					if(historyTable2 != null) {
						TableRow[] rows = historyTable2.getRows();
						for(TableRow row : rows) {
							if(row.getColumnCount() == 3 && row.toPlainTextString().trim().matches("\\d+.*")) {
								TableColumn[] cols = row.getColumns();
								List<String> line = new ArrayList<String>();
								line.add(cols[0].toPlainTextString().replaceAll("[^\\d]", ""));
								line.add(cols[1].toPlainTextString().trim());
								line.add(cols[2].toPlainTextString().replaceAll(",|\\$|-", ""));
								body.add(line);
							}
						}
					}
					
					String[] header = {"ReceiptNumber", "ReceiptDate", "ReceiptAmount"};
					ResultTable rt = GenericFunctions2.createResultTable(body, header);
					if (rt != null){
						resultMap.put("TaxHistorySet", rt);
					}
				}
			}
			
			Matcher m = Pattern.compile("(?is)<p>Subdivision: (.*)</p>").matcher(properHtml);
			if(m.find()) {
				String sub = m.group(1);
				sub = sub.replaceAll("\\d+-\\d+-\\d+", "");
				sub = sub.replaceAll("(.*)-.*", "$1");
				sub = sub.replaceAll("\\b(PH|BLK|UNIT|LOT)\\s+(\\d+|[IVX]+).*", "");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), sub);
			}
			
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			
			parseNames(resultMap, owner);
			parseLegal(resultMap, legal);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static void parseLegal(ResultMap resultMap, String legalDesc) {
		String legal = legalDesc;
		
		legal = legal.replaceAll("(?is)Personal Property", "");
		legal = legal.replaceAll("(?is)Business Personal", "");
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		if(StringUtils.isEmpty(legal)) {
			return;
		}

		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String tract = "";
		
		// extract sec-twn-rng
		Matcher m = Pattern.compile("(\\d+)-(\\d+)-(\\d+)").matcher(legal);
		if(m.find()) {
			String sec = LegalDescription.cleanValues(m.group(1), false, true);
			String twn = LegalDescription.cleanValues(m.group(2), false, true);
			String rng = LegalDescription.cleanValues(m.group(3), false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twn);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng);
		}
		
		// extract lots
		m = Pattern.compile("(?is)(?<=\\bLO?TS?)\\s+(\\d+[A-Z]?)(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			lot += m.group(1);
			if(m.group(3) != null) {
				lot += "-" + m.group(3);
			}
			lot += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(lot.length() > 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		
		// extract block
		m = Pattern.compile("(?is)\\bBL(?:OC)?K\\s+(\\w+)\\b").matcher(legal);
		while(m.find()) {
			blk += m.group(1) + ",";
			legal = m.replaceFirst("");
			m.reset(legal);
		}
		m = Pattern.compile("(?is)(?<=\\bBL(?:OC)?KS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			blk += m.group(1);
			if(m.group(3) != null) {
				blk += "-" + m.group(3);
			}
			blk += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(blk.length() > 0) {
			blk = LegalDescription.cleanValues(blk, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
		}
		
		// extract unit
		m = Pattern.compile("(?is)(?<=\\bUNITS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			unit += m.group(1);
			if(m.group(3) != null) {
				unit += "-" + m.group(3);
			}
			unit += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(unit.length() > 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}
		
		//extract phase
		m = Pattern.compile("(?is)(?<=\\bPH(?:ASE)?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			phase += m.group(1);
			if(m.group(3) != null) {
				phase += "-" + m.group(3);
			}
			phase += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}
		
		// extract tract
		m = Pattern.compile("(?is)(?<=\\bTRACTS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU|AND)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			tract += m.group(1);
			if(m.group(3) != null) {
				tract += "-" + m.group(3);
			}
			tract += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(tract.length() > 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
		}
		
		// extract plat book-page
		m = Pattern.compile("(?is)\\b(?:RE)?PLAT\\s+(?:\\d+[./]\\d+[./]\\d+\\s+)?(\\w+)-(\\w+)").matcher(legal);
		if(m.find()) {
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), m.group(1));
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), m.group(2));			
		}
	}

	public static void parseNames(ResultMap resultMap, String ownerName) {
		String owner = ownerName;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("\\d+/\\d+/\\d+");
		companyExpr.add("\\bTRUST\\b");
		companyExpr.add("\\bBVPOA\\b");
		
		owner = fixLines(owner);
		String[] lines = owner.split("\n");
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		lines = splitAndFixLines(lines, companyExpr);
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.ALL_NAMES_LF, -1);
	}

	private static String[] splitAndFixLines(String[] lines, Vector<String> companyExpr) {
		ArrayList<String> newLines = new ArrayList<String>();
		Pattern p1 = Pattern.compile("(?is)((\\w+).*TR(?:UST)?)\\s*-\\s*(.*)");
		Pattern p2 = Pattern.compile("(?is)(.*)&(.*TRUST.*)");
		Pattern p3 = Pattern.compile("(?is)(([A-Z]+).*TRUST)\\s+(([A-Z]+).*)");
		for(String line : lines) {
			if(COFremontAO.isCompany(line, companyExpr)) {
				Matcher m2 = p2.matcher(line);
				if(m2.find()) {
					if(!COFremontAO.isCompany(m2.group(1), companyExpr)) {
						newLines.add(m2.group(1));
						line = m2.group(2).trim();
					}
				}
				Matcher m1 = p1.matcher(line);
				if(m1.find()) {
					newLines.add(m1.group(1));
					String extraNames = m1.group(2) + "," + m1.group(3);
					String[] parts = extraNames.split("(?is)&|\\bAND\\b|\\bOR\\b");
					for(String part : parts) {
						newLines.add(part.trim());
					}
					continue;
				}
				Matcher m3 = p3.matcher(line);
				if(m3.find()) {
					if(FirstNameUtils.isFirstName(m3.group(4)) || LastNameUtils.isLastName(m3.group(2))) {
						newLines.add(m3.group(1));
						line = m3.group(2) + " "  + m3.group(3);
					}
				}
				String[] parts = line.split("\\s-\\s");
				if(parts.length > 1) {
					parts = splitAndFixLines(parts, companyExpr);
				}
				for(String part : parts) {
					newLines.add(part);
				}
			} else {
				String[] parts = line.split("(?is)&|-|\\bAND\\b|\\bOR\\b");
				for(String part : parts) {
					newLines.add(part.trim());
				}
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}

	private static String cleanName(String name) {
		String properName = name;
		properName = properName.replaceAll("IRA\\s+SERVICES", "IRA_SERVICES"); // prevent from cleaning "IRA" from company name
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "");
		properName = properName.replaceAll("IRA_SERVICES", "IRA SERVICES");
		properName = properName.replaceAll("&\\s*$", "");
		properName = properName.trim();
		
		return properName;
	}

	private static String fixLines(String owner) {
		String properOwner = owner;
		properOwner = properOwner.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF))?\\b", "\n$1\n");
		properOwner = properOwner.replaceAll("(?is)\\bET\\s*(AL|UX|VIR)\\b", "\nET$1\n");
		properOwner = properOwner.replaceAll("\\bFBO\\b", "\n");
		properOwner = properOwner.replaceAll("\\b(TR(UST)?)-", "$1 - ");
		properOwner = properOwner.replaceAll("\\b([A-Z])-([A-Z]{2,})\\b", "$1 - $2");
		properOwner = properOwner.replaceAll("\\s*\n\\s*", "\n").trim();
		
		return properOwner;
	}
}

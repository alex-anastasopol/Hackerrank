package ro.cst.tsearch.servers.functions;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;


import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


public class MOStLouisCityTR {

	
		public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
			ResultMap resultMap = new ResultMap();
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			TableColumn[] cols = row.getColumns();
			if(cols.length == 3) {
				String pid = cols[0].childAt(1).getFirstChild().toPlainTextString().trim();
				String address = cols[1].toPlainTextString().replaceAll("&nbsp;","").trim();
				String unparsedNames = cols[2].toPlainTextString().trim();
				
				if (StringUtils.isNotEmpty(pid)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
				}
				
				if (StringUtils.isNotEmpty(address)) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				}
				
				if (StringUtils.isNotEmpty(unparsedNames)) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), unparsedNames);
					parseNames(resultMap, searchId);
				}
			}
			return resultMap;
		}
		
		
		public static void parseDetails(NodeList list, ResultMap resultMap, long searchId) {
			String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
			if (StringUtils.isNotEmpty(address)) {
				if (address.contains("Primary address:") && address.contains("Related addresses:")) {
					//only primary address mapped
					address = address.replaceAll("(?is)<font[^>]+>([^<]+)</font>\\s*([^<]+)","$1 $2");
					resultMap.put(OtherInformationSetKey.REMARKS.getKeyName(), address);
					address = address.replaceFirst("(?is)Primary[^:]+:\\s*([^;]+);.*", "$1");
				} else {
					address = address.replaceAll("(?is)</?br\\s*>", "");
					
				}
				if (address.matches("(?is)\\d+\\s*[\\w-'\\s&\\.]+\\s+(ST|AVE|AV|BLVD|BLV|DR|CT|PL)\\s*$")) {
					String suffix = address.replaceFirst("(?is)\\d+\\s*[\\w-'\\s&\\.]+\\s+(ST|AVE|AV|BLVD|BLV|DR|CT)\\s*", "$1");
					resultMap.put(PropertyIdentificationSetKey.SUFFIX.getKeyName(), StringFormats.StreetName(suffix));
				}
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				
			}
			
			parseNames(resultMap, searchId);
			
			parseLegal(resultMap, searchId);
			
			parseTaxes(list, resultMap, searchId);
		}	
		
		
		@SuppressWarnings("rawtypes")
		public static void parseNames(ResultMap resultMap, long searchId) {
			String unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
			List<List> body = new ArrayList<List>();
			
			if(StringUtils.isNotEmpty(unparsedName)) {
				unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
				
				unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
				unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
				
				String[] mainTokens = unparsedName.split("\\s*<br>\\s*");
												
				for (int i = 0; i < mainTokens.length; i++) {
					String currentToken = mainTokens[i];
					String[] names;
						
					if (currentToken.matches("(?is)[\\w'-]+\\s*,\\s*(?:[A-Z]\\b\\s*\\w+|\\w+\\s*[A-Z])\\s*(?:II|I|SR|JR)?\\s*&\\s*(?:\\w+\\s*[A-Z]?\\s[A-Z]\\w+)")) {  
						//Parcel# 27040001600: WILKERSON, JERRY M & MACIE L PICKENS  ||  Parcel# 38920005020: ADAMS, H DOUGLAS & HILDA CHASKI
						names = StringFormats.parseNameDesotoRO(currentToken);
					
					} else {
						//Parcel# 27030001900: CARROLL, DANIEL Z SR   ||  Parcel# 38920005000: BERG, ALEX W TRS
						//Parcel# 27040001200: TOAL, KEVIN R II & KAREN G  ||  Parcel# 38920004210: HICKS, MURIEL I & ROBERT W
						names = StringFormats.parseNameNashville(currentToken, true);	
					}
						
					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
						
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);												
				}
				
				try {
					GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		public static void parseLegal(ResultMap resultMap,long searchId) {
			
			String fullLegal = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
			if(StringUtils.isEmpty(fullLegal)) {
				return;
			}
			
			fullLegal = fullLegal.replaceAll("(?is)</?\\s*br\\s*>","<br>");
			
			Pattern p = Pattern.compile("(?is)\\s*((?:C\\.?|\\d+)\\s*B\\.?\\s*\\d+[^<]+<br>[\\w\\d\\s\\.]+\\d+\\s*(?:FT|ACRES)\\s*[^<]+<br>\\s*([^<]+<br>\\s*[^<]+<br>)\\s*(?:[^<]+<br>)?)");
	  	   	Matcher ma = p.matcher(fullLegal);
	  	   	
			while (ma.find()) {
				fullLegal = fullLegal.replaceFirst(ma.group(1), ma.group(2));
			}
			
			fullLegal = fullLegal.replaceFirst("(?is)\\bB(?:OU)?ND\\b\\s*(?:N|S|W|E)\\s*-?\\d+\\s*\\bFT\\b\\s*\\d+\\s*\\bIN\\b\\s*(<br>\\s*)(?:N|S|W|E)\\s*\\b(?:N|S|E|W)L\\b\\s*\\bOF\\b\\s*\\w+\\s*(?:ST|DR|CT|AV)?(?:\\s*&\\s*ALLEY)?","$1");
			fullLegal = fullLegal.replaceFirst("(?is)(?:N(?:ORTH)?|S(?:OUTH?)|E(?:AST)?|W(?:EST)?)?\\s*(?:[\\d/]+\\s*(?:\\bFT\\b)?)\\s*OF\\s*\\b(BL(?:OC)?K|LOTS|LOT|LT|\\w+)\\b\\s*", " $1");
			fullLegal = fullLegal.replaceFirst("(?is)\\s*\\b(BL(?:OC)?K|LOTS|LOT|LT)\\b\\s*<br>\\s*([\\d\\s-]+)", " $1 $2");
			fullLegal = fullLegal.replaceAll("(?is)<br>\\s*<br>","<br>");
			
			String[] info = fullLegal.split("<br>"); 
			String subdiv = "";
			String lot = "";
			String block = "";
			String tract = "";
			
			for (int i = 0; i < info.length; i++) {
				String tmpLegal = info[i];
				
				tmpLegal = tmpLegal.replaceAll("(?is)\\s*[\\d.\\s]+ACS\\s*", "");
				if (tmpLegal.contains("TRACT ")) {
					tract = tmpLegal.replaceFirst("(?is)\\bTRACT\\b\\s*(?:(?:N|S|E|W)-?)?\\s*(\\d+)(?:[\\w&\\s]+(?:ST|CT|PL|DR|BLD))", "$1");
					tmpLegal = tmpLegal.replaceFirst("\\bTRACT\\b\\s*(?:(?:N|S|E|W)-?)?\\s*(\\d+)", "");
				}
				
				if (tmpLegal.contains(" ADDN") || tmpLegal.contains(" SUBDN")) {
    				//subdiv = subdiv.replaceFirst("(?is)(?:BND\\s*[\\d-\\w\\s/]+FT)?[\\d/\\s]+\\bIN\\b", "");
					subdiv = tmpLegal.replaceFirst("(?is)(.*)\\s*\\b(?:ADDN|SUBDN)\\b\\s*", "$1");
				}
					
				if (tmpLegal.contains("LOT ") || tmpLegal.contains("LOTS ") || tmpLegal.contains("LT ") || tmpLegal.contains("LTS ")) {
					lot = tmpLegal.replaceAll("(?is).*(\\bL(?:T|OT)(?:S)?\\b)\\s*((?:(?:N|S|E|W)?-?\\s*\\d+\\s*(?:N|S|E|W)?-?\\s*\\d+)|(?:\\d+\\s*TO\\s*\\d+)|(?:[\\d-&\\s]+)).*", "$1 $2");
					lot = lot.replaceAll("(?is)\\bL(?:T|OT)(?:S)?\\b\\s*", "");
					lot = lot.replaceAll("(?is)(\\d+)\\s*\\bTO\\b\\s*(\\d+)", "$1-$2");
					lot = lot.replaceAll("(?is)\\b(?:N|S|E|W)\\b[-\\s]+(\\d+)", "$1 ");
					lot = lot.replaceAll("\\s+", " ");
				}
				
				if (tmpLegal.contains("BLK ") || tmpLegal.contains("BLOCK ")) {
					block = tmpLegal.replaceAll("(?is).*\\bBL(?:OC)?K\\b\\s*([\\d\\s-]+).*", "$1");
					block = block.replaceAll("\\s+", " ");
					block = block.trim();
				}

			}
			
			if (StringUtils.isNotEmpty(subdiv)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), Roman.normalizeRomanNumbers(subdiv));
			} 
			
			if(StringUtils.isNotEmpty(lot))  {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));	
			}
			if(StringUtils.isNotEmpty(block))  {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), Roman.normalizeRomanNumbers(block));	
			}
			if(StringUtils.isNotEmpty(tract))  {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), Roman.normalizeRomanNumbers(tract));	
			}
		}
		
		
	public static void parseTaxes(NodeList nodeList, ResultMap map, long searchId) {
		String baseAmount = "";
		String amountDue = "";
		String taxYear = "";
		String amountPaid = "";
		String receiptDate = "";
		double priorDelinq = 0;
		double totalDue = 0;
			
		TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id","taxHistoryTable"), true).elementAt(0);
		ResultTable receipts = new ResultTable();
		Map<String, String[]> tmpMap = new HashMap<String, String[]>();
		String[] header = { "ReceiptAmount", "ReceiptDate" };
		List<List<String>> bodyRT = new ArrayList<List<String>>();
			
		if (table != null) {
			TableRow[] rows = table.getRows();
			if (rows.length > 1) {
				//grab value for Total Amount Due, in case exists in the last row
				String totalAmountDue = rows[rows.length-1].getChildrenHTML().trim();
				if (StringUtils.isNotEmpty(totalAmountDue) && totalAmountDue.contains("Total Amount Due For this Account")) {
					totalAmountDue = totalAmountDue.replaceFirst("(?is)Total Amount Due For this Account:\\s*([\\$\\d.,]+)", "$1")
							.replaceAll("(?is)<th[^?]+>\\s*([^<]+)</th>", "$1")
							.replaceAll("(?is)[\\$,]+", "").trim();
					totalDue = Double.parseDouble(totalAmountDue);
				}
				
				int numberOfCols = rows[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("th"), false).size();
				for (int i = 1; i < rows.length; i++ ) {
					TableColumn[] cols = rows[i].getColumns();
					if (cols.length > 7) {
						if (numberOfCols == 8) {
							// taxes are paid; 
							if (i == 1) { // taxes for current year
								taxYear = cols[0].getChildrenHTML().trim();
								map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
								
								baseAmount = cols[4].getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
								map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
								map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),"0.00");
									
								amountPaid = cols[5].getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
								map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
								
							} 
						} else if (numberOfCols == 12) {
								// taxes are unpaid or delinquent
								if (i == 1) {
									taxYear = cols[0].getChildrenHTML().trim();
									map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
									
									baseAmount = cols[4].getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
									map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
									
									amountDue = cols[cols.length-2].getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
									map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),amountDue);
									
									if (totalDue > 0 && totalDue >= Double.parseDouble(amountDue)) {
										NumberFormat formatter = new DecimalFormat("#.##");
										double ad = Double.parseDouble(amountDue);
										priorDelinq = totalDue - ad;
										priorDelinq = Double.valueOf(priorDelinq);
										map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), formatter.format(priorDelinq));
									}
									
								} else {
									String priorDue = cols[cols.length-2].getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
									if (StringUtils.isNotEmpty(priorDue)) {
										priorDelinq +=  Double.parseDouble(priorDue);
										if ("0.0".equals(priorDue) || "0.00".equals(priorDue)) {
											NumberFormat formatter = new DecimalFormat("#.##");
											priorDelinq = Double.valueOf(priorDelinq);
											map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), formatter.format(priorDelinq));
										}
									}
								}
								
								
							}
							
						try {
							receiptDate = cols[cols.length-1].getChildrenHTML().trim();
							amountPaid = cols[5].getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
							List<String> paymentRow = new ArrayList<String>();
							if (!"0.00".equals(amountPaid)) {
								paymentRow.add(amountPaid);
								paymentRow.add(receiptDate);
								bodyRT.add(paymentRow);
							}
								
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} 
			} 

			tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
			tmpMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
			try {
				receipts.setHead(header);
				receipts.setMap(tmpMap);
				receipts.setBody(bodyRT);
				receipts.setReadOnly();
				map.put("TaxHistorySet", receipts);	
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
}	

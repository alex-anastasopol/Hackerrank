package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.name.RomanNumber;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


public class MDBaltimoreTR {

	
		public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
			ResultMap resultMap = new ResultMap();
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			TableColumn[] cols = row.getColumns();
			if(cols.length == 2) {	
				String address = cols[0].toPlainTextString().trim();
				
				if (StringUtils.isNotEmpty(address)) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				}
				
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),cols[1].toPlainTextString().trim());

			}
			return resultMap;
		}
		
		
		public static void parseNames(ResultMap resultMap, long searchId) {
			String unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
			List<List> body = new ArrayList<List>();
			
			if(StringUtils.isNotEmpty(unparsedName)) {
				unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
				unparsedName = unparsedName.replaceAll("(?is)\\(\\b\\s*DEC\\s*\\b\\)*", "");
				
				if (unparsedName.indexOf(" - ")==-1) {							//bug 6871
					//WILSON, JAMES FORSYTHE, III , ELISE P. & MICHAEL F.
					unparsedName = unparsedName.replaceAll("(?is),\\s*(JR|SR|II|III|IV)\\b", " $1");
					unparsedName = unparsedName.replaceAll("(?is),\\s*(T(?:(?:RU?)?S?)?(?:TE)?E?S?)\\b", " $1");
					unparsedName = unparsedName.replaceAll("(?is),\\s*(ET[\\s,;]*UX|ET[\\s,;]*AL|ET[\\s,;]*VIR)\\b", " $1");
					//ZWALLY, H. JAY, JO ANN M. & KURT D.
		    		//RYCHEL B SUSAN W KENT & RYCHEL E LEIGH
					unparsedName = unparsedName.replaceFirst(",", "@@@");
					unparsedName = unparsedName.replaceFirst(",", " &");
					unparsedName = unparsedName.replaceFirst("@@@", ",");
				}
				
				unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
				unparsedName = unparsedName.replaceAll("(?is)\\bInCareOfName\\b", " - @@@FML@@@");
				unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
				
				String[] mainTokens = unparsedName.split("\\s*<br>\\s*");
												
				for (int i = 0; i < mainTokens.length; i++) {
					String currentToken = mainTokens[i];
					currentToken.replaceAll("\\s*/\\s*", " & ");
					
					String[] names = StringFormats.parseNameNashville(currentToken, true);	
						
					if (currentToken.startsWith("C/O")) {  
						// after "C/O" names are in FML format, not anymore in LFM
						currentToken.replaceAll("\\bC/O\\b", "");
						names = StringFormats.parseNameDesotoRO(currentToken);
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
		
		public static void parseLegalSummary(ResultMap resultMap,long searchId) {
			
			String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
			if(StringUtils.isEmpty(legalDescription)) {
				return;
			}
			
			legalDescription = legalDescription.replaceAll("(?is)\\s*(?:IMPS|VET\\s*EX)\\s*"," ");
			legalDescription = legalDescription.replaceAll("(?is)ADJ\\s*((?:LT|LOTS|LOT|LS|LTS)\\s*[\\dA-Z,]+)","$1")
					.replaceAll("(?is)ADJ\\s*[^<]+", "");
//			Normalize.isSuffix(legalDescription);
			legalDescription = legalDescription.replaceAll("(?is)\\s*(?:[\\d.-]+)*\\s*(?:[A-Za-z-.]+\\s)*(?:CT|RD|ROAD|AVE|AV|NS|ST|LN|HWY|HW|HGWY|DRV|DR|LN|LANE)\\s*(?:[A-Z]+)*(?:[&\\s]+[A-Z])?<", "<");
			legalDescription = legalDescription.replaceAll("CUL\\s*DE\\s*SAC", ""); //this should be eliminated
			
			String[] info = legalDescription.replaceAll("(?is)<\\s*/?\\s*br\\s*/?\\s*>\\s*", "<br/>").split("<br/>");
			
			if (info.length > 0) {
				String lot = info[0];
				if (!lot.contains("LT") && !lot.contains("LS")) {
					lot = lot.replaceAll("(?is)[\\d.-]+(?:\\s*AC\\s*)?\\s*[A-Z\\s]+", "");
					lot = lot.replaceAll("(?is)[.\\d]+\\s*\\bAC\\b\\s*[A-Z\\s\\d]+\\bFT\\b(?:[\\sA-Z,.]+)?","");
					if (StringUtils.isNotEmpty(lot)) {
						//we might have Subdivision name followed by Address
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), Roman.normalizeRomanNumbers(lot));
						return;
					}
				}
				
				lot = lot.replaceAll("(?is)[.\\d]+\\s*\\bAC\\b\\s*","");
				lot = lot.replaceAll("(?is)\\bFT\\b(?:[\\sA-Z,.]+)?","");
				lot = lot.replaceAll("(?is)PR?T\\s*((?:LT|LOTS|LOT|LS|LTS)[^<]+)", "$1");
				lot = lot.replaceAll("(?is)\\s*((?:LT|LOTS|LOT|LS|LTS)[^<]+)\\s*PT\\s*([\\d,-]+)\\s*", "$1,$2");
				lot = lot.replaceAll("(?is)(?:LT|LOTS|LOT|LS|LTS)\\s*(?:[N|S]*[E|W]*)*\\s*([\\d,-]+[A-Z]?)", "$1");		
				lot = lot.replaceAll("(?is)((?:LT|LOTS|LOT|LS|LTS)\\s*)\\1", "");
//				lot = lot.replaceAll("(?is)\\s*[\\d.-]+\\s*(?:[A-Za-z-.]+\\s)*(?:RD|ROAD|AVE|AV|NS|ST|LN|HWY|HW|HGWY|DRV|DR|LN)\\s*(?:NS)?","");
				lot = lot.replaceAll("(?is)([\\d-,]+)\\s*&\\s*[A-Za-z.\\d\\s*]+", "$1");
				lot = lot.replaceAll("(?is)[A-Z\\s*]+\\d+FT\\b","");
				
				if(StringUtils.isNotEmpty(lot))  {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));	
				}
			 	
				if (info.length > 1) {
					String subdiv = info[1].trim();
//					subdiv = subdiv.replaceAll("(?is)\\s*[\\d.-]+\\s*(?:[A-Za-z-.]+\\s)*(?:RD|ROAD|AV|AVE|NS|ST|LN|HWY|HW|HGWY)\\s*(?:NS)?","");
					
					if(StringUtils.isNotEmpty(subdiv))  {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), Roman.normalizeRomanNumbers(subdiv));
					}
				}
			}
			
		}
		
		
		public static void parseTaxes(NodeList nodeList, ResultMap map, long searchId) {
			String baseAmount = "";
			String amountDue = "";
			String taxYear = "";
			
			taxYear = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Tax Year:"),"",true).trim(); 
			amountDue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Total Tax Amount:"),"",true).replaceAll("(?is)[\\$,]+", "").trim();
			baseAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Taxes/Charges"),"",true).replaceAll("(?is)[\\$,]+", "").trim();
			
			map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear.trim());
			map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),amountDue);
			
			TableTag taxInfo = (TableTag) nodeList.elementAt(0).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("table"),true) 
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "mainTable")).elementAt(0); // get table with Tax info
			
			int noTaxYears = taxInfo.getRowCount() -3;
			
			try {

				ResultTable receipts = new ResultTable();
				Map<String, String[]> tmpMap = new HashMap<String, String[]>();
				String[] header = { "ReceiptAmount", "ReceiptDate" };
				List<List<String>> bodyRT = new ArrayList<List<String>>();
				
				for (int i=1; i <= noTaxYears; i++) {
					NodeList tmpTables =  nodeList.elementAt(0).getChildren()
//							.elementAt(1).getChildren()
//							.elementAt(2).getChildren()
//							.elementAt(0).getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id","histTable" + i), false)
							.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
					
					if (tmpTables.size() > 5) {
						TableTag t = (TableTag) tmpTables.elementAt(5);
						
						if (t.getRowCount() >= 1 ) {
							for (int j=1; j < t.getRowCount(); j++) {
								TableRow row = t.getRow(j);
								TableColumn[] cols = row.getColumns();
								
								if (cols.length == 1  &&  "NO PAYMENTS RECEIVED".equals(cols[0].getChildrenHTML().toUpperCase())) { 
									// for Full Unpaid cases
									map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "0.00");	
									break;
									
								} else if (cols.length > 3 ) {
									String receiptDate = cols[2].getChildrenHTML().trim();
									String  amountPaid = cols[3].getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
									
									if (i == 1) { 
										// set AmountPaid from last tax year (from histTable1)
										map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
									}
									
									List<String> paymentRow = new ArrayList<String>();
									paymentRow.add(amountPaid);
									paymentRow.add(receiptDate);
									bodyRT.add(paymentRow);
								}
							}
						}
					}
				}	
			
				tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				tmpMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
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

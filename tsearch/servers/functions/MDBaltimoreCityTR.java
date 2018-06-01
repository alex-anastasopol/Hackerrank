package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;


public class MDBaltimoreCityTR {

	private static String clean(String str) {
		if(StringUtils.isEmpty(str)) return "";
		return str .replaceAll("<br>", "\n")
				.replaceAll("[\\$]", " ")// dash must not be cleaned all the time, in names must be: BIANCO-ABASSI,LISA ANN
				.replaceAll("\\b[\\d/]+\\s+INT(?:\\s+EA)?", "&")
				.replaceAll("\\b[W|H]\\s*&\\s*[H|W]\\b", "")
				.replaceAll("\\b[A-Z]\\s*/\\s*[A-Z]\\b", "")
				.replaceAll("(?is)\\bJTWRS\\b", "")
				.replaceAll("(?is)\\bPR\\b", "")
				.replaceAll("(?is)\\b-?POA\\b", "")
				.replaceAll("(?is)\\A\\s*OF\\s+", "")
				.replaceAll("(?is)\\bTRE\\b", "TRUSTEE")
				.replaceAll("(?is)([\\w-']+)\\s*\\bETAL\\b([^\\n]+)", "$1$2")
				.replaceAll("(?is)\\(TRUSTEE\\)", "TRUSTEE")
				.replaceAll("(?is)\\bCO[-|\\s+](TR(?:USTEE)?S?)\\b", "$1")
				.replaceAll("C/O", " & ")
				.replaceAll("ATTN", "")
				.replaceAll("\\(LIFE\\)", "LIFE ESTATE")
				.replaceFirst("(?is)\\bP/R\\s*$", "")
				.trim();
		}
	
	private static void extractNames (String s, List<List> body) {
		if (body == null) {
			body = new ArrayList<List>();
		}
		if (StringUtils.isNotEmpty(s)) {
			String[] mainTokens = s.split("\\s*<br>\\s*");
											
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				if (currentToken.matches("\\d+\\s+.*")) {
					break;
					
				} else if (currentToken.toUpperCase().matches("(?is)\\bP\\.*\\s*O\\.*\\s*BO*X\\s*\\d+")) {
					break;
					
				} else if (currentToken.toUpperCase().contains("MD")) {
					break;
				
				} else {
					String[] names = null;
					currentToken = currentToken.replaceAll("\\s*/\\s*", " & ");
					currentToken = clean(currentToken);
					if (currentToken.contains(",")) {
						currentToken = currentToken.replaceFirst(",","");
						
						if (i+ 1 < mainTokens.length) {
							if (currentToken.toUpperCase().contains(" CITY ")) {
								//e.g: MAYOR AND CITY COUNCIL OF BALTIMORE								
								if (!mainTokens[i+1].matches("\\d+\\s+.*")) {
									currentToken += " " + mainTokens[i+1];
									i++;
								}										
							}
							if (currentToken.toUpperCase().contains(" CENTER ") && 
									(mainTokens[i+1].toUpperCase().matches("(?is)\\s*INC(?:ORPORATED)"))) {
								currentToken += " " + mainTokens[i+1];
								i++;
							}
							
							if ("LLC".equals(mainTokens[i+1].toUpperCase())) {
								currentToken +=  " " + mainTokens[i+1];
								i++;
							}							
							if (currentToken.toUpperCase().matches("(?is).*,\\s*INC\\s*")) {
								if (("THE".equals(mainTokens[i+1].toUpperCase()))) {
									currentToken = mainTokens[i+1] + " " + currentToken;
									i++;
								}
							} 
							if (mainTokens[i+1].matches("(?is)\\s*\\bAND\\b[\\w\\s\\d-']+,\\s*INC")) {
								currentToken += " " + mainTokens[i+1];
								i++;
							}
							if (currentToken.toUpperCase().contains(" APOSTOLIC ")) {
								if (mainTokens[i+1].toUpperCase().contains("CHURCH")) {
									currentToken += " " + mainTokens[i+1];
									i++;
								}
							}														
						}						
					names = StringFormats.parseNameNashville(currentToken, true);	
					
					} else {
						if (currentToken.matches("(?is)([\\w-\\s]+)&\\s*WF")) {
							names = StringFormats.parseNameDesotoRO(currentToken.replaceFirst("(?is)([\\w-\\s]+)&\\s*WF", "$1"), true);
						} else {
							names = StringFormats.parseNameDesotoRO(currentToken, true);
						}		
					}
					
					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
						
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				}																				
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNamesIntermediary(ResultMap resultMap, long searchId) throws Exception {
		   List<List> body = new ArrayList<List>();
		   String owners = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		   
		   if (StringUtils.isEmpty(owners))
			   return;
		   	
		   extractNames(owners, body);
		   try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		     
	   }
	
		public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
			ResultMap resultMap = new ResultMap();
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			TableColumn[] cols = row.getColumns();
			if(cols.length == 5) {					
				String blk = cols[0].toPlainTextString().trim();
				String lot = cols[1].toPlainTextString().trim();
				String address = cols[2].toPlainTextString().trim();
				String owners = cols[3].toHtml().replaceAll("(?is)<td>(.*)</td>", "$1");
				owners = owners.replaceAll("(?is)<span[^>]+>([^<]+)</span>", "$1");
				
				if (StringUtils.isNotEmpty(address)) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				}
				
				if (StringUtils.isNotEmpty(blk)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
				}
				
				if (StringUtils.isNotEmpty(lot)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
				}
								
				if (StringUtils.isNotEmpty(owners)) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owners);
					try {
						parseNamesIntermediary (resultMap, searchId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
			return resultMap;
		}
		
		
		public static String getTaxYear(String rsResponse) {
			String year = "";
			HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
			NodeList tmp = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_LabelStartFY"), true);
			if (tmp != null) {
				year = ((Span) tmp.elementAt(0)).getChildrenHTML();
			}
			
			return year;
		}
		
		public static void parseTaxInfo (NodeList list, ResultMap resultMap, long searchId) {
			TableTag table;
			TableRow row;
			TableColumn col;
			Span span;
			String baseAmount = ""; 
			double[] amountDue = new double[3]; 
			double priorDelinq = 0;
			
			ResultTable receipts = new ResultTable();
			Map<String, String[]> tmpMap = new HashMap<String, String[]>();
			String[] header = { "ReceiptAmount", "ReceiptDate" };
			List<List<String>> bodyRT = new ArrayList<List<String>>();
			
			try {
				for (int i = 3; i > 0; i--) {
					table =  (TableTag) list.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "tblDetail" + i))
							.elementAt(0);
					if (table != null) {
						if (i == 3) { // save current Tax Year, BA, AD, AP into resultMap
							//get taxYear
							if (table.getRowCount() > 5) {
								row = table.getRow(1);   					
								if (row.getColumnCount() > 0) {						
									col = row.getColumns()[0];
									span = (Span) col.getChildren().extractAllNodesThatMatch
											(new HasAttributeFilter("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_LabelStartFY"), true)
											.elementAt(0);   
								
									if (span != null) {
										String taxYear = span.getChildrenHTML().trim();
										if (StringUtils.isNotEmpty(taxYear)) {
											resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
										}
									}						
								}
								
								//get BA
								row = table.getRow(5);
								if (row.getColumnCount() > 1) {
									col = row.getColumns()[0]; 
									NodeList lst = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
											.extractAllNodesThatMatch(new HasAttributeFilter
											("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_Table1"), true);
									//extract BA
									span = (Span) lst.extractAllNodesThatMatch(new TagNameFilter("span"), true)
												.extractAllNodesThatMatch(new HasAttributeFilter
												("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_TotalTax"), true)	.elementAt(0);						
									if (span != null) {
										baseAmount = span.getChildrenHTML().replaceAll("(?is)[\\$,]+", "").trim();
										if (StringUtils.isNotEmpty(baseAmount)) { 							
											resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
										} else {
											resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), "0.00");
										}
									}
								}
							}
						}
						//get AP
						row = table.getRow(5);						
						if (row.getColumnCount() > 1) {
							col = row.getColumns()[0]; 
							NodeList lst = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter
									("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_Table1"), true);
							NodeList tmpSpanList = lst.extractAllNodesThatMatch(new TagNameFilter("span"), true);
							
							if (tmpSpanList != null) {
								int dim = tmpSpanList.size();
								
								if (dim > 16) {
									NodeList tmpList = HtmlParser3.getNodesBetween(tmpSpanList, 
											tmpSpanList.elementAt(14), tmpSpanList.elementAt(dim-2));
									
									if (tmpList != null) {
										String txt = tmpList.toHtml();
										
										if (StringUtils.isNotEmpty(txt)) {
											if (txt.contains("PAID ")) {
												Pattern p = Pattern.compile("(?is).*PAID\\s*(\\d{2}/\\d{2}/\\d{2})[^\"]+\"[^\"]+\">([^<]+)</span>");
												Matcher m = p.matcher(txt);
												
												if (m.find()) {
													String receiptDate = m.group(1);
													String amountPaid = m.group(2).replaceAll("[-,]", "");
													if (StringUtils.isNotEmpty(amountPaid)){
														if (i == 3) {
															resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
															if (StringUtils.isNotEmpty(receiptDate)) {
																resultMap.put(TaxHistorySetKey.RECEIPT_DATE.getKeyName(), receiptDate);
															}
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
								}
							}
						}
						// parsing AD values
						row =  (TableRow) list.extractAllNodesThatMatch(new TagNameFilter("tr"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "amountDue" + i))
								.elementAt(0);
						if (row != null) {
							if (row.getColumnCount() > 0) {
								String val = row.getColumns()[0].getChildrenHTML();
								val = val.replaceAll("(?is)Amount\\s*Due:([^;]+;)\\1*\\s*([^\\n]+)", "$2").trim();
								val = val.replaceAll("[$,]", "");
								amountDue[3-i] = Double.parseDouble(val);
								
								if (i == 3) {
									resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(amountDue[0]));
								
								} else {
									if (amountDue[3-i] != 0) {
										priorDelinq += amountDue[3-i];
									}
								}									
							}
						}						
					}
				}					
				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), Double.toString(priorDelinq));
				
				tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				tmpMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				receipts.setHead(header);
				receipts.setMap(tmpMap);
				receipts.setBody(bodyRT);
				receipts.setReadOnly();
				resultMap.put("TaxHistorySet", receipts);
				
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}
		
		@SuppressWarnings("rawtypes")
		public static void parseDetails(NodeList list, ResultMap resultMap, long searchId) {
			String unparsedNames = "";
			TableRow row = null;
			TableColumn col = null;
			String address = "";

			List<List> body = new ArrayList<List>();
			
			TableTag table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","tblDetail3"), true).elementAt(0);
			
			if (table != null) {				
				if (table.getRowCount() > 5) {
					//parse Owner information
					row = table.getRow(2);					
					if (row != null && row.getColumnCount() > 0) {						
						col = row.getColumns()[0];
						unparsedNames = col.getChildrenHTML().replaceAll("(?is)<span[^>]+>([^<]+)</span>", "$1");
						unparsedNames = unparsedNames.replaceAll("(?is)([^<]+)</*\\s*br\\s*/*>", "$1 <br>");
						
						extractNames(unparsedNames, body);
						
						try {
							GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
							
						} catch (Exception e) {
							e.printStackTrace();
						}
						//extract Address - from span with id=ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_PropertyAddr
						row = table.getRow(4);					
						if (row != null && row.getColumnCount() > 0) {	
							col = row.getColumns()[0];
							address = col.getChildrenHTML()
									.replaceAll("(?is)<span[^>]+>([^<]+)</span>", "$1").trim();
							
							if (StringUtils.isNotEmpty(address)) {
								resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
								resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
								resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
							}
						}
					}					
				}
			}
			
			parseTaxInfo(list, resultMap, searchId);
		}	
	
}

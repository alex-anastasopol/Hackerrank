package ro.cst.tsearch.servers.functions;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.name.RomanNumber;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class COEagleTR {
	
	private static final Pattern LEGAL_CROSS_REF_INSTRUMENT_PATTERN = 
		Pattern.compile("(?:\\s|^)(R?\\d+)\\s+([A-Z]+)\\s+(\\d\\d-\\d\\d-\\d\\d)(?:\\s|$)");
	private static final Pattern LEGAL_CROSS_REF_BOOK_PAGE_PATTERN = 
		Pattern.compile("BK-(\\d+)\\s+PG-(\\d+)\\s+([A-Z]*)\\s*((?:\\d\\d-\\d\\d-\\d\\d)?)(?:\\s|$)");
	private static final Pattern LEGAL_PHASE_PATTERN = 
		Pattern.compile("\\bPHASE\\s+([^\\s]+)");
	private static final Pattern LEGAL_TRACT_PATTERN = 
		Pattern.compile("\\bTRACT\\s+([^\\s]+)");
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 2) {
			resultMap.put("PropertyIdentificationSet.ParcelID",cols[0].toPlainTextString().trim());
			
			NodeList internalColumns = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("td"), true);
			if(internalColumns.size() >= 4) {
				resultMap.put("PropertyIdentificationSet.ParcelIDParcel",
						internalColumns.elementAt(0).toPlainTextString().trim().replaceAll("-", ""));
				String nameOnServer = null;
				if(internalColumns.elementAt(1).getChildren().size() > 1) {
					nameOnServer = internalColumns.elementAt(1).getChildren().elementAt(1).toHtml();
				} else {
					nameOnServer = internalColumns.elementAt(1).toPlainTextString().trim();
				}
				nameOnServer = nameOnServer.replace("&amp;", "&");
				resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);
				String fullAddress = ((TableColumn)internalColumns.elementAt(2)).getChildrenHTML();
				String[] addressParts = fullAddress.split("<br>");
				String[] address = StringFormats.parseAddress(addressParts[0].trim());
				resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
				resultMap.put("PropertyIdentificationSet.StreetName", address[1]);
				
			}

			parseNames(resultMap, searchId);

		}
		resultMap.removeTempDef();
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		List<List> body = new ArrayList<List>();
		if(StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
			
			unparsedName = unparsedName.replaceAll("(?is)\\bJO ANN\\b", "JOFAKEANN");
			unparsedName = unparsedName.replaceAll("(?is)\\.-\\s", ". - ");
			
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
			
			String[] mainTokens = unparsedName.split("\\s+-\\s+");
			
			boolean is_csc = false;//comma separated companies 
			
			// bug 7053
			if (unparsedName.split(",").length > 1) {
				if (unparsedName.split(",")[0].split(" ").length >= 4
						&& unparsedName.split(",")[1].split(" ").length >= 4) {
					mainTokens = unparsedName.split(",");
					is_csc = true;
				}
			}

			boolean onlyCompanies = true;
			for (int i=0;i<mainTokens.length && onlyCompanies;i++)
				onlyCompanies = onlyCompanies && NameUtils.isCompany(mainTokens[i]);
			if (onlyCompanies && unparsedName.contains("&")) {
					mainTokens = new String[] { unparsedName };
			}
			
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				if(is_csc){
					String[] names = StringFormats.parseNameFML(currentToken, new Vector<String>(), false);
					
					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					
				} else if(!currentToken.contains("&") && !currentToken.contains(".,") || NameUtils.isCompany(currentToken)) {	//single name
					
					String[] names;
					if (currentToken.trim().startsWith("@@@FML@@@")) {
						currentToken = currentToken.replaceAll("@@@FML@@@", "").trim();
						names = StringFormats.parseNameDesotoRO(currentToken, true);
					} else
						names = StringFormats.parseNameNashville(currentToken, true);
					
					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					
				} else {
					String[] possibleNames = currentToken.split("&|\\.,");
					String firstPossibleLastName = null;
					for (int j = 0; j < possibleNames.length; j++) {
						int len = possibleNames.length;
						if (j<len-1 && possibleNames[len-1].trim().toLowerCase().endsWith("trustees"))
							currentToken = possibleNames[j].trim()+ " TR";
						else 
							currentToken = possibleNames[j].trim();
						if(j == 0) {
							
							if( j+1 < possibleNames.length) {
								if(GenericFunctions.nameSuffix3.matcher(possibleNames[j+1].trim()).matches()) {
									currentToken += " " + possibleNames[j + 1].trim();
									j++;
								}
							}
							
							String[] names = StringFormats.parseNameNashville(currentToken, true);

							String[] types = GenericFunctions.extractAllNamesType(names);
							String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
							String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
							
							GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
									NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
							firstPossibleLastName = names[2];
						} else {
							
							if( j+1 < possibleNames.length) {
								if(GenericFunctions.nameSuffix3.matcher(possibleNames[j].trim()).matches()) {
									currentToken += possibleNames[j].trim();
									j++;
								}
							}
							
							if(!currentToken.startsWith(firstPossibleLastName)) {
								if(currentToken.matches("\\w\\.\\s+\\w+") 
										&& !currentToken.matches("(?is)W\\. KENT") && !currentToken.matches("(?is)E\\. LEIGH")) { //B 6871
									currentToken = currentToken.replaceAll("(\\w\\.)\\s+(\\w+)", "$2 $1");
								} 
								if (currentToken.indexOf(",")==-1)
									currentToken = firstPossibleLastName + ", " + currentToken; 
								
							}
							
							String[] names = StringFormats.parseNameNashville(currentToken, true);

							String[] types = GenericFunctions.extractAllNamesType(names);
							String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
							String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
							
							names[0] = names[0].replaceAll("(?is)\\bJOFAKEANN\\b", "JO ANN");
							
							GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
									NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
						}
					}
				}
			}
			
			try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap, List<List> body,
			long searchId) {
		String legalDescription = (String) resultMap.get("PropertyIdentificationSet.PropertyDescription");
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}

		boolean isCondo = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isCondo();
		
		legalDescription += " ";	//we need this to be sure all regex match :)
		String legalDescriptionFake = "FAKE " + legalDescription.toUpperCase().replaceAll(":", " ");
		
		legalDescriptionFake = GenericFunctions.replaceNumbers(legalDescriptionFake);
		//String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		//legalDescriptionFake = Roman.normalizeRomanNumbersExceptTokens("FAKE SUBDIVISION  ASPEN MESA ESTATES UNIT I LOT  22 BK-0218 PG-0891", exceptionTokens); // convert roman numbers
		
		
		if(body == null) {
			body = new ArrayList<List>();
		}
		List<String> line = null;
		Matcher matcher = LEGAL_CROSS_REF_INSTRUMENT_PATTERN.matcher(legalDescription);
		while (matcher.find()) {
			line = new ArrayList<String>();
			line.add("");
			line.add(matcher.group(3));
			line.add(matcher.group(1).replaceFirst("(?i)^[A-Z]", ""));
			line.add("");
			line.add("");
			line.add(matcher.group(2));
			body.add(line);
			legalDescription = legalDescription.replace(matcher.group(), " ");
			matcher.reset(legalDescription);
		}
		matcher = Pattern.compile("BK-(\\d+)\\s+PG-(\\d+)\\s+([A-Z]*)\\s*((?:\\d\\d-\\d\\d-\\d\\d)?)(?:\\s|$)").matcher(legalDescription);
		matcher = LEGAL_CROSS_REF_BOOK_PAGE_PATTERN.matcher(legalDescription);
		while (matcher.find()) {
			line = new ArrayList<String>();
			line.add("");
			line.add(matcher.group(4));
			line.add("");
			line.add(matcher.group(1).replaceAll("^0+", ""));
			line.add(matcher.group(2).replaceAll("^0+", ""));
			line.add(matcher.group(3));
			body.add(line);
			legalDescription = legalDescription.replace(matcher.group(), " ");
			matcher.reset(legalDescription);
		}
		
		if (legalDescription.contains("Subdivision:")){
			String[] tokens = {" Unit:", " Block:", " Lot:"};
			String subdName = LegalDescription.extractSubdivisionNameUntilToken(legalDescription, tokens);
		   
			if(StringUtils.isNotEmpty(subdName)) {
				int comaPos = subdName.indexOf(',');
				   
				if(comaPos>0){
					subdName = subdName.substring(0,comaPos);
				}
				subdName = subdName.replaceAll("(?is).*?Subdivision\\s*:", "");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), Roman.normalizeRomanNumbers(subdName.trim()));
			}
		}
		
		String lot = LegalDescription.extractLotFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(lot)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));
		}
		
		String block = LegalDescription.extractBlockFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(block)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), Roman.normalizeRomanNumbers(block));
		}
		
		if (isCondo){
			String unit = LegalDescription.extractUnitFromText(legalDescriptionFake);
			if(StringUtils.isNotEmpty(unit)) {
				if (lot.trim().matches("\\d+") && unit.trim().matches("[A-Z]")){//R009676   
					lot += unit.trim();
				} else {
					lot += " " + unit;
				}
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot.trim()));
			}
			
			String bldg = LegalDescription.extractBuildingFromText(legalDescriptionFake);
			if(StringUtils.isNotEmpty(bldg)) {
				block += " " + bldg;
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), Roman.normalizeRomanNumbers(block.trim()));
			}
		} else {
			
			String unit = LegalDescription.extractUnitFromText(legalDescriptionFake);
			if(StringUtils.isNotEmpty(unit)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), Roman.normalizeRomanNumbers(unit));
			}
			
			String bldg = LegalDescription.extractBuildingFromText(legalDescriptionFake);
			if(StringUtils.isNotEmpty(bldg)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);//Roman.normalizeRomanNumbers(bldg));
			}
		}
		
		matcher = LEGAL_PHASE_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			try {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), Integer.toString(RomanNumber.parse(matcher.group(1))));
				legalDescription = legalDescription.replace(matcher.group(), " ");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		matcher = LEGAL_TRACT_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), matcher.group(1));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
	}
	
	
	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		NodeList tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "currentTaxes"), true);
		double priorDelinquent = 0;
		boolean isCurrentTaxYear = false;
		
		if (tableList != null) {
			TableTag tmpTable = (TableTag) tableList.elementAt(0);
			if (tmpTable.getRowCount() > 2) {
				TableRow row = tmpTable.getRow(2);
				if (row.getColumnCount() >= 6) {
					String taxYear = row.getColumns()[0].getChildrenHTML().trim();
					String baseAmt = row.getColumns()[2].getChildrenHTML().trim().replaceAll("[$,]", "");
					String amtPaid = "";
					String amtDue =	"";
					if (row.getColumnCount() == 7) {
						amtPaid = row.getColumns()[4].getChildrenHTML().trim().replaceAll("[$,]", "");
						amtDue = row.getColumns()[6].getChildrenHTML().trim().replaceAll("[$,]", "");
					
					} else {
						amtDue = row.getColumns()[5].getChildrenHTML().trim().replaceAll("[$,]", "");
					}
					
					
					if (StringUtils.isNotEmpty(taxYear)) {
						isCurrentTaxYear = true;
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
					}
					if (StringUtils.isNotEmpty(baseAmt)) {
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmt);
					}
					if (StringUtils.isNotEmpty(amtPaid)) {
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);
					}
					if (StringUtils.isNotEmpty(amtDue)) {
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amtDue);
					}
				}
			}
		}
		
		tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "paymentTaxHistory"), true);
		if (tableList != null) {
			TableTag tmpTable = (TableTag) tableList.elementAt(0);
			try {
				ResultTable receipts = new ResultTable();
				Map<String, String[]> map = new HashMap<String, String[]>();
				String[] header = { "ReceiptAmount", "ReceiptDate" };
				List<List<String>> bodyRT = new ArrayList<List<String>>();
				NumberFormat formatter = new DecimalFormat("#.##");	
				
				if (tmpTable.getRowCount() > 2) {
					for (int i = 2; i < tmpTable.getRowCount(); i++) {
						TableRow row = tmpTable.getRow(i);
						if (row.getColumnCount() == 6) {
							TableColumn col = row.getColumns()[1];
							String status = col.getChildrenHTML().trim().toLowerCase();
							col = row.getColumns()[2];
							String baseAmt = col.getChildrenHTML().trim().replaceAll("[$,]", "");
							col = row.getColumns()[5];
							String amtPaid = col.getChildrenHTML().trim().replaceAll("[$,]", ""); 
							
							if ("delinquent".equals(status) && !baseAmt.equals(amtPaid) && !isCurrentTaxYear) {
								try {
									priorDelinquent += Double.parseDouble(baseAmt) - Double.parseDouble(amtPaid);
								} catch (Exception e) {
									e.printStackTrace();
								}
								
							}
							List<String> paymentRow = new ArrayList<String>();
							paymentRow.add(amtPaid);
							paymentRow.add("");
							bodyRT.add(paymentRow);
						}
						
					}
					
					map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
					map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
					receipts.setHead(header);
					receipts.setMap(map);
					receipts.setBody(bodyRT);
					receipts.setReadOnly();
					resultMap.put("TaxHistorySet", receipts);
					
					if (priorDelinquent != 0) {
						resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), formatter.format(priorDelinquent));
					}
				}
				
			} catch (Exception e) {
					e.printStackTrace();
			}
		}
	}
	
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.name.RomanNumber;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class COGarfieldAO {
	private static final Pattern LEGAL_CROSS_REF_INSTRUMENT_PATTERN = 
		Pattern.compile("(?:\\s|^)(R?\\d+)\\s+([A-Z]+)\\s+(\\d\\d-\\d\\d-\\d\\d)(?:\\s|$)");
	private static final Pattern LEGAL_CROSS_REF_BOOK_PAGE_PATTERN = 
		Pattern.compile("BK-(\\d+)\\s+PG-(\\d+)\\s+([A-Z]*)\\s*((?:\\d\\d-\\d\\d-\\d\\d)?)(?:\\s|$)");
	private static final Pattern LEGAL_PHASE_PATTERN = 
		Pattern.compile("\\bPHASE\\s+([^\\s]+)");
	private static final Pattern LEGAL_TRACT_PATTERN = 
		Pattern.compile("\\bTR(?:ACT)?\\s+([^\\s]+)");
	private static final Pattern LEGAL_SECTION_PATTERN = 
		Pattern.compile("\\bSection:\\s+([^\\s]+)");
	private static final Pattern LEGAL_TOWNSHIP_PATTERN = 
		Pattern.compile("\\bTownship:\\s+([^\\s]+)");
	private static final Pattern LEGAL_RANGE_PATTERN = 
		Pattern.compile("\\bRange:\\s+([^\\s]+)");
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");
		
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
				String[] addressParts = fullAddress.split("<br>|,");
				String address = addressParts[0].replaceAll("^0+", "").replaceAll("\\s+"," ");
				if(address.matches("(\\d+) (\\d+) ([^\\s]+) ([^\\s]*)")){
					address = address.replaceAll("(\\d+) (\\d+) ([^\\s]+) ([^\\s]*)","$1 $3 $4 $2");
				}
				//String[] address = StringFormats.parseAddressShelbyAO(addressParts[0].trim());
				resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
				resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
				
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
			String[] mainTokens = unparsedName.split("\\s+-\\s+");
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				if(!currentToken.contains("&") && !currentToken.contains(".,")) {	//single name
					String[] names = StringFormats.parseNameNashville(currentToken);

					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					
				} else {
					if(NameUtils.isCompany(currentToken)) {
						String[] names = StringFormats.parseNameNashville(currentToken);

						String[] types = GenericFunctions.extractAllNamesType(names);
						String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
						String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
						
						GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					} else {
						Matcher matcher = Pattern.compile("&[\\sa-zA-Z]+,").matcher(currentToken);
						if(matcher.find()) {
							List<String> namesToParse = new ArrayList<String>();
							int lastIndexOfStart = 0;
							do {
								int pos = matcher.start();
								namesToParse.add(currentToken.substring(lastIndexOfStart, pos));
								lastIndexOfStart = pos + 1;
							} while (matcher.find());
							namesToParse.add(currentToken.substring(lastIndexOfStart));
							for (String currentName : namesToParse) {
								String[] names = StringFormats.parseNameNashville(currentName);

								String[] types = GenericFunctions.extractAllNamesType(names);
								String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
								String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
								
								GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
										NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
							}
							
						} else {
							String[] possibleNames = currentToken.split("&|\\.,");
							String firstPossibleLastName = null;
							for (int j = 0; j < possibleNames.length; j++) {
								currentToken = possibleNames[j].trim();
								if(j == 0) {
									
									if( j+1 < possibleNames.length) {
										if(GenericFunctions.nameSuffix3.matcher(possibleNames[j+1].trim()).matches()) {
											currentToken += " " + possibleNames[j + 1].trim();
											j++;
										}
									}
									
									String[] names = StringFormats.parseNameNashville(currentToken);
									
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
										if(currentToken.matches("\\w\\.\\s+\\w+")) {
											currentToken = currentToken.replaceAll("(\\w\\.)\\s+(\\w+)", "$2 $1");
										} 
										currentToken = firstPossibleLastName + ", " + currentToken; 
										
									}
									String[] names = StringFormats.parseNameNashville(currentToken);
									
									String[] types = GenericFunctions.extractAllNamesType(names);
									String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
									String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
									
									GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
								}
							}
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
			line.add(matcher.group(1));
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
		
		String lot = LegalDescription.extractLotFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(lot)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", Roman.normalizeRomanNumbers(lot));
		}
		String unit = LegalDescription.extractUnitFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(unit)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit", Roman.normalizeRomanNumbers(unit));
		}
		String block = LegalDescription.extractBlockFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(block)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		matcher = LEGAL_PHASE_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			try {
				resultMap.put("PropertyIdentificationSet.SubdivisionPhase", Integer.toString(RomanNumber.parse(matcher.group(1))));
				legalDescription = legalDescription.replace(matcher.group(), " ");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		matcher = LEGAL_TRACT_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			resultMap.put("PropertyIdentificationSet.SubdivisionTract", matcher.group(1));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
		matcher = LEGAL_SECTION_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", matcher.group(1));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
		matcher = LEGAL_TOWNSHIP_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship", matcher.group(1));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
		matcher = LEGAL_RANGE_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", matcher.group(1));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
		
	}
	
}

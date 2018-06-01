package ro.cst.tsearch.servers.functions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.types.GenericSKLD;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;

public class GenericSKLDFunctions {
	
	
	public static final Pattern FIRST_LINE_PATTERN = Pattern
			.compile("\\{(?:([A-Z\\d]*)/([A-Z\\d]*))?\\s+([/\\d]+)\\s+(\\$\\s+[\\.,\\d]+\\s+)?([A-Z/]+)\\s+([-/\\dA-Z]+)(?:\\s+(\\d*))?");
	public static final Pattern FIRST_LINE_STR_PATTERN = Pattern
			.compile("\\{\\w{2}\\s+([/\\d]+)\\s+STR\\s+([^\\r\\n]*)");
	public static final Pattern REMARKS_LINE_PATTERN = Pattern
			.compile("\\{\\s+Remarks:([^\\r\\n]+)");
	public static final Pattern PARTY_LINE_PATTERN = Pattern
			.compile("\\{\\s+Party\\s(\\d):\\s*([^\\r\\n+]*)(?:Adr)?");
	public static final Pattern PARTY_NEXT_LINE_PATTERN = Pattern
			.compile("\\{\\s{11}(\\S[^\\r\\n+]*)(?:Adr)?");
	public static final Pattern FULL_ADDRESS_PATTERN = Pattern
			.compile("\\{\\s{13}(.{31})(.{14})(\\d*)(?:Adr)?");
	public static final Pattern SECOND_ADDRESS_PATTERN = Pattern
			.compile("\\{\\s{11}(.{31})(.{14})(\\d*)\\s+Adr");
	public static final Pattern CROSS_REF_TO_PATTERN = Pattern
			.compile("\\{\\s*Ref To:\\s\\s([^\\r\\n]{18})([^\\r\\n]{16})([^\\r\\n]{11})([^\\r\\n]*)");
	public static final Pattern CROSS_REF_BY_PATTERN = Pattern
			.compile("\\{\\s*Ref By:\\s\\s([^\\r\\n]{18})([^\\r\\n]{16})([^\\r\\n]{11})([^\\r\\n]*)");
	public static final Pattern LEGAL_ARB_PATTERN = Pattern
			.compile("\\{\\s*ARB\\s:\\s{4}([^\\r\\n]{19})T/R/S\\s:\\s{4}([^\\r\\n+]*)(?:\\+)?");
	public static final Pattern LEGAL_LOT_BLOCK_PATTERN = Pattern
			.compile("\\{\\s*B/L\\s+:\\s{2}([^\\r\\n]{19})MAPID\\s*:\\s{5}([^\\r\\n]*)");
	public static final Pattern LEGAL_SUBDIVISION_NAME_PATTERN = Pattern
			.compile("\\{\\s*LAND NAME:\\s([^\\r\\n+]+)");
	
	public static final int LINE_TYPE_UNKNOWN = 0;
	public static final int LINE_TYPE_PARTY_1 = 1;
	public static final int LINE_TYPE_PARTY_2 = 2;
	public static final int LINE_TYPE_PARTY_NEXT = 3;
	public static final int LINE_TYPE_CROSS_REF_BY = 4;
	public static final int LINE_TYPE_CROSS_REF_TO = 5;
	public static final int LINE_TYPE_FULL_ADDRESS = 6;
	public static final int LINE_TYPE_SECOND_ADDRESS = 7;
	public static final int LINE_TYPE_REMARKS = 8;
	
	public static final Pattern SIMPLE_BLOCK_LOT_PATTERN = Pattern.compile("([A-Z\\d]*)/([A-Z\\d]*)");
	private static final Pattern SIMPLE_INSTRUMENT_PATTERN = Pattern.compile("(\\d*)-([*\\d]*)");
	private static final Pattern SSN4_PATTERN = Pattern.compile("(?is)\\s*x*-x*-(\\d*)\\s*");
	private static final Pattern SIMPLE_MAPID_PATTERN = Pattern.compile("(\\w*)/(\\w*)");
	private static final Pattern SIMPLE_TOWNSHIP_RANGE_SECTION_PATTERN = Pattern.compile("(\\w*)/(\\w*)/?(\\w*)");
	private static final Pattern STARTER_AGENCY_PATTERN = Pattern.compile("^([A-Z]{2})\\s+.+");
	
	public static void parseBookPageInstrumentLine(Matcher matcher, ResultMap resultMap) {
		if(matcher != null) {
			matcher.reset();
		} else {
			return;
		}
		if(matcher.find()) {

			String book = matcher.group(1);
			String page = matcher.group(2);
			
			if( StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page) ){
				resultMap.put("SaleDataSet.Book", book);
				resultMap.put("SaleDataSet.Page", page);
			}
			
			String recordedDate = matcher.group(3);
			if(StringUtils.isNotEmpty(recordedDate)) {
				resultMap.put("SaleDataSet.RecordedDate", recordedDate);
			}
			
			String amount = matcher.group(4);
			if(StringUtils.isNotEmpty(amount)) {
				resultMap.put("SaleDataSet.MortgageAmount", amount.replaceAll("[\\$,\\s]*", ""));
				resultMap.put("SaleDataSet.ConsiderationAmount", amount.replaceAll("[\\$,\\s]*", ""));
			}
			
			String serverDocType = matcher.group(5);
			if(StringUtils.isNotEmpty(serverDocType)) {
				resultMap.put("SaleDataSet.DocumentType", serverDocType);
			}
			
			String instrumentNo = matcher.group(6).trim();
			if(StringUtils.isNotEmpty(instrumentNo) && !instrumentNo.startsWith("/")) {
				resultMap.put("SaleDataSet.InstrumentNumber", instrumentNo.trim());
				String fullLine = matcher.group();
				if(fullLine.startsWith("{")) {
					fullLine = fullLine.substring(1);
				}
				Matcher newMatcher = STARTER_AGENCY_PATTERN.matcher(fullLine);
				if(newMatcher.find()) {
					resultMap.put("tmpAgencyNo", newMatcher.group(1));
				}
			}
			
			resultMap.put("OtherInformationSet.SrcType", "SK");
		}
		
	}
	
	
	public static void parseSTRInstrumentLine(Matcher matcher,
			ResultMap resultMap) {
		if(matcher != null) {
			matcher.reset();
		} else {
			return;
		}
		if(matcher.find()) {

			String recordedDate = matcher.group(1);
			if(StringUtils.isNotEmpty(recordedDate)) {
				resultMap.put("SaleDataSet.RecordedDate", recordedDate);
			}
			
			String instrumentNo = matcher.group(2);
			if(StringUtils.isNotEmpty(instrumentNo)) {
				resultMap.put("SaleDataSet.InstrumentNumber", instrumentNo.replaceAll("/", "").trim().replaceAll("\\s", "_"));
			}
			resultMap.put("SaleDataSet.DocumentType", "STR");
			resultMap.put("OtherInformationSet.SrcType", "SK");
			
			String fullLine = matcher.group();
			if(fullLine.startsWith("{")) {
				fullLine = fullLine.substring(1);
			}
			Matcher newMatcher = STARTER_AGENCY_PATTERN.matcher(fullLine);
			if(newMatcher.find()) {
				resultMap.put("tmpAgencyNo", newMatcher.group(1));
			}
			
		}
	}
	

	public static void parseLegalLotBlock(Matcher matcher, PropertyIdentificationSet pis) {
		if(matcher != null && matcher.find()) {
			String blockLot = matcher.group(1).trim();
			Matcher simpleMatcher = SIMPLE_BLOCK_LOT_PATTERN.matcher(blockLot);
			if(simpleMatcher.find()) {
				if(StringUtils.isNotEmpty(simpleMatcher.group(1))){
					pis.setAtribute("SubdivisionBlock", simpleMatcher.group(1));
				}
				if(StringUtils.isNotEmpty(simpleMatcher.group(2))){
					pis.setAtribute("SubdivisionLotNumber", simpleMatcher.group(2));
				}
			}
			
			String mapId = matcher.group(2).trim();
			simpleMatcher = SIMPLE_MAPID_PATTERN.matcher(mapId);
			if(simpleMatcher.find()) {
				if(StringUtils.isNotEmpty(simpleMatcher.group(1))){
					pis.setAtribute("PlatBook", simpleMatcher.group(1));
				}
				if(StringUtils.isNotEmpty(simpleMatcher.group(2))){
					pis.setAtribute("PlatNo", simpleMatcher.group(2));
				}
			}
		}
	}

	public static void parseArbLine(Matcher matcher, PropertyIdentificationSet pis) {
		if(matcher != null && matcher.find()) {
			String fragment = matcher.group(1).trim();
			if(StringUtils.isNotEmpty(fragment)) {
				pis.setAtribute("ARB", fragment.trim());
			}
			fragment = matcher.group(1).trim();
			if(StringUtils.isNotEmpty(fragment)) {
				fragment = matcher.group(2).trim();
				Matcher simpleMatcher = SIMPLE_TOWNSHIP_RANGE_SECTION_PATTERN
					.matcher(fragment);
				if(simpleMatcher.find()) {
					if(StringUtils.isNotEmpty(simpleMatcher.group(1))){
						String value = simpleMatcher.group(1).trim();
						if(value.endsWith("S") || value.endsWith("N")) {
							pis.setAtribute("SubdivisionTownship", value);
						} else if(value.endsWith("W")) {
							pis.setAtribute("SubdivisionRange", value);
						} else {
							pis.setAtribute("SubdivisionSection", value);
						}
					}
					if(StringUtils.isNotEmpty(simpleMatcher.group(2))){
						String value = simpleMatcher.group(2).trim();
						if(value.endsWith("W")) {
							pis.setAtribute("SubdivisionRange", value);
						} else if(value.endsWith("S") || value.endsWith("N")) {
							pis.setAtribute("SubdivisionTownship", value);
						} else {
							pis.setAtribute("SubdivisionSection", value);
						}
					}
					if(StringUtils.isNotEmpty(simpleMatcher.group(3))){
						String value = simpleMatcher.group(3).trim();
						if(value.endsWith("S")) {
							pis.setAtribute("SubdivisionTownship", value);
						} else if(value.endsWith("W")) {
							pis.setAtribute("SubdivisionRange", value);
						} else {
							pis.setAtribute("SubdivisionSection", value);
						}
					}
				}
			}
		}
		
	}

	@SuppressWarnings("rawtypes")
	public static void parseCrossRef(Matcher matcher, ResultMap resultMap, 
			ro.cst.tsearch.servers.types.GenericSKLD genericSKLDparser) {
		if(matcher != null && matcher.find()) {
			String fragment = matcher.group(1).trim();
			Matcher simpleMatcher = SIMPLE_BLOCK_LOT_PATTERN.matcher(fragment);
			String book = "";
			String page = "";
			if(simpleMatcher.find()) {
				if(StringUtils.isNotEmpty(simpleMatcher.group(1))){
					book = simpleMatcher.group(1);
				}
				if(StringUtils.isNotEmpty(simpleMatcher.group(2))){
					page = simpleMatcher.group(2);
				}
			}
			String instrument = matcher.group(2).trim();
			instrument = instrument.replaceFirst("(?is)\\s*/\\s*$", "");
			//String doctype = matcher.group(3).trim();
			List<List>body = new ArrayList<List>();
			
			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			line.add(instrument);
			body.add(line);
			
			if (!body.isEmpty()){
				
				String[] header = { "Book", "Page", "InstrumentNumber" };
				
				ResultTable rt = (ResultTable) resultMap.get("CrossRefSet");
				if(rt == null) {
					rt = GenericFunctions2.createResultTable(body, header);
				} else {
					ResultTable crFromSet = rt;
					rt = GenericFunctions2.createResultTable(body, header);
					try {
						rt = ResultTable.joinVertical(rt, crFromSet, true);
					} catch (Exception e) {
						ro.cst.tsearch.servers.types.GenericSKLD.getLogger().error("Error while joining crossRefs", e);

					}
				}
				resultMap.put("CrossRefSet", rt);
				
				
			}
			
			String serverDoctype = (String) resultMap.get("SaleDataSet.DocumentType");
			String atsDoctypeCategory = null;
			if(StringUtils.isNotEmpty(serverDoctype)) {
				atsDoctypeCategory = DocumentTypes.getDocumentCategory(serverDoctype, 
						genericSKLDparser.getSearch().getID()); 
			}
			
			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
				TSServerInfoModule module = genericSKLDparser.getDefaultServerInfo()
					.getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, new SearchDataWrapper());
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
				if(atsDoctypeCategory != null) {
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CROSSREF_DOC_SOURCE, atsDoctypeCategory);
				}
				module.forceValue(0, book);
				module.forceValue(1, page);
				String tempBookPageLink = genericSKLDparser.createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) + 
					"&book=" + book + "&page=" + page;
				/*
				if(StringUtils.isNotEmpty(doctype)) {
					module.forceValue(7, doctype);
					tempBookPageLink += "&doctype=" + doctype;
				}
				*/
				resultMap.put("tmpModule", module);
				tempBookPageLink = "<a href=\"" + tempBookPageLink + "\" style=\"font-color: blue; font-weight: bold; font-size: 15px; \" >" + fragment + "</a>";
				resultMap.put("tmpBookPageLink", tempBookPageLink);
				
				
			} 
			if(StringUtils.isNotEmpty(instrument)) {
				TSServerInfoModule module = genericSKLDparser.getDefaultServerInfo()
					.getModuleForSearch(TSServerInfo.INSTR_NO_MODULE_IDX, new SearchDataWrapper());
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
				if(atsDoctypeCategory != null) {
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CROSSREF_DOC_SOURCE, atsDoctypeCategory);
				}
				simpleMatcher = SIMPLE_INSTRUMENT_PATTERN.matcher(instrument);
				
				if(simpleMatcher.find()) {
					String tempInstrumentLink = genericSKLDparser.createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX);
						
					if(StringUtils.isNotEmpty(simpleMatcher.group(1))){
						module.forceValue(2, simpleMatcher.group(1));
						tempInstrumentLink += "&instrumentNo=" + simpleMatcher.group(1);
					}
					if(StringUtils.isNotEmpty(simpleMatcher.group(2))){
						module.forceValue(10, simpleMatcher.group(2));
						tempInstrumentLink += "&year=" + simpleMatcher.group(2);
					}
					/*
					if(StringUtils.isNotEmpty(doctype)) {
						module.forceValue(7, doctype);
						tempInstrumentLink += "&doctype=" + doctype;
					}
					*/
					tempInstrumentLink = "<a href=\"" + tempInstrumentLink + "\" style=\"font-color: blue; font-weight: bold;  font-size: 15px;\" >" + instrument + "</a>";
					resultMap.put("tmpInstrumentLink", tempInstrumentLink);
					if(resultMap.get("tmpModule") == null) {
						resultMap.put("tmpModule", module);
					}
				}
				
				
			}
			
			
			
		}
	}


	@SuppressWarnings("rawtypes")
	public static void parseRemarksLine(Matcher matcher, ResultMap resultMap,
			GenericSKLD genericSKLDParser) {
		if(matcher == null) {
			return;
		}
		matcher.reset();
		if(matcher.find()) {
			String fragment = matcher.group(1).trim();
			fragment = fragment.replaceAll("\\bPART\\b", "");
			fragment = fragment.replaceAll("\\bINC LEGAL\\b", "");
			
			Matcher simpleMatcher = SSN4_PATTERN.matcher(fragment);
			if(simpleMatcher.find()) {
				String firstPart = simpleMatcher.group(1);
				String oldSsn = (String) resultMap.get("tmpSSN4");
				if(oldSsn == null) {
					resultMap.put("tmpSSN4", firstPart);
				} else {
					resultMap.put("tmpSSN4", oldSsn + "," + firstPart);
				}
				return;
			}
			
			//LTS , LOTS, LOT, BLK, BLOCK, BLOCKS, BLKS
			Matcher lbMatcher = Pattern.compile("\\b(LO?TS?|BLO?C?KS?)(\\b|\\d)").matcher(fragment);

			if(!lbMatcher.find()) {
			
				if(fragment.matches("[\\dA-Z=\\s-]+")) {
					String[] fragmentArrays = fragment.split("\\s+");
					String[] header = { "Book", "Page", "InstrumentNumber" };
					
					for (int i = 0; i < fragmentArrays.length; i++) {
						String subfragment = fragmentArrays[i];
						simpleMatcher = SIMPLE_INSTRUMENT_PATTERN.matcher(subfragment.replaceAll("\\s", ""));
						
						if(simpleMatcher.find()) {
							String firstPart = simpleMatcher.group(1);
							String secondPart = simpleMatcher.group(2);
							String thirdPard = "";
						
							List<List>body = new ArrayList<List>();
							List<String> line = new ArrayList<String>();
							
							try {
								if(firstPart.matches("\\d{4}") && secondPart.length() > 4) {
									Calendar cal = Calendar.getInstance();
									if(Integer.parseInt(firstPart) <= cal.get(Calendar.YEAR)) {
										Date effectiveStartDate = genericSKLDParser.getDataSite().getEffectiveStartDate();
										if (effectiveStartDate != null){
											Calendar calendarEffective = Calendar.getInstance();
											calendarEffective.setTime(effectiveStartDate);
											if (Integer.parseInt(firstPart) >= calendarEffective.get(Calendar.YEAR)){
												thirdPard = secondPart + "-" + firstPart;
												secondPart = "";
												firstPart = "";
											}
										}
									}
								}
							} catch (Exception e) {
								ro.cst.tsearch.servers.types.GenericSKLD.getLogger().error("Error while joining crossRefs", e);
	
							}
							
							line.add(firstPart);
							line.add(secondPart);
							line.add(thirdPard);
							body.add(line);
						
							ResultTable rt = (ResultTable) resultMap.get("CrossRefSet");
							if(rt == null) {
								rt = GenericFunctions2.createResultTable(body, header);
							} else {
								ResultTable crFromSet = rt;
								rt = GenericFunctions2.createResultTable(body, header);
								try {
									rt = ResultTable.joinVertical(rt, crFromSet, true);
								} catch (Exception e) {
									ro.cst.tsearch.servers.types.GenericSKLD.getLogger().error("Error while joining crossRefs", e);
	
								}
							}
							resultMap.put("CrossRefSet", rt);
						} else if(subfragment.matches("[\\d-]+")){
							subfragment = subfragment.replaceAll("\\s", "");
							if(i+1 >= fragmentArrays.length && subfragment.length() > 4) {
								if(subfragment.length() >= 7) {
									try {
										SimpleDateFormat temp = new SimpleDateFormat("yy");
										temp.setLenient(false);
										String year = (new SimpleDateFormat("yyyy")).format(temp.parse(subfragment.substring(0, 2)));
										if(year.matches("\\d+")) {
											int yearInt = Integer.parseInt(year);
											if(yearInt > 1950 && yearInt <= Calendar.getInstance().get(Calendar.YEAR)) {	
												subfragment = org.apache.commons.lang.StringUtils.removeStart(subfragment.substring(2), "0") + "-" + year;
											}
										}
									} catch (Exception e) {
										ro.cst.tsearch.servers.types.GenericSKLD.getLogger().error(e);
									}
								}
								List<List>body = new ArrayList<List>();
								List<String> line = new ArrayList<String>();
								line.add("");
								line.add("");
								line.add(subfragment);
								body.add(line);
							
								ResultTable rt = (ResultTable) resultMap.get("CrossRefSet");
								if(rt == null) {
									rt = GenericFunctions2.createResultTable(body, header);
								} else {
									ResultTable crFromSet = rt;
									rt = GenericFunctions2.createResultTable(body, header);
									try {
										rt = ResultTable.joinVertical(rt, crFromSet, true);
									} catch (Exception e) {
										ro.cst.tsearch.servers.types.GenericSKLD.getLogger().error("Error while joining crossRefs", e);
	
									}
								}
								resultMap.put("CrossRefSet", rt);
							} else if(i+1 < fragmentArrays.length && 
									subfragment.matches("\\d+") && 
									fragmentArrays[i+1].replaceAll("\\s", "").matches("\\d+")) {
								i++;
								String firstPart = subfragment;
								String secondPart = fragmentArrays[i].replaceAll("\\s", "");
							
								List<List>body = new ArrayList<List>();
								List<String> line = new ArrayList<String>();
								line.add(firstPart);
								line.add(secondPart);
								line.add("");
								body.add(line);
							
								ResultTable rt = (ResultTable) resultMap.get("CrossRefSet");
								if(rt == null) {
									rt = GenericFunctions2.createResultTable(body, header);
								} else {
									ResultTable crFromSet = rt;
									rt = GenericFunctions2.createResultTable(body, header);
									try {
										rt = ResultTable.joinVertical(rt, crFromSet, true);
									} catch (Exception e) {
										ro.cst.tsearch.servers.types.GenericSKLD.getLogger().error("Error while joining crossRefs", e);
	
									}
								}
								resultMap.put("CrossRefSet", rt);
							}
						}
					}
					
					
				
				
				
				}
			
			}
		}
		
		
		
		
	}


	

	

}

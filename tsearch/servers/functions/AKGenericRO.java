package ro.cst.tsearch.servers.functions;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

public class AKGenericRO {
	
	private static final Pattern PATTERN_RECORDED_DATE = Pattern.compile("Date Recorded:\\s*([0-9/]+)");
	private static final Pattern PATTERN_BOOK = Pattern.compile("Book:\\s*([0-9]+)");
	private static final Pattern PATTERN_PAGE = Pattern.compile("Page:\\s*([0-9]+)");
	private static final Pattern PATTERN_PLAT_BOOK_PAGE = Pattern.compile("(?is)Assoc\\.Plat:\\s*([A-Z\\d]+)\\s*-\\s*([A-Z\\d]+)");
	private static final Pattern PATTERN_OTHER_ID_FULL = Pattern.compile("BK\\s*(\\d+)\\s*PG\\s*(\\d+)");
	private static final Pattern PATTERN_OTHER_ID_SHORT = Pattern.compile("Other ID\\s*:\\s*(\\d+)\\s+(\\d+)");
	private static final Pattern PATTERN_FULL_INSTRUMENT_NUMBER = Pattern.compile("(\\d+)-(\\d+)-(\\d+)");
	private static final Pattern PATTERN_FULL_DIRTY_INSTRUMENT_NUMBER = Pattern.compile("(\\d+)_(\\d+)");
	//private static final Pattern PATTERN_SHORT_INSTRUMENT_NUMBER = Pattern.compile("(\\d+)-(\\d+)");
	
	public static String parseDetailedData(
			ServerResponse response, String detailsHtml, ResultMap resultMap, 
			ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		String imageLink = null;
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
		
			NodeList instrumentList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", ro.cst.tsearch.servers.types.AKGenericRO.ATS_PARSED_INSTRUMENT_ID));
			String accountId = null;
			if(instrumentList.size() == 0) {
				return null;
			} else {
				accountId = cleanInstrumentNumber(instrumentList.elementAt(0).toPlainTextString()
					.replace("Selected Document:", ""));
					
			}
			
			
			
			resultMap.put("SaleDataSet.InstrumentNumber", accountId);
			resultMap.put("OtherInformationSet.SrcType","RO");
			
			NodeList allTdList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new TagNameFilter("td"), true);
			List<String> grantorList = new ArrayList<String>();
			List<String> granteeList = new ArrayList<String>();
			List<String> legalList = new ArrayList<String>();
			List<LinkTag> referenceLinkList = new ArrayList<LinkTag>();
			List<String> referenceStringList = new ArrayList<String>();
			
			for (int i = 0; i < allTdList.size(); i++) {
				TableColumn column = (TableColumn) allTdList.elementAt(i);
				String plainText = column.toPlainTextString().trim();
				if(plainText.startsWith("Date Recorded:")) {
					Matcher matcher = PATTERN_RECORDED_DATE.matcher(plainText);
					if(matcher.find()) {
						resultMap.put("SaleDataSet.RecordedDate", matcher.group(1));
					}
					matcher = PATTERN_PAGE.matcher(plainText);
					if(matcher.find()) {
						resultMap.put("SaleDataSet.Page", matcher.group(1));
					}
					matcher = PATTERN_BOOK.matcher(plainText);
					if(matcher.find()) {
						resultMap.put("SaleDataSet.Book", matcher.group(1));
					}
				} else if(plainText.startsWith("Desc:")) {
					String nodesctext = plainText.replace("Desc:", "").trim();
					if(nodesctext.matches("^.*[\\$\\s]+([\\d,\\.]+)\\s*$")) {
						String amountText = nodesctext.replaceAll("^.*[\\$\\s]+([\\d,\\.]+)\\s*$", "$1").replaceAll("\\s+", "").replaceAll("[\\$,\\s]*", "");
						resultMap.put("SaleDataSet.MortgageAmount", amountText);
						resultMap.put("SaleDataSet.ConsiderationAmount", amountText);
					}
					String doctype = nodesctext.replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
					resultMap.put("SaleDataSet.DocumentType", doctype);
					if ("PLAT".equals(doctype)){
						column = (TableColumn) allTdList.elementAt(i-1);
						plainText = column.toPlainTextString().trim();
						if(plainText.startsWith("Assoc.Plat:")) {
							nodesctext = plainText.replaceAll("(?is)&nbsp;", " ").trim();
							Matcher matcher = PATTERN_PLAT_BOOK_PAGE.matcher(nodesctext);
							if(matcher.find()) {
								resultMap.put("SaleDataSet.Book", matcher.group(1));
								resultMap.put("SaleDataSet.Page", matcher.group(2));
							}
						}
					}
					
				} else if(plainText.startsWith("Grantor -")) {
					grantorList.add(plainText.replace("Grantor -", "").trim());
				} else if(plainText.startsWith("Debtor -")) {
					grantorList.add(plainText.replace("Debtor -", "").trim());
				} else if(plainText.startsWith("Grantee -")) {
					granteeList.add(plainText.replace("Grantee -", "").trim());
				} else if(plainText.startsWith("Secured -")) {
					granteeList.add(plainText.replace("Secured -", "").trim());
				} else if(plainText.startsWith("Location:")) {
					String legal = null;
					if(column.getParent() instanceof TableRow) {
						legal = column.getParent().toPlainTextString().replaceAll("&nbsp;", " ").trim();
						if(((TableRow)column.getParent()).getColumnCount() == 2) {
							i++;
						}
					} else {
						legal = plainText.replaceAll("&nbsp;", " ").trim();
						
					}
					if(i+1 < allTdList.size() && allTdList.elementAt(i+1).toPlainTextString().trim().startsWith("Additional Information:")) {
						i++;
						legal += " " + allTdList.elementAt(i).toPlainTextString().replaceAll("&nbsp;", " ").trim();
					}
					legalList.add(legal);
				} else if(plainText.startsWith("Amount:")) {
					String amount = plainText.replace("Amount:", "").trim().replaceAll("[\\$,\\s]*", "").replaceAll("&nbsp;", "");
					resultMap.put("SaleDataSet.MortgageAmount", amount);
					resultMap.put("SaleDataSet.ConsiderationAmount", amount);
				} else if(plainText.contains("Associated Doc:") || plainText.contains("Other ID: ")) {
					if(plainText.contains("Associated Doc:")) {
						NodeList associatedList =  column.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if(associatedList.size() > 0) {
							for (int j = 0; j < associatedList.size(); j++) {
								LinkTag associatedLink = (LinkTag) associatedList.elementAt(j);
								if(associatedLink.getLink().contains("AssocDocs.cfm")) {
									referenceLinkList.add(associatedLink);
								}
							}
						}
					}
					if (plainText.contains("Other ID: ")) {
						Matcher m = PATTERN_OTHER_ID_FULL.matcher(plainText);
						while(m.find()) {
							referenceStringList.add(m.group(1) + " " + m.group(2));
						}
						m = PATTERN_OTHER_ID_SHORT.matcher(plainText);
						while(m.find()) {
							referenceStringList.add(m.group(1) + " " + m.group(2));
						}
						
					}
					
				} 
			}
			
			
			parseNames(resultMap, grantorList, granteeList, akGenericRO.getSearch().getID());
			parseLegals(resultMap, legalList);
			parseReferences(response, resultMap, referenceLinkList, referenceStringList, akGenericRO);
			
			NodeList imageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			for (int i = 0; i < imageList.size(); i++) {
				LinkTag image = (LinkTag) imageList.elementAt(i);
				if(image.toPlainTextString().trim().equals("View Image")) {
					imageLink = image.extractLink();
					break;
				}
			}
			
		
		} catch (Exception e) {
			imageLink = null;
			e.printStackTrace();
		}
		
		return imageLink;
		
	}
	
	public static String cleanInstrumentNumber(String originalInstrumentNumber) {
		if(originalInstrumentNumber == null) {
			return "";
		}
		originalInstrumentNumber = originalInstrumentNumber.trim();
		Matcher matcher = PATTERN_FULL_INSTRUMENT_NUMBER.matcher(originalInstrumentNumber);
		if(matcher.find()) {
			if("0".equals(matcher.group(3))) {
				return matcher.group(1) + "_" + matcher.group(2);
			}
			return matcher.group(1) + matcher.group(2) + matcher.group(3);
		} else if(originalInstrumentNumber.matches("\\d{11}")) {
			if(originalInstrumentNumber.charAt(10) == '0') {
				return originalInstrumentNumber.substring(0, 4) + "_" + originalInstrumentNumber.substring(4, 10);
			}
			return originalInstrumentNumber;
		}
		return originalInstrumentNumber.replaceAll("-", "").trim();
	}
	
	public static String[] uncleanInstrumentNumber(String dirtyInstrumentNumber) {
		if(dirtyInstrumentNumber == null) {
			return null;
		}
		dirtyInstrumentNumber = dirtyInstrumentNumber.trim();
		Matcher matcher = PATTERN_FULL_DIRTY_INSTRUMENT_NUMBER.matcher(dirtyInstrumentNumber);
		if(matcher.find()) {
			String[] tokens = new String[3];
			tokens[0] = matcher.group(1);
			tokens[1] = matcher.group(2);
			if(tokens[1].length() == 7) {
				tokens[2] = tokens[1].substring(6);
				tokens[1] = tokens[1].substring(1, 6);
			} else {
				tokens[2] = "0";
			}
			return tokens;
		} else if(dirtyInstrumentNumber.matches("\\d{10,11}")) {
			String[] tokens = new String[3];
			tokens[0] = dirtyInstrumentNumber.substring(0, 4);
			tokens[1] = dirtyInstrumentNumber.substring(4, 10);
			if(dirtyInstrumentNumber.length() == 11) {
				tokens[2] = dirtyInstrumentNumber.substring(10);
			} else {
				tokens[2] = "0";
			}
			return tokens;
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private static void parseReferences(ServerResponse response, ResultMap resultMap,
			List<LinkTag> referenceLinkList, List<String> referenceStringList, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		if( (referenceLinkList == null || referenceLinkList.isEmpty()) && (referenceStringList == null || referenceStringList.isEmpty()) ) {
			return;
		}
		String[] header = { "InstrumentNumber" ,"Book", "Page" };
		List<List>body = new ArrayList<List>();
		HashSet<String> alreadyProcessed = new HashSet<String>();
		if(referenceLinkList != null) {
			for (LinkTag reference : referenceLinkList) {
				String toProcess = cleanInstrumentNumber(reference.toPlainTextString());
				if(!alreadyProcessed.contains(toProcess)) {
					alreadyProcessed.add(toProcess);
					List<String> line = new ArrayList<String>();
					line.add(toProcess);
					line.add("");
					line.add("");
					body.add(line);
					addCrossreferences(response, akGenericRO, reference);
				}
			}
		}
		
		if(referenceStringList != null ) {
			for (String reference : referenceStringList) {
				if(reference.contains(" ")) {
					try {
						String book = reference.substring(0, reference.indexOf(" "));
						String page = reference.substring(reference.lastIndexOf(" ") + 1);
						
						SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
						TSServerInfoModule module = new TSServerInfoModule(
								akGenericRO.getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX)); 
						
						module.setData(0, ro.cst.tsearch.servers.types.AKGenericRO.DISTRICT_MAP.get(akGenericRO.getServerID()));
						module.setData(1, book);
						module.setData(2, page);
						module.setVisible(false);
						module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);			
						
						ServerResponse serverResponse = akGenericRO.SearchBy(module, searchDataWrapper);
						
						String details = serverResponse.getResult();
						
						if(details != null) {
							StringBuilder outputTable = new StringBuilder();
							Collection<ParsedResponse> smartParsedResponses =
								smartParseDocumentIntermediary(serverResponse, details, outputTable , akGenericRO);
							if(!smartParsedResponses.isEmpty()) {
								ParsedResponse parsedResponse = smartParsedResponses.iterator().next();
								DocumentI documentI = parsedResponse.getDocument();
								if(documentI != null) {
									if(book.equals(documentI.getBook()) && page.equals(documentI.getPage())) {
										List<String> line = new ArrayList<String>();
										line.add(documentI.getInstno());
										line.add(documentI.getBook());
										line.add(documentI.getPage());
										body.add(line);
										LinkInPage linkInPage = parsedResponse.getPageLink();
										if (linkInPage != null) {
											String linkString = linkInPage.getLink() + "&isSubResult=true";
											LinkInPage pl = new LinkInPage(linkString,linkString,TSServer.REQUEST_SAVE_TO_TSD);
											parsedResponse.setPageLink(pl);
											
											response.getParsedResponse().addOneResultRowOnly(parsedResponse);
										}
									}
								}
							}
							
						}
					} catch (Exception e) {
						akGenericRO.getLogger().error("Error while parsing BP references", e);
					}
				}
			}
		}
		
		if(!body.isEmpty()) {
			resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
		}
		
	}

	private static void addCrossreferences(ServerResponse response,
			ro.cst.tsearch.servers.types.AKGenericRO akGenericRO,
			LinkTag referenceTag) {
		String originalLink = null;
		try {
			originalLink = referenceTag.extractLink();
			
			if(originalLink.contains("Link=")) {
				
				TSServerInfo info = akGenericRO.getDefaultServerInfoWrapper();
		        originalLink = info.getServerLink() + originalLink.substring(originalLink.indexOf("Link=") + 5);
			
				String referenceHtml = akGenericRO.getLinkContents(originalLink);
				
				Parser parser = Parser.createParser(referenceHtml, null);
				NodeList nodeList = parser.parse(null);
				
				
				NodeList linkList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
					.extractAllNodesThatMatch(new HasAttributeFilter("align"))
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);
				
				for (int i = 0; i < linkList.size(); i++) {
					ParsedResponse prChild = new ParsedResponse();
					LinkTag linkToDocument = (LinkTag) linkList.elementAt(i);
					String linkString = null;
					if(linkToDocument.extractLink().startsWith("/")) {
						linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idGET) + linkToDocument.extractLink().trim().replaceAll("\\s", "%20") + "&isSubResult=true";
					} else {
						linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idGET) + "/ssd/recoff/sag/" + linkToDocument.extractLink().trim().replaceAll("\\s", "%20") + "&isSubResult=true";
					}
					LinkInPage pl = new LinkInPage(linkString,linkString,TSServer.REQUEST_SAVE_TO_TSD);
					prChild.setPageLink(pl);
					
					response.getParsedResponse().addOneResultRowOnly(prChild);
					
				}
			}
		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing crossreferences for link " + originalLink, e);
		}
	}

	private static void parseLegals(ResultMap resultMap,
			List<String> legalList) {
		Vector<PropertyIdentificationSet> pisVector = new Vector<PropertyIdentificationSet>();
		for (String rawLegal : legalList) {
			PropertyIdentificationSet pis = parseLegal(rawLegal);
			if(pis != null && !pis.isEmpty()) {
				pisVector.add(pis);
			}
		}
		if(!pisVector.isEmpty()) {
			resultMap.put("PropertyIdentificationSet", pisVector);
		}
	}

	private static PropertyIdentificationSet parseLegal(String rawLegal) {
		PropertyIdentificationSet pis = new PropertyIdentificationSet();
		String lot = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Lot:\\s*([-\\w]+)");
		if(StringUtils.isNotEmpty(lot)) {
			pis.setAtribute("SubdivisionLotNumber", Roman.normalizeRomanNumbers(lot));
		}
		String block = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Block:\\s*([-\\w]+)");
		if(StringUtils.isNotEmpty(block)) {
			pis.setAtribute("SubdivisionBlock", block);
		}
		String section = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Section:\\s*(\\w+)");
		if(StringUtils.isNotEmpty(section)) {
			pis.setAtribute("SubdivisionSection", section);
		}
		String township = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Township:\\s*(\\w+)");
		if(StringUtils.isNotEmpty(township)) {
			pis.setAtribute("SubdivisionTownship", township);
		}
		String range = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Range:\\s*(\\w+)");
		if(StringUtils.isNotEmpty(range)) {
			pis.setAtribute("SubdivisionRange", range);
		}
		String tract = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Tract:\\s*([-\\w]+)");
		if(StringUtils.isNotEmpty(tract)) {
			pis.setAtribute("SubdivisionTract", tract);
		}
		
		String plat = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Plat:\\s*([-\\w]+)");
		if(StringUtils.isNotEmpty(plat)) {
			Pattern platPattern = Pattern.compile("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)(?:-([a-zA-Z\\d]+))?");
			Matcher platMatcher = platPattern.matcher(plat);
			if(platMatcher.find()) {
				pis.setAtribute("PlatBook", platMatcher.group(1));
				if(platMatcher.group(3) != null) { 
					pis.setAtribute("PlatNo", platMatcher.group(2) + platMatcher.group(3));
				} else {
					pis.setAtribute("PlatNo", platMatcher.group(2));
				}
			} else {
				pis.setAtribute("PlatInstr", plat);
			}
		}
		String unit = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "Apt/Unit:\\s*([-\\w]+)");
		if(StringUtils.isNotEmpty(unit)) {
			pis.setAtribute("SubdivisionUnit", unit);
			pis.setAtribute("SubdivisionCond", "true");
		}
		
		String bld = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "BLDG\\s+([-\\w]+)");
		if(StringUtils.isNotEmpty(bld)) {
			pis.setAtribute("SubdivisionBldg", bld);
		}
		String ap = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bAP\\s+([-\\w]+)");
		if(StringUtils.isNotEmpty(ap)) {
			if(ap.matches("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)")) {
				pis.setAtribute("SubdivisionBldg", ap.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$1"));
				pis.setAtribute("SubdivisionUnit", ap.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$2"));
			} else {
				pis.setAtribute("SubdivisionUnit", ap);
			}
		} else {
			ap = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bUNIT\\s+([-\\w]+)");
			if(StringUtils.isNotEmpty(ap)) {
				if(ap.matches("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)")) {
					pis.setAtribute("SubdivisionBldg", ap.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$1"));
					pis.setAtribute("SubdivisionUnit", ap.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$2"));
				} else {
					pis.setAtribute("SubdivisionUnit", ap);
				}
				
			} 
		}
		
		 
		
		return pis;
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m,List<String> grantorList, List<String> granteeList, long searchId) throws Exception {

		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		
		String grantorAsString = "";
		String granteeAsString = "";
		String[] suffixes, type, otherType;
		
		for (String grantorString : grantorList) {
			grantorString = cleanName(grantorString);
			if(StringUtils.isNotEmpty(grantorString)) {
				String names[] = null;
				if(NameUtils.isCompany(grantorString)) {
					names = new String[]{"", "", grantorString, "", "", ""};
				} else {
					names = StringFormats.parseNameLikeDavidsonAOOnlyBetter(grantorString);
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				grantorAsString += grantorString + "/";
				//GenericFunctions.addOwnerNames(grantorString, names, grantor);
				GenericFunctions.addOwnerNames(grantorString, names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantor);
			}
		}
		
		for (String granteeString : granteeList) {
			granteeString = cleanName(granteeString);
			if(StringUtils.isNotEmpty(granteeString)) {
				String names[] = null;
				if(NameUtils.isCompany(granteeString)) {
					names = new String[]{"", "", granteeString, "", "", ""};
				} else {
					names = StringFormats.parseNameLikeDavidsonAOOnlyBetter(granteeString);
				}
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				granteeAsString += granteeString + "/";
				GenericFunctions.addOwnerNames(granteeString, names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantee);
			}
		}
		if(StringUtils.isNotEmpty(granteeAsString)) {
			m.put("SaleDataSet.Grantee", granteeAsString.substring(0,granteeAsString.length() - 1));
		}
		if(StringUtils.isNotEmpty(grantorAsString)) {
			m.put("SaleDataSet.Grantor", grantorAsString.substring(0,grantorAsString.length() - 1));
		} 
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		

	}
	
	private static String cleanName(String toClean) {
		if(toClean == null) {
			return "";
		}
		toClean = toClean.replaceAll("TO WHOM IT MAY CONCERN", "");
		toClean = toClean.replaceAll("(?is)\\b(?:CO\\s+)?(TRUSTEE).*", "$1");
		toClean = toClean.replaceAll("(?is)\\b(?:SETTLOR AND)\\s+(TRUSTEE).*", "$1");
		return toClean.trim();
	}

	public static Collection<ParsedResponse> smartParseDocumentIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			for (int i = 1; i < trList.length; i++) {
				TableColumn[] columns = trList[i].getColumns();
				if(columns.length >= 6 ) {
					NodeList linkList = columns[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if(linkList.size() > 0) {
						ResultMap resultMap = new ResultMap();
						ParsedResponse currentResponse = new ParsedResponse();
						StringBuilder innerRow = new StringBuilder();
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						linkTag.setLink(linkString);
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						String documentNumber = cleanInstrumentNumber(linkTag.toPlainTextString());
						String serverDocType = columns[5].toPlainTextString().trim().replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
						resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
	    				resultMap.put("OtherInformationSet.SrcType", "RO");
	    				resultMap.put("SaleDataSet.RecordedDate", columns[1].toPlainTextString().trim());
	    				resultMap.put("SaleDataSet.Book", columns[2].toPlainTextString().replace("&nbsp;", "").trim());
						resultMap.put("SaleDataSet.Page", columns[3].toPlainTextString().replace("&nbsp;", "").trim());
						resultMap.put("SaleDataSet.DocumentType", serverDocType);
						
						
						HashMap<String, String> data = null;
						if (!StringUtils.isEmpty(serverDocType)) {
							data = new HashMap<String, String>();
							data.put("type", serverDocType);
							data.put("book", (String)resultMap.get("SaleDataSet.Book"));
							data.put("page", (String)resultMap.get("SaleDataSet.Page"));
						}
						String checkbox = null;
						if (akGenericRO.isInstrumentSaved(documentNumber, null, data) ) {
			    			checkbox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
	            			currentResponse.setPageLink(linkInPage);
			    		}
						
	    				for (int j = 0; j < columns.length; j++) {
	    					if(StringUtils.isEmpty(columns[j].toPlainTextString())) {
	    						innerRow.append("<td>&nbsp;</td>");
	    					} else {
	    						innerRow.append(columns[j].toHtml());
	    					}
						}
	    				innerRow.append("</tr>");
	    				
	    				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
	    				
	    				Bridge bridge = new Bridge(currentResponse, resultMap, akGenericRO.getSearch().getID());
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				currentResponse.setDocument(document);
	    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow.toString());
	    				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td>" + innerRow.toString());
	    				responses.add(currentResponse);
	    				
					} 
					
				}
				
			}
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				NodeList districtList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"))
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "District"));
				NodeList yearList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "text"))
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "StartingDocYear"));
				NodeList seqList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "text"))
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "StartingDocSeq"));
				NodeList suffixList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "text"))
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "StartingDocSuffix"));
				if(districtList.size() > 0 && yearList.size() > 0 && seqList.size() > 0 && suffixList.size() > 0) {
					NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "myFORM"));
					String destinationPage = "NumberDocs.cfm";
					if(formList.size() == 1) {
						destinationPage = ((FormTag)formList.elementAt(0)).getAttribute("action");
					}
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/" + destinationPage + "?Next=More+Documents" + 
						"&District=" + URLEncoder.encode(((InputTag)districtList.elementAt(0)).getAttribute("value"), "UTF-8") + 
						"&StartingDocYear=" + URLEncoder.encode(((InputTag)yearList.elementAt(0)).getAttribute("value"), "UTF-8") + 
						"&StartingDocSeq=" + URLEncoder.encode(((InputTag)seqList.elementAt(0)).getAttribute("value"), "UTF-8") + 
						"&StartingDocSuffix=" + URLEncoder.encode(((InputTag)suffixList.elementAt(0)).getAttribute("value"), "UTF-8");
					outputTable.append("<tr><td align=\"center\" colspan=\"9\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
					
				} else {
					NodeList savedBookList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"))
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "SavedBook"));
					NodeList savedPageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"))
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "SavedPage"));
					NodeList startingBookList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("type", "text"))
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "StartingBook"));
					NodeList startingPageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("type", "text"))
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "StartingPage"));
					if(districtList.size() > 0 && startingBookList.size() > 0 && startingPageList.size() > 0 && savedBookList.size() > 0 && savedPageList.size() > 0 ) {
						String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
							"/ssd/recoff/sag/BookandPageDocs.cfm?Next=More+Documents" + 
							"&District=" + URLEncoder.encode(((InputTag)districtList.elementAt(0)).getAttribute("value"), "UTF-8") + 
							"&SavedBook=" + URLEncoder.encode(((InputTag)savedBookList.elementAt(0)).getAttribute("value"), "UTF-8") + 
							"&SavedPage=" + URLEncoder.encode(((InputTag)savedPageList.elementAt(0)).getAttribute("value"), "UTF-8") + 
							"&StartingPage=" + URLEncoder.encode(((InputTag)startingPageList.elementAt(0)).getAttribute("value"), "UTF-8") + 
							"&StartingBook=" + URLEncoder.encode(((InputTag)startingBookList.elementAt(0)).getAttribute("value"), "UTF-8");
						outputTable.append("<tr><td align=\"center\" colspan=\"9\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
						
					}
				}
			}
			
			akGenericRO.SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	public static Collection<ParsedResponse> smartParseDateIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);

			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			int numberOfUncheckedElements = 0;
			for (int i = 0; i < trList.length; i++) {
				String serverDocType = null;
				ParsedResponse currentResponse = new ParsedResponse();
				ResultMap resultMap = new ResultMap();
				LinkInPage linkInPage = null;
				StringBuilder innerTable = new StringBuilder("<table BORDER='1' CELLPADDING='2' width='100%'>\n");
				String checkbox = "checked";
				String documentNumber = null;
				for (int j = i; j < trList.length; j++, i++) {
					TableRow row = trList[i];
					if( row.getAttribute("bgcolor") == null)  {
						break;
					}
					NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if(linkList.size() > 0) {
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						linkTag.setLink(linkString);
						linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						serverDocType = linkTag.toPlainTextString().replaceAll("[\\$\\d,\\.]+", "").trim().replaceAll("\\s+", "");
						resultMap.put("SaleDataSet.DocumentType", serverDocType);
	    				resultMap.put("OtherInformationSet.SrcType", "RO");
					} else {
						for (TableColumn column : row.getColumns()) {
							String columnPlainText = column.toPlainTextString().trim();
							if(columnPlainText.startsWith("Date Recorded:")) {
								resultMap.put("SaleDataSet.RecordedDate", columnPlainText.replace("Date Recorded:", "").trim());
							} else if(columnPlainText.startsWith("Doc.Number:")) {
								documentNumber = cleanInstrumentNumber(columnPlainText.replace("Doc.Number:", ""));
								resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
							} else if(columnPlainText.startsWith("Book:")) {
								Pattern BOOK_PAGE = Pattern.compile("Book:\\s*([0-9]+)\\s*Page:\\s*([0-9]+)");
								Matcher bookPageMatcher = BOOK_PAGE.matcher(columnPlainText);
								if(bookPageMatcher.find()) {
									resultMap.put("SaleDataSet.Book", bookPageMatcher.group(1));
									resultMap.put("SaleDataSet.Page", bookPageMatcher.group(2));
								}
							}
						}
					}
					innerTable.append(row.toHtml());
				}
				
				HashMap<String, String> data = null;
				if (!StringUtils.isEmpty(serverDocType)) {
					data = new HashMap<String, String>();
					data.put("type", serverDocType);
				}
				if (akGenericRO.isInstrumentSaved((String)resultMap.get("SaleDataSet.InstrumentNumber"), null, data) ) {
	    			checkbox = "saved";
	    		} else {
	    			numberOfUncheckedElements++;
	    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
        			currentResponse.setPageLink(linkInPage);
	    		}
				
				innerTable.append("</table>\n");
				
				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
				
				Bridge bridge = new Bridge(currentResponse,resultMap, akGenericRO.getSearch().getID());
				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
				
				currentResponse.setDocument(document);
				
				
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, innerTable.toString());
				
				
				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td><td>" + innerTable.toString() + "</td></tr>");
				
				responses.add(currentResponse);

				
			}
			akGenericRO.SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			
			if(nextList.size() > 0) {
				
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id"))
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new OrFilter(
							new HasAttributeFilter("type", "text"), 
							new HasAttributeFilter("type", "hidden"))
							);
				if(inputList.size() > 0) {
					
					NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "myFORM"));
					String destinationPage = "DateDocs.cfm";
					if(formList.size() == 1) {
						destinationPage = ((FormTag)formList.elementAt(0)).getAttribute("action");
					}
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/" + destinationPage + "?Next=More+Documents"; 
					for (int i = 0; i < inputList.size(); i++) {
						InputTag inputTag = (InputTag) inputList.elementAt(i);
						String name = inputTag.getAttribute("name");
						if(StringUtils.isNotEmpty(name)) {
							linkString += "&" + name + "=" + 
										URLEncoder.encode(org.apache.commons.lang.StringUtils.defaultString(inputTag.getAttribute("value")), "UTF-8");
						}
					}
					
					outputTable.append("<tr><td align=\"center\" colspan=\"2\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
					
				} 
			}
			

		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}
	
	public static void main(String[] args) {
		//parseLegal("Location:  Lot: 2A   Section: 27 Location:  Lot: 7     Block: 35-B  Township: 014N   Range: 001W   Meridian: S   Q.Quarter:NWNW");
		//parseNames(n, grantorList, granteeList, searchId)
	}

	public static Collection<ParsedResponse> smartParseAssociatedIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			for (int i = 1; i < trList.length; i++) {
				TableColumn[] columns = trList[i].getColumns();
				if(columns.length >= 5 ) {
					NodeList linkList = columns[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if(linkList.size() > 0) {
						ResultMap resultMap = new ResultMap();
						ParsedResponse currentResponse = new ParsedResponse();
						StringBuilder innerRow = new StringBuilder();
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						linkTag.setLink(linkString);
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						String documentNumber = cleanInstrumentNumber(linkTag.toPlainTextString());
						String serverDocType = columns[3].toPlainTextString().trim().replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
						resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
	    				resultMap.put("OtherInformationSet.SrcType", "RO");
	    				resultMap.put("SaleDataSet.RecordedDate", columns[1].toPlainTextString().trim());
						resultMap.put("SaleDataSet.DocumentType", serverDocType);
						
						HashMap<String, String> data = null;
						if (!StringUtils.isEmpty(serverDocType)) {
							data = new HashMap<String, String>();
							data.put("type", serverDocType);
						}
						String checkbox = null;
						if (akGenericRO.isInstrumentSaved(documentNumber, null, data) ) {
			    			checkbox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
	            			currentResponse.setPageLink(linkInPage);
			    		}
						
	    				for (int j = 0; j <= 4; j++) {
	    					if(StringUtils.isEmpty(columns[j].toPlainTextString())) {
	    						innerRow.append("<td>&nbsp;</td>");
	    					} else {
	    						innerRow.append(columns[j].toHtml());
	    					}
						}
	    				innerRow.append("</tr>");
	    				
	    				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
	    				
	    				Bridge bridge = new Bridge(currentResponse, resultMap, akGenericRO.getSearch().getID());
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				currentResponse.setDocument(document);
	    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow.toString());
	    				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td>" + innerRow.toString());
	    				responses.add(currentResponse);
	    				
					} 
					
				}
				
			}
			akGenericRO.SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	public static Collection<ParsedResponse> smartParsePlatNumberIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);

			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String rowAsString = null;
			
			for (int i = 1; i < trList.length; i++) {
				
				ParsedResponse currentResponse = new ParsedResponse();
				ResultMap resultMap = new ResultMap();
				resultMap.put("OtherInformationSet.SrcType","RO");
				TableRow row = trList[i];
				TableColumn[] columns = row.getColumns();
				if(columns.length >= 5) {
					String plat = columns[0].toPlainTextString().replaceAll("&nbsp;", " ").trim();
					if(StringUtils.isNotEmpty(plat)) {
						if(plat.matches("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)")) {
							resultMap.put("PropertyIdentificationSet.PlatBook", plat.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$1"));
							resultMap.put("PropertyIdentificationSet.PlatNo", plat.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$2"));
						} else {
							resultMap.put("PropertyIdentificationSet.PlatInstr", plat);
						}
					}
					resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", columns[1].toPlainTextString().replaceAll("&nbsp;", " ").trim());
					resultMap.put("PropertyIdentificationSet.SubdivisionBlock", columns[2].toPlainTextString().replaceAll("&nbsp;", " ").trim());
					resultMap.put("PropertyIdentificationSet.SubdivisionTract", columns[3].toPlainTextString().replaceAll("&nbsp;", " ").trim());
					resultMap.put("PropertyIdentificationSet.SubdivisionUnit", columns[4].toPlainTextString().replaceAll("&nbsp;", " ").trim());
					
				}
				
				Bridge b = new Bridge(currentResponse, resultMap, akGenericRO.getSearch().getID());
		        b.mergeInformation();
				
				NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				LinkTag linkTag = (LinkTag) linkList.elementAt(0);
				String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(linkString);
				rowAsString = row.toHtml();
				
				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
				currentResponse.setOnlyResponse(rowAsString);
				LinkInPage linkInPage = new LinkInPage(
						linkString, 
						linkString, 
    					TSServer.REQUEST_GO_TO_LINK);
				currentResponse.setPageLink(linkInPage);
				
				responses.add(currentResponse);
				
			}
			
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				NodeList hiddenList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"));
			NodeList textList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "text"));
			NodeList districtList = hiddenList.extractAllNodesThatMatch(new HasAttributeFilter("name", "District"));
			NodeList platList = textList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Plat"));
			NodeList lotList = textList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Lot"));
			NodeList blockList = textList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Block"));
			NodeList trackList = textList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Tract"));
			NodeList apartamentList = textList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Apartment"));
			if(districtList.size() > 0 
					&& platList.size() > 0
					&& lotList.size() > 0
					&& blockList.size() > 0
					&& trackList.size() > 0
					&& apartamentList.size() > 0
					
			) {
				String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
					"/ssd/recoff/sag/PlatSearch.cfm?Next=More+Documents" + 
					"&District=" + URLEncoder.encode(((InputTag)districtList.elementAt(0)).getAttribute("value"), "UTF-8") + 
					"&Plat=" + URLEncoder.encode(((InputTag)platList.elementAt(0)).getAttribute("value"), "UTF-8") + 
					"&Lot=" + URLEncoder.encode(((InputTag)lotList.elementAt(0)).getAttribute("value"), "UTF-8") + 
					"&Block=" + URLEncoder.encode(((InputTag)blockList.elementAt(0)).getAttribute("value"), "UTF-8") + 
					"&Tract=" + URLEncoder.encode(((InputTag)trackList.elementAt(0)).getAttribute("value"), "UTF-8") + 
					"&Apartment=" + URLEncoder.encode(((InputTag)apartamentList.elementAt(0)).getAttribute("value"), "UTF-8") ;
				outputTable.append("<tr><td align=\"center\" colspan=\"5\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
				serverResponse.getParsedResponse().setNextLink("<a href='" + linkString + "' />");
			} 
		}
			

		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	/**
	 * Recorders Office - Documents by a Plat Number
	 * @param serverResponse
	 * @param table
	 * @param outputTable
	 * @param akGenericRO
	 * @return
	 */
	public static Collection<ParsedResponse> smartParsePlatListIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			for (int i = 1; i < trList.length; i++) {
				TableColumn[] columns = trList[i].getColumns();
				if(columns.length >= 6 ) {
					NodeList linkList = columns[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if(linkList.size() > 0) {
						ResultMap resultMap = new ResultMap();
						ParsedResponse currentResponse = new ParsedResponse();
						StringBuilder innerRow = new StringBuilder();
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String documentNumber = cleanInstrumentNumber(akGenericRO.getInstrumentFromLink(linkTag.extractLink()));
						String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						linkTag.setLink(linkString);
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						
						String serverDocType = columns[0].toPlainTextString().trim()
							.replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
						resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
	    				resultMap.put("OtherInformationSet.SrcType", "RO");
	    				resultMap.put("SaleDataSet.RecordedDate", columns[1].toPlainTextString().trim());
						resultMap.put("SaleDataSet.DocumentType", serverDocType);
						resultMap.put("SaleDataSet.Book", columns[3].toPlainTextString().replace("&nbsp;", "").trim());
						resultMap.put("SaleDataSet.Page", columns[4].toPlainTextString().replace("&nbsp;", "").trim());
						
						HashMap<String, String> data = null;
						if (!StringUtils.isEmpty(serverDocType)) {
							data = new HashMap<String, String>();
							data.put("type", serverDocType);
							data.put("book", (String)resultMap.get("SaleDataSet.Book"));
							data.put("page", (String)resultMap.get("SaleDataSet.Page"));
						}
						String checkbox = null;
						if (akGenericRO.isInstrumentSaved(documentNumber, null, data) ) {
			    			checkbox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
	            			currentResponse.setPageLink(linkInPage);
			    		}
						
	    				for (int j = 0; j < 6; j++) {
	    					if(StringUtils.isEmpty(columns[j].toPlainTextString())) {
	    						innerRow.append("<td>&nbsp;</td>");
	    					} else {
	    						innerRow.append(columns[j].toHtml());
	    					}
						}
	    				innerRow.append("</tr>");
	    				
	    				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
	    				
	    				Bridge bridge = new Bridge(currentResponse, resultMap, akGenericRO.getSearch().getID());
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				currentResponse.setDocument(document);
	    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow.toString());
	    				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td>" + innerRow.toString());
	    				responses.add(currentResponse);
	    				
					} 
					
				}
				
			}
			akGenericRO.SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	/**
	 * http://dnr.alaska.gov/ssd/recoff/sag/MTRSDocs.cfm
	 * @param serverResponse
	 * @param table
	 * @param outputTable
	 * @param akGenericRO
	 * @return
	 */
	public static Collection<ParsedResponse> smartParseMTRSListIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			for (int i = 1; i < trList.length; i++) {
				TableColumn[] columns = trList[i].getColumns();
				if(columns.length >= 8 ) {
					NodeList linkList = columns[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if(linkList.size() > 0) {
						ResultMap resultMap = new ResultMap();
						ParsedResponse currentResponse = new ParsedResponse();
						StringBuilder innerRow = new StringBuilder();
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String documentNumber = cleanInstrumentNumber(akGenericRO.getInstrumentFromLink(linkTag.extractLink()));
						String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						linkTag.setLink(linkString);
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						
						String serverDocType = columns[2].toPlainTextString().trim()
							.replaceAll("^(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
						resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
	    				resultMap.put("OtherInformationSet.SrcType", "RO");
	    				resultMap.put("SaleDataSet.RecordedDate", columns[0].toPlainTextString().trim());
						resultMap.put("SaleDataSet.DocumentType", serverDocType);
						
						HashMap<String, String> data = null;
						if (!StringUtils.isEmpty(serverDocType)) {
							data = new HashMap<String, String>();
							data.put("type", serverDocType);
						}
						String checkbox = null;
						if (akGenericRO.isInstrumentSaved(documentNumber, null, data) ) {
			    			checkbox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
	            			currentResponse.setPageLink(linkInPage);
			    		}
						
	    				for (int j = 0; j < 8; j++) {
	    					if(StringUtils.isEmpty(columns[j].toPlainTextString())) {
	    						innerRow.append("<td>&nbsp;</td>");
	    					} else {
	    						innerRow.append(columns[j].toHtml());
	    					}
						}
	    				innerRow.append("</tr>");
	    				
	    				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
	    				
	    				Bridge bridge = new Bridge(currentResponse, resultMap, akGenericRO.getSearch().getID());
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				currentResponse.setDocument(document);
	    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow.toString());
	    				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td>" + innerRow.toString());
	    				responses.add(currentResponse);
	    				
					} 
					
				}
				
			}
			akGenericRO.SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "form"))
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new OrFilter(
							new HasAttributeFilter("type", "text"), 
							new HasAttributeFilter("type", "hidden"))
							);
				if(inputList.size() > 0) {
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/MTRSDocs.cfm?Next=More+Documents"; 
					for (int i = 0; i < inputList.size(); i++) {
						InputTag inputTag = (InputTag) inputList.elementAt(i);
						String name = inputTag.getAttribute("name");
						if(StringUtils.isNotEmpty(name)) {
							linkString += "&" + name + "=" + URLEncoder.encode(inputTag.getAttribute("value"), "UTF-8");
						}
					}
					outputTable.append("<tr><td align=\"center\" colspan=\"9\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
				}
				
			}
			
			
		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	public static Collection<ParsedResponse> smartParseSurveyIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);

			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String rowAsString = null;
			
			for (int i = 1; i < trList.length; i++) {
				
				ParsedResponse currentResponse = new ParsedResponse();
				TableRow row = trList[i];
				
				NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				LinkTag linkTag = (LinkTag) linkList.elementAt(0);
				String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(linkString);
				rowAsString = row.toHtml();
				
				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
				currentResponse.setOnlyResponse(rowAsString);
				LinkInPage linkInPage = new LinkInPage(
						linkString, 
						linkString, 
    					TSServer.REQUEST_GO_TO_LINK);
				currentResponse.setPageLink(linkInPage);
				
				responses.add(currentResponse);
				
			}
			
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "form"))
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new OrFilter(
							new HasAttributeFilter("type", "text"), 
							new HasAttributeFilter("type", "hidden"))
							);
				if(inputList.size() > 0) {
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/SurveySearch.cfm?Next=More+Documents"; 
					for (int i = 0; i < inputList.size(); i++) {
						InputTag inputTag = (InputTag) inputList.elementAt(i);
						String name = inputTag.getAttribute("name");
						if(StringUtils.isNotEmpty(name)) {
							linkString += "&" + name + "=" + URLEncoder.encode(inputTag.getAttribute("value"), "UTF-8");
						}
					}
					outputTable.append("<tr><td align=\"center\" colspan=\"6\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
				}
				
			}
		
			

		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	public static Collection<ParsedResponse> smartParseSubdivisionIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);

			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String rowAsString = null;
			
			for (int i = 1; i < trList.length; i++) {
				
				ParsedResponse currentResponse = new ParsedResponse();
				TableRow row = trList[i];
				
				NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				LinkTag linkTag = (LinkTag) linkList.elementAt(0);
				String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(linkString);
				rowAsString = row.toHtml();
				
				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
				currentResponse.setOnlyResponse(rowAsString);
				LinkInPage linkInPage = new LinkInPage(
						linkString, 
						linkString, 
    					TSServer.REQUEST_GO_TO_LINK);
				currentResponse.setPageLink(linkInPage);
				
				responses.add(currentResponse);
				
			}
			
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id"))
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new OrFilter(
							new HasAttributeFilter("type", "Text"), 
							new HasAttributeFilter("type", "hidden"))
							);
				if(inputList.size() > 0) {
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/SubDivisionSearch.cfm?Next=More+Documents"; 
					for (int i = 0; i < inputList.size(); i++) {
						InputTag inputTag = (InputTag) inputList.elementAt(i);
						String name = inputTag.getAttribute("name");
						if(StringUtils.isNotEmpty(name)) {
							linkString += "&" + name + "=" + URLEncoder.encode(inputTag.getAttribute("value"), "UTF-8");
						}
					}
					outputTable.append("<tr><td align=\"center\" colspan=\"6\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
				}
				
			}
		
			

		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	/**
	 * No Plat Subdivision 
	 * http://dnr.alaska.gov/ssd/recoff/sag/NoPlatSubDivSearch.cfm
	 * @param serverResponse
	 * @param table
	 * @param outputTable
	 * @param akGenericRO
	 * @return
	 */
	public static Collection<ParsedResponse> smartParseSubdivisionPlatIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);

			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String rowAsString = null;
			
			for (int i = 1; i < trList.length; i++) {
				ParsedResponse currentResponse = new ParsedResponse();
				TableRow row = trList[i];
				NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				LinkTag linkTag = (LinkTag) linkList.elementAt(0);
				String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(linkString);
				rowAsString = row.toHtml();
				
				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
				currentResponse.setOnlyResponse(rowAsString);
				LinkInPage linkInPage = new LinkInPage(
						linkString, 
						linkString, 
    					TSServer.REQUEST_GO_TO_LINK);
				currentResponse.setPageLink(linkInPage);
				responses.add(currentResponse);
			}
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id"))
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new OrFilter(
							new HasAttributeFilter("type", "Text"), 
							new HasAttributeFilter("type", "hidden"))
							);
				if(inputList.size() > 0) {
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/NoPlatSubDivSearch.cfm?Next=More+Documents"; 
					for (int i = 0; i < inputList.size(); i++) {
						InputTag inputTag = (InputTag) inputList.elementAt(i);
						String name = inputTag.getAttribute("name");
						if(StringUtils.isNotEmpty(name)) {
							linkString += "&" + name + "=" + URLEncoder.encode(inputTag.getAttribute("value"), "UTF-8");
						}
					}
					outputTable.append("<tr><td align=\"center\" colspan=\"7\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
				}
				
			}
		
			

		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	public static Collection<ParsedResponse> smartParseDocumentTypeIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			for (int i = 1; i < trList.length; i++) {
				TableColumn[] columns = trList[i].getColumns();
				if(columns.length >= 6 ) {
					NodeList linkList = columns[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if(linkList.size() > 0) {
						ResultMap resultMap = new ResultMap();
						ParsedResponse currentResponse = new ParsedResponse();
						StringBuilder innerRow = new StringBuilder();
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						linkTag.setLink(linkString);
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						String documentNumber = cleanInstrumentNumber(linkTag.toPlainTextString());
						String serverDocType = columns[4].toPlainTextString().trim().replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
						resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
	    				resultMap.put("OtherInformationSet.SrcType", "RO");
	    				resultMap.put("SaleDataSet.RecordedDate", columns[1].toPlainTextString().trim());
	    				resultMap.put("SaleDataSet.Book", columns[2].toPlainTextString().trim());
						resultMap.put("SaleDataSet.Page", columns[3].toPlainTextString().trim());
						resultMap.put("SaleDataSet.DocumentType", serverDocType);
						
						
						HashMap<String, String> data = null;
						if (!StringUtils.isEmpty(serverDocType)) {
							data = new HashMap<String, String>();
							data.put("type", serverDocType);
						}
						String checkbox = null;
						if (akGenericRO.isInstrumentSaved(documentNumber, null, data) ) {
			    			checkbox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
	            			currentResponse.setPageLink(linkInPage);
			    		}
						
	    				for (int j = 0; j < columns.length; j++) {
	    					if(StringUtils.isEmpty(columns[j].toPlainTextString())) {
	    						innerRow.append("<td>&nbsp;</td>");
	    					} else {
	    						innerRow.append(columns[j].toHtml());
	    					}
						}
	    				innerRow.append("</tr>");
	    				
	    				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
	    				
	    				Bridge bridge = new Bridge(currentResponse, resultMap, akGenericRO.getSearch().getID());
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				currentResponse.setDocument(document);
	    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow.toString());
	    				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td>" + innerRow.toString());
	    				responses.add(currentResponse);
	    				
					} 
					
				}
				
			}
			
			akGenericRO.SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id"))
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new OrFilter(
							new HasAttributeFilter("type", "text"), 
							new HasAttributeFilter("type", "hidden"))
							);
				if(inputList.size() > 0) {
					
					NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "myFORM"));
					String destinationPage = "IndexDocs.cfm";
					if(formList.size() == 1) {
						destinationPage = ((FormTag)formList.elementAt(0)).getAttribute("action");
					}
					
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/" + destinationPage + "?Next=More+Documents"; 
					for (int i = 0; i < inputList.size(); i++) {
						InputTag inputTag = (InputTag) inputList.elementAt(i);
						String name = inputTag.getAttribute("name");
						if(StringUtils.isNotEmpty(name)) {
							linkString += "&" + name + "=" + URLEncoder.encode(inputTag.getAttribute("value"), "UTF-8");
						}
					}
					outputTable.append("<tr><td align=\"center\" colspan=\"7\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
				}
				
			}
			
		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}

	public static Collection<ParsedResponse> smartParseUCCDocumentTypeIntermediary(
			ServerResponse serverResponse, String table,
			StringBuilder outputTable, ro.cst.tsearch.servers.types.AKGenericRO akGenericRO) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align"));
			
			if(tableList.size() < 1) {
				return null;
			}
			
			TableRow[] trList = null;
			TableTag resultTable = (TableTag)tableList.elementAt(0);
			trList = resultTable.getRows();
			if(trList == null || trList.length == 0) {
				return null;
			}
			String linkPrefix = akGenericRO.CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			for (int i = 1; i < trList.length; i++) {
				TableColumn[] columns = trList[i].getColumns();
				if(columns.length >= 4 ) {
					NodeList linkList = columns[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if(linkList.size() > 0) {
						ResultMap resultMap = new ResultMap();
						ParsedResponse currentResponse = new ParsedResponse();
						StringBuilder innerRow = new StringBuilder();
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						linkTag.setLink(linkString);
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						String documentNumber = cleanInstrumentNumber(linkTag.toPlainTextString());
						String serverDocType = columns[2].toPlainTextString().trim().replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
						resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
	    				resultMap.put("OtherInformationSet.SrcType", "RO");
	    				resultMap.put("SaleDataSet.RecordedDate", columns[1].toPlainTextString().trim());
	    				resultMap.put("SaleDataSet.DocumentType", serverDocType);
						
						
						HashMap<String, String> data = null;
						if (!StringUtils.isEmpty(serverDocType)) {
							data = new HashMap<String, String>();
							data.put("type", serverDocType);
						}
						String checkbox = null;
						if (akGenericRO.isInstrumentSaved(documentNumber, null, data) ) {
			    			checkbox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
	            			currentResponse.setPageLink(linkInPage);
			    		}
						
	    				for (int j = 0; j < columns.length; j++) {
	    					if(StringUtils.isEmpty(columns[j].toPlainTextString())) {
	    						innerRow.append("<td>&nbsp;</td>");
	    					} else {
	    						innerRow.append(columns[j].toHtml());
	    					}
						}
	    				innerRow.append("</tr>");
	    				
	    				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
	    				
	    				Bridge bridge = new Bridge(currentResponse, resultMap, akGenericRO.getSearch().getID());
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				currentResponse.setDocument(document);
	    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow.toString());
	    				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td>" + innerRow.toString());
	    				responses.add(currentResponse);
	    				
					} 
					
				}
				
			}
			
			akGenericRO.SetAttribute(TSServer.NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
			if(nextList.size() > 0) {
				
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id"))
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new OrFilter(
							new HasAttributeFilter("type", "text"), 
							new HasAttributeFilter("type", "hidden"))
							);
				if(inputList.size() > 0) {
					
					NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "myFORM"));
					String destinationPage = "UCCIndexDocs.cfm";
					if(formList.size() == 1) {
						destinationPage = ((FormTag)formList.elementAt(0)).getAttribute("action");
					}
					
					String linkString = akGenericRO.CreatePartialLink(TSConnectionURL.idPOST) + 
						"/ssd/recoff/sag/" + destinationPage + "?Next=More+Documents"; 
					for (int i = 0; i < inputList.size(); i++) {
						InputTag inputTag = (InputTag) inputList.elementAt(i);
						String name = inputTag.getAttribute("name");
						if(StringUtils.isNotEmpty(name)) {
							linkString += "&" + name + "=" + URLEncoder.encode(inputTag.getAttribute("value") == null ? "" : inputTag.getAttribute("value"), "UTF-8");
						}
					}
					outputTable.append("<tr><td align=\"center\" colspan=\"7\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
				}
				
			}
			
		} catch (Exception e) {
			akGenericRO.getLogger().error("Error while parsing intermediary results!" , e);
			e.printStackTrace();
		}
		
		return responses;
	}
	
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
*/

public class AKAnchorageAO {
		
	
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "").replaceAll("(?is)&amp;", "&")
								.replaceAll("(?is)<th\\b", "<td").replaceAll("(?is)</th\\b", "</td");
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null); 
					
			String pid = "";
			pid = StringUtils.extractParameter(detailsHtml, "(?i)PARCEL:\\s*(?:\\s*</span>\\s*<span[^>]*>)?([\\d-]+)\\s*");
			
			if (StringUtils.isNotEmpty(pid)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid.trim());
			}
			
			NodeList tableList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList.size() > 0){
				TableTag mainTable = (TableTag)tableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
				if (rows.length > 2){
					TableColumn[] cols = rows[3].getColumns();
					if (cols.length > 0){
						NodeList spanList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
						String allParts = spanList.elementAt(0).toHtml();
						allParts = allParts.replaceAll("(?is)\\A\\s*<SPAN[^>]*>([^<]+)<span[^>]*>Site.*", "$1");
						String[] rowsString = allParts.split("\r\n");
						int counter = 0;
						String owners = "", legal = "";
						for (String everyRow : rowsString){
							String[] itemsPerRow = everyRow.split("\\s{4,}");
							owners += itemsPerRow[0] + "###";
							if (itemsPerRow.length == 2){
								if (counter == 0){
									if (itemsPerRow[1].trim().matches("T\\s*\\d+[A-Z]?\\s+R\\s*\\d+[A-Z]?\\s+S(EC)?\\s*\\d+")){
										//051-081-11-000
										m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), StringUtils.extractParameter(itemsPerRow[1], "T([^\\s]+)"));
										m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), StringUtils.extractParameter(itemsPerRow[1], "R([^\\s]+)"));
										m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), StringUtils.extractParameter(itemsPerRow[1], "S(?:EC)?\\s*(\\d+)"));
									} else {
										String subName = itemsPerRow[1];
										subName = subName.replaceAll("(?is)\\([^\\)]*\\)", "");
										m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subName.trim());
										if (itemsPerRow[1].matches(".*\\bCOND.*"))
											m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subName.trim());
									}
									legal += itemsPerRow[1] + " ";
								} else {
									if (!itemsPerRow[1].matches("\\A\\s*AK.*")){
										legal += itemsPerRow[1] + " ";
									}
								}
							}
							counter++;
						}
						m.put("tmpOwner", owners);
						m.put("tmpLegal", legal);
						
						Pattern siteAdd = Pattern.compile("(?is)<span[^>]*>\\s*Site\\s*</span>\\s*<span[^>]*>([^<]*)");
						Matcher mat = siteAdd.matcher(spanList.elementAt(0).toHtml());					
						if (mat.find()){
							m.put("tmpAddress", mat.group(1));
						}
						
						String stringForDeedPart = ""; 
						for (int j = 1; j < rows.length; j++){
							if (rows[j].toHtml().toLowerCase().contains("stateid")){
								stringForDeedPart = rows[j].toHtml();
								break;
							}
						}
						if (!stringForDeedPart.equals("")){
							String deedParts = "";
							Pattern deedPartsPat = Pattern.compile("(?is)(<span[^>]*>Stateid.*)<span[^>]*>\\s*GRW");
							mat = deedPartsPat.matcher(stringForDeedPart);					
							if (mat.find()){
								deedParts = mat.group(1);
								String[] spans = deedParts.split("(?is)<span[^>]*>");
								String instrNo = "", date = "", platBP = "";
								for (int i = 0; i < spans.length; i++){
									if (spans[i].toLowerCase().contains("stateid")){
										if (spans[i+1].trim().matches("\\d+\\s+\\d+")){//012-163-27-000
											instrNo = spans[i+1].trim().replaceAll("(\\d+)\\s+(\\d+)", "$1/$2");
											instrNo = instrNo.replaceAll("\\A\\s*0+/0+\\s*$", "");
										} else {
											instrNo += spans[i+1] + spans[i+2] + spans[i+3];
											instrNo = instrNo.replaceAll("(?is)</span>", "").trim();
											instrNo = instrNo.replaceAll("\\s*/\\s*", "/");
											instrNo = instrNo.replaceAll("\\A\\s*0+/0+\\s*$", "");
										}
									} else if (spans[i].toLowerCase().contains("date")){
										date += spans[i+1] + spans[i+2] + spans[i+3] + spans[i+4] + spans[i+5];
										date = date.replaceAll("(?is)</span>", "").trim();
										date = date.replaceAll("\\s*/\\s*", "/");
									}
									else if (spans[i].toLowerCase().contains("plat")){
										platBP += spans[i+1];
										platBP = platBP.replaceAll("(?is)</span>", "").trim();
										if (!platBP.contains("-")){//012-163-27-000
											platBP = platBP.replaceAll("\\A\\s*(\\d{2})", "$1-");
										}
										String[] pbp = platBP.split("-");
										if (pbp.length > 0){
											m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pbp[0].replaceAll("\\A0+", "").trim());
										}
										if (pbp.length > 1){
											m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pbp[1].replaceAll("\\A0+", "").trim());
										}
									}
								}
								
								if (StringUtils.isNotEmpty(instrNo)){
									List<List> body = new ArrayList<List>();
									List<String> line = new ArrayList<String>();
									
									String[] instrNoSplitted = instrNo.split("/");
									String[] dateSplitted = date.split("/");
									if (dateSplitted.length > 2 && instrNoSplitted.length > 1){
										if (instrNoSplitted[0].trim().endsWith(dateSplitted[2].trim())){
											line.add(instrNo);
											line.add("");
											line.add("");
											line.add(date);
										} else {
											line.add("");
											line.add(instrNoSplitted[0].replaceAll("\\A0+", "").trim());
											line.add(instrNoSplitted[1].replaceAll("\\A0+", "").trim());
											line.add(date);
										}
										body.add(line);
									}
									
									
									if (body != null){
										ResultTable rt = new ResultTable();
										String[] header = {"InstrumentNumber", "Book", "Page", "InstrumentDate" };
										rt = GenericFunctions2.createResultTable(body, header);
										m.put("SaleDataSet", rt);
									}
								}						}
						}
					}
				}
			}			
			try {
				parseAddressAKAnchorage(m, searchId);
				parseLegalAKAnchorage(m, searchId);
				partyNamesAKAnchorage(m, searchId);

			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	

	public static ResultMap parseIntermediaryRowAKAnchorage(TableRow row, long searchId, int miServerId) throws Exception {
		
		ResultMap resultMap = new ResultMap();
		//String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		if (TSServersFactory.isCountyTax(miServerId)){
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		} else if (TSServersFactory.isAssesor(miServerId)){
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
		}
		
		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		NodeList nList = null;
		for(TableColumn col : cols) {
			//System.out.println(col.toHtml());
			if (count < 5){
				switch (count) {
				case 0:
					nList = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if (nList != null){
						contents = ((LinkTag) nList.elementAt(0)).getLinkText();
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), contents.trim());
					}

					break;
				case 1:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();
						resultMap.put("tmpOwner", contents);
					}
					break;
				case 2:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();
						resultMap.put("tmpAddress", contents.trim());
					}
					break;
				case 3:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();

						String[] legalLines = contents.split("<br>");
						String legal = "";
						if (legalLines[0].trim().matches("T\\s*\\d+[A-Z]?\\s+R\\s*\\d+[A-Z]?\\s+S(EC)?\\s*\\d+")){
							//051-081-11-000
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), StringUtils.extractParameter(legalLines[0], "T([^\\s]+)"));
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), StringUtils.extractParameter(legalLines[0], "R([^\\s]+)"));
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), StringUtils.extractParameter(legalLines[0], "S(?:EC)?\\s*(\\d+)"));
						} else {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legalLines[0]);
							if (legalLines[0].matches(".*\\bCOND.*")){
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), legalLines[0]);
							}
						}
						if (legalLines.length > 1){
							for (int i = 1; i < legalLines.length; i++){
								legal += legalLines[i] + " ";
							}
						}
						
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.trim());
						resultMap.put("tmpLegal", legal.trim());
					}
					break;
				default:
					break;
				}
				count++;
					
			}
		}
		
		partyNamesIntermAKAnchorage(resultMap, searchId);
		parseAddressAKAnchorage(resultMap, searchId);
		parseLegalAKAnchorage(resultMap, searchId);
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesAKAnchorage(ResultMap m, long searchId) throws Exception {
		
		String owner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(owner))
			return;		
		
		String[] ownerRows = owner.split("###");
		String stringOwner = "";
		for (String row : ownerRows){
			if (row.trim().matches("\\d+(-[A-B])?\\s+.*")){
				break;
			} else if (row.toLowerCase().contains(" box")){
				break;
			} else {
				stringOwner += row + " ";
			}
		}
		
		stringOwner = stringOwner.replaceAll("(?is)<br\\s*>", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bWI?FE?\\b", " ");
		
		//006-053-42-000
		stringOwner = stringOwner.replaceAll("(?is)&\\s*([A-Z]+\\s+[A-Z]\\s*/\\s*TT)\\s*$", " @@@@@@ $1");
		//051-063-30-000
		stringOwner = stringOwner.replaceAll("&\\s*(\\w+)\\s*\\d+\\s*/\\s*\\d+\\s*&", "$1 @@@@@@");
		
		stringOwner = stringOwner.replaceAll("\\b\\d+\\s*/\\s*\\d+\\b", "");
		stringOwner = stringOwner.replaceAll("\\d+\\s*%", " @@@@@@ ");
		stringOwner = stringOwner.replaceAll("\\s*@@@@@@\\s*$", "");
		stringOwner = stringOwner.replaceAll("%", "& %");
		stringOwner = stringOwner.replaceAll("\\bATTN:?", "& ");
		stringOwner = stringOwner.replaceAll("(?is)/?\\s*((?:CO-?\\s*)?(TRU?STEES?|\\bTTE?E?S?|\\bPER\\s+REP))\\b", " $2 & ");
		stringOwner = stringOwner.replaceAll("(?is)\\bDECD", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMRS", "");
		stringOwner = stringOwner.replaceAll("(?i)(\\w+\\s+\\w+(?:\\s+\\w+)?)\\s*,\\s*(\\w+\\s+\\w+)", "$1 AND $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		
		stringOwner = stringOwner.replaceAll("(?is)\\b(TRUSTS?(?:\\s+THE|\\s+DECLARATION)?)\\b", "$1 &");
		stringOwner = stringOwner.replaceAll("(?is)\\bPO\\s+BOX.*", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bAKA\\b", " & ");
		
		//075-152-37-001 
		stringOwner = stringOwner.replaceAll("(?is)\\bDBA\\b", " @@@@@@ ");
		stringOwner = stringOwner.replaceAll("(?is)\\bC/O\\b", " @@@@@@ ");
		
		//002-103-52-000 
		stringOwner = stringOwner.replaceAll("(?is)(&.*?TRUSTS?(?:\\s+DECLARATION)?\\s*)&\\s*(\\w+)", "$1 @@@@@@ $2");
		//019-101-53-000
		stringOwner = stringOwner.replaceAll("(?is)(.*?TRUSTS?(?:\\s+DECLARATION)?\\s*)&\\s*(\\w+)", "$1 @@@@@@ $2");
		stringOwner = stringOwner.replaceAll("(?is)&\\s+%", "@@@@@@ %");
		stringOwner = stringOwner.replaceAll("(?is)(@@@@@@)\\s+@@@@@@", "$1");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		String ln = "";
		//boolean coOwner = false;
		 stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		String[] owners = stringOwner.split("&");
		if (stringOwner.matches("(?is)[^&]+&\\s*[A-Z-]+(\\s+[A-Z]{1,2})?\\s*$") || stringOwner.contains("@@@@@@")){
			owners = stringOwner.split("@@@@@@");
		}

		for (int i = 0; i < owners.length; i++) {
			owners[i] = owners[i].replaceAll("\\A\\s*&", "");
			names = StringFormats.parseNameNashville(owners[i]);

			if (owners[i].trim().startsWith("%")){
				owners[i] = owners[i].replaceAll("\\A\\s*%", "");
				names = StringFormats.parseNameDesotoRO(owners[i]);
			}
			if (i > 0 && StringUtils.isNotEmpty(names[2].trim()) && LastNameUtils.isNotLastName(names[2]) && NameUtils.isNotCompany(names[2]) 
						&& StringUtils.isEmpty(names[1]) && NameUtils.isNotCompany(ln)){
				names[1] = names[0];
				names[0] = names[2];
				names[2] = ln;
			} else if (i > 0 && StringUtils.isNotEmpty(names[2].trim()) && LastNameUtils.isNotLastName(names[2]) && NameUtils.isNotCompany(names[2]) 
								&& names[0].length() == 1 && names[1].length() == 1 && NameUtils.isNotCompany(ln)){
				//01223259000
				names[1] = names[0] + " " + names[1];
				names[0] = names[2];
				names[2] = ln;
			}
			if (names[3].equals("ANN") && names[5].equals("MARY")){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			} else if (names[2].equals("MARY")){
				String aux = names[2];
				names[2] = names[1];
				names[1] = names[0];
				names[0] = aux;
			}
			/*if (StringUtils.isNotEmpty(names[5]) && 
					LastNameUtils.isNotLastName(names[5]) && 
					NameUtils.isNotCompany(names[5])){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
				
				if (names[3].equals(names[5])){
					names[3] = names[4];
					names[4] = "";
				}
			} */
			//014-101-05-000 
			if (names[5].trim().equals("TRUSTEES")){
				names[1] = names[1] + " TRUSTEE";
				names[4] = names[4] + " TRUSTEE";
				names[5] = names[2];
			}
			if (names[3].trim().contains("TRUSTEES")){
				names[1] = names[1] + " TRUSTEE";
				names[3] = names[3].replaceAll("(?is)\\s*TRUSTEES\\b", "");
				String aux = names[3];
				names[3]= names[4];
				names[4] = aux;
				names[4] = (names[4] + " TRUSTEE").trim();
			}
			ln = names[2];
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
												NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		stringOwner = stringOwner.replaceAll("(?is)\\s*@@@@@@\\s*", " ");
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);
				
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesIntermAKAnchorage(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.replaceAll("(?is)<br\\s*>", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bWI?FE?\\b", " ");
		
		stringOwner = stringOwner.replaceAll("\\d+\\s*%", "");
		stringOwner = stringOwner.replaceAll("%", "& %");
		stringOwner = stringOwner.replaceAll("\\bATTN:?", "& ");		
		stringOwner = stringOwner.replaceAll("(?is)/?\\s*((?:CO-?\\s*)?(TRU?STEES?|\\bTTE?E?S?|\\bPER\\s+REP))\\b", " $2 & ");
		stringOwner = stringOwner.replaceAll("(?is)\\bDECD", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMRS", "");
		stringOwner = stringOwner.replaceAll("(?i)(\\w+\\s+\\w+(?:\\s+\\w+)?)\\s*,\\s*(\\w+\\s+\\w+)", "$1 AND $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		stringOwner = stringOwner.replaceAll("(?is)\\b(TRUSTS?(?:\\s+THE)?)\\b", "$1 &");
		stringOwner = stringOwner.replaceAll("(?is)\\bPO\\s+BOX.*", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bAKA\\b", " & ");
		
		//075-152-37-001 
		stringOwner = stringOwner.replaceAll("(?is)\\bDBA\\b", " @@@@@@ ");
		stringOwner = stringOwner.replaceAll("(?is)\\bC/O\\b", " @@@@@@ ");
		
		stringOwner = stringOwner.replaceAll("(?is)(&.*?TRUSTS?\\s*)&", "$1 @@@@@@");
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		String ln = "";
		//boolean coOwner = false;

		String[] owners = stringOwner.split("&");
		if (stringOwner.matches("(?is)[^&]+&\\s*[A-Z-]+(\\s+[A-Z])?\\s*$") || stringOwner.contains("@@@@@@")){
			owners = stringOwner.split("@@@@@@");
		}

		for (int i = 0; i < owners.length; i++) {
			names = StringFormats.parseNameNashville(owners[i]);

			if (owners[i].trim().startsWith("%")){
				owners[i] = owners[i].replaceAll("\\A\\s*%", "");
				names = StringFormats.parseNameDesotoRO(owners[i]);
			}
			if (i > 0 && StringUtils.isNotEmpty(names[2].trim()) && LastNameUtils.isNotLastName(names[2]) && NameUtils.isNotCompany(names[2])){
				names[1] = names[0];
				names[0] = names[2];
				names[2] = ln;
			}
			if (StringUtils.isNotEmpty(names[5]) && 
					LastNameUtils.isNotLastName(names[5]) && 
					NameUtils.isNotCompany(names[5])){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			} 
			//014-101-05-000 
			if (names[5].trim().equals("TRUSTEES")){
				names[1] = names[1] + " TRUSTEE";
				names[4] = names[4] + " TRUSTEE";
				names[5] = names[2];
			}
			if (names[3].trim().contains("TRUSTEES")){
				names[1] = names[1] + " TRUSTEE";
				names[3] = names[3].replaceAll("(?is)\\s*TRUSTEES\\b", "");
				String aux = names[3];
				names[3]= names[4];
				names[4] = aux;
				names[4] = (names[4] + " TRUSTEE").trim();
			}
			ln = names[2];
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractNameType(names);
			otherType = GenericFunctions.extractNameOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
												NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner.replaceAll("@@@@@@", " & "));
		
	}
	
	
	public static void parseLegalAKAnchorage(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("tmpLegal");
				
		if (StringUtils.isEmpty(legal))
			return;
		
		m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
		
		legal = legal.replaceAll("(?is)\\s+THRU\\s+", "-");
		
		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)[\\d\\sX]+\\s*OF\\s+", " ");
		legal = legal.replaceAll("(?is)\\s+(TR(ACT)?)\\b", " , $1");
		legal = legal.replaceAll("(?is)\\s+(PH(ASE)?)\\b", " , $1");
		//legal = legal.replaceAll("(?is)\\s+[NSEW]\\s*/\\s*\\d+\\s*OF\\s*(\\d+)", " L $1");
		//legal = legal.replaceAll("(?is)(\\w),(\\w)\\s+(\\w)", "$1,$2,$3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
			
		if (legal.trim().matches(".*T\\s*\\d+[A-Z]?\\s+R\\s*\\d+[A-Z]?\\s+S(EC)?\\s*\\d+.*")){
			//051-081-11-000
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), StringUtils.extractParameter(legal, "T([^\\s]+)"));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), StringUtils.extractParameter(legal, "R([^\\s]+)"));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), StringUtils.extractParameter(legal, "S(?:EC)?\\s*(\\d+)"));
		}
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d,]+[A-Z]?(?:\\s*,\\s*[\\d,]+[A-Z])?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").replaceAll("\\b([NSWE]{1,2}\\s+)?PT(\\s+LOT)?\\b", "").replaceAll("\\bLOTS?\\b", "")
					.replaceAll("\\bLEASEHOLD\\b", "").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?)\\s*([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
			
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s*#\\s*([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

	
		// extract section from legal description
		p = Pattern.compile("(?is)\\bTR(?:ACT)?\\s+(\\d+[A-Z]?|[A-Z][\\d-]+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(1));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
				
			
	}
	
	public static void parseAddressAKAnchorage(ResultMap m, long searchId) throws Exception {
		
		String address = (String) m.get("tmpAddress");
		
		if (StringUtils.isEmpty(address))
			return;
				
		if (address.trim().matches("0+"))
			return;
		
		address = address.replaceAll("&nbsp;", " ");
		address = address.replaceAll("&amp;", "&");
		
		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address.trim());
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()));
		
	}
	
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class MOPlatteRO {
	
	private static final Pattern BOOK_PAGE_PAT = Pattern.compile("(?is)Book\\s*:\\s*(\\d+)\\s*Page\\s*:\\s*(\\d+)");
	
	
	public static void parseAndFillResultMap(ServerResponse response, String rsResponse, ResultMap m, long searchId) {
		
		try {
			m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
			
			rsResponse = rsResponse.replaceAll("&nbsp;", " ");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);
			
			String instrumentNo = HtmlParser3.getValueFromNextCell(mainList, "Document No.", "", false).trim();
			m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNo.trim());
			//m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), instrumentNo.trim());
			String docType = HtmlParser3.getValueFromAbsoluteCell(-1, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
			//docType = docType.replaceAll("(?is)[^-]+-", "").trim();
			docType = docType.replaceAll("(?is)\\s*-\\s*Unknown\\b", " ").replaceAll("(?is)\\s+", " ").trim();
			m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
			String instrDate = HtmlParser3.getValueFromNextCell(mainList, "Dated date", "", false).trim();
			instrDate = instrDate.replaceAll("(?is)([\\d/]+).*", "$1");
			m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate.trim());
			String recordedDate = HtmlParser3.getValueFromNextCell(mainList, "Recording Date", "", false).trim();
			recordedDate = recordedDate.replaceAll("(?is)([\\d/]+).*", "$1");
			m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate.trim());
			
			String consAmount = HtmlParser3.getValueFromNextCell(mainList, "Referenced Amount", "", false).trim();//1993015558
			m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), consAmount.trim().replaceAll("[\\$,]+", ""));
			m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), consAmount.trim().replaceAll("[\\$,]+", ""));
			
			String book = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
			String page = HtmlParser3.getValueFromAbsoluteCell(2, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
			
			m.put(SaleDataSetKey.BOOK.getKeyName(), book.trim());
			m.put(SaleDataSetKey.PAGE.getKeyName(), page.trim());
			
			String tableLegals = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Legal Description(s)"), "", true).trim();
			List<List<String>> listLegals = HtmlParser3.getTableAsList(tableLegals, true);
			String legal = "";
			for (List<String> list : listLegals){
				for (String item : list){
					if (StringUtils.isNotEmpty(item)){
							legal += "@@" + item.trim(); 
					}
				}
			}
			if (StringUtils.isNotEmpty(legal)){
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.replaceAll("(?is)\\A@@", "").trim());
			}
			
			String refsdByThisDoc = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Referenced By This Document"), "", true).trim();
			String refsToThisDoc = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "References To This Document"), "", true).trim();
			
			@SuppressWarnings("rawtypes")
			List<List> bodyCR = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			
			if (StringUtils.isNotEmpty(refsdByThisDoc)){				
				String[] crBP = refsdByThisDoc.split("(?is)\\s*</?br\\s   * /?>\\s*");
				for (String bp : crBP){
					Matcher mat = BOOK_PAGE_PAT.matcher(bp);
					if (mat.find()){
						line = new ArrayList<String>();
						line.add(mat.group(1));
						line.add(mat.group(2));
						line.add("");
						bodyCR.add(line);
					}
				}
			}
			if (StringUtils.isNotEmpty(refsToThisDoc)){				
				String[] crBP = refsToThisDoc.split("(?is)\\s*</?br\\s*/?>\\s*");
				for (String bp : crBP){
					Matcher mat = BOOK_PAGE_PAT.matcher(bp);
					if (mat.find()){
						line = new ArrayList<String>();
						line.add(mat.group(1));
						line.add(mat.group(2));
						line.add("");
						bodyCR.add(line);
					}
				}
			}
			String comments = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Comments"), "", true).trim();
			if (StringUtils.isNotEmpty(comments)){
				m.put("tmpComments", comments);
				Pattern bookPageFromCommPat = Pattern.compile("(?is)\\bBK\\s*(\\d+)\\s*PG\\s*(\\d+)");
				Matcher mat = bookPageFromCommPat.matcher(comments);
				while (mat.find()){
					boolean isAlready = false;
					line = new ArrayList<String>();
					line.add(mat.group(1));
					line.add(mat.group(2));
					line.add("");
					if (bodyCR.isEmpty()){
						bodyCR.add(line);
					} else {
						for (List<String> lst : bodyCR){
							if (lst.equals(line)){
								isAlready = true;
								break;
							}
						}
						if (!isAlready){
							bodyCR.add(line);
						}
					}
				}
			}
			
			if (!bodyCR.isEmpty()){
				String[] header = { "Book", "Page", "InstrumentNumber" };
				ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
				m.put("CrossRefSet", rt);
			}
			
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes, type, otherType;
			@SuppressWarnings("rawtypes")
			ArrayList<List> grantor = new ArrayList<List>();
			@SuppressWarnings("rawtypes")
			ArrayList<List> grantee = new ArrayList<List>();
			
			String grantorString = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Grantor(s)"), "", true).trim();
			
			if (StringUtils.isNotEmpty(grantorString)){
				grantorString = grantorString.replaceAll("(?is)\\bDeceased\\b", "").trim();
				grantorString = grantorString.replaceAll("(?is)\\bAS\\s+NOMINEE\\b", "").trim();
				grantorString = grantorString.replaceAll("(?is)\\bDBA\\b", "      ").trim();
				grantorString = grantorString.replaceAll("(?is)\\bC/O\\b", "      ").trim();
				grantorString = grantorString.replaceAll("(?is)&nbsp;", " ").trim();
				grantorString = grantorString.replaceAll("&amp;", "&");
				grantorString = grantorString.replaceAll("(?is)\\bLINEAL DESCENDANTS? PER STIRPES?\\b", "&");
				
				String[] gtors = grantorString.split("(?is)\\s*</?br\\s*/?>\\s*");
				
				for (int i = 0; i < gtors.length; i++){
					gtors[i] = gtors[i].trim();
					
					gtors[i]=StringFormats.cleanNameMERS(gtors[i]);
					names = StringFormats.parseNameNashville(gtors[i], true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gtors[i], names, suffixes[0],
													suffixes[1], type, otherType,
													NameUtils.isCompany(names[2]),
													NameUtils.isCompany(names[5]), grantor);
				}
				grantorString = grantorString.replaceAll("(?is)\\s*</?br\\s*/?>\\s*$", "").replaceAll("(?is)\\s*</?br\\s*/?>\\s*", " / ");	
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantorString);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			}
				
			String granteeString = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Grantee(s)"), "", true).trim();
			
			if (StringUtils.isNotEmpty(granteeString)){
				granteeString = granteeString.replaceAll("(?is)\\bDeceased\\b", "").trim();
				granteeString = granteeString.replaceAll("(?is)\\bAS\\s+NOMINEE\\b", "").trim();
				granteeString = granteeString.replaceAll("(?is)\\bDBA\\b", "      ").trim();
				granteeString = granteeString.replaceAll("(?is)\\bC/O\\b", "      ").trim();
				granteeString = granteeString.replaceAll("(?is)&nbsp;", " ").trim();
				granteeString = granteeString.replaceAll("&amp;", "&");
				granteeString = granteeString.replaceAll("(?is)\\bLINEAL DESCENDANTS? PER STIRPES?\\b", "&");
				
				String[] gtee = granteeString.split("(?is)\\s*</?br\\s*/?>\\s*");
				
				for (int i = 0; i < gtee.length; i++){
					gtee[i] = gtee[i].trim();

					names = StringFormats.parseNameNashville(gtee[i], true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gtee[i], names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
				
				granteeString = granteeString.replaceAll("(?is)\\s*</?br\\s*/?>\\s*$", "").replaceAll("(?is)\\s*</?br\\s*/?>\\s*", " / ");
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), granteeString);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			}
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
			GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
			
			try {
				parseLegalMOPlatteRO(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			@SuppressWarnings("unchecked")
			Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) m.get("PropertyIdentificationSet");
			if (pisVector != null && !pisVector.isEmpty()){
				for (PropertyIdentificationSet everyPis : pisVector){
					response.getParsedResponse().addPropertyIdentificationSet(everyPis);
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseNameInterMOPlatteRO(ResultMap m, long searchId) throws Exception{
		
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes, type, otherType;
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String tmpPartyGtor = (String)m.get("tmpPartyGtor");
			if (StringUtils.isNotEmpty(tmpPartyGtor)){
				tmpPartyGtor = tmpPartyGtor.replaceAll("\\sDBA\\s+", " / ");
				String[] gtors = tmpPartyGtor.split("/");
				for (String grantorName : gtors){
					grantorName = grantorName.replaceAll("\\bDECEASED\\b", "");
					
					names = StringFormats.parseNameNashville(grantorName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(grantorName, names, suffixes[0],
													suffixes[1], type, otherType,
													NameUtils.isCompany(names[2]),
													NameUtils.isCompany(names[5]), grantor);
				}
				
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			}
			
			String tmpPartyGtee = (String)m.get("tmpPartyGtee");
			if (StringUtils.isNotEmpty(tmpPartyGtee)){
				tmpPartyGtee = tmpPartyGtee.replaceAll("\\sDBA\\s+", " / ");
				String[] gtee = tmpPartyGtee.split("/");
				for (String granteeName : gtee){
					granteeName = granteeName.replaceAll("\\bDECEASED\\b", "");
					
					names = StringFormats.parseNameNashville(granteeName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(granteeName, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
				
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
				
			}
			
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
			GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
		
	}
	
	
	public static void parseLegalMOPlatteRO(ResultMap m, long searchId) throws Exception{
		
		String legal = (String)m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isNotEmpty(legal)){
			
			legal = GenericFunctions.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
			
			Vector<PropertyIdentificationSet> pisVector = new Vector<PropertyIdentificationSet>();

			legal = legal.replaceAll("(?is)@@(SUR)\\b", " $1");
			String legalTemp = legal;
			String[] legalRows = legal.split("@@");

			for (String legalRow : legalRows){
				PropertyIdentificationSet pis = new PropertyIdentificationSet();
				String subdivName = "", lot = "";//, blk = "", bg = "", tract = "";

				Pattern p = Pattern.compile("(?is)\\b(STR)\\s+(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)(?:\\s*/\\s*([NSWE]+)?\\s*/\\s*([NSWE]+))?\\b");
				Matcher ma = p.matcher(legalRow);
				if (ma.find()) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), ma.group(2).trim());
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ma.group(3).trim());
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), ma.group(4).trim());
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					
					if (ma.group(5) != null){//STR 11-51-34 /SW/SE
						pis.setAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName(), ma.group(5).trim());
						if (!containsPIS(pisVector, pis, true)){
							pisVector.add(pis);
						}
					}
					
					if (ma.group(6) != null){//STR 11-51-34 /SW/SE || STR 21-51-34 //SW
						pis = new PropertyIdentificationSet();
						pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), ma.group(2).trim());
						pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ma.group(3).trim());
						pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), ma.group(4).trim());
						pis.setAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName(), ma.group(6).trim());
						if (!containsPIS(pisVector, pis, true)){
							pisVector.add(pis);
						}
						break;
					} else{
						if (!containsPIS(pisVector, pis, true)){
							pisVector.add(pis);
							continue;
						}
					}
				}
				
				p = Pattern.compile("(?is)\\b(LTS?)\\s+([\\d-]+[A-Z]?|[A-Z])\\b");
				ma = p.matcher(legalRow);
				while (ma.find()) {
					lot = lot + " " + ma.group(2).trim();
					legalRow = legalRow.replaceFirst(ma.group(0), ma.group(1) + " ");
				}
				
				if (lot.trim().length() != 0) {
					lot = LegalDescription.cleanValues(lot, false, true);
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot.trim());
				}
				
				p = Pattern.compile("(?is)\\b(BL)\\s+(\\d+[A-Z]?|[A-Z])\\b");
				ma = p.matcher(legalRow);
				if (ma.find()) {
					legalRow = legalRow.replaceFirst(ma.group(0), ma.group(1) + " ");
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), ma.group(2).trim());
				}
				
				p = Pattern.compile("(?is)\\b(BG)\\s+(\\d+[A-Z]?)\\b");
				ma = p.matcher(legalRow);
				if (ma.find()) {
					legalRow = legalRow.replaceFirst(ma.group(0), ma.group(1) + " ");
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), ma.group(2).trim());
				}
				
				p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+(\\d+[A-Z]?|[A-Z][\\d-]?)\\b");
				ma = p.matcher(legalRow);
				if (ma.find()) {
					legalRow = legalRow.replaceFirst(ma.group(0), ma.group(1) + " ");
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), ma.group(2).trim());
				}
				
				p = Pattern.compile("(?is)\\b(UT)\\s+([\\d-]+[A-Z]?|[A-Z])\\b");
				ma = p.matcher(legalRow);
				if (ma.find()) {
					legalRow = legalRow.replaceFirst(ma.group(0), ma.group(1) + " ");
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), ma.group(2).trim());
				}
				
				p = Pattern.compile("(?is)\\b(B[L|G])\\s+(.+)\\b");
				ma = p.matcher(legalRow);
				if (ma.find()) {
					subdivName = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(LTS?|TR)\\s+(.+)\\b");
					ma = p.matcher(legalRow);
					if (ma.find()) {
						subdivName = ma.group(2);
					} else {
						if (!legalRow.trim().matches("(?is)\\ASTR\\s+.*")){
							subdivName = legalRow;
						}
					}
				}
				if (!"".equals(subdivName.trim())){
					subdivName = subdivName.replaceAll("(?is)\\bCERT(IFICATE)?\\s+OF\\s+SUR.*", "$1");
					subdivName = subdivName.replaceAll("(?is)\\bBLOCK.*", "");
					subdivName = subdivName.replaceAll("(?is)\\bTRACT\\b", "");
					subdivName = subdivName.replaceAll("(?is)\\bDOC\\b.*", "");
					subdivName = subdivName.replaceAll("(?is)\\s+", " ");
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdivName.trim());
				}
			
				p = Pattern.compile("(?is)\\bSUR\\s+BK\\s*/\\s*PG\\s+([^/]+)\\s*/\\s*([\\d[A-Z]-]+)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), ma.group(1).trim());
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), ma.group(2).trim());
				}
				
				if (!containsPIS(pisVector, pis, false)){
					pisVector.add(pis);
				}
			}
			if (!pisVector.isEmpty()){
				m.put("PropertyIdentificationSet", pisVector);
			}
			legal = legal.replaceAll("(?is)@@", " / ");
			m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
		}

	}
	
	public static boolean containsPIS(Vector<PropertyIdentificationSet> pisVector, PropertyIdentificationSet pis, boolean separatelySTR){
		if (pisVector == null){
			pisVector = new Vector<PropertyIdentificationSet>();
		}
		if (pis == null || pis.isEmpty()){
			return false;
		}
		
		for (PropertyIdentificationSet everyPis : pisVector){
			if (separatelySTR){
				if (StringUtils.isNotEmpty(everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName())) 
						&& StringUtils.isNotEmpty(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName()))){
					if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName())
							.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName()))){
						if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName())
								.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName()))){
							if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName())
									.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName()))){
								if (StringUtils.isNotEmpty(everyPis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName())) 
										&& StringUtils.isNotEmpty(pis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName()))){
									if (everyPis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName())
											.equals(pis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName()))){
											return true;
									} else {
										return false;
									}
								} else {
									return true;
								}
							} else {
								return false;
							}
						} else {
							return false;
						}
					} else {
						return false;
					}
				}
			} else {
				if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName())
						.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName()))){
					if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName())
								.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName()))){
						if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName())
									.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName()))){
							return true;
						} else {
							return false;
						}
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}
    
}

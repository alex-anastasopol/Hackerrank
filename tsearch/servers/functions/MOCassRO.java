package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

/**
 * @author mihaib
*/

public class MOCassRO {
	
	private static final Pattern LEGAL_TAB_PAT = Pattern.compile("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*LEGAL DESC\\s*\\.\\s+\\d+.*?<tr[^>]*>\\s*<td[^>]*>\\s*Sleeve\\s*:</td>\\s*<td[^>]*>[^<]*</td>\\s*</tr>");

	public static ResultMap parseIntermediaryRow(ResultMap m, TableRow row, long searchId) {
		
		m.put("OtherInformationSet.SrcType", "RO");
		
		TableColumn[] cols = row.getColumns();
		///String name = cols[1].getStringText().replaceAll("</?font[^>]*>", "").replaceAll("&nbsp;", " ");
		//if (StringUtils.isNotEmpty(name)){
			//m.put("tmpGrantor", name);
		//}
		String instrdate = cols[2].getStringText().replaceAll("</?font[^>]*>", "").replaceAll("&nbsp;", " ");
		m.put("SaleDataSet.RecordedDate", instrdate.trim());
		String docType = cols[3].getStringText().replaceAll("</?font[^>]*>", "").replaceAll("&nbsp;", " ");
		m.put("SaleDataSet.DocumentType", docType.trim());
		String book = cols[4].getStringText().replaceAll("</?font[^>]*>", "").replaceAll("&nbsp;", " ");
		m.put("SaleDataSet.Book", book.trim());
		String page = cols[5].getStringText().replaceAll("</?font[^>]*>", "").replaceAll("&nbsp;", " ");
		m.put("SaleDataSet.Page", page.trim());
		String legal = cols[6].getStringText().replaceAll("</?font[^>]*>", "").replaceAll("&nbsp;", " ");
		m.put("PropertyIdentificationSet.PropertyDescription", legal.trim());
		String instrumentNo = cols[7].getStringText().replaceAll("</?font[^>]*>", "").replaceAll("&nbsp;", " ");
		m.put("SaleDataSet.InstrumentNumber", instrumentNo.trim());
		
		try {
			//parseNameInterMOCassRO(m, searchId);
			parseLegalInterMOCassRO(m, searchId);
			m.removeTempDef();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return m;
	}
	
	@SuppressWarnings("unchecked")
	public static void parseAndFillResultMap(String rsResponse, ResultMap m, long searchId) {
		
		try {
			m.put("OtherInformationSet.SrcType","RO");
			
			rsResponse = rsResponse.replaceAll("&nbsp;", " ");
			org.w3c.dom.Document finalDoc = Tidy.tidyParse(rsResponse);
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);
			
			String instrumentNo = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "File No"),"").trim();
			m.put("SaleDataSet.InstrumentNumber", instrumentNo.trim());
			String docType = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "Type"),"").trim();
			m.put("SaleDataSet.DocumentType", docType.trim());
			String instrDate = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "Date/Time"),"").trim();
			instrDate = instrDate.replaceAll("(?is)([\\d/]+).*", "$1");
			m.put("SaleDataSet.RecordedDate", instrDate.trim());
			String consAmount = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "Consideration"),"").trim();
			m.put("SaleDataSet.ConsiderationAmount", consAmount.trim().replaceAll("[\\$,]+", ""));
			m.put("SaleDataSet.MortgageAmount", consAmount.trim().replaceAll("[\\$,]+", ""));
			String bookPage = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "Book/Page"),"").trim();
			String[] bp = bookPage.split("/");
			if (bp.length == 2){
				m.put("SaleDataSet.Book", bp[0].trim());
				m.put("SaleDataSet.Page", bp[1].trim());
			}
			List<List> bodyCR = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			String crossRefTable =  HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(finalDoc, "crossRef" , "table"));
			if (crossRefTable.contains("Linked")){
				List<List<String>> list = HtmlParser3.getTableAsList(crossRefTable, true);
				for (List<String> lst : list){
					String cross = lst.get(1).replaceAll("(?is)[A-Z\\d]+\\s+([^\\s-]+)\\s+-\\s*", "$1").trim();
					line = new ArrayList<String>();
					String[] crBP = cross.split("/");
					if (crBP.length == 2){
						line.add(crBP[0]);
						line.add(crBP[1]);
						line.add("");
					} else {
						line.add("");
						line.add("");
						line.add(cross);
					}
					bodyCR.add(line);
				}
				if (!bodyCR.isEmpty()){
					String[] header = { "Book", "Page", "InstrumentNumber" };
					ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
					m.put("CrossRefSet", rt);
				}
			}

			Vector pisVector = new Vector();
			String block = "", subdiv = "", lot = "", sec = "", twp = "", rng = "", tract = "";
			PropertyIdentificationSet pis = new PropertyIdentificationSet();
			boolean needToWrite = false;
			Matcher matcher = LEGAL_TAB_PAT.matcher(rsResponse);
			while (matcher.find()){
				org.htmlparser.Parser legalParser = org.htmlparser.Parser.createParser(matcher.group(0), null);
				NodeList legalList = legalParser.parse(null);
				String sub = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(legalList, "Subdiv"),"").trim();
				boolean emptySubdiv = StringUtils.isEmpty(sub);
				String blk = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(legalList, "Block"),"").trim();
				String lt = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(legalList, "Lot"),"").trim();
				String sect = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(legalList, "Section Num"),"").trim();
				String tp = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(legalList, "Township"),"").trim();
				String rg = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(legalList, "Range"),"").trim();
				String legalDesc = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(legalList, "Legal Desc"),"").trim();
				tract = "";
				
				if (!sub.equals(subdiv) || !blk.equals(block) || emptySubdiv){
					if (StringUtils.isNotEmpty(legalDesc)){
						legalDesc = GenericFunctions.replaceNumbers(legalDesc);
						String[] exceptionTokens = { "I", "M", "C", "L", "D" };
						legalDesc = Roman.normalizeRomanNumbersExceptTokens(legalDesc, exceptionTokens); // convert roman numbers
						
						legalDesc = legalDesc.replaceAll("\\s+AND\\s+(\\d)", "&$1");
						Pattern p = Pattern.compile("(?is)\\bLO?TS?\\s*([\\d,&]+)\\b");
						Matcher mat = p.matcher(legalDesc);
						if (mat.find()){
							lt = mat.group(1);
							legalDesc = legalDesc.replaceAll(mat.group(0), "");
						}
						p = Pattern.compile("(?is)\\bBLKS?\\s*([\\d,&]+)\\b");
						mat = p.matcher(legalDesc);
						if (mat.find()){
							blk = mat.group(1);
							legalDesc = legalDesc.replaceAll(mat.group(0), "");
						}
						p = Pattern.compile("(?is)\\bTRACTS?\\s+([\\d,&\\s]+|[A-Z\\d-]+)\\b");
						mat = p.matcher(legalDesc);
						if (mat.find()){
							tract = mat.group(1);
							legalDesc = legalDesc.replaceAll(mat.group(0), "");
						}
						if (emptySubdiv){
							//sub = legalDesc.replaceAll("\\b(.*?)\\s+(ADDITION\\s+)?(TO|LO?TS?|BLKS?|CORRECTED)\\b?.*", "$1").replaceAll(",", "");
						}
					}
					if (needToWrite){
						if (lot.length() != 0) {
							lot = LegalDescription.cleanValues(lot, false, true);
							pis.setAtribute("SubdivisionLotNumber", lot);
						}
						pis.setAtribute("SubdivisionSection", sec.trim());
						pis.setAtribute("SubdivisionTownship", twp.trim());
						pis.setAtribute("SubdivisionRange", rng.trim());
						needToWrite = false;
						pisVector.add(pis);
						
					} 
					if (!needToWrite) {
						pis = new PropertyIdentificationSet();
						lot = "";
						pis.setAtribute("SubdivisionBlock", blk.trim());
						pis.setAtribute("SubdivisionName", sub.replaceAll("\\b(ADDITION\\s+)?(TO|LTS|BLKS|CORRECTED)\\b?.*", "").trim().replaceAll(",", ""));
						pis.setAtribute("SubdivisionTract", tract.trim());
						block = blk;
						subdiv = sub;
						needToWrite = true;
					}
				}
				lot += " " + lt;
				sec = sect;
				twp = tp;
				rng = rg;
			}
			
			if (needToWrite){
				if (lot.length() != 0) {
					lot = LegalDescription.cleanValues(lot, false, true);
					pis.setAtribute("SubdivisionLotNumber", lot);
				}
				pis.setAtribute("SubdivisionSection", sec.trim());
				pis.setAtribute("SubdivisionTownship", twp.trim());
				pis.setAtribute("SubdivisionRange", rng.trim());
				pis.setAtribute("SubdivisionTract", tract.trim());
				pisVector.add(pis);
			} 
			
			
			m.put("PropertyIdentificationSet", pisVector);
			
			//
			/*NodeList lots = HtmlParser3.findNodeList(mainList, "Lot");
			String lot = "";
			for (int n = 0; n < lots.size(); n++) {
				NodeList nodLot = new NodeList(lots.elementAt(n)); 
				lot += " " + HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nodLot, "Lot"),"").trim();
			}
			if (lot.length() != 0) {
				lot = LegalDescription.cleanValues(lot, false, true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			}*/
			
						
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes, type, otherType;
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String grantorString = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "Grantors"),"").trim();
			
			if (StringUtils.isNotEmpty(grantorString)){
				grantorString = grantorString.replaceAll("(?is)\\bDeceased\\b", "").trim();
				grantorString = grantorString.replaceAll("(?is)\\bAS\\s+NOMINEE\\b", "").trim();
				grantorString = grantorString.replaceAll("(?is)\\bDBA\\b", "      ").trim();
				grantorString = grantorString.replaceAll("(?is)\\bC/O\\b", "      ").trim();
				grantorString = grantorString.replaceAll("(?is)&nbsp;", " ").trim();
				grantorString = grantorString.replaceAll("&amp;", "&");
				String[] gtors = grantorString.split("\\s{5,}");
				grantorString = grantorString.replaceAll("\\s{5,}", " / ");
				for (int i = 0; i < gtors.length; i++){
					gtors[i] = gtors[i].replaceAll("\\b(FKA|AKA).*", ""); 
					gtors[i] = gtors[i].replaceAll("(?is)\\b(CO\\s+)?TRUSTEE.*", "").trim();
					if (gtors[i].contains("TRUST")){
						gtors[i] = gtors[i].replaceAll(",", "");
					}
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
					
				m.put("SaleDataSet.Grantor", grantorString);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			}
				
			String granteeString = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "Grantees"),"").trim();
			
			if (StringUtils.isNotEmpty(granteeString)){
				granteeString = granteeString.replaceAll("(?is)\\bDeceased\\b", "").trim();
				granteeString = granteeString.replaceAll("(?is)\\bAS\\s+NOMINEE\\b", "").trim();
				granteeString = granteeString.replaceAll("(?is)\\bDBA\\b", "      ").trim();
				granteeString = granteeString.replaceAll("(?is)\\bC/O\\b", "      ").trim();
				granteeString = granteeString.replaceAll("(?is)&nbsp;", " ").trim();
				granteeString = granteeString.replaceAll("&amp;", "&");
				String[] gtee = granteeString.split("\\s{5,}");
				granteeString = granteeString.replaceAll("\\s{5,}", " / ");
				for (int i = 0; i < gtee.length; i++){
					gtee[i] = gtee[i].replaceAll("\\b(FKA|AKA).*", "");
					gtee[i] = gtee[i].replaceAll("(?is)\\b(CO\\s+)?TRUSTEE.*", "").trim();
					if (gtee[i].contains("TRUST")){
						gtee[i] = gtee[i].replaceAll(",", "");
					}
					gtee[i]=StringFormats.cleanNameMERS(gtee[i]);
					names = StringFormats.parseNameNashville(gtee[i], true);
					boolean containsAllLasts = false; // 150540
					for (List lst : grantee){
						if (gtee[i].contains(lst.get(3).toString())){
							containsAllLasts = true;
						} else {
							containsAllLasts = false;
							break;
						}
					}
					if (containsAllLasts && gtee[i].trim().matches("\\w+\\s+\\w+\\s*&\\s*\\w+")){
						String[] compNames = {"", "", "", "", "", ""};
						compNames[2] = gtee[i];
						names = compNames;

					}
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gtee[i], names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
				
				m.put("SaleDataSet.Grantee", granteeString);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			}
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
			GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
			
			try {
				
			}catch(Exception e) {
				e.printStackTrace();
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static void parseNameInterMOCassRO(ResultMap m, long searchId) throws Exception{
		
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
				
				m.put("SaleDataSet.Grantor", tmpPartyGtor);
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
				
				m.put("SaleDataSet.Grantee", tmpPartyGtee);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
				
			}
			
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
			GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
		
	}
	public static void parseLegalInterMOCassRO(ResultMap m, long searchId) throws Exception{
		
		String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isNotEmpty(legal)){
			legal = legal.replaceAll("\\b(.*?)\\s+(ADDITION\\s+)?(TO|LO?TS?|BLKS?|CORRECTED)\\b?.*", "$1").replaceAll(",", "");
			m.put("PropertyIdentificationSet.SubdivisionName", legal);
		}
				
	
}
}

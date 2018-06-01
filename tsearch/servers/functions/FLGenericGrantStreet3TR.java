/**
 * 
 */
package ro.cst.tsearch.servers.functions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.country.Countries;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;
/**
 * @author vladb
 *
 * @new modifications by Olivia
 * 
 * ADD here the new county implemented with this Generic
 *
 * for Alachua, Broward, Charlotte, Citrus, Indian River, Lake, Miami-Dade, 
 * Monroe, Okaloosa, Osceola, Pinellas, St Lucie, Sumter, Volusia -like sites   
 *      
 */

public class FLGenericGrantStreet3TR {

	@SuppressWarnings("rawtypes")
	public static void parseNamesIntermediary(ResultMap resultMap, long searchId) throws Exception {
	     
		   String owner = (String) resultMap.get("tmpOwner");
		   
		   if (StringUtils.isEmpty(owner))
			   return;
		   
		   owner = owner.replaceAll("(?is)</?br/?>", "\n");
		   owner = owner.replaceAll("(?is)\n\\s*CO[-\\s+]+(TRUSTEES?)\\s*\n", " $1\n");
		   owner = owner.replaceAll("(?is)-\n\\s*(\\w+)\\s*\n", "-$1\n");
		   
		   //String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		   int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
		   
		   
		   String[] ownerRows = owner.split("\n");
		   StringBuffer stringOwnerBuff = new StringBuffer();
		   for (String row : ownerRows){
			   if (row.trim().matches("\\d+\\s+.*")) {
				  if (NameUtils.isCompany(row.trim()) && row.trim().matches("(?is)[\\d\\s\\w]+\\b(LLC|L.L.C.|L L C|INC|CORP|LTD)\\b")) {
					// see FL Miami-Dade TR -> AccountNo 02-4203-340-1400: 1105 OCEAN C II LLC
					  stringOwnerBuff.append(row).append("\n");
				  } else {
					  break;  
				  }
			   } else if (row.toLowerCase().contains("box")){
				   break;
			   } else if (LastNameUtils.isNoNameOwner(row)) {
				   if (row.equalsIgnoreCase("the children's place") || 
					  (NameUtils.isCompany(row.trim()) && row.trim().matches("(?is)[\\d\\s\\w]+\\b(LLC|L.L.C.|L L C|INC|CORP|LTD|L.T.D.|L T D)\\b"))) {
					   //to keep "THE CHILDREN'S PLACE", otherwise it would have been eliminated
					   //when name is a company, it shouldn't be deleted also
					   stringOwnerBuff.append(row).append("\n");		
				   }
				   else break;												
			   } else {													
				   stringOwnerBuff.append(row).append("\n");
			   }
		   }
		   String stringOwner = stringOwnerBuff.toString();
		   stringOwner = stringOwner.replaceAll("\n$", "");
		   String[] nameLines = stringOwner.split("\n");

		   List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes, types, otherTypes;
		   StringBuffer nameOnServerBuff = new StringBuffer();
		   for (int i=0; i < nameLines.length; i++){
			   String ow = nameLines[i];
			   ow = clean(ow);
			   if (countyId == CountyConstants.FL_Volusia && i > 0) {
				   if (NameUtils.isNotCompany(ow)){
					   ow = ow.replaceAll("(?is)\\A\\s*(\\w+)\\s*&\\s*(.*)", "$2 & $1");
				   }
				   names = StringFormats.parseNameDesotoRO(ow, true);
			   } else if (countyId == CountyConstants.FL_Miami_Dade) {
				   if (ow.matches("(?is)[\\w\\s]+\\bTRS\\b")) {
					   ow = ow.replaceFirst("\\bTRS\\b", "TRUST");
				   	}
				   names = StringFormats.parseNameDesotoRO(ow, true);
			   }else if(countyId == CountyConstants.FL_Alachua){
				   ow = ro.cst.tsearch.servers.functions.FLAlachuaTR.cleanNameFLAlachuaTR(ow);
				   ow = ro.cst.tsearch.servers.functions.FLAlachuaTR.addAmpersandIfNecessary(ow);
				   names = StringFormats.parseNameNashville(ow, true);
			   } else {
				   names = StringFormats.parseNameNashville(ow, true);
			   }
			   
			   if (ow.trim().startsWith("%") || ow.trim().startsWith("ATTN ")){
				   ow = ow.replaceAll("%", "");
				   ow = ow.replaceAll("(?is)\\bATTN\\s*:?\\s*", "");
				   names = StringFormats.parseNameDesotoRO(ow, true);
			   }
			   types = GenericFunctions.extractAllNamesType(names);
			   otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			   suffixes = GenericFunctions.extractNameSuffixes(names);        
			   GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					   								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			   nameOnServerBuff.append("/").append(ow);
		   }
		   String nameOnServer = nameOnServerBuff.toString();
		   nameOnServer = nameOnServer.replaceFirst("/", "");
		   resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
		   GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		   
	   }
	
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, long searchId) {
		try {
			detailsHtml = detailsHtml.replaceAll("(?is)&amp;", "&").replaceAll("(?is)&nbsp;", " ");
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			//String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			boolean isPaidinInstallments = false;
			
			String ownerInfo = "";
			String address = "";
			String legal = "";
			String totalDue = "";
			
			String taxYear = "";
			Div divBillLabel = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "bill-label"), true)
									.elementAt(0);
			taxYear = divBillLabel.toPlainTextString().replaceAll("(?is)</?div[^>]*>", "");
			taxYear = taxYear.replaceAll("(?is).*?\\s*(\\d+)(?:\\s+annual bill|\\s+installment bill.*)?", "$1").trim();
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			if (divBillLabel.toPlainTextString().toLowerCase().contains("real estate")){
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
			}
			
			String currentInstallment = divBillLabel.toPlainTextString().replaceAll("(?is)</?div[^>]*>", "");
			currentInstallment = currentInstallment.replaceAll("(?is).*?\\s*\\d+(\\s+annual bill|\\s+installment bill\\s+#\\d+)", "$1");
			
			TableTag table1 = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "info_tb1"), true)
				.elementAt(0);
			//for these counties column 1 (Parcel Number) is ParcelID
			if (countyId == CountyConstants.FL_Citrus || countyId == CountyConstants.FL_Monroe || countyId == CountyConstants.FL_Pinellas)		
			{												
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), table1.getRow(1).getColumns()[1].toPlainTextString().replaceAll("[\\s/]", ""));
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), table1.getRow(1).getColumns()[1].toPlainTextString().replaceAll("[\\p{Punct}\\s]", ""));
			}
			else 
			{
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), table1.getRow(1).getColumns()[0].toPlainTextString().replaceAll("[\\s/]", ""));
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), table1.getRow(1).getColumns()[0].toPlainTextString().replaceAll("[\\p{Punct}\\s]", ""));
			}	
						
			TableTag table2 = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "info_tb2"), true)
				.elementAt(0);
			String info = table2.toPlainTextString();
			Matcher m = Pattern.compile("(?is)(?<=Owner).*").matcher(info);
			if(m.find()) {
				ownerInfo = m.group().replaceAll("(?is)Situs address.*", "").trim();
				if(countyId == CountyConstants.FL_Citrus) {
//					resultMap.put("tmpOwnerAddress", ownerInfo);
				} else if(countyId == CountyConstants.FL_Charlotte) {
					resultMap.put("tmpOwnerAddress", ownerInfo);
				} else if(countyId == CountyConstants.FL_Pinellas || countyId == CountyConstants.FL_Sumter) {
					resultMap.put("tmpOwnerNameAddr", ownerInfo);
				} else if(countyId == CountyConstants.FL_Alachua) {
					resultMap.put("tmpMaillingAddress", ownerInfo);
				} else {
					resultMap.put("tmpOwner", ownerInfo);
				}
			}
			m = Pattern.compile("(?is)(?<=Situs address).*").matcher(info);
			if(m.find()) {
				address = m.group().replaceAll("(?is)Legal description.*", "").trim();
				address = address.replaceAll("(?is)\\(?unknown\\)?", "").trim();
				address = address.replaceFirst("^\\.$", "");
				if (StringUtils.isNotEmpty(address)){
					resultMap.put("tmpAddress", address);
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					String[] addr = address.split("\n");
					if (addr.length > 0){
						String ad = addr[0].trim();
						
						ad = ad.replaceAll("(?is)\\A\\s*(\\d+[A-Z]?|[A-Z])\\s+(\\d+\\s+.*)", "$2 $1").trim();//for Monroe; 801 2000 COCO PLUM DR
						ad = ad.replaceAll("\\bTO\\s+LAKE\\b", "\"$0\""); // Citrus: 08044 E GULF TO LAKE (LAKE is not suffix)
						ad = ad.replaceAll("\\bHWY\\s+\\d+", "\"$0\""); // Citrus: 00505 NE US HWY 19 (HWY 19 is part of the name)
						ad = ad.replaceAll("(?is)\\bLO?T\\s+\\d+", "").trim(); //Monroe, Account number 1245216;  LOT 457 701 SPANISH MAIN DR
						ad = ad.replaceAll("(?is)\\s*-?\\s*(BLDG|BUILDNG|UT|UNIT)\\s*", " $1 ").trim(); //Charlotte, Account number 412213809008;  1359 ROCK DOVE CT -BLDG 2-UNIT 2-4
						
						if (ad.contains("BLDG") || ad.contains("BUILDING")) { //Charlotte, Account number 412213809008
							String bldg = ad;
							bldg = bldg.replaceFirst("(?is).*(?:BLDG|BUILDNG)\\s*([\\dA-Z-]+).*", "$1").trim();
//							if (StringUtils.isNotEmpty(bldg)) {
//								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
//							}
							ad = ad.replaceFirst("(?is)(.*)(?:BLDG|BUILDNG)\\s*[\\dA-Z-]+(.*)", "$1 $2").trim();
						}
						
						resultMap.put("tmpAddress", ad);
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(ad));
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(ad));
						if (addr.length > 1){
							String city = addr[1].replaceAll("(?is)\\bCITY\\s+OF\\b", "").trim();
							if(countyId == CountyConstants.FL_Citrus)  {
								city = city.split(",")[0];
							
							} else if (countyId == CountyConstants.FL_Miami_Dade || countyId == CountyConstants.FL_Alachua) {
								city = city.replaceFirst("(?is)\\bUnincorporated County", "").trim();
								String zip = StringUtils.extractParameter(city, ".*?\\b(\\d+\\s*(-\\s*\\d+)?)?\\s*$");
								if (StringUtils.isNotEmpty(zip)) {
									city = city.replaceFirst(Pattern.quote(zip), "").trim();
									zip = zip.replaceFirst("^(\\d+)-\\d+$", "$1");
									resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
								}
							}
							if (!city.isEmpty()) {
								resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
							}
						}
					}
				}
				
			}
			m = Pattern.compile("(?is)(?<=Legal description).*").matcher(info);
			if(m.find()) {
				legal = m.group().trim();
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
				if(countyId == CountyConstants.FL_Citrus) {
					resultMap.put("tmpPropAddrLegalDesc", legal);
				}
			}
			
			String baseAmount = "0.00";
			ParagraphTag combinedAssess = (ParagraphTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "combined"), true)
													.elementAt(0);
			baseAmount = combinedAssess.toPlainTextString();
			baseAmount = baseAmount.replaceAll("(?is)</?p[^>]*>", "").replaceAll("[^:]*:\\s+", "").replaceAll("[\\$,]+", "").trim();
			resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			
			org.w3c.dom.Node tbl = HtmlParserTidy.getNodeById(detailsHtml, "tabtax1", "table");
			String table = "";
			
					
			tbl = HtmlParserTidy.getNodeById(detailsHtml, "bill-if-paid-by", "table");
			try {
				table = HtmlParserTidy.getHtmlFromNode(tbl);
				if(table != null) {
					org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(table, null);
					NodeList rows = parser.extractAllNodesThatMatch(new TagNameFilter("tr"));
					if (rows.size() > 0){
						NodeList tdList = rows.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
						StringBuffer totalDueBuff = new StringBuffer();
						if (tdList.size() > 0){
							//String gross = "";
							for (int i = 0; i < tdList.size(); i++){
								String tempDue = tdList.elementAt(i).toHtml();
								if (!(tempDue.contains("Face") || tempDue.contains("Certificate") || tempDue.contains("Gross") || tempDue.contains("Discount"))) {
									tempDue = tempDue.replaceAll("</?td[^>]*>", "").replaceAll("</?br>", "  ");
									totalDueBuff.append(tempDue + "@@");
								}else if(tempDue.contains("Gross")){
									//gross = tempDue.replace("Gross", "").replace("<td>", "").replace("</td>", "").replace("<br>", "").replaceAll("[$,]", "").trim();
								}
							}
							totalDue = totalDueBuff.toString();
							totalDue = totalDue.replaceAll("\\$", "");
							resultMap.put("tmpAmountDue", totalDue.trim());
							if (StringUtils.isNotEmpty(totalDue)) {
								totalDue = GenericFunctions.extractAmountFromArray(totalDue);
								if(StringUtils.isNotEmpty(totalDue)){
									resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
								}else{
									//resultMap.put("TaxHistorySet.TotalDue", gross);
								}
							}
						}
					}
				}
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			
			List<org.w3c.dom.Node> taxTables = HtmlParserTidy.getNodeListByTagAndAttr(detailsHtml, "table", "id", "tax_tb3");
			ResultTable rt = new ResultTable();
			List<List> body = new ArrayList<List>();
			List<String> list = new ArrayList<String>();
			String amtPaid = "", priorDelinq = "", datePaid = "";
			Pattern datePattern = Pattern.compile("(\\d+/\\d+/\\d+).*");
			Pattern paidReceiptPattern = Pattern.compile("Paid\\s*\\$([\\d,\\.]+)\\s*Receipt\\s*#([\\w-]+)");
			try {
				for (org.w3c.dom.Node nodTable : taxTables) {
					list = new ArrayList<String>();
					String tabel = HtmlParserTidy.getHtmlFromNode(nodTable);
					if (StringUtils.isNotEmpty(tabel)){
						tabel = tabel.replaceAll("(?is)<col[^>]*>", "").replaceAll("(?is)<th ", "<td ").replaceAll("(?is)</th\\s*>", "</td>")
										.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "")
										.replaceAll("(?is)\r", " ").replaceAll("(?is)\n", " ");
						//System.err.println("Tabelul este " + tabel);
						List<List<String>> tableAsList = HtmlParser3.getTableAsList(tabel, false);
						//System.err.println("Tabelul ca lista este " + tableAsList);
						for (int i = 0; i < tableAsList.size(); i++){
							if (tableAsList.get(i).size() > 0){
								if (tableAsList.get(i).get(0).trim().startsWith(taxYear)){
									List<String> cols = tableAsList.get(i);
									if (cols.size() == 4){
										if (cols.get(0).toString().toLowerCase().contains("installment"))
											isPaidinInstallments = true;
										list = new ArrayList<String>();
										Matcher dateMatcher = datePattern.matcher(cols.get(2));
										if(dateMatcher.find()) {
											if(StringUtils.isEmpty(datePaid)) {
												datePaid = dateMatcher.group(1);
											}
											list.add(dateMatcher.group(1));
										}
										Matcher paidReceiptMatcher = paidReceiptPattern.matcher(cols.get(3).replaceAll(",", ""));
										if(paidReceiptMatcher.find()) {
											amtPaid += "+" + paidReceiptMatcher.group(1);
											list.add(paidReceiptMatcher.group(1));
											list.add(paidReceiptMatcher.group(2));
										}
										if(list.size() == 3) {
											body.add(list);
										}
									}
								} else {
									if(tableAsList.get(i).size() ==  4 && tableAsList.get(i).get(0).trim().startsWith("Total balance")){
										if(StringUtils.isEmpty(totalDue)){
											totalDue =  tableAsList.get(i).get(1).replaceAll("[$,]", "").trim();
											//resultMap.put("TaxHistorySet.TotalDue", totalDue);
										}
									}
									else if (tableAsList.get(i).size() ==  4){
										if (tableAsList.get(i).get(1).toLowerCase().contains("annual bill")){
											priorDelinq += "+" + tableAsList.get(i).get(2).replaceAll("[\\$,]", "").trim();
										}
										List<String> cols = tableAsList.get(i);
										if (cols.size() == 4){
											list = new ArrayList<String>();
											Matcher dateMatcher = datePattern.matcher(cols.get(2));
											if(dateMatcher.find()) {
												list.add(dateMatcher.group(1));
											}
											Matcher paidReceiptMatcher = paidReceiptPattern.matcher(cols.get(3).replaceAll(",", ""));
											if(paidReceiptMatcher.find()) {
												list.add(paidReceiptMatcher.group(1));
												list.add(paidReceiptMatcher.group(2));
											}
											if(list.size() == 3) {
												body.add(list);
											}
										}
									} else if (tableAsList.get(i).size() ==  3){
										if (tableAsList.get(i).get(0).toLowerCase().contains("annual bill") || 
											tableAsList.get(i).get(0).toLowerCase().contains("penalty bill")){
											priorDelinq += "+" + tableAsList.get(i).get(1).replaceAll("[\\$,]", "").trim();
										}
									}
								}
							}
						}
					}
					amtPaid = amtPaid.replaceAll("(?is)\\A\\s*\\+\\s*$", "");
				}
				String[] header = {"ReceiptDate", "ReceiptAmount", "ReceiptNumber"};
				rt = GenericFunctions2.createResultTable(body, header);
				if (rt != null){
					resultMap.put("TaxHistorySet", rt);
				}
				//System.err.println("Atentie Amount Paid = " + amtPaid);
				if (StringUtils.isNotEmpty(amtPaid)){
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), GenericFunctions.sum(amtPaid, searchId));
					if (StringUtils.isNotEmpty(currentInstallment)){
						if (currentInstallment.trim().contains("annual bill")){//when the payment are not made on installments, there is no total due
							resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "0.00");
						} /*else if (currentInstallment.trim().contains("installment bill #4")){//when the payment are made on installments and it's the last installment, there is no total due
							resultMap.put("TaxHistorySet.TotalDue", "0.00");
						}*/
					}
				}
				//System.err.println("Atentie Amount Paid = " + amtPaid);
				if(StringUtils.isNotEmpty(datePaid)) {
					resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), datePaid);
				}
				priorDelinq = GenericFunctions.sum(priorDelinq, searchId);
				if (StringUtils.isNotEmpty(priorDelinq)){
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), GenericFunctions.sum(priorDelinq, searchId));
				}
				totalDue = GenericFunctions.sum(totalDue, searchId);
				amtPaid = GenericFunctions.sum(amtPaid, searchId);
				if (isPaidinInstallments) {
					totalDue += "+-" + priorDelinq;
					totalDue = baseAmount + "+-" + amtPaid;
					totalDue = GenericFunctions.sum(totalDue, searchId);
				}
				if(totalDue.equals(GenericFunctions.sum(amtPaid, searchId))) {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), null);
				} else {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
				}
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			
			// parse transfers
			tbl = HtmlParserTidy.getNodeById(detailsHtml, "saleHistory", "table");
			try {
				String saleTable = HtmlParserTidy.getHtmlFromNode(tbl);
				if (StringUtils.isNotEmpty(saleTable)){
					saleTable = saleTable.replaceAll("[\\$,]", "");
					saleTable = saleTable.replaceAll("(?is)&amp;", "&");
					saleTable = saleTable.replaceAll("(?is)<tr[^>]*>\\s*<t[^>]+>\\s*Sales\\s+(Information|History)[^<]*</t[^>]+>\\s*</tr>", "");
					List<List<String>> transacHist = HtmlParser3.getTableAsList(saleTable, false);
					ResultTable rtSale = new ResultTable();
					if (countyId == CountyConstants.FL_Osceola){
						for(List<String> lst : transacHist){
							String bookPage = lst.get(1).trim();
							lst.set(0, bookPage.replaceAll("(?is)(\\d+)\\s*-\\s*(\\d+)", "$1").replaceAll("(?is)\\A0+", ""));
							lst.set(1, bookPage.replaceAll("(?is)(\\d+)\\s*-\\s*(\\d+)", "$2").replaceAll("(?is)\\A0+", ""));
						}
						List<List> newBody = new ArrayList<List>(transacHist);
						String[] header = {"Book", "Page", "SalesPrice", "InstrumentDate", "DocumentType"};
						rtSale = GenericFunctions2.createResultTable(newBody, header);
					} else if (countyId == CountyConstants.FL_Okaloosa){
						if (transacHist.size() > 0) {// remove table header
							transacHist.remove(0);
						}
						for(List<String> lst : transacHist){
							if (lst.size() == 9){
								lst.remove(6);
								lst.remove(5);
							}
						}
						List<List> newBody = new ArrayList<List>(transacHist);
						String[] header = {"InstrumentDate", "SalesPrice", "DocumentType", "Book", "Page", "Grantor", "Grantee"};
						rtSale = GenericFunctions2.createResultTable(newBody, header);
					} else if (countyId == CountyConstants.FL_Citrus) {
						for(List<String> lst : transacHist){
							if (lst.size() == 8){
								lst.remove(2);
								lst.remove(4);
								lst.remove(4);
							}
							if (lst.size()>=1)
								lst.set(0, lst.get(0).trim().replaceAll("(?is)\\A0+", ""));
							if (lst.size()>=2)
								lst.set(1, lst.get(1).trim().replaceAll("(?is)\\A0+", ""));
						}
						List<List> newBody = new ArrayList<List>(transacHist);
						String[] header = {"Book", "Page", "Year", "DocumentType", "SalesPrice"};
						rtSale = GenericFunctions2.createResultTable(newBody, header);
					} else if (countyId == CountyConstants.FL_Charlotte){
						for(List<String> lst : transacHist){
							String bookPage = lst.get(1).trim();
							lst.set(1, bookPage.replaceAll("(?is)(\\d+)\\s*/\\s*(\\d+)", "$1").replaceAll("(?is)\\A0+", ""));
							lst.set(2, bookPage.replaceAll("(?is)(\\d+)\\s*/\\s*(\\d+)", "$2").replaceAll("(?is)\\A0+", ""));
						}
						List<List> newBody = new ArrayList<List>(transacHist);
						String[] header = {"InstrumentDate", "Book", "Page", "SalesPrice"};
						rtSale = GenericFunctions2.createResultTable(newBody, header);
					} else if (countyId == CountyConstants.FL_Pinellas){
						List<List<String>> properTransacHist = new ArrayList<List<String>>();
						for(List<String> lst : transacHist){
							if(lst.size() == 5) {
								String bookPage = lst.get(1).trim();
								lst.set(1, bookPage.replaceAll("(?is)(\\d+)\\s*/\\s*(\\d+)", "$1").replaceAll("(?is)\\A0+", ""));
								lst.add(2, bookPage.replaceAll("(?is)(\\d+)\\s*/\\s*(\\d+)", "$2").replaceAll("(?is)\\A0+", ""));
								lst.remove(4);
								lst.remove(4);
								properTransacHist.add(lst);
							}
						}
						List<List> newBody = new ArrayList<List>(properTransacHist);
						String[] header = {"InstrumentDate", "Book", "Page", "SalesPrice"};
						rtSale = GenericFunctions2.createResultTable(newBody, header);
					} else if (countyId == CountyConstants.FL_Lake){
						for(List<String> lst : transacHist){
							lst.remove(4);
							lst.set(3, lst.get(2));
							lst.set(2, lst.get(1));
							String bookPage = lst.get(0).trim();
							lst.set(0, bookPage.replaceAll("(?is)(\\d+)\\s*/\\s*(\\d+)", "$1").replaceAll("(?is)\\A0+", ""));
							lst.set(1, bookPage.replaceAll("(?is)(\\d+)\\s*/\\s*(\\d+)", "$2").replaceAll("(?is)\\A0+", ""));
						}
						List<List> newBody = new ArrayList<List>(transacHist);
						String[] header = {"Book", "Page", "InstrumentDate", "DocumentType", "SalesPrice"};
						rtSale = GenericFunctions2.createResultTable(newBody, header);
					}
					 
					resultMap.put("SaleDataSet", rtSale);
				}
				
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			
			switch (countyId) {
				case CountyConstants.FL_Alachua:
					FLAlachuaTR.legalFLAlachuaTR(resultMap, legal);
					FLAlachuaTR.partyNamesFLAlachuaTR(resultMap, searchId);
					break;
				case CountyConstants.FL_Charlotte:
					FLCharlotteTR.partyNamesFLCharlotteTR(resultMap, searchId);
					FLCharlotteTR.legalFLCharlotteTR(resultMap, legal);
					break;
				case CountyConstants.FL_Citrus:
					FLCitrusTR.parseNames(resultMap, ownerInfo);
					FLOsceolaTR.legalFLOsceolaTR(resultMap, searchId);
					break;
				case CountyConstants.FL_Indian_River:
					FLIndianRiver.partyNamesFLIndianRiverTR(resultMap, searchId);
					FLIndianRiver.legalTokenizerFLIndianRiverTR(resultMap, legal);
					break;
				case CountyConstants.FL_Lake:
					FLLakeTR.partyNamesFLLakeTR(resultMap, searchId);
					FLLakeTR.legalFLLakeTR(resultMap, searchId);
					break;
				case CountyConstants.FL_Miami_Dade:
					FLMiamiDadeTR.legalFLMiamiDadeTR(resultMap, legal);
					FLMiamiDadeTR.parseNamesFLMiamiDadeTR(resultMap, searchId);
					break;
				case CountyConstants.FL_Monroe:
					FLMonroeTR.parseNames(resultMap, ownerInfo);
					break;
				case CountyConstants.FL_Pinellas:
					FLPinellasTR.parseNames(resultMap, ownerInfo);
					FLPinellasTR.parseAddressFLPinellasTR(resultMap, searchId);
					FLPinellasTR.legalTokenizerFLPinellasTR(resultMap, legal);
					break;
				case CountyConstants.FL_Sumter:
					FLSumterTR.parseNames(resultMap, ownerInfo);
					FLSumterTR.parseLegal(resultMap, legal);
					break;
				default:
					parseNamesFLXXXTR(resultMap, searchId);
					legalFLBrowardTR(resultMap, searchId);
					break;
			}
			
			saveTestDataToFiles(resultMap);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	protected static void saveTestDataToFiles(ResultMap map) {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String pin = "" + map.get("PropertyIdentificationSet.ParcelID");
			String text = pin + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") + "\r\n\r\n\r\n";
			String path = "D:\\work\\" + "FLCharlotteTR" + "\\";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);

			// text = pin + "\r\n" +
			// map.get("PropertyIdentificationSet.AddressOnServer") +
			// "\r\n\r\n\r\n";
			// ro.cst.tsearch.utils.FileUtils.appendToTextFile(path +
			// "address.txt", text);

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.NameOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt", text);
			
			text = pin + "\r\n" + map.get("PropertyIdentificationSet.LegalDescriptionOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "legal_description.txt", text);
			// end test
		}
	}

	@SuppressWarnings("rawtypes")
	public static void legalFLBrowardTR(ResultMap m, long searchId) throws Exception {

		//String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
		
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("\\b&amp;\\b", "&");
		legal = legal.replaceAll("\\bCON DO(MINIUM)?\\b", "CONDO");
		legal = legal.replaceAll("(?is)\\b(OR)\\s*([\\d-]+)\\s*:\\s*", "$1 $2 $1 ");
		legal = legal.replaceAll("(?is)\\b(ORB?)\\s*([\\d-]+)\\s*(\\d+)\\s*(\\bPG\\b)", "$1 " + "$2$3" + " $4");
		legal = legal.replaceFirst("(?is)(MB\\s*\\d+)\\s*(PGS?\\s*\\d+)\\s+(\\d+)", "$1 " + "$2$3");
		legal = legal.replaceAll("(?is)((ORB?)\\s*(?:[\\d-]+))\\s+P\\s*G", "$1 " + "PG");
		//legal = legal.replaceAll("(?is)(ORB?)\\s*(\\d+)\\s*(?:PGS)\\s*(\\d+)\\s*-\\s*(\\d+)", "$1 $2 PG $3" + " $1 $2 PG $4");
		legal = legal.replaceAll("(?is)(ORB?)\\s*(\\d+)\\s*(?:PGS)\\s*(\\d+)\\s*-\\s*(\\d+)", "$1 $2 PG $3");
		legal = legal.replaceAll("(?is)\\bBL?K\\s+(LO?TS?)\\b", "$1");
		legal = legal.replaceFirst("(?is)\\s*\\bSUB(?:DIV(?:ISION)?)?\\b", " ").trim();
		legal = legal.replaceFirst("(?is)\\s*\\d+\\s*(?:ST|ND|RD|TH)\\b\\s+ADD(?:ITION)?", " ").trim();
		legal = legal.replaceAll("(?is)\\s*\\b[W|E|S|N]\\b\\s*(?:\\d+\\s*/\\s*\\d+)\\s*OF\\s*", " ");
		legal = legal.replaceAll("(?is)(\\bLOTS?\\b\\s*[\\w]+(?:\\s*[-&]\\s*\\d+\\w{1})?)\\s*[&\\s-]\\s*LOTS?\\s*([\\w]+(?:\\s*[-&]\\s*\\d+\\w{1})?)", "$1 & $2");
		legal = legal.replaceAll("(?is)([\\d+A-z])\\s+([\\d+A-Z])\\s*&\\s*([\\d+A-Z])", "$1" + "&" + "$2" + "&" + "$3");
		
		legal = GenericFunctions.replaceNumbers(legal);
		
		boolean foundPlat = extractInfoIntoMap(m,legal,"(?:\\s|^)(\\d+)-(\\d+) [A-Z]\\b",
				PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
				PropertyIdentificationSetKey.PLAT_NO.getKeyName());

		if (!foundPlat){
			extractInfoIntoMap(m,legal,"(?:\\s|^)(?:PB|MB)\\s*(\\w+)\\s*PGS?\\s*([\\d]+)\\b",
					PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
					PropertyIdentificationSetKey.PLAT_NO.getKeyName());
		}
		extractInfoIntoMap(m,legal,"\\bBLDG ((\\d|[A-Z])+\\b( ([A-Z]|\\d+)$)?)", PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName());
		extractInfoIntoMap(m,legal,"(?is)\\bTRACT\\s+(\\d+)", PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName());
		extractInfoIntoMap(m,legal,"\\bPH(?:ASE)? (\\d+)\\b", PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName());
		extractInfoIntoMap(m,legal,"\\b(?:UNIT|APT(?:\\s+NO)?) ([-\\w]+)\\b", PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
		
		extractInfoIntoMap(m,legal, "(.*?)\\s*\\b([A-Z]/[A-Z]|LOT|UN(?:IT)?|APT|PHASE|PH|BLK|SEC|CONDO(MINIUM)?|\\d+-\\d+ [A-Z]|\\d+(ST|ND|RD|TH) " +
				"(REV(ISION)?|ADDN?|SEC(TION)?)|REV PLAT|SECTOR|SUB|LAND SEC(TION)?|SEC(TION)? \\d+|\\d+-\\d+-\\d+)\\b.*", PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
		
		if (countyId == CountyConstants.FL_Volusia) {
			String subdName = (String)m.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
			if (StringUtils.isEmpty(subdName)) {
				String regExp = "(?is)\\s*\\b(?:LOTS?|UN(?:IT)?S?)\\s*[\\d-&\\s]+([A-Z\\d\\s&]+)\\s*\\b(P\\s*ER)\\b.*";
				Matcher matcher = Pattern.compile("(?is)\\s*\\b(?:LOTS?|UN(?:IT)?S?)\\s*[\\d-&\\s]+([A-Z\\d\\s&]+)\\s*\\b(MB)\\b.*").matcher(legal);
				if (legal.contains(" MB")) {
					if (matcher.find()) {
						subdName = matcher.group(1);
						if (StringUtils.isNotEmpty(subdName)) {
							m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),subdName);
						}
					}
				} else {
					matcher = Pattern.compile(regExp).matcher(legal);
					if (matcher.find()) {
						subdName = matcher.group(1);
						if (StringUtils.isNotEmpty(subdName)) {
							m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),subdName);
						}
					}
				}
			}
			
		} else if (countyId == CountyConstants.FL_Monroe) {
			String subdName = (String)m.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
			if (StringUtils.isEmpty(subdName)) {
				Matcher matcher = Pattern.compile("(?is)(.*?)\\s*\\bPB\\s*\\d+\\b.*").matcher(legal);
				if (matcher.find()) {
					subdName = matcher.group(1);
					subdName = subdName.replaceAll("(?is)\\bBL?K\\s+(\\d+|[A-Z])", "");
					subdName = subdName.replaceAll("(?is)\\bLO?TS?\\s+([-\\d\\s&]+)", "");
					subdName = subdName.replaceAll("(?is)\\bPLAT\\s*$", "");
					String city = (String)m.get(PropertyIdentificationSetKey.CITY.getKeyName());
					if (!StringUtils.isEmpty(city)) {
						subdName = subdName.replaceFirst("(?is)\\b" + city + "\\b\\s*$", "");
					}
					subdName = subdName.trim();
					m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),subdName);
				}
			}
		}
		if (legal.contains("CONDO")) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), "CONDO");
		}	
		
		legal = legal.replaceAll("#", "");
		legal = legal.replaceAll("\\s+TO\\s+", "-");
		
		String lotPatt = "\\bLO?TS?\\s+([-\\w&]+(?:\\s*[-&]\\s*\\d+\\w{1})?)"; 
		extractInfoIntoMap(m,legal,lotPatt,"PropertyIdentificationSet.SubdivisionLotNumber");
		legal = legal.replaceFirst(lotPatt, "LOT");
		
		extractInfoIntoMap(m,legal,"\\bBL?K\\s+(\\d+|[A-Z])","PropertyIdentificationSet.SubdivisionBlock");
		
		boolean foundSec = extractInfoIntoMap(m,legal,"\\bSEC(?:TION)? (\\d+)(\\s|$)","PropertyIdentificationSet.SubdivisionSection");
		if(!foundSec) {
			extractInfoIntoMap(m,legal,"\\b(\\d+)(?:ST|ND|RD|TH) SEC(?:TION)?\\b","PropertyIdentificationSet.SubdivisionSection");
		}
		

		boolean foundStw = extractInfoIntoMap(m,legal,"\\b(\\d+)[-|\\s+](\\d+)[-|\\s+](\\d+)\\b",
				PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName());
			
		if(!foundStw) {
				extractInfoIntoMap(m,legal,"\\bSEC (\\d+).*,\\s*TWN (\\d+).*,\\s*RANGE (\\d+)\\b",
				PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName());
		}
		
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, new String[]{ "X", "L", "C", "D", "M" });
		
		List<List> bodyCR = new ArrayList<List>();
		
		//Pattern p = Pattern.compile("(?is)(ORB?)\\s*(\\d+)\\s*(?:-|PG)\\s*(\\d+)");
		Pattern p = Pattern.compile("(?is)(ORB?|\\bD\\s*/\\s*C\\b)\\s*(\\d+)\\s*(?:-|PG)\\s*(\\d+)");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2).replaceAll("(?is)\\A0+", ""));
			line.add(ma.group(3).replaceAll("(?is)\\A0+", ""));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
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
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	public static void parseNamesFLXXXTR(ResultMap resultMap, long searchId) throws Exception {
		   String owner = (String) resultMap.get("tmpOwner");
		   resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
		   
		   if (StringUtils.isEmpty(owner))
			   return;
		   
		   //String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		   int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
		   
		   owner = owner.replaceAll("(?is)\n\\s*CO[-\\s+]+(TRUSTEES?)\\s*\n", " $1\n");
		   owner = owner.replaceAll("(?is)-\n\\s*(\\w+)\\s*\n", "-$1\n");
		   String[] ownerRows = owner.split("\n");
		   StringBuffer stringOwnerBuff = new StringBuffer();
		   for (String row : ownerRows){
			   if (row.trim().matches("\\d+\\s+.*")){
			   } else if (row.toLowerCase().contains("box")){
			   } else if (LastNameUtils.isNoNameOwner(row)) {
			   } else if (row.trim().startsWith("*")) {					//*Kingston
			   } else if (Countries.isCountry(row.trim())) {	   		//Jamaica
			   } else if (row.matches(".*,\\s*[A-Z]{2}\\s+\\d+((\\s|-)\\d+)?")) {	//VIRGINIA BEACH, VA 23453
			   } else if (row.matches("(?is)\\w+\\s*HP\\s*\\d{2,3}\\s*[\\dA-Z]{3,4}")) { //BUCKINGHAMSHIRE    HP22 5JX
			   } else if (row.matches("(?is).+,\\s+[A-Z]{2}\\s*[A-Z]\\d[A-Z]\\s+\\d[A-Z]\\d+\\s*")) { //FORT MCMURRAY, AB T9K 1S7 (address in Canada) (FL Osceola R092527-305700010250)
			   } else if (row.matches("(?is)U\\.?\\s*K\\.?\\s*")) {
				   
				   
			   } else {
				   stringOwnerBuff.append(row.trim() + "\n");
			   } 
		   }
		   //ST Lucie 3420-670-1473-0001 - Owner  Gonzales, Armando S   719 Cordelia Place    The Villages, FL 32162-4407   Gonzales, Flora M   Gonzales, Lourdes M   Gonzales Grace M

		   String stringOwner = stringOwnerBuff.toString();
		   stringOwner = stringOwner.replaceAll("\n$", "");
		   stringOwner = stringOwner.replaceAll("(?is)\n\\s*\\(\\s*ADDRESS\\s+UNKNOWN.*", "");
		   stringOwner = stringOwner.replaceAll("\n(ASSOCIATIONS?)(\n|$)", " $1$2");

		   String[] nameLines = stringOwner.split("\n");
		   nameLines = fixLines(nameLines);

		   List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes, type, otherType;
		   StringBuffer nameOnServerBuff = new StringBuffer();
		   for (int i=0; i < nameLines.length; i++){
			   String ow = nameLines[i];
			   ow = clean(ow);
			   if(ow.matches("(?is)THE\\s+(.*)\\s+TR")) { // THE EVELYN M WILSON TR
				   if(NameUtils.isNotCompany(ow.replaceFirst("(?is)THE\\s+(.*)\\s+TR", "$1"))) {
					   ow = ow.replaceFirst("TR$", "TRUST");
				   }
			   }
			  
			   if (countyId == CountyConstants.FL_Volusia && i > 0){
				   if (NameUtils.isNotCompany(ow)){
					   ow = ow.replaceAll("(?is)\\A\\s*(\\w+)\\s*&\\s*(.*)", "$2 & $1");
				   }
				   names = StringFormats.parseNameDesotoRO(ow, true);
			   } else {
				   names = StringFormats.parseNameNashville(ow, true);
			   }
			   if (ow.trim().startsWith("%")){
				   ow = ow.replaceAll("%", "");
				   names = StringFormats.parseNameDesotoRO(ow, true);
			   }
			   if(NameUtils.isCompany(ow)) { // Task 7992 - Usually, there can't be 2 company names on the same line
				   String[] tokens = ow.split("[/&]");
				   if(tokens.length > 1 && NameUtils.isCompany(tokens[0]) && NameUtils.isCompany(tokens[1])) {
					   names = new String[]{"", "", ow, "", "", ""};
				   }
			   }
			   suffixes = GenericFunctions.extractNameSuffixes(names);
			   type = GenericFunctions.extractAllNamesType(names);
			   otherType = GenericFunctions.extractAllNamesOtherType(names);
			   GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					   							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			   nameOnServerBuff.append("/").append(ow);
		   }
		   String nameOnServer = nameOnServerBuff.toString();
		   nameOnServer = nameOnServer.replaceFirst("/", "");
		   resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
		   GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
	   }
	
	private static String[] fixLines(String[] nameLines) {
		ArrayList<String> newLines = new ArrayList<String>();
		Pattern p = Pattern.compile("(?is)([\\w\\s]+)\\s+TR(?:USTEE)?\\s*,\\s*([\\w\\s]+)\\s+TR(?:UST)?");
		
		for(String line : nameLines) {
			Matcher m = p.matcher(line);
			if(m.matches()) {
				// WILSON KERMIT Z TR,KERMIT Z WILSON TRUST
				if(NameUtils.isNotCompany(m.group(1)) && NameUtils.isNotCompany(m.group(2))) {
					String[] names = line.split(",");
					newLines.add(names[0]);
					newLines.add(names[1]);
				} else {
					newLines.add(line);
				}
			} else {
				newLines.add(line);
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}

	public static boolean extractInfoIntoMap(ResultMap m, String extractFrom, String regex, String... keys) {
	
		try {
			Pattern p = Pattern.compile(regex);
			Matcher ma = p.matcher(extractFrom);
			if (ma.find()) {
				for(int i=0; i<keys.length ; i++) {
					try {
						if (PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName().equals(keys[i])) {
							String subdName = ma.group(i+1).replaceAll("(?is)\\bBL?K\\s+(\\d+|[A-Z])", "");
							subdName = subdName.replaceAll("(?is)\\bLO?TS?\\s+([-\\d\\s&,]+)", "");
							subdName = subdName.replaceAll("(?is)\\s*\\bPH(?:ASE)?\\s*(?:[\\dA-Z]+)", "");
							subdName = subdName.replaceAll("(?is)\\s*\\bPB\\s*[\\dA-Z]+\\s+\\bPG\\s*[,\\dA-Z-&\\s:]+", "");
							subdName = subdName.replaceFirst("(?is)^\\s*KW\\s+KW\\s+REALTY\\b", "KEY WEST REALTY");
							subdName = subdName.replaceFirst("(?is)\\s*TRACT.*", "");
							subdName = subdName.replaceFirst("(?is)REPLAT\\s*.*", "");
							subdName = subdName.replaceFirst("(?is)^COM\\s+AT\\s*.*", "");
							String[] subdParts = subdName.split("\\s+");
							if (subdParts.length > 0) {
								String romanNumber = subdParts[subdParts.length - 1];
								if (Roman.isRoman(romanNumber)) {
									String number = Roman.transformToArabic(romanNumber);
									subdName = subdName.replaceFirst(romanNumber + "\\s*$", number);
								}
							}
							subdName = subdName.replaceFirst("\\d+\\s*$", "");
							subdName = subdName.trim();
							m.put(keys[i], subdName);
						} else if (PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName().equals(keys[i])) {
							m.put(keys[i], ma.group(i+1).replaceAll("&", " ").replaceAll("\\s{2,}", " ").trim());
						} 
						else if (PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName().equals(keys[i])) {
							if (Roman.isRoman(ma.group(i + 1).trim())) {
								m.put(keys[i], Roman.transformToArabic(ma.group(i + 1).trim()));
							}
							else
								m.put(keys[i], ma.group(i + 1).trim());
						} 
						else {
							m.put(keys[i], ma.group(i+1).trim().replaceFirst("^0+", ""));
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
				return true;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private static String clean(String str) {
		if(StringUtils.isEmpty(str)) return "";
		return 
			str .replaceAll("<br>", "\n")
				.replaceAll("[\\$,]", " ")// dash must not be cleaned all the time, in names must be: BIANCO-ABASSI,LISA ANN
				.replaceAll("\\b[\\d/]+\\s+INT(?:\\s+EA)?", "&")
				.replaceAll("\\b[W|H]\\s*&\\s*[H|W]\\b", "")
				.replaceAll("\\b[A-Z]\\s*/\\s*[A-Z]\\b", "")
				.replaceAll("(?is)\\bJTWRS\\b", "")
				.replaceAll("(?is)\\bPR\\b", "")
				.replaceAll("(?is)\\b-?POA\\b", "")
				.replaceAll("(?is)\\A\\s*OF\\s+", "")
				.replaceAll("(?is)\\bTRE\\b", "TRUSTEE")
				.replaceAll("(?is)\\bCO[-|\\s+](TR(?:USTEE)?S?)\\b", "$1")
				.replaceAll("C/O", "")
				.replaceAll("ATTN", "")
				.replaceFirst("(?is)\\bP/R\\s*$", "")
				.trim();
		}
	
	public static void parseAddress(ResultMap resultMap) throws Exception {
		
		String address = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get("tmpAddress")).trim();
		address = address.replaceFirst("^\\.$", "");// e.g. 333N2300000001002A ; found bug and fixed while fixing 9956
		if (StringUtils.isEmpty(address))
			return;
		
		parseAddress(resultMap, address);
	}
	public static void parseAddress(ResultMap resultMap, String origAddress) throws Exception {
		
		String addressLine=origAddress, cityStateZipLine = "", city = "", zip ="";
		String[] addressLines = origAddress.split(",");
		addressLine = addressLines[0];
		if(addressLines.length>1) {
			cityStateZipLine = addressLines[1];
		}
		
		addressLine = addressLine.replaceAll("(?is)\\A\\s*(\\d+[A-Z]?|[A-Z])\\s+(\\d+\\s+.*)", "$2 $1").trim();//for Monroe; 801 2000 COCO PLUM DR
		addressLine = addressLine.replaceAll("^0+", ""); // for Citrus: 00065 BEACH
		addressLine = addressLine.replaceAll("(?is)\\bLO?T\\s+\\d+", "").trim(); //Monroe, Account number 1245216;  LOT 457 701 SPANISH MAIN DR
		addressLine = addressLine.replaceAll("(?is)\\s*-\\s*(BLDG|BUILDNG|UT|UNIT)\\s*", " $1 ").trim(); //Charlotte, Account number 412213809008;  1359 ROCK DOVE CT -BLDG 2-UNIT 2-4
		
		if (addressLine.contains("BLDG") || addressLine.contains("BUILDING")) { //Charlotte, Account number 412213809008
			String bldg = addressLine;
			bldg = bldg.replaceFirst("(?is).*(?:BLDG|BUILDNG)\\s*([\\dA-Z-]+).*", "$1").trim();
//			if (StringUtils.isNotEmpty(bldg)) {
//				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
//			}
			addressLine = addressLine.replaceFirst("(?is)(.*)(?:BLDG|BUILDNG)\\s*[\\dA-Z-]+(.*)", "$1 $2").trim();
		}
		
		String streetNo = StringFormats.StreetNo(addressLine);

		if(!StringUtils.isEmpty(streetNo)) {
			addressLine = addressLine.replaceAll(streetNo,"").trim();
		}
		
		if(zip.isEmpty()) {
			Pattern zipPattern = Pattern.compile("([\\-0-9]){5,}");
			Matcher zipMatcher = zipPattern.matcher(cityStateZipLine);
			while(zipMatcher.find()) {
				zip = zipMatcher.group();
			}
			cityStateZipLine = cityStateZipLine.replaceAll(zip,"");
		}
		
		if(city.isEmpty()) {
			city = cityStateZipLine.replaceAll("FL|[^\\w\\s]","");
		}
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), addressLine);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.replaceAll("(?is)\\bCITY\\s+OF\\b", "").trim());
		resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), origAddress);
	}

}

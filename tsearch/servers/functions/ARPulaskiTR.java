package ro.cst.tsearch.servers.functions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.w3c.dom.Node;

import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

/**
 * @author Mihai Dediu
  */
public class ARPulaskiTR {

	public static void parseAndFillResultMap(String rsResponse, ResultMap m,
			long searchId) {
		 org.w3c.dom.Document finalDoc = Tidy.tidyParse(rsResponse);

		String instrumentNo = HtmlParserTidy.getValueFromInputByName(finalDoc, "STDID");
		
		m.put("PropertyIdentificationSet.ParcelID", instrumentNo);
		m.put("OtherInformationSet.SrcType","TR");
		
		try {
			addressARPulaskiTR(finalDoc, m,searchId);
			nameARPulaskiTR(finalDoc, m, searchId);
			legalARPulaskiTR(finalDoc, m, searchId);
			taxInfoARPulaskiTR(finalDoc, m,searchId);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void nameARPulaskiTR(org.w3c.dom.Document finalDoc, ResultMap m, long searchId) throws Exception {
		
		String name = HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_1");
		m.put("PropertyIdentificationSet.OwnerLastName", name);
		
		GenericFunctions2.parseName(m, name);
	}
	

	public static void addressARPulaskiTR( org.w3c.dom.Document finalDoc, ResultMap m, long searchId) throws Exception {
		
		String origAddress = HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_7");
		String mailingCity = HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_4");
		
		parseAddress(m,origAddress,mailingCity);
	}
	
	public static void parseAddress(ResultMap m, String origAddress, String mailingCity) throws Exception {
		
		String address=origAddress, city = "", zip ="";
		
		Pattern commonCitiesPattern = Pattern.compile("(?i)(?:(N(ORTH)? )?LITTLE ROCK)|(?:MAUMELLE)|(?:SHERWOOD)|(?:JACKSONVILLE)|(?:MABELVALE)|(?:WRIGHTSVILLE)|(?:SCOTT[ ,]?(\bAR\b)?\\s?(?:\\d+)?)");
		Matcher commonCitiesMatcher =commonCitiesPattern .matcher(address);  
		if(commonCitiesMatcher.find()) {
			city = commonCitiesMatcher.group();
			if(city.contains("SCOTT")) city = "SCOTT";
		} else if(address.contains(mailingCity)) {
			city = mailingCity;
		}
		address = address.replaceAll(city,"");
		
		if(address.indexOf(",")>0) {
			String firstPart = address.substring(0,address.indexOf(","));
			String lastPart = address.substring(address.indexOf(","));
			address = firstPart;
			Pattern zipPattern = Pattern.compile("[0-9]+");
			Pattern cityPattern = Pattern.compile("\\[a-zA-Z]{3,}");
			Matcher zipMatcher = zipPattern.matcher(lastPart);
			Matcher cityMatcher = cityPattern.matcher(lastPart);
			if(zipMatcher.find()) {
				zip = zipMatcher.group();
			}
			if(city.isEmpty() && cityMatcher.find()) {
				city = cityMatcher.group();
			}
		}
		
		String streetNo = StringFormats.StreetNo(address);
		
		Pattern highWayNoPattern = Pattern.compile("(?i)HIGHWAY\\s+(\\d+)");
		Matcher highWayNoMatcher = highWayNoPattern.matcher(address);
		if(StringUtils.isEmpty(streetNo)) {
			if(highWayNoMatcher.find()) {
				streetNo = highWayNoMatcher.group(1);
			}
		}else {
			if(highWayNoMatcher.find()) {
				address = address.replaceAll("\\b(HWY|HW)\\b", " ");
				address = address.replaceAll(highWayNoMatcher.group(),highWayNoMatcher.group()+" HWY ");
			}
		}
		if(!StringUtils.isEmpty(streetNo)) {
			address = address.replaceAll(streetNo,"");
		}
		
		if(zip.isEmpty()) {
			Pattern zipPattern = Pattern.compile("([\\-0-9]+){5,}");
			Matcher zipMatcher = zipPattern.matcher(address);
			while(zipMatcher.find()) {
				zip = zipMatcher.group();
			}
			address = address.replaceAll(zip,"");
		}
		
		m.put("PropertyIdentificationSet.StreetName",address);
		m.put("PropertyIdentificationSet.StreetNo",streetNo);
		m.put("PropertyIdentificationSet.City",city);
		m.put("PropertyIdentificationSet.Zip",zip);
		m.put("PropertyIdentificationSet.AddressOnServer",origAddress);
	}

	public static void legalARPulaskiTR( org.w3c.dom.Document finalDoc, ResultMap m, long searchId) throws Exception {
		
		String legal =  HtmlParserTidy.getValueFromTagByName(finalDoc, "P_50","textarea").replaceAll("\\\\n", " ").trim();
		String legal_aux =  HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_9") + " "
							+ HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_10") + " "
							+ HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_11");
							
		String name = HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_47");
		String lot = HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_45");
		String block = HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_46");
		String phase = "", tract = "";
		
		name = name.replaceAll("(?i)\\s+SUB\\b.*", "");
		name = name.replaceAll("\\(UNREC[^\\)\\s]*\\)?","");
		
		/* Lot */ 
		if(StringUtils.isEmpty(lot)) {
			Pattern lotPattern = Pattern.compile("(?i)(?:LOT|LT|LTS)(?:\\s|-)*(\\d+(?:\\s*-\\s*\\d+)?(?:\\s*&\\s*\\d+)?)");
			Matcher lotMatcher = lotPattern.matcher(legal_aux);
			while(lotMatcher.find()) {
				if(!lot.contains(lotMatcher.group(1)))
				lot += lotMatcher.group(1) + " ";
			}
			if(StringUtils.isEmpty(lot)) {
				lotMatcher = lotPattern.matcher(legal);
				while(lotMatcher.find()) {
					lot = lotMatcher.group(1);
				}
			}
		}
		lot = lot.replaceAll("(?i)LOT\\-?", "");
		
		/* Block */
		if(StringUtils.isEmpty(block)) {
			Pattern blockPattern = Pattern.compile("(?i)(?:BLK)(?:\\s|-)*(\\d+(?:\\s*-\\s*\\d+)?(?:\\s*&\\s*\\d+)?)");
			Matcher blockMatcher = blockPattern.matcher(legal_aux);
			while(blockMatcher.find()) {
				if(!block.contains(blockMatcher.group(1)))
				block += blockMatcher.group(1) + " ";
			}
		}
		if("0".equals(block)) {
			block = "";
		}
		if("0".equals(lot)) {
			lot = "";
		}
		
		/* Phase */ 
		Pattern phasePattern = Pattern.compile("(?i)PH(?:ASE)?\\s*(((?:I|V|X)+)|(?:\\d+(\\-\\w+)?))");
		Matcher phaseMatcher = phasePattern.matcher(legal);
		if(phaseMatcher.find()) {
			phase = phaseMatcher.group(1);
		}else {
			phaseMatcher = phasePattern.matcher(name);
			if(phaseMatcher.find()) {
				phase = phaseMatcher.group(1);
				name = name.replaceAll(phaseMatcher.group(),"");
			}
		}
		
		/* Tract */
		Pattern tractPattern = Pattern.compile("TR(?:ACT)?\\s+((?:\\d|\\w)+(?:\\-\\d+)?)");
		Matcher tractMatcher = tractPattern.matcher(name);
		if(tractMatcher.find()) {
			tract = tractMatcher.group(1);
			if(!StringUtils.isEmpty(tract)) {
				name = name.replaceAll(tractMatcher.group(), "");
			}
		}else {
			tractMatcher = tractPattern.matcher(legal);
			if(tractMatcher.find()) {
				tract = tractMatcher.group(1);
			}
		}
				
		m.put("PropertyIdentificationSet.PropertyDescription",legal);
		m.put("PropertyIdentificationSet.SubdivisionName",name);
		m.put("PropertyIdentificationSet.SubdivisionLotNumber",lot);
		m.put("PropertyIdentificationSet.SubdivisionPhase",phase);
		m.put("PropertyIdentificationSet.SubdivisionTract",tract);
		m.put("PropertyIdentificationSet.SubdivisionBlock",block);
	}
	
	public static void taxInfoARPulaskiTR( org.w3c.dom.Document finalDoc, ResultMap m, long searchId) throws Exception {
		String taxYear = HtmlParserTidy.getValueFromInputByName(finalDoc, "TXF_23");
		try {
			if(taxYear.length()==2) {
				if(Integer.parseInt(taxYear)>30) {
					taxYear = "19"+taxYear;
				}else {
					taxYear = "20"+taxYear;
				}
			}
		}catch(Exception e) {}
		
		String general =HtmlParserTidy.getValueFromTagById(finalDoc, "P_17_1","span").replaceAll(",", "");
			/* HtmlParserTidy.getValueFromInputByName(finalDoc, "P_6").replaceAll(",", ""); */
		String value = "0.0";
		try {
			value= HtmlParserTidy.getValueFromInputByName(finalDoc, "P_54").replaceAll(",", "");
		} catch (NullPointerException npe) {}
		String  crtYearBalance = "0.0";
		try {
			crtYearBalance = HtmlParserTidy.getValueFromInputByName(finalDoc, "P_27").replaceAll(",", "");
		} catch (NullPointerException npe) {}
		 
		String balance = HtmlParserTidy.getValueFromInputByName(finalDoc, "TOTAL_P_27").replaceAll(",", "");
		double priorDelinquent = 0.00d;
		try {
			priorDelinquent = Double.parseDouble(balance) - Double.parseDouble(crtYearBalance);
		}catch(Exception ignored) {}
		
		
		m.put("TaxHistorySet.Year",taxYear);
		m.put("TaxHistorySet.BaseAmount",general);
		m.put("TaxHistorySet.TotalDue",crtYearBalance);
		m.put("TaxHistorySet.CurrentYearDue",crtYearBalance);
		m.put("TaxHistorySet.PriorDelinquent",priorDelinquent+"");
		m.put("PropertyAppraisalSet.TotalAssessment",value);
		
		
		String receiptsHtmlTable =  HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(finalDoc, "receipts" , "table"));
		ResultTable receipts = new ResultTable();
		Map<String, String[]> map = new HashMap<String, String[]>();
		String[] header = { "Year", "Transaction Type", "ReceiptNumber", "ReceiptDate", "ReceiptAmount" };
		List<List<String>> bodyRT = HtmlParser3.getTableAsList(receiptsHtmlTable,false);
		for(List<String> rows : bodyRT) {
			String amount = rows.get(rows.size()-1);
			rows.set(rows.size()-1, clean(amount));
		}
			
		map.put("ReceiptNumber", new String[] {"ReceiptNumber", "" });
		map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
		map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
		receipts.setHead(header);
		receipts.setMap(map);
		receipts.setBody(bodyRT);
		receipts.setReadOnly();
		m.put("TaxHistorySet", receipts);
		
		double amountPaid = 0.00d;
		for(List<String> receipt : bodyRT) {
			String year = receipt.get(0);
			String type = receipt.get(1);
			String amount = receipt.get(4);
			if("payment".equalsIgnoreCase(type)) {
				if(taxYear.endsWith(year) ) {
					try { amountPaid += Double.parseDouble(amount); } catch(Exception ignored) { }
				}else {
					break;
				}
			}
		}
		
		m.put("TaxHistorySet.AmountPaid",amountPaid+"");
		
	}
	
	public static String clean(String str) {
		if(StringUtils.isEmpty(str)) return "";
		return 
			str .trim()
				.replaceAll("<br>", "\n")
				.replaceAll("[\\-\\$,]", "");
	}
	
	/* Pre-parsing functions */
	
	public static String gridToTable(String html, String initialResponse,boolean hasTotalsOnLastRow, String gridId,  String firstElementId) {
		int gridRows = 0; 
		try {
			Pattern gridRowsPattern = Pattern.compile("parent.PutMvals\\(\""+firstElementId+"\",\".*?\",(\\d+),\\d+\\);"); 
			Matcher gridRowsMatcher = gridRowsPattern.matcher(initialResponse);
			if(gridRowsMatcher.find()) {
				gridRows = Integer.parseInt(gridRowsMatcher.group(1));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return gridToTable(html, initialResponse,hasTotalsOnLastRow,gridId, gridRows);
	}
	
	public static String gridToTable(String html, String initialResponse, boolean hasTotalsOnLastRow,String gridId, int gridRows) {
		String table = "";
		try {
			String grid = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(html, gridId , "div"));
			grid = grid.replaceAll("(?ism)<table class=\"gridbottom\".*?</table>","");
			
			int headerStart = grid.indexOf("<tr>");
			int headerEnd = grid.indexOf("</tr>",headerStart);
			int firstRowStart = grid.indexOf("<tr>",headerEnd);
			int firstRowEnd = grid.indexOf("</tr>",firstRowStart);
			
			table= "<table border=0 cellpadding=0 cellspacing=0 >";
			if (!StringUtils.isEmpty(grid)) {
				table +=  grid.substring(headerStart, headerEnd);
				String row = grid.substring(firstRowStart, firstRowEnd);
				for(int i = 0; i<gridRows; i++) {
					table += row;
				}
			}
						
			if(hasTotalsOnLastRow) {
				int lastRowStart = grid.lastIndexOf("<tr>");
				int lastRowEnd = grid.lastIndexOf("</tr>");
				String lastRow = grid.substring(lastRowStart,lastRowEnd);
				table += lastRow;
			}
			table += "</table>";
			
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}
		return table;
		
	}
	
	public static String fillTableFromGridData(String table, String initialResponse, String htmlDetails, int gridJsIndex) {
		String filledTable = table;
		List<String> totalsNeeded = new ArrayList<String>();
		Pattern totalPattern = Pattern.compile("<input type=\"text\" name=\"TOTAL_(.*?)\"[^>]*>");
		Matcher totalMatcher = totalPattern.matcher(htmlDetails);
		while(totalMatcher.find()) {
			totalsNeeded.add(totalMatcher.group(1));
		}
		try {
			Pattern gridJsIdsPattern = Pattern.compile("Gflds\\.splice\\("+gridJsIndex+",0,((?:\".*?\",?)*)\\)");
			Matcher gridJsIdsMatcher = gridJsIdsPattern.matcher(htmlDetails);
			if(gridJsIdsMatcher.find()) {
				String ids = gridJsIdsMatcher.group(1);
				Pattern gridJsIdPattern = Pattern.compile("\"(.*?)\"");
				Matcher gridJsIdMatcher =  gridJsIdPattern.matcher(ids);
				while(gridJsIdMatcher.find()) {
					String id = gridJsIdMatcher.group(1);
					double total = 0;
					
					Pattern valuesPattern = Pattern.compile("parent.PutMvals\\(\""+id+"\",\"(.*?)\",(\\d+),\\d+\\);"); 
					Matcher valuesMatcher = valuesPattern.matcher(initialResponse);
					if(valuesMatcher.find()) {
						String values = valuesMatcher.group(1);
						int valuesNo = 0;
						try {
							valuesNo = Integer.parseInt(valuesMatcher.group(2));
						}catch(Exception ignored) {}
						String[] valArray = values.split(""+(char)65533);
						List<String> valList = new ArrayList<String>();
						for(int i=1;i<=valuesNo;i++) {
							if(i<valArray.length) {
								valList.add(valArray[i]);
							}else{
								valList.add(" ");
							}
						}
						for(String value : valList) {
							if(totalsNeeded.contains(id)) {
								try {
									total += Double.parseDouble(value.replaceAll(",", ""));
								}catch(Exception ignored) {}
							}
							if(!value.isEmpty()) {
								filledTable = filledTable.replaceFirst("<input size=\"(\\d+)\" name=\""+id+"\" type=\"text\">","<input type=\"text\" name=\""+id+"\" size=\"$1\" value=\""+value+"\">");
							}
						}
					}
					
					if(totalsNeeded.contains(id)) {
						DecimalFormat df = new DecimalFormat("#,###.##");
						filledTable = filledTable.replaceAll("<input type=\"text\" name=\"(TOTAL_.*?)\"[^>]*?size=\"(\\d+)\"[^>]*?>","<input type=\"text\" name=\"$1\" size=\"$2\" value=\""+df.format(total)+"\">");
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return filledTable;
	}

	public static String getReceiptsTable(String billHistoryTable, String instrumentNo, String serverName, long searchId) {
		String receiptsTable = "<table id=\"receipts\">";
		try {
			receiptsTable +="<tr><td>Year</td><td>Transaction Type</td><td>Receipt #</td><td>Date</td><td>Amount</td></tr>";
			List<Node> yearsList = HtmlParserTidy.getNodeListByTagAndAttr(billHistoryTable,"input","name","P_4");
			List<Node> transactionsList = HtmlParserTidy.getNodeListByTagAndAttr(billHistoryTable,"input","name","P_12");
			for(int i=0; i<yearsList.size(); i++) {
				try {
					String year = yearsList.get(i).getAttributes().getNamedItem("value").getNodeValue();
					int transactions = Integer.parseInt(transactionsList.get(i).getAttributes().getNamedItem("value").getNodeValue());
					String receipts = "";
					HttpSite site = HttpManager.getSite(serverName, searchId);
					try {
						receipts = ((ro.cst.tsearch.connection.http2.ARPulaskiTR)site).getReceiptsInfo(instrumentNo,"WTKCB_22_"+(i+1));
					}finally {
						HttpManager.releaseSite(site);
					}
					for(int rec=1; rec<=transactions; rec++) {
						Pattern receiptsPat = Pattern.compile("parent.PutFormVar\\(\"P_4(4|5|6|7)_"+rec+"\",\"(.*?)\",0\\);");
						Matcher receiptsMat = receiptsPat.matcher(receipts);
						String type ="", no ="", date ="", amount = "";
						while(receiptsMat.find()) {
							String what = receiptsMat.group(1);
							String value = receiptsMat.group(2);
							if("4".equals(what)) {
								type = value;
							}if("5".equals(what)) {
								no = value;
							}if("6".equals(what)) {
								date = value;
							}if("7".equals(what)) {
								amount = value;
							}
						}
						receiptsTable +="<tr><td>"+year+"</td><td>"+type+"</td><td>"+no+"</td><td>"+date+"</td><td>"+amount+"</td></tr>";
					}
				}catch(Exception ignored) {}						
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		receiptsTable +="</table>";
		
		return receiptsTable;
	}
	
	public static String getChargePage(String instrumentNo, String origOrCurrent, String serverName, long searchId) {
		
		String chargePage = "";
		String chargeData = "";
		boolean wantOriginal = "original".equals(origOrCurrent);
		
		HttpSite site = HttpManager.getSite(serverName, searchId);
		try {
			chargeData = ((ro.cst.tsearch.connection.http2.ARPulaskiTR)site).getChargeForLastYear(instrumentNo,origOrCurrent);
		}finally {
			HttpManager.releaseSite(site);
		}
		
		try {
			chargePage = "<b><h3>"+(wantOriginal?"Original charge":"Current charge")+"</h3></b><br>";
			chargePage += "<table><tr><td width='20'>Year</td><td width='30'>General</td><td width='30'>Special</td><td width='30'>Penalty</td><td width='30'>Cost</td><td width='30'>Interest</td><td width='30'>Total</td></tr>";
			if(wantOriginal) {
				chargePage += "<tr><td><span id='P_16_1'></span></td><td><span id='P_17_1'></span></td><td><span id='P_18_1'></span></td><td><span id='P_19_1'></span></td><td><span id='P_21_1'></span></td><td><span id='P_20_1'></span></td><td><span id='P_23_1'></span></td></tr>";
			}else {
				chargePage += "<tr><td><span id='P_37_1'></span></td><td><span id='P_38_1'></span></td><td><span id='P_39_1'></span></td><td><span id='P_40_1'></span></td><td><span id='P_41_1'></span></td><td><span id='P_42_1'></span></td><td><span id='P_43_1'></span></td></tr>";
			}
			chargePage += "</table>";
			
			Pattern origChargePat = Pattern.compile("parent.PutFormVar\\(\"(P_(?:16|17|18|19|21|20)_1)\",\"(.*?)\",0\\);");
			Pattern currChargePat = Pattern.compile("parent.PutFormVar\\(\"(P_(?:37|38|39|40|41|42)_1)\",\"(.*?)\",0\\);");
			Matcher chargeGridMat = origChargePat.matcher(chargeData);
			if(!wantOriginal) chargeGridMat = currChargePat.matcher(chargeData);
			
			double total = 0.00d;
			while(chargeGridMat.find()) {
				String what = chargeGridMat.group(1);
				String value = chargeGridMat.group(2);
				chargePage = chargePage.replaceFirst("<span id='"+what+"'></span>", Matcher.quoteReplacement("<span id='"+what+"'>"+value+"</span>"));
				if((wantOriginal && !"P_16_1".equalsIgnoreCase(what)) || (!wantOriginal && !"P_37_1".equalsIgnoreCase(what))) {
					try { total += Double.parseDouble(value);} catch(Exception e) {}
				}
			}
			chargePage = chargePage.replaceFirst("<span id='P_(23|43)_1'></span>",Matcher.quoteReplacement("<span id='P_$1_1'>"+total+"</span>")); 
			
			Pattern gridChargesCountPattern = Pattern.compile("P_(?:13|28)_(\\d+)");
			Matcher gridChargesCountMatcher = gridChargesCountPattern.matcher(chargeData);
			int gridChargesCount = 0;
			while(gridChargesCountMatcher.find()) {
				try {
					gridChargesCount = Integer.parseInt(gridChargesCountMatcher.group(1));
				}catch(Exception e) {}
			}
			
			chargePage += "<br><b>Entity Information</b>";
			chargePage += "<table>";
			chargePage += "<tr><td allign='center'>#</td><td align='center'><span id='L_P_7'>Code</span></td><td  align='center'><span id='L_P_13'>Entity</span></td><td  align='center'><span id='L_P_14'>Taxes</span></td></tr>";
			
			for(int i=1; i<=gridChargesCount ; i++) {
				Pattern chargePat = Pattern.compile("parent.PutFormVar\\(\"P_(7|13|14|28|33|35)_"+i+"\",\"(.*?)\",0\\);");
				Matcher chargeMat = chargePat.matcher(chargeData);
				String code ="", entity ="", taxes ="";
				while(chargeMat.find()) {
					String what = chargeMat.group(1);
					String value = chargeMat.group(2);
					if("7".equals(what) || "28".equals(what)) {
						code = value;
					}if("13".equals(what) || "33".equals(what)) {
						entity = value;
					}if("14".equals(what) || "35".equals(what)) {
						taxes= value;
					}
				}
				chargePage += "<tr><td>"+i+"</td><td><span id='P_7"+i+"'>"+code+"</span></td><td><span id='P_13"+i+"'>"+entity+"</span></td><td><span id='P_14"+i+"'>"+taxes+"</span></td></tr>";
			}
			chargePage += "</table>";
			
		}catch(Exception e) {
			e.printStackTrace();
		}
				
		return chargePage;
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 9) {
			resultMap.put("PropertyIdentificationSet.ParcelID",cols[1].toPlainTextString().trim());
			resultMap.put("PropertyIdentificationSet.ParcelID2",cols[1].toPlainTextString().trim());
				
			String nameOnServer = cols[3].toPlainTextString().trim().replace("&amp;", "&");
			
			nameOnServer = nameOnServer.replace("&amp;", "&");
			resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);
			String fullAddress = cols[5].toPlainTextString().trim().replace("&amp;", "&");
			String[] address = StringFormats.parseAddress(fullAddress);
			resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
			resultMap.put("PropertyIdentificationSet.StreetName", address[1]);
			resultMap.put("PropertyIdentificationSet.OwnerLastName", nameOnServer);
			
			try {
				GenericFunctions2.parseName(resultMap, nameOnServer);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		resultMap.removeTempDef();
		return resultMap;
	}
}

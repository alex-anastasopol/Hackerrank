package ro.cst.tsearch.servers.functions;

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;

public class FLPalmBeachTR extends ParseClass {

	public static final String[] CITIES = {"Atlantis", "Belle Glade", "Boca Raton", "Boynton Beach", "Briny Breezes",			//incorporated
										   "Cloud Lake", "Delray Beach", "Glen Ridge", "Golf", "Greenacres",
										   "Gulf Stream", "Haverhill", "Highland Beach", "Hypoluxo", "Juno Beach",
										   "Jupiter", "Jupiter Inlet Colony", "Lake Clarke Shores", "Lake Park", "Lake Worth",
										   "Lantana", "Loxahatchee Groves", "Manalapan", "Mangonia Park", "North Palm Beach",
										   "Ocean Ridge", "Pahokee", "Palm Beach", "Palm Beach Gardens", "Palm Beach Shores",
										   "Palm Springs", "Riviera Beach", "Royal Palm Beach", "South Bay", "South Palm Beach",
										   "Tequesta", "Wellington", "West Palm Beach", "Loxahatchee", 
										   "Belle Glade Camp", "Boca Del Mar", "Boca Pointe", "Canal Point", "Century Village",	//unincorporated
										   "Cypress Lakes", "Dunes Road", "Fremd Village-Padgett Island", "Golden Lakes", "Gun Club Estates",
										   "Hamptons at Boca Raton", "High Point", "Juno Ridge", "Kings Point", "Lake Belvedere Estates",
										   "Lake Harbor", "Lake Worth Corridor", "Lakeside Green(", "Limestone Creek", "Mission Bay",
										   "Plantation Mobile Home Park","Royal Palm Estates", "Sandalfoot Cove", "Schall Circle", "Seminole Manor",
										   "Stacey Street", "Villages of Oriole", "Westgate-Belvedere Homes", "Whisper Walk"}; 
	
	private static FLPalmBeachTR _instance = null;

	private FLPalmBeachTR() {
	}

	public static FLPalmBeachTR getInstance() {
		if (_instance == null) {
			_instance = new FLPalmBeachTR();
		}
		return _instance;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		
		HtmlParser3 parser = new HtmlParser3(response);
		String currentOwner = parser.getValueFromAbsoluteCell(0, 0, "Owner of Record");
		currentOwner = currentOwner.replaceAll("(?is)<em>.*?</em>\\s*<br/?>", "").replaceAll("&amp;", "&").trim();
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), currentOwner);
		
		String addressRaw = parser.getValueFromAbsoluteCell(0, 0, "Property Address:");
		addressRaw = addressRaw.replaceAll("(?is)<em>.*?</em>\\s*<br/?>", "").trim();
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addressRaw);
				
		parseName("", resultMap);
		parseAddress("", resultMap);

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "accountInformation"), true);
			if (tableList.size()>0) {
				TableTag table = (TableTag)tableList.elementAt(0);
				String legalDescription = table.getRows()[4].getColumns()[0].toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDescription);
				parseLegalDescription("", resultMap);
				
				String propertyType = table.getRows()[1].getColumns()[1].toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), propertyType);
			}
			
			tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxBills"), true);
			if (tableList.size()>0) {
				TableTag table = (TableTag)tableList.elementAt(0);
				
				int row = 1;
				if(table.getRows().length > row){
					if (table.getRows()[row].getColumnCount()<8)		//current row contains "This record is blocked from view."
						row++;
				}
				
				if(table.getRows().length > row && table.getRows()[row].getColumnCount() > 7){
					TableColumn[] tcs = table.getRows()[row].getColumns();
					
					String baseAmount = tcs[3].toPlainTextString().trim().replaceAll("[\\$,]", "");
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
					
					String amountDue = tcs[7].toPlainTextString().trim().replaceAll("[\\$,]", "");
					if (amountDue.matches("\\([^)]+\\)")) {		//negative number
						amountDue = "0.00";
					}
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
				}
				
				StringBuilder sb = new StringBuilder();
				for (int i=row+1;i<table.getRowCount();i++) {
					TableRow tr = table.getRows()[i];
					if (tr.getColumnCount()>=8)
						sb.append(tr.getColumns()[7].toPlainTextString().trim().replaceAll("[\\$,]", "")).append("+");
				}
				String sum = sb.toString().replaceFirst("\\+\\z", "");
				sum = GenericFunctions.sum(sb.toString(), searchId);
				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), sum);
				
			}
			
			String deedNumber = org.apache.commons.lang.StringUtils.defaultString(
								HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Deed Number:"), "", true));
			if(deedNumber.split("(?ism)<br[^>]*>").length==2){
				deedNumber = deedNumber.split("(?ism)<br[^>]*>")[1].trim();
				if (StringUtils.isNotEmpty(deedNumber) && !"0".equals(deedNumber))
					resultMap.put(CrossRefSetKey.INSTRUMENT_NUMBER.getKeyName(), org.apache.commons.lang.StringUtils.defaultString(deedNumber.trim()));
			}
			
		} catch (ParserException e) {
			e.printStackTrace();
		}
				
		// saleDataInfo
		parseSaleDataInfo(resultMap, parser);

		// currentYear
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), getCurrentYear(response));
		
		// taxhistorySet
		parseTaxHistorySet(response, resultMap, searchId);
		return;
	}

	@SuppressWarnings("rawtypes")
	private void parseTaxHistorySet(String response, ResultMap resultMap, long searchId) {

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList nodeTaxes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "payments"));
			
			ResultTable rt = new ResultTable();					//tax history table
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			String taxYear = (String)resultMap.get(TaxHistorySetKey.YEAR.getKeyName());
			StringBuilder sb = new StringBuilder();
			sb.append("0.0");
			
			if (nodeTaxes != null)
			{
				String year = "";
				String billNumber = "";
				String receiptNumber = "";
				String receiptAmount = "";
				String receiptDate = "";
				for (int i=0;i<nodeTaxes.size();i++)
				{
					TableTag table = (TableTag)nodeTaxes.elementAt(i);
					for (int j=1;j<table.getRowCount();j++) {
						TableRow row = table.getRow(j);
						year = row.getColumns()[0].toPlainTextString().trim();
						billNumber = row.getColumns()[1].toPlainTextString().trim();
						receiptNumber = row.getColumns()[2].toPlainTextString().trim();
						receiptAmount = row.getColumns()[3].toPlainTextString().replaceAll("[\\$,]", "").trim();
						receiptDate = row.getColumns()[4].toPlainTextString().trim();
						if (year.equals(taxYear)) {
							sb.append("+").append(receiptAmount);
						}
						
						list = new ArrayList<String>();
						list.add(year);
						list.add(billNumber);
						list.add(receiptNumber);
						list.add(receiptAmount);
						list.add(receiptDate);
						
						tablebody.add(list);
					}
				}	
			}
			
			String sum = sb.toString();
			sum = GenericFunctions.sum(sb.toString(), searchId);
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), sum);

			String[] header = {TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.TAX_BILL_NUMBER.getShortKeyName(), 
					TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName(),  
					TaxHistorySetKey.RECEIPT_DATE.getShortKeyName()};
			rt = GenericFunctions2.createResultTable(tablebody, header);
			if (rt != null){
				resultMap.put("TaxHistorySet", rt);
			}

		} catch (ParserException e) {
			e.printStackTrace();
		}
	}

	private void parseSaleDataInfo(ResultMap resultMap, HtmlParser3 parser) {
		Node saleDataInfoTable = HtmlParser3.getNodeByTypeAndAttribute(parser.getNodeList(), "TABLE", "class", "saleDataInfo", true);
		if (saleDataInfoTable instanceof TableTag) {

			List<HashMap<String, String>> saleDataInfoMap = HtmlParser3.getTableAsListMap((TableTag) saleDataInfoTable);

			for (HashMap<String, String> hashMap : saleDataInfoMap) {
				String string = hashMap.get("Book/page");
				if (StringUtils.isNotEmpty(string)) {
					String[] split = string.split("/");
					if (split.length == 2) {
						hashMap.put("Book", StringUtils.removeLeadingZeroes(split[0].trim()));
						hashMap.put("Page", StringUtils.removeLeadingZeroes(split[1].trim()));
					}
				}
			}

			String[] header = new String[] { "RecordedDate", "Book", "Page", "SalesPrice", "DocumentType", "Grantee" };
			Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String, String>();
			resultBodyHeaderToSourceTableHeader.put("RecordedDate", "Sales Date");
			resultBodyHeaderToSourceTableHeader.put("Book", "Book");
			resultBodyHeaderToSourceTableHeader.put("Page", "Page");
			resultBodyHeaderToSourceTableHeader.put("SalesPrice", "Price");
			resultBodyHeaderToSourceTableHeader.put("DocumentType", "Sale Type");
			resultBodyHeaderToSourceTableHeader.put("Grantee", "Owner");
			ResultBodyUtils.buildInfSet(resultMap, saleDataInfoMap, header, resultBodyHeaderToSourceTableHeader, SaleDataSet.class);

		}
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse response, String table, long searchId, MessageFormat format) {

		String startLink = createPartialLink(format, TSConnectionURL.idGET);
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		List<String> list = RegExUtils.getMatches("(?is)<tr(.*?)</tr>", table, 0);

		for (String row : list) {
			ParsedResponse currentResponse = new ParsedResponse();
			String rowHtml = row;
			String rawParcelId = "";
			if (rowHtml.toLowerCase().contains("<th"))					//header
				continue;
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
			
			List<String> cells = RegExUtils.getMatches("(?is)<td.*?>(.*?)</td>", row, 1);
			ResultMap resultMap = new ResultMap();

			String parcelId = "";
			String owner1="";
			String owner2 = "";
			if (cells.size() == 5) {
				int i = 0;
				owner1 = cells.get(i++).trim();
				owner2 = cells.get(i++).trim();
				if(owner1.equals(owner2)) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner1);	
				} else {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner1 + "<br>" + owner2);
				}
				
				i++;
				String addressOnServer = cells.get(i++);
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addressOnServer);
				rawParcelId = cells.get(i++);
				parcelId = rawParcelId.replaceAll("-", "").trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);
			}
			// saveTestDataToFiles(resultMap);

			getInstance().parseAddress("", resultMap);
			getInstance().parseName("", resultMap);
			
			String param1 = "";
			Matcher ma1 = Pattern.compile("(?is)<tr.*?a=\"(.*?)\"").matcher(rowHtml);
			if (ma1.find()) {
				param1 = ma1.group(1).trim();
			}
			String param2 = "";
			Matcher ma2 = Pattern.compile("(?is)<tr.*?p=\"(.*?)\"").matcher(rowHtml);
			if (ma2.find()) {
				param2 = ma2.group(1).trim();
			}
			String link = startLink + "http://taxcollectorpbc.manatron.com/tabs/propertyTax/accountdetail.aspx";
			link += "?p=" + param2;
			link += "&a=" + param1;
			
			if (!owner1.equals(owner2))
				try {
					link += "&owner2=" + URLEncoder.encode(URLEncoder.encode(owner2, "UTF-8"), "UTF-8");// owner2;
				} catch (Exception e) {
					e.printStackTrace();
				}	
			rowHtml = rowHtml.replaceAll(rawParcelId, "<a href=\"" + link + "\">" + rawParcelId + "</a>");
			
			currentResponse.setOnlyResponse(rowHtml);

			currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			currentResponse.setParentSite(response.isParentSiteSearch());
			try {
				document = bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			intermediaryResponse.add(currentResponse);
		}

		String tableHeader = RegExUtils.getFirstMatch("(?is)<tr><th.*</th></tr>", table, 0);
		
		response.getParsedResponse().setHeader("<table>" + tableHeader);
		response.getParsedResponse().setFooter("</table>");
		return intermediaryResponse;
	}

	protected void saveTestDataToFiles(ResultMap map) {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String pin = "" + map.get("PropertyIdentificationSet.ParcelID");
			String text = pin + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") + "\r\n\r\n\r\n";
			String path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);

			// text = pin + "\r\n" +
			// map.get("PropertyIdentificationSet.AddressOnServer") +
			// "\r\n\r\n\r\n";
			// ro.cst.tsearch.utils.FileUtils.appendToTextFile(path +
			// "address.txt", text);

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.NameOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt", text);
			// end test
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void parseName(String name, ResultMap resultMap) {
		if (StringUtils.isEmpty(name)) {
			name = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		}
		String[] namesList = name.split("<br/?>");
		List body = new ArrayList();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		for (String nm: namesList) {
			nm = nm.trim().replaceAll("&(\\r\\n|$)", "");
			names = StringFormats.parseNameNashville(nm, true);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
				NameUtils.isCompany(names[2],new Vector<String>(),true), NameUtils.isCompany(names[5],new Vector<String>(),true), body);
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		String addressRaw = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(addressRaw)) {
			addressRaw = addressOnServer;
		}
		
		if(addressRaw.replaceAll("(?ism)<[^>]*>", "").trim().equals("0")){
			resultMap.remove(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
			return;
		}
		
		addressRaw = addressRaw.replaceAll("(?is)\\bU\\s*S\\s+HIGHWAY\\b", "US HIGHWAY");
		
		String split[] = addressRaw.split("(?is)<br/?>");
		if (split.length>1) {								//617 SW 14TH ST<br>BELLE GLADE FL 33430 (in details)
			addressRaw = split[0];
			String cityZIP = split[1];
			Matcher matcher = Pattern.compile("(?is)(.*?)FL\\s*(\\d{5})?\\s*").matcher(cityZIP);
			if (matcher.find()) {
				if (matcher.group(1)!=null && matcher.group(1).length()!=0)
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), matcher.group(1));
				if (matcher.group(2)!=null && matcher.group(2).length()!=0)
					resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matcher.group(2));
			}
		} else {											//617 SW 14TH ST BELLE GLADE FL 33430 (in intermediary results)
			Matcher matcher = Pattern.compile("(?is)(.*?)FL\\s*(\\d{5})?\\s*").matcher(addressRaw);
			if (matcher.find()) {
				addressRaw = matcher.group(1);							//remove "FL" and ZIP (if any)
				if (matcher.group(2)!=null)
					resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matcher.group(2));
			}
			split = addressRaw.split("\\s");
			int split_len = split.length;
			for (int i=0;i<CITIES.length;i++) {
				String[] split_cities = CITIES[i].split("\\s"); 
				int len = split_cities.length;
				if (split_len>=len) {
					boolean isCity = true;
					for (int j=0;j<len&&isCity;j++)
						if (!split_cities[j].equalsIgnoreCase(split[split_len-len+j]))
							isCity = false;
					if (isCity) {										//remove city
						StringBuilder sb = new StringBuilder();
						for (int j=0;j<split_len-len;j++)
							sb.append(split[j]).append(" ");
						addressRaw = sb.toString().trim();
						sb = new StringBuilder();
						for (int j=0;j<len;j++)
							sb.append(split[split_len-len+j]).append(" ");
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), sb.toString().trim());
					}
				}
			}
		}
		
		addressRaw = addressRaw.replaceAll("-\\s\\d+", "");
		String unitRegEx = "(STE|APT|#|RM)\\s*(.*)";
		String unit = RegExUtils.getFirstMatch(unitRegEx, addressRaw, 2);

		if (StringUtils.isNotEmpty(unit)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
			addressRaw = addressRaw.replaceAll(unitRegEx, "");
		}

		// check to see if address contains lot and clean it
		String parcelId = (String) resultMap.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
		if (StringUtils.isNotEmpty(parcelId)) {
			int length = parcelId.length();

			String lot = parcelId.substring(length - 4, length);
			String regEx = "(?is)(?<=\\s.\\s)(" + lot + ")";

			//String match = RegExUtils.getFirstMatch(regEx, addressRaw, 1);
			// resultMap.put("PropertyIdentificationSet.SubdivisionUnit", lot);

			addressRaw = addressRaw.replaceAll(regEx, "# $1").trim();
		}

		String streetName = StringFormats.StreetName(addressRaw).trim();
		String streetNo = StringFormats.StreetNo(addressRaw);
		if (streetNo.matches("0+"))
			streetNo = "";

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(legal)) {
			legal = legalDescription;
		}
		
		String platBookPage = "PB\\s*([A-Z0-9]+)\\s*PG\\s*?([A-Z0-9]+)";
		
		//initial cleanup legal description
		String originalLegal = legal;
		legal = legal.replaceAll("\\bESTSLT\\b", "ESTS LT");
		legal = legal.replaceAll("\\bREPLWEST\\b", "REPL WEST");
		legal = legal.replaceAll("\\bREPL(" + platBookPage + ")\\b", "REPL $1");
		legal = legal.replaceAll("\\b(\\w+)(LT \\d+ BLK \\d+)", "$1 $2");
		legal = legal.replaceAll("(\\d+)(UNREC)", "$1 $2");
		legal = legal.replaceAll("^(\\d+(ST|ND|RD|TH) )?ADD(ITION)?( TO)?\\s*", "");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC(?:TION)?|UNIT)\\b", "$2 $1");
		legal = legal.replaceAll("(I{1,3}|\\d+[A-Z]?)(UNIT)", "$1 $2");
		legal = legal.replaceFirst("^CONDO(MINIUM)? \\d+ OF (THE )", "");  // e.g. PID=30-43-41-20-11-012-5040
		legal = legal.replaceAll("(?ism)(\\bLO?TS?\\s+\\w+)\\s+THRU\\s+(\\w+\\s+)", "$1-$2");
		legal = legal.replaceAll("(?ism)(\\bLO?TS?\\s+\\w+)\\s+TO\\s+(\\w+\\s+)", "$1-$2");
		legal = GenericFunctions1.replaceNumbers(legal);

		// extract subdivision name
		String subdiv = "";
		Pattern p = Pattern.compile("(.*?)\\s*\\b(LO?TS?|CONDO?(MINIUM)?|PAR(CEL)?|SEC(TION)?|SUB|\\d+-\\d+-\\d+|UNIT|PID|PH(ASE)?|(\\d+(ST|ND|RD|TH) )?ADD(ITION)?|REPL|[SWNE]+ [\\d\\./]+ FT|BLDG|BLK)\\b.*");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			subdiv = ma.group(1);
			
			if(StringUtils.isEmpty(subdiv)){
				p = Pattern.compile("(.*?)\\s*\\b(, ?|LO?TS?|CONDO?(MINIUM)?|PAR(CEL)?|SEC(TION)?|SUB|\\d+-\\d+-\\d+|UNIT|PID|PH(ASE)?|(\\d+(ST|ND|RD|TH) )?ADD(ITION)?|[SWNE]+ [\\d\\./]+ FT|BLDG|BLK)\\b.*");
				ma.usePattern(p);
				ma.reset();
				if(ma.find())
					subdiv = ma.group(1);
			}

			// cleanup subdivision name
			subdiv = subdiv.replaceAll("IN\\s*" + platBookPage, " ");
			subdiv = subdiv.replaceAll("\\s*\\bNO( \\d+)?\\b\\s*", " ");
			subdiv = subdiv.replaceAll("\\s*\\b(UNREC|PUD|POD [A-Z])\\b\\s*", " ");
			subdiv = subdiv.split("\\bPL\\b")[0];
			subdiv = subdiv.replaceAll("\\s*\\bPL(AT)?( \\d+[A-Z]?)?\\b\\s*", " ");
			subdiv = subdiv.replaceAll("\\s*\\b(RE)?PLAT\\b\\s*", " ");
			if(!subdiv.contains(" SECS "))
				subdiv = subdiv.replaceFirst("\\s*\\b([IVXCML]+|\\d+)\\s*$", ""); 		   // if subdivision name ends with a roman or arabic number, remove it
			subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();		   
		} 
		
		

		if (subdiv.length() != 0){		   
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			if (originalLegal.matches(".*\\b(CONDO?(MINIUM)?)\\b.*"))
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
		}

		legal = legal.replaceAll("\\s*[&,]\\s*", " ");	   
		legal = legal.replaceAll("\\s*\\bNO\\b\\s*", " ").trim();
		String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics

		// extract lot, block, sec-twn-rng from pid and add them to info extracted from legal description
//		String lotFromPid = "";
//		String blockFromPid = "";
		String secFromPid = "";
		String twnFromPid = "";
		String rngFromPid = "";
		String pid = (String) resultMap.get("PropertyIdentificationSet.ParcelID");
		if (pid != null && pid.length() != 0){
//			blockFromPid = pid.replaceFirst("\\w{2}-\\w{2}-\\w{2}-\\w{2}-\\w{2}-(\\w{3})-\\w{4}", "$1").replaceFirst(".*-.*", "").replaceFirst("^0+(\\d*)", "$1");
//			lotFromPid = pid.replaceFirst("\\w{2}-\\w{2}-\\w{2}-\\w{2}-\\w{2}-\\w{3}-(\\w{4})", "$1").replaceFirst(".*-.*", "").replaceFirst("^0+(\\d*)", "$1");
			secFromPid = pid.replaceFirst("\\w{2}-\\w{2}-\\w{2}-(\\w{2})-\\w{2}-\\w{3}-\\w{4}", "$1").replaceFirst(".*-.*", "").replaceFirst("^0+(\\d+)", "$1"); 
			twnFromPid = pid.replaceFirst("\\w{2}-\\w{2}-(\\w{2})-\\w{2}-\\w{2}-\\w{3}-\\w{4}", "$1").replaceFirst(".*-.*", "").replaceFirst("^0+(\\d+)", "$1");
			rngFromPid = pid.replaceFirst("\\w{2}-(\\w{2})-\\w{2}-\\w{2}-\\w{2}-\\w{3}-\\w{4}", "$1").replaceFirst(".*-.*", "").replaceFirst("^0+(\\d+)", "$1");
		}

		List<List> body = new ArrayList<List>();	   	   
		List<String> line;

		// extract section, township and range from legal description	   	   
		p = Pattern.compile("\\b(\\d+)-(\\d+)-(\\d+)\\b");
		ma = p.matcher(legal);	   	   	   	   
		while (ma.find()){
			String sec = ma.group(1);
			String twn = ma.group(2);
			String rng = ma.group(3);
			if (!sec.equals(secFromPid) || !twn.equals(twnFromPid) || !rng.equals(rngFromPid)){
				line = new ArrayList<String>();
				line.add(sec);
				line.add(twn);
				line.add(rng);
				body.add(line);
			}
		} 

		// extract section
		p = Pattern.compile("\\bSEC(?:TION)? (\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()){
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			body.add(line);
		}

		line = new ArrayList<String>();
		line.add(secFromPid);
		line.add(twnFromPid);
		line.add(rngFromPid);
		body.add(line);

		try {
			if (!body.isEmpty()){
				String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};

				Map<String,String[]> map = new HashMap<String,String[]>();
				map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
				map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
				map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});

				ResultTable pis = new ResultTable();	
				
				pis.setHead(header);
				pis.setBody(body);
				pis.setMap(map);
				resultMap.put("PropertyIdentificationSet", pis);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("\\bBLDG (\\d+|[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()){
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(1));
		}
		
		if(StringUtils.isEmpty(bldg)){
			p = Pattern.compile("\\bBLDG (\\w+-\\w+)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()){
				bldg = ma.group(1);
			}
		}
			
		if(StringUtils.isNotEmpty(bldg))
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);

		// extract plat book & page from legal description
		p = Pattern.compile("\\b" + platBookPage + "\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()){
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1));
			resultMap.put(PropertyIdentificationSetKey.PLAN_NO.getKeyName(), ma.group(2));
		} 

		// extract cross references B&P 
		ResultTable cr = new ResultTable();    
		List<List> bodyCR = new ArrayList<List>();       
		p = Pattern.compile("\\bOR(\\d+)P(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();	   
		while (ma.find()){
			List<String> ln = new ArrayList<String>();
			ln.add("O");
			ln.add(ma.group(1));
			ln.add(ma.group(2));
			bodyCR.add(ln);
		}

		try {
			if (!bodyCR.isEmpty()){	   
				String [] header = {"Book_Page_Type", "Book", "Page"};
				Map<String,String[]> map = new HashMap<String,String[]>();
				map.put("Book_Page_Type", new String[]{"Book_Page_Type", ""});
				map.put("Book", new String[]{"Book", ""});
				map.put("Page", new String[]{"Page", ""});

				cr.setHead(header);
				cr.setBody(bodyCR);
				cr.setMap(map);
				resultMap.put("CrossRefSet", cr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)? ((\\d+(-?[A-Z])?( \\d+)*)|([A-Z]+))\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()){
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(1).trim());
		}

		String unitFromLegal = "";
		
		// extract unit from legal description
		p = Pattern.compile("\\bUNIT ([\\w-]+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()){
			unitFromLegal =  ma.group(1);
		}
		
		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		
		p = Pattern.compile("\\bLO?TS? (\\d+-[a-zA-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		StringBuilder sb = new StringBuilder();
		while (ma.find()){
			sb.append(" ").append(ma.group(1));
		}
		lot = sb.toString().trim();
		
		if(StringUtils.isEmpty(lot)){
			p = Pattern.compile("\\bLO?TS? ([A-Z]?\\d+[A-Z]?([- ]\\d+)*|[A-Z])\\b");
			ma.usePattern(p);
			ma.reset();
			StringBuilder sb1 = new StringBuilder();
			while (ma.find()){
				sb1.append(" ").append(ma.group(1));
			}
			lot = sb1.toString().trim();
		}

//		//T7238
//		String newLotFromPid = lotFromPid.replaceAll("^0+|0+$", "");
//		String newBlockFromPid = blockFromPid.replaceAll("^0+|0+$", "");
//		
//		if(StringUtils.isEmpty(lot)){
//			p = Pattern.compile("\\b(PAR|APT) ([A-Z]?\\d+[A-Z]?([- ]\\w+)*|[A-Z])\\b");
//			ma.usePattern(p);
//			ma.reset();
//			StringBuilder sb1 = new StringBuilder();
//			while (ma.find()){
//				sb1.append(" ").append(ma.group(2));
//			}
//			lot = sb1.toString().trim();
//			
//			if(StringUtils.isNotEmpty(lot) && StringUtils.isNotEmpty(newLotFromPid)){
//				String parts[] = lot.split(" ");
//				
//				for(String s : parts){
//					if(newLotFromPid.equals(s)){
//						lot = unitFromLegal = s;
//						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unitFromLegal);
//						break;
//					}
//				}
//			}
//		}
//		
//		if(StringUtils.isEmpty(lot) && StringUtils.isNotEmpty(unitFromLegal)){
//			if((newBlockFromPid+newLotFromPid).equals(unitFromLegal) || (unitFromLegal.replace(newBlockFromPid,"").replaceAll("^0+|0+$","").equals(newLotFromPid))){
//				lot = unitFromLegal;
//				blockFromPid = "";
//			} else if(unitFromLegal.contains("-") || unitFromLegal.matches("([a-zA-Z]+)?\\d+([a-zA-Z]+)?")){
//				if(!unitFromLegal.matches("[a-zA-Z]+-?\\d+"))
//					blockFromPid = "";
//				if(unitFromLegal.matches("\\d+-\\d+-[a-zA-Z]+")){
//					unitFromLegal = unitFromLegal.replaceAll("[a-zA-Z]$", "");
//				}
//				unitFromLegal = unitFromLegal.replace("-", "");
//				lot = unitFromLegal;
//			} 
//		}
//		
//		//			   // verify if the lot extracted from PID, having the last zero removed, is already present in the lots list extracted 
//		//			   // from the legal description; if it's not, then add the lot extracted from PID to the list of lots
//		//			   if (lotFromPid.length() != 0){
//		//				   String lotTrimmed = lotFromPid.replaceFirst("(\\d+)0$", "$1");
//		//				   if (!lot.matches(".*\\b" + lotTrimmed + "\\b.*"))
//		//					   lot = lot + " " + lotFromPid;
//		//			   }
//		if (lot.length() == 0 && !legal.contains("PENTHOUSE")){
//			lot = lotFromPid;
//		}	   
//		if(StringUtils.isNotEmpty(lot) && lot.contains("-") && lot.replace("-", "").matches("\\d+[a-zA-Z]") 
//				&& StringUtils.replaceLast(lot.replace("-",""), lot.charAt(lot.length()-1)+"", ((int)lot.charAt(lot.length()-1)-65+1) +"").equals(lotFromPid)){
//			if(legal.contains("CONDO")){
//				blockFromPid = "";
//				lot = lot.replace("-","");
//				unitFromLegal = lot;
//			} else {
//				blockFromPid = lot.split("-")[0];
//				lot = lot.split("-")[1];
//				unitFromLegal = lot;
//			}
//		}
		
		
		if (lot.length() != 0){
			lot = LegalDescription.cleanValues(lot, false, true);
			lot = StringFormats.ReplaceIntervalWithEnumeration(lot);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		
		//unit
		if(StringUtils.isNotEmpty(unitFromLegal)){
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unitFromLegal);
		}

		// extract block from legal description
		String block = "";
		StringBuffer bSb = new StringBuffer();
		p = Pattern.compile("\\bBLK ([A-Z0-9]+(?:-[A-Z0-9]+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()){
			bSb.append(" "+ma.group(1));
		} 
		block = bSb.toString().trim();
//		if (block.length() == 0){ 
//			block = blockFromPid;
//		}
		if (block.length() != 0)
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

	}

	public String getCurrentYear(String serverResult) {
		String year = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(serverResult, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxBills"), true);
			if (tableList.size()>0) {
				TableTag table = (TableTag)tableList.elementAt(0);
				year = table.getRows()[1].getColumns()[0].toPlainTextString().trim();
				return year;
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return "";
	}

}

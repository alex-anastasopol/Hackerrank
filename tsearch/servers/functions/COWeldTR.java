package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.COGenericTylerTechTR;
import ro.cst.tsearch.servers.types.CORouttTR;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.StringUtils;

public class COWeldTR {

	final static Set<String> cities = new HashSet<String>(Arrays.asList(new String[] { "Weld","Brighton","Dacono","Evans","Fort Lupton","Greeley","Longmont","Northglenn","Thornton","Ault","Berthoud ","Eaton","Erie","Firestone","Frederick","Garden City","Gilcrest","Grover","Hudson","Johnstown","Keenesburg","Kersey","La Salle","Lochbuie","Lucerne","Mead","Milliken","Nunn","Pierce","Platteville","Raymer","Severance","Windsor" }));
	static String cityZipRegex = "";
	static {
		for(String city : cities) {
			cityZipRegex += "|" + "(?:"+city+")";
		}
		cityZipRegex = cityZipRegex.replaceFirst("\\|", "");
		cityZipRegex = "(?i)" + "("  +cityZipRegex + ")" + "\\s*(\\d+)?$"; 
	}
	final static Pattern cityZipPattern = Pattern.compile(cityZipRegex);

	
	public static ResultMap parseIntermediaryRow(TableRow row, COGenericTylerTechTR server) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		try {
			if(row.getColumns()[0]!=null) {
				String accountId = clean(HtmlParser3.getFirstTag(row.getColumns()[0].getChildren(), LinkTag.class, true).getLinkText().replaceAll("(?i)ACCOUNT", ""));
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountId);
				if(accountId.startsWith("R")) {
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");	
				}
			}
			
			TableTag table = HtmlParser3.getFirstTag(row.getColumns()[1].getChildren(), TableTag.class, true);
			TableRow innerRow = table.getRow(0);
			TableColumn[] cols = innerRow.getColumns();
			
			server.parseIntermediaryRow(server, resultMap, cols);
		}catch(Exception e) {
			e.printStackTrace();
		}
			
		return resultMap;
	}

	public static void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) throws Exception {
		for(int i=0; i <cols.length ; i++) {
			String contents = "";
			Node colText = HtmlParser3.getFirstTag(cols[i].getChildren(), TextNode.class, true);
			if(colText!=null) {
				contents = clean(colText.getText(),false,false);	
			}
							
			switch(i) {
				case 0:
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), contents);
					break;
				case 1:
					if ("CORouttTR".equals(server.toString())){
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), contents.trim());
						COPitkinAO.parseAddressCORouttTR(resultMap, -1);
					} else if ("COSan MiguelTR".equals(server.toString())) {
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), contents.trim());
						COSanMiguelTR.parseAddress(resultMap, -1);
					} else {
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), contents);
					}
					break;
				case 2:
					if ("CORouttTR".equals(server.toString())){
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), contents);
						COPitkinAO.parseNames(resultMap, -1);
					} else if ("COSan MiguelTR".equals(server.toString())) {
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), contents.trim());
						COSanMiguelTR.parseNames(resultMap, -1);
					} else{
						contents = contents.replaceAll(",", "&").replaceAll("&&", "");
						server.parseName(new HashSet<String>(Arrays.asList(contents.split("&"))),resultMap);
					}
					break;
				case 3:
					if ("CORouttTR".equals(server.toString())){
						contents = contents.replaceAll("\n", " ");
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), contents);
						COPitkinAO.parseLegalSummary(resultMap, -1);
					} else if ("COSan MiguelTR".equals(server.toString())) {
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), contents.trim());
						COSanMiguelTR.parseLegalSummary(resultMap, -1);
					} else{
						server.parseLegal(contents,resultMap);
					}
					break;
			}
		}
	}

	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, COGenericTylerTechTR server) {
		
		try {
			//detailsHtml = detailsHtml.replaceAll("<th(.*?)>", "<td$1>").replaceAll("</th>", "</td>");
			
			HtmlParser3 parser= new HtmlParser3(detailsHtml);
			String instrumentNo = clean(StringUtils.prepareStringForHTML(
								HtmlParser3.getValueFromNextCell(
										HtmlParser3.findNode(parser.getNodeList(), "Account&nbsp;Id"),"",true)));
			
			String parcelNo = clean(StringUtils.prepareStringForHTML(
					HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(parser.getNodeList(), "Parcel&nbsp;Number"),"",true)));

			
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), instrumentNo);
			m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelNo);
			m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			try {
				parseAddress(parser, m,searchId, server);
				parseName(parser, m, searchId,server);
				parseLegal(parser, m, searchId,server);
				parseTaxInfo(parser, m,searchId);
				if ("CORouttTR".equals(server.toString())){
					CORouttTR.parseSaleDataInfo(parser, m,searchId);
				} else{
					parseSaleDataInfo(parser, m,searchId);
				}
				
			}catch(Exception e) {
				e.printStackTrace();;
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	
	public static void parseName(HtmlParser3 parser, ResultMap m, long searchId, COGenericTylerTechTR server) throws Exception {
		
		Node ownersNode = HtmlParser3.findNode(parser.getNodeList(), "Owners");		
		Node addressNode = HtmlParser3.findNode(parser.getNodeList(), "Address");
		TableTag ownerAndOtherInfoTable = HtmlParser3.getFirstParentTag(ownersNode,TableTag.class);
		
		Set<String> allNames = new LinkedHashSet<String>();
		boolean moreNames = false; 
		
		for(TableRow row : ownerAndOtherInfoTable.getRows()) {
			if(row.getColumnCount()>=2) {
				TableColumn firstCol = row.getColumns()[0];
				if(firstCol.getChildren()!=null && firstCol.getChildren().contains(ownersNode)) {
					moreNames = true;
				}
				if(firstCol.getChildren()!=null && firstCol.getChildren().contains(addressNode)) {
					TableColumn secondCol = row.getColumns()[1];
					String addressRawText = secondCol.getStringText();
					String[] split = addressRawText.split("<br>");
					boolean mayContainOwnerName = (split.length ==3)? true : false;
					String address = clean(addressRawText).trim();
					if(address.contains("C/O")  || mayContainOwnerName) {
						String[] possibleNames = address.split("\n");
						for(String str : possibleNames) {							
							if(str.startsWith("C/O") || mayContainOwnerName) {
								mayContainOwnerName = false; // only the first row before <br> is expected to be a name
								allNames.add(str.trim());
							}
						}
					}					
					break;	
				}
				if(moreNames) {
					TableColumn secondCol = row.getColumns()[1];
					String name = clean(secondCol.getStringText()).trim();
					name = name.replaceAll("[\\d\\./]+(?:\\s+INT)?\\b", "");
					if(name.endsWith("&")) {
						name = name.substring(0, name.length()-1);
					}
					allNames.add(name.replaceAll("(?i)AKA", "&").trim());
				}
			} 
		}
		
		int countyId = server.getDataSite().getCountyId();
		if (CountyConstants.CO_San_Miguel==countyId) {
			StringBuilder sb = new StringBuilder();
			for (String s: allNames) {
				sb.append(s).append(" ");
				m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), sb.toString().trim());
			}
		} else if (CountyConstants.CO_Grand==countyId) {
			StringBuilder sb = new StringBuilder();
			for (String s: allNames) {
				sb.append(s).append("\n");
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), sb.toString().trim());
		} else {
			m.put("PropertyIdentificationSet.NameOnServer",Arrays.toString(allNames.toArray()));
		}
		
		server.parseName(allNames,m);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(Set<String> allNames, ResultMap m) throws Exception {
		List<List/*<String>*/> namesList = new ArrayList<List/*<String>*/>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };

		for(String eachName: allNames) {
			eachName = eachName.trim();
			eachName = eachName.replaceAll("(?is)(?:\\d+)?%", "");
			eachName = NameCleaner.cleanName(eachName,  new Vector<String>(), true);
			eachName = NameCleaner.fixScotishLikeNames(eachName);
			eachName = NameCleaner.multipleCommas(eachName);
			eachName = NameCleaner.composedNames(eachName);
			
			try {
				if(!NameUtils.isCompany(eachName)) {
					Pattern fmlNamePattern = Pattern.compile("^(\\w+)\\s&\\s(\\w+)\\s(\\w+)$");
					Matcher fmlNameMatcher = fmlNamePattern.matcher(eachName);
					if(fmlNameMatcher.find()) {
						eachName = fmlNameMatcher.group(3) + " " + fmlNameMatcher.group(1) + " & " + fmlNameMatcher.group(3) + " " + fmlNameMatcher.group(2); 
					}
				}else if (eachName.startsWith("C/O") && !NameUtils.isCompany(eachName)){//  co mesa tr instr# M085746 -- SPENCER ROBIN, C/O SHANE R BRAMLETT					
					Pattern fmlNamePattern2 = Pattern.compile("^(\\w+)\\s(\\w+)\\s(\\w+)$");
					eachName = eachName.replaceAll("C/O", "").trim();
					Matcher fmlNameMatcher = fmlNamePattern2.matcher(eachName);
					if ( fmlNameMatcher.find() ){
						eachName = fmlNameMatcher.group(3) + " " + fmlNameMatcher.group(1) + " " + fmlNameMatcher.group(2);
					}
				}else if (eachName.contains("C/O")){
					String fmlANDFLregEx = "^(\\w+)\\s(\\w+)\\s(\\w+)\\s&\\s(\\w+)\\s(\\w+)$";
					eachName = eachName.replaceAll(" C/O ", " ").replaceAll(" WM ", " ");
					if (RegExUtils.matches(fmlANDFLregEx, eachName)){
						eachName = eachName.replaceAll(fmlANDFLregEx, "$3 $1 $2 & $5 $4");
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			if (eachName.matches("(?is).*\\bATTN:?\\b.*") || eachName.trim().startsWith("C/O")){
				eachName = eachName.replaceAll("\\bATTN:", "");
				eachName = eachName.replaceAll("\\bATTN\\b", "");
				eachName = eachName.replaceAll("\\bC/O\\b", "").trim();
				if (!NameUtils.isCompany(eachName)) {
					eachName = eachName.replaceFirst("^(\\w+)\\s*&\\s*(\\w+)\\s+(\\w+)$", "$1 $3 & $2 $3");
				}
				names = StringFormats.parseNameDesotoRO(eachName, true);
			} else {
				if (NameUtils.isCompany(eachName)) {
					names = new String[]{ "", "", "", "", "", "" };
					names[2] = eachName;
				} else {
					names = StringFormats.parseNameNashville(eachName, true);
				}
			}
			//names = NameCleaner.tokenNameAdjustment(names);
			names = NameCleaner.composedNamesFix(names);
			names = NameCleaner.lastNameFix(names);
			names = NameCleaner.removeUnderscore(names);
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			
			boolean isCompanyOwner = NameUtils.isCompany(names[2]);
			boolean isCompanySpouse = NameUtils.isCompany(names[5]);
			if("A DAY AWAY".equalsIgnoreCase(eachName)) {
				isCompanyOwner = true;
				names[0]="";
				names[1]="";
				names[2]="A DAY AWAY";
			}
			//JOHNSON, ERIC E & HEATHER & ERIC E AS CUSTODIAN OF HOPE JOHNSON UNDER CALIFORNIA UTMA			parse as [ERIC, E, JOHNSON, HEATHER, & ERIC E, JOHNSON]
			// check to see if first name is contained in second name
			if (("& " + names[0] + " " + names [1]).equals(names[4])){
				names[4] = "";
			} else if (StringUtils.isNotEmpty(names[3]) && StringUtils.isNotEmpty(names[4])
					&& LastNameUtils.isLastName(names[3]) && LastNameUtils.isLastName(names[5])){
				names[5] = names[5] + " " + names[3];
				names[3] = names[4];
				names[4] = "";
			}
			GenericFunctions.addOwnerNames( names, suffixes[0],suffixes[1], types, otherTypes, isCompanyOwner,isCompanySpouse, namesList);
		}
		GenericFunctions.storeOwnerInPartyNames(m, namesList, true);
	}
	
	public static void parseAddress( HtmlParser3 parser, ResultMap m, long searchId, COGenericTylerTechTR server) throws Exception {

		
		String address =clean(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(parser.getNodeList(), "Situs&nbsp;Address"),"",true));
		address = address.replaceAll("(?ism)\\n.*$", "")
						.replaceAll("\\bCR\\b", "COUNTY ROAD")
						.replaceAll("(?ism)\\bSTRD\\b", "STREET ROAD");
		int countyId = server.getDataSite().getCountyId();
		if (CountyConstants.CO_San_Miguel==countyId || CountyConstants.CO_Grand==countyId) {
			m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address.trim());
		}	
				
		server.parseAddress(address, m);
	}
	
	public static void parseAddress(ResultMap m, String origAddress) throws Exception {
		
		String addressLine=origAddress, city = "", zip = "";
		
		addressLine = addressLine.replaceAll("(?is)\\b(NORTH|SOUTH|EAST|WEST) SIDE\\b", "\"$1 SIDE\"");

		Matcher cityZipMatcher = cityZipPattern.matcher(addressLine); 
		
		if(cityZipMatcher.find()) {
				city = cityZipMatcher.group(1);
				zip = clean(cityZipMatcher.group(2));
				addressLine = addressLine.replace(cityZipMatcher.group(), "");
				if(city.equalsIgnoreCase("WELD")) {
					city = ""; 
				}			
		}
				
		String streetNo = StringFormats.StreetNo(addressLine);
		if("0".equals(streetNo)) {
			addressLine = addressLine.replaceFirst(streetNo,"");
			streetNo = StringFormats.StreetNo(addressLine);
		}
		if(!StringUtils.isEmpty(streetNo)) {
			addressLine = addressLine.replaceAll(streetNo,"");
		}
		
		/* Bug 4578
		 * Situs Address: 4631 W 3 ST GREELEY. "3" is street name. It should be parsed as "3RD"
		 * For some reason Normalize.java:2495 treats this differently and doesn't add 'rd to the street name.
		 **/
		try {
			Pattern normalizeNumberPattern = Pattern.compile("(?ism)^(\\s*[NEWS]{1}\\s+)([0-9]+)\\s+(ST(REET)?)\\s*$");
			Matcher normalizeNumberMatcher = normalizeNumberPattern.matcher(addressLine);
			if(normalizeNumberMatcher.matches()) {
				String number = normalizeNumberMatcher.group(2); 
				if (number.compareTo("11") == 0 || number.compareTo("12") == 0 || number.compareTo("13") == 0)
					number = number + "TH";
				else {
					if (number.charAt(number.length() - 1) == '1')
						number = number + "ST";
					else if (number.charAt(number.length() - 1) == '2')
						number = number + "ND";
					else if (number.charAt(number.length() - 1) == '3')
						number = number + "RD";
					else
						number = number + "TH";
				}
	
				addressLine = addressLine.replaceFirst("(?ism)^(\\s*[NEWS]{1}\\s+)([0-9]+)(\\s+ST(REET)?)\\s*$", "$1"+number+"$3");
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		m.put("PropertyIdentificationSet.StreetName",addressLine);
		m.put("PropertyIdentificationSet.StreetNo",streetNo);
		if(StringUtils.isNotEmpty(city)) {
			m.put("PropertyIdentificationSet.City",city);
		}
		if(StringUtils.isNotEmpty(zip)) {
			m.put("PropertyIdentificationSet.Zip",zip);
		}
		m.put("PropertyIdentificationSet.AddressOnServer",origAddress);
	}
	
	public static void parseSaleDataInfo( HtmlParser3 parser, ResultMap m, long searchId) throws Exception {
		//Node nodeById = parser.getNodeById("");
		Node saleDataTable = parser.getNodeById("gdv_SalesDeeds");
		
		if (saleDataTable != null){
			List<HashMap<String,String>> tableAsListMap = HtmlParser3.getTableAsListMap(saleDataTable.toHtml());
			
			//HashMap<String, String> expectedValuesToActualValues = new HashMap<String,String>();
			String[] header= new String[]{"InstrumentDate","InstrumentNumber", "SalesPrice", "DocumentType"};
			 
			Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String,String>();;
			String receiptNumberHtmlKey = "Reception Number";
			resultBodyHeaderToSourceTableHeader.put("InstrumentNumber", receiptNumberHtmlKey);
			resultBodyHeaderToSourceTableHeader.put("DocumentType", "Doc. Type");
			resultBodyHeaderToSourceTableHeader.put("SalesPrice", "Price");
			resultBodyHeaderToSourceTableHeader.put("InstrumentDate", "Date");
			//clean table from Receipt Number 0
			for (Map<String,String> row: tableAsListMap) {
				String string = row.get(receiptNumberHtmlKey);
				if (string.equals("0")){
					row.put(receiptNumberHtmlKey,"");
				}
			}
			ResultBodyUtils.buildInfSet(m, tableAsListMap, header, resultBodyHeaderToSourceTableHeader , SaleDataSet.class);
		}
	}
	
	public static void parseTaxInfo( HtmlParser3 parser, ResultMap m, long searchId) throws Exception {
		
		//NodeList nl = parser.getNodeList();
		
		TableTag taxAssessmentTable = (TableTag)HtmlParser3.getNodeByAttribute(parser.getNodeById("accountValueString").getChildren(),"class","account stripe",true);
		NodeList nodeTransactionList = parser.getNodeById("transactionDetailString").getChildren();
		TableTag taxSummaryTable = null;
		if (nodeTransactionList!=null) { 
			taxSummaryTable = (TableTag)HtmlParser3.getNodeByAttribute(nodeTransactionList,"class","account",true);
		}
		NodeList nodeTransactionDetailList = parser.getNodeById("transactionDetailString").getChildren();
		TableTag transactionDetailsTable = null;
		if (nodeTransactionDetailList!=null) {
			transactionDetailsTable = (TableTag)HtmlParser3.getNodeByAttribute(nodeTransactionDetailList,"class","account stripe",true);
		}
		
		String taxYear = StringUtils.extractParameter(parser.getHtml(), "(?i)Tax Billed at (\\d{2,4}) Rates");
		NodeList nodeList = HtmlParser3.findNodeList(parser.getNodeById("taxAccountValueSummary").getChildren(), "Taxes");
		
		Node node = null;
		if (nodeList.size()==2){
			node = nodeList.elementAt(1);
		}else{
			node = nodeList.elementAt(0);
		}
		String baseAmount = clean(HtmlParser3.getValueFromNearbyCell(2,(Text) node, "", false)).replaceAll("[^0-9\\.]", "");
		if (StringUtils.isEmpty(baseAmount)){
			nodeList = HtmlParser3.findNodeList(parser.getNodeById("accountValueString").getChildren(), "Taxes Billed");
			if (nodeList.size() > 0){
				baseAmount = clean(HtmlParser3.getValueFromNearbyCell(3, (Text) nodeList.elementAt(0), "", false)).replaceAll("[^0-9\\.]", "");
			}
		}
		String assessedValue ="";
		try {
			assessedValue = clean(HtmlParser3.getValueFromNearbyCell(3,HtmlParser3.findNode(taxAssessmentTable.getChildren(), "Total"), "", false),false,false).replaceAll("[,\\$]+", "");
		}catch(Exception ignored) {}
		
		String amountDue = "", priorDelinq="";
		double priorDelinqDouble = 0.00d, amountPaidDouble = 0.00d;
		String paymentDateCurrentTaxes = "";
		
		if (taxSummaryTable!=null) {
			for(TableRow row : taxSummaryTable.getRows()) {
				try {
					if(row.getColumnCount()>7) {
						TableColumn[] cols = row.getColumns();
						String crtTaxYear = clean(cols[0].toPlainTextString());
						if("".equals(taxYear)) {
							taxYear = crtTaxYear;
						}
						//String taxDue = clean(cols[1].toPlainTextString()).replaceAll(",", "");
						String totalDue = clean(cols[7].toPlainTextString()).replaceAll(",", "");
						if(taxYear.equalsIgnoreCase(crtTaxYear)) {
							amountDue = totalDue;
						}else {
							priorDelinqDouble += Double.parseDouble(totalDue);
						}
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			amountDue = baseAmount;
		}
		
		if(priorDelinqDouble!=0.00d) {
			priorDelinq  = String.valueOf(priorDelinqDouble);
		}
			
		if(transactionDetailsTable != null) {
			ResultTable receipts = new ResultTable();
			Map<String, String[]> map = new HashMap<String, String[]>();
			String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
			List<List<String>> bodyRT = new ArrayList<List<String>>();
		
			for(TableRow row: transactionDetailsTable.getRows()) {
				if(row.getColumnCount()>2) {
					try {
						TableColumn[] cols = row.getColumns();
						String crtTaxYear =clean(cols[0].toPlainTextString());
						String paymentType =clean(cols[1].toPlainTextString());
						String paymentDate = clean(cols[2].toPlainTextString());
						String paymentAmount = clean(cols[3].toPlainTextString()).replaceAll(",", "");
						
						if(!paymentType.toLowerCase().contains("tax payment")){//if(!paymentType.toLowerCase().contains("payment") || "Miscellaneous Payment".equals(paymentType)) {
							continue;
						}
						if(crtTaxYear.equalsIgnoreCase(taxYear)) {
							amountPaidDouble += Double.parseDouble(paymentAmount);
							if (StringUtils.isEmpty(paymentDateCurrentTaxes)) {
								paymentDateCurrentTaxes = paymentDate;
							}
						}
						
						List<String> paymentRow = new ArrayList<String>();
						paymentRow.add("");
						paymentRow.add(paymentAmount);
						paymentRow.add(paymentDate);
						bodyRT.add(paymentRow);
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			map.put("ReceiptNumber", new String[] {"ReceiptNumber", "" });
			map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
			map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
			receipts.setHead(header);
			receipts.setMap(map);
			receipts.setBody(bodyRT);
			receipts.setReadOnly();
			m.put("TaxHistorySet", receipts);
		}
		
		m.put(TaxHistorySetKey.YEAR.getKeyName(),taxYear);
		m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),baseAmount);
		m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),amountPaidDouble+"");
		m.put(TaxHistorySetKey.DATE_PAID.getKeyName(), paymentDateCurrentTaxes);
		m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),amountDue);
		m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(),priorDelinq);
		m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(),assessedValue);
	
	}


	public static void parseLegal(HtmlParser3 parser , ResultMap m, long searchId, COGenericTylerTechTR server)
			throws Exception {
		try {
			String legal = clean(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(parser.getNodeList(), "Legal"),"",true));
			legal = legal.replaceAll("\n", " ");
			m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			if ("CORouttTR".equals(server.toString())){
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
				COPitkinAO.parseLegalSummary(m, -1);
			} else if ("COSan MiguelTR".equals(server.toString())) {
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
				COSanMiguelTR.parseLegalSummary(m, -1);
			} else if ("COClear CreekTR".equals(server.toString())) {
				m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName() ,legal);
				COClearCreekTR.parseLegalSummary(m,searchId);
			} else{
				server.parseLegal(legal,m);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void parseLegal(String legal, ResultMap m) throws Exception {

			m.put("PropertyIdentificationSet.LegalDescriptionOnServer", legal);

			extractInfoIntoMap(m, legal,
					"\\bLO?TS?\\s*([-\\d\\w]+)\\b",
					"PropertyIdentificationSet.SubdivisionLotNumber");
			extractInfoIntoMap(m, legal,
					"\\bBLK\\s*(\\d+)",
					"PropertyIdentificationSet.SubdivisionBlock");

			extractInfoIntoMap(m, legal,
					"\\b(\\w+)\\s+(\\d+)[-\\s](\\d+)[-\\s](\\d+)\\b",
					"PropertyIdentificationSet.QuarterValue",
					"PropertyIdentificationSet.SubdivisionSection",
					"PropertyIdentificationSet.SubdivisionTownship",
					"PropertyIdentificationSet.SubdivisionRange");

			extractInfoIntoMap(m, legal,
					"\\b(\\d+)[-\\s](\\d+)[-\\s](\\d+)\\b",
					"PropertyIdentificationSet.SubdivisionSection",
					"PropertyIdentificationSet.SubdivisionTownship",
					"PropertyIdentificationSet.SubdivisionRange");
	}
	
	public static String clean(String str) {
		return clean(str,true,false);
	}
	
	public static boolean extractInfoIntoMap(ResultMap m, String extractFrom, String regex, String... keys) {
		return RegExUtils.extractInfoIntoMap(m, extractFrom, regex, keys);
	}
	
	public static String clean(String str, boolean cleanComma, boolean cleanMinus) {
		if(StringUtils.isEmpty(str)) return "";
		str =	str.replaceAll("<br\\s?/?>", "\n")
				.replaceAll("[\\$]", "")
				.replaceAll("^[0\\s]+$", "")
				.replaceAll("&nbsp;"," ")
				.replaceAll("&amp;","&")
				.trim();

		if(cleanComma) {
			str = str.replaceAll("&,", ",");
		}
		if(cleanMinus) {
			str = str.replaceAll("\\-", "");
		}
		return str;
	}
	
	

}

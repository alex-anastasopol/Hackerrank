package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/* used for TNShelbyYB(Bartlett) and TNMontgomeryYA(Clarksville)*/

public class TNMontgomeryEP {
	
	public static Pattern splitInstrumentPattern = Pattern.compile("(.{1,4})-(.{1,2})-(.{5})(.*?)");
	
	public static ResultMap parseIntermediaryRow(TableRow row, int miServerID) {
		
		ResultMap resultMap = new ResultMap();
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), 
				HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID).getSiteTypeAbrev());
		
		TableColumn[] cols = row.getColumns();
		for(int i=0; i <cols.length ; i++) {
			
			String contents = cols[i].getStringText().trim();
			
			if(i==0) {
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), contents);
			}
			if(i==2) {
				resultMap.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), contents);
			}
			if(i==3) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), contents);
			}
			if(i==4) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), contents);
			}
		}
		
		return resultMap;
	}

	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		try {
			detailsHtml = detailsHtml.replaceAll("<th(.*?)>", "<td$1>").replaceAll("</th>", "</td>");
			HtmlParser3 parser = new HtmlParser3(detailsHtml);
			
			String siteType = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID).getSiteTypeAbrev();
			String instrumentNo = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_CategoryLabel").trim();
			if (StringUtils.isEmpty(instrumentNo)){
				instrumentNo = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ParcelIdLabel");
			}
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), instrumentNo);
			m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), siteType);
			
			List<List> body = new ArrayList<List>();
			List<String> list = new ArrayList<String>();
			
			String recordedDate = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_DeedRecordedLabel");
			String bookPage = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_BookPageLabel");
			
			if (StringUtils.isNotEmpty(bookPage)){
				String[] bp = bookPage.split("\\s*/\\s*");
				if (bp.length == 2){
					if (siteType.equals("YA")) {//task 9177
						bp[0] = bp[0].replaceFirst("^V", "");
					}
					list.add(bp[0]);
					list.add(bp[1]);
					if (StringUtils.isNotEmpty(recordedDate)){
						list.add(recordedDate.trim());
					} else{
						list.add("");
					}
					body.add(list);
				}
			}
			if (!body.isEmpty() && body.size() > 0){
				String[] header= new String[]{"Book", "Page", "RecordedDate"};
				ResultTable rt = new ResultTable();
				rt = GenericFunctions2.createResultTable(body, header);
				m.put(SaleDataSet.class.getSimpleName(), rt);
			}
			
			try {
				parseAddress(parser, m,searchId);
				parseName(parser, m, searchId);
				parseLegal(parser, m, searchId);
				parseTaxInfo(parser, m,searchId, miServerID);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void parseNamesShelbyYB(ResultMap m, List<String> all_names) {
		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();
			int i=0;
			for (String n : all_names) {
				n = org.apache.commons.lang.StringUtils.strip(n);
				n = NameCleaner.cleanNameAndFix(n, new Vector<String>(), true);
				n = n.replaceAll("\\&$","");
				n = org.apache.commons.lang.StringUtils.strip(n);
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					String[] names; 
					if(i>0)
						names = StringFormats.parseNameFML(n,new Vector<String>(),false,true);
					else 
						names = StringFormats.parseNameNashville(n, true);
					names = NameCleaner.tokenNameAdjustment(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);
					names = NameCleaner.removeUnderscore(names);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				}
				i++;
			}
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseName(HtmlParser3 parser, ResultMap m, long searchId) throws Exception {
		
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		
		String ownerCell = "";
		try {
			if (crtCounty.equalsIgnoreCase("Montgomery")){
				ownerCell = parser.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_OwnerInformation1_OwnerInformationFormLayout_CustomerNameFormLayoutItem_ctl01_CustomerNameLabel")
							.getChildren().toHtml().trim().replaceAll("<br ?/?>", "\n");
			} else if (crtCounty.equalsIgnoreCase("Shelby")){
				ownerCell = parser.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_OwnerInformation1_OwnerInformationFormLayout_CustomerNameFormLayoutItem_ctl01_CustomerNameLabel")
							.getChildren().toHtml().trim().replaceAll("<br ?/?>", "\n");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		ownerCell = ownerCell.replaceAll("%", "").replaceAll("\\([\\d/\\s]+\\)", "");
		ownerCell = ownerCell.replaceAll("\n(TR|ETAL)\\b", " $1\n");
		ownerCell = ownerCell.replaceAll("\\b(ET)\\s+(AL|UX|VIR)\\b", "$1$2");
		ownerCell = ownerCell.replaceAll("\\bETAL\\b", "ETAL").replaceAll("\\bET\\s+UX\\b", "ETUX").replaceAll("\\bET\\s+VIR\\b", "ETVIR");
		ownerCell = ownerCell.replaceAll("(?is)\n(ET(?:AL|UX|VIR))\\b", " $1");
		ownerCell = ownerCell.replace("(LE)","LE"); //bug 7196
		
		String[] nameLines = ownerCell.split("\n");
		m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), ownerCell);
				
		List<List/*<String>*/> namesList = new ArrayList<List/*<String>*/>();
		
		if(crtCounty.equals("Shelby")){
			nameLines = ownerCell.split("( AND )|( AND$)");
			parseNamesShelbyYB(m, Arrays.asList(nameLines));
			return;
		}
		
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		
		String namesWithoutCompaines = ""; 
		for(String name : nameLines) {
			if (NameUtils.isCompany(name, new Vector<String>(), true)) {
				names[2] = name;

			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils
							.isCompany(names[5]), namesList);
			GenericFunctions.storeOwnerInPartyNames(m, namesList, true);
			}else {
				namesWithoutCompaines += " & " + name ;
			}
		}
		namesWithoutCompaines = namesWithoutCompaines.replaceFirst("&", "").trim();		
		GenericFunctions2.parseName(m, namesWithoutCompaines, namesList);
		
	}
	
	public static void parseAddress( HtmlParser3 parser, ResultMap m, long searchId) throws Exception {

		String address = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_LocationLabel").trim().replaceAll("<br>", "\n");
		String city = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_JurisdictionLabel").trim().replaceAll("<br>", "\n");
		
		if(!StringUtils.isEmpty(city) && !org.apache.commons.lang.StringUtils.isNumeric(city)) {
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
		}
		parseAddress(m,address);
	}
	
	public static void parseAddress(ResultMap m, String origAddress) throws Exception {
		
		String addressLine=origAddress, zip ="";

		String streetNo = StringFormats.StreetNo(addressLine);
		if("0".equals(streetNo)) {
			addressLine = addressLine.replaceFirst(streetNo,"");
			streetNo = StringFormats.StreetNo(addressLine);
		}
		if(!StringUtils.isEmpty(streetNo)) {
			addressLine = addressLine.replaceAll(streetNo,"");
		}
		
		Pattern zipPattern = Pattern.compile("([\\-0-9]){5,}");
		Matcher zipMatcher = zipPattern.matcher(addressLine);
		while(zipMatcher.find()) {
			zip = zipMatcher.group();
		}
		addressLine = addressLine.replaceAll(zip,"").trim()
								.replaceAll("/(N|E|W|S)", " $1");
				
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), addressLine);
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), origAddress);
	}
	
	public static void parseTaxInfo( HtmlParser3 parser, ResultMap m, long searchId, int miServerID) throws Exception {
		NodeList nl = parser.getNodeList();
		
		String taxYear = clean(parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_FiscalYearLabel"));
		String baseAmount = clean(parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_TaxLabel"));
		String assessedValue = clean(parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_AssessedValueLabel"));
		String amountPaid = clean(HtmlParser3.getValueFromNearbyCell(2,HtmlParser3.findNode(HtmlParser3.getNodeByID("BillDetailTable", nl, true).getParent().getChildren(),"TOTAL"),"",false));
		String amountDue = clean(HtmlParser3.getValueFromNearbyCell(4,HtmlParser3.findNode(HtmlParser3.getNodeByID("BillDetailTable", nl, true).getParent().getChildren(),"TOTAL"),"",false));
		
		double priorDelinq = 0.00d; 
		TableTag allBillsTable = (TableTag)parser.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_BillsRepeater_ctl00_BillsGrid");
		for(TableRow row : allBillsTable.getRows()) {
			try {
				if(row.getColumnCount()>4) {
					TableColumn[] cols = row.getColumns();
					String year = cols[2].toPlainTextString();
					String paid = cols[4].toPlainTextString().trim();
					if("Outstanding".equalsIgnoreCase(paid) && !year.equalsIgnoreCase(taxYear)) {
						String da = clean(cols[5].toPlainTextString());
						priorDelinq += Double.parseDouble(da);
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
		m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
		m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
		m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), String.valueOf(priorDelinq));
		m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue);
		
		TableTag receiptTable = (TableTag) nl.extractAllNodesThatMatch(new HasAttributeFilter("class","datatable nomargin"),true).toNodeArray()[1];
		if(!receiptTable.toHtml().contains("No payment activity")) {
			ResultTable receipts = new ResultTable();
			Map<String, String[]> map = new HashMap<String, String[]>();
			String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
			List<List<String>> bodyRT = new ArrayList<List<String>>();
		
			for(TableRow row: receiptTable.getRows()) {
				if(row.getColumnCount()>3) {
					List<String> paymentRow = new ArrayList<String>();
					TableColumn[] cols = row.getColumns();
					String paymentAmount = clean(cols[3].getChildrenHTML());
					String paymentDate = clean(cols[1].getChildrenHTML());
					if(!org.apache.commons.lang.StringUtils.isNumeric(paymentAmount.replace(".",""))) {
						continue;
					}
					paymentRow.add("");
					paymentRow.add(paymentAmount);
					paymentRow.add(paymentDate);
					bodyRT.add(paymentRow);
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
				
	}


	public static void parseLegal(HtmlParser3 parser , ResultMap m, long searchId)
			throws Exception {
		
	}
	
	private static String clean(String str) {
		if(StringUtils.isEmpty(str)) return "";
		return 
			str .trim()
				.replaceAll("<br>", "\n")
				.replaceAll("[\\-\\$,]", "");
	}
	
	

}

package ro.cst.tsearch.servers.functions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class CODouglasTR {
		
	public static ResultMap parseIntermediaryRow(TableRow row) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		for(int i=0; i <cols.length ; i++) {
			
			String contents = cols[i].toPlainTextString().trim();
			
			switch(i) {
				case 0:
					resultMap.put("PropertyIdentificationSet.ParcelID",contents);
					if(!contents.startsWith("P")) {
						resultMap.put("PropertyIdentificationSet.PropertyType","real estate");
					}
					break;
				case 1:
					resultMap.put("PropertyIdentificationSet.ParcelID2",contents);
					break;
				case 2:
					resultMap.put("PropertyIdentificationSet.OwnerLastName",contents);
					break;
				case 3:
					resultMap.put("PropertyIdentificationSet.StreetName",contents);
					break;
				case 4:
					resultMap.put("PropertyIdentificationSet.LegalDescriptionOnServer",contents);
					break;
			}
		}
		
		try {
			parseAddress(resultMap, (String) resultMap.get(PropertyIdentificationSetKey.STREET_NAME.getKeyName()));
			parseName(null, resultMap, -1);
			parseLegal(null, resultMap, -1);
		}catch(Exception e) {
			e.printStackTrace();;
		}
		
		return resultMap;
	}

	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId) {
		
		try {
			detailsHtml = detailsHtml.replaceAll("<th(.*?)>", "<td$1>").replaceAll("</th>", "</td>");
			
			HtmlParser3 parser= new HtmlParser3(detailsHtml);
			String instrumentNo = StringUtils.prepareStringForHTML(
								HtmlParser3.getValueFromNextCell(
										HtmlParser3.findNode(parser.getNodeList(), "Property Account Number:"),"",true)).trim();
			
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), instrumentNo);
			
			String instrumentNo2 = StringUtils.prepareStringForHTML(
					HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(parser.getNodeList(), "State Parcel Number:"),"",true)).trim();

			m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), instrumentNo2);
			
			
			m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			String taxYear = StringUtils.extractParameter(detailsHtml, "(?ism)Estimated\\s+(\\d{2,4})\\s+Property Taxes");
			try {
				int year = Integer.parseInt(taxYear);
				year--;
				m.put(TaxHistorySetKey.YEAR.getKeyName(), year + "");
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}
			
			try {
				parseAddress(parser, m,searchId);
				parseName(parser, m, searchId);
				parseLegal(parser, m, searchId);
				parseTaxInfo(parser, m,searchId);
			}catch(Exception e) {
				e.printStackTrace();;
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static Pattern  nameFMFMLPattern = Pattern.compile("(?ism)\\w+\\s+\\w\\s+&\\s+(?:\\w\\s?)*\\s(\\w+)");
	
	@SuppressWarnings("rawtypes")
	public static void parseName(HtmlParser3 parser, ResultMap m, long searchId) throws Exception {
		
		String ownerCell = "";
		if (parser != null){
			NodeList tables = parser.getNodeListByTypeAndAttribute("table", "class", "Apptable", true);
			if (tables.size()>1) {
				TableTag table = (TableTag) tables.elementAt(1);
				if (table.getRowCount()>0) {
					TableRow row = table.getRow(0);
					if (row.getColumnCount()>1) {
						if (row.getColumns()[0].toPlainTextString().contains("Owner Name")) {
							ownerCell = clean(row.getColumns()[1].toPlainTextString());
						}
					}
				}
			}
			
		} else {
			ownerCell = (String) m.get(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName());
		}
		
		ownerCell = ownerCell.replaceAll("%", " & ");
		ownerCell = NameCleaner.cleanFreeformName(ownerCell);
		ownerCell = ownerCell.replaceAll("\\b(?:AS)?\\s*?(TRUSTEE)\\s*(?:OF)?\\b", "$1 C/O");
		ownerCell = ownerCell.replaceAll("\\bATTN:", "C/O");
		String[] nameLines = ownerCell.split("(\\bC/O\\b)");
		
		List<List/*<String>*/> namesList = new ArrayList<List/*<String>*/>();
		
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		
		String namesWithoutCompaines = "";
		for(String name : nameLines) {
			name = name.replaceFirst("(?is)\\bCPA\\s*$", "").trim();	//Certified Public Accountant
			if (NameUtils.isCompany(name, new Vector<String>(), true)) {
				names[2] = name;

			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2]), NameUtils
							.isCompany(names[5]), namesList);
			}else {
				/* ALLAN V & NANCY LOU SMITH */
				Matcher m1 = nameFMFMLPattern.matcher(name);
				if(m1.find()) {
					String last = m1.group(1);
					name = name.replaceFirst("&" , last + " & ");
				}
				namesWithoutCompaines += " & " + name ;
			}
		}
		namesWithoutCompaines = namesWithoutCompaines.replaceFirst("&", "").trim();
		
		//RYAN & PAMELA D SMITH
		Matcher m2 = Pattern.compile("^(\\w+)\\s*&\\s*(\\w+\\s+\\w)\\s+(\\w+)$").matcher(namesWithoutCompaines);
		if(m2.find()) {
			namesWithoutCompaines = m2.group(1) + " " + m2.group(3) + " & " + m2.group(2) + " " + m2.group(3);
		}
		
		if(StringUtils.isNotEmpty(namesWithoutCompaines)) {
			
			String[] namesArray = namesWithoutCompaines.split("[\\&,]");
			if(namesArray.length>2) {
//				String previousLast = "";
				for(String eachName: namesArray) {
//					eachName = eachName.trim();
//					if(!eachName.trim().matches("\\w{2,},.*?")) {
//						eachName = previousLast + ", " + eachName;	
//					}
					names = StringFormats.parseNameDesotoRO(eachName);
					names = NameCleaner.tokenNameAdjustment(names);
					names = NameCleaner.removeUnderscore(names);
					eachName = NameCleaner.removeUnderscore(eachName);
					
					types = GenericFunctions.extractAllNamesType(names);
					otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					   
					GenericFunctions.addOwnerNames( names, "", "", types, otherTypes, 
							NameUtils.isCompany(eachName, new Vector<String>(), true), false, namesList);
//					if(eachName.matches("\\w{2,},.*?")) {
//						previousLast = names[2];
//					}
				}
				GenericFunctions.storeOwnerInPartyNames(m, namesList, true);
			}else {
				GenericFunctions2.parseName(m, namesWithoutCompaines, namesList,1);
			}
		}else {
			GenericFunctions.storeOwnerInPartyNames(m, namesList, true);
		}
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerCell);
		
		m.remove(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName());
	
	}
	
	public static void parseAddress( HtmlParser3 parser, ResultMap m, long searchId) throws Exception {

		Div assessorDetails = (Div) parser.getNodeById("Apptable");
		
		String address = "";
		
		if(assessorDetails!=null) {
			Text propAddressNode = HtmlParser3.findNode(assessorDetails.getChildren(), "Property Address");
			if(propAddressNode!=null)  {
				TableTag addressTable = HtmlParser3.getFirstParentTag(propAddressNode,TableTag.class);
				if(addressTable.getRowCount() > 2 &&  addressTable.getRow(2).getColumnCount()>2) {
					address = clean(addressTable.getRow(2).getColumns()[0].toPlainTextString());
					m.put("PropertyIdentificationSet.City",clean(addressTable.getRow(2).getColumns()[1].toPlainTextString()));
					m.put("PropertyIdentificationSet.Zip",clean(addressTable.getRow(2).getColumns()[2].toPlainTextString()));
				}
			}
		}

		if(StringUtils.isEmpty(address)) {
			TableTag table = (TableTag)parser.getNodeByTypeAndAttribute("table", "class", "Apptable", true);
			if (table.getRowCount()>1) {
				TableRow row = table.getRow(1);
				if (row.getColumnCount()>1) {
					if (row.getColumns()[0].toPlainTextString().contains("Property Address")) {
						address = row.getColumns()[1].toHtml().replaceAll("(?is)</?td[^>]*>", "")
							.replaceAll("(?s)<!--.*-->", "").replaceAll("(?is)&nbsp;", "");
						String[] split = address.split("</?br/?>");
						if (split.length==2) {
							address = clean(split[0]);
							if (address.toLowerCase().startsWith("undetermined")) {
								address = "";
							}
							String city = split[1].trim();
							if (!city.toLowerCase().startsWith("undetermined")) {
								m.put(PropertyIdentificationSetKey.CITY.getKeyName(),  city);
							}
						}
					}
				}
			}
		}
				
		parseAddress(m,address);
	}
	
	public static void parseAddress(ResultMap m, String origAddress) throws Exception {
		
		if (StringUtils.isEmpty(origAddress) || origAddress.toLowerCase().startsWith("undetermined")
				|| origAddress.equalsIgnoreCase("various")) {
			m.remove(PropertyIdentificationSetKey.STREET_NAME.getKeyName());
			return;
		}
		
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
				
		m.put("PropertyIdentificationSet.StreetName",addressLine);
		m.put("PropertyIdentificationSet.StreetNo",streetNo);
		if(StringUtils.isNotEmpty(zip)) {
			m.put("PropertyIdentificationSet.Zip",zip);
		}
		m.put("PropertyIdentificationSet.AddressOnServer",origAddress);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseTaxInfo( HtmlParser3 parser, ResultMap m, long searchId) throws Exception {
		NodeList nl = parser.getNodeList();
		
		String assessedValue = clean(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nl, "Assessed Real Value:"), "", false));
		
		Text baseAmountNode = HtmlParser3.findNode(nl, "General Tax:");
		String baseAmount = clean(HtmlParser3.getValueFromAbsoluteCell(0, 1, baseAmountNode, "", false));
		
		String amountDue = "0.0"; 
		TableTag currentTaxesDue = HtmlParser3.getFirstParentTag(baseAmountNode, TableTag.class);
		if (currentTaxesDue.getRowCount()>0) {
			TableRow lastRow = currentTaxesDue.getRow(currentTaxesDue.getRowCount()-1);
			if (lastRow.getColumnCount()>1) {
				if ("Total Due:".equals(lastRow.getColumns()[0].toPlainTextString().trim())) {
					amountDue = clean(lastRow.getColumns()[1].toPlainTextString());
					// check if the installments are also paid (Task 8804)
					try {
						if(Double.parseDouble(amountDue) == 0d) {
							if(lastRow.getColumnCount() == 4) {
								String firstInstallmentDue = clean(lastRow.getColumns()[2].toPlainTextString());
								String secondInstallmentDue = clean(lastRow.getColumns()[3].toPlainTextString());
								amountDue = GenericFunctions1.sum(firstInstallmentDue + "+" + secondInstallmentDue, searchId);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
    	String crtState = currentInstance.getCurrentState().getStateAbv();
    	String crtCounty = currentInstance.getCurrentCounty().getName();
		Date payDate = HashCountyToIndex.getPayDate(currentInstance.getCommunityId(), crtState, crtCounty, DType.TAX);
		Calendar payDateCalendar = Calendar.getInstance();
		payDateCalendar.setTime(payDate);
		payDateCalendar.add(Calendar.YEAR, 1);
		Date payDatePlusOneYear = payDateCalendar.getTime();
				
		String priorDelinq = "0.0";
		boolean delinqTableFound = false;
		boolean transactionTableFound = false;
		boolean paidTableFound = false;
		double amountPaid = 0.00d;
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		
		ResultTable rt = new ResultTable();
		List<List> tablebody = new ArrayList<List>();
		List<String> list;
		
		ResultTable transactionHistory = new ResultTable();
		List<List> tablebodytrans = new ArrayList<List>();
		List<String> listtrans;
		
		NodeList tables = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "table", "class", "Apptable", true);
		for (int i=0;i<tables.size();i++) {
			TableTag table = (TableTag)tables.elementAt(i);
			if (!paidTableFound && table.toPlainTextString().contains("Payment History")) {
				TableRow[] rows = table.getRows();
				for(int j=2; j<rows.length; j++) {
					TableRow row = rows[j];
					if(row.getColumnCount()>2) {
						TableColumn[] cols = row.getColumns();
						String dateString = cols[0].toPlainTextString().trim();
						Date date = df.parse(dateString);
						list = new ArrayList<String>();
						list.add(dateString);
						list.add(clean(cols[1].toPlainTextString()));
						tablebody.add(list);
						if (date.after(payDate) && date.before(payDatePlusOneYear)) {
							try {
								amountPaid += Double.valueOf(clean(cols[1].toPlainTextString()));
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				paidTableFound = true;
			} else if (!transactionTableFound && table.toPlainTextString().contains("Sales Information")) {
				TableRow[] rows = table.getRows();
				for(int j=2; j<rows.length; j++) {
					TableRow row = rows[j];
					if(row.getColumnCount()>5) {
						TableColumn[] cols = row.getColumns();
						listtrans = new ArrayList<String>();
						listtrans.add(clean(cols[0].toPlainTextString()));
						listtrans.add(clean(cols[1].toPlainTextString()));
						listtrans.add(clean(cols[2].toPlainTextString()));
						listtrans.add(clean(cols[3].toPlainTextString()));
						listtrans.add(clean(cols[4].toPlainTextString()));
						listtrans.add(clean(cols[5].toPlainTextString()));
						tablebodytrans.add(listtrans);
					}
				}
				transactionTableFound = true;
			} else if (!delinqTableFound && table.toPlainTextString().contains("Grand Total Of All Delinquent Taxes Due as of")) {
				if (table.getRowCount()>0) {
					TableRow row = table.getRow(0);
					if (row.getColumnCount()>1) {
						priorDelinq = clean(row.getColumns()[1].toPlainTextString());
					}
				}
				delinqTableFound = true;
			}
			if (paidTableFound && delinqTableFound && transactionTableFound) {
				break;
			}
		}
		
		String[] header = {TaxHistorySetKey.RECEIPT_DATE.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName()};
		rt = GenericFunctions2.createResultTable(tablebody, header);
		if (rt != null && tablebody.size()>0){
			m.put("TaxHistorySet", rt);
		}
		
		String[] headertrans = {SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), SaleDataSetKey.RECORDED_DATE.getShortKeyName(),  
				SaleDataSetKey.SALES_PRICE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
				SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName()};
		transactionHistory = GenericFunctions2.createResultTable(tablebodytrans, headertrans);
		if (transactionHistory != null && tablebodytrans.size()>0){
			m.put("SaleDataSet", transactionHistory);
		}
		
		m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid+"");
		m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
		m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinq);
		m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue);
		
	}


	@SuppressWarnings("rawtypes")
	public static void parseLegal(HtmlParser3 parser , ResultMap m, long searchId)
			throws Exception {
		
		try {
			String legal = "";
			if (parser == null){
				legal = (String) m.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
				parser= new HtmlParser3(legal);
			} else {
				NodeList tables = parser.getNodeListByTypeAndAttribute("table", "class", "Apptable", true);
				if (tables.size()>0) {
					TableTag table = (TableTag) tables.elementAt(0);
					if (table.getRowCount()>2) {
						TableRow row = table.getRow(2);
						if (row.getColumnCount()>1) {
							if (row.getColumns()[0].toPlainTextString().contains("Legal Description")) {
								legal = clean(row.getColumns()[1].toPlainTextString());
								m.put("PropertyIdentificationSet.LegalDescriptionOnServer", legal);
							}
						}
					}
				}
			}
			
			if (StringUtils.isEmpty(legal))
				return;
			
			Node assesorDetailsLabel = HtmlParser3.findNode(parser.getNodeList(), "Assesor Details");
			if (assesorDetailsLabel!=null) {
				Node tableNode = assesorDetailsLabel.getParent().getNextSibling();
				if (tableNode!=null && tableNode instanceof TableTag) {
					NodeList tables = HtmlParser3.getNodeListByTypeAndAttribute(tableNode.getChildren(), "table", "class", "Apptable", true);
					if (tables.size()>0) {
						TableTag table = (TableTag)tables.elementAt(0);
						if (table.getRowCount()>4) {
							TableRow row3 = table.getRow(3);
							if (row3.getColumnCount()>1) {
								if ("Subdivision:".equalsIgnoreCase(row3.getColumns()[0].toPlainTextString()) && 
										"Name:".equalsIgnoreCase(row3.getColumns()[1].toPlainTextString())) {
									TableRow row4 = table.getRow(4);
									if (row4.getColumnCount()>0) {
										String subdivisionName = row4.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", "");
										subdivisionName = clean(subdivisionName);
										if (!StringUtils.isEmpty(subdivisionName)) {
											m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
										}
									}
								}
							}
						}
					}
				}
			}
						
			extractValues(legal, m, "(?is)\\bLOTS?\\s+(\\d+[A-Z]?(?:-\\d+[A-Z]?)?)", 1, PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), false);
			
			extractValues(legal, m, "(?is)\\bBL(?:OC)?K\\s+(\\d+)", 1, PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), false);
			
			extractValues(legal, m, "(?is)\\bUNIT\\s+(\\d+(?:-\\d+))", 1, PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), false);
			
			extractValues(legal, m, "(?is)\\bBLDG\\s+(\\d+)", 1, PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), false);
			
			extractValues(legal, m, "(?is)\\bTRACT\\s+(\\d+)", 1, PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), false);
			
			List<List> bodyPIS = new ArrayList<List>();
			List<String> line;
			Node plssNode = HtmlParser3.findNode(parser.getNodeList(), "Public Land Survey System (PLSS) Location");
			if(plssNode != null ) {
				TableTag plss = HtmlParser3.getFirstParentTag(plssNode,TableTag.class);
				TableRow[] rows = plss.getRows();
				for(int i=2; i<rows.length; i++) {
					TableRow row = rows[i];
					if(row.getColumnCount()>3) {
						line = new ArrayList<String>();
						TableColumn[] cols = row.getColumns();
						for(int j=0; j<cols.length; j++) {
							line.add(clean(cols[j].toPlainTextString()).replaceFirst("^0+", ""));
						}
						bodyPIS.add(line);
					}
				}
				if (bodyPIS.size() > 0) {
					String[] header = {PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName(), 
							           PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName()};
					ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
					m.put("PropertyIdentificationSet", rt);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static String clean(String str) {
		return clean(str,true,false);
	}
	
	private static String clean(String str, boolean cleanComma, boolean cleanMinus) {
		if(StringUtils.isEmpty(str)) return "";
		str =	str.replaceAll("<br\\s?/?>", "\n")
				.replaceAll("[\\$]", "")
				.replaceAll("^[0\\s]+$", "")
				.replaceAll("&nbsp;"," ")
				.replaceAll("&amp;","&")
				.trim();

		if(cleanComma) {
			str = str.replaceAll(",", "");
		}
		if(cleanMinus) {
			str = str.replaceAll("\\-", "");
		}
		return str;
	}
	
	public static void extractValues(String legalDescription, ResultMap resultMap, String regEx, int group, String key, boolean onlyNumbers) {
		StringBuilder sb = new StringBuilder();
		
		List<String> value = RegExUtils.getMatches(regEx, legalDescription, group);
		for (int i=0; i<value.size(); i++) {
			sb.append(value.get(i).replaceAll("&", " ")).append(" ");
		}
		String newValue = sb.toString().trim();
		if (newValue.length() != 0) {
			newValue = StringFormats.RemoveDuplicateValues(newValue);
			resultMap.put(key, newValue);
		}
	}

}

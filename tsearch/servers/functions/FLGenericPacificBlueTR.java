package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.servers.functions.FLGenericQPublicAO.FlGenericQPublicAOParseType;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLGenericPacificBlueTR extends ParseClass {

	private static FLGenericPacificBlueTR mainInstance = new FLGenericPacificBlueTR();
	
	public static final String[] LEVY_CITIES = {"BRONSON", "CEDAR KEY", "CHIEFLAND", "FANNING SPRINGS",					//incorporated 
		                                        "INGLIS", "OTTER CREEK", "WILLISTON", "YANKEETOWN", 
		                                        "ANDREWS", "EAST BRONSON", "EAST WILLISTON", "ELLZEY", 					//unincorporated
		                                        "FOWLERS BLUFF", "GULF HAMMOCK", "LEBANON JUNCTION", "MANATEE ROAD",
		                                        "RALEIGH", "ROSEWOOD", "TURKEYTOWN", "USHER", "WILLISTON HIGHLANDS"};
	
	public static final String[] MANATEE_CITIES = {"ANNA MARIA", "BRADENTON", "BRADENTON BEACH", "HOLMES BEACH",		//incorporated 
        										   "LONGBOAT KEY", "PALMETTO", 
        										   "BAYSHORE GARDENS", "BRADEN CASTLE", "CORTEZ", "ELLENTON",			//unincorporated
        										   "ILEXHURST", "MANATEE", "MEMPHIS", "SAMOSET",
        										   "SOUTH BRADENTON", "WEST BRADENTON", "WEST SAMOSET", "WHITFIELD",
        										   "WHITNEY BEACH",															 
        										   "BALLENTINE MANOR", "BETHANY", "BUNKER HILL", "CEDAR HAMMOCK",		//other populated places
        										   "DUETTE", "EASTGATE", "EDGEVILLE", "ELWOOD PARK",
        										   "ERIE", "FORT HAMMER", "FOXLEIGH", "FULLERS EARTH",
        										   "GILLETTE", "KEENTOWN", "LAKEWOOD RANCH", "LONGBEACH",
        										   "LORRAINE", "MANAVISTA", "MANHATTAN", "MATOAKA",
        										   "MEMPHIS HEIGHTS", "MYAKKA CITY", "MYAKKA HEAD", "OAK KNOL",
        										   "OLD DUETTE", "ONECO", "PALM VIEW", "PALMA SOLA",
        										   "PALMA SOLA PARK", "PARMALEE", "PARRISH", "PINEY POINT",
        										   "RUBONIA", "RYE", "SANDY", "TALLEVAST",
        										   "TERRA CEIA", "TERRA MANA", "TRAILER ESTATES", "VERNA",
        										   "WATERBURY", "WHITLFIELD ESTATES", "WILLOW",
        										   "LAKE MANATEE", "MARSH ISLAND", "OLD MYAKKA", "RATTLESNAKE KEY",		//other unincorporated  places 
        										   "RUTLAND", "SNEAD ISLAND", "TARA", "VILLAGE OF THE ARTS", 
        										   "WARD LAKE",
        										   "BOWLING GREEN"};			

	private FLGenericPacificBlueTR() {
	}

	public static enum FLGenericPacificBlueTRParseType {
		FLManateeTR, FLFranklinTR, FLLevyTR;
	}

//	private static FLGenericPacificBlueTR franklinFactory = mainInstance.new FLFranklinTR();
	private static FLGenericPacificBlueTR manateeFactory = mainInstance.new FLManateeTR();
	private static FLGenericPacificBlueTR levyFactory = mainInstance.new FLLevyTR();

	public static FLGenericPacificBlueTR getInstance(FLGenericPacificBlueTRParseType parseType) {
//		if (parseType.ordinal() == FLGenericPacificBlueTRParseType.FLFranklinTR.ordinal()) {
//			mainInstance = franklinFactory;
//		}
		if (parseType.ordinal() == FLGenericPacificBlueTRParseType.FLManateeTR.ordinal()) {
			mainInstance = manateeFactory;
		}
		if (parseType.ordinal() == FLGenericPacificBlueTRParseType.FLLevyTR.ordinal()) {
			mainInstance = levyFactory;
		}

		return mainInstance;
	}

//	private class FLFranklinTR extends FLGenericPacificBlueTR {
//
//		@SuppressWarnings("unused")
//		@Override
//		public void parseLegalDescription(String legalDescription, ResultMap m) {
//			String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
//			GenericFunctions2.legalFLFranklinTR(m);
//		}
//
//		@Override
//		public void parseName(String name, ResultMap m) {
//			try {
//				String names = ((String) m.get("PropertyIdentificationSet.NameOnServer")).replaceAll("<br>", "@@");
//				m.put("tmpOwner", names);
//				ro.cst.tsearch.servers.functions.FLFranklinTR.partyNamesFranklinTR(m);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//		public String parseParcelID(String detailsHtml) {
//			String pid = "";
//			pid = RegExUtils.getFirstMatch("(?is)\\w{2,2}-\\w{3,3}-\\w{3,3}-\\w{3,4}-\\w{3,4}-\\w{3,4}", detailsHtml, 0);
//			if (org.apache.commons.lang.StringUtils.isEmpty(pid)) {
//				pid = RegExUtils.getFirstMatch("(?is)PERSONAL PROPERTY ID #:\\s*(\\d+)(?=<br>)", detailsHtml, 1);
//			}
//			return pid;
//		}
//
//		@SuppressWarnings("rawtypes")
//		@Override
//		public void parseAddress(String addressOnServer, ResultMap m) {
//			super.parseAddress(addressOnServer, m);
//			String street = (String) m.get(PropertyIdentificationSetKey.STREET_NAME.getKeyName());
//			if (StringUtils.isNotEmpty(street)) {
//				street = street.replaceAll("\\sFL$", "");
//				Map streetsufixes = AddressAbrev.getStreetsufixes();
//				String lastEncounteredAbbreviation = AddressAbrev.detectLastEncounteredAbbreviation(street, streetsufixes);
//				String containsAbbreviation = AddressAbrev.detectFirstEncounteredAbbreviation(street, streetsufixes);
//				if (StringUtils.isNotEmpty(containsAbbreviation) && !street.matches("(?is)\\A\\\"?" + containsAbbreviation + "\\s+\\d+.*")) {
//					String newAbrev = containsAbbreviation + " " + lastEncounteredAbbreviation;
//					if (StringUtils.isNotEmpty(lastEncounteredAbbreviation) &&  street.contains( newAbrev )){
//						containsAbbreviation = newAbrev;
//					}
//					String[] split = street.split(containsAbbreviation);
//					if (split.length>=1){
//						m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), split[0] + " " + containsAbbreviation);
//					}else{
//						m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), containsAbbreviation);
//					}
//					
//				}
//			}
//		}
//
//		@Override
//		protected void saveTestDataToFiles(ResultMap map) {
//			super.saveTestDataToFiles(map);
//		}
//
//		@Override
//		protected void parseSaleData(ResultMap m, NodeList mainList, String detailsHtml) {
//			try {
//				FLGenericQPublicAO.getInstance(FlGenericQPublicAOParseType.FLGilchristAO).setSaleDataSetInfo(detailsHtml, m);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}

	private class FLManateeTR extends FLGenericPacificBlueTR {

	}
	
	private class FLLevyTR extends FLGenericPacificBlueTR {
		
		public String parseParcelID(String detailsHtml) {
			String pid;
			pid = StringUtils.extractParameter(detailsHtml, "(?is)PROPERTY\\s+ID\\s*#\\s*:(?:\\s*</b>)?\\s*([\\d-]+)").trim();
			return pid;
		}
		
		@Override
		protected void parseSaleData(ResultMap m, NodeList mainList, String detailsHtml) {
			try {
				ro.cst.tsearch.servers.functions.FLLevyTR.parseSaleData(m, mainList, detailsHtml);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void parseName(String name, ResultMap m) {
			try {
				ro.cst.tsearch.servers.functions.FLLevyTR.parseName(name, m) ;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void parseLegalDescription(String legalDescription, ResultMap m) {
			ro.cst.tsearch.servers.functions.FLLevyTR.parseLegalDescription(m);
		}
		
		@Override
		public void parseAddress(String addressOnServer, ResultMap m) {
			String address = (String) m.get("tmpAddress");

			if (StringUtils.isEmpty(address))
				return;

			if (address.trim().matches("0+"))
				return;

			if (address.trim().matches("(?is)^0\\s*\\.?\\s+FL$"))
				return;

			address = address.replaceAll("&nbsp;", " ");
			address = address.replaceAll("&amp;", "&");
			
			String[] split = address.split("\\s");						//11951 NW HIGHWAY 129 CHIEFLAND FL 32626
			int split_len = split.length;
			boolean done = false;
			if (split_len>=3 && "FL".equalsIgnoreCase(split[split_len-2]) && (split[split_len-1].matches("\\d{5}(-\\d{4})?") || split[split_len-1].matches("\\d+"))) {
				String city = "";
				for (int i=0;i<LEVY_CITIES.length && !done;i++) {
					boolean twoWords = LEVY_CITIES[i].contains(" ");
					boolean valid = true;
					if (twoWords)
						valid = split_len>=4;
					if (valid) {
						if (twoWords)
							city = split[split_len-4] + " " + split[split_len-3];
						else 
							city = split[split_len-3];
						if (city.equalsIgnoreCase(LEVY_CITIES[i])) {	
							m.put(PropertyIdentificationSetKey.CITY.getKeyName(), LEVY_CITIES[i]);
							m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), split[split_len-1]);
							StringBuilder sb = new StringBuilder();
							int index = split_len-3;
							if (twoWords)
								index--;
							for (int j=0;j<index;j++)
								sb.append(split[j]).append(" ");
							address = sb.toString().trim();
							done = true;
						}
					}
				}
			}
			
			if (split_len>=3 && "FL".equalsIgnoreCase(split[split_len-1])) { // 21590 NE 40 WILLISTON FL
				String city = "";
				for (int i=0;i<LEVY_CITIES.length && !done;i++) {
					boolean twoWords = LEVY_CITIES[i].contains(" ");
					boolean valid = true;
					if (twoWords)
						valid = split_len>=4;
					if (valid) {
						if (twoWords)
							city = split[split_len-3] + " " + split[split_len-2];
						else 
							city = split[split_len-2];
						if (city.equalsIgnoreCase(LEVY_CITIES[i])) {	
							m.put(PropertyIdentificationSetKey.CITY.getKeyName(), LEVY_CITIES[i]);
							StringBuilder sb = new StringBuilder();
							int index = split_len-2;
							if (twoWords)
								index--;
							for (int j=0;j<index;j++)
								sb.append(split[j]).append(" ");
							address = sb.toString().trim();
							done = true;
						}
					}
				}
			}
			
			done = false;														//11951 NW HIGHWAY 129 CHIEFLAN...
			String try1 = "";
			String try2 = "";
			if (split_len>1)
				try1 = split[split_len-1];
			if (try1.endsWith("..."))
				try1 = try1.replaceAll("\\.\\.\\.\\z", "");
			else try1 = "";
			if (split_len>2)
				try2 = split[split_len-2] + " " + split[split_len-1];
			if (try2.endsWith("..."))
				try2 = try2.replaceAll("\\.\\.\\.\\z", "");
			else 
				try2 = "";
			if (try1.length()!=0 || try2.length()!=0) {
				for (int i=0;i<LEVY_CITIES.length && !done;i++) {
					if (try1.length()>0) {
						if (LEVY_CITIES[i].startsWith(try1)) {
							m.put(PropertyIdentificationSetKey.CITY.getKeyName(), LEVY_CITIES[i]);
							address = address.replaceAll(try1 + "\\.\\.\\.\\z", "");
							done = true;
						}
					} 
					if (try2.length()>0) {
						if (LEVY_CITIES[i].startsWith(try2)) {
							m.put(PropertyIdentificationSetKey.CITY.getKeyName(), LEVY_CITIES[i]);
							address = address.replaceAll(try2 + "\\.\\.\\.\\z", "");
							done = true;
						}
					}
				}
			}
			
			if (address.trim().matches("HWY\\s+\\d+\\s+[A-Z]")){
				m.put("PropertyIdentificationSet.StreetName", "\"" + address.trim() + "\"");
			} else {
				m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
				m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()));
			}
		}
		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void parseDetails(String detailsHtml, long searchId, ResultMap m) {

		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "").replaceAll("(?is)&amp;", "&").replaceAll("(?is)<th\\b", "<td")
				.replaceAll("(?is)</th\\b", "</td").replaceAll("(?is)</?b>", "").replaceAll("(?is)</?font[^>]*>", "");
		m.put("OtherInformationSet.SrcType", "TR");
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String pid = "";
			pid = parseParcelID(detailsHtml);

			if (StringUtils.isNotEmpty(pid)) {
				m.put("PropertyIdentificationSet.ParcelID", pid.trim()); 
				m.put("PropertyIdentificationSet.ParcelID2", pid.replaceAll("[-_]+", ""));
			}

			String owners = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "PROPERTY DETAIL"), "", true).trim();
			m.put("tmpOwner", owners);
			m.put("PropertyIdentificationSet.NameOnServer", owners);

			String taxYear = "";
			taxYear = StringUtils.extractParameter(detailsHtml, "(?is)TAX\\s+YEAR\\s*:\\s*(\\d+)");
			if (StringUtils.isNotEmpty(taxYear)) {
				m.put("TaxHistorySet.Year", taxYear.trim());
			}

			String siteAddress = "";
			siteAddress = StringUtils.extractParameter(detailsHtml, "(?is)PROPERTY\\s+ADDRESS\\s*:\\s*([^<]*)").trim();
			m.put("PropertyIdentificationSet.AddressOnServer", siteAddress);
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress.trim());
				// m.put("PropertyIdentificationSet.StreetName",
				// StringFormats.StreetName(siteAddress));
				// m.put("PropertyIdentificationSet.StreetNo",
				// StringFormats.StreetNo(siteAddress));
			}

			String legal = "";
			legal = StringUtils.extractParameter(detailsHtml.replaceAll("<br>", " "), "(?is)LEGAL\\s+DESCRIPTION\\s*:\\s*([^<]*)");
			if (StringUtils.isNotEmpty(legal)) {
				m.put("PropertyIdentificationSet.PropertyDescription", legal.trim());
				m.put("PropertyIdentificationSet.LegalDescriptionOnServer", legal.trim());
			}

			// test
			// saveTestDataToFiles(m);

			String assessedValue = "0.00";
			assessedValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Assessed Value"), "", true).trim();
			if (StringUtils.isNotEmpty(assessedValue)) {
				assessedValue = assessedValue.replaceAll("[\\$,]", "");
				m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue.trim());
			}

			String baseAmount = "0.00";
			baseAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "GROSS TAX"), "", true).trim();
			if (StringUtils.isNotEmpty(baseAmount)) {
				baseAmount = baseAmount.replaceAll("[\\$,]", "");
				m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount.trim());
			}

			String status = "";
			status = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "STATUS:"), "", true).trim();
			if (StringUtils.isNotEmpty(status)) {
				m.put("tmpStatus", status);
			}

			String totalDue = "0.00";
			double qtrsum= 0.00;
			boolean allQuarters = false;
			boolean missingQtr = false;
			try {                                       
				String amountDueTable = HtmlParser3.getValueFromAbsoluteCell(1, 0,
						HtmlParser3.findNode(mainList, "Amount Due if Received by"), "", true).trim();
				amountDueTable = amountDueTable.replaceAll("(?is)[\r\n\t]+", "");
				List<List<String>> amountDueList = HtmlParser3.getTableAsList(amountDueTable, true);
				
				if (status.toLowerCase().trim().equals("qtrly pmts") && amountDueList.get(1).size() == 4) {
					allQuarters = true;
					for (int i=0;i<4;i++) {
						String sum = amountDueList.get(1).get(i).toString().replaceAll("[\\$,]","").trim();
						if (sum.length()!=0)
							qtrsum += Double.parseDouble(sum);
						else 
							missingQtr = true;
					}
				} 
				
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				SimpleDateFormat sdf1 = new SimpleDateFormat("MMM d yyyy");
				Calendar now = Calendar.getInstance();
				String t = sdf.format(now.getTime()).toString();
				now.setTime(sdf.parse(t));
				Calendar dat = Calendar.getInstance();
				String formattedDateString = taxYear;
				boolean passedDecemberMonth = false;
				if (status.toLowerCase().trim().equals("unpaid") && !amountDueList.isEmpty()) {
					if (amountDueList.size() == 2) {
						for (int i = 0; i < amountDueList.get(0).size(); i++) {
							String dateString = amountDueList.get(0).get(i).toString().trim();

							if (RegExUtils.matches("\\d{1,2}/\\d{1,2}/\\d{4}", dateString)) {
								dat.setTime(sdf.parse(dateString));
							} else { // fix for Bug 5836
								if (dateString.matches("DEC \\d+")) {

									dateString += " " + taxYear;
								} else if (dateString.matches("[A-Z]{3} \\d+")) {
									dateString += " " + (Integer.valueOf(taxYear) + 1);
								} else {
									break; // not a valid date format
								}
								dat.setTime(sdf1.parse(dateString));
							}
							if (dat.after(now) || dat.equals(now)) {			
								if (amountDueList.get(1).get(i).toString().trim().contains("N/A Until Sale") && i < amountDueList.get(0).size() - 1) 
									totalDue = amountDueList.get(1).get(i+1).toString().trim();
								else
									totalDue = amountDueList.get(1).get(i).toString().trim();
								totalDue = totalDue.replaceAll("[\\$,]+", "");
								break;
							}
						}
					}
					if (now.after(dat)) {						//current date after the last date from the table
						totalDue = HtmlParser3.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(mainList, "TOTAL:"), "", true).trim().replaceAll("[\\$,]+", "");
					}
				} else {
					if ((status.toLowerCase().trim().equals("minimum tax")) && !amountDueList.isEmpty()) {
						if (amountDueList.size() == 2) {
							for (int i = amountDueList.get(0).size() - 1; i >= 0; i--) {
								// dat.setTime(sdf.parse(amountDueList.get(0).get(i).toString().replaceAll("(?is)Qtr[^:]+:",
								// "").trim()));
								String dateString = amountDueList.get(0).get(i).toString().replaceAll("(?is)Qtr[^:]+:", "").trim();
								if (dateString.matches("DEC \\d+")) {
									dateString += " " + taxYear;
								} else if (dateString.matches("[A-Z]{3} \\d+")) {
									dateString += " " + (Integer.valueOf(taxYear) + 1);
								} else {
									break; // not a valid date format
								}
								dat.setTime(sdf1.parse(dateString));
								if (dat.before(now) || dat.equals(now)) {
									totalDue = amountDueList.get(1).get(i).toString().trim();
									totalDue = totalDue.replaceAll("[\\$,]+", "");
									break;
								}
							}
						}
					} else if ((status.toLowerCase().trim().equals("qtrly pmts")) && !amountDueList.isEmpty()) {
						if (amountDueList.size() == 2) {
							for (int i = 0; i < amountDueList.get(0).size(); i++) {
								String dateString = amountDueList.get(0).get(i).toString().replaceAll("(?is)Qtr[^:]+:", "").trim();
								if (passedDecemberMonth) {
									formattedDateString = dateString + " " + (Integer.valueOf(taxYear) + 1);
								} else if (dateString.matches("[A-Z]{3} \\d+")) {
									formattedDateString = dateString + " " + taxYear;
								} else {
									break; // not a valid date format
								}
								if (dateString.matches("DEC \\d+")) {
									passedDecemberMonth = true;
								}
								dat.setTime(sdf1.parse(formattedDateString));
								if (dat.after(now) || dat.equals(now)) {
									totalDue = amountDueList.get(1).get(i).toString().trim();
									totalDue = totalDue.replaceAll("[\\$,]+", "");
									break;
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			String amountPaid = "0.00";

			List<List> bodyTaxes = new ArrayList<List>();
			List<String> line = null;
			//int numberOfPayments = 0;
			String currentYearPaymentTable = HtmlParser3.getNodeByID("currentTableObject4CurrentYear", mainList, true).toHtml();
			if (StringUtils.isNotEmpty(currentYearPaymentTable)) {
				List<List<String>> taxTable = HtmlParser3.getTableAsList(currentYearPaymentTable, false);
				for (List lst : taxTable) {
					line = new ArrayList<String>();
					if (lst.size() > 3) {
						line.add(lst.get(0).toString().trim());
						line.add(lst.get(1).toString().trim());
						line.add(lst.get(3).toString().replaceAll("[\\$,]", "").replaceAll("\\A0+", "").trim());
						amountPaid += "+" + lst.get(3).toString().replaceAll("[\\$,]", "").replaceAll("\\A0+", "").trim();
						bodyTaxes.add(line);
						//numberOfPayments++;
					}
				}
			}
			
			m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), GenericFunctions.sum(amountPaid, searchId));

			if (allQuarters == true) {
				if (missingQtr)									//if not all Qtrs are displayed take base amount instead of the sum of Qtrs
					qtrsum = Double.parseDouble(baseAmount);
				double totalDueDouble = qtrsum - Double.parseDouble(GenericFunctions.sum(amountPaid, searchId));  
				if (totalDueDouble>0) totalDue = Double.toString(totalDueDouble);
				else totalDue = "0.00";
			}
			m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);

			NodeList paymentTablesList = HtmlParser3.getNodesByID("currentTableObject4", mainList, true);
			if (paymentTablesList != null) {
				for (int i = 0; i < paymentTablesList.size(); i++) {
					List<List<String>> taxTable = HtmlParser3.getTableAsList(paymentTablesList.elementAt(i).toHtml(), false);
					for (List lst : taxTable) {
						line = new ArrayList<String>();
						if (lst.size() > 3) {
							line.add(lst.get(0).toString().trim());
							line.add(lst.get(1).toString().trim());
							line.add(lst.get(3).toString().replaceAll("[\\$,]", "").replaceAll("\\A0+", "").trim());
							bodyTaxes.add(line);
						}
					}
				}

				if (bodyTaxes != null) {
					if (!bodyTaxes.isEmpty()) {
						ResultTable rt = new ResultTable();
						String[] header = { "ReceiptDate", "ReceiptNumber", "ReceiptAmount" };
						rt = GenericFunctions2.createResultTable(bodyTaxes, header);
						m.put("TaxHistorySet", rt);
					}
				}
			}
			
			NodeList priorDueTablesList = HtmlParser3.getNodesByID("idPriorDue", mainList, true);
			if (priorDueTablesList != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				Calendar now = Calendar.getInstance();
				String t = sdf.format(now.getTime()).toString();
				now.setTime(sdf.parse(t));
				Calendar dat = Calendar.getInstance();
				   
				String priorDue = "0.00";
				for (int i = 0; i < priorDueTablesList.size(); i++) {
					List<List<String>> taxTable = HtmlParser3.getTableAsList(priorDueTablesList.elementAt(i).toHtml(), true);
					if (taxTable != null & taxTable.size() == 2){
						List dueDateList = taxTable.get(0);
						List amountList = taxTable.get(1);
						if (dueDateList.size() == amountList.size()){
							for (int l = 1; l < dueDateList.size(); l++) {
								dat.setTime(sdf.parse(dueDateList.get(l).toString().trim()));
								   if (dat.after(now) || dat.equals(now)) {
									   String unpaidValue = amountList.get(l).toString().replaceAll("[\\$,]+", "").trim();
									   if (unpaidValue.contains("N/A Until Sale") && l < dueDateList.size() - 1)
										   priorDue += "+" + amountList.get(l+1).toString().replaceAll("[\\$,]+", "").trim();
									   else
										   priorDue += "+" + amountList.get(l).toString().replaceAll("[\\$,]+", "").trim();
									   break;
								   }
							}
						}
					}
				}
				m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), GenericFunctions2.sum(priorDue.trim(), searchId));

			}

			parseSaleData(m, mainList, detailsHtml);
			
			parseAddress("", m);
			parseLegalDescription("", m);
			parseName("", m);

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	protected void parseSaleData(ResultMap m, NodeList mainList, String detailsHtml) {
		List<String> line;
		NodeList saleTablesList = HtmlParser3.getNodesByID("saleTable", mainList, true);
		if (saleTablesList != null) {
			List<List> body = new ArrayList<List>();
			line = null;
			for (int i = 0; i < saleTablesList.size(); i++) {
				List<List<String>> taxTable = HtmlParser3.getTableAsList(saleTablesList.elementAt(i).toHtml(), false);
				if (taxTable.size() > 10) {
					taxTable.remove(10);
					taxTable.remove(4);
					taxTable.remove(3);
					taxTable.remove(2);
				}
				line = new ArrayList<String>();
				for (List lst : taxTable) {
					if (lst.get(1).toString().trim().matches("\\d+\\s*-\\s*[A-Z]{3}\\s*-\\s*\\d{4}")) {
						// line.add(FormatDate.getDateFromFormatedString(lst.get(1).toString().replaceAll("(\\d+)\\s*-\\s*([A-Z]{3})\\s*-\\s*(\\d{4})",
						// "$2, $1 $3").trim(), "MM/dd/yyyy").toString());
						line.add(lst.get(1).toString().replaceAll("(\\d+)\\s*-\\s*([A-Z]{3})\\s*-\\s*(\\d{4})", "$2, $1 $3").trim());
					} else {
						line.add(lst.get(1).toString().replaceAll("[\\$,]", "").replaceAll("\\A0+", "").trim());
					}
				}
				body.add(line);
			}

			if (body != null) {
				if (!body.isEmpty()) {
					ResultTable rt = new ResultTable();
					String[] header = { "InstrumentDate", "SalesPrice", "Book", "Page", "Grantee", "Grantor", "DocumentType" };
					rt = GenericFunctions2.createResultTable(body, header);
					m.put("SaleDataSet", rt);
				}
			}
		}
	}

	public String parseParcelID(String detailsHtml) {
		String pid;
		pid = StringUtils.extractParameter(detailsHtml, "(?is)PROPERTY\\s+ID\\s*#\\s*:(?:\\s*</b>)?\\s*(\\d+)").trim();
		// pid = StringUtils.extractParameter(detailsHtml,
		// "(?is)PROPERTY\\s+ID\\s*#\\s*:\\s*(\\d+)");
		return pid;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse response, String table, long searchId, MessageFormat format) {
		/*
		 * Vector<ParsedResponse> intermediaryResponse = new
		 * Vector<ParsedResponse>(); try { table = table.replaceAll("\n",
		 * "").replaceAll("\r", "");
		 * 
		 * Map<String, String> paramsLink = new HashMap<String, String>();
		 * 
		 * org.htmlparser.Parser htmlParser =
		 * org.htmlparser.Parser.createParser(table, null); NodeList
		 * mainTableList = htmlParser.parse(null); NodeList tableList =
		 * mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),
		 * true); if (tableList.size() > 0) { TableTag mainTable = (TableTag)
		 * tableList.elementAt(0); TableRow[] rows = mainTable.getRows();
		 * TableRow headerRow = rows[0];
		 * headerRow.removeChild(headerRow.getChildCount() - 1);
		 * headerRow.removeChild(headerRow.getChildCount() - 1); String
		 * tableHeader = headerRow.toHtml();
		 * 
		 * // String crtCounty = //
		 * InstanceManager.getManager().getCurrentInstance
		 * (searchId).getCurrentCounty().getName().toLowerCase(); for (TableRow
		 * row : rows) { if (row.getColumnCount() > 1) {
		 * row.removeChild(row.getChildCount() - 1);
		 * row.removeChild(row.getChildCount() - 1);
		 * 
		 * TableColumn[] cols = row.getColumns(); int actionColumnIndex =
		 * getActionColumnIndex(); NodeList aList =
		 * cols[actionColumnIndex].getChildren().extractAllNodesThatMatch(new
		 * TagNameFilter("a"), true); if
		 * (rows[0].toHtml().toLowerCase().contains("mortgage")) {// for //
		 * mortgage // code/name // search aList =
		 * cols[11].getChildren().extractAllNodesThatMatch(new
		 * TagNameFilter("a"), true); } if (aList.size() > 0) { String parcel =
		 * ((LinkTag) aList.elementAt(0)).extractLink(); String link =
		 * CreatePartialLink(TSConnectionURL.idGET) + parcel; if (aList.size()
		 * == 3) { String linkAO = ((LinkTag) aList.elementAt(2)).extractLink();
		 * String pin = StringUtils.extractParameterFromUrl(linkAO, "pin"); if
		 * (StringUtils.isNotEmpty(pin)) { paramsLink.put("assessorLink:" + pin,
		 * linkAO); } } row.removeChild(row.getChildCount() - 1); String rowHtml
		 * = row.toHtml(); rowHtml = rowHtml.replaceAll("(?is)</?a[^>]*>", "");
		 * rowHtml = rowHtml.replaceAll("(?is)</tr>", "<td><a href=\"" + link +
		 * "\"> VIEW </a></td></tr>"); ParsedResponse currentResponse = new
		 * ParsedResponse();
		 * currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
		 * rowHtml); currentResponse.setOnlyResponse(rowHtml);
		 * currentResponse.setPageLink(new LinkInPage(link, link,
		 * TSServer.REQUEST_SAVE_TO_TSD));
		 * 
		 * ResultMap m = parseIntermediaryRowFLManateeTR(row, searchId);
		 * m.removeTempDef(); Bridge bridge = new Bridge(currentResponse, m,
		 * searchId);
		 * 
		 * DocumentI document = (TaxDocumentI) bridge.importData();
		 * currentResponse.setDocument(document);
		 * 
		 * intermediaryResponse.add(currentResponse); } } } Search mSearch =
		 * InstanceManager
		 * .getManager().getCurrentInstance(searchId).getCrtSearchContext();;
		 * mSearch .setAdditionalInfo(getCurrentServerName() +
		 * ":paramsDetails:", paramsLink);
		 * 
		 * String result = response.getResult(); result =
		 * result.replaceAll("(?is)name\\s*=([A-Z]+)\\s+", "name=\"$1\" ");
		 * 
		 * response.getParsedResponse().setHeader( "&nbsp;&nbsp;&nbsp;" +
		 * "<br><br>" + table.substring(table.indexOf("<table"),
		 * table.indexOf(">") + 1) + tableHeader.replaceAll("(?is)</?a[^>]*>",
		 * "")); response.getParsedResponse().setFooter("</table><br><br>");
		 * 
		 * outputTable.append(table); }
		 * 
		 * } catch (Exception e) { e.printStackTrace(); }
		 * 
		 * return intermediaryResponse;
		 */
		return null;
	}

	@SuppressWarnings("unused")
	public ResultMap parseIntermediaryRow(TableRow row, long searchId) throws Exception {

		ResultMap resultMap = new ResultMap();
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		NodeList nList = null;
		if (cols.length == 11) {// for search with mortgage code/name, there is
								// another column
			row.removeChild(3);
			cols = row.getColumns();
		} else if (cols.length == 12) {
			row.removeChild(4);
			row.removeChild(3);
			cols = row.getColumns();
		}
		int i = 4;
		for (TableColumn col : cols) {
			// System.out.println(col.toHtml());
			if (count < 6) {
				switch (count) {
				case 0:
					nList = col.getChildren();
					if (nList != null) {
						contents = nList.toHtml();
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), contents.trim());
					}

					break;
				case 1:
					if (col != null) {
						if ("manatee".equalsIgnoreCase(crtCounty)) {
							contents = col.toPlainTextString().trim();
							resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), contents);
						} else {
							if (StringUtils.isNotEmpty(col.getAttribute("title"))) {
								resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), 
										col.getAttribute("title").trim());
							}
						}
					}
					break;
				case 2:
					nList = col.getChildren();
					if (nList != null) {
						contents = nList.toHtml();
						contents = contents.replaceAll("(?is)<input[^>]*>", "").trim();
						if ("manatee".equalsIgnoreCase(crtCounty)) {
							String[] split = contents.split("(?is)</?br/?>");
							if (split.length==2) {
								resultMap.put("tmpOwner", split[0].replaceAll("\\.+", "").trim());
								resultMap.put("tmpAddress", split[1].replaceAll("\\.+", "").trim());
							}
						} else {
							resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), contents);
						}
					}

					break;
				case 3:
					
						nList = col.getChildren();
						if (nList != null) {
							contents = nList.toHtml();
							if (!("manatee".equalsIgnoreCase(crtCounty))) {
								resultMap.put("tmpOwner", contents.replaceAll("\\.+", "").trim());
							}
						}
					
					break;
				case 4:
			
						nList = col.getChildren();
						if (nList != null) {
							contents = nList.toHtml();
							if (!("manatee".equalsIgnoreCase(crtCounty))) { 
								resultMap.put("tmpAddress", contents.replaceAll("\\.+", "").trim());
							}
						}
				
					break;
				
				default:
					break;
				}
				count++;

			}
		}
		partyNamesIntermFLManateeTR(resultMap, searchId);
		parseAddress("", resultMap);

		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void parseName(String name, ResultMap m) {
		String owner = (String) m.get("tmpOwner");

		if (StringUtils.isEmpty(owner))
			return;

		owner = owner.replaceAll("(?is)[\r\t\n]", "");
		String[] ownerRows = owner.split("<br>");
		String stringOwner = "";
		for (String row : ownerRows) {
			if (row.trim().matches("\\d+\\s+.*")) {
				break;
			} else if (row.toLowerCase().contains("box")) {
				break;
			} else {
				stringOwner += row + "<br>";
			}
		}
		stringOwner = stringOwner.replaceAll("(?is)\\bWI?FE?\\b", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bSUITE\\s+\\d+", "");
		stringOwner = stringOwner.replaceAll("&#39;", "'");
		stringOwner = stringOwner.replaceAll("%", "& %");
		stringOwner = stringOwner.replaceAll("(?is)\\bDTD\\s+[\\d/]+\\s*\\*?", "");//DTD 8/3/2007 *

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		// boolean coOwner = false;
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		String[] owners = stringOwner.split("<br>");

		for (int i = 0; i < owners.length; i++) {
			owners[i] = owners[i].replaceAll("\\A\\s*&", "");
			names = StringFormats.parseNameNashville(owners[i], true);
			if (owners[i].matches("\\A\\s*ATTN\\s*:?.*")) {
				owners[i] = owners[i].replaceAll("\\bATTN\\s*:?", "");
				if (owners[i].contains(",")) {
					names = StringFormats.parseNameNashville(owners[i], true);
				} else {
					names = StringFormats.parseNameDesotoRO(owners[i], true);
				}
			}

			if (names[3].equals("ANN") && names[5].equals("MARY")) {
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			}

			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		
		}
		try {
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		stringOwner = stringOwner.replaceAll("(?is)\\s*<br>\\s*", " & ");
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner);
		String[] a = StringFormats.parseNameNashville(stringOwner);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);

	}

	@SuppressWarnings("rawtypes")
	public void partyNamesIntermFLManateeTR(ResultMap m, long searchId) throws Exception {

		String stringOwner = (String) m.get("tmpOwner");

		if (StringUtils.isEmpty(stringOwner))
			return;

		stringOwner = stringOwner.replaceAll("(?is)<br\\s*>", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bWI?FE?\\b", " ");

		stringOwner = stringOwner.replaceAll("\\bATTN", "");
		stringOwner = stringOwner.replaceAll("(?is)/?\\s*(CO-\\s*)(TRU?STEES?)\\b", "$2");
		stringOwner = stringOwner.replaceAll("(?is)\\bDECD", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMRS", "");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		// String ln = "";
		// boolean coOwner = false;

		stringOwner = stringOwner.replaceAll("(?is),(\\S)", "###$1");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		String[] owners = stringOwner.split("###");
		String just2Owners = "";
		for (int i = 0; i < owners.length; i++) {
			if (owners[i].contains("...")) {
				continue;
			}
			if (i == 0) {
				just2Owners += owners[i];
			}
			if (i == 1) {
				just2Owners += " & " + owners[i];
			}
			names = StringFormats.parseNameNashville(owners[i], true);
			// ln = names[2];
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);

		m.put("PropertyIdentificationSet.NameOnServer", stringOwner.replaceAll("###", " & ").replaceAll("\\.+", ""));
		String[] a = StringFormats.parseNameNashville(just2Owners, true);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);

	}

	@SuppressWarnings("rawtypes")
	@Override
	public void parseLegalDescription(String legalDescription, ResultMap m) {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?is)\\s+THRU\\s+", "-");
		legal = legal.replaceAll("&#39;", "'");
		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers

		String legalTemp = legal;

		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d,]+[A-Z]?(?:\\s*,\\s*[\\d,]+[A-Z])?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}

		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?)\\s*([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s*([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);// ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section from legal description
		p = Pattern.compile("(?is)\\bTR(?:ACT)?\\s+(\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract STR
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s*,?\\s*TWN\\s*(\\d+\\s*[A-Z]?)\\s*,?\\s*RNG\\s*(\\d+\\s*[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2));
			m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(3));
			m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(4));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract Section
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("\\b(PB)\\s*(\\d+)\\s*[,|/]?\\s*(PG?S?)?\\s*([\\d-&\\s]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = pb + " " + ma.group(2);
			pg = pg + " " + ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.PlatBook", pb.trim());
			m.put("PropertyIdentificationSet.PlatNo", pg.trim());
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(ORB?)\\s*(\\d+)\\s*,?\\s*((?:PG?S?)|/)\\s*([\\d-&]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(4));
			line.add("");
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		p = Pattern.compile("(?is)\\((\\d+)\\s*,?\\s*((?:PG?S?)|/)\\s*([\\d-&]+)\\)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(3));
			line.add("");
			bodyCR.add(line);
		}

		p = Pattern.compile("(?is)\\b(O\\.?R\\.?B?\\.?)\\s*(\\d+)\\s*/\\s*(\\d+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			ResultTable rt = (ResultTable) m.get("SaleDataSet");
			if (rt != null) {
				String[][] body = rt.getBodyRef();
				for (int i = 0; i < body.length; i++) {
					for (int j = 0; j < bodyCR.size(); j++) {
						if (body[i][2].trim().equals(bodyCR.get(j).get(0).toString().trim())
								&& body[i][3].trim().equals(bodyCR.get(j).get(1).toString().trim())) {
							bodyCR.remove(j);
						}
					}
				}
			}
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			try {
				cr.setHead(header);
				cr.setBody(bodyCR);
				cr.setMap(map);
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(?:LOT)\\s+(.*)\\s+(UNIT|(RE-)?SUB)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\b(?:BLDG)\\s+(.*)\\s+(PI\\s*#)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("[,']+", "");
			subdiv = subdiv.replaceAll("\\A\\s*OF\\s+", "");
			subdiv = subdiv.replaceAll("\\A.*?LOT\\s+OF\\s+", "");
			subdiv = subdiv.replaceAll("(?is)(.*?)REV\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)", "$1" + "$2");
			subdiv = subdiv.replaceFirst("\\bBLK\\b", "");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d+)", "$1");
			subdiv = subdiv.replaceFirst("COM\\s+AT", "");
			subdiv = subdiv.replaceFirst("COR\\s+OF", "");
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv.trim());
		}
	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap m) {
		String address = (String) m.get("tmpAddress");
		
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		address = address.trim();
		
		if (address.equalsIgnoreCase("No Assigned Address")) {
			return;
		}

		if (address.matches("0+")) {
			return;
		}
		
		if (address.matches("(?is)^0\\s*\\.?\\s+FL$")) {
			return;
		}
		
		address = address.replaceAll("&nbsp;", " ");
		address = address.replaceAll("&amp;", "&");
		
		Matcher matcher = Pattern.compile("(?is)(.*?)FL\\s(\\d{5})").matcher(address);
		if (matcher.find()) {
			address = matcher.group(1).trim();
			m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matcher.group(2));
		}

		//0 455 Brownsville Fl
		address = address.replaceAll("^0\\s", "");
		address = address.replaceAll("\\s*\\.\\.\\.$", "");
		address = address.replaceAll("(?is)\\sFL(\\s\\d+)?$", "");
		
		String addressAndCity[] = StringFormats.parseCityFromAddress(address, MANATEE_CITIES);
		if (StringUtils.isNotEmpty(addressAndCity[0])) {
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), addressAndCity[0]);
		}
		address = addressAndCity[1];
		
		if (address.matches("HWY\\s+\\d+\\s+[A-Z]")){
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), "\"" + address + "\"");
		} else {
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		}
	}

}

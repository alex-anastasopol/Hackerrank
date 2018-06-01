package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.TaxSiteData;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.ARGenericCountyDataAOTR;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.tsrindex.client.SimpleChapter.TaxStructure;

/**
 * @author mihaib
 * @author l
 */

public class ARGenericCountyDataAO extends ParseClass {

	private static final String HTML_BR_REG_EX = "(?is)<br\\s*/?>";

	private static ARGenericCountyDataAO _instance = null;

	private String siteLink = "";

	private ARGenericCountyDataAO() {

	}

	public static ARGenericCountyDataAO getInstance() {
		if (_instance == null) {
			_instance = new ARGenericCountyDataAO();
		}
		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse response, String table, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			table = table.replaceAll("(?is)&amp;", "&").replaceAll("(?is)&nbsp;", " ");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			Integer siteType = (Integer) response.getParsedResponse().getAttribute(ARGenericCountyDataAOTR.TMP_SITE_TYPE);
			response.getParsedResponse().setAttribute(ARGenericCountyDataAOTR.TMP_SITE_TYPE, null);

			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = mainTable.getRows();
			ResultMap resultMap = new ResultMap();

			for (int i = 1; i < rows.length; i++) {
				String rowHtml = rows[i].toHtml();
				if (rows[i] != null) {
					TableColumn[] cols = rows[i].getColumns();

					String accountNo = cols[0].toPlainTextString();
					String ownerName = cols[1].toPlainTextString();
					String propertyType = cols[2].toPlainTextString();
					String siteAddress = cols[3].getStringText();
					String legal = cols[4].getStringText();

					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), siteAddress);
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), propertyType);
					resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);

					String startLink = createPartialLink(format, TSConnectionURL.idGET);
					String initialLink = rowHtml.replaceAll("(?is).*?href[^\\\"]+\\\"([^\\\"]+).*", "$1");
					String siteLink = getSiteLink();

					String link = startLink + siteLink + "/" + initialLink;
					rowHtml = rowHtml.replaceAll(Pattern.quote(initialLink), link);

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

					setSiteType(resultMap, siteType);
					parseName("", resultMap);
					parseLegalIntermediary(resultMap, searchId);
					parseAddress("", resultMap);

					resultMap.removeTempDef();

					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					DocumentI document = bridge.importData();
					currentResponse.setDocument(document);
					intermediaryResponse.add(currentResponse);
				}
			}

			String header0 = proccessLinks(response, format);

			String header1 = "<TABLE width='100%'>";

			header1 = header1.replaceAll("(?is)</?a[^>]*>", "");
			response.getParsedResponse().setHeader(header0 + header1);
			response.getParsedResponse().setFooter("</table>");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		parseAndFillResultMap(response, resultMap, searchId);
	}

	@SuppressWarnings("unchecked")
	public void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId) {

		Integer siteType = (Integer) m.get(ARGenericCountyDataAOTR.TMP_SITE_TYPE);
		setSiteType(m, siteType);

		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&nbsp", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "").replaceAll("\n", "")
				.replaceAll("</?div[^>]*>", "");

		try {

			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			String labelToLookFor = "Parcel Number:";
			String pid = getValueForLabel(htmlParser, labelToLookFor, false);
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);

			labelToLookFor = "County Name:";
			String countyName = getValueForLabel(htmlParser, labelToLookFor, false);
			m.put(PropertyIdentificationSetKey.COUNTY.getKeyName(), countyName);

			labelToLookFor = "Ownership Information:";
			String ownership = getValueForLabel(htmlParser, labelToLookFor, false);
			m.put("tmpOwnership", ownership);

			labelToLookFor = "Legal Description:";
			String legalDescriptionOnServer = getValueForLabel(htmlParser, labelToLookFor, false);
			m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDescriptionOnServer);

			labelToLookFor = "Subdivision:";
			String subdivision = getValueForLabel(htmlParser, labelToLookFor, false);
			m.put(PropertyIdentificationSetKey.SUBDIVISION.getKeyName(), subdivision);

			labelToLookFor = "Sec-Twp-Rng:";
			String secTwnRng = getValueForLabel(htmlParser, labelToLookFor, false);
			if (StringUtils.isNotEmpty(secTwnRng)) {
				String[] split = secTwnRng.split("-");
				if (split.length == 3) {
					int i = 0;
					split = StringUtils.stripStart(split, "0");
					m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), split[i++]);
					m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), split[i++]);
					m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), split[i++]);
				}
			}

			labelToLookFor = "Lot/Block:";
			String lotBlock = getValueForLabel(htmlParser, labelToLookFor, false);
			lotBlock = lotBlock.replaceAll("(?is)PART\\s+OF", "").trim();
			if (StringUtils.isNotEmpty(secTwnRng)) {
				String[] split = (lotBlock + " ").split("/");
				split = StringUtils.stripStart(split, "0");
				if (split.length == 2) {
					int i = 0;
					m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), cleanLot(split[i++]));
					m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), split[i++]);
				}
			}

			// Sales History
			setSalesData(detailsHtml, m, searchId);
			parseName("", m);
			parseAddress("", m);
			parseLegalDetails(m, searchId);

			if (siteType.intValue() == GWTDataSite.TR_TYPE) {
				setTaxHistorySet(detailsHtml, m, htmlParser);
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param m
	 * @param siteType
	 */
	public void setSiteType(ResultMap m, Integer siteType) {
		if (siteType.intValue() == GWTDataSite.TR_TYPE) {
			m.put("OtherInformationSet.SrcType", "TR");
		} else {
			m.put("OtherInformationSet.SrcType", "AO");
		}
	}

	/**
	 * @param detailsHtml
	 * @param m
	 * @param htmlParser
	 */
	public void setTaxHistorySet(String detailsHtml, ResultMap m, HtmlParser3 htmlParser) {
		String labelToLookFor;
		boolean taxesForSaline = (detailsHtml.contains("Saline County")) ? true : false;
		
		String total = "";
		if (!taxesForSaline)  {
			labelToLookFor = "Total:";
			// base amount
			total = getValueForLabel(htmlParser, labelToLookFor, false);
		}
		
		String totalAmount = StringUtils.cleanAmount(total);
		totalAmount = StringUtils.cleanAmount(totalAmount);
		if (StringUtils.isNotEmpty(totalAmount))
			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), totalAmount);
		
		BigDecimal currentAmountDue = new BigDecimal(0.0d);
		if (NumberUtils.isNumber(totalAmount)){
			currentAmountDue = currentAmountDue.add(new BigDecimal(totalAmount));
		}


//		String realEstateValue = getValueForLabel(htmlParser, labelToLookFor, false);
		String taxStatus = "";
		if (taxesForSaline) {
			labelToLookFor = "Tax Status";
			taxStatus = org.apache.commons.lang.StringUtils.defaultIfEmpty(
					htmlParser.getValueFromAbsoluteCell(1, 0, labelToLookFor, false), "");
			taxStatus = taxStatus.replaceAll("[\\r\\n\\s]+","");
		} else {
			labelToLookFor = "Status:";
			taxStatus = org.apache.commons.lang.StringUtils.defaultIfEmpty(getValueForLabel(htmlParser, labelToLookFor, true), "").trim();
		}	
		//tax year
//		if ((StringUtils.isEmpty(taxStatus) || taxStatus.contains("DELQ")  ||  taxStatus.contains("CERT") ||  taxStatus.contains("EXEMPT") ) && StringUtils.isEmpty(taxYear) ){ //unpaid 
//			labelToLookFor = "Assessment Year:";
//			taxYear = getValueForLabel(htmlParser, labelToLookFor, false);;
//		}		
		if (taxesForSaline)
			labelToLookFor = "Assessment Year";
		else
			labelToLookFor = "Tax Year:";
		
		String taxYear = getValueForLabel(htmlParser, labelToLookFor, false);

		if (StringUtils.isEmpty(taxYear)){
			m.put(TaxHistorySetKey.YEAR.getKeyName(), "-1");
		}else{
			m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
		}
		
		String salesTable = "";
		TableTag receiptHistTable = null;
		
		if (taxesForSaline) {
			NodeList n =  new HtmlParser3(detailsHtml).getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);
			TableTag taxInfoTable = null;
			
			if (n!= null && n.size() > 1) {
				int idx = 1;
				while (idx < n.size()) {
					TableTag tbl = (TableTag) n.elementAt(idx);
					if (tbl != null) {
						String tblAsHtml = tbl.getChildrenHTML();
						if (tblAsHtml.contains(">Tax Information<")) {
							taxInfoTable = tbl;
						} else if (tblAsHtml.contains("Receipts")) {
							receiptHistTable = tbl;
							idx ++;
						}
					}
					idx ++;
					if (taxInfoTable != null && receiptHistTable != null)
						break;
				}
			}
			
			if (taxInfoTable != null) {
				TableRow[] rows = taxInfoTable.getRows();
				if (rows.length >= 2) {
					TableRow row = rows[1];
					TableColumn[] cols = row.getColumns();
					if (cols.length == 5) {
						String year = cols[0].getChildrenHTML().trim();
						String book = cols[1].getChildrenHTML().trim();
						
						if (StringUtils.isEmpty(year))
							if ("Current".equals(book) || "Delinquent".equals(book)) {
								String baseAmt = cols[2].getChildrenHTML().replaceAll("[\\$,-]", "").trim();
								String amtPaid = cols[3].getChildrenHTML().replaceAll("[\\$,-]", "").trim();
								String amtDue = cols[4].getChildrenHTML().replaceAll("[\\$,-]", "").trim();
								if (StringUtils.isNotEmpty(baseAmt)) {
										m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmt);
								}
								if (StringUtils.isNotEmpty(amtPaid))
									m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);
								if (StringUtils.isNotEmpty(amtDue))	
									if ("Delinquent".equals(book))
										m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), amtDue);
									else
										m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amtDue);
							}
					}
				}
			}
			
			if (receiptHistTable != null) {
				salesTable = receiptHistTable.toHtml().trim();
			}
			
		} else
			salesTable = RegExUtils.getFirstMatch("(?is)Historical Tax Information.*?(<table.*?</table>)", detailsHtml, 1);
		
		if (StringUtils.isNotEmpty(salesTable)) {
			Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String,String>();
			String taxYearKey = "Tax Year";
			resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.YEAR.getShortKeyName(), taxYearKey);
			String receiptKey = "Receipt #";
			resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName(), receiptKey);
			String datePaidKey = "Date Paid";
			resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.DATE_PAID.getShortKeyName(), datePaidKey);
			String amountPaidKey = "Amount Paid";
			resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.AMOUNT_PAID.getShortKeyName(), amountPaidKey);

			String[] header = new String[] { TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName(),
					TaxHistorySetKey.DATE_PAID.getShortKeyName(), TaxHistorySetKey.AMOUNT_PAID.getShortKeyName() };
			
			if (taxesForSaline) { 
				ResultTable receipts = new ResultTable();
				List<List<String>> bodyRT = new ArrayList<List<String>>();
				Map<String, String[]> tmpMap = new HashMap<String, String[]>();
				TableRow[] rows = receiptHistTable.getRows();
				if (rows.length > 2) {
					for (int i=2; i< rows.length; i++) {
						TableColumn[] cols = rows[i].getColumns();
						if (cols.length == 9) {
								String receiptNo = cols[0].getChildrenHTML().trim();
								String recTaxYear = cols[2].getChildrenHTML().trim();
								String receiptDate = cols[3].getChildrenHTML().trim();
								String recAmtPaid = cols[7].getChildrenHTML().trim().replaceAll("[\\$,]","");
								List<String> paymentRow = new ArrayList<String>();
								
								if (StringUtils.isNotEmpty(recTaxYear))
									paymentRow.add(recTaxYear);
								else
									paymentRow.add("");
								
								if (StringUtils.isNotEmpty(receiptNo)) 
									paymentRow.add(receiptNo);
								else 
									paymentRow.add("");
								
								if (StringUtils.isNotEmpty(receiptDate)) 
									paymentRow.add(receiptDate);
								else 
									paymentRow.add("");
								
								if (StringUtils.isNotEmpty(recAmtPaid)) 
									paymentRow.add(recAmtPaid);
								else 
									paymentRow.add("");
								
								bodyRT.add(paymentRow);
						}
					}
				}

				tmpMap.put("Year", new String[] { "Year", "" });
				tmpMap.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
				tmpMap.put("DatePaid", new String[] { "DatePaid", "" });
				tmpMap.put("AmountPaid", new String[] { "AmountPaid", "" });
				try {
					receipts.setHead(header);
					receipts.setMap(tmpMap);
					receipts.setBody(bodyRT);
					receipts.setReadOnly();
					m.put("TaxHistorySet", receipts);	
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else { 
				List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(salesTable);
				for (HashMap<String, String> s : tableAsListMap) {
					String string = s.get(amountPaidKey);
					String cleanAmount = StringUtils.cleanAmount(string);
					s.put(amountPaidKey, cleanAmount);
					if (taxYear.equals(s.get(taxYearKey))) {
						if (NumberUtils.isNumber(cleanAmount)) {
							currentAmountDue = currentAmountDue.subtract( new BigDecimal(cleanAmount));
						}
					}
				}
			
				if (!"PAID".equals(taxStatus)) {
					HashMap<String, String> hashMap = new HashMap<String, String>();
					hashMap.put(taxYearKey, taxYear);
					hashMap.put(amountPaidKey, "0");
					hashMap.put(datePaidKey, "//");
					hashMap.put(receiptKey, "");
					tableAsListMap.add(0, hashMap);
				}
				ResultBodyUtils.buildInfSet(m, tableAsListMap, header, resultBodyHeaderToSourceTableHeader, TaxHistorySet.class);
				
				m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), ""+currentAmountDue);;

				if ("PAID".equals(taxStatus)) {
					labelToLookFor = "Receipt #:";
					String receipt = getValueForLabel(htmlParser, labelToLookFor, false);
					m.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(), receipt);

					labelToLookFor = "Payment Date:";
					String paymentDate = getValueForLabel(htmlParser, labelToLookFor, false);
					m.put(TaxHistorySetKey.DATE_PAID.getKeyName(), paymentDate);

					labelToLookFor = "Payment Amount:";
					String amountPaid = getValueForLabel(htmlParser, labelToLookFor, false);
					m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), StringUtils.cleanAmount(amountPaid));

					m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), StringUtils.cleanAmount("0"));
				} else if ("EXEMPT".equals(taxStatus)) {
					m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "0");
				} else if (StringUtils.isNotEmpty(taxStatus) && (taxStatus.contains("DELQ")|| "CERT".equals(taxStatus))) {
					if (NumberUtils.isNumber(totalAmount)){
						if (Double.valueOf(totalAmount).doubleValue() == BigDecimal.ZERO.doubleValue()){
							m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(),"-1" /*+ TaxStructure.DELINQUENT_UNKNOWN*/); 
							//put a negative value if the status  for delinquent is Delinquent or exempt and no value can be provided to prior delinquent 
						}else{
//							m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(),"" + TaxStructure.DELINQUENT_UNKNOWN);
							m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), totalAmount);
						}
					}else{
						m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), totalAmount);
					}
					
					m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), StringUtils.cleanAmount("0"));
				}
			
			}
		}
	}

	/**
	 * @param detailsHtml
	 * @param m
	 */
	public void setSalesData(String detailsHtml, ResultMap m, long searchId) {
		String salesTable = RegExUtils.getFirstMatch("(?is)Sales History.*?(<table.*?</table>)", detailsHtml, 1);
		if (StringUtils.isNotEmpty(salesTable)) {
			List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(salesTable);

			Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap();
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), "Date");
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.SALES_PRICE.getShortKeyName(), "Price");
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.GRANTOR.getShortKeyName(), "Grantor");
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.GRANTEE.getShortKeyName(), "Grantee");
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.BOOK.getShortKeyName(), "Book");
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.PAGE.getShortKeyName(), "Page");
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), "Deed Type");
			resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), "Instrument");

			String[] header = new String[] { 
					SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(),
					SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
					SaleDataSetKey.GRANTOR.getShortKeyName(),
					SaleDataSetKey.GRANTEE.getShortKeyName(), 
					SaleDataSetKey.BOOK.getShortKeyName(), 
					SaleDataSetKey.PAGE.getShortKeyName(),
					SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
					SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName()};

			for (HashMap<String,String> row : tableAsListMap) {
				row.put("Instrument", "");
			}
			
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			if(global.getCountyId().equals(CountyConstants.AR_Pulaski_STRING)) {
				for (HashMap<String,String> row : tableAsListMap) {
					String possibleYear = row.get("Book");
					if(possibleYear != null && possibleYear.matches("\\d+")) {
						int year = Integer.parseInt(possibleYear);
						if(year < 30) {
							year += 2000;
						} else if(year <= 99) {
							year += 1900;
						}
						String recordedDate = row.get("Date");
						if(StringUtils.isNotEmpty(recordedDate)) {
							Date date = Util.dateParser3(recordedDate);
							if(date != null) {
								Calendar cal = Calendar.getInstance();
								cal.setTime(date);
								if(cal.get(Calendar.YEAR) == year) {
									//force year in case it does not match recorded date (B7241)
									row.put("Instrument", row.get("Page").replaceFirst("^0+", ""));
									row.put("Book", "");
									row.put("Page", "");
								}
							}
						}
					}
				}
			} else if(global.getCountyId().equals(CountyConstants.AR_Benton_STRING)) { // Task 7187
				for (HashMap<String,String> row : tableAsListMap) {
					String docType = row.get("Deed Type"); //bug 7261
					if(StringUtils.isNotEmpty(docType) && docType.contains("(")){
						docType = docType.replaceAll(".*\\(([^)]*)\\).*", "$1");
						row.put("Deed Type", docType);
					}
					
					
					String possibleYear = row.get("Book");
					if(possibleYear != null && possibleYear.matches("\\d+")) {
						int year = Integer.parseInt(possibleYear);
						if(year <= 99) {
							if(year < 30) {
								year += 2000;
							} else if(year <= 99) {
								year += 1900;
							}
							String recordedDate = row.get("Date");
							if(StringUtils.isNotEmpty(recordedDate)) {
								Date date = Util.dateParser3(recordedDate);
								if(date != null) {
									Calendar cal = Calendar.getInstance();
									cal.setTime(date);
									if(cal.get(Calendar.YEAR) == year) {
										row.put("Book", "" + year);
									}
								}
							}
						}
					}
				}
			}
			
			
			ResultBodyUtils.buildInfSet(m, tableAsListMap, header, resultBodyHeaderToSourceTableHeader, SaleDataSet.class);
		}
	}

	/**
	 * @param htmlParser
	 * @param labelToLookFor
	 * @param exactMatch TODO
	 * @return
	 */
	public String getValueForLabel(HtmlParser3 htmlParser, String labelToLookFor, boolean exactMatch) {
		int colOffSet = 1;
		int rowOffSet = 0;
		String cell = org.apache.commons.lang.StringUtils.defaultIfEmpty(
				htmlParser.getValueFromAbsoluteCell(colOffSet, rowOffSet, labelToLookFor, exactMatch), "");
		return cell;
	}

	@Override
	public void parseName(String name, ResultMap m) {

		String stringOwner = (String) m.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());

		if (StringUtils.isEmpty(stringOwner)) {
			stringOwner = (String) m.get("tmpOwnership");
			if (StringUtils.isNotEmpty(stringOwner)) {
				String[] split = stringOwner.split(HTML_BR_REG_EX);
				if (split.length > 0) {
					stringOwner = split[0];
				}
			}
		}
		
		
		stringOwner = stringOwner.replaceAll("\\b\\w+\\.\\.\\.$", "");
		stringOwner = stringOwner.replaceAll("(.*)(\\(TRUSTEE\\)) (REVOCABLE TRUST)", "$1 $2 @@@ $1 $3");
		stringOwner = stringOwner.replaceAll("(.*)(LE:)(.*)", "$1 LE @@@ $3");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b", "");
		stringOwner = stringOwner.replaceAll("\\bF\\s*/\\s*B\\s*/\\s*O\\b", "FBO");
		stringOwner = stringOwner.replaceAll("\\s*/\\s*", " & ");
		stringOwner = stringOwner.replaceAll("[\\d%]+", "");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS TRUSTEE OF THE\\b", "TRUSTEE");
		stringOwner = stringOwner.replaceAll("\\(TRUSTEE\\)", "TRUSTEE");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+Heirs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMrs\\b", "");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] coNames = { "", "", "", "", "", "" };
		String[] suffixes;
		String[] owners = stringOwner.split("\\s*@@@\\s*");
		for (int i = 0; i < owners.length; i++) {
			names = StringFormats.parseNameNashville(owners[i],true);
			names[2] = names[2].replaceAll("(?is),", "");

			if (StringUtils.isNotEmpty(names[5]) && LastNameUtils.isNotLastName(names[5]) && NameUtils.isNotCompany(names[5])) {
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			} else if (NameUtils.isNotCompany(names[2])) {
				if (stringOwner.matches("[^&]+&\\s+\\w+\\s+\\w{3,}")) {
					String coOwner = stringOwner.replaceAll("[^&]+&\\s+(\\w+\\s+\\w{3,}(?:\\s+\\w+)?)", "$1");
					coNames = StringFormats.parseNameDesotoRO(coOwner);
					boolean dontReverseNames = LastNameUtils.isLastName(names[5]) && FirstNameUtils.isFemaleName(names[3]);
					if (!dontReverseNames) {
						names[3] = coNames[0];
						names[4] = coNames[1];
						names[5] = coNames[2];
					}else{ // if last word is last name then parseDesotto should be use
						boolean lastName = LastNameUtils.isLastName(coNames[2]);
						boolean ownerFirstNameEmpty =  StringUtils.isEmpty(names[0]);
						if (lastName && ownerFirstNameEmpty){
							names[0] = names[2];
							names[2] = coNames[2];
							names[3] = coNames[0];
							names[4] = coNames[1];
							names[5] = coNames[2];
						}
					}
				}
			}
			//check for missplaced suffix in wife first name
			if (names.length >=6){
				String n = names[3]; 
				String[] extractSuffix = GenericFunctions.extractSuffix(n);
				if (StringUtils.isNotEmpty(extractSuffix[1])){
					names[3] = extractSuffix[0];
					names[5] = names[5] + " " + extractSuffix[1];
				}
			}
			
			String[]  type, otherType;
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);        
	        GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
	        								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
//			suffixes = GenericFunctions.extractNameSuffixes(names);
//			
//			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]),
//					body);
		}

		try {
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		stringOwner = stringOwner.replaceAll("\\s*@@@\\s*", " AND ");

		String[] a = StringFormats.parseNameNashville(stringOwner);
		m.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		m.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		m.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		m.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		m.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);

		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);

	}

	private String proccessLinks(ServerResponse response, MessageFormat format) {
		String header = "";
		String html = response.getResult();
		header = RegExUtils.getFirstMatch("(?is)<table border=\"0\" cellspacing=\"0\" cellpadding=\"2\" >.*?</table>", html, 0);
		String siteLink = getSiteLink();
		String startLink = createPartialLink(format, TSConnectionURL.idGET);
		header = header.replaceAll("(?is)(a href=\")(.*?)(\")", "$1" + startLink + siteLink + "/$2$3");
		return header;
	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());

		if (StringUtils.isEmpty(address)) {
			address = (String) resultMap.get("tmpOwnership");
		}

		String[] lines = address.split(HTML_BR_REG_EX);
		if (lines.length == 3 || lines.length == 4) { // intermediary address
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), lines[1].trim());
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(lines[1].trim()));
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(lines[1]).replaceAll("\\A0+", "")
					.trim());

			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), lines[2].replaceAll("(?is)([^,]+),.*", "$1").trim());
		}

	}

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {

	}

	public void parseLegalIntermediary(ResultMap m, long searchId) throws Exception {

		String STR = (String) m.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if (StringUtils.isNotEmpty(STR)) {
			Pattern pat = Pattern.compile("(?is)(\\d+[A-Z]?)?\\s*-\\s*(\\d+[A-Z]?)?\\s*-\\s*(\\d+[A-Z]?)?");
			Matcher mat = pat.matcher(STR);
			if (mat.find()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), StringUtils.removeLeadingZeroes(mat.group(1)));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), StringUtils.removeLeadingZeroes(mat.group(2)));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), StringUtils.removeLeadingZeroes(mat.group(3)));
			}
		}

		if (STR.contains("<table")) {
			String[] split = STR.split("<BR />");
			if (split.length == 2) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), cleanSubdivision(split[0], true));
				HtmlParser3 parser = new HtmlParser3(split[1]);

				String lot = parser.getValueFromAbsoluteCell(1, 0, "Lot:");
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), StringUtils.removeLeadingZeroes(cleanLot(lot)));

				String block = parser.getValueFromAbsoluteCell(1, 0, "Block:");
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), StringUtils.removeLeadingZeroes(block));
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void parseLegalDetails(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		String legalTemp = "";
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		
		if (StringUtils.isNotEmpty(legal))

		{
			legal = GenericFunctions.replaceNumbers(legal);
			legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																						// roman
																						// numbers
			legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");

			legalTemp = legal;
			List<String> line = new ArrayList<String>();

			// extract lot from legal description
			String lot = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
			if (lot == null) {
				lot = "";
			}
			Pattern p = Pattern.compile("(?is)\\b(LO?T'?S?\\s*)\\s*([\\d\\s&]+[A-Z]?)\\b");
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + StringUtils.removeLeadingZeroes(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}

			lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			if (lot.length() != 0) {
				lot = LegalDescription.cleanValues(lot, false, true);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			// extract block from legal description
			String block = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());
			if (block == null) {
				block = "";
			}
			p = Pattern.compile("(?is)\\b(BLKS?)\\s*(\\d+[A-Z]?|[A-Z])\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				block = block + " " + StringUtils.removeLeadingZeroes(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
			block = block.replaceAll("\\s*&\\s*", " ").trim();
			if (block.length() != 0) {
				block = LegalDescription.cleanValues(block, false, true);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			List<List> bodySTR = new ArrayList<List>();
			p = Pattern.compile("(?is)\\b(\\d+[A-Z]?)\\s*-\\s*(\\d+[A-Z]?)\\s*-\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				line = new ArrayList<String>();
				line.add(StringUtils.removeLeadingZeroes(ma.group(1).replaceAll("\\A0+", "")));
				line.add(StringUtils.removeLeadingZeroes(ma.group(2).replaceAll("\\A0+", "")));
				line.add(StringUtils.removeLeadingZeroes(ma.group(3).replaceAll("\\A0+", "")));
				bodySTR.add(line);
				legal = legal.replace(ma.group(0), " SEC ");
			}
			if (!bodySTR.isEmpty()) {
				String[] header = { PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
						PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(),
						PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName() };

				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), new String[] {
						PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), "" });
				map.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), new String[] {
						PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), "" });
				map.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), new String[] {
						PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), "" });

				ResultTable pis = new ResultTable();
				pis.setHead(header);
				pis.setBody(bodySTR);
				pis.setMap(map);
				m.put("PropertyIdentificationSet", pis);
				legal = legal.replaceAll("\\s{2,}", " ").trim();
			}
		}
		String phaseRegEx = "(?is)\\b(PH(?:ASE)?)\\s*(\\d+)\\b";
		Pattern p = Pattern.compile(phaseRegEx);
		Matcher ma = p.matcher(legal);
		String phase = "";
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			phase = ma.group(2).trim().replaceAll("#", "");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}

		String subdivisionField = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION.getKeyName());
		subdivisionField = cleanSubdivision(subdivisionField, false);
		subdivisionField = Roman.normalizeRomanNumbersExceptTokens(subdivisionField, exceptionTokens);
		
		ma = p.matcher(subdivisionField);
		if (ma.find()) {
			String ph = ma.group(2);
			if (StringUtils.isEmpty(phase) && !phase.contains(ph)) {
				phase += " " + ph;
				m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			}
			subdivisionField = subdivisionField.replaceAll(phaseRegEx, "");
		}

		String unitRegEx = "#\\s*(\\d+)";
		String unit = RegExUtils.getFirstMatch(unitRegEx, subdivisionField, 1);
		if (StringUtils.isNotEmpty(unit)) {
			subdivisionField = subdivisionField.replaceAll(unitRegEx, "");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}

		m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionField);

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		p = Pattern.compile("(?is)\\b(?:TR|TRACT)\\s+(\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legal = legal.replaceAll(ma.group(0), "");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(1).replaceAll("\\s*,\\s*", " ").trim());
		}

	}
	
	private String cleanLot(String lot) {
		String properLot = lot;
		properLot = properLot.replaceAll("(?is)\\b[NSEW]{1,2}\\s*\\d+'(\\s*X\\s*\\d+')?", "");
		properLot = properLot.replaceAll("(?is)\\b([NSEW]{1,2}\\s+)?(PT|LT|TCT)\\b", "");
		properLot = properLot.replaceAll("(?is)\\bL(\\d+)\\b", "$1");
		properLot = properLot.replaceAll("\\s{2,}", " ").trim();
		
		return properLot;
	}
	
	private String cleanSubdivision(String subdiv, boolean removePhase) {
		String properSub = subdiv;
		properSub = properSub.replaceFirst("^\\d+-\\d+-\\d+-", "");
		properSub = properSub.replaceAll("(?is)\\bBLK\\s+\\d+", "");
		if(removePhase) {
			properSub = properSub.replaceAll("(?is)\\b(PH(?:ASE)?\\s+[IVX\\d]+)", "");
		}
		
		return properSub;
	}

	public void setSiteLink(String siteLink) {
		this.siteLink = siteLink;
	}

	public String getSiteLink() {
		String link = siteLink;
		if (siteLink.endsWith("/county.asp")) {
			link = siteLink.replaceAll(Pattern.quote("/county.asp"), "");
		}
		return link;
	}

}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
 */

public class TXBexarTR {

	@SuppressWarnings({ "rawtypes" })
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m,
			long searchId) {

		m.put("OtherInformationSet.SrcType", "TR");
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "");
		int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tempList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			TableRow[] rows = ((TableTag) tempList.elementAt(1)).getRows();

			for (int i = tempList.size() - 1; i > 0; i--) {
				if (tempList.elementAt(i).toHtml().contains("tax information for")) {
					rows = ((TableTag) tempList.elementAt(i)).getRows();
					break;
				} else if (tempList.elementAt(i).toHtml().contains("tax information refers to the")) {
					rows = ((TableTag) tempList.elementAt(i)).getRows();
					break;
				}
			}

			if (rows.length > 0) {
				TableColumn[] columns = rows[0].getColumns();
				String taxYear = "";
				try {
					taxYear = columns[0].childAt(1).getChildren().elementAt(1).toHtml();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (StringUtils.isNotEmpty(taxYear)) {
					taxYear = taxYear.replaceAll("(?is).*?information\\s+for\\s*(\\d+).*", "$1");
					taxYear = taxYear.replaceAll("(?is).*?to\\s+the\\s*(\\d+)\\s+Tax\\s+Year.*", "$1");
					m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear.trim());
				}
			}
			for (int i = 0; i < tempList.size(); i++) {
				if (tempList.elementAt(i).toHtml().contains("Account Number")) {
					rows = ((TableTag) tempList.elementAt(i)).getRows();
					break;
				}
			}

			if (rows.length > 0) {
				// TableColumn[] columns = rows[0].getColumns();
				String pid = HtmlParser3.findNode(tempList, "Account Number").getText();
				pid = pid.replaceAll("(?is)\\s*Account Number\\s*:?\\s*", "").trim();
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.isNotEmpty(pid) ? pid : "");

				Text node = HtmlParser3.findNode(tempList, "Address");
				NodeList ownerData = node.getParent().getChildren();
				String owner = ownerData.elementAt(5).toHtml();
				// columns[0].childAt(5).getChildren().elementAt(5).toHtml();
				// if the next line is not an address put it all in the owner
				// field
				for (int i = 6; i < ownerData.size(); i++) {
					Node elementAt = ownerData.elementAt(i);
					if (elementAt instanceof TextNode) {
						String text = elementAt.getText();
						if (!text.trim().matches("(?is).*\\d+.*")) {
							if (owner.endsWith("&")) {
								owner += " " + text;
							} else {
								owner += " ### " + text;
							}
						} else {// when numbers are found then the address part
								// is reached
							i = ownerData.size();
						}
					}
					owner = owner.replaceAll("(?is)\\A\\s*<br>\\s*&", "").trim();
				}
				if (countyId == CountyConstants.TX_Ellis) {
					owner = owner.replaceAll("(?is)###", "<br>").trim();
				} else {
					owner = owner.replaceAll("(?is)###", "&").trim();
				}

				m.put("tmpOwner", StringUtils.isNotEmpty(owner) ? owner.trim() : "");

				Text propertyAddress = HtmlParser3.findNode(tempList,"Property Site Address");
				if (propertyAddress.getParent() != null) {
					/*
					* String secondLine = propertyAddress.getParent().getChildren().elementAt(4).toHtml();
					* columns[0].childAt(5).getChildren().elementAt* (7).toHtml();
					* 
					* if (!secondLine.trim().matches("(?is).*\\d+.*")) {
					* // "(?is)\\d+.*" 
					* if (secondLine.trim().indexOf("BOX") == -1) { 
					*   if (owner.endsWith("&")) {
					*       owner += " " + secondLine.trim(); 
					*   }
					*   else { 
					*       owner += " & " + secondLine.trim();
					*   } 
					* } 
					* }
					*/

					/*
					 * String zip = ""; 
					 * if (propertyAddress.getParent().getChildren().size()>=7) {
					 *   zip = propertyAddress.getParent().getChildren().elementAt(6).toHtml(); 
					 * }
					 * 
					 * //columns[0].childAt(7).getChildren().elementAt(6).toHtml(); 
					 * m.put("PropertyIdentificationSet.Zip", zip.trim());
					 */

					String siteAddress = propertyAddress.getParent().getChildren().elementAt(4).toHtml();
					if (siteAddress != null) {
						if (StringUtils.isEmpty(siteAddress.trim()) && propertyAddress.getParent().getChildren().size() > 5) {
							siteAddress = propertyAddress.getParent().getChildren().elementAt(6).toHtml();
						}
					}
					// columns[0].childAt(7).getChildren().elementAt(4).toHtml();
					m.put("tmpAddress", StringUtils.isNotEmpty(siteAddress.trim()) ? siteAddress.trim() : "");
				}

				NodeList legalDescription = HtmlParser3.findNode(tempList, "Legal Description")
						.getParent().getChildren();
				String legal = "";
				for (int i = 4; i < legalDescription.size(); i++) {
					legal += legalDescription.elementAt(i).toHtml();
				}
				// HtmlParser3.findNode( tempList ,
				// "Legal Description").getParent().getChildren().elementAt(4).toHtml();
				// columns[0].childAt(9).toHtml();

				// <h3> <b>Current Tax Levy: &nbsp;</b>$2,345.50</h3>

				legal = legal.replaceAll("(?is)</?h3>", "");
				legal = legal.replaceAll("(?is)<b>Legal Description:</b>", "");
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), StringUtils.isNotEmpty(legal.trim()) ? legal.trim() : "");

				NodeList baseAmountNode = HtmlParser3.findNode(tempList, "Tax Levy")
						.getParent().getChildren();
				String baseAmount = "";
				if (baseAmountNode.size() > 0) {
					baseAmount = baseAmountNode.elementAt(4).toHtml();
					// columns[0].childAt(11).toHtml();
				}

				baseAmount = baseAmount.replaceAll("(?is)</?b\\s*>", "");
				baseAmount = baseAmount.replaceAll("(?is)</?h\\d*\\s*>", "");
				baseAmount = baseAmount.replaceAll("(?is)</?br\\s*>", "");
				baseAmount = baseAmount.replaceAll("(?is)Current\\s+Year\\s+Tax\\s+Levy\\s*:", "");
				baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
				m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), StringUtils.isNotEmpty(baseAmount.trim()) ? baseAmount.trim() : "0.00");

				String amountDue = "";
				Text currentAmountDue = HtmlParser3.findNode(tempList, "Current Year Amount Due");
				if (currentAmountDue == null) {
					currentAmountDue = HtmlParser3.findNode(tempList, "Current Amount Due");
					if (currentAmountDue != null) {
						amountDue = currentAmountDue.getParent().getChildren().elementAt(4).toHtml();
					}
				} else {
					amountDue = currentAmountDue.getParent().getChildren().elementAt(4).toHtml();
					if (amountDue.trim().length()==0) {
						NodeList children = currentAmountDue.getParent().getChildren();
						if (children.size()>=9) {
							amountDue = children.elementAt(8).toHtml();
						}
					}
				}

				// columns[0].childAt(13).toHtml();
				amountDue = amountDue.replaceAll("(?is)</?b\\s*>", "");
				amountDue = amountDue.replaceAll("(?is)</?h\\d*\\s*>", "");
				amountDue = amountDue.replaceAll("(?is)</?br\\s*>", "");
				amountDue = amountDue.replaceAll("(?is)Current\\s+Year\\s+Amount\\s+Due\\s*:", "");
				amountDue = amountDue.replaceAll("(?is)[\\$,]+", "");
				m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), StringUtils.isNotEmpty(amountDue.trim()) ? amountDue.trim() : "0.00");

				String text = "Prior Year Amount Due:";
				String priorDelinq = getValueForText(tempList, text);
				// columns[0].childAt(15).toHtml();
				priorDelinq = priorDelinq.replaceAll("(?is)</?b\\s*>", "");
				priorDelinq = priorDelinq.replaceAll("(?is)</?h\\d*\\s*>", "");
				priorDelinq = priorDelinq.replaceAll("(?is)</?br\\s*>", "");
				priorDelinq = priorDelinq.replaceAll("(?is)Prior\\s+Year\\s+Amount\\s+Due\\s*:", "");
				priorDelinq = priorDelinq.replaceAll("(?is)[\\$,]+", "");
				m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), StringUtils.isNotEmpty(priorDelinq.trim()) ? priorDelinq.trim() : "0.00");

				/*
				 * text = "Total Amount Due"; String splitBaseAMount =
				 * getValueForText(tempList, text); //
				 * columns[0].childAt(17).toHtml(); splitBaseAMount =
				 * splitBaseAMount.replaceAll("(?is)</?b\\s*>", "");
				 * splitBaseAMount =
				 * splitBaseAMount.replaceAll("(?is)</?h\\d*\\s*>", "");
				 * splitBaseAMount =
				 * splitBaseAMount.replaceAll("(?is)</?br\\s*>", "");
				 * splitBaseAMount =
				 * splitBaseAMount.replaceAll("(?is)Total\\s+Amount\\s+Due\\s*:"
				 * , ""); splitBaseAMount =
				 * splitBaseAMount.replaceAll("(?is)[\\$,]+", "");
				 * m.put("TaxHistorySet.SplitPaymentAmount",
				 * StringUtils.isNotEmpty(splitBaseAMount.trim()) ?
				 * splitBaseAMount.trim() : "0.00");
				 * 
				 * if (StringUtils.isNotEmpty(splitBaseAMount)){
				 * m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),
				 * splitBaseAMount.trim()); }
				 */

				text = "Last Payment Amount for Current Year Taxes";
				String amountPaid = getValueForText(tempList, text);
				// columns[0].childAt(19).toHtml();
				amountPaid = amountPaid.replaceAll("(?is)</?b\\s*>", "");
				amountPaid = amountPaid.replaceAll("(?is)</?h\\d*\\s*>", "");
				amountPaid = amountPaid.replaceAll("(?is)</?br\\s*>", "");
				amountPaid = amountPaid.replaceAll("(?is)Last\\s+Payment\\s+Amount\\s+for\\s+Current\\s+Year\\s+Taxes\\s*:", "");
				amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
				amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
				m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), StringUtils.isNotEmpty(amountPaid.trim()) ? amountPaid.trim() : "0.00");

				text = "Last Payment Date for Current Year Taxes";
				String dateLastPaidDate = getValueForText(tempList, text);
				amountPaid = RegExUtils.getFirstMatch("\\d{1,2}/\\d{1,2}/\\d{4}", dateLastPaidDate, 0);
				m.put(TaxHistorySetKey.DATE_PAID.getKeyName(), StringUtils.isNotEmpty(dateLastPaidDate.trim()) ? dateLastPaidDate.trim() : "0.00");
				
				String landValue = "";
				String improvementValue = "";
				String totalAppraisal = "";
				if (countyId == CountyConstants.TX_Bexar) {
					landValue = getValueForTextIndex3(tempList, "Land Value").replaceAll("[\\$,]", "").trim();
					improvementValue = getValueForTextIndex3(tempList, "Improvement Value").replaceAll("[\\$,]", "").trim();
					totalAppraisal = getValueForTextIndex3(tempList, "Total Market Value").replaceAll("[\\$,]", "").trim();
				} else if (countyId == CountyConstants.TX_Dallas) {
					landValue = getValueForText(tempList, "Land Value").replaceAll("[\\$,]", "").trim();
					improvementValue = getValueForText(tempList, "Improvement Value").replaceAll("[\\$,]", "").trim();
					totalAppraisal = getValueForText(tempList, "Market Value").replaceAll("[\\$,]", "").trim();
				} else if (countyId == CountyConstants.TX_Ellis) {
					landValue = getValueForTextIndex3(tempList, "Land Value").replaceAll("[\\$,]", "").trim();
					improvementValue = getValueForTextIndex3(tempList, "Improvement Value").replaceAll("[\\$,]", "").trim();
					totalAppraisal = getValueForTextIndex3(tempList, "Gross Value").replaceAll("[\\$,]", "").trim();
				} else if (countyId == CountyConstants.TX_Nueces) {
					landValue = getValueForTextIndex3(tempList, "Land Value").replaceAll("[\\$,]", "").trim();
					improvementValue = getValueForTextIndex3(tempList, "Improvement Value").replaceAll("[\\$,]", "").trim();
					totalAppraisal = getValueForTextIndex3(tempList, "Market Value").replaceAll("[\\$,]", "").trim();
				} else if (countyId == CountyConstants.TX_San_Patricio) {
					landValue = getValueForTextIndex3(tempList, "Land Value").replaceAll("[\\$,]", "").trim();
					improvementValue = getValueForTextIndex3(tempList, "Improvement Value").replaceAll("[\\$,]", "").trim();
					totalAppraisal = getValueForTextIndex3(tempList, "Gross Value").replaceAll("[\\$,]", "").trim();
				}
				
				if (StringUtils.isNotEmpty(landValue)) {
					m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue);
				}
				if (StringUtils.isNotEmpty(improvementValue)) {
					m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvementValue);
				}
				if (StringUtils.isNotEmpty(totalAppraisal)) {
					m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAppraisal);
				}
				
			}
			for (int i = 2; i < tempList.size(); i++) {
				if (tempList.elementAt(i).toHtml().contains("Receipt")) {
					rows = ((TableTag) tempList.elementAt(i)).getRows();
					break;
				}
			}
			if (rows.length > 0) {
				TableTag taxTab = (TableTag) tempList.elementAt(3);
				for (int i = 2; i < tempList.size(); i++) {
					if (tempList.elementAt(i).toHtml().contains("Receipt")) {
						taxTab = (TableTag) tempList.elementAt(i);
						break;
					}
				}
				if (taxTab != null) {
					String tabel = taxTab.toHtml();
					tabel = tabel.replaceAll("(?is)</?colgroup[^>]*>", "");
					tabel = tabel.replaceAll("(?is)</?col[^>]*>", "");
					tabel = tabel.replaceAll("(?is)[\\$,\\(\\)]+", "");
					tabel = tabel.replaceAll("(?is)(\\d+),(\\d+)", "$1$2");
					tabel = tabel.replaceAll("(?is)(\\d{4})\\s*-\\s*(\\d{1,2})\\s*-\\s*(\\d{1,2})", "$2/$3/$1");
					tabel = tabel.replaceAll("(?is)01/01/9999", " ");// weird on Ellis

					try {
						if (tabel.indexOf("No payments were found") == -1) {
							Map<String, List<String>> tableAsMap = HtmlParser3.getTableAsMap(tabel);
							List<String> receiptDateList = tableAsMap.get(" ReceiptDate ");// for Bexar, Nueces
							if (receiptDateList == null) {
								receiptDateList = tableAsMap.get(" Receipt Date ");// this is for Dallas
							}

							List<String> amountList = tableAsMap.get(" Amount ");// for Bexar, Nueces
							if (amountList == null) {
								amountList = tableAsMap.get(" Payment Amount ");// this is for Dallas
							}
							List<List> body = new ArrayList<List>();
							for (int i = 0; i < receiptDateList.size(); i++) {
								ArrayList<String> list = new ArrayList<String>();
								list.add(receiptDateList.get(i));
								list.add(amountList.get(i));
								body.add(list);
							}
							/*
							 * for(List<String> list : receiptsList){
							 * list.remove(4); list.remove(3); list.remove(1);
							 * body.add(list); }
							 */
							ResultTable rt = new ResultTable();
							String[] header = { "ReceiptDate", "ReceiptAmount" };
							rt = GenericFunctions2.createResultTable(body, header);
							m.put("TaxHistorySet", rt);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}

			try {
				if (countyId == CountyConstants.TX_Dallas) {
					parseAddressTXDallasTR(m, searchId);
				} else {
					parseAddressTXBexarTR(m, searchId);
				}
				if (countyId == CountyConstants.TX_Ellis) {
					parseLegalTXEllisTR(m, searchId);
				} else {
					parseLegalTXBexarTR(m, searchId);
				}
				taxTXBexarTR(m, searchId);
				if (countyId == CountyConstants.TX_Bexar || countyId == CountyConstants.TX_Dallas || countyId == CountyConstants.TX_San_Patricio) {
					partyNamesTXBexarTR(m, searchId);
				} else if (countyId == CountyConstants.TX_Nueces) {
					partyNamesTXNuecesTR(m, searchId);
				} else if (countyId == CountyConstants.TX_Ellis) {
					partyNamesTXEllisTR(m, searchId);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String getValueForText(NodeList tempList, String text) {
		Text findNode = HtmlParser3.findNode(tempList, text);
		String html = "";
		if (findNode != null && findNode.getParent() != null
				&& findNode.getParent().getChildren() != null
				&& findNode.getParent().getChildren().size() > 4) {
			html = findNode.getParent().getChildren().elementAt(4).toHtml();
		}
		return html;
	}
	
	private static String getValueForTextIndex3(NodeList tempList, String text) {
		Text findNode = HtmlParser3.findNode(tempList, text);
		String html = "";
		if (findNode != null && findNode.getParent() != null
				&& findNode.getParent().getChildren() != null
				&& findNode.getParent().getChildren().size() > 3) {
			html = findNode.getParent().getChildren().elementAt(3).toHtml();
		}
		return html;
	}

	public static ResultMap parseIntermediaryRowTXBexarTR(TableRow row,
			long searchId) throws Exception {

		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		String crtCounty = InstanceManager.getManager()
				.getCurrentInstance(searchId).getCurrentCounty().getName();

		TableColumn[] cols = row.getColumns();
		int count = 0;
		for (TableColumn col : cols) {

			if (count < 4) {
				String contents = col.getChildren().toHtml();
				switch (count) {
				case 0:
					contents = contents.replaceAll(
							"(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1").replaceAll(
							"(?is)</?h[^>]*>", "");
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
							contents.trim());

					break;
				case 1:
					if (StringUtils.isNotEmpty(contents)) {
						contents = contents.replaceAll("(?is)</?h[^>]*>", "");
						String[] lines = contents.split("<br>");
						String owner = "";
						if (!lines[1].trim().matches("(?is)\\d+.*")) {
							if (lines[1].trim().indexOf("BOX") == -1) {
								if (lines[0].endsWith("&")) {
									owner = lines[0].trim() + " "
											+ lines[1].trim();
								} else {
									owner = lines[0].trim() + " ### "
											+ lines[1].trim();
								}
							} else {
								owner = lines[0].trim();
							}
						} else {
							owner = lines[0].trim();
						}
						if ("Ellis".equals(crtCounty)) {
							owner = owner.replaceAll("(?is)###", "<br>").trim();
						} else {
							owner = owner.replaceAll("(?is)###", "&").trim();
						}
						resultMap.put("tmpOwner", owner);
					}
					break;
				case 2:
					if (StringUtils.isNotEmpty(contents)) {
						contents = contents.replaceAll("(?is)</?h[^>]*>", "");
						resultMap.put("tmpAddress", contents.trim());
					}
					break;
				case 3:
					if (StringUtils.isNotEmpty(contents)) {
						contents = contents.replaceAll("(?is)</?h[^>]*>", "");
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), contents.trim());
					}
					break;
				default:
					break;
				}
				count++;
			}

		}

		if ("Bexar".equals(crtCounty) || "Dallas".equals(crtCounty)) {
			partyNamesTXBexarTR(resultMap, searchId);
		} else if ("Nueces".equals(crtCounty)) {
			partyNamesTXNuecesTR(resultMap, searchId);
		} else if ("Ellis".equals(crtCounty)) {
			partyNamesTXEllisTR(resultMap, searchId);
		}
		if ("Dallas".equals(crtCounty)) {
			parseAddressTXDallasTR(resultMap, searchId);
		} else {
			parseAddressTXBexarTR(resultMap, searchId);
		}

		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static void partyNamesTXEllisTR(ResultMap m, long searchId)
			throws Exception {

		String stringOwner = (String) m.get("tmpOwner");

		if (StringUtils.isEmpty(stringOwner))
			return;

		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);

		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN: C/O\\b", "");

		stringOwner = stringOwner.replaceAll("(?is)[^L]/[A-Z]\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bL/TR\\b", "LIVING TRUST");
		stringOwner = stringOwner.replaceAll("(?is)\\bET\\s+AL\\b", "ETAL");
		stringOwner = stringOwner.replaceAll("(?is)\\bET\\s+UX\\b", "ETUX");
		stringOwner = stringOwner.replaceAll("\\s*&\\s*(ETAL|ETUX)\\s*$", " $1");
		stringOwner = stringOwner.replaceAll("\\s+AND\\s+(ETAL|ETUX)\\s*$", " $1");
		stringOwner = stringOwner.replaceAll("\\s*\\(\\s*", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bWFE?\\b", " ");
		
		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN?:?(\\s+TAX\\s+DEP(ARTMENT)?T)?", "");
		stringOwner = stringOwner.replaceAll("\\bVLB.*", "");
		stringOwner = stringOwner.replaceAll("\\s+/\\s+", " & ");
		stringOwner = stringOwner.replaceAll("\\bAND\\b", " & ");
		stringOwner = stringOwner.replaceAll("\\bAKA\\b.*", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s*<br>\\s*(\\w+)\\s*$"," $1");
		stringOwner = stringOwner.replaceAll("(?is)\\bDBA\\b", "<br>");
		
		stringOwner = stringOwner.replaceAll("(?is)(<br> )+", "<br>");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, types, otherType;
		String ln = "";
		boolean coOwner = false;
		String[] owners = stringOwner.split("(?is)<br>");
		if (stringOwner.matches("(?is).*?\\w+\\s*&\\s*\\w+\\s*&\\s*\\w+\\s*$")
				&& NameUtils.isCompany(stringOwner)) {// MIGHTY CLEAN COIN
														// LAUNDRY & WILSON &
														// WILSON
			stringOwner = stringOwner.replaceFirst("\\s*&\\s*", " aaaaaaa ");
			owners = stringOwner.split("aaaaaaa");
		}
		for (int i = 0; i < owners.length; i++) {
			boolean isCompany = false;
			boolean fistIsCompany = false;
			
			if (i == 0) {
				if (owners[i].contains(" ATTY")) {
					owners[i] = owners[i].replaceAll("\\bATTY", "");
					names = StringFormats.parseNameDesotoRO(owners[i], true);
				} else {
					names = StringFormats.parseNameNashville(owners[i], true);
				}
				fistIsCompany = NameUtils.isCompany(names[2]) || names[2].contains("&");
				if (!fistIsCompany) {
					ln = names[2];
				}
			} else {
				if (NameUtils.isCompany(owners[i])) {
					names[2] = owners[i].trim();
					isCompany = true;
				} else {
					owners[i] = owners[i].replaceAll("\\s*%\\s*", "");
					names = StringFormats.parseNameDesotoRO(owners[i], true);
					coOwner = true;
				}
				fistIsCompany = NameUtils.isCompany(names[2]) || names[2].contains("&");
			}
			
			if (coOwner) {
				if (fistIsCompany
						&& names[2].matches("(?is).*?\\s+TR\\b")
						&& !"".equals(ln)) {
					names[2] = names[2].replaceAll("(?is)(.*?)\\s+TR\\b", "$1 "
							+ ln + " TR");
				}

				if (NameUtils.isCompany(names[5]) && names[2].length() > 0) {
					names[2] = names[2] + "&" + names[5];
					names[5] = "";
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& !fistIsCompany
						&& names[0].length() == 0 && names[1].length() == 0
						&& !"".equals(ln)) {
					names[0] = names[2];
					names[2] = ln;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() != 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = names[1];
					names[1] = aux;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& !fistIsCompany
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() == 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;

				} else if (names[2].length() == 1
						|| (LastNameUtils.isNotLastName(names[2]) && !fistIsCompany) && !"".equals(ln)) {
					names[1] = names[2];
					names[2] = ln;
				} else if (names[0].length() == 0 && names[1].length() == 0
						&& !fistIsCompany && !"".equals(ln)) {
					names[0] = names[2];
					names[2] = ln;
				} else if (names[0].length() > 1 && names[1].length() == 0
						&& !fistIsCompany
						&& LastNameUtils.isNotLastName(names[2])
						&& !"".equals(ln)) {
					if (!names[2].equals(ln)) {
						names[1] = names[2];
						names[2] = ln;
					}
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;
				}
			}
			if (!fistIsCompany) {
				ln = names[2];
			}

			names[2] = names[2].replaceAll("(?is),", "");
			types = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			boolean secondIsCompany = NameUtils.isCompany(names[5]);
			if (isCompany) {
				fistIsCompany = true;
			}
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherType, fistIsCompany, secondIsCompany, body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);

		String[] a = StringFormats.parseNameNashville(stringOwner.replaceAll("\\s*<br>\\s*", " & "), true);
		m.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		m.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		m.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		m.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		m.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);

		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),	stringOwner.replaceAll("\\s*<br>\\s*", " & ").replaceAll("\\s+&\\s+&\\s+", " & "));

	}

	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesTXNuecesTR(ResultMap m, long searchId) throws Exception {

		String stringOwner = (String) m.get("tmpOwner");

		if (StringUtils.isEmpty(stringOwner))
			return;

		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);

		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN: C/O\\b", "");

		stringOwner = stringOwner.replaceAll("(?is)[^L]/[A-Z]\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bL/TR\\b", "LIVING TRUST");
		stringOwner = stringOwner.replaceAll("\\s*&\\s*(ETAL|ETUX)", " $1");
		stringOwner = stringOwner.replaceAll("\\s+AND\\s+(ETAL|ETUX)", " $1");
		stringOwner = stringOwner.replaceAll("\\s*\\(\\s*", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bWFE?\\b", " ");
		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN", "");
		stringOwner = stringOwner.replaceAll("\\bVLB.*", "");
		stringOwner = stringOwner.replaceAll("\\s+/\\s+", " & ");
		stringOwner = stringOwner.replaceAll("\\bAND\\b", " & ");
		stringOwner = stringOwner.replaceAll("\\bAKA\\b.*", "");// for
																// 747100050200
																// nueces.
																// perhaps this
																// is not good.
		stringOwner = stringOwner.replaceAll(
				"(?is)(%\\w+\\s+\\w)\\s*&\\s*(\\w+\\s+\\w\\s+(\\w+))",
				" $1 $3 & $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s+&\\s*&\\s+", " & ");// 307600000750
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, types, otherType;
		String ln = "";
		boolean coOwner = false;
		String[] owners = stringOwner.split(" & ");
		if (stringOwner.matches("\\w+(\\s+\\w+)?\\s+&.*")
				&& NameUtils.isCompany(stringOwner)) {
			stringOwner = stringOwner.replaceAll("\\s+&\\s+$", "");
			stringOwner = stringOwner.replaceAll("\\s*&\\s*%\\s*", " aaaaaaa ");
			stringOwner = stringOwner.replaceAll("\\s*%\\s*", " aaaaaaa ");
			owners = stringOwner.split("aaaaaaa");
		}

		for (int i = 0; i < owners.length; i++) {
			if (!NameUtils.isCompany(owners[i])) {
				owners[i] = owners[i].replaceFirst("/", " & ");
			}
			if (StringUtils.isNotEmpty(owners[i])) {
				if (i == 0) {
					names = StringFormats.parseNameNashville(owners[i], true);
					ln = names[2];
				} else {
					names = StringFormats.parseNameDesotoRO(owners[i], true);
					coOwner = true;
				}
			}

			if (coOwner) {
				if (NameUtils.isCompany(names[2])
						&& names[2].matches("(?is).*?\\s+TR\\b")) {
					names[2] = names[2].replaceAll("(?is)(.*?)\\s+TR\\b", "$1 "
							+ ln + " TR");
				}

				if (NameUtils.isCompany(names[5]) && names[2].length() > 0) {
					names[2] = names[2] + "&" + names[5];
					names[5] = "";
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& NameUtils.isNotCompany(names[2])
						&& names[0].length() == 0 && names[1].length() == 0) {
					names[0] = names[2];
					names[2] = ln;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() != 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = names[1];
					names[1] = aux;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() == 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;

				} else if (names[2].length() == 1
						|| (LastNameUtils.isNotLastName(names[2]) && NameUtils
								.isNotCompany(names[2]))) {
					names[1] = names[2];
					names[2] = ln;
				} else if (names[0].length() == 0 && names[1].length() == 0
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isNotLastName(names[2])) {
					names[0] = names[2];
					names[2] = ln;
				} else if (names[0].length() > 1 && names[1].length() == 0
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isNotLastName(names[2])) {
					if (!names[2].equals(ln)) {
						names[1] = names[2];
						names[2] = ln;
					}
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;
				}
			}

			ln = names[2];
			names[2] = names[2].replaceAll("(?is),", "");
			if (StringUtils.isNotEmpty(names[5])) {// 044200010010 SMITH GEORGE
													// T AND FAYE KEITH
				if (NameUtils.isNotCompany(names[5])) {
					if (LastNameUtils.isNotLastName(names[5])
							&& StringUtils.isNotEmpty(names[5])) {
						names[4] = names[3];
						names[3] = names[5];
						names[5] = ln;
					}
				}
			}
			types = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					types, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				stringOwner.replaceAll("\\s+&\\s+&\\s+", " & "));

	}

	@SuppressWarnings("rawtypes")
	public static void partyNamesTXBexarTR(ResultMap m, long searchId) throws Exception {

		String stringOwner = (String) m.get("tmpOwner");

		if (StringUtils.isEmpty(stringOwner))
			return;

		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);

		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN: C/O\\b", "");

		stringOwner = stringOwner.replaceAll("(?is)[^L]/[A-Z]\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bL/TR\\b", "LIVING TRUST");
		stringOwner = stringOwner.replaceAll("(?is)\\bET\\s+AL\\b", "ETAL");
		stringOwner = stringOwner.replaceAll("(?is)\\bET\\s+UX\\b", "ETUX");
		stringOwner = stringOwner
				.replaceAll("\\s*&\\s*(ETAL|ETUX)\\s*$", " $1");
		stringOwner = stringOwner.replaceAll("\\s+AND\\s+(ETAL|ETUX)\\s*$",
				" $1");
		stringOwner = stringOwner.replaceAll("\\s*\\(\\s*", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bWFE?\\b", " ");
		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN", "");
		stringOwner = stringOwner.replaceAll("\\bVLB.*", "");
		stringOwner = stringOwner.replaceAll("\\s+/\\s+", " & ");
		stringOwner = stringOwner.replaceAll("\\bAND\\b", " & ");
		stringOwner = stringOwner.replaceAll("\\bAKA\\b.*", "");
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, types, otherType;
		String ln = "";
		boolean coOwner = false;

		// Bug 6392 - (ATSdev6392) TX Dallas TR - name parsing issue
		stringOwner = org.apache.commons.lang.StringUtils.strip(stringOwner
				.replaceAll("<[^>]*>\\s*&*", " ").replaceAll("\\s+"," "));
		
		// T7615, TX Bexar TR Account Number: 017560000060  
		stringOwner = stringOwner.replaceAll("\\s+&\\s+((?:\\w+)(?:\\s+\\w+)?(?:\\s+\\w+)?(?:\\s+\\w+)?\\sL/E)", "&$1");

		String[] owners = stringOwner.split(" & ");
		if (stringOwner.matches("\\w+(\\s+\\w+)?\\s+&.*")
				&& NameUtils.isCompany(stringOwner)) {
			stringOwner = stringOwner.replaceAll("\\s+&\\s+$", "");
			stringOwner = stringOwner.replaceAll("\\s*&\\s*%\\s*", " aaaaaaa ");
			stringOwner = stringOwner.replaceAll("\\s*%\\s*", " aaaaaaa ");
			owners = stringOwner.split("aaaaaaa");
		} else if (stringOwner.indexOf("CITIES &") != -1) {
			stringOwner = stringOwner.replaceAll("(CITIES) &", "$1 aaaaaaa ");
			owners = stringOwner.split("aaaaaaa");
		}
		
		//OWENS & PERRY, TX Dallas TR, Account Number: 99200408300419800 
		boolean isCompanyTwoNames = false;
		if (owners.length==2 && LastNameUtils.isLastName(owners[0]) && LastNameUtils.isLastName(owners[1])) {
			String[] split1 = owners[0].split("\\s+");
			String[] split2 = owners[1].split("\\s+");
			if (split1.length==1 && split2.length==1) {
				owners = new String[1];
				owners[0] = stringOwner;
				isCompanyTwoNames = true;
			}
		}

		for (int i = 0; i < owners.length; i++) {
			if (i == 0) {
				if (NameUtils.isCompany(owners[i]) || isCompanyTwoNames){
					names[2] = owners[i].replaceAll("&", " & ").replaceAll("\\s{2,}", " ");
					types = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					GenericFunctions.addOwnerNames(names, "", "", types, otherType,
							true, false, body);
					continue;
				} else {
					names = StringFormats.parseNameNashville(owners[i], true);
					ln = names[2];
				}
			} else {
				String split[] = owners[i].split("\\s");
				if (split.length>=2 && LastNameUtils.isLastName(split[0]) &&
					!FirstNameUtils.isFirstName(split[0])) {
					names = StringFormats.parseNameNashville(owners[i], true);
				} else {
					names = StringFormats.parseNameDesotoRO(owners[i], true);
				}
				coOwner = true;
			}

			if (coOwner) {
				if (NameUtils.isCompany(names[2])
						&& names[2].matches("(?is).*?\\s+TR\\b")) {
					names[2] = names[2].replaceAll("(?is)(.*?)\\s+TR\\b", "$1 "
							+ ln + " TR");
				}

				if (NameUtils.isCompany(names[5]) && names[2].length() > 0) {
					names[2] = names[2] + "&" + names[5];
					names[5] = "";
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& NameUtils.isNotCompany(names[2])
						&& names[0].length() == 0 && names[1].length() == 0) {
					names[0] = names[2];
					names[2] = ln;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() != 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = names[1];
					names[1] = aux;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() == 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;

				} else if (names[2].length() == 1
						|| (LastNameUtils.isNotLastName(names[2]) && NameUtils
								.isNotCompany(names[2]))) {
					names[1] = names[2];
					names[2] = ln;
				} else if (names[0].length() == 0 && names[1].length() == 0
						&& NameUtils.isNotCompany(names[2])) {
					names[0] = names[2];
					names[2] = ln;
				} else if (names[0].length() > 1 && names[1].length() == 0
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isNotLastName(names[2])) {
					if (!names[2].equals(ln)) {
						names[1] = names[2];
						names[2] = ln;
					}
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;
				}
			}

			ln = names[2];
			names[2] = names[2].replaceAll("(?is),", "");
			types = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					types, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);

		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				stringOwner.replaceAll("\\s+&\\s+&\\s+", " & "));

	}

	public static void taxTXBexarTR(ResultMap m, long searchId)
			throws Exception {

	}

	@SuppressWarnings("rawtypes")
	public static void parseLegalTXBexarTR(ResultMap m, long searchId)
			throws Exception {

		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;
		
		//String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
		
		String originalLegal = legal;// i need the <br>
		if (countyId == CountyConstants.TX_San_Patricio)
			legal = legal.replaceAll("(?is)([A-Z]{2,5})<br>([A-Z]+)", "$1"+"$2");
		legal = legal.replaceAll("(?is)<br>", " ");
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		// legal = legal.replaceAll("(?is)&\\s+[NSWE]+\\s*(\\d+)\\s+", "& $1 ");
		// legal = legal.replaceAll("(?is)\\s+[\\d/]+\\s*FT\\s+OF\\s*(\\d+)",
		// " & $1");
		// legal =
		// legal.replaceAll("(?is)\\s+[NSEW]\\s*/\\s*\\d+\\s*OF\\s*(\\d+)",
		// " L $1");
		// legal = legal.replaceAll("(?is)(\\w),(\\w)\\s+(\\w)", "$1,$2,$3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		String legalTemp = legal;

		Pattern p = null;
		Matcher ma = null;
		// extract lot from legal description
		String lot = "";
		if (countyId == CountyConstants.TX_Nueces) {
			legal = legal.replaceAll("(?is)\\b\\s*THRU\\s*\\b", "-");
			legal = legal.replaceAll("(?is)\\b\\s*THUR\\s*\\b", "-");
			legal = legal.replaceAll("\\b\\s*AND\\s*\\b", " ");
			legal = legal.replaceAll("\\s*-\\s*", "-");
			p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*(\\d+-[A-Z]|[\\d&,\\s-]+|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b([\\.'])?");
			legalTemp = legal.replaceAll("(?is)\\bLOTT\\b", "") + " ";
			ma = p.matcher(legalTemp);
			while (ma.find()) {
				String group2 = ma.group(2).trim().replaceAll("^-$", "");
				if (ma.group(3)!=null) {
					group2 = group2.replaceAll("[^\\s]+$", "").trim();
				}
				lot = lot + " " + group2;
				//legalTemp = legalTemp.replaceFirst(group2, " ");
			}
		} else {
			p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,\\s-]+|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				if (ma.group(1).trim().length()>=2) {		//at least one of O, T and S from LO?T?S? must be present
					lot = lot + " " + ma.group(2);
					if (!ma.group(1).matches("LO?TS?\\s*"))
						legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					else 
						legalTemp = legalTemp.replaceFirst(ma.group(0),"");
				}
			}
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String ncb = "";
		String block = "";
		String blockRegEx = "";
		if (countyId == CountyConstants.TX_Bexar || countyId == CountyConstants.TX_San_Patricio) {
			// extract ncb from legal description
			p = Pattern.compile("(?is)\\b(NCB)\\s*([\\d]+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				ncb = ncb + " " + ma.group(2);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
			ncb = ncb.replaceAll("\\s*&\\s*", " ").trim();
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
	
			// extract block from legal description
			blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*(\\d+|[A-Z](?:\\s*/\\s*\\d+)?)\\b";
			p = Pattern.compile(blockRegEx);
			ma = p.matcher(legal);
			while (ma.find()) {
				block = block + " " + ma.group(2);
				if (!ma.group(1).matches("\\bBL?\\s*(?:OC)?KS?\\s*"))
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				else
					legalTemp = legalTemp.replaceFirst(ma.group(0), "");
			}
			block = block.replaceAll("\\s*&\\s*", " ").trim();
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
			
		} else if (countyId == CountyConstants.TX_Dallas) {
			blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*(\\d+|[A-Z])(?:\\s*/\\s*(\\d+))?\\b";
			p = Pattern.compile(blockRegEx);
			ma = p.matcher(legal);
			if (ma.find()) {
				block = block + " " + ma.group(2);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				ncb = ma.group(3);//third group, if exists, it is the City Block
			}
			
			block = block.replaceAll("\\s*&\\s*", " ").trim();
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else if (countyId == CountyConstants.TX_Nueces) {
			blockRegEx = "(?is)\\b(BL?K)\\s+([A-Z0-9-]+)\\b";
			p = Pattern.compile(blockRegEx);
			ma = p.matcher(legal);
			if (ma.find()) {
				block = block + " " + ma.group(2);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
			
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		if (StringUtils.isNotEmpty(ncb)) {
			ncb = LegalDescription.cleanValues(ncb, false, true);
			m.put(PropertyIdentificationSetKey.NCB_NO.getKeyName(), ncb);
		}
		
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		
		
		/*
		 * p = Pattern.compile("(?is)\\b(CB)\\s*(\\d+[A-Z]?)\\s*(P\\s*-?)\\s*(\\d+[A-Z]?)"); 
		 * ma = p.matcher(legal); while (ma.find()) { pb = pb + " " + ma.group(2); 
		 * pb = pb.trim(); pg = pg + " " + ma.group(4); pg = pg.trim(); legalTemp =
		 * legalTemp.replaceFirst(ma.group(0), ma.group(1));
		 * m.put("PropertyIdentificationSet.PlatBook", pb);
		 * m.put("PropertyIdentificationSet.PlatNo", pg); legalTemp =
		 * legalTemp.trim().replaceAll("\\s{2,}", " "); legal = legalTemp; }
		 */

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal, "SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract tract
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+([\\w-]+)\\s");
		ma = p.matcher(legal + " ");
		if (ma.find()) {
			tract = ma.group(2).replaceAll("-", " ");
			tract = LegalDescription.cleanValues(tract, false, true);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(REFER\\s*(?:TO)?\\s*:?)\\s*([\\d-\\s]+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(2).replaceAll("(?is)[\\s-]+", ""));
			line.add("");
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		ma.reset();
		p = Pattern.compile("(?is)\\bvol\\s*(\\d+)\\s*/\\s*(\\d+)\\s+(?:DD)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).trim().replaceAll("(?is)\\A0+", ""));
			line.add(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
			line.add("");
			line.add(ma.group(3).trim().replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		ma.reset();
		p = Pattern.compile("(?is)\\bvol\\s*(\\d+)\\s*/\\s*(\\d+)\\s+(?:EX)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).trim().replaceAll("(?is)\\A0+", ""));
			line.add(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
			line.add("");
			line.add(ma.group(3).trim().replaceAll("(?is)(\\d{2})(\\d{2})(\\d{2})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		ma.reset();
		p = Pattern.compile("(?is)\\bINT\\s*(\\d+)\\s+(?:DD)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1).trim());
			line.add(ma.group(2).trim().replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber", "InstrumentDate" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		legal = legal.replaceAll("", "");

		// extract subdivision name from legal description
		String subdiv = "";
		
		if (countyId == CountyConstants.TX_Dallas && !(legal.toLowerCase()).contains("personal")) {
			originalLegal = originalLegal.replaceFirst("(?is)\\A\\s*<\\s*/?\\s*br\\s*/?\\s*>\\s*", "");
			originalLegal = GenericFunctions.switchIdentifierWithNumber(originalLegal, "SEC(?:TION)?");
			String[] lines = originalLegal.split("(?is)<\\s*/?\\s*br\\s*/?\\s*>");
			if (lines.length > 0) {
				subdiv = lines[0];
				subdiv = subdiv.replaceFirst("(.*)\\s+PH\\s*.*", "$1");
				subdiv = subdiv.replaceFirst("(.*)\\s+(SEC)", "$1");
				subdiv = subdiv.replaceFirst("(.+)\\s+ABST?.*", "$1");
				subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
				subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
			}

		} else if (countyId == CountyConstants.TX_Nueces){
			p = Pattern.compile("(?i)(.*)\\b(BL?K|LO?TS?|UNIT|BLDG|TRACTS?|UNIT|PLUS|POR|SUBD?|S/D|UNREC|UNDIV|\\d+(?:\\.\\d+)?\\s*ACS|[^\\s]+')\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
				subdiv = subdiv.replaceFirst("(?i)\\bBL?K.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bLO?TS?.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bUNIT.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bBLDG.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bTRACTS?.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bUNIT.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bPOR.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bSUBD?.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bS/D.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bUNREC.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\bUNDIV.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\b\\d+(?:\\.\\d+)?\\s*ACS.*", "");
				subdiv = subdiv.replaceFirst("(?i)\\d+\\.\\s*", "");
				subdiv = subdiv.replaceFirst("(?i)\\b[^\\s]+'.*", "");
				subdiv = subdiv.replaceFirst("(?i),\\s*$", "");
				subdiv = subdiv.replaceFirst("(?i)-\\s*$", "");
				subdiv = subdiv.trim();
			}
		} else {
			boolean hasSub = false;
			p = Pattern.compile("(?is)\\(([A-Z]+[^\\)]+)\\)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
				hasSub = true;
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\\"([A-Z]+[^\\\"]+)\\\"");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(1);
					hasSub = true;
				} else {
					ma.reset();
					p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s+(.*)");
					ma.usePattern(p);
					if (ma.find()) {
						subdiv = ma.group(2);
						hasSub = true;
					}
				}
			}
			if (!hasSub | subdiv.matches("\\w\\s+\\w\\s+\\w+.*")) {
				subdiv = "";
			}
		}

		subdiv = subdiv.replaceAll("&", " & ");

		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\sFLG.*", "$1")
					.replaceFirst(blockRegEx, "")
					.replaceFirst(",", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*"))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
		}

		// extract plat book&page from legal description

		// 047110000111 CB 4711 P-11*(7.77), P-100 (.253) A-528 ; CB 4733 P-2D
		// (1.437),P-100A (1.194) A- 153

		String[] plats = legal.split("\\bCB\\b");
		String pb = "";
		String pg = "";
		for (String eachBook : plats) {
			if (StringUtils.isNotEmpty(eachBook)) {
				eachBook = "CB " + eachBook;
				p = Pattern.compile("(?is)\\b(CB)\\s*(\\d+[A-Z]?)\\s*(P\\s*-?)\\s*(\\d+[A-Z]?)");
				ma = p.matcher(eachBook);
				if (StringUtils.isNotEmpty(pb.trim())) {
					pb += ";";
				}
				if (ma.find()) {
					pb = pb + " " + ma.group(2).trim();
				}
				if (StringUtils.isNotEmpty(pg.trim())) {
					pg += ";";
				}
				p = Pattern.compile("(?is)\\b([P|A]\\s*-?)\\s*(\\d+[A-Z]?)");
				ma = p.matcher(eachBook);
				while (ma.find()) {
					pg = pg + " " + ma.group(2).trim();
				}
			}
		}
		m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb.trim());
		m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg.trim());

	}

	public static String cleanSubdivisionEllisTR(String subdivision){
		Pattern p = Pattern.compile("(?ism)SUBDIVISION\\s(\\w+)");
		Matcher ma = p.matcher(subdivision);
		String sub = "";
		
		if(ma.find()){
			sub = ma.group(1);
			return sub;
		}
		
		sub = subdivision.trim().replaceFirst("(?ism)^\\w\\s(\\w{2,}[^$]*)","$1")
				.replaceAll("(?ism)(\\d)-\\s","$1 ")
				.replaceAll(" ESTS( ?)"," ESTATES$1")
				.replaceAll("^TR\\s","")
				.replaceAll("^\\d+(-\\d+)?","").trim()
				.replaceAll("^\\d+","")
				.replaceAll("(?is)\\sPHA?S?E?\\s\\w+\\b","")
				.trim();
		
		return sub;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseLegalTXEllisTR(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?is)<br>", " ")
				.replaceAll("(?is)\\bLO T\\b", "LOT")
				.replaceAll("(?is)\\bBL OCK\\b", "BLOCK")
				.replaceAll("(?is)\\b(?:[SWNE]+|PT)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "")
				.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "")
				.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3")
				.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2")
				.replaceAll("\\s+"," ")
				.replaceAll("(?is)\\sPH\\sI\\s", " PH 1 ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		String legalTemp = legal;

		//get subdivision
		String subdivision = "";
		
		Pattern p = null;
		
		if (legal.contains("IMP ONLY")) {
			p = Pattern.compile("(?ism)()(\\d+[a-zA-Z]*\\s)(.*?)(IMP ONLY)?$");
		} else if (legal.contains("ACRES")) {
			p = Pattern.compile("(?is)(.*?)(\\d+[a-zA-Z]*\\s)(.*?)(\\d+(\\.\\d+)?\\sACRES).*$");
		} else {
			p = Pattern.compile("(?is)(.*?)(\\d+[a-zA-Z]*\\s)(.*?)$");
		} //(?ism)^(\\d+\\s)?(\\d+\\s)?([^\\d]+(#\\d+)?)
			
		Matcher ma = p.matcher(legal);
		
		if(ma.find()){
			subdivision = ma.group(3)+"";
			subdivision = cleanSubdivisionEllisTR(subdivision);
//			legalTemp = legalTemp.replaceFirst(subdivision, " ");
//			legalTemp = legalTemp.trim().replaceAll("\\s+", " ");
//			legal = legalTemp;
		}
		
		if (StringUtils.isNotEmpty(subdivision)) {
//			System.out.println(subdivision);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		}
		
		// extract lot from legal description
		String lot = "";
		p = Pattern.compile("(?is)\\b(LO?T?S?\\s*:?)\\s*([\\d&,\\s-]+|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract ncb from legal description
		String ncb = "";
		p = Pattern.compile("(?is)\\b(NCB)\\s*([\\d]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			ncb = ncb + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		ncb = ncb.replaceAll("\\s*&\\s*", " ").trim();
		if (ncb.length() != 0) {
			ncb = LegalDescription.cleanValues(ncb, false, true);
			m.put(PropertyIdentificationSetKey.NCB_NO.getKeyName(), ncb);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		String blockRegEx = "(?is)\\b(BL?\\s*(?:OC)?KS?\\s*:?)\\s*(\\d+|[A-Z](?:\\s*/\\s*\\d+)?)\\b";
		p = Pattern.compile(blockRegEx);
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern
				.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		legal = GenericFunctions.switchIdentifierWithNumber(legal,
				"SEC(?:TION)?");
		p = Pattern.compile("(?is)\\b(SEC?T?(?:ION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(REFER\\s*(?:TO)?\\s*:?)\\s*([\\d-\\s]+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(2).replaceAll("(?is)[\\s-]+", ""));
			line.add("");
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		ma.reset();
		p = Pattern
				.compile("(?is)\\bvol\\s*(\\d+)\\s*/\\s*(\\d+)\\s+(?:DD)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).trim().replaceAll("(?is)\\A0+", ""));
			line.add(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
			line.add("");
			line.add(ma.group(3).trim()
					.replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		ma.reset();
		p = Pattern
				.compile("(?is)\\bvol\\s*(\\d+)\\s*/\\s*(\\d+)\\s+(?:EX)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).trim().replaceAll("(?is)\\A0+", ""));
			line.add(ma.group(2).trim().replaceAll("(?is)\\A0+", ""));
			line.add("");
			line.add(ma.group(3).trim()
					.replaceAll("(?is)(\\d{2})(\\d{2})(\\d{2})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		ma.reset();
		p = Pattern.compile("(?is)\\bINT\\s*(\\d+)\\s+(?:DD)(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1).trim());
			line.add(ma.group(2).trim()
					.replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1-$2-$3"));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber",
					"InstrumentDate" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat book&page from legal description

		// 047110000111 CB 4711 P-11*(7.77), P-100 (.253) A-528 ; CB 4733 P-2D
		// (1.437),P-100A (1.194) A- 153

		String[] plats = legal.split("\\bCB\\b");
		String pb = "";
		String pg = "";
		for (String eachBook : plats) {
			if (StringUtils.isNotEmpty(eachBook)) {
				eachBook = "CB " + eachBook;
				p = Pattern
						.compile("(?is)\\b(CB)\\s*(\\d+[A-Z]?)\\s*(P\\s*-?)\\s*(\\d+[A-Z]?)");
				ma = p.matcher(eachBook);
				if (StringUtils.isNotEmpty(pb.trim())) {
					pb += ";";
				}
				if (ma.find()) {
					pb = pb + " " + ma.group(2).trim();
				}
				if (StringUtils.isNotEmpty(pg.trim())) {
					pg += ";";
				}
				p = Pattern.compile("(?is)\\b([P|A]\\s*-?)\\s*(\\d+[A-Z]?)");
				ma = p.matcher(eachBook);
				while (ma.find()) {
					pg = pg + " " + ma.group(2).trim();
				}
			}
		}
		m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb.trim());
		m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg.trim());

	}

	public static void parseAddressTXBexarTR(ResultMap m, long searchId)
			throws Exception {

		String address = (String) m.get("tmpAddress");

		if (StringUtils.isEmpty(address))
			return;
		address = address.replaceAll("@.*", "");
		address = address.replaceAll("(?is)<br/?>", " ");
		if (address.contains("ISLAND")) {
			address = address.replaceAll("\\bIS\\s", "");
		}
		if (address.matches("\\d+\\s+\\w+\\s+\\d+")) {// 051410000071
			address = address.replaceAll("(?is)(\\d+)\\s+(\\w+)\\s+(\\d+)",
					"$1 $2 #$3");
		}
		address = address.replaceAll("U.S.", "US");
		if (address.matches("(?is).*?\\bBUS\\s*\\(([^\\)]+)\\)")) {
			Pattern pat = Pattern.compile("(?is).*?\\bBUS\\s*\\(([^\\)]+)\\)");
			Matcher mat = pat.matcher(address);
			if (mat.find()) {
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), mat.group(1));
			}
		}
		address = address.replaceAll("(\\d+\\s+\\w)\\s+BUS\\s+\\(.*", "$1"); // 073700970200 - nueces

		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()));

	}

	public static void parseAddressTXDallasTR(ResultMap m, long searchId)
			throws Exception {

		String address = (String) m.get("tmpAddress");

		if (StringUtils.isEmpty(address))
			return;

		address = address.replaceAll("(?is)<br/?>", " ");
		address = address.replaceAll(",\\s*[A-Z]{2}\\s*$", "");
		address = address.replaceAll(",\\s*0+\\s*$", "");
		address = address.replaceAll("(?is)\\b(UNIT\\s+\\d*)[A-Z]+\\b", "$1");
		address = address.replaceAll(",", "");

		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(address.trim()));
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(address.trim()));

	}

}

package ro.cst.tsearch.servers.functions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

/**
 * 
 * @author Oprina George
 * 
 *         May 21, 2012
 */

public class COElPasoTR {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 3) {
			String parcel = cols[0].toPlainTextString();
			String owner = cols[1].toPlainTextString();
			String addr = cols[2].toPlainTextString();

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.replaceAll("[^\\d]", ""));
			}

			parseAddress(m, addr);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(owner)) {
				List<String> names = new ArrayList<String>();
				names.add(owner);
				parseNames(m, names, "");
			}
		}
		return m;
	}

	public static void parseNames(ResultMap m, List<String> all_names, String auxString) {
		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			for (String n : all_names) {
				n = n.toUpperCase().trim().replaceAll("\\s+", " ");
				n = n.replaceAll("\\sAND\\s", " & ");
				n = n.replaceAll("(?is)\\bCO\\s*(TRUSTEES?)\\b", "$1");
				n = n.replace("+", "&");
				n = n.replace("\\d+$", "").trim();

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), StringUtils.strip(nameOnServer.toString()).replaceAll("\\&$", ""));
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		if (StringUtils.isEmpty(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " "));

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(addr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(addr)));
	}

	public static void parseLegal(ResultMap m, String legal) {
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes);

		Pattern LEGAL_LOT = Pattern.compile("\\bLOTS? ?(\\d+)");
		Pattern LEGAL_BLK = Pattern.compile("\\bBLK ?(\\d+)");
		Pattern LEGAL_SEC = Pattern.compile("\\bSEC ?(\\d+-\\d+-\\d+)");
		Pattern LEGAL_TRACT = Pattern.compile("\\bTRACT ?(\\d+)");

		// get lot
		Matcher matcher = LEGAL_LOT.matcher(legalDes);
		if (matcher.find()) {
			String lot = matcher.group(1);
			if (StringUtils.isNotEmpty(lot)) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			}
			legalDes = legalDes.replaceFirst(matcher.group(1), " ");
		}

		// get blk
		matcher = LEGAL_BLK.matcher(legalDes);
		if (matcher.find()) {
			String blk = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(1), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk.trim());
		}

		// get S-T-R
		matcher = LEGAL_SEC.matcher(legalDes);
		if (matcher.find()) {
			String str = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(1), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), str.split("-")[0].replaceAll("[^\\d]", ""));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), str.split("-")[1].replaceAll("[^\\d]", ""));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), str.split("-")[2].replaceAll("[^\\d]", ""));
		}

		// get tract
		matcher = LEGAL_TRACT.matcher(legalDes);
		if (matcher.find()) {
			String tract = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(1), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract.trim());
		}
		
		// get subivision name
		legalDes = legalDes.replaceAll("\\s+", " ");
		Pattern p = Pattern.compile("\\b(LOTS?|BLK|TRACT) ([\\w ]+ (SUB|FILI?N?G?|ESTATES) ?(NO)? ?\\d*)");
		matcher = p.matcher(legalDes);

		String subName = "";

		if (matcher.find()) {
			subName = matcher.group(2).replace("BLK ", "");
		} else {
			if (legalDes.contains("LOT") || legalDes.contains("BLK")) {
				String[] parts = legalDes.split("(LOT | LOT)");
				if (parts.length != 2) {
					parts = legalDes.split("(BLK | BLK)");
				}

				if (parts.length == 2)
					if (StringUtils.isNotEmpty(parts[0].trim()))
						subName = parts[0].replace("BLK ", "").replace("LOT ", "").replaceAll("\\s+", " ").trim();
					else if (StringUtils.isNotEmpty(parts[1].trim())) {
						subName = parts[1].replace("BLK ", "").replace("LOT ", "").replaceAll("\\s+", " ").trim();
					}
			}
		}

		if (subName.contains(" OF ")) {
			String[] parts = subName.split(" OF ");
			subName = parts[parts.length - 1];
		}

		subName = subName.replaceAll("\\s+", " ").trim();

		if (StringUtils.isNotEmpty(subName)) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subName);
		}
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

			NodeList nodes = new HtmlParser3(detailsHtml).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			String parcel = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Schedule Number:"), "", true)
					.replaceAll("<[^>]*>", "").trim();
			if (StringUtils.isNotEmpty(parcel)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}

			String owner = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_txtName", tables, true).replaceAll("(?ism)&AMP;", " & ").replaceAll("\\s+", " ")
					.trim();
			if (StringUtils.isNotEmpty(owner)) {
				List<String> names = new ArrayList<String>();
				String parts[];
				if ((parts = owner.split(" ")).length == 6 && !owner.contains("&")) {
					String o1 = parts[0] + " " + parts[1] + " " + parts[2];
					names.add(o1);
					String o2 = parts[3] + " " + parts[4] + " " + parts[5];
					names.add(o2);
				} else if (owner.contains("TRUST") && 
						(parts = owner.replace("&", "").replaceAll("\\s+"," ").trim().split(" ")).length > 5 &&
						parts[parts.length-1].contains("TRUST")) {
					String o1 = parts[0] + " " + parts[1] + " " + parts[2];
					names.add(o1);
					StringBuffer o2 = new StringBuffer();
					for(int i =3 ; i<parts.length ;i++){
						o2.append(" " + parts[i]);
					}
					names.add(o2.toString().trim());
				} else {
					names.add(owner);
				}
				parseNames(resultMap, names, "");
			}

			String year = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblPropertyTaxesYear", tables, true).trim();
			year = year.replaceAll("(?ism)Due \\d+", "").replaceAll("[^\\d]", "");
			if (StringUtils.isNotEmpty(year)) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			}

			String addr = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_txtPropertyAddress", tables, true).replaceAll("\\s+", " ").trim();
			if (StringUtils.isNotEmpty(addr)) {
				parseAddress(resultMap, addr);
			}

			String legal = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblLegalDescription", tables, true).replaceAll("\\s+", " ").trim();

			if (StringUtils.isNotEmpty(legal)) {
				parseLegal(resultMap, legal);
			}

			// parse taxes
			String land = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblTotalAssessedLand", tables, true).replaceAll("[ $-,]", "");
			String improvments = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblTotalAssessedImprovements", tables, true).replaceAll("[ $-,]", "");
			String totalAssesed = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblTotalAssessed", tables, true).replaceAll("[ $-,]", "");
			String totalAppr = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblTotalMarketValue", tables, true).replaceAll("[ $-,]", "");

			if (StringUtils.isNotEmpty(land)) {
				resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
			}

			if (StringUtils.isNotEmpty(improvments)) {
				resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvments);
			}

			if (StringUtils.isNotEmpty(totalAssesed)) {
				resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssesed);
			}

			if (StringUtils.isNotEmpty(totalAppr)) {
				resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), totalAppr);
			}

			String ba = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblBaseTaxAmount", tables, true).replaceAll("[ $-,]", "");
			if (StringUtils.isNotEmpty(ba)) {
				resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), ba);
			}

			Node apNode = HtmlParser3.getNodeByID("ContentPlaceHolder1_gvCurrentYearPaymentsReceived", tables, true);

			double ap = 0;
			DecimalFormat df = new DecimalFormat("#.##");

			if (apNode != null) {
				TableTag apTable = (TableTag) apNode;
				TableRow[] rows = apTable.getRows();

				String dp = "";

				for (int i = 0; i < rows.length; i++) {
					if (rows[i].getColumnCount() == 2) {
						dp = rows[i].getColumns()[0].toPlainTextString().trim();
						String apString = rows[i].getColumns()[1].toPlainTextString().replaceAll("[ $-,]", "");
						if (StringUtils.isNotEmpty(apString) && apString.matches("\\d+\\.\\d+")) {
							ap += Double.parseDouble(apString);
						}
					}
				}

				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), df.format(ap));

				if (StringUtils.isNotEmpty(dp)) {
					resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), dp);
				}
			}

			if (!Double.toString(ap).equals(ba)) {
				String adString = HtmlParser3.getNodeValueByID("ContentPlaceHolder1_lblTotalAmount", tables, true).replaceAll("[ $-,]", "");

				if (StringUtils.isNotEmpty(adString) && adString.matches("\\d+\\.\\d+")) {
					double ad = Double.parseDouble(adString) - ap;
					if (ad > 0)
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), df.format(ad));
					// resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), r.getColumns()[1].toPlainTextString().trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}

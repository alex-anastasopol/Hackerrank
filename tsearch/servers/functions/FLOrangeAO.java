package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

public class FLOrangeAO {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");

		TableColumn[] cols = row.getColumns();

		if (cols.length >= 4) {
			String owner = cols[0].toHtml();
			String addr = cols[1].toPlainTextString();
			String parcel = cols[3].toPlainTextString();

			if (StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}

			parseAddress(m, addr);

			if (StringUtils.isNotEmpty(owner)) {
				List<String> names = new ArrayList<String>();

				names.addAll(Arrays.asList(owner.replace("&amp;", "&").split("<br ?/>")));
				parseNames(m, names, "");
			}

		} else if (cols.length >= 1) {
			// parse legal for 1st level intermediaries 
			String col = cols[0].toPlainTextString();
			Pattern pat = Pattern.compile("(?is)\\b(?:U(?:NI)?T|UN)\\b\\s*([\\dA-Z]*)");
			Matcher mat = pat.matcher(col);
			if (mat.find()) {
				String unit = mat.group(1);
				if (!unit.isEmpty()) {
					m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
				}
				col = col.replaceFirst(mat.group(), "").trim();
			}

			pat = Pattern.compile("(?is)\\bSEC\\b\\s*([\\dA-Z]+)\\b");
			mat = pat.matcher(col);
			if (mat.find()) {
				String sec = mat.group(1);
				if (!sec.isEmpty()) {
					m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
				}
				col = col.replaceFirst(mat.group(), "").replaceAll("\\)|\\(", "").trim();
			}
			
			col = col.replaceFirst("(?is)\\s*FIRST\\s+REP?(LAT)?\\s*$", "");
			if (!col.isEmpty()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), col);
			}
		}
		return m;
	}

	public static void parseNames(ResultMap m, List<String> all_names, String auxString) {
		try {
			if (all_names == null)
				return;
			if (all_names.size() == 0)
				return;

			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			for (String n : all_names) {
				n = n.toUpperCase().replaceAll("<[^>]*>", "")
						.replaceAll("REM:", " ")
						.replaceAll("(?ism)\\d+/\\d+ INT", " ")
						.replaceAll("\\s+", " ")
						.trim();

				if (StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = new String[] { "", "", "", "", "", "" };
					names = StringFormats.parseNameNashville(n, true);
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

		String newAddr = addr;

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(newAddr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(newAddr)));
	}

	public static void parseLegal(ResultMap m, String legal) {
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		legalDes = legalDes.replace("Abbreviated Description", "").
				replaceAll("[\r\t]", "").replaceAll("\n", "%").replaceAll("\\s+", " ").replaceAll(" *% *", "%").replaceAll("%+", "%").
				replaceAll("(?ism)&nbsp;", " ").replaceAll("(?ism)&amp;", " ").replaceAll("\\s+", " ").trim();

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes.replaceAll("%", " ").replaceAll("\\s+", " "));

		legalDes = GenericFunctions.replaceNumbers(legalDes);

		Pattern LEGAL_LOT = Pattern.compile("\\b(LOTS?) (\\d+|\\w{1})\\b,?&? ?");
		Pattern LEGAL_BLK = Pattern.compile("\\bBLK ?((?:[A-Z]-?)?\\d*)");
		Pattern LEGAL_PHASE = Pattern.compile("\\bPHA?S?E? (\\d+-\\w{1}|\\d+|\\w{1})");
		Pattern LEGAL_BLDG = Pattern.compile("\\bBLDG ?(\\d+|\\w{1})");
		Pattern LEGAL_UNIT = Pattern.compile("\\bUNIT ?(\\d+|\\w{1})");
		Pattern LEGAL_STR = Pattern.compile("\\bSEC ?(\\d+-\\d+-\\d+)");
		Pattern LEGAL_SUBDIV = Pattern.compile("\\b(.*?)\\s+(TR(?:ACTS?)?|BLK|PH(?:ASE)?|LOTS?|UNIT)");
		Pattern BK_PG_REF = Pattern.compile("(?is).*\\b([A-Z]|\\d+)\\s*/\\s*(\\d+)");

		// get subdiv name
		Matcher matcher = LEGAL_SUBDIV.matcher(legalDes);

		if (matcher.find()) {
			String subdiv = matcher.group(1);
			subdiv = subdiv.replaceAll("\\w+/\\w+", "").trim();
			subdiv = subdiv.replaceFirst("(?is)\\bSUB(?:DIVISION)?\\b", "").trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
		}

		matcher = LEGAL_LOT.matcher(legalDes);

		StringBuffer lots = new StringBuffer();

		for (int i = 0; matcher.find() && i < 5; i++) {
			String lot = matcher.group(2);
			if (StringUtils.isNotEmpty(lot)) {
				lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot.trim()).replace("&", " ").replaceAll("\\s+", " ");

				lots.append(" " + lot.trim()).append(" ");
			}
			legalDes = legalDes.replaceFirst(matcher.group(), matcher.group(1) + " ").replaceAll("\\s+", " ");
			matcher = LEGAL_LOT.matcher(legalDes);
		}

		legalDes = legalDes.replaceAll("(?ism)\\bLOTS?", "");

		m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.toString().trim().replaceAll(",", ""));

		// get blk
		matcher = LEGAL_BLK.matcher(legalDes);
		if (matcher.find()) {
			String blk = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk.trim());
		}

		// get unit
		matcher = LEGAL_UNIT.matcher(legalDes);
		if (matcher.find()) {
			String unit = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit.trim());
		}

		// get bldg
		matcher = LEGAL_BLDG.matcher(legalDes);
		if (matcher.find()) {
			String bldg = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg.trim());
		}

		// get phase
		matcher = LEGAL_PHASE.matcher(legalDes);
		if (matcher.find()) {
			String phase = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase.trim());
		}

		// get str
		matcher = LEGAL_STR.matcher(legalDes);
		if (matcher.find()) {
			String str = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");

			String section = str.split("-")[0];
			String township = str.split("-")[1];
			String range = str.split("-")[2];

			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(section.trim()));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(township.trim()));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(range.trim()));
		}
		
		//get bk-pg references
		matcher = BK_PG_REF.matcher(legalDes);
		if (matcher.find()) {
			String bk = matcher.group(1).trim();
			String pg = matcher.group(2).trim();
			
			if (StringUtils.isNotBlank(bk) && StringUtils.isNotBlank(pg)) {
//				m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), "P" + bk);
				m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), bk);
				m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg);
			}
		}

	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml).getNodeList();

			NodeList divs = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true);
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			NodeList auxNodes = divs.extractAllNodesThatMatch(new HasAttributeFilter("class", "DetailsSummary_TitleContainer"))
					.extractAllNodesThatMatch(new TagNameFilter("span"), true).extractAllNodesThatMatch(new HasAttributeFilter("style"));
			if (auxNodes != null && auxNodes.size() > 0) {
				String parcel = auxNodes.elementAt(0).toPlainTextString().replaceAll("[^\\d-]", "");

				if (StringUtils.isNotEmpty(parcel)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
				}
			}

			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("class", "DetailsSummary_Master"));

			if (auxNodes != null && auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				if (t.getRowCount() == 1 && t.getRow(0).getColumnCount() == 2) {
					TableColumn tc = t.getRow(0).getColumns()[0];
					if (tc.getChildCount() > 0) {
						auxNodes = tc.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"));

						ArrayList<String> names = new ArrayList<String>();

						for (int i = 0; i < auxNodes.size(); i++) {
							if (((Div) auxNodes.elementAt(i)).getAttributesEx().size() == 1) {
								names.addAll(Arrays.asList(auxNodes.elementAt(i).toPlainTextString()));
							}
						}

						parseNames(resultMap, names, "");
					}

					tc = t.getRow(0).getColumns()[1];
					if (tc.getChildCount() > 5) {
						parseAddress(resultMap, tc.getChild(5).toHtml().trim());
					}

					if (tc.getChildCount() > 12) {
						String cityZip = tc.getChild(12).toHtml();

						if (StringUtils.isNotEmpty(cityZip) && cityZip.contains(",")) {
							String city = cityZip.split(",")[0];
							String zip = cityZip.split(",")[1].replaceAll("[^\\d]", "");

							if (StringUtils.isNotEmpty(city)) {
								resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.trim());
							}

							if (StringUtils.isNotEmpty(zip)) {
								resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip.trim());
							}
						}
					}
				}
			}

			auxNodes = divs
					.extractAllNodesThatMatch(new HasAttributeFilter("id",
							"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_DetailsTab_DetailsController1_ctl00_TabContainer1_PropertyFeature"));

			// get legal
			if (auxNodes != null && auxNodes.size() > 0) {
				if (auxNodes.elementAt(0).getChildren().size() > 7) {
					parseLegal(resultMap, auxNodes.elementAt(0).getChildren().elementAt(7).toPlainTextString());
				}
			}

			auxNodes = tables
					.extractAllNodesThatMatch(new HasAttributeFilter(
							"id",
							"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_DetailsTab_DetailsController1_ctl00_TabContainer1_PropertyFeature_PropertyFeatures1_LandGrid"));

			if (auxNodes != null && auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				if (t.getRowCount() == 2) {
					TableRow tr = t.getRow(1);

					if (tr.getColumnCount() == 7) {
						String landValue = tr.getColumns()[4].toPlainTextString().replaceAll("[ $-,]", "");

						if (StringUtils.isNotEmpty(landValue)) {
							resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue);
						}
					}
				}
			}

			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("class", "BenefitsGrid EqGrid"));
			if (auxNodes != null && auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				if (t.getRowCount() == 2) {
					TableRow tr = t.getRow(1);

					if (tr.getColumnCount() >= 3) {
						try {
							int index = tr.getColumnCount() - 1;

							if (tr.getColumns()[index - 1].getChildCount() > 1 && tr.getColumns()[index - 1].getChild(1) instanceof TableTag) {
								TableTag tt = (TableTag) tr.getColumns()[index - 1].getChild(1);
								String market = tt.getRow(0).toPlainTextString().replaceAll("\\([^)]*\\)", "").replaceAll("[ =$,-]", "")
										.replaceAll("[^\\d.]", "");

								if (StringUtils.isNotEmpty(market)) {
									resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), market);
								}
							}

							if (tr.getColumns()[index].getChildCount() > 1 && tr.getColumns()[index].getChild(1) instanceof TableTag) {
								TableTag tt = (TableTag) tr.getColumns()[index].getChild(1);
								String assessed = tt.getRow(0).toPlainTextString().replaceAll("\\([^)]*\\)", "").replaceAll("[ =$,-]", "")
										.replaceAll("[^\\d.]", "");

								if (StringUtils.isNotEmpty(assessed)) {
									resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessed);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}

			// related docs
			auxNodes = tables
					.extractAllNodesThatMatch(new HasAttributeFilter(
							"id",
							"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_DetailsTab_DetailsController1_ctl00_TabContainer1_SaleAnalysis_SalesAnalysis1_Grid1"));

			if (auxNodes.size() > 0) {

				TableTag t = (TableTag) auxNodes.elementAt(0);
				TableRow[] rows = t.getRows();

				NodeList aux = new NodeList();

				String[] salesHeader = { SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
						SaleDataSetKey.BOOK.getShortKeyName(),
						SaleDataSetKey.PAGE.getShortKeyName(),
						SaleDataSetKey.SALES_PRICE.getShortKeyName(),
						SaleDataSetKey.RECORDED_DATE.getShortKeyName(),
						SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
						SaleDataSetKey.GRANTOR.getShortKeyName(),
						SaleDataSetKey.GRANTEE.getShortKeyName() };
				List<List<String>> salesBody = new ArrayList<List<String>>();
				List<String> salesRow = new ArrayList<String>();
				ResultTable resT = new ResultTable();
				Map<String, String[]> salesMap = new HashMap<String, String[]>();
				for (String s : salesHeader) {
					salesMap.put(s, new String[] { s, "" });
				}

				for (int i = 1; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.getColumnCount() == 8) {
						// parse related docs
						salesRow = new ArrayList<String>();

						String recDate = r.getColumns()[0].toPlainTextString();
						String saleAmount = r.getColumns()[1].toPlainTextString();
						String instr = r.getColumns()[2].toPlainTextString();
						String bp = r.getColumns()[3].toPlainTextString();
						String grantee = r.getColumns()[4].toPlainTextString();
						String grantor = r.getColumns()[5].toPlainTextString();

						String type = "";
						if (r.getColumns()[6].getChildCount() >= 3 && r.getColumns()[6].getChild(3) instanceof Div) {
							type = r.getColumns()[6].getChild(3).toPlainTextString().replace("&nbsp;", "");
						}
						String book = "";
						String page = "";

						if (bp.contains("/")) {
							book = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bp.split("/")[0].trim());
							page = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bp.split("/")[1].trim());
						}

						salesRow.add(StringUtils.defaultString(instr).trim());
						salesRow.add(StringUtils.defaultString(book).trim());
						salesRow.add(StringUtils.defaultString(page).trim());
						salesRow.add(StringUtils.defaultString(saleAmount).replaceAll("[ $,-]", ""));
						salesRow.add(StringUtils.defaultString(recDate).trim());
						salesRow.add(StringUtils.defaultString(type).trim());
						salesRow.add(StringUtils.defaultString(grantor).replace("&nbsp;", "").trim());
						salesRow.add(StringUtils.defaultString(grantee).replace("&nbsp;", "").trim());

						salesBody.add(salesRow);
						aux = new NodeList();
					} else {
						aux.add(r);
					}
				}

				if (salesBody.size() > 0) {
					resT.setHead(salesHeader);
					resT.setMap(salesMap);
					resT.setBody(salesBody);
					resT.setReadOnly();
					resultMap.put("SaleDataSet", resT);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}

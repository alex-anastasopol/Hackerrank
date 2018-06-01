package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
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
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

public class ILCookRO {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();

		TableColumn[] cols = row.getColumns();

		if (row.getColumnCount() == 3 || cols.length == 6 || cols.length == 7 || cols.length == 8) {
			switch (additionalInfo) {
			case 0: {
				ArrayList<String> names = new ArrayList<String>();
				names.add(cols[0].toPlainTextString().trim());

				parseNames(m, names, "GrantorSet");

				names = new ArrayList<String>();
				names.add(cols[1].toPlainTextString().trim());

				parseNames(m, names, "GranteeSet");

				String parcel = cols[5].toPlainTextString();

				if (StringUtils.isNotEmpty(parcel)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.trim());
				}

				String docNo = cols[2].toPlainTextString();

				if (StringUtils.isNotEmpty(docNo)) {
					m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), docNo.trim());
				}

				String instrumentType = cols[3].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(instrumentType)) {
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrumentType);
				}

				String recDate = cols[4].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(recDate)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				}

				break;
			}
			case 1: {
				ArrayList<String> names = new ArrayList<String>();
				names.add(cols[4].toPlainTextString().trim());

				parseNames(m, names, "GrantorSet");

				names = new ArrayList<String>();
				names.add(cols[5].toPlainTextString().trim());

				parseNames(m, names, "GranteeSet");

				String parcel = cols[1].toPlainTextString();
				if (StringUtils.isNotEmpty(parcel)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.trim());
				}

				String docNo = cols[3].toPlainTextString();
				if (StringUtils.isNotEmpty(docNo)) {
					m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), docNo.trim());
				}

				String instrumentType = cols[2].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(instrumentType)) {
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrumentType);
				}

				String recDate = cols[0].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(recDate)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				}
				break;
			}
			case 2: {

				ArrayList<String> names = new ArrayList<String>();
				names.add(cols[4].toPlainTextString().trim());

				parseNames(m, names, "GrantorSet");

				names = new ArrayList<String>();
				names.add(cols[5].toPlainTextString().trim());

				parseNames(m, names, "GranteeSet");

				String parcel = cols[3].toPlainTextString();
				if (StringUtils.isNotEmpty(parcel)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.trim());
				}

				String docNo = cols[0].toPlainTextString();
				if (StringUtils.isNotEmpty(docNo)) {
					m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), docNo.trim());
				}

				String instrumentType = cols[1].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(instrumentType)) {
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrumentType);
				}

				String recDate = cols[2].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(recDate)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				}
				break;

			}
			case 3: {
				String docNo = cols[0].toPlainTextString();
				if (StringUtils.isNotEmpty(docNo)) {
					m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), docNo.trim());
				}

				String instrumentType = cols[2].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(instrumentType)) {
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrumentType);
				}

				String recDate = cols[1].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(recDate)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				}
				break;
			}
			case 5: {
				String docNo = cols[2].toPlainTextString();
				if (StringUtils.isNotEmpty(docNo)) {
					m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), docNo.trim());
				}

				String instrumentType = cols[1].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(instrumentType)) {
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrumentType);
				}

				String recDate = cols[1].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(recDate)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				}
				break;
			}
			default:
				break;
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

			if (StringUtils.isEmpty(auxString))
				return;

			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer name = new StringBuffer();

			for (String n : all_names) {
				n = n.toUpperCase()
						.replaceAll("<[^>]*?>", "")
						.replaceAll("\\s+", " ")
						.trim();

				if (StringUtils.isNotEmpty(n)) {
					name.append(n + " & ");
					String[] names = new String[] { "", "", "", "", "", "" };
					names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(n, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}

			if ("GrantorSet".equals(auxString)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), StringUtils.strip(name.toString()).replaceAll("\\&$", ""));
			} else if ("GranteeSet".equals(auxString)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), StringUtils.strip(name.toString()).replaceAll("\\&$", ""));
			}

			if (body.size() > 0) {
				m.put(auxString, GenericFunctions.storeOwnerInSet(body, true));
			}

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
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml.replaceAll("(?sim)&nbsp;", " ").replaceAll("(?sim)<th", "<td").replaceAll("(?sim)</th", "</td"))
					.getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			NodeList auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Details"));

			if (auxNodes.size() > 0) {
				String documentNo = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Document No."), "", false).trim();

				if (StringUtils.isNotEmpty(documentNo)) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNo);
				}

				String documentDate = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Executed"), "", false).trim();

				if (StringUtils.isNotEmpty(documentDate)) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), documentDate);
				}

				String recDate = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Recorded"), "", false).trim();

				if (StringUtils.isNotEmpty(recDate)) {
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				}

				String docType = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Document Type"), "", false).trim();

				if (StringUtils.isNotEmpty(docType)) {
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
				}

				String caseNo = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Case No."), "", false).trim();

				if (StringUtils.isNotEmpty(caseNo)) {
					resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), caseNo);
				}

				String amount = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Amount"), "", false).trim();

				if (StringUtils.isNotEmpty(amount)) {
					resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount.replaceAll("[ ,$-]", ""));
				}
			}

			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_LegalDescription"));

			if (auxNodes.size() > 0) {

				String parcel = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "PIN"), "", false).trim();
				if (StringUtils.isNotEmpty(parcel)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
				}

				String unit = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Condo Unit Num."), "", false).trim();
				if (StringUtils.isNotBlank(unit)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
				}

				String str = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "S-T-R"), "", false).trim();
				if (StringUtils.isNotBlank(str) && str.split("-").length == 3) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), str.split("-")[0]);
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), str.split("-")[1]);
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), str.split("-")[2]);
				}

				String subdiv = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Sub Div-Condo"), "", false).trim();
				if (StringUtils.isNotBlank(subdiv)) {
					if (subdiv.matches("\\d+")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_CODE.getKeyName(), subdiv);
					} else {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					}
				}

				String lot = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Lot"), "", false).trim();
				String partOflot = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Part of Lot"), "", false).trim();
				if (StringUtils.isNotBlank(lot)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), (lot + partOflot).replaceAll("-$", ""));
				}

				String block = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Block"), "", false).trim();
				if (StringUtils.isNotBlank(block)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
				}

				String building = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(auxNodes, "Building"), "", false).trim();
				if (StringUtils.isNotBlank(building)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), building);
				}
			}

			// names
			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Grantor"));

			if (auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				TableRow[] rows = t.getRows();

				ArrayList<String> names = new ArrayList<String>();

				for (int i = 1; i < rows.length; i++) {
					names.add(rows[i].getColumns()[0].toPlainTextString());
				}

				parseNames(resultMap, names, "GrantorSet");
			}

			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Grantee"));

			if (auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				TableRow[] rows = t.getRows();

				ArrayList<String> names = new ArrayList<String>();

				for (int i = 1; i < rows.length; i++) {
					names.add(rows[i].getColumns()[0].toPlainTextString());
				}

				parseNames(resultMap, names, "GranteeSet");
			}

			// crossrefs
			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Referance"));

			if (auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				TableRow[] rows = t.getRows();

				@SuppressWarnings("rawtypes")
				List<List> bodyCR = new ArrayList<List>();
				List<String> line = new ArrayList<String>();

				for (int j = 1; j < rows.length; j++) {
					if (rows[j].getColumnCount() == 2) {
						line = new ArrayList<String>();
						line.add(rows[j].getColumns()[0].toPlainTextString());
						line.add(rows[j].getColumns()[1].toPlainTextString());
						bodyCR.add(line);
					}
				}

				if (bodyCR.size() > 0) {
					String[] header = { CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(), CrossRefSetKey.YEAR.getShortKeyName() };
					ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
					resultMap.put("CrossRefSet", rt);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static HashMap<String, String> docTypeSelect = new HashMap<String, String>();
	
	static {
		docTypeSelect.put("-2","Search All Document Types");
		docTypeSelect.put("-145","ABANDONMENT");
		docTypeSelect.put("-228","ABROGATION");
		docTypeSelect.put("-156","ACCEPTANCE");
		docTypeSelect.put("-110","ACKNOWLEDGMENT");
		docTypeSelect.put("-45","ADDENDUM");
		docTypeSelect.put("-22","AFFIDAVIT");
		docTypeSelect.put("-20","AGREEMENT");
		docTypeSelect.put("166","AMENDED LIS PENDENS ");
		docTypeSelect.put("168","AMENDED LIS PENDENS FORECLOSURE");
		docTypeSelect.put("-31","AMENDMENT");
		docTypeSelect.put("-55","AMENDMENT RESTRICTIONS");
		docTypeSelect.put("-133","APPLICATION");
		docTypeSelect.put("-100","APPOINTMENT");
		docTypeSelect.put("-14","ASSIGNMENT");
		docTypeSelect.put("-137","ASSIGNMENT OF BENEFICIAL INT");
		docTypeSelect.put("149","ATTY REGIS DISC COMM");
		docTypeSelect.put("-41","AUTHORITY");
		docTypeSelect.put("-122","AUTHORIZATION");
		docTypeSelect.put("-131","BANKRUPTCY");
		docTypeSelect.put("-102","BILL OF SALE");
		docTypeSelect.put("-130","BOND");
		docTypeSelect.put("-124","BY-LAWS");
		docTypeSelect.put("-84","CANCELLATION");
		docTypeSelect.put("-157","CEMETERY DEED");
		docTypeSelect.put("-36","CERTIFICATE");
		docTypeSelect.put("-219","CERTIFICATE OF LEVY");
		docTypeSelect.put("-203","CERTIFICATE OF PURCHASE");
		docTypeSelect.put("-189","CERTIFICATE OF TITLE");
		docTypeSelect.put("-74","CERTIFIED COPY");
		docTypeSelect.put("-176","CHANGE NAME ");
		docTypeSelect.put("-114","CHARTER");
		docTypeSelect.put("-223","CHILD SUPPORT LIEN ");
		docTypeSelect.put("-178","CONDEMNATION");
		docTypeSelect.put("-73","CONDOMINIUM DECLARATION");
		docTypeSelect.put("-78","CONSENT");
		docTypeSelect.put("-134","CONTINUING FINANCING STMT");
		docTypeSelect.put("-43","CONTRACT");
		docTypeSelect.put("-135","CONVEYANCE");
		docTypeSelect.put("189","CORR. TRANSFER DEATH INSTR");
		docTypeSelect.put("139","CORRECTED AMENDMENT");
		docTypeSelect.put("131","CORRECTED ASSIGNMENT");
		docTypeSelect.put("137","CORRECTED COURT");
		docTypeSelect.put("122","CORRECTED DEED");
		docTypeSelect.put("143","CORRECTED FINANCIAL STATEMENT");
		docTypeSelect.put("116","CORRECTED JUDGEMENT");
		docTypeSelect.put("141","CORRECTED LIEN");
		docTypeSelect.put("147","CORRECTED LIS PENDENS");
		docTypeSelect.put("163","CORRECTED LIS PENDENS FORECLOSURE");
		docTypeSelect.put("129","CORRECTED MORTGAGE");
		docTypeSelect.put("126","CORRECTED QUIT CLAIM DEED");
		docTypeSelect.put("135","CORRECTED RELEASE");
		docTypeSelect.put("172","CORRECTED SPECIAL WARRANTY DEED");
		docTypeSelect.put("133","CORRECTED SUBORDINATION");
		docTypeSelect.put("124","CORRECTED WARRANTY DEED");
		docTypeSelect.put("-49","CORRECTION");
		docTypeSelect.put("-195","COURT DOC");
		docTypeSelect.put("178","DECLARATION LAND PATENT");
		docTypeSelect.put("-12","DEED");
		docTypeSelect.put("-18","DEED IN TRUST");
		docTypeSelect.put("-85","DEED OF TRUST");
		docTypeSelect.put("-118","DESIGNATION");
		docTypeSelect.put("-136","DISCHARGE");
		docTypeSelect.put("-69","DISCLAIMER");
		docTypeSelect.put("-216","DISMISSAL NON PROPERTY");
		docTypeSelect.put("-7","DISSOLUTION");
		docTypeSelect.put("110","DOCUMENT OF DECLARATION");
		docTypeSelect.put("-174","DONATION");
		docTypeSelect.put("118","ENVIRON LAND USE CONTROL");
		docTypeSelect.put("-95","EXERCISE OPTION");
		docTypeSelect.put("-27","EXTENSION");
		docTypeSelect.put("-202","FEDERAL LIEN");
		docTypeSelect.put("-70","FINANCING STATEMENT");
		docTypeSelect.put("-200","FORFEITURE");
		docTypeSelect.put("104","FRAUDULENT DOC NOTICE");
		docTypeSelect.put("-143","GUARANTEE");
		docTypeSelect.put("-29","INCORPORATION");
		docTypeSelect.put("-68","JOINT VENTURE AGREEMENT");
		docTypeSelect.put("-120","JUDGMENT ");
		docTypeSelect.put("-57","LEASE");
		docTypeSelect.put("-103","LETTER");
		docTypeSelect.put("-111","LICENSE");
		docTypeSelect.put("-119","LIEN");
		docTypeSelect.put("-39","LIS PENDENS");
		docTypeSelect.put("151","LIS PENDENS FORECLOSURE");
		docTypeSelect.put("176","MARITIME LIEN");
		docTypeSelect.put("-132","MECHANICS LIEN");
		docTypeSelect.put("-65","MEMORANDUM");
		docTypeSelect.put("-158","MERGER  ");
		docTypeSelect.put("145","MILITARY");
		docTypeSelect.put("-126","MISCELLANEOUS");
		docTypeSelect.put("112","MISCELLANEOUS UCC");
		docTypeSelect.put("-227","MISSING DOCUMENT");
		docTypeSelect.put("-23","MODIFICATION");
		docTypeSelect.put("-128","MONUMENT");
		docTypeSelect.put("-13","MORTGAGE");
		docTypeSelect.put("-61","MORTGAGE AND ASSIGNMENT");
		docTypeSelect.put("-3","NON ATTACHMENT");
		docTypeSelect.put("192","NON CONDO DECLARATION");
		docTypeSelect.put("154","NOTARIAL RECORD");
		docTypeSelect.put("-88","NOTE");
		docTypeSelect.put("-117","NOTICE ");
		docTypeSelect.put("-210","NOTICE PROP TAX DEFERRAL LIEN");
		docTypeSelect.put("-142","OATH");
		docTypeSelect.put("159","OBITUARY");
		docTypeSelect.put("-99","OPTION");
		docTypeSelect.put("-32","ORDER");
		docTypeSelect.put("-191","ORDER OF DISMISSAL");
		docTypeSelect.put("-59","ORDINANCE");
		docTypeSelect.put("-125","PARTIAL ASSIGNMENT");
		docTypeSelect.put("-225","PARTIAL REL FED LIEN");
		docTypeSelect.put("-199","PARTIAL REL STATE LIEN");
		docTypeSelect.put("-9","PARTIAL RELEASE");
		docTypeSelect.put("-162","PARTIAL RELEASE TAX LIEN");
		docTypeSelect.put("-38","PARTNERSHIP AGREEMENT");
		docTypeSelect.put("-127","PERMIT");
		docTypeSelect.put("-89","PETITION");
		docTypeSelect.put("-62","PLAT");
		docTypeSelect.put("-151","PLEDGE");
		docTypeSelect.put("-51","POWER OF ATTORNEY");
		docTypeSelect.put("-16","QUIT CLAIM DEED");
		docTypeSelect.put("-109","RATIFICATION");
		docTypeSelect.put("-115","RECEIPT");
		docTypeSelect.put("-226","RECORD OF PAYMENT");
		docTypeSelect.put("-60","REDEMPTION");
		docTypeSelect.put("-26","REGISTERED AGENT");
		docTypeSelect.put("-37","REINSTATEMENT");
		docTypeSelect.put("-8","RELEASE");
		docTypeSelect.put("-224","RELEASE FED LIEN ");
		docTypeSelect.put("174","RELEASE PROBATE");
		docTypeSelect.put("-40","RELEASE STATE LIEN");
		docTypeSelect.put("-198","RELEASE TAX LIEN ");
		docTypeSelect.put("-113","REMOVAL");
		docTypeSelect.put("-104","RENEWAL");
		docTypeSelect.put("-34","REPORT");
		docTypeSelect.put("-148","REQUEST");
		docTypeSelect.put("-82","RESIGNATION");
		docTypeSelect.put("-141","RESIGNATION AND APPOINTMENT");
		docTypeSelect.put("-150","RESOLUTION ");
		docTypeSelect.put("-86","RESTRICTION");
		docTypeSelect.put("-215","REVOCATION ");
		docTypeSelect.put("-181","REVOCATION CTF REL FED TX LN");
		docTypeSelect.put("-185","REVOCATION OF CTF FED TAX LIEN");
		docTypeSelect.put("-129","RIGHT OF WAY DEED");
		docTypeSelect.put("-179","SALE OF SEIZED PROPERTY");
		docTypeSelect.put("-167","SEIZURE");
		docTypeSelect.put("102","SKIPPED DOCUMENT");
		docTypeSelect.put("170","SPECIAL WARRANTY DEED ");
		docTypeSelect.put("-190","STATE LIEN");
		docTypeSelect.put("-96","STIPULATION");
		docTypeSelect.put("-172","SUBORDINATE FEDERAL LIEN");
		docTypeSelect.put("-186","SUBORDINATE STATE LIEN");
		docTypeSelect.put("-54","SUBORDINATION");
		docTypeSelect.put("-81","SUBROGATION");
		docTypeSelect.put("-98","TAX LETTER");
		docTypeSelect.put("-66","TAX LIEN");
		docTypeSelect.put("-72","TERMINATION");
		docTypeSelect.put("-152","TRADE NAME");
		docTypeSelect.put("-107","TRANSFER");
		docTypeSelect.put("181","TRANSFER ON DEATH INSTRUMENT");
		docTypeSelect.put("-46","TRUST");
		docTypeSelect.put("-19","TRUST DEED");
		docTypeSelect.put("-47","TRUST INDENTURE");
		docTypeSelect.put("-17","TRUSTEES DEED");
		docTypeSelect.put("1","UNKNOWN");
		docTypeSelect.put("-63","WAIVER");
		docTypeSelect.put("-11","WARRANTY DEED");
		docTypeSelect.put("-35","WILL");
		docTypeSelect.put("-159","WITHDRAWAL");
		docTypeSelect.put("-140","WRIT");
	}
}

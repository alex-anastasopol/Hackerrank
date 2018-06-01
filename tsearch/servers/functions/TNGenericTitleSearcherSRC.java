package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 26, 2012
 */

public class TNGenericTitleSearcherSRC {

	public static final Pattern LEGAL_PLAT = Pattern.compile("\\bPLAT BK (\\d+) PG (\\d+)\\b");
	public static final Pattern LEGAL_BOOK = Pattern.compile("\\bBOOK (\\d+)");
	public static final Pattern LEGAL_PAGE = Pattern.compile("\\bPAGE (\\d+)");
	public static final Pattern LEGAL_LOT = Pattern.compile("(?is)\\b(LO?T?S?)\\s+(?:NO\\s+)?([\\d&,\\s-]+)\\b");
	public static final Pattern LEGAL_BLOCK = Pattern.compile("(?is)\\b(BL?O?C?K?S?)\\.?\\s+([\\d&,\\s-]+|[A-Z])\\b");
	public static final Pattern LEGAL_TRACT = Pattern.compile("\\bTRACT (\\d+)");
	public static final Pattern LEGAL_SECTION = Pattern.compile("\\b(?:SEC|SECTION) (\\d+)");
	public static final Pattern LEGAL_PHASE = Pattern.compile("\\b(?:PH|PHASE) (\\d+)");
	
	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo, ResultMap m) {

		TableColumn[] cols = row.getColumns();

		if (cols.length >= 7) {
			String instrBp = cols[1].toHtml().replaceAll("\\s+", " ").trim();
			String type = cols[2].toPlainTextString().replaceAll("\\s+", " ").trim();
			String legal = cols[3].toPlainTextString().replaceAll("\\s+", " ").trim();
			String grantor = cols[4].getChildrenHTML();
			String grantee = cols[5].getChildrenHTML();
			String refs = "";
			if (cols.length > 7){
				cols[6].toHtml().replaceAll("\\s+", " ").trim();
			}

			if (StringUtils.isNotEmpty(instrBp) && instrBp.contains("<BR>")) {
				String date = instrBp.split("(?is)<br>")[0];
				String instr = instrBp.split("(?is)<br>")[1];

				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), date.replaceAll("<[^>]*>", "").trim());
				if (StringUtils.isNotEmpty(instr)) {
					if (!instr.contains("-")) {
						m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr.replaceAll("<[^>]*>", "").trim());
					} else {
						String bp = instr.replaceAll("<[^>]*>", "").trim();
						String book = "", page = "";
						Matcher mat = Pattern.compile("(?is)(.*?)-(\\w+)$").matcher(bp);
						if (mat.find()){
							book = mat.group(1);
							page = mat.group(2);
						} else{
							book = bp.split("\\s*-\\s*")[0];
							page = bp.split("\\s*-\\s*")[1];
						}
						m.put(SaleDataSetKey.BOOK.getKeyName(), book.trim());
						m.put(SaleDataSetKey.PAGE.getKeyName(), page.trim());
					}
				}
			}

			if (StringUtils.isNotEmpty(type)) {
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type.trim());
			}

			if (StringUtils.isNotEmpty(legal)) {
				parseLegal(m, legal);
			}

			if (StringUtils.isNotEmpty(grantor)) {
				grantor = grantor.replaceAll("(?is)</?div[^>]*>", "");
				grantor = grantor.replaceAll("(?is)</?a[^>]*>", "");
				grantor = grantor.replaceAll("(?is)</?span[^>]*>", "");
				
				List<String> names = new ArrayList<String>();
				names.addAll(Arrays.asList(grantor.replace("&amp;", "&").split("\\s*<br>\\s*")));
				parseNames(m, names, "GrantorSet");
			}

			if (StringUtils.isNotEmpty(grantee)) {
				grantee = grantee.replaceAll("(?is)</?div[^>]*>", "");
				grantee = grantee.replaceAll("(?is)</?a[^>]*>", "");
				grantee = grantee.replaceAll("(?is)</?span[^>]*>", "");
				
				List<String> names = new ArrayList<String>();
				names.addAll(Arrays.asList(grantee.replace("&amp;", "&").split("\\s*<br>\\s*")));
				parseNames(m, names, "GranteeSet");
			}

			if (StringUtils.isNotEmpty(refs)) {
				refs = refs.replaceAll("(?ism)<br[^>]*>", "@@@").replaceAll("(?ism)<[^>]*>", "").replaceAll("\\s+", "");

				if (StringUtils.isNotEmpty(refs)) {
					String parts[] = refs.split("@@@");

					@SuppressWarnings("rawtypes")
					List<List> body = new ArrayList<List>();
					List<String> line = new ArrayList<String>();

					for (String s : parts) {
						line = new ArrayList<String>();

						String[] subparts = s.split("-");

						if (subparts.length == 3) {
							line.add(subparts[1].replaceAll("^0+$", ""));
							line.add(subparts[2].replaceAll("^0+$", ""));
							line.add(subparts[0]);
						} else if (subparts.length == 2) {
							line.add(subparts[0].replaceAll("^0+$", ""));
							line.add(subparts[1].replaceAll("^0+$", ""));
							line.add("");
						}

						body.add(line);
					}

					if (body.size() > 0) {
						String[] header = { "Book", "Page", "DocumentType" };
						ResultTable rt = GenericFunctions2.createResultTable(body, header);
						m.put("CrossRefSet", rt);
					}
				}
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
						.replaceAll("(?is)<!--.*?-->", "")
						.replaceAll("<[^>]*?>", "")
						.replaceAll("\\s+", " ")
						.trim();
				n = n.replaceAll("(?is)\\b(et)\\s+(ux|al|vir)\\b", "$1$2");
				n = n.replaceAll("(?is)\\bDEC(EASED)?\\b", "");
				
				//AIF = Attorney In Fact
				n = n.replaceAll("(?is)\\b\\(?BY AIF .*", "");
				n = n.replaceAll("(?is)\\b\\(?AIF FOR .*", "");
				
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
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes);

		Pattern LEGAL_PLAT = Pattern.compile("\\bPLAT B(?:OO)?K (\\d+) P(?:AGE|G) (\\d+)\\b");
//		Pattern LEGAL_BOOK = Pattern.compile("\\bBOOK (\\d+)");
//		Pattern LEGAL_PAGE = Pattern.compile("\\bPAGE (\\d+)");
		Pattern LEGAL_LOT = Pattern.compile("(?is)\\b(LO?T?S?)\\s+(?:NO\\s+)?([\\d&,\\s-]+)\\b");
		Pattern LEGAL_BLOCK = Pattern.compile("(?is)\\b(BL?O?C?K?S?)\\.?\\s+([\\d&,\\s-]+)\\b");
		Pattern LEGAL_TRACT = Pattern.compile("\\bTRACT (\\d+)");
		Pattern LEGAL_SECTION = Pattern.compile("\\b(?:SEC|SECTION) (\\d+)");
		Pattern LEGAL_PHASE = Pattern.compile("\\b(?:PH|PHASE) (\\d+)");

		// get lot
		Matcher matcher = LEGAL_LOT.matcher(legalDes);
		if (matcher.find()) {
			String lot = matcher.group(2);
			legalDes = legalDes.replaceFirst(matcher.group(0), "LOT ");
			lot = LegalDescription.cleanValues(lot.trim(), false, true);
			lot = lot.replaceAll("\\s*&\\s*", " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
		}
		
		// get block
		matcher = LEGAL_BLOCK.matcher(legalDes);
		if (matcher.find()) {
			String block = matcher.group(2);
			legalDes = legalDes.replaceFirst(matcher.group(0), "BLOCK ");
			block = LegalDescription.cleanValues(block.trim(), false, true);
			block = block.replaceAll("\\s*&\\s*", " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
		}

		// get section
		matcher = LEGAL_SECTION.matcher(legalDes);
		if (matcher.find()) {
			String section = matcher.group(1);
			legalDes = legalDes.replaceFirst(matcher.group(0), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section.trim());
		}
				
		// get tract
		matcher = LEGAL_TRACT.matcher(legalDes);
		if (matcher.find()) {
			String tract = matcher.group(1);
			legalDes = legalDes.replaceFirst(matcher.group(0), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract.trim());
		}
		
		// get phase
		matcher = LEGAL_PHASE.matcher(legalDes);
		if (matcher.find()) {
			String phase = matcher.group(1);
			legalDes = legalDes.replaceFirst(matcher.group(0), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase.trim());
		}

		//get plat book and page
		matcher = LEGAL_PLAT.matcher(legalDes);
		if (matcher.find()) {
			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), matcher.group(1).trim());
			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), matcher.group(2).trim());
			legalDes = legalDes.replace(matcher.group(), " ");
		}
		
		// get book
//		matcher = LEGAL_BOOK.matcher(legalDes);
//		if (matcher.find()) {
//			String book = matcher.group(1);
//			legalDes = legalDes.replaceFirst(matcher.group(0), " ");
//			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), book.trim());
//		}

		// get page
//		matcher = LEGAL_PAGE.matcher(legalDes);
//		if (matcher.find()) {
//			String book = matcher.group(1);
//			legalDes = legalDes.replaceFirst(matcher.group(0), " ");
//			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), book.trim());
//		}
		cleanSubdivisionName(m, legalDes);
	}

	protected static void cleanSubdivisionName(ResultMap m, String legalDes) {
		Pattern LEGAL_SUBD;
		Matcher matcher;
		String subdivision = StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName()));
		boolean subdivisWasChanged = false;
		if (StringUtils.isNotEmpty(legalDes) && StringUtils.isEmpty(subdivision)) {
			LEGAL_SUBD = Pattern.compile("(?is)\\b(BLOCK)\\s+(.*)");
			matcher = LEGAL_SUBD.matcher(legalDes);
			if (matcher.find()) {
				subdivision = matcher.group(2);
				subdivisWasChanged = true;
			} else {
				LEGAL_SUBD = Pattern.compile("(?is)\\b(LOT)\\s+(.*)");
				matcher = LEGAL_SUBD.matcher(legalDes);
				if (matcher.find()) {
					subdivision = matcher.group(2);
					subdivisWasChanged = true;
				} else {
					LEGAL_SUBD = Pattern.compile("(?is)\\b(ACRES)\\s+(.*)");
					matcher = LEGAL_SUBD.matcher(legalDes);
					if (matcher.find()) {
						subdivision = matcher.group(2);
						subdivisWasChanged = true;
					}
				}
			}
		}

		LEGAL_SUBD = Pattern.compile("(?i)\\s*\\b(N\\s*(?:W|E)?|S\\s*(?:W|E)?|W|E)\\b\\s*");
		matcher = LEGAL_SUBD.matcher(subdivision);
		if (matcher.find()) {// e.g. Sevier: for inst No. 12021282 -> S W MCCROSKEY FARM & CAMP SITES
			subdivision = subdivision.replaceFirst(Pattern.quote(matcher.group(1)), " ");
			subdivisWasChanged = true;
		}

		if (subdivisWasChanged) {
			subdivision = subdivision.replaceAll("\\s+", " ").trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		}
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap, long searchId) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml.replaceAll("(?ism)&nbsp;", " ")).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			NodeList auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("border", "3")).extractAllNodesThatMatch(
					new HasAttributeFilter("width", "95%"));

			if (auxNodes != null && auxNodes.size() > 0) {
				String recDate = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "File Date:"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").replaceAll("File Date:", "").trim();

				String docType = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "Doc Type:"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").replaceAll("Doc Type:", "").trim();

				String bookPage = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "Book#"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").replaceAll("Book#", "").trim();

//				String instr = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "File#"), "", true)
//						.replaceAll("<[^>]*>", "").replaceAll("File#", "").replaceAll("\\s+", " ").trim();
				String instr = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodes, "Detail Information for Instrument #"), "", false)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+"," ").replaceAll("(?sim).*Detail Information for Instrument #([^\\s]+) In Year.*", "$1").trim();

				String instrDate = HtmlParser3
						.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "Instrument Date:"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("Instrument Date:", "").replaceAll("\\s+", " ").trim();

				String mortgageAmount = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "Mort"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("Mort", "").replaceAll("\\s+", " ").replaceAll("[ $,-]", "");
				
				String transferAmount = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "Trans"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("Trans", "").replaceAll("\\s+", " ").replaceAll("[ $,-]", "");
				
				String lienAmount = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(auxNodes.elementAt(0).getChildren(), "Lien Amt"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("Lien Amt", "").replaceAll("\\s+", " ").replaceAll("[ $,-]", "");

				if (StringUtils.isNotEmpty(recDate)) {
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				}

				if (StringUtils.isNotEmpty(docType)) {
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
				}

				if (StringUtils.isNotEmpty(bookPage)) {
					String book = "";
					String page = "";

					if (bookPage.contains("Page#")) {
						book = bookPage.split("Page#")[0].trim();
						book = book.replaceFirst("(?is)\\bUCC\\b", "").trim();
						
						page = bookPage.split("Page#")[1].trim();
					}

					if (page.matches("0+")) {
						page = "";
					}

					if (StringUtils.isNotEmpty(book)) {
						resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book);
					}

					if (StringUtils.isNotEmpty(page)) {
						resultMap.put(SaleDataSetKey.PAGE.getKeyName(), page);
					}
				}

				if (StringUtils.isNotEmpty(instr) && !instr.matches("0+")) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
				}

				if (StringUtils.isNotEmpty(instrDate)) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate);
				}

				if (StringUtils.isNotEmpty(mortgageAmount) && !mortgageAmount.matches("0+\\.?0*")) {
					resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), mortgageAmount);
				}
				if (StringUtils.isNotEmpty(transferAmount) && !transferAmount.matches("0+\\.?0*")) {
					resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), transferAmount);
				}
				if (StringUtils.isNotEmpty(lienAmount) && !lienAmount.matches("0+\\.?0*")) {
					resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), lienAmount);
				}

			}

			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("border", "2")).extractAllNodesThatMatch(new HasAttributeFilter("width", "95%"));

			Node n = HtmlParser3.findNode(auxNodes, "Grantor(s)");
			
			if (n == null){//for UCC files
				n = HtmlParser3.findNode(auxNodes, "Debtor(s)");
			}

			if (n != null && n.getParent() instanceof TableColumn) {
				TableTag t = (TableTag) n.getParent().getParent().getParent();

				if (t.getRowCount() == 2) {
					TableRow r = t.getRow(1);

					TableColumn[] cols = r.getColumns();

					if (cols.length == 3) {
						
						TableColumn grantors = cols[0];
						List<String> names = new ArrayList<String>();
						names.addAll(Arrays.asList(grantors.toHtml().split("(?ism)<br[^>]*>")));
						parseNames(resultMap, names, "GrantorSet");

						TableColumn grantees = cols[1];
						names = new ArrayList<String>();
						names.addAll(Arrays.asList(grantees.toHtml().split("(?ism)<br[^>]*>")));
						parseNames(resultMap, names, "GranteeSet");

						GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId,true);
						GenericFunctions.checkTNCountyROForMERSForMortgage(resultMap, searchId);
						
						if (cols[2].getChildCount() > 1 && cols[2].getChild(1) instanceof TableTag) {
							TableTag crossrefs = (TableTag) cols[2].getChild(1);

							TableRow[] rows = crossrefs.getRows();

							@SuppressWarnings("rawtypes")
							List<List> body = new ArrayList<List>();
							List<String> line = new ArrayList<String>();

							for (int i = 1; i < rows.length; i++) {
								TableColumn[] ccols = rows[i].getColumns();
								if (ccols.length == 6) {
									String book = StringUtils.defaultString(ccols[4].toPlainTextString().trim());
									book = book.replaceFirst("(?is)\\bUCC\\b", "");
									String page = StringUtils.defaultString(ccols[5].toPlainTextString().trim());
									page = StringUtils.stripStart(page, "0");
									String instNo = ccols[1].toPlainTextString().trim();
									
									boolean alreadyContained = false;
									if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
										for (List list : body) {
											String alreadyBook = (String) list.get(4);
											String alreadyPage = (String) list.get(5);
											if (StringUtils.isNotEmpty(alreadyBook) && StringUtils.isNotEmpty(alreadyPage)){
												if (alreadyBook.equals(book) && alreadyPage.equals(page)){
													alreadyContained = true;
													break;
												}
											}
										}
									} else if (StringUtils.isNotEmpty(instNo)){
										for (List list : body) {
											String alreadyInst = (String) list.get(0);
											if (StringUtils.isNotEmpty(alreadyInst)){
												if (alreadyInst.equals(instNo)){
													alreadyContained = true;
													break;
												}
											}
										}
									}
									
									if (!alreadyContained){
										line = new ArrayList<String>();
										line.add(instNo);
										line.add(ccols[2].toPlainTextString().trim().replaceAll("^0+$", ""));
										line.add("");
										line.add("");
										line.add(book);
										line.add(page);
										body.add(line);
									}
								}
							}

							if (body.size() > 0) {
								String[] header = { "InstrumentNumber", "Year", "DocumentType", "DocSubtype", "Book", "Page" };
								ResultTable rt = GenericFunctions2.createResultTable(body, header);
								resultMap.put("CrossRefSet", rt);
							}
						}
					}
				}
			}

			// get legal
			n = getNodeByTypeAttributeDescription(auxNodes, "table", "border", "2", new String[] { "DISTRICT", "ACRES", "SECTION" }, true);
			
			if (n == null){
				n = getNodeByTypeAttributeDescription(auxNodes, "table", "border", "2", new String[] { "DISTRICT", "ACRES", "SUBDIVISION" }, true);
			}

			String lot = "", block = "";
			if (n != null && n instanceof TableTag && ((TableTag) n).getRowCount() > 1) {

				boolean blockIsMissing = true;
				TableTag tableTag = (TableTag) n;
				TableRow firstRow = tableTag.getRow(0);
				if (firstRow.toHtml().contains("BLOCK") || firstRow.toHtml().contains("BLK")){
					blockIsMissing = false;
				}
				 
				
				String district = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(n.getChildren(), "DISTRICT"), "", true);
				String subdivision = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(n.getChildren(), "SUBDIVISION"), "", true);
				block = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(n.getChildren(), "BLOCK"), "", true);
				String section = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(n.getChildren(), "SECTION"), "", true);

				if (StringUtils.isNotEmpty(district)) {
					if (district.matches("\\s*\\.\\s*")){
						district = district.replaceAll("\\s*\\.\\s*", "");
					}
					if (district.matches("\\s*0\\s*")){
						district = district.replaceAll("\\s*0\\s*", "");
					}
					district = district.replaceAll("(?is)\\b(ST|ND|RD|TH)\\b", "");
				}
				if (StringUtils.isNotEmpty(district)) {
					resultMap.put(PropertyIdentificationSetKey.DISTRICT.getKeyName(), district);
				}
				if (blockIsMissing){
					Matcher blMat = LEGAL_BLOCK.matcher(subdivision);
					if (blMat.find()){
						block = blMat.group(2);
						subdivision = subdivision.replaceAll(blMat.group(0), "");
					}
				}
				if (StringUtils.isNotEmpty(block)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
				}
				
				for (int i = 1; i < ((TableTag) n).getRowCount(); i++){
					String newLot = HtmlParser3.getValueFromAbsoluteCell(i, 0, HtmlParser3.findNode(n.getChildren(), "LOT"), "", true);
					if (StringUtils.isNotEmpty(newLot)) {
						newLot = newLot.replaceAll("(?is)\\bP/O\\b", "").replaceAll("(?is)#", "");
						lot += newLot + " ";
					}
					String newSubdivision = HtmlParser3.getValueFromAbsoluteCell(i, 0, HtmlParser3.findNode(n.getChildren(), "SUBDIVISION"), "", true);
					if (blockIsMissing){
						Matcher blMat = LEGAL_BLOCK.matcher(newSubdivision);
						if (blMat.find()){
							block = blMat.group(2);
							newSubdivision = newSubdivision.replaceAll(blMat.group(0), "");
						}
					}
					
					if (StringUtils.isNotEmpty(newSubdivision)){
						subdivision = newSubdivision;
					}
				}
				if (StringUtils.isNotEmpty(subdivision)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
					cleanSubdivisionName(resultMap, "");
				}
				if (StringUtils.isNotEmpty(lot.trim())) {
					if (lot.contains("TR")) {
						lot = lot.replaceAll("(?is)(TR)", " $1");
						lot = lot.replaceAll("\\bTR(ACTS?)?\\b", "");
						lot = LegalDescription.cleanValues(lot, false, true);
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), lot.trim());
					} else if (section.contains("TRACT")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), lot.trim());
					} else {
						lot = LegalDescription.cleanValues(lot, false, true);
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
					}
				}

				if (StringUtils.isNotEmpty(section)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
				}
			}

			n = getNodeByTypeAttributeDescription(auxNodes, "table", "border", "2", new String[] { "COMMENTS" }, true);

			if (n != null) {
				String legal = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(n.getChildren(), "COMMENTS"), "", false).trim();

				if (StringUtils.isNotEmpty(legal)) {
					parseLegal(resultMap, legal);
				}
			}
			n = getNodeByTypeAttributeDescription(auxNodes, "table", "border", "2", new String[] { "LEGAL DESCRIPTION" }, true);

			if (n != null && StringUtils.isEmpty(lot) && StringUtils.isEmpty(block)) {
				String legal = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(n.getChildren(), "LEGAL DESCRIPTION"), "", false).trim();

				if (StringUtils.isNotEmpty(legal)) {
					parseLegal(resultMap, legal);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String[] description,
			boolean recursive) {
		NodeList returnList = null;

		if (!StringUtils.isEmpty(attributeName))
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
					new HasAttributeFilter(attributeName, attributeValue), recursive);
		else
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

		for (int i = returnList.size() - 1; i >= 0; i--) {
			boolean flag = true;
			for (String s : description) {
				if (!StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s)) {
					flag = false;
					break;
				}
			}
			if (flag)
				return returnList.elementAt(i);
		}

		return null;
	}
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.AKGenericMotznikRO.IntermediaryResultTypes;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class AKGenericMotznikRO {

	public static ResultMap parseIntermediaryRow(TableRow row, IntermediaryResultTypes type, long searchId ) {

		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "R2");

		TableColumn[] cols = row.getColumns();
		
		for (int i=0; i<cols.length; i++) {
			
			try {
				TableColumn col = cols[i];
				switch(type) {
					case NAME_LEVEL_1:
						if(i==0) {
							String name = clean(col.toPlainTextString(),false,false);
							List<String> grantorList = new ArrayList<String>();
							List<String> granteeList = new ArrayList<String>();
							grantorList.add(name);
							ro.cst.tsearch.servers.functions.AKGenericRO.parseNames(resultMap, grantorList, granteeList, searchId);
						}
						break;
						
					case NAME_LEVEL_2:
						
						switch(i) {
							case 0:
								String doctype = clean(col.toPlainTextString(),false,false).replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
								resultMap.put("SaleDataSet.DocumentType",doctype);
								break;
							case 1:
								resultMap.put("SaleDataSet.RecordedDate",clean(col.toPlainTextString(),false,false));
								break;
							case 5:
								RegExUtils.extractInfoIntoMap(resultMap,clean(col.toPlainTextString(),false,false),"(\\d+)/(\\d+)",
										"SaleDataSet.Book",
										"SaleDataSet.Page");
								break;
							case 6:
								resultMap.put("SaleDataSet.InstrumentNumber",AKGenericRO.cleanInstrumentNumber(clean(col.toPlainTextString(),false,false)));
								break;
						}
						break;
					
					case DOCUMENT_NUMBER:
						switch(i) {
							case 0:
								resultMap.put("SaleDataSet.InstrumentNumber",AKGenericRO.cleanInstrumentNumber(clean(col.toPlainTextString(),false,false)));
								break;
							case 1:
								resultMap.put("SaleDataSet.Book",clean(col.toPlainTextString(),false,false));
								break;
							case 2:
								resultMap.put("SaleDataSet.Page",clean(col.toPlainTextString(),false,false));
								break;
							case 4:
								resultMap.put("SaleDataSet.RecordedDate",clean(col.toPlainTextString(),false,false));
								break;
							case 6:
								String doctype = clean(col.toPlainTextString(),false,false).replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
								resultMap.put("SaleDataSet.DocumentType",doctype);
								break;
						}
						break;
						
					case BOOK_PAGE:
						switch(i) {
							case 0:
								resultMap.put("SaleDataSet.Book",clean(col.toPlainTextString(),false,false));
								break;
							case 1:
								resultMap.put("SaleDataSet.Page",clean(col.toPlainTextString(),false,false));
								break;
							case 3:
								resultMap.put("SaleDataSet.InstrumentNumber",AKGenericRO.cleanInstrumentNumber(clean(col.toPlainTextString(),false,false)));
								break;
							case 4:
								resultMap.put("SaleDataSet.RecordedDate",clean(col.toPlainTextString(),false,false));
								break;
							case 6:
								String doctype = clean(col.toPlainTextString(),false,false).replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
								resultMap.put("SaleDataSet.DocumentType",doctype);
								break;
						}
						break;
					
					case DISTINCT_LEGALS:
						switch(i) {
						
							case 6:	
								String plat = clean(col.toPlainTextString(),false,false);
								if(StringUtils.isNotEmpty(plat)) {
									if(plat.matches("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)")) {
										resultMap.put("PropertyIdentificationSet.PlatBook", plat.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$1"));
										resultMap.put("PropertyIdentificationSet.PlatNo", plat.replaceAll("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)", "$2"));
									} else {
										resultMap.put("PropertyIdentificationSet.PlatInstr", plat);
									}
								}
								break;
								
							case 7:
								resultMap.put("PropertyIdentificationSet.SubdivisionTract", clean(col.toPlainTextString(),false,false));
								break;
								
							case 8:
								resultMap.put("PropertyIdentificationSet.SubdivisionBlock", clean(col.toPlainTextString(),false,false));
								break;
								
							case 9:
								resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", clean(col.toPlainTextString(),false,false));
								break;
								
							case 10:
								resultMap.put("PropertyIdentificationSet.SubdivisionUnit", clean(col.toPlainTextString(),false,false));
								break;
						}
					break;
					
					case SUBDIVISION_LEVEL_1:
					case SUBDIVISION_LEVEL_2:
					case NO_PLAT_SUBDIVISION_LEVEL_1:
					case LEGAL_DESCRIPTION:
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		return resultMap;
	}

	public static String parseAndFillResultMap(ServerResponse sr, String detailsHtml, ResultMap m,
			long searchId, ro.cst.tsearch.servers.types.AKGenericMotznikRO akGenericMotznikRO) {

		HtmlParser3 parser = new HtmlParser3(detailsHtml);
		String instrumentNo = AKGenericRO.cleanInstrumentNumber(getNodeAfterAsString(parser.getNodeList(),"DOCUMENT NUMBER (SERIAL NUMBER)",3));

		m.put("SaleDataSet.InstrumentNumber", instrumentNo);
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO2");

		String imageLink = null;
		
		try {
			List<String> grantorList = new ArrayList<String>();
			List<String> granteeList = new ArrayList<String>();
			List<TableRow> legalList = new ArrayList<TableRow>();
			List<LinkTag> referenceLinkList = new ArrayList<LinkTag>();
			List<String> referenceStringList = new ArrayList<String>();
			
			String serverDocType = getNodeAfterAsString(parser.getNodeList(),"DOCUMENT DESCRIPTION",3);
			serverDocType = serverDocType.replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
			
			TableTag bookPageTable = HtmlParser3.getFirstParentTag(HtmlParser3.findNode(parser.getNodeList(), "NUMBER OF PAGES"), TableTag.class);
			String book = getNodeAfterAsString(bookPageTable.getChildren(),"BOOK",3);
			String page = getNodeAfterAsString(bookPageTable.getChildren(),"PAGE",3);
				
			String recordedDate = getNodeAfterAsString(parser.getNodeList(),"DATE AND TIME",3);
			String amount = clean(getNodeAfterAsString(parser.getNodeList(),"AMOUNT",3),false,false);
			
			if(serverDocType.matches("^.*[\\$\\s]+([\\d,\\.]+)\\s*$")) {
				if("0.00".equals(amount) || "".equals(amount)) {
					amount = serverDocType.replaceAll("^.*[\\$\\s]+([\\d,\\.]+)\\s*$", "$1").replaceAll("\\s+", "").replaceAll("[\\$,\\s]*", "");	
				}
			}
			
			amount = clean(amount,true,false);
			
			
			TableTag grantorGranteeTable = HtmlParser3.getFirstParentTag(HtmlParser3.findNode(parser.getNodeList(), "GRANTORS"), TableTag.class);
			if(grantorGranteeTable!=null && grantorGranteeTable.getRowCount()>0) {
				TableColumn grantorColumn = grantorGranteeTable.getRow(0).getColumns()[0];
				TableColumn granteeColumn = grantorGranteeTable.getRow(0).getColumns()[1];
				
				NodeList grantors = HtmlParser3.getNodeListByTypeAndAttribute(grantorColumn.getChildren(), "span", "class", "data1", true);
				for(Node node : grantors.toNodeArray()) {
					grantorList.add(clean(node.toPlainTextString(),false,false));
				}
				
				NodeList grantees = HtmlParser3.getNodeListByTypeAndAttribute(granteeColumn.getChildren(), "span", "class", "data1", true);
				for(Node node : grantees.toNodeArray()) {
					granteeList.add(clean(node.toPlainTextString(),false,false));
				}
			}
			 
						
			m.put("SaleDataSet.RecordedDate", recordedDate);
			m.put("SaleDataSet.Book", book);
			m.put("SaleDataSet.Page", page);
			m.put("SaleDataSet.MortgageAmount", amount);
			m.put("SaleDataSet.ConsiderationAmount", amount);
			m.put("SaleDataSet.DocumentType", serverDocType);
			
			
			Node allRelatedDocumentsText = HtmlParser3.findNode(parser.getNodeList(), "ALL RELATED DOCUMENTS").getParent();
			Node otherIdText = HtmlParser3.findNode(parser.getNodeList(), "OTHER ID").getParent();
			Node viewImageText = HtmlParser3.findNode(parser.getNodeList(), "VIEW IMAGE");
			//Node subdivisionText = HtmlParser3.findNode(parser.getNodeList(), "SUBDIVISION");
			
			if(viewImageText!=null) {
				Node imageLinkTag = HtmlParser3.getFirstTag(getNodeAfter(parser.getNodeList(),"VIEW IMAGE",3).getChildren(),LinkTag.class,true);
				imageLink = ((LinkTag) imageLinkTag).extractLink();
			}
			
			TableColumn mainTableColumn = HtmlParser3.getFirstParentTag(allRelatedDocumentsText, TableColumn.class);
			
			NodeList otherIds =  HtmlParser3.getNodesBetween(mainTableColumn.getChildren(), otherIdText, allRelatedDocumentsText);
			otherIds = HtmlParser3.getNodeListByTypeAndAttribute(otherIds, "span", "class", "data1", true);
			
			NodeList allRelatedDocuments;
			if(viewImageText == null) {
				allRelatedDocuments = HtmlParser3.getNodesBetween(mainTableColumn.getChildren(), allRelatedDocumentsText, mainTableColumn.getEndTag());	
			}else {
				allRelatedDocuments = HtmlParser3.getNodesBetween(mainTableColumn.getChildren(), allRelatedDocumentsText, viewImageText.getParent());
			}
			 
			allRelatedDocuments = HtmlParser3.getNodeListByTypeAndAttribute(allRelatedDocuments, "span", "class", "data1", true);
			
			for(Node node : otherIds.toNodeArray()) {
				referenceStringList.add(node.toPlainTextString());
			}
			
			for(Node node : allRelatedDocuments.toNodeArray()) {
				LinkTag link = HtmlParser3.getFirstTag(node.getChildren(), LinkTag.class, true);
				if (link != null){
					referenceLinkList.add(link);
				}
			}
					
			Node townshipTextNode = HtmlParser3.findNode(parser.getNodeList(), "TNSHP");
			if(townshipTextNode!=null) {
				TableTag legalTable = HtmlParser3.getFirstParentTag(townshipTextNode, TableTag.class);
				if(legalTable !=null && legalTable.getRowCount()>2) {
					for(int i=2; i<legalTable.getRowCount(); i++) {
						legalList.add(legalTable.getRow(i));
					}
				}
			}
			
			AKGenericRO.parseNames(m, grantorList, granteeList, searchId);
			parseLegals(m, legalList);
			parseReferences(sr, m, referenceLinkList, referenceStringList, akGenericMotznikRO);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return imageLink;
	}
	
	@SuppressWarnings("rawtypes")
	private static void parseReferences(ServerResponse response, ResultMap resultMap,
			List<LinkTag> referenceLinkList, List<String> referenceStringList, ro.cst.tsearch.servers.types.AKGenericMotznikRO akGenericMotznikRO) {
		if( (referenceLinkList == null || referenceLinkList.isEmpty()) && (referenceStringList == null || referenceStringList.isEmpty()) ) {
			return;
		}
		String[] header = { "InstrumentNumber" ,"Book", "Page" };
		List<List>body = new ArrayList<List>();
		HashSet<String> alreadyProcessed = new HashSet<String>();
		if(referenceLinkList != null) {
			for (LinkTag reference : referenceLinkList) {
				try {
					String toProcess = AKGenericRO.cleanInstrumentNumber(reference.toPlainTextString());
					if(!alreadyProcessed.contains(toProcess)) {
						alreadyProcessed.add(toProcess);
						List<String> line = new ArrayList<String>();
						line.add(toProcess);
						line.add("");
						line.add("");
						body.add(line);
						addCrossreferences(response, akGenericMotznikRO, reference);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if(referenceStringList != null ) {
			for (String reference : referenceStringList) {
				if(reference.contains(" ")) {
					try {/*
						String book = reference.substring(0, reference.indexOf(" "));
						String page = reference.substring(reference.lastIndexOf(" ") + 1);
						
						SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
						TSServerInfoModule module = new TSServerInfoModule(
								akGenericRO.getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX)); 
						
						module.setData(0, ro.cst.tsearch.servers.types.AKGenericRO.DISTRICT_MAP.get(akGenericRO.getServerID()));
						module.setData(1, book);
						module.setData(2, page);
						module.setVisible(false);
						module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);			
						
						ServerResponse serverResponse = akGenericRO.SearchBy(module, searchDataWrapper);
						
						String details = serverResponse.getResult();
						
						if(details != null) {
							StringBuilder outputTable = new StringBuilder();
							Collection<ParsedResponse> smartParsedResponses =
								smartParseDocumentIntermediary(serverResponse, details, outputTable , akGenericRO);
							if(!smartParsedResponses.isEmpty()) {
								ParsedResponse parsedResponse = smartParsedResponses.iterator().next();
								DocumentI documentI = parsedResponse.getDocument();
								if(documentI != null) {
									if(book.equals(documentI.getBook()) && page.equals(documentI.getPage())) {
										List<String> line = new ArrayList<String>();
										line.add(documentI.getInstno());
										line.add(documentI.getBook());
										line.add(documentI.getPage());
										body.add(line);
										
										String linkString = parsedResponse.getPageLink().getLink() + "&isSubResult=true";
										LinkInPage pl = new LinkInPage(linkString,linkString,TSServer.REQUEST_SAVE_TO_TSD);
										parsedResponse.setPageLink(pl);
										
										response.getParsedResponse().addOneResultRowOnly(parsedResponse);
									}
								}
							}
							
						}*/
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		if(!body.isEmpty()) {
			resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
		}
		
	}

	private static void addCrossreferences(ServerResponse response,
			ro.cst.tsearch.servers.types.AKGenericMotznikRO akGenericMotznikRO,
			LinkTag referenceTag) {
		String originalLink = null;
		try {
			
			ParsedResponse prChild = new ParsedResponse();
			LinkInPage pl = new LinkInPage(originalLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD);
			prChild.setPageLink(pl);
			response.getParsedResponse().addOneResultRowOnly(prChild);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void parseLegals(ResultMap resultMap,
			List<TableRow> legalList) {
		Vector<PropertyIdentificationSet> pisVector = new Vector<PropertyIdentificationSet>();
		
		boolean setBookAndPageForPlat = false;
		for (TableRow rowLegal : legalList) {
			PropertyIdentificationSet pis = parseLegal(rowLegal);
			if(pis != null && !pis.isEmpty()) {
				pisVector.add(pis);
				
				String doctype = (String) resultMap.get("SaleDataSet.DocumentType");
				if (StringUtils.isNotEmpty(doctype)){
					if ("PLAT".equals(doctype.trim()) && !setBookAndPageForPlat){
						if (StringUtils.isNotEmpty(pis.getAtribute("PlatBook"))){
							resultMap.put("SaleDataSet.Book", pis.getAtribute("PlatBook"));
							if (StringUtils.isNotEmpty(pis.getAtribute("PlatNo"))){
								resultMap.put("SaleDataSet.Page", pis.getAtribute("PlatNo"));
							}
							setBookAndPageForPlat = true;
						}
					}
				}
			}
		}
		if(!pisVector.isEmpty()) {
			resultMap.put("PropertyIdentificationSet", pisVector);
		}
	}
	

	private static PropertyIdentificationSet parseLegal(TableRow rowLegal) {
		PropertyIdentificationSet pis = new PropertyIdentificationSet();
		
		TableColumn[] cols = rowLegal.getColumns();
		
		pis.setAtribute("SubdivisionTownship", clean(cols[1].toPlainTextString(),false,false));
		pis.setAtribute("SubdivisionRange", clean(cols[2].toPlainTextString(),false,false));
		pis.setAtribute("SubdivisionSection", clean(cols[3].toPlainTextString(),false,false));
		pis.setAtribute("SubdivisionName", clean(cols[5].toPlainTextString(),false,false));
		
		String plat = clean(cols[6].toPlainTextString(),false,false);
		if(StringUtils.isNotEmpty(plat)) {
			Pattern platPattern = Pattern.compile("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)(?:-([a-zA-Z\\d]+))?");
			Matcher platMatcher = platPattern.matcher(plat);
			if(platMatcher.find()) {
				pis.setAtribute("PlatBook", platMatcher.group(1));
				if(platMatcher.group(3) != null) { 
					pis.setAtribute("PlatNo", platMatcher.group(2) + platMatcher.group(3));
				} else {
					pis.setAtribute("PlatNo", platMatcher.group(2));
				}
			} else {
				pis.setAtribute("PlatInstr", plat);
			}
		}
		
		pis.setAtribute("SubdivisionTract", clean(cols[7].toPlainTextString(),false,false));
		pis.setAtribute("SubdivisionBlock", clean(cols[8].toPlainTextString(),false,false));
		pis.setAtribute("SubdivisionLotNumber", Roman.normalizeRomanNumbers(clean(cols[9].toPlainTextString(),false,false)));
		
		String unit = clean(cols[10].toPlainTextString(),false,false);
		if(StringUtils.isNotEmpty(unit)) {
			pis.setAtribute("SubdivisionUnit", unit);
			pis.setAtribute("SubdivisionCond", "true");	 
		}
		
		return pis;
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
		
	public static String getNodeAfterAsString(NodeList nl, String text, int offset) {
		Node nodeAfter =  getNodeAfter(nl,text,offset);
		if(nodeAfter==null) {
			return "";
		}else {
			return nodeAfter.toPlainTextString();
		}
	}
	
	public static Node getNodeAfter(NodeList nl, String text, int offset) {
		
		Node nodeText = HtmlParser3.findNode(nl,text).getParent();
		
		Node[] nodeArray = nodeText.getParent().getChildren().toNodeArray();
		for(int i=0;i<nodeArray.length;i++) {
			Node eachNode = nodeArray[i];
			if(eachNode.equals(nodeText)) {
				if(nodeArray.length > i+offset) {
					if(nodeArray[i+offset]==null) {
						return null;
					}else {
						return nodeArray[i+offset];
					}
				}else {
					return null;
				}
			}
		}
		
		return null;
	}

}

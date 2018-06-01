package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.RegisterDocumentI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.types.GenericOncoreServerRO;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class GenericOncoreFunctionsRO {
	
	protected static final Category logger = Logger.getLogger(GenericOncoreFunctionsRO.class);
	
	public static Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String responseHtml, StringBuilder outputTable, GenericOncoreServerRO genericOncoreServerRO) {
		
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
		if(responseHtml == null || response == null) {
			return responses;
		}
		long searchId = -1;
		try {
			HtmlParser3 parser = new HtmlParser3(responseHtml);
			
			Node mainTableNode = parser.getNodeById("dgResults");
			if(mainTableNode == null) {
				return responses;
			}
			
			Search search = genericOncoreServerRO.getSearch();
			searchId = search.getID();
			
			/**
			 * We need to find what was the original search module
			 * in case we need some info from it like in the new PS interface
			 */
			TSServerInfoModule moduleSource = null;
			Object objectModuleSource = response.getParsedResponse().getAttribute(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			if(objectModuleSource != null) {
				if(objectModuleSource instanceof TSServerInfoModule) {
					moduleSource = (TSServerInfoModule) objectModuleSource;
				} 
			} else {
				objectModuleSource = search.getAdditionalInfo(
						genericOncoreServerRO.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
				if (objectModuleSource instanceof TSServerInfoModule) {
					moduleSource = (TSServerInfoModule) objectModuleSource;
				}
			}

			
			String linkPrefix = genericOncoreServerRO.CreatePartialLink(TSConnectionURL.idGET);
			int numberOfUncheckedElements = 0;
			
			TableTag mainTable = (TableTag) mainTableNode;
			TableRow[] mainRows = mainTable.getRows();
			
			int indexDocType = -1;
			int indexBook = -1;
			int indexPage = -1;
			int indexInstrument = -1;
			int indexFullName = -1;
			
			
			
			for (TableRow tableRow : mainRows) {
				String classAttribute = tableRow.getAttribute("class");
				if(classAttribute == null) {
					TableColumn[] columns = tableRow.getColumns();
					if(columns.length == 9  ||				//name search 
							columns.length == 11 || 		//instrument number
							columns.length == 13 ) {		//legal search
						
						
						NodeList linkList = tableRow.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						
						ResultMap resultMap = new ResultMap();
						ParsedResponse currentResponse = new ParsedResponse();
						StringBuilder innerRow = new StringBuilder();
						LinkTag linkTag = (LinkTag) linkList.elementAt(0);
						String tempLink = linkTag.extractLink().trim().replaceAll("\\s", "%20");
						if(tempLink.startsWith("showdetails")) {
							tempLink = tempLink.substring(4);
						}
						String documentNumber = columns[indexInstrument].toPlainTextString().trim();	//Default to Name Result
						String linkString = linkPrefix + "/oncoreweb/" + tempLink + "&dummy=" + documentNumber;
						linkTag.setLink(linkString);
						
						for (int indexLink = 1; indexLink < linkList.size(); indexLink++) {
							LinkTag linkTagTemp = (LinkTag) linkList.elementAt(indexLink);
							linkTagTemp.setLink(linkString);
						}
						
						LinkInPage linkInPage = new LinkInPage(
								linkString, 
								linkString, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						
						String serverDocType = columns[indexDocType].toPlainTextString().trim();
						
						String book = "";	//Default to Name Result
						String page = "";	//Default to Name Result
						
						if(indexBook != -1) {
							book = columns[indexBook].toPlainTextString().trim();
						}
						if(indexPage != -1) {
							page = columns[indexPage].toPlainTextString().trim();
						}
						
						if(columns.length == 13) {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), 
									columns[9].toPlainTextString().trim());
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), 
									columns[10].toPlainTextString().trim());
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), 
									columns[11].toPlainTextString().trim());
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), 
									columns[12].toPlainTextString().trim());
							
						}
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
	    				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
	    				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), columns[5].toPlainTextString().trim());
						resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
						resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book);
						resultMap.put(SaleDataSetKey.PAGE.getKeyName(), page);
						
						if(indexFullName > 0) {
							String fullName = columns[indexFullName].toPlainTextString().trim();
							parseNames(
									resultMap, 
									searchId,
									new String[]{fullName},
									new String[0]);
						}
						
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type", serverDocType);
						data.put("book", book);
						data.put("page", page);

						
						String checkbox = null;
						if (genericOncoreServerRO.isInstrumentSavedInIntermediary(documentNumber, data) ) {
			    			checkbox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
	            			currentResponse.setPageLink(linkInPage);
	            			
	            			/**
	            			 * Save module in key in additional info. The key is instrument number that should be always available. 
	            			 */
	            			String keyForSavingModules = genericOncoreServerRO.getKeyForSavingInIntermediary(documentNumber);
	            			search.setAdditionalInfo(keyForSavingModules, 
	            					moduleSource);
			    		}
						columns[0].setText("<td>" + checkbox + "</td>");
						
						
						currentResponse.setParentSite(response.isParentSiteSearch());
						
						
						tableRow.getChildren().elementAt(tableRow.getChildren().indexOf(columns[0])).setText("<td>" + checkbox + "</td>");
						
						if(columns[1].toPlainTextString().trim().isEmpty()) {
							innerRow.append("<td>&nbsp;</td>");
						} else {
							innerRow.append("<td>" + columns[1].toPlainTextString() + "</td>");
						}
						
						for (int i = 2; i < columns.length; i++) {
							innerRow.append(columns[i].toHtml());
						}
						
						Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
	    				RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
	    				currentResponse.setDocument(document);
						
						currentResponse.setOnlyResponse("<tr><td align=\"center\">" + checkbox + "</td>" + innerRow + "</tr>" );
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr>" + innerRow + "</tr>");
						
						responses.add(currentResponse);
					}
				} else if("stdFontPager".equals(classAttribute) && StringUtils.isEmpty(response.getParsedResponse().getFooter())) {
					tableRow.setAttribute("align", "center");
					
					Map<String,String> params = HttpSite.fillConnectionParams(responseHtml, new String[] {"__VIEWSTATE"},"Form1");;
					int seq = GenericOncoreServerRO.getSeq();
					genericOncoreServerRO.getSearch().setAdditionalInfo(genericOncoreServerRO.getCurrentServerName() + ":params:" + seq, params);
					
					NodeList crossreferencesList = tableRow.getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("a"), true);
					int crtPage = -1;
					if(crossreferencesList.size() > 0) {
						NodeList spanList = tableRow.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
						if(spanList.size() > 0) {
							crtPage = Integer.parseInt(spanList.elementAt(0).toPlainTextString().trim());
						}
					}
					LinkTag crossLink = null;
					boolean foundNextPage = false;
					for (int i = 0; i < crossreferencesList.size() && crtPage >= 0; i++) {
						crossLink = (LinkTag) crossreferencesList.elementAt(i);
						crossLink.setLink(crossLink.getLink().replaceAll("(?i)__doPostBack\\('([^']+)','([^']*)'\\)", 
								genericOncoreServerRO.CreatePartialLink(TSConnectionURL.idPOST) +"/oncoreweb/search.aspx?q=detail&__EVENTTARGET=$1&__EVENTARGUMENT=$2&seq=" + seq ).replace("$", ":"));
						
						String keyForSavingModules = genericOncoreServerRO.getKeyForSavingInIntermediaryNextLink(
								crossLink.getLink().substring((crossLink.getLink().indexOf("__EVENTTARGET="))));
            			search.setAdditionalInfo(keyForSavingModules, moduleSource);
						
						if(!foundNextPage) {
							try {
								String crossLinkText = crossLink.toPlainTextString().trim();
								if(Integer.parseInt(crossLinkText) == (crtPage + 1)) {
									response.getParsedResponse().setNextLink("<a href=" + crossLink.extractLink()+ ">Next</a>");
									foundNextPage = true;
								}
								
							} catch (Exception e) {
							}
						}							
					}
					if(!foundNextPage && crossLink!= null) {
						String crossLinkText = crossLink.toPlainTextString().trim();
						if("...".equals(crossLinkText)) {
							response.getParsedResponse().setNextLink("<a href=" + crossLink.extractLink()+ ">Next</a>");
						}
						
					}
					
					response.getParsedResponse().setFooter(tableRow.toHtml());
					response.getParsedResponse().setHeader(tableRow.toHtml());
				} else if("navtop".equals(classAttribute)) {
					
					TableColumn[] columns = tableRow.getColumns();
					StringBuilder innerRow = new StringBuilder("<tr style=\"color:White;background-color:#324396;border-style:None;font-weight:bold;\"><td align=\"center\">" + TSServer.SELECT_ALL_CHECKBOXES + "</td>");
					for (int i = 1; i < columns.length; i++) {
						String name = columns[i].toPlainTextString().trim();
						innerRow.append("<td>").append(name).append("</td>");
						if("Doc Type".equals(name) || "DocTypeKey".equals(name)) {
							indexDocType = i;
						} else if("Book".equals(name)) {
							indexBook = i;
						} else if("Page".equals(name)) {
							indexPage = i;
						} else if("Instrument".equals(name)) {
							indexInstrument = i;
						} else if("FullName".equals(name)) {
							indexFullName = i;
						}
					}
					innerRow.append("</tr>");
					
					response.getParsedResponse().setHeader(response.getParsedResponse().getHeader() + innerRow);
				}
			}
			
			
			
			
		} catch (Exception e) {
			logger.error("SearchId: " + searchId + ": Error while parsing intermediary response", e);
		}
		return responses;
	}
	
	public static String parseAndFillResultsMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap, Search search) {
		String link = null;
		try {
			if(detailsHtml != null) {
				HtmlParser3 parser = new HtmlParser3(detailsHtml);
				
				resultMap.put(
						OtherInformationSetKey.SRC_TYPE.getKeyName(),
						"RO");
				
				resultMap.put(
						SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), 
						parser.getPlainTextFromNodeById("lblCfn", ""));
				
				String[] lblBookPage = parser.getPlainTextFromNodeById("lblBookPage", "").split("/");
				if(lblBookPage.length == 2) {
					if(!"0".equals(lblBookPage[0].trim())) {
						resultMap.put(SaleDataSetKey.BOOK.getKeyName(),lblBookPage[0].trim());
					}
					if(!"0".equals(lblBookPage[1].trim())) {
						resultMap.put(SaleDataSetKey.PAGE.getKeyName(),lblBookPage[1].trim());
					}
				}
				resultMap.put(
						SaleDataSetKey.BOOK_TYPE.getKeyName(), 
						parser.getPlainTextFromNodeById("lblBookType", ""));
				
				resultMap.put(
						SaleDataSetKey.RECORDED_DATE.getKeyName(), 
						parser.getPlainTextFromNodeById("lblRecordDate", "").replaceAll("([\\d/]+)\\s+.*", "$1"));
				String serverDocType = parser.getPlainTextFromNodeById("lblDocumentType", "").replaceAll("[^\\(]*\\(([^\\)]+)\\).*", "$1");
				resultMap.put(
						SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), 
						serverDocType);
				
				
				String numberOfPagesAsString = parser.getPlainTextFromNodeById("lblNumberPages", "");
				if(StringUtils.isNotEmpty(numberOfPagesAsString)) {
					try {
						int numberOfPages = Integer.parseInt(numberOfPagesAsString);
						if(numberOfPages > 0) {
							String serverHiddenId = ((InputTag)parser.getNodeById("server_hidden_id")).getAttribute("value");
							link = "/oncoreweb/ImageBrowser/default.aspx?id=" + serverHiddenId + 
								"&dtk=" + resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()) +
								"&fakeName=" + serverHiddenId + ".pdf";			
						}
					} catch (Exception e) {
					}
				}
				
				
				
				resultMap.put(
						SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), 
						parser.getPlainTextFromNodeById("lblConsideration", "").replaceAll("[$,]", ""));
				
				
				NodeList tempNodeList = parser.getNodeById("lblDirectName").getChildren();
				String[] grantorArray = null;
				if(tempNodeList != null) {
					grantorArray = tempNodeList.toHtml().split("<br>");
				} else {
					grantorArray = new String[0];
				}
				tempNodeList = parser.getNodeById("lblReverseName").getChildren();
				String[] granteeArray = null;
				if(tempNodeList != null) {
					granteeArray = tempNodeList.toHtml().split("<br>");
				} else {
					granteeArray = new String[0];
				}
				
				parseNames(
						resultMap, 
						search.getID(),
						grantorArray,
						granteeArray);
				List<Node> allCrossrefNodesToFollow = new ArrayList<Node>();
				List<Node> allCrossrefNodesToFindOnly = new ArrayList<Node>();
				
				int searchType = search.getSearchType();
				
				
				if(searchType == Search.PARENT_SITE_SEARCH) {
					allCrossrefNodesToFollow.add(parser.getNodeById("pnlRelatedDocs"));
					allCrossrefNodesToFollow.add(parser.getNodeById("pnlPrevDocs"));
					allCrossrefNodesToFollow.add(parser.getNodeById("pnlFutureDocs"));
					allCrossrefNodesToFollow.add(parser.getNodeById("lblUnresolvedLinks"));	
				} else {
					if("MEL".equals(serverDocType)) {
						allCrossrefNodesToFollow.add(parser.getNodeById("pnlFutureDocs"));
						allCrossrefNodesToFollow.add(parser.getNodeById("pnlPrevDocs"));
						
						allCrossrefNodesToFindOnly.add(parser.getNodeById("pnlRelatedDocs"));
						allCrossrefNodesToFindOnly.add(parser.getNodeById("lblUnresolvedLinks"));	
					} else if("RML".equals(serverDocType) || "SML".equals(serverDocType)) {
						//no reference to RML
						allCrossrefNodesToFindOnly.add(parser.getNodeById("pnlFutureDocs"));
						allCrossrefNodesToFindOnly.add(parser.getNodeById("pnlPrevDocs"));
						allCrossrefNodesToFindOnly.add(parser.getNodeById("pnlRelatedDocs"));
						allCrossrefNodesToFindOnly.add(parser.getNodeById("lblUnresolvedLinks"));
					} else {
						allCrossrefNodesToFollow.add(parser.getNodeById("pnlRelatedDocs"));
						allCrossrefNodesToFollow.add(parser.getNodeById("pnlPrevDocs"));
						allCrossrefNodesToFollow.add(parser.getNodeById("pnlFutureDocs"));
						allCrossrefNodesToFollow.add(parser.getNodeById("lblUnresolvedLinks"));	
					}
				}
				
				
				
				
				parseCrossreferences(
						response,
						resultMap, 
						allCrossrefNodesToFollow, true);
				
				parseCrossreferences(
						response,
						resultMap, 
						allCrossrefNodesToFindOnly, false);
				
				tempNodeList = parser.getNodeById("lblLegal").getChildren();
				if(tempNodeList != null) {
					parseLegals(
							resultMap, 
							tempNodeList.toHtml().split("<br>"));
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while parsing document", e);
		}
		return link;
	}
	
	private static void parseLegals(ResultMap resultMap, String[] legalRows) {
		Vector<PropertyIdentificationSet> pisVector = new Vector<PropertyIdentificationSet>();
		for (String rawLegal : legalRows) {
			PropertyIdentificationSet pis = parseLegal(rawLegal);
			if(pis != null && !pis.isEmpty()) {
				pisVector.add(pis);
			}
		}
		if(!pisVector.isEmpty()) {
			resultMap.put("PropertyIdentificationSet", pisVector);
		}
		
	}

	private static PropertyIdentificationSet parseLegal(String rawLegal) {
		PropertyIdentificationSet pis = new PropertyIdentificationSet();
		String lot = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bLT\\s+([-\\w]+)");
		if(StringUtils.isNotEmpty(lot)) {
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), Roman.normalizeRomanNumbers(lot));
			rawLegal = rawLegal.replaceAll("\\bLT\\s+([-\\w]+)", "");
		}
		String tract = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bTRCT\\s+([-\\w]+)");
		if(StringUtils.isNotEmpty(tract)) {
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
			rawLegal = rawLegal.replaceAll("\\bTRCT\\s+([-\\w]+)", "");
		}
		
		String phase = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bPH\\s+([-\\w]+)");
		if(StringUtils.isNotEmpty(phase)) {
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), Roman.normalizeRomanNumbers(phase));
			rawLegal = rawLegal.replaceAll("\\bPH\\s+([-\\w]+)", "");
		}
		String section = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bSE?C\\s+(\\w+)");
		if(StringUtils.isNotEmpty(section)) {
			if(section.matches("[A-Z]+")) {
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), Roman.normalizeRomanNumbers(section));
			} else {
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), section);
			}
			rawLegal = rawLegal.replaceAll("\\bSE?C\\s+(\\w+)", "");
		}
		String township = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bTS\\s+(\\w+)");
		if(StringUtils.isNotEmpty(township)) {
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), township);
			rawLegal = rawLegal.replaceAll("\\bTS\\s+(\\w+)", "");
		}
		String range = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bRG\\s+(\\w+)");
		if(StringUtils.isNotEmpty(range)) {
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), range);
			rawLegal = rawLegal.replaceAll("\\bRG\\s+(\\w+)", "");
		}
		
		String block = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bBLK\\s+\\s*([-\\w]+)");
		if(StringUtils.isNotEmpty(block)) {
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
			rawLegal = rawLegal.replaceAll("\\bBLK\\s+\\s*([-\\w]+)", "");
		}
		
		String quarter = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "\\bQTR\\s+([-\\w]+)");
		if(StringUtils.isNotEmpty(quarter)) {
			pis.setAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName(), quarter);
			rawLegal = rawLegal.replaceAll("\\bQTR\\s+([-\\w]+)", "");
		}
		
		pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), rawLegal.trim());
		
		return pis;
	}

	@SuppressWarnings("rawtypes")
	private static void parseNames(ResultMap m, long searchId, String[] grantorList, String[] granteeList) throws Exception {
		
		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		
		String grantorAsString = parseNameList(grantorList, grantor, "");
		String granteeAsString = parseNameList(granteeList, grantee, "");
		
		if(StringUtils.isNotEmpty(granteeAsString)) {
			m.put("SaleDataSet.Grantee", granteeAsString.substring(0,granteeAsString.length() - 1));
		}
		if(StringUtils.isNotEmpty(grantorAsString)) {
			m.put("SaleDataSet.Grantor", grantorAsString.substring(0,grantorAsString.length() - 1));
		} 
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId, true);
		
	}

	@SuppressWarnings("rawtypes")
	protected static String parseNameList(String[] grantorList,
			ArrayList<List> grantor, String grantorAsString) {
		String[] suffixes;
		String[] type;
		String[] otherType;
		for (String grantorString : grantorList) {
			grantorString = cleanName(grantorString);
			if(StringUtils.isNotEmpty(grantorString)) {
				String names[] = null;
				if(NameUtils.isCompany(grantorString)) {
					names = new String[]{"", "", grantorString, "", "", ""};
				} else {
					names = StringFormats.parseNameNashville(grantorString, true);
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				grantorAsString += grantorString + "/";
				
				GenericFunctions.addOwnerNames(grantorString, names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantor);
			}
		}
		return grantorAsString;
	}

	private static String cleanName(String grantorString) {
		if(grantorString != null) {
			grantorString = grantorString.trim().replaceAll("\\([^\\)]*\\)$", "");
			grantorString = grantorString.replaceAll("\\((?:IND)?\\s*EXEC\\)+", " ");
			grantorString = grantorString.replaceAll("\\(POA\\)+", " ");
			grantorString = grantorString.replaceAll("\\(ADMIN\\)+", " ");
			grantorString = grantorString.replaceAll("\\(GUARDIAN\\)+", " ");
			grantorString = grantorString.replaceAll("\\(REP(?:RESENTATIVE)?\\)+", " ");
			grantorString = grantorString.replaceAll("\\(ATTY\\)+", " ");
			grantorString = grantorString.replaceAll("\\(ALM\\)+", " ");
			grantorString = grantorString.replaceAll("\\(ESTATE\\),", " ESTATE, ");	
			grantorString = grantorString.replaceAll("\\s+\\([^\\)]*\\),", ",");
			grantorString = grantorString.replaceAll("[\\(\\)]+", " ");
			grantorString = grantorString.replaceAll("\\s+,", ",");
			return grantorString.replaceAll("\\s{2,}", " ");
		}
		return "";
	}
	
	@SuppressWarnings("rawtypes")
	protected static void parseCrossreferences(ServerResponse response, ResultMap resultMap,
			List<Node> allCrossrefNodes, boolean saveLinks) {
		List<List>body = new ArrayList<List>();
		for (Node relatedDocsNode : allCrossrefNodes) {
			if(relatedDocsNode != null && relatedDocsNode.getChildren() != null) {
				NodeList list = relatedDocsNode.getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);
				for (int i = 0; i < list.size(); i++) {
					LinkTag linkTag = (LinkTag) list.elementAt(i);
					
					List<String> line = new ArrayList<String>();
					
					String linkAsPlainText = linkTag.toPlainTextString().trim();
					String[] mainParts = linkAsPlainText.split("-");
					String[] secondaryParts = null;
					if(mainParts.length == 2) {
						secondaryParts = mainParts[1].split("/");
					}
					
					
					line.add(mainParts[0].trim());
					if(secondaryParts != null) {
						if(secondaryParts.length == 2) {
							line.add(secondaryParts[0].trim());
							line.add(secondaryParts[1].trim());
						} else {
							line.add(secondaryParts[0].trim());
							line.add("");
						}
					} else {
						line.add("");
						line.add("");
					}
					body.add(line);
					
					if(saveLinks) {
					
						ParsedResponse prChild = new ParsedResponse();
						
						String linkString = null;
						if(linkTag.extractLink().startsWith("/")) {
							linkString = linkTag.extractLink().trim().replaceAll("\\s", "%20") + "&isSubResult=true";
						}
						LinkInPage pl = new LinkInPage(linkString,linkString,TSServer.REQUEST_SAVE_TO_TSD);
						prChild.setPageLink(pl);
						
						response.getParsedResponse().addOneResultRowOnly(prChild);
					
					}
				}
			}
		}
		
		
		if(!body.isEmpty()) {
			resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, new String[]{ "InstrumentNumber", "Book", "Page", }));
		}
	}

	protected static String getValueForRow(NodeList allRowsAsList, String rowId) {
		try {
			return ((TableRow)allRowsAsList.extractAllNodesThatMatch(new HasAttributeFilter("id",rowId )).elementAt(0))
				.getColumns()[1].toPlainTextString().trim();
		} catch (Exception e) {
			logger.error("Missing rowID: " + rowId, e);
			return "";
		}
	}
	
	/**
	 * Returns some useful information<br>
	 * [0] - HTML content parsed (always returned)<br>
	 * [1] - id from official site (always returned)<br>
	 * [2] - document type (if parsing original HTML - rsResponse contains exactly the table needed)<br>
	 * [3] - instrument number if available or book (if parsing original HTML)<br>
	 * [4] - page if [3] contains book or numberOfPagesForImage (if parsing original HTML)<br>
	 * [5] - numberOfPagesForImage if [3] contains page or does not exist otherwise (if parsing original HTML)<br>
	 * @param rsResponse content to parse
	 * @param link link which generated this content
	 * @return an array filled with useful information
	 */
	public static String[] getDetailedContent(String rsResponse, String link, GenericOncoreServerRO genericOncoreServerRO) {
		long searchId = -1;
		try {
			if(rsResponse != null) {
				searchId = genericOncoreServerRO.getSearch().getID();
				HtmlParser3 parser = new HtmlParser3(rsResponse);
				NodeList nodeList = parser.getNodeList();
				
				if(rsResponse.startsWith("<table")) {
					String[] result = new String[2];
					result[0] = rsResponse;
					result[1] = GenericOncoreServerRO.getDocumentServerIfFromLink(link + "&");
					
					return result;
					
				} else {
				
					NodeList mainTableList = nodeList
						.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "DetailBackground"))
						.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "4"));
					if(mainTableList.size() > 0) {
						
						
						Map<String,String> params = HttpSite.fillConnectionParams(rsResponse, new String[] {"__VIEWSTATE"},"Form1");;
						int seq = GenericOncoreServerRO.getSeq();
						genericOncoreServerRO.getSearch().setAdditionalInfo(genericOncoreServerRO.getCurrentServerName() + ":params:" + seq, params);
						
						
						StringBuilder content = new StringBuilder("<table align=\"center\" border=\"1\" cellpadding=\"4\" cellspacing=\"1\" >");
						TableTag tableTag = (TableTag) mainTableList.elementAt(0);
						NodeList instrumentNumberList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "lblCfn"), true);
						String[] result = null;
						String numberOfPagesAsString = parser.getPlainTextFromNodeById("lblNumberPages", "");
						if(instrumentNumberList.size() > 0) {
							result = new String[5];
							result[3] = instrumentNumberList.elementAt(0).toPlainTextString();
							if(StringUtils.isNotEmpty(numberOfPagesAsString)) {
								try {
									int numberOfPages = Integer.parseInt(numberOfPagesAsString);
									if(numberOfPages > 0) {
										result[4] = numberOfPagesAsString;	
									}
								} catch (Exception e) {
								}
							}
						} else {
							result = new String[6];
							String[] lblBookPage = parser.getPlainTextFromNodeById("lblBookPage", "").split("/");
							if(lblBookPage.length == 2) {
								if(!"0".equals(lblBookPage[0].trim())) {
									result[3] = lblBookPage[0].trim();
								}
								if(!"0".equals(lblBookPage[1].trim())) {
									result[4] = lblBookPage[1].trim();
								}
							}
							if(StringUtils.isNotEmpty(numberOfPagesAsString)) {
								try {
									int numberOfPages = Integer.parseInt(numberOfPagesAsString);
									if(numberOfPages > 0) {
										result[5] = numberOfPagesAsString;	
									}
								} catch (Exception e) {
								}
							}
							
						}
						
						
						
						
						result[1] = GenericOncoreServerRO.getDocumentServerIfFromLink(link + "&");
						if(result[1] == null) {
							Node hlDirectLinkNode = parser.getNodeById("hlDirectLink");
							if(hlDirectLinkNode != null && hlDirectLinkNode instanceof LinkTag) {
								String hlDirectLink = ((LinkTag)hlDirectLinkNode).extractLink();
								result[1] = GenericOncoreServerRO.getDocumentServerIfFromLink(hlDirectLink);
							}
						}
						content.append("<input type=\"hidden\" id=\"server_hidden_id\" value=\"").append(result[1]).append("\">");
						
						if(instrumentNumberList.size() > 0) {
							content.append("<tr id=\"trInstrumentNoAts\"><td>Instrument Number:</td><td id=\"lblCfn\">" + instrumentNumberList.elementAt(0).toPlainTextString() + "</td></tr>");
						}
						
						for (TableRow row : tableTag.getRows()) {
							NodeList crossreferencesList = row.getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("a"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "stdFontSmall"));
							for (int i = 0; i < crossreferencesList.size(); i++) {
								LinkTag crossLink = (LinkTag) crossreferencesList.elementAt(i);
								crossLink.removeAttribute("class");
								crossLink.setLink(crossLink.getLink().replaceAll("(?i)__doPostBack\\('([^']+)','([^']+)'\\)", 
										genericOncoreServerRO.CreatePartialLink(TSConnectionURL.idPOST) +"/oncoreweb/details.aspx?__EVENTTARGET=$1&__EVENTARGUMENT=$2&seq=" + seq ));
								
							}
							
							content.append(row.toHtml());
						}
						content.append("</table>");
						result[0] = content.toString();
						
						
						NodeList doctypeList = nodeList
							.extractAllNodesThatMatch(new TagNameFilter("tr"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "trDocumentType"));
						if(doctypeList.size() > 0) {
							TableRow tableRow = (TableRow) doctypeList.elementAt(0);
							result[2] = tableRow.getColumns()[1].toPlainTextString().replaceAll("[^\\(]*\\(([^\\)]+)\\).*", "$1");
						} else {
							result[2] = "MISCELLANEOUS";
						}
						
						
						
						
											
						return result;
					}
				}
			}
		} catch (Exception e) {
			logger.error("SearchId: " + searchId + ": Error while geting detailed response", e);
		}	
		return null;
	}
}

package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.html.parser.HtmlHelper;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 17, 2011
 */

public class OHSummitTR extends TSServer {

	private static final long	serialVersionUID	= 1L;

	public OHSummitTR(long searchId) {
		super(searchId);
	}

	public OHSummitTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		if ((viParseID == ID_SEARCH_BY_INSTRUMENT_NO || viParseID == ID_SEARCH_BY_PARCEL) && rsResponse.contains("Appraisal and Tax")) {
			viParseID = ID_INTERMEDIARY;
		}
		
		if ((viParseID == ID_SEARCH_BY_NAME || viParseID == ID_SEARCH_BY_ADDRESS) && rsResponse.contains("BASIC INFORMATION FOR PARCEL")) {
			viParseID = ID_DETAILS;
		}

		if (rsResponse.contains("No records selected")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}
		
		if (rsResponse.contains("The parcel") && rsResponse.contains("has been deactivated")) {
			String htmlMessage = rsResponse.replaceFirst("(?is).*<body>\\s*<center>\\s*(?:</?br/?>)*(The parcel[^\\.]+\\.<br/?>[^<]+)<br/?>.*", "$1");
			Response.getParsedResponse().setError("No tax document available. <br><br>Official site response: <br/>" + htmlMessage);
			Response.getParsedResponse().setResponse("");
			return;
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_INTERMEDIARY:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			break;

		case ID_DETAILS:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_PARCEL:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(Response, rsResponse, accountId, data);
			String accountName = accountId.toString();

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = accountName + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("<TITLE>SUMMIT COUNTY FISCAL OFFICE PROPERTY CARD</TITLE>") ? ID_DETAILS : ID_INTERMEDIARY);
			break;
		default:
			break;
		}
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			loadDataHash(data);
			StringBuilder details = new StringBuilder();
			NodeList nodes = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("nowrap"), true);

			if (tables.size() > 0) {
				String id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "PARCEL", true), "", true).replaceAll("<[^>]*>", "").trim();
				accountId.append(id);
			} else
				return null;
			
			Node yNode = HtmlParser3.findNode(tables, "Summit County Auditor Division, OH - Tax Year", false);
			if (yNode != null) {
				String year = yNode.toPlainTextString().replace("Summit County Auditor Division, OH - Tax Year", "").trim();
				if (StringUtils.isNotEmpty(year)) {
					data.put("year", year);
				}
			}

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			// arange the summary html
			TableTag summaryTable = (TableTag) tables.elementAt(0);
			TableRow[] summaryRows = summaryTable.getRows();
			NodeList newSummmaryRows = new NodeList();

			boolean toAdd = true;
			for (TableRow r : summaryRows) {
				toAdd = true;
				if (StringUtils.isNotEmpty(r.getAttribute("bgcolor")) && r.getAttribute("bgcolor").equals("515d88")) {
					toAdd = false;
				}
				if (r.toHtml().contains("<pre>")) {
					for (TableColumn c : r.getColumns())
						c.removeAttribute("rowspan");
				}
				if (r.toHtml().contains("Where Do My Tax Dollars Go?")) {
					toAdd = false;
				}
				if (r.toHtml().contains("GENERAL INFORMATION") || r.toHtml().contains("Click the Following Links to Navigate the Tax Years")) {
					break;
				}

				if (toAdd) {
					newSummmaryRows.add(r);
				}
			}

			if (newSummmaryRows.size() > 0) {
				summaryTable.setChildren(newSummmaryRows);
			}
			summaryTable.setAttribute("id", "SummaryTable");
			summaryTable.setAttribute("width", "95%");
			summaryTable.setAttribute("align", "center");
			summaryTable.removeAttribute("nowrap");

			String summaryInfo = summaryTable.toHtml();
			String taxInfo = "";
			
			if (summaryInfo.contains("FOR CURRENT TAX INFORMATION (TAX YEAR ") && summaryInfo.contains(" CLICK HERE")) {
				String linkTaxes = summaryInfo;
				linkTaxes = linkTaxes.replaceFirst("(?is).*<a href\\s*=\\s*\"([^\"]+)[^>]*>.*\\bFOR CURRENT TAX INFORMATION \\(\\s*TAX YEAR \\d{4}\\s*\\),? CLICK HERE\\s*\\.?.*", "$1");
				if (StringUtils.isNotEmpty(linkTaxes)) {
					taxInfo = getLinkContents(linkTaxes);
					NodeList list = new HtmlParser3(Tidy.tidyParse(taxInfo, null)).getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (list != null && list.size() == 1) {
						TableTag taxTable = (TableTag) list.elementAt(0);
						if (taxTable != null) {
							taxTable.getRows()[0].removeChild(1);
							taxTable.getRows()[0].getChild(0).setText("<td align=\"center\" style=\"padding-left:5; padding-top:5; padding-bottom:5;\" colspan=\"90\">" +
									"<font color=\"yellow\" size=\"14\"> <b> <span align=\"center\"> --- TAX INFORMATION FOR PREVIOS TAX YEAR --- </span> </b> </font>" + 
									"</td>");
							taxInfo = "<br/> <br/>" + taxTable.toHtml();
							newSummmaryRows.remove(newSummmaryRows.size()-1);
							summaryTable.setChildren(newSummmaryRows);
							summaryInfo = summaryTable.toHtml();
						}
					}
				}
			}
			
			details.append(summaryInfo + "\n");
			
			if (StringUtils.isNotEmpty(taxInfo)) {
				details.append(taxInfo + "\n");
			
			} else {
				Node n = ro.cst.tsearch.servers.functions.OHSummitTR.getNodeByTypeAttributeDescription(nodes, "table", "", "", new String[] { "New Query",
						"Tax Hist", "Pay Hist" }, true);

				if (n != null) {
					// extract other links
					TableTag linkT = (TableTag) n;
					TableRow[] linkR = linkT.getRows();
					for (TableRow r : linkR) {
						if (r.toPlainTextString().contains("Document")) {
							details.append(getDocument(getLinkFromTC(r.getColumns()[0]).getLink()));
						}
						if (r.toPlainTextString().contains("Conveyance")) {
							details.append(getConveyance(getLinkFromTC(r.getColumns()[0]).getLink()));
						}
						if (r.toPlainTextString().contains("Tax Hist")) {
							details.append(getTaxHist(getLinkFromTC(r.getColumns()[0]).getLink()));
						}
						if (r.toPlainTextString().contains("Pay Hist")) {
							details.append(getPayHist(getLinkFromTC(r.getColumns()[0]).getLink()));
						}
					}
				}
			}
			
			return details.toString().replaceAll("(?ism)</?a [^>]*>", "");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private LinkTag getLinkFromTC(TableColumn tc) {
		for (int j = 0; j < tc.getChildren().size(); j++) {
			if (tc.getChild(j) instanceof LinkTag)
				return (LinkTag) tc.getChild(j);
		}
		LinkTag l = new LinkTag();
		l.setLink("");
		return l;
	}

	private String getDocument(String link) {
		if (StringUtils.isEmpty(link))
			return "";

		try {
			String documentPage = getLinkContents(link);

			if (StringUtils.isNotEmpty(documentPage)) {
				NodeList nodes = new HtmlParser3(documentPage).getNodeList();

				nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"))
						.extractAllNodesThatMatch(new HasAttributeFilter("nowrap"));

				if (nodes.size() > 0) {
					TableTag t = (TableTag) nodes.elementAt(0);
					t.setAttribute("id", "DocumentTable");
					t.setAttribute("align", "center");
					t.setAttribute("width", "95%");
					String header = "<p align=\"center\"><font size=\"+1\"><b>Document</b></font></p>" + "<br>";
					return "\n<br>" + header + t.toHtml() + "<br>\n";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getConveyance(String link) {
		return "";
	}

	private String getTaxHist(String link) {
		if (StringUtils.isEmpty(link))
			return "";

		try {
			String taxHistPage = getLinkContents(ro.cst.tsearch.connection.http2.OHSummitTR.SERVER_LINK + link);

			if (StringUtils.isNotEmpty(taxHistPage)) {
				NodeList nodes = new HtmlParser3(taxHistPage).getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);

				if (nodes.size() > 0) {
					TableTag t = (TableTag) nodes.elementAt(0);
					t.setAttribute("id", "TaxHistTable");
					t.setAttribute("align", "center");
					t.setAttribute("width", "95%");
					String header = "<p align=\"center\"><font size=\"+1\"><b>Tax History</b></font></p>" + "<br>";
					return "\n<br>" + header + t.toHtml() + "<br>\n";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getPayHist(String link) {
		if (StringUtils.isEmpty(link))
			return "";

		try {
			String documentPage = getLinkContents(ro.cst.tsearch.connection.http2.OHSummitTR.SERVER_LINK + link);

			if (StringUtils.isNotEmpty(documentPage)) {
				NodeList nodes = new HtmlParser3(documentPage).getNodeList()
						.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("border"));

				if (nodes.size() > 0) {
					TableTag t = (TableTag) nodes.elementAt(0);
					t.setAttribute("id", "PayHistTable");
					t.setAttribute("align", "center");
					t.setAttribute("width", "95%");
					String header = "<p align=\"center\"><font size=\"+1\"><b>Payment History</b></font></p>" + "<br>";
					return "\n<br>" + header + t.toHtml() + "<br>\n";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(int viParseID, boolean haveExtraCols) {
		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		header += "<tr bgcolor=\"9d9ed9\">"
				+ "<th>#</th>"
				+ "<th>PARCEL</th>"
				+ "<th>ROUTE</th>";
		
		if (haveExtraCols) {
			header += "<th> </th>"
				+ "<th>ADDRESS</th>"
				+ "<th> </th>";
		} else {
			header += "<th>ADDRESS</th>";
		}
			
		header += "<th>OWNER</th>"
			+ "<th>#CRDS</th>"
			+ "</tr>";

		return header;
	}

//	private String getPrevNext(ServerResponse resp, NodeList nodes) {
//		try {
//			Node table = ro.cst.tsearch.servers.functions.OHSummitTR.getNodeByTypeAttributeDescription(nodes, "table", "width", "650", new String[] { "of",
//					"Next" }, true);
//
//			if (table == null) {
//				table = ro.cst.tsearch.servers.functions.OHSummitTR.getNodeByTypeAttributeDescription(nodes, "table", "width", "650", new String[] { "of",
//						"Prev" }, true);
//			}
//
//			if (table != null) {
//				TableTag t = (TableTag) table;
//				if (t.getRowCount() > 0 && t.getRow(0).getColumnCount() > 0) {
//					TableColumn c = t.getRow(0).getColumns()[0];
//					c.setAttribute("align", "left");
//					NodeList children = c.getChildren();
//
//					if (children.size() == 2) {
//						if (children.elementAt(0) instanceof TextNode) {
//							if (children.elementAt(1) instanceof LinkTag) {
//								LinkTag linkT = (LinkTag) children.elementAt(1);
//								linkT.setLink(CreatePartialLink(TSConnectionURL.idGET) + linkT.getLink());
//							}
//						} else {
//							if (children.elementAt(0) instanceof LinkTag) {
//								LinkTag linkT = (LinkTag) children.elementAt(0);
//								linkT.setLink(CreatePartialLink(TSConnectionURL.idGET) + linkT.getLink());
//							}
//						}
//					} else if (children.size() == 3) {
//						if (children.elementAt(0) instanceof LinkTag) {
//							LinkTag linkT = (LinkTag) children.elementAt(0);
//							linkT.setLink(CreatePartialLink(TSConnectionURL.idGET) + linkT.getLink());
//						}
//						if (children.elementAt(1) instanceof TextNode) {
//						}
//						if (children.elementAt(2) instanceof LinkTag) {
//							LinkTag linkT = (LinkTag) children.elementAt(2);
//							linkT.setLink(CreatePartialLink(TSConnectionURL.idGET) + linkT.getLink());
//						}
//					} else {
//						return "";
//					}
//					t.setAttribute("align", "center");
//					t.setAttribute("width", "95%");
//					t.setAttribute("id", "prevNextLinks");
//					return t.toHtml().replaceAll("(?ism)</?font[^>]*>", "");
//				}
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return "";
//	}

	
	private String getPrevNext(ServerResponse resp, NodeList nodes) {
		String nextLink = "";
		String prevLink = "";
		String links = "";
		
		try {
			if (nodes != null && nodes.size() == 4) {
				TableTag linksTbl = (TableTag) nodes.elementAt(3);
				if (linksTbl != null) {
					NodeList hrefs = linksTbl.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if (hrefs.size() > 0) {
						int noOfLinks = hrefs.size();
						for (int i=0; i < noOfLinks; i++) {
							LinkTag linkT = (LinkTag) hrefs.elementAt(i);
							String url = linkT.getLink();
							String btnType = linkT.getAttribute("onMouseOver");
							
							if (StringUtils.isNotEmpty(btnType)) 
								btnType = btnType.replaceFirst("([^\\.]+).*", "$1").trim();
							if ("previous".equals(btnType.toLowerCase())) 
								prevLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + url +  "\">Prev</a>";
							else if ("next".equals(btnType.toLowerCase()))  {
								nextLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + url +  "\">Next</a>";
								resp.getParsedResponse().setNextLink(nextLink);
								
							}
						}
						
						links = prevLink + "&nbsp; &nbsp;" + nextLink;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return links;
	}
	
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			Integer viParseId = (Integer) mSearch.getAdditionalInfo("viParseID");
			String rsResponse = response.getResult();
			boolean haveExtraCols = false;
			
			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("action", "/clt/refintg3.main"), true);

			if (tableList.size() > 0) {

				FormTag form = (FormTag) tableList.elementAt(0);

//				tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(new HasAttributeFilter("border"));
				String serverLink = ro.cst.tsearch.connection.http2.OHSummitTR.SERVER_LINK;
				
				if (tableList.size() == 1) {
					NodeList inputs = form.getFormInputs();
					StringBuffer postParams = new StringBuffer();
					
					for (int i = 0; i < inputs.size(); i++) {
						String inName = ((InputTag) inputs.elementAt(i)).getAttribute("name");

						if (StringUtils.isNotEmpty(inName) && !"parcel".equals(inName)) {
							postParams.append(inName + "=" + ((InputTag) inputs.elementAt(i)).getAttribute("value") + "&");
						}
					}

					tableList = nodes.extractAllNodesThatMatch(new HasAttributeFilter("class", "results"), true);
					
					if (tableList != null && tableList.size() == 4) {
						TableTag intermediary = (TableTag) tableList.elementAt(2);
						TableRow[] rows = intermediary.getRows();
						for (int i = 1; i < rows.length; i++) {
							TableRow row = rows[i];
							int noOfCols = row.getColumnCount();
							if (noOfCols == 8)
								haveExtraCols = true;
							
							if (haveExtraCols || noOfCols == 6) {
								String link = "";
								TableColumn c = row.getColumns()[1];
								if (c.getChildCount() > 0 && c.childAt(0) instanceof InputTag) {
									InputTag in = (InputTag) c.childAt(0);
									LinkTag linkTag = (LinkTag) HtmlHelper.createTag(LinkTag.class, "a", "/a");
									NodeList children = new NodeList();
									children.add(linkTag);
									c.setChildren(children);
									String parcel = in.getAttribute("value");
									if (linkTag != null && StringUtils.isNotEmpty(parcel)) {
										link = CreatePartialLink(TSConnectionURL.idPOST) + serverLink + "refintg3.main?parcel=" + parcel + "&"
												+ postParams.toString().trim().replaceAll("&$", "");
										linkTag.setLink(link);
										NodeList linkC = new NodeList();
										linkC.add(HtmlHelper.createPlainText(parcel));
										linkTag.setChildren(linkC);
									} else {
										c.setChildren(new NodeList());
									}
								}

								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml().replaceAll("(?ism)</?font[^>]*>", ""));
								currentResponse.setOnlyResponse(row.toHtml());
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

								ResultMap m = ro.cst.tsearch.servers.functions.OHSummitTR.parseIntermediaryRow(row, viParseId);
								Bridge bridge = new Bridge(currentResponse, m, searchId);

								DocumentI document = (TaxDocumentI) bridge.importData();
								currentResponse.setDocument(document);

								intermediaryResponse.add(currentResponse);
							}
						}
					}
				}
			}

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(makeHeader(viParseId, haveExtraCols));
				//parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, nodes));
			} else {
				parsedResponse.setHeader("<table border=\"1\">");
				//parsedResponse.setFooter("</table>");
			}
			parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, tableList));
			outputTable.append(table);
			 	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		ro.cst.tsearch.servers.functions.OHSummitTR.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();

			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.forceValue(0, parcelno);
			moduleList.add(module);
		}

//		//alernate pin
//		String instrNo = getSearchAttribute(SearchAttributes.LD_PARCELNO2);
//		if (StringUtils.isNotEmpty(instrNo)) {
//			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
//			module.clearSaKeys();
//
//			module.forceValue(0, instrNo.replaceAll("[^\\d]", ""));
//			moduleList.add(module);
//		}
		
		// address
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();

			if (hasStreetNo()) {
				module.setSaKey(0, SearchAttributes.P_STREET_NO_NAME);
			} else {
				module.setSaKey(0, SearchAttributes.P_STREETNAME);
			}
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);
		}

		// owner
		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);

	}

}

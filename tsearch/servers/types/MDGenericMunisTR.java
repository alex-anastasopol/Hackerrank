/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author Vladimir
 *
 */
public class MDGenericMunisTR extends TSServer {

	private static final long serialVersionUID = 1L;
	private static final String FORM_NAME = "aspnetForm";

	public MDGenericMunisTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:
				// no result
				if (rsResponse.contains("No Parcels found based on specified search criteria")) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				if(rsResponse.contains("Bill Year")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					StringBuilder outputTable = new StringBuilder();
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
					if(smartParsedResponses.size() == 0) {
						return;
					}

					parsedResponse.setHeader("<table style='border-collapse: collapse' border='2' width='80%' align='center'>" + parsedResponse.getHeader());
					parsedResponse.setFooter("</table>");
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				}
				
				break;
			case ID_GET_LINK:
				if(rsResponse.contains("Bill Year")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				}
				break;
			case ID_DETAILS:
				if (rsResponse.contains("No Parcels found based on specified search criteria")) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder accountId = new StringBuilder();
				StringBuilder year = new StringBuilder();
				String details = getDetails(rsResponse, accountId, year);
				
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				data.put("year", year.toString());
				
				if (isInstrumentSaved(accountId.toString().trim(), null, data)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
				
				break;
			case ID_SAVE_TO_TSD:
				accountId = new StringBuilder();
				year = new StringBuilder();
				details = getDetails(rsResponse, accountId, year);
				
				String filename = accountId + ".html";
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				parsedResponse.setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
				break;
		}
	}
	
	private String getDetails(String page, StringBuilder accountId, StringBuilder year) {
		StringBuilder details = new StringBuilder();
		
		try {
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node accountIdNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_CategoryLabel"), true).elementAt(0);
			if(accountIdNode != null) {
				accountId.append(accountIdNode.toPlainTextString());
			}

			Node yearNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_FiscalYearLabel"), true).elementAt(0);
			if(yearNode != null) {
				year.append(yearNode.toPlainTextString());
			}
			
			// page from memory
			if(!page.toLowerCase().contains("<html")) {
				return page;
			}
			
			// get address from AO link
			LinkTag assessorLinkTag = (LinkTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "right captioninformation"), true)
					.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
			if(assessorLinkTag != null) {
				String assessorPage = getLinkContents(assessorLinkTag.getLink().replaceAll("(?is)&amp;", "&"));
				
				org.htmlparser.Parser htmlParser3 = org.htmlparser.Parser.createParser(assessorPage, null);
				NodeList nodeList3 = htmlParser3.parse(null);
				
				Node addrNode = HtmlParser3.getAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList3, "Premises Address"), false);
				if(addrNode != null) {
					String address = addrNode.toPlainTextString().trim();
					details.append("<div align='center'><b>Property Address:</b> " + address + "</div>");
				}
			}
			
			
			NodeList linkList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "submenu"), true)
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			
			for(int i = 0; i < linkList.size(); i++) {
				LinkTag linkTag = (LinkTag) linkList.elementAt(i);
				String link = linkTag.getLink();
				
				if(link.contains("ContactUs.aspx")) {
					break;
				}
				
				String htmlPage = getLinkContents(dataSite.getLink().replaceFirst("(?is)(.*?[^/])/[^/].*", "$1") + link);
				
				try {
					org.htmlparser.Parser htmlParser1 = org.htmlparser.Parser.createParser(htmlPage, null);
					NodeList nodeList1 = htmlParser1.parse(null);
					
					if(link.contains("ViewBill")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "BillDetailTable"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }
						
						table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("class", "datatable nomargin"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }
					} else if(link.contains("TaxCharges")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_TaxChargesTable"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }
						
						table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_TaxExemptionsTable"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }

						table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_TotalTaxTable"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }
					}  else if(link.contains("ParcelDetail")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ParcelTable"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }
					} else if(link.contains("OwnerInformation")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("class", "informationtable"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table, "OwnerTable")); }
					} else if(link.contains("Assessments")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("class", "datatable"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table, "AssessmentTable")); }
					} else if(link.contains("AssessmentHistory")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_AssessmentHistoryGrid"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }
					} else if(link.contains("TaxRates")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_MolGridView1"), true).elementAt(0);
						if(table != null) { details.append(tableToHtml(table)); }
					} else if(link.contains("AllBills")) {
						TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_BillsRepeater_ctl00_BillsGrid"), true).elementAt(0);
						TableRow[] rows = table.getRows();
						boolean first = true;
						
						Form form = new SimpleHtmlParser(htmlPage).getForm(FORM_NAME);
						Map<String, String> params = form.getParams();
						int seq = getSeq();
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						
						for(TableRow row : rows) {
							if(row.toPlainTextString().contains("View Bill")) {
								LinkTag billLinkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
								if(billLinkTag != null) {
									if(first) {
										// we skip current year
										first = false;
										continue;
									}
									
									processIntermediaryLink(billLinkTag, form.action, seq);
									String billLink = dataSite.getLink().replaceFirst("(?is)(.*?[^/])/[^/].*", "$1") + billLinkTag.getLink().replaceFirst(".*?&Link=", "");
									String[] linkParts = billLink.split("[?&]");
									
									HTTPRequest reqP = new HTTPRequest(linkParts[0]);
									for(int j = 1; j < linkParts.length; j++) {
										String part = linkParts[j];
										String[] tokens = part.split("=");
										reqP.setPostParameter(tokens[0], tokens.length > 1 ? tokens[1] : "");
									}
							    	reqP.setMethod(HTTPRequest.POST);
							    	HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
							    	String billPage = "";
						        	
							    	try {
						        		HTTPResponse resP = site.process(reqP);
						        		billPage = resP.getResponseAsString();
						        	} finally {
										HttpManager3.releaseSite(site);
									}
							    	
							    	if(StringUtils.isNotEmpty(billPage)) {
							    		org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(billPage, null);
										NodeList nodeList2 = htmlParser2.parse(null);
										
							    		TableTag table2 = (TableTag) nodeList2.extractAllNodesThatMatch(new HasAttributeFilter("id", "BillDetailTable"), true).elementAt(0);
							    		if(table2 != null) { details.append(tableToHtml(table2)); }
										
										table2 = (TableTag) nodeList2.extractAllNodesThatMatch(new HasAttributeFilter("class", "datatable nomargin"), true).elementAt(0);
										if(table2 != null) { details.append(tableToHtml(table2)); }
							    	}
								}
							}
						}
						
					}
				} catch (Exception e) {
					logger.error("Error while getting details", e);
				}
			}
		} catch(Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return details.toString();
	}

	private String tableToHtml(TableTag table) {
		return tableToHtml(table, null);
	}
	
	private String tableToHtml(TableTag table, String id) {
		table.setAttribute("style", "border-collapse: collapse");
		table.setAttribute("border", "2");
		table.setAttribute("width", "80%");
		table.setAttribute("align", "center");
		if(id != null) {
			table.setAttribute("id", id);
		}
		return table.toHtml().replaceAll("<a [^>]*>(.*?)</a>", "$1")
				.replaceAll("View state assessment data", "")
				.replaceAll("View payments/adjustments", "");
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Form form = new SimpleHtmlParser(page).getForm(FORM_NAME);
			Map<String, String> params = form.getParams();
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			
			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_MolGridView1"), true)
					.elementAt(0);
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString();
				
				if(rowText.contains("Bill Type")) {
					// table header
					parsedResponse.setHeader(row.toHtml().replaceAll("(?is)<a [^>]*>(.*?)</a>", "$1"));
					continue;
				}
				
				LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				if(linkTag == null) {
					continue;
				}
				processIntermediaryLink(linkTag, form.action, seq);
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
				currentResponse.setOnlyResponse(row.toHtml());
				currentResponse.setPageLink(new LinkInPage(linkTag.getLink(), linkTag.getLink(), TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.MDGenericMunisTR.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			outputTable.append(page);
		} catch(Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		
		return intermediaryResponse;
	}

	private void processIntermediaryLink(LinkTag linkTag, String formAction, int seq) {
		String link = CreatePartialLink(TSConnectionURL.idPOST) + "/citizens/RealEstate/" + formAction + "?" + "seq=" + seq + "&";
		
		Pattern p = Pattern.compile("(?is)__doPostBack[(]'([^']*)','([^']*)'[)]");
		Matcher m = p.matcher(linkTag.getLink().replace("&#39;", "'"));
		
		if(m.find()) {
			link += "__EVENTTARGET=" + m.group(1) + "&";
			link += "__EVENTARGUMENT=" + m.group(2) + "&";
		}
		
		linkTag.setLink(link);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		try {
			String owner = "";
			String legal = "";
			String address = "";
			double delinq = 0d;
			
			Matcher m = Pattern.compile("Property Address:</b> (.*?)</div>").matcher(detailsHtml);
			if(m.find()) {
				address = m.group(1);
				
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			detailsHtml = detailsHtml.replaceAll("(?is)<br(\\s*)?/?>", "\n");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node accountIdNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_CategoryLabel"), true).elementAt(0);
			if(accountIdNode != null) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountIdNode.toPlainTextString());
			}
			Node yearNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_FiscalYearLabel"), true).elementAt(0);
			if(yearNode != null) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), yearNode.toPlainTextString());
			}
			Node billNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_BillNumberLabel"), true).elementAt(0);
			if(billNode != null) {
				resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), billNode.toPlainTextString());
			}

			// get tax tables for current year and previous ones
			NodeList billTableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "datatable nomargin"), true);
			NodeList billDetailTableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "BillDetailTable"), true);
			boolean firstYear = true;
			
			if(billTableList != null && billDetailTableList != null && billTableList.size() == billDetailTableList.size()) {
				for(int i = 0; i < billTableList.size(); i++) {
					TableTag billTable = (TableTag) billTableList.elementAt(i);
					TableRow[] rows = billTable.getRows();
					
					double amountDue = 0d;
					double penalties = 0d;
					double total = 0d;
					String amountPaid = "";
	
					for(TableRow row : rows) {
	
						if(row.getColumnCount() > 0 && row.getColumns()[0].toPlainTextString().contains("Interest and Penalties")) {
							try {
								penalties = Double.parseDouble(row.getColumns()[1].toPlainTextString().replaceAll("[$,]", "").trim());
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if(row.toPlainTextString().contains("TOTAL")) {
							try {
								total = Double.parseDouble(row.getColumns()[0].toPlainTextString().replaceAll("[$,]", "").trim());
								amountPaid = row.getColumns()[1].toPlainTextString().replaceAll("[$,]", "").trim();
								amountDue = Double.parseDouble(row.getColumns()[3].toPlainTextString().replaceAll("[$,]", "").trim());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					if(firstYear) {
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), "" + (total - penalties));
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "" + amountPaid);
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "" + amountDue);
						firstYear = false;
					} else {
						delinq += amountDue;
					}
					
				}
				
			} else {
				logger.error("Error while parsing details");
			}
			
			TableTag parcelTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ParcelTable"), true).elementAt(0);
			TableRow[] rows = parcelTable.getRows();
			
			for(TableRow row : rows) {
				if(row.getColumnCount() == 1) {
					String rowText = row.toPlainTextString();
					String col = row.getColumns()[0].toPlainTextString().trim();
					
					if(rowText.contains("Location")) {
						legal = col;
					} else if(rowText.contains("Book/Page")) {
						String book = col.replaceFirst("/.*", "").replaceFirst("^0+", "");
						String page = col.replaceFirst(".*/", "").replaceFirst("^0+", "");
						resultMap.put(CrossRefSetKey.BOOK.getKeyName(), book);
						resultMap.put(CrossRefSetKey.PAGE.getKeyName(), page);
					}
				}
			}
			
			
			TableTag ownerTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "OwnerTable"), true).elementAt(0);
			rows = ownerTable.getRows();
			
			for(TableRow row : rows) {
				if(row.getColumnCount() == 1) {
					String rowText = row.toPlainTextString();
					String col = row.getColumns()[0].toPlainTextString().trim();
					
					if(rowText.contains("Name")) {
						owner = col;
					}
				}
			}


			TableTag assessmentTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "AssessmentTable"), true).elementAt(0);
			rows = assessmentTable.getRows();
			
			for(TableRow row : rows) {
				if(row.getColumnCount() == 1) {
					String rowText = row.toPlainTextString();
					String col = row.getColumns()[0].toPlainTextString().trim();
					
					if(rowText.contains("Land")) {
						resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), col.replaceAll("[$,]", ""));
					} else if(rowText.contains("Total")) {
						resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), col.replaceAll("[$,]", ""));
					}
				}
			}
			
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "" + delinq);
			ro.cst.tsearch.servers.functions.MDGenericMunisTR.parseOwners(resultMap, owner);
			ro.cst.tsearch.servers.functions.MDGenericMunisTR.parseLegal(resultMap, legal);
		} catch(Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		
		// search by account number
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(2).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
			module.getFunction(1).setSaKey(SearchAttributes.P_STREETNAME);
			
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(taxYearFilter);
			
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
}

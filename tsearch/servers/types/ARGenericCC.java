
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator.SEARCH_WITH_TYPE;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.LienI;
import com.stewart.ats.base.document.RegisterDocumentI;

/**
 * @author vladb
 *
 */
public class ARGenericCC extends TSServer {

	private static final long serialVersionUID = 1L;
	private static int seq = 0;

	public ARGenericCC(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	protected synchronized static int getSeq(){
		return seq++;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch(viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_PARCEL:
			if(rsResponse.indexOf("There were no records found") > -1) {
				Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			String resultsPageLink = dataSite.getLink() + "/search.php?" + Response.getRawQuerry();
			int seqNo = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":resultsPageLink:" + seqNo, resultsPageLink);
			outputTable.append(seqNo);
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() == 0) {
				return;
			}
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
            	String footer = parsedResponse.getFooter();

            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument);
            	} else {
            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
            	}
            	
            	parsedResponse.setFooter(footer);
            }
			
			break;
		case ID_GET_LINK:
			if (rsResponse.indexOf("Select a filing and press Display") > -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			StringBuilder accountId = new StringBuilder();
			String details = getDetails(Response, sAction, accountId);
			
			if(viParseID == ID_DETAILS) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "UCC");
				if (isInstrumentSaved(accountId.toString().trim(), null, data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
			} else {
				String filename = accountId + ".html";
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				parsedResponse.setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			
			break;
		}
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		int numberOfUncheckedElements = 0;
		int seqNo = Integer.parseInt(outputTable.toString());
		outputTable.delete(0, outputTable.length());
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
				.elementAt(0);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString().toLowerCase().replaceAll("(?i)&nbsp;", " ");
				
				if(rowText.indexOf("submission time") > -1) {
					String tableStart = "<table border=\"1\" width=\"90%\" align=\"center\">";
					String header = "<tr><td>"+ SELECT_ALL_CHECKBOXES + "</td>" + row.toHtml().replaceFirst("(?is)<tr[^>]*>", "");
					parsedResponse.setHeader(CreateSaveToTSDFormHeader(
							URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + 
							tableStart +
							header);
					continue;
				}
			
				String filingNo = row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;", "")
					.replaceAll("[(].*?[)]", "").trim();
				String link = CreatePartialLink(TSConnectionURL.idPOST) + "search.php?seq=" + seqNo + "&filing_numbers[]=" + 
					filingNo + "&ac:save_step3_search_results:=Display&nextaction=save_step3_search_results";
				String rowHtml = row.toHtml().replaceFirst("(?is)<input .*?>", "")
					.replaceAll("[(]Viewed[)]", "").replace(filingNo, "<a href='" + link + "'>" + filingNo + "</a>");
				
				ParsedResponse currentResponse = new ParsedResponse();
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "UCC");
				String checkBox = "checked";
				if (isInstrumentSaved(filingNo, null, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
	    			checkBox = "saved";
	    		} else {
	    			numberOfUncheckedElements++;
	    			checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
	    			currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
	    		}
				rowHtml = rowHtml.replaceFirst("(?is)<tr[^>]*>", "$0<td>" + checkBox + "</td>");
				
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				
				ResultMap m = ro.cst.tsearch.servers.functions.ARGenericCC.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (RegisterDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			
			// set navigation links
			String nextLink = "";
			TableTag navigTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "links")).elementAt(0);
			if(navigTable != null) {
				for(TableColumn col : navigTable.getRow(1).getColumns()) {
					String colText = col.toPlainTextString();
					// there is a problem with First and Last links on official site, so I remove them
					if(colText.indexOf("First") > -1 || colText.indexOf("Last") > -1) {
						col.getParent().getChildren().remove(col);
					} else {
						NodeList links = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						for(int i = 0; i < links.size(); i++) {
							LinkTag link = (LinkTag) links.elementAt(i);
							link.setLink(link.getLink().replaceFirst("/sos/ucc/", CreatePartialLink(TSConnectionURL.idGET)));
							if(colText.indexOf("Next") > -1) {
								nextLink = "<a href='" + link.getLink() + "'>";
							}
						}
					}
				}
				parsedResponse.setFooter("<tr><td colspan='5' align='center'>" + navigTable.toHtml() + "</td></tr></table>");
				parsedResponse.setNextLink(nextLink);
			} else {
				parsedResponse.setFooter("</table>");
			}
			
			outputTable.append(rsResponse);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	private String getDetails(ServerResponse Response, String sAction, StringBuilder accountId) {
		String rsResponse = Response.getResult();
		StringBuilder details = new StringBuilder();
		
		try {
			if(rsResponse.indexOf("The following filing numbers were part of a previous search which has expired") > -1) {
				String query = Response.getQuerry();
				Matcher m = Pattern.compile("[&?]seq=(\\d+)").matcher(sAction);
				if(m.find()) {
					String seq = m.group(1);
					String resultsPageLink = (String) mSearch.getAdditionalInfo(getCurrentServerName() + ":resultsPageLink:" + seq);
					getLinkContents(resultsPageLink);
					
					DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
					String detailsLink = dataSite.getLink() + "/search.php";
					HTTPRequest reqP = new HTTPRequest(detailsLink);
			    	reqP.setMethod(HTTPRequest.POST);
			    	String[] params = query.split("&");
			    	for(String param : params) {
			    		String[] entries = param.split("=");
			    		if(entries.length == 2) {
			    			reqP.setPostParameter(entries[0], entries[1]);
			    		}
			    	}
			    	
			    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		        	try {
		        		HTTPResponse resP = site.process(reqP);
		        		rsResponse = resP.getResponseAsString();
		        	} finally {
						HttpManager.releaseSite(site);
					}	
				}
			}
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag detailsTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "search_results"), true)
				.elementAt(0);
			String detailsHtml = detailsTable.toHtml();
			detailsHtml = detailsHtml.replaceAll("<a[^>]*>(.*?)</a>", "$1")
				.replaceAll("&lt;View Image&gt;", "");
			
			Matcher m = Pattern.compile("(?is)Filing Number:\\s*(\\d+)").matcher(detailsTable.toPlainTextString());
			if(m.find()) {
				accountId.append(m.group(1));
			}
			
			details.append(detailsHtml);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return details.toString();
	}
	
	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse) {
		LienI document = (LienI) super.smartParseDetails(response, detailsHtml, fillServerResponse);
		
		detailsHtml = detailsHtml.replaceAll("(?is)<[^>]*>", ""); // remove tags
		Matcher m = Pattern.compile("(?i)Status:\\s*(.*)").matcher(detailsHtml);
		if(m.find()) {
			document.setStatus(m.group(1).trim());
		}
		
		return document;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag detailsTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "search_results"), true)
				.elementAt(0);
			String details = detailsTable.toPlainTextString();
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");
			
			Matcher m = Pattern.compile("(?is)Filing Number:\\s*(\\d+)").matcher(details);
			if(m.find()) {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), m.group(1));
			}
			
			m = Pattern.compile("(?i)Date/Time:\\s*(.*)").matcher(details);
			if(m.find()) {
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), m.group(1));
			}
			m = Pattern.compile("(?i)Expiration Date:\\s*(.*)").matcher(details);
			if(m.find()) {
				resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), m.group(1));
			}
			
			ArrayList<String> grantors = new ArrayList<String>();
			m = Pattern.compile("(?is)Debtor\\s*[(].*?[)]\\s*Name:\\s*(.*?);").matcher(details);
			while(m.find()) {
				grantors.add(m.group(1));
			}
			ro.cst.tsearch.servers.functions.ARGenericCC.parseNames(resultMap, grantors, "GrantorSet");
			
			ArrayList<String> grantees = new ArrayList<String>();
			m = Pattern.compile("(?is)Secured Party\\s*[(].*?[)]\\s*Name:\\s*(.*?);").matcher(details);
			while(m.find()) {
				grantees.add(m.group(1));
			}
			ro.cst.tsearch.servers.functions.ARGenericCC.parseNames(resultMap, grantees, "GranteeSet");
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
		if (hasOwner()){
			// search with person name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(defaultNameFilter);
			addFilterForUpdate(module, true);
			
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[] {"L;F;M"});
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.PERSON_NAME);
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
				
			modules.add(module);
			
			// search with Organization / Business Name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(defaultNameFilter);
			addFilterForUpdate(module, true);
			
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			nameIterator = new ConfigurableNameIterator(searchId, new String[] {"L;;"});
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
				
			modules.add(module);
    	}
			
	    serverInfo.setModulesForAutoSearch(modules);
	}
}

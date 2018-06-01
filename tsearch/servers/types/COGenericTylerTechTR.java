package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;

import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;


/**
 * 
 * 
 * used by COGarfieldTR, COMesaTR, COPitkinTR, CORouttTR, COSanMiguelTR, COWeldTR
 *
 */
public class COGenericTylerTechTR extends TSServer {

	static final long serialVersionUID = 10000000;
	private static int unqDmyNmbr = 0;

	
	public String getDetailsPrefix() {
		return "/treasurer/treasurerweb/";
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
	    
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule m;
	                         	
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.7d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
			l.add(m);
		}
		
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(3).setSaKey(SearchAttributes.LD_PARCELNO2);
			l.add(m);
		}
		
		if(hasPin()){			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(3).setSaKey(SearchAttributes.LD_PARCELNO);
			l.add(m);
		}
		
		if(hasStreet()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(5).setSaKey( SearchAttributes.P_STREETNO );
			m.getFunction(7).setSaKey( SearchAttributes.P_STREETNAME );
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(addressFilter);
			m.addFilter(defaultLegalFilter);
			l.add(m);
		}
		
		if( hasOwner() )
	    {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);			
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.addFilter(nameFilterHybrid);
			m.addFilter(rejectNonRealEstateFilter);
			m.addFilter(addressFilter);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] {"L;F;M","L;F;"});
			m.addIterator(nameIterator);
			l.add(m);
	    }
	
		serverInfo.setModulesForAutoSearch(l);
	
	  }

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
	
	    String rsResponse = Response.getResult();
	    
		switch (viParseID) {
		
		case ID_SEARCH_BY_NAME:
	
			if (rsResponse.contains("No accounts found, please refine your search")) {
				return;
			}
	
			try {
	
				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();
	
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
	
				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				}
	
			} catch (Exception e) {
				e.printStackTrace();
			}
	
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
	
			
			String accountId = StringUtils.extractParameterFromUrl(Response.getQuerry(),"account");
	
			if(StringUtils.isEmpty(accountId)) {
				return;
			}
			
			String originalLink = sAction + "&" + Response.getRawQuerry();
			String details = getDetails(rsResponse, originalLink, accountId);

			String filename = accountId + ".html";
	
			if (viParseID == ID_DETAILS) {
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
				boolean isInstrumentSaved = false;
				try {
					String year = StringUtils.extractParameter(details, "(?i)Tax Billed at (\\d{2,4}) Rates");
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("year",year);
					isInstrumentSaved =  isInstrumentSaved(accountId,null,data);
				}catch(Exception e) {
					e.printStackTrace();
				}
				
				if (isInstrumentSaved){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,viParseID);
				}
				
				//ResultMap map = new ResultMap();
				//test
				//parseAndFillResultMap(Response, details,  map );
				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
	
			} else {
				smartParseDetails(Response, details);
	
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);
	
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	
			}
			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response,idGetLink(rsResponse));
			break;
		}
	}

	protected String getSaleInfoTable(String accountId) {
		return "";
	}

	protected int idGetLink(String rsResponse) {
		int viParseID = ID_DETAILS;
		if (rsResponse.contains("Search Results")) {
			viParseID = ID_SEARCH_BY_NAME;
		}
		return viParseID;
	}
	protected String getDetails(String response, String originalLink, String accountNo) {
		
		/* If from memory - use it as is */
		if(!response.contains("<html")){
			return response;
		}
		
		originalLink = originalLink.replaceFirst("\\?dummy=true&", "?");
		HtmlParser3 parser= new HtmlParser3(response);
	
		Div taxAccountSummary = (Div)parser.getNodeById("taxAccountSummary", true);
		Div taxAccountValueSummary = (Div)parser.getNodeById("taxAccountValueSummary", true);
		
		String accountValuePage = "";
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {		
			accountValuePage = ((ro.cst.tsearch.connection.http2.COGenericTylerTechTR)site).getPage(originalLink+"&action=billing");
			accountValuePage = Tidy.tidyParse(accountValuePage, null);
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			HttpManager.releaseSite(site);
		}
		
		String transactionDetailPage = "";
		site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {		
			transactionDetailPage = ((ro.cst.tsearch.connection.http2.COGenericTylerTechTR)site).getPage(originalLink+"&action=tx");
			transactionDetailPage = Tidy.tidyParse(transactionDetailPage, null);
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			HttpManager.releaseSite(site);
		}
		
		parser = new HtmlParser3(accountValuePage);
		Div accountValueDiv = (Div)parser.getNodeById("middle", true);
		String accountValueString = "";
		if(accountValueDiv!=null) {
			accountValueString = accountValueDiv.toHtml();
		}
		
		parser= new HtmlParser3(transactionDetailPage);
		Div transactionDetailDiv = (Div)parser.getNodeById("middle", true);
		String transactionDetailString = "";
		if(transactionDetailDiv!=null) {
			transactionDetailString = transactionDetailDiv.toHtml();
		}
		
		String result = taxAccountSummary.toHtml() + 
		 	   (taxAccountValueSummary.toHtml()).replaceAll("(?i)<a.*?>(.*?)</a>", "$1")  + 
		 	   "<div id='accountValueString'>"+ accountValueString +  "</div>" + 
		 	   "<div id='transactionDetailString'>" + transactionDetailString +  "</div>";
		result = clean(result);
		
		result = result + getSaleInfoTable(accountNo);
		result = result.replaceAll("(?is)<table ", "<table border=\"1\" ");
		result = result.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
		
		return result ;
		
	}

	protected String clean(String result) {
		return result;
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			 			
			HtmlParser3 parser = new HtmlParser3(table);
			
			TableTag mainTable = (TableTag) parser.getNodeById("searchResultsTable", true);
						
			TableRow[] rows = mainTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					
					String link = HtmlParser3.getFirstTag(row.getColumns()[0].getChildren(), LinkTag.class, true).getLink();		
						
					link = link.replaceFirst("\\?", "?dummy=true&");
					link = CreatePartialLink(TSConnectionURL.idGET) + getDetailsPrefix() + link;//+ "&print=true";
					String rowHtml =  row.toHtml().replaceAll("(?s)<a.*?>(.*?)</a>","<a href=\""+Matcher.quoteReplacement(link)+"\">$1</a>" );
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = parseIntermediaryRow(row);
					Bridge bridge = new Bridge(currentResponse,m,searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
		String header0 = "<tr><td colspan=2>"+((Span)parser.getNodeByAttribute("class", "pagebanner", true)).toHtml() + proccessLinks(response,(Span)parser.getNodeByAttribute("class", "pagelinks", true))+"</td></tr>";
		String header1 = rows[0].toHtml();
		
		if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n"+ header0 + header1 );
			response.getParsedResponse().setFooter("</table>");
	    }
		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	private String proccessLinks(ServerResponse response, Span linksSpan) {
				
		try {
			Node[] links = linksSpan.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"),true).toNodeArray();
			for(Node n : links) {
				LinkTag lnk = (LinkTag) n;
				String link = lnk.getLink();
				link = link.replace("/treasurerweb/..", "");
				lnk.setLink(CreatePartialLink(TSConnectionURL.idGET) + link);
				if(lnk.getChildrenHTML().contains("Next")) {
					
					if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() != Search.PARENT_SITE_SEARCH) {
						lnk.setLink(lnk.getLink()+ "&unqDmyNmbr=" + unqDmyNmbr++);
					}
					String linkNext = lnk.toHtml().replaceAll("(?i)&amp;", "&");
					response.getParsedResponse().setNextLink( linkNext );
				}
			}
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return linksSpan.toHtml();
	}

	protected ResultMap parseIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.COWeldTR.parseIntermediaryRow(row, this);
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.COWeldTR.parseAndFillResultMap(detailsHtml,map,searchId, this);
//		test
		//saveTestDataToFiles( map );
		return null;
	}

	protected void saveTestDataToFiles(ResultMap map) {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String pin = "" + map.get("PropertyIdentificationSet.ParcelID");
			String text = pin + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") + "\r\n\r\n\r\n";
			String path = "D:\\" + this.getClass().getSimpleName() + "\\";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.LegalDescriptionOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "legal_description.txt", text);

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.NameOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt", text);
			// end test

		}
	}

	
	public COGenericTylerTechTR(long searchId) {
		super(searchId);
	}

	public COGenericTylerTechTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	

	public void parseLegal(String contents, ResultMap resultMap) throws Exception {
			ro.cst.tsearch.servers.functions.COWeldTR.parseLegal(contents, resultMap);
	}

	public void parseName(Set<String> hashSet, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COWeldTR.parseName(hashSet, resultMap);
	}
	
	public void parseAddress(String address, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COWeldTR.parseAddress(resultMap, address);
	}

	public void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) throws Exception {
		int countyId = server.getDataSite().getCountyId();
		if (CountyConstants.CO_Grand==countyId) {
			ro.cst.tsearch.servers.functions.COGrandTR.parseIntermediaryRow(server, resultMap, cols);
		} else {
			ro.cst.tsearch.servers.functions.COWeldTR.parseIntermediaryRow(server, resultMap, cols);
		}
	}

}
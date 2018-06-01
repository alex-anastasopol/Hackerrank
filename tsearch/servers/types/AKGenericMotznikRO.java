package ro.cst.tsearch.servers.types;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateFormatUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SynonimNameFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.instrument.InstrumentAKMotznikROIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class AKGenericMotznikRO extends TSServer implements TSServerROLikeI {

	static final long serialVersionUID = 10000000;
	public static String selectAll = 
		"<input type=\"checkbox\" onClick=\"var elems=document.getElementsByName('docLink'); for(var i=0; i<elems.length;i++) {elems[i].checked = this.checked;}\"/>";
	private static final Pattern certDatePattern = Pattern.compile("(?is)LabelRecordersOffice\"\\s*>\\s*(\\d{1,2}/\\d{1,2}/\\d{4})");
	
	public AKGenericMotznikRO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public AKGenericMotznikRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public static enum IntermediaryResultTypes {
		NAME_LEVEL_1,
		NAME_LEVEL_2,
		DOCUMENT_NUMBER,
		BOOK_PAGE,
		SUBDIVISION_LEVEL_1,
		SUBDIVISION_LEVEL_2,
		NO_PLAT_SUBDIVISION_LEVEL_1,
		LEGAL_DESCRIPTION,
		DISTINCT_LEGALS
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
					
		switch (viParseID)
		{
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_INSTRUMENT_NO:
			case ID_SEARCH_BY_BOOK_AND_PAGE:
			case ID_SEARCH_BY_SUBDIVISION_NAME:	
			case ID_SEARCH_BY_MODULE50:
			case ID_SEARCH_BY_MODULE51:
			case ID_SEARCH_BY_MODULE52:
			case ID_SEARCH_BY_MODULE53:
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					HtmlParser3 parser = new HtmlParser3(rsResponse);
					
					String table = parser.getNodeContentsById("Table2");
					if(table == null || table.isEmpty()) {
						/* For level 2 intermediary results */
						table = parser.getNodeContentsById("Table1");
					}
					
					if(table==null || rsResponse.indexOf("NO RESULTS")>0 || rsResponse.contains("Application error (Rails)")) {
						 rsResponse ="<table><th><b>No records found! Please try again.</b></th></table>";
								Response.getParsedResponse().setOnlyResponse(rsResponse);
						    	return;
					}
					
					Collection<ParsedResponse> smartParsedResponses = new ArrayList<ParsedResponse>();
					IntermediaryResultTypes type = null;
					switch (viParseID) {
						case ID_SEARCH_BY_NAME : 
							if(sAction.contains("/distinct_names")) {
								type = IntermediaryResultTypes.NAME_LEVEL_1;
							}else if(sAction.contains("/name")) {
								type = IntermediaryResultTypes.NAME_LEVEL_2;
							}
							smartParsedResponses = smartParseNameIntermediary(Response, table, outputTable, viParseID, type);
							parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
							break;
						
						case ID_SEARCH_BY_INSTRUMENT_NO:
							smartParsedResponses = smartParseDocumentNumberIntermediary(Response, table, outputTable,viParseID);
							break;
						
						case ID_SEARCH_BY_BOOK_AND_PAGE:
							smartParsedResponses = smartParseBookPageIntermediary(Response, table, outputTable,viParseID);
							break;
							
						case ID_SEARCH_BY_SUBDIVISION_NAME:
							if(sAction.contains("/distinct_subdivisions")) {
								type = IntermediaryResultTypes.SUBDIVISION_LEVEL_1;
							}else if(sAction.contains("/subdivision")) {
								type = IntermediaryResultTypes.SUBDIVISION_LEVEL_2;
							}else if(sAction.contains("/legal")) {
								type = IntermediaryResultTypes.LEGAL_DESCRIPTION;
							}
							smartParsedResponses = smartParseSubdivisionIntermediary(Response, table, outputTable, viParseID, type);
							parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
							break;
								
						case ID_SEARCH_BY_MODULE50:
						case ID_SEARCH_BY_MODULE51:
						case ID_SEARCH_BY_MODULE52:
							if(sAction.contains("/distinct_legals")) {
								type = IntermediaryResultTypes.DISTINCT_LEGALS;
							}else if(sAction.contains("/legal")) {
								type = IntermediaryResultTypes.LEGAL_DESCRIPTION;
							}
							smartParsedResponses = smartParsePlatTractBlockLotIntermediary(Response, table, outputTable, viParseID, type);
							parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
							break;
							
						case ID_SEARCH_BY_MODULE53:
							if(sAction.contains("/distinct_no_plat_subdivisions")) {
								type = IntermediaryResultTypes.NO_PLAT_SUBDIVISION_LEVEL_1;
							}else if(sAction.contains("/legal")) {
								type = IntermediaryResultTypes.LEGAL_DESCRIPTION;
							}
							smartParsedResponses = smartParseNoPlatSubdivisionIntermediary(Response, table, outputTable, viParseID, type);
							parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
							break;
							
					}
																
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }
	
				}catch(Exception e) {
					e.printStackTrace();
				}
				break;
				
			case ID_DETAILS:		    
			case ID_SAVE_TO_TSD :
				
				String accountId = StringUtils.extractParameter(rsResponse,"<input type=\"hidden\" name=\"account_id\" id=\"account_id\" value=\"(.*?)\">");
				String details = getDetails(rsResponse);						
				String filename = accountId + ".html";
				String instrumentNo = "", serverDocType = "", book ="", page = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					
					Span documentNumberNodeText = (Span) HtmlParser3.findNode(mainList,"DOCUMENT NUMBER (SERIAL NUMBER)").getParent();
					Span documentNumberNode = HtmlParser3.findNodeAfter(documentNumberNodeText.getParent().getChildren(),documentNumberNodeText , Span.class);
					instrumentNo = ro.cst.tsearch.servers.functions.AKGenericRO.cleanInstrumentNumber(documentNumberNode.getStringText());
					
					Span serverDocTypeNodeText = (Span) HtmlParser3.findNode(mainList,"DOCUMENT DESCRIPTION").getParent();
					Span serverDocTypeNode = HtmlParser3.findNodeAfter(serverDocTypeNodeText.getParent().getChildren(),serverDocTypeNodeText , Span.class);
					serverDocType = serverDocTypeNode.getStringText().replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", ""); 
					
					TableTag bookPageTable = HtmlParser3.getFirstParentTag(HtmlParser3.findNode(mainList, "NUMBER OF PAGES"), TableTag.class);
					book = ro.cst.tsearch.servers.functions.AKGenericMotznikRO.getNodeAfterAsString(bookPageTable.getChildren(),"BOOK",3);
					page = ro.cst.tsearch.servers.functions.AKGenericMotznikRO.getNodeAfterAsString(bookPageTable.getChildren(),"PAGE",3);
					
				}catch(Exception e) {
					e.printStackTrace();
				}
				
				if (viParseID==ID_DETAILS) {
					String originalLink = sAction + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type",serverDocType);
					data.put("book",book);
					data.put("page",page);
					
					//details = details.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1").replaceAll("<img[^>]*?>", "");
					
					if(isInstrumentSaved(instrumentNo, null, data)){
		                details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse( details );
					
				}  else {
					smartParseDetails(Response,details);
					
					details = details.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1").replaceAll("<img[^>]*?>", "");
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse( details );
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
					
				}
				break;
			case ID_GET_LINK :
    			if(sAction.contains("/distinct_names") || sAction.contains("/name")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
    			} else if(sAction.contains("/details")) {
    				ParseResponse(sAction, Response, ID_DETAILS);
    			} else if(sAction.contains("/distinct_document_number")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_INSTRUMENT_NO);
    			} else if(sAction.contains("/distinct_book_page")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_BOOK_AND_PAGE);
    			} else if(sAction.contains("/subdivision") || sAction.contains("/distinct_subdivisions") || sAction.contains("/legal")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_SUBDIVISION_NAME);
    			} else if(sAction.contains("/distinct_legals")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE50);
    			} else if(sAction.contains("/distinct_no_plat_subdivisions")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE53);
    			}
				break;
			default:
				break;
		}
	}


	private String getDetails(String response) {
		
		/* If from memory - use it as is */
		if(!response.contains("<HTML")){
			return response;
		}
		
		HtmlParser3 parser = new HtmlParser3(Tidy.tidyParse(response,null));
		
		String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
		try {
			NodeList linkList = HtmlParser3.getNodeListByType(parser.getNodeList(), "a", true);
		
			for (int i = 0; i < linkList.size(); i++) {
				LinkTag linkTag = (LinkTag) linkList.elementAt(i);
				String linkString = linkPrefix + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(linkString);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		String table1 = "";
		Node nodeTable1 = parser.getNodeById("Table1");
		if(nodeTable1 != null) {
			NodeList children = nodeTable1.getChildren();
			children.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("href")), true);
			table1 = nodeTable1.toHtml();
		}
		String table2 = parser.getNodeContentsById("Table2");
		
		
		return table1 + table2;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		return ro.cst.tsearch.servers.functions.AKGenericMotznikRO.parseAndFillResultMap(response,detailsHtml,map,searchId,this);
	}

	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			String imageLink = (String)parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			document = bridge.importData();
			
			if(imageLink != null) {
				getSearch().addImagesToDocument(document, imageLink + "&amp;fakeName=name.pdf");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.getParsedResponse().setOnlyResponse(detailsHtml.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1"));
        if(document!=null) {
        	response.getParsedResponse().setDocument(document);
		}
		
		return document;
	}
	
	public Collection<ParsedResponse> smartParseSubdivisionIntermediary(ServerResponse response, String table, StringBuilder outputTable, int viParseID, IntermediaryResultTypes type) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			
			table = Tidy.tidyParse(table,null);
			HtmlParser3 htmlParser = new HtmlParser3(table);
			
			if(type == IntermediaryResultTypes.SUBDIVISION_LEVEL_1) {
				TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table2");
				TableTag resultRowsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(resultsTable.getChildren(), "table", "width", "100%", true);
				
				Node firstRow = resultsTable.getRow(0);
				Node secondRow = resultsTable.getRow(1);
				
				resultsTable.getChildren().remove(firstRow);
				resultsTable.getChildren().remove(secondRow);
				
				TableRow[] rows = resultRowsTable.getRows();
				for(TableRow row : rows ) {
					if(row.getColumnCount() > 0) {
						processIntermediaryRow( intermediaryResponse, row , IntermediaryResultTypes.SUBDIVISION_LEVEL_1, response);
					}
				}
				
			String header0 = proccessLinks(response,(TableRow)firstRow);
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header0);
			response.getParsedResponse().setFooter("</table>");
	
			outputTable.append(resultsTable.toHtml());
		}else if(type == IntermediaryResultTypes.SUBDIVISION_LEVEL_2 || type == IntermediaryResultTypes.LEGAL_DESCRIPTION) {
			TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table1");
			
		
			TableTag resultRowsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(resultsTable.getChildren(), "table", "class", ".rcell", true);
			if(resultRowsTable==null) {
				Node doctypeTextNode = HtmlParser3.findNode(resultsTable.getChildren(), "TYPE OF DOCUMENT");
				resultRowsTable = HtmlParser3.getFirstParentTag(doctypeTextNode, TableTag.class);
			}
			
			TableRow[] rows = resultRowsTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					processIntermediaryRow( intermediaryResponse, row, type, response);					
				}
			}
			
			if(type == IntermediaryResultTypes.SUBDIVISION_LEVEL_2) {
				String header0 = proccessLinks(response,resultsTable.getRow(0));
				response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header0);
				response.getParsedResponse().setFooter("</table>");
			}else {
				addHeaderAndFooter(response, outputTable, viParseID, resultsTable, resultRowsTable);
			}
			outputTable.append(resultsTable.toHtml());
		}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	public Collection<ParsedResponse> smartParsePlatTractBlockLotIntermediary(ServerResponse response, String table, StringBuilder outputTable, int viParseID, IntermediaryResultTypes type) {

		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			
			table = Tidy.tidyParse(table,null);
			HtmlParser3 htmlParser = new HtmlParser3(table);
			
			if(type == IntermediaryResultTypes.DISTINCT_LEGALS) {
				TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table2");
				TableTag resultRowsTable = HtmlParser3.getFirstTag(resultsTable.getChildren() , TableTag.class, true);
				
				Node firstRow = resultsTable.getRow(0);
				Node headerRow = resultRowsTable.getRow(1);
				
				resultsTable.getChildren().remove(firstRow);
				
				TableRow[] rows = resultRowsTable.getRows();
				for(TableRow row : rows ) {
					if(row.getColumnCount() > 0) {
						processIntermediaryRow( intermediaryResponse, row , IntermediaryResultTypes.DISTINCT_LEGALS, response);
					}
				}
				
			String header0 = proccessLinks(response,(TableRow)firstRow);
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header0 + headerRow.toHtml());
			response.getParsedResponse().setFooter("</table>");
	
			outputTable.append(resultsTable.toHtml());
		}else if(type == IntermediaryResultTypes.LEGAL_DESCRIPTION) {
			TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table1");
			
		
			TableTag resultRowsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(resultsTable.getChildren(), "table", "class", ".rcell", true);
			if(resultRowsTable==null) {
				Node doctypeTextNode = HtmlParser3.findNode(resultsTable.getChildren(), "TYPE OF DOCUMENT");
				resultRowsTable = HtmlParser3.getFirstParentTag(doctypeTextNode, TableTag.class);
			}
			
			TableRow[] rows = resultRowsTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					processIntermediaryRow( intermediaryResponse, row, type, response);					
				}
			}
			
			addHeaderAndFooter(response, outputTable, viParseID, resultsTable, resultRowsTable);
			
			outputTable.append(resultsTable.toHtml());
		}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;		
	}
	
	public Collection<ParsedResponse> smartParseNoPlatSubdivisionIntermediary(ServerResponse response, String table, StringBuilder outputTable, int viParseID, IntermediaryResultTypes type) {

		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			
			table = Tidy.tidyParse(table,null);
			HtmlParser3 htmlParser = new HtmlParser3(table);
			
			if(type == IntermediaryResultTypes.NO_PLAT_SUBDIVISION_LEVEL_1) {
				TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table2");
				TableTag resultRowsTable = HtmlParser3.getFirstTag(resultsTable.getChildren() , TableTag.class, true);
				
				Node firstRow = resultsTable.getRow(0);
				Node headerRow = resultRowsTable.getRow(0);
				
				resultsTable.getChildren().remove(firstRow);
				
				TableRow[] rows = resultRowsTable.getRows();
				for(TableRow row : rows ) {
					if(row.getColumnCount() > 0) {
						processIntermediaryRow( intermediaryResponse, row , type, response);
					}
				}
				
			String header0 = proccessLinks(response,(TableRow)firstRow);
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header0 + headerRow.toHtml());
			response.getParsedResponse().setFooter("</table>");
	
			outputTable.append(resultsTable.toHtml());
		}else if(type == IntermediaryResultTypes.LEGAL_DESCRIPTION) {
			TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table1");
			
		
			TableTag resultRowsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(resultsTable.getChildren(), "table", "class", ".rcell", true);
			if(resultRowsTable==null) {
				Node doctypeTextNode = HtmlParser3.findNode(resultsTable.getChildren(), "TYPE OF DOCUMENT");
				resultRowsTable = HtmlParser3.getFirstParentTag(doctypeTextNode, TableTag.class);
			}
			
			TableRow[] rows = resultRowsTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					processIntermediaryRow( intermediaryResponse, row, type, response);					
				}
			}
			
			addHeaderAndFooter(response, outputTable, viParseID, resultsTable, resultRowsTable);
			
			outputTable.append(resultsTable.toHtml());
		}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;		
	}
	
	public Collection<ParsedResponse> smartParseNameIntermediary(ServerResponse response, String table, StringBuilder outputTable, int viParseID, IntermediaryResultTypes type) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			
			table = Tidy.tidyParse(table,null);
			HtmlParser3 htmlParser = new HtmlParser3(table);
			
			if(type == IntermediaryResultTypes.NAME_LEVEL_1) {
				TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table2");
				
				Node firstRow = resultsTable.getRow(0);
				Node secondRow = resultsTable.getRow(1);
				
				resultsTable.getChildren().remove(firstRow);
				resultsTable.getChildren().remove(secondRow);
				
				TableRow[] rows = resultsTable.getRows();
				for(TableRow row : rows ) {
					if(row.getColumnCount() > 0) {
						processIntermediaryRow( intermediaryResponse, row , type, response);
					}
				}
				
			String header0 = proccessLinks(response,(TableRow)firstRow);
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header0);
			response.getParsedResponse().setFooter("</table>");
	
			outputTable.append(resultsTable.toHtml());
		}else {
			TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table1");
			
		
			TableTag resultRowsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(resultsTable.getChildren(), "table", "class", ".rcell", true); 
			
			int numberOfUncheckedElements = 0;
			TableRow[] rows = resultRowsTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					boolean isAlreadySaved =  processIntermediaryRow( intermediaryResponse, row, type, response);
					if(!isAlreadySaved) {
						numberOfUncheckedElements ++;
					}					
				}
			}
			
			addHeaderAndFooter(response, outputTable, viParseID, resultsTable, resultRowsTable);
			outputTable.append(resultsTable.toHtml());
		}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	public void addHeaderAndFooter(ServerResponse response,
			StringBuilder outputTable, int viParseID, TableTag resultsTable,
			TableTag resultRowsTable) {
		String header = "", footer = "";

		if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");               	
		    header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		    header += proccessLinks(response,resultsTable.getRow(0));
		    header += "<tr><td>"+selectAll+"</td>"+resultRowsTable.getRow(0).getChildrenHTML() + "</tr>";
		        	
		    footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_DOCUMENT_BUTTON_LABEL, viParseID, -1);
		        	
		    response.getParsedResponse().setHeader(header);
		    response.getParsedResponse().setFooter(footer);
		}
	}

	public Collection<ParsedResponse> smartParseDocumentNumberIntermediary(ServerResponse response, String table, StringBuilder outputTable, int viParseID) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			
			table = Tidy.tidyParse(table,null);
			HtmlParser3 htmlParser = new HtmlParser3(table);
			
			TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table2");
			
			
			TableTag resultRowsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(resultsTable.getChildren(), "table", "border", "1", true); 
			
			TableRow[] rows = resultRowsTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					processIntermediaryRow( intermediaryResponse, row, IntermediaryResultTypes.DOCUMENT_NUMBER, response);
				}
			}
	
			addHeaderAndFooter(response, outputTable, viParseID, resultsTable, resultRowsTable);
			outputTable.append(resultsTable.toHtml());
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	public Collection<ParsedResponse> smartParseBookPageIntermediary(ServerResponse response, String table, StringBuilder outputTable, int viParseID) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			
			table = Tidy.tidyParse(table,null);
			HtmlParser3 htmlParser = new HtmlParser3(table);
			
			TableTag resultsTable = (TableTag)htmlParser.getNodeById("Table2");
			
			
			TableTag resultRowsTable = (TableTag) HtmlParser3.getNodeByTypeAndAttribute(resultsTable.getChildren(), "table", "border", "1", true); 
			
			TableRow[] rows = resultRowsTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					processIntermediaryRow( intermediaryResponse, row, IntermediaryResultTypes.BOOK_PAGE, response);
				}
			}
			
			addHeaderAndFooter(response, outputTable, viParseID, resultsTable, resultRowsTable);	
			outputTable.append(resultsTable.toHtml());
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	
	private boolean processIntermediaryRow( Vector<ParsedResponse> intermediaryResponse, TableRow row, IntermediaryResultTypes type, ServerResponse response) throws Exception {
		
		boolean isAlreadySaved = false;
		
		NodeList linkTags = HtmlParser3.getNodeListByType(row.getChildren(), "a",true);
		if(linkTags == null || linkTags.size()==0) {
			return true;
		}
		
		String link = "";
		for(Node linkNode : linkTags.toNodeArray()) {
			LinkTag linkTag = (LinkTag) linkNode;
			link = linkTag.getLink();
			link = CreatePartialLink(TSConnectionURL.idGET) + link;
			linkTag.setLink(link);
		}
				
		boolean doNotPutLink = false;
		/* Bug 5577 , Comment 7 */
		if(type == IntermediaryResultTypes.DISTINCT_LEGALS) {
			try {
				if(row.getColumnCount()>5) {
					TableColumn[] cols = row.getColumns();
					TableColumn countyCol = row.getColumns()[5];
					if(!countyCol.toPlainTextString().trim().equalsIgnoreCase("ANCHORAGE")) {
						for(TableColumn col : cols) {
							if(col.getChildCount()>0) {
								LinkTag linkTag = HtmlParser3.getFirstTag(col.getChildren(), LinkTag.class, true);
								if(linkTag!=null) {
									if(!linkTag.getLinkText().trim().isEmpty()) {
										col.getChildren().removeAll();
										col.getChildren().add(new TextNode(linkTag.getLinkText()));
									}
								}
							}	
						}
						doNotPutLink = true;
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}		 
		
		ResultMap m = parseIntermediaryRow(row,type);
		
		ParsedResponse currentResponse = new ParsedResponse();
		
		currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
		currentResponse.setParentSite(response.isParentSiteSearch());
		
		switch(type) {
			case NAME_LEVEL_1:
			case SUBDIVISION_LEVEL_1:
			case SUBDIVISION_LEVEL_2:
			case NO_PLAT_SUBDIVISION_LEVEL_1:
			case DISTINCT_LEGALS:
					currentResponse.setOnlyResponse(row.toHtml());
					if(!doNotPutLink) {
						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_GO_TO_LINK));	
					}
				break;
			
			default:
				String checkbox = "saved";
				HashMap<String, String> data = null;
				String serverDocType = (String)m.get("SaleDataSet.DocumentType");
				
				if (!StringUtils.isEmpty(serverDocType)) {
					data = new HashMap<String, String>();
					data.put("type", serverDocType);
					if(ro.cst.tsearch.utils.StringUtils.isNotEmpty((String)m.get("SaleDataSet.Book"))) {
						data.put("book", (String)m.get("SaleDataSet.Book"));	
						data.put("page", (String)m.get("SaleDataSet.Page"));
					}
				}
				if (isInstrumentSaved((String)m.get("SaleDataSet.InstrumentNumber"), null, data) ) {
					isAlreadySaved = true;
				} else {
					isAlreadySaved = false;
					checkbox = "<input type='checkbox' name='docLink' value='" + link + "'>";
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				}
				
				currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td>" + row.getChildrenHTML() + "</tr>");
				break;
				
			
		}
		
		
		Bridge bridge = new Bridge(currentResponse,m,searchId);
		
		DocumentI document = (RegisterDocumentI)bridge.importData();				
		currentResponse.setDocument(document);
		
		intermediaryResponse.add(currentResponse);
		return isAlreadySaved;
	}

	protected ResultMap parseIntermediaryRow(TableRow row, IntermediaryResultTypes type) {
		return ro.cst.tsearch.servers.functions.AKGenericMotznikRO.parseIntermediaryRow( row,type, searchId);
	}

	private String proccessLinks(ServerResponse response, TableRow row) {
		String nextLink = "", prevLink = "";
		String header0 = "";
		
		try {
			
			HtmlParser3 htmlParser = new HtmlParser3(response.getResult());
			NodeList nl = HtmlParser3.getNodeListByType(htmlParser.getNodeList(), "input", true);
			FormTag form = (FormTag) htmlParser.getNodeByAttribute("name", "Form1", true);
			String action = form.getAttribute("action");
			
			Map<String,String> parameters = new HashMap<String, String>();
			for(Node input : nl.toNodeArray()) {
				InputTag inputTag = (InputTag) input;
				parameters.put(inputTag.getAttribute("name"),
						URLEncoder.encode(StringUtils.prepareStringForHTML(inputTag.getAttribute("value")), "UTF-8"));
			}
			
			nextLink = CreatePartialLink(TSConnectionURL.idPOST) + action + "?";
			parameters.put("commit","Next");
			nextLink = StringUtils.addParametersToUrl(nextLink,parameters);
			
			prevLink = CreatePartialLink(TSConnectionURL.idPOST) + action + "?";
			parameters.put("commit","Prev");
			prevLink = StringUtils.addParametersToUrl(prevLink,parameters);
			
			
			response.getParsedResponse().setNextLink( "<a href='"+nextLink+"'>Next</a>" );
			
			boolean hasPrev = htmlParser.getNodeById("ButtonPrev")!=null;
			boolean hasNext = htmlParser.getNodeById("ButtonNext")!=null;
			
			header0 = "<tr><td colspan=13>" +
					  "<table width='100%'>"+
					  "<tr><td width='20%' align='center'>" + 
					  (hasPrev?"<a href='"+prevLink+"'>Prev</a>":"")+
					  "</td><td align='center'>" + 
					  htmlParser.getNodeContentsById("StartLabel") + 
					  "</td><td width='20%' align='center'>"+
					  (hasNext?"<a href='"+nextLink+"'>Next</a>":"")+ 
					  "</td></tr>"+
					  "</table>"+
					  "</td></tr>";
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return header0;
	}

	 public static String addHiddenHtmlInput(String name, String value ){
		 return  "<input type='hidden' name='" + name + "'"+ "value='" + 
			value  + "'" +"/>" ;
	 }
	 
	 
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
				
		TSServerInfoModule m = null;
		
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultSynonimNameInfoPinCandidateFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , m );
		((SynonimNameFilter)defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
		boolean lookUpWasDoneWithNames = true;
		
		InstrumentAKMotznikROIterator instrumentBPInterator = new InstrumentAKMotznikROIterator(searchId, true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
		m.clearSaKeys();
		//m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
		m.addIterator(instrumentBPInterator);
		if(!instrumentBPInterator.createDerrivations().isEmpty()) {
			m.addFilter(new GenericInstrumentFilter(searchId));
			l.add(m);
			lookUpWasDoneWithNames = false;
		}
		
		InstrumentAKMotznikROIterator instrumentNoInterator = new InstrumentAKMotznikROIterator(searchId);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
		m.clearSaKeys();
		m.addIterator(instrumentNoInterator);
		if(!instrumentNoInterator.createDerrivations().isEmpty()) {
			m.addFilter(new GenericInstrumentFilter(searchId));
			l.add(m);
			lookUpWasDoneWithNames = false;
		}
		
		GenericMultipleLegalFilter genericMultipleLegalFilter = new GenericMultipleLegalFilter(searchId);
		genericMultipleLegalFilter.setAdditionalInfoKey("AKMotznikRO_LOOK_UP_DATA");
		genericMultipleLegalFilter.setUseLegalFromSearchPage(true);
		DocsValidator genericMultipleLegalValidator = genericMultipleLegalFilter.getValidator();
		
		if (!isUpdate()) {
			
			//plat search
			m = new TSServerInfoModule(serverInfo.getModule(ID_SEARCH_BY_MODULE50));
			m.clearSaKeys();
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookUpWasDoneWithNames, false, getDataSite()) {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				protected void loadDerrivation(TSServerInfoModule module,
						LegalStruct str) {
					for (Object functionObject : module.getFunctionList()) {
						if (functionObject instanceof TSServerInfoFunction) {
							TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
							switch (function.getIteratorType()) {
							case FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE:
								function.setParamValue(str.getPlatBook());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE:
								function.setParamValue(str.getPlatPage());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_LOT:
								function.setParamValue(str.getLot());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_BLOCK:
								function.setParamValue(str.getBlock());
								break;
							}
						}
					}
				}
			};
			it.setAdditionalInfoKey("AKMotznikRO_LOOK_UP_DATA");
			it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
			it.setEnableTownshipLegal(false);
			it.setEnableSubdivision(false);
			it.setRoDoctypesToLoad(new String[]{"MORTGAGE", "TRANSFER", "RELEASE"});
			m.addIterator(it);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_LOT);
			m.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
			
			GenericMultipleLegalFilter multipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			multipleLegalFilter.setAdditionalInfoKey("AKMotznikRO_LOOK_UP_DATA");
			multipleLegalFilter.setUseLegalFromSearchPage(true);
			multipleLegalFilter.setFilterForNextFollowLinkLimit(2);
			multipleLegalFilter.disableAll();
			multipleLegalFilter.setEnablePlatBook(true);
			multipleLegalFilter.setEnablePlatPage(true);
			m.addFilterForNext(multipleLegalFilter );
			m.addFilter(genericMultipleLegalFilter);
			//APN/PIN = 00503465000 - need extra validation for this case
			m.addValidator(genericMultipleLegalValidator);
			m.addValidator(rejectSavedDocuments.getValidator());
			m.addCrossRefValidator(genericMultipleLegalValidator);
			l.add(m);
		}
		
		// search by name
		ConfigurableNameIterator nameIterator = null;
		if(hasOwner()) {
			//name modules with names from search page.
	    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			
			m.addFilter(defaultNameFilter);
			addBetweenDateTest(m, true, true, false);
			addFilterForUpdate(m, true);
			m.addValidator(genericMultipleLegalValidator);
			m.addValidator(new LastTransferDateFilter(searchId).getValidator());
			m.addValidator(defaultNameFilter.getValidator());
			m.addValidator(rejectSavedDocuments.getValidator());
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;" });
			nameIterator.setInitAgain(true);		//initialize again after all parameters are set
			m.addIterator(nameIterator);
			l.add(m);
		}
		
		
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
	    m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
	    m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
	    //m.addFilter(genericMultipleLegalFilter);
	    m.addValidator(genericMultipleLegalValidator);
	    m.addCrossRefValidator(genericMultipleLegalValidator);
		l.add(m);
		
	    // OCR last transfer - instrument search
	    m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
	    m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
	    m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    
	    m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    //m.addFilter(genericMultipleLegalFilter);
	    m.addValidator(genericMultipleLegalValidator);
	    m.addCrossRefValidator(genericMultipleLegalValidator);
	    l.add(m);
	    
	    
	    //name modules with extra names from search page (for example added by OCR)
    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();
		m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		m.addFilter(defaultNameFilter);
		addBetweenDateTest(m, true, true, false);
		m.addValidator(genericMultipleLegalValidator);
		m.addValidator(new LastTransferDateFilter(searchId).getValidator());
		m.addValidator(defaultNameFilter.getValidator());
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
		ArrayList<NameI> searchedNames = null;
		if (nameIterator != null) {
			searchedNames = nameIterator.getSearchedNames();
		} else {
			searchedNames = new ArrayList<NameI>();
		}
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;" });
		// get your values at runtime
		nameIterator.setInitAgain(true);
		nameIterator.setSearchedNames(searchedNames);
		m.addIterator(nameIterator);
		l.add(m);
				
		//done in order to search for book-page that was not saved from TS
		InstrumentAKMotznikROIterator instrumentBPROInterator = new InstrumentAKMotznikROIterator(searchId, true);
		instrumentBPROInterator.setLoadFromRoLike(true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		//m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.clearSaKeys();
		//m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
		m.addIterator(instrumentBPROInterator);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addValidator(genericMultipleLegalValidator);
		l.add(m);
		
		//done in order to search for instrumentNo that was not saved from TS
		InstrumentAKMotznikROIterator instrumentNoROInterator = new InstrumentAKMotznikROIterator(searchId);
		instrumentNoROInterator.setLoadFromRoLike(true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		//m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.clearSaKeys();
		m.addIterator(instrumentNoROInterator);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addValidator(genericMultipleLegalValidator);
		l.add(m);
		
		serverInfo.setModulesForAutoSearch(l);
	
		
	}	
	
	protected ServerResponse FollowLink(String sLink, String imagePath)
			throws ServerResponseException {

		String sAction = GetRequestSettings(false, sLink);

		return performRequest(sAction, miGetLinkActionType, "FollowLink",
				ID_GET_LINK, imagePath, null, null);
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
  	  return isInstrumentSaved(instrumentNo, documentToCheck, data, false);
    }
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
	@Override
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null)
    				return true;
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType("MISCELLANEOUS");
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
	
	@Override
	protected ServerResponse SearchBy(boolean bResetQuery,
			TSServerInfoModule module, Object sd)
			throws ServerResponseException {
		if(!isParentSite() && ("true".equalsIgnoreCase(getSearchAttribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND)))	){
			return new ServerResponse();
		}
		return super.SearchBy(bResetQuery, module, sd);
	}
	
	@Override
	protected void setCertificationDate() {
		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				String page = getLinkContents(dataSite.getLink() + (dataSite.getLink().endsWith("/") ? "" : "/") + "menu");
					
				if (StringUtils.isNotEmpty(page)){
					Matcher certDateMatcher = certDatePattern.matcher(page);
					
					if(certDateMatcher.find()) {
						String date = certDateMatcher.group(1).trim();
						date = DateFormatUtils.format(Util.dateParser3(date), "MM/dd/yyyy");
						
						CertificationDateManager.cacheCertificationDate(dataSite, date);
						getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
					} else {
						CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
					}
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}
}
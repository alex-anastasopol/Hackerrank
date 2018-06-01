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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
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
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SynonimNameFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentAKROIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class AKGenericRO extends TSServer implements TSServerROLikeI{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Used to map each site in ATS to a district option
	 */
	public static final Map<Integer, String> DISTRICT_MAP = new HashMap<Integer, String>(){
		private static final long serialVersionUID = 1L;
		{
			put(316704, "301");
		}
	};
	
	public static final String ATS_PARSED_INSTRUMENT_ID = "ats_parsed_instrument_id";
	private static final Pattern certDatePattern = Pattern.compile("(?is)Last updated on	(\\d{1,2}/\\d{1,2}/\\d{4})");

	public AKGenericRO(long searchId) {
    	super(searchId);
        resultType = MULTIPLE_RESULT_TYPE;
    }
	
	public AKGenericRO(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
		
	@SuppressWarnings("rawtypes")
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
    	String district = DISTRICT_MAP.get(miServerID);
    	Map allModules = msiServerInfoDefault.getAllModules();
    	for (Object object : allModules.values()) {
			if(object instanceof TSServerInfoModule) {
				TSServerInfoModule module = (TSServerInfoModule)object;
				if("District".equals(module.getFunction(0).getParamName())) {
					if(!module.getFunction(0).getDefaultValue().matches("\\d+")) {
						module.forceValue(0, district);
					}
				}
			}
		}
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponce = Response.getResult();
    	ParsedResponse parsedResponse = Response.getParsedResponse();
    	
    	if(rsResponce.contains("Sorry, we are unable to complete your request. Please try again later.")) {
    		parsedResponse.setError("<font color=\"red\">Official Site Says: </font> Sorry, we are unable to complete your request. Please try again later.");
    		Response.setError(parsedResponse.getError());
			return;
    	}
    	
    	switch(viParseID){
    		case ID_SEARCH_BY_PARCEL:
    		case ID_SEARCH_BY_NAME:
	    		{
	    			StringBuilder outputTable = new StringBuilder();
					boolean documentsByAName = rsResponce.contains("Documents by Name");
					
					Collection<ParsedResponse> smartParsedResponses = smartParseNameIntermediary(Response,
							rsResponce, outputTable, documentsByAName);
					if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						//parsedResponse.setOnlyResponse(outputTable.toString());
						
			            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			            	String header = parsedResponse.getHeader();
			               	String footer = parsedResponse.getFooter();         
			               	if(documentsByAName) {
			               		header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
			               	}
			            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
			            	if(documentsByAName) {
			            		header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td><td>Document Information</td><tr>";
			            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
				            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
				            				viParseID, (Integer)numberOfUnsavedDocument);
				            	} else {
				            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
				            	}
			            	} else {
			            		footer = "\n</table>";
			            	}
			            	footer = outputTable.toString() + footer;
			            	
			            	parsedResponse.setHeader(header);
			            	parsedResponse.setFooter(footer);
			            } else {
			            	parsedResponse.setOnlyResponse(rsResponce);
			            	parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
			            }
						
						
					} else {
						parsedResponse.setError("<font color=\"red\">No results found</font>");
						return;
					}
	    		}
    			break;
    		case ID_SEARCH_BY_INSTRUMENT_NO:
    		{
    			StringBuilder outputTable = new StringBuilder();
				
				
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParseDocumentIntermediary(Response, rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					//parsedResponse.setOnlyResponse(outputTable.toString());
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		               	
		               	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		               	
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	
		            	header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td>" +
		            			"<td>Document Number</td><td>Date Recorded</td>" +
		            			"<td>Book</td><td>Page</td><td>Index</td><td>Document Desc.</td><td>Status</td><tr>";
	            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	
		            	
		            	footer = outputTable.toString() + footer;
		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
					
					
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_MODULE53:
    		{
    			StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO
						.smartParseDateIntermediary(Response, rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		               	
		               	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		               	
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	
		            	header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td>" + "<td>Document Information</td><tr>";
	            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_MODULE52:
    		{
    			StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParseAssociatedIntermediary(Response, rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		               	
		               	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		               	
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	
		            	header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td>" + "<td>Document Number</td><td>Date Recorded</td><td>Doc. Index</td><td>Document Description</td><td>Status</td><tr>";
	            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_MODULE51:
    		{
    			StringBuilder outputTable = new StringBuilder();
				
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParsePlatNumberIntermediary(Response, rsResponce, outputTable, this);
					
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
		            		"<tr><td>Plat</td><td>Lot</td><td>Block</td><td>Tract</td><td>Apt/Unit</td></tr>\n";
		            	footer = "\n</table>";
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            } else {
		            	parsedResponse.setOnlyResponse(rsResponce);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_MODULE50:
    		{
    			StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.smartParsePlatListIntermediary(Response,
						rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		               	
		               	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		               	
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	
		            	String lastColumn = "Additional Legal";
		            	if(sAction.contains("/NoPlatSubDivDocs.cfm")) {
		            		lastColumn = "Additional Info.";
		            	}
		            	
		            	header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td>" + 
		            		"<td>Document Desc.</td><td>Date Recorded</td><td>Index</td><td>Book</td><td>Page</td><td>" + lastColumn + "</td><tr>";
	            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            } else {
		            	parsedResponse.setOnlyResponse(rsResponce);
		            	parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_SECTION_LAND:
    		{
    			StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.smartParseMTRSListIntermediary(Response,
						rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		               	
		               	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		               	
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	
		            	header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td>" + 
		            		"<td>Date Recorded</td><td>Document Number</td><td>Index</td>" +
		            		"<td>Qtr/Qtr</td><td>Lot</td><td>Block</td>" +
		            		"<td>Tract</td><td>Additional Legal</td><tr>";
	            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_SURVEYS:
    		{
    			StringBuilder outputTable = new StringBuilder();
				
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParseSurveyIntermediary(Response, rsResponce, outputTable, this);
					
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
		            		"<tr><td>Survey</td><td>Lot</td><td>Block</td><td>Tract</td><td>Apt/Unit</td><td>Plat</td></tr>\n";
		            	footer = "\n</table>";
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_SUBDIVISION_NAME:
    		{
    			StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParseSubdivisionIntermediary(Response, rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
		            		"<tr><td>Subdivision Names</td><td>Associated Plat</td></tr>\n";
		            	footer = "\n</table>";
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_SUBDIVISION_PLAT:
    		{
    			StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParseSubdivisionPlatIntermediary(Response, rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
		            		"<tr><td>Subdivision without Plat</td><td>Lot</td><td>Block</td><td>Tract</td><td>Apt/Unit</td><td>Survey</td><td>Additional</td></tr>\n";
		            	footer = "\n</table>";
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_MODULE49:
    		{
    			StringBuilder outputTable = new StringBuilder();
				
				
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParseDocumentTypeIntermediary(Response, rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		               	
		               	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		               	
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	
		            	header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td>" +
		            			"<td>Document Number</td><td>Date Recorded</td>" +
		            			"<td>Book</td><td>Page</td><td>Document Desc.</td><td>Status</td><tr>";
	            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_SEARCH_BY_MODULE47:
    		{
    			StringBuilder outputTable = new StringBuilder();
				
				
				Collection<ParsedResponse> smartParsedResponses = 
					ro.cst.tsearch.servers.functions.AKGenericRO.
						smartParseUCCDocumentTypeIntermediary(Response, rsResponce, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();         
		               	
		               	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		               	
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	
		            	header += "<tr><td>" + SELECT_ALL_CHECKBOXES + "</td>" +
		            			"<td>Document Number</td><td>Date Recorded</td>" +
		            			"<td>Document Desc.</td><td>Status</td><tr>";
	            		Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	footer = outputTable.toString() + footer;
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} else {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}
    		}
    			break;
    		case ID_GET_IMAGE:
    			if(rsResponce.contains("Image is currently unavailable")) {
    				parsedResponse.setError("<font color=\"red\">Image is currently unavailable.</font> Please try again.");
    				parsedResponse.setFooter("");
    			}
				return;
    			
    		case ID_DETAILS:
    		case ID_SAVE_TO_TSD:
    			String contents = getDetailedContent(rsResponce, sAction, Response.getRawQuerry());
    			if(contents == null) {
		    		parsedResponse.setError("<font color=\"red\">Could not parse document.</font>  Please search again.");
					return;
    			}
    			contents = "<table border=\"0\" align=\"center\" cellspacing=\"0\" cellpadding=\"0\">" +					  
			           contents +
			           "</table>"; 
			           
			           
			           
    			parsedResponse.setResponse(contents);
    			String keyCode = getInstrumentFromLink(sAction + "&");
    			
    			if(viParseID == ID_SAVE_TO_TSD) {
    				msSaveToTSDFileName = keyCode + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = "<form>" + contents + CreateFileAlreadyInTSD();                
	                smartParseDetails(Response,contents, false);
	                
	                
    			} else {
    				String originalLink = sAction + "&dummy=" + keyCode + "&" + "shortened=true"; //Response.getQuerry();
                    String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                    
                    HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type",ro.cst.tsearch.utils.StringUtils.extractParameter(
    						contents, 
    						">\\s*<i>Desc:</i>\\s+([^<]+)<").trim().replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", ""));
    				data.put("book",
    							ro.cst.tsearch.utils.StringUtils.extractParameter(
    									contents, 
    									"<i>Book:\\s*</i>\\s+([\\d]+)")
    									.trim().replaceAll("\\s+", ""));
    				data.put("page",
							ro.cst.tsearch.utils.StringUtils.extractParameter(
									contents, 
									"<i>Page:\\s*</i>\\s+([\\d]+)")
									.trim().replaceAll("\\s+", ""));
    				
    				if(isInstrumentSaved(
    						ro.cst.tsearch.servers.functions.AKGenericRO.cleanInstrumentNumber(keyCode), 
    						null, 
    						data)){
    					contents += CreateFileAlreadyInTSD();
    				} else {
    					mSearch.addInMemoryDoc(sSave2TSDLink, contents/*.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1")*/);
    					contents = addSaveToTsdButton(contents, sSave2TSDLink, viParseID);
    				}
                    
                    Response.getParsedResponse().setResponse(contents);
                    Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
    			}
    			
    			break;
    			
    		case ID_GET_LINK:
    			System.out.println("sAction: " + sAction);
    			if(sAction.contains("/NameDocs.cfm") || sAction.contains("/NameSearch.cfm") || sAction.contains("/NameSearchUCC.cfm")
    						|| sAction.contains("NameDocsUCC.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
    			} else if(sAction.contains("NumberDocs.cfm") || sAction.contains("/BookandPageDocs.cfm") || sAction.contains("NumberDocsUCC.cfm") ) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_INSTRUMENT_NO);
    			} else if(sAction.contains("DocDisplay.cfm") || sAction.contains("DocDisplayUCC.cfm")) {
    				ParseResponse(sAction, Response, ID_DETAILS);
    			} else if(sAction.contains("DateDocs.cfm") || sAction.contains("DateDocsUCC.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE53);
    			} else if(sAction.contains("AssocDocs.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE52);
    			} else if(sAction.contains("/PlatSearch.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE51);
    			} else if(sAction.contains("/PlatDocs.cfm") || sAction.contains("/SurveyDocs.cfm") || sAction.contains("/NoPlatSubDivDocs.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE50);
    			} else if(sAction.contains("/MTRSDocs.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_SECTION_LAND);
    			} else if(sAction.contains("/SubDivisionSearch.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_SUBDIVISION_NAME);
    			} else if(sAction.contains("/SurveySearch.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_SURVEYS);
    			} else if(sAction.contains("/NoPlatSubDivSearch.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_SUBDIVISION_PLAT);
    			} else if(sAction.contains("/IndexDocs.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE49);
    			} else if(sAction.contains("/IndexDocsUCC.cfm")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE47);
    			}
    			break;
    			
    	}
	}
	
	
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			String imageLink = (String)parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			document = bridge.importData();
			
			if(imageLink != null) {
				getSearch().addImagesToDocument(document, imageLink);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.getParsedResponse().setOnlyResponse(detailsHtml.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1").replace("View Image", ""));
        if(document!=null) {
        	response.getParsedResponse().setDocument(document);
		}
		
		return document;
	}

	public Collection<ParsedResponse> smartParseNameIntermediary(
			ServerResponse serverResponse, String table, StringBuilder outputTable, boolean documentsByAName) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
		try {
			Parser parser = Parser.createParser(table, null);
			NodeList nodeList = parser.parse(null);
			
			if(!documentsByAName) {
				NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "5"))
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
					.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"));
				
				if(tableList.size() < 1) {
					return null;
				}
				
				TableRow[] trList = null;
				TableTag resultTable = (TableTag)tableList.elementAt(0);
				trList = resultTable.getRows();
				if(trList == null || trList.length == 0) {
					return null;
				}
				
				//outputTable.append("<table BORDER='1' CELLPADDING='2'>\n");
				String rowAsString = null;
				
				for (int i = 1; i < trList.length; i++) {
					
					ParsedResponse currentResponse = new ParsedResponse();
					//currentResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
					ResultMap resultMap = new ResultMap();
					resultMap.put("OtherInformationSet.SrcType","RO");
					TableRow row = trList[i];
					
					NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					LinkTag linkTag = (LinkTag) linkList.elementAt(0);
					String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
					
					List<String> grantorList = new ArrayList<String>();
					List<String> granteeList = new ArrayList<String>();
					grantorList.add(linkTag.toPlainTextString().trim());
					ro.cst.tsearch.servers.functions.AKGenericRO.parseNames(resultMap, grantorList, granteeList, searchId);
					
					Bridge b = new Bridge(currentResponse, resultMap, searchId);
			        b.mergeInformation();
					
					linkTag.setLink(linkString);
					rowAsString = row.toHtml();
					
					
					
					currentResponse.setParentSite(serverResponse.isParentSiteSearch());
					currentResponse.setOnlyResponse(rowAsString);
					LinkInPage linkInPage = new LinkInPage(
							linkString, 
							linkString, 
	    					TSServer.REQUEST_GO_TO_LINK);
					currentResponse.setPageLink(linkInPage);
					
					responses.add(currentResponse);
					//outputTable.append(rowAsString);
					
				}
				
				
				NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "Submit"))
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "Next"));
				if(nextList.size() > 0) {
					NodeList districtList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"))
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "District"));
					NodeList nameList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("type", "Hidden"))
						.extractAllNodesThatMatch(new HasAttributeFilter("name", "StartingName"));
					if(districtList.size() > 0 && nameList.size() > 0) {
						NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "myFORM"));
						String destinationPage = "NameSearch.cfm";
						if(formList.size() == 1) {
							destinationPage = ((FormTag)formList.elementAt(0)).getAttribute("action");
						}
						
						String linkString = CreatePartialLink(TSConnectionURL.idPOST) + 
							"/ssd/recoff/sag/" + destinationPage + "?Next=More+Documents" + 
							"&District=" + URLEncoder.encode(((InputTag)districtList.elementAt(0)).getAttribute("value"), "UTF-8") + 
							"&StartingName=" + URLEncoder.encode(((InputTag)nameList.elementAt(0)).getAttribute("value"), "UTF-8") ;
						outputTable.append("<tr><td align=\"center\"><a href=\"").append(linkString).append("\">More Results</a></td></tr>");
						serverResponse.getParsedResponse().setNextLink("<a href='" + linkString + "' />");
					} 
				}
			} else {
				NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"))
					.extractAllNodesThatMatch(new HasAttributeFilter("align"));
				
				if(tableList.size() < 1) {
					return null;
				}
				
				TableRow[] trList = null;
				TableTag resultTable = (TableTag)tableList.elementAt(0);
				trList = resultTable.getRows();
				if(trList == null || trList.length == 0) {
					return null;
				}
				
				//outputTable.append("<table BORDER='1' CELLPADDING='2' width='100%'>\n");
				
				int numberOfUncheckedElements = 0;
				for (int i = 0; i < trList.length; i++) {
					String serverDocType = null;
					ParsedResponse currentResponse = new ParsedResponse();
					//currentResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
					ResultMap resultMap = new ResultMap();
					LinkInPage linkInPage = null;
					StringBuilder innerTable = new StringBuilder("<table BORDER='1' CELLPADDING='2' width='100%'>\n");
					String checkbox = "checked";
					String documentNumber = null;
					for (int j = i; j < trList.length; j++, i++) {
						TableRow row = trList[i];
						if( row.getAttribute("bgcolor") == null)  {
							break;
						}
						NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if(linkList.size() > 0) {
							LinkTag linkTag = (LinkTag) linkList.elementAt(0);
							String linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
							linkTag.setLink(linkString);
							linkInPage = new LinkInPage(
									linkString, 
									linkString, 
			    					TSServer.REQUEST_SAVE_TO_TSD);
							
							serverDocType = linkTag.toPlainTextString().trim().replaceAll("(.*)[\\$\\s]+[\\d,\\.]+\\s*$", "$1").replaceAll("\\s+", "");
							resultMap.put("SaleDataSet.DocumentType", serverDocType);
		    				resultMap.put("OtherInformationSet.SrcType", "RO");
						} else {
							for (TableColumn column : row.getColumns()) {
								String columnPlainText = column.toPlainTextString().trim();
								if(columnPlainText.startsWith("Date Recorded:")) {
									resultMap.put("SaleDataSet.RecordedDate", columnPlainText.replace("Date Recorded:", "").trim());
								} else if(columnPlainText.startsWith("Doc.Number:")) {
									documentNumber = ro.cst.tsearch.servers.functions.AKGenericRO.
										cleanInstrumentNumber(columnPlainText.replace("Doc.Number:", ""));
									resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
								} else if(columnPlainText.startsWith("Book:")) {
									Pattern BOOK_PAGE = Pattern.compile("Book:\\s*([0-9]+)\\s*Page:\\s*([0-9]+)");
									Matcher bookPageMatcher = BOOK_PAGE.matcher(columnPlainText);
									if(bookPageMatcher.find()) {
										resultMap.put("SaleDataSet.Book", bookPageMatcher.group(1));
										resultMap.put("SaleDataSet.Page", bookPageMatcher.group(2));
									}
								}
							}
						}
						innerTable.append(row.toHtml());
					}
					
					HashMap<String, String> data = null;
					if (!StringUtils.isEmpty(serverDocType)) {
						data = new HashMap<String, String>();
						data.put("type", serverDocType);
						if(ro.cst.tsearch.utils.StringUtils.isNotEmpty((String)resultMap.get("SaleDataSet.Book"))) {
							data.put("book", (String)resultMap.get("SaleDataSet.Book"));
							data.put("page", (String)resultMap.get("SaleDataSet.Page"));
						}
					}
					if (isInstrumentSaved((String)resultMap.get("SaleDataSet.InstrumentNumber"), null, data) ) {
		    			checkbox = "saved";
		    		} else {
		    			numberOfUncheckedElements++;
		    			checkbox = "<input type='checkbox' name='docLink' value='" + linkInPage.getLink() + "'>";
            			currentResponse.setPageLink(linkInPage);
		    		}
					
					innerTable.append("</table>\n");
					
					currentResponse.setParentSite(serverResponse.isParentSiteSearch());
					
					Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
					RegisterDocumentI document = (RegisterDocumentI)bridge.importData();
					
					currentResponse.setDocument(document);
					
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, innerTable.toString());
					
					
					currentResponse.setOnlyResponse("<tr><td>"  +  checkbox + "</td><td>" + innerTable.toString() + "</td></tr>");
					
					responses.add(currentResponse);
					//outputTable.append(innerTable.toString());
					
				}
				//outputTable.append("</table>\n");
				
				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			}
		} catch (Exception e) {
			logger.error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
	}
	
	private String getDetailedContent(String rsResponce, String sAction,
			String rawQuerry) {
		
		if(!rsResponce.contains("<!DOCTYPE")) {
			return rsResponce;
		}
		
		String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
		StringBuilder detailedContent = new StringBuilder();
		try {
			rsResponce = Tidy.tidyParse(rsResponce, null);
			Parser parser = Parser.createParser(rsResponce, null);
			NodeList nodeList = parser.parse(null);

			NodeList divCenterList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "CENTER"));
			
			if(divCenterList.size() < 1) {
				logger.error("ro.cst.tsearch.servers.types.AKGenericRO.getDetailedContent(String, String, String):" +
					" Could not find divCenter with Selected Document");
				return null;
			}
			NodeList trList = divCenterList.extractAllNodesThatMatch(new TagNameFilter("tr"), true);
			if(trList.size() < 0) {
				logger.error("ro.cst.tsearch.servers.types.AKGenericRO.getDetailedContent(String, String, String):" +
						" Could not find row with Selected Document");
				return null;
			}
			TableRow selectedDocument = (TableRow) trList.elementAt(0);
			boolean added = false;
			try {
				NodeList imageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"))
					.extractAllNodesThatMatch(
							new OrFilter(new NodeFilter[] {
									new HasAttributeFilter("name", "docPath"), 
									new HasAttributeFilter("name", "platPath"),
									new HasAttributeFilter("name", "fcPath")
									
									}
							)
							);
				if(imageList.size() > 0) {
					
					InputTag inputTag = (InputTag) imageList.elementAt(0);
					
					String imagePrefix = "/docimages/";
					if("platPath".equals(inputTag.getAttribute("name"))) {
						imagePrefix = "/recorded-plats/";
					} else if("fcPath".equals(inputTag.getAttribute("name"))) {
						imagePrefix = "/filmimages/";
					}
					
					String imageLink = linkPrefix + imagePrefix + inputTag.getAttribute("value").replace("\\", "/");
					TableColumn column = selectedDocument.getColumns()[1];
					detailedContent.append("<tr><td id=\"" + ATS_PARSED_INSTRUMENT_ID + "\">Selected Document: " + column.toPlainTextString().trim() + "</td><td><a href=\"" + imageLink + "\" target=\"_blank\">View Image</a></td><tr>");
					added = true;
				}
			} catch (Exception e) {
				logger.error("Error while tring to get Image Link for " + sAction, e);
			}
			if(!added) {
				detailedContent.append("<tr><td colspan=\"2\" id=\"" + ATS_PARSED_INSTRUMENT_ID + "\">" + selectedDocument.toPlainTextString().trim() + "</td><tr>");
			}
			
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"))
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"));
			
			if(mainTableList.size() < 0) {
				logger.error("ro.cst.tsearch.servers.types.AKGenericRO.getDetailedContent(String, String, String):" +
						" Could not find MAIN TABLE");
				return null;
			}
			
			NodeList linkList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			for (int i = 0; i < linkList.size(); i++) {
				LinkTag linkTag = (LinkTag) linkList.elementAt(i);
				String linkString = null;
				if(linkTag.extractLink().startsWith("/")) {
					linkString = linkPrefix + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				} else {
					linkString = linkPrefix + "/ssd/recoff/sag/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				}
				
				linkTag.setLink(linkString);
			}
			NodeList children = mainTableList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"), true);
			Node nodeToRemove = null;
			for (int i = 0; i < children.size(); i++) {
				if(ro.cst.tsearch.utils.StringUtils.isEmpty(children.elementAt(i).toPlainTextString().trim())) {
					nodeToRemove = children.elementAt(i);
					break;
				}
			}
			if(nodeToRemove != null) {
				mainTableList.elementAt(0).getChildren().remove(nodeToRemove);
			}
			detailedContent.append("<tr><td colspan=\"2\">").append(mainTableList.toHtml()).append("</td></tr>");
			
			
		} catch (Exception e) {
			logger.error("Error while getDetailedContent!" , e);
		}
		return detailedContent.toString();
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		try {
			return ro.cst.tsearch.servers.functions.AKGenericRO.
				parseDetailedData(response, detailsHtml, resultMap, this);
		} catch (Exception e) {
			logger.error("Error while parsing document", e);
		}
		return null;
	}
	
	public String getInstrumentFromLink(String link) {
		if(link != null) {
			return StringUtils.substringBetween(link, "SelectedDoc=", "&");
		}
		return null;
	}
	
	
	
	@Override
    public String getContinueForm(String p1, String p2, long searchId) {
    	
    	long userId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue();
    	
    	String form = "<br>\n<form name=\"formContinue\" "        	
        	+ "action=\"/title-search/jsp/newtsdi/tsdindexpage.jsp?searchId="+searchId+"&userId=" +  userId + "\""
        	+ " method=\"POST\">\n"
        	+ "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Continue\">\n"
            + "</form>";
		return form;
    }

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		
		String district = DISTRICT_MAP.get(miServerID);
		
		//Search search = getSearch();
		TSServerInfoModule m = null;
		
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultSynonimNameInfoPinCandidateFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , m );
		((SynonimNameFilter)defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		boolean lookUpWasDoneWithNames = true;
		
		InstrumentAKROIterator instrumentBPInterator = new InstrumentAKROIterator(searchId, true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
		m.clearSaKeys();
		m.forceValue(0, district);
		//m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
		m.addIterator(instrumentBPInterator);
		if(!instrumentBPInterator.createDerrivations().isEmpty()) {
			m.addFilter(new GenericInstrumentFilter(searchId));
			l.add(m);
			lookUpWasDoneWithNames = false;
		}
		
		InstrumentAKROIterator instrumentNoInterator = new InstrumentAKROIterator(searchId);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
		m.clearSaKeys();
		m.forceValue(0, district);
		m.addIterator(instrumentNoInterator);
		if(!instrumentNoInterator.createDerrivations().isEmpty()) {
			m.addFilter(new GenericInstrumentFilter(searchId));
			l.add(m);
			lookUpWasDoneWithNames = false;
		}
		
		GenericMultipleLegalFilter genericMultipleLegalFilter = new GenericMultipleLegalFilter(searchId);
		genericMultipleLegalFilter.setAdditionalInfoKey("AKRO_LOOK_UP_DATA");
		genericMultipleLegalFilter.setUseLegalFromSearchPage(true);
		DocsValidator genericMultipleLegalValidator = genericMultipleLegalFilter.getValidator();
		
		if (!isUpdate()) {
			
			//plat search
			m = new TSServerInfoModule(serverInfo.getModule(ID_SEARCH_BY_MODULE51));
			m.clearSaKeys();
			m.forceValue(0, district);
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookUpWasDoneWithNames, false, getDataSite());
			it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
			it.setAdditionalInfoKey("AKRO_LOOK_UP_DATA");
			it.setEnableTownshipLegal(false);
			it.setEnableSubdivision(false);
			it.setRoDoctypesToLoad(new String[]{"MORTGAGE", "TRANSFER", "RELEASE"});
			m.addIterator(it);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LOT);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
			
			GenericMultipleLegalFilter multipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			multipleLegalFilter.setAdditionalInfoKey("AKRO_LOOK_UP_DATA");
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
			m.forceValue(0, district);
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
	    m.forceValue(0, district);
	    m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
	    m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
	    
	    m.addFilter(new GenericInstrumentFilter(searchId));
	    m.addValidator(genericMultipleLegalValidator);
	    m.addCrossRefValidator(genericMultipleLegalValidator);
		l.add(m);
		
	    // OCR last transfer - instrument search
	    m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
	    m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
	    m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    
	    m.forceValue(0, district);
	    m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    
	    m.addFilter(new GenericInstrumentFilter(searchId));
	    m.addValidator(genericMultipleLegalValidator);
	    m.addCrossRefValidator(genericMultipleLegalValidator);
	    l.add(m);
	    
	    //name modules with extra names from search page (for example added by OCR)
    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();
		m.forceValue(0, district);
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
		InstrumentAKROIterator instrumentBPROInterator = new InstrumentAKROIterator(searchId, true);
		instrumentBPROInterator.setLoadFromRoLike(true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		//m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.clearSaKeys();
		m.forceValue(0, district);
		//m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
		m.addIterator(instrumentBPROInterator);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addValidator(genericMultipleLegalValidator);
		l.add(m);
		
		//done in order to search for instrumentNo that was not saved from TS
		InstrumentAKROIterator instrumentNoROInterator = new InstrumentAKROIterator(searchId);
		instrumentNoROInterator.setLoadFromRoLike(true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		//m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.clearSaKeys();
		m.forceValue(0, district);
		m.addIterator(instrumentNoROInterator);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addValidator(genericMultipleLegalValidator);
		l.add(m);
		
		
		m = new TSServerInfoModule(serverInfo.getModule(ID_SEARCH_BY_MODULE28));
    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
    	m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();
		m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		
		m.addFilter(defaultNameFilter);
		addBetweenDateTest(m, true, true, false);
		addFilterForUpdate(m, true);
		m.addValidator(new LastTransferDateFilter(searchId).getValidator());
		m.addValidator(defaultNameFilter.getValidator());
		m.addValidator(DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ).getValidator());
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
		ConfigurableNameIterator nameUCCIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;" });
		nameUCCIterator.setInitAgain(true);		//initialize again after all parameters are set
		m.addIterator(nameUCCIterator);
		
		l.add(m);
		
		if(hasBuyer()) {
			
			FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, getSearch().getID(), null );
			((GenericNameFilter)nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
			
			m = new TSServerInfoModule(serverInfo.getModule(ID_SEARCH_BY_MODULE28));
	    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
			m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
			m.clearSaKeys();
			m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			
			m.addFilter(nameFilterBuyer);
			addBetweenDateTest(m, true, true, false);
			addFilterForUpdate(m, true);
			m.addValidator(DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ).getValidator());
			m.addValidator(new LastTransferDateFilter(searchId).getValidator());
			m.addValidator(nameFilterBuyer.getValidator());
			m.addValidator(rejectSavedDocuments.getValidator());
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;" });
			buyerNameIterator.setInitAgain(true);		//initialize again after all parameters are set
			m.addIterator(buyerNameIterator);
			
			l.add(m);
		}
		
		serverInfo.setModulesForAutoSearch(l);
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		Search search = getSearch();
		int searchType = search.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		if(searchType == Search.GO_BACK_ONE_LEVEL_SEARCH) {
			String district = DISTRICT_MAP.get(miServerID);
			
			GenericMultipleLegalFilter genericMultipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			genericMultipleLegalFilter.setAdditionalInfoKey("AKRO_LOOK_UP_DATA");
			genericMultipleLegalFilter.setUseLegalFromSearchPage(true);
			SearchAttributes sa = search.getSa();	
			
		    TSServerInfoModule module;	
		    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	
		    InstrumentAKROIterator instrumentBPInterator = new InstrumentAKROIterator(searchId, true);
		    InstrumentAKROIterator instrumentNoInterator = new InstrumentAKROIterator(searchId);
		    
		    boolean useNameForLookUp = false;
			if(instrumentBPInterator.createDerrivations().isEmpty() && instrumentNoInterator.createDerrivations().isEmpty() ){
				useNameForLookUp = true;
			}
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, useNameForLookUp, false, getDataSite());
			it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
			it.setAdditionalInfoKey("AKRO_LOOK_UP_DATA");
			it.setRoDoctypesToLoad(new String[]{"MORTGAGE", "TRANSFER", "RELEASE"});
			it.createDerrivations();	
	
		    for (String id : gbm.getGbTransfers()) {
				  		   	    	 
		    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		    	module.setIndexInGB(id);
		    	module.setTypeSearchGB("grantor");
		    	module.clearSaKeys();
		    	module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		    	
		    	module.forceValue(0, district);
		    	module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
		    	module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			    module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			 	module.addFilter( genericMultipleLegalFilter );
				module.addFilter( AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d) );
				module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
		    	module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
				nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;" } );
			 	module.addIterator(nameIterator);
			 	modules.add(module);
			    
			     
			 	if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			 		module =new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			 		module.setIndexInGB(id);
			 		module.setTypeSearchGB("grantee");
			 		module.clearSaKeys();
			 		module.forceValue(0, district);
			    	module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			 		module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			 		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			 		module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			 		module.addFilter( genericMultipleLegalFilter );
			 		module.addFilter( AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d) );
			 		module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			 		module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
			 		nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;" } );
			 		module.addIterator(nameIterator);			
			 		modules.add(module);
			 	}
		    }	 
		}
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
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
	
	public String getPrettyFollowedLink (String initialFollowedLnk)
    {	
		if (initialFollowedLnk.contains("SelectedDoc="))
    	{
    		String retStr =  "Instrument "+getInstrumentFromLink(initialFollowedLnk)+
    				" has already been processed from a previous search in the log file.";
    		return  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    	}
    	return "<br/><span class='followed'>Link already followed: </span>" + preProcessLink(initialFollowedLnk) + "<br/>";
    }
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String district = DISTRICT_MAP.get(miServerID);
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		String[] tokens = ro.cst.tsearch.servers.functions.AKGenericRO.uncleanInstrumentNumber(
				restoreDocumentDataI.getInstrumentNumber()
				);
		TSServerInfoModule module = null;
		if(tokens != null) {
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			if("0".equals(tokens[2])) {
				module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
				module.forceValue(0, district);
				filterCriteria.put("InstrumentNumber", tokens[0] + "_" + tokens[1]);
			} else {
				module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX);
				filterCriteria.put("InstrumentNumber", tokens[0] + "_" + tokens[1] + tokens[2]);
			}
			module.forceValue(1, tokens[0]);
			module.forceValue(2, tokens[1]);
			module.forceValue(3, tokens[2]);
			
			
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
			
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		    module.forceValue(0, district);
			module.forceValue(1, book);
			module.forceValue(2, page);
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
			
		}
		return module;
	}
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(
			ServerResponse response, String htmlContent, boolean forceOverritten) {
		
		DocumentI doc = response.getParsedResponse().getDocument() ;
		
		if(doc != null) {
		
			HashMap<String, String> data = new HashMap<String, String>();
			data.put("type", doc.getServerDocType());
			if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc.getBook())) {
				data.put("book", doc.getBook());
				data.put("page", doc.getPage());
			}
			if(isInstrumentSaved(doc.getInstno(), null, data)) {
				return ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS;
			}
		
		}
		
		return super.addDocumentInATS(response, htmlContent, forceOverritten);
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
				String page = getLinkContents(dataSite.getLink().replace("search.cfm", "searchRO.cfm"));
					
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

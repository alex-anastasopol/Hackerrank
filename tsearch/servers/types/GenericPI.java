package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.servlet.parentsite.ParentSiteActions;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.MortgageI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class GenericPI extends TSServerPI{ 

	private static final long serialVersionUID = 880423446024981132L;
	protected static final String LINK_TO_IMAGE_REGEX = "(?i)[<][ \t\r\n]*a[ \t\r\n]+href[ \t\r\n]*[=][ \t\r\n]*\"#LINK_TO_IMAGE\"[ \t\r\n]*>[ \t\r\n]*View[ \t\r\n]*Image([^<]*)</a>";
	protected static final Pattern patImageLink = Pattern.compile(LINK_TO_IMAGE_REGEX);
	
	public GenericPI(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId, mid);
	}

	public static final HashSet<String> mapIdModuleSaKeys = new HashSet<String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		add(SearchAttributes.LD_PI_MAP_BOOK);
		add(SearchAttributes.LD_PI_MAP_PAGE);
		add(SearchAttributes.LD_PI_LOT);
		add(SearchAttributes.LD_PI_TRACT);
		add(SearchAttributes.LD_PI_MAP_CODE);
		add(SearchAttributes.LD_PI_MAJ_LEGAL_NAME);
	}};
	
	public static final String MAP_LOT 				= "Lot=";
	public static final String MAP_TRACT			= "Tract=";
	public static final String MAP_BOOK 			= "mapBook=";
	public static final String MAP_PAGE 			= "mapPage=";
	public static final String MAP_CODE 			= "mapCode=";
	public static final String MAP_MJR_LEGAL_NAME 	= "majorLegalName=";
	
	public GenericPI(long searchId) {
		super(searchId);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {
		ParsedResponse pr = response.getParsedResponse();		
//		String moduleIdx = sAction.replaceFirst("(?is)\\A(\\d+)&.*", "$1");
		
		switch (viParseID){
			case ID_SEARCH_BY_SUBDIVISION_NAME:
				
				String result = response.getResult();
				if (StringUtils.isNotEmpty(result)){
					try {
						JSONObject jsonObject = XML.toJSONObject(result);
						if (jsonObject != null && jsonObject.has("Items")){
							JSONObject itemsObject = jsonObject.getJSONObject("Items");
							if (itemsObject != null && itemsObject.has("Item")){
								JSONArray jsonArray = null;
								try {
									jsonArray = itemsObject.getJSONArray("Item");
								} catch (Exception e) {
									//found a single json object
									jsonObject = itemsObject.getJSONObject("Item");
									jsonArray = new JSONArray();
									jsonArray.put(jsonObject);
								}
								
								StringBuilder outputTable = new StringBuilder();
								Collection<ParsedResponse> smartParsedResponses = smartParseLookup(response, jsonArray, result, outputTable, false, sAction);
								
								if (smartParsedResponses.size() > 0) {
									String footer = pr.getFooter();     
						               	
									String header = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"/title-search" + URLMaping.PARENT_SITE_ACTIONS + "\"" + " method=\"POST\" > "
											+ "<input type=\"hidden\" name=\""+ TSOpCode.OPCODE + "\" value=\""+ ParentSiteActions.SUBDIVISION_NAME_LOOKUP + "\">"
											+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
											+ "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "; 
						               		
						               		
									header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
									String firstRow = "<tr><th>Select</th><th>Name</th><th>Legal</th><th>Description</th><th>MapType</th><th>Major Legal Value</th>"
											+ "<th>Major Legal Name</th>"
											+ "<th>Book</th><th>Page</th><th>Map Name</th><th>ArbTract</th>"
											+ "<th>Sec</th><th>Twp</th><th>Rng</th>"
//											+ "<th>Condo PlanDate</th><th>Vacated</th>"
											+ "<th>Tract Remarks</th><th>Tract Amendments</th>"
//											+ "<th>OpenDate</th><th>MapDate</th>"
											+ "<th>LegalNarrative</th><th>SpecialInstructions</th>"
											+ "</tr>\n";
									 
									 header += firstRow;
									
									pr.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
										
									footer = "\n</table>" + "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Select Legal\">\r\n</form>\n" ;
									pr.setOnlyResponse(outputTable.toString());
									pr.setHeader(header);
									pr.setFooter(footer);
									
									pr.setAttribute(PARENT_SITE_LOOKUP_MODE, true);
								}
							}
						} else if (jsonObject != null && jsonObject.has("PropertyDetails")){
							JSONObject itemsObject = jsonObject.getJSONObject("PropertyDetails");
							if (itemsObject != null && itemsObject.has("Item")){
								JSONArray jsonArray = itemsObject.getJSONArray("Item");
								StringBuilder outputTable = new StringBuilder();
								Collection<ParsedResponse> smartParsedResponses = smartParseLookup(response, jsonArray, result, outputTable, true, sAction);
								
								if (smartParsedResponses.size() > 0) {
									String footer = pr.getFooter();     
						               	
									String header = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"/title-search" + URLMaping.PARENT_SITE_ACTIONS + "\"" + " method=\"POST\" > "
											+ "<input type=\"hidden\" name=\""+ TSOpCode.OPCODE + "\" value=\""+ ParentSiteActions.SUBDIVISION_NAME_LOOKUP + "\">"
											+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
											+ "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "; 
						               		
						               		
									header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
									header += "<tr><th width=\"10%\">Select</th><th>Lot</th><th>Common Lot</th>"
											+ "<th>Block</th><th>Building</th><th>Unit</th>"
											+ ""
											+ "<th>Map Book</th><th>Map Page</th>"
											+ "</tr>\n";
									
									pr.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
										
									footer = "\n</table>" + "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Select Legal\">\r\n</form>\n" ;
									pr.setOnlyResponse(outputTable.toString());
									pr.setHeader(header);
									pr.setFooter(footer);
									
									pr.setAttribute(PARENT_SITE_LOOKUP_MODE, true);
								}
							}
						}
					} catch (Exception e) {
						logger.error("Exception when converting from Xml to Json on Subdivision Lookup on PI: " + searchId, e);
					}
				}
                
			
				break;
			case ID_SAVE_TO_TSD:{
				DocumentI document = pr.getDocument();

				if (document != null) {
					msSaveToTSDFileName = document.getId() + ".html";
					response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				}

		            
		        }
		    break;
			case ID_SEARCH_BY_BOOK_AND_PAGE:
			case ID_DETAILS:
				{
					boolean isAlreadySaved = false;
					long countSavedRows = 0;
					
					Vector<ParsedResponse> newList = new Vector<ParsedResponse>();
			    	// parse all records
		            for ( ParsedResponse item: (Vector<ParsedResponse>)pr.getResultRows()) {            	
		            	String itemHtml = item.getResponse();
		            	String shortType = "PI";
		            	
		            	item.setParentSite(pr.isParentSite());
		            	item.setResponse( itemHtml );
		            	String instrNo="";
		            	HashMap<String, String> data = new HashMap<String, String>();
		            	
		            	DocumentI doc = item.getDocument();
		            	
//		            	if(doc instanceof RegisterDocumentI){
//		            		if("0".equalsIgnoreCase(moduleIdx)){
//		            			((RegisterDocumentI)doc).setSearchType(SearchType.GI);
//		            		}else{
//		            			((RegisterDocumentI)doc).setSearchType(SearchType.PI);
//		            		}
//		            	}
		            	
		            	instrNo = doc.getInstno();  
		            	
		            	if (doc.hasBookPage()){
		            		if (StringUtils.isBlank(instrNo)){
		            			instrNo = doc.getBook() + "-" + doc.getPage();
		            		} else{
		            			instrNo += "_" + doc.getBook() + "-" + doc.getPage();
		            		}
		            	}
		            	
		            	if(StringUtils.isBlank(instrNo)){
		            		logger.warn(searchId + ": Document from SSF has NO Instrument number. It has been skipped!");           		
		            		continue;
		            	}
		            	instrNo = instrNo.replaceAll("[ \t\r\n]+","");
		            	
		            	if (this.toString().startsWith("IL")){
		            		String doctype = doc.getServerDocType();
		            		data.put("type", doctype);
		            	}
		            	Date recDate = ((RegisterDocumentI)doc).getRecordedDate();
	            		
	            		if (recDate != null){
	            			String recDateString = FormatDate.getDateFormat(FormatDate.PATTERN_MMddyyyy).format(recDate);
	            			instrNo += "_" + recDateString;
	            		}
		            	
		            	// create links
		            	String originalLink = PREFIX_FINAL_LINK + instrNo;  
		            	String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;   
			               
			            msSaveToTSDFileName = instrNo + ".html";     
			            
			            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		            	String checkbox = "";
		            		        
		            	boolean isSaved = isAlreadySaved(instrNo, doc);
		            	
		            	DocumentsManagerI manager = getSearch().getDocManager();
		            	try{
		                	manager.getAccess();
			            	if (doc.isFake()  && manager.flexibleContains(doc)){
			            		isSaved =  true;
			            	}
		            	} catch(Exception e){  
		                	e.printStackTrace(); 
		                } finally{
		                	manager.releaseAccess();
		                }	
		            	
	            		if (isSaved && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
	            			checkbox = "saved";
	            			countSavedRows++;
	            			isAlreadySaved = true;
	            		} else {
	            			checkbox = DOCLINK_CHECKBOX_START + sSave2TSDLink + "'>";
	            			mSearch.addInMemoryDoc(sSave2TSDLink, item);
	            		}
		                itemHtml =	"<tr> <td valign=\"center\" align=\"center\">" + checkbox + "</td> <td align=\"center\"><b>" + shortType + 
		                			"</b></td><td>" + itemHtml + "</td><tr>"; 
		               
		                Matcher mat = patImageLink.matcher(itemHtml);
		                if(mat.find()){
		                	data.put("imageId", item.getSaleDataSet(0).getAtribute("InstrCode"));
		                	String imageLink = createLinkForImage(data);
		                	itemHtml = itemHtml.replaceAll(LINK_TO_IMAGE_REGEX,  "<a href=\"" + imageLink + "\">View Image</a>");
		                	if(item.getImageLinksCount() == 0){
		                		item.addImageLink(new ImageLinkInPage (imageLink, instrNo + ".tiff" ));
		                	}
		                }  
		                
		                item.setPageLink(new LinkInPage(sSave2TSDLink, sSave2TSDLink, TSServer.REQUEST_SAVE_TO_TSD));
		                parser.Parse(item, itemHtml,Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
			            
		                Set<InstrumentI> parsedRefs = doc.getParsedReferences();
			            if (parsedRefs != null && !parsedRefs.isEmpty()){
			            	DocumentsManagerI documentManager = getSearch().getDocManager();
			            	try{
			            		documentManager.getAccess();
			            		for (InstrumentI documentToCheck : parsedRefs){
			            			List<DocumentI> docList = checkFlexibleInclusion(documentToCheck, documentManager, false);
			            			if (docList == null || docList.isEmpty()){
			            				saveWithCrossReferences(documentToCheck, item);
						           	}
			            		}
			            	} finally{
			            		documentManager.releaseAccess();
			            	}
			            }
			            
		                newList.add(item);
		            }
		            pr.setResultRows(newList);
		            
		            if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = pr.getHeader();
		               	String footer = pr.getFooter();                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n<tr bgcolor=\"#cccccc\">\n<th width=\"1%\">" +
		            			"<div>" + SELECT_ALL_CHECKBOXES + "</div>" +
		            			"</th> <th width=\"1%\">Type</th> \n<th width=\"98%\" align=\"left\">Document</th> \n</tr>";
		            	
		            	if (isAlreadySaved && pr.getResultsCount() == countSavedRows) {
		            		footer = "\n</table>" + CreateFileAlreadyInTSD();       
		            	} else { 
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);        
		            	}
		            	
		            	pr.setHeader(header);
		            	pr.setFooter(footer);
		            }
				}
			break;
		}
	}

	/**
	 * implement it in the derived class (either in the class specific for a county if exists, either in the generic class for a state)
	 *       where you want to do the shit for equivalence
	 * @param instrumentNo
	 * @param doc
	 * @return
	 */
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc){
		return isInstrumentSaved(instrumentNo, doc, null);
	}
	
	private Collection<ParsedResponse> smartParseLookup(ServerResponse response, JSONArray jsonArray, String rsResponce, StringBuilder outputTable, boolean isDetailsLookup, String sAction) {
    	Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
    	
    	outputTable.append("<table BORDER='1' CELLPADDING='2'>");
    	
    	if (isDetailsLookup){
    		String mapBook = "", mapPage = "";
    		if (sAction.contains("Property.MajorLegalValue=")){
    			String bp = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(sAction.replaceAll(";", "&"), "Property.MajorLegalValue");
    			if (StringUtils.isNotEmpty(bp)){
    				String[] bps = bp.split("!");
    				if (bps.length == 2){
    					mapBook = bps[0];
    					mapPage = bps[1];
    				}
    			}
    		}
    		for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					StringBuffer row = new StringBuffer();
					boolean hasAtLeastOneItem = false;
					
					String lot = "";
					if (jsonObject.has("Lot")){
						Object lotValue = jsonObject.get("Lot");
						if (lotValue instanceof String){
							lot = (String) lotValue;
						} else if (lotValue instanceof Long){
							lot = ((Long) lotValue).toString();
						}
						hasAtLeastOneItem = true;
					}
					String commonLot = "";
					if (jsonObject.has("CommonLot")){
						Object commonLotValue = jsonObject.get("CommonLot");
						if (commonLotValue instanceof String){
							commonLot = (String) commonLotValue;
						} else if (commonLotValue instanceof Long){
							commonLot = ((Long) commonLotValue).toString();
						}
						hasAtLeastOneItem = true;
					}
					String block = "";
					if (jsonObject.has("Block")){
						Object blockValue = jsonObject.get("Block");
						if (blockValue instanceof String){
							block = (String) blockValue;
						} else if (blockValue instanceof Long){
							block = ((Long) blockValue).toString();
						}
						hasAtLeastOneItem = true;
					}
					String bldg = "";
					if (jsonObject.has("Building")){
						Object bldgValue = jsonObject.get("Building");
						if (bldgValue instanceof String){
							bldg = (String) bldgValue;
						} else if (bldgValue instanceof Long){
							bldg = ((Long) bldgValue).toString();
						}
						hasAtLeastOneItem = true;
					}
					String unit = "";
					if (jsonObject.has("Unit")){
						Object unitValue = jsonObject.get("Unit");
						if (unitValue instanceof String){
							unit = (String) unitValue;
						} else if (unitValue instanceof Long){
							unit = ((Long) unitValue).toString();
						}
						hasAtLeastOneItem = true;
					}
					
					if (hasAtLeastOneItem){
						row.append("<td>").append(lot).append("</td>");
						row.append("<td>").append(commonLot).append("</td>");
						row.append("<td>").append(block).append("</td>");
						row.append("<td>").append(bldg).append("</td>");
						row.append("<td>").append(unit).append("</td>");
						row.append("<td>").append(mapBook).append("</td>");
						row.append("<td>").append(mapPage).append("</td>");
							
						String value = MAP_LOT + lot + "/____@@" + MAP_BOOK + mapBook + "/____@@" + MAP_PAGE + mapPage;
							
						ParsedResponse currentResponse = new ParsedResponse();
			
						String radioStart = "<input type=\"radio\" name=\"doclink\" value=\"";
						currentResponse.setOnlyResponse("<tr><td>" + radioStart + URLEncoder.encode(value, "UTF-8") + "\"> " + "</td>" + row.toString() + "</tr>");
						outputTable.append(currentResponse.getResponse());
						responses.add(currentResponse);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
    	} else{
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					StringBuffer row = new StringBuffer();
					
					String name = "";
					if (jsonObject.has("Name")){
						name = (String) jsonObject.get("Name");
					}
					String descr = "";
					if (jsonObject.has("Description")){
						descr = (String) jsonObject.get("Description");
					}
					String legal = "";
					if (jsonObject.has("Legal")){
						legal = (String) jsonObject.get("Legal");
					}
					String mapType = "";
					if (jsonObject.has("MapType")){
						mapType = (String) jsonObject.get("MapType");
					}
					
					String mjrLegalValue = "";
					if (jsonObject.has("MajorLegalValue")){
						Object mjrLglValue = jsonObject.get("MajorLegalValue");
						if (mjrLglValue instanceof String){
							mjrLegalValue = (String) mjrLglValue;
						} else if (mjrLglValue instanceof Long){
							mjrLegalValue = ((Long) mjrLglValue).toString();
						}
					}
					String mjrLegalName = "";
					if (jsonObject.has("MajorLegalName")){
						mjrLegalName = (String) jsonObject.get("MajorLegalName");
					}
					String tractRemarks = "";
					if (jsonObject.has("TractRemarks")){
						tractRemarks = (String) jsonObject.get("TractRemarks");
					}
					String tractAmendments = "";
					if (jsonObject.has("TractAmendments")){
						tractAmendments = (String) jsonObject.get("TractAmendments");
					}
					String book = "";
					String page = "";
					if (jsonObject.has("Book")){
						Object bookValue = jsonObject.get("Book");
						if (bookValue instanceof String){
							book = (String) bookValue;
						} else if (bookValue instanceof Long){
							book = ((Long) bookValue).toString();
						}
					}
					if (jsonObject.has("Page")){
						Object pageValue = jsonObject.get("Page");
						if (pageValue instanceof String){
							page = (String) pageValue;
						} else if (pageValue instanceof Long){
							page = ((Long) pageValue).toString();
						}
					}
					String mapName = "";
					if (jsonObject.has("MapName")){
						mapName = (String) jsonObject.get("MapName");
					}
					String arbTract = "";
					if (jsonObject.has("ArbTract")){
						arbTract = (String) jsonObject.get("ArbTract");
					}
//					String condoPlanDate = (String) jsonObject.get("CondoPlanDate");
//					String vacated = (String) jsonObject.get("Vacated");
//					String openDate = (String) jsonObject.get("OpenDate");
//					String mapDate = (String) jsonObject.get("MapDate");
					String legalNarrative = "";
					if (jsonObject.has("LegalNarrative")){
						legalNarrative = (String) jsonObject.get("LegalNarrative");
					}
					
					String specialInstr = "";
					if (jsonObject.has("SpecialInstructions")){
						specialInstr = (String) jsonObject.get("SpecialInstructions");
					}
					String sec = "";
					if (jsonObject.has("Section")){
						Object secValue = jsonObject.get("Section");
						if (secValue instanceof String){
							sec = (String) secValue;
						} else if (secValue instanceof Long){
							sec = ((Long) secValue).toString();
						}
					}
					String twp = "";
					if (jsonObject.has("Township")){
						Object twpValue = jsonObject.get("Township");
						if (twpValue instanceof String){
							twp = (String) twpValue;
						} else if (twpValue instanceof Long){
							twp = ((Long) twpValue).toString();
						}
					}
					String rng = "";
					if (jsonObject.has("Range")){
						Object rngValue = jsonObject.get("Range");
						if (rngValue instanceof String){
							rng = (String) rngValue;
						} else if (rngValue instanceof Long){
							rng = ((Long) rngValue).toString();
						}
					}
					row.append("<td>").append(name).append("</td>");
					row.append("<td>").append(legal).append("</td>");
					row.append("<td>").append(descr).append("</td>");
					row.append("<td>").append(mapType).append("</td>");
					row.append("<td>").append(mjrLegalValue).append("</td>");
					row.append("<td>").append(mjrLegalName).append("</td>");
					row.append("<td>").append(book).append("</td>");
					row.append("<td>").append(page).append("</td>");
					row.append("<td>").append(mapName).append("</td>");
					row.append("<td>").append(arbTract).append("</td>");
					row.append("<td>").append(sec).append("</td>");
					row.append("<td>").append(twp).append("</td>");
					row.append("<td>").append(rng).append("</td>");
//					row.append("<td>").append(condoPlanDate).append("</td>");
//					row.append("<td>").append(vacated).append("</td>");
					row.append("<td>").append(tractRemarks).append("</td>");
					row.append("<td>").append(tractAmendments).append("</td>");
//					row.append("<td>").append(openDate).append("</td>");
//					row.append("<td>").append(mapDate).append("</td>");
					row.append("<td>").append(legalNarrative).append("</td>");
					row.append("<td>").append(specialInstr).append("</td>");
					
					String tract = "";
					if (StringUtils.isNotEmpty(legal) && legal.matches("(?is)\\bTR\\s+\\w+")){
						tract = legal.replaceFirst("(?is)\\bTR\\s+(\\w+)", "$1");
					}
					
									
					String value = MAP_BOOK + book + "/____@@" + MAP_PAGE + page + "/____@@" + MAP_CODE + mapType + "/____@@" + MAP_MJR_LEGAL_NAME + mjrLegalName
							+ "/____@@" + MAP_TRACT + tract;
					
					ParsedResponse currentResponse = new ParsedResponse();
	
					String radioStart = "<input type=\"radio\" name=\"doclink\" value=\"";
					currentResponse.setOnlyResponse("<tr><td>" + radioStart + URLEncoder.encode(value, "UTF-8") + "\"> " + "</td>" + row.toString() + "</tr>");
					outputTable.append(currentResponse.getResponse());
					responses.add(currentResponse);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
    	}
		outputTable.append("</table>");
		return responses;
	}
	
   @Override
   protected DownloadImageResult saveImage(ImageLinkInPage image)  throws ServerResponseException{
   
   	
   	TSServerInfo info = getDefaultServerInfo();
   	TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
    String imageId = "";
	String link = image.getLink();
	int poz = link.indexOf("?");
	
	if(poz>0){
		link = link.substring(poz+1);
	}
	
	String[] allParameters = link.split("[&=]");
	
	for(int i=0;i<allParameters.length-1;i+=2){
		if("imageId".equalsIgnoreCase(allParameters[i])){
			imageId = allParameters[i+1];
		}
	}
   	
   	module.setParamValue( 0, imageId );
  
   
   	String imageName = image.getPath();
   	if(FileUtils.existPath(imageName)){
   		byte b[] = FileUtils.readBinaryFile(imageName);
   		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
   	}
   	
   	ServerResponse response = SearchBy(module, null);
   	DownloadImageResult res = response.getImageResult();
   	return res;
   	
   }
	
	protected  String createLinkForImage(HashMap<String,String> value){
		 
		 TSServerInfoModule imgModule = getDefaultServerInfoWrapper().getModule(TSServerInfo.IMG_MODULE_IDX);
		 
		 StringBuilder build = new StringBuilder("");//<a href=\"
		 build .append(createPartialLink(TSConnectionURL.idDASL,TSServerInfo.IMG_MODULE_IDX));
		 build .append("DASLIMAGE&");
		 //imgModule.getFunction(0).getName()
		 
		 build .append( imgModule.getFunction(0).getParamName() ); /*type*/
		 build .append("=");
		 build .append(StringUtils.defaultString(value.get("imageId")));
		 
		 return build.toString();
	}
	
	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		build.append("&");
		build.append(getLinkPrefix(iActionType));
		return build.toString();
	}



	public void setServerID(int serverID) {
		super.setServerID(serverID);
		setRangeNotExpanded(true);
	}
	 
	 private String  preparePinForPI(String pin){
		 
		 if(StringUtils.isBlank(pin)){
			 return "";
		 }
		 
		 String retPin = "";
		 
		 State state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState();
		 County county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
		 Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		 String stateAbrev = state.getStateAbv();
		 String countyName = county.getName();
		 
		 if("FL".equalsIgnoreCase(stateAbrev)){
			 if("Broward".equalsIgnoreCase(countyName)){ //##-##-##-##-###
				 if(pin.length()>=11){
					 retPin = pin.substring(0,2)+"-"+pin.substring(2,4)+"-"+pin.substring(4,6)
					 		+"-"+pin.substring(6,8)+"-"+pin.substring(8);
				 }
			 }else if("Miami-Dade".equalsIgnoreCase(countyName)){//##-####-###-####
				 if(pin.length()>=12){
					 retPin = pin.substring(0,2)+"-"+pin.substring(2,6)+"-"+pin.substring(6,9)+"-"+pin.substring(9);
				 }
			 }else if("Palm Beach".equalsIgnoreCase(countyName)){ //41-41-43-17-01-127-0050
				 if(pin.length()>=16){
					 retPin = pin.substring(0,2)+"-"+pin.substring(2,4)+"-"+pin.substring(4,6)
					 		+"-"+pin.substring(6,8)+"-"+pin.substring(8,10)+"-"+pin.substring(10,13)+"-"+pin.substring(13);
				 }
			 }else if("Hillsborough".equalsIgnoreCase(countyName)){
				 pin = search.getSa().getAtribute(SearchAttributes.LD_PARCELNO3);
				 pin = pin.replaceAll("[.-]", "");
				 if(pin.length()>=10){
					 retPin = pin;
				 }
			 }else if("Orange".equalsIgnoreCase(countyName)){ //##-##-##-####-##-###
				 pin = search.getSa().getAtribute(SearchAttributes.LD_PARCELNONDB);
				 if(StringUtils.isBlank(pin)){
					 pin = search.getSa().getAtribute(SearchAttributes.LD_PARCELNO);
					 if(StringUtils.isBlank(pin)){
						 pin = search.getSa().getAtribute(SearchAttributes.LD_PARCELNO2);
					 }
					 pin = pin.replaceAll("[.-]", "");
					 if(pin.length()>=15){
						 retPin = pin.substring(0,2)+"-"+pin.substring(2,4)+"-"+pin.substring(4,6)+"-"+pin.substring(6,10)
						 			+"-"+pin.substring(10,12)+"-"+pin.substring(12);
					 }
				 } else {
					 //we have pin from NDB observation we must permute it 
					 //bug 7303
					 pin = pin.replaceAll("[.-]", "");
					 if(pin.length()>=15){
						 retPin = pin.substring(4,6)+"-"+pin.substring(2,4)+"-"+pin.substring(0,2)+"-"+pin.substring(6,10)
						 			+"-"+pin.substring(10,12)+"-"+pin.substring(12);
					 }
				 }
			 }
		 }
		 return retPin;
	 }
	
	 protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
			List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			SearchAttributes sa = global.getSa();
			
			String parcel = sa.getAtribute(SearchAttributes.LD_PARCELNO);
			if(StringUtils.isBlank(parcel)){
				parcel = sa.getAtribute(SearchAttributes.LD_PARCELNO2);
			}
			
			if(StringUtils.isBlank(parcel)){
				parcel = sa.getAtribute(SearchAttributes.LD_PARCELNONDB);
			}
			
			if(StringUtils.isNotBlank(parcel)){
				parcel = parcel.replaceAll("[ -]", "");
				parcel = preparePinForPI(parcel);
				sa.setAtribute(SearchAttributes.LD_PARCELNO2_ALTERNATE, parcel);
			}
			
			FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			nameFilterOwner.setInitAgain(true);
			FilterResponse legalFilter 		= LegalFilterFactory.getDefaultLegalFilter(searchId);
			LastTransferOrMortgageDateFilter lastTransferDateFilter 	= new LastTransferOrMortgageDateFilter(searchId);
			PinFilterResponse pinFilter = new PinFilterResponse(SearchAttributes.LD_PARCELNO2_ALTERNATE, searchId);
			pinFilter.setStartWith(true);
			pinFilter.setIgNoreZeroes(true);
			
			FilterResponse[] filtersO 	= { nameFilterOwner, legalFilter, pinFilter, lastTransferDateFilter, new PIDoctypeFilterResponse(searchId) };
			FilterResponse[] filters 	= { legalFilter, pinFilter };
			
			if(StringUtils.isNotBlank(parcel)){
				TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX) );
				module.clearSaKeys();
				module.forceValue(0, parcel);
				modules.add(module);
				
				module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX) );
				module.clearSaKeys();
				module.forceValue(0, parcel);
				module.forceValue(1, "MortgageReport");
				modules.add(module);
			}else{
				String[] streetNo = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETNO)).split("-");
				String streetName = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETNAME));
				String unit = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETUNIT));
				String city = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_CITY));
				String zipp = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_ZIP));
				String predir = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETDIRECTION_ABBREV));
				String postDir = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREET_POST_DIRECTION));
				String suffix = StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETSUFIX));
				
				String fullStreetName = streetName;
				
				if(StringUtils.isBlank(zipp)){
					fullStreetName = (predir + " " + streetName + " " + postDir + " "+ suffix).trim().replaceAll("[ ]+", " ");
				}
				
				if(StringUtils.isNotBlank(fullStreetName)&&StringUtils.isNotBlank(streetNo[0])
						&& (StringUtils.isNotBlank(city)||StringUtils.isNotBlank(zipp)) ){
					TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX) );
					module.clearSaKeys();
					module.forceValue(0, streetNo[0]);
					module.forceValue(1, fullStreetName);
					module.forceValue(2, unit);
					module.forceValue(3, city);
					module.forceValue(4, zipp);
					if( streetNo.length>1 ){
						module.forceValue(5, streetNo[1]);
					}else{
						module.forceValue(5, "");
					}
					modules.add(module);
					
					module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX) );
					module.clearSaKeys();
					module.forceValue(0, streetNo[0]);
					module.forceValue(1, fullStreetName);
					module.forceValue(2, unit);
					module.forceValue(3, city);
					module.forceValue(4, zipp);
					if( streetNo.length>1 ){
						module.forceValue(5, streetNo[1]);
					}else{
						module.forceValue(5, "");
					}
					module.forceValue(6, "MortgageReport");
					modules.add(module);
				}
			}
			
			addNameSearch(  modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersO  );
			
			addOCRSearch(modules, serverInfo, filters);
			
			serverInfo.setModulesForAutoSearch(modules);	
	   }
	 
	 class PIDoctypeFilterResponse extends DocTypeSimpleFilter{
		
		private static final long serialVersionUID = -5217156667252029059L;

		public PIDoctypeFilterResponse(long searchId){
			super(searchId);
			String []docTypes = {
					DocumentTypes.MISCELLANEOUS,
					DocumentTypes.LIEN,
					DocumentTypes.COURT,
					DocumentTypes.AFFIDAVIT,
					DocumentTypes.MORTGAGE,
					DocumentTypes.RELEASE
			};
			setDocTypes(docTypes);
			threshold = BigDecimal.ONE; 
		}
		
		public BigDecimal getScoreOneRow(ParsedResponse row) {	
			
			BigDecimal result = super.getScoreOneRow(row);
			
			String docType = row.getSaleDataSet(0).getAtribute("DocumentType").trim();
			
			String OeRating = row.getSaleDataSet(0).getAtribute("OeRating").trim();
			
			String docTypes[] = {DocumentTypes.MORTGAGE};
			
			
			if(DocumentTypes.isOfDocType(docType, docTypes , searchId)){
				if("FVD".equalsIgnoreCase(OeRating)||"Key".equalsIgnoreCase(OeRating)
						||"Major".equalsIgnoreCase(OeRating)||"Minor".equalsIgnoreCase(OeRating)){
					return BigDecimal.ONE;
				}
			}
			
			return result;
		}
		
	 }
	 
	 protected void addOCRSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, FilterResponse ...filters){
			// OCR last transfer - book / page search
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		    module.clearSaKeys();
		    module.getFunction(0).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
		    module.getFunction(1).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
		    for(int i=0;i<filters.length;i++){
		    	module.addFilter(filters[i]);
		    }
		    addBetweenDateTest(module, false, false, false);
			modules.add(module);
			
		    // OCR last transfer - instrument search
		    module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		    module.clearSaKeys();
		    module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		    for(int i=0;i<filters.length;i++){
		    	module.addFilter(filters[i]);
		    }
		    addBetweenDateTest(module, false, false, false);
			modules.add(module);
		}
	 
	  protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames, FilterResponse ...filters ) {
			ConfigurableNameIterator nameIterator = null;
			
			TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			module.clearSaKeys();
			module.setSaObjKey(key);
			
			for (int i = 0; i < filters.length; i++) {
				module.addFilter(filters[i]);
			}
			addBetweenDateTest(module, false, true, true);
			addFilterForUpdate(module, true);

			module.setIteratorType( 0,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			module.setIteratorType( 3,  FunctionStatesIterator.ITERATOR_TYPE_OE_RESULT_TYPE );
			
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			nameIterator.setAllowMcnPersons( true );
			
			if ( searchedNames!=null ) {
				nameIterator.setInitAgain( true );
				nameIterator.setSearchedNames( searchedNames );
			}
			
			searchedNames = nameIterator.getSearchedNames() ;
			module.addIterator( nameIterator );
			
		
			modules.add( module );
			return searchedNames;
		}
	  
		 protected static class LastTransferOrMortgageDateFilter extends FilterResponse{
			
			private static final long serialVersionUID = -5449648091634602030L;
			private Date lastRecordedDate = null;
			 
			public LastTransferOrMortgageDateFilter(long searchId) {
				super(searchId);
				setInitAgain(true);
				super.threshold = BigDecimal.ONE;;
			}
			
			@Override
			public void init() {
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI docManager = global.getDocManager();
				try{
					docManager.getAccess();
					//TransferI transfer = docManager.getLastRealTransferWithGoodName(false);
					TransferI transfer = docManager.getLastRealTransfer();
					if(transfer!=null){
						lastRecordedDate = transfer.getRecordedDate();
					}
					if(lastRecordedDate==null ){
						MortgageI mortgage = docManager.getLastMortgageForOwner();
						if(mortgage!=null){
							lastRecordedDate = mortgage.getRecordedDate();
						}
					}
					if(lastRecordedDate==null ){
						transfer = docManager.getLastRealTransfer();
						if(transfer!=null){
							lastRecordedDate = transfer.getRecordedDate();
						}
					}
				}finally{
					docManager.releaseAccess();
				}
			}
			
			@Override
			public BigDecimal getScoreOneRow(ParsedResponse row){
				if(lastRecordedDate==null){
					init();
				}
				if(lastRecordedDate!=null){
					DocumentI doc = row.getDocument();
					if(doc instanceof RegisterDocumentI){
						RegisterDocumentI regDoc = (RegisterDocumentI)doc;
						Date recordedDate = regDoc.getRecordedDate();
						if(lastRecordedDate.after(recordedDate)){
							return  BigDecimal.ZERO;
						}
					}
				}
				return BigDecimal.ONE;
			}
			
			public String getFilterName(){
		    	return "Filter by Last Trancation Date";
		    }
			
			@Override
			public String getFilterCriteria(){

				String lastTransFerdate = "";
				SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
				
				if(lastRecordedDate!=null){
					lastTransFerdate = format.format(lastRecordedDate);
				}
				
		    	return "Last Transaction Date='" + lastTransFerdate + "'";
		    }
		 }
		 @Override
		public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
			if(restoreDocumentDataI == null) {
				return null;
			}
			String book = restoreDocumentDataI.getBook();
			String page = restoreDocumentDataI.getPage();
			TSServerInfoModule module = null;
			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
				module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
				module.forceValue(0, book);
				module.forceValue(1, page);
				HashMap<String, String> filterCriteria = new HashMap<String, String>();
				filterCriteria.put("Book", book);
				filterCriteria.put("Page", page);
				GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
				module.getFilterList().clear();
				module.addFilter(filter);
			} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
				module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
				module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
				//module.forceValue(1, Integer.toString(restoreDocumentDataI.getYear()));
			} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
				module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
				module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
				//module.forceValue(1, Integer.toString(restoreDocumentDataI.getYear()));
			}
			return module;
		}
		
		public Object getImageDownloader(RestoreDocumentDataI document) {
			return getRecoverModuleFrom(document);
		}
		
		 public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
		    	if(StringUtils.isEmpty(instrumentNo))
		    		return false;
		    	
		    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
		    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
		    		return false;
		    	}
		    	
		    	if("ILCook".equals(getDataSite().getSTCounty())){
		    		checkMiServerId = false;
		    		documentToCheck = documentToCheck.clone();
					documentToCheck.setDocSubType(DocumentTypes.getDocumentCategory("MISC", searchId));
		    	}
		    	
		    	DocumentsManagerI documentManager = getSearch().getDocManager();
		    	try {
		    		documentManager.getAccess();
		    		if(documentToCheck != null) {
		    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
		    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
			    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
			    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
			    				
			    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
		    				}
		    				return true;
		    			} else if(!checkMiServerId) {
		    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
		    				if(almostLike != null && !almostLike.isEmpty()) {
		    					return true;
		    				} else if("ILCook".equals(getDataSite().getSTCounty())) {
		    					documentToCheck.setInstno(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(documentToCheck.getInstno()));
		    					almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
		    					if(almostLike != null && !almostLike.isEmpty()) {
			    					return true;
			    				}
		    				}
		    			}
		    		} else {
			    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
			    		if(data != null) {
				    		if(!StringUtils.isEmpty(data.get("type"))) {
				        		String serverDocType = data.get("type");
				    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
				            	instr.setDocType(docCateg);
				            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
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
			    			
			    			if(almostLike.isEmpty() && "ILCook".equals(getDataSite().getSTCounty())){
			    				instr.setInstno(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(instr.getInstno()));
			    				almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
			    			}
			    			
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
}

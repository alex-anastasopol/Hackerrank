package ro.cst.tsearch.servers.types;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.OffenderInformationDocumentI;

public class GenericIL extends TSServer {
	
	static final long serialVersionUID = 16469563L;
	
	public GenericIL(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public GenericIL(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		boolean hasOwner = hasOwner();
								
		if(hasOwner) {
			GenericNameFilter nameFilterOwner = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterOwner.setIgnoreEmptyMiddleOnCandidat(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.addFilter(nameFilterOwner);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"}));
			moduleList.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		
		switch (viParseID) {
		
			case ID_SEARCH_BY_PARCEL :
			case ID_SEARCH_BY_NAME :
			    
			    if (rsResponse.indexOf("Inmate Not Found") != -1){
	            	Response.getParsedResponse().setError("Inmate Not Found");
			    	return;
	            }
	            
			    //intermediary table
			    String table = "";			
			    Node divNode = HtmlParserTidy.getNodeByTagAndAttr(rsResponse, "div", "class", "position-content");
			    if (divNode != null)
			    {
			    	try {
						String divHtml = HtmlParserTidy.getHtmlFromNode(divNode);
						Node outerTableNode = HtmlParserTidy.getNodeByTagAndAttr(divHtml, "table", "border", "0");
						String outerTableHtml = HtmlParserTidy.getHtmlFromNode(outerTableNode);
						Node tableNode = HtmlParserTidy.getNodeByTagAndAttr(outerTableHtml, "table", "cellspacing", "0");
						table = HtmlParserTidy.getHtmlFromNode(tableNode);
						table = table.replaceAll("(?s)<!--.*?-->", "");						//remove html comments 
						table = table.replaceAll("(?i)<a href=\".*?\">(.*?)</a>", "$1");	//remove links
					} catch (TransformerException e) {
						e.printStackTrace();
					}
			    }
			    
			    //page navigation links (previous and next)
			    Node pagenavNode = HtmlParserTidy.getNodeByTagAndAttr(rsResponse, "div", "style", "text-align:center; margin:auto auto; font-size:12pt;");
			    String pagenav = "";
			    if (pagenavNode != null)
			    {
			    	try {
						pagenav = HtmlParserTidy.getHtmlFromNode(pagenavNode);
						pagenav = pagenav.replaceAll("(?is)<img.*?>", "")				//remove images
							.replaceAll("(?is)href=\"#\"", "")
							.replaceFirst("(?is)text-align:center", "text-align:left");
						String onStart = "";
						Matcher matcher = Pattern.compile("(?is)<input type=\"hidden\" name=\"onStart\" value=\"(\\d+)\">").matcher(rsResponse);
						if (matcher.find()) onStart = matcher.group(1);
						String link = CreatePartialLink(TSConnectionURL.idGET);			//new link
						link = link + Response.getLastURI().toString()
							.replaceFirst("(?i)http://www.bop.gov", "")
							.replaceFirst("(?i)NameSearch", "moreList")
							.replaceFirst("(?i)&needingMoreList=false", "")
							.replaceFirst("(?i)&direction=(for|back)ward", "")
							.replaceFirst("(?i)&onStart=\\d+", "");
						link = link + "&onStart=" + onStart;
						pagenav = pagenav.replaceAll("(?is)onclick=\"notherSet\\('backward'\\); return false;\"", "href=\"" + link + "&direction=backward\"");
						pagenav = pagenav.replaceAll("(?is)onclick=\"notherSet\\('forward'\\); return false;\"", "href=\"" + link + "&direction=forward\"");
					} catch (TransformerException e) {
						e.printStackTrace();
					}
			    }
			    
				StringBuilder outputTable = new StringBuilder();
				
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, table, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
		            			"<tr><td rowspan=1>" + SELECT_ALL_CHECKBOXES + "</td>" +
		            	        "<th width=\"25\" valign=\"middle\">&nbsp;</th>" +
		            			"<th width=\"135\" align=\"left\" valign=\"middle\">Name</th>" +
		            			"<th width=\"85\" valign=\"middle\">Register #</th>" +
		            			"<th width=\"100\" align=\"center\" valign=\"middle\" class=\"boldTitle\">Age-Race-Sex</th>" +
		            			"<th width=\"140\" align=\"center\" valign=\"middle\" class=\"boldTitle\">Release Date<br>Actual or Projected</th>" +
		            			"<th width=\"110\" align=\"left\" valign=\"middle\" class=\"boldTitle\">Location</th>";
		            	
		            	footer += "</tr></table><br>" + pagenav;
		            	
		            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer += "<br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer += "<br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
				} 
				break;
				
			case ID_DETAILS :
				
				DocumentI document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				}
				break;
			
			case ID_GET_LINK :
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				break;
				
			case ID_SAVE_TO_TSD :
				document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				} else {
					ParseResponse(sAction, Response, ID_DETAILS);
				}
				break;
		}
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		StringBuilder newTable = new StringBuilder();
		newTable.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
		int numberOfUncheckedElements = 0;
		
		if(table != null) {
			try {
				org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(table, null);
				NodeList rows = parser.extractAllNodesThatMatch(new TagNameFilter("tr"));
				newTable.append("<tr>" +
						"<th width=\"25\" valign=\"middle\">&nbsp;</th>" +
            			"<th width=\"135\" align=\"left\" valign=\"middle\">Name</th>" +
            			"<th width=\"85\" valign=\"middle\">Register #</th>" +
            			"<th width=\"100\" align=\"center\" valign=\"middle\" class=\"boldTitle\">Age-Race-Sex</th>" +
            			"<th width=\"140\" align=\"center\" valign=\"middle\" class=\"boldTitle\">Release Date<br>Actual or Projected</th>" +
            			"<th width=\"110\" align=\"left\" valign=\"middle\" class=\"boldTitle\">Location</th>" +
						"</tr>");
				
				for (int i = 2; i < rows.size(); i++) {
					NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
					if(tdList.size() == 6) {	
					
						String documentNumber = tdList.elementAt(2).toPlainTextString();
						ParsedResponse currentResponse = new ParsedResponse();
						responses.put(documentNumber, currentResponse);
						ResultMap resultMap = new ResultMap();
							
						String responseHtml = "<tr>" +
							tdList.elementAt(0).toHtml().replaceFirst("</td>", "&nbsp;&nbsp;</td>") +
							tdList.elementAt(1).toHtml() +
							tdList.elementAt(2).toHtml() +
							tdList.elementAt(3).toHtml() + 
							tdList.elementAt(4).toHtml() +
							tdList.elementAt(5).toHtml() + "</tr>"; 
						
						String releaseDate = tdList.elementAt(4).toPlainTextString().trim();
						DateFormat df1 = new SimpleDateFormat("MM-dd-yyyy");
						DateFormat df2 = new SimpleDateFormat("MM/dd/yyyy");
						try {
							Date release = df1.parse(releaseDate);
							releaseDate = df2.format(release);
						} catch (ParseException e) {
							releaseDate = "";
						}
						
						String descriptionRelease = "";
						if (releaseDate.length()==0)
						{
							descriptionRelease = tdList.elementAt(4).toPlainTextString().trim();
						}
							
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tdList.elementAt(1).toPlainTextString().trim());
						
						resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), tdList.elementAt(1).toPlainTextString().trim());
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
			    		resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), releaseDate);
			    		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), tdList.elementAt(5).toPlainTextString().trim());
			    		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "INMATE LOCATOR");
			    		
			    		resultMap.put("OtherInformationSet.SrcType", "IL");
			    		
			    		String ageRaceSex = tdList.elementAt(3).toPlainTextString().trim();
			    		String age = "", race = "", sex = "";
			    		Matcher  matcher = Pattern.compile("(.+)-(.+)-(.+)").matcher(ageRaceSex);
			    		if (matcher.find())
			    		{
			    			age = matcher.group(1);
			    			race = matcher.group(2);
			    			sex = matcher.group(3);
			    		}
			    		if (!age.matches("\\d+")) age = ""; //e.g. "N/A"
			    		if (!sex.matches("[MF]")) sex = ""; 
			    					    		
			    		ro.cst.tsearch.servers.functions.GenericIL.parseNames(resultMap, getSearch().getID());
			    					    				
						Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
														
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" +
								rows.elementAt(1).toHtml().replaceFirst("(?is)<img.*?>", "Actual or Projected") + responseHtml + "</table>");
						OffenderInformationDocumentI document = (OffenderInformationDocumentI)bridge.importData();
						document.setAge(age);
						document.setRace(race);
						document.setSex(sex);
						document.setReleaseDate(descriptionRelease);
						
						currentResponse.setDocument(document);
						
						String checkBox = "checked";
						if (isInstrumentSaved(documentNumber, document, null)) {
					   		checkBox = "saved";
					   	} else {
					   		numberOfUncheckedElements++;
					   		String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
					   		LinkInPage linkInPage = new LinkInPage(
					   				linkPrefix + documentNumber, 
					   				linkPrefix + documentNumber, 
					   				TSServer.REQUEST_SAVE_TO_TSD);
					   		checkBox = "<input type='checkbox' name='docLink' value='" + 
					   		linkPrefix + documentNumber + "'>";
					   		
			           		if(getSearch().getInMemoryDoc(linkPrefix + documentNumber)==null){
			           			getSearch().addInMemoryDoc(linkPrefix + documentNumber, currentResponse);
			           		}
			           		currentResponse.setPageLink(linkInPage);
			           						    			
					   	}
						currentResponse.setOnlyResponse("<tr><th rowspan=1>"  +  checkBox + "</th>" + responseHtml.substring(responseHtml.indexOf("<tr>") + 4));
						newTable.append(currentResponse.getResponse());
						
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}		
		}
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return responses.values();
	}
}

package ro.cst.tsearch.servers.types;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;


/**
* @author mihaib
**/

public class COBroomfieldAO extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
		
	public COBroomfieldAO(long searchId) {
		super(searchId);
	}

	public COBroomfieldAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
	
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );

		if (hasPin()) {
			//Search by Parcel Number
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}		
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;

		if (rsResponse.indexOf("No matching records") != -1){
			Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
			return;
		} else if (rsResponse.indexOf("Property Not Found") != -1){
			Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
			return;
		}
		
		switch (viParseID) {				
			case ID_SEARCH_BY_ADDRESS :
			
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
											
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }

				}catch(Exception e) {
					e.printStackTrace();
				}
								
				break;
				
			case ID_DETAILS :
				
				String details = "";
				details = getDetails(rsResponse);				
				String pid = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Account Number"), "", true).replaceAll("(?is)</?font[^>]*>", "").trim();
				} catch(Exception e) {
					e.printStackTrace();
				}

				if ((!downloadingForSave)){	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", "ASSESSOR");
					data.put("dataSource", "AO");
					
					if(isInstrumentSaved(pid, null, data)){
		                details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
	                Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } else{      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("Details") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (sAction.indexOf("List") != -1){
						ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				} 
				
				break;
			
			case ID_SAVE_TO_TSD :

				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}
	
	protected String getDetails(String rsResponse){
		
		if (rsResponse.toLowerCase().indexOf("<html") == -1)
			return rsResponse;
		
		rsResponse = rsResponse.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", "&").replaceAll("(?is)</?blockquote>", "");
		
		String contents = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);

			NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "rightDiv"))
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("title"));

			if (divList != null && divList.size() > 0){
				for(int i = 0; i < divList.size(); i++){
					Div div = (Div) divList.elementAt(i);
					div.getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("img")), true);
					div.getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("div")), true);
					
					String atribut = div.getAttribute("title");
					
					contents += "<br /><br />" + atribut + "<br /><br />";
					
					NodeList paragraphList = div.getChildren().extractAllNodesThatMatch(new TagNameFilter("p"));
					paragraphList.keepAllNodesThatMatch(new NotFilter(new TagNameFilter("a")), true);
					
					
					if (paragraphList != null && paragraphList.size() > 0){
						String table = "<table border='1'>";
						for(int p = 0; p < paragraphList.size(); p++ ) {
							if(paragraphList.size() > 1) {
								ParagraphTag prg = (ParagraphTag) paragraphList.elementAt(p);
								
								String rowHtml = prg.toHtml();
								if ("property tax".equals(atribut.toLowerCase())){
									rowHtml = rowHtml.replaceAll("(?is)<p[^>]*>", "<tr>").replaceAll("(?is)</p[^>]*>", "</td></tr>");
									rowHtml = rowHtml.replaceAll("(?is)(<b[^>]*>)", "<td>$1").replaceAll("(?is)(</b[^>]*>)", "$1</td><td>");
								} else {
									rowHtml = rowHtml.replaceAll("(?is)<p[^>]*>", "<tr>").replaceAll("(?is)</p[^>]*>", "</td></tr>");
									rowHtml = rowHtml.replaceAll("(?is)(<strong[^>]*>)", "<td>$1").replaceAll("(?is)(</strong[^>]*>)", "$1</td><td>");
								}
								table += rowHtml;
							}
						}
						contents += table + "</table>";
					}
				}
				
				contents = contents.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
				contents = contents.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			table = table.replaceAll("(?is)</?font[^>]*>\\s*", "").replaceAll("(?is)&amp;", "&").replaceAll("(?is)&nbsp;", " ");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainList = htmlParser.parse(null);
			
			//NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									//.extractAllNodesThatMatch(new HasAttributeFilter("id", "rightDiv"));
			NodeList tableList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "restultTable"));
			
			/*<table id="restultTable">
			<tr>
			    <td>
			        <div>
			            <p>
			                <img style="cursor:pointer" src=/apps/ParcelSearch/Content/tool_zoomin.png onclick="onLinkClickHandler('157536300002');" alt="See on map" />
			                 <b>200</b> <b>MAIN</b> ST<br/>
			                <a href="/apps/ParcelSearch/Parcel/Details/157536300002">157536300002</a><br/>
			                BROOMFIELD SWIMMING POOL ASSN<br />
			            </p>
			        </div>
			    </td>
			</tr>....*/
			
			if (tableList != null && tableList.size() > 0){
				TableTag resultTable = (TableTag) tableList.elementAt(0);
				TableRow[] rows = resultTable.getRows();
				
				for (TableRow tableRow : rows) {
					TableColumn[] columns = tableRow.getColumns();
					
					for (TableColumn tableColumn : columns) {
						NodeList paragraphList = tableColumn.getChildren().extractAllNodesThatMatch(new TagNameFilter("p"), true);

						if (paragraphList != null && paragraphList.size() > 0){

							if(paragraphList.size() > 0) {
								ParagraphTag prg = (ParagraphTag) paragraphList.elementAt(0);
								prg.getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("img")));
								
								NodeList aList = prg.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
								String link = CreatePartialLink(TSConnectionURL.idGET) + ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "").replaceAll("&amp;", "&");
								String parcelNo = ((LinkTag) aList.elementAt(0)).getStringText();
								ResultMap resultMap = new ResultMap();
								
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
								resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNo);
								
								String[] ps = prg.getChildren().toHtml().split("(?is)<br\\s*/?\\s*>");
								String rowHtml = "<tr>";
								for(int p = 0; p < ps.length; p++){
									if (p == 0){
										String address = ps[p].replaceAll("(?is)</?b>", "").trim();
										rowHtml += "<td>" + address + "</td>";
										resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
										resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
									} else if (p == 1){
										rowHtml += "<td>" + ps[p].replaceAll("(?is)\\b(href\\s*=\\s*\\\")[^\\\"]+", "$1" + link) + "</td>";
									} else if (p == 2){
										rowHtml += "<td>" + ps[p] + "</td>";
										resultMap.put("tmpOwner", ps[p].trim());
									}
								}
								rowHtml += "</tr>";
								rowHtml = rowHtml.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
	
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
													
								ro.cst.tsearch.servers.functions.COBroomfieldAO.partyNamesCOBroomfieldAO(resultMap, getSearch().getID());
			
								resultMap.removeTempDef();
								
								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								
								DocumentI document = (AssessorDocumentI)bridge.importData();				
								currentResponse.setDocument(document);
								
								intermediaryResponse.add(currentResponse);
							}
						}
					}
				}
			}
			
			NodeList divNavList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
										.extractAllNodesThatMatch(new HasAttributeFilter("class", "pagination"));
			String pagination = "";
			if (divNavList != null && divNavList.size() > 0){
				pagination = divNavList.elementAt(0).toHtml();
				pagination = pagination.replaceAll("(?is)(href\\s*=\\s*[\\\"|'])", "$1"+ CreatePartialLink(TSConnectionURL.idGET));
			}
		
		String header1 = "<TABLE width='100%' border='1'><tr><th>Address</th><th>Parcel Number</th><th>Owner Name</th></tr>";;
	
		response.getParsedResponse().setHeader(pagination + "<br/ >" + header1);	
		response.getParsedResponse().setFooter("</table><br/ >" + pagination);

		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.COBroomfieldAO.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
		return null;
	}
	
    /**
     * get file name from link
     */
	@Override
	protected String getFileNameFromLink(String link)
	{
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if(dummyMatcher.find())
		{
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
        return fileName;
    }

}
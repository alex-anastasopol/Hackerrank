package ro.cst.tsearch.servers.types;

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.tsrindex.server.UploadImage;

public class TXGenericComptroller extends TemplatedServer {

	private static final long serialVersionUID = -1801510994140090445L;

	public TXGenericComptroller(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}


	protected String getDetails(String response, StringBuilder accountId) {
		if (!response.contains("<html>")) {
			if (response.contains("taxIDno")) 
				accountId.append(getAccountNumber(response));
			return response;
		}
		
		StringBuilder details = new StringBuilder();
		String cleanedDetailsPage = response;
		cleanedDetailsPage = cleanedDetailsPage.replaceAll("(?is)<th([^>]+>)", "<td" + "$1");
		cleanedDetailsPage = cleanedDetailsPage.replaceAll("(?is)<\\s*/\\s*th\\s*>", "</td>");
		cleanedDetailsPage = cleanedDetailsPage.replaceAll("(?is)<img[^>]+>", "");
		cleanedDetailsPage = cleanedDetailsPage.replaceAll("(?is)<div[^>]+>\\s*<a[^>]+>([^<]+)<\\s*/\\s*a\\s*>\\s*</div>", "$1");
		
		HtmlParser3 htmlParser = new HtmlParser3(cleanedDetailsPage);
		NodeList nodeList = htmlParser.getNodeList();
		TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "detail")).elementAt(0);
		
		if (table != null) {
			table.setAttribute("border", "\"1\"");
			if (table.getRowCount() > 0) {
				TableRow row = table.getRow(0);
				row.getColumns()[0].setAttribute("style", "\"align:center; background-color:aliceblue;\"");
			}
			
			String accNo = table.getRow(1).getColumns()[1].getChildrenHTML().trim();
			if (StringUtils.isNotEmpty(accNo)) {
				accountId.append(accNo);
				table.getRow(1).setAttribute("id", "\"taxIDno\"");
			}
			
			details.append(table.toHtml());
			
			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
			String params = "";
			
			if (nodeList != null) {
				String link = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("form"), true)
						.elementAt(0).getText();
				link = link.replaceFirst("(?is)<?form\\s*action\\s*=\\s*\\\"([^\\\"]+)\\\".*", "$1");
				
				for (int i = 0; i < nodeList.size(); i++) {
					String input = nodeList.elementAt(i).toHtml();
					String regExp = "(?is)<input\\s*type\\s*=\\s*\\\"[^\\\"]+\\\"\\s*value\\s*=\\\"([^\\\"]+)\\\"\\s*name\\s*=\\s*\\\"([^\\\"]+)\\\"\\s*>\\s*(?:</input>)?";
					Matcher m = Pattern.compile(regExp).matcher(input);
					if (m.find()) {
						params += m.group(2) + "=" + m.group(1);
						if (i != nodeList.size()-1)
							params += "&";
					} else {
						regExp = "(?is)<input\\s*type\\s*=\\s*\\\"[^\\\"]+\\\"\\s*name\\s*=\\s*\\\"([^\\\"]+)\\\"\\s*value\\s*=\\\"([^\\\"]+)\\\"\\s*>\\s*(?:</input>)?";
						m = Pattern.compile(regExp).matcher(input);
						if (m.find()) {
							params += m.group(1) + "=" + m.group(2);
							if (i != nodeList.size()-1)
								params += "&";
						}
					}
				}
				
				if (params != null) {
					String htmlContent = "";
					link =  dataSite.getDocumentServerLink().replaceAll("\\s", "%20").replaceFirst("(?is)(https?://[^/]+).*", "$1") 
							+ link + "?" + params;
					String rsp = getLinkContents(link);
					HtmlParser3 htmlLD = new HtmlParser3(rsp);
					table = (TableTag) htmlLD.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "width60 centermargin"), true).elementAt(0);
					if (table != null) {
						htmlContent = table.toHtml();
						htmlContent = htmlContent.replaceAll("(?is)<a[^>]+>([^<]+)<\\s*/\\s*a\\s*>", " $1");
						
					} else {
						Node info = htmlLD.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "centermargin width70"), true).elementAt(0);	
						if (info != null) {
							String divContent = info.toHtml();
							divContent = divContent.replaceAll("(?is)<a[^>]+>([^<]+)<\\s*/\\s*a\\s*>", " $1");
							htmlContent = divContent;
						}
					}
					
					details.append("<br/><br/>");
					details.append(htmlContent);
				}
			}	
			
		} else {
			if (cleanedDetailsPage.contains("is not set up for Franchise Tax")) {
				NodeList divs = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(new HasAttributeFilter("style", "margin-left:auto; margin-right:auto; text-align:center;"), true);
				if (divs != null && divs.size() == 2) {
					String printInfo = divs.elementAt(1).toHtml();
					printInfo = printInfo.replaceAll("(?is)<span>\\s*<a[^>]+>[^<]+</a>\\s*</span>", "");
					printInfo = printInfo.replaceAll("(?is)<a[^>]+>([^<]+)</a>", " $1");
					if (StringUtils.isNotEmpty(printInfo)) {
						String accNo = printInfo.trim();
						accNo = accNo.replaceFirst("(?is)<span[^>]+>\\s*Taxpayer Number[^\\d]+(\\d+).*", "$1").trim();
						if (StringUtils.isNotEmpty(accNo)) {
							accountId.append(accNo);
						}
					}
					
					details.append(printInfo);
				}
			}
		}
		
		return details.toString();
	}

	
	@Override
	protected String detailsCasesParse(String action, ServerResponse response, int viParseID, String serverResult, String accountNumber) {
		String originalLink = setOriginalLink(action, response);
		String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

		HashMap<String, String> data = putAdditionalData(serverResult);
		if (!isAccountStatusPage(serverResult)) { // remove the save buttons
			if (isInstrumentSaved(accountNumber, null, putAdditionalData(""))) {
				serverResult += CreateFileAlreadyInTSD();
			} else {
				mSearch.addInMemoryDoc(sSave2TSDLink, serverResult);
				serverResult = addSaveToTsdButton(serverResult, sSave2TSDLink, viParseID);
			}
		}

		response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
		response.getParsedResponse().setResponse(serverResult);
		return serverResult;
	}

	/**
	 * @param response
	 * @return
	 */
	public boolean isAccountStatusPage(String response) {
		return response.contains("Return to: Taxable Entity Search")
				|| response.contains(ro.cst.tsearch.connection.http2.TXGenericComptroller.THIS_IS_ACCOUNT_STATUS_PAGE)
				|| response.contains("Certificate of Account Status page");
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		return RegExUtils.getFirstMatch("(?is)Texas Taxpayer Number</TD>.*?<td[^>]+>\\s*(\\d+)\\s*</td>", serverResult, 1);
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addTooManyResultsMessages("refine Search by Entity Name.");
		getErrorMessages().addNoResultsMessages("Was Not Found.");
		
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getParserInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}

	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml) {
		return super.smartParseDetails(response, detailsHtml);
	}



	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		super.setModulesForAutoSearch(serverInfo);
	}

	public ro.cst.tsearch.servers.functions.TXGenericComptroller getParserInstance() {
		ro.cst.tsearch.servers.functions.TXGenericComptroller instance = ro.cst.tsearch.servers.functions.TXGenericComptroller
				.getInstance();
		return instance;
	}

	@Override
	public void addDocumentAdditionalProcessing(DocumentI doc, ServerResponse response) {
		try {

			String taxIndex = (String) mSearch.getAdditionalInfo(doc.getInstno());
			if (taxIndex == null)
				return;

			SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
			Date sdate = mSearch.getStartDate();

			String basePath = ServerConfig.getImageDirectory() + File.separator + format.format(sdate) + File.separator + searchId;
			File file = new File(basePath);
			if (!file.exists()) {
				file.mkdirs();
			}

			String tiffFileName = doc.getId() + ".tiff";
			String htmlFileName = doc.getId() + ".htm";
			String fullHtmlFileName = basePath + File.separator + htmlFileName;

			File f = new File(fullHtmlFileName);
			f.createNewFile();
			FileUtils.writeTextFile(fullHtmlFileName, taxIndex);
			UploadImage.createTempTIFF(fullHtmlFileName, basePath, null);
			f.delete();

			String path = basePath + File.separator + tiffFileName;
			UploadImage.updateImage(doc, path, tiffFileName, "tiff", searchId);

			Set<String> links = new HashSet<String>();
			links.add(path);
			doc.getImage().setLinks(links);
			doc.setIncludeImage(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();
		Vector<ParsedResponse> parseIntermediary = getParserInstance().parseIntermediary(response, table, searchId, format);

		String checkbox = "";
		for (ParsedResponse parsedResponse : parseIntermediary) {

			String parcelId = parsedResponse.getInstrumentNumber();
			if (isInstrumentSaved(parcelId, null, putAdditionalData(""))) {
				checkbox = "saved";
			} else {
				String linkToDetails = parsedResponse.getResponse();
				linkToDetails = linkToDetails.replaceFirst("(?is).*<a\\s*href\\s*=\\s*\\\"([^\\\"]+).*", "$1");
				checkbox = "<input type='checkbox' name='docLink' value='" + linkToDetails + "'>";
				mSearch.addInMemoryDoc(linkToDetails, parsedResponse);
			}

			String rowHtml = parsedResponse.getResponse();
			rowHtml = rowHtml.replaceAll("(?is)<tr>", "<tr><td width = \"2%\">" + checkbox + "</td>");
			parsedResponse.setOnlyResponse(rowHtml);
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
		}
		String currentHeader = response.getParsedResponse().getHeader();
		
		String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		header = header + "<table border=\"1\">" + "<td width = \"2%\"><div >" + SELECT_ALL_CHECKBOXES
				+ "</div></td>"+currentHeader; //
		response.getParsedResponse().setHeader(header);
		
		String footer  = "\n</table>\n" + "<br/>" + CreateSaveToTSDFormEnd(SAVE_DOCUMENT_BUTTON_LABEL, REQUEST_SAVE_TO_TSD, -1);
		response.getParsedResponse().setFooter(footer);
		
		return parseIntermediary;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    		
    	} else if ((rsResponse.indexOf("Search will return ") > -1 && rsResponse.indexOf("refine Search by Entity Name") > -1)
    			 || ((rsResponse.indexOf("Taxpayer Number") > -1 || rsResponse.indexOf("File Number") > -1 || rsResponse.indexOf("Federal Employer's Identification Number") > -1) && rsResponse.indexOf("Was Not Found") > -1)
    			 || (rsResponse.indexOf("Taxpayer ID must be") > -1 &&  (rsResponse.indexOf(" 11 digits") > -1 || rsResponse.indexOf(" 10 digits") > -1))
    			 || (rsResponse.indexOf("Taxpayer Number") > -1 && rsResponse.indexOf("is not set up for Franchise Tax") > -1)
    			 || rsResponse.indexOf("Invalid Taxpayer ID") > -1)
    	{
			Response.getParsedResponse().setOnlyResponse(NO_DATA_FOUND);
			return;
		} 
		
		switch (viParseID) {	
			case ID_INTERMEDIARY:
			case ID_SEARCH_BY_NAME:	
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if (StringUtils.isEmpty(outputTable.toString())){
					outputTable.append(rsResponse);
				}
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }			
				break;
				
			case ID_SEARCH_BY_INSTRUMENT_NO:
			case ID_SEARCH_BY_PROP_NO:
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
			case REQUEST_SAVE_TO_TSD:
				StringBuilder accountId = new StringBuilder();
				String details = "";
				
				//details = cleanDetails(rsResponse);
				details = getDetails(rsResponse, accountId);
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", DocumentTypes.CORPORATION);
					data.put("dataSource", "BT");
					
					if (isInstrumentSaved(accountId.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);					
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} else {
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();					
				}
				break;
				
			case ID_GET_LINK :
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
				
			default:
				break;
		}
	}	
	
	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> dataMap = null;
		if (dataMap == null) {
			dataMap = new HashMap<String, String>();
		}
		dataMap.put("type", DocumentTypes.CORPORATION);
		dataMap.put("dataSource", "BT");
		return dataMap;
	}
}

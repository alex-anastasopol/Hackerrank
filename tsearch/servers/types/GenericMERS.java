package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.MERSDocument;

public class GenericMERS extends TSServer {

	private static final long serialVersionUID = 7652879692520898135L;

	public GenericMERS(long searchId) {
		super(searchId);
	}
	
	public GenericMERS(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}
	
	public static class Credentials {

		public String	imageId	 = "";
		public String	fileName = "";
		
		public Credentials(String imageId, String fileName) {
			this.imageId = imageId;
			this.fileName = fileName;
		}

		public String toString() {
			return "Credentials(imageId=" + imageId + ",fileName=" + fileName + ")";
		}

	}

	private Credentials getImageCredentials() {
		Credentials cr = (Credentials) getSearch().getAdditionalInfo(getCurrentServerName() + ":credentials");
		for (int i = 0; i < 5 && cr == null; i++) {
			cr = createImageCredentials();
		}
		if (cr != null) {
			getSearch().setAdditionalInfo(getCurrentServerName() + ":credentials", cr);
		}
		return cr;
	}

	private Credentials createImageCredentials() {

		HttpSite httpSite = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			// get captcha
			HTTPResponse httpResponse = httpSite.process(new HTTPRequest("https://www.mers-servicerid.org/sis/kaptcha.jpg"));
			if (!httpResponse.getContentType().contains("image/")) {
				logger.error("Did not obtain \"image/\");");
				return null;
			}

			String fn = Long.toString(new Random().nextLong()).replace("-", "");

			String folderName = getCrtSearchDir() + "temp";
			new File(folderName).mkdirs();
			String fileName = folderName + File.separator + fn + ".jpg";

			InputStream inputStream = httpResponse.getResponseAsStream();
			FileUtils.writeStreamToFile(inputStream, fileName);

			if (!FileUtils.existPath(fileName)) {
				logger.error("Image was not downloaded!");
				return null;
			}

			return new Credentials(fn, fileName);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(httpSite);
		}
		return null;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		boolean isMERSParentSite = false;
		Search search = getSearch();
		if (search!=null) {
			String p2 = search.getP2ParentSite();
			String mersString = Integer.toString(GWTDataSite.MERS_TYPE);
			if (mersString.equals(p2)) {
				isMERSParentSite = true;
			}
		}
		if (!isMERSParentSite) {
			return msiServerInfoDefault;
		}
		
		Credentials credentials = getImageCredentials();
		if (credentials == null) {
			return msiServerInfoDefault;
		}
		
		//set image
		TSServerInfoModule tsServerInfoModule0 = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if (tsServerInfoModule0 != null && tsServerInfoModule0.getFunctionList() != null && tsServerInfoModule0.getFunctionList().size() > 4) {
			
			TSServerInfoFunction func = tsServerInfoModule0.getFunction(4);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 3, 3, 30, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}
		
		TSServerInfoModule tsServerInfoModule1 = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if (tsServerInfoModule1 != null && tsServerInfoModule1.getFunctionList() != null && tsServerInfoModule1.getFunctionList().size() > 12) {
			
			TSServerInfoFunction func = tsServerInfoModule1.getFunction(12);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 5, 5, 30, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}
		
		TSServerInfoModule tsServerInfoModule2 = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX38);
		if (tsServerInfoModule2 != null && tsServerInfoModule2.getFunctionList() != null && tsServerInfoModule2.getFunctionList().size() > 15) {
			
			TSServerInfoFunction func = tsServerInfoModule2.getFunction(15);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 7, 7, 30, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}
		
		TSServerInfoModule tsServerInfoModule3 = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX39);
		if (tsServerInfoModule3 != null && tsServerInfoModule3.getFunctionList() != null && tsServerInfoModule3.getFunctionList().size() > 14) {
			
			TSServerInfoFunction func = tsServerInfoModule3.getFunction(14);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 7, 7, 30, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}
		
		TSServerInfoModule tsServerInfoModule4 = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX40);
		if (tsServerInfoModule4 != null && tsServerInfoModule4.getFunctionList() != null && tsServerInfoModule4.getFunctionList().size() > 9) {
			
			TSServerInfoFunction func = tsServerInfoModule4.getFunction(9);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 3, 3, 30, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}
		
		TSServerInfoModule tsServerInfoModule5 = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX41);
		if (tsServerInfoModule5 != null && tsServerInfoModule5.getFunctionList() != null && tsServerInfoModule5.getFunctionList().size() > 8) {
			
			TSServerInfoFunction func = tsServerInfoModule5.getFunction(8);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 3, 3, 30, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}
		
		TSServerInfoModule tsServerInfoModule6 = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX42);
		if (tsServerInfoModule6 != null && tsServerInfoModule6.getFunctionList() != null && tsServerInfoModule6.getFunctionList().size() > 4) {
			
			TSServerInfoFunction func = tsServerInfoModule6.getFunction(4);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 2, 2, 30, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}
		
		//set notes
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
				
			}
			
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(
							siteName, TSServerInfo.ADDRESS_MODULE_IDX, nameToIndex.get(functionName));
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}

		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;

	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		Credentials cr = (Credentials) getSearch().getAdditionalInfo(getCurrentServerName() + ":credentials");
		if (cr == null && getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
			return ServerResponse.createErrorResponse("Authentication Image Missing");
		}

		try {
			// remove credentials
			if (cr != null) {
				getSearch().removeAdditionalInfo(getCurrentServerName() + ":credentials");
			}
			// perform search
			return super.SearchBy(module, sd);
		} finally {
			if (cr != null)
			{// delete credentials file
				new File(ServerConfig.getFilePath() + cr.fileName).delete();
			}
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		
		case ID_SEARCH_BY_INSTRUMENT_NO:	//Search by MIN
		case ID_SEARCH_BY_ADDRESS:			//Search by Property Address Only
		case ID_SEARCH_BY_MODULE38:			//Search by Individual Borrower and Property Address
		case ID_SEARCH_BY_MODULE39:			//Search by Corporation/Non-Person Entity Borrower and Property Address
		case ID_SEARCH_BY_MODULE40:			//Search by Individual Borrower, SSN and Property Zip Code
		case ID_SEARCH_BY_MODULE41:			//Search by Corporation/Non-Person Entity Borrower, Taxpayer Identification Number and Property Zip Code
		case ID_SEARCH_BY_MODULE42:			//Search by FHA/VA/MI Certificate	
			
			if (rsResponse.indexOf("Wrong MIN format!") > -1) {
				Response.getParsedResponse().setError("Wrong MIN format!");
				return;
			}
			
			if (rsResponse.indexOf("Wrong Zip Code format!") > -1) {
				Response.getParsedResponse().setError("Wrong Zip Code format!");
				return;
			}
			
			if (rsResponse.indexOf("Wrong SSN format!") > -1) {
				Response.getParsedResponse().setError("Wrong SSN format!");
				return;
			}
			
			if (rsResponse.indexOf("Wrong Taxpayer Identification Number format!") > -1) {
				Response.getParsedResponse().setError("Wrong Taxpayer Identification Number format!");
				return;
			}
			
			if (rsResponse.indexOf("No captcha entered!") > -1) {
				Response.getParsedResponse().setError("No captcha entered!");
				return;
			}
			
			if (rsResponse.indexOf("Login Error!") > -1) {
				Response.getParsedResponse().setError("Login Error!");
				return;
			}
			
			if (rsResponse.indexOf("No MINs can be located that match the search criteria entered") > -1) {
				Response.getParsedResponse().setError("No data found.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("The search criteria you entered were too general") > -1) {
				Response.getParsedResponse().setError("The search criteria you entered were too general and the number of MINs located " + 
						"exceeds the limit we are able to return to you. Please narrow your search by adding additional information.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("The number of requests you have made exceeds the maximum number allowed in one minute") > -1) {
				Response.getParsedResponse().setError("The number of requests you have made exceeds the maximum number allowed in one minute. " + 
						"Please try again in 60 seconds");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			rsResponse = rsResponse.replaceAll("(?is)<sup>[^<]+</sup>", "");
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				
	            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
	            	String header = parsedResponse.getHeader();
	               	String footer = parsedResponse.getFooter();                           	
	            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
	            	header += "<table border=\"0\" width=\"100%\"><tr><td>";
	               	header += "\n<table border=\"1\" width=\"100%\" cellspacing=\"0\">\n" +
	            			"<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES + "</th>" +
	            	        "<th align=\"center\">Document</th></tr>";
	            	
	            	footer += "</table></td></tr></table>";
	            	
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

		default:
			break;
		}

	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		StringBuilder newTable = new StringBuilder();
		newTable.append("<table width=\"100%\">");
		int numberOfUncheckedElements = 0;
		
		if(table != null) {
			try {
				org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(table, null);
				NodeList rows = parser.extractAllNodesThatMatch(new TagNameFilter("div"))
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "results"), false);
				newTable.append("<tr><th align=\"center\">Document</th></tr>");
				
				for (int i = 0; i < rows.size(); i++) {
					
					NodeList tableList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"));
					
					String min = "";
					String noteDate = "";
					String minStatus = "";
					String servicer = "";
					
					int startIndex = 0;
					if (tableList.toHtml().contains("This mortgage loan is registered on the MERS System for informational purposes only.")) {
						startIndex++;
					}
					
					String tables = "";
					if (tableList.size()>0) {
						tables += tableList.elementAt(0).toHtml();
						TableTag table1 = (TableTag)tableList.elementAt(0);
						if (table1.getRowCount()>startIndex) {
							TableRow row11 = table1.getRow(startIndex);
							if (row11.getColumnCount()>0) {
								min = row11.getColumns()[0].toPlainTextString().replaceAll("MIN:", "").trim();
							}
							if (row11.getColumnCount()>1) {
								noteDate = row11.getColumns()[1].toPlainTextString().replaceAll("Note Date:", "").trim();
							}
							if (row11.getColumnCount()>2) {
								minStatus = row11.getColumns()[2].toPlainTextString().trim();
							}
						}
					}
					
					if (tableList.size()>1) {
						tables += tableList.elementAt(1).toHtml();
						TableTag table2 = (TableTag)tableList.elementAt(1);
						if (table2.getRowCount()>0) {
							TableRow row12 = table2.getRow(0);
							if (row12.getColumnCount()>0) {
								servicer = row12.getColumns()[0].toPlainTextString().replaceAll("Servicer:", "").trim();
							}
						}
					}
					tables = tables.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");	//remove external links
					String responseHtml = "<td>" + tables + "</td>"; 
										
					ParsedResponse currentResponse = new ParsedResponse();
					responses.put(min, currentResponse);
					ResultMap resultMap = new ResultMap();
					
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "MERS");
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), min);
			    	resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), noteDate);
			    				    	
			    	ArrayList<List> body = new ArrayList<List>();
			    	String[] names = {"", "", "", "", "", ""};
					String[] suffixes = {"", ""} , type, otherType;
			    	
			    	servicer = servicer.replaceAll("(?is)&amp;", "&");
					names = StringFormats.parseNameNashville(servicer, true);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					GenericFunctions.addOwnerNames(servicer, names, suffixes[0], suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(body, true));
										    				
					Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
															
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table width=\"100%\" BORDER='1' CELLPADDING='2'>" +
							"<tr>" + responseHtml + "</tr></table>");
					MERSDocument document = (MERSDocument)bridge.importData();
					document.setMINStatus(minStatus);
					currentResponse.setDocument(document);
							
					String checkBox = "checked";
					if (isInstrumentSaved(min, document, null)) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
						LinkInPage linkInPage = new LinkInPage(
								linkPrefix + min, 
								linkPrefix + min, 
								TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + min + "'>";
					   	
						if(getSearch().getInMemoryDoc(linkPrefix + min)==null){
							getSearch().addInMemoryDoc(linkPrefix + min, currentResponse);
						}
						currentResponse.setPageLink(linkInPage);
			            						    			
				 	}
					currentResponse.setOnlyResponse("<tr><th rowspan=1>" + checkBox + "</th>" + responseHtml);
					newTable.append(currentResponse.getResponse());
										
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

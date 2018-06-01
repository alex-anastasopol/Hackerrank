package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

public class FLGenericDMV extends TSServer {
	
	private static final long	serialVersionUID	= -7421223747800940939L;

	public FLGenericDMV(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public FLGenericDMV(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		
		switch (viParseID) {
				
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			if (rsResponse.indexOf("You must enter the title or VIN of the vehicle") > -1) {
				Response.getParsedResponse().setError("You must enter the title or VIN of the vehicle.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("The title should contain numbers only") > -1) {
				Response.getParsedResponse().setError("The title should contain numbers only.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.indexOf("The title or VIN provided does not match any record in our database") > -1) {
				Response.getParsedResponse().setError("No data found!");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			String lastAddress = "";
			URI uri = Response.getLastURI();
			if (uri!=null) {
				try {
					lastAddress = uri.getPath();
				} catch (URIException e) {
					e.printStackTrace();
				} 
			}
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber, lastAddress);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "DMV");
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
				
			} else {
				smartParseDetails(Response,details);
				
				//remove links
				details = details.replaceAll("(?is)<a[^>]+href=[^>]*>.*?</a>", "");
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			break;
		
		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
			
		default:
			break;
		}
		
	}
	
	public String getVin(NodeList nodeList) {
		String vin = "";
		NodeList vinList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_lblVin"));
		if (vinList.size()>0) {
			vin = vinList.elementAt(0).toPlainTextString().trim();
		}
			
		if (vin.length()<=5) {
			NodeList yearMakeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_lblYearMake"));
			if (yearMakeList.size()>0) {
				vin += yearMakeList.elementAt(0).toPlainTextString().trim().replaceAll("\\W", "");
			}
		}
		return vin;
	}
	
	public String replaceButton(String rsResponse, String detailsString, String address, String viewState, String eventValidation, String buttonName) {
		Matcher ma = Pattern.compile("(?is)<input[^>]+name=\"([^\"]+)\"[^>]+value=\"(" + buttonName + ")\"[^>]*>").matcher(rsResponse);
		if (ma.find()) {
			int seq = getSeq();
			String link = CreatePartialLink(TSConnectionURL.idPOST) + address + "?seq=" + seq;
			Map<String, String> params = new HashMap<String, String>();
			params.put(ro.cst.tsearch.connection.http3.FLGenericDMV.VIEWSTATE, viewState);
			params.put(ro.cst.tsearch.connection.http3.FLGenericDMV.EVENTVALIDATION, eventValidation);
			params.put(ma.group(1), ma.group(2));
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			detailsString = detailsString.replace(ma.group(0), "<a href=\"" + link + "\">" + ma.group(2) + "</a>");
		}
		return detailsString;
	}

	protected String getDetails(String rsResponse, StringBuilder parcelNumber, String address) {
		try {
			
			rsResponse = rsResponse.replaceAll("\"&lt;&lt;", "\"<<");
			rsResponse = rsResponse.replaceAll("&gt;&gt;\"", ">>\"");
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")){
				String vin = getVin(nodeList);
				parcelNumber.append(vin);
				return rsResponse;
			}
			
			String vin = getVin(nodeList);
			parcelNumber.append(vin);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "672px"));
			
			if (tables.size()>0) {
				details.append(tables.elementAt(0).toHtml());
			}
			
			String detailsString = details.toString();
			detailsString = detailsString.replaceAll("(?is)(<span[^>]+id=\"MainContent_lblPageTitle\"[^>]*>)(.+?)(</span>)(\\s*<br\\s*/?>\\s*<br\\s*/?>)?", "$1<h2>$2</h2>$3");
			detailsString = detailsString.replaceAll("(?is)(<table[^>]+)class=\"table1\"([^>]*>)", "$1border=\"1\" style=\"border-collapse: collapse;\"$2");
			detailsString = detailsString
					.replaceAll("(?is)<tr>\\s*<td>\\s*<span[^>]*>\\s*(<b>)?\\s*If\\s+any\\s+of\\s+the\\s+information\\s+on\\s+this\\s+record\\s+needs\\s+to\\s+be\\s+corrected.*?</td>\\s*</tr>", "");
			detailsString = detailsString.replaceAll("(?is)<tr>\\s*<td[^>]+class=\"do-not-print\"[^>]*>.*?</td>\\s*</tr>", "");
			
			String viewState = ro.cst.tsearch.connection.http3.FLGenericDMV.getParamValue(ro.cst.tsearch.connection.http3.FLGenericDMV.VIEWSTATE, rsResponse);
			String eventValidation = ro.cst.tsearch.connection.http3.FLGenericDMV.getParamValue(ro.cst.tsearch.connection.http3.FLGenericDMV.EVENTVALIDATION, rsResponse);
			
			detailsString = replaceButton(rsResponse, detailsString, address, viewState, eventValidation, "<<\\s+Prev\\s+Vehicle");
			detailsString = replaceButton(rsResponse, detailsString, address, viewState, eventValidation, "Next\\s+Vehicle\\s+>>");
			
			detailsString = detailsString.replaceAll("(?is)<input.+?class=\"button\".*?/>", "");
			
			detailsString += "<br><br>";
			
			return detailsString;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
	
		try {
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"DMV");
							
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
							
			String vin = getVin(nodeList);
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), vin);
			
			NodeList titleIssueDateList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_lblTitleIssueDate"));
			if (titleIssueDateList.size()>0) {
				String titleIssueDate = titleIssueDateList.elementAt(0).toPlainTextString().trim();
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), titleIssueDate);
			}
			
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
}

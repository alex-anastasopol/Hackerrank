package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

public class ILLakeTU extends TSServer {

	private static final long serialVersionUID = -8112217024705605174L;
	
	public ILLakeTU(long searchId) {
		super(searchId);
	}

	public ILLakeTU(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}
		
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		
		switch (viParseID) {
		
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			if (rsResponse.indexOf("Invalid Entry - Incorrect length for PIN.") > -1) {
				Response.getParsedResponse().setError("Invalid Entry - Incorrect length for PIN.");
				return;
			}
			
			if (rsResponse.indexOf("No record found for PIN") > -1) {
				Response.getParsedResponse().setError("No Data Found.");
				return;
			}
			
			if (rsResponse.indexOf("Invalid date format.  Please enter a valid date.") > -1) {
				Response.getParsedResponse().setError("Invalid date format. Please enter a valid date.");
				return;
			}
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							ID_DETAILS);
				}

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
		
		default:
			break;
		}

	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {

			StringBuilder details = new StringBuilder();

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);

			/* If from memory - use it as is */
			if (!rsResponse.toLowerCase().contains("<html")) {
				NodeList parcelList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("span"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("id", "lblPIN"));
				if (parcelList.size()>0) {
					String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
					parcelNumber.append(parcelID);
				}
				
				return rsResponse;
			}

			NodeList parcelList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblPIN"));
			if (parcelList.size()>0) {
				String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
				parcelNumber.append(parcelID);
			}

			NodeList divs = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true);
			if (divs.size() > 2) {
				details.append(divs.elementAt(2).toHtml());
			}
			
			String detailsString = details.toString();
			detailsString = detailsString.replaceAll("(?is)<img[^>]+>", "");
			detailsString = detailsString.replaceAll("(?is)<input[^>]+>", "");
			detailsString = detailsString.replaceAll("(?is)<a[^>]+>[^<]+</a>", "");
			
			return detailsString;

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
	
		try {
						
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TU");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
							
			NodeList parcelList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblPIN"));
			if (parcelList.size()>0) {
				String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			}
			
			NodeList addressList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblPropertyLocation"));
			if (addressList.size()>0) {
				String address = addressList.elementAt(0).toHtml().trim();
				Matcher matcher = Pattern.compile("(?is)<span[^>]+>([^<]+)<br\\s*/?>([^<]+)<").matcher(address);
				if (matcher.find()) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), 
							matcher.group(1).replaceAll("\\s{2,}", " ").trim()+"<br>" + 
							matcher.group(2).replaceAll("\\s{2,}", " ").trim());
				}
			}
			
			NodeList yearList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblDetailCaption1"));
			if (yearList.size()>0) {
				String year = yearList.elementAt(0).toPlainTextString().trim();
				Matcher matcher = Pattern.compile("(?is)Sale of taxes (\\d{4})").matcher(year);
				if (matcher.find()) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), matcher.group(1));
				}
			}
			
			NodeList baseAmountList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblDetail1"));
			if (parcelList.size()>0) {
				String baseAmount = baseAmountList.elementAt(0).toHtml().trim();
				Matcher matcher = Pattern.compile("(?is)<span[^>]+>([^<]+)<").matcher(baseAmount);
				if (matcher.find()) {
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), matcher.group(1).replaceAll("[,\\$]", ""));
				}
			}
			
			NodeList totalDueList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblTotal1"));
			if (parcelList.size()>0) {
				String totalDue = totalDueList.elementAt(0).toPlainTextString().trim();
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue.replaceAll("[,\\$]", ""));
			}
			
			ro.cst.tsearch.servers.functions.ILLakeTU.parseAddress(resultMap);
			
		
		} catch (ParserException e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
}

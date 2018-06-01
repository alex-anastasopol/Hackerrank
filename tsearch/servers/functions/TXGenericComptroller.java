package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.AKGenericRO;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.RegExUtils;

public class TXGenericComptroller extends ParseClassWithExceptionTreatment {

	private static TXGenericComptroller _instance = null;

	private static MessageFormat linkFormat;

	public static MessageFormat getLinkFormat() {
		return linkFormat;
	}

	public static void setLinkFormat(MessageFormat linkFormat) {
		TXGenericComptroller.linkFormat = linkFormat;
	}

	private TXGenericComptroller() {
		super(_instance);
	}

	public static TXGenericComptroller getInstance() {
		if (_instance == null) {
			_instance = new TXGenericComptroller();
		}
		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ResultMap resultMap = new ResultMap();
		HtmlParser3 parser = new HtmlParser3(response);
		NodeList tablesList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);
		if (tablesList != null && tablesList.size() >= 1) {
			TableTag resultTable = (TableTag) tablesList.extractAllNodesThatMatch(new HasAttributeFilter("class", "width90 centermargin"), true).elementAt(0);
			
			if (resultTable != null) {
				TableRow[] rows = resultTable.getRows();
				String startLink = createPartialLink(format, TSConnectionURL.idGET);
				
				for (int i = 2; i < rows.length; i += 2) {
					TableColumn[] cols = rows[i].getColumns(); 
					if (cols.length == 3) {
						resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
						resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);
						resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "BT");
						
						TableColumn col = cols[0];  //Name
						if (col != null) {
							LinkTag linkTag = ((LinkTag)col.getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
							String link = startLink + linkTag.extractLink().trim().replaceAll("\\s", "%20");
							
							String name = linkTag.getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(name)) {
								resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
								XXGenericFDIC.setName(resultMap, name);
							}
							
							linkTag.setLink(link);
						}
						
						col = cols[1];  //Taxpayer ID
						if (col != null) {
							String parcelId = col.getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(parcelId)) {
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), parcelId);
							}
						}	
						col = cols[2];  //Zip
						if (col != null) {
							String zipNumber = col.getChildrenHTML().trim();
							zipNumber = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(zipNumber);
							if (StringUtils.isNotEmpty(zipNumber)) {
								resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.ZIP.getKeyName(), zipNumber);
							}
						}
					}
					
					ParsedResponse currentResponse = new ParsedResponse();
					String rowHtml = rows[i].toHtml();
//					currentHtmlRow = currentHtmlRow.replaceAll("<td width = \"2%\"></td>", "<td width = \"2%\">" + checkbox + "</td>");
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					createDocument(searchId, currentResponse, resultMap);
					
					intermediaryResponse.add(currentResponse);
				}

				String tableHeader = rows[0].getChildren().toHtml();

				if (serverResponse != null) {
					String tableFooter = "";
					serverResponse.getParsedResponse().setHeader(tableHeader);
					serverResponse.getParsedResponse().setFooter(tableFooter + "</table>");
				}
			}
		}

		return intermediaryResponse;
	}
	

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		HtmlParser3 parser = new HtmlParser3(response);
		TableTag table = (TableTag) parser.getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("class", "detail"), true).elementAt(0);
		
		if (table != null) {
			String corpInfo = table.getRow(0).getColumns()[0].getChildrenHTML().trim();
			if (corpInfo != null) {
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
				resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);
				String name = ro.cst.tsearch.utils.StringUtils.cleanHtml(corpInfo);
				XXGenericFDIC.setName(resultMap, name);
			}
			
			String cell = StringUtils.defaultIfEmpty(HtmlParser3.getValueFromNextCell(parser.getNodeList(), "Registered Office Street Address", "", true), "").trim();
			String[] split = cell.split("<br>");
			if (split != null && split.length == 2) {
				String address = ro.cst.tsearch.utils.StringUtils.cleanHtml(split[0]).trim();
				parseAddress(address, resultMap);

				String stateZip = split[1].trim();
				// stateZip
				List<String> matches = RegExUtils.getMatches("([\\w\\s]+),\\s*(\\w{2,2})\\s*(.*)", stateZip);
				if (matches.size() == 3) {
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), matches.get(0));
					resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(), matches.get(1));
					resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matches.get(2));
				}
			}
			
			String fileNumber = StringUtils.defaultIfEmpty(HtmlParser3.getValueFromNextCell(parser.getNodeList(), "Texas SOS File Number", "", true), "").trim();
			if (StringUtils.isNotEmpty(fileNumber))
				resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), fileNumber);
			
			String taxPayerNumber = StringUtils.defaultIfEmpty(HtmlParser3.getValueFromNextCell(parser.getNodeList(), "Texas Taxpayer Number", "", true), "").trim();
			if (StringUtils.isNotEmpty(taxPayerNumber))
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), taxPayerNumber);
			
			String recordedDate = StringUtils.defaultIfEmpty(HtmlParser3.getValueFromNextCell(parser.getNodeList(), "SOS Registration Date", "", true), "").trim();
			if (StringUtils.isNotEmpty(recordedDate))
				resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), recordedDate);
		
		} else {
			Node divNode = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true).elementAt(0);
			if (divNode != null) {
				String instrNo = divNode.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true).elementAt(0).toHtml();
				instrNo = instrNo.replaceFirst("(?is)<span[^>]+>\\s*Taxpayer Number[^\\d]+(\\d+).*", "$1").trim();
				if (StringUtils.isNotEmpty(instrNo)) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
					resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);
				}
			}
		}
	}

	@Override
	public void parseAddress(String address, ResultMap resultMap) {
		String streetName = StringFormats.StreetName(address).trim();
		String streetNo = StringFormats.StreetNo(address);
		streetNo = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(streetNo);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(streetNo));

	}

}

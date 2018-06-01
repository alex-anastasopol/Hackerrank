package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSServer;

public class TNKnoxTR extends ParseClass {

	private static TNKnoxTR _instance = null;

	private TNKnoxTR() {
	}

	public static TNKnoxTR getInstance() {
		if (_instance == null) {
			_instance = new TNKnoxTR();
		}
		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> vector = new Vector<ParsedResponse>();
		
		HtmlParser3 parser = new HtmlParser3(response);
		NodeList tableTag = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), false);
		TableTag resultTable = (TableTag) tableTag.elementAt(0);
		TableRow[] rows = resultTable.getRows();
		
		for (int i = 0; i < rows.length; i++) {
			ResultMap resultMap = new ResultMap();
			resultMap.put("OtherInformationSet.SrcType", "TR");
			
			NodeList cells = rows[i].getChildren();
			int j = 3;
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),cells.elementAt(j).toPlainTextString());
			j = 5;
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), cells.elementAt(j).toPlainTextString());
			j = 7;
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), cells.elementAt(j).toPlainTextString());

			parseName("", resultMap);
			parseAddress("", resultMap);
			
			ParsedResponse currentResponse = new ParsedResponse();
			LinkTag linkTag = HtmlParser3.getFirstTag(cells, LinkTag.class, true);
			currentResponse.setPageLink(new LinkInPage(linkTag.extractLink(), linkTag.extractLink(), TSServer.REQUEST_SAVE_TO_TSD));

			String rowHtml = rows[i].toHtml();
			currentResponse.setOnlyResponse(rowHtml);
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
			createDocument(searchId, currentResponse, resultMap);
			vector.add(currentResponse);
		}
		
		serverResponse.getParsedResponse().setHeader("<table>");
		serverResponse.getParsedResponse().setFooter("</table>");
		return vector;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		HtmlParser3 htmlParser3 = new HtmlParser3(response);
		String labelToLookFor = "Owner's name";
		String ownerName = getValueForLabel(htmlParser3, labelToLookFor);
		
		labelToLookFor = "Property address";
		String propertyAddress = getValueForLabel(htmlParser3, labelToLookFor);
		
		labelToLookFor = "Mailing address";
//		String mailingAddress = getValueForLabel(htmlParser3, labelToLookFor);
		
		labelToLookFor = "";
		String parcelId = htmlParser3.getNodePlainTextById("parcel_id");
		if (StringUtils.isNotEmpty(parcelId)){
			parcelId =   parcelId.replaceAll("Property tax details for Property ID :", "");
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);
		}
		
		parseAddress(propertyAddress, resultMap);
		parseName(ownerName, resultMap);
		
		//appraisser parse 
		labelToLookFor = "Appraised Value";
		String appraisedValue = getValueForLabel(htmlParser3, labelToLookFor);
		
		labelToLookFor = "Assessed Value";
		String assessedValue = getValueForLabel(htmlParser3, labelToLookFor);
		
		resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),  ro.cst.tsearch.utils.StringUtils.cleanAmount(appraisedValue));
		resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(),  ro.cst.tsearch.utils.StringUtils.cleanAmount(assessedValue));
		
		//deed info
		labelToLookFor = "Deed Book";
		String deedBook = getValueForLabel(htmlParser3, labelToLookFor);
		resultMap.put(SaleDataSetKey.BOOK.getKeyName(),  deedBook);   
		
		labelToLookFor = "Deed Page";
		String deedPage = getValueForLabel(htmlParser3, labelToLookFor);
		resultMap.put(SaleDataSetKey.PAGE.getKeyName(),  deedPage);
		
		labelToLookFor = "Deed Record Number";
		String deedRecordNumber = getValueForLabel(htmlParser3, labelToLookFor);
		resultMap.put( SaleDataSetKey.DOCUMENT_NUMBER.getKeyName() , deedRecordNumber);
		
		labelToLookFor = "Deed Sale Date";
		String deedSaleDate = getValueForLabel(htmlParser3, labelToLookFor);		
		resultMap.put( SaleDataSetKey.INSTRUMENT_DATE.getKeyName() , deedSaleDate);
		
		labelToLookFor = "Deed Recorded Date";
		String deedRecordedDate = getValueForLabel(htmlParser3, labelToLookFor);
		resultMap.put( SaleDataSetKey.RECORDED_DATE.getKeyName() , deedRecordedDate);
		
		labelToLookFor = "Deed Type";
		String deedType = getValueForLabel(htmlParser3, labelToLookFor);
		resultMap.put( SaleDataSetKey.DOCUMENT_TYPE.getKeyName() , deedType);
		
	}

	/**
	 * @param htmlParser3
	 * @param labelToLookFor
	 * @return
	 */
	public String getValueForLabel(HtmlParser3 htmlParser3, String labelToLookFor) {
		String valueFromAbsoluteCell = htmlParser3.getValueFromAbsoluteCell(1, 0, labelToLookFor);
		if (valueFromAbsoluteCell.contains("No Data")){
			valueFromAbsoluteCell = "";
		}
		return  valueFromAbsoluteCell;
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		Object object = resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty((String) object)){
			object = name;
		}
		resultMap.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), object);
		try {
			TNKnoxEP.stdPisTNKnoxEP(resultMap, 1L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		if (StringUtils.isEmpty(addressOnServer)){
			addressOnServer = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		}
		String streetName = StringFormats.StreetName(addressOnServer).trim();
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
	}

	@Override
	public void setTaxData(String text, ResultMap resultMap) {
		super.setTaxData(text, resultMap);
	}

}

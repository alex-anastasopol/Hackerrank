package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.RegExUtils;

public class TXGenericInsuranceDepartment extends ParseClassWithExceptionTreatment {

	private static TXGenericInsuranceDepartment _instance = null;

	private static MessageFormat linkFormat;

	public static MessageFormat getLinkFormat() {
		return linkFormat;
	}

	public static void setLinkFormat(MessageFormat linkFormat) {
		TXGenericInsuranceDepartment.linkFormat = linkFormat;
	}

	private TXGenericInsuranceDepartment() {
		super(_instance);
	}

	public static TXGenericInsuranceDepartment getInstance() {
		if (_instance == null) {
			_instance = new TXGenericInsuranceDepartment();
		}
		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		HtmlParser3 parser = new HtmlParser3(response);
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		NodeList tableTag = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), false);
		TableTag resultTable = (TableTag) tableTag.elementAt(0);
		if (resultTable != null) {

			TableRow[] rows = resultTable.getRows();

			int tdiColumn = 2;
			int naicColumn = 3;
//			int feinCol = 0;
			int companyNameColumn = 4;
			int statusColumn = 5;
			String startLink = createPartialLink(format, TSConnectionURL.idGET);

			for (int i = 1; i < rows.length; i++) {
				TableColumn[] columns = rows[i].getColumns();
				if (columns.length < 6) {
					System.err.println("Unable to parse intermediary row.");
					continue;
				}
				String parcelId = "";
				parcelId = StringUtils.defaultIfEmpty(columns[tdiColumn].toPlainTextString(), "").trim();
				if (StringUtils.isNotEmpty(parcelId)) {
					ResultMap resultMap = new ResultMap();
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "DI");
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), parcelId);

					Node nameNode = columns[companyNameColumn];
					String name = nameNode.toPlainTextString().trim();
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
					parseName(name, resultMap);

//					String FEIN = columns[feinCol].toPlainTextString();
//					resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), FEIN);

					String companyStatus = columns[statusColumn].toPlainTextString();
					// resultMap.put(SaleDataSetKey., companyStatus);

					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
					resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);

					ParsedResponse currentResponse = new ParsedResponse();
					NodeList linkTag = HtmlParser3.getNodeListByType(rows[i].getChildren(), "a", true);

					if (linkTag != null && linkTag.size() > 0 && linkTag.elementAt(0) instanceof LinkTag) {
						LinkTag detailLinkTag = (LinkTag) linkTag.elementAt(0);
						String link = detailLinkTag.getLink();
						String url = startLink + "/pcci/" + link;
						detailLinkTag.setLink(url);
						detailLinkTag.removeAttribute("onclick");
						detailLinkTag.removeAttribute("target");
						detailLinkTag.removeAttribute("title");
						currentResponse.setPageLink(new LinkInPage(url, url, TSServer.REQUEST_SAVE_TO_TSD));
					}

					String rowHtml = rows[i].toHtml();
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					createDocument(searchId, currentResponse, resultMap);
					intermediaryResponse.add(currentResponse);
				}
			}

			String tableHeader = rows[0].getChildren().toHtml();

			String formAction = RegExUtils.getFirstMatch("name=\"actionForm\" value=\"(.*?)\"", response, 1);
			if (serverResponse != null) {
				serverResponse.getParsedResponse().setHeader("<table  border=0 cellspacing=4>" + tableHeader);
				serverResponse.getParsedResponse().setFooter("</table>");
			}
		}

		return intermediaryResponse;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
		resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"DI");

		String accountNumber = RegExUtils.getFirstMatch("name='tdiNum' value='(.*?)'/>", response, 1);
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), accountNumber);

		String companyName = RegExUtils.getFirstMatch("name='companyName' value='(.*?)'/>", response, 1);
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), companyName);
		parseName(companyName, resultMap);

		String typeCode = RegExUtils.getFirstMatch("name='sysTypeCode' value='(.*?)'/>", response, 1);

		HtmlParser3 parser = new HtmlParser3(response);
		Text text = HtmlParser3.findNode(parser.getNodeList(), "Home City/State:");
		String cityState = StringUtils.defaultIfEmpty(HtmlParser3.getValueFromAbsoluteCell(0, 1, text, "", true), "");
		cityState = ro.cst.tsearch.utils.StringUtils.cleanHtml(cityState);
		String[] split = cityState.split(",");
		if (split.length == 2) {
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), split[0]);
			resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(), split[1]);
		}

		text = HtmlParser3.findNode(parser.getNodeList(), "Date Incorporated/Organized:");
		String dateIncorporated = StringUtils.defaultIfEmpty(HtmlParser3.getValueFromAbsoluteCell(0, 1, text, "", true), "");
		dateIncorporated = ro.cst.tsearch.utils.StringUtils.cleanHtml(dateIncorporated);		
		resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), dateIncorporated);

		text = HtmlParser3.findNode(parser.getNodeList(), "Date Licensed/Eligible/Registered in Texas:");
		String dateRegistered = StringUtils.defaultIfEmpty(HtmlParser3.getValueFromAbsoluteCell(0, 1, text, "", true), "");
		dateRegistered = ro.cst.tsearch.utils.StringUtils.cleanHtml(dateRegistered);
		resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), dateRegistered);

	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		name = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		List body = new ArrayList();
		GenericSunbiz.setName(resultMap, name);;
//		ParseNameUtil.putCompanyInResultMap(resultMap, body, name);

	}

}

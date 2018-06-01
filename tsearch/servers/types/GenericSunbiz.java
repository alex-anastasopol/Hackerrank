package ro.cst.tsearch.servers.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LabelTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;

public class GenericSunbiz extends TemplatedServer {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public GenericSunbiz(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		int[] intermediaryCases = new int[] { ID_SEARCH_BY_NAME, ID_INTERMEDIARY };
		setIntermediaryCases(intermediaryCases);
		int[] link_cases = new int[] { ID_GET_LINK };
		setLINK_CASES(link_cases);
		int[] save_cases = new int[] { ID_SAVE_TO_TSD };

		int[] details_cases = new int[] { ID_DETAILS };
		setDetailsCases(details_cases);

		setSAVE_CASES(save_cases);
		setIntermediaryMessage("Entity Name List");
		setDetailsMessage("Detail by Entity Name");
		getErrorMessages().addServerErrorMessage("We're sorry but the Public Access System is unable to process your request at this time");

		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	protected void setMessages() {
	}

	protected String getAccountNumber(String serverResult) {
		String serverR = serverResult.replaceAll("\\s+", "").replaceAll("&nbsp;", "");

		Pattern p = Pattern.compile("(?ism)DocumentNumber</label>:*<span>([^<]*)</span>");
		Matcher m = p.matcher(serverR);

		String documentNumber = "";

		if (m.find()) {
			documentNumber = m.group(1);
		}

		putAdditionalData(serverResult).put("docno", documentNumber);
		return documentNumber;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		NodeList nodes = new HtmlParser3(response.getResult()).getNodeList();

		NodeList divs = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true);

		NodeList auxNodes = divs.extractAllNodesThatMatch(new HasAttributeFilter("id", "search-results"));

		TableTag resultTable = null;

		if (auxNodes.size() > 0) {
			auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (auxNodes.size() > 0) {
				resultTable = (TableTag) auxNodes.elementAt(0);
			}
		}

		if (resultTable == null) {
			return intermediaryResponse;
		}

		outputTable.append(resultTable.toHtml());

		String linkStart = CreatePartialLink(TSConnectionURL.idGET);

		TableRow[] rows = resultTable.getRows();

		for (int i = 1; i < rows.length; i++) {
			ParsedResponse currentResponse = new ParsedResponse();
			TableRow currentHtmlRow = rows[i];
			LinkTag linkTag = HtmlParser3.getFirstTag(currentHtmlRow.getChildren(), LinkTag.class, true);
			String newLink = linkStart + linkTag.extractLink();
			linkTag.setLink(newLink);

			String rowHtml = currentHtmlRow.toHtml();

			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
			currentResponse.setOnlyResponse(rowHtml);

			currentResponse.setPageLink(new LinkInPage(newLink, newLink, TSServer.REQUEST_SAVE_TO_TSD));

			ResultMap resultMap = new ResultMap();

			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), currentHtmlRow.getColumns()[0].toPlainTextString());

			String docNumber = currentHtmlRow.getColumns()[1].toPlainTextString();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), docNumber);
			resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), docNumber);
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), docNumber);
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "SB");

			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			try {
				document = bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			intermediaryResponse.add(currentResponse);
		}

		response.getParsedResponse().setHeader("<table border=1 align=center>" + rows[0].toHtml());

		String nextLink = "";

		auxNodes = divs.extractAllNodesThatMatch(new HasAttributeFilter("class", "navigationBarPaging"));

		if (auxNodes.size() > 0) {
			Div footerDiv = (Div) auxNodes.elementAt(0);
			footerDiv.removeAttribute("class");

			auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("a"), true);

			for (int i = 0; i < auxNodes.size(); i++) {
				LinkTag l = (LinkTag) auxNodes.elementAt(i);
				l.setLink(linkStart + l.getLink());

				if (l.toHtml().contains("Next")) {
					response.getParsedResponse().setNextLink(nextLink);
				}
			}

			response.getParsedResponse().setFooter("<tr><td colspan=3>" + footerDiv.toHtml() + "</td></tr>" + "</table>");
		} else {
			response.getParsedResponse().setFooter("</table>");
		}

		return intermediaryResponse;
	}

	@Override
	protected String cleanDetails(String response) {
		String html = "";
		HtmlParser3 parser = new HtmlParser3(response);
		NodeList detailTableList = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "div", "class", "searchResultDetail", true);

		if (detailTableList.size() > 0) {
			Node detNode = detailTableList.elementAt(0);
			NodeList n = detNode.getChildren();
			n.remove(n.size() - 1);
			n.remove(n.size() - 1);
			n.remove(n.size() - 1);
			n.remove(n.size() - 1);

			// clean images
			NodeList links = n.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			
			for(int i=0; i<links.size(); i++){
				LinkTag l = (LinkTag) links.elementAt(i);
				//l.removeAttribute("target");
				l.setLink(CreatePartialLink(TSConnectionURL.idGET) + l.getLink());
				if("View Document as Image".equals(l.getAttribute("title"))){
					l.setLink("FAKE");
				}
			}

			html = detailTableList.elementAt(0).toHtml().replaceAll("(?ism)<label", "<br><label")
					.replaceAll("(?ism)</label>", "</label>&nbsp;:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
					.replaceAll("(?ism)<a[^>]*FAKE[^>]*>([^<]*)</a>", "$1");
		}

		return html;
	}

	HashMap<String, String>	dataMap	= null;

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		if (dataMap == null) {
			dataMap = new HashMap<String, String>();
		}
		dataMap.put("type", "CORPORATION");

		return dataMap;
	}

	protected String detailsCasesParse(String action, ServerResponse response, int viParseID, String serverResult, String accountNumber) {
		String originalLink = setOriginalLink(action, response).replaceFirst("&$", "");
		String sSave2TSDLink = "";
		try {
//			originalLink = originalLink.replaceFirst("%20", " ");
			sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
		} catch (Exception e) {
			e.printStackTrace();
		}

		HashMap<String, String> data = putAdditionalData(serverResult);
		
		if (isInstrumentSaved(accountNumber, null, data)) {
			serverResult += CreateFileAlreadyInTSD();
		} else {
			mSearch.addInMemoryDoc(sSave2TSDLink.replaceAll("%", "%25"), serverResult);
			serverResult = addSaveToTsdButton(serverResult, sSave2TSDLink, viParseID);
		}
		
//					if (serverResult.contains(INTERMEDIARY_MESSAGE)) {
			response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
			response.getParsedResponse().setResponse(serverResult);
//					} 
		
//					viParseID = ID_INTERMEDIARY;
		return serverResult;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		try {
			NodeList nodes = new HtmlParser3(Tidy.tidyParse(detailsHtml, null)).getNodeList();

			NodeList nodesAux = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "detailSection corporationName"));

			if (nodesAux.size() > 0) {
				Div d = (Div) nodesAux.elementAt(0);

				Node nd = null;
				
				if ((d.getChildCount() == 5 && (nd=d.getChild(3)) instanceof Span) || (d.getChildCount() == 3 && (nd=d.getChild(2)) instanceof Span)) {
					map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nd.toPlainTextString().replaceAll("&amp;", "&"));
				}
			}

			nodesAux = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "detailSection filingInformation"));

			if (nodesAux.size() > 0) {
				Div d = (Div) nodesAux.elementAt(0);
				
				nodesAux = d.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"),true);
				
				if (nodesAux.size() > 0) {
					// parse filling info
					NodeList children = nodesAux.elementAt(0).getChildren();

					for(int i=0;i<children.size();i++){
						if(children.elementAt(i) instanceof LabelTag){
							if(children.elementAt(i).toHtml().contains("Document Number") && i<children.size()-2 && children.elementAt(i+2) instanceof Span){
								map.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), children.elementAt(i+2).toPlainTextString());
							} else if(children.elementAt(i).toHtml().contains("Date Filed") && i<children.size()-2 && children.elementAt(i+2) instanceof Span){
								map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), children.elementAt(i+2).toPlainTextString());
								break;
							} 
						}
					}
				}
			}
			
			nodesAux = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "detailSection"));

			if (nodesAux.size() > 0) {
				
				for(int i=0;i<nodesAux.size();i++){
					if(nodesAux.elementAt(i).toHtml().contains("Principal Address") && nodesAux.elementAt(i) instanceof Div){
						Div d = (Div) nodesAux.elementAt(i);
						
						if (d.getChildCount() == 6 & d.getChild(4) instanceof Div) {
							map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), d.getChild(4).toHtml());
							break;
						}
					}
				}
			}
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "SB");

			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), DocumentTypes.CORPORATION);
			map.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), DocumentTypes.CORPORATION);

			ro.cst.tsearch.servers.functions.GenericSunbiz.parseAndFillResultMap(map);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

	}

}

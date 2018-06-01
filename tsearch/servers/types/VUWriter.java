package ro.cst.tsearch.servers.types;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

/**
 * @author Vladimir
 *
 */
public class VUWriter extends TSServer {

	private static final long serialVersionUID = 1L;
	
	public VUWriter(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
				// no result
				if (rsResponse.contains("No documents found.")) {
					Response.getParsedResponse().setError(NO_DATA_FOUND);
					Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
					return;
				}
			
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				if(smartParsedResponses.size() == 0) {
					return;
				}

				parsedResponse.setHeader("<table style='border-collapse: collapse' border='2' width='80%' align='center'>");
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_DETAILS:
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "Bulletin");
				
				if (isInstrumentSaved(accountId.toString().trim(), null, data, false)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
				
				break;
			case ID_SAVE_TO_TSD:
				accountId = new StringBuilder();
				details = getDetails(rsResponse, accountId);
				
				String filename = accountId + ".html";
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				parsedResponse.setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
				break;
			case ID_GET_LINK:
				if (rsResponse.contains("Filter By Keyword")) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				} else {
					ParseResponse(sAction, Response, ID_DETAILS);
				}
				break;
		}
	}
	
	private String getDetails(String rsResponse, StringBuilder accountId) {
		StringBuilder details = new StringBuilder();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node detailsNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "bulletin st-documentNode"), true).elementAt(0);
			if(detailsNode != null) {
				
				NodeList toBeRemoved = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "doc-actions clearfix"), true);
				for (int i=0;i<toBeRemoved.size();i++) {
					detailsNode.getChildren().remove(toBeRemoved.elementAt(i));
				}
				
				String det = detailsNode.toHtml();
				det = det.replaceAll("(?is)<dl[^>]*>\\s*<dt>(.*?)</dt>\\s*<dd>(.*?)</dd>", "<tr><td valign=\"top\"><b>$1</b></td><td valign=\"top\">$2</td></tr>");
				det = det.replaceAll("(?is)<dt>(.*?)</dt>\\s*<dd>(.*?)</dd>", "<tr><td valign=\"top\"><b>$1</b></td><td valign=\"top\">$2</td></tr>");
				det = det.replaceAll("(?is)</dl>", "");
				det = det.replaceAll("(?is)<div[^>]+class=\"underline clearfix\"[^>]*>(.*?)</div>", "<table>$1</table>");
				det = det.replaceAll("(?is)<div[^>]+class=\"js-AddReferences\"[^>]*>.*?(<h2>.*?</h2>)(.*?)</div>", "<table><tr><td>$1</td></tr>$2</table>");
				det = det.replaceAll("(?is)<blockquote>(.*?)</blockquote>", "$1");
				
				det = "<table style=\"margin-left:10px;\"><tr><td>" + det + "</td></tr></table>";
				
				details.append(det);
				
				Node instNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "doc-title"), true).elementAt(0);
				if(instNode != null) {
					accountId.append(instNode.toPlainTextString().replaceAll("(?is)\\bBulletin:", "").trim());
				}
				
			} else {
				logger.error("Error while parsing details");
			}
		} catch(Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		return details.toString().replaceAll("(?is)<a [^>]*>(.*?)</a>", "$1").replaceAll("(?is)<img [^>]*>", "");
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList ulList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("data-id", "content"), true);
			if (ulList==null || ulList.size()==0) {
				return intermediaryResponse;
			}
			
			BulletList bulletList = (BulletList)ulList.elementAt(0);
			
			NodeList children = bulletList.getChildren();
			
			for(int i = 0; i < children.size(); i++) {
				if (children.elementAt(i) instanceof Bullet) {
					
					Bullet bullet = (Bullet)children.elementAt(i);
					
					LinkTag linkTag = (LinkTag) bullet.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
					if(linkTag == null) {
						continue;
					}
					processIntermediaryLink(linkTag);
					
					String bulletHtml = bullet.toHtml();
					bulletHtml = bulletHtml.replaceAll("(?is)<div[^>]*>\\s*<ul[^>]*>\\s*<li>(.*?)</li>\\s*</ul>\\s*</div>", "<tr><td>$1</td></tr>");	//type
					bulletHtml = bulletHtml.replaceAll("(?is)<div[^>]+class=\"search-desc\"[^>]*>(.*?)</div>", "<tr><td>$1</td></tr>");					//text
					bulletHtml = bulletHtml.replaceAll("(?is)<a[^>]+>.*?</a>", "<tr><td>$0</td></tr>");													//title
					bulletHtml = bulletHtml.replaceAll("(?is)<li>", "<tr><td><table>");
					bulletHtml = bulletHtml.replaceAll("(?is)</li>", "</table></td></tr>");
					bulletHtml = bulletHtml.replaceAll("(?is)\\bhref=\"([^\"]+)\"", "href=\"" + linkTag.getLink() + "\"");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setParentSite(isParentSite());

					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, bulletHtml);
					currentResponse.setOnlyResponse(bulletHtml);
					currentResponse.setPageLink(new LinkInPage(linkTag.getLink(), linkTag.getLink(), TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.VUWriter.parseIntermediaryRow(bullet);
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (RegisterDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					
				}
				
			}
			
			String footer = "";
			
			NodeList prevNextList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "pagination"), true);
			if (prevNextList!=null && prevNextList.size()>0) {
				String prevNextString = prevNextList.elementAt(0).toHtml();
				prevNextString = prevNextString.replaceAll("(?is)<li[^>]*>(.*?)</li>", "<td>$1</td>");
				prevNextString = prevNextString.replaceAll("(?is)<ul[^>]*>", "<tr align=\"center\"><td><table><tr>");
				prevNextString = prevNextString.replaceAll("(?is)</ul>", "</tr></table></td></tr>");
				prevNextString = prevNextString.replaceAll("(?is)<a[^>]+\\bhref=\"#\"[^>]*>(.*?)</a>", "$1");
				prevNextString = prevNextString.replaceAll("(?is)\\bhref=\"([^\"]+)\"", "href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "$1\"");
				footer += prevNextString;
				Matcher ma = Pattern.compile("(?s)<a[^>]+href=\"([^\"]+)\"[^>]*>Next</a>").matcher(prevNextString);
				if (ma.find()) {
					parsedResponse.setNextLink("<a href=\"" + ma.group(1).replaceAll("(?is)&amp;", "&") + "\">");
				}
			}
			
			footer += "</table>";
			parsedResponse.setFooter(footer);
			
			outputTable.append(page);
		} catch(Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		
		return intermediaryResponse;
	}

	private void processIntermediaryLink(LinkTag linkTag) {
		String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.getLink();
		linkTag.setLink(link);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		
		try {
			String grantee = "";
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node instNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "doc-title"), true).elementAt(0);
			if(instNode != null) {
				map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instNode.toPlainTextString().replaceAll("(?is)\\bBulletin:", "") .trim());
			}
			
			String toNodeText = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "From:"),	"", true);
			if (!StringUtils.isEmpty(toNodeText)) {
				String[] split = grantee.split("(?is)<br>");
				if (split.length>0) {
					grantee = split[0].trim();
				}
			}
			
			String text = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Date:"),	"", false);
			try {
					SimpleDateFormat sdf = new SimpleDateFormat("MMMMM dd, yyyy");
					Date date = sdf.parse(text);
					sdf.applyPattern("MM/dd/yyyy");
					String recDate = sdf.format(date);
					map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
			} catch (Exception e) {
				logger.error("Error while parsing details", e);
			}
			
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bulletin");
			
			ro.cst.tsearch.servers.functions.VUWriter.parseNames(map, grantee);
		} catch(Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		if(getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
			
			TSServerInfoModule module;
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(1, "exactphrase");
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FML_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;M", "L;F;"});
			module.addIterator(nameIterator);
			module.addFilterForNext(new NameFilterForNext(module.getSaObjKey(), searchId, module, false));
			
			modules.add(module);
			
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
}

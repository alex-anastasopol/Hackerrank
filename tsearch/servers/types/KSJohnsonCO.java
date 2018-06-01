package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.ATSConn;
import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class KSJohnsonCO extends TSServer implements TSServerROLikeI {

	private static final long serialVersionUID = 2184482850593133381L;

	private final static String SERVER_ADDR = "www.jococourts.org";
	private final static String SEARCH_PATH = "/index.aspx";

	private boolean downloadingForSave = false;

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		String initialResponse = rsResponse;
		String linkStart;
		int istart = -1, iend = -1;

		switch (viParseID) {

		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_NAME:

			// ////////////////////////////////////////////////////////////////
			// verificam daca vine din search by caseNo, prin a ne uita in
			// rsResponse
			// sa vedem daca exista value="Print Friendly"
			if (rsResponse.indexOf("value=\"Print Friendly\"") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
			}

			if (rsResponse.indexOf("Enter a case number or a name") != -1) {
				Response.getParsedResponse().setError(
						"Enter a case number OR a name");
				return;
			}

			if (rsResponse.contains("Please Use Search Screen To Access Cases")) {
				Response.getParsedResponse().setError(
						"Please Use Search Screen To Access Cases");
				return;
			}

			if (rsResponse.indexOf("Legal Disclaimer") != -1)
				return;

			istart = rsResponse.indexOf("<table width='90%' border=8>");
			if (istart == -1 && rsResponse.contains("Marriage License")) { //to fix doc# 92MR00175
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
			}
			iend = rsResponse.indexOf("</table>", istart) + "</table>".length();

			// XXX: Maybe 'no records found' should not be treated as an error ? 
			if (istart == -1 || iend == -1) {
				Response.getParsedResponse().setError("No results found");
				return;
			}

			rsResponse = rsResponse.substring(istart, iend);
			linkStart = CreatePartialLink(TSConnectionURL.idGET);

			Matcher matcher = Pattern.compile("(?is)<a href='([^\\?]*)\\?([^']*)'>\\s*([A-Z\\d/]+)\\s*</a>").matcher(rsResponse);
			while(matcher.find()) {
				String checkBox = "saved";
				String instNo = matcher.group(3);
				//instNo = instNo.replaceAll("/", "");
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "COURT");
				if(!isInstrumentSaved(instNo, null, data)) {
					checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\""
						+ linkStart + matcher.group(1) + "&" + matcher.group(2) + "\">";
				}

				rsResponse = rsResponse.replace(matcher.group(), checkBox + "&nbsp; &nbsp; &nbsp; &nbsp;" + "<a href='"
						+ linkStart + matcher.group(1) + "&" + matcher.group(2) + "'>" + instNo + "</a>");
			}

			rsResponse = rsResponse.replaceAll("\\+", " ");

			// corectare HTML - inchidere TR-uri
			rsResponse = rsResponse.replaceAll("<TR", "</TR><TR").replaceFirst("</TR>", "");
			rsResponse = rsResponse.replaceAll("(?is)(<TR bgcolor='Florawhite'>\\s*<TH>)(.*</TR>)", "$1" + SELECT_ALL_CHECKBOXES + " $2");
			rsResponse = rsResponse.replaceAll("(?is)'Florawhite'", "'lightgrey'");
			rsResponse = rsResponse.replaceAll("(?is)'wheat'", "'white'");
			rsResponse = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
					+ rsResponse
					+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			
			parser.Parse(Response.getParsedResponse(), rsResponse,
					Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET),
					TSServer.REQUEST_SAVE_TO_TSD);

			break;

		case ID_DETAILS:
			//String fileName = getFileNameFromLink(Response.getQuerry());
			String fileName = getInstrNoFromPage(rsResponse);
			boolean isProbateDoc = false;
			boolean isCivilDoc = false;
			boolean isMarriageDoc = false;
			boolean needsChargesInfo = false;
			String responseForParser = "";
			String originalLink = sAction + "&" + Response.getQuerry();
			String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

			
			/*
			 * Se va mai face o cerere intermediara la un PrintFriendly pentru
			 * document
			 */
			String target = StringUtils.getTextBetweenDelimiters("<form name=\"Form1\" method=\"post\" action=\"", "\" id=\"Form1\">", rsResponse);
			String link = "http://" + SERVER_ADDR + "/" + target;

			HashMap<String, String> conparams = new HashMap<String, String>();

			if (rsResponse.indexOf("cmdDispoPrint") >= 0) {
				conparams.put("cmdDispoPrint", "Print Friendly");
			} else {
				conparams.put("btmPrint", "Print Friendly");
				// conparams.put("Button1", "Print Friendly");
			}
			
			if (rsResponse.contains("btmPlaintiffDef")) {
				isCivilDoc = true;
			}
			
			if (rsResponse.indexOf("txtOriJail") > -1 || rsResponse.indexOf("txtOriProb") > -1 || 
					(rsResponse.indexOf("ARCHIVED CASE") > -1 && rsResponse.indexOf("txtJudgeName") > -1)) { 
				// incorrect req. made for Case History for doc# 98CR02382 (Tsk 9484 - issue 5)
				if (conparams.containsKey("cmdDispoPrint")) {
					conparams.remove("cmdDispoPrint");
				} else if (conparams.containsKey("btmPrint")) {
					conparams.remove("btmPrint");
				}
				conparams.put("btmCRROA", "CASE HISTORY(ROA)");
				needsChargesInfo = true;
			}
			
			if (rsResponse.indexOf("Marriage License") > -1) {
				isMarriageDoc = true;
			}
			
			if (isMarriageDoc) {
				rsResponse = rsResponse.replaceAll("(?is).*<H3", "<H3");
				rsResponse = rsResponse.replaceAll("(?is)<th", "<td");
				rsResponse = rsResponse.replaceAll("(?is)</th", "</td");
				rsResponse = rsResponse.replaceAll("(?is)</th", "</td");
				rsResponse = rsResponse.replaceAll("(?is)</body>\\s*</html>", "");
				responseForParser = rsResponse;
			
			} else {
				conparams.put("__VIEWSTATE", StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", rsResponse));
				conparams.put("__EVENTVALIDATION", StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", rsResponse));

				ATSConn c = new ATSConn(666, link, ATSConnConstants.POST, conparams, null, searchId, miServerID);
				c.setSiteId("KSJohnsonCO");
				c.doConnection();
				
				String tmpRsp =  c.getResult().toString();
				if (tmpRsp.contains("The Error Code is:  'Sub Routine'")) {
					isProbateDoc = true;
				} else {
					rsResponse = tmpRsp;
					isProbateDoc = false;
					if (needsChargesInfo) {
						conparams.clear();
						conparams.put("btmcrDisposition", "Charges");
						conparams.put("__VIEWSTATE", StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", rsResponse));
						conparams.put("__EVENTVALIDATION", StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", rsResponse));
						target = StringUtils.getTextBetweenDelimiters("<form name=\"Form1\" method=\"post\" action=\"",	"\" id=\"Form1\">", rsResponse);
						link = "http://" + SERVER_ADDR + "/" + target;
					}
				}
				
				if (rsResponse.contains("Runtime Error")) {
					Response.getParsedResponse().setError("An error has occured: There was a problem with the request");
					return;
				}

				/* nu se inchide un table */
				Pattern pTable = Pattern.compile("</table>\\s*</body>");
				Matcher mTable = pTable.matcher(rsResponse);
				if (!mTable.find()) {
					rsResponse = rsResponse.replaceAll("</body>", "</table></body>");
				}

				rsResponse = rsResponse.replaceFirst("(?is)<body[^>]*>", "<body>");
				rsResponse = rsResponse.replaceAll("(?is)<\\s*\\*+\\s*Bench notes\\s*\\*+\\s*>", "");
				
				istart = rsResponse.indexOf("<body>");
				iend = rsResponse.indexOf("</body>");
				if (istart > -1 && iend > -1) {
					rsResponse = rsResponse.substring(istart + "<body>".length(),iend);
				}

				if (isProbateDoc || (tmpRsp.contains("CASE HISTORY") && !tmpRsp.contains("Civil CASE HISTORY"))) {
					tmpRsp = rsResponse;
					HtmlParser3 htmlPrs = new HtmlParser3(tmpRsp);
					NodeList list = htmlPrs.getNodeList();
					NodeList caseHistoryList = list;
					
					if (list.size() > 0) {
						list = list.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table1"), true);
						if (list.size() > 0) {
							TableTag tbl = (TableTag) list.elementAt(0);
							if (tbl.getRowCount() > 3) {
								tmpRsp = "<TABLE>";
								for (int i=0; i<tbl.getRowCount(); i++) {
									if (i<4) {
										tmpRsp += tbl.getRow(i).toHtml();
										tmpRsp = tmpRsp.replaceAll("(?is)<span[^>]*>([A-Z\\s]+)</span>", "<b> $1: </b>");
										tmpRsp = tmpRsp.replaceAll("(?is)(<input[^>]+>\\s*</td>)(\\s*<td>)", "$1" + "</tr>  <tr>" + "$2");
										tmpRsp = tmpRsp.replaceAll("(?is)<input name=\\s*(\\\"[^\\\"]+\\\")[A-Z=\\\"\\s\\d]+ value\\s*=\\s*\\\"([^\\\"]+)\\\"[^>]*>", "<span name=" + "$1" + ">" + "$2" + "</span>");
										tmpRsp = tmpRsp.replaceAll("(?is)<input[^>]+>", "");
									} else {
										tmpRsp += "</TABLE>";
										break;
									}
								}
							}
						}
					}
						
					if (rsResponse.contains("CASE HISTORY")) {
						caseHistoryList = caseHistoryList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("width", "90%"), true);
						if (caseHistoryList.size() > 0) {
							TableTag tbl = (TableTag) caseHistoryList.elementAt(0);
							tmpRsp += "<br/> <br/> " + tbl.toHtml();
						}
					}	
					rsResponse = tmpRsp + "<br/> <br/>" + rsResponse;
				}

				if (tmpRsp.contains("Civil CASE HISTORY") && isCivilDoc) {
					if (initialResponse.contains("CASE HISTORY")) {
						tmpRsp = initialResponse;
						HtmlParser3 htmlPrs = new HtmlParser3(tmpRsp);
						NodeList list = htmlPrs.getNodeList();
						
						if (list.size() > 0) {
							list = list.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table1"), true);
							if (list.size() > 0) {
								TableTag tbl = (TableTag) list.elementAt(0);
								if (tbl.getRowCount() > 3) {
									tmpRsp = "<TABLE>";
									for (int i=0; i<tbl.getRowCount(); i++) {
										if (i<4) {
											tmpRsp += tbl.getRow(i).toHtml();
											tmpRsp = tmpRsp.replaceAll("(?is)<span[^>]*>([A-Z\\s]+)</span>", "<b> $1: </b>");
											tmpRsp = tmpRsp.replaceAll("(?is)(<input[^>]+>\\s*</td>)(\\s*<td>)", "$1" + "</tr>  <tr>" + "$2");
											tmpRsp = tmpRsp.replaceAll("(?is)<input[A-Z=\\\"\\s\\d]+ value\\s*=\\s*\\\"([^\\\"]+)\\\"[^>]*>", "$1");
											tmpRsp = tmpRsp.replaceAll("(?is)<input[^>]+>", "");
										} else {
											tmpRsp += "</TABLE>";
											break;
										}
									}
								}
							}
							
							htmlPrs = new HtmlParser3(initialResponse);
							list = htmlPrs.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("width", "90%"), true);
							if (list.size() > 0) {
								TableTag tbl = (TableTag) list.elementAt(0);
								tmpRsp += "<br/> <br/> " + tbl.toHtml();
							}
						}
					}
					
					rsResponse = tmpRsp + "<br/> <br/>";
				}
				
				if (needsChargesInfo && conparams.containsKey("btmcrDisposition")) {
					c = new ATSConn(666, link, ATSConnConstants.POST, conparams, null, searchId, miServerID);
					c.setSiteId("KSJohnsonCO");
					c.doConnection();
					tmpRsp =  c.getResult().toString();
					/* nu se inchide un table */
					mTable = pTable.matcher(tmpRsp);
					if (!mTable.find()) {
						tmpRsp = tmpRsp.replaceAll("</body>", "</table></body>");
					}
					
					
					HtmlParser3 htmlPrs = new HtmlParser3(tmpRsp);
					NodeList list = htmlPrs.getNodeList();
					if (list.size() > 0) {
						list = list.extractAllNodesThatMatch(new TagNameFilter("table"), true);
						
						if (list.size() > 0) {
							String info = "";
							TableTag table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table3"), true)
									.elementAt(0);
							if (table != null) {
								//Jail and Probation info
								info = table.toHtml();
								rsResponse += info + "<br> <br>";
							}
							
							list = list.extractAllNodesThatMatch(new HasAttributeFilter("width", "90%"), true);
							if (list.size() > 0) {
								table = (TableTag) list.elementAt(0);
								if (table != null) {
									//Charges info
									info = table.toHtml();
									rsResponse += info + "<br> <br>";
								}
							}
						}
					}
				}
				
				rsResponse = rsResponse.replaceAll("(?is)\\s*style\\s*=\\s*\\\"[^\\\"]+\\\"", "");
				rsResponse = rsResponse.replaceAll("(?is)\\s*style\\s*=\\s*'[^']+'", "");
				rsResponse = rsResponse.replaceAll("(?is)\\s*border\\s*=\\s*(?:\\\")?\\d+(?:\\\")?", "");
				rsResponse = rsResponse.replaceAll("(?is)width\\s*[=:]\\s*\\\"?[\\d%:px;]+\\\"", "");
				rsResponse = rsResponse.replaceAll("(?is)\\s*bgcolor\\s*=\\s*['\\\"][A-Z#\\d\\s]+['\\\"]", "");
				rsResponse = rsResponse.replaceAll("(?s)<form.*?</form>", "");

				rsResponse = rsResponse.replaceAll("(?s)<a  href.*?</a>", "");
				rsResponse = rsResponse.replaceAll("(?s)(?i)(<table[^>]+)<", "$1"+ "><");
				rsResponse = rsResponse.replaceAll("(<[\\*]+.*[\\*]+>)", "");

				if (rsResponse.indexOf("Nature") == -1) {
					if (rsResponse.indexOf("Marriage License") != -1) {
						rsResponse = rsResponse.replaceAll("(?is)OCTYPE", "<!DOCTYPE");
						rsResponse = rsResponse.replaceAll("(?is)<IMG(.*?)>", "");
						rsResponse = rsResponse.replaceAll("(?is)<TH", "<TD");
						rsResponse = rsResponse.replaceAll("(?is)</TH>", "</TD>");
					}
				}
				rsResponse = rsResponse.replaceAll("</?TEXT>", "");
				rsResponse = rsResponse.replaceAll("(?is).*?<body [^>]*>(.*?)</body>.*", "$1");
				rsResponse = rsResponse.replaceAll("(?is)<a href='INDEX.ASPX'>.*?</a>", "");
				rsResponse = rsResponse.replaceAll("(?is)<input [A-Z\\s=\\\"]+ value\\s*=\\s*\\\"([^\\\"]+)\\\"[^>]*>", "$1");
				rsResponse = rsResponse.replaceAll("(?is)<input[^>]+>", ""); 

				if (rsResponse.contains("Marriage License")) {
					responseForParser = rsResponse;
				} else {
					Pattern p = Pattern.compile("(?i)<input(.*?)/>");
					Matcher m = p.matcher(initialResponse);

					/*
					 * Get all the HTML input fields and put them in a table for the
					 * parser
					 */
					responseForParser = "<TABLE>";
					while (m.find()) {
						try {
							String contents = m.group(1);
							Matcher typeM = Pattern.compile("(?i)type=\"(text|submit)\"").matcher(contents);
							if (typeM.find()) {
								Matcher nameM = Pattern.compile("(?i)name=\"(.*?)\"").matcher(contents);
								nameM.find();
								Matcher valueM = Pattern.compile("(?i)value=\"(.*?)\"").matcher(contents);
								valueM.find();
								String element = nameM.group(1).trim();
								String value = "";
								try {
									value = valueM.group(1).trim();
								} catch (Exception noValue) {}
								if (typeM.group(1).equalsIgnoreCase("text")) {
									responseForParser += "<TR><TD>" + element + "</TD><TD>" + value + "</TD></TR>";
								} else {
									responseForParser += "<TR><TD>" + element + "</TD><TD>" + " &nbsp; " + "</TD></TR>";
								}

							}
						} catch (Exception ignored) {}
					}
					responseForParser += "</TABLE>";

					/* Append the original response to responseForParser */
					responseForParser = responseForParser + rsResponse;

					/* Probate - also get 'case parties' */
					if (initialResponse.contains("btmprCaseParties")) {
						HashMap<String, String> conparams1 = new HashMap<String, String>();
						conparams1.put("__VIEWSTATE", StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"",initialResponse));
						conparams1.put("__EVENTVALIDATION", StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"",initialResponse));
						conparams1.put("btmprCaseParties", "Case Parties");
						ATSConn c1 = new ATSConn(666, link, ATSConnConstants.POST,conparams1, null, searchId, miServerID);
						c1.setSiteId("KSJohnsonCO");
						c1.doConnection();
						String parties = c1.getResult().toString();

						Matcher m1 = Pattern.compile("(<table width='90%' border=8  align='top'><TR bgcolor='Florawhite'>.*?</TABLE>)").matcher(parties);
						try {
							if (m1.find()) {
								parties = m1.group(1);
								parties = parties.replaceAll("(?i)<table.*?>","<table>")
												 .replaceAll("(?i)<td.*?>", "<td>")
												 .replaceAll("(?is)<a.*?>(.*?)</a>", "$1");

								responseForParser = parties .replaceAll("(?i)<tr bgcolor=\"Wheat\"><td>(.*?)</td><td>(.*?)</td>","<tr><td>$1</td><PD>$2</PD>")
															.replaceAll("(?i)<tr.*?>", "<tr>")
															.replaceAll("(?i)<pd>\\s*</pd>","<td></td>")
														+ responseForParser;
							}
						} catch (Exception ignored) {}
					}

					/* Civil - also get 'plaintiff/defendant' */
					if (initialResponse.contains("btmPlaintiffDef")) {
						HashMap<String, String> conparams1 = new HashMap<String, String>();
						conparams1.put("__VIEWSTATE", StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"",initialResponse));
						conparams1.put("__EVENTVALIDATION", StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"",initialResponse));
						conparams1.put("btmPlaintiffDef", "Plaintiff/Defendant");
						ATSConn c1 = new ATSConn(666, link, ATSConnConstants.POST,conparams1, null, searchId, miServerID);
						c1.setSiteId("KSJohnsonCO");
						c1.doConnection();
						String parties = c1.getResult().toString();

						Matcher m1 = Pattern.compile("(<table width='90%' border=8  align='top'><TR bgcolor='Florawhite'>.*?</TABLE>)").matcher(parties);

						try {
							if (m1.find()) {
								parties = m1.group(1);
								parties = parties.replaceAll("(?i)<table.*?>","<table>")
												 .replaceAll("(?i)<tr.*?>", "<tr>")
												 .replaceAll("(?i)<td.*?>", "<td>")
												 .replaceAll("(?is)<a.*?>(.*?)</a>", "$1");
								
								rsResponse += parties;
								
								responseForParser = parties.replaceAll("(?is)<TD>\\((P|D)[0-9]*\\)</TD><TD>(.*?)</TD>","<TD>$1</TD><$1D>$2</$1D>")
														   .replaceAll("(?i)<pd>\\s*</pd>","<td></td>")
														   .replaceAll("(?i)<dd>\\s*</dd>", "<td></td>")
														  + responseForParser;
								responseForParser = responseForParser.replaceAll("(?is)(<td>\\s*[^<]+),(</td>)", "$1");
								
							}
						} catch (Exception ignored) {}
					}
					
					responseForParser = responseForParser.replaceAll("(?is)<input [A-Z\\s=\\\"]+ value\\s*=\\s*\\\"([^\\\"]+)\\\"[^>]*", " $1");
					responseForParser = responseForParser.replaceAll("(?is)<input[^>]+>", ""); 
					responseForParser = responseForParser.replaceAll("(?is)<\\s*(\\$|\\d{3,}+)\\b", "&lt;$1");
					responseForParser = responseForParser.replaceAll("(?is)>\\s*(\\$|\\d{3,}+)\\b", "&gt;$1");
					responseForParser = responseForParser.replaceAll("(?is)<\\s*NAME>", ""); //fix for issue on saving doc# 98CR00119
					responseForParser = responseForParser.replaceAll("(?is)<\\s*ATTACHMENT>", ""); //fix for issue on saving doc# 00JV02861
					responseForParser = responseForParser.replaceAll("(?is)(</table>\\s*<br/?>\\s*<br/?>\\s*)</table>", "$1");
				}
			}
			
			if (!downloadingForSave) {
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "COURT");

				if (isInstrumentSaved(fileName.replaceFirst("\\.html", ""), null, data)) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);

			} else {
				// save html file
				msSaveToTSDFileName = fileName.replaceAll("/", "");
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				parser.Parse(Response.getParsedResponse(), responseForParser, Parser.PAGE_DETAILS);
				Response.getParsedResponse().setResponse(rsResponse);
			}

			break;

		case ID_GET_LINK:
		case ID_SAVE_TO_TSD:

			if (sAction.indexOf("index.aspx") >= 0) {
				if (viParseID == ID_SAVE_TO_TSD) {
					downloadingForSave = true;
				}
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				if (viParseID == ID_SAVE_TO_TSD) {
					downloadingForSave = false;
				}
				break;
			} else if (viParseID == ID_GET_LINK) {
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
			} else if (viParseID == ID_SAVE_TO_TSD) {
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
			}

		}
	}

	private String getInstrNoFromPage(String rsResponse) {
		String instrNo = "";
		HtmlParser3 htmlP = new HtmlParser3(rsResponse);
		NodeList list = htmlP.getNodeList();
		
		if (list.size() > 0) {
			list = list.extractAllNodesThatMatch(new HasAttributeFilter("id", "txtCaseNo"), true);
			if (list.size() > 0) {
				instrNo = list.elementAt(0).toHtml();
				instrNo = instrNo.replaceFirst("(?is)<input[\\s\\\"=A-Z\\d]+ value\\s*=\\s*\\\"([A-Z\\d/]+)\\\"[^>]*>", "$1");
				//instrNo = instrNo.replaceAll("/", "");
			} else {
				if (rsResponse.contains("Marriage License")) {
					list = htmlP.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (list.size() >= 3) {
						instrNo = list.elementAt(0).toPlainTextString();
						instrNo = instrNo.replaceFirst("(?is).*License No\\s*:?\\s*([^$]+)", "$1");
					}
				}
			}
		}
		
		return instrNo + ".html";
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;

    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
    				return true;
    			} else if(!checkMiServerId) {
    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    				if(almostLike != null && !almostLike.isEmpty()) {
    					return true;
    				}
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if( (!checkMiServerId || miServerID==documentI.getSiteId()) /*&& 
			    	    				(documentI.getDocType().equals(docCateg) || "MISCELLANEOUS".equalsIgnoreCase(documentI.getDocType()))*/) {
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
	
	
	protected String getFileNameFromLink(String link) {
		String filename = null;
		try {
			filename = TSServer.getParameter("which", link) + ".html";
		} catch (Exception e) {
		}
		if (filename == null || filename.isEmpty()
				|| filename.equals("null.html")) {
			filename = TSServer.getParameter("txtCaseNo", link) + ".html";
		}

		return filename;
	}

	/*
	 * am suprascris metoda asta pentru a calca pe conditia generala de
	 * identificare a documentului @param fileName @return
	 */
	protected boolean isOverwriteDocNew(String fileName) {
		return true;
	}

	public KSJohnsonCO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {

		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	
	/**
	 * Create Already Present filter
	 * 
	 * @return
	 */
	protected FilterResponse getAlreadyPresentFilter() {
		RejectAlreadyPresentFilterResponse filter = new RejectAlreadyPresentFilterResponse(
				searchId);
		filter.setUseInstr(true);
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		FilterResponse alreadyPresentFilter = getAlreadyPresentFilter();

		FilterResponse scLaCourtsFilter = new ScLaCourtsFilter(searchId);

		FilterResponse nameFilter = new MOGenericCaseNetCO.NameFilter(SearchAttributes.OWNER_OBJECT, searchId);
		
		//Search by Exact Name with Person & Company names
		for (String caseType : new String[] { "Civil", "Marriage License", "Probate" }) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.getFunction(3).setDefaultValue(caseType);
			
			module.addFilter(scLaCourtsFilter);
			module.addFilter(nameFilter);
			module.addFilter(alreadyPresentFilter);
			addBetweenDateTest(module, true, true, false);
			
			//ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] { "L;f;" });
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] { "L;F;" });
			iterator.setInitAgain(true);
			module.addIterator(iterator);

			modules.add(module);
		}
		
		//Search by Business Partial Name only with Company names
		for (String caseType : new String[] { "Civil", "Marriage License", "Probate" }) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			module.getFunction(3).setDefaultValue(caseType);
			
			module.addFilter(scLaCourtsFilter);
			module.addFilter(nameFilter);
			module.addFilter(alreadyPresentFilter);
			addBetweenDateTest(module, true, true, false);
			
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] { "L;;" });
			iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			iterator.setInitAgain(true);
			module.addIterator(iterator);

			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		FilterResponse alreadyPresentFilter = getAlreadyPresentFilter();
		FilterResponse intervalFilter = BetweenDatesFilterResponse
				.getDefaultIntervalFilter(searchId);
		FilterResponse scLaCourtsFilter = new ScLaCourtsFilter(searchId);
		ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(
				searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa
				.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			module.getFunction(0).setIteratorType(
					FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.getFunction(1).setIteratorType(
					FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.getFunction(3).setDefaultValue("Civil");
			module.addFilter(scLaCourtsFilter);
			module.addFilter(alreadyPresentFilter);
			module.addFilter(intervalFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(
					searchId, 0.90d, module).getValidator());
			module.addValidator(DateFilterFactory.getDateFilterForGoBack(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
					.getValidator());
			
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;f;" });
			module.addIterator(nameIterator);

			

			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo
						.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.getFunction(0).setIteratorType(
						FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.getFunction(1).setIteratorType(
						FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.getFunction(3).setDefaultValue("Civil");
				module.addFilter(scLaCourtsFilter);
				module.addFilter(alreadyPresentFilter);
				module.addFilter(intervalFilter);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.addValidator(NameFilterFactory
						.getDefaultTransferNameFilter(searchId, 0.90d, module)
						.getValidator());
				module.addValidator(DateFilterFactory.getDateFilterForGoBack(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
						.getValidator());
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId,
								new String[] { "L;f;" });
				module.addIterator(nameIterator);

				modules.add(module);

			}

		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);

	}

	public static void splitResultRows(Parser p, ParsedResponse pr,
			String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {

		p.splitResultRows(pr, htmlString, pageId, "<TR bgcolor='white'>",
				"</TR>", linkStart, action);
		// remove table header
		/*
		 * Vector rows = pr.getResultRows();
		 * 
		 * if (rows.size()>0) { ParsedResponse firstRow =
		 * (ParsedResponse)rows.remove(0); pr.setResultRows(rows);
		 * pr.setHeader(pr.getHeader() + firstRow.getResponse()); }
		 * 
		 * rows = pr.getResultRows();
		 */
	}

	private static class ScLaCourtsFilter extends FilterResponse {

		private static final long serialVersionUID = 2767178948899823368L;

		public ScLaCourtsFilter(long searchId) {
			super(searchId);
			setThreshold(new BigDecimal("0.95"));
		}

		@Override
		public BigDecimal getScoreOneRow(ParsedResponse pr) {
			if (pr.getSaleDataSetsCount() == 0) {
				return ATSDecimalNumberFormat.ONE;
			}
			String instNo = pr.getSaleDataSet(0)
					.getAtribute("InstrumentNumber");
			return instNo.contains("SC") || instNo.contains("LA") ? ATSDecimalNumberFormat.ZERO
					: ATSDecimalNumberFormat.ONE;
		}

		@Override
		public String getFilterCriteria() {
			return "SC,LA";
		}

		@Override
		public String getFilterName() {
			return "Reject SC,LA Cases";
		}

	}
}
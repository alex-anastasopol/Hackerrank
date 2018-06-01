package ro.cst.tsearch.servers.types;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

import com.stewart.ats.tsrindex.server.UtilForGwtServer;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class KSJohnsonTR extends TSServer {

	static final long serialVersionUID = 10000000;
	
	private static final String PRM_NAME_PARCEL = "parcelID";
	
	private boolean downloadingForSave = false;
	
	public static String old_status = null;
	
	private static final Pattern truri = Pattern.compile("(?is)(<tr>.*?</tr>)\\s*(<tr>.*?</tr>)");
	private static final Pattern currYearTruri = Pattern.compile("(?is)(<table[^>]+>)\\s*(<tr>.*?</tr>)\\s*(<tr>.*?</tr>)\\s*(<tr>.*?</tr>)\\s*(<tr>.*?</tr>)");

	public KSJohnsonTR(long searchId) {
		super(searchId);
	}

	public KSJohnsonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		boolean bResetQueryParam = true;
		String sParcelNo;
		ServerResponse rtrnResponse = new ServerResponse();
		int ServerInfoModuleID = module.getModuleIdx();
		if (ServerInfoModuleID == 2) {
			sParcelNo = module.getFunction(0).getParamValue();
			if (sParcelNo != null) {
				sParcelNo = sParcelNo.replaceAll("- -", "-  -");
				// sParcelNo=sParcelNo.replaceAll("-", "/"); //commented for
				// B1957
				if (sParcelNo.matches("(?i)\\s*[\\dA-Z]{8}\\s+[\\dA-Z]{3,5}\\s*")) {
					// replace YYYYYYYY XXXX with YYYYYYYY-XXXX
					// e.g. replace 3F221328 2006 with 3F221328-2006
					// otherwise results aren't fetched
					String[] parcelTokens = sParcelNo.trim().split("\\s+");
					sParcelNo = parcelTokens[0] + "-" + parcelTokens[1];
				}
				module.getFunction(0).setParamValue(sParcelNo);
			} else {
				if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() != Search.PARENT_SITE_SEARCH) {// cautare
																																					// automata
					return super.SearchBy(true, module, sd);
				} else {
					rtrnResponse.getParsedResponse().setError(
							"We could not process your request because you did not enter a complete Parcel ID Number<br>"
									+ "Please go back and fill in the complete Parcel ID Number.");
					throw new ServerResponseException(rtrnResponse);
				}
			}
		} else
			bResetQueryParam = true;
		return super.SearchBy(bResetQueryParam, module, sd);

	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponce = Response.getResult();
		String initialResponse = rsResponce;
		int istart = -1, iend = -1;

		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_TAX_BIL_NO:
			if (rsResponce.indexOf("An unexpected error has occurred") != -1)
				return;

			String prev = null,
			next = null,
			viewState = null,
			formAction = null;
			Map<String,String> m = parseIntermPage(rsResponce);
			if (m == null)
				return;
			istart = rsResponce.indexOf("<TABLE ");
			istart = rsResponce.indexOf("<TABLE ", istart + 1);
			istart = rsResponce.indexOf("<table ", istart + 1);
			istart = rsResponce.indexOf("<table ", istart + 1);
			istart = rsResponce.indexOf("<table ", istart + 1);
			//istart = rsResponce.indexOf("<table ", istart + 1);
			if (istart == -1)
				return;
			iend = rsResponce.indexOf("</table>", istart) + "</table>".length();
			rsResponce = rsResponce.substring(istart, iend);
			istart = rsResponce.indexOf("<td colspan=\"2\">");
			rsResponce = rsResponce.replaceAll("<td colspan=\"2\">.*</td>", "");

			String linkStart = CreatePartialLink(TSConnectionURL.idGET);
			// View Image link fix
			// rsResponce = sBfr.toString();
			rsResponce = rsResponce.replaceAll("<a href='retaxbills.aspx\\?" + "([^>]*)>", "<a href='" + linkStart + "retaxbills.aspx&$1>");

			rsResponce = rsResponce.replaceFirst("(?s)<tr.*?Address.*?</tr>", "");
			rsResponce = rsResponce.replaceFirst("(?s)<tr[^>]*>\\s*?</tr>", "");
			rsResponce += "<br>";

			// rsResponce=(String)m.get("rsResponce");
			formAction = (String) m.get("formAction");
			viewState = (String) m.get("viewState");
			prev = (String) m.get("prev");
			next = (String) m.get("next");
			m = null;
			if (prev != null) {
				linkStart = CreatePartialLink(TSConnectionURL.idPOST) + "/realestate.aspx&" + formAction;
				rsResponce += "<form name=\"frmRealEstatePrev\" action=\"" + linkStart + "\" method=\"post\">";
				rsResponce += "<input type=\"hidden\" name=\"__EVENTTARGET\" value=\"" + prev.replaceAll("\\$_", ":_") + "\">\n";
				rsResponce += "<input type=\"hidden\" name=\"__EVENTARGUMENT\" value=\"\">";
				rsResponce += "<input type=\"hidden\" name=\"__VIEWSTATE\" value=\"" + viewState + "\">";

				rsResponce += "</form>";
			}
			if (next != null) {
				linkStart = CreatePartialLink(TSConnectionURL.idPOST) + "/realestate.aspx&" + formAction;
				rsResponce += "<form name=\"frmRealEstateNext\" action=\"" + linkStart + "\" method=\"post\">";
				rsResponce += "<input type=\"hidden\" name=\"__EVENTTARGET\" value=\"" + next.replaceAll("\\$_", ":_") + "\">\n";
				rsResponce += "<input type=\"hidden\" name=\"__EVENTARGUMENT\" value=\"\">";
				rsResponce += "<input type=\"hidden\" name=\"__VIEWSTATE\" value=\"" + viewState + "\">";

				rsResponce += "</form>";
			}
			if (prev != null) {
				rsResponce += "<a href='javascript:document.frmRealEstatePrev.submit()'>Previous</a>";
			}
			if (next != null) {
				rsResponce += "&nbsp;&nbsp;";
				rsResponce += "<a href='javascript:document.frmRealEstateNext.submit()'>Next</a>";
			}
			rsResponce += "<br>";

			parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);

			break;

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:

			istart = -1;
			iend = -1;
			istart = rsResponce
					.indexOf("<table cellspacing=\"0\" cellpadding=\"2\" bordercolor=\"#6DA3BF\" border=\"0\" id=\"dgTaxBills\" style=\"border-color:#6DA3BF;border-width:1px;border-style:Solid;border-collapse:collapse;\">");
			if (istart == -1)
				return;
			iend = rsResponce.indexOf("</table>", istart) + "</table>".length();
			rsResponce = rsResponce.substring(istart, iend);
			
			String tablesTaxHistory = "";
			int ijk = 0;
			// concateneaza toate html-urile cu taxe detaliate
			int iwhere = 1;
			String rcptLink;

			String newHtml = "";

			do {

				istart = rsResponce.indexOf("<a href='", iwhere) + "<a href='".length();
				rsResponce = rsResponce.replaceAll(">\\s*View", ">View");
				iend = rsResponce.indexOf("'>View", istart);

				if (istart != -1 && iend != -1) {

					iwhere = iend;
					rcptLink = rsResponce.substring(istart, iend).replaceAll(" ", "%20").replaceAll("taxbill", "friendly");
					
					// refriendly.aspx?parcelID=EP90000003%200005&taxYear=2007
					
					if(rcptLink.contains("<tr") || rcptLink.contains("<td")){
						break;
					}

					HttpSite site = HttpManager.getSite("KSJohnsonTR", searchId);
					try {
						HTTPRequest request = new HTTPRequest("http://taxbill.jocogov.org/" + rcptLink);
						rcptLink = site.process(request).getResponseAsString();
					} catch (Exception e) {
						rcptLink = "";
						logger.error("Error while following " + rcptLink, e);
					} finally {
						HttpManager.releaseSite(site);
					}

					if (rcptLink.indexOf("PAYMENT HISTORY") < 0) {
						int index = rcptLink.indexOf("FEES DUE");
						if (index >= 0) {
							index = rcptLink.indexOf("</table>", index);
							if (index >= 0) {
								index += 8;
								rcptLink = rcptLink.substring(0, index)
										+ "<table id=\"tblPayments\" cellspacing=\"0\" cellpadding=\"0\" align=\"Left\" border=\"0\" style=\"width:92%;border-collapse:collapse;\">"
										+ "<tr align=\"Center\" style=\"color:White;background-color:#607090;font-weight:bold;\">"
										+ "<td align=\"Center\" colspan=\"2\">PAYMENT HISTORY</td>"
										+ "</tr><tr align=\"Center\" style=\"color:White;background-color:#607090;\">"
										+ "<td align=\"Center\" colspan=\"1\">Date Paid</td><td align=\"Center\" colspan=\"1\">Amount Paid</td></tr></table>"
										+ rcptLink.substring(index);
							}
						}
					}

					rcptLink = rcptLink.replaceAll("</TABLE>", "</table>");
					istart = 0;
					iend = 0;
						//pentru 3323
						Node tbl = HtmlParserTidy.getNodeById(rcptLink, "tblPayments", "table");
						String html = "";
						try {
							html = HtmlParserTidy.getHtmlFromNode(tbl);
						} catch (TransformerException e) {
							e.printStackTrace();
						}
						
						if (ijk == 0){
							html = html.replaceAll("(?is)<input[^>]+>", "");
							html = html.replaceAll("</table>", "");
							Matcher ma = currYearTruri.matcher(html);
							if (ma.find()) {// ca sa fie ordonate
								tablesTaxHistory = tablesTaxHistory + ma.group(1) + ma.group(2) + ma.group(3) + ma.group(5) + ma.group(4);
							} else {
								tablesTaxHistory = html;
							}
							
							tablesTaxHistory = tablesTaxHistory.replaceAll("(?is)Date\\s*Paid", "Date Paid#");
						} else {
							html = html.replaceAll("(?is).*?Receipt</td>\\s*</tr>(.*?)</table>", "$1");
							html = html.replaceAll("(?is)<input[^>]+>", "");
							Matcher ma = truri.matcher(html);
							if (ma.find()){// ca sa fie ordonate
								tablesTaxHistory = tablesTaxHistory + ma.group(2)+ ma.group(1);
							}
						}
						ijk ++;
					
					for (int i = 0; i < 3; i++) {
						istart = rcptLink.indexOf("<table ", istart + 1);
					}

					for (int i = 0; i < 10; i++) {
						iend = rcptLink.indexOf("</table>", iend + 1) + "</table>".length();
					}

					if (istart != -1 && iend != -1) {

						int is = 0;
						int ie = 0;
						for (int i = 0; i < 14; i++) {
							is = rcptLink.indexOf("<table ", is + 1);
						}

						for (int i = 0; i < 15; i++) {
							ie = rcptLink.indexOf("</table>", ie + 1) + "</table>".length();
						}
						String tmpLegal = rcptLink.substring(is, ie);
						rcptLink = rcptLink.substring(istart, iend);
						rcptLink = rcptLink + tmpLegal + "<br><br><hr>";
						newHtml += rcptLink + " <br><br><br>&nbsp;<!-- NEW PAGE --> ";

					} else
						break;

				} else
					break;

			} while (true);

			rsResponce = newHtml + "</div></td></tr></table></td></tr></table>";

			// get parcel id
			String keyNumber = StringUtils.getTextBetweenDelimiters("<span id=\"lblPropertyNumber\">", "</span>", rsResponce).replaceAll(" ", "");

			rsResponce = rsResponce.replaceAll("bgColor=\"#ced5db\"", "");
			rsResponce = rsResponce.replaceAll("bgColor=\"#607090\"", "");
			rsResponce = rsResponce.replaceAll("bgcolor=\"#607090\"", "");
			rsResponce = rsResponce.replaceAll("color=\"white", "color=\"black");
			rsResponce = rsResponce.replaceAll("COLOR: white", "COLOR: black");
			rsResponce = rsResponce.replaceAll("background-color:#CED5DB;", "");
			rsResponce = rsResponce.replaceAll("BACKGROUND-COLOR: #ced5db;", "");
			rsResponce = rsResponce.replaceAll("BACKGROUND-COLOR: #607090;", "");
			rsResponce = rsResponce.replaceAll("color:White;background-color:#607090;", "color:black;");
			rsResponce = rsResponce.replaceAll("(?is)<input [^>]*value=\"View eReceipt\"[^>]*>", "");
			rsResponce = rsResponce.replaceAll("(?is)<input [^>]*value=\"View eBill\"[^>]*>", "");
			rsResponce = rsResponce.replaceAll("(?is)Click here to generate a printable bill.", "");
			rsResponce = rsResponce.replaceAll("(?ism)<TR bgColor=\"#6da3bf\">\\s*<TD width=\"10%\">&nbsp;</TD>\\s*<TD align=\"center\" width=\"80%\" height=\"19\"><FONT color=\"#ffffff\"><STRONG>PRINT YOUR TAX\\s*BILL ONLINE!</STRONG></FONT>\\s*</TD>", "");
			rsResponce = rsResponce.replaceAll("(?is)<img[^>]*>", "");
			
			tablesTaxHistory = tablesTaxHistory.replaceAll("(?is)style=\\\"width[^\\\"]+\\\"", "style=\"display:none\"");
			rsResponce = rsResponce + tablesTaxHistory + "</table>";
			
			if ((!downloadingForSave)) {

				String qry = Response.getQuerry();
				qry = "dummy=" + keyNumber + "&" + qry;
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (FileAlreadyExist(keyNumber + ".html") ) {
					rsResponce += CreateFileAlreadyInTSD();
				} else {
					rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));

				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);

			} else {

				// for html
				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_DETAILS);
				
			}

			break;

		case ID_GET_LINK:
		case ID_SAVE_TO_TSD:
			
			if (sAction.equals("/realestate.aspx"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
			else if (viParseID == ID_GET_LINK)
				ParseResponse(sAction, Response, ID_DETAILS);
			else {// on save
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
			}
			
			break;

		}
	}

	/**
	 * 
	 * @param rsResponce
	 * @return
	 */
	protected Map<String,String> parseIntermPage(String rsResponce) {

		int iTmp = rsResponce.indexOf("action=\"") + 8;
		iTmp = rsResponce.indexOf('?', iTmp) + 1;
		String formAction = rsResponce.substring(iTmp, rsResponce.indexOf('"', iTmp)).replaceAll("&amp;", "&");

		int startIndex = rsResponce.indexOf("__VIEWSTATE\" value=\"") + 20;
		String viewState = rsResponce.substring(startIndex, rsResponce.indexOf("\" />", startIndex));
		try {
			viewState = URLEncoder.encode(viewState, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}

		iTmp = rsResponce.indexOf("<td colspan=\"2\">");
		if (iTmp == -1) {
			return null;
		}// no result

		int ii = rsResponce.indexOf("</td>", iTmp);
		String str = rsResponce.substring(iTmp, ii + 5);

		String prev = null, next = null;

		next = str.replaceAll(".*<a[^\\(]*\\('([^']*)[^>]*>[^<]*Next Page.*", "$1");
		if (next.equals(str))
			next = null;
		prev = str.replaceAll(".*<a[^\\(]*\\('([^']*)[^>]*>[^<]*Previous Page.*", "$1");
		if (prev.equals(str))
			prev = null;

		Map<String,String> m = new HashMap<String,String>();
		m.put("rsResponce", rsResponce);
		m.put("formAction", formAction);
		m.put("viewState", viewState);
		m.put("prev", prev);
		m.put("next", next);
		
		return m;
	}


	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule m = null;

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String parcelId = sa.getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();

		if (!"".equals(parcelId)) {
			l.add(new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)));
		}

		if (!"".equals(streetName)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX2));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_NOT_EMPTY);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addValidator(defaultLegalValidator);
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	@Override
	protected String getFileNameFromLink(String link) {
		link = link.replaceAll(" ", "") + "&";
		String parcelId = StringUtils.getTextBetweenDelimiters(PRM_NAME_PARCEL + "=", "&", link).trim();
		return parcelId + ".html";
	}

	@SuppressWarnings("rawtypes")
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {

		htmlString = htmlString.replaceAll("<tr style=\"color:White;background-color:#000066;\">\\W+.*\\W+</tr>", "");
		p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart, action);

		// insert dummy parcelid for getfilefromlink()
		Vector rows = pr.getResultRows();

		rows = pr.getResultRows();
		for (int i = 0; i < rows.size(); i++) {
			ParsedResponse row = (ParsedResponse) rows.get(i);
			PropertyIdentificationSet data = row.getPropertyIdentificationSet(0);
			String pid = data.getAtribute("ParcelID");
			// now instrumentnumber must be placed as a dummy param in HTTP
			// link query
			row.getPageLink().setOnlyLink(row.getPageLink().getLink().replaceFirst("\\.html", ".html&dummy=" + pid));
			row.getPageLink().setOnlyOriginalLink(row.getPageLink().getOriginalLink().replaceFirst("\\.html", ".html&dummy=" + pid));
		}
	}

	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent,
			boolean forceOverritten) {
		ADD_DOCUMENT_RESULT_TYPES result =  super.addDocumentInATS(response, htmlContent, forceOverritten);
		try {
			if(result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				UtilForGwtServer.uploadDocumentToSSF(searchId, response.getParsedResponse().getDocument());
			}
		} catch (Exception e) {
			logger.error("Error while saving index for " + searchId, e);
		}
		return result;
	}
	
}

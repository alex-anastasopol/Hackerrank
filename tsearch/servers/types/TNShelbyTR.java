package ro.cst.tsearch.servers.types;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.ParcelIDFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

public class TNShelbyTR extends TSServer {

	static final long serialVersionUID = 10000000;
	
	public TNShelbyTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		boolean emptyPid = "".equals(sa.getAtribute(SearchAttributes.LD_PARCELNO));
		boolean emptyStreet = "".equals(sa.getAtribute(SearchAttributes.P_STREETNAME));

		FilterResponse addressFilter 	= 
			AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		FilterResponse nameFilterHybrid = 
			NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		FilterResponse pinFilter = 
			new ParcelIDFilterResponse(SearchAttributes.LD_PARCELNO,searchId);

		TSServerInfoModule m;

		if (!emptyPid) {
			try {
				sa.TNShelbyTRAOtoTRPID();
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				//m.setIteratorType(ModuleStatesIterator.TYPE_PARCEL_ID_FAKE);
				//m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
				l.add(m);
			} catch(Exception e){
				String PID = sa.getAtribute(SearchAttributes.LD_PARCELNO);
				SearchLogger.logWithServerName("I will not search with parcel id = " + PID + " because I can't convert the AO pid to TR pid", searchId, SearchLogger.ERROR_MESSAGE, getDataSite());
			}
		}

		if (!emptyStreet) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_NOT_EMPTY);
			m.addFilter(pinFilter);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			l.add(m);
		}

		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
//			m.setIteratorType(ModuleStatesIterator.TYPE_NAME__FIRST_NOT_EMPTY);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);
			
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	/**
	 * @param rsResponce
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponce = Response.getResult();
		String initialResponse = rsResponce;

		logger.debug("TNShelbyTR: Entering ParseResponse with viParceId = " + viParseID);
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
			
			if(rsResponce.matches("(?is).*There\\s+were\\s+no\\s+records\\s+found.*")) {
				logger.info("TNShelbyTR: ParseResponse END: No records found!");
				return;
			}
			
			logger.debug("TNShelbyTR: ParseResponse continue: some records found");
			
			String interm = getIntermediateResults(rsResponce);
			
			if(interm == null) {
				logger.info("ParseResponse END: No records found!");
				return;
			}
			
			logger.debug("TNShelbyTR: interm is not null");
			
			// parse and store parameters on search
			Form form = new SimpleHtmlParser(initialResponse).getForm("ctl00");
			Map<String, String> params = form.getParams();
			params.remove("__EVENTTARGET");
			params.remove("__EVENTARGUMENT");
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			logger.debug("TNShelbyTR: Stored additional parameters");
			
			// rewrite intermediate links
			interm = rewriteIntermediateLinks(interm, "TaxQry/" + form.action, seq);
			
			logger.debug("TNShelbyTR: Rewrote intermediate links");
			
			parser.Parse(
					Response.getParsedResponse(), 
					interm, 
					Parser.PAGE_ROWS_NAME,
					getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD);
			logger.debug("TNShelbyTR: finished parsing");
			
			break;
		case ID_SEARCH_BY_PARCEL:
		case ID_GET_LINK:
		case ID_SAVE_TO_TSD:
		
			
			String details = getDetails(rsResponce);
			
			if (viParseID != ID_SAVE_TO_TSD) {
				if(rsResponce.matches("(?is).*There\\s+were\\s+no\\s+receivables\\s+found\\s+for\\s+.*parcel\\s+ID\\s+number:\\s+.*"))
				{	logger.info("TNShelbyTR: ParseResponse END: No records found!");
					return;
				}
				
				String parcelNo = getParcelNoFromResponse(details);
				String originalLink = getOriginalLink(parcelNo);
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				if (isInstrumentSaved(parcelNo,null,data)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);
				
				
			} else {

				String parcelNo = getParcelNoFromResponse(rsResponce);
				
				msSaveToTSDFileName = parcelNo + ".html" ;
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();        		
				org.htmlparser.Parser detailsParser = org.htmlparser.Parser.createParser(details, null);
				NodeList nodeList;
				try {
					nodeList = detailsParser.parse(null);
					NodeList notePanelNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "notePanel"), true);
					if (notePanelNode.size() != 0)
						nodeList.remove(notePanelNode.toNodeArray()[0]);
					details = nodeList.toHtml();
				} catch (ParserException e) {
					e.printStackTrace();
				}
				parser.Parse(Response.getParsedResponse(),details,Parser.PAGE_DETAILS,getLinkPrefix(TSConnectionURL.idGET),TSServer.REQUEST_SAVE_TO_TSD);               
				Response.getParsedResponse().setOnlyResponse(details);     
			}
			break;
		default:
			break;
		}
	}
	
	
	/**
	 * Get details result page
	 * @param response
	 * @return
	 */
	protected String getDetails(String response) {
		
		// if from memory - use it as is
		if(!response.contains("<html")){
			return response;
		}
		
		// isolate the details table
		int istart = response.indexOf("<!-- Display Owner info -->");
		if(istart == -1)
			istart = 0;
		istart = response.indexOf("<table", istart);
		int iend = response.indexOf("<!-- Back -->");
		iend = response.lastIndexOf("</table>", iend);
		if (istart == -1 || iend == -1) {
			return "";
		}
		String details = response.substring(istart, iend + "</table>".length());
		
		/*
		istart = details.indexOf("<!-- Display as of -->");
		if(istart >= 0) {
			istart = details.substring(0,istart).lastIndexOf("</table>");
			if(istart >= 0) {
				iend = details.indexOf("<tr", istart);
				if(iend >= 0) {
					details = details.substring(0,istart) + details.substring(iend);
				}
			}
		}
		*/
		
		istart = details.indexOf("<!-- Total -->");
		if(istart >= 0) {
			istart = details.substring(0,istart).lastIndexOf("</table>");
			if(istart >= 0) {
				iend = details.indexOf("<tr", istart);
				if(iend >= 0) {
					details = details.substring(0,istart) + details.substring(iend);
				}
			}
		}
		
		details = details.replaceAll("(?is)<!-- Print Notice  -->.*click\\s+on\\s+the\\s+year.*<br\\s*/>\\s*(<table)", "$1 ");
		details = details.replaceAll("(?is)<tr>\\s*<td[^>]+>\\s*(<!--\\s*Total\\s*-->)", "$1");
		details = details.replaceAll("(?is)(<!--\\s*Total\\s*-->.*</table>)\\s*</td>\\s*</tr>\\s*</table>", "</table>\n$1");
		/*if(istart >= 0) {
			iend = details.indexOf("</table>", istart);
			details = details.substring(0,istart) + details.substring(iend + 8);
		}*/
		String legalLink = getLegalLink(details);
		if(!legalLink.isEmpty()) {
			details = details.replace(legalLink, "#");
			details = details.replaceFirst("<a[^>]*>\\s*Click Here\\s*</a>", "See below");
			legalLink = getCrtServerLink() + "/TaxQry/" + legalLink;
			details += getLegal(legalLink);
		}
		details = addYearsDetail(details);
		//FileUtils.writeTextFile("d://legal.html", details);

		// remove all links
		details = details.replaceAll("(?i)</?(A|FORM|INPUT|IMG)[^>]*>", "");
		details = details.replaceAll("</html>", "").replaceAll("</body>", "");
		return details;
	}
	
	private String addYearsDetail(String details) {
		Pattern pattern = Pattern.compile("<a (href=\"(drilldown.aspx[^\"]+)\")");
		Matcher matcher = pattern.matcher(details);
		Vector<String> yearLinks = new Vector<String>();
		while(matcher.find()){
			yearLinks.add(matcher.group(2));
			details = details.replace(matcher.group(1), "");
		}
		if(yearLinks.size() > 0) {
			details += "<span style=\"font-size: 10pt; font-family: Arial; text-decoration: underline\"><strong>Transaction Details</strong></span>";
			details += "<table width=\"95%\" style=\" font-family:Arial; font-size: 9pt;\">" + 
            	"<tr style=\"background-color:#009999; font-weight: bold; color: white;\">" + 
                "<th align=\"center\" style=\"text-decoration: underline;\">Transaction#</th>" + 
                "<th align=\"center\" style=\"text-decoration: underline;\">Date</th>" + 
                "<th align=\"center\" style=\"text-decoration: underline;\">Type</th>" +
                "<th align=\"center\" style=\"text-decoration: underline;\">Amount</th></tr>";
		}
		int istart = 0; 
		int iend = 0;
		int lastYear  = 0;
		String link = "";
		String content = "";
		for (String yearLink : yearLinks) {
			String yearContent = getLinkContents(getCrtServerLink() + "/TaxQry/" +yearLink.replaceAll("&amp;", "&"));
			if (lastYear == 0) {
				content = yearContent;
				lastYear++;
			}
			istart = yearContent.indexOf("PrintReceipt.aspx");
			if(istart > 0){
				istart = yearContent.indexOf("<td", istart);
				iend = yearContent.indexOf("</table>", istart);
				if(istart > 0 && iend > 0) {
					String transacDetails = yearContent.substring(istart, iend + 5);
					transacDetails = transacDetails.replaceAll("(?is)<td[^>]+>\\s*<a[^>]+>\\s*.*?</td>", "");
					transacDetails = transacDetails.replaceAll("(?is)</tab(le>)?", "");
					details += "<tr>" + transacDetails;
				}
			}
		}
		if(yearLinks.size() > 0) {
			details += "</table>";
		}
		istart = content.indexOf("PrintReceipt.aspx"); // We take Base Amount for taxes that have been paid from Print Receipt 
		iend = content.indexOf("</html>");
		if(istart > 0 && iend > 0) {
			link = content.substring(istart, iend);
			link = link.replaceAll("(?is)(PrintReceipt.aspx[^\\\"]+).*", "$1");
		}
		String taxDetailsLastYear = "";
		if (!StringUtils.isEmpty(link)) {
			taxDetailsLastYear = getLinkContents(getCrtServerLink() + "/TaxQry/" +link.replaceAll("&amp;", "&"));
			 if (!taxDetailsLastYear.contains("Error")) {
				 taxDetailsLastYear = taxDetailsLastYear.replaceAll("(?is).*(<div id=\\\"Section2.*)<div id=\\\"Section4.*", "$1");
			/*String tableHeader = taxDetailsLastYear.
				replaceAll("(?is).*<div id=\\\"Section2.*(<div class[^>]+>\\s*<div style[^>]+>\\s*<div style[^>]+>\\s*<span class[^>]+>Date.*<span class[^>]+>Conv[^<]+?</span>).*",
					"$1");
			tableHeader = tableHeader.replaceAll("(?is)</?div[^>]*>", "");
			tableHeader = tableHeader.replaceAll("(?is)<span[^>]*>", "<td>");
			tableHeader = tableHeader.replaceAll("(?is)</span>", "</td>");
			*/
				taxDetailsLastYear = taxDetailsLastYear.replaceAll("(?is).*(<div id=\"Section3.*)", "$1");
				taxDetailsLastYear = taxDetailsLastYear.replaceAll("(?is)</?div[^>]*>", "");
				taxDetailsLastYear = taxDetailsLastYear.replaceAll("(?is).*?<span[^>]*>(.*?)</span>", "<td>$1</td>");
				taxDetailsLastYear = taxDetailsLastYear.replaceAll("(?is)</tr>.*", "");
				taxDetailsLastYear = "<table style=\"display: none\"><tr><td>Base Amount</td>" + taxDetailsLastYear + "</tr></table>";
			 } else taxDetailsLastYear = "<table style=\"display: none\"><tr><td>Base Amount</td><td></td><td></td><td></td></tr></table>";
		} else taxDetailsLastYear = "<table style=\"display: none\"><tr><td>Base Amount</td><td></td><td></td><td></td></tr></table>";
		
		details = details + taxDetailsLastYear;
		return details;
	}

	private String getLegal(String linkContents) {
		String legal = getLinkContents(linkContents);
		
		if(StringUtils.isEmpty(legal))
			return "";
		
		int istart = legal.indexOf("<!-- Dispaly parcel info -->");
		int iend = legal.indexOf("</tr>",istart);
		
		String result = "";
		if(istart != -1 && iend != -1) {
			result = legal.substring(istart, iend) + "</tr></table>";
		}
		
		istart = legal.indexOf("<!-- Dispaly Legal Description -->");
		iend = legal.indexOf("<!-- Back -->");
		if(iend >= 0) {
			iend = legal.lastIndexOf("</table>", iend);
		}
		if(istart != -1 && iend != -1) {
			result += legal.substring(istart, iend + 8);
		}
		
		return result;
	}

	private String getCrtServerLink() {
		String link = getDataSite().getLink();
		int idx = link.indexOf("/Tax");
		if(idx == -1){
			throw new RuntimeException("County " + getCurrentServerName() + " not supported by this class!");
		}
		return link.substring(0, idx);	
	}

	private String getLegalLink(String details) {
		Pattern pattern = Pattern.compile("<a href=\"(LegalDesc.aspx[^\"]+)\"");
		Matcher matcher = pattern.matcher(details);
		if(matcher.find()){
			return matcher.group(1);			
		} else {
			return "";
		}
	}

	/**
	 * Get intermediate results table
	 * @param response
	 * @return
	 */
	protected String getIntermediateResults(String response) {
		String interm = null;
		int iTmp = response.indexOf("Parcel Number");
		if(iTmp == -1) {
			return null;
		}
		iTmp = response.substring(0,iTmp).lastIndexOf("<table");
		int iTmp2 = response.indexOf("Choose a Parcel ID Number link to view the tax receivable history");
		iTmp2 = response.substring(0,iTmp2).lastIndexOf("</table>");
		interm =  response.substring(iTmp, iTmp2 + 8);
		
		iTmp = interm.indexOf("<tr>");
		iTmp2 = interm.indexOf("</tr>");
		interm = interm.substring(0, iTmp) + interm.substring(iTmp2 + 5);
		interm = interm.replace(" style=\"font-size: 9pt;\"", "");
		interm = interm.replace(" align=\"left\"", "");
		interm = interm.replace(" valign=\"top\"", "");
		interm = interm.replace(" class=\"databoundLinkBackground\"", "");
		interm = interm.replaceAll("<tr>\\s*<td[^>]*>\\s*", "<tr><td>");
		
		return interm;
	}

	/**
	 * 
	 * @param rsResponce
	 * @return
	 */
	private String getParcelNoFromResponse(String rsResponce) {
		// find parcel No. in html
		int parcelIdIndex = rsResponce.indexOf("Parcel ID#:");
		if (parcelIdIndex != -1) {
			rsResponce = rsResponce.substring(parcelIdIndex);

			Pattern p = Pattern.compile("<td[^>]*>([^<]+)<");
			Matcher m = p.matcher(rsResponce);
			if (m.find()) {
				return m.group(1).replaceAll("\\s", "");
			}
		}
		return "parcelNo";
	}
	
	/**
	 * Rewrite intermediate links to work for ATS
	 * @param form
	 * @param response
	 * @return
	 */
	private String rewriteIntermediateLinks(String interm, String action, int seq) {

		String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
		
		Pattern pattern = Pattern.compile("(?i)<a id=\"[^\"]*\" href=\"javascript:__doPostBack\\(&#39;([^&#;]*)&#39;,&#39;([^&#39;]*)&#39;\\)\\\">");
		Matcher matcher = pattern.matcher(interm);
		while (matcher.find()) {
			String link = matcher.group(0);
			String target = matcher.group(1);
			String argument = matcher.group(2);
			String newLink = "";
			try {
				newLink = "<a href=\"" + linkStart + "/" + URLEncoder.encode(action, "UTF-8") + 
						"&__EVENTTARGET=" + target + "&__EVENTARGUMENT=" + argument + "&seq=" + seq + "\">";
				interm = interm.replace(link, newLink);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return interm;
	}

	/**
	 * 
	 * @param parcelNo
	 * @return
	 */
	private String getOriginalLink(String parcelNo) {
		return "/TaxQry/Inquiry.asp&ParcelID=" + parcelNo;
	}

	@Override
	protected String getFileNameFromLink(String link) {
		String parcelId = StringUtils.getTextBetweenDelimiters("ParcelID=", "&", link).trim();
		return parcelId + ".html";
	}

	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}

}

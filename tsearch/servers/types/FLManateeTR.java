package ro.cst.tsearch.servers.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.servers.functions.FLGenericPacificBlueTR.FLGenericPacificBlueTRParseType;
import ro.cst.tsearch.utils.StringUtils;

public class FLManateeTR extends FLGenericPacificBlueTR {
	/**
	 * 
	 */
	private static final long serialVersionUID = 148731310746937763L;


	public FLManateeTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	private static final Pattern SALES_LINK_PAT = Pattern.compile("(?is)href\\s*=\\s*'\\.\\.([^']+)'[^>]*>\\s*Sales");
	private static final Pattern SALES_LINK_NEXT_PAT = Pattern.compile("(?i)<a\\s+href\\s*=\\s*\\.\\.([^>]+)>\\s*<img.*?next\\s+record");
	private static final Pattern NO_OF_PAGES_PAT = Pattern
			.compile("(?i)<table[^>]+>\\s*<tr[^>]*>\\s*<td[^>]*>\\s*<font[^>]*>\\s*[nbsp;\\&]+\\s*\\d+\\s*[nbsp;\\&]+\\s*of\\s*[nbsp;\\&]+\\s*(\\d+)\\s*[nbsp;\\&]+\\s*</font");

	
	@Override
	protected  ro.cst.tsearch.servers.functions.FLGenericPacificBlueTR getParser()  {
		return ro.cst.tsearch.servers.functions.FLGenericPacificBlueTR.getInstance(FLGenericPacificBlueTRParseType.FLManateeTR);
	}

	protected String getLinkForAssessorPage(String pin) {
		String linkForAssessor = getBaseLink() + "?assessorLink=assessorLink:" + pin;
		return linkForAssessor;
	}
	
	protected String getPinFromLink(String linkAO) {
		String pin = StringUtils.extractParameterFromUrl(linkAO, "pin");
		return pin;
	}

	protected String addAppraissalDataToContents(String contents, String linkForAssessor, String assessorPage) {
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			assessorPage = ((ro.cst.tsearch.connection.http2.FLGenericPacificBlueTR) site)
					.getAssessorPage(linkForAssessor, HTTPRequest.GET);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		if (StringUtils.isNotEmpty(assessorPage)) {
			Matcher mat = SALES_LINK_PAT.matcher(assessorPage);
			if (mat.find()) {
				String salesLink = "http://www.manateepao.com" + mat.group(1);
				HTTPRequest req = new HTTPRequest(salesLink, HTTPRequest.GET);
				HTTPResponse res = null;
				try {
					res = site.process(req);
				} finally {
					HttpManager.releaseSite(site);
				}
				String resp = res.getResponseAsString();

				contents += "<br><br><br><b>Sale</b>";
				contents = addSale(contents, resp, "table", "account");
				int noOfPages = 1;
				Matcher matPages = NO_OF_PAGES_PAT.matcher(resp);
				if (matPages.find()) {
					noOfPages = Integer.parseInt(matPages.group(1));
				}
				for (int i = 1; i < noOfPages; i++) {
					resp = resp.replaceAll("(?is)(<a\\s+)", "\r\n$1");
					mat.reset();
					mat = SALES_LINK_NEXT_PAT.matcher(resp);
					if (mat.find()) {
						salesLink = "http://www.manateepao.com" + mat.group(1).replaceAll("\\|", "%7C");
						req = new HTTPRequest(salesLink, HTTPRequest.GET);
						res = null;
						try {
							res = site.process(req);
						} finally {
							HttpManager.releaseSite(site);
						}
						resp = res.getResponseAsString();

						contents = addSale(contents, resp, "table", "account");
					}
				}
			}
		}
		return contents;
	}

	public String addSale(String contents, String resp, String tagName, String name) {

		String table = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tables.size() > 3) {
				for (int i = tables.size() - 1; i > 1; i--) {
					if (tables.elementAt(i).toHtml().toLowerCase().contains("account")) {
						table = tables.elementAt(i).toHtml();
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		contents += "<br><br>" + table.replaceAll("(?is)(<table\\s+)", "$1 id=\"saleTable\" ");

		return contents;
	}

	
	protected String getParcelID(String contents) {
		String pin =  
			StringUtils.extractParameter(contents, "(?is)PROPERTY\\s+ID\\s*#\\s*:(?:\\s*</b>)?\\s*(\\d+)").trim();
		return pin;
	}

}

package ro.cst.tsearch.connection.http2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FrameTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 17, 2011
 */

public class OHSummitTR extends HttpSite {
	public static String	SERVER_LINK		= "http://fiscalweb.summitoh.net/clt/";
	public static String	REFINTG2_PARAMS	= "DOLAND=ON&DOLAND=OFF&DOCARD=ON&DOCARD=OFF&CARDNUM=ALL&DOSALES=ON&DOSALES=OFF&DOPERMITS=ON&DOPERMITS=OFF&DOTAXES=ON&DOTAXES=OFF&DOASSESS=ON&DOASSESS=OFF&DOCAUV=OFF&DOCAUV=OFF&DONOTES=ON&DONOTES=OFF&DOGENINF=ON&DOGENINF=OFF&";

	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest("http://fiscaloffice.summitoh.net/index.php/property-tax-search");

			String resp = execute(req);

			if (resp.contains("Property Tax &amp; Appraisal"))
				return LoginResponse.getDefaultSuccessResponse();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	public void onBeforeRequestExcl(HTTPRequest req) {

		return;
	}

	public void onBeforeRequest(HTTPRequest req) {
		String url = fakeUrl(req.getURL());

		if (url.contains(SERVER_LINK) && (url.contains("refintg2.opt") || url.contains("refintg3.main")) && !url.contains("fakeReq")) {
			try {
				if (req.getMethod() == HTTPRequest.GET)
					req.setURL(url + "&" + REFINTG2_PARAMS + "fakeReq");
				else
					req.setURL(url + "fakeReq");
				HTTPResponse resp = process(req);

				if (resp.getResponseAsString().contains("<TITLE>SUMMIT COUNTY FISCAL OFFICE PROPERTY CARD</TITLE>")) {
					NodeList nodes = new HtmlParser3(resp.getResponseAsString()).getNodeList();
					if (nodes.size() > 0 && (nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("frame"), true)).size() > 0) {

						NodeList aaa = nodes.extractAllNodesThatMatch(new HasAttributeFilter("name", "aaa"));
						NodeList zzz = nodes.extractAllNodesThatMatch(new HasAttributeFilter("name", "zzz"));

						if (aaa.size() > 0 && zzz.size() > 0) {
							String aaaUrl = ((FrameTag) aaa.elementAt(0)).getFrameLocation();
							String zzzUrl = ((FrameTag) zzz.elementAt(0)).getFrameLocation();
							if (StringUtils.isNotEmpty(aaaUrl) && StringUtils.isNotEmpty(zzzUrl)) {
								HTTPResponse aaaResp = process(new HTTPRequest(SERVER_LINK.replace("/clt/", "") + aaaUrl));
								HTTPResponse zzzResp = process(new HTTPRequest(SERVER_LINK.replace("/clt/", "") + zzzUrl));

								//if (aaaResp.getResponseAsString().contains("Glossary") && zzzResp.getResponseAsString().contains("Kristen M. Scalise CPA, CFE")) {
								boolean aaaRespValid = aaaResp.getResponseAsString().contains("Summary");
								boolean zzzRespValidCase1 = zzzResp.getResponseAsString().contains("Kristen M. Scalise CPA, CFE");
								boolean zzzRespValidCase2 = zzzResp.getResponseAsString().contains("The parcel") && zzzResp.getResponseAsString().contains("has been deactivated");
								
								if (aaaRespValid && (zzzRespValidCase1 || zzzRespValidCase2)) {
									String body = resp.body;
									
									if (zzzRespValidCase1 && !zzzRespValidCase2)
										body = body.replaceAll("(?ism)(<frame[^>]*name=aaa[^>]*>)", "$1" + aaaResp.getResponseAsString().replaceAll("\\$", "\\\\\\$"));
									else 
										body = body.replaceAll("(?ism)(<frame[^>]*name=aaa[^>]*>)", "");
									body = body.replaceAll("(?ism)(<frame[^>]*name=zzz[^>]*>)", "$1" + zzzResp.getResponseAsString().replaceAll("\\$", "\\\\\\$"));

									resp.body = body;
									resp.contentLenght = body.length();
									resp.is = IOUtils.toInputStream(body);

									req.setBypassResponse(resp);
								}
							}
						}
					}
				} else if (resp.getResponseAsString().contains("<TITLE>SUMMIT COUNTY FISCAL OFFICE</TITLE>")) {
					resp.is = IOUtils.toInputStream(resp.body);
					req.setBypassResponse(resp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		if (url.contains("fakeReq")) {
			req.setURL(url.replace("fakeReq", ""));
		}
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
	}

	public String fakeUrl(String url) {
		if (url.contains("fakeReq"))
			return url;

		Pattern p = Pattern.compile("(?ism)parcel=([^&$]*)");
		Matcher ma = p.matcher(url);
		if (ma.find() && StringUtils.isNotEmpty(ma.group(1)) && !ma.group(1).contains("%")) {
			url = url.replaceAll(ma.group(), ma.group() + "%25");
		}

		p = Pattern.compile("(?ism)own=([^&$]*)");
		ma.reset();
		ma.usePattern(p);
		if (ma.find() && StringUtils.isNotEmpty(ma.group(1)) && !ma.group(1).contains("%")) {
			url = url.replace(ma.group(), ma.group() + "%25");
		}

		p = Pattern.compile("(?ism)route=([^&$]*)");
		ma.reset();
		ma.usePattern(p);
		if (ma.find() && StringUtils.isNotEmpty(ma.group(1)) && !ma.group(1).contains("%")) {
			url = url.replaceAll(ma.group(), ma.group() + "%25");
		}

		url = url.replaceAll("%([^2][^5])", "%25$1");
		url = url.replaceAll("%$", "%25");
		url = url.replaceAll("own=(.*?)%25(&|$)", "own=$1$2");
		return url;
	}
}

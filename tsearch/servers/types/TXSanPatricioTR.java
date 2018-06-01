package ro.cst.tsearch.servers.types;

import org.htmlparser.Text;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.parser.HtmlParser3;

/** 
 * @author Olivia
 * 
 */

public class TXSanPatricioTR extends TXGenericACTTR {

	private static final long serialVersionUID = 679493777119984078L;

	public TXSanPatricioTR(long searchId) {
		super(searchId);
	}

	public TXSanPatricioTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	} 
	
	
	@Override
	protected String cleanIntermediaryResponse(String rsResponse, String linkStart) {
		String contents;
		String delimiterString = "<table width=\"780\" align=\"center\" cellspacing=\"0\"  cellpadding=\"0\" leftmargin=\"0\" topmargin=\"0\">";
		int indexOf = rsResponse.indexOf(delimiterString);
		
		if (indexOf != -1) {
			contents = rsResponse.substring(indexOf);
		} else {
			contents = rsResponse;
		}
		
		contents = contents.replaceFirst("(?is)<td>[^\\.]+\\.[^\\.]+[^/]+/a>\\s*</h3>\\s*", "<td>");
		contents = contents.replaceFirst("(?is)<p>[^!]+![^!]+!--[^-]+-->\\s*<table", "<#br/> <table");
		contents = contents.replaceFirst("(?is)\\s*<caption[^/]+/caption>\\s*", "");
		contents = contents.replaceAll("(?is)\\s*</td>\\s*</tr>\\s*<style[^<]+</style>", "");
		contents = contents.replaceFirst("(?is)(?:</div>\\s+){3}<tr>\\s*<td>\\s*<hr[^_]+[^<]+</a>\\s*</div>\\s*<br>\\s*</td>\\s*</tr>[^!]+!--[^#]+[^-]+-->\\s+", "");
		contents = contents.replaceFirst("(?is)<div[^>]+>\\s*</div>[^/]+/body>\\s*</html>\\s*", "");
		contents = contents.replaceFirst("(?is)<table[^>]+>\\s*" +
				"(<tr width=\"780\">\\s*<td>\\s*)<table[^>]+>\\s*<\\s*tr\\s*>\\s*<td>\\s*" +
				"(<div[^>]+>)\\s+[^\\\"]+\\\"[^/]+/[^/]+/h3>\\s+<h3>\\s*" +
				"(<strong[^/]+/i>\\s*[^/]+/strong>)?[^#]+#br/>\\s*(<table[^>]+>)", "$4" + "$1" + "$2" + "$3" + " </div> </td>  </tr>");
		
		contents = contents.replaceAll("(?is)<a\\s*href\\s*=\\s*\\\"showlist\\.jsp\\?sort[^\\\"]+\\\">\\s+(<b>[^/]+/b>)\\s*</a>", "$1");
		contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*'([^']+)'[^>]*>", "<a href=\"" + linkStart + "/act_webdev/sanpatricio/" + "$1" + "\">");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)onMouse[^\\\"]+\\\"[^\\\"]+\\\"\\s*", "");
		contents = contents.replaceAll("(?is)<tr\\s{2,}", "<tr ");
		contents = contents.replaceAll("(?is)&nbsp;", " ");
		
		String regExp = "(?is)<table.*(<tr[^>]+>\\s*<td>\\s*<div[^\\.]+\\.[^>]+>\\s*</strong>\\s*</div>\\s*</td>\\s*</tr>)\\s*<tr>\\s*<td>\\s*(<table[^>]+>)";
		if (contents.contains("Only the first 100 accounts of") && contents.contains("matches are shown"))
			contents = contents.replaceFirst(regExp, "$2" + "$1");
		else {
			regExp = "(?is)<table[^>]+>\\s*<tr width=\\\"780\\\">\\s*<td>\\s*<div[^>]+>\\s+</div>\\s*</td>\\s*</tr>\\s*\\s*<tr>\\s*<td>\\s*(<table[^>]+>)";
			contents = contents.replaceFirst(regExp, "$1");
		}
		contents = contents.replaceAll("(?is)(?:</table>\\s+</td>\\s+</tr>)", "");
		
		return contents;

	}
	
	@Override
	protected String cleanDetailsResponse(String response) {
		String contents = "";
		try {
			HtmlParser3 parser = new HtmlParser3(response);
			NodeList mainTableList = parser.getNodeList();
			NodeList nodeList = HtmlParser3.getTag(mainTableList, new TableTag(), true);
			Text tableHeading = HtmlParser3.findNode(mainTableList, "Property Tax Balance");
			if (nodeList != null && nodeList.size() == 5)  {
				nodeList.remove(4);
				nodeList.remove(3);
				nodeList.remove(0);
			}

			contents = "<table>" + "\n" + "<tr><td>" +
					"<h6>" + tableHeading.toHtml() + "</h6>  </td></tr>" + "\n" +
					"<tr><td>" + nodeList.elementAt(0).toHtml() + "</td></tr>" + "\n" +
					"<tr><td>" + nodeList.elementAt(1).toHtml() + "</td></tr>" + "\n" + 
					"</table>"; 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		contents = contents.replaceAll("(?is)<a[^>]+>\\s*[^/]+/a>\\s+(?:(?:<\\s*br\\s*/?>\\s*){3})?", "");
		contents = contents.replaceAll("(?is)<h3>\\s*</h3>", "");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)<i>\\s*Make your check.*?</font>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]+>", "");
		contents = contents.replaceAll("(?is)<form[^>]+>.*</form>", "");
		contents = contents.replaceAll("(?is)(Property\\s+Tax\\s+Balance)", "<b>$1</b>");
		contents = contents.replaceAll("(?is)h6", "h2");
		return contents;
	}
//	
//	@Override
//	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
//		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
//		return super.parseAndFillResultMap(response,detailsHtml,map);
//	}
	
}

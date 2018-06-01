package ro.cst.tsearch.servers.types;

import org.htmlparser.Text;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.parser.HtmlParser3;

public class TXNuecesTR extends TXGenericACTTR {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2009947418006527962L;

	public TXNuecesTR(long searchId) {
		super(searchId);
	}

	public TXNuecesTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected String cleanIntermediaryResponse(String rsResponse, String linkStart) {
		String contents;
		String delimiterString = "<table width=\"80%\" cellpadding=\"4\" cellspacing=\"0\" border=\"1\" align=\"center\">";
		int indexOf = rsResponse.indexOf(delimiterString);
		if (indexOf != -1) {
			contents = rsResponse.substring(indexOf);
		} else {
			contents = rsResponse;
		}
		contents = contents
				.replace("(?im)<caption align=\"right\">Your search took 0 seconds.</caption>.*", "</table>");
		contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*\\\"showlist.jsp\\?sort[^\\\"]+\\\"[^>]*>", "");
		contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*'([^']+)'[^>]*>", "<a href=\"" + linkStart
				+ "/act_webdev/nueces/$1\">");
		contents = contents.replaceAll("(?is)\\s+<table", "<table");
		contents = contents.replaceAll("(?is)onMouse[^\\\"]*\\\"[^\\\"]*\\\"", "");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)<tr\\s{2,}", "<tr ");
		contents = contents.replaceAll("(?is)&nbsp;", " ");
		return contents;

	}

	@Override
	protected String cleanDetailsResponse(String response) {
		String contents = "";
		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList nodeList = HtmlParser3.getTag(mainTableList, new TableTag(), true);
			NodeList tag = HtmlParser3.getTag(mainTableList, new HeadingTag()
					, true);
			Text tableHeading = HtmlParser3.findNode(mainTableList, "Property Tax Balance");
			mainTableList.remove(nodeList.elementAt(1));
			mainTableList.remove(nodeList.elementAt(3));
			nodeList.remove(1);
			nodeList.remove(3);
			contents =
				"<table>" + "\n" +
				"<tr><td><h6>"       + tableHeading.toHtml() + "</td></tr></h6>" +  
				"\n" +"<tr><td>" + nodeList.elementAt(1).toHtml() + "</td></tr>" + 
				"\n"+ "<tr><td>" + nodeList.elementAt(2).toHtml() + "</td></tr>" +
				"\n" + "</table>"; 
//			contents = mainTableList.elementAt(3).getChildren().elementAt(3).getChildren().elementAt(11).getChildren()
//					.elementAt(6).getChildren().elementAt(0).getChildren().elementAt(5).toHtml();
		} catch (Exception e) {
			e.printStackTrace();
		}
		contents = contents.replaceAll("(?is)<a[^>]+>[A-Z\\s]+</a>", "");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)<i>\\s*Make your check.*?</font>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]+>", "");
		contents = contents.replaceAll("(?is)<form[^>]+>.*</form>", "");
		contents = contents.replaceAll("(?is)(Property\\s+Tax\\s+Balance)", "<b>$1</b>");
		contents = contents.replaceAll("(?is)h6", "h2");
		return contents;
	}
	
	@Override
	protected String getPID(String details) {
		String pid = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tempList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			pid = HtmlParser3.findNode(tempList, "Account Number").getText();
			pid = pid.replaceAll("(?is)[^\\d]+(\\d+).*", "$1");

		} catch(Exception e) {
			e.printStackTrace();
		}
		return pid;
	}
}

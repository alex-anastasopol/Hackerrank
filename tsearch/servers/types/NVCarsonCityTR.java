/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.util.Map;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.servers.TSConnectionURL;

/**
 * @author vladb
 *
 */
public class NVCarsonCityTR extends NVGenericCountyTR {

	private static final long serialVersionUID = 1L;

	public NVCarsonCityTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected TableTag extractIntermediaryTable(NodeList nodeList) {
		
		TableTag interTable = (TableTag) nodeList
			.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), false)
			.extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "white"), false)
			.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"), false)
			.elementAt(0);
		
		return interTable;
	}
	
	@Override
	protected String processFooter(String page, StringBuilder footer) {
		
		try {
			Form form = new SimpleHtmlParser(page).getForm("Results");
			Map<String, String> params = form.getParams();
			
			String prevLink = CreatePartialLink(TSConnectionURL.idPOST) + "/cgi-bin/tcw100?";
			String nextLink = CreatePartialLink(TSConnectionURL.idPOST) + "/cgi-bin/tcw100?";
			for(Map.Entry<String, String> entry: params.entrySet()){
				String name = entry.getKey();
				String value = entry.getValue();
				if(name.equals("CGIOption")) {
					prevLink += name + "=" + "Page Up" + "&";
					nextLink += name + "=" + "Page Down" + "&";
				} else {
					prevLink += name + "=" + value + "&";
					nextLink += name + "=" + value + "&";
				}
			}
			footer.append("<tr><td colspan=\"5\" align=\"center\"><a href=\"" + prevLink + "\">Prev</a>&nbsp;&nbsp;&nbsp;" +
					"<a href=\"" + nextLink + "\">Next</a></td></tr></table>");
			
			return nextLink;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}

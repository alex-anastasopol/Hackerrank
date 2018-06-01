/**
 * 
 */
package ro.cst.tsearch.connection.http2;


import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
 *
 *for  Collin, Denton, Tarrant -like sites        ADD here the new county implemented with this Generic
 */

public class TXGenericSMediaTR extends HttpSite {

	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		String accountNumber = StringUtils.extractParameterFromUrl(req.getURL(), "accountNumber");
		if(StringUtils.isNotEmpty(accountNumber)) {
			String oldUrl = req.getURL();
			String url = oldUrl.replaceAll("(?is)accountInfo.asp.*", "") + "searchResults.asp";
			req.setMethod(HTTPRequest.POST);
			req.setURL(url);
			req.setPostParameter("cboSearch", "1");
			req.setPostParameter("txtSearch", accountNumber);
			req.setPostParameter("Go.x", "0");
			req.setPostParameter("Go.y", "0");
			String page = process(req).getResponseAsString();
			Parser parser = Parser.createParser(page, null);
			try {
				String table = "";
				NodeList mainList = parser.parse(null);
				NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				for (int k = tables.size() - 1; k < tables.size(); k--){
					if (tables.elementAt(k).toHtml().contains("Legal")){
						table = tables.elementAt(k).toHtml();
						break;
					}
				}
				table = table.replaceAll("\\s*</?font[^>]*>\\s*", "").replaceAll("(?is)<!--[^-]+-->", "");
				Parser tableParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = tableParser.parse(null);
				TableTag mainTable = (TableTag)mainTableList.elementAt(0);
				
				TableRow[] rows = mainTable.getRows();
							
				for(int i = 1; i < rows.length; i++ ) {
					if(rows[i].getColumnCount() > 1) {						
						TableColumn[] cols = rows[i].getColumns();
						String parcelNo = cols[0].toHtml().replaceAll("</?td[^>]*>", "").replaceAll("</?a[^>]*>", "").trim();
						if(accountNumber.equals(parcelNo)){
							String link = oldUrl.replaceAll("(?is)accountInfo.asp.*", "") + rows[i].toHtml().replaceAll("(?is).*?href[^\\\"]+\\\"([^\\\"]+).*", "$1");
							req.setMethod(HTTPRequest.GET);
							req.removePostParameters("cboSearch");
							req.removePostParameters("txtSearch");
							req.removePostParameters("Go.x");
							req.removePostParameters("Go.y");
							req.modifyURL(link);
							req.setHeader("Referer", oldUrl.replaceAll("(?is)accountInfo.asp.*", "") + "accountSearch.asp");
							return;
						}
					}
				}		
			} catch (ParserException e) {
				e.printStackTrace();
			}
		}
	}
	
}

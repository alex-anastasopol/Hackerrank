package ro.cst.tsearch.servers.functions;

import java.util.Vector;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.LinkTag;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author Vladimir
 *
 */
public class VUWriter {

	public static ResultMap parseIntermediaryRow(Bullet bullet) {
		ResultMap map = new ResultMap();
		map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "Bulletin");
		LinkTag linkTag = (LinkTag) bullet.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
		if(linkTag != null) {
			String instNo = linkTag.getLinkText();
			if (instNo!=null) {
				String[] split = instNo.trim().split("\\s+-\\s+");
				if (split.length>=2) {
					map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), split[0].trim());
				}
			}
		}
		
		return map;
	}

	public static void parseNames(ResultMap map, String grantee) {
		String names = grantee;
		if(StringUtils.isEmpty(names)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		String[] lines = new String[]{names};
		
		map.put("tmpAll", names);
		
		COFremontAO.genericParseNames(map, "GranteeSet", lines, null, companyExpr, 
				new Vector<String>(), new Vector<String>(), false, COFremontAO.ALL_NAMES_FL, -1);
	}

}

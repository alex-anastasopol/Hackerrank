
package ro.cst.tsearch.servers.functions;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;

/**
 * @author vladb
 *
 */
public class NVElkoTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {

		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		InputTag input = (InputTag) row.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("name", "detlparc"), true).elementAt(0);
		if(input != null) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), input.getAttribute("value"));
		}
		
		NVGenericCountyTR.parseNames(resultMap, row.getColumns()[2].toPlainTextString().trim(), false);
		
		return resultMap;
	}
}

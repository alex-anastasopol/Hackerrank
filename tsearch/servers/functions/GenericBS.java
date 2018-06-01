package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class GenericBS {
		
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "BS");
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "BANK SUCCESSOR");
				
		TableColumn[] cols = row.getColumns();
		
		String nameAndRSSDID =  cols[0].toPlainTextString().trim();
		String name = "";
		String rssdID = "";
		Matcher matcher = Pattern.compile("(.*?)\\((.*?)\\)").matcher(nameAndRSSDID);
		if (matcher.find()) {
			name = matcher.group(1).trim();
			rssdID = matcher.group(2).trim();
		}
		
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), rssdID);
		parseNames(resultMap, searchId);
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String names[] = {"", "", "", "", "", ""};
		String type[] = {"", ""};
		String otherType[] = {"", ""};
		
		ArrayList<List> grantor = new ArrayList<List>();
		String grantorName = (String)resultMap.get(SaleDataSetKey.GRANTOR.getKeyName());
		
		if (StringUtils.isNotEmpty(grantorName)){
			
			names[2] = grantorName;
			GenericFunctions.addOwnerNames(grantorName, names, "",	"", 
							type, otherType, true, false, grantor);
		}
			
		try {
			resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
		resultMap.remove("tmpGrantor");
	}
	
}

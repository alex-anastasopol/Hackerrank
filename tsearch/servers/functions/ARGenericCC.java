package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class ARGenericCC {

	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), 
			row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;", "").replaceAll("[(].*?[)]", "").trim());
		
		String[] grantors = row.getColumns()[3].toPlainTextString()
			.replaceAll("&nbsp;", "").split(";");
		ArrayList<String> grantorList = new ArrayList<String>();
		for(String grantor : grantors) {
			grantorList.add(grantor);
		}
		parseNames(resultMap, grantorList, "GrantorSet");
		resultMap.removeTempDef();
		
		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, List<String> names, String set) {
		if(names.isEmpty()) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		String[] namesArray = new String[names.size()];
		
		String allNames = "";
		int i = 0;
		for(String name : names) {
			if(!StringUtils.isEmpty(name)) {
				name = cleanName(name);
				allNames += ", " + name;
				namesArray[i++] = name;
			}
		}
		allNames = allNames.replaceFirst(", ", "");
		resultMap.put("tmpAll", allNames);
		
		COFremontAO.genericParseNames(resultMap, set, namesArray, null, companyExpr, 
				new Vector<String>(), new Vector<String>(), false, COFremontAO.ALL_NAMES_FL, -1);
	}

	private static String cleanName(String name) {
		String properName = name.trim();
		properName = properName.replace(".", "");
		
		return properName;
	}

}

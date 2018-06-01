package ro.cst.tsearch.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;

public class ResultBodyUtils {

	/**
	 * 
	 * @param map
	 * @param saleDataSet
	 */
	public static void buildSaleDataSet(ResultMap map, List<HashMap<String, String>> saleDataSet) {
		HashMap<String, String> expectedValuesToActualValues = new HashMap<String,String>();
		buildSaleDataSet(map, saleDataSet, expectedValuesToActualValues);
	}

	public static void buildSaleDataSet(ResultMap map, List<HashMap<String, String>> saleDataSet, HashMap<String,String> expectedValuesToActualValues) {
		
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		String defaultStr = "";
		
		String bookKey = StringUtils.defaultIfEmpty(expectedValuesToActualValues.get("Book"), "Book");
		String pageKey = StringUtils.defaultIfEmpty(expectedValuesToActualValues.get("Page"), "Page");
		String intstrDateKey = StringUtils.defaultIfEmpty(expectedValuesToActualValues.get("InstrumentDate"), "InstrumentDate");
		String docTypeKey = StringUtils.defaultIfEmpty(expectedValuesToActualValues.get("DocumentType"), "DocumentType");
		String salesPriceKey = StringUtils.defaultIfEmpty(expectedValuesToActualValues.get("SalesPrice"), "SalesPrice");
		String instrNoKey = StringUtils.defaultIfEmpty(expectedValuesToActualValues.get("InstrumentNumber"), "InstrumentNumber");
		
		for (HashMap<String, String> list : saleDataSet) {
			line = new ArrayList<String>();
			line.add(StringUtils.defaultIfEmpty(list.get(bookKey), defaultStr));
			line.add(StringUtils.defaultIfEmpty(list.get(pageKey), defaultStr));
			line.add(StringUtils.defaultIfEmpty(list.get(intstrDateKey), defaultStr));
			line.add(StringUtils.defaultIfEmpty(list.get(docTypeKey), defaultStr));
			line.add(StringUtils.defaultIfEmpty(list.get(salesPriceKey), defaultStr));
			line.add(StringUtils.defaultIfEmpty(list.get(instrNoKey), defaultStr));
			body.add(line);
		}

		// adding all cross references - should contain transfer table and info
		// parsed from legal description
		if (body != null && body.size() > 0) {
			ResultTable rt = new ResultTable();
			String[] header = { "Book", pageKey, intstrDateKey, docTypeKey, salesPriceKey, instrNoKey };
			rt = GenericFunctions2.createResultTable(body, header);
			map.put("SaleDataSet", rt);
		}
		
	}

	public static void buildInfSet(ResultMap map, List<HashMap<String, String>> sourceSet,String[] header, Class setName) {
		buildInfSet(map, sourceSet, header, null, setName);
	}
	
	public static void buildInfSet(ResultMap map, List<HashMap<String, String>> sourceSet,String[] header,
			Map<String,String> resultBodyHeaderToSourceTableHeader, Class setName) {
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		String defaultStr = "";
		
		for (HashMap<String, String> list : sourceSet) {
			line = new ArrayList<String>();
			
			for (String headerName : header) {
				if (resultBodyHeaderToSourceTableHeader != null){
					 headerName = resultBodyHeaderToSourceTableHeader.get(headerName);
				}
				line.add(StringUtils.defaultIfEmpty(list.get(headerName), defaultStr));
			}
			
			body.add(line);
		}
	
		if (body != null && body.size() > 0) {
			ResultTable rt = new ResultTable();
			rt = GenericFunctions2.createResultTable(body, header);
			map.put(setName.getSimpleName(), rt);
		}
	}
	
}
